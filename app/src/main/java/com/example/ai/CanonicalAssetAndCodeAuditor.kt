package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.importer.UniversalAssetImporter
import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Compliance & provenance classification assigned by [CanonicalAssetAndCodeAuditor].
 */
enum class AssetFidelityTag(val label: String, val badge: String) {
    PUBLIC_CLEAN_ROOM("Public Clean-Room", "CLEAN-ROOM"),
    PRIVATE_FAN_DERIVED("Private Fan-Derived", "FAN-DERIVED"),
    NEEDS_REVISION("Needs Revision", "NEEDS REVISION")
}

/**
 * Structured audit verdict for a generated or imported 2D/3D studio asset.
 */
data class AssetAuditReport(
    val styleFidelityScore: Int,
    val visualDeltaPercent: Int,
    val chromaAndAlphaClean: Boolean,
    val hardwareBudgetCompliant: Boolean,
    val fidelityTag: AssetFidelityTag,
    val improvementsSummary: List<String>,
    val regressionOrIpFlags: List<String>,
    val auditSource: String
)

/**
 * Hardware budget & ecosystem manifest contract compliance report (`asset-index.json` / `skins/{id}.json`).
 */
data class ContractComplianceReport(
    val hardwareBudgetPassed: Boolean,
    val contractJsonPassed: Boolean,
    val meshTriangles: Int,
    val maxRecommendedTriangles: Int,
    val spriteColorsDetected: Int,
    val maxAllowedSpriteColors: Int,
    val estimatedVramBytes: Long,
    val vramBudgetBytes: Long,
    val passedChecks: List<String>,
    val findings: List<String>
)

/**
 * Canonical Asset & Official Spec Auditor.
 * Performs deterministic on-device visual delta, chroma-key alpha bleed, hardware palette/tile budget,
 * and IP clean-room compliance audits, plus optional OmniAiRouter Vision Review augmentation
 * and ephemeral reference cache cleanup (`cacheDir/ephemeral_sources`).
 */
object CanonicalAssetAndCodeAuditor {

    private const val DELTA_SAMPLE_SIZE = 48
    private const val CHROMA_TOLERANCE = 42

    /**
     * Runs a fast, deterministic local audit on [generatedBitmap], optionally comparing it against
     * [referenceBitmapOrNull] for visual delta % and clean-room IP release compliance, and validating
     * against [targetHardware] budgets.
     */
    fun auditAssetLocally(
        generatedBitmap: Bitmap,
        referenceBitmapOrNull: Bitmap?,
        targetHardware: ConsoleHardwareSpec,
        intendedForPublicRelease: Boolean = false
    ): AssetAuditReport {
        val improvements = mutableListOf<String>()
        val flags = mutableListOf<String>()

        // 1. Perceptual visual delta % (0..100%)
        val visualDeltaPercent = if (referenceBitmapOrNull != null) {
            computePerceptualVisualDeltaPercent(generatedBitmap, referenceBitmapOrNull)
        } else {
            100
        }

        if (referenceBitmapOrNull != null) {
            if (visualDeltaPercent < 15) {
                flags.add(
                    "Visual Delta is only $visualDeltaPercent% (< 15% threshold) — asset is nearly identical to the reference image and needs substantial redesign."
                )
            } else {
                improvements.add(
                    "Verified $visualDeltaPercent% perceptual visual delta from source reference (clears 15% redesign threshold)."
                )
            }
            if (intendedForPublicRelease) {
                flags.add(
                    "Public Release IP Guard: asset was derived from a reference image. Redraw clean-room from scratch or switch classification to Private Fan-Derived."
                )
            }
        } else {
            improvements.add("Standalone clean-room asset with no reference bitmap dependency.")
        }

        // 2. Border opaque chroma-key bleed check (#FF00FF / #00FF00)
        val chromaBleedResult = inspectBorderChromaKeyBleed(generatedBitmap)
        val chromaAndAlphaClean = chromaBleedResult.isClean
        if (chromaAndAlphaClean) {
            improvements.add("Border alpha is clean with zero unstripped #FF00FF or #00FF00 chroma-key bleed.")
        } else {
            flags.add(chromaBleedResult.diagnostic)
        }

        // 3. Hardware color & resolution budget check
        val colorStats = analyzeQuantizedColorsAndContrast(generatedBitmap)
        val maxAllowedColors = targetHardware.maxOnScreenColors.coerceAtLeast(targetHardware.maxSpriteColorsPerPalette)
        val maxTileDim = (targetHardware.targetSpriteResolution * 8).coerceAtLeast(256)
        val colorOk = colorStats.quantizedUniqueColors <= maxAllowedColors
        val dimOk = generatedBitmap.width <= maxTileDim && generatedBitmap.height <= maxTileDim
        val hardwareBudgetCompliant = colorOk && dimOk

        if (colorOk) {
            improvements.add(
                "Palette density (${colorStats.quantizedUniqueColors} quantized colors) fits ${targetHardware.shortName} budget (<= $maxAllowedColors)."
            )
        } else {
            flags.add(
                "Detected ${colorStats.quantizedUniqueColors} quantized colors, exceeding ${targetHardware.shortName} on-screen limit ($maxAllowedColors)."
            )
        }
        if (!dimOk) {
            flags.add(
                "Dimensions ${generatedBitmap.width}x${generatedBitmap.height} exceed ${targetHardware.shortName} tile sheet envelope (${maxTileDim}px)."
            )
        }

        // 4. Style fidelity score (0..100)
        var score = 62
        if (chromaAndAlphaClean) score += 14 else score -= 18
        if (hardwareBudgetCompliant) score += 12 else score -= 10
        if (colorStats.highContrastEdgeRatio >= 0.05f) {
            score += 8
            improvements.add("Crisp cel-shaded silhouette edge contrast detected.")
        }
        if (referenceBitmapOrNull != null) {
            if (visualDeltaPercent >= 25) score += 4
            if (visualDeltaPercent < 15) score -= 24
        }
        val styleFidelityScore = score.coerceIn(0, 100)

        // 5. Assign fidelity tag
        val fidelityTag = when {
            referenceBitmapOrNull != null && visualDeltaPercent < 15 -> AssetFidelityTag.NEEDS_REVISION
            referenceBitmapOrNull != null && intendedForPublicRelease -> AssetFidelityTag.NEEDS_REVISION
            !chromaAndAlphaClean -> AssetFidelityTag.NEEDS_REVISION
            referenceBitmapOrNull != null -> AssetFidelityTag.PRIVATE_FAN_DERIVED
            else -> AssetFidelityTag.PUBLIC_CLEAN_ROOM
        }

        return AssetAuditReport(
            styleFidelityScore = styleFidelityScore,
            visualDeltaPercent = visualDeltaPercent,
            chromaAndAlphaClean = chromaAndAlphaClean,
            hardwareBudgetCompliant = hardwareBudgetCompliant,
            fidelityTag = fidelityTag,
            improvementsSummary = improvements,
            regressionOrIpFlags = flags,
            auditSource = "Local Canonical Spec Auditor"
        )
    }

    /**
     * Runs [auditAssetLocally] first, then optionally queries [OmniAiRouter.generateTextOrJson]
     * under [AiTaskRole.VISION_REVIEW] to enrich style/IP commentary.
     * Falls back cleanly to the local audit result when offline or unconfigured.
     */
    suspend fun auditAssetWithOmniAi(
        generatedBitmap: Bitmap,
        referenceBitmapOrNull: Bitmap?,
        targetHardware: ConsoleHardwareSpec,
        intendedForPublicRelease: Boolean = false,
        sharedContextBlock: String = "",
        assetTitle: String = "Studio Asset"
    ): AssetAuditReport {
        val localReport = auditAssetLocally(
            generatedBitmap = generatedBitmap,
            referenceBitmapOrNull = referenceBitmapOrNull,
            targetHardware = targetHardware,
            intendedForPublicRelease = intendedForPublicRelease
        )

        return try {
            val activeVision = OmniAiRouter.getRoutingConfig().activeVisionProvider
            val profile = OmniAiRouter.getProviderProfile(activeVision)
            if (!profile.isConnected) {
                return localReport
            }

            val auditPrompt = buildString {
                appendLine("Perform a concise Canonical Asset & Style Audit for \"$assetTitle\".")
                appendLine("Target Hardware: ${targetHardware.name} (${targetHardware.shortName}), max colors ${targetHardware.maxOnScreenColors}.")
                appendLine("Local metrics: visualDeltaPercent=${localReport.visualDeltaPercent}%, chromaAndAlphaClean=${localReport.chromaAndAlphaClean}, hardwareBudgetCompliant=${localReport.hardwareBudgetCompliant}, intendedForPublicRelease=$intendedForPublicRelease.")
                appendLine("Return ONLY valid JSON with keys:")
                appendLine("{ \"styleFidelityScore\": 0-100, \"improvements\": [\"...\"], \"flags\": [\"...\"] }")
            }

            val scaled = GeminiAssetForge.scaleBitmapForInference(generatedBitmap, maxDimension = 512)
            val response = OmniAiRouter.generateTextOrJson(
                role = AiTaskRole.VISION_REVIEW,
                prompt = auditPrompt,
                systemInstruction = "You are a strict 2D/3D game art director and clean-room IP auditor. Reply with compact JSON only.",
                imageBitmap = scaled,
                expectJson = true,
                sharedContextBlock = sharedContextBlock,
                firebaseDelegate = { _, _ ->
                    throw IllegalStateException("Vision review Firebase fallback delegated to local audit when unavailable.")
                }
            )

            val parsed = JSONObject(response.text)
            val aiScore = parsed.optInt("styleFidelityScore", localReport.styleFidelityScore).coerceIn(0, 100)
            val aiImprovements = mutableListOf<String>().apply {
                addAll(localReport.improvementsSummary)
                val arr = parsed.optJSONArray("improvements")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotEmpty() && none { it.equals(item, ignoreCase = true) }) {
                            add(item)
                        }
                    }
                }
            }
            val aiFlags = mutableListOf<String>().apply {
                addAll(localReport.regressionOrIpFlags)
                val arr = parsed.optJSONArray("flags")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotEmpty() && none { it.equals(item, ignoreCase = true) }) {
                            add(item)
                        }
                    }
                }
            }

            // Preserve hard local safety guards (never allow AI to override NEEDS_REVISION on IP/chroma failure)
            val mergedScore = ((localReport.styleFidelityScore + aiScore) / 2).coerceIn(0, 100)
            localReport.copy(
                styleFidelityScore = mergedScore,
                improvementsSummary = aiImprovements.take(6),
                regressionOrIpFlags = aiFlags.take(6),
                auditSource = "Local Auditor + ${response.providerUsed.defaultDisplayName} (${response.modelIdUsed})"
            )
        } catch (_: Exception) {
            localReport
        }
    }

    /**
     * Approves the audited asset and purges any temporary reference files inside
     * `cacheDir/ephemeral_sources` via [UniversalAssetImporter.sweepEphemeralSources].
     */
    fun approveAndCleanEphemeralReference(context: Context): Int {
        return UniversalAssetImporter.sweepEphemeralSources(context)
    }

    /**
     * Audits 3D mesh triangle budgets, 2D sprite color & VRAM budgets against [hardwareSpec],
     * and validates `asset-index.json` or `skins/{id}.json` contract fields if [assetIndexJson] is supplied.
     */
    fun auditHardwareAndContractCompliance(
        mesh: Mesh3D?,
        spriteSheet: SpriteSheetData?,
        hardwareSpec: ConsoleHardwareSpec,
        assetIndexJson: String? = null
    ): ContractComplianceReport {
        val passedChecks = mutableListOf<String>()
        val findings = mutableListOf<String>()

        val meshTris = mesh?.faceCount ?: 0
        val maxTris = hardwareSpec.recommendedModelTriBudget
        val meshOk = if (mesh == null) {
            true
        } else if (!hardwareSpec.hasHardware3D && meshTris > 0) {
            findings.add("${hardwareSpec.shortName} has no dedicated 3D hardware (mesh has $meshTris tris; bake to 2D sprite tiles).")
            false
        } else if (meshTris > maxTris) {
            findings.add("3D mesh ($meshTris tris) exceeds ${hardwareSpec.shortName} recommended budget ($maxTris tris).")
            false
        } else {
            passedChecks.add("3D mesh ($meshTris / $maxTris tris) fits ${hardwareSpec.shortName} geometry budget.")
            true
        }

        val spriteColors = spriteSheet?.let { analyzeQuantizedColorsAndContrast(it.sourceBitmap).quantizedUniqueColors } ?: 0
        val maxSpriteColors = hardwareSpec.maxOnScreenColors.coerceAtLeast(hardwareSpec.maxSpriteColorsPerPalette)
        val spriteColorOk = spriteSheet == null || spriteColors <= maxSpriteColors
        if (spriteSheet != null) {
            if (spriteColorOk) {
                passedChecks.add("Sprite sheet ($spriteColors / $maxSpriteColors colors) fits ${hardwareSpec.shortName} color limit.")
            } else {
                findings.add("Sprite sheet ($spriteColors colors) exceeds ${hardwareSpec.shortName} color budget ($maxSpriteColors).")
            }
        }

        val meshBytes = (mesh?.vertices?.size ?: 0).toLong() * 32L + meshTris.toLong() * 12L
        val spritePixels = spriteSheet?.let { it.sourceBitmap.width.toLong() * it.sourceBitmap.height.toLong() } ?: 0L
        val bytesPerPixel = max(1, hardwareSpec.bitsPerPixelSprite / 8).toLong()
        val estimatedVram = meshBytes + (spritePixels * bytesPerPixel)
        val vramOk = estimatedVram <= hardwareSpec.vramBytes
        if (vramOk) {
            passedChecks.add("Estimated VRAM (${estimatedVram / 1024} KB) fits ${hardwareSpec.shortName} (${hardwareSpec.vramBytes / 1024} KB).")
        } else {
            findings.add("Estimated VRAM (${estimatedVram / 1024} KB) exceeds ${hardwareSpec.shortName} VRAM (${hardwareSpec.vramBytes / 1024} KB).")
        }

        var contractOk = true
        if (!assetIndexJson.isNullOrBlank()) {
            try {
                val root = JSONObject(assetIndexJson)
                val isAssetIndex = root.has("baseUrl") || root.has("assets")
                val isSkinManifest = root.has("skinId") || root.has("slotBindings")
                when {
                    isAssetIndex -> {
                        val baseUrl = root.optString("baseUrl", "").trim()
                        val assetsArr = root.optJSONArray("assets")
                        if (baseUrl.isEmpty() || !baseUrl.startsWith("http")) {
                            contractOk = false
                            findings.add("asset-index.json missing valid https baseUrl.")
                        }
                        if (assetsArr == null) {
                            contractOk = false
                            findings.add("asset-index.json missing 'assets' array.")
                        } else {
                            for (i in 0 until assetsArr.length()) {
                                val entry = assetsArr.optJSONObject(i)
                                if (entry == null || entry.optString("key").isBlank() || entry.optString("path").isBlank()) {
                                    contractOk = false
                                    findings.add("asset-index.json entry #$i is missing required 'key' or 'path'.")
                                }
                            }
                            if (contractOk) {
                                passedChecks.add("asset-index.json contract valid (${assetsArr.length()} entries).")
                            }
                        }
                    }
                    isSkinManifest -> {
                        val skinId = root.optString("skinId", "").trim()
                        val bindings = root.optJSONObject("slotBindings")
                        if (skinId.isEmpty() || bindings == null) {
                            contractOk = false
                            findings.add("skins/*.json manifest missing required 'skinId' or 'slotBindings' object.")
                        } else {
                            passedChecks.add("Skin manifest '$skinId' valid (${bindings.length()} slot bindings).")
                        }
                    }
                    else -> {
                        contractOk = false
                        findings.add("JSON contract must declare either asset-index ('baseUrl'+'assets') or skin manifest ('skinId'+'slotBindings').")
                    }
                }
            } catch (e: Exception) {
                contractOk = false
                findings.add("Invalid JSON contract: ${e.message ?: "parse error"}")
            }
        }

        return ContractComplianceReport(
            hardwareBudgetPassed = meshOk && spriteColorOk && vramOk,
            contractJsonPassed = contractOk,
            meshTriangles = meshTris,
            maxRecommendedTriangles = maxTris,
            spriteColorsDetected = spriteColors,
            maxAllowedSpriteColors = maxSpriteColors,
            estimatedVramBytes = estimatedVram,
            vramBudgetBytes = hardwareSpec.vramBytes,
            passedChecks = passedChecks,
            findings = findings
        )
    }

    // -------------------------------------------------------------------------
    // Internal Perceptual & Pixel Metrics
    // -------------------------------------------------------------------------

    private data class ChromaBleedResult(
        val isClean: Boolean,
        val diagnostic: String
    )

    private data class ColorContrastStats(
        val quantizedUniqueColors: Int,
        val highContrastEdgeRatio: Float
    )

    private fun computePerceptualVisualDeltaPercent(a: Bitmap, b: Bitmap): Int {
        val scaledA = Bitmap.createScaledBitmap(a, DELTA_SAMPLE_SIZE, DELTA_SAMPLE_SIZE, false)
        val scaledB = Bitmap.createScaledBitmap(b, DELTA_SAMPLE_SIZE, DELTA_SAMPLE_SIZE, false)
        val count = DELTA_SAMPLE_SIZE * DELTA_SAMPLE_SIZE
        val pxA = IntArray(count)
        val pxB = IntArray(count)
        scaledA.getPixels(pxA, 0, DELTA_SAMPLE_SIZE, 0, 0, DELTA_SAMPLE_SIZE, DELTA_SAMPLE_SIZE)
        scaledB.getPixels(pxB, 0, DELTA_SAMPLE_SIZE, 0, 0, DELTA_SAMPLE_SIZE, DELTA_SAMPLE_SIZE)
        if (scaledA !== a) scaledA.recycle()
        if (scaledB !== b) scaledB.recycle()

        var totalNormalizedDiff = 0.0
        var significantlyChangedPixels = 0

        for (i in 0 until count) {
            val c1 = pxA[i]
            val c2 = pxB[i]
            val a1 = Color.alpha(c1)
            val a2 = Color.alpha(c2)
            if (a1 < 16 && a2 < 16) continue

            val dr = (Color.red(c1) - Color.red(c2)).toDouble()
            val dg = (Color.green(c1) - Color.green(c2)).toDouble()
            val db = (Color.blue(c1) - Color.blue(c2)).toDouble()
            val da = (a1 - a2).toDouble()

            // Weighted perceptual Euclidean distance normalized to 0..1
            val dist = sqrt(0.30 * dr * dr + 0.59 * dg * dg + 0.11 * db * db + 0.50 * da * da) / 312.0
            val clamped = dist.coerceIn(0.0, 1.0)
            totalNormalizedDiff += clamped
            if (clamped >= 0.12) {
                significantlyChangedPixels++
            }
        }

        val meanDiffPct = (totalNormalizedDiff / count.toDouble()) * 100.0
        val changedAreaPct = (significantlyChangedPixels.toDouble() / count.toDouble()) * 100.0
        // Combine mean perceptual distance and changed-area coverage so identical = 0%, major redesign = 40..100%
        return (meanDiffPct * 0.65 + changedAreaPct * 0.55).roundToInt().coerceIn(0, 100)
    }

    private fun inspectBorderChromaKeyBleed(bitmap: Bitmap): ChromaBleedResult {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return ChromaBleedResult(true, "")

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val magenta = Color.rgb(255, 0, 255)
        val green = Color.rgb(0, 255, 0)

        fun isChromaKey(px: Int, key: Int): Boolean {
            return Color.alpha(px) >= 240 &&
                abs(Color.red(px) - Color.red(key)) <= CHROMA_TOLERANCE &&
                abs(Color.green(px) - Color.green(key)) <= CHROMA_TOLERANCE &&
                abs(Color.blue(px) - Color.blue(key)) <= CHROMA_TOLERANCE
        }

        var borderTotal = 0
        var magentaBleed = 0
        var greenBleed = 0

        fun checkPixel(x: Int, y: Int) {
            val px = pixels[y * w + x]
            borderTotal++
            if (isChromaKey(px, magenta)) magentaBleed++
            if (isChromaKey(px, green)) greenBleed++
        }

        for (x in 0 until w) {
            checkPixel(x, 0)
            if (h > 1) checkPixel(x, h - 1)
        }
        for (y in 1 until h - 1) {
            checkPixel(0, y)
            if (w > 1) checkPixel(w - 1, y)
        }

        val threshold = max(2, (borderTotal * 0.05f).roundToInt())
        return when {
            magentaBleed >= threshold -> ChromaBleedResult(
                isClean = false,
                diagnostic = "Unstripped opaque Magenta (#FF00FF) chroma-key border detected ($magentaBleed border px). Run Strip Solid / Magenta BG."
            )
            greenBleed >= threshold -> ChromaBleedResult(
                isClean = false,
                diagnostic = "Unstripped opaque Lime Green (#00FF00) chroma-key border detected ($greenBleed border px). Run Strip Solid / Magenta BG."
            )
            else -> ChromaBleedResult(true, "")
        }
    }

    private fun analyzeQuantizedColorsAndContrast(bitmap: Bitmap): ColorContrastStats {
        val sampleW = bitmap.width.coerceAtMost(96)
        val sampleH = bitmap.height.coerceAtMost(96)
        val sampled = if (sampleW == bitmap.width && sampleH == bitmap.height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        }
        val pixels = IntArray(sampleW * sampleH)
        sampled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        if (sampled !== bitmap) sampled.recycle()

        // 5-bit per channel quantization (matching 15-bit BGR555 / arcade ramp granularity)
        val uniqueBuckets = HashSet<Int>()
        var edgePixels = 0
        var opaquePixels = 0

        for (y in 0 until sampleH) {
            for (x in 0 until sampleW) {
                val px = pixels[y * sampleW + x]
                if (Color.alpha(px) < 128) continue
                opaquePixels++
                val r5 = Color.red(px) shr 3
                val g5 = Color.green(px) shr 3
                val b5 = Color.blue(px) shr 3
                uniqueBuckets.add((r5 shl 10) or (g5 shl 5) or b5)

                if (x + 1 < sampleW) {
                    val right = pixels[y * sampleW + (x + 1)]
                    val lumDiff = abs(
                        (Color.red(px) * 3 + Color.green(px) * 6 + Color.blue(px)) -
                            (Color.red(right) * 3 + Color.green(right) * 6 + Color.blue(right))
                    ) / 10
                    if (lumDiff >= 55 || Color.alpha(right) < 128) {
                        edgePixels++
                    }
                }
            }
        }

        val ratio = if (opaquePixels > 0) edgePixels.toFloat() / opaquePixels.toFloat() else 0f
        return ColorContrastStats(
            quantizedUniqueColors = uniqueBuckets.size,
            highContrastEdgeRatio = ratio
        )
    }
}
