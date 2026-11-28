package com.example.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Multi-Source Knowledge & Reference Registry shared across every connected AI provider.
 *
 * - Persists [KnowledgeSourceEntry] items to `File(context.filesDir, "omni_ai_sources.json")`
 *   (keeping the Room database schema completely untouched).
 * - Supports local files (`.md`, `.txt`, `.json` via SAF), validated public HTTPS / Raw GitHub URLs
 *   (with SSRF protection blocking private, loopback, link-local, and non-HTTPS targets),
 *   pasted Notebook/lore notes, and the live active PixelForge project context.
 * - Assembles a cleanly budgeted context block (`### [Source Title]`) trimmed safely within `maxChars`.
 */
class MultiSourceContextRegistry(private val appContext: Context? = null) {

    private val lock = Any()
    private val sources = mutableListOf<KnowledgeSourceEntry>()

    private val fetchHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    init {
        loadFromDisk()
        ensureBuiltInLayer1BasePresent()
        ensureActiveProjectSourcePresent()
    }

    /**
     * Ensures the built-in Layer 1 Base entry exists at index `0` of [sources] without duplicating it.
     * Modular Layer 2 plug-in slots intentionally start empty until the user plugs one in.
     */
    fun ensureBuiltInLayer1BasePresent() = synchronized(lock) {
        val existingIdx = sources.indexOfFirst { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }
        if (existingIdx < 0) {
            sources.add(
                0,
                KnowledgeSourceEntry(
                    id = DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID,
                    title = DefaultAiArtTrainingProfiles.LAYER_1_TITLE,
                    kind = KnowledgeSourceKind.NOTEBOOK_MARKDOWN,
                    originLabel = "Built-In Layer 1 Canonical AI Base",
                    content = DefaultAiArtTrainingProfiles.LAYER_1_CANONICAL_PROFILE,
                    enabled = true
                )
            )
            persistToDisk()
        } else if (existingIdx > 0) {
            val entry = sources.removeAt(existingIdx)
            sources.add(0, entry)
            persistToDisk()
        }
    }

    /**
     * Updates the built-in Layer 1 Base content in-place at index `0` and persists to disk.
     */
    fun updateBuiltInLayer1Content(customContent: String) = synchronized(lock) {
        ensureBuiltInLayer1BasePresent()
        val cleaned = customContent.trim().take(MAX_SINGLE_SOURCE_CHARS)
        val effectiveContent = cleaned.ifBlank { DefaultAiArtTrainingProfiles.LAYER_1_CANONICAL_PROFILE }
        val idx = sources.indexOfFirst { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }
        if (idx >= 0) {
            sources[idx] = sources[idx].copy(
                id = DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID,
                title = DefaultAiArtTrainingProfiles.LAYER_1_TITLE,
                content = effectiveContent,
                enabled = true,
                updatedAtMillis = System.currentTimeMillis()
            )
            persistToDisk()
        }
    }

    /**
     * Restores the built-in Layer 1 Base cleanly to [DefaultAiArtTrainingProfiles.LAYER_1_CANONICAL_PROFILE].
     */
    fun resetBuiltInLayer1ToDefault() = synchronized(lock) {
        ensureBuiltInLayer1BasePresent()
        val idx = sources.indexOfFirst { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }
        if (idx >= 0) {
            sources[idx] = sources[idx].copy(
                id = DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID,
                title = DefaultAiArtTrainingProfiles.LAYER_1_TITLE,
                content = DefaultAiArtTrainingProfiles.LAYER_1_CANONICAL_PROFILE,
                enabled = true,
                updatedAtMillis = System.currentTimeMillis()
            )
            persistToDisk()
        }
    }

    fun getBuiltInLayer1Content(): String = synchronized(lock) {
        sources.firstOrNull { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }?.content
            ?: DefaultAiArtTrainingProfiles.LAYER_1_CANONICAL_PROFILE
    }

    fun isBuiltInLayer1Customized(): Boolean = synchronized(lock) {
        getBuiltInLayer1Content().trim() != DefaultAiArtTrainingProfiles.LAYER_1_CANONICAL_PROFILE.trim()
    }

    fun isBuiltInLayer1Enabled(): Boolean = synchronized(lock) {
        sources.firstOrNull { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }?.enabled ?: true
    }

    fun setBuiltInLayer1Enabled(enabled: Boolean) = synchronized(lock) {
        ensureBuiltInLayer1BasePresent()
        val idx = sources.indexOfFirst { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }
        if (idx >= 0) {
            sources[idx] = sources[idx].copy(
                enabled = enabled,
                updatedAtMillis = System.currentTimeMillis()
            )
            persistToDisk()
        }
    }

    /**
     * Attaches a user-supplied modular Layer 2 plug-in (e.g., franchise art bible, specific game style,
     * or custom rulebook) that stacks immediately after Layer 1 in [buildCombinedContextBlock].
     */
    fun attachLayer2Plugin(
        pluginName: String,
        content: String,
        enabled: Boolean = true
    ): KnowledgeSourceEntry = synchronized(lock) {
        ensureBuiltInLayer1BasePresent()
        val cleanName = pluginName.trim().ifBlank { "Layer 2 Style Plug-In" }
        val cleanContent = content.trim().take(MAX_SINGLE_SOURCE_CHARS)
        val entry = KnowledgeSourceEntry(
            id = "${DefaultAiArtTrainingProfiles.LAYER_2_PLUGIN_ID_PREFIX}${UUID.randomUUID().toString().take(8)}",
            title = cleanName,
            kind = KnowledgeSourceKind.NOTEBOOK_MARKDOWN,
            originLabel = "Layer 2 Modular Plug-In",
            content = cleanContent,
            enabled = enabled,
            updatedAtMillis = System.currentTimeMillis()
        )
        // Insert right after index 0 (Layer 1) and any existing Layer 2 plug-ins
        val insertIdx = sources.indexOfLast {
            it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID || DefaultAiArtTrainingProfiles.isLayer2PluginSource(it)
        } + 1
        sources.add(insertIdx.coerceIn(1, sources.size), entry)
        persistToDisk()
        entry
    }

    fun listLayer2Plugins(): List<KnowledgeSourceEntry> = synchronized(lock) {
        sources.filter { DefaultAiArtTrainingProfiles.isLayer2PluginSource(it) }
    }

    /**
     * Assembles the complete shared AI Base context in strict, prefix-stable layered order
     * (zero changing timestamps at byte 0 for local LLM KV-cache prompt reuse):
     * 1. `[LAYER 1 BASE]` (full profile or [DefaultAiArtTrainingProfiles.LAYER_1_MICRO_DIGEST] in low-token micro mode)
     * 2. `[LAYER 2 PLUG-IN(S)]` (any enabled modular Layer 2 plug-ins)
     * 3. `[USER SOURCES]` (enabled Active Project Context, user knowledge files, and pinned training examples)
     */
    fun buildCombinedContextBlock(
        maxTotalChars: Int = DEFAULT_MAX_CONTEXT_CHARS,
        lowTokenMicroMode: Boolean = false,
        queryHint: String = "",
        activeProjectSummary: String = ""
    ): String {
        val safeBudget = (if (lowTokenMicroMode) minOf(maxTotalChars, 1_400) else maxTotalChars).coerceAtLeast(240)
        val builder = StringBuilder(safeBudget.coerceAtMost(16_384))

        fun appendSectionBudgeted(sectionText: String) {
            if (sectionText.isBlank()) return
            val sep = if (builder.isNotEmpty()) "\n\n" else ""
            val remaining = safeBudget - builder.length
            if (remaining <= sep.length + 24) return
            builder.append(sep)
            val allow = safeBudget - builder.length
            if (sectionText.length <= allow) {
                builder.append(sectionText)
            } else {
                val suffix = "\n...[trimmed]"
                val takeLen = (allow - suffix.length).coerceAtLeast(0)
                if (takeLen > 0) {
                    builder.append(sectionText.substring(0, takeLen).trimEnd())
                    builder.append(suffix)
                }
            }
        }

        val (layer1Section, layer2Section) = synchronized(lock) {
            val l1Entry = sources.firstOrNull { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }
            val l1Text = if (l1Entry != null && l1Entry.enabled && l1Entry.content.isNotBlank()) {
                val body = if (lowTokenMicroMode) {
                    DefaultAiArtTrainingProfiles.LAYER_1_MICRO_DIGEST
                } else {
                    l1Entry.content.trim()
                }
                "[LAYER 1 BASE]\n$body"
            } else {
                ""
            }

            val enabledL2 = sources.filter {
                DefaultAiArtTrainingProfiles.isLayer2PluginSource(it) && it.enabled && it.content.isNotBlank()
            }
            val l2Text = if (enabledL2.isNotEmpty()) {
                val perPluginCap = if (lowTokenMicroMode) 280 else MAX_SINGLE_SOURCE_CHARS
                val body = enabledL2.joinToString("\n\n") { plugin ->
                    val content = plugin.content.trim()
                    val snippet = if (content.length <= perPluginCap) content else content.take(perPluginCap).trimEnd() + "..."
                    "### [${plugin.title}]\n$snippet"
                }
                "[LAYER 2 PLUG-IN(S)]\n$body"
            } else {
                ""
            }
            l1Text to l2Text
        }

        if (layer1Section.isNotBlank()) appendSectionBudgeted(layer1Section)
        if (layer2Section.isNotBlank()) appendSectionBudgeted(layer2Section)

        val remainingForUserSources = safeBudget - builder.length - if (builder.isNotEmpty()) 18 else 0
        if (remainingForUserSources >= 120) {
            val userBlock = assembleContextBlock(
                queryHint = queryHint,
                activeProjectSummary = activeProjectSummary,
                maxChars = remainingForUserSources
            )
            if (userBlock.isNotBlank()) {
                appendSectionBudgeted("[USER SOURCES]\n$userBlock")
            }
        }

        val combined = builder.toString().trim()
        return if (combined.length <= safeBudget) combined else combined.substring(0, safeBudget)
    }

    fun getSources(): List<KnowledgeSourceEntry> = synchronized(lock) {
        sources.toList()
    }

    fun addSource(entry: KnowledgeSourceEntry): List<KnowledgeSourceEntry> = synchronized(lock) {
        ensureBuiltInLayer1BasePresent()
        val cleaned = entry.copy(
            title = entry.title.trim().ifBlank { "Untitled Reference" },
            content = entry.content.trim().take(MAX_SINGLE_SOURCE_CHARS),
            updatedAtMillis = System.currentTimeMillis()
        )
        val existingIdx = sources.indexOfFirst { it.id == cleaned.id }
        if (existingIdx >= 0) {
            sources[existingIdx] = cleaned
        } else if (cleaned.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID) {
            sources.add(0, cleaned)
        } else {
            // Keep index 0 reserved for Layer 1 Base
            sources.add(1.coerceAtMost(sources.size), cleaned)
        }
        persistToDisk()
        sources.toList()
    }

    fun addPastedNoteSource(
        title: String,
        content: String,
        kind: KnowledgeSourceKind = KnowledgeSourceKind.PASTED_NOTES,
        originLabel: String = "Pasted in AI Hub"
    ): List<KnowledgeSourceEntry> {
        return addSource(
            KnowledgeSourceEntry(
                id = "src_${UUID.randomUUID().toString().take(8)}",
                title = title.trim().ifBlank { "Notebook Lore Notes" },
                kind = kind,
                originLabel = originLabel,
                content = content,
                enabled = true
            )
        )
    }

    fun toggleSource(id: String, enabled: Boolean): List<KnowledgeSourceEntry> = synchronized(lock) {
        val idx = sources.indexOfFirst { it.id == id }
        if (idx >= 0) {
            sources[idx] = sources[idx].copy(enabled = enabled, updatedAtMillis = System.currentTimeMillis())
            persistToDisk()
        }
        sources.toList()
    }

    fun removeSource(id: String): List<KnowledgeSourceEntry> = synchronized(lock) {
        if (id == ACTIVE_PROJECT_SOURCE_ID || id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID) {
            // Keep built-in Layer 1 and live project slots toggleable rather than permanently deleting them
            val idx = sources.indexOfFirst { it.id == id }
            if (idx >= 0) {
                sources[idx] = sources[idx].copy(enabled = false, updatedAtMillis = System.currentTimeMillis())
                persistToDisk()
            }
        } else {
            sources.removeAll { it.id == id }
            persistToDisk()
        }
        sources.toList()
    }

    /**
     * Imports a local `.md`, `.txt`, or `.json` document (such as an exported Gemini Notebook `.md`
     * or `world_bible.json`) via Android Storage Access Framework (`OpenDocument`).
     */
    suspend fun importLocalDocumentUri(
        context: Context,
        uri: Uri
    ): Result<KnowledgeSourceEntry> = withContext(Dispatchers.IO) {
        try {
            val fileName = queryDisplayName(context, uri) ?: "Imported_Source.md"
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                val rawBytes = readCappedBytes(stream.readBytes(), MAX_IMPORT_BYTES)
                String(rawBytes, Charsets.UTF_8)
            }.orEmpty().trim()

            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("The selected file is empty."))
            }

            val lower = fileName.lowercase()
            val detectedKind = if (lower.endsWith(".md") || lower.contains("notebook")) {
                KnowledgeSourceKind.NOTEBOOK_MARKDOWN
            } else {
                KnowledgeSourceKind.LOCAL_FILE
            }
            val title = fileName.substringBeforeLast('.').ifBlank { fileName }
            val entry = KnowledgeSourceEntry(
                id = "src_${UUID.randomUUID().toString().take(8)}",
                title = title,
                kind = detectedKind,
                originLabel = fileName,
                content = text.take(MAX_SINGLE_SOURCE_CHARS),
                enabled = true
            )
            addSource(entry)
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches a public HTTPS text/markdown/JSON URL (such as a Raw GitHub document) after strict
     * SSRF validation blocking non-HTTPS schemes, localhost, private RFC1918 networks, link-local
     * metadata endpoints, and unsafe redirects.
     */
    suspend fun fetchAndAddPublicHttpsSource(
        rawUrl: String,
        customTitle: String = "",
        maxRedirectsRemaining: Int = 3
    ): Result<KnowledgeSourceEntry> = withContext(Dispatchers.IO) {
        val validated = validatePublicHttpsUrl(rawUrl, resolveDns = true)
        if (validated.isFailure) {
            return@withContext Result.failure(validated.exceptionOrNull()!!)
        }
        val uri = validated.getOrThrow()

        try {
            val request = Request.Builder()
                .url(uri.toString())
                .get()
                .header("Accept", "text/plain, text/markdown, application/json;q=0.9, */*;q=0.5")
                .header("User-Agent", "PixelForge-OmniAiHub/1.0")
                .build()

            fetchHttpClient.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    if (maxRedirectsRemaining <= 0) {
                        return@withContext Result.failure(IllegalArgumentException("Too many redirects while fetching remote source."))
                    }
                    val redirectLocation = response.header("Location")
                        ?: return@withContext Result.failure(IllegalArgumentException("Blocked unsafe redirect without Location header."))
                    val resolvedRedirect = uri.resolve(redirectLocation).toString()
                    val redirectCheck = validatePublicHttpsUrl(resolvedRedirect, resolveDns = true)
                    if (redirectCheck.isFailure) {
                        return@withContext Result.failure(redirectCheck.exceptionOrNull()!!)
                    }
                    return@withContext fetchAndAddPublicHttpsSource(
                        rawUrl = resolvedRedirect,
                        customTitle = customTitle,
                        maxRedirectsRemaining = maxRedirectsRemaining - 1
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Failed to fetch URL (HTTP ${response.code}).")
                    )
                }

                val rawBytes = response.body?.bytes() ?: ByteArray(0)
                val capped = readCappedBytes(rawBytes, MAX_IMPORT_BYTES)
                val text = String(capped, Charsets.UTF_8).trim()
                if (text.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Remote document returned empty content."))
                }

                val inferredName = uri.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: uri.host
                val title = customTitle.trim().ifBlank { inferredName ?: "Remote Reference" }
                val entry = KnowledgeSourceEntry(
                    id = "src_${UUID.randomUUID().toString().take(8)}",
                    title = title,
                    kind = KnowledgeSourceKind.GITHUB_RAW_URL,
                    originLabel = uri.toString(),
                    content = text.take(MAX_SINGLE_SOURCE_CHARS),
                    enabled = true
                )
                addSource(entry)
                Result.success(entry)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Combines all enabled [KnowledgeSourceEntry] records (plus the live [activeProjectSummary] when
     * active project context is enabled) with clear `### [Source Title]` headers and trims safely
     * within [maxChars] so every connected AI receives identical shared knowledge without overflow.
     */
    fun assembleContextBlock(
        queryHint: String,
        activeProjectSummary: String,
        maxChars: Int = DEFAULT_MAX_CONTEXT_CHARS
    ): String {
        val safeBudget = maxChars.coerceAtLeast(120)
        val enabledEntries = synchronized(lock) {
            sources.filter {
                it.enabled &&
                    it.id != DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID &&
                    !DefaultAiArtTrainingProfiles.isLayer2PluginSource(it)
            }.toList()
        }
        if (enabledEntries.isEmpty()) return ""

        val queryTokens = queryHint.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()

        // Order: Live Active Project first, then highest keyword relevance to queryHint, then most recently updated
        val prioritized = enabledEntries.sortedWith(
            compareByDescending<KnowledgeSourceEntry> { it.kind == KnowledgeSourceKind.ACTIVE_PROJECT_CONTEXT }
                .thenByDescending { entry ->
                    if (queryTokens.isEmpty()) 0
                    else {
                        val hay = "${entry.title} ${entry.content}".lowercase()
                        queryTokens.count { token -> hay.contains(token) }
                    }
                }
                .thenByDescending { it.updatedAtMillis }
        )

        val builder = StringBuilder(safeBudget.coerceAtMost(16_384))
        for (entry in prioritized) {
            val bodyText = if (entry.kind == KnowledgeSourceKind.ACTIVE_PROJECT_CONTEXT) {
                activeProjectSummary.trim().ifBlank { entry.content.trim() }
            } else {
                entry.content.trim()
            }
            if (bodyText.isBlank()) continue

            val header = "### [${entry.title}]\n"
            val separator = if (builder.isNotEmpty()) "\n\n" else ""
            val remaining = safeBudget - builder.length

            if (remaining <= separator.length + header.length + 12) {
                break
            }

            builder.append(separator)
            builder.append(header)

            val contentAllowance = safeBudget - builder.length
            if (bodyText.length <= contentAllowance) {
                builder.append(bodyText)
            } else {
                val ellipsis = "\n...[trimmed]"
                val takeLen = (contentAllowance - ellipsis.length).coerceAtLeast(0)
                if (takeLen > 0) {
                    builder.append(bodyText.substring(0, takeLen).trimEnd())
                    builder.append(ellipsis)
                }
                break
            }
        }

        val assembled = builder.toString()
        return if (assembled.length <= safeBudget) assembled else assembled.substring(0, safeBudget)
    }

    private fun ensureActiveProjectSourcePresent() = synchronized(lock) {
        if (sources.none { it.id == ACTIVE_PROJECT_SOURCE_ID || it.kind == KnowledgeSourceKind.ACTIVE_PROJECT_CONTEXT }) {
            sources.add(
                KnowledgeSourceEntry(
                    id = ACTIVE_PROJECT_SOURCE_ID,
                    title = "Active PixelForge Project Context",
                    kind = KnowledgeSourceKind.ACTIVE_PROJECT_CONTEXT,
                    originLabel = "Live Studio Mesh, Spritesheet, Palette & Hardware",
                    content = "Automatically populated from the active 3D mesh, 2D spritesheet, palette, character tag, and console hardware target.",
                    enabled = true
                )
            )
            persistToDisk()
        }
    }

    private fun getStorageFile(): File? {
        val ctx = appContext ?: return null
        val filesDir = ctx.filesDir ?: return null
        val target = File(filesDir, SOURCES_FILE_NAME)
        val canonicalBase = filesDir.canonicalPath
        val canonicalTarget = target.canonicalPath
        val prefix = if (canonicalBase.endsWith(File.separator)) canonicalBase else "$canonicalBase${File.separator}"
        return if (canonicalTarget.startsWith(prefix)) target else null
    }

    private fun loadFromDisk() {
        synchronized(lock) {
            val file = getStorageFile() ?: return@synchronized
            if (!file.exists()) return@synchronized
            try {
                val raw = file.readText(Charsets.UTF_8)
                if (raw.isBlank()) return@synchronized
                val root = JSONObject(raw)
                val arr = root.optJSONArray("sources")
                sources.clear()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val kind = KnowledgeSourceKind.entries.firstOrNull { it.name == obj.optString("kind") }
                            ?: KnowledgeSourceKind.PASTED_NOTES
                        sources.add(
                            KnowledgeSourceEntry(
                                id = obj.optString("id").ifBlank { "src_$i" },
                                title = obj.optString("title", "Reference"),
                                kind = kind,
                                originLabel = obj.optString("originLabel", ""),
                                content = obj.optString("content", ""),
                                enabled = obj.optBoolean("enabled", true),
                                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                            )
                        )
                    }
                }
                // Migrate legacy standalone layer1BaseContent field if LAYER_1_SOURCE_ID wasn't in sources yet
                val savedLegacyLayer1 = root.optString("layer1BaseContent", "").trim()
                if (savedLegacyLayer1.isNotEmpty() && sources.none { it.id == DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID }) {
                    sources.add(
                        0,
                        KnowledgeSourceEntry(
                            id = DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID,
                            title = DefaultAiArtTrainingProfiles.LAYER_1_TITLE,
                            kind = KnowledgeSourceKind.NOTEBOOK_MARKDOWN,
                            originLabel = "Built-In Layer 1 Canonical AI Base",
                            content = savedLegacyLayer1.take(MAX_SINGLE_SOURCE_CHARS),
                            enabled = root.optBoolean("layer1BaseEnabled", true)
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("MultiSourceContext", "Failed to load saved AI sources from disk: ${e.message}")
            }
        }
    }

    private fun persistToDisk() {
        val file = getStorageFile() ?: return
        try {
            val arr = JSONArray()
            sources.forEach { entry ->
                arr.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("title", entry.title)
                        put("kind", entry.kind.name)
                        put("originLabel", entry.originLabel)
                        put("content", entry.content)
                        put("enabled", entry.enabled)
                        put("updatedAtMillis", entry.updatedAtMillis)
                    }
                )
            }
            val root = JSONObject().apply {
                put("version", 3)
                put("sources", arr)
            }
            file.writeText(root.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w("MultiSourceContext", "Failed to persist AI sources to disk: ${e.message}")
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            Log.w("MultiSourceContext", "Failed to query display name: ${e.message}")
            uri.lastPathSegment
        }
    }

    private fun readCappedBytes(bytes: ByteArray, maxBytes: Int): ByteArray {
        return if (bytes.size <= maxBytes) bytes else bytes.copyOf(maxBytes)
    }

    companion object {
        const val SOURCES_FILE_NAME = "omni_ai_sources.json"
        const val ACTIVE_PROJECT_SOURCE_ID = "src_active_pixelforge_project"
        const val DEFAULT_MAX_CONTEXT_CHARS = 10_000
        private const val MAX_SINGLE_SOURCE_CHARS = 100_000
        private const val MAX_IMPORT_BYTES = 512 * 1024

        /**
         * Validates that [rawUrl] is a strictly public HTTPS URL (blocking non-HTTPS schemes,
         * embedded credentials, localhost, private RFC1918 networks, link-local cloud metadata IPs,
         * and IPv6 loopback/ULA/link-local addresses).
         */
        fun validatePublicHttpsUrl(rawUrl: String, resolveDns: Boolean = false): Result<URI> {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("URL cannot be empty."))
            }

            val uri = try {
                URI(trimmed)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("Malformed URL: ${e.message}"))
            }

            val scheme = uri.scheme?.lowercase()
            if (scheme != "https") {
                return Result.failure(IllegalArgumentException("Only HTTPS URLs are allowed for remote knowledge sources."))
            }

            if (!uri.userInfo.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("URLs containing embedded credentials are not allowed."))
            }

            val rawHost = uri.host?.trim()?.lowercase()?.removeSurrounding("[", "]")
            if (rawHost.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("URL must include a valid public host."))
            }

            if (isBlockedHostname(rawHost)) {
                return Result.failure(IllegalArgumentException("Blocked local/private host '$rawHost'."))
            }

            if (isLiteralIpAddress(rawHost)) {
                val parsedIp = try {
                    InetAddress.getByName(rawHost)
                } catch (_: Exception) {
                    return Result.failure(IllegalArgumentException("Invalid IP address '$rawHost'."))
                }
                if (isDisallowedInetAddress(parsedIp)) {
                    return Result.failure(IllegalArgumentException("Blocked private, loopback, or link-local IP address '$rawHost'."))
                }
            } else if (resolveDns) {
                val resolved = try {
                    InetAddress.getAllByName(rawHost)
                } catch (e: Exception) {
                    return Result.failure(IllegalArgumentException("Could not resolve host '$rawHost': ${e.message}"))
                }
                if (resolved.isEmpty() || resolved.any { isDisallowedInetAddress(it) }) {
                    return Result.failure(IllegalArgumentException("Blocked host '$rawHost' resolving to a private or loopback IP."))
                }
            }

            return Result.success(uri)
        }

        private fun isBlockedHostname(host: String): Boolean {
            return host == "localhost" ||
                host == "localhost.localdomain" ||
                host.endsWith(".localhost") ||
                host.endsWith(".local") ||
                host.endsWith(".internal") ||
                host == "metadata.google.internal" ||
                host == "0.0.0.0" ||
                host == "::1" ||
                host == "::"
        }

        private fun isLiteralIpAddress(host: String): Boolean {
            if (host.contains(':')) return true
            val parts = host.split('.')
            if (parts.size == 4 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) return true
            // Catch integer/hex/octal IPv4 obfuscations like 2130706433 or 0x7f000001
            if (parts.size == 1 && (host.startsWith("0x") || host.all(Char::isDigit))) return true
            return false
        }

        fun isDisallowedInetAddress(addr: InetAddress): Boolean {
            if (addr.isAnyLocalAddress ||
                addr.isLoopbackAddress ||
                addr.isLinkLocalAddress ||
                addr.isSiteLocalAddress ||
                addr.isMulticastAddress
            ) {
                return true
            }
            val bytes = addr.address
            if (addr is Inet4Address && bytes.size == 4) {
                val b0 = bytes[0].toInt() and 0xFF
                val b1 = bytes[1].toInt() and 0xFF
                // 0.0.0.0/8, 10.0.0.0/8, 127.0.0.0/8
                if (b0 == 0 || b0 == 10 || b0 == 127) return true
                // 100.64.0.0/10 (Carrier-Grade NAT)
                if (b0 == 100 && b1 in 64..127) return true
                // 169.254.0.0/16 (Link-local / Cloud Metadata 169.254.169.254)
                if (b0 == 169 && b1 == 254) return true
                // 172.16.0.0/12
                if (b0 == 172 && b1 in 16..31) return true
                // 192.168.0.0/16
                if (b0 == 192 && b1 == 168) return true
                // 198.18.0.0/15 (Benchmarking)
                if (b0 == 198 && (b1 == 18 || b1 == 19)) return true
            } else if (addr is Inet6Address && bytes.size == 16) {
                val firstByte = bytes[0].toInt() and 0xFF
                // fc00::/7 (Unique Local Addresses)
                if ((firstByte and 0xFE) == 0xFC) return true
                // fe80::/10 (Link-local)
                if (firstByte == 0xFE && ((bytes[1].toInt() and 0xC0) == 0x80)) return true
                // IPv4-mapped IPv6 ::ffff:a.b.c.d
                val isV4Mapped = (0..9).all { bytes[it].toInt() == 0 } &&
                    (bytes[10].toInt() and 0xFF) == 0xFF &&
                    (bytes[11].toInt() and 0xFF) == 0xFF
                if (isV4Mapped) {
                    val v4 = InetAddress.getByAddress(bytes.copyOfRange(12, 16))
                    return isDisallowedInetAddress(v4)
                }
            }
            return false
        }
    }
}
