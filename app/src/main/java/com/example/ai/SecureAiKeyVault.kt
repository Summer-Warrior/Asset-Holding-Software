package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local encrypted credential vault for per-provider AI API keys and OAuth tokens.
 *
 * - On Android devices: encrypts every secret using an [AndroidKeyStore]-backed AES-256-GCM key
 *   (`AES/GCM/NoPadding`, fresh random 12-byte IV per write, 128-bit authentication tag) and stores
 *   `Base64(iv || ciphertext)` inside private [SharedPreferences].
 * - In JVM / Robolectric unit tests where `AndroidKeyStore` is unavailable: cleanly falls back to
 *   a process-local in-memory store without throwing or writing plaintext secrets to disk.
 * - Never logs or exports stored secrets.
 */
class SecureAiKeyVault(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.applicationContext?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val inMemoryFallback = ConcurrentHashMap<AiProviderType, String>()
    private val inMemoryHints = ConcurrentHashMap<AiProviderType, String>()
    private val secureRandom = SecureRandom()

    /**
     * Encrypts and stores [secret] for [provider]. Blank values clear the credential instead.
     */
    fun saveCredential(provider: AiProviderType, secret: String, hintPrefix: String = "Key") {
        val trimmed = secret.trim()
        if (trimmed.isEmpty()) {
            clearCredential(provider)
            return
        }

        val maskedHint = formatMaskedHint(trimmed, hintPrefix)
        val encryptedPayload = encryptWithKeystoreOrNull(trimmed)
        if (encryptedPayload != null && prefs != null) {
            prefs.edit()
                .putString(prefKeyFor(provider), encryptedPayload)
                .putString(hintKeyFor(provider), maskedHint)
                .apply()
            inMemoryFallback.remove(provider)
            inMemoryHints[provider] = maskedHint
        } else {
            // Clean JVM / Robolectric fallback (never persists plaintext secrets to SharedPreferences)
            inMemoryFallback[provider] = trimmed
            inMemoryHints[provider] = maskedHint
            prefs?.edit()
                ?.remove(prefKeyFor(provider))
                ?.putString(hintKeyFor(provider), maskedHint)
                ?.apply()
        }
    }

    /**
     * Decrypts and returns the secret for [provider], or `null` if none is stored.
     */
    fun getCredential(provider: AiProviderType): String? {
        inMemoryFallback[provider]?.let { return it }
        val stored = prefs?.getString(prefKeyFor(provider), null) ?: return null
        return decryptWithKeystoreOrNull(stored)
    }

    /**
     * Returns `true` if a non-empty credential is stored for [provider].
     */
    fun hasCredential(provider: AiProviderType): Boolean {
        return !getCredential(provider).isNullOrBlank()
    }

    /**
     * Returns a non-sensitive display hint (e.g., `"Key ••••4f9a"`) safe to show in UI state.
     */
    fun getAccountHint(provider: AiProviderType): String {
        if (!hasCredential(provider)) return ""
        return inMemoryHints[provider]
            ?: prefs?.getString(hintKeyFor(provider), null)
            ?: "Connected"
    }

    /**
     * Removes any stored credential and display hint for [provider].
     */
    fun clearCredential(provider: AiProviderType) {
        inMemoryFallback.remove(provider)
        inMemoryHints.remove(provider)
        prefs?.edit()
            ?.remove(prefKeyFor(provider))
            ?.remove(hintKeyFor(provider))
            ?.apply()
    }

    /**
     * Clears all stored credentials across every provider.
     */
    fun clearAll() {
        inMemoryFallback.clear()
        inMemoryHints.clear()
        prefs?.edit()?.clear()?.apply()
    }

    private fun prefKeyFor(provider: AiProviderType): String = "enc_cred_${provider.name.lowercase()}"

    private fun hintKeyFor(provider: AiProviderType): String = "hint_cred_${provider.name.lowercase()}"

    private fun formatMaskedHint(secret: String, prefix: String): String {
        val suffix = if (secret.length >= 4) secret.takeLast(4) else "••••"
        return "$prefix ••••$suffix"
    }

    private fun getOrCreateAesKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
            keyStore.load(null)
            val existing = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    private fun encryptWithKeystoreOrNull(plaintext: String): String? {
        return try {
            val secretKey = getOrCreateAesKey() ?: return null
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            if (iv == null || iv.size != GCM_IV_LENGTH_BYTES) {
                // Fallback explicit IV if provider did not auto-generate a 12-byte IV
                val explicitIv = ByteArray(GCM_IV_LENGTH_BYTES)
                secureRandom.nextBytes(explicitIv)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, explicitIv))
                val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
                val combined = ByteArray(explicitIv.size + cipherBytes.size)
                System.arraycopy(explicitIv, 0, combined, 0, explicitIv.size)
                System.arraycopy(cipherBytes, 0, combined, explicitIv.size, cipherBytes.size)
                return Base64.encodeToString(combined, Base64.NO_WRAP)
            }
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decryptWithKeystoreOrNull(encoded: String): String? {
        return try {
            val secretKey = getOrCreateAesKey() ?: return null
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH_BYTES) return null

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "pixelforge_secure_ai_vault"
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "pixelforge_omni_ai_aes_gcm_key_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
