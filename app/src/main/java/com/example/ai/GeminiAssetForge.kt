package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.model.concept.ConceptAnalysis
import com.example.model.concept.ExtractedColor
import com.example.model.model3d.Face3D
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ProceduralMeshes
import com.example.model.model3d.Vector3
import com.example.model.sprite.SpriteSheetData
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TAG = "GeminiAssetForge"

enum class AssetTarget {
    MODEL_3D,
    SPRITE_SHEET
}

enum class ArtTheme(
    val displayName: String,
    val styleDirective: String = ""
) {
    CYBERPUNK("Cyberpunk Sci-Fi"),
    FANTASY_RPG("Fantasy RPG"),
    RETRO_VOXEL("Retro Voxel"),
    LOW_POLY("Stylized Low-Poly"),
    ARCADE_COMIC_CEL(
        displayName = "32-Bit Arcade Comic-Cel Fusion",
        styleDirective = GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_BLOCK
    )
}

enum class GeminiInputImageSource(val label: String, val badge: String) {
    IMPORTED_CONCEPT_ART("Imported Concept / Photo", "CONCEPT PHOTO"),
    ACTIVE_SPRITESHEET("Active / Imported 2D Spritesheet", "SPRITESHEET"),
    ACTIVE_3D_TEXTURE("Active / Imported 3D Texture", "UV TEXTURE"),
    DESCRIPTION_ONLY("Text Description Only", "TEXT ONLY")
}

enum class GeminiOperationMode(val title: String, val subtitle: String) {
    ENHANCE_ASSET(
        title = "Auto-Enhance Asset",
        subtitle = "Upgrades geometry silhouette, shader palette & technical lore from image/description"
    ),
    GENERATE_VARIANTS(
        title = "Generate Multi-Variants",
        subtitle = "Synthesizes 4 playable 3D & 2D studio variants with distinct palettes & attachments"
    ),
    AI_IMAGE_SYNTHESIS(
        title = "AI Visual Image Render",
        subtitle = "Uses gemini-nano-banana-2.1 to render or edit a concept/sprite image into 3D & 2D"
    )
}

data class GeminiAssetVariant(
    val id: String,
    val variantName: String,
    val variantBadge: String,
    val characterTag: String,
    val technicalBreakdown: String,
    val loreDescription: String,
    val archetype: String,
    val primaryHex: String,
    val secondaryHex: String,
    val accentHex: String,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float,
    val addWingsOrFins: Boolean,
    val addOrbitalRing: Boolean,
    val mesh: Mesh3D,
    val spriteSheet: SpriteSheetData,
    val palette: List<ExtractedColor>
)

data class GeminiForgeReport(
    val operationMode: GeminiOperationMode,
    val enhancedTitle: String,
    val characterIdentity: String,
    val technicalEnhancementSummary: String,
    val enhancedLoreSpec: String,
    val primaryMesh: Mesh3D,
    val primarySpriteSheet: SpriteSheetData,
    val enhancedPalette: List<ExtractedColor>,
    val variants: List<GeminiAssetVariant>,
    val aiSynthesizedBitmap: Bitmap? = null,
    val usedImageInput: Boolean = false,
    val modelNameUsed: String = "gemini-3.8-flash"
)

sealed interface GeminiForgeUiState {
    data object Idle : GeminiForgeUiState
    data class Loading(val progressLabel: String) : GeminiForgeUiState
    data class Success(val report: GeminiForgeReport) : GeminiForgeUiState
    data class Error(val message: String) : GeminiForgeUiState
}

data class GenerationResult(
    val title: String,
    val loreDescription: String,
    val mesh: Mesh3D? = null,
    val spriteSheet: SpriteSheetData? = null
)

object GeminiAssetForge {

    const val ARCADE_COMIC_CEL_STYLE_PRESET_TITLE: String = "32-Bit Arcade Comic-Cel Fusion"
    const val ARCADE_COMIC_CEL_STYLE_BLOCK: String =
        "32-bit arcade & 2D comic-cel style: punchy high-contrast ramps, crisp edges, clearcoat armor/visors w/ glints vs matte skin/fabric, bold black ink, hard 2-step cel-shading, ink-pool shadows w/ ordered dithering & halftones, anime rim-light, glowing Mana Circuits, cartoon color-blocking. No 3D, photorealism, blur, airbrush, text, grids."

    /**
     * Structured JSON Schema for `gemini-3.8-flash` asset enhancement and multi-variant generation.
     */
    private val variantItemSchema: Schema = Schema.obj(
        mapOf(
            "variantName" to Schema.string("Distinct studio name for this generated variant"),
            "variantBadge" to Schema.string("Short uppercase role tag such as OVERCLOCKED, ELEMENTAL, BOSS FORM, STEALTH, or RETRO 16-BIT"),
            "technicalBreakdown" to Schema.string("Detailed explanation of how geometry, silhouette, and color quantization are modified for this variant"),
            "loreDescription" to Schema.string("Vivid in-universe lore and combat/gameplay role description"),
            "archetype" to Schema.enumeration(
                listOf("STARFIGHTER", "CYBER_MECH", "TREASURE_CHEST", "CRYSTAL_ARTIFACT", "VOXEL_RELIEF"),
                "Base 3D structural archetype best matching this variant"
            ),
            "primaryHex" to Schema.string("Primary surface hex color #RRGGBB"),
            "secondaryHex" to Schema.string("Secondary armor/chassis hex color #RRGGBB"),
            "accentHex" to Schema.string("Emissive energy/highlight hex color #RRGGBB"),
            "scaleX" to Schema.double("Horizontal X silhouette scale multiplier between 0.65 and 1.55"),
            "scaleY" to Schema.double("Vertical Y silhouette scale multiplier between 0.65 and 1.55"),
            "scaleZ" to Schema.double("Depth Z silhouette scale multiplier between 0.65 and 1.55"),
            "addWingsOrFins" to Schema.boolean("Whether to attach swept thruster wings or stabilizer fins to the 3D mesh"),
            "addOrbitalRing" to Schema.boolean("Whether to attach an orbital energy halo ring around the 3D mesh")
        )
    )

    private val assetForgeResponseSchema: Schema = Schema.obj(
        mapOf(
            "enhancedTitle" to Schema.string("Polished studio title for the enhanced asset"),
            "characterIdentity" to Schema.string("Detected or synthesized named character or entity tag (1-2 words)"),
            "technicalEnhancementSummary" to Schema.string("Vivid technical summary detailing how Gemini enhanced the asset's 3D vertex/face silhouette, sub-pixel sprite readability, and hardware palette harmony"),
            "enhancedLoreSpec" to Schema.string("Rich narrative and art-direction specification for the enhanced asset"),
            "recommendedArchetype" to Schema.enumeration(
                listOf("STARFIGHTER", "CYBER_MECH", "TREASURE_CHEST", "CRYSTAL_ARTIFACT", "VOXEL_RELIEF"),
                "Recommended 3D structural archetype"
            ),
            "geometryScaleX" to Schema.double("Primary mesh X scale factor between 0.7 and 1.45"),
            "geometryScaleY" to Schema.double("Primary mesh Y scale factor between 0.7 and 1.45"),
            "geometryScaleZ" to Schema.double("Primary mesh Z scale factor between 0.7 and 1.45"),
            "extrusionDepthBoost" to Schema.double("Depth extrusion boost factor between 0.35 and 1.15"),
            "addWingsOrFins" to Schema.boolean("Attach aerodynamic/energy fins to primary enhanced 3D mesh"),
            "addOrbitalRing" to Schema.boolean("Attach orbital runic/energy ring to primary enhanced 3D mesh"),
            "paletteHexColors" to Schema.array(
                Schema.string("Hex color string in #RRGGBB format"),
                "6 harmonized hex colors optimized for high-contrast game rendering"
            ),
            "variants" to Schema.array(
                variantItemSchema,
                "List of 3 to 4 distinct asset variants with unique silhouettes, palettes, and roles"
            )
        )
    )

    /**
     * Lazy initialization ensures `Firebase.ai` is only accessed after `configureAppCheck(this, intent)`
     * has executed in `MainActivity.onCreate()`.
     */
    private val structuredForgeModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = assetForgeResponseSchema
                temperature = 0.75f
            }
        )
    }

    private val imageSynthesisModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    /**
     * Downscales large bitmaps before multimodal inference so payloads stay compact and fast.
     */
    fun scaleBitmapForInference(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / max(width, height).toFloat()
        val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    /**
     * Sends an imported asset image (concept art, spritesheet, photo, or UV texture) AND/OR
     * an asset description to `gemini-3.8-flash` for Automatic Asset Enhancement or Multi-Variant Generation.
     */
    suspend fun enhanceOrGenerateVariantsWithGemini(
        promptDescription: String,
        customInstruction: String,
        inputBitmap: Bitmap?,
        inputSource: GeminiInputImageSource,
        operationMode: GeminiOperationMode,
        theme: ArtTheme,
        hardwareName: String,
        baseMesh: Mesh3D,
        baseSpriteSheet: SpriteSheetData,
        explicitCharacterTag: String,
        sharedContextBlock: String = ""
    ): GeminiForgeReport {
        val effectivePrompt = buildString {
            appendLine("You are the PixelForge Studio Lead Technical Artist & Game Asset Architect.")
            if (operationMode == GeminiOperationMode.ENHANCE_ASSET) {
                appendLine("TASK: Perform AUTOMATIC ASSET ENHANCEMENT on the provided game asset (image and/or description).")
                appendLine("Analyze the silhouette, proportions, shading contrast, and color harmony. Produce an upgraded primary 3D/2D specification plus 3 complementary studio variants.")
            } else {
                appendLine("TASK: Perform AUTOMATIC MULTI-VARIANT GENERATION from the provided game asset (image and/or description).")
                appendLine("Design 4 distinct, playable game asset variants (such as Overclocked Boss Form, Elemental Infusion, Stealth Spec-Ops, and Retro Silicon-Adapted) with distinct 3D geometry modifiers, structural attachments, and high-contrast hex palettes.")
            }
            appendLine("Art Theme: ${theme.displayName}")
            if (theme.styleDirective.isNotBlank()) {
                appendLine("Mandatory Visual Style Directive: ${theme.styleDirective}")
            }
            appendLine("Target Console Silicon Profile: $hardwareName")
            appendLine("Current Active 3D Mesh: '${baseMesh.name}' (${baseMesh.vertexCount} vertices, ${baseMesh.faceCount} faces, format: ${baseMesh.sourceFormat})")
            appendLine("Current Active 2D Spritesheet: '${baseSpriteSheet.name}' (${baseSpriteSheet.columns}x${baseSpriteSheet.rows} grid, ${baseSpriteSheet.frames.size} frames)")
            if (explicitCharacterTag.isNotBlank()) {
                appendLine("Named Character Tag: $explicitCharacterTag")
            }
            if (promptDescription.isNotBlank()) {
                appendLine("Asset Description / Lore: $promptDescription")
            }
            if (customInstruction.isNotBlank()) {
                appendLine("Specific Enhancement / Variant Direction: $customInstruction")
            }
            if (inputBitmap != null && inputSource != GeminiInputImageSource.DESCRIPTION_ONLY) {
                appendLine("Attached Visual Asset Source: ${inputSource.label} (${inputBitmap.width}x${inputBitmap.height}px). Inspect its colors, forms, silhouette, and visual details closely.")
            }
            appendLine("Return strict JSON with keys: enhancedTitle, characterIdentity, technicalEnhancementSummary, enhancedLoreSpec, recommendedArchetype (STARFIGHTER|CYBER_MECH|TREASURE_CHEST|CRYSTAL_ARTIFACT|VOXEL_RELIEF), geometryScaleX, geometryScaleY, geometryScaleZ, extrusionDepthBoost, addWingsOrFins, addOrbitalRing, paletteHexColors (array of 6 #RRGGBB strings), and variants (array of 3-4 objects with variantName, variantBadge, technicalBreakdown, loreDescription, archetype, primaryHex, secondaryHex, accentHex, scaleX, scaleY, scaleZ, addWingsOrFins, addOrbitalRing).")
        }

        val scaledBitmap = if (inputBitmap != null && inputSource != GeminiInputImageSource.DESCRIPTION_ONLY) {
            scaleBitmapForInference(inputBitmap, maxDimension = 1024)
        } else {
            null
        }

        val omniResponse = OmniAiRouter.generateTextOrJson(
            role = AiTaskRole.TEXT_AND_SPECS,
            prompt = effectivePrompt,
            systemInstruction = "You are PixelForge Studio Lead Technical Artist. Always return valid JSON only.",
            imageBitmap = scaledBitmap,
            expectJson = true,
            sharedContextBlock = sharedContextBlock,
            firebaseDelegate = { combinedPrompt, bmp ->
                val response = withTimeout(60_000L) {
                    if (bmp != null) {
                        structuredForgeModel.generateContent(
                            content {
                                image(bmp)
                                text(combinedPrompt)
                            }
                        )
                    } else {
                        structuredForgeModel.generateContent(combinedPrompt)
                    }
                }
                response.text?.trim()
                    ?: throw IllegalStateException("Gemini returned an empty response.")
            }
        )

        val rawJson = omniResponse.text.trim()
        if (rawJson.isEmpty()) {
            throw IllegalStateException("${omniResponse.providerUsed.defaultDisplayName} returned an empty response.")
        }

        val resolvedModelLabel = if (omniResponse.providerUsed == AiProviderType.FIREBASE_STUDIO) {
            "gemini-3.8-flash"
        } else {
            "${omniResponse.providerUsed.name}:${omniResponse.modelIdUsed}"
        }

        return parseStructuredForgeResponse(
            rawJson = rawJson,
            operationMode = operationMode,
            inputBitmap = if (inputSource != GeminiInputImageSource.DESCRIPTION_ONLY) inputBitmap else null,
            baseMesh = baseMesh,
            baseSpriteSheet = baseSpriteSheet,
            fallbackCharacterTag = explicitCharacterTag.ifBlank { "Kaelen" }
        ).copy(modelNameUsed = resolvedModelLabel)
    }

    /**
     * Uses `gemini-nano-banana-2.1` to synthesize or edit a game asset image at runtime from an imported
     * asset image + edit instruction (image-to-image) or from a text description (text-to-image),
     * and automatically builds matching 3D Relief Mesh, 2D Spritesheet, and studio variants.
     */
    suspend fun synthesizeAiVariantImageWithGemini(
        promptDescription: String,
        customInstruction: String,
        inputBitmap: Bitmap?,
        inputSource: GeminiInputImageSource,
        theme: ArtTheme,
        hardwareName: String,
        explicitCharacterTag: String,
        sharedContextBlock: String = ""
    ): GeminiForgeReport {
        val imagePrompt = buildString {
            append("Create a crisp, high-contrast video game character or vehicle asset sprite/concept illustration ")
            append("in ${theme.displayName} style, optimized for $hardwareName aesthetics, centered on a clean dark background. ")
            if (theme.styleDirective.isNotBlank()) {
                append("${theme.styleDirective} ")
            }
            if (explicitCharacterTag.isNotBlank()) {
                append("Character/Entity: $explicitCharacterTag. ")
            }
            if (promptDescription.isNotBlank()) {
                append("Asset description: $promptDescription. ")
            }
            if (customInstruction.isNotBlank()) {
                append("Enhancement / Variant modification: $customInstruction.")
            }
        }

        val scaledRef = if (inputBitmap != null && inputSource != GeminiInputImageSource.DESCRIPTION_ONLY) {
            scaleBitmapForInference(inputBitmap, maxDimension = 1024)
        } else {
            null
        }

        val omniImage = OmniAiRouter.generateImageBitmap(
            prompt = imagePrompt,
            referenceBitmap = scaledRef,
            sharedContextBlock = sharedContextBlock,
            firebaseImageDelegate = { combinedPrompt, refBmp ->
                withTimeout(60_000L) {
                    val response = if (refBmp != null) {
                        imageSynthesisModel.generateContent(
                            content {
                                image(refBmp)
                                text(combinedPrompt)
                            }
                        )
                    } else {
                        imageSynthesisModel.generateContent(combinedPrompt)
                    }
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                } ?: throw IllegalStateException("No image was returned by gemini-nano-banana-2.1.")
            }
        )

        val generatedBitmap = omniImage.bitmap
        val resolvedModelLabel = if (omniImage.providerUsed == AiProviderType.FIREBASE_STUDIO) {
            "gemini-nano-banana-2.1"
        } else {
            "${omniImage.providerUsed.name}:${omniImage.modelIdUsed}"
        }

        val charName = explicitCharacterTag.ifBlank { "Kaelen" }
        val enhancedTitle = "$charName • AI Visual Synthesis"
        val extractedPalette = ConceptAnalysis.extractPalette(generatedBitmap, maxColors = 6)
        val primaryColor = extractedPalette.getOrNull(0)?.hex ?: "#00E5FF"
        val secondaryColor = extractedPalette.getOrNull(1)?.hex ?: "#1E293B"
        val accentColor = extractedPalette.getOrNull(2)?.hex ?: "#FF2A85"

        val reliefMesh = ProceduralMeshes.createVoxelReliefFromBitmap(
            bitmap = generatedBitmap,
            targetGrid = 28,
            depthScale = 0.65f,
            name = "$enhancedTitle [3D Relief]",
            sourceKindLabel = "GEMINI AI",
            sourceSizeLabel = "${generatedBitmap.width}×${generatedBitmap.height}px"
        )
        val spriteBmp = ConceptAnalysis.convertConceptToSprite(generatedBitmap, resolution = 32, addOutline = true)
        val synthesizedSheet = SpriteSheetData.fromBitmap(
            bitmap = spriteBmp,
            name = "$enhancedTitle [Sprite]",
            columns = 1,
            rows = 1,
            sourceKindLabel = "GEMINI AI",
            sourceSizeLabel = "${generatedBitmap.width}×${generatedBitmap.height}px"
        )

        val variants = listOf(
            buildVariantFromParameters(
                index = 0,
                variantName = "$charName • AI Relief Primary",
                variantBadge = "AI RENDER",
                characterTag = charName,
                technicalBreakdown = "Direct 28×28 luminance depth extrusion and 32px outlined sprite synthesis from $resolvedModelLabel output.",
                loreDescription = "Synthesized visual concept variant for $promptDescription.",
                archetype = "VOXEL_RELIEF",
                primaryHex = primaryColor,
                secondaryHex = secondaryColor,
                accentHex = accentColor,
                scaleX = 1.0f,
                scaleY = 1.0f,
                scaleZ = 1.0f,
                addWingsOrFins = false,
                addOrbitalRing = false,
                inputBitmap = generatedBitmap,
                baseMesh = reliefMesh,
                baseSpriteSheet = synthesizedSheet
            ),
            buildVariantFromParameters(
                index = 1,
                variantName = "$charName • Overclocked Winged Form",
                variantBadge = "OVERCLOCKED",
                characterTag = charName,
                technicalBreakdown = "Adds swept plasma wings and orbital halo ring to the AI-synthesized relief mesh with boosted Z-depth.",
                loreDescription = "Awakened high-output combat variant derived from the AI visual synthesis.",
                archetype = "VOXEL_RELIEF",
                primaryHex = accentColor,
                secondaryHex = secondaryColor,
                accentHex = primaryColor,
                scaleX = 1.15f,
                scaleY = 1.08f,
                scaleZ = 1.25f,
                addWingsOrFins = true,
                addOrbitalRing = true,
                inputBitmap = generatedBitmap,
                baseMesh = reliefMesh,
                baseSpriteSheet = synthesizedSheet
            )
        )

        return GeminiForgeReport(
            operationMode = GeminiOperationMode.AI_IMAGE_SYNTHESIS,
            enhancedTitle = enhancedTitle,
            characterIdentity = charName,
            technicalEnhancementSummary = "Synthesized a ${generatedBitmap.width}×${generatedBitmap.height}px visual asset via $resolvedModelLabel, extracted ${extractedPalette.size} dominant shader swatches, extruded a ${reliefMesh.vertexCount}-vertex 3D relief mesh, and quantized an outlined 2D sprite.",
            enhancedLoreSpec = "Generated from prompt: \"$promptDescription\" ${if (customInstruction.isNotBlank()) "with instruction: \"$customInstruction\"" else ""}.",
            primaryMesh = reliefMesh,
            primarySpriteSheet = synthesizedSheet,
            enhancedPalette = extractedPalette,
            variants = variants,
            aiSynthesizedBitmap = generatedBitmap,
            usedImageInput = inputBitmap != null && inputSource != GeminiInputImageSource.DESCRIPTION_ONLY,
            modelNameUsed = resolvedModelLabel
        )
    }

    /**
     * Parses the structured JSON returned by `gemini-3.8-flash` into concrete 3D Meshes, 2D Spritesheets,
     * palette swatches, and inspectable `GeminiAssetVariant` items.
     */
    internal fun parseStructuredForgeResponse(
        rawJson: String,
        operationMode: GeminiOperationMode,
        inputBitmap: Bitmap?,
        baseMesh: Mesh3D,
        baseSpriteSheet: SpriteSheetData,
        fallbackCharacterTag: String
    ): GeminiForgeReport {
        val root = JSONObject(rawJson)
        val enhancedTitle = root.optString("enhancedTitle", "Enhanced ${baseMesh.name}").ifBlank { "Enhanced ${baseMesh.name}" }
        val characterIdentity = root.optString("characterIdentity", fallbackCharacterTag).ifBlank { fallbackCharacterTag }
        val technicalSummary = root.optString(
            "technicalEnhancementSummary",
            "Gemini analyzed silhouette proportions, vertex normals, and hardware color quantization."
        )
        val enhancedLore = root.optString(
            "enhancedLoreSpec",
            "Studio-grade enhanced game asset specification."
        )
        val recommendedArchetype = root.optString("recommendedArchetype", "CYBER_MECH")
        val scaleX = root.optDouble("geometryScaleX", 1.0).toFloat().coerceIn(0.6f, 1.6f)
        val scaleY = root.optDouble("geometryScaleY", 1.0).toFloat().coerceIn(0.6f, 1.6f)
        val scaleZ = root.optDouble("geometryScaleZ", 1.0).toFloat().coerceIn(0.6f, 1.6f)
        val depthBoost = root.optDouble("extrusionDepthBoost", 0.65).toFloat().coerceIn(0.25f, 1.3f)
        val addWings = root.optBoolean("addWingsOrFins", true)
        val addRing = root.optBoolean("addOrbitalRing", false)

        val paletteArray = root.optJSONArray("paletteHexColors")
        val hexList = mutableListOf<String>()
        if (paletteArray != null) {
            for (i in 0 until paletteArray.length()) {
                val hex = normalizeHexColor(paletteArray.optString(i, "#00E5FF"))
                hexList.add(hex)
            }
        }
        if (hexList.size < 3) {
            hexList.addAll(listOf("#00E5FF", "#FF2A85", "#F59E0B", "#10B981", "#38BDF8", "#1E293B"))
        }

        val extractedPalette = hexList.take(8).mapIndexed { index, hex ->
            val colorInt = parseColorIntSafe(hex)
            val pct = (100f / hexList.size.coerceAtLeast(1))
            ExtractedColor(colorInt = colorInt, hex = hex, percentage = pct)
        }

        val primaryHex = hexList.getOrElse(0) { "#00E5FF" }
        val secondaryHex = hexList.getOrElse(1) { "#1E293B" }
        val accentHex = hexList.getOrElse(2) { "#FF2A85" }

        val seedMesh = selectBaseMeshForEnhancement(
            archetype = recommendedArchetype,
            inputBitmap = inputBitmap,
            currentMesh = baseMesh,
            depthBoost = depthBoost,
            title = enhancedTitle
        )

        val enhancedPrimaryMesh = buildEnhancedVariantMesh(
            baseMesh = seedMesh,
            variantName = enhancedTitle,
            primaryColorLong = parseColorLongSafe(primaryHex),
            secondaryColorLong = parseColorLongSafe(secondaryHex),
            accentColorLong = parseColorLongSafe(accentHex),
            scaleX = scaleX,
            scaleY = scaleY,
            scaleZ = scaleZ,
            addWingsOrFins = addWings,
            addOrbitalRing = addRing
        )

        val enhancedPrimarySprite = buildEnhancedVariantSpriteSheet(
            variantName = enhancedTitle,
            inputBitmap = inputBitmap,
            baseSpriteSheet = baseSpriteSheet,
            primaryColorInt = parseColorIntSafe(primaryHex),
            secondaryColorInt = parseColorIntSafe(secondaryHex),
            accentColorInt = parseColorIntSafe(accentHex),
            variantIndex = 0
        )

        val variantsJson = root.optJSONArray("variants")
        val builtVariants = mutableListOf<GeminiAssetVariant>()
        if (variantsJson != null) {
            for (i in 0 until variantsJson.length()) {
                val vObj = variantsJson.optJSONObject(i) ?: continue
                val vName = vObj.optString("variantName", "$enhancedTitle Variant ${i + 1}")
                val vBadge = vObj.optString("variantBadge", "VARIANT ${i + 1}").uppercase()
                val vTech = vObj.optString("technicalBreakdown", "Custom silhouette and shader palette adaptation.")
                val vLore = vObj.optString("loreDescription", "Specialized tactical variant for $characterIdentity.")
                val vArch = vObj.optString("archetype", recommendedArchetype)
                val vPri = normalizeHexColor(vObj.optString("primaryHex", primaryHex))
                val vSec = normalizeHexColor(vObj.optString("secondaryHex", secondaryHex))
                val vAcc = normalizeHexColor(vObj.optString("accentHex", accentHex))
                val vSx = vObj.optDouble("scaleX", 1.0).toFloat().coerceIn(0.6f, 1.6f)
                val vSy = vObj.optDouble("scaleY", 1.0).toFloat().coerceIn(0.6f, 1.6f)
                val vSz = vObj.optDouble("scaleZ", 1.0).toFloat().coerceIn(0.6f, 1.6f)
                val vWings = vObj.optBoolean("addWingsOrFins", i % 2 == 0)
                val vRing = vObj.optBoolean("addOrbitalRing", i % 2 == 1)

                builtVariants.add(
                    buildVariantFromParameters(
                        index = i,
                        variantName = vName,
                        variantBadge = vBadge,
                        characterTag = characterIdentity,
                        technicalBreakdown = vTech,
                        loreDescription = vLore,
                        archetype = vArch,
                        primaryHex = vPri,
                        secondaryHex = vSec,
                        accentHex = vAcc,
                        scaleX = vSx,
                        scaleY = vSy,
                        scaleZ = vSz,
                        addWingsOrFins = vWings,
                        addOrbitalRing = vRing,
                        inputBitmap = inputBitmap,
                        baseMesh = baseMesh,
                        baseSpriteSheet = baseSpriteSheet
                    )
                )
            }
        }

        if (builtVariants.isEmpty()) {
            builtVariants.add(
                GeminiAssetVariant(
                    id = "var_primary_${System.currentTimeMillis()}",
                    variantName = enhancedTitle,
                    variantBadge = "ENHANCED",
                    characterTag = characterIdentity,
                    technicalBreakdown = technicalSummary,
                    loreDescription = enhancedLore,
                    archetype = recommendedArchetype,
                    primaryHex = primaryHex,
                    secondaryHex = secondaryHex,
                    accentHex = accentHex,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    scaleZ = scaleZ,
                    addWingsOrFins = addWings,
                    addOrbitalRing = addRing,
                    mesh = enhancedPrimaryMesh,
                    spriteSheet = enhancedPrimarySprite,
                    palette = extractedPalette
                )
            )
        }

        return GeminiForgeReport(
            operationMode = operationMode,
            enhancedTitle = enhancedTitle,
            characterIdentity = characterIdentity,
            technicalEnhancementSummary = technicalSummary,
            enhancedLoreSpec = enhancedLore,
            primaryMesh = enhancedPrimaryMesh,
            primarySpriteSheet = enhancedPrimarySprite,
            enhancedPalette = extractedPalette,
            variants = builtVariants,
            aiSynthesizedBitmap = null,
            usedImageInput = inputBitmap != null,
            modelNameUsed = "gemini-3.8-flash"
        )
    }

    private fun buildVariantFromParameters(
        index: Int,
        variantName: String,
        variantBadge: String,
        characterTag: String,
        technicalBreakdown: String,
        loreDescription: String,
        archetype: String,
        primaryHex: String,
        secondaryHex: String,
        accentHex: String,
        scaleX: Float,
        scaleY: Float,
        scaleZ: Float,
        addWingsOrFins: Boolean,
        addOrbitalRing: Boolean,
        inputBitmap: Bitmap?,
        baseMesh: Mesh3D,
        baseSpriteSheet: SpriteSheetData
    ): GeminiAssetVariant {
        val seedMesh = selectBaseMeshForEnhancement(
            archetype = archetype,
            inputBitmap = inputBitmap,
            currentMesh = baseMesh,
            depthBoost = 0.55f + (index * 0.1f),
            title = variantName
        )
        val mesh = buildEnhancedVariantMesh(
            baseMesh = seedMesh,
            variantName = variantName,
            primaryColorLong = parseColorLongSafe(primaryHex),
            secondaryColorLong = parseColorLongSafe(secondaryHex),
            accentColorLong = parseColorLongSafe(accentHex),
            scaleX = scaleX,
            scaleY = scaleY,
            scaleZ = scaleZ,
            addWingsOrFins = addWingsOrFins,
            addOrbitalRing = addOrbitalRing
        )
        val sprite = buildEnhancedVariantSpriteSheet(
            variantName = variantName,
            inputBitmap = inputBitmap,
            baseSpriteSheet = baseSpriteSheet,
            primaryColorInt = parseColorIntSafe(primaryHex),
            secondaryColorInt = parseColorIntSafe(secondaryHex),
            accentColorInt = parseColorIntSafe(accentHex),
            variantIndex = index + 1
        )
        val palette = listOf(
            ExtractedColor(parseColorIntSafe(primaryHex), primaryHex, 45f),
            ExtractedColor(parseColorIntSafe(secondaryHex), secondaryHex, 35f),
            ExtractedColor(parseColorIntSafe(accentHex), accentHex, 20f)
        )
        return GeminiAssetVariant(
            id = "gemini_var_${index}_${System.currentTimeMillis()}",
            variantName = variantName,
            variantBadge = variantBadge,
            characterTag = characterTag,
            technicalBreakdown = technicalBreakdown,
            loreDescription = loreDescription,
            archetype = archetype,
            primaryHex = primaryHex,
            secondaryHex = secondaryHex,
            accentHex = accentHex,
            scaleX = scaleX,
            scaleY = scaleY,
            scaleZ = scaleZ,
            addWingsOrFins = addWingsOrFins,
            addOrbitalRing = addOrbitalRing,
            mesh = mesh,
            spriteSheet = sprite,
            palette = palette
        )
    }

    private fun selectBaseMeshForEnhancement(
        archetype: String,
        inputBitmap: Bitmap?,
        currentMesh: Mesh3D,
        depthBoost: Float,
        title: String
    ): Mesh3D {
        if (inputBitmap != null && archetype.equals("VOXEL_RELIEF", ignoreCase = true)) {
            return ProceduralMeshes.createVoxelReliefFromBitmap(
                bitmap = inputBitmap,
                targetGrid = 26,
                depthScale = depthBoost,
                name = title,
                sourceKindLabel = "GEMINI ENHANCED",
                sourceSizeLabel = "${inputBitmap.width}×${inputBitmap.height}px"
            )
        }
        // If user has an imported custom mesh with high vertex count, preserve its base topology!
        if (currentMesh.sourceKindLabel != "STUDIO" && currentMesh.vertexCount > 0) {
            return currentMesh
        }
        return when (archetype.uppercase()) {
            "STARFIGHTER" -> ProceduralMeshes.createSciFiStarfighter()
            "CYBER_MECH" -> ProceduralMeshes.createCyberMech()
            "TREASURE_CHEST" -> ProceduralMeshes.createTreasureChest()
            "CRYSTAL_ARTIFACT" -> ProceduralMeshes.createCrystalArtifact()
            "VOXEL_RELIEF" -> {
                if (inputBitmap != null) {
                    ProceduralMeshes.createVoxelReliefFromBitmap(
                        bitmap = inputBitmap,
                        targetGrid = 26,
                        depthScale = depthBoost,
                        name = title,
                        sourceKindLabel = "GEMINI ENHANCED"
                    )
                } else {
                    currentMesh
                }
            }
            else -> currentMesh
        }
    }

    /**
     * Applies Gemini's non-uniform silhouette scaling, hex color palette remapping, and optional
     * 3D structural attachments (swept energy wings/fins and tilted orbital halo rings) to a `Mesh3D`.
     */
    fun buildEnhancedVariantMesh(
        baseMesh: Mesh3D,
        variantName: String,
        primaryColorLong: Long,
        secondaryColorLong: Long,
        accentColorLong: Long,
        scaleX: Float,
        scaleY: Float,
        scaleZ: Float,
        addWingsOrFins: Boolean,
        addOrbitalRing: Boolean
    ): Mesh3D {
        val vertices = ArrayList<Vector3>(baseMesh.vertices.size + 48)
        for (v in baseMesh.vertices) {
            vertices.add(Vector3(v.x * scaleX, v.y * scaleY, v.z * scaleZ))
        }

        val faces = ArrayList<Face3D>(baseMesh.faces.size + 48)
        baseMesh.faces.forEachIndexed { idx, face ->
            val mappedColor = when {
                idx % 5 == 0 -> accentColorLong
                idx % 2 == 0 -> primaryColorLong
                else -> secondaryColorLong
            }
            faces.add(face.copy(color = mappedColor))
        }

        if (addWingsOrFins) {
            val baseIdx = vertices.size
            // Left & right swept energy stabilizer wings
            vertices.add(Vector3(-0.35f, 0.25f, 0.1f))   // 0
            vertices.add(Vector3(-1.45f, 0.45f, -0.35f)) // 1
            vertices.add(Vector3(-1.25f, 0.05f, -0.65f)) // 2
            vertices.add(Vector3(-0.35f, -0.1f, -0.25f)) // 3

            vertices.add(Vector3(0.35f, 0.25f, 0.1f))    // 4
            vertices.add(Vector3(1.45f, 0.45f, -0.35f))  // 5
            vertices.add(Vector3(1.25f, 0.05f, -0.65f))  // 6
            vertices.add(Vector3(0.35f, -0.1f, -0.25f))  // 7

            faces.add(Face3D(intArrayOf(baseIdx, baseIdx + 1, baseIdx + 2), color = accentColorLong))
            faces.add(Face3D(intArrayOf(baseIdx, baseIdx + 2, baseIdx + 3), color = primaryColorLong))
            faces.add(Face3D(intArrayOf(baseIdx + 4, baseIdx + 6, baseIdx + 5), color = accentColorLong))
            faces.add(Face3D(intArrayOf(baseIdx + 4, baseIdx + 7, baseIdx + 6), color = primaryColorLong))
        }

        if (addOrbitalRing) {
            val ringSegments = 12
            val ringRadius = 1.18f
            val ringThickness = 0.06f
            val ringStart = vertices.size
            for (i in 0 until ringSegments) {
                val angle = i * (2f * Math.PI.toFloat() / ringSegments)
                val rx = cos(angle) * ringRadius
                val rz = sin(angle) * ringRadius
                val ry = sin(angle * 2f) * 0.18f
                vertices.add(Vector3(rx, ry + ringThickness, rz))
                vertices.add(Vector3(rx, ry - ringThickness, rz))
            }
            for (i in 0 until ringSegments) {
                val i0 = ringStart + (i * 2)
                val i1 = ringStart + (i * 2 + 1)
                val next = (i + 1) % ringSegments
                val i2 = ringStart + (next * 2)
                val i3 = ringStart + (next * 2 + 1)
                val segColor = if (i % 2 == 0) accentColorLong else primaryColorLong
                faces.add(Face3D(intArrayOf(i0, i2, i3), color = segColor))
                faces.add(Face3D(intArrayOf(i0, i3, i1), color = segColor))
            }
        }

        return Mesh3D.normalizeMesh(
            name = variantName,
            rawVertices = vertices,
            faces = faces,
            uvs = baseMesh.uvs,
            materials = baseMesh.materials,
            sourceFormat = "Gemini AI Enhanced (${baseMesh.sourceFormat})",
            sourceKindLabel = "GEMINI AI",
            rawVertexCountOverride = vertices.size,
            rawFaceCountOverride = faces.size,
            sourceSizeLabel = baseMesh.sourceSizeLabel
        )
    }

    /**
     * Builds an enhanced 2D `SpriteSheetData` for a variant. If an imported image or spritesheet is present,
     * remaps its luminance/clusters toward Gemini's variant palette and adds animated energy particles;
     * otherwise synthesizes a 4x2 multi-frame sheet with Gemini's palette.
     */
    fun buildEnhancedVariantSpriteSheet(
        variantName: String,
        inputBitmap: Bitmap?,
        baseSpriteSheet: SpriteSheetData,
        primaryColorInt: Int,
        secondaryColorInt: Int,
        accentColorInt: Int,
        variantIndex: Int
    ): SpriteSheetData {
        val sourceFrame = inputBitmap ?: baseSpriteSheet.frames.firstOrNull()
        if (sourceFrame != null) {
            val fw = 32
            val fh = 32
            val cols = 4
            val rows = 2
            val scaledBase = Bitmap.createScaledBitmap(sourceFrame, fw, fh, false)
            val sheetBmp = Bitmap.createBitmap(fw * cols, fh * rows, Bitmap.Config.ARGB_8888)

            val pR = Color.red(primaryColorInt)
            val pG = Color.green(primaryColorInt)
            val pB = Color.blue(primaryColorInt)
            val sR = Color.red(secondaryColorInt)
            val sG = Color.green(secondaryColorInt)
            val sB = Color.blue(secondaryColorInt)
            val aR = Color.red(accentColorInt)
            val aG = Color.green(accentColorInt)
            val aB = Color.blue(accentColorInt)

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val ox = c * fw
                    val oy = r * fh
                    val bounceY = if (c % 2 == 1) -1 else 0

                    for (y in 0 until fh) {
                        for (x in 0 until fw) {
                            val srcY = (y - bounceY).coerceIn(0, fh - 1)
                            val px = scaledBase.getPixel(x, srcY)
                            val alpha = Color.alpha(px)
                            if (alpha < 35) continue

                            val origR = Color.red(px)
                            val origG = Color.green(px)
                            val origB = Color.blue(px)
                            val lum = (0.299f * origR + 0.587f * origG + 0.114f * origB) / 255f

                            // Blend original pixel with Gemini's variant palette based on luminance
                            val (tR, tG, tB) = when {
                                lum > 0.72f -> Triple(aR, aG, aB)
                                lum > 0.38f -> Triple(pR, pG, pB)
                                else -> Triple(sR, sG, sB)
                            }
                            val mixR = ((origR * 0.4f) + (tR * 0.6f)).toInt().coerceIn(0, 255)
                            val mixG = ((origG * 0.4f) + (tG * 0.6f)).toInt().coerceIn(0, 255)
                            val mixB = ((origB * 0.4f) + (tB * 0.6f)).toInt().coerceIn(0, 255)
                            sheetBmp.setPixel(ox + x, oy + y, Color.argb(alpha, mixR, mixG, mixB))
                        }
                    }

                    // Variant aura / weapon pulse pixels per animation frame
                    val pulseX = (ox + 4 + (c * 6 + variantIndex * 3) % (fw - 8)).coerceIn(ox, ox + fw - 1)
                    val pulseY = (oy + 4 + (r * 10 + c * 4) % (fh - 8)).coerceIn(oy, oy + fh - 1)
                    sheetBmp.setPixel(pulseX, pulseY, accentColorInt)
                    if (pulseX + 1 < ox + fw) sheetBmp.setPixel(pulseX + 1, pulseY, primaryColorInt)
                }
            }

            val upscaled = Bitmap.createScaledBitmap(sheetBmp, fw * cols * 2, fh * rows * 2, false)
            return SpriteSheetData.fromBitmap(
                bitmap = upscaled,
                name = variantName,
                columns = cols,
                rows = rows,
                sourceKindLabel = "GEMINI AI",
                sourceSizeLabel = "${upscaled.width}×${upscaled.height}px"
            )
        }

        return generateParameterizedSpriteSheet(
            title = variantName,
            primaryColor = primaryColorInt,
            secondaryColor = secondaryColorInt,
            highlightColor = accentColorInt
        )
    }

    fun generateAssetFromDescription(
        prompt: String,
        target: AssetTarget,
        theme: ArtTheme
    ): GenerationResult {
        val cleanPrompt = prompt.lowercase().trim()

        if (target == AssetTarget.MODEL_3D) {
            val mesh: Mesh3D = when {
                cleanPrompt.contains("ship") || cleanPrompt.contains("plane") || cleanPrompt.contains("star") || cleanPrompt.contains("fighter") -> {
                    ProceduralMeshes.createSciFiStarfighter()
                }
                cleanPrompt.contains("mech") || cleanPrompt.contains("robot") || cleanPrompt.contains("warrior") || cleanPrompt.contains("knight") -> {
                    ProceduralMeshes.createCyberMech()
                }
                cleanPrompt.contains("chest") || cleanPrompt.contains("box") || cleanPrompt.contains("mimic") || cleanPrompt.contains("loot") -> {
                    ProceduralMeshes.createTreasureChest()
                }
                cleanPrompt.contains("crystal") || cleanPrompt.contains("monolith") || cleanPrompt.contains("magic") || cleanPrompt.contains("gem") -> {
                    ProceduralMeshes.createCrystalArtifact()
                }
                else -> {
                    when (theme) {
                        ArtTheme.CYBERPUNK -> ProceduralMeshes.createCyberMech()
                        ArtTheme.FANTASY_RPG -> ProceduralMeshes.createTreasureChest()
                        ArtTheme.RETRO_VOXEL -> ProceduralMeshes.createCrystalArtifact()
                        ArtTheme.LOW_POLY -> ProceduralMeshes.createSciFiStarfighter()
                        ArtTheme.ARCADE_COMIC_CEL -> ProceduralMeshes.createCyberMech()
                    }
                }
            }

            val title = if (prompt.isNotBlank()) prompt.take(30).replaceFirstChar { it.uppercase() } else mesh.name
            val lore = "Procedurally synthesized 3D Mesh with ${mesh.vertexCount} vertices, ${mesh.faceCount} triangles, normal coordinates, and Material3D parameters based on prompt: \"$prompt\"."
            return GenerationResult(title, lore, mesh = mesh.copy(name = title))
        } else {
            val spriteSheet = generateSpriteSheetFromPrompt(prompt, theme)
            val lore = "Synthesized multi-frame game sprite sheet with ${spriteSheet.columns * spriteSheet.rows} frames across ${spriteSheet.animations.size} animations for \"$prompt\"."
            return GenerationResult(spriteSheet.name, lore, spriteSheet = spriteSheet)
        }
    }

    private fun generateSpriteSheetFromPrompt(prompt: String, theme: ArtTheme): SpriteSheetData {
        val primaryColor = when (theme) {
            ArtTheme.CYBERPUNK -> Color.rgb(0, 240, 255)
            ArtTheme.FANTASY_RPG -> Color.rgb(245, 158, 11)
            ArtTheme.RETRO_VOXEL -> Color.rgb(16, 185, 129)
            ArtTheme.LOW_POLY -> Color.rgb(255, 42, 133)
            ArtTheme.ARCADE_COMIC_CEL -> Color.rgb(255, 56, 100)
        }
        val secondaryColor = Color.rgb(30, 41, 59)
        val highlightColor = Color.rgb(255, 255, 255)
        val title = if (prompt.isNotBlank()) prompt.take(24).replaceFirstChar { it.uppercase() } else "Synthesized Sprite"
        return generateParameterizedSpriteSheet(title, primaryColor, secondaryColor, highlightColor)
    }

    private fun generateParameterizedSpriteSheet(
        title: String,
        primaryColor: Int,
        secondaryColor: Int,
        highlightColor: Int
    ): SpriteSheetData {
        val fw = 28
        val fh = 28
        val cols = 4
        val rows = 2
        val bmp = Bitmap.createBitmap(fw * cols, fh * rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()

        fun px(x: Int, y: Int, color: Int) {
            paint.color = color
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat(), paint)
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val ox = c * fw + 4
                val oy = r * fh + 4
                val step = c

                val bounce = if (c % 2 == 1) 1 else 0
                for (y in 4..14) {
                    for (x in 4..16) {
                        val color = if (x in 7..13 && y in 6..12) primaryColor else secondaryColor
                        px(ox + x, oy + y + bounce, color)
                    }
                }

                px(ox + 8, oy + 8 + bounce, highlightColor)
                px(ox + 12, oy + 8 + bounce, highlightColor)

                val flair = (step % 4)
                for (fx in 0..3) {
                    px(ox + 2 - fx, oy + 10 + flair, primaryColor)
                    px(ox + 18 + fx, oy + 10 + flair, primaryColor)
                }

                if (r == 0) {
                    px(ox + 7, oy + 15, secondaryColor)
                    px(ox + 13, oy + 15, secondaryColor)
                } else {
                    for (burst in 0..step) {
                        px(ox + 10 + burst, oy + 17, highlightColor)
                    }
                }
            }
        }

        val scaled = Bitmap.createScaledBitmap(bmp, fw * cols * 2, fh * rows * 2, false)
        return SpriteSheetData.fromBitmap(scaled, title, cols, rows)
    }

    fun normalizeHexColor(raw: String): String {
        val cleaned = raw.trim().removePrefix("#").uppercase()
        val sixChar = cleaned.take(6).padEnd(6, '0')
        val valid = sixChar.map { if (it in '0'..'9' || it in 'A'..'F') it else 'F' }.joinToString("")
        return "#$valid"
    }

    fun parseColorIntSafe(hex: String): Int {
        return try {
            Color.parseColor(normalizeHexColor(hex))
        } catch (e: Exception) {
            Color.rgb(0, 229, 255)
        }
    }

    fun parseColorLongSafe(hex: String): Long {
        val c = parseColorIntSafe(hex)
        val r = Color.red(c).toLong() and 0xFFL
        val g = Color.green(c).toLong() and 0xFFL
        val b = Color.blue(c).toLong() and 0xFFL
        return (0xFFL shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Maps exceptions from Firebase AI Logic into clear, actionable messages while rethrowing `CancellationException`.
     */
    fun mapGeminiException(e: Throwable): String {
        if (e is CancellationException && e !is TimeoutCancellationException) {
            throw e
        }
        Log.e(TAG, "Gemini AI operation failed", e)
        return when (e) {
            is TimeoutCancellationException -> "Gemini AI request timed out after 60 seconds. Please try again."
            is QuotaExceededException -> "Gemini API rate limit or quota exceeded. Please wait a moment and retry."
            else -> e.localizedMessage ?: "Gemini AI request failed. Check network and Firebase App Check configuration."
        }
    }
}
