package com.example.model.audio

import org.json.JSONArray
import org.json.JSONObject

/**
 * Supported first-class audio asset formats in PixelForge's Audio Asset Vault.
 */
enum class AudioAssetFormat(
    val extension: String,
    val displayName: String
) {
    WAV_PCM("wav", "WAV PCM"),
    OGG_VORBIS("ogg", "OGG Vorbis"),
    MIDI_SEQ("mid", "MIDI Sequence"),
    SCORE_SEQ_JSON("json", "Score Sequence JSON"),
    AUDIO_CUE_JSON("json", "Audio Cue JSON")
}

/**
 * Runtime usage classification for vault audio assets.
 */
enum class AudioUsageCategory(val displayName: String) {
    BGM_LOOP("BGM Loop"),
    SFX_ONE_SHOT("SFX One-Shot"),
    VOICE_LINE("Voice Line"),
    PROCEDURAL_CRY("Procedural Cry")
}

/**
 * Hardware Soundchip DSP Split profiles.
 * Lets creators audition and compile a single master audio asset for 8 distinct retro and modern soundchips.
 */
enum class SoundchipDspProfile(
    val displayName: String,
    val targetSampleRateHz: Int,
    val bitDepth: Int,
    val technicalDescription: String
) {
    DMG_GB_4CH(
        displayName = "Game Boy DMG",
        targetSampleRateHz = 16000,
        bitDepth = 4,
        technicalDescription = "4-ch pulse/wave/noise • 4-bit DAC crush @ 16 kHz"
    ),
    NES_2A03_5CH(
        displayName = "NES 2A03",
        targetSampleRateHz = 22050,
        bitDepth = 8,
        technicalDescription = "5-ch APU pulse/triangle/DPCM • 8-bit stairstep @ 22.05 kHz"
    ),
    GENESIS_YM2612_FM(
        displayName = "Genesis YM2612 FM",
        targetSampleRateHz = 26000,
        bitDepth = 12,
        technicalDescription = "6-ch 4-op FM synthesis • 12-bit ladder harmonic fold @ 26 kHz"
    ),
    SNES_SPC700_BRR(
        displayName = "SNES SPC700 BRR",
        targetSampleRateHz = 32000,
        bitDepth = 16,
        technicalDescription = "8-ch BRR ADPCM • 3-tap Gaussian LPF + warm echo @ 32 kHz"
    ),
    GBA_DIRECTSOUND(
        displayName = "GBA DirectSound",
        targetSampleRateHz = 18157,
        bitDepth = 8,
        technicalDescription = "Dual DMA DirectSound FIFO • 8-bit decimation @ 18.157 kHz"
    ),
    PS1_SPU_ADPCM(
        displayName = "PS1 SPU Reverb",
        targetSampleRateHz = 44100,
        bitDepth = 16,
        technicalDescription = "24-ch SPU ADPCM • 16-bit hall feedback delay @ 44.1 kHz"
    ),
    MSU1_CD_PCM(
        displayName = "MSU-1 / Modern PCM",
        targetSampleRateHz = 44100,
        bitDepth = 16,
        technicalDescription = "Lossless 16-bit CD-quality PCM pass-through @ 44.1 kHz"
    ),
    TECHNO_THAUMIC_CUSTOM(
        displayName = "Techno-Thaumic Synth",
        targetSampleRateHz = 44100,
        bitDepth = 16,
        technicalDescription = "Resonant Mana-harmonic shimmer modulation @ 44.1 kHz"
    )
}

/**
 * Sample-accurate loop metadata for BGM tracks and sustained audio cues.
 */
data class AudioLoopPoints(
    val loopStartSample: Int = 0,
    val loopEndSample: Int = 0,
    val loopEnabled: Boolean = false
)

/**
 * Deterministic procedural creature/character cry synthesizer specification (`audio_cue.json`).
 */
data class ProceduralCrySpec(
    val speciesKey: String,
    val baseFreqHz: Float,
    val endFreqHz: Float,
    val durationMs: Int,
    val modulationHz: Float,
    val noiseMix: Float
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("schema", "pixelforge.procedural_cry.v1")
        put("speciesKey", speciesKey)
        put("baseFreqHz", baseFreqHz.toDouble())
        put("endFreqHz", endFreqHz.toDouble())
        put("durationMs", durationMs)
        put("modulationHz", modulationHz.toDouble())
        put("noiseMix", noiseMix.coerceIn(0f, 1f).toDouble())
    }

    fun toJsonString(): String = toJsonObject().toString(2)

    companion object {
        fun fromJsonString(json: String): ProceduralCrySpec {
            val root = JSONObject(json)
            return ProceduralCrySpec(
                speciesKey = root.optString("speciesKey", "studio_species").ifBlank { "studio_species" },
                baseFreqHz = root.optDouble("baseFreqHz", 440.0).toFloat().coerceIn(50f, 4000f),
                endFreqHz = root.optDouble("endFreqHz", 220.0).toFloat().coerceIn(50f, 4000f),
                durationMs = root.optInt("durationMs", 420).coerceIn(60, 4000),
                modulationHz = root.optDouble("modulationHz", 18.0).toFloat().coerceIn(0f, 120f),
                noiseMix = root.optDouble("noiseMix", 0.15).toFloat().coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * Single multi-channel chiptune note event inside a [ScoreSequenceSpec].
 */
data class ScoreSequenceNote(
    val startBeat: Float,
    val durationBeats: Float,
    val midiNote: Int,
    val velocity: Float,
    val channel: Int
)

/**
 * Lightweight deterministic multi-channel chiptune sequence (`score_seq.json`).
 */
data class ScoreSequenceSpec(
    val title: String,
    val bpm: Int,
    val notes: List<ScoreSequenceNote>
) {
    fun toJsonObject(): JSONObject {
        val notesArray = JSONArray()
        notes.forEach { n ->
            notesArray.put(
                JSONObject().apply {
                    put("startBeat", n.startBeat.toDouble())
                    put("durationBeats", n.durationBeats.toDouble())
                    put("midiNote", n.midiNote)
                    put("velocity", n.velocity.coerceIn(0f, 1f).toDouble())
                    put("channel", n.channel)
                }
            )
        }
        return JSONObject().apply {
            put("schema", "pixelforge.score_seq.v1")
            put("title", title)
            put("bpm", bpm)
            put("notes", notesArray)
        }
    }

    fun toJsonString(): String = toJsonObject().toString(2)

    companion object {
        fun fromJsonString(json: String): ScoreSequenceSpec {
            val root = JSONObject(json)
            val title = root.optString("title", "Untitled Sequence").ifBlank { "Untitled Sequence" }
            val bpm = root.optInt("bpm", 128).coerceIn(40, 300)
            val arr = root.optJSONArray("notes") ?: JSONArray()
            val parsedNotes = mutableListOf<ScoreSequenceNote>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parsedNotes.add(
                    ScoreSequenceNote(
                        startBeat = obj.optDouble("startBeat", 0.0).toFloat().coerceAtLeast(0f),
                        durationBeats = obj.optDouble("durationBeats", 0.5).toFloat().coerceAtLeast(0.05f),
                        midiNote = obj.optInt("midiNote", 60).coerceIn(12, 115),
                        velocity = obj.optDouble("velocity", 0.8).toFloat().coerceIn(0f, 1f),
                        channel = obj.optInt("channel", 0).coerceIn(0, 7)
                    )
                )
            }
            return ScoreSequenceSpec(
                title = title,
                bpm = bpm,
                notes = parsedNotes
            )
        }
    }
}

/**
 * First-class audio asset stored in PixelForge's Audio Vault (never as raw SQLite blobs).
 */
data class AudioAssetEntry(
    val id: String,
    val slotKey: String,
    val title: String,
    val category: AudioUsageCategory,
    val format: AudioAssetFormat,
    val sampleRateHz: Int,
    val durationMs: Int,
    val loopPoints: AudioLoopPoints = AudioLoopPoints(),
    val waveformPeaks: List<Float> = emptyList(),
    val filePath: String = "",
    val sha256Hex: String = "",
    val byteSize: Long = 0L,
    val gameTag: String = "General Studio",
    val groupTag: String = "Audio & SFX",
    val characterTag: String = "Studio",
    val crySpec: ProceduralCrySpec? = null,
    val scoreSpec: ScoreSequenceSpec? = null
) {
    /**
     * Exports either the embedded `score_seq.json`, `procedural_cry` spec, or a clean `audio_cue.json` manifest.
     */
    fun toExportSpecJson(): String {
        scoreSpec?.let { return it.toJsonString() }
        crySpec?.let { return it.toJsonString() }
        return JSONObject().apply {
            put("schema", "pixelforge.audio_cue.v1")
            put("id", id)
            put("slotKey", slotKey)
            put("title", title)
            put("category", category.name)
            put("format", format.name)
            put("sampleRateHz", sampleRateHz)
            put("durationMs", durationMs)
            put("loopPoints", JSONObject().apply {
                put("loopEnabled", loopPoints.loopEnabled)
                put("loopStartSample", loopPoints.loopStartSample)
                put("loopEndSample", loopPoints.loopEndSample)
            })
            put("filePath", filePath)
            put("sha256Hex", sha256Hex)
            put("byteSize", byteSize)
            put("hierarchy", JSONObject().apply {
                put("gameTag", gameTag)
                put("groupTag", groupTag)
                put("characterTag", characterTag)
            })
        }.toString(2)
    }
}
