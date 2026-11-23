package com.example.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Repository mediating between the Room AssetHistoryDao and ViewModels.
 */
class AssetHistoryRepository(
    private val assetHistoryDao: AssetHistoryDao
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

    suspend fun recordGeneratedAsset(entity: AssetHistoryEntity): Long =
        assetHistoryDao.insertAsset(entity)

    suspend fun deleteHistoryAsset(id: Long) =
        assetHistoryDao.deleteAssetById(id)

    suspend fun clearAllHistory() =
        assetHistoryDao.clearAllHistory()

    suspend fun getHistoryCount(): Int =
        assetHistoryDao.getHistoryCount()
}
