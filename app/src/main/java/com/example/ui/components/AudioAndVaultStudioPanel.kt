package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.audio.AudioAssetEntry
import com.example.model.audio.SoundchipDspProfile
import com.example.model.vault.ImportPipelineStage
import com.example.model.vault.VaultCacheState
import com.example.model.vault.VaultCatalogEntry
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import java.util.Locale

private val ObsidianSurface = Color(0xFF0A1120)
private val ObsidianCard = Color(0xFF0F172A)
private val MutedSlate = Color(0xFF94A3B8)

/**
 * Hardware-Foundry Obsidian studio panel uniting:
 * - Tab 0: First-Class Audio Asset Vault & 8-Profile Soundchip DSP Split with live Canvas waveform preview
 * - Tab 1: 6-Stage SHA-256 Importer, AES-256-GCM Cold Vault (`.vault.enc`), 200 MB LRU Hot Phone Cache,
 *          PIN Lock, and optional Cloudflare R2 / Local Cold Vault configuration.
 */
@Composable
fun AudioAndVaultStudioPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    openCreateTabTrigger: Int = 0
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }
    var importSlotName by remember { mutableStateOf("") }

    LaunchedEffect(openCreateTabTrigger) {
        if (openCreateTabTrigger > 0) {
            activeTab = 2
        }
    }
    var markSensitive by remember { mutableStateOf(false) }
    var vaultPassphrase by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var r2EndpointInput by remember(uiState.coldVaultEndpointConfig.r2EndpointUrl) {
        mutableStateOf(uiState.coldVaultEndpointConfig.r2EndpointUrl)
    }
    var r2BucketInput by remember(uiState.coldVaultEndpointConfig.r2BucketName) {
        mutableStateOf(uiState.coldVaultEndpointConfig.r2BucketName)
    }

    val audioImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val loaded = readUriBytesAndName(context, uri)
            if (loaded != null) {
                val (fileName, bytes) = loaded
                val slot = importSlotName.ifBlank {
                    "audio_${fileName.substringBeforeLast('.')}"
                }
                viewModel.importRawBytesThroughSixStageVault(
                    context = context,
                    rawBytes = bytes,
                    fileName = fileName,
                    slotName = slot,
                    sensitive = false
                )
            } else {
                viewModel.postStatusMessage("Unable to read selected audio file.")
            }
        }
    }

    val universalVaultImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val loaded = readUriBytesAndName(context, uri)
            if (loaded != null) {
                val (fileName, bytes) = loaded
                val slot = importSlotName.ifBlank { fileName.substringBeforeLast('.') }
                viewModel.importRawBytesThroughSixStageVault(
                    context = context,
                    rawBytes = bytes,
                    fileName = fileName,
                    slotName = slot,
                    sensitive = markSensitive
                )
            } else {
                viewModel.postStatusMessage("Unable to read selected file for 6-Stage import.")
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_and_vault_studio_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = if (activeTab == 0) Icons.Default.GraphicEq else Icons.Default.Shield,
                        contentDescription = "Audio & Cold Vault Studio",
                        tint = if (activeTab == 0) CyanNeon else EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "AUDIO DSP & COLD VAULT STUDIO",
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                        Text(
                            text = "${uiState.audioVaultEntries.size} audio tracks • ${uiState.vaultCatalog.size} vault files • ${uiState.activeSkinManifest.slotBindings.size} skin slots",
                            fontSize = 10.sp,
                            color = MutedSlate
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.isVaultUnlocked) EmeraldGreen.copy(alpha = 0.14f) else AmberGold.copy(alpha = 0.16f),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isVaultUnlocked) EmeraldGreen.copy(alpha = 0.5f) else AmberGold.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = if (uiState.isVaultUnlocked) "VAULT READY" else "PIN LOCKED",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isVaultUnlocked) EmeraldGreen else AmberGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3 Sub-Tabs: Audio Vault & DSP | 6-Stage & Cold Vault | Create Audio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VaultStudioTabButton(
                    label = "Audio Vault",
                    selected = activeTab == 0,
                    accent = CyanNeon,
                    onClick = { activeTab = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_vault_tab_btn")
                )
                VaultStudioTabButton(
                    label = "Cold Vault",
                    selected = activeTab == 1,
                    accent = EmeraldGreen,
                    onClick = { activeTab = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cold_vault_tab_btn")
                )
                VaultStudioTabButton(
                    label = "Create Audio",
                    selected = activeTab == 2,
                    accent = MagentaNeon,
                    onClick = { activeTab = 2 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_audio_tab_btn")
                )
            }

            when (activeTab) {
                0 -> AudioVaultDspTabContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onImportAudioClick = {
                        audioImportLauncher.launch(arrayOf("audio/*", "application/json", "*/*"))
                    }
                )
                2 -> AudioComposerPanel(
                    uiState = uiState,
                    viewModel = viewModel
                )
                else -> ColdVaultAndSixStageTabContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    slotName = importSlotName,
                    onSlotNameChange = { importSlotName = it },
                    markSensitive = markSensitive,
                    onMarkSensitiveChange = { markSensitive = it },
                    vaultPassphrase = vaultPassphrase,
                    onVaultPassphraseChange = { vaultPassphrase = it },
                    pinInput = pinInput,
                    onPinInputChange = { pinInput = it },
                    r2EndpointInput = r2EndpointInput,
                    onR2EndpointChange = { r2EndpointInput = it },
                    r2BucketInput = r2BucketInput,
                    onR2BucketChange = { r2BucketInput = it },
                    onImportAnyAssetClick = {
                        universalVaultImportLauncher.launch(arrayOf("*/*"))
                    }
                )
            }
        }
    }
}

@Composable
private fun VaultStudioTabButton(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else ObsidianCard,
        border = BorderStroke(1.dp, if (selected) accent else CardBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                fontFamily = TechMono,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MutedSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AudioVaultDspTabContent(
    uiState: AppUiState,
    viewModel: MainViewModel,
    onImportAudioClick: () -> Unit
) {
    val selectedAudio = uiState.audioVaultEntries.firstOrNull { it.id == uiState.selectedAudioEntryId }
        ?: uiState.audioVaultEntries.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Import Audio CTA
        OutlinedButton(
            onClick = onImportAudioClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("import_audio_six_stage_btn"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = "Import Audio", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Import Audio (.wav / .ogg / .mid / .json)",
                fontFamily = TechMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Audio Preset & Imported Track Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            uiState.audioVaultEntries.forEach { entry ->
                AudioVaultEntryRow(
                    entry = entry,
                    selected = entry.id == selectedAudio?.id,
                    onClick = { viewModel.selectAudioEntry(entry.id) }
                )
            }
        }

        // Real Compose Canvas Waveform Preview
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
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
                    Text(
                        text = selectedAudio?.title ?: "No Audio Selected",
                        fontFamily = TechMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = selectedAudio?.let { "${it.sampleRateHz} Hz • ${it.durationMs} ms" } ?: "",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = CyanNeon
                    )
                }

                WaveformCanvasPreview(
                    peaks = selectedAudio?.waveformPeaks.orEmpty(),
                    accentColor = when (uiState.selectedSoundchipProfile) {
                        SoundchipDspProfile.DMG_GB_4CH, SoundchipDspProfile.GBA_DIRECTSOUND -> EmeraldGreen
                        SoundchipDspProfile.GENESIS_YM2612_FM, SoundchipDspProfile.TECHNO_THAUMIC_CUSTOM -> MagentaNeon
                        SoundchipDspProfile.NES_2A03_5CH, SoundchipDspProfile.PS1_SPU_ADPCM -> AmberGold
                        else -> CyanNeon
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .testTag("audio_waveform_canvas")
                )
            }
        }

        // 8-Profile Soundchip DSP Split Selector
        Text(
            text = "SOUNDCHIP DSP SPLIT (8 PROFILES)",
            fontFamily = TechMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AmberGold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SoundchipDspProfile.entries.forEach { profile ->
                val isSelected = profile == uiState.selectedSoundchipProfile
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectSoundchipProfile(profile) },
                    label = {
                        Text(
                            text = profile.displayName,
                            fontFamily = TechMono,
                            fontSize = 10.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MagentaNeon.copy(alpha = 0.24f),
                        selectedLabelColor = MagentaNeon,
                        containerColor = ObsidianCard,
                        labelColor = MutedSlate
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = CardBorder,
                        selectedBorderColor = MagentaNeon
                    ),
                    modifier = Modifier.testTag("soundchip_chip_${profile.name.lowercase()}")
                )
            }
        }

        Text(
            text = uiState.selectedSoundchipProfile.technicalDescription,
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = MutedSlate
        )

        // Audition & Export JSON Spec Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.auditionSelectedAudioWithDsp() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("audition_audio_dsp_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Audition with DSP", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Audition DSP", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { viewModel.exportSelectedAudioSpecJson() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("export_audio_spec_json_btn"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold)
            ) {
                Icon(Icons.Default.Code, contentDescription = "Export Audio JSON Spec", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export JSON", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AudioVaultEntryRow(
    entry: AudioAssetEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("audio_entry_${entry.id}"),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) CyanNeon.copy(alpha = 0.12f) else ObsidianCard,
        border = BorderStroke(1.dp, if (selected) CyanNeon else CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = entry.category.displayName,
                tint = if (selected) CyanNeon else MutedSlate,
                modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.category.displayName} • ${entry.format.displayName} • slot:${entry.slotKey}",
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    color = MutedSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${entry.durationMs}ms",
                fontFamily = TechMono,
                fontSize = 10.sp,
                color = if (selected) CyanNeon else EmeraldGreen
            )
        }
    }
}

@Composable
private fun WaveformCanvasPreview(
    peaks: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val barList = if (peaks.isEmpty()) List(48) { 0.1f } else peaks
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF060B16))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
    ) {
        val midY = size.height / 2f
        // Center horizon guide line
        drawLine(
            color = accentColor.copy(alpha = 0.25f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.5f
        )
        val count = barList.size.coerceAtLeast(1)
        val slotW = size.width / count.toFloat()
        val barW = (slotW * 0.64f).coerceAtLeast(2f)
        val barBrush = Brush.verticalGradient(
            colors = listOf(accentColor, accentColor.copy(alpha = 0.45f), accentColor)
        )
        barList.forEachIndexed { idx, peak ->
            val clamped = peak.coerceIn(0.05f, 1f)
            val halfH = clamped * (size.height * 0.42f)
            val left = idx * slotW + (slotW - barW) / 2f
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, midY - halfH),
                size = Size(barW, halfH * 2f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

@Composable
private fun ColdVaultAndSixStageTabContent(
    uiState: AppUiState,
    viewModel: MainViewModel,
    slotName: String,
    onSlotNameChange: (String) -> Unit,
    markSensitive: Boolean,
    onMarkSensitiveChange: (Boolean) -> Unit,
    vaultPassphrase: String,
    onVaultPassphraseChange: (String) -> Unit,
    pinInput: String,
    onPinInputChange: (String) -> Unit,
    r2EndpointInput: String,
    onR2EndpointChange: (String) -> Unit,
    r2BucketInput: String,
    onR2BucketChange: (String) -> Unit,
    onImportAnyAssetClick: () -> Unit
) {
    val context = LocalContext.current
    val stats = uiState.hotCacheStats
    val usedMb = stats.usedBytes / (1024.0 * 1024.0)
    val maxMb = stats.maxBytes / (1024.0 * 1024.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Vault PIN Gate if configured and locked
        if (uiState.isVaultPinConfigured && !uiState.isVaultUnlocked) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = "Vault Locked", tint = AmberGold, modifier = Modifier.size(18.dp))
                        Text("VAULT PIN LOCKED", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                    }
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = onPinInputChange,
                        label = { Text("Enter Vault PIN", fontFamily = TechMono, fontSize = 10.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_pin_unlock_input"),
                        colors = obsidianFieldColors(AmberGold)
                    )
                    Button(
                        onClick = {
                            viewModel.unlockVaultWithPin(pinInput, context)
                            onPinInputChange("")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("vault_pin_unlock_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color(0xFF1F1300))
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Unlock Vault", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Unlock Cold Vault", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        // 1. 200 MB LRU Hot Phone Cache Meter
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = "Hot Phone Cache", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "HOT PHONE CACHE (LRU)",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f MB / %.1f MB", usedMb, maxMb),
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                LinearProgressIndicator(
                    progress = { stats.usageFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("hot_cache_progress_bar"),
                    color = if (stats.usageFraction > 0.85f) AmberGold else EmeraldGreen,
                    trackColor = Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOCAL: ${stats.cachedLocalCount}  •  COLD/REMOTE: ${stats.remoteOnlyCount}",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = MutedSlate
                    )
                    OutlinedButton(
                        onClick = { viewModel.trimHotPhoneCacheLru(context, vaultPassphrase) },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("run_lru_trim_btn"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Run LRU Trim", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Run LRU Trim", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. 6-Stage SHA-256 Asset Importer Controls
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "6-STAGE SHA-256 IMPORTER",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
                OutlinedTextField(
                    value = slotName,
                    onValueChange = onSlotNameChange,
                    label = { Text("Named Skin Slot (optional)", fontFamily = TechMono, fontSize = 10.sp) },
                    placeholder = { Text("e.g. hero_idle_sheet or cry_kaelen", fontSize = 11.sp, color = MutedSlate) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("six_stage_slot_input"),
                    colors = obsidianFieldColors(CyanNeon)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "Sensitive Blur", tint = MagentaNeon, modifier = Modifier.size(16.dp))
                        Text("Mark Sensitive (Auto-Blur Thumb)", fontSize = 11.sp, color = Color.White)
                    }
                    Switch(
                        checked = markSensitive,
                        onCheckedChange = onMarkSensitiveChange,
                        modifier = Modifier.testTag("mark_sensitive_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MagentaNeon)
                    )
                }
                Button(
                    onClick = onImportAnyAssetClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_any_asset_six_stage_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26))
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Import Asset via 6-Stage Pipeline", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Any Asset via 6-Stage Pipeline", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 6-Stage Verification Checklist
                val lastResult = uiState.lastSixStageImportResult
                if (lastResult != null) {
                    HorizontalDivider(color = CardBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lastResult.success) "PIPELINE VERIFIED (6/6)" else "PIPELINE BLOCKED",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lastResult.success) EmeraldGreen else MagentaNeon
                        )
                        if (lastResult.wasDeduplicated) {
                            Text(
                                text = "SHA-256 DEDUPLICATED",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                        }
                    }
                    ImportPipelineStage.entries.forEach { stage ->
                        val record = lastResult.stages.firstOrNull { it.stage == stage }
                        val passed = record?.passed == true
                        val tint = when {
                            record == null -> MutedSlate.copy(alpha = 0.5f)
                            passed -> EmeraldGreen
                            else -> MagentaNeon
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = stage.name,
                                tint = tint,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${stage.name}: ${record?.detail ?: "Not reached"}",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = if (record == null) MutedSlate else Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. AES-256-GCM Cold Vault (.vault.enc) Passphrase + Vault Catalog Entries
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AES-256-GCM COLD VAULT (.vault.enc)",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MagentaNeon
                )
                OutlinedTextField(
                    value = vaultPassphrase,
                    onValueChange = onVaultPassphraseChange,
                    label = { Text("Cold Vault Encryption Passphrase", fontFamily = TechMono, fontSize = 10.sp) },
                    placeholder = { Text("PBKDF2-SHA256 (65,536 iter) • never stored", fontSize = 11.sp, color = MutedSlate) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cold_vault_passphrase_input"),
                    colors = obsidianFieldColors(MagentaNeon)
                )

                if (uiState.vaultCatalog.isEmpty()) {
                    Text(
                        text = "No files imported into the Content-Addressed Vault yet. Import any PNG, WAV, OGG, MIDI, OBJ, or JSON asset above.",
                        fontSize = 11.sp,
                        color = MutedSlate
                    )
                } else {
                    uiState.vaultCatalog.take(10).forEach { entry ->
                        VaultCatalogEntryRow(
                            entry = entry,
                            onOffloadClick = {
                                viewModel.offloadVaultEntryToColdVault(context, entry.id, vaultPassphrase)
                            },
                            onHydrateClick = {
                                viewModel.hydrateVaultEntryFromColdVault(context, entry.id, vaultPassphrase)
                            }
                        )
                    }
                }
            }
        }

        // 4. Vault PIN Lock Controls & Honest Cloudflare R2 / Local Cold Vault Config
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "VAULT PIN LOCK & COLD BACKUP TARGET",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = onPinInputChange,
                        label = { Text("Set/Update PIN (4+ chars)", fontFamily = TechMono, fontSize = 10.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_pin_set_input"),
                        colors = obsidianFieldColors(AmberGold)
                    )
                    Button(
                        onClick = {
                            viewModel.configureVaultPin(pinInput, context)
                            onPinInputChange("")
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("vault_pin_save_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color(0xFF1F1300))
                    ) {
                        Text("Set PIN", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.isVaultPinConfigured) {
                    OutlinedButton(
                        onClick = { viewModel.lockVaultNow() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("vault_lock_now_btn"),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Vault Now", modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lock Cold Vault Now", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = CardBorder)
                Text(
                    text = "Active Mode: ${uiState.coldVaultEndpointConfig.backupModeLabel}",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    color = EmeraldGreen
                )
                OutlinedTextField(
                    value = r2EndpointInput,
                    onValueChange = onR2EndpointChange,
                    label = { Text("Optional Cloudflare R2 Endpoint (https://...)", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("r2_endpoint_input"),
                    colors = obsidianFieldColors(CyanNeon)
                )
                OutlinedTextField(
                    value = r2BucketInput,
                    onValueChange = onR2BucketChange,
                    label = { Text("Optional R2 Bucket Name", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("r2_bucket_input"),
                    colors = obsidianFieldColors(CyanNeon)
                )
                OutlinedButton(
                    onClick = { viewModel.updateColdVaultEndpointConfig(r2EndpointInput, r2BucketInput, context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_cold_vault_config_btn"),
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Cold Vault Config", modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Cold Vault Target Config", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VaultCatalogEntryRow(
    entry: VaultCatalogEntry,
    onOffloadClick: () -> Unit,
    onHydrateClick: () -> Unit
) {
    val isLocal = entry.cacheState == VaultCacheState.CACHED_LOCAL
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, if (isLocal) EmeraldGreen.copy(alpha = 0.35f) else AmberGold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "slot:${entry.slotName} • ${entry.extension.uppercase()} • ${entry.byteSize} B • #${entry.sha256Hex.take(10)}",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = MutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (isLocal) "LOCAL" else "REMOTE_ONLY",
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocal) EmeraldGreen else AmberGold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (entry.sensitive) "SENSITIVE • BLURRED THUMB" else "${entry.gameTag} / ${entry.groupTag}",
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    color = if (entry.sensitive) MagentaNeon else MutedSlate,
                    modifier = Modifier.weight(1f)
                )
                if (isLocal) {
                    OutlinedButton(
                        onClick = onOffloadClick,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("offload_vault_btn_${entry.id}"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = "Offload to Cold Vault", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Offload (.vault.enc)", fontFamily = TechMono, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onHydrateClick,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("hydrate_vault_btn_${entry.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color(0xFF042013))
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Hydrate from Cold Vault", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hydrate", fontFamily = TechMono, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun obsidianFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = CardBorder,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = accent,
    unfocusedLabelColor = MutedSlate
)

private fun readUriBytesAndName(context: Context, uri: Uri): Pair<String, ByteArray>? {
    return try {
        var displayName = "imported_asset.bin"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (col >= 0) {
                    val queried = cursor.getString(col)
                    if (!queried.isNullOrBlank()) displayName = queried
                }
            }
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        displayName to bytes
    } catch (e: Exception) {
        Log.e("AudioAndVaultStudio", "Failed reading picked URI bytes", e)
        null
    }
}
