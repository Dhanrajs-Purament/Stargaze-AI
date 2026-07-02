package com.stargaze.ai.astronomy

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A position in the TEME (True Equator Mean Equinox) inertial frame, kilometres. */
data class TemeVector(val xKm: Double, val yKm: Double, val zKm: Double) {
    val magnitudeKm: Double get() = sqrt(xKm * xKm + yKm * yKm + zKm * zKm)
}

/**
 * SGP4 near-earth simplified perturbations propagator (Hoots & Roehrich, Spacetrack Report #3),
 * using WGS-72 constants — the model TLEs are generated for. Given a TLE, [propagate] returns the
 * satellite's position in the TEME frame at a time offset from the element-set epoch.
 *
 * This is a real implementation of the standard algorithm (secular + periodic perturbations from
 * J2/J3/J4 and atmospheric drag via the B* term), not an approximation. It targets the near-earth
 * regime (orbital period < 225 minutes), which covers the ISS, Hubble, Tiangong, Starlink, etc. Deep
 * space resonance terms (SDP4) are intentionally omitted as out of scope for those targets.
 */
class Sgp4(private val tle: Tle) {

    private companion object {
        const val KE = 0.0743669161               // sqrt(GM) in earth-radii^1.5 / min (WGS-72)
        const val J2 = 1.082616e-3
        const val J3 = -2.53881e-6
        const val J4 = -1.65597e-6
        const val EARTH_RADIUS_KM = 6378.135
        const val MIN_PER_DAY = 1440.0
        const val TWO_PI = 2.0 * Math.PI
        const val DEG2RAD = Math.PI / 180.0
        const val CK2 = 0.5 * J2
        val CK4 = -0.375 * J4
        const val QOMS2T = 1.880279159015270e-9    // (q0 - s)^4 in earth radii^4
        const val S = 1.012229                      // s constant in earth radii
    }

    // --- Precomputed initialization constants (done once per TLE) ---
    private val inclo = tle.inclinationDeg * DEG2RAD
    private val nodeo = tle.raanDeg * DEG2RAD
    private val argpo = tle.argPerigeeDeg * DEG2RAD
    private val mo = tle.meanAnomalyDeg * DEG2RAD
    private val ecco = tle.eccentricity
    private val bstar = tle.bstar

    // Original mean motion (rad/min) recovered from the TLE Kozai mean motion.
    private val noKozai = tle.meanMotionRevPerDay * TWO_PI / MIN_PER_DAY

    private val cosio = cos(inclo)
    private val sinio = sin(inclo)
    private val theta2 = cosio * cosio
    private val x3thm1 = 3.0 * theta2 - 1.0
    private val eosq = ecco * ecco
    private val betao2 = 1.0 - eosq
    private val betao = sqrt(betao2)

    // Recover original (Brouwer) mean motion and semi-major axis.
    private val a1 = (KE / noKozai).pow(2.0 / 3.0)
    private val del1 = 1.5 * CK2 * x3thm1 / (a1 * a1 * betao * betao2)
    private val ao = a1 * (1.0 - del1 * (1.0 / 3.0 + del1 * (1.0 + 134.0 / 81.0 * del1)))
    private val delo = 1.5 * CK2 * x3thm1 / (ao * ao * betao * betao2)
    private val noUnkozai = noKozai / (1.0 + delo)         // mean motion (rad/min)
    private val aodp = ao / (1.0 - delo)                   // semi-major axis (earth radii)

    private val perigee = (aodp * (1.0 - ecco) - 1.0) * EARTH_RADIUS_KM

    // s and qoms2t adjusted for low perigee (atmospheric model).
    private val sfour: Double
    private val qoms24: Double

    private val tsi: Double
    private val eta: Double
    private val etasq: Double
    private val eeta: Double
    private val coef: Double
    private val coef1: Double
    private val c1: Double
    private val c2: Double
    private val c3: Double
    private val c4: Double
    private val c5: Double

    private val x1mth2 = 1.0 - theta2
    private val xmdot: Double
    private val omgdot: Double
    private val xnodot: Double
    private val xnodcf: Double
    private val t2cof: Double
    private val xlcof: Double
    private val aycof: Double
    private val x7thm1 = 7.0 * theta2 - 1.0
    private val omgcof: Double
    private val xmcof: Double
    private val delmo: Double
    private val sinmo: Double

    private val isimpleFlag: Boolean
    private val d2: Double
    private val d3: Double
    private val d4: Double
    private val t3cof: Double
    private val t4cof: Double
    private val t5cof: Double

    init {
        // Adjust s for low perigee.
        var sTmp = S
        var qoms24Tmp = QOMS2T
        if (perigee < 156.0) {
            var sNew = perigee - 78.0
            if (perigee < 98.0) sNew = 20.0
            qoms24Tmp = ((120.0 - sNew) / EARTH_RADIUS_KM).pow(4.0)
            sTmp = sNew / EARTH_RADIUS_KM + 1.0
        }
        sfour = sTmp
        qoms24 = qoms24Tmp

        val pinvsq = 1.0 / (aodp * aodp * betao2 * betao2)
        tsi = 1.0 / (aodp - sfour)
        eta = aodp * ecco * tsi
        etasq = eta * eta
        eeta = ecco * eta
        val psisq = kotlin.math.abs(1.0 - etasq)
        coef = qoms24 * tsi.pow(4.0)
        coef1 = coef / psisq.pow(3.5)

        c2 = coef1 * noUnkozai * (aodp * (1.0 + 1.5 * etasq + eeta * (4.0 + etasq)) +
            0.375 * CK2 * tsi / psisq * x3thm1 * (8.0 + 3.0 * etasq * (8.0 + etasq)))
        c1 = bstar * c2

        c3 = if (ecco > 1.0e-4) coef * tsi * J3 / CK2 * noUnkozai * sinio / ecco else 0.0
        c4 = 2.0 * noUnkozai * coef1 * aodp * betao2 * (
            eta * (2.0 + 0.5 * etasq) + ecco * (0.5 + 2.0 * etasq) -
                CK2 * tsi / (aodp * psisq) * (
                    -3.0 * x3thm1 * (1.0 - 2.0 * eeta + etasq * (1.5 - 0.5 * eeta)) +
                        0.75 * x1mth2 * (2.0 * etasq - eeta * (1.0 + etasq)) * cos(2.0 * argpo)
                    )
            )
        c5 = 2.0 * coef1 * aodp * betao2 * (1.0 + 2.75 * (etasq + eeta) + eeta * etasq)

        val theta4 = theta2 * theta2
        val temp1 = 3.0 * CK2 * pinvsq * noUnkozai
        val temp2 = temp1 * CK2 * pinvsq
        val temp3 = 1.25 * CK4 * pinvsq * pinvsq * noUnkozai

        xmdot = noUnkozai + 0.5 * temp1 * betao * x3thm1 +
            0.0625 * temp2 * betao * (13.0 - 78.0 * theta2 + 137.0 * theta4)
        omgdot = -0.5 * temp1 * (1.0 - 5.0 * theta2) +
            0.0625 * temp2 * (7.0 - 114.0 * theta2 + 395.0 * theta4) +
            temp3 * (3.0 - 36.0 * theta2 + 49.0 * theta4)
        val xhdot1 = -temp1 * cosio
        xnodot = xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * theta2) + 2.0 * temp3 * (3.0 - 7.0 * theta2)) * cosio

        omgcof = bstar * c3 * cos(argpo)
        xmcof = if (ecco > 1.0e-4) -(2.0 / 3.0) * coef * bstar / eeta else 0.0
        xnodcf = 3.5 * betao2 * xhdot1 * c1
        t2cof = 1.5 * c1

        xlcof = 0.125 * J3 / CK2 * sinio * (3.0 + 5.0 * cosio) / (1.0 + cosio)
        aycof = 0.25 * J3 / CK2 * sinio

        delmo = (1.0 + eta * cos(mo)).pow(3.0)
        sinmo = sin(mo)

        // Determine whether to use the simplified drag model (perigee height < 220 km).
        isimpleFlag = (aodp * (1.0 - ecco) / 1.0) < (220.0 / EARTH_RADIUS_KM + 1.0)
        if (!isimpleFlag) {
            val c1sq = c1 * c1
            d2 = 4.0 * aodp * tsi * c1sq
            val temp = d2 * tsi * c1 / 3.0
            d3 = (17.0 * aodp + sfour) * temp
            d4 = 0.5 * temp * aodp * tsi * (221.0 * aodp + 31.0 * sfour) * c1
            t3cof = d2 + 2.0 * c1sq
            t4cof = 0.25 * (3.0 * d3 + c1 * (12.0 * d2 + 10.0 * c1sq))
            t5cof = 0.2 * (3.0 * d4 + 12.0 * c1 * d3 + 6.0 * d2 * d2 + 15.0 * c1sq * (2.0 * d2 + c1sq))
        } else {
            d2 = 0.0; d3 = 0.0; d4 = 0.0; t3cof = 0.0; t4cof = 0.0; t5cof = 0.0
        }
    }

    /**
     * Propagates the orbit to [minutesSinceEpoch] minutes from the TLE epoch and returns the TEME
     * position in kilometres.
     */
    fun propagate(minutesSinceEpoch: Double): TemeVector {
        val tsince = minutesSinceEpoch

        // Secular gravity and drag.
        val xmdf = mo + xmdot * tsince
        val omgadf = argpo + omgdot * tsince
        val xnoddf = nodeo + xnodot * tsince
        var omega = omgadf
        var xmp = xmdf
        val tsq = tsince * tsince
        val xnode = xnoddf + xnodcf * tsq
        var tempa = 1.0 - c1 * tsince
        var tempe = bstar * c4 * tsince
        var templ = t2cof * tsq

        if (!isimpleFlag) {
            val delomg = omgcof * tsince
            val delm = xmcof * ((1.0 + eta * cos(xmdf)).pow(3.0) - delmo)
            val temp = delomg + delm
            xmp = xmdf + temp
            omega = omgadf - temp
            val tcube = tsq * tsince
            val tfour = tsince * tcube
            tempa -= d2 * tsq + d3 * tcube + d4 * tfour
            tempe += bstar * c5 * (sin(xmp) - sinmo)
            templ += t3cof * tcube + tfour * (t4cof + tsince * t5cof)
        }

        val a = aodp * tempa * tempa
        val e = (ecco - tempe).coerceIn(1.0e-6, 0.999999)
        val xl = xmp + omega + xnode + noUnkozai * templ

        val beta = sqrt(1.0 - e * e)

        // Long-period periodics.
        val axn = e * cos(omega)
        val temp = 1.0 / (a * beta * beta)
        val xll = temp * xlcof * axn
        val aynl = temp * aycof
        val xlt = xl + xll
        val ayn = e * sin(omega) + aynl

        // Solve Kepler's equation for (E + omega).
        val capu = ((xlt - xnode) % TWO_PI)
        var epw = capu
        var sinepw = 0.0
        var cosepw = 0.0
        for (i in 0 until 10) {
            sinepw = sin(epw)
            cosepw = cos(epw)
            val ecosE = axn * cosepw + ayn * sinepw
            val esinE = axn * sinepw - ayn * cosepw
            val f = capu - epw + esinE
            val df = 1.0 - ecosE
            val delta = (f / df).coerceIn(-0.95, 0.95)
            epw += delta
            if (kotlin.math.abs(delta) < 1.0e-12) break
        }

        // Short-period preliminary quantities.
        val ecosE = axn * cosepw + ayn * sinepw
        val esinE = axn * sinepw - ayn * cosepw
        val elsq = axn * axn + ayn * ayn
        val tempA = 1.0 - elsq
        val pl = a * tempA
        val r = a * (1.0 - ecosE)
        val temp1 = 1.0 / r
        val temp2 = a * temp1
        val betal = sqrt(tempA)
        val temp3 = 1.0 / (1.0 + betal)
        val cosu = temp2 * (cosepw - axn + ayn * esinE * temp3)
        val sinu = temp2 * (sinepw - ayn - axn * esinE * temp3)
        val u = atan2(sinu, cosu)
        val sin2u = 2.0 * sinu * cosu
        val cos2u = 2.0 * cosu * cosu - 1.0
        val temp4 = 1.0 / pl
        val temp5 = CK2 * temp4
        val temp6 = temp5 * temp4

        // Update for short periodics.
        val rk = r * (1.0 - 1.5 * temp6 * betal * x3thm1) + 0.5 * temp5 * x1mth2 * cos2u
        val uk = u - 0.25 * temp6 * x7thm1 * sin2u
        val xnodek = xnode + 1.5 * temp6 * cosio * sin2u
        val xinck = inclo + 1.5 * temp6 * cosio * sinio * cos2u

        // Orientation vectors.
        val sinuk = sin(uk)
        val cosuk = cos(uk)
        val sinik = sin(xinck)
        val cosik = cos(xinck)
        val sinnok = sin(xnodek)
        val cosnok = cos(xnodek)

        val xmx = -sinnok * cosik
        val xmy = cosnok * cosik
        val ux = xmx * sinuk + cosnok * cosuk
        val uy = xmy * sinuk + sinnok * cosuk
        val uz = sinik * sinuk

        // Position in earth radii -> km.
        return TemeVector(
            xKm = rk * ux * EARTH_RADIUS_KM,
            yKm = rk * uy * EARTH_RADIUS_KM,
            zKm = rk * uz * EARTH_RADIUS_KM,
        )
    }
}
