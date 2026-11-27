package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.ai.AssetFidelityTag
import com.example.ai.PromptAssetPreset
import com.example.ai.PromptHardwareTier
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

/**
 * Modular Studio section combining:
 * 1) The Style-Locked True Fusion Prompt Studio (single-active tier, auto chroma-key,
 *    3 copyable formats, and 1-tap OmniAiHub run).
 * 2) The Canonical Asset & Official Spec Auditor (visual delta %, style fidelity score,
 *    clean-room IP guard, contract compliance, and ephemeral reference purge).
 */
@Composable
fun PromptStudioAndAuditPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val promptOutput = remember(
        uiState.promptText,
        uiState.promptHardwareTier,
        uiState.promptAssetPreset,
        uiState.includeHumanoidHeroicProportions,
        uiState.extractedPalette,
        uiState.activeGameTagForImports,
        uiState.activeGroupTagForImports,
        uiState.activeCharacterTagForImports,
        uiState.currentSpriteSheet.name
    ) {
        viewModel.buildCurrentPromptStudioOutput(uiState)
    }
    var selectedFormatTab by remember { mutableIntStateOf(0) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        viewModel.postStatusMessage("Copied $label (${text.length} chars) to clipboard!")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // =====================================================================
        // 1. STYLE-LOCKED TRUE FUSION PROMPT STUDIO
        // =====================================================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071020)),
            border = BorderStroke(1.2.dp, MagentaNeon.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prompt_studio_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Prompt Studio",
                            tint = MagentaNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "STYLE-LOCKED TRUE FUSION PROMPT STUDIO",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaNeon
                            )
                            Text(
                                text = "Single-active hardware tier • Auto chroma-key • Zero checkerboard leaks",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    val keyBgColor = if (promptOutput.chromaKey.hexCode == "#00FF00") Color(0xFF00FF00) else Color(0xFFFF00FF)
                    Surface(
                        color = keyBgColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, keyBgColor.copy(alpha = 0.75f))
                    ) {
                        Text(
                            text = "KEY ${promptOutput.chromaKey.hexCode}",
                            fontFamily = TechMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = keyBgColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Single-Active Hardware Tier Picker
                Text("HARDWARE TIER (SINGLE ACTIVE):", fontFamily = TechMono, fontSize = 9.sp, color = CyanNeon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PromptHardwareTier.entries.forEach { tier ->
                        val selected = uiState.promptHardwareTier == tier
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPromptHardwareTier(tier) },
                            label = { Text(tier.displayName, fontFamily = TechMono, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MagentaNeon,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF162135),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("prompt_tier_${tier.name.lowercase()}")
                        )
                    }
                }

                // Asset Preset Picker
                Text("ASSET LAYOUT PRESET:", fontFamily = TechMono, fontSize = 9.sp, color = AmberGold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PromptAssetPreset.entries.forEach { preset ->
                        val selected = uiState.promptAssetPreset == preset
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPromptAssetPreset(preset) },
                            label = { Text(preset.displayName, fontFamily = TechMono, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold,
                                selectedLabelColor = Color(0xFF1E1300),
                                containerColor = Color(0xFF162135),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("prompt_preset_${preset.name.lowercase()}")
                        )
                    }
                }

                // Optional 6.5-Head Humanoid Proportions Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C182D))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Heroic 6.5-Head Humanoid Proportions",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Optional — leave OFF for creatures, mechs, props, and icons",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = uiState.includeHumanoidHeroicProportions,
                        onCheckedChange = { viewModel.setIncludeHumanoidHeroicProportions(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF031B26),
                            checkedTrackColor = CyanNeon
                        ),
                        modifier = Modifier.testTag("prompt_humanoid_proportions_switch")
                    )
                }

                // Auto Chroma-Key Reason
                Text(
                    text = "${promptOutput.chromaKey.colorName} (${promptOutput.chromaKey.hexCode}): ${promptOutput.chromaKey.reason}",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E1)
                )

                // Format Selector Tabs & Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val formatLabels = listOf(
                        "Compact (${promptOutput.compact500.length}/500)",
                        "4-Sentence",
                        "Full Spec"
                    )
                    formatLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedFormatTab == idx,
                            onClick = { selectedFormatTab = idx },
                            label = { Text(label, fontFamily = TechMono, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF162135),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("prompt_format_tab_$idx")
                        )
                    }
                }

                val activePreviewText = when (selectedFormatTab) {
                    0 -> promptOutput.compact500
                    1 -> promptOutput.natural4Sentence
                    else -> promptOutput.fullSpec
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF050A14))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = activePreviewText,
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 14.sp
                    )
                }

                // 1-Tap Copy Row for all 3 formats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { copyToClipboard("Compact <=500 Prompt", promptOutput.compact500) },
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("copy_prompt_compact500_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy <=500", fontFamily = TechMono, fontSize = 9.sp)
                    }
                    OutlinedButton(
                        onClick = { copyToClipboard("Natural 4-Sentence Prompt", promptOutput.natural4Sentence) },
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("copy_prompt_4sentence_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy 4-Sent", fontFamily = TechMono, fontSize = 9.sp)
                    }
                    OutlinedButton(
                        onClick = { copyToClipboard("Full Spec Prompt", promptOutput.fullSpec) },
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("copy_prompt_fullspec_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy Spec", fontFamily = TechMono, fontSize = 9.sp)
                    }
                }

                // 1-Tap Run via Active OmniAiHub Provider
                Button(
                    onClick = { viewModel.runPromptStudioViaActiveOmniAi() },
                    colors = ButtonDefaults.buttonColors(containerColor = MagentaNeon, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("prompt_studio_run_omni_ai_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Run via Active OmniAiHub Provider",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =====================================================================
        // 2. CANONICAL ASSET & OFFICIAL SPEC AUDITOR
        // =====================================================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF07131E)),
            border = BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("canonical_asset_auditor_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Canonical Asset Auditor",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "CANONICAL ASSET & SPEC AUDITOR",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "Visual Delta %, Chroma/Alpha Check, Hardware Budget & IP Clean-Room Guard",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    val report = uiState.lastAssetAuditReport
                    if (report != null) {
                        val badgeColor = when (report.fidelityTag) {
                            AssetFidelityTag.PUBLIC_CLEAN_ROOM -> EmeraldGreen
                            AssetFidelityTag.PRIVATE_FAN_DERIVED -> AmberGold
                            AssetFidelityTag.NEEDS_REVISION -> Color(0xFFF43F5E)
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = report.fidelityTag.badge,
                                fontFamily = TechMono,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Public Release Clean-Room Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C182D))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Public Release Clean-Room Strict Check",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Flags NEEDS_REVISION if generated from a reference image",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = uiState.auditIntendedForPublicRelease,
                        onCheckedChange = { viewModel.setAuditIntendedForPublicRelease(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF022015),
                            checkedTrackColor = EmeraldGreen
                        ),
                        modifier = Modifier.testTag("audit_public_release_switch")
                    )
                }

                // Action Row: Audit Asset + Approve & Purge Temp Sources
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.runCanonicalAssetAudit(useOmniAiVision = true) },
                        enabled = !uiState.isAuditingAsset,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color(0xFF022015)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("run_canonical_asset_audit_btn")
                    ) {
                        if (uiState.isAuditingAsset) {
                            CircularProgressIndicator(
                                color = Color(0xFF022015),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Auditing...", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Audit Asset", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.approveAuditAndPurgeEphemeralSources(context) },
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("audit_approve_purge_btn")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Approve & Purge", fontFamily = TechMono, fontSize = 10.sp)
                    }
                }

                // Audit Report Display
                val report = uiState.lastAssetAuditReport
                if (report != null) {
                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuditMetricPill("STYLE", "${report.styleFidelityScore}/100", CyanNeon, Modifier.weight(1f))
                        AuditMetricPill("DELTA", "${report.visualDeltaPercent}%", MagentaNeon, Modifier.weight(1f))
                        AuditMetricPill(
                            "CHROMA",
                            if (report.chromaAndAlphaClean) "CLEAN" else "BLEED",
                            if (report.chromaAndAlphaClean) EmeraldGreen else Color(0xFFF43F5E),
                            Modifier.weight(1f)
                        )
                        AuditMetricPill(
                            "BUDGET",
                            if (report.hardwareBudgetCompliant) "PASS" else "OVER",
                            if (report.hardwareBudgetCompliant) EmeraldGreen else AmberGold,
                            Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Auditor: ${report.auditSource}",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )

                    if (report.improvementsSummary.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            report.improvementsSummary.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(text = item, fontSize = 10.sp, color = Color(0xFFD1FAE5))
                                }
                            }
                        }
                    }

                    if (report.regressionOrIpFlags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            report.regressionOrIpFlags.forEach { flag ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(text = flag, fontSize = 10.sp, color = Color(0xFFFDA4AF))
                                }
                            }
                        }
                    }

                    val contract = uiState.lastContractComplianceReport
                    if (contract != null) {
                        Text(
                            text = "Contract & Hardware Check: ${contract.meshTriangles}/${contract.maxRecommendedTriangles} tris • ${contract.spriteColorsDetected}/${contract.maxAllowedSpriteColors} colors • VRAM ${contract.estimatedVramBytes / 1024}/${contract.vramBudgetBytes / 1024} KB • JSON ${if (contract.contractJsonPassed) "VALID" else "NEEDS FIX"}",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = if (contract.hardwareBudgetPassed && contract.contractJsonPassed) EmeraldGreen else AmberGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontFamily = TechMono, fontSize = 8.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}
