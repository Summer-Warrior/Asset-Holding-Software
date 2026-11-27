package com.example.model.masterpack

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.hardware.ConsoleHardwareSpec
import com.example.model.sprite.SpriteSheetData
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * Result of foot-aligning every frame in a [SpriteSheetData] and deriving deterministic [RigMetadata].
 */
data class FootAlignedSliceResult(
    val alignedSheet: SpriteSheetData,
    val rigMetadata: RigMetadata,
    val framesAlignedCount: Int,
    val cellWidth: Int,
    val cellHeight: Int,
    val summary: String
)

/**
 * Foot-Aligned Slicer & Hardware Visual Output Split Compiler.
 * Eliminates frame-to-frame vertical/horizontal bounce by snapping every frame's opaque silhouette
 * bottom-center to `(cellW / 2, cellH - paddingPx)` and compiles all 4 hardware visual tiers +
 * `master_pack_manifest.json` from a single 3-Map + Depth/Normal layer set.
 */
object FootAlignedSlicerEngine {

    private const val ALPHA_THRESHOLD = 24

    private data class TightBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int get() = (right - left + 1).coerceAtLeast(1)
        val height: Int get() = (bottom - top + 1).coerceAtLeast(1)
    }

    /**
     * Inspects every frame in [sheet.frames], computes a uniform cell fitting the largest tight
     * opaque bounding box + `paddingPx * 2`, redraws every frame with its bottom-center aligned
     * identically at `(cellW / 2, cellH - paddingPx)`, and auto-computes [RigMetadata].
     */
    fun sliceAndFootAlignSheet(
        sheet: SpriteSheetData,
        cadenceFps: FrameCadenceFps = FrameCadenceFps.FPS_15,
        paddingPx: Int = 2
    ): FootAlignedSliceResult {
        val safePadding = paddingPx.coerceAtLeast(0)
        val frames = sheet.frames.ifEmpty { listOf(sheet.sourceBitmap) }
        val boundsList = frames.map { findTightOpaqueBounds(it) }

        val maxBoxW = boundsList.filterNotNull().maxOfOrNull { it.width } ?: sheet.frameWidth.coerceAtLeast(1)
        val maxBoxH = boundsList.filterNotNull().maxOfOrNull { it.height } ?: sheet.frameHeight.coerceAtLeast(1)
        val cellW = (maxBoxW + safePadding * 2).coerceAtLeast(4)
        val cellH = (maxBoxH + safePadding * 2).coerceAtLeast(4)

        val alignedFrames = mutableListOf<Bitmap>()
        var alignedCount = 0

        // Track aggregate union silhouette across aligned frames for tight RigMetadata computation
        var unionLeft = cellW
        var unionTop = cellH
        var unionRight = 0
        var unionBottom = 0

        frames.forEachIndexed { idx, frame ->
            val dstBmp = Bitmap.createBitmap(cellW, cellH, Bitmap.Config.ARGB_8888)
            dstBmp.setHasAlpha(true)
            dstBmp.eraseColor(Color.TRANSPARENT)

            val bounds = boundsList[idx]
            if (bounds != null) {
                val destLeft = ((cellW - bounds.width) / 2).coerceIn(0, (cellW - bounds.width).coerceAtLeast(0))
                val destBottomExclusive = (cellH - safePadding).coerceIn(bounds.height, cellH)
                val destTop = (destBottomExclusive - bounds.height).coerceAtLeast(0)

                val patchPixels = IntArray(bounds.width * bounds.height)
                frame.getPixels(patchPixels, 0, bounds.width, bounds.left, bounds.top, bounds.width, bounds.height)
                dstBmp.setPixels(patchPixels, 0, bounds.width, destLeft, destTop, bounds.width, bounds.height)
                alignedCount++

                unionLeft = min(unionLeft, destLeft)
                unionTop = min(unionTop, destTop)
                unionRight = max(unionRight, destLeft + bounds.width - 1)
                unionBottom = max(unionBottom, destTop + bounds.height - 1)
            }
            alignedFrames.add(dstBmp)
        }

        // Reassemble crisp atlas bitmap preserving columns x rows via lossless pixel blitting
        val cols = sheet.columns.coerceAtLeast(1)
        val rows = ((alignedFrames.size + cols - 1) / cols).coerceAtLeast(sheet.rows.coerceAtLeast(1))
        val atlasBmp = Bitmap.createBitmap(cellW * cols, cellH * rows, Bitmap.Config.ARGB_8888)
        atlasBmp.setHasAlpha(true)
        atlasBmp.eraseColor(Color.TRANSPARENT)
        val frameBuffer = IntArray(cellW * cellH)
        alignedFrames.forEachIndexed { i, alignedFrame ->
            val col = i % cols
            val row = i / cols
            if (row < rows) {
                alignedFrame.getPixels(frameBuffer, 0, cellW, 0, 0, cellW, cellH)
                atlasBmp.setPixels(frameBuffer, 0, cellW, col * cellW, row * cellH, cellW, cellH)
            }
        }

        val alignedSheet = sheet.copy(
            sourceBitmap = atlasBmp,
            columns = cols,
            rows = rows,
            frameWidth = cellW,
            frameHeight = cellH,
            frames = alignedFrames,
            originalWidth = atlasBmp.width,
            originalHeight = atlasBmp.height,
            sourceKindLabel = "${sheet.sourceKindLabel} • FOOT-ALIGNED"
        )

        // Derive deterministic RigMetadata from aligned cell & union silhouette
        val hasUnion = unionLeft <= unionRight && unionTop <= unionBottom
        val uLeft = if (hasUnion) unionLeft.toFloat() / cellW else 0.24f
        val uTop = if (hasUnion) unionTop.toFloat() / cellH else 0.14f
        val uRight = if (hasUnion) (unionRight + 1).toFloat() / cellW else 0.76f
        val uBottom = if (hasUnion) (unionBottom + 1).toFloat() / cellH else (cellH - safePadding).toFloat() / cellH
        val uW = (uRight - uLeft).coerceAtLeast(0.10f)
        val uH = (uBottom - uTop).coerceAtLeast(0.10f)
        val feetPivotY = ((cellH - safePadding).toFloat() / cellH.toFloat()).coerceIn(0f, 1f)

        val hurtbox = NormalizedRectBox(
            xNorm = uLeft + uW * 0.10f,
            yNorm = uTop + uH * 0.08f,
            wNorm = uW * 0.80f,
            hNorm = uH * 0.88f
        ).clamped()

        val hitbox = NormalizedRectBox(
            xNorm = (uLeft + uW * 0.48f).coerceIn(0f, 0.90f),
            yNorm = uTop + uH * 0.22f,
            wNorm = (uW * 0.62f).coerceIn(0.12f, 0.55f),
            hNorm = uH * 0.46f
        ).clamped()

        val anchors = linkedMapOf(
            AnchorPointType.FEET_PIVOT to NormalizedAnchor.of(AnchorPointType.FEET_PIVOT, 0.50f, feetPivotY),
            AnchorPointType.HEAD to NormalizedAnchor.of(AnchorPointType.HEAD, 0.50f, uTop + uH * 0.18f),
            AnchorPointType.BODY to NormalizedAnchor.of(AnchorPointType.BODY, 0.50f, uTop + uH * 0.52f),
            AnchorPointType.WING to NormalizedAnchor.of(AnchorPointType.WING, (uLeft + uW * 0.22f), uTop + uH * 0.42f),
            AnchorPointType.HORN to NormalizedAnchor.of(AnchorPointType.HORN, 0.50f, (uTop + uH * 0.05f)),
            AnchorPointType.TAIL to NormalizedAnchor.of(AnchorPointType.TAIL, (uLeft + uW * 0.12f), uTop + uH * 0.72f),
            AnchorPointType.VISOR to NormalizedAnchor.of(AnchorPointType.VISOR, (uLeft + uW * 0.58f), uTop + uH * 0.20f)
        )

        val rigMeta = RigMetadata(
            assetName = sheet.name,
            frameWidth = cellW,
            frameHeight = cellH,
            frameCount = alignedFrames.size,
            cadenceFps = cadenceFps,
            feetPivotYNorm = feetPivotY,
            hurtbox = hurtbox,
            hitbox = hitbox,
            anchors = anchors
        )

        val summary = "Foot-aligned $alignedCount/${alignedFrames.size} frames into uniform ${cellW}×${cellH}px cells (pivot Y=${String.format("%.2f", feetPivotY)}, ${cadenceFps.label})"

        return FootAlignedSliceResult(
            alignedSheet = alignedSheet,
            rigMetadata = rigMeta,
            framesAlignedCount = alignedCount,
            cellWidth = cellW,
            cellHeight = cellH,
            summary = summary
        )
    }

    /**
     * Compiles all 4 [HardwareVisualOutputTier] variants from a single [ThreeMapLayerSet].
     */
    fun compileHardwareVisualSplit(
        layers: ThreeMapLayerSet,
        selectedWheelHexes: List<String>,
        targetHardware: ConsoleHardwareSpec,
        rigMeta: RigMetadata
    ): List<CompiledHardwareVisualOutput> {
        val wheelLabel = if (selectedWheelHexes.isEmpty()) {
            "original palette"
        } else {
            "${selectedWheelHexes.size} wheel swap(s)"
        }
        return HardwareVisualOutputTier.entries.map { tier ->
            val rendered = ThreeMapRecolorEngine.compositeRecoloredSprite(
                layers = layers,
                selectedWheelHexes = selectedWheelHexes,
                tier = tier
            )
            val colorCount = countQuantizedUniqueColors(rendered)
            val budgetTag = if (colorCount <= targetHardware.maxOnScreenColors) "PASS" else "OVER"
            val summary = "${tier.displayName} • $colorCount colors ($budgetTag ${targetHardware.shortName}) • $wheelLabel • ${rigMeta.cadenceFps.label}"
            CompiledHardwareVisualOutput(
                tier = tier,
                bitmap = rendered,
                paletteColorCount = colorCount,
                technicalSummary = summary
            )
        }
    }

    /**
     * Builds the unified `master_pack_manifest.json` documenting the 4 visual layer paths,
     * embedded `rig_meta`, selected 20-Color Wheel hexes, and compiled hardware outputs.
     */
    fun buildMasterPackManifestJson(
        assetName: String,
        gameTag: String,
        groupTag: String,
        characterTag: String,
        selectedWheelHexes: List<String>,
        rigMeta: RigMetadata,
        targetHardware: ConsoleHardwareSpec,
        outputs: List<CompiledHardwareVisualOutput>
    ): String {
        val slug = assetName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "studio_asset" }

        val wheelArray = JSONArray().apply {
            selectedWheelHexes.forEach { put(it) }
        }
        val outputsArray = JSONArray().apply {
            outputs.forEach { out ->
                put(
                    JSONObject().apply {
                        put("tier", out.tier.name)
                        put("displayName", out.tier.displayName)
                        put("outputPath", "compiled/${slug}_${out.tier.name.lowercase()}.png")
                        put("width", out.bitmap.width)
                        put("height", out.bitmap.height)
                        put("paletteColorCount", out.paletteColorCount)
                        put("technicalSummary", out.technicalSummary)
                    }
                )
            }
        }

        val root = JSONObject().apply {
            put("schema", "pixelforge.master_pack.v1")
            put("assetName", assetName)
            put("hierarchy", JSONObject().apply {
                put("gameTag", gameTag)
                put("groupTag", groupTag)
                put("characterTag", characterTag)
            })
            put("targetHardware", JSONObject().apply {
                put("id", targetHardware.id)
                put("name", targetHardware.name)
                put("shortName", targetHardware.shortName)
                put("maxOnScreenColors", targetHardware.maxOnScreenColors)
            })
            put("selectedColorWheelHexes", wheelArray)
            put("visualLayers", JSONObject().apply {
                put("colorIdMap", "layers/$slug/color_id.png")
                put("inkShadowMap", "layers/$slug/ink_shadow.png")
                put("matEmissiveMap", "layers/$slug/mat_emissive.png")
                put("depthNormMap", "layers/$slug/depth_norm.png")
            })
            put("rig_meta", rigMeta.toJsonObject())
            put("compiledHardwareOutputs", outputsArray)
        }
        return root.toString(2)
    }

    private fun findTightOpaqueBounds(bitmap: Bitmap): TightBounds? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null
        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        for (y in 0 until h) {
            val rowOff = y * w
            for (x in 0 until w) {
                if (Color.alpha(px[rowOff + x]) >= ALPHA_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        return if (maxX >= minX && maxY >= minY) {
            TightBounds(minX, minY, maxX, maxY)
        } else {
            null
        }
    }

    private fun countQuantizedUniqueColors(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return 0
        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)
        val buckets = HashSet<Int>()
        for (c in px) {
            if (Color.alpha(c) < ALPHA_THRESHOLD) continue
            val r5 = Color.red(c) shr 3
            val g5 = Color.green(c) shr 3
            val b5 = Color.blue(c) shr 3
            buckets.add((r5 shl 10) or (g5 shl 5) or b5)
        }
        return buckets.size
    }
}
