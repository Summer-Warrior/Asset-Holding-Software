package com.example.ui.components

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.model.hardware.ChipFusionEngine
import com.example.model.hardware.ConsoleHardwareCatalog
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.hardware.FusionInterconnectMode
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.ReadingLevelMode
import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import com.example.ui.viewer3d.Viewport3D
import kotlin.math.roundToInt

@Composable
fun ChipFusionLabPanel(
    mesh: Mesh3D,
    spriteSheet: SpriteSheetData,
    activeTexture: Bitmap?,
    readingLevelMode: ReadingLevelMode,
    showTileGrid: Boolean,
    onActivateFusedHardware: (ConsoleHardwareSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    var hostSystemId by remember { mutableStateOf("sega_genesis") }
    var primaryDonorId by remember { mutableStateOf("donor_superfx2") }
    var secondaryDonorId by remember { mutableStateOf<String?>(null) }
    var interconnect by remember { mutableStateOf(FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM) }
    var previewModalityTab by remember { mutableIntStateOf(0) } // 0 = 3D Output, 1 = 2D Sprite Output

    val hostSpec = remember(hostSystemId) { ConsoleHardwareCatalog.getById(hostSystemId) }
    val activeDonors = remember(primaryDonorId, secondaryDonorId) {
        buildList {
            add(ChipFusionEngine.getDonorById(primaryDonorId))
            if (secondaryDonorId != null && secondaryDonorId != primaryDonorId) {
                add(ChipFusionEngine.getDonorById(secondaryDonorId!!))
            }
        }
    }

    val fusionReport = remember(mesh, spriteSheet, hostSpec, activeDonors, interconnect, showTileGrid) {
        ChipFusionEngine.evaluateAssetOnFusedSystem(
            mesh = mesh,
            spriteSheet = spriteSheet,
            hostSpec = hostSpec,
            donors = activeDonors,
            interconnect = interconnect,
            showTileGrid = showTileGrid
        )
    }

    val adaptedHostMesh = remember(mesh, hostSpec) {
        HardwareAssetSimulator.adaptMeshToHardware(mesh, hostSpec)
    }
    val adaptedFusedMesh = remember(mesh, fusionReport.fusedSpec) {
        HardwareAssetSimulator.adaptMeshToHardware(mesh, fusionReport.fusedSpec)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1120)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MagentaNeon.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chip_fusion_lab_panel")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = null,
                        tint = MagentaNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CROSS-SYSTEM SILICON & CHIP FUSION LAB",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MagentaNeon,
                            letterSpacing = 0.7.sp
                        )
                        Text(
                            text = "Attach co-processors, DSPs & video chips across consoles to inspect fused output",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "+${fusionReport.triBudgetGainPercent}% 3D BUDGET",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Curated Cross-System Chip Fusion Blueprints
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CURATED CROSS-SYSTEM CHIP FUSION BLUEPRINTS",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChipFusionEngine.curatedFusionBlueprints.forEach { bp ->
                        val isSelected = hostSystemId == bp.hostSystemId &&
                                primaryDonorId == bp.primaryDonorChipId &&
                                secondaryDonorId == bp.secondaryDonorChipId
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MagentaNeon else CardBorder
                            ),
                            modifier = Modifier
                                .width(205.dp)
                                .clickable {
                                    hostSystemId = bp.hostSystemId
                                    primaryDonorId = bp.primaryDonorChipId
                                    secondaryDonorId = bp.secondaryDonorChipId
                                    interconnect = bp.interconnect
                                }
                                .testTag("fusion_blueprint_${bp.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = bp.shortTitle,
                                    fontFamily = TechMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MagentaNeon else Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = bp.subtitle,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // 2. Custom Host System + Donor Chip(s) + Bus Interconnect Builder
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CUSTOM SILICON GRAFT MATRIX (HOST SYSTEM + ATTACHED DONOR CHIPS)",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )

                    // Host System Selector Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Host Base:",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = CyanNeon,
                            modifier = Modifier.width(68.dp)
                        )
                        ConsoleHardwareCatalog.allSystems.forEach { sys ->
                            val selected = sys.id == hostSystemId
                            Surface(
                                color = if (selected) CyanNeon else Color(0xFF111827),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) CyanNeon else CardBorder
                                ),
                                modifier = Modifier
                                    .clickable { hostSystemId = sys.id }
                                    .testTag("fusion_host_${sys.id}")
                            ) {
                                Text(
                                    text = sys.shortName,
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color(0xFF031B26) else Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Primary Donor Chip Selector Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Donor IC 1:",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = MagentaNeon,
                            modifier = Modifier.width(68.dp)
                        )
                        ChipFusionEngine.allDonorChipsWithFanSilicon.forEach { donor ->
                            val selected = donor.id == primaryDonorId
                            Surface(
                                color = if (selected) MagentaNeon else Color(0xFF111827),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) MagentaNeon else CardBorder
                                ),
                                modifier = Modifier
                                    .clickable { primaryDonorId = donor.id }
                                    .testTag("fusion_donor1_${donor.id}")
                            ) {
                                Text(
                                    text = donor.shortName,
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Secondary Optional Co-Chip Selector Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Donor IC 2:",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = AmberGold,
                            modifier = Modifier.width(68.dp)
                        )
                        Surface(
                            color = if (secondaryDonorId == null) AmberGold else Color(0xFF111827),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (secondaryDonorId == null) AmberGold else CardBorder
                            ),
                            modifier = Modifier
                                .clickable { secondaryDonorId = null }
                                .testTag("fusion_donor2_none")
                        ) {
                            Text(
                                text = "Single Donor Only",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = if (secondaryDonorId == null) FontWeight.Bold else FontWeight.Normal,
                                color = if (secondaryDonorId == null) Color(0xFF090D16) else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        ChipFusionEngine.allDonorChipsWithFanSilicon.filter { it.id != primaryDonorId }.forEach { donor ->
                            val selected = donor.id == secondaryDonorId
                            Surface(
                                color = if (selected) AmberGold else Color(0xFF111827),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) AmberGold else CardBorder
                                ),
                                modifier = Modifier
                                    .clickable { secondaryDonorId = donor.id }
                                    .testTag("fusion_donor2_${donor.id}")
                            ) {
                                Text(
                                    text = "+ ${donor.shortName}",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color(0xFF090D16) else Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Bus Interconnect Architecture Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bus Bridge:",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = EmeraldGreen,
                            modifier = Modifier.width(68.dp)
                        )
                        FusionInterconnectMode.entries.forEach { mode ->
                            val selected = mode == interconnect
                            FilterChip(
                                selected = selected,
                                onClick = { interconnect = mode },
                                label = { Text(mode.label, fontFamily = TechMono, fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color(0xFF031B26),
                                    containerColor = Color(0xFF111827),
                                    labelColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.testTag("fusion_interconnect_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // 3. Before & After Visual Asset Output Comparison (Stock Host vs. Chip-Fused System)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STOCK HOST VS. FUSED SILICON ASSET OUTPUT",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "3D Mesh Output", 1 to "2D Sprite Output").forEach { (idx, label) ->
                            val selected = previewModalityTab == idx
                            Surface(
                                color = if (selected) MagentaNeon else Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable { previewModalityTab = idx }
                                    .testTag("fusion_preview_tab_$idx")
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (previewModalityTab == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stock Host 3D Output
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        ) {
                            Viewport3D(
                                mesh = adaptedHostMesh,
                                activeTexture = activeTexture,
                                hardwareSpec = hostSpec,
                                hardwareSimulationEnabled = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color(0xD9070B12),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "STOCK: ${hostSpec.shortName} (${fusionReport.baseMeshReport.recommendedTriBudget}T)",
                                    fontFamily = TechMono,
                                    fontSize = 8.sp,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Chip-Fused 3D Output
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, MagentaNeon, RoundedCornerShape(10.dp))
                        ) {
                            Viewport3D(
                                mesh = adaptedFusedMesh,
                                activeTexture = activeTexture,
                                hardwareSpec = fusionReport.fusedSpec,
                                hardwareSimulationEnabled = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color(0xD91E1B4B),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MagentaNeon),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "FUSED: ${fusionReport.fusedSpec.shortName} (${fusionReport.fusedMeshReport.recommendedTriBudget}T)",
                                    fontFamily = TechMono,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MagentaNeon,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stock Host 2D Sprite Output
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF070B12))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            fusionReport.baseSpriteReport?.let { rep ->
                                Image(
                                    bitmap = rep.simulatedBitmap.asImageBitmap(),
                                    contentDescription = "Stock Host Sprite",
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(130.dp)
                                )
                                Surface(
                                    color = Color(0xD9070B12),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "STOCK: ${hostSpec.bitsPerPixelSprite}bpp • ${rep.hardwarePaletteSwatches.size} cols • ${hostSpec.maxSpritesPerScanline}/line",
                                        fontFamily = TechMono,
                                        fontSize = 8.sp,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Chip-Fused 2D Sprite Output
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF070B12))
                                .border(1.5.dp, MagentaNeon, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            fusionReport.fusedSpriteReport?.let { rep ->
                                Image(
                                    bitmap = rep.simulatedBitmap.asImageBitmap(),
                                    contentDescription = "Chip-Fused Sprite",
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(130.dp)
                                )
                                Surface(
                                    color = Color(0xD91E1B4B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MagentaNeon),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "FUSED: ${fusionReport.fusedSpec.bitsPerPixelSprite}bpp • ${rep.hardwarePaletteSwatches.size} cols • ${fusionReport.fusedSpec.maxSpritesPerScanline}/line",
                                        fontFamily = TechMono,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MagentaNeon,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Silicon Delta Metrics Bar
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070C17)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FusionMetricDeltaItem(
                            label = "3D Tri Budget",
                            before = "${hostSpec.recommendedModelTriBudget}T",
                            after = "${fusionReport.fusedSpec.recommendedModelTriBudget}T",
                            accent = EmeraldGreen
                        )
                        FusionMetricDeltaItem(
                            label = "Video / Co-RAM",
                            before = "${hostSpec.vramBytes / 1024}KB",
                            after = "${fusionReport.fusedSpec.vramBytes / 1024}KB",
                            accent = CyanNeon
                        )
                        FusionMetricDeltaItem(
                            label = "Vertex / Z-Buf",
                            before = if (hostSpec.hasZBuffer) "Z-Buf" else "Snap ${hostSpec.vertexSnapGrid}",
                            after = if (fusionReport.fusedSpec.hasZBuffer) "Z-Buf (Sub-Px)" else "Snap ${fusionReport.fusedSpec.vertexSnapGrid}",
                            accent = AmberGold
                        )
                        FusionMetricDeltaItem(
                            label = "Sprites/Line",
                            before = "${hostSpec.maxSpritesPerScanline}",
                            after = "${fusionReport.fusedSpec.maxSpritesPerScanline}",
                            accent = MagentaNeon
                        )
                    }
                }
            }

            // 5. Vivid Dual-Level Fused Asset Output Readout
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chip_fusion_readout_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "FUSED SILICON ASSET OUTPUT READOUT (${fusionReport.fusedSpec.name.uppercase()})",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }

                    if (readingLevelMode == ReadingLevelMode.EVERYDAY_READER || readingLevelMode == ReadingLevelMode.BOTH_LEVELS) {
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "EVERYDAY READER — HOW THE ATTACHED CHIPS TRANSFORM YOUR ASSET",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                                Text(
                                    text = fusionReport.everydayFusionReadout,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }

                    if (readingLevelMode == ReadingLevelMode.HIGH_END_TECHNICAL || readingLevelMode == ReadingLevelMode.BOTH_LEVELS) {
                        Surface(
                            color = Color(0xFF090D16),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "HIGH-END SILICON — BUS BRIDGE, DMA & RASTERIZATION TELEMETRY",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon
                                )
                                Text(
                                    text = fusionReport.highEndTechnicalFusionReadout,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                                HorizontalDivider(color = CardBorder)
                                Text(
                                    text = fusionReport.busBridgeRegisterDump,
                                    fontFamily = TechMono,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }

            // 6. Activate Fused Hybrid System Across Studio & Previews Button
            Button(
                onClick = { onActivateFusedHardware(fusionReport.fusedSpec) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MagentaNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("activate_fused_system_btn")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Activate '${fusionReport.fusedSpec.shortName}' Across Studio & Previews",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FusionMetricDeltaItem(
    label: String,
    before: String,
    after: String,
    accent: Color
) {
    Column {
        Text(
            text = label,
            fontFamily = TechMono,
            fontSize = 9.sp,
            color = Color(0xFF64748B)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = before,
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = " → ",
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = accent
            )
            Text(
                text = after,
                fontFamily = TechMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}
