package com.example.model.library

import android.graphics.Bitmap
import com.example.model.model3d.Mesh3D
import com.example.model.sprite.SpriteSheetData

enum class LibraryAssetType(val label: String) {
    MODEL_3D("3D Model"),
    SPRITE_2D("2D Sprite"),
    UNIFIED_PAIR("3D + Sprite Pair")
}

data class SavedLibraryAsset(
    val id: String,
    val title: String,
    val characterTag: String,
    val type: LibraryAssetType,
    val mesh: Mesh3D? = null,
    val spriteSheet: SpriteSheetData? = null,
    val textureBitmap: Bitmap? = null,
    val inspectedYawDegrees: Float = 40f,
    val inspectedPitchDegrees: Float = 20f,
    val inspectedSpriteRotationDegrees: Float = 0f,
    val hardwareBadge: String = "PS1",
    val metricsSummary: String = "",
    val sourceBadge: String = "STUDIO",
    val formatSummary: String = "Unbounded Stream"
)
