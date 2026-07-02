package com.stargaze.ai.astronomy

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class KnowledgeEngineTest {

    private val engine = KnowledgeEngine()
    private fun ctx(iso: String = "2026-06-07T18:30:00Z") =
        KnowledgeEngine.Context(GeoLocation.MUMBAI, Instant.parse(iso).toEpochMilli())

    @Test
    fun answersAboutKnownStar_includesConstellationAndVisibility() {
        val a = engine.answer("Tell me about Sirius", ctx())
        assertTrue(a.contains("Sirius"))
        assertTrue(a.contains("Canis Major"))
        // must include a computed visibility verdict
        assertTrue(a.contains("up") || a.contains("below your horizon"))
    }

    @Test
    fun answersAboutMarsRed_explainsRust() {
        val a = engine.answer("why is mars red", ctx())
        assertTrue(a.lowercase().contains("rust") || a.lowercase().contains("iron oxide"))
    }

    @Test
    fun answersAboutIss_includesStation() {
        val a = engine.answer("where is the ISS now?", ctx())
        assertTrue(a.contains("International Space Station"))
    }

    @Test
    fun answersTonight_isLocationAware() {
        val a = engine.answer("what should I look at tonight?", ctx())
        assertTrue(a.contains("Mumbai"))
    }

    @Test
    fun answersLightYear_concept() {
        val a = engine.answer("what's a light-year?", ctx())
        assertTrue(a.contains("distance"))
    }

    @Test
    fun unknownQuestion_returnsHelpfulFallback() {
        val a = engine.answer("asdfqwer zzz", ctx())
        assertTrue(a.isNotBlank())
    }

    @Test
    fun doesNotMatchSubstringsOfCommonWords() {
        // "weight" contains "wei" (star), but should not match it.
        val a = engine.tryAnswer("what is the weight of Mars?", ctx())
        // Since it's about Mars, it should match the planet Mars rather than the star Wei!
        assertTrue(a != null)
        assertTrue(a!!.contains("Mars"))
        assertTrue(!a.contains("Wei"))

        // "sunspot" contains "sun" (body), but should not match it.
        val b = engine.tryAnswer("tell me about sunspots", ctx())
        org.junit.Assert.assertNull(b)
    }
}

class SkyEngineTest {

    private val engine = SkyEngine()
    private val millis = Instant.parse("2026-06-07T18:30:00Z").toEpochMilli()

    @Test
    fun catalogsAreLoaded() {
        assertTrue(engine.stars.size >= 70)
        assertTrue(engine.planets.size == 7)
        assertTrue(engine.constellations.size >= 16)
        assertTrue(engine.satellites.size == 3)
    }

    @Test
    fun constellationCentroid_resolvesForOrion() {
        val orion = ConstellationCatalog.byName.getValue("Orion")
        val centroid = engine.centroidOf(orion, GeoLocation.MUMBAI, millis)
        assertTrue(centroid != null)
        assertTrue(centroid!!.azimuthDeg in 0.0..360.0)
    }

    @Test
    fun planetsUp_andStarsUp_returnInRangePositions() {
        val planets = engine.planetsUp(GeoLocation.MUMBAI, millis)
        planets.forEach { assertTrue(it.horizontal.isAboveHorizon) }
        val stars = engine.brightStarsUp(GeoLocation.MUMBAI, millis)
        stars.forEach { assertTrue(it.horizontal.altitudeDeg > 5.0) }
    }

    @Test
    fun everyConstellationLineEndpoint_existsInStarCatalog() {
        // Guards against typos that would break the renderer's line drawing.
        for (c in engine.constellations) {
            for ((a, b) in c.lines) {
                assertTrue("missing $a in ${c.name}", StarCatalog.byName.containsKey(a))
                assertTrue("missing $b in ${c.name}", StarCatalog.byName.containsKey(b))
            }
        }
    }
}
