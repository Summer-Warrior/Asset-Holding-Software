package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.R
import com.example.ai.ArtTheme
import com.example.ai.AssetTarget
import com.example.ai.GeminiInputImageSource
import com.example.ai.GeminiOperationMode
import com.example.model.importer.UniversalAssetImporter
import com.example.ui.components.AiFileImportReviewCard
import com.example.ui.components.GeminiEnhancementPanel
import com.example.ui.components.QuickAddToGalleryBar
import com.example.ui.components.RoomAssetHistoryPanel
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ForgeStudioScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateTo3D: (() -> Unit)? = null,
    onNavigateToSprites: (() -> Unit)? = null,
    onNavigateToSuite: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var spriteRes by remember { mutableIntStateOf(32) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val decoded = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            UniversalAssetImporter.decodeBitmapUnbounded(
                                inputStream = stream,
                                maxWorkingDimension = 2048
                            )
                        }
                    }
                    if (decoded != null) {
                        viewModel.importConceptArtBitmap(decoded.bitmap, "Imported Concept Image")
                        viewModel.setGeminiInputImageSource(GeminiInputImageSource.IMPORTED_CONCEPT_ART)
                    }
                } catch (e: Exception) {
                    viewModel.postStatusMessage("Failed to load image: ${e.message}")
                }
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
        // Hero Visual Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.hero_banner),
                    contentDescription = "PixelForge Studio Workshop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xEE090D16))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "GEMINI AI ASSET SYNTHESIS & VARIANT LAB",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Enhance Imported Images & Generate Playable Variants",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Quick Launcher to Single Asset Suite Compiler
        if (onNavigateToSuite != null) {
            OutlinedButton(
                onClick = onNavigateToSuite,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_suite_compiler_from_forge_btn")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Open Single Asset Suite Compiler (Combine All Imported Assets)",
                    fontFamily = TechMono,
                    fontSize = 11.sp
                )
            }
        }

        // Sub Tabs: 0 = Gemini AI Enhance & Variants, 1 = Prompt Synthesis, 2 = Concept Art Lab
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0F172A),
            contentColor = CyanNeon,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyanNeon
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Gemini Enhance", fontFamily = TechMono, fontSize = 11.sp) },
                modifier = Modifier.testTag("forge_subtab_gemini_enhance")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Prompt Synth", fontFamily = TechMono, fontSize = 11.sp) },
                modifier = Modifier.testTag("forge_subtab_prompt_synth")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Concept Art Lab", fontFamily = TechMono, fontSize = 11.sp) },
                modifier = Modifier.testTag("forge_subtab_concept_lab")
            )
        }

        when (selectedTab) {
            0 -> {
                // Full Gemini Multimodal Asset Enhancement & Variant Generation Lab
                GeminiEnhancementPanel(
                    uiState = uiState,
                    viewModel = viewModel,
                    onPickNewImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onNavigateTo3D = onNavigateTo3D,
                    onNavigateToSprites = onNavigateToSprites
                )
            }

            1 -> {
                // Text Prompt Synthesizer + Direct Gemini AI Enhance / Variant Triggers
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "DESCRIBE YOUR GAME ASSET",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )

                        OutlinedTextField(
                            value = uiState.promptText,
                            onValueChange = { viewModel.updatePromptText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("prompt_text_field"),
                            placeholder = { Text("e.g., Cyberpunk assault mecha with dual energy blades...", color = Color(0xFF64748B)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Quick Inspirations
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val ideas = listOf(
                                "Cyber Mech",
                                "Sci-Fi Starfighter",
                                "Dungeon Mimic Chest",
                                "Aether Crystal",
                                "Cyber Blade Ninja"
                            )
                            ideas.forEach { idea ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.updatePromptText(idea) },
                                    label = { Text(idea, fontFamily = TechMono, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Target Type: 3D vs 2D
                        Text("Output Format", fontFamily = TechMono, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = uiState.selectedTarget == AssetTarget.MODEL_3D,
                                onClick = { viewModel.setTarget(AssetTarget.MODEL_3D) },
                                label = { Text("3D Model (.OBJ)", fontFamily = TechMono, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Color(0xFF031B26),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.testTag("target_model_3d")
                            )
                            FilterChip(
                                selected = uiState.selectedTarget == AssetTarget.SPRITE_SHEET,
                                onClick = { viewModel.setTarget(AssetTarget.SPRITE_SHEET) },
                                label = { Text("2D Sprite Sheet", fontFamily = TechMono, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MagentaNeon,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.testTag("target_spritesheet")
                            )
                        }

                        // Theme selector
                        Text("Art Theme", fontFamily = TechMono, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ArtTheme.entries.forEach { theme ->
                                FilterChip(
                                    selected = uiState.selectedTheme == theme,
                                    onClick = { viewModel.setTheme(theme) },
                                    label = { Text(theme.displayName, fontFamily = TechMono, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF031B26),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Fast Procedural Synthesize Button
                        Button(
                            onClick = {
                                viewModel.generateFromPrompt()
                                if (uiState.selectedTarget == AssetTarget.MODEL_3D) {
                                    onNavigateTo3D?.invoke()
                                } else {
                                    onNavigateToSprites?.invoke()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = Color(0xFF031B26)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("synthesize_asset_btn")
                        ) {
                            if (uiState.isGenerating) {
                                CircularProgressIndicator(
                                    color = Color(0xFF031B26),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Synthesizing Geometry...", fontFamily = TechMono)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Synthesize Base Asset", fontFamily = TechMono, fontSize = 14.sp)
                            }
                        }

                        // Direct Gemini AI Enhancement & Variant Generation Buttons from Description
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.runGeminiEnhancementOrVariantGeneration(
                                        modeOverride = GeminiOperationMode.ENHANCE_ASSET,
                                        sourceOverride = GeminiInputImageSource.DESCRIPTION_ONLY
                                    )
                                    selectedTab = 0
                                },
                                enabled = !uiState.isGenerating,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("prompt_gemini_enhance_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Gemini Enhance", fontFamily = TechMono, fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.runGeminiEnhancementOrVariantGeneration(
                                        modeOverride = GeminiOperationMode.GENERATE_VARIANTS,
                                        sourceOverride = GeminiInputImageSource.DESCRIPTION_ONLY
                                    )
                                    selectedTab = 0
                                },
                                enabled = !uiState.isGenerating,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MagentaNeon),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("prompt_gemini_variants_btn")
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Gemini Variants", fontFamily = TechMono, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                // Concept Art Lab + Send Imported Image to Gemini for Enhancement / Variants
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "IMPORT CONCEPT ART OR ASSET PHOTO (UNBOUNDED SIZE)",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )

                        val concept = uiState.conceptArtBitmap
                        if (concept != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070B12)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = concept.asImageBitmap(),
                                    contentDescription = "Imported Concept Art",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070B12))
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text("No concept art loaded yet", color = Color(0xFF64748B), fontSize = 13.sp)
                                }
                            }
                        }

                        // Import Concept Art Button
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_concept_art_btn")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Select Concept Image", fontFamily = TechMono, fontSize = 13.sp)
                        }

                        // Extracted Palette
                        if (uiState.extractedPalette.isNotEmpty()) {
                            Text(
                                text = "DOMINANT PALETTE (${uiState.extractedPalette.size} COLORS)",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                color = CyanNeon
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.extractedPalette.forEach { colorItem ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(colorItem.colorInt))
                                                .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(colorItem.hex, fontFamily = TechMono, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }

                        // Conversion & Gemini AI Actions
                        if (uiState.conceptArtBitmap != null) {
                            Text(
                                text = "CONVERT OR SEND IMPORTED IMAGE TO GEMINI AI",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            // Resolution Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pixel Scale:", fontFamily = TechMono, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                listOf(16, 24, 32, 48).forEach { res ->
                                    FilterChip(
                                        selected = spriteRes == res,
                                        onClick = { spriteRes = res },
                                        label = { Text("${res}px", fontFamily = TechMono, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MagentaNeon,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.convertConceptToSprite(spriteRes)
                                        onNavigateToSprites?.invoke()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MagentaNeon, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("convert_concept_to_sprite")
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Make Sprite", fontFamily = TechMono, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.convertConceptTo3D()
                                        onNavigateTo3D?.invoke()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("convert_concept_to_3d")
                                ) {
                                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Extrude 3D", fontFamily = TechMono, fontSize = 12.sp)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.runGeminiEnhancementOrVariantGeneration(
                                            modeOverride = GeminiOperationMode.ENHANCE_ASSET,
                                            sourceOverride = GeminiInputImageSource.IMPORTED_CONCEPT_ART
                                        )
                                        selectedTab = 0
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("concept_gemini_enhance_btn")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Gemini Enhance", fontFamily = TechMono, fontSize = 10.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.runGeminiEnhancementOrVariantGeneration(
                                            modeOverride = GeminiOperationMode.GENERATE_VARIANTS,
                                            sourceOverride = GeminiInputImageSource.IMPORTED_CONCEPT_ART
                                        )
                                        selectedTab = 0
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MagentaNeon),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("concept_gemini_variants_btn")
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Gemini Variants", fontFamily = TechMono, fontSize = 10.sp)
                                }
                            }
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

        QuickAddToGalleryBar(
            uiState = uiState,
            viewModel = viewModel,
            assetScope = "UNIFIED_PAIR",
            label = "Pin 3D+2D Asset to Gallery"
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
}
