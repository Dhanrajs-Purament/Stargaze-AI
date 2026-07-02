package com.stargaze.ai.render

import com.stargaze.ai.astronomy.Constellation
import com.stargaze.ai.astronomy.GeoLocation
import com.stargaze.ai.astronomy.Planet
import com.stargaze.ai.astronomy.Satellite
import com.stargaze.ai.astronomy.SkyObject
import com.stargaze.ai.astronomy.Star

/** Immutable description of the camera/view state for the sky renderer. */
data class SkyViewState(
    val centerAzimuthDeg: Double = 180.0,
    val centerAltitudeDeg: Double = 35.0,
    val fieldOfViewDeg: Double = 78.0,
    val nightMode: Boolean = false,
    val showConstellationLines: Boolean = true,
    val showLabels: Boolean = true,
    val showSatellites: Boolean = true,
    val arMode: Boolean = false,
)

/** A precomputed renderable: a sky object resolved to its current alt/az. */
data class RenderStar(val star: Star, val azimuthDeg: Double, val altitudeDeg: Double)
data class RenderPlanet(val planet: Planet, val azimuthDeg: Double, val altitudeDeg: Double)
data class RenderSatellite(val satellite: Satellite, val azimuthDeg: Double, val altitudeDeg: Double)

/** A bundle of all renderable positions for one frame's celestial snapshot. */
data class SkySnapshot(
    val stars: List<RenderStar> = emptyList(),
    val planets: List<RenderPlanet> = emptyList(),
    val satellites: List<RenderSatellite> = emptyList(),
    val constellations: List<Constellation> = emptyList(),
    val location: GeoLocation = GeoLocation.MUMBAI,
)

/** A target the user can tap, with the sky object it maps to. */
data class HitTarget(val x: Float, val y: Float, val radius: Float, val obj: SkyObject)
