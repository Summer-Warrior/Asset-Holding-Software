package com.example.model.sprite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object SampleSpriteSheets {

    fun createCyberNinjaSheet(): SpriteSheetData {
        val frameW = 24
        val frameH = 24
        val cols = 4
        val rows = 3
        val width = frameW * cols
        val height = frameH * rows

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        fun drawPixel(bx: Int, by: Int, color: Int) {
            paint.color = color
            canvas.drawRect(
                bx.toFloat(),
                by.toFloat(),
                (bx + 1).toFloat(),
                (by + 1).toFloat(),
                paint
            )
        }

        val armorDark = Color.rgb(20, 27, 45)
        val armorSlate = Color.rgb(45, 60, 90)
        val cyanVisor = Color.rgb(0, 240, 255)
        val pinkBlade = Color.rgb(255, 42, 133)
        val skinTone = Color.rgb(255, 204, 153)
        val goldBelt = Color.rgb(245, 158, 11)

        // Draw 3 rows of animations: Row 0: Idle (4 frames), Row 1: Run (4 frames), Row 2: Slash (4 frames)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val ox = col * frameW + 4
                val oy = row * frameH + 3

                val bob = if (row == 0) if (col % 2 == 1) 1 else 0 else 0
                val runLeg = if (row == 1) (col % 4) else 0
                val slash = if (row == 2) col else 0

                // Head / Hair (4x4)
                for (hy in 0..3) {
                    for (hx in 2..7) {
                        drawPixel(ox + hx, oy + hy + bob, armorSlate)
                    }
                }
                // Visor glow
                for (vx in 3..6) {
                    drawPixel(ox + vx, oy + 2 + bob, cyanVisor)
                }

                // Torso (6x6)
                for (ty in 4..9) {
                    for (tx in 2..7) {
                        val color = if (ty == 7) goldBelt else if (tx in 3..6 && ty in 5..6) cyanVisor else armorDark
                        drawPixel(ox + tx, oy + ty + bob, color)
                    }
                }

                // Scarf / Cloak flapping
                val scarfFlap = (col % 2) * 2
                drawPixel(ox + 1, oy + 4 + bob, pinkBlade)
                drawPixel(ox + 0, oy + 5 + bob + scarfFlap, pinkBlade)
                drawPixel(ox - 1, oy + 6 + bob + scarfFlap, pinkBlade)

                // Legs & Pose
                when (row) {
                    0 -> { // Idle
                        for (ly in 10..14) {
                            drawPixel(ox + 3, oy + ly, armorSlate)
                            drawPixel(ox + 6, oy + ly, armorSlate)
                        }
                    }
                    1 -> { // Run
                        when (runLeg) {
                            0 -> {
                                for (ly in 10..13) drawPixel(ox + 2, oy + ly, armorSlate)
                                for (ly in 10..14) drawPixel(ox + 7, oy + ly, armorSlate)
                            }
                            1 -> {
                                for (ly in 10..14) drawPixel(ox + 4, oy + ly, armorSlate)
                                for (ly in 10..13) drawPixel(ox + 5, oy + ly, armorSlate)
                            }
                            2 -> {
                                for (ly in 10..14) drawPixel(ox + 6, oy + ly, armorSlate)
                                for (ly in 10..12) drawPixel(ox + 1, oy + ly, armorSlate)
                            }
                            else -> {
                                for (ly in 10..13) drawPixel(ox + 5, oy + ly, armorSlate)
                                for (ly in 10..14) drawPixel(ox + 3, oy + ly, armorSlate)
                            }
                        }
                    }
                    2 -> { // Slash attack
                        for (ly in 10..14) {
                            drawPixel(ox + 2 + (col % 2), oy + ly, armorSlate)
                            drawPixel(ox + 6, oy + ly, armorSlate)
                        }

                        // Energy Katana blade trail
                        val bladeLength = 7 + col * 2
                        for (bx in 0 until bladeLength) {
                            val bladeY = oy + 7 - (col * 2) + (bx / 3)
                            drawPixel(ox + 7 + bx, bladeY, pinkBlade)
                            if (col >= 2) {
                                drawPixel(ox + 7 + bx, bladeY - 1, Color.WHITE)
                            }
                        }
                    }
                }
            }
        }

        // Scale up by 2x for sharp viewing
        val scaled = Bitmap.createScaledBitmap(bitmap, width * 2, height * 2, false)
        return SpriteSheetData.fromBitmap(scaled, "Cyber Blade Ninja", cols, rows)
    }

    fun createDungeonSlimeSheet(): SpriteSheetData {
        val frameW = 20
        val frameH = 20
        val cols = 4
        val rows = 2
        val width = frameW * cols
        val height = frameH * rows

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        fun drawPixel(bx: Int, by: Int, color: Int) {
            paint.color = color
            canvas.drawRect(bx.toFloat(), by.toFloat(), (bx + 1).toFloat(), (by + 1).toFloat(), paint)
        }

        val slimeGreen = Color.rgb(16, 185, 129)
        val slimeHighlight = Color.rgb(110, 231, 183)
        val slimeDark = Color.rgb(5, 120, 85)
        val eyeColor = Color.rgb(15, 23, 42)

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val ox = col * frameW
                val oy = row * frameH

                val squishX = if (col == 1) 2 else if (col == 3) -1 else 0
                val squishY = if (col == 1) -2 else if (col == 3) 2 else 0

                val left = 4 - squishX
                val right = 15 + squishX
                val top = 6 + squishY
                val bottom = 17

                // Slime body
                for (y in top..bottom) {
                    val radiusAtY = if (y < top + 3) (y - top) * 2 else (right - left) / 2
                    val minX = 10 - radiusAtY
                    val maxX = 10 + radiusAtY
                    for (x in minX..maxX) {
                        val color = if (y == top + 1 && x in 8..11) slimeHighlight
                        else if (y > bottom - 2) slimeDark
                        else slimeGreen
                        drawPixel(ox + x, oy + y, color)
                    }
                }

                // Eyes
                drawPixel(ox + 8, oy + top + 4, eyeColor)
                drawPixel(ox + 12, oy + top + 4, eyeColor)
                drawPixel(ox + 8, oy + top + 3, Color.WHITE)
                drawPixel(ox + 12, oy + top + 3, Color.WHITE)
            }
        }

        val scaled = Bitmap.createScaledBitmap(bitmap, width * 2, height * 2, false)
        return SpriteSheetData.fromBitmap(scaled, "Dungeon Slime", cols, rows)
    }
}
