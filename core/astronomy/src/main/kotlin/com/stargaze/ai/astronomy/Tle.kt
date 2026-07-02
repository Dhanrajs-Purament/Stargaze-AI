package com.stargaze.ai.astronomy

/**
 * A parsed Two-Line Element set (TLE) — the standard format for distributing satellite orbital data
 * (e.g. from CelesTrak/NORAD). Angles are stored in degrees, mean motion in revolutions/day.
 *
 * Only the fields the near-earth SGP4 propagator needs are retained. Parsing is strict: malformed
 * lines or bad checksums are rejected, since TLEs are untrusted external input.
 */
data class Tle(
    val name: String,
    val catalogNumber: Int,
    val epochYear: Int,            // full year, e.g. 2026
    val epochDay: Double,          // day of year with fractional part
    val meanMotionDot: Double,     // first derivative of mean motion, rev/day^2 (already /2 in TLE)
    val bstar: Double,             // drag term, 1/earth radii
    val inclinationDeg: Double,
    val raanDeg: Double,           // right ascension of ascending node
    val eccentricity: Double,
    val argPerigeeDeg: Double,
    val meanAnomalyDeg: Double,
    val meanMotionRevPerDay: Double,
) {
    /** Unix epoch milliseconds corresponding to the TLE epoch (UTC). */
    val epochMillis: Long by lazy { tleEpochToMillis(epochYear, epochDay) }

    companion object {
        /**
         * Parses a TLE from its (optional) name line and two data lines. Returns null if the lines
         * are malformed or fail checksum validation.
         */
        fun parse(name: String, line1: String, line2: String): Tle? {
            if (line1.length < 69 || line2.length < 69) return null
            if (line1[0] != '1' || line2[0] != '2') return null
            if (!checksumValid(line1) || !checksumValid(line2)) return null

            return try {
                val catalog = line1.substring(2, 7).trim().toInt()
                val epochYY = line1.substring(18, 20).trim().toInt()
                val epochYear = if (epochYY < 57) 2000 + epochYY else 1900 + epochYY
                val epochDay = line1.substring(20, 32).trim().toDouble()
                val nDot = line1.substring(33, 43).trim().toDouble()
                val bstar = parseExp(line1.substring(53, 61))

                val incl = line2.substring(8, 16).trim().toDouble()
                val raan = line2.substring(17, 25).trim().toDouble()
                val eccStr = line2.substring(26, 33).replace(' ', '0')
                val ecc = ("0." + eccStr).toDouble()
                val argp = line2.substring(34, 42).trim().toDouble()
                val ma = line2.substring(43, 51).trim().toDouble()
                val mm = line2.substring(52, 63).trim().toDouble()

                Tle(
                    name = name.trim().ifBlank { "CAT-$catalog" },
                    catalogNumber = catalog,
                    epochYear = epochYear,
                    epochDay = epochDay,
                    meanMotionDot = nDot,
                    bstar = bstar,
                    inclinationDeg = incl,
                    raanDeg = raan,
                    eccentricity = ecc,
                    argPerigeeDeg = argp,
                    meanAnomalyDeg = ma,
                    meanMotionRevPerDay = mm,
                )
            } catch (e: NumberFormatException) {
                null
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        }

        /** TLE mod-10 checksum: digits in columns 0–67 summed (minus signs count as 1), column 68 is the check digit. */
        private fun checksumValid(line: String): Boolean {
            val body = line.substring(0, 69)
            var sum = 0
            for (c in body.take(68)) {
                when {
                    c.isDigit() -> sum += c - '0'
                    c == '-' -> sum += 1
                }
            }
            val expected = body[68]
            if (!expected.isDigit()) return false
            return sum % 10 == expected - '0'
        }

        /** Parses TLE exponential notation like " 12345-3" => 0.12345e-3. */
        private fun parseExp(field: String): Double {
            val f = field.trim()
            if (f.isEmpty() || f == "00000-0" || f == "00000+0" || f == "0") return 0.0
            val sign = if (f.startsWith("-")) -1.0 else 1.0
            val core = f.trimStart('+', '-')
            // mantissa is everything except a trailing sign+digit exponent
            val expIdx = core.indexOfLast { it == '+' || it == '-' }
            return if (expIdx > 0) {
                val mantissa = ("0." + core.substring(0, expIdx)).toDouble()
                val exp = core.substring(expIdx).toInt()
                sign * mantissa * Math.pow(10.0, exp.toDouble())
            } else {
                sign * ("0.$core").toDouble()
            }
        }
    }
}

/** Converts a TLE epoch (full year + fractional day-of-year) to Unix epoch milliseconds (UTC). */
internal fun tleEpochToMillis(year: Int, dayOfYear: Double): Long {
    // Days since Unix epoch for Jan 1 of `year` (UTC), then add (dayOfYear - 1).
    var days = 0L
    if (year >= 1970) {
        for (y in 1970 until year) days += if (isLeap(y)) 366 else 365
    } else {
        for (y in year until 1970) days -= if (isLeap(y)) 366 else 365
    }
    val millisAtYearStart = days * 86_400_000L
    val millisIntoYear = ((dayOfYear - 1.0) * 86_400_000.0).toLong()
    return millisAtYearStart + millisIntoYear
}

private fun isLeap(y: Int): Boolean = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)

/**
 * Parses a CelesTrak-style multi-stanza TLE document (repeating: name line, line 1, line 2) into a
 * list of valid [Tle]s. Invalid stanzas are skipped rather than failing the whole batch, since the
 * source is untrusted external data.
 */
fun parseTleDocument(text: String): List<Tle> {
    val lines = text.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
    val result = ArrayList<Tle>()
    var i = 0
    while (i + 2 < lines.size + 1) {
        // A stanza is: name, "1 ...", "2 ...". Some feeds omit the name line.
        if (i + 1 < lines.size && lines[i].startsWith("1 ") && lines[i + 1].startsWith("2 ")) {
            Tle.parse("", lines[i], lines[i + 1])?.let { result.add(it) }
            i += 2
        } else if (i + 2 < lines.size && lines[i + 1].startsWith("1 ") && lines[i + 2].startsWith("2 ")) {
            Tle.parse(lines[i], lines[i + 1], lines[i + 2])?.let { result.add(it) }
            i += 3
        } else {
            i += 1
        }
    }
    return result
}
