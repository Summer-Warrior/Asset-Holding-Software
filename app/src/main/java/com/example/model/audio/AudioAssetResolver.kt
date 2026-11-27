package com.example.model.audio

import com.example.model.vault.SkinManifest
import kotlin.math.abs

/**
 * Resolution result from [AudioAssetResolver.resolve].
 * Guarantees every named audio slot returns either a verified [BoundAudio] binding from the
 * active [SkinManifest] or a deterministic [ProceduralFallbackCry] so missing audio packs never crash.
 */
sealed interface ResolvedAudioAsset {
    val slotName: String

    data class BoundAudio(
        override val slotName: String,
        val assetPath: String,
        val format: AudioAssetFormat,
        val skinId: String
    ) : ResolvedAudioAsset

    data class ProceduralFallbackCry(
        override val slotName: String,
        val fallbackCrySpec: ProceduralCrySpec
    ) : ResolvedAudioAsset
}

/**
 * Named-slot audio resolver enforcing code-content separation across BGM, SFX, voices, and creature cries.
 */
class AudioAssetResolver(
    private var activeManifest: SkinManifest = SkinManifest.DEFAULT_CLEAN_ROOM
) {
    fun setActiveManifest(manifest: SkinManifest) {
        activeManifest = manifest
    }

    fun getActiveManifest(): SkinManifest = activeManifest

    /**
     * Resolves [slotName] against [activeManifest.slotBindings].
     * Always falls back cleanly to a deterministic [ResolvedAudioAsset.ProceduralFallbackCry] on miss.
     */
    fun resolve(slotName: String): ResolvedAudioAsset {
        val normalizedSlot = slotName.trim()
        val boundPath = activeManifest.slotBindings[normalizedSlot]?.trim()
        if (!boundPath.isNullOrEmpty()) {
            return ResolvedAudioAsset.BoundAudio(
                slotName = normalizedSlot,
                assetPath = boundPath,
                format = inferAudioFormat(boundPath),
                skinId = activeManifest.skinId
            )
        }

        val safeKey = normalizedSlot.ifEmpty { "unnamed_audio_slot" }
        val h = abs(safeKey.hashCode())
        val baseFreq = 260f + (h % 580).toFloat()
        val endFreq = 180f + ((h / 7) % 620).toFloat()
        val durationMs = 260 + ((h / 13) % 340)
        val modulationHz = 10f + ((h / 29) % 32).toFloat()
        val noiseMix = ((h / 43) % 25) / 100f

        return ResolvedAudioAsset.ProceduralFallbackCry(
            slotName = safeKey,
            fallbackCrySpec = ProceduralCrySpec(
                speciesKey = safeKey,
                baseFreqHz = baseFreq,
                endFreqHz = endFreq,
                durationMs = durationMs,
                modulationHz = modulationHz,
                noiseMix = noiseMix
            )
        )
    }

    private fun inferAudioFormat(path: String): AudioAssetFormat {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".wav") -> AudioAssetFormat.WAV_PCM
            lower.endsWith(".ogg") -> AudioAssetFormat.OGG_VORBIS
            lower.endsWith(".mid") || lower.endsWith(".midi") -> AudioAssetFormat.MIDI_SEQ
            lower.contains("score") && lower.endsWith(".json") -> AudioAssetFormat.SCORE_SEQ_JSON
            lower.endsWith(".json") -> AudioAssetFormat.AUDIO_CUE_JSON
            else -> AudioAssetFormat.WAV_PCM
        }
    }
}
