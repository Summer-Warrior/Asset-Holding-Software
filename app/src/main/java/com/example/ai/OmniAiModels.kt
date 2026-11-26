package com.example.ai

import java.util.UUID

/**
 * Supported AI backend providers in PixelForge's Multi-AI Hub.
 * Users can connect multiple providers simultaneously and assign each to a specific [AiTaskRole].
 */
enum class AiProviderType(
    val defaultDisplayName: String,
    val defaultTextModelId: String,
    val defaultImageModelId: String,
    val defaultBaseUrl: String,
    val supportsImageGeneration: Boolean,
    val requiresApiKey: Boolean
) {
    FIREBASE_STUDIO(
        defaultDisplayName = "Firebase Studio (Built-In)",
        defaultTextModelId = "gemini-3.8-flash",
        defaultImageModelId = "gemini-nano-banana-2.1",
        defaultBaseUrl = "firebase://ai-logic",
        supportsImageGeneration = true,
        requiresApiKey = false
    ),
    OPENROUTER(
        defaultDisplayName = "OpenRouter (PKCE / Key)",
        defaultTextModelId = "google/gemini-2.5-flash",
        defaultImageModelId = "google/gemini-2.5-flash-image-preview",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        supportsImageGeneration = true,
        requiresApiKey = true
    ),
    DIRECT_GEMINI(
        defaultDisplayName = "Google Gemini API (Direct)",
        defaultTextModelId = "gemini-2.5-flash",
        defaultImageModelId = "gemini-2.5-flash-image-preview",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        supportsImageGeneration = true,
        requiresApiKey = true
    ),
    ANTHROPIC_CLAUDE(
        defaultDisplayName = "Anthropic Claude",
        defaultTextModelId = "claude-sonnet-4-20250514",
        defaultImageModelId = "",
        defaultBaseUrl = "https://api.anthropic.com/v1",
        supportsImageGeneration = false,
        requiresApiKey = true
    ),
    OPENAI(
        defaultDisplayName = "OpenAI",
        defaultTextModelId = "gpt-4o-mini",
        defaultImageModelId = "gpt-image-1",
        defaultBaseUrl = "https://api.openai.com/v1",
        supportsImageGeneration = true,
        requiresApiKey = true
    ),
    LOCAL_OPENAI_COMPAT(
        defaultDisplayName = "Local Ollama / LM Studio",
        defaultTextModelId = "llama3.2-vision",
        defaultImageModelId = "sdxl-local",
        defaultBaseUrl = "http://127.0.0.1:11434/v1",
        supportsImageGeneration = true,
        requiresApiKey = false
    )
}

/**
 * Distinct studio AI responsibilities so users can mix-and-match providers
 * (e.g., Claude for specs, Gemini for vision review, OpenRouter/OpenAI for image art).
 */
enum class AiTaskRole(val label: String, val shortLabel: String) {
    TEXT_AND_SPECS("Text & 3D/2D Specs", "Text"),
    VISION_REVIEW("Spritesheet Vision Review", "Vision"),
    IMAGE_GENERATION("Image Synthesis", "Image")
}

/**
 * UI-safe representation of a configured AI provider profile.
 * NEVER stores raw API keys or OAuth tokens — secrets live exclusively in [SecureAiKeyVault].
 */
data class AiProviderProfile(
    val provider: AiProviderType,
    val displayName: String,
    val isConnected: Boolean,
    val textModelId: String,
    val imageModelId: String,
    val baseUrl: String,
    val accountHint: String = ""
)

/**
 * Supported knowledge/reference source categories shared across every connected AI provider.
 */
enum class KnowledgeSourceKind(val label: String, val badge: String) {
    LOCAL_FILE("Local File (.md/.txt/.json)", "FILE"),
    NOTEBOOK_MARKDOWN("Notebook Markdown", "NOTEBOOK"),
    GITHUB_RAW_URL("GitHub / HTTPS URL", "HTTPS"),
    PASTED_NOTES("Pasted Lore / Notes", "NOTES"),
    ACTIVE_PROJECT_CONTEXT("Live PixelForge Project", "LIVE PROJECT")
}

/**
 * A single reusable knowledge/reference source entry that can be toggled on/off and shared
 * automatically with every connected AI provider.
 */
data class KnowledgeSourceEntry(
    val id: String = "src_${UUID.randomUUID().toString().take(8)}",
    val title: String,
    val kind: KnowledgeSourceKind,
    val originLabel: String,
    val content: String,
    val enabled: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Role routing snapshot holding separate user-selected providers for Text, Vision, and Image tasks.
 */
data class OmniAiRoutingConfig(
    val activeTextProvider: AiProviderType = AiProviderType.FIREBASE_STUDIO,
    val activeVisionProvider: AiProviderType = AiProviderType.FIREBASE_STUDIO,
    val activeImageProvider: AiProviderType = AiProviderType.FIREBASE_STUDIO
) {
    fun providerFor(role: AiTaskRole): AiProviderType = when (role) {
        AiTaskRole.TEXT_AND_SPECS -> activeTextProvider
        AiTaskRole.VISION_REVIEW -> activeVisionProvider
        AiTaskRole.IMAGE_GENERATION -> activeImageProvider
    }
}
