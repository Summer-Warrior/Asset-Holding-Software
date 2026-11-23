package com.example.model.hardware

import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData
import kotlin.math.max
import kotlin.math.roundToInt

enum class FusionInterconnectMode(
    val label: String,
    val badge: String,
    val technicalArchitecture: String,
    val everydayExplanation: String
) {
    CARTRIDGE_DUAL_PORT_SRAM(
        label = "Cartridge Dual-Port SRAM + DMA",
        badge = "CART SRAM DMA",
        technicalArchitecture = "Donor chip renders into on-cartridge dual-port SRAM/DRAM while the host CPU triggers VBLANK/HDMA burst transfers into host PPU/VDP tile VRAM.",
        everydayExplanation = "The extra chip sits inside the game cartridge with its own scratchpad memory, draws the 3D/2D frame there, and hands the finished tiles to the console between TV frames."
    ),
    DIRECT_COPROCESSOR_BUS(
        label = "Direct CPU Co-Processor / FIFO Bus",
        badge = "CPU FIFO BUS",
        technicalArchitecture = "Donor silicon attaches directly to the host CPU data/address bus via memory-mapped command FIFOs or coprocessor instruction traps, feeding transformed primitives straight to the host rasterizer.",
        everydayExplanation = "The donor chip plugs straight into the console's main processor like a turbo math brain, calculating 3D points and sprite movements instantly before the graphics chip draws them."
    ),
    GENLOCK_VIDEO_OVERLAY(
        label = "Genlock Video Pass-Through Overlay",
        badge = "GENLOCK MIXER",
        technicalArchitecture = "Donor video/3D subsystem maintains an independent high-color framebuffer and genlocks its pixel clock to the host console's RGB/Composite video encoder, compositing 3D polygons over host 2D background planes via a chroma-key window register.",
        everydayExplanation = "Just like the Sega 32X add-on, the fused chip generates its own high-color 3D picture and layers it directly on top of the original console's 2D backgrounds through a video mixer cable."
    )
}

data class DonorChipAttachment(
    val id: String,
    val name: String,
    val shortName: String,
    val originSystem: String,
    val releaseYear: Int,
    val chipSpec: ChipSpec,
    val grantsHardware3D: Boolean,
    val preferred3DProfile: Hardware3DRenderProfile?,
    val triBudgetBonus: Int,
    val triBudgetMultiplier: Float,
    val overridesVertexSnap: Float?, // e.g., 0f if sub-pixel accurate like N64 RDP, or 5.5f for PS1 GTE
    val grantsZBuffer: Boolean,
    val upgradesBitsPerPixel: Int?, // e.g., 15-bit/8bpp for 32X Super VDP
    val upgradesPaletteType: PaletteConstraintType?,
    val extraVramBytes: Long,
    val extraTextureCacheBytes: Int,
    val scanlineSpriteMultiplier: Float,
    val donor3DSynthesisNote: String,
    val donor2DSynthesisNote: String
)

data class CrossSystemFusionBlueprint(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val hostSystemId: String,
    val primaryDonorChipId: String,
    val secondaryDonorChipId: String? = null,
    val interconnect: FusionInterconnectMode
)

data class ChipFusionAssetOutputReport(
    val fusedSpec: ConsoleHardwareSpec,
    val hostSpec: ConsoleHardwareSpec,
    val donorChips: List<DonorChipAttachment>,
    val interconnect: FusionInterconnectMode,
    val baseMeshReport: MeshHardwareReport,
    val fusedMeshReport: MeshHardwareReport,
    val baseSpriteReport: SpriteHardwareReport?,
    val fusedSpriteReport: SpriteHardwareReport?,
    val triBudgetGainPercent: Int,
    val vramGainBytes: Long,
    val everydayFusionReadout: String,
    val highEndTechnicalFusionReadout: String,
    val busBridgeRegisterDump: String
)

object ChipFusionEngine {

    val donorChips: List<DonorChipAttachment> = listOf(
        DonorChipAttachment(
            id = "donor_superfx2",
            name = "Argonaut Super FX2 (GSU-2 @ 21.48 MHz)",
            shortName = "Super FX2 GSU-2",
            originSystem = "SNES Cartridge (Star Fox 2 / Yoshi's Island)",
            releaseYear = 1995,
            chipSpec = ChipSpec(
                name = "Grafted Super FX2 GSU-2 RISC + PLOT Unit",
                role = "16-Bit RISC Geometry & Hardware Bitplane/Nibble Pixel Plotter",
                architecture = "16× 16-bit Registers (R0–R15) + 512B I-Cache + Dual Pixel Row Cache",
                clockSpeed = "21.48 MHz",
                busAndProcess = "64 KB Cartridge Framebuffer SRAM + Host DMA Bridge",
                technicalNotes = "Executes single-cycle 16-bit `FMULT`/`LMULT` matrix rotations and hardware `PLOT` with 2×2 ordered Bayer dithering (`CMODE`) into character tiles."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
            triBudgetBonus = 140,
            triBudgetMultiplier = 2.8f,
            overridesVertexSnap = 3.5f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = null,
            upgradesPaletteType = null,
            extraVramBytes = 65536L,
            extraTextureCacheBytes = 512,
            scanlineSpriteMultiplier = 1.5f,
            donor3DSynthesisNote = "GSU-2 transforms 3D vertices at 21.48 MHz and hardware-plots flat/textured polygons with 2×2 Bayer dithering into a 64 KB SRAM tile buffer before DMAing to the host.",
            donor2DSynthesisNote = "GSU-2 hardware-scales and rotates arbitrary 2D sprites directly into the cartridge tile buffer, bypassing host scanline sprite limits."
        ),

        DonorChipAttachment(
            id = "donor_sega_svp",
            name = "Sega Virtua Processor (Samsung SSP1601 @ 23 MHz)",
            shortName = "Sega SVP DSP",
            originSystem = "Genesis Cartridge (Virtua Racing)",
            releaseYear = 1994,
            chipSpec = ChipSpec(
                name = "Grafted Sega SVP (Samsung SSP1601 DSP)",
                role = "25 MIPS 16-Bit Fixed-Point 3D Geometry & Span Rasterizer",
                architecture = "16-bit DSP w/ Single-Cycle 16×16→32 Multiply-Accumulate (MAC)",
                clockSpeed = "23.01 MHz",
                busAndProcess = "128 KB On-Cartridge DRAM + Address Translator ASIC",
                technicalNotes = "Computes 3D frustum clipping, depth sorting, and checkerboard-dithered polygon span filling at 300–500 polygons/frame into 128 KB cartridge DRAM."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.SVP_DSP_FLAT,
            triBudgetBonus = 190,
            triBudgetMultiplier = 3.2f,
            overridesVertexSnap = 3.0f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = null,
            upgradesPaletteType = null,
            extraVramBytes = 131072L,
            extraTextureCacheBytes = 2048,
            scanlineSpriteMultiplier = 1.25f,
            donor3DSynthesisNote = "SSP1601 executes single-cycle MAC matrix transforms and scanline span conversion into 128 KB DRAM, feeding pre-rendered tile strips to the host PPU/VDP.",
            donor2DSynthesisNote = "Uses 128 KB cartridge DRAM to pre-composite multi-layered scaled 2D objects with 2×2 checkerboard gradient dithering."
        ),

        DonorChipAttachment(
            id = "donor_capcom_cx4",
            name = "Capcom CX4 (Hitachi HG51B169 @ 20 MHz)",
            shortName = "Capcom CX4 DSP",
            originSystem = "SNES Cartridge (Mega Man X2 / X3)",
            releaseYear = 1994,
            chipSpec = ChipSpec(
                name = "Grafted Capcom CX4 Trigonometric Vector DSP",
                role = "24-Bit Fixed-Point Wireframe & Multi-Sprite Chain Processor",
                architecture = "Hitachi HG51B169 DSP w/ Internal Sin/Cos Lookup ROM",
                clockSpeed = "20.0 MHz",
                busAndProcess = "2 KB Internal Vector RAM + Host OAM/Tile Bus Bridge",
                technicalNotes = "Hardware sine/cosine ROM tables accelerate 60 FPS 3D wireframe line plotting and real-time multi-joint OAM sprite skeleton rotation."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.CX4_TRIG_WIREFRAME,
            triBudgetBonus = 95,
            triBudgetMultiplier = 2.4f,
            overridesVertexSnap = 2.5f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = null,
            upgradesPaletteType = null,
            extraVramBytes = 16384L,
            extraTextureCacheBytes = 1024,
            scanlineSpriteMultiplier = 1.6f,
            donor3DSynthesisNote = "CX4 rotates 3D meshes at 60 FPS using internal trigonometric ROM tables and plots glowing vector wireframe edges into host character tiles.",
            donor2DSynthesisNote = "CX4 automatically calculates rotated X/Y screen coordinates and affine tile chains for multi-part boss sprites."
        ),

        DonorChipAttachment(
            id = "donor_sa1_mmc5",
            name = "Nintendo SA-1 + MMC5 ExRAM Accelerator",
            shortName = "SA-1 + MMC5",
            originSystem = "SNES / NES Cartridge Co-CPU & Mapper ASIC",
            releaseYear = 1995,
            chipSpec = ChipSpec(
                name = "Grafted Nintendo SA-1 (10.74 MHz) + MMC5 ExRAM Bridge",
                role = "Character Conversion DMA, Barrel Shifter & Per-Tile Palette Expander",
                architecture = "10.74 MHz 65C816 Core + Hardware Linear-to-Planar DMA + 1KB ExRAM",
                clockSpeed = "10.74 MHz",
                busAndProcess = "64 KB BW-RAM + 2 KB Fast I-RAM + Split-Screen Scanline IRQ",
                technicalNotes = "Converts packed linear bitmaps into planar 2bpp/4bpp tiles on the fly during DMA while MMC5 ExRAM lifts tile index and per-tile palette restrictions."
            ),
            grantsHardware3D = false,
            preferred3DProfile = null,
            triBudgetBonus = 70,
            triBudgetMultiplier = 1.9f,
            overridesVertexSnap = null,
            grantsZBuffer = false,
            upgradesBitsPerPixel = null,
            upgradesPaletteType = null,
            extraVramBytes = 65536L,
            extraTextureCacheBytes = 2048,
            scanlineSpriteMultiplier = 1.8f,
            donor3DSynthesisNote = "SA-1's hardware barrel shifter and Character Conversion DMA convert software-rendered 3D polygons into planar PPU tiles at 3× native bus speed.",
            donor2DSynthesisNote = "MMC5 ExRAM + SA-1 DMA eliminate CHR bank bottlenecks and boost effective scanline sprite throughput and sub-palette variety."
        ),

        DonorChipAttachment(
            id = "donor_sega_32x_vdp",
            name = "Sega 32X Dual Hitachi SH-2 + Super VDP",
            shortName = "32X Dual SH-2",
            originSystem = "Sega 32X Add-On (Mars Architecture)",
            releaseYear = 1994,
            chipSpec = ChipSpec(
                name = "Grafted 32X Dual SH-2 (23 MHz × 2) + 32,768-Color Super VDP",
                role = "Dual 32-Bit RISC 3D Engine & 15-Bit Genlock Framebuffer Overlay",
                architecture = "Two 32-bit Hitachi SH-2 RISC CPUs (40 MIPS) + Dual 128 KB Framebuffers",
                clockSpeed = "23.01 MHz (Dual Core)",
                busAndProcess = "256 KB SDRAM + 256 KB Dual Framebuffer VRAM + Video Genlock Mixer",
                technicalNotes = "Renders 15-bit RGB555 (32,768-color) or 256-color packed-pixel 3D polygons and RLE sprites into dual framebuffers genlocked directly over the host console's 2D video output."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.PS1_GTE_AFFINE_WOBBLE,
            triBudgetBonus = 280,
            triBudgetMultiplier = 3.5f,
            overridesVertexSnap = 3.0f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = 8,
            upgradesPaletteType = PaletteConstraintType.RGB555_15BIT,
            extraVramBytes = 262144L,
            extraTextureCacheBytes = 4096,
            scanlineSpriteMultiplier = 2.5f,
            donor3DSynthesisNote = "Twin 32-bit SH-2 RISC processors rasterize Gouraud-shaded and texture-mapped 15-bit RGB555 polygons into a 256 KB dual framebuffer genlocked over the host display.",
            donor2DSynthesisNote = "Upgrades sprite output to 8bpp (256-color) and 15-bit RGB555 Direct Color with hardware RLE decompression and arbitrary scaling."
        ),

        DonorChipAttachment(
            id = "donor_ps1_gte",
            name = "Sony PlayStation GTE (COP2 Matrix Engine @ 33.8 MHz)",
            shortName = "PS1 GTE (COP2)",
            originSystem = "Sony PlayStation (R3000A Die Co-Processor)",
            releaseYear = 1994,
            chipSpec = ChipSpec(
                name = "Grafted Sony GTE (Geometry Transformation Engine)",
                role = "66 MIPS Fixed-Point SIMD 3D Matrix, Perspective & Lighting Unit",
                architecture = "32-bit COP2 Vector Unit (RTPS/RTPT 22-Cycle Triangle Transform + NCLIP/MVMVA)",
                clockSpeed = "33.8688 MHz",
                busAndProcess = "Direct CPU Co-Processor Register Interface + 1 KB Fast Scratchpad",
                technicalNotes = "Transforms a 3D triangle with perspective projection and 3×3 directional lighting color matrix in 22 cycles, outputting 16-bit integer screen X/Y coordinates."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.PS1_GTE_AFFINE_WOBBLE,
            triBudgetBonus = 360,
            triBudgetMultiplier = 3.8f,
            overridesVertexSnap = 5.0f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = null,
            upgradesPaletteType = null,
            extraVramBytes = 131072L,
            extraTextureCacheBytes = 2048,
            scanlineSpriteMultiplier = 1.4f,
            donor3DSynthesisNote = "Offloads all 3D matrix multiplication, perspective division (`RTPT`), backface culling (`NCLIP`), and vertex lighting to Sony's 66 MIPS GTE co-processor.",
            donor2DSynthesisNote = "Accelerates 2D quad corner rotation, pseudo-3D sprite billboarding, and depth-bucket sorting (`OTZ`) in hardware."
        ),

        DonorChipAttachment(
            id = "donor_n64_rdp",
            name = "SGI N64 Reality Display Processor (RDP + 4KB TMEM)",
            shortName = "N64 RDP + Z-Buf",
            originSystem = "Nintendo 64 (SGI Reality Coprocessor)",
            releaseYear = 1996,
            chipSpec = ChipSpec(
                name = "Grafted SGI N64 RDP (Sub-Pixel Z-Buffer + 3-Point Texture Filter)",
                role = "Perspective-Correct Sub-Pixel Rasterizer w/ 4 KB On-Chip TMEM",
                architecture = "Sub-Pixel Stepper + Hardware Z-Buffer + 3-Point Bilinear Filter + Color Combiner",
                clockSpeed = "62.5 MHz",
                busAndProcess = "4,096-Byte (4 KB) On-Chip TMEM Cache + RDRAM Framebuffer Bus",
                technicalNotes = "Eliminates integer vertex wobble and affine texture swimming via sub-pixel screen coordinates, per-pixel 1/W perspective division, and hardware Z-buffering—while imposing the strict 4 KB TMEM texture cache rule."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.N64_RCP_TMEM_3POINT,
            triBudgetBonus = 420,
            triBudgetMultiplier = 2.6f,
            overridesVertexSnap = 0f, // Cures vertex wobble!
            grantsZBuffer = true,
            upgradesBitsPerPixel = 16,
            upgradesPaletteType = PaletteConstraintType.RGB555_15BIT,
            extraVramBytes = 1048576L,
            extraTextureCacheBytes = 4096,
            scanlineSpriteMultiplier = 2.2f,
            donor3DSynthesisNote = "Replaces integer vertex snapping and affine UV warping with N64 RDP sub-pixel precision, hardware Z-buffering, and 3-point bilinear texture filtering (requiring 32×32 TMEM tile slicing).",
            donor2DSynthesisNote = "Renders 2D sprites as sub-pixel positioned, 3-point filtered RDP textured rectangles streamed through the 4,096-byte TMEM cache."
        ),

        DonorChipAttachment(
            id = "donor_saturn_vdp2",
            name = "Sega Saturn VDP2 (5-Plane + Dual 3D RBG0/1 Engine)",
            shortName = "Saturn VDP2",
            originSystem = "Sega Saturn (315-5690 Background/Plane Processor)",
            releaseYear = 1994,
            chipSpec = ChipSpec(
                name = "Grafted Sega VDP2 (315-5690) Multi-Plane & 3D Horizon Compositor",
                role = "5 Simultaneous Scroll Layers + Dual 3D Rotating/Scaling RBG0/RBG1 Planes",
                architecture = "dedicated 512 KB Background VRAM + 4 KB Color RAM + Line/Cell Scroll LUTs",
                clockSpeed = "28.63 MHz",
                busAndProcess = "32-bit Dedicated VDP2 VRAM Bus + Priority Color Mixer",
                technicalNotes = "Renders infinite 3D voxel/affine ground & sky horizons and 5 parallax layers at 60 FPS with zero polygon overhead, freeing 100% of the 3D polygon budget for character assets."
            ),
            grantsHardware3D = false,
            preferred3DProfile = null,
            triBudgetBonus = 220,
            triBudgetMultiplier = 1.6f,
            overridesVertexSnap = null,
            grantsZBuffer = false,
            upgradesBitsPerPixel = 8,
            upgradesPaletteType = PaletteConstraintType.RGB555_15BIT,
            extraVramBytes = 524288L,
            extraTextureCacheBytes = 4096,
            scanlineSpriteMultiplier = 2.0f,
            donor3DSynthesisNote = "Offloads all environment floors, skies, and distant terrain to VDP2's RBG0/RBG1 hardware matrix planes so 100% of polygon throughput goes to your 3D character model.",
            donor2DSynthesisNote = "Adds 4 KB of dedicated Color RAM (2,048 simultaneous 15/24-bit colors), per-scanline zoom, and hardware color ratio blending."
        ),

        DonorChipAttachment(
            id = "donor_nds_arm9_3d",
            name = "Nintendo DS ARM9 3D Geometry & Toon Engine (@ 67 MHz)",
            shortName = "NDS ARM9 3D",
            originSystem = "Nintendo DS (ARM946E-S + GX 3D Core)",
            releaseYear = 2004,
            chipSpec = ChipSpec(
                name = "Grafted NDS Hardware 3D Matrix Stack & Scanline Toon Rasterizer",
                role = "Fixed-Point Hardware T&L, 32-Entry Toon Table & 2048-Poly Sorter",
                architecture = "4×4 Hardware Matrix Stack + 6,144-Vertex / 2,048-Polygon RAM + Dual Line Buffers",
                clockSpeed = "67.028 MHz",
                busAndProcess = "32-bit GX_FIFO DMA Bus + Scanline Capture Bridge",
                technicalNotes = "Provides hardware transform & lighting, contour edge outlining, and a 32-entry Toon Shading lookup table without needing a full 3D VRAM framebuffer (renders via dual scanline buffers)."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.NDS_FIXED_TOON_2048,
            triBudgetBonus = 400,
            triBudgetMultiplier = 3.6f,
            overridesVertexSnap = 2.5f,
            grantsZBuffer = true,
            upgradesBitsPerPixel = 8,
            upgradesPaletteType = PaletteConstraintType.RGB555_15BIT,
            extraVramBytes = 524288L,
            extraTextureCacheBytes = 65536,
            scanlineSpriteMultiplier = 2.4f,
            donor3DSynthesisNote = "Streams 3D geometry into a 67 MHz hardware matrix stack with built-in cel/toon table shading and scanline depth sorting (up to 2,048 polygons/frame) with zero framebuffer VRAM cost.",
            donor2DSynthesisNote = "Captures 3D scanline output directly as a 2D background/sprite layer while expanding sprite palettes to 16×256-color extended slots."
        )
    )

    val curatedFusionBlueprints: List<CrossSystemFusionBlueprint> = listOf(
        CrossSystemFusionBlueprint(
            id = "fusion_genesis_superfx2",
            title = "Sega Genesis + Super FX2 (GSU-2)",
            shortTitle = "Genesis + Super FX2",
            subtitle = "21.48 MHz GSU-2 plotting directly into Genesis linear 4bpp nibbles + 68000 Blast DMA",
            hostSystemId = "sega_genesis",
            primaryDonorChipId = "donor_superfx2",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_snes_svp",
            title = "SNES (S-PPU) + Sega SVP (SSP1601 DSP)",
            shortTitle = "SNES + Sega SVP",
            subtitle = "Sega's 23 MHz Virtua Racing DSP paired with SNES 32,768-color BGR555 & SA-1 Planar DMA",
            hostSystemId = "snes",
            primaryDonorChipId = "donor_sega_svp",
            secondaryDonorChipId = "donor_sa1_mmc5",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_ps1_n64_rdp",
            title = "PlayStation (GTE) + N64 RDP Z-Buffer Bridge",
            shortTitle = "PS1 GTE + N64 RDP",
            subtitle = "PS1 R3000A/GTE speed + CD/VRAM capacity fused with N64 sub-pixel Z-buffer (Zero Wobble!)",
            hostSystemId = "ps1",
            primaryDonorChipId = "donor_n64_rdp",
            interconnect = FusionInterconnectMode.DIRECT_COPROCESSOR_BUS
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_saturn_ps1_gte",
            title = "Sega Saturn (VDP1+2) + Sony PS1 GTE",
            shortTitle = "Saturn + PS1 GTE",
            subtitle = "66 MIPS Sony GTE co-processor feeding transformed 3D quads into Saturn VDP1 & VDP2",
            hostSystemId = "sega_saturn",
            primaryDonorChipId = "donor_ps1_gte",
            interconnect = FusionInterconnectMode.DIRECT_COPROCESSOR_BUS
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_nes_superfx_mmc5",
            title = "NES (2C02) + Super FX2 + MMC5 ExRAM",
            shortTitle = "NES + Super FX2",
            subtitle = "21.48 MHz 3D polygon plotter & MMC5 per-tile attributes streaming into 8-bit 2C02 CHR-RAM",
            hostSystemId = "nes_2c02",
            primaryDonorChipId = "donor_superfx2",
            secondaryDonorChipId = "donor_sa1_mmc5",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_dmg_cx4",
            title = "Game Boy (DMG-01) + Capcom CX4 Vector DSP",
            shortTitle = "Game Boy + CX4 DSP",
            subtitle = "Hitachi 20 MHz trigonometric wireframe & sprite-chain DSP on the 4-shade olive LCD",
            hostSystemId = "gameboy_dmg",
            primaryDonorChipId = "donor_capcom_cx4",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_genesis_32x_svp",
            title = "Genesis + 32X Dual SH-2 + SVP DSP",
            shortTitle = "Genesis + 32X + SVP",
            subtitle = "Triple-DSP Mars/Virtua hybrid: SVP geometry + Dual SH-2 15-bit rasterizer over Genesis VDP",
            hostSystemId = "sega_genesis",
            primaryDonorChipId = "donor_sega_32x_vdp",
            secondaryDonorChipId = "donor_sega_svp",
            interconnect = FusionInterconnectMode.GENLOCK_VIDEO_OVERLAY
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_gba_nds3d",
            title = "GBA (ARM7TDMI) + NDS ARM9 3D Toon Engine",
            shortTitle = "GBA + NDS 3D Core",
            subtitle = "67 MHz hardware matrix stack, 2,048-poly sorter & Toon Table feeding GBA's 240×160 TFT",
            hostSystemId = "gba",
            primaryDonorChipId = "donor_nds_arm9_3d",
            interconnect = FusionInterconnectMode.DIRECT_COPROCESSOR_BUS
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_fan_snes_msu1_fxpak",
            title = "SNES + MSU-1 + SuperFX3 (43MHz FPGA)",
            shortTitle = "SNES + MSU-1 + FX3",
            subtitle = "Fan-made 4GB B-Bus streamer + 42.95 MHz zero-wait-state SuperFX3 FPGA core",
            hostSystemId = "snes",
            primaryDonorChipId = "donor_fan_msu1_fxpak",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM
        ),
        CrossSystemFusionBlueprint(
            id = "fusion_fan_atari_cdfj",
            title = "Atari 2600 + Melody CDFJ+ (70MHz ARM)",
            shortTitle = "2600 + CDFJ+ ARM",
            subtitle = "Fan-made 70 MHz ARM7TDMI cartridge chip feeding 2-cycle Fast-Fetch streams to TIA",
            hostSystemId = "atari_2600",
            primaryDonorChipId = "donor_fan_atari_cdfj",
            interconnect = FusionInterconnectMode.DIRECT_COPROCESSOR_BUS
        )
    )

    val allDonorChipsWithFanSilicon: List<DonorChipAttachment> by lazy {
        donorChips + FanChipCatalog.fanDonorAttachments
    }

    fun getDonorById(id: String): DonorChipAttachment {
        return allDonorChipsWithFanSilicon.find { it.id == id } ?: donorChips.first()
    }

    /**
     * Synthesizes a complete, playable ConsoleHardwareSpec by fusing a Host System with 1 or 2 Donor Chips
     * from other systems across the chosen bus interconnect.
     */
    fun synthesizeFusedSystemSpec(
        hostSpec: ConsoleHardwareSpec,
        donors: List<DonorChipAttachment>,
        interconnect: FusionInterconnectMode
    ): ConsoleHardwareSpec {
        if (donors.isEmpty()) return hostSpec

        val donorShortList = donors.joinToString(" + ") { it.shortName }
        val fusedId = "fused_${hostSpec.id}_${donors.joinToString("_") { it.id.removePrefix("donor_") }}"
        val fusedName = "${hostSpec.shortName} + $donorShortList [Hybrid]"
        val fusedShortName = "${hostSpec.shortName.substringBefore(" (")} + ${donors.first().shortName.substringBefore(" ")}"

        val has3D = hostSpec.hasHardware3D || donors.any { it.grantsHardware3D }
        val chosen3DProfile = donors.firstNotNullOfOrNull { it.preferred3DProfile } ?: hostSpec.renderProfile3D

        // Calculate combined triangle budget
        val maxMultiplier = donors.maxOf { it.triBudgetMultiplier }
        val totalBonus = donors.sumOf { it.triBudgetBonus }
        val newRecTris = ((hostSpec.recommendedModelTriBudget * maxMultiplier).roundToInt() + totalBonus)
            .coerceAtLeast(hostSpec.recommendedModelTriBudget + 60)
        val newMaxSceneTris = ((hostSpec.maxSceneTrisPerFrame * maxMultiplier).roundToInt() + totalBonus * 3)
            .coerceAtLeast(newRecTris * 2)

        // Vertex snap grid: if a donor explicitly overrides (e.g. N64 RDP = 0f sub-pixel), apply it
        val snapOverride = donors.mapNotNull { it.overridesVertexSnap }.minOrNull()
        val newVertexSnap = snapOverride ?: hostSpec.vertexSnapGrid

        val newHasZBuffer = hostSpec.hasZBuffer || donors.any { it.grantsZBuffer }

        // Color & Sprite BPP upgrades (especially in Genlock Overlay or Direct Bus modes)
        val upgradedBpp = donors.mapNotNull { it.upgradesBitsPerPixel }.maxOrNull()
        val newBpp = if (upgradedBpp != null && (interconnect != FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM || upgradedBpp <= 8)) {
            max(hostSpec.bitsPerPixelSprite, upgradedBpp)
        } else {
            hostSpec.bitsPerPixelSprite
        }

        val newPaletteType = if (interconnect == FusionInterconnectMode.GENLOCK_VIDEO_OVERLAY) {
            donors.firstNotNullOfOrNull { it.upgradesPaletteType } ?: hostSpec.paletteType
        } else {
            hostSpec.paletteType
        }

        val newMaxSpriteCols = when {
            newBpp >= 8 -> max(hostSpec.maxSpriteColorsPerPalette, 256)
            newBpp >= 4 -> max(hostSpec.maxSpriteColorsPerPalette, 16)
            else -> hostSpec.maxSpriteColorsPerPalette
        }

        val newOnScreenCols = if (newPaletteType == PaletteConstraintType.RGB555_15BIT) {
            max(hostSpec.maxOnScreenColors, 32768)
        } else {
            max(hostSpec.maxOnScreenColors, hostSpec.maxOnScreenColors * 2)
        }

        val newMasterPalette = if (newPaletteType == PaletteConstraintType.RGB555_15BIT) {
            max(hostSpec.masterPaletteSize, 32768)
        } else {
            hostSpec.masterPaletteSize
        }

        val extraVram = donors.sumOf { it.extraVramBytes }
        val newVramBytes = hostSpec.vramBytes + extraVram
        val newTexCache = max(hostSpec.textureCacheBytes, donors.maxOf { it.extraTextureCacheBytes })

        val scanlineMult = donors.maxOf { it.scanlineSpriteMultiplier }
        val newScanlineSprites = (hostSpec.maxSpritesPerScanline * scanlineMult).roundToInt()
            .coerceAtLeast(hostSpec.maxSpritesPerScanline + 4)

        val bridgeChip = ChipSpec(
            name = "Cross-System ${interconnect.badge} Interconnect ASIC",
            role = "Bus Translator, Clock Synchronizer & Format Bridge",
            architecture = interconnect.label,
            clockSpeed = "${donors.first().chipSpec.clockSpeed} ↔ Host Bus Sync",
            busAndProcess = "Bridges ${donors.joinToString(" + ") { it.originSystem.substringBefore(" (") }} silicon onto ${hostSpec.shortName}",
            technicalNotes = interconnect.technicalArchitecture
        )

        val combinedChips = hostSpec.chips + donors.map { it.chipSpec } + bridgeChip

        val fusedSpritePipeline = buildString {
            append("[CHIP FUSION 2D PIPELINE — ${hostSpec.shortName} Host + $donorShortList]: ")
            append(donors.joinToString(" ") { it.donor2DSynthesisNote })
            append(" Final output is composited through ${hostSpec.shortName}'s ${hostSpec.gpuOrPpuSummary} via ${interconnect.label}.")
        }

        val fused3DPipeline = buildString {
            append("[CHIP FUSION 3D PIPELINE — ${hostSpec.shortName} Host + $donorShortList]: ")
            append(donors.joinToString(" ") { it.donor3DSynthesisNote })
            append(" Interconnect: ${interconnect.technicalArchitecture}")
        }

        return hostSpec.copy(
            id = fusedId,
            name = fusedName,
            shortName = fusedShortName,
            releaseYear = max(hostSpec.releaseYear, donors.maxOf { it.releaseYear }),
            manufacturer = "${hostSpec.manufacturer} × Grafted Silicon Lab",
            category = HardwareCategory.EXPANSION_CHIP,
            cpuSummary = "${hostSpec.cpuSummary.substringBefore(" +")} + $donorShortList",
            gpuOrPpuSummary = "${hostSpec.gpuOrPpuSummary.substringBefore(" (")} + ${donors.first().shortName}",
            memorySummary = "${hostSpec.memorySummary} + ${extraVram / 1024} KB Grafted Silicon RAM",
            vramBytes = newVramBytes,
            textureCacheBytes = newTexCache,
            bitsPerPixelSprite = newBpp,
            maxSpriteColorsPerPalette = newMaxSpriteCols,
            maxOnScreenColors = newOnScreenCols,
            masterPaletteSize = newMasterPalette,
            maxSpritesPerScanline = newScanlineSprites,
            hasHardware3D = has3D,
            recommendedModelTriBudget = newRecTris,
            maxSceneTrisPerFrame = newMaxSceneTris,
            vertexSnapGrid = newVertexSnap,
            hasZBuffer = newHasZBuffer,
            renderProfile3D = chosen3DProfile,
            paletteType = newPaletteType,
            chips = combinedChips,
            spritePipelineExplanation = fusedSpritePipeline,
            model3DPipelineExplanation = fused3DPipeline
        )
    }

    /**
     * Generates a complete before-and-after Asset System Output Report comparing the Base Host System
     * against the Fused Host + Donor Chip(s) Architecture for the user's current 3D mesh and 2D sprite.
     */
    fun evaluateAssetOnFusedSystem(
        mesh: Mesh3D,
        spriteSheet: SpriteSheetData,
        hostSpec: ConsoleHardwareSpec,
        donors: List<DonorChipAttachment>,
        interconnect: FusionInterconnectMode,
        showTileGrid: Boolean = true
    ): ChipFusionAssetOutputReport {
        val fusedSpec = synthesizeFusedSystemSpec(hostSpec, donors, interconnect)
        val baseMeshReport = HardwareAssetSimulator.analyzeMeshOnHardware(mesh, hostSpec)
        val fusedMeshReport = HardwareAssetSimulator.analyzeMeshOnHardware(mesh, fusedSpec)

        val firstFrame = spriteSheet.frames.firstOrNull()
        val baseSpriteReport = firstFrame?.let {
            HardwareAssetSimulator.simulateSpriteOnHardware(it, hostSpec, showTileGrid)
        }
        val fusedSpriteReport = firstFrame?.let {
            HardwareAssetSimulator.simulateSpriteOnHardware(it, fusedSpec, showTileGrid)
        }

        val triGainPct = (((fusedSpec.recommendedModelTriBudget - hostSpec.recommendedModelTriBudget).toFloat() /
                hostSpec.recommendedModelTriBudget.coerceAtLeast(1)) * 100f).roundToInt()
        val vramGain = fusedSpec.vramBytes - hostSpec.vramBytes
        val donorNames = donors.joinToString(" + ") { it.name }

        val everydayText = buildString {
            append("What Happens When You Graft $donorNames onto ${hostSpec.name}:\n\n")
            append("• How the Chips Connect (${interconnect.label}): ${interconnect.everydayExplanation}\n\n")
            append("• 3D Model Output ('${mesh.name}'): ")
            if (!hostSpec.hasHardware3D && fusedSpec.hasHardware3D) {
                append(
                    "Normally, a stock ${hostSpec.shortName} has no 3D polygon hardware and can only draw '${mesh.name}' as a slow software wireframe (budget: ${hostSpec.recommendedModelTriBudget} tris). " +
                            "By attaching ${donors.first().shortName}, your console gains a dedicated 3D math & polygon engine! " +
                            "The per-character polygon budget jumps by +$triGainPct% (from ${hostSpec.recommendedModelTriBudget} up to ${fusedSpec.recommendedModelTriBudget} triangles), rendering '${mesh.name}' (${mesh.faceCount} tris) as ${fusedSpec.renderProfile3D.badge}.\n\n"
                )
            } else if (hostSpec.vertexSnapGrid > 0f && fusedSpec.vertexSnapGrid == 0f) {
                append(
                    "On a stock ${hostSpec.shortName}, rotating '${mesh.name}' causes classic retro polygon wobble because vertices snap to integer screen pixels without a Z-buffer. " +
                            "Grafting ${donors.first().shortName} onto the bus adds true sub-pixel vertex positioning, perspective-correct textures, and a hardware Z-buffer—completely curing vertex wobble while boosting the polygon budget to ${fusedSpec.recommendedModelTriBudget} triangles!\n\n"
                )
            } else {
                append(
                    "Attaching ${donors.joinToString(" and ") { it.shortName }} supercharges ${hostSpec.shortName}'s 3D pipeline (${fusedSpec.renderProfile3D.badge}), expanding the character polygon budget by +$triGainPct% (${hostSpec.recommendedModelTriBudget} → ${fusedSpec.recommendedModelTriBudget} triangles) so '${mesh.name}' (${mesh.faceCount} tris) takes only ${fusedMeshReport.assetBudgetPercent.roundToInt()}% of the character budget instead of ${baseMeshReport.assetBudgetPercent.roundToInt()}%.\n\n"
                )
            }

            append("• 2D Sprite Output ('${spriteSheet.name}'): ")
            if (baseSpriteReport != null && fusedSpriteReport != null) {
                append(
                    "Your sprite is processed with +${vramGain / 1024} KB of additional silicon buffer RAM and ${fusedSpec.bitsPerPixelSprite}bpp color depth (${fusedSpriteReport.hardwarePaletteSwatches.size} visible colors). " +
                            "Scanline sprite capacity expands from ${hostSpec.maxSpritesPerScanline} to ${fusedSpec.maxSpritesPerScanline} slots per line (${if (fusedSpriteReport.causesScanlineFlicker) "requires OAM multiplexing" else "100% flicker-free"})."
                )
            }
        }

        val technicalText = buildString {
            append("CROSS-SILICON BUS ARCHITECTURE & ASSET OUTPUT TELEMETRY:\n")
            append("1. Host & Donor Topology: Host [${hostSpec.name} — ${hostSpec.cpuSummary} / ${hostSpec.gpuOrPpuSummary}] bridged via [${interconnect.label}] to Donor Silicon [$donorNames].\n")
            append("2. Interconnect & DMA Protocol: ${interconnect.technicalArchitecture}\n")
            append(
                "3. 3D Geometry & Rasterization Output ('${mesh.name}', ${mesh.vertexCount}v/${mesh.faceCount}f): " +
                        "Stock ${hostSpec.shortName} allocates ${baseMeshReport.totalAssetRamBytes}B under [${hostSpec.renderProfile3D.badge}] (${baseMeshReport.assetBudgetPercent.roundToInt()}% budget load). " +
                        "Fused pipeline shifts transform/rasterization to [${fusedSpec.renderProfile3D.badge}], expanding single-character budget from ${hostSpec.recommendedModelTriBudget} → ${fusedSpec.recommendedModelTriBudget} tris (+$triGainPct%), " +
                        "scene ceiling to ${fusedSpec.maxSceneTrisPerFrame} tris/frame, vertex snap grid ${hostSpec.vertexSnapGrid} → ${fusedSpec.vertexSnapGrid}, and Z-Buffer: ${hostSpec.hasZBuffer} → ${fusedSpec.hasZBuffer}.\n"
            )
            if (baseSpriteReport != null && fusedSpriteReport != null) {
                append(
                    "4. 2D PPU/VDP & CHR Transfer Output ('${spriteSheet.name}'): " +
                            "Allocates ${fusedSpriteReport.chrVramBytes}B across ${fusedSpriteReport.totalChrTiles} tiles (${fusedSpec.bitsPerPixelSprite}bpp, ${fusedSpriteReport.hardwarePaletteSwatches.size} quantized sub-palette swatches). " +
                            "Combined VRAM pool expands from ${hostSpec.vramBytes / 1024} KB → ${fusedSpec.vramBytes / 1024} KB (+${vramGain / 1024} KB), reducing asset VRAM pressure from ${String.format("%.2f%%", baseSpriteReport.vramUsagePercent)} down to ${String.format("%.2f%%", fusedSpriteReport.vramUsagePercent)}."
                )
            }
        }

        val hexDump = buildString {
            append("// FUSED SILICON INTERCONNECT & DMA BRIDGE REGISTER MAP\n")
            append("HOST_BUS_BASE     = 0x${hostSpec.id.hashCode().ushr(4).toString(16).uppercase().padStart(6, '0')}  // ${hostSpec.shortName} Native Bus\n")
            donors.forEachIndexed { index, donor ->
                val addr = (0x7E8000 + index * 0x4000).toString(16).uppercase()
                append("DONOR_${index + 1}_MMIO_REG  = 0x$addr  // ${donor.shortName} (${donor.chipSpec.clockSpeed})\n")
            }
            append("BRIDGE_MODE_CTRL  = 0x00${interconnect.ordinal + 1}F  // ${interconnect.badge}\n")
            append("MESH_VTX_DMA_LEN  = 0x${fusedMeshReport.vertexBufferBytes.toString(16).uppercase().padStart(4, '0')}  // ${fusedMeshReport.vertexBufferBytes} Bytes (${mesh.vertexCount} Verts)\n")
            append("MESH_TRI_CMD_LEN  = 0x${fusedMeshReport.faceTableBytes.toString(16).uppercase().padStart(4, '0')}  // ${fusedMeshReport.faceTableBytes} Bytes (${mesh.faceCount} Faces)\n")
            if (fusedSpriteReport != null) {
                append("SPR_CHR_XFER_LEN  = 0x${fusedSpriteReport.chrVramBytes.toString(16).uppercase().padStart(4, '0')}  // ${fusedSpriteReport.chrVramBytes} Bytes (${fusedSpriteReport.totalChrTiles} Tiles @ ${fusedSpec.bitsPerPixelSprite}bpp)")
            }
        }

        return ChipFusionAssetOutputReport(
            fusedSpec = fusedSpec,
            hostSpec = hostSpec,
            donorChips = donors,
            interconnect = interconnect,
            baseMeshReport = baseMeshReport,
            fusedMeshReport = fusedMeshReport,
            baseSpriteReport = baseSpriteReport,
            fusedSpriteReport = fusedSpriteReport,
            triBudgetGainPercent = triGainPct,
            vramGainBytes = vramGain,
            everydayFusionReadout = everydayText,
            highEndTechnicalFusionReadout = technicalText,
            busBridgeRegisterDump = hexDump
        )
    }
}
