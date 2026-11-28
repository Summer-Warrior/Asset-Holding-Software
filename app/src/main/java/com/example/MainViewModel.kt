package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiBuildTargetKind
import com.example.ai.AiImportSpriteSheetReviewer
import com.example.ai.AiMultiAssetBuilder
import com.example.ai.AiProviderProfile
import com.example.ai.AiProviderType
import com.example.ai.AiSpriteSheetReviewResult
import com.example.ai.AiTaskRole
import com.example.ai.AiTrainingDatasetExporter
import com.example.ai.ArtTheme
import com.example.ai.AssetAuditReport
import com.example.ai.AssetTarget
import com.example.ai.CanonicalAssetAndCodeAuditor
import com.example.ai.ContractComplianceReport
import com.example.ai.ForgeChatMessage
import com.example.ai.ForgeChatPromptBuilder
import com.example.ai.ForgeChatRole
import com.example.ai.GeminiAssetForge
import com.example.ai.GeminiAssetVariant
import com.example.ai.GeminiForgeReport
import com.example.ai.GeminiForgeUiState
import com.example.ai.GeminiInputImageSource
import com.example.ai.GeminiOperationMode
import com.example.ai.KnowledgeSourceEntry
import com.example.ai.KnowledgeSourceKind
import com.example.ai.MultiSourceContextRegistry
import com.example.ai.OmniAiRouter
import com.example.ai.OmniAiRoutingConfig
import com.example.ai.OpenRouterPkceHelper
import com.example.ai.OpenRouterPkceResult
import com.example.ai.PromptAssetPreset
import com.example.ai.PromptHardwareTier
import com.example.ai.PromptStudioEngine
import com.example.ai.PromptStudioOutput
import com.example.ai.UserAiTrainingProfile
import com.example.ai.UserAiTrainingProfileStore
import com.example.data.local.AssetGalleryEntity
import com.example.data.local.AssetGalleryItemEntity
import com.example.data.local.AssetGalleryRepository
import com.example.data.local.AssetGalleryWithItems
import com.example.data.local.AssetHistoryEntity
import com.example.data.local.AssetHistoryRepository
import com.example.data.local.MeshDiskStorage
import com.example.data.local.PixelForgeDatabase
import com.example.model.audio.AudioAssetEntry
import com.example.model.audio.AudioAssetFormat
import com.example.model.audio.AudioLoopPoints
import com.example.model.audio.AudioSynthAndDspEngine
import com.example.model.audio.AudioUsageCategory
import com.example.model.audio.ProceduralCrySpec
import com.example.model.audio.ScoreSequenceSpec
import com.example.model.audio.SoundchipDspProfile
import com.example.model.concept.ConceptAnalysis
import com.example.model.concept.ExtractedColor
import com.example.model.fusion.ModelSpriteFusionEngine
import com.example.model.fusion.ModelSpriteFusionMode
import com.example.model.fusion.ModelSpriteFusionResult
import com.example.model.hardware.ConsoleHardwareCatalog
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.HardwareCategory
import com.example.model.hardware.ReadingLevelMode
import com.example.model.importer.AssetImportSourceKind
import com.example.model.importer.ImportedAssetMetadata
import com.example.model.importer.ModelImportResult
import com.example.model.importer.SpriteImportResult
import com.example.model.importer.UniversalAssetImporter
import com.example.model.library.LibraryAssetType
import com.example.model.library.SavedLibraryAsset
import com.example.model.masterpack.AnchorPointType
import com.example.model.masterpack.FootAlignedSlicerEngine
import com.example.model.masterpack.FrameCadenceFps
import com.example.model.masterpack.HardwareVisualOutputTier
import com.example.model.masterpack.NormalizedAnchor
import com.example.model.masterpack.RigMetadata
import com.example.model.masterpack.ThreeMapRecolorEngine
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ObjParser
import com.example.model.model3d.PrimitiveShapeType
import com.example.model.model3d.ProceduralMeshes
import com.example.model.model3d.Scratch3DBuilderEngine
import com.example.model.model3d.Scratch3DPart
import com.example.model.model3d.Scratch3DProject
import com.example.model.sprite.PixelDrawTool
import com.example.model.sprite.SampleSpriteSheets
import com.example.model.sprite.ScratchSpriteProject
import com.example.model.sprite.SpriteCanvasEditorEngine
import com.example.model.sprite.SpriteSheetData
import com.example.model.suite.AssetSuiteSynthesizer
import com.example.model.suite.CharacterAssetFusionEngine
import com.example.model.suite.CharacterAssetModality
import com.example.model.suite.NamedCharacterFusionGroup
import com.example.model.suite.StagedCharacterAsset
import com.example.model.suite.SuiteBuildConfig
import com.example.model.suite.UnifiedAssetSuite
import com.example.model.texture.ProceduralMaterialPattern
import com.example.model.texture.ProceduralTextures
import com.example.model.texture.ScratchTextureEngine
import com.example.model.texture.ScratchTextureProject
import com.example.model.texture.TexturePreset
import com.example.model.vault.ByosCloudFolderManager
import com.example.model.vault.ByosStorageProvider
import com.example.model.vault.ByosSyncReport
import com.example.model.vault.ByosWorkspaceConfig
import com.example.model.vault.ColdVaultEndpointConfig
import com.example.model.vault.EncryptedColdVaultManager
import com.example.model.vault.HotCacheStats
import com.example.model.vault.HotPhoneLruCacheManager
import com.example.model.vault.SixStageAssetImporter
import com.example.model.vault.SixStageImportResult
import com.example.model.vault.SkinManifest
import com.example.model.vault.VaultAssetKind
import com.example.model.vault.VaultCatalogEntry
import com.example.model.vault.VaultSettingsAndCatalogStore
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

data class AppUiState(
    val currentMesh: Mesh3D = ProceduralMeshes.createSciFiStarfighter(),
    val currentSpriteSheet: SpriteSheetData = SampleSpriteSheets.createCyberNinjaSheet(),
    val activeTexture: Bitmap? = null,
    val activeTextureName: String = "Default Procedural",
    val conceptArtBitmap: Bitmap? = null,
    val extractedPalette: List<ExtractedColor> = emptyList(),
    val promptText: String = "Kaelen cyber-shinobi assault vanguard with dual plasma katana blades",
    val selectedTarget: AssetTarget = AssetTarget.MODEL_3D,
    val selectedTheme: ArtTheme = ArtTheme.CYBERPUNK,
    val isGenerating: Boolean = false,
    val statusMessage: String? = null,
    val exportDialogText: String? = null,
    val exportDialogTitle: String = "Export Wavefront 3D (.OBJ)",
    val texturePresets: List<TexturePreset> = ProceduralTextures.getAllPresets(),
    val hardwareCatalog: List<ConsoleHardwareSpec> = ConsoleHardwareCatalog.allSystemsWithFusions(),
    val selectedHardware: ConsoleHardwareSpec = ConsoleHardwareCatalog.getById("ps1"),
    val hardwareFilterCategory: HardwareCategory = HardwareCategory.ALL,
    val hardwareSimulationEnabled: Boolean = true,
    val showHardwareTileGrid: Boolean = true,
    val readingLevelMode: ReadingLevelMode = ReadingLevelMode.BOTH_LEVELS,
    // Imported asset tracking & Single Asset Suite state
    val hasImportedMesh: Boolean = false,
    val hasImportedSprite: Boolean = false,
    val hasImportedTexture: Boolean = false,
    val hasImportedConcept: Boolean = false,
    val hasEditedPrompt: Boolean = false,
    val suiteBuildConfig: SuiteBuildConfig = SuiteBuildConfig(),
    val activeAssetSuite: UnifiedAssetSuite? = null,
    val savedAssetSuites: List<UnifiedAssetSuite> = emptyList(),
    // Named-Character & Game -> Group -> Character Hierarchy State
    val autoFuseNamedCharacters: Boolean = true,
    val activeGameTagForImports: String = "General Studio",
    val activeGroupTagForImports: String = "Core Roster",
    val activeCharacterTagForImports: String = "Kaelen",
    val stagedCharacterPool: List<StagedCharacterAsset> = emptyList(),
    val characterFusionGroups: List<NamedCharacterFusionGroup> = emptyList(),
    val lastAutoFusionBanner: String? = null,
    // Personal Saved Asset Library (Inspected 3D Models, Sprites, and Pairs)
    val savedLibraryAssets: List<SavedLibraryAsset> = emptyList(),
    // Unbounded File & Photo Import Metadata Telemetry
    val lastModelImportMeta: ImportedAssetMetadata? = null,
    val lastSpriteImportMeta: ImportedAssetMetadata? = null,
    val stripSolidBgOnImport: Boolean = true,
    // Gemini API Asset Enhancement & Multi-Variant Generation State
    val geminiForgeState: GeminiForgeUiState = GeminiForgeUiState.Idle,
    val geminiInputImageSource: GeminiInputImageSource = GeminiInputImageSource.IMPORTED_CONCEPT_ART,
    val geminiOperationMode: GeminiOperationMode = GeminiOperationMode.ENHANCE_ASSET,
    val geminiCustomInstruction: String = "Enhance silhouette details, boost emissive armor contrast, and generate elemental/overclocked variants",
    val geminiGeneratedVariants: List<GeminiAssetVariant> = emptyList(),
    val lastGeminiReport: GeminiForgeReport? = null,
    // 3D Model + 2D Spritesheet Hybrid Asset Fusion State
    val selectedModelSpriteFusionMode: ModelSpriteFusionMode = ModelSpriteFusionMode.SPRITE_ARMOR_RELIEF_GRAFT,
    val modelSpriteBlendRatio: Float = 0.55f,
    val modelSpriteReliefDepth: Float = 0.50f,
    val modelSpriteHarmonizePalette: Boolean = true,
    val customModelSpriteFusionName: String = "",
    val activeModelSpriteFusion: ModelSpriteFusionResult? = null,
    val savedModelSpriteFusions: List<ModelSpriteFusionResult> = emptyList(),
    // Room Database Asset History (Generated & Imported 3D Models, 2D Spritesheets, Fusions)
    val roomAssetHistory: List<AssetHistoryEntity> = emptyList(),
    // AI File Import Spritesheet-in-Single-Image Reviewer State
    val lastAiFileImportReview: AiSpriteSheetReviewResult? = null,
    val isReviewingImportedFileWithAi: Boolean = false,
    // Custom Asset Galleries & Exhibitions
    val assetGalleries: List<AssetGalleryWithItems> = emptyList(),
    val selectedGalleryId: Long? = null,
    // Multi-AI & Multi-Source Hub (UI-safe profiles; never stores raw API keys)
    val aiProviderProfiles: List<AiProviderProfile> = OmniAiRouter.getAllProviderProfiles(),
    val omniAiRouting: OmniAiRoutingConfig = OmniAiRouter.getRoutingConfig(),
    val knowledgeSources: List<KnowledgeSourceEntry> = OmniAiRouter.getContextRegistry().getSources(),
    val isSigningInOpenRouterPkce: Boolean = false,
    val isFetchingKnowledgeUrl: Boolean = false,
    // Style-Locked True Fusion Prompt Studio State
    val promptHardwareTier: PromptHardwareTier = PromptHardwareTier.ARCADE_32BIT_60FPS,
    val promptAssetPreset: PromptAssetPreset = PromptAssetPreset.SINGLE_SPRITE_1X1,
    val includeHumanoidHeroicProportions: Boolean = false,
    // Canonical Asset & Official Spec Auditor State
    val lastAssetAuditReport: AssetAuditReport? = null,
    val isAuditingAsset: Boolean = false,
    val auditIntendedForPublicRelease: Boolean = false,
    val lastContractComplianceReport: ContractComplianceReport? = null,
    // 6-Layer Visual/Rig Master Pack State (MUST boot with 0 colors selected!)
    val selectedColorWheelHexes: List<String> = emptyList(),
    val activeMasterOutputTier: HardwareVisualOutputTier = HardwareVisualOutputTier.ARCADE_32BIT_60FPS,
    val activeRigMetadata: RigMetadata = RigMetadata.defaultForSprite("Cyber Blade Ninja", 48, 48, 12),
    val lastFootAlignmentSummary: String? = null,
    // First-Class Audio Asset Vault, 6-Stage Importer, AES-256-GCM Cold Vault & 200 MB LRU Hot Cache
    val audioVaultEntries: List<AudioAssetEntry> = AudioSynthAndDspEngine.createDefaultAudioPresets(),
    val selectedAudioEntryId: String? = audioVaultEntries.firstOrNull()?.id,
    val selectedSoundchipProfile: SoundchipDspProfile = SoundchipDspProfile.SNES_SPC700_BRR,
    val vaultCatalog: List<VaultCatalogEntry> = emptyList(),
    val activeSkinManifest: SkinManifest = SkinManifest.DEFAULT_CLEAN_ROOM,
    val lastSixStageImportResult: SixStageImportResult? = null,
    val hotCacheStats: HotCacheStats = HotCacheStats(0L, HotPhoneLruCacheManager.DEFAULT_MAX_CACHE_BYTES, 0, 0, 0f),
    val coldVaultEndpointConfig: ColdVaultEndpointConfig = ColdVaultEndpointConfig(),
    val isVaultPinConfigured: Boolean = false,
    val isVaultUnlocked: Boolean = true,
    // From-Scratch 2D Pixel & Animation Canvas Editor + Pure From-Scratch AI Asset Creator
    val scratchSpriteProject: ScratchSpriteProject = SpriteCanvasEditorEngine.createBlankProject("New Original Sprite", 32, 32, 1),
    val activeDrawTool: PixelDrawTool = PixelDrawTool.PENCIL,
    val activeBrushColorInt: Int = Color.rgb(0x00, 0xE5, 0xFF),
    val activeBrushSizePx: Int = 1,
    val scratchOnionSkinEnabled: Boolean = true,
    val scratchCanUndo: Boolean = false,
    val scratchCanRedo: Boolean = false,
    val scratchCreateAssetName: String = "",
    val isCreatingFromScratchWithAi: Boolean = false,
    val lastScratchAiCreationBitmap: Bitmap? = null,
    val lastScratchAiError: String? = null,
    // From-Scratch 3D Primitive Kitbash Builder & From-Scratch Texture Painter
    val scratch3DProject: Scratch3DProject = Scratch3DBuilderEngine.createEmptyProject("New Original 3D Model"),
    val scratchTextureProject: ScratchTextureProject = ScratchTextureEngine.createBlankTexture("New Original Texture", 32),
    val activeTextureBrushColorInt: Int = Color.rgb(0x00, 0xE5, 0xFF),
    val activeTextureBrushSizePx: Int = 1,
    // Closed-Loop AI Style Auditing & 1-Pass Auto-Repair
    val autoRetryAiStyleGate: Boolean = true,
    val lastAiAutoRepairNote: String? = null,
    // BYOS (Bring-Your-Own-Storage) Personal Cloud / SAF Folder Workspace
    val byosWorkspaceConfig: ByosWorkspaceConfig = ByosWorkspaceConfig(),
    val lastByosSyncReport: ByosSyncReport? = null,
    // AI Multi-Asset Builder (2D Sprite, 3D Model Blueprint, Texture, Audio Cry, 16-Step Chiptune)
    val selectedAiBuildTarget: AiBuildTargetKind = AiBuildTargetKind.SPRITE_2D,
    val lastAiBuildSummary: String? = null,
    // Target Asset FPS, Dual-Mode Forge Chat Director, and Unified User Training Profile
    val targetAssetFps: Int = 15,
    val forgeCreateUxMode: Int = 0, // 0 = Studio Form, 1 = AI Chat
    val forgeChatMessages: List<ForgeChatMessage> = listOf(
        ForgeChatMessage(
            id = "welcome",
            role = ForgeChatRole.ASSISTANT,
            text = "AI Asset Director online. Brainstorm sprites, 3D models, textures, cries, or chiptunes with me—then tap any Build button on my reply to materialize it directly into your studio. Your connected training profile syncs across both Form and Chat.",
            providerUsed = "Director"
        )
    ),
    val isForgeChatSending: Boolean = false,
    val userAiTrainingProfile: UserAiTrainingProfile = UserAiTrainingProfile()
)

class MainViewModel(
    private var historyRepository: AssetHistoryRepository? = null,
    private var galleryRepository: AssetGalleryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var appContext: Context? = null
    private var historyCollectionStarted = false
    private var galleryCollectionStarted = false
    private val scratchUndoStack = ArrayDeque<ScratchSpriteProject>()
    private val scratchRedoStack = ArrayDeque<ScratchSpriteProject>()
    private val maxScratchUndoSnapshots = 20

    init {
        val initialPool = CharacterAssetFusionEngine.createInitialCharacterAssetPool()
        val groups = CharacterAssetFusionEngine.clusterAssetsByNamedCharacter(initialPool)
        val fusedSuites = groups
            .filter { it.hasMultipleAssetsToFuse }
            .map { group -> CharacterAssetFusionEngine.fuseNamedCharacterGroup(group, unifyPalette = true) }

        val primarySuite = fusedSuites.firstOrNull() ?: run {
            val initialTex = ProceduralTextures.createCyberGrid()
            val state = _uiState.value
            AssetSuiteSynthesizer.buildUnifiedSuite(
                config = state.suiteBuildConfig,
                activeMesh = state.currentMesh,
                activeSpriteSheet = state.currentSpriteSheet,
                activeTexture = initialTex,
                activeTextureName = "Cyber Grid",
                activeConceptArt = null,
                promptText = state.promptText,
                hasUserImportedMesh = false,
                hasUserImportedSprite = false,
                hasUserImportedTexture = false,
                hasUserImportedConcept = false,
                hasUserEditedPrompt = false
            )
        }

        val initialLibrary = fusedSuites.take(2).mapIndexed { index, suite ->
            SavedLibraryAsset(
                id = "lib_init_$index",
                title = suite.name.substringBefore(" •"),
                characterTag = suite.name.substringBefore(" •"),
                type = LibraryAssetType.UNIFIED_PAIR,
                mesh = suite.mesh3D,
                spriteSheet = suite.spriteSheet,
                textureBitmap = suite.textureBitmap,
                inspectedYawDegrees = 45f,
                inspectedPitchDegrees = 20f,
                inspectedSpriteRotationDegrees = 0f,
                hardwareBadge = "PS1",
                metricsSummary = "${suite.mesh3D.vertexCount}v/${suite.mesh3D.faceCount}f • ${suite.spriteSheet.frames.size} frames"
            )
        }

        val initialModelSpriteFusion = ModelSpriteFusionEngine.fuseModelAndSpriteSheet(
            mesh = primarySuite.mesh3D,
            spriteSheet = primarySuite.spriteSheet,
            mode = ModelSpriteFusionMode.SPRITE_ARMOR_RELIEF_GRAFT,
            blendRatio = 0.55f,
            reliefDepth = 0.50f,
            harmonizePalette = true,
            characterTag = primarySuite.name.substringBefore(" •")
        )

        val initialRoomHistory = listOf(
            AssetHistoryEntity.fromStudioAssets(
                title = initialModelSpriteFusion.fusedName,
                characterTag = initialModelSpriteFusion.characterTag,
                assetType = "FUSED_3D_2D",
                sourceOrigin = "3D+2D FUSION",
                hardwareTarget = "PS1",
                metricsSummary = "${initialModelSpriteFusion.fusedMesh.vertexCount}v/${initialModelSpriteFusion.fusedMesh.faceCount}f + ${initialModelSpriteFusion.fusedSpriteSheet.frames.size}f",
                mesh = initialModelSpriteFusion.fusedMesh,
                spriteSheet = initialModelSpriteFusion.fusedSpriteSheet,
                paletteHexList = initialModelSpriteFusion.unifiedPalette.map { it.hex }
            ).copy(id = 1L),
            AssetHistoryEntity.fromStudioAssets(
                title = primarySuite.mesh3D.name,
                characterTag = primarySuite.name.substringBefore(" •"),
                assetType = "MODEL_3D",
                sourceOrigin = "PROCEDURAL FORGE",
                hardwareTarget = "PS1",
                metricsSummary = "${primarySuite.mesh3D.vertexCount}v/${primarySuite.mesh3D.faceCount}f",
                mesh = primarySuite.mesh3D
            ).copy(id = 2L),
            AssetHistoryEntity.fromStudioAssets(
                title = primarySuite.spriteSheet.name,
                characterTag = primarySuite.name.substringBefore(" •"),
                assetType = "SPRITE_2D",
                sourceOrigin = "PROCEDURAL FORGE",
                hardwareTarget = "PS1",
                metricsSummary = "${primarySuite.spriteSheet.frames.size} frames (${primarySuite.spriteSheet.frameWidth}×${primarySuite.spriteSheet.frameHeight}px)",
                spriteSheet = primarySuite.spriteSheet
            ).copy(id = 3L)
        )

        val initialAiReview = AiImportSpriteSheetReviewer.analyzeSpriteSheetInSingleImageDeterministically(
            bitmap = primarySuite.spriteSheet.sourceBitmap,
            fileName = "${primarySuite.spriteSheet.name.replace(' ', '_')}_sheet.png",
            userRequestedColumns = primarySuite.spriteSheet.columns,
            userRequestedRows = primarySuite.spriteSheet.rows
        )

        val starfighterSample = ProceduralMeshes.createSciFiStarfighter()
        val slimeSheetSample = SampleSpriteSheets.createDungeonSlimeSheet()
        val initialGalleries = listOf(
            AssetGalleryWithItems(
                gallery = AssetGalleryEntity(
                    id = 1L,
                    name = "Kaelen Vanguard Exhibition",
                    description = "Flagship 3D+2D cyber-shinobi models, fused armor silhouettes, and animation sheets.",
                    themeTag = "CYBERPUNK",
                    accentColorHex = "#00E5FF",
                    layoutStyle = "GRID_2COL",
                    createdAtMillis = 2000L
                ),
                items = listOf(
                    AssetGalleryItemEntity.fromStudioAssets(
                        galleryId = 1L,
                        title = initialModelSpriteFusion.fusedName,
                        characterTag = initialModelSpriteFusion.characterTag,
                        assetType = "FUSED_3D_2D",
                        sourceOrigin = "3D+2D FUSION",
                        hardwareTarget = "PS1",
                        metricsSummary = "${initialModelSpriteFusion.fusedMesh.vertexCount}v/${initialModelSpriteFusion.fusedMesh.faceCount}f + ${initialModelSpriteFusion.fusedSpriteSheet.frames.size}f",
                        curatorNote = "Relief-grafted 3D armor silhouette synchronized with 2D action frames.",
                        mesh = initialModelSpriteFusion.fusedMesh,
                        spriteSheet = initialModelSpriteFusion.fusedSpriteSheet
                    ).copy(id = 101L),
                    AssetGalleryItemEntity.fromStudioAssets(
                        galleryId = 1L,
                        title = primarySuite.spriteSheet.name,
                        characterTag = primarySuite.name.substringBefore(" •"),
                        assetType = "SPRITE_2D",
                        sourceOrigin = "STUDIO CURATION",
                        hardwareTarget = "SNES",
                        metricsSummary = "${primarySuite.spriteSheet.frames.size} frames (${primarySuite.spriteSheet.columns}×${primarySuite.spriteSheet.rows})",
                        curatorNote = "Full directional idle, dash, and plasma slash sprite sheet.",
                        spriteSheet = primarySuite.spriteSheet
                    ).copy(id = 102L)
                )
            ),
            AssetGalleryWithItems(
                gallery = AssetGalleryEntity(
                    id = 2L,
                    name = "Retro Silicon Showcase",
                    description = "Cross-console 3D crafts and pixel dungeon creatures benchmarked for classic hardware.",
                    themeTag = "RETRO SILICON",
                    accentColorHex = "#FF2A85",
                    layoutStyle = "CAROUSEL",
                    createdAtMillis = 1000L
                ),
                items = listOf(
                    AssetGalleryItemEntity.fromStudioAssets(
                        galleryId = 2L,
                        title = starfighterSample.name,
                        characterTag = "Starfighter NX-3",
                        assetType = "MODEL_3D",
                        sourceOrigin = "PROCEDURAL FORGE",
                        hardwareTarget = "N64",
                        metricsSummary = "${starfighterSample.vertexCount}v/${starfighterSample.faceCount}f",
                        curatorNote = "Low-poly delta interceptor ready for RDP Gouraud shading.",
                        mesh = starfighterSample
                    ).copy(id = 201L),
                    AssetGalleryItemEntity.fromStudioAssets(
                        galleryId = 2L,
                        title = slimeSheetSample.name,
                        characterTag = "Gloom Slime",
                        assetType = "SPRITE_2D",
                        sourceOrigin = "STUDIO CURATION",
                        hardwareTarget = "GBA",
                        metricsSummary = "${slimeSheetSample.frames.size} frames (${slimeSheetSample.columns}×${slimeSheetSample.rows})",
                        curatorNote = "Translucent bounce cycle tuned for handheld 15-bit palettes.",
                        spriteSheet = slimeSheetSample
                    ).copy(id = 202L)
                )
            )
        )

        _uiState.update {
            it.copy(
                currentMesh = primarySuite.mesh3D,
                currentSpriteSheet = primarySuite.spriteSheet,
                activeTexture = primarySuite.textureBitmap,
                activeTextureName = primarySuite.textureName,
                extractedPalette = primarySuite.unifiedPalette,
                activeAssetSuite = primarySuite,
                savedAssetSuites = fusedSuites.ifEmpty { listOf(primarySuite) },
                stagedCharacterPool = initialPool,
                characterFusionGroups = groups,
                lastAutoFusionBanner = "Auto-Fused ${fusedSuites.size} Named Characters (${groups.joinToString { g -> "${g.characterName}: ${g.totalAssetCount} assets" }})",
                savedLibraryAssets = initialLibrary,
                activeModelSpriteFusion = initialModelSpriteFusion,
                savedModelSpriteFusions = listOf(initialModelSpriteFusion),
                roomAssetHistory = initialRoomHistory,
                lastAiFileImportReview = initialAiReview,
                assetGalleries = initialGalleries,
                selectedGalleryId = initialGalleries.firstOrNull()?.gallery?.id
            )
        }

        historyRepository?.let { attachHistoryRepository(it) }
        galleryRepository?.let { attachGalleryRepository(it) }
    }

    /**
     * Attaches the Android Context's Room database (`PixelForgeDatabase`) to this ViewModel
     * and begins collecting reactive `Flow<List<AssetHistoryEntity>>` and Asset Gallery updates.
     */
    fun attachDatabaseContext(context: Context) {
        val appCtx = context.applicationContext
        this.appContext = appCtx
        OmniAiRouter.attachContext(appCtx)
        val loadedTrainingProfile = UserAiTrainingProfileStore.load(appCtx)
        _uiState.update { it.copy(userAiTrainingProfile = loadedTrainingProfile) }
        refreshOmniAiHubState()
        loadVaultPersistence(appCtx)
        val db = PixelForgeDatabase.getDatabase(appCtx)
        attachHistoryRepository(AssetHistoryRepository(db.assetHistoryDao(), appCtx))
        attachGalleryRepository(AssetGalleryRepository(db.assetGalleryDao(), appCtx))
    }

    private fun loadVaultPersistence(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loadedCatalog = VaultSettingsAndCatalogStore.loadCatalog(context)
                val loadedSkin = VaultSettingsAndCatalogStore.loadActiveSkinManifest(context)
                val loadedEndpoint = VaultSettingsAndCatalogStore.loadEndpointConfig(context)
                val pinConfigured = VaultSettingsAndCatalogStore.isPinConfigured(context)
                val loadedByosConfig = ByosCloudFolderManager.loadConfig(context)
                val stats = HotPhoneLruCacheManager.computeStats(loadedCatalog)
                _uiState.update { state ->
                    state.copy(
                        vaultCatalog = loadedCatalog,
                        activeSkinManifest = loadedSkin,
                        hotCacheStats = stats,
                        coldVaultEndpointConfig = loadedEndpoint,
                        isVaultPinConfigured = pinConfigured,
                        isVaultUnlocked = !pinConfigured,
                        byosWorkspaceConfig = loadedByosConfig
                    )
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Vault persistence load warning: ${e.message}")
            }
        }
    }

    /**
     * Attaches an [AssetHistoryRepository] (either persistent SQLite or Robolectric in-memory Room DB)
     * and synchronizes initial history records.
     */
    fun attachHistoryRepository(repository: AssetHistoryRepository) {
        this.historyRepository = repository
        if (historyCollectionStarted) return
        historyCollectionStarted = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingCount = repository.getHistoryCount()
                if (existingCount == 0) {
                    val seedEntities = _uiState.value.roomAssetHistory.map { it.copy(id = 0L) }
                    seedEntities.reversed().forEach { entity ->
                        repository.recordGeneratedAsset(entity)
                    }
                }
                repository.allHistoryFlow.collect { rows ->
                    _uiState.update { state ->
                        state.copy(roomAssetHistory = rows)
                    }
                }
            } catch (_: Exception) {
                // Keep in-memory fallback if DB stream is closed
            }
        }
    }

    /**
     * Attaches an [AssetGalleryRepository] and synchronizes user-created & curated Asset Galleries.
     */
    fun attachGalleryRepository(repository: AssetGalleryRepository) {
        this.galleryRepository = repository
        if (galleryCollectionStarted) return
        galleryCollectionStarted = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingCount = repository.getGalleryCount()
                if (existingCount == 0) {
                    val seedGalleries = _uiState.value.assetGalleries
                    seedGalleries.reversed().forEach { galleryWithItems ->
                        val insertedGalleryId = repository.createGallery(
                            galleryWithItems.gallery.copy(id = 0L)
                        )
                        galleryWithItems.items.reversed().forEach { item ->
                            repository.addItemToGallery(
                                item.copy(id = 0L, galleryId = insertedGalleryId)
                            )
                        }
                    }
                }
                repository.allGalleriesWithItemsFlow.collect { galleries ->
                    _uiState.update { state ->
                        val currentSelectedId = state.selectedGalleryId
                        val resolvedSelectedId = if (currentSelectedId != null && galleries.any { it.gallery.id == currentSelectedId }) {
                            currentSelectedId
                        } else {
                            galleries.firstOrNull()?.gallery?.id
                        }
                        state.copy(
                            assetGalleries = galleries,
                            selectedGalleryId = resolvedSelectedId
                        )
                    }
                }
            } catch (_: Exception) {
                // Keep in-memory fallback if DB stream is closed
            }
        }
    }

    /**
     * Persists a generated, fused, or imported asset into the Room `asset_history` table
     * (and immediately updates the UI state list if no Room DB is attached yet).
     */
    private fun recordAssetInRoomHistory(
        title: String,
        characterTag: String,
        assetType: String,
        sourceOrigin: String,
        hardwareTarget: String,
        metricsSummary: String,
        mesh: Mesh3D? = null,
        spriteSheet: SpriteSheetData? = null,
        paletteHexList: List<String> = emptyList(),
        aiReviewNotes: String? = null,
        gameTag: String = _uiState.value.activeGameTagForImports,
        groupTag: String = _uiState.value.activeGroupTagForImports
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val entity = AssetHistoryEntity.fromStudioAssets(
                title = title,
                characterTag = characterTag,
                assetType = assetType,
                sourceOrigin = sourceOrigin,
                hardwareTarget = hardwareTarget,
                metricsSummary = metricsSummary,
                mesh = mesh,
                spriteSheet = spriteSheet,
                paletteHexList = paletteHexList,
                aiReviewNotes = aiReviewNotes,
                gameTag = gameTag.ifBlank { "General Studio" },
                groupTag = groupTag.ifBlank { "Core Roster" }
            )
            val repo = historyRepository
            if (repo != null) {
                try {
                    val insertedId = repo.recordGeneratedAsset(entity)
                    val snapshot = repo.getAllHistorySnapshot()
                    _uiState.update { state ->
                        state.copy(
                            roomAssetHistory = snapshot.ifEmpty {
                                listOf(entity.copy(id = insertedId)) + state.roomAssetHistory
                            }
                        )
                    }
                } catch (_: Exception) {
                    _uiState.update { state ->
                        val nextId = (state.roomAssetHistory.maxOfOrNull { it.id } ?: 0L) + 1L
                        state.copy(roomAssetHistory = listOf(entity.copy(id = nextId)) + state.roomAssetHistory)
                    }
                }
            } else {
                _uiState.update { state ->
                    val nextId = (state.roomAssetHistory.maxOfOrNull { it.id } ?: 0L) + 1L
                    state.copy(roomAssetHistory = listOf(entity.copy(id = nextId)) + state.roomAssetHistory)
                }
            }
        }
    }

    /**
     * Records a snapshot of the current active 3D Model + 2D Spritesheet into the Room database history.
     */
    fun saveCurrentStudioStateToRoomHistory() {
        val snapshot = _uiState.value
        val title = "${snapshot.currentMesh.name} + ${snapshot.currentSpriteSheet.name}"
        val charTag = snapshot.activeCharacterTagForImports.ifBlank { "Studio" }
        val metrics = "${snapshot.currentMesh.vertexCount}v/${snapshot.currentMesh.faceCount}f • ${snapshot.currentSpriteSheet.frames.size}f (${snapshot.currentSpriteSheet.columns}×${snapshot.currentSpriteSheet.rows})"
        recordAssetInRoomHistory(
            title = title,
            characterTag = charTag,
            assetType = "UNIFIED_PAIR",
            sourceOrigin = "STUDIO SNAPSHOT",
            hardwareTarget = snapshot.selectedHardware.shortName,
            metricsSummary = metrics,
            mesh = snapshot.currentMesh,
            spriteSheet = snapshot.currentSpriteSheet,
            paletteHexList = snapshot.extractedPalette.map { it.hex },
            aiReviewNotes = snapshot.lastAiFileImportReview?.verdictTitle
        )
        _uiState.update {
            it.copy(statusMessage = "Recorded '$title' to Room Asset History Vault!")
        }
    }

    /**
     * Restores a 3D Model and/or 2D Spritesheet from a Room [AssetHistoryEntity] into the active Studio.
     */
    fun restoreAssetFromRoomHistory(entity: AssetHistoryEntity) {
        viewModelScope.launch(Dispatchers.Default) {
            val restoredMesh = entity.toMesh3D(appContext)
            val restoredSprite = entity.toSpriteSheetData()
            _uiState.update { state ->
                state.copy(
                    currentMesh = restoredMesh ?: state.currentMesh,
                    currentSpriteSheet = restoredSprite ?: state.currentSpriteSheet,
                    activeGameTagForImports = entity.gameTag.ifBlank { state.activeGameTagForImports },
                    activeGroupTagForImports = entity.groupTag.ifBlank { state.activeGroupTagForImports },
                    activeCharacterTagForImports = entity.characterTag.ifBlank { state.activeCharacterTagForImports },
                    statusMessage = "Restored '${entity.title}' (${entity.assetType}) from Room History Vault!"
                )
            }
        }
    }

    /**
     * Deletes a single history item from the Room `asset_history` table.
     */
    fun deleteAssetFromRoomHistory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = historyRepository
            if (repo != null) {
                try {
                    repo.deleteHistoryAsset(id)
                    val updated = repo.getAllHistorySnapshot()
                    _uiState.update { it.copy(roomAssetHistory = updated, statusMessage = "Deleted item #$id from Room History") }
                    return@launch
                } catch (_: Exception) {
                }
            }
            _uiState.update { state ->
                state.copy(
                    roomAssetHistory = state.roomAssetHistory.filterNot { it.id == id },
                    statusMessage = "Deleted item #$id from Room History"
                )
            }
        }
    }

    /**
     * Clears all records in the Room `asset_history` table.
     */
    fun clearRoomAssetHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository?.let {
                try {
                    it.clearAllHistory()
                } catch (_: Exception) {
                }
            }
            _uiState.update {
                it.copy(
                    roomAssetHistory = emptyList(),
                    statusMessage = "Cleared Room Database Asset History"
                )
            }
        }
    }

    fun saveInspectedAssetToLibrary(
        title: String,
        characterTag: String,
        type: LibraryAssetType,
        yawDeg: Float,
        pitchDeg: Float,
        spriteRotDeg: Float
    ) {
        val snapshot = _uiState.value
        val mesh = snapshot.currentMesh
        val sheet = snapshot.currentSpriteSheet
        val metrics = when (type) {
            LibraryAssetType.MODEL_3D -> "${mesh.vertexCount}v/${mesh.faceCount}f • Yaw ${yawDeg.toInt()}° Pitch ${pitchDeg.toInt()}°"
            LibraryAssetType.SPRITE_2D -> "${sheet.frames.size}f (${sheet.frameWidth}×${sheet.frameHeight}) • Rot ${spriteRotDeg.toInt()}°"
            LibraryAssetType.UNIFIED_PAIR -> "${mesh.vertexCount}v/${mesh.faceCount}f + ${sheet.frames.size}f • Yaw ${yawDeg.toInt()}°"
        }
        val entry = SavedLibraryAsset(
            id = "lib_${System.currentTimeMillis()}",
            title = title.ifBlank { mesh.name },
            characterTag = characterTag.ifBlank { snapshot.activeCharacterTagForImports.ifBlank { "Studio" } },
            type = type,
            mesh = if (type != LibraryAssetType.SPRITE_2D) mesh else null,
            spriteSheet = if (type != LibraryAssetType.MODEL_3D) sheet else null,
            textureBitmap = snapshot.activeTexture,
            inspectedYawDegrees = yawDeg,
            inspectedPitchDegrees = pitchDeg,
            inspectedSpriteRotationDegrees = spriteRotDeg,
            hardwareBadge = snapshot.selectedHardware.shortName,
            metricsSummary = metrics,
            gameTag = snapshot.activeGameTagForImports.ifBlank { "General Studio" },
            groupTag = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        )
        _uiState.update { state ->
            state.copy(
                savedLibraryAssets = listOf(entry) + state.savedLibraryAssets,
                statusMessage = "Saved '${entry.title}' (${type.label}) to your Asset Library!"
            )
        }
        recordAssetInRoomHistory(
            title = entry.title,
            characterTag = entry.characterTag,
            assetType = type.name,
            sourceOrigin = "LIBRARY INSPECTOR",
            hardwareTarget = entry.hardwareBadge,
            metricsSummary = metrics,
            mesh = entry.mesh,
            spriteSheet = entry.spriteSheet,
            gameTag = entry.gameTag,
            groupTag = entry.groupTag
        )
    }

    fun loadLibraryAssetIntoStudio(item: SavedLibraryAsset) {
        _uiState.update { state ->
            state.copy(
                currentMesh = item.mesh ?: state.currentMesh,
                currentSpriteSheet = item.spriteSheet ?: state.currentSpriteSheet,
                activeTexture = item.textureBitmap ?: state.activeTexture,
                activeGameTagForImports = item.gameTag.ifBlank { state.activeGameTagForImports },
                activeGroupTagForImports = item.groupTag.ifBlank { state.activeGroupTagForImports },
                activeCharacterTagForImports = item.characterTag,
                statusMessage = "Loaded '${item.title}' (${item.type.label}) from Asset Library!"
            )
        }
    }

    fun deleteLibraryAsset(id: String) {
        _uiState.update { state ->
            state.copy(
                savedLibraryAssets = state.savedLibraryAssets.filterNot { it.id == id },
                statusMessage = "Removed asset from Library"
            )
        }
    }

    fun setAutoFuseNamedCharacters(enabled: Boolean) {
        _uiState.update {
            it.copy(
                autoFuseNamedCharacters = enabled,
                statusMessage = if (enabled) {
                    "Automatic Named-Character Asset Fusion ENABLED"
                } else {
                    "Automatic Named-Character Asset Fusion PAUSED (Manual Mode)"
                }
            )
        }
        if (enabled) {
            fuseAllDetectedNamedCharacters()
        }
    }

    fun setActiveCharacterTagForImports(characterTag: String) {
        _uiState.update { it.copy(activeCharacterTagForImports = characterTag) }
    }

    fun setActiveGameTagForImports(gameTag: String) {
        _uiState.update {
            it.copy(
                activeGameTagForImports = gameTag.trim().ifBlank { "General Studio" }
            )
        }
    }

    fun setActiveGroupTagForImports(groupTag: String) {
        _uiState.update {
            it.copy(
                activeGroupTagForImports = groupTag.trim().ifBlank { "Core Roster" }
            )
        }
    }

    fun setStripSolidBgOnImport(enabled: Boolean) {
        _uiState.update {
            it.copy(
                stripSolidBgOnImport = enabled,
                statusMessage = if (enabled) {
                    "Strip Solid / Magenta BG ENABLED for sprite & sheet imports"
                } else {
                    "Strip Solid / Magenta BG disabled (keeping original opaque background)"
                }
            )
        }
    }

    /**
     * Automatically fuses all assets for a specific Named Character in the pool and activates the resulting suite.
     */
    fun fuseNamedCharacterByName(characterName: String) {
        val snapshot = _uiState.value
        val group = snapshot.characterFusionGroups.firstOrNull {
            it.characterName.equals(characterName, ignoreCase = true)
        } ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isGenerating = true) }
            val fusedSuite = CharacterAssetFusionEngine.fuseNamedCharacterGroup(
                group = group,
                unifyPalette = snapshot.suiteBuildConfig.unifyPaletteAcrossAssets
            )
            _uiState.update { state ->
                val updatedSaved = listOf(fusedSuite) + state.savedAssetSuites.filterNot {
                    it.name.equals(fusedSuite.name, ignoreCase = true)
                }.take(10)
                state.copy(
                    isGenerating = false,
                    activeAssetSuite = fusedSuite,
                    savedAssetSuites = updatedSaved,
                    currentMesh = fusedSuite.mesh3D,
                    currentSpriteSheet = fusedSuite.spriteSheet,
                    activeTexture = fusedSuite.textureBitmap,
                    activeTextureName = fusedSuite.textureName,
                    conceptArtBitmap = fusedSuite.conceptArtBitmap,
                    extractedPalette = fusedSuite.unifiedPalette,
                    suiteBuildConfig = state.suiteBuildConfig.copy(suiteName = fusedSuite.name),
                    lastAutoFusionBanner = "Fused ${group.totalAssetCount} assets sharing character '${group.characterName}' into '${fusedSuite.name}'!",
                    statusMessage = "Auto-Fused '${group.characterName}' (${group.totalAssetCount} assets across ${group.modalityCount} modalities)!"
                )
            }
        }
    }

    /**
     * Scans the entire staged asset pool and automatically fuses every Named Character that has >= 2 matching assets.
     */
    fun fuseAllDetectedNamedCharacters() {
        val snapshot = _uiState.value
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isGenerating = true) }
            val groups = CharacterAssetFusionEngine.clusterAssetsByNamedCharacter(snapshot.stagedCharacterPool)
            val eligibleGroups = groups.filter { it.hasMultipleAssetsToFuse }
            val fusedList = eligibleGroups.map { group ->
                CharacterAssetFusionEngine.fuseNamedCharacterGroup(
                    group = group,
                    unifyPalette = snapshot.suiteBuildConfig.unifyPaletteAcrossAssets
                )
            }
            val primary = fusedList.firstOrNull() ?: snapshot.activeAssetSuite
            _uiState.update { state ->
                val mergedSaved = (fusedList + state.savedAssetSuites)
                    .distinctBy { it.name.lowercase() }
                    .take(12)
                state.copy(
                    isGenerating = false,
                    characterFusionGroups = groups,
                    activeAssetSuite = primary,
                    savedAssetSuites = mergedSaved,
                    currentMesh = primary?.mesh3D ?: state.currentMesh,
                    currentSpriteSheet = primary?.spriteSheet ?: state.currentSpriteSheet,
                    activeTexture = primary?.textureBitmap ?: state.activeTexture,
                    activeTextureName = primary?.textureName ?: state.activeTextureName,
                    extractedPalette = primary?.unifiedPalette ?: state.extractedPalette,
                    lastAutoFusionBanner = "Batch Auto-Fused ${fusedList.size} Named Character Suites (${eligibleGroups.joinToString { "${it.characterName} [${it.totalAssetCount}]" }})",
                    statusMessage = "Auto-fused ${fusedList.size} Named Character Suites from ${snapshot.stagedCharacterPool.size} staged assets!"
                )
            }
        }
    }

    /**
     * Registers a newly imported or created asset into the StagedCharacterAsset pool and automatically
     * fuses it with any existing assets that share the same Named Character!
     */
    private fun registerAssetAndAutoFuseIfMatched(newAsset: StagedCharacterAsset) {
        val snapshot = _uiState.value
        val updatedPool = listOf(newAsset) + snapshot.stagedCharacterPool.filterNot { it.id == newAsset.id }
        val updatedGroups = CharacterAssetFusionEngine.clusterAssetsByNamedCharacter(updatedPool)
        val matchedGroup = updatedGroups.firstOrNull {
            it.characterName.equals(newAsset.characterName, ignoreCase = true)
        }

        if (snapshot.autoFuseNamedCharacters && matchedGroup != null && matchedGroup.hasMultipleAssetsToFuse) {
            val fusedSuite = CharacterAssetFusionEngine.fuseNamedCharacterGroup(
                group = matchedGroup,
                unifyPalette = snapshot.suiteBuildConfig.unifyPaletteAcrossAssets
            )
            _uiState.update { state ->
                val updatedSaved = listOf(fusedSuite) + state.savedAssetSuites.filterNot {
                    it.name.equals(fusedSuite.name, ignoreCase = true)
                }.take(10)
                state.copy(
                    stagedCharacterPool = updatedPool,
                    characterFusionGroups = updatedGroups,
                    activeAssetSuite = fusedSuite,
                    savedAssetSuites = updatedSaved,
                    currentMesh = fusedSuite.mesh3D,
                    currentSpriteSheet = fusedSuite.spriteSheet,
                    activeTexture = fusedSuite.textureBitmap,
                    activeTextureName = fusedSuite.textureName,
                    conceptArtBitmap = fusedSuite.conceptArtBitmap,
                    extractedPalette = fusedSuite.unifiedPalette,
                    suiteBuildConfig = state.suiteBuildConfig.copy(suiteName = fusedSuite.name),
                    lastAutoFusionBanner = "AUTO-FUSED '${matchedGroup.characterName}': Combined '${newAsset.assetTitle}' with ${matchedGroup.totalAssetCount - 1} sibling asset(s)!",
                    statusMessage = "Auto-Fused '${newAsset.assetTitle}' into Named Character Suite '${matchedGroup.characterName}' (${matchedGroup.totalAssetCount} assets)!"
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    stagedCharacterPool = updatedPool,
                    characterFusionGroups = updatedGroups,
                    lastAutoFusionBanner = "Staged '${newAsset.assetTitle}' under character '${newAsset.characterName}' (${matchedGroup?.totalAssetCount ?: 1} asset in group)."
                )
            }
        }
    }

    /**
     * Allows the user to bind/tag any active studio asset (3D Mesh, Sprite, Texture, Concept, or Lore)
     * to a chosen Named Character and immediately trigger automatic fusion.
     */
    fun stageActiveStudioAssetForCharacter(characterName: String, modality: CharacterAssetModality) {
        val snapshot = _uiState.value
        val existingNames = snapshot.characterFusionGroups.map { it.characterName }
        val resolvedChar = CharacterAssetFusionEngine.extractCharacterIdentity(
            rawTitleOrPrompt = characterName,
            explicitCharacterTag = characterName,
            existingCharacterNames = existingNames
        )

        viewModelScope.launch(Dispatchers.Default) {
            val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
            val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
            val staged = when (modality) {
                CharacterAssetModality.MESH_3D -> StagedCharacterAsset(
                    id = "staged_mesh_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - ${snapshot.currentMesh.name}",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.MESH_3D,
                    isUserImported = true,
                    mesh = snapshot.currentMesh,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
                CharacterAssetModality.SPRITESHEET_2D -> StagedCharacterAsset(
                    id = "staged_sprite_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - ${snapshot.currentSpriteSheet.name}",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.SPRITESHEET_2D,
                    isUserImported = true,
                    spriteSheet = snapshot.currentSpriteSheet,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
                CharacterAssetModality.TEXTURE_UV -> StagedCharacterAsset(
                    id = "staged_tex_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - ${snapshot.activeTextureName}",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.TEXTURE_UV,
                    isUserImported = true,
                    bitmap = snapshot.activeTexture,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
                CharacterAssetModality.CONCEPT_ART -> StagedCharacterAsset(
                    id = "staged_concept_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - Concept Reference",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.CONCEPT_ART,
                    isUserImported = true,
                    bitmap = snapshot.conceptArtBitmap ?: snapshot.activeAssetSuite?.conceptArtBitmap,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
                CharacterAssetModality.LORE_SPEC -> StagedCharacterAsset(
                    id = "staged_lore_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - Lore Spec",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.LORE_SPEC,
                    isUserImported = true,
                    loreText = snapshot.promptText,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
            }
            registerAssetAndAutoFuseIfMatched(staged)
        }
    }

    fun updateSuiteBuildConfig(transform: (SuiteBuildConfig) -> SuiteBuildConfig) {
        _uiState.update { it.copy(suiteBuildConfig = transform(it.suiteBuildConfig)) }
    }

    fun compileSingleAssetSuite(syncToAllStudios: Boolean = true) {
        val snapshot = _uiState.value
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isGenerating = true) }
            val compiledSuite = AssetSuiteSynthesizer.buildUnifiedSuite(
                config = snapshot.suiteBuildConfig,
                activeMesh = snapshot.currentMesh,
                activeSpriteSheet = snapshot.currentSpriteSheet,
                activeTexture = snapshot.activeTexture,
                activeTextureName = snapshot.activeTextureName,
                activeConceptArt = snapshot.conceptArtBitmap,
                promptText = snapshot.promptText,
                hasUserImportedMesh = snapshot.hasImportedMesh,
                hasUserImportedSprite = snapshot.hasImportedSprite,
                hasUserImportedTexture = snapshot.hasImportedTexture,
                hasUserImportedConcept = snapshot.hasImportedConcept,
                hasUserEditedPrompt = snapshot.hasEditedPrompt
            )
            _uiState.update { state ->
                val updatedList = listOf(compiledSuite) + state.savedAssetSuites.filterNot { it.name == compiledSuite.name }.take(9)
                if (syncToAllStudios) {
                    state.copy(
                        isGenerating = false,
                        activeAssetSuite = compiledSuite,
                        savedAssetSuites = updatedList,
                        currentMesh = compiledSuite.mesh3D,
                        currentSpriteSheet = compiledSuite.spriteSheet,
                        activeTexture = compiledSuite.textureBitmap,
                        activeTextureName = compiledSuite.textureName,
                        conceptArtBitmap = state.conceptArtBitmap ?: compiledSuite.conceptArtBitmap,
                        extractedPalette = compiledSuite.unifiedPalette,
                        statusMessage = "Compiled Single Asset Suite '${compiledSuite.name}' (${compiledSuite.cohesionScore}% Cohesion) & synced studios!"
                    )
                } else {
                    state.copy(
                        isGenerating = false,
                        activeAssetSuite = compiledSuite,
                        savedAssetSuites = updatedList,
                        statusMessage = "Compiled Single Asset Suite '${compiledSuite.name}'!"
                    )
                }
            }
        }
    }

    fun selectSavedSuite(suite: UnifiedAssetSuite) {
        _uiState.update {
            it.copy(
                activeAssetSuite = suite,
                currentMesh = suite.mesh3D,
                currentSpriteSheet = suite.spriteSheet,
                activeTexture = suite.textureBitmap,
                activeTextureName = suite.textureName,
                conceptArtBitmap = suite.conceptArtBitmap,
                extractedPalette = suite.unifiedPalette,
                suiteBuildConfig = it.suiteBuildConfig.copy(
                    suiteName = suite.name,
                    entityCategory = suite.category
                ),
                statusMessage = "Loaded Asset Suite '${suite.name}' into all studios"
            )
        }
    }

    fun prepareSuiteManifestExport() {
        val state = _uiState.value
        val suite = state.activeAssetSuite ?: return
        val alloc = AssetSuiteSynthesizer.analyzeSuiteOnHardware(suite, state.selectedHardware)
        val manifest = AssetSuiteSynthesizer.exportSuiteBundleManifest(suite, state.selectedHardware, alloc)
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export Single Asset Suite Bundle (${suite.name})",
                exportDialogText = manifest
            )
        }
    }

    fun selectHardware(spec: ConsoleHardwareSpec) {
        _uiState.update {
            it.copy(
                selectedHardware = spec,
                hardwareSimulationEnabled = true,
                statusMessage = "Hardware Target: ${spec.name} (${spec.releaseYear})"
            )
        }
    }

    fun activateFusedHardwareSpec(fusedSpec: ConsoleHardwareSpec) {
        ConsoleHardwareCatalog.registerCustomFusedSpec(fusedSpec)
        _uiState.update {
            it.copy(
                hardwareCatalog = ConsoleHardwareCatalog.allSystemsWithFusions(),
                selectedHardware = fusedSpec,
                hardwareSimulationEnabled = true,
                statusMessage = "Activated Cross-System Chip Fusion: ${fusedSpec.name}!"
            )
        }
    }

    fun setHardwareCategoryFilter(category: HardwareCategory) {
        _uiState.update { it.copy(hardwareFilterCategory = category) }
    }

    fun setReadingLevelMode(mode: ReadingLevelMode) {
        _uiState.update {
            it.copy(
                readingLevelMode = mode,
                statusMessage = "Readout Mode: ${mode.label}"
            )
        }
    }

    fun postStatusMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun toggleHardwareSimulation() {
        _uiState.update {
            val next = !it.hardwareSimulationEnabled
            it.copy(
                hardwareSimulationEnabled = next,
                statusMessage = if (next) "Enabled ${it.selectedHardware.shortName} Silicon Emulation" else "Switched to Unconstrained Studio View"
            )
        }
    }

    fun toggleHardwareTileGrid() {
        _uiState.update { it.copy(showHardwareTileGrid = !it.showHardwareTileGrid) }
    }

    fun bakeHardwareAdaptationToMesh() {
        val mesh = _uiState.value.currentMesh
        val spec = _uiState.value.selectedHardware
        viewModelScope.launch(Dispatchers.Default) {
            val adapted = HardwareAssetSimulator.adaptMeshToHardware(mesh, spec)
            _uiState.update {
                it.copy(
                    currentMesh = adapted,
                    statusMessage = "Baked ${spec.shortName} fixed-point & palette profile into 3D Mesh!"
                )
            }
        }
    }

    fun bakeHardwareQuantizationToSprite() {
        val sheet = _uiState.value.currentSpriteSheet
        val spec = _uiState.value.selectedHardware
        val frame = sheet.frames.firstOrNull() ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val report = HardwareAssetSimulator.simulateSpriteOnHardware(frame, spec, showTileGrid = false)
            val newSheet = SpriteSheetData.fromBitmap(
                report.simulatedBitmap,
                "${sheet.name.substringBefore(" [")} [${spec.shortName}]",
                1,
                1
            )
            _uiState.update {
                it.copy(
                    currentSpriteSheet = newSheet,
                    statusMessage = "Baked ${spec.shortName} (${spec.bitsPerPixelSprite}bpp) sprite quantization!"
                )
            }
        }
    }

    fun loadSampleMesh(type: String) {
        val mesh = when (type) {
            "starfighter" -> ProceduralMeshes.createSciFiStarfighter()
            "mech" -> ProceduralMeshes.createCyberMech()
            "chest" -> ProceduralMeshes.createTreasureChest()
            "crystal" -> ProceduralMeshes.createCrystalArtifact()
            else -> ProceduralMeshes.createSciFiStarfighter()
        }
        _uiState.update { it.copy(currentMesh = mesh, statusMessage = "Loaded ${mesh.name}") }
    }

    fun loadSampleSprite(type: String) {
        val sheet = when (type) {
            "ninja" -> SampleSpriteSheets.createCyberNinjaSheet()
            "slime" -> SampleSpriteSheets.createDungeonSlimeSheet()
            else -> SampleSpriteSheets.createCyberNinjaSheet()
        }
        _uiState.update { it.copy(currentSpriteSheet = sheet, statusMessage = "Loaded ${sheet.name}") }
    }

    fun importObjText(
        objContent: String,
        name: String = "Imported Mesh",
        characterTagOverride: String? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val snapshot = _uiState.value
                val existingChars = snapshot.characterFusionGroups.map { it.characterName }
                val tagToUse = characterTagOverride ?: snapshot.activeCharacterTagForImports
                val resolvedCharacter = CharacterAssetFusionEngine.extractCharacterIdentity(
                    rawTitleOrPrompt = name,
                    explicitCharacterTag = tagToUse,
                    existingCharacterNames = existingChars
                )
                val titledName = if (name.contains(resolvedCharacter, ignoreCase = true)) name else "$resolvedCharacter - $name"
                val byteLen = objContent.toByteArray(Charsets.UTF_8).size.toLong()
                val sizeLabel = UniversalAssetImporter.formatBytes(byteLen)
                val mesh = ObjParser.parse(
                    objContent = objContent,
                    name = titledName,
                    sourceKindLabel = "FILE",
                    sourceSizeLabel = sizeLabel
                )
                val meta = ImportedAssetMetadata(
                    fileName = titledName,
                    mimeType = "model/obj",
                    sourceKind = AssetImportSourceKind.RAW_TEXT,
                    byteSize = byteLen,
                    formattedSize = sizeLabel,
                    originalDimensions = "${mesh.rawVertexCount}v • ${mesh.rawFaceCount}f",
                    formatLabel = mesh.sourceFormat
                )
                val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
                val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
                val libEntry = SavedLibraryAsset(
                    id = "lib_mesh_${System.currentTimeMillis()}",
                    title = titledName,
                    characterTag = resolvedCharacter,
                    type = LibraryAssetType.MODEL_3D,
                    mesh = mesh,
                    hardwareBadge = snapshot.selectedHardware.shortName,
                    metricsSummary = "${mesh.rawVertexCount}v/${mesh.rawFaceCount}f • ${mesh.sourceFormat}",
                    sourceBadge = "FILE",
                    formatSummary = sizeLabel,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
                _uiState.update {
                    it.copy(
                        currentMesh = mesh,
                        hasImportedMesh = true,
                        lastModelImportMeta = meta,
                        savedLibraryAssets = listOf(libEntry) + it.savedLibraryAssets,
                        statusMessage = "Imported 3D Model '$titledName' (${mesh.vertexCount} verts, ${mesh.faceCount} faces • $sizeLabel)"
                    )
                }
                registerAssetAndAutoFuseIfMatched(
                    StagedCharacterAsset(
                        id = "imp_mesh_${System.currentTimeMillis()}",
                        assetTitle = titledName,
                        characterName = resolvedCharacter,
                        modality = CharacterAssetModality.MESH_3D,
                        isUserImported = true,
                        mesh = mesh,
                        gameTag = activeGame,
                        groupTag = activeGroup
                    )
                )
                recordAssetInRoomHistory(
                    title = titledName,
                    characterTag = resolvedCharacter,
                    assetType = "MODEL_3D",
                    sourceOrigin = "FILE IMPORT",
                    hardwareTarget = snapshot.selectedHardware.shortName,
                    metricsSummary = "${mesh.vertexCount}v/${mesh.faceCount}f • $sizeLabel",
                    mesh = mesh,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Failed to parse 3D Model: ${e.message}") }
            }
        }
    }

    /**
     * Handles an unbounded 3D Model import result from either a File (.OBJ, .STL, .PLY, .GLTF) or a Photo!
     */
    fun importModelFromResult(
        result: ModelImportResult,
        characterTagOverride: String? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val snapshot = _uiState.value
            val existingChars = snapshot.characterFusionGroups.map { it.characterName }
            val tagToUse = characterTagOverride ?: snapshot.activeCharacterTagForImports
            val resolvedCharacter = CharacterAssetFusionEngine.extractCharacterIdentity(
                rawTitleOrPrompt = result.mesh.name,
                explicitCharacterTag = tagToUse,
                existingCharacterNames = existingChars
            )
            val titledName = if (result.mesh.name.contains(resolvedCharacter, ignoreCase = true)) {
                result.mesh.name
            } else {
                "$resolvedCharacter - ${result.mesh.name}"
            }
            val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
            val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
            val updatedMesh = result.mesh.copy(name = titledName)
            val libEntry = SavedLibraryAsset(
                id = "lib_mesh_${System.currentTimeMillis()}",
                title = titledName,
                characterTag = resolvedCharacter,
                type = LibraryAssetType.MODEL_3D,
                mesh = updatedMesh,
                textureBitmap = result.previewThumbnail ?: snapshot.activeTexture,
                hardwareBadge = snapshot.selectedHardware.shortName,
                metricsSummary = "${updatedMesh.rawVertexCount}v/${updatedMesh.rawFaceCount}f • ${result.metadata.formatLabel}",
                sourceBadge = result.metadata.sourceKind.badge,
                formatSummary = "${result.metadata.formattedSize} • ${result.metadata.readabilityStatus}",
                gameTag = activeGame,
                groupTag = activeGroup
            )
            _uiState.update { state ->
                state.copy(
                    currentMesh = updatedMesh,
                    activeTexture = result.previewThumbnail ?: state.activeTexture,
                    activeTextureName = if (result.previewThumbnail != null) "$titledName Photo Texture" else state.activeTextureName,
                    hasImportedMesh = true,
                    lastModelImportMeta = result.metadata,
                    savedLibraryAssets = listOf(libEntry) + state.savedLibraryAssets,
                    statusMessage = "Imported 3D Model [${result.metadata.sourceKind.badge}] '$titledName' (${result.metadata.originalDimensions} • ${result.metadata.formattedSize})"
                )
            }
            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "imp_mesh_${System.currentTimeMillis()}",
                    assetTitle = titledName,
                    characterName = resolvedCharacter,
                    modality = CharacterAssetModality.MESH_3D,
                    isUserImported = true,
                    mesh = updatedMesh,
                    bitmap = result.previewThumbnail,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
            )
            // If this 3D model came from a photo/image file, review via AI whether a spritesheet was imported as a single picture image!
            val photoThumb = result.previewThumbnail
            val aiReview = if (photoThumb != null) {
                runAiFileImportSpriteSheetCheck(
                    bitmap = photoThumb,
                    fileName = result.metadata.fileName,
                    requestedCols = 1,
                    requestedRows = 1
                )
            } else null

            recordAssetInRoomHistory(
                title = titledName,
                characterTag = resolvedCharacter,
                assetType = "MODEL_3D",
                sourceOrigin = "${result.metadata.sourceKind.badge} IMPORT",
                hardwareTarget = snapshot.selectedHardware.shortName,
                metricsSummary = "${updatedMesh.rawVertexCount}v/${updatedMesh.rawFaceCount}f • ${result.metadata.formatLabel}",
                mesh = updatedMesh,
                aiReviewNotes = aiReview?.verdictTitle,
                gameTag = activeGame,
                groupTag = activeGroup
            )
        }
    }

    /**
     * Converts any imported Photo / Bitmap directly into a 3D Model with no limitations on source image size.
     */
    fun importModelFromPhotoBitmap(
        bitmap: Bitmap,
        name: String = "Photo 3D Model",
        targetGrid: Int = 28,
        depthScale: Float = 0.55f,
        characterTagOverride: String? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val byteEstimate = (bitmap.width.toLong() * bitmap.height.toLong() * 4L)
            val sizeLabel = UniversalAssetImporter.formatBytes(byteEstimate)
            val mesh = ProceduralMeshes.createVoxelReliefFromBitmap(
                bitmap = bitmap,
                targetGrid = targetGrid,
                depthScale = depthScale,
                name = name,
                sourceKindLabel = "PHOTO",
                sourceSizeLabel = "${bitmap.width}×${bitmap.height}px • $sizeLabel"
            )
            val meta = ImportedAssetMetadata(
                fileName = name,
                mimeType = "image/*",
                sourceKind = AssetImportSourceKind.PHOTO,
                byteSize = byteEstimate,
                formattedSize = sizeLabel,
                originalDimensions = "${bitmap.width}×${bitmap.height}px → ${mesh.vertexCount}v/${mesh.faceCount}f",
                formatLabel = "Photo-to-3D Relief Mesh (${targetGrid}×${targetGrid})"
            )
            importModelFromResult(
                result = ModelImportResult(mesh = mesh, metadata = meta, previewThumbnail = bitmap),
                characterTagOverride = characterTagOverride
            )
        }
    }

    fun importSpriteSheetBitmap(
        bitmap: Bitmap,
        name: String,
        cols: Int,
        rows: Int,
        characterTagOverride: String? = null,
        sourceKind: AssetImportSourceKind = AssetImportSourceKind.PHOTO,
        originalWidthOverride: Int? = null,
        originalHeightOverride: Int? = null,
        byteSizeOverride: Long = -1L
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val snapshot = _uiState.value
            val existingChars = snapshot.characterFusionGroups.map { it.characterName }
            val tagToUse = characterTagOverride ?: snapshot.activeCharacterTagForImports
            val resolvedCharacter = CharacterAssetFusionEngine.extractCharacterIdentity(
                rawTitleOrPrompt = name,
                explicitCharacterTag = tagToUse,
                existingCharacterNames = existingChars
            )
            val titledName = if (name.contains(resolvedCharacter, ignoreCase = true)) name else "$resolvedCharacter - $name"
            val origW = originalWidthOverride ?: bitmap.width
            val origH = originalHeightOverride ?: bitmap.height
            val bytes = if (byteSizeOverride > 0L) byteSizeOverride else (origW.toLong() * origH.toLong() * 4L)
            val sizeLabel = UniversalAssetImporter.formatBytes(bytes)

            val sheet = SpriteSheetData.fromBitmap(
                bitmap = bitmap,
                name = titledName,
                columns = cols,
                rows = rows,
                originalWidthOverride = origW,
                originalHeightOverride = origH,
                sourceKindLabel = sourceKind.badge,
                sourceSizeLabel = "${origW}×${origH}px • $sizeLabel"
            )
            val meta = ImportedAssetMetadata(
                fileName = titledName,
                mimeType = "image/*",
                sourceKind = sourceKind,
                byteSize = bytes,
                formattedSize = sizeLabel,
                originalDimensions = "${origW}×${origH}px (${sheet.columns}×${sheet.rows} • ${sheet.frames.size}f)",
                formatLabel = "${sourceKind.badge} Spritesheet Atlas"
            )
            val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
            val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
            val libEntry = SavedLibraryAsset(
                id = "lib_sprite_${System.currentTimeMillis()}",
                title = titledName,
                characterTag = resolvedCharacter,
                type = LibraryAssetType.SPRITE_2D,
                spriteSheet = sheet,
                hardwareBadge = snapshot.selectedHardware.shortName,
                metricsSummary = "${sheet.frames.size}f (${sheet.frameWidth}×${sheet.frameHeight}px) • ${origW}×${origH}px",
                sourceBadge = sourceKind.badge,
                formatSummary = "${sizeLabel} • ${meta.readabilityStatus}",
                gameTag = activeGame,
                groupTag = activeGroup
            )
            _uiState.update {
                it.copy(
                    currentSpriteSheet = sheet,
                    hasImportedSprite = true,
                    lastSpriteImportMeta = meta,
                    savedLibraryAssets = listOf(libEntry) + it.savedLibraryAssets,
                    statusMessage = "Imported Spritesheet [${sourceKind.badge}] '$titledName' (${origW}×${origH}px → ${sheet.frames.size} frames)"
                )
            }
            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "imp_sprite_${System.currentTimeMillis()}",
                    assetTitle = titledName,
                    characterName = resolvedCharacter,
                    modality = CharacterAssetModality.SPRITESHEET_2D,
                    isUserImported = true,
                    spriteSheet = sheet,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
            )
            val aiReview = runAiFileImportSpriteSheetCheck(
                bitmap = bitmap,
                fileName = titledName,
                requestedCols = cols,
                requestedRows = rows
            )
            recordAssetInRoomHistory(
                title = titledName,
                characterTag = resolvedCharacter,
                assetType = "SPRITE_2D",
                sourceOrigin = "${sourceKind.badge} IMPORT",
                hardwareTarget = snapshot.selectedHardware.shortName,
                metricsSummary = "${sheet.frames.size}f (${sheet.frameWidth}×${sheet.frameHeight}px) • ${origW}×${origH}px",
                spriteSheet = sheet,
                aiReviewNotes = aiReview.verdictTitle,
                gameTag = activeGame,
                groupTag = activeGroup
            )
        }
    }

    /**
     * Handles an unbounded Spritesheet import result from either a Photo or a File of any size!
     */
    fun importSpriteSheetFromResult(
        result: SpriteImportResult,
        characterTagOverride: String? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val snapshot = _uiState.value
            val existingChars = snapshot.characterFusionGroups.map { it.characterName }
            val tagToUse = characterTagOverride ?: snapshot.activeCharacterTagForImports
            val resolvedCharacter = CharacterAssetFusionEngine.extractCharacterIdentity(
                rawTitleOrPrompt = result.spriteSheet.name,
                explicitCharacterTag = tagToUse,
                existingCharacterNames = existingChars
            )
            val titledName = if (result.spriteSheet.name.contains(resolvedCharacter, ignoreCase = true)) {
                result.spriteSheet.name
            } else {
                "$resolvedCharacter - ${result.spriteSheet.name}"
            }
            val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
            val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
            val updatedSheet = result.spriteSheet.copy(name = titledName)
            val libEntry = SavedLibraryAsset(
                id = "lib_sprite_${System.currentTimeMillis()}",
                title = titledName,
                characterTag = resolvedCharacter,
                type = LibraryAssetType.SPRITE_2D,
                spriteSheet = updatedSheet,
                hardwareBadge = snapshot.selectedHardware.shortName,
                metricsSummary = "${updatedSheet.frames.size}f (${updatedSheet.frameWidth}×${updatedSheet.frameHeight}px) • ${result.metadata.formatLabel}",
                sourceBadge = result.metadata.sourceKind.badge,
                formatSummary = "${result.metadata.formattedSize} • ${result.metadata.readabilityStatus}",
                gameTag = activeGame,
                groupTag = activeGroup
            )
            _uiState.update { state ->
                state.copy(
                    currentSpriteSheet = updatedSheet,
                    hasImportedSprite = true,
                    lastSpriteImportMeta = result.metadata,
                    savedLibraryAssets = listOf(libEntry) + state.savedLibraryAssets,
                    statusMessage = "Imported Spritesheet [${result.metadata.sourceKind.badge}] '$titledName' (${result.metadata.originalDimensions} • ${result.metadata.formattedSize})"
                )
            }
            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "imp_sprite_${System.currentTimeMillis()}",
                    assetTitle = titledName,
                    characterName = resolvedCharacter,
                    modality = CharacterAssetModality.SPRITESHEET_2D,
                    isUserImported = true,
                    spriteSheet = updatedSheet,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
            )
            val aiReview = runAiFileImportSpriteSheetCheck(
                bitmap = updatedSheet.sourceBitmap,
                fileName = result.metadata.fileName,
                requestedCols = updatedSheet.columns,
                requestedRows = updatedSheet.rows
            )
            recordAssetInRoomHistory(
                title = titledName,
                characterTag = resolvedCharacter,
                assetType = "SPRITE_2D",
                sourceOrigin = "${result.metadata.sourceKind.badge} IMPORT",
                hardwareTarget = snapshot.selectedHardware.shortName,
                metricsSummary = "${updatedSheet.frames.size}f (${updatedSheet.frameWidth}×${updatedSheet.frameHeight}px) • ${result.metadata.formatLabel}",
                spriteSheet = updatedSheet,
                aiReviewNotes = aiReview.verdictTitle,
                gameTag = activeGame,
                groupTag = activeGroup
            )
        }
    }

    fun importConceptArtBitmap(
        bitmap: Bitmap,
        name: String = "Concept Art",
        characterTagOverride: String? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val snapshot = _uiState.value
            val existingChars = snapshot.characterFusionGroups.map { it.characterName }
            val tagToUse = characterTagOverride ?: snapshot.activeCharacterTagForImports
            val resolvedCharacter = CharacterAssetFusionEngine.extractCharacterIdentity(
                rawTitleOrPrompt = name,
                explicitCharacterTag = tagToUse,
                existingCharacterNames = existingChars
            )
            val titledName = if (name.contains(resolvedCharacter, ignoreCase = true)) name else "$resolvedCharacter - $name"
            val palette = ConceptAnalysis.extractPalette(bitmap, 8)
            _uiState.update {
                it.copy(
                    conceptArtBitmap = bitmap,
                    extractedPalette = palette,
                    hasImportedConcept = true,
                    statusMessage = "Imported Concept Art for '$resolvedCharacter': Extracted ${palette.size} dominant colors"
                )
            }
            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "imp_concept_${System.currentTimeMillis()}",
                    assetTitle = titledName,
                    characterName = resolvedCharacter,
                    modality = CharacterAssetModality.CONCEPT_ART,
                    isUserImported = true,
                    bitmap = bitmap
                )
            )
            // Also review imported concept art image via AI to check if the user actually imported a multi-frame spritesheet as a single picture!
            runAiFileImportSpriteSheetCheck(
                bitmap = bitmap,
                fileName = titledName,
                requestedCols = 1,
                requestedRows = 1
            )
        }
    }

    /**
     * Runs AI File Import Review on an imported bitmap/file to check whether a multi-frame
     * spritesheet is being added as a single picture image (1×1) or with mismatched grid dimensions.
     */
    private suspend fun runAiFileImportSpriteSheetCheck(
        bitmap: Bitmap,
        fileName: String,
        requestedCols: Int,
        requestedRows: Int,
        preferCloudVision: Boolean = true
    ): AiSpriteSheetReviewResult {
        _uiState.update { it.copy(isReviewingImportedFileWithAi = true) }
        val sharedContext = assembleSharedOmniContextBlock(
            queryHint = "$fileName spritesheet grid ${_uiState.value.activeCharacterTagForImports}",
            maxChars = MultiSourceContextRegistry.DEFAULT_MAX_CONTEXT_CHARS
        )
        val review = AiImportSpriteSheetReviewer.reviewImportedFile(
            bitmap = bitmap,
            fileName = fileName,
            userRequestedColumns = requestedCols,
            userRequestedRows = requestedRows,
            preferCloudVision = preferCloudVision,
            sharedContextBlock = sharedContext
        )
        _uiState.update { state ->
            val alertMsg = if (review.isSpriteSheetInSingleImage) {
                "AI File Review Alert: '${review.fileName}' looks like a ${review.recommendedColumns}×${review.recommendedRows} Spritesheet (${review.detectedFrameCount} frames) added as a ${requestedCols}×${requestedRows} picture!"
            } else {
                state.statusMessage
            }
            state.copy(
                lastAiFileImportReview = review,
                isReviewingImportedFileWithAi = false,
                statusMessage = alertMsg
            )
        }
        return review
    }

    /**
     * Triggers a deep Gemini Vision (`gemini-3.8-flash`) + Structural Grid Review on the active imported image/spritesheet.
     */
    fun runDeepGeminiImportFileReview() {
        val snapshot = _uiState.value
        val targetBitmap = snapshot.currentSpriteSheet.sourceBitmap
        val fileName = snapshot.lastSpriteImportMeta?.fileName ?: snapshot.currentSpriteSheet.name
        val cols = snapshot.currentSpriteSheet.columns
        val rows = snapshot.currentSpriteSheet.rows
        viewModelScope.launch(Dispatchers.Default) {
            runAiFileImportSpriteSheetCheck(
                bitmap = targetBitmap,
                fileName = fileName,
                requestedCols = cols,
                requestedRows = rows,
                preferCloudVision = true
            )
        }
    }

    /**
     * One-tap action when AI File Review detects that a spritesheet was added as a single picture image (1×1):
     * automatically slices the active image into the AI-recommended `recommendedColumns × recommendedRows` grid!
     */
    fun applyAiRecommendedSpriteSheetGrid(reviewOverride: AiSpriteSheetReviewResult? = null) {
        val snapshot = _uiState.value
        val review = reviewOverride ?: snapshot.lastAiFileImportReview ?: return
        val recCols = review.recommendedColumns.coerceAtLeast(1)
        val recRows = review.recommendedRows.coerceAtLeast(1)
        val sourceBitmap = snapshot.currentSpriteSheet.sourceBitmap

        viewModelScope.launch(Dispatchers.Default) {
            val reslicedSheet = SpriteSheetData.fromBitmap(
                bitmap = sourceBitmap,
                name = "${snapshot.currentSpriteSheet.name.substringBefore(" [")} [${recCols}×${recRows} AI Sliced]",
                columns = recCols,
                rows = recRows,
                originalWidthOverride = snapshot.currentSpriteSheet.originalWidth,
                originalHeightOverride = snapshot.currentSpriteSheet.originalHeight,
                sourceKindLabel = snapshot.currentSpriteSheet.sourceKindLabel,
                sourceSizeLabel = snapshot.currentSpriteSheet.sourceSizeLabel
            )
            val updatedReview = review.copy(
                userRequestedColumns = recCols,
                userRequestedRows = recRows,
                isSpriteSheetInSingleImage = false,
                verdictTitle = "Auto-Sliced into ${recCols}×${recRows} Spritesheet (${reslicedSheet.frames.size} Frames)",
                aiReviewSummary = "Applied AI File Review recommendation: sliced '${review.fileName}' into $recCols columns × $recRows rows (${reslicedSheet.frameWidth}×${reslicedSheet.frameHeight}px per frame)."
            )
            _uiState.update { state ->
                state.copy(
                    currentSpriteSheet = reslicedSheet,
                    lastAiFileImportReview = updatedReview,
                    statusMessage = "AI Auto-Sliced '${reslicedSheet.name}' into ${recCols}×${recRows} (${reslicedSheet.frames.size} frames)!"
                )
            }
            recordAssetInRoomHistory(
                title = reslicedSheet.name,
                characterTag = snapshot.activeCharacterTagForImports.ifBlank { "Kaelen" },
                assetType = "SPRITE_2D",
                sourceOrigin = "AI AUTO-SLICED SHEET",
                hardwareTarget = snapshot.selectedHardware.shortName,
                metricsSummary = "${reslicedSheet.frames.size}f (${reslicedSheet.frameWidth}×${reslicedSheet.frameHeight}px) • ${recCols}×${recRows}",
                spriteSheet = reslicedSheet,
                aiReviewNotes = updatedReview.verdictTitle
            )
        }
    }

    /**
     * Keeps the imported image as a 1×1 single picture image when the user dismisses the AI grid suggestion.
     */
    fun keepImportedFileAsSingleImage() {
        val currentReview = _uiState.value.lastAiFileImportReview ?: return
        _uiState.update { state ->
            state.copy(
                lastAiFileImportReview = currentReview.copy(
                    isSpriteSheetInSingleImage = false,
                    verdictTitle = "Kept as 1×1 Single Picture Image (User Confirmed)"
                ),
                statusMessage = "Kept '${currentReview.fileName}' as a 1×1 single picture image."
            )
        }
    }

    fun convertConceptToSprite(resolution: Int = 32) {
        val concept = _uiState.value.conceptArtBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val spriteBmp = ConceptAnalysis.convertConceptToSprite(concept, resolution)
            val charTag = _uiState.value.activeCharacterTagForImports.ifBlank { "Concept" }
            val sheet = SpriteSheetData.fromBitmap(spriteBmp, "$charTag Sprite ($resolution x $resolution)", 1, 1)
            _uiState.update {
                it.copy(
                    currentSpriteSheet = sheet,
                    statusMessage = "Generated $resolution px sprite from concept art!"
                )
            }
        }
    }

    fun convertConceptTo3D() {
        val concept = _uiState.value.conceptArtBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val mesh = ProceduralMeshes.createVoxelReliefFromBitmap(concept, targetGrid = 24)
            _uiState.update {
                it.copy(
                    currentMesh = mesh,
                    statusMessage = "Extruded 3D Voxel Mesh with ${mesh.vertexCount} vertices!"
                )
            }
        }
    }

    fun convertSpriteTo3D() {
        val frame = _uiState.value.currentSpriteSheet.frames.firstOrNull() ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val mesh = ProceduralMeshes.createVoxelReliefFromBitmap(frame, targetGrid = 20)
            _uiState.update {
                it.copy(
                    currentMesh = mesh,
                    statusMessage = "Extruded active sprite into 3D mesh (${mesh.vertexCount} vertices)!"
                )
            }
        }
    }

    fun updatePromptText(text: String) {
        _uiState.update { it.copy(promptText = text, hasEditedPrompt = true) }
    }

    fun setTarget(target: AssetTarget) {
        _uiState.update { it.copy(selectedTarget = target) }
    }

    fun setTheme(theme: ArtTheme) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun generateFromPrompt() {
        val prompt = _uiState.value.promptText
        val target = _uiState.value.selectedTarget
        val theme = _uiState.value.selectedTheme

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isGenerating = true) }
            val result = GeminiAssetForge.generateAssetFromDescription(prompt, target, theme)
            val snapshot = _uiState.value
            val resolvedCharacter = CharacterAssetFusionEngine.extractCharacterIdentity(
                rawTitleOrPrompt = prompt,
                explicitCharacterTag = snapshot.activeCharacterTagForImports,
                existingCharacterNames = snapshot.characterFusionGroups.map { it.characterName }
            )
            _uiState.update { state ->
                state.copy(
                    isGenerating = false,
                    currentMesh = result.mesh ?: state.currentMesh,
                    currentSpriteSheet = result.spriteSheet ?: state.currentSpriteSheet,
                    statusMessage = "Synthesized: ${result.title} (Character: $resolvedCharacter)"
                )
            }
            if (result.mesh != null) {
                registerAssetAndAutoFuseIfMatched(
                    StagedCharacterAsset(
                        id = "gen_mesh_${System.currentTimeMillis()}",
                        assetTitle = "$resolvedCharacter - ${result.title} [3D]",
                        characterName = resolvedCharacter,
                        modality = CharacterAssetModality.MESH_3D,
                        isUserImported = true,
                        mesh = result.mesh
                    )
                )
            }
            if (result.spriteSheet != null) {
                registerAssetAndAutoFuseIfMatched(
                    StagedCharacterAsset(
                        id = "gen_sprite_${System.currentTimeMillis()}",
                        assetTitle = "$resolvedCharacter - ${result.title} [Sprite]",
                        characterName = resolvedCharacter,
                        modality = CharacterAssetModality.SPRITESHEET_2D,
                        isUserImported = true,
                        spriteSheet = result.spriteSheet
                    )
                )
            }
            recordAssetInRoomHistory(
                title = result.title,
                characterTag = resolvedCharacter,
                assetType = when {
                    result.mesh != null && result.spriteSheet != null -> "UNIFIED_PAIR"
                    result.mesh != null -> "MODEL_3D"
                    else -> "SPRITE_2D"
                },
                sourceOrigin = "PROCEDURAL FORGE",
                hardwareTarget = snapshot.selectedHardware.shortName,
                metricsSummary = "${result.mesh?.vertexCount ?: 0}v/${result.mesh?.faceCount ?: 0}f • ${result.spriteSheet?.frames?.size ?: 0}f",
                mesh = result.mesh,
                spriteSheet = result.spriteSheet
            )
        }
    }

    fun applyPresetTexture(preset: TexturePreset) {
        _uiState.update {
            it.copy(
                activeTexture = preset.bitmap,
                activeTextureName = preset.name,
                statusMessage = "Applied texture: ${preset.name}"
            )
        }
    }

    fun importCustomTexture(
        bitmap: Bitmap,
        name: String = "Imported Texture",
        characterTagOverride: String? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val snapshot = _uiState.value
            val existingChars = snapshot.characterFusionGroups.map { it.characterName }
            val tagToUse = characterTagOverride ?: snapshot.activeCharacterTagForImports
            val resolvedCharacter = CharacterAssetFusionEngine.extractCharacterIdentity(
                rawTitleOrPrompt = name,
                explicitCharacterTag = tagToUse,
                existingCharacterNames = existingChars
            )
            val titledName = if (name.contains(resolvedCharacter, ignoreCase = true)) name else "$resolvedCharacter - $name"
            _uiState.update {
                it.copy(
                    activeTexture = bitmap,
                    activeTextureName = titledName,
                    hasImportedTexture = true,
                    statusMessage = "Imported 3D Texture '$titledName' for character '$resolvedCharacter'"
                )
            }
            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "imp_tex_${System.currentTimeMillis()}",
                    assetTitle = titledName,
                    characterName = resolvedCharacter,
                    modality = CharacterAssetModality.TEXTURE_UV,
                    isUserImported = true,
                    bitmap = bitmap
                )
            )
        }
    }

    fun setGeminiInputImageSource(source: GeminiInputImageSource) {
        _uiState.update { it.copy(geminiInputImageSource = source) }
    }

    fun setGeminiOperationMode(mode: GeminiOperationMode) {
        _uiState.update { it.copy(geminiOperationMode = mode) }
    }

    fun updateGeminiCustomInstruction(instruction: String) {
        _uiState.update { it.copy(geminiCustomInstruction = instruction) }
    }

    fun selectArcadeComicCelStylePreset() {
        _uiState.update {
            it.copy(
                selectedTheme = ArtTheme.ARCADE_COMIC_CEL,
                geminiCustomInstruction = GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_BLOCK,
                statusMessage = "Selected '${GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_PRESET_TITLE}' style preset!"
            )
        }
    }

    /**
     * Resolves the currently selected image Bitmap to send to Gemini alongside the asset description.
     */
    fun resolveSelectedGeminiInputBitmap(
        state: AppUiState = _uiState.value,
        sourceOverride: GeminiInputImageSource? = null
    ): Bitmap? {
        val source = sourceOverride ?: state.geminiInputImageSource
        return when (source) {
            GeminiInputImageSource.IMPORTED_CONCEPT_ART ->
                state.conceptArtBitmap ?: state.activeTexture ?: state.currentSpriteSheet.frames.firstOrNull()
            GeminiInputImageSource.ACTIVE_SPRITESHEET ->
                state.currentSpriteSheet.frames.firstOrNull() ?: state.currentSpriteSheet.sourceBitmap
            GeminiInputImageSource.ACTIVE_3D_TEXTURE ->
                state.activeTexture ?: state.conceptArtBitmap
            GeminiInputImageSource.DESCRIPTION_ONLY ->
                null
        }
    }

    /**
     * Invokes the Gemini API (`gemini-3.8-flash` for structured enhancement/variants or
     * `gemini-nano-banana-2.1` for runtime visual synthesis) using the imported asset image and/or description.
     */
    fun runGeminiEnhancementOrVariantGeneration(
        modeOverride: GeminiOperationMode? = null,
        sourceOverride: GeminiInputImageSource? = null
    ) {
        val snapshot = _uiState.value
        val mode = modeOverride ?: snapshot.geminiOperationMode
        val source = sourceOverride ?: snapshot.geminiInputImageSource
        val inputBitmap = resolveSelectedGeminiInputBitmap(snapshot, source)

        viewModelScope.launch {
            val loadingText = when (mode) {
                GeminiOperationMode.ENHANCE_ASSET ->
                    "Gemini (gemini-3.8-flash) analyzing ${if (inputBitmap != null) "${source.badge} + description" else "description"} for automatic enhancement..."
                GeminiOperationMode.GENERATE_VARIANTS ->
                    "Gemini (gemini-3.8-flash) synthesizing 4 playable 3D & 2D asset variants..."
                GeminiOperationMode.AI_IMAGE_SYNTHESIS ->
                    "Gemini (gemini-nano-banana-2.1) rendering visual asset image (may take 30–60s)..."
            }
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    geminiOperationMode = mode,
                    geminiInputImageSource = source,
                    geminiForgeState = GeminiForgeUiState.Loading(loadingText)
                )
            }

            try {
                val sharedContext = assembleSharedOmniContextBlock(
                    queryHint = "${snapshot.promptText} ${snapshot.geminiCustomInstruction} ${snapshot.activeCharacterTagForImports}",
                    maxChars = MultiSourceContextRegistry.DEFAULT_MAX_CONTEXT_CHARS
                )
                val rawReport = if (mode == GeminiOperationMode.AI_IMAGE_SYNTHESIS) {
                    GeminiAssetForge.synthesizeAiVariantImageWithGemini(
                        promptDescription = snapshot.promptText,
                        customInstruction = snapshot.geminiCustomInstruction,
                        inputBitmap = inputBitmap,
                        inputSource = source,
                        theme = snapshot.selectedTheme,
                        hardwareName = snapshot.selectedHardware.name,
                        explicitCharacterTag = snapshot.activeCharacterTagForImports,
                        sharedContextBlock = sharedContext,
                        hardwareTier = snapshot.promptHardwareTier,
                        autoStripChromaKey = snapshot.stripSolidBgOnImport,
                        autoRetryOnStyleFail = snapshot.autoRetryAiStyleGate
                    )
                } else {
                    GeminiAssetForge.enhanceOrGenerateVariantsWithGemini(
                        promptDescription = snapshot.promptText,
                        customInstruction = snapshot.geminiCustomInstruction,
                        inputBitmap = inputBitmap,
                        inputSource = source,
                        operationMode = mode,
                        theme = snapshot.selectedTheme,
                        hardwareName = snapshot.selectedHardware.name,
                        baseMesh = snapshot.currentMesh,
                        baseSpriteSheet = snapshot.currentSpriteSheet,
                        explicitCharacterTag = snapshot.activeCharacterTagForImports,
                        sharedContextBlock = sharedContext
                    )
                }

                // Ensure every AI enhancement/variant flow runs automatic deterministic style auditing
                val autoAudit = rawReport.autoAuditReport ?: rawReport.primarySpriteSheet.frames.firstOrNull()?.let { frameBmp ->
                    CanonicalAssetAndCodeAuditor.auditAsset(
                        bitmap = frameBmp,
                        assetName = rawReport.enhancedTitle,
                        promptUsed = "${snapshot.promptText} ${snapshot.geminiCustomInstruction}".trim(),
                        hardwareTier = snapshot.promptHardwareTier,
                        expectedChromaKeyHex = "#FF00FF",
                        referenceBitmap = inputBitmap
                    )
                }
                val report = rawReport.copy(autoAuditReport = autoAudit)
                val repairNote = formatAutoStyleRepairNote(
                    report = autoAudit,
                    attempted = report.autoRepairAttempted,
                    improved = report.autoRepairImprovedScore
                )
                val auditMetricsTag = autoAudit?.let { " • Audit ${it.styleFidelityScore}% (${it.fidelityTag.badge})" } ?: ""

                val libEntry = SavedLibraryAsset(
                    id = "lib_gemini_${System.currentTimeMillis()}",
                    title = report.enhancedTitle,
                    characterTag = report.characterIdentity,
                    type = LibraryAssetType.UNIFIED_PAIR,
                    mesh = report.primaryMesh,
                    spriteSheet = report.primarySpriteSheet,
                    textureBitmap = report.aiSynthesizedBitmap ?: snapshot.activeTexture,
                    hardwareBadge = snapshot.selectedHardware.shortName,
                    metricsSummary = "${report.primaryMesh.vertexCount}v/${report.primaryMesh.faceCount}f • ${report.variants.size} AI Variants$auditMetricsTag",
                    sourceBadge = "GEMINI AI",
                    formatSummary = report.modelNameUsed
                )

                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        geminiForgeState = GeminiForgeUiState.Success(report),
                        geminiGeneratedVariants = report.variants,
                        lastGeminiReport = report,
                        lastAssetAuditReport = autoAudit ?: state.lastAssetAuditReport,
                        lastAiAutoRepairNote = repairNote ?: state.lastAiAutoRepairNote,
                        currentMesh = report.primaryMesh,
                        currentSpriteSheet = report.primarySpriteSheet,
                        conceptArtBitmap = report.aiSynthesizedBitmap ?: state.conceptArtBitmap,
                        extractedPalette = report.enhancedPalette.ifEmpty { state.extractedPalette },
                        activeCharacterTagForImports = report.characterIdentity,
                        savedLibraryAssets = listOf(libEntry) + state.savedLibraryAssets,
                        statusMessage = "Gemini (${report.modelNameUsed}): Enhanced '${report.enhancedTitle}' & generated ${report.variants.size} variants$auditMetricsTag!"
                    )
                }

                // Register enhanced 3D and 2D assets in the Named-Character Fusion pool
                registerAssetAndAutoFuseIfMatched(
                    StagedCharacterAsset(
                        id = "gemini_mesh_${System.currentTimeMillis()}",
                        assetTitle = "${report.enhancedTitle} [Gemini 3D]",
                        characterName = report.characterIdentity,
                        modality = CharacterAssetModality.MESH_3D,
                        isUserImported = true,
                        mesh = report.primaryMesh
                    )
                )
                registerAssetAndAutoFuseIfMatched(
                    StagedCharacterAsset(
                        id = "gemini_sprite_${System.currentTimeMillis()}",
                        assetTitle = "${report.enhancedTitle} [Gemini Sprite]",
                        characterName = report.characterIdentity,
                        modality = CharacterAssetModality.SPRITESHEET_2D,
                        isUserImported = true,
                        spriteSheet = report.primarySpriteSheet
                    )
                )
                recordAssetInRoomHistory(
                    title = report.enhancedTitle,
                    characterTag = report.characterIdentity,
                    assetType = "GEMINI_VARIANT",
                    sourceOrigin = "GEMINI AI (${report.modelNameUsed})",
                    hardwareTarget = snapshot.selectedHardware.shortName,
                    metricsSummary = "${report.primaryMesh.vertexCount}v/${report.primaryMesh.faceCount}f • ${report.variants.size} AI Variants$auditMetricsTag",
                    mesh = report.primaryMesh,
                    spriteSheet = report.primarySpriteSheet,
                    paletteHexList = report.enhancedPalette.map { it.hex }
                )
            } catch (e: Throwable) {
                val errorMessage = GeminiAssetForge.mapGeminiException(e)
                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        geminiForgeState = GeminiForgeUiState.Error(errorMessage),
                        statusMessage = "Gemini Error: $errorMessage"
                    )
                }
            }
        }
    }

    /**
     * Applies any Gemini-generated variant into the active 3D Model and 2D Sprite studios.
     */
    fun applyGeminiVariantToStudio(variant: GeminiAssetVariant) {
        _uiState.update { state ->
            state.copy(
                currentMesh = variant.mesh,
                currentSpriteSheet = variant.spriteSheet,
                extractedPalette = variant.palette.ifEmpty { state.extractedPalette },
                activeCharacterTagForImports = variant.characterTag,
                statusMessage = "Applied Gemini Variant '${variant.variantName}' [${variant.variantBadge}] to 3D & 2D Studios!"
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "var_mesh_${System.currentTimeMillis()}",
                    assetTitle = "${variant.variantName} [3D Variant]",
                    characterName = variant.characterTag,
                    modality = CharacterAssetModality.MESH_3D,
                    isUserImported = true,
                    mesh = variant.mesh
                )
            )
        }
    }

    /**
     * Saves a specific Gemini-generated variant directly into the Personal Asset Library.
     */
    fun saveGeminiVariantToLibrary(variant: GeminiAssetVariant) {
        val snapshot = _uiState.value
        val entry = SavedLibraryAsset(
            id = "lib_var_${System.currentTimeMillis()}",
            title = variant.variantName,
            characterTag = variant.characterTag,
            type = LibraryAssetType.UNIFIED_PAIR,
            mesh = variant.mesh,
            spriteSheet = variant.spriteSheet,
            textureBitmap = snapshot.activeTexture,
            hardwareBadge = snapshot.selectedHardware.shortName,
            metricsSummary = "${variant.mesh.vertexCount}v/${variant.mesh.faceCount}f + ${variant.spriteSheet.frames.size}f • ${variant.variantBadge}",
            sourceBadge = "GEMINI VARIANT",
            formatSummary = "${variant.primaryHex} / ${variant.accentHex}"
        )
        _uiState.update { state ->
            state.copy(
                savedLibraryAssets = listOf(entry) + state.savedLibraryAssets,
                statusMessage = "Saved Gemini Variant '${variant.variantName}' to Asset Library!"
            )
        }
    }

    fun setModelSpriteFusionMode(mode: ModelSpriteFusionMode) {
        _uiState.update { it.copy(selectedModelSpriteFusionMode = mode) }
    }

    fun setModelSpriteBlendRatio(ratio: Float) {
        _uiState.update { it.copy(modelSpriteBlendRatio = ratio.coerceIn(0.10f, 0.90f)) }
    }

    fun setModelSpriteReliefDepth(depth: Float) {
        _uiState.update { it.copy(modelSpriteReliefDepth = depth.coerceIn(0.15f, 1.25f)) }
    }

    fun setModelSpriteHarmonizePalette(harmonize: Boolean) {
        _uiState.update { it.copy(modelSpriteHarmonizePalette = harmonize) }
    }

    fun setCustomModelSpriteFusionName(name: String) {
        _uiState.update { it.copy(customModelSpriteFusionName = name) }
    }

    /**
     * Fuses a 3D Model ([Mesh3D]) and a 2D Spritesheet ([SpriteSheetData]) into a hybrid 3D+2D asset!
     */
    fun fuseModelAndSpriteSheetToMakeAsset(
        meshOverride: Mesh3D? = null,
        spriteOverride: SpriteSheetData? = null,
        modeOverride: ModelSpriteFusionMode? = null,
        applyToStudioImmediately: Boolean = true
    ) {
        val snapshot = _uiState.value
        val sourceMesh = meshOverride ?: snapshot.currentMesh
        val sourceSprite = spriteOverride ?: snapshot.currentSpriteSheet
        val mode = modeOverride ?: snapshot.selectedModelSpriteFusionMode

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isGenerating = true, selectedModelSpriteFusionMode = mode) }
            val result = ModelSpriteFusionEngine.fuseModelAndSpriteSheet(
                mesh = sourceMesh,
                spriteSheet = sourceSprite,
                mode = mode,
                blendRatio = snapshot.modelSpriteBlendRatio,
                reliefDepth = snapshot.modelSpriteReliefDepth,
                harmonizePalette = snapshot.modelSpriteHarmonizePalette,
                customName = snapshot.customModelSpriteFusionName,
                characterTag = snapshot.activeCharacterTagForImports.ifBlank { "Kaelen" }
            )

            val libEntry = SavedLibraryAsset(
                id = "lib_ms_fusion_${System.currentTimeMillis()}",
                title = result.fusedName,
                characterTag = result.characterTag,
                type = LibraryAssetType.UNIFIED_PAIR,
                mesh = result.fusedMesh,
                spriteSheet = result.fusedSpriteSheet,
                textureBitmap = result.fusedTextureAtlas,
                hardwareBadge = snapshot.selectedHardware.shortName,
                metricsSummary = "${result.fusedMesh.vertexCount}v/${result.fusedMesh.faceCount}f + ${result.fusedSpriteSheet.frames.size}f • ${result.fusionMode.badge}",
                sourceBadge = "3D+2D FUSION",
                formatSummary = "Harmony ${result.harmonyScore}% • Blend ${(result.blendRatio * 100).toInt()}%"
            )

            _uiState.update { state ->
                val updatedHistory = listOf(result) + state.savedModelSpriteFusions
                    .filterNot { it.fusedName.equals(result.fusedName, ignoreCase = true) }
                    .take(9)

                if (applyToStudioImmediately) {
                    state.copy(
                        isGenerating = false,
                        activeModelSpriteFusion = result,
                        savedModelSpriteFusions = updatedHistory,
                        currentMesh = result.fusedMesh,
                        currentSpriteSheet = result.fusedSpriteSheet,
                        activeTexture = result.fusedTextureAtlas,
                        activeTextureName = "${result.fusedName} UV Atlas",
                        extractedPalette = result.unifiedPalette,
                        savedLibraryAssets = listOf(libEntry) + state.savedLibraryAssets,
                        statusMessage = "Fused 3D Model & Spritesheet into '${result.fusedName}' (${result.fusedMesh.vertexCount}v + ${result.fusedSpriteSheet.frames.size} frames)!"
                    )
                } else {
                    state.copy(
                        isGenerating = false,
                        activeModelSpriteFusion = result,
                        savedModelSpriteFusions = updatedHistory,
                        savedLibraryAssets = listOf(libEntry) + state.savedLibraryAssets,
                        statusMessage = "Created 3D+2D Fused Asset '${result.fusedName}'!"
                    )
                }
            }

            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "ms_fused_mesh_${System.currentTimeMillis()}",
                    assetTitle = "${result.fusedName} [3D]",
                    characterName = result.characterTag,
                    modality = CharacterAssetModality.MESH_3D,
                    isUserImported = true,
                    mesh = result.fusedMesh
                )
            )
            registerAssetAndAutoFuseIfMatched(
                StagedCharacterAsset(
                    id = "ms_fused_sprite_${System.currentTimeMillis()}",
                    assetTitle = "${result.fusedName} [Sprite]",
                    characterName = result.characterTag,
                    modality = CharacterAssetModality.SPRITESHEET_2D,
                    isUserImported = true,
                    spriteSheet = result.fusedSpriteSheet
                )
            )
            recordAssetInRoomHistory(
                title = result.fusedName,
                characterTag = result.characterTag,
                assetType = "FUSED_3D_2D",
                sourceOrigin = "3D+2D FUSION (${result.fusionMode.badge})",
                hardwareTarget = snapshot.selectedHardware.shortName,
                metricsSummary = "${result.fusedMesh.vertexCount}v/${result.fusedMesh.faceCount}f + ${result.fusedSpriteSheet.frames.size}f • Harmony ${result.harmonyScore}%",
                mesh = result.fusedMesh,
                spriteSheet = result.fusedSpriteSheet,
                paletteHexList = result.unifiedPalette.map { it.hex }
            )
        }
    }

    /**
     * Generates all 5 3D Model + 2D Spritesheet fusion modes at once so the user can compare and apply any of them.
     */
    fun batchFuseAllModelSpriteModes(
        meshOverride: Mesh3D? = null,
        spriteOverride: SpriteSheetData? = null
    ) {
        val snapshot = _uiState.value
        val sourceMesh = meshOverride ?: snapshot.currentMesh
        val sourceSprite = spriteOverride ?: snapshot.currentSpriteSheet

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isGenerating = true) }
            val allResults = ModelSpriteFusionEngine.fuseAllModesForComparison(
                mesh = sourceMesh,
                spriteSheet = sourceSprite,
                blendRatio = snapshot.modelSpriteBlendRatio,
                reliefDepth = snapshot.modelSpriteReliefDepth,
                harmonizePalette = snapshot.modelSpriteHarmonizePalette,
                characterTag = snapshot.activeCharacterTagForImports.ifBlank { "Kaelen" }
            )
            val primary = allResults.firstOrNull { it.fusionMode == snapshot.selectedModelSpriteFusionMode }
                ?: allResults.first()

            _uiState.update { state ->
                val mergedHistory = (allResults + state.savedModelSpriteFusions)
                    .distinctBy { it.fusedName.lowercase() }
                    .take(12)
                state.copy(
                    isGenerating = false,
                    activeModelSpriteFusion = primary,
                    savedModelSpriteFusions = mergedHistory,
                    currentMesh = primary.fusedMesh,
                    currentSpriteSheet = primary.fusedSpriteSheet,
                    activeTexture = primary.fusedTextureAtlas,
                    activeTextureName = "${primary.fusedName} UV Atlas",
                    extractedPalette = primary.unifiedPalette,
                    statusMessage = "Synthesized all ${allResults.size} 3D Model + Spritesheet Fusion variants!"
                )
            }
        }
    }

    /**
     * Applies any previously fused [ModelSpriteFusionResult] into the active 3D, 2D, and Texture studios.
     */
    fun applyModelSpriteFusionResult(result: ModelSpriteFusionResult) {
        _uiState.update { state ->
            state.copy(
                activeModelSpriteFusion = result,
                selectedModelSpriteFusionMode = result.fusionMode,
                currentMesh = result.fusedMesh,
                currentSpriteSheet = result.fusedSpriteSheet,
                activeTexture = result.fusedTextureAtlas,
                activeTextureName = "${result.fusedName} UV Atlas",
                extractedPalette = result.unifiedPalette,
                statusMessage = "Loaded 3D+2D Fused Asset '${result.fusedName}' into all studios!"
            )
        }
    }

    /**
     * Saves a [ModelSpriteFusionResult] directly to the Personal Asset Library.
     */
    fun saveModelSpriteFusionToLibrary(result: ModelSpriteFusionResult) {
        val snapshot = _uiState.value
        val entry = SavedLibraryAsset(
            id = "lib_ms_save_${System.currentTimeMillis()}",
            title = result.fusedName,
            characterTag = result.characterTag,
            type = LibraryAssetType.UNIFIED_PAIR,
            mesh = result.fusedMesh,
            spriteSheet = result.fusedSpriteSheet,
            textureBitmap = result.fusedTextureAtlas,
            hardwareBadge = snapshot.selectedHardware.shortName,
            metricsSummary = "${result.fusedMesh.vertexCount}v/${result.fusedMesh.faceCount}f + ${result.fusedSpriteSheet.frames.size}f • ${result.fusionMode.badge}",
            sourceBadge = "3D+2D FUSION",
            formatSummary = "Harmony ${result.harmonyScore}% • Blend ${(result.blendRatio * 100).toInt()}%"
        )
        _uiState.update { state ->
            state.copy(
                savedLibraryAssets = listOf(entry) + state.savedLibraryAssets,
                statusMessage = "Saved 3D+2D Fused Asset '${result.fusedName}' to Library!"
            )
        }
    }

    fun prepareObjExport() {
        val mesh = _uiState.value.currentMesh
        val objText = ObjParser.exportToObj(mesh)
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export Wavefront 3D (.OBJ)",
                exportDialogText = objText
            )
        }
    }

    // -------------------------------------------------------------------------
    // Custom Asset Galleries CRUD, Curation & Manifest Export
    // -------------------------------------------------------------------------

    fun selectAssetGallery(galleryId: Long) {
        _uiState.update { state ->
            state.copy(selectedGalleryId = galleryId)
        }
    }

    fun createNewAssetGallery(
        name: String,
        description: String,
        themeTag: String = "SHOWCASE",
        accentColorHex: String = "#00E5FF",
        layoutStyle: String = "GRID_2COL",
        seedWithCurrentStudioAsset: Boolean = false
    ) {
        val cleanName = name.trim().ifBlank { "Untitled Asset Gallery" }
        val cleanDesc = description.trim().ifBlank {
            "Curated $themeTag collection of 3D models, 2D spritesheets, and fused assets."
        }
        val now = System.currentTimeMillis()
        val snapshot = _uiState.value

        val newGallery = AssetGalleryEntity(
            id = 0L,
            name = cleanName,
            description = cleanDesc,
            themeTag = themeTag.uppercase(),
            accentColorHex = accentColorHex,
            layoutStyle = layoutStyle,
            createdAtMillis = now
        )

        val optionalSeedItem = if (seedWithCurrentStudioAsset) {
            AssetGalleryItemEntity.fromStudioAssets(
                galleryId = 0L,
                title = "${snapshot.currentMesh.name} + ${snapshot.currentSpriteSheet.name}",
                characterTag = snapshot.activeCharacterTagForImports.ifBlank { "Studio" },
                assetType = "UNIFIED_PAIR",
                sourceOrigin = "STUDIO CURATION",
                hardwareTarget = snapshot.selectedHardware.shortName,
                metricsSummary = "${snapshot.currentMesh.vertexCount}v/${snapshot.currentMesh.faceCount}f • ${snapshot.currentSpriteSheet.frames.size}f",
                curatorNote = "Added on gallery creation from active Studio workspace.",
                mesh = snapshot.currentMesh,
                spriteSheet = snapshot.currentSpriteSheet,
                addedAtMillis = now
            )
        } else null

        val repo = galleryRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val insertedId = repo.createGallery(newGallery)
                    if (optionalSeedItem != null) {
                        repo.addItemToGallery(optionalSeedItem.copy(galleryId = insertedId))
                    }
                    val refreshed = repo.getAllGalleriesWithItemsSnapshot()
                    _uiState.update { state ->
                        state.copy(
                            assetGalleries = refreshed,
                            selectedGalleryId = insertedId,
                            statusMessage = "Created new Asset Gallery '$cleanName'!"
                        )
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
        } else {
            _uiState.update { state ->
                val nextGalleryId = (state.assetGalleries.maxOfOrNull { it.gallery.id } ?: 0L) + 1L
                val createdGallery = newGallery.copy(id = nextGalleryId)
                val nextItemId = (state.assetGalleries.flatMap { it.items }.maxOfOrNull { it.id } ?: 0L) + 1L
                val seedList = if (optionalSeedItem != null) {
                    listOf(optionalSeedItem.copy(id = nextItemId, galleryId = nextGalleryId))
                } else {
                    emptyList()
                }
                val createdEntry = AssetGalleryWithItems(gallery = createdGallery, items = seedList)
                state.copy(
                    assetGalleries = listOf(createdEntry) + state.assetGalleries,
                    selectedGalleryId = nextGalleryId,
                    statusMessage = "Created new Asset Gallery '$cleanName'!"
                )
            }
        }
    }

    fun updateAssetGallery(
        galleryId: Long,
        name: String? = null,
        description: String? = null,
        themeTag: String? = null,
        accentColorHex: String? = null,
        layoutStyle: String? = null
    ) {
        val existing = _uiState.value.assetGalleries.firstOrNull { it.gallery.id == galleryId }?.gallery ?: return
        val updated = existing.copy(
            name = name?.trim()?.ifBlank { existing.name } ?: existing.name,
            description = description?.trim()?.ifBlank { existing.description } ?: existing.description,
            themeTag = themeTag?.uppercase() ?: existing.themeTag,
            accentColorHex = accentColorHex ?: existing.accentColorHex,
            layoutStyle = layoutStyle ?: existing.layoutStyle
        )

        val repo = galleryRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.updateGallery(updated)
                    val refreshed = repo.getAllGalleriesWithItemsSnapshot()
                    _uiState.update { state ->
                        state.copy(
                            assetGalleries = refreshed,
                            statusMessage = "Updated gallery '${updated.name}'"
                        )
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    assetGalleries = state.assetGalleries.map { entry ->
                        if (entry.gallery.id == galleryId) entry.copy(gallery = updated) else entry
                    },
                    statusMessage = "Updated gallery '${updated.name}'"
                )
            }
        }
    }

    fun deleteAssetGallery(galleryId: Long) {
        val targetName = _uiState.value.assetGalleries.firstOrNull { it.gallery.id == galleryId }?.gallery?.name ?: "Gallery"
        val repo = galleryRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.deleteGallery(galleryId)
                    val refreshed = repo.getAllGalleriesWithItemsSnapshot()
                    _uiState.update { state ->
                        val nextSelected = if (state.selectedGalleryId == galleryId) {
                            refreshed.firstOrNull()?.gallery?.id
                        } else {
                            state.selectedGalleryId
                        }
                        state.copy(
                            assetGalleries = refreshed,
                            selectedGalleryId = nextSelected,
                            statusMessage = "Deleted Asset Gallery '$targetName'"
                        )
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
        } else {
            _uiState.update { state ->
                val remaining = state.assetGalleries.filterNot { it.gallery.id == galleryId }
                val nextSelected = if (state.selectedGalleryId == galleryId) {
                    remaining.firstOrNull()?.gallery?.id
                } else {
                    state.selectedGalleryId
                }
                state.copy(
                    assetGalleries = remaining,
                    selectedGalleryId = nextSelected,
                    statusMessage = "Deleted Asset Gallery '$targetName'"
                )
            }
        }
    }

    fun addActiveStudioAssetToGallery(
        galleryId: Long? = _uiState.value.selectedGalleryId,
        assetScope: String = "UNIFIED_PAIR",
        curatorNote: String = ""
    ) {
        val snapshot = _uiState.value
        val resolvedGalleryId = galleryId ?: snapshot.assetGalleries.firstOrNull()?.gallery?.id ?: return
        val galleryName = snapshot.assetGalleries.firstOrNull { it.gallery.id == resolvedGalleryId }?.gallery?.name ?: "Gallery"

        val meshToSave = if (assetScope == "SPRITE_2D") null else snapshot.currentMesh
        val spriteToSave = if (assetScope == "MODEL_3D") null else snapshot.currentSpriteSheet

        val title = when (assetScope) {
            "MODEL_3D" -> snapshot.currentMesh.name
            "SPRITE_2D" -> snapshot.currentSpriteSheet.name
            "FUSED_3D_2D" -> snapshot.activeModelSpriteFusion?.fusedName ?: "${snapshot.currentMesh.name} × ${snapshot.currentSpriteSheet.name}"
            else -> "${snapshot.currentMesh.name} + ${snapshot.currentSpriteSheet.name}"
        }
        val metrics = when (assetScope) {
            "MODEL_3D" -> "${snapshot.currentMesh.vertexCount}v/${snapshot.currentMesh.faceCount}f"
            "SPRITE_2D" -> "${snapshot.currentSpriteSheet.frames.size} frames (${snapshot.currentSpriteSheet.columns}×${snapshot.currentSpriteSheet.rows})"
            else -> "${snapshot.currentMesh.vertexCount}v/${snapshot.currentMesh.faceCount}f + ${snapshot.currentSpriteSheet.frames.size}f"
        }

        val item = AssetGalleryItemEntity.fromStudioAssets(
            galleryId = resolvedGalleryId,
            title = title,
            characterTag = snapshot.activeCharacterTagForImports.ifBlank { "Studio" },
            assetType = assetScope,
            sourceOrigin = if (assetScope == "FUSED_3D_2D") "3D+2D FUSION" else "STUDIO CURATION",
            hardwareTarget = snapshot.selectedHardware.shortName,
            metricsSummary = metrics,
            curatorNote = curatorNote.ifBlank { "Curated for '$galleryName' (${snapshot.selectedHardware.shortName})" },
            mesh = meshToSave,
            spriteSheet = spriteToSave,
            gameTag = snapshot.activeGameTagForImports.ifBlank { "General Studio" },
            groupTag = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        )
        insertItemIntoGalleryInternal(item, galleryName)
    }

    fun addLibraryAssetToGallery(
        galleryId: Long,
        libraryItem: SavedLibraryAsset,
        curatorNote: String = ""
    ) {
        val galleryName = _uiState.value.assetGalleries.firstOrNull { it.gallery.id == galleryId }?.gallery?.name ?: "Gallery"
        val item = AssetGalleryItemEntity.fromStudioAssets(
            galleryId = galleryId,
            title = libraryItem.title,
            characterTag = libraryItem.characterTag,
            assetType = libraryItem.type.name,
            sourceOrigin = libraryItem.sourceBadge,
            hardwareTarget = libraryItem.hardwareBadge,
            metricsSummary = libraryItem.metricsSummary,
            curatorNote = curatorNote.ifBlank { "Added from Saved Asset Library" },
            mesh = libraryItem.mesh,
            spriteSheet = libraryItem.spriteSheet,
            gameTag = libraryItem.gameTag.ifBlank { "General Studio" },
            groupTag = libraryItem.groupTag.ifBlank { "Core Roster" }
        )
        insertItemIntoGalleryInternal(item, galleryName)
    }

    fun addHistoryEntityToGallery(
        galleryId: Long,
        historyEntity: AssetHistoryEntity,
        curatorNote: String = ""
    ) {
        val galleryName = _uiState.value.assetGalleries.firstOrNull { it.gallery.id == galleryId }?.gallery?.name ?: "Gallery"
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedObjData = historyEntity.objMeshData?.takeIf { it.isNotBlank() }
                ?: if (historyEntity.meshFilePath.isNotBlank()) {
                    MeshDiskStorage.readMeshObj(historyEntity.meshFilePath, appContext)
                } else {
                    null
                }
            val item = AssetGalleryItemEntity(
                galleryId = galleryId,
                title = historyEntity.title,
                characterTag = historyEntity.characterTag,
                assetType = historyEntity.assetType,
                sourceOrigin = historyEntity.sourceOrigin,
                hardwareTarget = historyEntity.hardwareTarget,
                metricsSummary = historyEntity.metricsSummary,
                curatorNote = curatorNote.ifBlank { "Imported from Room Asset History Vault" },
                objMeshData = resolvedObjData,
                meshFilePath = "",
                spritePngBytes = historyEntity.spritePngBytes,
                spriteColumns = historyEntity.spriteColumns,
                spriteRows = historyEntity.spriteRows,
                gameTag = historyEntity.gameTag.ifBlank { "General Studio" },
                groupTag = historyEntity.groupTag.ifBlank { "Core Roster" }
            )
            insertItemIntoGalleryInternal(item, galleryName)
        }
    }

    private fun insertItemIntoGalleryInternal(item: AssetGalleryItemEntity, galleryName: String) {
        val repo = galleryRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.addItemToGallery(item)
                    val refreshed = repo.getAllGalleriesWithItemsSnapshot()
                    _uiState.update { state ->
                        state.copy(
                            assetGalleries = refreshed,
                            selectedGalleryId = item.galleryId,
                            statusMessage = "Added '${item.title}' to gallery '$galleryName'!"
                        )
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
        } else {
            _uiState.update { state ->
                val nextItemId = (state.assetGalleries.flatMap { it.items }.maxOfOrNull { it.id } ?: 0L) + 1L
                val inserted = item.copy(id = nextItemId)
                state.copy(
                    assetGalleries = state.assetGalleries.map { entry ->
                        if (entry.gallery.id == item.galleryId) {
                            entry.copy(items = listOf(inserted) + entry.items)
                        } else {
                            entry
                        }
                    },
                    selectedGalleryId = item.galleryId,
                    statusMessage = "Added '${item.title}' to gallery '$galleryName'!"
                )
            }
        }
    }

    fun loadGalleryItemIntoStudio(item: AssetGalleryItemEntity) {
        viewModelScope.launch(Dispatchers.Default) {
            val restoredMesh = item.toMesh3D(appContext)
            val restoredSprite = item.toSpriteSheetData()
            _uiState.update { state ->
                state.copy(
                    currentMesh = restoredMesh ?: state.currentMesh,
                    currentSpriteSheet = restoredSprite ?: state.currentSpriteSheet,
                    activeGameTagForImports = item.gameTag.ifBlank { state.activeGameTagForImports },
                    activeGroupTagForImports = item.groupTag.ifBlank { state.activeGroupTagForImports },
                    activeCharacterTagForImports = item.characterTag.ifBlank { state.activeCharacterTagForImports },
                    statusMessage = "Loaded '${item.title}' from gallery into Studio!"
                )
            }
        }
    }

    fun removeItemFromGallery(itemId: Long) {
        val repo = galleryRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.removeItemFromGallery(itemId)
                    val refreshed = repo.getAllGalleriesWithItemsSnapshot()
                    _uiState.update { state ->
                        state.copy(
                            assetGalleries = refreshed,
                            statusMessage = "Removed asset from gallery"
                        )
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    assetGalleries = state.assetGalleries.map { entry ->
                        entry.copy(items = entry.items.filterNot { it.id == itemId })
                    },
                    statusMessage = "Removed asset from gallery"
                )
            }
        }
    }

    fun prepareGalleryManifestExport(galleryId: Long) {
        val entry = _uiState.value.assetGalleries.firstOrNull { it.gallery.id == galleryId } ?: return
        val g = entry.gallery
        val manifest = buildString {
            appendLine("=== PIXELFORGE ASSET GALLERY MANIFEST ===")
            appendLine("Gallery Name : ${g.name}")
            appendLine("Theme Tag    : ${g.themeTag}")
            appendLine("Layout Style : ${g.layoutStyle}")
            appendLine("Accent Color : ${g.accentColorHex}")
            appendLine("Description  : ${g.description}")
            appendLine("Curated Items: ${entry.items.size}")
            appendLine("-----------------------------------------")
            entry.items.forEachIndexed { idx, item ->
                appendLine("[${idx + 1}] ${item.title} (${item.assetType})")
                appendLine("    Character : ${item.characterTag} | Target: ${item.hardwareTarget} | Origin: ${item.sourceOrigin}")
                appendLine("    Metrics   : ${item.metricsSummary}")
                if (item.curatorNote.isNotBlank()) {
                    appendLine("    Note      : ${item.curatorNote}")
                }
            }
        }
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export Asset Gallery (${g.name})",
                exportDialogText = manifest
            )
        }
    }

    fun prepareAssetIndexJsonExport(galleryId: Long): String? {
        val entry = _uiState.value.assetGalleries.firstOrNull { it.gallery.id == galleryId } ?: return null
        val json = buildMpvAssetIndexJson(entry)
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export asset-index.json (${entry.gallery.name})",
                exportDialogText = json
            )
        }
        return json
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(exportDialogText = null) }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    // -------------------------------------------------------------------------
    // Multi-AI & Multi-Source Hub State Management
    // -------------------------------------------------------------------------

    fun refreshOmniAiHubState() {
        _uiState.update { state ->
            state.copy(
                aiProviderProfiles = OmniAiRouter.getAllProviderProfiles(),
                omniAiRouting = OmniAiRouter.getRoutingConfig(),
                knowledgeSources = OmniAiRouter.getContextRegistry().getSources()
            )
        }
    }

    fun setActiveAiProviderForRole(role: AiTaskRole, provider: AiProviderType) {
        if (role == AiTaskRole.IMAGE_GENERATION && !provider.supportsImageGeneration) {
            postStatusMessage("${provider.defaultDisplayName} does not support direct image generation.")
            return
        }
        val updatedRouting = OmniAiRouter.setActiveProviderForRole(role, provider)
        _uiState.update {
            it.copy(
                omniAiRouting = updatedRouting,
                statusMessage = "${role.label} AI set to ${provider.defaultDisplayName}"
            )
        }
    }

    fun saveAiProviderCredential(provider: AiProviderType, secret: String) {
        OmniAiRouter.saveProviderCredential(provider, secret)
        refreshOmniAiHubState()
        postStatusMessage("Saved encrypted key for ${provider.defaultDisplayName}")
    }

    fun clearAiProviderCredential(provider: AiProviderType) {
        OmniAiRouter.clearProviderCredential(provider)
        refreshOmniAiHubState()
        postStatusMessage("Cleared saved key for ${provider.defaultDisplayName}")
    }

    fun updateAiProviderSettings(
        provider: AiProviderType,
        textModelId: String? = null,
        imageModelId: String? = null,
        baseUrl: String? = null,
        markLocalConnected: Boolean? = null
    ) {
        OmniAiRouter.updateProviderSettings(
            provider = provider,
            textModelId = textModelId,
            imageModelId = imageModelId,
            baseUrl = baseUrl,
            markLocalConnected = markLocalConnected
        )
        refreshOmniAiHubState()
        postStatusMessage("Updated ${provider.defaultDisplayName} settings")
    }

    fun startOpenRouterPkceSignIn(onLaunchAuthUrl: (String) -> Unit) {
        if (_uiState.value.isSigningInOpenRouterPkce) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSigningInOpenRouterPkce = true,
                    statusMessage = "Opening OpenRouter PKCE sign-in in browser..."
                )
            }
            val result = OpenRouterPkceHelper.signInWithLoopbackPkce(
                vault = OmniAiRouter.getVault(),
                onLaunchAuthUrl = { session -> onLaunchAuthUrl(session.authUrl) }
            )
            when (result) {
                is OpenRouterPkceResult.Success -> {
                    OmniAiRouter.setActiveProviderForRole(AiTaskRole.TEXT_AND_SPECS, AiProviderType.OPENROUTER)
                    _uiState.update {
                        it.copy(
                            isSigningInOpenRouterPkce = false,
                            aiProviderProfiles = OmniAiRouter.getAllProviderProfiles(),
                            omniAiRouting = OmniAiRouter.getRoutingConfig(),
                            statusMessage = "Connected OpenRouter via PKCE (${result.accountHint})!"
                        )
                    }
                }
                is OpenRouterPkceResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSigningInOpenRouterPkce = false,
                            statusMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    fun toggleKnowledgeSource(id: String, enabled: Boolean) {
        val updated = OmniAiRouter.getContextRegistry().toggleSource(id, enabled)
        _uiState.update { it.copy(knowledgeSources = updated) }
    }

    fun removeKnowledgeSource(id: String) {
        val updated = OmniAiRouter.getContextRegistry().removeSource(id)
        _uiState.update {
            it.copy(
                knowledgeSources = updated,
                statusMessage = "Removed knowledge source from AI Hub"
            )
        }
    }

    fun addPastedKnowledgeSource(title: String, content: String) {
        if (content.isBlank()) return
        val updated = OmniAiRouter.getContextRegistry().addPastedNoteSource(
            title = title,
            content = content
        )
        _uiState.update {
            it.copy(
                knowledgeSources = updated,
                statusMessage = "Attached shared knowledge source '${title.ifBlank { "Notebook Lore Notes" }}'"
            )
        }
    }

    fun importLocalKnowledgeDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            val res = OmniAiRouter.getContextRegistry().importLocalDocumentUri(context, uri)
            res.fold(
                onSuccess = { entry ->
                    _uiState.update {
                        it.copy(
                            knowledgeSources = OmniAiRouter.getContextRegistry().getSources(),
                            statusMessage = "Imported '${entry.title}' (${entry.content.length} chars) into Shared AI Sources!"
                        )
                    }
                },
                onFailure = { err ->
                    postStatusMessage("Failed to import document: ${err.message}")
                }
            )
        }
    }

    fun addHttpsKnowledgeSource(url: String, customTitle: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingKnowledgeUrl = true) }
            val res = OmniAiRouter.getContextRegistry().fetchAndAddPublicHttpsSource(
                rawUrl = url,
                customTitle = customTitle
            )
            res.fold(
                onSuccess = { entry ->
                    _uiState.update {
                        it.copy(
                            isFetchingKnowledgeUrl = false,
                            knowledgeSources = OmniAiRouter.getContextRegistry().getSources(),
                            statusMessage = "Fetched & attached '${entry.title}' (${entry.content.length} chars)!"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isFetchingKnowledgeUrl = false,
                            statusMessage = "Source URL rejected or failed: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun buildActiveProjectContextSummary(state: AppUiState = _uiState.value): String {
        val paletteHex = state.extractedPalette.take(6).joinToString(", ") { it.hex }.ifBlank { "Default Studio" }
        val galleryNames = state.assetGalleries.take(4).joinToString(", ") { it.gallery.name }
        val charGroups = state.characterFusionGroups.take(4).joinToString(", ") { it.characterName }
        return buildString {
            appendLine("Active 3D Mesh: ${state.currentMesh.name} (${state.currentMesh.vertexCount}v/${state.currentMesh.faceCount}f, format=${state.currentMesh.sourceFormat})")
            appendLine("Active 2D Spritesheet: ${state.currentSpriteSheet.name} (${state.currentSpriteSheet.columns}x${state.currentSpriteSheet.rows}, ${state.currentSpriteSheet.frames.size} frames)")
            appendLine("Hierarchy Path: ${state.activeGameTagForImports} -> ${state.activeGroupTagForImports} -> ${state.activeCharacterTagForImports.ifBlank { "Studio" }}")
            appendLine("Detected Character Groups: $charGroups")
            appendLine("Hardware Target: ${state.selectedHardware.name} (${state.selectedHardware.shortName})")
            appendLine("Active Palette: $paletteHex")
            if (galleryNames.isNotBlank()) {
                appendLine("Active Asset Galleries: $galleryNames")
            }
        }.trim()
    }

    fun assembleSharedOmniContextBlock(
        queryHint: String = _uiState.value.promptText,
        maxChars: Int = MultiSourceContextRegistry.DEFAULT_MAX_CONTEXT_CHARS
    ): String {
        return OmniAiRouter.getContextRegistry().assembleContextBlock(
            queryHint = queryHint,
            activeProjectSummary = buildActiveProjectContextSummary(),
            maxChars = maxChars
        )
    }

    // -------------------------------------------------------------------------
    // Game -> Group -> Character Tag Reassignment (Room v4 + Library)
    // -------------------------------------------------------------------------

    fun reassignGalleryItemTags(
        itemId: Long,
        gameTag: String,
        groupTag: String,
        characterTag: String
    ) {
        val cleanGame = gameTag.trim().ifBlank { "General Studio" }
        val cleanGroup = groupTag.trim().ifBlank { "Core Roster" }
        val cleanChar = characterTag.trim().ifBlank { "Studio Asset" }
        val repo = galleryRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.updateGalleryItemTags(itemId, cleanGame, cleanGroup, cleanChar)
                    val refreshed = repo.getAllGalleriesWithItemsSnapshot()
                    _uiState.update { state ->
                        state.copy(
                            assetGalleries = refreshed,
                            statusMessage = "Updated gallery item tags -> $cleanGame / $cleanGroup / $cleanChar"
                        )
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
        }
        _uiState.update { state ->
            state.copy(
                assetGalleries = state.assetGalleries.map { entry ->
                    entry.copy(
                        items = entry.items.map { item ->
                            if (item.id == itemId) {
                                item.copy(gameTag = cleanGame, groupTag = cleanGroup, characterTag = cleanChar)
                            } else {
                                item
                            }
                        }
                    )
                },
                statusMessage = "Updated gallery item tags -> $cleanGame / $cleanGroup / $cleanChar"
            )
        }
    }

    fun reassignHistoryItemTags(
        id: Long,
        gameTag: String,
        groupTag: String,
        characterTag: String
    ) {
        val cleanGame = gameTag.trim().ifBlank { "General Studio" }
        val cleanGroup = groupTag.trim().ifBlank { "Core Roster" }
        val cleanChar = characterTag.trim().ifBlank { "Studio Asset" }
        val repo = historyRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.updateHistoryTags(id, cleanGame, cleanGroup, cleanChar)
                    val refreshed = repo.getAllHistorySnapshot()
                    _uiState.update { state ->
                        state.copy(
                            roomAssetHistory = refreshed,
                            statusMessage = "Updated history tags -> $cleanGame / $cleanGroup / $cleanChar"
                        )
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
        }
        _uiState.update { state ->
            state.copy(
                roomAssetHistory = state.roomAssetHistory.map { entity ->
                    if (entity.id == id) {
                        entity.copy(gameTag = cleanGame, groupTag = cleanGroup, characterTag = cleanChar)
                    } else {
                        entity
                    }
                },
                statusMessage = "Updated history tags -> $cleanGame / $cleanGroup / $cleanChar"
            )
        }
    }

    fun reassignLibraryAssetTags(
        id: String,
        gameTag: String,
        groupTag: String,
        characterTag: String
    ) {
        val cleanGame = gameTag.trim().ifBlank { "General Studio" }
        val cleanGroup = groupTag.trim().ifBlank { "Core Roster" }
        val cleanChar = characterTag.trim().ifBlank { "Studio Asset" }
        _uiState.update { state ->
            state.copy(
                savedLibraryAssets = state.savedLibraryAssets.map { asset ->
                    if (asset.id == id) {
                        asset.copy(gameTag = cleanGame, groupTag = cleanGroup, characterTag = cleanChar)
                    } else {
                        asset
                    }
                },
                statusMessage = "Updated library tags -> $cleanGame / $cleanGroup / $cleanChar"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Style-Locked True Fusion Prompt Studio Actions
    // -------------------------------------------------------------------------

    fun setPromptHardwareTier(tier: PromptHardwareTier) {
        _uiState.update { it.copy(promptHardwareTier = tier) }
    }

    fun setPromptAssetPreset(preset: PromptAssetPreset) {
        _uiState.update { it.copy(promptAssetPreset = preset) }
    }

    fun setIncludeHumanoidHeroicProportions(enabled: Boolean) {
        _uiState.update { it.copy(includeHumanoidHeroicProportions = enabled) }
    }

    fun buildCurrentPromptStudioOutput(state: AppUiState = _uiState.value): PromptStudioOutput {
        val baseSubject = state.promptText.ifBlank { state.currentSpriteSheet.name }
        val effectiveSubject = if (state.userAiTrainingProfile.applyToFormBuilder) {
            state.userAiTrainingProfile.applyTriggerToPrompt(baseSubject)
        } else {
            baseSubject
        }
        return PromptStudioEngine.buildPrompts(
            subjectDescription = effectiveSubject,
            tier = state.promptHardwareTier,
            preset = state.promptAssetPreset,
            includeHumanoidHeroicProportions = state.includeHumanoidHeroicProportions,
            paletteHexList = state.extractedPalette.map { it.hex },
            gameTag = state.activeGameTagForImports,
            groupTag = state.activeGroupTagForImports,
            characterTag = state.activeCharacterTagForImports,
            targetFps = state.targetAssetFps
        )
    }

    fun runPromptStudioViaActiveOmniAi() {
        val output = buildCurrentPromptStudioOutput()
        _uiState.update {
            it.copy(
                selectedTheme = ArtTheme.ARCADE_COMIC_CEL,
                geminiCustomInstruction = output.fullSpec,
                statusMessage = "Running Style-Locked Prompt (${output.tier.displayName} • ${output.preset.displayName}) via active OmniAiHub provider..."
            )
        }
        runGeminiEnhancementOrVariantGeneration()
    }

    // -------------------------------------------------------------------------
    // Canonical Asset & Official Spec Auditor Actions
    // -------------------------------------------------------------------------

    fun setAuditIntendedForPublicRelease(enabled: Boolean) {
        _uiState.update { it.copy(auditIntendedForPublicRelease = enabled) }
    }

    fun runCanonicalAssetAudit(useOmniAiVision: Boolean = true) {
        val snapshot = _uiState.value
        val generatedBmp = snapshot.lastGeminiReport?.aiSynthesizedBitmap
            ?: snapshot.currentSpriteSheet.sourceBitmap
        val referenceBmp = snapshot.conceptArtBitmap
        val activeGallery = snapshot.assetGalleries.firstOrNull { it.gallery.id == snapshot.selectedGalleryId }
            ?: snapshot.assetGalleries.firstOrNull()
        val indexJson = activeGallery?.let { buildMpvAssetIndexJson(it) }

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isAuditingAsset = true) }
            val contractReport = CanonicalAssetAndCodeAuditor.auditHardwareAndContractCompliance(
                mesh = snapshot.currentMesh,
                spriteSheet = snapshot.currentSpriteSheet,
                hardwareSpec = snapshot.selectedHardware,
                assetIndexJson = indexJson
            )
            val auditReport = if (useOmniAiVision) {
                CanonicalAssetAndCodeAuditor.auditAssetWithOmniAi(
                    generatedBitmap = generatedBmp,
                    referenceBitmapOrNull = referenceBmp,
                    targetHardware = snapshot.selectedHardware,
                    intendedForPublicRelease = snapshot.auditIntendedForPublicRelease,
                    sharedContextBlock = assembleSharedOmniContextBlock(queryHint = snapshot.promptText),
                    assetTitle = snapshot.currentSpriteSheet.name
                )
            } else {
                CanonicalAssetAndCodeAuditor.auditAssetLocally(
                    generatedBitmap = generatedBmp,
                    referenceBitmapOrNull = referenceBmp,
                    targetHardware = snapshot.selectedHardware,
                    intendedForPublicRelease = snapshot.auditIntendedForPublicRelease
                )
            }
            _uiState.update {
                it.copy(
                    isAuditingAsset = false,
                    lastAssetAuditReport = auditReport,
                    lastContractComplianceReport = contractReport,
                    statusMessage = "Audit Complete: ${auditReport.fidelityTag.label} • Style ${auditReport.styleFidelityScore}/100 • Delta ${auditReport.visualDeltaPercent}%"
                )
            }
        }
    }

    fun approveAuditAndPurgeEphemeralSources(context: Context? = appContext) {
        val ctx = context ?: appContext
        val deleted = if (ctx != null) {
            CanonicalAssetAndCodeAuditor.approveAndCleanEphemeralReference(ctx)
        } else {
            0
        }
        _uiState.update {
            it.copy(
                statusMessage = if (deleted > 0) {
                    "Approved & purged $deleted ephemeral reference file(s) from cacheDir/ephemeral_sources."
                } else {
                    "Approved — ephemeral reference cache is already clean (0 temp files)."
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // 6-Layer Visual/Rig Master Pack Actions (20-Color Wheel, 3-Map Recolor,
    // Foot-Aligned Slicer & Hardware Visual Output Split)
    // -------------------------------------------------------------------------

    fun toggleColorWheelHex(hex: String) {
        val normalized = hex.trim().uppercase()
        if (normalized.isEmpty()) return
        _uiState.update { state ->
            val current = state.selectedColorWheelHexes
            if (current.any { it.equals(normalized, ignoreCase = true) }) {
                state.copy(
                    selectedColorWheelHexes = current.filterNot { it.equals(normalized, ignoreCase = true) }
                )
            } else if (current.size >= 4) {
                state.copy(
                    statusMessage = "Max 4 Color Wheel regions active — tap a selected swatch to swap."
                )
            } else {
                state.copy(
                    selectedColorWheelHexes = current + normalized
                )
            }
        }
    }

    fun clearColorWheelSelection() {
        _uiState.update {
            it.copy(
                selectedColorWheelHexes = emptyList(),
                statusMessage = "Color Wheel reset to 0 selected (preserving original sprite colors)."
            )
        }
    }

    fun setMasterOutputTier(tier: HardwareVisualOutputTier) {
        _uiState.update {
            it.copy(
                activeMasterOutputTier = tier,
                statusMessage = "Active Visual Output Tier: ${tier.displayName}"
            )
        }
    }

    fun applyThreeMapRecolorToActiveSprite() {
        val snapshot = _uiState.value
        val sheet = snapshot.currentSpriteSheet
        val selectedHexes = snapshot.selectedColorWheelHexes
        val tier = snapshot.activeMasterOutputTier

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val recoloredFrames = sheet.frames.map { frame ->
                    val layers = ThreeMapRecolorEngine.decomposeSpriteToThreeMaps(frame)
                    ThreeMapRecolorEngine.compositeRecoloredSprite(
                        layers = layers,
                        selectedWheelHexes = selectedHexes,
                        tier = tier
                    )
                }
                if (recoloredFrames.isEmpty()) {
                    _uiState.update { it.copy(statusMessage = "No sprite frames available to recolor.") }
                    return@launch
                }

                val cols = sheet.columns.coerceAtLeast(1)
                val rows = sheet.rows.coerceAtLeast(1)
                val fw = recoloredFrames.first().width.coerceAtLeast(1)
                val fh = recoloredFrames.first().height.coerceAtLeast(1)
                val atlasBmp = Bitmap.createBitmap(fw * cols, fh * rows, Bitmap.Config.ARGB_8888).apply {
                    setHasAlpha(true)
                    eraseColor(Color.TRANSPARENT)
                }
                val canvas = Canvas(atlasBmp)
                val paint = Paint().apply {
                    isFilterBitmap = false
                    isAntiAlias = false
                }
                recoloredFrames.forEachIndexed { idx, frameBmp ->
                    val col = idx % cols
                    val row = idx / cols
                    if (row < rows) {
                        canvas.drawBitmap(frameBmp, (col * fw).toFloat(), (row * fh).toFloat(), paint)
                    }
                }

                val suffix = if (selectedHexes.isEmpty()) tier.displayName else "${tier.displayName} • ${selectedHexes.size}C"
                val newName = "${sheet.name.substringBefore(" [")} [$suffix]"
                val updatedSheet = sheet.copy(
                    id = "recolor_${System.currentTimeMillis()}",
                    name = newName,
                    sourceBitmap = atlasBmp,
                    frameWidth = fw,
                    frameHeight = fh,
                    frames = recoloredFrames,
                    originalWidth = atlasBmp.width,
                    originalHeight = atlasBmp.height,
                    sourceKindLabel = "3-MAP RECOLOR"
                )
                _uiState.update { state ->
                    state.copy(
                        currentSpriteSheet = updatedSheet,
                        statusMessage = "Baked 3-Map Recolor (${tier.displayName}, ${selectedHexes.size} wheel overrides) across ${recoloredFrames.size} frames!"
                    )
                }
                recordAssetInRoomHistory(
                    title = newName,
                    characterTag = snapshot.activeCharacterTagForImports.ifBlank { "Studio" },
                    assetType = "SPRITE_2D",
                    sourceOrigin = "3-MAP RECOLOR",
                    hardwareTarget = snapshot.selectedHardware.shortName,
                    metricsSummary = "${recoloredFrames.size}f (${fw}×${fh}px) • ${tier.displayName}",
                    spriteSheet = updatedSheet,
                    paletteHexList = selectedHexes
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "3-Map Recolor bake failed", e)
                _uiState.update { it.copy(statusMessage = "3-Map Recolor failed: ${e.message}") }
            }
        }
    }

    fun footAlignActiveSpriteSheet() {
        val snapshot = _uiState.value
        val sheet = snapshot.currentSpriteSheet
        val cadence = snapshot.activeRigMetadata.cadenceFps

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = FootAlignedSlicerEngine.sliceAndFootAlignSheet(
                    sheet = sheet,
                    cadenceFps = cadence,
                    paddingPx = 2
                )
                _uiState.update { state ->
                    state.copy(
                        currentSpriteSheet = result.alignedSheet,
                        activeRigMetadata = result.rigMetadata,
                        lastFootAlignmentSummary = result.summary,
                        statusMessage = result.summary
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Foot-aligned slicing failed", e)
                _uiState.update { it.copy(statusMessage = "Foot-alignment failed: ${e.message}") }
            }
        }
    }

    fun updateRigCadenceFps(cadence: FrameCadenceFps) {
        _uiState.update { state ->
            state.copy(
                activeRigMetadata = state.activeRigMetadata.copy(cadenceFps = cadence),
                statusMessage = "Rig cadence set to ${cadence.label}"
            )
        }
    }

    fun updateRigAnchor(type: AnchorPointType, xNorm: Float, yNorm: Float) {
        val clamped = NormalizedAnchor.of(type, xNorm, yNorm)
        _uiState.update { state ->
            val currentRig = state.activeRigMetadata
            val updatedAnchors = currentRig.anchors.toMutableMap().apply {
                put(type, clamped)
            }
            val updatedFeetPivot = if (type == AnchorPointType.FEET_PIVOT) {
                clamped.yNorm
            } else {
                currentRig.feetPivotYNorm
            }
            state.copy(
                activeRigMetadata = currentRig.copy(
                    feetPivotYNorm = updatedFeetPivot,
                    anchors = updatedAnchors
                )
            )
        }
    }

    fun prepareRigMetaJsonExport() {
        val json = _uiState.value.activeRigMetadata.toJsonString()
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export Rig Metadata (rig_meta.json)",
                exportDialogText = json
            )
        }
    }

    fun prepareMasterPackManifestExport() {
        val snapshot = _uiState.value
        val sampleFrame = snapshot.currentSpriteSheet.frames.firstOrNull()
            ?: snapshot.currentSpriteSheet.sourceBitmap
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val layers = ThreeMapRecolorEngine.decomposeSpriteToThreeMaps(sampleFrame)
                val compiledOutputs = FootAlignedSlicerEngine.compileHardwareVisualSplit(
                    layers = layers,
                    selectedWheelHexes = snapshot.selectedColorWheelHexes,
                    targetHardware = snapshot.selectedHardware,
                    rigMeta = snapshot.activeRigMetadata
                )
                val manifestJson = FootAlignedSlicerEngine.buildMasterPackManifestJson(
                    assetName = snapshot.currentSpriteSheet.name,
                    gameTag = snapshot.activeGameTagForImports.ifBlank { "General Studio" },
                    groupTag = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" },
                    characterTag = snapshot.activeCharacterTagForImports.ifBlank { "Studio" },
                    selectedWheelHexes = snapshot.selectedColorWheelHexes,
                    rigMeta = snapshot.activeRigMetadata,
                    targetHardware = snapshot.selectedHardware,
                    outputs = compiledOutputs
                )
                _uiState.update {
                    it.copy(
                        exportDialogTitle = "Export Master Pack Manifest (master_pack_manifest.json)",
                        exportDialogText = manifestJson
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Master Pack manifest export failed", e)
                _uiState.update { it.copy(statusMessage = "Master Pack export failed: ${e.message}") }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // FIRST-CLASS AUDIO ASSET VAULT, SOUNDCHIP DSP SPLIT, 6-STAGE IMPORTER & AES-256-GCM COLD VAULT
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    fun selectAudioEntry(id: String) {
        _uiState.update { state ->
            val exists = state.audioVaultEntries.any { it.id == id }
            if (!exists) state else state.copy(selectedAudioEntryId = id)
        }
    }

    fun selectSoundchipProfile(profile: SoundchipDspProfile) {
        _uiState.update { state ->
            state.copy(
                selectedSoundchipProfile = profile,
                statusMessage = "Soundchip DSP Split set to ${profile.displayName} (${profile.bitDepth}-bit @ ${profile.targetSampleRateHz} Hz)"
            )
        }
    }

    fun auditionSelectedAudioWithDsp() {
        val snapshot = _uiState.value
        val selected = snapshot.audioVaultEntries.firstOrNull { it.id == snapshot.selectedAudioEntryId }
            ?: snapshot.audioVaultEntries.firstOrNull()
        if (selected == null) {
            postStatusMessage("No audio asset selected to audition.")
            return
        }
        val profile = snapshot.selectedSoundchipProfile
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val (rawPcm, sampleRate) = resolvePcmForAudioEntry(selected)
                if (rawPcm.isEmpty()) {
                    _uiState.update {
                        it.copy(statusMessage = "Cannot audition '${selected.title}': no PCM waveform data available.")
                    }
                    return@launch
                }
                val processedPcm = AudioSynthAndDspEngine.applyHardwareSoundchipDsp(
                    pcm = rawPcm,
                    inputRateHz = sampleRate,
                    profile = profile
                )
                AudioSynthAndDspEngine.playPcmSafely(processedPcm, sampleRate)
                val updatedPeaks = AudioSynthAndDspEngine.computeWaveformPeaks(processedPcm)
                _uiState.update { state ->
                    state.copy(
                        audioVaultEntries = state.audioVaultEntries.map { entry ->
                            if (entry.id == selected.id) entry.copy(waveformPeaks = updatedPeaks) else entry
                        },
                        statusMessage = "Auditioning '${selected.title}' through ${profile.displayName} (${profile.bitDepth}-bit DSP)"
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Audio DSP audition failed", e)
                _uiState.update { it.copy(statusMessage = "Audio audition failed: ${e.message}") }
            }
        }
    }

    fun exportSelectedAudioSpecJson() {
        val snapshot = _uiState.value
        val selected = snapshot.audioVaultEntries.firstOrNull { it.id == snapshot.selectedAudioEntryId }
            ?: snapshot.audioVaultEntries.firstOrNull()
        if (selected == null) {
            postStatusMessage("No audio asset selected for JSON spec export.")
            return
        }
        val fileLabel = when {
            selected.scoreSpec != null -> "score_seq.json"
            selected.crySpec != null -> "procedural_cry.json"
            else -> "audio_cue.json"
        }
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export Audio Spec ($fileLabel — ${selected.title})",
                exportDialogText = selected.toExportSpecJson()
            )
        }
    }

    fun importRawBytesThroughSixStageVault(
        context: Context,
        rawBytes: ByteArray,
        fileName: String,
        slotName: String,
        sensitive: Boolean
    ) {
        val appCtx = context.applicationContext
        val snapshot = _uiState.value
        val gameTag = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
        val groupTag = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        val charTag = snapshot.activeCharacterTagForImports.ifBlank { "Studio" }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (result, updatedManifest) = SixStageAssetImporter.importRawBytes(
                    context = appCtx,
                    rawBytes = rawBytes,
                    fileName = fileName,
                    slotName = slotName,
                    sensitive = sensitive,
                    gameTag = gameTag,
                    groupTag = groupTag,
                    characterTag = charTag,
                    existingCatalog = snapshot.vaultCatalog,
                    activeSkinManifest = snapshot.activeSkinManifest
                )

                if (!result.success || result.entry == null) {
                    _uiState.update { state ->
                        state.copy(
                            lastSixStageImportResult = result,
                            statusMessage = result.errorMessage ?: "6-Stage import rejected."
                        )
                    }
                    return@launch
                }

                val newEntry = result.entry
                val updatedCatalog = listOf(newEntry) + snapshot.vaultCatalog.filterNot { it.id == newEntry.id }
                val updatedStats = HotPhoneLruCacheManager.computeStats(updatedCatalog)
                VaultSettingsAndCatalogStore.saveCatalog(appCtx, updatedCatalog)
                VaultSettingsAndCatalogStore.saveActiveSkinManifest(appCtx, updatedManifest)

                // If this import is an audio asset, also register it inside the First-Class Audio Vault
                val importedAudioEntry = if (newEntry.assetKind == VaultAssetKind.AUDIO) {
                    buildAudioVaultEntryFromVaultImport(newEntry)
                } else {
                    null
                }

                _uiState.update { state ->
                    val updatedAudioList = if (importedAudioEntry != null) {
                        listOf(importedAudioEntry) + state.audioVaultEntries.filterNot { it.id == importedAudioEntry.id }
                    } else {
                        state.audioVaultEntries
                    }
                    state.copy(
                        vaultCatalog = updatedCatalog,
                        activeSkinManifest = updatedManifest,
                        lastSixStageImportResult = result,
                        hotCacheStats = updatedStats,
                        audioVaultEntries = updatedAudioList,
                        selectedAudioEntryId = importedAudioEntry?.id ?: state.selectedAudioEntryId,
                        statusMessage = if (result.wasDeduplicated) {
                            "6-Stage Import deduplicated (SHA-256 ${newEntry.sha256Hex.take(10)}…) & bound slot '${newEntry.slotName}'"
                        } else {
                            "6-Stage Import passed all 6 stages: stored '${newEntry.title}' & bound slot '${newEntry.slotName}'"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "6-Stage Vault import failed", e)
                _uiState.update { it.copy(statusMessage = "6-Stage import error: ${e.message}") }
            }
        }
    }

    fun offloadVaultEntryToColdVault(context: Context, entryId: String, passphrase: String) {
        if (passphrase.isBlank()) {
            postStatusMessage("Enter a Cold Vault passphrase before encrypting to .vault.enc.")
            return
        }
        val appCtx = context.applicationContext
        val snapshot = _uiState.value
        val target = snapshot.vaultCatalog.firstOrNull { it.id == entryId }
        if (target == null) {
            postStatusMessage("Vault entry not found.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val offloaded = EncryptedColdVaultManager.offloadEntryToColdVault(appCtx, target, passphrase)
                val updatedCatalog = _uiState.value.vaultCatalog.map { if (it.id == entryId) offloaded else it }
                val updatedStats = HotPhoneLruCacheManager.computeStats(updatedCatalog)
                VaultSettingsAndCatalogStore.saveCatalog(appCtx, updatedCatalog)
                _uiState.update { state ->
                    state.copy(
                        vaultCatalog = updatedCatalog,
                        hotCacheStats = updatedStats,
                        statusMessage = "Encrypted '${offloaded.title}' to AES-256-GCM .vault.enc & cleared local hot copy."
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Cold vault offload failed", e)
                _uiState.update { it.copy(statusMessage = "Cold vault offload failed: ${e.message}") }
            }
        }
    }

    fun hydrateVaultEntryFromColdVault(context: Context, entryId: String, passphrase: String) {
        if (passphrase.isBlank()) {
            postStatusMessage("Enter your Cold Vault passphrase to decrypt and hydrate this asset.")
            return
        }
        val appCtx = context.applicationContext
        val target = _uiState.value.vaultCatalog.firstOrNull { it.id == entryId }
        if (target == null) {
            postStatusMessage("Vault entry not found.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hydrated = EncryptedColdVaultManager.hydrateEntryFromColdVault(appCtx, target, passphrase)
                val updatedCatalog = _uiState.value.vaultCatalog.map { if (it.id == entryId) hydrated else it }
                val updatedStats = HotPhoneLruCacheManager.computeStats(updatedCatalog)
                VaultSettingsAndCatalogStore.saveCatalog(appCtx, updatedCatalog)
                _uiState.update { state ->
                    state.copy(
                        vaultCatalog = updatedCatalog,
                        hotCacheStats = updatedStats,
                        statusMessage = "Hydrated '${hydrated.title}' from .vault.enc back to Hot Phone Cache."
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Cold vault hydrate failed", e)
                _uiState.update { it.copy(statusMessage = "Decrypt/hydrate failed (wrong passphrase or corrupted archive): ${e.message}") }
            }
        }
    }

    fun trimHotPhoneCacheLru(context: Context, passphrase: String) {
        val appCtx = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentCatalog = _uiState.value.vaultCatalog
                val (updatedCatalog, evictedCount) = HotPhoneLruCacheManager.enforceLruBudget(
                    context = appCtx,
                    catalog = currentCatalog,
                    maxCacheBytes = HotPhoneLruCacheManager.DEFAULT_MAX_CACHE_BYTES,
                    passphrase = passphrase
                )
                val updatedStats = HotPhoneLruCacheManager.computeStats(updatedCatalog)
                VaultSettingsAndCatalogStore.saveCatalog(appCtx, updatedCatalog)
                _uiState.update { state ->
                    state.copy(
                        vaultCatalog = updatedCatalog,
                        hotCacheStats = updatedStats,
                        statusMessage = if (evictedCount > 0) {
                            "LRU Trim offloaded $evictedCount oldest asset(s) to Cold Vault."
                        } else {
                            "Hot Phone Cache is within the 200 MB budget (${String.format("%.2f", updatedStats.usedBytes / (1024.0 * 1024.0))} MB used)."
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "LRU cache trim failed", e)
                _uiState.update { it.copy(statusMessage = "LRU cache trim failed: ${e.message}") }
            }
        }
    }

    fun updateColdVaultEndpointConfig(endpointUrl: String, bucketName: String, context: Context? = null) {
        val cleanUrl = endpointUrl.trim()
        if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            postStatusMessage("R2 endpoint URL must start with https:// (or leave blank for Local / USB-C Cold Vault).")
            return
        }
        val ctx = context?.applicationContext ?: appContext
        val updatedConfig = if (ctx != null) {
            VaultSettingsAndCatalogStore.saveEndpointConfig(ctx, cleanUrl, bucketName)
        } else {
            val modeLabel = if (cleanUrl.startsWith("https://", ignoreCase = true) && bucketName.isNotBlank()) {
                "Cloudflare R2 Configured (${bucketName.trim()})"
            } else {
                "Local / USB-C Cold Vault"
            }
            ColdVaultEndpointConfig(cleanUrl, bucketName.trim(), modeLabel)
        }
        _uiState.update {
            it.copy(
                coldVaultEndpointConfig = updatedConfig,
                statusMessage = "Cold Vault storage mode saved: ${updatedConfig.backupModeLabel}"
            )
        }
    }

    fun configureVaultPin(pin: String, context: Context? = null) {
        val trimmed = pin.trim()
        if (trimmed.length < 4) {
            postStatusMessage("Vault PIN must be at least 4 digits/characters.")
            return
        }
        val ctx = context?.applicationContext ?: appContext
        val saved = if (ctx != null) VaultSettingsAndCatalogStore.configurePin(ctx, trimmed) else true
        if (saved) {
            _uiState.update {
                it.copy(
                    isVaultPinConfigured = true,
                    isVaultUnlocked = true,
                    statusMessage = "Vault PIN configured with salted SHA-256 verification."
                )
            }
        } else {
            postStatusMessage("Unable to configure Vault PIN.")
        }
    }

    fun unlockVaultWithPin(pin: String, context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext
        val verified = if (ctx != null) {
            VaultSettingsAndCatalogStore.verifyPin(ctx, pin)
        } else {
            false
        }
        if (verified) {
            _uiState.update {
                it.copy(
                    isVaultUnlocked = true,
                    statusMessage = "Vault unlocked."
                )
            }
        } else {
            postStatusMessage("Incorrect Vault PIN.")
        }
    }

    fun lockVaultNow() {
        _uiState.update { state ->
            if (!state.isVaultPinConfigured) {
                state.copy(statusMessage = "Set a Vault PIN first before locking the Cold Vault.")
            } else {
                state.copy(
                    isVaultUnlocked = false,
                    statusMessage = "Vault locked."
                )
            }
        }
    }

    private fun resolvePcmForAudioEntry(entry: AudioAssetEntry): Pair<ShortArray, Int> {
        entry.crySpec?.let { spec ->
            return AudioSynthAndDspEngine.synthesizeProceduralCryPcm(spec, entry.sampleRateHz) to entry.sampleRateHz
        }
        entry.scoreSpec?.let { spec ->
            return AudioSynthAndDspEngine.synthesizeScoreSequencePcm(spec, entry.sampleRateHz) to entry.sampleRateHz
        }
        if (entry.filePath.isNotBlank()) {
            val file = File(entry.filePath)
            if (file.exists() && entry.format == AudioAssetFormat.WAV_PCM) {
                val decoded = AudioSynthAndDspEngine.decodeWavBytesToPcm(file.readBytes())
                if (decoded != null) return decoded
            }
        }
        // Deterministic fallback cry synthesis for non-PCM containers (e.g., MIDI/OGG cue preview)
        val fallbackCry = ProceduralCrySpec(
            speciesKey = entry.slotKey,
            baseFreqHz = 420f,
            endFreqHz = 660f,
            durationMs = entry.durationMs.coerceIn(200, 1200),
            modulationHz = 18f,
            noiseMix = 0.08f
        )
        return AudioSynthAndDspEngine.synthesizeProceduralCryPcm(fallbackCry, 22050) to 22050
    }

    private fun buildAudioVaultEntryFromVaultImport(entry: VaultCatalogEntry): AudioAssetEntry {
        val file = File(entry.localFilePath)
        val ext = entry.extension.lowercase()
        var format = when (ext) {
            "wav" -> AudioAssetFormat.WAV_PCM
            "ogg" -> AudioAssetFormat.OGG_VORBIS
            "mid", "midi" -> AudioAssetFormat.MIDI_SEQ
            else -> AudioAssetFormat.AUDIO_CUE_JSON
        }
        var category = AudioUsageCategory.SFX_ONE_SHOT
        var sampleRate = 22050
        var durationMs = 450
        var peaks: List<Float> = List(48) { idx -> if (idx % 3 == 0) 0.55f else 0.25f }
        var crySpec: ProceduralCrySpec? = null
        var scoreSpec: ScoreSequenceSpec? = null

        try {
            if (ext == "wav" && file.exists()) {
                val decoded = AudioSynthAndDspEngine.decodeWavBytesToPcm(file.readBytes())
                if (decoded != null) {
                    val (pcm, rate) = decoded
                    sampleRate = rate
                    durationMs = ((pcm.size.toDouble() / rate.toDouble()) * 1000.0).roundToInt().coerceAtLeast(1)
                    peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm)
                    if (durationMs > 3000) category = AudioUsageCategory.BGM_LOOP
                }
            } else if (ext == "json" && file.exists()) {
                val rawJson = file.readText()
                if (rawJson.contains("\"notes\"") && rawJson.contains("\"bpm\"")) {
                    val parsedScore = ScoreSequenceSpec.fromJsonString(rawJson)
                    val pcm = AudioSynthAndDspEngine.synthesizeScoreSequencePcm(parsedScore, 22050)
                    scoreSpec = parsedScore
                    format = AudioAssetFormat.SCORE_SEQ_JSON
                    category = AudioUsageCategory.BGM_LOOP
                    durationMs = ((pcm.size.toDouble() / 22050.0) * 1000.0).roundToInt().coerceAtLeast(1)
                    peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm)
                } else if (rawJson.contains("\"baseFreqHz\"")) {
                    val parsedCry = ProceduralCrySpec.fromJsonString(rawJson)
                    val pcm = AudioSynthAndDspEngine.synthesizeProceduralCryPcm(parsedCry, 22050)
                    crySpec = parsedCry
                    format = AudioAssetFormat.AUDIO_CUE_JSON
                    category = AudioUsageCategory.PROCEDURAL_CRY
                    durationMs = parsedCry.durationMs
                    peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm)
                }
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "Audio metadata extraction fallback for '${entry.title}': ${e.message}")
        }

        return AudioAssetEntry(
            id = "audio_${entry.id}",
            slotKey = entry.slotName,
            title = entry.title,
            category = category,
            format = format,
            sampleRateHz = sampleRate,
            durationMs = durationMs,
            waveformPeaks = peaks,
            filePath = entry.localFilePath,
            sha256Hex = entry.sha256Hex,
            byteSize = entry.byteSize,
            gameTag = entry.gameTag,
            groupTag = entry.groupTag,
            characterTag = entry.characterTag,
            crySpec = crySpec,
            scoreSpec = scoreSpec
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // FROM-SCRATCH 2D SPRITE/ANIMATION CANVAS EDITOR & PURE FROM-SCRATCH AI ASSET CREATOR
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private fun pushScratchUndoSnapshot(projectSnapshot: ScratchSpriteProject) {
        if (scratchUndoStack.size >= maxScratchUndoSnapshots) {
            scratchUndoStack.removeFirst()
        }
        scratchUndoStack.addLast(projectSnapshot.deepCopy())
        scratchRedoStack.clear()
    }

    fun startNewBlankSpriteProject(name: String, sizePx: Int, frameCount: Int) {
        val cleanName = name.trim().ifBlank { "New Original Sprite" }
        val blankProject = SpriteCanvasEditorEngine.createBlankProject(
            name = cleanName,
            width = sizePx,
            height = sizePx,
            frameCount = frameCount
        )
        scratchUndoStack.clear()
        scratchRedoStack.clear()
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = blankProject,
                scratchCanUndo = false,
                scratchCanRedo = false,
                statusMessage = "Started blank ${blankProject.width}×${blankProject.height}px canvas (${blankProject.frames.size} frame${if (blankProject.frames.size == 1) "" else "s"}) for '${blankProject.name}'."
            )
        }
    }

    fun updateScratchSpriteName(name: String) {
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = state.scratchSpriteProject.copy(name = name)
            )
        }
    }

    fun setPixelDrawTool(tool: PixelDrawTool) {
        _uiState.update { it.copy(activeDrawTool = tool) }
    }

    fun setBrushColorInt(colorInt: Int) {
        _uiState.update { it.copy(activeBrushColorInt = colorInt) }
    }

    fun setBrushSizePx(size: Int) {
        _uiState.update { it.copy(activeBrushSizePx = size.coerceIn(1, 3)) }
    }

    fun toggleScratchMirrorX() {
        _uiState.update { state ->
            val nextMirror = !state.scratchSpriteProject.mirrorX
            state.copy(
                scratchSpriteProject = state.scratchSpriteProject.copy(mirrorX = nextMirror),
                statusMessage = if (nextMirror) "Mirror X symmetry enabled." else "Mirror X symmetry disabled."
            )
        }
    }

    fun toggleScratchOnionSkin() {
        _uiState.update { state ->
            state.copy(scratchOnionSkinEnabled = !state.scratchOnionSkinEnabled)
        }
    }

    fun applyCanvasStrokeAt(x: Int, y: Int, pushUndoSnapshot: Boolean) {
        val state = _uiState.value
        val project = state.scratchSpriteProject
        if (x !in 0 until project.width || y !in 0 until project.height) return
        val activeIdx = project.activeFrameIndex.coerceIn(0, (project.frames.size - 1).coerceAtLeast(0))
        val currentPixels = project.frames.getOrNull(activeIdx) ?: return

        if (state.activeDrawTool == PixelDrawTool.EYEDROPPER) {
            val pickedColor = currentPixels[y * project.width + x]
            val resolvedColor = if (Color.alpha(pickedColor) == 0) state.activeBrushColorInt else pickedColor
            _uiState.update {
                it.copy(
                    activeBrushColorInt = resolvedColor,
                    activeDrawTool = PixelDrawTool.PENCIL
                )
            }
            return
        }

        val updatedFramePixels = when (state.activeDrawTool) {
            PixelDrawTool.PENCIL -> SpriteCanvasEditorEngine.paintPixel(
                framePixels = currentPixels,
                width = project.width,
                height = project.height,
                x = x,
                y = y,
                colorInt = state.activeBrushColorInt,
                brushSize = state.activeBrushSizePx,
                mirrorX = project.mirrorX
            )
            PixelDrawTool.ERASER -> SpriteCanvasEditorEngine.paintPixel(
                framePixels = currentPixels,
                width = project.width,
                height = project.height,
                x = x,
                y = y,
                colorInt = Color.TRANSPARENT,
                brushSize = state.activeBrushSizePx,
                mirrorX = project.mirrorX
            )
            PixelDrawTool.FILL_BUCKET -> SpriteCanvasEditorEngine.floodFill(
                framePixels = currentPixels,
                width = project.width,
                height = project.height,
                startX = x,
                startY = y,
                replacementColor = state.activeBrushColorInt
            )
            PixelDrawTool.EYEDROPPER -> currentPixels
        }

        if (updatedFramePixels.contentEquals(currentPixels)) return

        if (pushUndoSnapshot) {
            pushScratchUndoSnapshot(project)
        }

        val newFrames = project.frames.mapIndexed { idx, buf ->
            if (idx == activeIdx) updatedFramePixels else buf
        }
        _uiState.update { s ->
            s.copy(
                scratchSpriteProject = project.copy(frames = newFrames),
                scratchCanUndo = scratchUndoStack.isNotEmpty(),
                scratchCanRedo = scratchRedoStack.isNotEmpty()
            )
        }
    }

    fun undoScratchSprite() {
        if (scratchUndoStack.isEmpty()) return
        val current = _uiState.value.scratchSpriteProject
        if (scratchRedoStack.size >= maxScratchUndoSnapshots) {
            scratchRedoStack.removeFirst()
        }
        scratchRedoStack.addLast(current.deepCopy())
        val previous = scratchUndoStack.removeLast()
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = previous,
                scratchCanUndo = scratchUndoStack.isNotEmpty(),
                scratchCanRedo = scratchRedoStack.isNotEmpty()
            )
        }
    }

    fun redoScratchSprite() {
        if (scratchRedoStack.isEmpty()) return
        val current = _uiState.value.scratchSpriteProject
        if (scratchUndoStack.size >= maxScratchUndoSnapshots) {
            scratchUndoStack.removeFirst()
        }
        scratchUndoStack.addLast(current.deepCopy())
        val next = scratchRedoStack.removeLast()
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = next,
                scratchCanUndo = scratchUndoStack.isNotEmpty(),
                scratchCanRedo = scratchRedoStack.isNotEmpty()
            )
        }
    }

    fun selectScratchFrame(index: Int) {
        _uiState.update { state ->
            val maxIdx = (state.scratchSpriteProject.frames.size - 1).coerceAtLeast(0)
            state.copy(
                scratchSpriteProject = state.scratchSpriteProject.copy(
                    activeFrameIndex = index.coerceIn(0, maxIdx)
                )
            )
        }
    }

    fun addBlankScratchFrame() {
        val current = _uiState.value.scratchSpriteProject
        if (current.frames.size >= SpriteCanvasEditorEngine.MAX_FRAME_COUNT) {
            postStatusMessage("Maximum ${SpriteCanvasEditorEngine.MAX_FRAME_COUNT} animation frames reached.")
            return
        }
        pushScratchUndoSnapshot(current)
        val updated = SpriteCanvasEditorEngine.addBlankFrame(current)
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = updated,
                scratchCanUndo = scratchUndoStack.isNotEmpty(),
                scratchCanRedo = scratchRedoStack.isNotEmpty(),
                statusMessage = "Added blank frame #${updated.frames.size}."
            )
        }
    }

    fun duplicateActiveScratchFrame() {
        val current = _uiState.value.scratchSpriteProject
        if (current.frames.size >= SpriteCanvasEditorEngine.MAX_FRAME_COUNT) {
            postStatusMessage("Maximum ${SpriteCanvasEditorEngine.MAX_FRAME_COUNT} animation frames reached.")
            return
        }
        pushScratchUndoSnapshot(current)
        val updated = SpriteCanvasEditorEngine.duplicateActiveFrame(current)
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = updated,
                scratchCanUndo = scratchUndoStack.isNotEmpty(),
                scratchCanRedo = scratchRedoStack.isNotEmpty(),
                statusMessage = "Duplicated frame to slot #${updated.activeFrameIndex + 1}."
            )
        }
    }

    fun deleteActiveScratchFrame() {
        val current = _uiState.value.scratchSpriteProject
        if (current.frames.size <= SpriteCanvasEditorEngine.MIN_FRAME_COUNT) {
            postStatusMessage("At least 1 animation frame is required.")
            return
        }
        pushScratchUndoSnapshot(current)
        val updated = SpriteCanvasEditorEngine.deleteActiveFrame(current)
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = updated,
                scratchCanUndo = scratchUndoStack.isNotEmpty(),
                scratchCanRedo = scratchRedoStack.isNotEmpty(),
                statusMessage = "Deleted frame (${updated.frames.size} frame${if (updated.frames.size == 1) "" else "s"} remaining)."
            )
        }
    }

    fun clearActiveScratchFrame() {
        val current = _uiState.value.scratchSpriteProject
        pushScratchUndoSnapshot(current)
        val updated = SpriteCanvasEditorEngine.clearActiveFrame(current)
        _uiState.update { state ->
            state.copy(
                scratchSpriteProject = updated,
                scratchCanUndo = scratchUndoStack.isNotEmpty(),
                scratchCanRedo = scratchRedoStack.isNotEmpty(),
                statusMessage = "Cleared active frame to transparent."
            )
        }
    }

    fun setScratchFps(fps: Int) {
        updateTargetAssetFps(fps)
    }

    fun commitScratchSpriteProjectToStudioAndVault() {
        val snapshot = _uiState.value
        val project = snapshot.scratchSpriteProject
        val exportedSheet = SpriteCanvasEditorEngine.exportProjectToSpriteSheet(project)
        val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
        val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        val activeChar = exportedSheet.name.ifBlank { "New Original Sprite" }

        val updatedRig = RigMetadata.defaultForSprite(
            name = exportedSheet.name,
            width = exportedSheet.frameWidth,
            height = exportedSheet.frameHeight,
            frameCount = exportedSheet.frames.size
        )

        val savedLibEntry = SavedLibraryAsset(
            id = "lib_scratch_${System.currentTimeMillis()}",
            title = exportedSheet.name,
            characterTag = activeChar,
            gameTag = activeGame,
            groupTag = activeGroup,
            type = LibraryAssetType.SPRITE_2D,
            spriteSheet = exportedSheet,
            hardwareBadge = snapshot.selectedHardware.shortName,
            metricsSummary = "${exportedSheet.frameWidth}×${exportedSheet.frameHeight}px • ${exportedSheet.frames.size}f • SCRATCH"
        )

        val historyEntity = AssetHistoryEntity.fromStudioAssets(
            title = exportedSheet.name,
            characterTag = activeChar,
            assetType = "SPRITE_2D",
            sourceOrigin = "SCRATCH 2D CANVAS",
            hardwareTarget = snapshot.selectedHardware.shortName,
            metricsSummary = "${exportedSheet.frameWidth}×${exportedSheet.frameHeight}px • ${exportedSheet.frames.size} frames",
            spriteSheet = exportedSheet,
            gameTag = activeGame,
            groupTag = activeGroup
        )

        _uiState.update { state ->
            val nextHistoryId = (state.roomAssetHistory.maxOfOrNull { it.id } ?: 0L) + 1L
            state.copy(
                currentSpriteSheet = exportedSheet,
                hasImportedSprite = true,
                activeRigMetadata = updatedRig,
                savedLibraryAssets = listOf(savedLibEntry) + state.savedLibraryAssets,
                roomAssetHistory = listOf(historyEntity.copy(id = nextHistoryId)) + state.roomAssetHistory,
                statusMessage = "Saved brand-new sprite '${exportedSheet.name}' (${exportedSheet.frames.size}f) to Studio, Library & Room History!"
            )
        }

        // Persist asynchronously to Room if attached (without duplicating in-memory state)
        val repo = historyRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.recordGeneratedAsset(historyEntity)
                    val dbRows = repo.getAllHistorySnapshot()
                    if (dbRows.isNotEmpty()) {
                        _uiState.update { it.copy(roomAssetHistory = dbRows) }
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Room persistence skipped for scratch sprite: ${e.message}")
                }
            }
        }

        // Stage under active Game -> Group -> Character pool
        registerAssetAndAutoFuseIfMatched(
            StagedCharacterAsset(
                id = "staged_scratch_${System.currentTimeMillis()}",
                assetTitle = exportedSheet.name,
                characterName = activeChar,
                modality = CharacterAssetModality.SPRITESHEET_2D,
                isUserImported = true,
                spriteSheet = exportedSheet,
                gameTag = activeGame,
                groupTag = activeGroup
            )
        )
    }

    fun updateScratchCreateAssetName(name: String) {
        _uiState.update { it.copy(scratchCreateAssetName = name) }
    }

    fun createBrandNewAssetFromScratchWithAi(alsoExtrude3D: Boolean = false) {
        val snapshot = _uiState.value
        val rawSubject = snapshot.promptText.trim()
        if (rawSubject.isBlank()) {
            _uiState.update {
                it.copy(
                    lastScratchAiError = "Enter a subject description before generating a brand-new asset from scratch.",
                    statusMessage = "Describe your new asset first."
                )
            }
            return
        }

        val subject = if (snapshot.userAiTrainingProfile.applyToFormBuilder) {
            snapshot.userAiTrainingProfile.applyTriggerToPrompt(rawSubject)
        } else {
            rawSubject
        }
        val targetFps = snapshot.targetAssetFps.coerceIn(1, 120)
        val assetName = snapshot.scratchCreateAssetName.trim()
        val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
        val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        val activeChar = assetName.ifBlank {
            rawSubject.split(Regex("\\s+")).take(3).joinToString(" ").ifBlank { "Original Creation" }
        }
        val paletteHexes = snapshot.selectedColorWheelHexes.ifEmpty {
            snapshot.extractedPalette.map { it.hex }
        }
        val sharedContextBlock = buildCombinedSharedTrainingContext(
            forChat = false,
            queryHint = subject,
            state = snapshot
        )

        _uiState.update {
            it.copy(
                isCreatingFromScratchWithAi = true,
                lastScratchAiError = null,
                statusMessage = "Generating brand-new asset from scratch (${snapshot.promptAssetPreset.displayName} • ${targetFps} FPS • ${snapshot.promptHardwareTier.displayName})..."
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = GeminiAssetForge.createBrandNewAssetFromScratch(
                    assetName = assetName,
                    subjectDescription = subject,
                    tier = snapshot.promptHardwareTier,
                    preset = snapshot.promptAssetPreset,
                    includeHumanoidHeroicProportions = snapshot.includeHumanoidHeroicProportions,
                    paletteHexList = paletteHexes,
                    gameTag = activeGame,
                    groupTag = activeGroup,
                    characterTag = activeChar,
                    sharedContextBlock = sharedContextBlock,
                    autoStripChromaKey = snapshot.stripSolidBgOnImport,
                    alsoExtrude3D = alsoExtrude3D,
                    autoRetryOnStyleFail = snapshot.autoRetryAiStyleGate,
                    targetFps = targetFps
                )

                val audit = result.autoAuditReport
                val repairNote = formatAutoStyleRepairNote(
                    report = audit,
                    attempted = result.autoRepairAttempted,
                    improved = result.autoRepairImprovedScore
                )
                val auditMetricsTag = audit?.let { " • Audit ${it.styleFidelityScore}% (${it.fidelityTag.badge})" } ?: ""

                val savedType = if (result.extrudedMeshOrNull != null) {
                    LibraryAssetType.UNIFIED_PAIR
                } else {
                    LibraryAssetType.SPRITE_2D
                }
                val savedEntry = SavedLibraryAsset(
                    id = "lib_ai_scratch_${System.currentTimeMillis()}",
                    title = result.title,
                    characterTag = activeChar,
                    gameTag = activeGame,
                    groupTag = activeGroup,
                    type = savedType,
                    mesh = result.extrudedMeshOrNull,
                    spriteSheet = result.spriteSheet,
                    hardwareBadge = snapshot.selectedHardware.shortName,
                    metricsSummary = "${result.spriteSheet.frameWidth}×${result.spriteSheet.frameHeight}px • ${result.spriteSheet.frames.size}f @${targetFps}fps • ${result.modelUsed}$auditMetricsTag"
                )

                val updatedRig = RigMetadata.defaultForSprite(
                    name = result.title,
                    width = result.spriteSheet.frameWidth,
                    height = result.spriteSheet.frameHeight,
                    frameCount = result.spriteSheet.frames.size
                ).copy(cadenceFps = AiTrainingDatasetExporter.nearestRigCadence(targetFps))

                _uiState.update { state ->
                    state.copy(
                        isCreatingFromScratchWithAi = false,
                        lastScratchAiError = null,
                        lastScratchAiCreationBitmap = result.rawGeneratedBitmap,
                        lastAssetAuditReport = audit ?: state.lastAssetAuditReport,
                        lastAiAutoRepairNote = repairNote ?: state.lastAiAutoRepairNote,
                        currentSpriteSheet = result.spriteSheet,
                        currentMesh = result.extrudedMeshOrNull ?: state.currentMesh,
                        activeRigMetadata = updatedRig,
                        savedLibraryAssets = listOf(savedEntry) + state.savedLibraryAssets,
                        lastAiBuildSummary = "Built 2D sprite '${result.title}' (${result.spriteSheet.frames.size}f @ ${targetFps} FPS, key ${result.chromaKeyHex}) via ${result.modelUsed}$auditMetricsTag — open Sprite Studio to inspect or edit.",
                        statusMessage = "Created '${result.title}' from scratch via ${result.modelUsed} (${result.spriteSheet.frames.size}f @ ${targetFps} FPS, key ${result.chromaKeyHex}$auditMetricsTag)!"
                    )
                }

                recordAssetInRoomHistory(
                    title = result.title,
                    characterTag = activeChar,
                    assetType = if (result.extrudedMeshOrNull != null) "FUSED_3D_2D" else "SPRITE_2D",
                    sourceOrigin = "AI SCRATCH CREATION",
                    hardwareTarget = snapshot.selectedHardware.shortName,
                    metricsSummary = "${result.spriteSheet.frameWidth}×${result.spriteSheet.frameHeight}px • ${result.spriteSheet.frames.size}f • ${result.modelUsed}$auditMetricsTag",
                    mesh = result.extrudedMeshOrNull,
                    spriteSheet = result.spriteSheet,
                    paletteHexList = paletteHexes,
                    gameTag = activeGame,
                    groupTag = activeGroup
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "From-scratch AI asset creation failed", e)
                val msg = e.localizedMessage
                    ?: "AI generation failed. Connect a provider in OmniAiHub or draw offline on the Blank 2D Pixel Canvas."
                _uiState.update { state ->
                    state.copy(
                        isCreatingFromScratchWithAi = false,
                        lastScratchAiError = msg,
                        statusMessage = msg
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Creator Upgrade B: From-Scratch 3D Primitive / Kitbash Model Builder
    // -------------------------------------------------------------------------

    fun startNewBlank3DProject(name: String = "New Original 3D Model") {
        val blank = Scratch3DBuilderEngine.createEmptyProject(name)
        _uiState.update { state ->
            state.copy(
                scratch3DProject = blank,
                statusMessage = "Started blank 3D scene '${blank.name}' (0 parts). Tap any shape to start building."
            )
        }
    }

    fun updateScratch3DProjectName(name: String) {
        _uiState.update { state ->
            state.copy(
                scratch3DProject = state.scratch3DProject.copy(name = name)
            )
        }
    }

    fun addScratch3DPart(shape: PrimitiveShapeType) {
        val colorPalette = longArrayOf(
            0xFF00E5FF,
            0xFFFF2A85,
            0xFF10B981,
            0xFFF59E0B,
            0xFF8B5CF6,
            0xFF38BDF8,
            0xFFE2E8F0
        )
        _uiState.update { state ->
            val nextColor = colorPalette[state.scratch3DProject.parts.size % colorPalette.size]
            val updated = Scratch3DBuilderEngine.addPart(
                project = state.scratch3DProject,
                shape = shape,
                colorLong = nextColor
            )
            state.copy(
                scratch3DProject = updated,
                statusMessage = "Added ${shape.label} to '${updated.name}' (${updated.parts.size} part${if (updated.parts.size == 1) "" else "s"})."
            )
        }
    }

    fun selectScratch3DPart(partId: String) {
        _uiState.update { state ->
            if (state.scratch3DProject.parts.none { it.id == partId }) return@update state
            state.copy(
                scratch3DProject = state.scratch3DProject.copy(selectedPartId = partId)
            )
        }
    }

    fun updateSelectedScratch3DPart(transform: (Scratch3DPart) -> Scratch3DPart) {
        _uiState.update { state ->
            val updated = Scratch3DBuilderEngine.updateSelectedPart(state.scratch3DProject, transform)
            state.copy(scratch3DProject = updated)
        }
    }

    fun duplicateSelectedScratch3DPart() {
        _uiState.update { state ->
            if (state.scratch3DProject.selectedPart == null) {
                return@update state.copy(statusMessage = "Add and select a 3D part before duplicating.")
            }
            val updated = Scratch3DBuilderEngine.duplicateSelectedPart(state.scratch3DProject)
            state.copy(
                scratch3DProject = updated,
                statusMessage = "Duplicated selected part (${updated.parts.size} parts total)."
            )
        }
    }

    fun deleteSelectedScratch3DPart() {
        _uiState.update { state ->
            if (state.scratch3DProject.selectedPart == null) {
                return@update state.copy(statusMessage = "No 3D part selected to delete.")
            }
            val updated = Scratch3DBuilderEngine.deleteSelectedPart(state.scratch3DProject)
            state.copy(
                scratch3DProject = updated,
                statusMessage = "Deleted 3D part (${updated.parts.size} part${if (updated.parts.size == 1) "" else "s"} remaining)."
            )
        }
    }

    fun commitScratch3DProjectToStudioAndVault() {
        val snapshot = _uiState.value
        val project = snapshot.scratch3DProject
        if (project.parts.isEmpty()) {
            postStatusMessage("Add at least 1 3D shape before baking & saving your model.")
            return
        }

        val compiledMesh = Scratch3DBuilderEngine.compileProjectToMesh3D(project)
        val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
        val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        val activeChar = compiledMesh.name.ifBlank { "New Original 3D Model" }
        val metrics = "${compiledMesh.vertexCount}v / ${compiledMesh.faceCount}f • ${project.parts.size} parts"

        val savedLibEntry = SavedLibraryAsset(
            id = "lib_scratch3d_${System.currentTimeMillis()}",
            title = compiledMesh.name,
            characterTag = activeChar,
            gameTag = activeGame,
            groupTag = activeGroup,
            type = LibraryAssetType.MODEL_3D,
            mesh = compiledMesh,
            hardwareBadge = snapshot.selectedHardware.shortName,
            metricsSummary = metrics,
            sourceBadge = "SCRATCH 3D",
            formatSummary = compiledMesh.sourceFormat
        )

        val historyEntity = AssetHistoryEntity.fromStudioAssets(
            title = compiledMesh.name,
            characterTag = activeChar,
            assetType = "MODEL_3D",
            sourceOrigin = "SCRATCH 3D BUILDER",
            hardwareTarget = snapshot.selectedHardware.shortName,
            metricsSummary = metrics,
            mesh = compiledMesh,
            gameTag = activeGame,
            groupTag = activeGroup
        )

        _uiState.update { state ->
            val nextHistoryId = (state.roomAssetHistory.maxOfOrNull { it.id } ?: 0L) + 1L
            state.copy(
                currentMesh = compiledMesh,
                hasImportedMesh = true,
                savedLibraryAssets = listOf(savedLibEntry) + state.savedLibraryAssets,
                roomAssetHistory = listOf(historyEntity.copy(id = nextHistoryId)) + state.roomAssetHistory,
                statusMessage = "Baked & saved brand-new 3D model '${compiledMesh.name}' ($metrics) to Studio, Library & Room History!"
            )
        }

        // Persist asynchronously to Room if attached (without losing synchronous UI state)
        val repo = historyRepository
        if (repo != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repo.recordGeneratedAsset(historyEntity)
                    val dbRows = repo.getAllHistorySnapshot()
                    if (dbRows.isNotEmpty()) {
                        _uiState.update { it.copy(roomAssetHistory = dbRows) }
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Room persistence skipped for scratch 3D mesh: ${e.message}")
                }
            }
        }

        // Stage under active Game -> Group -> Character pool using the brand-new model's own identity
        registerAssetAndAutoFuseIfMatched(
            StagedCharacterAsset(
                id = "staged_scratch3d_${System.currentTimeMillis()}",
                assetTitle = compiledMesh.name,
                characterName = activeChar,
                modality = CharacterAssetModality.MESH_3D,
                isUserImported = true,
                mesh = compiledMesh,
                gameTag = activeGame,
                groupTag = activeGroup
            )
        )
    }

    // -------------------------------------------------------------------------
    // Creator Upgrade B: From-Scratch Texture Painter & Material Generator
    // -------------------------------------------------------------------------

    fun startNewBlankTextureProject(name: String = "New Original Texture", sizePx: Int = 32) {
        val blank = ScratchTextureEngine.createBlankTexture(name = name, sizePx = sizePx)
        _uiState.update { state ->
            state.copy(
                scratchTextureProject = blank,
                statusMessage = "Started blank ${blank.sizePx}×${blank.sizePx}px texture canvas '${blank.name}'."
            )
        }
    }

    fun updateScratchTextureName(name: String) {
        _uiState.update { state ->
            state.copy(
                scratchTextureProject = state.scratchTextureProject.copy(name = name)
            )
        }
    }

    fun generateScratchTexturePattern(
        pattern: ProceduralMaterialPattern,
        primaryColorInt: Int,
        secondaryColorInt: Int,
        accentColorInt: Int
    ) {
        _uiState.update { state ->
            val current = state.scratchTextureProject
            val generated = ScratchTextureEngine.generateMaterialPattern(
                name = current.name.ifBlank { "New ${pattern.label} Texture" },
                sizePx = current.sizePx,
                pattern = pattern,
                primaryColorInt = primaryColorInt,
                secondaryColorInt = secondaryColorInt,
                accentColorInt = accentColorInt
            ).copy(
                mirrorX = current.mirrorX,
                mirrorY = current.mirrorY
            )
            state.copy(
                scratchTextureProject = generated,
                statusMessage = "Generated '${pattern.label}' procedural material on ${generated.sizePx}×${generated.sizePx}px canvas."
            )
        }
    }

    fun setTextureBrushColorInt(colorInt: Int) {
        _uiState.update { it.copy(activeTextureBrushColorInt = colorInt) }
    }

    fun setTextureBrushSizePx(size: Int) {
        _uiState.update { it.copy(activeTextureBrushSizePx = size.coerceIn(1, 3)) }
    }

    fun toggleScratchTextureMirrorX() {
        _uiState.update { state ->
            val next = !state.scratchTextureProject.mirrorX
            state.copy(
                scratchTextureProject = state.scratchTextureProject.copy(mirrorX = next),
                statusMessage = if (next) "Texture Mirror X enabled." else "Texture Mirror X disabled."
            )
        }
    }

    fun toggleScratchTextureMirrorY() {
        _uiState.update { state ->
            val next = !state.scratchTextureProject.mirrorY
            state.copy(
                scratchTextureProject = state.scratchTextureProject.copy(mirrorY = next),
                statusMessage = if (next) "Texture Mirror Y enabled." else "Texture Mirror Y disabled."
            )
        }
    }

    fun paintScratchTextureAt(x: Int, y: Int) {
        _uiState.update { state ->
            val updated = ScratchTextureEngine.paintTexturePixel(
                project = state.scratchTextureProject,
                x = x,
                y = y,
                colorInt = state.activeTextureBrushColorInt,
                brushSize = state.activeTextureBrushSizePx
            )
            state.copy(scratchTextureProject = updated)
        }
    }

    fun commitScratchTextureTo3DStudio() {
        val snapshot = _uiState.value
        val project = snapshot.scratchTextureProject
        val exportBmp = ScratchTextureEngine.toExportBitmap(project, outputDim = 128)
        val cleanName = project.name.trim().ifBlank { "New Original Texture" }
        val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
        val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        val activeChar = cleanName

        _uiState.update { state ->
            state.copy(
                activeTexture = exportBmp,
                activeTextureName = cleanName,
                hasImportedTexture = true,
                statusMessage = "Applied brand-new 128×128 texture '$cleanName' to 3D Studio & staged to Vault!"
            )
        }

        registerAssetAndAutoFuseIfMatched(
            StagedCharacterAsset(
                id = "staged_scratchtex_${System.currentTimeMillis()}",
                assetTitle = cleanName,
                characterName = activeChar,
                modality = CharacterAssetModality.TEXTURE_UV,
                isUserImported = true,
                bitmap = exportBmp,
                gameTag = activeGame,
                groupTag = activeGroup
            )
        )
    }

    // -------------------------------------------------------------------------
    // Creator Upgrade B: From-Scratch Audio Cry Designer & 16-Step Sequencer
    // -------------------------------------------------------------------------

    fun saveBrandNewCryToAudioVault(
        title: String,
        slotKey: String,
        spec: ProceduralCrySpec
    ) {
        val cleanTitle = title.trim().ifBlank { "Original Cry" }
        val cleanSlot = sanitizeAssetSlug(slotKey.ifBlank { cleanTitle }, "cry_custom")
        val normalizedSpec = spec.copy(speciesKey = cleanSlot)
        val sampleRate = 22050
        val pcm = AudioSynthAndDspEngine.synthesizeProceduralCryPcm(normalizedSpec, sampleRateHz = sampleRate)
        val peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm, bucketCount = 48)
        val jsonBytes = normalizedSpec.toJsonString().toByteArray(Charsets.UTF_8)
        val snapshot = _uiState.value

        val newEntry = AudioAssetEntry(
            id = "audio_cry_${System.currentTimeMillis()}_${cleanSlot.take(10)}",
            slotKey = cleanSlot,
            title = cleanTitle,
            category = AudioUsageCategory.PROCEDURAL_CRY,
            format = AudioAssetFormat.AUDIO_CUE_JSON,
            sampleRateHz = sampleRate,
            durationMs = normalizedSpec.durationMs,
            waveformPeaks = peaks,
            byteSize = jsonBytes.size.toLong(),
            gameTag = snapshot.activeGameTagForImports.ifBlank { "General Studio" },
            groupTag = "Audio & SFX",
            characterTag = cleanTitle,
            crySpec = normalizedSpec
        )

        _uiState.update { state ->
            state.copy(
                audioVaultEntries = listOf(newEntry) + state.audioVaultEntries,
                selectedAudioEntryId = newEntry.id,
                statusMessage = "Saved brand-new procedural cry '$cleanTitle' (slot '$cleanSlot') to Audio Vault!"
            )
        }
    }

    fun saveBrandNewSequenceToAudioVault(
        title: String,
        slotKey: String,
        spec: ScoreSequenceSpec
    ) {
        val cleanTitle = title.trim().ifBlank { "Original Chiptune Loop" }
        val cleanSlot = sanitizeAssetSlug(slotKey.ifBlank { cleanTitle }, "bgm_custom")
        val normalizedSpec = spec.copy(title = cleanTitle)
        val sampleRate = 22050
        val pcm = AudioSynthAndDspEngine.synthesizeScoreSequencePcm(normalizedSpec, sampleRateHz = sampleRate)
        val peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm, bucketCount = 48)
        val durationMs = ((pcm.size.toDouble() / sampleRate.toDouble()) * 1000.0)
            .let { kotlin.math.round(it).toInt() }
            .coerceAtLeast(100)
        val jsonBytes = normalizedSpec.toJsonString().toByteArray(Charsets.UTF_8)
        val snapshot = _uiState.value

        val newEntry = AudioAssetEntry(
            id = "audio_seq_${System.currentTimeMillis()}_${cleanSlot.take(10)}",
            slotKey = cleanSlot,
            title = cleanTitle,
            category = AudioUsageCategory.BGM_LOOP,
            format = AudioAssetFormat.SCORE_SEQ_JSON,
            sampleRateHz = sampleRate,
            durationMs = durationMs,
            loopPoints = AudioLoopPoints(
                loopStartSample = 0,
                loopEndSample = pcm.size,
                loopEnabled = true
            ),
            waveformPeaks = peaks,
            byteSize = jsonBytes.size.toLong(),
            gameTag = snapshot.activeGameTagForImports.ifBlank { "General Studio" },
            groupTag = "Audio & SFX",
            characterTag = cleanTitle,
            scoreSpec = normalizedSpec
        )

        _uiState.update { state ->
            state.copy(
                audioVaultEntries = listOf(newEntry) + state.audioVaultEntries,
                selectedAudioEntryId = newEntry.id,
                statusMessage = "Saved brand-new chiptune track '$cleanTitle' (${normalizedSpec.bpm} BPM, ${normalizedSpec.notes.size} notes) to Audio Vault!"
            )
        }
    }

    fun auditionCustomCrySpecWithDsp(spec: ProceduralCrySpec) {
        val profile = _uiState.value.selectedSoundchipProfile
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val basePcm = AudioSynthAndDspEngine.synthesizeProceduralCryPcm(spec, sampleRateHz = 22050)
                val dspPcm = AudioSynthAndDspEngine.applyHardwareSoundchipDsp(basePcm, 22050, profile)
                AudioSynthAndDspEngine.playPcmSafely(dspPcm, sampleRateHz = 22050)
                postStatusMessage("Auditioning custom cry through ${profile.displayName} DSP...")
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to audition custom cry: ${e.message}")
                postStatusMessage("Unable to audition cry: ${e.message}")
            }
        }
    }

    fun auditionCustomScoreSpecWithDsp(spec: ScoreSequenceSpec) {
        val profile = _uiState.value.selectedSoundchipProfile
        if (spec.notes.isEmpty()) {
            postStatusMessage("Tap at least one step on the 16-step grid to audition your sequence.")
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val basePcm = AudioSynthAndDspEngine.synthesizeScoreSequencePcm(spec, sampleRateHz = 22050)
                val dspPcm = AudioSynthAndDspEngine.applyHardwareSoundchipDsp(basePcm, 22050, profile)
                AudioSynthAndDspEngine.playPcmSafely(dspPcm, sampleRateHz = 22050)
                postStatusMessage("Auditioning custom ${spec.bpm} BPM sequence through ${profile.displayName} DSP...")
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to audition custom sequence: ${e.message}")
                postStatusMessage("Unable to audition sequence: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Closed-Loop AI Style Auditing & BYOS Personal Cloud / SAF Folder Actions
    // -------------------------------------------------------------------------

    fun toggleAutoRetryAiStyleGate() {
        _uiState.update { state ->
            val next = !state.autoRetryAiStyleGate
            state.copy(
                autoRetryAiStyleGate = next,
                statusMessage = if (next) {
                    "Automatic Style Audit & 1-Pass Auto-Repair enabled."
                } else {
                    "Automatic 1-Pass Style Repair paused (audit still runs on every AI creation)."
                }
            )
        }
    }

    private fun formatAutoStyleRepairNote(
        report: AssetAuditReport?,
        attempted: Boolean,
        improved: Boolean
    ): String? {
        if (report == null) return null
        return when {
            attempted && improved ->
                "1-Pass Auto-Repair succeeded: improved Style Score to ${report.styleFidelityScore}% (${report.fidelityTag.badge})."
            attempted ->
                "1-Pass Auto-Repair attempted: kept highest-scoring render at ${report.styleFidelityScore}% (${report.fidelityTag.badge})."
            else ->
                "Auto-audited directly: ${report.styleFidelityScore}% (${report.fidelityTag.badge}) • ${if (report.chromaBleedDetected) "Chroma bleed flagged" else "Alpha clean"}."
        }
    }

    fun mountByosSafFolder(context: Context, treeUri: Uri) {
        val appCtx = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val currentCfg = _uiState.value.byosWorkspaceConfig
            val (updatedConfig, report) = ByosCloudFolderManager.mountSafTreeUri(
                context = appCtx,
                treeUri = treeUri,
                existingConfig = currentCfg
            )
            _uiState.update { state ->
                state.copy(
                    byosWorkspaceConfig = updatedConfig,
                    lastByosSyncReport = report,
                    statusMessage = report.summary
                )
            }
        }
    }

    fun syncVaultToByosFolder(context: Context) {
        val appCtx = context.applicationContext
        val snapshot = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val report = ByosCloudFolderManager.syncLocalVaultToSafTree(
                context = appCtx,
                config = snapshot.byosWorkspaceConfig,
                catalog = snapshot.vaultCatalog,
                activeSkin = snapshot.activeSkinManifest
            )
            val refreshedConfig = ByosCloudFolderManager.loadConfig(appCtx)
            _uiState.update { state ->
                state.copy(
                    byosWorkspaceConfig = refreshedConfig,
                    lastByosSyncReport = report,
                    statusMessage = report.summary
                )
            }
        }
    }

    fun readVaultFromByosFolder(context: Context) {
        val appCtx = context.applicationContext
        val snapshot = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val report = ByosCloudFolderManager.readVaultAndSkinFromSafTree(
                context = appCtx,
                config = snapshot.byosWorkspaceConfig
            )
            val loadedSkin = report.syncedSkinManifest
            if (loadedSkin != null) {
                VaultSettingsAndCatalogStore.saveActiveSkinManifest(appCtx, loadedSkin)
            }
            _uiState.update { state ->
                state.copy(
                    activeSkinManifest = loadedSkin ?: state.activeSkinManifest,
                    lastByosSyncReport = report,
                    statusMessage = report.summary
                )
            }
        }
    }

    fun updateByosProviderAndHubConfig(
        context: Context,
        provider: ByosStorageProvider,
        masterHubEmail: String,
        autoSyncOnImport: Boolean
    ) {
        val appCtx = context.applicationContext
        val updated = _uiState.value.byosWorkspaceConfig.copy(
            provider = provider,
            masterHubAccountEmail = masterHubEmail.trim(),
            autoSyncOnImport = autoSyncOnImport
        )
        ByosCloudFolderManager.saveConfig(appCtx, updated)
        _uiState.update { state ->
            state.copy(
                byosWorkspaceConfig = updated,
                statusMessage = "Saved BYOS Workspace target: ${provider.label}${if (updated.masterHubAccountEmail.isNotBlank()) " (${updated.masterHubAccountEmail})" else ""}."
            )
        }
    }

    fun disconnectByosFolder(context: Context) {
        val appCtx = context.applicationContext
        val cleared = _uiState.value.byosWorkspaceConfig.copy(
            persistedTreeUri = "",
            displayFolderName = "",
            lastSyncEpochMs = 0L
        )
        ByosCloudFolderManager.saveConfig(appCtx, cleared)
        _uiState.update { state ->
            state.copy(
                byosWorkspaceConfig = cleared,
                lastByosSyncReport = null,
                statusMessage = "Disconnected personal SAF workspace folder."
            )
        }
    }

    // -------------------------------------------------------------------------
    // AI Multi-Asset Builder (3D Models, Custom Textures, Audio Cries & Chiptunes)
    // -------------------------------------------------------------------------

    fun selectAiBuildTarget(target: AiBuildTargetKind) {
        _uiState.update { state ->
            state.copy(
                selectedAiBuildTarget = target,
                lastScratchAiError = null
            )
        }
    }

    fun buildBrandNewAssetWithAi(alsoExtrude3D: Boolean = false) {
        val snapshot = _uiState.value
        val target = snapshot.selectedAiBuildTarget
        if (target == AiBuildTargetKind.SPRITE_2D) {
            createBrandNewAssetFromScratchWithAi(alsoExtrude3D = alsoExtrude3D)
            return
        }

        val rawSubject = snapshot.promptText.trim()
        if (rawSubject.isBlank()) {
            _uiState.update {
                it.copy(
                    lastScratchAiError = "Enter a description before building a brand-new ${target.label} with AI.",
                    statusMessage = "Describe your ${target.label} first."
                )
            }
            return
        }

        val subject = if (snapshot.userAiTrainingProfile.applyToFormBuilder) {
            snapshot.userAiTrainingProfile.applyTriggerToPrompt(rawSubject)
        } else {
            rawSubject
        }
        val assetName = snapshot.scratchCreateAssetName.trim()
        val activeGame = snapshot.activeGameTagForImports.ifBlank { "General Studio" }
        val activeGroup = snapshot.activeGroupTagForImports.ifBlank { "Core Roster" }
        val activeChar = assetName.ifBlank {
            rawSubject.split(Regex("\\s+")).take(3).joinToString(" ")
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Original Creation" }
        }
        val sharedContextBlock = buildCombinedSharedTrainingContext(
            forChat = false,
            queryHint = subject,
            state = snapshot
        )

        _uiState.update {
            it.copy(
                isCreatingFromScratchWithAi = true,
                lastScratchAiError = null,
                statusMessage = "Building brand-new ${target.label} from scratch with AI..."
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (target) {
                    AiBuildTargetKind.MODEL_3D -> {
                        val built = AiMultiAssetBuilder.build3DModelFromPrompt(
                            assetName = assetName,
                            subjectDescription = subject,
                            sharedContextBlock = sharedContextBlock
                        )
                        val mesh = built.compiledMesh
                        val metrics = "${mesh.vertexCount}v/${mesh.faceCount}f • ${built.project.parts.size} parts • ${built.modelUsed}"
                        val savedEntry = SavedLibraryAsset(
                            id = "lib_ai3d_${System.currentTimeMillis()}",
                            title = mesh.name,
                            characterTag = activeChar,
                            gameTag = activeGame,
                            groupTag = activeGroup,
                            type = LibraryAssetType.MODEL_3D,
                            mesh = mesh,
                            hardwareBadge = snapshot.selectedHardware.shortName,
                            metricsSummary = metrics,
                            sourceBadge = "AI BUILT 3D",
                            formatSummary = built.modelUsed
                        )
                        val summary = "Built ${built.project.parts.size}-part editable 3D model '${mesh.name}' (${mesh.vertexCount}v/${mesh.faceCount}f) via ${built.modelUsed} — open 3D Builder to tweak any part."
                        _uiState.update { state ->
                            state.copy(
                                isCreatingFromScratchWithAi = false,
                                lastScratchAiError = null,
                                scratch3DProject = built.project,
                                currentMesh = mesh,
                                hasImportedMesh = true,
                                savedLibraryAssets = listOf(savedEntry) + state.savedLibraryAssets,
                                lastAiBuildSummary = summary,
                                statusMessage = summary
                            )
                        }
                        recordAssetInRoomHistory(
                            title = mesh.name,
                            characterTag = activeChar,
                            assetType = "MODEL_3D",
                            sourceOrigin = "AI BUILT 3D",
                            hardwareTarget = snapshot.selectedHardware.shortName,
                            metricsSummary = metrics,
                            mesh = mesh,
                            gameTag = activeGame,
                            groupTag = activeGroup
                        )
                        registerAssetAndAutoFuseIfMatched(
                            StagedCharacterAsset(
                                id = "staged_ai3d_${System.currentTimeMillis()}",
                                assetTitle = mesh.name,
                                characterName = activeChar,
                                modality = CharacterAssetModality.MESH_3D,
                                isUserImported = true,
                                mesh = mesh,
                                gameTag = activeGame,
                                groupTag = activeGroup
                            )
                        )
                    }

                    AiBuildTargetKind.TEXTURE_MATERIAL -> {
                        val built = AiMultiAssetBuilder.buildTextureFromPrompt(
                            assetName = assetName,
                            subjectDescription = subject,
                            sizePx = 32,
                            sharedContextBlock = sharedContextBlock
                        )
                        val texTitle = built.project.name
                        val summary = "Built ${built.project.sizePx}×${built.project.sizePx} editable texture '$texTitle' (128×128 UV applied) via ${built.modelUsed} — open Texture Studio to paint or tweak."
                        _uiState.update { state ->
                            state.copy(
                                isCreatingFromScratchWithAi = false,
                                lastScratchAiError = null,
                                scratchTextureProject = built.project,
                                activeTexture = built.exportBitmap,
                                activeTextureName = texTitle,
                                hasImportedTexture = true,
                                lastAiBuildSummary = summary,
                                statusMessage = summary
                            )
                        }
                        registerAssetAndAutoFuseIfMatched(
                            StagedCharacterAsset(
                                id = "staged_aitex_${System.currentTimeMillis()}",
                                assetTitle = texTitle,
                                characterName = activeChar,
                                modality = CharacterAssetModality.TEXTURE_UV,
                                isUserImported = true,
                                bitmap = built.exportBitmap,
                                gameTag = activeGame,
                                groupTag = activeGroup
                            )
                        )
                    }

                    AiBuildTargetKind.AUDIO_CRY -> {
                        val built = AiMultiAssetBuilder.buildAudioCryFromPrompt(
                            assetName = assetName,
                            subjectDescription = subject,
                            sharedContextBlock = sharedContextBlock
                        )
                        val cryTitle = assetName.ifBlank {
                            built.spec.speciesKey.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        }
                        val slotKey = "cry_${built.spec.speciesKey}"
                        val entry = AudioAssetEntry(
                            id = "audio_aicry_${System.currentTimeMillis()}",
                            slotKey = slotKey,
                            title = cryTitle,
                            category = AudioUsageCategory.PROCEDURAL_CRY,
                            format = AudioAssetFormat.AUDIO_CUE_JSON,
                            sampleRateHz = built.sampleRateHz,
                            durationMs = built.spec.durationMs,
                            waveformPeaks = built.waveformPeaks,
                            filePath = "",
                            sha256Hex = "",
                            byteSize = built.rawJson.toByteArray().size.toLong(),
                            gameTag = activeGame,
                            groupTag = activeGroup,
                            characterTag = activeChar,
                            crySpec = built.spec
                        )
                        val dspProfile = _uiState.value.selectedSoundchipProfile
                        val dspPcm = AudioSynthAndDspEngine.applyHardwareSoundchipDsp(
                            built.pcm,
                            built.sampleRateHz,
                            dspProfile
                        )
                        AudioSynthAndDspEngine.playPcmSafely(dspPcm, built.sampleRateHz)

                        val summary = "Built procedural cry '$cryTitle' (${built.spec.baseFreqHz.roundToInt()}→${built.spec.endFreqHz.roundToInt()} Hz, ${built.spec.durationMs}ms) via ${built.modelUsed} and auditioned on ${dspProfile.displayName}."
                        _uiState.update { state ->
                            state.copy(
                                isCreatingFromScratchWithAi = false,
                                lastScratchAiError = null,
                                audioVaultEntries = listOf(entry) + state.audioVaultEntries,
                                selectedAudioEntryId = entry.id,
                                lastAiBuildSummary = summary,
                                statusMessage = summary
                            )
                        }
                    }

                    AiBuildTargetKind.AUDIO_CHIPTUNE -> {
                        val built = AiMultiAssetBuilder.buildChiptuneFromPrompt(
                            assetName = assetName,
                            subjectDescription = subject,
                            sharedContextBlock = sharedContextBlock
                        )
                        val trackTitle = built.spec.title
                        val slotSlug = trackTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "ai_chiptune" }
                        val durationMs = ((built.pcm.size.toDouble() / built.sampleRateHz.toDouble()) * 1000.0).roundToInt().coerceAtLeast(250)
                        val entry = AudioAssetEntry(
                            id = "audio_aichip_${System.currentTimeMillis()}",
                            slotKey = "bgm_$slotSlug",
                            title = trackTitle,
                            category = AudioUsageCategory.BGM_LOOP,
                            format = AudioAssetFormat.SCORE_SEQ_JSON,
                            sampleRateHz = built.sampleRateHz,
                            durationMs = durationMs,
                            waveformPeaks = built.waveformPeaks,
                            filePath = "",
                            sha256Hex = "",
                            byteSize = built.rawJson.toByteArray().size.toLong(),
                            gameTag = activeGame,
                            groupTag = activeGroup,
                            characterTag = activeChar,
                            scoreSpec = built.spec
                        )
                        val dspProfile = _uiState.value.selectedSoundchipProfile
                        val dspPcm = AudioSynthAndDspEngine.applyHardwareSoundchipDsp(
                            built.pcm,
                            built.sampleRateHz,
                            dspProfile
                        )
                        AudioSynthAndDspEngine.playPcmSafely(dspPcm, built.sampleRateHz)

                        val summary = "Composed 16-step chiptune '$trackTitle' (${built.spec.bpm} BPM, ${built.spec.notes.size} notes) via ${built.modelUsed} and auditioned on ${dspProfile.displayName}."
                        _uiState.update { state ->
                            state.copy(
                                isCreatingFromScratchWithAi = false,
                                lastScratchAiError = null,
                                audioVaultEntries = listOf(entry) + state.audioVaultEntries,
                                selectedAudioEntryId = entry.id,
                                lastAiBuildSummary = summary,
                                statusMessage = summary
                            )
                        }
                    }

                    AiBuildTargetKind.SPRITE_2D -> Unit
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI multi-asset build failed for ${target.name}", e)
                val msg = e.localizedMessage
                    ?: "AI ${target.label} build failed. Check your connected provider in Omni-AI Hub."
                _uiState.update { state ->
                    state.copy(
                        isCreatingFromScratchWithAi = false,
                        lastScratchAiError = msg,
                        statusMessage = msg
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Target FPS, Dual-Mode Forge AI Chat Director & Unified User Training Hub
    // -------------------------------------------------------------------------

    private val directorChatFirebaseModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    private fun buildCombinedSharedTrainingContext(
        forChat: Boolean,
        queryHint: String = _uiState.value.promptText,
        state: AppUiState = _uiState.value
    ): String {
        val registryBlock = OmniAiRouter.getContextRegistry().assembleContextBlock(
            queryHint = queryHint,
            activeProjectSummary = buildActiveProjectContextSummary(state)
        ).trim()
        val profile = state.userAiTrainingProfile
        val trainingEnabled = if (forChat) profile.applyToChatStudio else profile.applyToFormBuilder
        val trainingBlock = if (trainingEnabled) profile.buildUnifiedTrainingContextBlock() else ""

        return listOf(trainingBlock, registryBlock)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    fun updateTargetAssetFps(fps: Int) {
        val clamped = fps.coerceIn(1, 120)
        val nearestCadence = AiTrainingDatasetExporter.nearestRigCadence(clamped)
        _uiState.update { state ->
            state.copy(
                targetAssetFps = clamped,
                scratchSpriteProject = state.scratchSpriteProject.copy(fps = clamped.coerceIn(1, 60)),
                activeRigMetadata = state.activeRigMetadata.copy(cadenceFps = nearestCadence)
            )
        }
    }

    fun setForgeCreateUxMode(mode: Int) {
        _uiState.update { it.copy(forgeCreateUxMode = mode.coerceIn(0, 1)) }
    }

    fun sendForgeChatMessage(userText: String) {
        val cleanText = userText.trim()
        if (cleanText.isEmpty() || _uiState.value.isForgeChatSending) return

        val snapshot = _uiState.value
        val userMessage = ForgeChatMessage(
            id = "chat_u_${UUID.randomUUID().toString().take(8)}",
            role = ForgeChatRole.USER,
            text = cleanText
        )
        val historyBeforeSend = snapshot.forgeChatMessages
        _uiState.update { state ->
            state.copy(
                forgeChatMessages = state.forgeChatMessages + userMessage,
                isForgeChatSending = true
            )
        }

        val sharedRegistryBlock = OmniAiRouter.getContextRegistry().assembleContextBlock(
            queryHint = cleanText,
            activeProjectSummary = buildActiveProjectContextSummary(snapshot)
        )
        val conversationPrompt = ForgeChatPromptBuilder.buildConversationPrompt(
            history = historyBeforeSend,
            userMessage = cleanText,
            targetFps = snapshot.targetAssetFps,
            trainingProfile = snapshot.userAiTrainingProfile,
            sharedContextBlock = sharedRegistryBlock
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = OmniAiRouter.generateTextOrJson(
                    role = AiTaskRole.TEXT_AND_SPECS,
                    prompt = conversationPrompt,
                    systemInstruction = "You are PixelForge Game Asset Director. Give clear, concise creative and technical guidance. Always include a `BUILD_PROMPT:` line when proposing a buildable asset.",
                    imageBitmap = null,
                    expectJson = false,
                    sharedContextBlock = "",
                    firebaseDelegate = { effectivePrompt, _ ->
                        withTimeout(60_000L) {
                            val resp = directorChatFirebaseModel.generateContent(effectivePrompt)
                            resp.text?.trim()
                                ?: throw IllegalStateException("Empty reply from Firebase gemini-3.8-flash.")
                        }
                    }
                )

                val replyText = response.text.trim().ifBlank {
                    "I have refined your concept. BUILD_PROMPT: $cleanText"
                }
                val (suggestedTarget, extractedPrompt) = ForgeChatPromptBuilder.extractSuggestedTargetAndPrompt(
                    replyText = replyText,
                    fallbackUserPrompt = cleanText
                )
                val providerLabel = if (response.providerUsed == AiProviderType.FIREBASE_STUDIO) {
                    "Firebase (${response.modelIdUsed})"
                } else {
                    "${response.providerUsed.defaultDisplayName} (${response.modelIdUsed})"
                }

                val assistantMessage = ForgeChatMessage(
                    id = "chat_a_${UUID.randomUUID().toString().take(8)}",
                    role = ForgeChatRole.ASSISTANT,
                    text = replyText,
                    providerUsed = providerLabel,
                    extractedBuildPrompt = extractedPrompt,
                    suggestedBuildTarget = suggestedTarget
                )
                _uiState.update { state ->
                    state.copy(
                        forgeChatMessages = state.forgeChatMessages + assistantMessage,
                        isForgeChatSending = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Forge AI chat director request failed", e)
                val errText = e.localizedMessage?.take(220)
                    ?: "No connected AI provider responded. Tap Select / Connect AIs to configure a provider."
                val errorBubble = ForgeChatMessage(
                    id = "chat_err_${UUID.randomUUID().toString().take(8)}",
                    role = ForgeChatRole.ASSISTANT,
                    text = "AI Director could not reach your active provider: $errText",
                    providerUsed = "Error",
                    extractedBuildPrompt = cleanText,
                    suggestedBuildTarget = _uiState.value.selectedAiBuildTarget
                )
                _uiState.update { state ->
                    state.copy(
                        forgeChatMessages = state.forgeChatMessages + errorBubble,
                        isForgeChatSending = false,
                        statusMessage = "AI Chat error: $errText"
                    )
                }
            }
        }
    }

    fun buildAssetFromChatMessage(messageId: String, targetKind: AiBuildTargetKind) {
        val snapshot = _uiState.value
        val message = snapshot.forgeChatMessages.firstOrNull { it.id == messageId } ?: return
        val basePrompt = (message.extractedBuildPrompt?.takeIf { it.isNotBlank() } ?: message.text)
            .trim()
            .take(400)
        val effectivePrompt = if (snapshot.userAiTrainingProfile.applyToChatStudio) {
            snapshot.userAiTrainingProfile.applyTriggerToPrompt(basePrompt)
        } else {
            basePrompt
        }
        val inferredName = snapshot.scratchCreateAssetName.trim().ifBlank {
            basePrompt.split(Regex("\\s+")).take(3).joinToString(" ")
                .replace(Regex("[^A-Za-z0-9 _-]"), "")
                .trim()
                .ifBlank { "Chat ${targetKind.label}" }
        }
        _uiState.update { state ->
            state.copy(
                promptText = effectivePrompt,
                scratchCreateAssetName = inferredName,
                selectedAiBuildTarget = targetKind
            )
        }
        buildBrandNewAssetWithAi()
    }

    fun pinActiveAssetAsAiTrainingExample() {
        val state = _uiState.value
        val target = state.selectedAiBuildTarget
        val (title, specSummary) = when (target) {
            AiBuildTargetKind.MODEL_3D -> {
                val p3d = state.scratch3DProject
                val desc = if (p3d.parts.isNotEmpty()) {
                    "3D Kitbash (${p3d.parts.size} parts): " + p3d.parts.take(8).joinToString(", ") { "${it.name}[${it.shape.name}]" }
                } else {
                    "Mesh '${state.currentMesh.name}' (${state.currentMesh.vertexCount}v/${state.currentMesh.faceCount}f)"
                }
                p3d.name.ifBlank { state.currentMesh.name } to desc
            }
            AiBuildTargetKind.TEXTURE_MATERIAL -> {
                val tex = state.scratchTextureProject
                tex.name to "${tex.sizePx}x${tex.sizePx}px texture (mirrorX=${tex.mirrorX}, mirrorY=${tex.mirrorY})"
            }
            AiBuildTargetKind.AUDIO_CRY, AiBuildTargetKind.AUDIO_CHIPTUNE -> {
                val audio = state.audioVaultEntries.firstOrNull { it.id == state.selectedAudioEntryId }
                    ?: state.audioVaultEntries.firstOrNull()
                if (audio != null) {
                    audio.title to audio.toExportSpecJson()
                } else {
                    "Studio Audio Preset" to "Soundchip DSP: ${state.selectedSoundchipProfile.displayName}"
                }
            }
            AiBuildTargetKind.SPRITE_2D -> {
                val sheet = state.currentSpriteSheet
                sheet.name to "${sheet.frameWidth}x${sheet.frameHeight}px, ${sheet.columns}x${sheet.rows} grid (${sheet.frames.size} frames), ${state.targetAssetFps} FPS, tier=${state.promptHardwareTier.name}"
            }
        }

        val exampleText = AiTrainingDatasetExporter.buildFewShotTrainingExampleText(
            assetTitle = title,
            targetKind = target,
            promptUsed = state.promptText.ifBlank { title },
            targetFps = state.targetAssetFps,
            structuredRecipeOrMetadata = specSummary
        )
        OmniAiRouter.getContextRegistry().addPastedNoteSource(
            title = "Training Example: $title",
            content = exampleText,
            kind = KnowledgeSourceKind.PASTED_NOTES,
            originLabel = "Pinned Active Studio Asset (${target.label})"
        )
        refreshOmniAiHubState()
        _uiState.update {
            it.copy(statusMessage = "Pinned '$title' as a shared AI few-shot training example (synced across Form + Chat)!")
        }
    }

    fun pinChatMessageAsSharedTraining(messageId: String) {
        val state = _uiState.value
        val msg = state.forgeChatMessages.firstOrNull { it.id == messageId } ?: return
        val target = msg.suggestedBuildTarget ?: state.selectedAiBuildTarget
        val promptSnippet = msg.extractedBuildPrompt ?: msg.text.take(160)
        val exampleText = AiTrainingDatasetExporter.buildFewShotTrainingExampleText(
            assetTitle = "Director Note (${target.label})",
            targetKind = target,
            promptUsed = promptSnippet,
            targetFps = state.targetAssetFps,
            structuredRecipeOrMetadata = msg.text.take(1200)
        )
        OmniAiRouter.getContextRegistry().addPastedNoteSource(
            title = "Chat Training: ${promptSnippet.take(28)}",
            content = exampleText,
            kind = KnowledgeSourceKind.PASTED_NOTES,
            originLabel = "Pinned from AI Chat Studio"
        )
        refreshOmniAiHubState()
        _uiState.update {
            it.copy(statusMessage = "Pinned AI Chat direction to shared training sources (synced across Form + Chat)!")
        }
    }

    fun clearForgeChatHistory() {
        _uiState.update { state ->
            state.copy(
                forgeChatMessages = listOf(
                    ForgeChatMessage(
                        id = "welcome_${System.currentTimeMillis()}",
                        role = ForgeChatRole.ASSISTANT,
                        text = "Chat cleared. AI Asset Director is ready—describe your next sprite, 3D model, texture, cry, or chiptune at ${state.targetAssetFps} FPS.",
                        providerUsed = "Director"
                    )
                ),
                isForgeChatSending = false,
                statusMessage = "Cleared AI Chat Studio conversation."
            )
        }
    }

    fun updateUserAiTrainingProfile(
        context: Context,
        triggerToken: String,
        customLoraOrModelTag: String,
        trainingNotes: String,
        applyToForm: Boolean,
        applyToChat: Boolean
    ) {
        val current = _uiState.value.userAiTrainingProfile
        val updated = current.copy(
            styleTriggerToken = triggerToken.trim(),
            customLoraOrModelTag = customLoraOrModelTag.trim(),
            customTrainingNotes = trainingNotes.trim(),
            applyToFormBuilder = applyToForm,
            applyToChatStudio = applyToChat
        )
        UserAiTrainingProfileStore.save(context, updated)
        _uiState.update { state ->
            state.copy(
                userAiTrainingProfile = updated,
                statusMessage = "Saved Unified AI Training Profile (Form=${if (applyToForm) "ON" else "OFF"}, Chat=${if (applyToChat) "ON" else "OFF"})."
            )
        }
    }

    fun importUserTrainingFileToSharedContext(
        context: Context,
        uri: Uri,
        displayName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val maxBytes = 256 * 1024
                val rawText = context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(8192)
                    val out = ByteArrayOutputStream()
                    var total = 0
                    while (total < maxBytes) {
                        val toRead = (maxBytes - total).coerceAtMost(buffer.size)
                        val read = input.read(buffer, 0, toRead)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        total += read
                    }
                    String(out.toByteArray(), Charsets.UTF_8)
                }.orEmpty()

                val safeName = displayName.trim().ifBlank { "user_training_file.txt" }
                val parsedBlock = AiTrainingDatasetExporter.parseImportedUserTrainingFile(safeName, rawText)
                OmniAiRouter.getContextRegistry().addPastedNoteSource(
                    title = "User Training: $safeName",
                    content = parsedBlock,
                    kind = KnowledgeSourceKind.LOCAL_FILE,
                    originLabel = "Imported Training File ($safeName)"
                )

                val currentProfile = _uiState.value.userAiTrainingProfile
                val updatedProfile = currentProfile.copy(
                    attachedTrainingFileCount = currentProfile.attachedTrainingFileCount + 1
                )
                UserAiTrainingProfileStore.save(context, updatedProfile)

                _uiState.update { state ->
                    state.copy(
                        userAiTrainingProfile = updatedProfile,
                        knowledgeSources = OmniAiRouter.getContextRegistry().getSources(),
                        statusMessage = "Imported '$safeName' into shared AI training context (synced across Form + Chat)!"
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to import user training file '$displayName'", e)
                _uiState.update { state ->
                    state.copy(
                        statusMessage = "Training file import failed: ${e.localizedMessage ?: "Unsupported or unreadable file."}"
                    )
                }
            }
        }
    }

    fun prepareLoraTrainingManifestExport(triggerToken: String = "") {
        val state = _uiState.value
        val activeGallery = state.assetGalleries.firstOrNull { it.gallery.id == state.selectedGalleryId }
            ?: state.assetGalleries.firstOrNull()
            ?: AssetGalleryWithItems(
                gallery = AssetGalleryEntity(
                    id = 0L,
                    name = state.activeGameTagForImports.ifBlank { "Studio Pack" },
                    description = "Active Studio Export"
                ),
                items = listOf(
                    AssetGalleryItemEntity.fromStudioAssets(
                        galleryId = 0L,
                        title = state.currentSpriteSheet.name,
                        characterTag = state.activeCharacterTagForImports,
                        assetType = "SPRITE_2D",
                        sourceOrigin = "STUDIO",
                        hardwareTarget = state.selectedHardware.shortName,
                        metricsSummary = "${state.currentSpriteSheet.frames.size}f @ ${state.targetAssetFps} FPS",
                        spriteSheet = state.currentSpriteSheet,
                        gameTag = state.activeGameTagForImports,
                        groupTag = state.activeGroupTagForImports
                    )
                )
            )
        val effectiveTrigger = triggerToken.trim()
            .ifBlank { state.userAiTrainingProfile.styleTriggerToken.trim() }
            .ifBlank { "mpv_truefusion" }

        val manifestJson = AiTrainingDatasetExporter.buildLoraCaptionManifestJson(
            gallery = activeGallery,
            triggerToken = effectiveTrigger,
            targetFps = state.targetAssetFps,
            tier = state.promptHardwareTier
        )
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export LoRA Caption Manifest (.json — $effectiveTrigger)",
                exportDialogText = manifestJson,
                statusMessage = "Prepared LoRA caption manifest (${activeGallery.items.size} entries @ ${state.targetAssetFps} FPS)."
            )
        }
    }

    fun prepareJsonlFineTuneExport() {
        val state = _uiState.value
        val jsonl = AiTrainingDatasetExporter.buildJsonlFineTuneDataset(
            project3D = state.scratch3DProject,
            textureProject = state.scratchTextureProject,
            audioEntries = state.audioVaultEntries,
            targetFps = state.targetAssetFps
        )
        val lineCount = jsonl.lineSequence().count { it.isNotBlank() }
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export Fine-Tune Dataset (.jsonl — $lineCount samples)",
                exportDialogText = jsonl,
                statusMessage = "Prepared fine-tune JSONL dataset ($lineCount training lines @ ${state.targetAssetFps} FPS)."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        AudioSynthAndDspEngine.stopActiveTrack()
    }

    companion object {
        const val MPV_ASSET_INDEX_BASE_URL =
            "https://raw.githubusercontent.com/Summer-Warrior/Asset-Holding-Software/main/"

        val ECOSYSTEM_GAME_PRESETS: List<String> = listOf(
            "General Studio",
            "PokéFusion Studio",
            "MPV In-Universe MMO",
            "NexusTCG / TCG Studio",
            "Aegis Grid SRW",
            "Chronicle Nexus",
            "ChronoStage AI",
            "MPV In-Universe ROMs"
        )

        val ECOSYSTEM_GROUP_PRESETS: List<String> = listOf(
            "Core Roster",
            "Protagonists",
            "Bosses & Rivals",
            "Official Baked Species",
            "Infinite Fusions",
            "Unique Fakemon (F-Slots)",
            "Mechs & Vehicles",
            "Stages & Props",
            "TCG Cards",
            "Audio & SFX"
        )

        private fun sanitizeAssetSlug(raw: String, fallback: String): String {
            val slug = raw.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
            return slug.ifBlank { fallback }
        }

        /**
         * Serializes a gallery's curated assets into MPV Omni-Store's exact `asset-index.json` schema:
         * `{ "baseUrl": "https://raw.githubusercontent.com/Summer-Warrior/Asset-Holding-Software/main/", "assets": [ ... ] }`
         */
        fun buildMpvAssetIndexJson(
            galleryEntry: AssetGalleryWithItems,
            baseUrl: String = MPV_ASSET_INDEX_BASE_URL
        ): String {
            val gallerySlug = sanitizeAssetSlug(galleryEntry.gallery.name, "gallery_${galleryEntry.gallery.id}")
            val assetsArray = JSONArray()
            val usedKeys = mutableSetOf<String>()

            galleryEntry.items.forEachIndexed { idx, item ->
                val itemSlug = sanitizeAssetSlug(item.title, "asset_${idx + 1}")
                val baseKey = "${gallerySlug}/${itemSlug}"
                val uniqueKey = if (usedKeys.add(baseKey)) baseKey else "${baseKey}_${idx + 1}"

                val hasSprite = item.spritePngBytes != null && item.spritePngBytes.isNotEmpty()
                val hasMesh = !item.objMeshData.isNullOrBlank() || item.meshFilePath.isNotBlank()
                val primaryExt = if (hasSprite || item.assetType != "MODEL_3D") "png" else "obj"
                val primaryPath = "assets/$uniqueKey.$primaryExt"

                val assetObj = JSONObject().apply {
                    put("key", uniqueKey)
                    put("path", primaryPath)
                    put("title", item.title)
                    put("assetType", item.assetType)
                    put("gameTag", item.gameTag)
                    put("groupTag", item.groupTag)
                    put("characterTag", item.characterTag)
                    put("hardwareTarget", item.hardwareTarget)
                    put("gallery", galleryEntry.gallery.name)
                    if (hasMesh && primaryExt != "obj") {
                        put("meshPath", "assets/$uniqueKey.obj")
                    }
                }
                assetsArray.put(assetObj)
            }

            val root = JSONObject().apply {
                put("baseUrl", baseUrl)
                put("assets", assetsArray)
            }
            return root.toString(2)
        }
    }
}
