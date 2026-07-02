package com.stargaze.ai.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StarDetectorTest {

    @Test
    fun detectsBrightSpotsAboveNoise() {
        val w = 64; val h = 64
        val lum = IntArray(w * h) { 10 } // dark background
        // Plant three bright blobs.
        fun blob(cx: Int, cy: Int, peak: Int) {
            for (dy in -1..1) for (dx in -1..1) {
                lum[(cy + dy) * w + (cx + dx)] = peak
            }
        }
        blob(10, 10, 240); blob(40, 30, 220); blob(20, 50, 200)

        val detected = StarDetector(sigmaThreshold = 4.0).detect(lum, w, h)
        assertEquals(3, detected.size)
        // Centroids should be near the planted centres.
        assertTrue(detected.any { kotlin.math.hypot(it.x - 10, it.y - 10) < 1.5 })
        assertTrue(detected.any { kotlin.math.hypot(it.x - 40, it.y - 30) < 1.5 })
    }

    @Test
    fun returnsEmptyForFlatImage() {
        val detected = StarDetector().detect(IntArray(32 * 32) { 50 }, 32, 32)
        assertTrue(detected.isEmpty())
    }
}

class PlateSolverTest {

    private val location = GeoLocation.MUMBAI
    // A time when Orion is comfortably above Mumbai's horizon (Jan evening, UTC).
    private val epochMillis = Instant.parse("2026-01-15T16:30:00Z").toEpochMilli()
    private val width = 1080
    private val height = 1920
    private val fov = 60.0

    /** Finds a pointing (az/alt) centred on a constellation that is currently up. */
    private fun centreOn(constellation: String): Horizontal? =
        SkyEngine().centroidOf(ConstellationCatalog.byName.getValue(constellation), location, epochMillis)

    @Test
    fun recoversKnownOffset_andIdentifiesConstellation() {
        val centre = centreOn("Orion") ?: return // skip if not up at this instant
        if (centre.altitudeDeg < 15) return

        // Truth pointing is the constellation centre. We synthesise detections by projecting catalog
        // stars at the TRUTH pointing, then give the solver a PRIOR that is offset (simulated drift).
        val truthAz = centre.azimuthDeg
        val truthAlt = centre.altitudeDeg
        val truth = GnomonicProjection(width, height, truthAz, truthAlt, fov)
        val detections = StarCatalog.stars.mapNotNull { star ->
            val hz = CoordinateTransforms.equatorialToHorizontal(star.equatorial, location, epochMillis)
            if (hz.altitudeDeg < 0) return@mapNotNull null
            truth.project(hz.azimuthDeg, hz.altitudeDeg)
                ?.takeIf { it.x in 0.0..width.toDouble() && it.y in 0.0..height.toDouble() }
                ?.let { DetectedStar(it.x, it.y, 100.0 - star.magnitude * 10) }
        }
        if (detections.size < 4) return

        val driftAz = 3.0   // degrees of simulated compass drift
        val driftAlt = -2.0
        val solver = PlateSolver()
        val solution = solver.solve(
            detections = detections,
            location = location,
            epochMillis = epochMillis,
            priorAzimuthDeg = truthAz + driftAz,
            priorAltitudeDeg = truthAlt + driftAlt,
            fieldOfViewDeg = fov,
            rollDeg = 0.0,
            widthPx = width,
            heightPx = height,
        )

        assertTrue("should solve", solution.solved)
        // The solver should correct back toward the truth (offset roughly cancels the drift).
        assertEquals(-driftAz, solution.azimuthOffsetDeg, 1.0)
        assertEquals(-driftAlt, solution.altitudeOffsetDeg, 1.0)
        assertEquals(truthAz, solution.correctedAzimuthDeg, 1.0)
        assertTrue("Orion identified", solution.constellations.contains("Orion"))
        assertTrue(solution.confidence > 0.0)
    }

    @Test
    fun reportsUnsolved_withTooFewDetections() {
        val solution = PlateSolver().solve(
            detections = listOf(DetectedStar(10.0, 10.0, 100.0)),
            location = location, epochMillis = epochMillis,
            priorAzimuthDeg = 180.0, priorAltitudeDeg = 45.0, fieldOfViewDeg = fov,
            rollDeg = 0.0, widthPx = width, heightPx = height,
        )
        assertFalse(solution.solved)
        assertEquals(0.0, solution.confidence, 1e-9)
    }
}
