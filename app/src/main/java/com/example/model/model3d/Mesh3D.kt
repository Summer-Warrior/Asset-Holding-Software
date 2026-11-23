package com.example.model.model3d

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float): Vector3 = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else this

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0.00001f) Vector3(x / len, y / len, z / len) else Vector3(0f, 1f, 0f)
    }
}

data class UV(
    val u: Float,
    val v: Float
)

data class Face3D(
    val indices: IntArray,
    val uvIndices: IntArray? = null,
    val normal: Vector3? = null,
    val color: Long = 0xFF00E5FF,
    val materialName: String = "default"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Face3D) return false
        return indices.contentEquals(other.indices) &&
                (uvIndices contentEquals other.uvIndices) &&
                normal == other.normal &&
                color == other.color &&
                materialName == other.materialName
    }

    override fun hashCode(): Int {
        var result = indices.contentHashCode()
        result = 31 * result + (uvIndices?.contentHashCode() ?: 0)
        result = 31 * result + (normal?.hashCode() ?: 0)
        result = 31 * result + color.hashCode()
        result = 31 * result + materialName.hashCode()
        return result
    }
}

data class Material3D(
    val name: String,
    val diffuseColor: Long = 0xFF00E5FF,
    val specular: Float = 0.6f,
    val roughness: Float = 0.4f,
    val textureKey: String? = null
)

data class Mesh3D(
    val name: String,
    val vertices: List<Vector3>,
    val faces: List<Face3D>,
    val uvs: List<UV> = emptyList(),
    val materials: Map<String, Material3D> = emptyMap(),
    val boundsRadius: Float = 1f,
    val center: Vector3 = Vector3(0f, 0f, 0f),
    val sourceFormat: String = "Wavefront OBJ / Procedural",
    val sourceKindLabel: String = "STUDIO",
    val rawVertexCount: Int = vertices.size,
    val rawFaceCount: Int = faces.size,
    val sourceSizeLabel: String = "Unbounded Stream"
) {
    val vertexCount: Int get() = vertices.size
    val faceCount: Int get() = faces.size

    companion object {
        fun normalizeMesh(
            name: String,
            rawVertices: List<Vector3>,
            faces: List<Face3D>,
            uvs: List<UV> = emptyList(),
            materials: Map<String, Material3D> = emptyMap(),
            sourceFormat: String = "Wavefront OBJ / Procedural",
            sourceKindLabel: String = "STUDIO",
            rawVertexCountOverride: Int? = null,
            rawFaceCountOverride: Int? = null,
            sourceSizeLabel: String = "Unbounded Stream"
        ): Mesh3D {
            if (rawVertices.isEmpty()) {
                return Mesh3D(
                    name = name,
                    vertices = emptyList(),
                    faces = emptyList(),
                    sourceFormat = sourceFormat,
                    sourceKindLabel = sourceKindLabel,
                    rawVertexCount = rawVertexCountOverride ?: 0,
                    rawFaceCount = rawFaceCountOverride ?: 0,
                    sourceSizeLabel = sourceSizeLabel
                )
            }

            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            for (v in rawVertices) {
                if (v.x.isFinite() && v.y.isFinite() && v.z.isFinite()) {
                    if (v.x < minX) minX = v.x
                    if (v.y < minY) minY = v.y
                    if (v.z < minZ) minZ = v.z
                    if (v.x > maxX) maxX = v.x
                    if (v.y > maxY) maxY = v.y
                    if (v.z > maxZ) maxZ = v.z
                }
            }

            if (minX == Float.MAX_VALUE) {
                minX = -1f; maxX = 1f
                minY = -1f; maxY = 1f
                minZ = -1f; maxZ = 1f
            }

            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val centerZ = (minZ + maxZ) / 2f
            val extentX = maxX - minX
            val extentY = maxY - minY
            val extentZ = maxZ - minZ
            val maxExtent = max(extentX, max(extentY, extentZ)).coerceAtLeast(0.00001f)
            val scale = 2.0f / maxExtent // normalize inside unit sphere radius ~ 1.0

            val centeredVertices = ArrayList<Vector3>(rawVertices.size)
            for (v in rawVertices) {
                val sx = if (v.x.isFinite()) (v.x - centerX) * scale else 0f
                val sy = if (v.y.isFinite()) (v.y - centerY) * scale else 0f
                val sz = if (v.z.isFinite()) (v.z - centerZ) * scale else 0f
                centeredVertices.add(Vector3(sx, sy, sz))
            }

            val vertSize = centeredVertices.size
            // Compute face normals if missing and guard index bounds for arbitrarily formatted meshes
            val computedFaces = ArrayList<Face3D>(faces.size)
            for (face in faces) {
                if (face.indices.size < 3) continue
                val i0 = face.indices[0]
                val i1 = face.indices[1]
                val i2 = face.indices[2]
                if (i0 !in 0 until vertSize || i1 !in 0 until vertSize || i2 !in 0 until vertSize) continue

                if (face.normal != null) {
                    computedFaces.add(face)
                } else {
                    val v0 = centeredVertices[i0]
                    val v1 = centeredVertices[i1]
                    val v2 = centeredVertices[i2]
                    val edge1 = v1 - v0
                    val edge2 = v2 - v0
                    val norm = edge1.cross(edge2).normalize()
                    computedFaces.add(face.copy(normal = norm))
                }
            }

            return Mesh3D(
                name = name,
                vertices = centeredVertices,
                faces = computedFaces,
                uvs = uvs,
                materials = materials,
                boundsRadius = 1.2f,
                center = Vector3(0f, 0f, 0f),
                sourceFormat = sourceFormat,
                sourceKindLabel = sourceKindLabel,
                rawVertexCount = rawVertexCountOverride ?: centeredVertices.size,
                rawFaceCount = rawFaceCountOverride ?: computedFaces.size,
                sourceSizeLabel = sourceSizeLabel
            )
        }
    }
}
