package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository mediating between the Room [AssetHistoryDao] and ViewModels.
 * Automatically offloads `.OBJ` payloads exceeding 256 KB to `File(context.filesDir, "meshes/${id}.obj")`
 * with canonical path validation to prevent SQLite 2 MB `CursorWindow` crashes, and deletes
 * offloaded `.obj` files when history records are removed or cleared.
 */
class AssetHistoryRepository(
    private val assetHistoryDao: AssetHistoryDao,
    private val appContext: Context? = null
) {
    val allHistoryFlow: Flow<List<AssetHistoryEntity>> = assetHistoryDao.getAllHistoryFlow()

    fun getHistoryByTypeFlow(assetType: String): Flow<List<AssetHistoryEntity>> =
        assetHistoryDao.getHistoryByTypeFlow(assetType)

    fun searchHistoryFlow(query: String): Flow<List<AssetHistoryEntity>> =
        assetHistoryDao.searchHistoryFlow(query)

    suspend fun getAllHistorySnapshot(): List<AssetHistoryEntity> =
        assetHistoryDao.getAllHistorySnapshot()

    suspend fun getHistoryItemById(id: Long): AssetHistoryEntity? =
        assetHistoryDao.getHistoryItemById(id)

    suspend fun recordGeneratedAsset(entity: AssetHistoryEntity): Long {
        val ctx = appContext
        val rawObj = entity.objMeshData
        if (ctx != null && !rawObj.isNullOrEmpty() && MeshDiskStorage.exceedsInlineThreshold(rawObj)) {
            val insertedId = assetHistoryDao.insertAsset(
                entity.copy(objMeshData = "", meshFilePath = "")
            )
            val savedPath = MeshDiskStorage.writeLargeMeshObj(
                context = ctx,
                fileId = insertedId.toString(),
                objMeshData = rawObj
            )
            assetHistoryDao.updateMeshFilePath(insertedId, savedPath)
            return insertedId
        }
        return assetHistoryDao.insertAsset(entity)
    }

    suspend fun deleteHistoryAsset(id: Long) {
        val existing = assetHistoryDao.getHistoryItemById(id)
        if (existing != null && existing.meshFilePath.isNotBlank()) {
            MeshDiskStorage.deleteMeshObj(existing.meshFilePath, appContext)
        }
        assetHistoryDao.deleteAssetById(id)
    }

    suspend fun clearAllHistory() {
        val all = assetHistoryDao.getAllHistorySnapshot()
        all.forEach { entity ->
            if (entity.meshFilePath.isNotBlank()) {
                MeshDiskStorage.deleteMeshObj(entity.meshFilePath, appContext)
            }
        }
        assetHistoryDao.clearAllHistory()
    }

    suspend fun updateHistoryTags(
        id: Long,
        gameTag: String,
        groupTag: String,
        characterTag: String
    ) {
        assetHistoryDao.updateHistoryTags(
            id = id,
            gameTag = gameTag.trim().ifBlank { "General Studio" },
            groupTag = groupTag.trim().ifBlank { "Core Roster" },
            characterTag = characterTag.trim().ifBlank { "Studio Asset" }
        )
    }

    suspend fun getHistoryCount(): Int =
        assetHistoryDao.getHistoryCount()
}
