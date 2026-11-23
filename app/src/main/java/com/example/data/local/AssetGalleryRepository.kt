package com.example.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository mediating between Room's [AssetGalleryDao] and ViewModels.
 */
class AssetGalleryRepository(
    private val assetGalleryDao: AssetGalleryDao
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

    suspend fun deleteGallery(galleryId: Long) =
        assetGalleryDao.deleteGalleryById(galleryId)

    suspend fun addItemToGallery(item: AssetGalleryItemEntity): Long =
        assetGalleryDao.insertGalleryItem(item)

    suspend fun removeItemFromGallery(itemId: Long) =
        assetGalleryDao.deleteGalleryItemById(itemId)

    suspend fun getGalleryCount(): Int =
        assetGalleryDao.getGalleryCount()
}
