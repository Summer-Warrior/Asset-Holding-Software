package com.example.model.hardware

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.Vector3
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CompatibilityLevel(val label: String, val colorHex: Long) {
    NATIVE_FIT("NATIVE FIT", 0xFF10B981),
    CO_PROCESSOR_OR_LOD("NEEDS LOD / CO-CHIP", 0xFFF59E0B),
    OVER_BUDGET("EXCEEDS SILICON LIMIT", 0xFFEF4444)
}

data class SpriteHardwareReport(
    val simulatedBitmap: Bitmap,
    val hardwarePaletteSwatches: List<Int>,
    val chrTilesWide: Int,
    val chrTilesHigh: Int,
    val totalChrTiles: Int,
    val chrVramBytes: Int,
    val vramUsagePercent: Float,
    val scanlineSpritesUsed: Int,
    val scanlineLimitMax: Int,
    val causesScanlineFlicker: Boolean,
    val compatibility: CompatibilityLevel,
    val technicalVerdict: String
)

data class MeshHardwareReport(
    val vertexCount: Int,
    val triangleCount: Int,
    val recommendedTriBudget: Int,
    val maxSceneTriBudget: Int,
    val assetBudgetPercent: Float,
    val vertexBufferBytes: Int,
    val faceTableBytes: Int,
    val estimatedTextureBytes: Int,
    val totalAssetRamBytes: Int,
    val fitsInTextureCache: Boolean,
    val textureCacheNote: String,
    val compatibility: CompatibilityLevel,
    val siliconBottlenecks: List<String>
)

object HardwareAssetSimulator {

    // Authentic Commodore 64 VIC-II 16-Color Palette (Pepto)
    private val C64_PALETTE = intArrayOf(
        0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF68372B.toInt(), 0xFF70A4B2.toInt(),
        0xFF6F3D86.toInt(), 0xFF588D43.toInt(), 0xFF352879.toInt(), 0xFFB8C76F.toInt(),
        0xFF6F4F25.toInt(), 0xFF433900.toInt(), 0xFF9A6759.toInt(), 0xFF444444.toInt(),
        0xFF6C6C6C.toInt(), 0xFF9AD284.toInt(), 0xFF6C5EB5.toInt(), 0xFF959595.toInt()
    )

    // Authentic NES Ricoh 2C02 Representative 54-Color Palette
    private val NES_2C02_PALETTE = intArrayOf(
        0xFF545454.toInt(), 0xFF001E74.toInt(), 0xFF081090.toInt(), 0xFF300088.toInt(),
        0xFF440064.toInt(), 0xFF5C0030.toInt(), 0xFF540400.toInt(), 0xFF3C1800.toInt(),
        0xFF202A00.toInt(), 0xFF083A00.toInt(), 0xFF004000.toInt(), 0xFF003C00.toInt(),
        0xFF00323C.toInt(), 0xFF000000.toInt(), 0xFF989698.toInt(), 0xFF084CC4.toInt(),
        0xFF3032EC.toInt(), 0xFF5C1EE4.toInt(), 0xFF8814B0.toInt(), 0xFFA01464.toInt(),
        0xFF982220.toInt(), 0xFF783C00.toInt(), 0xFF545A00.toInt(), 0xFF287200.toInt(),
        0xFF087C00.toInt(), 0xFF007628.toInt(), 0xFF006678.toInt(), 0xFFECEEEC.toInt(),
        0xFF4C9AEC.toInt(), 0xFF787CEC.toInt(), 0xFFB062EC.toInt(), 0xFFE454EC.toInt(),
        0xFFEC58B4.toInt(), 0xFFEC6A64.toInt(), 0xFFD48820.toInt(), 0xFFA0AA00.toInt(),
        0xFF74C400.toInt(), 0xFF4CD020.toInt(), 0xFF38CC6C.toInt(), 0xFF38B4CC.toInt(),
        0xFF3C3C3C.toInt(), 0xFFA8CCEC.toInt(), 0xFFBCBCEC.toInt(), 0xFFD4B2EC.toInt(),
        0xFFECAEEC.toInt(), 0xFFECAED4.toInt(), 0xFFECB4B0.toInt(), 0xFFE4C490.toInt(),
        0xFFCCD278.toInt(), 0xFFB4DE78.toInt(), 0xFFA8E290.toInt(), 0xFF98E2B4.toInt(),
        0xFFA0D6E4.toInt(), 0xFFA0A2A0.toInt()
    )

    // Game Boy DMG-01 4 Olive-Green Shades
    private val DMG_PALETTE = intArrayOf(
        0xFF0F380F.toInt(),
        0xFF306230.toInt(),
        0xFF8BAC0F.toInt(),
        0xFF9BBC0F.toInt()
    )

    // Virtual Boy VIP 4 Red LED Shades
    private val VB_RED_PALETTE = intArrayOf(
        0xFF080000.toInt(),
        0xFF550000.toInt(),
        0xFFAA0000.toInt(),
        0xFFFF1A1A.toInt()
    )

    fun quantizeColorForHardware(argb: Int, type: PaletteConstraintType): Int {
        val a = (argb ushr 24) and 0xFF
        if (a < 35) return Color.TRANSPARENT

        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF

        return when (type) {
            PaletteConstraintType.DMG_4_OLIVE -> {
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                val idx = (lum / 64).coerceIn(0, 3)
                DMG_PALETTE[idx]
            }
            PaletteConstraintType.VIRTUAL_BOY_4_RED -> {
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                val idx = (lum / 64).coerceIn(0, 3)
                VB_RED_PALETTE[idx]
            }
            PaletteConstraintType.C64_16_MULTICOLOR_WIDE -> {
                findNearestInPalette(r, g, b, C64_PALETTE)
            }
            PaletteConstraintType.NES_2C02_2BPP_54 -> {
                findNearestInPalette(r, g, b, NES_2C02_PALETTE)
            }
            PaletteConstraintType.ATARI_2600_NTSC_SCANLINE -> {
                // 7-bit NTSC (128 colors: 16 hues x 8 luminances)
                val qr = (r / 32) * 36
                val qg = (g / 32) * 36
                val qb = (b / 32) * 36
                Color.rgb(qr.coerceIn(0, 255), qg.coerceIn(0, 255), qb.coerceIn(0, 255))
            }
            PaletteConstraintType.MASTER_SYSTEM_6bit -> {
                // 2 bits per channel (64 colors: 0, 85, 170, 255)
                val qr = (r / 64) * 85
                val qg = (g / 64) * 85
                val qb = (b / 64) * 85
                Color.rgb(qr, qg, qb)
            }
            PaletteConstraintType.GENESIS_9BIT_4BPP -> {
                // 3 bits per channel (512 colors: 0, 36, 73, 109, 146, 182, 219, 255)
                val qr = ((r * 7 + 127) / 255) * 36
                val qg = ((g * 7 + 127) / 255) * 36
                val qb = ((b * 7 + 127) / 255) * 36
                Color.rgb(qr.coerceIn(0, 255), qg.coerceIn(0, 255), qb.coerceIn(0, 255))
            }
            PaletteConstraintType.SNES_15BIT_4BPP,
            PaletteConstraintType.SNES_15BIT_8BPP,
            PaletteConstraintType.RGB555_15BIT -> {
                // 5 bits per channel (32,768 colors: steps of 8)
                val qr = (r ushr 3) shl 3
                val qg = (g ushr 3) shl 3
                val qb = (b ushr 3) shl 3
                Color.rgb(qr, qg, qb)
            }
            PaletteConstraintType.TRUECOLOR_24BIT -> {
                Color.rgb(r, g, b)
            }
        }
    }

    private fun findNearestInPalette(r: Int, g: Int, b: Int, palette: IntArray): Int {
        var bestColor = palette[0]
        var bestDist = Int.MAX_VALUE
        for (c in palette) {
            val pr = (c ushr 16) and 0xFF
            val pg = (c ushr 8) and 0xFF
            val pb = c and 0xFF
            val dr = r - pr
            val dg = g - pg
            val db = b - pb
            val dist = dr * dr * 3 + dg * dg * 4 + db * db * 2
            if (dist < bestDist) {
                bestDist = dist
                bestColor = c
            }
        }
        return bestColor
    }

    fun simulateSpriteOnHardware(
        sourceBitmap: Bitmap,
        spec: ConsoleHardwareSpec,
        showTileGrid: Boolean = true
    ): SpriteHardwareReport {
        val targetH = spec.targetSpriteResolution.coerceIn(16, 128)
        // Account for wide-pixel hardware like C64 Multicolor (12x21 -> 2:1 wide) or Atari 2600 (8x16 -> 2:1 wide)
        val isWidePixelMode = spec.paletteType == PaletteConstraintType.C64_16_MULTICOLOR_WIDE ||
                spec.paletteType == PaletteConstraintType.ATARI_2600_NTSC_SCANLINE

        val logicalW = if (isWidePixelMode) (targetH / 2).coerceAtLeast(8) else targetH
        val logicalH = targetH

        val downsampled = Bitmap.createScaledBitmap(sourceBitmap, logicalW, logicalH, false)
        val processed = Bitmap.createBitmap(logicalW, logicalH, Bitmap.Config.ARGB_8888)

        // First pass: map all pixels to the console's master color space and collect frequency
        val colorFreq = mutableMapOf<Int, Int>()
        for (y in 0 until logicalH) {
            for (x in 0 until logicalW) {
                val p = downsampled.getPixel(x, y)
                val q = quantizeColorForHardware(p, spec.paletteType)
                processed.setPixel(x, y, q)
                if (q != Color.TRANSPARENT) {
                    colorFreq[q] = (colorFreq[q] ?: 0) + 1
                }
            }
        }

        // Enforce sub-palette color count limit (e.g. 3 visible colors on NES/GB, 15 on Genesis/SNES)
        val maxVisibleColors = (spec.maxSpriteColorsPerPalette - 1).coerceAtLeast(1)
        val allowedPalette = colorFreq.entries
            .sortedByDescending { it.value }
            .take(maxVisibleColors.coerceAtMost(32))
            .map { it.key }
            .toIntArray()

        if (allowedPalette.isNotEmpty() && colorFreq.size > maxVisibleColors) {
            for (y in 0 until logicalH) {
                for (x in 0 until logicalW) {
                    val p = processed.getPixel(x, y)
                    if (p != Color.TRANSPARENT) {
                        val r = (p ushr 16) and 0xFF
                        val g = (p ushr 8) and 0xFF
                        val b = p and 0xFF
                        processed.setPixel(x, y, findNearestInPalette(r, g, b, allowedPalette))
                    }
                }
            }
        }

        // Special Atari 2600 rule: 1 color + transparent per individual scanline!
        if (spec.paletteType == PaletteConstraintType.ATARI_2600_NTSC_SCANLINE) {
            for (y in 0 until logicalH) {
                // Find dominant color on this scanline
                var rowColor = Color.TRANSPARENT
                var maxCount = 0
                val rowCounts = mutableMapOf<Int, Int>()
                for (x in 0 until logicalW) {
                    val c = processed.getPixel(x, y)
                    if (c != Color.TRANSPARENT) {
                        val count = (rowCounts[c] ?: 0) + 1
                        rowCounts[c] = count
                        if (count > maxCount) {
                            maxCount = count
                            rowColor = c
                        }
                    }
                }
                for (x in 0 until logicalW) {
                    if (processed.getPixel(x, y) != Color.TRANSPARENT) {
                        processed.setPixel(x, y, rowColor)
                    }
                }
            }
        }

        // Render upscaled preview (256x256) with hardware CRT/LCD/Tile characteristics
        val previewSize = 256
        val displayBmp = Bitmap.createBitmap(previewSize, previewSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(displayBmp)
        val paint = Paint()

        // Background tint for Game Boy DMG or Virtual Boy
        when (spec.paletteType) {
            PaletteConstraintType.DMG_4_OLIVE -> {
                canvas.drawColor(Color.rgb(155, 188, 15))
            }
            PaletteConstraintType.VIRTUAL_BOY_4_RED -> {
                canvas.drawColor(Color.rgb(10, 0, 0))
            }
            else -> {
                canvas.drawColor(Color.rgb(9, 13, 22))
            }
        }

        val cellW = previewSize.toFloat() / logicalW
        val cellH = previewSize.toFloat() / logicalH

        for (y in 0 until logicalH) {
            for (x in 0 until logicalW) {
                val c = processed.getPixel(x, y)
                if (c != Color.TRANSPARENT) {
                    paint.color = c
                    val left = x * cellW
                    val top = y * cellH
                    val right = left + cellW
                    val bottom = if (spec.paletteType == PaletteConstraintType.VIRTUAL_BOY_4_RED ||
                        spec.paletteType == PaletteConstraintType.ATARI_2600_NTSC_SCANLINE
                    ) {
                        // Subtle scanline gap
                        top + cellH * 0.82f
                    } else {
                        top + cellH
                    }
                    canvas.drawRect(left, top, right, bottom, paint)
                }
            }
        }

        // Draw 8x8 CHR Tile Grid Overlay if requested
        if (showTileGrid) {
            paint.color = Color.argb(90, 0, 240, 255)
            paint.strokeWidth = 1.5f
            val tileStepX = if (isWidePixelMode) 4 else 8
            val tileStepY = 8

            var gx = tileStepX
            while (gx < logicalW) {
                val px = gx * cellW
                canvas.drawLine(px, 0f, px, previewSize.toFloat(), paint)
                gx += tileStepX
            }

            var gy = tileStepY
            while (gy < logicalH) {
                val py = gy * cellH
                canvas.drawLine(0f, py, previewSize.toFloat(), py, paint)
                gy += tileStepY
            }
        }

        // Calculate CHR & OAM Telemetry
        val tilesW = ceil(targetH / 8.0).toInt().coerceAtLeast(1)
        val tilesH = ceil(targetH / 8.0).toInt().coerceAtLeast(1)
        val totalTiles = tilesW * tilesH
        val bytesPerTile = (64 * spec.bitsPerPixelSprite) / 8
        val chrVramBytes = totalTiles * bytesPerTile
        val vramPct = ((chrVramBytes.toFloat() / spec.vramBytes.coerceAtLeast(1L)) * 100f).coerceIn(0.01f, 100f)

        val causesFlicker = tilesW > spec.maxSpritesPerScanline
        val compatibility = when {
            causesFlicker -> CompatibilityLevel.CO_PROCESSOR_OR_LOD
            chrVramBytes > spec.vramBytes -> CompatibilityLevel.OVER_BUDGET
            else -> CompatibilityLevel.NATIVE_FIT
        }

        val verdict = buildString {
            append("Quantized to ${spec.shortName} (${spec.bitsPerPixelSprite}bpp, max ${allowedPalette.size} visible colors). ")
            append("Requires $totalTiles 8×8 CHR tiles ($chrVramBytes Bytes VRAM). ")
            if (causesFlicker) {
                append("WARNING: $tilesW tiles wide exceeds ${spec.maxSpritesPerScanline} sprites/scanline limit (will trigger OAM flicker!).")
            } else {
                append("Uses $tilesW/${spec.maxSpritesPerScanline} scanline sprite slots (Flicker-free).")
            }
        }

        return SpriteHardwareReport(
            simulatedBitmap = displayBmp,
            hardwarePaletteSwatches = allowedPalette.toList(),
            chrTilesWide = tilesW,
            chrTilesHigh = tilesH,
            totalChrTiles = totalTiles,
            chrVramBytes = chrVramBytes,
            vramUsagePercent = vramPct,
            scanlineSpritesUsed = tilesW,
            scanlineLimitMax = spec.maxSpritesPerScanline,
            causesScanlineFlicker = causesFlicker,
            compatibility = compatibility,
            technicalVerdict = verdict
        )
    }

    fun analyzeMeshOnHardware(
        mesh: Mesh3D,
        spec: ConsoleHardwareSpec
    ): MeshHardwareReport {
        val vCount = mesh.vertexCount
        val fCount = mesh.faceCount

        // Vertex byte size on target silicon:
        // Fixed-point 16-bit systems (Super FX, PS1 GTE, Saturn, NDS): 3x 16-bit XYZ + 2x 16-bit UV + 4B color = 14–16 bytes/vert
        // Floating-point systems (N64, Dreamcast, PS2, GameCube, Modern): 24–32 bytes/vert
        val bytesPerVertex = if (spec.era == HardwareEra.ERA_8BIT || spec.era == HardwareEra.ERA_16BIT) 8
        else if (spec.era == HardwareEra.ERA_32_64BIT) 16
        else 32

        val bytesPerFace = if (spec.id == "ps1" || spec.id == "sega_saturn") 20 // Packet + OT pointer
        else if (spec.era == HardwareEra.ERA_8BIT || spec.era == HardwareEra.ERA_16BIT) 6
        else 12

        val vBytes = vCount * bytesPerVertex
        val fBytes = fCount * bytesPerFace

        // Texture footprint estimate based on bitsPerPixelSprite
        val texDim = if (spec.id == "n64") 32 else if (spec.era == HardwareEra.ERA_32_64BIT) 64 else 128
        val estTexBytes = if (!spec.hasHardware3D && spec.era == HardwareEra.ERA_8BIT) 0
        else (texDim * texDim * spec.bitsPerPixelSprite) / 8

        val totalBytes = vBytes + fBytes + estTexBytes
        val assetBudgetPct = (fCount.toFloat() / spec.recommendedModelTriBudget.coerceAtLeast(1)) * 100f

        val fitsCache = spec.textureCacheBytes == 0 || estTexBytes <= spec.textureCacheBytes
        val cacheNote = when {
            spec.id == "n64" -> "N64 RDP has a strict 4,096-byte (4 KB) on-chip TMEM cache. Single 32×32 16-bit tile = 2,048 B (FITS TMEM)."
            spec.id == "ps1" -> "PS1 CXD8514Q GPU has a 2 KB texture cache; 4bpp/8bpp CLUT pages fit cleanly inside the 1024×512 VRAM sheet."
            spec.id == "nintendo_ds" -> "NDS Polygon RAM has a hard silicon ceiling of 2,048 triangles / 6,144 vertices per frame (Current: $fCount tris)."
            spec.id == "ps2" -> "PS2 GS has 4 MB embedded eDRAM @ 48 GB/s; textures stream continuously from 32 MB Main RDRAM via GIF DMA."
            spec.id == "gamecube" -> "GameCube Flipper caches S3TC 4bpp compressed textures inside 1 MB on-die 1T-SRAM."
            else -> "Asset geometry + texture footprint: ${totalBytes / 1024f} KB."
        }

        val bottlenecks = mutableListOf<String>()
        if (!spec.hasHardware3D) {
            bottlenecks.add("No dedicated 3D silicon on base ${spec.shortName}: Must render as software wireframe, pre-rendered 2D sprite impostor, or use a cartridge co-processor.")
        }
        if (fCount > spec.recommendedModelTriBudget) {
            bottlenecks.add("Triangle count ($fCount) exceeds recommended single-asset budget (${spec.recommendedModelTriBudget} tris) for 60 FPS on ${spec.shortName}.")
        } else {
            bottlenecks.add("Triangle count ($fCount) is well within ${spec.shortName}'s per-asset budget (${spec.recommendedModelTriBudget} tris).")
        }

        if (!spec.hasZBuffer && spec.hasHardware3D) {
            bottlenecks.add("No Hardware Z-Buffer on ${spec.shortName}: Uses Painter's Algorithm / Ordering Table sorting (long intersecting polygons may sort-pop).")
        }
        if (spec.vertexSnapGrid > 0f && spec.hasHardware3D) {
            bottlenecks.add("Fixed-Point Integer Screen Coordinates: Vertices snap to integer pixels without sub-pixel precision, causing visible polygon jitter during rotation.")
        }

        val compatibility = when {
            fCount > spec.maxSceneTrisPerFrame -> CompatibilityLevel.OVER_BUDGET
            !spec.hasHardware3D || fCount > spec.recommendedModelTriBudget -> CompatibilityLevel.CO_PROCESSOR_OR_LOD
            else -> CompatibilityLevel.NATIVE_FIT
        }

        return MeshHardwareReport(
            vertexCount = vCount,
            triangleCount = fCount,
            recommendedTriBudget = spec.recommendedModelTriBudget,
            maxSceneTriBudget = spec.maxSceneTrisPerFrame,
            assetBudgetPercent = assetBudgetPct,
            vertexBufferBytes = vBytes,
            faceTableBytes = fBytes,
            estimatedTextureBytes = estTexBytes,
            totalAssetRamBytes = totalBytes,
            fitsInTextureCache = fitsCache,
            textureCacheNote = cacheNote,
            compatibility = compatibility,
            siliconBottlenecks = bottlenecks
        )
    }

    /**
     * Adapts a 3D Mesh to the target hardware's fixed-point vertex precision and color palette!
     */
    fun adaptMeshToHardware(mesh: Mesh3D, spec: ConsoleHardwareSpec): Mesh3D {
        // Quantize vertex coordinates if older hardware
        val precisionSteps = when (spec.era) {
            HardwareEra.ERA_8BIT -> 16f
            HardwareEra.ERA_16BIT -> 32f
            HardwareEra.ERA_32_64BIT -> 64f
            else -> 256f
        }

        val adaptedVertices = mesh.vertices.map { v ->
            Vector3(
                x = (v.x * precisionSteps).roundToInt() / precisionSteps,
                y = (v.y * precisionSteps).roundToInt() / precisionSteps,
                z = (v.z * precisionSteps).roundToInt() / precisionSteps
            )
        }

        // Quantize face colors to the console's hardware color space
        val adaptedFaces = mesh.faces.map { face ->
            val argb = (0xFF000000L or (face.color and 0xFFFFFFL)).toInt()
            val qColor = quantizeColorForHardware(argb, spec.paletteType)
            val newColorLong = 0xFF000000L or (qColor.toLong() and 0xFFFFFFL)
            face.copy(color = newColorLong)
        }

        val baseName = mesh.name.substringBefore(" [")
        return mesh.copy(
            name = "$baseName [${spec.shortName}]",
            vertices = adaptedVertices,
            faces = adaptedFaces
        )
    }
}
