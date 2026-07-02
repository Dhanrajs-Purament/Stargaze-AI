package com.stargaze.ai.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset

/**
 * StarGaze AI Motion System — Production-grade animation specs, transitions, and modifiers.
 *
 * Follows Material Motion Guidelines (https://m3.material.io/styles/motion/overview) adapted
 * for a cinematic dark-space aesthetic. All durations are chosen to feel snappy yet luxurious.
 */
object StarGazeMotion {

    /* ── Durations ── */
    const val DURATION_FAST = 150
    const val DURATION_NORMAL = 300
    const val DURATION_SLOW = 500
    const val DURATION_SLOWER = 800
    const val DURATION_ENTRANCE = 600
    const val DURATION_STAGGER_STEP = 80

    /* ── Specs ── */
    val TweenFast = tween<Float>(DURATION_FAST, easing = FastOutSlowInEasing)
    val TweenFastOffset = tween<IntOffset>(DURATION_FAST, easing = FastOutSlowInEasing)
    val TweenNormal = tween<Float>(DURATION_NORMAL, easing = FastOutSlowInEasing)
    val TweenNormalOffset = tween<IntOffset>(DURATION_NORMAL, easing = FastOutSlowInEasing)
    val TweenSlow = tween<Float>(DURATION_SLOW, easing = FastOutSlowInEasing)
    val TweenEntrance = tween<Float>(DURATION_ENTRANCE, easing = FastOutSlowInEasing)
    val TweenEntranceOffset = tween<IntOffset>(DURATION_ENTRANCE, easing = FastOutSlowInEasing)

    /* ── Enter/Exit Transitions ── */
    /** Slide up + fade in — ideal for bottom sheets, cards, overlays. */
    val SlideUpFadeIn = androidx.compose.animation.fadeIn(TweenEntrance) +
        androidx.compose.animation.slideInVertically(TweenEntranceOffset) { it / 3 }

    /** Slide down + fade out — counterpart to [SlideUpFadeIn]. */
    val SlideDownFadeOut = androidx.compose.animation.fadeOut(TweenNormal) +
        androidx.compose.animation.slideOutVertically(TweenNormalOffset) { it / 3 }

    /** Scale + fade — for dialog/modal popups. */
    val ScaleFadeIn = androidx.compose.animation.fadeIn(TweenNormal) +
        androidx.compose.animation.scaleIn(TweenNormal, initialScale = 0.92f)

    val ScaleFadeOut = androidx.compose.animation.fadeOut(TweenFast) +
        androidx.compose.animation.scaleOut(TweenFast, targetScale = 0.92f)

    /** Horizontal slide for page transitions. */
    val SlideInFromRight = androidx.compose.animation.fadeIn(TweenEntrance) +
        androidx.compose.animation.slideInHorizontally(TweenEntranceOffset) { it }

    val SlideOutToLeft = androidx.compose.animation.fadeOut(TweenNormal) +
        androidx.compose.animation.slideOutHorizontally(TweenNormalOffset) { -it }
}

/* ── Modifiers ── */

/**
 * Applies a subtle continual pulse to the element.
 * Typically used for "LIVE" indicators, recording dots, or primary CTAs.
 */
fun Modifier.pulsing(
    minAlpha: Float = 0.6f,
    maxAlpha: Float = 1.0f,
    durationMillis: Int = 1200,
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha = infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    ).value
    this.then(alpha(alpha))
}

/**
 * Applies a subtle scale-breathing effect — a gentler version of [pulsing].
 */
fun Modifier.breathe(
    minScale: Float = 1.0f,
    maxScale: Float = 1.03f,
    durationMillis: Int = 3000,
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val scale = infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    ).value
    this.then(scale(scale))
}
