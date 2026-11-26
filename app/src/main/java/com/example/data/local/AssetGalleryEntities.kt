package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.ObjParser
import com.example.model.sprite.SpriteSheetData
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Room Entity representing a user-created or curated Asset Gallery / Exhibition.
 */
@Entity(tableName = "asset_galleries")
data class AssetGalleryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String,
    val themeTag: String = "SHOWCASE",
    val accentColorHex: String = "#00E5FF",
    val layoutStyle: String = "GRID_2COL", // "GRID_2COL", "CAROUSEL", "COMPACT_LIST"
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Room Entity representing an individual 3D Model, 2D Spritesheet, or 3D+2D Pair curated inside an [AssetGalleryEntity].
 */
@Entity(
    tableName = "asset_gallery_items",
    foreignKeys = [
        ForeignKey(
            entity = AssetGalleryEntity::class,
            parentColumns = ["id"],
            childColumns = ["galleryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["galleryId"])]
)
data class AssetGalleryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val galleryId: Long,
    val title: String,
    val characterTag: String,
    val assetType: String, // "MODEL_3D", "SPRITE_2D", "FUSED_3D_2D", "UNIFIED_PAIR", "GEMINI_VARIANT"
    val sourceOrigin: String,
    val hardwareTarget: String,
    val metricsSummary: String,
    val curatorNote: String = "",
    val objMeshData: String? = null,
    val meshFilePath: String = "",
    val spritePngBytes: ByteArray? = null,
    val spriteColumns: Int = 1,
    val spriteRows: Int = 1,
    val addedAtMillis: Long = System.currentTimeMillis()
) {
    fun toMesh3D(context: Context? = null): Mesh3D? {
        val rawObj = objMeshData?.takeIf { it.isNotBlank() }
            ?: MeshDiskStorage.readMeshObj(meshFilePath, context)?.takeIf { it.isNotBlank() }
            ?: return null
        return try {
            ObjParser.parse(
                objContent = rawObj,
                name = title,
                sourceKindLabel = sourceOrigin,
                sourceSizeLabel = "Gallery #$galleryId"
            )
        } catch (_: Exception) {
            null
        }
    }

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
                sourceSizeLabel = "Gallery #$galleryId"
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssetGalleryItemEntity) return false
        return id == other.id &&
                galleryId == other.galleryId &&
                title == other.title &&
                characterTag == other.characterTag &&
                assetType == other.assetType &&
                sourceOrigin == other.sourceOrigin &&
                hardwareTarget == other.hardwareTarget &&
                metricsSummary == other.metricsSummary &&
                curatorNote == other.curatorNote &&
                objMeshData == other.objMeshData &&
                meshFilePath == other.meshFilePath &&
                (spritePngBytes contentEquals other.spritePngBytes) &&
                spriteColumns == other.spriteColumns &&
                spriteRows == other.spriteRows &&
                addedAtMillis == other.addedAtMillis
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + galleryId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + assetType.hashCode()
        result = 31 * result + (objMeshData?.hashCode() ?: 0)
        result = 31 * result + meshFilePath.hashCode()
        result = 31 * result + (spritePngBytes?.contentHashCode() ?: 0)
        result = 31 * result + addedAtMillis.hashCode()
        return result
    }

    companion object {
        fun fromStudioAssets(
            galleryId: Long,
            title: String,
            characterTag: String,
            assetType: String,
            sourceOrigin: String,
            hardwareTarget: String,
            metricsSummary: String,
            curatorNote: String = "",
            mesh: Mesh3D? = null,
            spriteSheet: SpriteSheetData? = null,
            addedAtMillis: Long = System.currentTimeMillis()
        ): AssetGalleryItemEntity {
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

            return AssetGalleryItemEntity(
                galleryId = galleryId,
                title = title,
                characterTag = characterTag,
                assetType = assetType,
                sourceOrigin = sourceOrigin,
                hardwareTarget = hardwareTarget,
                metricsSummary = metricsSummary,
                curatorNote = curatorNote,
                objMeshData = serializedObj,
                spritePngBytes = encodedSpriteBytes,
                spriteColumns = spriteSheet?.columns ?: 1,
                spriteRows = spriteSheet?.rows ?: 1,
                addedAtMillis = addedAtMillis
            )
        }
    }
}

/**
 * Combined UI & domain projection pairing an [AssetGalleryEntity] with its curated [items].
 */
data class AssetGalleryWithItems(
    val gallery: AssetGalleryEntity,
    val items: List<AssetGalleryItemEntity>
)
