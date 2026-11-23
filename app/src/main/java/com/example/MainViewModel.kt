package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiImportSpriteSheetReviewer
import com.example.ai.AiSpriteSheetReviewResult
import com.example.ai.ArtTheme
import com.example.ai.AssetTarget
import com.example.ai.GeminiAssetForge
import com.example.ai.GeminiAssetVariant
import com.example.ai.GeminiForgeReport
import com.example.ai.GeminiForgeUiState
import com.example.ai.GeminiInputImageSource
import com.example.ai.GeminiOperationMode
import com.example.data.local.AssetGalleryEntity
import com.example.data.local.AssetGalleryItemEntity
import com.example.data.local.AssetGalleryRepository
import com.example.data.local.AssetGalleryWithItems
import com.example.data.local.AssetHistoryEntity
import com.example.data.local.AssetHistoryRepository
import com.example.data.local.PixelForgeDatabase
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
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ObjParser
import com.example.model.model3d.ProceduralMeshes
import com.example.model.sprite.SampleSpriteSheets
import com.example.model.sprite.SpriteSheetData
import com.example.model.suite.AssetSuiteSynthesizer
import com.example.model.suite.CharacterAssetFusionEngine
import com.example.model.suite.CharacterAssetModality
import com.example.model.suite.NamedCharacterFusionGroup
import com.example.model.suite.StagedCharacterAsset
import com.example.model.suite.SuiteBuildConfig
import com.example.model.suite.UnifiedAssetSuite
import com.example.model.texture.ProceduralTextures
import com.example.model.texture.TexturePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    // Named-Character Automatic Asset Fusion state
    val autoFuseNamedCharacters: Boolean = true,
    val activeCharacterTagForImports: String = "Kaelen",
    val stagedCharacterPool: List<StagedCharacterAsset> = emptyList(),
    val characterFusionGroups: List<NamedCharacterFusionGroup> = emptyList(),
    val lastAutoFusionBanner: String? = null,
    // Personal Saved Asset Library (Inspected 3D Models, Sprites, and Pairs)
    val savedLibraryAssets: List<SavedLibraryAsset> = emptyList(),
    // Unbounded File & Photo Import Metadata Telemetry
    val lastModelImportMeta: ImportedAssetMetadata? = null,
    val lastSpriteImportMeta: ImportedAssetMetadata? = null,
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
    val selectedGalleryId: Long? = null
)

class MainViewModel(
    private var historyRepository: AssetHistoryRepository? = null,
    private var galleryRepository: AssetGalleryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var historyCollectionStarted = false
    private var galleryCollectionStarted = false

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
        val db = PixelForgeDatabase.getDatabase(context.applicationContext)
        attachHistoryRepository(AssetHistoryRepository(db.assetHistoryDao()))
        attachGalleryRepository(AssetGalleryRepository(db.assetGalleryDao()))
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
        aiReviewNotes: String? = null
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
                aiReviewNotes = aiReviewNotes
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
            val restoredMesh = entity.toMesh3D()
            val restoredSprite = entity.toSpriteSheetData()
            _uiState.update { state ->
                state.copy(
                    currentMesh = restoredMesh ?: state.currentMesh,
                    currentSpriteSheet = restoredSprite ?: state.currentSpriteSheet,
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
            metricsSummary = metrics
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
            spriteSheet = entry.spriteSheet
        )
    }

    fun loadLibraryAssetIntoStudio(item: SavedLibraryAsset) {
        _uiState.update { state ->
            state.copy(
                currentMesh = item.mesh ?: state.currentMesh,
                currentSpriteSheet = item.spriteSheet ?: state.currentSpriteSheet,
                activeTexture = item.textureBitmap ?: state.activeTexture,
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
            val staged = when (modality) {
                CharacterAssetModality.MESH_3D -> StagedCharacterAsset(
                    id = "staged_mesh_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - ${snapshot.currentMesh.name}",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.MESH_3D,
                    isUserImported = true,
                    mesh = snapshot.currentMesh
                )
                CharacterAssetModality.SPRITESHEET_2D -> StagedCharacterAsset(
                    id = "staged_sprite_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - ${snapshot.currentSpriteSheet.name}",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.SPRITESHEET_2D,
                    isUserImported = true,
                    spriteSheet = snapshot.currentSpriteSheet
                )
                CharacterAssetModality.TEXTURE_UV -> StagedCharacterAsset(
                    id = "staged_tex_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - ${snapshot.activeTextureName}",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.TEXTURE_UV,
                    isUserImported = true,
                    bitmap = snapshot.activeTexture
                )
                CharacterAssetModality.CONCEPT_ART -> StagedCharacterAsset(
                    id = "staged_concept_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - Concept Reference",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.CONCEPT_ART,
                    isUserImported = true,
                    bitmap = snapshot.conceptArtBitmap ?: snapshot.activeAssetSuite?.conceptArtBitmap
                )
                CharacterAssetModality.LORE_SPEC -> StagedCharacterAsset(
                    id = "staged_lore_${System.currentTimeMillis()}",
                    assetTitle = "${resolvedChar} - Lore Spec",
                    characterName = resolvedChar,
                    modality = CharacterAssetModality.LORE_SPEC,
                    isUserImported = true,
                    loreText = snapshot.promptText
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
                val libEntry = SavedLibraryAsset(
                    id = "lib_mesh_${System.currentTimeMillis()}",
                    title = titledName,
                    characterTag = resolvedCharacter,
                    type = LibraryAssetType.MODEL_3D,
                    mesh = mesh,
                    hardwareBadge = snapshot.selectedHardware.shortName,
                    metricsSummary = "${mesh.rawVertexCount}v/${mesh.rawFaceCount}f • ${mesh.sourceFormat}",
                    sourceBadge = "FILE",
                    formatSummary = sizeLabel
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
                        mesh = mesh
                    )
                )
                recordAssetInRoomHistory(
                    title = titledName,
                    characterTag = resolvedCharacter,
                    assetType = "MODEL_3D",
                    sourceOrigin = "FILE IMPORT",
                    hardwareTarget = snapshot.selectedHardware.shortName,
                    metricsSummary = "${mesh.vertexCount}v/${mesh.faceCount}f • $sizeLabel",
                    mesh = mesh
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
                formatSummary = "${result.metadata.formattedSize} • ${result.metadata.readabilityStatus}"
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
                    bitmap = result.previewThumbnail
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
                aiReviewNotes = aiReview?.verdictTitle
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
            val libEntry = SavedLibraryAsset(
                id = "lib_sprite_${System.currentTimeMillis()}",
                title = titledName,
                characterTag = resolvedCharacter,
                type = LibraryAssetType.SPRITE_2D,
                spriteSheet = sheet,
                hardwareBadge = snapshot.selectedHardware.shortName,
                metricsSummary = "${sheet.frames.size}f (${sheet.frameWidth}×${sheet.frameHeight}px) • ${origW}×${origH}px",
                sourceBadge = sourceKind.badge,
                formatSummary = "${sizeLabel} • ${meta.readabilityStatus}"
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
                    spriteSheet = sheet
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
                aiReviewNotes = aiReview.verdictTitle
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
                formatSummary = "${result.metadata.formattedSize} • ${result.metadata.readabilityStatus}"
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
                    spriteSheet = updatedSheet
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
                aiReviewNotes = aiReview.verdictTitle
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
        val review = AiImportSpriteSheetReviewer.reviewImportedFile(
            bitmap = bitmap,
            fileName = fileName,
            userRequestedColumns = requestedCols,
            userRequestedRows = requestedRows,
            preferCloudVision = preferCloudVision
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
                val report = if (mode == GeminiOperationMode.AI_IMAGE_SYNTHESIS) {
                    GeminiAssetForge.synthesizeAiVariantImageWithGemini(
                        promptDescription = snapshot.promptText,
                        customInstruction = snapshot.geminiCustomInstruction,
                        inputBitmap = inputBitmap,
                        inputSource = source,
                        theme = snapshot.selectedTheme,
                        hardwareName = snapshot.selectedHardware.name,
                        explicitCharacterTag = snapshot.activeCharacterTagForImports
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
                        explicitCharacterTag = snapshot.activeCharacterTagForImports
                    )
                }

                val libEntry = SavedLibraryAsset(
                    id = "lib_gemini_${System.currentTimeMillis()}",
                    title = report.enhancedTitle,
                    characterTag = report.characterIdentity,
                    type = LibraryAssetType.UNIFIED_PAIR,
                    mesh = report.primaryMesh,
                    spriteSheet = report.primarySpriteSheet,
                    textureBitmap = report.aiSynthesizedBitmap ?: snapshot.activeTexture,
                    hardwareBadge = snapshot.selectedHardware.shortName,
                    metricsSummary = "${report.primaryMesh.vertexCount}v/${report.primaryMesh.faceCount}f • ${report.variants.size} AI Variants",
                    sourceBadge = "GEMINI AI",
                    formatSummary = report.modelNameUsed
                )

                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        geminiForgeState = GeminiForgeUiState.Success(report),
                        geminiGeneratedVariants = report.variants,
                        lastGeminiReport = report,
                        currentMesh = report.primaryMesh,
                        currentSpriteSheet = report.primarySpriteSheet,
                        conceptArtBitmap = report.aiSynthesizedBitmap ?: state.conceptArtBitmap,
                        extractedPalette = report.enhancedPalette.ifEmpty { state.extractedPalette },
                        activeCharacterTagForImports = report.characterIdentity,
                        savedLibraryAssets = listOf(libEntry) + state.savedLibraryAssets,
                        statusMessage = "Gemini (${report.modelNameUsed}): Enhanced '${report.enhancedTitle}' & generated ${report.variants.size} variants!"
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
                    metricsSummary = "${report.primaryMesh.vertexCount}v/${report.primaryMesh.faceCount}f • ${report.variants.size} AI Variants",
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
            spriteSheet = spriteToSave
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
            spriteSheet = libraryItem.spriteSheet
        )
        insertItemIntoGalleryInternal(item, galleryName)
    }

    fun addHistoryEntityToGallery(
        galleryId: Long,
        historyEntity: AssetHistoryEntity,
        curatorNote: String = ""
    ) {
        val galleryName = _uiState.value.assetGalleries.firstOrNull { it.gallery.id == galleryId }?.gallery?.name ?: "Gallery"
        val item = AssetGalleryItemEntity(
            galleryId = galleryId,
            title = historyEntity.title,
            characterTag = historyEntity.characterTag,
            assetType = historyEntity.assetType,
            sourceOrigin = historyEntity.sourceOrigin,
            hardwareTarget = historyEntity.hardwareTarget,
            metricsSummary = historyEntity.metricsSummary,
            curatorNote = curatorNote.ifBlank { "Imported from Room Asset History Vault" },
            objMeshData = historyEntity.objMeshData,
            spritePngBytes = historyEntity.spritePngBytes,
            spriteColumns = historyEntity.spriteColumns,
            spriteRows = historyEntity.spriteRows
        )
        insertItemIntoGalleryInternal(item, galleryName)
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
            val restoredMesh = item.toMesh3D()
            val restoredSprite = item.toSpriteSheetData()
            _uiState.update { state ->
                state.copy(
                    currentMesh = restoredMesh ?: state.currentMesh,
                    currentSpriteSheet = restoredSprite ?: state.currentSpriteSheet,
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

    fun dismissExportDialog() {
        _uiState.update { it.copy(exportDialogText = null) }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
