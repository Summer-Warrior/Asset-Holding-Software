package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.data.local.AssetGalleryEntity
import com.example.data.local.AssetGalleryItemEntity
import com.example.data.local.AssetGalleryWithItems
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

private val GALLERY_THEME_TAGS = listOf(
    "CYBERPUNK",
    "CHARACTERS",
    "RETRO SILICON",
    "3D + 2D FUSIONS",
    "GEMINI AI",
    "SHOWCASE"
)

private val GALLERY_LAYOUT_STYLES = listOf(
    "GRID_2COL" to "2-Column Grid",
    "CAROUSEL" to "Showcase Carousel",
    "COMPACT_LIST" to "Compact List"
)

private val GALLERY_ACCENT_SWATCHES = listOf(
    "#00E5FF" to "Cyan Neon",
    "#FF2A85" to "Magenta Neon",
    "#10B981" to "Emerald",
    "#F59E0B" to "Amber Gold",
    "#A855F7" to "Ultraviolet"
)

private fun parseGalleryAccentColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        CyanNeon
    }
}

/**
 * Full-screen Asset Galleries & Exhibitions overlay accessible from the TopAppBar,
 * complete with system back navigation handling.
 */
@Composable
fun AssetGalleriesOverlayScreen(
    uiState: AppUiState,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("galleries_overlay_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Studio",
                        tint = CyanNeon
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        text = "ASSET GALLERIES",
                        fontFamily = TechMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                    Text(
                        text = "${uiState.assetGalleries.size} custom exhibitions • Persisted in Room",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("overlay_create_gallery_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Gallery", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        AssetGalleriesPanel(
            uiState = uiState,
            viewModel = viewModel
        )
    }

    if (showCreateDialog) {
        CreateOrEditGalleryDialog(
            existingGallery = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, desc, themeTag, accentHex, layoutStyle, seedActive ->
                viewModel.createNewAssetGallery(
                    name = name,
                    description = desc,
                    themeTag = themeTag,
                    accentColorHex = accentHex,
                    layoutStyle = layoutStyle,
                    seedWithCurrentStudioAsset = seedActive
                )
                showCreateDialog = false
            }
        )
    }
}

/**
 * Interactive Asset Galleries Manager & Exhibition Showcase Panel.
 * Allows creating brand-new Asset Galleries, curating active/library/history 3D & 2D assets into them,
 * switching showcase layouts (2-Column Grid / Carousel / Compact List), exporting manifests, and loading assets back into Studio.
 */
@Composable
fun AssetGalleriesPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val galleries = uiState.assetGalleries
    val selectedEntry = remember(galleries, uiState.selectedGalleryId) {
        galleries.firstOrNull { it.gallery.id == uiState.selectedGalleryId } ?: galleries.firstOrNull()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var galleryBeingEdited by remember { mutableStateOf<AssetGalleryEntity?>(null) }
    var showPickExistingDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1221)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_galleries_panel")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header + "Make New Gallery" CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CollectionsBookmark,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ASSET GALLERIES & EXHIBITIONS",
                            fontFamily = TechMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                        Text(
                            text = "${galleries.size} galleries • ${galleries.sumOf { it.items.size }} curated assets",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = Color(0xFF031B26)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("create_new_gallery_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "New Gallery",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Horizontal Gallery Selector Strip
            if (galleries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    galleries.forEach { entry ->
                        val g = entry.gallery
                        val isSelected = selectedEntry?.gallery?.id == g.id
                        val accent = parseGalleryAccentColor(g.accentColorHex)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accent.copy(alpha = 0.16f) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accent else CardBorder
                            ),
                            modifier = Modifier
                                .width(205.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectAssetGallery(g.id) }
                                .testTag("gallery_tab_card_${g.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = accent.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = g.themeTag,
                                            fontFamily = TechMono,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accent,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${entry.items.size} items",
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Text(
                                    text = g.name,
                                    fontFamily = TechMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = g.description,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Active Gallery Details & Exhibition Canvas
            if (selectedEntry != null) {
                val gallery = selectedEntry.gallery
                val accent = parseGalleryAccentColor(gallery.accentColorHex)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF060B15))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Gallery Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Text(
                                    text = gallery.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("active_gallery_title")
                                )
                            }
                            Text(
                                text = gallery.description,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 15.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { galleryBeingEdited = gallery },
                                modifier = Modifier.testTag("edit_active_gallery_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Gallery",
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.prepareGalleryManifestExport(gallery.id) },
                                modifier = Modifier.testTag("export_active_gallery_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Gallery Manifest",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteAssetGallery(gallery.id) },
                                modifier = Modifier.testTag("delete_active_gallery_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Gallery",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Layout Switcher Pills (Grid / Carousel / List)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Layout:",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        GALLERY_LAYOUT_STYLES.forEach { (styleKey, styleLabel) ->
                            val selected = gallery.layoutStyle == styleKey
                            val icon = when (styleKey) {
                                "GRID_2COL" -> Icons.Default.GridView
                                "CAROUSEL" -> Icons.Default.ViewCarousel
                                else -> Icons.AutoMirrored.Filled.ViewList
                            }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    viewModel.updateAssetGallery(
                                        galleryId = gallery.id,
                                        layoutStyle = styleKey
                                    )
                                },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text(styleLabel, fontFamily = TechMono, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = Color(0xFF031B26),
                                    selectedLeadingIconColor = Color(0xFF031B26),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White,
                                    iconColor = accent
                                ),
                                modifier = Modifier.testTag("gallery_layout_chip_${styleKey.lowercase()}")
                            )
                        }
                    }

                    // Quick Curation Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.addActiveStudioAssetToGallery(gallery.id, "MODEL_3D")
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                            modifier = Modifier.testTag("gallery_add_active_3d_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Active 3D", fontFamily = TechMono, fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.addActiveStudioAssetToGallery(gallery.id, "SPRITE_2D")
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon),
                            modifier = Modifier.testTag("gallery_add_active_sprite_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Active Sprite", fontFamily = TechMono, fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.addActiveStudioAssetToGallery(gallery.id, "UNIFIED_PAIR")
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                            modifier = Modifier.testTag("gallery_add_active_pair_btn")
                        ) {
                            Icon(Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add 3D+2D Pair", fontFamily = TechMono, fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { showPickExistingDialog = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                            modifier = Modifier.testTag("gallery_pick_library_history_btn")
                        ) {
                            Text("From Library / History", fontFamily = TechMono, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                val json = viewModel.prepareAssetIndexJsonExport(gallery.id)
                                if (json != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("asset-index.json", json))
                                    viewModel.postStatusMessage("Exported & copied asset-index.json for '${gallery.name}'!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color(0xFF031B26)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_asset_index_json_btn")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "Export asset-index.json", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export asset-index.json", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = CardBorder)

                    // Curated Assets Display according to Layout Style
                    if (selectedEntry.items.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0A111E))
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "THIS GALLERY IS READY FOR CURATION",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Text(
                                text = "Use the buttons above to add your active 3D model, 2D spritesheet, or pick assets from your Library and Room History.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Button(
                                onClick = {
                                    viewModel.addActiveStudioAssetToGallery(gallery.id, "UNIFIED_PAIR")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = Color(0xFF031B26)
                                ),
                                modifier = Modifier.testTag("empty_gallery_curate_now_btn")
                            ) {
                                Text("Add Current Studio Asset", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        when (gallery.layoutStyle) {
                            "CAROUSEL" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    selectedEntry.items.forEach { item ->
                                        GalleryItemCardWrapper(
                                            item = item,
                                            accentColor = accent,
                                            onLoadToStudio = { viewModel.loadGalleryItemIntoStudio(item) },
                                            onRemove = { viewModel.removeItemFromGallery(item.id) },
                                            modifier = Modifier.width(265.dp)
                                        )
                                    }
                                }
                            }
                            "COMPACT_LIST" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    selectedEntry.items.forEach { item ->
                                        GalleryItemCardWrapper(
                                            item = item,
                                            accentColor = accent,
                                            onLoadToStudio = { viewModel.loadGalleryItemIntoStudio(item) },
                                            onRemove = { viewModel.removeItemFromGallery(item.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            else -> {
                                // GRID_2COL
                                val rows = selectedEntry.items.chunked(2)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rows.forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { item ->
                                                GalleryItemCardWrapper(
                                                    item = item,
                                                    accentColor = accent,
                                                    onLoadToStudio = { viewModel.loadGalleryItemIntoStudio(item) },
                                                    onRemove = { viewModel.removeItemFromGallery(item.id) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // No galleries exist at all
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF060B15))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No Asset Galleries Created Yet",
                        fontFamily = TechMono,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26))
                    ) {
                        Text("Create Your First Asset Gallery", fontFamily = TechMono, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrEditGalleryDialog(
            existingGallery = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, desc, themeTag, accentHex, layoutStyle, seedActive ->
                viewModel.createNewAssetGallery(
                    name = name,
                    description = desc,
                    themeTag = themeTag,
                    accentColorHex = accentHex,
                    layoutStyle = layoutStyle,
                    seedWithCurrentStudioAsset = seedActive
                )
                showCreateDialog = false
            }
        )
    }

    galleryBeingEdited?.let { editing ->
        CreateOrEditGalleryDialog(
            existingGallery = editing,
            onDismiss = { galleryBeingEdited = null },
            onSave = { name, desc, themeTag, accentHex, layoutStyle, _ ->
                viewModel.updateAssetGallery(
                    galleryId = editing.id,
                    name = name,
                    description = desc,
                    themeTag = themeTag,
                    accentColorHex = accentHex,
                    layoutStyle = layoutStyle
                )
                galleryBeingEdited = null
            }
        )
    }

    if (showPickExistingDialog && selectedEntry != null) {
        PickExistingAssetForGalleryDialog(
            gallery = selectedEntry.gallery,
            uiState = uiState,
            onDismiss = { showPickExistingDialog = false },
            onPickLibraryItem = { libItem ->
                viewModel.addLibraryAssetToGallery(selectedEntry.gallery.id, libItem)
                showPickExistingDialog = false
            },
            onPickHistoryItem = { histItem ->
                viewModel.addHistoryEntityToGallery(selectedEntry.gallery.id, histItem)
                showPickExistingDialog = false
            }
        )
    }
}

@Composable
private fun GalleryItemCardWrapper(
    item: AssetGalleryItemEntity,
    accentColor: Color,
    onLoadToStudio: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val decodedMesh = remember(item.id, item.objMeshData, item.meshFilePath) { item.toMesh3D(context) }
    val decodedSprite = remember(item.id, item.spritePngBytes) { item.toSpriteSheetData() }
    val previewBmp = decodedSprite?.frames?.firstOrNull() ?: decodedSprite?.sourceBitmap
    val extraTags = buildList {
        if (item.curatorNote.isNotBlank()) add(item.curatorNote)
    }

    AssetPreviewCard(
        title = item.title,
        assetTypeTag = item.assetType,
        metadataSummary = item.metricsSummary,
        subtitleOrCharacterTag = item.characterTag,
        hardwareBadge = item.hardwareTarget,
        sourceBadge = item.sourceOrigin,
        metadataTags = extraTags,
        meshPreview = decodedMesh,
        bitmapPreview = previewBmp,
        accentColor = accentColor,
        testTagPrefix = "gallery_item_card_${item.id}",
        primaryActionLabel = "Load to Studio",
        onPrimaryAction = onLoadToStudio,
        onDeleteAction = onRemove,
        onClick = onLoadToStudio,
        modifier = modifier
    )
}

/**
 * Reusable compact bar dropped into Model, Sprite, and Forge Studios to add the active asset to an existing gallery
 * or spin up a brand-new Asset Gallery on the spot.
 */
@Composable
fun QuickAddToGalleryBar(
    uiState: AppUiState,
    viewModel: MainViewModel,
    assetScope: String,
    label: String = "Pin to Asset Gallery",
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val activeGallery = uiState.assetGalleries.firstOrNull { it.gallery.id == uiState.selectedGalleryId }
        ?: uiState.assetGalleries.firstOrNull()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = {
                if (activeGallery != null) {
                    viewModel.addActiveStudioAssetToGallery(activeGallery.gallery.id, assetScope)
                } else {
                    showCreateDialog = true
                }
            },
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("quick_add_${assetScope.lowercase()}_to_gallery_btn")
        ) {
            Icon(Icons.Default.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (activeGallery != null) "$label (${activeGallery.gallery.name})" else label,
                fontFamily = TechMono,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedButton(
            onClick = { showCreateDialog = true },
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("quick_new_gallery_${assetScope.lowercase()}_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create New Gallery", modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("New Gallery", fontFamily = TechMono, fontSize = 10.sp)
        }
    }

    if (showCreateDialog) {
        CreateOrEditGalleryDialog(
            existingGallery = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, desc, themeTag, accentHex, layoutStyle, seedActive ->
                viewModel.createNewAssetGallery(
                    name = name,
                    description = desc,
                    themeTag = themeTag,
                    accentColorHex = accentHex,
                    layoutStyle = layoutStyle,
                    seedWithCurrentStudioAsset = seedActive
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateOrEditGalleryDialog(
    existingGallery: AssetGalleryEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String,
        themeTag: String,
        accentColorHex: String,
        layoutStyle: String,
        seedWithActiveAsset: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingGallery?.name ?: "") }
    var description by remember { mutableStateOf(existingGallery?.description ?: "") }
    var themeTag by remember { mutableStateOf(existingGallery?.themeTag ?: GALLERY_THEME_TAGS.first()) }
    var accentHex by remember { mutableStateOf(existingGallery?.accentColorHex ?: "#00E5FF") }
    var layoutStyle by remember { mutableStateOf(existingGallery?.layoutStyle ?: "GRID_2COL") }
    var seedActive by remember { mutableStateOf(existingGallery == null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CollectionsBookmark, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (existingGallery == null) "Make New Asset Gallery" else "Edit Asset Gallery",
                    fontFamily = TechMono,
                    fontSize = 15.sp,
                    color = CyanNeon
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Gallery Name", fontFamily = TechMono, fontSize = 11.sp) },
                    placeholder = { Text("e.g., Cyber Boss Collection, GBA Portolio", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gallery_name_input"),
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Gallery Description", fontFamily = TechMono, fontSize = 11.sp) },
                    placeholder = { Text("Curated models, sprites, and hardware target showcases...", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .testTag("gallery_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = CyanNeon,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                Text("Theme Tag:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GALLERY_THEME_TAGS.forEach { tag ->
                        FilterChip(
                            selected = themeTag == tag,
                            onClick = { themeTag = tag },
                            label = { Text(tag, fontFamily = TechMono, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF031B26),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Text("Exhibition Layout:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GALLERY_LAYOUT_STYLES.forEach { (key, label) ->
                        FilterChip(
                            selected = layoutStyle == key,
                            onClick = { layoutStyle = key },
                            label = { Text(label, fontFamily = TechMono, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MagentaNeon,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Text("Accent Color:", fontFamily = TechMono, fontSize = 10.sp, color = Color(0xFF94A3B8))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GALLERY_ACCENT_SWATCHES.forEach { (hex, _) ->
                        val color = parseGalleryAccentColor(hex)
                        val selected = accentHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color.White else Color(0xFF334155),
                                    shape = CircleShape
                                )
                                .clickable { accentHex = hex }
                        )
                    }
                }

                if (existingGallery == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seed with active Studio 3D+2D asset",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = seedActive,
                            onCheckedChange = { seedActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreen
                            ),
                            modifier = Modifier.testTag("gallery_seed_active_switch")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.ifBlank { "Custom ${themeTag.lowercase().replaceFirstChar { it.uppercase() }} Gallery" },
                        description,
                        themeTag,
                        accentHex,
                        layoutStyle,
                        seedActive
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                modifier = Modifier.testTag("confirm_create_gallery_btn")
            ) {
                Text(
                    text = if (existingGallery == null) "Create Gallery" else "Save Changes",
                    fontFamily = TechMono,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A)
    )
}

@Composable
private fun PickExistingAssetForGalleryDialog(
    gallery: AssetGalleryEntity,
    uiState: AppUiState,
    onDismiss: () -> Unit,
    onPickLibraryItem: (com.example.model.library.SavedLibraryAsset) -> Unit,
    onPickHistoryItem: (com.example.data.local.AssetHistoryEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Existing Asset to '${gallery.name}'",
                fontFamily = TechMono,
                fontSize = 13.sp,
                color = CyanNeon
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.savedLibraryAssets.isNotEmpty()) {
                    Text(
                        text = "SAVED ASSET LIBRARY (${uiState.savedLibraryAssets.size})",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                    uiState.savedLibraryAssets.take(10).forEach { libItem ->
                        Surface(
                            color = Color(0xFF080E1A),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPickLibraryItem(libItem) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = libItem.title,
                                        fontFamily = TechMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${libItem.type.label} • ${libItem.metricsSummary}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = CyanNeon)
                            }
                        }
                    }
                }

                if (uiState.roomAssetHistory.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ROOM ASSET HISTORY VAULT (${uiState.roomAssetHistory.size})",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                    uiState.roomAssetHistory.take(10).forEach { histItem ->
                        Surface(
                            color = Color(0xFF080E1A),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPickHistoryItem(histItem) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = histItem.title,
                                        fontFamily = TechMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${histItem.assetType} • ${histItem.metricsSummary}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = EmeraldGreen)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanNeon, fontFamily = TechMono)
            }
        },
        containerColor = Color(0xFF0F172A)
    )
}
