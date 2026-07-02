package com.stargaze.ai.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.stargaze.ai.astronomy.Angles
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Device pointing orientation: where on the celestial sphere the back of the phone points. */
data class DeviceOrientation(
    val azimuthDeg: Double, // compass direction the phone points, 0=N clockwise
    val altitudeDeg: Double, // elevation above horizon the phone points
    val rollDeg: Double,
)

/**
 * Streams smoothed device orientation from the rotation-vector sensor.
 *
 * Design choices that directly address SkyView's "janky AR" complaint:
 *  - Uses [Sensor.TYPE_ROTATION_VECTOR] (fused accelerometer+gyro+magnetometer) for low latency and
 *    drift correction, falling back to [Sensor.TYPE_GAME_ROTATION_VECTOR] if needed.
 *  - Applies angle-aware exponential smoothing (low-pass) on the output angles so the sky doesn't
 *    jitter, while remaining responsive.
 *  - Remaps the coordinate axes so that "pointing the phone at the sky" maps intuitively to looking
 *    up (altitude), accounting for the natural held-up posture.
 */
class OrientationProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    /** True when the device can provide orientation; UI falls back to drag-mode otherwise. */
    val isAvailable: Boolean get() = rotationSensor != null

    fun orientation(): Flow<DeviceOrientation> = callbackFlow {
        val sensor = rotationSensor
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val remapped = FloatArray(9)
        val orientationAngles = FloatArray(3)

        // Smoothing state held as unit vectors per angle to average across the 0/360 wrap correctly.
        var smoothAzSin = Double.NaN
        var smoothAzCos = 0.0
        var smoothAlt = Double.NaN
        var smoothRoll = Double.NaN
        val alpha = 0.18 // smoothing factor: lower = smoother, higher = more responsive

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // Remap so the device's natural "pointing at sky" axis maps to looking outward.
                val (axisX, axisY) = when (displayRotation()) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Z to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Z to SensorManager.AXIS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Z
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Z
                }
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)
                SensorManager.getOrientation(remapped, orientationAngles)

                val rawAz = Angles.normalizeDegrees(Math.toDegrees(orientationAngles[0].toDouble()))
                // pitch: when phone points up at sky, altitude increases toward +90
                val rawAlt = (-Math.toDegrees(orientationAngles[1].toDouble())).coerceIn(-90.0, 90.0)
                val rawRoll = Math.toDegrees(orientationAngles[2].toDouble())

                // Exponential smoothing; azimuth via vector form to handle wraparound.
                val azRad = rawAz * Angles.DEG_TO_RAD
                if (smoothAzSin.isNaN()) {
                    smoothAzSin = sin(azRad); smoothAzCos = cos(azRad)
                    smoothAlt = rawAlt; smoothRoll = rawRoll
                } else {
                    smoothAzSin += alpha * (sin(azRad) - smoothAzSin)
                    smoothAzCos += alpha * (cos(azRad) - smoothAzCos)
                    smoothAlt += alpha * (rawAlt - smoothAlt)
                    smoothRoll += alpha * (rawRoll - smoothRoll)
                }
                val az = Angles.normalizeDegrees(atan2(smoothAzSin, smoothAzCos) * Angles.RAD_TO_DEG)

                trySend(DeviceOrientation(az, smoothAlt, smoothRoll))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    @Suppress("DEPRECATION")
    private fun displayRotation(): Int = windowManager.defaultDisplay.rotation
}
