package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.masterpack.TWENTY_COLOR_WHEEL
import com.example.model.sprite.PixelDrawTool
import com.example.model.sprite.SpriteCanvasEditorEngine
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import kotlinx.coroutines.delay

private val CanvasObsidianBg = Color(0xFF0A1120)
private val CanvasCardBg = Color(0xFF0F172A)
private val CanvasMutedText = Color(0xFF94A3B8)

/**
 * Interactive Blank-Canvas 2D Pixel & Animation Editor.
 * Lets creators start from a completely blank transparent canvas (16×16..48×48, 1..16 frames),
 * paint with tap/drag strokes, mirror X symmetry, onion skinning, 20-Color Wheel palette,
 * multi-frame animation playback, undo/redo, and commit straight to Studio & Vault.
 */
@Composable
fun SpriteCanvasEditorPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onCommittedToStudio: (() -> Unit)? = null
) {
    val project = uiState.scratchSpriteProject
    val latestProject by rememberUpdatedState(project)

    var setupSizePx by remember(project.width) { mutableIntStateOf(project.width) }
    var setupFrameCount by remember { mutableIntStateOf(project.frames.size.coerceIn(1, 8)) }
    var isPlayingPreview by remember { mutableStateOf(false) }
    var previewFrameIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlayingPreview, project.frames.size, project.fps) {
        if (!isPlayingPreview || project.frames.size <= 1) return@LaunchedEffect
        while (true) {
            val delayMs = (1000L / project.fps.coerceIn(1, 30)).coerceAtLeast(33L)
            delay(delayMs)
            previewFrameIdx = (previewFrameIdx + 1) % project.frames.size
        }
    }

    val displayedFrameIndex = if (isPlayingPreview && project.frames.isNotEmpty()) {
        previewFrameIdx.coerceIn(0, project.frames.lastIndex)
    } else {
        project.activeFrameIndex.coerceIn(0, (project.frames.size - 1).coerceAtLeast(0))
    }

    val displayedPixels = project.frames.getOrElse(displayedFrameIndex) { project.activeFramePixels }
    val onionPixels = if (uiState.scratchOnionSkinEnabled && !isPlayingPreview && project.frames.size > 1) {
        val prevIdx = if (displayedFrameIndex > 0) displayedFrameIndex - 1 else project.frames.lastIndex
        project.frames.getOrNull(prevIdx)
    } else {
        null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sprite_canvas_editor_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CanvasObsidianBg),
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header & "New Blank Canvas" Setup Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FROM-SCRATCH 2D PIXEL & ANIMATION CANVAS",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                    Text(
                        text = "${project.width}×${project.height}px • Frame ${displayedFrameIndex + 1}/${project.frames.size} • ${project.fps} FPS",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = CanvasMutedText
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldGreen.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "OFFLINE READY",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CanvasCardBg,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = project.name,
                        onValueChange = { viewModel.updateScratchSpriteName(it) },
                        label = { Text("New Sprite Asset Name", fontFamily = TechMono, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scratch_sprite_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyanNeon,
                            unfocusedLabelColor = CanvasMutedText
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Size:", fontFamily = TechMono, fontSize = 10.sp, color = CanvasMutedText)
                        listOf(16, 24, 32, 48).forEach { dim ->
                            FilterChip(
                                selected = setupSizePx == dim,
                                onClick = { setupSizePx = dim },
                                label = { Text("${dim}×${dim}", fontFamily = TechMono, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon.copy(alpha = 0.22f),
                                    selectedLabelColor = CyanNeon,
                                    containerColor = CanvasObsidianBg,
                                    labelColor = CanvasMutedText
                                ),
                                modifier = Modifier.testTag("canvas_size_chip_$dim")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Frames:", fontFamily = TechMono, fontSize = 10.sp, color = CanvasMutedText)
                            listOf(1, 4, 8).forEach { count ->
                                FilterChip(
                                    selected = setupFrameCount == count,
                                    onClick = { setupFrameCount = count },
                                    label = { Text("$count", fontFamily = TechMono, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MagentaNeon.copy(alpha = 0.22f),
                                        selectedLabelColor = MagentaNeon,
                                        containerColor = CanvasObsidianBg,
                                        labelColor = CanvasMutedText
                                    ),
                                    modifier = Modifier.testTag("canvas_frames_chip_$count")
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isPlayingPreview = false
                                viewModel.startNewBlankSpriteProject(project.name, setupSizePx, setupFrameCount)
                            },
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("start_blank_canvas_btn"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CyanNeon),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                        ) {
                            Text("Start Blank Canvas", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Interactive Square Touch Pixel Canvas (Tap + Continuous Drag)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (isPlayingPreview) EmeraldGreen else CyanNeon.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("interactive_pixel_canvas")
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (isPlayingPreview) return@detectTapGestures
                            val p = latestProject
                            val gx = ((offset.x / size.width.coerceAtLeast(1)) * p.width).toInt().coerceIn(0, p.width - 1)
                            val gy = ((offset.y / size.height.coerceAtLeast(1)) * p.height).toInt().coerceIn(0, p.height - 1)
                            viewModel.applyCanvasStrokeAt(gx, gy, pushUndoSnapshot = true)
                        }
                    }
                    .pointerInput(Unit) {
                        var strokeStarted = false
                        var lastCellX = -1
                        var lastCellY = -1
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                if (isPlayingPreview) return@detectDragGestures
                                val p = latestProject
                                val gx = ((startOffset.x / size.width.coerceAtLeast(1)) * p.width).toInt().coerceIn(0, p.width - 1)
                                val gy = ((startOffset.y / size.height.coerceAtLeast(1)) * p.height).toInt().coerceIn(0, p.height - 1)
                                strokeStarted = true
                                lastCellX = gx
                                lastCellY = gy
                                viewModel.applyCanvasStrokeAt(gx, gy, pushUndoSnapshot = true)
                            },
                            onDragEnd = {
                                strokeStarted = false
                                lastCellX = -1
                                lastCellY = -1
                            },
                            onDragCancel = {
                                strokeStarted = false
                                lastCellX = -1
                                lastCellY = -1
                            },
                            onDrag = { change, _ ->
                                if (isPlayingPreview) return@detectDragGestures
                                change.consume()
                                val p = latestProject
                                val pos = change.position
                                val gx = ((pos.x / size.width.coerceAtLeast(1)) * p.width).toInt().coerceIn(0, p.width - 1)
                                val gy = ((pos.y / size.height.coerceAtLeast(1)) * p.height).toInt().coerceIn(0, p.height - 1)
                                if (gx != lastCellX || gy != lastCellY) {
                                    lastCellX = gx
                                    lastCellY = gy
                                    viewModel.applyCanvasStrokeAt(gx, gy, pushUndoSnapshot = !strokeStarted)
                                    strokeStarted = true
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = project.width.coerceAtLeast(1)
                    val rows = project.height.coerceAtLeast(1)
                    val cellW = size.width / cols.toFloat()
                    val cellH = size.height / rows.toFloat()

                    val checkerDark = Color(0xFF0A1120)
                    val checkerLight = Color(0xFF131F36)

                    // Checkerboard transparency background + Onion Skin + Active Pixels
                    for (y in 0 until rows) {
                        val rowOff = y * cols
                        val top = y * cellH
                        for (x in 0 until cols) {
                            val left = x * cellW
                            drawRect(
                                color = if ((x + y) % 2 == 0) checkerDark else checkerLight,
                                topLeft = Offset(left, top),
                                size = Size(cellW + 0.5f, cellH + 0.5f)
                            )

                            val idx = rowOff + x
                            if (onionPixels != null && idx < onionPixels.size) {
                                val ghostArgb = onionPixels[idx]
                                if (AndroidColor.alpha(ghostArgb) > 0) {
                                    drawRect(
                                        color = Color(ghostArgb).copy(alpha = 0.30f),
                                        topLeft = Offset(left, top),
                                        size = Size(cellW + 0.5f, cellH + 0.5f)
                                    )
                                }
                            }

                            if (idx < displayedPixels.size) {
                                val argb = displayedPixels[idx]
                                if (AndroidColor.alpha(argb) > 0) {
                                    drawRect(
                                        color = Color(argb),
                                        topLeft = Offset(left, top),
                                        size = Size(cellW + 0.5f, cellH + 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Subtle pixel gridlines
                    val gridColor = Color.White.copy(alpha = if (cols <= 32) 0.08f else 0.05f)
                    for (x in 1 until cols) {
                        val xPos = x * cellW
                        drawLine(gridColor, Offset(xPos, 0f), Offset(xPos, size.height), strokeWidth = 1f)
                    }
                    for (y in 1 until rows) {
                        val yPos = y * cellH
                        drawLine(gridColor, Offset(0f, yPos), Offset(size.width, yPos), strokeWidth = 1f)
                    }

                    // Center vertical symmetry guide line when Mirror X is ON
                    if (project.mirrorX) {
                        val centerX = size.width / 2f
                        drawLine(
                            color = MagentaNeon.copy(alpha = 0.85f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 2.5f
                        )
                    }
                }
            }

            // 3. Tool & Brush Bar + Undo / Redo / Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelDrawTool.entries.forEach { tool ->
                    val selected = uiState.activeDrawTool == tool
                    val icon = when (tool) {
                        PixelDrawTool.PENCIL -> Icons.Default.Brush
                        PixelDrawTool.ERASER -> Icons.Default.DeleteSweep
                        PixelDrawTool.FILL_BUCKET -> Icons.Default.FormatColorFill
                        PixelDrawTool.EYEDROPPER -> Icons.Default.Colorize
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setPixelDrawTool(tool) },
                        leadingIcon = { Icon(icon, contentDescription = tool.label, modifier = Modifier.size(14.dp)) },
                        label = { Text(tool.label, fontFamily = TechMono, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon.copy(alpha = 0.24f),
                            selectedLabelColor = CyanNeon,
                            selectedLeadingIconColor = CyanNeon,
                            containerColor = CanvasCardBg,
                            labelColor = CanvasMutedText
                        ),
                        modifier = Modifier.testTag("draw_tool_${tool.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 3).forEach { sz ->
                        FilterChip(
                            selected = uiState.activeBrushSizePx == sz,
                            onClick = { viewModel.setBrushSizePx(sz) },
                            label = { Text("${sz}px", fontFamily = TechMono, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold.copy(alpha = 0.22f),
                                selectedLabelColor = AmberGold,
                                containerColor = CanvasCardBg,
                                labelColor = CanvasMutedText
                            ),
                            modifier = Modifier.testTag("brush_size_$sz")
                        )
                    }
                    FilterChip(
                        selected = project.mirrorX,
                        onClick = { viewModel.toggleScratchMirrorX() },
                        leadingIcon = { Icon(Icons.Default.Flip, contentDescription = "Mirror X", modifier = Modifier.size(14.dp)) },
                        label = { Text("Mirror X", fontFamily = TechMono, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MagentaNeon.copy(alpha = 0.24f),
                            selectedLabelColor = MagentaNeon,
                            selectedLeadingIconColor = MagentaNeon,
                            containerColor = CanvasCardBg,
                            labelColor = CanvasMutedText
                        ),
                        modifier = Modifier.testTag("toggle_mirror_x_chip")
                    )
                    FilterChip(
                        selected = uiState.scratchOnionSkinEnabled,
                        onClick = { viewModel.toggleScratchOnionSkin() },
                        leadingIcon = { Icon(Icons.Default.Layers, contentDescription = "Onion Skin", modifier = Modifier.size(14.dp)) },
                        label = { Text("Onion", fontFamily = TechMono, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.22f),
                            selectedLabelColor = EmeraldGreen,
                            selectedLeadingIconColor = EmeraldGreen,
                            containerColor = CanvasCardBg,
                            labelColor = CanvasMutedText
                        ),
                        modifier = Modifier.testTag("toggle_onion_skin_chip")
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.undoScratchSprite() },
                        enabled = uiState.scratchCanUndo,
                        modifier = Modifier.testTag("scratch_undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Stroke",
                            tint = if (uiState.scratchCanUndo) CyanNeon else CanvasMutedText.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redoScratchSprite() },
                        enabled = uiState.scratchCanRedo,
                        modifier = Modifier.testTag("scratch_redo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo Stroke",
                            tint = if (uiState.scratchCanRedo) CyanNeon else CanvasMutedText.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearActiveScratchFrame() },
                        modifier = Modifier.testTag("scratch_clear_frame_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Active Frame",
                            tint = MagentaNeon
                        )
                    }
                }
            }

            // 4. 20-Color Wheel Palette Swatches
            val activeSwatchEntry = TWENTY_COLOR_WHEEL.firstOrNull { it.colorInt == uiState.activeBrushColorInt }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "20-COLOR PALETTE",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(uiState.activeBrushColorInt))
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Text(
                        text = activeSwatchEntry?.let { "${it.name} (${it.hex})" }
                            ?: String.format("#%06X", 0xFFFFFF and uiState.activeBrushColorInt),
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TWENTY_COLOR_WHEEL.chunked(10).forEach { rowSwatches ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowSwatches.forEach { swatch ->
                            val isSelected = uiState.activeBrushColorInt == swatch.colorInt
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch.colorInt))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else CardBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setBrushColorInt(swatch.colorInt) }
                                    .testTag("palette_swatch_${swatch.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = swatch.name,
                                        tint = if (AndroidColor.red(swatch.colorInt) + AndroidColor.green(swatch.colorInt) > 320) {
                                            Color.Black
                                        } else {
                                            Color.White
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = CardBorder)

            // 5. Animation Frame Strip + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANIMATION FRAMES (${project.frames.size}/16)",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(4, 8, 12, 15).forEach { targetFps ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.setScratchFps(targetFps) },
                            color = if (project.fps == targetFps) CyanNeon.copy(alpha = 0.22f) else CanvasCardBg,
                            border = BorderStroke(1.dp, if (project.fps == targetFps) CyanNeon else CardBorder)
                        ) {
                            Text(
                                text = "${targetFps}fps",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = if (project.fps == targetFps) CyanNeon else CanvasMutedText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(project.frames) { idx, pixels ->
                    val frameBmp = remember(pixels, project.width, project.height) {
                        SpriteCanvasEditorEngine.frameToBitmap(pixels, project.width, project.height)
                    }
                    val selected = idx == displayedFrameIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) CyanNeon.copy(alpha = 0.16f) else CanvasCardBg)
                            .border(1.5.dp, if (selected) CyanNeon else CardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                isPlayingPreview = false
                                viewModel.selectScratchFrame(idx)
                            }
                            .padding(5.dp)
                            .testTag("scratch_frame_thumb_$idx")
                    ) {
                        Image(
                            bitmap = frameBmp.asImageBitmap(),
                            contentDescription = "Frame ${idx + 1}",
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF070C17), RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "F${idx + 1}",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = if (selected) CyanNeon else CanvasMutedText
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isPlayingPreview = false
                        viewModel.addBlankScratchFrame()
                    },
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("add_blank_frame_btn"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Blank Frame", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Blank", fontFamily = TechMono, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = {
                        isPlayingPreview = false
                        viewModel.duplicateActiveScratchFrame()
                    },
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("duplicate_frame_btn"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Frame", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Duplicate", fontFamily = TechMono, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = {
                        isPlayingPreview = false
                        viewModel.deleteActiveScratchFrame()
                    },
                    enabled = project.frames.size > 1,
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("delete_frame_btn"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Frame", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontFamily = TechMono, fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        if (project.frames.size > 1) {
                            isPlayingPreview = !isPlayingPreview
                        }
                    },
                    enabled = project.frames.size > 1,
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("play_pause_scratch_anim_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingPreview) MagentaNeon else CyanNeon,
                        contentColor = if (isPlayingPreview) Color.White else Color(0xFF031B26)
                    )
                ) {
                    Icon(
                        imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingPreview) "Pause Animation" else "Play Animation",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPlayingPreview) "Pause" else "Play", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 6. Prominent Emerald Commit CTA
            Button(
                onClick = {
                    isPlayingPreview = false
                    viewModel.commitScratchSpriteProjectToStudioAndVault()
                    onCommittedToStudio?.invoke()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("commit_scratch_sprite_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color(0xFF032014))
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Brand-New Sprite Sheet", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save Brand-New Sprite Sheet to Studio & Vault",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
