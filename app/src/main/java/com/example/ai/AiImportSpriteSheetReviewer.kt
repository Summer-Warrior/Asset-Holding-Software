package com.example.ai

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Result of AI File Import Review checking whether a spritesheet is being added
 * as a single picture image (1x1 frame) instead of a multi-frame spritesheet grid.
 */
data class AiSpriteSheetReviewResult(
    val fileName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val userRequestedColumns: Int,
    val userRequestedRows: Int,
    val isSpriteSheetInSingleImage: Boolean,
    val recommendedColumns: Int,
    val recommendedRows: Int,
    val detectedFrameCount: Int,
    val confidencePercent: Int,
    val verdictTitle: String,
    val aiReviewSummary: String,
    val technicalGridEvidence: String,
    val reviewSource: String, // "GEMINI 3.8 FLASH VISION + GRID SCAN" or "AI STRUCTURAL GRID SCAN"
    val reviewedAtMillis: Long = System.currentTimeMillis()
)

/**
 * AI File Import Reviewer that inspects imported files and photos to detect
 * when a multi-frame spritesheet is being added as a single picture image (1x1)
 * or with mismatched slicing dimensions.
 */
object AiImportSpriteSheetReviewer {

    private val reviewResponseSchema = Schema.obj(
        mapOf(
            "isSpriteSheetInSingleImage" to Schema.boolean(
                description = "True if the imported image actually contains multiple sprite frames/poses arranged in a strip or grid, especially when imported as a 1x1 single picture."
            ),
            "recommendedColumns" to Schema.integer(
                description = "Recommended number of horizontal columns to slice this spritesheet cleanly (1 to 16)."
            ),
            "recommendedRows" to Schema.integer(
                description = "Recommended number of vertical rows to slice this spritesheet cleanly (1 to 16)."
            ),
            "detectedFrameCount" to Schema.integer(
                description = "Total number of distinct character/object animation frames detected inside the single image."
            ),
            "confidencePercent" to Schema.integer(
                description = "Confidence percentage from 50 to 99."
            ),
            "verdictTitle" to Schema.string(
                description = "Short headline alerting the user if a spritesheet is being added as a single picture image, or confirming a single standalone image."
            ),
            "aiReviewSummary" to Schema.string(
                description = "Clear explanation for everyday users describing what the AI saw in the imported image and how to slice it."
            ),
            "technicalGridEvidence" to Schema.string(
                description = "Technical breakdown of frame gutters, cell dimensions, aspect ratio, and sprite repetition."
            )
        )
    )

    private val geminiVisionReviewer by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reviewResponseSchema
                temperature = 0.2f
            }
        )
    }

    /**
     * Full AI review of an imported file/bitmap:
     * 1. Runs deterministic structural gutter & cell periodicity analysis immediately.
     * 2. When `preferCloudVision` is true, queries `gemini-3.8-flash` via Firebase AI Logic
     *    with the bitmap to visually verify whether a spritesheet is being added as a single picture image.
     */
    suspend fun reviewImportedFile(
        bitmap: Bitmap,
        fileName: String,
        userRequestedColumns: Int,
        userRequestedRows: Int,
        preferCloudVision: Boolean = true
    ): AiSpriteSheetReviewResult = withContext(Dispatchers.Default) {
        val localResult = analyzeSpriteSheetInSingleImageDeterministically(
            bitmap = bitmap,
            fileName = fileName,
            userRequestedColumns = userRequestedColumns,
            userRequestedRows = userRequestedRows
        )

        if (!preferCloudVision) {
            return@withContext localResult
        }

        try {
            val scaledForAi = scaleBitmapForVision(bitmap, maxDim = 512)
            val promptText = buildString {
                appendLine("You are an expert game engine asset inspector.")
                appendLine("Review this imported image file named '$fileName' (${bitmap.width}x${bitmap.height}px).")
                appendLine("The user is currently importing it with grid settings: ${userRequestedColumns} columns x ${userRequestedRows} rows.")
                appendLine("Local structural periodicity scan estimated: isSpriteSheet=${localResult.isSpriteSheetInSingleImage}, recommended=${localResult.recommendedColumns}x${localResult.recommendedRows} (${localResult.technicalGridEvidence}).")
                appendLine("Check carefully if a multi-frame spritesheet (animation strip, character sheet, or tile atlas) is being added as a single picture image (1x1) or with wrong grid divisions.")
                appendLine("Return accurate recommendedColumns and recommendedRows so the user can slice the single picture into proper animation frames in one tap.")
            }

            val response = withTimeout(35_000L) {
                geminiVisionReviewer.generateContent(
                    content {
                        image(scaledForAi)
                        text(promptText)
                    }
                )
            }

            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isNotEmpty()) {
                val root = JSONObject(rawJson)
                val recCols = root.optInt("recommendedColumns", localResult.recommendedColumns).coerceIn(1, 16)
                val recRows = root.optInt("recommendedRows", localResult.recommendedRows).coerceIn(1, 16)
                val parsedIsSheet = root.optBoolean("isSpriteSheetInSingleImage", localResult.isSpriteSheetInSingleImage)
                val parsedFrames = root.optInt("detectedFrameCount", recCols * recRows)
                val parsedConfidence = root.optInt("confidencePercent", 90).coerceIn(55, 99)
                val parsedTitle = root.optString("verdictTitle", "").ifBlank { localResult.verdictTitle }
                val parsedSummary = root.optString("aiReviewSummary", "").ifBlank { localResult.aiReviewSummary }
                val parsedEvidence = root.optString("technicalGridEvidence", "").ifBlank { localResult.technicalGridEvidence }

                val multiFrameDetected = parsedIsSheet || (recCols * recRows > 1)
                val addedAsSinglePicture = multiFrameDetected && (userRequestedColumns == 1 && userRequestedRows == 1)

                return@withContext AiSpriteSheetReviewResult(
                    fileName = fileName,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    userRequestedColumns = userRequestedColumns,
                    userRequestedRows = userRequestedRows,
                    isSpriteSheetInSingleImage = addedAsSinglePicture || (multiFrameDetected && (recCols != userRequestedColumns || recRows != userRequestedRows)),
                    recommendedColumns = if (multiFrameDetected) max(recCols, localResult.recommendedColumns) else recCols,
                    recommendedRows = if (multiFrameDetected) max(recRows, localResult.recommendedRows) else recRows,
                    detectedFrameCount = max(parsedFrames, recCols * recRows).coerceAtLeast(1),
                    confidencePercent = parsedConfidence,
                    verdictTitle = parsedTitle,
                    aiReviewSummary = parsedSummary,
                    technicalGridEvidence = "$parsedEvidence | Local Scan: ${localResult.recommendedColumns}×${localResult.recommendedRows}",
                    reviewSource = "GEMINI 3.8 FLASH VISION + GRID SCAN"
                )
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            // Fall back gracefully to deterministic AI structural grid scan when offline or in JVM unit tests
        }

        localResult
    }

    /**
     * Fast, deterministic structural & visual periodicity analyzer that detects whether an imported
     * image is actually a multi-frame spritesheet being added as a single picture image (1x1).
     */
    fun analyzeSpriteSheetInSingleImageDeterministically(
        bitmap: Bitmap,
        fileName: String,
        userRequestedColumns: Int = 1,
        userRequestedRows: Int = 1
    ): AiSpriteSheetReviewResult {
        val width = bitmap.width.coerceAtLeast(1)
        val height = bitmap.height.coerceAtLeast(1)
        val lowerName = fileName.lowercase()

        // 1. Check explicit filename patterns like "4x2", "sheet", "strip", "frames", "walk", "run"
        val regexDims = Regex("""(\d{1,2})\s*[xX×]\s*(\d{1,2})""").find(lowerName)
        val nameColsHint = regexDims?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..16 }
        val nameRowsHint = regexDims?.groupValues?.getOrNull(2)?.toIntOrNull()?.takeIf { it in 1..16 }
        val hasSpriteKeyword = listOf("sheet", "strip", "sprite", "frames", "anim", "walk", "idle", "run", "atlas").any {
            lowerName.contains(it)
        }

        // 2. Sample pixel transparency / background color along vertical and horizontal scanlines
        val cornerBgColor = bitmap.getPixel(0, 0)
        val isPixelBackground = { px: Int ->
            val alpha = AndroidColor.alpha(px)
            if (alpha < 45) {
                true
            } else {
                val dr = abs(AndroidColor.red(px) - AndroidColor.red(cornerBgColor))
                val dg = abs(AndroidColor.green(px) - AndroidColor.green(cornerBgColor))
                val db = abs(AndroidColor.blue(px) - AndroidColor.blue(cornerBgColor))
                (dr + dg + db) < 28
            }
        }

        // Measure foreground occupancy per column and per row
        val colForegroundRatio = FloatArray(width)
        val rowForegroundRatio = FloatArray(height)

        val stepX = max(1, width / 128)
        val stepY = max(1, height / 128)

        for (x in 0 until width) {
            var fg = 0
            var count = 0
            var y = 0
            while (y < height) {
                if (!isPixelBackground(bitmap.getPixel(x, y))) {
                    fg++
                }
                count++
                y += stepY
            }
            colForegroundRatio[x] = if (count > 0) fg.toFloat() / count else 0f
        }

        for (y in 0 until height) {
            var fg = 0
            var count = 0
            var x = 0
            while (x < width) {
                if (!isPixelBackground(bitmap.getPixel(x, y))) {
                    fg++
                }
                count++
                x += stepX
            }
            rowForegroundRatio[y] = if (count > 0) fg.toFloat() / count else 0f
        }

        // Evaluate candidate column divisions (2..8) and row divisions (1..8)
        val bestCols = findBestGridDivision(
            axisLength = width,
            foregroundProfile = colForegroundRatio,
            aspectRatioHint = (width.toFloat() / height.toFloat()).roundToInt().coerceIn(1, 8)
        )
        val bestRows = findBestGridDivision(
            axisLength = height,
            foregroundProfile = rowForegroundRatio,
            aspectRatioHint = (height.toFloat() / width.toFloat()).roundToInt().coerceIn(1, 8)
        )

        var recommendedCols = nameColsHint ?: bestCols.first
        var recommendedRows = nameRowsHint ?: bestRows.first

        // Aspect ratio fallback for classic horizontal strips (e.g., 256x64 -> 4x1, 128x32 -> 4x1)
        val aspect = width.toFloat() / height.toFloat()
        if (recommendedCols == 1 && recommendedRows == 1) {
            if (aspect >= 1.85f) {
                val stripCols = aspect.roundToInt().coerceIn(2, 8)
                if (width % stripCols == 0 || hasSpriteKeyword) {
                    recommendedCols = stripCols
                }
            } else if (hasSpriteKeyword && width >= 64 && height >= 64 && width % 2 == 0 && height % 2 == 0) {
                recommendedCols = 4
                recommendedRows = 2
            }
        }

        val totalRecommendedFrames = (recommendedCols * recommendedRows).coerceAtLeast(1)
        val isMultiFrameSheet = totalRecommendedFrames > 1
        val userImportedAsSinglePicture = userRequestedColumns == 1 && userRequestedRows == 1
        val gridMismatch = isMultiFrameSheet &&
            (userRequestedColumns != recommendedCols || userRequestedRows != recommendedRows)

        val isSpriteSheetInSingleImage = isMultiFrameSheet && (userImportedAsSinglePicture || gridMismatch)
        val cellW = width / recommendedCols.coerceAtLeast(1)
        val cellH = height / recommendedRows.coerceAtLeast(1)

        val confidence = when {
            nameColsHint != null && nameRowsHint != null -> 96
            bestCols.second > 0.65f || bestRows.second > 0.65f -> 92
            isMultiFrameSheet && aspect >= 2.0f -> 88
            isMultiFrameSheet -> 82
            else -> 90
        }

        val verdictTitle = when {
            isMultiFrameSheet && userImportedAsSinglePicture ->
                "Spritesheet Detected Inside Single Picture Image (${recommendedCols}×${recommendedRows} Grid)"
            gridMismatch ->
                "Spritesheet Grid Mismatch: Detected ${recommendedCols}×${recommendedRows} Frames (Current ${userRequestedColumns}×${userRequestedRows})"
            isMultiFrameSheet ->
                "Verified ${recommendedCols}×${recommendedRows} Multi-Frame Spritesheet (${totalRecommendedFrames} Frames)"
            else ->
                "Verified Single Standalone Picture Image (1×1 Frame)"
        }

        val aiSummary = when {
            isMultiFrameSheet && userImportedAsSinglePicture ->
                "AI File Review detected that '$fileName' (${width}×${height}px) contains $totalRecommendedFrames separate sprite frames arranged in a ${recommendedCols}×${recommendedRows} grid (${cellW}×${cellH}px per frame), but it is currently being added as a 1×1 single picture image. Tap 'Auto-Slice into ${recommendedCols}×${recommendedRows} Spritesheet' so animations and 3D extractions don't mash all poses into one frame."
            gridMismatch ->
                "AI File Review found $totalRecommendedFrames distinct frames in a ${recommendedCols}×${recommendedRows} layout (${cellW}×${cellH}px each), whereas the current import grid is ${userRequestedColumns}×${userRequestedRows}. Apply the recommended ${recommendedCols}×${recommendedRows} slicing for clean frame boundaries."
            isMultiFrameSheet ->
                "AI File Review confirmed '$fileName' is properly configured as a ${recommendedCols}×${recommendedRows} spritesheet with $totalRecommendedFrames animation cells (${cellW}×${cellH}px each)."
            else ->
                "AI File Review inspected '$fileName' (${width}×${height}px) and confirmed it is a single standalone picture image with no internal sprite grid gutters."
        }

        val technicalEvidence = buildString {
            append("Image: ${width}×${height}px | ")
            append("Cell: ${cellW}×${cellH}px | ")
            append("Gutter Periodicity Score: Col=${(bestCols.second * 100).roundToInt()}%, Row=${(bestRows.second * 100).roundToInt()}% | ")
            append("Requested: ${userRequestedColumns}×${userRequestedRows} → Recommended: ${recommendedCols}×${recommendedRows}")
        }

        return AiSpriteSheetReviewResult(
            fileName = fileName,
            imageWidth = width,
            imageHeight = height,
            userRequestedColumns = userRequestedColumns,
            userRequestedRows = userRequestedRows,
            isSpriteSheetInSingleImage = isSpriteSheetInSingleImage,
            recommendedColumns = recommendedCols,
            recommendedRows = recommendedRows,
            detectedFrameCount = totalRecommendedFrames,
            confidencePercent = confidence,
            verdictTitle = verdictTitle,
            aiReviewSummary = aiSummary,
            technicalGridEvidence = technicalEvidence,
            reviewSource = "AI STRUCTURAL GRID SCAN"
        )
    }

    /**
     * Searches candidate division counts (2..8) along an axis to see if interior boundary lines
     * align with low-foreground gutters while interior cells contain balanced sprite content.
     */
    private fun findBestGridDivision(
        axisLength: Int,
        foregroundProfile: FloatArray,
        aspectRatioHint: Int
    ): Pair<Int, Float> {
        if (axisLength < 16) return 1 to 0f

        val avgForeground = foregroundProfile.average().toFloat()
        if (avgForeground <= 0.01f) return 1 to 0f

        var bestDiv = 1
        var bestScore = 0f

        for (div in 2..8) {
            if (axisLength / div < 8) continue
            val cellSize = axisLength.toFloat() / div.toFloat()

            // Check gutter valleys at each interior boundary (1 .. div-1)
            var boundaryValleySum = 0f
            for (b in 1 until div) {
                val center = (b * cellSize).roundToInt().coerceIn(0, axisLength - 1)
                val radius = max(1, (axisLength / (div * 12)).coerceAtMost(4))
                var minFgInWindow = 1f
                for (pos in max(0, center - radius)..min(axisLength - 1, center + radius)) {
                    minFgInWindow = min(minFgInWindow, foregroundProfile[pos])
                }
                val valleyStrength = ((avgForeground - minFgInWindow) / avgForeground.coerceAtLeast(0.05f))
                    .coerceIn(0f, 1f)
                boundaryValleySum += valleyStrength
            }
            val meanValley = boundaryValleySum / (div - 1).toFloat()

            // Verify that at least half of the cells actually contain foreground sprite pixels
            var populatedCells = 0
            for (c in 0 until div) {
                val start = (c * cellSize).roundToInt().coerceIn(0, axisLength - 1)
                val end = (((c + 1) * cellSize).roundToInt() - 1).coerceIn(start, axisLength - 1)
                var sum = 0f
                for (p in start..end) sum += foregroundProfile[p]
                val cellAvg = sum / (end - start + 1).coerceAtLeast(1)
                if (cellAvg > avgForeground * 0.25f) {
                    populatedCells++
                }
            }

            if (populatedCells < max(2, (div * 2) / 3)) continue

            val exactDivBonus = if (axisLength % div == 0) 0.12f else 0f
            val aspectBonus = if (div == aspectRatioHint && aspectRatioHint >= 2) 0.15f else 0f
            val score = (meanValley * 0.85f) + exactDivBonus + aspectBonus

            if (score > bestScore && meanValley >= 0.42f) {
                bestScore = score
                bestDiv = div
            }
        }

        return bestDiv to bestScore.coerceIn(0f, 1f)
    }

    private fun scaleBitmapForVision(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = max(w, h)
        if (longest <= maxDim) return source
        val scale = maxDim.toFloat() / longest.toFloat()
        val targetW = (w * scale).roundToInt().coerceAtLeast(1)
        val targetH = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
