package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.local.AssetGalleryWithItems
import com.example.model.audio.AudioAssetEntry
import com.example.model.masterpack.FrameCadenceFps
import com.example.model.model3d.Scratch3DProject
import com.example.model.texture.ScratchTextureProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TRAINING_BASE_WIKI_TAG = "AiTrainingBaseWiki"

// =============================================================================
// 1. Canonical Adjustable Layer 1 Base & Modular Layer 2 Plug-In Architecture
// =============================================================================

/**
 * Canonical factory default Layer 1 game-art & 4-tier hardware architecture doctrine
 * plus modular Layer 2 plug-in identification helpers.
 */
object DefaultAiArtTrainingProfiles {

    const val LAYER_1_SOURCE_ID = "builtin_layer1_core_mediums_hardware"
    const val LAYER_1_TITLE = "Layer 1: Core Mediums & 4-Tier Hardware Architecture (Built-In Base)"
    const val LAYER_2_PLUGIN_ID_PREFIX = "layer2_plugin_"

    /**
     * Complete canonical Layer 1 doctrine across all 6 core sections.
     * Invariant: zero occurrences of "checkerboard" anywhere before "Strict Exclusions".
     */
    const val LAYER_1_CANONICAL_PROFILE: String =
        "(1) 4-MEDIUM BLEND: Fuse Tokusatsu tactile material contrast (glossy clearcoat armor & reflective visor white glints vs matte civilian fabric/skin, practical sparks, grounded martial weight), Graphic Novel clarity (variable-weight black ink outlines, hard 2-to-3-step cel shading, deep ink-pool shadows with angled halftones or ordered Bayer dithering strictly inside shadow regions), Classic Anime dynamism (razor silhouettes, crisp rim-light, layered hair/eye speculars, glowing emissive Mana Circuit waveguides, action smear frames & hitstop), and Western Cartoon readability (crisp shape hierarchy & flat color-blocking).\n" +
        "(2) DESIGN & RECOLOR INVARIANTS: Enforce Functional Maximalism ('No Feature Without a Schematic') plus the 'Paint Exemption' for pure decorative livery. Apply ~6.5-head proportions strictly for humanoid subjects only—omit humanoid proportions for monsters, Fakemon, mechs, vehicles, props, and icons. Distinguish Civilian matte anime/comic textures from Morphed lacquered Tokusatsu clearcoat + reflective visor + glowing circuit seams (Dual-Track Aesthetic). Use the full 20-Color Wheel (5 Anchors + 15 Bridges, Layer 0 Colorless, Layer 20 Omnichroma, 4 Value States: Pure/Tint/Shade/Muted) with zero soft airbrush gradients.\n" +
        "(3) 'SHOVEL KNIGHT' HARDWARE RULEBOOK: Honor an Authentic Silicon Core paired with Documented Modern Breaks (native 16:9 or 9:16 framing, zero sprite flicker or hardware slowdown, expanded simultaneous sub-palettes for readability).\n" +
        "(4) 4-TIER HARDWARE CONTINUUM (SINGLE ACTIVE TIER PER BATCH): " +
        "Tier 1 Native — 1-Bit Playdate (#000000/#F4F1EA screentones & 4x4 ordered Bayer), 8-Bit NES/DMG/C64/PICO-8 (CIELAB-snapped 8x8 tiles & 1px dark contours), 16-Bit Genesis (RGB333 + VDP 0.5x/1.5x Shadow/Highlight), 16-Bit SNES (BGR555 + Mode 7 + Additive/Subtractive Color Math), 32-Bit Arcade CPS-2/Neo Geo (bold ink + 2-step cel + ordered Bayer shadow dithering), 32-Bit PS1 (1,500-4,500 tris, nearest-neighbor textures, vertex-snap, Gouraud) & Saturn (VDP1 quads, VDP2 parallax, 50% stipple mesh transparency), and In-Lore Solid Vision/PET holographic projection; " +
        "Tier 2A Sleeper Cartridge (16-bit 426x240 + Mode 7 with modern ray-traced lighting quantized natively to BGR555 + SNES Color Math, zero blur) & Tier 2B Legacy-Silicon; " +
        "Tier 3 True Fusion Monolith (1080p/4K clean-line master core); " +
        "Tier 4 Omnichroma 3-Map Rig (Color-ID, Ink & Detail, Specular/Emissive).\n" +
        "(5) 7-STAGE RENDERING PIPELINE: Stage 1 Geometry/Projection -> Stage 2 4-Medium Lighting/Material -> Stage 3 Ink & Halftone/Ordered-Dither -> Stage 4 Hardware Color-Space & 20-Color Quantization -> Stage 5 Hardware Compositor Blending -> Stage 6 Internal Resolution & Frame-Clock Gate (nearest-neighbor scaling, 12-15 FPS on 2s/3s with hitstop or smooth 60 FPS) -> Stage 7 Optional Display Optics (clean broadcast default; optional CRT/LCD/DMG filter).\n" +
        "(6) SPRITE HYGIENE & EXCLUSIONS: Render sprites on a flat uniform solid chroma-key background auto-selected to contrast the subject (Magenta #FF00FF default, Lime Green #00FF00 whenever the subject features pink, magenta, or purple), with zero scenery, gradients, vignette, or floor shadow. Maintain consistent character height, grounded feet baseline, and uniform ink weight across sheets with zero grid lines or text. Strict Exclusions: No photorealism, soft airbrush, painterly blur, 3D plastic toy shading, visible frame borders, or gray-and-white checkerboard backgrounds."

    /**
     * Compact low-token (~75 tokens) micro-digest for llama.cpp / Ollama / fast mobile inference.
     * Invariant: zero occurrences of "checkerboard" before "No ".
     */
    const val LAYER_1_MICRO_DIGEST: String =
        "LAYER1_MICRO: 4-Medium Blend (Tokusatsu clearcoat glints vs matte fabric, bold black ink + hard 2-step cel + deep ink-pool shadows w/ ordered Bayer dithering or angled halftones strictly in shadows, anime rim-light & Mana Circuits, flat 20-Color Wheel blocking). Humanoids ~6.5 heads only. Single active hardware tier. Solid #FF00FF or #00FF00 bg. No photorealism, blur, 3D plastic, or checkerboard bg."

    fun isLayer2PluginSource(source: KnowledgeSourceEntry): Boolean =
        source.id.startsWith(LAYER_2_PLUGIN_ID_PREFIX)
}

// =============================================================================
// 2. Unified User Training & Explicit Chosen Model Persistence
// =============================================================================

/**
 * Unified user AI training profile + explicit model selection persisted via private SharedPreferences
 * and automatically synchronized across BOTH Studio Form and AI Chat Studio.
 */
data class UserAiTrainingProfile(
    val styleTriggerToken: String = "",
    val customLoraOrModelTag: String = "",
    val customTrainingNotes: String = "",
    val attachedTrainingFileCount: Int = 0,
    val applyToFormBuilder: Boolean = true,
    val applyToChatStudio: Boolean = true,
    val explicitChosenProviderName: String = "",
    val explicitChosenModelId: String = ""
) {
    /**
     * Formats non-blank trigger token, custom LoRA / fine-tuned model tag, and custom training notes
     * into a compact structured context block shared across AI requests. Returns `""` if all blank.
     */
    fun buildUnifiedTrainingContextBlock(): String {
        val trigger = styleTriggerToken.trim()
        val loraTag = customLoraOrModelTag.trim()
        val notes = customTrainingNotes.trim()
        if (trigger.isEmpty() && loraTag.isEmpty() && notes.isEmpty()) return ""

        return buildString {
            appendLine("### [USER UNIFIED AI TRAINING PROFILE]")
            if (trigger.isNotEmpty()) {
                appendLine("STYLE_TRIGGER_TOKEN: $trigger")
            }
            if (loraTag.isNotEmpty()) {
                appendLine("CUSTOM_LORA_OR_MODEL_TAG: $loraTag")
            }
            if (notes.isNotEmpty()) {
                appendLine("TRAINING_DIRECTIVES: ${notes.take(2400)}")
            }
            if (attachedTrainingFileCount > 0) {
                appendLine("ATTACHED_TRAINING_FILES: $attachedTrainingFileCount")
            }
        }.trim()
    }

    /**
     * Prepends [styleTriggerToken] (and [customLoraOrModelTag] if non-blank) to [basePrompt]
     * without duplicating tokens that already appear in the prompt.
     */
    fun applyTriggerToPrompt(basePrompt: String): String {
        val cleanBase = basePrompt.trim()
        val tokensToPrepend = buildList {
            val trigger = styleTriggerToken.trim()
            if (trigger.isNotEmpty() && !containsTokenCaseInsensitive(cleanBase, trigger)) {
                add(trigger)
            }
            val loraTag = customLoraOrModelTag.trim()
            if (loraTag.isNotEmpty() &&
                !loraTag.equals(trigger, ignoreCase = true) &&
                !containsTokenCaseInsensitive(cleanBase, loraTag)
            ) {
                add(loraTag)
            }
        }
        if (tokensToPrepend.isEmpty()) return cleanBase
        if (cleanBase.isEmpty()) return tokensToPrepend.joinToString(", ")
        return "${tokensToPrepend.joinToString(", ")}, $cleanBase"
    }

    fun toJsonString(): String {
        return JSONObject().apply {
            put("schema", "pixelforge.user_ai_training_profile.v2")
            put("styleTriggerToken", styleTriggerToken)
            put("customLoraOrModelTag", customLoraOrModelTag)
            put("customTrainingNotes", customTrainingNotes)
            put("attachedTrainingFileCount", attachedTrainingFileCount.coerceAtLeast(0))
            put("applyToFormBuilder", applyToFormBuilder)
            put("applyToChatStudio", applyToChatStudio)
            put("explicitChosenProviderName", explicitChosenProviderName)
            put("explicitChosenModelId", explicitChosenModelId)
        }.toString(2)
    }

    private fun containsTokenCaseInsensitive(text: String, token: String): Boolean {
        if (text.isBlank() || token.isBlank()) return false
        return text.contains(token, ignoreCase = true)
    }

    companion object {
        fun fromJsonString(json: String): UserAiTrainingProfile {
            return try {
                val root = JSONObject(json)
                UserAiTrainingProfile(
                    styleTriggerToken = root.optString("styleTriggerToken", "").trim(),
                    customLoraOrModelTag = root.optString("customLoraOrModelTag", "").trim(),
                    customTrainingNotes = root.optString("customTrainingNotes", "").trim(),
                    attachedTrainingFileCount = root.optInt("attachedTrainingFileCount", 0).coerceAtLeast(0),
                    applyToFormBuilder = root.optBoolean("applyToFormBuilder", true),
                    applyToChatStudio = root.optBoolean("applyToChatStudio", true),
                    explicitChosenProviderName = root.optString("explicitChosenProviderName", "").trim(),
                    explicitChosenModelId = root.optString("explicitChosenModelId", "").trim()
                )
            } catch (e: Exception) {
                Log.w(TRAINING_BASE_WIKI_TAG, "Failed to parse UserAiTrainingProfile JSON: ${e.message}")
                UserAiTrainingProfile()
            }
        }
    }
}

/**
 * Persists [UserAiTrainingProfile] (including explicit user-selected AI provider/model)
 * via private [android.content.SharedPreferences].
 */
object UserAiTrainingProfileStore {
    private const val PREFS_NAME = "pixelforge_user_ai_training_profile"
    private const val KEY_PROFILE_JSON = "profile_json"

    fun save(context: Context, profile: UserAiTrainingProfile) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PROFILE_JSON, profile.toJsonString()).apply()
        } catch (e: Exception) {
            Log.w(TRAINING_BASE_WIKI_TAG, "Could not persist UserAiTrainingProfile: ${e.message}")
        }
    }

    fun load(context: Context): UserAiTrainingProfile {
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PROFILE_JSON, null)
            if (raw.isNullOrBlank()) UserAiTrainingProfile() else UserAiTrainingProfile.fromJsonString(raw)
        } catch (e: Exception) {
            Log.w(TRAINING_BASE_WIKI_TAG, "Could not load UserAiTrainingProfile: ${e.message}")
            UserAiTrainingProfile()
        }
    }
}

// =============================================================================
// 3. Dataset Export, Few-Shot & Multi-Turn Forge Chat Models
// =============================================================================

enum class ForgeChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ForgeChatMessage(
    val id: String,
    val role: ForgeChatRole,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val providerUsed: String = "",
    val extractedBuildPrompt: String? = null,
    val suggestedBuildTarget: AiBuildTargetKind? = null
)

/**
 * Pure training dataset exporters (LoRA caption manifest `.json`, OpenAI/Gemini fine-tune `.jsonl`),
 * few-shot example formatters, FPS-to-rig cadence mapper, and user training file parser.
 */
object AiTrainingDatasetExporter {

    private const val MAX_JSONL_IMPORT_PAIRS = 8
    private const val MAX_IMPORTED_TRAINING_CHARS = 8_000

    /**
     * Maps any integer target FPS (`1..120`) to the nearest [FrameCadenceFps] rig cadence tier.
     */
    fun nearestRigCadence(fps: Int): FrameCadenceFps = when {
        fps <= 13 -> FrameCadenceFps.FPS_12
        fps <= 22 -> FrameCadenceFps.FPS_15
        fps <= 45 -> FrameCadenceFps.FPS_30
        else -> FrameCadenceFps.FPS_60
    }

    /**
     * Builds a concise few-shot training example block ready to pin into [MultiSourceContextRegistry].
     */
    fun buildFewShotTrainingExampleText(
        assetTitle: String,
        targetKind: AiBuildTargetKind,
        promptUsed: String,
        targetFps: Int,
        structuredRecipeOrMetadata: String
    ): String {
        val safeFps = targetFps.coerceIn(1, 120)
        return buildString {
            appendLine("[FEW-SHOT TRAINING EXAMPLE]")
            appendLine("Asset: ${assetTitle.trim().ifBlank { "Studio Asset" }}")
            appendLine("Modality: ${targetKind.name} (${targetKind.label})")
            appendLine("Target FPS: $safeFps FPS")
            appendLine("Prompt: ${promptUsed.trim().take(360)}")
            if (structuredRecipeOrMetadata.isNotBlank()) {
                appendLine("Reference Spec:")
                append(structuredRecipeOrMetadata.trim().take(1600))
            }
        }.trim()
    }

    /**
     * Emits a valid LoRA caption training manifest JSON (`schema = "pixelforge.lora_dataset.v1"`)
     * from an [AssetGalleryWithItems] collection.
     */
    fun buildLoraCaptionManifestJson(
        gallery: AssetGalleryWithItems,
        triggerToken: String = "mpv_truefusion",
        targetFps: Int = 15,
        tier: PromptHardwareTier = PromptHardwareTier.ARCADE_32BIT_60FPS
    ): String {
        val safeTrigger = triggerToken.trim().ifBlank { "mpv_truefusion" }
        val safeFps = targetFps.coerceIn(1, 120)
        val entriesArray = JSONArray()

        gallery.items.forEachIndexed { idx, item ->
            val slug = sanitizeSlug(item.title, "asset_${idx + 1}")
            val imageFile = "images/${String.format("%03d", idx + 1)}_$slug.png"
            val captionFile = "images/${String.format("%03d", idx + 1)}_$slug.txt"
            val cadenceNote = if (item.spriteColumns * item.spriteRows > 1) {
                ", ${item.spriteColumns}x${item.spriteRows} sprite sheet at $safeFps FPS animation cadence"
            } else {
                ", $safeFps FPS target timing"
            }
            val captionText = buildString {
                append("$safeTrigger, ${item.title}, character ${item.characterTag}, ")
                append("game ${item.gameTag}, group ${item.groupTag}, ")
                append("${tier.compactDirective}$cadenceNote")
                if (item.curatorNote.isNotBlank()) {
                    append(", ${item.curatorNote.trim()}")
                }
            }

            entriesArray.put(
                JSONObject().apply {
                    put("imageFile", imageFile)
                    put("captionFile", captionFile)
                    put("captionText", captionText)
                    put("gameTag", item.gameTag)
                    put("groupTag", item.groupTag)
                    put("characterTag", item.characterTag)
                    put("assetType", item.assetType)
                    put("hardwareTarget", item.hardwareTarget)
                }
            )
        }

        return JSONObject().apply {
            put("schema", "pixelforge.lora_dataset.v1")
            put("galleryName", gallery.gallery.name)
            put("triggerToken", safeTrigger)
            put("targetFps", safeFps)
            put("hardwareTier", tier.name)
            put("entryCount", entriesArray.length())
            put("entries", entriesArray)
        }.toString(2)
    }

    /**
     * Emits valid line-delimited JSONL (`{"messages": [{"role":"system",...},{"role":"user",...},{"role":"assistant",...}]}`)
     * for LLM fine-tuning from the user's active 3D kitbash blueprint, Texture recipe, and Audio Vault entries.
     */
    fun buildJsonlFineTuneDataset(
        project3D: Scratch3DProject?,
        textureProject: ScratchTextureProject?,
        audioEntries: List<AudioAssetEntry>,
        targetFps: Int = 15
    ): String {
        val safeFps = targetFps.coerceIn(1, 120)
        val lines = mutableListOf<String>()

        if (project3D != null && project3D.parts.isNotEmpty()) {
            val partsJson = JSONArray()
            project3D.parts.forEach { p ->
                partsJson.put(
                    JSONObject().apply {
                        put("name", p.name)
                        put("shape", p.shape.name)
                        put("posX", p.posX.toDouble())
                        put("posY", p.posY.toDouble())
                        put("posZ", p.posZ.toDouble())
                        put("scaleX", p.scaleX.toDouble())
                        put("scaleY", p.scaleY.toDouble())
                        put("scaleZ", p.scaleZ.toDouble())
                        put("rotYDeg", p.rotYDeg.toDouble())
                        put("colorHex", String.format("#%06X", (p.colorLong and 0xFFFFFFL).toInt()))
                        put("mirrorX", p.mirrorX)
                    }
                )
            }
            val assistantPayload = JSONObject().apply {
                put("title", project3D.name)
                put("targetFps", safeFps)
                put("parts", partsJson)
            }.toString()

            lines.add(
                makeFineTuneLine(
                    systemPrompt = "You are PixelForge 3D Kitbash Architect. Return strict JSON blueprints with parts array.",
                    userPrompt = "Build a 3D kitbash blueprint named '${project3D.name}' with ${project3D.parts.size} parts for $safeFps FPS animation.",
                    assistantContent = assistantPayload
                )
            )
        }

        if (textureProject != null) {
            val nonTransparent = textureProject.pixels.filter { (it ushr 24) > 0 }
            val topColors = nonTransparent
                .groupingBy { it and 0xFFFFFF }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { String.format("#%06X", it.key) }

            val primaryHex = topColors.getOrElse(0) { "#0F172A" }
            val secondaryHex = topColors.getOrElse(1) { "#1E293B" }
            val accentHex = topColors.getOrElse(2) { "#00E5FF" }

            val assistantPayload = JSONObject().apply {
                put("title", textureProject.name)
                put("sizePx", textureProject.sizePx)
                put("pattern", "ENERGY_CIRCUIT")
                put("primaryHex", primaryHex)
                put("secondaryHex", secondaryHex)
                put("accentHex", accentHex)
                put("mirrorX", textureProject.mirrorX)
                put("mirrorY", textureProject.mirrorY)
            }.toString()

            lines.add(
                makeFineTuneLine(
                    systemPrompt = "You are PixelForge Texture Architect. Return strict JSON procedural material recipes.",
                    userPrompt = "Generate a ${textureProject.sizePx}x${textureProject.sizePx} seamless game material texture named '${textureProject.name}'.",
                    assistantContent = assistantPayload
                )
            )
        }

        audioEntries.take(12).forEach { entry ->
            when {
                entry.crySpec != null -> {
                    lines.add(
                        makeFineTuneLine(
                            systemPrompt = "You are PixelForge Soundchip SFX Designer. Return strict JSON procedural cry specifications.",
                            userPrompt = "Synthesize a procedural creature cry SFX for '${entry.title}' (${entry.characterTag}, slot ${entry.slotKey}).",
                            assistantContent = entry.crySpec.toJsonObject().toString()
                        )
                    )
                }
                entry.scoreSpec != null -> {
                    val notesArr = JSONArray()
                    entry.scoreSpec.notes.forEach { n ->
                        notesArr.put(
                            JSONObject().apply {
                                put("step", (n.startBeat * 4f).roundToInt().coerceIn(0, 15))
                                put("midiNote", n.midiNote)
                                put("channel", n.channel)
                                put("durationBeats", n.durationBeats.toDouble())
                                put("velocity", n.velocity.toDouble())
                            }
                        )
                    }
                    val scorePayload = JSONObject().apply {
                        put("title", entry.scoreSpec.title)
                        put("bpm", entry.scoreSpec.bpm)
                        put("notes", notesArr)
                    }.toString()
                    lines.add(
                        makeFineTuneLine(
                            systemPrompt = "You are PixelForge 16-Step Chiptune Composer. Return strict JSON score sequences.",
                            userPrompt = "Compose a 16-step chiptune loop titled '${entry.title}' at ${entry.scoreSpec.bpm} BPM for ${entry.gameTag}.",
                            assistantContent = scorePayload
                        )
                    )
                }
            }
        }

        if (lines.isEmpty()) {
            lines.add(
                makeFineTuneLine(
                    systemPrompt = "You are PixelForge Game Asset Director. Target animation cadence is $safeFps FPS.",
                    userPrompt = "Create a starter 2D arcade hero sprite specification at $safeFps FPS.",
                    assistantContent = JSONObject().apply {
                        put("title", "Starter Arcade Hero")
                        put("targetFps", safeFps)
                        put("tier", PromptHardwareTier.ARCADE_32BIT_60FPS.name)
                    }.toString()
                )
            )
        }

        return lines.joinToString("\n")
    }

    /**
     * Safely parses user-uploaded `.jsonl`, `.json`, `.md`, or `.txt` training files into a compact
     * knowledge block ready for [MultiSourceContextRegistry].
     */
    fun parseImportedUserTrainingFile(fileName: String, rawText: String): String {
        val cleanName = fileName.trim().ifBlank { "user_training_file.txt" }
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Selected training file '$cleanName' is empty.")
        }

        val lower = cleanName.lowercase()
        val isJsonl = lower.endsWith(".jsonl") ||
            (trimmed.contains("\n") && trimmed.lines().firstOrNull { it.isNotBlank() }?.trim()?.startsWith("{") == true &&
                trimmed.lines().count { it.trim().startsWith("{") } >= 2)

        if (isJsonl) {
            val extractedPairs = mutableListOf<String>()
            var skippedBadLines = 0
            trimmed.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { line ->
                    if (extractedPairs.size >= MAX_JSONL_IMPORT_PAIRS) return@forEach
                    try {
                        val obj = JSONObject(line)
                        val msgs = obj.optJSONArray("messages")
                        if (msgs != null && msgs.length() > 0) {
                            val parts = mutableListOf<String>()
                            for (i in 0 until msgs.length()) {
                                val m = msgs.optJSONObject(i) ?: continue
                                val role = m.optString("role", "").trim().uppercase()
                                val content = m.optString("content", "").trim()
                                if (role.isNotEmpty() && content.isNotEmpty()) {
                                    parts.add("$role: ${content.take(360)}")
                                }
                            }
                            if (parts.isNotEmpty()) {
                                extractedPairs.add("Example #${extractedPairs.size + 1}:\n" + parts.joinToString("\n"))
                            }
                        } else {
                            val prompt = obj.optString("prompt", obj.optString("input", "")).trim()
                            val completion = obj.optString("completion", obj.optString("output", obj.optString("captionText", ""))).trim()
                            if (prompt.isNotEmpty() || completion.isNotEmpty()) {
                                extractedPairs.add(
                                    "Example #${extractedPairs.size + 1}:\nPROMPT: ${prompt.take(280)}\nTARGET: ${completion.take(400)}"
                                )
                            } else {
                                skippedBadLines++
                            }
                        }
                    } catch (e: Exception) {
                        skippedBadLines++
                        Log.w(TRAINING_BASE_WIKI_TAG, "Skipping malformed JSONL line in $cleanName: ${e.message}")
                    }
                }

            if (extractedPairs.isEmpty()) {
                throw IllegalArgumentException("No valid JSONL training examples were found in '$cleanName'.")
            }

            return buildString {
                appendLine("USER FINE-TUNE DATASET ($cleanName — ${extractedPairs.size} examples parsed${if (skippedBadLines > 0) ", $skippedBadLines invalid lines skipped" else ""}):")
                append(extractedPairs.joinToString("\n\n"))
            }.take(MAX_IMPORTED_TRAINING_CHARS)
        }

        if (lower.endsWith(".json")) {
            return try {
                val root = JSONObject(trimmed)
                val schema = root.optString("schema", "")
                val trigger = root.optString("triggerToken", "")
                val entries = root.optJSONArray("entries")
                if (entries != null && entries.length() > 0) {
                    buildString {
                        appendLine("USER LORA/JSON TRAINING MANIFEST ($cleanName${if (schema.isNotBlank()) " • $schema" else ""}):")
                        if (trigger.isNotBlank()) appendLine("Trigger Token: $trigger")
                        val count = entries.length().coerceAtMost(12)
                        for (i in 0 until count) {
                            val e = entries.optJSONObject(i) ?: continue
                            val caption = e.optString("captionText", e.optString("caption", "")).trim()
                            val file = e.optString("imageFile", "item_${i + 1}")
                            if (caption.isNotEmpty()) {
                                appendLine("- $file => ${caption.take(260)}")
                            }
                        }
                    }.trim().take(MAX_IMPORTED_TRAINING_CHARS)
                } else {
                    "USER JSON TRAINING SPEC ($cleanName):\n${root.toString(2)}".take(MAX_IMPORTED_TRAINING_CHARS)
                }
            } catch (e: Exception) {
                Log.w(TRAINING_BASE_WIKI_TAG, "JSON training file '$cleanName' fell back to raw text: ${e.message}")
                "USER TRAINING REFERENCE ($cleanName):\n$trimmed".take(MAX_IMPORTED_TRAINING_CHARS)
            }
        }

        return "USER TRAINING NOTES ($cleanName):\n$trimmed".take(MAX_IMPORTED_TRAINING_CHARS)
    }

    private fun makeFineTuneLine(
        systemPrompt: String,
        userPrompt: String,
        assistantContent: String
    ): String {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
            put(JSONObject().apply {
                put("role", "assistant")
                put("content", assistantContent)
            })
        }
        return JSONObject().apply {
            put("messages", messages)
        }.toString()
    }

    private fun sanitizeSlug(raw: String, fallback: String): String {
        val slug = raw.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return slug.ifBlank { fallback }
    }
}

/**
 * Multi-turn AI Asset Director chat prompt builder and reply build-intent extractor.
 */
object ForgeChatPromptBuilder {

    fun buildConversationPrompt(
        history: List<ForgeChatMessage>,
        userMessage: String,
        targetFps: Int,
        trainingProfile: UserAiTrainingProfile,
        sharedContextBlock: String
    ): String {
        val safeFps = targetFps.coerceIn(1, 120)
        val recentTurns = history.takeLast(10)
        val trainingBlock = if (trainingProfile.applyToChatStudio) {
            trainingProfile.buildUnifiedTrainingContextBlock()
        } else {
            ""
        }
        val cleanShared = sharedContextBlock.trim()

        return buildString {
            appendLine("ROLE: PixelForge Game Asset Director (2D Sprites/Sheets, 3D Kitbash Models, Seamless Textures, Procedural Creature Cries & 16-Step Chiptunes).")
            appendLine("TARGET_FPS=$safeFps")
            appendLine("INSTRUCTIONS: Collaborate concisely with the game creator. Give concrete art-direction, palette, silhouette, rig/FPS, or sound-design advice.")
            appendLine("Whenever you propose or refine a buildable asset, end your reply with a single clear summary line starting with `BUILD_PROMPT:` followed by the ready-to-generate asset description.")
            if (cleanShared.isNotBlank()) {
                appendLine()
                appendLine("=== SHARED KNOWLEDGE & AI BASE ===")
                appendLine(cleanShared.take(4500))
                appendLine("=== END SHARED KNOWLEDGE ===")
            }
            if (trainingBlock.isNotBlank()) {
                appendLine()
                appendLine(trainingBlock)
            }
            if (recentTurns.isNotEmpty()) {
                appendLine()
                appendLine("=== CONVERSATION HISTORY (LAST ${recentTurns.size} TURNS) ===")
                recentTurns.forEach { msg ->
                    val speaker = when (msg.role) {
                        ForgeChatRole.USER -> "USER"
                        ForgeChatRole.ASSISTANT -> "DIRECTOR"
                        ForgeChatRole.SYSTEM -> "SYSTEM"
                    }
                    appendLine("$speaker: ${msg.text.trim()}")
                }
            }
            appendLine()
            appendLine("USER: ${userMessage.trim()}")
            append("DIRECTOR:")
        }
    }

    fun extractSuggestedTargetAndPrompt(
        replyText: String,
        fallbackUserPrompt: String
    ): Pair<AiBuildTargetKind, String> {
        val buildLineRegex = Regex("""(?im)^\s*(?:\*\*)?BUILD_PROMPT(?:\*\*)?\s*:\s*(.+)$""")
        val matchedPrompt = buildLineRegex.find(replyText)?.groupValues?.getOrNull(1)
            ?.trim()
            ?.trim('"', '`', '*')
            ?.take(360)

        val extractedPrompt = when {
            !matchedPrompt.isNullOrBlank() -> matchedPrompt
            replyText.isNotBlank() -> replyText
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString(" ")
                .take(280)
            else -> fallbackUserPrompt.trim().take(280)
        }.ifBlank { fallbackUserPrompt.trim().ifBlank { "original arcade game asset" } }

        val combinedSignal = "$extractedPrompt ${replyText.take(220)} ${fallbackUserPrompt.take(120)}".lowercase()

        val inferredTarget = when {
            Regex("""\b(chiptune|16-step|sequencer|bgm|soundtrack|melody|arpeggio|bpm|boss theme|music loop)\b""")
                .containsMatchIn(combinedSignal) -> AiBuildTargetKind.AUDIO_CHIPTUNE

            Regex("""\b(creature cry|roar|chirp|sfx|sound effect|siren|growl|voice cry|audio cry|warble)\b""")
                .containsMatchIn(combinedSignal) -> AiBuildTargetKind.AUDIO_CRY

            Regex("""\b(texture|material|seamless tile|armor plate|bricks|scales texture|uv map|surface pattern|tileable)\b""")
                .containsMatchIn(combinedSignal) -> AiBuildTargetKind.TEXTURE_MATERIAL

            Regex("""\b(3d model|3d blueprint|kitbash|low-poly|low poly|\.obj|mesh|turret|starfighter|hover tank|obelisk|mech chassis)\b""")
                .containsMatchIn(combinedSignal) -> AiBuildTargetKind.MODEL_3D

            else -> AiBuildTargetKind.SPRITE_2D
        }

        return inferredTarget to extractedPrompt
    }
}

// =============================================================================
// 4. Master Hub / LoreWeaver Wiki Canon Reader & Offline Cache
// =============================================================================

data class WikiCanonEntry(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val visualTraits: String = "",
    val tags: List<String> = emptyList()
)

data class MasterHubWikiCacheState(
    val sourceLabel: String = "Not Synced",
    val wikiUrl: String = "",
    val entries: List<WikiCanonEntry> = emptyList(),
    val lastSyncedMillis: Long = 0L,
    val enabledForAiBase: Boolean = true
)

object MasterHubWikiSyncEngine {

    const val CACHE_FILE_NAME = "master_hub_wiki_cache.json"
    private const val PREFS_NAME = "pixelforge_master_hub_wiki_prefs"
    private const val KEY_SOURCE_LABEL = "wiki_source_label"
    private const val KEY_WIKI_URL = "wiki_url"
    private const val KEY_LAST_SYNCED = "wiki_last_synced"
    private const val KEY_ENABLED_FOR_AI = "wiki_enabled_for_ai"
    private const val MAX_REMOTE_BYTES = 1_500_000

    private val KNOWN_ARRAY_KEYS = listOf(
        "entries" to "Entry",
        "characters" to "Character",
        "factions" to "Faction",
        "locations" to "Location",
        "species" to "Species",
        "lore" to "Lore",
        "items" to "Item",
        "articles" to "Article",
        "nodes" to "Node",
        "wikiEntries" to "Wiki Entry"
    )

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "from", "that", "this", "into", "your",
        "make", "build", "create", "generate", "draw", "sprite", "model",
        "asset", "image", "sheet", "about", "have", "will", "would", "could",
        "should", "what", "when", "where", "which", "their", "there", "these",
        "those", "using", "style", "like", "just", "some", "more", "than",
        "very", "also", "then", "them", "they", "been", "being", "over", "under"
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    fun parseWorldBibleJson(rawJson: String): List<WikiCanonEntry> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()

        val collected = mutableListOf<WikiCanonEntry>()
        val seenIds = HashSet<String>()

        fun addUnique(entry: WikiCanonEntry) {
            val cleanTitle = entry.title.trim()
            if (cleanTitle.isEmpty()) return
            val key = "${entry.id.lowercase()}|${cleanTitle.lowercase()}"
            if (seenIds.add(key)) {
                collected.add(
                    entry.copy(
                        title = cleanTitle,
                        category = entry.category.trim().ifBlank { "Lore" },
                        summary = entry.summary.trim().take(1200),
                        visualTraits = entry.visualTraits.trim().take(600),
                        tags = entry.tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                    )
                )
            }
        }

        try {
            if (trimmed.startsWith("[")) {
                parseArrayEntries(JSONArray(trimmed), "Entry", ::addUnique)
                return collected
            }

            val root = JSONObject(trimmed)

            val worldName = firstNonBlankString(root, "worldName", "universeName", "projectName", "title", "name")
            val worldSummary = firstNonBlankString(root, "summary", "description", "overview", "premise", "artDirection")
            val worldVisuals = extractVisualTraits(root)
            val worldTags = extractTags(root)
            if (worldName.isNotEmpty() && (worldSummary.isNotEmpty() || worldVisuals.isNotEmpty())) {
                addUnique(
                    WikiCanonEntry(
                        id = "world_primer_${slugify(worldName)}",
                        title = worldName,
                        category = "World Primer",
                        summary = worldSummary.ifBlank { "Canonical Master Hub universe primer." },
                        visualTraits = worldVisuals,
                        tags = worldTags
                    )
                )
            }

            val containers = buildList {
                add(root)
                listOf("worldBible", "wiki", "data", "codex").forEach { nestedKey ->
                    root.optJSONObject(nestedKey)?.let { add(it) }
                }
            }

            containers.forEach { container ->
                KNOWN_ARRAY_KEYS.forEach { (arrayKey, defaultCategory) ->
                    val arr = container.optJSONArray(arrayKey)
                    if (arr != null && arr.length() > 0) {
                        parseArrayEntries(arr, defaultCategory, ::addUnique)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TRAINING_BASE_WIKI_TAG, "Failed to parse World Bible JSON: ${e.message}")
            return emptyList()
        }

        return collected
    }

    fun parseWorldBibleMarkdown(rawMarkdown: String): List<WikiCanonEntry> {
        val normalized = rawMarkdown.replace("\r\n", "\n").trim()
        if (normalized.isEmpty()) return emptyList()

        val entries = mutableListOf<WikiCanonEntry>()
        var currentCategory = "World Lore"
        var activeTitle = ""
        var activeCategory = "World Lore"
        val bodyLines = mutableListOf<String>()
        var indexCounter = 0

        fun flushSection() {
            val cleanTitle = activeTitle.trim()
            val rawBody = bodyLines.joinToString("\n").trim()
            if (cleanTitle.isNotEmpty() && rawBody.isNotEmpty()) {
                indexCounter++
                val visualPieces = mutableListOf<String>()
                val tagPieces = mutableListOf<String>()
                val summaryLines = mutableListOf<String>()

                rawBody.lineSequence().forEach { line ->
                    val stripped = line.trim().removePrefix("- ").removePrefix("* ").trim()
                    val lower = stripped.lowercase()
                    when {
                        lower.startsWith("visual traits:") ||
                            lower.startsWith("**visual traits**:") ||
                            lower.startsWith("appearance:") ||
                            lower.startsWith("**appearance**:") ||
                            lower.startsWith("design notes:") ||
                            lower.startsWith("**design notes**:") ||
                            lower.startsWith("palette:") ||
                            lower.startsWith("**palette**:") -> {
                            val value = stripped.substringAfter(':', "").trim().trim('*').trim()
                            if (value.isNotEmpty()) visualPieces.add(value)
                        }
                        lower.startsWith("tags:") || lower.startsWith("**tags**:") -> {
                            val value = stripped.substringAfter(':', "").trim().trim('*').trim()
                            value.split(',', ';', '|').map { it.trim().removePrefix("#") }
                                .filter { it.isNotEmpty() }
                                .forEach { tagPieces.add(it) }
                        }
                        else -> {
                            if (stripped.isNotEmpty()) summaryLines.add(stripped)
                        }
                    }
                }

                Regex("""#([A-Za-z0-9_-]{2,28})""").findAll(rawBody).forEach { match ->
                    tagPieces.add(match.groupValues[1])
                }

                val summaryText = summaryLines.joinToString(" ").trim().take(1200)
                val visualText = visualPieces.joinToString("; ").trim().take(600)
                if (summaryText.isNotEmpty() || visualText.isNotEmpty()) {
                    entries.add(
                        WikiCanonEntry(
                            id = "md_${indexCounter}_${slugify(cleanTitle)}",
                            title = cleanTitle,
                            category = activeCategory.ifBlank { "World Lore" },
                            summary = summaryText.ifBlank { visualText },
                            visualTraits = visualText,
                            tags = tagPieces.distinct()
                        )
                    )
                }
            }
            bodyLines.clear()
        }

        normalized.lineSequence().forEach { rawLine ->
            val trimmedLine = rawLine.trim()
            when {
                trimmedLine.startsWith("### ") -> {
                    flushSection()
                    activeTitle = trimmedLine.removePrefix("### ").trim().trim('#').trim()
                    activeCategory = currentCategory
                }
                trimmedLine.startsWith("## ") -> {
                    flushSection()
                    val heading = trimmedLine.removePrefix("## ").trim().trim('#').trim()
                    currentCategory = heading.ifBlank { "World Lore" }
                    activeTitle = heading
                    activeCategory = heading.ifBlank { "World Lore" }
                }
                trimmedLine.startsWith("# ") -> {
                    if (activeTitle.isEmpty()) {
                        activeTitle = trimmedLine.removePrefix("# ").trim().trim('#').trim()
                        activeCategory = "World Primer"
                    } else {
                        bodyLines.add(trimmedLine)
                    }
                }
                else -> {
                    if (activeTitle.isNotEmpty()) {
                        bodyLines.add(rawLine)
                    }
                }
            }
        }
        flushSection()

        return entries
    }

    fun parseAuto(fileNameOrUrl: String, rawText: String): List<WikiCanonEntry> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lowerName = fileNameOrUrl.lowercase()
        val looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[") || lowerName.endsWith(".json")
        if (looksLikeJson) {
            val jsonEntries = parseWorldBibleJson(trimmed)
            if (jsonEntries.isNotEmpty()) return jsonEntries
        }
        return parseWorldBibleMarkdown(trimmed)
    }

    fun buildRelevantWikiContextBlock(
        entries: List<WikiCanonEntry>,
        activePromptOrChat: String,
        maxEntries: Int = 5,
        maxChars: Int = 2_400
    ): String {
        if (entries.isEmpty()) return ""
        val safeMaxEntries = maxEntries.coerceAtLeast(1)
        val safeMaxChars = maxChars.coerceAtLeast(200)

        val queryLower = activePromptOrChat.trim().lowercase()
        val queryTokens = tokenizeMeaningful(queryLower)

        val selectedEntries: List<WikiCanonEntry> = if (queryTokens.isEmpty()) {
            entries.take(3.coerceAtMost(safeMaxEntries))
        } else {
            val scored = entries.mapIndexed { idx, entry ->
                val titleLower = entry.title.lowercase()
                val categoryLower = entry.category.lowercase()
                val tagsLower = entry.tags.joinToString(" ").lowercase()
                val traitsLower = entry.visualTraits.lowercase()
                val summaryLower = entry.summary.lowercase()

                var score = 0
                if (titleLower.length >= 3 && queryLower.contains(titleLower)) {
                    score += 12
                }
                for (token in queryTokens) {
                    if (titleLower.contains(token)) score += 10
                    if (categoryLower.contains(token) || tagsLower.contains(token)) score += 5
                    if (traitsLower.contains(token) || summaryLower.contains(token)) score += 2
                }
                Triple(entry, score, idx)
            }

            val matched = scored
                .filter { it.second > 0 }
                .sortedWith(compareByDescending<Triple<WikiCanonEntry, Int, Int>> { it.second }.thenBy { it.third })
                .take(safeMaxEntries)
                .map { it.first }

            if (matched.isNotEmpty()) matched else entries.take(3.coerceAtMost(safeMaxEntries))
        }

        if (selectedEntries.isEmpty()) return ""

        val header = "[MASTER HUB WIKI CANON DETAILS]\n"
        val sb = StringBuilder(safeMaxChars.coerceAtMost(4096))
        sb.append(header)

        for (entry in selectedEntries) {
            val block = buildString {
                append("- ${entry.title} (${entry.category})")
                if (entry.visualTraits.isNotBlank()) {
                    append("\n  Visual Traits: ${entry.visualTraits}")
                }
                if (entry.summary.isNotBlank()) {
                    append("\n  Canon Summary: ${entry.summary}")
                }
                if (entry.tags.isNotEmpty()) {
                    append("\n  Tags: ${entry.tags.joinToString(", ")}")
                }
                append("\n")
            }

            val remaining = safeMaxChars - sb.length
            if (remaining <= 28) break
            if (block.length <= remaining) {
                sb.append(block)
            } else {
                val suffix = "..."
                val takeLen = (remaining - suffix.length).coerceAtLeast(0)
                if (takeLen > 0) {
                    sb.append(block.substring(0, takeLen).trimEnd())
                    sb.append(suffix)
                }
                break
            }
        }

        val result = sb.toString().trim()
        return if (result.length <= safeMaxChars) result else result.substring(0, safeMaxChars).trimEnd()
    }

    suspend fun fetchWikiFromHttpsUrl(
        url: String,
        maxRedirectsRemaining: Int = 3
    ): Pair<String, List<WikiCanonEntry>> = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            throw IllegalArgumentException("Master Hub Wiki URL must start with https://")
        }
        val validated = MultiSourceContextRegistry.validatePublicHttpsUrl(trimmed, resolveDns = true)
        if (validated.isFailure) {
            throw IllegalArgumentException(
                validated.exceptionOrNull()?.message ?: "Blocked unsafe Master Hub Wiki URL."
            )
        }
        val uri = validated.getOrThrow()

        val request = Request.Builder()
            .url(uri.toString())
            .get()
            .header("Accept", "application/json, text/markdown, text/plain;q=0.9, */*;q=0.5")
            .header("User-Agent", "PixelForge-MasterHubWiki/1.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isRedirect) {
                if (maxRedirectsRemaining <= 0) {
                    throw IllegalStateException("Too many redirects while syncing Master Hub Wiki.")
                }
                val location = response.header("Location")
                    ?: throw IllegalStateException("Redirect response missing Location header.")
                val nextUrl = uri.resolve(location).toString()
                return@withContext fetchWikiFromHttpsUrl(nextUrl, maxRedirectsRemaining - 1)
            }

            if (!response.isSuccessful) {
                throw IllegalStateException("Master Hub Wiki sync failed (HTTP ${response.code}). Verify the URL is public.")
            }

            val rawText = response.body?.byteStream()?.use { input ->
                val buffer = ByteArray(8192)
                val out = ByteArrayOutputStream()
                var total = 0
                while (total < MAX_REMOTE_BYTES) {
                    val toRead = (MAX_REMOTE_BYTES - total).coerceAtMost(buffer.size)
                    val read = input.read(buffer, 0, toRead)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    total += read
                }
                String(out.toByteArray(), Charsets.UTF_8)
            }.orEmpty().trim()

            if (rawText.isEmpty()) {
                throw IllegalArgumentException("Master Hub Wiki URL returned an empty document.")
            }

            val parsed = parseAuto(uri.toString(), rawText)
            if (parsed.isEmpty()) {
                throw IllegalArgumentException("No Wiki canon entries could be parsed from the synced file.")
            }

            val fileName = uri.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: uri.host
                ?: "Master Hub Wiki"
            fileName to parsed
        }
    }

    fun saveCache(context: Context, state: MasterHubWikiCacheState) {
        val appCtx = context.applicationContext
        try {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SOURCE_LABEL, state.sourceLabel)
                .putString(KEY_WIKI_URL, state.wikiUrl)
                .putLong(KEY_LAST_SYNCED, state.lastSyncedMillis)
                .putBoolean(KEY_ENABLED_FOR_AI, state.enabledForAiBase)
                .apply()

            val targetFile = resolveSafeCacheFile(appCtx) ?: return
            val arr = JSONArray()
            state.entries.forEach { entry ->
                arr.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("title", entry.title)
                        put("category", entry.category)
                        put("summary", entry.summary)
                        put("visualTraits", entry.visualTraits)
                        put("tags", JSONArray(entry.tags))
                    }
                )
            }
            val payload = JSONObject().apply {
                put("schema", "pixelforge.master_hub_wiki_cache.v1")
                put("sourceLabel", state.sourceLabel)
                put("wikiUrl", state.wikiUrl)
                put("lastSyncedMillis", state.lastSyncedMillis)
                put("enabledForAiBase", state.enabledForAiBase)
                put("entries", arr)
            }
            targetFile.writeText(payload.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TRAINING_BASE_WIKI_TAG, "Failed to persist Master Hub Wiki cache: ${e.message}", e)
        }
    }

    fun loadCache(context: Context): MasterHubWikiCacheState {
        val appCtx = context.applicationContext
        return try {
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedLabel = prefs.getString(KEY_SOURCE_LABEL, "Not Synced").orEmpty().ifBlank { "Not Synced" }
            val savedUrl = prefs.getString(KEY_WIKI_URL, "").orEmpty()
            val savedMillis = prefs.getLong(KEY_LAST_SYNCED, 0L)
            val savedEnabled = prefs.getBoolean(KEY_ENABLED_FOR_AI, true)

            val file = resolveSafeCacheFile(appCtx)
            if (file == null || !file.exists()) {
                return MasterHubWikiCacheState(
                    sourceLabel = savedLabel,
                    wikiUrl = savedUrl,
                    entries = emptyList(),
                    lastSyncedMillis = savedMillis,
                    enabledForAiBase = savedEnabled
                )
            }

            val raw = file.readText(Charsets.UTF_8).trim()
            if (raw.isEmpty()) {
                return MasterHubWikiCacheState(
                    sourceLabel = savedLabel,
                    wikiUrl = savedUrl,
                    entries = emptyList(),
                    lastSyncedMillis = savedMillis,
                    enabledForAiBase = savedEnabled
                )
            }

            val root = JSONObject(raw)
            val entriesArr = root.optJSONArray("entries")
            val entries = mutableListOf<WikiCanonEntry>()
            if (entriesArr != null) {
                for (i in 0 until entriesArr.length()) {
                    val obj = entriesArr.optJSONObject(i) ?: continue
                    val tags = mutableListOf<String>()
                    val tagsArr = obj.optJSONArray("tags")
                    if (tagsArr != null) {
                        for (t in 0 until tagsArr.length()) {
                            val tagVal = tagsArr.optString(t, "").trim()
                            if (tagVal.isNotEmpty()) tags.add(tagVal)
                        }
                    }
                    val title = obj.optString("title", "").trim()
                    if (title.isNotEmpty()) {
                        entries.add(
                            WikiCanonEntry(
                                id = obj.optString("id", "wiki_$i").ifBlank { "wiki_$i" },
                                title = title,
                                category = obj.optString("category", "Lore").ifBlank { "Lore" },
                                summary = obj.optString("summary", ""),
                                visualTraits = obj.optString("visualTraits", ""),
                                tags = tags
                            )
                        )
                    }
                }
            }

            MasterHubWikiCacheState(
                sourceLabel = root.optString("sourceLabel", savedLabel).ifBlank { savedLabel },
                wikiUrl = root.optString("wikiUrl", savedUrl),
                entries = entries,
                lastSyncedMillis = root.optLong("lastSyncedMillis", savedMillis),
                enabledForAiBase = prefs.getBoolean(KEY_ENABLED_FOR_AI, root.optBoolean("enabledForAiBase", true))
            )
        } catch (e: Exception) {
            Log.w(TRAINING_BASE_WIKI_TAG, "Failed to load Master Hub Wiki cache: ${e.message}")
            MasterHubWikiCacheState()
        }
    }

    fun clearCache(context: Context): MasterHubWikiCacheState {
        val appCtx = context.applicationContext
        try {
            resolveSafeCacheFile(appCtx)?.let { file ->
                if (file.exists()) file.delete()
            }
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        } catch (e: Exception) {
            Log.w(TRAINING_BASE_WIKI_TAG, "Failed to clear Master Hub Wiki cache: ${e.message}")
        }
        return MasterHubWikiCacheState()
    }

    private fun parseArrayEntries(
        arr: JSONArray,
        defaultCategory: String,
        onEntry: (WikiCanonEntry) -> Unit
    ) {
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val title = firstNonBlankString(item, "title", "name", "heading", "label", "id")
            if (title.isEmpty()) continue
            val id = firstNonBlankString(item, "id", "slug").ifBlank { "${slugify(defaultCategory)}_${i}_${slugify(title)}" }
            val category = firstNonBlankString(item, "category", "type", "kind", "role").ifBlank { defaultCategory }
            val summary = firstNonBlankString(item, "summary", "description", "content", "bio", "lore", "overview", "body")
            val visualTraits = extractVisualTraits(item)
            val tags = extractTags(item)
            onEntry(
                WikiCanonEntry(
                    id = id,
                    title = title,
                    category = category,
                    summary = summary.ifBlank { visualTraits },
                    visualTraits = visualTraits,
                    tags = tags
                )
            )
        }
    }

    private fun firstNonBlankString(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            val value = obj.opt(key)
            if (value is String && value.trim().isNotEmpty()) {
                return value.trim()
            }
        }
        return ""
    }

    private fun extractVisualTraits(obj: JSONObject): String {
        val keys = listOf("visualTraits", "appearance", "designNotes", "visuals", "palette", "silhouette")
        val collected = mutableListOf<String>()
        for (key in keys) {
            when (val raw = obj.opt(key)) {
                is String -> {
                    val trimmed = raw.trim()
                    if (trimmed.isNotEmpty()) collected.add(trimmed)
                }
                is JSONArray -> {
                    val items = mutableListOf<String>()
                    for (i in 0 until raw.length()) {
                        val s = raw.optString(i, "").trim()
                        if (s.isNotEmpty()) items.add(s)
                    }
                    if (items.isNotEmpty()) collected.add(items.joinToString(", "))
                }
                is JSONObject -> {
                    val pairs = mutableListOf<String>()
                    val names = raw.keys()
                    while (names.hasNext()) {
                        val k = names.next()
                        val v = raw.optString(k, "").trim()
                        if (v.isNotEmpty()) pairs.add("$k: $v")
                    }
                    if (pairs.isNotEmpty()) collected.add(pairs.joinToString("; "))
                }
            }
        }
        return collected.distinct().joinToString("; ")
    }

    private fun extractTags(obj: JSONObject): List<String> {
        val keys = listOf("tags", "traits", "keywords", "affiliations")
        val tags = mutableListOf<String>()
        for (key in keys) {
            when (val raw = obj.opt(key)) {
                is JSONArray -> {
                    for (i in 0 until raw.length()) {
                        val s = raw.optString(i, "").trim()
                        if (s.isNotEmpty()) tags.add(s)
                    }
                }
                is String -> {
                    raw.split(',', ';', '|').map { it.trim() }.filter { it.isNotEmpty() }.forEach { tags.add(it) }
                }
            }
        }
        return tags.distinct()
    }

    private fun tokenizeMeaningful(text: String): Set<String> {
        return text.split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .toSet()
    }

    private fun slugify(raw: String): String {
        return raw.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(36)
            .ifBlank { "entry" }
    }

    private fun resolveSafeCacheFile(context: Context): File? {
        val filesDir = context.filesDir ?: return null
        val target = File(filesDir, CACHE_FILE_NAME)
        val canonicalBase = filesDir.canonicalPath
        val canonicalTarget = target.canonicalPath
        val prefix = if (canonicalBase.endsWith(File.separator)) canonicalBase else "$canonicalBase${File.separator}"
        return if (canonicalTarget.startsWith(prefix)) target else null
    }
}
