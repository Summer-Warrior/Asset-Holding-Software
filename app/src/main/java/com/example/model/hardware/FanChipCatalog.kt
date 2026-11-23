package com.example.model.hardware

import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData
import kotlin.math.roundToInt

data class FanChipProfile(
    val id: String,
    val chipName: String,
    val shortName: String,
    val communityCreators: String,
    val debutYear: Int,
    val baseHostSystemId: String,
    val standaloneSystemSpecId: String,
    val donorAttachmentId: String,
    val interconnect: FusionInterconnectMode,
    val architectureHeadline: String,
    val busHookMechanism: String,
    val preview3DImpactBadge: String,
    val preview2DImpactBadge: String,
    val everydayExplanation: String,
    val highEndTechnicalBreakdown: String,
    val signatureFeatures: List<String>
)

data class FanChipPreviewComparisonData(
    val profile: FanChipProfile,
    val stockHostSpec: ConsoleHardwareSpec,
    val fanEnhancedSpec: ConsoleHardwareSpec,
    val stockMeshReport: MeshHardwareReport,
    val fanMeshReport: MeshHardwareReport,
    val stockSpriteReport: SpriteHardwareReport?,
    val fanSpriteReport: SpriteHardwareReport?,
    val triBudgetMultiplierText: String,
    val vramMultiplierText: String,
    val scanlineSpriteGainText: String,
    val registerStreamDump: String
)

object FanChipCatalog {

    val fanDonorAttachments: List<DonorChipAttachment> = listOf(
        DonorChipAttachment(
            id = "donor_fan_msu1_fxpak",
            name = "MSU-1 (4GB Streamer) + SuperFX3 (43MHz FPGA GSU)",
            shortName = "MSU-1 + SuperFX3",
            originSystem = "SNES Homebrew / FXPak Pro Community Core (Near & ikari_01)",
            releaseYear = 2011,
            chipSpec = ChipSpec(
                name = "Community MSU-1 B-Bus Streamer + 42.95 MHz Zero-Wait FPGA GSU",
                role = "4 GB Uncompressed Tile/Audio Streamer + Overclocked 3D RISC Plotter",
                architecture = "8-Port B-Bus MMIO ($2000–$2007) + Single-Cycle 42.95 MHz GSU RISC + 256 KB SRAM",
                clockSpeed = "42.95 MHz (2× GSU-2, Zero Wait-States)",
                busAndProcess = "Cyclone IV / MAX 10 FPGA Cartridge Bus Bridge + SD/RAM DMA",
                technicalNotes = "Eliminates original Super FX ROM/SRAM bus wait-states (achieving 3.5× real-world polygon fill throughput) while MSU-1 streams 4 GB of uncompressed 4bpp/8bpp tiles directly into VRAM via VBLANK DMA."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
            triBudgetBonus = 360,
            triBudgetMultiplier = 4.2f,
            overridesVertexSnap = 2.0f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = 8,
            upgradesPaletteType = PaletteConstraintType.SNES_15BIT_8BPP,
            extraVramBytes = 262144L,
            extraTextureCacheBytes = 8192,
            scanlineSpriteMultiplier = 2.0f,
            donor3DSynthesisNote = "42.95 MHz zero-wait-state FPGA GSU plots 400+ Bayer-dithered and textured 3D triangles per frame into 256 KB single-cycle SRAM while MSU-1 streams uncompressed environment textures.",
            donor2DSynthesisNote = "MSU-1 streams uncompressed 8bpp (256-color) 15-bit BGR555 sprite sheets from a 4 GB address space straight into PPU VRAM every VBLANK."
        ),

        DonorChipAttachment(
            id = "donor_fan_atari_cdfj",
            name = "Melody DPC+ / CDFJ+ (70 MHz ARM7TDMI Fast-Fetch)",
            shortName = "CDFJ+ 70MHz ARM",
            originSystem = "Atari 2600 Homebrew Cartridge (batari, SpiceWare & Chris Walton)",
            releaseYear = 2017,
            chipSpec = ChipSpec(
                name = "Harmony / Melody CDFJ+ (NXP LPC2103 70 MHz ARM7TDMI)",
                role = "32-Bit RISC Co-Processor & 2-Cycle 6507 'Fast-Fetch' Bus Stuffer",
                architecture = "32-bit ARM7TDMI + 34 Hardware Data Stream Pointers + 3-Ch Waveform DAC",
                clockSpeed = "70.0 MHz (58× Atari 6507 Clock!)",
                busAndProcess = "32 KB Flash + 8 KB Fast SRAM + 2-Cycle `LDA #imm` Cartridge Bus Injection",
                technicalNotes = "Runs 3D projection and sprite sorting in 32-bit C/ARM code during VBLANK/Overscan, then intercepts 6507 `LDA #immediate` opcodes during active scanlines to feed TIA `GRP0`/`GRP1`/`COLUP` bytes in just 2 CPU cycles!"
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.SVP_DSP_FLAT,
            triBudgetBonus = 180,
            triBudgetMultiplier = 5.5f,
            overridesVertexSnap = 3.0f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = 4,
            upgradesPaletteType = null,
            extraVramBytes = 8192L,
            extraTextureCacheBytes = 2048,
            scanlineSpriteMultiplier = 4.0f,
            donor3DSynthesisNote = "70 MHz ARM7TDMI executes 32-bit fixed-point 3D matrix transforms and scanline edge tables in 8 KB SRAM, stuffing the host display registers every scanline.",
            donor2DSynthesisNote = "34 independent fractional-increment Fast-Fetch data streams allow arbitrary per-scanline sprite color shifts and 4×–8× flicker-free sprite multiplexing."
        ),

        DonorChipAttachment(
            id = "donor_fan_nes_rainbow",
            name = "Rainbow FPGA Mapper + UNROM-512 (8×1 ExRAM + WiFi/DSP)",
            shortName = "Rainbow FPGA Mapper",
            originSystem = "NES Homebrew Cartridge (Broke Studio / NESdev Community)",
            releaseYear = 2020,
            chipSpec = ChipSpec(
                name = "Broke Studio Rainbow FPGA Mapper + ESP32 Co-Processor",
                role = "8×1 Per-Scanline Attribute Expander, 64KB CHR-RAM & Hardware Math Unit",
                architecture = "FPGA Extended PPU Bus Interceptor + 4 KB Dual-Port ExRAM + 16×16 HW Multiplier",
                clockSpeed = "40.0 MHz FPGA / 240 MHz Co-CPU",
                busAndProcess = "512 KB–1 MB PRG Flash + 64 KB Banked CHR-RAM + Scanline Window IRQ",
                technicalNotes = "Intercepts PPU nametable/attribute fetches to replace the NES's coarse 16×16 pixel attribute grid with ultra-fine 8×1 scanline color attributes and 16,384 simultaneously addressable CHR tiles."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.CX4_TRIG_WIREFRAME,
            triBudgetBonus = 130,
            triBudgetMultiplier = 3.2f,
            overridesVertexSnap = 2.5f,
            grantsZBuffer = false,
            upgradesBitsPerPixel = 4,
            upgradesPaletteType = null,
            extraVramBytes = 65536L,
            extraTextureCacheBytes = 4096,
            scanlineSpriteMultiplier = 2.0f,
            donor3DSynthesisNote = "Hardware 16×16 multiplier and dual-port 64 KB CHR-RAM enable 60 FPS double-buffered 3D vector/polygon plotting without waiting for narrow NES VBLANK windows.",
            donor2DSynthesisNote = "8×1 pixel attribute granularity and 64 KB banked CHR-RAM let background and sprite layers combine up to 25 colors with zero 16×16 attribute clash."
        ),

        DonorChipAttachment(
            id = "donor_fan_rp2040_pio",
            name = "PicoCart RP2040 Dual Cortex-M0+ PIO Bus Accelerator",
            shortName = "RP2040 PIO 200MHz",
            originSystem = "Game Boy / 8-Bit Community Cartridge (RP2040 PIO Bus Sniffer)",
            releaseYear = 2022,
            chipSpec = ChipSpec(
                name = "Raspberry Pi RP2040 Dual ARM Cortex-M0+ & 8-State-Machine PIO",
                role = "15ns GPIO Bus Emulator + 264 KB SRAM Software 3D Rasterizer",
                architecture = "Dual 32-bit Cortex-M0+ @ 200 MHz + Single-Cycle Interpolator + PIO DMA",
                clockSpeed = "200.0 MHz",
                busAndProcess = "264 KB Multi-Bank On-Chip SRAM + 16 MB QSPI Flash + 5V Level-Shifted PIO Bus",
                technicalNotes = "PIO state machines respond to vintage 8-bit memory reads in <15ns while Core 1 renders full 3D raycast/polygon scenes in 264 KB SRAM and converts them on the fly into 2bpp/4bpp planar tiles."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
            triBudgetBonus = 310,
            triBudgetMultiplier = 6.0f,
            overridesVertexSnap = 1.5f,
            grantsZBuffer = true,
            upgradesBitsPerPixel = null,
            upgradesPaletteType = null,
            extraVramBytes = 270336L,
            extraTextureCacheBytes = 16384,
            scanlineSpriteMultiplier = 2.5f,
            donor3DSynthesisNote = "RP2040's hardware affine texture interpolator (`SIO INTERP0/1`) and 200 MHz dual cores rasterize Z-buffered 3D meshes into 264 KB SRAM and stream finished planar tiles to the host PPU.",
            donor2DSynthesisNote = "Hardware SIO interpolators perform real-time Mode-7 style affine sprite rotation and scaling in cartridge SRAM at 60 FPS."
        ),

        DonorChipAttachment(
            id = "donor_fan_ps1_pgxp",
            name = "Community PGXP (Parallel Geometry Expression Precision)",
            shortName = "PGXP Float GTE",
            originSystem = "PlayStation Homebrew / FPGA Silicon Extension (iCatButler)",
            releaseYear = 2016,
            chipSpec = ChipSpec(
                name = "PGXP 32-Bit IEEE-754 Shadow GTE + Perspective-Correct Divider",
                role = "Sub-Pixel Vertex Stabilizer, 1/W Texture Corrector & 24-Bit Z-Buffer",
                architecture = "Parallel 32-bit Floating-Point COP2 Shadow Registers + Perspective UV Divider",
                clockSpeed = "67.73 MHz (2× PS1 GTE Sync)",
                busAndProcess = "Shadow Bus Snooper on MIPS R3000A ↔ GTE / CXD8561 GPU FIFO",
                technicalNotes = "Tracks every 16-bit integer GTE `RTPS`/`RTPT` operation with a parallel 32-bit float transform and `1/W` depth value—completely eliminating PS1 vertex snap wobble, polygon seam gaps, and affine texture warping!"
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.POWERVR_TBDR_DREAMCAST,
            triBudgetBonus = 650,
            triBudgetMultiplier = 2.4f,
            overridesVertexSnap = 0f, // Zero wobble!
            grantsZBuffer = true,
            upgradesBitsPerPixel = 16,
            upgradesPaletteType = PaletteConstraintType.RGB555_15BIT,
            extraVramBytes = 2097152L,
            extraTextureCacheBytes = 16384,
            scanlineSpriteMultiplier = 2.0f,
            donor3DSynthesisNote = "Replaces 16-bit integer screen snapping and Ordering Table sort-popping with 32-bit floating-point sub-pixel vertices, true `1/W` perspective-correct textures, and a 24-bit Z-buffer.",
            donor2DSynthesisNote = "Enables sub-pixel smooth 2D sprite scaling/rotation and expands PS1 VRAM from 1 MB to 3 MB for uncompressed 8bpp/15-bit sprite sheets."
        ),

        DonorChipAttachment(
            id = "donor_fan_n64_f3dex3",
            name = "F3DEX3 Community Microcode + SummerCart64 FPGA DMA",
            shortName = "F3DEX3 + SC64 FPGA",
            originSystem = "Nintendo 64 Homebrew (Sauraen, LibDragon & SummerCart64)",
            releaseYear = 2023,
            chipSpec = ChipSpec(
                name = "F3DEX3 Custom RSP Microcode + Lattice ECP5 FPGA 64MB SDRAM Bridge",
                role = "Hardware Phong Specular/Toon RSP Pipeline & 50 MB/s Multi-Tile TMEM Streamer",
                architecture = "Rewritten 128-bit SIMD RSP IMEM Microcode + 64 MB DDR2/SDRAM Cartridge DMA",
                clockSpeed = "62.5 MHz RCP + 100 MHz FPGA SDRAM Controller",
                busAndProcess = "50 MB/s PI Burst DMA + Automated RDP `gDPLoadMultiTile` Streaming",
                technicalNotes = "Optimizes RSP SIMD vector loops to add per-vertex specular highlights, rim/cel shading, and occlusion plane culling (+65% polygon throughput) while streaming multi-tile textures to mask the 4 KB TMEM limit."
            ),
            grantsHardware3D = true,
            preferred3DProfile = Hardware3DRenderProfile.GAMECUBE_TEV_STAGES,
            triBudgetBonus = 750,
            triBudgetMultiplier = 2.5f,
            overridesVertexSnap = 0f,
            grantsZBuffer = true,
            upgradesBitsPerPixel = 16,
            upgradesPaletteType = PaletteConstraintType.TRUECOLOR_24BIT,
            extraVramBytes = 67108864L,
            extraTextureCacheBytes = 32768,
            scanlineSpriteMultiplier = 2.5f,
            donor3DSynthesisNote = "Upgrades N64 RCP with F3DEX3 hardware specular/cel shading, occlusion plane culling (+65% tri budget), and automated multi-tile DMA streaming from 64 MB cartridge SDRAM.",
            donor2DSynthesisNote = "Streams high-resolution 16-bit/32-bit RGBA sprite strips automatically across RDP display lists at 60 FPS."
        )
    )

    val allFanChipProfiles: List<FanChipProfile> = listOf(
        FanChipProfile(
            id = "fanchip_snes_msu1_fxpak",
            chipName = "MSU-1 + SuperFX3 (FXPak Pro FPGA)",
            shortName = "MSU-1 + SuperFX3",
            communityCreators = "Near (byuu) & ikari_01 / RedGuy",
            debutYear = 2011,
            baseHostSystemId = "snes",
            standaloneSystemSpecId = "fan_snes_msu1_fxpak",
            donorAttachmentId = "donor_fan_msu1_fxpak",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM,
            architectureHeadline = "4 GB B-Bus Linear Data/PCM Streamer + 42.95 MHz Zero-Wait-State FPGA GSU",
            busHookMechanism = "Maps 8 MMIO registers onto SNES B-Bus ($2000–$2007) for instant 4 GB DMA streaming while running an FPGA GSU core at 42.95 MHz with single-cycle 256 KB SRAM.",
            preview3DImpactBadge = "3.5× Fillrate • 420+ Tris/Frame • Zero Wait-State GSU",
            preview2DImpactBadge = "4 GB Streaming • 8bpp 256-Color Direct DMA • CD PCM",
            everydayExplanation = "Created by legendary emulator author Near (byuu) and FPGA engineer ikari_01, MSU-1 + SuperFX3 removes the two biggest walls of the 1990s Super Nintendo: tiny 4 MB cartridge sizes and slow Super FX memory stalls. Your 3D models render at over triple the polygon budget without lag, and your 2D sprites can stream hundreds of uncompressed 256-color animation frames straight from SD/flash memory alongside CD-quality music!",
            highEndTechnicalBreakdown = "Hooks `$2000–$2007` (`MSU_STATUS`, `MSU_READ`, `MSU_SEEK`) on the 8-bit S-PPU B-Bus so Fixed-Address DMA Channel 0 transfers raw 4bpp/8bpp planar tile banks from a 32-bit (4 GB) byte offset straight into `$2118` (`VMDATAL/H`) at 2.68 MB/s. Simultaneously, the FPGA GSU-3 executes `PLOT`/`CMODE` dithered rasterization at 42.95 MHz with 1-cycle SRAM access (vs 3–5 cycles on original 1993 GSU-1/2).",
            signatureFeatures = listOf(
                "4 GB (32-bit) Linear Data Streaming via $2000–$2007 B-Bus",
                "42.95 MHz Single-Cycle SRAM SuperFX3 3D Polygon Plotter",
                "Uncompressed 8bpp (256-color) VBLANK Sprite Tile Streaming",
                "44.1 kHz 16-Bit Stereo Lossless PCM Audio Mixing"
            )
        ),

        FanChipProfile(
            id = "fanchip_atari_cdfj",
            chipName = "Harmony / Melody DPC+ & CDFJ+ (70 MHz ARM)",
            shortName = "Atari CDFJ+ ARM",
            communityCreators = "batari (Fred Quimby), SpiceWare (Darrell Spice Jr.) & Chris Walton",
            debutYear = 2017,
            baseHostSystemId = "atari_2600",
            standaloneSystemSpecId = "fan_atari_cdfj_arm",
            donorAttachmentId = "donor_fan_atari_cdfj",
            interconnect = FusionInterconnectMode.DIRECT_COPROCESSOR_BUS,
            architectureHeadline = "70 MHz 32-Bit ARM7TDMI Co-Processor + 34 'Fast-Fetch' Scanline Streams",
            busHookMechanism = "Intercepts 6507 `LDA #immediate` (0xA9) fetches on the cartridge bus, replacing the operand byte on the fly from 34 hardware-indexed ARM SRAM queues in just 2 CPU cycles.",
            preview3DImpactBadge = "70 MHz 3D Math • 200+ Tris • Scanline Span Plotter",
            preview2DImpactBadge = "Single-Line Multicolor Sprites • 4× Multiplexed OAM",
            everydayExplanation = "The 1977 Atari 2600 had only 128 bytes of RAM and a 1.19 MHz processor. Fan engineers put a 70 MHz 32-bit ARM chip inside the cartridge that acts like a firehose of graphics data: during the TV beam's invisible return, the ARM chip calculates full 3D shapes and dozens of colorful sprites, then spoon-feeds the exact color and pixel bytes to the Atari chip on every single TV line!",
            highEndTechnicalBreakdown = "On a stock 6507, loading a sprite byte via `LDA (zp),Y` + `STA GRP0` takes 8 cycles (24 TIA color clocks), leaving almost no time per 76-cycle scanline. CDFJ+ overrides opcode `0xA9` (`LDA #imm`, 2 cycles) using 34 fractional-increment Fast-Fetch streams (`DS0DATA`–`DS33DATA` + `FASTJMP`), allowing a 5-cycle `LDA #DSx / STA GRP0` loop that updates both players, missiles, playfield, and colors on every scanline.",
            signatureFeatures = listOf(
                "70 MHz 32-Bit ARM7TDMI C/Thumb Game & 3D Math Execution",
                "2-Cycle `LDA #Immediate` Fast-Fetch Scanline Bus Stuffing",
                "34 Fractional-Step Data Streams for Hardware Sprite Scaling",
                "3-Voice Digital Waveform + Sample Audio Synthesizer"
            )
        ),

        FanChipProfile(
            id = "fanchip_nes_rainbow",
            chipName = "Broke Studio Rainbow FPGA Mapper + UNROM-512",
            shortName = "NES Rainbow FPGA",
            communityCreators = "Antoine Gohin (Broke Studio) & NESdev Homebrew Community",
            debutYear = 2020,
            baseHostSystemId = "nes_2c02",
            standaloneSystemSpecId = "fan_nes_rainbow_fpga",
            donorAttachmentId = "donor_fan_nes_rainbow",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM,
            architectureHeadline = "8×1 Fine Attribute ExRAM + 64 KB Dual-Port CHR-RAM + FPGA Math/IRQ",
            busHookMechanism = "Watches PPU address lines A0–A13 during tile fetches, substituting 4 KB Dual-Port FPGA ExRAM bytes so every 8×1 pixel sliver gets its own palette + 14-bit tile index.",
            preview3DImpactBadge = "Dual-Port 64KB CHR-RAM • 60 FPS Vector/Poly Plot",
            preview2DImpactBadge = "8×1 Attribute Granularity • Zero Color Clash • 16K Tiles",
            everydayExplanation = "Standard NES games could only change background colors in blocky 16×16 pixel squares and could only see 256 tiles at once. The fan-made Rainbow FPGA Mapper upgrades the NES so every tiny 8×1 strip of pixels can have its own palette, adds 64 KB of instant dual-port video RAM for smooth 3D wireframe/polygon drawing, and includes a built-in math accelerator!",
            highEndTechnicalBreakdown = "Extends Nintendo's MMC5 concept via modern FPGA logic: provides 4 KB of Dual-Port Extended RAM accessible by the 2A03 CPU at `0x5000–0x5FFF` even during active PPU rendering (no VBLANK wait!), enables 8×1 scanline-granular attribute palettes, expands CHR addressing to 16,384 tiles, and adds a cycle-accurate scanline/window split IRQ counter.",
            signatureFeatures = listOf(
                "8×1 Pixel Attribute Granularity (Eliminates 16×16 Color Clash)",
                "Dual-Port 64 KB CHR-RAM Writable During Active Scanlines",
                "Hardware 16×16 Multiplier & 32÷16 Divider Registers",
                "Cycle-Accurate Window Split & Scanline IRQ Generator"
            )
        ),

        FanChipProfile(
            id = "fanchip_gb_rp2040_pio",
            chipName = "PicoCart RP2040 Dual Cortex-M0+ PIO Accelerator",
            shortName = "GB RP2040 PIO",
            communityCreators = "Open-Source Game Boy & RP2040 Homebrew Community",
            debutYear = 2022,
            baseHostSystemId = "gameboy_dmg",
            standaloneSystemSpecId = "fan_gb_rp2040_pio",
            donorAttachmentId = "donor_fan_rp2040_pio",
            interconnect = FusionInterconnectMode.CARTRIDGE_DUAL_PORT_SRAM,
            architectureHeadline = "200 MHz Dual Cortex-M0+ + 8 PIO State Machines + Affine SIO Interpolators",
            busHookMechanism = "Uses RP2040 Programmable I/O (PIO) state machines to answer 4.19 MHz Game Boy cartridge bus reads in <15ns while Core 1 rasterizes 3D framebuffers into 264 KB SRAM.",
            preview3DImpactBadge = "200 MHz Dual-Core 3D • Z-Buffered Dithered Meshes",
            preview2DImpactBadge = "Hardware SIO Affine Sprite Rotator • 264 KB Tile Stream",
            everydayExplanation = "By putting a tiny, $1 Raspberry Pi RP2040 microcontroller on a Game Boy cartridge PCB, homebrew creators gave the 1989 Game Boy a 200 MHz dual-core 3D graphics engine! One core talks to the Game Boy's cartridge pins at lightning speed, while the second core renders solid, shaded 3D models and rotating sprites into 2bpp Game Boy tiles at 60 frames per second.",
            highEndTechnicalBreakdown = "Core 0 services Game Boy `/RD`, `/WR`, `/CS`, and `A0–A15` via deterministic 200 MHz PIO state machines + DMA channels, exposing a lock-free 16 KB window at `0xA000–0xBFFF` Cartridge RAM. Core 1 uses the RP2040's single-cycle `SIO INTERP0/INTERP1` hardware affine texture interpolators to rasterize Z-buffered 3D triangles and pack 2bpp interleaved bitplanes directly for Game Boy VRAM DMA.",
            signatureFeatures = listOf(
                "Deterministic <15ns PIO State-Machine Bus Emulation",
                "Single-Cycle Hardware `SIO INTERP0/1` Affine Texture Stepper",
                "264 KB On-Chip Multi-Bank SRAM for Z-Buffered 3D Rendering",
                "Real-Time Linear-to-2bpp Interleaved Bitplane Packing"
            )
        ),

        FanChipProfile(
            id = "fanchip_ps1_pgxp",
            chipName = "Community PGXP (32-Bit Float GTE + Perspective UV)",
            shortName = "PS1 PGXP Float",
            communityCreators = "iCatButler & PlayStation Community Silicon Project",
            debutYear = 2016,
            baseHostSystemId = "ps1",
            standaloneSystemSpecId = "fan_ps1_pgxp_float",
            donorAttachmentId = "donor_fan_ps1_pgxp",
            interconnect = FusionInterconnectMode.DIRECT_COPROCESSOR_BUS,
            architectureHeadline = "Parallel 32-Bit Float GTE Shadow Pipeline + True 1/W Perspective Rasterizer",
            busHookMechanism = "Mirrors every MIPS R3000A ↔ COP2 (GTE) register transfer (`MTC2`/`LWC2`/`RTPS`/`RTPT`) in a 32-bit IEEE-754 floating-point shadow file and injects sub-pixel X/Y/W coordinates into the GPU.",
            preview3DImpactBadge = "0.0 Vertex Wobble • Perspective-Correct UV • 24-Bit Z-Buf",
            preview2DImpactBadge = "Sub-Pixel Affine Quads • 3 MB Expanded CLUT VRAM",
            everydayExplanation = "Everyone recognizes the classic PlayStation 1's wobbly, swimming polygons—caused by Sony's GTE chip rounding every 3D corner to a whole pixel and skipping perspective texture math. The community-engineered PGXP architecture runs a parallel high-precision decimal math pipeline alongside the PS1 chip, locking vertices in place with zero wobble and crystal-clear textures while keeping the unmistakable PS1 color and lighting vibe!",
            highEndTechnicalBreakdown = "Intercepts COP2 instructions (`RTPS` 0x180001, `RTPT` 0x280030, `MVMVA` 0x400012) and tracks 32-bit floating-point `{sx, sy, sz, sw}` tuples indexed by Scratchpad/RAM destination addresses. When the CPU submits `POLY_FT3`/`POLY_GT4` packets to GPU port `GP0` (`$1F801810`), PGXP substitutes IEEE-754 sub-pixel vertices, performs per-pixel `(u/w, v/w) / (1/w)` perspective division, and resolves depth via a 24-bit Z-buffer.",
            signatureFeatures = listOf(
                "Eliminates 16-Bit Integer Screen-Space Vertex Snap Wobble",
                "True Per-Pixel `1/W` Perspective-Correct UV Texture Mapping",
                "Hardware 24-Bit Z-Buffer (No Ordering Table Polygon Popping)",
                "Preserves Authentic PS1 15-Bit BGR555 & 4×4 Dither Aesthetics"
            )
        ),

        FanChipProfile(
            id = "fanchip_n64_f3dex3",
            chipName = "F3DEX3 Community Microcode + SummerCart64 FPGA",
            shortName = "N64 F3DEX3 + SC64",
            communityCreators = "Sauraen (F3DEX3), Polprzewodnikowiec (SC64) & LibDragon",
            debutYear = 2023,
            baseHostSystemId = "n64",
            standaloneSystemSpecId = "fan_n64_f3dex3_sc64",
            donorAttachmentId = "donor_fan_n64_f3dex3",
            interconnect = FusionInterconnectMode.DIRECT_COPROCESSOR_BUS,
            architectureHeadline = "Specular/Cel RSP Microcode + Occlusion Culling + 50 MB/s FPGA SDRAM DMA",
            busHookMechanism = "Loads custom 4 KB RSP IMEM vector microcode (`F3DEX3`) paired with a Lattice ECP5 FPGA cartridge controller streaming multi-tile textures at 50 MB/s over the N64 Peripheral Interface (PI) bus.",
            preview3DImpactBadge = "+65% Poly Throughput • Hardware Specular/Cel • Multi-Tile",
            preview2DImpactBadge = "Automated Multi-Tile TMEM Streamer • 64 MB Fast SDRAM",
            everydayExplanation = "Because the Nintendo 64's graphics processor runs programmable software ('microcode'), N64 homebrew engineers rewrote Nintendo's original 1990s code from scratch! Combined with the open-source SummerCart64 FPGA board, F3DEX3 unlocks GameCube-style shiny specular highlights, comic-book cel shading, and 65% more triangles on screen while streaming larger textures effortlessly.",
            highEndTechnicalBreakdown = "Replaces SGI's `F3DEX2` inside the 4 KB RSP Instruction Memory (IMEM) with tightly scheduled 8-wide 16-bit SIMD vector instructions (`VMULF`, `VMUDM`) that compute half-angle Phong specular dot products, Fresnel rim lighting, and software occlusion-plane culling before triangles ever hit the RDP rasterizer. Meanwhile, SummerCart64's ECP5 FPGA streams 32×32 texture tiles at 50 MB/s to keep the 4 KB TMEM continuously fed.",
            signatureFeatures = listOf(
                "Custom RSP SIMD Microcode w/ Hardware Phong Specular & Cel Shading",
                "Occlusion-Plane Frustum Culling (+65% Scene Triangle Budget)",
                "50 MB/s Lattice ECP5 FPGA SDRAM Burst PI DMA Streaming",
                "Automated Multi-Tile TMEM Slicing for High-Res Textures"
            )
        )
    )

    val standaloneFanSystemSpecs: List<ConsoleHardwareSpec> by lazy {
        allFanChipProfiles.map { profile ->
            val host = ConsoleHardwareCatalog.allSystems.first { it.id == profile.baseHostSystemId }
            val donor = fanDonorAttachments.first { it.id == profile.donorAttachmentId }
            val synthesized = ChipFusionEngine.synthesizeFusedSystemSpec(
                hostSpec = host,
                donors = listOf(donor),
                interconnect = profile.interconnect
            )
            synthesized.copy(
                id = profile.standaloneSystemSpecId,
                name = "${host.shortName} + ${profile.chipName}",
                shortName = profile.shortName,
                releaseYear = profile.debutYear,
                manufacturer = "Fan & Homebrew Silicon (${profile.communityCreators.substringBefore(",")})",
                category = HardwareCategory.FAN_CHIP,
                spritePipelineExplanation = "[FAN CHIP 2D PIPELINE — ${profile.chipName}]: ${donor.donor2DSynthesisNote} (${profile.busHookMechanism})",
                model3DPipelineExplanation = "[FAN CHIP 3D PIPELINE — ${profile.chipName}]: ${donor.donor3DSynthesisNote} (${profile.busHookMechanism})"
            )
        }
    }

    fun getProfileById(id: String): FanChipProfile {
        return allFanChipProfiles.find { it.id == id } ?: allFanChipProfiles.first()
    }

    fun evaluateFanChipPreviewData(
        mesh: Mesh3D,
        spriteSheet: SpriteSheetData,
        profile: FanChipProfile,
        showTileGrid: Boolean = true
    ): FanChipPreviewComparisonData {
        val stockHost = ConsoleHardwareCatalog.getById(profile.baseHostSystemId)
        val fanSpec = standaloneFanSystemSpecs.first { it.id == profile.standaloneSystemSpecId }

        val stockMeshReport = HardwareAssetSimulator.analyzeMeshOnHardware(mesh, stockHost)
        val fanMeshReport = HardwareAssetSimulator.analyzeMeshOnHardware(mesh, fanSpec)

        val firstFrame = spriteSheet.frames.firstOrNull()
        val stockSpriteReport = firstFrame?.let {
            HardwareAssetSimulator.simulateSpriteOnHardware(it, stockHost, showTileGrid)
        }
        val fanSpriteReport = firstFrame?.let {
            HardwareAssetSimulator.simulateSpriteOnHardware(it, fanSpec, showTileGrid)
        }

        val triMult = String.format(
            "%.1f× (%d → %d tris)",
            fanSpec.recommendedModelTriBudget.toFloat() / stockHost.recommendedModelTriBudget.coerceAtLeast(1),
            stockHost.recommendedModelTriBudget,
            fanSpec.recommendedModelTriBudget
        )

        val vramMult = "${stockHost.vramBytes / 1024} KB → ${fanSpec.vramBytes / 1024} KB"
        val scanlineGain = "${stockHost.maxSpritesPerScanline} → ${fanSpec.maxSpritesPerScanline} sprites/line"

        val regDump = buildString {
            append("// FAN & HOMEBREW SILICON BUS HOOK TELEMETRY [${profile.shortName.uppercase()}]\n")
            append("CREATORS          = \"${profile.communityCreators}\"\n")
            append("HOST_TARGET       = ${stockHost.shortName} (${stockHost.cpuSummary.substringBefore(" @")})\n")
            append("BUS_INTERCEPT     = ${profile.interconnect.badge}\n")
            append("3D_TRI_BUDGET     = ${stockHost.recommendedModelTriBudget} -> ${fanSpec.recommendedModelTriBudget} Tris (${fanMeshReport.assetBudgetPercent.roundToInt()}% Load)\n")
            append("VERTEX_SNAP_GRID  = ${stockHost.vertexSnapGrid} -> ${fanSpec.vertexSnapGrid} (Z-Buf: ${fanSpec.hasZBuffer})\n")
            if (fanSpriteReport != null) {
                append("SPRITE_CHR_STREAM = ${fanSpriteReport.chrVramBytes} Bytes (${fanSpriteReport.totalChrTiles} Tiles @ ${fanSpec.bitsPerPixelSprite}bpp)")
            }
        }

        return FanChipPreviewComparisonData(
            profile = profile,
            stockHostSpec = stockHost,
            fanEnhancedSpec = fanSpec,
            stockMeshReport = stockMeshReport,
            fanMeshReport = fanMeshReport,
            stockSpriteReport = stockSpriteReport,
            fanSpriteReport = fanSpriteReport,
            triBudgetMultiplierText = triMult,
            vramMultiplierText = vramMult,
            scanlineSpriteGainText = scanlineGain,
            registerStreamDump = regDump
        )
    }
}
