package com.example.model.sprite

import android.graphics.Bitmap
import android.graphics.Color
import java.util.UUID
import kotlin.math.min

/**
 * Interactive pixel drawing tools for the From-Scratch 2D Sprite & Animation Canvas Editor.
 */
enum class PixelDrawTool(val label: String) {
    PENCIL("Pencil"),
    ERASER("Eraser"),
    FILL_BUCKET("Fill"),
    EYEDROPPER("Pick")
}

/**
 * Immutable-snapshot scratch sprite & frame animation project backed by raw ARGB [IntArray] buffers
 * so pixel editing works identically across Android devices and Robolectric JVM unit tests.
 */
data class ScratchSpriteProject(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val frames: List<IntArray>,
    val activeFrameIndex: Int = 0,
    val fps: Int = 8,
    val mirrorX: Boolean = false
) {
    val activeFramePixels: IntArray
        get() = frames.getOrElse(activeFrameIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0))) {
            IntArray(width * height) { Color.TRANSPARENT }
        }

    fun deepCopy(): ScratchSpriteProject = copy(
        frames = frames.map { it.copyOf() }
    )
}

/**
 * Pure, deterministic pixel-buffer and frame animation engine for creating brand-new 2D sprites
 * and spritesheets from scratch (starting from 100% transparent canvases).
 */
object SpriteCanvasEditorEngine {

    const val MIN_CANVAS_DIM = 8
    const val MAX_CANVAS_DIM = 64
    const val MIN_FRAME_COUNT = 1
    const val MAX_FRAME_COUNT = 16

    /**
     * Creates a brand-new blank project with completely transparent (`Color.TRANSPARENT`) frames.
     */
    fun createBlankProject(
        name: String,
        width: Int,
        height: Int,
        frameCount: Int = 1
    ): ScratchSpriteProject {
        val safeW = width.coerceIn(MIN_CANVAS_DIM, MAX_CANVAS_DIM)
        val safeH = height.coerceIn(MIN_CANVAS_DIM, MAX_CANVAS_DIM)
        val safeFrames = frameCount.coerceIn(MIN_FRAME_COUNT, MAX_FRAME_COUNT)
        val cleanName = name.trim().ifBlank { "New Original Sprite" }
        val blankFrames = List(safeFrames) { IntArray(safeW * safeH) { Color.TRANSPARENT } }

        return ScratchSpriteProject(
            id = "scratch_${UUID.randomUUID().toString().take(10)}",
            name = cleanName,
            width = safeW,
            height = safeH,
            frames = blankFrames,
            activeFrameIndex = 0,
            fps = 8,
            mirrorX = false
        )
    }

    /**
     * Paints a square brush (`1..3` px) onto a copied pixel buffer at `(x, y)` and, when [mirrorX]
     * is enabled, also paints symmetrically across `(width - 1 - x, y)`.
     */
    fun paintPixel(
        framePixels: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        colorInt: Int,
        brushSize: Int = 1,
        mirrorX: Boolean = false
    ): IntArray {
        val out = framePixels.copyOf()
        if (width <= 0 || height <= 0 || out.size < width * height) return out
        val safeBrush = brushSize.coerceIn(1, 3)

        val offsets = when (safeBrush) {
            1 -> intArrayOf(0)
            2 -> intArrayOf(0, 1)
            else -> intArrayOf(-1, 0, 1)
        }

        for (dy in offsets) {
            val py = y + dy
            if (py !in 0 until height) continue
            val rowOff = py * width
            for (dx in offsets) {
                val px = x + dx
                if (px in 0 until width) {
                    out[rowOff + px] = colorInt
                }
                if (mirrorX) {
                    val mx = (width - 1) - px
                    if (mx in 0 until width) {
                        out[rowOff + mx] = colorInt
                    }
                }
            }
        }
        return out
    }

    /**
     * Iterative 4-connected BFS flood fill (never recursive, zero stack overflow risk).
     */
    fun floodFill(
        framePixels: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        replacementColor: Int
    ): IntArray {
        val out = framePixels.copyOf()
        if (width <= 0 || height <= 0 || out.size < width * height) return out
        if (startX !in 0 until width || startY !in 0 until height) return out

        val startIdx = startY * width + startX
        val targetColor = out[startIdx]
        if (targetColor == replacementColor) return out

        val queue = IntArray(width * height)
        val visited = BooleanArray(width * height)
        var head = 0
        var tail = 0

        queue[tail++] = startIdx
        visited[startIdx] = true

        while (head < tail) {
            val idx = queue[head++]
            out[idx] = replacementColor
            val cx = idx % width
            val cy = idx / width

            if (cx > 0) {
                val left = idx - 1
                if (!visited[left] && out[left] == targetColor) {
                    visited[left] = true
                    queue[tail++] = left
                }
            }
            if (cx + 1 < width) {
                val right = idx + 1
                if (!visited[right] && out[right] == targetColor) {
                    visited[right] = true
                    queue[tail++] = right
                }
            }
            if (cy > 0) {
                val up = idx - width
                if (!visited[up] && out[up] == targetColor) {
                    visited[up] = true
                    queue[tail++] = up
                }
            }
            if (cy + 1 < height) {
                val down = idx + width
                if (!visited[down] && out[down] == targetColor) {
                    visited[down] = true
                    queue[tail++] = down
                }
            }
        }
        return out
    }

    /**
     * Appends a brand-new blank transparent frame (up to 16 frames max) and selects it.
     */
    fun addBlankFrame(project: ScratchSpriteProject): ScratchSpriteProject {
        if (project.frames.size >= MAX_FRAME_COUNT) return project
        val blank = IntArray(project.width * project.height) { Color.TRANSPARENT }
        val updated = project.frames.map { it.copyOf() } + listOf(blank)
        return project.copy(
            frames = updated,
            activeFrameIndex = updated.lastIndex
        )
    }

    /**
     * Duplicates the currently active frame right after its current slot (up to 16 frames max).
     */
    fun duplicateActiveFrame(project: ScratchSpriteProject): ScratchSpriteProject {
        if (project.frames.size >= MAX_FRAME_COUNT || project.frames.isEmpty()) return project
        val activeIdx = project.activeFrameIndex.coerceIn(0, project.frames.lastIndex)
        val clonedFrames = project.frames.map { it.copyOf() }.toMutableList()
        val copyOfActive = clonedFrames[activeIdx].copyOf()
        val insertIdx = activeIdx + 1
        clonedFrames.add(insertIdx, copyOfActive)
        return project.copy(
            frames = clonedFrames,
            activeFrameIndex = insertIdx
        )
    }

    /**
     * Deletes the active frame while always keeping at least 1 frame in the project.
     */
    fun deleteActiveFrame(project: ScratchSpriteProject): ScratchSpriteProject {
        if (project.frames.size <= MIN_FRAME_COUNT) return project
        val activeIdx = project.activeFrameIndex.coerceIn(0, project.frames.lastIndex)
        val remaining = project.frames.map { it.copyOf() }.toMutableList()
        remaining.removeAt(activeIdx)
        val nextActive = activeIdx.coerceAtMost(remaining.lastIndex)
        return project.copy(
            frames = remaining,
            activeFrameIndex = nextActive
        )
    }

    /**
     * Clears every pixel of the active frame back to `Color.TRANSPARENT`.
     */
    fun clearActiveFrame(project: ScratchSpriteProject): ScratchSpriteProject {
        if (project.frames.isEmpty()) return project
        val activeIdx = project.activeFrameIndex.coerceIn(0, project.frames.lastIndex)
        val updated = project.frames.mapIndexed { idx, buf ->
            if (idx == activeIdx) IntArray(project.width * project.height) { Color.TRANSPARENT } else buf.copyOf()
        }
        return project.copy(frames = updated)
    }

    /**
     * Converts a raw ARGB pixel buffer into a crisp ARGB_8888 [Bitmap] with alpha preserved.
     */
    fun frameToBitmap(framePixels: IntArray, width: Int, height: Int): Bitmap {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        bmp.setHasAlpha(true)
        if (framePixels.size >= safeW * safeH) {
            bmp.setPixels(framePixels, 0, safeW, 0, 0, safeW, safeH)
        } else {
            bmp.eraseColor(Color.TRANSPARENT)
        }
        return bmp
    }

    /**
     * Exports all frames of [project] into a crisp [SpriteSheetData] atlas via lossless pixel blitting,
     * tagged `sourceKindLabel = "CREATED FROM SCRATCH"` with an auto-built `Custom Loop` animation.
     */
    fun exportProjectToSpriteSheet(project: ScratchSpriteProject): SpriteSheetData {
        val frameBuffers = project.frames.ifEmpty {
            listOf(IntArray(project.width * project.height) { Color.TRANSPARENT })
        }
        val frameBitmaps = frameBuffers.map { frameToBitmap(it, project.width, project.height) }
        val frameCount = frameBitmaps.size
        val cols = min(4, frameCount).coerceAtLeast(1)
        val rows = (frameCount + cols - 1) / cols

        val atlasW = project.width * cols
        val atlasH = project.height * rows
        val atlasBmp = Bitmap.createBitmap(atlasW, atlasH, Bitmap.Config.ARGB_8888)
        atlasBmp.setHasAlpha(true)
        atlasBmp.eraseColor(Color.TRANSPARENT)

        frameBuffers.forEachIndexed { idx, pixels ->
            val col = idx % cols
            val row = idx / cols
            if (pixels.size >= project.width * project.height) {
                atlasBmp.setPixels(
                    pixels,
                    0,
                    project.width,
                    col * project.width,
                    row * project.height,
                    project.width,
                    project.height
                )
            }
        }

        val cleanName = project.name.trim().ifBlank { "New Original Sprite" }
        val customAnim = SpriteAnimation(
            name = "Custom Loop",
            frameIndices = (0 until frameCount).toList(),
            fps = project.fps.coerceIn(1, 30),
            loop = true
        )

        return SpriteSheetData(
            id = project.id,
            name = cleanName,
            sourceBitmap = atlasBmp,
            columns = cols,
            rows = rows,
            frameWidth = project.width,
            frameHeight = project.height,
            frames = frameBitmaps,
            animations = listOf(customAnim),
            originalWidth = atlasW,
            originalHeight = atlasH,
            sourceKindLabel = "CREATED FROM SCRATCH",
            sourceSizeLabel = "${project.width}×${project.height}px • ${frameCount}f"
        )
    }
}
