package com.stargaze.ai.ui.components

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stargaze.ai.ui.Destination
import com.stargaze.ai.ui.theme.StarColors

/** Frosted card surface used across detail/content screens. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StarColors.Card)
            .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
            .padding(15.dp),
    ) { content() }
}

/** Small status pill (VISIBLE / SOON / PRO). */
@Composable
fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * The bottom navigation bar with 5 destinations.
 *
 * Improvements:
 * - Glass-like translucent bar that blends with the sky rather than a solid block.
 * - Each item has a distinct pressed/active state (scale + background).
 * - Minimum 48dp touch target for accessibility.
 * - Material Icons replace emoji and each item has a content description for TalkBack.
 */
@Composable
fun StarGazeBottomBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        StarColors.Bg2.copy(alpha = 0.0f),
                        StarColors.Bg2.copy(alpha = 0.93f),
                    ),
                ),
            )
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Destination.entries.forEach { dest ->
            val selected = dest == current
            NavItem(
                destination = dest,
                selected = selected,
                onClick = { onSelect(dest) },
            )
        }
    }
}

@Composable
private fun NavItem(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "navItemScale",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) StarColors.Accent else StarColors.Faint,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "navItemColor",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) StarColors.Accent.copy(alpha = 0.13f) else Color.Transparent,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "navItemBg",
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (selected) "${destination.label}, selected" else destination.label
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            destination.label,
            color = iconColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
