package com.stargaze.ai.ui.settings

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.ai.ModelState
import com.stargaze.ai.ui.components.GlassCard
import com.stargaze.ai.ui.components.SkySheet
import com.stargaze.ai.ui.components.StatusPill
import com.stargaze.ai.ui.theme.StarColors

/** In-app legal/privacy reader. */
@Composable
fun LegalSheet(onDismiss: () -> Unit) {
    SkySheet(onDismiss = onDismiss, maxHeightFraction = 0.9f) {
        Text("Legal & Privacy", color = StarColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Plain-language summaries", color = StarColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
        LazyColumn {
            items(LegalContent.all.size) { i ->
                val doc = LegalContent.all[i]
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(doc.title, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(doc.body, color = Color(0xFFD7DCFF), fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** AI settings: cloud consent toggle and the on-device model picker. */
@Composable
fun AiSettingsSheet(onDismiss: () -> Unit, viewModel: AiSettingsViewModel = hiltViewModel()) {
    val s by viewModel.uiState.collectAsState()

    SkySheet(onDismiss = onDismiss, maxHeightFraction = 0.92f) {
        Text("AI settings", color = StarColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Control how the AI Sky Guide works", color = StarColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

        LazyColumn {
            item {
                ToggleCard(
                    title = "Cloud AI",
                    subtitle = if (s.cloudAiConsent)
                        "On — questions may be sent securely to our AI service for the richest answers."
                    else "Off — your questions never leave your device. Turn on for the richest answers.",
                    checked = s.cloudAiConsent,
                    onCheckedChange = viewModel::setCloudConsent,
                )
                Spacer(Modifier.height(16.dp))
                ToggleCard(
                    title = "High-contrast UI",
                    subtitle = if (s.highContrast)
                        "On — stronger colour contrast and clearer outlines."
                    else "Off — default dark-space styling.",
                    checked = s.highContrast,
                    onCheckedChange = viewModel::setHighContrast,
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("On-device AI", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Runs entirely on your phone — private and offline. Choose a model to download.",
                            color = StarColors.Muted, fontSize = 13.sp,
                        )
                    }
                    if (s.models.any { it.downloaded }) {
                        Switch(checked = s.onDeviceAiEnabled, onCheckedChange = viewModel::setOnDeviceEnabled)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (!s.onDeviceSupported) {
                item {
                    Text(
                        "No downloadable models are available right now.",
                        color = StarColors.Muted, fontSize = 13.sp,
                    )
                }
            } else {
                items(s.models.size) { i ->
                    ModelCard(
                        option = s.models[i],
                        downloadState = if (s.downloadingModelId == s.models[i].id) s.downloadState else ModelState.Absent,
                        onDownload = { viewModel.downloadModel(s.models[i].id) },
                        onSelect = { viewModel.selectModel(s.models[i].id) },
                        onDelete = { viewModel.deleteModel(s.models[i].id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun ModelCard(
    option: ModelOption,
    downloadState: ModelState,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(enabled = option.downloaded, onClick = onSelect)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(option.label, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(option.description, color = StarColors.Muted, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.width(8.dp))
                when {
                    option.selected && option.downloaded ->
                        StatusPill("IN USE", StarColors.Accent, StarColors.Accent.copy(alpha = 0.18f))
                    option.suitable ->
                        StatusPill("FITS", StarColors.Green, StarColors.Green.copy(alpha = 0.16f))
                    else ->
                        StatusPill("HEAVY", StarColors.Gold, StarColors.Gold.copy(alpha = 0.16f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "~${option.approxMb} MB" + if (!option.suitable) " · may be slow on this device" else "",
                color = StarColors.Faint, fontSize = 11.sp,
            )
            Spacer(Modifier.height(10.dp))

            when (downloadState) {
                is ModelState.Downloading -> {
                    Text("Downloading… ${(downloadState.progress * 100).toInt()}%", color = StarColors.Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { downloadState.progress }, modifier = Modifier.fillMaxWidth())
                }
                is ModelState.Verifying -> Text("Verifying integrity…", color = StarColors.Muted, fontSize = 12.sp)
                is ModelState.Failed -> Text(downloadState.reason, color = StarColors.Red, fontSize = 12.sp)
                else -> Unit
            }

            if (downloadState !is ModelState.Downloading && downloadState !is ModelState.Verifying) {
                if (option.downloaded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (!option.selected) ActionButton("Use this model", Modifier.weight(1f)) { onSelect() }
                        ActionButton("Remove", Modifier.weight(1f), outlined = true) { onDelete() }
                    }
                } else {
                    ActionButton("Download (~${option.approxMb} MB, Wi-Fi)", Modifier.fillMaxWidth()) { onDownload() }
                }
            }
        }
    }
}

@Composable
private fun ToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = StarColors.Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ActionButton(text: String, modifier: Modifier = Modifier, outlined: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (outlined) StarColors.Card else StarColors.Accent.copy(alpha = 0.16f))
            .border(1.dp, if (outlined) StarColors.Line else StarColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (outlined) StarColors.Muted else StarColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp) }
}
