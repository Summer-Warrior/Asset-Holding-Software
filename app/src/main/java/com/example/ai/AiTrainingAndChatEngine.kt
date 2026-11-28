package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.local.AssetGalleryWithItems
import com.example.model.audio.AudioAssetEntry
import com.example.model.masterpack.FrameCadenceFps
import com.example.model.model3d.Scratch3DProject
import com.example.model.texture.ScratchTextureProject
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TRAINING_CHAT_TAG = "AiTrainingAndChat"

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
 * Unified user AI training & style-trigger profile automatically synchronized across
 * BOTH Studio Form creation and AI Chat Studio multi-turn directing.
 */
data class UserAiTrainingProfile(
    val styleTriggerToken: String = "",
    val customLoraOrModelTag: String = "",
    val customTrainingNotes: String = "",
    val attachedTrainingFileCount: Int = 0,
    val applyToFormBuilder: Boolean = true,
    val applyToChatStudio: Boolean = true
) {
    /**
     * Formats non-blank trigger token, custom LoRA / fine-tuned model tag, and custom training notes
     * into a compact structured context header shared across AI requests. Returns `""` if all blank.
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
            put("schema", "pixelforge.user_ai_training_profile.v1")
            put("styleTriggerToken", styleTriggerToken)
            put("customLoraOrModelTag", customLoraOrModelTag)
            put("customTrainingNotes", customTrainingNotes)
            put("attachedTrainingFileCount", attachedTrainingFileCount.coerceAtLeast(0))
            put("applyToFormBuilder", applyToFormBuilder)
            put("applyToChatStudio", applyToChatStudio)
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
                    applyToChatStudio = root.optBoolean("applyToChatStudio", true)
                )
            } catch (e: Exception) {
                Log.w(TRAINING_CHAT_TAG, "Failed to parse UserAiTrainingProfile JSON: ${e.message}")
                UserAiTrainingProfile()
            }
        }
    }
}

/**
 * Persists [UserAiTrainingProfile] via private [android.content.SharedPreferences].
 */
object UserAiTrainingProfileStore {
    private const val PREFS_NAME = "pixelforge_user_ai_training_profile"
    private const val KEY_PROFILE_JSON = "profile_json"

    fun save(context: Context, profile: UserAiTrainingProfile) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PROFILE_JSON, profile.toJsonString()).apply()
        } catch (e: Exception) {
            Log.w(TRAINING_CHAT_TAG, "Could not persist UserAiTrainingProfile: ${e.message}")
        }
    }

    fun load(context: Context): UserAiTrainingProfile {
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PROFILE_JSON, null)
            if (raw.isNullOrBlank()) UserAiTrainingProfile() else UserAiTrainingProfile.fromJsonString(raw)
        } catch (e: Exception) {
            Log.w(TRAINING_CHAT_TAG, "Could not load UserAiTrainingProfile: ${e.message}")
            UserAiTrainingProfile()
        }
    }
}

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

        // 1. Active 3D Blueprint JSONL sample
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

        // 2. Active Texture Project JSONL sample
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

        // 3. Audio Vault Procedural Cry & Chiptune JSONL samples
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

        // Guarantee at least 1 valid schema exemplar line if studio is completely blank
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
     * For `.jsonl`, extracts up to 8 valid `messages` conversation pairs while skipping malformed lines cleanly.
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
                        Log.w(TRAINING_CHAT_TAG, "Skipping malformed JSONL line in $cleanName: ${e.message}")
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
                Log.w(TRAINING_CHAT_TAG, "JSON training file '$cleanName' fell back to raw text: ${e.message}")
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

    /**
     * Assembles a multi-turn conversation prompt containing the Game Asset Director role,
     * unified user training profile (when enabled), shared multi-source context, `TARGET_FPS`,
     * the last 10 turns of conversation [history], and the latest [userMessage].
     */
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
            if (trainingBlock.isNotBlank()) {
                appendLine()
                appendLine(trainingBlock)
            }
            if (cleanShared.isNotBlank()) {
                appendLine()
                appendLine("=== SHARED KNOWLEDGE & PINNED EXAMPLES ===")
                appendLine(cleanShared.take(4500))
                appendLine("=== END SHARED KNOWLEDGE ===")
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

    /**
     * Extracts any `BUILD_PROMPT:` line (or a clean trimmed summary) from [replyText]
     * and infers the matching [AiBuildTargetKind].
     */
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
