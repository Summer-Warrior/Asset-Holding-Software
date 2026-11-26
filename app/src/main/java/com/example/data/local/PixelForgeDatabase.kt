package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database for PixelForge Studio storing generated and imported
 * 3D models, 2D spritesheets, 3D+2D hybrid assets, Gemini AI variants, and Asset Galleries.
 */
@Database(
    entities = [
        AssetHistoryEntity::class,
        AssetGalleryEntity::class,
        AssetGalleryItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PixelForgeDatabase : RoomDatabase() {

    abstract fun assetHistoryDao(): AssetHistoryDao

    abstract fun assetGalleryDao(): AssetGalleryDao

    companion object {
        @Volatile
        private var INSTANCE: PixelForgeDatabase? = null

        /**
         * Explicit non-destructive migration from schema v2 -> v3 adding the `meshFilePath`
         * column so large `.OBJ` meshes (> 256 KB) can be stored on disk without wiping
         * existing user galleries or history records.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE asset_history ADD COLUMN meshFilePath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE asset_gallery_items ADD COLUMN meshFilePath TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): PixelForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PixelForgeDatabase::class.java,
                    "pixelforge_asset_history.db"
                )
                    .addMigrations(MIGRATION_2_3)
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
                .addMigrations(MIGRATION_2_3)
                .allowMainThreadQueries()
                .build()
        }
    }
}
