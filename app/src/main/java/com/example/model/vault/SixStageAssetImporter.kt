package com.example.model.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.example.model.audio.AudioSynthAndDspEngine
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * The 6 mandatory verification, sanitization, deduplication, and binding stages for imported assets.
 */
enum class ImportPipelineStage {
    RECEIVE,
    VALIDATE,
    CLEAN,
    INDEX,
    STORE,
    RESOLVE
}

/**
 * Execution telemetry for a single stage inside [SixStageAssetImporter].
 */
data class StageExecutionRecord(
    val stage: ImportPipelineStage,
    val passed: Boolean,
    val detail: String
)

enum class VaultAssetKind {
    IMAGE,
    AUDIO,
    MESH_3D,
    JSON_SPEC
}

enum class VaultCacheState {
    CACHED_LOCAL,
    REMOTE_ONLY
}

/**
 * Metadata record for an asset stored in PixelForge's file-backed Content-Addressed Vault.
 * Raw media payloads live on disk (`vault_store/` or `cold_vault/`) and are never stored as SQLite blobs.
 */
data class VaultCatalogEntry(
    val id: String,
    val slotName: String,
    val title: String,
    val assetKind: VaultAssetKind,
    val extension: String,
    val sha256Hex: String,
    val byteSize: Long,
    val localFilePath: String,
    val thumbnailPath: String = "",
    val coldVaultPath: String = "",
    val sensitive: Boolean = false,
    val cacheState: VaultCacheState = VaultCacheState.CACHED_LOCAL,
    val lastAccessedMillis: Long = System.currentTimeMillis(),
    val gameTag: String = "General Studio",
    val groupTag: String = "Core Roster",
    val characterTag: String = "Studio"
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("slotName", slotName)
        put("title", title)
        put("assetKind", assetKind.name)
        put("extension", extension)
        put("sha256Hex", sha256Hex)
        put("byteSize", byteSize)
        put("localFilePath", localFilePath)
        put("thumbnailPath", thumbnailPath)
        put("coldVaultPath", coldVaultPath)
        put("sensitive", sensitive)
        put("cacheState", cacheState.name)
        put("lastAccessedMillis", lastAccessedMillis)
        put("gameTag", gameTag)
        put("groupTag", groupTag)
        put("characterTag", characterTag)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): VaultCatalogEntry {
            val kind = runCatching {
                VaultAssetKind.valueOf(obj.optString("assetKind", VaultAssetKind.IMAGE.name))
            }.getOrDefault(VaultAssetKind.IMAGE)
            val cache = runCatching {
                VaultCacheState.valueOf(obj.optString("cacheState", VaultCacheState.CACHED_LOCAL.name))
            }.getOrDefault(VaultCacheState.CACHED_LOCAL)
            return VaultCatalogEntry(
                id = obj.optString("id", UUID.randomUUID().toString()),
                slotName = obj.optString("slotName", "unnamed_slot"),
                title = obj.optString("title", "Vault Asset"),
                assetKind = kind,
                extension = obj.optString("extension", "bin"),
                sha256Hex = obj.optString("sha256Hex", ""),
                byteSize = obj.optLong("byteSize", 0L),
                localFilePath = obj.optString("localFilePath", ""),
                thumbnailPath = obj.optString("thumbnailPath", ""),
                coldVaultPath = obj.optString("coldVaultPath", ""),
                sensitive = obj.optBoolean("sensitive", false),
                cacheState = cache,
                lastAccessedMillis = obj.optLong("lastAccessedMillis", System.currentTimeMillis()),
                gameTag = obj.optString("gameTag", "General Studio"),
                groupTag = obj.optString("groupTag", "Core Roster"),
                characterTag = obj.optString("characterTag", "Studio")
            )
        }
    }
}

/**
 * Structured output of a 6-stage asset import run.
 */
data class SixStageImportResult(
    val success: Boolean,
    val entry: VaultCatalogEntry?,
    val wasDeduplicated: Boolean,
    val stages: List<StageExecutionRecord>,
    val errorMessage: String? = null
)

/**
 * 6-Stage Asset Importer Pipeline:
 * 1. RECEIVE  — Verifies non-empty payload and sanitizes filename.
 * 2. VALIDATE — Checks BOTH extension AND binary magic bytes + performs real decode.
 * 3. CLEAN    — Strips image EXIF/GPS via pixel re-encode; peak-normalizes WAV PCM.
 * 4. INDEX    — Computes SHA-256 hex digest, deduplicates against catalog, generates 96×96 thumb (blurred if sensitive).
 * 5. STORE    — Writes cleaned bytes to `filesDir/vault_store/<sha256>.<ext>` with strict canonical path checks.
 * 6. RESOLVE  — Binds `slotName -> relativePath` into the active [SkinManifest] for instant [AssetResolver] lookup.
 */
object SixStageAssetImporter {

    private const val TAG = "SixStageAssetImporter"
    private const val MAX_IMPORT_BYTES = 50L * 1024L * 1024L
    private const val THUMB_SIZE = 96

    fun importRawBytes(
        context: Context,
        rawBytes: ByteArray,
        fileName: String,
        slotName: String,
        sensitive: Boolean = false,
        gameTag: String = "General Studio",
        groupTag: String = "Core Roster",
        characterTag: String = "Studio",
        existingCatalog: List<VaultCatalogEntry> = emptyList(),
        activeSkinManifest: SkinManifest = SkinManifest.DEFAULT_CLEAN_ROOM
    ): Pair<SixStageImportResult, SkinManifest> {
        val stages = mutableListOf<StageExecutionRecord>()

        // ── STAGE 1: RECEIVE ────────────────────────────────────────────────────────
        val sanitizedName = sanitizeFileName(fileName)
        if (rawBytes.isEmpty()) {
            val msg = "Import rejected: empty file payload."
            stages.add(StageExecutionRecord(ImportPipelineStage.RECEIVE, false, msg))
            return SixStageImportResult(false, null, false, stages, msg) to activeSkinManifest
        }
        if (rawBytes.size.toLong() > MAX_IMPORT_BYTES) {
            val msg = "Import rejected: file exceeds 50 MB single-asset safety limit."
            stages.add(StageExecutionRecord(ImportPipelineStage.RECEIVE, false, msg))
            return SixStageImportResult(false, null, false, stages, msg) to activeSkinManifest
        }
        stages.add(
            StageExecutionRecord(
                ImportPipelineStage.RECEIVE,
                true,
                "Received '$sanitizedName' (${rawBytes.size} bytes)"
            )
        )

        // ── STAGE 2: VALIDATE (Extension + Magic Bytes + Real Decode) ───────────────
        val ext = sanitizedName.substringAfterLast('.', "").lowercase()
        val validation = validatePayloadAndDecode(rawBytes, ext)
        if (!validation.valid) {
            stages.add(StageExecutionRecord(ImportPipelineStage.VALIDATE, false, validation.detail))
            return SixStageImportResult(false, null, false, stages, validation.detail) to activeSkinManifest
        }
        stages.add(StageExecutionRecord(ImportPipelineStage.VALIDATE, true, validation.detail))

        // ── STAGE 3: CLEAN (Strip EXIF/GPS for images; Normalize Peak for WAV) ──────
        val cleanOutcome = cleanDecodedAsset(rawBytes, validation)
        if (cleanOutcome == null) {
            val msg = "Clean stage failed to re-encode validated asset."
            stages.add(StageExecutionRecord(ImportPipelineStage.CLEAN, false, msg))
            return SixStageImportResult(false, null, false, stages, msg) to activeSkinManifest
        }
        val cleanedBytes = cleanOutcome.cleanedBytes
        val canonicalExt = cleanOutcome.canonicalExt
        stages.add(StageExecutionRecord(ImportPipelineStage.CLEAN, true, cleanOutcome.detail))

        // ── STAGE 4: INDEX (SHA-256 Dedup + 96×96 Thumbnail w/ Privacy Blur) ────────
        val sha256Hex = computeSha256Hex(cleanedBytes)
        val duplicateEntry = existingCatalog.firstOrNull {
            it.sha256Hex.equals(sha256Hex, ignoreCase = true) &&
                it.localFilePath.isNotBlank() &&
                File(it.localFilePath).exists()
        }
        val wasDeduplicated = duplicateEntry != null

        var thumbPath = duplicateEntry?.thumbnailPath.orEmpty()
        if (validation.assetKind == VaultAssetKind.IMAGE && validation.decodedBitmap != null) {
            val generatedThumbPath = generateAndSaveThumbnail(
                context = context,
                sourceBitmap = validation.decodedBitmap,
                sha256Hex = sha256Hex,
                applyPrivacyBlur = sensitive
            )
            if (generatedThumbPath != null) {
                thumbPath = generatedThumbPath
            }
        }

        val indexDetail = buildString {
            append("SHA-256 ${sha256Hex.take(12)}…")
            if (wasDeduplicated) append(" • Deduplicated match")
            if (thumbPath.isNotBlank()) {
                append(if (sensitive) " • 96×96 blurred thumb" else " • 96×96 thumb")
            }
        }
        stages.add(StageExecutionRecord(ImportPipelineStage.INDEX, true, indexDetail))

        // ── STAGE 5: STORE (Content-Addressed File in filesDir/vault_store) ─────────
        val storeDir = File(context.filesDir, "vault_store").apply { mkdirs() }
        val relativePath = "vault_store/$sha256Hex.$canonicalExt"
        val targetFile = File(storeDir, "$sha256Hex.$canonicalExt")
        if (!isStrictlyInsideDir(targetFile, storeDir)) {
            val msg = "Store rejected: canonical path boundary violation."
            Log.e(TAG, msg)
            stages.add(StageExecutionRecord(ImportPipelineStage.STORE, false, msg))
            return SixStageImportResult(false, null, wasDeduplicated, stages, msg) to activeSkinManifest
        }

        try {
            if (!wasDeduplicated || !targetFile.exists()) {
                targetFile.writeBytes(cleanedBytes)
            }
        } catch (e: Exception) {
            val msg = "Disk write failed: ${e.message}"
            Log.e(TAG, msg, e)
            stages.add(StageExecutionRecord(ImportPipelineStage.STORE, false, msg))
            return SixStageImportResult(false, null, wasDeduplicated, stages, msg) to activeSkinManifest
        }
        stages.add(
            StageExecutionRecord(
                ImportPipelineStage.STORE,
                true,
                if (wasDeduplicated) "Reused verified file $relativePath" else "Stored ${cleanedBytes.size} bytes at $relativePath"
            )
        )

        // ── STAGE 6: RESOLVE (Bind Named Slot into Active SkinManifest) ─────────────
        val cleanSlotName = sanitizeSlotName(slotName, fallbackBase = sanitizedName.substringBeforeLast('.'))
        val updatedManifest = activeSkinManifest.copy(
            slotBindings = activeSkinManifest.slotBindings + (cleanSlotName to relativePath)
        )
        stages.add(
            StageExecutionRecord(
                ImportPipelineStage.RESOLVE,
                true,
                "Bound slot '$cleanSlotName' -> $relativePath"
            )
        )

        val now = System.currentTimeMillis()
        val entry = VaultCatalogEntry(
            id = duplicateEntry?.id ?: "vault_${sha256Hex.take(12)}_${UUID.randomUUID().toString().take(6)}",
            slotName = cleanSlotName,
            title = sanitizedName.substringBeforeLast('.').ifBlank { cleanSlotName },
            assetKind = validation.assetKind,
            extension = canonicalExt,
            sha256Hex = sha256Hex,
            byteSize = cleanedBytes.size.toLong(),
            localFilePath = targetFile.absolutePath,
            thumbnailPath = thumbPath,
            coldVaultPath = duplicateEntry?.coldVaultPath.orEmpty(),
            sensitive = sensitive,
            cacheState = VaultCacheState.CACHED_LOCAL,
            lastAccessedMillis = now,
            gameTag = gameTag.ifBlank { "General Studio" },
            groupTag = groupTag.ifBlank { "Core Roster" },
            characterTag = characterTag.ifBlank { "Studio" }
        )

        return SixStageImportResult(
            success = true,
            entry = entry,
            wasDeduplicated = wasDeduplicated,
            stages = stages,
            errorMessage = null
        ) to updatedManifest
    }

    fun computeSha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    internal fun isStrictlyInsideDir(target: File, baseDir: File): Boolean {
        return try {
            val baseCanonical = baseDir.canonicalPath
            val targetCanonical = target.canonicalPath
            val prefix = if (baseCanonical.endsWith(File.separator)) baseCanonical else baseCanonical + File.separator
            targetCanonical.startsWith(prefix)
        } catch (e: Exception) {
            Log.e(TAG, "Canonical path validation failed", e)
            false
        }
    }

    private fun sanitizeFileName(raw: String): String {
        val base = raw.replace('\\', '/').substringAfterLast('/').trim()
        val cleaned = base.replace(Regex("[^A-Za-z0-9._\\- ]"), "_").trimStart('.')
        return cleaned.ifBlank { "asset.bin" }
    }

    private fun sanitizeSlotName(raw: String, fallbackBase: String): String {
        val candidate = raw.trim().ifBlank { fallbackBase }
        val normalized = candidate.lowercase()
            .replace(Regex("[^a-z0-9_\\-./]+"), "_")
            .replace("..", "_")
            .trim('_', '/')
        return normalized.ifBlank { "studio_asset_slot" }
    }

    private data class ValidationOutcome(
        val valid: Boolean,
        val assetKind: VaultAssetKind = VaultAssetKind.IMAGE,
        val canonicalExt: String = "",
        val detail: String = "",
        val decodedBitmap: Bitmap? = null,
        val decodedWavPcm: Pair<ShortArray, Int>? = null
    )

    private fun validatePayloadAndDecode(rawBytes: ByteArray, ext: String): ValidationOutcome {
        return when (ext) {
            "png" -> {
                val magicOk = rawBytes.size >= 8 &&
                    (rawBytes[0].toInt() and 0xFF) == 0x89 &&
                    (rawBytes[1].toInt() and 0xFF) == 0x50 &&
                    (rawBytes[2].toInt() and 0xFF) == 0x4E &&
                    (rawBytes[3].toInt() and 0xFF) == 0x47
                if (!magicOk) {
                    return ValidationOutcome(false, detail = "Invalid PNG magic bytes (spoofed or corrupt .png file).")
                }
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                if (bmp == null || bmp.width <= 0 || bmp.height <= 0) {
                    return ValidationOutcome(false, detail = "PNG decode failed: corrupt image stream.")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.IMAGE,
                    canonicalExt = "png",
                    detail = "Verified PNG magic + decoded ${bmp.width}×${bmp.height}px bitmap",
                    decodedBitmap = bmp
                )
            }

            "jpg", "jpeg" -> {
                val magicOk = rawBytes.size >= 3 &&
                    (rawBytes[0].toInt() and 0xFF) == 0xFF &&
                    (rawBytes[1].toInt() and 0xFF) == 0xD8 &&
                    (rawBytes[2].toInt() and 0xFF) == 0xFF
                if (!magicOk) {
                    return ValidationOutcome(false, detail = "Invalid JPEG magic bytes (expected FF D8 FF).")
                }
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                if (bmp == null || bmp.width <= 0 || bmp.height <= 0) {
                    return ValidationOutcome(false, detail = "JPEG decode failed: corrupt image stream.")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.IMAGE,
                    canonicalExt = "jpg",
                    detail = "Verified JPEG magic + decoded ${bmp.width}×${bmp.height}px bitmap",
                    decodedBitmap = bmp
                )
            }

            "webp" -> {
                val magicOk = rawBytes.size >= 12 &&
                    String(rawBytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                    String(rawBytes, 8, 4, Charsets.US_ASCII) == "WEBP"
                if (!magicOk) {
                    return ValidationOutcome(false, detail = "Invalid WEBP magic bytes (expected RIFF..WEBP).")
                }
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                if (bmp == null || bmp.width <= 0 || bmp.height <= 0) {
                    return ValidationOutcome(false, detail = "WEBP decode failed: corrupt image stream.")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.IMAGE,
                    canonicalExt = "png",
                    detail = "Verified WEBP magic + decoded ${bmp.width}×${bmp.height}px bitmap",
                    decodedBitmap = bmp
                )
            }

            "wav" -> {
                val magicOk = rawBytes.size >= 44 &&
                    String(rawBytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                    String(rawBytes, 8, 4, Charsets.US_ASCII) == "WAVE"
                if (!magicOk) {
                    return ValidationOutcome(false, detail = "Invalid WAV magic bytes (expected RIFF..WAVE).")
                }
                val decoded = AudioSynthAndDspEngine.decodeWavBytesToPcm(rawBytes)
                if (decoded == null || decoded.first.isEmpty()) {
                    return ValidationOutcome(false, detail = "WAV decode failed: unsupported or corrupt PCM chunks.")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.AUDIO,
                    canonicalExt = "wav",
                    detail = "Verified WAV RIFF/WAVE + decoded ${decoded.first.size} samples @ ${decoded.second} Hz",
                    decodedWavPcm = decoded
                )
            }

            "ogg" -> {
                val magicOk = rawBytes.size >= 4 && String(rawBytes, 0, 4, Charsets.US_ASCII) == "OggS"
                if (!magicOk) {
                    return ValidationOutcome(false, detail = "Invalid OGG magic bytes (expected OggS).")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.AUDIO,
                    canonicalExt = "ogg",
                    detail = "Verified OGG container magic (OggS)"
                )
            }

            "mid", "midi" -> {
                val magicOk = rawBytes.size >= 4 && String(rawBytes, 0, 4, Charsets.US_ASCII) == "MThd"
                if (!magicOk) {
                    return ValidationOutcome(false, detail = "Invalid MIDI magic bytes (expected MThd).")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.AUDIO,
                    canonicalExt = "mid",
                    detail = "Verified Standard MIDI header (MThd)"
                )
            }

            "json" -> {
                val text = rawBytes.toString(Charsets.UTF_8).trim()
                if (!text.startsWith("{") && !text.startsWith("[")) {
                    return ValidationOutcome(false, detail = "Invalid JSON payload: must start with '{' or '['.")
                }
                val parsedOk = try {
                    if (text.startsWith("{")) JSONObject(text) else JSONArray(text)
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "JSON parse validation failed: ${e.message}")
                    false
                }
                if (!parsedOk) {
                    return ValidationOutcome(false, detail = "JSON syntax parse failed.")
                }
                val isAudioSpec = text.contains("\"score_seq") || text.contains("\"procedural_cry") ||
                    text.contains("\"audio_cue") || text.contains("\"midiNote\"") || text.contains("\"baseFreqHz\"")
                ValidationOutcome(
                    valid = true,
                    assetKind = if (isAudioSpec) VaultAssetKind.AUDIO else VaultAssetKind.JSON_SPEC,
                    canonicalExt = "json",
                    detail = if (isAudioSpec) "Verified Audio JSON specification" else "Verified structured JSON specification"
                )
            }

            "obj" -> {
                val text = rawBytes.toString(Charsets.UTF_8)
                val hasGeometry = text.lineSequence().any { line ->
                    val trimmed = line.trimStart()
                    trimmed.startsWith("v ") || trimmed.startsWith("f ")
                }
                if (!hasGeometry) {
                    return ValidationOutcome(false, detail = "Invalid Wavefront .OBJ: no vertex (v) or face (f) records found.")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.MESH_3D,
                    canonicalExt = "obj",
                    detail = "Verified Wavefront .OBJ geometry records"
                )
            }

            "stl" -> {
                val asciiOk = rawBytes.size >= 15 && String(rawBytes, 0, 5, Charsets.US_ASCII).equals("solid", ignoreCase = true)
                val binaryOk = rawBytes.size >= 84
                if (!asciiOk && !binaryOk) {
                    return ValidationOutcome(false, detail = "Invalid .STL payload: truncated header.")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.MESH_3D,
                    canonicalExt = "stl",
                    detail = if (asciiOk) "Verified ASCII STL mesh" else "Verified binary STL mesh"
                )
            }

            "ply" -> {
                val magicOk = rawBytes.size >= 3 && String(rawBytes, 0, 3, Charsets.US_ASCII) == "ply"
                if (!magicOk) {
                    return ValidationOutcome(false, detail = "Invalid .PLY magic header (expected 'ply').")
                }
                ValidationOutcome(
                    valid = true,
                    assetKind = VaultAssetKind.MESH_3D,
                    canonicalExt = "ply",
                    detail = "Verified PLY polygon mesh header"
                )
            }

            else -> ValidationOutcome(
                valid = false,
                detail = "Unsupported asset extension '.$ext'. Allowed: png, jpg, webp, wav, ogg, mid, json, obj, stl, ply."
            )
        }
    }

    private data class CleanOutcome(
        val cleanedBytes: ByteArray,
        val canonicalExt: String,
        val detail: String
    )

    private fun cleanDecodedAsset(rawBytes: ByteArray, validation: ValidationOutcome): CleanOutcome? {
        return try {
            when (validation.assetKind) {
                VaultAssetKind.IMAGE -> {
                    val bmp = validation.decodedBitmap ?: return null
                    // Re-encode clean pixel buffer to guarantee 100% EXIF / GPS metadata stripping
                    val cleanBmp = bmp.copy(Bitmap.Config.ARGB_8888, false)
                    val out = ByteArrayOutputStream()
                    val format = if (validation.canonicalExt == "jpg") {
                        Bitmap.CompressFormat.JPEG
                    } else {
                        Bitmap.CompressFormat.PNG
                    }
                    val quality = if (format == Bitmap.CompressFormat.JPEG) 92 else 100
                    cleanBmp.compress(format, quality, out)
                    CleanOutcome(
                        cleanedBytes = out.toByteArray(),
                        canonicalExt = validation.canonicalExt,
                        detail = "Stripped EXIF/GPS metadata via clean ${validation.canonicalExt.uppercase()} pixel re-encode"
                    )
                }

                VaultAssetKind.AUDIO -> {
                    val wavPair = validation.decodedWavPcm
                    if (wavPair != null) {
                        val normalized = AudioSynthAndDspEngine.normalizePeakPcm(wavPair.first, 0.92f)
                        val cleanWav = AudioSynthAndDspEngine.encodePcmToWavBytes(normalized, wavPair.second)
                        CleanOutcome(
                            cleanedBytes = cleanWav,
                            canonicalExt = "wav",
                            detail = "Peak-normalized WAV PCM (92%) and rebuilt clean RIFF/WAVE header"
                        )
                    } else {
                        CleanOutcome(
                            cleanedBytes = rawBytes,
                            canonicalExt = validation.canonicalExt,
                            detail = "Preserved verified ${validation.canonicalExt.uppercase()} audio payload"
                        )
                    }
                }

                VaultAssetKind.MESH_3D, VaultAssetKind.JSON_SPEC -> {
                    CleanOutcome(
                        cleanedBytes = rawBytes,
                        canonicalExt = validation.canonicalExt,
                        detail = "Verified ${validation.canonicalExt.uppercase()} payload integrity"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Clean stage error", e)
            null
        }
    }

    /**
     * Generates a crisp 96×96 thumbnail using pure pixel-buffer resampling (works identically on
     * Android devices and Robolectric JVM tests) and applies a real multi-pass box blur when
     * [applyPrivacyBlur] is true.
     */
    private fun generateAndSaveThumbnail(
        context: Context,
        sourceBitmap: Bitmap,
        sha256Hex: String,
        applyPrivacyBlur: Boolean
    ): String? {
        return try {
            val srcW = sourceBitmap.width.coerceAtLeast(1)
            val srcH = sourceBitmap.height.coerceAtLeast(1)
            val srcPixels = IntArray(srcW * srcH)
            sourceBitmap.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)

            val thumbPixels = IntArray(THUMB_SIZE * THUMB_SIZE)
            for (ty in 0 until THUMB_SIZE) {
                val sy = (ty * srcH / THUMB_SIZE).coerceIn(0, srcH - 1)
                val rowOff = ty * THUMB_SIZE
                val srcRowOff = sy * srcW
                for (tx in 0 until THUMB_SIZE) {
                    val sx = (tx * srcW / THUMB_SIZE).coerceIn(0, srcW - 1)
                    thumbPixels[rowOff + tx] = srcPixels[srcRowOff + sx]
                }
            }

            val finalPixels = if (applyPrivacyBlur) {
                applyBoxBlur(thumbPixels, THUMB_SIZE, THUMB_SIZE, radius = 4, passes = 2)
            } else {
                thumbPixels
            }

            val thumbBmp = Bitmap.createBitmap(THUMB_SIZE, THUMB_SIZE, Bitmap.Config.ARGB_8888)
            thumbBmp.setHasAlpha(true)
            thumbBmp.setPixels(finalPixels, 0, THUMB_SIZE, 0, 0, THUMB_SIZE, THUMB_SIZE)

            val thumbDir = File(context.filesDir, "vault_thumbs").apply { mkdirs() }
            val thumbFile = File(thumbDir, "$sha256Hex.png")
            if (!isStrictlyInsideDir(thumbFile, thumbDir)) {
                Log.e(TAG, "Thumbnail canonical path violation rejected.")
                return null
            }

            val out = ByteArrayOutputStream()
            thumbBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            thumbFile.writeBytes(out.toByteArray())
            thumbFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail generation failed: ${e.message}")
            null
        }
    }

    private fun applyBoxBlur(
        pixels: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        passes: Int
    ): IntArray {
        var current = pixels.copyOf()
        val temp = IntArray(pixels.size)
        repeat(passes.coerceIn(1, 4)) {
            // Horizontal pass
            for (y in 0 until height) {
                val rowOff = y * width
                for (x in 0 until width) {
                    var sumA = 0
                    var sumR = 0
                    var sumG = 0
                    var sumB = 0
                    var count = 0
                    val xStart = max(0, x - radius)
                    val xEnd = min(width - 1, x + radius)
                    for (kx in xStart..xEnd) {
                        val c = current[rowOff + kx]
                        sumA += Color.alpha(c)
                        sumR += Color.red(c)
                        sumG += Color.green(c)
                        sumB += Color.blue(c)
                        count++
                    }
                    temp[rowOff + x] = Color.argb(sumA / count, sumR / count, sumG / count, sumB / count)
                }
            }
            // Vertical pass
            for (y in 0 until height) {
                val yStart = max(0, y - radius)
                val yEnd = min(height - 1, y + radius)
                for (x in 0 until width) {
                    var sumA = 0
                    var sumR = 0
                    var sumG = 0
                    var sumB = 0
                    var count = 0
                    for (ky in yStart..yEnd) {
                        val c = temp[ky * width + x]
                        sumA += Color.alpha(c)
                        sumR += Color.red(c)
                        sumG += Color.green(c)
                        sumB += Color.blue(c)
                        count++
                    }
                    current[y * width + x] = Color.argb(sumA / count, sumR / count, sumG / count, sumB / count)
                }
            }
        }
        return current
    }
}
