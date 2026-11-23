package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.hardware.ConsoleHardwareCatalog
import com.example.model.hardware.HardwareAssetSimulator
import com.example.model.hardware.HardwareReadoutGenerator
import com.example.model.model3d.ObjParser
import com.example.model.model3d.ProceduralMeshes
import com.example.model.sprite.SampleSpriteSheets
import com.example.model.suite.AssetSuiteSynthesizer
import com.example.model.suite.CharacterAssetFusionEngine
import com.example.model.suite.SuiteBuildConfig
import com.example.model.suite.SuiteSpriteStrategy
import com.example.model.texture.ProceduralTextures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PixelForge", appName)
  }

  @Test
  fun `test obj parser and exporter`() {
    val sampleObj = """
      v 0.0 1.0 0.0
      v -1.0 -1.0 1.0
      v 1.0 -1.0 1.0
      f 1 2 3
    """.trimIndent()
    val mesh = ObjParser.parse(sampleObj, "TestPyramid")
    assertEquals(3, mesh.vertexCount)
    assertEquals(1, mesh.faceCount)

    val exported = ObjParser.exportToObj(mesh)
    assertTrue(exported.contains("v "))
    assertTrue(exported.contains("f "))
  }

  @Test
  fun `test procedural meshes and sprite generation`() {
    val starfighter = ProceduralMeshes.createSciFiStarfighter()
    assertTrue(starfighter.vertexCount > 10)
    assertTrue(starfighter.faceCount > 10)

    val ninjaSheet = SampleSpriteSheets.createCyberNinjaSheet()
    assertNotNull(ninjaSheet)
    assertEquals(4, ninjaSheet.columns)
    assertEquals(3, ninjaSheet.rows)
    assertEquals(12, ninjaSheet.frames.size)
  }

  @Test
  fun `test console handheld and chip hardware simulator`() {
    val systems = ConsoleHardwareCatalog.allSystems
    assertTrue(systems.size >= 15)

    val mesh = ProceduralMeshes.createSciFiStarfighter()
    val spriteSheet = SampleSpriteSheets.createCyberNinjaSheet()
    val spriteFrame = spriteSheet.frames.first()

    for (spec in systems) {
      assertTrue(spec.chips.isNotEmpty())
      val meshReport = HardwareAssetSimulator.analyzeMeshOnHardware(mesh, spec)
      assertTrue(meshReport.vertexBufferBytes > 0)
      assertTrue(meshReport.siliconBottlenecks.isNotEmpty())

      val meshDossier = HardwareReadoutGenerator.generate3DMeshDossier(mesh, spec, meshReport)
      assertEquals(4, meshDossier.sections.size)
      meshDossier.sections.forEach { section ->
        assertTrue(section.everydayReaderText.isNotBlank())
        assertTrue(section.highEndTechnicalText.isNotBlank())
        assertTrue(section.keyMetrics.isNotEmpty())
      }
      assertTrue(meshDossier.hardwareRegisterHexDump.isNotBlank())

      val spriteReport = HardwareAssetSimulator.simulateSpriteOnHardware(spriteFrame, spec)
      assertNotNull(spriteReport.simulatedBitmap)
      assertTrue(spriteReport.totalChrTiles > 0)

      val spriteDossier = HardwareReadoutGenerator.generate2DSpriteDossier(spriteSheet, spec, spriteReport)
      assertEquals(4, spriteDossier.sections.size)
      spriteDossier.sections.forEach { section ->
        assertTrue(section.everydayReaderText.isNotBlank())
        assertTrue(section.highEndTechnicalText.isNotBlank())
        assertTrue(section.keyMetrics.isNotEmpty())
      }
      assertTrue(spriteDossier.hardwareRegisterHexDump.isNotBlank())
    }
  }

  @Test
  fun `test unified single asset suite synthesis and hardware allocation`() {
    val mesh = ProceduralMeshes.createCyberMech()
    val spriteSheet = SampleSpriteSheets.createCyberNinjaSheet()
    val texture = ProceduralTextures.createCyberGrid()
    val config = SuiteBuildConfig(
      suiteName = "Valkyrie-X Suite",
      spriteStrategy = SuiteSpriteStrategy.RENDER_TURNTABLE_SHEET_FROM_3D,
      unifyPaletteAcrossAssets = true
    )

    val suite = AssetSuiteSynthesizer.buildUnifiedSuite(
      config = config,
      activeMesh = mesh,
      activeSpriteSheet = spriteSheet,
      activeTexture = texture,
      activeTextureName = "Cyber Grid",
      activeConceptArt = null,
      promptText = "Armored valkyrie mecha with plasma wings",
      hasUserImportedMesh = true,
      hasUserImportedSprite = true,
      hasUserImportedTexture = true,
      hasUserImportedConcept = false,
      hasUserEditedPrompt = true
    )

    assertEquals("Valkyrie-X Suite", suite.name)
    assertTrue(suite.unifiedPalette.isNotEmpty())
    assertEquals(4, suite.spriteSheet.frames.size)
    assertTrue(suite.mesh3D.uvs.isNotEmpty())
    assertTrue(suite.buildNotes.isNotEmpty())

    val ps1 = ConsoleHardwareCatalog.getById("ps1")
    val allocation = AssetSuiteSynthesizer.analyzeSuiteOnHardware(suite, ps1)
    assertTrue(allocation.totalVramUsedBytes > 0)
    assertTrue(allocation.everydaySuiteReadout.isNotBlank())
    assertTrue(allocation.technicalSuiteReadout.isNotBlank())

    val manifest = AssetSuiteSynthesizer.exportSuiteBundleManifest(suite, ps1, allocation)
    assertTrue(manifest.contains("Valkyrie-X Suite"))
    assertTrue(manifest.contains("UNIFIED SUITE MASTER PALETTE"))
  }

  @Test
  fun `test automatic fusion of assets sharing named characters`() {
    val pool = CharacterAssetFusionEngine.createInitialCharacterAssetPool()
    val groups = CharacterAssetFusionEngine.clusterAssetsByNamedCharacter(pool)
    assertTrue(groups.size >= 3)

    val kaelenGroup = groups.firstOrNull { it.characterName == "Kaelen" }
    assertNotNull(kaelenGroup)
    assertTrue(kaelenGroup!!.hasMultipleAssetsToFuse)
    assertEquals(2, kaelenGroup.meshAssets.size)

    val fusedKaelenSuite = CharacterAssetFusionEngine.fuseNamedCharacterGroup(kaelenGroup, unifyPalette = true)
    assertTrue(fusedKaelenSuite.name.contains("Kaelen"))
    assertTrue(fusedKaelenSuite.mesh3D.vertexCount > ProceduralMeshes.createCyberMech().vertexCount)
    assertTrue(fusedKaelenSuite.spriteSheet.frames.size >= 12)
    assertTrue(fusedKaelenSuite.buildNotes.any { it.contains("NAMED-CHARACTER AUTO-FUSION") })
  }

  @Test
  fun `test inspect and save 3d model and sprite to library`() {
    val vm = MainViewModel()
    val initialCount = vm.uiState.value.savedLibraryAssets.size
    vm.saveInspectedAssetToLibrary(
      title = "Inspected Starfighter Rig",
      characterTag = "Starfighter NX-3",
      type = com.example.model.library.LibraryAssetType.UNIFIED_PAIR,
      yawDeg = 135f,
      pitchDeg = 30f,
      spriteRotDeg = 90f
    )
    val afterSave = vm.uiState.value.savedLibraryAssets
    assertEquals(initialCount + 1, afterSave.size)
    val savedItem = afterSave.first()
    assertEquals("Inspected Starfighter Rig", savedItem.title)
    assertEquals(135f, savedItem.inspectedYawDegrees, 0.01f)
    assertEquals(90f, savedItem.inspectedSpriteRotationDegrees, 0.01f)

    vm.loadLibraryAssetIntoStudio(savedItem)
    assertEquals("Starfighter NX-3", vm.uiState.value.activeCharacterTagForImports)

    vm.deleteLibraryAsset(savedItem.id)
    assertEquals(initialCount, vm.uiState.value.savedLibraryAssets.size)
  }

  @Test
  fun `test system comparison preview across hardware rivalries`() {
    val mesh = ProceduralMeshes.createSciFiStarfighter()
    val spriteFrame = SampleSpriteSheets.createCyberNinjaSheet().frames.first()

    com.example.ui.components.ICONIC_HARDWARE_MATCHUPS.forEach { preset ->
      val specA = ConsoleHardwareCatalog.getById(preset.systemAId)
      val specB = ConsoleHardwareCatalog.getById(preset.systemBId)
      assertNotNull(specA)
      assertNotNull(specB)

      val meshA = HardwareAssetSimulator.adaptMeshToHardware(mesh, specA)
      val meshB = HardwareAssetSimulator.adaptMeshToHardware(mesh, specB)
      assertEquals(mesh.vertexCount, meshA.vertexCount)
      assertEquals(mesh.vertexCount, meshB.vertexCount)

      val report3DA = HardwareAssetSimulator.analyzeMeshOnHardware(mesh, specA)
      val report3DB = HardwareAssetSimulator.analyzeMeshOnHardware(mesh, specB)
      assertTrue(report3DA.totalAssetRamBytes > 0)
      assertTrue(report3DB.totalAssetRamBytes > 0)

      val report2DA = HardwareAssetSimulator.simulateSpriteOnHardware(spriteFrame, specA)
      val report2DB = HardwareAssetSimulator.simulateSpriteOnHardware(spriteFrame, specB)
      assertNotNull(report2DA.simulatedBitmap)
      assertNotNull(report2DB.simulatedBitmap)
      assertTrue(report2DA.hardwarePaletteSwatches.isNotEmpty())
      assertTrue(report2DB.hardwarePaletteSwatches.isNotEmpty())
    }
  }

  @Test
  fun `test cross system chip fusion asset output and activation`() {
    val mesh = ProceduralMeshes.createCyberMech()
    val spriteSheet = SampleSpriteSheets.createCyberNinjaSheet()

    com.example.model.hardware.ChipFusionEngine.curatedFusionBlueprints.forEach { bp ->
      val host = ConsoleHardwareCatalog.getById(bp.hostSystemId)
      val donors = buildList {
        add(com.example.model.hardware.ChipFusionEngine.getDonorById(bp.primaryDonorChipId))
        if (bp.secondaryDonorChipId != null) {
          add(com.example.model.hardware.ChipFusionEngine.getDonorById(bp.secondaryDonorChipId!!))
        }
      }
      val report = com.example.model.hardware.ChipFusionEngine.evaluateAssetOnFusedSystem(
        mesh = mesh,
        spriteSheet = spriteSheet,
        hostSpec = host,
        donors = donors,
        interconnect = bp.interconnect
      )
      assertTrue(report.triBudgetGainPercent > 0)
      assertTrue(report.vramGainBytes > 0L)
      assertTrue(report.everydayFusionReadout.isNotBlank())
      assertTrue(report.highEndTechnicalFusionReadout.isNotBlank())
      assertTrue(report.busBridgeRegisterDump.contains("DONOR_1_MMIO_REG"))
      assertNotNull(report.fusedSpriteReport?.simulatedBitmap)
    }

    val vm = MainViewModel()
    val customFused = com.example.model.hardware.ChipFusionEngine.synthesizeFusedSystemSpec(
      hostSpec = ConsoleHardwareCatalog.getById("nes_2c02"),
      donors = listOf(com.example.model.hardware.ChipFusionEngine.getDonorById("donor_sega_svp")),
      interconnect = com.example.model.hardware.FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM
    )
    vm.activateFusedHardwareSpec(customFused)
    assertEquals(customFused.id, vm.uiState.value.selectedHardware.id)
    assertTrue(vm.uiState.value.hardwareCatalog.any { it.id == customFused.id })
  }

  @Test
  fun `test fan and homebrew chips catalog and preview data`() {
    val mesh = ProceduralMeshes.createSciFiStarfighter()
    val spriteSheet = SampleSpriteSheets.createCyberNinjaSheet()

    val fanProfiles = com.example.model.hardware.FanChipCatalog.allFanChipProfiles
    assertTrue(fanProfiles.size >= 6)

    fanProfiles.forEach { profile ->
      assertTrue(profile.communityCreators.isNotBlank())
      assertTrue(profile.signatureFeatures.size >= 4)
      assertTrue(profile.everydayExplanation.isNotBlank())
      assertTrue(profile.highEndTechnicalBreakdown.isNotBlank())

      val previewData = com.example.model.hardware.FanChipCatalog.evaluateFanChipPreviewData(
        mesh = mesh,
        spriteSheet = spriteSheet,
        profile = profile
      )
      assertTrue(previewData.fanEnhancedSpec.recommendedModelTriBudget > previewData.stockHostSpec.recommendedModelTriBudget)
      assertTrue(previewData.fanEnhancedSpec.vramBytes > previewData.stockHostSpec.vramBytes)
      assertNotNull(previewData.fanSpriteReport?.simulatedBitmap)
      assertTrue(previewData.registerStreamDump.contains("CREATORS"))
    }

    val fanSystemsInCatalog = ConsoleHardwareCatalog.allSystemsWithFusions().filter {
      it.category == com.example.model.hardware.HardwareCategory.FAN_CHIP
    }
    assertEquals(fanProfiles.size, fanSystemsInCatalog.size)
  }

  @Test
  fun `test unbounded file and photo import for models and spritesheets`() {
    // 1. Test 3D Model from File Stream (.OBJ, ASCII .STL, and .PLY)
    val asciiStlContent = """
      solid test_prism
        facet normal 0.0 0.0 1.0
          outer loop
            vertex 0.0 1.0 0.0
            vertex -1.0 -1.0 0.0
            vertex 1.0 -1.0 0.0
          endloop
        endfacet
      endsolid test_prism
    """.trimIndent()
    val stlModelResult = com.example.model.importer.UniversalAssetImporter.importModelFromStream(
      inputStream = java.io.ByteArrayInputStream(asciiStlContent.toByteArray(Charsets.UTF_8)),
      fileName = "mecha_shield.stl",
      sourceKind = com.example.model.importer.AssetImportSourceKind.FILE
    )
    assertNotNull(stlModelResult)
    assertEquals(3, stlModelResult!!.mesh.vertexCount)
    assertEquals(1, stlModelResult.mesh.faceCount)
    assertEquals("FILE", stlModelResult.metadata.sourceKind.badge)

    val plyContent = """
      ply
      format ascii 1.0
      element vertex 4
      property float x
      property float y
      property float z
      element face 2
      property list uchar int vertex_indices
      end_header
      0 0 0
      1 0 0
      1 1 0
      0 1 0
      3 0 1 2
      3 0 2 3
    """.trimIndent()
    val plyModelResult = com.example.model.importer.UniversalAssetImporter.importModelFromStream(
      inputStream = java.io.ByteArrayInputStream(plyContent.toByteArray(Charsets.UTF_8)),
      fileName = "quad_plate.ply",
      sourceKind = com.example.model.importer.AssetImportSourceKind.FILE
    )
    assertNotNull(plyModelResult)
    assertEquals(4, plyModelResult!!.mesh.vertexCount)
    assertEquals(2, plyModelResult.mesh.faceCount)

    // 2. Test 3D Model from Photo Stream (PNG image stream -> 3D Relief Mesh)
    val sampleBitmap = SampleSpriteSheets.createCyberNinjaSheet().sourceBitmap
    val pngOut = java.io.ByteArrayOutputStream()
    sampleBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, pngOut)
    val photoBytes = pngOut.toByteArray()

    val photoTo3DResult = com.example.model.importer.UniversalAssetImporter.importModelFromStream(
      inputStream = java.io.ByteArrayInputStream(photoBytes),
      fileName = "hero_photo.png",
      sourceKind = com.example.model.importer.AssetImportSourceKind.PHOTO,
      photoGridResolution = 20
    )
    assertNotNull(photoTo3DResult)
    assertTrue(photoTo3DResult!!.mesh.vertexCount > 0)
    assertEquals("PHOTO", photoTo3DResult.metadata.sourceKind.badge)
    assertNotNull(photoTo3DResult.previewThumbnail)

    // 3. Test 2D Spritesheet from Photo Stream AND from 3D File Stream
    val photoSpriteResult = com.example.model.importer.UniversalAssetImporter.importSpriteSheetFromStream(
      inputStream = java.io.ByteArrayInputStream(photoBytes),
      fileName = "ninja_atlas_photo.png",
      sourceKind = com.example.model.importer.AssetImportSourceKind.PHOTO,
      columns = 6,
      rows = 3
    )
    assertNotNull(photoSpriteResult)
    assertEquals(18, photoSpriteResult!!.spriteSheet.frames.size)
    assertEquals(sampleBitmap.width, photoSpriteResult.spriteSheet.originalWidth)
    assertEquals("PHOTO", photoSpriteResult.metadata.sourceKind.badge)

    val modelFileToSpriteResult = com.example.model.importer.UniversalAssetImporter.importSpriteSheetFromStream(
      inputStream = java.io.ByteArrayInputStream(asciiStlContent.toByteArray(Charsets.UTF_8)),
      fileName = "mecha_shield.stl",
      sourceKind = com.example.model.importer.AssetImportSourceKind.FILE,
      columns = 4,
      rows = 2
    )
    assertNotNull(modelFileToSpriteResult)
    assertEquals(8, modelFileToSpriteResult!!.spriteSheet.frames.size)
    assertEquals("FILE", modelFileToSpriteResult.metadata.sourceKind.badge)
  }

  @Test
  fun `test gemini asset enhancement and multi variant generation`() {
    val baseMesh = ProceduralMeshes.createCyberMech()
    val baseSheet = SampleSpriteSheets.createCyberNinjaSheet()
    val inputImage = baseSheet.frames.first()

    val mockStructuredGeminiJson = """
      {
        "enhancedTitle": "Kaelen Apex Cyber-Shinobi",
        "characterIdentity": "Kaelen",
        "technicalEnhancementSummary": "Upgraded silhouette with swept plasma stabilizer wings, orbital energy ring, and high-contrast neon palette.",
        "enhancedLoreSpec": "Awakened orbital assault vanguard configured for high-speed plasma strikes.",
        "recommendedArchetype": "CYBER_MECH",
        "geometryScaleX": 1.15,
        "geometryScaleY": 1.08,
        "geometryScaleZ": 1.12,
        "extrusionDepthBoost": 0.75,
        "addWingsOrFins": true,
        "addOrbitalRing": true,
        "paletteHexColors": ["#00E5FF", "#1E293B", "#FF2A85", "#F59E0B", "#10B981", "#38BDF8"],
        "variants": [
          {
            "variantName": "Kaelen • Overclocked Plasma Boss",
            "variantBadge": "BOSS FORM",
            "technicalBreakdown": "Expanded X/Z silhouette with dual swept wings and orbital ring.",
            "loreDescription": "Overclocked reactor boss phase.",
            "archetype": "CYBER_MECH",
            "primaryHex": "#FF2A85",
            "secondaryHex": "#0F172A",
            "accentHex": "#00E5FF",
            "scaleX": 1.25,
            "scaleY": 1.15,
            "scaleZ": 1.20,
            "addWingsOrFins": true,
            "addOrbitalRing": true
          },
          {
            "variantName": "Kaelen • Cryo-Stealth Infiltrator",
            "variantBadge": "STEALTH",
            "technicalBreakdown": "Streamlined narrow silhouette with low-visibility cryo-cyan highlights.",
            "loreDescription": "Zero-signature infiltration variant.",
            "archetype": "STARFIGHTER",
            "primaryHex": "#38BDF8",
            "secondaryHex": "#090D16",
            "accentHex": "#10B981",
            "scaleX": 0.85,
            "scaleY": 1.05,
            "scaleZ": 1.10,
            "addWingsOrFins": true,
            "addOrbitalRing": false
          }
        ]
      }
    """.trimIndent()

    val report = com.example.ai.GeminiAssetForge.parseStructuredForgeResponse(
      rawJson = mockStructuredGeminiJson,
      operationMode = com.example.ai.GeminiOperationMode.GENERATE_VARIANTS,
      inputBitmap = inputImage,
      baseMesh = baseMesh,
      baseSpriteSheet = baseSheet,
      fallbackCharacterTag = "Kaelen"
    )

    assertEquals("Kaelen Apex Cyber-Shinobi", report.enhancedTitle)
    assertEquals("Kaelen", report.characterIdentity)
    assertTrue(report.primaryMesh.vertexCount > baseMesh.vertexCount)
    assertEquals(8, report.primarySpriteSheet.frames.size)
    assertEquals(6, report.enhancedPalette.size)
    assertEquals(2, report.variants.size)
    assertTrue(report.usedImageInput)

    val firstVariant = report.variants.first()
    assertEquals("BOSS FORM", firstVariant.variantBadge)
    assertTrue(firstVariant.mesh.vertexCount > baseMesh.vertexCount)
    assertEquals(8, firstVariant.spriteSheet.frames.size)

    val vm = MainViewModel()
    val libCountBefore = vm.uiState.value.savedLibraryAssets.size
    vm.applyGeminiVariantToStudio(firstVariant)
    assertEquals(firstVariant.mesh.name, vm.uiState.value.currentMesh.name)
    assertEquals(firstVariant.spriteSheet.name, vm.uiState.value.currentSpriteSheet.name)

    vm.saveGeminiVariantToLibrary(firstVariant)
    assertEquals(libCountBefore + 1, vm.uiState.value.savedLibraryAssets.size)
    assertEquals("GEMINI VARIANT", vm.uiState.value.savedLibraryAssets.first().sourceBadge)
  }

  @Test
  fun `test fusions of 3d models and spritesheets to make assets`() {
    val mesh = ProceduralMeshes.createSciFiStarfighter()
    val spriteSheet = SampleSpriteSheets.createCyberNinjaSheet()

    val allFusions = com.example.model.fusion.ModelSpriteFusionEngine.fuseAllModesForComparison(
      mesh = mesh,
      spriteSheet = spriteSheet,
      blendRatio = 0.60f,
      reliefDepth = 0.55f,
      harmonizePalette = true,
      characterTag = "Kaelen"
    )
    assertEquals(com.example.model.fusion.ModelSpriteFusionMode.entries.size, allFusions.size)

    allFusions.forEach { fusion ->
      assertTrue(fusion.fusedMesh.vertexCount >= mesh.vertexCount)
      assertTrue(fusion.fusedMesh.faceCount >= mesh.faceCount)
      assertTrue(fusion.fusedMesh.uvs.isNotEmpty())
      assertEquals(spriteSheet.frames.size, fusion.fusedSpriteSheet.frames.size)
      assertNotNull(fusion.fusedTextureAtlas)
      assertTrue(fusion.unifiedPalette.isNotEmpty())
      assertTrue(fusion.everydayReadout.isNotBlank())
      assertTrue(fusion.technicalReadout.isNotBlank())
    }

    val reliefGraft = allFusions.first {
      it.fusionMode == com.example.model.fusion.ModelSpriteFusionMode.SPRITE_ARMOR_RELIEF_GRAFT
    }
    assertTrue(reliefGraft.fusedMesh.vertexCount > mesh.vertexCount)

    val vm = MainViewModel()
    assertNotNull(vm.uiState.value.activeModelSpriteFusion)
    vm.applyModelSpriteFusionResult(reliefGraft)
    assertEquals(reliefGraft.fusedMesh.name, vm.uiState.value.currentMesh.name)
    assertEquals(reliefGraft.fusedSpriteSheet.name, vm.uiState.value.currentSpriteSheet.name)
  }

  @Test
  fun `test room database entity and dao for generated models and spritesheets`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.PixelForgeDatabase.createInMemoryDatabase(context)
    val dao = db.assetHistoryDao()
    val repo = com.example.data.local.AssetHistoryRepository(dao)

    val mesh = ProceduralMeshes.createSciFiStarfighter()
    val spriteSheet = SampleSpriteSheets.createCyberNinjaSheet()

    val modelEntity = com.example.data.local.AssetHistoryEntity.fromStudioAssets(
      title = "Kaelen Starfighter 3D",
      characterTag = "Kaelen",
      assetType = "MODEL_3D",
      sourceOrigin = "PROCEDURAL FORGE",
      hardwareTarget = "PS1",
      metricsSummary = "${mesh.vertexCount}v/${mesh.faceCount}f",
      mesh = mesh,
      createdAtMillis = 1000L
    )

    val spriteEntity = com.example.data.local.AssetHistoryEntity.fromStudioAssets(
      title = "Kaelen Cyber Ninja Sheet",
      characterTag = "Kaelen",
      assetType = "SPRITE_2D",
      sourceOrigin = "GEMINI AI",
      hardwareTarget = "SNES",
      metricsSummary = "${spriteSheet.frames.size} frames (4x3)",
      spriteSheet = spriteSheet,
      paletteHexList = listOf("#00E5FF", "#FF2A85"),
      aiReviewNotes = "Verified 4x3 Multi-Frame Spritesheet",
      createdAtMillis = 2000L
    )

    val modelId = repo.recordGeneratedAsset(modelEntity)
    val spriteId = repo.recordGeneratedAsset(spriteEntity)
    assertTrue(modelId > 0L)
    assertTrue(spriteId > 0L)
    assertEquals(2, repo.getHistoryCount())

    val allHistory = repo.getAllHistorySnapshot()
    assertEquals(2, allHistory.size)
    // Ordered by createdAtMillis DESC so spriteEntity (2000L) is first
    assertEquals("Kaelen Cyber Ninja Sheet", allHistory[0].title)
    assertEquals("Kaelen Starfighter 3D", allHistory[1].title)

    // Reconstruct 2D Spritesheet & 3D Mesh from Room entities
    val restoredSprite = allHistory[0].toSpriteSheetData()
    assertNotNull(restoredSprite)
    assertEquals(4, restoredSprite!!.columns)
    assertEquals(3, restoredSprite.rows)
    assertEquals(12, restoredSprite.frames.size)

    val restoredMesh = allHistory[1].toMesh3D()
    assertNotNull(restoredMesh)
    assertEquals(mesh.vertexCount, restoredMesh!!.vertexCount)
    assertEquals(mesh.faceCount, restoredMesh.faceCount)

    // Delete single record & clear
    repo.deleteHistoryAsset(modelId)
    assertEquals(1, repo.getHistoryCount())
    repo.clearAllHistory()
    assertEquals(0, repo.getHistoryCount())
    db.close()
  }

  @Test
  fun `test ai file import review detects spritesheet added as single picture image`() {
    val multiFrameSheetBitmap = SampleSpriteSheets.createCyberNinjaSheet().sourceBitmap

    // Case 1: User imports a multi-frame spritesheet (4x3) as a 1x1 single picture image
    val singlePictureReview = com.example.ai.AiImportSpriteSheetReviewer.analyzeSpriteSheetInSingleImageDeterministically(
      bitmap = multiFrameSheetBitmap,
      fileName = "cyber_ninja_walk_sheet_4x3.png",
      userRequestedColumns = 1,
      userRequestedRows = 1
    )
    assertTrue(singlePictureReview.isSpriteSheetInSingleImage)
    assertEquals(4, singlePictureReview.recommendedColumns)
    assertEquals(3, singlePictureReview.recommendedRows)
    assertEquals(12, singlePictureReview.detectedFrameCount)
    assertTrue(singlePictureReview.verdictTitle.contains("Spritesheet Detected"))
    assertTrue(singlePictureReview.aiReviewSummary.contains("1×1 single picture image"))

    // Case 2: User imports a horizontal 4-frame strip without explicit dimensions in filename as 1x1
    val stripSheet = com.example.model.sprite.SpriteSheetData.fromBitmap(
      bitmap = multiFrameSheetBitmap,
      name = "Hero Strip",
      columns = 4,
      rows = 3
    )
    val stripReview = com.example.ai.AiImportSpriteSheetReviewer.analyzeSpriteSheetInSingleImageDeterministically(
      bitmap = stripSheet.sourceBitmap,
      fileName = "hero_animation_strip.png",
      userRequestedColumns = 1,
      userRequestedRows = 1
    )
    assertTrue(stripReview.isSpriteSheetInSingleImage)
    assertTrue(stripReview.detectedFrameCount > 1)

    // Case 3: Solid single-frame portrait image imported as 1x1 is confirmed as a standalone picture
    val singlePortrait = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
    singlePortrait.eraseColor(android.graphics.Color.rgb(30, 140, 220))
    val standaloneReview = com.example.ai.AiImportSpriteSheetReviewer.analyzeSpriteSheetInSingleImageDeterministically(
      bitmap = singlePortrait,
      fileName = "portrait_avatar.png",
      userRequestedColumns = 1,
      userRequestedRows = 1
    )
    assertEquals(false, standaloneReview.isSpriteSheetInSingleImage)
    assertEquals(1, standaloneReview.recommendedColumns)
    assertEquals(1, standaloneReview.recommendedRows)
  }

  @Test
  fun `test asset galleries room dao repository and viewmodel workflow`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.PixelForgeDatabase.createInMemoryDatabase(context)
    val galleryRepo = com.example.data.local.AssetGalleryRepository(db.assetGalleryDao())

    // 1. Create a new gallery in Room
    val createdId = galleryRepo.createGallery(
      com.example.data.local.AssetGalleryEntity(
        name = "Cyber Boss Showcase",
        description = "Curated boss 3D meshes and plasma spritesheets",
        themeTag = "CYBERPUNK",
        accentColorHex = "#00E5FF",
        layoutStyle = "CAROUSEL",
        createdAtMillis = 5000L
      )
    )
    assertTrue(createdId > 0L)
    assertEquals(1, galleryRepo.getGalleryCount())

    // 2. Insert a 3D Model item and a 2D Spritesheet item into the gallery
    val mesh = ProceduralMeshes.createCyberMech()
    val spriteSheet = SampleSpriteSheets.createCyberNinjaSheet()

    val item1Id = galleryRepo.addItemToGallery(
      com.example.data.local.AssetGalleryItemEntity.fromStudioAssets(
        galleryId = createdId,
        title = "Boss Mech Frame",
        characterTag = "Kaelen",
        assetType = "MODEL_3D",
        sourceOrigin = "STUDIO CURATION",
        hardwareTarget = "PS1",
        metricsSummary = "${mesh.vertexCount}v/${mesh.faceCount}f",
        curatorNote = "Hero boss silhouette",
        mesh = mesh,
        addedAtMillis = 6000L
      )
    )
    val item2Id = galleryRepo.addItemToGallery(
      com.example.data.local.AssetGalleryItemEntity.fromStudioAssets(
        galleryId = createdId,
        title = "Boss Ninja Sprites",
        characterTag = "Kaelen",
        assetType = "SPRITE_2D",
        sourceOrigin = "STUDIO CURATION",
        hardwareTarget = "SNES",
        metricsSummary = "${spriteSheet.frames.size} frames",
        spriteSheet = spriteSheet,
        addedAtMillis = 7000L
      )
    )
    assertTrue(item1Id > 0L)
    assertTrue(item2Id > 0L)

    // 3. Verify snapshot returns gallery + both reconstructable items
    val snapshot = galleryRepo.getAllGalleriesWithItemsSnapshot()
    assertEquals(1, snapshot.size)
    assertEquals("Cyber Boss Showcase", snapshot.first().gallery.name)
    assertEquals(2, snapshot.first().items.size)

    val restoredMesh = snapshot.first().items.first { it.assetType == "MODEL_3D" }.toMesh3D()
    assertNotNull(restoredMesh)
    assertEquals(mesh.vertexCount, restoredMesh!!.vertexCount)

    val restoredSprite = snapshot.first().items.first { it.assetType == "SPRITE_2D" }.toSpriteSheetData()
    assertNotNull(restoredSprite)
    assertEquals(spriteSheet.frames.size, restoredSprite!!.frames.size)

    // 4. Delete one item, then cascade delete gallery
    galleryRepo.removeItemFromGallery(item1Id)
    assertEquals(1, galleryRepo.getAllGalleriesWithItemsSnapshot().first().items.size)
    galleryRepo.deleteGallery(createdId)
    assertEquals(0, galleryRepo.getGalleryCount())
    db.close()

    // 5. Verify MainViewModel createNewAssetGallery + addActiveStudioAssetToGallery + manifest export
    val vm = MainViewModel()
    val initialGalleryCount = vm.uiState.value.assetGalleries.size
    assertTrue(initialGalleryCount >= 2)

    vm.createNewAssetGallery(
      name = "Mecha Armada Exhibition",
      description = "All orbital interceptors and fused armor rigs",
      themeTag = "3D + 2D FUSIONS",
      accentColorHex = "#10B981",
      layoutStyle = "COMPACT_LIST",
      seedWithCurrentStudioAsset = true
    )

    val galleriesAfterCreate = vm.uiState.value.assetGalleries
    assertEquals(initialGalleryCount + 1, galleriesAfterCreate.size)
    val createdEntry = galleriesAfterCreate.first { it.gallery.name == "Mecha Armada Exhibition" }
    assertEquals(1, createdEntry.items.size)
    assertEquals("COMPACT_LIST", createdEntry.gallery.layoutStyle)

    vm.addActiveStudioAssetToGallery(createdEntry.gallery.id, "MODEL_3D", curatorNote = "Added directly")
    val updatedEntry = vm.uiState.value.assetGalleries.first { it.gallery.id == createdEntry.gallery.id }
    assertEquals(2, updatedEntry.items.size)

    vm.prepareGalleryManifestExport(createdEntry.gallery.id)
    val exportText = vm.uiState.value.exportDialogText
    assertNotNull(exportText)
    assertTrue(exportText!!.contains("Mecha Armada Exhibition"))
    assertTrue(exportText.contains("Curated Items: 2"))
  }
}
