package com.example.ui.components

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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.model.audio.AudioSynthAndDspEngine
import com.example.model.audio.ProceduralCrySpec
import com.example.model.audio.ScoreSequenceNote
import com.example.model.audio.ScoreSequenceSpec
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono
import kotlin.math.roundToInt

private val SEQUENCER_NOTES: List<Pair<String, Int>> = listOf(
    "C5" to 72,
    "A4" to 69,
    "G4" to 67,
    "E4" to 64,
    "D4" to 62,
    "C4" to 60,
    "A3" to 57,
    "G3" to 55
)

private val SEQUENCER_CHANNELS: List<Triple<Int, String, Color>> = listOf(
    Triple(0, "Ch0 Pulse", CyanNeon),
    Triple(1, "Ch1 Triangle Bass", AmberGold),
    Triple(2, "Ch2 Lead", MagentaNeon)
)

/**
 * From-Scratch Audio Cry Designer (`audio_cue.json`) & 16-Step Chiptune Sequencer (`score_seq.json`)
 * rendered inside PixelForge's Audio & Cold Vault Studio.
 */
@Composable
fun AudioComposerPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // 0 = Procedural Cry Designer, 1 = 16-Step Chiptune Sequencer
    var composerMode by remember { mutableIntStateOf(0) }

    // Cry Designer state
    var cryTitle by remember { mutableStateOf("Emerald Wyrm Cry") }
    var crySlotKey by remember { mutableStateOf("cry_emerald_wyrm") }
    var cryStartHz by remember { mutableFloatStateOf(520f) }
    var cryEndHz by remember { mutableFloatStateOf(1180f) }
    var cryDurationMs by remember { mutableFloatStateOf(420f) }
    var cryModHz by remember { mutableFloatStateOf(24f) }
    var cryNoiseMix by remember { mutableFloatStateOf(0.18f) }

    // 16-Step Sequencer state (stores active steps as set of "ch:row:step" keys for multi-channel layering)
    var seqTitle by remember { mutableStateOf("Obsidian Sky Loop") }
    var seqSlotKey by remember { mutableStateOf("bgm_obsidian_sky") }
    var seqBpm by remember { mutableFloatStateOf(136f) }
    var activeChannel by remember { mutableIntStateOf(0) }
    var activeCells by remember {
        mutableStateOf(
            setOf(
                // Starter 4-note chord arp on Ch0 + bass hits on Ch1 so users can immediately audition or edit
                "0:5:0", "0:3:2", "0:2:4", "0:0:6", "0:2:8", "0:3:10", "0:4:12", "0:5:14",
                "1:7:0", "1:7:4", "1:6:8", "1:5:12"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_composer_panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = composerMode == 0,
                onClick = { composerMode = 0 },
                leadingIcon = {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("Cry Designer", fontFamily = TechMono, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanNeon,
                    selectedLabelColor = Color(0xFF031B26),
                    selectedLeadingIconColor = Color(0xFF031B26),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("audio_composer_mode_cry")
            )
            FilterChip(
                selected = composerMode == 1,
                onClick = { composerMode = 1 },
                leadingIcon = {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("16-Step Sequencer", fontFamily = TechMono, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MagentaNeon,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("audio_composer_mode_sequencer")
            )
        }

        // Active DSP Preview Badge
        Text(
            text = "LIVE DSP MONITOR: ${uiState.selectedSoundchipProfile.displayName} (${uiState.selectedSoundchipProfile.targetSampleRateHz} Hz • ${uiState.selectedSoundchipProfile.bitDepth}-bit)",
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = AmberGold
        )

        if (composerMode == 0) {
            // -----------------------------------------------------------------
            // Sub-mode A: Procedural Cry Designer (audio_cue.json)
            // -----------------------------------------------------------------
            val currentCrySpec = remember(crySlotKey, cryStartHz, cryEndHz, cryDurationMs, cryModHz, cryNoiseMix) {
                ProceduralCrySpec(
                    speciesKey = crySlotKey.ifBlank { "custom_cry" },
                    baseFreqHz = cryStartHz,
                    endFreqHz = cryEndHz,
                    durationMs = cryDurationMs.roundToInt().coerceIn(100, 1500),
                    modulationHz = cryModHz,
                    noiseMix = cryNoiseMix.coerceIn(0f, 1f)
                )
            }
            val previewPeaks = remember(currentCrySpec) {
                val pcm = AudioSynthAndDspEngine.synthesizeProceduralCryPcm(currentCrySpec, sampleRateHz = 22050)
                AudioSynthAndDspEngine.computeWaveformPeaks(pcm, bucketCount = 48)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = cryTitle,
                    onValueChange = { cryTitle = it },
                    singleLine = true,
                    label = { Text("Cry Title", fontFamily = TechMono, fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cry_designer_title_field")
                )
                OutlinedTextField(
                    value = crySlotKey,
                    onValueChange = { crySlotKey = it },
                    singleLine = true,
                    label = { Text("Slot Key", fontFamily = TechMono, fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cry_designer_slot_field")
                )
            }

            // Live Waveform Preview Canvas
            ComposerWaveformCanvas(
                peaks = previewPeaks,
                accentColor = CyanNeon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .testTag("cry_designer_waveform_canvas")
            )

            // Parameter Sliders
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ComposerSliderRow(
                        label = "Start Hz",
                        value = cryStartHz,
                        range = 80f..2000f,
                        valueText = "${cryStartHz.roundToInt()} Hz",
                        accent = CyanNeon,
                        onValueChange = { cryStartHz = it }
                    )
                    ComposerSliderRow(
                        label = "End Hz",
                        value = cryEndHz,
                        range = 80f..2000f,
                        valueText = "${cryEndHz.roundToInt()} Hz",
                        accent = CyanNeon,
                        onValueChange = { cryEndHz = it }
                    )
                    ComposerSliderRow(
                        label = "Duration",
                        value = cryDurationMs,
                        range = 100f..1500f,
                        valueText = "${cryDurationMs.roundToInt()} ms",
                        accent = EmeraldGreen,
                        onValueChange = { cryDurationMs = it }
                    )
                    ComposerSliderRow(
                        label = "FM Mod",
                        value = cryModHz,
                        range = 0f..80f,
                        valueText = "${cryModHz.roundToInt()} Hz",
                        accent = AmberGold,
                        onValueChange = { cryModHz = it }
                    )
                    ComposerSliderRow(
                        label = "Noise Mix",
                        value = cryNoiseMix,
                        range = 0f..1f,
                        valueText = "${(cryNoiseMix * 100f).roundToInt()}%",
                        accent = MagentaNeon,
                        onValueChange = { cryNoiseMix = it }
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.auditionCustomCrySpecWithDsp(currentCrySpec) },
                    border = BorderStroke(1.dp, CyanNeon),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cry_designer_audition_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Audition DSP", fontFamily = TechMono, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        viewModel.saveBrandNewCryToAudioVault(
                            title = cryTitle,
                            slotKey = crySlotKey,
                            spec = currentCrySpec
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color(0xFF031B12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("cry_designer_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Brand-New Cry to Vault", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // -----------------------------------------------------------------
            // Sub-mode B: 16-Step Chiptune Sequencer (score_seq.json)
            // -----------------------------------------------------------------
            val currentScoreSpec = remember(seqTitle, seqBpm, activeCells) {
                val notes = activeCells.mapNotNull { key ->
                    val parts = key.split(":")
                    if (parts.size != 3) return@mapNotNull null
                    val ch = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val row = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val step = parts[2].toIntOrNull() ?: return@mapNotNull null
                    val midi = SEQUENCER_NOTES.getOrNull(row)?.second ?: return@mapNotNull null
                    ScoreSequenceNote(
                        startBeat = step * 0.25f,
                        durationBeats = 0.24f,
                        midiNote = midi,
                        velocity = if (ch == 1) 0.90f else 0.84f,
                        channel = ch
                    )
                }.sortedWith(compareBy<ScoreSequenceNote> { it.startBeat }.thenBy { it.channel })

                ScoreSequenceSpec(
                    title = seqTitle.ifBlank { "Original Chiptune Loop" },
                    bpm = seqBpm.roundToInt().coerceIn(80, 200),
                    notes = notes
                )
            }

            val seqPreviewPeaks = remember(currentScoreSpec) {
                if (currentScoreSpec.notes.isEmpty()) {
                    List(48) { 0f }
                } else {
                    val pcm = AudioSynthAndDspEngine.synthesizeScoreSequencePcm(currentScoreSpec, sampleRateHz = 22050)
                    AudioSynthAndDspEngine.computeWaveformPeaks(pcm, bucketCount = 48)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = seqTitle,
                    onValueChange = { seqTitle = it },
                    singleLine = true,
                    label = { Text("Track Title", fontFamily = TechMono, fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MagentaNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("seq_composer_title_field")
                )
                OutlinedTextField(
                    value = seqSlotKey,
                    onValueChange = { seqSlotKey = it },
                    singleLine = true,
                    label = { Text("Slot Key", fontFamily = TechMono, fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MagentaNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("seq_composer_slot_field")
                )
            }

            ComposerSliderRow(
                label = "Tempo",
                value = seqBpm,
                range = 80f..200f,
                valueText = "${seqBpm.roundToInt()} BPM",
                accent = MagentaNeon,
                onValueChange = { seqBpm = it }
            )

            // Channel Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SEQUENCER_CHANNELS.forEach { (chId, chLabel, chColor) ->
                    val countForCh = activeCells.count { it.startsWith("$chId:") }
                    FilterChip(
                        selected = activeChannel == chId,
                        onClick = { activeChannel = chId },
                        label = {
                            Text(
                                text = "$chLabel ($countForCh)",
                                fontFamily = TechMono,
                                fontSize = 10.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chColor,
                            selectedLabelColor = if (chId == 2) Color.White else Color(0xFF031B26),
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("seq_channel_chip_$chId")
                    )
                }
            }

            // 16-Step × 8-Note Tappable Grid (horizontally scrollable for crisp 48dp-friendly touch cells)
            val activeChannelColor = SEQUENCER_CHANNELS.firstOrNull { it.first == activeChannel }?.third ?: CyanNeon
            Surface(
                color = Color(0xFF070B12),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "16-STEP × 8-NOTE GRID (TAP CELLS TO TOGGLE • ${currentScoreSpec.notes.size} TOTAL NOTES)",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Step header numbers 1..16
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(Modifier.width(30.dp))
                                for (step in 0 until 16) {
                                    Text(
                                        text = "${step + 1}",
                                        fontFamily = TechMono,
                                        fontSize = 9.sp,
                                        color = if (step % 4 == 0) AmberGold else Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(28.dp)
                                    )
                                }
                            }

                            SEQUENCER_NOTES.forEachIndexed { rowIdx, (noteLabel, _) ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = noteLabel,
                                        fontFamily = TechMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFCBD5E1),
                                        modifier = Modifier.width(30.dp)
                                    )
                                    for (step in 0 until 16) {
                                        val cellKey = "$activeChannel:$rowIdx:$step"
                                        val isOnActiveChannel = cellKey in activeCells
                                        val hasOtherChannelNote = !isOnActiveChannel && (0..2).any { otherCh ->
                                            otherCh != activeChannel && "$otherCh:$rowIdx:$step" in activeCells
                                        }
                                        val cellBg = when {
                                            isOnActiveChannel -> activeChannelColor
                                            hasOtherChannelNote -> Color(0xFF334155)
                                            step % 4 == 0 -> Color(0xFF162033)
                                            else -> Color(0xFF0F172A)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(width = 28.dp, height = 26.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(cellBg)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isOnActiveChannel) Color.White else CardBorder,
                                                    shape = RoundedCornerShape(5.dp)
                                                )
                                                .clickable {
                                                    activeCells = if (isOnActiveChannel) {
                                                        activeCells - cellKey
                                                    } else {
                                                        activeCells + cellKey
                                                    }
                                                }
                                                .testTag("seq_cell_${rowIdx}_$step")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live Waveform Preview
            ComposerWaveformCanvas(
                peaks = seqPreviewPeaks,
                accentColor = MagentaNeon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("seq_composer_waveform_canvas")
            )

            // Action Row: Clear | Audition DSP | Save Brand-New Track to Vault
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { activeCells = emptySet() },
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("seq_composer_clear_btn")
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clear Grid", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = { viewModel.auditionCustomScoreSpecWithDsp(currentScoreSpec) },
                    border = BorderStroke(1.dp, CyanNeon),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("seq_composer_audition_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Audition DSP", fontFamily = TechMono, fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        viewModel.saveBrandNewSequenceToAudioVault(
                            title = seqTitle,
                            slotKey = seqSlotKey,
                            spec = currentScoreSpec
                        )
                    },
                    enabled = currentScoreSpec.notes.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color(0xFF031B12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("seq_composer_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Save Brand-New Track to Vault", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ComposerWaveformCanvas(
    peaks: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF070B12),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            val count = peaks.size.coerceAtLeast(1)
            val barSlot = size.width / count
            val barW = (barSlot * 0.65f).coerceAtLeast(2f)
            val midY = size.height / 2f

            // Baseline
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1f
            )

            peaks.forEachIndexed { idx, amp ->
                val clamped = amp.coerceIn(0.04f, 1f)
                val barH = clamped * (size.height * 0.88f)
                val left = idx * barSlot + (barSlot - barW) / 2f
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.35f + clamped * 0.65f),
                    topLeft = Offset(left, midY - barH / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
private fun ComposerSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E1),
            modifier = Modifier.width(64.dp)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            fontFamily = TechMono,
            fontSize = 10.sp,
            color = accent,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End
        )
    }
}
