package com.stargaze.ai.astronomy

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SceneContextBuilderTest {

    private val location = GeoLocation.MUMBAI
    private val epochMillis = Instant.parse("2026-01-15T16:30:00Z").toEpochMilli()

    @Test
    fun describesObserverAndPointing() {
        val ctx = SceneContextBuilder().describe(location, epochMillis, 120.0, 40.0)
        assertTrue(ctx.contains("Mumbai"))
        assertTrue(ctx.contains("azimuth"))
        assertTrue(ctx.contains("altitude"))
    }

    @Test
    fun includesIdentifiedConstellationsWhenProvided() {
        val ctx = SceneContextBuilder().describe(
            location, epochMillis, 120.0, 40.0,
            identifiedConstellations = listOf("Orion"),
        )
        assertTrue(ctx.contains("Orion"))
        assertTrue(ctx.contains("plate-solved"))
    }

    @Test
    fun groundsWithBrightStarsInView() {
        // Point at Orion's centroid; the context should mention at least one of its stars.
        val centre = SkyEngine().centroidOf(ConstellationCatalog.byName.getValue("Orion"), location, epochMillis)
        if (centre == null || centre.altitudeDeg < 10) return
        val ctx = SceneContextBuilder().describe(location, epochMillis, centre.azimuthDeg, centre.altitudeDeg)
        assertTrue(ctx.contains("Bright stars in view") || ctx.contains("Orion"))
    }
}
