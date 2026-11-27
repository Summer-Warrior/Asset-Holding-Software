package com.example.model.suite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.model.concept.ExtractedColor
import com.example.model.model3d.Face3D
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ProceduralMeshes
import com.example.model.model3d.UV
import com.example.model.model3d.Vector3
import com.example.model.sprite.SampleSpriteSheets
import com.example.model.sprite.SpriteAnimation
import com.example.model.sprite.SpriteSheetData
import com.example.model.texture.ProceduralTextures
import kotlin.math.min

enum class CharacterAssetModality(val label: String, val badge: String) {
    MESH_3D("3D Model (.OBJ)", "3D MESH"),
    SPRITESHEET_2D("2D Spritesheet", "2D SPRITE"),
    TEXTURE_UV("3D UV Texture", "UV TEX"),
    CONCEPT_ART("Concept Art", "CONCEPT"),
    LORE_SPEC("Text / Lore Spec", "LORE")
}

data class StagedCharacterAsset(
    val id: String,
    val assetTitle: String,
    val characterName: String,
    val modality: CharacterAssetModality,
    val isUserImported: Boolean,
    val mesh: Mesh3D? = null,
    val spriteSheet: SpriteSheetData? = null,
    val bitmap: Bitmap? = null,
    val loreText: String? = null,
    val gameTag: String = "General Studio",
    val groupTag: String = "Core Roster"
)

data class NamedCharacterFusionGroup(
    val characterName: String,
    val canonicalKey: String,
    val assets: List<StagedCharacterAsset>,
    val meshAssets: List<StagedCharacterAsset>,
    val spriteAssets: List<StagedCharacterAsset>,
    val textureAssets: List<StagedCharacterAsset>,
    val conceptAssets: List<StagedCharacterAsset>,
    val loreAssets: List<StagedCharacterAsset>
) {
    val totalAssetCount: Int get() = assets.size
    val modalityCount: Int
        get() = listOf(
            meshAssets.isNotEmpty(),
            spriteAssets.isNotEmpty(),
            textureAssets.isNotEmpty(),
            conceptAssets.isNotEmpty(),
            loreAssets.isNotEmpty()
        ).count { it }

    val hasMultipleAssetsToFuse: Boolean get() = assets.size >= 2
    val userImportedCount: Int get() = assets.count { it.isUserImported }
}

object CharacterAssetFusionEngine {

    private val NOISE_WORDS = setOf(
        "mesh", "model", "obj", "sprite", "sprites", "spritesheet", "sheet",
        "texture", "tex", "uv", "atlas", "concept", "art", "drawing", "sketch",
        "lore", "spec", "description", "prompt", "imported", "custom", "default",
        "idle", "walk", "run", "attack", "strike", "action", "frame", "frames",
        "3d", "2d", "hd", "lowpoly", "voxel", "relief", "turntable", "suite",
        "cyber", "dungeon", "scifi", "sci-fi", "the", "and", "with", "for", "from"
    )

    /**
     * Automatically extracts or matches a canonical Named Character identity from an asset title,
     * filename, or prompt description.
     */
    fun extractCharacterIdentity(
        rawTitleOrPrompt: String,
        explicitCharacterTag: String = "",
        existingCharacterNames: List<String> = emptyList()
    ): String {
        if (explicitCharacterTag.isNotBlank()) {
            val cleanedTag = explicitCharacterTag.trim()
            val matchedExisting = existingCharacterNames.firstOrNull {
                it.equals(cleanedTag, ignoreCase = true)
            }
            return matchedExisting ?: cleanedTag
        }

        val cleaned = rawTitleOrPrompt
            .substringBeforeLast(".") // strip .obj / .png extension
            .replace(Regex("\\[.*?]"), " ")
            .replace(Regex("\\(.*?\\)"), " ")
            .replace(Regex("[_\\-/:•]+"), " ")
            .trim()

        // 1. Check if the title/prompt directly mentions any known character name in the pool
        for (known in existingCharacterNames) {
            if (known.length >= 3 && cleaned.contains(known, ignoreCase = true)) {
                return known
            }
            val primaryKnownToken = known.split(" ", "-").firstOrNull { it.length >= 3 }
            if (primaryKnownToken != null && cleaned.split(Regex("\\s+")).any { it.equals(primaryKnownToken, ignoreCase = true) }) {
                return known
            }
        }

        // 2. Built-in canonical character aliases
        val lower = cleaned.lowercase()
        when {
            lower.contains("kaelen") || lower.contains("ninja") || lower.contains("shinobi") -> return "Kaelen"
            lower.contains("valkyrie") || lower.contains("mech") || lower.contains("strider") -> return "Valkyrie-09"
            lower.contains("starfighter") || lower.contains("nx-3") || lower.contains("nx3") -> return "Starfighter NX-3"
            lower.contains("slime") || lower.contains("gloom") -> return "Gloom Slime"
            lower.contains("mimic") || lower.contains("chest") -> return "Mimic Chest"
            lower.contains("aether") || lower.contains("crystal") -> return "Aether Monolith"
        }

        // 3. Extract leading capitalized / proper-noun character tokens
        val tokens = cleaned.split(Regex("\\s+"))
            .filter { it.length >= 2 && it.lowercase() !in NOISE_WORDS }

        if (tokens.isEmpty()) return "Unnamed Character"

        val selectedTokens = tokens.take(2)
        return selectedTokens.joinToString(" ") { token ->
            token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Clusters all staged/imported assets by their canonical Named Character identity.
     */
    fun clusterAssetsByNamedCharacter(
        pool: List<StagedCharacterAsset>
    ): List<NamedCharacterFusionGroup> {
        val grouped = pool.groupBy { it.characterName.trim().lowercase() }
        return grouped.map { (canonicalKey, items) ->
            val displayName = items.first().characterName.trim()
            NamedCharacterFusionGroup(
                characterName = displayName,
                canonicalKey = canonicalKey,
                assets = items,
                meshAssets = items.filter { it.modality == CharacterAssetModality.MESH_3D && it.mesh != null },
                spriteAssets = items.filter { it.modality == CharacterAssetModality.SPRITESHEET_2D && it.spriteSheet != null },
                textureAssets = items.filter { it.modality == CharacterAssetModality.TEXTURE_UV && it.bitmap != null },
                conceptAssets = items.filter { it.modality == CharacterAssetModality.CONCEPT_ART && it.bitmap != null },
                loreAssets = items.filter { it.modality == CharacterAssetModality.LORE_SPEC && !it.loreText.isNullOrBlank() }
            )
        }.sortedWith(
            compareByDescending<NamedCharacterFusionGroup> { it.userImportedCount }
                .thenByDescending { it.totalAssetCount }
        )
    }

    /**
     * Automatically fuses all assets in a NamedCharacterFusionGroup into a single cohesive UnifiedAssetSuite.
     * - Fuses multiple 3D meshes sharing the character name into a single composite multi-part 3D mesh.
     * - Fuses multiple 2D spritesheets & concept/3D turntable frames into a unified Master Character Spritesheet.
     * - Fuses textures, concept art, and sprite emblems into a unified 128x128 Character UV Atlas.
     * - Harmonizes all fused assets to a shared Character DNA Master Palette.
     */
    fun fuseNamedCharacterGroup(
        group: NamedCharacterFusionGroup,
        unifyPalette: Boolean = true
    ): UnifiedAssetSuite {
        val charName = group.characterName
        val fusionNotes = mutableListOf<String>()
        fusionNotes.add(
            "NAMED-CHARACTER AUTO-FUSION ['$charName']: Automatically matched and fused ${group.totalAssetCount} assets (${group.modalityCount} modalities) sharing character identity '$charName'."
        )

        // 1. Fuse Lore & Character Specifications
        val combinedLore = if (group.loreAssets.isNotEmpty()) {
            val joined = group.loreAssets.mapNotNull { it.loreText?.trim() }.distinct().joinToString(" | ")
            fusionNotes.add("Fused ${group.loreAssets.size} Lore/Text Specification(s) for '$charName'.")
            joined
        } else {
            "Unified character asset suite for $charName compiled via automatic named-character fusion."
        }

        // 2. Fuse or Synthesize Concept Art Board
        val primaryConcept = group.conceptAssets.firstOrNull()?.bitmap
        val primarySpriteFrame = group.spriteAssets.firstOrNull()?.spriteSheet?.frames?.firstOrNull()

        // 3. Fuse 3D Meshes for this Named Character
        val (fusedRawMesh, meshOrigin) = fuseMeshesForCharacter(group, fusionNotes)

        // 4. Extract Shared Character DNA Master Palette across ALL matched assets for this character
        val masterPalette = extractCharacterMasterPalette(group, fusedRawMesh)
        fusionNotes.add(
            "Extracted ${masterPalette.size}-color Character DNA Master Palette across all '$charName' assets (${masterPalette.take(4).joinToString { it.hex }})."
        )

        // Apply UV auto-wrapping and Character DNA palette harmonization to the fused 3D mesh
        val finalizedMesh = harmonizeAndUVWrapFusedMesh(fusedRawMesh, masterPalette, unifyPalette)

        // 5. Fuse 2D Spritesheets & Turntable/Concept Animations for this Named Character
        val (fusedSpriteSheet, spriteOrigin) = fuseSpriteSheetsForCharacter(
            group = group,
            fusedMesh = finalizedMesh,
            palette = masterPalette,
            unifyPalette = unifyPalette,
            fusionNotes = fusionNotes
        )

        // 6. Fuse 3D UV Textures, Concept Art & Sprite Decals into a Character Master UV Atlas
        val (fusedTexture, fusedTextureName, textureOrigin) = fuseTexturesForCharacter(
            group = group,
            palette = masterPalette,
            primarySpriteFrame = fusedSpriteSheet.frames.firstOrNull() ?: primarySpriteFrame,
            fusionNotes = fusionNotes
        )

        // 7. Build Composite Concept Reference Board if multiple visual sources exist
        val fusedConcept = if (primaryConcept != null && group.conceptAssets.size == 1) {
            primaryConcept
        } else {
            buildCharacterFusionConceptBoard(
                charName = charName,
                conceptBmp = primaryConcept,
                spriteBmp = fusedSpriteSheet.frames.firstOrNull(),
                textureBmp = fusedTexture,
                palette = masterPalette
            )
        }
        val conceptOrigin = if (group.conceptAssets.any { it.isUserImported }) {
            AssetSourceOrigin.USER_IMPORTED
        } else {
            AssetSourceOrigin.DERIVED_FROM_IMPORTS
        }

        val inputTitles = group.assets.map { "${it.assetTitle} [${it.modality.badge}]" }
        val cohesion = min(100, 90 + group.totalAssetCount * 2)

        return UnifiedAssetSuite(
            id = "char_fusion_${group.canonicalKey}_${System.currentTimeMillis()}",
            name = "$charName • Fused Character Suite",
            category = "Auto-Fused Character Suite (${group.totalAssetCount} Assets)",
            descriptionPrompt = combinedLore,
            mesh3D = finalizedMesh,
            meshOrigin = meshOrigin,
            spriteSheet = fusedSpriteSheet,
            spriteOrigin = spriteOrigin,
            textureBitmap = fusedTexture,
            textureName = fusedTextureName,
            textureOrigin = textureOrigin,
            conceptArtBitmap = fusedConcept,
            conceptOrigin = conceptOrigin,
            unifiedPalette = masterPalette,
            cohesionScore = cohesion,
            importedInputsSummary = inputTitles,
            buildNotes = fusionNotes
        )
    }

    /**
     * Fuses all 3D meshes belonging to the named character into a single composite Mesh3D.
     * If multiple meshes exist (e.g. Base Body + Weapon/Wings/Attachment), their vertices and faces
     * are merged into one unified topology. If only 2D assets exist, extrudes a 3D character mesh.
     */
    private fun fuseMeshesForCharacter(
        group: NamedCharacterFusionGroup,
        fusionNotes: MutableList<String>
    ): Pair<Mesh3D, AssetSourceOrigin> {
        val meshes = group.meshAssets.mapNotNull { it.mesh }
        val anyImported = group.meshAssets.any { it.isUserImported }

        if (meshes.size >= 2) {
            val mergedVertices = mutableListOf<Vector3>()
            val mergedFaces = mutableListOf<Face3D>()
            val mergedUvs = mutableListOf<UV>()

            meshes.forEachIndexed { meshIdx, m ->
                val vOffset = mergedVertices.size
                val uvOffset = mergedUvs.size
                mergedVertices.addAll(m.vertices)
                mergedUvs.addAll(m.uvs)

                m.faces.forEach { face ->
                    val shiftedIndices = IntArray(face.indices.size) { i -> face.indices[i] + vOffset }
                    val shiftedUvs = face.uvIndices?.let { uvs ->
                        IntArray(uvs.size) { i -> uvs[i] + uvOffset }
                    }
                    mergedFaces.add(
                        face.copy(
                            indices = shiftedIndices,
                            uvIndices = shiftedUvs,
                            materialName = "${group.characterName}_part_${meshIdx + 1}"
                        )
                    )
                }
            }

            val fused = Mesh3D.normalizeMesh(
                name = "${group.characterName} [Fused ${meshes.size}-Part 3D Rig]",
                rawVertices = mergedVertices,
                faces = mergedFaces,
                uvs = mergedUvs
            )
            fusionNotes.add(
                "3D GEOMETRY FUSION: Merged ${meshes.size} character meshes (${group.meshAssets.joinToString { it.assetTitle }}) into a single ${fused.vertexCount}-vertex, ${fused.faceCount}-face composite 3D rig."
            )
            return fused to (if (anyImported) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.DERIVED_FROM_IMPORTS)
        } else if (meshes.size == 1) {
            val baseMesh = meshes.first()
            // If we also have a 2D sprite or concept art for this character, fuse an extruded dorsal emblem/aura crest onto the mesh
            val spriteRef = group.spriteAssets.firstOrNull()?.spriteSheet?.frames?.firstOrNull()
                ?: group.conceptAssets.firstOrNull()?.bitmap

            if (spriteRef != null) {
                val crestMesh = ProceduralMeshes.createVoxelReliefFromBitmap(spriteRef, targetGrid = 10, depthScale = 0.2f)
                val mergedVertices = baseMesh.vertices.toMutableList()
                val mergedFaces = baseMesh.faces.toMutableList()
                val vOffset = mergedVertices.size

                // Position the fused character crest halo slightly behind/above the character mesh
                crestMesh.vertices.forEach { v ->
                    mergedVertices.add(Vector3(v.x * 0.45f, v.y * 0.45f + 0.65f, v.z * 0.35f - 0.55f))
                }
                crestMesh.faces.forEach { f ->
                    val shifted = IntArray(f.indices.size) { i -> f.indices[i] + vOffset }
                    mergedFaces.add(f.copy(indices = shifted, materialName = "${group.characterName}_crest"))
                }

                val augmented = Mesh3D.normalizeMesh(
                    name = "${group.characterName} [Fused 3D + Sprite Crest]",
                    rawVertices = mergedVertices,
                    faces = mergedFaces,
                    uvs = baseMesh.uvs
                )
                fusionNotes.add(
                    "3D + 2D TOPOLOGY FUSION: Bound '${baseMesh.name}' (${baseMesh.vertexCount}v) with a 3D Voxel Crest extruded from ${group.characterName}'s 2D art (${augmented.vertexCount} total vertices, ${augmented.faceCount} faces)."
                )
                return augmented to (if (anyImported) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.DERIVED_FROM_IMPORTS)
            }

            return baseMesh.copy(name = "${group.characterName} [3D Model]") to
                    (if (anyImported) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.STUDIO_PRESET)
        } else {
            // Extrude 3D model from the character's Concept Art or Spritesheet
            val sourceBmp = group.conceptAssets.firstOrNull()?.bitmap
                ?: group.spriteAssets.firstOrNull()?.spriteSheet?.frames?.firstOrNull()
            return if (sourceBmp != null) {
                val extruded = ProceduralMeshes.createVoxelReliefFromBitmap(sourceBmp, targetGrid = 20)
                fusionNotes.add(
                    "3D AUTO-EXTRUSION: Synthesized ${extruded.vertexCount}-vertex 3D character mesh from '${group.characterName}' 2D visual assets."
                )
                extruded.copy(name = "${group.characterName} [Auto-Extruded 3D]") to AssetSourceOrigin.DERIVED_FROM_IMPORTS
            } else {
                val fallback = ProceduralMeshes.createCyberMech().copy(name = "${group.characterName} [Synthesized 3D]")
                fallback to AssetSourceOrigin.DERIVED_FROM_IMPORTS
            }
        }
    }

    /**
     * Fuses all 2D spritesheets belonging to the named character—plus concept & 3D turntable frames—into
     * one unified Master Character Spritesheet.
     */
    private fun fuseSpriteSheetsForCharacter(
        group: NamedCharacterFusionGroup,
        fusedMesh: Mesh3D,
        palette: List<ExtractedColor>,
        unifyPalette: Boolean,
        fusionNotes: MutableList<String>
    ): Pair<SpriteSheetData, AssetSourceOrigin> {
        val sheets = group.spriteAssets.mapNotNull { it.spriteSheet }
        val anyImported = group.spriteAssets.any { it.isUserImported }

        if (sheets.isEmpty()) {
            // Synthesize from Concept Art or 3D Turntable
            val conceptBmp = group.conceptAssets.firstOrNull()?.bitmap
            val generated = if (conceptBmp != null) {
                fusionNotes.add("2D SPRITE AUTO-SYNTHESIS: Generated 4-frame animated sheet from '${group.characterName}' Concept Art.")
                AssetSuiteSynthesizer.buildAnimatedSheetFromConcept(conceptBmp, group.characterName, 32, palette)
            } else {
                fusionNotes.add("2D SPRITE TURNTABLE FUSION: Rendered 4-angle 2D spritesheet directly from '${group.characterName}' fused 3D rig.")
                AssetSuiteSynthesizer.renderTurntableSpriteSheetFromMesh(fusedMesh, group.characterName, 32)
            }
            return generated to AssetSourceOrigin.DERIVED_FROM_IMPORTS
        }

        // Standardize frame size to the primary sheet's frame dimensions
        val targetW = sheets.first().frameWidth.coerceIn(32, 256)
        val targetH = sheets.first().frameHeight.coerceIn(32, 256)
        val combinedFrames = mutableListOf<Bitmap>()
        val combinedAnimations = mutableListOf<SpriteAnimation>()

        sheets.forEachIndexed { sheetIdx, sheet ->
            val startIndex = combinedFrames.size
            sheet.frames.forEach { frame ->
                val normalized = if (frame.width != targetW || frame.height != targetH) {
                    Bitmap.createScaledBitmap(frame, targetW, targetH, false)
                } else {
                    frame
                }
                combinedFrames.add(normalized)
            }

            if (sheet.animations.isNotEmpty()) {
                sheet.animations.forEach { anim ->
                    val shiftedIndices = anim.frameIndices.map { (it + startIndex).coerceAtMost(combinedFrames.size - 1) }
                    combinedAnimations.add(
                        anim.copy(
                            name = if (sheets.size > 1) "${sheet.name.take(10)}:${anim.name}" else anim.name,
                            frameIndices = shiftedIndices
                        )
                    )
                }
            } else {
                val indices = (startIndex until combinedFrames.size).toList()
                combinedAnimations.add(SpriteAnimation("Bank ${sheetIdx + 1}", indices, fps = 8))
            }
        }

        // Also fuse 2 bonus cross-modal frames (3D Turntable Front/Side projection) if a 3D mesh was part of the character group
        if (group.meshAssets.isNotEmpty() && combinedFrames.size <= 16) {
            val turntable = AssetSuiteSynthesizer.renderTurntableSpriteSheetFromMesh(fusedMesh, group.characterName, 32)
            val startIdx = combinedFrames.size
            turntable.frames.take(2).forEach { tf ->
                combinedFrames.add(Bitmap.createScaledBitmap(tf, targetW, targetH, false))
            }
            combinedAnimations.add(
                SpriteAnimation("3D Rig Projection", (startIdx until combinedFrames.size).toList(), fps = 6)
            )
            fusionNotes.add(
                "2D + 3D SPRITE BANK FUSION: Combined ${sheets.size} spritesheet(s) (${sheets.sumOf { it.frames.size }} frames) with 2 orthographic 3D rig projection frames into a ${combinedFrames.size}-frame Master Character Sheet."
            )
        } else if (sheets.size >= 2) {
            fusionNotes.add(
                "MULTI-SHEET FUSION: Stitched ${sheets.size} spritesheets sharing character '${group.characterName}' into a unified ${combinedFrames.size}-frame Master Character Spritesheet."
            )
        } else {
            fusionNotes.add(
                "Bound '${sheets.first().name}' (${combinedFrames.size} frames) and synchronized palette with '${group.characterName}' 3D & Texture assets."
            )
        }

        val cols = 4.coerceAtMost(combinedFrames.size.coerceAtLeast(1))
        val rows = (combinedFrames.size + cols - 1) / cols
        val assembled = SpriteSheetData.assembleSheet(combinedFrames, columns = cols)

        val fusedSheet = SpriteSheetData(
            id = "fused_sheet_${group.canonicalKey}_${System.currentTimeMillis()}",
            name = "${group.characterName} [Fused ${combinedFrames.size}f Master Sheet]",
            sourceBitmap = assembled,
            columns = cols,
            rows = rows,
            frameWidth = targetW,
            frameHeight = targetH,
            frames = combinedFrames,
            animations = combinedAnimations
        )

        return fusedSheet to (if (anyImported) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.DERIVED_FROM_IMPORTS)
    }

    /**
     * Fuses all 3D textures, concept art color zones, and sprite decals for the named character
     * into a cohesive 128x128 UV Texture Atlas.
     */
    private fun fuseTexturesForCharacter(
        group: NamedCharacterFusionGroup,
        palette: List<ExtractedColor>,
        primarySpriteFrame: Bitmap?,
        fusionNotes: MutableList<String>
    ): Triple<Bitmap, String, AssetSourceOrigin> {
        val textures = group.textureAssets.mapNotNull { it.bitmap }
        val anyImported = group.textureAssets.any { it.isUserImported }
        val conceptBmp = group.conceptAssets.firstOrNull()?.bitmap

        val size = 128
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (textures.isNotEmpty()) {
            // Draw primary texture as base layer
            val baseScaled = Bitmap.createScaledBitmap(textures.first(), size, size, true)
            canvas.drawBitmap(baseScaled, 0f, 0f, null)

            // If multiple textures exist for the same character, composite secondary texture into armor trim quadrants
            if (textures.size >= 2) {
                val secScaled = Bitmap.createScaledBitmap(textures[1], 60, 60, true)
                canvas.drawBitmap(secScaled, 64f, 4f, null)
                fusionNotes.add(
                    "MULTI-TEXTURE UV FUSION: Composited ${textures.size} character textures (${group.textureAssets.joinToString { it.assetTitle }}) with '${group.characterName}' emblem & palette trim."
                )
            } else {
                fusionNotes.add(
                    "UV TEXTURE & EMBLEM FUSION: Fused '${group.textureAssets.first().assetTitle}' with '${group.characterName}' sprite insignia and Master Palette swatch strip."
                )
            }

            // Stamp character sprite/concept emblem into bottom-right UV decal zone
            val decal = primarySpriteFrame ?: conceptBmp
            if (decal != null) {
                val scaledDecal = Bitmap.createScaledBitmap(decal, 44, 44, false)
                paint.color = Color.argb(180, 9, 13, 22)
                canvas.drawRect(76f, 76f, 124f, 124f, paint)
                canvas.drawBitmap(scaledDecal, 78f, 78f, null)
            }

            // Stamp unified Character DNA palette strip along bottom-left UV zone
            if (palette.isNotEmpty()) {
                val swW = 64f / palette.size
                palette.forEachIndexed { idx, ec ->
                    paint.color = ec.colorInt
                    canvas.drawRect(6f + idx * swW, 112f, 6f + (idx + 1) * swW, 124f, paint)
                }
            }

            val texName = "${group.characterName} [Fused UV Atlas]"
            return Triple(out, texName, if (anyImported) AssetSourceOrigin.USER_IMPORTED else AssetSourceOrigin.DERIVED_FROM_IMPORTS)
        } else {
            val baked = AssetSuiteSynthesizer.bakeSuiteTextureAtlas(palette, primarySpriteFrame, conceptBmp)
            fusionNotes.add("Baked 128×128 Character UV Texture Atlas from '${group.characterName}' Master Palette & Sprite.")
            return Triple(baked, "${group.characterName} [Baked UV Atlas]", AssetSourceOrigin.DERIVED_FROM_IMPORTS)
        }
    }

    private fun extractCharacterMasterPalette(
        group: NamedCharacterFusionGroup,
        fusedMesh: Mesh3D
    ): List<ExtractedColor> {
        val concept = group.conceptAssets.firstOrNull()?.bitmap
        val sprite = group.spriteAssets.firstOrNull()?.spriteSheet?.frames?.firstOrNull()
        val texture = group.textureAssets.firstOrNull()?.bitmap
        return AssetSuiteSynthesizer.extractMasterSuitePalette(
            concept = concept,
            sprite = sprite,
            texture = texture,
            mesh = fusedMesh
        )
    }

    private fun harmonizeAndUVWrapFusedMesh(
        mesh: Mesh3D,
        palette: List<ExtractedColor>,
        unifyPalette: Boolean
    ): Mesh3D {
        val paletteInts = palette.map { it.colorInt }
        val uvs = if (mesh.uvs.size < 4) {
            listOf(UV(0f, 0f), UV(1f, 0f), UV(1f, 1f), UV(0f, 1f))
        } else {
            mesh.uvs
        }

        val updatedFaces = mesh.faces.mapIndexed { idx, face ->
            val harmonizedColor = if (unifyPalette && paletteInts.isNotEmpty()) {
                val orig = face.color.toInt()
                val r = Color.red(orig)
                val g = Color.green(orig)
                val b = Color.blue(orig)
                val nearest = paletteInts.minByOrNull { pc ->
                    val dr = r - Color.red(pc)
                    val dg = g - Color.green(pc)
                    val db = b - Color.blue(pc)
                    dr * dr + dg * dg + db * db
                } ?: orig
                // Blend 60% original material contrast with 40% unified character palette
                val br = (r * 0.45f + Color.red(nearest) * 0.55f).toInt().coerceIn(0, 255)
                val bg = (g * 0.45f + Color.green(nearest) * 0.55f).toInt().coerceIn(0, 255)
                val bb = (b * 0.45f + Color.blue(nearest) * 0.55f).toInt().coerceIn(0, 255)
                (0xFFL shl 24) or (br.toLong() shl 16) or (bg.toLong() shl 8) or bb.toLong()
            } else {
                face.color
            }

            val uvIndices = face.uvIndices ?: IntArray(face.indices.size) { corner -> corner % uvs.size }
            face.copy(color = harmonizedColor, uvIndices = uvIndices)
        }

        return mesh.copy(faces = updatedFaces, uvs = uvs)
    }

    private fun buildCharacterFusionConceptBoard(
        charName: String,
        conceptBmp: Bitmap?,
        spriteBmp: Bitmap?,
        textureBmp: Bitmap,
        palette: List<ExtractedColor>
    ): Bitmap {
        val size = 192
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(9, 14, 24)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Subtle UV texture backdrop in top-right
        val scaledTex = Bitmap.createScaledBitmap(textureBmp, 82, 82, true)
        canvas.drawBitmap(scaledTex, 102f, 8f, null)

        if (conceptBmp != null) {
            val scaledConcept = Bitmap.createScaledBitmap(conceptBmp, 88, 88, true)
            canvas.drawBitmap(scaledConcept, 8f, 8f, null)
        }

        if (spriteBmp != null) {
            val scaledSprite = Bitmap.createScaledBitmap(spriteBmp, 84, 84, false)
            canvas.drawBitmap(scaledSprite, 8f, 96f, null)
        }

        // Draw palette swatches on bottom right
        palette.take(6).forEachIndexed { i, ec ->
            paint.color = ec.colorInt
            val x = 102f + (i % 3) * 28f
            val y = 102f + (i / 3) * 38f
            canvas.drawRect(x, y, x + 24f, y + 32f, paint)
        }

        return out
    }

    /**
     * Creates a secondary 3D weapon/wing attachment mesh for Kaelen so multi-mesh character fusion
     * can be demonstrated immediately out of the box.
     */
    private fun createKaelenTwinBladesAttachment(): Mesh3D {
        val v = listOf(
            // Left Plasma Katana Wing
            Vector3(-0.95f, 0.2f, -0.3f),
            Vector3(-1.55f, 1.35f, -0.5f),
            Vector3(-1.35f, 1.45f, -0.4f),
            Vector3(-0.85f, 0.35f, -0.25f),
            // Right Plasma Katana Wing
            Vector3(0.95f, 0.2f, -0.3f),
            Vector3(1.55f, 1.35f, -0.5f),
            Vector3(1.35f, 1.45f, -0.4f),
            Vector3(0.85f, 0.35f, -0.25f)
        )
        val neonMagenta = 0xFFFF2A85
        val cyanEdge = 0xFF00E5FF
        val f = listOf(
            Face3D(intArrayOf(0, 1, 2), color = neonMagenta),
            Face3D(intArrayOf(0, 2, 3), color = cyanEdge),
            Face3D(intArrayOf(4, 6, 5), color = neonMagenta),
            Face3D(intArrayOf(4, 7, 6), color = cyanEdge)
        )
        return Mesh3D("Kaelen Twin Plasma Katana [3D Sub-Mesh]", v, f)
    }

    /**
     * Creates a dorsal booster pack attachment mesh for Valkyrie-09.
     */
    private fun createValkyrieBoosterPackAttachment(): Mesh3D {
        val v = listOf(
            Vector3(-1.1f, 0.6f, -0.5f),
            Vector3(-1.8f, 1.2f, -0.7f),
            Vector3(-1.6f, 0.1f, -0.6f),
            Vector3(1.1f, 0.6f, -0.5f),
            Vector3(1.8f, 1.2f, -0.7f),
            Vector3(1.6f, 0.1f, -0.6f)
        )
        val gold = 0xFFF59E0B
        val cyan = 0xFF00E5FF
        val f = listOf(
            Face3D(intArrayOf(0, 1, 2), color = gold),
            Face3D(intArrayOf(3, 5, 4), color = cyan)
        )
        return Mesh3D("Valkyrie-09 Orbital Wing Pylons [3D Sub-Mesh]", v, f)
    }

    /**
     * Builds the initial multi-character asset pool with shared Named Characters ("Kaelen", "Valkyrie-09", "Gloom Slime")
     * so automatic character fusion is immediately active and demonstrable.
     */
    fun createInitialCharacterAssetPool(): List<StagedCharacterAsset> {
        val ninjaSheet = SampleSpriteSheets.createCyberNinjaSheet()
        val slimeSheet = SampleSpriteSheets.createDungeonSlimeSheet()
        val mechMesh = ProceduralMeshes.createCyberMech()
        val starfighterMesh = ProceduralMeshes.createSciFiStarfighter()
        val crystalMesh = ProceduralMeshes.createCrystalArtifact()
        val cyberTex = ProceduralTextures.createCyberGrid()
        val AllPresets = ProceduralTextures.getAllPresets()
        val metalTex = AllPresets.getOrNull(1)?.bitmap ?: cyberTex
        val lavaTex = AllPresets.getOrNull(2)?.bitmap ?: cyberTex

        return listOf(
            // Character 1: Kaelen (4 matching assets across 3D Body, 3D Weapon Attachment, 2D Spritesheet, 3D Texture, Lore)
            StagedCharacterAsset(
                id = "init_kaelen_mesh_1",
                assetTitle = "Kaelen - Shinobi Armor Rig.obj",
                characterName = "Kaelen",
                modality = CharacterAssetModality.MESH_3D,
                isUserImported = false,
                mesh = mechMesh.copy(name = "Kaelen Shinobi Armor Rig")
            ),
            StagedCharacterAsset(
                id = "init_kaelen_mesh_2",
                assetTitle = "Kaelen - Twin Plasma Katana.obj",
                characterName = "Kaelen",
                modality = CharacterAssetModality.MESH_3D,
                isUserImported = false,
                mesh = createKaelenTwinBladesAttachment()
            ),
            StagedCharacterAsset(
                id = "init_kaelen_sprite",
                assetTitle = "Kaelen_CyberNinja_Sheet.png",
                characterName = "Kaelen",
                modality = CharacterAssetModality.SPRITESHEET_2D,
                isUserImported = false,
                spriteSheet = ninjaSheet.copy(name = "Kaelen Cyber Ninja (12f)")
            ),
            StagedCharacterAsset(
                id = "init_kaelen_tex",
                assetTitle = "Kaelen_CarbonWeave_UV.png",
                characterName = "Kaelen",
                modality = CharacterAssetModality.TEXTURE_UV,
                isUserImported = false,
                bitmap = cyberTex
            ),
            StagedCharacterAsset(
                id = "init_kaelen_lore",
                assetTitle = "Kaelen Character Dossier",
                characterName = "Kaelen",
                modality = CharacterAssetModality.LORE_SPEC,
                isUserImported = false,
                loreText = "Kaelen: Vanguard cyber-shinobi equipped with twin high-frequency plasma katanas and stealth optical camo."
            ),

            // Character 2: Valkyrie-09 (4 matching assets)
            StagedCharacterAsset(
                id = "init_valkyrie_mesh_1",
                assetTitle = "Valkyrie-09_AssaultChassis.obj",
                characterName = "Valkyrie-09",
                modality = CharacterAssetModality.MESH_3D,
                isUserImported = false,
                mesh = starfighterMesh.copy(name = "Valkyrie-09 Core Frame")
            ),
            StagedCharacterAsset(
                id = "init_valkyrie_mesh_2",
                assetTitle = "Valkyrie-09_WingPylons.obj",
                characterName = "Valkyrie-09",
                modality = CharacterAssetModality.MESH_3D,
                isUserImported = false,
                mesh = createValkyrieBoosterPackAttachment()
            ),
            StagedCharacterAsset(
                id = "init_valkyrie_tex",
                assetTitle = "Valkyrie-09_TitaniumPlate.png",
                characterName = "Valkyrie-09",
                modality = CharacterAssetModality.TEXTURE_UV,
                isUserImported = false,
                bitmap = metalTex
            ),
            StagedCharacterAsset(
                id = "init_valkyrie_lore",
                assetTitle = "Valkyrie-09 Technical Spec",
                characterName = "Valkyrie-09",
                modality = CharacterAssetModality.LORE_SPEC,
                isUserImported = false,
                loreText = "Valkyrie-09: Orbital intercept strider with twin dorsal booster pylons and reactive titanium plating."
            ),

            // Character 3: Gloom Slime (3 matching assets)
            StagedCharacterAsset(
                id = "init_slime_mesh",
                assetTitle = "Gloom Slime_CoreMonolith.obj",
                characterName = "Gloom Slime",
                modality = CharacterAssetModality.MESH_3D,
                isUserImported = false,
                mesh = crystalMesh.copy(name = "Gloom Slime Core")
            ),
            StagedCharacterAsset(
                id = "init_slime_sprite",
                assetTitle = "Gloom Slime_BounceSheet.png",
                characterName = "Gloom Slime",
                modality = CharacterAssetModality.SPRITESHEET_2D,
                isUserImported = false,
                spriteSheet = slimeSheet.copy(name = "Gloom Slime (8f)")
            ),
            StagedCharacterAsset(
                id = "init_slime_tex",
                assetTitle = "Gloom Slime_BioGel_UV.png",
                characterName = "Gloom Slime",
                modality = CharacterAssetModality.TEXTURE_UV,
                isUserImported = false,
                bitmap = lavaTex
            )
        )
    }
}
