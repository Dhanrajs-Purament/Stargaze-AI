package com.stargaze.ai.ui.tonight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.astronomy.EventVisibility
import com.stargaze.ai.astronomy.SkyEvent
import com.stargaze.ai.astronomy.SkyObject
import com.stargaze.ai.ui.components.AnimatedQualityBars
import com.stargaze.ai.ui.components.GlassCard
import com.stargaze.ai.ui.components.SheetTitle
import com.stargaze.ai.ui.components.SkySheet
import com.stargaze.ai.ui.components.StatusPill
import com.stargaze.ai.ui.theme.StarColors

@Composable
fun TonightSheet(
    onLocate: (SkyObject) -> Unit,
    onDismiss: () -> Unit,
    viewModel: TonightViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    SkySheet(onDismiss = onDismiss) {
        LazyColumn {
            item {
                SheetTitle(
                    title = "\uD83C\uDF0C Tonight for you",
                    subtitle = "${state.dateLabel} \u00B7 ${state.locationName}",
                )
            }
            item {
                HeroCard(state)
                Spacer(Modifier.height(14.dp))
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("\uD83E\uDD16 AI Sky Brief", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(state.brief, color = StarColors.Muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Up right now \u00B7 tap to locate", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (state.upItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(StarColors.Card)
                            .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
                            .padding(18.dp),
                    ) {
                        Text(
                            "Nothing above the horizon yet \u2014 try the time slider to preview later tonight.",
                            color = StarColors.Muted,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                items(state.upItems) { item ->
                    UpRow(
                        name = item.name,
                        meta = item.meta,
                        live = item.live,
                        onClick = { onLocate(item.obj); onDismiss() },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Upcoming sky events", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(state.events) { ev ->
                EventRow(ev)
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun HeroCard(state: TonightUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A2150), Color(0xFF2A1A4A))))
            .border(1.dp, StarColors.Line, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text("${state.headline} sky", color = StarColors.Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "${state.upItems.count { !it.live }} objects \u00B7 ${state.upItems.count { it.live }} satellites overhead",
            color = StarColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        AnimatedQualityBars(score = state.qualityScore, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun UpRow(name: String, meta: String, live: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StarColors.Card)
            .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(15.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name, $meta${if (live) ", live" else ", visible"}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(meta, color = StarColors.Muted, fontSize = 13.sp)
        }
        if (live) StatusPill("LIVE", StarColors.Gold, StarColors.Gold.copy(alpha = 0.16f))
        else StatusPill("VISIBLE", StarColors.Green, StarColors.Green.copy(alpha = 0.16f))
    }
}

@Composable
private fun EventRow(ev: SkyEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StarColors.Card)
            .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ev.emoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ev.title, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(ev.description, color = StarColors.Muted, fontSize = 13.sp)
        }
        val (c, bg) = when (ev.visibility) {
            EventVisibility.VISIBLE -> StarColors.Green to StarColors.Green.copy(alpha = 0.16f)
            EventVisibility.SOON -> StarColors.Gold to StarColors.Gold.copy(alpha = 0.16f)
            EventVisibility.PRO -> StarColors.Accent2 to StarColors.Accent2.copy(alpha = 0.18f)
        }
        StatusPill(ev.whenLabel, c, bg)
    }
}
