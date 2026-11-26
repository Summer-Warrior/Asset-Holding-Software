package com.example.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
        ensureActiveProjectSourcePresent()
    }

    fun getSources(): List<KnowledgeSourceEntry> = synchronized(lock) {
        sources.toList()
    }

    fun addSource(entry: KnowledgeSourceEntry): List<KnowledgeSourceEntry> = synchronized(lock) {
        val cleaned = entry.copy(
            title = entry.title.trim().ifBlank { "Untitled Reference" },
            content = entry.content.trim().take(MAX_SINGLE_SOURCE_CHARS),
            updatedAtMillis = System.currentTimeMillis()
        )
        val existingIdx = sources.indexOfFirst { it.id == cleaned.id }
        if (existingIdx >= 0) {
            sources[existingIdx] = cleaned
        } else {
            sources.add(0, cleaned)
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
        if (id == ACTIVE_PROJECT_SOURCE_ID) {
            // Keep the live project slot toggleable rather than permanently deleting it
            val idx = sources.indexOfFirst { it.id == id }
            if (idx >= 0) {
                sources[idx] = sources[idx].copy(enabled = false)
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
            sources.filter { it.enabled }.toList()
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

    private fun loadFromDisk() = synchronized(lock) {
        val file = getStorageFile() ?: return
        if (!file.exists()) return
        try {
            val raw = file.readText(Charsets.UTF_8)
            if (raw.isBlank()) return
            val root = JSONObject(raw)
            val arr = root.optJSONArray("sources") ?: return
            sources.clear()
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
        } catch (_: Exception) {
            // Keep clean state if storage file was corrupted
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
                put("version", 1)
                put("sources", arr)
            }
            file.writeText(root.toString(2), Charsets.UTF_8)
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
