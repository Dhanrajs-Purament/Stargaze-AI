package com.stargaze.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stargaze.ai.ui.theme.StarColors
import com.stargaze.ai.ui.theme.StarGazeMotion

/**
 * A card with an animated shimmering border, intended for the "best value" pricing option.
 * The shimmer sweeps a gold-to-accent gradient across the border continuously.
 */
@Composable
fun ShimmerBorderCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 17.dp,
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .drawWithContent {
                drawContent()
                val brush = Brush.linearGradient(
                    colors = listOf(
                        StarColors.Accent.copy(alpha = 0.3f),
                        StarColors.Gold.copy(alpha = 0.65f),
                        StarColors.Accent.copy(alpha = 0.3f),
                    ),
                    start = Offset(size.width * progress, 0f),
                    end = Offset(size.width * progress + size.width, 0f),
                )
                drawRoundRect(
                    brush = brush,
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            },
    ) {
        content()
    }
}

/**
 * Staggered quality-score bars used in the Tonight hero. Bars illuminate left-to-right as the
 * sheet enters, giving the score a sense of "filling up".
 */
@Composable
fun AnimatedQualityBars(score: Int, count: Int = 5, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp), modifier = modifier) {
        repeat(count) { i ->
            val color by animateFloatAsState(
                targetValue = if (i < score) 1f else 0f,
                animationSpec = tween(500, delayMillis = i * StarGazeMotion.DURATION_STAGGER_STEP),
                label = "qualityBar$i",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (i < score) StarColors.Accent.copy(alpha = 0.3f + 0.7f * color)
                        else Color(0x1FFFFFFF)
                    ),
            )
        }
    }
}

/** Three-dot typing indicator with staggered pulse. */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing$i")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(450, delayMillis = i * 120, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "typingDot$i",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(StarColors.Accent),
            )
            if (i < 2) Spacer(Modifier.width(4.dp))
        }
    }
}

/** Feature checklist row with an animated checkmark that scales in on appearance. */
@Composable
fun FeatureCheck(text: String, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .scale(scale.value)
                .size(20.dp)
                .clip(CircleShape)
                .background(StarColors.Green.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = StarColors.Green,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color(0xFFD7DCFF), fontSize = 14.sp)
    }
}
