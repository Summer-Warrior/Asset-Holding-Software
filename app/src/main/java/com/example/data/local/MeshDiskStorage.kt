package com.example.data.local

import android.content.Context
import java.io.File

/**
 * Disk storage manager for large Wavefront `.OBJ` meshes (> 256 KB) to prevent
 * Android SQLite 2 MB `CursorWindow` allocation crashes while maintaining strict
 * canonical path validation inside `context.filesDir/meshes`.
 */
object MeshDiskStorage {

    const val INLINE_OBJ_BYTE_THRESHOLD: Int = 256 * 1024 // 256 KB
    const val MESHES_SUBDIR: String = "meshes"

    fun getMeshesDir(context: Context): File {
        val dir = File(context.filesDir, MESHES_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Validates that [candidateFile] canonically resolves strictly inside [meshesDir]
     * and ends with `.obj`, preventing path traversal attacks.
     */
    fun isCanonicalMeshPathValid(meshesDir: File, candidateFile: File): Boolean {
        return try {
            val baseCanonical = meshesDir.canonicalFile
            val targetCanonical = candidateFile.canonicalFile
            val basePrefix = if (baseCanonical.path.endsWith(File.separator)) {
                baseCanonical.path
            } else {
                baseCanonical.path + File.separator
            }
            targetCanonical.path.startsWith(basePrefix) &&
                targetCanonical.name.endsWith(".obj", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns true when serialized `.OBJ` UTF-8 text exceeds the 256 KB inline SQLite threshold.
     */
    fun exceedsInlineThreshold(objMeshData: String?): Boolean {
        if (objMeshData.isNullOrEmpty()) return false
        return objMeshData.toByteArray(Charsets.UTF_8).size > INLINE_OBJ_BYTE_THRESHOLD
    }

    /**
     * Writes large `.OBJ` text to `File(context.filesDir, "meshes/${fileId}.obj")` after
     * canonical path validation. Returns the validated canonical file path, or empty string on failure.
     */
    fun writeLargeMeshObj(context: Context, fileId: String, objMeshData: String): String {
        val safeId = fileId.replace(Regex("[^A-Za-z0-9_\\-]"), "").ifBlank { "mesh_${System.currentTimeMillis()}" }
        val meshesDir = getMeshesDir(context)
        val targetFile = File(meshesDir, "$safeId.obj")
        if (!isCanonicalMeshPathValid(meshesDir, targetFile)) {
            throw SecurityException("Invalid canonical mesh path: ${targetFile.path}")
        }
        targetFile.writeText(objMeshData, Charsets.UTF_8)
        return targetFile.canonicalPath
    }

    /**
     * Reads `.OBJ` mesh text from [meshFilePath] if it exists and passes canonical path validation.
     * When [context] is supplied, validates containment strictly within `File(context.filesDir, "meshes")`.
     */
    fun readMeshObj(meshFilePath: String, context: Context? = null): String? {
        if (meshFilePath.isBlank()) return null
        return try {
            val candidate = File(meshFilePath)
            val valid = if (context != null) {
                isCanonicalMeshPathValid(getMeshesDir(context), candidate)
            } else {
                val canonical = candidate.canonicalFile
                val expectedSegment = "${File.separator}$MESHES_SUBDIR${File.separator}"
                canonical.path.contains(expectedSegment) &&
                    canonical.name.endsWith(".obj", ignoreCase = true) &&
                    !meshFilePath.contains("..")
            }
            if (!valid || !candidate.isFile) return null
            candidate.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deletes an offloaded `.OBJ` mesh file if it passes canonical path validation.
     */
    fun deleteMeshObj(meshFilePath: String, context: Context? = null): Boolean {
        if (meshFilePath.isBlank()) return false
        return try {
            val candidate = File(meshFilePath)
            val valid = if (context != null) {
                isCanonicalMeshPathValid(getMeshesDir(context), candidate)
            } else {
                val canonical = candidate.canonicalFile
                val expectedSegment = "${File.separator}$MESHES_SUBDIR${File.separator}"
                canonical.path.contains(expectedSegment) &&
                    canonical.name.endsWith(".obj", ignoreCase = true) &&
                    !meshFilePath.contains("..")
            }
            if (!valid || !candidate.exists()) return false
            candidate.delete()
        } catch (_: Exception) {
            false
        }
    }
}
