package com.stargaze.ai.astronomy

import kotlin.math.sin

/**
 * Live satellite position provider.
 *
 * The interface deliberately isolates the position source so a production **SGP4 + live TLE**
 * implementation (from CelesTrak) can be dropped in without changing any caller. The bundled
 * [AnalyticSatelliteProvider] gives smooth, plausible passes for offline operation.
 */
interface SatelliteProvider {
    val satellites: List<Satellite>
    fun positionOf(satellite: Satellite, location: GeoLocation, epochMillis: Long): Horizontal
}

/** The default satellites tracked by the app (ISS, Hubble, Tiangong). */
object SatelliteCatalog {
    val satellites: List<Satellite> = listOf(
        Satellite(
            name = "International Space Station", shortName = "ISS", emoji = "🛰️",
            orbitalPeriodMinutes = 92.7, phase = 0.0, inclinationDeg = 51.6, colorHex = 0xFF8FE3FF,
            description = "A 420-tonne laboratory orbiting ~420 km up at 28,000 km/h. It circles " +
                "Earth every ~93 minutes and is the third-brightest object in the night sky.",
        ),
        Satellite(
            name = "Hubble Space Telescope", shortName = "HST", emoji = "🔭",
            orbitalPeriodMinutes = 95.4, phase = 0.38, inclinationDeg = 28.5, colorHex = 0xFFFFD9A0,
            description = "Humanity's most famous telescope, orbiting ~535 km up since 1990, " +
                "delivering the deepest views of the universe.",
        ),
        Satellite(
            name = "Tiangong Station", shortName = "CSS", emoji = "🛰️",
            orbitalPeriodMinutes = 91.2, phase = 0.66, inclinationDeg = 41.5, colorHex = 0xFFC8FFD0,
            description = "China's crewed space station, continuously inhabited since 2021.",
        ),
    )
}

/**
 * Analytic pass model: synthesises a smooth ground pass where altitude rises and sets across the
 * visible arc of each orbit. Deterministic for a given instant, so the renderer can draw a
 * coherent motion trail. Matches the prototype's `satState`.
 */
class AnalyticSatelliteProvider(
    override val satellites: List<Satellite> = SatelliteCatalog.satellites,
) : SatelliteProvider {

    override fun positionOf(satellite: Satellite, location: GeoLocation, epochMillis: Long): Horizontal {
        val minutes = epochMillis / 60_000.0
        val t = Angles.floorMod(minutes / satellite.orbitalPeriodMinutes + satellite.phase, 1.0)
        val arc = sin(t * Math.PI * 2.0)
        val altitude = arc * 62.0 - 8.0 // peaks ~54 deg, below horizon for ~half the orbit
        val azimuth = Angles.normalizeDegrees(
            satellite.inclinationDeg * sin(t * Math.PI * 2.0) * 0.9 +
                t * 360.0 * 0.6 + satellite.phase * 220.0,
        )
        return Horizontal(altitude, azimuth)
    }
}
