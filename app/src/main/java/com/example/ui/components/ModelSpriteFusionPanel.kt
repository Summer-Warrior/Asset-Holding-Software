package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.fusion.ModelSpriteFusionMode
import com.example.model.fusion.ModelSpriteFusionResult
import com.example.model.hardware.ReadingLevelMode
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ProceduralMeshes
import com.example.model.sprite.SampleSpriteSheets
import com.example.model.sprite.SpriteSheetData
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import kotlin.math.roundToInt

/**
 * Interactive 3D Model + 2D Spritesheet Hybrid Asset Fusion Lab Panel.
 * Allows users to select any 3D Model and any 2D Spritesheet, choose one of 5 fusion algorithms,
 * tune blend ratio & relief depth, and fuse them into a unified hybrid game asset.
 */
@Composable
fun ModelSpriteFusionPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Build candidate list of 3D Models (Active, Library/Staged, and Studio Archetypes)
    val availableMeshes: List<Pair<String, Mesh3D>> = remember(
        uiState.currentMesh,
        uiState.savedLibraryAssets,
        uiState.stagedCharacterPool
    ) {
        buildList {
            add("Active: ${uiState.currentMesh.name.take(20)}" to uiState.currentMesh)
            uiState.savedLibraryAssets.mapNotNull { it.mesh }.take(3).forEach { m ->
                add("Lib: ${m.name.take(18)}" to m)
            }
            add("Starfighter NX-3" to ProceduralMeshes.createSciFiStarfighter())
            add("Cyber Mech Strider" to ProceduralMeshes.createCyberMech())
            add("Dungeon Mimic Chest" to ProceduralMeshes.createTreasureChest())
            add("Aether Crystal" to ProceduralMeshes.createCrystalArtifact())
        }.distinctBy { it.first }
    }

    // Build candidate list of 2D Spritesheets (Active, Library/Staged, and Studio Presets)
    val availableSprites: List<Pair<String, SpriteSheetData>> = remember(
        uiState.currentSpriteSheet,
        uiState.savedLibraryAssets,
        uiState.stagedCharacterPool
    ) {
        buildList {
            add("Active: ${uiState.currentSpriteSheet.name.take(20)}" to uiState.currentSpriteSheet)
            uiState.savedLibraryAssets.mapNotNull { it.spriteSheet }.take(3).forEach { s ->
                add("Lib: ${s.name.take(18)}" to s)
            }
            add("Cyber Ninja Sheet" to SampleSpriteSheets.createCyberNinjaSheet())
            add("Dungeon Slime Sheet" to SampleSpriteSheets.createDungeonSlimeSheet())
        }.distinctBy { it.first }
    }

    var selectedMeshIndex by remember { mutableIntStateOf(0) }
    var selectedSpriteIndex by remember { mutableIntStateOf(0) }

    val chosenMesh = availableMeshes.getOrElse(selectedMeshIndex) { availableMeshes.first() }.second
    val chosenSprite = availableSprites.getOrElse(selectedSpriteIndex) { availableSprites.first() }.second

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091322)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("model_sprite_fusion_panel")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MergeType,
                        contentDescription = "3D + 2D Fusion",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "3D MODEL × 2D SPRITESHEET ASSET FUSION LAB",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "Fuse any 3D Mesh and 2D Spritesheet into a hybrid 3D+2D asset with UV projection & relief grafting",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    color = CyanNeon.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = uiState.selectedModelSpriteFusionMode.badge,
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Step 1: Select Source 3D Model to Fuse
            Text(
                text = "1. SELECT SOURCE 3D MODEL (${chosenMesh.vertexCount}v / ${chosenMesh.faceCount}f)",
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = CyanNeon
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableMeshes.forEachIndexed { idx, (label, _) ->
                    val selected = selectedMeshIndex == idx
                    FilterChip(
                        selected = selected,
                        onClick = { selectedMeshIndex = idx },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text(label, fontFamily = TechMono, fontSize = 10.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon,
                            selectedLabelColor = Color(0xFF031B26),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("fusion_pick_mesh_$idx")
                    )
                }
            }

            // Step 2: Select Source 2D Spritesheet to Fuse
            Text(
                text = "2. SELECT SOURCE 2D SPRITESHEET (${chosenSprite.frames.size} frames • ${chosenSprite.columns}×${chosenSprite.rows})",
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = MagentaNeon
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSprites.forEachIndexed { idx, (label, _) ->
                    val selected = selectedSpriteIndex == idx
                    FilterChip(
                        selected = selected,
                        onClick = { selectedSpriteIndex = idx },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text(label, fontFamily = TechMono, fontSize = 10.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MagentaNeon,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("fusion_pick_sprite_$idx")
                    )
                }
            }

            // Step 3: Select 3D + 2D Fusion Algorithm Mode
            Text(
                text = "3. SELECT 3D × 2D FUSION PIPELINE MODE",
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = AmberGold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelSpriteFusionMode.entries.forEach { mode ->
                    val selected = uiState.selectedModelSpriteFusionMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setModelSpriteFusionMode(mode) },
                        label = {
                            Text(
                                text = mode.title,
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color(0xFF031B26),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("fusion_mode_chip_${mode.name.lowercase()}")
                    )
                }
            }
            Text(
                text = uiState.selectedModelSpriteFusionMode.summary,
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )

            // Step 4: Blend Ratio, Relief Depth & Palette Harmonization Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF060B15))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "2D Sprite ↔ 3D Mesh Dominance Blend:",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${(uiState.modelSpriteBlendRatio * 100).roundToInt()}% Sprite / ${100 - (uiState.modelSpriteBlendRatio * 100).roundToInt()}% 3D",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                }
                Slider(
                    value = uiState.modelSpriteBlendRatio,
                    onValueChange = { viewModel.setModelSpriteBlendRatio(it) },
                    valueRange = 0.15f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanNeon,
                        activeTrackColor = CyanNeon,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("fusion_blend_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sprite-to-3D Relief Extrusion Depth:",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${"%.2f".format(uiState.modelSpriteReliefDepth)}x",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagentaNeon
                    )
                }
                Slider(
                    value = uiState.modelSpriteReliefDepth,
                    onValueChange = { viewModel.setModelSpriteReliefDepth(it) },
                    valueRange = 0.20f..1.20f,
                    colors = SliderDefaults.colors(
                        thumbColor = MagentaNeon,
                        activeTrackColor = MagentaNeon,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("fusion_depth_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unify Shared CLUT Palette Across 3D & 2D Output",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                    Switch(
                        checked = uiState.modelSpriteHarmonizePalette,
                        onCheckedChange = { viewModel.setModelSpriteHarmonizePalette(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen
                        ),
                        modifier = Modifier.testTag("fusion_harmonize_palette_switch")
                    )
                }

                OutlinedTextField(
                    value = uiState.customModelSpriteFusionName,
                    onValueChange = { viewModel.setCustomModelSpriteFusionName(it) },
                    label = {
                        Text(
                            "Optional Custom Fused Asset Name",
                            fontFamily = TechMono,
                            fontSize = 10.sp
                        )
                    },
                    placeholder = {
                        Text(
                            "${chosenMesh.name.take(14)} × ${chosenSprite.name.take(14)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fusion_custom_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )
            }

            // Primary Action Buttons: Fuse Selected Mode | Compare All 5 Modes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.fuseModelAndSpriteSheetToMakeAsset(
                            meshOverride = chosenMesh,
                            spriteOverride = chosenSprite,
                            applyToStudioImmediately = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color(0xFF031B26)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("execute_model_sprite_fusion_btn")
                ) {
                    Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Fuse 3D + Sprite",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.batchFuseAllModelSpriteModes(
                            meshOverride = chosenMesh,
                            spriteOverride = chosenSprite
                        )
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("batch_all_model_sprite_fusions_btn")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Fuse All 5 Modes",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Active 3D + 2D Fused Asset Readout & Preview
            val activeFusion = uiState.activeModelSpriteFusion
            if (activeFusion != null) {
                HorizontalDivider(color = CardBorder)
                ActiveModelSpriteFusionReadout(
                    fusion = activeFusion,
                    hardwareShortName = uiState.selectedHardware.shortName,
                    readingMode = uiState.readingLevelMode,
                    onApplyToStudio = { viewModel.applyModelSpriteFusionResult(activeFusion) },
                    onSaveToLibrary = { viewModel.saveModelSpriteFusionToLibrary(activeFusion) }
                )
            }

            // Gallery of Synthesized 3D + 2D Fused Assets
            if (uiState.savedModelSpriteFusions.size > 1) {
                Text(
                    text = "SYNTHESIZED 3D × 2D FUSED ASSETS (${uiState.savedModelSpriteFusions.size})",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.savedModelSpriteFusions.forEachIndexed { idx, item ->
                        val isCurrent = activeFusion?.id == item.id
                        AssetPreviewCard(
                            title = item.fusedName,
                            assetTypeTag = "3D + SPRITE FUSION • ${item.fusionMode.badge}",
                            metadataSummary = "${item.fusedMesh.vertexCount}v/${item.fusedMesh.faceCount}f + ${item.fusedSpriteSheet.frames.size}f (${item.fusedSpriteSheet.columns}×${item.fusedSpriteSheet.rows}) • Harmony ${item.harmonyScore}%",
                            subtitleOrCharacterTag = item.characterTag,
                            hardwareBadge = uiState.selectedHardware.shortName,
                            sourceBadge = "3D+2D FUSION",
                            metadataTags = listOf(
                                "3D: ${item.sourceMeshName.take(18)}",
                                "2D: ${item.sourceSpriteName.take(18)}",
                                "Blend ${(item.blendRatio * 100).roundToInt()}%"
                            ),
                            meshPreview = item.fusedMesh,
                            bitmapPreview = item.fusedSpriteSheet.frames.firstOrNull(),
                            isSelected = isCurrent,
                            testTagPrefix = "saved_ms_fusion_card_$idx",
                            primaryActionLabel = if (isCurrent) "Active in Studio (Re-Sync)" else "Load Fused Asset into Studios",
                            onPrimaryAction = { viewModel.applyModelSpriteFusionResult(item) },
                            onClick = { viewModel.applyModelSpriteFusionResult(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveModelSpriteFusionReadout(
    fusion: ModelSpriteFusionResult,
    hardwareShortName: String,
    readingMode: ReadingLevelMode,
    onApplyToStudio: () -> Unit,
    onSaveToLibrary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF050C18))
            .border(1.dp, EmeraldGreen.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("active_model_sprite_fusion_readout"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Text(
                    text = "ACTIVE 3D × 2D FUSED ASSET",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }
            Surface(
                color = EmeraldGreen.copy(alpha = 0.16f),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text(
                    text = "${fusion.harmonyScore}% HARMONY",
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    color = EmeraldGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Reusable AssetPreviewCard for the Active Fused Asset
        AssetPreviewCard(
            title = fusion.fusedName,
            assetTypeTag = "3D + SPRITE FUSION • ${fusion.fusionMode.badge}",
            metadataSummary = "${fusion.fusedMesh.vertexCount} Vertices • ${fusion.fusedMesh.faceCount} Faces • ${fusion.fusedSpriteSheet.frames.size} Fused Frames",
            subtitleOrCharacterTag = fusion.characterTag,
            hardwareBadge = hardwareShortName,
            sourceBadge = "3D+2D FUSION",
            metadataTags = listOf(
                "3D Source: ${fusion.sourceMeshName.take(18)}",
                "Sprite Source: ${fusion.sourceSpriteName.take(18)}",
                "Blend ${(fusion.blendRatio * 100).roundToInt()}%",
                "Depth ${"%.2f".format(fusion.reliefDepth)}x"
            ),
            meshPreview = fusion.fusedMesh,
            bitmapPreview = fusion.fusedSpriteSheet.frames.firstOrNull(),
            isSelected = true,
            testTagPrefix = "active_ms_fusion_preview_card"
        )

        // Baked UV Atlas & Fused Spritesheet Strip Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A1220))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = fusion.fusedTextureAtlas.asImageBitmap(),
                        contentDescription = "Baked 3D UV Atlas",
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(2.dp)
                    )
                }
                Text("Baked UV Atlas", fontFamily = TechMono, fontSize = 8.sp, color = CyanNeon)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "FUSED SPRITESHEET FRAMES (${fusion.fusedSpriteSheet.frames.size})",
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    color = MagentaNeon
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fusion.fusedSpriteSheet.frames.take(8).forEachIndexed { idx, frameBmp ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0A1220))
                                .border(1.dp, MagentaNeon.copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = frameBmp.asImageBitmap(),
                                contentDescription = "Fused Frame ${idx + 1}",
                                filterQuality = FilterQuality.None,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dual-Level Readout (Everyday + High-End Technical)
        if (readingMode == ReadingLevelMode.EVERYDAY_READER || readingMode == ReadingLevelMode.BOTH_LEVELS) {
            Text(
                text = "EVERYDAY FUSION SUMMARY: ${fusion.everydayReadout}",
                fontSize = 11.sp,
                color = Color(0xFFE2E8F0)
            )
        }
        if (readingMode == ReadingLevelMode.HIGH_END_TECHNICAL || readingMode == ReadingLevelMode.BOTH_LEVELS) {
            Text(
                text = fusion.technicalReadout,
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = CyanNeon
            )
        }

        // Action Row: Apply to Studios | Save to Library
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSaveToLibrary,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("save_active_ms_fusion_btn")
            ) {
                Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Save to Library", fontFamily = TechMono, fontSize = 10.sp)
            }

            Button(
                onClick = onApplyToStudio,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("apply_active_ms_fusion_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Sync to 3D & 2D", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
