package com.stargaze.ai.astronomy

import kotlin.math.hypot

/** The result of solving a camera frame against the star catalog. */
data class PlateSolution(
    val solved: Boolean,
    val correctedAzimuthDeg: Double,
    val correctedAltitudeDeg: Double,
    val azimuthOffsetDeg: Double,    // applied correction to the prior (drift)
    val altitudeOffsetDeg: Double,
    val matchedStarNames: List<String>,
    val constellations: List<String>,
    val confidence: Double,           // 0..1, matched / expected
)

/**
 * Orientation-prior plate solver.
 *
 * Given detected star positions in a frame and a sensor-derived pointing **prior** (where the phone
 * thinks it's aimed), it predicts where catalogued stars should fall, greedily matches the brightest
 * detections to the nearest predicted catalog stars, and estimates the residual az/alt offset. That
 * offset is the **compass/gyro drift correction** — the fix for the "AR is off by several degrees"
 * problem. It also reports which constellations are confidently in view.
 *
 * This is deterministic and pure (no Android), so it is fully unit-testable. It deliberately uses a
 * prior (we always have a decent sensor estimate) rather than blind whole-sky solving, which is far
 * cheaper and robust enough for a real-time phone experience.
 */
class PlateSolver(
    private val maxMatchDistanceDeg: Double = 6.0,
    private val minMatches: Int = 4,
) {
    fun solve(
        detections: List<DetectedStar>,
        location: GeoLocation,
        epochMillis: Long,
        priorAzimuthDeg: Double,
        priorAltitudeDeg: Double,
        fieldOfViewDeg: Double,
        rollDeg: Double,
        widthPx: Int,
        heightPx: Int,
    ): PlateSolution {
        val projection = GnomonicProjection(
            widthPx, heightPx, priorAzimuthDeg, priorAltitudeDeg, fieldOfViewDeg, rollDeg,
        )
        // Capture radius in pixels, derived from a degree tolerance so it scales with the FOV and
        // comfortably exceeds realistic compass/gyro drift (a few degrees).
        val degPerPxLocal = fieldOfViewDeg / heightPx
        val maxMatchDistancePx = maxMatchDistanceDeg / degPerPxLocal

        // Predict catalog star positions in the frame for the prior pointing.
        data class Predicted(val star: Star, val p: ImagePoint)
        val predicted = StarCatalog.stars.mapNotNull { star ->
            val h = CoordinateTransforms.equatorialToHorizontal(star.equatorial, location, epochMillis)
            if (h.altitudeDeg < 0) return@mapNotNull null
            projection.project(h.azimuthDeg, h.altitudeDeg)
                ?.takeIf { it.x in 0.0..widthPx.toDouble() && it.y in 0.0..heightPx.toDouble() }
                ?.let { Predicted(star, it) }
        }
        if (predicted.isEmpty() || detections.size < minMatches) {
            return unsolved(priorAzimuthDeg, priorAltitudeDeg)
        }

        // Greedy nearest-neighbour match, brightest detections first.
        val usedCatalog = HashSet<String>()
        val matches = ArrayList<Pair<DetectedStar, Predicted>>()
        for (det in detections.sortedByDescending { it.brightness }) {
            var best: Predicted? = null
            var bestD = maxMatchDistancePx
            for (pred in predicted) {
                if (pred.star.name in usedCatalog) continue
                val d = hypot(det.x - pred.p.x, det.y - pred.p.y)
                if (d < bestD) { bestD = d; best = pred }
            }
            if (best != null) { matches.add(det to best); usedCatalog.add(best.star.name) }
        }
        if (matches.size < minMatches) return unsolved(priorAzimuthDeg, priorAltitudeDeg)

        // Estimate the pointing correction in ANGULAR space (robust median).
        // For each matched detection: the pixel, unprojected under the prior, gives the (az,alt) the
        // phone *thinks* it points at; the catalog star's computed (az,alt) is the truth. The signed
        // difference is the prior's error. The median over matches is the correction to apply.
        val azErrors = ArrayList<Double>()
        val altErrors = ArrayList<Double>()
        for ((det, pred) in matches) {
            val thoughtPoint = projection.unproject(det.x, det.y)
            val truth = CoordinateTransforms.equatorialToHorizontal(pred.star.equatorial, location, epochMillis)
            azErrors.add(Angles.normalizeSigned(thoughtPoint.azimuthDeg - truth.azimuthDeg))
            altErrors.add(thoughtPoint.altitudeDeg - truth.altitudeDeg)
        }
        azErrors.sort(); altErrors.sort()
        // Correction = -(prior error). The prior is off by +error, so subtract it.
        val azOffset = -azErrors[azErrors.size / 2]
        val altOffset = -altErrors[altErrors.size / 2]

        val matchedNames = matches.map { it.second.star.name }
        val constellations = matchedNames
            .mapNotNull { StarCatalog.byName[it]?.constellation }
            .groupingBy { it }.eachCount()
            .filter { it.value >= 2 }      // a constellation is "in view" if >=2 of its stars matched
            .keys.sorted()
        val confidence = (matches.size.toDouble() / predicted.size.coerceAtLeast(1)).coerceIn(0.0, 1.0)

        return PlateSolution(
            solved = true,
            correctedAzimuthDeg = Angles.normalizeDegrees(priorAzimuthDeg + azOffset),
            correctedAltitudeDeg = (priorAltitudeDeg + altOffset).coerceIn(-90.0, 90.0),
            azimuthOffsetDeg = azOffset,
            altitudeOffsetDeg = altOffset,
            matchedStarNames = matchedNames,
            constellations = constellations,
            confidence = confidence,
        )
    }

    private fun unsolved(az: Double, alt: Double) = PlateSolution(
        solved = false,
        correctedAzimuthDeg = az,
        correctedAltitudeDeg = alt,
        azimuthOffsetDeg = 0.0,
        altitudeOffsetDeg = 0.0,
        matchedStarNames = emptyList(),
        constellations = emptyList(),
        confidence = 0.0,
    )
}
