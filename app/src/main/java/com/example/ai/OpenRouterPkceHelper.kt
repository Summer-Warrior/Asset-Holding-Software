package com.example.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Pure-Kotlin OAuth2 PKCE (`S256`) session bundle for OpenRouter 1-click sign-in.
 */
data class OpenRouterPkceSession(
    val codeVerifier: String,
    val codeChallenge: String,
    val loopbackPort: Int,
    val callbackUrl: String,
    val authUrl: String
)

/**
 * Outcome of an OpenRouter OAuth2 PKCE loopback sign-in flow.
 */
sealed interface OpenRouterPkceResult {
    data class Success(val accountHint: String) : OpenRouterPkceResult
    data class Failure(val reason: String) : OpenRouterPkceResult
}

/**
 * Pure-Kotlin OAuth2 PKCE (`S256`) helper for 1-click OpenRouter sign-in without a web server.
 *
 * 1. Generates a cryptographically random `codeVerifier` and URL-safe Base64 (no padding) SHA-256 `codeChallenge`.
 * 2. Binds a temporary local loopback listener on `127.0.0.1` (`ServerSocket(0, 1, 127.0.0.1)`), always closed in `finally`.
 * 3. Builds `https://openrouter.ai/auth?callback_url=http://127.0.0.1:<port>/callback&code_challenge=<challenge>&code_challenge_method=S256`.
 * 4. Exchanges the returned `code` + `codeVerifier` via `POST https://openrouter.ai/api/v1/auth/keys` and saves
 *    the user-owned OpenRouter API key straight into [SecureAiKeyVault] under [AiProviderType.OPENROUTER].
 */
object OpenRouterPkceHelper {

    const val OPENROUTER_AUTH_ENDPOINT = "https://openrouter.ai/auth"
    const val OPENROUTER_KEY_EXCHANGE_ENDPOINT = "https://openrouter.ai/api/v1/auth/keys"

    private val secureRandom = SecureRandom()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Generates a cryptographically random RFC 7636 PKCE `code_verifier` (43..128 URL-safe chars, no padding).
     */
    fun generateCodeVerifier(byteLength: Int = 48): String {
        val safeLength = byteLength.coerceIn(32, 96)
        val bytes = ByteArray(safeLength)
        secureRandom.nextBytes(bytes)
        return encodeBase64UrlNoPadding(bytes)
    }

    /**
     * Computes the `S256` PKCE `code_challenge = BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))` without padding.
     */
    fun computeS256CodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return encodeBase64UrlNoPadding(digest)
    }

    /**
     * Formats the exact 1-click OpenRouter PKCE authorization URL for a local loopback port.
     */
    fun buildAuthUrl(port: Int, codeChallenge: String): String {
        val safePort = port.coerceIn(1, 65535)
        val callbackUrl = "http://127.0.0.1:$safePort/callback"
        return "$OPENROUTER_AUTH_ENDPOINT?callback_url=$callbackUrl&code_challenge=$codeChallenge&code_challenge_method=S256"
    }

    /**
     * Extracts the OAuth `code` query parameter from an HTTP request line (e.g., `GET /callback?code=abc HTTP/1.1`).
     */
    fun extractCodeFromRequestLine(requestLine: String): String? {
        val parts = requestLine.trim().split(" ")
        if (parts.size < 2) return null
        val target = parts[1]
        if (!target.startsWith("/callback")) return null
        val query = target.substringAfter("?", missingDelimiterValue = "")
        if (query.isBlank()) return null

        query.split("&").forEach { pair ->
            val key = pair.substringBefore("=")
            val value = pair.substringAfter("=", missingDelimiterValue = "")
            if (key == "code" && value.isNotBlank()) {
                return URLDecoder.decode(value, "UTF-8")
            }
        }
        return null
    }

    /**
     * Runs the complete local loopback PKCE listener flow:
     * - Opens `ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))`
     * - Invokes [onLaunchAuthUrl] with the generated auth URL so the UI opens the browser / Custom Tab
     * - Waits up to [timeoutMillis] for the loopback `/callback?code=...` redirect
     * - Exchanges `code` + `codeVerifier` via OpenRouter's key exchange endpoint
     * - Saves the returned user-owned key directly into [vault] under [AiProviderType.OPENROUTER]
     * - Always closes the temporary [ServerSocket] in `finally`
     */
    suspend fun signInWithLoopbackPkce(
        vault: SecureAiKeyVault,
        timeoutMillis: Int = 180_000,
        client: OkHttpClient = httpClient,
        onLaunchAuthUrl: (OpenRouterPkceSession) -> Unit
    ): OpenRouterPkceResult = withContext(Dispatchers.IO) {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = computeS256CodeChallenge(codeVerifier)
        val loopbackAddr = InetAddress.getByName("127.0.0.1")

        var serverSocket: ServerSocket? = null
        try {
            serverSocket = ServerSocket(0, 1, loopbackAddr).apply {
                soTimeout = timeoutMillis.coerceIn(5_000, 300_000)
            }
            val port = serverSocket.localPort
            val callbackUrl = "http://127.0.0.1:$port/callback"
            val authUrl = buildAuthUrl(port = port, codeChallenge = codeChallenge)
            val session = OpenRouterPkceSession(
                codeVerifier = codeVerifier,
                codeChallenge = codeChallenge,
                loopbackPort = port,
                callbackUrl = callbackUrl,
                authUrl = authUrl
            )

            withContext(Dispatchers.Main) {
                onLaunchAuthUrl(session)
            }

            // Accept incoming loopback requests until we receive /callback?code=... or time out
            var authCode: String? = null
            val deadline = System.currentTimeMillis() + timeoutMillis.toLong()
            while (authCode == null && System.currentTimeMillis() < deadline) {
                val remaining = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1_000)
                serverSocket.soTimeout = remaining
                val clientSocket = serverSocket.accept()
                clientSocket.use { sock ->
                    sock.soTimeout = 5_000
                    val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                    val requestLine = reader.readLine().orEmpty()
                    val candidateCode = extractCodeFromRequestLine(requestLine)
                    val htmlBody = if (!candidateCode.isNullOrBlank()) {
                        authCode = candidateCode
                        """
                        <!DOCTYPE html>
                        <html><head><meta charset="utf-8"><title>PixelForge Connected</title></head>
                        <body style="background:#090D16;color:#E2E8F0;font-family:sans-serif;text-align:center;padding:40px;">
                        <h2 style="color:#00E5FF;">OpenRouter Connected to PixelForge</h2>
                        <p>Authorization received securely. You can close this tab and return to PixelForge.</p>
                        </body></html>
                        """.trimIndent()
                    } else {
                        """
                        <!DOCTYPE html>
                        <html><head><meta charset="utf-8"><title>PixelForge Auth</title></head>
                        <body style="background:#090D16;color:#94A3B8;font-family:sans-serif;text-align:center;padding:40px;">
                        <p>Waiting for OpenRouter authorization callback...</p>
                        </body></html>
                        """.trimIndent()
                    }
                    val bodyBytes = htmlBody.toByteArray(Charsets.UTF_8)
                    val httpResponse = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: text/html; charset=utf-8\r\n")
                        append("Content-Length: ${bodyBytes.size}\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    val out = sock.getOutputStream()
                    out.write(httpResponse.toByteArray(Charsets.US_ASCII))
                    out.write(bodyBytes)
                    out.flush()
                }
            }

            val resolvedCode = authCode
                ?: return@withContext OpenRouterPkceResult.Failure("OpenRouter sign-in timed out before callback was received.")

            val exchangedKey = exchangeCodeForApiKey(
                code = resolvedCode,
                codeVerifier = codeVerifier,
                client = client
            ) ?: return@withContext OpenRouterPkceResult.Failure("OpenRouter key exchange did not return a valid API key.")

            vault.saveCredential(
                provider = AiProviderType.OPENROUTER,
                secret = exchangedKey,
                hintPrefix = "OAuth PKCE"
            )
            OpenRouterPkceResult.Success(vault.getAccountHint(AiProviderType.OPENROUTER))
        } catch (_: SocketTimeoutException) {
            OpenRouterPkceResult.Failure("OpenRouter sign-in timed out waiting for browser callback.")
        } catch (e: Exception) {
            OpenRouterPkceResult.Failure("OpenRouter PKCE sign-in failed: ${e.message ?: "Unknown error"}")
        } finally {
            try {
                serverSocket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Exchanges the OAuth authorization [code] and [codeVerifier] with OpenRouter for a user-owned API key.
     */
    suspend fun exchangeCodeForApiKey(
        code: String,
        codeVerifier: String,
        client: OkHttpClient = httpClient
    ): String? = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("code", code)
            put("code_verifier", codeVerifier)
            put("code_challenge_method", "S256")
        }
        val request = Request.Builder()
            .url(OPENROUTER_KEY_EXCHANGE_ENDPOINT)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val bodyStr = response.body?.string().orEmpty()
            if (bodyStr.isBlank()) return@withContext null
            val json = JSONObject(bodyStr)
            json.optString("key", "").trim().takeIf { it.isNotEmpty() }
        }
    }

    private fun encodeBase64UrlNoPadding(bytes: ByteArray): String {
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }
}
