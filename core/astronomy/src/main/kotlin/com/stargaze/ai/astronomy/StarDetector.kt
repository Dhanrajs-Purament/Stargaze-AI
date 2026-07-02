package com.stargaze.ai.astronomy

import kotlin.math.sqrt

/** A star-like bright spot detected in a camera frame, in image pixel coordinates. */
data class DetectedStar(val x: Double, val y: Double, val brightness: Double)

/**
 * Detects star-like point sources in a grayscale (luminance) image.
 *
 * Pipeline (classic astrometry star extraction, adapted for a phone frame):
 *  1. Estimate the background level and noise (mean + standard deviation of the luminance grid).
 *  2. Threshold at background + k·sigma to isolate candidate bright pixels.
 *  3. Find local maxima and compute a brightness-weighted centroid over a small window (sub-pixel).
 *  4. Suppress detections too close together, sort by brightness, and cap the count.
 *
 * Pure Kotlin (operates on an `IntArray` of 0..255 luminance), so it is unit-testable without
 * Android and reusable. The app feeds it a downscaled luminance plane from CameraX.
 */
class StarDetector(
    private val sigmaThreshold: Double = 5.0,
    private val minSeparationPx: Int = 6,
    private val maxStars: Int = 60,
    private val window: Int = 2,
) {
    fun detect(luminance: IntArray, width: Int, height: Int): List<DetectedStar> {
        require(luminance.size >= width * height) { "luminance grid smaller than width*height" }
        if (width < 3 || height < 3) return emptyList()

        // 1. Background statistics.
        var sum = 0.0
        for (i in 0 until width * height) sum += luminance[i]
        val mean = sum / (width * height)
        var varSum = 0.0
        for (i in 0 until width * height) {
            val d = luminance[i] - mean
            varSum += d * d
        }
        val sigma = sqrt(varSum / (width * height))
        // No meaningful signal (flat/!uniform image) — nothing to detect.
        if (sigma < 1e-6) return emptyList()
        val threshold = mean + sigmaThreshold * sigma

        // 2 & 3. Local maxima above threshold -> centroids.
        val candidates = ArrayList<DetectedStar>()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val v = luminance[y * width + x]
                if (v <= threshold) continue
                if (!isLocalMaximum(luminance, width, height, x, y)) continue
                candidates.add(centroid(luminance, width, height, x, y, mean))
            }
        }

        // 4. Non-maximum suppression by separation, brightest first.
        candidates.sortByDescending { it.brightness }
        val kept = ArrayList<DetectedStar>()
        val minSep2 = (minSeparationPx * minSeparationPx).toDouble()
        for (c in candidates) {
            if (kept.none { (it.x - c.x) * (it.x - c.x) + (it.y - c.y) * (it.y - c.y) < minSep2 }) {
                kept.add(c)
                if (kept.size >= maxStars) break
            }
        }
        return kept
    }

    private fun isLocalMaximum(lum: IntArray, w: Int, h: Int, x: Int, y: Int): Boolean {
        val v = lum[y * w + x]
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until w && ny in 0 until h && lum[ny * w + nx] > v) return false
        }
        return true
    }

    private fun centroid(lum: IntArray, w: Int, h: Int, cx: Int, cy: Int, background: Double): DetectedStar {
        var wsum = 0.0; var xsum = 0.0; var ysum = 0.0; var peak = 0.0
        for (dy in -window..window) for (dx in -window..window) {
            val nx = cx + dx; val ny = cy + dy
            if (nx in 0 until w && ny in 0 until h) {
                val weight = (lum[ny * w + nx] - background).coerceAtLeast(0.0)
                wsum += weight; xsum += weight * nx; ysum += weight * ny
                if (lum[ny * w + nx] > peak) peak = lum[ny * w + nx].toDouble()
            }
        }
        return if (wsum > 0) DetectedStar(xsum / wsum, ysum / wsum, wsum) else DetectedStar(cx.toDouble(), cy.toDouble(), peak)
    }
}
