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
import org.junit.Assert.assertFalse
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

  @Test
  fun `test room migration 2 to 3 and large obj mesh disk offloading`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val meshesDir = com.example.data.local.MeshDiskStorage.getMeshesDir(context)

    // 1. Canonical path validation rejects traversal outside meshesDir
    val validFile = java.io.File(meshesDir, "42.obj")
    val traversalFile = java.io.File(meshesDir, "../escape.obj")
    assertTrue(com.example.data.local.MeshDiskStorage.isCanonicalMeshPathValid(meshesDir, validFile))
    assertEquals(false, com.example.data.local.MeshDiskStorage.isCanonicalMeshPathValid(meshesDir, traversalFile))

    // 2. Verify MIGRATION_2_3 upgrades a v2 SQLite schema cleanly without dropping existing user data
    val migratedDbName = "pixelforge_migration_test_${System.currentTimeMillis()}.db"
    val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
      context.getDatabasePath(migratedDbName),
      null
    )
    sqliteDb.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `asset_history` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `title` TEXT NOT NULL,
        `characterTag` TEXT NOT NULL,
        `assetType` TEXT NOT NULL,
        `sourceOrigin` TEXT NOT NULL,
        `hardwareTarget` TEXT NOT NULL,
        `metricsSummary` TEXT NOT NULL,
        `objMeshData` TEXT,
        `spritePngBytes` BLOB,
        `spriteColumns` INTEGER NOT NULL,
        `spriteRows` INTEGER NOT NULL,
        `paletteHexCsv` TEXT NOT NULL,
        `aiReviewNotes` TEXT,
        `createdAtMillis` INTEGER NOT NULL
      )
      """.trimIndent()
    )
    sqliteDb.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `asset_galleries` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `name` TEXT NOT NULL,
        `description` TEXT NOT NULL,
        `themeTag` TEXT NOT NULL,
        `accentColorHex` TEXT NOT NULL,
        `layoutStyle` TEXT NOT NULL,
        `createdAtMillis` INTEGER NOT NULL
      )
      """.trimIndent()
    )
    sqliteDb.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `asset_gallery_items` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `galleryId` INTEGER NOT NULL,
        `title` TEXT NOT NULL,
        `characterTag` TEXT NOT NULL,
        `assetType` TEXT NOT NULL,
        `sourceOrigin` TEXT NOT NULL,
        `hardwareTarget` TEXT NOT NULL,
        `metricsSummary` TEXT NOT NULL,
        `curatorNote` TEXT NOT NULL,
        `objMeshData` TEXT,
        `spritePngBytes` BLOB,
        `spriteColumns` INTEGER NOT NULL,
        `spriteRows` INTEGER NOT NULL,
        `addedAtMillis` INTEGER NOT NULL,
        FOREIGN KEY(`galleryId`) REFERENCES `asset_galleries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
      )
      """.trimIndent()
    )
    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_gallery_items_galleryId` ON `asset_gallery_items` (`galleryId`)")
    sqliteDb.execSQL(
      "INSERT INTO asset_galleries (id, name, description, themeTag, accentColorHex, layoutStyle, createdAtMillis) VALUES (1, 'Legacy v2 Gallery', 'Must survive migration', 'CYBERPUNK', '#00E5FF', 'GRID_2COL', 1000)"
    )
    sqliteDb.execSQL("PRAGMA user_version = 2")
    sqliteDb.close()

    val migratedRoomDb = androidx.room.Room.databaseBuilder(
      context,
      com.example.data.local.PixelForgeDatabase::class.java,
      migratedDbName
    )
      .addMigrations(
        com.example.data.local.PixelForgeDatabase.MIGRATION_2_3,
        com.example.data.local.PixelForgeDatabase.MIGRATION_3_4
      )
      .allowMainThreadQueries()
      .build()
    try {
      val migratedRepo = com.example.data.local.AssetGalleryRepository(migratedRoomDb.assetGalleryDao(), context)
      val preservedGalleries = migratedRepo.getAllGalleriesWithItemsSnapshot()
      assertEquals(1, preservedGalleries.size)
      assertEquals("Legacy v2 Gallery", preservedGalleries.first().gallery.name)
    } finally {
      migratedRoomDb.close()
      context.deleteDatabase(migratedDbName)
    }

    // 3. Build a large >256 KB .OBJ mesh and test offloading, reading, and deletion in History & Gallery repos
    val largeObjBuilder = StringBuilder(340 * 1024)
    largeObjBuilder.append("# Large Test Mesh\n")
    for (i in 0 until 14000) {
      largeObjBuilder.append("v ${i % 50}.125 ${(i / 50) % 50}.250 ${(i % 7)}.500\n")
    }
    largeObjBuilder.append("f 1 2 3\nf 2 3 4\n")
    val largeObjText = largeObjBuilder.toString()
    assertTrue(com.example.data.local.MeshDiskStorage.exceedsInlineThreshold(largeObjText))

    val db = com.example.data.local.PixelForgeDatabase.createInMemoryDatabase(context)
    val historyRepo = com.example.data.local.AssetHistoryRepository(db.assetHistoryDao(), context)
    val galleryRepo = com.example.data.local.AssetGalleryRepository(db.assetGalleryDao(), context)

    // 3a. History offloading
    val histId = historyRepo.recordGeneratedAsset(
      com.example.data.local.AssetHistoryEntity(
        title = "Mega Sculpt 300KB",
        characterTag = "Titan",
        assetType = "MODEL_3D",
        sourceOrigin = "FILE IMPORT",
        hardwareTarget = "PS2",
        metricsSummary = "14000v/2f",
        objMeshData = largeObjText
      )
    )
    val storedHist = historyRepo.getAllHistorySnapshot().first { it.id == histId }
    assertEquals("", storedHist.objMeshData)
    assertTrue(storedHist.meshFilePath.endsWith("${histId}.obj"))
    val histDiskFile = java.io.File(storedHist.meshFilePath)
    assertTrue(histDiskFile.exists())

    val restoredHistMesh = storedHist.toMesh3D(context)
    assertNotNull(restoredHistMesh)
    assertEquals(14000, restoredHistMesh!!.vertexCount)

    historyRepo.deleteHistoryAsset(histId)
    assertEquals(false, histDiskFile.exists())

    // 3b. Gallery offloading + cascade file cleanup
    val gId = galleryRepo.createGallery(
      com.example.data.local.AssetGalleryEntity(
        name = "High-Poly Vault",
        description = "Offloaded large meshes",
        themeTag = "SHOWCASE"
      )
    )
    val gItemId = galleryRepo.addItemToGallery(
      com.example.data.local.AssetGalleryItemEntity(
        galleryId = gId,
        title = "High-Poly Titan",
        characterTag = "Titan",
        assetType = "MODEL_3D",
        sourceOrigin = "FILE IMPORT",
        hardwareTarget = "PS2",
        metricsSummary = "14000v/2f",
        objMeshData = largeObjText
      )
    )
    val storedGalleryItem = galleryRepo.getAllGalleriesWithItemsSnapshot()
      .first { it.gallery.id == gId }
      .items.first { it.id == gItemId }
    assertEquals("", storedGalleryItem.objMeshData)
    assertTrue(storedGalleryItem.meshFilePath.isNotBlank())
    val galleryDiskFile = java.io.File(storedGalleryItem.meshFilePath)
    assertTrue(galleryDiskFile.exists())

    val restoredGalleryMesh = storedGalleryItem.toMesh3D(context)
    assertNotNull(restoredGalleryMesh)
    assertEquals(14000, restoredGalleryMesh!!.vertexCount)

    galleryRepo.deleteGallery(gId)
    assertEquals(false, galleryDiskFile.exists())
    db.close()
  }

  @Test
  fun `test chroma key background stripper for magenta green solid and transparent pngs`() {
    // 1. Solid bright magenta (#FF00FF) border with cyan center sprite
    val magentaBmp = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
    magentaBmp.eraseColor(android.graphics.Color.MAGENTA)
    for (y in 4..11) {
      for (x in 4..11) {
        magentaBmp.setPixel(x, y, android.graphics.Color.CYAN)
      }
    }
    val strippedMagenta = com.example.model.importer.UniversalAssetImporter.stripChromaKeyBackground(magentaBmp)
    assertEquals(android.graphics.Color.TRANSPARENT, strippedMagenta.getPixel(0, 0))
    assertEquals(android.graphics.Color.TRANSPARENT, strippedMagenta.getPixel(15, 15))
    assertEquals(android.graphics.Color.CYAN, strippedMagenta.getPixel(8, 8))

    // 2. Solid bright green (#00FF00) border
    val greenBmp = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
    greenBmp.eraseColor(android.graphics.Color.GREEN)
    greenBmp.setPixel(8, 8, android.graphics.Color.RED)
    val strippedGreen = com.example.model.importer.UniversalAssetImporter.stripChromaKeyBackground(greenBmp)
    assertEquals(android.graphics.Color.TRANSPARENT, strippedGreen.getPixel(0, 0))
    assertEquals(android.graphics.Color.RED, strippedGreen.getPixel(8, 8))

    // 3. Uniform solid opaque background corners (e.g. dark slate)
    val solidColor = android.graphics.Color.rgb(24, 32, 48)
    val solidBmp = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
    solidBmp.eraseColor(solidColor)
    solidBmp.setPixel(8, 8, android.graphics.Color.YELLOW)
    val strippedSolid = com.example.model.importer.UniversalAssetImporter.stripChromaKeyBackground(solidBmp)
    assertEquals(android.graphics.Color.TRANSPARENT, strippedSolid.getPixel(0, 0))
    assertEquals(android.graphics.Color.YELLOW, strippedSolid.getPixel(8, 8))

    // 4. Already-transparent PNG must remain untouched
    val transparentBmp = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
    transparentBmp.eraseColor(android.graphics.Color.TRANSPARENT)
    transparentBmp.setPixel(8, 8, android.graphics.Color.MAGENTA)
    val preserved = com.example.model.importer.UniversalAssetImporter.stripChromaKeyBackground(transparentBmp)
    assertEquals(android.graphics.Color.TRANSPARENT, preserved.getPixel(0, 0))
    assertEquals(android.graphics.Color.MAGENTA, preserved.getPixel(8, 8))

    // 5. Verify importSpriteSheetFromStream honors stripChromaKeyBg toggle
    val out = java.io.ByteArrayOutputStream()
    magentaBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    val pngBytes = out.toByteArray()

    val importedStripped = com.example.model.importer.UniversalAssetImporter.importSpriteSheetFromStream(
      inputStream = java.io.ByteArrayInputStream(pngBytes),
      fileName = "magenta_sprite.png",
      sourceKind = com.example.model.importer.AssetImportSourceKind.FILE,
      columns = 1,
      rows = 1,
      stripChromaKeyBg = true
    )
    assertNotNull(importedStripped)
    assertEquals(android.graphics.Color.TRANSPARENT, importedStripped!!.spriteSheet.sourceBitmap.getPixel(0, 0))
    assertEquals(android.graphics.Color.CYAN, importedStripped.spriteSheet.sourceBitmap.getPixel(8, 8))

    val importedUnstripped = com.example.model.importer.UniversalAssetImporter.importSpriteSheetFromStream(
      inputStream = java.io.ByteArrayInputStream(pngBytes),
      fileName = "magenta_sprite.png",
      sourceKind = com.example.model.importer.AssetImportSourceKind.FILE,
      columns = 1,
      rows = 1,
      stripChromaKeyBg = false
    )
    assertNotNull(importedUnstripped)
    assertEquals(android.graphics.Color.MAGENTA, importedUnstripped!!.spriteSheet.sourceBitmap.getPixel(0, 0))
  }

  @Test
  fun `test mpv omni store asset index json export ephemeral sweep and arcade comic cel preset`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Test MPV Omni-Store asset-index.json export
    val vm = MainViewModel()
    val gallery = vm.uiState.value.assetGalleries.first()
    val exportedJson = vm.prepareAssetIndexJsonExport(gallery.gallery.id)
    assertNotNull(exportedJson)

    val parsed = org.json.JSONObject(exportedJson!!)
    assertEquals(
      "https://raw.githubusercontent.com/Summer-Warrior/Asset-Holding-Software/main/",
      parsed.getString("baseUrl")
    )
    val assets = parsed.getJSONArray("assets")
    assertEquals(gallery.items.size, assets.length())
    val firstEntry = assets.getJSONObject(0)
    assertTrue(firstEntry.getString("key").isNotBlank())
    assertTrue(firstEntry.getString("path").startsWith("assets/"))
    assertEquals(exportedJson, vm.uiState.value.exportDialogText)

    // 2. Test startup ephemeral_sources cache directory sweep
    val ephemeralDir = java.io.File(context.cacheDir, "ephemeral_sources")
    ephemeralDir.mkdirs()
    val leftover1 = java.io.File(ephemeralDir, "temp_ref_01.png").apply { writeText("temp") }
    val nestedDir = java.io.File(ephemeralDir, "nested").apply { mkdirs() }
    val leftover2 = java.io.File(nestedDir, "temp_ref_02.obj").apply { writeText("v 0 0 0") }
    assertTrue(leftover1.exists())
    assertTrue(leftover2.exists())

    val deletedCount = com.example.model.importer.UniversalAssetImporter.sweepEphemeralSources(context)
    assertTrue(deletedCount >= 2)
    assertEquals(false, leftover1.exists())
    assertEquals(false, leftover2.exists())

    // 3. Test 32-Bit Arcade Comic-Cel Fusion style preset in GeminiAssetForge & MainViewModel
    assertEquals(
      "32-Bit Arcade Comic-Cel Fusion",
      com.example.ai.ArtTheme.ARCADE_COMIC_CEL.displayName
    )
    assertTrue(
      com.example.ai.GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_BLOCK.contains(
        "32-bit arcade & 2D comic-cel style: punchy high-contrast ramps, crisp edges, clearcoat armor/visors w/ glints vs matte skin/fabric"
      )
    )
    vm.selectArcadeComicCelStylePreset()
    assertEquals(com.example.ai.ArtTheme.ARCADE_COMIC_CEL, vm.uiState.value.selectedTheme)
    assertEquals(
      com.example.ai.GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_BLOCK,
      vm.uiState.value.geminiCustomInstruction
    )
  }

  @Test
  fun `test openrouter pkce s256 challenge generation and loopback auth url formatting`() {
    // 1. Random code verifier meets RFC 7636 bounds (43..128 URL-safe chars, no padding)
    val verifier = com.example.ai.OpenRouterPkceHelper.generateCodeVerifier()
    assertTrue(verifier.length in 43..128)
    assertTrue(verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    assertEquals(false, verifier.contains('='))

    // 2. Deterministic RFC 7636 Appendix B S256 test vector
    val rfcVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    val rfcExpectedChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
    val computed = com.example.ai.OpenRouterPkceHelper.computeS256CodeChallenge(rfcVerifier)
    assertEquals(rfcExpectedChallenge, computed)
    assertEquals(false, computed.contains('='))
    assertEquals(false, computed.contains('+'))
    assertEquals(false, computed.contains('/'))

    // 3. Exact loopback auth URL formatting
    val authUrl = com.example.ai.OpenRouterPkceHelper.buildAuthUrl(43123, computed)
    assertEquals(
      "https://openrouter.ai/auth?callback_url=http://127.0.0.1:43123/callback&code_challenge=$rfcExpectedChallenge&code_challenge_method=S256",
      authUrl
    )

    // 4. Loopback HTTP callback request line extraction
    val extracted = com.example.ai.OpenRouterPkceHelper.extractCodeFromRequestLine(
      "GET /callback?code=or_pkce_code_98765&state=ok HTTP/1.1"
    )
    assertEquals("or_pkce_code_98765", extracted)
    assertEquals(
      null,
      com.example.ai.OpenRouterPkceHelper.extractCodeFromRequestLine("GET /favicon.ico HTTP/1.1")
    )
  }

  @Test
  fun `test secure ai key vault save read hint masking and clear behavior`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vault = com.example.ai.SecureAiKeyVault(context)
    vault.clearAll()

    assertEquals(false, vault.hasCredential(com.example.ai.AiProviderType.OPENROUTER))
    assertEquals(null, vault.getCredential(com.example.ai.AiProviderType.OPENROUTER))

    val secretKey = "sk-or-v1-supersecret9876543210abcd"
    vault.saveCredential(com.example.ai.AiProviderType.OPENROUTER, secretKey, hintPrefix = "OAuth PKCE")
    assertTrue(vault.hasCredential(com.example.ai.AiProviderType.OPENROUTER))
    assertEquals(secretKey, vault.getCredential(com.example.ai.AiProviderType.OPENROUTER))

    // Display hint must mask secret and never expose raw key
    val hint = vault.getAccountHint(com.example.ai.AiProviderType.OPENROUTER)
    assertTrue(hint.contains("abcd"))
    assertEquals(false, hint.contains("supersecret"))

    // Multiple providers stored independently
    vault.saveCredential(com.example.ai.AiProviderType.ANTHROPIC_CLAUDE, "sk-ant-api03-testkey9999")
    assertTrue(vault.hasCredential(com.example.ai.AiProviderType.ANTHROPIC_CLAUDE))
    assertEquals("sk-ant-api03-testkey9999", vault.getCredential(com.example.ai.AiProviderType.ANTHROPIC_CLAUDE))

    // Clearing one provider does not clear another
    vault.clearCredential(com.example.ai.AiProviderType.OPENROUTER)
    assertEquals(false, vault.hasCredential(com.example.ai.AiProviderType.OPENROUTER))
    assertEquals(null, vault.getCredential(com.example.ai.AiProviderType.OPENROUTER))
    assertTrue(vault.hasCredential(com.example.ai.AiProviderType.ANTHROPIC_CLAUDE))

    // Blank secret clears existing credential
    vault.saveCredential(com.example.ai.AiProviderType.ANTHROPIC_CLAUDE, "   ")
    assertEquals(false, vault.hasCredential(com.example.ai.AiProviderType.ANTHROPIC_CLAUDE))
  }

  @Test
  fun `test multi source context registry toggling ssrf protection and max chars budget`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    java.io.File(context.filesDir, com.example.ai.MultiSourceContextRegistry.SOURCES_FILE_NAME).delete()
    val registry = com.example.ai.MultiSourceContextRegistry(context)

    // 1. Add multiple knowledge sources
    registry.addPastedNoteSource(
      title = "Cyber Knight World Bible",
      content = "Armor uses clearcoat cobalt visors with warm tungsten rim lighting.",
      kind = com.example.ai.KnowledgeSourceKind.NOTEBOOK_MARKDOWN
    )
    registry.addPastedNoteSource(
      title = "Palette Constraints",
      content = "Limit sprite shading ramps to 4 crisp steps per hue for 32-bit readability.",
      kind = com.example.ai.KnowledgeSourceKind.PASTED_NOTES
    )

    val allSources = registry.getSources()
    assertEquals(4, allSources.size) // Built-in Layer 1 + Active Project + 2 added notes
    val paletteEntry = allSources.first { it.title == "Palette Constraints" }

    // 2. Assemble with all enabled
    val combined = registry.assembleContextBlock(
      queryHint = "cobalt visor armor",
      activeProjectSummary = "Active Asset: Mecha Hero 3D Mesh + 4x4 Sprite Sheet",
      maxChars = 10_000
    )
    assertTrue(combined.contains("### [Active PixelForge Project Context]"))
    assertTrue(combined.contains("Active Asset: Mecha Hero 3D Mesh + 4x4 Sprite Sheet"))
    assertTrue(combined.contains("### [Cyber Knight World Bible]"))
    assertTrue(combined.contains("### [Palette Constraints]"))

    // 3. Toggle one source OFF and verify exclusion
    registry.toggleSource(paletteEntry.id, enabled = false)
    val toggledBlock = registry.assembleContextBlock(
      queryHint = "cobalt visor",
      activeProjectSummary = "Active Asset: Mecha Hero",
      maxChars = 10_000
    )
    assertTrue(toggledBlock.contains("### [Cyber Knight World Bible]"))
    assertEquals(false, toggledBlock.contains("### [Palette Constraints]"))

    // 4. Verify strict maxChars budget trimming with huge source
    registry.addPastedNoteSource(
      title = "Huge Lore Compendium",
      content = "PIXELFORGE_LORE_ENTRY_".repeat(500),
      kind = com.example.ai.KnowledgeSourceKind.LOCAL_FILE
    )
    val trimmed = registry.assembleContextBlock(
      queryHint = "lore",
      activeProjectSummary = "Compact summary",
      maxChars = 320
    )
    assertTrue(trimmed.length <= 320)
    assertTrue(trimmed.contains("### ["))

    // 5. Verify SSRF protection blocks non-HTTPS, localhost, RFC1918 private, and link-local metadata IPs
    val blockedUrls = listOf(
      "http://raw.githubusercontent.com/Summer-Warrior/Asset-Holding-Software/main/README.md",
      "https://127.0.0.1/secret.json",
      "https://localhost:8080/internal",
      "https://169.254.169.254/latest/meta-data/",
      "https://10.0.0.5/private.md",
      "https://192.168.1.10/notes.txt",
      "https://172.16.0.12/internal.json",
      "https://[::1]/loopback.md",
      "https://user:pass@raw.githubusercontent.com/org/repo/main/file.md"
    )
    blockedUrls.forEach { url ->
      val check = com.example.ai.MultiSourceContextRegistry.validatePublicHttpsUrl(url, resolveDns = false)
      assertTrue("Expected SSRF check to block $url", check.isFailure)
    }

    val allowed = com.example.ai.MultiSourceContextRegistry.validatePublicHttpsUrl(
      "https://raw.githubusercontent.com/Summer-Warrior/Asset-Holding-Software/main/README.md",
      resolveDns = false
    )
    assertTrue(allowed.isSuccess)
  }

  @Test
  fun `test omni ai router payload serialization for all five external endpoints and role routing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.ai.OmniAiRouter.attachContext(context)

    val fakeImageB64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB"
    val prompt = "Enhance this cyber knight sprite into a 32-bit arcade comic-cel fusion."
    val systemPrompt = "Return strict PixelForge asset JSON."

    // 1. Direct Gemini payload (text + vision JSON & image generation)
    val directGeminiJson = com.example.ai.OmniAiRouter.buildDirectGeminiPayload(
      prompt = prompt,
      systemInstruction = systemPrompt,
      imageBase64Jpeg = fakeImageB64,
      expectJson = true,
      requestImageOutput = false
    )
    assertEquals(
      "application/json",
      directGeminiJson.getJSONObject("generationConfig").getString("responseMimeType")
    )
    val geminiParts = directGeminiJson.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
    assertEquals("image/jpeg", geminiParts.getJSONObject(0).getJSONObject("inlineData").getString("mimeType"))
    assertEquals(fakeImageB64, geminiParts.getJSONObject(0).getJSONObject("inlineData").getString("data"))
    assertEquals(prompt, geminiParts.getJSONObject(1).getString("text"))

    val directGeminiImg = com.example.ai.OmniAiRouter.buildDirectGeminiPayload(
      prompt = prompt,
      requestImageOutput = true
    )
    val geminiModalities = directGeminiImg.getJSONObject("generationConfig").getJSONArray("responseModalities")
    assertEquals("IMAGE", geminiModalities.getString(0))
    assertEquals("TEXT", geminiModalities.getString(1))

    // 2. OpenRouter payload (multimodal image_url + image generation modalities)
    val openRouterImg = com.example.ai.OmniAiRouter.buildOpenRouterPayload(
      modelId = "google/gemini-2.5-flash-image-preview",
      prompt = prompt,
      systemInstruction = systemPrompt,
      imageBase64Jpeg = fakeImageB64,
      requestImageOutput = true
    )
    assertEquals("google/gemini-2.5-flash-image-preview", openRouterImg.getString("model"))
    val orModalities = openRouterImg.getJSONArray("modalities")
    assertEquals("image", orModalities.getString(0))
    assertEquals("text", orModalities.getString(1))
    val orUserContent = openRouterImg.getJSONArray("messages").getJSONObject(1).getJSONArray("content")
    assertEquals(
      "data:image/jpeg;base64,$fakeImageB64",
      orUserContent.getJSONObject(1).getJSONObject("image_url").getString("url")
    )

    // 3. Anthropic Claude payload (native base64 image block + system + max_tokens)
    val claudePayload = com.example.ai.OmniAiRouter.buildAnthropicClaudePayload(
      modelId = "claude-sonnet-4-20250514",
      prompt = prompt,
      systemInstruction = systemPrompt,
      imageBase64Jpeg = fakeImageB64
    )
    assertEquals("claude-sonnet-4-20250514", claudePayload.getString("model"))
    assertEquals(systemPrompt, claudePayload.getString("system"))
    assertTrue(claudePayload.getInt("max_tokens") >= 1024)
    val claudeBlocks = claudePayload.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
    assertEquals("image", claudeBlocks.getJSONObject(0).getString("type"))
    assertEquals("base64", claudeBlocks.getJSONObject(0).getJSONObject("source").getString("type"))
    assertEquals(fakeImageB64, claudeBlocks.getJSONObject(0).getJSONObject("source").getString("data"))
    assertEquals("text", claudeBlocks.getJSONObject(1).getString("type"))

    // 4. OpenAI text + image payloads
    val openAiText = com.example.ai.OmniAiRouter.buildOpenAiTextPayload(
      modelId = "gpt-4o",
      prompt = prompt,
      expectJson = true
    )
    assertEquals("gpt-4o", openAiText.getString("model"))
    assertEquals("json_object", openAiText.getJSONObject("response_format").getString("type"))

    val openAiImg = com.example.ai.OmniAiRouter.buildOpenAiImagePayload(
      modelId = "gpt-image-1",
      prompt = prompt
    )
    assertEquals("gpt-image-1", openAiImg.getString("model"))
    assertEquals("b64_json", openAiImg.getString("response_format"))

    // 5. Local OpenAI-Compatible payload
    val localCompat = com.example.ai.OmniAiRouter.buildLocalOpenAiCompatPayload(
      modelId = "llama3.2-vision",
      prompt = prompt,
      imageBase64Jpeg = fakeImageB64
    )
    assertEquals("llama3.2-vision", localCompat.getString("model"))
    assertEquals(1, localCompat.getJSONArray("messages").length())

    // 6. Role routing + profile never leaks raw credentials
    com.example.ai.OmniAiRouter.saveProviderCredential(
      com.example.ai.AiProviderType.DIRECT_GEMINI,
      "AIzaSyTestSecretKeyNeverLeak1234"
    )
    val profile = com.example.ai.OmniAiRouter.getProviderProfile(com.example.ai.AiProviderType.DIRECT_GEMINI)
    assertTrue(profile.isConnected)
    assertEquals(false, profile.toString().contains("AIzaSyTestSecretKeyNeverLeak"))

    val updatedRouting = com.example.ai.OmniAiRouter.setActiveProviderForRole(
      com.example.ai.AiTaskRole.TEXT_AND_SPECS,
      com.example.ai.AiProviderType.OPENROUTER
    )
    assertEquals(com.example.ai.AiProviderType.OPENROUTER, updatedRouting.activeTextProvider)
    assertEquals(com.example.ai.AiProviderType.FIREBASE_STUDIO, updatedRouting.activeImageProvider)

    // Restore default Firebase Studio routing cleanly
    com.example.ai.OmniAiRouter.setActiveProviderForRole(
      com.example.ai.AiTaskRole.TEXT_AND_SPECS,
      com.example.ai.AiProviderType.FIREBASE_STUDIO
    )
    com.example.ai.OmniAiRouter.clearProviderCredential(com.example.ai.AiProviderType.DIRECT_GEMINI)
  }

  @Test
  fun `test room migration 3 to 4 preserves existing rows with default game and group tags`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbName = "pixelforge_migration_3_4_test_${System.currentTimeMillis()}.db"
    val dbFile = context.getDatabasePath(dbName)
    dbFile.parentFile?.mkdirs()
    if (dbFile.exists()) dbFile.delete()

    // 1. Seed a real v3 SQLite database (matching Room v3 schemas before gameTag & groupTag existed)
    val rawDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
    try {
      rawDb.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `asset_history` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          `title` TEXT NOT NULL,
          `characterTag` TEXT NOT NULL,
          `assetType` TEXT NOT NULL,
          `sourceOrigin` TEXT NOT NULL,
          `hardwareTarget` TEXT NOT NULL,
          `metricsSummary` TEXT NOT NULL,
          `objMeshData` TEXT,
          `meshFilePath` TEXT NOT NULL DEFAULT '',
          `spritePngBytes` BLOB,
          `spriteColumns` INTEGER NOT NULL,
          `spriteRows` INTEGER NOT NULL,
          `paletteHexCsv` TEXT NOT NULL,
          `aiReviewNotes` TEXT,
          `createdAtMillis` INTEGER NOT NULL
        )
        """.trimIndent()
      )
      rawDb.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `asset_galleries` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          `name` TEXT NOT NULL,
          `description` TEXT NOT NULL,
          `themeTag` TEXT NOT NULL,
          `accentColorHex` TEXT NOT NULL,
          `layoutStyle` TEXT NOT NULL,
          `createdAtMillis` INTEGER NOT NULL
        )
        """.trimIndent()
      )
      rawDb.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `asset_gallery_items` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          `galleryId` INTEGER NOT NULL,
          `title` TEXT NOT NULL,
          `characterTag` TEXT NOT NULL,
          `assetType` TEXT NOT NULL,
          `sourceOrigin` TEXT NOT NULL,
          `hardwareTarget` TEXT NOT NULL,
          `metricsSummary` TEXT NOT NULL,
          `curatorNote` TEXT NOT NULL,
          `objMeshData` TEXT,
          `meshFilePath` TEXT NOT NULL DEFAULT '',
          `spritePngBytes` BLOB,
          `spriteColumns` INTEGER NOT NULL,
          `spriteRows` INTEGER NOT NULL,
          `addedAtMillis` INTEGER NOT NULL,
          FOREIGN KEY(`galleryId`) REFERENCES `asset_galleries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
      )
      rawDb.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_gallery_items_galleryId` ON `asset_gallery_items` (`galleryId`)")

      rawDb.execSQL(
        """
        INSERT INTO asset_history (id, title, characterTag, assetType, sourceOrigin, hardwareTarget, metricsSummary, objMeshData, meshFilePath, spritePngBytes, spriteColumns, spriteRows, paletteHexCsv, aiReviewNotes, createdAtMillis)
        VALUES (10, 'Legacy Cyber Mech', 'Kaelen', 'MODEL_3D', 'STUDIO', 'PS1', '128v/64f', 'v 0 0 0', '', NULL, 1, 1, '#00E5FF', NULL, 1700000000000)
        """.trimIndent()
      )
      rawDb.execSQL(
        """
        INSERT INTO asset_galleries (id, name, description, themeTag, accentColorHex, layoutStyle, createdAtMillis)
        VALUES (1, 'Legacy Showcase', 'Pre-v4 gallery', 'CYBERPUNK', '#00E5FF', 'GRID_2COL', 1700000000000)
        """.trimIndent()
      )
      rawDb.execSQL(
        """
        INSERT INTO asset_gallery_items (id, galleryId, title, characterTag, assetType, sourceOrigin, hardwareTarget, metricsSummary, curatorNote, objMeshData, meshFilePath, spritePngBytes, spriteColumns, spriteRows, addedAtMillis)
        VALUES (20, 1, 'Legacy Valkyrie Sheet', 'Valkyrie-09', 'SPRITE_2D', 'IMPORT', 'GBA', '4 frames', 'Curated in v3', NULL, '', NULL, 4, 1, 1700000000000)
        """.trimIndent()
      )
      rawDb.version = 3
    } finally {
      rawDb.close()
    }

    // 2. Open with Room v4 using MIGRATION_3_4 (no destructive migration!)
    val migratedDb = androidx.room.Room.databaseBuilder(
      context,
      com.example.data.local.PixelForgeDatabase::class.java,
      dbName
    )
      .addMigrations(
        com.example.data.local.PixelForgeDatabase.MIGRATION_2_3,
        com.example.data.local.PixelForgeDatabase.MIGRATION_3_4
      )
      .allowMainThreadQueries()
      .build()

    try {
      val historyRepo = com.example.data.local.AssetHistoryRepository(migratedDb.assetHistoryDao(), context)
      val galleryRepo = com.example.data.local.AssetGalleryRepository(migratedDb.assetGalleryDao(), context)

      val historyRows = historyRepo.getAllHistorySnapshot()
      assertEquals(1, historyRows.size)
      assertEquals("Legacy Cyber Mech", historyRows[0].title)
      assertEquals("General Studio", historyRows[0].gameTag)
      assertEquals("Core Roster", historyRows[0].groupTag)

      val galleries = galleryRepo.getAllGalleriesWithItemsSnapshot()
      assertEquals(1, galleries.size)
      assertEquals(1, galleries[0].items.size)
      assertEquals("Legacy Valkyrie Sheet", galleries[0].items[0].title)
      assertEquals("General Studio", galleries[0].items[0].gameTag)
      assertEquals("Core Roster", galleries[0].items[0].groupTag)

      // 3. Verify 1-tap tag reassignment persists cleanly in v4
      historyRepo.updateHistoryTags(10L, "Aegis Grid SRW", "Mechs & Vehicles", "Kaelen")
      galleryRepo.updateGalleryItemTags(20L, "MPV In-Universe MMO", "Protagonists", "Valkyrie-09")

      val updatedHist = historyRepo.getHistoryItemById(10L)
      assertEquals("Aegis Grid SRW", updatedHist?.gameTag)
      assertEquals("Mechs & Vehicles", updatedHist?.groupTag)

      val updatedGalleryItem = galleryRepo.getAllGalleriesWithItemsSnapshot()[0].items[0]
      assertEquals("MPV In-Universe MMO", updatedGalleryItem.gameTag)
      assertEquals("Protagonists", updatedGalleryItem.groupTag)
    } finally {
      migratedDb.close()
      dbFile.delete()
    }
  }

  @Test
  fun `test prompt studio engine constraints single active tier chroma key and zero positive checkerboard`() {
    // 1. Verify updated ARCADE_COMIC_CEL_STYLE_BLOCK
    assertTrue(
      com.example.ai.GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_BLOCK.contains("ink-pool shadows w/ ordered dithering & halftones")
    )
    assertTrue(
      com.example.ai.GeminiAssetForge.ARCADE_COMIC_CEL_STYLE_BLOCK.contains("glowing Mana Circuits")
    )

    // 2. Long subject description + pink/magenta/purple palette -> Lime Green #00FF00 & compact <= 500 chars
    val longDesc = "Sakura cyber-valkyrie guardian with glowing lavender plasma wings, neon magenta visor, checkerboard cape trim, and ornate cobalt gauntlet plating ".repeat(6)
    val pinkOut = com.example.ai.PromptStudioEngine.buildPrompts(
      subjectDescription = longDesc,
      tier = com.example.ai.PromptHardwareTier.TRUE_FUSION_HD,
      preset = com.example.ai.PromptAssetPreset.GRID_SHEET_2X4,
      includeHumanoidHeroicProportions = false,
      paletteHexList = listOf("#FF40C8", "#7B2CBF", "#00E5FF"),
      gameTag = "MPV In-Universe MMO",
      groupTag = "Protagonists",
      characterTag = "Valkyrie-09"
    )
    assertEquals("#00FF00", pinkOut.chromaKey.hexCode)
    assertTrue("compact500 must be <= 500 chars (was ${pinkOut.compact500.length})", pinkOut.compact500.length <= 500)
    // Single active tier: TRUE_FUSION_HD uses comic halftone, NOT 16-bit BGR555 or 4x4 dithering
    assertTrue(pinkOut.positiveSpecOnly.contains("halftone", ignoreCase = true))
    assertEquals(false, pinkOut.positiveSpecOnly.contains("BGR555", ignoreCase = true))
    assertEquals(false, pinkOut.positiveSpecOnly.contains("4x4 dithering", ignoreCase = true))
    // Proportions toggle OFF -> omits 6.5-head
    assertEquals(false, pinkOut.positiveSpecOnly.contains("6.5-head", ignoreCase = true))
    assertEquals(false, pinkOut.compact500.contains("6.5-head", ignoreCase = true))
    // Zero "checkerboard" inside positive prompts, present inside negative prompt
    assertEquals(false, pinkOut.compact500.contains("checkerboard", ignoreCase = true))
    assertEquals(false, pinkOut.natural4Sentence.contains("checkerboard", ignoreCase = true))
    assertEquals(false, pinkOut.positiveSpecOnly.contains("checkerboard", ignoreCase = true))
    assertTrue(pinkOut.negativePrompt.contains("checkerboard", ignoreCase = true))

    // 3. Non-pink/purple subject -> Magenta #FF00FF, 6.5-head toggle ON, 16-bit tier only, 4 clean sentences
    val knightOut = com.example.ai.PromptStudioEngine.buildPrompts(
      subjectDescription = "cobalt armored knight holding a golden solar shield",
      tier = com.example.ai.PromptHardwareTier.PIXEL_16BIT,
      preset = com.example.ai.PromptAssetPreset.SINGLE_SPRITE_1X1,
      includeHumanoidHeroicProportions = true,
      paletteHexList = listOf("#1D4ED8", "#FACC15", "#0F172A")
    )
    assertEquals("#FF00FF", knightOut.chromaKey.hexCode)
    assertTrue(knightOut.compact500.length <= 500)
    assertTrue(knightOut.positiveSpecOnly.contains("BGR555"))
    assertEquals(false, knightOut.positiveSpecOnly.contains("halftone", ignoreCase = true))
    assertTrue(knightOut.positiveSpecOnly.contains("6.5-head"))
    assertTrue(knightOut.natural4Sentence.contains("6.5-head"))
    // Count sentences in natural4Sentence (should be 4 periods ending 4 sentences)
    val sentenceCount = knightOut.natural4Sentence.split(Regex("(?<=\\.)\\s+")).filter { it.isNotBlank() }.size
    assertEquals(4, sentenceCount)
  }

  @Test
  fun `test canonical asset auditor delta ip clean room chroma bleed ephemeral sweep and contracts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ps1 = ConsoleHardwareCatalog.getById("ps1")

    // Helper: clean transparent-bordered 64x64 sprite with solid cyan square inside
    fun makeCleanSprite(fillColor: Int, offsetX: Int = 12): android.graphics.Bitmap {
      val bmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
      bmp.eraseColor(android.graphics.Color.TRANSPARENT)
      for (y in 12 until 52) {
        for (x in offsetX until (offsetX + 36).coerceAtMost(60)) {
          bmp.setPixel(x, y, fillColor)
        }
      }
      return bmp
    }

    val spriteA = makeCleanSprite(android.graphics.Color.rgb(0, 229, 255), offsetX = 12)
    val spriteIdentical = makeCleanSprite(android.graphics.Color.rgb(0, 229, 255), offsetX = 12)
    val spriteRedesigned = makeCleanSprite(android.graphics.Color.rgb(255, 160, 0), offsetX = 24)

    // 1. Identical bitmaps -> visualDeltaPercent == 0 & NEEDS_REVISION
    val identicalReport = com.example.ai.CanonicalAssetAndCodeAuditor.auditAssetLocally(
      generatedBitmap = spriteA,
      referenceBitmapOrNull = spriteIdentical,
      targetHardware = ps1,
      intendedForPublicRelease = false
    )
    assertEquals(0, identicalReport.visualDeltaPercent)
    assertEquals(com.example.ai.AssetFidelityTag.NEEDS_REVISION, identicalReport.fidelityTag)
    assertTrue(identicalReport.regressionOrIpFlags.any { it.contains("15%") })

    // 2. Meaningfully redesigned bitmap with reference (Private Fan-Derived)
    val redesignedPrivate = com.example.ai.CanonicalAssetAndCodeAuditor.auditAssetLocally(
      generatedBitmap = spriteRedesigned,
      referenceBitmapOrNull = spriteA,
      targetHardware = ps1,
      intendedForPublicRelease = false
    )
    assertTrue("Expected visualDelta >= 15%, got ${redesignedPrivate.visualDeltaPercent}", redesignedPrivate.visualDeltaPercent >= 15)
    assertEquals(com.example.ai.AssetFidelityTag.PRIVATE_FAN_DERIVED, redesignedPrivate.fidelityTag)
    assertTrue(redesignedPrivate.chromaAndAlphaClean)

    // 3. Same derived bitmap marked intendedForPublicRelease = true -> NEEDS_REVISION IP guard
    val redesignedPublic = com.example.ai.CanonicalAssetAndCodeAuditor.auditAssetLocally(
      generatedBitmap = spriteRedesigned,
      referenceBitmapOrNull = spriteA,
      targetHardware = ps1,
      intendedForPublicRelease = true
    )
    assertEquals(com.example.ai.AssetFidelityTag.NEEDS_REVISION, redesignedPublic.fidelityTag)
    assertTrue(redesignedPublic.regressionOrIpFlags.any { it.contains("Public Release", ignoreCase = true) })

    // 4. Clean standalone bitmap -> PUBLIC_CLEAN_ROOM
    val cleanStandalone = com.example.ai.CanonicalAssetAndCodeAuditor.auditAssetLocally(
      generatedBitmap = spriteA,
      referenceBitmapOrNull = null,
      targetHardware = ps1,
      intendedForPublicRelease = true
    )
    assertEquals(com.example.ai.AssetFidelityTag.PUBLIC_CLEAN_ROOM, cleanStandalone.fidelityTag)
    assertEquals(100, cleanStandalone.visualDeltaPercent)

    // 5. Unstripped opaque magenta chroma-key border -> chromaAndAlphaClean == false & NEEDS_REVISION
    val magentaBorderBmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
    magentaBorderBmp.eraseColor(android.graphics.Color.rgb(255, 0, 255))
    val chromaReport = com.example.ai.CanonicalAssetAndCodeAuditor.auditAssetLocally(
      generatedBitmap = magentaBorderBmp,
      referenceBitmapOrNull = null,
      targetHardware = ps1,
      intendedForPublicRelease = false
    )
    assertEquals(false, chromaReport.chromaAndAlphaClean)
    assertEquals(com.example.ai.AssetFidelityTag.NEEDS_REVISION, chromaReport.fidelityTag)

    // 6. approveAndCleanEphemeralReference purges cacheDir/ephemeral_sources
    val ephDir = java.io.File(context.cacheDir, "ephemeral_sources")
    ephDir.mkdirs()
    val tmpRef = java.io.File(ephDir, "temp_audit_ref.png")
    tmpRef.writeText("ephemeral")
    assertTrue(tmpRef.exists())
    val purged = com.example.ai.CanonicalAssetAndCodeAuditor.approveAndCleanEphemeralReference(context)
    assertTrue(purged >= 1)
    assertEquals(false, tmpRef.exists())

    // 7. Hardware & JSON contract compliance check
    val validIndexJson = """
      {
        "baseUrl": "https://raw.githubusercontent.com/Summer-Warrior/Asset-Holding-Software/main/",
        "assets": [
          { "key": "cyber_pack/kaelen", "path": "assets/cyber_pack/kaelen.png" }
        ]
      }
    """.trimIndent()
    val contractReport = com.example.ai.CanonicalAssetAndCodeAuditor.auditHardwareAndContractCompliance(
      mesh = ProceduralMeshes.createSciFiStarfighter(),
      spriteSheet = SampleSpriteSheets.createCyberNinjaSheet(),
      hardwareSpec = ps1,
      assetIndexJson = validIndexJson
    )
    assertTrue(contractReport.contractJsonPassed)
    assertTrue(contractReport.passedChecks.isNotEmpty())
  }

  @Test
  fun `test asset resolver bound slots geometric placeholder fallback and skin manifest json`() {
    val manifest = com.example.model.vault.SkinManifest(
      skinId = "mpv-arcade-cel-v1",
      displayName = "MPV 32-Bit Arcade Cel Pack",
      gameTag = "MPV In-Universe MMO",
      slotBindings = mapOf(
        "hero_idle_sheet" to "assets/mpv/protagonists/kaelen_idle.png",
        "boss_mech_mesh" to "assets/mpv/bosses/valkyrie_mech.obj"
      )
    )
    val resolver = com.example.model.vault.AssetResolver(manifest)

    // 1. Bound slot resolves cleanly to BoundAsset
    val bound = resolver.resolve("hero_idle_sheet")
    assertTrue(bound is com.example.model.vault.ResolvedAsset.BoundAsset)
    val boundAsset = bound as com.example.model.vault.ResolvedAsset.BoundAsset
    assertEquals("assets/mpv/protagonists/kaelen_idle.png", boundAsset.assetPath)
    assertEquals("mpv-arcade-cel-v1", boundAsset.skinId)

    // 2. Missing slot returns deterministic GeometricPlaceholder without crashing
    val missing = resolver.resolve("unassigned_secret_stage_bg")
    assertTrue(missing is com.example.model.vault.ResolvedAsset.GeometricPlaceholder)
    val placeholder = missing as com.example.model.vault.ResolvedAsset.GeometricPlaceholder
    assertEquals("unassigned_secret_stage_bg", placeholder.slotName)
    assertTrue(placeholder.fallbackColorHex.startsWith("#"))

    // 3. SkinManifest JSON round-trip preserves skinId, gameTag, and slotBindings
    val json = manifest.toJsonString()
    val parsed = com.example.model.vault.SkinManifest.fromJsonString(json)
    assertEquals(manifest.skinId, parsed.skinId)
    assertEquals(manifest.gameTag, parsed.gameTag)
    assertEquals(manifest.slotBindings, parsed.slotBindings)
  }

  @Test
  fun `test twenty color wheel boots with zero colors selected and toggles cleanly`() {
    // 1. Exactly 20 curated colors on the True Fusion Color Wheel
    assertEquals(20, com.example.model.masterpack.TWENTY_COLOR_WHEEL.size)
    assertEquals(20, com.example.model.masterpack.TWENTY_COLOR_WHEEL.map { it.hex.uppercase() }.distinct().size)

    // 2. MainViewModel MUST boot with 0 colors selected
    val vm = MainViewModel()
    assertTrue(vm.uiState.value.selectedColorWheelHexes.isEmpty())

    // 3. Toggle up to 4 swatches cleanly
    val w = com.example.model.masterpack.TWENTY_COLOR_WHEEL
    vm.toggleColorWheelHex(w[0].hex)
    vm.toggleColorWheelHex(w[4].hex)
    vm.toggleColorWheelHex(w[9].hex)
    vm.toggleColorWheelHex(w[12].hex)
    assertEquals(4, vm.uiState.value.selectedColorWheelHexes.size)

    // Trying to add a 5th respects max-4 cap
    vm.toggleColorWheelHex(w[17].hex)
    assertEquals(4, vm.uiState.value.selectedColorWheelHexes.size)
    assertEquals(false, vm.uiState.value.selectedColorWheelHexes.contains(w[17].hex.uppercase()))

    // 4. Tapping an already-selected hex deselects it cleanly
    vm.toggleColorWheelHex(w[4].hex)
    assertEquals(3, vm.uiState.value.selectedColorWheelHexes.size)
    assertEquals(false, vm.uiState.value.selectedColorWheelHexes.contains(w[4].hex.uppercase()))

    // 5. Reset returns to empty (0 selected)
    vm.clearColorWheelSelection()
    assertTrue(vm.uiState.value.selectedColorWheelHexes.isEmpty())
  }

  @Test
  fun `test three map decomposition recolor and hardware visual output split`() {
    val sourceFrame = SampleSpriteSheets.createCyberNinjaSheet().frames.first()
    val layers = com.example.model.masterpack.ThreeMapRecolorEngine.decomposeSpriteToThreeMaps(sourceFrame)

    // 1. All 4 maps are non-null and match source frame dimensions
    assertNotNull(layers.colorIdMap)
    assertNotNull(layers.inkShadowMap)
    assertNotNull(layers.matEmissiveMap)
    assertNotNull(layers.depthNormMap)
    assertEquals(sourceFrame.width, layers.colorIdMap.width)
    assertEquals(sourceFrame.height, layers.colorIdMap.height)
    assertEquals(sourceFrame.width, layers.inkShadowMap.width)
    assertEquals(sourceFrame.height, layers.inkShadowMap.height)
    assertEquals(sourceFrame.width, layers.matEmissiveMap.width)
    assertEquals(sourceFrame.height, layers.matEmissiveMap.height)
    assertEquals(sourceFrame.width, layers.depthNormMap.width)
    assertEquals(sourceFrame.height, layers.depthNormMap.height)
    assertTrue(layers.regionBaseColors.isNotEmpty())

    // 2. Compare 0-color baseline vs 2-color wheel swap
    val baseline = com.example.model.masterpack.ThreeMapRecolorEngine.compositeRecoloredSprite(
      layers = layers,
      selectedWheelHexes = emptyList(),
      tier = com.example.model.masterpack.HardwareVisualOutputTier.ARCADE_32BIT_60FPS
    )
    val recolored = com.example.model.masterpack.ThreeMapRecolorEngine.compositeRecoloredSprite(
      layers = layers,
      selectedWheelHexes = listOf("#E11D48", "#FACC15"),
      tier = com.example.model.masterpack.HardwareVisualOutputTier.ARCADE_32BIT_60FPS
    )
    assertEquals(sourceFrame.width, recolored.width)
    assertEquals(sourceFrame.height, recolored.height)

    val count = sourceFrame.width * sourceFrame.height
    val basePx = IntArray(count)
    val recPx = IntArray(count)
    val inkPx = IntArray(count)
    baseline.getPixels(basePx, 0, sourceFrame.width, 0, 0, sourceFrame.width, sourceFrame.height)
    recolored.getPixels(recPx, 0, sourceFrame.width, 0, 0, sourceFrame.width, sourceFrame.height)
    layers.inkShadowMap.getPixels(inkPx, 0, sourceFrame.width, 0, 0, sourceFrame.width, sourceFrame.height)

    var changedPixels = 0
    var checkedInkPixels = 0
    for (i in 0 until count) {
      if (basePx[i] != recPx[i]) changedPixels++
      if (android.graphics.Color.alpha(inkPx[i]) >= 220) {
        checkedInkPixels++
        val r = android.graphics.Color.red(recPx[i])
        val g = android.graphics.Color.green(recPx[i])
        val b = android.graphics.Color.blue(recPx[i])
        assertTrue("Ink pixel must remain dark after recolor (got rgb=$r,$g,$b)", r <= 40 && g <= 40 && b <= 45)
      }
    }
    assertTrue("Recoloring with 2 wheel colors must produce a distinct bitmap", changedPixels > 0)
    assertTrue("Expected ink pixels to be verified dark", checkedInkPixels > 0)

    // 3. Hardware visual output split compiles all 4 tiers + valid master_pack_manifest.json
    val ps1 = ConsoleHardwareCatalog.getById("ps1")
    val rig = com.example.model.masterpack.RigMetadata.defaultForSprite(
      name = "Cyber Blade Ninja",
      width = sourceFrame.width,
      height = sourceFrame.height,
      frameCount = 12
    )
    val splitOutputs = com.example.model.masterpack.FootAlignedSlicerEngine.compileHardwareVisualSplit(
      layers = layers,
      selectedWheelHexes = listOf("#E11D48", "#FACC15"),
      targetHardware = ps1,
      rigMeta = rig
    )
    assertEquals(4, splitOutputs.size)
    assertEquals(
      com.example.model.masterpack.HardwareVisualOutputTier.entries.toSet(),
      splitOutputs.map { it.tier }.toSet()
    )
    splitOutputs.forEach { out ->
      assertEquals(sourceFrame.width, out.bitmap.width)
      assertEquals(sourceFrame.height, out.bitmap.height)
      assertTrue(out.paletteColorCount > 0)
    }

    val manifestJson = com.example.model.masterpack.FootAlignedSlicerEngine.buildMasterPackManifestJson(
      assetName = "Cyber Blade Ninja",
      gameTag = "MPV In-Universe MMO",
      groupTag = "Protagonists",
      characterTag = "Kaelen",
      selectedWheelHexes = listOf("#E11D48", "#FACC15"),
      rigMeta = rig,
      targetHardware = ps1,
      outputs = splitOutputs
    )
    val parsedManifest = org.json.JSONObject(manifestJson)
    val visualLayers = parsedManifest.getJSONObject("visualLayers")
    assertTrue(visualLayers.getString("colorIdMap").endsWith("color_id.png"))
    assertTrue(visualLayers.getString("inkShadowMap").endsWith("ink_shadow.png"))
    assertTrue(visualLayers.getString("matEmissiveMap").endsWith("mat_emissive.png"))
    assertTrue(visualLayers.getString("depthNormMap").endsWith("depth_norm.png"))
    assertNotNull(parsedManifest.getJSONObject("rig_meta"))
    assertEquals(4, parsedManifest.getJSONArray("compiledHardwareOutputs").length())
  }

  @Test
  fun `test foot aligned slicer eliminates vertical jitter and rig meta json round trip`() {
    // 1. Construct a 2-frame sprite sheet (2x1, each frame 24x24) with intentional vertical jitter:
    //    Frame 0 opaque pixels sit at y = 2..10; Frame 1 opaque pixels sit at y = 14..22
    val atlas = android.graphics.Bitmap.createBitmap(48, 24, android.graphics.Bitmap.Config.ARGB_8888)
    atlas.eraseColor(android.graphics.Color.TRANSPARENT)

    // Frame 0 (x in 6..15, y in 2..10)
    for (y in 2..10) {
      for (x in 6..15) {
        atlas.setPixel(x, y, android.graphics.Color.rgb(0, 229, 255))
      }
    }
    // Frame 1 (x in 24+10..24+19, y in 14..22)
    for (y in 14..22) {
      for (x in 34..43) {
        atlas.setPixel(x, y, android.graphics.Color.rgb(255, 42, 133))
      }
    }

    val jitterySheet = com.example.model.sprite.SpriteSheetData.fromBitmap(
      bitmap = atlas,
      name = "Jittery Test Walker",
      columns = 2,
      rows = 1
    )

    val result = com.example.model.masterpack.FootAlignedSlicerEngine.sliceAndFootAlignSheet(
      sheet = jitterySheet,
      cadenceFps = com.example.model.masterpack.FrameCadenceFps.FPS_30,
      paddingPx = 2
    )
    assertEquals(2, result.framesAlignedCount)
    assertEquals(2, result.alignedSheet.frames.size)

    fun bottomMostOpaqueY(bmp: android.graphics.Bitmap): Int {
      var maxY = -1
      for (y in 0 until bmp.height) {
        for (x in 0 until bmp.width) {
          if (android.graphics.Color.alpha(bmp.getPixel(x, y)) >= 24) {
            if (y > maxY) maxY = y
          }
        }
      }
      return maxY
    }

    val bottom0 = bottomMostOpaqueY(result.alignedSheet.frames[0])
    val bottom1 = bottomMostOpaqueY(result.alignedSheet.frames[1])
    assertTrue("Expected opaque pixels in aligned frame 0", bottom0 >= 0)
    assertEquals("Both foot-aligned frames must share the exact same bottom-most opaque pixel Y", bottom0, bottom1)

    // 2. RigMetadata JSON round-trip preserves cadence, pivot, boxes, and all 7 anchor types
    val rigJson = result.rigMetadata.toJsonString()
    val roundTripped = com.example.model.masterpack.RigMetadata.fromJsonString(rigJson)
    assertEquals(result.rigMetadata.assetName, roundTripped.assetName)
    assertEquals(com.example.model.masterpack.FrameCadenceFps.FPS_30, roundTripped.cadenceFps)
    assertEquals(result.rigMetadata.feetPivotYNorm, roundTripped.feetPivotYNorm, 0.001f)
    assertEquals(result.rigMetadata.hurtbox.xNorm, roundTripped.hurtbox.xNorm, 0.001f)
    assertEquals(result.rigMetadata.hurtbox.wNorm, roundTripped.hurtbox.wNorm, 0.001f)
    assertEquals(result.rigMetadata.hitbox.xNorm, roundTripped.hitbox.xNorm, 0.001f)
    assertEquals(result.rigMetadata.hitbox.hNorm, roundTripped.hitbox.hNorm, 0.001f)
    assertEquals(7, roundTripped.anchors.size)
    com.example.model.masterpack.AnchorPointType.entries.forEach { anchorType ->
      val orig = result.rigMetadata.anchors[anchorType]!!
      val parsed = roundTripped.anchors[anchorType]!!
      assertEquals(orig.xNorm, parsed.xNorm, 0.001f)
      assertEquals(orig.yNorm, parsed.yNorm, 0.001f)
    }
  }

  @Test
  fun `test audio synth soundchip dsp split wav round trip and audio asset resolver`() {
    // 1. Procedural cry & score sequence synthesis + waveform peaks in 0f..1f
    val crySpec = com.example.model.audio.ProceduralCrySpec(
      speciesKey = "volt_shinobi_test",
      baseFreqHz = 680f,
      endFreqHz = 1120f,
      durationMs = 340,
      modulationHz = 24f,
      noiseMix = 0.16f
    )
    val cryPcm = com.example.model.audio.AudioSynthAndDspEngine.synthesizeProceduralCryPcm(crySpec, 22050)
    assertTrue("Procedural cry PCM must be non-empty", cryPcm.isNotEmpty())
    val cryPeaks = com.example.model.audio.AudioSynthAndDspEngine.computeWaveformPeaks(cryPcm, bucketCount = 48)
    assertEquals(48, cryPeaks.size)
    assertTrue("All waveform peaks must be normalized to 0f..1f", cryPeaks.all { it in 0f..1f })
    assertTrue("Cry waveform should have audible energy", cryPeaks.maxOrNull()!! > 0.2f)

    val scoreSpec = com.example.model.audio.ScoreSequenceSpec(
      title = "Obsidian Synth Test",
      bpm = 140,
      notes = listOf(
        com.example.model.audio.ScoreSequenceNote(0.0f, 0.5f, 64, 0.85f, 0),
        com.example.model.audio.ScoreSequenceNote(0.5f, 0.5f, 67, 0.90f, 0),
        com.example.model.audio.ScoreSequenceNote(0.0f, 1.0f, 40, 0.88f, 1),
        com.example.model.audio.ScoreSequenceNote(0.0f, 1.0f, 76, 0.80f, 2)
      )
    )
    val scorePcm = com.example.model.audio.AudioSynthAndDspEngine.synthesizeScoreSequencePcm(scoreSpec, 22050)
    assertTrue("Score sequence PCM must be non-empty", scorePcm.isNotEmpty())

    // 2. Run all 8 SoundchipDspProfiles and verify DMG_GB_4CH differs from MSU1_CD_PCM
    val outputsByProfile = mutableMapOf<com.example.model.audio.SoundchipDspProfile, ShortArray>()
    com.example.model.audio.SoundchipDspProfile.entries.forEach { profile ->
      val dspPcm = com.example.model.audio.AudioSynthAndDspEngine.applyHardwareSoundchipDsp(
        pcm = cryPcm,
        inputRateHz = 22050,
        profile = profile
      )
      assertTrue("DSP output for ${profile.name} must be non-empty", dspPcm.isNotEmpty())
      outputsByProfile[profile] = dspPcm
    }
    assertEquals(8, outputsByProfile.size)
    val dmgPcm = outputsByProfile[com.example.model.audio.SoundchipDspProfile.DMG_GB_4CH]!!
    val msu1Pcm = outputsByProfile[com.example.model.audio.SoundchipDspProfile.MSU1_CD_PCM]!!
    assertTrue(
      "Game Boy DMG 4-bit decimated DSP must differ from lossless MSU-1 PCM",
      !dmgPcm.contentEquals(msu1Pcm)
    )

    // 3. WAV encode -> decode round-trip preserves sample rate & length
    val wavBytes = com.example.model.audio.AudioSynthAndDspEngine.encodePcmToWavBytes(cryPcm, 22050)
    val decoded = com.example.model.audio.AudioSynthAndDspEngine.decodeWavBytesToPcm(wavBytes)
    assertNotNull("WAV round-trip decode must succeed", decoded)
    assertEquals(22050, decoded!!.second)
    assertEquals(cryPcm.size, decoded.first.size)

    // 4. ProceduralCrySpec & ScoreSequenceSpec JSON round-trips
    val parsedCry = com.example.model.audio.ProceduralCrySpec.fromJsonString(crySpec.toJsonString())
    assertEquals(crySpec.speciesKey, parsedCry.speciesKey)
    assertEquals(crySpec.baseFreqHz, parsedCry.baseFreqHz, 0.01f)
    assertEquals(crySpec.endFreqHz, parsedCry.endFreqHz, 0.01f)
    assertEquals(crySpec.durationMs, parsedCry.durationMs)
    assertEquals(crySpec.modulationHz, parsedCry.modulationHz, 0.01f)
    assertEquals(crySpec.noiseMix, parsedCry.noiseMix, 0.01f)

    val parsedScore = com.example.model.audio.ScoreSequenceSpec.fromJsonString(scoreSpec.toJsonString())
    assertEquals(scoreSpec.title, parsedScore.title)
    assertEquals(scoreSpec.bpm, parsedScore.bpm)
    assertEquals(scoreSpec.notes.size, parsedScore.notes.size)
    assertEquals(scoreSpec.notes[2].midiNote, parsedScore.notes[2].midiNote)

    // 5. AudioAssetResolver resolves bound slots vs deterministic ProceduralFallbackCry
    val manifest = com.example.model.vault.SkinManifest(
      skinId = "obsidian-audio-skin",
      displayName = "Obsidian Audio Pack",
      slotBindings = mapOf("bgm_boss" to "vault_store/abc123.wav")
    )
    val resolver = com.example.model.audio.AudioAssetResolver(manifest)
    val bound = resolver.resolve("bgm_boss")
    assertTrue(bound is com.example.model.audio.ResolvedAudioAsset.BoundAudio)
    val boundAudio = bound as com.example.model.audio.ResolvedAudioAsset.BoundAudio
    assertEquals("vault_store/abc123.wav", boundAudio.assetPath)
    assertEquals(com.example.model.audio.AudioAssetFormat.WAV_PCM, boundAudio.format)
    assertEquals("obsidian-audio-skin", boundAudio.skinId)

    val fallback = resolver.resolve("missing_species_cry")
    assertTrue(fallback is com.example.model.audio.ResolvedAudioAsset.ProceduralFallbackCry)
    val fallbackCry = fallback as com.example.model.audio.ResolvedAudioAsset.ProceduralFallbackCry
    assertEquals("missing_species_cry", fallbackCry.fallbackCrySpec.speciesKey)
    assertTrue(fallbackCry.fallbackCrySpec.durationMs >= 200)
  }

  @Test
  fun `test six stage importer magic byte validation sha256 dedup sensitive blur and skin resolve`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Build a real valid PNG in memory
    val bmp = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
    for (y in 0 until 32) {
      for (x in 0 until 32) {
        val color = if ((x + y) % 2 == 0) android.graphics.Color.rgb(0, 229, 255) else android.graphics.Color.rgb(255, 42, 133)
        bmp.setPixel(x, y, color)
      }
    }
    val pngOut = java.io.ByteArrayOutputStream()
    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, pngOut)
    val validPngBytes = pngOut.toByteArray()

    // 1. Valid PNG passes all 6 stages, writes to filesDir/vault_store, and resolves via AssetResolver
    val (firstResult, updatedManifest) = com.example.model.vault.SixStageAssetImporter.importRawBytes(
      context = context,
      rawBytes = validPngBytes,
      fileName = "kaelen_hero.png",
      slotName = "hero_idle_sheet",
      sensitive = false,
      gameTag = "MPV In-Universe MMO",
      groupTag = "Protagonists",
      characterTag = "Kaelen"
    )
    assertTrue("Expected valid PNG 6-stage import to succeed: ${firstResult.errorMessage}", firstResult.success)
    assertEquals(6, firstResult.stages.size)
    assertTrue("All 6 stages must pass", firstResult.stages.all { it.passed })
    assertFalse("First import should not be marked deduplicated", firstResult.wasDeduplicated)
    val entry = firstResult.entry!!
    val storedFile = java.io.File(entry.localFilePath)
    assertTrue("Stored asset file must exist on disk", storedFile.exists())
    assertTrue("Stored asset must reside inside vault_store", storedFile.canonicalPath.contains("vault_store"))

    val resolver = com.example.model.vault.AssetResolver(updatedManifest)
    val resolved = resolver.resolve("hero_idle_sheet")
    assertTrue("Named slot must resolve to BoundAsset", resolved is com.example.model.vault.ResolvedAsset.BoundAsset)
    assertEquals("vault_store/${entry.sha256Hex}.png", (resolved as com.example.model.vault.ResolvedAsset.BoundAsset).assetPath)

    // 2. Re-importing identical bytes triggers SHA-256 deduplication
    val (dedupResult, _) = com.example.model.vault.SixStageAssetImporter.importRawBytes(
      context = context,
      rawBytes = validPngBytes,
      fileName = "kaelen_hero_copy.png",
      slotName = "hero_idle_sheet_alt",
      sensitive = false,
      existingCatalog = listOf(entry),
      activeSkinManifest = updatedManifest
    )
    assertTrue(dedupResult.success)
    assertTrue("Identical bytes must trigger SHA-256 deduplication", dedupResult.wasDeduplicated)
    assertEquals(entry.sha256Hex, dedupResult.entry!!.sha256Hex)

    // 3. Spoofed PNG fails Stage 2 VALIDATE cleanly
    val (spoofResult, _) = com.example.model.vault.SixStageAssetImporter.importRawBytes(
      context = context,
      rawBytes = "NOT_A_PNG".toByteArray(),
      fileName = "fake.png",
      slotName = "spoof_slot"
    )
    assertFalse("Spoofed PNG must be rejected", spoofResult.success)
    val validateStage = spoofResult.stages.firstOrNull { it.stage == com.example.model.vault.ImportPipelineStage.VALIDATE }
    assertNotNull("Stage 2 VALIDATE record must be present", validateStage)
    assertFalse("Stage 2 VALIDATE must fail on spoofed magic bytes", validateStage!!.passed)

    // 4. Sensitive import creates a privacy-blurred thumbnail cleanly on disk
    val (sensitiveResult, _) = com.example.model.vault.SixStageAssetImporter.importRawBytes(
      context = context,
      rawBytes = validPngBytes,
      fileName = "private_concept.png",
      slotName = "private_concept_slot",
      sensitive = true
    )
    assertTrue(sensitiveResult.success)
    val sensEntry = sensitiveResult.entry!!
    assertTrue(sensEntry.sensitive)
    assertTrue("Sensitive thumbnail path must be non-blank", sensEntry.thumbnailPath.isNotBlank())
    assertTrue("Sensitive thumbnail file must exist on disk", java.io.File(sensEntry.thumbnailPath).exists())
  }

  @Test
  fun `test aes256gcm vault enc round trip wrong passphrase rejection and lru hot cache eviction`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val secretPayload = "PixelForge AES-256-GCM Cold Vault Payload • Kaelen Master Pack".toByteArray(Charsets.UTF_8)
    val passphrase = "ObsidianFoundry!2026"

    // 1. Encrypt -> verify PFVAULT1 header -> decrypt round-trip
    val encrypted = com.example.model.vault.EncryptedColdVaultManager.encryptBytesToVaultEnc(secretPayload, passphrase)
    assertEquals("PFVAULT1", String(encrypted, 0, 8, Charsets.US_ASCII))
    val decrypted = com.example.model.vault.EncryptedColdVaultManager.decryptVaultEncToBytes(encrypted, passphrase)
    assertTrue("Decrypted bytes must match original plaintext", secretPayload.contentEquals(decrypted))

    // 2. Wrong passphrase throws GeneralSecurityException
    var wrongPassRejected = false
    try {
      com.example.model.vault.EncryptedColdVaultManager.decryptVaultEncToBytes(encrypted, "WrongPassphrase999")
    } catch (_: java.security.GeneralSecurityException) {
      wrongPassRejected = true
    }
    assertTrue("Wrong passphrase must throw GeneralSecurityException", wrongPassRejected)

    // 3. 1-bit tampered ciphertext throws GeneralSecurityException
    val tampered = encrypted.copyOf()
    tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0x01).toByte()
    var tamperRejected = false
    try {
      com.example.model.vault.EncryptedColdVaultManager.decryptVaultEncToBytes(tampered, passphrase)
    } catch (_: java.security.GeneralSecurityException) {
      tamperRejected = true
    }
    assertTrue("1-bit tampered ciphertext must fail GCM authentication", tamperRejected)

    // 4. LRU Hot Phone Cache eviction offloads oldest entry to .vault.enc & keeps newer entry CACHED_LOCAL
    val jsonOld = """{"asset":"old_entry","padding":"${"A".repeat(400)}"}""".toByteArray(Charsets.UTF_8)
    val jsonNew = """{"asset":"new_entry","padding":"${"B".repeat(400)}"}""".toByteArray(Charsets.UTF_8)

    val (oldImport, _) = com.example.model.vault.SixStageAssetImporter.importRawBytes(
      context = context,
      rawBytes = jsonOld,
      fileName = "old_spec.json",
      slotName = "old_slot"
    )
    val (newImport, _) = com.example.model.vault.SixStageAssetImporter.importRawBytes(
      context = context,
      rawBytes = jsonNew,
      fileName = "new_spec.json",
      slotName = "new_slot"
    )
    assertTrue(oldImport.success && newImport.success)

    val oldEntry = oldImport.entry!!.copy(lastAccessedMillis = 1_000L)
    val newEntry = newImport.entry!!.copy(lastAccessedMillis = 9_000L)
    val budgetCap = newEntry.byteSize + 32L // Fits 1 file cleanly, forces eviction of the older file

    val (trimmedCatalog, evictedCount) = com.example.model.vault.HotPhoneLruCacheManager.enforceLruBudget(
      context = context,
      catalog = listOf(oldEntry, newEntry),
      maxCacheBytes = budgetCap,
      passphrase = passphrase
    )
    assertEquals(1, evictedCount)
    val trimmedOld = trimmedCatalog.first { it.id == oldEntry.id }
    val trimmedNew = trimmedCatalog.first { it.id == newEntry.id }

    assertEquals(com.example.model.vault.VaultCacheState.REMOTE_ONLY, trimmedOld.cacheState)
    assertTrue("Evicted entry must have .vault.enc archive on disk", java.io.File(trimmedOld.coldVaultPath).exists())
    assertFalse("Evicted entry hot local file must be deleted", java.io.File(trimmedOld.localFilePath).exists())

    assertEquals(com.example.model.vault.VaultCacheState.CACHED_LOCAL, trimmedNew.cacheState)
    assertTrue("Newer entry hot local file must remain cached", java.io.File(trimmedNew.localFilePath).exists())

    // 5. Hydrating the evicted entry back from .vault.enc restores the exact file bytes
    val hydratedOld = com.example.model.vault.EncryptedColdVaultManager.hydrateEntryFromColdVault(
      context = context,
      entry = trimmedOld,
      passphrase = passphrase
    )
    assertEquals(com.example.model.vault.VaultCacheState.CACHED_LOCAL, hydratedOld.cacheState)
    assertTrue(java.io.File(hydratedOld.localFilePath).exists())
    assertTrue(jsonOld.contentEquals(java.io.File(hydratedOld.localFilePath).readBytes()))
  }

  @Test
  fun `test scratch sprite canvas engine blank creation mirror paint flood fill frames and export`() {
    val engine = com.example.model.sprite.SpriteCanvasEditorEngine

    // 1. Blank 16x16 2-frame project starts 100% transparent
    val blank = engine.createBlankProject(
      name = "Emerald Hatchling",
      width = 16,
      height = 16,
      frameCount = 2
    )
    assertEquals("Emerald Hatchling", blank.name)
    assertEquals(16, blank.width)
    assertEquals(16, blank.height)
    assertEquals(2, blank.frames.size)
    blank.frames.forEach { frame ->
      assertEquals(256, frame.size)
      assertTrue(
        "Every pixel in a blank scratch frame must start Color.TRANSPARENT",
        frame.all { it == android.graphics.Color.TRANSPARENT }
      )
    }

    // 2. Paint with mirrorX = true at (2, 4) -> paints both (2, 4) and (13, 4)
    val emerald = android.graphics.Color.rgb(16, 185, 129)
    val mirrored = engine.paintPixel(
      framePixels = blank.frames[0],
      width = 16,
      height = 16,
      x = 2,
      y = 4,
      colorInt = emerald,
      brushSize = 1,
      mirrorX = true
    )
    assertEquals(emerald, mirrored[4 * 16 + 2])
    assertEquals(emerald, mirrored[4 * 16 + 13])
    assertEquals(android.graphics.Color.TRANSPARENT, mirrored[4 * 16 + 3])

    // 3. Draw closed 6x6 border from x=5..10, y=5..10 and flood-fill interior at (7, 7)
    val borderColor = android.graphics.Color.rgb(15, 23, 42)
    val fillColor = android.graphics.Color.rgb(56, 189, 248)
    var boxed = blank.frames[0].copyOf()
    for (x in 5..10) {
      boxed = engine.paintPixel(boxed, 16, 16, x, 5, borderColor)
      boxed = engine.paintPixel(boxed, 16, 16, x, 10, borderColor)
    }
    for (y in 5..10) {
      boxed = engine.paintPixel(boxed, 16, 16, 5, y, borderColor)
      boxed = engine.paintPixel(boxed, 16, 16, 10, y, borderColor)
    }
    val filled = engine.floodFill(boxed, 16, 16, 7, 7, fillColor)

    // Verify interior (6..9, 6..9) is filled, border stays borderColor, exterior stays transparent
    for (y in 6..9) {
      for (x in 6..9) {
        assertEquals("Interior pixel ($x,$y) must be flood-filled", fillColor, filled[y * 16 + x])
      }
    }
    assertEquals(borderColor, filled[5 * 16 + 5])
    assertEquals(android.graphics.Color.TRANSPARENT, filled[0])
    assertEquals(android.graphics.Color.TRANSPARENT, filled[15 * 16 + 15])

    // 4. Duplicate active frame & export to SpriteSheetData
    val withDrawnFrame = blank.copy(frames = listOf(filled, blank.frames[1]), activeFrameIndex = 0)
    val duplicated = engine.duplicateActiveFrame(withDrawnFrame)
    assertEquals(3, duplicated.frames.size)
    assertEquals(1, duplicated.activeFrameIndex)
    assertTrue("Duplicated frame pixels must match original frame", duplicated.frames[1].contentEquals(filled))

    val sheet = engine.exportProjectToSpriteSheet(duplicated)
    assertEquals("CREATED FROM SCRATCH", sheet.sourceKindLabel)
    assertEquals("Emerald Hatchling", sheet.name)
    assertEquals(3, sheet.frames.size)
    assertEquals(16, sheet.frameWidth)
    assertEquals(16, sheet.frameHeight)
    assertEquals(fillColor, sheet.frames[0].getPixel(7, 7))
    assertEquals(fillColor, sheet.frames[1].getPixel(7, 7))
    assertEquals(android.graphics.Color.TRANSPARENT, sheet.frames[2].getPixel(7, 7))
  }

  @Test
  fun `test main viewmodel scratch canvas undo redo and commit to studio library and history`() {
    val vm = MainViewModel()
    val initialLibrarySize = vm.uiState.value.savedLibraryAssets.size

    // 1. Start a fresh blank 16x16 1-frame project
    vm.startNewBlankSpriteProject(name = "Original Dragon", sizePx = 16, frameCount = 1)
    assertEquals("Original Dragon", vm.uiState.value.scratchSpriteProject.name)
    assertFalse(vm.uiState.value.scratchCanUndo)
    assertFalse(vm.uiState.value.scratchCanRedo)

    // 2. Paint a pixel with undo snapshot enabled
    val goldColor = android.graphics.Color.rgb(245, 158, 11)
    vm.setPixelDrawTool(com.example.model.sprite.PixelDrawTool.PENCIL)
    vm.setBrushColorInt(goldColor)
    vm.setBrushSizePx(1)
    vm.applyCanvasStrokeAt(x = 4, y = 6, pushUndoSnapshot = true)

    assertEquals(goldColor, vm.uiState.value.scratchSpriteProject.activeFramePixels[6 * 16 + 4])
    assertTrue("scratchCanUndo must become true after stroke", vm.uiState.value.scratchCanUndo)
    assertFalse(vm.uiState.value.scratchCanRedo)

    // 3. Undo reverts canvas back to transparent and enables redo
    vm.undoScratchSprite()
    assertEquals(
      android.graphics.Color.TRANSPARENT,
      vm.uiState.value.scratchSpriteProject.activeFramePixels[6 * 16 + 4]
    )
    assertFalse(vm.uiState.value.scratchCanUndo)
    assertTrue("scratchCanRedo must become true after undo", vm.uiState.value.scratchCanRedo)

    // 4. Redo restores painted pixel
    vm.redoScratchSprite()
    assertEquals(goldColor, vm.uiState.value.scratchSpriteProject.activeFramePixels[6 * 16 + 4])
    assertTrue(vm.uiState.value.scratchCanUndo)
    assertFalse(vm.uiState.value.scratchCanRedo)

    // 5. Commit to Studio, Saved Library & Room History
    vm.commitScratchSpriteProjectToStudioAndVault()
    val state = vm.uiState.value
    assertEquals("Original Dragon", state.currentSpriteSheet.name)
    assertEquals("CREATED FROM SCRATCH", state.currentSpriteSheet.sourceKindLabel)
    assertEquals(initialLibrarySize + 1, state.savedLibraryAssets.size)
    assertTrue(
      "roomAssetHistory must contain an entry with sourceOrigin == SCRATCH 2D CANVAS",
      state.roomAssetHistory.any { it.sourceOrigin == "SCRATCH 2D CANVAS" && it.title == "Original Dragon" }
    )
  }

  @Test
  fun `test scratch 3d primitive builder blank scene shapes mirror x and obj export`() {
    val engine = com.example.model.model3d.Scratch3DBuilderEngine

    // 1. Blank scene starts with 0 parts and compiles to 0 vertices
    val blank = engine.createEmptyProject("Valkyrie Striker")
    assertEquals(0, blank.parts.size)
    val emptyMesh = engine.compileProjectToMesh3D(blank)
    assertEquals(0, emptyMesh.vertexCount)
    assertEquals(0, emptyMesh.faceCount)
    assertEquals("CREATED FROM SCRATCH", emptyMesh.sourceKindLabel)

    // 2. Add BOX, SPHERE, and mirrored WEDGE_WING (posX = 0.6f, mirrorX = true)
    var proj = engine.addPart(blank, com.example.model.model3d.PrimitiveShapeType.BOX, 0xFF00E5FF)
    proj = engine.addPart(proj, com.example.model.model3d.PrimitiveShapeType.SPHERE, 0xFFFF2A85)
    proj = engine.addPart(proj, com.example.model.model3d.PrimitiveShapeType.WEDGE_WING, 0xFF10B981)
    proj = engine.updateSelectedPart(proj) { it.copy(posX = 0.6f, mirrorX = true) }

    assertEquals(3, proj.parts.size)
    val compiled = engine.compileProjectToMesh3D(proj)
    assertEquals("CREATED FROM SCRATCH", compiled.sourceKindLabel)
    assertTrue("Compiled 3D mesh must have vertices", compiled.vertexCount > 30)
    assertTrue("Compiled 3D mesh must have faces", compiled.faceCount > 20)
    assertTrue("Mirrored mesh must have positive X vertices", compiled.vertices.any { it.x > 0.3f })
    assertTrue("Mirrored mesh must have negative X vertices", compiled.vertices.any { it.x < -0.3f })

    // 3. Wavefront OBJ export contains v and f records
    val objText = com.example.model.model3d.ObjParser.exportToObj(compiled)
    assertTrue("Exported OBJ must contain vertex rows", objText.contains("v "))
    assertTrue("Exported OBJ must contain face rows", objText.contains("f "))

    // 4. MainViewModel commit updates currentMesh, savedLibraryAssets, and roomAssetHistory
    val vm = MainViewModel()
    val initialLibCount = vm.uiState.value.savedLibraryAssets.size
    vm.startNewBlank3DProject("Valkyrie Striker")
    assertEquals(0, vm.uiState.value.scratch3DProject.parts.size)
    vm.addScratch3DPart(com.example.model.model3d.PrimitiveShapeType.BOX)
    vm.addScratch3DPart(com.example.model.model3d.PrimitiveShapeType.WEDGE_WING)
    vm.commitScratch3DProjectToStudioAndVault()

    val state = vm.uiState.value
    assertEquals("Valkyrie Striker", state.currentMesh.name)
    assertEquals("CREATED FROM SCRATCH", state.currentMesh.sourceKindLabel)
    assertEquals(initialLibCount + 1, state.savedLibraryAssets.size)
    assertTrue(
      "roomAssetHistory must record SCRATCH 3D BUILDER entry",
      state.roomAssetHistory.any { it.sourceOrigin == "SCRATCH 3D BUILDER" && it.title == "Valkyrie Striker" }
    )
  }

  @Test
  fun `test scratch texture engine mirror paint patterns and audio composer saves new tracks`() {
    val texEngine = com.example.model.texture.ScratchTextureEngine

    // 1. Blank 16x16 texture + 4-way quadrant mirror paint paints all 4 symmetrical corners cleanly
    val blankTex = texEngine.createBlankTexture("Quad Armor", sizePx = 16)
      .copy(mirrorX = true, mirrorY = true)
    val cyan = android.graphics.Color.rgb(0x00, 0xE5, 0xFF)
    val painted = texEngine.paintTexturePixel(blankTex, x = 1, y = 2, colorInt = cyan, brushSize = 1)

    assertEquals(cyan, painted.pixels[2 * 16 + 1])
    assertEquals(cyan, painted.pixels[2 * 16 + 14])
    assertEquals(cyan, painted.pixels[13 * 16 + 1])
    assertEquals(cyan, painted.pixels[13 * 16 + 14])

    // 2. All 6 ProceduralMaterialPatterns produce valid 128x128 bitmaps
    val p1 = android.graphics.Color.rgb(0x16, 0x1B, 0x26)
    val p2 = android.graphics.Color.rgb(0x33, 0x41, 0x55)
    val p3 = android.graphics.Color.rgb(0x10, 0xB9, 0x81)
    com.example.model.texture.ProceduralMaterialPattern.entries.forEach { pattern ->
      val proj = texEngine.generateMaterialPattern(
        name = "Test ${pattern.label}",
        sizePx = 32,
        pattern = pattern,
        primaryColorInt = p1,
        secondaryColorInt = p2,
        accentColorInt = p3
      )
      val bmp = texEngine.toExportBitmap(proj, outputDim = 128)
      assertEquals("Bitmap width for ${pattern.name}", 128, bmp.width)
      assertEquals("Bitmap height for ${pattern.name}", 128, bmp.height)
    }

    // 3. MainViewModel audio composer saves new cry and sequence entries with valid waveformPeaks (0f..1f)
    val vm = MainViewModel()
    val initialAudioCount = vm.uiState.value.audioVaultEntries.size

    val crySpec = com.example.model.audio.ProceduralCrySpec(
      speciesKey = "emerald_drake_custom",
      baseFreqHz = 540f,
      endFreqHz = 1240f,
      durationMs = 450,
      modulationHz = 22f,
      noiseMix = 0.20f
    )
    vm.saveBrandNewCryToAudioVault(
      title = "Emerald Drake Custom Cry",
      slotKey = "cry_emerald_drake_custom",
      spec = crySpec
    )

    val scoreSpec = com.example.model.audio.ScoreSequenceSpec(
      title = "Foundry Overdrive",
      bpm = 144,
      notes = listOf(
        com.example.model.audio.ScoreSequenceNote(0.0f, 0.25f, 60, 0.85f, 0),
        com.example.model.audio.ScoreSequenceNote(0.25f, 0.25f, 64, 0.85f, 0),
        com.example.model.audio.ScoreSequenceNote(0.50f, 0.25f, 67, 0.85f, 0),
        com.example.model.audio.ScoreSequenceNote(0.75f, 0.25f, 72, 0.90f, 2),
        com.example.model.audio.ScoreSequenceNote(0.0f, 0.50f, 48, 0.90f, 1)
      )
    )
    vm.saveBrandNewSequenceToAudioVault(
      title = "Foundry Overdrive",
      slotKey = "bgm_foundry_overdrive",
      spec = scoreSpec
    )

    val entries = vm.uiState.value.audioVaultEntries
    assertEquals(initialAudioCount + 2, entries.size)

    val savedCry = entries.firstOrNull { it.slotKey == "cry_emerald_drake_custom" }
    assertNotNull("Saved cry must exist in audioVaultEntries", savedCry)
    assertEquals(com.example.model.audio.AudioAssetFormat.AUDIO_CUE_JSON, savedCry!!.format)
    assertTrue("Saved cry waveformPeaks must be non-empty", savedCry.waveformPeaks.isNotEmpty())
    assertTrue("Saved cry peaks must be normalized 0f..1f", savedCry.waveformPeaks.all { it in 0f..1f })
    assertTrue("Saved cry must have non-zero audio energy", savedCry.waveformPeaks.any { it > 0.05f })

    val savedSeq = entries.firstOrNull { it.slotKey == "bgm_foundry_overdrive" }
    assertNotNull("Saved sequence must exist in audioVaultEntries", savedSeq)
    assertEquals(com.example.model.audio.AudioAssetFormat.SCORE_SEQ_JSON, savedSeq!!.format)
    assertTrue("Saved sequence waveformPeaks must be non-empty", savedSeq.waveformPeaks.isNotEmpty())
    assertTrue("Saved sequence peaks must be normalized 0f..1f", savedSeq.waveformPeaks.all { it in 0f..1f })
    assertTrue("Saved sequence must have non-zero audio energy", savedSeq.waveformPeaks.any { it > 0.05f })
  }

  @Test
  fun `test closed loop ai style gate auto audits and retries on style failure`() = kotlinx.coroutines.runBlocking {
    // 1. Create a failing 64x64 flat gray bitmap with an unstripped #FF00FF magenta border
    val failingBitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
    failingBitmap.eraseColor(android.graphics.Color.rgb(128, 128, 128))
    val magenta = android.graphics.Color.rgb(255, 0, 255)
    for (i in 0 until 64) {
      for (b in 0 until 3) {
        failingBitmap.setPixel(i, b, magenta)
        failingBitmap.setPixel(i, 63 - b, magenta)
        failingBitmap.setPixel(b, i, magenta)
        failingBitmap.setPixel(63 - b, i, magenta)
      }
    }

    // 2. Create a clean repaired 64x64 cel-shaded black-ink + cyan sprite on transparent background
    val repairedBitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
    repairedBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
    val inkBlack = android.graphics.Color.rgb(12, 12, 18)
    val neonCyan = android.graphics.Color.rgb(0, 229, 255)
    val highlightWhite = android.graphics.Color.rgb(245, 252, 255)
    for (y in 10 until 54) {
      for (x in 14 until 50) {
        val isBorder = x <= 16 || x >= 47 || y <= 12 || y >= 51
        val isHighlight = x in 20..28 && y in 18..26
        val color = when {
          isBorder -> inkBlack
          isHighlight -> highlightWhite
          else -> neonCyan
        }
        repairedBitmap.setPixel(x, y, color)
      }
    }

    // Verify failing bitmap triggers NEEDS_REVISION with chroma bleed & low score (< 68%)
    val failingAudit = com.example.ai.CanonicalAssetAndCodeAuditor.auditAsset(
      bitmap = failingBitmap,
      assetName = "Emerald Hatchling",
      promptUsed = "emerald dragon hatchling 32-bit arcade comic cel sprite",
      hardwareTier = com.example.ai.PromptHardwareTier.ARCADE_32BIT_60FPS,
      expectedChromaKeyHex = "#FF00FF"
    )
    assertTrue("Failing bitmap must flag chromaBleedDetected", failingAudit.chromaBleedDetected)
    assertTrue(
      "Failing bitmap score should be < 68f, got ${failingAudit.styleFidelityScorePercent}",
      failingAudit.styleFidelityScorePercent < 68f
    )
    assertEquals(com.example.ai.AssetFidelityTag.NEEDS_REVISION, failingAudit.fidelityTag)

    // 3. Run closed-loop style gate with 1-pass auto-repair callback
    var repairInvocations = 0
    var capturedDirective = ""
    val gateResult = com.example.ai.CanonicalAssetAndCodeAuditor.runClosedLoopStyleGate(
      initialBitmap = failingBitmap,
      assetName = "Emerald Hatchling",
      promptUsed = "emerald dragon hatchling 32-bit arcade comic cel sprite",
      hardwareTier = com.example.ai.PromptHardwareTier.ARCADE_32BIT_60FPS,
      expectedChromaKeyHex = "#FF00FF",
      referenceBitmap = null,
      allowSingleAutoRepair = true,
      repairGenerator = { directive ->
        repairInvocations++
        capturedDirective = directive
        repairedBitmap
      }
    )

    assertEquals("Auto-repair must trigger exactly once", 1, repairInvocations)
    assertTrue("Correction directive must target chroma cleanup", capturedDirective.contains("CHROMA", ignoreCase = true))
    assertTrue(gateResult.autoRepairAttempted)
    assertTrue(gateResult.autoRepairImprovedScore)
    assertEquals(repairedBitmap, gateResult.finalBitmap)
    assertEquals(false, gateResult.auditReport.chromaBleedDetected)
    assertTrue(
      "Repaired bitmap score should be >= 75f, got ${gateResult.auditReport.styleFidelityScorePercent}",
      gateResult.auditReport.styleFidelityScorePercent >= 75f
    )
    assertEquals(com.example.ai.AssetFidelityTag.PUBLIC_CLEAN_ROOM, gateResult.auditReport.fidelityTag)

    // 4. Clean bitmap passes on first attempt without invoking repairGenerator
    var cleanPassRepairs = 0
    val cleanGate = com.example.ai.CanonicalAssetAndCodeAuditor.runClosedLoopStyleGate(
      initialBitmap = repairedBitmap,
      assetName = "Emerald Hatchling Clean",
      promptUsed = "emerald dragon hatchling 32-bit arcade comic cel sprite",
      hardwareTier = com.example.ai.PromptHardwareTier.ARCADE_32BIT_60FPS,
      expectedChromaKeyHex = "#FF00FF",
      allowSingleAutoRepair = true,
      repairGenerator = {
        cleanPassRepairs++
        failingBitmap
      }
    )
    assertEquals(0, cleanPassRepairs)
    assertEquals(false, cleanGate.autoRepairAttempted)
    assertEquals(com.example.ai.AssetFidelityTag.PUBLIC_CLEAN_ROOM, cleanGate.auditReport.fidelityTag)
  }

  @Test
  fun `test byos workspace folder layout sync read round trip and config json contract`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Config JSON contract & SharedPreferences persistence round-trip
    val config = com.example.model.vault.ByosWorkspaceConfig(
      provider = com.example.model.vault.ByosStorageProvider.GOOGLE_DRIVE_APP_SCOPE,
      persistedTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AStudioSync",
      displayFolderName = "StudioSync",
      masterHubAccountEmail = "creator@mpvecosystem.dev",
      lastSyncEpochMs = 1764280000000L,
      autoSyncOnImport = true
    )
    val parsedConfig = com.example.model.vault.ByosWorkspaceConfig.fromJsonString(config.toJsonString())
    assertEquals(config, parsedConfig)
    assertTrue(parsedConfig.isMounted)

    com.example.model.vault.ByosCloudFolderManager.saveConfig(context, config)
    val loadedFromPrefs = com.example.model.vault.ByosCloudFolderManager.loadConfig(context)
    assertEquals(config, loadedFromPrefs)

    // 2. Directory layout creation, push (vault_catalog.json + active_skin.json + .vault.enc), and read round-trip
    val tempRoot = java.io.File(context.cacheDir, "byos_test_root_${System.currentTimeMillis()}")
    val localColdFile = java.io.File(context.cacheDir, "byos_sample_abc123.vault.enc")
    try {
      tempRoot.mkdirs()
      localColdFile.writeBytes("ENCRYPTED_COLD_VAULT_PAYLOAD".toByteArray())

      val sampleEntry = com.example.model.vault.VaultCatalogEntry(
        id = "vault_entry_001",
        title = "Kaelen Arcade Sprite",
        slotName = "hero_idle_sheet",
        assetKind = com.example.model.vault.VaultAssetKind.IMAGE,
        extension = "png",
        sha256Hex = "a1b2c3d4e5f60718293a4b5c6d7e8f90",
        byteSize = 4096L,
        localFilePath = "",
        coldVaultPath = localColdFile.absolutePath,
        cacheState = com.example.model.vault.VaultCacheState.REMOTE_ONLY,
        gameTag = "MPV In-Universe MMO",
        groupTag = "Protagonists",
        characterTag = "Kaelen"
      )

      val customSkin = com.example.model.vault.SkinManifest(
        skinId = "mpv-byos-test-skin",
        displayName = "MPV BYOS Synced Skin Pack",
        gameTag = "MPV In-Universe MMO",
        slotBindings = mapOf(
          "hero_idle_sheet" to "vault_entry_001",
          "cry_kaelen" to "audio_kaelen_01"
        )
      )

      val syncReport = com.example.model.vault.ByosCloudFolderManager.syncVaultToDirectory(
        rootDir = tempRoot,
        catalog = listOf(sampleEntry),
        activeSkin = customSkin,
        provider = com.example.model.vault.ByosStorageProvider.LOCAL_SAF_SYNCED_FOLDER
      )

      assertTrue("BYOS directory sync must succeed: ${syncReport.summary}", syncReport.success)
      assertEquals(4, syncReport.foldersVerified.size)
      assertEquals(3, syncReport.filesWritten)
      assertEquals(1, syncReport.coldVaultFilesFound)

      val ecoRoot = java.io.File(tempRoot, com.example.model.vault.ByosFolderLayout.ROOT_FOLDER)
      assertTrue(java.io.File(ecoRoot, "Vault/vault_catalog.json").isFile)
      assertTrue(java.io.File(ecoRoot, "Skins/active_skin.json").isFile)
      assertTrue(java.io.File(ecoRoot, "ColdVault/${localColdFile.name}").isFile)
      assertTrue(java.io.File(ecoRoot, "Exports").isDirectory)

      val readReport = com.example.model.vault.ByosCloudFolderManager.readWorkspaceFromDirectory(
        rootDir = tempRoot,
        provider = com.example.model.vault.ByosStorageProvider.LOCAL_SAF_SYNCED_FOLDER
      )

      assertTrue("BYOS directory read must succeed: ${readReport.summary}", readReport.success)
      assertEquals(4, readReport.foldersVerified.size)
      assertEquals(3, readReport.filesRead)
      assertEquals(1, readReport.catalogEntryCountFound)
      assertEquals(1, readReport.coldVaultFilesFound)
      assertNotNull("Active skin manifest must be read back", readReport.syncedSkinManifest)
      assertEquals("mpv-byos-test-skin", readReport.syncedSkinManifest?.skinId)
      assertEquals("vault_entry_001", readReport.syncedSkinManifest?.slotBindings?.get("hero_idle_sheet"))
    } finally {
      localColdFile.delete()
      tempRoot.deleteRecursively()
    }
  }

  @Test
  fun `test ai multi asset builder parses 3d blueprint texture cry and chiptune json safely`() {
    val builder = com.example.ai.AiMultiAssetBuilder

    // 1. Markdown-fenced 3D blueprint JSON with 3 parts (including mirrored WEDGE_WING & out-of-bounds values)
    val fenced3DJson = """
      Here is your 3D blueprint:
      ```json
      {
        "title": "Cyber Dragon Mech",
        "parts": [
          {
            "name": "Armored Torso",
            "shape": "BOX",
            "posX": 0.0, "posY": 0.0, "posZ": 0.0,
            "scaleX": 5.0, "scaleY": 0.85, "scaleZ": 0.6,
            "rotYDeg": 740.0,
            "colorHex": "#1E293B",
            "mirrorX": false
          },
          {
            "name": "Dragon Helm",
            "shape": "CONE",
            "posX": 0.0, "posY": 9.5, "posZ": 0.1,
            "scaleX": 0.01, "scaleY": 0.55, "scaleZ": 0.5,
            "rotYDeg": 0.0,
            "colorHex": "#00E5FF",
            "mirrorX": false
          },
          {
            "name": "Plasma Wing",
            "shape": "WEDGE_WING",
            "posX": 0.7, "posY": 0.2, "posZ": -0.1,
            "scaleX": 0.9, "scaleY": 0.25, "scaleZ": 0.65,
            "rotYDeg": -30.0,
            "colorHex": "#FF2A85",
            "mirrorX": true
          }
        ]
      }
      ```
    """.trimIndent()

    val project3D = builder.parse3DBlueprintJson("Cyber Dragon Mech", fenced3DJson)
    assertEquals("Cyber Dragon Mech", project3D.name)
    assertEquals(3, project3D.parts.size)
    // Confirm strict clamping
    assertEquals(1.8f, project3D.parts[0].scaleX, 0.001f)
    assertEquals(20.0f, project3D.parts[0].rotYDeg, 0.001f)
    assertEquals(1.5f, project3D.parts[1].posY, 0.001f)
    assertEquals(0.1f, project3D.parts[1].scaleX, 0.001f)
    assertTrue(project3D.parts[2].mirrorX)
    assertEquals(330.0f, project3D.parts[2].rotYDeg, 0.001f)

    val compiledMesh = com.example.model.model3d.Scratch3DBuilderEngine.compileProjectToMesh3D(project3D)
    assertTrue("Compiled 3D blueprint must have vertices", compiledMesh.vertices.isNotEmpty())
    assertTrue("Compiled 3D blueprint must have faces", compiledMesh.faces.isNotEmpty())

    // Malformed / empty parts JSON must throw IllegalArgumentException (never silent stock fallback!)
    var threwOnEmpty = false
    try {
      builder.parse3DBlueprintJson("Broken", """{"parts": []}""")
    } catch (_: IllegalArgumentException) {
      threwOnEmpty = true
    }
    assertTrue("Empty 3D blueprint JSON must throw IllegalArgumentException", threwOnEmpty)

    var threwOnGarbage = false
    try {
      builder.parse3DBlueprintJson("Broken", "not json at all")
    } catch (_: IllegalArgumentException) {
      threwOnGarbage = true
    }
    assertTrue("Non-JSON 3D blueprint must throw IllegalArgumentException", threwOnGarbage)

    // 2. Texture recipe JSON -> editable ScratchTextureProject & 128x128 bitmap
    val textureJson = """
      ```json
      {
        "title": "Obsidian Mana Plate",
        "pattern": "ENERGY_CIRCUIT",
        "primaryHex": "#0F172A",
        "secondaryHex": "#1E293B",
        "accentHex": "#00E5FF",
        "mirrorX": true,
        "mirrorY": false,
        "pixelAccents": [
          {"x": 4, "y": 4, "hex": "#FACC15"},
          {"x": 999, "y": -5, "hex": "#FF2A85"}
        ]
      }
      ```
    """.trimIndent()
    val texProject = builder.parseTextureRecipeJson("Obsidian Mana Plate", textureJson, sizePx = 32)
    assertEquals(32, texProject.sizePx)
    assertTrue(texProject.mirrorX)
    val texBmp = com.example.model.texture.ScratchTextureEngine.toExportBitmap(texProject, outputDim = 128)
    assertEquals(128, texBmp.width)
    assertEquals(128, texBmp.height)

    // 3. Procedural Cry JSON -> clamped ProceduralCrySpec & valid non-empty PCM
    val cryJson = """
      {
        "speciesKey": "crystal_phoenix",
        "baseFreqHz": 5000.0,
        "endFreqHz": 12.0,
        "durationMs": 420,
        "modulationHz": 24.0,
        "noiseMix": 0.25
      }
    """.trimIndent()
    val crySpec = builder.parseCrySpecJson("crystal_phoenix", cryJson)
    assertEquals(2000f, crySpec.baseFreqHz, 0.01f)
    assertEquals(80f, crySpec.endFreqHz, 0.01f)
    val cryPcm = com.example.model.audio.AudioSynthAndDspEngine.synthesizeProceduralCryPcm(crySpec, 22050)
    assertTrue("Cry PCM must be non-empty", cryPcm.isNotEmpty())
    assertTrue("Cry PCM must contain audible amplitude", cryPcm.any { kotlin.math.abs(it.toInt()) > 100 })

    // 4. 16-Step Chiptune JSON -> clamped ScoreSequenceSpec, PCM & normalized waveform peaks
    val chiptuneJson = """
      ```json
      {
        "title": "Boss Theme 140",
        "bpm": 260,
        "notes": [
          {"step": 0, "midiNote": 64, "channel": 0, "durationBeats": 0.25, "velocity": 0.85},
          {"step": 2, "midiNote": 67, "channel": 0, "durationBeats": 0.25, "velocity": 0.85},
          {"step": 4, "midiNote": 71, "channel": 2, "durationBeats": 0.50, "velocity": 0.80},
          {"step": 8, "midiNote": 48, "channel": 1, "durationBeats": 0.50, "velocity": 0.90},
          {"step": 12, "midiNote": 76, "channel": 0, "durationBeats": 0.50, "velocity": 0.95}
        ]
      }
      ```
    """.trimIndent()
    val scoreSpec = builder.parseScoreSequenceJson("Boss Theme 140", chiptuneJson)
    assertEquals(200, scoreSpec.bpm)
    assertEquals(5, scoreSpec.notes.size)
    val chipPcm = com.example.model.audio.AudioSynthAndDspEngine.synthesizeScoreSequencePcm(scoreSpec, 22050)
    val chipPeaks = com.example.model.audio.AudioSynthAndDspEngine.computeWaveformPeaks(chipPcm)
    assertTrue("Chiptune PCM must be non-empty", chipPcm.isNotEmpty())
    assertTrue("Chiptune waveformPeaks must be non-empty", chipPeaks.isNotEmpty())
    assertTrue("Chiptune peaks must be normalized 0..1", chipPeaks.all { it in 0f..1f })
    assertTrue("Chiptune peaks must contain non-zero energy", chipPeaks.any { it > 0.05f })
  }

  @Test
  fun `test omni ai provider capability checksheet covers all six providers`() {
    val matrix = com.example.ai.OmniAiProviderCapabilities.capabilityMatrix()
    assertEquals(6, matrix.size)
    assertEquals(
      com.example.ai.AiProviderType.entries.toSet(),
      matrix.map { it.provider }.toSet()
    )

    val byProvider = matrix.associateBy { it.provider }

    val local = byProvider[com.example.ai.AiProviderType.LOCAL_OPENAI_COMPAT]!!
    assertTrue("LOCAL_OPENAI_COMPAT must be marked isLocalSelfHosted", local.isLocalSelfHosted)
    assertTrue(local.supportsTextAndJsonBuild)

    val claude = byProvider[com.example.ai.AiProviderType.ANTHROPIC_CLAUDE]!!
    assertTrue("ANTHROPIC_CLAUDE must support Text/JSON builds", claude.supportsTextAndJsonBuild)
    assertTrue("ANTHROPIC_CLAUDE must support Vision audits", claude.supportsVisionAudit)
    assertEquals("ANTHROPIC_CLAUDE does not output 2D images natively", false, claude.supportsImage2D)
    assertEquals(false, claude.isLocalSelfHosted)

    val firebase = byProvider[com.example.ai.AiProviderType.FIREBASE_STUDIO]!!
    assertTrue(firebase.supportsTextAndJsonBuild)
    assertTrue(firebase.supportsVisionAudit)
    assertTrue(firebase.supportsImage2D)
    assertEquals(false, firebase.isLocalSelfHosted)

    val openRouter = byProvider[com.example.ai.AiProviderType.OPENROUTER]!!
    assertTrue(openRouter.supportsTextAndJsonBuild)
    assertTrue(openRouter.supportsVisionAudit)
    assertTrue(openRouter.supportsImage2D)
    assertEquals(false, openRouter.isLocalSelfHosted)
  }

  @Test
  fun `test target fps propagation and unified training dataset export`() {
    val vm = MainViewModel()

    // 1. Target FPS updates & clamping (999 -> 120, 0 -> 1, 60 -> 60)
    vm.updateTargetAssetFps(999)
    assertEquals(120, vm.uiState.value.targetAssetFps)
    assertEquals(com.example.model.masterpack.FrameCadenceFps.FPS_60, vm.uiState.value.activeRigMetadata.cadenceFps)

    vm.updateTargetAssetFps(0)
    assertEquals(1, vm.uiState.value.targetAssetFps)
    assertEquals(com.example.model.masterpack.FrameCadenceFps.FPS_12, vm.uiState.value.activeRigMetadata.cadenceFps)

    vm.updateTargetAssetFps(60)
    assertEquals(60, vm.uiState.value.targetAssetFps)
    assertEquals(60, vm.uiState.value.scratchSpriteProject.fps)
    assertEquals(com.example.model.masterpack.FrameCadenceFps.FPS_60, vm.uiState.value.activeRigMetadata.cadenceFps)

    // 2. PromptStudioEngine embeds 24 FPS cadence for GRID_SHEET_2X4 with compact500 <= 500 & zero "checkerboard"
    val sheetPrompts = com.example.ai.PromptStudioEngine.buildPrompts(
      subjectDescription = "armored emerald dragon guardian leaping through arcade flames",
      tier = com.example.ai.PromptHardwareTier.ARCADE_32BIT_60FPS,
      preset = com.example.ai.PromptAssetPreset.GRID_SHEET_2X4,
      targetFps = 24
    )
    assertTrue("positiveSpecOnly must include 24 FPS", sheetPrompts.positiveSpecOnly.contains("24 FPS"))
    assertTrue("natural4Sentence must include 24 FPS", sheetPrompts.natural4Sentence.contains("24 FPS"))
    assertTrue("compact500 must be <= 500 chars", sheetPrompts.compact500.length <= 500)
    assertEquals(false, sheetPrompts.positiveSpecOnly.contains("checkerboard", ignoreCase = true))
    assertEquals(false, sheetPrompts.natural4Sentence.contains("checkerboard", ignoreCase = true))
    assertEquals(false, sheetPrompts.compact500.contains("checkerboard", ignoreCase = true))

    // 3. LoRA Caption Manifest JSON export
    val sampleGallery = com.example.data.local.AssetGalleryWithItems(
      gallery = com.example.data.local.AssetGalleryEntity(
        id = 7L,
        name = "Arcade Heroes",
        description = "Test Exhibition"
      ),
      items = listOf(
        com.example.data.local.AssetGalleryItemEntity(
          id = 11L,
          galleryId = 7L,
          title = "Kaelen Sprint",
          characterTag = "Kaelen",
          assetType = "SPRITE_2D",
          sourceOrigin = "STUDIO",
          hardwareTarget = "PS1",
          metricsSummary = "8 frames",
          spriteColumns = 4,
          spriteRows = 2,
          gameTag = "MPV In-Universe MMO",
          groupTag = "Protagonists"
        )
      )
    )
    val loraJson = com.example.ai.AiTrainingDatasetExporter.buildLoraCaptionManifestJson(
      gallery = sampleGallery,
      triggerToken = "mpv_cel_v2",
      targetFps = 24,
      tier = com.example.ai.PromptHardwareTier.TRUE_FUSION_HD
    )
    val loraObj = org.json.JSONObject(loraJson)
    assertEquals("pixelforge.lora_dataset.v1", loraObj.getString("schema"))
    assertEquals("mpv_cel_v2", loraObj.getString("triggerToken"))
    assertEquals(24, loraObj.getInt("targetFps"))
    val entries = loraObj.getJSONArray("entries")
    assertEquals(1, entries.length())
    val firstEntry = entries.getJSONObject(0)
    assertTrue(firstEntry.getString("captionText").contains("mpv_cel_v2"))
    assertTrue(firstEntry.getString("captionText").contains("24 FPS"))
    assertEquals("MPV In-Universe MMO", firstEntry.getString("gameTag"))

    // 4. Fine-Tune JSONL export (valid JSON with messages array on every line)
    val sample3D = com.example.model.model3d.Scratch3DBuilderEngine.addPart(
      com.example.model.model3d.Scratch3DBuilderEngine.createEmptyProject("Test Mech"),
      com.example.model.model3d.PrimitiveShapeType.BOX,
      0xFF00E5FF
    )
    val sampleTex = com.example.model.texture.ScratchTextureEngine.createBlankTexture("Test Plate", 16)
    val jsonl = com.example.ai.AiTrainingDatasetExporter.buildJsonlFineTuneDataset(
      project3D = sample3D,
      textureProject = sampleTex,
      audioEntries = vm.uiState.value.audioVaultEntries,
      targetFps = 30
    )
    val jsonlLines = jsonl.lineSequence().filter { it.isNotBlank() }.toList()
    assertTrue("JSONL must contain multiple training lines", jsonlLines.size >= 3)
    jsonlLines.forEach { line ->
      val obj = org.json.JSONObject(line)
      val msgs = obj.getJSONArray("messages")
      assertEquals(3, msgs.length())
      assertEquals("system", msgs.getJSONObject(0).getString("role"))
      assertEquals("user", msgs.getJSONObject(1).getString("role"))
      assertEquals("assistant", msgs.getJSONObject(2).getString("role"))
    }

    // 5. Pinning active asset adds an enabled source to knowledgeSources
    val beforeCount = vm.uiState.value.knowledgeSources.count { it.enabled }
    vm.pinActiveAssetAsAiTrainingExample()
    val afterCount = vm.uiState.value.knowledgeSources.count { it.enabled }
    assertTrue("Pinning active asset must add an enabled knowledge source", afterCount > beforeCount)
  }

  @Test
  fun `test forge chat prompt builder and unified user training profile sync`() {
    // 1. UserAiTrainingProfile JSON round-trip
    val profile = com.example.ai.UserAiTrainingProfile(
      styleTriggerToken = "mpv_truefusion",
      customLoraOrModelTag = "<lora:mpv_cel_32bit:0.85>",
      customTrainingNotes = "Always keep crisp 2-step cel shadows and glowing cyan Mana Circuits.",
      attachedTrainingFileCount = 2,
      applyToFormBuilder = true,
      applyToChatStudio = true
    )
    val roundTripped = com.example.ai.UserAiTrainingProfile.fromJsonString(profile.toJsonString())
    assertEquals(profile, roundTripped)

    // 2. applyTriggerToPrompt prepends without duplicating tokens already present
    val prepended = profile.applyTriggerToPrompt("cyber knight with plasma shield")
    assertTrue(prepended.startsWith("mpv_truefusion, <lora:mpv_cel_32bit:0.85>,"))
    val noDup = profile.applyTriggerToPrompt("mpv_truefusion cyber knight <lora:mpv_cel_32bit:0.85>")
    assertEquals("mpv_truefusion cyber knight <lora:mpv_cel_32bit:0.85>", noDup)

    // 3. parseImportedUserTrainingFile parses multi-line .jsonl while skipping malformed lines safely
    val mixedJsonl = """
      {"messages":[{"role":"system","content":"You are a sprite artist."},{"role":"user","content":"Make a dragon"},{"role":"assistant","content":"BUILD_PROMPT: emerald dragon sprite"}]}
      THIS IS NOT VALID JSON AT ALL
      {"messages":[{"role":"user","content":"Make a mech"},{"role":"assistant","content":"{\"parts\":[{\"shape\":\"BOX\"}]}"}]}
    """.trimIndent()
    val parsedTraining = com.example.ai.AiTrainingDatasetExporter.parseImportedUserTrainingFile(
      fileName = "custom_finetune.jsonl",
      rawText = mixedJsonl
    )
    assertTrue(parsedTraining.contains("Example #1"))
    assertTrue(parsedTraining.contains("Example #2"))
    assertTrue(parsedTraining.contains("1 invalid lines skipped"))

    // 4. ForgeChatPromptBuilder preserves multi-turn history, TARGET_FPS=30, and training context
    val history = listOf(
      com.example.ai.ForgeChatMessage(
        id = "m1",
        role = com.example.ai.ForgeChatRole.USER,
        text = "Let's design a stealth mech boss"
      ),
      com.example.ai.ForgeChatMessage(
        id = "m2",
        role = com.example.ai.ForgeChatRole.ASSISTANT,
        text = "Give it swept obsidian wings and glowing cyan thrusters."
      )
    )
    val convPrompt = com.example.ai.ForgeChatPromptBuilder.buildConversationPrompt(
      history = history,
      userMessage = "Now add twin dorsal plasma cannons",
      targetFps = 30,
      trainingProfile = profile,
      sharedContextBlock = "### [World Bible]\nAegis Grid faction colors: obsidian and cyan."
    )
    assertTrue(convPrompt.contains("TARGET_FPS=30"))
    assertTrue(convPrompt.contains("mpv_truefusion"))
    assertTrue(convPrompt.contains("<lora:mpv_cel_32bit:0.85>"))
    assertTrue(convPrompt.contains("Let's design a stealth mech boss"))
    assertTrue(convPrompt.contains("Now add twin dorsal plasma cannons"))

    // 5. extractSuggestedTargetAndPrompt detects 3D, Texture, Cry, Chiptune, and 2D cleanly
    val (t3d, p3d) = com.example.ai.ForgeChatPromptBuilder.extractSuggestedTargetAndPrompt(
      replyText = "Great silhouette!\nBUILD_PROMPT: Low-poly 3D model hover tank with twin turret cannons",
      fallbackUserPrompt = "tank"
    )
    assertEquals(com.example.ai.AiBuildTargetKind.MODEL_3D, t3d)
    assertTrue(p3d.contains("hover tank"))

    val (tTex, _) = com.example.ai.ForgeChatPromptBuilder.extractSuggestedTargetAndPrompt(
      replyText = "BUILD_PROMPT: Seamless tileable obsidian armor plate texture with cyan circuit lines",
      fallbackUserPrompt = "plate"
    )
    assertEquals(com.example.ai.AiBuildTargetKind.TEXTURE_MATERIAL, tTex)

    val (tCry, _) = com.example.ai.ForgeChatPromptBuilder.extractSuggestedTargetAndPrompt(
      replyText = "BUILD_PROMPT: Resonant crystal phoenix creature cry roar SFX with harmonic chirp",
      fallbackUserPrompt = "cry"
    )
    assertEquals(com.example.ai.AiBuildTargetKind.AUDIO_CRY, tCry)

    val (tChip, _) = com.example.ai.ForgeChatPromptBuilder.extractSuggestedTargetAndPrompt(
      replyText = "BUILD_PROMPT: Driving 145 BPM 16-step chiptune boss theme melody loop",
      fallbackUserPrompt = "music"
    )
    assertEquals(com.example.ai.AiBuildTargetKind.AUDIO_CHIPTUNE, tChip)

    val (t2d, _) = com.example.ai.ForgeChatPromptBuilder.extractSuggestedTargetAndPrompt(
      replyText = "BUILD_PROMPT: Emerald dragon hatchling 2x4 arcade hero sprite sheet with golden horns",
      fallbackUserPrompt = "dragon"
    )
    assertEquals(com.example.ai.AiBuildTargetKind.SPRITE_2D, t2d)
  }

  @Test
  fun `test master hub wiki parser json markdown relevance lookup and disk cache round trip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.ai.MasterHubWikiSyncEngine.clearCache(context)

    // 1. Parse sample world_bible.json with characters, factions, and species arrays
    val sampleWorldBibleJson = """
      {
        "worldName": "Aether Nexus Chronicles",
        "summary": "High-contrast 32-bit arcade sci-fantasy realm powered by Mana Circuits.",
        "characters": [
          {
            "id": "char_kaelen",
            "name": "Kaelen",
            "category": "Protagonist",
            "summary": "Vanguard courier of the Aegis Grid wielding a twin-phase plasma glaive.",
            "visualTraits": "Matte obsidian armor with glowing cyan Mana Circuit trim and asymmetric cape",
            "tags": ["aegis", "obsidian", "courier"]
          },
          {
            "id": "char_lyra",
            "name": "Lyra",
            "category": "Tactician",
            "summary": "Archivist commander coordinating orbital prism relays.",
            "visualTraits": "Ivory cloak, emerald visor lens, brass astrolabe staff",
            "tags": ["archivist", "prism"]
          }
        ],
        "factions": [
          {
            "id": "fac_aegis",
            "title": "Aegis Grid",
            "description": "Orbital defense guild safeguarding ancient star-forges.",
            "designNotes": "Hexagonal obsidian plating and electric cyan emblems",
            "tags": ["guild", "obsidian", "cyan"]
          }
        ],
        "species": [
          {
            "id": "sp_solarix",
            "name": "Solarix",
            "bio": "Solar-winged crystal drakes nesting on orbital spires.",
            "appearance": "Golden crystalline crest, translucent solar feathers",
            "traits": ["drake", "solar", "aerial"]
          }
        ]
      }
    """.trimIndent()

    val jsonEntries = com.example.ai.MasterHubWikiSyncEngine.parseAuto("world_bible.json", sampleWorldBibleJson)
    assertTrue("Expected at least 5 parsed JSON entries (primer + 2 chars + faction + species)", jsonEntries.size >= 5)
    assertTrue(jsonEntries.any { it.title == "Kaelen" && it.visualTraits.contains("obsidian armor") })
    assertTrue(jsonEntries.any { it.title == "Aegis Grid" })
    assertTrue(jsonEntries.any { it.title == "Solarix" })

    // 2. Parse sample MPV_01_LORE_AND_WIKI.md with ## and ### sections
    val sampleMarkdown = """
      # MPV Nexus World Bible
      Core archive of realms and iconic fighters.

      ## Characters
      ### Kaelen
      - Visual Traits: Matte obsidian armor, twin cyan visor glow, weathered crimson scarf.
      - Tags: #protagonist, #obsidian, #aegis
      Elite vanguard scout from Sector Nine.

      ### Vexra
      - Visual Traits: Chrome mantis carapace with magenta arc blades.
      Rogue automaton hunter.

      ## Factions
      ### Iron Vanguard
      Heavy mech corps patrolling the outer ring.
    """.trimIndent()

    val mdEntries = com.example.ai.MasterHubWikiSyncEngine.parseAuto("MPV_01_LORE_AND_WIKI.md", sampleMarkdown)
    assertTrue("Expected parsed Markdown sections", mdEntries.size >= 3)
    val mdKaelen = mdEntries.firstOrNull { it.title == "Kaelen" }
    assertNotNull("Kaelen should be parsed from Markdown", mdKaelen)
    assertEquals("Characters", mdKaelen!!.category)
    assertTrue(mdKaelen.visualTraits.contains("obsidian armor"))

    // 3. Prompt-aware relevance lookup ranks Kaelen first and includes visualTraits in [MASTER HUB WIKI CANON DETAILS]
    val relevantBlock = com.example.ai.MasterHubWikiSyncEngine.buildRelevantWikiContextBlock(
      entries = jsonEntries,
      activePromptOrChat = "Kaelen obsidian armor"
    )
    assertTrue(relevantBlock.startsWith("[MASTER HUB WIKI CANON DETAILS]"))
    val kaelenIdx = relevantBlock.indexOf("- Kaelen")
    val lyraIdx = relevantBlock.indexOf("- Lyra")
    assertTrue("Kaelen must appear in relevant block", kaelenIdx > 0)
    if (lyraIdx >= 0) {
      assertTrue("Kaelen must be ranked ahead of unrelated Lyra", kaelenIdx < lyraIdx)
    }
    assertTrue(relevantBlock.contains("Matte obsidian armor with glowing cyan Mana Circuit trim"))

    // 4. Disk cache saveCache -> loadCache round-trip to filesDir/master_hub_wiki_cache.json
    val cacheState = com.example.ai.MasterHubWikiCacheState(
      sourceLabel = "world_bible.json",
      wikiUrl = "https://raw.githubusercontent.com/Summer-Warrior/Nexus-Hub/main/world_bible.json",
      entries = jsonEntries,
      lastSyncedMillis = 1_730_000_000_000L,
      enabledForAiBase = true
    )
    com.example.ai.MasterHubWikiSyncEngine.saveCache(context, cacheState)
    val cacheFile = java.io.File(context.filesDir, com.example.ai.MasterHubWikiSyncEngine.CACHE_FILE_NAME)
    assertTrue("Disk cache file must exist in filesDir", cacheFile.exists())

    val reloaded = com.example.ai.MasterHubWikiSyncEngine.loadCache(context)
    assertEquals(cacheState.sourceLabel, reloaded.sourceLabel)
    assertEquals(cacheState.wikiUrl, reloaded.wikiUrl)
    assertEquals(cacheState.entries.size, reloaded.entries.size)
    assertEquals(true, reloaded.enabledForAiBase)
    assertTrue(reloaded.entries.any { it.title == "Kaelen" && it.visualTraits.contains("Mana Circuit") })
  }

  @Test
  fun `test adjustable layer 1 base modular layer 2 plugin slot and master hub wiki sync`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    java.io.File(context.filesDir, com.example.ai.MultiSourceContextRegistry.SOURCES_FILE_NAME).delete()
    val registry = com.example.ai.MultiSourceContextRegistry(context)

    // 1. Canonical Layer 1 & Micro-Digest checkerboard invariant (zero "checkerboard" before exclusions)
    val fullProfile = com.example.ai.DefaultAiArtTrainingProfiles.LAYER_1_CANONICAL_PROFILE
    val fullExclusionsIdx = fullProfile.indexOf("Strict Exclusions")
    assertTrue("LAYER_1_CANONICAL_PROFILE must contain Strict Exclusions", fullExclusionsIdx > 0)
    assertFalse(
      "LAYER_1_CANONICAL_PROFILE must not mention checkerboard before Strict Exclusions",
      fullProfile.substring(0, fullExclusionsIdx).lowercase().contains("checkerboard")
    )

    val microDigest = com.example.ai.DefaultAiArtTrainingProfiles.LAYER_1_MICRO_DIGEST
    val microNoIdx = microDigest.indexOf("No ")
    assertTrue("LAYER_1_MICRO_DIGEST must contain 'No ' exclusions", microNoIdx > 0)
    assertFalse(
      "LAYER_1_MICRO_DIGEST must not mention checkerboard before 'No '",
      microDigest.substring(0, microNoIdx).lowercase().contains("checkerboard")
    )
    assertTrue(
      "Micro digest must be compact (< 40% of full Layer 1 profile)",
      microDigest.length < (fullProfile.length * 0.40)
    )

    // 2. Fresh registry starts with Layer 1 at index 0, factory default, and EMPTY Layer 2 plug-in slot
    assertEquals(com.example.ai.DefaultAiArtTrainingProfiles.LAYER_1_SOURCE_ID, registry.getSources().first().id)
    assertEquals(false, registry.isBuiltInLayer1Customized())
    assertTrue("Layer 2 plug-in slot must start empty on fresh instance", registry.listLayer2Plugins().isEmpty())

    // 3. Prefix-stable buildCombinedContextBlock: [LAYER 1 BASE] -> [LAYER 2 PLUG-IN(S)] -> [USER SOURCES]
    val plugin = registry.attachLayer2Plugin(
      pluginName = "Aether Tactics Bible",
      content = "Use high-contrast emerald and obsidian armor silhouettes with 2-step cel shading."
    )
    assertEquals(1, registry.listLayer2Plugins().size)
    assertTrue(com.example.ai.DefaultAiArtTrainingProfiles.isLayer2PluginSource(plugin))

    val fullCombined = registry.buildCombinedContextBlock(
      maxTotalChars = 10_000,
      lowTokenMicroMode = false,
      queryHint = "emerald armor",
      activeProjectSummary = "Active Mesh: Cyber Vanguard"
    )
    assertTrue("Combined context must start with [LAYER 1 BASE]", fullCombined.startsWith("[LAYER 1 BASE]\n"))
    val l1Idx = fullCombined.indexOf("[LAYER 1 BASE]")
    val l2Idx = fullCombined.indexOf("[LAYER 2 PLUG-IN(S)]")
    val userIdx = fullCombined.indexOf("[USER SOURCES]")
    assertTrue("Layer 2 must follow Layer 1", l2Idx > l1Idx)
    assertTrue("User sources must follow Layer 2", userIdx > l2Idx)
    assertTrue(fullCombined.contains("Aether Tactics Bible"))

    // 4. Low-Token Micro Mode caps <= 1,400 chars and uses LAYER_1_MICRO_DIGEST
    val microCombined = registry.buildCombinedContextBlock(
      maxTotalChars = 10_000,
      lowTokenMicroMode = true,
      queryHint = "emerald armor",
      activeProjectSummary = "Active Mesh: Cyber Vanguard"
    )
    assertTrue("Low-token micro mode must cap to <= 1400 chars", microCombined.length <= 1_400)
    assertTrue("Low-token micro mode must start with [LAYER 1 BASE]", microCombined.startsWith("[LAYER 1 BASE]\n"))
    assertTrue(microCombined.contains("LAYER1_MICRO:"))

    // 5. Edit Layer 1 Base in-place, persist across reload, and restore factory default
    registry.updateBuiltInLayer1Content("Custom Layer 1 Studio Override")
    assertEquals(true, registry.isBuiltInLayer1Customized())
    assertEquals("Custom Layer 1 Studio Override", registry.getBuiltInLayer1Content())
    assertTrue(registry.buildCombinedContextBlock(lowTokenMicroMode = false).contains("Custom Layer 1 Studio Override"))

    val reloadedRegistry = com.example.ai.MultiSourceContextRegistry(context)
    assertEquals(true, reloadedRegistry.isBuiltInLayer1Customized())
    assertEquals(1, reloadedRegistry.listLayer2Plugins().size)
    assertTrue(reloadedRegistry.buildCombinedContextBlock(lowTokenMicroMode = false).contains("Custom Layer 1 Studio Override"))

    reloadedRegistry.resetBuiltInLayer1ToDefault()
    assertEquals(false, reloadedRegistry.isBuiltInLayer1Customized())
    val restoredBlock = reloadedRegistry.buildCombinedContextBlock(lowTokenMicroMode = false)
    assertTrue(restoredBlock.contains("4-MEDIUM BLEND"))
    assertEquals(false, restoredBlock.contains("Custom Layer 1 Studio Override"))
  }

  @Test
  fun `test auto picked offline mode explicit model gate trained model ranking and training aware offline forge`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Clear any saved explicit model preference
    com.example.ai.UserAiTrainingProfileStore.save(context, com.example.ai.UserAiTrainingProfile())

    // 1. Fresh ViewModel auto-picks Offline (0 Tokens) + Low-Token Micro Mode + NO auto-chosen Live model
    val vm = MainViewModel()
    assertTrue("Offline (0 Tokens) must be ON by default", vm.uiState.value.offlineZeroTokenMode)
    assertTrue("Low-Token Micro Prompt Mode must be ON by default", vm.uiState.value.lowTokenMicroPromptMode)
    assertFalse("No AI model may be auto-selected on boot", vm.uiState.value.userExplicitlySelectedAiModel)
    assertEquals("", vm.uiState.value.activeExplicitModelId)

    // 2. Attempting to switch to Live AI before explicitly selecting a model is cleanly blocked
    vm.toggleOfflineZeroTokenMode()
    assertTrue("Must remain in Offline mode until a model is explicitly chosen", vm.uiState.value.offlineZeroTokenMode)
    assertTrue(
      "Status message should instruct user to choose a model",
      vm.uiState.value.statusMessage.orEmpty().contains("Choose an AI model", ignoreCase = true)
    )

    // 3. Explicit model selection enables Live AI, and Clear Model returns cleanly to Auto-Offline
    vm.selectExplicitAiModel(com.example.ai.AiProviderType.LOCAL_OPENAI_COMPAT, "mpv-lora-qwen:7b")
    assertTrue(vm.uiState.value.userExplicitlySelectedAiModel)
    assertEquals("mpv-lora-qwen:7b", vm.uiState.value.activeExplicitModelId)
    assertFalse("Selecting an explicit model switches out of Offline mode", vm.uiState.value.offlineZeroTokenMode)

    vm.clearExplicitAiModelSelection()
    assertFalse(vm.uiState.value.userExplicitlySelectedAiModel)
    assertEquals("", vm.uiState.value.activeExplicitModelId)
    assertTrue("Clearing explicit model returns to Offline (0 Tokens) mode", vm.uiState.value.offlineZeroTokenMode)

    // 4. Local AI Presets are URL-only (zero fake/pre-filled default model)
    assertEquals(4, com.example.ai.LocalAiEndpointDiagnostics.LOCAL_AI_PRESETS.size)
    val ollamaPreset = com.example.ai.LocalAiEndpointDiagnostics.LOCAL_AI_PRESETS.first()
    vm.applyLocalAiPreset(ollamaPreset)
    val localProfile = com.example.ai.OmniAiRouter.getProviderProfile(com.example.ai.AiProviderType.LOCAL_OPENAI_COMPAT)
    assertEquals(ollamaPreset.baseUrl, localProfile.baseUrl)
    assertEquals("", localProfile.textModelId)
    assertTrue(vm.uiState.value.offlineZeroTokenMode)

    // 5. Real model discovery parser handles both OpenAI data[].id and Ollama models[].name
    val openAiModelsJson = """{"object":"list","data":[{"id":"llama3.2:3b"},{"id":"mpv-truefusion-qwen:7b"}]}"""
    val parsedOpenAi = com.example.ai.LocalAiEndpointDiagnostics.parseDiscoveredModelIds(openAiModelsJson)
    assertEquals(listOf("llama3.2:3b", "mpv-truefusion-qwen:7b"), parsedOpenAi)

    val ollamaTagsJson = """{"models":[{"name":"mistral:7b"},{"name":"mpv_cel_forge:latest"}]}"""
    val parsedOllama = com.example.ai.LocalAiEndpointDiagnostics.parseDiscoveredModelIds(ollamaTagsJson)
    assertEquals(listOf("mistral:7b", "mpv_cel_forge:latest"), parsedOllama)

    // 6. Discovered models matching UserAiTrainingProfile are ranked first with isUserTrainedMatch == true
    val trainedProfile = com.example.ai.UserAiTrainingProfile(
      styleTriggerToken = "mpv_truefusion",
      customLoraOrModelTag = "<lora:mpv_cel_forge:0.85>",
      explicitChosenProviderName = com.example.ai.AiProviderType.LOCAL_OPENAI_COMPAT.name,
      explicitChosenModelId = "mpv-truefusion-qwen:7b"
    )
    com.example.ai.UserAiTrainingProfileStore.save(context, trainedProfile)
    val reloadedProfile = com.example.ai.UserAiTrainingProfileStore.load(context)
    assertEquals("mpv-truefusion-qwen:7b", reloadedProfile.explicitChosenModelId)
    assertEquals(com.example.ai.AiProviderType.LOCAL_OPENAI_COMPAT.name, reloadedProfile.explicitChosenProviderName)

    val ranked = com.example.ai.LocalAiEndpointDiagnostics.rankDiscoveredModelsWithUserTrainingFirst(
      discoveredModels = listOf("llama3.2:3b", "qwen2.5:7b", "mpv-truefusion-qwen:7b"),
      trainingProfile = trainedProfile
    )
    assertEquals("mpv-truefusion-qwen:7b", ranked.first().first)
    assertTrue("Matched user-trained model must be flagged true", ranked.first().second)
    assertFalse("Unmatched base model must be flagged false", ranked.last().second)

    // 7. Training-Aware OfflineProceduralForge builds all 5 modalities + Director Chat at 0 tokens
    val hints = "obsidian armor with glowing cyan Mana Circuit trim and solar gold crest"
    val (spriteProj, spriteSheet) = com.example.ai.OfflineProceduralForge.buildOffline2DSpriteSheet(
      assetName = "Kaelen Offline",
      prompt = "emerald dragon knight with twin wings",
      sizePx = 32,
      frameCount = 4,
      targetFps = 24,
      trainingHints = hints
    )
    assertEquals(4, spriteProj.frames.size)
    assertEquals(4, spriteSheet.frames.size)
    assertEquals("OFFLINE PROCEDURAL 2D", spriteSheet.sourceKindLabel)
    assertTrue("Offline sprite must paint non-transparent pixels", spriteProj.frames[0].any { it != android.graphics.Color.TRANSPARENT })

    val (modelProj, mesh3D) = com.example.ai.OfflineProceduralForge.buildOffline3DModel(
      assetName = "Vanguard Mech",
      prompt = "twin-winged cyber dragon mech with visor",
      trainingHints = hints
    )
    assertTrue("Offline 3D model must contain multiple kitbash parts", modelProj.parts.size >= 4)
    assertTrue("Compiled offline mesh must have vertices and faces", mesh3D.vertexCount > 24 && mesh3D.faceCount > 16)
    assertEquals("OFFLINE PROCEDURAL 3D", mesh3D.sourceKindLabel)

    val texProj = com.example.ai.OfflineProceduralForge.buildOfflineTexture(
      assetName = "Circuit Plate",
      prompt = "obsidian mana circuit armor plate",
      sizePx = 32,
      trainingHints = hints
    )
    assertEquals(32, texProj.sizePx)
    assertEquals(1024, texProj.pixels.size)

    val (crySpec, cryEntry) = com.example.ai.OfflineProceduralForge.buildOfflineAudioCry(
      title = "Solar Drake Roar",
      prompt = "deep resonant crystal dragon roar",
      trainingHints = hints
    )
    assertTrue(crySpec.durationMs >= 200)
    assertTrue("Offline cry must compute real waveform peaks", cryEntry.waveformPeaks.isNotEmpty())

    val (scoreSpec, bgmEntry) = com.example.ai.OfflineProceduralForge.buildOfflineChiptune(
      title = "Boss Overdrive",
      prompt = "144 BPM boss battle chiptune",
      trainingHints = hints
    )
    assertEquals(144, scoreSpec.bpm)
    assertTrue("Offline 16-step chiptune must have multi-channel notes", scoreSpec.notes.size >= 16)
    assertTrue("Offline chiptune must compute real waveform peaks", bgmEntry.waveformPeaks.isNotEmpty())

    val chatReply = com.example.ai.OfflineProceduralForge.buildOfflineDirectorChatReply(
      userMessage = "Design an armored cyber dragon sprite at 24 fps",
      targetFps = 24,
      relevantWikiContext = hints
    )
    assertEquals("Offline Procedural Engine (0 Tokens)", chatReply.providerUsed)
    assertTrue("Offline Director chat must include BUILD_PROMPT:", chatReply.text.contains("BUILD_PROMPT:"))
    assertFalse(chatReply.extractedBuildPrompt.isNullOrBlank())

    // 8. End-to-end ViewModel offline creation executes cleanly with 0 network calls
    vm.updatePromptText("emerald cyber knight with glowing cyan visor")
    vm.updateScratchCreateAssetName("Kaelen ZeroToken")
    vm.selectAiBuildTarget(com.example.ai.AiBuildTargetKind.MODEL_3D)
    vm.buildBrandNewAssetWithAi()
    assertEquals("Kaelen ZeroToken", vm.uiState.value.currentMesh.name)
    assertEquals("OFFLINE PROCEDURAL 3D", vm.uiState.value.currentMesh.sourceKindLabel)
  }
}




