package com.stargaze.ai.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the AI Sky Guide proxy. All fields are validated/bounded before use; the
 * serializer is configured with `ignoreUnknownKeys = true` so an evolving server contract cannot
 * crash the client (untrusted-input hardening).
 */
@Serializable
data class AiGuideRequest(
    @SerialName("question") val question: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("location_name") val locationName: String,
    @SerialName("epoch_millis") val epochMillis: Long,
    @SerialName("client") val client: String = "android",
)

@Serializable
data class AiGuideResponse(
    @SerialName("answer") val answer: String = "",
)

/**
 * Scene-analysis request: a grounded sky description (computed on-device) plus the user's question
 * and an optional base64-encoded JPEG of the sky. The image is only ever included on the cloud path
 * and only with explicit consent. Coarse location/time accompany it for context.
 */
@Serializable
data class SceneAnalysisRequest(
    @SerialName("question") val question: String,
    @SerialName("scene_context") val sceneContext: String,
    @SerialName("image_base64") val imageBase64: String? = null,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("epoch_millis") val epochMillis: Long,
    @SerialName("client") val client: String = "android",
)
