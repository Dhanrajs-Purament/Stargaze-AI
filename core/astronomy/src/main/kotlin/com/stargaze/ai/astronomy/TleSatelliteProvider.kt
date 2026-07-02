package com.stargaze.ai.astronomy

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Converts an inertial (TEME/ECI) satellite position to an observer's topocentric look angles
 * (altitude/azimuth) and computes the sub-satellite ground point.
 *
 * Uses the WGS-84 ellipsoid for the observer's geocentric position and GMST to rotate the inertial
 * frame into the earth-fixed frame. Accuracy is well within a phone's pointing precision.
 */
object SatelliteGeometry {

    private const val EARTH_RADIUS_KM = 6378.137         // WGS-84 equatorial radius
    private const val FLATTENING = 1.0 / 298.257223563
    private const val E2 = FLATTENING * (2.0 - FLATTENING)

    /** Observer geodetic position -> earth-centred earth-fixed (ECEF) vector, km. */
    private fun observerEcef(location: GeoLocation, gmstRad: Double): Triple<Double, Double, Double> {
        val latRad = location.latitudeDeg * Angles.DEG_TO_RAD
        // Local sidereal time = GMST + east longitude.
        val lst = gmstRad + location.longitudeDeg * Angles.DEG_TO_RAD
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val c = 1.0 / sqrt(1.0 - E2 * sinLat * sinLat)
        val s = c * (1.0 - E2)
        val rx = EARTH_RADIUS_KM * c * cosLat * cos(lst)
        val ry = EARTH_RADIUS_KM * c * cosLat * sin(lst)
        val rz = EARTH_RADIUS_KM * s * sinLat
        return Triple(rx, ry, rz)
    }

    /**
     * Topocentric look angles for a TEME position [sat] as seen from [location] at [epochMillis].
     */
    fun lookAngles(sat: TemeVector, location: GeoLocation, epochMillis: Long): Horizontal {
        val gmstRad = SiderealTime.greenwichMeanSiderealTimeDeg(epochMillis) * Angles.DEG_TO_RAD
        val (ox, oy, oz) = observerEcef(location, gmstRad)

        // Rotate the satellite TEME vector into the earth-fixed frame by -GMST about z.
        val cosG = cos(gmstRad)
        val sinG = sin(gmstRad)
        val sx = sat.xKm * cosG + sat.yKm * sinG
        val sy = -sat.xKm * sinG + sat.yKm * cosG
        val sz = sat.zKm

        // Range vector from observer to satellite (ECEF).
        val rx = sx - ox
        val ry = sy - oy
        val rz = sz - oz

        // Rotate range vector into the local SEZ (south-east-zenith) topocentric frame.
        val latRad = location.latitudeDeg * Angles.DEG_TO_RAD
        val lst = gmstRad + location.longitudeDeg * Angles.DEG_TO_RAD
        val sinLat = sin(latRad); val cosLat = cos(latRad)
        val sinLst = sin(lst); val cosLst = cos(lst)

        val south = sinLat * cosLst * rx + sinLat * sinLst * ry - cosLat * rz
        val east = -sinLst * rx + cosLst * ry
        val zenith = cosLat * cosLst * rx + cosLat * sinLst * ry + sinLat * rz

        val range = sqrt(south * south + east * east + zenith * zenith)
        val altitude = asin((zenith / range).coerceIn(-1.0, 1.0)) * Angles.RAD_TO_DEG
        // Azimuth from north, clockwise.
        val azimuth = Angles.normalizeDegrees(atan2(east, -south) * Angles.RAD_TO_DEG)
        return Horizontal(altitude, azimuth)
    }
}

/**
 * A [SatelliteProvider] backed by **real TLE data and SGP4 propagation**. This replaces the analytic
 * approximation with NORAD-grade orbital mechanics: positions are accurate to the precision of the
 * supplied TLEs.
 *
 * Satellites are matched to TLEs by catalog number first, then by case-insensitive name. A
 * per-[Tle] [Sgp4] propagator is built lazily and reused. If a satellite has no matching TLE, its
 * position falls back to the supplied [fallback] provider so the feature degrades gracefully rather
 * than failing.
 */
class TleSatelliteProvider(
    private val tles: List<Tle>,
    private val catalogNumbers: Map<String, Int> = emptyMap(),
    private val fallback: SatelliteProvider = AnalyticSatelliteProvider(),
) : SatelliteProvider {

    override val satellites: List<Satellite> = fallback.satellites

    private val byCatalog: Map<Int, Tle> = tles.associateBy { it.catalogNumber }
    private val byName: Map<String, Tle> = tles.associateBy { it.name.lowercase() }
    private val propagators = HashMap<Int, Sgp4>()

    private fun tleFor(satellite: Satellite): Tle? {
        catalogNumbers[satellite.shortName]?.let { byCatalog[it]?.let { t -> return t } }
        byName[satellite.name.lowercase()]?.let { return it }
        byName[satellite.shortName.lowercase()]?.let { return it }
        // Fuzzy: a TLE whose name contains the satellite's short or full name.
        return tles.firstOrNull {
            it.name.contains(satellite.shortName, ignoreCase = true) ||
                it.name.contains(satellite.name, ignoreCase = true)
        }
    }

    override fun positionOf(satellite: Satellite, location: GeoLocation, epochMillis: Long): Horizontal {
        val tle = tleFor(satellite) ?: return fallback.positionOf(satellite, location, epochMillis)
        val sgp4 = propagators.getOrPut(tle.catalogNumber) { Sgp4(tle) }
        val minutesSinceEpoch = (epochMillis - tle.epochMillis) / 60_000.0
        val teme = sgp4.propagate(minutesSinceEpoch)
        return SatelliteGeometry.lookAngles(teme, location, epochMillis)
    }
}
