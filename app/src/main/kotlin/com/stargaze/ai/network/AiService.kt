package com.stargaze.ai.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit contract for the first-party AI proxy. The proxy holds the LLM API key server-side; the
 * app never sees or stores it. The endpoint is reached only over HTTPS (enforced by the network
 * security config and OkHttp configuration).
 */
interface AiService {
    @POST("v1/guide")
    suspend fun ask(@Body request: AiGuideRequest): AiGuideResponse

    @POST("v1/analyze-scene")
    suspend fun analyzeScene(@Body request: SceneAnalysisRequest): AiGuideResponse
}
