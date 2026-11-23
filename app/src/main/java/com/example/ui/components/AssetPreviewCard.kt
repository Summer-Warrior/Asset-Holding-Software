package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.library.LibraryAssetType
import com.example.model.library.SavedLibraryAsset
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.Vector3
import com.example.model.sprite.SpriteSheetData
import com.example.model.suite.CharacterAssetModality
import com.example.model.suite.StagedCharacterAsset
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Reusable Composable Card component that displays:
 * 1. An interactive/visual Asset Preview (3D Mesh projection, 2D Sprite/Bitmap checkerboard stage, or Dual 3D+2D split preview)
 * 2. A prominent, color-coded Tag for Asset Type (e.g., "3D MODEL", "2D SPRITE", "3D + SPRITE PAIR", "UV TEXTURE", "CONCEPT ART")
 * 3. Rich Asset Metadata (Title, Character Tag, Hardware Target, Geometry/Resolution Metrics, Source Badge, and Unbounded Readability pills)
 */
@Composable
fun AssetPreviewCard(
    title: String,
    assetTypeTag: String,
    metadataSummary: String,
    modifier: Modifier = Modifier,
    subtitleOrCharacterTag: String? = null,
    hardwareBadge: String? = null,
    sourceBadge: String? = null,
    metadataTags: List<String> = emptyList(),
    meshPreview: Mesh3D? = null,
    bitmapPreview: Bitmap? = null,
    previewYawDegrees: Float = 40f,
    previewPitchDegrees: Float = 20f,
    accentColor: Color = resolveAssetTypeAccent(assetTypeTag),
    isSelected: Boolean = false,
    testTagPrefix: String = "asset_preview_card",
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    onDeleteAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTagPrefix)
    } else {
        modifier.testTag(testTagPrefix)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF0F1B2E) else Color(0xFF080E1A)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else accentColor.copy(alpha = 0.42f)
        ),
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Asset Type Tag + Source Badge + Optional Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Asset Type Tag Pill
                    Surface(
                        color = accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("${testTagPrefix}_type_tag")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = resolveAssetTypeIcon(assetTypeTag),
                                contentDescription = assetTypeTag,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = assetTypeTag.uppercase(),
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                maxLines = 1
                            )
                        }
                    }

                    // Source Badge (FILE / PHOTO / STUDIO / UNBOUNDED)
                    if (!sourceBadge.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.testTag("${testTagPrefix}_source_badge")
                        ) {
                            Text(
                                text = sourceBadge.uppercase(),
                                fontFamily = TechMono,
                                fontSize = 8.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                if (onDeleteAction != null) {
                    IconButton(
                        onClick = onDeleteAction,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("${testTagPrefix}_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Asset",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Visual Asset Preview Stage + Title/Metadata Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Miniature Visual Preview Box (Supports 3D Mesh, 2D Bitmap, or Dual Split)
                AssetThumbnailViewport(
                    mesh = meshPreview,
                    bitmap = bitmapPreview,
                    yawDegrees = previewYawDegrees,
                    pitchDegrees = previewPitchDegrees,
                    accentColor = accentColor,
                    contentDescription = title,
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("${testTagPrefix}_preview_box")
                )

                // Title & Primary Metadata Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = TechMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("${testTagPrefix}_title")
                    )

                    if (!subtitleOrCharacterTag.isNullOrBlank() || !hardwareBadge.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!subtitleOrCharacterTag.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = subtitleOrCharacterTag,
                                        fontSize = 10.sp,
                                        color = AmberGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (!hardwareBadge.isNullOrBlank()) {
                                Surface(
                                    color = AmberGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeveloperBoard,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = hardwareBadge,
                                            fontFamily = TechMono,
                                            fontSize = 8.sp,
                                            color = AmberGold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = metadataSummary,
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("${testTagPrefix}_metadata")
                    )
                }
            }

            // Optional Metadata Chips Row
            if (metadataTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    metadataTags.forEach { tagText ->
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(5.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = tagText,
                                fontFamily = TechMono,
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Optional Primary Action Button
            if (primaryActionLabel != null && onPrimaryAction != null) {
                OutlinedButton(
                    onClick = onPrimaryAction,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("${testTagPrefix}_primary_action")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdded,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = primaryActionLabel,
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Convenience overload for rendering a [SavedLibraryAsset] using [AssetPreviewCard].
 */
@Composable
fun AssetPreviewCard(
    item: SavedLibraryAsset,
    onLoad: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val thumb = item.spriteSheet?.frames?.firstOrNull() ?: item.textureBitmap
    val extraTags = buildList {
        if (item.formatSummary.isNotBlank()) add(item.formatSummary)
        item.mesh?.let { add("${it.rawVertexCount}v / ${it.rawFaceCount}f") }
        item.spriteSheet?.let { add("${it.originalWidth}×${it.originalHeight}px (${it.columns}×${it.rows})") }
    }.distinct()

    AssetPreviewCard(
        title = item.title,
        assetTypeTag = item.type.label,
        metadataSummary = item.metricsSummary,
        subtitleOrCharacterTag = item.characterTag,
        hardwareBadge = item.hardwareBadge,
        sourceBadge = item.sourceBadge,
        metadataTags = extraTags,
        meshPreview = item.mesh,
        bitmapPreview = thumb,
        previewYawDegrees = item.inspectedYawDegrees,
        previewPitchDegrees = item.inspectedPitchDegrees,
        testTagPrefix = "library_item_card_${item.id}",
        primaryActionLabel = "Load into Studio",
        onPrimaryAction = onLoad,
        onDeleteAction = onDelete,
        onClick = onLoad,
        modifier = modifier
    )
}

/**
 * Convenience overload for rendering an active or imported 3D [Mesh3D] or 2D [SpriteSheetData] studio asset.
 */
@Composable
fun StudioAssetSummaryCard(
    title: String,
    assetTypeTag: String,
    characterTag: String,
    hardwareBadge: String,
    sourceBadge: String,
    metadataSummary: String,
    metadataTags: List<String>,
    mesh: Mesh3D? = null,
    spriteSheet: SpriteSheetData? = null,
    textureBitmap: Bitmap? = null,
    testTagPrefix: String = "studio_asset_card",
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bmp = spriteSheet?.frames?.firstOrNull() ?: textureBitmap
    AssetPreviewCard(
        title = title,
        assetTypeTag = assetTypeTag,
        metadataSummary = metadataSummary,
        subtitleOrCharacterTag = characterTag,
        hardwareBadge = hardwareBadge,
        sourceBadge = sourceBadge,
        metadataTags = metadataTags,
        meshPreview = mesh,
        bitmapPreview = bmp,
        testTagPrefix = testTagPrefix,
        primaryActionLabel = primaryActionLabel,
        onPrimaryAction = onPrimaryAction,
        modifier = modifier
    )
}

/**
 * Convenience overload for rendering a [StagedCharacterAsset] from the Named Character Auto-Fusion pool.
 */
@Composable
fun AssetPreviewCard(
    stagedAsset: StagedCharacterAsset,
    hardwareBadge: String = "STUDIO",
    onSelectOrFuse: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val thumb = stagedAsset.spriteSheet?.frames?.firstOrNull() ?: stagedAsset.bitmap
    val metrics = when (stagedAsset.modality) {
        CharacterAssetModality.MESH_3D -> {
            val m = stagedAsset.mesh
            if (m != null) "${m.rawVertexCount}v • ${m.rawFaceCount}f (${m.sourceFormat})" else "3D Geometry Asset"
        }
        CharacterAssetModality.SPRITESHEET_2D -> {
            val s = stagedAsset.spriteSheet
            if (s != null) "${s.frames.size} frames (${s.originalWidth}×${s.originalHeight}px)" else "2D Spritesheet"
        }
        CharacterAssetModality.TEXTURE_UV -> {
            val b = stagedAsset.bitmap
            if (b != null) "UV Map ${b.width}×${b.height}px" else "UV Texture Surface"
        }
        CharacterAssetModality.CONCEPT_ART -> {
            val b = stagedAsset.bitmap
            if (b != null) "Concept ${b.width}×${b.height}px" else "Concept Art Reference"
        }
        CharacterAssetModality.LORE_SPEC -> {
            stagedAsset.loreText?.take(48) ?: "Character Lore Specification"
        }
    }

    val sourceBadge = when {
        stagedAsset.mesh != null -> stagedAsset.mesh.sourceKindLabel
        stagedAsset.spriteSheet != null -> stagedAsset.spriteSheet.sourceKindLabel
        stagedAsset.isUserImported -> "IMPORTED"
        else -> "STUDIO"
    }

    AssetPreviewCard(
        title = stagedAsset.assetTitle,
        assetTypeTag = stagedAsset.modality.label,
        metadataSummary = metrics,
        subtitleOrCharacterTag = stagedAsset.characterName,
        hardwareBadge = hardwareBadge,
        sourceBadge = sourceBadge,
        meshPreview = stagedAsset.mesh,
        bitmapPreview = thumb,
        testTagPrefix = "staged_asset_card_${stagedAsset.id}",
        primaryActionLabel = if (onSelectOrFuse != null) "Inspect / Fuse" else null,
        onPrimaryAction = onSelectOrFuse,
        onClick = onSelectOrFuse,
        modifier = modifier
    )
}

@Composable
private fun AssetThumbnailViewport(
    mesh: Mesh3D?,
    bitmap: Bitmap?,
    yawDegrees: Float,
    pitchDegrees: Float,
    accentColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF060A12))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Checkerboard backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 10.dp.toPx()
            val cols = (size.width / step).toInt() + 1
            val rows = (size.height / step).toInt() + 1
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if ((r + c) % 2 == 0) {
                        drawRect(
                            color = Color(0xFF0D1526),
                            topLeft = Offset(c * step, r * step),
                            size = Size(step, step)
                        )
                    }
                }
            }
        }

        if (mesh != null && bitmap != null) {
            // Dual 3D + 2D Split Preview
            Row(modifier = Modifier.fillMaxSize()) {
                MiniMeshCanvas(
                    mesh = mesh,
                    yawDegrees = yawDegrees,
                    pitchDegrees = pitchDegrees,
                    accentColor = CyanNeon,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(CardBorder)
                )
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    filterQuality = FilterQuality.None,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(3.dp)
                )
            }
        } else if (mesh != null && mesh.vertices.isNotEmpty()) {
            MiniMeshCanvas(
                mesh = mesh,
                yawDegrees = yawDegrees,
                pitchDegrees = pitchDegrees,
                accentColor = accentColor,
                modifier = Modifier.fillMaxSize()
            )
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.ViewInAr,
                contentDescription = contentDescription,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun MiniMeshCanvas(
    mesh: Mesh3D,
    yawDegrees: Float,
    pitchDegrees: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (mesh.vertices.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val scale = minOf(w, h) * 0.38f

        val yawRad = (yawDegrees * PI / 180.0).toFloat()
        val pitchRad = (pitchDegrees * PI / 180.0).toFloat()
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)

        val rotated = ArrayList<Vector3>(mesh.vertices.size)
        for (v in mesh.vertices) {
            val x1 = v.x * cosY + v.z * sinY
            val z1 = -v.x * sinY + v.z * cosY
            val y2 = v.y * cosP - z1 * sinP
            val z2 = v.y * sinP + z1 * cosP
            rotated.add(Vector3(x1, y2, z2))
        }

        val stride = (mesh.faces.size / 180).coerceAtLeast(1)
        val lightDir = Vector3(0.5f, 0.7f, 0.5f).normalize()
        val path = Path()

        var idx = 0
        while (idx < mesh.faces.size) {
            val face = mesh.faces[idx]
            if (face.indices.size >= 3) {
                val i0 = face.indices[0]
                val i1 = face.indices[1]
                val i2 = face.indices[2]
                if (i0 in rotated.indices && i1 in rotated.indices && i2 in rotated.indices) {
                    val v0 = rotated[i0]
                    val v1 = rotated[i1]
                    val v2 = rotated[i2]
                    val norm = (v1 - v0).cross(v2 - v0).normalize()
                    if (norm.z > -0.2f) {
                        val diffuse = max(0.3f, norm.dot(lightDir))
                        path.reset()
                        path.moveTo(cx + v0.x * scale, cy - v0.y * scale)
                        path.lineTo(cx + v1.x * scale, cy - v1.y * scale)
                        path.lineTo(cx + v2.x * scale, cy - v2.y * scale)
                        path.close()
                        drawPath(path, accentColor.copy(alpha = (0.35f * diffuse).coerceIn(0.15f, 0.6f)), style = Fill)
                        drawPath(path, accentColor.copy(alpha = 0.85f), style = Stroke(width = 1f))
                    }
                }
            }
            idx += stride
        }
    }
}

private fun resolveAssetTypeAccent(assetTypeTag: String): Color {
    val upper = assetTypeTag.uppercase()
    return when {
        upper.contains("PAIR") || upper.contains("SUITE") || upper.contains("DUAL") -> EmeraldGreen
        upper.contains("3D") || upper.contains("MESH") || upper.contains("MODEL") -> CyanNeon
        upper.contains("SPRITE") || upper.contains("2D") -> MagentaNeon
        upper.contains("TEXTURE") || upper.contains("UV") -> EmeraldGreen
        else -> AmberGold
    }
}

private fun resolveAssetTypeIcon(assetTypeTag: String): ImageVector {
    val upper = assetTypeTag.uppercase()
    return when {
        upper.contains("PAIR") || upper.contains("SUITE") -> Icons.Default.Layers
        upper.contains("3D") || upper.contains("MESH") || upper.contains("MODEL") -> Icons.Default.ViewInAr
        upper.contains("SPRITE") || upper.contains("2D") -> Icons.Default.GridOn
        upper.contains("TEXTURE") || upper.contains("UV") -> Icons.Default.Palette
        upper.contains("PHOTO") || upper.contains("CONCEPT") -> Icons.Default.Image
        else -> Icons.Default.AutoAwesome
    }
}
