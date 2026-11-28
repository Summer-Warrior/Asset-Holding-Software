package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.audio.AudioSynthAndDspEngine
import com.example.model.audio.ProceduralCrySpec
import com.example.model.audio.ScoreSequenceNote
import com.example.model.audio.ScoreSequenceSpec
import com.example.model.model3d.Mesh3D
import com.example.model.model3d.PrimitiveShapeType
import com.example.model.model3d.Scratch3DBuilderEngine
import com.example.model.model3d.Scratch3DPart
import com.example.model.model3d.Scratch3DProject
import com.example.model.texture.ProceduralMaterialPattern
import com.example.model.texture.ScratchTextureEngine
import com.example.model.texture.ScratchTextureProject
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Target asset modality selectable in PixelForge's From-Scratch AI Creator.
 */
enum class AiBuildTargetKind(val label: String) {
    SPRITE_2D("2D Sprite / Sheet"),
    MODEL_3D("3D Model Blueprint"),
    TEXTURE_MATERIAL("Texture / Material"),
    AUDIO_CRY("Audio Cry (SFX)"),
    AUDIO_CHIPTUNE("16-Step Chiptune")
}

data class AiBuilt3DModelResult(
    val project: Scratch3DProject,
    val compiledMesh: Mesh3D,
    val modelUsed: String,
    val rawJson: String
)

data class AiBuiltTextureResult(
    val project: ScratchTextureProject,
    val exportBitmap: Bitmap,
    val modelUsed: String,
    val rawJson: String
)

data class AiBuiltAudioCryResult(
    val spec: ProceduralCrySpec,
    val pcm: ShortArray,
    val waveformPeaks: List<Float>,
    val sampleRateHz: Int,
    val modelUsed: String,
    val rawJson: String
)

data class AiBuiltChiptuneResult(
    val spec: ScoreSequenceSpec,
    val pcm: ShortArray,
    val waveformPeaks: List<Float>,
    val sampleRateHz: Int,
    val modelUsed: String,
    val rawJson: String
)

/**
 * Pure-Kotlin AI Multi-Asset Builder for 3D Model Blueprints, Procedural Textures/Materials,
 * Procedural Creature Cries, and 16-Step Chiptune Sequences.
 *
 * Dispatches JSON blueprint generation through [OmniAiRouter.generateTextOrJson] so every connected
 * text provider (Firebase Studio, OpenRouter, Direct Gemini, Claude, OpenAI, Local Ollama/LM Studio)
 * can build editable studio assets from scratch without silent template fallbacks.
 */
object AiMultiAssetBuilder {

    private const val TAG = "AiMultiAssetBuilder"
    private const val MAX_3D_PARTS = 16
    private const val MIN_CHIPTUNE_NOTES = 4
    private const val DEFAULT_AUDIO_SAMPLE_RATE = 22050

    private val jsonBlueprintModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.7f
            }
        )
    }

    /**
     * Strips optional markdown code fences (```json ... ```) and extracts the outer `{ ... }` JSON object.
     * Throws [IllegalArgumentException] if no balanced JSON object is present.
     */
    fun extractJsonObjectString(rawResponse: String): String {
        val trimmed = rawResponse.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("AI returned an empty response instead of JSON.")
        }

        // Strip leading/trailing markdown fences if present
        val unfenced = trimmed
            .replace(Regex("^```(?:json|JSON)?\\s*\\n?"), "")
            .replace(Regex("\\n?```\\s*$"), "")
            .trim()

        val startIdx = unfenced.indexOf('{')
        if (startIdx < 0) {
            throw IllegalArgumentException("AI response did not contain a JSON object ('{' missing).")
        }

        var depth = 0
        var inString = false
        var escaped = false
        for (i in startIdx until unfenced.length) {
            val ch = unfenced[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && inString) {
                escaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (ch == '{') {
                    depth++
                } else if (ch == '}') {
                    depth--
                    if (depth == 0) {
                        return unfenced.substring(startIdx, i + 1)
                    }
                }
            }
        }

        val lastBrace = unfenced.lastIndexOf('}')
        if (lastBrace > startIdx) {
            return unfenced.substring(startIdx, lastBrace + 1)
        }
        throw IllegalArgumentException("AI response contained an unclosed JSON object.")
    }

    // -------------------------------------------------------------------------
    // 1. AI 3D Model Blueprint Builder
    // -------------------------------------------------------------------------

    /**
     * Parses a strict JSON blueprint into an editable [Scratch3DProject] with 1..16 clamped parts.
     * Throws [IllegalArgumentException] if zero valid primitive parts are found (never falls back to stock meshes).
     */
    fun parse3DBlueprintJson(assetName: String, rawJson: String): Scratch3DProject {
        val jsonText = extractJsonObjectString(rawJson)
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing 3D blueprint JSON: ${e.message}")
            throw IllegalArgumentException("Invalid 3D blueprint JSON: ${e.message}", e)
        }

        val partsArray: JSONArray = root.optJSONArray("parts")
            ?: throw IllegalArgumentException("3D blueprint JSON is missing a 'parts' array.")

        val parsedParts = mutableListOf<Scratch3DPart>()
        for (i in 0 until partsArray.length()) {
            if (parsedParts.size >= MAX_3D_PARTS) break
            val obj = partsArray.optJSONObject(i) ?: continue
            val shapeRaw = obj.optString("shape", "").trim().uppercase()
            val shape = PrimitiveShapeType.entries.firstOrNull { it.name == shapeRaw }
            if (shape == null) {
                Log.w(TAG, "Skipping unknown 3D primitive shape '$shapeRaw' at index $i")
                continue
            }

            val partName = obj.optString("name", "${shape.label} #${parsedParts.size + 1}")
                .trim()
                .ifBlank { "${shape.label} #${parsedParts.size + 1}" }
                .take(36)

            val posX = obj.optDouble("posX", 0.0).toFloat().coerceIn(-1.5f, 1.5f)
            val posY = obj.optDouble("posY", 0.0).toFloat().coerceIn(-1.5f, 1.5f)
            val posZ = obj.optDouble("posZ", 0.0).toFloat().coerceIn(-1.5f, 1.5f)
            val scaleX = obj.optDouble("scaleX", 0.6).toFloat().coerceIn(0.1f, 1.8f)
            val scaleY = obj.optDouble("scaleY", 0.6).toFloat().coerceIn(0.1f, 1.8f)
            val scaleZ = obj.optDouble("scaleZ", 0.6).toFloat().coerceIn(0.1f, 1.8f)
            val rawRot = obj.optDouble("rotYDeg", 0.0).toFloat()
            val rotYDeg = ((rawRot % 360f) + 360f) % 360f
            val colorHex = obj.optString("colorHex", "#00E5FF")
            val colorLong = GeminiAssetForge.parseColorLongSafe(colorHex)
            val mirrorX = obj.optBoolean("mirrorX", false)

            parsedParts.add(
                Scratch3DPart(
                    id = "ai3d_${UUID.randomUUID().toString().take(8)}",
                    name = partName,
                    shape = shape,
                    posX = posX,
                    posY = posY,
                    posZ = posZ,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    scaleZ = scaleZ,
                    rotYDeg = rotYDeg,
                    colorLong = colorLong,
                    mirrorX = mirrorX
                )
            )
        }

        if (parsedParts.isEmpty()) {
            throw IllegalArgumentException(
                "3D blueprint JSON contained 0 valid primitive parts (allowed shapes: BOX, SPHERE, CYLINDER, CONE, PYRAMID, TORUS_RING, WEDGE_WING)."
            )
        }

        val resolvedName = assetName.trim()
            .ifBlank { root.optString("title", "").trim() }
            .ifBlank { "AI Built 3D Model" }
            .take(40)

        return Scratch3DProject(
            id = "ai3dproj_${UUID.randomUUID().toString().take(10)}",
            name = resolvedName,
            parts = parsedParts,
            selectedPartId = parsedParts.first().id
        )
    }

    suspend fun build3DModelFromPrompt(
        assetName: String,
        subjectDescription: String,
        sharedContextBlock: String = ""
    ): AiBuilt3DModelResult {
        val cleanSubject = subjectDescription.trim()
        require(cleanSubject.isNotBlank()) { "Describe the 3D model you want the AI to build." }

        val systemInstruction = """
            You are a 3D kitbash game asset architect. Respond ONLY with a single raw JSON object (no markdown commentary).
            Construct the requested subject from scratch using 4 to 14 distinct 3D primitives arranged cohesively in 3D space.
            Allowed shapes: BOX, SPHERE, CYLINDER, CONE, PYRAMID, TORUS_RING, WEDGE_WING.
            Use mirrorX=true for symmetrical limbs, wings, horns, pauldrons, or thrusters positioned at posX > 0.
            Ranges: posX/posY/posZ in [-1.5, 1.5], scaleX/scaleY/scaleZ in [0.1, 1.8], rotYDeg in [0, 360], colorHex as "#RRGGBB".
        """.trimIndent()

        val prompt = """
            Build a brand-new 3D kitbash blueprint for: "$cleanSubject"
            Asset Name: "${assetName.trim().ifBlank { cleanSubject.take(32) }}"
            Return strict JSON matching:
            {
              "title": "Short Asset Name",
              "parts": [
                {
                  "name": "Torso Core",
                  "shape": "BOX",
                  "posX": 0.0, "posY": 0.0, "posZ": 0.0,
                  "scaleX": 0.7, "scaleY": 0.8, "scaleZ": 0.5,
                  "rotYDeg": 0.0,
                  "colorHex": "#1E293B",
                  "mirrorX": false
                }
              ]
            }
        """.trimIndent()

        val response = invokeTextProvider(
            prompt = prompt,
            systemInstruction = systemInstruction,
            sharedContextBlock = sharedContextBlock,
            assetLabel = "3D model blueprint"
        )
        val project = parse3DBlueprintJson(assetName.ifBlank { cleanSubject.take(28) }, response.text)
        val compiledMesh = Scratch3DBuilderEngine.compileProjectToMesh3D(project).copy(
            sourceKindLabel = "AI BUILT 3D"
        )

        return AiBuilt3DModelResult(
            project = project,
            compiledMesh = compiledMesh,
            modelUsed = formatModelLabel(response),
            rawJson = extractJsonObjectString(response.text)
        )
    }

    // -------------------------------------------------------------------------
    // 2. AI Texture / Material Builder
    // -------------------------------------------------------------------------

    /**
     * Parses a strict JSON texture recipe into an editable [ScratchTextureProject] with optional pixel accents.
     */
    fun parseTextureRecipeJson(
        assetName: String,
        rawJson: String,
        sizePx: Int = 32
    ): ScratchTextureProject {
        val jsonText = extractJsonObjectString(rawJson)
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing texture recipe JSON: ${e.message}")
            throw IllegalArgumentException("Invalid texture recipe JSON: ${e.message}", e)
        }

        val patternRaw = root.optString("pattern", "").trim().uppercase()
        val pattern = ProceduralMaterialPattern.entries.firstOrNull { it.name == patternRaw }
            ?: throw IllegalArgumentException(
                "Texture recipe JSON must specify a valid 'pattern' (ENERGY_CIRCUIT, ARMOR_PLATE, ORGANIC_SCALES, DUNGEON_BRICK, or CEL_GRADIENT)."
            )

        val primaryHex = root.optString("primaryHex", "#161B26")
        val secondaryHex = root.optString("secondaryHex", "#334155")
        val accentHex = root.optString("accentHex", "#00E5FF")
        val mirrorX = root.optBoolean("mirrorX", false)
        val mirrorY = root.optBoolean("mirrorY", false)

        val resolvedName = assetName.trim()
            .ifBlank { root.optString("title", "").trim() }
            .ifBlank { "AI ${pattern.label} Texture" }
            .take(40)

        var project = ScratchTextureEngine.generateMaterialPattern(
            name = resolvedName,
            sizePx = sizePx,
            pattern = pattern,
            primaryColorInt = GeminiAssetForge.parseColorIntSafe(primaryHex),
            secondaryColorInt = GeminiAssetForge.parseColorIntSafe(secondaryHex),
            accentColorInt = GeminiAssetForge.parseColorIntSafe(accentHex)
        ).copy(
            mirrorX = mirrorX,
            mirrorY = mirrorY
        )

        val accentsArray = root.optJSONArray("pixelAccents")
        if (accentsArray != null) {
            val dim = project.sizePx
            val maxAccents = accentsArray.length().coerceAtMost(256)
            for (i in 0 until maxAccents) {
                val pt = accentsArray.optJSONObject(i) ?: continue
                if (!pt.has("x") || !pt.has("y")) continue
                val x = pt.optInt("x", 0).coerceIn(0, dim - 1)
                val y = pt.optInt("y", 0).coerceIn(0, dim - 1)
                val hex = pt.optString("hex", accentHex)
                val colorInt = GeminiAssetForge.parseColorIntSafe(hex)
                project = ScratchTextureEngine.paintTexturePixel(
                    project = project,
                    x = x,
                    y = y,
                    colorInt = colorInt,
                    brushSize = 1
                )
            }
        }

        return project
    }

    suspend fun buildTextureFromPrompt(
        assetName: String,
        subjectDescription: String,
        sizePx: Int = 32,
        sharedContextBlock: String = ""
    ): AiBuiltTextureResult {
        val cleanSubject = subjectDescription.trim()
        require(cleanSubject.isNotBlank()) { "Describe the texture or material you want the AI to build." }

        val systemInstruction = """
            You are a 2D/3D game texture artist. Respond ONLY with a single raw JSON object (no markdown commentary).
            Allowed pattern values: ENERGY_CIRCUIT, ARMOR_PLATE, ORGANIC_SCALES, DUNGEON_BRICK, CEL_GRADIENT.
            Provide high-contrast hex colors (#RRGGBB) and 4 to 24 optional pixelAccents within 0..${sizePx - 1}.
        """.trimIndent()

        val prompt = """
            Design a brand-new seamless ${sizePx}x${sizePx} game material texture for: "$cleanSubject"
            Asset Name: "${assetName.trim().ifBlank { cleanSubject.take(32) }}"
            Return strict JSON matching:
            {
              "title": "Short Material Name",
              "pattern": "ENERGY_CIRCUIT",
              "primaryHex": "#0F172A",
              "secondaryHex": "#1E293B",
              "accentHex": "#00E5FF",
              "mirrorX": true,
              "mirrorY": true,
              "pixelAccents": [
                {"x": 4, "y": 4, "hex": "#FACC15"}
              ]
            }
        """.trimIndent()

        val response = invokeTextProvider(
            prompt = prompt,
            systemInstruction = systemInstruction,
            sharedContextBlock = sharedContextBlock,
            assetLabel = "texture material recipe"
        )
        val project = parseTextureRecipeJson(
            assetName = assetName.ifBlank { cleanSubject.take(28) },
            rawJson = response.text,
            sizePx = sizePx
        )
        val exportBitmap = ScratchTextureEngine.toExportBitmap(project, outputDim = 128)

        return AiBuiltTextureResult(
            project = project,
            exportBitmap = exportBitmap,
            modelUsed = formatModelLabel(response),
            rawJson = extractJsonObjectString(response.text)
        )
    }

    // -------------------------------------------------------------------------
    // 3. AI Procedural Audio Cry Builder
    // -------------------------------------------------------------------------

    /**
     * Parses a strict JSON creature/SFX cry spec into a clamped [ProceduralCrySpec].
     */
    fun parseCrySpecJson(speciesKey: String, rawJson: String): ProceduralCrySpec {
        val jsonText = extractJsonObjectString(rawJson)
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing procedural cry JSON: ${e.message}")
            throw IllegalArgumentException("Invalid procedural cry JSON: ${e.message}", e)
        }

        if (!root.has("baseFreqHz") && !root.has("endFreqHz")) {
            throw IllegalArgumentException("Procedural cry JSON must include 'baseFreqHz' and 'endFreqHz'.")
        }

        val cleanKey = speciesKey.trim()
            .ifBlank { root.optString("speciesKey", "").trim() }
            .ifBlank { "ai_creature_cry" }
            .lowercase()
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .ifBlank { "ai_creature_cry" }

        val baseFreqHz = root.optDouble("baseFreqHz", 440.0).toFloat().coerceIn(80f, 2000f)
        val endFreqHz = root.optDouble("endFreqHz", 260.0).toFloat().coerceIn(80f, 2000f)
        val durationMs = root.optInt("durationMs", 460).coerceIn(120, 1500)
        val modulationHz = root.optDouble("modulationHz", 18.0).toFloat().coerceIn(0f, 80f)
        val noiseMix = root.optDouble("noiseMix", 0.18).toFloat().coerceIn(0f, 1f)

        return ProceduralCrySpec(
            speciesKey = cleanKey,
            baseFreqHz = baseFreqHz,
            endFreqHz = endFreqHz,
            durationMs = durationMs,
            modulationHz = modulationHz,
            noiseMix = noiseMix
        )
    }

    suspend fun buildAudioCryFromPrompt(
        assetName: String,
        subjectDescription: String,
        sharedContextBlock: String = ""
    ): AiBuiltAudioCryResult {
        val cleanSubject = subjectDescription.trim()
        require(cleanSubject.isNotBlank()) { "Describe the creature cry or retro SFX you want the AI to synthesize." }

        val systemInstruction = """
            You are a retro arcade & RPG soundchip SFX designer. Respond ONLY with a single raw JSON object (no markdown commentary).
            Ranges: baseFreqHz in [80, 2000], endFreqHz in [80, 2000], durationMs in [120, 1500], modulationHz in [0, 80], noiseMix in [0.0, 1.0].
        """.trimIndent()

        val prompt = """
            Synthesize a brand-new procedural creature cry / retro SFX specification for: "$cleanSubject"
            Asset Name: "${assetName.trim().ifBlank { cleanSubject.take(32) }}"
            Return strict JSON matching:
            {
              "speciesKey": "crystal_phoenix_roar",
              "baseFreqHz": 580.0,
              "endFreqHz": 1120.0,
              "durationMs": 540,
              "modulationHz": 26.0,
              "noiseMix": 0.22
            }
        """.trimIndent()

        val response = invokeTextProvider(
            prompt = prompt,
            systemInstruction = systemInstruction,
            sharedContextBlock = sharedContextBlock,
            assetLabel = "procedural cry SFX"
        )
        val keySeed = assetName.trim().ifBlank { cleanSubject.split(Regex("\\s+")).take(3).joinToString("_") }
        val spec = parseCrySpecJson(keySeed, response.text)
        val pcm = AudioSynthAndDspEngine.synthesizeProceduralCryPcm(spec, sampleRateHz = DEFAULT_AUDIO_SAMPLE_RATE)
        val peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm)

        return AiBuiltAudioCryResult(
            spec = spec,
            pcm = pcm,
            waveformPeaks = peaks,
            sampleRateHz = DEFAULT_AUDIO_SAMPLE_RATE,
            modelUsed = formatModelLabel(response),
            rawJson = extractJsonObjectString(response.text)
        )
    }

    // -------------------------------------------------------------------------
    // 4. AI 16-Step Chiptune Sequencer Builder
    // -------------------------------------------------------------------------

    /**
     * Parses a strict JSON 16-step chiptune score into a clamped [ScoreSequenceSpec].
     * Requires at least 4 valid notes so malformed responses fail cleanly.
     */
    fun parseScoreSequenceJson(title: String, rawJson: String): ScoreSequenceSpec {
        val jsonText = extractJsonObjectString(rawJson)
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing chiptune sequence JSON: ${e.message}")
            throw IllegalArgumentException("Invalid chiptune sequence JSON: ${e.message}", e)
        }

        val bpm = root.optInt("bpm", 132).coerceIn(80, 200)
        val notesArray = root.optJSONArray("notes")
            ?: throw IllegalArgumentException("Chiptune sequence JSON is missing a 'notes' array.")

        val parsedNotes = mutableListOf<ScoreSequenceNote>()
        val maxNotes = notesArray.length().coerceAtMost(96)
        for (i in 0 until maxNotes) {
            val obj = notesArray.optJSONObject(i) ?: continue
            if (!obj.has("midiNote")) continue
            val step = obj.optInt("step", i % 16).coerceIn(0, 15)
            val midiNote = obj.optInt("midiNote", 60).coerceIn(48, 84)
            val channel = obj.optInt("channel", 0).coerceIn(0, 2)
            val durationBeats = obj.optDouble("durationBeats", 0.25).toFloat().coerceIn(0.25f, 1.0f)
            val velocity = obj.optDouble("velocity", 0.8).toFloat().coerceIn(0.3f, 1.0f)

            parsedNotes.add(
                ScoreSequenceNote(
                    startBeat = step * 0.25f,
                    durationBeats = durationBeats,
                    midiNote = midiNote,
                    velocity = velocity,
                    channel = channel
                )
            )
        }

        if (parsedNotes.size < MIN_CHIPTUNE_NOTES) {
            throw IllegalArgumentException(
                "Chiptune sequence JSON must contain at least $MIN_CHIPTUNE_NOTES valid notes (found ${parsedNotes.size})."
            )
        }

        val resolvedTitle = title.trim()
            .ifBlank { root.optString("title", "").trim() }
            .ifBlank { "AI Chiptune Loop" }
            .take(40)

        return ScoreSequenceSpec(
            title = resolvedTitle,
            bpm = bpm,
            notes = parsedNotes.sortedBy { it.startBeat }
        )
    }

    suspend fun buildChiptuneFromPrompt(
        assetName: String,
        subjectDescription: String,
        sharedContextBlock: String = ""
    ): AiBuiltChiptuneResult {
        val cleanSubject = subjectDescription.trim()
        require(cleanSubject.isNotBlank()) { "Describe the 16-step chiptune theme or melody you want the AI to compose." }

        val systemInstruction = """
            You are a 16-bit / 32-bit arcade chiptune composer. Respond ONLY with a single raw JSON object (no markdown commentary).
            Compose a catchy 16-step (4-beat) multi-channel chiptune loop using 8 to 28 notes across:
            channel 0 = Lead Pulse, channel 1 = Bass Triangle, channel 2 = Harmony / Arp.
            Ranges: bpm in [80, 200], step in [0, 15], midiNote in [48, 84], channel in [0, 2], durationBeats in [0.25, 1.0], velocity in [0.3, 1.0].
        """.trimIndent()

        val prompt = """
            Compose a brand-new 16-step chiptune loop for: "$cleanSubject"
            Track Title: "${assetName.trim().ifBlank { cleanSubject.take(32) }}"
            Return strict JSON matching:
            {
              "title": "Arcade Boss Loop",
              "bpm": 140,
              "notes": [
                {"step": 0, "midiNote": 64, "channel": 0, "durationBeats": 0.25, "velocity": 0.85},
                {"step": 0, "midiNote": 48, "channel": 1, "durationBeats": 0.50, "velocity": 0.90},
                {"step": 2, "midiNote": 67, "channel": 0, "durationBeats": 0.25, "velocity": 0.85},
                {"step": 4, "midiNote": 71, "channel": 2, "durationBeats": 0.50, "velocity": 0.75}
              ]
            }
        """.trimIndent()

        val response = invokeTextProvider(
            prompt = prompt,
            systemInstruction = systemInstruction,
            sharedContextBlock = sharedContextBlock,
            assetLabel = "16-step chiptune score"
        )
        val spec = parseScoreSequenceJson(
            title = assetName.ifBlank { cleanSubject.take(28) },
            rawJson = response.text
        )
        val pcm = AudioSynthAndDspEngine.synthesizeScoreSequencePcm(spec, sampleRateHz = DEFAULT_AUDIO_SAMPLE_RATE)
        val peaks = AudioSynthAndDspEngine.computeWaveformPeaks(pcm)

        return AiBuiltChiptuneResult(
            spec = spec,
            pcm = pcm,
            waveformPeaks = peaks,
            sampleRateHz = DEFAULT_AUDIO_SAMPLE_RATE,
            modelUsed = formatModelLabel(response),
            rawJson = extractJsonObjectString(response.text)
        )
    }

    // -------------------------------------------------------------------------
    // Shared OmniAiRouter Text Dispatch
    // -------------------------------------------------------------------------

    private suspend fun invokeTextProvider(
        prompt: String,
        systemInstruction: String,
        sharedContextBlock: String,
        assetLabel: String
    ): OmniTextResponse {
        return try {
            OmniAiRouter.generateTextOrJson(
                role = AiTaskRole.TEXT_AND_SPECS,
                prompt = prompt,
                systemInstruction = systemInstruction,
                imageBitmap = null,
                expectJson = true,
                sharedContextBlock = sharedContextBlock,
                firebaseDelegate = { combinedPrompt, _ ->
                    withTimeout(60_000L) {
                        val fullFirebasePrompt = "$systemInstruction\n\n$combinedPrompt"
                        val resp = jsonBlueprintModel.generateContent(fullFirebasePrompt)
                        resp.text ?: throw IllegalStateException("Empty text response from Firebase gemini-3.8-flash.")
                    }
                }
            )
        } catch (ce: CancellationException) {
            if (ce is TimeoutCancellationException) {
                Log.w(TAG, "AI $assetLabel generation timed out", ce)
                throw IllegalStateException("AI $assetLabel build timed out after 60s. Retry or switch provider in Omni-AI Hub.")
            }
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "AI $assetLabel generation failed", e)
            val detail = e.localizedMessage?.take(140) ?: "No text provider responded"
            throw IllegalStateException(
                "AI $assetLabel build failed ($detail). Check your connected provider in Omni-AI Hub.",
                e
            )
        }
    }

    private fun formatModelLabel(response: OmniTextResponse): String {
        return if (response.providerUsed == AiProviderType.FIREBASE_STUDIO) {
            response.modelIdUsed.ifBlank { "gemini-3.8-flash" }
        } else {
            "${response.providerUsed.name}:${response.modelIdUsed}"
        }
    }
}
