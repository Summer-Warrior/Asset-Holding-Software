package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.hardware.FullAssetHardwareDossier
import com.example.model.hardware.ReadingLevelMode
import com.example.model.hardware.ReadoutSection
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

@Composable
fun AssetHardwareReadoutPanel(
    dossier: FullAssetHardwareDossier,
    readingLevel: ReadingLevelMode,
    onSelectReadingLevel: (ReadingLevelMode) -> Unit,
    onCopiedMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Reading Level Selector Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VIVID ASSET OUTPUT & ADAPTATION READOUT",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            color = CyanNeon,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${dossier.assetName} → ${dossier.systemName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val fullText = buildFullTextForClipboard(dossier)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Asset Hardware Dossier", fullText))
                            onCopiedMessage?.invoke("Copied full Technical & Everyday Readout for ${dossier.systemName}!")
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                        modifier = Modifier.testTag("copy_dossier_btn")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Dossier",
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Report", fontFamily = TechMono, fontSize = 11.sp, color = CyanNeon)
                    }
                }

                Text(
                    text = dossier.executiveHeadline,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                HorizontalDivider(color = CardBorder)

                // Reading Level Mode Chips
                Text(
                    text = "SELECT READING LEVEL:",
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
                    ReadingLevelMode.entries.forEach { mode ->
                        val selected = readingLevel == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectReadingLevel(mode) },
                            label = {
                                Text(
                                    text = mode.shortLabel,
                                    fontFamily = TechMono,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                val icon = when (mode) {
                                    ReadingLevelMode.EVERYDAY_READER -> Icons.Default.AutoStories
                                    ReadingLevelMode.HIGH_END_TECHNICAL -> Icons.Default.DeveloperBoard
                                    ReadingLevelMode.BOTH_LEVELS -> Icons.Default.Science
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (mode) {
                                    ReadingLevelMode.EVERYDAY_READER -> EmeraldGreen
                                    ReadingLevelMode.HIGH_END_TECHNICAL -> AmberGold
                                    ReadingLevelMode.BOTH_LEVELS -> CyanNeon
                                },
                                selectedLabelColor = Color(0xFF031B26),
                                selectedLeadingIconColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White,
                                iconColor = CyanNeon
                            ),
                            modifier = Modifier.testTag("reading_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Render Each Pipeline Stage Section
        dossier.sections.forEach { section ->
            ReadoutStageCard(
                section = section,
                readingLevel = readingLevel
            )
        }

        // Hardware Register & Memory Map Dump Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hardware_hex_dump_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ADJUSTED ASSET SILICON MEMORY & REGISTER DUMP",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        color = AmberGold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = dossier.hardwareRegisterHexDump,
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

@Composable
private fun ReadoutStageCard(
    section: ReadoutSection,
    readingLevel: ReadingLevelMode
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("readout_stage_${section.stepNumber}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stage Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(CyanNeon.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, CyanNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${section.stepNumber}",
                        fontFamily = TechMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = section.subtitle,
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Key Metrics Strip (2x2 or 4-across)
            if (section.keyMetrics.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF070B12))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    section.keyMetrics.take(2).forEach { (k, v) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = k, fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(text = v, fontFamily = TechMono, fontSize = 11.sp, color = CyanNeon)
                        }
                    }
                }
                if (section.keyMetrics.size > 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070B12))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        section.keyMetrics.drop(2).take(2).forEach { (k, v) ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = k, fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(text = v, fontFamily = TechMono, fontSize = 11.sp, color = AmberGold)
                            }
                        }
                    }
                }
            }

            // Everyday Reader Block
            if (readingLevel == ReadingLevelMode.EVERYDAY_READER || readingLevel == ReadingLevelMode.BOTH_LEVELS) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B1D1B))
                        .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "EVERYDAY VISUAL & PLAIN-ENGLISH READOUT",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    Text(
                        text = section.everydayReaderText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFECFDF5)
                    )
                }
            }

            // High-End Technical Block
            if (readingLevel == ReadingLevelMode.HIGH_END_TECHNICAL || readingLevel == ReadingLevelMode.BOTH_LEVELS) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1A160B))
                        .border(1.dp, AmberGold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperBoard,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "HIGH-END SILICON & REGISTER ENGINEERING READOUT",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                    Text(
                        text = section.highEndTechnicalText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFFEF3C7)
                    )
                }
            }
        }
    }
}

private fun buildFullTextForClipboard(dossier: FullAssetHardwareDossier): String {
    return buildString {
        appendLine("==================================================")
        appendLine("PIXELFORGE HARDWARE OUTPUT & ADAPTATION DOSSIER")
        appendLine("System: ${dossier.systemName}")
        appendLine("Asset:  ${dossier.assetName} (${dossier.assetTypeLabel})")
        appendLine("==================================================")
        appendLine(dossier.executiveHeadline)
        appendLine()
        dossier.sections.forEach { sec ->
            appendLine("--- STAGE ${sec.stepNumber}: ${sec.title.uppercase()} ---")
            appendLine("[Metrics] " + sec.keyMetrics.joinToString(" | ") { "${it.first}: ${it.second}" })
            appendLine()
            appendLine("[Everyday Reader Guide]:")
            appendLine(sec.everydayReaderText)
            appendLine()
            appendLine("[High-End Silicon Engineering Readout]:")
            appendLine(sec.highEndTechnicalText)
            appendLine()
        }
        appendLine("--- SILICON REGISTER & MEMORY DUMP ---")
        appendLine(dossier.hardwareRegisterHexDump)
    }
}
