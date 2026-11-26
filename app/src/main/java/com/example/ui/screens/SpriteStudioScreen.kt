package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.HardwareReadoutGenerator
import com.example.model.importer.AssetImportSourceKind
import com.example.model.importer.UniversalAssetImporter
import com.example.model.library.LibraryAssetType
import com.example.model.sprite.SpriteAnimation
import com.example.ui.components.AiFileImportReviewCard
import com.example.ui.components.AssetHardwareReadoutPanel
import com.example.ui.components.AssetPreviewCard
import com.example.ui.components.AssetPreviewInspectorCard
import com.example.ui.components.InspectorMode
import com.example.ui.components.QuickAddToGalleryBar
import com.example.ui.components.RoomAssetHistoryPanel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SpriteStudioScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateTo3D: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentSheet = uiState.currentSpriteSheet

    var isPlaying by remember { mutableStateOf(true) }
    var currentAnimIndex by remember { mutableIntStateOf(0) }
    var currentFrameIndexInAnim by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(8) }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSourceKind by remember { mutableStateOf(AssetImportSourceKind.PHOTO) }
    var importCols by remember { mutableStateOf("4") }
    var importRows by remember { mutableStateOf("2") }

    val activeAnim = currentSheet.animations.getOrNull(currentAnimIndex)
        ?: currentSheet.animations.firstOrNull()
        ?: SpriteAnimation("All", (0 until currentSheet.frames.size).toList())

    val firstSpriteFrame = currentSheet.frames.firstOrNull()
    val spriteReport = remember(firstSpriteFrame, uiState.selectedHardware, uiState.showHardwareTileGrid) {
        firstSpriteFrame?.let {
            HardwareAssetSimulator.simulateSpriteOnHardware(it, uiState.selectedHardware, uiState.showHardwareTileGrid)
        }
    }
    val spriteDossier = remember(currentSheet, uiState.selectedHardware, spriteReport) {
        spriteReport?.let {
            HardwareReadoutGenerator.generate2DSpriteDossier(currentSheet, uiState.selectedHardware, it)
        }
    }

    // Animation frame timer loop
    LaunchedEffect(isPlaying, fps, activeAnim) {
        if (!isPlaying || activeAnim.frameIndices.isEmpty()) return@LaunchedEffect
        while (true) {
            val delayMs = (1000L / fps.coerceAtLeast(1)).coerceAtLeast(16L)
            delay(delayMs)
            currentFrameIndexInAnim = (currentFrameIndexInAnim + 1) % activeAnim.frameIndices.size
        }
    }

    // 1. Unbounded Spritesheet PHOTO Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingSourceKind = AssetImportSourceKind.PHOTO
            pendingImportUri = uri
        }
    }

    // 2. Unbounded Spritesheet FILE Picker (Any image file, high-res atlas, or 3D file -> turntable sheet)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingSourceKind = AssetImportSourceKind.FILE
            pendingImportUri = uri
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preset Chips
        Text(
            text = "SPRITESHEET CATALOG",
            fontFamily = TechMono,
            fontSize = 12.sp,
            color = CyanNeon,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf("ninja" to "Cyber Ninja", "slime" to "Dungeon Slime")
            presets.forEach { (key, title) ->
                val selected = currentSheet.name.contains(title, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = {
                        viewModel.loadSampleSprite(key)
                        currentAnimIndex = 0
                        currentFrameIndexInAnim = 0
                    },
                    label = { Text(title, fontFamily = TechMono, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MagentaNeon,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.testTag("sprite_preset_$key")
                )
            }
        }

        // Animation Player Preview Stage
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentActualFrameIdx = activeAnim.frameIndices.getOrNull(currentFrameIndexInAnim) ?: 0
                val frameBmp = currentSheet.frames.getOrNull(currentActualFrameIdx)

                if (frameBmp != null) {
                    Image(
                        bitmap = frameBmp.asImageBitmap(),
                        contentDescription = "Active Animation Frame",
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.None, // Crisp Pixel Art
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(160.dp)
                            .testTag("animated_sprite_display")
                    )
                }

                // Info badge
                Surface(
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "${activeAnim.name} [Frame ${currentFrameIndexInAnim + 1}/${activeAnim.frameIndices.size}]",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = CyanNeon,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Playback Controls Row
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Animation track selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentSheet.animations.forEachIndexed { idx, anim ->
                        val isSelected = currentAnimIndex == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentAnimIndex = idx
                                currentFrameIndexInAnim = 0
                            },
                            label = { Text(anim.name, fontFamily = TechMono, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("anim_track_$idx")
                        )
                    }
                }

                // Play / Pause & FPS slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(44.dp)
                            .background(CyanNeon, CircleShape)
                            .testTag("toggle_play_sprite")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF031B26),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Speed: $fps FPS", fontFamily = TechMono, fontSize = 12.sp, color = Color.White)
                        }
                        Slider(
                            value = fps.toFloat(),
                            onValueChange = { fps = it.toInt() },
                            valueRange = 1f..24f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.testTag("fps_slider")
                        )
                    }
                }
            }
        }

        // Unbounded Import Toolbar: Import Spritesheet from Photo | Import from File | Extrude to 3D
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNBOUNDED SPRITESHEET IMPORT (PHOTOS & FILES • NO SIZE LIMIT)",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "100% SIZE READABILITY",
                        fontFamily = TechMono,
                        fontSize = 8.sp,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_spritesheet_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(17.dp), tint = CyanNeon)
                    Spacer(Modifier.width(5.dp))
                    Text("Import Photo", fontFamily = TechMono, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_spritesheet_file_btn")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(17.dp), tint = CyanNeon)
                    Spacer(Modifier.width(5.dp))
                    Text("Import File", fontFamily = TechMono, fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    viewModel.convertSpriteTo3D()
                    onNavigateTo3D?.invoke()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MagentaNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("extrude_sprite_3d_btn")
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Extrude Active Sprite to 3D Mesh", fontFamily = TechMono, fontSize = 13.sp)
            }
        }

        // Reusable AssetPreviewCard displaying Active 2D Spritesheet Preview, Metadata & Asset Type Tag
        val lastSpriteMeta = uiState.lastSpriteImportMeta
        AssetPreviewCard(
            title = currentSheet.name,
            assetTypeTag = "2D SPRITESHEET",
            metadataSummary = "${currentSheet.frames.size} Frames (${currentSheet.columns}×${currentSheet.rows}) • Frame: ${currentSheet.frameWidth}×${currentSheet.frameHeight}px • Atlas: ${currentSheet.originalWidth}×${currentSheet.originalHeight}px",
            subtitleOrCharacterTag = uiState.activeCharacterTagForImports.ifBlank { "Studio" },
            hardwareBadge = "${uiState.selectedHardware.shortName} (${uiState.selectedHardware.bitsPerPixelSprite}bpp)",
            sourceBadge = lastSpriteMeta?.sourceKind?.badge ?: currentSheet.sourceKindLabel,
            metadataTags = listOf(
                lastSpriteMeta?.formattedSize ?: currentSheet.sourceSizeLabel,
                lastSpriteMeta?.readabilityStatus ?: "UNBOUNDED • 100% READABLE",
                "${currentSheet.animations.size} Anim Tracks"
            ),
            bitmapPreview = firstSpriteFrame,
            testTagPrefix = "active_sprite_preview_card",
            primaryActionLabel = "Gemini AI Enhance & Generate Variants",
            onPrimaryAction = {
                viewModel.runGeminiEnhancementOrVariantGeneration(
                    sourceOverride = com.example.ai.GeminiInputImageSource.ACTIVE_SPRITESHEET
                )
            }
        )

        OutlinedButton(
            onClick = { viewModel.fuseModelAndSpriteSheetToMakeAsset() },
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("fuse_sprite_and_3d_from_sprite_studio_btn")
        ) {
            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Fuse Spritesheet × '${uiState.currentMesh.name.take(18)}' 3D Model",
                fontFamily = TechMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Sliced Frame Timeline Strip
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "FRAME ATLAS STRIP (${currentSheet.frames.size} FRAMES)",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(currentSheet.frames) { idx, frame ->
                        val isCurrent = activeAnim.frameIndices.getOrNull(currentFrameIndexInAnim) == idx
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070B12))
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) CyanNeon else CardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val animIdx = activeAnim.frameIndices.indexOf(idx)
                                    if (animIdx >= 0) {
                                        currentFrameIndexInAnim = animIdx
                                    }
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = frame.asImageBitmap(),
                                contentDescription = "Frame $idx",
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Quick Console / Handheld Target Selector Strip
        Text(
            text = "SWITCH CONSOLE / CHIP SPRITE OUTPUT TARGET:",
            fontFamily = TechMono,
            fontSize = 11.sp,
            color = AmberGold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.hardwareCatalog.forEach { hw ->
                val isSelected = hw.id == uiState.selectedHardware.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectHardware(hw) },
                    label = { Text(hw.shortName, fontFamily = TechMono, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberGold,
                        selectedLabelColor = Color(0xFF031B26),
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color.White
                    )
                )
            }
        }

        // AI File Import Spritesheet-in-Single-Picture Reviewer Card
        AiFileImportReviewCard(
            reviewResult = uiState.lastAiFileImportReview,
            isReviewing = uiState.isReviewingImportedFileWithAi,
            readingLevelMode = uiState.readingLevelMode,
            onApplyRecommendedGrid = { review ->
                viewModel.applyAiRecommendedSpriteSheetGrid(review)
                currentAnimIndex = 0
                currentFrameIndexInAnim = 0
            },
            onRunDeepGeminiReview = { viewModel.runDeepGeminiImportFileReview() },
            onKeepAsSingleImage = { viewModel.keepImportedFileAsSingleImage() }
        )

        // Interactive Pre-Save 2D Sprite & 3D Rotation Inspector + Personal Library
        AssetPreviewInspectorCard(
            mesh = uiState.currentMesh,
            spriteSheet = uiState.currentSpriteSheet,
            activeTexture = uiState.activeTexture,
            hardwareSpec = uiState.selectedHardware,
            hardwareSimulationEnabled = uiState.hardwareSimulationEnabled,
            defaultCharacterTag = uiState.activeCharacterTagForImports,
            defaultMode = InspectorMode.INSPECT_SPRITE,
            defaultSaveType = LibraryAssetType.SPRITE_2D,
            savedLibraryAssets = uiState.savedLibraryAssets,
            onToggleHardwareSimulation = { viewModel.toggleHardwareSimulation() },
            onSaveToLibrary = { title, charTag, type, yaw, pitch, spriteRot ->
                viewModel.saveInspectedAssetToLibrary(title, charTag, type, yaw, pitch, spriteRot)
            },
            onLoadFromLibrary = { viewModel.loadLibraryAssetIntoStudio(it) },
            onDeleteFromLibrary = { viewModel.deleteLibraryAsset(it) },
            onActivateFusedHardware = { viewModel.activateFusedHardwareSpec(it) }
        )

        QuickAddToGalleryBar(
            uiState = uiState,
            viewModel = viewModel,
            assetScope = "SPRITE_2D",
            label = "Pin Spritesheet to Gallery"
        )

        // Persistent Room Database Asset History Vault
        RoomAssetHistoryPanel(
            historyEntities = uiState.roomAssetHistory,
            onRestoreEntity = { viewModel.restoreAssetFromRoomHistory(it) },
            onDeleteEntity = { viewModel.deleteAssetFromRoomHistory(it) },
            onSaveCurrentSnapshotToHistory = { viewModel.saveCurrentStudioStateToRoomHistory() },
            onClearAllHistory = { viewModel.clearRoomAssetHistory() }
        )

        // Vivid Dual-Level Readout Dossier for Active 2D Sprite Sheet
        if (spriteDossier != null) {
            AssetHardwareReadoutPanel(
                dossier = spriteDossier,
                readingLevel = uiState.readingLevelMode,
                onSelectReadingLevel = { viewModel.setReadingLevelMode(it) },
                onCopiedMessage = { viewModel.postStatusMessage(it) }
            )
        }
    }

    // Unbounded Import & Frame Slicing Dialog
    if (pendingImportUri != null) {
        val uri = pendingImportUri!!
        val (queriedName, queriedBytes, _) = remember(uri) {
            UniversalAssetImporter.queryUriInfo(context, uri)
        }
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = {
                Text(
                    "Slice Imported ${pendingSourceKind.badge} Spritesheet",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanNeon
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF070B12),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${pendingSourceKind.label} • $queriedName",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${UniversalAssetImporter.formatBytes(queriedBytes)} • UNBOUNDED 100% SIZE READABILITY",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = EmeraldGreen
                            )
                        }
                    }

                    Text(
                        "Specify grid columns & rows for automatic frame slicing (supports any image resolution or 3D file):",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    // Quick Grid Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "1×1" to ("1" to "1"),
                            "2×2" to ("2" to "2"),
                            "4×1" to ("4" to "1"),
                            "4×2" to ("4" to "2"),
                            "4×4" to ("4" to "4"),
                            "8×4" to ("8" to "4")
                        ).forEach { (label, pair) ->
                            val selected = importCols == pair.first && importRows == pair.second
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    importCols = pair.first
                                    importRows = pair.second
                                },
                                label = { Text(label, fontFamily = TechMono, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MagentaNeon,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = importCols,
                            onValueChange = { importCols = it },
                            label = { Text("Columns", fontFamily = TechMono, fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("slice_cols_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = importRows,
                            onValueChange = { importRows = it },
                            label = { Text("Rows", fontFamily = TechMono, fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("slice_rows_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070B12))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strip Solid / Magenta BG",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Flood-fills opaque magenta/green/solid corners to transparent (preserves transparent PNGs)",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = uiState.stripSolidBgOnImport,
                            onCheckedChange = { viewModel.setStripSolidBgOnImport(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF031B26),
                                checkedTrackColor = CyanNeon
                            ),
                            modifier = Modifier.testTag("strip_chroma_bg_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUri = pendingImportUri
                        val sourceKind = pendingSourceKind
                        val cols = importCols.toIntOrNull()?.coerceAtLeast(1) ?: 4
                        val rows = importRows.toIntOrNull()?.coerceAtLeast(1) ?: 2
                        val stripBg = uiState.stripSolidBgOnImport
                        pendingImportUri = null
                        if (targetUri != null) {
                            scope.launch(Dispatchers.IO) {
                                val result = UniversalAssetImporter.importSpriteSheetFromUri(
                                    context = context,
                                    uri = targetUri,
                                    sourceKind = sourceKind,
                                    columns = cols,
                                    rows = rows,
                                    stripChromaKeyBg = stripBg
                                )
                                if (result != null) {
                                    viewModel.importSpriteSheetFromResult(result)
                                    currentAnimIndex = 0
                                    currentFrameIndexInAnim = 0
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                    modifier = Modifier.testTag("confirm_slice_btn")
                ) {
                    Text("Slice & Load", fontFamily = TechMono)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}
