package com.example.model.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure-Kotlin 16-bit PCM synthesizer, WAV RIFF codec, waveform analyzer, and
 * 8-profile Hardware Soundchip DSP Split engine (zero external native dependencies).
 */
object AudioSynthAndDspEngine {

    private const val TAG = "AudioSynthAndDspEngine"
    private val playbackLock = Any()
    private var activeTrack: AudioTrack? = null

    /**
     * Synthesizes a pitch-swept FM/pulse + deterministic noise-mixed creature/character cry
     * with a clean attack/release envelope.
     */
    fun synthesizeProceduralCryPcm(
        spec: ProceduralCrySpec,
        sampleRateHz: Int = 22050
    ): ShortArray {
        val safeRate = sampleRateHz.coerceIn(8000, 48000)
        val totalSamples = ((spec.durationMs.coerceIn(60, 4000) / 1000.0) * safeRate)
            .roundToInt()
            .coerceAtLeast(128)
        val floatBuf = FloatArray(totalSamples)
        var carrierPhase = 0.0
        var modPhase = 0.0
        // Deterministic LCG seeded by speciesKey so identical specs produce identical cries
        var lcgState = (spec.speciesKey.hashCode().toLong() and 0xFFFFFFFFL) xor 0x5DEECE66DL
        val noiseMix = spec.noiseMix.coerceIn(0f, 1f)
        val attackSamples = max(1, (totalSamples * 0.08f).roundToInt())
        val releaseStart = (totalSamples * 0.82f).roundToInt().coerceIn(attackSamples, totalSamples - 1)
        val releaseLen = max(1, totalSamples - releaseStart)

        for (i in 0 until totalSamples) {
            val tNorm = i.toFloat() / totalSamples.toFloat()
            val env = when {
                i < attackSamples -> i.toFloat() / attackSamples.toFloat()
                i >= releaseStart -> ((totalSamples - i).toFloat() / releaseLen.toFloat()).coerceIn(0f, 1f)
                else -> 1f
            }
            val baseFreq = spec.baseFreqHz + (spec.endFreqHz - spec.baseFreqHz) * tNorm
            modPhase += (2.0 * PI * spec.modulationHz.coerceAtLeast(0f)) / safeRate
            val fmOffset = sin(modPhase) * (baseFreq * 0.14)
            val instFreq = (baseFreq + fmOffset).coerceIn(45.0, safeRate * 0.45)
            carrierPhase += (2.0 * PI * instFreq) / safeRate

            val sineCarrier = sin(carrierPhase).toFloat()
            val pulseWave = if (sin(carrierPhase) >= 0.25) 0.85f else -0.85f
            val tonal = sineCarrier * 0.55f + pulseWave * 0.45f

            lcgState = (lcgState * 1664525L + 1013904223L) and 0xFFFFFFFFL
            val noiseSample = ((lcgState.toFloat() / 2147483648.0f) - 1.0f)

            floatBuf[i] = (tonal * (1f - noiseMix) + noiseSample * noiseMix) * env
        }

        val rawPcm = floatBufferToPcm(floatBuf)
        return normalizePeakPcm(rawPcm, targetPeakRatio = 0.92f)
    }

    /**
     * Renders a multi-channel [ScoreSequenceSpec] cleanly into normalized 16-bit mono PCM:
     * - Channel 0: 25% duty pulse wave
     * - Channel 1: triangle bass
     * - Channel 2+: bright lead square/harmonic blend
     */
    fun synthesizeScoreSequencePcm(
        spec: ScoreSequenceSpec,
        sampleRateHz: Int = 22050
    ): ShortArray {
        val safeRate = sampleRateHz.coerceIn(8000, 48000)
        val safeBpm = spec.bpm.coerceIn(40, 300)
        val secondsPerBeat = 60.0 / safeBpm.toDouble()
        val maxBeat = spec.notes.maxOfOrNull { it.startBeat + it.durationBeats }?.coerceAtLeast(0.5f) ?: 1.0f
        val totalSamples = (maxBeat * secondsPerBeat * safeRate)
            .roundToInt()
            .coerceIn(256, safeRate * 30)

        val mix = FloatArray(totalSamples)

        spec.notes.forEach { note ->
            val startIdx = (note.startBeat * secondsPerBeat * safeRate).roundToInt().coerceIn(0, totalSamples - 1)
            val lengthSamples = (note.durationBeats * secondsPerBeat * safeRate).roundToInt().coerceAtLeast(16)
            val endIdx = min(totalSamples, startIdx + lengthSamples)
            val freqHz = 440.0 * 2.0.pow((note.midiNote - 69) / 12.0)
            val vel = note.velocity.coerceIn(0.05f, 1.0f) * 0.42f
            val attackLen = max(8, min(lengthSamples / 6, (safeRate * 0.012f).roundToInt()))
            val releaseLen = max(12, min(lengthSamples / 4, (safeRate * 0.035f).roundToInt()))

            for (i in startIdx until endIdx) {
                val localIdx = i - startIdx
                val remaining = endIdx - i
                val env = when {
                    localIdx < attackLen -> localIdx.toFloat() / attackLen.toFloat()
                    remaining < releaseLen -> remaining.toFloat() / releaseLen.toFloat()
                    else -> 1f
                }
                val cyclePos = ((localIdx.toDouble() * freqHz) / safeRate) % 1.0
                val sample = when (note.channel % 3) {
                    0 -> if (cyclePos < 0.25) 0.9f else -0.9f // 25% duty pulse
                    1 -> (4.0 * abs(cyclePos - 0.5) - 1.0).toFloat() // Triangle bass
                    else -> {
                        val sq = if (cyclePos < 0.5) 0.75f else -0.75f
                        val harm = sin(2.0 * PI * cyclePos * 2.0).toFloat() * 0.25f
                        sq + harm
                    }
                }
                mix[i] += sample * vel * env
            }
        }

        val rawPcm = floatBufferToPcm(mix)
        return normalizePeakPcm(rawPcm, targetPeakRatio = 0.90f)
    }

    /**
     * Applies genuine hardware-specific DSP transforms for the chosen [SoundchipDspProfile].
     */
    fun applyHardwareSoundchipDsp(
        pcm: ShortArray,
        inputRateHz: Int,
        profile: SoundchipDspProfile
    ): ShortArray {
        if (pcm.isEmpty()) return ShortArray(0)
        val safeInputRate = inputRateHz.coerceIn(8000, 96000)

        return when (profile) {
            SoundchipDspProfile.MSU1_CD_PCM -> pcm.copyOf()

            SoundchipDspProfile.DMG_GB_4CH -> {
                // Sample-and-hold decimation @ 16 kHz + 4-bit DAC crushing (16 amplitude steps)
                val decimated = applySampleAndHoldDecimation(pcm, safeInputRate, profile.targetSampleRateHz)
                applyBitDepthCrush(decimated, bits = 4)
            }

            SoundchipDspProfile.NES_2A03_5CH -> {
                // Sample-and-hold decimation @ 22.05 kHz + 8-bit APU stairstep
                val decimated = applySampleAndHoldDecimation(pcm, safeInputRate, profile.targetSampleRateHz)
                applyBitDepthCrush(decimated, bits = 8)
            }

            SoundchipDspProfile.GBA_DIRECTSOUND -> {
                // Authentic 18.157 kHz DMA DirectSound decimation + 8-bit quantization
                val decimated = applySampleAndHoldDecimation(pcm, safeInputRate, profile.targetSampleRateHz)
                applyBitDepthCrush(decimated, bits = 8)
            }

            SoundchipDspProfile.GENESIS_YM2612_FM -> {
                // 26 kHz FM ladder saturation harmonic wavefold + 12-bit DAC quantization
                val decimated = applySampleAndHoldDecimation(pcm, safeInputRate, profile.targetSampleRateHz)
                val folded = ShortArray(decimated.size)
                for (i in decimated.indices) {
                    val x = decimated[i] / 32768.0
                    val saturated = (sin(x * 1.48) * 0.86 + sin(x * 3.15) * 0.14).coerceIn(-1.0, 1.0)
                    folded[i] = (saturated * 32767.0).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                applyBitDepthCrush(folded, bits = 12)
            }

            SoundchipDspProfile.SNES_SPC700_BRR -> {
                // 3-tap Gaussian low-pass filter + warm short echo tap (~95 ms)
                val filtered = FloatArray(pcm.size)
                for (i in pcm.indices) {
                    val prev = if (i > 0) pcm[i - 1].toFloat() else pcm[i].toFloat()
                    val cur = pcm[i].toFloat()
                    val next = if (i + 1 < pcm.size) pcm[i + 1].toFloat() else pcm[i].toFloat()
                    filtered[i] = (prev * 0.25f + cur * 0.50f + next * 0.25f) / 32768f
                }
                val delaySamples = max(1, (safeInputRate * 0.095f).roundToInt())
                val out = FloatArray(pcm.size)
                for (i in filtered.indices) {
                    val echo = if (i >= delaySamples) out[i - delaySamples] * 0.28f else 0f
                    out[i] = (filtered[i] * 0.84f + echo).coerceIn(-1f, 1f)
                }
                floatBufferToPcm(out)
            }

            SoundchipDspProfile.PS1_SPU_ADPCM -> {
                // Subtle dual-tap hall reverb feedback delay (~62 ms + ~108 ms)
                val tap1 = max(1, (safeInputRate * 0.062f).roundToInt())
                val tap2 = max(1, (safeInputRate * 0.108f).roundToInt())
                val out = FloatArray(pcm.size)
                for (i in pcm.indices) {
                    val dry = pcm[i] / 32768f
                    val fb1 = if (i >= tap1) out[i - tap1] * 0.26f else 0f
                    val fb2 = if (i >= tap2) out[i - tap2] * 0.18f else 0f
                    out[i] = (dry * 0.78f + fb1 + fb2).coerceIn(-1f, 1f)
                }
                floatBufferToPcm(out)
            }

            SoundchipDspProfile.TECHNO_THAUMIC_CUSTOM -> {
                // Resonant harmonic Mana shimmer modulation + gentle crystalline sheen
                val out = FloatArray(pcm.size)
                val delayTap = max(1, (safeInputRate * 0.042f).roundToInt())
                for (i in pcm.indices) {
                    val dry = pcm[i] / 32768f
                    val lfo = 0.84f + 0.16f * sin(2.0 * PI * 7.5 * i / safeInputRate).toFloat()
                    val harmonicSheen = sin(dry * PI * 2.0).toFloat() * 0.14f
                    val shimmerEcho = if (i >= delayTap) out[i - delayTap] * 0.22f else 0f
                    out[i] = (dry * lfo + harmonicSheen + shimmerEcho).coerceIn(-1f, 1f)
                }
                floatBufferToPcm(out)
            }
        }
    }

    /**
     * Peak-normalizes PCM samples cleanly to [targetPeakRatio] (default 92% of full scale).
     */
    fun normalizePeakPcm(
        pcm: ShortArray,
        targetPeakRatio: Float = 0.92f
    ): ShortArray {
        if (pcm.isEmpty()) return ShortArray(0)
        var maxAbs = 0
        for (s in pcm) {
            val a = abs(s.toInt())
            if (a > maxAbs) maxAbs = a
        }
        if (maxAbs == 0) return pcm.copyOf()
        val targetPeak = (32767.0 * targetPeakRatio.coerceIn(0.1f, 0.99f))
        val gain = targetPeak / maxAbs.toDouble()
        return ShortArray(pcm.size) { idx ->
            (pcm[idx] * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /**
     * Computes normalized `0f..1f` peak bars for Compose Canvas waveform preview.
     */
    fun computeWaveformPeaks(
        pcm: ShortArray,
        bucketCount: Int = 48
    ): List<Float> {
        val count = bucketCount.coerceIn(8, 256)
        if (pcm.isEmpty()) return List(count) { 0f }
        val result = ArrayList<Float>(count)
        val step = pcm.size.toDouble() / count.toDouble()
        for (b in 0 until count) {
            val start = (b * step).toInt().coerceIn(0, pcm.size - 1)
            val end = max(start + 1, ((b + 1) * step).toInt().coerceAtMost(pcm.size))
            var peak = 0
            for (i in start until end) {
                val a = abs(pcm[i].toInt())
                if (a > peak) peak = a
            }
            result.add((peak / 32768f).coerceIn(0f, 1f))
        }
        return result
    }

    /**
     * Encodes mono 16-bit PCM into standard 44-byte RIFF/WAVE bytes.
     */
    fun encodePcmToWavBytes(
        pcm: ShortArray,
        sampleRateHz: Int = 22050
    ): ByteArray {
        val safeRate = sampleRateHz.coerceIn(8000, 96000)
        val channels = 1
        val bitsPerSample = 16
        val byteRate = safeRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)
        val dataSize = pcm.size * 2
        val riffChunkSize = 36 + dataSize

        val out = ByteArrayOutputStream(44 + dataSize)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(riffChunkSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM fmt chunk size
            putShort(1) // AudioFormat = 1 (PCM)
            putShort(channels.toShort())
            putInt(safeRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }
        out.write(header.array())

        val sampleBuf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) {
            sampleBuf.putShort(s)
        }
        out.write(sampleBuf.array())
        return out.toByteArray()
    }

    /**
     * Validates and decodes standard RIFF/WAVE 16-bit PCM bytes back into `(ShortArray, sampleRateHz)`.
     * Returns `null` on invalid magic bytes, unsupported encoding, or truncated chunks.
     */
    fun decodeWavBytesToPcm(wavBytes: ByteArray): Pair<ShortArray, Int>? {
        if (wavBytes.size < 44) return null
        val riff = String(wavBytes, 0, 4, Charsets.US_ASCII)
        val wave = String(wavBytes, 8, 4, Charsets.US_ASCII)
        if (riff != "RIFF" || wave != "WAVE") return null

        val buf = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        var foundFmt = false
        var sampleRate = 22050
        var channels = 1
        var bitsPerSample = 16
        var pcmBytesOffset = -1
        var pcmByteLength = 0

        while (offset + 8 <= wavBytes.size) {
            val chunkId = String(wavBytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = buf.getInt(offset + 4)
            val dataStart = offset + 8
            if (chunkSize < 0 || dataStart + chunkSize > wavBytes.size) {
                return null
            }
            when (chunkId) {
                "fmt " -> {
                    if (chunkSize < 16) return null
                    val audioFormat = buf.getShort(dataStart).toInt() and 0xFFFF
                    channels = buf.getShort(dataStart + 2).toInt() and 0xFFFF
                    sampleRate = buf.getInt(dataStart + 4)
                    bitsPerSample = buf.getShort(dataStart + 14).toInt() and 0xFFFF
                    if (audioFormat != 1 || bitsPerSample != 16 || channels !in 1..2 || sampleRate !in 4000..192000) {
                        return null
                    }
                    foundFmt = true
                }
                "data" -> {
                    pcmBytesOffset = dataStart
                    pcmByteLength = chunkSize
                    break
                }
            }
            // Chunks are word-aligned (padded to even byte boundary)
            offset = dataStart + chunkSize + (chunkSize and 1)
        }

        if (!foundFmt || pcmBytesOffset < 0 || pcmByteLength < 2) return null

        val totalShorts = pcmByteLength / 2
        return if (channels == 1) {
            val samples = ShortArray(totalShorts) { idx ->
                buf.getShort(pcmBytesOffset + idx * 2)
            }
            samples to sampleRate
        } else {
            // Downmix stereo 16-bit PCM to mono cleanly
            val frames = totalShorts / 2
            val mono = ShortArray(frames) { idx ->
                val l = buf.getShort(pcmBytesOffset + idx * 4).toInt()
                val r = buf.getShort(pcmBytesOffset + idx * 4 + 2).toInt()
                ((l + r) / 2).toShort()
            }
            mono to sampleRate
        }
    }

    /**
     * Plays 16-bit mono PCM safely via [AudioTrack] on a background thread.
     * Stops any previous track cleanly and logs warnings instead of crashing if audio hardware is unavailable.
     */
    fun playPcmSafely(
        pcm: ShortArray,
        sampleRateHz: Int = 22050
    ) {
        if (pcm.isEmpty()) return
        val safeRate = sampleRateHz.coerceIn(8000, 48000)
        val worker = Thread {
            var track: AudioTrack? = null
            try {
                stopActiveTrack()
                val minBufBytes = AudioTrack.getMinBufferSize(
                    safeRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val targetBytes = max(pcm.size * 2, max(minBufBytes, 4096))
                val builtTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(safeRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(targetBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track = builtTrack
                synchronized(playbackLock) {
                    activeTrack = builtTrack
                }
                builtTrack.write(pcm, 0, pcm.size)
                builtTrack.play()

                val durationMs = ((pcm.size.toDouble() / safeRate) * 1000.0).toLong().coerceIn(80L, 12_000L)
                Thread.sleep(durationMs + 120L)
            } catch (t: Throwable) {
                Log.w(TAG, "AudioTrack playback unavailable or interrupted: ${t.message}")
            } finally {
                synchronized(playbackLock) {
                    if (activeTrack === track) {
                        activeTrack = null
                    }
                }
                try {
                    track?.stop()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "AudioTrack stop skipped: ${e.message}")
                }
                try {
                    track?.release()
                } catch (t: Throwable) {
                    Log.w(TAG, "AudioTrack release warning: ${t.message}")
                }
            }
        }
        worker.isDaemon = true
        worker.name = "PixelForge-AudioAudition"
        worker.start()
    }

    /**
     * Stops any currently auditioning AudioTrack safely.
     */
    fun stopActiveTrack() {
        synchronized(playbackLock) {
            val prev = activeTrack
            activeTrack = null
            if (prev != null) {
                try {
                    if (prev.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        prev.stop()
                    }
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Previous AudioTrack stop warning: ${e.message}")
                }
                try {
                    prev.release()
                } catch (t: Throwable) {
                    Log.w(TAG, "Previous AudioTrack release warning: ${t.message}")
                }
            }
        }
    }

    /**
     * Creates 5 built-in audio vault presets: 3 procedural cries + 2 multi-channel chiptune `score_seq.json` tracks
     * with real precomputed waveform peaks.
     */
    fun createDefaultAudioPresets(): List<AudioAssetEntry> {
        val cry1 = ProceduralCrySpec(
            speciesKey = "kaelen_volt_dash",
            baseFreqHz = 620f,
            endFreqHz = 1180f,
            durationMs = 380,
            modulationHz = 28f,
            noiseMix = 0.14f
        )
        val cry2 = ProceduralCrySpec(
            speciesKey = "aegis_drake_roar",
            baseFreqHz = 340f,
            endFreqHz = 135f,
            durationMs = 540,
            modulationHz = 16f,
            noiseMix = 0.28f
        )
        val cry3 = ProceduralCrySpec(
            speciesKey = "mana_prism_chime",
            baseFreqHz = 880f,
            endFreqHz = 520f,
            durationMs = 460,
            modulationHz = 34f,
            noiseMix = 0.06f
        )

        val score1 = ScoreSequenceSpec(
            title = "Obsidian Foundry Theme",
            bpm = 138,
            notes = listOf(
                // Channel 0 pulse arpeggio
                ScoreSequenceNote(0.0f, 0.45f, 64, 0.85f, 0),
                ScoreSequenceNote(0.5f, 0.45f, 67, 0.80f, 0),
                ScoreSequenceNote(1.0f, 0.45f, 71, 0.88f, 0),
                ScoreSequenceNote(1.5f, 0.45f, 76, 0.90f, 0),
                ScoreSequenceNote(2.0f, 0.45f, 74, 0.85f, 0),
                ScoreSequenceNote(2.5f, 0.45f, 71, 0.82f, 0),
                ScoreSequenceNote(3.0f, 0.90f, 69, 0.92f, 0),
                // Channel 1 triangle bass
                ScoreSequenceNote(0.0f, 1.0f, 40, 0.90f, 1),
                ScoreSequenceNote(1.0f, 1.0f, 43, 0.88f, 1),
                ScoreSequenceNote(2.0f, 1.0f, 45, 0.90f, 1),
                ScoreSequenceNote(3.0f, 1.0f, 47, 0.92f, 1),
                // Channel 2 lead melody
                ScoreSequenceNote(0.0f, 0.9f, 76, 0.78f, 2),
                ScoreSequenceNote(1.0f, 0.9f, 79, 0.82f, 2),
                ScoreSequenceNote(2.0f, 0.9f, 81, 0.85f, 2),
                ScoreSequenceNote(3.0f, 0.9f, 76, 0.80f, 2)
            )
        )

        val score2 = ScoreSequenceSpec(
            title = "Aegis Spire Boss Rush",
            bpm = 156,
            notes = listOf(
                ScoreSequenceNote(0.0f, 0.4f, 69, 0.90f, 0),
                ScoreSequenceNote(0.5f, 0.4f, 72, 0.85f, 0),
                ScoreSequenceNote(1.0f, 0.4f, 76, 0.92f, 0),
                ScoreSequenceNote(1.5f, 0.4f, 75, 0.88f, 0),
                ScoreSequenceNote(2.0f, 0.4f, 76, 0.95f, 0),
                ScoreSequenceNote(2.5f, 0.4f, 81, 0.95f, 0),
                ScoreSequenceNote(3.0f, 0.9f, 79, 0.92f, 0),
                ScoreSequenceNote(0.0f, 0.5f, 45, 0.92f, 1),
                ScoreSequenceNote(0.5f, 0.5f, 45, 0.85f, 1),
                ScoreSequenceNote(1.0f, 0.5f, 48, 0.90f, 1),
                ScoreSequenceNote(1.5f, 0.5f, 47, 0.88f, 1),
                ScoreSequenceNote(2.0f, 1.0f, 45, 0.92f, 1),
                ScoreSequenceNote(3.0f, 1.0f, 43, 0.90f, 1),
                ScoreSequenceNote(0.0f, 1.4f, 81, 0.80f, 2),
                ScoreSequenceNote(1.5f, 1.4f, 83, 0.85f, 2),
                ScoreSequenceNote(3.0f, 0.9f, 84, 0.88f, 2)
            )
        )

        val pcmCry1 = synthesizeProceduralCryPcm(cry1, 22050)
        val pcmCry2 = synthesizeProceduralCryPcm(cry2, 22050)
        val pcmCry3 = synthesizeProceduralCryPcm(cry3, 22050)
        val pcmScore1 = synthesizeScoreSequencePcm(score1, 22050)
        val pcmScore2 = synthesizeScoreSequencePcm(score2, 22050)

        return listOf(
            AudioAssetEntry(
                id = "audio_preset_cry_kaelen",
                slotKey = "cry_kaelen_volt",
                title = "Kaelen Volt Dash Cry",
                category = AudioUsageCategory.PROCEDURAL_CRY,
                format = AudioAssetFormat.AUDIO_CUE_JSON,
                sampleRateHz = 22050,
                durationMs = cry1.durationMs,
                waveformPeaks = computeWaveformPeaks(pcmCry1),
                byteSize = cry1.toJsonString().toByteArray().size.toLong(),
                gameTag = "MPV In-Universe MMO",
                groupTag = "Audio & SFX",
                characterTag = "Kaelen",
                crySpec = cry1
            ),
            AudioAssetEntry(
                id = "audio_preset_cry_drake",
                slotKey = "cry_aegis_drake",
                title = "Aegis Drake Roar Cry",
                category = AudioUsageCategory.PROCEDURAL_CRY,
                format = AudioAssetFormat.AUDIO_CUE_JSON,
                sampleRateHz = 22050,
                durationMs = cry2.durationMs,
                waveformPeaks = computeWaveformPeaks(pcmCry2),
                byteSize = cry2.toJsonString().toByteArray().size.toLong(),
                gameTag = "PokéFusion Studio",
                groupTag = "Audio & SFX",
                characterTag = "Aegis Drake",
                crySpec = cry2
            ),
            AudioAssetEntry(
                id = "audio_preset_cry_prism",
                slotKey = "cry_mana_prism",
                title = "Mana Prism Resonance Cry",
                category = AudioUsageCategory.PROCEDURAL_CRY,
                format = AudioAssetFormat.AUDIO_CUE_JSON,
                sampleRateHz = 22050,
                durationMs = cry3.durationMs,
                waveformPeaks = computeWaveformPeaks(pcmCry3),
                byteSize = cry3.toJsonString().toByteArray().size.toLong(),
                gameTag = "Chronicle Nexus",
                groupTag = "Audio & SFX",
                characterTag = "Prism Wisp",
                crySpec = cry3
            ),
            AudioAssetEntry(
                id = "audio_preset_bgm_foundry",
                slotKey = "bgm_obsidian_foundry",
                title = "Obsidian Foundry Theme (score_seq)",
                category = AudioUsageCategory.BGM_LOOP,
                format = AudioAssetFormat.SCORE_SEQ_JSON,
                sampleRateHz = 22050,
                durationMs = ((pcmScore1.size.toDouble() / 22050.0) * 1000.0).roundToInt(),
                loopPoints = AudioLoopPoints(0, pcmScore1.size, loopEnabled = true),
                waveformPeaks = computeWaveformPeaks(pcmScore1),
                byteSize = score1.toJsonString().toByteArray().size.toLong(),
                gameTag = "General Studio",
                groupTag = "Audio & SFX",
                characterTag = "Studio",
                scoreSpec = score1
            ),
            AudioAssetEntry(
                id = "audio_preset_bgm_boss",
                slotKey = "bgm_aegis_boss_rush",
                title = "Aegis Spire Boss Rush (score_seq)",
                category = AudioUsageCategory.BGM_LOOP,
                format = AudioAssetFormat.SCORE_SEQ_JSON,
                sampleRateHz = 22050,
                durationMs = ((pcmScore2.size.toDouble() / 22050.0) * 1000.0).roundToInt(),
                loopPoints = AudioLoopPoints(0, pcmScore2.size, loopEnabled = true),
                waveformPeaks = computeWaveformPeaks(pcmScore2),
                byteSize = score2.toJsonString().toByteArray().size.toLong(),
                gameTag = "Aegis Grid SRW",
                groupTag = "Audio & SFX",
                characterTag = "Boss Theme",
                scoreSpec = score2
            )
        )
    }

    private fun applySampleAndHoldDecimation(
        pcm: ShortArray,
        inputRateHz: Int,
        targetRateHz: Int
    ): ShortArray {
        val ratio = (inputRateHz.toDouble() / targetRateHz.coerceAtLeast(4000).toDouble()).coerceAtLeast(1.0)
        if (ratio <= 1.02) return pcm.copyOf()
        val out = ShortArray(pcm.size)
        var acc = 0.0
        var held = pcm[0]
        for (i in pcm.indices) {
            if (i == 0 || acc >= ratio) {
                held = pcm[i]
                acc %= ratio
            }
            out[i] = held
            acc += 1.0
        }
        return out
    }

    private fun applyBitDepthCrush(pcm: ShortArray, bits: Int): ShortArray {
        val clampedBits = bits.coerceIn(2, 16)
        val levels = (1 shl clampedBits) - 1
        val out = ShortArray(pcm.size)
        for (i in pcm.indices) {
            val normalized = (pcm[i] + 32768.0) / 65535.0
            val quantized = (normalized * levels).roundToInt().toDouble() / levels.toDouble()
            val sample = (quantized * 65535.0 - 32768.0)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i] = sample.toShort()
        }
        return out
    }

    private fun floatBufferToPcm(buf: FloatArray): ShortArray =
        ShortArray(buf.size) { idx ->
            (buf[idx].coerceIn(-1f, 1f) * 32767f)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
}
