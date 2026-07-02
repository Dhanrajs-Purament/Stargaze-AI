package com.stargaze.ai.astronomy

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** A point in normalized image space (pixels), top-left origin, y down. */
data class ImagePoint(val x: Double, val y: Double)

/**
 * Gnomonic (tangent-plane) projection from horizontal sky coordinates (az/alt) to image pixels,
 * given a view centre, field of view, and optional camera roll. This is the core-side twin of the
 * app's renderer projection, used by the plate solver to predict where catalog stars should appear
 * in a camera frame for a given pointing prior.
 */
class GnomonicProjection(
    val widthPx: Int,
    val heightPx: Int,
    val centerAzimuthDeg: Double,
    val centerAltitudeDeg: Double,
    val fieldOfViewDeg: Double,
    val rollDeg: Double = 0.0,
) {
    private val scale = heightPx / (2.0 * tan(fieldOfViewDeg / 2.0 * Angles.DEG_TO_RAD))
    private val cosRoll = cos(rollDeg * Angles.DEG_TO_RAD)
    private val sinRoll = sin(rollDeg * Angles.DEG_TO_RAD)

    /** Projects (az, alt) to image pixels, or null if behind the view plane. */
    fun project(azimuthDeg: Double, altitudeDeg: Double): ImagePoint? {
        val dAz = Angles.normalizeSigned(azimuthDeg - centerAzimuthDeg) * Angles.DEG_TO_RAD
        val a = altitudeDeg * Angles.DEG_TO_RAD
        val c = centerAltitudeDeg * Angles.DEG_TO_RAD
        val sinA = sin(a); val cosA = cos(a)
        val cosC = sin(c) * sinA + cos(c) * cosA * cos(dAz)
        if (cosC <= 0.04) return null
        val xt = cosA * sin(dAz) / cosC
        val yt = (cos(c) * sinA - sin(c) * cosA * cos(dAz)) / cosC
        // Apply camera roll about the view axis.
        val xr = xt * cosRoll - yt * sinRoll
        val yr = xt * sinRoll + yt * cosRoll
        return ImagePoint(widthPx / 2.0 + xr * scale, heightPx / 2.0 - yr * scale)
    }

    /**
     * Inverse of [project]: maps an image pixel back to the horizontal (az, alt) it points at.
     * Used by the plate solver to compute the pointing correction directly in angular space.
     */
    fun unproject(px: Double, py: Double): Horizontal {
        // Screen pixel -> tangent-plane coords (undo roll).
        val xr = (px - widthPx / 2.0) / scale
        val yr = (heightPx / 2.0 - py) / scale
        val xt = xr * cosRoll + yr * sinRoll
        val yt = -xr * sinRoll + yr * cosRoll

        val c = centerAltitudeDeg * Angles.DEG_TO_RAD
        val rho = kotlin.math.hypot(xt, yt)
        if (rho < 1e-12) {
            return Horizontal(centerAltitudeDeg, Angles.normalizeDegrees(centerAzimuthDeg))
        }
        val cc = kotlin.math.atan(rho)
        val sinCc = sin(cc); val cosCc = cos(cc)
        val alt = kotlin.math.asin(cosCc * sin(c) + yt * sinCc * cos(c) / rho)
        val az = centerAzimuthDeg * Angles.DEG_TO_RAD +
            kotlin.math.atan2(xt * sinCc, rho * cos(c) * cosCc - yt * sin(c) * sinCc)
        return Horizontal(alt * Angles.RAD_TO_DEG, Angles.normalizeDegrees(az * Angles.RAD_TO_DEG))
    }
}
