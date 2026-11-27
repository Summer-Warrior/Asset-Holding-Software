package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ObjParser
import com.example.model.sprite.SpriteSheetData
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Room Database Entity storing generated, fused, AI-enhanced, and imported assets
 * (both 3D models and 2D spritesheets) so users can browse and restore their full creation history.
 */
@Entity(tableName = "asset_history")
data class AssetHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val characterTag: String,
    val assetType: String, // e.g., "MODEL_3D", "SPRITE_2D", "UNIFIED_PAIR", "FUSED_3D_2D", "GEMINI_VARIANT"
    val sourceOrigin: String, // e.g., "GEMINI AI", "3D+2D FUSION", "PROCEDURAL FORGE", "FILE IMPORT", "PHOTO IMPORT"
    val hardwareTarget: String,
    val metricsSummary: String,
    val objMeshData: String? = null,
    val meshFilePath: String = "",
    val spritePngBytes: ByteArray? = null,
    val spriteColumns: Int = 1,
    val spriteRows: Int = 1,
    val paletteHexCsv: String = "",
    val aiReviewNotes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val gameTag: String = "General Studio",
    val groupTag: String = "Core Roster"
) {
    /**
     * Reconstructs the [Mesh3D] stored in this Room history record (either inline or from `meshFilePath`), if present.
     */
    fun toMesh3D(context: Context? = null): Mesh3D? {
        val rawObj = objMeshData?.takeIf { it.isNotBlank() }
            ?: MeshDiskStorage.readMeshObj(meshFilePath, context)?.takeIf { it.isNotBlank() }
            ?: return null
        return try {
            ObjParser.parse(
                objContent = rawObj,
                name = title,
                sourceKindLabel = sourceOrigin,
                sourceSizeLabel = "Room History #${id}"
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reconstructs the [SpriteSheetData] stored in this Room history record, if present.
     */
    fun toSpriteSheetData(): SpriteSheetData? {
        val bytes = spritePngBytes?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            SpriteSheetData.fromBitmap(
                bitmap = bmp,
                name = title,
                columns = spriteColumns.coerceAtLeast(1),
                rows = spriteRows.coerceAtLeast(1),
                sourceKindLabel = sourceOrigin,
                sourceSizeLabel = "Room History #${id}"
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssetHistoryEntity) return false
        return id == other.id &&
                title == other.title &&
                characterTag == other.characterTag &&
                gameTag == other.gameTag &&
                groupTag == other.groupTag &&
                assetType == other.assetType &&
                sourceOrigin == other.sourceOrigin &&
                hardwareTarget == other.hardwareTarget &&
                metricsSummary == other.metricsSummary &&
                objMeshData == other.objMeshData &&
                meshFilePath == other.meshFilePath &&
                (spritePngBytes contentEquals other.spritePngBytes) &&
                spriteColumns == other.spriteColumns &&
                spriteRows == other.spriteRows &&
                paletteHexCsv == other.paletteHexCsv &&
                aiReviewNotes == other.aiReviewNotes &&
                createdAtMillis == other.createdAtMillis
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + characterTag.hashCode()
        result = 31 * result + gameTag.hashCode()
        result = 31 * result + groupTag.hashCode()
        result = 31 * result + assetType.hashCode()
        result = 31 * result + sourceOrigin.hashCode()
        result = 31 * result + (objMeshData?.hashCode() ?: 0)
        result = 31 * result + meshFilePath.hashCode()
        result = 31 * result + (spritePngBytes?.contentHashCode() ?: 0)
        result = 31 * result + createdAtMillis.hashCode()
        return result
    }

    companion object {
        /**
         * Serializes a 3D [Mesh3D] and/or 2D [SpriteSheetData] into a persistent [AssetHistoryEntity].
         */
        fun fromStudioAssets(
            title: String,
            characterTag: String,
            assetType: String,
            sourceOrigin: String,
            hardwareTarget: String,
            metricsSummary: String,
            mesh: Mesh3D? = null,
            spriteSheet: SpriteSheetData? = null,
            paletteHexList: List<String> = emptyList(),
            aiReviewNotes: String? = null,
            createdAtMillis: Long = System.currentTimeMillis(),
            gameTag: String = "General Studio",
            groupTag: String = "Core Roster"
        ): AssetHistoryEntity {
            val serializedObj = mesh?.let {
                try {
                    ObjParser.exportToObj(it)
                } catch (_: Exception) {
                    null
                }
            }

            val encodedSpriteBytes = spriteSheet?.let { sheet ->
                try {
                    val bmp = sheet.sourceBitmap
                    // Keep SQLite blob compact (< 256px max dimension) so Room CursorWindow never overflows
                    val maxDim = max(bmp.width, bmp.height)
                    val compactBmp = if (maxDim > 256) {
                        val ratio = 256f / maxDim.toFloat()
                        val w = (bmp.width * ratio).roundToInt().coerceAtLeast(sheet.columns.coerceAtLeast(1))
                        val h = (bmp.height * ratio).roundToInt().coerceAtLeast(sheet.rows.coerceAtLeast(1))
                        Bitmap.createScaledBitmap(bmp, w, h, false)
                    } else {
                        bmp
                    }
                    val out = ByteArrayOutputStream()
                    compactBmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                    out.toByteArray()
                } catch (_: Exception) {
                    null
                }
            }

            return AssetHistoryEntity(
                title = title,
                characterTag = characterTag,
                assetType = assetType,
                sourceOrigin = sourceOrigin,
                hardwareTarget = hardwareTarget,
                metricsSummary = metricsSummary,
                objMeshData = serializedObj,
                spritePngBytes = encodedSpriteBytes,
                spriteColumns = spriteSheet?.columns ?: 1,
                spriteRows = spriteSheet?.rows ?: 1,
                paletteHexCsv = paletteHexList.joinToString(","),
                aiReviewNotes = aiReviewNotes,
                createdAtMillis = createdAtMillis,
                gameTag = gameTag.ifBlank { "General Studio" },
                groupTag = groupTag.ifBlank { "Core Roster" }
            )
        }
    }
}
