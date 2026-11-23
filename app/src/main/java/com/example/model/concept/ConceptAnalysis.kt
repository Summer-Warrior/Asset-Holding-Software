package com.example.model.concept

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

data class ExtractedColor(
    val colorInt: Int,
    val hex: String,
    val percentage: Float
)

object ConceptAnalysis {

    fun extractPalette(bitmap: Bitmap, maxColors: Int = 8): List<ExtractedColor> {
        val small = Bitmap.createScaledBitmap(bitmap, 48, 48, false)
        val colorCounts = mutableMapOf<Int, Int>()
        val totalPixels = small.width * small.height
        var nonTransparent = 0

        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                val pixel = small.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 40) continue
                nonTransparent++

                // Quantize to 5 bits per channel (32 levels) to cluster similar shades
                val r = ((pixel ushr 16) and 0xFF) / 8 * 8
                val g = ((pixel ushr 8) and 0xFF) / 8 * 8
                val b = (pixel and 0xFF) / 8 * 8
                val clustered = Color.rgb(r, g, b)

                colorCounts[clustered] = (colorCounts[clustered] ?: 0) + 1
            }
        }

        if (nonTransparent == 0) return emptyList()

        return colorCounts.entries
            .sortedByDescending { it.value }
            .take(maxColors)
            .map { entry ->
                val c = entry.key
                val hex = String.format("#%02X%02X%02X", Color.red(c), Color.green(c), Color.blue(c))
                val pct = (entry.value.toFloat() / nonTransparent) * 100f
                ExtractedColor(c, hex, pct)
            }
    }

    /**
     * Converts concept art into a crisp pixel art sprite
     */
    fun convertConceptToSprite(
        conceptBitmap: Bitmap,
        resolution: Int = 32,
        addOutline: Boolean = true
    ): Bitmap {
        // Step 1: Scaled down with nearest-neighbor or bilinear
        val small = Bitmap.createScaledBitmap(conceptBitmap, resolution, resolution, true)
        val w = small.width
        val h = small.height

        // Step 2: Extract top palette for quantization
        val palette = extractPalette(small, 16).map { it.colorInt }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        fun findNearestColor(c: Int): Int {
            if (palette.isEmpty()) return c
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            var bestDist = Float.MAX_VALUE
            var bestColor = c
            for (pc in palette) {
                val pr = Color.red(pc)
                val pg = Color.green(pc)
                val pb = Color.blue(pc)
                val dist = (r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb)
                if (dist < bestDist) {
                    bestDist = dist.toFloat()
                    bestColor = pc
                }
            }
            return bestColor
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = small.getPixel(x, y)
                val a = (p ushr 24) and 0xFF
                if (a < 35) {
                    output.setPixel(x, y, Color.TRANSPARENT)
                } else {
                    val quantized = findNearestColor(p)
                    output.setPixel(x, y, quantized)
                }
            }
        }

        // Optional Step 3: Add dark pixel outline for game sprite feel
        if (addOutline) {
            val outlined = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val outlineColor = Color.rgb(15, 23, 42)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val p = output.getPixel(x, y)
                    if (Color.alpha(p) > 50) {
                        outlined.setPixel(x, y, p)
                    } else {
                        // Check neighbors
                        val hasSolidNeighbor = (x > 0 && Color.alpha(output.getPixel(x - 1, y)) > 50) ||
                                (x < w - 1 && Color.alpha(output.getPixel(x + 1, y)) > 50) ||
                                (y > 0 && Color.alpha(output.getPixel(x, y - 1)) > 50) ||
                                (y < h - 1 && Color.alpha(output.getPixel(x, y + 1)) > 50)
                        if (hasSolidNeighbor) {
                            outlined.setPixel(x, y, outlineColor)
                        }
                    }
                }
            }
            // Scale up 8x for clear pixel inspection
            return Bitmap.createScaledBitmap(outlined, w * 8, h * 8, false)
        }

        return Bitmap.createScaledBitmap(output, w * 8, h * 8, false)
    }
}
