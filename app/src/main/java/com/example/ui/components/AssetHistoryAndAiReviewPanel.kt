package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiSpriteSheetReviewResult
import com.example.data.local.AssetHistoryEntity
import com.example.model.hardware.ReadingLevelMode
import com.example.model.library.LibraryAssetType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI File Import Reviewer Banner & Interactive Card.
 * Automatically reviews imported files and photos via AI (Gemini Vision + Structural Grid Scan)
 * to check whether a multi-frame spritesheet is being added as a single picture image (1×1).
 */
@Composable
fun AiFileImportReviewCard(
    reviewResult: AiSpriteSheetReviewResult?,
    isReviewing: Boolean,
    readingLevelMode: ReadingLevelMode,
    onApplyRecommendedGrid: (AiSpriteSheetReviewResult) -> Unit,
    onRunDeepGeminiReview: () -> Unit,
    onKeepAsSingleImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alertColor = if (reviewResult?.isSpriteSheetInSingleImage == true) {
        Color(0xFFFFB300) // Amber Warning for Spritesheet-in-Single-Picture detection
    } else {
        Color(0xFF00E676) // Emerald Verified
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_file_import_review_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (reviewResult != null) alertColor.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
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
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(alertColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (reviewResult?.isSpriteSheetInSingleImage == true) {
                                Icons.Default.Warning
                            } else {
                                Icons.Default.AutoAwesome
                            },
                            contentDescription = "AI File Import Reviewer",
                            tint = alertColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AI FILE IMPORT SPRITESHEET REVIEWER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = alertColor
                        )
                        Text(
                            text = "Checks if an imported file/photo is a multi-frame spritesheet added as a single 1×1 picture",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isReviewing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = alertColor
                    )
                } else if (reviewResult != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = alertColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${reviewResult.confidencePercent}% AI CONFIDENCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = alertColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (reviewResult != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        alertColor.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (reviewResult.isSpriteSheetInSingleImage) {
                                        Icons.Default.GridOn
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = "Verdict Icon",
                                    tint = alertColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = reviewResult.verdictTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = reviewResult.reviewSource,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (readingLevelMode != ReadingLevelMode.HIGH_END_TECHNICAL) {
                            Text(
                                text = reviewResult.aiReviewSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (readingLevelMode != ReadingLevelMode.EVERYDAY_READER) {
                            Text(
                                text = "TECH TELEMETRY: ${reviewResult.technicalGridEvidence}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (reviewResult.isSpriteSheetInSingleImage) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApplyRecommendedGrid(reviewResult) },
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .testTag("ai_review_auto_slice_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = alertColor,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridOn,
                                        contentDescription = "Auto-Slice Spritesheet",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Auto-Slice ${reviewResult.recommendedColumns}×${reviewResult.recommendedRows} (${reviewResult.detectedFrameCount}f)",
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = onKeepAsSingleImage,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_review_keep_single_button")
                                ) {
                                    Text(
                                        text = "Keep 1×1 Image",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRunDeepGeminiReview,
                    enabled = !isReviewing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_review_gemini_rescan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Run AI Review on Active Image",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (reviewResult == null) {
                            "AI Review Active File/Image for Hidden Spritesheet Grid"
                        } else {
                            "Re-Run Gemini Vision Spritesheet-in-Single-Image Check"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Room Database Asset History Panel (`AssetHistoryEntity` + `AssetHistoryDao`).
 * Lets users browse, filter, inspect, and restore generated 3D models, 2D spritesheets,
 * 3D+2D hybrid fusions, and Gemini variants stored in the local Room SQLite database.
 */
@Composable
fun RoomAssetHistoryPanel(
    historyEntities: List<AssetHistoryEntity>,
    onRestoreEntity: (AssetHistoryEntity) -> Unit,
    onDeleteEntity: (Long) -> Unit,
    onSaveCurrentSnapshotToHistory: () -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filteredEntities = remember(historyEntities, selectedFilter) {
        when (selectedFilter) {
            "MODEL_3D" -> historyEntities.filter { it.assetType == "MODEL_3D" || it.objMeshData != null }
            "SPRITE_2D" -> historyEntities.filter { it.assetType == "SPRITE_2D" || it.spritePngBytes != null }
            "FUSED_3D_2D" -> historyEntities.filter {
                it.assetType == "FUSED_3D_2D" || it.sourceOrigin.contains("FUSION", ignoreCase = true)
            }
            "GEMINI_AI" -> historyEntities.filter {
                it.assetType == "GEMINI_VARIANT" || it.sourceOrigin.contains("GEMINI", ignoreCase = true)
            }
            else -> historyEntities
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_asset_history_panel"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Room Database Asset History",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = "ROOM DB ASSET HISTORY VAULT",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Persistent SQLite Entity & DAO history of generated 3D models, spritesheets & fusions",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${historyEntities.size} Stored",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Action bar: Snapshot current studio state into Room DB or Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveCurrentSnapshotToHistory,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("room_history_save_snapshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Record Active Asset to Room History",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Record Active Asset to Room DB",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (historyEntities.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearAllHistory,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("room_history_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Room History",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All (${historyEntities.size})",
                    "MODEL_3D" to "3D Models",
                    "SPRITE_2D" to "2D Spritesheets",
                    "FUSED_3D_2D" to "3D+2D Fusions",
                    "GEMINI_AI" to "Gemini AI"
                )
                filters.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (filteredEntities.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Empty History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No matching assets in Room history yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Generate a 3D model, spritesheet, 3D+2D fusion, or tap 'Record Active Asset to Room DB' above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredEntities.take(8).forEach { entity ->
                        val reconstructedMesh = remember(entity.id, entity.objMeshData) {
                            entity.toMesh3D()
                        }
                        val reconstructedSprite = remember(entity.id, entity.spritePngBytes) {
                            entity.toSpriteSheetData()
                        }
                        val mappedType = when (entity.assetType) {
                            "MODEL_3D" -> LibraryAssetType.MODEL_3D
                            "SPRITE_2D" -> LibraryAssetType.SPRITE_2D
                            else -> LibraryAssetType.UNIFIED_PAIR
                        }
                        val timeFormatted = remember(entity.createdAtMillis) {
                            SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
                                .format(Date(entity.createdAtMillis))
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssetPreviewCard(
                                title = entity.title,
                                assetTypeTag = mappedType.label,
                                metadataSummary = "${entity.metricsSummary} • $timeFormatted",
                                subtitleOrCharacterTag = entity.characterTag,
                                hardwareBadge = entity.hardwareTarget,
                                sourceBadge = "ROOM DB • ${entity.sourceOrigin}",
                                metadataTags = listOfNotNull(
                                    "Grid ${entity.spriteColumns}×${entity.spriteRows}",
                                    entity.aiReviewNotes
                                ),
                                meshPreview = reconstructedMesh,
                                bitmapPreview = reconstructedSprite?.frames?.firstOrNull() ?: reconstructedSprite?.sourceBitmap,
                                testTagPrefix = "room_history_card_${entity.id}",
                                primaryActionLabel = "Restore",
                                onPrimaryAction = { onRestoreEntity(entity) },
                                onDeleteAction = { onDeleteEntity(entity.id) },
                                onClick = { onRestoreEntity(entity) }
                            )
                        }
                    }
                }
            }
        }
    }
}
