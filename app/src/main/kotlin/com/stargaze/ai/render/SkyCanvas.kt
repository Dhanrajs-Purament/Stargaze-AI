package com.stargaze.ai.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.stargaze.ai.astronomy.Angles
import com.stargaze.ai.astronomy.Body
import com.stargaze.ai.astronomy.SkyObject
import com.stargaze.ai.ui.theme.StarColors
import com.stargaze.ai.ui.theme.StarGazeMotion
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * The interactive AR sky map.
 *
 * Draws (in order): background gradient, Milky Way band, twinkling faint-star field, constellation
 * lines + name badges, catalogued stars, planets (with Saturn's ring and the Moon's terminator),
 * satellites with motion trails, shooting stars (meteors), the compass cardinals, a ground glow,
 * and a selection reticle. Supports drag-to-pan and pinch-to-zoom; taps are hit-tested against
 * rendered objects.
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

    // Shooting-star manager: spawns meteors at random intervals, each with a curved trajectory
    // and a fading particle trail. Lifetime is frame-driven for smooth animation.
    val meteorManager = remember { MeteorManager() }

    // Constellation line-draw animation: progress 0→1 when lines are toggled on or a different
    // constellation is selected. Resets when lines are toggled off.
    // Keying on the selected constellation NAME (not the full selected object) prevents the
    // animation from replaying every time the user taps a star or planet.
    val constellationAnimProgress = remember { Animatable(0f) }
    val linesVisible = viewState.showConstellationLines
    val selectedConstellationName = (selected as? SkyObject.ConstellationObject)?.constellation?.name
    LaunchedEffect(linesVisible, selectedConstellationName) {
        if (linesVisible) {
            constellationAnimProgress.snapTo(0f)
            constellationAnimProgress.animateTo(
                1f,
                animationSpec = tween(
                    StarGazeMotion.DURATION_CONSTELLATION_DRAW,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            constellationAnimProgress.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) onZoom(zoom)
                    if (pan != Offset.Zero) {
                        // Convert pixel drag to angular pan, scaled by current FOV.
                        // Uses currentViewState (rememberUpdatedState) so zoom changes take effect
                        // immediately rather than using a stale initial FOV.
                        val fov = currentViewState.fieldOfViewDeg
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

        // Update meteor manager inside the draw phase where canvas dimensions are available,
        // avoiding both a composition-side-effect and hardcoded spawn dimensions.
        meteorManager.update(frameTimeMs, viewState.nightMode, size.width, size.height)

        drawBackground(viewState.nightMode)
        drawMilkyWay(projection, frameTimeMs, tint)
        drawTwinkleField(projection, frameTimeMs, tint)
        if (viewState.showConstellationLines) {
            drawConstellations(projection, snapshot, tint, constellationAnimProgress.value, selected)
        }
        drawStars(projection, snapshot, frameTimeMs, tint, textMeasurer, viewState.showLabels)
        drawPlanets(projection, snapshot, tint, textMeasurer, viewState.showLabels)
        if (viewState.showSatellites) drawSatellites(projection, snapshot, frameTimeMs, tint, textMeasurer, viewState.showLabels)
        drawShootingStars(meteorManager, frameTimeMs, tint)
        drawCompass(projection)
        drawGroundGlow()
        currentSelected?.let { drawReticle(projection, snapshot, it, frameTimeMs) }
    }
}

private fun DrawScope.drawBackground(nightMode: Boolean) {
    val colors = if (nightMode) {
        listOf(Color(0xFF240808), Color(0xFF070101))
    } else {
        listOf(Color(0xFF101A40), Color(0xFF0A0E22), Color(0xFF04050D))
    }
    drawRect(brush = Brush.verticalGradient(colors), size = size)
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

private fun DrawScope.drawConstellations(
    p: SkyProjection,
    snap: SkySnapshot,
    tint: Color?,
    animProgress: Float,
    selected: SkyObject?,
) {
    val lineColor = tint ?: StarColors.Accent
    // Build an O(1) lookup map once per frame instead of O(n) firstOrNull for every line endpoint.
    val starMap: Map<String, RenderStar> = snap.stars.associateBy { it.star.name }
    for (c in snap.constellations) {
        val isSelected = selected is SkyObject.ConstellationObject && selected.constellation.name == c.name
        val lineCount = c.lines.size
        val staggerPerLine = 1f / max(lineCount, 1)
        for ((idx, pair) in c.lines.withIndex()) {
            val (aName, bName) = pair
            val a = starMap[aName] ?: continue
            val b = starMap[bName] ?: continue
            if (a.altitudeDeg < -3 || b.altitudeDeg < -3) continue
            val pa = p.project(a.azimuthDeg, a.altitudeDeg) ?: continue
            val pb = p.project(b.azimuthDeg, b.altitudeDeg) ?: continue

            // Each line draws progressively within its stagger window:
            // line idx starts at idx*stagger and completes at (idx+1)*stagger of the total animation.
            val lineStartTime = idx * staggerPerLine
            val lineEndTime = (idx + 1) * staggerPerLine
            val segmentProgress = if (lineEndTime <= lineStartTime) {
                if (animProgress >= lineStartTime) 1f else 0f
            } else {
                ((animProgress - lineStartTime) / (lineEndTime - lineStartTime)).coerceIn(0f, 1f)
            }
            if (segmentProgress <= 0f) continue

            val endX = pa.x + (pb.x - pa.x) * segmentProgress
            val endY = pa.y + (pb.y - pa.y) * segmentProgress
            val alpha = if (isSelected) 0.6f else 0.32f
            val strokeWidth = if (isSelected) 2.5f else 1.5f
            drawLine(
                color = lineColor.copy(alpha = alpha),
                start = Offset(pa.x, pa.y),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
            )
        }

        // Highlight selected constellation's stars with a localized glow.
        if (isSelected && animProgress > 0.5f) {
            for ((aName, bName) in c.lines) {
                for (name in listOf(aName, bName)) {
                    val rs = starMap[name] ?: continue
                    if (rs.altitudeDeg < -2) continue
                    val pt = p.project(rs.azimuthDeg, rs.altitudeDeg) ?: continue
                    drawCircle(
                        color = lineColor.copy(alpha = 0.15f),
                        radius = 10f,
                        center = Offset(pt.x, pt.y),
                    )
                }
            }
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
    tm: TextMeasurer, showLabels: Boolean,
) {
    for (rs in snap.stars) {
        if (rs.altitudeDeg < -2) continue
        val pt = p.project(rs.azimuthDeg, rs.altitudeDeg) ?: continue
        val tw = 0.8 + 0.2 * sin(t / 600.0 + rs.star.rightAscensionDeg)
        val r = magToRadius(rs.star.magnitude) * tw.toFloat()
        drawCircle(color = tint ?: starColor(rs.star.magnitude), radius = r, center = Offset(pt.x, pt.y))
        if (showLabels && rs.altitudeDeg > 2 && rs.star.magnitude <= 1.5) {
            drawSkyLabel(tm, rs.star.name, pt.x, pt.y, tint ?: Color(0xFFAEBBE8))
        }
    }
}

private fun DrawScope.drawPlanets(
    p: SkyProjection, snap: SkySnapshot, tint: Color?,
    tm: TextMeasurer, showLabels: Boolean,
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
        if (showLabels) drawSkyLabel(tm, rp.planet.displayName, pt.x, pt.y, color)
    }
}

private fun DrawScope.drawSatellites(
    p: SkyProjection, snap: SkySnapshot, t: Long, tint: Color?,
    tm: TextMeasurer, showLabels: Boolean,
) {
    for (rsat in snap.satellites) {
        if (rsat.altitudeDeg < 0) continue
        val pt = p.project(rsat.azimuthDeg, rsat.altitudeDeg) ?: continue
        val blink = (0.6 + 0.4 * sin(t / 180.0)).toFloat().coerceIn(0f, 1f)
        val color = tint ?: Color(rsat.satellite.colorHex)
        drawCircle(color = color.copy(alpha = blink), radius = 3.4f, center = Offset(pt.x, pt.y))
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
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x00040513), Color(0xE6080A18)),
            startY = size.height * 0.72f,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height * 0.72f),
        size = Size(size.width, size.height * 0.28f),
    )
}

// ── Milky Way band ──────────────────────────────────────────────────────────────

/**
 * Deterministic noise field for the Milky Way band. Seeded so the band is stable across frames
 * but varies spatially. Each entry: [azimuth, altitudeOffset, brightness, sizeFactor].
 */
private val milkyWayField: List<FloatArray> by lazy {
    val rng = Random(42L)
    // The Milky Way roughly follows a great circle; we approximate it as a band centred at a
    // slowly varying altitude across azimuth, with scatter above and below.
    List(600) {
        val az = rng.nextDouble(360.0)
        val bandCenter = 30.0 + 25.0 * cos(az * Math.PI / 180.0 + 1.2)
        val alt = bandCenter + rng.nextDouble(-18.0, 18.0)
        floatArrayOf(
            az.toFloat(),
            alt.toFloat(),
            (rng.nextDouble(0.15, 0.55)).toFloat(),
            (rng.nextDouble(0.5, 2.5)).toFloat(),
        )
    }
}

private fun DrawScope.drawMilkyWay(p: SkyProjection, t: Long, tint: Color?) {
    // Slow drift to give the band a living feel without distracting.
    val drift = sin(t / 9000.0) * 1.5
    // Hoist colour outside the per-particle loop — Color is a value class so no allocation,
    // but this avoids re-evaluating the branch 600 times per frame.
    val particleColor = tint ?: Color(0xFFB8C6E8)
    for (b in milkyWayField) {
        val pt = p.project(b[0].toDouble(), (b[1] + drift)) ?: continue
        val tw = (0.7 + 0.3 * sin(t / 4000.0 + b[0])).toFloat()
        val alpha = (b[2] * tw * 0.25f).coerceIn(0f, 0.18f)
        drawCircle(
            color = particleColor.copy(alpha = alpha),
            radius = b[3] * 3f,
            center = Offset(pt.x, pt.y),
        )
    }
    // Subtle radial glow layer to add depth to the band.
    val bandCenterAz = 180.0 + drift * 5
    val bandCenterPt = p.project(bandCenterAz, 42.0 + drift) ?: return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                (tint ?: Color(0xFF6B7FB5)).copy(alpha = 0.06f),
                Color.Transparent,
            ),
            radius = size.minDimension * 0.45f,
        ),
        center = Offset(bandCenterPt.x, bandCenterPt.y),
        radius = size.minDimension * 0.45f,
    )
}

// ── Shooting stars (meteors) ────────────────────────────────────────────────────

/** A single meteor with a curved trajectory and fading particle trail. */
private data class Meteor(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val curvatureX: Float,
    val curvatureY: Float,
    val spawnTimeMs: Long,
    val lifeMs: Long,
    val trailLength: Int,
)

/**
 * Manages spawning, lifetime, and removal of meteors.
 * Called once per frame from the Canvas draw phase where canvas dimensions are available.
 */
private class MeteorManager {
    private val active = mutableListOf<Meteor>()
    private var nextSpawnMs: Long = 3_000L
    private val rng = Random(System.currentTimeMillis())

    fun update(frameTimeMs: Long, nightMode: Boolean, canvasWidth: Float, canvasHeight: Float) {
        // Spawn new meteors at random intervals (less frequent in night mode to preserve dark adaptation).
        val spawnIntervalMin = if (nightMode) 8_000L else 4_000L
        val spawnIntervalMax = if (nightMode) 20_000L else 12_000L
        if (frameTimeMs > nextSpawnMs) {
            spawn(frameTimeMs, canvasWidth, canvasHeight)
            nextSpawnMs = frameTimeMs + rng.nextLong(spawnIntervalMin, spawnIntervalMax)
        }
        // Remove expired meteors.
        active.removeAll { frameTimeMs - it.spawnTimeMs > it.lifeMs }
    }

    private fun spawn(frameTimeMs: Long, canvasWidth: Float, canvasHeight: Float) {
        // Spawn from upper portion of the screen, travelling diagonally downward.
        // Uses actual canvas dimensions so meteors cover the full screen on any device.
        val startX = rng.nextFloat() * canvasWidth
        val startY = rng.nextFloat() * (canvasHeight * 0.4f)
        val angle = rng.nextDouble(Math.PI / 6.0, Math.PI / 2.5) // 30°–72° downward
        val speed = (0.6f + rng.nextFloat() * 0.8f) * 600f
        val vx = (cos(angle) * speed).toFloat() * (if (rng.nextBoolean()) 1f else -1f)
        val vy = (sin(angle) * speed).toFloat()
        val curvature = speed * 0.15f
        active += Meteor(
            startX = startX,
            startY = startY,
            velocityX = vx,
            velocityY = vy,
            curvatureX = (rng.nextFloat() * 2f - 1f) * curvature,
            curvatureY = (rng.nextFloat() * 2f - 1f) * curvature * 0.3f,
            spawnTimeMs = frameTimeMs,
            lifeMs = rng.nextLong(900L, 1800L),
            trailLength = rng.nextInt(12, 25),
        )
    }

    fun activeMeteors(): List<Meteor> = active
}

private fun DrawScope.drawShootingStars(manager: MeteorManager, t: Long, tint: Color?) {
    for (meteor in manager.activeMeteors()) {
        val age = (t - meteor.spawnTimeMs).toFloat()
        if (age < 0f) continue
        val progress = age / meteor.lifeMs
        if (progress > 1f) continue

        // Fade in quickly, hold, then fade out.
        val alpha = when {
            progress < 0.15f -> (progress / 0.15f)
            progress > 0.75f -> ((1f - progress) / 0.25f)
            else -> 1f
        }.coerceIn(0f, 1f)

        // Compute current head position with quadratic curve.
        val headX = meteor.startX + meteor.velocityX * age / 1000f + meteor.curvatureX * progress * progress
        val headY = meteor.startY + meteor.velocityY * age / 1000f + meteor.curvatureY * progress * progress

        // Draw trail as a series of fading circles from head backward.
        val trailColor = tint ?: Color(0xFFFFFFFF)
        for (i in 0 until meteor.trailLength) {
            val trailProgress = progress - i * 0.012f
            if (trailProgress < 0f) break
            val trailAge = trailProgress * meteor.lifeMs
            val tx = meteor.startX + meteor.velocityX * trailAge / 1000f + meteor.curvatureX * trailProgress * trailProgress
            val ty = meteor.startY + meteor.velocityY * trailAge / 1000f + meteor.curvatureY * trailProgress * trailProgress
            val trailAlpha = alpha * (1f - i.toFloat() / meteor.trailLength) * 0.8f
            val radius = (2.5f - i * 0.08f).coerceAtLeast(0.3f)
            drawCircle(
                color = trailColor.copy(alpha = trailAlpha),
                radius = radius,
                center = Offset(tx, ty),
            )
        }

        // Bright head.
        drawCircle(
            color = trailColor.copy(alpha = alpha),
            radius = 2.5f,
            center = Offset(headX, headY),
        )
        // Glow around head.
        drawCircle(
            color = (tint ?: Color(0xFFDDEEFF)).copy(alpha = alpha * 0.3f),
            radius = 6f,
            center = Offset(headX, headY),
        )
    }
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
