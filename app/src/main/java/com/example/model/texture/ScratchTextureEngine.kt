package com.example.model.texture

import android.graphics.Bitmap
import android.graphics.Color
import java.util.UUID
import kotlin.math.abs

/**
 * Built-in procedural material patterns for PixelForge's From-Scratch Texture Painter & Material Generator.
 */
enum class ProceduralMaterialPattern(val label: String) {
    BLANK_CANVAS("Blank"),
    ENERGY_CIRCUIT("Mana Circuit"),
    ARMOR_PLATE("Clearcoat Plate"),
    ORGANIC_SCALES("Scales"),
    DUNGEON_BRICK("Brick"),
    CEL_GRADIENT("Cel Ramp")
}

/**
 * Pixel-buffer backed texture creation project supporting X, Y, and 4-way quadrant mirror painting.
 */
data class ScratchTextureProject(
    val id: String,
    val name: String,
    val sizePx: Int = 32,
    val pixels: IntArray,
    val mirrorX: Boolean = false,
    val mirrorY: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScratchTextureProject) return false
        return id == other.id &&
            name == other.name &&
            sizePx == other.sizePx &&
            pixels.contentEquals(other.pixels) &&
            mirrorX == other.mirrorX &&
            mirrorY == other.mirrorY
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + sizePx
        result = 31 * result + pixels.contentHashCode()
        result = 31 * result + mirrorX.hashCode()
        result = 31 * result + mirrorY.hashCode()
        return result
    }
}

/**
 * Pure pixel-buffer texture painter and 6-pattern procedural material generator.
 * Uses direct ARGB [IntArray] + [Bitmap.setPixels] so rendering is lossless and deterministic
 * across Android devices and Robolectric JVM unit tests.
 */
object ScratchTextureEngine {

    const val MIN_TEX_DIM = 8
    const val MAX_TEX_DIM = 64

    /**
     * Creates a brand-new flat texture canvas filled with [fillColorInt].
     */
    fun createBlankTexture(
        name: String = "New Original Texture",
        sizePx: Int = 32,
        fillColorInt: Int = Color.rgb(0x16, 0x1B, 0x26)
    ): ScratchTextureProject {
        val safeDim = sizePx.coerceIn(MIN_TEX_DIM, MAX_TEX_DIM)
        val cleanName = name.trim().ifBlank { "New Original Texture" }
        return ScratchTextureProject(
            id = "scratchtex_${UUID.randomUUID().toString().take(10)}",
            name = cleanName,
            sizePx = safeDim,
            pixels = IntArray(safeDim * safeDim) { fillColorInt },
            mirrorX = false,
            mirrorY = false
        )
    }

    /**
     * Procedurally generates a crisp tileable pixel material for all 6 [ProceduralMaterialPattern]s
     * driven by the 3 user-selected colors.
     */
    fun generateMaterialPattern(
        name: String,
        sizePx: Int,
        pattern: ProceduralMaterialPattern,
        primaryColorInt: Int,
        secondaryColorInt: Int,
        accentColorInt: Int
    ): ScratchTextureProject {
        val dim = sizePx.coerceIn(MIN_TEX_DIM, MAX_TEX_DIM)
        val cleanName = name.trim().ifBlank { "New ${pattern.label} Texture" }
        val buf = IntArray(dim * dim) { primaryColorInt }

        when (pattern) {
            ProceduralMaterialPattern.BLANK_CANVAS -> {
                // Solid primary fill already populated
            }

            ProceduralMaterialPattern.ENERGY_CIRCUIT -> {
                // Obsidian substrate + dark secondary trace channels + glowing accent circuit bus & nodes
                val darkSub = blendColors(primaryColorInt, Color.rgb(8, 12, 20), 0.35f)
                val gridStep = (dim / 4).coerceAtLeast(4)
                for (y in 0 until dim) {
                    for (x in 0 until dim) {
                        var c = primaryColorInt
                        val gx = x % gridStep
                        val gy = y % gridStep
                        if (gx == 0 || gy == 0) {
                            c = secondaryColorInt
                        }
                        // Diagonal and orthogonal glowing Mana Circuit traces
                        val isBus = (x == dim / 4 && y in 2 until dim - 2) ||
                            (y == dim / 2 && x in 2 until dim - 2) ||
                            (x == (dim * 3) / 4 && y in 4 until dim - 4) ||
                            (abs((x - dim / 4) - (y - dim / 4)) == 0 && x in (dim / 4)..(dim / 2))
                        if (isBus) {
                            c = accentColorInt
                        }
                        // Glowing junction nodes
                        val isNode = (abs(x - dim / 4) <= 1 && abs(y - dim / 2) <= 1) ||
                            (abs(x - (dim * 3) / 4) <= 1 && abs(y - dim / 2) <= 1) ||
                            (abs(x - dim / 2) <= 1 && abs(y - dim / 4) <= 1)
                        if (isNode) {
                            c = blendColors(accentColorInt, Color.WHITE, 0.35f)
                        } else if ((x + y) % 5 == 0 && c == primaryColorInt) {
                            c = darkSub
                        }
                        buf[y * dim + x] = c
                    }
                }
            }

            ProceduralMaterialPattern.ARMOR_PLATE -> {
                // Clearcoat metallic bevel armor panels with specular highlight streak and corner rivets
                val highlight = blendColors(accentColorInt, Color.WHITE, 0.45f)
                val shadow = blendColors(secondaryColorInt, Color.BLACK, 0.40f)
                val half = dim / 2
                for (y in 0 until dim) {
                    for (x in 0 until dim) {
                        val lx = x % half
                        val ly = y % half
                        val c = when {
                            lx == 0 || ly == 0 -> shadow
                            lx == half - 1 || ly == half - 1 -> secondaryColorInt
                            lx == 1 || ly == 1 -> blendColors(primaryColorInt, highlight, 0.42f)
                            (lx == 2 || lx == half - 3) && (ly == 2 || ly == half - 3) -> accentColorInt
                            abs(lx - ly) <= 1 && lx in 3..(half - 4) -> blendColors(primaryColorInt, highlight, 0.25f)
                            else -> primaryColorInt
                        }
                        buf[y * dim + x] = c
                    }
                }
            }

            ProceduralMaterialPattern.ORGANIC_SCALES -> {
                // Overlapping dragon/creature scales with staggered rows, ink ridge, and accent gleam
                val scaleW = (dim / 4).coerceAtLeast(4)
                val scaleH = (dim / 4).coerceAtLeast(4)
                val ridgeInk = blendColors(secondaryColorInt, Color.BLACK, 0.45f)
                for (y in 0 until dim) {
                    val row = y / scaleH
                    val localY = y % scaleH
                    val rowShift = if (row % 2 == 1) scaleW / 2 else 0
                    for (x in 0 until dim) {
                        val localX = (x + rowShift) % scaleW
                        val dx = (localX.toFloat() - scaleW * 0.5f) / (scaleW * 0.5f)
                        val dy = (localY.toFloat() - scaleH * 0.2f) / (scaleH * 0.85f)
                        val distSq = dx * dx + dy * dy
                        val c = when {
                            distSq > 0.92f -> ridgeInk
                            distSq > 0.72f -> secondaryColorInt
                            localY == 1 && abs(dx) < 0.45f -> accentColorInt
                            else -> primaryColorInt
                        }
                        buf[y * dim + x] = c
                    }
                }
            }

            ProceduralMaterialPattern.DUNGEON_BRICK -> {
                // Staggered stone brickwork with mortar seams, top chisel highlight, and weathered grain
                val brickH = (dim / 4).coerceAtLeast(4)
                val brickW = (dim / 2).coerceAtLeast(6)
                val bevelLight = blendColors(primaryColorInt, accentColorInt, 0.35f)
                val bevelDark = blendColors(primaryColorInt, Color.BLACK, 0.30f)
                for (y in 0 until dim) {
                    val row = y / brickH
                    val ly = y % brickH
                    val shift = if (row % 2 == 1) brickW / 2 else 0
                    for (x in 0 until dim) {
                        val lx = (x + shift) % brickW
                        val hash = ((x * 73856093) xor (y * 19349663)) and 0x7
                        val c = when {
                            ly == 0 || lx == 0 -> secondaryColorInt
                            ly == 1 -> bevelLight
                            ly == brickH - 1 || lx == brickW - 1 -> bevelDark
                            hash == 0 -> accentColorInt
                            else -> primaryColorInt
                        }
                        buf[y * dim + x] = c
                    }
                }
            }

            ProceduralMaterialPattern.CEL_GRADIENT -> {
                // Hard 3-step True Fusion cel-shaded color ramp with crisp angled halftone dots in dark shadow band
                val darkBand = blendColors(secondaryColorInt, Color.BLACK, 0.35f)
                val midBand = primaryColorInt
                val litBand = blendColors(primaryColorInt, accentColorInt, 0.45f)
                val specBand = accentColorInt
                for (y in 0 until dim) {
                    val yNorm = y.toFloat() / dim.toFloat()
                    for (x in 0 until dim) {
                        val c = when {
                            yNorm < 0.16f -> specBand
                            yNorm < 0.42f -> litBand
                            yNorm < 0.72f -> midBand
                            else -> {
                                // Subtle comic halftone dots strictly inside dark shadow band
                                val isHalftoneDot = ((x + y) % 4 == 0) && ((x - y + dim) % 4 == 0)
                                if (isHalftoneDot) secondaryColorInt else darkBand
                            }
                        }
                        buf[y * dim + x] = c
                    }
                }
            }
        }

        return ScratchTextureProject(
            id = "scratchtex_${UUID.randomUUID().toString().take(10)}",
            name = cleanName,
            sizePx = dim,
            pixels = buf,
            mirrorX = false,
            mirrorY = false
        )
    }

    /**
     * Paints a square brush (`1..3` px) onto a copied buffer with optional `mirrorX`, `mirrorY`,
     * and 4-way quadrant symmetry when both flags are enabled.
     */
    fun paintTexturePixel(
        project: ScratchTextureProject,
        x: Int,
        y: Int,
        colorInt: Int,
        brushSize: Int = 1
    ): ScratchTextureProject {
        val dim = project.sizePx
        if (dim <= 0 || project.pixels.size < dim * dim) return project
        val out = project.pixels.copyOf()
        val safeBrush = brushSize.coerceIn(1, 3)

        val offsets = when (safeBrush) {
            1 -> intArrayOf(0)
            2 -> intArrayOf(0, 1)
            else -> intArrayOf(-1, 0, 1)
        }

        fun plot(px: Int, py: Int) {
            if (px in 0 until dim && py in 0 until dim) {
                out[py * dim + px] = colorInt
            }
        }

        for (dy in offsets) {
            val py = y + dy
            val my = (dim - 1) - py
            for (dx in offsets) {
                val px = x + dx
                val mx = (dim - 1) - px

                plot(px, py)
                if (project.mirrorX) {
                    plot(mx, py)
                }
                if (project.mirrorY) {
                    plot(px, my)
                }
                if (project.mirrorX && project.mirrorY) {
                    plot(mx, my)
                }
            }
        }

        return project.copy(pixels = out)
    }

    /**
     * Exports [project] to a crisp nearest-neighbor ARGB_8888 [Bitmap] of size [outputDim]×[outputDim]
     * ready for 3D UV mapping and Vault storage.
     */
    fun toExportBitmap(
        project: ScratchTextureProject,
        outputDim: Int = 128
    ): Bitmap {
        val srcDim = project.sizePx.coerceAtLeast(1)
        val dstDim = outputDim.coerceIn(16, 1024)
        val outPixels = IntArray(dstDim * dstDim)
        val srcPixels = project.pixels

        if (srcPixels.size >= srcDim * srcDim) {
            for (dy in 0 until dstDim) {
                val sy = (dy * srcDim) / dstDim
                val srcRow = sy * srcDim
                val dstRow = dy * dstDim
                for (dx in 0 until dstDim) {
                    val sx = (dx * srcDim) / dstDim
                    outPixels[dstRow + dx] = srcPixels[srcRow + sx]
                }
            }
        }

        val bmp = Bitmap.createBitmap(dstDim, dstDim, Bitmap.Config.ARGB_8888)
        bmp.setHasAlpha(true)
        bmp.setPixels(outPixels, 0, dstDim, 0, 0, dstDim, dstDim)
        return bmp
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val inv = 1f - t
        val r = (Color.red(c1) * inv + Color.red(c2) * t).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * inv + Color.green(c2) * t).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * inv + Color.blue(c2) * t).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
