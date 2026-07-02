package com.stargaze.ai.astronomy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Verifies the astronomy core against independently-known reference values. These are the
 * highest-risk computations in the product, so they are asserted directly rather than by snapshot.
 */
class AstronomyEngineTest {

    private fun millisOf(isoUtc: String): Long = Instant.parse(isoUtc).toEpochMilli()

    // J2000.0 epoch = 2000-01-01T12:00:00Z, JD should be exactly 2451545.0
    @Test
    fun julianDate_atJ2000_isCorrect() {
        val millis = millisOf("2000-01-01T12:00:00Z")
        assertEquals(2451545.0, SiderealTime.julianDate(millis), 1e-6)
        assertEquals(0.0, SiderealTime.daysSinceJ2000(millis), 1e-6)
    }

    // GMST at J2000.0 noon is ~280.46 degrees by definition of the formula.
    @Test
    fun gmst_atJ2000_matchesReference() {
        val millis = millisOf("2000-01-01T12:00:00Z")
        val gmst = SiderealTime.greenwichMeanSiderealTimeDeg(millis)
        assertEquals(280.46061837, gmst, 1e-4)
    }

    @Test
    fun normalizeDegrees_wrapsIntoRange() {
        assertEquals(10.0, Angles.normalizeDegrees(370.0), 1e-9)
        assertEquals(350.0, Angles.normalizeDegrees(-10.0), 1e-9)
        assertEquals(0.0, Angles.normalizeDegrees(360.0), 1e-9)
    }

    // Polaris sits ~0.7 deg from the celestial pole, so its altitude must equal the observer's
    // latitude to within ~1 degree, regardless of time of day.
    @Test
    fun polaris_altitude_equalsObserverLatitude() {
        val polaris = StarCatalog.byName.getValue("Polaris")
        val location = GeoLocation(40.0, -74.0, "Test")
        // Sample several times across a day; altitude should stay near the latitude.
        for (hour in 0..23 step 3) {
            val millis = millisOf("2026-03-20T%02d:00:00Z".format(hour))
            val h = CoordinateTransforms.equatorialToHorizontal(polaris.equatorial, location, millis)
            assertEquals("hour=$hour", 40.0, h.altitudeDeg, 1.2)
        }
    }

    // A star with declination > (90 - latitude) is circumpolar (never sets) for a northern observer.
    @Test
    fun circumpolarStar_neverSets() {
        val location = GeoLocation(60.0, 10.0, "Oslo-ish")
        val dubhe = StarCatalog.byName.getValue("Dubhe") // dec ~+61.7, circumpolar at lat 60
        for (hour in 0..23) {
            val millis = millisOf("2026-06-21T%02d:00:00Z".format(hour))
            val h = CoordinateTransforms.equatorialToHorizontal(dubhe.equatorial, location, millis)
            assertTrue("Dubhe should stay above horizon at hour=$hour (alt=${h.altitudeDeg})", h.altitudeDeg > 0)
        }
    }

    // Horizontal output must always be in valid ranges for any input.
    @Test
    fun horizontal_rangesAreValid() {
        val location = GeoLocation.MUMBAI
        val millis = millisOf("2026-06-07T18:30:00Z")
        for (star in StarCatalog.stars) {
            val h = CoordinateTransforms.equatorialToHorizontal(star.equatorial, location, millis)
            assertTrue("alt range ${star.name}", h.altitudeDeg in -90.0..90.0)
            assertTrue("az range ${star.name}", h.azimuthDeg in 0.0..360.0)
        }
    }

    // The Sun's declination is bounded by the obliquity (~23.44 deg) year-round.
    @Test
    fun sun_declination_withinObliquityBounds() {
        for (month in 1..12) {
            val millis = millisOf("2026-%02d-15T12:00:00Z".format(month))
            val eq = Ephemeris.position(Body.SUN, millis)
            assertTrue("month=$month dec=${eq.declinationDeg}", eq.declinationDeg in -23.6..23.6)
        }
    }

    // Around the June solstice the Sun's declination is near its maximum (+23.4 deg).
    @Test
    fun sun_nearJuneSolstice_isHighDeclination() {
        val millis = millisOf("2026-06-21T12:00:00Z")
        val eq = Ephemeris.position(Body.SUN, millis)
        assertEquals(23.4, eq.declinationDeg, 0.6)
    }

    // Around the March equinox the Sun's RA is near 0h and dec near 0.
    @Test
    fun sun_nearMarchEquinox_isNearZeroDeclination() {
        val millis = millisOf("2026-03-20T12:00:00Z")
        val eq = Ephemeris.position(Body.SUN, millis)
        assertEquals(0.0, eq.declinationDeg, 1.0)
    }

    // All planets produce finite, in-range equatorial coordinates.
    @Test
    fun planets_produceValidCoordinates() {
        val millis = millisOf("2026-06-07T00:00:00Z")
        for (body in Body.entries) {
            val eq = Ephemeris.position(body, millis)
            assertTrue("${body.displayName} ra", eq.rightAscensionDeg in 0.0..360.0)
            assertTrue("${body.displayName} dec", eq.declinationDeg in -90.0..90.0)
        }
    }

    @Test
    fun geoLocation_ofClamped_handlesOutOfRangeInput() {
        val g = GeoLocation.ofClamped(120.0, 540.0, "bad")
        assertTrue(g.latitudeDeg == 90.0)
        assertTrue(g.longitudeDeg in -180.0..180.0)
    }
}
