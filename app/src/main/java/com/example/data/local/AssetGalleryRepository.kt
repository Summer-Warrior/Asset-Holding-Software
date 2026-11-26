package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository mediating between Room's [AssetGalleryDao] and ViewModels.
 * Automatically offloads `.OBJ` payloads exceeding 256 KB to `File(context.filesDir, "meshes/gallery_${id}.obj")`
 * with canonical path validation to avoid SQLite `CursorWindow` crashes, and cleans up offloaded `.obj`
 * files when an item or parent gallery is deleted.
 */
class AssetGalleryRepository(
    private val assetGalleryDao: AssetGalleryDao,
    private val appContext: Context? = null
) {
    val allGalleriesWithItemsFlow: Flow<List<AssetGalleryWithItems>> = combine(
        assetGalleryDao.getAllGalleriesFlow(),
        assetGalleryDao.getAllGalleryItemsFlow()
    ) { galleries, items ->
        val grouped = items.groupBy { it.galleryId }
        galleries.map { gallery ->
            AssetGalleryWithItems(
                gallery = gallery,
                items = grouped[gallery.id].orEmpty()
            )
        }
    }

    suspend fun getAllGalleriesWithItemsSnapshot(): List<AssetGalleryWithItems> {
        val galleries = assetGalleryDao.getAllGalleriesSnapshot()
        val grouped = assetGalleryDao.getAllGalleryItemsSnapshot().groupBy { it.galleryId }
        return galleries.map { gallery ->
            AssetGalleryWithItems(
                gallery = gallery,
                items = grouped[gallery.id].orEmpty()
            )
        }
    }

    suspend fun createGallery(gallery: AssetGalleryEntity): Long =
        assetGalleryDao.insertGallery(gallery)

    suspend fun updateGallery(gallery: AssetGalleryEntity) =
        assetGalleryDao.updateGallery(gallery)

    suspend fun deleteGallery(galleryId: Long) {
        val itemsToDelete = assetGalleryDao.getItemsForGallerySnapshot(galleryId)
        itemsToDelete.forEach { item ->
            if (item.meshFilePath.isNotBlank()) {
                MeshDiskStorage.deleteMeshObj(item.meshFilePath, appContext)
            }
        }
        assetGalleryDao.deleteGalleryById(galleryId)
    }

    suspend fun addItemToGallery(item: AssetGalleryItemEntity): Long {
        val ctx = appContext
        val rawObj = item.objMeshData
        if (ctx != null && !rawObj.isNullOrEmpty() && MeshDiskStorage.exceedsInlineThreshold(rawObj)) {
            val insertedId = assetGalleryDao.insertGalleryItem(
                item.copy(objMeshData = "", meshFilePath = "")
            )
            val savedPath = MeshDiskStorage.writeLargeMeshObj(
                context = ctx,
                fileId = "gallery_$insertedId",
                objMeshData = rawObj
            )
            assetGalleryDao.updateItemMeshFilePath(insertedId, savedPath)
            return insertedId
        }
        return assetGalleryDao.insertGalleryItem(item)
    }

    suspend fun removeItemFromGallery(itemId: Long) {
        val existing = assetGalleryDao.getGalleryItemById(itemId)
        if (existing != null && existing.meshFilePath.isNotBlank()) {
            MeshDiskStorage.deleteMeshObj(existing.meshFilePath, appContext)
        }
        assetGalleryDao.deleteGalleryItemById(itemId)
    }

    suspend fun getGalleryCount(): Int =
        assetGalleryDao.getGalleryCount()
}
