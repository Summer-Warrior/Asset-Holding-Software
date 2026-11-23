package com.example.model.model3d

import android.graphics.Bitmap
import kotlin.math.cos
import kotlin.math.sin

object ProceduralMeshes {

    fun createSciFiStarfighter(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()
        val uvs = mutableListOf<UV>()

        // Nose cone
        vertices.add(Vector3(0f, 0f, 1.8f)) // 0: Nose tip
        vertices.add(Vector3(-0.35f, 0.15f, 0.8f)) // 1: Cockpit top left
        vertices.add(Vector3(0.35f, 0.15f, 0.8f))  // 2: Cockpit top right
        vertices.add(Vector3(-0.3f, -0.2f, 0.8f))  // 3: Nose bottom left
        vertices.add(Vector3(0.3f, -0.2f, 0.8f))   // 4: Nose bottom right

        // Cockpit glass
        vertices.add(Vector3(0f, 0.45f, 0.3f))    // 5: Canopy peak
        vertices.add(Vector3(-0.4f, 0.2f, -0.2f)) // 6: Canopy rear left
        vertices.add(Vector3(0.4f, 0.2f, -0.2f))  // 7: Canopy rear right

        // Main fuselage
        vertices.add(Vector3(-0.5f, -0.3f, -0.2f)) // 8: Fuselage bot left
        vertices.add(Vector3(0.5f, -0.3f, -0.2f))  // 9: Fuselage bot right
        vertices.add(Vector3(-0.45f, 0.25f, -1.2f)) // 10: Engine top left
        vertices.add(Vector3(0.45f, 0.25f, -1.2f))  // 11: Engine top right
        vertices.add(Vector3(-0.45f, -0.3f, -1.2f)) // 12: Engine bot left
        vertices.add(Vector3(0.45f, -0.3f, -1.2f))  // 13: Engine bot right

        // Wings
        vertices.add(Vector3(-2.2f, 0.05f, -0.7f))  // 14: Left wingtip front
        vertices.add(Vector3(-2.0f, 0.05f, -1.3f))  // 15: Left wingtip rear
        vertices.add(Vector3(2.2f, 0.05f, -0.7f))   // 16: Right wingtip front
        vertices.add(Vector3(2.0f, 0.05f, -1.3f))   // 17: Right wingtip rear

        // Wingtip Cannons
        vertices.add(Vector3(-2.2f, 0.05f, 0.2f))   // 18: Left cannon tip
        vertices.add(Vector3(2.2f, 0.05f, 0.2f))    // 19: Right cannon tip

        // Vertical Fins
        vertices.add(Vector3(-0.4f, 0.9f, -1.1f))   // 20: Left fin top
        vertices.add(Vector3(0.4f, 0.9f, -1.1f))    // 21: Right fin top

        // UVs
        uvs.add(UV(0.5f, 1.0f))
        uvs.add(UV(0.2f, 0.6f))
        uvs.add(UV(0.8f, 0.6f))
        uvs.add(UV(0.5f, 0.0f))

        fun addTri(i0: Int, i1: Int, i2: Int, color: Long) {
            faces.add(Face3D(intArrayOf(i0, i1, i2), color = color))
        }

        fun addQuad(i0: Int, i1: Int, i2: Int, i3: Int, color: Long) {
            addTri(i0, i1, i2, color)
            addTri(i0, i2, i3, color)
        }

        val hullCyan = 0xFF00E5FF
        val cockpitGold = 0xFFF59E0B
        val darkMetal = 0xFF1E293B
        val neonMagenta = 0xFFFF2A85

        // Nose
        addTri(0, 2, 1, hullCyan)
        addTri(0, 1, 3, darkMetal)
        addTri(0, 4, 2, darkMetal)
        addTri(0, 3, 4, darkMetal)

        // Canopy
        addTri(1, 2, 5, cockpitGold)
        addTri(1, 5, 6, cockpitGold)
        addTri(2, 7, 5, cockpitGold)
        addTri(5, 7, 6, cockpitGold)

        // Fuselage sides & bottom
        addQuad(1, 6, 8, 3, darkMetal)
        addQuad(2, 4, 9, 7, darkMetal)
        addQuad(3, 8, 9, 4, darkMetal)
        addQuad(6, 10, 12, 8, hullCyan)
        addQuad(7, 9, 13, 11, hullCyan)
        addQuad(8, 12, 13, 9, darkMetal)
        addQuad(6, 7, 11, 10, darkMetal)

        // Wings
        addTri(1, 14, 6, hullCyan)
        addTri(6, 14, 15, hullCyan)
        addTri(6, 15, 10, hullCyan)

        addTri(2, 7, 16, hullCyan)
        addTri(7, 17, 16, hullCyan)
        addTri(7, 11, 17, hullCyan)

        // Wing bottoms
        addTri(8, 15, 14, darkMetal)
        addTri(9, 16, 17, darkMetal)

        // Cannons
        addTri(14, 18, 15, neonMagenta)
        addTri(16, 17, 19, neonMagenta)

        // Tail Fins
        addTri(10, 20, 6, neonMagenta)
        addTri(11, 7, 21, neonMagenta)

        // Thruster rear exhaust
        addQuad(10, 11, 13, 12, 0xFFFF5722)

        return Mesh3D.normalizeMesh("Starfighter NX-3", vertices, faces, uvs)
    }

    fun createCyberMech(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, color: Long) {
            val i = vertices.size
            val hw = w / 2f
            val hh = h / 2f
            val hd = d / 2f

            vertices.add(Vector3(x - hw, y - hh, z + hd)) // 0
            vertices.add(Vector3(x + hw, y - hh, z + hd)) // 1
            vertices.add(Vector3(x + hw, y + hh, z + hd)) // 2
            vertices.add(Vector3(x - hw, y + hh, z + hd)) // 3
            vertices.add(Vector3(x - hw, y - hh, z - hd)) // 4
            vertices.add(Vector3(x + hw, y - hh, z - hd)) // 5
            vertices.add(Vector3(x + hw, y + hh, z - hd)) // 6
            vertices.add(Vector3(x - hw, y + hh, z - hd)) // 7

            // Front
            faces.add(Face3D(intArrayOf(i, i + 1, i + 2), color = color))
            faces.add(Face3D(intArrayOf(i, i + 2, i + 3), color = color))
            // Back
            faces.add(Face3D(intArrayOf(i + 5, i + 4, i + 7), color = color))
            faces.add(Face3D(intArrayOf(i + 5, i + 7, i + 6), color = color))
            // Left
            faces.add(Face3D(intArrayOf(i + 4, i, i + 3), color = color))
            faces.add(Face3D(intArrayOf(i + 4, i + 3, i + 7), color = color))
            // Right
            faces.add(Face3D(intArrayOf(i + 1, i + 5, i + 6), color = color))
            faces.add(Face3D(intArrayOf(i + 1, i + 6, i + 2), color = color))
            // Top
            faces.add(Face3D(intArrayOf(i + 3, i + 2, i + 6), color = color))
            faces.add(Face3D(intArrayOf(i + 3, i + 6, i + 7), color = color))
            // Bottom
            faces.add(Face3D(intArrayOf(i + 4, i + 5, i + 1), color = color))
            faces.add(Face3D(intArrayOf(i + 4, i + 1, i), color = color))
        }

        val armorCyan = 0xFF00E5FF
        val armorDark = 0xFF1E293B
        val visorGold = 0xFFF59E0B
        val bladeNeon = 0xFFFF2A85
        val jointMetal = 0xFF475569

        // Head
        addBox(0f, 1.4f, 0f, 0.45f, 0.4f, 0.45f, armorCyan)
        // Visor
        addBox(0f, 1.42f, 0.24f, 0.35f, 0.14f, 0.05f, visorGold)
        // Antenna
        addBox(0.25f, 1.65f, 0f, 0.04f, 0.35f, 0.04f, visorGold)

        // Torso
        addBox(0f, 0.85f, 0f, 0.9f, 0.8f, 0.6f, armorDark)
        // Chest Plate Armor
        addBox(0f, 0.95f, 0.28f, 0.7f, 0.5f, 0.15f, armorCyan)
        // Power Core
        addBox(0f, 0.95f, 0.36f, 0.2f, 0.2f, 0.06f, bladeNeon)

        // Shoulders
        addBox(-0.65f, 1.15f, 0f, 0.4f, 0.35f, 0.45f, armorCyan)
        addBox(0.65f, 1.15f, 0f, 0.4f, 0.35f, 0.45f, armorCyan)

        // Arms
        addBox(-0.65f, 0.65f, 0.05f, 0.26f, 0.65f, 0.26f, armorDark)
        addBox(0.65f, 0.65f, 0.05f, 0.26f, 0.65f, 0.26f, armorDark)

        // Right hand Energy Blade
        addBox(0.65f, 0.3f, 0.7f, 0.06f, 0.28f, 1.1f, bladeNeon)

        // Left arm Shield
        addBox(-0.85f, 0.65f, 0.15f, 0.12f, 0.75f, 0.55f, armorCyan)

        // Waist / Belt
        addBox(0f, 0.35f, 0f, 0.65f, 0.25f, 0.45f, jointMetal)

        // Legs
        addBox(-0.28f, -0.25f, 0f, 0.32f, 0.9f, 0.35f, armorDark)
        addBox(0.28f, -0.25f, 0f, 0.32f, 0.9f, 0.35f, armorDark)

        // Knee guards
        addBox(-0.28f, -0.1f, 0.2f, 0.24f, 0.24f, 0.12f, armorCyan)
        addBox(0.28f, -0.1f, 0.2f, 0.24f, 0.24f, 0.12f, armorCyan)

        // Feet
        addBox(-0.28f, -0.75f, 0.1f, 0.35f, 0.2f, 0.6f, armorCyan)
        addBox(0.28f, -0.75f, 0.1f, 0.35f, 0.2f, 0.6f, armorCyan)

        // Back Thruster
        addBox(0f, 0.9f, -0.4f, 0.5f, 0.5f, 0.25f, jointMetal)

        return Mesh3D.normalizeMesh("Cyber Mech Strider", vertices, faces)
    }

    fun createTreasureChest(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val woodColor = 0xFF8D6E63
        val ironColor = 0xFF37474F
        val goldColor = 0xFFFFD54F

        fun addBox(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, color: Long) {
            val i = vertices.size
            val hw = w / 2f
            val hh = h / 2f
            val hd = d / 2f

            vertices.add(Vector3(x - hw, y - hh, z + hd))
            vertices.add(Vector3(x + hw, y - hh, z + hd))
            vertices.add(Vector3(x + hw, y + hh, z + hd))
            vertices.add(Vector3(x - hw, y + hh, z + hd))
            vertices.add(Vector3(x - hw, y - hh, z - hd))
            vertices.add(Vector3(x + hw, y - hh, z - hd))
            vertices.add(Vector3(x + hw, y + hh, z - hd))
            vertices.add(Vector3(x - hw, y + hh, z - hd))

            faces.add(Face3D(intArrayOf(i, i + 1, i + 2), color = color))
            faces.add(Face3D(intArrayOf(i, i + 2, i + 3), color = color))
            faces.add(Face3D(intArrayOf(i + 5, i + 4, i + 7), color = color))
            faces.add(Face3D(intArrayOf(i + 5, i + 7, i + 6), color = color))
            faces.add(Face3D(intArrayOf(i + 4, i, i + 3), color = color))
            faces.add(Face3D(intArrayOf(i + 4, i + 3, i + 7), color = color))
            faces.add(Face3D(intArrayOf(i + 1, i + 5, i + 6), color = color))
            faces.add(Face3D(intArrayOf(i + 1, i + 6, i + 2), color = color))
            faces.add(Face3D(intArrayOf(i + 3, i + 2, i + 6), color = color))
            faces.add(Face3D(intArrayOf(i + 3, i + 6, i + 7), color = color))
            faces.add(Face3D(intArrayOf(i + 4, i + 5, i + 1), color = color))
            faces.add(Face3D(intArrayOf(i + 4, i + 1, i), color = color))
        }

        // Chest Base Body
        addBox(0f, -0.2f, 0f, 1.4f, 0.7f, 1.0f, woodColor)

        // Iron Corner Bands
        addBox(-0.66f, -0.2f, 0f, 0.12f, 0.74f, 1.04f, ironColor)
        addBox(0.66f, -0.2f, 0f, 0.12f, 0.74f, 1.04f, ironColor)

        // Curved / Arched Lid Layers
        addBox(0f, 0.25f, 0f, 1.44f, 0.22f, 1.04f, woodColor)
        addBox(0f, 0.42f, 0f, 1.36f, 0.18f, 0.82f, woodColor)
        addBox(0f, 0.54f, 0f, 1.28f, 0.12f, 0.55f, woodColor)

        // Lid Iron Strips
        addBox(-0.66f, 0.36f, 0f, 0.14f, 0.4f, 1.06f, ironColor)
        addBox(0.66f, 0.36f, 0f, 0.14f, 0.4f, 1.06f, ironColor)
        addBox(0f, 0.36f, 0f, 0.14f, 0.4f, 1.06f, ironColor)

        // Front Golden Lock Plate & Keyhole
        addBox(0f, 0.05f, 0.53f, 0.22f, 0.28f, 0.06f, goldColor)
        addBox(0f, 0.02f, 0.57f, 0.06f, 0.12f, 0.03f, 0xFF212121)

        return Mesh3D.normalizeMesh("Dungeon Mimic Chest", vertices, faces)
    }

    fun createCrystalArtifact(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val crystalColor = 0xFF00F0FF
        val coreColor = 0xFFFF2A85
        val ringColor = 0xFFF59E0B

        // Double pyramid crystal core
        vertices.add(Vector3(0f, 1.5f, 0f))  // 0: top tip
        vertices.add(Vector3(0f, -1.5f, 0f)) // 1: bottom tip

        val segments = 8
        val radius = 0.6f
        for (i in 0 until segments) {
            val angle = i * (2f * Math.PI.toFloat() / segments)
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            vertices.add(Vector3(x, 0f, z))
        }

        for (i in 0 until segments) {
            val curr = 2 + i
            val next = 2 + (i + 1) % segments
            val color = if (i % 2 == 0) crystalColor else coreColor
            // Top pyramid
            faces.add(Face3D(intArrayOf(0, curr, next), color = color))
            // Bottom pyramid
            faces.add(Face3D(intArrayOf(1, next, curr), color = color))
        }

        // Orbiting Runic Ring
        val ringSegments = 16
        val ringRadius = 1.2f
        val ringThickness = 0.08f
        val ringStart = vertices.size
        for (i in 0 until ringSegments) {
            val angle = i * (2f * Math.PI.toFloat() / ringSegments)
            // Tilted ring in X/Z plane
            val rx = cos(angle) * ringRadius
            val rz = sin(angle) * ringRadius
            val ry = sin(angle * 2f) * 0.2f
            vertices.add(Vector3(rx, ry + ringThickness, rz))
            vertices.add(Vector3(rx, ry - ringThickness, rz))
        }

        for (i in 0 until ringSegments) {
            val i0 = ringStart + (i * 2)
            val i1 = ringStart + (i * 2 + 1)
            val next = (i + 1) % ringSegments
            val i2 = ringStart + (next * 2)
            val i3 = ringStart + (next * 2 + 1)

            faces.add(Face3D(intArrayOf(i0, i2, i3), color = ringColor))
            faces.add(Face3D(intArrayOf(i0, i3, i1), color = ringColor))
        }

        return Mesh3D.normalizeMesh("Aether Crystal Monolith", vertices, faces)
    }

    /**
     * Converts any 2D sprite, concept art, or high-resolution photo into a 3D relief voxel/polygon mesh
     * with no limitations on input image dimensions.
     */
    fun createVoxelReliefFromBitmap(
        bitmap: Bitmap,
        targetGrid: Int = 24,
        depthScale: Float = 0.5f,
        name: String = "Extruded Concept 3D",
        sourceKindLabel: String = "PHOTO",
        sourceSizeLabel: String = "${bitmap.width}×${bitmap.height}px"
    ): Mesh3D {
        val safeGrid = targetGrid.coerceIn(8, 64)
        val scaled = Bitmap.createScaledBitmap(bitmap, safeGrid, safeGrid, true)
        val w = scaled.width
        val h = scaled.height

        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val halfW = w / 2f
        val halfH = h / 2f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = scaled.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 30) continue // Skip transparent background

                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val depth = (0.15f + luminance * depthScale)

                val posX = (x - halfW) / halfW
                val posY = -(y - halfH) / halfH // Flip Y for 3D coordinates
                val posZ = depth / 2f

                val cubeSize = 0.95f / halfW
                val halfSize = cubeSize / 2f

                val vi = vertices.size
                vertices.add(Vector3(posX - halfSize, posY - halfSize, posZ + depth / 2f)) // 0
                vertices.add(Vector3(posX + halfSize, posY - halfSize, posZ + depth / 2f)) // 1
                vertices.add(Vector3(posX + halfSize, posY + halfSize, posZ + depth / 2f)) // 2
                vertices.add(Vector3(posX - halfSize, posY + halfSize, posZ + depth / 2f)) // 3
                vertices.add(Vector3(posX - halfSize, posY - halfSize, posZ - depth / 2f)) // 4
                vertices.add(Vector3(posX + halfSize, posY - halfSize, posZ - depth / 2f)) // 5
                vertices.add(Vector3(posX + halfSize, posY + halfSize, posZ - depth / 2f)) // 6
                vertices.add(Vector3(posX - halfSize, posY + halfSize, posZ - depth / 2f)) // 7

                val colorLong = (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()

                // Front face
                faces.add(Face3D(intArrayOf(vi, vi + 1, vi + 2), color = colorLong))
                faces.add(Face3D(intArrayOf(vi, vi + 2, vi + 3), color = colorLong))
                // Top
                faces.add(Face3D(intArrayOf(vi + 3, vi + 2, vi + 6), color = colorLong))
                faces.add(Face3D(intArrayOf(vi + 3, vi + 6, vi + 7), color = colorLong))
                // Right
                faces.add(Face3D(intArrayOf(vi + 1, vi + 5, vi + 6), color = colorLong))
                faces.add(Face3D(intArrayOf(vi + 1, vi + 6, vi + 2), color = colorLong))
                // Left
                faces.add(Face3D(intArrayOf(vi + 4, vi, vi + 3), color = colorLong))
                faces.add(Face3D(intArrayOf(vi + 4, vi + 3, vi + 7), color = colorLong))
                // Back
                faces.add(Face3D(intArrayOf(vi + 5, vi + 4, vi + 7), color = colorLong))
                faces.add(Face3D(intArrayOf(vi + 5, vi + 7, vi + 6), color = colorLong))
            }
        }

        // Fallback if the image was completely transparent
        if (vertices.isEmpty()) {
            return createCrystalArtifact().copy(
                name = name,
                sourceFormat = "Photo-to-3D Relief Mesh",
                sourceKindLabel = sourceKindLabel,
                sourceSizeLabel = sourceSizeLabel
            )
        }

        return Mesh3D.normalizeMesh(
            name = name,
            rawVertices = vertices,
            faces = faces,
            sourceFormat = "Photo-to-3D Relief Mesh (${safeGrid}×${safeGrid} Grid)",
            sourceKindLabel = sourceKindLabel,
            rawVertexCountOverride = vertices.size,
            rawFaceCountOverride = faces.size,
            sourceSizeLabel = sourceSizeLabel
        )
    }
}
