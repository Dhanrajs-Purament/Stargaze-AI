package com.stargaze.ai.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thin lifecycle wrapper around MediaPipe's [LlmInference] running a local Gemma 3n model.
 *
 * - The engine is created lazily from a verified model file path and reused across questions
 *   (model load is expensive).
 * - Inference runs on [Dispatchers.Default] (CPU/accelerator-bound) and is serialised with a mutex,
 *   since a single [LlmInference] session is not safe for concurrent calls.
 * - Inputs are wrapped in a system instruction that scopes the model to astronomy and adds the
 *   educational, non-advice framing required by our AI disclosure.
 */
class OnDeviceAiEngine(private val context: Context) {

    private var inference: LlmInference? = null
    private var loadedPath: String? = null
    private val mutex = Mutex()

    val isLoaded: Boolean get() = inference != null

    /** Loads the model at [modelPath] if not already loaded. Safe to call repeatedly. */
    suspend fun ensureLoaded(modelPath: String): Boolean = mutex.withLock {
        if (inference != null && loadedPath == modelPath) return@withLock true
        closeInternal()
        withContext(Dispatchers.Default) {
            runCatching {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .build()
                inference = LlmInference.createFromOptions(context, options)
                loadedPath = modelPath
            }.isSuccess
        }
    }

    /**
     * Generates an answer for [question]. Returns null if the model is not loaded or inference fails,
     * so the caller can fall back to another tier.
     */
    suspend fun generate(question: String): String? = mutex.withLock {
        val engine = inference ?: return@withLock null
        withContext(Dispatchers.Default) {
            runCatching {
                engine.generateResponse(buildPrompt(question))?.trim()?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }

    /** Releases native resources. Call when the model is disabled or on memory pressure. */
    suspend fun close() = mutex.withLock { closeInternal() }

    /**
     * Explains the sky region described by [sceneContext] (computed ground truth) in response to
     * [question]. The model is grounded with the facts and explicitly told not to identify from
     * imagination — it explains the supplied truth. Returns null if unavailable.
     */
    suspend fun explainScene(question: String, sceneContext: String): String? = mutex.withLock {
        val engine = inference ?: return@withLock null
        withContext(Dispatchers.Default) {
            runCatching {
                engine.generateResponse(buildScenePrompt(question, sceneContext))?.trim()?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }

    private fun closeInternal() {
        runCatching { inference?.close() }
        inference = null
        loadedPath = null
    }

    private fun buildPrompt(question: String): String =
        """
        You are StarGaze AI, a friendly astronomy tutor inside a stargazing app.
        Answer clearly and concisely for a curious learner. Only discuss astronomy and the night sky.
        This is educational information, not professional, medical, legal, or safety advice.
        If you are unsure, say so rather than inventing facts.

        Question: ${question.take(MAX_QUESTION_CHARS)}
        Answer:
        """.trimIndent()

    private fun buildScenePrompt(question: String, sceneContext: String): String =
        """
        You are StarGaze AI, a friendly astronomy tutor. Below is a FACTUAL description of the sky
        the user is currently looking at, computed precisely from their location, the time, star
        catalogs, and live satellite orbits. Treat it as ground truth. Explain what they're seeing in
        an engaging, educational way. Do NOT invent objects that are not listed; if the user asks
        about something not present, say it isn't currently in view. This is educational information,
        not professional or safety advice.

        SKY FACTS:
        $sceneContext

        User question: ${question.take(MAX_QUESTION_CHARS).ifBlank { "What am I looking at?" }}
        Answer:
        """.trimIndent()

    private companion object {
        const val MAX_TOKENS = 512
        const val MAX_QUESTION_CHARS = 500
    }
}
