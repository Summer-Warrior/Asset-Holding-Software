package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.ai.ArtTheme
import com.example.ai.GeminiAssetForge
import com.example.ai.GeminiAssetVariant
import com.example.ai.GeminiForgeUiState
import com.example.ai.GeminiInputImageSource
import com.example.ai.GeminiOperationMode
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

/**
 * Interactive Gemini AI Asset Enhancement & Variant Generation Lab panel.
 * Allows users to send an imported asset image (Concept Art / Photo, Active 2D Spritesheet,
 * Active 3D Texture) and/or asset description to Gemini (`gemini-3.8-flash` and `gemini-nano-banana-2.1`)
 * for automatic enhancement or multi-variant generation.
 */
@Composable
fun GeminiEnhancementPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    onPickNewImage: (() -> Unit)? = null,
    onNavigateTo3D: (() -> Unit)? = null,
    onNavigateToSprites: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeInputBitmap: Bitmap? = viewModel.resolveSelectedGeminiInputBitmap(uiState)
    var showOmniAiHubSheet by remember { mutableStateOf(false) }
    val enabledSourceCount = uiState.knowledgeSources.count { it.enabled }
    val activeTextLabel = shortProviderName(uiState.omniAiRouting.activeTextProvider)
    val activeImageLabel = shortProviderName(uiState.omniAiRouting.activeImageProvider)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1222)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_enhancement_panel")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI",
                        tint = CyanNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "GEMINI AI ENHANCEMENT & VARIANT FORGE",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                        Text(
                            text = "Send imported asset image &/or description to Gemini for automatic enhancement or variants",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    color = EmeraldGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = if (uiState.geminiOperationMode == GeminiOperationMode.AI_IMAGE_SYNTHESIS) {
                            activeImageLabel.uppercase()
                        } else {
                            activeTextLabel.uppercase()
                        },
                        fontFamily = TechMono,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Multi-AI & Multi-Source Hub Launcher Pill
            OutlinedButton(
                onClick = { showOmniAiHubSheet = true },
                border = androidx.compose.foundation.BorderStroke(1.2.dp, CyanNeon.copy(alpha = 0.75f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF0E1B30),
                    contentColor = CyanNeon
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("open_omni_ai_hub_btn")
            ) {
                Icon(Icons.Default.Hub, contentDescription = "Open Multi-AI Hub", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AI Hub • Text: $activeTextLabel • Img: $activeImageLabel • $enabledSourceCount Src",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Step 1: Select Operation Mode
            Text(
                text = "1. SELECT GEMINI OPERATION MODE",
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
                GeminiOperationMode.entries.forEach { mode ->
                    val selected = uiState.geminiOperationMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setGeminiOperationMode(mode) },
                        label = {
                            Text(
                                text = mode.title,
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon,
                            selectedLabelColor = Color(0xFF031B26),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("gemini_mode_${mode.name.lowercase()}")
                    )
                }
            }
            Text(
                text = uiState.geminiOperationMode.subtitle,
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )

            // Step 2: Choose Imported Asset Image Source (or Description Only)
            Text(
                text = "2. MULTIMODAL INPUT IMAGE SOURCE (OPTIONAL WITH DESCRIPTION)",
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
                GeminiInputImageSource.entries.forEach { src ->
                    val selected = uiState.geminiInputImageSource == src
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setGeminiInputImageSource(src) },
                        label = { Text(src.label, fontFamily = TechMono, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MagentaNeon,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("gemini_source_${src.name.lowercase()}")
                    )
                }
            }

            // Visual Preview of Selected Input Asset Image + Quick Photo Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070C17))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeInputBitmap != null && uiState.geminiInputImageSource != GeminiInputImageSource.DESCRIPTION_ONLY) {
                        Image(
                            bitmap = activeInputBitmap.asImageBitmap(),
                            contentDescription = "Selected Gemini Input Image",
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(4.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Description Only Mode",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (activeInputBitmap != null && uiState.geminiInputImageSource != GeminiInputImageSource.DESCRIPTION_ONLY) {
                            "Sending Image: ${uiState.geminiInputImageSource.badge} (${activeInputBitmap.width}×${activeInputBitmap.height}px)"
                        } else {
                            "Sending Description Text Only (No Image Attached)"
                        },
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Active 3D: ${uiState.currentMesh.name} (${uiState.currentMesh.vertexCount}v) • Active Sprite: ${uiState.currentSpriteSheet.name}",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (onPickNewImage != null) {
                    OutlinedButton(
                        onClick = onPickNewImage,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("gemini_pick_image_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import Photo", modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import", fontFamily = TechMono, fontSize = 10.sp)
                    }
                }
            }

            // Step 3: Asset Description & Enhancement/Variant Direction
            Text(
                text = "3. ASSET DESCRIPTION & ENHANCEMENT / VARIANT INSTRUCTIONS",
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = CyanNeon
            )
            OutlinedTextField(
                value = uiState.promptText,
                onValueChange = { viewModel.updatePromptText(it) },
                label = { Text("Asset Description / Character Lore", fontFamily = TechMono, fontSize = 10.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_description_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CyanNeon,
                    unfocusedLabelColor = Color(0xFF94A3B8)
                )
            )

            OutlinedTextField(
                value = uiState.geminiCustomInstruction,
                onValueChange = { viewModel.updateGeminiCustomInstruction(it) },
                label = { Text("Enhancement / Variant Instruction", fontFamily = TechMono, fontSize = 10.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_instruction_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MagentaNeon,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = MagentaNeon,
                    unfocusedLabelColor = Color(0xFF94A3B8)
                )
            )

            // Quick Preset Instructions & Style Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isArcadeComicCelSelected = uiState.selectedTheme == ArtTheme.ARCADE_COMIC_CEL
                FilterChip(
                    selected = isArcadeComicCelSelected,
                    onClick = { viewModel.selectArcadeComicCelStylePreset() },
                    label = {
                        Text(
                            text = GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_PRESET_TITLE,
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberGold,
                        selectedLabelColor = Color(0xFF031B26),
                        containerColor = Color(0xFF271F0E),
                        labelColor = AmberGold
                    ),
                    modifier = Modifier.testTag("gemini_preset_arcade_comic_cel")
                )
                val presets = listOf(
                    "Boost cyber-armor silhouette + 4 elemental variants",
                    "Create Boss Form, Stealth Ops & 16-Bit Retro variants",
                    "Add swept plasma wings, orbital halo & high-contrast neon palette",
                    "Adapt geometry & sprite sub-pixels for ${uiState.selectedHardware.shortName} silicon"
                )
                presets.forEachIndexed { idx, preset ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.updateGeminiCustomInstruction(preset) },
                        label = { Text(preset, fontFamily = TechMono, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("gemini_preset_chip_$idx")
                    )
                }
            }

            // Automatic Closed-Loop Style Audit & 1-Pass Repair Toggle
            AutoStyleAuditToggleRow(
                enabled = uiState.autoRetryAiStyleGate,
                onToggle = { viewModel.toggleAutoRetryAiStyleGate() },
                testTag = "gemini_auto_audit_repair_switch"
            )

            // Primary Gemini Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.runGeminiEnhancementOrVariantGeneration(
                            modeOverride = GeminiOperationMode.ENHANCE_ASSET
                        )
                    },
                    enabled = !uiState.isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = Color(0xFF031B26)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gemini_enhance_asset_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enhance with Gemini", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.runGeminiEnhancementOrVariantGeneration(
                            modeOverride = GeminiOperationMode.GENERATE_VARIANTS
                        )
                    },
                    enabled = !uiState.isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MagentaNeon,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gemini_generate_variants_btn")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate Variants", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Optional Secondary Button: Runtime Visual Image Render via gemini-nano-banana-2.1
            OutlinedButton(
                onClick = {
                    viewModel.runGeminiEnhancementOrVariantGeneration(
                        modeOverride = GeminiOperationMode.AI_IMAGE_SYNTHESIS
                    )
                },
                enabled = !uiState.isGenerating,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("gemini_synthesize_image_btn")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Render AI Visual Variant Image (gemini-nano-banana-2.1 → 3D & 2D)",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Live Gemini Status / Loading / Error / Result Banner
            when (val forgeState = uiState.geminiForgeState) {
                is GeminiForgeUiState.Idle -> {
                    // Ready hint
                }
                is GeminiForgeUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1B2E))
                            .border(1.dp, CyanNeon, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .testTag("gemini_loading_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CyanNeon,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = forgeState.progressLabel,
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = CyanNeon
                        )
                    }
                }
                is GeminiForgeUiState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2A0E17))
                            .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .testTag("gemini_error_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Gemini Error",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = forgeState.message,
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = Color(0xFFFDA4AF)
                        )
                    }
                }
                is GeminiForgeUiState.Success -> {
                    val report = forgeState.report
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF07131F))
                            .border(1.dp, EmeraldGreen.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .testTag("gemini_success_report"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "GEMINI ENHANCED: ${report.enhancedTitle}",
                                fontFamily = TechMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }

                        Text(
                            text = report.technicalEnhancementSummary,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )

                        Text(
                            text = "Lore & Art Direction: ${report.enhancedLoreSpec}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // If Gemini synthesized a runtime bitmap via gemini-nano-banana-2.1, display it!
                        if (report.aiSynthesizedBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF050811))
                                    .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = report.aiSynthesizedBitmap.asImageBitmap(),
                                    contentDescription = "Gemini Synthesized Visual Asset",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        // Automatic Closed-Loop Style Check Verdict Strip
                        AutoStyleAuditVerdictStrip(
                            report = report.autoAuditReport ?: uiState.lastAssetAuditReport,
                            repairNote = uiState.lastAiAutoRepairNote,
                            testTag = "gemini_auto_style_verdict_strip"
                        )

                        // Quick Navigation to Inspect Enhanced Primary in 3D or 2D Studio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onNavigateTo3D != null) {
                                OutlinedButton(
                                    onClick = onNavigateTo3D,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Inspect 3D Mesh", fontFamily = TechMono, fontSize = 10.sp)
                                }
                            }
                            if (onNavigateToSprites != null) {
                                OutlinedButton(
                                    onClick = onNavigateToSprites,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MagentaNeon),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Inspect 2D Sprite", fontFamily = TechMono, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Generated Variants Gallery using Reusable AssetPreviewCard
            if (uiState.geminiGeneratedVariants.isNotEmpty()) {
                Text(
                    text = "GEMINI GENERATED VARIANTS (${uiState.geminiGeneratedVariants.size} PLAYABLE FORMS)",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold,
                    modifier = Modifier.testTag("gemini_variants_header")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.geminiGeneratedVariants.forEachIndexed { idx, variant ->
                        GeminiVariantCardItem(
                            variant = variant,
                            index = idx,
                            hardwareShortName = uiState.selectedHardware.shortName,
                            onApplyToStudio = {
                                viewModel.applyGeminiVariantToStudio(variant)
                            },
                            onSaveToLibrary = {
                                viewModel.saveGeminiVariantToLibrary(variant)
                            }
                        )
                    }
                }
            }

            // Style-Locked True Fusion Prompt Studio + Canonical Asset & Spec Auditor
            PromptStudioAndAuditPanel(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }

    if (showOmniAiHubSheet) {
        OmniAiConnectionsSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showOmniAiHubSheet = false }
        )
    }
}

@Composable
private fun GeminiVariantCardItem(
    variant: GeminiAssetVariant,
    index: Int,
    hardwareShortName: String,
    onApplyToStudio: () -> Unit,
    onSaveToLibrary: () -> Unit
) {
    val tags = buildList {
        add(variant.variantBadge)
        add("Scale ${"%.2f".format(variant.scaleX)}×${"%.2f".format(variant.scaleY)}×${"%.2f".format(variant.scaleZ)}")
        if (variant.addWingsOrFins) add("+Swept Wings")
        if (variant.addOrbitalRing) add("+Orbital Ring")
        add("${variant.primaryHex} / ${variant.accentHex}")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF060B16))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssetPreviewCard(
            title = variant.variantName,
            assetTypeTag = "3D + SPRITE VARIANT • ${variant.variantBadge}",
            metadataSummary = "${variant.mesh.vertexCount}v/${variant.mesh.faceCount}f • ${variant.spriteSheet.frames.size}f — ${variant.technicalBreakdown}",
            subtitleOrCharacterTag = variant.characterTag,
            hardwareBadge = hardwareShortName,
            sourceBadge = "GEMINI AI",
            metadataTags = tags,
            meshPreview = variant.mesh,
            bitmapPreview = variant.spriteSheet.frames.firstOrNull(),
            testTagPrefix = "gemini_variant_card_$index",
            primaryActionLabel = "Apply Variant to 3D & 2D Studio",
            onPrimaryAction = onApplyToStudio,
            onClick = onApplyToStudio
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Variant Swatch Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                variant.palette.forEach { swatch ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(swatch.colorInt))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = swatch.hex,
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onSaveToLibrary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("gemini_variant_save_btn_$index")
                ) {
                    Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save to Library", fontFamily = TechMono, fontSize = 9.sp)
                }

                Button(
                    onClick = onApplyToStudio,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("gemini_variant_apply_btn_$index")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Use", fontFamily = TechMono, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
