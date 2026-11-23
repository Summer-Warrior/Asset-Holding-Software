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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.hardware.FanChipCatalog
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
import kotlin.math.PI
import kotlin.math.roundToInt

@Composable
fun FanChipPreviewPanel(
    mesh: Mesh3D,
    spriteSheet: SpriteSheetData,
    activeTexture: Bitmap?,
    yawDegrees: Float,
    pitchDegrees: Float,
    zoom3D: Float,
    lightAzimuthDegrees: Float,
    onUpdate3DCamera: (yawDeg: Float, pitchDeg: Float, zoom: Float) -> Unit,
    spriteRotationDegrees: Float,
    spriteZoom: Float,
    flipHorizontal: Boolean,
    showTileGrid: Boolean,
    onUpdateSpriteRotation: (Float) -> Unit,
    onActivateFanSystem: (ConsoleHardwareSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFanProfileId by remember { mutableStateOf(FanChipCatalog.allFanChipProfiles.first().id) }
    var previewScopeTab by remember { mutableIntStateOf(0) } // 0 = Dual 3D+2D, 1 = 3D Only, 2 = 2D Only
    var readingLevel by remember { mutableStateOf(ReadingLevelMode.BOTH_LEVELS) }

    val activeProfile = remember(selectedFanProfileId) {
        FanChipCatalog.getProfileById(selectedFanProfileId)
    }

    val previewData = remember(mesh, spriteSheet, activeProfile, showTileGrid) {
        FanChipCatalog.evaluateFanChipPreviewData(
            mesh = mesh,
            spriteSheet = spriteSheet,
            profile = activeProfile,
            showTileGrid = showTileGrid
        )
    }

    val adaptedStockMesh = remember(mesh, previewData.stockHostSpec) {
        HardwareAssetSimulator.adaptMeshToHardware(mesh, previewData.stockHostSpec)
    }
    val adaptedFanMesh = remember(mesh, previewData.fanEnhancedSpec) {
        HardwareAssetSimulator.adaptMeshToHardware(mesh, previewData.fanEnhancedSpec)
    }

    val yawRadians = (yawDegrees * PI / 180.0).toFloat()
    val pitchRadians = (pitchDegrees * PI / 180.0).toFloat()
    val lightRadians = (lightAzimuthDegrees * PI / 180.0).toFloat()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF09111F)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("fan_chip_preview_panel")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        tint = EmeraldGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FAN, HOMEBREW & FPGA CARTRIDGE CHIP LAB",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Inspect community-engineered silicon (MSU-1, CDFJ+ ARM, Rainbow FPGA, RP2040 PIO, PGXP, F3DEX3)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Surface(
                    color = AmberGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${FanChipCatalog.allFanChipProfiles.size} FAN CHIPS",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Fan Chip Selector Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FanChipCatalog.allFanChipProfiles.forEach { profile ->
                    val selected = profile.id == selectedFanProfileId
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) Color(0xFF0D2824) else Color(0xFF0F172A)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) EmeraldGreen else CardBorder
                        ),
                        modifier = Modifier
                            .width(205.dp)
                            .clickable { selectedFanProfileId = profile.id }
                            .testTag("fan_chip_card_${profile.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.shortName,
                                    fontFamily = TechMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) EmeraldGreen else Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${profile.debutYear}",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    color = AmberGold
                                )
                            }
                            Text(
                                text = "By ${profile.communityCreators}",
                                fontSize = 9.sp,
                                color = CyanNeon,
                                maxLines = 1
                            )
                            Text(
                                text = profile.architectureHeadline,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = Color(0xFFCBD5E1),
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // 2. Selected Fan Chip Architecture & Community Creator Dossier Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COMMUNITY CREATORS: ${activeProfile.communityCreators.uppercase()} (${activeProfile.debutYear})",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = AmberGold
                            )
                            Text(
                                text = activeProfile.chipName,
                                fontFamily = TechMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = CyanNeon.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "HOST: ${previewData.stockHostSpec.shortName.uppercase()}",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = CyanNeon,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Bus Hook Mechanism: ${activeProfile.busHookMechanism}",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )

                    // Signature Feature Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeProfile.signatureFeatures.forEach { feat ->
                            Surface(
                                color = Color(0xFF111C2D),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = feat,
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Live Stock Console vs. Fan-Chip Preview Data Stage
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE PREVIEW DATA: STOCK ${previewData.stockHostSpec.shortName.uppercase()} VS. ${activeProfile.shortName.uppercase()}",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0 to "3D + 2D", 1 to "3D Mesh", 2 to "2D Sprite").forEach { (idx, label) ->
                            val selected = previewScopeTab == idx
                            Surface(
                                color = if (selected) EmeraldGreen else Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable { previewScopeTab = idx }
                                    .testTag("fan_preview_scope_$idx")
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color(0xFF031B26) else Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 3D Preview Row (Stock vs. Fan Chip)
                if (previewScopeTab == 0 || previewScopeTab == 1) {
                    val h3D = if (previewScopeTab == 1) 235.dp else 185.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(h3D),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stock 3D
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .testTag("fan_chip_stock_3d_viewport")
                        ) {
                            Viewport3D(
                                mesh = adaptedStockMesh,
                                activeTexture = activeTexture,
                                hardwareSpec = previewData.stockHostSpec,
                                hardwareSimulationEnabled = true,
                                externalYawRadians = yawRadians,
                                externalPitchRadians = pitchRadians,
                                externalZoom = zoom3D,
                                externalLightAngle = lightRadians,
                                initialAutoRotate = false,
                                onCameraOrientationChanged = { newYawRad, newPitchRad, newZoom ->
                                    val degY = ((newYawRad * 180f / PI.toFloat()) % 360f + 360f) % 360f
                                    val degP = (newPitchRad * 180f / PI.toFloat()).coerceIn(-85f, 85f)
                                    onUpdate3DCamera(degY, degP, newZoom)
                                },
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
                                    text = "STOCK: ${previewData.stockHostSpec.shortName} (${previewData.stockHostSpec.recommendedModelTriBudget}T)",
                                    fontFamily = TechMono,
                                    fontSize = 8.sp,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Fan-Chip Enhanced 3D
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, EmeraldGreen, RoundedCornerShape(10.dp))
                                .testTag("fan_chip_enhanced_3d_viewport")
                        ) {
                            Viewport3D(
                                mesh = adaptedFanMesh,
                                activeTexture = activeTexture,
                                hardwareSpec = previewData.fanEnhancedSpec,
                                hardwareSimulationEnabled = true,
                                externalYawRadians = yawRadians,
                                externalPitchRadians = pitchRadians,
                                externalZoom = zoom3D,
                                externalLightAngle = lightRadians,
                                initialAutoRotate = false,
                                onCameraOrientationChanged = { newYawRad, newPitchRad, newZoom ->
                                    val degY = ((newYawRad * 180f / PI.toFloat()) % 360f + 360f) % 360f
                                    val degP = (newPitchRad * 180f / PI.toFloat()).coerceIn(-85f, 85f)
                                    onUpdate3DCamera(degY, degP, newZoom)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color(0xD906281E),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "FAN CHIP: ${activeProfile.shortName} (${previewData.fanEnhancedSpec.recommendedModelTriBudget}T)",
                                    fontFamily = TechMono,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // 2D Sprite Preview Row (Stock vs. Fan Chip)
                if (previewScopeTab == 0 || previewScopeTab == 2) {
                    val h2D = if (previewScopeTab == 2) 205.dp else 165.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(h2D),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stock 2D Sprite
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF070B12))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            previewData.stockSpriteReport?.let { rep ->
                                Image(
                                    bitmap = rep.simulatedBitmap.asImageBitmap(),
                                    contentDescription = "Stock Console Sprite",
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .graphicsLayer {
                                            rotationZ = spriteRotationDegrees
                                            scaleX = spriteZoom.coerceIn(0.7f, 2.5f) * (if (flipHorizontal) -1f else 1f)
                                            scaleY = spriteZoom.coerceIn(0.7f, 2.5f)
                                        }
                                )
                                Surface(
                                    color = Color(0xD9070B12),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "STOCK: ${previewData.stockHostSpec.bitsPerPixelSprite}bpp • ${rep.hardwarePaletteSwatches.size} col • ${previewData.stockHostSpec.maxSpritesPerScanline}/line",
                                        fontFamily = TechMono,
                                        fontSize = 8.sp,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Fan-Chip Enhanced 2D Sprite
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF070B12))
                                .border(1.5.dp, EmeraldGreen, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            previewData.fanSpriteReport?.let { rep ->
                                Image(
                                    bitmap = rep.simulatedBitmap.asImageBitmap(),
                                    contentDescription = "Fan Chip Sprite",
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .graphicsLayer {
                                            rotationZ = spriteRotationDegrees
                                            scaleX = spriteZoom.coerceIn(0.7f, 2.5f) * (if (flipHorizontal) -1f else 1f)
                                            scaleY = spriteZoom.coerceIn(0.7f, 2.5f)
                                        }
                                )
                                Surface(
                                    color = Color(0xD906281E),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "FAN CHIP: ${previewData.fanEnhancedSpec.bitsPerPixelSprite}bpp • ${rep.hardwarePaletteSwatches.size} col • ${previewData.fanEnhancedSpec.maxSpritesPerScanline}/line",
                                        fontFamily = TechMono,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Synchronized Rotation Sliders for Fan-Chip Preview
                if (previewScopeTab != 2) {
                    FanPreviewSliderRow(
                        label = "Sync 3D Yaw",
                        valueText = "${yawDegrees.roundToInt()}°",
                        value = yawDegrees,
                        valueRange = 0f..360f,
                        accentColor = EmeraldGreen,
                        testTag = "fan_chip_3d_yaw_slider",
                        onValueChange = { onUpdate3DCamera(it, pitchDegrees, zoom3D) }
                    )
                }
                if (previewScopeTab != 1) {
                    FanPreviewSliderRow(
                        label = "Sync 2D Angle",
                        valueText = "${spriteRotationDegrees.roundToInt()}°",
                        value = spriteRotationDegrees,
                        valueRange = 0f..360f,
                        accentColor = MagentaNeon,
                        testTag = "fan_chip_2d_rot_slider",
                        onValueChange = { onUpdateSpriteRotation(it) }
                    )
                }
            }

            // 4. Fan Chip Preview Telemetry & Delta Summary Card
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("3D Polygon Gain", fontFamily = TechMono, fontSize = 9.sp, color = Color(0xFF64748B))
                            Text(previewData.triBudgetMultiplierText, fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VRAM / Stream Pool", fontFamily = TechMono, fontSize = 9.sp, color = Color(0xFF64748B))
                            Text(previewData.vramMultiplierText, fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Scanline Sprites", fontFamily = TechMono, fontSize = 9.sp, color = Color(0xFF64748B))
                            Text(previewData.scanlineSpriteGainText, fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                        }
                    }
                    HorizontalDivider(color = CardBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "3D Impact: ${activeProfile.preview3DImpactBadge}",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = CyanNeon,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "2D Impact: ${activeProfile.preview2DImpactBadge}",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = MagentaNeon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Dual-Level Fan Chip Information & Register Stream Readout
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fan_chip_info_readout_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "FAN CHIP ARCHITECTURE & ASSET OUTPUT READOUT",
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReadingLevelMode.entries.forEach { mode ->
                                val selected = readingLevel == mode
                                Surface(
                                    color = if (selected) AmberGold else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { readingLevel = mode }
                                ) {
                                    Text(
                                        text = mode.shortLabel,
                                        fontFamily = TechMono,
                                        fontSize = 8.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color(0xFF090D16) else Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (readingLevel == ReadingLevelMode.EVERYDAY_READER || readingLevel == ReadingLevelMode.BOTH_LEVELS) {
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
                                    text = "EVERYDAY READER — COMMUNITY HISTORY & VISUAL UPGRADE",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                                Text(
                                    text = activeProfile.everydayExplanation,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }

                    if (readingLevel == ReadingLevelMode.HIGH_END_TECHNICAL || readingLevel == ReadingLevelMode.BOTH_LEVELS) {
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
                                    text = "HIGH-END SILICON — BUS HOOKS, OPCODES & DMA PIPELINE",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon
                                )
                                Text(
                                    text = activeProfile.highEndTechnicalBreakdown,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                                HorizontalDivider(color = CardBorder)
                                Text(
                                    text = previewData.registerStreamDump,
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

            // 6. Activate Fan Chip System Across Studio Button
            Button(
                onClick = { onActivateFanSystem(previewData.fanEnhancedSpec) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = Color(0xFF031B26)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("activate_fan_chip_target_btn")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Activate '${activeProfile.shortName}' Across Studio & Previews",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FanPreviewSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E1),
            modifier = Modifier.width(105.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .testTag(testTag)
        )
        Text(
            text = valueText,
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = accentColor,
            modifier = Modifier.width(42.dp)
        )
    }
}
