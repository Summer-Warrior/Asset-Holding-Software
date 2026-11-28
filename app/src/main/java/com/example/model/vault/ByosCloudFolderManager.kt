package com.example.model.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Bring-Your-Own-Storage (BYOS) personal cloud / synced folder providers supported by PixelForge
 * and the MPV Ecosystem Master Hub handoff contract.
 */
enum class ByosStorageProvider(val label: String, val badge: String) {
    LOCAL_SAF_SYNCED_FOLDER("Personal SAF Folder", "SAF SYNC"),
    GOOGLE_DRIVE_APP_SCOPE("Google Drive (Master Hub)", "DRIVE HUB"),
    CLOUDFLARE_R2_S3("Cloudflare R2", "R2 COLD")
}

/**
 * Canonical app-owned workspace directory contract inside the user's chosen personal cloud or
 * Storage Access Framework (SAF) synced folder (`MPV_Ecosystem/...`).
 */
object ByosFolderLayout {
    const val ROOT_FOLDER = "MPV_Ecosystem"
    const val VAULT_SUBFOLDER = "Vault"
    const val COLD_VAULT_SUBFOLDER = "ColdVault"
    const val SKINS_SUBFOLDER = "Skins"
    const val EXPORTS_SUBFOLDER = "Exports"

    const val VAULT_CATALOG_FILENAME = "vault_catalog.json"
    const val ACTIVE_SKIN_FILENAME = "active_skin.json"

    val ALL_SUBFOLDERS: List<String> = listOf(
        VAULT_SUBFOLDER,
        COLD_VAULT_SUBFOLDER,
        SKINS_SUBFOLDER,
        EXPORTS_SUBFOLDER
    )

    fun relativePaths(): List<String> = ALL_SUBFOLDERS.map { "$ROOT_FOLDER/$it" }
}

/**
 * Serializable BYOS personal workspace configuration & Master Hub handoff contract.
 */
data class ByosWorkspaceConfig(
    val provider: ByosStorageProvider = ByosStorageProvider.LOCAL_SAF_SYNCED_FOLDER,
    val persistedTreeUri: String = "",
    val displayFolderName: String = "",
    val masterHubAccountEmail: String = "",
    val lastSyncEpochMs: Long = 0L,
    val autoSyncOnImport: Boolean = false
) {
    val isMounted: Boolean
        get() = persistedTreeUri.isNotBlank()

    fun toJsonString(): String {
        return JSONObject().apply {
            put("schemaVersion", 1)
            put("workspaceRoot", ByosFolderLayout.ROOT_FOLDER)
            put("provider", provider.name)
            put("persistedTreeUri", persistedTreeUri)
            put("displayFolderName", displayFolderName)
            put("masterHubAccountEmail", masterHubAccountEmail)
            put("lastSyncEpochMs", lastSyncEpochMs)
            put("autoSyncOnImport", autoSyncOnImport)
            put("subfolders", JSONArray(ByosFolderLayout.ALL_SUBFOLDERS))
        }.toString(2)
    }

    companion object {
        private const val TAG = "ByosWorkspaceConfig"

        fun fromJsonString(json: String): ByosWorkspaceConfig {
            if (json.isBlank()) return ByosWorkspaceConfig()
            return try {
                val obj = JSONObject(json)
                val providerName = obj.optString("provider", ByosStorageProvider.LOCAL_SAF_SYNCED_FOLDER.name)
                val provider = ByosStorageProvider.entries.firstOrNull { it.name == providerName }
                    ?: ByosStorageProvider.LOCAL_SAF_SYNCED_FOLDER
                ByosWorkspaceConfig(
                    provider = provider,
                    persistedTreeUri = obj.optString("persistedTreeUri", ""),
                    displayFolderName = obj.optString("displayFolderName", ""),
                    masterHubAccountEmail = obj.optString("masterHubAccountEmail", ""),
                    lastSyncEpochMs = obj.optLong("lastSyncEpochMs", 0L),
                    autoSyncOnImport = obj.optBoolean("autoSyncOnImport", false)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing ByosWorkspaceConfig JSON, returning defaults: ${e.message}")
                ByosWorkspaceConfig()
            }
        }
    }
}

/**
 * Honest telemetry summary returned by real SAF or local directory BYOS workspace operations.
 */
data class ByosSyncReport(
    val success: Boolean,
    val provider: ByosStorageProvider,
    val foldersVerified: List<String>,
    val filesWritten: Int,
    val filesRead: Int,
    val summary: String,
    val syncedSkinManifest: SkinManifest? = null,
    val catalogEntryCountFound: Int = 0,
    val coldVaultFilesFound: Int = 0
)

/**
 * Manages Bring-Your-Own-Storage (BYOS) personal cloud / synced folder workspaces using ONLY
 * built-in Android SDK APIs (`DocumentsContract`, `ContentResolver`, `File`, `SharedPreferences`,
 * and `org.json`). Zero broad storage permissions and zero fake cloud upload simulations.
 */
object ByosCloudFolderManager {

    private const val TAG = "ByosCloudFolderManager"
    private const val PREFS_NAME = "pixelforge_byos_workspace_prefs"
    private const val KEY_CONFIG_JSON = "byos_workspace_config_json"

    fun saveConfig(context: Context, config: ByosWorkspaceConfig) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CONFIG_JSON, config.toJsonString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving BYOS workspace config", e)
        }
    }

    fun loadConfig(context: Context): ByosWorkspaceConfig {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_CONFIG_JSON, "") ?: ""
            ByosWorkspaceConfig.fromJsonString(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed loading BYOS workspace config: ${e.message}")
            ByosWorkspaceConfig()
        }
    }

    /**
     * Persists SAF tree URI permissions and creates/verifies the `MPV_Ecosystem` app-owned
     * subfolder layout (`Vault`, `ColdVault`, `Skins`, `Exports`) inside the selected folder.
     */
    fun mountSafTreeUri(
        context: Context,
        treeUri: Uri,
        existingConfig: ByosWorkspaceConfig = loadConfig(context)
    ): Pair<ByosWorkspaceConfig, ByosSyncReport> {
        val resolver = context.contentResolver
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            resolver.takePersistableUriPermission(treeUri, flags)
        } catch (e: Exception) {
            Log.w(TAG, "takePersistableUriPermission warning for $treeUri: ${e.message}")
        }

        val displayName = resolveTreeDisplayName(treeUri)
        val verifiedFolders = mutableListOf<String>()

        return try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val ecosystemUri = findOrCreateSubdirectory(resolver, treeUri, rootDocUri, ByosFolderLayout.ROOT_FOLDER)
                ?: throw IllegalStateException("Could not create or access ${ByosFolderLayout.ROOT_FOLDER} inside selected SAF folder.")

            ByosFolderLayout.ALL_SUBFOLDERS.forEach { sub ->
                val subUri = findOrCreateSubdirectory(resolver, treeUri, ecosystemUri, sub)
                if (subUri != null) {
                    verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/$sub")
                }
            }

            val updatedConfig = existingConfig.copy(
                persistedTreeUri = treeUri.toString(),
                displayFolderName = displayName
            )
            saveConfig(context, updatedConfig)

            val allVerified = verifiedFolders.size == ByosFolderLayout.ALL_SUBFOLDERS.size
            val report = ByosSyncReport(
                success = allVerified,
                provider = updatedConfig.provider,
                foldersVerified = verifiedFolders,
                filesWritten = 0,
                filesRead = 0,
                summary = if (allVerified) {
                    "Mounted '$displayName' and verified all ${verifiedFolders.size} workspace folders (${ByosFolderLayout.ROOT_FOLDER}/Vault, ColdVault, Skins, Exports)."
                } else {
                    "Mounted '$displayName', verified ${verifiedFolders.size}/${ByosFolderLayout.ALL_SUBFOLDERS.size} folders. Check folder write permissions."
                }
            )
            updatedConfig to report
        } catch (e: Exception) {
            Log.e(TAG, "Failed mounting SAF tree URI $treeUri", e)
            val fallbackConfig = existingConfig.copy(
                persistedTreeUri = treeUri.toString(),
                displayFolderName = displayName
            )
            saveConfig(context, fallbackConfig)
            val report = ByosSyncReport(
                success = false,
                provider = fallbackConfig.provider,
                foldersVerified = verifiedFolders,
                filesWritten = 0,
                filesRead = 0,
                summary = "Mounted URI '$displayName' but failed creating workspace folders: ${e.localizedMessage ?: "Permission denied"}"
            )
            fallbackConfig to report
        }
    }

    /**
     * Pushes local `vault_catalog.json` into `MPV_Ecosystem/Vault/`, `active_skin.json` into
     * `MPV_Ecosystem/Skins/`, and copies existing local `.vault.enc` encrypted cold files into
     * `MPV_Ecosystem/ColdVault/`.
     */
    fun syncLocalVaultToSafTree(
        context: Context,
        config: ByosWorkspaceConfig,
        catalog: List<VaultCatalogEntry>,
        activeSkin: SkinManifest
    ): ByosSyncReport {
        if (!config.isMounted) {
            return ByosSyncReport(
                success = false,
                provider = config.provider,
                foldersVerified = emptyList(),
                filesWritten = 0,
                filesRead = 0,
                summary = "No personal SAF folder is mounted. Tap 'Mount Personal Folder' first."
            )
        }

        val resolver = context.contentResolver
        val treeUri = Uri.parse(config.persistedTreeUri)
        val verifiedFolders = mutableListOf<String>()
        var filesWritten = 0

        return try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val ecosystemUri = findOrCreateSubdirectory(resolver, treeUri, rootDocUri, ByosFolderLayout.ROOT_FOLDER)
                ?: throw IllegalStateException("Unable to access ${ByosFolderLayout.ROOT_FOLDER} in mounted folder.")

            val vaultDirUri = findOrCreateSubdirectory(resolver, treeUri, ecosystemUri, ByosFolderLayout.VAULT_SUBFOLDER)
                ?: throw IllegalStateException("Unable to access ${ByosFolderLayout.VAULT_SUBFOLDER} folder.")
            verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.VAULT_SUBFOLDER}")

            val coldVaultDirUri = findOrCreateSubdirectory(resolver, treeUri, ecosystemUri, ByosFolderLayout.COLD_VAULT_SUBFOLDER)
                ?: throw IllegalStateException("Unable to access ${ByosFolderLayout.COLD_VAULT_SUBFOLDER} folder.")
            verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.COLD_VAULT_SUBFOLDER}")

            val skinsDirUri = findOrCreateSubdirectory(resolver, treeUri, ecosystemUri, ByosFolderLayout.SKINS_SUBFOLDER)
                ?: throw IllegalStateException("Unable to access ${ByosFolderLayout.SKINS_SUBFOLDER} folder.")
            verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.SKINS_SUBFOLDER}")

            if (findOrCreateSubdirectory(resolver, treeUri, ecosystemUri, ByosFolderLayout.EXPORTS_SUBFOLDER) != null) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.EXPORTS_SUBFOLDER}")
            }

            // 1. Write vault_catalog.json -> Vault/
            val catalogJson = serializeCatalogJson(catalog)
            writeTextDocument(
                resolver = resolver,
                treeUri = treeUri,
                parentDirUri = vaultDirUri,
                fileName = ByosFolderLayout.VAULT_CATALOG_FILENAME,
                mimeType = "application/json",
                text = catalogJson
            )
            filesWritten++

            // 2. Write active_skin.json -> Skins/
            writeTextDocument(
                resolver = resolver,
                treeUri = treeUri,
                parentDirUri = skinsDirUri,
                fileName = ByosFolderLayout.ACTIVE_SKIN_FILENAME,
                mimeType = "application/json",
                text = activeSkin.toJsonString()
            )
            filesWritten++

            // 3. Copy existing local .vault.enc files -> ColdVault/
            var coldCopied = 0
            val copiedNames = HashSet<String>()
            catalog.forEach { entry ->
                val coldPath = entry.coldVaultPath.trim()
                if (coldPath.isNotEmpty()) {
                    val coldFile = File(coldPath)
                    if (coldFile.exists() && coldFile.isFile && coldFile.name.endsWith(".vault.enc") && copiedNames.add(coldFile.name)) {
                        writeBinaryFileToDocument(
                            resolver = resolver,
                            treeUri = treeUri,
                            parentDirUri = coldVaultDirUri,
                            fileName = coldFile.name,
                            mimeType = "application/octet-stream",
                            sourceFile = coldFile
                        )
                        filesWritten++
                        coldCopied++
                    }
                }
            }

            val updatedConfig = config.copy(lastSyncEpochMs = System.currentTimeMillis())
            saveConfig(context, updatedConfig)

            ByosSyncReport(
                success = true,
                provider = config.provider,
                foldersVerified = verifiedFolders,
                filesWritten = filesWritten,
                filesRead = 0,
                summary = "Pushed $filesWritten file(s) to '${config.displayFolderName.ifBlank { "Mounted Folder" }}' (vault_catalog.json with ${catalog.size} entries, active_skin.json, and $coldCopied .vault.enc file(s)).",
                syncedSkinManifest = activeSkin,
                catalogEntryCountFound = catalog.size,
                coldVaultFilesFound = coldCopied
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing local vault to SAF tree ${config.persistedTreeUri}", e)
            ByosSyncReport(
                success = false,
                provider = config.provider,
                foldersVerified = verifiedFolders,
                filesWritten = filesWritten,
                filesRead = 0,
                summary = "Sync to personal folder failed: ${e.localizedMessage ?: "SAF write error"}"
            )
        }
    }

    /**
     * Reads `Skins/active_skin.json` (if present) and scans `Vault/` and `ColdVault/` inside
     * the mounted SAF folder, returning an honest [ByosSyncReport].
     */
    fun readVaultAndSkinFromSafTree(
        context: Context,
        config: ByosWorkspaceConfig
    ): ByosSyncReport {
        if (!config.isMounted) {
            return ByosSyncReport(
                success = false,
                provider = config.provider,
                foldersVerified = emptyList(),
                filesWritten = 0,
                filesRead = 0,
                summary = "No personal SAF folder is mounted. Mount a folder first to read synced Vault & Skins."
            )
        }

        val resolver = context.contentResolver
        val treeUri = Uri.parse(config.persistedTreeUri)
        val verifiedFolders = mutableListOf<String>()
        var filesRead = 0
        var parsedSkin: SkinManifest? = null
        var catalogCount = 0
        var coldCount = 0

        return try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val ecosystemUri = findChildDocument(resolver, treeUri, rootDocUri, ByosFolderLayout.ROOT_FOLDER)?.first
                ?: return ByosSyncReport(
                    success = false,
                    provider = config.provider,
                    foldersVerified = emptyList(),
                    filesWritten = 0,
                    filesRead = 0,
                    summary = "Folder '${ByosFolderLayout.ROOT_FOLDER}' not found inside mounted workspace. Push Vault & Skins first."
                )

            val vaultUri = findChildDocument(resolver, treeUri, ecosystemUri, ByosFolderLayout.VAULT_SUBFOLDER)?.first
            if (vaultUri != null) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.VAULT_SUBFOLDER}")
                val catalogDoc = findChildDocument(resolver, treeUri, vaultUri, ByosFolderLayout.VAULT_CATALOG_FILENAME)?.first
                if (catalogDoc != null) {
                    val text = readTextDocument(resolver, catalogDoc)
                    if (!text.isNullOrBlank()) {
                        catalogCount = parseCatalogEntryCount(text)
                        filesRead++
                    }
                }
            }

            val coldUri = findChildDocument(resolver, treeUri, ecosystemUri, ByosFolderLayout.COLD_VAULT_SUBFOLDER)?.first
            if (coldUri != null) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.COLD_VAULT_SUBFOLDER}")
                val coldFiles = listChildDisplayNames(resolver, treeUri, coldUri)
                coldCount = coldFiles.count { it.endsWith(".vault.enc", ignoreCase = true) }
                filesRead += coldCount
            }

            val skinsUri = findChildDocument(resolver, treeUri, ecosystemUri, ByosFolderLayout.SKINS_SUBFOLDER)?.first
            if (skinsUri != null) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.SKINS_SUBFOLDER}")
                val skinDoc = findChildDocument(resolver, treeUri, skinsUri, ByosFolderLayout.ACTIVE_SKIN_FILENAME)?.first
                if (skinDoc != null) {
                    val skinJson = readTextDocument(resolver, skinDoc)
                    if (!skinJson.isNullOrBlank()) {
                        parsedSkin = SkinManifest.fromJsonString(skinJson)
                        filesRead++
                    }
                }
            }

            val exportsUri = findChildDocument(resolver, treeUri, ecosystemUri, ByosFolderLayout.EXPORTS_SUBFOLDER)?.first
            if (exportsUri != null) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.EXPORTS_SUBFOLDER}")
            }

            ByosSyncReport(
                success = true,
                provider = config.provider,
                foldersVerified = verifiedFolders,
                filesWritten = 0,
                filesRead = filesRead,
                summary = buildString {
                    append("Read $filesRead item(s) from '${config.displayFolderName.ifBlank { "Mounted Folder" }}': ")
                    append("$catalogCount catalog entr${if (catalogCount == 1) "y" else "ies"}, ")
                    append("$coldCount .vault.enc archive(s)")
                    if (parsedSkin != null) {
                        append(", active skin '${parsedSkin.displayName}' (${parsedSkin.slotBindings.size} slot(s))")
                    }
                    append(".")
                },
                syncedSkinManifest = parsedSkin,
                catalogEntryCountFound = catalogCount,
                coldVaultFilesFound = coldCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading BYOS workspace from SAF tree ${config.persistedTreeUri}", e)
            ByosSyncReport(
                success = false,
                provider = config.provider,
                foldersVerified = verifiedFolders,
                filesWritten = 0,
                filesRead = filesRead,
                summary = "Failed reading personal folder: ${e.localizedMessage ?: "SAF read error"}"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Pure File Directory Mirror Helpers (Canonical Path Guarded & Unit-Testable)
    // -------------------------------------------------------------------------

    fun ensureWorkspaceFoldersInDirectory(rootDir: File): List<String> {
        val canonicalRoot = rootDir.canonicalFile
        if (!canonicalRoot.exists()) {
            canonicalRoot.mkdirs()
        }
        val ecosystemDir = resolveSafeChild(canonicalRoot, ByosFolderLayout.ROOT_FOLDER)
        if (!ecosystemDir.exists()) {
            ecosystemDir.mkdirs()
        }
        val verified = mutableListOf<String>()
        ByosFolderLayout.ALL_SUBFOLDERS.forEach { sub ->
            val subDir = resolveSafeChild(ecosystemDir, sub)
            if (subDir.exists() || subDir.mkdirs()) {
                verified.add("${ByosFolderLayout.ROOT_FOLDER}/$sub")
            }
        }
        return verified
    }

    fun syncVaultToDirectory(
        rootDir: File,
        catalog: List<VaultCatalogEntry>,
        activeSkin: SkinManifest,
        provider: ByosStorageProvider = ByosStorageProvider.LOCAL_SAF_SYNCED_FOLDER
    ): ByosSyncReport {
        val verifiedFolders = ensureWorkspaceFoldersInDirectory(rootDir)
        val ecosystemDir = resolveSafeChild(rootDir.canonicalFile, ByosFolderLayout.ROOT_FOLDER)
        val vaultDir = resolveSafeChild(ecosystemDir, ByosFolderLayout.VAULT_SUBFOLDER)
        val coldVaultDir = resolveSafeChild(ecosystemDir, ByosFolderLayout.COLD_VAULT_SUBFOLDER)
        val skinsDir = resolveSafeChild(ecosystemDir, ByosFolderLayout.SKINS_SUBFOLDER)

        var filesWritten = 0
        var coldCopied = 0

        return try {
            val catalogFile = resolveSafeChild(vaultDir, ByosFolderLayout.VAULT_CATALOG_FILENAME)
            catalogFile.writeText(serializeCatalogJson(catalog))
            filesWritten++

            val skinFile = resolveSafeChild(skinsDir, ByosFolderLayout.ACTIVE_SKIN_FILENAME)
            skinFile.writeText(activeSkin.toJsonString())
            filesWritten++

            val copiedNames = HashSet<String>()
            catalog.forEach { entry ->
                val coldPath = entry.coldVaultPath.trim()
                if (coldPath.isNotEmpty()) {
                    val srcFile = File(coldPath)
                    if (srcFile.exists() && srcFile.isFile && srcFile.name.endsWith(".vault.enc") && copiedNames.add(srcFile.name)) {
                        val destFile = resolveSafeChild(coldVaultDir, srcFile.name)
                        FileInputStream(srcFile).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        filesWritten++
                        coldCopied++
                    }
                }
            }

            ByosSyncReport(
                success = true,
                provider = provider,
                foldersVerified = verifiedFolders,
                filesWritten = filesWritten,
                filesRead = 0,
                summary = "Synced $filesWritten file(s) to ${ecosystemDir.name} (${catalog.size} catalog entries, active skin '${activeSkin.displayName}', $coldCopied .vault.enc file(s)).",
                syncedSkinManifest = activeSkin,
                catalogEntryCountFound = catalog.size,
                coldVaultFilesFound = coldCopied
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing vault to local directory ${rootDir.path}", e)
            ByosSyncReport(
                success = false,
                provider = provider,
                foldersVerified = verifiedFolders,
                filesWritten = filesWritten,
                filesRead = 0,
                summary = "Directory sync failed: ${e.localizedMessage ?: "I/O error"}"
            )
        }
    }

    fun readWorkspaceFromDirectory(
        rootDir: File,
        provider: ByosStorageProvider = ByosStorageProvider.LOCAL_SAF_SYNCED_FOLDER
    ): ByosSyncReport {
        val canonicalRoot = rootDir.canonicalFile
        val ecosystemDir = resolveSafeChild(canonicalRoot, ByosFolderLayout.ROOT_FOLDER)
        if (!ecosystemDir.exists() || !ecosystemDir.isDirectory) {
            return ByosSyncReport(
                success = false,
                provider = provider,
                foldersVerified = emptyList(),
                filesWritten = 0,
                filesRead = 0,
                summary = "${ByosFolderLayout.ROOT_FOLDER} directory does not exist in ${canonicalRoot.name}."
            )
        }

        val verifiedFolders = mutableListOf<String>()
        var filesRead = 0
        var catalogCount = 0
        var coldCount = 0
        var parsedSkin: SkinManifest? = null

        return try {
            val vaultDir = resolveSafeChild(ecosystemDir, ByosFolderLayout.VAULT_SUBFOLDER)
            if (vaultDir.isDirectory) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.VAULT_SUBFOLDER}")
                val catalogFile = resolveSafeChild(vaultDir, ByosFolderLayout.VAULT_CATALOG_FILENAME)
                if (catalogFile.isFile) {
                    catalogCount = parseCatalogEntryCount(catalogFile.readText())
                    filesRead++
                }
            }

            val coldVaultDir = resolveSafeChild(ecosystemDir, ByosFolderLayout.COLD_VAULT_SUBFOLDER)
            if (coldVaultDir.isDirectory) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.COLD_VAULT_SUBFOLDER}")
                coldCount = coldVaultDir.listFiles()?.count { it.isFile && it.name.endsWith(".vault.enc", ignoreCase = true) } ?: 0
                filesRead += coldCount
            }

            val skinsDir = resolveSafeChild(ecosystemDir, ByosFolderLayout.SKINS_SUBFOLDER)
            if (skinsDir.isDirectory) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.SKINS_SUBFOLDER}")
                val skinFile = resolveSafeChild(skinsDir, ByosFolderLayout.ACTIVE_SKIN_FILENAME)
                if (skinFile.isFile) {
                    parsedSkin = SkinManifest.fromJsonString(skinFile.readText())
                    filesRead++
                }
            }

            val exportsDir = resolveSafeChild(ecosystemDir, ByosFolderLayout.EXPORTS_SUBFOLDER)
            if (exportsDir.isDirectory) {
                verifiedFolders.add("${ByosFolderLayout.ROOT_FOLDER}/${ByosFolderLayout.EXPORTS_SUBFOLDER}")
            }

            ByosSyncReport(
                success = true,
                provider = provider,
                foldersVerified = verifiedFolders,
                filesWritten = 0,
                filesRead = filesRead,
                summary = "Read $filesRead item(s) from ${ByosFolderLayout.ROOT_FOLDER}: $catalogCount catalog entries, $coldCount .vault.enc file(s), skin=${parsedSkin?.displayName ?: "none"}.",
                syncedSkinManifest = parsedSkin,
                catalogEntryCountFound = catalogCount,
                coldVaultFilesFound = coldCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading BYOS workspace directory ${rootDir.path}", e)
            ByosSyncReport(
                success = false,
                provider = provider,
                foldersVerified = verifiedFolders,
                filesWritten = 0,
                filesRead = filesRead,
                summary = "Directory read failed: ${e.localizedMessage ?: "I/O error"}"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Internal SAF & Serialization Helpers
    // -------------------------------------------------------------------------

    private fun resolveSafeChild(parentDir: File, childName: String): File {
        val cleanName = childName.replace("/", "_").replace("\\", "_").trim()
        val canonicalParent = parentDir.canonicalFile
        val candidate = File(canonicalParent, cleanName).canonicalFile
        require(
            candidate.path == canonicalParent.path ||
                candidate.path.startsWith(canonicalParent.path + File.separator)
        ) {
            "Unsafe path outside BYOS workspace root: $childName"
        }
        return candidate
    }

    private fun serializeCatalogJson(catalog: List<VaultCatalogEntry>): String {
        val arr = JSONArray()
        catalog.forEach { entry -> arr.put(entry.toJsonObject()) }
        return JSONObject().apply {
            put("schemaVersion", 1)
            put("entryCount", catalog.size)
            put("entries", arr)
        }.toString(2)
    }

    private fun parseCatalogEntryCount(rawJson: String): Int {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed).length()
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("entries")?.length() ?: obj.optInt("entryCount", 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing vault_catalog.json count: ${e.message}")
            0
        }
    }

    private fun resolveTreeDisplayName(treeUri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val tail = docId.substringAfterLast(':').substringAfterLast('/').ifBlank { docId }
            tail.ifBlank { treeUri.lastPathSegment ?: "Personal Folder" }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback display name for SAF tree URI: ${e.message}")
            treeUri.lastPathSegment ?: "Personal Folder"
        }
    }

    private fun findChildDocument(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        parentDocUri: Uri,
        targetName: String
    ): Pair<Uri, String>? {
        val parentDocId = DocumentsContract.getDocumentId(parentDocUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: ""
                if (name.equals(targetName, ignoreCase = false)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    return docUri to mime
                }
            }
        }
        return null
    }

    private fun listChildDisplayNames(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        parentDocUri: Uri
    ): List<String> {
        val names = mutableListOf<String>()
        val parentDocId = DocumentsContract.getDocumentId(parentDocUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) names.add(name)
            }
        }
        return names
    }

    private fun findOrCreateSubdirectory(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        parentDocUri: Uri,
        folderName: String
    ): Uri? {
        val existing = findChildDocument(resolver, treeUri, parentDocUri, folderName)
        if (existing != null && existing.second == DocumentsContract.Document.MIME_TYPE_DIR) {
            return existing.first
        }
        return DocumentsContract.createDocument(
            resolver,
            parentDocUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            folderName
        )
    }

    private fun writeTextDocument(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        parentDirUri: Uri,
        fileName: String,
        mimeType: String,
        text: String
    ) {
        val existing = findChildDocument(resolver, treeUri, parentDirUri, fileName)?.first
        val targetUri = existing ?: DocumentsContract.createDocument(resolver, parentDirUri, mimeType, fileName)
            ?: throw IllegalStateException("Could not create SAF document '$fileName'.")
        resolver.openOutputStream(targetUri, "wt")?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.flush()
        } ?: throw IllegalStateException("Could not open output stream for SAF document '$fileName'.")
    }

    private fun writeBinaryFileToDocument(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        parentDirUri: Uri,
        fileName: String,
        mimeType: String,
        sourceFile: File
    ) {
        val existing = findChildDocument(resolver, treeUri, parentDirUri, fileName)?.first
        val targetUri = existing ?: DocumentsContract.createDocument(resolver, parentDirUri, mimeType, fileName)
            ?: throw IllegalStateException("Could not create SAF binary document '$fileName'.")
        FileInputStream(sourceFile).use { input ->
            resolver.openOutputStream(targetUri, "w")?.use { output ->
                input.copyTo(output)
                output.flush()
            } ?: throw IllegalStateException("Could not open binary output stream for SAF document '$fileName'.")
        }
    }

    private fun readTextDocument(
        resolver: android.content.ContentResolver,
        docUri: Uri
    ): String? {
        return resolver.openInputStream(docUri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    }
}
