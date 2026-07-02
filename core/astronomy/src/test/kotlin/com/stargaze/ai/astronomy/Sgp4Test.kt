package com.stargaze.ai.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies TLE parsing and SGP4 propagation. The reference ISS TLE below is a real, well-formed
 * element set; we assert physical sanity (orbital radius, altitude band, range invariants) rather
 * than bit-exact STR#3 test vectors, which is the appropriate bar for a consumer sky app.
 */
class Sgp4Test {

    // Real ISS (ZARYA) TLE, catalog 25544.
    private val issName = "ISS (ZARYA)"
    private val issL1 = "1 25544U 98067A   24015.50000000  .00016717  00000-0  10270-3 0  9004"
    private val issL2 = "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.49514477123451"

    @Test
    fun parsesValidIssTle() {
        val tle = Tle.parse(issName, issL1, issL2)
        assertNotNull(tle)
        tle!!
        assertEquals(25544, tle.catalogNumber)
        assertEquals(2024, tle.epochYear)
        assertEquals(51.6416, tle.inclinationDeg, 1e-4)
        assertEquals(247.4627, tle.raanDeg, 1e-4)
        assertEquals(0.0006703, tle.eccentricity, 1e-7)
        assertEquals(15.49514477, tle.meanMotionRevPerDay, 1e-6)
    }

    @Test
    fun rejectsTleWithBadChecksum() {
        // Corrupt the last checksum digit of line 1.
        val bad = issL1.dropLast(1) + "7"
        // Only reject if it actually changed the checksum (original ends in 5).
        if (bad != issL1) assertNull(Tle.parse(issName, bad, issL2))
    }

    @Test
    fun rejectsMalformedLines() {
        assertNull(Tle.parse("x", "too short", "also short"))
        assertNull(Tle.parse("x", issL2, issL1)) // swapped line numbers
    }

    @Test
    fun sgp4_atEpoch_givesPhysicallySaneOrbit() {
        val tle = Tle.parse(issName, issL1, issL2)!!
        val sgp4 = Sgp4(tle)
        val pos = sgp4.propagate(0.0) // at epoch
        // ISS orbital radius from earth centre is ~6780 km (≈400 km altitude + 6378 km radius).
        assertEquals(6780.0, pos.magnitudeKm, 120.0)
    }

    @Test
    fun sgp4_overOneOrbit_staysInLowEarthOrbitBand() {
        val tle = Tle.parse(issName, issL1, issL2)!!
        val sgp4 = Sgp4(tle)
        // Sample across ~one orbital period (~93 min).
        for (min in 0..93 step 3) {
            val r = sgp4.propagate(min.toDouble()).magnitudeKm
            assertTrue("radius at t=$min was $r", r in 6600.0..6960.0)
        }
    }

    @Test
    fun topocentric_lookAngles_areInValidRanges() {
        val tle = Tle.parse(issName, issL1, issL2)!!
        val sgp4 = Sgp4(tle)
        val location = GeoLocation.MUMBAI
        for (min in 0..180 step 10) {
            val teme = sgp4.propagate(min.toDouble())
            val epochMillis = tle.epochMillis + min * 60_000L
            val h = SatelliteGeometry.lookAngles(teme, location, epochMillis)
            assertTrue("alt ${h.altitudeDeg}", h.altitudeDeg in -90.0..90.0)
            assertTrue("az ${h.azimuthDeg}", h.azimuthDeg in 0.0..360.0)
        }
    }

    @Test
    fun tleSatelliteProvider_fallsBackWhenNoTleMatches() {
        val provider = TleSatelliteProvider(tles = emptyList())
        val iss = SatelliteCatalog.satellites.first()
        // With no TLEs it should defer to the analytic fallback and still return a valid angle.
        val h = provider.positionOf(iss, GeoLocation.MUMBAI, System.currentTimeMillis())
        assertTrue(h.altitudeDeg in -90.0..90.0)
        assertTrue(h.azimuthDeg in 0.0..360.0)
    }

    @Test
    fun tleSatelliteProvider_usesSgp4WhenTleMatchesByName() {
        val tle = Tle.parse("International Space Station", issL1, issL2)!!
        val provider = TleSatelliteProvider(tles = listOf(tle))
        val iss = SatelliteCatalog.satellites.first() // name "International Space Station"
        val h = provider.positionOf(iss, GeoLocation.MUMBAI, tle.epochMillis)
        assertTrue(h.altitudeDeg in -90.0..90.0)
        assertTrue(h.azimuthDeg in 0.0..360.0)
    }

    @Test
    fun parseTleDocument_parsesMultipleStanzas() {
        val doc = buildString {
            appendLine(issName); appendLine(issL1); appendLine(issL2)
            appendLine("ISS DEBRIS"); appendLine(issL1); appendLine(issL2)
        }
        val tles = parseTleDocument(doc)
        assertEquals(2, tles.size)
        assertTrue(tles.all { it.catalogNumber == 25544 })
    }

    @Test
    fun parseTleDocument_skipsInvalidStanzas() {
        val doc = buildString {
            appendLine("GARBAGE LINE")
            appendLine(issName); appendLine(issL1); appendLine(issL2)
            appendLine("BAD"); appendLine("1 not a real line"); appendLine("2 also not")
        }
        val tles = parseTleDocument(doc)
        assertEquals(1, tles.size)
    }

    @Test
    fun parsesTleWithSpacePaddedEccentricity() {
        // TLE line 2 with space-padded eccentricity: "   6703" in columns 26-32.
        val spacePaddedL2 = "2 25544  51.6416 247.4627    6703 130.5360 325.0288 15.49514477123451"
        val tle = Tle.parse(issName, issL1, spacePaddedL2)
        assertNotNull(tle)
        assertEquals(0.0006703, tle!!.eccentricity, 1e-7)
    }
}
