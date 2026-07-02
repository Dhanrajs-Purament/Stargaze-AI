package com.stargaze.ai.ui.pro

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.billing.ProSku
import com.stargaze.ai.billing.PurchaseState
import com.stargaze.ai.ui.components.FeatureCheck
import com.stargaze.ai.ui.components.SheetTitle
import com.stargaze.ai.ui.components.ShimmerBorderCard
import com.stargaze.ai.ui.components.SkySheet
import com.stargaze.ai.ui.onboarding.PrimaryButton
import com.stargaze.ai.ui.theme.StarColors

private data class PlanUi(
    val productId: String,
    val displayName: String,
    val description: String,
    val best: Boolean,
    val saveLabel: String? = null,
)

private fun planUiFor(productId: String): PlanUi = when (productId) {
    ProViewModel.PRO_PRODUCT_IDS[0] -> PlanUi(productId, "Annual", "Billed yearly \u00B7 7-day free trial", best = true, saveLabel = "SAVE 58%")
    ProViewModel.PRO_PRODUCT_IDS[1] -> PlanUi(productId, "Monthly", "Cancel anytime", best = false)
    else -> PlanUi(productId, "Lifetime", "Pay once, yours forever", best = false)
}

@Composable
fun ProSheet(
    onDismiss: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
    onOpenLegal: () -> Unit = {},
    viewModel: ProViewModel = hiltViewModel(),
) {
    val isPro by viewModel.isPro.collectAsState()
    val products by viewModel.products.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val activity = LocalContext.current as? Activity

    val sortedProducts = rememberProductsOrder(products)

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
                SheetTitle("StarGaze Pro")
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isPro) "You're Pro \u2713" else "Unlock the whole universe",
                        color = StarColors.Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (isPro) "Thanks for supporting StarGaze" else "Choose a plan that works for you",
                        color = StarColors.Muted,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
            if (!isPro) {
                item {
                    if (sortedProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(17.dp))
                                .background(StarColors.Card)
                                .border(1.dp, StarColors.Line, RoundedCornerShape(17.dp))
                                .padding(18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Loading plans from Google Play\u2026",
                                color = StarColors.Muted,
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        sortedProducts.forEach { sku ->
                            PlanCard(
                                sku = sku,
                                onClick = { activity?.let { viewModel.purchase(it, sku.productId) } },
                            )
                            Spacer(Modifier.height(11.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            item {
                features.forEach { f ->
                    FeatureCheck(f, modifier = Modifier.padding(vertical = 7.dp))
                }
                Spacer(Modifier.height(10.dp))
                if (!isPro) {
                    when (purchaseState) {
                        is PurchaseState.Error -> Text(
                            (purchaseState as PurchaseState.Error).message,
                            color = StarColors.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        PurchaseState.Loading -> Text(
                            "Contacting Google Play\u2026",
                            color = StarColors.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        else -> {}
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        "Restore purchases",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = viewModel::restorePurchases,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FooterLink("AI settings", Modifier.weight(1f), onOpenAiSettings)
                    FooterLink("Legal & Privacy", Modifier.weight(1f), onOpenLegal)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Subscriptions auto-renew until cancelled. Restore \u00B7 Terms \u00B7 Privacy",
                    color = StarColors.Faint,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun rememberProductsOrder(products: List<ProSku>): List<ProSku> {
    return androidx.compose.runtime.remember(products) {
        val order = ProViewModel.PRO_PRODUCT_IDS.withIndex().associate { it.value to it.index }
        products.sortedBy { order[it.productId] ?: Int.MAX_VALUE }
    }
}

@Composable
private fun PlanCard(sku: ProSku, onClick: () -> Unit) {
    val meta = planUiFor(sku.productId)
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meta.displayName, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    meta.saveLabel?.let {
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
                Text(meta.description, color = StarColors.Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(sku.formattedPrice, color = StarColors.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }
    }

    val semanticsModifier = Modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = "${meta.displayName} plan, ${sku.formattedPrice}"
        }

    if (meta.best) {
        ShimmerBorderCard(
            modifier = semanticsModifier
                .clickable(onClick = onClick),
            cornerRadius = 17.dp,
        ) {
            content()
        }
    } else {
        Box(
            modifier = semanticsModifier
                .clip(RoundedCornerShape(17.dp))
                .background(StarColors.Card)
                .border(1.dp, StarColors.Line, RoundedCornerShape(17.dp))
                .clickable(onClick = onClick),
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
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = text
            }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = StarColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
