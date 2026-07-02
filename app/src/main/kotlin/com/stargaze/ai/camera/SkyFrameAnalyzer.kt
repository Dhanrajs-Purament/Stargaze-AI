package com.stargaze.ai.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.stargaze.ai.astronomy.DetectedStar
import com.stargaze.ai.astronomy.StarDetector

/** A captured frame's detected stars plus the grid size they were detected in. */
data class FrameDetection(
    val stars: List<DetectedStar>,
    val gridWidth: Int,
    val gridHeight: Int,
)

/**
 * CameraX [ImageAnalysis.Analyzer] that turns each frame into detected star positions.
 *
 * It reads the Y (luminance) plane of the YUV_420_888 image — which is exactly what star detection
 * needs — downscales it to a manageable grid for speed, runs [StarDetector], and reports detections
 * scaled back to the analysis grid. Frames are processed on CameraX's analyzer executor; the
 * heavy-but-bounded detection runs there and results are delivered via [onDetections].
 *
 * Analysis is gated by [enabled] so the camera can stream a preview without burning CPU until the
 * user taps "Identify sky".
 */
class SkyFrameAnalyzer(
    private val onDetections: (FrameDetection) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile var enabled: Boolean = false

    private val detector = StarDetector()

    override fun analyze(image: ImageProxy) {
        if (!enabled) { image.close(); return }
        try {
            val plane = image.planes[0]
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val srcW = image.width
            val srcH = image.height
            val buffer = plane.buffer

            // Downscale to a grid no larger than TARGET on its long edge for fast detection.
            val step = maxOf(1, maxOf(srcW, srcH) / TARGET_LONG_EDGE)
            val gridW = srcW / step
            val gridH = srcH / step
            if (gridW < 8 || gridH < 8) { image.close(); return }

            val lum = IntArray(gridW * gridH)
            for (gy in 0 until gridH) {
                val sy = gy * step
                val rowBase = sy * rowStride
                for (gx in 0 until gridW) {
                    val sx = gx * step
                    val idx = rowBase + sx * pixelStride
                    lum[gy * gridW + gx] = buffer.get(idx).toInt() and 0xFF
                }
            }

            val stars = detector.detect(lum, gridW, gridH)
            onDetections(FrameDetection(stars, gridW, gridH))
        } catch (e: Exception) {
            // A bad frame must never crash the camera pipeline.
        } finally {
            image.close()
        }
    }

    private companion object {
        const val TARGET_LONG_EDGE = 640
    }
}
