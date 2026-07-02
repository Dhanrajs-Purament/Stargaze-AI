package com.stargaze.ai.astronomy

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Solar-system body positions using Paul Schlyter's low-precision ephemeris
 * ("How to compute planetary positions"). Accuracy is ~1–2 arcminutes for the Sun and a few
 * arcminutes for planets over the modern era — far better than the human eye or a phone's pointing
 * accuracy, and identical in approach to the validated web prototype.
 *
 * Output is geocentric apparent equatorial coordinates (RA/Dec, degrees), ready for
 * [CoordinateTransforms.equatorialToHorizontal].
 */
object Ephemeris {

    /** Obliquity of the ecliptic (mean), degrees. */
    private const val OBLIQUITY_DEG = 23.4393

    /** Keplerian orbital elements as linear functions of d = days since the Schlyter epoch. */
    private data class Elements(
        val nodeBase: Double, val nodeRate: Double,        // longitude of ascending node N (deg)
        val inclination: Double,                            // inclination i (deg, treated constant)
        val periBase: Double, val periRate: Double,         // argument of perihelion w (deg)
        val axis: Double,                                   // semi-major axis a (AU)
        val eccentricity: Double,                           // eccentricity e
        val anomalyBase: Double, val anomalyRate: Double,   // mean anomaly M (deg)
    )

    private val ELEMENTS: Map<Body, Elements> = mapOf(
        Body.MERCURY to Elements(48.3313, 3.24587e-5, 7.0047, 29.1241, 1.01444e-5, 0.387098, 0.205635, 168.6562, 4.0923344368),
        Body.VENUS to Elements(76.6799, 2.46590e-5, 3.3946, 54.8910, 1.38374e-5, 0.723330, 0.006773, 48.0052, 1.6021302244),
        Body.MARS to Elements(49.5574, 2.11081e-5, 1.8497, 286.5016, 2.92961e-5, 1.523688, 0.093405, 18.6021, 0.5240207766),
        Body.JUPITER to Elements(100.4542, 2.76854e-5, 1.3030, 273.8777, 1.64505e-5, 5.20256, 0.048498, 19.8950, 0.0830853001),
        Body.SATURN to Elements(113.6634, 2.38980e-5, 2.4886, 339.3939, 2.97661e-5, 9.55475, 0.055546, 316.9670, 0.0334442282),
    )

    /** Days since the Schlyter epoch (1999-12-31 00:00 UT = JD 2451543.5). */
    private fun schlyterDay(epochMillis: Long): Double = SiderealTime.julianDate(epochMillis) - 2451543.5

    /** Solve Kepler's equation for the eccentric anomaly (radians) by Newton iteration. */
    private fun eccentricAnomaly(meanAnomalyRad: Double, e: Double, iterations: Int = 6): Double {
        var ea = meanAnomalyRad + e * sin(meanAnomalyRad) * (1.0 + e * cos(meanAnomalyRad))
        repeat(iterations) {
            ea -= (ea - e * sin(ea) - meanAnomalyRad) / (1.0 - e * cos(ea))
        }
        return ea
    }

    /** Sun's geocentric ecliptic longitude (rad) and distance (AU). */
    private data class SunPosition(val longitudeRad: Double, val distanceAu: Double)

    private fun sunPosition(d: Double): SunPosition {
        val w = Angles.normalizeDegrees(282.9404 + 4.70935e-5 * d)
        val e = 0.016709 - 1.151e-9 * d
        val m = Angles.normalizeDegrees(356.0470 + 0.9856002585 * d)
        val mRad = m * Angles.DEG_TO_RAD
        val eRad = eccentricAnomaly(mRad, e)
        val xv = cos(eRad) - e
        val yv = sqrt(1.0 - e * e) * sin(eRad)
        val trueAnomalyDeg = Angles.normalizeDegrees(atan2(yv, xv) * Angles.RAD_TO_DEG)
        val r = sqrt(xv * xv + yv * yv)
        return SunPosition((trueAnomalyDeg + w).let { Angles.normalizeDegrees(it) } * Angles.DEG_TO_RAD, r)
    }

    /** Convert rectangular geocentric equatorial coordinates to RA/Dec degrees. */
    private fun equatorialFromXyz(x: Double, y: Double, z: Double): Equatorial {
        val ra = Angles.normalizeDegrees(atan2(y, x) * Angles.RAD_TO_DEG)
        val dec = atan2(z, sqrt(x * x + y * y)) * Angles.RAD_TO_DEG
        return Equatorial(ra, dec)
    }

    /** Geocentric apparent equatorial position of [body] at the instant. */
    fun position(body: Body, epochMillis: Long): Equatorial {
        val d = schlyterDay(epochMillis)
        val ecl = OBLIQUITY_DEG * Angles.DEG_TO_RAD
        return when (body) {
            Body.SUN -> {
                val s = sunPosition(d)
                val xs = s.distanceAu * cos(s.longitudeRad)
                val ys = s.distanceAu * sin(s.longitudeRad)
                equatorialFromXyz(xs, ys * cos(ecl), ys * sin(ecl))
            }
            Body.MOON -> moonPosition(d, ecl)
            else -> planetPosition(body, d, ecl)
        }
    }

    private fun moonPosition(d: Double, ecl: Double): Equatorial {
        val n = Angles.normalizeDegrees(125.1228 - 0.0529538083 * d) * Angles.DEG_TO_RAD
        val i = 5.1454 * Angles.DEG_TO_RAD
        val w = Angles.normalizeDegrees(318.0634 + 0.1643573223 * d) * Angles.DEG_TO_RAD
        val a = 60.2666
        val e = 0.054900
        val m = Angles.normalizeDegrees(115.3654 + 13.0649929509 * d) * Angles.DEG_TO_RAD

        val eRad = eccentricAnomaly(m, e)
        val xv = a * (cos(eRad) - e)
        val yv = a * sqrt(1.0 - e * e) * sin(eRad)
        val v = atan2(yv, xv)
        val r = sqrt(xv * xv + yv * yv)

        val xh = r * (cos(n) * cos(v + w) - sin(n) * sin(v + w) * cos(i))
        val yh = r * (sin(n) * cos(v + w) + cos(n) * sin(v + w) * cos(i))
        val zh = r * (sin(v + w) * sin(i))

        // geocentric ecliptic -> equatorial
        return equatorialFromXyz(
            xh,
            yh * cos(ecl) - zh * sin(ecl),
            yh * sin(ecl) + zh * cos(ecl),
        )
    }

    private fun planetPosition(body: Body, d: Double, ecl: Double): Equatorial {
        val el = ELEMENTS.getValue(body)
        val n = Angles.normalizeDegrees(el.nodeBase + el.nodeRate * d) * Angles.DEG_TO_RAD
        val i = el.inclination * Angles.DEG_TO_RAD
        val w = Angles.normalizeDegrees(el.periBase + el.periRate * d) * Angles.DEG_TO_RAD
        val a = el.axis
        val e = el.eccentricity
        val m = Angles.normalizeDegrees(el.anomalyBase + el.anomalyRate * d) * Angles.DEG_TO_RAD

        val eRad = eccentricAnomaly(m, e)
        val xv = a * (cos(eRad) - e)
        val yv = a * sqrt(1.0 - e * e) * sin(eRad)
        val v = atan2(yv, xv)
        val r = sqrt(xv * xv + yv * yv)

        // heliocentric ecliptic rectangular
        val xh = r * (cos(n) * cos(v + w) - sin(n) * sin(v + w) * cos(i))
        val yh = r * (sin(n) * cos(v + w) + cos(n) * sin(v + w) * cos(i))
        val zh = r * (sin(v + w) * sin(i))

        // add the Sun's geocentric position to get geocentric ecliptic coordinates
        val s = sunPosition(d)
        val xs = s.distanceAu * cos(s.longitudeRad)
        val ys = s.distanceAu * sin(s.longitudeRad)
        val xg = xh + xs
        val yg = yh + ys
        val zg = zh

        // ecliptic -> equatorial
        return equatorialFromXyz(
            xg,
            yg * cos(ecl) - zg * sin(ecl),
            yg * sin(ecl) + zg * cos(ecl),
        )
    }
}
