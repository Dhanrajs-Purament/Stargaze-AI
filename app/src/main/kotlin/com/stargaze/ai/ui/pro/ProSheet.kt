package com.stargaze.ai.ui.pro

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.ui.components.FeatureCheck
import com.stargaze.ai.ui.components.SheetTitle
import com.stargaze.ai.ui.components.ShimmerBorderCard
import com.stargaze.ai.ui.components.SkySheet
import com.stargaze.ai.ui.onboarding.PrimaryButton
import com.stargaze.ai.ui.theme.StarColors

private data class Plan(
    val name: String,
    val desc: String,
    val price: String,
    val per: String,
    val best: Boolean,
    val save: String? = null,
)

@Composable
fun ProSheet(
    onDismiss: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
    onOpenLegal: () -> Unit = {},
    viewModel: ProViewModel = hiltViewModel(),
) {
    val isPro by viewModel.isPro.collectAsState()

    val plans = listOf(
        Plan("Annual", "Billed yearly \u00B7 7-day free trial", "$24.99", "\u20B9599 / year", best = true, save = "SAVE 58%"),
        Plan("Monthly", "Cancel anytime", "$3.99", "\u20B9149 / mo", best = false),
        Plan("Lifetime", "Pay once, yours forever", "$44.99", "\u20B91,799 once", best = false),
    )
    val features = listOf(
        "Unlimited AI Sky Guide questions & voice mode",
        "Full Gaia catalog - 1.7 billion stars",
        "Live satellite pass alerts (ISS, Starlink & more)",
        "All guided tours, quizzes & learning paths",
        "Astrophotography planner & deep-sky objects",
        "Ad-free, offline-first experience",
    )

    SkySheet(onDismiss = onDismiss, maxHeightFraction = 0.9f) {
        LazyColumn {
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDC51", fontSize = 48.sp)
                    Text(
                        if (isPro) "You're Pro" else "StarGaze Pro",
                        color = StarColors.Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        if (isPro) "Thanks for supporting StarGaze" else "Unlock the whole universe",
                        color = StarColors.Muted,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
            if (!isPro) {
                item {
                    plans.forEach { plan ->
                        PlanCard(plan)
                        Spacer(Modifier.height(11.dp))
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                features.forEach { f ->
                    FeatureCheck(f, modifier = Modifier.padding(vertical = 7.dp))
                }
                Spacer(Modifier.height(10.dp))
                if (!isPro) {
                    PrimaryButton("Start 7-day free trial", modifier = Modifier.fillMaxWidth(), onClick = viewModel::activatePro)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(StarColors.Accent.copy(alpha = 0.1f))
                        .border(1.dp, StarColors.Accent.copy(alpha = 0.25f), RoundedCornerShape(13.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("\u2708\uFE0F", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("On Telegram, unlock Pro instantly with Telegram Stars - no app store needed.", color = Color(0xFFCDD6FF), fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FooterLink("AI settings", Modifier.weight(1f), onOpenAiSettings)
                    FooterLink("Legal & Privacy", Modifier.weight(1f), onOpenLegal)
                }
                Spacer(Modifier.height(12.dp))
                Text("Prototype - no real charge. Restore \u00B7 Terms \u00B7 Privacy", color = StarColors.Faint, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PlanCard(plan: Plan) {
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.name, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    plan.save?.let {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(StarColors.Gold)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(it, color = Color(0xFF0A0C1A), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text(plan.desc, color = StarColors.Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(plan.price, color = StarColors.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(plan.per, color = StarColors.Muted, fontSize = 12.sp)
            }
        }
    }

    if (plan.best) {
        ShimmerBorderCard(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "${plan.name} plan, best value" },
            cornerRadius = 17.dp,
        ) {
            content()
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(StarColors.Card)
                .border(1.dp, StarColors.Line, RoundedCornerShape(17.dp))
                .semantics { contentDescription = "${plan.name} plan" },
        ) {
            content()
        }
    }
}

@Composable
private fun FooterLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StarColors.Card)
            .border(1.dp, StarColors.Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = StarColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
