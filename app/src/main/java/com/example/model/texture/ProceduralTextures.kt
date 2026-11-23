package com.example.model.texture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sin

data class TexturePreset(
    val id: String,
    val name: String,
    val bitmap: Bitmap
)

object ProceduralTextures {

    fun createCyberGrid(size: Int = 128): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Background
        paint.color = Color.rgb(10, 15, 26)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Grid lines
        paint.color = Color.rgb(0, 240, 255)
        paint.strokeWidth = 2f
        val step = size / 8f
        for (i in 0..8) {
            val pos = i * step
            canvas.drawLine(pos, 0f, pos, size.toFloat(), paint)
            canvas.drawLine(0f, pos, size.toFloat(), pos, paint)
        }

        // Subgrid dots
        paint.color = Color.rgb(255, 42, 133)
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                canvas.drawCircle(x * step + step / 2f, y * step + step / 2f, 2.5f, paint)
            }
        }

        return bitmap
    }

    fun createLavaCrust(size: Int = 128): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val nx = x.toDouble() / 15.0
                val ny = y.toDouble() / 15.0
                val pattern = sin(nx) * sin(ny) + sin(nx * 2.3 + ny * 1.7) * 0.5
                val isCrust = pattern > -0.2

                val color = if (isCrust) {
                    val shade = (25 + (pattern * 20)).toInt().coerceIn(15, 50)
                    Color.rgb(shade, shade, shade + 5)
                } else {
                    val fire = ((pattern + 1.0) * 120.0).toInt().coerceIn(0, 255)
                    Color.rgb(255, fire, (fire / 4))
                }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    fun createStonePavers(size: Int = 128): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = Color.rgb(40, 48, 60)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val brickH = size / 8f
        val brickW = size / 4f

        paint.color = Color.rgb(85, 95, 110)
        for (r in 0 until 8) {
            val offset = if (r % 2 == 1) brickW / 2f else 0f
            for (c in -1..4) {
                val left = c * brickW + offset + 2f
                val top = r * brickH + 2f
                val right = left + brickW - 4f
                val bottom = top + brickH - 4f
                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
        return bitmap
    }

    fun createUvTestPattern(size: Int = 128): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // 4 color quadrants
        val half = size / 2f
        paint.color = Color.rgb(220, 38, 38) // Top-Left Red
        canvas.drawRect(0f, 0f, half, half, paint)
        paint.color = Color.rgb(37, 99, 235) // Top-Right Blue
        canvas.drawRect(half, 0f, size.toFloat(), half, paint)
        paint.color = Color.rgb(16, 185, 129) // Bottom-Left Green
        canvas.drawRect(0f, half, half, size.toFloat(), paint)
        paint.color = Color.rgb(245, 158, 11) // Bottom-Right Amber
        canvas.drawRect(half, half, size.toFloat(), size.toFloat(), paint)

        // Center circle
        paint.color = Color.WHITE
        canvas.drawCircle(half, half, size / 8f, paint)

        return bitmap
    }

    fun getAllPresets(): List<TexturePreset> = listOf(
        TexturePreset("cyber_grid", "Cyber Grid", createCyberGrid()),
        TexturePreset("lava_crust", "Lava Crust", createLavaCrust()),
        TexturePreset("stone_pavers", "Stone Pavers", createStonePavers()),
        TexturePreset("uv_test", "UV Calibration", createUvTestPattern())
    )
}
