package com.stargaze.ai.astronomy

/**
 * Time and sidereal-time computations.
 *
 * The engine works from a Unix epoch milliseconds value ([epochMillis]) so it is fully decoupled
 * from any platform clock or time zone. The UI supplies "now plus an optional offset" (for the
 * time-travel slider) and the engine treats it as an instant in UTC.
 */
object SiderealTime {

    /** Julian Date corresponding to the Unix epoch (1970-01-01T00:00:00Z). */
    private const val JD_UNIX_EPOCH = 2440587.5

    /** Milliseconds per day. */
    private const val MILLIS_PER_DAY = 86_400_000.0

    /** Julian Date for J2000.0 (2000-01-01T12:00:00 TT). */
    const val JD_J2000 = 2451545.0

    /** Convert Unix epoch milliseconds to Julian Date. */
    fun julianDate(epochMillis: Long): Double = epochMillis / MILLIS_PER_DAY + JD_UNIX_EPOCH

    /** Days elapsed since J2000.0 for the given instant. */
    fun daysSinceJ2000(epochMillis: Long): Double = julianDate(epochMillis) - JD_J2000

    /**
     * Greenwich Mean Sidereal Time in degrees, normalised to [0, 360).
     *
     * Uses the standard linear approximation in days since J2000, matching the validated prototype:
     * GMST = 280.46061837 + 360.98564736629 * d   (degrees)
     */
    fun greenwichMeanSiderealTimeDeg(epochMillis: Long): Double {
        val d = daysSinceJ2000(epochMillis)
        return Angles.normalizeDegrees(280.46061837 + 360.98564736629 * d)
    }

    /**
     * Local Sidereal Time in degrees for an observer longitude (east positive), normalised to [0, 360).
     */
    fun localSiderealTimeDeg(epochMillis: Long, longitudeDeg: Double): Double {
        return Angles.normalizeDegrees(greenwichMeanSiderealTimeDeg(epochMillis) + longitudeDeg)
    }
}
