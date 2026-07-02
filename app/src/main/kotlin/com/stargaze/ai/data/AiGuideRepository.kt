package com.stargaze.ai.data

import com.stargaze.ai.ai.OnDeviceAi
import com.stargaze.ai.astronomy.GeoLocation
import com.stargaze.ai.astronomy.KnowledgeEngine
import com.stargaze.ai.network.AiGuideRequest
import com.stargaze.ai.network.AiService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Snapshot of the AI consent/mode flags the repository needs to route a question. */
data class AiPrefs(val cloudAiConsent: Boolean, val onDeviceAiEnabled: Boolean)

/** Supplies the current [AiPrefs]. Implemented by the settings layer; faked in tests. */
fun interface AiPrefsProvider {
    suspend fun current(): AiPrefs
}

/**
 * The AI Sky Guide's answer engine, composed of three real tiers (no mocks), tried most-private and
 * most-reliable first:
 *
 * 1. **Structured offline knowledge** ([KnowledgeEngine.tryAnswer]) — deterministic, location-aware,
 *    instant, fully offline. No data leaves the device.
 * 2. **On-device Gemma 3n** ([OnDeviceAi]) — for open-ended questions the knowledge engine doesn't
 *    recognise, *if* the user enabled it and the model is downloaded. Runs locally; no data egress.
 * 3. **Cloud LLM** ([AiService] via proxy) — only with explicit cloud-AI consent and a configured
 *    proxy. Sends question + coarse context over HTTPS.
 *
 * Final fallback is the knowledge engine's generic help text, so a useful answer is always returned.
 */
@Singleton
class AiGuideRepository @Inject constructor(
    private val aiService: AiService?,                 // null when no proxy configured
    private val knowledgeEngine: KnowledgeEngine,
    private val onDeviceAi: OnDeviceAi,
    private val aiPrefsProvider: AiPrefsProvider,
    private val sceneContextBuilder: com.stargaze.ai.astronomy.SceneContextBuilder,
) {
    /** Where the answer came from, surfaced to the UI for transparency. */
    enum class Source { OFFLINE_KNOWLEDGE, ON_DEVICE, REMOTE }

    data class Answer(val text: String, val source: Source)

    suspend fun ask(question: String, location: GeoLocation, epochMillis: Long): Answer {
        val sanitized = sanitize(question)
        val ctx = KnowledgeEngine.Context(location, epochMillis)

        // Tier 1: structured offline knowledge (deterministic, private, instant).
        knowledgeEngine.tryAnswer(sanitized, ctx)?.let { return Answer(it, Source.OFFLINE_KNOWLEDGE) }

        val prefs = aiPrefsProvider.current()

        // Tier 2: on-device Gemma, if the user enabled it and the model is ready.
        if (prefs.onDeviceAiEnabled && onDeviceAi.isDownloaded()) {
            onDeviceAi.ask(sanitized)?.let { return Answer(it, Source.ON_DEVICE) }
        }

        // Tier 3: cloud LLM, only with explicit consent and a configured proxy.
        val service = aiService
        if (prefs.cloudAiConsent && service != null) {
            runCatching {
                service.ask(
                    AiGuideRequest(
                        question = sanitized,
                        latitude = location.latitudeDeg,
                        longitude = location.longitudeDeg,
                        locationName = location.name,
                        epochMillis = epochMillis,
                    ),
                ).answer.trim()
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return Answer(it, Source.REMOTE) }
        }

        // Guaranteed useful fallback (still on-device).
        return Answer(knowledgeEngine.answer(sanitized, ctx), Source.OFFLINE_KNOWLEDGE)
    }

    /**
     * Grounded multimodal scene explanation for the camera "Identify sky" flow.
     *
     * The sky region is described factually from computed truth (catalog + ephemerides + live TLE
     * satellites + plate-solved constellations); the AI is asked to *explain* that truth, never to
     * identify a photo blindly. Tiers: on-device Gemma (grounded text) → cloud VLM (grounded, with
     * optional image, consent-gated) → deterministic summary of the computed context.
     */
    suspend fun analyzeScene(
        question: String,
        location: GeoLocation,
        epochMillis: Long,
        centerAzimuthDeg: Double,
        centerAltitudeDeg: Double,
        identifiedConstellations: List<String>,
        imageBase64: String? = null,
    ): Answer {
        val sceneContext = sceneContextBuilder.describe(
            location, epochMillis, centerAzimuthDeg, centerAltitudeDeg,
            identifiedConstellations = identifiedConstellations,
        )
        val q = sanitize(question.ifBlank { "What am I looking at in the sky right now?" })
        val prefs = aiPrefsProvider.current()

        if (prefs.onDeviceAiEnabled && onDeviceAi.isDownloaded()) {
            onDeviceAi.explainScene(q, sceneContext)?.let { return Answer(it, Source.ON_DEVICE) }
        }

        val service = aiService
        if (prefs.cloudAiConsent && service != null) {
            runCatching {
                service.analyzeScene(
                    com.stargaze.ai.network.SceneAnalysisRequest(
                        question = q,
                        sceneContext = sceneContext,
                        imageBase64 = imageBase64,
                        latitude = location.latitudeDeg,
                        longitude = location.longitudeDeg,
                        epochMillis = epochMillis,
                    ),
                ).answer.trim()
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return Answer(it, Source.REMOTE) }
        }

        // Deterministic, useful summary of the computed scene (always works offline).
        return Answer("Here's what's in view right now:\n\n$sceneContext", Source.OFFLINE_KNOWLEDGE)
    }

    /**
     * Bounds and cleans user input before it leaves the device. Length-capping mitigates abuse and
     * runaway cost; control-character stripping avoids malformed payloads.
     */
    private fun sanitize(raw: String): String {
        val trimmed = raw.trim().take(MAX_QUESTION_CHARS)
        return trimmed.filter { it == '\n' || !it.isISOControl() }
    }

    private companion object {
        const val MAX_QUESTION_CHARS = 500
    }
}
