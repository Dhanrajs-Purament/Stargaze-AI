package com.stargaze.ai.astronomy

import kotlin.math.roundToInt

/**
 * Builds a factual, computed description of what is actually in a region of sky, for **grounding**
 * an AI explanation. This is the key to using a multimodal/LLM model safely for sky analysis: the
 * model is told the ground truth (computed from ephemerides, the catalog, and live TLEs) and asked
 * to *explain* it, rather than being asked to identify a dark night-sky photo (which it would
 * hallucinate).
 *
 * Pure and deterministic, so it is unit-testable and reusable by both the on-device and cloud tiers.
 */
class SceneContextBuilder(private val engine: SkyEngine = SkyEngine()) {

    /**
     * Describes the sky around a pointing direction within [radiusDeg], plus what's notable overall.
     * If [identifiedConstellations] is supplied (from the plate solver), it is treated as confirmed.
     */
    fun describe(
        location: GeoLocation,
        epochMillis: Long,
        centerAzimuthDeg: Double,
        centerAltitudeDeg: Double,
        radiusDeg: Double = 30.0,
        identifiedConstellations: List<String> = emptyList(),
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Observer: ${location.name.ifBlank { "lat ${location.latitudeDeg.roundToInt()}, lon ${location.longitudeDeg.roundToInt()}" }}.")
        sb.appendLine("Looking toward azimuth ${centerAzimuthDeg.roundToInt()}° (${compass(centerAzimuthDeg)}), altitude ${centerAltitudeDeg.roundToInt()}°.")

        if (identifiedConstellations.isNotEmpty()) {
            sb.appendLine("Confirmed in view (plate-solved): ${identifiedConstellations.joinToString(", ")}.")
        }

        // Bright stars within the field.
        val starsInView = engine.stars.mapNotNull { star ->
            val h = engine.horizontalOf(star, location, epochMillis)
            if (h.isAboveHorizon && angularSeparation(centerAzimuthDeg, centerAltitudeDeg, h.azimuthDeg, h.altitudeDeg) <= radiusDeg) {
                star to h
            } else null
        }.sortedBy { it.first.magnitude }.take(6)
        if (starsInView.isNotEmpty()) {
            sb.appendLine("Bright stars in view: " + starsInView.joinToString(", ") {
                "${it.first.name} (${it.first.constellation}, mag ${"%.1f".format(it.first.magnitude)})"
            } + ".")
        }

        // Planets in view.
        val planetsInView = engine.planets.mapNotNull { p ->
            val h = engine.horizontalOf(p, location, epochMillis)
            if (h.isAboveHorizon && angularSeparation(centerAzimuthDeg, centerAltitudeDeg, h.azimuthDeg, h.altitudeDeg) <= radiusDeg) p else null
        }
        if (planetsInView.isNotEmpty()) {
            sb.appendLine("Planets/bodies in view: " + planetsInView.joinToString(", ") { it.displayName } + ".")
        }

        // Live satellites overhead (real SGP4 positions).
        val satsUp = engine.satellitesUp(location, epochMillis)
        if (satsUp.isNotEmpty()) {
            sb.appendLine("Satellites currently above the horizon: " + satsUp.joinToString(", ") {
                val s = (it.obj as SkyObject.SatelliteObject).satellite
                "${s.shortName} at ${it.horizontal.altitudeDeg.roundToInt()}° ${compass(it.horizontal.azimuthDeg)}"
            } + ".")
        }

        return sb.toString().trim()
    }

    private fun angularSeparation(az1: Double, alt1: Double, az2: Double, alt2: Double): Double {
        val a1 = alt1 * Angles.DEG_TO_RAD; val a2 = alt2 * Angles.DEG_TO_RAD
        val dAz = (az1 - az2) * Angles.DEG_TO_RAD
        val cosSep = kotlin.math.sin(a1) * kotlin.math.sin(a2) +
            kotlin.math.cos(a1) * kotlin.math.cos(a2) * kotlin.math.cos(dAz)
        return kotlin.math.acos(cosSep.coerceIn(-1.0, 1.0)) * Angles.RAD_TO_DEG
    }

    private fun compass(az: Double): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((az / 45.0).roundToInt()) % 8]
    }
}
