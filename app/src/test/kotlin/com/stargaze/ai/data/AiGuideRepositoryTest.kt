package com.stargaze.ai.data

import com.stargaze.ai.ai.OnDeviceAi
import com.stargaze.ai.astronomy.GeoLocation
import com.stargaze.ai.astronomy.KnowledgeEngine
import com.stargaze.ai.network.AiGuideRequest
import com.stargaze.ai.network.AiGuideResponse
import com.stargaze.ai.network.AiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Verifies the AI Guide's 3-tier routing using test doubles (no mocking library). Every tier is a
 * real implementation; these tests prove the routing/consent/fallback contract.
 */
class AiGuideRepositoryTest {

    private val location = GeoLocation.MUMBAI
    private val now = 1_780_000_000_000L

    private class SuccessService(val answer: String) : AiService {
        override suspend fun ask(request: AiGuideRequest) = AiGuideResponse(answer)
        override suspend fun analyzeScene(request: com.stargaze.ai.network.SceneAnalysisRequest) = AiGuideResponse(answer)
    }
    private class FailingService : AiService {
        override suspend fun ask(request: AiGuideRequest): AiGuideResponse = throw IOException("offline")
        override suspend fun analyzeScene(request: com.stargaze.ai.network.SceneAnalysisRequest): AiGuideResponse = throw IOException("offline")
    }
    private class EchoService : AiService {
        var lastQuestion: String = ""
        override suspend fun ask(request: AiGuideRequest): AiGuideResponse {
            lastQuestion = request.question; return AiGuideResponse("cloud answer")
        }
        override suspend fun analyzeScene(request: com.stargaze.ai.network.SceneAnalysisRequest): AiGuideResponse {
            lastQuestion = request.question; return AiGuideResponse("cloud scene")
        }
    }

    private class FakeOnDevice(val downloaded: Boolean, val answer: String?) : OnDeviceAi {
        override fun isDownloaded(): Boolean = downloaded
        override suspend fun ask(question: String): String? = answer
        override suspend fun explainScene(question: String, sceneContext: String): String? = answer
    }

    private fun repo(
        service: AiService?,
        onDevice: OnDeviceAi,
        cloudConsent: Boolean,
        deviceEnabled: Boolean,
    ) = AiGuideRepository(
        aiService = service,
        knowledgeEngine = KnowledgeEngine(),
        onDeviceAi = onDevice,
        aiPrefsProvider = { AiPrefs(cloudAiConsent = cloudConsent, onDeviceAiEnabled = deviceEnabled) },
        sceneContextBuilder = com.stargaze.ai.astronomy.SceneContextBuilder(),
    )

    @Test
    fun tier1_structuredKnowledge_wins_andReportsOfflineKnowledge() = runTest {
        val r = repo(SuccessService("cloud"), FakeOnDevice(true, "device"), cloudConsent = true, deviceEnabled = true)
        val a = r.ask("Tell me about Sirius", location, now)
        assertEquals(AiGuideRepository.Source.OFFLINE_KNOWLEDGE, a.source)
        assertTrue(a.text.contains("Sirius"))
    }

    @Test
    fun tier2_onDevice_usedForOpenEnded_whenEnabledAndDownloaded() = runTest {
        val r = repo(SuccessService("cloud"), FakeOnDevice(true, "On-device says hi"), cloudConsent = true, deviceEnabled = true)
        val a = r.ask("compose a haiku about nebulae", location, now)
        assertEquals(AiGuideRepository.Source.ON_DEVICE, a.source)
        assertEquals("On-device says hi", a.text)
    }

    @Test
    fun tier3_cloud_usedWhenConsented_andOnDeviceUnavailable() = runTest {
        val r = repo(SuccessService("Cloud answer"), FakeOnDevice(false, null), cloudConsent = true, deviceEnabled = false)
        val a = r.ask("compose a haiku about nebulae", location, now)
        assertEquals(AiGuideRepository.Source.REMOTE, a.source)
        assertEquals("Cloud answer", a.text)
    }

    @Test
    fun onDevice_skipped_whenDisabled_evenIfDownloaded() = runTest {
        val r = repo(SuccessService("Cloud answer"), FakeOnDevice(true, "device"), cloudConsent = true, deviceEnabled = false)
        val a = r.ask("compose a haiku about nebulae", location, now)
        assertEquals(AiGuideRepository.Source.REMOTE, a.source)
    }

    @Test
    fun cloud_notUsed_withoutConsent_fallsBackToOfflineGeneric() = runTest {
        val r = repo(SuccessService("Cloud answer"), FakeOnDevice(false, null), cloudConsent = false, deviceEnabled = false)
        val a = r.ask("compose a haiku about nebulae", location, now)
        assertEquals(AiGuideRepository.Source.OFFLINE_KNOWLEDGE, a.source)
        assertTrue(a.text.isNotBlank())
    }

    @Test
    fun cloudFailure_fallsBackToOfflineGeneric() = runTest {
        val r = repo(FailingService(), FakeOnDevice(false, null), cloudConsent = true, deviceEnabled = false)
        val a = r.ask("compose a haiku about nebulae", location, now)
        assertEquals(AiGuideRepository.Source.OFFLINE_KNOWLEDGE, a.source)
    }

    @Test
    fun sanitizesInput_cappingLengthAndStrippingControlChars() = runTest {
        val echo = EchoService()
        val r = repo(echo, FakeOnDevice(false, null), cloudConsent = true, deviceEnabled = false)
        val noisy = "haiku\u0000\u0007 " + "x".repeat(1000)
        r.ask(noisy, location, now)
        assertTrue("control chars stripped", !echo.lastQuestion.contains('\u0000'))
        assertTrue("length capped to 500", echo.lastQuestion.length <= 500)
    }
}
