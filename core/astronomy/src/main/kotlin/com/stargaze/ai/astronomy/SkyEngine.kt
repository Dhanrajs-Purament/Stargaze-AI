package com.stargaze.ai.astronomy

/** A sky object resolved to its current horizontal position, ready to render or describe. */
data class PositionedObject(
    val obj: SkyObject,
    val horizontal: Horizontal,
)

/** Curated upcoming sky events for the Tonight tab. */
object SkyEventCatalog {
    val events: List<SkyEvent> = listOf(
        SkyEvent("🚀", "ISS Bright Pass", "Visible arc across your sky tonight", EventVisibility.VISIBLE, "9:42 PM"),
        SkyEvent("🌕", "Full Buck Moon", "Rises at dusk in the southeast", EventVisibility.VISIBLE, "TONIGHT"),
        SkyEvent("☄️", "Perseid Meteor Shower", "Up to 100 meteors/hour at peak", EventVisibility.SOON, "AUG 12"),
        SkyEvent("🪐", "Saturn at Opposition", "Rings at their brightest all year", EventVisibility.SOON, "SEP 8"),
        SkyEvent("🌑", "Total Lunar Eclipse", "Visible across Asia & Europe", EventVisibility.PRO, "SEP 7"),
    )
}

/**
 * High-level facade over the astronomy engine. This is the only type the app layer needs to talk
 * to for computing what is in the sky; it composes the ephemeris, coordinate transforms, catalogs,
 * and satellite provider. Pure and deterministic given (location, instant).
 */
class SkyEngine(
    initialSatelliteProvider: SatelliteProvider = AnalyticSatelliteProvider(),
) {
    /**
     * The active satellite position source. Defaults to the analytic model so the app works
     * immediately and offline; the app layer swaps in a live TLE + SGP4 provider once orbital data
     * is fetched/loaded. Reads are cheap and thread-safe enough for the render loop (reference assign).
     */
    @Volatile
    var satelliteProvider: SatelliteProvider = initialSatelliteProvider

    val stars: List<Star> get() = StarCatalog.stars
    val planets: List<Planet> get() = PlanetCatalog.planets
    val constellations: List<Constellation> get() = ConstellationCatalog.constellations
    val satellites: List<Satellite> get() = satelliteProvider.satellites

    fun horizontalOf(star: Star, location: GeoLocation, epochMillis: Long): Horizontal =
        CoordinateTransforms.equatorialToHorizontal(star.equatorial, location, epochMillis)

    fun horizontalOf(planet: Planet, location: GeoLocation, epochMillis: Long): Horizontal {
        val eq = Ephemeris.position(planet.body, epochMillis)
        return CoordinateTransforms.equatorialToHorizontal(eq, location, epochMillis)
    }

    fun equatorialOf(planet: Planet, epochMillis: Long): Equatorial =
        Ephemeris.position(planet.body, epochMillis)

    fun horizontalOf(satellite: Satellite, location: GeoLocation, epochMillis: Long): Horizontal =
        satelliteProvider.positionOf(satellite, location, epochMillis)

    /** Centroid of a constellation's stars in horizontal coordinates (for labelling/locating). */
    fun centroidOf(constellation: Constellation, location: GeoLocation, epochMillis: Long): Horizontal? {
        val names = constellation.lines.flatMap { listOf(it.first, it.second) }.toSet()
        val positions = names.mapNotNull { StarCatalog.byName[it] }
            .map { horizontalOf(it, location, epochMillis) }
        if (positions.isEmpty()) return null
        val avgAlt = positions.sumOf { it.altitudeDeg } / positions.size
        // average azimuth via vector mean to handle wraparound correctly
        val sinSum = positions.sumOf { kotlin.math.sin(it.azimuthDeg * Angles.DEG_TO_RAD) }
        val cosSum = positions.sumOf { kotlin.math.cos(it.azimuthDeg * Angles.DEG_TO_RAD) }
        val avgAz = Angles.normalizeDegrees(kotlin.math.atan2(sinSum, cosSum) * Angles.RAD_TO_DEG)
        return Horizontal(avgAlt, avgAz)
    }

    /** All planets currently above the horizon, brightest/highest first. */
    fun planetsUp(location: GeoLocation, epochMillis: Long): List<PositionedObject> =
        planets.map { PositionedObject(SkyObject.PlanetObject(it), horizontalOf(it, location, epochMillis)) }
            .filter { it.horizontal.isAboveHorizon }
            .sortedByDescending { it.horizontal.altitudeDeg }

    /** Brightest stars currently above [minAltitudeDeg], brightest first. */
    fun brightStarsUp(location: GeoLocation, epochMillis: Long, minAltitudeDeg: Double = 5.0, limit: Int = 5): List<PositionedObject> =
        stars.map { PositionedObject(SkyObject.StarObject(it), horizontalOf(it, location, epochMillis)) }
            .filter { it.horizontal.altitudeDeg > minAltitudeDeg }
            .sortedBy { (it.obj as SkyObject.StarObject).star.magnitude }
            .take(limit)

    /** Satellites currently overhead for the observer. */
    fun satellitesUp(location: GeoLocation, epochMillis: Long): List<PositionedObject> =
        satellites.map { PositionedObject(SkyObject.SatelliteObject(it), horizontalOf(it, location, epochMillis)) }
            .filter { it.horizontal.isAboveHorizon }

    val events: List<SkyEvent> get() = SkyEventCatalog.events
}
