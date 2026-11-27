package com.example.ui.components

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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.data.local.AssetGalleryItemEntity
import com.example.data.local.AssetHistoryEntity
import com.example.model.library.SavedLibraryAsset
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

private enum class VaultRecordSource(val badge: String) {
    GALLERY("GALLERY"),
    HISTORY("HISTORY"),
    LIBRARY("LIBRARY")
}

private data class UnifiedHierarchyAsset(
    val uid: String,
    val title: String,
    val assetType: String,
    val gameTag: String,
    val groupTag: String,
    val characterTag: String,
    val hardwareTarget: String,
    val metricsSummary: String,
    val source: VaultRecordSource,
    val galleryItem: AssetGalleryItemEntity? = null,
    val historyEntity: AssetHistoryEntity? = null,
    val libraryAsset: SavedLibraryAsset? = null
)

/**
 * Clean 3-level drill-down explorer (`Game -> Group -> Character`) across Galleries, Room History,
 * and Saved Library assets, with search filtering, live asset counts, ecosystem presets,
 * load-to-studio, and 1-tap tag reassignment.
 */
@Composable
fun GameGroupCharacterBrowserPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGameFilter by remember { mutableStateOf<String?>(null) }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    var selectedCharacterFilter by remember { mutableStateOf<String?>(null) }
    var editingAsset by remember { mutableStateOf<UnifiedHierarchyAsset?>(null) }

    val allRecords = remember(
        uiState.assetGalleries,
        uiState.roomAssetHistory,
        uiState.savedLibraryAssets
    ) {
        buildList {
            uiState.assetGalleries.forEach { galleryWithItems ->
                galleryWithItems.items.forEach { item ->
                    add(
                        UnifiedHierarchyAsset(
                            uid = "gal_${item.id}",
                            title = item.title,
                            assetType = item.assetType,
                            gameTag = item.gameTag.ifBlank { "General Studio" },
                            groupTag = item.groupTag.ifBlank { "Core Roster" },
                            characterTag = item.characterTag.ifBlank { "Studio Asset" },
                            hardwareTarget = item.hardwareTarget,
                            metricsSummary = item.metricsSummary,
                            source = VaultRecordSource.GALLERY,
                            galleryItem = item
                        )
                    )
                }
            }
            uiState.roomAssetHistory.forEach { hist ->
                add(
                    UnifiedHierarchyAsset(
                        uid = "hist_${hist.id}",
                        title = hist.title,
                        assetType = hist.assetType,
                        gameTag = hist.gameTag.ifBlank { "General Studio" },
                        groupTag = hist.groupTag.ifBlank { "Core Roster" },
                        characterTag = hist.characterTag.ifBlank { "Studio Asset" },
                        hardwareTarget = hist.hardwareTarget,
                        metricsSummary = hist.metricsSummary,
                        source = VaultRecordSource.HISTORY,
                        historyEntity = hist
                    )
                )
            }
            uiState.savedLibraryAssets.forEach { lib ->
                add(
                    UnifiedHierarchyAsset(
                        uid = "lib_${lib.id}",
                        title = lib.title,
                        assetType = lib.type.name,
                        gameTag = lib.gameTag.ifBlank { "General Studio" },
                        groupTag = lib.groupTag.ifBlank { "Core Roster" },
                        characterTag = lib.characterTag.ifBlank { "Studio Asset" },
                        hardwareTarget = lib.hardwareBadge,
                        metricsSummary = lib.metricsSummary,
                        source = VaultRecordSource.LIBRARY,
                        libraryAsset = lib
                    )
                )
            }
        }
    }

    val queryLower = searchQuery.trim().lowercase()
    val searchedRecords = remember(allRecords, queryLower) {
        if (queryLower.isEmpty()) {
            allRecords
        } else {
            allRecords.filter { rec ->
                rec.title.lowercase().contains(queryLower) ||
                    rec.gameTag.lowercase().contains(queryLower) ||
                    rec.groupTag.lowercase().contains(queryLower) ||
                    rec.characterTag.lowercase().contains(queryLower) ||
                    rec.assetType.lowercase().contains(queryLower)
            }
        }
    }

    val gameCounts = remember(searchedRecords) {
        searchedRecords.groupingBy { it.gameTag }.eachCount()
    }
    val orderedGames = remember(gameCounts) {
        (MainViewModel.ECOSYSTEM_GAME_PRESETS + gameCounts.keys).distinct()
            .filter { game -> gameCounts.containsKey(game) || game == uiState.activeGameTagForImports }
    }

    val recordsInGame = remember(searchedRecords, selectedGameFilter) {
        if (selectedGameFilter == null) searchedRecords
        else searchedRecords.filter { it.gameTag.equals(selectedGameFilter, ignoreCase = true) }
    }
    val groupCounts = remember(recordsInGame) {
        recordsInGame.groupingBy { it.groupTag }.eachCount()
    }
    val orderedGroups = remember(groupCounts) {
        (MainViewModel.ECOSYSTEM_GROUP_PRESETS + groupCounts.keys).distinct()
            .filter { grp -> groupCounts.containsKey(grp) || grp == uiState.activeGroupTagForImports }
    }

    val recordsInGroup = remember(recordsInGame, selectedGroupFilter) {
        if (selectedGroupFilter == null) recordsInGame
        else recordsInGame.filter { it.groupTag.equals(selectedGroupFilter, ignoreCase = true) }
    }
    val characterCounts = remember(recordsInGroup) {
        recordsInGroup.groupingBy { it.characterTag }.eachCount()
            .entries.sortedByDescending { it.value }
    }

    val visibleAssets = remember(recordsInGroup, selectedCharacterFilter) {
        if (selectedCharacterFilter == null) recordsInGroup
        else recordsInGroup.filter { it.characterTag.equals(selectedCharacterFilter, ignoreCase = true) }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1222)),
        border = BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_group_character_browser_panel")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = "Game Group Character Hierarchy",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "GAME -> GROUP -> CHARACTER EXPLORER",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "${allRecords.size} vault assets • Active target: ${uiState.activeGameTagForImports} / ${uiState.activeGroupTagForImports}",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${visibleAssets.size} SHOWN",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Active Import Target Presets (Game & Group quick-select)
            Text(
                text = "ACTIVE IMPORT / GENERATION GAME PRESET:",
                fontFamily = TechMono,
                fontSize = 9.sp,
                color = CyanNeon
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MainViewModel.ECOSYSTEM_GAME_PRESETS.forEach { presetGame ->
                    val isActive = uiState.activeGameTagForImports.equals(presetGame, ignoreCase = true)
                    FilterChip(
                        selected = isActive,
                        onClick = { viewModel.setActiveGameTagForImports(presetGame) },
                        label = { Text(presetGame, fontFamily = TechMono, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon,
                            selectedLabelColor = Color(0xFF031B26),
                            containerColor = Color(0xFF162135),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("active_game_preset_${presetGame.lowercase().replace(Regex("[^a-z0-9]+"), "_")}")
                    )
                }
            }

            Text(
                text = "ACTIVE IMPORT / GENERATION GROUP PRESET:",
                fontFamily = TechMono,
                fontSize = 9.sp,
                color = AmberGold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MainViewModel.ECOSYSTEM_GROUP_PRESETS.forEach { presetGroup ->
                    val isActive = uiState.activeGroupTagForImports.equals(presetGroup, ignoreCase = true)
                    FilterChip(
                        selected = isActive,
                        onClick = { viewModel.setActiveGroupTagForImports(presetGroup) },
                        label = { Text(presetGroup, fontFamily = TechMono, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGold,
                            selectedLabelColor = Color(0xFF1E1300),
                            containerColor = Color(0xFF162135),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("active_group_preset_${presetGroup.lowercase().replace(Regex("[^a-z0-9]+"), "_")}")
                    )
                }
            }

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search game, group, character, or asset title...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search hierarchy", tint = CyanNeon) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hierarchy_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // LEVEL 1: GAME DRILL-DOWN
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                Text("1. GAME (${orderedGames.size})", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedGameFilter == null,
                    onClick = {
                        selectedGameFilter = null
                        selectedGroupFilter = null
                        selectedCharacterFilter = null
                    },
                    label = { Text("All Games (${searchedRecords.size})", fontFamily = TechMono, fontSize = 9.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreen,
                        selectedLabelColor = Color(0xFF022015),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.testTag("hierarchy_game_all_chip")
                )
                orderedGames.forEach { game ->
                    val count = gameCounts[game] ?: 0
                    val selected = selectedGameFilter.equals(game, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedGameFilter = if (selected) null else game
                            selectedGroupFilter = null
                            selectedCharacterFilter = null
                        },
                        label = { Text("$game ($count)", fontFamily = TechMono, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon,
                            selectedLabelColor = Color(0xFF031B26),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // LEVEL 2: GROUP DRILL-DOWN
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                Text("2. GROUP (${orderedGroups.size})", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberGold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedGroupFilter == null,
                    onClick = {
                        selectedGroupFilter = null
                        selectedCharacterFilter = null
                    },
                    label = { Text("All Groups (${recordsInGame.size})", fontFamily = TechMono, fontSize = 9.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberGold,
                        selectedLabelColor = Color(0xFF1E1300),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.testTag("hierarchy_group_all_chip")
                )
                orderedGroups.forEach { grp ->
                    val count = groupCounts[grp] ?: 0
                    val selected = selectedGroupFilter.equals(grp, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedGroupFilter = if (selected) null else grp
                            selectedCharacterFilter = null
                        },
                        label = { Text("$grp ($count)", fontFamily = TechMono, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGold,
                            selectedLabelColor = Color(0xFF1E1300),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // LEVEL 3: CHARACTER DRILL-DOWN
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MagentaNeon, modifier = Modifier.size(14.dp))
                Text("3. CHARACTER (${characterCounts.size})", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MagentaNeon)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCharacterFilter == null,
                    onClick = { selectedCharacterFilter = null },
                    label = { Text("All Characters (${recordsInGroup.size})", fontFamily = TechMono, fontSize = 9.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MagentaNeon,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.testTag("hierarchy_char_all_chip")
                )
                characterCounts.forEach { (charName, count) ->
                    val selected = selectedCharacterFilter.equals(charName, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCharacterFilter = if (selected) null else charName },
                        label = { Text("$charName ($count)", fontFamily = TechMono, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MagentaNeon,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // Filtered Asset List (capped at 12 visible rows for snappy scroll)
            if (visibleAssets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF060B16))
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No assets match this Game / Group / Character path yet. Save or curate an asset to populate it.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleAssets.take(12).forEach { asset ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF07101F))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = asset.title,
                                        fontFamily = TechMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${asset.gameTag}  ›  ${asset.groupTag}  ›  ${asset.characterTag}",
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = EmeraldGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = CyanNeon.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = asset.source.badge,
                                        fontFamily = TechMono,
                                        fontSize = 8.sp,
                                        color = CyanNeon,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editingAsset = asset },
                                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("reassign_tags_btn_${asset.uid}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reassign Tags", fontFamily = TechMono, fontSize = 9.sp)
                                }

                                Button(
                                    onClick = {
                                        when {
                                            asset.galleryItem != null -> viewModel.loadGalleryItemIntoStudio(asset.galleryItem)
                                            asset.historyEntity != null -> viewModel.restoreAssetFromRoomHistory(asset.historyEntity)
                                            asset.libraryAsset != null -> viewModel.loadLibraryAssetIntoStudio(asset.libraryAsset)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("load_hierarchy_asset_btn_${asset.uid}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Load", fontFamily = TechMono, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (visibleAssets.size > 12) {
                        Text(
                            text = "+ ${visibleAssets.size - 12} more matching asset(s) — refine search or pick a Group/Character chip to narrow down.",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }

    // 1-Tap Tag Reassignment Dialog
    val target = editingAsset
    if (target != null) {
        ReassignHierarchyTagsDialog(
            assetTitle = target.title,
            initialGame = target.gameTag,
            initialGroup = target.groupTag,
            initialCharacter = target.characterTag,
            onDismiss = { editingAsset = null },
            onSave = { newGame, newGroup, newChar ->
                when {
                    target.galleryItem != null -> viewModel.reassignGalleryItemTags(
                        itemId = target.galleryItem.id,
                        gameTag = newGame,
                        groupTag = newGroup,
                        characterTag = newChar
                    )
                    target.historyEntity != null -> viewModel.reassignHistoryItemTags(
                        id = target.historyEntity.id,
                        gameTag = newGame,
                        groupTag = newGroup,
                        characterTag = newChar
                    )
                    target.libraryAsset != null -> viewModel.reassignLibraryAssetTags(
                        id = target.libraryAsset.id,
                        gameTag = newGame,
                        groupTag = newGroup,
                        characterTag = newChar
                    )
                }
                editingAsset = null
            }
        )
    }
}

@Composable
private fun ReassignHierarchyTagsDialog(
    assetTitle: String,
    initialGame: String,
    initialGroup: String,
    initialCharacter: String,
    onDismiss: () -> Unit,
    onSave: (gameTag: String, groupTag: String, characterTag: String) -> Unit
) {
    var gameInput by remember { mutableStateOf(initialGame) }
    var groupInput by remember { mutableStateOf(initialGroup) }
    var charInput by remember { mutableStateOf(initialCharacter) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text(
                text = "REASSIGN GAME / GROUP / CHARACTER",
                fontFamily = TechMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = assetTitle,
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = gameInput,
                    onValueChange = { gameInput = it },
                    label = { Text("Game Tag", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MainViewModel.ECOSYSTEM_GAME_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = gameInput.equals(preset, ignoreCase = true),
                            onClick = { gameInput = preset },
                            label = { Text(preset, fontFamily = TechMono, fontSize = 9.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = groupInput,
                    onValueChange = { groupInput = it },
                    label = { Text("Group Tag", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MainViewModel.ECOSYSTEM_GROUP_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = groupInput.equals(preset, ignoreCase = true),
                            onClick = { groupInput = preset },
                            label = { Text(preset, fontFamily = TechMono, fontSize = 9.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = charInput,
                    onValueChange = { charInput = it },
                    label = { Text("Character Tag", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(gameInput, groupInput, charInput) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color(0xFF022015))
            ) {
                Text("Save Tags", fontFamily = TechMono, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = TechMono, color = Color(0xFF94A3B8))
            }
        }
    )
}
