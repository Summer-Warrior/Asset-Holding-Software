package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AssetAuditReport
import com.example.ai.AssetFidelityTag
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

/**
 * Compact Obsidian toggle row enabling/disabling the 1-pass automatic style repair retry gate.
 */
@Composable
fun AutoStyleAuditToggleRow(
    enabled: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0B1220),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (enabled) EmeraldGreen.copy(alpha = 0.45f) else CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                    contentDescription = "Auto-Audit and 1-Pass Style Repair",
                    tint = if (enabled) EmeraldGreen else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Auto-Audit & 1-Pass Style Repair",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Audits chroma bleed & cel contrast; auto-retries once if < 68%",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF031B12),
                    checkedTrackColor = EmeraldGreen
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

/**
 * Compact Obsidian Automatic Style Check Verdict Strip shown directly beneath AI generation results.
 */
@Composable
fun AutoStyleAuditVerdictStrip(
    report: AssetAuditReport?,
    repairNote: String?,
    modifier: Modifier = Modifier,
    testTag: String = "auto_style_audit_verdict_strip"
) {
    if (report == null) return

    val tagAccent = when (report.fidelityTag) {
        AssetFidelityTag.PUBLIC_CLEAN_ROOM -> EmeraldGreen
        AssetFidelityTag.PRIVATE_FAN_DERIVED -> AmberGold
        AssetFidelityTag.NEEDS_REVISION -> MagentaNeon
    }

    Surface(
        color = Color(0xFF07101D),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, tagAccent.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (report.fidelityTag == AssetFidelityTag.NEEDS_REVISION) {
                            Icons.Default.WarningAmber
                        } else {
                            Icons.Default.Verified
                        },
                        contentDescription = "Automatic Style Check Verdict",
                        tint = tagAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "AUTO STYLE AUDIT",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = tagAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, tagAccent.copy(alpha = 0.65f))
                    ) {
                        Text(
                            text = report.fidelityTag.badge,
                            fontFamily = TechMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = "${report.styleFidelityScore}%",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    val chromaColor = if (report.chromaBleedDetected) MagentaNeon else EmeraldGreen
                    Surface(
                        color = chromaColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = if (report.chromaBleedDetected) "BLEED" else "CLEAN",
                            fontFamily = TechMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = chromaColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!repairNote.isNullOrBlank()) {
                Text(
                    text = repairNote,
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E182A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            if (report.issues.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    report.issues.take(3).forEach { issue ->
                        Text(
                            text = "• $issue",
                            fontSize = 10.sp,
                            color = Color(0xFFFDA4AF)
                        )
                    }
                }
            }
        }
    }
}
