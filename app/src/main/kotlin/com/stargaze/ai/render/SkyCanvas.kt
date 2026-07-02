package com.stargaze.ai.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.stargaze.ai.astronomy.Angles
import com.stargaze.ai.astronomy.Body
import com.stargaze.ai.astronomy.SkyObject
import com.stargaze.ai.ui.theme.StarColors
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The interactive AR sky map.
 *
 * Draws (in order): background gradient, twinkling faint-star field, constellation lines + name
 * badges, catalogued stars, planets (with Saturn's ring and the Moon's terminator), satellites with
 * motion trails, the compass cardinals, a ground glow, and a selection reticle. Supports drag-to-pan
 * and pinch-to-zoom; taps are hit-tested against rendered objects.
 *
 * @param frameTimeMs a monotonically increasing time used for twinkle/animation, driven by the
 *   host's frame loop so the canvas animates smoothly.
 */
@Composable
fun SkyCanvas(
    snapshot: SkySnapshot,
    viewState: SkyViewState,
    frameTimeMs: Long,
    selected: SkyObject?,
    onPan: (deltaAzDeg: Double, deltaAltDeg: Double) -> Unit,
    onZoom: (fovScale: Float) -> Unit,
    onTapObject: (SkyObject) -> Unit,
    onTapEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    // Snapshot/viewState are read fresh on each tap instead of being written during draw.
    val currentSnapshot by rememberUpdatedState(snapshot)
    val currentViewState by rememberUpdatedState(viewState)
    val currentFrameTimeMs by rememberUpdatedState(frameTimeMs)
    val currentSelected by rememberUpdatedState(selected)
    val tint = if (viewState.nightMode) StarColors.NightRed else null

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) onZoom(zoom)
                    if (pan != Offset.Zero) {
                        // Convert pixel drag to angular pan, scaled by FOV (matches prototype feel).
                        val fov = viewState.fieldOfViewDeg
                        onPan(-pan.x * fov / size.height, pan.y * fov / size.height)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val targets = buildHitTargets(
                        snapshot = currentSnapshot,
                        viewState = currentViewState,
                        frameTimeMs = currentFrameTimeMs,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                    )
                    val hit = targets
                        .filter { hypot((it.x - offset.x).toDouble(), (it.y - offset.y).toDouble()) < it.radius }
                        .minByOrNull { hypot((it.x - offset.x).toDouble(), (it.y - offset.y).toDouble()) }
                    if (hit != null) onTapObject(hit.obj) else onTapEmpty()
                }
            },
    ) {
        val projection = SkyProjection(
            viewWidthPx = size.width,
            viewHeightPx = size.height,
            centerAzimuthDeg = viewState.centerAzimuthDeg,
            centerAltitudeDeg = viewState.centerAltitudeDeg,
            fieldOfViewDeg = viewState.fieldOfViewDeg,
        )
        val targets = mutableListOf<HitTarget>()

        drawBackground(viewState.nightMode)
        drawTwinkleField(projection, frameTimeMs, tint)
        if (viewState.showConstellationLines) drawConstellations(projection, snapshot, tint)
        drawStars(projection, snapshot, frameTimeMs, tint, targets, textMeasurer, viewState.showLabels)
        drawPlanets(projection, snapshot, tint, targets, textMeasurer, viewState.showLabels)
        if (viewState.showSatellites) drawSatellites(projection, snapshot, frameTimeMs, tint, targets, textMeasurer, viewState.showLabels)
        drawCompass(projection)
        drawGroundGlow()
        currentSelected?.let { drawReticle(projection, snapshot, it, frameTimeMs) }

        // local targets list is used only to avoid repeated Allocation during this draw pass.
    }
}

private fun DrawScope.drawBackground(nightMode: Boolean) {
    val colors = if (nightMode) {
        listOf(Color(0xFF240808), Color(0xFF070101))
    } else {
        listOf(Color(0xFF101A40), Color(0xFF0A0E22), Color(0xFF04050D))
    }
    drawRect(brush = androidx.compose.ui.graphics.Brush.verticalGradient(colors), size = size)
}

// Deterministic faint background star field (seeded), twinkling over time.
private val bgField: List<FloatArray> by lazy {
    var s = 12345L
    fun rnd(): Double { s = (s * 1103515245 + 12345) and 0x7fffffff; return s.toDouble() / 0x7fffffff }
    List(420) { floatArrayOf((rnd() * 360).toFloat(), (rnd() * 90).toFloat(), (rnd() * 1.4 + 0.25).toFloat(), (rnd() * 6.28).toFloat(), (rnd() * 2 + 1).toFloat()) }
}

private fun DrawScope.drawTwinkleField(p: SkyProjection, t: Long, tint: Color?) {
    for (b in bgField) {
        val pt = p.project(b[0].toDouble(), b[1].toDouble()) ?: continue
        val tw = 0.55 + 0.45 * sin(t / 700.0 * b[4] + b[3])
        drawCircle(
            color = (tint ?: Color(0xFF9FB0E8)).copy(alpha = (b[2] * tw * 0.6).toFloat().coerceIn(0f, 1f)),
            radius = b[2],
            center = Offset(pt.x, pt.y),
        )
    }
}

private fun DrawScope.drawConstellations(p: SkyProjection, snap: SkySnapshot, tint: Color?) {
    val lineColor = tint ?: StarColors.Accent
    for (c in snap.constellations) {
        for ((aName, bName) in c.lines) {
            val a = snap.stars.firstOrNull { it.star.name == aName } ?: continue
            val b = snap.stars.firstOrNull { it.star.name == bName } ?: continue
            if (a.altitudeDeg < -3 || b.altitudeDeg < -3) continue
            val pa = p.project(a.azimuthDeg, a.altitudeDeg) ?: continue
            val pb = p.project(b.azimuthDeg, b.altitudeDeg) ?: continue
            drawLine(
                color = lineColor.copy(alpha = 0.32f),
                start = Offset(pa.x, pa.y),
                end = Offset(pb.x, pb.y),
                strokeWidth = 1.5f,
            )
        }
    }
}

private fun magToRadius(m: Double): Float = max(0.7, (3.3 - m) * 1.2).toFloat()
private fun starColor(m: Double): Color = when {
    m < 0.4 -> Color(0xFFDFF0FF)
    m < 1.5 -> Color(0xFFEEF4FF)
    else -> Color(0xFFCDD6FF)
}

private fun DrawScope.drawStars(
    p: SkyProjection, snap: SkySnapshot, t: Long, tint: Color?,
    targets: MutableList<HitTarget>, tm: TextMeasurer, showLabels: Boolean,
) {
    for (rs in snap.stars) {
        if (rs.altitudeDeg < -2) continue
        val pt = p.project(rs.azimuthDeg, rs.altitudeDeg) ?: continue
        val tw = 0.8 + 0.2 * sin(t / 600.0 + rs.star.rightAscensionDeg)
        val r = magToRadius(rs.star.magnitude) * tw.toFloat()
        drawCircle(color = tint ?: starColor(rs.star.magnitude), radius = r, center = Offset(pt.x, pt.y))
        targets += HitTarget(pt.x, pt.y, r + 15f, SkyObject.StarObject(rs.star))
        if (showLabels && rs.altitudeDeg > 2 && rs.star.magnitude <= 1.5) {
            drawSkyLabel(tm, rs.star.name, pt.x, pt.y, tint ?: Color(0xFFAEBBE8))
        }
    }
}

private fun DrawScope.drawPlanets(
    p: SkyProjection, snap: SkySnapshot, tint: Color?,
    targets: MutableList<HitTarget>, tm: TextMeasurer, showLabels: Boolean,
) {
    for (rp in snap.planets) {
        if (rp.altitudeDeg < -2) continue
        val pt = p.project(rp.azimuthDeg, rp.altitudeDeg) ?: continue
        val radius = when (rp.planet.body) {
            Body.SUN -> 15f; Body.MOON -> 13f; Body.JUPITER -> 7.5f; Body.VENUS -> 6.5f; else -> 5.5f
        }
        val color = tint ?: Color(rp.planet.colorHex)
        drawCircle(color = color, radius = radius, center = Offset(pt.x, pt.y))
        if (rp.planet.body == Body.SATURN) {
            drawCircle(
                color = (tint ?: Color(0xFFF0DCAA)), radius = radius + 6f,
                center = Offset(pt.x, pt.y), style = Stroke(width = 1.8f),
            )
        }
        targets += HitTarget(pt.x, pt.y, radius + 16f, SkyObject.PlanetObject(rp.planet))
        if (showLabels) drawSkyLabel(tm, rp.planet.displayName, pt.x, pt.y, color)
    }
}

private fun DrawScope.drawSatellites(
    p: SkyProjection, snap: SkySnapshot, t: Long, tint: Color?,
    targets: MutableList<HitTarget>, tm: TextMeasurer, showLabels: Boolean,
) {
    for (rsat in snap.satellites) {
        if (rsat.altitudeDeg < 0) continue
        val pt = p.project(rsat.azimuthDeg, rsat.altitudeDeg) ?: continue
        val blink = (0.6 + 0.4 * sin(t / 180.0)).toFloat().coerceIn(0f, 1f)
        val color = tint ?: Color(rsat.satellite.colorHex)
        drawCircle(color = color.copy(alpha = blink), radius = 3.4f, center = Offset(pt.x, pt.y))
        targets += HitTarget(pt.x, pt.y, 14f, SkyObject.SatelliteObject(rsat.satellite))
        if (showLabels) drawSkyLabel(tm, "${rsat.satellite.emoji} ${rsat.satellite.shortName}", pt.x, pt.y, color)
    }
}

private fun DrawScope.drawSkyLabel(tm: TextMeasurer, text: String, x: Float, y: Float, color: Color) {
    val measured = tm.measure(text)
    drawText(measured, color = color, topLeft = Offset(x + 10f, y - 8f - measured.size.height))
}

private fun DrawScope.drawCompass(p: SkyProjection) {
    val cardinals = listOf(0.0 to "N", 45.0 to "NE", 90.0 to "E", 135.0 to "SE", 180.0 to "S", 225.0 to "SW", 270.0 to "W", 315.0 to "NW")
    for ((az, label) in cardinals) {
        val pt = p.project(az, 0.4) ?: continue
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.argb(115, 255, 255, 255)
                textSize = if (label.length > 1) 28f else 34f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            drawText(label, pt.x, pt.y, paint)
        }
    }
}

private fun DrawScope.drawGroundGlow() {
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0x00040513), Color(0xE6080A18)),
            startY = size.height * 0.72f,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height * 0.72f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.28f),
    )
}

private fun DrawScope.drawReticle(p: SkyProjection, snap: SkySnapshot, selected: SkyObject, t: Long) {
    val pos = when (selected) {
        is SkyObject.StarObject -> snap.stars.firstOrNull { it.star.name == selected.star.name }
            ?.let { p.project(it.azimuthDeg, it.altitudeDeg) }
        is SkyObject.PlanetObject -> snap.planets.firstOrNull { it.planet.body == selected.planet.body }
            ?.let { p.project(it.azimuthDeg, it.altitudeDeg) }
        is SkyObject.SatelliteObject -> snap.satellites.firstOrNull { it.satellite.shortName == selected.satellite.shortName }
            ?.let { p.project(it.azimuthDeg, it.altitudeDeg) }
        is SkyObject.ConstellationObject -> null
    } ?: return
    val pulse = 1.0 + 0.12 * sin(t / 300.0)
    val r = (20.0 * pulse).toFloat()
    drawCircle(color = Color(0xFF9BB6FF), radius = r, center = Offset(pos.x, pos.y), style = Stroke(width = 1.6f))
    // ticks
    for (a in listOf(0.0, 90.0, 180.0, 270.0)) {
        val rad = a * Angles.DEG_TO_RAD
        drawLine(
            color = Color(0xFF9BB6FF),
            start = Offset((pos.x + cos(rad) * (r - 5)).toFloat(), (pos.y + sin(rad) * (r - 5)).toFloat()),
            end = Offset((pos.x + cos(rad) * (r + 6)).toFloat(), (pos.y + sin(rad) * (r + 6)).toFloat()),
            strokeWidth = 1.6f,
        )
    }
}

/** Builds tap hit targets deterministically from the current snapshot, outside of draw. */
private fun buildHitTargets(
    snapshot: SkySnapshot,
    viewState: SkyViewState,
    frameTimeMs: Long,
    width: Float,
    height: Float,
): List<HitTarget> {
    val projection = SkyProjection(
        viewWidthPx = width,
        viewHeightPx = height,
        centerAzimuthDeg = viewState.centerAzimuthDeg,
        centerAltitudeDeg = viewState.centerAltitudeDeg,
        fieldOfViewDeg = viewState.fieldOfViewDeg,
    )
    val targets = mutableListOf<HitTarget>()

    for (rs in snapshot.stars) {
        if (rs.altitudeDeg < -2) continue
        val pt = projection.project(rs.azimuthDeg, rs.altitudeDeg) ?: continue
        val tw = 0.8 + 0.2 * sin(frameTimeMs / 600.0 + rs.star.rightAscensionDeg)
        val r = magToRadius(rs.star.magnitude) * tw.toFloat()
        targets += HitTarget(pt.x, pt.y, r + 15f, SkyObject.StarObject(rs.star))
    }
    for (rp in snapshot.planets) {
        if (rp.altitudeDeg < -2) continue
        val pt = projection.project(rp.azimuthDeg, rp.altitudeDeg) ?: continue
        val radius = when (rp.planet.body) {
            Body.SUN -> 15f; Body.MOON -> 13f; Body.JUPITER -> 7.5f; Body.VENUS -> 6.5f; else -> 5.5f
        }
        targets += HitTarget(pt.x, pt.y, radius + 16f, SkyObject.PlanetObject(rp.planet))
    }
    if (viewState.showSatellites) {
        for (rsat in snapshot.satellites) {
            if (rsat.altitudeDeg < 0) continue
            val pt = projection.project(rsat.azimuthDeg, rsat.altitudeDeg) ?: continue
            targets += HitTarget(pt.x, pt.y, 14f, SkyObject.SatelliteObject(rsat.satellite))
        }
    }
    return targets
}
