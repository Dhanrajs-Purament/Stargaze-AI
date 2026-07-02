package com.stargaze.ai.astronomy

import kotlin.math.floor

/**
 * Shared angle utilities and the observer location model.
 *
 * All angles in this engine are in **degrees** at the public API boundary; radians are used only
 * inside individual computations. Keeping a single convention avoids the most common class of
 * astronomy bugs (mixed units).
 */
object Angles {
    const val DEG_TO_RAD: Double = Math.PI / 180.0
    const val RAD_TO_DEG: Double = 180.0 / Math.PI

    /** Normalise an angle in degrees to the half-open range `[0, 360)`. */
    fun normalizeDegrees(value: Double): Double {
        val r = value % 360.0
        return if (r < 0) r + 360.0 else r
    }

    /** Normalise an angle in degrees to `[-180, 180)`, useful for shortest-path differences. */
    fun normalizeSigned(value: Double): Double {
        return ((value + 540.0) % 360.0) - 180.0
    }

    /** Floor-based modulo that always returns a non-negative result. */
    fun floorMod(a: Double, m: Double): Double = a - m * floor(a / m)
}

/**
 * Observer position on Earth.
 *
 * @param latitudeDeg geographic latitude in degrees, clamped to [-90, 90].
 * @param longitudeDeg geographic longitude in degrees (east positive), normalised to [-180, 180).
 * @param name human-readable label for UI.
 */
data class GeoLocation(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val name: String = "",
) {
    init {
        require(latitudeDeg in -90.0..90.0) { "latitude out of range: $latitudeDeg" }
        require(longitudeDeg in -180.0..180.0) { "longitude out of range: $longitudeDeg" }
    }

    companion object {
        /** Default location used before the user grants location permission (Mumbai). */
        val MUMBAI = GeoLocation(19.0760, 72.8777, "Mumbai")

        /**
         * Safe factory that clamps/normalises raw device coordinates instead of throwing,
         * since GPS/sensor input is untrusted at the system boundary.
         */
        fun ofClamped(latitudeDeg: Double, longitudeDeg: Double, name: String = ""): GeoLocation {
            val lat = latitudeDeg.coerceIn(-90.0, 90.0)
            var lon = Angles.normalizeDegrees(longitudeDeg)
            if (lon >= 180.0) lon -= 360.0
            return GeoLocation(lat, lon, name)
        }
    }
}

/** Horizontal (alt-az) coordinate. Altitude in degrees [-90, 90], azimuth in degrees [0, 360). */
data class Horizontal(val altitudeDeg: Double, val azimuthDeg: Double) {
    val isAboveHorizon: Boolean get() = altitudeDeg > 0.0
}

/** Equatorial coordinate. Right ascension in degrees [0, 360), declination in degrees [-90, 90]. */
data class Equatorial(val rightAscensionDeg: Double, val declinationDeg: Double)
