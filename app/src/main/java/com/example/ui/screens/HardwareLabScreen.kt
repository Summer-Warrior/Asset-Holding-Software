package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.HardwareCategory
import com.example.model.hardware.HardwareReadoutGenerator
import com.example.ui.components.AssetHardwareReadoutPanel
import com.example.ui.components.ChipFusionLabPanel
import com.example.ui.components.FanChipPreviewPanel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import com.example.ui.viewer3d.Viewport3D

@Composable
fun HardwareLabScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSpec = uiState.selectedHardware
    var previewModeTab by remember { mutableIntStateOf(0) } // 0 = 3D Pipeline, 1 = 2D Sprite PPU/VDP

    val filteredSystems = remember(uiState.hardwareCatalog, uiState.hardwareFilterCategory) {
        if (uiState.hardwareFilterCategory == HardwareCategory.ALL) {
            uiState.hardwareCatalog
        } else {
            uiState.hardwareCatalog.filter { it.category == uiState.hardwareFilterCategory }
        }
    }

    val meshReport = remember(uiState.currentMesh, selectedSpec) {
        HardwareAssetSimulator.analyzeMeshOnHardware(uiState.currentMesh, selectedSpec)
    }

    val activeSpriteFrame = uiState.currentSpriteSheet.frames.firstOrNull()
    val spriteReport = remember(activeSpriteFrame, selectedSpec, uiState.showHardwareTileGrid) {
        activeSpriteFrame?.let {
            HardwareAssetSimulator.simulateSpriteOnHardware(
                sourceBitmap = it,
                spec = selectedSpec,
                showTileGrid = uiState.showHardwareTileGrid
            )
        }
    }

    val meshDossier = remember(uiState.currentMesh, selectedSpec, meshReport) {
        HardwareReadoutGenerator.generate3DMeshDossier(uiState.currentMesh, selectedSpec, meshReport)
    }

    val spriteDossier = remember(uiState.currentSpriteSheet, selectedSpec, spriteReport) {
        spriteReport?.let {
            HardwareReadoutGenerator.generate2DSpriteDossier(uiState.currentSpriteSheet, selectedSpec, it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Category Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONSOLE, HANDHELD & SILICON ARCHITECTURE LAB",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    color = CyanNeon,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Real-Time Hardware Target Emulation",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Icons.Default.DeveloperBoard,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(28.dp)
            )
        }

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HardwareCategory.entries.forEach { cat ->
                val selected = uiState.hardwareFilterCategory == cat
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setHardwareCategoryFilter(cat) },
                    label = { Text(cat.label, fontFamily = TechMono, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeon,
                        selectedLabelColor = Color(0xFF031B26),
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.testTag("hw_cat_${cat.name.lowercase()}")
                )
            }
        }

        // Hardware System Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filteredSystems.forEach { system ->
                val isSelected = system.id == selectedSpec.id
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF162C46) else Color(0xFF0F172A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AmberGold else CardBorder
                    ),
                    modifier = Modifier
                        .width(175.dp)
                        .clickable { viewModel.selectHardware(system) }
                        .testTag("hw_system_${system.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${system.releaseYear}",
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                color = AmberGold
                            )
                            Text(
                                text = system.era.label,
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = CyanNeon
                            )
                        }
                        Text(
                            text = system.shortName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = system.gpuOrPpuSummary,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Selected System Banner Summary
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedSpec.manufacturer.uppercase()} • ${selectedSpec.releaseYear}",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = AmberGold
                        )
                        Text(
                            text = selectedSpec.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = Color(meshReport.compatibility.colorHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(meshReport.compatibility.colorHex)
                        )
                    ) {
                        Text(
                            text = meshReport.compatibility.label,
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color(meshReport.compatibility.colorHex),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = CardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpecMetricColumn("Resolution", selectedSpec.nativeResolution.substringBefore(" ("))
                    SpecMetricColumn("Sprite BPP", "${selectedSpec.bitsPerPixelSprite}bpp (${selectedSpec.maxSpriteColorsPerPalette} cols)")
                    SpecMetricColumn("On-Screen Cols", "${selectedSpec.maxOnScreenColors} / ${selectedSpec.masterPaletteSize}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpecMetricColumn("CPU Core", selectedSpec.cpuSummary.take(26))
                    SpecMetricColumn("Video Silicon", selectedSpec.gpuOrPpuSummary.take(26))
                }
            }
        }

        // Asset Simulation Mode Switcher (3D Model Pipeline vs 2D Sprite PPU/VDP)
        TabRow(
            selectedTabIndex = previewModeTab,
            containerColor = Color(0xFF0F172A),
            contentColor = CyanNeon,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[previewModeTab]),
                    color = AmberGold
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = previewModeTab == 0,
                onClick = { previewModeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("3D Silicon Pipeline", fontFamily = TechMono, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("hw_tab_3d_pipeline")
            )
            Tab(
                selected = previewModeTab == 1,
                onClick = { previewModeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("2D PPU/VDP Sprite", fontFamily = TechMono, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("hw_tab_2d_sprite")
            )
        }

        if (previewModeTab == 0) {
            // ==================== 3D MODEL SILICON PIPELINE VIEW ====================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                Viewport3D(
                    mesh = uiState.currentMesh,
                    activeTexture = uiState.activeTexture,
                    hardwareSpec = selectedSpec,
                    hardwareSimulationEnabled = uiState.hardwareSimulationEnabled,
                    onToggleHardwareSimulation = { viewModel.toggleHardwareSimulation() },
                    onExportObj = { viewModel.prepareObjExport() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3D Pipeline Telemetry & Polygon Budget Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3D HARDWARE RASTERIZATION & MEMORY DIAGNOSTICS",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = CyanNeon
                    )

                    Text(
                        text = selectedSpec.model3DPipelineExplanation,
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0)
                    )

                    // Polygon Budget Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Asset Triangle Budget (${meshReport.triangleCount} / ${meshReport.recommendedTriBudget} tris)",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${String.format("%.0f", meshReport.assetBudgetPercent)}%",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                color = if (meshReport.assetBudgetPercent > 100f) AmberGold else EmeraldGreen
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (meshReport.assetBudgetPercent / 100f).coerceIn(0.02f, 1f) },
                            color = if (meshReport.assetBudgetPercent > 100f) AmberGold else CyanNeon,
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    // Memory Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecMetricColumn("Vertex RAM", "${meshReport.vertexBufferBytes} B")
                        SpecMetricColumn("Face/OT List", "${meshReport.faceTableBytes} B")
                        SpecMetricColumn("Tex Footprint", "${meshReport.estimatedTextureBytes} B")
                        SpecMetricColumn("Max Scene/Frame", "${meshReport.maxSceneTriBudget} tris")
                    }

                    Text(
                        text = meshReport.textureCacheNote,
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = AmberGold
                    )

                    // Silicon Bottlenecks List
                    meshReport.siliconBottlenecks.forEach { note ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (note.contains("exceeds") || note.contains("No ")) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (note.contains("exceeds") || note.contains("No ")) AmberGold else EmeraldGreen,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = note,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.bakeHardwareAdaptationToMesh() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = Color(0xFF031B26)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("bake_hw_mesh_btn")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Bake ${selectedSpec.shortName} Precision & Palette to 3D Mesh",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // ==================== 2D SPRITE PPU / VDP HARDWARE VIEW ====================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PPU / VDP SPRITE HARDWARE OUTPUT",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = CyanNeon
                        )
                        FilterChip(
                            selected = uiState.showHardwareTileGrid,
                            onClick = { viewModel.toggleHardwareTileGrid() },
                            label = { Text("8×8 CHR Grid", fontFamily = TechMono, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("toggle_chr_grid_chip")
                        )
                    }

                    // Side-by-Side Original vs Hardware Emulated Sprite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Source Studio Sprite
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SOURCE STUDIO", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(145.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070B12))
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activeSpriteFrame != null) {
                                    Image(
                                        bitmap = activeSpriteFrame.asImageBitmap(),
                                        contentDescription = "Original Studio Sprite",
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // Emulated PPU/VDP Output
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedSpec.shortName.uppercase(),
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                color = AmberGold
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(145.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070B12))
                                    .border(2.dp, AmberGold, RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                                    .testTag("hw_simulated_sprite_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (spriteReport != null) {
                                    Image(
                                        bitmap = spriteReport.simulatedBitmap.asImageBitmap(),
                                        contentDescription = "Hardware Quantized Sprite",
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    if (spriteReport != null) {
                        // Quantized Hardware Sub-Palette Swatches
                        Text(
                            text = "ACTIVE HARDWARE SUB-PALETTE (${spriteReport.hardwarePaletteSwatches.size} / ${selectedSpec.maxSpriteColorsPerPalette - 1} VISIBLE + TRANSPARENT)",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            spriteReport.hardwarePaletteSwatches.forEach { colorInt ->
                                val hex = String.format(
                                    "#%02X%02X%02X",
                                    android.graphics.Color.red(colorInt),
                                    android.graphics.Color.green(colorInt),
                                    android.graphics.Color.blue(colorInt)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(colorInt))
                                            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                    )
                                    Text(hex, fontFamily = TechMono, fontSize = 8.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }

                        HorizontalDivider(color = CardBorder)

                        // CHR & OAM Telemetry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SpecMetricColumn("8×8 CHR Tiles", "${spriteReport.chrTilesWide}×${spriteReport.chrTilesHigh} (${spriteReport.totalChrTiles})")
                            SpecMetricColumn("CHR VRAM Size", "${spriteReport.chrVramBytes} Bytes")
                            SpecMetricColumn("Scanline Slots", "${spriteReport.scanlineSpritesUsed} / ${spriteReport.scanlineLimitMax}")
                            SpecMetricColumn("Max OAM Sprites", "${selectedSpec.maxSpritesTotal}")
                        }

                        Text(
                            text = spriteReport.technicalVerdict,
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = if (spriteReport.causesScanlineFlicker) AmberGold else EmeraldGreen
                        )

                        Text(
                            text = selectedSpec.spritePipelineExplanation,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )

                        Button(
                            onClick = { viewModel.bakeHardwareQuantizationToSprite() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MagentaNeon,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("bake_hw_sprite_btn")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Bake ${selectedSpec.shortName} Sprite to Studio",
                                fontFamily = TechMono,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ==================== FAN, HOMEBREW & FPGA CARTRIDGE CHIP LAB ====================
        var fanYawDeg by remember { mutableFloatStateOf(40f) }
        var fanPitchDeg by remember { mutableFloatStateOf(22f) }
        var fanZoom3D by remember { mutableFloatStateOf(1.35f) }
        var fanSpriteRotDeg by remember { mutableFloatStateOf(0f) }

        FanChipPreviewPanel(
            mesh = uiState.currentMesh,
            spriteSheet = uiState.currentSpriteSheet,
            activeTexture = uiState.activeTexture,
            yawDegrees = fanYawDeg,
            pitchDegrees = fanPitchDeg,
            zoom3D = fanZoom3D,
            lightAzimuthDegrees = 45f,
            onUpdate3DCamera = { newYaw, newPitch, newZoom ->
                fanYawDeg = newYaw
                fanPitchDeg = newPitch
                fanZoom3D = newZoom
            },
            spriteRotationDegrees = fanSpriteRotDeg,
            spriteZoom = 1.4f,
            flipHorizontal = false,
            showTileGrid = uiState.showHardwareTileGrid,
            onUpdateSpriteRotation = { fanSpriteRotDeg = it },
            onActivateFanSystem = { fanSpec ->
                viewModel.activateFusedHardwareSpec(fanSpec)
            }
        )

        // ==================== CROSS-SYSTEM SILICON & CHIP FUSION LAB ====================
        ChipFusionLabPanel(
            mesh = uiState.currentMesh,
            spriteSheet = uiState.currentSpriteSheet,
            activeTexture = uiState.activeTexture,
            readingLevelMode = uiState.readingLevelMode,
            showTileGrid = uiState.showHardwareTileGrid,
            onActivateFusedHardware = { fusedSpec ->
                viewModel.activateFusedHardwareSpec(fusedSpec)
            }
        )

        // ==================== VIVID DUAL-LEVEL ASSET OUTPUT READOUT DOSSIER ====================
        val activeDossier = if (previewModeTab == 0) meshDossier else (spriteDossier ?: meshDossier)
        AssetHardwareReadoutPanel(
            dossier = activeDossier,
            readingLevel = uiState.readingLevelMode,
            onSelectReadingLevel = { viewModel.setReadingLevelMode(it) },
            onCopiedMessage = { viewModel.postStatusMessage(it) }
        )

        // ==================== SILICON & CO-PROCESSOR CHIP BREAKDOWN ====================
        Text(
            text = "SILICON ARCHITECTURE & CUSTOM CHIPS (${selectedSpec.chips.size} IC MODULES)",
            fontFamily = TechMono,
            fontSize = 12.sp,
            color = AmberGold,
            letterSpacing = 1.sp
        )

        selectedSpec.chips.forEach { chip ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = chip.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = chip.clockSpeed,
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                color = AmberGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "${chip.role} • ${chip.architecture}",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = CyanNeon
                    )

                    Text(
                        text = "Bus / Memory Interface: ${chip.busAndProcess}",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Text(
                        text = chip.technicalNotes,
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecMetricColumn(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
        Text(text = value, fontFamily = TechMono, fontSize = 12.sp, color = Color.White)
    }
}
