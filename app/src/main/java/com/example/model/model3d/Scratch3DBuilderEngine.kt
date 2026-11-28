package com.example.model.model3d

import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Built-in 3D primitives for PixelForge's From-Scratch 3D Kitbash & Model Sculptor.
 */
enum class PrimitiveShapeType(val label: String) {
    BOX("Box"),
    SPHERE("Sphere"),
    CYLINDER("Cylinder"),
    CONE("Cone"),
    PYRAMID("Pyramid"),
    TORUS_RING("Ring"),
    WEDGE_WING("Wing")
}

/**
 * Single transformable 3D primitive part inside a [Scratch3DProject].
 */
data class Scratch3DPart(
    val id: String,
    val name: String,
    val shape: PrimitiveShapeType,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val scaleX: Float = 0.6f,
    val scaleY: Float = 0.6f,
    val scaleZ: Float = 0.6f,
    val rotYDeg: Float = 0f,
    val colorLong: Long = 0xFF00E5FF,
    val mirrorX: Boolean = false
)

/**
 * Blank-slate 3D sculpting / kitbash project composed of user-added primitive parts.
 */
data class Scratch3DProject(
    val id: String,
    val name: String,
    val parts: List<Scratch3DPart> = emptyList(),
    val selectedPartId: String? = null
) {
    val selectedPart: Scratch3DPart?
        get() = parts.firstOrNull { it.id == selectedPartId } ?: parts.lastOrNull()
}

/**
 * Pure Kotlin 3D primitive geometry compiler and kitbash editor engine.
 * Compiles [Scratch3DProject] scenes into normalized [Mesh3D] assets ready for Viewport3D,
 * Wavefront `.OBJ` export, Room history, and Game -> Group -> Character staging.
 */
object Scratch3DBuilderEngine {

    private data class RawTriGeometry(
        val vertices: List<Vector3>,
        val uvs: List<UV>,
        val triangles: List<Triple<Int, Int, Int>>
    )

    /**
     * Starts a 100% blank 3D scene with 0 parts.
     */
    fun createEmptyProject(name: String = "New Original 3D Model"): Scratch3DProject {
        val cleanName = name.trim().ifBlank { "New Original 3D Model" }
        return Scratch3DProject(
            id = "scratch3d_${UUID.randomUUID().toString().take(10)}",
            name = cleanName,
            parts = emptyList(),
            selectedPartId = null
        )
    }

    /**
     * Appends a new primitive part with sensible non-overlapping placement, selects it, and returns
     * the updated project.
     */
    fun addPart(
        project: Scratch3DProject,
        shape: PrimitiveShapeType,
        colorLong: Long
    ): Scratch3DProject {
        val count = project.parts.size
        val partIndex = count + 1

        val defaultPosX: Float
        val defaultPosY: Float
        val defaultPosZ: Float
        val defaultScaleX: Float
        val defaultScaleY: Float
        val defaultScaleZ: Float
        val defaultMirrorX: Boolean

        when (shape) {
            PrimitiveShapeType.WEDGE_WING -> {
                defaultPosX = 0.58f
                defaultPosY = (count % 3) * 0.14f
                defaultPosZ = 0f
                defaultScaleX = 0.72f
                defaultScaleY = 0.24f
                defaultScaleZ = 0.55f
                defaultMirrorX = true
            }
            PrimitiveShapeType.TORUS_RING -> {
                defaultPosX = 0f
                defaultPosY = if (count == 0) 0f else 0.32f
                defaultPosZ = 0f
                defaultScaleX = 0.72f
                defaultScaleY = 0.72f
                defaultScaleZ = 0.72f
                defaultMirrorX = false
            }
            else -> {
                if (count == 0) {
                    defaultPosX = 0f
                    defaultPosY = 0f
                    defaultPosZ = 0f
                } else {
                    val tier = ((count - 1) / 2) + 1
                    val sign = if (count % 2 == 1) 1f else -1f
                    defaultPosX = 0f
                    defaultPosY = (sign * 0.42f * tier).coerceIn(-1.2f, 1.2f)
                    defaultPosZ = 0f
                }
                defaultScaleX = 0.6f
                defaultScaleY = 0.6f
                defaultScaleZ = 0.6f
                defaultMirrorX = false
            }
        }

        val newPart = Scratch3DPart(
            id = "part_${UUID.randomUUID().toString().take(8)}",
            name = "${shape.label} #$partIndex",
            shape = shape,
            posX = defaultPosX,
            posY = defaultPosY,
            posZ = defaultPosZ,
            scaleX = defaultScaleX,
            scaleY = defaultScaleY,
            scaleZ = defaultScaleZ,
            rotYDeg = 0f,
            colorLong = colorLong,
            mirrorX = defaultMirrorX
        )

        return project.copy(
            parts = project.parts + newPart,
            selectedPartId = newPart.id
        )
    }

    /**
     * Duplicates the currently selected part with a slight vertical/lateral offset so it is immediately visible.
     */
    fun duplicateSelectedPart(project: Scratch3DProject): Scratch3DProject {
        val target = project.selectedPart ?: return project
        val clone = target.copy(
            id = "part_${UUID.randomUUID().toString().take(8)}",
            name = "${target.name} Copy",
            posY = (target.posY + 0.28f).coerceIn(-1.5f, 1.5f)
        )
        return project.copy(
            parts = project.parts + clone,
            selectedPartId = clone.id
        )
    }

    /**
     * Deletes the currently selected part and selects a neighbor part if any remain.
     */
    fun deleteSelectedPart(project: Scratch3DProject): Scratch3DProject {
        val target = project.selectedPart ?: return project
        val idx = project.parts.indexOfFirst { it.id == target.id }
        val remaining = project.parts.filterNot { it.id == target.id }
        val nextSelected = if (remaining.isEmpty()) {
            null
        } else {
            remaining[idx.coerceAtMost(remaining.lastIndex)].id
        }
        return project.copy(
            parts = remaining,
            selectedPartId = nextSelected
        )
    }

    /**
     * Applies [transform] to the currently selected part cleanly.
     */
    fun updateSelectedPart(
        project: Scratch3DProject,
        transform: (Scratch3DPart) -> Scratch3DPart
    ): Scratch3DProject {
        val target = project.selectedPart ?: return project
        val updatedParts = project.parts.map { part ->
            if (part.id == target.id) {
                val t = transform(part)
                t.copy(
                    posX = t.posX.coerceIn(-1.5f, 1.5f),
                    posY = t.posY.coerceIn(-1.5f, 1.5f),
                    posZ = t.posZ.coerceIn(-1.5f, 1.5f),
                    scaleX = t.scaleX.coerceIn(0.1f, 1.8f),
                    scaleY = t.scaleY.coerceIn(0.1f, 1.8f),
                    scaleZ = t.scaleZ.coerceIn(0.1f, 1.8f),
                    rotYDeg = ((t.rotYDeg % 360f) + 360f) % 360f
                )
            } else {
                part
            }
        }
        return project.copy(parts = updatedParts)
    }

    /**
     * Compiles all parts (plus mirrored X twins where enabled) into a normalized, UV-ready [Mesh3D]
     * tagged `sourceKindLabel = "CREATED FROM SCRATCH"`.
     */
    fun compileProjectToMesh3D(project: Scratch3DProject): Mesh3D {
        val cleanName = project.name.trim().ifBlank { "New Original 3D Model" }
        if (project.parts.isEmpty()) {
            return Mesh3D(
                name = cleanName,
                vertices = emptyList(),
                faces = emptyList(),
                sourceFormat = "Scratch 3D Kitbash (.OBJ)",
                sourceKindLabel = "CREATED FROM SCRATCH",
                sourceSizeLabel = "0 parts • Blank Scene"
            )
        }

        val allVertices = mutableListOf<Vector3>()
        val allUvs = mutableListOf<UV>()
        val allFaces = mutableListOf<Face3D>()
        val materials = linkedMapOf<String, Material3D>()

        project.parts.forEachIndexed { partIdx, part ->
            val matKey = "mat_part_${partIdx + 1}"
            materials[matKey] = Material3D(
                name = matKey,
                diffuseColor = part.colorLong,
                specular = 0.65f,
                roughness = 0.35f
            )

            val baseGeom = buildPrimitiveGeometry(part.shape)

            // Primary instance
            appendTransformedPart(
                geom = baseGeom,
                part = part,
                mirrored = false,
                materialName = matKey,
                outVertices = allVertices,
                outUvs = allUvs,
                outFaces = allFaces
            )

            // Symmetric X mirror twin (reverses triangle winding so outward normals are preserved)
            if (part.mirrorX && abs(part.posX) > 0.02f) {
                appendTransformedPart(
                    geom = baseGeom,
                    part = part,
                    mirrored = true,
                    materialName = matKey,
                    outVertices = allVertices,
                    outUvs = allUvs,
                    outFaces = allFaces
                )
            }
        }

        return Mesh3D.normalizeMesh(
            name = cleanName,
            rawVertices = allVertices,
            faces = allFaces,
            uvs = allUvs,
            materials = materials,
            sourceFormat = "Scratch 3D Kitbash (.OBJ)",
            sourceKindLabel = "CREATED FROM SCRATCH",
            sourceSizeLabel = "${project.parts.size} parts • ${allVertices.size}v / ${allFaces.size}f"
        )
    }

    private fun appendTransformedPart(
        geom: RawTriGeometry,
        part: Scratch3DPart,
        mirrored: Boolean,
        materialName: String,
        outVertices: MutableList<Vector3>,
        outUvs: MutableList<UV>,
        outFaces: MutableList<Face3D>
    ) {
        val baseVertexIndex = outVertices.size
        val baseUvIndex = outUvs.size

        val angleRad = Math.toRadians((if (mirrored) -part.rotYDeg else part.rotYDeg).toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        val tx = if (mirrored) -part.posX else part.posX
        val ty = part.posY
        val tz = part.posZ

        for (v in geom.vertices) {
            val sx = (if (mirrored) -v.x else v.x) * part.scaleX
            val sy = v.y * part.scaleY
            val sz = v.z * part.scaleZ

            // Rotate around Y axis
            val rx = sx * cosA + sz * sinA
            val rz = -sx * sinA + sz * cosA

            outVertices.add(Vector3(rx + tx, sy + ty, rz + tz))
        }

        for (uv in geom.uvs) {
            outUvs.add(uv)
        }

        for ((a, b, c) in geom.triangles) {
            val i0 = baseVertexIndex + a
            val i1 = baseVertexIndex + b
            val i2 = baseVertexIndex + c
            val u0 = baseUvIndex + a
            val u1 = baseUvIndex + b
            val u2 = baseUvIndex + c

            // Flipping X reverses handedness; reversing triangle winding keeps normals outward-facing
            val faceIndices = if (mirrored) intArrayOf(i0, i2, i1) else intArrayOf(i0, i1, i2)
            val uvIndices = if (mirrored) intArrayOf(u0, u2, u1) else intArrayOf(u0, u1, u2)

            outFaces.add(
                Face3D(
                    indices = faceIndices,
                    uvIndices = uvIndices,
                    normal = null,
                    color = part.colorLong,
                    materialName = materialName
                )
            )
        }
    }

    private fun buildPrimitiveGeometry(shape: PrimitiveShapeType): RawTriGeometry = when (shape) {
        PrimitiveShapeType.BOX -> buildBox()
        PrimitiveShapeType.SPHERE -> buildSphere(latSegments = 8, lonSegments = 12)
        PrimitiveShapeType.CYLINDER -> buildCylinder(radialSegments = 12)
        PrimitiveShapeType.CONE -> buildCone(radialSegments = 12)
        PrimitiveShapeType.PYRAMID -> buildPyramid()
        PrimitiveShapeType.TORUS_RING -> buildTorusRing(majorSegments = 14, minorSegments = 8)
        PrimitiveShapeType.WEDGE_WING -> buildWedgeWing()
    }

    private fun buildBox(): RawTriGeometry {
        val h = 0.5f
        val verts = listOf(
            Vector3(-h, -h,  h), // 0 front-bottom-left
            Vector3( h, -h,  h), // 1 front-bottom-right
            Vector3( h,  h,  h), // 2 front-top-right
            Vector3(-h,  h,  h), // 3 front-top-left
            Vector3(-h, -h, -h), // 4 back-bottom-left
            Vector3( h, -h, -h), // 5 back-bottom-right
            Vector3( h,  h, -h), // 6 back-top-right
            Vector3(-h,  h, -h)  // 7 back-top-left
        )
        val uvs = listOf(
            UV(0f, 1f), UV(1f, 1f), UV(1f, 0f), UV(0f, 0f),
            UV(1f, 1f), UV(0f, 1f), UV(0f, 0f), UV(1f, 0f)
        )
        val tris = listOf(
            // Front (+Z)
            Triple(0, 1, 2), Triple(0, 2, 3),
            // Back (-Z)
            Triple(5, 4, 7), Triple(5, 7, 6),
            // Right (+X)
            Triple(1, 5, 6), Triple(1, 6, 2),
            // Left (-X)
            Triple(4, 0, 3), Triple(4, 3, 7),
            // Top (+Y)
            Triple(3, 2, 6), Triple(3, 6, 7),
            // Bottom (-Y)
            Triple(4, 5, 1), Triple(4, 1, 0)
        )
        return RawTriGeometry(verts, uvs, tris)
    }

    private fun buildSphere(latSegments: Int, lonSegments: Int): RawTriGeometry {
        val verts = mutableListOf<Vector3>()
        val uvs = mutableListOf<UV>()
        val tris = mutableListOf<Triple<Int, Int, Int>>()
        val r = 0.5f

        for (lat in 0..latSegments) {
            val vFrac = lat.toFloat() / latSegments.toFloat()
            val theta = vFrac * PI.toFloat()
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)
            for (lon in 0..lonSegments) {
                val uFrac = lon.toFloat() / lonSegments.toFloat()
                val phi = uFrac * (2.0 * PI).toFloat()
                val x = r * sinTheta * cos(phi)
                val y = r * cosTheta
                val z = r * sinTheta * sin(phi)
                verts.add(Vector3(x, y, z))
                uvs.add(UV(uFrac, vFrac))
            }
        }

        val ringStride = lonSegments + 1
        for (lat in 0 until latSegments) {
            for (lon in 0 until lonSegments) {
                val a = lat * ringStride + lon
                val b = a + 1
                val c = (lat + 1) * ringStride + lon
                val d = c + 1
                if (lat > 0) {
                    tris.add(Triple(a, b, c))
                }
                if (lat < latSegments - 1) {
                    tris.add(Triple(b, d, c))
                }
            }
        }
        return RawTriGeometry(verts, uvs, tris)
    }

    private fun buildCylinder(radialSegments: Int): RawTriGeometry {
        val verts = mutableListOf<Vector3>()
        val uvs = mutableListOf<UV>()
        val tris = mutableListOf<Triple<Int, Int, Int>>()
        val r = 0.5f
        val halfH = 0.5f

        // 0 = top center, 1 = bottom center
        verts.add(Vector3(0f, halfH, 0f))
        uvs.add(UV(0.5f, 0.5f))
        verts.add(Vector3(0f, -halfH, 0f))
        uvs.add(UV(0.5f, 0.5f))

        val topRingStart = 2
        val botRingStart = 2 + radialSegments

        for (i in 0 until radialSegments) {
            val frac = i.toFloat() / radialSegments.toFloat()
            val angle = frac * (2.0 * PI).toFloat()
            val x = r * cos(angle)
            val z = r * sin(angle)
            verts.add(Vector3(x, halfH, z))
            uvs.add(UV(frac, 0f))
        }
        for (i in 0 until radialSegments) {
            val frac = i.toFloat() / radialSegments.toFloat()
            val angle = frac * (2.0 * PI).toFloat()
            val x = r * cos(angle)
            val z = r * sin(angle)
            verts.add(Vector3(x, -halfH, z))
            uvs.add(UV(frac, 1f))
        }

        for (i in 0 until radialSegments) {
            val next = (i + 1) % radialSegments
            val t0 = topRingStart + i
            val t1 = topRingStart + next
            val b0 = botRingStart + i
            val b1 = botRingStart + next

            // Top cap
            tris.add(Triple(0, t1, t0))
            // Bottom cap
            tris.add(Triple(1, b0, b1))
            // Side wall
            tris.add(Triple(t0, t1, b0))
            tris.add(Triple(t1, b1, b0))
        }
        return RawTriGeometry(verts, uvs, tris)
    }

    private fun buildCone(radialSegments: Int): RawTriGeometry {
        val verts = mutableListOf<Vector3>()
        val uvs = mutableListOf<UV>()
        val tris = mutableListOf<Triple<Int, Int, Int>>()
        val r = 0.5f
        val halfH = 0.5f

        // 0 = apex, 1 = base center
        verts.add(Vector3(0f, halfH, 0f))
        uvs.add(UV(0.5f, 0f))
        verts.add(Vector3(0f, -halfH, 0f))
        uvs.add(UV(0.5f, 0.5f))

        val baseStart = 2
        for (i in 0 until radialSegments) {
            val frac = i.toFloat() / radialSegments.toFloat()
            val angle = frac * (2.0 * PI).toFloat()
            val x = r * cos(angle)
            val z = r * sin(angle)
            verts.add(Vector3(x, -halfH, z))
            uvs.add(UV(frac, 1f))
        }

        for (i in 0 until radialSegments) {
            val next = (i + 1) % radialSegments
            val b0 = baseStart + i
            val b1 = baseStart + next
            // Side cone triangle
            tris.add(Triple(0, b1, b0))
            // Base cap
            tris.add(Triple(1, b0, b1))
        }
        return RawTriGeometry(verts, uvs, tris)
    }

    private fun buildPyramid(): RawTriGeometry {
        val h = 0.5f
        val verts = listOf(
            Vector3(0f, h, 0f),    // 0 apex
            Vector3(-h, -h,  h),   // 1 front-left
            Vector3( h, -h,  h),   // 2 front-right
            Vector3( h, -h, -h),   // 3 back-right
            Vector3(-h, -h, -h)    // 4 back-left
        )
        val uvs = listOf(
            UV(0.5f, 0f),
            UV(0f, 1f),
            UV(1f, 1f),
            UV(1f, 0f),
            UV(0f, 0f)
        )
        val tris = listOf(
            Triple(0, 1, 2), // Front
            Triple(0, 2, 3), // Right
            Triple(0, 3, 4), // Back
            Triple(0, 4, 1), // Left
            Triple(1, 4, 3), // Base 1
            Triple(1, 3, 2)  // Base 2
        )
        return RawTriGeometry(verts, uvs, tris)
    }

    private fun buildTorusRing(majorSegments: Int, minorSegments: Int): RawTriGeometry {
        val verts = mutableListOf<Vector3>()
        val uvs = mutableListOf<UV>()
        val tris = mutableListOf<Triple<Int, Int, Int>>()
        val majorR = 0.36f
        val minorR = 0.14f

        for (i in 0 until majorSegments) {
            val uFrac = i.toFloat() / majorSegments.toFloat()
            val u = uFrac * (2.0 * PI).toFloat()
            val cosU = cos(u)
            val sinU = sin(u)
            for (j in 0 until minorSegments) {
                val vFrac = j.toFloat() / minorSegments.toFloat()
                val v = vFrac * (2.0 * PI).toFloat()
                val cosV = cos(v)
                val sinV = sin(v)
                val radial = majorR + minorR * cosV
                val x = radial * cosU
                val y = minorR * sinV
                val z = radial * sinU
                verts.add(Vector3(x, y, z))
                uvs.add(UV(uFrac, vFrac))
            }
        }

        for (i in 0 until majorSegments) {
            val nextI = (i + 1) % majorSegments
            for (j in 0 until minorSegments) {
                val nextJ = (j + 1) % minorSegments
                val a = i * minorSegments + j
                val b = nextI * minorSegments + j
                val c = nextI * minorSegments + nextJ
                val d = i * minorSegments + nextJ
                tris.add(Triple(a, b, c))
                tris.add(Triple(a, c, d))
            }
        }
        return RawTriGeometry(verts, uvs, tris)
    }

    /**
     * Swept aerodynamic wedge/wing extending along +X from root (-0.45) to swept tip (+0.55),
     * designed to pair naturally with `mirrorX = true` for mechs, starfighters, and winged creatures.
     */
    private fun buildWedgeWing(): RawTriGeometry {
        val verts = listOf(
            // Root airfoil diamond at x = -0.45
            Vector3(-0.45f,  0.14f,  0.42f), // 0 root leading top
            Vector3(-0.45f, -0.14f,  0.42f), // 1 root leading bottom
            Vector3(-0.45f,  0.06f, -0.48f), // 2 root trailing top
            Vector3(-0.45f, -0.06f, -0.48f), // 3 root trailing bottom
            // Swept wingtip wedge at x = +0.55 (swept back toward -Z, slight dihedral +Y)
            Vector3( 0.55f,  0.12f, -0.10f), // 4 tip leading top
            Vector3( 0.55f,  0.02f, -0.10f), // 5 tip leading bottom
            Vector3( 0.55f,  0.08f, -0.46f), // 6 tip trailing top
            Vector3( 0.55f,  0.02f, -0.46f)  // 7 tip trailing bottom
        )
        val uvs = listOf(
            UV(0f, 0f), UV(0f, 1f), UV(0.5f, 0f), UV(0.5f, 1f),
            UV(1f, 0f), UV(1f, 1f), UV(1f, 0.4f), UV(1f, 0.8f)
        )
        val tris = listOf(
            // Top wing surface
            Triple(0, 4, 6), Triple(0, 6, 2),
            // Bottom wing surface
            Triple(1, 3, 7), Triple(1, 7, 5),
            // Leading edge (+Z)
            Triple(0, 1, 5), Triple(0, 5, 4),
            // Trailing edge (-Z)
            Triple(2, 6, 7), Triple(2, 7, 3),
            // Root cap (-X)
            Triple(0, 2, 3), Triple(0, 3, 1),
            // Tip cap (+X)
            Triple(4, 5, 7), Triple(4, 7, 6)
        )
        return RawTriGeometry(verts, uvs, tris)
    }
}
