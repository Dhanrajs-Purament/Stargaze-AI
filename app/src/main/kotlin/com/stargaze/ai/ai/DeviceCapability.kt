package com.stargaze.ai.ai

import android.app.ActivityManager
import android.content.Context

/**
 * Reports device memory, used to advise which on-device model a phone can comfortably run.
 *
 * On-device LLM inference is memory-bound, so total RAM is the practical gate. The model catalog
 * declares a `minRamMb` per model; the picker compares it against [totalRamMb] to show a
 * suitability hint (and warn before downloading a model heavier than the device can handle).
 */
class DeviceCapability(private val context: Context) {

    /** Total physical RAM in megabytes. */
    fun totalRamMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024 * 1024)
    }
}
