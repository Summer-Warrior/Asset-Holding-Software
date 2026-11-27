package com.example.model.masterpack

import android.graphics.Bitmap
import android.graphics.Color
import org.json.JSONObject

/**
 * Curated color entry on PixelForge's 20-Color True Fusion Wheel.
 */
data class ColorWheelEntry(
    val id: String,
    val name: String,
    val hex: String,
    val colorInt: Int
)

/**
 * Exactly 20 curated, high-contrast True Fusion colors covering neutrals/metals,
 * warm reds/golds, greens/teals, cyans/blues, violets/magentas, and skin/earth tones.
 */
val TWENTY_COLOR_WHEEL: List<ColorWheelEntry> = listOf(
    // Neutrals & Metals (4)
    ColorWheelEntry("obsidian_ink", "Obsidian Ink", "#161B26", Color.rgb(0x16, 0x1B, 0x26)),
    ColorWheelEntry("gunmetal_steel", "Gunmetal Steel", "#5C697C", Color.rgb(0x5C, 0x69, 0x7C)),
    ColorWheelEntry("titanium_silver", "Titanium Silver", "#D5DEE8", Color.rgb(0xD5, 0xDE, 0xE8)),
    ColorWheelEntry("ceramic_white", "Ceramic White", "#F8FAFC", Color.rgb(0xF8, 0xFA, 0xFC)),
    // Warm Reds & Golds (4)
    ColorWheelEntry("crimson_flare", "Crimson Flare", "#E11D48", Color.rgb(0xE1, 0x1D, 0x48)),
    ColorWheelEntry("tokusatsu_blaze", "Tokusatsu Blaze", "#F97316", Color.rgb(0xF9, 0x73, 0x16)),
    ColorWheelEntry("amber_reactor", "Amber Reactor", "#F59E0B", Color.rgb(0xF5, 0x9E, 0x0B)),
    ColorWheelEntry("solar_gold", "Solar Gold", "#FACC15", Color.rgb(0xFA, 0xCC, 0x15)),
    // Greens & Teals (4)
    ColorWheelEntry("acid_volt", "Acid Volt", "#84CC16", Color.rgb(0x84, 0xCC, 0x16)),
    ColorWheelEntry("emerald_pulse", "Emerald Pulse", "#10B981", Color.rgb(0x10, 0xB9, 0x81)),
    ColorWheelEntry("forest_armor", "Forest Armor", "#15803D", Color.rgb(0x15, 0x80, 0x3D)),
    ColorWheelEntry("jade_circuit", "Jade Circuit", "#14B8A6", Color.rgb(0x14, 0xB8, 0xA6)),
    // Cyans & Blues (4)
    ColorWheelEntry("mana_cyan", "Mana Cyan", "#00E5FF", Color.rgb(0x00, 0xE5, 0xFF)),
    ColorWheelEntry("glacier_ice", "Glacier Ice", "#38BDF8", Color.rgb(0x38, 0xBD, 0xF8)),
    ColorWheelEntry("cobalt_vanguard", "Cobalt Vanguard", "#2563EB", Color.rgb(0x25, 0x63, 0xEB)),
    ColorWheelEntry("astral_navy", "Astral Navy", "#1E3A8A", Color.rgb(0x1E, 0x3A, 0x8A)),
    // Violets & Magentas (2)
    ColorWheelEntry("ultraviolet_core", "Ultraviolet Core", "#9333EA", Color.rgb(0x93, 0x33, 0xEA)),
    ColorWheelEntry("neon_magenta", "Neon Magenta", "#FF2A85", Color.rgb(0xFF, 0x2A, 0x85)),
    // Skin & Earth Tones (2)
    ColorWheelEntry("warm_ochre_skin", "Warm Ochre", "#E0A370", Color.rgb(0xE0, 0xA3, 0x70)),
    ColorWheelEntry("terracotta_bronze", "Terracotta Bronze", "#9A5536", Color.rgb(0x9A, 0x55, 0x36))
)

/**
 * Rig anchor points supporting foot-aligned animation and Infinite Fusion Head x Body / form-shift sockets.
 */
enum class AnchorPointType(val label: String) {
    FEET_PIVOT("Feet Pivot"),
    HEAD("Head"),
    BODY("Body Core"),
    WING("Wing Socket"),
    HORN("Horn / Crest"),
    TAIL("Tail Socket"),
    VISOR("Visor / Eye")
}

/**
 * Normalized anchor position on a sprite cell, clamped cleanly to `0f..1f`.
 */
data class NormalizedAnchor(
    val type: AnchorPointType,
    val xNorm: Float,
    val yNorm: Float
) {
    fun clamped(): NormalizedAnchor = copy(
        xNorm = xNorm.coerceIn(0f, 1f),
        yNorm = yNorm.coerceIn(0f, 1f)
    )

    companion object {
        fun of(type: AnchorPointType, xNorm: Float, yNorm: Float): NormalizedAnchor =
            NormalizedAnchor(type, xNorm.coerceIn(0f, 1f), yNorm.coerceIn(0f, 1f))
    }
}

/**
 * Normalized bounding box for combat hurtbox / hitbox definitions (`0f..1f`).
 */
data class NormalizedRectBox(
    val xNorm: Float,
    val yNorm: Float,
    val wNorm: Float,
    val hNorm: Float
) {
    fun clamped(): NormalizedRectBox {
        val x = xNorm.coerceIn(0f, 1f)
        val y = yNorm.coerceIn(0f, 1f)
        val w = wNorm.coerceIn(0.02f, (1f - x).coerceAtLeast(0.02f))
        val h = hNorm.coerceIn(0.02f, (1f - y).coerceAtLeast(0.02f))
        return NormalizedRectBox(x, y, w, h)
    }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        val c = clamped()
        put("xNorm", c.xNorm.toDouble())
        put("yNorm", c.yNorm.toDouble())
        put("wNorm", c.wNorm.toDouble())
        put("hNorm", c.hNorm.toDouble())
    }

    companion object {
        fun fromJsonObject(obj: JSONObject?, fallback: NormalizedRectBox): NormalizedRectBox {
            if (obj == null) return fallback.clamped()
            return NormalizedRectBox(
                xNorm = obj.optDouble("xNorm", fallback.xNorm.toDouble()).toFloat(),
                yNorm = obj.optDouble("yNorm", fallback.yNorm.toDouble()).toFloat(),
                wNorm = obj.optDouble("wNorm", fallback.wNorm.toDouble()).toFloat(),
                hNorm = obj.optDouble("hNorm", fallback.hNorm.toDouble()).toFloat()
            ).clamped()
        }
    }
}

/**
 * Standard animation playback cadence tiers for sprite sheets and rig metadata.
 */
enum class FrameCadenceFps(val fps: Int, val label: String) {
    FPS_12(12, "12 FPS"),
    FPS_15(15, "15 FPS"),
    FPS_30(30, "30 FPS"),
    FPS_60(60, "60 FPS");

    companion object {
        fun fromFps(fps: Int): FrameCadenceFps =
            entries.minByOrNull { kotlin.math.abs(it.fps - fps) } ?: FPS_15
    }
}

/**
 * Deterministic `rig_meta.json` model capturing frame dimensions, cadence, foot pivot,
 * combat hurtbox/hitbox, and all 7 fusion/rig anchor sockets.
 */
data class RigMetadata(
    val assetName: String,
    val frameWidth: Int,
    val frameHeight: Int,
    val frameCount: Int,
    val cadenceFps: FrameCadenceFps,
    val feetPivotYNorm: Float,
    val hurtbox: NormalizedRectBox,
    val hitbox: NormalizedRectBox,
    val anchors: Map<AnchorPointType, NormalizedAnchor>
) {
    fun toJsonObject(): JSONObject {
        val anchorsObj = JSONObject()
        AnchorPointType.entries.forEach { type ->
            val anchor = (anchors[type] ?: defaultAnchor(type, feetPivotYNorm)).clamped()
            anchorsObj.put(
                type.name,
                JSONObject().apply {
                    put("xNorm", anchor.xNorm.toDouble())
                    put("yNorm", anchor.yNorm.toDouble())
                }
            )
        }
        return JSONObject().apply {
            put("assetName", assetName)
            put("frameWidth", frameWidth)
            put("frameHeight", frameHeight)
            put("frameCount", frameCount)
            put("cadenceFps", cadenceFps.fps)
            put("feetPivotYNorm", feetPivotYNorm.coerceIn(0f, 1f).toDouble())
            put("hurtbox", hurtbox.toJsonObject())
            put("hitbox", hitbox.toJsonObject())
            put("anchors", anchorsObj)
        }
    }

    fun toJsonString(): String = toJsonObject().toString(2)

    companion object {
        private fun defaultAnchor(type: AnchorPointType, feetPivotY: Float): NormalizedAnchor {
            val safeFeet = feetPivotY.coerceIn(0.5f, 1f)
            return when (type) {
                AnchorPointType.FEET_PIVOT -> NormalizedAnchor.of(type, 0.50f, safeFeet)
                AnchorPointType.HEAD -> NormalizedAnchor.of(type, 0.50f, (safeFeet - 0.72f).coerceIn(0.08f, 0.45f))
                AnchorPointType.BODY -> NormalizedAnchor.of(type, 0.50f, (safeFeet - 0.42f).coerceIn(0.25f, 0.75f))
                AnchorPointType.WING -> NormalizedAnchor.of(type, 0.32f, (safeFeet - 0.50f).coerceIn(0.20f, 0.70f))
                AnchorPointType.HORN -> NormalizedAnchor.of(type, 0.50f, (safeFeet - 0.84f).coerceIn(0.04f, 0.35f))
                AnchorPointType.TAIL -> NormalizedAnchor.of(type, 0.24f, (safeFeet - 0.26f).coerceIn(0.35f, 0.88f))
                AnchorPointType.VISOR -> NormalizedAnchor.of(type, 0.56f, (safeFeet - 0.70f).coerceIn(0.10f, 0.45f))
            }
        }

        fun defaultForSprite(
            name: String,
            width: Int,
            height: Int,
            frameCount: Int
        ): RigMetadata {
            val safeW = width.coerceAtLeast(1)
            val safeH = height.coerceAtLeast(1)
            val safeCount = frameCount.coerceAtLeast(1)
            val feetY = 0.94f
            val defaultAnchors = AnchorPointType.entries.associateWith { defaultAnchor(it, feetY) }
            return RigMetadata(
                assetName = name.ifBlank { "Studio Sprite" },
                frameWidth = safeW,
                frameHeight = safeH,
                frameCount = safeCount,
                cadenceFps = FrameCadenceFps.FPS_15,
                feetPivotYNorm = feetY,
                hurtbox = NormalizedRectBox(0.24f, 0.14f, 0.52f, 0.78f).clamped(),
                hitbox = NormalizedRectBox(0.54f, 0.26f, 0.36f, 0.44f).clamped(),
                anchors = defaultAnchors
            )
        }

        fun fromJsonString(json: String): RigMetadata {
            val root = JSONObject(json)
            val name = root.optString("assetName", "Studio Sprite").ifBlank { "Studio Sprite" }
            val fw = root.optInt("frameWidth", 48).coerceAtLeast(1)
            val fh = root.optInt("frameHeight", 48).coerceAtLeast(1)
            val fc = root.optInt("frameCount", 1).coerceAtLeast(1)
            val cadence = FrameCadenceFps.fromFps(root.optInt("cadenceFps", 15))
            val feetY = root.optDouble("feetPivotYNorm", 0.94).toFloat().coerceIn(0f, 1f)
            val fallback = defaultForSprite(name, fw, fh, fc)
            val hurtbox = NormalizedRectBox.fromJsonObject(root.optJSONObject("hurtbox"), fallback.hurtbox)
            val hitbox = NormalizedRectBox.fromJsonObject(root.optJSONObject("hitbox"), fallback.hitbox)
            val anchorsJson = root.optJSONObject("anchors")
            val parsedAnchors = linkedMapOf<AnchorPointType, NormalizedAnchor>()
            AnchorPointType.entries.forEach { type ->
                val aObj = anchorsJson?.optJSONObject(type.name)
                parsedAnchors[type] = if (aObj != null) {
                    NormalizedAnchor.of(
                        type = type,
                        xNorm = aObj.optDouble("xNorm", 0.5).toFloat(),
                        yNorm = aObj.optDouble("yNorm", 0.5).toFloat()
                    )
                } else {
                    defaultAnchor(type, feetY)
                }
            }
            return RigMetadata(
                assetName = name,
                frameWidth = fw,
                frameHeight = fh,
                frameCount = fc,
                cadenceFps = cadence,
                feetPivotYNorm = feetY,
                hurtbox = hurtbox,
                hitbox = hitbox,
                anchors = parsedAnchors
            )
        }
    }
}

/**
 * 4-map visual decomposition produced from any 2D sprite frame:
 * - [colorIdMap]: flat color-blocking regions wired to the 20-Color Wheel
 * - [inkShadowMap]: black ink outlines + deep ink-pool cast shadows + shadow mask
 * - [matEmissiveMap]: R = clearcoat gloss, G = metallic bands, B = self-emissive Mana Circuits / visors
 * - [depthNormMap]: 2.5D/3D convex height + RGB tangent normal map
 */
data class ThreeMapLayerSet(
    val colorIdMap: Bitmap,
    val inkShadowMap: Bitmap,
    val matEmissiveMap: Bitmap,
    val depthNormMap: Bitmap,
    val regionBaseColors: List<Int>
)

/**
 * Single-active compiled hardware visual output tier.
 */
enum class HardwareVisualOutputTier(
    val displayName: String,
    val technicalSubtitle: String
) {
    PIXEL_16BIT(
        displayName = "16-Bit Pixel",
        technicalSubtitle = "1px dark outline • nearest-neighbor • 15-bit BGR555"
    ),
    ARCADE_32BIT_60FPS(
        displayName = "32-Bit Arcade 60 FPS",
        technicalSubtitle = "hard 2-step cel • ordered 4x4 shadow dithering"
    ),
    TRUE_FUSION_HD(
        displayName = "True Fusion HD",
        technicalSubtitle = "bold ink • angled shadow halftones • clearcoat glints"
    ),
    MODERN_25D_3D(
        displayName = "2.5D / 3D Normal-Lit",
        technicalSubtitle = "depth_norm directional lighting + clearcoat/emissive"
    )
}

/**
 * Output produced by [FootAlignedSlicerEngine.compileHardwareVisualSplit] for each hardware tier.
 */
data class CompiledHardwareVisualOutput(
    val tier: HardwareVisualOutputTier,
    val bitmap: Bitmap,
    val paletteColorCount: Int,
    val technicalSummary: String
)
