package com.example.ai

/**
 * Capability matrix row describing what studio asset tasks each [AiProviderType] supports.
 */
data class AiProviderCapabilityRow(
    val provider: AiProviderType,
    val displayName: String,
    val supportsTextAndJsonBuild: Boolean,
    val supportsVisionAudit: Boolean,
    val supportsImage2D: Boolean,
    val isLocalSelfHosted: Boolean,
    val routingNote: String
)

/**
 * Canonical capability checksheet for all 6 AI providers in PixelForge's Omni-AI Hub.
 */
object OmniAiProviderCapabilities {

    fun capabilityMatrix(): List<AiProviderCapabilityRow> = listOf(
        AiProviderCapabilityRow(
            provider = AiProviderType.FIREBASE_STUDIO,
            displayName = "Firebase Studio",
            supportsTextAndJsonBuild = true,
            supportsVisionAudit = true,
            supportsImage2D = true,
            isLocalSelfHosted = false,
            routingNote = "Built-in default for 2D art, 3D/Texture/Audio JSON & Vision."
        ),
        AiProviderCapabilityRow(
            provider = AiProviderType.OPENROUTER,
            displayName = "OpenRouter",
            supportsTextAndJsonBuild = true,
            supportsVisionAudit = true,
            supportsImage2D = true,
            isLocalSelfHosted = false,
            routingNote = "1-click PKCE login; routes Text/3D/Audio, Vision & 2D Image models."
        ),
        AiProviderCapabilityRow(
            provider = AiProviderType.DIRECT_GEMINI,
            displayName = "Direct Gemini",
            supportsTextAndJsonBuild = true,
            supportsVisionAudit = true,
            supportsImage2D = true,
            isLocalSelfHosted = false,
            routingNote = "Direct Gemini API key; full Text/3D/Audio, Vision & 2D Image support."
        ),
        AiProviderCapabilityRow(
            provider = AiProviderType.ANTHROPIC_CLAUDE,
            displayName = "Anthropic Claude",
            supportsTextAndJsonBuild = true,
            supportsVisionAudit = true,
            supportsImage2D = false,
            isLocalSelfHosted = false,
            routingNote = "Strong 3D/Texture/Audio JSON blueprints & Vision audits (no native 2D image output)."
        ),
        AiProviderCapabilityRow(
            provider = AiProviderType.OPENAI,
            displayName = "OpenAI",
            supportsTextAndJsonBuild = true,
            supportsVisionAudit = true,
            supportsImage2D = true,
            isLocalSelfHosted = false,
            routingNote = "GPT text/JSON builds, multimodal Vision & gpt-image-1 2D art."
        ),
        AiProviderCapabilityRow(
            provider = AiProviderType.LOCAL_OPENAI_COMPAT,
            displayName = "Local / Ollama",
            supportsTextAndJsonBuild = true,
            supportsVisionAudit = true,
            supportsImage2D = true,
            isLocalSelfHosted = true,
            routingNote = "Self-hosted OpenAI-compatible server; 2D art requires /v1/images/generations."
        )
    )
}
