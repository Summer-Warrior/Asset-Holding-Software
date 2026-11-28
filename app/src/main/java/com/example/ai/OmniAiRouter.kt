package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class OmniTextResponse(
    val text: String,
    val providerUsed: AiProviderType,
    val modelIdUsed: String,
    val usedFallbackFromFirebase: Boolean = false
)

data class OmniImageResponse(
    val bitmap: Bitmap,
    val providerUsed: AiProviderType,
    val modelIdUsed: String,
    val usedFallbackFromFirebase: Boolean = false
)

/**
 * Unified Multi-AI Router dispatching multimodal text/vision requests (`generateTextOrJson`)
 * AND image generation requests (`generateImageBitmap`) across all 6 [AiProviderType] backends:
 *
 * - `FIREBASE_STUDIO`: delegates cleanly to existing `Firebase.ai` (`gemini-3.8-flash` / `gemini-nano-banana-2.1`)
 * - `OPENROUTER`: `https://openrouter.ai/api/v1/chat/completions` (Bearer key, OpenAI multimodal + `modalities: ["image", "text"]`)
 * - `DIRECT_GEMINI`: `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
 * - `ANTHROPIC_CLAUDE`: `https://api.anthropic.com/v1/messages` (`x-api-key`, `anthropic-version: 2023-06-01`, native base64 image blocks)
 * - `OPENAI`: `https://api.openai.com/v1/chat/completions` & `https://api.openai.com/v1/images/generations` (`b64_json`)
 * - `LOCAL_OPENAI_COMPAT`: user-configured local URL (Ollama, LM Studio, or local image bridge)
 *
 * Keeps separate user-selected active providers for Text, Vision Review, and Image Generation,
 * defaulting to `FIREBASE_STUDIO` and automatically falling back to any connected non-Firebase
 * provider if `FIREBASE_STUDIO` fails on a private sideloaded install.
 */
object OmniAiRouter {

    private const val PREFS_NAME = "pixelforge_omni_ai_router_config"
    private const val KEY_ACTIVE_TEXT = "active_text_provider"
    private const val KEY_ACTIVE_VISION = "active_vision_provider"
    private const val KEY_ACTIVE_IMAGE = "active_image_provider"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var configPrefs: SharedPreferences? = null

    @Volatile
    private var vault: SecureAiKeyVault = SecureAiKeyVault(null)

    @Volatile
    private var contextRegistry: MultiSourceContextRegistry = MultiSourceContextRegistry(null)

    @Volatile
    private var routingConfig: OmniAiRoutingConfig = OmniAiRoutingConfig()

    private val customTextModels = ConcurrentHashMap<AiProviderType, String>()
    private val customImageModels = ConcurrentHashMap<AiProviderType, String>()
    private val customBaseUrls = ConcurrentHashMap<AiProviderType, String>()
    private val localProviderEnabled = ConcurrentHashMap<AiProviderType, Boolean>()

    /**
     * Attaches Android [Context] to initialize the encrypted [SecureAiKeyVault],
     * [MultiSourceContextRegistry], and saved provider routing settings.
     */
    @Synchronized
    fun attachContext(context: Context) {
        val appCtx = context.applicationContext
        vault = SecureAiKeyVault(appCtx)
        contextRegistry = MultiSourceContextRegistry(appCtx)
        configPrefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadPersistedConfig()
    }

    fun getVault(): SecureAiKeyVault = vault

    fun getContextRegistry(): MultiSourceContextRegistry = contextRegistry

    fun getRoutingConfig(): OmniAiRoutingConfig = routingConfig

    fun setActiveProviderForRole(role: AiTaskRole, provider: AiProviderType): OmniAiRoutingConfig {
        routingConfig = when (role) {
            AiTaskRole.TEXT_AND_SPECS -> routingConfig.copy(activeTextProvider = provider)
            AiTaskRole.VISION_REVIEW -> routingConfig.copy(activeVisionProvider = provider)
            AiTaskRole.IMAGE_GENERATION -> routingConfig.copy(activeImageProvider = provider)
        }
        persistRoutingConfig()
        return routingConfig
    }

    fun updateProviderSettings(
        provider: AiProviderType,
        textModelId: String? = null,
        imageModelId: String? = null,
        baseUrl: String? = null,
        markLocalConnected: Boolean? = null
    ) {
        val editor = configPrefs?.edit()
        textModelId?.trim()?.takeIf { it.isNotEmpty() }?.let {
            customTextModels[provider] = it
            editor?.putString("text_model_${provider.name}", it)
        }
        imageModelId?.trim()?.let {
            customImageModels[provider] = it
            editor?.putString("image_model_${provider.name}", it)
        }
        baseUrl?.trim()?.takeIf { it.isNotEmpty() }?.let {
            customBaseUrls[provider] = it
            editor?.putString("base_url_${provider.name}", it)
        }
        if (markLocalConnected != null && provider == AiProviderType.LOCAL_OPENAI_COMPAT) {
            localProviderEnabled[provider] = markLocalConnected
            editor?.putBoolean("local_connected_${provider.name}", markLocalConnected)
        }
        editor?.apply()
    }

    /**
     * Explicitly clears the configured text model ID for [provider] to `""` (never pre-filling
     * a fake or guessed default model ID) and persists the cleared state.
     */
    fun clearProviderModelId(provider: AiProviderType) {
        customTextModels[provider] = ""
        configPrefs?.edit()?.putString("text_model_${provider.name}", "")?.apply()
    }

    fun saveProviderCredential(provider: AiProviderType, secret: String, hintPrefix: String = "API Key") {
        vault.saveCredential(provider, secret, hintPrefix)
        if (provider == AiProviderType.LOCAL_OPENAI_COMPAT && secret.isNotBlank()) {
            updateProviderSettings(provider, markLocalConnected = true)
        }
    }

    fun clearProviderCredential(provider: AiProviderType) {
        vault.clearCredential(provider)
        if (provider == AiProviderType.LOCAL_OPENAI_COMPAT) {
            updateProviderSettings(provider, markLocalConnected = false)
        }
    }

    fun getProviderProfile(provider: AiProviderType): AiProviderProfile {
        val textModel = customTextModels[provider] ?: provider.defaultTextModelId
        val imageModel = customImageModels[provider] ?: provider.defaultImageModelId
        val baseUrl = customBaseUrls[provider] ?: provider.defaultBaseUrl
        val isConnected = when (provider) {
            AiProviderType.FIREBASE_STUDIO -> true
            AiProviderType.LOCAL_OPENAI_COMPAT ->
                localProviderEnabled[provider] == true || vault.hasCredential(provider)
            else -> vault.hasCredential(provider)
        }
        val hint = when {
            provider == AiProviderType.FIREBASE_STUDIO -> "Built-In AI Studio Runtime"
            provider == AiProviderType.LOCAL_OPENAI_COMPAT && isConnected ->
                if (vault.hasCredential(provider)) vault.getAccountHint(provider) else "Local Endpoint Ready"
            isConnected -> vault.getAccountHint(provider)
            else -> ""
        }
        return AiProviderProfile(
            provider = provider,
            displayName = provider.defaultDisplayName,
            isConnected = isConnected,
            textModelId = textModel,
            imageModelId = imageModel,
            baseUrl = baseUrl,
            accountHint = hint
        )
    }

    fun getAllProviderProfiles(): List<AiProviderProfile> {
        return AiProviderType.entries.map { getProviderProfile(it) }
    }

    /**
     * Returns the first connected non-Firebase provider suitable for [role], or `null` if none are connected.
     */
    fun findFallbackProviderForRole(role: AiTaskRole): AiProviderType? {
        val candidates = listOf(
            AiProviderType.OPENROUTER,
            AiProviderType.DIRECT_GEMINI,
            AiProviderType.OPENAI,
            AiProviderType.ANTHROPIC_CLAUDE,
            AiProviderType.LOCAL_OPENAI_COMPAT
        )
        return candidates.firstOrNull { candidate ->
            val profile = getProviderProfile(candidate)
            profile.isConnected && (role != AiTaskRole.IMAGE_GENERATION || candidate.supportsImageGeneration)
        }
    }

    /**
     * Dispatches a multimodal text/vision/JSON request using the active provider for [role],
     * injecting any assembled [sharedContextBlock], and falling back gracefully to a connected
     * external provider if `FIREBASE_STUDIO` fails on a private sideloaded install.
     */
    suspend fun generateTextOrJson(
        role: AiTaskRole,
        prompt: String,
        systemInstruction: String = "",
        imageBitmap: Bitmap? = null,
        expectJson: Boolean = false,
        sharedContextBlock: String = "",
        providerOverride: AiProviderType? = null,
        firebaseDelegate: suspend (effectivePrompt: String, bitmap: Bitmap?) -> String
    ): OmniTextResponse = withContext(Dispatchers.IO) {
        val targetProvider = providerOverride ?: routingConfig.providerFor(role)
        val combinedPrompt = mergePromptWithSharedContext(prompt, sharedContextBlock)

        if (targetProvider == AiProviderType.FIREBASE_STUDIO) {
            try {
                val text = firebaseDelegate(combinedPrompt, imageBitmap)
                return@withContext OmniTextResponse(
                    text = text,
                    providerUsed = AiProviderType.FIREBASE_STUDIO,
                    modelIdUsed = getProviderProfile(AiProviderType.FIREBASE_STUDIO).textModelId,
                    usedFallbackFromFirebase = false
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (firebaseError: Exception) {
                val fallback = findFallbackProviderForRole(role)
                if (fallback != null) {
                    val fallbackText = dispatchExternalTextProvider(
                        provider = fallback,
                        prompt = combinedPrompt,
                        systemInstruction = systemInstruction,
                        imageBitmap = imageBitmap,
                        expectJson = expectJson
                    )
                    return@withContext OmniTextResponse(
                        text = fallbackText,
                        providerUsed = fallback,
                        modelIdUsed = getProviderProfile(fallback).textModelId,
                        usedFallbackFromFirebase = true
                    )
                }
                throw firebaseError
            }
        }

        val text = dispatchExternalTextProvider(
            provider = targetProvider,
            prompt = combinedPrompt,
            systemInstruction = systemInstruction,
            imageBitmap = imageBitmap,
            expectJson = expectJson
        )
        OmniTextResponse(
            text = text,
            providerUsed = targetProvider,
            modelIdUsed = getProviderProfile(targetProvider).textModelId,
            usedFallbackFromFirebase = false
        )
    }

    /**
     * Dispatches an image generation / image-to-image synthesis request using the active
     * [AiTaskRole.IMAGE_GENERATION] provider, falling back to any connected image-capable provider
     * if `FIREBASE_STUDIO` fails on a private sideloaded install.
     */
    suspend fun generateImageBitmap(
        prompt: String,
        referenceBitmap: Bitmap? = null,
        sharedContextBlock: String = "",
        providerOverride: AiProviderType? = null,
        firebaseImageDelegate: suspend (effectivePrompt: String, referenceBitmap: Bitmap?) -> Bitmap
    ): OmniImageResponse = withContext(Dispatchers.IO) {
        val targetProvider = providerOverride ?: routingConfig.activeImageProvider
        val combinedPrompt = mergePromptWithSharedContext(prompt, sharedContextBlock)

        if (targetProvider == AiProviderType.FIREBASE_STUDIO) {
            try {
                val bmp = firebaseImageDelegate(combinedPrompt, referenceBitmap)
                return@withContext OmniImageResponse(
                    bitmap = bmp,
                    providerUsed = AiProviderType.FIREBASE_STUDIO,
                    modelIdUsed = getProviderProfile(AiProviderType.FIREBASE_STUDIO).imageModelId,
                    usedFallbackFromFirebase = false
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (firebaseError: Exception) {
                val fallback = findFallbackProviderForRole(AiTaskRole.IMAGE_GENERATION)
                if (fallback != null) {
                    val bmp = dispatchExternalImageProvider(
                        provider = fallback,
                        prompt = combinedPrompt,
                        referenceBitmap = referenceBitmap
                    )
                    return@withContext OmniImageResponse(
                        bitmap = bmp,
                        providerUsed = fallback,
                        modelIdUsed = getProviderProfile(fallback).imageModelId,
                        usedFallbackFromFirebase = true
                    )
                }
                throw firebaseError
            }
        }

        val bmp = dispatchExternalImageProvider(
            provider = targetProvider,
            prompt = combinedPrompt,
            referenceBitmap = referenceBitmap
        )
        OmniImageResponse(
            bitmap = bmp,
            providerUsed = targetProvider,
            modelIdUsed = getProviderProfile(targetProvider).imageModelId,
            usedFallbackFromFirebase = false
        )
    }

    // -------------------------------------------------------------------------
    // Pure JSON Payload Builders (deterministically unit-testable without live network)
    // -------------------------------------------------------------------------

    fun buildOpenRouterPayload(
        modelId: String,
        prompt: String,
        systemInstruction: String = "",
        imageBase64Jpeg: String? = null,
        expectJson: Boolean = false,
        requestImageOutput: Boolean = false
    ): JSONObject {
        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }
        val userContent = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })
            if (!imageBase64Jpeg.isNullOrBlank()) {
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$imageBase64Jpeg")
                    })
                })
            }
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userContent)
        })

        return JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            if (requestImageOutput) {
                put("modalities", JSONArray().apply {
                    put("image")
                    put("text")
                })
            } else if (expectJson) {
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }
        }
    }

    fun buildDirectGeminiPayload(
        prompt: String,
        systemInstruction: String = "",
        imageBase64Jpeg: String? = null,
        expectJson: Boolean = false,
        requestImageOutput: Boolean = false
    ): JSONObject {
        val parts = JSONArray().apply {
            if (!imageBase64Jpeg.isNullOrBlank()) {
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", imageBase64Jpeg)
                    })
                })
            }
            put(JSONObject().apply {
                put("text", prompt)
            })
        }
        val contents = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", parts)
            })
        }
        val genConfig = JSONObject().apply {
            if (requestImageOutput) {
                put("responseModalities", JSONArray().apply {
                    put("IMAGE")
                    put("TEXT")
                })
            } else if (expectJson) {
                put("responseMimeType", "application/json")
            }
        }

        return JSONObject().apply {
            if (systemInstruction.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
            }
            put("contents", contents)
            if (genConfig.length() > 0) {
                put("generationConfig", genConfig)
            }
        }
    }

    fun buildAnthropicClaudePayload(
        modelId: String,
        prompt: String,
        systemInstruction: String = "",
        imageBase64Jpeg: String? = null,
        maxTokens: Int = 2048
    ): JSONObject {
        val contentBlocks = JSONArray().apply {
            if (!imageBase64Jpeg.isNullOrBlank()) {
                put(JSONObject().apply {
                    put("type", "image")
                    put("source", JSONObject().apply {
                        put("type", "base64")
                        put("media_type", "image/jpeg")
                        put("data", imageBase64Jpeg)
                    })
                })
            }
            put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })
        }
        return JSONObject().apply {
            put("model", modelId)
            put("max_tokens", maxTokens)
            if (systemInstruction.isNotBlank()) {
                put("system", systemInstruction)
            }
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentBlocks)
                })
            })
        }
    }

    fun buildOpenAiTextPayload(
        modelId: String,
        prompt: String,
        systemInstruction: String = "",
        imageBase64Jpeg: String? = null,
        expectJson: Boolean = false
    ): JSONObject {
        return buildOpenRouterPayload(
            modelId = modelId,
            prompt = prompt,
            systemInstruction = systemInstruction,
            imageBase64Jpeg = imageBase64Jpeg,
            expectJson = expectJson,
            requestImageOutput = false
        )
    }

    fun buildOpenAiImagePayload(
        modelId: String,
        prompt: String,
        size: String = "1024x1024"
    ): JSONObject {
        return JSONObject().apply {
            put("model", modelId)
            put("prompt", prompt)
            put("n", 1)
            put("size", size)
            put("response_format", "b64_json")
        }
    }

    fun buildLocalOpenAiCompatPayload(
        modelId: String,
        prompt: String,
        systemInstruction: String = "",
        imageBase64Jpeg: String? = null,
        expectJson: Boolean = false
    ): JSONObject {
        return buildOpenRouterPayload(
            modelId = modelId,
            prompt = prompt,
            systemInstruction = systemInstruction,
            imageBase64Jpeg = imageBase64Jpeg,
            expectJson = expectJson,
            requestImageOutput = false
        )
    }

    // -------------------------------------------------------------------------
    // HTTP Dispatch Implementation
    // -------------------------------------------------------------------------

    private fun dispatchExternalTextProvider(
        provider: AiProviderType,
        prompt: String,
        systemInstruction: String,
        imageBitmap: Bitmap?,
        expectJson: Boolean
    ): String {
        val profile = getProviderProfile(provider)
        val secret = vault.getCredential(provider)
        if (provider.requiresApiKey && secret.isNullOrBlank()) {
            throw IllegalStateException("No API key saved for ${profile.displayName}. Open AI Hub to connect.")
        }

        val imageB64 = imageBitmap?.let { encodeBitmapToJpegBase64(it) }
        val jsonHintPrompt = if (expectJson) {
            "$prompt\n\nIMPORTANT: Return ONLY valid JSON matching the requested schema without markdown fences."
        } else {
            prompt
        }

        val responseBody = when (provider) {
            AiProviderType.OPENROUTER -> {
                val payload = buildOpenRouterPayload(
                    modelId = profile.textModelId,
                    prompt = jsonHintPrompt,
                    systemInstruction = systemInstruction,
                    imageBase64Jpeg = imageB64,
                    expectJson = expectJson,
                    requestImageOutput = false
                )
                val url = "${profile.baseUrl.trimEnd('/')}/chat/completions"
                executeJsonPost(
                    url = url,
                    payload = payload,
                    headers = mapOf(
                        "Authorization" to "Bearer $secret",
                        "HTTP-Referer" to "https://github.com/Summer-Warrior/Asset-Holding-Software",
                        "X-Title" to "PixelForge"
                    )
                )
            }
            AiProviderType.DIRECT_GEMINI -> {
                val payload = buildDirectGeminiPayload(
                    prompt = jsonHintPrompt,
                    systemInstruction = systemInstruction,
                    imageBase64Jpeg = imageB64,
                    expectJson = expectJson,
                    requestImageOutput = false
                )
                val url = "${profile.baseUrl.trimEnd('/')}/models/${profile.textModelId}:generateContent"
                executeJsonPost(
                    url = url,
                    payload = payload,
                    headers = mapOf("x-goog-api-key" to secret.orEmpty())
                )
            }
            AiProviderType.ANTHROPIC_CLAUDE -> {
                val payload = buildAnthropicClaudePayload(
                    modelId = profile.textModelId,
                    prompt = jsonHintPrompt,
                    systemInstruction = systemInstruction,
                    imageBase64Jpeg = imageB64
                )
                val url = "${profile.baseUrl.trimEnd('/')}/messages"
                executeJsonPost(
                    url = url,
                    payload = payload,
                    headers = mapOf(
                        "x-api-key" to secret.orEmpty(),
                        "anthropic-version" to "2023-06-01"
                    )
                )
            }
            AiProviderType.OPENAI -> {
                val payload = buildOpenAiTextPayload(
                    modelId = profile.textModelId,
                    prompt = jsonHintPrompt,
                    systemInstruction = systemInstruction,
                    imageBase64Jpeg = imageB64,
                    expectJson = expectJson
                )
                val url = "${profile.baseUrl.trimEnd('/')}/chat/completions"
                executeJsonPost(
                    url = url,
                    payload = payload,
                    headers = mapOf("Authorization" to "Bearer $secret")
                )
            }
            AiProviderType.LOCAL_OPENAI_COMPAT -> {
                val payload = buildLocalOpenAiCompatPayload(
                    modelId = profile.textModelId,
                    prompt = jsonHintPrompt,
                    systemInstruction = systemInstruction,
                    imageBase64Jpeg = imageB64,
                    expectJson = expectJson
                )
                val normalizedBase = normalizeLocalBaseUrl(profile.baseUrl)
                val url = "$normalizedBase/chat/completions"
                val hdrs = if (!secret.isNullOrBlank()) mapOf("Authorization" to "Bearer $secret") else emptyMap()
                executeJsonPost(url = url, payload = payload, headers = hdrs)
            }
            AiProviderType.FIREBASE_STUDIO -> {
                throw IllegalStateException("Firebase Studio must be handled via firebaseDelegate.")
            }
        }

        val extracted = extractTextFromProviderResponse(provider, responseBody)
        return if (expectJson) stripJsonMarkdownFences(extracted) else extracted
    }

    private fun dispatchExternalImageProvider(
        provider: AiProviderType,
        prompt: String,
        referenceBitmap: Bitmap?
    ): Bitmap {
        val profile = getProviderProfile(provider)
        val secret = vault.getCredential(provider)
        if (provider == AiProviderType.ANTHROPIC_CLAUDE) {
            throw IllegalStateException(
                "Anthropic Claude does not support direct raster image generation. Switch your Image AI role in AI Hub to Firebase Studio, Direct Gemini, OpenRouter, OpenAI, or Local OpenAI-Compat."
            )
        }
        if (provider.requiresApiKey && secret.isNullOrBlank()) {
            throw IllegalStateException("No API key saved for ${profile.displayName}. Open AI Hub to connect.")
        }

        val refB64 = referenceBitmap?.let { encodeBitmapToJpegBase64(it) }
        val responseBody = when (provider) {
            AiProviderType.OPENROUTER -> {
                val payload = buildOpenRouterPayload(
                    modelId = profile.imageModelId.ifBlank { AiProviderType.OPENROUTER.defaultImageModelId },
                    prompt = prompt,
                    imageBase64Jpeg = refB64,
                    requestImageOutput = true
                )
                executeJsonPost(
                    url = "${profile.baseUrl.trimEnd('/')}/chat/completions",
                    payload = payload,
                    headers = mapOf(
                        "Authorization" to "Bearer $secret",
                        "HTTP-Referer" to "https://github.com/Summer-Warrior/Asset-Holding-Software",
                        "X-Title" to "PixelForge"
                    )
                )
            }
            AiProviderType.DIRECT_GEMINI -> {
                val payload = buildDirectGeminiPayload(
                    prompt = prompt,
                    imageBase64Jpeg = refB64,
                    requestImageOutput = true
                )
                val model = profile.imageModelId.ifBlank { AiProviderType.DIRECT_GEMINI.defaultImageModelId }
                executeJsonPost(
                    url = "${profile.baseUrl.trimEnd('/')}/models/$model:generateContent",
                    payload = payload,
                    headers = mapOf("x-goog-api-key" to secret.orEmpty())
                )
            }
            AiProviderType.OPENAI -> {
                val payload = buildOpenAiImagePayload(
                    modelId = profile.imageModelId.ifBlank { AiProviderType.OPENAI.defaultImageModelId },
                    prompt = prompt
                )
                executeJsonPost(
                    url = "${profile.baseUrl.trimEnd('/')}/images/generations",
                    payload = payload,
                    headers = mapOf("Authorization" to "Bearer $secret")
                )
            }
            AiProviderType.LOCAL_OPENAI_COMPAT -> {
                val payload = buildOpenAiImagePayload(
                    modelId = profile.imageModelId.ifBlank { AiProviderType.LOCAL_OPENAI_COMPAT.defaultImageModelId },
                    prompt = prompt
                )
                val normalizedBase = normalizeLocalBaseUrl(profile.baseUrl)
                val hdrs = if (!secret.isNullOrBlank()) mapOf("Authorization" to "Bearer $secret") else emptyMap()
                executeJsonPost(
                    url = "$normalizedBase/images/generations",
                    payload = payload,
                    headers = hdrs
                )
            }
            AiProviderType.FIREBASE_STUDIO -> {
                throw IllegalStateException("Firebase Studio must be handled via firebaseImageDelegate.")
            }
            AiProviderType.ANTHROPIC_CLAUDE -> error("Handled above")
        }

        return extractBitmapFromProviderResponse(provider, responseBody)
            ?: throw IllegalStateException("${profile.displayName} did not return an image payload.")
    }

    private fun executeJsonPost(
        url: String,
        payload: JSONObject,
        headers: Map<String, String>
    ): String {
        val reqBuilder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Accept", "application/json")
        headers.forEach { (k, v) ->
            if (v.isNotBlank()) reqBuilder.header(k, v)
        }
        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val snippet = bodyStr.take(280).ifBlank { response.message }
                throw IllegalStateException("AI Provider HTTP ${response.code}: $snippet")
            }
            return bodyStr
        }
    }

    internal fun extractTextFromProviderResponse(provider: AiProviderType, responseJson: String): String {
        val root = JSONObject(responseJson)
        return when (provider) {
            AiProviderType.DIRECT_GEMINI -> {
                val candidates = root.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                buildString {
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val t = parts.optJSONObject(i)?.optString("text", "").orEmpty()
                            if (t.isNotEmpty()) append(t)
                        }
                    }
                }.trim()
            }
            AiProviderType.ANTHROPIC_CLAUDE -> {
                val contentArr = root.optJSONArray("content")
                buildString {
                    if (contentArr != null) {
                        for (i in 0 until contentArr.length()) {
                            val block = contentArr.optJSONObject(i)
                            if (block?.optString("type") == "text") {
                                append(block.optString("text", ""))
                            }
                        }
                    }
                }.trim()
            }
            AiProviderType.OPENROUTER,
            AiProviderType.OPENAI,
            AiProviderType.LOCAL_OPENAI_COMPAT,
            AiProviderType.FIREBASE_STUDIO -> {
                val message = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                val contentVal = message?.opt("content")
                when (contentVal) {
                    is String -> contentVal.trim()
                    is JSONArray -> buildString {
                        for (i in 0 until contentVal.length()) {
                            val item = contentVal.optJSONObject(i)
                            if (item?.optString("type") == "text") {
                                append(item.optString("text", ""))
                            }
                        }
                    }.trim()
                    else -> ""
                }
            }
        }
    }

    internal fun extractBitmapFromProviderResponse(provider: AiProviderType, responseJson: String): Bitmap? {
        val root = JSONObject(responseJson)
        val b64Data: String? = when (provider) {
            AiProviderType.DIRECT_GEMINI -> {
                val parts = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                var found: String? = null
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val inlineData = parts.optJSONObject(i)?.optJSONObject("inlineData")
                        val data = inlineData?.optString("data", "").orEmpty()
                        if (data.isNotBlank()) {
                            found = data
                            break
                        }
                    }
                }
                found
            }
            AiProviderType.OPENROUTER -> {
                val message = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                val imagesArr = message?.optJSONArray("images")
                val imgUrl = imagesArr?.optJSONObject(0)?.optJSONObject("image_url")?.optString("url", "")
                    ?: imagesArr?.optJSONObject(0)?.optString("url", "")
                extractBase64FromDataUri(imgUrl)
                    ?: extractBase64FromDataUri(message?.optString("content", ""))
            }
            AiProviderType.OPENAI,
            AiProviderType.LOCAL_OPENAI_COMPAT -> {
                val dataArr = root.optJSONArray("data")
                val b64 = dataArr?.optJSONObject(0)?.optString("b64_json", "")
                if (!b64.isNullOrBlank()) b64 else null
            }
            else -> null
        }

        if (b64Data.isNullOrBlank()) return null
        val decoded = Base64.decode(b64Data, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    }

    private fun extractBase64FromDataUri(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val marker = "base64,"
        val idx = raw.indexOf(marker)
        if (idx < 0) return null
        return raw.substring(idx + marker.length).takeWhile { !it.isWhitespace() && it != ')' && it != '"' }
    }

    private fun stripJsonMarkdownFences(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("```")) return trimmed
        val withoutStart = trimmed.removePrefix("```json").removePrefix("```").trim()
        return withoutStart.removeSuffix("```").trim()
    }

    private fun mergePromptWithSharedContext(prompt: String, sharedContextBlock: String): String {
        val cleanContext = sharedContextBlock.trim()
        if (cleanContext.isEmpty()) return prompt
        return buildString {
            appendLine("=== SHARED MULTI-SOURCE KNOWLEDGE & PROJECT CONTEXT ===")
            appendLine(cleanContext)
            appendLine("=== END SHARED CONTEXT ===")
            appendLine()
            append(prompt)
        }
    }

    private fun normalizeLocalBaseUrl(rawBase: String): String {
        val trimmed = rawBase.trim().trimEnd('/')
        if (trimmed.isEmpty()) return AiProviderType.LOCAL_OPENAI_COMPAT.defaultBaseUrl
        return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
    }

    private fun encodeBitmapToJpegBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun loadPersistedConfig() {
        val prefs = configPrefs ?: return
        val textProv = prefs.getString(KEY_ACTIVE_TEXT, null)?.let { name ->
            AiProviderType.entries.firstOrNull { it.name == name }
        } ?: AiProviderType.FIREBASE_STUDIO
        val visionProv = prefs.getString(KEY_ACTIVE_VISION, null)?.let { name ->
            AiProviderType.entries.firstOrNull { it.name == name }
        } ?: AiProviderType.FIREBASE_STUDIO
        val imageProv = prefs.getString(KEY_ACTIVE_IMAGE, null)?.let { name ->
            AiProviderType.entries.firstOrNull { it.name == name }
        } ?: AiProviderType.FIREBASE_STUDIO

        routingConfig = OmniAiRoutingConfig(
            activeTextProvider = textProv,
            activeVisionProvider = visionProv,
            activeImageProvider = imageProv
        )

        AiProviderType.entries.forEach { p ->
            prefs.getString("text_model_${p.name}", null)?.let { customTextModels[p] = it }
            prefs.getString("image_model_${p.name}", null)?.let { customImageModels[p] = it }
            prefs.getString("base_url_${p.name}", null)?.let { customBaseUrls[p] = it }
            if (p == AiProviderType.LOCAL_OPENAI_COMPAT) {
                localProviderEnabled[p] = prefs.getBoolean("local_connected_${p.name}", false)
            }
        }
    }

    private fun persistRoutingConfig() {
        configPrefs?.edit()
            ?.putString(KEY_ACTIVE_TEXT, routingConfig.activeTextProvider.name)
            ?.putString(KEY_ACTIVE_VISION, routingConfig.activeVisionProvider.name)
            ?.putString(KEY_ACTIVE_IMAGE, routingConfig.activeImageProvider.name)
            ?.apply()
    }
}
