package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppUiState
import com.example.MainViewModel
import com.example.ai.AiBuildTargetKind
import com.example.ai.ForgeChatMessage
import com.example.ai.ForgeChatRole
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

/**
 * Multi-turn Obsidian AI Asset Director Chat Studio.
 * Allows creators to iterate conversationally on sprites, 3D blueprints, textures, cries, and chiptunes,
 * then materialize any AI reply directly into the studio with inline 1-tap Build & Pin-to-Training chips.
 */
@Composable
fun ForgeChatStudioPanel(
    uiState: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var draftMessage by remember { mutableStateOf("") }

    val quickStarters = remember {
        listOf(
            "Design a 2x4 arcade boss sprite sheet with glowing cyan Mana Circuits",
            "Architect a twin-winged 3D dragon mech blueprint with dorsal thrusters",
            "Create a seamless obsidian armor plate texture with gold rivets",
            "Design a crystal phoenix creature cry SFX and 140 BPM boss chiptune"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("forge_chat_studio_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header strip with live Target FPS pill + Clear button
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
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI DIRECTOR CHAT STUDIO",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("chat_live_target_fps_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Target FPS",
                            tint = AmberGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Target FPS: ${uiState.targetAssetFps}",
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearForgeChatHistory() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("forge_chat_clear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear AI Chat History",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick Starter Chips when conversation is fresh
        if (uiState.forgeChatMessages.size <= 1) {
            Text(
                text = "QUICK STARTER PROMPTS",
                fontFamily = TechMono,
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickStarters.forEachIndexed { idx, starter ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.sendForgeChatMessage(starter) },
                        label = {
                            Text(
                                text = starter,
                                fontFamily = TechMono,
                                fontSize = 9.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF162033),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("forge_chat_starter_$idx")
                    )
                }
            }
        }

        // Message Transcript
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070C17), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            uiState.forgeChatMessages.forEach { message ->
                ForgeChatBubble(
                    message = message,
                    isBuilding = uiState.isCreatingFromScratchWithAi,
                    onBuildTarget = { targetKind ->
                        viewModel.buildAssetFromChatMessage(message.id, targetKind)
                    },
                    onPinToTraining = {
                        viewModel.pinChatMessageAsSharedTraining(message.id)
                    }
                )
            }

            if (uiState.isForgeChatSending) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyanNeon,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AI Director is thinking (${shortProviderName(uiState.omniAiRouting.activeTextProvider)})...",
                        fontFamily = TechMono,
                        fontSize = 10.sp,
                        color = CyanNeon
                    )
                }
            }
        }

        HorizontalDivider(color = CardBorder)

        // Bottom Composer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draftMessage,
                onValueChange = { draftMessage = it },
                placeholder = {
                    Text(
                        text = "Ask AI Director to design a sprite, 3D mech, texture, cry, or chiptune...",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                },
                minLines = 1,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("forge_chat_input_field")
            )

            Button(
                onClick = {
                    val toSend = draftMessage.trim()
                    if (toSend.isNotEmpty()) {
                        draftMessage = ""
                        viewModel.sendForgeChatMessage(toSend)
                    }
                },
                enabled = !uiState.isForgeChatSending && draftMessage.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = Color(0xFF031B26),
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF64748B)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("forge_chat_send_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ForgeChatBubble(
    message: ForgeChatMessage,
    isBuilding: Boolean,
    onBuildTarget: (AiBuildTargetKind) -> Unit,
    onPinToTraining: () -> Unit
) {
    val isUser = message.role == ForgeChatRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 12.dp
            ),
            color = if (isUser) Color(0xFF0E2338) else Color(0xFF111B2E),
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) CyanNeon.copy(alpha = 0.7f) else CardBorder
            ),
            modifier = Modifier
                .widthIn(max = 330.dp)
                .testTag("forge_chat_bubble_${message.id}")
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isUser) "YOU" else "AI DIRECTOR",
                        fontFamily = TechMono,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) CyanNeon else EmeraldGreen
                    )
                    if (!isUser && message.providerUsed.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (message.providerUsed == "Error") {
                                Color(0xFFF43F5E).copy(alpha = 0.2f)
                            } else {
                                CyanNeon.copy(alpha = 0.14f)
                            }
                        ) {
                            Text(
                                text = message.providerUsed,
                                fontFamily = TechMono,
                                fontSize = 8.sp,
                                color = if (message.providerUsed == "Error") Color(0xFFFDA4AF) else CyanNeon,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 16.sp
                )
            }
        }

        // Inline 1-tap Build & Pin-to-Training chips under every Assistant bubble
        if (!isUser) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buildActions = listOf(
                    AiBuildTargetKind.SPRITE_2D to "Build 2D",
                    AiBuildTargetKind.MODEL_3D to "Build 3D",
                    AiBuildTargetKind.TEXTURE_MATERIAL to "Build Texture",
                    AiBuildTargetKind.AUDIO_CRY to "Build Cry",
                    AiBuildTargetKind.AUDIO_CHIPTUNE to "Build Chiptune"
                )
                buildActions.forEach { (kind, label) ->
                    val isRecommended = message.suggestedBuildTarget == kind
                    FilterChip(
                        selected = isRecommended,
                        enabled = !isBuilding,
                        onClick = { onBuildTarget(kind) },
                        label = {
                            Text(
                                text = label,
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = if (isRecommended) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.24f),
                            selectedLabelColor = EmeraldGreen,
                            containerColor = Color(0xFF131F36),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("chat_build_${kind.name.lowercase()}_${message.id}")
                    )
                }

                FilterChip(
                    selected = false,
                    onClick = onPinToTraining,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Pin to Training",
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Pin to Training",
                            fontFamily = TechMono,
                            fontSize = 9.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MagentaNeon.copy(alpha = 0.16f),
                        labelColor = MagentaNeon,
                        iconColor = MagentaNeon
                    ),
                    modifier = Modifier.testTag("chat_pin_training_${message.id}")
                )
            }
        }
    }
}
