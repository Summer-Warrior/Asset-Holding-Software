package com.example.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.hardware.ReadingLevelMode
import com.example.model.importer.AssetImportSourceKind
import com.example.model.importer.UniversalAssetImporter
import com.example.model.library.LibraryAssetType
import com.example.model.suite.AssetSuiteSynthesizer
import com.example.model.suite.CharacterAssetModality
import com.example.model.suite.NamedCharacterFusionGroup
import com.example.model.suite.SuiteMeshStrategy
import com.example.model.suite.SuiteSpriteStrategy
import com.example.model.suite.SuiteTextureStrategy
import com.example.ui.components.AiFileImportReviewCard
import com.example.ui.components.AssetGalleriesPanel
import com.example.ui.components.AssetPreviewCard
import com.example.ui.components.AssetPreviewInspectorCard
import com.example.ui.components.AudioAndVaultStudioPanel
import com.example.ui.components.GameGroupCharacterBrowserPanel
import com.example.ui.components.InspectorMode
import com.example.ui.components.ModelSpriteFusionPanel
import com.example.ui.components.RoomAssetHistoryPanel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import com.example.ui.viewer3d.Viewport3D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AssetSuiteScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = uiState.suiteBuildConfig
    val activeSuite = uiState.activeAssetSuite

    var showObjImportDialog by remember { mutableStateOf(false) }
    var rawObjInput by remember { mutableStateOf("") }
    var customObjName by remember { mutableStateOf("${uiState.activeCharacterTagForImports}_Attachment.obj") }
    var spriteImportCols by remember { mutableIntStateOf(4) }
    var spriteImportRows by remember { mutableIntStateOf(1) }
    var previewTab by remember { mutableIntStateOf(0) }
    var animFrameIndex by remember { mutableIntStateOf(0) }

    // Launcher 1A: Import Spritesheet PHOTO (Unbounded size readability, tagged with active character)
    val spritePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val charTag = uiState.activeCharacterTagForImports.ifBlank { "Character" }
            val cols = spriteImportCols
            val rows = spriteImportRows
            val stripBg = uiState.stripSolidBgOnImport
            scope.launch(Dispatchers.IO) {
                val result = UniversalAssetImporter.importSpriteSheetFromUri(
                    context = context,
                    uri = uri,
                    sourceKind = AssetImportSourceKind.PHOTO,
                    columns = cols,
                    rows = rows,
                    customName = "$charTag Imported Sheet",
                    stripChromaKeyBg = stripBg
                )
                if (result != null) {
                    viewModel.importSpriteSheetFromResult(result, characterTagOverride = charTag)
                }
            }
        }
    }

    // Launcher 1B: Import Spritesheet FILE (Unbounded size readability, handles any image or 3D file)
    val spriteFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val charTag = uiState.activeCharacterTagForImports.ifBlank { "Character" }
            val cols = spriteImportCols
            val rows = spriteImportRows
            val stripBg = uiState.stripSolidBgOnImport
            scope.launch(Dispatchers.IO) {
                val result = UniversalAssetImporter.importSpriteSheetFromUri(
                    context = context,
                    uri = uri,
                    sourceKind = AssetImportSourceKind.FILE,
                    columns = cols,
                    rows = rows,
                    customName = "$charTag Imported Sheet",
                    stripChromaKeyBg = stripBg
                )
                if (result != null) {
                    viewModel.importSpriteSheetFromResult(result, characterTagOverride = charTag)
                }
            }
        }
    }

    // Launcher 2: Import 3D Texture Map Image (Unbounded size readability, tagged with active character)
    val texturePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val charTag = uiState.activeCharacterTagForImports.ifBlank { "Character" }
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val decoded = UniversalAssetImporter.decodeBitmapUnbounded(stream)
                        if (decoded != null) {
                            viewModel.importCustomTexture(
                                bitmap = decoded.bitmap,
                                name = "$charTag UV Texture (${decoded.originalWidth}×${decoded.originalHeight}px)",
                                characterTagOverride = charTag
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    // Launcher 3: Import Concept Art Image (Unbounded size readability, tagged with active character)
    val conceptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val charTag = uiState.activeCharacterTagForImports.ifBlank { "Character" }
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val decoded = UniversalAssetImporter.decodeBitmapUnbounded(stream)
                        if (decoded != null) {
                            viewModel.importConceptArtBitmap(
                                bitmap = decoded.bitmap,
                                name = "$charTag Concept Art (${decoded.originalWidth}×${decoded.originalHeight}px)",
                                characterTagOverride = charTag
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    // Launcher 4A: Import 3D Model FILE (.OBJ, .STL, .PLY, .GLTF, or any size file)
    val objFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val charTag = uiState.activeCharacterTagForImports.ifBlank { "Character" }
            scope.launch(Dispatchers.IO) {
                val result = UniversalAssetImporter.importModelFromUri(
                    context = context,
                    uri = uri,
                    sourceKind = AssetImportSourceKind.FILE,
                    customName = "$charTag Imported 3D"
                )
                if (result != null) {
                    viewModel.importModelFromResult(result, characterTagOverride = charTag)
                }
            }
        }
    }

    // Launcher 4B: Import 3D Model from PHOTO (Unbounded photo-to-3D relief mesh)
    val objPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val charTag = uiState.activeCharacterTagForImports.ifBlank { "Character" }
            scope.launch(Dispatchers.IO) {
                val result = UniversalAssetImporter.importModelFromUri(
                    context = context,
                    uri = uri,
                    sourceKind = AssetImportSourceKind.PHOTO,
                    customName = "$charTag Photo 3D"
                )
                if (result != null) {
                    viewModel.importModelFromResult(result, characterTagOverride = charTag)
                }
            }
        }
    }

    // Animate the active suite's 2D spritesheet preview
    val suiteFrames = activeSuite?.spriteSheet?.frames ?: uiState.currentSpriteSheet.frames
    LaunchedEffect(suiteFrames.size) {
        animFrameIndex = 0
        while (suiteFrames.size > 1) {
            delay(160L)
            animFrameIndex = (animFrameIndex + 1) % suiteFrames.size
        }
    }

    val suiteAllocation = remember(activeSuite, uiState.selectedHardware) {
        activeSuite?.let { AssetSuiteSynthesizer.analyzeSuiteOnHardware(it, uiState.selectedHardware) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A111E)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SINGLE ASSET SUITE & CHARACTER AUTO-FUSION",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${activeSuite?.cohesionScore ?: 98}% COHESION",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Automatic Fusion of Assets Sharing Named Characters",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Whenever multiple imported or staged assets (3D Models, 2D Spritesheets, UV Textures, Concept Art, or Lore Specs) share a Named Character, PixelForge automatically detects their shared identity and fuses their 3D meshes, sprite animation banks, UV decals, and palettes into one unified Character Suite.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Dedicated 3D Model × 2D Spritesheet Hybrid Asset Fusion Lab
        ModelSpriteFusionPanel(
            uiState = uiState,
            viewModel = viewModel
        )

        // Section 0: Named-Character Automatic Asset Fusion Engine
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1526)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MagentaNeon.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("named_character_auto_fusion_card")
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.MergeType,
                            contentDescription = null,
                            tint = MagentaNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "NAMED-CHARACTER AUTO-FUSION ENGINE",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaNeon
                            )
                            Text(
                                text = "${uiState.characterFusionGroups.count { it.hasMultipleAssetsToFuse }} Named Characters with multi-asset fusion links",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Switch(
                        checked = uiState.autoFuseNamedCharacters,
                        onCheckedChange = { viewModel.setAutoFuseNamedCharacters(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MagentaNeon
                        ),
                        modifier = Modifier.testTag("auto_fuse_named_characters_switch")
                    )
                }

                // Live Auto-Fusion Event Banner
                if (!uiState.lastAutoFusionBanner.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFF190D24),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MagentaNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = uiState.lastAutoFusionBanner,
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                color = Color(0xFFF5D0FE)
                            )
                        }
                    }
                }

                // Active Character Identity Tag for New Imports & Quick-Staging
                OutlinedTextField(
                    value = uiState.activeCharacterTagForImports,
                    onValueChange = { viewModel.setActiveCharacterTagForImports(it) },
                    label = {
                        Text(
                            "Named Character Tag for Imports & Auto-Fusion",
                            fontFamily = TechMono,
                            fontSize = 10.sp
                        )
                    },
                    placeholder = { Text("e.g., Kaelen, Valkyrie-09, Samus, Zero...", fontSize = 11.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CyanNeon)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_tag_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MagentaNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = MagentaNeon,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                // Quick Character Tag Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Characters:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                    val presetChars = (uiState.characterFusionGroups.map { it.characterName } + listOf("Kaelen", "Valkyrie-09", "Gloom Slime", "Starfighter NX-3")).distinct()
                    presetChars.forEach { charName ->
                        val selected = uiState.activeCharacterTagForImports.equals(charName, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setActiveCharacterTagForImports(charName) },
                            label = { Text(charName, fontFamily = TechMono, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MagentaNeon,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("select_char_tag_${charName.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // Quick-Bind Active Studio Assets to Selected Named Character (Triggers Instant Auto-Fusion!)
                Text(
                    text = "BIND ACTIVE STUDIO ASSET TO '${uiState.activeCharacterTagForImports.ifBlank { "CHARACTER" }}' & AUTO-FUSE:",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    color = CyanNeon
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val targetChar = uiState.activeCharacterTagForImports.ifBlank { "Kaelen" }
                    listOf(
                        CharacterAssetModality.MESH_3D to "+ Fuse Active 3D Mesh",
                        CharacterAssetModality.SPRITESHEET_2D to "+ Fuse Active Sprite",
                        CharacterAssetModality.TEXTURE_UV to "+ Fuse Active Texture",
                        CharacterAssetModality.LORE_SPEC to "+ Fuse Active Lore"
                    ).forEach { (modality, btnLabel) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.stageActiveStudioAssetForCharacter(targetChar, modality)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                            modifier = Modifier.testTag("stage_${modality.name.lowercase()}_btn")
                        ) {
                            Text(btnLabel, fontFamily = TechMono, fontSize = 10.sp)
                        }
                    }
                }

                HorizontalDivider(color = CardBorder)

                // Detected Named Character Clusters in Staged Pool
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DETECTED NAMED CHARACTER CLUSTERS (${uiState.characterFusionGroups.size})",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                    TextButton(
                        onClick = { viewModel.fuseAllDetectedNamedCharacters() },
                        modifier = Modifier.testTag("batch_auto_fuse_all_btn")
                    ) {
                        Icon(Icons.Default.GroupWork, contentDescription = null, tint = MagentaNeon, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Auto-Fuse All Characters", fontFamily = TechMono, fontSize = 10.sp, color = MagentaNeon)
                    }
                }

                uiState.characterFusionGroups.forEach { group ->
                    CharacterClusterCard(
                        group = group,
                        isActiveInSuite = activeSuite?.name?.startsWith(group.characterName, ignoreCase = true) == true,
                        onFuseGroup = { viewModel.fuseNamedCharacterByName(group.characterName) }
                    )
                }
            }
        }

        // Section 1: 5-Slot Imported Asset Intake Board
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. IMPORT ASSETS FOR '${uiState.activeCharacterTagForImports.ifBlank { "NAMED CHARACTER" }}' (AUTO-FUSES ON IMPORT)",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )

                // Slot A: 2D Spritesheet Import Slot (Photos & Files • Unbounded Readability)
                IntakeSlotCard(
                    slotNumber = "01",
                    title = "2D Spritesheet (File or Photo)",
                    statusText = "${uiState.currentSpriteSheet.name} • ${uiState.currentSpriteSheet.frames.size} frames (${uiState.currentSpriteSheet.originalWidth}×${uiState.currentSpriteSheet.originalHeight}px • ${uiState.currentSpriteSheet.columns}×${uiState.currentSpriteSheet.rows})",
                    isUserImported = uiState.hasImportedSprite,
                    accentColor = MagentaNeon,
                    previewBitmap = uiState.currentSpriteSheet.frames.firstOrNull(),
                    primaryActionLabel = "Import Photo",
                    primaryActionTag = "suite_import_spritesheet_btn",
                    onPrimaryAction = {
                        spritePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    secondaryActionLabel = "Import File",
                    secondaryActionTag = "suite_import_spritesheet_file_btn",
                    onSecondaryAction = {
                        spriteFileLauncher.launch(arrayOf("*/*"))
                    },
                    extraControls = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grid:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                listOf(1 to 1, 4 to 1, 4 to 2, 4 to 3, 4 to 4).forEach { (c, r) ->
                                    val selected = spriteImportCols == c && spriteImportRows == r
                                    Surface(
                                        color = if (selected) MagentaNeon else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.clickable {
                                            spriteImportCols = c
                                            spriteImportRows = r
                                        }
                                    ) {
                                        Text(
                                            text = "${c}×${r}",
                                            fontFamily = TechMono,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Strip Solid / Magenta BG",
                                    fontFamily = TechMono,
                                    fontSize = 10.sp,
                                    color = CyanNeon
                                )
                                Switch(
                                    checked = uiState.stripSolidBgOnImport,
                                    onCheckedChange = { viewModel.setStripSolidBgOnImport(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF031B26),
                                        checkedTrackColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("suite_strip_chroma_bg_switch")
                                )
                            }
                        }
                    }
                )

                // Slot B: 3D Model Import Slot (Files & Photos • Unbounded Readability)
                IntakeSlotCard(
                    slotNumber = "02",
                    title = "3D Model Mesh (File or Photo)",
                    statusText = "${uiState.currentMesh.name} • ${uiState.currentMesh.rawVertexCount} verts, ${uiState.currentMesh.rawFaceCount} faces (${uiState.currentMesh.sourceFormat})",
                    isUserImported = uiState.hasImportedMesh,
                    accentColor = CyanNeon,
                    previewBitmap = null,
                    primaryActionLabel = "Import File",
                    primaryActionTag = "suite_import_obj_file_btn",
                    onPrimaryAction = { objFileLauncher.launch(arrayOf("*/*")) },
                    secondaryActionLabel = "Photo / Paste",
                    secondaryActionTag = "suite_paste_obj_btn",
                    onSecondaryAction = {
                        customObjName = "${uiState.activeCharacterTagForImports.ifBlank { "Kaelen" }}_ArmorPart.obj"
                        showObjImportDialog = true
                    },
                    extraControls = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Photo → 3D:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Surface(
                                color = CyanNeon.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable {
                                        objPhotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("suite_import_model_photo_btn")
                            ) {
                                Text(
                                    text = "Pick Photo as 3D Model (No Size Limit)",
                                    fontFamily = TechMono,
                                    fontSize = 9.sp,
                                    color = CyanNeon,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                )

                // Slot C: 3D Model Texture Import Slot
                IntakeSlotCard(
                    slotNumber = "03",
                    title = "3D Model UV Texture",
                    statusText = "${uiState.activeTextureName} • ${uiState.activeTexture?.width ?: 128}×${uiState.activeTexture?.height ?: 128}px",
                    isUserImported = uiState.hasImportedTexture,
                    accentColor = EmeraldGreen,
                    previewBitmap = uiState.activeTexture,
                    primaryActionLabel = "Import Texture",
                    primaryActionTag = "suite_import_texture_btn",
                    onPrimaryAction = {
                        texturePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                // Slot D: Concept Art Import Slot
                IntakeSlotCard(
                    slotNumber = "04",
                    title = "Concept Art Reference",
                    statusText = if (uiState.conceptArtBitmap != null) {
                        "Loaded (${uiState.conceptArtBitmap.width}×${uiState.conceptArtBitmap.height}px) • ${uiState.extractedPalette.size} dominant colors"
                    } else {
                        "Auto-synthesized from Character Sprite & 3D Silhouette (or Import Image)"
                    },
                    isUserImported = uiState.hasImportedConcept,
                    accentColor = AmberGold,
                    previewBitmap = uiState.conceptArtBitmap ?: activeSuite?.conceptArtBitmap,
                    primaryActionLabel = "Import Concept",
                    primaryActionTag = "suite_import_concept_btn",
                    onPrimaryAction = {
                        conceptPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                // Slot E: Suite Identity & Text Description Spec
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF080D18))
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[05] SUITE IDENTITY & TEXT SPECIFICATION",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                        Surface(
                            color = if (uiState.hasEditedPrompt) EmeraldGreen.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (uiState.hasEditedPrompt) "CUSTOM SPEC" else "STUDIO SPEC",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = if (uiState.hasEditedPrompt) EmeraldGreen else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = config.suiteName,
                        onValueChange = { name -> viewModel.updateSuiteBuildConfig { it.copy(suiteName = name) } },
                        label = { Text("Single Asset Suite Name", fontFamily = TechMono, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suite_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyanNeon,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssetSuiteSynthesizer.entityCategories.forEach { cat ->
                            FilterChip(
                                selected = config.entityCategory == cat,
                                onClick = { viewModel.updateSuiteBuildConfig { it.copy(entityCategory = cat) } },
                                label = { Text(cat, fontFamily = TechMono, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Color(0xFF031B26),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.promptText,
                        onValueChange = { viewModel.updatePromptText(it) },
                        label = { Text("Text Description & Lore Specification", fontFamily = TechMono, fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .testTag("suite_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyanNeon,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        // Section 2: Cross-Asset Synthesis & Harmonization Rules
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. CROSS-ASSET SYNTHESIS & HARMONIZATION RULES",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )

                // 3D Model Derivation Strategy
                Text("3D Model Pipeline:", fontFamily = TechMono, fontSize = 11.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuiteMeshStrategy.entries.forEach { strategy ->
                        FilterChip(
                            selected = config.meshStrategy == strategy,
                            onClick = { viewModel.updateSuiteBuildConfig { it.copy(meshStrategy = strategy) } },
                            label = { Text(strategy.shortLabel, fontFamily = TechMono, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("mesh_strategy_${strategy.name.lowercase()}")
                        )
                    }
                }

                // 2D Spritesheet Derivation Strategy
                Text("2D Spritesheet Pipeline:", fontFamily = TechMono, fontSize = 11.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuiteSpriteStrategy.entries.forEach { strategy ->
                        FilterChip(
                            selected = config.spriteStrategy == strategy,
                            onClick = { viewModel.updateSuiteBuildConfig { it.copy(spriteStrategy = strategy) } },
                            label = { Text(strategy.shortLabel, fontFamily = TechMono, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MagentaNeon,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("sprite_strategy_${strategy.name.lowercase()}")
                        )
                    }
                }

                // 3D Texture Derivation Strategy
                Text("3D UV Texture Pipeline:", fontFamily = TechMono, fontSize = 11.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuiteTextureStrategy.entries.forEach { strategy ->
                        FilterChip(
                            selected = config.textureStrategy == strategy,
                            onClick = { viewModel.updateSuiteBuildConfig { it.copy(textureStrategy = strategy) } },
                            label = { Text(strategy.shortLabel, fontFamily = TechMono, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("texture_strategy_${strategy.name.lowercase()}")
                        )
                    }
                }

                // Master Palette Lock Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF080D18))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock Unified Master Palette & Auto-UVs",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Harmonizes 3D Mesh faces, 2D Sprite frames, and 3D UV Texture to share one console CLUT palette",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = config.unifyPaletteAcrossAssets,
                        onCheckedChange = { enabled ->
                            viewModel.updateSuiteBuildConfig { it.copy(unifyPaletteAcrossAssets = enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF031B26),
                            checkedTrackColor = CyanNeon
                        ),
                        modifier = Modifier.testTag("suite_unify_palette_switch")
                    )
                }

                // Primary Compile Single Asset Suite Button
                Button(
                    onClick = { viewModel.compileSingleAssetSuite(syncToAllStudios = true) },
                    enabled = !uiState.isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = Color(0xFF031B26)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("compile_asset_suite_btn")
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            color = Color(0xFF031B26),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Compiling Single Asset Suite...", fontFamily = TechMono, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Compile Single Asset Suite & Sync Studios",
                            fontFamily = TechMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Saved Suites Switcher (if multiple suites compiled)
        if (uiState.savedAssetSuites.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FUSED & COMPILED SUITES:",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                uiState.savedAssetSuites.forEach { suite ->
                    val isCurrent = activeSuite?.id == suite.id
                    FilterChip(
                        selected = isCurrent,
                        onClick = { viewModel.selectSavedSuite(suite) },
                        label = {
                            Text(
                                text = "${suite.name} (${suite.cohesionScore}%)",
                                fontFamily = TechMono,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color(0xFF031B26),
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }

        // Pre-Save 3D Model & Sprite Rotation Inspector + Personal Library
        AssetPreviewInspectorCard(
            mesh = activeSuite?.mesh3D ?: uiState.currentMesh,
            spriteSheet = activeSuite?.spriteSheet ?: uiState.currentSpriteSheet,
            activeTexture = activeSuite?.textureBitmap ?: uiState.activeTexture,
            hardwareSpec = uiState.selectedHardware,
            hardwareSimulationEnabled = uiState.hardwareSimulationEnabled,
            defaultCharacterTag = uiState.activeCharacterTagForImports,
            defaultMode = InspectorMode.INSPECT_DUAL,
            defaultSaveType = LibraryAssetType.UNIFIED_PAIR,
            savedLibraryAssets = uiState.savedLibraryAssets,
            onToggleHardwareSimulation = { viewModel.toggleHardwareSimulation() },
            onSaveToLibrary = { title, charTag, type, yaw, pitch, spriteRot ->
                viewModel.saveInspectedAssetToLibrary(title, charTag, type, yaw, pitch, spriteRot)
            },
            onLoadFromLibrary = { viewModel.loadLibraryAssetIntoStudio(it) },
            onDeleteFromLibrary = { viewModel.deleteLibraryAsset(it) },
            onActivateFusedHardware = { viewModel.activateFusedHardwareSpec(it) }
        )

        // Section 3: Live Compiled Single Asset Suite Showcase
        if (activeSuite != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compiled_suite_showcase_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACTIVE SINGLE ASSET SUITE",
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                color = CyanNeon
                            )
                            Text(
                                text = activeSuite.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeSuite.category} • Fused Inputs: ${activeSuite.importedInputsSummary.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.prepareSuiteManifestExport() },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                            modifier = Modifier.testTag("export_suite_bundle_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export Suite", fontFamily = TechMono, fontSize = 11.sp)
                        }
                    }

                    // Unified Master Palette Strip
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "UNIFIED SUITE MASTER PALETTE (${activeSuite.unifiedPalette.size} SHARED CLUT COLORS)",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = AmberGold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeSuite.unifiedPalette.forEach { colorEntry ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(colorEntry.colorInt))
                                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = colorEntry.hex,
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }

                    // 3-Way Interactive Suite Preview Tabs
                    TabRow(
                        selectedTabIndex = previewTab,
                        containerColor = Color(0xFF070B12),
                        contentColor = CyanNeon,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[previewTab]),
                                color = CyanNeon
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = previewTab == 0,
                            onClick = { previewTab = 0 },
                            text = { Text("Fused 3D + UV", fontFamily = TechMono, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = previewTab == 1,
                            onClick = { previewTab = 1 },
                            text = { Text("Fused Sprite (${activeSuite.spriteSheet.frames.size}f)", fontFamily = TechMono, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = previewTab == 2,
                            onClick = { previewTab = 2 },
                            text = { Text("UV Atlas & Concept", fontFamily = TechMono, fontSize = 11.sp) }
                        )
                    }

                    when (previewTab) {
                        0 -> {
                            // 3D Model with Bound Suite Texture
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Viewport3D(
                                    mesh = activeSuite.mesh3D,
                                    activeTexture = activeSuite.textureBitmap,
                                    hardwareSpec = uiState.selectedHardware,
                                    hardwareSimulationEnabled = uiState.hardwareSimulationEnabled,
                                    onToggleHardwareSimulation = { viewModel.toggleHardwareSimulation() },
                                    onExportObj = { viewModel.prepareObjExport() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        1 -> {
                            // Animated 2D Suite Spritesheet & Strip
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070B12))
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val currentFrame = suiteFrames.getOrElse(animFrameIndex % suiteFrames.size.coerceAtLeast(1)) {
                                    activeSuite.spriteSheet.sourceBitmap
                                }
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, MagentaNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = currentFrame.asImageBitmap(),
                                        contentDescription = "Animated Suite Sprite Frame",
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    )
                                }
                                Text(
                                    text = "${activeSuite.spriteSheet.name} • Frame ${(animFrameIndex % suiteFrames.size.coerceAtLeast(1)) + 1}/${suiteFrames.size} [${activeSuite.spriteOrigin.label}]",
                                    fontFamily = TechMono,
                                    fontSize = 11.sp,
                                    color = MagentaNeon
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    suiteFrames.forEachIndexed { idx, frameBmp ->
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A))
                                                .border(
                                                    width = if (idx == animFrameIndex % suiteFrames.size.coerceAtLeast(1)) 2.dp else 1.dp,
                                                    color = if (idx == animFrameIndex % suiteFrames.size.coerceAtLeast(1)) CyanNeon else CardBorder,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { animFrameIndex = idx },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = frameBmp.asImageBitmap(),
                                                contentDescription = "Suite Frame $idx",
                                                filterQuality = FilterQuality.None,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Side-by-Side UV Texture Atlas & Concept Reference
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF070B12))
                                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "3D UV TEXTURE ATLAS",
                                        fontFamily = TechMono,
                                        fontSize = 10.sp,
                                        color = EmeraldGreen
                                    )
                                    Image(
                                        bitmap = activeSuite.textureBitmap.asImageBitmap(),
                                        contentDescription = "Suite 3D UV Texture Atlas",
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Text(
                                        text = activeSuite.textureOrigin.label,
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF070B12))
                                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "CONCEPT REFERENCE",
                                        fontFamily = TechMono,
                                        fontSize = 10.sp,
                                        color = AmberGold
                                    )
                                    Image(
                                        bitmap = activeSuite.conceptArtBitmap.asImageBitmap(),
                                        contentDescription = "Suite Concept Reference",
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Text(
                                        text = activeSuite.conceptOrigin.label,
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // Synthesis & Character Fusion Pipeline Log
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF070B12))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CHARACTER AUTO-FUSION & COMPILATION LOG",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = CyanNeon,
                            fontWeight = FontWeight.Bold
                        )
                        activeSuite.buildNotes.forEach { note ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", color = EmeraldGreen, fontFamily = TechMono, fontSize = 11.sp)
                                Text(note, color = Color(0xFFCBD5E1), fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Combined Suite Hardware Coexistence & Dual-Level Readout
        if (activeSuite != null && suiteAllocation != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A111E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("suite_hardware_readout_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "UNIFIED SUITE HARDWARE COEXISTENCE & OUTPUT READOUT",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )

                    // Quick Hardware Target Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.hardwareCatalog.forEach { spec ->
                            val selected = uiState.selectedHardware.id == spec.id
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectHardware(spec) },
                                label = { Text(spec.shortName, fontFamily = TechMono, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberGold,
                                    selectedLabelColor = Color(0xFF031B26),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    // Reading Level Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReadingLevelMode.entries.forEach { mode ->
                            val selected = uiState.readingLevelMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setReadingLevelMode(mode) },
                                label = { Text(mode.shortLabel, fontFamily = TechMono, fontSize = 10.sp) },
                                leadingIcon = {
                                    val icon = when (mode) {
                                        ReadingLevelMode.EVERYDAY_READER -> Icons.Default.AutoStories
                                        ReadingLevelMode.HIGH_END_TECHNICAL -> Icons.Default.DeveloperBoard
                                        ReadingLevelMode.BOTH_LEVELS -> Icons.Default.Science
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Color(0xFF031B26),
                                    selectedLeadingIconColor = Color(0xFF031B26),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White,
                                    iconColor = CyanNeon
                                )
                            )
                        }
                    }

                    // Coexistence Verdict Banner
                    Surface(
                        color = Color(0xFF070B12),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = suiteAllocation.coexistenceVerdict,
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Everyday Reader Suite Output Readout
                    if (uiState.readingLevelMode == ReadingLevelMode.EVERYDAY_READER ||
                        uiState.readingLevelMode == ReadingLevelMode.BOTH_LEVELS
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF062019))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "EVERYDAY READER GUIDE • UNIFIED SUITE ON SCREEN",
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Text(
                                text = suiteAllocation.everydaySuiteReadout,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // High-End Technical Suite Silicon Readout
                    if (uiState.readingLevelMode == ReadingLevelMode.HIGH_END_TECHNICAL ||
                        uiState.readingLevelMode == ReadingLevelMode.BOTH_LEVELS
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1406))
                                .border(1.dp, AmberGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "HIGH-END TECHNICAL • MULTI-ASSET VRAM & CLUT PARTITION",
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                            Text(
                                text = suiteAllocation.technicalSuiteReadout,
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = Color(0xFFFDE68A)
                            )
                            HorizontalDivider(color = AmberGold.copy(alpha = 0.25f))
                            Text(
                                text = suiteAllocation.memoryMapDiagram,
                                fontFamily = TechMono,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = CyanNeon
                            )
                        }
                    }
                }
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

        // First-Class Audio Vault, 8-Profile Soundchip DSP Split, 6-Stage Importer & AES-256-GCM Cold Vault
        AudioAndVaultStudioPanel(
            uiState = uiState,
            viewModel = viewModel
        )

        // 3-Level Game -> Group -> Character Explorer & Quick Presets
        GameGroupCharacterBrowserPanel(
            uiState = uiState,
            viewModel = viewModel
        )

        // Custom Asset Galleries & Exhibitions
        AssetGalleriesPanel(
            uiState = uiState,
            viewModel = viewModel
        )

        // Persistent Room Database Asset History Vault
        RoomAssetHistoryPanel(
            historyEntities = uiState.roomAssetHistory,
            onRestoreEntity = { viewModel.restoreAssetFromRoomHistory(it) },
            onDeleteEntity = { viewModel.deleteAssetFromRoomHistory(it) },
            onSaveCurrentSnapshotToHistory = { viewModel.saveCurrentStudioStateToRoomHistory() },
            onClearAllHistory = { viewModel.clearRoomAssetHistory() }
        )
    }

    // Paste .OBJ Dialog for Slot 02 (with Named Character Tag)
    if (showObjImportDialog) {
        AlertDialog(
            onDismissRequest = { showObjImportDialog = false },
            title = {
                Text(
                    "Import .OBJ for '${uiState.activeCharacterTagForImports.ifBlank { "Character" }}'",
                    fontFamily = TechMono,
                    fontSize = 14.sp,
                    color = CyanNeon
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customObjName,
                        onValueChange = { customObjName = it },
                        label = { Text("Asset Title (Includes Character Name)", fontFamily = TechMono, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Text(
                        text = "Paste Wavefront .OBJ vertices (v) and faces (f). If another asset shares this character name, they will automatically fuse:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    OutlinedTextField(
                        value = rawObjInput,
                        onValueChange = { rawObjInput = it },
                        placeholder = {
                            Text(
                                "v 0.0 1.2 0.0\nv -0.8 -0.8 0.8\nv 0.8 -0.8 0.8\nf 1 2 3",
                                fontFamily = TechMono,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("suite_obj_paste_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawObjInput.isNotBlank()) {
                            viewModel.importObjText(
                                objContent = rawObjInput,
                                name = customObjName.ifBlank { "${uiState.activeCharacterTagForImports}_Mesh.obj" },
                                characterTagOverride = uiState.activeCharacterTagForImports
                            )
                            showObjImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26))
                ) {
                    Text("Import & Auto-Fuse", fontFamily = TechMono)
                }
            },
            dismissButton = {
                TextButton(onClick = { showObjImportDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}

@Composable
private fun CharacterClusterCard(
    group: NamedCharacterFusionGroup,
    isActiveInSuite: Boolean,
    onFuseGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070C17))
            .border(
                width = if (isActiveInSuite) 1.5.dp else 1.dp,
                color = if (isActiveInSuite) EmeraldGreen else CardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.characterName,
                        fontFamily = TechMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActiveInSuite) EmeraldGreen else Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (group.hasMultipleAssetsToFuse) MagentaNeon.copy(alpha = 0.2f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (group.hasMultipleAssetsToFuse) {
                                "${group.totalAssetCount} ASSETS • ${group.modalityCount} MODALITIES"
                            } else {
                                "1 ASSET STAGED"
                            },
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = if (group.hasMultipleAssetsToFuse) MagentaNeon else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = group.assets.joinToString(" + ") { "${it.assetTitle} [${it.modality.badge}]" },
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 15.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onFuseGroup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActiveInSuite) EmeraldGreen else MagentaNeon,
                    contentColor = if (isActiveInSuite) Color(0xFF031B26) else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("fuse_character_${group.canonicalKey.replace(" ", "_")}_btn")
            ) {
                Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isActiveInSuite) "Re-Fuse" else "Fuse Suite",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Reusable AssetPreviewCards for each asset in this Character Cluster
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.assets.forEach { staged ->
                AssetPreviewCard(
                    stagedAsset = staged,
                    onSelectOrFuse = onFuseGroup,
                    modifier = Modifier.width(235.dp)
                )
            }
        }
    }
}

@Composable
private fun IntakeSlotCard(
    slotNumber: String,
    title: String,
    statusText: String,
    isUserImported: Boolean,
    accentColor: Color,
    previewBitmap: android.graphics.Bitmap?,
    primaryActionLabel: String,
    primaryActionTag: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    secondaryActionTag: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    extraControls: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF080D18))
            .border(1.dp, if (isUserImported) accentColor.copy(alpha = 0.6f) else CardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = title,
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "[$slotNumber] $title",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = if (isUserImported) EmeraldGreen.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isUserImported) "IMPORTED" else "ACTIVE",
                                fontFamily = TechMono,
                                fontSize = 8.sp,
                                color = if (isUserImported) EmeraldGreen else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        modifier = if (secondaryActionTag != null) Modifier.testTag(secondaryActionTag) else Modifier
                    ) {
                        Text(secondaryActionLabel, fontFamily = TechMono, fontSize = 10.sp)
                    }
                }
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag(primaryActionTag)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(primaryActionLabel, fontFamily = TechMono, fontSize = 10.sp)
                }
            }
        }
        if (extraControls != null) {
            extraControls()
        }
    }
}
