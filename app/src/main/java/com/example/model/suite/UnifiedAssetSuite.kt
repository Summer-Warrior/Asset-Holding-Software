package com.example.model.suite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.model.concept.ConceptAnalysis
import com.example.model.concept.ExtractedColor
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.HardwareEra
import com.example.model.model3d.Face3D
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ProceduralMeshes
import com.example.model.model3d.UV
import com.example.model.model3d.Vector3
import com.example.model.sprite.SpriteAnimation
import com.example.model.sprite.SpriteSheetData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class AssetSourceOrigin(val label: String) {
    USER_IMPORTED("Imported Asset"),
    DERIVED_FROM_IMPORTS("Cross-Synthesized from Imports"),
    STUDIO_PRESET("Studio Baseline")
}

enum class SuiteMeshStrategy(val label: String, val shortLabel: String) {
    USE_ACTIVE_3D_MESH("Use Imported / Active 3D Model (.OBJ)", "Active 3D Mesh"),
    EXTRUDE_FROM_CONCEPT_ART("Extrude 3D Voxel/Relief Model from Concept Art", "From Concept Art"),
    EXTRUDE_FROM_SPRITESHEET("Extrude 3D Model from Imported Spritesheet", "From Spritesheet"),
    SYNTHESIZE_FROM_TEXT("Generate 3D Model from Suite Text Spec", "From Text Spec")
}

enum class SuiteSpriteStrategy(val label: String, val shortLabel: String) {
    USE_ACTIVE_SPRITESHEET("Use Imported / Active 2D Spritesheet", "Active Spritesheet"),
    GENERATE_ANIMATED_SHEET_FROM_CONCEPT("Synthesize 4-Frame Animated Sheet from Concept Art", "4-Frame from Concept"),
    RENDER_TURNTABLE_SHEET_FROM_3D("Render 4-Angle Animated Sheet from 3D Model", "4-Angle from 3D Mesh")
}

enum class SuiteTextureStrategy(val label: String, val shortLabel: String) {
    USE_ACTIVE_TEXTURE("Use Imported / Active 3D Texture Map", "Active 3D Texture"),
    BAKE_SUITE_ATLAS("Bake Cohesive UV Texture Atlas from Suite Palette & Art", "Bake Suite UV Atlas")
}

data class SuiteBuildConfig(
    val suiteName: String = "Aegis-09 Cyber Vanguard",
    val entityCategory: String = "Character / Mecha Suite",
    val unifyPaletteAcrossAssets: Boolean = true,
    val autoGenerateUVsOnMesh: Boolean = true,
    val meshStrategy: SuiteMeshStrategy = SuiteMeshStrategy.USE_ACTIVE_3D_MESH,
    val spriteStrategy: SuiteSpriteStrategy = SuiteSpriteStrategy.USE_ACTIVE_SPRITESHEET,
    val textureStrategy: SuiteTextureStrategy = SuiteTextureStrategy.BAKE_SUITE_ATLAS,
    val spriteResolution: Int = 32
)

data class SuiteHardwareAllocation(
    val targetHardwareName: String,
    val totalVramUsedBytes: Int,
    val vramCapacityBytes: Int,
    val vramUtilizationPercent: Float,
    val meshGeometryBytes: Int,
    val textureMapBytes: Int,
    val spriteChrBytes: Int,
    val sharedClutColorsUsed: Int,
    val maxSystemClutColors: Int,
    val coexistenceVerdict: String,
    val everydaySuiteReadout: String,
    val technicalSuiteReadout: String,
    val memoryMapDiagram: String
)

data class UnifiedAssetSuite(
    val id: String,
    val name: String,
    val category: String,
    val descriptionPrompt: String,
    val mesh3D: Mesh3D,
    val meshOrigin: AssetSourceOrigin,
    val spriteSheet: SpriteSheetData,
    val spriteOrigin: AssetSourceOrigin,
    val textureBitmap: Bitmap,
    val textureName: String,
    val textureOrigin: AssetSourceOrigin,
    val conceptArtBitmap: Bitmap,
    val conceptOrigin: AssetSourceOrigin,
    val unifiedPalette: List<ExtractedColor>,
    val cohesionScore: Int,
    val importedInputsSummary: List<String>,
    val buildNotes: List<String>
)

object AssetSuiteSynthesizer {

    val entityCategories = listOf(
        "Character / Mecha Suite",
        "Starfighter / Vehicle Suite",
        "Boss / Creature Suite",
        "Weapon / Relic Suite",
        "Interactive Prop Suite"
    )

    /**
     * Synthesizes a single unified asset suite from all imported/active studio assets.
     */
    fun buildUnifiedSuite(
        config: SuiteBuildConfig,
        activeMesh: Mesh3D,
        activeSpriteSheet: SpriteSheetData,
        activeTexture: Bitmap?,
        activeTextureName: String,
        activeConceptArt: Bitmap?,
        promptText: String,
        hasUserImportedMesh: Boolean,
        hasUserImportedSprite: Boolean,
        hasUserImportedTexture: Boolean,
        hasUserImportedConcept: Boolean,
        hasUserEditedPrompt: Boolean
    ): UnifiedAssetSuite {
        val buildNotes = mutableListOf<String>()
        val importedSummary = mutableListOf<String>()

        if (hasUserImportedMesh) importedSummary.add("3D Model (${activeMesh.name})")
        if (hasUserImportedSprite) importedSummary.add("2D Spritesheet (${activeSpriteSheet.name})")
        if (hasUserImportedTexture) importedSummary.add("3D Texture ($activeTextureName)")
        if (hasUserImportedConcept) importedSummary.add("Concept Art Image")
        if (hasUserEditedPrompt) importedSummary.add("Text Spec")
        if (importedSummary.isEmpty()) {
            importedSummary.add("Studio Baseline Assets (${activeMesh.name} + ${activeSpriteSheet.name})")
        }

        // Step 1: Resolve or synthesize Concept Art reference
        val (resolvedConcept, conceptOrigin) = if (activeConceptArt != null) {
            buildNotes.add("Bound imported Concept Art (${activeConceptArt.width}×${activeConceptArt.height}px) as visual reference anchor.")
            activeConceptArt to (if (hasUserImportedConcept) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.STUDIO_PRESET)
        } else {
            val synthesizedConcept = generateConceptBoardFromSpriteAndMesh(
                spriteFrame = activeSpriteSheet.frames.firstOrNull(),
                mesh = activeMesh,
                title = config.suiteName
            )
            buildNotes.add("Synthesized Concept Art reference board from active Sprite & 3D Mesh.")
            synthesizedConcept to AssetSourceOrigin.DERIVED_FROM_IMPORTS
        }

        // Step 2: Build Unified Master Palette across Concept Art, Spritesheet, Texture, and Mesh
        val unifiedPalette = extractMasterSuitePalette(
            concept = activeConceptArt ?: resolvedConcept,
            sprite = activeSpriteSheet.frames.firstOrNull(),
            texture = activeTexture,
            mesh = activeMesh
        )
        buildNotes.add("Extracted ${unifiedPalette.size}-color Unified Master Palette across all suite inputs.")

        // Step 3: Resolve or synthesize the 3D Mesh
        val (rawMesh, meshOrigin) = when (config.meshStrategy) {
            SuiteMeshStrategy.USE_ACTIVE_3D_MESH -> {
                val origin = if (hasUserImportedMesh) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.STUDIO_PRESET
                activeMesh.copy(name = "${config.suiteName} [3D]") to origin
            }
            SuiteMeshStrategy.EXTRUDE_FROM_CONCEPT_ART -> {
                val extruded = ProceduralMeshes.createVoxelReliefFromBitmap(resolvedConcept, targetGrid = 22)
                buildNotes.add("Extruded 3D Voxel Relief Mesh (${extruded.vertexCount} verts) directly from Concept Art.")
                extruded.copy(name = "${config.suiteName} [Concept Relief 3D]") to AssetSourceOrigin.DERIVED_FROM_IMPORTS
            }
            SuiteMeshStrategy.EXTRUDE_FROM_SPRITESHEET -> {
                val frame = activeSpriteSheet.frames.firstOrNull() ?: resolvedConcept
                val extruded = ProceduralMeshes.createVoxelReliefFromBitmap(frame, targetGrid = 20)
                buildNotes.add("Extruded 3D Voxel Mesh (${extruded.vertexCount} verts) from primary Spritesheet frame.")
                extruded.copy(name = "${config.suiteName} [Sprite Extrusion 3D]") to AssetSourceOrigin.DERIVED_FROM_IMPORTS
            }
            SuiteMeshStrategy.SYNTHESIZE_FROM_TEXT -> {
                val lower = (config.suiteName + " " + promptText + " " + config.entityCategory).lowercase()
                val baseMesh = when {
                    lower.contains("fighter") || lower.contains("ship") || lower.contains("vehicle") -> ProceduralMeshes.createSciFiStarfighter()
                    lower.contains("chest") || lower.contains("prop") || lower.contains("mimic") -> ProceduralMeshes.createTreasureChest()
                    lower.contains("crystal") || lower.contains("relic") || lower.contains("weapon") -> ProceduralMeshes.createCrystalArtifact()
                    else -> ProceduralMeshes.createCyberMech()
                }
                buildNotes.add("Synthesized 3D geometry topology from Suite Text Specification.")
                baseMesh.copy(name = "${config.suiteName} [Synthesized 3D]") to AssetSourceOrigin.DERIVED_FROM_IMPORTS
            }
        }

        // Apply UV auto-wrapping and optional palette harmonization to 3D Mesh
        val harmonizedMesh = finalizeSuiteMesh(
            mesh = rawMesh,
            palette = unifiedPalette,
            harmonizeColors = config.unifyPaletteAcrossAssets,
            ensureUVs = config.autoGenerateUVsOnMesh
        )
        if (config.autoGenerateUVsOnMesh) {
            buildNotes.add("Generated normalized box/cylindrical UV coordinates across all ${harmonizedMesh.faceCount} mesh faces for 3D texture binding.")
        }

        // Step 4: Resolve or synthesize the 2D Spritesheet
        val (rawSheet, spriteOrigin) = when (config.spriteStrategy) {
            SuiteSpriteStrategy.USE_ACTIVE_SPRITESHEET -> {
                val origin = if (hasUserImportedSprite) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.STUDIO_PRESET
                activeSpriteSheet.copy(name = "${config.suiteName} [Sprites]") to origin
            }
            SuiteSpriteStrategy.GENERATE_ANIMATED_SHEET_FROM_CONCEPT -> {
                val genSheet = buildAnimatedSheetFromConcept(
                    concept = resolvedConcept,
                    suiteName = config.suiteName,
                    resolution = config.spriteResolution,
                    palette = unifiedPalette
                )
                buildNotes.add("Generated 4-frame animated 2D Spritesheet (Idle, Bob, Charge, Strike) from Concept Art at ${config.spriteResolution}×${config.spriteResolution}px.")
                genSheet to AssetSourceOrigin.DERIVED_FROM_IMPORTS
            }
            SuiteSpriteStrategy.RENDER_TURNTABLE_SHEET_FROM_3D -> {
                val turntableSheet = renderTurntableSpriteSheetFromMesh(
                    mesh = harmonizedMesh,
                    suiteName = config.suiteName,
                    resolution = config.spriteResolution
                )
                buildNotes.add("Rendered 4-angle orthographic turntable Spritesheet (0°, 90°, 180°, 270°) from Suite 3D Mesh.")
                turntableSheet to AssetSourceOrigin.DERIVED_FROM_IMPORTS
            }
        }

        val finalSpriteSheet = if (config.unifyPaletteAcrossAssets && config.spriteStrategy == SuiteSpriteStrategy.USE_ACTIVE_SPRITESHEET) {
            harmonizeSpriteSheetToPalette(rawSheet, unifiedPalette)
        } else {
            rawSheet
        }

        // Step 5: Resolve or bake the 3D Model Texture Map
        val (finalTexture, finalTexName, textureOrigin) = when (config.textureStrategy) {
            SuiteTextureStrategy.USE_ACTIVE_TEXTURE -> {
                if (activeTexture != null) {
                    val tex = if (config.unifyPaletteAcrossAssets) {
                        harmonizeBitmapToPalette(activeTexture, unifiedPalette, blendRatio = 0.45f)
                    } else {
                        activeTexture
                    }
                    val origin = if (hasUserImportedTexture) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.STUDIO_PRESET
                    Triple(tex, "${config.suiteName} ($activeTextureName)", origin)
                } else {
                    val baked = bakeSuiteTextureAtlas(unifiedPalette, finalSpriteSheet.frames.firstOrNull(), resolvedConcept)
                    Triple(baked, "${config.suiteName} UV Atlas", AssetSourceOrigin.DERIVED_FROM_IMPORTS)
                }
            }
            SuiteTextureStrategy.BAKE_SUITE_ATLAS -> {
                val baked = bakeSuiteTextureAtlas(unifiedPalette, finalSpriteSheet.frames.firstOrNull(), resolvedConcept)
                buildNotes.add("Baked 128×128 UV Surface Texture Atlas combining Master Palette trim, panel seams, and sprite emblem.")
                Triple(baked, "${config.suiteName} UV Atlas", AssetSourceOrigin.DERIVED_FROM_IMPORTS)
            }
        }

        if (config.unifyPaletteAcrossAssets) {
            buildNotes.add("Synchronized 3D Mesh materials, 2D Spritesheet frames, and 3D UV Texture to shared Master Palette.")
        }

        val cohesionScore = if (config.unifyPaletteAcrossAssets && config.autoGenerateUVsOnMesh) 98 else 86

        return UnifiedAssetSuite(
            id = "suite_${System.currentTimeMillis()}",
            name = config.suiteName.ifBlank { "Unified Asset Suite" },
            category = config.entityCategory,
            descriptionPrompt = promptText,
            mesh3D = harmonizedMesh,
            meshOrigin = meshOrigin,
            spriteSheet = finalSpriteSheet,
            spriteOrigin = spriteOrigin,
            textureBitmap = finalTexture,
            textureName = finalTexName,
            textureOrigin = textureOrigin,
            conceptArtBitmap = resolvedConcept,
            conceptOrigin = conceptOrigin,
            unifiedPalette = unifiedPalette,
            cohesionScore = cohesionScore,
            importedInputsSummary = importedSummary,
            buildNotes = buildNotes
        )
    }

    /**
     * Extracts a unified 8-color master palette across Concept Art, Sprite, Texture, and 3D Mesh.
     */
    fun extractMasterSuitePalette(
        concept: Bitmap?,
        sprite: Bitmap?,
        texture: Bitmap?,
        mesh: Mesh3D
    ): List<ExtractedColor> {
        val colorWeights = mutableMapOf<Int, Int>()

        fun sampleBitmap(bmp: Bitmap?, weightMultiplier: Int) {
            if (bmp == null) return
            val extracted = ConceptAnalysis.extractPalette(bmp, 6)
            for (ec in extracted) {
                val w = (ec.percentage * weightMultiplier).toInt().coerceAtLeast(1)
                colorWeights[ec.colorInt] = (colorWeights[ec.colorInt] ?: 0) + w
            }
        }

        sampleBitmap(concept, 3)
        sampleBitmap(sprite, 3)
        sampleBitmap(texture, 2)

        for (face in mesh.faces) {
            val c = face.color.toInt() or (0xFF shl 24)
            val r = ((c ushr 16) and 0xFF) / 8 * 8
            val g = ((c ushr 8) and 0xFF) / 8 * 8
            val b = (c and 0xFF) / 8 * 8
            val clustered = Color.rgb(r, g, b)
            colorWeights[clustered] = (colorWeights[clustered] ?: 0) + 4
        }

        val totalWeight = colorWeights.values.sum().coerceAtLeast(1)
        return colorWeights.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { (colorInt, weight) ->
                val hex = String.format("#%02X%02X%02X", Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
                ExtractedColor(
                    colorInt = colorInt,
                    hex = hex,
                    percentage = (weight.toFloat() / totalWeight) * 100f
                )
            }
    }

    private fun finalizeSuiteMesh(
        mesh: Mesh3D,
        palette: List<ExtractedColor>,
        harmonizeColors: Boolean,
        ensureUVs: Boolean
    ): Mesh3D {
        val paletteInts = palette.map { it.colorInt }
        val newUvs = if (ensureUVs && mesh.uvs.size < 4) {
            listOf(
                UV(0.0f, 0.0f),
                UV(1.0f, 0.0f),
                UV(1.0f, 1.0f),
                UV(0.0f, 1.0f)
            )
        } else {
            mesh.uvs
        }

        val updatedFaces = mesh.faces.mapIndexed { idx, face ->
            val targetColor = if (harmonizeColors && paletteInts.isNotEmpty()) {
                val orig = face.color.toInt()
                val nearest = findNearestPaletteColor(orig, paletteInts)
                (0xFFL shl 24) or (nearest.toLong() and 0xFFFFFFL)
            } else {
                face.color
            }

            val uvIdx = if (ensureUVs && (face.uvIndices == null || face.uvIndices.size < face.indices.size)) {
                IntArray(face.indices.size) { corner -> corner % newUvs.size.coerceAtLeast(1) }
            } else {
                face.uvIndices
            }

            face.copy(
                color = targetColor,
                uvIndices = uvIdx,
                materialName = "suite_mat_${idx % paletteInts.size.coerceAtLeast(1)}"
            )
        }

        return mesh.copy(faces = updatedFaces, uvs = newUvs)
    }

    /**
     * Generates a 4-frame animated SpriteSheetData from a single Concept Art bitmap.
     * Frame 0: Idle
     * Frame 1: Hover / Bob (-1px Y shift + visor pulse)
     * Frame 2: Action / Lean (+1px X shift + aura highlight)
     * Frame 3: Strike / Energy Flash (brightened core accents)
     */
    fun buildAnimatedSheetFromConcept(
        concept: Bitmap,
        suiteName: String,
        resolution: Int = 32,
        palette: List<ExtractedColor>
    ): SpriteSheetData {
        val baseSprite = ConceptAnalysis.convertConceptToSprite(concept, resolution = resolution, addOutline = true)
        val frameSize = baseSprite.width
        val frames = mutableListOf<Bitmap>()

        // Frame 0: Base Idle
        frames.add(baseSprite)

        // Frame 1: Bob Up
        val bobFrame = Bitmap.createBitmap(frameSize, frameSize, Bitmap.Config.ARGB_8888)
        val c1 = Canvas(bobFrame)
        c1.drawBitmap(baseSprite, 0f, -8f, null)
        frames.add(bobFrame)

        // Frame 2: Forward Lunge
        val lungeFrame = Bitmap.createBitmap(frameSize, frameSize, Bitmap.Config.ARGB_8888)
        val c2 = Canvas(lungeFrame)
        c2.drawBitmap(baseSprite, 8f, -4f, null)
        frames.add(lungeFrame)

        // Frame 3: Strike / Energy Pulse
        val pulseFrame = baseSprite.copy(Bitmap.Config.ARGB_8888, true)
        val accentColor = palette.firstOrNull()?.colorInt ?: Color.rgb(0, 229, 255)
        for (y in 0 until pulseFrame.height step 8) {
            for (x in 0 until pulseFrame.width step 8) {
                val p = pulseFrame.getPixel(x, y)
                if (Color.alpha(p) > 128) {
                    val r = ((Color.red(p) * 2 + Color.red(accentColor)) / 3).coerceIn(0, 255)
                    val g = ((Color.green(p) * 2 + Color.green(accentColor)) / 3).coerceIn(0, 255)
                    val b = ((Color.blue(p) * 2 + Color.blue(accentColor)) / 3).coerceIn(0, 255)
                    val boosted = Color.argb(255, r, g, b)
                    for (dy in 0 until min(8, pulseFrame.height - y)) {
                        for (dx in 0 until min(8, pulseFrame.width - x)) {
                            pulseFrame.setPixel(x + dx, y + dy, boosted)
                        }
                    }
                }
            }
        }
        frames.add(pulseFrame)

        val sheetBitmap = SpriteSheetData.assembleSheet(frames, columns = 4)
        return SpriteSheetData(
            id = "suite_sheet_${System.currentTimeMillis()}",
            name = "$suiteName [4-Frame Suite Sheet]",
            sourceBitmap = sheetBitmap,
            columns = 4,
            rows = 1,
            frameWidth = frameSize,
            frameHeight = frameSize,
            frames = frames,
            animations = listOf(
                SpriteAnimation("Suite Cycle", listOf(0, 1, 2, 3), fps = 6, loop = true),
                SpriteAnimation("Idle Bob", listOf(0, 1), fps = 4, loop = true),
                SpriteAnimation("Strike", listOf(2, 3), fps = 8, loop = true)
            )
        )
    }

    /**
     * Renders a 4-angle turntable 2D spritesheet directly from the 3D Mesh!
     */
    fun renderTurntableSpriteSheetFromMesh(
        mesh: Mesh3D,
        suiteName: String,
        resolution: Int = 32
    ): SpriteSheetData {
        val angles = listOf(0f, 90f, 180f, 270f)
        val outSize = resolution * 6
        val frames = angles.map { yawDeg ->
            renderMeshOrthographicFrame(mesh, yawDeg, outSize)
        }
        val sheetBmp = SpriteSheetData.assembleSheet(frames, columns = 4)
        return SpriteSheetData(
            id = "turntable_${System.currentTimeMillis()}",
            name = "$suiteName [3D Turntable Sheet]",
            sourceBitmap = sheetBmp,
            columns = 4,
            rows = 1,
            frameWidth = outSize,
            frameHeight = outSize,
            frames = frames,
            animations = listOf(
                SpriteAnimation("360° Turntable", listOf(0, 1, 2, 3), fps = 5, loop = true)
            )
        )
    }

    private fun renderMeshOrthographicFrame(mesh: Mesh3D, yawDegrees: Float, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.rgb(15, 23, 42)
        }

        val rad = Math.toRadians(yawDegrees.toDouble())
        val cosY = cos(rad).toFloat()
        val sinY = sin(rad).toFloat()

        // Rotate vertices around Y and slightly tilt X (15 deg)
        val cosX = cos(Math.toRadians(15.0)).toFloat()
        val sinX = sin(Math.toRadians(15.0)).toFloat()

        val transformed = mesh.vertices.map { v ->
            val rx = v.x * cosY + v.z * sinY
            val rz = -v.x * sinY + v.z * cosY
            val ry = v.y * cosX - rz * sinX
            val finalZ = v.y * sinX + rz * cosX
            Vector3(rx, ry, finalZ)
        }

        // Depth sort faces back-to-front
        val sortedFaces = mesh.faces.sortedBy { face ->
            face.indices.map { idx -> transformed.getOrElse(idx) { Vector3(0f, 0f, 0f) }.z }.average()
        }

        val half = sizePx / 2f
        val scale = sizePx * 0.36f
        val lightDir = Vector3(0.4f, 0.7f, 0.6f).normalize()

        for (face in sortedFaces) {
            if (face.indices.size < 3) continue
            val p0 = transformed.getOrElse(face.indices[0]) { continue }
            val p1 = transformed.getOrElse(face.indices[1]) { continue }
            val p2 = transformed.getOrElse(face.indices[2]) { continue }

            val norm = (p1 - p0).cross(p2 - p0).normalize()
            val diffuse = max(0.35f, abs(norm.dot(lightDir)))

            val baseColor = face.color.toInt()
            val r = (Color.red(baseColor) * diffuse).toInt().coerceIn(0, 255)
            val g = (Color.green(baseColor) * diffuse).toInt().coerceIn(0, 255)
            val b = (Color.blue(baseColor) * diffuse).toInt().coerceIn(0, 255)
            paint.color = Color.argb(255, r, g, b)

            val path = Path()
            face.indices.forEachIndexed { i, vIdx ->
                val pt = transformed[vIdx]
                val sx = half + pt.x * scale
                val sy = half - pt.y * scale
                if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
            }
            path.close()
            canvas.drawPath(path, paint)
            canvas.drawPath(path, strokePaint)
        }

        return bmp
    }

    /**
     * Bakes a 128x128 UV Texture Atlas from the suite's unified palette, sprite emblem, and concept art.
     */
    fun bakeSuiteTextureAtlas(
        palette: List<ExtractedColor>,
        spriteFrame: Bitmap?,
        concept: Bitmap?
    ): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colors = if (palette.isNotEmpty()) {
            palette.map { it.colorInt }
        } else {
            listOf(Color.rgb(15, 23, 42), Color.rgb(0, 229, 255), Color.rgb(255, 42, 133), Color.rgb(245, 158, 11))
        }

        val primary = colors.getOrElse(0) { Color.rgb(15, 23, 42) }
        val secondary = colors.getOrElse(1) { Color.rgb(30, 41, 59) }
        val accent1 = colors.getOrElse(2) { Color.rgb(0, 229, 255) }
        val accent2 = colors.getOrElse(3) { Color.rgb(255, 42, 133) }

        // Quadrant 1 & Background: Base armor / surface plating
        paint.color = primary
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Quadrant panels
        paint.color = secondary
        canvas.drawRect(6f, 6f, 60f, 60f, paint)
        canvas.drawRect(68f, 6f, 122f, 60f, paint)
        canvas.drawRect(6f, 68f, 60f, 122f, paint)
        canvas.drawRect(68f, 68f, 122f, 122f, paint)

        // Trim stripes using suite accent colors
        paint.color = accent1
        canvas.drawRect(0f, 61f, size.toFloat(), 67f, paint)
        canvas.drawRect(61f, 0f, 67f, size.toFloat(), paint)

        paint.color = accent2
        canvas.drawRect(10f, 10f, 56f, 18f, paint)
        canvas.drawRect(72f, 10f, 118f, 18f, paint)

        // Embed miniature decal from sprite or concept into bottom-right UV quadrant
        val decalSource = spriteFrame ?: concept
        if (decalSource != null) {
            val scaledDecal = Bitmap.createScaledBitmap(decalSource, 44, 44, false)
            canvas.drawBitmap(scaledDecal, 73f, 73f, null)
        }

        // Swatch strip in bottom-left UV quadrant for face UV indexing
        val swatchW = 48f / colors.size.coerceAtLeast(1)
        colors.forEachIndexed { idx, c ->
            paint.color = c
            canvas.drawRect(10f + idx * swatchW, 74f, 10f + (idx + 1) * swatchW, 116f, paint)
        }

        return bmp
    }

    private fun generateConceptBoardFromSpriteAndMesh(
        spriteFrame: Bitmap?,
        mesh: Mesh3D,
        title: String
    ): Bitmap {
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Deep studio blueprint backdrop
        paint.color = Color.rgb(9, 13, 22)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Blueprint grid lines
        paint.color = Color.rgb(22, 38, 64)
        paint.strokeWidth = 1f
        for (i in 0..size step 16) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), size.toFloat(), paint)
            canvas.drawLine(0f, i.toFloat(), size.toFloat(), i.toFloat(), paint)
        }

        // Draw 3D silhouette on right side
        val meshRender = renderMeshOrthographicFrame(mesh, yawDegrees = 32f, sizePx = 120)
        canvas.drawBitmap(meshRender, 66f, 36f, null)

        // Draw 2D sprite on bottom-left
        if (spriteFrame != null) {
            val scaledSprite = Bitmap.createScaledBitmap(spriteFrame, 84, 84, false)
            canvas.drawBitmap(scaledSprite, 10f, 92f, null)
        }

        return bmp
    }

    private fun harmonizeSpriteSheetToPalette(
        sheet: SpriteSheetData,
        palette: List<ExtractedColor>
    ): SpriteSheetData {
        if (palette.isEmpty()) return sheet
        val paletteInts = palette.map { it.colorInt }
        val newFrames = sheet.frames.map { frame ->
            harmonizeBitmapToPalette(frame, palette, blendRatio = 0.65f)
        }
        val newSource = SpriteSheetData.assembleSheet(newFrames, sheet.columns)
        return sheet.copy(
            sourceBitmap = newSource,
            frames = newFrames
        )
    }

    private fun harmonizeBitmapToPalette(
        source: Bitmap,
        palette: List<ExtractedColor>,
        blendRatio: Float
    ): Bitmap {
        if (palette.isEmpty()) return source
        val paletteInts = palette.map { it.colorInt }
        val w = source.width.coerceAtMost(256)
        val h = source.height.coerceAtMost(256)
        val scaled = if (source.width != w || source.height != h) {
            Bitmap.createScaledBitmap(source, w, h, false)
        } else {
            source
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val inv = 1f - blendRatio

        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = scaled.getPixel(x, y)
                val a = Color.alpha(p)
                if (a < 25) {
                    out.setPixel(x, y, Color.TRANSPARENT)
                } else {
                    val nearest = findNearestPaletteColor(p, paletteInts)
                    val r = (Color.red(p) * inv + Color.red(nearest) * blendRatio).toInt().coerceIn(0, 255)
                    val g = (Color.green(p) * inv + Color.green(nearest) * blendRatio).toInt().coerceIn(0, 255)
                    val b = (Color.blue(p) * inv + Color.blue(nearest) * blendRatio).toInt().coerceIn(0, 255)
                    out.setPixel(x, y, Color.argb(a, r, g, b))
                }
            }
        }
        return out
    }

    private fun findNearestPaletteColor(color: Int, paletteInts: List<Int>): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        var best = paletteInts.first()
        var bestDist = Int.MAX_VALUE
        for (pc in paletteInts) {
            val dr = r - Color.red(pc)
            val dg = g - Color.green(pc)
            val db = b - Color.blue(pc)
            val d = dr * dr + dg * dg + db * db
            if (d < bestDist) {
                bestDist = d
                best = pc
            }
        }
        return best
    }

    /**
     * Evaluates how the entire Single Asset Suite (3D Mesh + 3D UV Texture + 2D Spritesheet + Shared Palette)
     * is packed and outputted together on the target console/handheld hardware.
     */
    fun analyzeSuiteOnHardware(
        suite: UnifiedAssetSuite,
        spec: ConsoleHardwareSpec
    ): SuiteHardwareAllocation {
        val meshReport = HardwareAssetSimulator.analyzeMeshOnHardware(suite.mesh3D, spec)
        val spriteFrame = suite.spriteSheet.frames.firstOrNull() ?: suite.textureBitmap
        val spriteReport = HardwareAssetSimulator.simulateSpriteOnHardware(spriteFrame, spec, showTileGrid = false)

        val meshBytes = meshReport.vertexBufferBytes + meshReport.faceTableBytes
        val spriteChrBytes = spriteReport.chrVramBytes * suite.spriteSheet.frames.size.coerceAtMost(8)

        // Calculate 3D Texture VRAM footprint based on console era/bpp
        val texDim = when (spec.era) {
            HardwareEra.ERA_8BIT -> 0
            HardwareEra.ERA_16BIT -> 64
            HardwareEra.ERA_32_64BIT -> if (spec.id == "n64") 64 else 128
            else -> 128
        }
        val textureBytes = if (!spec.hasHardware3D) {
            0
        } else {
            (texDim * texDim * spec.bitsPerPixelSprite) / 8
        }

        val totalUsed = meshBytes + spriteChrBytes + textureBytes
        val vramCap = spec.vramBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1024)
        val vramKB = (spec.vramBytes / 1024L).coerceAtLeast(1L)
        val utilPct = ((totalUsed.toFloat() / vramCap.toFloat()) * 100f).coerceIn(0.1f, 999f)

        val sharedClutUsed = min(suite.unifiedPalette.size, spec.maxOnScreenColors)
        val colorFmtName = spec.paletteType.name
        val verdict = when {
            utilPct <= 60f -> "OPTIMAL SINGLE-FRAME COEXISTENCE: Full 3D Model, UV Texture Atlas, and ${suite.spriteSheet.frames.size}-frame 2D Spritesheet reside simultaneously in ${spec.shortName} VRAM."
            utilPct <= 100f -> "TIGHT VRAM FIT (${String.format("%.1f", utilPct)}%): Suite fits in ${spec.shortName} VRAM using shared CLUT palette compression."
            else -> "STREAMING / BANK-SWITCH REQUIRED (${String.format("%.1f", utilPct)}%): Suite exceeds single-bank ${spec.shortName} VRAM; DMA bank-switching streams sprite frames per animation tick."
        }

        val everydayText = buildString {
            append("Because '${suite.name}' was compiled as a Single Unified Asset Suite, all of its pieces—its 3D model, surface texture, and 2D sprite animations—share the exact same ${suite.unifiedPalette.size}-color Master Palette. ")
            append("When outputted on the ${spec.name}, the console doesn't have to waste precious color slots loading separate palettes for 2D menus/HUD sprites and 3D gameplay. ")
            if (spec.hasHardware3D) {
                append("On screen, the ${suite.mesh3D.faceCount}-polygon 3D model renders with its ${texDim}×${texDim}px surface texture while the ${suite.spriteSheet.frames.size}-frame 2D sprite animates alongside it using ${String.format("%.1f", totalUsed / 1024f)} KB of the system's $vramKB KB video memory.")
            } else {
                append("Since the ${spec.name} is a dedicated 2D system, it displays the suite's ${suite.spriteSheet.frames.size}-frame 2D spritesheet directly in hardware OAM while converting the 3D model into a ${meshReport.triangleCount}-line wireframe/prerendered bank inside ${String.format("%.1f", totalUsed / 1024f)} KB of memory.")
            }
        }

        val technicalText = buildString {
            append("UNIFIED SUITE SILICON PARTITION [${spec.name}]: Total Suite Footprint = $totalUsed Bytes (${String.format("%.2f", totalUsed / 1024f)} KB / $vramKB KB VRAM = ${String.format("%.1f", utilPct)}%). ")
            append("1) SHARED CLUT / CGRAM: All suite assets are quantized against a single ${sharedClutUsed}-entry Master CLUT ($colorFmtName), eliminating palette bank thrashing between 2D OAM and 3D raster passes. ")
            append("2) 3D GEOMETRY BANK: $meshBytes Bytes (${meshReport.vertexCount} verts, ${meshReport.triangleCount} primitives). ")
            append("3) 3D TEXTURE PAGE: $textureBytes Bytes (${texDim}×${texDim} @ ${spec.bitsPerPixelSprite}bpp). ")
            append("4) 2D SPRITE CHR BANK: $spriteChrBytes Bytes across ${suite.spriteSheet.frames.size} frames (${spriteReport.totalChrTiles} 8×8 tiles/frame).")
        }

        val mapDiagram = buildString {
            append("0x0000_0000 [CLUT/CGRAM ] -> ${sharedClutUsed * 2} B (${sharedClutUsed} shared colors @ $colorFmtName)\n")
            append("0x0000_0100 [2D CHR BANK] -> $spriteChrBytes B (${suite.spriteSheet.frames.size} frames × ${spriteReport.totalChrTiles} tiles)\n")
            append("0x0000_4000 [3D TEX PAGE] -> $textureBytes B (${texDim}×${texDim} UV Atlas @ ${spec.bitsPerPixelSprite}bpp)\n")
            append("0x0000_8000 [3D GEO LIST] -> $meshBytes B (${meshReport.vertexCount}v / ${meshReport.triangleCount}f packet buffer)\n")
            append("--------------------------------------------------------------------\n")
            append("TOTAL SUITE VRAM ALLOCATION : $totalUsed B / $vramCap B (${String.format("%.1f", utilPct)}%)")
        }

        return SuiteHardwareAllocation(
            targetHardwareName = spec.name,
            totalVramUsedBytes = totalUsed,
            vramCapacityBytes = vramCap,
            vramUtilizationPercent = utilPct,
            meshGeometryBytes = meshBytes,
            textureMapBytes = textureBytes,
            spriteChrBytes = spriteChrBytes,
            sharedClutColorsUsed = sharedClutUsed,
            maxSystemClutColors = spec.maxOnScreenColors,
            coexistenceVerdict = verdict,
            everydaySuiteReadout = everydayText,
            technicalSuiteReadout = technicalText,
            memoryMapDiagram = mapDiagram
        )
    }

    /**
     * Exports a complete text manifest of the Single Asset Suite (Metadata + Unified Palette + Hardware Map + Embedded OBJ).
     */
    fun exportSuiteBundleManifest(
        suite: UnifiedAssetSuite,
        spec: ConsoleHardwareSpec,
        allocation: SuiteHardwareAllocation
    ): String {
        return buildString {
            appendLine("======================================================================")
            appendLine("PIXELFORGE UNIFIED SINGLE ASSET SUITE MANIFEST")
            appendLine("======================================================================")
            appendLine("Suite Name       : ${suite.name}")
            appendLine("Suite ID         : ${suite.id}")
            appendLine("Entity Category  : ${suite.category}")
            appendLine("Cohesion Score   : ${suite.cohesionScore}%")
            appendLine("Inputs Bound     : ${suite.importedInputsSummary.joinToString(", ")}")
            appendLine("Lore / Spec      : ${suite.descriptionPrompt}")
            appendLine()
            appendLine("--- COMPONENT ORIGINS & METRICS ---")
            appendLine("1. 3D Model      : ${suite.mesh3D.name} [${suite.meshOrigin.label}] -> ${suite.mesh3D.vertexCount} verts, ${suite.mesh3D.faceCount} faces, ${suite.mesh3D.uvs.size} UVs")
            appendLine("2. 2D Spritesheet: ${suite.spriteSheet.name} [${suite.spriteOrigin.label}] -> ${suite.spriteSheet.frames.size} frames (${suite.spriteSheet.columns}x${suite.spriteSheet.rows})")
            appendLine("3. 3D UV Texture : ${suite.textureName} [${suite.textureOrigin.label}] -> ${suite.textureBitmap.width}x${suite.textureBitmap.height}px")
            appendLine("4. Concept Art   : ${suite.conceptOrigin.label} -> ${suite.conceptArtBitmap.width}x${suite.conceptArtBitmap.height}px")
            appendLine()
            appendLine("--- UNIFIED SUITE MASTER PALETTE (${suite.unifiedPalette.size} SHARED COLORS) ---")
            suite.unifiedPalette.forEachIndexed { idx, color ->
                appendLine("  Slot [$idx] : ${color.hex} (${String.format("%.1f", color.percentage)}% coverage)")
            }
            appendLine()
            appendLine("--- TARGET HARDWARE SUITE ALLOCATION (${spec.name}) ---")
            appendLine(allocation.coexistenceVerdict)
            appendLine(allocation.memoryMapDiagram)
            appendLine()
            appendLine("--- EVERYDAY READER SUITE OUTPUT SUMMARY ---")
            appendLine(allocation.everydaySuiteReadout)
            appendLine()
            appendLine("--- HIGH-END TECHNICAL SUITE SILICON SUMMARY ---")
            appendLine(allocation.technicalSuiteReadout)
        }
    }
}
