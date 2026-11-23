package com.example.model.model3d

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ObjParser {

    /**
     * Parses a Wavefront .OBJ, ASCII .STL, or ASCII .PLY string of any length without size caps.
     */
    fun parse(
        objContent: String,
        name: String = "ImportedModel",
        sourceKindLabel: String = "FILE",
        sourceSizeLabel: String = "Unbounded Stream"
    ): Mesh3D {
        val trimmedStart = objContent.trimStart()
        return when {
            trimmedStart.startsWith("solid ", ignoreCase = true) ||
                    trimmedStart.contains("facet normal", ignoreCase = true) -> {
                parseAsciiStlLines(
                    lines = objContent.lineSequence(),
                    name = name,
                    sourceKindLabel = sourceKindLabel,
                    sourceSizeLabel = sourceSizeLabel
                )
            }
            trimmedStart.startsWith("ply", ignoreCase = true) -> {
                parseAsciiPlyLines(
                    lines = objContent.lineSequence(),
                    name = name,
                    sourceKindLabel = sourceKindLabel,
                    sourceSizeLabel = sourceSizeLabel
                )
            }
            else -> {
                parseObjLines(
                    lines = objContent.lineSequence(),
                    name = name,
                    sourceKindLabel = sourceKindLabel,
                    sourceSizeLabel = sourceSizeLabel
                )
            }
        }
    }

    /**
     * Streams and parses a 3D model from an InputStream of ANY byte size without loading the entire
     * file into a single String. Supports Wavefront .OBJ, Binary & ASCII .STL, and Stanford .PLY.
     */
    fun parseStream(
        inputStream: InputStream,
        name: String = "ImportedModel",
        fileExtensionHint: String = "",
        sourceKindLabel: String = "FILE",
        sourceSizeLabel: String = "Unbounded Stream"
    ): Mesh3D {
        val buffered = if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream, 65536)
        buffered.mark(512)
        val headerBytes = ByteArray(256)
        val readCount = buffered.read(headerBytes)
        buffered.reset()

        if (readCount <= 0) {
            return Mesh3D(name, emptyList(), emptyList(), sourceFormat = "Empty Stream", sourceKindLabel = sourceKindLabel)
        }

        val headerAscii = String(headerBytes, 0, readCount.coerceAtMost(128), Charsets.ISO_8859_1).trimStart()
        val ext = fileExtensionHint.lowercase()

        // Detect Binary STL (80-byte header + 4-byte uint32 triangle count, not starting with ASCII "solid" + "facet")
        val looksLikeAsciiStl = headerAscii.startsWith("solid", ignoreCase = true) &&
                headerAscii.contains("facet", ignoreCase = true)
        val hasBinaryBytes = (0 until readCount.coerceAtMost(120)).any { i ->
            val b = headerBytes[i].toInt() and 0xFF
            b == 0
        }

        if ((ext == "stl" && !looksLikeAsciiStl) || (hasBinaryBytes && readCount >= 84)) {
            val binaryStlMesh = tryParseBinaryStl(buffered, name, sourceKindLabel, sourceSizeLabel)
            if (binaryStlMesh != null && binaryStlMesh.vertexCount > 0) {
                return binaryStlMesh
            }
        }

        val reader = BufferedReader(InputStreamReader(buffered, Charsets.UTF_8), 65536)
        return when {
            looksLikeAsciiStl || ext == "stl" -> {
                parseAsciiStlLines(reader.lineSequence(), name, sourceKindLabel, sourceSizeLabel)
            }
            headerAscii.startsWith("ply", ignoreCase = true) || ext == "ply" -> {
                parseAsciiPlyLines(reader.lineSequence(), name, sourceKindLabel, sourceSizeLabel)
            }
            else -> {
                parseObjLines(reader.lineSequence(), name, sourceKindLabel, sourceSizeLabel)
            }
        }
    }

    fun parseObjLines(
        lines: Sequence<String>,
        name: String = "ImportedModel",
        sourceKindLabel: String = "FILE",
        sourceSizeLabel: String = "Unbounded Stream"
    ): Mesh3D {
        val rawVertices = ArrayList<Vector3>(1024)
        val vertexColors = ArrayList<Long?>(1024)
        val rawUvs = ArrayList<UV>(512)
        val rawNormals = ArrayList<Vector3>(512)
        val faces = ArrayList<Face3D>(1024)
        var currentMaterial = "default"
        val materials = mutableMapOf<String, Material3D>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val tokens = line.split("\\s+".toRegex())
            if (tokens.isEmpty()) continue

            when (tokens[0]) {
                "v" -> {
                    if (tokens.size >= 4) {
                        val x = tokens[1].toFloatOrNull() ?: 0f
                        val y = tokens[2].toFloatOrNull() ?: 0f
                        val z = tokens[3].toFloatOrNull() ?: 0f
                        rawVertices.add(Vector3(x, y, z))

                        // Support optional per-vertex RGB color (v x y z r g b)
                        if (tokens.size >= 7) {
                            val rVal = tokens[4].toFloatOrNull() ?: 0f
                            val gVal = tokens[5].toFloatOrNull() ?: 0f
                            val bVal = tokens[6].toFloatOrNull() ?: 0f
                            val scale = if (rVal <= 1.0f && gVal <= 1.0f && bVal <= 1.0f) 255f else 1f
                            val r = (rVal * scale).toInt().coerceIn(0, 255)
                            val g = (gVal * scale).toInt().coerceIn(0, 255)
                            val b = (bVal * scale).toInt().coerceIn(0, 255)
                            val colorLong = (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
                            vertexColors.add(colorLong)
                        } else {
                            vertexColors.add(null)
                        }
                    }
                }
                "vt" -> {
                    if (tokens.size >= 3) {
                        val u = tokens[1].toFloatOrNull() ?: 0f
                        val v = tokens[2].toFloatOrNull() ?: 0f
                        rawUvs.add(UV(u, v))
                    }
                }
                "vn" -> {
                    if (tokens.size >= 4) {
                        val nx = tokens[1].toFloatOrNull() ?: 0f
                        val ny = tokens[2].toFloatOrNull() ?: 0f
                        val nz = tokens[3].toFloatOrNull() ?: 0f
                        rawNormals.add(Vector3(nx, ny, nz).normalize())
                    }
                }
                "usemtl" -> {
                    if (tokens.size >= 2) {
                        currentMaterial = tokens[1]
                    }
                }
                "f" -> {
                    if (tokens.size >= 4) {
                        val faceVertIndices = IntArray(tokens.size - 1)
                        var validVertCount = 0
                        val faceUvIndices = IntArray(tokens.size - 1)
                        var validUvCount = 0
                        var normalFromObj: Vector3? = null

                        for (i in 1 until tokens.size) {
                            val part = tokens[i]
                            val slash1 = part.indexOf('/')
                            if (slash1 < 0) {
                                val vIdx = part.toIntOrNull()
                                if (vIdx != null) {
                                    val actualVIdx = if (vIdx > 0) vIdx - 1 else rawVertices.size + vIdx
                                    if (actualVIdx in 0 until rawVertices.size) {
                                        faceVertIndices[validVertCount++] = actualVIdx
                                    }
                                }
                            } else {
                                val vStr = part.substring(0, slash1)
                                val vIdx = vStr.toIntOrNull()
                                if (vIdx != null) {
                                    val actualVIdx = if (vIdx > 0) vIdx - 1 else rawVertices.size + vIdx
                                    if (actualVIdx in 0 until rawVertices.size) {
                                        faceVertIndices[validVertCount++] = actualVIdx
                                    }
                                }
                                val slash2 = part.indexOf('/', slash1 + 1)
                                val vtStr = if (slash2 < 0) part.substring(slash1 + 1) else part.substring(slash1 + 1, slash2)
                                if (vtStr.isNotEmpty()) {
                                    val vtIdx = vtStr.toIntOrNull()
                                    if (vtIdx != null) {
                                        val actualVtIdx = if (vtIdx > 0) vtIdx - 1 else rawUvs.size + vtIdx
                                        if (actualVtIdx in 0 until rawUvs.size) {
                                            faceUvIndices[validUvCount++] = actualVtIdx
                                        }
                                    }
                                }
                                if (slash2 >= 0 && slash2 + 1 < part.length) {
                                    val vnStr = part.substring(slash2 + 1)
                                    val vnIdx = vnStr.toIntOrNull()
                                    if (vnIdx != null) {
                                        val actualVnIdx = if (vnIdx > 0) vnIdx - 1 else rawNormals.size + vnIdx
                                        if (actualVnIdx in 0 until rawNormals.size) {
                                            normalFromObj = rawNormals[actualVnIdx]
                                        }
                                    }
                                }
                            }
                        }

                        // Fan triangulation for polygons with any number of vertices >= 3
                        if (validVertCount >= 3) {
                            val v0Idx = faceVertIndices[0]
                            val customVertColor = vertexColors.getOrNull(v0Idx)
                            for (t in 1 until validVertCount - 1) {
                                val triIndices = intArrayOf(
                                    v0Idx,
                                    faceVertIndices[t],
                                    faceVertIndices[t + 1]
                                )
                                val triUvs = if (validUvCount == validVertCount) {
                                    intArrayOf(faceUvIndices[0], faceUvIndices[t], faceUvIndices[t + 1])
                                } else null

                                val faceColor = customVertColor ?: when ((faces.size % 4)) {
                                    0 -> 0xFF00E5FF
                                    1 -> 0xFF38BDF8
                                    2 -> 0xFF0284C7
                                    else -> 0xFF00B4D8
                                }

                                faces.add(
                                    Face3D(
                                        indices = triIndices,
                                        uvIndices = triUvs,
                                        normal = normalFromObj,
                                        color = faceColor,
                                        materialName = currentMaterial
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return Mesh3D.normalizeMesh(
            name = name,
            rawVertices = rawVertices,
            faces = faces,
            uvs = rawUvs,
            materials = materials,
            sourceFormat = "Wavefront .OBJ",
            sourceKindLabel = sourceKindLabel,
            rawVertexCountOverride = rawVertices.size,
            rawFaceCountOverride = faces.size,
            sourceSizeLabel = sourceSizeLabel
        )
    }

    /**
     * Parses ASCII .STL files of any size into a normalized Mesh3D.
     */
    fun parseAsciiStlLines(
        lines: Sequence<String>,
        name: String = "ImportedSTL",
        sourceKindLabel: String = "FILE",
        sourceSizeLabel: String = "Unbounded Stream"
    ): Mesh3D {
        val rawVertices = ArrayList<Vector3>(1024)
        val faces = ArrayList<Face3D>(512)
        var currentNormal: Vector3? = null
        val pendingTriVerts = ArrayList<Int>(3)

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val tokens = line.split("\\s+".toRegex())
            if (tokens.isEmpty()) continue

            when (tokens[0].lowercase()) {
                "facet" -> {
                    if (tokens.size >= 5 && tokens[1].equals("normal", ignoreCase = true)) {
                        val nx = tokens[2].toFloatOrNull() ?: 0f
                        val ny = tokens[3].toFloatOrNull() ?: 0f
                        val nz = tokens[4].toFloatOrNull() ?: 0f
                        currentNormal = Vector3(nx, ny, nz).normalize()
                    }
                    pendingTriVerts.clear()
                }
                "vertex" -> {
                    if (tokens.size >= 4) {
                        val x = tokens[1].toFloatOrNull() ?: 0f
                        val y = tokens[2].toFloatOrNull() ?: 0f
                        val z = tokens[3].toFloatOrNull() ?: 0f
                        val idx = rawVertices.size
                        rawVertices.add(Vector3(x, y, z))
                        pendingTriVerts.add(idx)
                        if (pendingTriVerts.size == 3) {
                            val faceColor = when (faces.size % 3) {
                                0 -> 0xFF00E5FF
                                1 -> 0xFF38BDF8
                                else -> 0xFF0284C7
                            }
                            faces.add(
                                Face3D(
                                    indices = intArrayOf(pendingTriVerts[0], pendingTriVerts[1], pendingTriVerts[2]),
                                    normal = currentNormal,
                                    color = faceColor
                                )
                            )
                            pendingTriVerts.clear()
                        }
                    }
                }
            }
        }

        return Mesh3D.normalizeMesh(
            name = name,
            rawVertices = rawVertices,
            faces = faces,
            sourceFormat = "ASCII .STL Stereolithography",
            sourceKindLabel = sourceKindLabel,
            rawVertexCountOverride = rawVertices.size,
            rawFaceCountOverride = faces.size,
            sourceSizeLabel = sourceSizeLabel
        )
    }

    /**
     * Parses Binary .STL files of any triangle count into a normalized Mesh3D.
     */
    private fun tryParseBinaryStl(
        inputStream: InputStream,
        name: String,
        sourceKindLabel: String,
        sourceSizeLabel: String
    ): Mesh3D? {
        return try {
            val header = ByteArray(80)
            if (inputStream.read(header) < 80) return null
            val countBytes = ByteArray(4)
            if (inputStream.read(countBytes) < 4) return null
            val triCount = ByteBuffer.wrap(countBytes).order(ByteOrder.LITTLE_ENDIAN).int
            if (triCount <= 0 || triCount > 5_000_000) return null

            val rawVertices = ArrayList<Vector3>((triCount * 3).coerceAtMost(300_000))
            val faces = ArrayList<Face3D>(triCount.coerceAtMost(100_000))
            val recordBuf = ByteArray(50)
            val bb = ByteBuffer.wrap(recordBuf).order(ByteOrder.LITTLE_ENDIAN)

            for (t in 0 until triCount) {
                var readTotal = 0
                while (readTotal < 50) {
                    val r = inputStream.read(recordBuf, readTotal, 50 - readTotal)
                    if (r <= 0) break
                    readTotal += r
                }
                if (readTotal < 50) break

                bb.position(0)
                val nx = bb.float
                val ny = bb.float
                val nz = bb.float
                val v0 = Vector3(bb.float, bb.float, bb.float)
                val v1 = Vector3(bb.float, bb.float, bb.float)
                val v2 = Vector3(bb.float, bb.float, bb.float)

                val baseIdx = rawVertices.size
                rawVertices.add(v0)
                rawVertices.add(v1)
                rawVertices.add(v2)

                val norm = if (nx != 0f || ny != 0f || nz != 0f) Vector3(nx, ny, nz).normalize() else null
                val faceColor = when (t % 3) {
                    0 -> 0xFF00E5FF
                    1 -> 0xFF38BDF8
                    else -> 0xFF0284C7
                }
                faces.add(Face3D(intArrayOf(baseIdx, baseIdx + 1, baseIdx + 2), normal = norm, color = faceColor))
            }

            Mesh3D.normalizeMesh(
                name = name,
                rawVertices = rawVertices,
                faces = faces,
                sourceFormat = "Binary .STL Stereolithography",
                sourceKindLabel = sourceKindLabel,
                rawVertexCountOverride = rawVertices.size,
                rawFaceCountOverride = faces.size,
                sourceSizeLabel = sourceSizeLabel
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses Stanford .PLY (ASCII Polygon File Format) of any size into a normalized Mesh3D.
     */
    fun parseAsciiPlyLines(
        lines: Sequence<String>,
        name: String = "ImportedPLY",
        sourceKindLabel: String = "FILE",
        sourceSizeLabel: String = "Unbounded Stream"
    ): Mesh3D {
        val rawVertices = ArrayList<Vector3>(1024)
        val faces = ArrayList<Face3D>(1024)
        var inHeader = true
        var expectedVertices = 0
        var expectedFaces = 0
        var readVertices = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("comment")) continue
            val tokens = line.split("\\s+".toRegex())

            if (inHeader) {
                if (tokens[0].equals("element", ignoreCase = true) && tokens.size >= 3) {
                    when (tokens[1].lowercase()) {
                        "vertex" -> expectedVertices = tokens[2].toIntOrNull() ?: 0
                        "face" -> expectedFaces = tokens[2].toIntOrNull() ?: 0
                    }
                } else if (tokens[0].equals("end_header", ignoreCase = true)) {
                    inHeader = false
                }
                continue
            }

            if (readVertices < expectedVertices) {
                if (tokens.size >= 3) {
                    val x = tokens[0].toFloatOrNull() ?: 0f
                    val y = tokens[1].toFloatOrNull() ?: 0f
                    val z = tokens[2].toFloatOrNull() ?: 0f
                    rawVertices.add(Vector3(x, y, z))
                }
                readVertices++
            } else if (faces.size < expectedFaces.coerceAtLeast(1_000_000)) {
                val count = tokens[0].toIntOrNull() ?: 0
                if (count >= 3 && tokens.size >= count + 1) {
                    val idx0 = tokens[1].toIntOrNull() ?: 0
                    for (t in 1 until count - 1) {
                        val idx1 = tokens[1 + t].toIntOrNull() ?: 0
                        val idx2 = tokens[2 + t].toIntOrNull() ?: 0
                        if (idx0 in 0 until rawVertices.size &&
                            idx1 in 0 until rawVertices.size &&
                            idx2 in 0 until rawVertices.size
                        ) {
                            val faceColor = when (faces.size % 3) {
                                0 -> 0xFF00E5FF
                                1 -> 0xFF38BDF8
                                else -> 0xFF0284C7
                            }
                            faces.add(Face3D(intArrayOf(idx0, idx1, idx2), color = faceColor))
                        }
                    }
                }
            }
        }

        return Mesh3D.normalizeMesh(
            name = name,
            rawVertices = rawVertices,
            faces = faces,
            sourceFormat = "Stanford .PLY Polygon Mesh",
            sourceKindLabel = sourceKindLabel,
            rawVertexCountOverride = rawVertices.size,
            rawFaceCountOverride = faces.size,
            sourceSizeLabel = sourceSizeLabel
        )
    }

    fun exportToObj(mesh: Mesh3D): String {
        val sb = StringBuilder()
        sb.appendLine("# PixelForge Studio 3D Model Export")
        sb.appendLine("# Model: ${mesh.name}")
        sb.appendLine("# Vertices: ${mesh.vertices.size} (Raw Source: ${mesh.rawVertexCount})")
        sb.appendLine("# Faces: ${mesh.faces.size} (Raw Source: ${mesh.rawFaceCount})")
        sb.appendLine("# Source: ${mesh.sourceFormat} [${mesh.sourceKindLabel}] (${mesh.sourceSizeLabel})")
        sb.appendLine()

        for (v in mesh.vertices) {
            sb.appendLine("v ${String.format("%.4f", v.x)} ${String.format("%.4f", v.y)} ${String.format("%.4f", v.z)}")
        }

        sb.appendLine()
        for (uv in mesh.uvs) {
            sb.appendLine("vt ${String.format("%.4f", uv.u)} ${String.format("%.4f", uv.v)}")
        }

        sb.appendLine()
        for (face in mesh.faces) {
            face.normal?.let { n ->
                sb.appendLine("vn ${String.format("%.4f", n.x)} ${String.format("%.4f", n.y)} ${String.format("%.4f", n.z)}")
            }
        }

        sb.appendLine()
        sb.appendLine("s 1")
        var normIndex = 1
        for (face in mesh.faces) {
            val i0 = face.indices[0] + 1
            val i1 = face.indices[1] + 1
            val i2 = face.indices[2] + 1

            if (face.uvIndices != null && face.uvIndices.size == 3 && face.normal != null) {
                val uv0 = face.uvIndices[0] + 1
                val uv1 = face.uvIndices[1] + 1
                val uv2 = face.uvIndices[2] + 1
                sb.appendLine("f $i0/$uv0/$normIndex $i1/$uv1/$normIndex $i2/$uv2/$normIndex")
                normIndex++
            } else if (face.uvIndices != null && face.uvIndices.size == 3) {
                val uv0 = face.uvIndices[0] + 1
                val uv1 = face.uvIndices[1] + 1
                val uv2 = face.uvIndices[2] + 1
                sb.appendLine("f $i0/$uv0 $i1/$uv1 $i2/$uv2")
            } else if (face.normal != null) {
                sb.appendLine("f $i0//$normIndex $i1//$normIndex $i2//$normIndex")
                normIndex++
            } else {
                sb.appendLine("f $i0 $i1 $i2")
            }
        }

        return sb.toString()
    }
}
