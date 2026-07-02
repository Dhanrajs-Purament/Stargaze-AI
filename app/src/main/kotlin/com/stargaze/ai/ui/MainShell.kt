package com.stargaze.ai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.render.SkyCanvas
import com.stargaze.ai.ui.components.StarGazeBottomBar
import com.stargaze.ai.ui.guide.GuideSheet
import com.stargaze.ai.ui.learn.LearnSheet
import com.stargaze.ai.ui.pro.ProSheet
import com.stargaze.ai.ui.settings.AiConsentDialog
import com.stargaze.ai.ui.settings.AiSettingsSheet
import com.stargaze.ai.ui.settings.AiSettingsViewModel
import com.stargaze.ai.ui.settings.LegalSheet
import com.stargaze.ai.ui.sky.SkyOverlay
import com.stargaze.ai.ui.sky.SkyViewModel
import com.stargaze.ai.ui.theme.StarColors
import com.stargaze.ai.ui.tonight.TonightSheet

/** Transient overlays opened on top of the main shell (not bottom-nav destinations). */
private enum class Overlay { NONE, AI_SETTINGS, LEGAL, IDENTIFY }

/**
 * Main application shell. The animated [SkyCanvas] is always present as the background. The four
 * non-Sky destinations slide up as bottom sheets over the live sky. A first-run AI/privacy consent
 * dialog gates the experience until the user makes a choice (no login required).
 *
 * Enhancements:
 * - Bottom-sheet destinations cross-fade and slide instead of switching instantly.
 * - A translucent central FAB exposes the primary "Identify" action.
 * - The system back button dismisses open sheets or overlays before exiting.
 */
@Composable
fun MainShell(
    skyViewModel: SkyViewModel = hiltViewModel(),
    aiSettingsViewModel: AiSettingsViewModel = hiltViewModel(),
) {
    var current by remember { mutableStateOf(Destination.SKY) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    val uiState by skyViewModel.uiState.collectAsState()
    val snapshot by skyViewModel.snapshot.collectAsState()
    val aiState by aiSettingsViewModel.uiState.collectAsState()

    var frameTimeMs by remember { mutableLongStateOf(0L) }
    var lastRecompute by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { ms ->
                frameTimeMs = ms
                if (ms - lastRecompute > 1200L) {
                    lastRecompute = ms
                    skyViewModel.recomputeSnapshot()
                }
            }
        }
    }

    // Back handler: dismiss sheets/overlays first, then let the activity handle back.
    BackHandler(enabled = current != Destination.SKY || overlay != Overlay.NONE) {
        when {
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            current != Destination.SKY -> current = Destination.SKY
        }
    }

    val openIdentify = {
        overlay = Overlay.IDENTIFY
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SkyCanvas(
            snapshot = snapshot,
            viewState = uiState.viewState,
            frameTimeMs = frameTimeMs,
            selected = uiState.selected,
            onPan = skyViewModel::pan,
            onZoom = skyViewModel::zoom,
            onTapObject = skyViewModel::select,
            onTapEmpty = { skyViewModel.select(null) },
            modifier = Modifier.fillMaxSize(),
        )

        SkyOverlay(
            uiState = uiState,
            viewModel = skyViewModel,
            onIdentifySky = openIdentify,
            modifier = Modifier.fillMaxSize(),
        )

        // Primary identify action floating above the bottom nav.
        FloatingActionButton(
            onClick = openIdentify,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(y = (-88).dp),
            containerColor = StarColors.Accent.copy(alpha = 0.16f),
            contentColor = StarColors.Accent,
            shape = androidx.compose.foundation.shape.CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = "Identify objects in the sky",
            )
        }

        StarGazeBottomBar(
            current = current,
            onSelect = { current = it },
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
        )

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(300)))
                    .togetherWith(slideOutVertically(tween(250)) { it / 8 } + fadeOut(tween(250)))
            },
            label = "destinationSheet",
        ) { dest ->
            when (dest) {
                Destination.SKY -> Unit
                Destination.TONIGHT -> TonightSheet(
                    onLocate = { skyViewModel.locate(it) },
                    onDismiss = { current = Destination.SKY },
                )
                Destination.GUIDE -> GuideSheet(onDismiss = { current = Destination.SKY })
                Destination.LEARN -> LearnSheet(onDismiss = { current = Destination.SKY })
                Destination.PRO -> ProSheet(
                    onDismiss = { current = Destination.SKY },
                    onOpenAiSettings = { overlay = Overlay.AI_SETTINGS },
                    onOpenLegal = { overlay = Overlay.LEGAL },
                )
            }
        }

        when (overlay) {
            Overlay.NONE -> Unit
            Overlay.AI_SETTINGS -> AiSettingsSheet(onDismiss = { overlay = Overlay.NONE })
            Overlay.LEGAL -> LegalSheet(onDismiss = { overlay = Overlay.NONE })
            Overlay.IDENTIFY -> com.stargaze.ai.camera.IdentifySkyOverlay(
                identifyState = uiState.identifyState,
                onDetections = skyViewModel::onFrameDetections,
                onBeginScan = skyViewModel::beginIdentify,
                onDismiss = { skyViewModel.cancelIdentify(); overlay = Overlay.NONE },
            )
        }

        // First-run AI & privacy disclosure — gates until the user chooses (no login).
        if (!aiState.disclosureAcknowledged) {
            AiConsentDialog(
                onChoice = { allowCloud -> aiSettingsViewModel.acknowledgeDisclosure(allowCloud) },
                onOpenPrivacy = { overlay = Overlay.LEGAL },
            )
        }
    }
}
