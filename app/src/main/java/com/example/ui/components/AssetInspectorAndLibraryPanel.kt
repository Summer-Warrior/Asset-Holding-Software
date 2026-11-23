package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.library.LibraryAssetType
import com.example.model.library.SavedLibraryAsset
import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import com.example.ui.viewer3d.Viewport3D
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt

enum class InspectorMode(val label: String) {
    INSPECT_3D("3D Rotate"),
    INSPECT_SPRITE("Sprite Rotate"),
    INSPECT_DUAL("Dual 3D+2D"),
    INSPECT_COMPARE("Sys Compare"),
    INSPECT_CHIP_FUSION("Chip Fusion"),
    INSPECT_FAN_CHIPS("Fan Chips")
}

@Composable
fun AssetPreviewInspectorCard(
    mesh: Mesh3D,
    spriteSheet: SpriteSheetData,
    activeTexture: android.graphics.Bitmap?,
    hardwareSpec: ConsoleHardwareSpec,
    hardwareSimulationEnabled: Boolean,
    defaultCharacterTag: String,
    defaultMode: InspectorMode = InspectorMode.INSPECT_DUAL,
    defaultSaveType: LibraryAssetType = LibraryAssetType.UNIFIED_PAIR,
    savedLibraryAssets: List<SavedLibraryAsset>,
    onToggleHardwareSimulation: () -> Unit,
    onSaveToLibrary: (
        title: String,
        characterTag: String,
        type: LibraryAssetType,
        yawDeg: Float,
        pitchDeg: Float,
        spriteRotDeg: Float
    ) -> Unit,
    onLoadFromLibrary: (SavedLibraryAsset) -> Unit,
    onDeleteFromLibrary: (String) -> Unit,
    onActivateFusedHardware: (ConsoleHardwareSpec) -> Unit = {
        com.example.model.hardware.ConsoleHardwareCatalog.registerCustomFusedSpec(it)
    },
    modifier: Modifier = Modifier
) {
    var inspectorMode by remember { mutableStateOf(defaultMode) }
    var saveType by remember { mutableStateOf(defaultSaveType) }
    var customTitle by remember(mesh.name, spriteSheet.name, saveType) {
        mutableStateOf(
            when (saveType) {
                LibraryAssetType.MODEL_3D -> mesh.name
                LibraryAssetType.SPRITE_2D -> spriteSheet.name
                LibraryAssetType.UNIFIED_PAIR -> "${defaultCharacterTag.ifBlank { "Studio" }} • ${mesh.name.substringBefore(" [")}"
            }
        )
    }

    // 3D Inspection Camera & Light State
    var yawDegrees by remember { mutableFloatStateOf(40f) }
    var pitchDegrees by remember { mutableFloatStateOf(22f) }
    var zoom3D by remember { mutableFloatStateOf(1.35f) }
    var lightAzimuthDegrees by remember { mutableFloatStateOf(45f) }

    // 2D Sprite Inspection Rotation, Flip, Zoom & Playback State
    var spriteRotationDegrees by remember { mutableFloatStateOf(0f) }
    var spriteZoom by remember { mutableFloatStateOf(1.4f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var showPixelGrid by remember { mutableStateOf(true) }
    var showOnionSkin by remember { mutableStateOf(false) }
    var isPlayingSprite by remember { mutableStateOf(true) }
    var spriteFps by remember { mutableIntStateOf(8) }
    var selectedFrameIdx by remember { mutableIntStateOf(0) }

    // System Comparison State (System A vs. System B)
    var compareSystemAId by remember(hardwareSpec.id) { mutableStateOf(hardwareSpec.id) }
    var compareSystemBId by remember(hardwareSpec.id) {
        mutableStateOf(defaultRivalSystemId(hardwareSpec.id))
    }

    val frames = spriteSheet.frames
    LaunchedEffect(isPlayingSprite, spriteFps, frames.size) {
        if (isPlayingSprite && frames.size > 1) {
            while (true) {
                delay((1000L / spriteFps.coerceIn(1, 30)).coerceAtLeast(33L))
                selectedFrameIdx = (selectedFrameIdx + 1) % frames.size
            }
        }
    }

    val yawRadians = (yawDegrees * PI / 180.0).toFloat()
    val pitchRadians = (pitchDegrees * PI / 180.0).toFloat()
    val lightRadians = (lightAzimuthDegrees * PI / 180.0).toFloat()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_preview_inspector_card")
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
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PRE-SAVE 3D & SPRITE ROTATION INSPECTOR",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Rotate, zoom, and inspect your 3D model and 2D sprite before saving to your Asset Library",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${savedLibraryAssets.size} IN LIBRARY",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Mode Tabs: 3D Rotate | Sprite Rotate | Dual 3D+2D | Sys Compare | Chip Fusion | Fan Chips
            ScrollableTabRow(
                selectedTabIndex = inspectorMode.ordinal,
                edgePadding = 4.dp,
                containerColor = Color(0xFF070B12),
                contentColor = CyanNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[inspectorMode.ordinal]),
                        color = CyanNeon
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            ) {
                InspectorMode.entries.forEach { mode ->
                    Tab(
                        selected = inspectorMode == mode,
                        onClick = { inspectorMode = mode },
                        text = {
                            Text(
                                text = mode.label,
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("inspector_tab_${mode.name.lowercase()}")
                    )
                }
            }

            // Active Inspection Viewport Area
            when (inspectorMode) {
                InspectorMode.INSPECT_3D -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Viewport3D(
                                mesh = mesh,
                                activeTexture = activeTexture,
                                hardwareSpec = hardwareSpec,
                                hardwareSimulationEnabled = hardwareSimulationEnabled,
                                onToggleHardwareSimulation = onToggleHardwareSimulation,
                                externalYawRadians = yawRadians,
                                externalPitchRadians = pitchRadians,
                                externalZoom = zoom3D,
                                externalLightAngle = lightRadians,
                                initialAutoRotate = false,
                                onCameraOrientationChanged = { newYawRad, newPitchRad, newZoom ->
                                    val degY = ((newYawRad * 180f / PI.toFloat()) % 360f + 360f) % 360f
                                    val degP = (newPitchRad * 180f / PI.toFloat()).coerceIn(-85f, 85f)
                                    yawDegrees = degY
                                    pitchDegrees = degP
                                    zoom3D = newZoom
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Quick 3D Camera Angle Presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Angles:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            listOf(
                                "Front 0°" to (0f to 10f),
                                "Iso 45°" to (45f to 25f),
                                "Right 90°" to (90f to 10f),
                                "Back 180°" to (180f to 10f),
                                "Left 270°" to (270f to 10f),
                                "Top-Down" to (45f to 80f)
                            ).forEach { (label, angles) ->
                                FilterChip(
                                    selected = (yawDegrees - angles.first).let { kotlin.math.abs(it) < 3f } &&
                                            (pitchDegrees - angles.second).let { kotlin.math.abs(it) < 5f },
                                    onClick = {
                                        yawDegrees = angles.first
                                        pitchDegrees = angles.second
                                    },
                                    label = { Text(label, fontFamily = TechMono, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanNeon,
                                        selectedLabelColor = Color(0xFF031B26),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Precision 3D Sliders (Yaw, Pitch, Zoom, Light)
                        InspectorSliderRow(
                            label = "3D Yaw Orbit",
                            valueText = "${yawDegrees.roundToInt()}°",
                            value = yawDegrees,
                            valueRange = 0f..360f,
                            accentColor = CyanNeon,
                            testTag = "inspector_3d_yaw_slider",
                            onValueChange = { yawDegrees = it }
                        )
                        InspectorSliderRow(
                            label = "3D Pitch Tilt",
                            valueText = "${pitchDegrees.roundToInt()}°",
                            value = pitchDegrees,
                            valueRange = -85f..85f,
                            accentColor = CyanNeon,
                            testTag = "inspector_3d_pitch_slider",
                            onValueChange = { pitchDegrees = it }
                        )
                        InspectorSliderRow(
                            label = "Light Azimuth",
                            valueText = "${lightAzimuthDegrees.roundToInt()}°",
                            value = lightAzimuthDegrees,
                            valueRange = 0f..360f,
                            accentColor = AmberGold,
                            testTag = "inspector_3d_light_slider",
                            onValueChange = { lightAzimuthDegrees = it }
                        )
                    }
                }

                InspectorMode.INSPECT_SPRITE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val safeIdx = selectedFrameIdx.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
                        val currentFrame = frames.getOrElse(safeIdx) { spriteSheet.sourceBitmap }
                        val prevFrame = if (frames.size > 1) {
                            frames[(safeIdx - 1 + frames.size) % frames.size]
                        } else null

                        // Interactive 2D Sprite Inspection Stage (Touch Rotate & Zoom + Grid + Onion Skin)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF070B12))
                                .border(1.dp, MagentaNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoomChange, rotationChange ->
                                        spriteZoom = (spriteZoom * zoomChange).coerceIn(0.6f, 4.0f)
                                        spriteRotationDegrees = ((spriteRotationDegrees + rotationChange) % 360f + 360f) % 360f
                                    }
                                }
                                .testTag("sprite_inspector_stage"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle pixel checkerboard & grid canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val step = 20.dp.toPx()
                                val cols = (size.width / step).toInt() + 1
                                val rows = (size.height / step).toInt() + 1
                                for (r in 0 until rows) {
                                    for (c in 0 until cols) {
                                        if ((r + c) % 2 == 0) {
                                            drawRect(
                                                color = Color(0xFF0F172A),
                                                topLeft = Offset(c * step, r * step),
                                                size = Size(step, step)
                                            )
                                        }
                                    }
                                }
                                if (showPixelGrid) {
                                    val gridStep = 16.dp.toPx()
                                    var gx = 0f
                                    while (gx < size.width) {
                                        drawLine(Color(0x2238BDF8), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                                        gx += gridStep
                                    }
                                    var gy = 0f
                                    while (gy < size.height) {
                                        drawLine(Color(0x2238BDF8), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                                        gy += gridStep
                                    }
                                }
                            }

                            // Onion Skin Previous Frame Overlay
                            if (showOnionSkin && prevFrame != null) {
                                Image(
                                    bitmap = prevFrame.asImageBitmap(),
                                    contentDescription = "Previous Frame Onion Skin",
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(165.dp)
                                        .graphicsLayer {
                                            rotationZ = spriteRotationDegrees
                                            scaleX = spriteZoom * (if (flipHorizontal) -1f else 1f)
                                            scaleY = spriteZoom * (if (flipVertical) -1f else 1f)
                                            alpha = 0.32f
                                        }
                                )
                            }

                            // Active Sprite Frame with Live Rotation, Flip, and Zoom
                            Image(
                                bitmap = currentFrame.asImageBitmap(),
                                contentDescription = "Inspected Sprite Frame",
                                filterQuality = FilterQuality.None,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(165.dp)
                                    .graphicsLayer {
                                        rotationZ = spriteRotationDegrees
                                        scaleX = spriteZoom * (if (flipHorizontal) -1f else 1f)
                                        scaleY = spriteZoom * (if (flipVertical) -1f else 1f)
                                    }
                            )

                            // Top-Left Sprite Telemetry Badge
                            Surface(
                                color = Color(0xCC090D16),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Frame ${safeIdx + 1}/${frames.size} • Rot: ${spriteRotationDegrees.roundToInt()}° • Zoom: ${String.format("%.1fx", spriteZoom)}",
                                    fontFamily = TechMono,
                                    fontSize = 10.sp,
                                    color = MagentaNeon,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Top-Right Sprite Inspection Quick Toggles
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { spriteRotationDegrees = (spriteRotationDegrees + 90f) % 360f },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xCC1E293B), RoundedCornerShape(8.dp))
                                        .testTag("sprite_rotate_90_btn")
                                ) {
                                    Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = "Rotate 90°", tint = MagentaNeon, modifier = Modifier.size(17.dp))
                                }
                                IconButton(
                                    onClick = { flipHorizontal = !flipHorizontal },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (flipHorizontal) MagentaNeon else Color(0xCC1E293B), RoundedCornerShape(8.dp))
                                        .testTag("sprite_flip_h_btn")
                                ) {
                                    Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = Color.White, modifier = Modifier.size(17.dp))
                                }
                                IconButton(
                                    onClick = { showOnionSkin = !showOnionSkin },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (showOnionSkin) CyanNeon else Color(0xCC1E293B), RoundedCornerShape(8.dp))
                                        .testTag("sprite_onion_skin_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = "Onion Skin",
                                        tint = if (showOnionSkin) Color(0xFF031B26) else Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showPixelGrid = !showPixelGrid },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (showPixelGrid) EmeraldGreen else Color(0xCC1E293B), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        Icons.Default.GridOn,
                                        contentDescription = "Pixel Grid",
                                        tint = if (showPixelGrid) Color(0xFF031B26) else Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        spriteRotationDegrees = 0f
                                        spriteZoom = 1.4f
                                        flipHorizontal = false
                                        flipVertical = false
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xCC1E293B), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Sprite View", tint = Color.White, modifier = Modifier.size(17.dp))
                                }
                            }
                        }

                        // Frame Scrubber & Playback Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isPlayingSprite = !isPlayingSprite },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MagentaNeon.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .testTag("sprite_inspector_play_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingSprite) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause Sprite",
                                    tint = MagentaNeon
                                )
                            }
                            frames.forEachIndexed { idx, frameBmp ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(
                                            width = if (idx == safeIdx) 2.dp else 1.dp,
                                            color = if (idx == safeIdx) MagentaNeon else CardBorder,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            isPlayingSprite = false
                                            selectedFrameIdx = idx
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = frameBmp.asImageBitmap(),
                                        contentDescription = "Frame $idx",
                                        filterQuality = FilterQuality.None,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }

                        // Sprite Rotation & Zoom Sliders
                        InspectorSliderRow(
                            label = "Sprite Rotation",
                            valueText = "${spriteRotationDegrees.roundToInt()}°",
                            value = spriteRotationDegrees,
                            valueRange = 0f..360f,
                            accentColor = MagentaNeon,
                            testTag = "inspector_sprite_rotation_slider",
                            onValueChange = { spriteRotationDegrees = it }
                        )
                        InspectorSliderRow(
                            label = "Sprite Zoom",
                            valueText = String.format("%.1fx", spriteZoom),
                            value = spriteZoom,
                            valueRange = 0.6f..4.0f,
                            accentColor = EmeraldGreen,
                            testTag = "inspector_sprite_zoom_slider",
                            onValueChange = { spriteZoom = it }
                        )
                    }
                }

                InspectorMode.INSPECT_DUAL -> {
                    // Synchronized Split 3D Model + 2D Sprite Turntable Inspector
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(235.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left Pane: 3D Model Orbit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Viewport3D(
                                    mesh = mesh,
                                    activeTexture = activeTexture,
                                    hardwareSpec = hardwareSpec,
                                    hardwareSimulationEnabled = hardwareSimulationEnabled,
                                    externalYawRadians = yawRadians,
                                    externalPitchRadians = pitchRadians,
                                    externalZoom = zoom3D,
                                    externalLightAngle = lightRadians,
                                    initialAutoRotate = false,
                                    onCameraOrientationChanged = { newYawRad, newPitchRad, newZoom ->
                                        yawDegrees = ((newYawRad * 180f / PI.toFloat()) % 360f + 360f) % 360f
                                        pitchDegrees = (newPitchRad * 180f / PI.toFloat()).coerceIn(-85f, 85f)
                                        zoom3D = newZoom
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Right Pane: 2D Sprite Rotation & Animation Stage
                            val safeIdx = selectedFrameIdx.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
                            val currentFrame = frames.getOrElse(safeIdx) { spriteSheet.sourceBitmap }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070B12))
                                    .border(1.dp, MagentaNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoomChange, rotationChange ->
                                            spriteZoom = (spriteZoom * zoomChange).coerceIn(0.6f, 3.5f)
                                            spriteRotationDegrees = ((spriteRotationDegrees + rotationChange) % 360f + 360f) % 360f
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentFrame.asImageBitmap(),
                                    contentDescription = "Dual Inspector Sprite",
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(135.dp)
                                        .graphicsLayer {
                                            rotationZ = spriteRotationDegrees
                                            scaleX = spriteZoom * (if (flipHorizontal) -1f else 1f)
                                            scaleY = spriteZoom * (if (flipVertical) -1f else 1f)
                                        }
                                )
                                Surface(
                                    color = Color(0xCC090D16),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "2D • ${spriteRotationDegrees.roundToInt()}° (${safeIdx + 1}/${frames.size})",
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = MagentaNeon,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Color(0xDD1E293B),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable {
                                            spriteRotationDegrees = (spriteRotationDegrees + 90f) % 360f
                                        }
                                    ) {
                                        Text(
                                            text = "Rot 90°",
                                            fontFamily = TechMono,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        color = if (flipHorizontal) MagentaNeon else Color(0xDD1E293B),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { flipHorizontal = !flipHorizontal }
                                    ) {
                                        Text(
                                            text = "Flip X",
                                            fontFamily = TechMono,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Linked 3D Yaw + 2D Sprite Rotation Sliders
                        InspectorSliderRow(
                            label = "3D Model Yaw",
                            valueText = "${yawDegrees.roundToInt()}°",
                            value = yawDegrees,
                            valueRange = 0f..360f,
                            accentColor = CyanNeon,
                            testTag = "dual_inspector_3d_yaw_slider",
                            onValueChange = { yawDegrees = it }
                        )
                        InspectorSliderRow(
                            label = "2D Sprite Angle",
                            valueText = "${spriteRotationDegrees.roundToInt()}°",
                            value = spriteRotationDegrees,
                            valueRange = 0f..360f,
                            accentColor = MagentaNeon,
                            testTag = "dual_inspector_sprite_rot_slider",
                            onValueChange = { spriteRotationDegrees = it }
                        )
                    }
                }

                InspectorMode.INSPECT_COMPARE -> {
                    val safeIdx = selectedFrameIdx.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
                    val currentFrame = frames.getOrElse(safeIdx) { spriteSheet.sourceBitmap }
                    SystemComparisonPreviewSection(
                        mesh = mesh,
                        currentSpriteFrame = currentFrame,
                        activeTexture = activeTexture,
                        systemAId = compareSystemAId,
                        systemBId = compareSystemBId,
                        onSelectSystemA = { compareSystemAId = it },
                        onSelectSystemB = { compareSystemBId = it },
                        onSwapSystems = {
                            val tmp = compareSystemAId
                            compareSystemAId = compareSystemBId
                            compareSystemBId = tmp
                        },
                        yawDegrees = yawDegrees,
                        pitchDegrees = pitchDegrees,
                        zoom3D = zoom3D,
                        lightAzimuthDegrees = lightAzimuthDegrees,
                        onUpdate3DCamera = { newYaw, newPitch, newZoom ->
                            yawDegrees = newYaw
                            pitchDegrees = newPitch
                            zoom3D = newZoom
                        },
                        spriteRotationDegrees = spriteRotationDegrees,
                        spriteZoom = spriteZoom,
                        flipHorizontal = flipHorizontal,
                        showPixelGrid = showPixelGrid,
                        onUpdateSpriteTransform = { newRot, newZoom ->
                            spriteRotationDegrees = newRot
                            spriteZoom = newZoom
                        },
                        onToggleFlipHorizontal = { flipHorizontal = !flipHorizontal },
                        onTogglePixelGrid = { showPixelGrid = !showPixelGrid }
                    )
                }

                InspectorMode.INSPECT_CHIP_FUSION -> {
                    ChipFusionLabPanel(
                        mesh = mesh,
                        spriteSheet = spriteSheet,
                        activeTexture = activeTexture,
                        readingLevelMode = com.example.model.hardware.ReadingLevelMode.BOTH_LEVELS,
                        showTileGrid = showPixelGrid,
                        onActivateFusedHardware = { fusedSpec ->
                            onActivateFusedHardware(fusedSpec)
                            compareSystemBId = fusedSpec.id
                        }
                    )
                }

                InspectorMode.INSPECT_FAN_CHIPS -> {
                    FanChipPreviewPanel(
                        mesh = mesh,
                        spriteSheet = spriteSheet,
                        activeTexture = activeTexture,
                        yawDegrees = yawDegrees,
                        pitchDegrees = pitchDegrees,
                        zoom3D = zoom3D,
                        lightAzimuthDegrees = lightAzimuthDegrees,
                        onUpdate3DCamera = { newYaw, newPitch, newZoom ->
                            yawDegrees = newYaw
                            pitchDegrees = newPitch
                            zoom3D = newZoom
                        },
                        spriteRotationDegrees = spriteRotationDegrees,
                        spriteZoom = spriteZoom,
                        flipHorizontal = flipHorizontal,
                        showTileGrid = showPixelGrid,
                        onUpdateSpriteRotation = { spriteRotationDegrees = it },
                        onActivateFanSystem = { fanSpec ->
                            onActivateFusedHardware(fanSpec)
                            compareSystemBId = fanSpec.id
                        }
                    )
                }
            }

            HorizontalDivider(color = CardBorder)

            // Pre-Save Library Staging & Commit Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070C16))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SAVE INSPECTED ASSET TO PERSONAL LIBRARY",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )

                // Save Scope Chips (3D Model | 2D Sprite | 3D + Sprite Pair)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryAssetType.entries.forEach { type ->
                        FilterChip(
                            selected = saveType == type,
                            onClick = { saveType = type },
                            label = { Text(type.label, fontFamily = TechMono, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("save_scope_${type.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Library Asset Name", fontFamily = TechMono, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inspector_library_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = {
                            onSaveToLibrary(
                                customTitle.ifBlank { mesh.name },
                                defaultCharacterTag.ifBlank { "Studio" },
                                saveType,
                                yawDegrees,
                                pitchDegrees,
                                spriteRotationDegrees
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color(0xFF031B26)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("save_inspected_asset_to_library_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save to Library", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Saved User Asset Library Shelf
            if (savedLibraryAssets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CollectionsBookmark,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "SAVED ASSET LIBRARY (${savedLibraryAssets.size})",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        savedLibraryAssets.forEach { item ->
                            SavedLibraryItemCard(
                                item = item,
                                onLoad = {
                                    yawDegrees = item.inspectedYawDegrees
                                    pitchDegrees = item.inspectedPitchDegrees
                                    spriteRotationDegrees = item.inspectedSpriteRotationDegrees
                                    onLoadFromLibrary(item)
                                },
                                onDelete = { onDeleteFromLibrary(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorSliderRow(
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

@Composable
private fun SavedLibraryItemCard(
    item: SavedLibraryAsset,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    AssetPreviewCard(
        item = item,
        onLoad = onLoad,
        onDelete = onDelete,
        modifier = Modifier.width(245.dp)
    )
}
