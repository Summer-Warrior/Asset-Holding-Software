package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for storing and retrieving generated and imported 3D models,
 * 2D spritesheets, 3D+2D hybrid fusions, and AI-enhanced assets so users
 * can view and restore their full asset history.
 */
@Dao
interface AssetHistoryDao {

    @Query("SELECT * FROM asset_history ORDER BY createdAtMillis DESC")
    fun getAllHistoryFlow(): Flow<List<AssetHistoryEntity>>

    @Query("SELECT * FROM asset_history ORDER BY createdAtMillis DESC")
    suspend fun getAllHistorySnapshot(): List<AssetHistoryEntity>

    @Query("SELECT * FROM asset_history WHERE assetType = :assetType ORDER BY createdAtMillis DESC")
    fun getHistoryByTypeFlow(assetType: String): Flow<List<AssetHistoryEntity>>

    @Query("SELECT * FROM asset_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryItemById(id: Long): AssetHistoryEntity?

    @Query(
        """
        SELECT * FROM asset_history 
        WHERE title LIKE '%' || :query || '%' 
           OR characterTag LIKE '%' || :query || '%' 
           OR gameTag LIKE '%' || :query || '%' 
           OR groupTag LIKE '%' || :query || '%' 
           OR sourceOrigin LIKE '%' || :query || '%'
        ORDER BY createdAtMillis DESC
        """
    )
    fun searchHistoryFlow(query: String): Flow<List<AssetHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(entity: AssetHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAssets(entities: List<AssetHistoryEntity>): List<Long>

    @Delete
    suspend fun deleteAsset(entity: AssetHistoryEntity)

    @Query("DELETE FROM asset_history WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    @Query("DELETE FROM asset_history")
    suspend fun clearAllHistory()

    @Query("UPDATE asset_history SET meshFilePath = :meshFilePath WHERE id = :id")
    suspend fun updateMeshFilePath(id: Long, meshFilePath: String)

    @Query("UPDATE asset_history SET gameTag = :gameTag, groupTag = :groupTag, characterTag = :characterTag WHERE id = :id")
    suspend fun updateHistoryTags(id: Long, gameTag: String, groupTag: String, characterTag: String)

    @Query("SELECT COUNT(*) FROM asset_history")
    suspend fun getHistoryCount(): Int
}
