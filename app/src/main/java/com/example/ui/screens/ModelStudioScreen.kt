package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.viewer3d.Viewport3D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ModelStudioScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenHardwareLab: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showImportDialog by remember { mutableStateOf(false) }
    var rawObjInput by remember { mutableStateOf("") }
    var photoGridDensity by remember { mutableIntStateOf(28) }

    // 1. Unbounded 3D Model FILE Picker (.OBJ, .STL, .PLY, .GLTF, or any size file)
    val modelFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val result = UniversalAssetImporter.importModelFromUri(
                    context = context,
                    uri = uri,
                    sourceKind = AssetImportSourceKind.FILE,
                    photoGridResolution = photoGridDensity
                )
                if (result != null) {
                    viewModel.importModelFromResult(result)
                }
            }
        }
    }

    // 2. Unbounded 3D Model PHOTO Picker (Converts any resolution photo/image into a 3D mesh)
    val modelPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val result = UniversalAssetImporter.importModelFromUri(
                    context = context,
                    uri = uri,
                    sourceKind = AssetImportSourceKind.PHOTO,
                    photoGridResolution = photoGridDensity
                )
                if (result != null) {
                    viewModel.importModelFromResult(result)
                }
            }
        }
    }

    val meshReport = remember(uiState.currentMesh, uiState.selectedHardware) {
        HardwareAssetSimulator.analyzeMeshOnHardware(uiState.currentMesh, uiState.selectedHardware)
    }
    val meshDossier = remember(uiState.currentMesh, uiState.selectedHardware, meshReport) {
        HardwareReadoutGenerator.generate3DMeshDossier(uiState.currentMesh, uiState.selectedHardware, meshReport)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preset Model Switcher Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "3D MESH REPOSITORY",
                fontFamily = TechMono,
                fontSize = 12.sp,
                color = CyanNeon,
                letterSpacing = 1.sp
            )
            if (onOpenHardwareLab != null) {
                TextButton(
                    onClick = onOpenHardwareLab,
                    modifier = Modifier.testTag("open_hw_lab_from_3d")
                ) {
                    Icon(
                        Icons.Default.DeveloperBoard,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Target: ${uiState.selectedHardware.shortName}",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = AmberGold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "starfighter" to "Starfighter NX-3",
                "mech" to "Cyber Mech",
                "chest" to "Mimic Chest",
                "crystal" to "Aether Crystal"
            )
            presets.forEach { (key, title) ->
                val selected = uiState.currentMesh.name.contains(title, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.loadSampleMesh(key) },
                    label = { Text(title, fontFamily = TechMono, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeon,
                        selectedLabelColor = Color(0xFF031B26),
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.testTag("mesh_preset_$key")
                )
            }
        }

        // Main 3D Viewport
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            Viewport3D(
                mesh = uiState.currentMesh,
                activeTexture = uiState.activeTexture,
                hardwareSpec = uiState.selectedHardware,
                hardwareSimulationEnabled = uiState.hardwareSimulationEnabled,
                onToggleHardwareSimulation = { viewModel.toggleHardwareSimulation() },
                onExportObj = { viewModel.prepareObjExport() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Universal Unbounded Import Toolbar (File | Photo | Raw Text & Export)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNBOUNDED 3D IMPORT (FILES & PHOTOS • NO SIZE LIMIT)",
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
                    onClick = { modelFileLauncher.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = Color(0xFF031B26)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_model_file_btn")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = "Import 3D File", modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Import File", fontFamily = TechMono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        modelPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MagentaNeon,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_model_photo_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import 3D from Photo", modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("From Photo", fontFamily = TechMono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_obj_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import OBJ Options", modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Import OBJ / Options", fontFamily = TechMono, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.prepareObjExport() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_obj_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export OBJ", modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Export OBJ", fontFamily = TechMono, fontSize = 12.sp)
                }
            }
        }

        // Reusable AssetPreviewCard displaying Active 3D Model Preview, Metadata & Asset Type Tag
        val activeMesh = uiState.currentMesh
        val lastMeta = uiState.lastModelImportMeta
        AssetPreviewCard(
            title = activeMesh.name,
            assetTypeTag = "3D MODEL",
            metadataSummary = "${activeMesh.rawVertexCount} Vertices • ${activeMesh.rawFaceCount} Faces • ${activeMesh.sourceFormat}",
            subtitleOrCharacterTag = uiState.activeCharacterTagForImports.ifBlank { "Studio" },
            hardwareBadge = if (uiState.hardwareSimulationEnabled) uiState.selectedHardware.shortName else "Unconstrained",
            sourceBadge = lastMeta?.sourceKind?.badge ?: activeMesh.sourceKindLabel,
            metadataTags = listOf(
                lastMeta?.formattedSize ?: activeMesh.sourceSizeLabel,
                lastMeta?.readabilityStatus ?: "UNBOUNDED • 100% READABLE",
                "Target: ${uiState.selectedHardware.name}"
            ),
            meshPreview = activeMesh,
            bitmapPreview = uiState.activeTexture,
            testTagPrefix = "active_model_preview_card",
            primaryActionLabel = "Gemini AI Enhance & Generate Variants",
            onPrimaryAction = {
                viewModel.runGeminiEnhancementOrVariantGeneration()
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
                .testTag("fuse_3d_and_sprite_from_model_studio_btn")
        ) {
            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Fuse 3D Model × '${uiState.currentSpriteSheet.name.take(18)}' Spritesheet",
                fontFamily = TechMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Mesh Details & Telemetry Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MESH TELEMETRY & SPECIFICATION",
                        fontFamily = TechMono,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Icon(Icons.Default.ViewInAr, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Vertex Count", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("${uiState.currentMesh.vertexCount}", fontFamily = TechMono, fontSize = 16.sp, color = Color.White)
                    }
                    Column {
                        Text("Triangle Faces", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("${uiState.currentMesh.faceCount}", fontFamily = TechMono, fontSize = 16.sp, color = Color.White)
                    }
                    Column {
                        Text("Silicon Target", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(
                            text = if (uiState.hardwareSimulationEnabled) uiState.selectedHardware.shortName else "Unconstrained",
                            fontFamily = TechMono,
                            fontSize = 14.sp,
                            color = if (uiState.hardwareSimulationEnabled) AmberGold else CyanNeon
                        )
                    }
                }
            }
        }

        // Quick Console / Handheld Target Selector Strip
        Text(
            text = "SWITCH CONSOLE / CHIP OUTPUT TARGET:",
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
                val isSelected = hw.id == uiState.selectedHardware.id && uiState.hardwareSimulationEnabled
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
            },
            onRunDeepGeminiReview = { viewModel.runDeepGeminiImportFileReview() },
            onKeepAsSingleImage = { viewModel.keepImportedFileAsSingleImage() }
        )

        // Interactive Pre-Save 3D & Sprite Rotation Inspector + Personal Library
        AssetPreviewInspectorCard(
            mesh = uiState.currentMesh,
            spriteSheet = uiState.currentSpriteSheet,
            activeTexture = uiState.activeTexture,
            hardwareSpec = uiState.selectedHardware,
            hardwareSimulationEnabled = uiState.hardwareSimulationEnabled,
            defaultCharacterTag = uiState.activeCharacterTagForImports,
            defaultMode = InspectorMode.INSPECT_3D,
            defaultSaveType = LibraryAssetType.MODEL_3D,
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
            assetScope = "MODEL_3D",
            label = "Pin 3D Model to Gallery"
        )

        // Persistent Room Database Asset History Vault
        RoomAssetHistoryPanel(
            historyEntities = uiState.roomAssetHistory,
            onRestoreEntity = { viewModel.restoreAssetFromRoomHistory(it) },
            onDeleteEntity = { viewModel.deleteAssetFromRoomHistory(it) },
            onSaveCurrentSnapshotToHistory = { viewModel.saveCurrentStudioStateToRoomHistory() },
            onClearAllHistory = { viewModel.clearRoomAssetHistory() }
        )

        // Vivid Dual-Level Readout Dossier for Active 3D Mesh
        AssetHardwareReadoutPanel(
            dossier = meshDossier,
            readingLevel = uiState.readingLevelMode,
            onSelectReadingLevel = { viewModel.setReadingLevelMode(it) },
            onCopiedMessage = { viewModel.postStatusMessage(it) }
        )
    }

    // Universal Import 3D Model Dialog (Files, Photos, or Raw .OBJ / .STL / .PLY Text)
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    "Import 3D Model (File, Photo, or Raw Mesh)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanNeon
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Import any 3D model file (.OBJ, .STL, .PLY, .GLTF) or convert any photo/image into a 3D mesh with zero file-size or resolution limits:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showImportDialog = false
                                modelFileLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = Color(0xFF031B26)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_pick_3d_file_btn")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pick File", fontFamily = TechMono, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                showImportDialog = false
                                modelPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MagentaNeon,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_pick_3d_photo_btn")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pick Photo", fontFamily = TechMono, fontSize = 11.sp)
                        }
                    }

                    // Photo-to-3D Mesh Grid Density selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Photo 3D Grid:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        listOf(16, 28, 40, 56).forEach { grid ->
                            FilterChip(
                                selected = photoGridDensity == grid,
                                onClick = { photoGridDensity = grid },
                                label = { Text("${grid}×${grid}", fontFamily = TechMono, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MagentaNeon,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = rawObjInput,
                        onValueChange = { rawObjInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("obj_raw_input"),
                        placeholder = {
                            Text(
                                "v 0.0 1.0 0.0\nv -1.0 -1.0 1.0\nf 1 2 3...\n(Supports .OBJ, ASCII .STL, or .PLY of any size)",
                                fontFamily = TechMono,
                                fontSize = 11.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = TechMono, fontSize = 11.sp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                rawObjInput = """
                                    # Low-Poly Prism
                                    v 0.0 1.2 0.0
                                    v -1.0 -0.8 1.0
                                    v 1.0 -0.8 1.0
                                    v 1.0 -0.8 -1.0
                                    v -1.0 -0.8 -1.0
                                    f 1 2 3
                                    f 1 3 4
                                    f 1 4 5
                                    f 1 5 2
                                    f 5 4 3 2
                                """.trimIndent()
                            }
                        ) {
                            Text("Sample Prism (.OBJ)", fontFamily = TechMono, fontSize = 11.sp, color = CyanNeon)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawObjInput.isNotBlank()) {
                            viewModel.importObjText(rawObjInput, "Custom Import")
                        }
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                    modifier = Modifier.testTag("confirm_import_obj")
                ) {
                    Text("Parse & Load", fontFamily = TechMono)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}
