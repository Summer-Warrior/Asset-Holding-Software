package com.example.model.hardware

enum class HardwareCategory(val label: String) {
    ALL("All Systems"),
    CONSOLE("Home Consoles"),
    HANDHELD("Handhelds"),
    EXPANSION_CHIP("Silicon & Chips"),
    FAN_CHIP("Fan & Homebrew Chips")
}

enum class HardwareEra(val label: String) {
    ERA_8BIT("8-Bit & Early"),
    ERA_16BIT("16-Bit & Co-Chips"),
    ERA_32_64BIT("32/64-Bit 3D Dawn"),
    ERA_128BIT("128-Bit 6th Gen"),
    ERA_MODERN("HD & Modern APU")
}

enum class Hardware3DRenderProfile(val badge: String) {
    NONE_2D_WIREFRAME("Software 8/16-Bit Wireframe / Sprite Impostor"),
    SUPER_FX_DITHER_FLAT("Super FX GSU Plotter (4x4 Bayer Dither + 4bpp/8bpp)"),
    CX4_TRIG_WIREFRAME("Capcom CX4 DSP Trigonometric Vector / Wireframe"),
    SVP_DSP_FLAT("Sega SVP SSP1601 23MHz Flat Polygon Tile Window"),
    PS1_GTE_AFFINE_WOBBLE("PS1 GTE Fixed-Point (Vertex Snap Wobble + 15-bit Ordered Dither)"),
    SATURN_VDP1_QUAD_MESH("Saturn VDP1 Distorted Quads + Screen-Door Stipple Mesh"),
    N64_RCP_TMEM_3POINT("N64 RCP (Z-Buffered Sub-Pixel + 3-Point Filter + Horizon Fog)"),
    NDS_FIXED_TOON_2048("NDS ARM9 3D Engine (Fixed-Point + Toon Table Shading, 2048 Tri Cap)"),
    POWERVR_TBDR_DREAMCAST("Dreamcast PowerVR2 CLX2 (Tile-Based Deferred + Hardware Specular)"),
    PS2_EE_GS_EDRAM("PS2 Emotion Engine VU0/VU1 + GS 4MB eDRAM High-Fillrate"),
    GAMECUBE_TEV_STAGES("GameCube/Wii Flipper/Hollywood 16-Stage TEV Combiner"),
    MODERN_PBR_SHADER("Unified Programmable Mesh & PBR Shader Pipeline")
}

enum class PaletteConstraintType {
    ATARI_2600_NTSC_SCANLINE,
    C64_16_MULTICOLOR_WIDE,
    NES_2C02_2BPP_54,
    DMG_4_OLIVE,
    VIRTUAL_BOY_4_RED,
    MASTER_SYSTEM_6bit,
    GENESIS_9BIT_4BPP,
    SNES_15BIT_4BPP,
    SNES_15BIT_8BPP,
    RGB555_15BIT,
    TRUECOLOR_24BIT
}

data class ChipSpec(
    val name: String,
    val role: String,
    val architecture: String,
    val clockSpeed: String,
    val busAndProcess: String,
    val technicalNotes: String
)

data class ConsoleHardwareSpec(
    val id: String,
    val name: String,
    val shortName: String,
    val releaseYear: Int,
    val manufacturer: String,
    val category: HardwareCategory,
    val era: HardwareEra,
    val nativeResolution: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val cpuSummary: String,
    val gpuOrPpuSummary: String,
    val memorySummary: String,
    val mainRamBytes: Long,
    val vramBytes: Long,
    val textureCacheBytes: Int, // e.g. 4096 for N64 TMEM, 2048 for PS1
    val bitsPerPixelSprite: Int,
    val maxSpriteColorsPerPalette: Int,
    val maxOnScreenColors: Int,
    val masterPaletteSize: Int,
    val maxSpritesTotal: Int,
    val maxSpritesPerScanline: Int,
    val spriteTileSize: String,
    val targetSpriteResolution: Int, // Practical character sprite px height
    val hasHardware3D: Boolean,
    val recommendedModelTriBudget: Int,
    val maxSceneTrisPerFrame: Int,
    val vertexSnapGrid: Float, // > 0 simulates integer screen-space snap (e.g. PS1 GTE)
    val hasZBuffer: Boolean,
    val renderProfile3D: Hardware3DRenderProfile,
    val paletteType: PaletteConstraintType,
    val chips: List<ChipSpec>,
    val spritePipelineExplanation: String,
    val model3DPipelineExplanation: String
)

object ConsoleHardwareCatalog {

    val allSystems: List<ConsoleHardwareSpec> = listOf(
        // ==================== 8-BIT & EARLY ====================
        ConsoleHardwareSpec(
            id = "atari_2600",
            name = "Atari 2600 (VCS)",
            shortName = "Atari 2600",
            releaseYear = 1977,
            manufacturer = "Atari",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_8BIT,
            nativeResolution = "160 × 192 (NTSC Scanline Driven)",
            screenWidth = 160,
            screenHeight = 192,
            cpuSummary = "MOS Technology 6507 @ 1.19 MHz",
            gpuOrPpuSummary = "TIA (Television Interface Adapter 1A)",
            memorySummary = "128 Bytes RIOT RAM (No Framebuffer / No VRAM)",
            mainRamBytes = 128L,
            vramBytes = 128L,
            textureCacheBytes = 0,
            bitsPerPixelSprite = 1,
            maxSpriteColorsPerPalette = 2,
            maxOnScreenColors = 128,
            masterPaletteSize = 128,
            maxSpritesTotal = 5,
            maxSpritesPerScanline = 2,
            spriteTileSize = "8×N 1-bit Scanline Register (GRP0/GRP1)",
            targetSpriteResolution = 16,
            hasHardware3D = false,
            recommendedModelTriBudget = 16,
            maxSceneTrisPerFrame = 24,
            vertexSnapGrid = 8f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.NONE_2D_WIREFRAME,
            paletteType = PaletteConstraintType.ATARI_2600_NTSC_SCANLINE,
            chips = listOf(
                ChipSpec(
                    name = "MOS 6507",
                    role = "Main CPU",
                    architecture = "8-bit MOS 6502 Reduced-Pinout",
                    clockSpeed = "1.19 MHz",
                    busAndProcess = "8-bit Data / 13-bit Address Bus (8 KB Addressable)",
                    technicalNotes = "Lacks NMI/IRQ pins; CPU must manually synchronize to TIA horizontal blank via WSYNC strobe."
                ),
                ChipSpec(
                    name = "Atari TIA (CO10444D)",
                    role = "Line Buffer Video & 2-Ch Audio",
                    architecture = "Custom Shift-Register Scanline Generator",
                    clockSpeed = "3.58 MHz Color Clocks (3× CPU)",
                    busAndProcess = "20-bit Playfield + two 8-bit Player Sprites + 2 Missiles + 1 Ball",
                    technicalNotes = "Zero VRAM framebuffer. CPU races the electron beam ('Racing the Beam') to rewrite GRP0/GRP1 and COLUP0/COLUP1 registers every scanline."
                ),
                ChipSpec(
                    name = "MOS 6532 RIOT",
                    role = "RAM, I/O & Interval Timer",
                    architecture = "128-Byte Static RAM + Programmable Timer",
                    clockSpeed = "1.19 MHz",
                    busAndProcess = "128 Bytes total system memory",
                    technicalNotes = "Holds stack, game state variables, and dynamic sprite pointers in just 128 bytes."
                )
            ),
            spritePipelineExplanation = "Sprites are 8-pixel wide 1-bit shift registers (GRP0, GRP1) stretched horizontally by 1x/2x/4x NUSIZ registers. Color can be changed per scanline via CPU kernel.",
            model3DPipelineExplanation = "No framebuffer exists; 3D requires calculating 2D projection endpoints during VBLANK and racing the beam with playfield/missile registers."
        ),

        ConsoleHardwareSpec(
            id = "c64",
            name = "Commodore 64",
            shortName = "C64 (VIC-II)",
            releaseYear = 1982,
            manufacturer = "Commodore",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_8BIT,
            nativeResolution = "320 × 200 (Hi-Res) / 160 × 200 (2:1 Multicolor)",
            screenWidth = 160,
            screenHeight = 200,
            cpuSummary = "MOS Technology 6510 @ 1.023 MHz (NTSC)",
            gpuOrPpuSummary = "MOS VIC-II 6567 (NTSC) / 6569 (PAL)",
            memorySummary = "64 KB DRAM + 1 KB × 4-bit Color Static RAM",
            mainRamBytes = 65536L,
            vramBytes = 16384L,
            textureCacheBytes = 0,
            bitsPerPixelSprite = 2,
            maxSpriteColorsPerPalette = 4,
            maxOnScreenColors = 16,
            masterPaletteSize = 16,
            maxSpritesTotal = 8,
            maxSpritesPerScanline = 8,
            spriteTileSize = "24×21 (Hi-Res 1bpp) or 12×21 (Multicolor 2bpp Wide)",
            targetSpriteResolution = 21,
            hasHardware3D = false,
            recommendedModelTriBudget = 32,
            maxSceneTrisPerFrame = 48,
            vertexSnapGrid = 6f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.NONE_2D_WIREFRAME,
            paletteType = PaletteConstraintType.C64_16_MULTICOLOR_WIDE,
            chips = listOf(
                ChipSpec(
                    name = "MOS 6510",
                    role = "Main CPU",
                    architecture = "8-bit 6502 w/ 6-bit Internal I/O Port",
                    clockSpeed = "1.023 MHz (NTSC) / 0.985 MHz (PAL)",
                    busAndProcess = "8-bit Data / 16-bit Address Bus w/ PLA Bank Switching",
                    technicalNotes = "Shares memory bus interleaved with VIC-II; VIC-II halts CPU on 'Bad Lines' to fetch character pointers."
                ),
                ChipSpec(
                    name = "MOS VIC-II (6567/6569)",
                    role = "Video Interface Chip II",
                    architecture = "Character/Bitmap Generator + 8 Hardware MOBs (Sprites)",
                    clockSpeed = "8.18 MHz Dot Clock",
                    busAndProcess = "Addresses 16 KB bank at a time + dedicated 4-bit Color RAM",
                    technicalNotes = "Fixed 16-color palette designed by Bob Yannes. Multicolor sprites trade horizontal resolution (12 double-wide pixels × 21 rows) for 3 colors + transparent."
                ),
                ChipSpec(
                    name = "MOS 6581 / 8580 SID",
                    role = "Sound Interface Device",
                    architecture = "3-Voice Hybrid Digital Osc + Analog Multimode Filter",
                    clockSpeed = "1.0 MHz",
                    busAndProcess = "NMOS (6581) / HMOS-II (8580)",
                    technicalNotes = "Features ring modulation, oscillator sync, ADSR envelopes, and resonant low/band/high-pass analog filters."
                )
            ),
            spritePipelineExplanation = "Each hardware sprite (MOB) is exactly 63 bytes (24×21 bits). In Multicolor mode, bit pairs select Transparent (00), Shared Multi-Color 0 (01), Sprite Color (10), or Shared Multi-Color 1 (11) with 2:1 wide pixels.",
            model3DPipelineExplanation = "Software 3D (like Elite) plots Bresenham lines into a 320×200 character-mapped bitmap buffer at 6–12 FPS."
        ),

        ConsoleHardwareSpec(
            id = "nes_2c02",
            name = "Nintendo Entertainment System (NES)",
            shortName = "NES (2C02 PPU)",
            releaseYear = 1983,
            manufacturer = "Nintendo",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_8BIT,
            nativeResolution = "256 × 240 (256 × 224 Visible NTSC)",
            screenWidth = 256,
            screenHeight = 240,
            cpuSummary = "Ricoh 2A03 (MOS 6502 core, no BCD) @ 1.79 MHz",
            gpuOrPpuSummary = "Ricoh 2C02 PPU (Picture Processing Unit)",
            memorySummary = "2 KB WRAM + 2 KB Nametable VRAM + 256B OAM + 8 KB CHR",
            mainRamBytes = 2048L,
            vramBytes = 8192L,
            textureCacheBytes = 256,
            bitsPerPixelSprite = 2,
            maxSpriteColorsPerPalette = 4,
            maxOnScreenColors = 25,
            masterPaletteSize = 54,
            maxSpritesTotal = 64,
            maxSpritesPerScanline = 8,
            spriteTileSize = "8×8 or 8×16 CHR Planar 2bpp Tiles",
            targetSpriteResolution = 24,
            hasHardware3D = false,
            recommendedModelTriBudget = 28,
            maxSceneTrisPerFrame = 45,
            vertexSnapGrid = 5f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.NONE_2D_WIREFRAME,
            paletteType = PaletteConstraintType.NES_2C02_2BPP_54,
            chips = listOf(
                ChipSpec(
                    name = "Ricoh 2A03 (NTSC) / 2A07 (PAL)",
                    role = "Main CPU + Integrated APU + OAM DMA",
                    architecture = "8-bit 6502 second-source (decimal mode disabled)",
                    clockSpeed = "1.789773 MHz (21.477 MHz ÷ 12)",
                    busAndProcess = "8-bit Data / 16-bit Address + 5-Channel Hardware APU",
                    technicalNotes = "Includes 2 Pulse channels, 1 Triangle, 1 Noise, 1 DMC sample channel, and a 513-cycle OAM Sprite DMA controller."
                ),
                ChipSpec(
                    name = "Ricoh 2C02 PPU",
                    role = "Picture Processing Unit",
                    architecture = "2bpp Planar Tile & 64-Entry OAM Sprite Compositor",
                    clockSpeed = "5.369318 MHz (3 dots per CPU cycle)",
                    busAndProcess = "14-bit PPU Bus + 256-Byte Internal OAM + 32-Byte Palette RAM",
                    technicalNotes = "Generates composite video directly in Phase/Chroma/Luma domain (54 usable colors). Secondary OAM holds only 8 sprites per scanline; a 9th sprite sets the overflow flag and drops out (requiring software OAM cycling/flicker)."
                ),
                ChipSpec(
                    name = "Nintendo MMC3 / MMC5 Mapper ASICs",
                    role = "Cartridge Bank Switcher & Scanline IRQ",
                    architecture = "Custom Cartridge ASIC (MMC5 adds 1KB ExRAM + 8x8 Multiplier)",
                    clockSpeed = "PPU A12 Clocked / Bus Driven",
                    busAndProcess = "Fine-grained 1KB/2KB CHR Bank Switching",
                    technicalNotes = "Allows instant sprite animation without copying pixels by swapping CHR-ROM banks on the fly."
                )
            ),
            spritePipelineExplanation = "Every 8×8 tile is stored as two planar bitplanes (16 bytes total = 2bpp), yielding 3 visible colors + 1 transparent per sprite sub-palette from a 54-color composite master palette. Max 8 sprites (64 pixels) per horizontal scanline.",
            model3DPipelineExplanation = "Lacks a linear bitmap framebuffer; 3D wireframe requires dynamically rewriting CHR-RAM bitplane tiles during narrow VBLANK windows."
        ),

        ConsoleHardwareSpec(
            id = "gameboy_dmg",
            name = "Nintendo Game Boy (DMG-01)",
            shortName = "Game Boy (DMG)",
            releaseYear = 1989,
            manufacturer = "Nintendo",
            category = HardwareCategory.HANDHELD,
            era = HardwareEra.ERA_8BIT,
            nativeResolution = "160 × 144 Reflective STN Dot-Matrix LCD",
            screenWidth = 160,
            screenHeight = 144,
            cpuSummary = "Sharp LR35902 @ 4.194304 MHz",
            gpuOrPpuSummary = "Integrated Sharp LR35902 PPU Core",
            memorySummary = "8 KB Work RAM + 8 KB VRAM + 160 Bytes OAM",
            mainRamBytes = 8192L,
            vramBytes = 8192L,
            textureCacheBytes = 160,
            bitsPerPixelSprite = 2,
            maxSpriteColorsPerPalette = 4,
            maxOnScreenColors = 4,
            masterPaletteSize = 4,
            maxSpritesTotal = 40,
            maxSpritesPerScanline = 10,
            spriteTileSize = "8×8 or 8×16 2bpp Interleaved Planar",
            targetSpriteResolution = 24,
            hasHardware3D = false,
            recommendedModelTriBudget = 24,
            maxSceneTrisPerFrame = 40,
            vertexSnapGrid = 5f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.NONE_2D_WIREFRAME,
            paletteType = PaletteConstraintType.DMG_4_OLIVE,
            chips = listOf(
                ChipSpec(
                    name = "Sharp LR35902 SoC",
                    role = "Unified CPU, PPU, 4-Ch Audio & Timer",
                    architecture = "8-bit Hybrid Intel 8080 / Zilog Z80 Core",
                    clockSpeed = "4.194304 MHz (1.048576 MHz machine cycles)",
                    busAndProcess = "CMOS Low-Power Custom SoC",
                    technicalNotes = "Drops Z80 IX/IY index registers and alternate register set, but adds bit-manipulation ops and fast 0xFF00 High-RAM (HRAM) zero-page addressing."
                ),
                ChipSpec(
                    name = "DMG PPU & Pixel FIFO",
                    role = "Background, Window & 40-Sprite OAM Engine",
                    architecture = "Dual 16-Pixel Shift Register FIFOs (BG + OBJ)",
                    clockSpeed = "4.19 MHz Dot Clock",
                    busAndProcess = "8 KB VRAM (384 8×8 Tiles + Two 32×32 Tilemaps)",
                    technicalNotes = "Outputs 2-bit shade indices mapped via OBP0/OBP1 registers to 4 reflective olive-green LCD levels. Supports 40 sprites total, up to 10 per scanline."
                )
            ),
            spritePipelineExplanation = "Uses 2bpp interleaved bitplanes (2 bytes per 8-pixel row = 16 bytes per 8×8 tile). Sprites use color 0 as transparent, leaving 3 visible olive-green shades per sprite.",
            model3DPipelineExplanation = "Games like X and Faceball 2000 software-rasterize wireframe or raycast columns directly into a grid of 8×8 VRAM tiles."
        ),

        ConsoleHardwareSpec(
            id = "virtual_boy",
            name = "Nintendo Virtual Boy",
            shortName = "Virtual Boy (VIP)",
            releaseYear = 1995,
            manufacturer = "Nintendo",
            category = HardwareCategory.HANDHELD,
            era = HardwareEra.ERA_32_64BIT,
            nativeResolution = "384 × 224 × 2 (Dual Stereoscopic Red LED Arrays)",
            screenWidth = 384,
            screenHeight = 224,
            cpuSummary = "NEC V810 (uPD70732) 32-bit RISC @ 20 MHz",
            gpuOrPpuSummary = "Nintendo VIP (Virtual Image Processor)",
            memorySummary = "128 KB PSRAM + 128 KB VRAM + 128 KB Framebuffer",
            mainRamBytes = 131072L,
            vramBytes = 262144L,
            textureCacheBytes = 0,
            bitsPerPixelSprite = 2,
            maxSpriteColorsPerPalette = 4,
            maxOnScreenColors = 4,
            masterPaletteSize = 4,
            maxSpritesTotal = 1024,
            maxSpritesPerScanline = 128,
            spriteTileSize = "8×8 2bpp Cells in 32 'Worlds' w/ Parallax",
            targetSpriteResolution = 32,
            hasHardware3D = false,
            recommendedModelTriBudget = 120,
            maxSceneTrisPerFrame = 250,
            vertexSnapGrid = 3f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.CX4_TRIG_WIREFRAME,
            paletteType = PaletteConstraintType.VIRTUAL_BOY_4_RED,
            chips = listOf(
                ChipSpec(
                    name = "NEC V810 32-Bit RISC",
                    role = "Main CPU",
                    architecture = "32-bit RISC w/ 32 General-Purpose Registers & Hardware Float",
                    clockSpeed = "20 MHz",
                    busAndProcess = "16-bit External Bus / 32-bit Internal",
                    technicalNotes = "Fast 32-bit RISC processor capable of true 3D matrix math and wireframe/polygon rasterization in software."
                ),
                ChipSpec(
                    name = "Nintendo VIP (Virtual Image Processor)",
                    role = "Stereoscopic Dual-Framebuffer Compositor",
                    architecture = "32 'World' Layers (Normal, H-Bias, Affine, OBJ) + Left/Right Framebuffers",
                    clockSpeed = "20 MHz (50.2 Hz Mirror Servo Scan)",
                    busAndProcess = "Two 384×224 2bpp Framebuffers (Left & Right Eye)",
                    technicalNotes = "Drives two 1×224 Red GaAsP LED linear arrays swept horizontally by oscillating mechanical mirrors at 50 Hz."
                )
            ),
            spritePipelineExplanation = "2bpp monochrome red palette (Black + 3 programmable pulse-width modulated red brightness levels). Up to 1,024 objects with hardware horizontal parallax shift (MPX) for stereoscopic depth.",
            model3DPipelineExplanation = "NEC V810 CPU computes 3D projections in software and renders stereo wireframe/flat polygons into the left/right 2bpp red LED framebuffers."
        ),

        // ==================== 16-BIT & CO-PROCESSORS ====================
        ConsoleHardwareSpec(
            id = "sega_genesis",
            name = "Sega Genesis / Mega Drive",
            shortName = "Genesis (YM7101)",
            releaseYear = 1988,
            manufacturer = "Sega",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_16BIT,
            nativeResolution = "320 × 224 (H40 Mode) / 256 × 224 (H32 Mode)",
            screenWidth = 320,
            screenHeight = 224,
            cpuSummary = "Motorola 68000 @ 7.67 MHz + Zilog Z80 @ 3.58 MHz",
            gpuOrPpuSummary = "Sega 315-5313 (Yamaha YM7101) VDP",
            memorySummary = "64 KB 68K RAM + 64 KB VRAM + 8 KB Z80 RAM + 128B CRAM",
            mainRamBytes = 65536L,
            vramBytes = 65536L,
            textureCacheBytes = 128,
            bitsPerPixelSprite = 4,
            maxSpriteColorsPerPalette = 15,
            maxOnScreenColors = 61,
            masterPaletteSize = 512,
            maxSpritesTotal = 80,
            maxSpritesPerScanline = 20,
            spriteTileSize = "8×8 to 32×32 (1×1 to 4×4 4bpp Linear Packed Tiles)",
            targetSpriteResolution = 32,
            hasHardware3D = false,
            recommendedModelTriBudget = 60,
            maxSceneTrisPerFrame = 120,
            vertexSnapGrid = 4f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.SVP_DSP_FLAT,
            paletteType = PaletteConstraintType.GENESIS_9BIT_4BPP,
            chips = listOf(
                ChipSpec(
                    name = "Motorola 68000",
                    role = "Main CPU",
                    architecture = "16/32-bit CISC (32-bit Registers, 16-bit ALU & Bus)",
                    clockSpeed = "7.670454 MHz (NTSC)",
                    busAndProcess = "16-bit Data / 24-bit Address Bus",
                    technicalNotes = "High clock speed and linear addressing make software 3D math and ‘Blast Processing’ DMA transfers fast."
                ),
                ChipSpec(
                    name = "Yamaha YM7101 (Sega 315-5313) VDP",
                    role = "Video Display Processor",
                    architecture = "2 Scroll Planes + Window + 80-Sprite Linked List + Bus DMA",
                    clockSpeed = "13.42 MHz Dot Clock (H40)",
                    busAndProcess = "64 KB VRAM + 128 Bytes CRAM (4×16 9-bit RGB333) + 80 Bytes VSRAM",
                    technicalNotes = "Stores 4bpp tiles in linear packed format (2 pixels per byte), making CPU bitmap-to-tile rendering and checkerboard dithering much faster than planar formats."
                ),
                ChipSpec(
                    name = "Yamaha YM2612 (OPN2) + Zilog Z80",
                    role = "6-Channel FM Synthesizer + Sound Co-CPU",
                    architecture = "4-Operator FM Synthesis w/ 8-bit DAC on Ch 6",
                    clockSpeed = "7.67 MHz (YM2612) / 3.58 MHz (Z80)",
                    busAndProcess = "Dedicated 8 KB Sound RAM",
                    technicalNotes = "Delivers signature gritty metallic bass and synth leads while offloading PCM drum playback to the Z80."
                )
            ),
            spritePipelineExplanation = "4bpp linear packed pixels (2 pixels per byte). Master palette is 9-bit RGB333 (8 levels per R/G/B = 512 colors). 4 CRAM palette lines of 15 visible colors + 1 transparent. Artists frequently used vertical column dithering to blend extra colors over composite video.",
            model3DPipelineExplanation = "Because Genesis tiles are linear packed 4bpp (nibbles), the 68000 (or SVP DSP) can plot flat-shaded and dithered polygons directly into a tilemap window much faster than planar 16-bit systems."
        ),

        ConsoleHardwareSpec(
            id = "snes",
            name = "Super Nintendo (SNES / Super Famicom)",
            shortName = "SNES (S-PPU)",
            releaseYear = 1990,
            manufacturer = "Nintendo",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_16BIT,
            nativeResolution = "256 × 224 (Progressive) / 512 × 448 (Interlaced)",
            screenWidth = 256,
            screenHeight = 224,
            cpuSummary = "Ricoh 5A22 (WDC 65C816 16-bit) @ 3.58 MHz",
            gpuOrPpuSummary = "Dual S-PPU1 (5C77) + S-PPU2 (5C78)",
            memorySummary = "128 KB WRAM + 64 KB VRAM + 64 KB Audio RAM + 512B CGRAM",
            mainRamBytes = 131072L,
            vramBytes = 65536L,
            textureCacheBytes = 544,
            bitsPerPixelSprite = 4,
            maxSpriteColorsPerPalette = 15,
            maxOnScreenColors = 256,
            masterPaletteSize = 32768,
            maxSpritesTotal = 128,
            maxSpritesPerScanline = 32,
            spriteTileSize = "8×8 to 64×64 (Two Selected Sizes per Frame, 4bpp Planar)",
            targetSpriteResolution = 32,
            hasHardware3D = false,
            recommendedModelTriBudget = 45,
            maxSceneTrisPerFrame = 80,
            vertexSnapGrid = 4f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.NONE_2D_WIREFRAME,
            paletteType = PaletteConstraintType.SNES_15BIT_4BPP,
            chips = listOf(
                ChipSpec(
                    name = "Ricoh 5A22 (WDC 65C816 Core)",
                    role = "Main CPU + 8-Channel DMA & HDMA Controller",
                    architecture = "16-bit Internal / 8-bit External Data Bus, 24-bit Address",
                    clockSpeed = "3.58 MHz (FastROM) / 2.68 MHz (SlowROM) / 1.79 MHz",
                    busAndProcess = "8-bit Data Bus w/ Hardware 8×8 Multiply & 16÷8 Divide",
                    technicalNotes = "HDMA (Horizontal Blank DMA) automatically updates PPU registers every scanline for perspective Mode 7 floors, window masks, and sky gradients."
                ),
                ChipSpec(
                    name = "Nintendo S-PPU1 & S-PPU2",
                    role = "Dual-Chip Picture Processing Unit",
                    architecture = "8 Background Modes (2bpp/4bpp/8bpp + Mode 7 Affine Matrix) & Color Math",
                    clockSpeed = "5.37 MHz Dot Clock",
                    busAndProcess = "Two 8-bit VRAM buses (64 KB total) + 512B CGRAM (256 × 15-bit BGR555) + 544B OAM",
                    technicalNotes = "Supports true hardware additive/subtractive color blending (translucent ghosts/water), mosaic pixelation, and Mode 7 16-bit matrix rotation/scaling."
                ),
                ChipSpec(
                    name = "Sony SPC700 + S-DSP",
                    role = "Autonomous 8-Voice ADPCM Audio Subsystem",
                    architecture = "8-bit Sony CPU @ 1.024 MHz + 16-bit 8-Voice BRR Sample DSP",
                    clockSpeed = "24.576 MHz Master Ceramic Resonator",
                    busAndProcess = "Dedicated 64 KB PSRAM",
                    technicalNotes = "Hardware Gaussian pitch interpolation, ADSR envelopes, and programmable 8-tap FIR stereo echo filter."
                )
            ),
            spritePipelineExplanation = "Sprites use 4bpp composite bitplanes (32 bytes per 8×8 tile) indexing 8 sprite palettes of 15 colors + transparent from a lush 15-bit BGR555 (32,768-color) master palette. Up to 128 sprites total and 32 sprites (34 8×8 tile slivers) per scanline.",
            model3DPipelineExplanation = "Base SNES uses Mode 7 affine matrix math combined with per-scanline HDMA scaling to render pseudo-3D ground planes, while polygonal 3D uses cartridge co-processors like Super FX."
        ),

        ConsoleHardwareSpec(
            id = "snes_superfx",
            name = "SNES + Super FX2 (GSU-2 Chip)",
            shortName = "Super FX (GSU-2)",
            releaseYear = 1993,
            manufacturer = "Nintendo / Argonaut Games",
            category = HardwareCategory.EXPANSION_CHIP,
            era = HardwareEra.ERA_16BIT,
            nativeResolution = "256 × 192 (Software Framebuffer inside SNES 256 × 224)",
            screenWidth = 256,
            screenHeight = 192,
            cpuSummary = "Argonaut Super FX GSU-2 16-bit RISC @ 21.48 MHz + Ricoh 5A22",
            gpuOrPpuSummary = "GSU Hardware Bitplane Pixel Plotter + SNES S-PPU",
            memorySummary = "64 KB Cartridge Framebuffer SRAM + 128 KB WRAM + 64 KB VRAM",
            mainRamBytes = 65536L,
            vramBytes = 65536L,
            textureCacheBytes = 512,
            bitsPerPixelSprite = 4,
            maxSpriteColorsPerPalette = 15,
            maxOnScreenColors = 256,
            masterPaletteSize = 32768,
            maxSpritesTotal = 128,
            maxSpritesPerScanline = 32,
            spriteTileSize = "Hardware PLOT to 4bpp/8bpp Bitplane Character Buffer",
            targetSpriteResolution = 32,
            hasHardware3D = true,
            recommendedModelTriBudget = 160,
            maxSceneTrisPerFrame = 350,
            vertexSnapGrid = 3.5f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
            paletteType = PaletteConstraintType.SNES_15BIT_4BPP,
            chips = listOf(
                ChipSpec(
                    name = "Super FX GSU-1 / GSU-2 (MARIO Chip)",
                    role = "Cartridge 3D RISC Co-Processor & Pixel Plotter",
                    architecture = "16-bit Custom RISC (Sixteen 16-bit Registers R0–R15 + 512B Instruction Cache)",
                    clockSpeed = "10.74 MHz (GSU-1) / 21.48 MHz (GSU-2)",
                    busAndProcess = "Dedicated Cartridge ROM & 32–64 KB Framebuffer SRAM Bus",
                    technicalNotes = "Features specialized `PLOT` and `CMODE` instructions that automatically convert linear X/Y pixel writes + 2x2 dither flags into SNES planar bitplane tile format in a two-row primary/secondary pixel cache!"
                ),
                ChipSpec(
                    name = "SNES 5A22 DMA Bridge",
                    role = "Cartridge-to-VRAM Framebuffer Transfer",
                    architecture = "16-bit VBLANK DMA Transfer",
                    clockSpeed = "2.68 MHz DMA Rate (~5–6 KB per VBLANK)",
                    busAndProcess = "Transfers rendered GSU bitplane tiles from Cart SRAM to S-PPU VRAM",
                    technicalNotes = "Because NTSC VBLANK can only DMA ~5.5 KB per frame, Super FX games use a letterboxed 256×192 window at 4bpp and run at 15–20 FPS (3–4 VBLANKs per rendered frame)."
                )
            ),
            spritePipelineExplanation = "The GSU can scale, rotate, and plot arbitrary 2D sprites into the cartridge tile buffer (as seen in Yoshi's Island morphing bosses) without using SNES OAM limits.",
            model3DPipelineExplanation = "GSU-2 transforms 3D vertices using fast 16-bit hardware multiply (`FMULT`/`LMULT`) and rasterizes flat-shaded or texture-mapped polygons using hardware 2×2 ordered dithering (`CMODE` dither bit) to simulate extra shading shades inside a 16-color 4bpp palette."
        ),

        ConsoleHardwareSpec(
            id = "snes_cx4_sa1",
            name = "SNES + Capcom CX4 / Nintendo SA-1",
            shortName = "Capcom CX4 DSP",
            releaseYear = 1994,
            manufacturer = "Capcom / Hitachi / Nintendo",
            category = HardwareCategory.EXPANSION_CHIP,
            era = HardwareEra.ERA_16BIT,
            nativeResolution = "256 × 224",
            screenWidth = 256,
            screenHeight = 224,
            cpuSummary = "Hitachi HG51B169 DSP @ 20 MHz + Nintendo SA-1 @ 10.74 MHz",
            gpuOrPpuSummary = "CX4 Trigonometric Vector Wireframe & Sprite Chain Engine",
            memorySummary = "2 KB Internal DSP RAM + 128 KB WRAM + 64 KB VRAM",
            mainRamBytes = 131072L,
            vramBytes = 65536L,
            textureCacheBytes = 1024,
            bitsPerPixelSprite = 4,
            maxSpriteColorsPerPalette = 15,
            maxOnScreenColors = 256,
            masterPaletteSize = 32768,
            maxSpritesTotal = 128,
            maxSpritesPerScanline = 32,
            spriteTileSize = "Hardware Multi-Sprite Trigonometric Assembly",
            targetSpriteResolution = 32,
            hasHardware3D = true,
            recommendedModelTriBudget = 110,
            maxSceneTrisPerFrame = 220,
            vertexSnapGrid = 3f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.CX4_TRIG_WIREFRAME,
            paletteType = PaletteConstraintType.SNES_15BIT_4BPP,
            chips = listOf(
                ChipSpec(
                    name = "Capcom CX4 (Hitachi HG51B169)",
                    role = "Trigonometric Vector & OAM Sprite Chain DSP",
                    architecture = "24-bit Fixed-Point DSP w/ Internal Sin/Cos Lookup ROM",
                    clockSpeed = "20 MHz",
                    busAndProcess = "Cartridge Co-Processor (Mega Man X2 / X3)",
                    technicalNotes = "Specializes in real-time 3D wireframe mesh rotation, line rasterization into SNES planar tiles, and rotating multi-part OAM sprite skeletons."
                ),
                ChipSpec(
                    name = "Nintendo SA-1 (Super Accelerator 1)",
                    role = "10.74 MHz 65C816 Co-CPU & Bitmap-to-Bitplane DMA",
                    architecture = "High-Speed 65C816 Core + Character Conversion DMA",
                    clockSpeed = "10.74 MHz (3× SNES CPU)",
                    busAndProcess = "2 KB Fast I-RAM + Hardware Barrel Shifter & Arithmetic Unit",
                    technicalNotes = "Includes dedicated hardware Character Conversion DMA that converts packed 2bpp/4bpp/8bpp linear bitmaps into SNES planar tiles on the fly during DMA."
                )
            ),
            spritePipelineExplanation = "CX4 automatically computes rotated X/Y screen coordinates for dozens of linked OAM sub-sprites, while SA-1 converts linear packed pixel buffers into SNES bitplanes.",
            model3DPipelineExplanation = "CX4 executes high-speed 3D matrix rotations using internal sine/cosine tables to draw glowing neon 3D wireframe boss structures at 60 FPS."
        ),

        ConsoleHardwareSpec(
            id = "sega_svp",
            name = "Sega Genesis + Virtua Processor (SVP)",
            shortName = "Sega SVP DSP",
            releaseYear = 1994,
            manufacturer = "Sega / Samsung",
            category = HardwareCategory.EXPANSION_CHIP,
            era = HardwareEra.ERA_16BIT,
            nativeResolution = "320 × 224 (256 × 192 3D Viewport)",
            screenWidth = 320,
            screenHeight = 224,
            cpuSummary = "Samsung SSP1601 16-bit DSP @ 23 MHz + Motorola 68000",
            gpuOrPpuSummary = "SVP Software-to-Tile Polygon Rasterizer + YM7101 VDP",
            memorySummary = "128 KB Cartridge DRAM + 2 KB Internal DSP RAM + 64 KB VRAM",
            mainRamBytes = 131072L,
            vramBytes = 65536L,
            textureCacheBytes = 2048,
            bitsPerPixelSprite = 4,
            maxSpriteColorsPerPalette = 15,
            maxOnScreenColors = 61,
            masterPaletteSize = 512,
            maxSpritesTotal = 80,
            maxSpritesPerScanline = 20,
            spriteTileSize = "8×8 Linear 4bpp Tile Framebuffer",
            targetSpriteResolution = 32,
            hasHardware3D = true,
            recommendedModelTriBudget = 220,
            maxSceneTrisPerFrame = 500,
            vertexSnapGrid = 3f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.SVP_DSP_FLAT,
            paletteType = PaletteConstraintType.GENESIS_9BIT_4BPP,
            chips = listOf(
                ChipSpec(
                    name = "Sega Virtua Processor (Samsung SSP1601)",
                    role = "Cartridge 3D Geometry & Polygon Rasterizer DSP",
                    architecture = "16-bit Fixed-Point DSP (Single-Cycle 16×16->32 MAC)",
                    clockSpeed = "23.01 MHz (3× Genesis 68000)",
                    busAndProcess = "128 KB On-Cartridge DRAM + Address Translator ASIC (315-5687)",
                    technicalNotes = "Capable of 25 MIPS and ~300–500 flat/dithered polygons per frame at 15–20 FPS (6,500 polys/sec), used in Virtua Racing."
                )
            ),
            spritePipelineExplanation = "Renders into Genesis 4bpp linear tiles using a dedicated 15-color 9-bit RGB333 sub-palette with 2×2 checkerboard dithering to create intermediate color gradients.",
            model3DPipelineExplanation = "SSP1601 performs 3D matrix transforms, backface culling, depth sorting, and scanline span filling into 128 KB DRAM, which Genesis VDP DMAs into VRAM."
        ),

        ConsoleHardwareSpec(
            id = "neo_geo",
            name = "SNK Neo Geo AES / MVS",
            shortName = "Neo Geo (LSPC2)",
            releaseYear = 1990,
            manufacturer = "SNK",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_16BIT,
            nativeResolution = "320 × 224",
            screenWidth = 320,
            screenHeight = 224,
            cpuSummary = "Motorola 68000 @ 12 MHz + Zilog Z80 @ 4 MHz",
            gpuOrPpuSummary = "SNK LSPC2-A2 Line Sprite Processor + NEO-B1",
            memorySummary = "64 KB 68K RAM + 68 KB VRAM + 16 KB Palette RAM + 716 Mb ROM",
            mainRamBytes = 65536L,
            vramBytes = 69632L,
            textureCacheBytes = 0,
            bitsPerPixelSprite = 4,
            maxSpriteColorsPerPalette = 15,
            maxOnScreenColors = 4096,
            masterPaletteSize = 65536,
            maxSpritesTotal = 380,
            maxSpritesPerScanline = 96,
            spriteTileSize = "16×16 Tiles Chained Vertically up to 16×512 Hardware Strips",
            targetSpriteResolution = 48,
            hasHardware3D = false,
            recommendedModelTriBudget = 80,
            maxSceneTrisPerFrame = 150,
            vertexSnapGrid = 3f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.NONE_2D_WIREFRAME,
            paletteType = PaletteConstraintType.RGB555_15BIT,
            chips = listOf(
                ChipSpec(
                    name = "SNK LSPC2-A2 & NEO-B1",
                    role = "Line-Buffer Pure Sprite Graphics Engine",
                    architecture = "380 Hardware Sprite Strips + 256-Palette Line Compositor",
                    clockSpeed = "24 MHz Master Clock",
                    busAndProcess = "Direct C-ROM Streaming Bus (Up to 716 Megabits Cartridge ROM)",
                    technicalNotes = "Has NO scrolling tilemap backgrounds! Everything on screen (backgrounds, parallax layers, giant fighters) is built from 16-pixel-wide vertical sprite strips chained together with hardware shrink LUTs."
                ),
                ChipSpec(
                    name = "Yamaha YM2610 (OPNB)",
                    role = "15-Channel Arcade Audio Processor",
                    architecture = "4 FM + 3 SSG + 7 ADPCM-A/B Sample Channels",
                    clockSpeed = "8 MHz",
                    busAndProcess = "Dedicated V-ROM Sample Bus",
                    technicalNotes = "Streams multi-megabyte arcade voice and percussion samples directly from cartridge ROM without CPU intervention."
                )
            ),
            spritePipelineExplanation = "Every 16×16 sprite tile selects 1 of 256 simultaneous 16-color palettes (4,096 colors on-screen out of 65,536 16-bit RGB+DarkBit colors), streamed directly from massive C-ROM chips.",
            model3DPipelineExplanation = "Pure 2D sprite powerhouse; 3D models must be pre-rendered into multi-angle 16-color sprite sheets with hardware vertical/horizontal shrink scaling."
        ),

        // ==================== 32/64-BIT 3D DAWN ====================
        ConsoleHardwareSpec(
            id = "ps1",
            name = "Sony PlayStation (PS1)",
            shortName = "PS1 (GTE + GPU)",
            releaseYear = 1994,
            manufacturer = "Sony Computer Entertainment",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_32_64BIT,
            nativeResolution = "320 × 240 (Progressive) to 640 × 480 (Interlaced)",
            screenWidth = 320,
            screenHeight = 240,
            cpuSummary = "LSI LR33300 (MIPS R3000A) @ 33.8688 MHz + GTE Co-Processor",
            gpuOrPpuSummary = "Sony CXD8514Q / CXD8561 2D/Polygon Rasterizer GPU",
            memorySummary = "2 MB Main EDO RAM + 1 MB Dual-Port VRAM + 1 KB Scratchpad",
            mainRamBytes = 2097152L,
            vramBytes = 1048576L,
            textureCacheBytes = 2048,
            bitsPerPixelSprite = 8,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 32768,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 4000,
            maxSpritesPerScanline = 256,
            spriteTileSize = "Free-Form Textured Quads / Sprites (4bpp/8bpp CLUT or 15-bit)",
            targetSpriteResolution = 48,
            hasHardware3D = true,
            recommendedModelTriBudget = 450,
            maxSceneTrisPerFrame = 1500,
            vertexSnapGrid = 5.5f, // Authentic PS1 GTE integer vertex snap!
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.PS1_GTE_AFFINE_WOBBLE,
            paletteType = PaletteConstraintType.RGB555_15BIT,
            chips = listOf(
                ChipSpec(
                    name = "MIPS R3000A + GTE (Geometry Transformation Engine)",
                    role = "Main 32-Bit RISC CPU + COP2 Vector Matrix Co-Processor",
                    architecture = "32-bit MIPS I + 16-bit Fixed-Point SIMD Vector Engine (66 MIPS)",
                    clockSpeed = "33.8688 MHz",
                    busAndProcess = "32-bit Main Bus + 4 KB I-Cache + 1 KB Fast Scratchpad Data RAM",
                    technicalNotes = "GTE lives on Coprocessor 2 (`RTPS`/`RTPT` instructions), transforming a 3D triangle in 22 cycles. However, GTE outputs strictly 16-bit INTEGER screen X/Y coordinates (no sub-pixel fraction), causing the iconic PS1 vertex jitter/wobble!"
                ),
                ChipSpec(
                    name = "Sony CXD8514Q / CXD8561 GPU",
                    role = "Flat/Gouraud Affine Polygon & Sprite Rasterizer",
                    architecture = "1024×512 16-bit Framebuffer + 2 KB Texture Cache + 4×4 Dither Unit",
                    clockSpeed = "53.2224 MHz",
                    busAndProcess = "1 MB VRAM (Holds both Framebuffer, CLUT Palettes & Texture Pages)",
                    technicalNotes = "NO hardware Z-Buffer (uses CPU Ordering Table linked-list bucket sort) and NO perspective-correct texture division (uses affine UV interpolation, causing textures to swim/warp unless triangles are subdivided)."
                ),
                ChipSpec(
                    name = "MDEC + Sony SPU (CXD2922BQ)",
                    role = "Motion JPEG Decoder & 24-Channel ADPCM Audio DSP",
                    architecture = "Hardware IDCT/YUV-to-RGB + 24-Voice 44.1 kHz Reverb DSP",
                    clockSpeed = "33.8688 MHz",
                    busAndProcess = "512 KB Dedicated Sound RAM",
                    technicalNotes = "MDEC decodes full-motion video streams in hardware while SPU mixes 24 ADPCM channels with hardware pitch modulation."
                )
            ),
            spritePipelineExplanation = "Sprites are rendered as textured rectangles directly into the 15-bit RGB555 framebuffer using 4bpp (16-color) or 8bpp (256-color) CLUT palettes stored right inside the 1024×512 VRAM sheet.",
            model3DPipelineExplanation = "GTE transforms 16-bit fixed-point vertices into integer screen coordinates (producing vertex wobble), sorts faces into an Ordering Table (no Z-buffer), and rasterizes with affine UV textures and a hardware 4×4 ordered dither matrix."
        ),

        ConsoleHardwareSpec(
            id = "sega_saturn",
            name = "Sega Saturn",
            shortName = "Saturn (VDP1+VDP2)",
            releaseYear = 1994,
            manufacturer = "Sega",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_32_64BIT,
            nativeResolution = "320 × 224 to 704 × 480",
            screenWidth = 352,
            screenHeight = 240,
            cpuSummary = "Dual Hitachi SH-2 32-bit RISC @ 28.63 MHz + SCU DSP",
            gpuOrPpuSummary = "Sega VDP1 (Quad/Sprite Warper) + VDP2 (Background/Mode 7 Engine)",
            memorySummary = "2 MB Main RAM + 1.5 MB VRAM (1 MB VDP1 + 512 KB VDP2) + 512 KB Audio",
            mainRamBytes = 2097152L,
            vramBytes = 1572864L,
            textureCacheBytes = 524288,
            bitsPerPixelSprite = 8,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 32768,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 4096,
            maxSpritesPerScanline = 256,
            spriteTileSize = "Hardware Distorted Quads (4-Corner Forward Texture Warping)",
            targetSpriteResolution = 48,
            hasHardware3D = true,
            recommendedModelTriBudget = 400,
            maxSceneTrisPerFrame = 1200,
            vertexSnapGrid = 3.5f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.SATURN_VDP1_QUAD_MESH,
            paletteType = PaletteConstraintType.RGB555_15BIT,
            chips = listOf(
                ChipSpec(
                    name = "Dual Hitachi SH-2 (7604) + SCU Geometry DSP",
                    role = "Master/Slave 32-Bit RISC CPUs + Matrix Co-Processor",
                    architecture = "Dual 32-bit SuperH RISC (25 MIPS each) + 14.3 MHz Fixed-Point SCU DSP",
                    clockSpeed = "28.63 MHz",
                    busAndProcess = "Shared 32-bit Bus w/ 4 KB On-Chip Cache/Scratchpad per SH-2",
                    technicalNotes = "Requires carefully partitioning 3D vertex transforms and game logic across two SH-2 cores and the SCU DSP."
                ),
                ChipSpec(
                    name = "Sega VDP1 (315-5689)",
                    role = "3D Distorted Sprite & Quadrilateral Rasterizer",
                    architecture = "Forward-Texture-Reading Quad Engine + Dual 256 KB Framebuffers",
                    clockSpeed = "28.63 MHz",
                    busAndProcess = "512 KB Texture VRAM + 512 KB Dual Framebuffers",
                    technicalNotes = "Every 3D primitive is actually a 4-vertex distorted sprite (QUAD)! Triangles are rendered as degenerate quads with two identical vertices. Uses 'Mesh' (50% checkerboard screen-door stipple) to avoid framebuffer read-modify-write translucency stalls."
                ),
                ChipSpec(
                    name = "Sega VDP2 (315-5690)",
                    role = "5-Layer Parallax & Dual 3D RBG0/RBG1 Plane Compositor",
                    architecture = "Up to 5 Simultaneous Scroll Planes + 2 Affine Rotating/Scaling 3D Planes",
                    clockSpeed = "28.63 MHz",
                    busAndProcess = "512 KB VDP2 VRAM + 4 KB Color RAM",
                    technicalNotes = "Capable of rendering infinite horizon terrains (Panzer Dragoon / Virtua Fighter 2 arenas) with zero VDP1 polygon overhead."
                )
            ),
            spritePipelineExplanation = "Unmatched 2D powerhouse: VDP1 draws scaled/rotated sprites into dual framebuffers while VDP2 composites 5 independent background layers with per-line scaling.",
            model3DPipelineExplanation = "VDP1 renders 3D polygons as 4-point distorted quads using forward texture stepping, Gouraud shading, and signature checkerboard screen-door mesh transparency."
        ),

        ConsoleHardwareSpec(
            id = "n64",
            name = "Nintendo 64 (N64)",
            shortName = "N64 (RCP: RSP+RDP)",
            releaseYear = 1996,
            manufacturer = "Nintendo / SGI",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_32_64BIT,
            nativeResolution = "320 × 240 (Standard) / 640 × 480 (Hi-Res Expansion)",
            screenWidth = 320,
            screenHeight = 240,
            cpuSummary = "NEC VR4300 (64-bit MIPS III) @ 93.75 MHz",
            gpuOrPpuSummary = "SGI Reality Coprocessor (RCP: RSP + RDP) @ 62.5 MHz",
            memorySummary = "4 MB (8 MB Expanded) Unified Rambus RDRAM + 4 KB TMEM Cache",
            mainRamBytes = 4194304L,
            vramBytes = 4194304L,
            textureCacheBytes = 4096, // Strict 4 KB TMEM!
            bitsPerPixelSprite = 16,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 32768,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 2048,
            maxSpritesPerScanline = 256,
            spriteTileSize = "RDP Textured Rectangles (Must fit in 4 KB On-Chip TMEM)",
            targetSpriteResolution = 32,
            hasHardware3D = true,
            recommendedModelTriBudget = 600,
            maxSceneTrisPerFrame = 2000,
            vertexSnapGrid = 0f, // Sub-pixel accurate!
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.N64_RCP_TMEM_3POINT,
            paletteType = PaletteConstraintType.RGB555_15BIT,
            chips = listOf(
                ChipSpec(
                    name = "NEC VR4300 (64-Bit MIPS III)",
                    role = "Main CPU",
                    architecture = "64-bit MIPS R4200 Derivative w/ Hardware 64-bit FPU",
                    clockSpeed = "93.75 MHz",
                    busAndProcess = "16 KB I-Cache + 8 KB D-Cache, 562.5 MB/s Rambus Interface",
                    technicalNotes = "Full 64-bit floating-point and integer arithmetic, usually running in 32-bit addressing mode to save code footprint."
                ),
                ChipSpec(
                    name = "SGI RCP — RSP (Reality Signal Processor)",
                    role = "Programmable 128-Bit Vector Geometry & Audio Microcode DSP",
                    architecture = "MIPS Scalar Core + 8-Element 16-bit SIMD Vector Unit (VU)",
                    clockSpeed = "62.5 MHz",
                    busAndProcess = "4 KB IMEM (Microcode) + 4 KB DMEM (Data Scratchpad)",
                    technicalNotes = "Runs downloadable microcode (Fast3D, F3DEX2) to execute vertex transformations, lighting, clipping, and audio mixing."
                ),
                ChipSpec(
                    name = "SGI RCP — RDP (Reality Display Processor)",
                    role = "Z-Buffered Perspective-Correct Rasterizer w/ 4 KB TMEM",
                    architecture = "Sub-Pixel Rasterizer + Texture Filter + Color Combiner + Blender",
                    clockSpeed = "62.5 MHz",
                    busAndProcess = "4,096 Bytes (4 KB) On-Chip Texture Memory (TMEM)",
                    technicalNotes = "Features true perspective-correct texture mapping, sub-pixel vertices (zero wobble), hardware Z-buffering, edge anti-aliasing, and 3-point bilinear texture filtering—all constrained by the tiny 4 KB on-chip TMEM texture cache!"
                )
            ),
            spritePipelineExplanation = "2D sprites are drawn via S2DEX microcode or RDP textured rectangles, sliced into small strips (e.g. 64×32 4bpp or 32×32 16-bit) so each strip fits inside the 4,096-byte TMEM cache.",
            model3DPipelineExplanation = "Eliminates PS1 vertex wobble and texture warping via sub-pixel precision, perspective-correct division, and hardware Z-buffering, but requires tiny tiled or vertex-colored textures to fit inside 4 KB TMEM, smoothed by 3-point bilinear filtering and distance fog."
        ),

        ConsoleHardwareSpec(
            id = "gba",
            name = "Game Boy Advance (GBA)",
            shortName = "GBA (ARM7TDMI)",
            releaseYear = 2001,
            manufacturer = "Nintendo",
            category = HardwareCategory.HANDHELD,
            era = HardwareEra.ERA_32_64BIT,
            nativeResolution = "240 × 160 Reflective / Backlit TFT LCD",
            screenWidth = 240,
            screenHeight = 160,
            cpuSummary = "ARM7TDMI 32-bit RISC @ 16.78 MHz + Sharp LR35902 @ 8.38 MHz",
            gpuOrPpuSummary = "GBA Custom 2D Affine/Bitmap PPU",
            memorySummary = "32 KB Fast 32-bit IWRAM + 256 KB EWRAM + 96 KB VRAM + 1 KB OAM",
            mainRamBytes = 294912L,
            vramBytes = 98304L,
            textureCacheBytes = 1024,
            bitsPerPixelSprite = 4,
            maxSpriteColorsPerPalette = 15,
            maxOnScreenColors = 512,
            masterPaletteSize = 32768,
            maxSpritesTotal = 128,
            maxSpritesPerScanline = 128,
            spriteTileSize = "8×8 to 64×64 (4bpp/8bpp Linear + 32 Affine Matrices)",
            targetSpriteResolution = 32,
            hasHardware3D = false,
            recommendedModelTriBudget = 90,
            maxSceneTrisPerFrame = 180,
            vertexSnapGrid = 3.5f,
            hasZBuffer = false,
            renderProfile3D = Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
            paletteType = PaletteConstraintType.SNES_15BIT_4BPP,
            chips = listOf(
                ChipSpec(
                    name = "ARM7TDMI 32-Bit RISC",
                    role = "Main CPU (GBA Mode)",
                    architecture = "32-bit ARMv4T (32-bit ARM + 16-bit Thumb Instruction Sets)",
                    clockSpeed = "16.777216 MHz",
                    busAndProcess = "32-bit Zero-Wait-State Bus to 32 KB IWRAM; 16-bit Bus to ROM/EWRAM",
                    technicalNotes = "Placing inner scanline polygon loops in 32-bit ARM assembly inside 32 KB IWRAM enables software 3D engines on a handheld!"
                ),
                ChipSpec(
                    name = "GBA 2D / Bitmap PPU",
                    role = "Tile Modes 0–2 & Bitmap Framebuffer Modes 3–5",
                    architecture = "4 BG Layers (2 Affine Mode-7 style) + 128 Affine Sprites",
                    clockSpeed = "16.78 MHz",
                    busAndProcess = "96 KB VRAM + 1 KB Palette RAM + 1 KB OAM",
                    technicalNotes = "Bitmap Mode 4 provides double-buffered 240×160 8bpp framebuffers for software-rendered 3D games."
                )
            ),
            spritePipelineExplanation = "Supports 128 sprites up to 64×64 pixels in 4bpp (16 palettes × 15 colors) or 8bpp (255 colors) with 32 hardware affine rotation/scaling matrices.",
            model3DPipelineExplanation = "No hardware 3D GPU; 3D games run fast fixed-point ARM assembly routines out of 32 KB IWRAM into Bitmap Mode 4 (240×160 8bpp)."
        ),

        ConsoleHardwareSpec(
            id = "nintendo_ds",
            name = "Nintendo DS (NDS)",
            shortName = "Nintendo DS (ARM9 3D)",
            releaseYear = 2004,
            manufacturer = "Nintendo",
            category = HardwareCategory.HANDHELD,
            era = HardwareEra.ERA_32_64BIT,
            nativeResolution = "256 × 192 × 2 (Dual Screen TFT LCD)",
            screenWidth = 256,
            screenHeight = 192,
            cpuSummary = "ARM946E-S @ 67.028 MHz + ARM7TDMI @ 33.514 MHz",
            gpuOrPpuSummary = "NDS Hardware 3D Geometry & Rendering Engine + Dual 2D Engines",
            memorySummary = "4 MB PSRAM + 656 KB Configurable Banked VRAM",
            mainRamBytes = 4194304L,
            vramBytes = 671744L,
            textureCacheBytes = 524288,
            bitsPerPixelSprite = 8,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 32768,
            masterPaletteSize = 262144,
            maxSpritesTotal = 128,
            maxSpritesPerScanline = 128,
            spriteTileSize = "8×8 to 64×64 OAM or 3D Textured Billboards",
            targetSpriteResolution = 32,
            hasHardware3D = true,
            recommendedModelTriBudget = 500,
            maxSceneTrisPerFrame = 2048, // Exact NDS hardware Polygon RAM limit!
            vertexSnapGrid = 3.0f,
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.NDS_FIXED_TOON_2048,
            paletteType = PaletteConstraintType.RGB555_15BIT,
            chips = listOf(
                ChipSpec(
                    name = "ARM946E-S + ARM7TDMI Dual-CPU SoC",
                    role = "Main Application CPU + Audio/Touch/Wireless Co-CPU",
                    architecture = "ARMv5TE (8 KB I-Cache, 4 KB D-Cache, 32 KB Tightly Coupled TCM)",
                    clockSpeed = "67.028 MHz (ARM9) / 33.514 MHz (ARM7)",
                    busAndProcess = "Hardware Fixed-Point Square Root & Division Accelerators",
                    technicalNotes = "ARM9 feeds 3D geometry commands via FIFO or DMA while ARM7 manages 16-channel ADPCM/PSG audio and touchscreen."
                ),
                ChipSpec(
                    name = "NDS Hardware 3D Geometry & Scanline Rendering Engine",
                    role = "Fixed-Point Hardware T&L, Toon Shading & Scanline Sorter",
                    architecture = "4×4 Fixed-Point Matrix Stack + 6,144-Vertex / 2,048-Polygon RAM",
                    clockSpeed = "67 MHz (120,000 Triangles/sec)",
                    busAndProcess = "Two Scanline Line-Buffers (No Full 3D Framebuffer in VRAM)",
                    technicalNotes = "Has a hard silicon limit of 2,048 polygons and 6,144 vertices per frame in Polygon RAM! Features built-in 32-entry Toon Shading lookup tables, contour edge marking, and 4 directional hardware lights."
                )
            ),
            spritePipelineExplanation = "Dual 2D Engines (Engine A & B) support 128 OAM sprites per screen plus extended 16-slot palettes, or 3D Engine output can be captured as a 256×192 BG layer.",
            model3DPipelineExplanation = "Fixed-point 1.19.12 hardware T&L with perspective-correct textures, built-in cel/toon table shading, and a strict 2,048-polygon per frame hardware buffer cap."
        ),

        // ==================== 128-BIT 6TH GEN ====================
        ConsoleHardwareSpec(
            id = "dreamcast",
            name = "Sega Dreamcast",
            shortName = "Dreamcast (PowerVR2)",
            releaseYear = 1998,
            manufacturer = "Sega",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_128BIT,
            nativeResolution = "640 × 480 (Progressive VGA)",
            screenWidth = 640,
            screenHeight = 480,
            cpuSummary = "Hitachi SH-4 32-bit RISC w/ 128-bit Vector FPU @ 200 MHz",
            gpuOrPpuSummary = "NEC / VideoLogic PowerVR2 (CLX2 / Holly) TBDR @ 100 MHz",
            memorySummary = "16 MB Main SDRAM + 8 MB Texture VRAM + 2 MB Audio RAM",
            mainRamBytes = 16777216L,
            vramBytes = 8388608L,
            textureCacheBytes = 8388608,
            bitsPerPixelSprite = 16,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 16777216,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 16384,
            maxSpritesPerScanline = 1024,
            spriteTileSize = "VQ-Compressed or ARGB4444/1555 Textured Quads",
            targetSpriteResolution = 64,
            hasHardware3D = true,
            recommendedModelTriBudget = 2500,
            maxSceneTrisPerFrame = 25000,
            vertexSnapGrid = 0f,
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.POWERVR_TBDR_DREAMCAST,
            paletteType = PaletteConstraintType.TRUECOLOR_24BIT,
            chips = listOf(
                ChipSpec(
                    name = "Hitachi SH-4 (SH7091)",
                    role = "Main CPU + 128-Bit 1.4 GFLOPS Vector FPU",
                    architecture = "2-Way Superscalar 32-bit RISC (360 MIPS) + `FTRV` Matrix Instruction",
                    clockSpeed = "200 MHz",
                    busAndProcess = "64-bit 100 MHz Bus (800 MB/s) + Single-Cycle 4D Dot Product (`FIPR`)",
                    technicalNotes = "Executes a full 4×4 matrix × 4D vector transform in 4 cycles via `FTRV`."
                ),
                ChipSpec(
                    name = "NEC / VideoLogic PowerVR2 DC (CLX2)",
                    role = "Tile-Based Deferred Rendering (TBDR) GPU",
                    architecture = "32×32 Pixel On-Chip Tile Buffer w/ Hidden Surface Removal (HSR)",
                    clockSpeed = "100 MHz",
                    busAndProcess = "8 MB 64-bit SDRAM VRAM w/ Hardware VQ Texture Compression",
                    technicalNotes = "Bins the screen into 32×32 tiles, resolves Z-depth and order-independent translucency completely on-chip in 32-bit float before shading a single pixel (zero overdraw!)."
                ),
                ChipSpec(
                    name = "Yamaha AICA (ARM7DI + 64-Ch Sound DSP)",
                    role = "Autonomous 64-Voice Audio Subsystem",
                    architecture = "ARM7 @ 45 MHz + 64-Channel ADPCM/PCM DSP",
                    clockSpeed = "45 MHz",
                    busAndProcess = "2 MB Dedicated Audio RAM",
                    technicalNotes = "Supports hardware low-pass resonant filters and reverb per channel."
                )
            ),
            spritePipelineExplanation = "Renders crisp 640×480 2D sprites as hardware polygons with order-independent per-pixel alpha translucency and Vector Quantization (VQ) 2bpp/4bpp compression.",
            model3DPipelineExplanation = "SH-4 `FTRV` transforms vertices and feeds display lists to PowerVR2, which performs on-chip 32×32 Tile-Based Deferred Rendering with hardware bump mapping and trilinear filtering."
        ),

        ConsoleHardwareSpec(
            id = "ps2",
            name = "Sony PlayStation 2 (PS2)",
            shortName = "PS2 (EE + GS)",
            releaseYear = 2000,
            manufacturer = "Sony Computer Entertainment",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_128BIT,
            nativeResolution = "640 × 448 (NTSC) / 640 × 512 (PAL)",
            screenWidth = 640,
            screenHeight = 448,
            cpuSummary = "Emotion Engine (MIPS R5900 + Dual 128-bit VU0/VU1) @ 294.9 MHz",
            gpuOrPpuSummary = "Graphics Synthesizer (GS) @ 147.456 MHz w/ 4 MB Embedded eDRAM",
            memorySummary = "32 MB Dual-Channel RDRAM + 4 MB GS eDRAM + 2 MB SPU2 RAM",
            mainRamBytes = 33554432L,
            vramBytes = 4194304L, // Strict 4 MB eDRAM!
            textureCacheBytes = 4194304,
            bitsPerPixelSprite = 8,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 16777216,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 32768,
            maxSpritesPerScanline = 2048,
            spriteTileSize = "4bpp/8bpp CLUT Textures Streamed via 1.2 GB/s GIF DMA",
            targetSpriteResolution = 64,
            hasHardware3D = true,
            recommendedModelTriBudget = 3500,
            maxSceneTrisPerFrame = 45000,
            vertexSnapGrid = 0f,
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.PS2_EE_GS_EDRAM,
            paletteType = PaletteConstraintType.TRUECOLOR_24BIT,
            chips = listOf(
                ChipSpec(
                    name = "Emotion Engine (EE: R5900 + VU0 + VU1 + 10-Ch DMAC)",
                    role = "128-Bit CPU & Dual SIMD Vector Geometry Synthesizers",
                    architecture = "64-bit MIPS Core + Two 4-Wide 32-bit Float Vector Units (6.2 GFLOPS)",
                    clockSpeed = "294.912 MHz",
                    busAndProcess = "128-bit Internal SIMD Bus + 16 KB Fast Scratchpad + 3.2 GB/s RDRAM",
                    technicalNotes = "VU0 operates as a CPU co-processor for physics/animation, while VU1 runs autonomous geometry/lighting microcode and feeds triangle strips directly to the GS via PATH1."
                ),
                ChipSpec(
                    name = "Sony Graphics Synthesizer (GS)",
                    role = "16-Pixel-Pipe Ultra-Fillrate Rasterizer w/ 4 MB eDRAM",
                    architecture = "2,560-bit Wide Internal eDRAM Bus (48 GB/s Bandwidth!)",
                    clockSpeed = "147.456 MHz",
                    busAndProcess = "4 MB On-Die Embedded DRAM (38.4 GB/s Pixel + 9.6 GB/s Texture)",
                    technicalNotes = "Delivers 2.4 Gigapixels/sec fillrate for multi-pass particle/post-processing effects, but has only 4 MB of eDRAM—requiring constant high-speed DMA texture streaming of 4bpp/8bpp CLUT textures from 32 MB Main RAM."
                )
            ),
            spritePipelineExplanation = "Uses 4bpp and 8bpp palletized CLUT textures swizzled inside the 4 MB GS eDRAM for ultra-fast multi-layer alpha blending.",
            model3DPipelineExplanation = "VU1 executes custom vertex lighting/skinning microcode and streams triangle strips to the GS, which rasterizes with 48 GB/s eDRAM bandwidth."
        ),

        ConsoleHardwareSpec(
            id = "gamecube",
            name = "Nintendo GameCube",
            shortName = "GameCube (Flipper TEV)",
            releaseYear = 2001,
            manufacturer = "Nintendo / ArtX (ATI)",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_128BIT,
            nativeResolution = "640 × 480 (480i / 480p Progressive)",
            screenWidth = 640,
            screenHeight = 480,
            cpuSummary = "IBM PowerPC 750CXe 'Gekko' @ 486 MHz (Paired-Singles SIMD)",
            gpuOrPpuSummary = "ArtX / ATI 'Flipper' @ 162 MHz w/ 16-Stage TEV Pipeline",
            memorySummary = "24 MB 1T-SRAM Main + 3 MB Embedded 1T-SRAM on Flipper + 16 MB ARAM",
            mainRamBytes = 25165824L,
            vramBytes = 3145728L,
            textureCacheBytes = 1048576,
            bitsPerPixelSprite = 16,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 16777216,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 16384,
            maxSpritesPerScanline = 2048,
            spriteTileSize = "S3TC/CMPR Compressed or RGBA8 Hardware Quads",
            targetSpriteResolution = 64,
            hasHardware3D = true,
            recommendedModelTriBudget = 4500,
            maxSceneTrisPerFrame = 60000,
            vertexSnapGrid = 0f,
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.GAMECUBE_TEV_STAGES,
            paletteType = PaletteConstraintType.TRUECOLOR_24BIT,
            chips = listOf(
                ChipSpec(
                    name = "IBM PowerPC 'Gekko' (750CXe Derivative)",
                    role = "Main CPU w/ 50 New Paired-Single 3D SIMD Instructions",
                    architecture = "32-bit PowerPC Superscalar + Dual 32-bit Float SIMD + Write Gather Pipe",
                    clockSpeed = "486 MHz",
                    busAndProcess = "64-bit 162 MHz Bus + 16 KB Data Cache (Lockable as 16 KB Scratchpad)",
                    technicalNotes = "Hardware quantization engines compress/decompress 3D vertex positions and normals between 8/16-bit integers and 32-bit floats with zero cycle penalty."
                ),
                ChipSpec(
                    name = "ArtX / ATI 'Flipper' GPU",
                    role = "Hardware T&L + 16-Stage TEV (Texture Environment) Combiner",
                    architecture = "4 Pixel Pipes + 8 Hardware Lights + 3 MB On-Die 1T-SRAM",
                    clockSpeed = "162 MHz",
                    busAndProcess = "2 MB Embedded Framebuffer/Z 1T-SRAM + 1 MB Texture Cache (10.4 GB/s)",
                    technicalNotes = "The 16-stage TEV unit sequentially combines up to 8 textures, vertex colors, and Emboss/Indirect bump maps per pass (enabling Mario Sunshine water and Wind Waker cel-shading)."
                )
            ),
            spritePipelineExplanation = "Hardware S3TC (CMPR 4bpp) block compression packs high-resolution textures and sprites into the 1 MB on-die 1T-SRAM texture cache.",
            model3DPipelineExplanation = "Flipper's hardware T&L and 16-stage TEV pipeline evaluate multi-texture specular, rim lighting, and toon/cel shading in a single raster pass."
        ),

        ConsoleHardwareSpec(
            id = "psp",
            name = "Sony PlayStation Portable (PSP)",
            shortName = "PSP (MIPS + GU)",
            releaseYear = 2004,
            manufacturer = "Sony Computer Entertainment",
            category = HardwareCategory.HANDHELD,
            era = HardwareEra.ERA_128BIT,
            nativeResolution = "480 × 272 Widescreen 16:9 TFT LCD",
            screenWidth = 480,
            screenHeight = 272,
            cpuSummary = "Dual MIPS R4000 32-bit Cores + VFPU Vector Unit @ 333 MHz",
            gpuOrPpuSummary = "PSP Graphics Core (GU) @ 166 MHz w/ 2 MB Embedded eDRAM",
            memorySummary = "32 MB (64 MB PSP-2000+) Main SDRAM + 2 MB Embedded eDRAM",
            mainRamBytes = 33554432L,
            vramBytes = 2097152L,
            textureCacheBytes = 2097152,
            bitsPerPixelSprite = 16,
            maxSpriteColorsPerPalette = 256,
            maxOnScreenColors = 16777216,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 16384,
            maxSpritesPerScanline = 1024,
            spriteTileSize = "Swizzled 4bpp/8bpp CLUT or DXT1/3/5 Hardware Sprites",
            targetSpriteResolution = 64,
            hasHardware3D = true,
            recommendedModelTriBudget = 2200,
            maxSceneTrisPerFrame = 30000,
            vertexSnapGrid = 0f,
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.PS2_EE_GS_EDRAM,
            paletteType = PaletteConstraintType.TRUECOLOR_24BIT,
            chips = listOf(
                ChipSpec(
                    name = "PSP Allegrex CPU + VFPU + Media Engine",
                    role = "Dual MIPS R4000 Cores + 128-Register Matrix/Vector FPU",
                    architecture = "32-bit MIPS + 8×(4×4) Matrix Register File (2.6 GFLOPS)",
                    clockSpeed = "1–333 MHz",
                    busAndProcess = "90nm CMOS Multi-Chip Module",
                    technicalNotes = "VFPU supports native 4×4 matrix multiplies, quaternions, and trigonometry in single instructions."
                ),
                ChipSpec(
                    name = "PSP Rendering Engine (GU)",
                    role = "Hardware T&L, Bezier/Spline Surface Tessellator & Rasterizer",
                    architecture = "Hardware 8-Bone Vertex Skinning + 8 Hardware Lights + 2 MB eDRAM",
                    clockSpeed = "166 MHz",
                    busAndProcess = "5.3 GB/s Embedded eDRAM Bus",
                    technicalNotes = "Includes hardware Bezier patch and B-Spline subdivision, hardware morphing (8 targets), and DXT texture compression."
                )
            ),
            spritePipelineExplanation = "Uses swizzled 4bpp/8bpp CLUT or DXT-compressed textures in 2 MB eDRAM for 60 FPS widescreen 2D/3D action.",
            model3DPipelineExplanation = "Hardware T&L with 8-weight skeletal skinning and environment-mapped specular highlights."
        ),

        // ==================== HD & MODERN APU ====================
        ConsoleHardwareSpec(
            id = "nintendo_switch",
            name = "Nintendo Switch (Tegra X1)",
            shortName = "Switch (Tegra X1)",
            releaseYear = 2017,
            manufacturer = "Nintendo / NVIDIA",
            category = HardwareCategory.HANDHELD,
            era = HardwareEra.ERA_MODERN,
            nativeResolution = "1280 × 720 (Handheld) / 1920 × 1080 (Docked)",
            screenWidth = 1280,
            screenHeight = 720,
            cpuSummary = "Quad-Core ARM Cortex-A57 @ 1.02 GHz",
            gpuOrPpuSummary = "NVIDIA GM20B Maxwell GPU (256 CUDA Cores) @ 307–768 MHz",
            memorySummary = "4 GB LPDDR4 Unified Memory (25.6 GB/s Bandwidth)",
            mainRamBytes = 4294967296L,
            vramBytes = 3221225472L,
            textureCacheBytes = 67108864,
            bitsPerPixelSprite = 32,
            maxSpriteColorsPerPalette = 16777216,
            maxOnScreenColors = 16777216,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 65536,
            maxSpritesPerScanline = 8192,
            spriteTileSize = "ASTC / BC7 Hardware Compressed Texture Atlases",
            targetSpriteResolution = 128,
            hasHardware3D = true,
            recommendedModelTriBudget = 25000,
            maxSceneTrisPerFrame = 450000,
            vertexSnapGrid = 0f,
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.MODERN_PBR_SHADER,
            paletteType = PaletteConstraintType.TRUECOLOR_24BIT,
            chips = listOf(
                ChipSpec(
                    name = "NVIDIA Custom Tegra X1 (T210 / T214 Mariko)",
                    role = "Unified ARMv8 CPU + 256-Core Maxwell GPU SoC",
                    architecture = "4× ARM Cortex-A57 + 2 SMMs (256 CUDA Cores, FP16 1 TFLOP)",
                    clockSpeed = "1.02 GHz CPU / 307.2–460 MHz (Portable) to 768 MHz (Docked) GPU",
                    busAndProcess = "20nm (T210) / 16nm FinFET (T214) w/ 128-bit LPDDR4",
                    technicalNotes = "Supports native hardware ASTC (Adaptive Scalable Texture Compression) from 4×4 down to 12×12 blocks, programmable Vulkan/NVN shaders, and half-precision FP16 double-rate compute."
                )
            ),
            spritePipelineExplanation = "Full 32-bit RGBA or ASTC-compressed texture atlases rendered via the low-overhead NVN graphics API.",
            model3DPipelineExplanation = "Programmable vertex, tessellation, geometry, and fragment PBR shaders with normal mapping and dynamic shadows."
        ),

        ConsoleHardwareSpec(
            id = "ps5_series_x",
            name = "PlayStation 5 / Xbox Series X (9th Gen RDNA 2)",
            shortName = "PS5 / Series X (RDNA 2)",
            releaseYear = 2020,
            manufacturer = "Sony / Microsoft / AMD",
            category = HardwareCategory.CONSOLE,
            era = HardwareEra.ERA_MODERN,
            nativeResolution = "3840 × 2160 (4K UHD @ 60–120 FPS)",
            screenWidth = 3840,
            screenHeight = 2160,
            cpuSummary = "8-Core / 16-Thread AMD Zen 2 @ 3.5–3.8 GHz",
            gpuOrPpuSummary = "AMD Custom RDNA 2 GPU (36–52 CUs, 10.3–12.15 TFLOPS + Ray Tracing)",
            memorySummary = "16 GB GDDR6 Unified Memory (448–560 GB/s) + Custom NVMe I/O Engine",
            mainRamBytes = 17179869184L,
            vramBytes = 13958643712L,
            textureCacheBytes = 536870912,
            bitsPerPixelSprite = 32,
            maxSpriteColorsPerPalette = 16777216,
            maxOnScreenColors = 16777216,
            masterPaletteSize = 16777216,
            maxSpritesTotal = 1000000,
            maxSpritesPerScanline = 65536,
            spriteTileSize = "BC7 / Kraken / BCPack Hardware-Decompressed Atlases",
            targetSpriteResolution = 256,
            hasHardware3D = true,
            recommendedModelTriBudget = 120000,
            maxSceneTrisPerFrame = 10000000,
            vertexSnapGrid = 0f,
            hasZBuffer = true,
            renderProfile3D = Hardware3DRenderProfile.MODERN_PBR_SHADER,
            paletteType = PaletteConstraintType.TRUECOLOR_24BIT,
            chips = listOf(
                ChipSpec(
                    name = "AMD Custom Zen 2 + RDNA 2 SoC (Oberon / Arden)",
                    role = "Unified 8-Core CPU + Mesh Shader & Ray-Tracing GPU",
                    architecture = "8× Zen 2 Cores (AVX2) + RDNA 2 CUs w/ Hardware BVH Ray Intersection",
                    clockSpeed = "3.5–3.8 GHz CPU / 1.825–2.23 GHz GPU",
                    busAndProcess = "TSMC N7 / N6 FinFET, 256/320-bit GDDR6 Bus",
                    technicalNotes = "Replaces traditional vertex shaders with Primitive/Mesh Shaders capable of culling sub-pixel micro-polygon clusters (Nanite-style geometry) at billions of triangles per second."
                ),
                ChipSpec(
                    name = "Custom I/O Complex (Kraken / Velocity DirectStorage + Tempest 3D)",
                    role = "Hardware Decompression & 3D Spatial Audio DSP",
                    architecture = "5.5–9 GB/s Hardware Decompressor + Custom RDNA Compute Audio Engine",
                    clockSpeed = "Multi-GB/s Dedicated DMA",
                    busAndProcess = "PCIe 4.0 x4 NVMe Direct-to-VRAM Pipeline",
                    technicalNotes = "Streams mipmaps and mesh clusters directly from SSD into GPU memory in single frames."
                )
            ),
            spritePipelineExplanation = "Unconstrained 4K HDR sprite sheets with BC7 lossless-grade block compression and programmable shader effects.",
            model3DPipelineExplanation = "Hardware Mesh Shaders, Physically Based Rendering (PBR), and BVH hardware ray-traced reflections and global illumination."
        )
    )

    private val customFusedRegistry = mutableMapOf<String, ConsoleHardwareSpec>()

    fun registerCustomFusedSpec(spec: ConsoleHardwareSpec) {
        customFusedRegistry[spec.id] = spec
    }

    val allCuratedFusedSystems: List<ConsoleHardwareSpec> by lazy {
        ChipFusionEngine.curatedFusionBlueprints.map { bp ->
            val host = allSystems.find { it.id == bp.hostSystemId } ?: allSystems.first()
            val donors = buildList {
                add(ChipFusionEngine.getDonorById(bp.primaryDonorChipId))
                if (bp.secondaryDonorChipId != null) {
                    add(ChipFusionEngine.getDonorById(bp.secondaryDonorChipId))
                }
            }
            ChipFusionEngine.synthesizeFusedSystemSpec(host, donors, bp.interconnect)
        }
    }

    fun allSystemsWithFusions(): List<ConsoleHardwareSpec> {
        val baseIds = allSystems.map { it.id }.toSet()
        val fanSystems = FanChipCatalog.standaloneFanSystemSpecs.filterNot { it.id in baseIds }
        val fanIds = fanSystems.map { it.id }.toSet()
        val curated = allCuratedFusedSystems.filterNot { it.id in baseIds || it.id in fanIds }
        val custom = customFusedRegistry.values.filterNot { spec ->
            spec.id in baseIds || spec.id in fanIds || curated.any { it.id == spec.id }
        }
        return allSystems + fanSystems + custom + curated
    }

    fun getById(id: String): ConsoleHardwareSpec {
        allSystems.find { it.id == id }?.let { return it }
        customFusedRegistry[id]?.let { return it }
        FanChipCatalog.standaloneFanSystemSpecs.find { it.id == id }?.let { return it }
        allCuratedFusedSystems.find { it.id == id }?.let { return it }
        return allSystems.first { it.id == "ps1" }
    }
}
