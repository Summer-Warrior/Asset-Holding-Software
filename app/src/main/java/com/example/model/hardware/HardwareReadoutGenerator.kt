package com.example.model.hardware

import android.graphics.Color
import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData
import kotlin.math.min

enum class ReadingLevelMode(val label: String, val shortLabel: String) {
    EVERYDAY_READER("Everyday Reader (Plain-English & Visual)", "Everyday Level"),
    HIGH_END_TECHNICAL("High-End Technical (Silicon & Register Engineering)", "Technical Level"),
    BOTH_LEVELS("Dual Readout (Both Technical & Everyday)", "Both Levels")
}

data class ReadoutSection(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val highEndTechnicalText: String,
    val everydayReaderText: String,
    val keyMetrics: List<Pair<String, String>>
)

data class FullAssetHardwareDossier(
    val systemName: String,
    val assetName: String,
    val assetTypeLabel: String,
    val executiveHeadline: String,
    val sections: List<ReadoutSection>,
    val hardwareRegisterHexDump: String
)

object HardwareReadoutGenerator {

    fun generate3DMeshDossier(
        mesh: Mesh3D,
        spec: ConsoleHardwareSpec,
        report: MeshHardwareReport
    ): FullAssetHardwareDossier {
        val vCount = mesh.vertexCount
        val fCount = mesh.faceCount
        val rawFloatBytes = vCount * 36 + fCount * 16 // 3x float pos + 3x float norm + 2x float uv + 4B color

        val coordPrecisionTech = when (spec.era) {
            HardwareEra.ERA_8BIT -> "8-bit signed integer (-128..+127) screen-space endpoints"
            HardwareEra.ERA_16BIT -> "16-bit fixed-point (Q8.8) cartridge co-processor coordinates"
            HardwareEra.ERA_32_64BIT -> if (spec.id == "n64") "16-bit fixed-point (s10.5) RSP sub-pixel coordinates" else "16-bit fixed-point (Q4.12) GTE/VDP1/ARM9 coordinates"
            else -> "32-bit IEEE-754 floating-point / 16-bit half-float SIMD vectors"
        }

        // Section 1: Raw Studio Asset Intake
        val sec1 = ReadoutSection(
            stepNumber = 1,
            title = "Raw Studio 3D Asset Intake (Pre-Adjustment)",
            subtitle = "Unconstrained Floating-Point Geometry & Color Profile",
            highEndTechnicalText = buildString {
                append("Source asset '${mesh.name}' enters the pipeline as an unconstrained 32-bit IEEE-754 floating-point mesh containing ")
                append("$vCount unique vertices and $fCount triangulated faces (${fCount * 3} vertex indices). ")
                append("In studio memory, each vertex occupies 36 bytes (12B XYZ position, 12B surface normal, 8B UV coordinates, 4B ARGB8888 color), ")
                append("plus ${fCount * 16} bytes of face topology and material metadata, totaling $rawFloatBytes bytes (${String.format("%.2f", rawFloatBytes / 1024f)} KB) of raw uncompressed geometry.")
            },
            everydayReaderText = buildString {
                append("Right now, your 3D model '${mesh.name}' exists in modern, ultra-precise studio quality. ")
                append("It is built out of $vCount corner points (vertices) connected into $fCount flat triangle surfaces, painted with millions of possible smooth colors. ")
                append("In this raw state, the points are positioned with microscopic decimal accuracy, and there are no limits on how many triangles or colors it can use.")
            },
            keyMetrics = listOf(
                "Studio Vertices" to "$vCount (32-bit Float)",
                "Studio Triangles" to "$fCount Faces",
                "Raw Studio Size" to "$rawFloatBytes Bytes",
                "Color Space" to "32-Bit ARGB (16.7M Colors)"
            )
        )

        // Section 2: App Adjustment & Silicon Adaptation Pipeline
        val sec2 = ReadoutSection(
            stepNumber = 2,
            title = "App Transformation & Silicon Adaptation Pipeline",
            subtitle = "How PixelForge Reshapes & Quantizes the Asset for ${spec.shortName}",
            highEndTechnicalText = buildString {
                append("To make '${mesh.name}' executable on ${spec.name} (${spec.releaseYear}), PixelForge's adaptation pipeline performs 4 mandatory binary transformations:\n")
                append("1. Coordinate Quantization: Converts 32-bit float XYZ vertices into $coordPrecisionTech, compacting the vertex buffer from ${vCount * 36} B down to ${report.vertexBufferBytes} B.\n")
                when (spec.renderProfile3D) {
                    Hardware3DRenderProfile.NONE_2D_WIREFRAME,
                    Hardware3DRenderProfile.CX4_TRIG_WIREFRAME -> {
                        append("2. Edge Extraction & Planar Bitplane Packing: Because ${spec.shortName} lacks a polygon fill rasterizer, the app extracts unique wireframe edge pairs from the $fCount triangles and pre-computes Bresenham line-plot tables targeting ${spec.bitsPerPixelSprite}bpp planar CHR tiles.\n")
                    }
                    Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
                    Hardware3DRenderProfile.SVP_DSP_FLAT -> {
                        append("2. Co-Processor Display List & Bayer Matrix Baking: Formats the $fCount faces into a compact ${report.faceTableBytes}-byte cartridge RAM command stream with pre-calculated face normals and 4×4 ordered Bayer dither flags (`CMODE` bitplane plot indices) to synthesize mid-tone shading inside a ${spec.maxSpriteColorsPerPalette}-color (${spec.bitsPerPixelSprite}bpp) palette.\n")
                    }
                    Hardware3DRenderProfile.PS1_GTE_AFFINE_WOBBLE -> {
                        append("2. GTE Packet & Ordering Table (OT) Formatting: Packs the $fCount triangles into `POLY_FT3` / `POLY_G3` GPU command packets (${report.faceTableBytes} B total) linked into a Z-bucket Ordering Table, and quantizes surface colors to 15-bit BGR555 with hardware 4×4 dither bits enabled.\n")
                    }
                    Hardware3DRenderProfile.SATURN_VDP1_QUAD_MESH -> {
                        append("2. Triangle-to-Distorted-Quad Conversion: Because Sega Saturn's VDP1 hardware exclusively renders 4-vertex distorted sprites (quadrilaterals), the app converts all $fCount triangles into $fCount degenerate 4-corner VDP1 command tables (duplicating Vertex C into Vertex D) and replaces alpha blending with VDP1 50% checkerboard screen-door stipple flags.\n")
                    }
                    Hardware3DRenderProfile.N64_RCP_TMEM_3POINT -> {
                        append("2. Fast3D / F3DEX2 Microcode Display List & 4KB TMEM Tiling: Compiles the $fCount triangles into 64-bit RCP `G_TRI1`/`G_TRI2` display list commands and slices texture maps into 32×32 tiles (${report.estimatedTextureBytes} B) so every texture load (`gDPLoadTextureBlock`) fits strictly inside the RDP's 4,096-byte on-chip TMEM cache.\n")
                    }
                    Hardware3DRenderProfile.NDS_FIXED_TOON_2048 -> {
                        append("2. ARM9 GX_FIFO Command Packing & Toon Table Quantization: Packs vertices into 16-bit `vtx16` commands for the 4×4 hardware matrix stack, maps diffuse lighting intensities into the NDS 32-entry hardware Toon Shading table, and verifies geometry against the strict 2,048-polygon / 6,144-vertex Polygon RAM ceiling (Current usage: $fCount / 2,048 tris).\n")
                    }
                    else -> {
                        append("2. SIMD Triangle Strip & Hardware Texture Compression: Optimizes the $fCount faces into cache-coherent indexed triangle strips (${report.faceTableBytes} B) and compresses surface textures (${report.estimatedTextureBytes} B) for burst DMA transfer into ${spec.shortName} VRAM.\n")
                    }
                }
                append("3. Palette & DAC Mapping: Maps all face colors into ${spec.shortName}'s ${spec.masterPaletteSize}-color master palette (${spec.bitsPerPixelSprite}bpp).")
            },
            everydayReaderText = buildString {
                append("To get '${mesh.name}' ready to run on a real ${spec.name}, the app acts like a translator and a sculptor:\n")
                append("• Snapping the Corners: Modern 3D points use super-fine decimals, which older console chips can't digest quickly. The app rounds all $vCount points onto a simpler grid that ${spec.shortName}'s processor can calculate in a split second.\n")
                when (spec.renderProfile3D) {
                    Hardware3DRenderProfile.NONE_2D_WIREFRAME,
                    Hardware3DRenderProfile.CX4_TRIG_WIREFRAME -> {
                        append("• Turning Solid Shapes into Glowing Blueprints: Since ${spec.shortName} was built for 2D games and can't fill solid 3D shapes fast enough, the app converts your model's $fCount triangles into a glowing 3D wireframe outline—just like classic vector arcade games!\n")
                    }
                    Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
                    Hardware3DRenderProfile.SVP_DSP_FLAT -> {
                        append("• Checkerboard Shading Trick: Because ${spec.shortName} can only use ${spec.maxSpriteColorsPerPalette} colors at once on this layer, the app uses a classic 1990s checkerboard pixel pattern (dithering) on angled surfaces so your eyes blend the dots into smooth 3D shadows.\n")
                    }
                    Hardware3DRenderProfile.PS1_GTE_AFFINE_WOBBLE -> {
                        append("• Classic 90s PlayStation Conversion: The app packages your $fCount triangles for Sony's GTE math chip. Because the PlayStation doesn't have a depth map (Z-buffer), the app sets up a back-to-front sorting list so the furthest triangles draw first and the closest triangles draw on top.\n")
                    }
                    Hardware3DRenderProfile.SATURN_VDP1_QUAD_MESH -> {
                        append("• Turning Triangles into Stretched Squares: Uniquely, the Sega Saturn doesn't draw triangles—it stretches 2D rectangles in 3D space! The app pinches two corners of a 4-sided square together to form each of your $fCount triangles, and uses a tiny 'screen-door' mesh pattern for see-through parts.\n")
                    }
                    Hardware3DRenderProfile.N64_RCP_TMEM_3POINT -> {
                        append("• Slicing Textures for N64's Tiny 4KB Pocket: The Nintendo 64 has a super-fast texture memory pocket, but it can only hold 4 Kilobytes at a time! The app slices your model's surface patterns into tiny 32×32 patches and blends them with smooth vertex colors.\n")
                    }
                    Hardware3DRenderProfile.NDS_FIXED_TOON_2048 -> {
                        append("• Handheld Cel-Shading & Polygon Cap Check: The Nintendo DS has a strict hardware limit of 2,048 triangles on screen at once. Your model uses $fCount triangles (${String.format("%.0f", report.assetBudgetPercent)}% of a single character budget), and the app maps its lighting into crisp comic-book style toon shading bands.\n")
                    }
                    else -> {
                        append("• High-Speed Strip Packing: The app chains your $fCount triangles into continuous ribbons (triangle strips) so ${spec.shortName}'s graphics chip can draw the entire model in one smooth burst.\n")
                    }
                }
                append("• Shrinking the Memory Size: After adjustment, your model's geometry shrinks from $rawFloatBytes bytes down to just ${report.vertexBufferBytes + report.faceTableBytes} bytes!")
            },
            keyMetrics = listOf(
                "Adjusted Vertex RAM" to "${report.vertexBufferBytes} Bytes",
                "Adjusted Face/Cmd List" to "${report.faceTableBytes} Bytes",
                "Total Asset Footprint" to "${report.totalAssetRamBytes} Bytes",
                "Per-Asset Tri Budget" to "$fCount / ${report.recommendedTriBudget} (${String.format("%.0f", report.assetBudgetPercent)}%)"
            )
        )

        // Section 3: Silicon Chip Execution & Bus Flow
        val chipNames = spec.chips.joinToString(" -> ") { it.name }
        val sec3 = ReadoutSection(
            stepNumber = 3,
            title = "Silicon Chip Execution & Bus Pipeline",
            subtitle = "How $chipNames Process the Adjusted Asset",
            highEndTechnicalText = buildString {
                append("During each 16.67ms (60 Hz) or 33.33ms (30 Hz) frame interval on ${spec.name}:\n")
                spec.chips.forEachIndexed { idx, chip ->
                    append("• [IC ${idx + 1}] ${chip.name} (${chip.clockSpeed}, ${chip.role}): ${chip.technicalNotes}\n")
                }
                append("• Memory Bus & Cache Verdict: ${report.textureCacheNote}")
            },
            everydayReaderText = buildString {
                append("Once you press play, here is how the chips inside ${spec.name} team up to draw '${mesh.name}' on screen:\n")
                spec.chips.forEach { chip ->
                    append("• ${chip.name} (${chip.role}): Running at ${chip.clockSpeed}, this chip acts as the ${chip.role.lowercase()}. ")
                }
                append("\nTogether, the main processor figures out where your model is rotated in 3D space, and hands the adjusted triangle list to the graphics chip (${spec.gpuOrPpuSummary}), which paints the pixels onto a ${spec.nativeResolution} screen.")
            },
            keyMetrics = listOf(
                "Primary CPU" to spec.cpuSummary.take(24),
                "Graphics Silicon" to spec.gpuOrPpuSummary.take(24),
                "System Memory" to "${spec.mainRamBytes / 1024} KB Main / ${spec.vramBytes / 1024} KB VRAM",
                "Z-Buffer Hardware" to if (spec.hasZBuffer) "Hardware Z-Buffer Active" else "None (Painter's Sort)"
            )
        )

        // Section 4: Vivid On-Screen Output Readout
        val sec4 = ReadoutSection(
            stepNumber = 4,
            title = "Vivid On-Screen Visual Output Readout",
            subtitle = "Exact Visual Appearance & Hardware Artifacts When Outputted",
            highEndTechnicalText = buildString {
                append("Final Raster Output at ${spec.nativeResolution} (${spec.maxOnScreenColors} max on-screen colors from ${spec.masterPaletteSize} master palette):\n")
                when (spec.renderProfile3D) {
                    Hardware3DRenderProfile.PS1_GTE_AFFINE_WOBBLE -> {
                        append("• Vertex Snap Jitter: Because the GTE outputs integer XY screen coordinates without sub-pixel fractional bits (simulated at grid step ${spec.vertexSnapGrid}), vertices visibly snap from pixel to pixel as the camera orbits.\n")
                        append("• Affine UV Swim & 4×4 Dither: Lacking perspective-correct 1/Z division per pixel, textures warp diagonally across large triangles, while the CXD8514Q GPU applies a 4×4 ordered dither matrix to prevent 15-bit RGB555 color banding.")
                    }
                    Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
                    Hardware3DRenderProfile.SVP_DSP_FLAT -> {
                        append("• Low-Res Framebuffer Window & Bitplane Dither: Rendered into a ${spec.screenWidth}×${spec.screenHeight} tile-mapped window transferred over VBLANK DMA at 15–20 FPS. Flat-shaded faces exhibit crisp 2×2 / 4×4 crosshatch dithering between adjacent ${spec.bitsPerPixelSprite}bpp palette indices.")
                    }
                    Hardware3DRenderProfile.SATURN_VDP1_QUAD_MESH -> {
                        append("• Forward Quad Rasterization & Stipple Mesh: VDP1 steps across each 4-point quad using forward texture coordinates. Translucent or highlighted surfaces render as an alternating 50% checkerboard 'screen-door' mesh pattern.")
                    }
                    Hardware3DRenderProfile.N64_RCP_TMEM_3POINT -> {
                        append("• Sub-Pixel Locked Geometry & 3-Point Bilinear Filter: Vertices glide smoothly with zero PS1-style wobble thanks to sub-pixel precision and hardware Z-buffering. Textures exhibit the N64's signature soft 3-point triangular bilinear interpolation blended with atmospheric depth fog.")
                    }
                    Hardware3DRenderProfile.NDS_FIXED_TOON_2048 -> {
                        append("• Stepped Hardware Toon Shading & Edge Marking: Lighting snaps cleanly into 3 quantized cel-shaded bands via the NDS hardware Toon Table, framed by crisp 1-pixel hardware contour outlines at 256×192 LCD resolution.")
                    }
                    Hardware3DRenderProfile.NONE_2D_WIREFRAME,
                    Hardware3DRenderProfile.CX4_TRIG_WIREFRAME -> {
                        append("• High-Contrast Vector Wireframe Projection: Displayed as glowing 1-pixel wide Bresenham vector edges over scanlines at ${spec.nativeResolution}, restricted to ${spec.shortName}'s ${spec.bitsPerPixelSprite}bpp color output.")
                    }
                    else -> {
                        append("• Sub-Pixel Perspective-Correct High-Fillrate Output: Smoothly interpolated Gouraud/Phong/PBR surfaces with hardware specular highlights, trilinear/anisotropic texture filtering, and 24-bit/32-bit truecolor precision.")
                    }
                }
            },
            everydayReaderText = buildString {
                append("When you look at the screen on ${spec.name}, here is exactly what '${mesh.name}' looks like in motion:\n")
                when (spec.renderProfile3D) {
                    Hardware3DRenderProfile.PS1_GTE_AFFINE_WOBBLE -> {
                        append("• That Iconic 1995 PlayStation Look: As your model spins, you'll see the corners gently 'shimmer' and snap from pixel to pixel—the famous PS1 polygon wobble! The colors have a warm, slightly grainy 16-bit dither texture, and CRT scanlines give the whole model a nostalgic mid-90s arcade glow.")
                    }
                    Hardware3DRenderProfile.SUPER_FX_DITHER_FLAT,
                    Hardware3DRenderProfile.SVP_DSP_FLAT -> {
                        append("• Chunky 16-Bit Star Fox / Virtua Racing Vibe: Your model looks like a bold, low-poly origami sculpture! Flat-colored panels catch the light cleanly, and angled sides show a cool retro checkerboard pixel pattern where the chip blends two colors together.")
                    }
                    Hardware3DRenderProfile.SATURN_VDP1_QUAD_MESH -> {
                        append("• Bold Arcade Quads & Screen-Door Glass: The model has crisp, solid arcade-style surfaces. Where parts of the model have energy glow or transparency, you can see a tiny checkerboard mesh pattern—a signature visual quirk of 1990s Sega Saturn games!")
                    }
                    Hardware3DRenderProfile.N64_RCP_TMEM_3POINT -> {
                        append("• Rock-Solid N64 Geometry with Soft Misty Depth: Unlike the PlayStation, the corners of your model never wobble—they glide with silky smoothness! Surfaces look soft and rounded, and the back of the model gently fades into atmospheric N64 horizon mist.")
                    }
                    Hardware3DRenderProfile.NDS_FIXED_TOON_2048 -> {
                        append("• Crisp Handheld Cel-Shaded Cartoon Style: On the 256×192 DS screen, your model looks like a living comic book or anime prop! Instead of blurry shadows, the light steps in bold, clean bands wrapped in a dark ink outline.")
                    }
                    Hardware3DRenderProfile.NONE_2D_WIREFRAME,
                    Hardware3DRenderProfile.CX4_TRIG_WIREFRAME -> {
                        append("• Retro Sci-Fi Hologram Blueprint: Your model appears as a glowing, high-contrast 3D wireframe hologram! Every triangle edge is drawn in sharp retro pixels, pulsing across the scanlines.")
                    }
                    else -> {
                        append("• Glossy, High-Definition Modern Showcase: Every angle of your model is razor-sharp with zero pixel wobble, smooth metallic reflections, bright specular light highlights, and vibrant full-spectrum colors.")
                    }
                }
            },
            keyMetrics = listOf(
                "Output Resolution" to spec.nativeResolution.substringBefore(" ("),
                "Vertex Stability" to if (spec.vertexSnapGrid > 0f) "Integer Snap (±${spec.vertexSnapGrid}px Wobble)" else "Sub-Pixel Smooth",
                "Shading Pipeline" to spec.renderProfile3D.badge.take(26),
                "Compatibility" to report.compatibility.label
            )
        )

        val hexDump = buildString {
            appendLine("// ${spec.name.uppercase()} — ADJUSTED 3D ASSET HARDWARE DUMP")
            appendLine("// Asset: ${mesh.name} | Target Silicon: ${spec.gpuOrPpuSummary}")
            appendLine("ASSET_HEADER_3D:")
            appendLine("  .word 0x${vCount.toString(16).uppercase().padStart(4, '0')}      ; VERTEX_COUNT ($vCount)")
            appendLine("  .word 0x${fCount.toString(16).uppercase().padStart(4, '0')}      ; PRIMITIVE_COUNT ($fCount)")
            appendLine("  .dword 0x${report.totalAssetRamBytes.toString(16).uppercase().padStart(8, '0')} ; TOTAL_RAM_BYTES (${report.totalAssetRamBytes} B)")
            appendLine("VERTEX_TABLE_FIXED_POINT:")
            mesh.vertices.take(4).forEachIndexed { idx, v ->
                val fx = (v.x * 4096).toInt() and 0xFFFF
                val fy = (v.y * 4096).toInt() and 0xFFFF
                val fz = (v.z * 4096).toInt() and 0xFFFF
                appendLine("  VTX_$idx: .short 0x${fx.toString(16).uppercase().padStart(4, '0')}, 0x${fy.toString(16).uppercase().padStart(4, '0')}, 0x${fz.toString(16).uppercase().padStart(4, '0')} ; (${String.format("%.2f", v.x)}, ${String.format("%.2f", v.y)}, ${String.format("%.2f", v.z)})")
            }
            if (mesh.vertices.size > 4) {
                appendLine("  ... ; +${mesh.vertices.size - 4} additional quantized vertices (${report.vertexBufferBytes} bytes total)")
            }
        }

        return FullAssetHardwareDossier(
            systemName = spec.name,
            assetName = mesh.name,
            assetTypeLabel = "3D Polygon Mesh (${mesh.vertexCount}V / ${mesh.faceCount}F)",
            executiveHeadline = "How '${mesh.name}' is adapted, processed by ${spec.shortName}'s silicon, and outputted at ${spec.nativeResolution}.",
            sections = listOf(sec1, sec2, sec3, sec4),
            hardwareRegisterHexDump = hexDump
        )
    }

    fun generate2DSpriteDossier(
        sheet: SpriteSheetData,
        spec: ConsoleHardwareSpec,
        report: SpriteHardwareReport
    ): FullAssetHardwareDossier {
        val rawW = sheet.frameWidth
        val rawH = sheet.frameHeight
        val frameCount = sheet.frames.size
        val rawFrameBytes = rawW * rawH * 4
        val totalSheetRamAdjusted = report.chrVramBytes * frameCount

        val tileFormatTech = when (spec.paletteType) {
            PaletteConstraintType.DMG_4_OLIVE -> "2bpp Interleaved Planar (2 bytes per 8-pixel row = 16 bytes per 8×8 CHR tile)"
            PaletteConstraintType.NES_2C02_2BPP_54 -> "2bpp Split Planar (Bitplane 0 at +0..+7, Bitplane 1 at +8..+15 = 16 bytes per 8×8 CHR tile)"
            PaletteConstraintType.ATARI_2600_NTSC_SCANLINE -> "1bpp 8-bit GRP0/GRP1 Horizontal Shift Register + Per-Scanline COLUP0 Lookup Table"
            PaletteConstraintType.C64_16_MULTICOLOR_WIDE -> "2bpp Multicolor MOB Block (12 double-wide bit-pairs × 21 rows = exactly 63 bytes per hardware sprite)"
            PaletteConstraintType.GENESIS_9BIT_4BPP -> "4bpp Linear Packed Nibbles (4 bytes per 8-pixel row = 32 bytes per 8×8 VDP pattern cell)"
            PaletteConstraintType.SNES_15BIT_4BPP -> "4bpp Composite Interleaved Bitplanes (Bitplanes 0/1 + Bitplanes 2/3 = 32 bytes per 8×8 S-PPU tile)"
            PaletteConstraintType.VIRTUAL_BOY_4_RED -> "2bpp Packed 4-Shade Red LED Character Cells (16 bytes per 8×8 VIP cell)"
            else -> "${spec.bitsPerPixelSprite}bpp Hardware Texture / Sprite Atlas Cell (${(64 * spec.bitsPerPixelSprite) / 8} bytes per 8×8 block)"
        }

        // Section 1: Raw Studio Sprite Sheet Intake
        val sec1 = ReadoutSection(
            stepNumber = 1,
            title = "Raw Studio 2D Sprite Intake (Pre-Adjustment)",
            subtitle = "Uncompressed 32-Bit ARGB8888 Frame & Atlas Profile",
            highEndTechnicalText = buildString {
                append("Source spritesheet '${sheet.name}' consists of $frameCount animation frames sliced on a ${sheet.columns}×${sheet.rows} grid. ")
                append("Each raw studio frame measures ${rawW}×${rawH} pixels in 32-bit ARGB8888 format ($rawFrameBytes bytes per frame; ${rawFrameBytes * frameCount} bytes for the complete $frameCount-frame sheet) ")
                append("with unrestricted 8-bit alpha channel and 24-bit RGB color depth.")
            },
            everydayReaderText = buildString {
                append("Your sprite sheet '${sheet.name}' starts out as a modern digital image with $frameCount animation frames (${sheet.columns} columns by ${sheet.rows} rows). ")
                append("Each frame is ${rawW}×${rawH} pixels and can use millions of smooth colors without any rules on how many colors touch each other or how much memory each frame takes.")
            },
            keyMetrics = listOf(
                "Studio Frame Size" to "${rawW} × ${rawH} px",
                "Animation Frames" to "$frameCount Frames (${sheet.animations.size} Tracks)",
                "Raw Frame RAM" to "$rawFrameBytes Bytes (32-bit)",
                "Raw Full Sheet" to "${rawFrameBytes * frameCount} Bytes"
            )
        )

        // Section 2: App Transformation & PPU/VDP Quantization
        val sec2 = ReadoutSection(
            stepNumber = 2,
            title = "App Transformation & PPU/VDP Bitplane Packing",
            subtitle = "How PixelForge Slices, Quantizes & Packs '${sheet.name}' for ${spec.shortName}",
            highEndTechnicalText = buildString {
                append("To output '${sheet.name}' through ${spec.name}'s ${spec.gpuOrPpuSummary}, PixelForge executes 3 hardware-accurate stages:\n")
                append("1. Spatial Resampling & Aspect Calibration: Resamples each ${rawW}×${rawH} frame to ${spec.targetSpriteResolution}×${spec.targetSpriteResolution} hardware pixels ")
                if (spec.paletteType == PaletteConstraintType.C64_16_MULTICOLOR_WIDE || spec.paletteType == PaletteConstraintType.ATARI_2600_NTSC_SCANLINE) {
                    append("with a 2:1 wide horizontal pixel clock divider (${spec.targetSpriteResolution / 2}×${spec.targetSpriteResolution} logical bit-cells).\n")
                } else {
                    append("aligned on 8×8 pixel hardware tile boundaries (${report.chrTilesWide}×${report.chrTilesHigh} = ${report.totalChrTiles} CHR tiles).\n")
                }
                append("2. Color Space & Sub-Palette Quantization: Maps 32-bit ARGB pixels into ${spec.shortName}'s ${spec.masterPaletteSize}-color hardware DAC domain and clusters frequencies down to ${report.hardwarePaletteSwatches.size} active visible colors + 1 transparent index (0x00), respecting the hardware limit of ${spec.maxSpriteColorsPerPalette} colors per sprite sub-palette.\n")
                append("3. Binary Bitplane / Nibble Encoding: Encodes pixels into $tileFormatTech, compressing a single frame from $rawFrameBytes bytes down to ${report.chrVramBytes} bytes ($totalSheetRamAdjusted bytes for all $frameCount animation frames).")
            },
            everydayReaderText = buildString {
                append("Old and classic game consoles don't draw whole pictures—they build sprites out of tiny 8×8 pixel 'Lego blocks' called tiles, painted from a small paintbox. Here is how the app adjusts your sprite so ${spec.name} can display it:\n")
                append("• Sizing to the Console Grid: The app resizes your sprite to ${spec.targetSpriteResolution}×${spec.targetSpriteResolution} pixels and neatly slices it into a ${report.chrTilesWide}×${report.chrTilesHigh} grid (${report.totalChrTiles} total 8×8 blocks).\n")
                append("• Matching the Console's Paintbox: ${spec.shortName} only allows ${spec.maxSpriteColorsPerPalette - 1} visible colors (plus see-through background) on a single sprite. The app analyzes your sprite, picks the ${report.hardwarePaletteSwatches.size} most important shades that exist in ${spec.shortName}'s ${spec.masterPaletteSize}-color hardware palette, and repaints every pixel to match.\n")
                append("• Huge Memory Savings: By packing the pixels into ${spec.bitsPerPixelSprite}-bit console tiles, a single frame shrinks from $rawFrameBytes bytes down to just ${report.chrVramBytes} bytes!")
            },
            keyMetrics = listOf(
                "Sliced 8×8 Tiles" to "${report.chrTilesWide}×${report.chrTilesHigh} (${report.totalChrTiles} Tiles)",
                "Packed Frame VRAM" to "${report.chrVramBytes} Bytes (${spec.bitsPerPixelSprite}bpp)",
                "Full $frameCount-Frame Sheet" to "$totalSheetRamAdjusted Bytes",
                "Active Sub-Palette" to "${report.hardwarePaletteSwatches.size} / ${spec.maxSpriteColorsPerPalette - 1} Visible Colors"
            )
        )

        // Section 3: PPU/VDP OAM & Scanline FIFO Execution
        val sec3 = ReadoutSection(
            stepNumber = 3,
            title = "PPU / VDP Silicon & Scanline FIFO Execution",
            subtitle = "How ${spec.gpuOrPpuSummary} Fetches & Composites the Tiles",
            highEndTechnicalText = buildString {
                append("1. VBLANK DMA & CHR Banking: To animate '${sheet.name}' at ${sheet.animations.firstOrNull()?.fps ?: 8} FPS, the CPU either pre-loads all $frameCount frames ($totalSheetRamAdjusted B) into ${spec.vramBytes / 1024} KB VRAM or streams ${report.chrVramBytes} bytes per frame during VBLANK DMA.\n")
                append("2. OAM Evaluation & Secondary Scanline Buffer: Each frame, OAM entries specify X/Y screen coordinates, tile index, palette bank, and H/V flip bits. During HBLANK, ${spec.gpuOrPpuSummary} scans OAM to find sprites intersecting the current scanline.\n")
                append("3. Scanline Overdraw Verdict: This sprite spans ${report.scanlineSpritesUsed} tiles horizontally against ${spec.shortName}'s hardware ceiling of ${report.scanlineLimitMax} sprites per scanline (${String.format("%.1f", (report.scanlineSpritesUsed.toFloat() / report.scanlineLimitMax.coerceAtLeast(1)) * 100f)}% of the entire scanline's sprite budget).")
            },
            everydayReaderText = buildString {
                append("Inside ${spec.name}, the graphics chip (${spec.gpuOrPpuSummary}) draws the TV screen one horizontal line at a time from top to bottom:\n")
                append("• Swapping Animation Frames: To make your sprite move across its $frameCount frames, the console swaps the ${report.chrVramBytes}-byte tile group in the tiny pause between TV frames (called VBLANK).\n")
                append("• The Scanline Speed Limit: Classic consoles can only draw a certain number of sprite blocks on the same horizontal line before the chip runs out of time. Your adjusted sprite uses ${report.scanlineSpritesUsed} out of ${report.scanlineLimitMax} available horizontal slots!")
                if (report.causesScanlineFlicker) {
                    append(" Because this exceeds the single-line limit, the right edge of the sprite would drop out unless the game alternates (flickers) which tiles draw each frame!")
                } else {
                    append(" That leaves room for ${report.scanlineLimitMax - report.scanlineSpritesUsed} more sprite tiles on the exact same horizontal line without any retro sprite flickering!")
                }
            },
            keyMetrics = listOf(
                "VRAM Footprint %" to "${String.format("%.2f", report.vramUsagePercent)}% of System VRAM",
                "Scanline Usage" to "${report.scanlineSpritesUsed} / ${report.scanlineLimitMax} Slots",
                "OAM Flicker Risk" to if (report.causesScanlineFlicker) "HIGH (Exceeds Scanline Cap)" else "NONE (Flicker-Free)",
                "Max Total Sprites" to "${spec.maxSpritesTotal} OAM Entries"
            )
        )

        // Section 4: Vivid On-Screen Output Readout
        val sec4 = ReadoutSection(
            stepNumber = 4,
            title = "Vivid On-Screen Visual Output Readout",
            subtitle = "How Your Adjusted Sprite Looks on ${spec.name}'s Display",
            highEndTechnicalText = buildString {
                append("Visual Signal & Pixel Output Characteristics at ${spec.nativeResolution}:\n")
                when (spec.paletteType) {
                    PaletteConstraintType.DMG_4_OLIVE -> {
                        append("• Reflective STN Dot-Matrix LCD: Outputted as 4 discrete olive-green voltage shading levels (`#0F380F` darkest ink to `#9BBC0F` reflective background) with visible 160×144 sub-pixel LCD grid separation and natural passive-matrix ghosting during fast motion.")
                    }
                    PaletteConstraintType.VIRTUAL_BOY_4_RED -> {
                        append("• Oscillating Red GaAsP LED Array: Rendered in 4 pulse-width modulated monochrome red intensities (`#000000` pitch black void to `#FF1A1A` high-intensity red LED) with crisp horizontal scanline gaps swept at 50.2 Hz.")
                    }
                    PaletteConstraintType.NES_2C02_2BPP_54 -> {
                        append("• Ricoh 2C02 Composite Phase/Luma Signal: Rendered with razor-sharp 3-color + transparent silhouette contrast against 240p NTSC scanlines. Adjacent color transitions exhibit subtle composite NTSC artifact fringing.")
                    }
                    PaletteConstraintType.C64_16_MULTICOLOR_WIDE -> {
                        append("• VIC-II 2:1 Multicolor Wide-Pixel Aspect: Each pixel is twice as wide as it is tall, giving the sprite the unmistakable chunky horizontal proportions and earthy muted 16-color palette of 1980s Commodore 64 games.")
                    }
                    PaletteConstraintType.ATARI_2600_NTSC_SCANLINE -> {
                        append("• TIA Racing-the-Beam Scanline Stretch: Wide 1-bit horizontal player blocks with color changes occurring strictly on horizontal scanline boundaries, separated by deep CRT scanline gaps.")
                    }
                    PaletteConstraintType.GENESIS_9BIT_4BPP -> {
                        append("• YM7101 9-Bit RGB333 High-Contrast Arcade Output: Bold, punchy 15-color palette stepped in 3-bit RGB increments (0, 36, 73..255), producing the high-contrast, slightly metallic shading signature of Sega Genesis action games.")
                    }
                    PaletteConstraintType.SNES_15BIT_4BPP,
                    PaletteConstraintType.SNES_15BIT_8BPP -> {
                        append("• S-PPU 15-Bit BGR555 Rich Gradient Output: Smooth 15-color sub-palette drawn from a 32,768-color master space with soft horizontal pixel filtering and zero color-clash across 8×8 tiles.")
                    }
                    else -> {
                        append("• Crisp Hardware Clut/Truecolor Sprite Output: Clean pixel-accurate silhouette with full sub-palette fidelity and zero scanline dropout.")
                    }
                }
            },
            everydayReaderText = buildString {
                append("Here is what a player actually sees when '${sheet.name}' appears on ${spec.name}:\n")
                when (spec.paletteType) {
                    PaletteConstraintType.DMG_4_OLIVE -> {
                        append("• Classic Pea-Soup Game Boy Charm: Your sprite is transformed into 3 rich shades of dark forest and olive green against a sunlit greenish-yellow background. Every individual square dot on the 160×144 screen has a tiny physical gap around it, looking just like an original 1989 Game Boy held under a lamp!")
                    }
                    PaletteConstraintType.VIRTUAL_BOY_4_RED -> {
                        append("• Glowing Neon-Red Cyberspace: Floating in pitch-black darkness, your sprite glows in 3 fiery shades of ruby red LED light, sliced by fine horizontal scanlines like a 1995 virtual reality visor.")
                    }
                    PaletteConstraintType.NES_2C02_2BPP_54 -> {
                        append("• Iconic 8-Bit NES Cartridge Look: Reduced to 3 bold, carefully chosen colors plus a clean cutout outline, your sprite pops off the screen with the unmistakable crispness of Super Mario Bros. or Mega Man.")
                    }
                    PaletteConstraintType.C64_16_MULTICOLOR_WIDE -> {
                        append("• Chunky Double-Wide 80s Computer Pixels: Because the Commodore 64 trades horizontal sharpness to get extra colors, every pixel in your sprite stretches into a wide little brick, painted in warm, nostalgic 1982 earth tones.")
                    }
                    PaletteConstraintType.ATARI_2600_NTSC_SCANLINE -> {
                        append("• Pure 1977 Atari Arcade Nostalgia: Your sprite becomes a bold, blocky silhouette made of wide horizontal bars, where each horizontal row glows in its own bright TV color!")
                    }
                    PaletteConstraintType.GENESIS_9BIT_4BPP -> {
                        append("• Punchy 16-Bit Sega Arcade Vibe: With up to 15 vibrant colors chosen from Sega's high-contrast 512-color palette, your sprite has deep shadows and bright neon highlights that look ready for Streets of Rage or Sonic.")
                    }
                    else -> {
                        append("• Rich 16-Bit/32-Bit Pixel Art Perfection: Your sprite keeps smooth shading gradients, crisp dark outlines, and vibrant colors across every 8×8 tile.")
                    }
                }
            },
            keyMetrics = listOf(
                "Color Depth" to "${spec.bitsPerPixelSprite}bpp (${report.hardwarePaletteSwatches.size} Active Shades)",
                "Master Palette" to "${spec.masterPaletteSize} Total System Colors",
                "Pixel Aspect" to if (spec.paletteType == PaletteConstraintType.C64_16_MULTICOLOR_WIDE || spec.paletteType == PaletteConstraintType.ATARI_2600_NTSC_SCANLINE) "2:1 Wide Multicolor" else "1:1 Square / 4:3 CRT",
                "Hardware Verdict" to report.compatibility.label
            )
        )

        val paletteHexList = report.hardwarePaletteSwatches.mapIndexed { idx, c ->
            val r5 = (Color.red(c) ushr 3) and 0x1F
            val g5 = (Color.green(c) ushr 3) and 0x1F
            val b5 = (Color.blue(c) ushr 3) and 0x1F
            val bgr555 = (b5 shl 10) or (g5 shl 5) or r5
            String.format(
                "PAL_%02d: #0x%04X (%s)",
                idx + 1,
                bgr555,
                String.format("#%02X%02X%02X", Color.red(c), Color.green(c), Color.blue(c))
            )
        }

        val hexDump = buildString {
            appendLine("// ${spec.name.uppercase()} — ADJUSTED 2D SPRITE PPU/VDP DUMP")
            appendLine("// Sprite: ${sheet.name} | PPU/VDP: ${spec.gpuOrPpuSummary}")
            appendLine("SPRITE_CHR_HEADER:")
            appendLine("  .byte ${report.chrTilesWide}, ${report.chrTilesHigh}          ; TILES_W x TILES_H (${report.totalChrTiles} 8x8 CHR blocks)")
            appendLine("  .word 0x${report.chrVramBytes.toString(16).uppercase().padStart(4, '0')}      ; FRAME_VRAM_BYTES (${report.chrVramBytes} B @ ${spec.bitsPerPixelSprite}bpp)")
            appendLine("  .word 0x${totalSheetRamAdjusted.toString(16).uppercase().padStart(4, '0')}      ; FULL_SHEET_BYTES ($totalSheetRamAdjusted B for $frameCount frames)")
            appendLine("HARDWARE_PALETTE_REGISTERS (Index 00 = Transparent):")
            paletteHexList.take(8).forEach { line ->
                appendLine("  $line")
            }
            if (paletteHexList.size > 8) {
                appendLine("  ... ; +${paletteHexList.size - 8} additional quantized sub-palette entries")
            }
            appendLine("OAM_ATTRIBUTE_ENTRY_0:")
            appendLine("  .byte Y=0x60, TILE=0x00, ATTR=0x${spec.bitsPerPixelSprite.toString(16).padStart(2, '0')}, X=0x78")
        }

        return FullAssetHardwareDossier(
            systemName = spec.name,
            assetName = sheet.name,
            assetTypeLabel = "2D Sprite Sheet (${sheet.frames.size} Frames, ${report.chrTilesWide}×${report.chrTilesHigh} Tiles)",
            executiveHeadline = "How '${sheet.name}' is quantized, packed into ${spec.bitsPerPixelSprite}bpp tiles, and drawn by ${spec.shortName}.",
            sections = listOf(sec1, sec2, sec3, sec4),
            hardwareRegisterHexDump = hexDump
        )
    }
}
