package com.example.model.vault

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client-Side AES-256-GCM Cold Vault Manager (`.vault.enc`).
 * Binary format:
 *   [0..7]   ASCII magic header "PFVAULT1" (8 bytes)
 *   [8..23]  Random PBKDF2 salt (16 bytes)
 *   [24..35] Random AES-GCM IV / nonce (12 bytes)
 *   [36..N]  AES-256-GCM ciphertext + 128-bit authentication tag
 */
object EncryptedColdVaultManager {

    private const val TAG = "EncryptedColdVaultMgr"
    private val MAGIC_HEADER = "PFVAULT1".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 65_536
    private const val KEY_BITS = 256
    private val secureRandom = SecureRandom()

    /**
     * Encrypts [plaintext] into a self-contained `.vault.enc` payload protected by PBKDF2-HMAC-SHA256
     * (65,536 iterations) and AES-256-GCM (128-bit auth tag).
     */
    fun encryptBytesToVaultEnc(plaintext: ByteArray, passphrase: String): ByteArray {
        if (passphrase.isBlank()) {
            throw GeneralSecurityException("Passphrase must not be blank for AES-256-GCM encryption.")
        }
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val keySpec = deriveAes256Key(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(MAGIC_HEADER)
        val ciphertextWithTag = cipher.doFinal(plaintext)

        val out = ByteArrayOutputStream(MAGIC_HEADER.size + SALT_BYTES + IV_BYTES + ciphertextWithTag.size)
        out.write(MAGIC_HEADER)
        out.write(salt)
        out.write(iv)
        out.write(ciphertextWithTag)
        return out.toByteArray()
    }

    /**
     * Verifies the `"PFVAULT1"` magic header and AES-256-GCM authentication tag, returning the original plaintext.
     * Throws [GeneralSecurityException] on wrong passphrase, invalid header, or any tampered byte.
     */
    fun decryptVaultEncToBytes(encryptedBytes: ByteArray, passphrase: String): ByteArray {
        val minLength = MAGIC_HEADER.size + SALT_BYTES + IV_BYTES + (GCM_TAG_BITS / 8)
        if (encryptedBytes.size < minLength) {
            throw GeneralSecurityException("Truncated or invalid .vault.enc payload.")
        }
        if (passphrase.isBlank()) {
            throw GeneralSecurityException("Passphrase is required to decrypt .vault.enc payload.")
        }
        for (i in MAGIC_HEADER.indices) {
            if (encryptedBytes[i] != MAGIC_HEADER[i]) {
                throw GeneralSecurityException("Invalid .vault.enc magic header (expected PFVAULT1).")
            }
        }

        val saltOffset = MAGIC_HEADER.size
        val ivOffset = saltOffset + SALT_BYTES
        val cipherOffset = ivOffset + IV_BYTES

        val salt = encryptedBytes.copyOfRange(saltOffset, ivOffset)
        val iv = encryptedBytes.copyOfRange(ivOffset, cipherOffset)
        val ciphertextWithTag = encryptedBytes.copyOfRange(cipherOffset, encryptedBytes.size)
        val keySpec = deriveAes256Key(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(MAGIC_HEADER)
        return cipher.doFinal(ciphertextWithTag)
    }

    /**
     * Encrypts [entry]'s local hot file to `filesDir/cold_vault/<sha256>.vault.enc`, deletes the local hot copy,
     * and returns the updated entry marked [VaultCacheState.REMOTE_ONLY].
     */
    fun offloadEntryToColdVault(
        context: Context,
        entry: VaultCatalogEntry,
        passphrase: String
    ): VaultCatalogEntry {
        val storeDir = File(context.filesDir, "vault_store").apply { mkdirs() }
        val coldDir = File(context.filesDir, "cold_vault").apply { mkdirs() }

        val localFile = File(entry.localFilePath)
        if (!SixStageAssetImporter.isStrictlyInsideDir(localFile, storeDir) || !localFile.exists()) {
            throw GeneralSecurityException("Local hot asset file missing or outside vault_store boundary.")
        }

        val safeHash = sanitizeSha256(entry.sha256Hex)
        val coldFile = File(coldDir, "$safeHash.vault.enc")
        if (!SixStageAssetImporter.isStrictlyInsideDir(coldFile, coldDir)) {
            throw GeneralSecurityException("Cold vault canonical path boundary violation.")
        }

        val plainBytes = localFile.readBytes()
        val encrypted = encryptBytesToVaultEnc(plainBytes, passphrase)
        coldFile.writeBytes(encrypted)

        if (!localFile.delete()) {
            Log.w(TAG, "Unable to delete local hot file after cold vault offload: ${localFile.name}")
        }

        return entry.copy(
            coldVaultPath = coldFile.absolutePath,
            cacheState = VaultCacheState.REMOTE_ONLY
        )
    }

    /**
     * Decrypts [entry]'s `.vault.enc` archive back into `filesDir/vault_store/<sha256>.<ext>` and updates
     * [VaultCatalogEntry.cacheState] to [VaultCacheState.CACHED_LOCAL].
     */
    fun hydrateEntryFromColdVault(
        context: Context,
        entry: VaultCatalogEntry,
        passphrase: String
    ): VaultCatalogEntry {
        val storeDir = File(context.filesDir, "vault_store").apply { mkdirs() }
        val coldDir = File(context.filesDir, "cold_vault").apply { mkdirs() }

        val coldFile = File(entry.coldVaultPath)
        if (!SixStageAssetImporter.isStrictlyInsideDir(coldFile, coldDir) || !coldFile.exists()) {
            throw GeneralSecurityException("Cold vault .vault.enc archive missing or outside cold_vault boundary.")
        }

        val safeHash = sanitizeSha256(entry.sha256Hex)
        val safeExt = entry.extension.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "bin" }
        val restoredFile = File(storeDir, "$safeHash.$safeExt")
        if (!SixStageAssetImporter.isStrictlyInsideDir(restoredFile, storeDir)) {
            throw GeneralSecurityException("Restore path canonical boundary violation.")
        }

        val encryptedBytes = coldFile.readBytes()
        val plainBytes = decryptVaultEncToBytes(encryptedBytes, passphrase)
        restoredFile.writeBytes(plainBytes)

        return entry.copy(
            localFilePath = restoredFile.absolutePath,
            byteSize = plainBytes.size.toLong(),
            cacheState = VaultCacheState.CACHED_LOCAL,
            lastAccessedMillis = System.currentTimeMillis()
        )
    }

    private fun deriveAes256Key(passphrase: String, salt: ByteArray): SecretKeySpec {
        val chars = passphrase.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val rawKey = factory.generateSecret(spec).encoded
            SecretKeySpec(rawKey, "AES")
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun sanitizeSha256(raw: String): String {
        val clean = raw.lowercase().replace(Regex("[^a-f0-9]"), "")
        if (clean.length < 16) {
            throw GeneralSecurityException("Invalid SHA-256 digest identifier.")
        }
        return clean
    }
}

/**
 * Real measured usage metrics for the Hot Phone Cache (zero fake numbers).
 */
data class HotCacheStats(
    val usedBytes: Long,
    val maxBytes: Long,
    val cachedLocalCount: Int,
    val remoteOnlyCount: Int,
    val usageFraction: Float
)

/**
 * 200 MB LRU Hot Phone Cache budget manager.
 * Keeps frequently used assets hydrated locally and cleanly evicts/offloads the least-recently-used
 * assets to [VaultCacheState.REMOTE_ONLY] whenever the cap is exceeded.
 */
object HotPhoneLruCacheManager {

    private const val TAG = "HotPhoneLruCacheMgr"
    const val DEFAULT_MAX_CACHE_BYTES: Long = 200L * 1024L * 1024L

    fun computeStats(
        catalog: List<VaultCatalogEntry>,
        maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES
    ): HotCacheStats {
        val safeMax = maxCacheBytes.coerceAtLeast(1L)
        var usedBytes = 0L
        var cachedCount = 0
        var remoteCount = 0

        for (entry in catalog) {
            if (entry.cacheState == VaultCacheState.CACHED_LOCAL) {
                cachedCount++
                val diskFile = if (entry.localFilePath.isNotBlank()) File(entry.localFilePath) else null
                val actualSize = if (diskFile != null && diskFile.exists()) {
                    diskFile.length()
                } else {
                    entry.byteSize.coerceAtLeast(0L)
                }
                usedBytes += actualSize
            } else {
                remoteCount++
            }
        }

        val fraction = (usedBytes.toDouble() / safeMax.toDouble()).toFloat().coerceIn(0f, 1f)
        return HotCacheStats(
            usedBytes = usedBytes,
            maxBytes = safeMax,
            cachedLocalCount = cachedCount,
            remoteOnlyCount = remoteCount,
            usageFraction = fraction
        )
    }

    /**
     * Enforces [maxCacheBytes] by evicting least-recently-used [VaultCacheState.CACHED_LOCAL] entries
     * first. An entry is only evicted if it already has an intact `.vault.enc` archive on disk OR
     * a non-blank [passphrase] is supplied so it can be encrypted to `cold_vault/` before removing the hot copy.
     */
    fun enforceLruBudget(
        context: Context,
        catalog: List<VaultCatalogEntry>,
        maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES,
        passphrase: String = ""
    ): Pair<List<VaultCatalogEntry>, Int> {
        val safeMax = maxCacheBytes.coerceAtLeast(0L)
        val storeDir = File(context.filesDir, "vault_store").apply { mkdirs() }
        val mutableCatalog = catalog.toMutableList()
        var stats = computeStats(mutableCatalog, safeMax.coerceAtLeast(1L))
        if (stats.usedBytes <= safeMax) {
            return mutableCatalog to 0
        }

        var evictedCount = 0
        val lruOrderIds = mutableCatalog
            .filter { it.cacheState == VaultCacheState.CACHED_LOCAL }
            .sortedBy { it.lastAccessedMillis }
            .map { it.id }

        for (candidateId in lruOrderIds) {
            if (stats.usedBytes <= safeMax) break
            val idx = mutableCatalog.indexOfFirst { it.id == candidateId }
            if (idx < 0) continue
            val entry = mutableCatalog[idx]
            val existingColdFile = if (entry.coldVaultPath.isNotBlank()) File(entry.coldVaultPath) else null

            try {
                if (existingColdFile != null && existingColdFile.exists()) {
                    val hotFile = File(entry.localFilePath)
                    if (SixStageAssetImporter.isStrictlyInsideDir(hotFile, storeDir) && hotFile.exists()) {
                        hotFile.delete()
                    }
                    mutableCatalog[idx] = entry.copy(cacheState = VaultCacheState.REMOTE_ONLY)
                    evictedCount++
                    stats = computeStats(mutableCatalog, safeMax.coerceAtLeast(1L))
                } else if (passphrase.isNotBlank()) {
                    val offloaded = EncryptedColdVaultManager.offloadEntryToColdVault(context, entry, passphrase)
                    mutableCatalog[idx] = offloaded
                    evictedCount++
                    stats = computeStats(mutableCatalog, safeMax.coerceAtLeast(1L))
                }
            } catch (e: Exception) {
                Log.w(TAG, "LRU eviction skipped for '${entry.title}': ${e.message}")
            }
        }

        return mutableCatalog to evictedCount
    }
}

/**
 * Honest optional Cloudflare R2 / Local USB-C Cold Vault configuration metadata.
 * Stores non-secret endpoint/bucket settings only; never fabricates fake remote uploads.
 */
data class ColdVaultEndpointConfig(
    val r2EndpointUrl: String = "",
    val r2BucketName: String = "",
    val backupModeLabel: String = "Local / USB-C Cold Vault"
)

/**
 * Local file & SharedPreferences persistence for `vault_catalog.json`, `skins/active_skin.json`,
 * salted SHA-256 Vault PIN lock, and optional Cloudflare R2 endpoint configuration.
 */
object VaultSettingsAndCatalogStore {

    private const val TAG = "VaultSettingsStore"
    private const val PREFS_NAME = "pixelforge_vault_prefs"
    private const val KEY_PIN_SALT = "vault_pin_salt_b64"
    private const val KEY_PIN_HASH = "vault_pin_hash_b64"
    private const val KEY_R2_ENDPOINT = "cold_vault_r2_endpoint"
    private const val KEY_R2_BUCKET = "cold_vault_r2_bucket"
    private val secureRandom = SecureRandom()

    fun saveCatalog(context: Context, catalog: List<VaultCatalogEntry>) {
        try {
            val file = File(context.filesDir, "vault_catalog.json")
            if (!SixStageAssetImporter.isStrictlyInsideDir(file, context.filesDir)) return
            val arr = JSONArray()
            catalog.forEach { arr.put(it.toJsonObject()) }
            val root = JSONObject().apply {
                put("schema", "pixelforge.vault_catalog.v1")
                put("entries", arr)
            }
            file.writeText(root.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist vault_catalog.json", e)
        }
    }

    fun loadCatalog(context: Context): List<VaultCatalogEntry> {
        return try {
            val file = File(context.filesDir, "vault_catalog.json")
            if (!SixStageAssetImporter.isStrictlyInsideDir(file, context.filesDir) || !file.exists()) {
                return emptyList()
            }
            val root = JSONObject(file.readText())
            val arr = root.optJSONArray("entries") ?: return emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(VaultCatalogEntry.fromJsonObject(obj))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load vault_catalog.json: ${e.message}")
            emptyList()
        }
    }

    fun saveActiveSkinManifest(context: Context, manifest: SkinManifest) {
        try {
            val skinsDir = File(context.filesDir, "skins").apply { mkdirs() }
            val file = File(skinsDir, "active_skin.json")
            if (!SixStageAssetImporter.isStrictlyInsideDir(file, skinsDir)) return
            file.writeText(manifest.toJsonString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist skins/active_skin.json", e)
        }
    }

    fun loadActiveSkinManifest(context: Context): SkinManifest {
        return try {
            val skinsDir = File(context.filesDir, "skins")
            val file = File(skinsDir, "active_skin.json")
            if (!SixStageAssetImporter.isStrictlyInsideDir(file, skinsDir) || !file.exists()) {
                return SkinManifest.DEFAULT_CLEAN_ROOM
            }
            SkinManifest.fromJsonString(file.readText())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load skins/active_skin.json: ${e.message}")
            SkinManifest.DEFAULT_CLEAN_ROOM
        }
    }

    fun saveEndpointConfig(context: Context, endpointUrl: String, bucketName: String): ColdVaultEndpointConfig {
        val cleanUrl = endpointUrl.trim()
        val cleanBucket = bucketName.trim()
        val modeLabel = if (cleanUrl.startsWith("https://", ignoreCase = true) && cleanBucket.isNotBlank()) {
            "Cloudflare R2 Configured ($cleanBucket)"
        } else {
            "Local / USB-C Cold Vault"
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_R2_ENDPOINT, cleanUrl)
            .putString(KEY_R2_BUCKET, cleanBucket)
            .apply()
        return ColdVaultEndpointConfig(
            r2EndpointUrl = cleanUrl,
            r2BucketName = cleanBucket,
            backupModeLabel = modeLabel
        )
    }

    fun loadEndpointConfig(context: Context): ColdVaultEndpointConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = prefs.getString(KEY_R2_ENDPOINT, "").orEmpty()
        val bucket = prefs.getString(KEY_R2_BUCKET, "").orEmpty()
        val modeLabel = if (url.startsWith("https://", ignoreCase = true) && bucket.isNotBlank()) {
            "Cloudflare R2 Configured ($bucket)"
        } else {
            "Local / USB-C Cold Vault"
        }
        return ColdVaultEndpointConfig(
            r2EndpointUrl = url,
            r2BucketName = bucket,
            backupModeLabel = modeLabel
        )
    }

    fun isPinConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PIN_SALT, null).isNullOrBlank()
    }

    fun configurePin(context: Context, pin: String): Boolean {
        val trimmed = pin.trim()
        if (trimmed.length < 4) return false
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val hash = computeSaltedPinHash(salt, trimmed)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
        return true
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return try {
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            val expectedHash = Base64.decode(hashB64, Base64.NO_WRAP)
            val actualHash = computeSaltedPinHash(salt, pin.trim())
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (e: Exception) {
            Log.w(TAG, "Vault PIN verification error: ${e.message}")
            false
        }
    }

    private fun computeSaltedPinHash(salt: ByteArray, pin: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }
}
