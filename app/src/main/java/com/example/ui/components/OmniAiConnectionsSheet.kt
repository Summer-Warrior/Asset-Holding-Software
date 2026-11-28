package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.AppUiState
import com.example.MainViewModel
import com.example.ai.AiProviderCapabilityRow
import com.example.ai.AiProviderProfile
import com.example.ai.AiProviderType
import com.example.ai.AiTaskRole
import com.example.ai.KnowledgeSourceEntry
import com.example.ai.KnowledgeSourceKind
import com.example.ai.MultiSourceContextRegistry
import com.example.ai.OmniAiProviderCapabilities
import com.example.ai.OmniAiRoutingConfig
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.TechMono

/**
 * Foundry-styled Multi-AI & Multi-Source Hub dialog/sheet.
 *
 * Tab 0 — **AI Providers**: 1-click OpenRouter PKCE sign-in, encrypted per-provider API key vault,
 * model/base-URL configuration, and independent active role assignment (Text, Vision Review, Image Art).
 *
 * Tab 1 — **Knowledge Sources**: toggleable shared knowledge base across every connected AI
 * (SAF `.md/.txt/.json` import, SSRF-validated GitHub/HTTPS URLs, pasted Notebook/lore text,
 * live PixelForge project context, and a live character/token budget meter).
 */
@Composable
fun OmniAiConnectionsSheet(
    uiState: AppUiState,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importLocalKnowledgeDocument(context, uri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF070C17),
            border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("omni_ai_connections_sheet")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1526))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanNeon.copy(alpha = 0.16f))
                                .border(1.dp, CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Multi-AI Hub",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "OMNI-AI & MULTI-SOURCE HUB",
                                fontFamily = TechMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon
                            )
                            Text(
                                text = "Connect multiple AI providers & share knowledge sources across every studio AI",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_omni_ai_hub_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close AI Hub",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                // Active Routing Summary Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A111E))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val routing = uiState.omniAiRouting
                    val enabledSources = uiState.knowledgeSources.count { it.enabled }
                    RoleSummaryPill("TEXT", shortProviderName(routing.activeTextProvider), CyanNeon)
                    RoleSummaryPill("VISION", shortProviderName(routing.activeVisionProvider), MagentaNeon)
                    RoleSummaryPill("IMAGE", shortProviderName(routing.activeImageProvider), EmeraldGreen)
                    RoleSummaryPill("SOURCES", "$enabledSources Active", AmberGold)
                }

                // Two Tabs: AI Providers | Knowledge Sources
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = CyanNeon,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyanNeon
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            val connectedCount = uiState.aiProviderProfiles.count { it.isConnected }
                            Text(
                                text = "AI Providers ($connectedCount/${uiState.aiProviderProfiles.size})",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("omni_hub_tab_providers")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            val enabledCount = uiState.knowledgeSources.count { it.enabled }
                            Text(
                                text = "Knowledge Sources ($enabledCount)",
                                fontFamily = TechMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("omni_hub_tab_sources")
                    )
                }

                // Tab Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        AiProvidersTabContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onLaunchUrl = { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.postStatusMessage("Could not open browser for sign-in: ${e.message}")
                                }
                            }
                        )
                    } else {
                        KnowledgeSourcesTabContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onPickLocalFile = {
                                documentPickerLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleSummaryPill(role: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = "$role: $value",
            fontFamily = TechMono,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AiProvidersTabContent(
    uiState: AppUiState,
    viewModel: MainViewModel,
    onLaunchUrl: (String) -> Unit
) {
    // 1-Click OpenRouter PKCE Banner Card
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A2F)),
        border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                Text(
                    text = "1-CLICK IN-APP OPENROUTER OAUTH (PKCE)",
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
            }
            Text(
                text = "Sign into OpenRouter directly from your device browser via local loopback PKCE (no backend server required). Access Gemini, Claude, GPT, Flux & more with one encrypted key.",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 15.sp
            )
            Button(
                onClick = { viewModel.startOpenRouterPkceSignIn(onLaunchUrl) },
                enabled = !uiState.isSigningInOpenRouterPkce,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = Color(0xFF031B26)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("openrouter_pkce_signin_btn")
            ) {
                if (uiState.isSigningInOpenRouterPkce) {
                    CircularProgressIndicator(
                        color = Color(0xFF031B26),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Waiting for OpenRouter Callback...", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with OpenRouter (In-App PKCE)", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Provider Capability Checksheet & Connected Fallback Order Card
    ProviderCapabilityChecksheetCard(
        profiles = uiState.aiProviderProfiles,
        routing = uiState.omniAiRouting
    )

    // Provider Connection Cards
    uiState.aiProviderProfiles.forEach { profile ->
        ProviderConnectionCard(
            profile = profile,
            routing = uiState.omniAiRouting,
            onSaveKey = { secret -> viewModel.saveAiProviderCredential(profile.provider, secret) },
            onClearKey = { viewModel.clearAiProviderCredential(profile.provider) },
            onUpdateSettings = { textModel, imageModel, baseUrl, localConn ->
                viewModel.updateAiProviderSettings(
                    provider = profile.provider,
                    textModelId = textModel,
                    imageModelId = imageModel,
                    baseUrl = baseUrl,
                    markLocalConnected = localConn
                )
            },
            onAssignRole = { role -> viewModel.setActiveAiProviderForRole(role, profile.provider) }
        )
    }
}

@Composable
private fun ProviderCapabilityChecksheetCard(
    profiles: List<AiProviderProfile>,
    routing: OmniAiRoutingConfig
) {
    val rows = remember { OmniAiProviderCapabilities.capabilityMatrix() }
    val profileByProvider = remember(profiles) { profiles.associateBy { it.provider } }
    val fallbackOrder = remember {
        listOf(
            AiProviderType.FIREBASE_STUDIO,
            AiProviderType.OPENROUTER,
            AiProviderType.DIRECT_GEMINI,
            AiProviderType.OPENAI,
            AiProviderType.ANTHROPIC_CLAUDE,
            AiProviderType.LOCAL_OPENAI_COMPAT
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1222)),
        border = BorderStroke(1.25.dp, EmeraldGreen.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("omni_provider_capability_checksheet")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROVIDER CAPABILITY CHECKSHEET",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
                Text(
                    text = "${profiles.count { it.isConnected }}/${rows.size} CONNECTED",
                    fontFamily = TechMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
            }

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF111C31))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROVIDER",
                    fontFamily = TechMono,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.weight(1.75f)
                )
                CapabilityHeaderCell("Text/3D\nAudio", Modifier.weight(1f))
                CapabilityHeaderCell("Vision", Modifier.weight(0.75f))
                CapabilityHeaderCell("2D Art", Modifier.weight(0.75f))
                CapabilityHeaderCell("Local", Modifier.weight(0.75f))
            }

            // All 6 Provider Rows
            rows.forEach { cap: AiProviderCapabilityRow ->
                val isConnected = profileByProvider[cap.provider]?.isConnected == true
                val isAssignedAny = routing.activeTextProvider == cap.provider ||
                    routing.activeVisionProvider == cap.provider ||
                    routing.activeImageProvider == cap.provider

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isAssignedAny) Color(0xFF0E1D33) else Color(0xFF080E1B))
                        .border(
                            width = 1.dp,
                            color = if (isConnected) EmeraldGreen.copy(alpha = 0.25f) else CardBorder.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(7.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("cap_row_${cap.provider.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1.75f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = cap.displayName,
                            fontFamily = TechMono,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isConnected) "CONNECTED" else "OFFLINE",
                            fontFamily = TechMono,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) EmeraldGreen else Color(0xFF64748B)
                        )
                    }
                    CapabilityBadgeCell(cap.supportsTextAndJsonBuild, EmeraldGreen, Modifier.weight(1f))
                    CapabilityBadgeCell(cap.supportsVisionAudit, CyanNeon, Modifier.weight(0.75f))
                    CapabilityBadgeCell(cap.supportsImage2D, MagentaNeon, Modifier.weight(0.75f))
                    CapabilityBadgeCell(cap.isLocalSelfHosted, AmberGold, Modifier.weight(0.75f))
                }
            }

            HorizontalDivider(color = CardBorder)

            // Active Connected Fallback Order Strip
            Text(
                text = "CONNECTED FALLBACK ORDER",
                fontFamily = TechMono,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = AmberGold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fallbackOrder.forEachIndexed { idx, provider ->
                    val connected = profileByProvider[provider]?.isConnected == true
                    val chipAccent = if (connected) EmeraldGreen else Color(0xFF64748B)
                    Surface(
                        color = chipAccent.copy(alpha = if (connected) 0.16f else 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, chipAccent.copy(alpha = if (connected) 0.6f else 0.35f))
                    ) {
                        Text(
                            text = "${idx + 1}. ${shortProviderName(provider)} • ${if (connected) "CONNECTED" else "OFFLINE"}",
                            fontFamily = TechMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityHeaderCell(label: String, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = label,
            fontFamily = TechMono,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            lineHeight = 9.sp
        )
    }
}

@Composable
private fun CapabilityBadgeCell(supported: Boolean, accent: Color, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            color = if (supported) accent.copy(alpha = 0.18f) else Color(0xFF1E293B).copy(alpha = 0.6f),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(
                1.dp,
                if (supported) accent.copy(alpha = 0.65f) else Color(0xFF334155)
            )
        ) {
            Text(
                text = if (supported) "✓" else "—",
                fontFamily = TechMono,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (supported) accent else Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun ProviderConnectionCard(
    profile: AiProviderProfile,
    routing: OmniAiRoutingConfig,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onUpdateSettings: (textModel: String?, imageModel: String?, baseUrl: String?, localConnected: Boolean?) -> Unit,
    onAssignRole: (AiTaskRole) -> Unit
) {
    var apiKeyDraft by remember(profile.provider) { mutableStateOf("") }
    var textModelDraft by remember(profile.provider, profile.textModelId) { mutableStateOf(profile.textModelId) }
    var imageModelDraft by remember(profile.provider, profile.imageModelId) { mutableStateOf(profile.imageModelId) }
    var baseUrlDraft by remember(profile.provider, profile.baseUrl) { mutableStateOf(profile.baseUrl) }

    val statusColor = if (profile.isConnected) EmeraldGreen else Color(0xFF64748B)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(
            width = if (profile.isConnected) 1.25.dp else 1.dp,
            color = if (profile.isConnected) statusColor.copy(alpha = 0.55f) else CardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${profile.provider.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Provider Title & Connected Badge
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Column {
                        Text(
                            text = profile.displayName,
                            fontFamily = TechMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile.accountHint.isNotBlank()) {
                            Text(
                                text = profile.accountHint,
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                color = statusColor
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (profile.isConnected) "CONNECTED" else "NOT SET",
                        fontFamily = TechMono,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Active Role Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiTaskRole.entries.forEach { role ->
                    val isAssigned = routing.providerFor(role) == profile.provider
                    val canUseForRole = role != AiTaskRole.IMAGE_GENERATION || profile.provider.supportsImageGeneration
                    FilterChip(
                        selected = isAssigned,
                        enabled = canUseForRole,
                        onClick = { onAssignRole(role) },
                        label = {
                            Text(
                                text = if (canUseForRole) "Active ${role.shortLabel}" else "No Image Gen",
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (role) {
                                AiTaskRole.TEXT_AND_SPECS -> CyanNeon
                                AiTaskRole.VISION_REVIEW -> MagentaNeon
                                AiTaskRole.IMAGE_GENERATION -> EmeraldGreen
                            },
                            selectedLabelColor = Color(0xFF031B26),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("assign_${role.name.lowercase()}_${profile.provider.name.lowercase()}")
                    )
                }
            }

            // Key Input & Save/Clear Controls (skip for built-in Firebase Studio)
            if (profile.provider != AiProviderType.FIREBASE_STUDIO) {
                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    label = {
                        Text(
                            text = if (profile.provider == AiProviderType.LOCAL_OPENAI_COMPAT) {
                                "Optional Bearer Key (Stored Encrypted)"
                            } else {
                                "API Key (Encrypted in AndroidKeyStore)"
                            },
                            fontFamily = TechMono,
                            fontSize = 10.sp
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (profile.isConnected) "Saved securely • Paste new key to replace" else "Paste API key...",
                            fontSize = 10.sp
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider_key_input_${profile.provider.name.lowercase()}"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = CyanNeon,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                // Model ID & Base URL configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textModelDraft,
                        onValueChange = { textModelDraft = it },
                        label = { Text("Text / Vision Model ID", fontFamily = TechMono, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyanNeon,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )
                    if (profile.provider.supportsImageGeneration) {
                        OutlinedTextField(
                            value = imageModelDraft,
                            onValueChange = { imageModelDraft = it },
                            label = { Text("Image Model ID", fontFamily = TechMono, fontSize = 9.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = EmeraldGreen,
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                if (profile.provider == AiProviderType.LOCAL_OPENAI_COMPAT) {
                    OutlinedTextField(
                        value = baseUrlDraft,
                        onValueChange = { baseUrlDraft = it },
                        label = { Text("Local Base URL (Ollama / LM Studio / PC Wi-Fi IP)", fontFamily = TechMono, fontSize = 9.sp) },
                        placeholder = { Text("http://127.0.0.1:11434/v1", fontSize = 10.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyanNeon,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )
                }

                if (profile.provider == AiProviderType.ANTHROPIC_CLAUDE) {
                    Text(
                        text = "Tip: Claude excels at Text/Specs and Vision Review. Pair it with Firebase Studio, Direct Gemini, OpenRouter, or OpenAI for Image Generation.",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (apiKeyDraft.isNotBlank()) {
                                onSaveKey(apiKeyDraft)
                                apiKeyDraft = ""
                            }
                            onUpdateSettings(
                                textModelDraft,
                                imageModelDraft,
                                if (profile.provider == AiProviderType.LOCAL_OPENAI_COMPAT) baseUrlDraft else null,
                                if (profile.provider == AiProviderType.LOCAL_OPENAI_COMPAT) true else null
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = Color(0xFF031B26)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("provider_save_btn_${profile.provider.name.lowercase()}")
                    ) {
                        Text(
                            text = if (profile.provider == AiProviderType.LOCAL_OPENAI_COMPAT) "Save & Connect Local" else "Save Key & Models",
                            fontFamily = TechMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (profile.isConnected) {
                        OutlinedButton(
                            onClick = {
                                apiKeyDraft = ""
                                onClearKey()
                            },
                            border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDA4AF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("provider_clear_btn_${profile.provider.name.lowercase()}")
                        ) {
                            Text("Clear Key", fontFamily = TechMono, fontSize = 10.sp)
                        }
                    }
                }
            } else {
                Text(
                    text = "Built-in Google AI Studio preview runtime (${profile.textModelId} & ${profile.imageModelId}). If Firebase App Check is unavailable on a private sideloaded APK, PixelForge automatically falls back to any connected provider below.",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun KnowledgeSourcesTabContent(
    uiState: AppUiState,
    viewModel: MainViewModel,
    onPickLocalFile: () -> Unit
) {
    var showUrlForm by remember { mutableStateOf(false) }
    var showPasteForm by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var urlTitleInput by remember { mutableStateOf("") }
    var pasteTitleInput by remember { mutableStateOf("") }
    var pasteBodyInput by remember { mutableStateOf("") }

    val assembledPreview = remember(
        uiState.knowledgeSources,
        uiState.currentMesh.name,
        uiState.currentSpriteSheet.name,
        uiState.activeCharacterTagForImports,
        uiState.selectedHardware.id
    ) {
        viewModel.assembleSharedOmniContextBlock(
            queryHint = uiState.promptText,
            maxChars = MultiSourceContextRegistry.DEFAULT_MAX_CONTEXT_CHARS
        )
    }
    val usedChars = assembledPreview.length
    val maxChars = MultiSourceContextRegistry.DEFAULT_MAX_CONTEXT_CHARS
    val approxTokens = (usedChars / 4).coerceAtLeast(if (usedChars > 0) 1 else 0)
    val budgetRatio = (usedChars.toFloat() / maxChars.toFloat()).coerceIn(0f, 1f)

    // Live Token / Char Budget Card
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A2F)),
        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("omni_context_budget_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHARED MULTI-AI CONTEXT BUDGET",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold
                )
                Text(
                    text = "$usedChars / $maxChars chars (~$approxTokens tokens)",
                    fontFamily = TechMono,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
            LinearProgressIndicator(
                progress = { budgetRatio },
                color = if (budgetRatio > 0.85f) AmberGold else EmeraldGreen,
                trackColor = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Text(
                text = "All enabled sources below are automatically combined and shared with every connected AI (Firebase, OpenRouter, Gemini, Claude, OpenAI & Local).",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }

    // Add Source Action Buttons
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onPickLocalFile,
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("omni_import_file_source_btn")
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("Import File (.md / .txt / .json)", fontFamily = TechMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    showUrlForm = !showUrlForm
                    if (showUrlForm) showPasteForm = false
                },
                border = BorderStroke(1.dp, EmeraldGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("omni_add_url_source_btn")
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add GitHub / HTTPS URL", fontFamily = TechMono, fontSize = 9.sp)
            }

            OutlinedButton(
                onClick = {
                    showPasteForm = !showPasteForm
                    if (showPasteForm) showUrlForm = false
                },
                border = BorderStroke(1.dp, MagentaNeon),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MagentaNeon),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("omni_add_pasted_source_btn")
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Paste Notebook / Lore", fontFamily = TechMono, fontSize = 9.sp)
            }
        }
    }

    // Inline HTTPS / Raw GitHub URL Form
    if (showUrlForm) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ADD PUBLIC GITHUB RAW / HTTPS SOURCE", fontFamily = TechMono, fontSize = 10.sp, color = EmeraldGreen)
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("HTTPS URL (raw.githubusercontent.com/...)", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("omni_url_source_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = urlTitleInput,
                    onValueChange = { urlTitleInput = it },
                    label = { Text("Optional Source Title", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                viewModel.addHttpsKnowledgeSource(urlInput, urlTitleInput)
                                urlInput = ""
                                urlTitleInput = ""
                                showUrlForm = false
                            }
                        },
                        enabled = !uiState.isFetchingKnowledgeUrl,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color(0xFF031B26)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isFetchingKnowledgeUrl) {
                            CircularProgressIndicator(color = Color(0xFF031B26), strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        } else {
                            Text("Fetch & Attach Source", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { showUrlForm = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }

    // Inline Pasted Notebook / Lore Form
    if (showPasteForm) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, MagentaNeon.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PASTE NOTEBOOK / WORLD BIBLE / LORE NOTES", fontFamily = TechMono, fontSize = 10.sp, color = MagentaNeon)
                OutlinedTextField(
                    value = pasteTitleInput,
                    onValueChange = { pasteTitleInput = it },
                    label = { Text("Source Title (e.g., World Bible, Art Style Rules)", fontFamily = TechMono, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("omni_paste_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MagentaNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = pasteBodyInput,
                    onValueChange = { pasteBodyInput = it },
                    label = { Text("Reference Markdown / JSON / Notes", fontFamily = TechMono, fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("omni_paste_body_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MagentaNeon,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (pasteBodyInput.isNotBlank()) {
                                viewModel.addPastedKnowledgeSource(pasteTitleInput, pasteBodyInput)
                                pasteTitleInput = ""
                                pasteBodyInput = ""
                                showPasteForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MagentaNeon, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("omni_save_pasted_source_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Attach Notes", fontFamily = TechMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showPasteForm = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }

    HorizontalDivider(color = CardBorder)

    // Attached Sources List
    uiState.knowledgeSources.forEach { entry ->
        KnowledgeSourceCard(
            entry = entry,
            onToggle = { enabled -> viewModel.toggleKnowledgeSource(entry.id, enabled) },
            onDelete = { viewModel.removeKnowledgeSource(entry.id) }
        )
    }
}

@Composable
private fun KnowledgeSourceCard(
    entry: KnowledgeSourceEntry,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (entry.kind) {
        KnowledgeSourceKind.ACTIVE_PROJECT_CONTEXT -> CyanNeon
        KnowledgeSourceKind.NOTEBOOK_MARKDOWN -> AmberGold
        KnowledgeSourceKind.LOCAL_FILE -> EmeraldGreen
        KnowledgeSourceKind.GITHUB_RAW_URL -> Color(0xFF38BDF8)
        KnowledgeSourceKind.PASTED_NOTES -> MagentaNeon
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(
            width = 1.dp,
            color = if (entry.enabled) badgeColor.copy(alpha = 0.55f) else CardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("knowledge_source_card_${entry.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = if (entry.enabled) badgeColor else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = entry.kind.badge,
                            fontFamily = TechMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${entry.content.length} chars",
                        fontFamily = TechMono,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = entry.title,
                    fontFamily = TechMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.originLabel,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = entry.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF031B26),
                    checkedTrackColor = badgeColor
                ),
                modifier = Modifier.testTag("knowledge_source_switch_${entry.id}")
            )

            if (entry.kind != KnowledgeSourceKind.ACTIVE_PROJECT_CONTEXT) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("knowledge_source_delete_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Source",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun shortProviderName(provider: AiProviderType): String = when (provider) {
    AiProviderType.FIREBASE_STUDIO -> "Firebase"
    AiProviderType.OPENROUTER -> "OpenRouter"
    AiProviderType.DIRECT_GEMINI -> "Gemini"
    AiProviderType.ANTHROPIC_CLAUDE -> "Claude"
    AiProviderType.OPENAI -> "OpenAI"
    AiProviderType.LOCAL_OPENAI_COMPAT -> "Local"
}
