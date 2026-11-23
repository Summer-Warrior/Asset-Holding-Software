package com.example.model.sprite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

data class SpriteAnimation(
    val name: String,
    val frameIndices: List<Int>,
    val fps: Int = 8,
    val loop: Boolean = true
)

data class SpriteSheetData(
    val id: String,
    val name: String,
    val sourceBitmap: Bitmap,
    val columns: Int,
    val rows: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val frames: List<Bitmap>,
    val animations: List<SpriteAnimation>,
    val originalWidth: Int = sourceBitmap.width,
    val originalHeight: Int = sourceBitmap.height,
    val sourceKindLabel: String = "STUDIO",
    val sourceSizeLabel: String = "Unbounded Stream"
) {
    companion object {
        fun fromBitmap(
            bitmap: Bitmap,
            name: String,
            columns: Int,
            rows: Int,
            originalWidthOverride: Int? = null,
            originalHeightOverride: Int? = null,
            sourceKindLabel: String = "STUDIO",
            sourceSizeLabel: String = "Unbounded Stream"
        ): SpriteSheetData {
            val safeCols = columns.coerceAtLeast(1)
            val safeRows = rows.coerceAtLeast(1)

            // Ensure the working sheet bitmap is at least safeCols x safeRows pixels so any small or huge
            // photo/file can be sliced into any grid without IllegalArgumentException
            val minRequiredW = safeCols.coerceAtLeast(1)
            val minRequiredH = safeRows.coerceAtLeast(1)
            val workingBitmap = if (bitmap.width < minRequiredW || bitmap.height < minRequiredH) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    bitmap.width.coerceAtLeast(minRequiredW * 8),
                    bitmap.height.coerceAtLeast(minRequiredH * 8),
                    false
                )
            } else {
                bitmap
            }

            val frameW = (workingBitmap.width / safeCols).coerceAtLeast(1)
            val frameH = (workingBitmap.height / safeRows).coerceAtLeast(1)

            // Cap individual preview frame bitmap dimensions to 512px if source sheet is massive (e.g. 8K/16K)
            // so UI playback stays 60fps and never exceeds Android Canvas limits while preserving full readability
            val maxFrameDim = maxOf(frameW, frameH)
            val targetFrameW = if (maxFrameDim > 512) (frameW * 512 / maxFrameDim).coerceAtLeast(1) else frameW
            val targetFrameH = if (maxFrameDim > 512) (frameH * 512 / maxFrameDim).coerceAtLeast(1) else frameH

            val frameList = ArrayList<Bitmap>(safeCols * safeRows)
            for (r in 0 until safeRows) {
                for (c in 0 until safeCols) {
                    val srcX = (c * frameW).coerceIn(0, (workingBitmap.width - 1).coerceAtLeast(0))
                    val srcY = (r * frameH).coerceIn(0, (workingBitmap.height - 1).coerceAtLeast(0))
                    val sliceW = frameW.coerceAtMost((workingBitmap.width - srcX).coerceAtLeast(1))
                    val sliceH = frameH.coerceAtMost((workingBitmap.height - srcY).coerceAtLeast(1))

                    val rawSlice = Bitmap.createBitmap(workingBitmap, srcX, srcY, sliceW, sliceH)
                    val frameBitmap = if (sliceW != targetFrameW || sliceH != targetFrameH) {
                        Bitmap.createScaledBitmap(rawSlice, targetFrameW, targetFrameH, true)
                    } else {
                        rawSlice
                    }
                    frameList.add(frameBitmap)
                }
            }

            val defaultAnims = mutableListOf<SpriteAnimation>()
            if (safeRows == 1) {
                defaultAnims.add(SpriteAnimation("All Frames", (0 until frameList.size).toList(), fps = 8))
            } else {
                for (r in 0 until safeRows) {
                    val rowIndices = (r * safeCols until (r + 1) * safeCols).toList()
                    val animName = when (r) {
                        0 -> "Idle"
                        1 -> "Walk"
                        2 -> "Attack"
                        3 -> "Action"
                        else -> "Row ${r + 1}"
                    }
                    defaultAnims.add(SpriteAnimation(animName, rowIndices, fps = 8))
                }
            }

            val origW = originalWidthOverride ?: bitmap.width
            val origH = originalHeightOverride ?: bitmap.height

            return SpriteSheetData(
                id = System.currentTimeMillis().toString(),
                name = name,
                sourceBitmap = workingBitmap,
                columns = safeCols,
                rows = safeRows,
                frameWidth = origW / safeCols,
                frameHeight = origH / safeRows,
                frames = frameList,
                animations = defaultAnims,
                originalWidth = origW,
                originalHeight = origH,
                sourceKindLabel = sourceKindLabel,
                sourceSizeLabel = sourceSizeLabel
            )
        }

        fun assembleSheet(frames: List<Bitmap>, columns: Int): Bitmap {
            if (frames.isEmpty()) return Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val fw = frames[0].width.coerceAtLeast(1)
            val fh = frames[0].height.coerceAtLeast(1)
            val cols = columns.coerceIn(1, frames.size)
            val rows = (frames.size + cols - 1) / cols

            val sheet = Bitmap.createBitmap(fw * cols, fh * rows, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(sheet)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)

            for ((i, frame) in frames.withIndex()) {
                val c = i % cols
                val r = i / cols
                canvas.drawBitmap(frame, (c * fw).toFloat(), (r * fh).toFloat(), paint)
            }
            return sheet
        }
    }
}
