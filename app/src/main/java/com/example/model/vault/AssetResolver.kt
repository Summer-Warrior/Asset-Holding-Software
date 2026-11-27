package com.example.model.vault

import org.json.JSONObject
import kotlin.math.abs

/**
 * Named-slot skin manifest decoupling runtime game/app code from external art assets.
 * No third-party or game-specific media needs to be committed into Git: slots resolve dynamically
 * from the active [SkinManifest] or fall back safely to [ResolvedAsset.GeometricPlaceholder].
 */
data class SkinManifest(
    val skinId: String,
    val displayName: String,
    val gameTag: String = "General Studio",
    val slotBindings: Map<String, String> = emptyMap()
) {
    fun toJsonString(): String {
        val bindingsObj = JSONObject()
        slotBindings.entries.sortedBy { it.key }.forEach { (slot, path) ->
            bindingsObj.put(slot, path)
        }
        return JSONObject().apply {
            put("skinId", skinId)
            put("displayName", displayName)
            put("gameTag", gameTag)
            put("slotBindings", bindingsObj)
        }.toString(2)
    }

    companion object {
        val DEFAULT_CLEAN_ROOM = SkinManifest(
            skinId = "clean-room-default",
            displayName = "Clean-Room Geometric Default",
            gameTag = "General Studio",
            slotBindings = emptyMap()
        )

        fun fromJsonString(json: String): SkinManifest {
            val root = JSONObject(json)
            val skinId = root.optString("skinId", "clean-room-default").ifBlank { "clean-room-default" }
            val displayName = root.optString("displayName", skinId).ifBlank { skinId }
            val gameTag = root.optString("gameTag", "General Studio").ifBlank { "General Studio" }
            val bindings = linkedMapOf<String, String>()
            val bindingsObj = root.optJSONObject("slotBindings")
            if (bindingsObj != null) {
                val keys = bindingsObj.keys()
                while (keys.hasNext()) {
                    val slot = keys.next()
                    val path = bindingsObj.optString(slot, "").trim()
                    if (slot.isNotBlank() && path.isNotEmpty()) {
                        bindings[slot] = path
                    }
                }
            }
            return SkinManifest(
                skinId = skinId,
                displayName = displayName,
                gameTag = gameTag,
                slotBindings = bindings
            )
        }
    }
}

/**
 * Resolution result from [AssetResolver.resolve].
 * Guarantees every named slot returns either a verified [BoundAsset] or a safe
 * [GeometricPlaceholder] so missing media packs never crash the UI or game engine.
 */
sealed interface ResolvedAsset {
    val slotName: String

    data class BoundAsset(
        override val slotName: String,
        val assetPath: String,
        val skinId: String
    ) : ResolvedAsset

    data class GeometricPlaceholder(
        override val slotName: String,
        val fallbackColorHex: String
    ) : ResolvedAsset
}

/**
 * Named-slot asset resolver enforcing strict code-content separation.
 */
class AssetResolver(
    private var activeManifest: SkinManifest = SkinManifest.DEFAULT_CLEAN_ROOM
) {
    private val placeholderPalette = listOf(
        "#00E5FF",
        "#FF6D00",
        "#7C4DFF",
        "#00E676",
        "#FFD600",
        "#FF4081",
        "#40C4FF",
        "#69F0AE"
    )

    fun setActiveManifest(manifest: SkinManifest) {
        activeManifest = manifest
    }

    fun getActiveManifest(): SkinManifest = activeManifest

    /**
     * Resolves [slotName] against [activeManifest.slotBindings].
     * Always returns [ResolvedAsset.GeometricPlaceholder] on any missing or blank binding.
     */
    fun resolve(slotName: String): ResolvedAsset {
        val normalizedSlot = slotName.trim()
        val boundPath = activeManifest.slotBindings[normalizedSlot]?.trim()
        if (!boundPath.isNullOrEmpty()) {
            return ResolvedAsset.BoundAsset(
                slotName = normalizedSlot,
                assetPath = boundPath,
                skinId = activeManifest.skinId
            )
        }
        val colorIdx = abs(normalizedSlot.hashCode()) % placeholderPalette.size
        return ResolvedAsset.GeometricPlaceholder(
            slotName = normalizedSlot.ifEmpty { "unnamed_slot" },
            fallbackColorHex = placeholderPalette[colorIdx]
        )
    }
}
