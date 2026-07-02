package com.stargaze.ai.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stargaze.ai.ui.theme.StarColors
import com.stargaze.ai.ui.theme.breathe
import kotlin.math.sin

private data class Feature(val emoji: String, val title: String, val body: String)

/** Animated twinkling background star for the onboarding screen. */
@Composable
private fun TwinkleBackground() {
    val stars = remember {
        List(80) {
            floatArrayOf(
                (Math.random() * 1000).toFloat(),
                (Math.random() * 2000).toFloat(),
                (Math.random() * 2 + 0.5f).toFloat(),
                (Math.random() * 6000 + 2000).toFloat(),
                (Math.random() * 6.28).toFloat(),
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "twinkle")
    val t = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "twinkle",
    ).value

    Box(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            val (x, y, size, duration, phase) = star
            val tw = 0.55f + 0.45f * sin((t * duration + phase).toDouble()).toFloat()
            Box(
                modifier = Modifier
                    .padding(start = x.dp, top = y.dp)
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9FB0E8).copy(alpha = (tw * 0.6f).coerceIn(0.05f, 0.8f))),
            )
        }
    }
}

/** Three-step onboarding ending with a location-permission request.
 *  Fixed: robust lifecycle handling, safe permission launch, smooth animations.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var permissionError by remember { mutableStateOf(false) }

    // Permission launcher with safe handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        // Succeed if any one of the requested permissions is granted (coarse is enough).
        val granted = results.values.any { it }
        if (granted) {
            onFinished()
        } else {
            // Permission denied — still let the user proceed with default (Mumbai).
            permissionError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05060F), Color(0xFF0A0E1F), Color(0xFF141A35)),
                ),
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Animated twinkling background
        TwinkleBackground()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Step 0: Welcome / Brand
            AnimatedVisibility(
                visible = step == 0,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 4 },
            ) {
                StepWelcome()
            }

            // Step 1: Features
            AnimatedVisibility(
                visible = step == 1,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 4 },
            ) {
                StepFeatures()
            }

            // Step 2: Location ask
            AnimatedVisibility(
                visible = step >= 2,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 4 },
            ) {
                StepLocation(
                    permissionError = permissionError,
                    onEnableLocation = {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                    onSkip = { onFinished() },
                )
            }

            Spacer(Modifier.height(28.dp))

            if (step < 2) {
                PrimaryButton(
                    text = when (step) { 0 -> "Get started"; else -> "Continue" },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { step++ },
                )
            }

            Spacer(Modifier.height(26.dp))
            // Page indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(if (i == step) 22.dp else 7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i == step) StarColors.Accent else Color(0x33FFFFFF))
                            .animateContentSize(tween(300)),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(StarColors.Accent.copy(alpha = 0.12f))
                .border(1.dp, StarColors.Accent.copy(alpha = 0.3f), CircleShape)
                .breathe(1.0f, 1.03f, 3000),
            contentAlignment = Alignment.Center,
        ) {
            Text("\uD83D\uDD2D", fontSize = 64.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "StarGaze AI",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = StarColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Your pocket astronomer that actually talks back. Point, discover and learn the night sky.",
            color = StarColors.Muted,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun StepFeatures() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Why you'll love it",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = StarColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FEATURES.forEach { f ->
            FeatureRow(f)
            Spacer(Modifier.height(9.dp))
        }
    }
}

@Composable
private fun StepLocation(
    permissionError: Boolean,
    onEnableLocation: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(StarColors.Gold.copy(alpha = 0.10f))
                .border(1.dp, StarColors.Gold.copy(alpha = 0.3f), CircleShape)
                .breathe(1.0f, 1.02f, 4000),
            contentAlignment = Alignment.Center,
        ) {
            Text("\uD83C\uDF0D", fontSize = 48.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Where are you?",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = StarColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "I use your location to compute exactly what's above your horizon right now. You can skip and use the default (Mumbai).",
            color = StarColors.Muted,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Enable location", modifier = Modifier.fillMaxWidth(), onClick = onEnableLocation)
        if (permissionError) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Permission was denied. You can always change this later in your device settings.",
                color = StarColors.Gold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Skip for now",
            color = StarColors.Muted,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onSkip),
        )
    }
}

@Composable
private fun FeatureRow(f: Feature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, StarColors.Line, RoundedCornerShape(15.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(f.emoji, fontSize = 24.sp)
        Spacer(Modifier.width(13.dp))
        Column {
            Text(f.title, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(f.body, color = StarColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bgModifier = if (enabled) {
        Modifier.background(StarColors.BrandGradient)
    } else {
        Modifier.background(StarColors.Line)
    }
    Box(
        modifier = modifier
            .then(bgModifier)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 16.dp)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Color(0xFF0A0C1A) else StarColors.Muted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private val FEATURES = listOf(
    Feature("\uD83D\uDCF1", "Point & identify", "Hold your phone up to instantly name any star, planet, satellite or constellation."),
    Feature("\u2728", "AI Sky Guide", "Ask anything in plain language and get real, location-aware answers."),
    Feature("\uD83D\uDEF0\uFE0F", "Track the ISS live", "Follow the space station and satellites across your sky in real time."),
)
