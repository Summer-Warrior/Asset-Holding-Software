package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.model.hardware.ConsoleHardwareCatalog
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.importer.UniversalAssetImporter
import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData
import kotlinx.coroutines.CancellationException
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
) {
    val styleFidelityScorePercent: Float
        get() = styleFidelityScore.toFloat()

    val chromaBleedDetected: Boolean
        get() = !chromaAndAlphaClean

    val issues: List<String>
        get() = regressionOrIpFlags
}

/**
 * Result of running the closed-loop automatic AI style gate with optional 1-pass auto-repair.
 */
data class AutoStyleGateResult(
    val finalBitmap: Bitmap,
    val auditReport: AssetAuditReport,
    val autoRepairAttempted: Boolean,
    val autoRepairImprovedScore: Boolean,
    val correctionDirectiveUsed: String = ""
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

    /**
     * Deterministic closed-loop asset auditor calibrated for [PromptHardwareTier], custom
     * [expectedChromaKeyHex] border bleed detection, and bold-ink cel-shading silhouette contrast.
     */
    fun auditAsset(
        bitmap: Bitmap,
        assetName: String,
        promptUsed: String,
        hardwareTier: PromptHardwareTier = PromptHardwareTier.ARCADE_32BIT_60FPS,
        expectedChromaKeyHex: String = "#FF00FF",
        referenceBitmap: Bitmap? = null,
        intendedForPublicRelease: Boolean = false
    ): AssetAuditReport {
        val targetHardware = resolveHardwareSpecForTier(hardwareTier)
        val baseReport = auditAssetLocally(
            generatedBitmap = bitmap,
            referenceBitmapOrNull = referenceBitmap,
            targetHardware = targetHardware,
            intendedForPublicRelease = intendedForPublicRelease
        )

        val improvements = baseReport.improvementsSummary.toMutableList()
        val flags = baseReport.regressionOrIpFlags.toMutableList()
        var score = baseReport.styleFidelityScore
        var chromaClean = baseReport.chromaAndAlphaClean

        // Check custom expectedChromaKeyHex border bleed if not already flagged
        if (chromaClean && expectedChromaKeyHex.isNotBlank()) {
            val expectedKeyColor = parseHexColorOrNull(expectedChromaKeyHex)
            if (expectedKeyColor != null && inspectCustomBorderChromaBleed(bitmap, expectedKeyColor)) {
                chromaClean = false
                score -= 18
                flags.add(
                    "Unstripped expected chroma-key border ($expectedChromaKeyHex) detected on '$assetName'. Strip chroma-key background cleanly."
                )
            }
        }

        // Enforce crisp silhouette & cel-shading contrast (penalize flat, low-contrast / washed-out renders)
        val colorStats = analyzeQuantizedColorsAndContrast(bitmap)
        if (colorStats.highContrastEdgeRatio < 0.03f) {
            score -= 12
            flags.add(
                "Low silhouette contrast (${(colorStats.highContrastEdgeRatio * 100f).roundToInt()}% edge ratio) — strengthen bold black ink lineart and hard 2-step cel-shading."
            )
        } else if (promptUsed.isNotBlank()) {
            improvements.add(
                "Prompt-aligned silhouette for '$assetName' passed ${hardwareTier.displayName} contrast gate (${(colorStats.highContrastEdgeRatio * 100f).roundToInt()}% edge contrast)."
            )
        }

        val finalScore = score.coerceIn(0, 100)
        val finalTag = when {
            referenceBitmap != null && baseReport.visualDeltaPercent < 15 -> AssetFidelityTag.NEEDS_REVISION
            referenceBitmap != null && intendedForPublicRelease -> AssetFidelityTag.NEEDS_REVISION
            !chromaClean || finalScore < 68 -> AssetFidelityTag.NEEDS_REVISION
            referenceBitmap != null -> AssetFidelityTag.PRIVATE_FAN_DERIVED
            else -> AssetFidelityTag.PUBLIC_CLEAN_ROOM
        }

        return baseReport.copy(
            styleFidelityScore = finalScore,
            chromaAndAlphaClean = chromaClean,
            fidelityTag = finalTag,
            improvementsSummary = improvements.distinct(),
            regressionOrIpFlags = flags.distinct(),
            auditSource = "Automatic Closed-Loop Style Gate (${hardwareTier.displayName})"
        )
    }

    /**
     * Converts [AssetAuditReport] findings into a concise, deterministic style-correction
     * instruction suitable for a 1-pass AI image auto-repair prompt.
     */
    fun buildStyleCorrectionDirective(report: AssetAuditReport): String {
        val directives = mutableListOf<String>()
        if (report.chromaBleedDetected) {
            directives.add(
                "CHROMA-KEY CLEANUP: Isolate the subject completely on a solid, flat, unshaded key background with razor-sharp opaque edges, zero colored halo/spill, and zero background gradients or shadows."
            )
        }
        if (report.styleFidelityScorePercent < 75f || report.issues.any { it.contains("contrast", ignoreCase = true) }) {
            directives.add(
                "SILHOUETTE & CEL-SHADING BOOST: Enforce crisp bold black ink outlines (2px equivalent), hard 2-step cel-shading with deep ink-pool shadows, punchy high-contrast color-blocking, and unmistakable silhouette readability."
            )
        }
        if (!report.hardwareBudgetCompliant) {
            directives.add(
                "HARDWARE PALETTE & TILE BUDGET: Reduce color count into tight, quantized color ramps with clean pixel clusters and no muddy airbrush gradients or soft blur."
            )
        }
        if (report.issues.any { it.contains("Visual Delta", ignoreCase = true) || it.contains("IP Guard", ignoreCase = true) }) {
            directives.add(
                "ORIGINAL CLEAN-ROOM REDESIGN: Substantially transform silhouette proportions, armor trim, and pose so the sprite is unmistakably original clean-room art."
            )
        }
        report.issues.take(3).forEach { issue ->
            directives.add("FIX REQUIRED: $issue")
        }
        if (directives.isEmpty()) {
            directives.add("Maintain crisp black ink outlines, clean alpha boundary, and high-contrast 2-step cel-shading.")
        }
        return directives.joinToString(separator = "\n")
    }

    /**
     * Runs the closed-loop automatic style gate on [initialBitmap].
     * When [allowSingleAutoRepair] is true and the initial audit fails (`NEEDS_REVISION`,
     * score < 68%, or chroma bleed detected), triggers at most ONE [repairGenerator] pass
     * with [buildStyleCorrectionDirective], re-audits the repaired bitmap, and keeps whichever
     * result scores higher (preferring zero chroma bleed on ties).
     */
    suspend fun runClosedLoopStyleGate(
        initialBitmap: Bitmap,
        assetName: String,
        promptUsed: String,
        hardwareTier: PromptHardwareTier,
        expectedChromaKeyHex: String,
        referenceBitmap: Bitmap? = null,
        allowSingleAutoRepair: Boolean = true,
        repairGenerator: (suspend (correctionDirective: String) -> Bitmap?)? = null
    ): AutoStyleGateResult {
        val initialReport = auditAsset(
            bitmap = initialBitmap,
            assetName = assetName,
            promptUsed = promptUsed,
            hardwareTier = hardwareTier,
            expectedChromaKeyHex = expectedChromaKeyHex,
            referenceBitmap = referenceBitmap
        )

        val failedStyleGate = initialReport.fidelityTag == AssetFidelityTag.NEEDS_REVISION ||
            initialReport.styleFidelityScorePercent < 68f ||
            initialReport.chromaBleedDetected

        if (!allowSingleAutoRepair || !failedStyleGate || repairGenerator == null) {
            return AutoStyleGateResult(
                finalBitmap = initialBitmap,
                auditReport = initialReport,
                autoRepairAttempted = false,
                autoRepairImprovedScore = false,
                correctionDirectiveUsed = ""
            )
        }

        val correctionDirective = buildStyleCorrectionDirective(initialReport)
        val repairedBitmap: Bitmap? = try {
            repairGenerator.invoke(correctionDirective)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w("CanonicalAuditor", "1-pass auto-repair generation failed for '$assetName': ${e.message}", e)
            null
        }

        if (repairedBitmap == null) {
            return AutoStyleGateResult(
                finalBitmap = initialBitmap,
                auditReport = initialReport,
                autoRepairAttempted = true,
                autoRepairImprovedScore = false,
                correctionDirectiveUsed = correctionDirective
            )
        }

        val repairedReport = auditAsset(
            bitmap = repairedBitmap,
            assetName = assetName,
            promptUsed = promptUsed,
            hardwareTier = hardwareTier,
            expectedChromaKeyHex = expectedChromaKeyHex,
            referenceBitmap = referenceBitmap
        )

        val repairWon = when {
            repairedReport.styleFidelityScorePercent > initialReport.styleFidelityScorePercent -> true
            repairedReport.styleFidelityScorePercent < initialReport.styleFidelityScorePercent -> false
            else -> !repairedReport.chromaBleedDetected && initialReport.chromaBleedDetected
        }

        return if (repairWon) {
            AutoStyleGateResult(
                finalBitmap = repairedBitmap,
                auditReport = repairedReport,
                autoRepairAttempted = true,
                autoRepairImprovedScore = true,
                correctionDirectiveUsed = correctionDirective
            )
        } else {
            AutoStyleGateResult(
                finalBitmap = initialBitmap,
                auditReport = initialReport,
                autoRepairAttempted = true,
                autoRepairImprovedScore = false,
                correctionDirectiveUsed = correctionDirective
            )
        }
    }

    private fun resolveHardwareSpecForTier(tier: PromptHardwareTier): ConsoleHardwareSpec {
        val targetId = when (tier) {
            PromptHardwareTier.PIXEL_16BIT -> "snes"
            PromptHardwareTier.ARCADE_32BIT_60FPS -> "ps1"
            PromptHardwareTier.TRUE_FUSION_HD -> "switch"
        }
        return ConsoleHardwareCatalog.allSystemsWithFusions().firstOrNull { it.id.equals(targetId, ignoreCase = true) }
            ?: ConsoleHardwareCatalog.getById("ps1")
    }

    private fun parseHexColorOrNull(hex: String): Int? {
        return try {
            val clean = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
            if (clean.length == 7 || clean.length == 9) Color.parseColor(clean) else null
        } catch (e: Exception) {
            Log.w("CanonicalAuditor", "Ignoring invalid expectedChromaKeyHex '$hex': ${e.message}")
            null
        }
    }

    private fun inspectCustomBorderChromaBleed(bitmap: Bitmap, keyColor: Int): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return false
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var borderTotal = 0
        var bleedCount = 0
        fun check(x: Int, y: Int) {
            val px = pixels[y * w + x]
            borderTotal++
            if (
                Color.alpha(px) >= 240 &&
                abs(Color.red(px) - Color.red(keyColor)) <= CHROMA_TOLERANCE &&
                abs(Color.green(px) - Color.green(keyColor)) <= CHROMA_TOLERANCE &&
                abs(Color.blue(px) - Color.blue(keyColor)) <= CHROMA_TOLERANCE
            ) {
                bleedCount++
            }
        }
        for (x in 0 until w) {
            check(x, 0)
            if (h > 1) check(x, h - 1)
        }
        for (y in 1 until h - 1) {
            check(0, y)
            if (w > 1) check(w - 1, y)
        }
        val threshold = max(2, (borderTotal * 0.05f).roundToInt())
        return bleedCount >= threshold
    }
}
