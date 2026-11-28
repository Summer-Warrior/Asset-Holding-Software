package com.example.ai

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Single-active rendering hardware tier for the Style-Locked True Fusion Prompt Studio.
 * Only one tier is active at a time so prompts never mix conflicting linework or shading rules.
 */
enum class PromptHardwareTier(
    val displayName: String,
    val badge: String,
    val positiveDirective: String,
    val compactDirective: String
) {
    TRUE_FUSION_HD(
        displayName = "True Fusion HD",
        badge = "HD COMIC-CEL",
        positiveDirective = "True Fusion HD 2D comic-cel art with variable-width black ink outlines, hard 2-to-3-step cel shading, clearcoat armor/visor specular glints contrasting matte skin and fabric, anime rim-light, glowing Mana Circuits, and subtle angled comic halftone dots strictly inside dark shadows.",
        compactDirective = "True Fusion HD 2D comic-cel: variable-width black ink, 2-3-step cel shading, clearcoat glints vs matte fabric, subtle halftone dots strictly in dark shadows"
    ),
    ARCADE_32BIT_60FPS(
        displayName = "32-Bit Arcade 60fps",
        badge = "32-BIT ARCADE",
        positiveDirective = "32-bit arcade 60fps 2D sprite art with punchy high-contrast color ramps, crisp hand-tuned edges, bold black ink silhouettes, hard 2-step cel shading, anime rim-light, clearcoat specular glints vs matte skin/fabric, and subtle ordered 4x4 dithering strictly inside dark shadows.",
        compactDirective = "32-bit arcade 2D sprite: punchy high-contrast ramps, crisp edges, bold black ink, hard 2-step cel, subtle ordered 4x4 dithering strictly in dark shadows"
    ),
    PIXEL_16BIT(
        displayName = "16-Bit Pixel",
        badge = "16-BIT PIXEL",
        positiveDirective = "16-bit console pixel art with a crisp 1px dark outline, strict nearest-neighbor pixel edges, clean silhouette readability, and 15-bit BGR555 flat high-contrast color ramps without anti-aliased blur.",
        compactDirective = "16-bit pixel art: 1px crisp dark outline, nearest-neighbor edges, 15-bit BGR555 flat high-contrast ramps"
    )
}

/**
 * Target asset framing and layout preset for generated sprites, sheets, portraits, or icons.
 */
enum class PromptAssetPreset(
    val displayName: String,
    val badge: String,
    val layoutDirective: String,
    val compactLayout: String
) {
    SINGLE_SPRITE_1X1(
        displayName = "Single Sprite 1x1",
        badge = "1x1 SPRITE",
        layoutDirective = "Single full-body centered game sprite (1x1 frame), heroic 3/4 action stance, unobstructed silhouette with generous margin from all canvas borders.",
        compactLayout = "single 1x1 full-body centered sprite, clean margin"
    ),
    ROW_SHEET_1X4(
        displayName = "Row Sheet 1x4",
        badge = "1x4 ROW",
        layoutDirective = "Horizontal 1x4 animation sprite sheet (4 evenly spaced side-by-side frames in one row, zero dividing lines) showing consistent proportions across idle/walk/attack keyframes.",
        compactLayout = "1x4 horizontal sprite strip (4 evenly spaced frames, no borders)"
    ),
    GRID_SHEET_2X4(
        displayName = "Grid Sheet 2x4",
        badge = "2x4 GRID",
        layoutDirective = "Organized 2x4 animation sprite sheet (2 rows of 4 evenly spaced poses, 8 frames total, zero dividing lines or boxes) preserving identical scale, anchor line, and costume colors across all frames.",
        compactLayout = "2x4 sprite sheet (8 evenly spaced frames, no dividing lines)"
    ),
    VN_PORTRAIT(
        displayName = "VN Bust Portrait",
        badge = "VN PORTRAIT",
        layoutDirective = "Expressive visual-novel upper-body bust portrait facing 3/4 toward viewer, dynamic facial expression, crisp shoulder/collar framing, and dramatic comic-cel rim lighting.",
        compactLayout = "VN upper-body bust portrait, expressive 3/4 view"
    ),
    VECTOR_UI_ICON(
        displayName = "Vector UI Icon",
        badge = "UI ICON",
        layoutDirective = "Bold readable 2D game UI emblem/icon centered in a square composition with thick outer silhouette ink, high-contrast color blocking, and instant readability at small HUD sizes.",
        compactLayout = "centered 2D game UI icon, bold silhouette, high contrast"
    )
}

/**
 * Automatic chroma-key backdrop recommendation preventing foreground color collision
 * with the transparent background stripper (`UniversalAssetImporter.stripChromaKeyBackground`).
 */
data class ChromaKeyRecommendation(
    val colorName: String,
    val hexCode: String,
    val reason: String,
    val positiveBackdropDirective: String,
    val compactBackdropDirective: String,
    val negativeExclusions: List<String>
)

/**
 * Ready-to-copy and ready-to-dispatch prompt bundle produced by [PromptStudioEngine].
 */
data class PromptStudioOutput(
    val tier: PromptHardwareTier,
    val preset: PromptAssetPreset,
    val includeHumanoidHeroicProportions: Boolean,
    val chromaKey: ChromaKeyRecommendation,
    val compact500: String,
    val natural4Sentence: String,
    val positiveSpecOnly: String,
    val negativePrompt: String,
    val fullSpec: String
)

/**
 * Style-Locked True Fusion Prompt Studio engine.
 * Guarantees:
 * - Only one active [PromptHardwareTier] per prompt (never mixes HD halftone + 32-bit dither + 16-bit pixel).
 * - Automatic `#00FF00` Lime Green vs `#FF00FF` Magenta chroma-key selection based on subject/palette colors.
 * - Optional 6.5-head heroic proportions strictly gated behind `includeHumanoidHeroicProportions`.
 * - Strictly `<= 500` chars in `compact500`.
 * - Exactly 4 clean sentences in `natural4Sentence`.
 * - Zero `"checkerboard"` token inside any positive prompt text (reserved exclusively for negative exclusions).
 */
object PromptStudioEngine {

    const val MAX_COMPACT_LENGTH: Int = 500

    private val pinkMagentaPurpleKeywords = listOf(
        "pink", "magenta", "fuchsia", "purple", "violet", "lavender",
        "lilac", "plum", "amethyst", "rose", "sakura", "berry",
        "orchid", "indigo", "neon pink", "hot pink", "psychic", "fairy"
    )

    private val baseNegativeExclusions = listOf(
        "photorealism",
        "3D render",
        "3D plastic shading",
        "soft airbrush",
        "gaussian blur",
        "fake gray-and-white checkerboard pattern",
        "visible grid lines",
        "text",
        "letters",
        "watermarks",
        "drop shadow on backdrop",
        "gradient background"
    )

    /**
     * Recommends a flat solid chroma-key backdrop that will NOT collide with the subject's colors.
     * Picks flat Lime Green (`#00FF00`) if the subject or palette includes pink/magenta/purple;
     * otherwise defaults to flat Magenta (`#FF00FF`).
     */
    fun recommendChromaKeyBackground(
        subjectDescription: String,
        paletteHexList: List<String> = emptyList()
    ): ChromaKeyRecommendation {
        val lower = subjectDescription.lowercase()
        val keywordHit = pinkMagentaPurpleKeywords.firstOrNull { kw ->
            Regex("\\b${Regex.escape(kw)}\\b").containsMatchIn(lower)
        }
        val paletteHit = paletteHexList.firstOrNull { isPinkMagentaOrPurpleHex(it) }

        val useLimeGreen = keywordHit != null || paletteHit != null
        return if (useLimeGreen) {
            val reason = when {
                keywordHit != null -> "Detected \"$keywordHit\" in subject — using flat Lime Green (#00FF00) so magenta/pink/purple details stay intact during chroma-key stripping."
                else -> "Detected magenta/purple palette swatch ($paletteHit) — using flat Lime Green (#00FF00) to avoid foreground key collision."
            }
            ChromaKeyRecommendation(
                colorName = "Flat Solid Lime Green",
                hexCode = "#00FF00",
                reason = reason,
                positiveBackdropDirective = "Isolated cleanly on a 100% uniform flat solid Lime Green (#00FF00) studio chroma-key backdrop with zero floor shadow or lighting spill.",
                compactBackdropDirective = "flat solid #00FF00 lime green BG, no floor shadow",
                negativeExclusions = baseNegativeExclusions + "green halo spill on edges"
            )
        } else {
            ChromaKeyRecommendation(
                colorName = "Flat Solid Magenta",
                hexCode = "#FF00FF",
                reason = "No pink/magenta/purple clash detected — using classic flat Magenta (#FF00FF) for crisp zero-bleed sprite alpha keying.",
                positiveBackdropDirective = "Isolated cleanly on a 100% uniform flat solid Magenta (#FF00FF) studio chroma-key backdrop with zero floor shadow or lighting spill.",
                compactBackdropDirective = "flat solid #FF00FF magenta BG, no floor shadow",
                negativeExclusions = baseNegativeExclusions + "magenta halo spill on edges"
            )
        }
    }

    /**
     * Builds all three prompt formats (`compact500`, `natural4Sentence`, `fullSpec`) from the
     * selected single-active [tier], [preset], subject description, optional palette, and optional
     * 6.5-head humanoid proportion toggle.
     */
    fun buildPrompts(
        subjectDescription: String,
        tier: PromptHardwareTier = PromptHardwareTier.ARCADE_32BIT_60FPS,
        preset: PromptAssetPreset = PromptAssetPreset.SINGLE_SPRITE_1X1,
        includeHumanoidHeroicProportions: Boolean = false,
        paletteHexList: List<String> = emptyList(),
        gameTag: String = "",
        groupTag: String = "",
        characterTag: String = "",
        targetFps: Int = 15
    ): PromptStudioOutput {
        val safeFps = targetFps.coerceIn(1, 120)
        val isAnimSheetPreset = preset == PromptAssetPreset.ROW_SHEET_1X4 ||
            preset == PromptAssetPreset.GRID_SHEET_2X4
        val fpsCadencePhrase = if (isAnimSheetPreset) {
            "$safeFps FPS animation cadence"
        } else {
            ""
        }

        val sanitizedSubject = sanitizePositiveText(
            subjectDescription.trim().ifBlank { "armored cyber-knight game hero with glowing visor and energy blade" }
        )
        val chroma = recommendChromaKeyBackground(sanitizedSubject, paletteHexList)
        val cleanPalette = paletteHexList
            .map { it.trim().uppercase() }
            .filter { Regex("^#[0-9A-F]{6}$").matches(it) }
            .distinct()
            .take(5)

        val proportionClause = if (includeHumanoidHeroicProportions) {
            "Heroic 6.5-head humanoid anime proportions with balanced silhouette weight and readable limb articulation."
        } else {
            ""
        }
        val compactProportion = if (includeHumanoidHeroicProportions) {
            "6.5-head heroic humanoid proportions, "
        } else {
            ""
        }

        val paletteClause = if (cleanPalette.isNotEmpty()) {
            "Locked color-blocking palette: ${cleanPalette.joinToString(", ")}."
        } else {
            "Locked high-contrast cartoon color-blocking with crisp separation between clearcoat armor glints and matte fabrics."
        }

        // 1. Natural 4-Sentence Format (strictly 4 crisp sentences, zero "checkerboard")
        val sentence1 = "${preset.displayName} of ${sanitizedSubject.trimEnd('.')}."
        val sentence2 = if (includeHumanoidHeroicProportions) {
            "${tier.positiveDirective.trimEnd('.')} with heroic 6.5-head humanoid proportions."
        } else {
            tier.positiveDirective
        }
        val fpsNaturalSuffix = if (fpsCadencePhrase.isNotEmpty()) " tuned for $fpsCadencePhrase" else ""
        val cleanSentence3 = "${preset.layoutDirective.trimEnd('.')}$fpsNaturalSuffix; ${paletteClause.trimEnd('.').replaceFirstChar { it.lowercase() }}."
        val sentence4 = chroma.positiveBackdropDirective
        val natural4Sentence = sanitizePositiveText(
            listOf(sentence1, sentence2, cleanSentence3, sentence4).joinToString(" ")
        )

        // 2. Compact <= 500 Char Format (guaranteed <= 500 chars even on very long descriptions)
        val paletteSuffix = if (cleanPalette.isNotEmpty()) {
            "; palette ${cleanPalette.take(4).joinToString("/")}"
        } else {
            ""
        }
        val compactFpsSuffix = if (isAnimSheetPreset) " @${safeFps}fps" else ""
        val fixedTemplateOverhead =
            "${tier.compactDirective}; ${preset.compactLayout}$compactFpsSuffix; $compactProportion${chroma.compactBackdropDirective}$paletteSuffix. No 3D, blur, airbrush, text, grids."
        val maxSubjectBudget = (MAX_COMPACT_LENGTH - fixedTemplateOverhead.length - 12).coerceAtLeast(36)
        val compactSubject = if (sanitizedSubject.length > maxSubjectBudget) {
            sanitizedSubject.take(maxSubjectBudget - 1).trimEnd(' ', ',', ';', '.') + "…"
        } else {
            sanitizedSubject
        }
        val rawCompact = sanitizePositiveText(
            "$compactSubject — ${tier.compactDirective}; ${preset.compactLayout}$compactFpsSuffix; ${compactProportion}${chroma.compactBackdropDirective}$paletteSuffix. No 3D, blur, airbrush, text, grids."
                .replace("; ,", ";")
                .replace("  ", " ")
        )
        val compact500 = if (rawCompact.length <= MAX_COMPACT_LENGTH) {
            rawCompact
        } else {
            rawCompact.take(MAX_COMPACT_LENGTH - 1).trimEnd() + "…"
        }

        // 3. Full Structured Spec + Separated Negative Prompt
        val contextMeta = listOfNotNull(
            gameTag.trim().takeIf { it.isNotEmpty() }?.let { "Game: $it" },
            groupTag.trim().takeIf { it.isNotEmpty() }?.let { "Group: $it" },
            characterTag.trim().takeIf { it.isNotEmpty() }?.let { "Character: $it" }
        ).joinToString(" | ")

        val positiveSpecOnly = sanitizePositiveText(
            buildString {
                if (contextMeta.isNotBlank()) {
                    appendLine("PROJECT CONTEXT: $contextMeta")
                }
                appendLine("SUBJECT: $sanitizedSubject")
                appendLine("ACTIVE HARDWARE TIER (${tier.displayName}): ${tier.positiveDirective}")
                appendLine("ASSET PRESET (${preset.displayName}): ${preset.layoutDirective}")
                if (fpsCadencePhrase.isNotEmpty()) {
                    appendLine("ANIMATION TIMING: $fpsCadencePhrase with smooth keyframe spacing across every frame.")
                }
                if (proportionClause.isNotBlank()) {
                    appendLine("PROPORTIONS: $proportionClause")
                }
                appendLine("PALETTE & MATERIALITY: $paletteClause")
                append("CHROMA-KEY BACKDROP (${chroma.hexCode}): ${chroma.positiveBackdropDirective}")
            }.trim()
        )

        val negativePrompt = chroma.negativeExclusions.joinToString(", ")

        val fullSpec = buildString {
            appendLine(positiveSpecOnly)
            appendLine()
            append("NEGATIVE PROMPT: $negativePrompt")
        }

        return PromptStudioOutput(
            tier = tier,
            preset = preset,
            includeHumanoidHeroicProportions = includeHumanoidHeroicProportions,
            chromaKey = chroma,
            compact500 = compact500,
            natural4Sentence = natural4Sentence,
            positiveSpecOnly = positiveSpecOnly,
            negativePrompt = negativePrompt,
            fullSpec = fullSpec
        )
    }

    /**
     * Strips any accidental `"checkerboard"` token from positive prompt text so image models
     * never hallucinate baked gray-and-white tiles in the background.
     */
    private fun sanitizePositiveText(raw: String): String {
        return raw
            .replace(Regex("checker\\s*-?\\s*board", RegexOption.IGNORE_CASE), "solid backdrop")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    internal fun isPinkMagentaOrPurpleHex(hex: String): Boolean {
        val cleaned = hex.trim()
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(cleaned)) return false
        return try {
            val color = Color.parseColor(cleaned)
            val r = Color.red(color) / 255f
            val g = Color.green(color) / 255f
            val b = Color.blue(color) / 255f
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val delta = maxC - minC
            if (delta < 0.12f || maxC < 0.22f) return false
            val saturation = delta / maxC
            if (saturation < 0.22f) return false

            val hue = when (maxC) {
                r -> 60f * (((g - b) / delta) % 6f)
                g -> 60f * (((b - r) / delta) + 2f)
                else -> 60f * (((r - g) / delta) + 4f)
            }.let { if (it < 0f) it + 360f else it }

            // Purple (~260..295), Magenta (~295..325), Pink/Rose (~325..348)
            hue in 260f..348f
        } catch (_: Exception) {
            false
        }
    }
}
