package com.example.model.masterpack

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 3-Map + Depth/Normal Decomposition and Single-Active Hardware Tier Recolor Engine.
 * Separates any sprite into:
 * 1. `color_id` (up to 4 dominant flat color-blocking regions)
 * 2. `ink_shadow` (crisp black ink outlines + deep ink-pool shadow mask)
 * 3. `mat_emissive` (R = Tokusatsu clearcoat gloss, G = metallic bands, B = vivid emissive Mana Circuits)
 * 4. `depth_norm` (interior distance height + finite-difference RGB tangent normal map)
 */
object ThreeMapRecolorEngine {

    private const val TAG = "ThreeMapRecolorEngine"
    private const val ALPHA_THRESHOLD = 24
    private const val MAX_REGIONS = 4

    // Normalized 4x4 Bayer ordered dithering matrix (values 0..15 / 16)
    private val BAYER_4X4 = arrayOf(
        floatArrayOf(0f / 16f, 8f / 16f, 2f / 16f, 10f / 16f),
        floatArrayOf(12f / 16f, 4f / 16f, 14f / 16f, 6f / 16f),
        floatArrayOf(3f / 16f, 11f / 16f, 1f / 16f, 9f / 16f),
        floatArrayOf(15f / 16f, 7f / 16f, 13f / 16f, 5f / 16f)
    )

    /**
     * Decomposes [source] into a 4-layer [ThreeMapLayerSet] while preserving transparency
     * (`alpha < 24` remains transparent across all four maps).
     */
    fun decomposeSpriteToThreeMaps(source: Bitmap): ThreeMapLayerSet {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val srcPixels = IntArray(w * h)
        source.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val isOpaque = BooleanArray(w * h)
        val isEdge = BooleanArray(w * h)
        val lum = FloatArray(w * h)
        val sat = FloatArray(w * h)

        for (i in srcPixels.indices) {
            val px = srcPixels[i]
            val a = Color.alpha(px)
            if (a < ALPHA_THRESHOLD) continue
            isOpaque[i] = true
            val r = Color.red(px) / 255f
            val g = Color.green(px) / 255f
            val b = Color.blue(px) / 255f
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
            val mx = max(r, max(g, b))
            val mn = min(r, min(g, b))
            sat[i] = if (mx > 0.001f) (mx - mn) / mx else 0f
        }

        // Detect 1px silhouette contour pixels
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (!isOpaque[idx]) continue
                val touchesTransparent = x == 0 || y == 0 || x == w - 1 || y == h - 1 ||
                    !isOpaque[idx - 1] || !isOpaque[idx + 1] || !isOpaque[idx - w] || !isOpaque[idx + w]
                if (touchesTransparent) {
                    isEdge[idx] = true
                }
            }
        }

        // Extract up to 4 dominant non-ink chromatic region colors
        val regionColors = extractDominantRegionColors(srcPixels, isOpaque, lum)

        val colorIdPx = IntArray(w * h)
        val inkShadowPx = IntArray(w * h)
        val matEmissivePx = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (!isOpaque[idx]) {
                    colorIdPx[idx] = Color.TRANSPARENT
                    inkShadowPx[idx] = Color.TRANSPARENT
                    matEmissivePx[idx] = Color.TRANSPARENT
                    continue
                }
                val px = srcPixels[idx]
                val l = lum[idx]
                val s = sat[idx]

                // 1. Flat Color-ID region assignment
                val nearestRegionIdx = findNearestRegionIndex(px, regionColors)
                val baseRegionColor = regionColors[nearestRegionIdx]
                colorIdPx[idx] = Color.argb(
                    255,
                    Color.red(baseRegionColor),
                    Color.green(baseRegionColor),
                    Color.blue(baseRegionColor)
                )

                // 2. Ink & Shadow map:
                // Alpha encodes strength: 255 = pure ink line, 150 = deep ink-pool shadow, 0 = lit midtone/highlight
                val isDarkInk = l < 0.20f || (isEdge[idx] && l < 0.34f)
                val isDeepShadow = !isDarkInk && l in 0.20f..0.38f
                inkShadowPx[idx] = when {
                    isDarkInk -> Color.argb(255, 10, 12, 18)
                    isDeepShadow -> Color.argb(150, 18, 22, 34)
                    else -> Color.TRANSPARENT
                }

                // 3. Material & Emissive map (R = clearcoat gloss, G = metallic bands, B = vivid emissive Mana Circuits)
                val clearcoatR = if (l >= 0.72f) {
                    (((l - 0.72f) / 0.28f).coerceIn(0f, 1f) * 255f).roundToInt()
                } else {
                    0
                }
                val metallicG = if (l in 0.42f..0.82f && s <= 0.32f) {
                    (((0.32f - s) / 0.32f) * 220f).roundToInt().coerceIn(0, 255)
                } else {
                    0
                }
                val emissiveB = if (s >= 0.62f && l >= 0.42f) {
                    ((((s - 0.62f) / 0.38f) * 0.6f + ((l - 0.42f) / 0.58f) * 0.4f).coerceIn(0f, 1f) * 255f).roundToInt()
                } else {
                    0
                }
                matEmissivePx[idx] = Color.argb(255, clearcoatR, metallicG, emissiveB)
            }
        }

        // 4. Depth & Tangent Normal Map (interior distance transform -> convex height -> finite-difference RGB normal)
        val depthNormPx = computeDepthNormalPixels(w, h, isOpaque)

        return ThreeMapLayerSet(
            colorIdMap = createArgbBitmap(w, h, colorIdPx),
            inkShadowMap = createArgbBitmap(w, h, inkShadowPx),
            matEmissiveMap = createArgbBitmap(w, h, matEmissivePx),
            depthNormMap = createArgbBitmap(w, h, depthNormPx),
            regionBaseColors = regionColors
        )
    }

    /**
     * Composites a recolored sprite from [layers], applying up to 4 region swaps from
     * [selectedWheelHexes] and a single-active [tier] visual finish.
     *
     * CRITICAL: When [selectedWheelHexes] is empty (0 colors selected), original `colorIdMap`
     * region colors are preserved untouched while still applying the selected [tier] finish.
     */
    fun compositeRecoloredSprite(
        layers: ThreeMapLayerSet,
        selectedWheelHexes: List<String>,
        tier: HardwareVisualOutputTier
    ): Bitmap {
        val w = layers.colorIdMap.width.coerceAtLeast(1)
        val h = layers.colorIdMap.height.coerceAtLeast(1)
        val total = w * h

        val colorIdPx = IntArray(total)
        val inkShadowPx = IntArray(total)
        val matEmissivePx = IntArray(total)
        val depthNormPx = IntArray(total)

        layers.colorIdMap.getPixels(colorIdPx, 0, w, 0, 0, w, h)
        layers.inkShadowMap.getPixels(inkShadowPx, 0, w, 0, 0, w, h)
        layers.matEmissiveMap.getPixels(matEmissivePx, 0, w, 0, 0, w, h)
        layers.depthNormMap.getPixels(depthNormPx, 0, w, 0, 0, w, h)

        val parsedOverrides = selectedWheelHexes.take(MAX_REGIONS).mapNotNull { hex ->
            try {
                Color.parseColor(hex.trim())
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Ignoring invalid wheel hex '$hex': ${e.message}")
                null
            }
        }

        val regions = layers.regionBaseColors.ifEmpty { listOf(Color.rgb(0x00, 0xE5, 0xFF)) }
        val outPx = IntArray(total)
        val opaqueMask = BooleanArray(total)

        // Light direction for MODERN_25D_3D directional shading (upper-left key light)
        val lightX = -0.45f
        val lightY = -0.55f
        val lightZ = 0.70f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val cid = colorIdPx[idx]
                val a = Color.alpha(cid)
                if (a < ALPHA_THRESHOLD) {
                    outPx[idx] = Color.TRANSPARENT
                    continue
                }
                opaqueMask[idx] = true

                val inkPx = inkShadowPx[idx]
                val inkAlpha = Color.alpha(inkPx)
                val isInkLine = inkAlpha >= 220
                val isDeepShadow = inkAlpha in 80..219

                val matPx = matEmissivePx[idx]
                val clearcoatR = Color.red(matPx)
                val metallicG = Color.green(matPx)
                val emissiveB = Color.blue(matPx)

                // Preserve pure black ink lines across all recolor permutations
                if (isInkLine) {
                    outPx[idx] = when (tier) {
                        HardwareVisualOutputTier.PIXEL_16BIT -> quantizeBgr555(Color.rgb(16, 16, 24))
                        else -> Color.rgb(12, 14, 22)
                    }
                    continue
                }

                // Region color lookup: untouched when parsedOverrides is empty!
                val regionIndex = findNearestRegionIndex(cid, regions)
                val baseColor = if (regionIndex < parsedOverrides.size) {
                    parsedOverrides[regionIndex]
                } else {
                    cid
                }

                var r = Color.red(baseColor).toFloat()
                var g = Color.green(baseColor).toFloat()
                var b = Color.blue(baseColor).toFloat()

                // Apply subtle metallic sheen from G channel
                if (metallicG > 90) {
                    val metalBoost = ((metallicG - 90) / 165f) * 28f
                    r = (r + metalBoost).coerceIn(0f, 255f)
                    g = (g + metalBoost).coerceIn(0f, 255f)
                    b = (b + metalBoost * 1.1f).coerceIn(0f, 255f)
                }

                // Single-active hardware visual finish
                when (tier) {
                    HardwareVisualOutputTier.PIXEL_16BIT -> {
                        if (isDeepShadow) {
                            r *= 0.62f
                            g *= 0.62f
                            b *= 0.68f
                        } else if (clearcoatR > 120) {
                            r = (r + 36f).coerceAtMost(255f)
                            g = (g + 36f).coerceAtMost(255f)
                            b = (b + 36f).coerceAtMost(255f)
                        }
                        if (emissiveB > 120) {
                            val boost = (emissiveB - 120) / 135f * 0.35f
                            r = (r * (1f + boost)).coerceAtMost(255f)
                            g = (g * (1f + boost)).coerceAtMost(255f)
                            b = (b * (1f + boost)).coerceAtMost(255f)
                        }
                        outPx[idx] = quantizeBgr555(
                            Color.rgb(r.roundToInt().coerceIn(0, 255), g.roundToInt().coerceIn(0, 255), b.roundToInt().coerceIn(0, 255))
                        )
                    }

                    HardwareVisualOutputTier.ARCADE_32BIT_60FPS -> {
                        // Hard 2-step cel quantization + ordered 4x4 Bayer dithering strictly inside dark shadows
                        if (isDeepShadow) {
                            val bayer = BAYER_4X4[y and 3][x and 3]
                            val shadeMul = if (bayer < 0.45f) 0.42f else 0.64f
                            r *= shadeMul
                            g *= shadeMul
                            b *= (shadeMul + 0.05f).coerceAtMost(0.75f)
                        } else {
                            // Punchy 2-step cel ramp
                            r = quantizeStep(r, 42f)
                            g = quantizeStep(g, 42f)
                            b = quantizeStep(b, 42f)
                            if (clearcoatR > 130) {
                                r = (r + 48f).coerceAtMost(255f)
                                g = (g + 48f).coerceAtMost(255f)
                                b = (b + 52f).coerceAtMost(255f)
                            }
                        }
                        if (emissiveB > 110) {
                            val glow = (emissiveB / 255f) * 44f
                            r = (r + glow * 0.5f).coerceAtMost(255f)
                            g = (g + glow * 0.9f).coerceAtMost(255f)
                            b = (b + glow).coerceAtMost(255f)
                        }
                        outPx[idx] = Color.rgb(
                            r.roundToInt().coerceIn(0, 255),
                            g.roundToInt().coerceIn(0, 255),
                            b.roundToInt().coerceIn(0, 255)
                        )
                    }

                    HardwareVisualOutputTier.TRUE_FUSION_HD -> {
                        // Crisp ink-pool shadows + angled comic halftone dot pattern strictly in dark shadows + white clearcoat glints
                        if (isDeepShadow) {
                            val diagDot = ((x + y) % 4 == 0) && ((x - y + 4096) % 4 == 0)
                            val shadeMul = if (diagDot) 0.22f else 0.54f
                            r *= shadeMul
                            g *= shadeMul
                            b *= (shadeMul + 0.06f).coerceAtMost(0.70f)
                        } else if (clearcoatR > 150) {
                            // Crisp Tokusatsu white clearcoat glint
                            val mix = ((clearcoatR - 150) / 105f).coerceIn(0f, 1f) * 0.72f
                            r = r + (255f - r) * mix
                            g = g + (255f - g) * mix
                            b = b + (255f - b) * mix
                        }
                        if (emissiveB > 100) {
                            val boost = (emissiveB / 255f) * 52f
                            r = (r + boost * 0.45f).coerceAtMost(255f)
                            g = (g + boost * 0.85f).coerceAtMost(255f)
                            b = (b + boost).coerceAtMost(255f)
                        }
                        outPx[idx] = Color.rgb(
                            r.roundToInt().coerceIn(0, 255),
                            g.roundToInt().coerceIn(0, 255),
                            b.roundToInt().coerceIn(0, 255)
                        )
                    }

                    HardwareVisualOutputTier.MODERN_25D_3D -> {
                        // Directional lighting from depth_norm RGB tangent normal + specular/emissive boost
                        val dn = depthNormPx[idx]
                        val nx = (Color.red(dn) - 128f) / 127f
                        val ny = (Color.green(dn) - 128f) / 127f
                        val nz = ((Color.blue(dn) - 128f) / 127f).coerceAtLeast(0.1f)
                        val ndotl = (nx * lightX + ny * lightY + nz * lightZ).coerceIn(0f, 1f)
                        val shadowBias = if (isDeepShadow) 0.62f else 1.0f
                        val lighting = (0.34f + 0.86f * ndotl) * shadowBias
                        r *= lighting
                        g *= lighting
                        b *= lighting

                        if (clearcoatR > 100) {
                            val spec = (ndotl * ndotl) * (clearcoatR / 255f) * 115f
                            r += spec
                            g += spec
                            b += spec
                        }
                        if (emissiveB > 90) {
                            val em = (emissiveB / 255f) * 70f
                            r += em * 0.5f
                            g += em * 0.85f
                            b += em
                        }
                        outPx[idx] = Color.rgb(
                            r.roundToInt().coerceIn(0, 255),
                            g.roundToInt().coerceIn(0, 255),
                            b.roundToInt().coerceIn(0, 255)
                        )
                    }
                }
            }
        }

        // For 16-Bit Pixel tier, enforce a crisp 1px dark silhouette outline around opaque pixels
        if (tier == HardwareVisualOutputTier.PIXEL_16BIT) {
            val outlineColor = quantizeBgr555(Color.rgb(16, 16, 24))
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val idx = y * w + x
                    if (!opaqueMask[idx]) continue
                    val onBorder = x == 0 || y == 0 || x == w - 1 || y == h - 1 ||
                        !opaqueMask[idx - 1] || !opaqueMask[idx + 1] || !opaqueMask[idx - w] || !opaqueMask[idx + w]
                    if (onBorder) {
                        outPx[idx] = outlineColor
                    }
                }
            }
        }

        return createArgbBitmap(w, h, outPx)
    }

    // -------------------------------------------------------------------------
    // Internal Region Quantization, Distance Height & Normal Helpers
    // -------------------------------------------------------------------------

    private fun extractDominantRegionColors(
        pixels: IntArray,
        isOpaque: BooleanArray,
        lum: FloatArray
    ): List<Int> {
        val bucketCounts = HashMap<Int, Int>()
        val bucketSumR = HashMap<Int, Long>()
        val bucketSumG = HashMap<Int, Long>()
        val bucketSumB = HashMap<Int, Long>()

        fun accumulate(includeDark: Boolean) {
            for (i in pixels.indices) {
                if (!isOpaque[i]) continue
                if (!includeDark && lum[i] < 0.20f) continue
                val px = pixels[i]
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)
                val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                bucketCounts[key] = (bucketCounts[key] ?: 0) + 1
                bucketSumR[key] = (bucketSumR[key] ?: 0L) + r
                bucketSumG[key] = (bucketSumG[key] ?: 0L) + g
                bucketSumB[key] = (bucketSumB[key] ?: 0L) + b
            }
        }

        accumulate(includeDark = false)
        if (bucketCounts.isEmpty()) {
            accumulate(includeDark = true)
        }
        if (bucketCounts.isEmpty()) {
            return listOf(Color.rgb(0x00, 0xE5, 0xFF))
        }

        val sortedCandidates = bucketCounts.entries
            .sortedByDescending { it.value }
            .map { (key, count) ->
                val avgR = (bucketSumR[key]!! / count).toInt().coerceIn(0, 255)
                val avgG = (bucketSumG[key]!! / count).toInt().coerceIn(0, 255)
                val avgB = (bucketSumB[key]!! / count).toInt().coerceIn(0, 255)
                Color.rgb(avgR, avgG, avgB)
            }

        val chosen = mutableListOf<Int>()
        for (candidate in sortedCandidates) {
            if (chosen.size >= MAX_REGIONS) break
            val distinct = chosen.all { colorDistanceSq(it, candidate) >= 2400 }
            if (distinct) {
                chosen.add(candidate)
            }
        }
        for (candidate in sortedCandidates) {
            if (chosen.size >= MAX_REGIONS) break
            if (candidate !in chosen) {
                chosen.add(candidate)
            }
        }
        return chosen
    }

    private fun findNearestRegionIndex(pixelColor: Int, regions: List<Int>): Int {
        var bestIdx = 0
        var bestDist = Int.MAX_VALUE
        for (i in regions.indices) {
            val d = colorDistanceSq(pixelColor, regions[i])
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun colorDistanceSq(c1: Int, c2: Int): Int {
        val dr = Color.red(c1) - Color.red(c2)
        val dg = Color.green(c1) - Color.green(c2)
        val db = Color.blue(c1) - Color.blue(c2)
        return dr * dr + dg * dg + db * db
    }

    private fun computeDepthNormalPixels(w: Int, h: Int, isOpaque: BooleanArray): IntArray {
        val total = w * h
        val dist = IntArray(total) { Int.MAX_VALUE }
        val queue = IntArray(total)
        var head = 0
        var tail = 0

        // Seed all opaque pixels adjacent to transparent edge or bitmap boundary at distance = 1
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (!isOpaque[idx]) {
                    dist[idx] = 0
                    continue
                }
                val isBorder = x == 0 || y == 0 || x == w - 1 || y == h - 1 ||
                    !isOpaque[idx - 1] || !isOpaque[idx + 1] || !isOpaque[idx - w] || !isOpaque[idx + w]
                if (isBorder) {
                    dist[idx] = 1
                    queue[tail++] = idx
                }
            }
        }

        // 4-connected BFS interior distance transform
        var maxDist = 1
        while (head < tail) {
            val idx = queue[head++]
            val dNext = dist[idx] + 1
            val x = idx % w
            val y = idx / w
            fun visit(nIdx: Int) {
                if (isOpaque[nIdx] && dist[nIdx] > dNext) {
                    dist[nIdx] = dNext
                    if (dNext > maxDist) maxDist = dNext
                    queue[tail++] = nIdx
                }
            }
            if (x > 0) visit(idx - 1)
            if (x + 1 < w) visit(idx + 1)
            if (y > 0) visit(idx - w)
            if (y + 1 < h) visit(idx + w)
        }

        val heightMap = FloatArray(total)
        val invMax = 1f / maxDist.coerceAtLeast(1).toFloat()
        for (i in 0 until total) {
            if (isOpaque[i]) {
                val t = (dist[i] * invMax).coerceIn(0f, 1f)
                // Smooth convex dome profile sqrt(t)
                heightMap[i] = sqrt(t)
            }
        }

        val normPx = IntArray(total)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (!isOpaque[idx]) {
                    normPx[idx] = Color.TRANSPARENT
                    continue
                }
                val hL = if (x > 0) heightMap[idx - 1] else 0f
                val hR = if (x + 1 < w) heightMap[idx + 1] else 0f
                val hU = if (y > 0) heightMap[idx - w] else 0f
                val hD = if (y + 1 < h) heightMap[idx + w] else 0f

                val dx = (hL - hR) * 1.8f
                val dy = (hU - hD) * 1.8f
                val dz = 1.0f
                val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001f)
                val nx = dx / len
                val ny = dy / len
                val nz = dz / len

                val rByte = (128f + nx * 127f).roundToInt().coerceIn(0, 255)
                val gByte = (128f + ny * 127f).roundToInt().coerceIn(0, 255)
                val bByte = (128f + nz * 127f).roundToInt().coerceIn(0, 255)
                normPx[idx] = Color.argb(255, rByte, gByte, bByte)
            }
        }
        return normPx
    }

    private fun quantizeBgr555(color: Int): Int {
        val r = (Color.red(color) shr 3) shl 3
        val g = (Color.green(color) shr 3) shl 3
        val b = (Color.blue(color) shr 3) shl 3
        return Color.argb(255, r, g, b)
    }

    private fun quantizeStep(v: Float, step: Float): Float {
        return (kotlin.math.round(v / step) * step).coerceIn(0f, 255f)
    }

    private fun createArgbBitmap(w: Int, h: Int, pixels: IntArray): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setHasAlpha(true)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
