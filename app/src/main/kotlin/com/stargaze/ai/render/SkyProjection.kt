package com.stargaze.ai.render

import com.stargaze.ai.astronomy.Angles
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** A 2D screen point in pixels, or null when the target is behind the view (not projectable). */
data class ScreenPoint(val x: Float, val y: Float)

/**
 * Gnomonic-style projection of horizontal (az, alt) coordinates onto the screen, given the current
 * view center and field of view. This is a faithful port of the validated prototype's `project()`
 * so on-device rendering matches the approved web experience.
 *
 * Kept as a pure class so the projection can be unit-tested without Android.
 */
class SkyProjection(
    var viewWidthPx: Float = 0f,
    var viewHeightPx: Float = 0f,
    var centerAzimuthDeg: Double = 180.0,
    var centerAltitudeDeg: Double = 35.0,
    var fieldOfViewDeg: Double = 78.0,
) {
    /** Project a sky position to screen pixels, or null if it is outside the visible hemisphere. */
    fun project(azimuthDeg: Double, altitudeDeg: Double): ScreenPoint? {
        val dAz = Angles.normalizeSigned(azimuthDeg - centerAzimuthDeg) * Angles.DEG_TO_RAD
        val a = altitudeDeg * Angles.DEG_TO_RAD
        val c = centerAltitudeDeg * Angles.DEG_TO_RAD
        val sinAlt = sin(a)
        val cosAlt = cos(a)
        val cosC = sin(c) * sinAlt + cos(c) * cosAlt * cos(dAz)
        if (cosC <= 0.04) return null // behind the viewer / too near the edge
        val x = cosAlt * sin(dAz) / cosC
        val y = (cos(c) * sinAlt - sin(c) * cosAlt * cos(dAz)) / cosC
        val scale = viewHeightPx / (2.0 * tan(fieldOfViewDeg / 2.0 * Angles.DEG_TO_RAD))
        return ScreenPoint(
            x = (viewWidthPx / 2.0 + x * scale).toFloat(),
            y = (viewHeightPx / 2.0 - y * scale).toFloat(),
        )
    }
}
