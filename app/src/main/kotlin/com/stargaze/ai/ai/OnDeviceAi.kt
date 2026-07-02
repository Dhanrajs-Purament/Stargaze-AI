package com.stargaze.ai.ai

/**
 * Narrow interface the AI repository depends on for the on-device tier. Extracting it keeps the
 * repository testable with a simple fake (no Context, no native MediaPipe, no multi-GB model).
 */
interface OnDeviceAi {
    /** True when a verified model for this device is present and ready to answer. */
    fun isDownloaded(): Boolean

    /** Answer locally, or null if unavailable/failed so the caller can fall back. */
    suspend fun ask(question: String): String?

    /** Explain a grounded sky scene locally, or null if unavailable. */
    suspend fun explainScene(question: String, sceneContext: String): String?
}
