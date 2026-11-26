package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ai.configureAppCheck
import com.example.model.importer.UniversalAssetImporter
import com.example.ui.components.AssetGalleriesOverlayScreen
import com.example.ui.screens.AssetSuiteScreen
import com.example.ui.screens.ForgeStudioScreen
import com.example.ui.screens.HardwareLabScreen
import com.example.ui.screens.ModelStudioScreen
import com.example.ui.screens.SpriteStudioScreen
import com.example.ui.screens.TextureStudioScreen
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TechMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        viewModel.attachDatabaseContext(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            UniversalAssetImporter.sweepEphemeralSources(applicationContext)
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PixelForgeApp(viewModel)
            }
        }
    }
}

enum class StudioTab(val title: String) {
    MODELS_3D("3D"),
    SPRITES("Sprites"),
    ASSET_SUITE("Suite"),
    FORGE_AI("Forge"),
    TEXTURES("Textures"),
    HARDWARE_LAB("Hardware")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelForgeApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showGalleriesOverlay by remember { mutableStateOf(false) }

    // Surface status notifications
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_fg),
                            contentDescription = "PixelForge Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "PIXELFORGE",
                            fontFamily = TechMono,
                            fontSize = 17.sp,
                            color = CyanNeon,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showGalleriesOverlay = !showGalleriesOverlay },
                        modifier = Modifier.testTag("top_bar_galleries_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = "Asset Galleries",
                            tint = if (showGalleriesOverlay) Color.White else CyanNeon
                        )
                    }
                    IconButton(
                        onClick = { viewModel.prepareSuiteManifestExport() },
                        modifier = Modifier.testTag("top_bar_suite_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Export Asset Suite Bundle",
                            tint = CyanNeon
                        )
                    }
                    IconButton(
                        onClick = { viewModel.prepareObjExport() },
                        modifier = Modifier.testTag("top_bar_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Asset",
                            tint = CyanNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val tabs = listOf(
                    StudioTab.MODELS_3D to (Icons.Filled.ViewInAr to Icons.Outlined.ViewInAr),
                    StudioTab.SPRITES to (Icons.Filled.GridView to Icons.Outlined.GridView),
                    StudioTab.ASSET_SUITE to (Icons.Filled.Inventory2 to Icons.Outlined.Inventory2),
                    StudioTab.FORGE_AI to (Icons.Filled.AutoAwesome to Icons.Outlined.AutoAwesome),
                    StudioTab.TEXTURES to (Icons.Filled.Palette to Icons.Outlined.Palette),
                    StudioTab.HARDWARE_LAB to (Icons.Filled.DeveloperBoard to Icons.Outlined.DeveloperBoard)
                )

                tabs.forEachIndexed { index, (tab, icons) ->
                    val selected = !showGalleriesOverlay && selectedTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            showGalleriesOverlay = false
                            selectedTab = index
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) icons.first else icons.second,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontFamily = TechMono,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF031B26),
                            selectedTextColor = CyanNeon,
                            indicatorColor = CyanNeon,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showGalleriesOverlay) {
                AssetGalleriesOverlayScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { showGalleriesOverlay = false }
                )
            } else {
                when (selectedTab) {
                    0 -> ModelStudioScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenHardwareLab = { selectedTab = 5 }
                    )
                    1 -> SpriteStudioScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateTo3D = { selectedTab = 0 }
                    )
                    2 -> AssetSuiteScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    3 -> ForgeStudioScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateTo3D = { selectedTab = 0 },
                        onNavigateToSprites = { selectedTab = 1 },
                        onNavigateToSuite = { selectedTab = 2 }
                    )
                    4 -> TextureStudioScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateTo3D = { selectedTab = 0 }
                    )
                    5 -> HardwareLabScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Export OBJ / Suite Manifest Dialog
    val exportText = uiState.exportDialogText
    if (exportText != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.exportDialogTitle, style = MaterialTheme.typography.titleMedium, color = CyanNeon)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ready for game engines, studio pipelines, or hardware verification:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    OutlinedTextField(
                        value = exportText.take(2500) + if (exportText.length > 2500) "\n... [${exportText.lines().size} lines total]" else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("export_obj_preview"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = TechMono, fontSize = 10.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(uiState.exportDialogTitle, exportText)
                        clipboard.setPrimaryClip(clip)
                        scope.launch {
                            snackbarHostState.showSnackbar("${uiState.exportDialogTitle} copied to clipboard!")
                        }
                        viewModel.dismissExportDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF031B26)),
                    modifier = Modifier.testTag("copy_obj_clipboard_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy to Clipboard", fontFamily = TechMono)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExportDialog() }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}
