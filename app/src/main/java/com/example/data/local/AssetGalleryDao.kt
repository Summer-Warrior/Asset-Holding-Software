package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for creating, querying, updating, and curating Asset Galleries and their items.
 */
@Dao
interface AssetGalleryDao {

    @Query("SELECT * FROM asset_galleries ORDER BY createdAtMillis DESC, id DESC")
    fun getAllGalleriesFlow(): Flow<List<AssetGalleryEntity>>

    @Query("SELECT * FROM asset_gallery_items ORDER BY addedAtMillis DESC, id DESC")
    fun getAllGalleryItemsFlow(): Flow<List<AssetGalleryItemEntity>>

    @Query("SELECT * FROM asset_gallery_items WHERE galleryId = :galleryId ORDER BY addedAtMillis DESC, id DESC")
    fun getItemsForGalleryFlow(galleryId: Long): Flow<List<AssetGalleryItemEntity>>

    @Query("SELECT * FROM asset_galleries ORDER BY createdAtMillis DESC, id DESC")
    suspend fun getAllGalleriesSnapshot(): List<AssetGalleryEntity>

    @Query("SELECT * FROM asset_gallery_items ORDER BY addedAtMillis DESC, id DESC")
    suspend fun getAllGalleryItemsSnapshot(): List<AssetGalleryItemEntity>

    @Query("SELECT * FROM asset_gallery_items WHERE galleryId = :galleryId ORDER BY addedAtMillis DESC, id DESC")
    suspend fun getItemsForGallerySnapshot(galleryId: Long): List<AssetGalleryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGallery(gallery: AssetGalleryEntity): Long

    @Update
    suspend fun updateGallery(gallery: AssetGalleryEntity)

    @Query("DELETE FROM asset_galleries WHERE id = :galleryId")
    suspend fun deleteGalleryById(galleryId: Long)

    @Query("SELECT * FROM asset_gallery_items WHERE id = :itemId LIMIT 1")
    suspend fun getGalleryItemById(itemId: Long): AssetGalleryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGalleryItem(item: AssetGalleryItemEntity): Long

    @Query("UPDATE asset_gallery_items SET meshFilePath = :meshFilePath WHERE id = :itemId")
    suspend fun updateItemMeshFilePath(itemId: Long, meshFilePath: String)

    @Query("UPDATE asset_gallery_items SET gameTag = :gameTag, groupTag = :groupTag, characterTag = :characterTag WHERE id = :itemId")
    suspend fun updateItemTags(itemId: Long, gameTag: String, groupTag: String, characterTag: String)

    @Query("DELETE FROM asset_gallery_items WHERE id = :itemId")
    suspend fun deleteGalleryItemById(itemId: Long)

    @Query("SELECT COUNT(*) FROM asset_galleries")
    suspend fun getGalleryCount(): Int
}
