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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.R
import com.example.ai.GeminiInputImageSource
import com.example.ai.GeminiOperationMode
import com.example.ai.PromptAssetPreset
import com.example.ai.PromptHardwareTier
import com.example.model.importer.UniversalAssetImporter
import com.example.ui.components.AiFileImportReviewCard
import com.example.ui.components.GeminiEnhancementPanel
import com.example.ui.components.QuickAddToGalleryBar
import com.example.ui.components.RoomAssetHistoryPanel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
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
    openCreateTabTrigger: Int = 0,
    onNavigateTo3D: (() -> Unit)? = null,
    onNavigateToSprites: (() -> Unit)? = null,
    onNavigateToScratchCanvas: (() -> Unit)? = null,
    onNavigateToSuite: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var spriteRes by remember { mutableIntStateOf(32) }
    var alsoExtrude3D by remember { mutableStateOf(false) }

    LaunchedEffect(openCreateTabTrigger) {
        if (openCreateTabTrigger > 0) {
            selectedTab = 0
        }
    }

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
                        text = "FROM-SCRATCH ASSET CREATOR & AI FORGE",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = CyanNeon,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Create Brand-New Sprites, Sheets, Icons & Variants",
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

        // Sub Tabs: 0 = Create New (From Scratch), 1 = Enhance / Variants, 2 = Concept Lab
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
                text = { Text("Create New", fontFamily = TechMono, fontSize = 11.sp) },
                modifier = Modifier.testTag("forge_subtab_create_new")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Enhance / Variants", fontFamily = TechMono, fontSize = 11.sp) },
                modifier = Modifier.testTag("forge_subtab_gemini_enhance")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Concept Lab", fontFamily = TechMono, fontSize = 11.sp) },
                modifier = Modifier.testTag("forge_subtab_concept_lab")
            )
        }

        when (selectedTab) {
            0 -> {
                // True From-Scratch AI Asset Creator Card (Zero Sample Preload)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "TRUE FROM-SCRATCH AI ASSET CREATOR",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }

                        Text(
                            text = "Generate an original 2D sprite, multi-frame sheet, portrait, or icon directly from your description using your connected AI provider (zero template fallback). Auto-strips chroma-key backgrounds and slices multi-frame sheets cleanly.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // Asset Name
                        OutlinedTextField(
                            value = uiState.scratchCreateAssetName,
                            onValueChange = { viewModel.updateScratchCreateAssetName(it) },
                            singleLine = true,
                            label = { Text("Asset Name", fontFamily = TechMono, fontSize = 11.sp) },
                            placeholder = {
                                Text(
                                    "e.g., Emerald Hatchling",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            },
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
                                .testTag("scratch_ai_asset_name_field")
                        )

                        // Subject Description
                        OutlinedTextField(
                            value = uiState.promptText,
                            onValueChange = { viewModel.updatePromptText(it) },
                            minLines = 3,
                            maxLines = 5,
                            label = { Text("Subject Description", fontFamily = TechMono, fontSize = 11.sp) },
                            placeholder = {
                                Text(
                                    "emerald dragon hatchling with golden horns and glowing cyan Mana Circuits",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = CyanNeon,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prompt_text_field")
                        )

                        // Asset Type Preset Chips
                        Text(
                            text = "ASSET TYPE PRESET",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PromptAssetPreset.entries.forEach { preset ->
                                val shortLabel = when (preset) {
                                    PromptAssetPreset.SINGLE_SPRITE_1X1 -> "1×1 Sprite"
                                    PromptAssetPreset.ROW_SHEET_1X4 -> "1×4 Row Sheet"
                                    PromptAssetPreset.GRID_SHEET_2X4 -> "2×4 Grid Sheet"
                                    PromptAssetPreset.VN_PORTRAIT -> "VN Portrait"
                                    PromptAssetPreset.VECTOR_UI_ICON -> "UI Icon"
                                }
                                FilterChip(
                                    selected = uiState.promptAssetPreset == preset,
                                    onClick = { viewModel.setPromptAssetPreset(preset) },
                                    label = { Text(shortLabel, fontFamily = TechMono, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanNeon,
                                        selectedLabelColor = Color(0xFF031B26),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("scratch_preset_${preset.name.lowercase()}")
                                )
                            }
                        }

                        // Single-Active Hardware Tier Chips
                        Text(
                            text = "HARDWARE STYLE TIER (SINGLE-ACTIVE)",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PromptHardwareTier.entries.forEach { tier ->
                                FilterChip(
                                    selected = uiState.promptHardwareTier == tier,
                                    onClick = { viewModel.setPromptHardwareTier(tier) },
                                    label = { Text(tier.displayName, fontFamily = TechMono, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MagentaNeon,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("scratch_tier_${tier.name.lowercase()}")
                                )
                            }
                        }

                        // Toggles: 6.5-Head Humanoid & Also Extrude 3D Relief
                        Surface(
                            color = Color(0xFF0B1220),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "6.5-Head Humanoid Proportions",
                                        fontFamily = TechMono,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Switch(
                                        checked = uiState.includeHumanoidHeroicProportions,
                                        onCheckedChange = { viewModel.setIncludeHumanoidHeroicProportions(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF031B26),
                                            checkedTrackColor = CyanNeon
                                        ),
                                        modifier = Modifier.testTag("scratch_humanoid_proportions_switch")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Also Extrude 3D Relief (.OBJ)",
                                        fontFamily = TechMono,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Switch(
                                        checked = alsoExtrude3D,
                                        onCheckedChange = { alsoExtrude3D = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF031B26),
                                            checkedTrackColor = EmeraldGreen
                                        ),
                                        modifier = Modifier.testTag("scratch_also_extrude_3d_switch")
                                    )
                                }
                            }
                        }

                        // Primary From-Scratch AI Generate CTA
                        Button(
                            onClick = { viewModel.createBrandNewAssetFromScratchWithAi(alsoExtrude3D) },
                            enabled = !uiState.isCreatingFromScratchWithAi,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color(0xFF031B12)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("create_brand_new_ai_asset_btn")
                        ) {
                            if (uiState.isCreatingFromScratchWithAi) {
                                CircularProgressIndicator(
                                    color = Color(0xFF031B12),
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Generating From Scratch...",
                                    fontFamily = TechMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Generate Brand-New Asset From Scratch (AI)",
                                    fontFamily = TechMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Actionable Error Card (never silently swaps to sample assets)
                        val aiErr = uiState.lastScratchAiError
                        if (!aiErr.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFF2B1118),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFB7185)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scratch_ai_error_banner")
                            ) {
                                Text(
                                    text = aiErr,
                                    fontFamily = TechMono,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFDA4AF),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Live Preview of Newly Generated From-Scratch Asset
                        val createdBmp = uiState.lastScratchAiCreationBitmap
                        if (createdBmp != null) {
                            Surface(
                                color = Color(0xFF090D16),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scratch_ai_preview_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "LATEST FROM-SCRATCH CREATION (${createdBmp.width}×${createdBmp.height}px)",
                                        fontFamily = TechMono,
                                        fontSize = 10.sp,
                                        color = EmeraldGreen
                                    )
                                    Image(
                                        bitmap = createdBmp.asImageBitmap(),
                                        contentDescription = "Newly generated from-scratch asset preview",
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF111827))
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (onNavigateToSprites != null) {
                                            OutlinedButton(
                                                onClick = onNavigateToSprites,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("scratch_ai_open_2d_btn")
                                            ) {
                                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Inspect in 2D", fontFamily = TechMono, fontSize = 10.sp)
                                            }
                                        }
                                        if (alsoExtrude3D && onNavigateTo3D != null) {
                                            OutlinedButton(
                                                onClick = onNavigateTo3D,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("scratch_ai_open_3d_btn")
                                            ) {
                                                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("View 3D Relief", fontFamily = TechMono, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Offline Direct-Draw Alternative CTA
                        if (onNavigateToScratchCanvas != null) {
                            OutlinedButton(
                                onClick = onNavigateToScratchCanvas,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("forge_open_scratch_canvas_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Or Draw Offline on Blank 2D Pixel Canvas",
                                    fontFamily = TechMono,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
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
                                        selectedTab = 1
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
                                        selectedTab = 1
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
