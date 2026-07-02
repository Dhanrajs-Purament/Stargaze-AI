package com.stargaze.ai.astronomy

/** Solar-system bodies the ephemeris can compute. */
enum class Body(val displayName: String, val symbol: String) {
    SUN("Sun", "☀️"),
    MOON("Moon", "🌕"),
    MERCURY("Mercury", "☿"),
    VENUS("Venus", "♀"),
    MARS("Mars", "♂"),
    JUPITER("Jupiter", "♃"),
    SATURN("Saturn", "♄"),
}

/** A planet/Sun/Moon catalog entry with presentation metadata and a fun description. */
data class Planet(
    val body: Body,
    val colorHex: Long,
    val description: String,
) {
    val displayName: String get() = body.displayName
    val symbol: String get() = body.symbol
}

/**
 * A catalogued star.
 *
 * @param magnitude apparent visual magnitude (lower = brighter; negative for the brightest stars).
 */
data class Star(
    val name: String,
    val rightAscensionDeg: Double,
    val declinationDeg: Double,
    val magnitude: Double,
    val constellation: String,
    val blurb: String,
) {
    val equatorial: Equatorial get() = Equatorial(rightAscensionDeg, declinationDeg)
}

/** A constellation figure: ordered pairs of star names that form its connecting lines. */
data class Constellation(
    val name: String,
    val emoji: String,
    val tag: String,
    val lines: List<Pair<String, String>>,
)

/** An artificial satellite tracked by the engine. */
data class Satellite(
    val name: String,
    val shortName: String,
    val emoji: String,
    val orbitalPeriodMinutes: Double,
    val phase: Double,
    val inclinationDeg: Double,
    val colorHex: Long,
    val description: String,
)

/** A curated upcoming sky event for the Tonight tab. */
data class SkyEvent(
    val emoji: String,
    val title: String,
    val description: String,
    val visibility: EventVisibility,
    val whenLabel: String,
)

enum class EventVisibility { VISIBLE, SOON, PRO }

/** Discriminated type for anything selectable/searchable in the sky. */
sealed interface SkyObject {
    val displayName: String

    data class StarObject(val star: Star) : SkyObject {
        override val displayName: String get() = star.name
    }

    data class PlanetObject(val planet: Planet) : SkyObject {
        override val displayName: String get() = planet.displayName
    }

    data class SatelliteObject(val satellite: Satellite) : SkyObject {
        override val displayName: String get() = satellite.name
    }

    data class ConstellationObject(val constellation: Constellation) : SkyObject {
        override val displayName: String get() = constellation.name
    }
}
