package com.stargaze.ai.astronomy

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Conversions between celestial coordinate systems.
 *
 * The core transform takes an equatorial position (RA/Dec, J2000) and the observer's location and
 * instant, and returns the horizontal (alt/az) position as seen from that point on Earth. This is
 * the single function every renderer and "is it up right now?" query depends on.
 */
object CoordinateTransforms {

    /**
     * Convert equatorial (RA/Dec, degrees) to horizontal (alt/az, degrees) for the given observer
     * and instant.
     *
     * Azimuth is measured clockwise from **North** (N=0°, E=90°, S=180°, W=270°), matching the
     * compass convention used throughout the UI.
     */
    fun equatorialToHorizontal(
        equatorial: Equatorial,
        location: GeoLocation,
        epochMillis: Long,
    ): Horizontal {
        val lstDeg = SiderealTime.localSiderealTimeDeg(epochMillis, location.longitudeDeg)
        val hourAngleRad = Angles.normalizeDegrees(lstDeg - equatorial.rightAscensionDeg) * Angles.DEG_TO_RAD
        val decRad = equatorial.declinationDeg * Angles.DEG_TO_RAD
        val latRad = location.latitudeDeg * Angles.DEG_TO_RAD

        val sinAlt = sin(decRad) * sin(latRad) +
            cos(decRad) * cos(latRad) * cos(hourAngleRad)
        val altRad = asin(sinAlt.coerceIn(-1.0, 1.0))

        // Azimuth from north, clockwise. The +180 rotation aligns atan2's south-referenced
        // result with the north-referenced compass convention used by the renderer and HUD.
        val azRad = atan2(
            sin(hourAngleRad),
            cos(hourAngleRad) * sin(latRad) - tan(decRad) * cos(latRad),
        )
        val azimuthDeg = Angles.normalizeDegrees(azRad * Angles.RAD_TO_DEG + 180.0)

        return Horizontal(altitudeDeg = altRad * Angles.RAD_TO_DEG, azimuthDeg = azimuthDeg)
    }
}
