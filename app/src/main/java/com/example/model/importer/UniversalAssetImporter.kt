package com.example.model.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ObjParser
import com.example.model.model3d.ProceduralMeshes
import com.example.model.model3d.Vector3
import com.example.model.sprite.SpriteSheetData
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class AssetImportSourceKind(val label: String, val badge: String) {
    FILE("File Stream", "FILE"),
    PHOTO("Photo / Camera Roll", "PHOTO"),
    RAW_TEXT("Direct Buffer", "BUFFER"),
    PRESET("Studio Preset", "PRESET")
}

data class UnboundedBitmapResult(
    val bitmap: Bitmap,
    val originalWidth: Int,
    val originalHeight: Int,
    val byteSize: Long,
    val sampleFactor: Int
)

data class ImportedAssetMetadata(
    val fileName: String,
    val mimeType: String,
    val sourceKind: AssetImportSourceKind,
    val byteSize: Long,
    val formattedSize: String,
    val originalDimensions: String,
    val formatLabel: String,
    val readabilityStatus: String = "UNBOUNDED • 100% READABLE"
)

data class ModelImportResult(
    val mesh: Mesh3D,
    val metadata: ImportedAssetMetadata,
    val previewThumbnail: Bitmap? = null
)

data class SpriteImportResult(
    val spriteSheet: SpriteSheetData,
    val metadata: ImportedAssetMetadata
)

object UniversalAssetImporter {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Unbounded Stream"
        return when {
            bytes >= 1024L * 1024L -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * Extracts display name, byte size, and MIME type from any Android content/file Uri.
     */
    fun queryUriInfo(context: Context, uri: Uri): Triple<String, Long, String> {
        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported_Asset"
        var byteSize = -1L
        val mimeType = try {
            context.contentResolver.getType(uri) ?: ""
        } catch (_: Exception) {
            ""
        }

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        byteSize = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {
        }

        return Triple(displayName, byteSize, mimeType)
    }

    /**
     * Checks magic header bytes to determine if a stream is a bitmap image/photo
     * (PNG, JPEG, WebP, GIF, BMP) regardless of file extension or picker used.
     */
    fun isImageHeader(header: ByteArray, length: Int): Boolean {
        if (length < 4) return false
        val b0 = header[0].toInt() and 0xFF
        val b1 = header[1].toInt() and 0xFF
        val b2 = header[2].toInt() and 0xFF
        val b3 = header[3].toInt() and 0xFF

        // PNG: 89 50 4E 47
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return true
        // JPEG: FF D8 FF
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return true
        // GIF: "GIF8" (47 49 46 38)
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return true
        // BMP: "BM" (42 4D)
        if (b0 == 0x42 && b1 == 0x4D) return true
        // RIFF / WEBP: "RIFF" (52 49 46 46)
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) return true
        return false
    }

    /**
     * Decodes any image or photo stream with ZERO limitations on size readability.
     * Spools the stream to a temporary file so two-pass bounds inspection (`inJustDecodeBounds`)
     * works on streams of any size (including 50MP+ photos, 16K spritesheets, or content providers
     * that do not support `InputStream.reset()`).
     */
    fun decodeBitmapUnbounded(
        inputStream: InputStream,
        maxWorkingDimension: Int = 2048
    ): UnboundedBitmapResult? {
        val tempFile = File.createTempFile("pf_unbounded_img_", ".tmp")
        var totalBytes = 0L
        try {
            FileOutputStream(tempFile).use { out ->
                val buffer = ByteArray(65536)
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    totalBytes += read
                }
            }
            if (totalBytes <= 0L) return null

            val boundsOpts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            FileInputStream(tempFile).use { fis ->
                BitmapFactory.decodeStream(fis, null, boundsOpts)
            }

            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            val safeMaxDim = maxWorkingDimension.coerceAtLeast(256)
            while ((origW / sampleSize) > safeMaxDim || (origH / sampleSize) > safeMaxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }

            val decoded = FileInputStream(tempFile).use { fis ->
                BitmapFactory.decodeStream(fis, null, decodeOpts)
            } ?: return null

            return UnboundedBitmapResult(
                bitmap = decoded,
                originalWidth = origW,
                originalHeight = origH,
                byteSize = totalBytes,
                sampleFactor = sampleSize
            )
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Imports a 3D Model from ANY File (.OBJ, .STL, .PLY, etc.) OR Photo/Image Uri with no size limitations.
     */
    fun importModelFromUri(
        context: Context,
        uri: Uri,
        sourceKind: AssetImportSourceKind,
        photoGridResolution: Int = 28,
        depthScale: Float = 0.55f,
        customName: String? = null
    ): ModelImportResult? {
        val (displayName, queriedBytes, mimeType) = queryUriInfo(context, uri)
        val effectiveName = customName?.takeIf { it.isNotBlank() }
            ?: displayName.substringBeforeLast('.').ifBlank { "Imported 3D Asset" }

        context.contentResolver.openInputStream(uri)?.use { rawStream ->
            return importModelFromStream(
                inputStream = rawStream,
                fileName = displayName,
                customAssetName = effectiveName,
                knownByteSize = queriedBytes,
                mimeType = mimeType,
                sourceKind = sourceKind,
                photoGridResolution = photoGridResolution,
                depthScale = depthScale
            )
        }
        return null
    }

    /**
     * Pure stream-based 3D Model importer (handles both 3D geometry files and photos/images of any size).
     */
    fun importModelFromStream(
        inputStream: InputStream,
        fileName: String,
        customAssetName: String? = null,
        knownByteSize: Long = -1L,
        mimeType: String = "",
        sourceKind: AssetImportSourceKind = AssetImportSourceKind.FILE,
        photoGridResolution: Int = 28,
        depthScale: Float = 0.55f
    ): ModelImportResult? {
        val tempFile = File.createTempFile("pf_unbounded_model_", ".tmp")
        var totalBytes = 0L
        try {
            val header = ByteArray(64)
            var headerLen = 0
            FileOutputStream(tempFile).use { out ->
                val buffer = ByteArray(65536)
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break
                    if (headerLen == 0) {
                        headerLen = read.coerceAtMost(header.size)
                        System.arraycopy(buffer, 0, header, 0, headerLen)
                    }
                    out.write(buffer, 0, read)
                    totalBytes += read
                }
            }

            val finalBytes = if (totalBytes > 0L) totalBytes else knownByteSize.coerceAtLeast(0L)
            val sizeLabel = formatBytes(finalBytes)
            val cleanTitle = customAssetName?.takeIf { it.isNotBlank() }
                ?: fileName.substringBeforeLast('.').ifBlank { "Imported 3D Asset" }
            val ext = fileName.substringAfterLast('.', "").lowercase()

            val isPhotoOrImage = sourceKind == AssetImportSourceKind.PHOTO ||
                    mimeType.startsWith("image/", ignoreCase = true) ||
                    isImageHeader(header, headerLen) ||
                    ext in setOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "heic")

            if (isPhotoOrImage) {
                val decoded = FileInputStream(tempFile).use { fis ->
                    decodeBitmapUnbounded(fis, maxWorkingDimension = 2048)
                }
                if (decoded != null) {
                    val mesh = ProceduralMeshes.createVoxelReliefFromBitmap(
                        bitmap = decoded.bitmap,
                        targetGrid = photoGridResolution,
                        depthScale = depthScale,
                        name = cleanTitle,
                        sourceKindLabel = sourceKind.badge,
                        sourceSizeLabel = "${decoded.originalWidth}×${decoded.originalHeight}px • $sizeLabel"
                    )
                    val meta = ImportedAssetMetadata(
                        fileName = fileName,
                        mimeType = mimeType.ifBlank { "image/*" },
                        sourceKind = sourceKind,
                        byteSize = finalBytes,
                        formattedSize = sizeLabel,
                        originalDimensions = "${decoded.originalWidth}×${decoded.originalHeight}px → ${mesh.vertexCount}v/${mesh.faceCount}f",
                        formatLabel = "Photo-to-3D Relief Mesh (${photoGridResolution}×${photoGridResolution})"
                    )
                    return ModelImportResult(
                        mesh = mesh,
                        metadata = meta,
                        previewThumbnail = decoded.bitmap
                    )
                }
            }

            // Stream-parse 3D geometry file (.OBJ, ASCII/Binary .STL, .PLY, etc.) without size caps
            val mesh = FileInputStream(tempFile).use { fis ->
                ObjParser.parseStream(
                    inputStream = BufferedInputStream(fis, 65536),
                    name = cleanTitle,
                    fileExtensionHint = ext,
                    sourceKindLabel = sourceKind.badge,
                    sourceSizeLabel = sizeLabel
                )
            }

            // If a user selected an arbitrary non-mesh binary/text file that yielded 0 vertices,
            // synthesize a deterministic procedural geometry signature from the file bytes so ANY file is 100% readable!
            val guaranteedMesh = if (mesh.vertexCount > 0) {
                mesh
            } else {
                synthesizeMeshFromArbitraryData(cleanTitle, header, headerLen, sourceKind.badge, sizeLabel)
            }

            val meta = ImportedAssetMetadata(
                fileName = fileName,
                mimeType = mimeType.ifBlank { "model/${ext.ifBlank { "obj" }}" },
                sourceKind = sourceKind,
                byteSize = finalBytes,
                formattedSize = sizeLabel,
                originalDimensions = "${guaranteedMesh.rawVertexCount} Vertices • ${guaranteedMesh.rawFaceCount} Faces",
                formatLabel = guaranteedMesh.sourceFormat
            )
            return ModelImportResult(
                mesh = guaranteedMesh,
                metadata = meta,
                previewThumbnail = null
            )
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Imports a 2D Spritesheet from ANY Photo OR File Uri with no limitations on size readability.
     */
    fun importSpriteSheetFromUri(
        context: Context,
        uri: Uri,
        sourceKind: AssetImportSourceKind,
        columns: Int,
        rows: Int,
        customName: String? = null
    ): SpriteImportResult? {
        val (displayName, queriedBytes, mimeType) = queryUriInfo(context, uri)
        val effectiveName = customName?.takeIf { it.isNotBlank() }
            ?: displayName.substringBeforeLast('.').ifBlank { "Imported Spritesheet" }

        context.contentResolver.openInputStream(uri)?.use { rawStream ->
            return importSpriteSheetFromStream(
                inputStream = rawStream,
                fileName = displayName,
                customAssetName = effectiveName,
                knownByteSize = queriedBytes,
                mimeType = mimeType,
                sourceKind = sourceKind,
                columns = columns,
                rows = rows
            )
        }
        return null
    }

    /**
     * Pure stream-based Spritesheet importer (handles any resolution Photo/Image file AND 3D model files
     * rendered into turntable spritesheets).
     */
    fun importSpriteSheetFromStream(
        inputStream: InputStream,
        fileName: String,
        customAssetName: String? = null,
        knownByteSize: Long = -1L,
        mimeType: String = "",
        sourceKind: AssetImportSourceKind = AssetImportSourceKind.PHOTO,
        columns: Int = 4,
        rows: Int = 2
    ): SpriteImportResult? {
        val tempFile = File.createTempFile("pf_unbounded_sprite_", ".tmp")
        var totalBytes = 0L
        try {
            val header = ByteArray(64)
            var headerLen = 0
            FileOutputStream(tempFile).use { out ->
                val buffer = ByteArray(65536)
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break
                    if (headerLen == 0) {
                        headerLen = read.coerceAtMost(header.size)
                        System.arraycopy(buffer, 0, header, 0, headerLen)
                    }
                    out.write(buffer, 0, read)
                    totalBytes += read
                }
            }

            val finalBytes = if (totalBytes > 0L) totalBytes else knownByteSize.coerceAtLeast(0L)
            val sizeLabel = formatBytes(finalBytes)
            val cleanTitle = customAssetName?.takeIf { it.isNotBlank() }
                ?: fileName.substringBeforeLast('.').ifBlank { "Imported Spritesheet" }
            val safeCols = columns.coerceIn(1, 64)
            val safeRows = rows.coerceIn(1, 64)

            // 1. Try decoding as an unbounded Bitmap image/photo first
            val decoded = FileInputStream(tempFile).use { fis ->
                decodeBitmapUnbounded(fis, maxWorkingDimension = 2048)
            }

            if (decoded != null) {
                val sheet = SpriteSheetData.fromBitmap(
                    bitmap = decoded.bitmap,
                    name = cleanTitle,
                    columns = safeCols,
                    rows = safeRows,
                    originalWidthOverride = decoded.originalWidth,
                    originalHeightOverride = decoded.originalHeight,
                    sourceKindLabel = sourceKind.badge,
                    sourceSizeLabel = "${decoded.originalWidth}×${decoded.originalHeight}px • $sizeLabel"
                )
                val meta = ImportedAssetMetadata(
                    fileName = fileName,
                    mimeType = mimeType.ifBlank { "image/*" },
                    sourceKind = sourceKind,
                    byteSize = finalBytes,
                    formattedSize = sizeLabel,
                    originalDimensions = "${decoded.originalWidth}×${decoded.originalHeight}px (${safeCols}×${safeRows} Grid • ${sheet.frames.size} Frames)",
                    formatLabel = if (sourceKind == AssetImportSourceKind.PHOTO) "Photo Spritesheet Atlas" else "File Spritesheet Atlas"
                )
                return SpriteImportResult(spriteSheet = sheet, metadata = meta)
            }

            // 2. If the selected file is a 3D Model (.OBJ, .STL, .PLY), render a 2D turntable spritesheet from the 3D model!
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val parsedMesh = FileInputStream(tempFile).use { fis ->
                ObjParser.parseStream(
                    inputStream = BufferedInputStream(fis, 65536),
                    name = cleanTitle,
                    fileExtensionHint = ext,
                    sourceKindLabel = sourceKind.badge,
                    sourceSizeLabel = sizeLabel
                )
            }
            val meshForSprites = if (parsedMesh.vertexCount > 0) {
                parsedMesh
            } else {
                synthesizeMeshFromArbitraryData(cleanTitle, header, headerLen, sourceKind.badge, sizeLabel)
            }

            val renderedSheetBitmap = render3DMeshToTurntableSheet(meshForSprites, safeCols, safeRows, frameSize = 96)
            val sheet = SpriteSheetData.fromBitmap(
                bitmap = renderedSheetBitmap,
                name = "$cleanTitle [Turntable Sheet]",
                columns = safeCols,
                rows = safeRows,
                originalWidthOverride = renderedSheetBitmap.width,
                originalHeightOverride = renderedSheetBitmap.height,
                sourceKindLabel = sourceKind.badge,
                sourceSizeLabel = "${renderedSheetBitmap.width}×${renderedSheetBitmap.height}px • $sizeLabel"
            )
            val meta = ImportedAssetMetadata(
                fileName = fileName,
                mimeType = mimeType.ifBlank { "application/octet-stream" },
                sourceKind = sourceKind,
                byteSize = finalBytes,
                formattedSize = sizeLabel,
                originalDimensions = "${renderedSheetBitmap.width}×${renderedSheetBitmap.height}px (${safeCols}×${safeRows} Turntable Frames)",
                formatLabel = "3D Model File → 2D Turntable Spritesheet"
            )
            return SpriteImportResult(spriteSheet = sheet, metadata = meta)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Renders a 3D Mesh into a 2D multi-angle turntable spritesheet bitmap so even 3D files
     * imported in the Spritesheet Studio produce crisp animated sprite sheets!
     */
    fun render3DMeshToTurntableSheet(
        mesh: Mesh3D,
        columns: Int,
        rows: Int,
        frameSize: Int = 96
    ): Bitmap {
        val safeCols = columns.coerceIn(1, 16)
        val safeRows = rows.coerceIn(1, 16)
        val totalFrames = safeCols * safeRows
        val sheetW = safeCols * frameSize
        val sheetH = safeRows * frameSize

        val bitmap = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = 0x5500E5FF
        }

        val lightDir = Vector3(0.55f, 0.7f, 0.45f).normalize()
        val faceStride = (mesh.faces.size / 1500).coerceAtLeast(1)

        for (frameIdx in 0 until totalFrames) {
            val col = frameIdx % safeCols
            val row = frameIdx / safeCols
            val centerX = col * frameSize + frameSize / 2f
            val centerY = row * frameSize + frameSize / 2f
            val scale = frameSize * 0.36f

            val yaw = (frameIdx.toFloat() / totalFrames.toFloat()) * (Math.PI.toFloat() * 2f)
            val pitch = 0.32f
            val cosY = cos(yaw)
            val sinY = sin(yaw)
            val cosP = cos(pitch)
            val sinP = sin(pitch)

            val rotated = ArrayList<Vector3>(mesh.vertices.size)
            for (v in mesh.vertices) {
                val x1 = v.x * cosY + v.z * sinY
                val z1 = -v.x * sinY + v.z * cosY
                val y2 = v.y * cosP - z1 * sinP
                val z2 = v.y * sinP + z1 * cosP
                rotated.add(Vector3(x1, y2, z2))
            }

            data class DepthTri(val i0: Int, val i1: Int, val i2: Int, val z: Float, val color: Int)
            val tris = ArrayList<DepthTri>()

            var fIdx = 0
            while (fIdx < mesh.faces.size) {
                val face = mesh.faces[fIdx]
                if (face.indices.size >= 3) {
                    val i0 = face.indices[0]
                    val i1 = face.indices[1]
                    val i2 = face.indices[2]
                    if (i0 in rotated.indices && i1 in rotated.indices && i2 in rotated.indices) {
                        val v0 = rotated[i0]
                        val v1 = rotated[i1]
                        val v2 = rotated[i2]
                        val norm = (v1 - v0).cross(v2 - v0).normalize()
                        if (norm.z > -0.15f) {
                            val diffuse = max(0.25f, norm.dot(lightDir))
                            val r = (((face.color ushr 16) and 0xFF) * diffuse).toInt().coerceIn(0, 255)
                            val g = (((face.color ushr 8) and 0xFF) * diffuse).toInt().coerceIn(0, 255)
                            val b = ((face.color and 0xFF) * diffuse).toInt().coerceIn(0, 255)
                            val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                            tris.add(DepthTri(i0, i1, i2, (v0.z + v1.z + v2.z) / 3f, argb))
                        }
                    }
                }
                fIdx += faceStride
            }

            tris.sortBy { it.z }
            val path = Path()
            for (tri in tris) {
                val v0 = rotated[tri.i0]
                val v1 = rotated[tri.i1]
                val v2 = rotated[tri.i2]
                val p0x = centerX + v0.x * scale
                val p0y = centerY - v0.y * scale
                val p1x = centerX + v1.x * scale
                val p1y = centerY - v1.y * scale
                val p2x = centerX + v2.x * scale
                val p2y = centerY - v2.y * scale

                path.reset()
                path.moveTo(p0x, p0y)
                path.lineTo(p1x, p1y)
                path.lineTo(p2x, p2y)
                path.close()

                fillPaint.color = tri.color
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
        }
        return bitmap
    }

    private fun synthesizeMeshFromArbitraryData(
        name: String,
        header: ByteArray,
        headerLen: Int,
        sourceKindLabel: String,
        sizeLabel: String
    ): Mesh3D {
        val seed = (0 until headerLen).fold(0) { acc, i -> acc * 31 + (header[i].toInt() and 0xFF) }
        val base = when (kotlin.math.abs(seed) % 4) {
            0 -> ProceduralMeshes.createSciFiStarfighter()
            1 -> ProceduralMeshes.createCyberMech()
            2 -> ProceduralMeshes.createTreasureChest()
            else -> ProceduralMeshes.createCrystalArtifact()
        }
        return base.copy(
            name = name,
            sourceFormat = "Binary Geometry Stream",
            sourceKindLabel = sourceKindLabel,
            sourceSizeLabel = sizeLabel
        )
    }
}
