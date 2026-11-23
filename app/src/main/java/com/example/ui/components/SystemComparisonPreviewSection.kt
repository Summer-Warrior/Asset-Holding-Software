package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.hardware.CompatibilityLevel
import com.example.model.hardware.ConsoleHardwareCatalog
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.MeshHardwareReport
import com.example.model.hardware.SpriteHardwareReport
import com.example.model.model3d.Mesh3D
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import com.example.ui.viewer3d.Viewport3D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ComparePreviewScope(val label: String) {
    BOTH_3D_AND_2D("3D + 2D Matrix"),
    MODELS_3D_ONLY("3D Models"),
    SPRITES_2D_ONLY("2D Sprites")
}

data class HardwareMatchupPreset(
    val label: String,
    val systemAId: String,
    val systemBId: String
)

val ICONIC_HARDWARE_MATCHUPS = listOf(
    HardwareMatchupPreset("SNES vs Genesis", "snes", "sega_genesis"),
    HardwareMatchupPreset("SNES vs MSU-1+SuperFX3", "snes", "fan_snes_msu1_fxpak"),
    HardwareMatchupPreset("2600 vs CDFJ+ 70MHz ARM", "atari_2600", "fan_atari_cdfj_arm"),
    HardwareMatchupPreset("NES vs Rainbow FPGA", "nes_2c02", "fan_nes_rainbow_fpga"),
    HardwareMatchupPreset("PS1 vs PGXP Float GTE", "ps1", "fan_ps1_pgxp_float"),
    HardwareMatchupPreset("Genesis vs Gen+SuperFX2", "sega_genesis", "fused_sega_genesis_superfx2"),
    HardwareMatchupPreset("SNES vs SNES+SVP", "snes", "fused_snes_sega_svp_sa1_mmc5"),
    HardwareMatchupPreset("PS1 vs PS1+N64 RDP", "ps1", "fused_ps1_n64_rdp"),
    HardwareMatchupPreset("PS1 vs N64", "ps1", "n64"),
    HardwareMatchupPreset("PS1 vs Saturn", "ps1", "sega_saturn"),
    HardwareMatchupPreset("Super FX vs SVP", "snes_superfx", "sega_svp"),
    HardwareMatchupPreset("NES vs Game Boy", "nes_2c02", "gameboy_dmg"),
    HardwareMatchupPreset("GBA vs NDS", "gba", "nintendo_ds"),
    HardwareMatchupPreset("PS2 vs GameCube", "ps2", "gamecube"),
    HardwareMatchupPreset("8-Bit vs RDNA 2", "nes_2c02", "ps5_series_x")
)

fun defaultRivalSystemId(primaryId: String): String = when (primaryId) {
    "snes" -> "sega_genesis"
    "sega_genesis" -> "snes"
    "ps1" -> "n64"
    "n64" -> "ps1"
    "sega_saturn" -> "ps1"
    "snes_superfx" -> "sega_svp"
    "sega_svp" -> "snes_superfx"
    "nes_2c02" -> "gameboy_dmg"
    "gameboy_dmg" -> "nes_2c02"
    "gba" -> "nintendo_ds"
    "nintendo_ds" -> "psp"
    "ps2" -> "gamecube"
    "gamecube" -> "ps2"
    "dreamcast" -> "ps2"
    else -> "n64"
}

@Composable
fun SystemComparisonPreviewSection(
    mesh: Mesh3D,
    currentSpriteFrame: Bitmap,
    activeTexture: Bitmap?,
    systemAId: String,
    systemBId: String,
    onSelectSystemA: (String) -> Unit,
    onSelectSystemB: (String) -> Unit,
    onSwapSystems: () -> Unit,
    yawDegrees: Float,
    pitchDegrees: Float,
    zoom3D: Float,
    lightAzimuthDegrees: Float,
    onUpdate3DCamera: (yawDeg: Float, pitchDeg: Float, zoom: Float) -> Unit,
    spriteRotationDegrees: Float,
    spriteZoom: Float,
    flipHorizontal: Boolean,
    showPixelGrid: Boolean,
    onUpdateSpriteTransform: (rotDeg: Float, zoom: Float) -> Unit,
    onToggleFlipHorizontal: () -> Unit,
    onTogglePixelGrid: () -> Unit
) {
    var compareScope by remember { mutableStateOf(ComparePreviewScope.BOTH_3D_AND_2D) }
    var technicalReadingMode by remember { mutableStateOf(false) }

    val systemA = remember(systemAId) { ConsoleHardwareCatalog.getById(systemAId) }
    val systemB = remember(systemBId) { ConsoleHardwareCatalog.getById(systemBId) }

    // Hardware-adapted 3D meshes & reports for System A and System B
    val adaptedMeshA = remember(mesh, systemA) {
        HardwareAssetSimulator.adaptMeshToHardware(mesh, systemA)
    }
    val adaptedMeshB = remember(mesh, systemB) {
        HardwareAssetSimulator.adaptMeshToHardware(mesh, systemB)
    }
    val report3DA = remember(mesh, systemA) {
        HardwareAssetSimulator.analyzeMeshOnHardware(mesh, systemA)
    }
    val report3DB = remember(mesh, systemB) {
        HardwareAssetSimulator.analyzeMeshOnHardware(mesh, systemB)
    }

    // Hardware-simulated 2D sprites & reports for System A and System B
    val report2DA = remember(currentSpriteFrame, systemA, showPixelGrid) {
        HardwareAssetSimulator.simulateSpriteOnHardware(currentSpriteFrame, systemA, showPixelGrid)
    }
    val report2DB = remember(currentSpriteFrame, systemB, showPixelGrid) {
        HardwareAssetSimulator.simulateSpriteOnHardware(currentSpriteFrame, systemB, showPixelGrid)
    }

    val yawRadians = (yawDegrees * PI / 180.0).toFloat()
    val pitchRadians = (pitchDegrees * PI / 180.0).toFloat()
    val lightRadians = (lightAzimuthDegrees * PI / 180.0).toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("system_comparison_preview_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Iconic Console Rivalry & Cross-Gen Matchup Presets Bar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "HARDWARE MATCHUP PRESETS & SCOPE",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                }

                // Scope selector pills (3D + 2D Matrix | 3D Models | 2D Sprites)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ComparePreviewScope.entries.forEach { scope ->
                        val selected = compareScope == scope
                        Surface(
                            color = if (selected) CyanNeon else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable { compareScope = scope }
                                .testTag("compare_scope_${scope.name.lowercase()}")
                        ) {
                            Text(
                                text = scope.label,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ICONIC_HARDWARE_MATCHUPS.forEach { preset ->
                    val isSelected =
                        (systemAId == preset.systemAId && systemBId == preset.systemBId) ||
                                (systemAId == preset.systemBId && systemBId == preset.systemAId)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectSystemA(preset.systemAId)
                            onSelectSystemB(preset.systemBId)
                        },
                        label = { Text(preset.label, fontFamily = TechMono, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGold,
                            selectedLabelColor = Color(0xFF090D16),
                            containerColor = Color(0xFF162032),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("matchup_preset_${preset.systemAId}_${preset.systemBId}")
                    )
                }
            }
        }

        // 2. Dual System Selector Bar (System A [Left] ⇄ System B [Right])
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System A Badge
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyanNeon)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "SYSTEM A (LEFT DECK)",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = CyanNeon
                            )
                            Text(
                                text = "${systemA.shortName} (${systemA.releaseYear})",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    // Swap Button
                    IconButton(
                        onClick = onSwapSystems,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .border(1.dp, AmberGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .testTag("compare_swap_systems_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap System A and System B",
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // System B Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SYSTEM B (RIGHT DECK)",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = AmberGold
                            )
                            Text(
                                text = "${systemB.shortName} (${systemB.releaseYear})",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AmberGold)
                        )
                    }
                }

                // System A Horizontal Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deck A:",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = CyanNeon,
                        modifier = Modifier.width(46.dp)
                    )
                    ConsoleHardwareCatalog.allSystemsWithFusions().forEach { spec ->
                        val selected = spec.id == systemA.id
                        Surface(
                            color = if (selected) CyanNeon else Color(0xFF111827),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) CyanNeon else CardBorder
                            ),
                            modifier = Modifier
                                .clickable { onSelectSystemA(spec.id) }
                                .testTag("compare_select_a_${spec.id}")
                        ) {
                            Text(
                                text = spec.shortName,
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFF031B26) else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // System B Horizontal Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deck B:",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = AmberGold,
                        modifier = Modifier.width(46.dp)
                    )
                    ConsoleHardwareCatalog.allSystemsWithFusions().forEach { spec ->
                        val selected = spec.id == systemB.id
                        Surface(
                            color = if (selected) AmberGold else Color(0xFF111827),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) AmberGold else CardBorder
                            ),
                            modifier = Modifier
                                .clickable { onSelectSystemB(spec.id) }
                                .testTag("compare_select_b_${spec.id}")
                        ) {
                            Text(
                                text = spec.shortName,
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFF090D16) else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Synchronized Side-by-Side 3D Model Comparison Stage
        if (compareScope == ComparePreviewScope.BOTH_3D_AND_2D || compareScope == ComparePreviewScope.MODELS_3D_ONLY) {
            val stageHeight = if (compareScope == ComparePreviewScope.MODELS_3D_ONLY) 245.dp else 195.dp
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "3D HARDWARE RASTER & GEOMETRY COMPARISON (LOCKSTEP ORBIT)",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stageHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // System A 3D Viewport
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, CyanNeon.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                            .testTag("compare_3d_viewport_a")
                    ) {
                        Viewport3D(
                            mesh = adaptedMeshA,
                            activeTexture = activeTexture,
                            hardwareSpec = systemA,
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
                        CompatibilityCornerBadge(
                            systemLabel = "A: ${systemA.shortName}",
                            compatibility = report3DA.compatibility,
                            accentColor = CyanNeon,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                        )
                    }

                    // System B 3D Viewport
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, AmberGold.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                            .testTag("compare_3d_viewport_b")
                    ) {
                        Viewport3D(
                            mesh = adaptedMeshB,
                            activeTexture = activeTexture,
                            hardwareSpec = systemB,
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
                        CompatibilityCornerBadge(
                            systemLabel = "B: ${systemB.shortName}",
                            compatibility = report3DB.compatibility,
                            accentColor = AmberGold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                        )
                    }
                }
            }
        }

        // 4. Synchronized Side-by-Side 2D Sprite Hardware Comparison Stage
        if (compareScope == ComparePreviewScope.BOTH_3D_AND_2D || compareScope == ComparePreviewScope.SPRITES_2D_ONLY) {
            val spriteStageHeight = if (compareScope == ComparePreviewScope.SPRITES_2D_ONLY) 215.dp else 175.dp
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2D PPU/VDP SPRITE QUANTIZATION & PALETTE COMPARISON",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagentaNeon
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                onUpdateSpriteTransform((spriteRotationDegrees + 90f) % 360f, spriteZoom)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null, tint = MagentaNeon, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("+90°", fontFamily = TechMono, fontSize = 9.sp, color = Color.White)
                            }
                        }
                        Surface(
                            color = if (flipHorizontal) MagentaNeon else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onToggleFlipHorizontal() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Flip", fontFamily = TechMono, fontSize = 9.sp, color = Color.White)
                            }
                        }
                        Surface(
                            color = if (showPixelGrid) EmeraldGreen else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onTogglePixelGrid() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = null,
                                    tint = if (showPixelGrid) Color(0xFF031B26) else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "CHR Grid",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    color = if (showPixelGrid) Color(0xFF031B26) else Color.White
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spriteStageHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // System A 2D Sprite Card
                    SimulatedSpriteDeckPane(
                        deckTitle = "A: ${systemA.shortName}",
                        spec = systemA,
                        report = report2DA,
                        rotationDegrees = spriteRotationDegrees,
                        zoom = spriteZoom,
                        flipHorizontal = flipHorizontal,
                        borderColor = CyanNeon,
                        onGestureTransform = { zoomDelta, rotDelta ->
                            val newZoom = (spriteZoom * zoomDelta).coerceIn(0.6f, 3.8f)
                            val newRot = ((spriteRotationDegrees + rotDelta) % 360f + 360f) % 360f
                            onUpdateSpriteTransform(newRot, newZoom)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .testTag("compare_2d_sprite_a")
                    )

                    // System B 2D Sprite Card
                    SimulatedSpriteDeckPane(
                        deckTitle = "B: ${systemB.shortName}",
                        spec = systemB,
                        report = report2DB,
                        rotationDegrees = spriteRotationDegrees,
                        zoom = spriteZoom,
                        flipHorizontal = flipHorizontal,
                        borderColor = AmberGold,
                        onGestureTransform = { zoomDelta, rotDelta ->
                            val newZoom = (spriteZoom * zoomDelta).coerceIn(0.6f, 3.8f)
                            val newRot = ((spriteRotationDegrees + rotDelta) % 360f + 360f) % 360f
                            onUpdateSpriteTransform(newRot, newZoom)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .testTag("compare_2d_sprite_b")
                    )
                }
            }
        }

        // 5. Synchronized Comparison Rotation Sliders
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (compareScope != ComparePreviewScope.SPRITES_2D_ONLY) {
                CompareSliderRow(
                    label = "Sync 3D Yaw",
                    valueText = "${yawDegrees.roundToInt()}°",
                    value = yawDegrees,
                    valueRange = 0f..360f,
                    accentColor = CyanNeon,
                    testTag = "compare_sync_3d_yaw_slider",
                    onValueChange = { onUpdate3DCamera(it, pitchDegrees, zoom3D) }
                )
            }
            if (compareScope != ComparePreviewScope.MODELS_3D_ONLY) {
                CompareSliderRow(
                    label = "Sync 2D Angle",
                    valueText = "${spriteRotationDegrees.roundToInt()}°",
                    value = spriteRotationDegrees,
                    valueRange = 0f..360f,
                    accentColor = MagentaNeon,
                    testTag = "compare_sync_2d_rot_slider",
                    onValueChange = { onUpdateSpriteTransform(it, spriteZoom) }
                )
            }
        }

        // 6. Head-to-Head System Delta & Telemetry Readout Card
        SystemComparisonTelemetryCard(
            mesh = mesh,
            systemA = systemA,
            systemB = systemB,
            report3DA = report3DA,
            report3DB = report3DB,
            report2DA = report2DA,
            report2DB = report2DB,
            technicalReadingMode = technicalReadingMode,
            onToggleReadingMode = { technicalReadingMode = !technicalReadingMode }
        )
    }
}

@Composable
private fun SimulatedSpriteDeckPane(
    deckTitle: String,
    spec: ConsoleHardwareSpec,
    report: SpriteHardwareReport,
    rotationDegrees: Float,
    zoom: Float,
    flipHorizontal: Boolean,
    borderColor: Color,
    onGestureTransform: (zoomChange: Float, rotationChange: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070B12))
            .border(1.5.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomChange, rotationChange ->
                    onGestureTransform(zoomChange, rotationChange)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = report.simulatedBitmap.asImageBitmap(),
            contentDescription = "$deckTitle Simulated Sprite",
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(125.dp)
                .graphicsLayer {
                    rotationZ = rotationDegrees
                    scaleX = zoom * (if (flipHorizontal) -1f else 1f)
                    scaleY = zoom
                }
        )

        // Top Header Pill
        Surface(
            color = Color(0xCC090D16),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Text(
                text = "$deckTitle • ${spec.bitsPerPixelSprite}bpp (${report.hardwarePaletteSwatches.size} col)",
                fontFamily = TechMono,
                fontSize = 8.sp,
                color = borderColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        // Bottom Extracted Hardware Palette Swatches + Tile Count
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC070B12))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                report.hardwarePaletteSwatches.take(8).forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(argb))
                            .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                    )
                }
            }
            Text(
                text = "${report.totalChrTiles}T • ${report.chrVramBytes}B",
                fontFamily = TechMono,
                fontSize = 8.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun CompatibilityCornerBadge(
    systemLabel: String,
    compatibility: CompatibilityLevel,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xD9070B12),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(compatibility.colorHex))
            )
            Text(
                text = "$systemLabel • ${compatibility.label}",
                fontFamily = TechMono,
                fontSize = 8.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SystemComparisonTelemetryCard(
    mesh: Mesh3D,
    systemA: ConsoleHardwareSpec,
    systemB: ConsoleHardwareSpec,
    report3DA: MeshHardwareReport,
    report3DB: MeshHardwareReport,
    report2DA: SpriteHardwareReport,
    report2DB: SpriteHardwareReport,
    technicalReadingMode: Boolean,
    onToggleReadingMode: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070C17)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("system_comparison_telemetry_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HEAD-TO-HEAD SILICON DELTA (${systemA.shortName.uppercase()} VS ${systemB.shortName.uppercase()})",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (technicalReadingMode) CyanNeon.copy(alpha = 0.2f) else AmberGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (technicalReadingMode) CyanNeon else AmberGold
                    ),
                    modifier = Modifier
                        .clickable { onToggleReadingMode() }
                        .testTag("compare_reading_mode_toggle")
                ) {
                    Text(
                        text = if (technicalReadingMode) "Mode: High-End Silicon" else "Mode: Everyday Reader",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = if (technicalReadingMode) CyanNeon else AmberGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Column Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "METRIC",
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.weight(0.9f)
                )
                Text(
                    text = systemA.shortName,
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon,
                    modifier = Modifier.weight(1.05f)
                )
                Text(
                    text = systemB.shortName,
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold,
                    modifier = Modifier.weight(1.05f)
                )
            }

            HorizontalDivider(color = CardBorder)

            ComparisonMetricRow(
                metric = "Native Display",
                valA = "${systemA.screenWidth}×${systemA.screenHeight} (${systemA.releaseYear})",
                valB = "${systemB.screenWidth}×${systemB.screenHeight} (${systemB.releaseYear})"
            )
            ComparisonMetricRow(
                metric = "3D Pipeline",
                valA = if (systemA.hasHardware3D) systemA.renderProfile3D.badge.substringBefore(" (") else "SW 2D Wireframe",
                valB = if (systemB.hasHardware3D) systemB.renderProfile3D.badge.substringBefore(" (") else "SW 2D Wireframe"
            )
            ComparisonMetricRow(
                metric = "3D Tri Budget",
                valA = "${mesh.faceCount}/${systemA.recommendedModelTriBudget} (${report3DA.assetBudgetPercent.roundToInt()}%)",
                valB = "${mesh.faceCount}/${systemB.recommendedModelTriBudget} (${report3DB.assetBudgetPercent.roundToInt()}%)"
            )
            ComparisonMetricRow(
                metric = "Vertex & Depth",
                valA = "${if (systemA.vertexSnapGrid > 0f) "Snap ${systemA.vertexSnapGrid}" else "Sub-Pixel"} • ${if (systemA.hasZBuffer) "Z-Buf" else "Painter/OT"}",
                valB = "${if (systemB.vertexSnapGrid > 0f) "Snap ${systemB.vertexSnapGrid}" else "Sub-Pixel"} • ${if (systemB.hasZBuffer) "Z-Buf" else "Painter/OT"}"
            )
            ComparisonMetricRow(
                metric = "2D Color Depth",
                valA = "${systemA.bitsPerPixelSprite}bpp (${report2DA.hardwarePaletteSwatches.size} vis / ${systemA.maxOnScreenColors} scr)",
                valB = "${systemB.bitsPerPixelSprite}bpp (${report2DB.hardwarePaletteSwatches.size} vis / ${systemB.maxOnScreenColors} scr)"
            )
            ComparisonMetricRow(
                metric = "Sprite VRAM",
                valA = "${report2DA.chrVramBytes} B (${String.format("%.1f%%", report2DA.vramUsagePercent)} VRAM)",
                valB = "${report2DB.chrVramBytes} B (${String.format("%.1f%%", report2DB.vramUsagePercent)} VRAM)"
            )
            ComparisonMetricRow(
                metric = "Scanline Limit",
                valA = "${report2DA.scanlineSpritesUsed}/${report2DA.scanlineLimitMax} slots (${if (report2DA.causesScanlineFlicker) "FLICKER" else "CLEAN"})",
                valB = "${report2DB.scanlineSpritesUsed}/${report2DB.scanlineLimitMax} slots (${if (report2DB.causesScanlineFlicker) "FLICKER" else "CLEAN"})"
            )

            HorizontalDivider(color = CardBorder)

            // Dual-Level Comparison Narrative (Everyday vs. High-End Silicon)
            val narrative = remember(systemA, systemB, report3DA, report3DB, report2DA, report2DB, technicalReadingMode) {
                buildComparisonNarrative(
                    systemA = systemA,
                    systemB = systemB,
                    report3DA = report3DA,
                    report3DB = report3DB,
                    report2DA = report2DA,
                    report2DB = report2DB,
                    technicalMode = technicalReadingMode
                )
            }

            Text(
                text = narrative,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.testTag("compare_narrative_text")
            )
        }
    }
}

@Composable
private fun ComparisonMetricRow(
    metric: String,
    valA: String,
    valB: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = metric,
            fontFamily = TechMono,
            fontSize = 9.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = valA,
            fontFamily = TechMono,
            fontSize = 9.sp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.weight(1.05f)
        )
        Text(
            text = valB,
            fontFamily = TechMono,
            fontSize = 9.sp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.weight(1.05f)
        )
    }
}

@Composable
private fun CompareSliderRow(
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
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = androidx.compose.material3.SliderDefaults.colors(
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

private fun buildComparisonNarrative(
    systemA: ConsoleHardwareSpec,
    systemB: ConsoleHardwareSpec,
    report3DA: MeshHardwareReport,
    report3DB: MeshHardwareReport,
    report2DA: SpriteHardwareReport,
    report2DB: SpriteHardwareReport,
    technicalMode: Boolean
): String {
    return if (!technicalMode) {
        buildString {
            append("Everyday Visual Summary: ")
            // 2D Sprite comparison
            if (systemA.bitsPerPixelSprite != systemB.bitsPerPixelSprite ||
                report2DA.hardwarePaletteSwatches.size != report2DB.hardwarePaletteSwatches.size
            ) {
                append(
                    "For 2D sprites, ${systemA.shortName} renders up to ${report2DA.hardwarePaletteSwatches.size} visible colors (${systemA.bitsPerPixelSprite}-bit) from a ${systemA.masterPaletteSize}-color master palette, whereas ${systemB.shortName} renders ${report2DB.hardwarePaletteSwatches.size} visible colors (${systemB.bitsPerPixelSprite}-bit) from ${systemB.masterPaletteSize} total colors. "
                )
            } else {
                append(
                    "Both ${systemA.shortName} and ${systemB.shortName} quantize the 2D sprite to ${report2DA.hardwarePaletteSwatches.size} active colors, though ${systemA.shortName} chooses from ${systemA.masterPaletteSize} master shades vs ${systemB.masterPaletteSize} on ${systemB.shortName}. "
                )
            }
            // 3D Model comparison
            when {
                systemA.hasHardware3D && !systemB.hasHardware3D ->
                    append("In 3D, ${systemA.shortName} has dedicated 3D polygon hardware for shaded surfaces, while ${systemB.shortName} must drop back to a retro wireframe outline or pre-rendered sprite impostor.")
                !systemA.hasHardware3D && systemB.hasHardware3D ->
                    append("In 3D, ${systemA.shortName} lacks polygon hardware and draws a wireframe silhouette, whereas ${systemB.shortName} renders shaded 3D triangles in hardware.")
                systemA.vertexSnapGrid > 0f && systemB.vertexSnapGrid == 0f ->
                    append("As you rotate the 3D model, notice how ${systemA.shortName} snaps vertices to whole screen pixels (classic retro polygon wobble), while ${systemB.shortName} glides smoothly with sub-pixel precision and a Z-buffer.")
                else ->
                    append("Rotating the 3D model uses ${report3DA.assetBudgetPercent.roundToInt()}% of ${systemA.shortName}'s per-character polygon budget compared to ${report3DB.assetBudgetPercent.roundToInt()}% on ${systemB.shortName}.")
            }
        }
    } else {
        val byteDelta = abs(report2DA.chrVramBytes - report2DB.chrVramBytes)
        buildString {
            append("Silicon Engineering Readout: ")
            append(
                "2D PPU/VDP Delta — ${systemA.shortName} packs ${report2DA.totalChrTiles} tiles into ${report2DA.chrVramBytes}B (${systemA.spriteTileSize}) taking ${String.format("%.2f%%", report2DA.vramUsagePercent)} of its ${systemA.vramBytes / 1024}KB VRAM, while ${systemB.shortName} allocates ${report2DB.chrVramBytes}B (Δ ${byteDelta}B) across ${report2DB.totalChrTiles} tiles (${String.format("%.2f%%", report2DB.vramUsagePercent)} of ${systemB.vramBytes / 1024}KB VRAM). "
            )
            append(
                "3D Geometry & Raster Delta — ${systemA.shortName} (${systemA.gpuOrPpuSummary}) allocates ${report3DA.totalAssetRamBytes}B (${report3DA.vertexBufferBytes}B VBO + ${report3DA.faceTableBytes}B Face/OT + ${report3DA.estimatedTextureBytes}B Tex) under ${systemA.renderProfile3D.badge}, whereas ${systemB.shortName} (${systemB.gpuOrPpuSummary}) allocates ${report3DB.totalAssetRamBytes}B under ${systemB.renderProfile3D.badge}."
            )
        }
    }
}
