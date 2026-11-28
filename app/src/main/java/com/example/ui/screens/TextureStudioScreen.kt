package com.example.ui.screens

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.masterpack.TWENTY_COLOR_WHEEL
import com.example.model.texture.ProceduralMaterialPattern
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

@Composable
fun TextureStudioScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    openInCreateModeTrigger: Int = 0,
    onNavigateTo3D: (() -> Unit)? = null
) {
    val context = LocalContext.current
    // 0 = Presets & UV Lab, 1 = Create New Texture
    var studioMode by remember { mutableIntStateOf(0) }
    var showUvOverlay by remember { mutableStateOf(true) }

    // 3 customizable pattern generator colors (Primary, Secondary, Accent)
    var activePatternColorSlot by remember { mutableIntStateOf(0) } // 0=Primary, 1=Secondary, 2=Accent
    var patternPrimaryInt by remember { mutableIntStateOf(AndroidColor.rgb(0x16, 0x1B, 0x26)) }
    var patternSecondaryInt by remember { mutableIntStateOf(AndroidColor.rgb(0x33, 0x41, 0x55)) }
    var patternAccentInt by remember { mutableIntStateOf(AndroidColor.rgb(0x00, 0xE5, 0xFF)) }

    LaunchedEffect(openInCreateModeTrigger) {
        if (openInCreateModeTrigger > 0) {
            studioMode = 1
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.importCustomTexture(bitmap, "Imported Texture")
                    }
                }
            } catch (e: Exception) {
                Log.w("TextureStudioScreen", "Failed to decode picked texture image: ${e.message}")
                viewModel.postStatusMessage("Failed to import texture image: ${e.message}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 2-Option Segmented Mode Switcher: Create New Texture vs Presets & UV Lab
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = studioMode == 1,
                onClick = { studioMode = 1 },
                leadingIcon = {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("Create New Texture", fontFamily = TechMono, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen,
                    selectedLabelColor = Color(0xFF031B12),
                    selectedLeadingIconColor = Color(0xFF031B12),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("texture_studio_mode_create")
            )
            FilterChip(
                selected = studioMode == 0,
                onClick = { studioMode = 0 },
                leadingIcon = {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("Presets & UV Lab", fontFamily = TechMono, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanNeon,
                    selectedLabelColor = Color(0xFF031B26),
                    selectedLeadingIconColor = Color(0xFF031B26),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("texture_studio_mode_presets")
            )
        }

        if (studioMode == 1) {
            val texProject = uiState.scratchTextureProject

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1120)),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scratch_texture_creator_panel")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header & Blank Canvas Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FROM-SCRATCH TEXTURE & MATERIAL LAB",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "${texProject.sizePx}×${texProject.sizePx}px canvas • Exports 128×128 UV Map",
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.startNewBlankTextureProject(texProject.name, texProject.sizePx) },
                            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("scratch_texture_blank_reset_btn")
                        ) {
                            Icon(Icons.Default.LayersClear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Blank Canvas", fontFamily = TechMono, fontSize = 10.sp)
                        }
                    }

                    // Texture Name Input
                    OutlinedTextField(
                        value = texProject.name,
                        onValueChange = { viewModel.updateScratchTextureName(it) },
                        singleLine = true,
                        label = { Text("Texture / Material Name", fontFamily = TechMono, fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = EmeraldGreen,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scratch_texture_name_field")
                    )

                    // Procedural Material Generator Strip (3-Color + 5 Patterns)
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "PROCEDURAL MATERIAL GENERATOR (3-COLOR)",
                                    fontFamily = TechMono,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                            }

                            // 3 Pattern Color Slots
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val slots = listOf(
                                    Triple(0, "Primary", patternPrimaryInt),
                                    Triple(1, "Secondary", patternSecondaryInt),
                                    Triple(2, "Accent", patternAccentInt)
                                )
                                slots.forEach { (slotIdx, label, colInt) ->
                                    val isSlotActive = activePatternColorSlot == slotIdx
                                    Surface(
                                        color = if (isSlotActive) Color(0xFF1E293B) else Color(0xFF090D16),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isSlotActive) CyanNeon else CardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { activePatternColorSlot = slotIdx }
                                            .testTag("pattern_color_slot_$slotIdx")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colInt))
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                            Text(
                                                text = label,
                                                fontFamily = TechMono,
                                                fontSize = 9.sp,
                                                color = if (isSlotActive) CyanNeon else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Pattern Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProceduralMaterialPattern.entries
                                    .filter { it != ProceduralMaterialPattern.BLANK_CANVAS }
                                    .forEach { pattern ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                viewModel.generateScratchTexturePattern(
                                                    pattern = pattern,
                                                    primaryColorInt = patternPrimaryInt,
                                                    secondaryColorInt = patternSecondaryInt,
                                                    accentColorInt = patternAccentInt
                                                )
                                            },
                                            label = { Text(pattern.label, fontFamily = TechMono, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = Color(0xFF1E293B),
                                                labelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("gen_pattern_${pattern.name.lowercase()}")
                                        )
                                    }
                            }
                        }
                    }

                    // Interactive Touch-Paintable Square Pixel Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, CyanNeon.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B12))
                            .testTag("scratch_texture_pixel_canvas")
                            .pointerInput(texProject.sizePx) {
                                detectTapGestures { offset ->
                                    val dim = texProject.sizePx.coerceAtLeast(1)
                                    val px = ((offset.x / size.width) * dim).toInt().coerceIn(0, dim - 1)
                                    val py = ((offset.y / size.height) * dim).toInt().coerceIn(0, dim - 1)
                                    viewModel.paintScratchTextureAt(px, py)
                                }
                            }
                            .pointerInput(texProject.sizePx) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val dim = texProject.sizePx.coerceAtLeast(1)
                                    val px = ((change.position.x / size.width) * dim).toInt().coerceIn(0, dim - 1)
                                    val py = ((change.position.y / size.height) * dim).toInt().coerceIn(0, dim - 1)
                                    viewModel.paintScratchTextureAt(px, py)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dim = texProject.sizePx.coerceAtLeast(1)
                            val cellW = size.width / dim
                            val cellH = size.height / dim
                            val pixels = texProject.pixels

                            if (pixels.size >= dim * dim) {
                                for (y in 0 until dim) {
                                    val rowOff = y * dim
                                    for (x in 0 until dim) {
                                        drawRect(
                                            color = Color(pixels[rowOff + x]),
                                            topLeft = Offset(x * cellW, y * cellH),
                                            size = Size(cellW + 0.6f, cellH + 0.6f)
                                        )
                                    }
                                }
                            }

                            // Subtle pixel grid
                            val gridColor = Color(0x22FFFFFF)
                            for (i in 1 until dim) {
                                drawLine(gridColor, Offset(i * cellW, 0f), Offset(i * cellW, size.height), 0.7f)
                                drawLine(gridColor, Offset(0f, i * cellH), Offset(size.width, i * cellH), 0.7f)
                            }

                            // Mirror X / Mirror Y symmetry guidelines
                            if (texProject.mirrorX) {
                                drawLine(
                                    color = CyanNeon.copy(alpha = 0.85f),
                                    start = Offset(size.width / 2f, 0f),
                                    end = Offset(size.width / 2f, size.height),
                                    strokeWidth = 2.5f
                                )
                            }
                            if (texProject.mirrorY) {
                                drawLine(
                                    color = MagentaNeon.copy(alpha = 0.85f),
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f),
                                    strokeWidth = 2.5f
                                )
                            }
                        }
                    }

                    // Brush Size (1px / 2px / 3px) & Mirror X / Mirror Y Symmetry Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3).forEach { brushPx ->
                            FilterChip(
                                selected = uiState.activeTextureBrushSizePx == brushPx,
                                onClick = { viewModel.setTextureBrushSizePx(brushPx) },
                                label = { Text("${brushPx}px", fontFamily = TechMono, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Color(0xFF031B26),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.testTag("texture_brush_${brushPx}px")
                            )
                        }
                        FilterChip(
                            selected = texProject.mirrorX,
                            onClick = { viewModel.toggleScratchTextureMirrorX() },
                            label = { Text("Mirror X", fontFamily = TechMono, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("texture_mirror_x_chip")
                        )
                        FilterChip(
                            selected = texProject.mirrorY,
                            onClick = { viewModel.toggleScratchTextureMirrorY() },
                            label = { Text("Mirror Y", fontFamily = TechMono, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MagentaNeon,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("texture_mirror_y_chip")
                        )
                    }

                    // 20-Color Wheel Palette (sets active brush color AND updates selected pattern color slot)
                    Text(
                        text = "20-COLOR WHEEL (TAP TO SET BRUSH & ACTIVE PATTERN COLOR)",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TWENTY_COLOR_WHEEL.forEach { swatch ->
                            val isSelected = uiState.activeTextureBrushColorInt == swatch.colorInt
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch.colorInt))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else CardBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.setTextureBrushColorInt(swatch.colorInt)
                                        when (activePatternColorSlot) {
                                            0 -> patternPrimaryInt = swatch.colorInt
                                            1 -> patternSecondaryInt = swatch.colorInt
                                            else -> patternAccentInt = swatch.colorInt
                                        }
                                    }
                                    .testTag("texture_swatch_${swatch.id}")
                            )
                        }
                    }

                    // Emerald Primary CTA: Save & Apply Brand-New Texture to 3D
                    Button(
                        onClick = {
                            viewModel.commitScratchTextureTo3DStudio()
                            onNavigateTo3D?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color(0xFF031B12)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("scratch_texture_save_apply_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save & Apply Brand-New Texture to 3D",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Text(
                text = "3D MODEL TEXTURE LAB",
                fontFamily = TechMono,
                fontSize = 12.sp,
                color = CyanNeon,
                letterSpacing = 1.sp
            )

            // Texture Map Stage with UV Overlay
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val currentTex = uiState.activeTexture
                    if (currentTex != null) {
                        Box(modifier = Modifier.size(240.dp)) {
                            Image(
                                bitmap = currentTex.asImageBitmap(),
                                contentDescription = "Active 3D Texture Map",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                    .testTag("active_texture_display")
                            )

                            if (showUvOverlay) {
                                // Draw procedural UV wireframe overlay
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    drawLine(CyanNeon.copy(alpha = 0.5f), Offset(w * 0.1f, h * 0.1f), Offset(w * 0.5f, h * 0.9f), 1.5f)
                                    drawLine(CyanNeon.copy(alpha = 0.5f), Offset(w * 0.5f, h * 0.9f), Offset(w * 0.9f, h * 0.2f), 1.5f)
                                    drawLine(CyanNeon.copy(alpha = 0.5f), Offset(w * 0.9f, h * 0.2f), Offset(w * 0.1f, h * 0.1f), 1.5f)

                                    drawLine(Color(0x80FF2A85), Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.8f), 1.5f)
                                    drawLine(Color(0x80FF2A85), Offset(w * 0.8f, h * 0.8f), Offset(w * 0.5f, h * 0.3f), 1.5f)
                                    drawLine(Color(0x80FF2A85), Offset(w * 0.5f, h * 0.3f), Offset(w * 0.2f, h * 0.8f), 1.5f)
                                }
                            }
                        }
                    }

                    // Header Tag
                    Text(
                        text = "${uiState.activeTextureName} [UV Mapped]",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = CyanNeon,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(Color(0xCC0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Action: Import & Apply to Model
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_texture_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Texture", fontFamily = TechMono, fontSize = 12.sp)
                }

                Button(
                    onClick = { onNavigateTo3D?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("view_in_3d_btn")
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply & View 3D", fontFamily = TechMono, fontSize = 12.sp)
                }
            }

            // Texture Preset Library
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PROCEDURAL TEXTURE PRESETS",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.texturePresets.forEach { preset ->
                            val isSelected = uiState.activeTextureName == preset.name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.applyPresetTexture(preset) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) CyanNeon else CardBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Image(
                                        bitmap = preset.bitmap.asImageBitmap(),
                                        contentDescription = preset.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = CyanNeon,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = preset.name,
                                    fontFamily = TechMono,
                                    fontSize = 11.sp,
                                    color = if (isSelected) CyanNeon else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
