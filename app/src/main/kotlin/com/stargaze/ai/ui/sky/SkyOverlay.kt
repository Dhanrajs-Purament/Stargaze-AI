package com.stargaze.ai.ui.sky

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stargaze.ai.ui.theme.StarColors
import com.stargaze.ai.ui.theme.pulsing
import kotlin.math.roundToInt

/** Cardinal label for an azimuth. */
private fun azName(az: Double): String {
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return dirs[((az / 45.0).roundToInt()) % 8]
}

/**
 * All persistent on-sky chrome: brand/top bar, right-side tool rail, the bottom HUD pills, an
 * optional time-travel slider, and the tapped-object detail sheet.
 *
 * Enhancements:
 * - Emoji replaced by Material Icons with content descriptions.
 * - Tool rail buttons give tactile press feedback (scale) and clear active states.
 * - HUD pills and the time bar use the shared design tokens.
 * - Minimum touch targets are accessibility-friendly.
 */
@Composable
fun SkyOverlay(
    uiState: SkyUiState,
    viewModel: SkyViewModel,
    onIdentifySky: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimeBar by remember { mutableStateOf(false) }
    val vs = uiState.viewState

    Box(modifier = modifier) {
        // ---- Top bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "\uD83D\uDD2D",
                fontSize = 18.sp,
                modifier = Modifier.semantics { contentDescription = "StarGaze AI" },
            )
            Spacer(Modifier.width(6.dp))
            Text("StarGaze", color = StarColors.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(StarColors.Gold)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) { Text("AI", color = Color(0xFF0A0C1A), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }

            Spacer(Modifier.weight(1f))

            IconChip(
                icon = Icons.Filled.ScreenRotation,
                contentDescription = if (vs.arMode) "AR mode on" else "AR mode off",
                active = vs.arMode,
                enabled = uiState.sensorAvailable,
                onClick = { viewModel.toggleAr() },
            )
            Spacer(Modifier.width(8.dp))
            IconChip(
                icon = Icons.Filled.CenterFocusStrong,
                contentDescription = "Identify objects in the sky",
                active = false,
                onClick = onIdentifySky,
            )
            Spacer(Modifier.width(8.dp))
            IconChip(
                icon = Icons.Filled.Nightlight,
                contentDescription = if (vs.nightMode) "Night mode on" else "Night mode off",
                active = vs.nightMode,
                onClick = { viewModel.toggleNightMode() },
            )
        }

        // ---- Right-side tool rail ----
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ToolButton(
                icon = Icons.Filled.Polyline,
                label = "LINES",
                active = vs.showConstellationLines,
                contentDescription = "Toggle constellation lines",
            ) { viewModel.toggleLines() }
            ToolButton(
                icon = Icons.AutoMirrored.Filled.Label,
                label = "LABELS",
                active = vs.showLabels,
                contentDescription = "Toggle labels",
            ) { viewModel.toggleLabels() }
            ToolButton(
                icon = Icons.Filled.SatelliteAlt,
                label = "SATS",
                active = vs.showSatellites,
                contentDescription = "Toggle satellites",
            ) { viewModel.toggleSatellites() }
            ToolButton(
                icon = Icons.Filled.Schedule,
                label = "TIME",
                active = showTimeBar,
                contentDescription = "Toggle time travel bar",
            ) { showTimeBar = !showTimeBar }
            ToolButton(
                icon = Icons.Filled.MyLocation,
                label = "RESET",
                active = false,
                contentDescription = "Recenter the sky",
            ) { viewModel.recenter() }
        }

        // ---- HUD pills + time bar bottom-center ----
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(visible = showTimeBar) {
                TimeBar(
                    offsetMinutes = uiState.timeOffsetMinutes,
                    onOffsetChange = viewModel::setTimeOffsetMinutes,
                    onReset = viewModel::resetTime,
                )
            }
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudPill("\uD83E\uDDED", "${azName(vs.centerAzimuthDeg)} ${vs.centerAzimuthDeg.roundToInt()}\u00B0")
                HudPill("Alt", "${vs.centerAltitudeDeg.roundToInt()}\u00B0")
                HudPill("\uD83D\uDCCD", uiState.location.name.ifBlank { "Sky" })
            }
        }

        // ---- Tapped object detail sheet ----
        uiState.selected?.let { obj ->
            ObjectDetailSheet(
                obj = obj,
                location = uiState.location,
                timeOffsetMinutes = uiState.timeOffsetMinutes,
                onLocate = { viewModel.locate(obj) },
                onDismiss = { viewModel.select(null) },
            )
        }
    }
}

@Composable
private fun IconChip(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "iconChipScale",
    )
    val bg by animateColorAsState(
        targetValue = if (active) StarColors.Card2 else StarColors.Card,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "iconChipBg",
    )
    val stroke by animateColorAsState(
        targetValue = if (active) StarColors.Green else StarColors.Line,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "iconChipStroke",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) StarColors.Green else StarColors.Ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "toolButtonScale",
    )
    val tint by animateColorAsState(
        targetValue = if (active) StarColors.Accent else StarColors.Muted,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "toolButtonTint",
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (active) {
                    StarColors.Accent.copy(alpha = 0.16f)
                } else {
                    Color(0x990C1022)
                },
            )
            .border(
                1.dp,
                if (active) StarColors.Accent.copy(alpha = 0.55f) else StarColors.Line,
                RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HudPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0x9E0A0D1C))
            .border(1.dp, StarColors.Line, RoundedCornerShape(11.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = StarColors.Muted, fontSize = 11.sp)
        Spacer(Modifier.width(5.dp))
        Text(value, color = StarColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimeBar(offsetMinutes: Int, onOffsetChange: (Int) -> Unit, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD10A0D1C))
            .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (offsetMinutes == 0) "Now \u00B7 live" else "${offsetMinutes / 60}h ${offsetMinutes % 60}m offset",
                color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StarColors.Green.copy(alpha = 0.14f))
                    .clickable(
                        onClickLabel = "Reset time to live",
                        role = Role.Button,
                        onClick = onReset,
                    )
                    .semantics { contentDescription = "Reset time to live" }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StarColors.Green)
                        .pulsing(minAlpha = 0.5f, maxAlpha = 1f, durationMillis = 1200),
                )
                Spacer(Modifier.width(5.dp))
                Text("LIVE", color = StarColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Slider(
            value = offsetMinutes.toFloat(),
            onValueChange = { onOffsetChange(it.roundToInt()) },
            valueRange = -720f..720f,
        )
    }
}
