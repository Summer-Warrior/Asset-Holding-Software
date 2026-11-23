package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for PixelForge Studio storing generated and imported
 * 3D models, 2D spritesheets, 3D+2D hybrid assets, and Gemini AI variants.
 */
@Database(
    entities = [
        AssetHistoryEntity::class,
        AssetGalleryEntity::class,
        AssetGalleryItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PixelForgeDatabase : RoomDatabase() {

    abstract fun assetHistoryDao(): AssetHistoryDao

    abstract fun assetGalleryDao(): AssetGalleryDao

    companion object {
        @Volatile
        private var INSTANCE: PixelForgeDatabase? = null

        fun getDatabase(context: Context): PixelForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PixelForgeDatabase::class.java,
                    "pixelforge_asset_history.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Creates an in-memory database instance for fast Robolectric / JVM testing.
         */
        fun createInMemoryDatabase(context: Context): PixelForgeDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PixelForgeDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
