package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignVerticalBottom
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.masterpack.AnchorPointType
import com.example.model.masterpack.FootAlignedSlicerEngine
import com.example.model.masterpack.FrameCadenceFps
import com.example.model.masterpack.HardwareVisualOutputTier
import com.example.model.masterpack.NormalizedAnchor
import com.example.model.masterpack.TWENTY_COLOR_WHEEL
import com.example.model.masterpack.ThreeMapRecolorEngine
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

/**
 * 6-Layer Visual/Rig Master Pack Studio Panel:
 * 1. 20-Color Wheel (boots with 0 selected) & Live 3-Map + Depth/Normal Recolor Studio
 * 2. Foot-Aligned Slicer & `rig_meta.json` Anchor / Hitbox / Hurtbox Editor
 * 3. Hardware Visual Output Split Compiler (16-Bit Pixel, 32-Bit Arcade 60 FPS, True Fusion HD, 2.5D/3D Normal-Lit)
 */
@Composable
fun MasterAssetPipelinePanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeFrame = remember(uiState.currentSpriteSheet) {
        uiState.currentSpriteSheet.frames.firstOrNull() ?: uiState.currentSpriteSheet.sourceBitmap
    }

    val threeMapLayers = remember(activeFrame) {
        ThreeMapRecolorEngine.decomposeSpriteToThreeMaps(activeFrame)
    }

    val liveRecoloredPreview = remember(
        threeMapLayers,
        uiState.selectedColorWheelHexes,
        uiState.activeMasterOutputTier
    ) {
        ThreeMapRecolorEngine.compositeRecoloredSprite(
            layers = threeMapLayers,
            selectedWheelHexes = uiState.selectedColorWheelHexes,
            tier = uiState.activeMasterOutputTier
        )
    }

    val compiledOutputs = remember(
        threeMapLayers,
        uiState.selectedColorWheelHexes,
        uiState.selectedHardware,
        uiState.activeRigMetadata.cadenceFps
    ) {
        FootAlignedSlicerEngine.compileHardwareVisualSplit(
            layers = threeMapLayers,
            selectedWheelHexes = uiState.selectedColorWheelHexes,
            targetHardware = uiState.selectedHardware,
            rigMeta = uiState.activeRigMetadata
        )
    }

    var selectedAnchorType by remember { mutableStateOf(AnchorPointType.FEET_PIVOT) }
    val activeAnchor = uiState.activeRigMetadata.anchors[selectedAnchorType]
        ?: NormalizedAnchor.of(selectedAnchorType, 0.5f, 0.5f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1120)),
        border = BorderStroke(1.3.dp, CyanNeon.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("master_asset_pipeline_panel")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                        imageVector = Icons.Default.Layers,
                        contentDescription = "6-Layer Master Pack Pipeline",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "6-LAYER VISUAL / RIG MASTER PACK",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                        Text(
                            text = "3-Map Recolor • 20-Color Wheel • Foot-Aligned Rig • Hardware Output Split",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Surface(
                    color = MagentaNeon.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${uiState.selectedColorWheelHexes.size} / 4 SELECTED",
                        fontFamily = TechMono,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagentaNeon,
                        modifier = Modifier
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("color_wheel_count_badge")
                    )
                }
            }

            // =================================================================
            // SECTION 1: 20-COLOR WHEEL & 3-MAP RECOLOR STUDIO
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "20-Color Wheel",
                            tint = MagentaNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "1. 20-COLOR WHEEL & 3-MAP RECOLOR",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearColorWheelSelection() },
                        border = BorderStroke(1.dp, CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("color_wheel_reset_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset (0 Selected)", fontFamily = TechMono, fontSize = 9.sp)
                    }
                }

                // 20-Color Wheel arranged cleanly in 2 rows of 10 compact tactile swatches
                val firstRow = remember { TWENTY_COLOR_WHEEL.take(10) }
                val secondRow = remember { TWENTY_COLOR_WHEEL.drop(10) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(firstRow, secondRow).forEach { rowEntries ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowEntries.forEach { entry ->
                                val selectedIdx = uiState.selectedColorWheelHexes.indexOfFirst {
                                    it.equals(entry.hex, ignoreCase = true)
                                }
                                val isSelected = selectedIdx >= 0
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF070C17))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) CyanNeon else CardBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.toggleColorWheelHex(entry.hex) }
                                        .testTag("color_wheel_swatch_${entry.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(entry.colorInt))
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.45f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected ${entry.name}",
                                                tint = if (entry.hex == "#F8FAFC" || entry.hex == "#FACC15" || entry.hex == "#D5DEE8") {
                                                    Color(0xFF0A1120)
                                                } else {
                                                    Color.White
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4 Live Decomposed Layer Thumbnails + Live Recolored Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LayerThumbTile("color_id", threeMapLayers.colorIdMap, CyanNeon)
                    LayerThumbTile("ink_shadow", threeMapLayers.inkShadowMap, Color(0xFF94A3B8))
                    LayerThumbTile("mat_emissive", threeMapLayers.matEmissiveMap, MagentaNeon)
                    LayerThumbTile("depth_norm", threeMapLayers.depthNormMap, EmeraldGreen)
                    LayerThumbTile("LIVE OUTPUT", liveRecoloredPreview, AmberGold, highlighted = true)
                }

                Button(
                    onClick = { viewModel.applyThreeMapRecolorToActiveSprite() },
                    colors = ButtonDefaults.buttonColors(containerColor = MagentaNeon, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("bake_three_map_recolor_btn")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Bake Recolor to Sheet (${uiState.activeMasterOutputTier.displayName})",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // =================================================================
            // SECTION 2: FOOT-ALIGNED SLICER & rig_meta.json ANCHOR EDITOR
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlignVerticalBottom,
                            contentDescription = "Foot-Aligned Slicer",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "2. FOOT-ALIGNED SLICER & RIG ANCHORS",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.prepareRigMetaJsonExport() },
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("export_rig_meta_json_btn")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("rig_meta.json", fontFamily = TechMono, fontSize = 9.sp)
                    }
                }

                Button(
                    onClick = { viewModel.footAlignActiveSpriteSheet() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color(0xFF022015)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("foot_align_all_frames_btn")
                ) {
                    Icon(Icons.Default.AlignVerticalBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Foot-Align All Frames (${uiState.currentSpriteSheet.frames.size} Frames)",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                uiState.lastFootAlignmentSummary?.let { summary ->
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = summary,
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Frame Cadence FPS Chips (12 / 15 / 30 / 60 FPS)
                Text("FRAME CADENCE:", fontFamily = TechMono, fontSize = 9.sp, color = CyanNeon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FrameCadenceFps.entries.forEach { cadence ->
                        val selected = uiState.activeRigMetadata.cadenceFps == cadence
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateRigCadenceFps(cadence) },
                            label = { Text(cadence.label, fontFamily = TechMono, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF162135),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("rig_cadence_${cadence.fps}")
                        )
                    }
                }

                // Anchor Point Selector Chips (7 sockets)
                Text("RIG & FUSION ANCHOR SOCKETS:", fontFamily = TechMono, fontSize = 9.sp, color = AmberGold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnchorPointType.entries.forEach { anchorType ->
                        val selected = selectedAnchorType == anchorType
                        FilterChip(
                            selected = selected,
                            onClick = { selectedAnchorType = anchorType },
                            label = { Text(anchorType.label, fontFamily = TechMono, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold,
                                selectedLabelColor = Color(0xFF1E1300),
                                containerColor = Color(0xFF162135),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("rig_anchor_chip_${anchorType.name.lowercase()}")
                        )
                    }
                }

                // Live Crosshair + Hurtbox/Hitbox Overlay over Active Frame Preview + X/Y Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF060A13))
                            .border(1.dp, CyanNeon.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = liveRecoloredPreview.asImageBitmap(),
                            contentDescription = "Rig Anchor Preview Frame",
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        val rig = uiState.activeRigMetadata
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Hurtbox (Emerald)
                            drawRect(
                                color = Color(0xFF10B981).copy(alpha = 0.85f),
                                topLeft = Offset(rig.hurtbox.xNorm * size.width, rig.hurtbox.yNorm * size.height),
                                size = Size(rig.hurtbox.wNorm * size.width, rig.hurtbox.hNorm * size.height),
                                style = Stroke(width = 2f)
                            )
                            // Hitbox (Magenta)
                            drawRect(
                                color = Color(0xFFFF2A85).copy(alpha = 0.85f),
                                topLeft = Offset(rig.hitbox.xNorm * size.width, rig.hitbox.yNorm * size.height),
                                size = Size(rig.hitbox.wNorm * size.width, rig.hitbox.hNorm * size.height),
                                style = Stroke(width = 2f)
                            )
                            // Feet pivot baseline
                            val feetY = rig.feetPivotYNorm * size.height
                            drawLine(
                                color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                                start = Offset(0f, feetY),
                                end = Offset(size.width, feetY),
                                strokeWidth = 1.5f
                            )
                            // All sockets as subtle dots
                            rig.anchors.values.forEach { pt ->
                                val cx = pt.xNorm * size.width
                                val cy = pt.yNorm * size.height
                                val isCurrent = pt.type == selectedAnchorType
                                if (isCurrent) {
                                    drawLine(Color(0xFFFACC15), Offset(cx - 10f, cy), Offset(cx + 10f, cy), strokeWidth = 2.5f)
                                    drawLine(Color(0xFFFACC15), Offset(cx, cy - 10f), Offset(cx, cy + 10f), strokeWidth = 2.5f)
                                    drawCircle(Color(0xFFFACC15), radius = 5f, center = Offset(cx, cy), style = Stroke(width = 2f))
                                } else {
                                    drawCircle(Color(0xFF38BDF8).copy(alpha = 0.8f), radius = 3f, center = Offset(cx, cy))
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${selectedAnchorType.label} X: ${String.format("%.2f", activeAnchor.xNorm)}",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                        Slider(
                            value = activeAnchor.xNorm,
                            onValueChange = { newX ->
                                viewModel.updateRigAnchor(selectedAnchorType, newX, activeAnchor.yNorm)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon),
                            modifier = Modifier.testTag("rig_anchor_x_slider")
                        )
                        Text(
                            text = "${selectedAnchorType.label} Y: ${String.format("%.2f", activeAnchor.yNorm)}",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                        Slider(
                            value = activeAnchor.yNorm,
                            onValueChange = { newY ->
                                viewModel.updateRigAnchor(selectedAnchorType, activeAnchor.xNorm, newY)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold),
                            modifier = Modifier.testTag("rig_anchor_y_slider")
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

            // =================================================================
            // SECTION 3: HARDWARE VISUAL OUTPUT SPLIT PREVIEW
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. HARDWARE VISUAL OUTPUT SPLIT (4 TIERS)",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.prepareMasterPackManifestExport() },
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("export_master_pack_json_btn")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Master Pack JSON", fontFamily = TechMono, fontSize = 9.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    compiledOutputs.forEach { output ->
                        val isActiveTier = uiState.activeMasterOutputTier == output.tier
                        Column(
                            modifier = Modifier
                                .width(134.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF070C17))
                                .border(
                                    width = if (isActiveTier) 2.dp else 1.dp,
                                    color = if (isActiveTier) CyanNeon else CardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setMasterOutputTier(output.tier) }
                                .padding(8.dp)
                                .testTag("hardware_split_card_${output.tier.name.lowercase()}"),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF04070E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = output.bitmap.asImageBitmap(),
                                    contentDescription = output.tier.displayName,
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = output.tier.displayName,
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActiveTier) CyanNeon else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = EmeraldGreen.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = "${output.paletteColorCount} COLORS",
                                    fontFamily = TechMono,
                                    fontSize = 8.sp,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = output.tier.technicalSubtitle,
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerThumbTile(
    label: String,
    bitmap: android.graphics.Bitmap,
    accent: Color,
    highlighted: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF060A13))
            .border(
                width = if (highlighted) 1.5.dp else 1.dp,
                color = if (highlighted) accent else CardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF03060C)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = label,
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = label,
            fontFamily = TechMono,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
