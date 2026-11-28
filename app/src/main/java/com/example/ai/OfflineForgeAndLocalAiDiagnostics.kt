package com.example.ai

import android.graphics.Color
import android.util.Log
import com.example.model.audio.AudioAssetEntry
import com.example.model.audio.AudioAssetFormat
import com.example.model.audio.AudioLoopPoints
import com.example.model.audio.AudioSynthAndDspEngine
import com.example.model.audio.AudioUsageCategory
import com.example.model.audio.ProceduralCrySpec
import com.example.model.audio.ScoreSequenceNote
import com.example.model.audio.ScoreSequenceSpec
import com.example.model.masterpack.ColorWheelEntry
import com.example.model.masterpack.TWENTY_COLOR_WHEEL
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.PrimitiveShapeType
import com.example.model.model3d.Scratch3DBuilderEngine
import com.example.model.model3d.Scratch3DPart
import com.example.model.model3d.Scratch3DProject
import com.example.model.sprite.ScratchSpriteProject
import com.example.model.sprite.SpriteCanvasEditorEngine
import com.example.model.sprite.SpriteSheetData
import com.example.model.texture.ProceduralMaterialPattern
import com.example.model.texture.ScratchTextureEngine
import com.example.model.texture.ScratchTextureProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private const val OFFLINE_FORGE_TAG = "OfflineForgeDiag"

// =============================================================================
// 1. Offline Procedural Forge (Zero-Token, Zero-Network, Training-Aware)
// =============================================================================

/**
 * Deterministic, zero-token, zero-network procedural asset builder supporting all 5 PixelForge
 * modalities (2D Sprite Sheets, 3D Kitbash Models, Seamless Textures, Creature Cries, 16-Step Chiptunes)
 * plus Offline Director Chat. Reads both the user's prompt AND active training/wiki hints.
 */
object OfflineProceduralForge {

    private data class ExtractedTraits(
        val primary: ColorWheelEntry,
        val secondary: ColorWheelEntry,
        val accent: ColorWheelEntry,
        val hasWings: Boolean,
        val hasHorns: Boolean,
        val hasTail: Boolean,
        val isVisorOrMech: Boolean,
        val isCapeOrKnight: Boolean,
        val hasBladeOrCannon: Boolean,
        val isSlimeOrCrystal: Boolean,
        val isShipOrHover: Boolean,
        val bpm: Int,
        val seed: Int
    )

    private fun extractTraits(prompt: String, trainingHints: String): ExtractedTraits {
        val combined = "${prompt.trim()} ${trainingHints.trim()}".lowercase()
        val seed = abs(combined.hashCode()).coerceAtLeast(1)

        val matchedColors = mutableListOf<ColorWheelEntry>()
        fun pickColor(id: String) {
            TWENTY_COLOR_WHEEL.firstOrNull { it.id == id }?.let { entry ->
                if (matchedColors.none { it.id == entry.id }) matchedColors.add(entry)
            }
        }

        if (containsAny(combined, "obsidian", "onyx", "shadow", "stealth", "black", "dark")) pickColor("obsidian_ink")
        if (containsAny(combined, "crimson", "red", "ruby", "scarlet", "flame", "fire", "magma")) pickColor("crimson_flare")
        if (containsAny(combined, "orange", "blaze", "inferno", "ember", "tokusatsu")) pickColor("tokusatsu_blaze")
        if (containsAny(combined, "gold", "golden", "solar", "yellow", "sun")) pickColor("solar_gold")
        if (containsAny(combined, "amber", "reactor", "honey", "brass")) pickColor("amber_reactor")
        if (containsAny(combined, "emerald", "green", "venom", "leaf", "jade")) pickColor("emerald_pulse")
        if (containsAny(combined, "forest", "moss", "jungle")) pickColor("forest_armor")
        if (containsAny(combined, "acid", "lime", "volt", "toxic")) pickColor("acid_volt")
        if (containsAny(combined, "cyan", "mana", "plasma", "circuit", "electric", "neon blue")) pickColor("mana_cyan")
        if (containsAny(combined, "ice", "frost", "glacier", "crystal", "aqua")) pickColor("glacier_ice")
        if (containsAny(combined, "cobalt", "blue", "sapphire", "vanguard")) pickColor("cobalt_vanguard")
        if (containsAny(combined, "navy", "astral", "cosmic", "space")) pickColor("astral_navy")
        if (containsAny(combined, "violet", "purple", "ultraviolet", "void", "amethyst")) pickColor("ultraviolet_core")
        if (containsAny(combined, "magenta", "pink", "rose", "cyber pink")) pickColor("neon_magenta")
        if (containsAny(combined, "silver", "chrome", "titanium", "metallic")) pickColor("titanium_silver")
        if (containsAny(combined, "steel", "gunmetal", "iron", "mech")) pickColor("gunmetal_steel")
        if (containsAny(combined, "white", "ivory", "ceramic", "pearl")) pickColor("ceramic_white")
        if (containsAny(combined, "bronze", "copper", "terracotta", "earth", "rust")) pickColor("terracotta_bronze")

        val primary = matchedColors.getOrNull(0)
            ?: TWENTY_COLOR_WHEEL[seed % TWENTY_COLOR_WHEEL.size]
        val secondary = matchedColors.getOrNull(1)
            ?: TWENTY_COLOR_WHEEL[(seed / 3 + 5) % TWENTY_COLOR_WHEEL.size]
        val accent = matchedColors.getOrNull(2)
            ?: TWENTY_COLOR_WHEEL.firstOrNull { it.id == "mana_cyan" && it.id != primary.id }
            ?: TWENTY_COLOR_WHEEL[(seed / 7 + 12) % TWENTY_COLOR_WHEEL.size]

        val bpmMatch = Regex("""\b(\d{2,3})\s*bpm\b""").find(combined)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val bpm = when {
            bpmMatch != null -> bpmMatch.coerceIn(80, 200)
            containsAny(combined, "boss", "battle", "turbo", "rush", "hyper", "speed", "fighting") -> 152
            containsAny(combined, "calm", "town", "village", "ambient", "chill", "lullaby") -> 98
            containsAny(combined, "dungeon", "stealth", "cave", "shadow") -> 114
            else -> 128 + (seed % 24)
        }

        return ExtractedTraits(
            primary = primary,
            secondary = secondary,
            accent = accent,
            hasWings = containsAny(combined, "wing", "dragon", "drake", "phoenix", "angel", "falcon", "jet", "bird", "aerial", "griffin"),
            hasHorns = containsAny(combined, "horn", "crest", "antler", "crown", "oni", "demon", "dragon", "stag", "ram"),
            hasTail = containsAny(combined, "tail", "dragon", "fox", "wolf", "beast", "monster", "dino", "lizard", "cat"),
            isVisorOrMech = containsAny(combined, "visor", "mech", "mecha", "robot", "android", "cyber", "cyborg", "automaton", "ranger", "tokusatsu", "pilot"),
            isCapeOrKnight = containsAny(combined, "cape", "knight", "paladin", "cloak", "scarf", "hero", "samurai", "vanguard", "courier"),
            hasBladeOrCannon = containsAny(combined, "blade", "sword", "glaive", "katana", "spear", "cannon", "blaster", "lance", "rifle", "hammer"),
            isSlimeOrCrystal = containsAny(combined, "slime", "crystal", "gem", "blob", "golem", "ooze", "prism", "orb", "jelly"),
            isShipOrHover = containsAny(combined, "ship", "starfighter", "hover", "tank", "cruiser", "craft", "drone", "vehicle", "jet", "speeder"),
            bpm = bpm,
            seed = seed
        )
    }

    /**
     * Generates a mirrored, cel-shaded, 1px-ink-outlined, foot-aligned animated 2D sprite sheet
     * completely offline using zero AI tokens.
     */
    fun buildOffline2DSpriteSheet(
        assetName: String,
        prompt: String,
        sizePx: Int = 32,
        frameCount: Int = 4,
        targetFps: Int = 15,
        trainingHints: String = ""
    ): Pair<ScratchSpriteProject, SpriteSheetData> {
        val dim = sizePx.coerceIn(SpriteCanvasEditorEngine.MIN_CANVAS_DIM, SpriteCanvasEditorEngine.MAX_CANVAS_DIM)
        val safeFrames = frameCount.coerceIn(1, 8)
        val safeFps = targetFps.coerceIn(1, 120)
        val traits = extractTraits(prompt, trainingHints)
        val cleanName = assetName.trim().ifBlank { deriveNameFromPrompt(prompt, "Offline Sprite") }

        val inkColor = Color.rgb(0x16, 0x1B, 0x26)
        val primaryColor = traits.primary.colorInt
        val shadeColor = darkenColor(primaryColor, 0.62f)
        val secondaryColor = traits.secondary.colorInt
        val accentColor = traits.accent.colorInt
        val glintColor = Color.rgb(0xF8, 0xFA, 0xFC)

        val frames = List(safeFrames) { frameIdx ->
            val buf = IntArray(dim * dim) { Color.TRANSPARENT }
            val bobY = if (frameIdx % 2 == 1) -1 else 0
            val wingLift = when (frameIdx % 4) {
                0 -> 0
                1 -> -2
                2 -> -1
                else -> 1
            }
            val cx = dim / 2
            val footY = dim - 2

            fun putMirror(xFromCenter: Int, y: Int, color: Int) {
                if (y !in 1 until dim - 1) return
                val lx = cx - 1 - xFromCenter
                val rx = cx + xFromCenter
                if (lx in 1 until dim - 1) buf[y * dim + lx] = color
                if (rx in 1 until dim - 1) buf[y * dim + rx] = color
            }

            fun putSingle(x: Int, y: Int, color: Int) {
                if (x in 1 until dim - 1 && y in 1 until dim - 1) {
                    buf[y * dim + x] = color
                }
            }

            if (traits.isShipOrHover) {
                // Sleek starfighter / hovercraft hull
                val bodyTop = (dim * 0.25f).roundToInt() + bobY
                val bodyBottom = (dim * 0.72f).roundToInt() + bobY
                for (y in bodyTop..bodyBottom) {
                    val t = (y - bodyTop).toFloat() / (bodyBottom - bodyTop).coerceAtLeast(1)
                    val halfW = ((0.08f + t * 0.34f) * dim).roundToInt().coerceAtLeast(2)
                    for (dx in 0 until halfW) {
                        val c = when {
                            dx == halfW - 1 -> shadeColor
                            t < 0.35f && dx < 2 -> accentColor
                            dx % 3 == 0 -> secondaryColor
                            else -> primaryColor
                        }
                        putMirror(dx, y, c)
                    }
                }
                // Specular cockpit glint & thruster glow
                putMirror(0, bodyTop + 2, glintColor)
                val thrustLen = 2 + (frameIdx % 2)
                for (ty in 1..thrustLen) {
                    putMirror(2, bodyBottom + ty, accentColor)
                }
            } else if (traits.isSlimeOrCrystal) {
                // Crystalline / slime creature anchored cleanly to footY
                val domeH = (dim * 0.44f).roundToInt() + if (frameIdx % 2 == 1) -1 else 1
                val topY = (footY - domeH).coerceAtLeast(3)
                for (y in topY..footY) {
                    val t = (y - topY).toFloat() / (footY - topY).coerceAtLeast(1)
                    val halfW = ((0.14f + kotlin.math.sin(t * Math.PI * 0.85).toFloat() * 0.26f) * dim)
                        .roundToInt()
                        .coerceAtLeast(2)
                    for (dx in 0 until halfW) {
                        val c = when {
                            y > footY - 2 -> shadeColor
                            dx == 0 && y < topY + 4 -> glintColor
                            (dx + y) % 5 == 0 -> accentColor
                            else -> primaryColor
                        }
                        putMirror(dx, y, c)
                    }
                }
                // Expressive eyes
                val eyeY = topY + (domeH * 0.48f).roundToInt()
                putMirror(2, eyeY, inkColor)
                putMirror(2, eyeY - 1, glintColor)
            } else {
                // Hero / Mech / Creature figure with foot-aligned legs, torso, head, visor, wings, horns, cape, weapon
                val legTop = (footY - (dim * 0.24f).roundToInt()).coerceAtLeast(dim / 2)
                val torsoTop = (legTop - (dim * 0.25f).roundToInt() + bobY).coerceAtLeast(6)
                val headTop = (torsoTop - (dim * 0.20f).roundToInt()).coerceAtLeast(3)
                val strideShift = if (frameIdx % 2 == 0) 0 else 1

                // Optional cape backdrop
                if (traits.isCapeOrKnight) {
                    for (y in torsoTop..(footY - 1)) {
                        val capeW = ((dim * 0.22f) + ((y - torsoTop) * 0.14f)).roundToInt() + (if (frameIdx % 2 == 1) 1 else 0)
                        for (dx in 0 until capeW.coerceAtMost(dim / 2 - 2)) {
                            putMirror(dx, y, secondaryColor)
                        }
                    }
                }

                // Optional wings
                if (traits.hasWings) {
                    val wingSpan = (dim * 0.36f).roundToInt().coerceAtLeast(4)
                    for (step in 2..wingSpan) {
                        val wy = torsoTop + 1 + wingLift + (step / 3)
                        for (thick in 0..2) {
                            putMirror(step + 2, wy + thick, if (thick == 0) accentColor else secondaryColor)
                        }
                    }
                }

                // Grounded legs / boots anchored at footY
                for (y in legTop..footY) {
                    val legOffset = if (y == footY) 2 + strideShift else 2
                    putMirror(legOffset, y, if (y >= footY - 1) shadeColor else secondaryColor)
                    putMirror(legOffset + 1, y, if (y >= footY - 1) inkColor else primaryColor)
                }

                // Torso & Mana Circuit core
                val torsoHalfW = (dim * 0.16f).roundToInt().coerceAtLeast(3)
                for (y in torsoTop until legTop) {
                    for (dx in 0 until torsoHalfW) {
                        val c = when {
                            dx == 0 && (y == torsoTop + 2 || y == torsoTop + 3) -> accentColor
                            dx == torsoHalfW - 1 -> shadeColor
                            else -> primaryColor
                        }
                        putMirror(dx, y, c)
                    }
                    // Arms / pauldrons
                    if (y in torsoTop..(torsoTop + 4)) {
                        putMirror(torsoHalfW, y + (if (frameIdx % 2 == 1) 1 else 0), secondaryColor)
                        putMirror(torsoHalfW + 1, y + (if (frameIdx % 2 == 1) 1 else 0), primaryColor)
                    }
                }

                // Head & reflective Tokusatsu visor / eyes
                val headHalfW = (dim * 0.13f).roundToInt().coerceAtLeast(2)
                for (y in headTop until torsoTop) {
                    for (dx in 0 until headHalfW) {
                        val isVisorRow = y == headTop + ((torsoTop - headTop) / 2)
                        val c = when {
                            isVisorRow && traits.isVisorOrMech -> accentColor
                            isVisorRow && dx == 1 -> glintColor
                            y == headTop -> glintColor
                            else -> primaryColor
                        }
                        putMirror(dx, y, c)
                    }
                }
                // Specular clearcoat visor glint
                putMirror(0, headTop + 1, glintColor)

                // Optional horns / crest
                if (traits.hasHorns) {
                    for (h in 1..3) {
                        putMirror(headHalfW - 1 + h, headTop - h + 1, accentColor)
                    }
                }

                // Optional tail (asymmetric right sway)
                if (traits.hasTail) {
                    val tailY = legTop - 1
                    for (t in 0..4) {
                        putSingle(cx + torsoHalfW + t, tailY + (if (frameIdx % 2 == 0) 0 else -1) - (t / 2), secondaryColor)
                    }
                }

                // Optional blade / cannon on right hand
                if (traits.hasBladeOrCannon) {
                    val weaponX = cx + torsoHalfW + 2
                    for (wy in (headTop + 1)..(legTop - 1)) {
                        putSingle(weaponX, wy + bobY, if (wy <= torsoTop) glintColor else accentColor)
                    }
                }
            }

            // Apply crisp 1px dark ink outline around every opaque pixel (Graphic Novel / 32-bit Cel rule)
            val outlined = buf.copyOf()
            for (y in 0 until dim) {
                for (x in 0 until dim) {
                    val idx = y * dim + x
                    if (Color.alpha(buf[idx]) == 0) {
                        val touchesOpaque =
                            (x > 0 && Color.alpha(buf[idx - 1]) > 0) ||
                            (x < dim - 1 && Color.alpha(buf[idx + 1]) > 0) ||
                            (y > 0 && Color.alpha(buf[idx - dim]) > 0) ||
                            (y < dim - 1 && Color.alpha(buf[idx + dim]) > 0)
                        if (touchesOpaque) {
                            outlined[idx] = inkColor
                        }
                    }
                }
            }
            outlined
        }

        val scratchProject = ScratchSpriteProject(
            id = "offline2d_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            width = dim,
            height = dim,
            frames = frames,
            activeFrameIndex = 0,
            fps = safeFps.coerceIn(1, 60),
            mirrorX = true
        )

        val exportedSheet = SpriteCanvasEditorEngine.exportProjectToSpriteSheet(scratchProject)
        val finalSheet = exportedSheet.copy(
            sourceKindLabel = "OFFLINE PROCEDURAL 2D",
            animations = exportedSheet.animations.map { it.copy(fps = safeFps) }
        )
        return scratchProject to finalSheet
    }

    /**
     * Generates a genuine editable 3D kitbash project (`5..12` primitive parts including mirrored
     * limbs/wings/horns) and compiles it via [Scratch3DBuilderEngine.compileProjectToMesh3D].
     */
    fun buildOffline3DModel(
        assetName: String,
        prompt: String,
        trainingHints: String = ""
    ): Pair<Scratch3DProject, Mesh3D> {
        val traits = extractTraits(prompt, trainingHints)
        val cleanName = assetName.trim().ifBlank { deriveNameFromPrompt(prompt, "Offline 3D Model") }

        val pColor = colorIntToLong(traits.primary.colorInt)
        val sColor = colorIntToLong(traits.secondary.colorInt)
        val aColor = colorIntToLong(traits.accent.colorInt)
        val parts = mutableListOf<Scratch3DPart>()

        fun addPart(
            name: String,
            shape: PrimitiveShapeType,
            px: Float,
            py: Float,
            pz: Float,
            sx: Float,
            sy: Float,
            sz: Float,
            color: Long,
            mirrorX: Boolean = false,
            rotY: Float = 0f
        ) {
            parts.add(
                Scratch3DPart(
                    id = "off3d_${parts.size + 1}_${UUID.randomUUID().toString().take(4)}",
                    name = name,
                    shape = shape,
                    posX = px,
                    posY = py,
                    posZ = pz,
                    scaleX = sx,
                    scaleY = sy,
                    scaleZ = sz,
                    rotYDeg = rotY,
                    colorLong = color,
                    mirrorX = mirrorX
                )
            )
        }

        if (traits.isShipOrHover) {
            addPart("Main Fuselage", PrimitiveShapeType.BOX, 0f, 0f, 0f, 0.56f, 0.30f, 1.35f, pColor)
            addPart("Forward Nose Cone", PrimitiveShapeType.PYRAMID, 0f, 0.02f, 0.85f, 0.42f, 0.28f, 0.55f, sColor)
            addPart("Canopy Cockpit", PrimitiveShapeType.SPHERE, 0f, 0.22f, 0.16f, 0.34f, 0.20f, 0.52f, aColor)
            addPart("Swept Wings", PrimitiveShapeType.WEDGE_WING, 0.62f, -0.02f, -0.10f, 0.84f, 0.14f, 0.62f, sColor, mirrorX = true)
            addPart("Twin Thrusters", PrimitiveShapeType.CYLINDER, 0.26f, -0.02f, -0.78f, 0.20f, 0.20f, 0.38f, aColor, mirrorX = true)
            addPart("Hover Stabilizer Ring", PrimitiveShapeType.TORUS_RING, 0f, -0.18f, -0.05f, 0.68f, 0.16f, 0.68f, aColor)
        } else {
            // Core chassis / hero / creature assembly (5..12 editable parts)
            addPart("Torso Core", PrimitiveShapeType.BOX, 0f, 0.18f, 0f, 0.62f, 0.68f, 0.44f, pColor)
            addPart("Mana Reactor Crest", PrimitiveShapeType.SPHERE, 0f, 0.24f, 0.23f, 0.22f, 0.22f, 0.16f, aColor)
            addPart("Head Unit", if (traits.isSlimeOrCrystal) PrimitiveShapeType.PYRAMID else PrimitiveShapeType.SPHERE, 0f, 0.72f, 0f, 0.40f, 0.40f, 0.40f, pColor)
            addPart("Visor Strip", PrimitiveShapeType.BOX, 0f, 0.74f, 0.20f, 0.32f, 0.11f, 0.12f, aColor)
            addPart("Limb / Arm Pair", PrimitiveShapeType.CYLINDER, 0.48f, 0.20f, 0f, 0.22f, 0.56f, 0.22f, sColor, mirrorX = true)
            addPart("Boot / Leg Pair", PrimitiveShapeType.BOX, 0.22f, -0.44f, 0f, 0.24f, 0.56f, 0.28f, sColor, mirrorX = true)

            if (traits.hasWings) {
                addPart("Aero Wings", PrimitiveShapeType.WEDGE_WING, 0.66f, 0.34f, -0.22f, 0.82f, 0.22f, 0.48f, aColor, mirrorX = true)
            }
            if (traits.hasHorns) {
                addPart("Crest Horns", PrimitiveShapeType.CONE, 0.18f, 0.98f, 0.04f, 0.14f, 0.32f, 0.14f, aColor, mirrorX = true)
            }
            if (traits.hasBladeOrCannon) {
                addPart("Dorsal Cannon / Blade", PrimitiveShapeType.CYLINDER, 0.34f, 0.58f, -0.18f, 0.14f, 0.48f, 0.14f, aColor, mirrorX = true)
            }
            if (traits.hasTail) {
                addPart("Tail Counterweight", PrimitiveShapeType.CONE, 0f, -0.18f, -0.52f, 0.20f, 0.46f, 0.20f, sColor)
            }
            if (traits.isCapeOrKnight) {
                addPart("Back Mantle", PrimitiveShapeType.WEDGE_WING, 0f, 0.06f, -0.32f, 0.68f, 0.78f, 0.16f, sColor)
            }
            if (traits.isVisorOrMech && parts.size < 12) {
                addPart("Halo Energy Ring", PrimitiveShapeType.TORUS_RING, 0f, 0.36f, -0.26f, 0.54f, 0.54f, 0.18f, aColor)
            }
        }

        val project = Scratch3DProject(
            id = "offline3d_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            parts = parts.take(12),
            selectedPartId = parts.firstOrNull()?.id
        )
        val compiledMesh = Scratch3DBuilderEngine.compileProjectToMesh3D(project)
            .copy(sourceKindLabel = "OFFLINE PROCEDURAL 3D")
        return project to compiledMesh
    }

    /**
     * Generates a seamless procedural material texture matching prompt & training traits via [ScratchTextureEngine].
     */
    fun buildOfflineTexture(
        assetName: String,
        prompt: String,
        sizePx: Int = 32,
        trainingHints: String = ""
    ): ScratchTextureProject {
        val traits = extractTraits(prompt, trainingHints)
        val combined = "${prompt.trim()} ${trainingHints.trim()}".lowercase()
        val cleanName = assetName.trim().ifBlank { deriveNameFromPrompt(prompt, "Offline Texture") }
        val dim = sizePx.coerceIn(ScratchTextureEngine.MIN_TEX_DIM, ScratchTextureEngine.MAX_TEX_DIM)

        val pattern = when {
            containsAny(combined, "circuit", "mana", "neon", "cyber", "energy", "grid", "hologram") ->
                ProceduralMaterialPattern.ENERGY_CIRCUIT
            containsAny(combined, "armor", "plate", "metal", "steel", "clearcoat", "mech", "hull", "chrome") ->
                ProceduralMaterialPattern.ARMOR_PLATE
            containsAny(combined, "scale", "dragon", "reptile", "organic", "monster", "skin", "hide") ->
                ProceduralMaterialPattern.ORGANIC_SCALES
            containsAny(combined, "brick", "stone", "dungeon", "wall", "castle", "tile", "ruin") ->
                ProceduralMaterialPattern.DUNGEON_BRICK
            else -> ProceduralMaterialPattern.CEL_GRADIENT
        }

        val base = ScratchTextureEngine.generateMaterialPattern(
            name = cleanName,
            sizePx = dim,
            pattern = pattern,
            primaryColorInt = traits.primary.colorInt,
            secondaryColorInt = traits.secondary.colorInt,
            accentColorInt = traits.accent.colorInt
        )

        // Overlay crisp symmetric specular rivets / circuit accents
        val pixels = base.pixels.copyOf()
        val glint = Color.rgb(0xF8, 0xFA, 0xFC)
        val inset = (dim / 8).coerceAtLeast(1)
        val corners = listOf(
            inset to inset,
            (dim - 1 - inset) to inset,
            inset to (dim - 1 - inset),
            (dim - 1 - inset) to (dim - 1 - inset)
        )
        corners.forEach { (cx, cy) ->
            if (cx in 0 until dim && cy in 0 until dim) {
                pixels[cy * dim + cx] = glint
            }
        }
        return base.copy(pixels = pixels, mirrorX = true, mirrorY = true)
    }

    /**
     * Synthesizes a real procedural creature/character cry with genuine PCM waveform peaks offline.
     */
    fun buildOfflineAudioCry(
        title: String,
        prompt: String,
        trainingHints: String = ""
    ): Pair<ProceduralCrySpec, AudioAssetEntry> {
        val traits = extractTraits(prompt, trainingHints)
        val combined = "${prompt.trim()} ${trainingHints.trim()}".lowercase()
        val cleanTitle = title.trim().ifBlank { deriveNameFromPrompt(prompt, "Offline Cry SFX") }
        val slug = cleanTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "offline_cry" }

        val (startHz, endHz, durMs, modHz, noise) = when {
            traits.hasWings || containsAny(combined, "chirp", "bird", "phoenix", "high") ->
                listOf(880f, 1460f, 440f, 28f, 0.10f)
            traits.isVisorOrMech || traits.isShipOrHover || containsAny(combined, "laser", "siren", "mech") ->
                listOf(620f, 210f, 520f, 44f, 0.16f)
            traits.isSlimeOrCrystal ->
                listOf(520f, 940f, 380f, 32f, 0.08f)
            containsAny(combined, "roar", "dragon", "boss", "giant", "growl", "heavy") ->
                listOf(280f, 115f, 760f, 16f, 0.32f)
            else ->
                listOf(420f + (traits.seed % 180), 240f + (traits.seed % 140), 480f, 22f, 0.18f)
        }

        val spec = ProceduralCrySpec(
            speciesKey = slug,
            baseFreqHz = startHz,
            endFreqHz = endHz,
            durationMs = durMs.roundToInt().coerceIn(120, 2000),
            modulationHz = modHz,
            noiseMix = noise
        )
        val pcm = AudioSynthAndDspEngine.synthesizeProceduralCryPcm(spec, sampleRateHz = 22050)
        val peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm, bucketCount = 48)

        val entry = AudioAssetEntry(
            id = "offline_cry_${UUID.randomUUID().toString().take(8)}",
            slotKey = "sfx_cry_$slug",
            title = cleanTitle,
            category = AudioUsageCategory.PROCEDURAL_CRY,
            format = AudioAssetFormat.AUDIO_CUE_JSON,
            sampleRateHz = 22050,
            durationMs = spec.durationMs,
            loopPoints = AudioLoopPoints(0, pcm.size, false),
            waveformPeaks = peaks,
            byteSize = (pcm.size * 2).toLong(),
            crySpec = spec
        )
        return spec to entry
    }

    /**
     * Composes a real 16-step multi-channel chiptune sequence with synthesized PCM waveform peaks offline.
     */
    fun buildOfflineChiptune(
        title: String,
        prompt: String,
        trainingHints: String = ""
    ): Pair<ScoreSequenceSpec, AudioAssetEntry> {
        val traits = extractTraits(prompt, trainingHints)
        val combined = "${prompt.trim()} ${trainingHints.trim()}".lowercase()
        val cleanTitle = title.trim().ifBlank { deriveNameFromPrompt(prompt, "Offline Chiptune Loop") }
        val slug = cleanTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "offline_bgm" }

        val scale = when {
            containsAny(combined, "boss", "dark", "shadow", "dungeon", "battle", "cyber") ->
                intArrayOf(57, 60, 62, 64, 67, 69, 72, 74) // A minor / Dorian energy
            containsAny(combined, "hero", "victory", "sky", "town", "arcade", "bright") ->
                intArrayOf(60, 62, 64, 67, 69, 72, 74, 76) // C major pentatonic hero
            else ->
                intArrayOf(62, 65, 67, 69, 72, 74, 77, 79) // D minor arcade groove
        }

        val notes = mutableListOf<ScoreSequenceNote>()
        for (step in 0 until 16) {
            val beat = step * 0.25f
            // Channel 0: energetic 16-step pulse lead
            if (step % 2 == 0 || (step + traits.seed) % 5 == 0) {
                val noteIdx = (step + (traits.seed % 4)) % scale.size
                notes.add(
                    ScoreSequenceNote(
                        startBeat = beat,
                        durationBeats = if (step % 4 == 0) 0.45f else 0.22f,
                        midiNote = scale[noteIdx],
                        velocity = if (step % 4 == 0) 0.92f else 0.74f,
                        channel = 0
                    )
                )
            }
            // Channel 1: driving triangle bass on quarter-note pulse
            if (step % 4 == 0 || step % 4 == 2) {
                val bassRoot = scale[0] - 12 + if (step >= 8) 5 else 0
                notes.add(
                    ScoreSequenceNote(
                        startBeat = beat,
                        durationBeats = 0.42f,
                        midiNote = bassRoot.coerceIn(36, 60),
                        velocity = 0.85f,
                        channel = 1
                    )
                )
            }
            // Channel 2: harmonic arpeggio shimmer on off-beats
            if (step % 4 == 1 || step % 4 == 3) {
                val arpIdx = ((step / 2) + 2) % scale.size
                notes.add(
                    ScoreSequenceNote(
                        startBeat = beat,
                        durationBeats = 0.20f,
                        midiNote = (scale[arpIdx] + 12).coerceAtMost(96),
                        velocity = 0.56f,
                        channel = 2
                    )
                )
            }
        }

        val spec = ScoreSequenceSpec(
            title = cleanTitle,
            bpm = traits.bpm,
            notes = notes
        )
        val pcm = AudioSynthAndDspEngine.synthesizeScoreSequencePcm(spec, sampleRateHz = 22050)
        val peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm, bucketCount = 48)
        val durationMs = ((pcm.size.toDouble() / 22050.0) * 1000.0).roundToInt().coerceAtLeast(250)

        val entry = AudioAssetEntry(
            id = "offline_bgm_${UUID.randomUUID().toString().take(8)}",
            slotKey = "bgm_loop_$slug",
            title = cleanTitle,
            category = AudioUsageCategory.BGM_LOOP,
            format = AudioAssetFormat.SCORE_SEQ_JSON,
            sampleRateHz = 22050,
            durationMs = durationMs,
            loopPoints = AudioLoopPoints(0, pcm.size, true),
            waveformPeaks = peaks,
            byteSize = (pcm.size * 2).toLong(),
            scoreSpec = spec
        )
        return spec to entry
    }

    /**
     * Generates a structured, training- and wiki-aware offline Asset Director reply with an
     * actionable `BUILD_PROMPT:` line using 0 tokens and 0 network calls.
     */
    fun buildOfflineDirectorChatReply(
        userMessage: String,
        targetFps: Int,
        relevantWikiContext: String = ""
    ): ForgeChatMessage {
        val safeFps = targetFps.coerceIn(1, 120)
        val traits = extractTraits(userMessage, relevantWikiContext)
        val (suggestedTarget, _) = ForgeChatPromptBuilder.extractSuggestedTargetAndPrompt(
            replyText = userMessage,
            fallbackUserPrompt = userMessage
        )

        val featureList = buildList {
            if (traits.hasWings) add("swept aero wings")
            if (traits.hasHorns) add("sculpted crest horns")
            if (traits.isVisorOrMech) add("reflective Tokusatsu visor + Mana Circuits")
            if (traits.isCapeOrKnight) add("layered hero mantle")
            if (traits.hasBladeOrCannon) add("signature blade/cannon silhouette")
            if (traits.hasTail) add("dynamic counter-balanced tail")
            if (traits.isShipOrHover) add("twin thruster fuselage")
            if (traits.isSlimeOrCrystal) add("faceted crystal core")
            if (isEmpty()) add("high-contrast 2-step cel silhouette")
        }.joinToString(", ")

        val wikiSnippet = relevantWikiContext.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("- ") }
            ?.removePrefix("- ")
            ?.take(90)
            .orEmpty()

        val buildPrompt = buildString {
            append(userMessage.trim().ifBlank { "Original arcade game asset" })
            append(" — palette ${traits.primary.name} (${traits.primary.hex}), ${traits.secondary.name} (${traits.secondary.hex}), accent ${traits.accent.name} (${traits.accent.hex})")
            append(", features: $featureList")
            if (suggestedTarget == AiBuildTargetKind.AUDIO_CHIPTUNE) {
                append(", ${traits.bpm} BPM 16-step chiptune")
            } else {
                append(", $safeFps FPS animation cadence")
            }
        }

        val replyText = buildString {
            appendLine("Offline Director Plan (${suggestedTarget.label} @ $safeFps FPS):")
            appendLine("• 20-Color Wheel Palette: ${traits.primary.name} (${traits.primary.hex}) + ${traits.secondary.name} (${traits.secondary.hex}) + ${traits.accent.name} (${traits.accent.hex})")
            appendLine("• Key Silhouette Traits: $featureList")
            if (wikiSnippet.isNotBlank()) {
                appendLine("• Matched Master Hub Wiki Canon: $wikiSnippet")
            }
            appendLine("Tap any Build button below to materialize this immediately offline (0 Tokens).")
            append("BUILD_PROMPT: $buildPrompt")
        }

        return ForgeChatMessage(
            id = "chat_off_${UUID.randomUUID().toString().take(8)}",
            role = ForgeChatRole.ASSISTANT,
            text = replyText,
            providerUsed = "Offline Procedural Engine (0 Tokens)",
            extractedBuildPrompt = buildPrompt,
            suggestedBuildTarget = suggestedTarget
        )
    }

    private fun containsAny(haystack: String, vararg needles: String): Boolean {
        return needles.any { haystack.contains(it) }
    }

    private fun deriveNameFromPrompt(prompt: String, fallback: String): String {
        val words = prompt.trim()
            .replace(Regex("[^A-Za-z0-9\\s-]+"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
            .take(3)
        return if (words.isEmpty()) fallback else words.joinToString(" ") { w ->
            w.replaceFirstChar { it.uppercase() }
        }
    }

    private fun darkenColor(colorInt: Int, factor: Float): Int {
        val r = (Color.red(colorInt) * factor).roundToInt().coerceIn(0, 255)
        val g = (Color.green(colorInt) * factor).roundToInt().coerceIn(0, 255)
        val b = (Color.blue(colorInt) * factor).roundToInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun colorIntToLong(colorInt: Int): Long {
        return (colorInt.toLong() and 0xFFFFFFFFL) or 0xFF000000L
    }
}

// =============================================================================
// 2. Local AI Endpoint Presets & Live Diagnostics (Zero Fake Pre-Filled Models)
// =============================================================================

/**
 * 1-tap Local AI endpoint URL preset.
 * Intentionally carries NO `defaultModel` field — the user MUST explicitly choose their model!
 */
data class LocalAiEndpointPreset(
    val id: String,
    val label: String,
    val baseUrl: String,
    val setupTip: String
)

/**
 * Result of a real HTTP diagnostic ping against a local OpenAI-compatible or Ollama server.
 */
data class LocalEndpointPingResult(
    val reachable: Boolean,
    val latencyMs: Long,
    val discoveredModels: List<String>,
    val summary: String
)

/**
 * Real HTTP endpoint discovery (`GET /models`) and user-trained model ranking for local AI servers.
 * Never fakes reachability and never pre-fills a default model.
 */
object LocalAiEndpointDiagnostics {

    val LOCAL_AI_PRESETS: List<LocalAiEndpointPreset> = listOf(
        LocalAiEndpointPreset(
            id = "on_phone_llamacpp",
            label = "On-Phone llama.cpp",
            baseUrl = "http://127.0.0.1:8080/v1",
            setupTip = "Run llama-server locally on-device (port 8080), then tap Ping & Discover Models to pick your GGUF."
        ),
        LocalAiEndpointPreset(
            id = "pc_ollama_lan",
            label = "PC Ollama (LAN)",
            baseUrl = "http://192.168.1.100:11434/v1",
            setupTip = "Start Ollama with OLLAMA_HOST=0.0.0.0 on your PC, replace the LAN IP, and tap Ping & Discover Models."
        ),
        LocalAiEndpointPreset(
            id = "pc_lm_studio_lan",
            label = "PC LM Studio (LAN)",
            baseUrl = "http://192.168.1.100:1234/v1",
            setupTip = "Enable Local Server + Serve on Local Network in LM Studio (port 1234), then discover loaded models."
        ),
        LocalAiEndpointPreset(
            id = "pc_local_image_bridge",
            label = "PC Local Image/LoRA",
            baseUrl = "http://192.168.1.100:8188/v1",
            setupTip = "Point to your local ComfyUI / A1111 OpenAI-compatible bridge (port 8188) to pick your trained LoRA checkpoint."
        )
    )

    private val pingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Performs a real `GET ${baseUrl.trimEnd('/')}/models` probe (3s timeout) on [Dispatchers.IO].
     * Parses both OpenAI `data[].id` and Ollama `models[].name`. Never fakes reachability.
     */
    suspend fun pingAndDiscoverModels(baseUrl: String): LocalEndpointPingResult = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().trimEnd('/')
        if (cleanBase.isEmpty() || (!cleanBase.startsWith("http://", ignoreCase = true) && !cleanBase.startsWith("https://", ignoreCase = true))) {
            return@withContext LocalEndpointPingResult(
                reachable = false,
                latencyMs = 0L,
                discoveredModels = emptyList(),
                summary = "Enter a valid http:// or https:// local endpoint URL first."
            )
        }

        val modelsUrl = if (cleanBase.endsWith("/models", ignoreCase = true)) cleanBase else "$cleanBase/models"
        val startNs = System.nanoTime()

        try {
            val request = Request.Builder()
                .url(modelsUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            pingClient.newCall(request).execute().use { response ->
                val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext LocalEndpointPingResult(
                        reachable = false,
                        latencyMs = elapsedMs,
                        discoveredModels = emptyList(),
                        summary = "Server responded HTTP ${response.code} in ${elapsedMs}ms at $modelsUrl"
                    )
                }

                val discovered = parseDiscoveredModelIds(body)
                val summary = if (discovered.isEmpty()) {
                    "Reachable (${elapsedMs}ms) — 0 models reported at $modelsUrl"
                } else {
                    "Reachable (${elapsedMs}ms) — discovered ${discovered.size} model(s). Tap one to activate Live AI."
                }
                LocalEndpointPingResult(
                    reachable = true,
                    latencyMs = elapsedMs,
                    discoveredModels = discovered,
                    summary = summary
                )
            }
        } catch (e: Exception) {
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            Log.w(OFFLINE_FORGE_TAG, "Local endpoint ping failed for $modelsUrl: ${e.message}")
            LocalEndpointPingResult(
                reachable = false,
                latencyMs = elapsedMs,
                discoveredModels = emptyList(),
                summary = "Unreachable (${elapsedMs}ms): ${e.localizedMessage ?: "Connection refused or timed out."}"
            )
        }
    }

    /**
     * Parses OpenAI (`data[].id`) and Ollama (`models[].name`) model lists from a JSON payload.
     */
    fun parseDiscoveredModelIds(rawJson: String): List<String> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()
        val found = linkedSetOf<String>()
        try {
            val root = JSONObject(trimmed)
            val dataArr = root.optJSONArray("data")
            if (dataArr != null) {
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", obj.optString("name", "")).trim()
                    if (id.isNotEmpty()) found.add(id)
                }
            }
            val modelsArr = root.optJSONArray("models")
            if (modelsArr != null) {
                for (i in 0 until modelsArr.length()) {
                    val obj = modelsArr.optJSONObject(i)
                    if (obj != null) {
                        val name = obj.optString("name", obj.optString("id", obj.optString("model", ""))).trim()
                        if (name.isNotEmpty()) found.add(name)
                    } else {
                        val str = modelsArr.optString(i, "").trim()
                        if (str.isNotEmpty()) found.add(str)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(OFFLINE_FORGE_TAG, "Could not parse discovered models JSON: ${e.message}")
        }
        return found.toList()
    }

    /**
     * Marks discovered models matching [trainingProfile.customLoraOrModelTag] or
     * [trainingProfile.styleTriggerToken] as `isUserTrainedMatch = true` and sorts them first.
     */
    fun rankDiscoveredModelsWithUserTrainingFirst(
        discoveredModels: List<String>,
        trainingProfile: UserAiTrainingProfile
    ): List<Pair<String, Boolean>> {
        if (discoveredModels.isEmpty()) return emptyList()

        val needles = buildSet {
            val rawLora = trainingProfile.customLoraOrModelTag.trim()
            if (rawLora.isNotEmpty()) {
                add(rawLora.lowercase())
                // Extract inner name from <lora:my_model:0.85>
                val stripped = rawLora
                    .removePrefix("<")
                    .removeSuffix(">")
                    .removePrefix("lora:")
                    .substringBefore(":")
                    .trim()
                if (stripped.length >= 2) add(stripped.lowercase())
            }
            val trigger = trainingProfile.styleTriggerToken.trim()
            if (trigger.length >= 2) add(trigger.lowercase())
        }

        val annotated = discoveredModels.distinct().map { modelId ->
            val lowerModel = modelId.lowercase()
            val normalizedModel = lowerModel.replace(Regex("[^a-z0-9]+"), "")
            val isMatch = needles.any { needle ->
                val normNeedle = needle.replace(Regex("[^a-z0-9]+"), "")
                lowerModel.contains(needle) || (normNeedle.length >= 3 && normalizedModel.contains(normNeedle))
            }
            modelId to isMatch
        }

        return annotated.sortedByDescending { it.second }
    }
}
