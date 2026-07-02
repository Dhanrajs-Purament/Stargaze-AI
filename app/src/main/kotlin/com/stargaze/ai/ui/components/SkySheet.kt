package com.stargaze.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stargaze.ai.ui.theme.StarColors
import com.stargaze.ai.ui.theme.StarGazeMotion

/**
 * A bottom sheet that overlays the live sky. Tapping the scrim dismisses it. Content scrolls
 * within a max height so the sky stays partially visible behind it. Sheet content animates in
 * with a slide-up + fade; scrim cross-fades in and out.
 */
@Composable
fun SkySheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeightFraction: Float = 0.84f,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim — animated cross-fade so the live sky visibly dim/lightens as the sheet opens.
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(StarGazeMotion.DURATION_SLOW)),
            exit = fadeOut(tween(StarGazeMotion.DURATION_NORMAL)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA020308))
                    .clickable(onClick = onDismiss),
            )
        }

        // Sheet — animated slide-up + fade-out on dismiss.
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                tween(
                    durationMillis = StarGazeMotion.DURATION_SLOW,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                ),
            ) { it } + fadeIn(tween(StarGazeMotion.DURATION_SLOW)),
            exit = slideOutVertically(tween(StarGazeMotion.DURATION_NORMAL)) { it } +
                fadeOut(tween(StarGazeMotion.DURATION_NORMAL)),
        ) {
            Column(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * maxHeightFraction)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF11152D), Color(0xFF0A0D1D))),
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp),
            ) {
                // Grab handle
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 14.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x33FFFFFF)),
                )
                content()
            }
        }
    }
}

/** Section heading used at the top of a sheet. */
@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    Text(title, color = StarColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    if (subtitle != null) {
        Text(subtitle, color = StarColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
    }
}
