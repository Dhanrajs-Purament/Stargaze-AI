package com.stargaze.ai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.ui.onboarding.OnboardingScreen
import com.stargaze.ai.ui.onboarding.OnboardingViewModel

/** Top-level destinations shown in the bottom navigation bar. */
enum class Destination(val label: String, val icon: ImageVector) {
    SKY("Sky", Icons.Filled.Home),
    TONIGHT("Tonight", Icons.Filled.WbTwilight),
    GUIDE("Guide", Icons.Filled.AutoAwesome),
    LEARN("Learn", Icons.Filled.School),
    PRO("Pro", Icons.Filled.WorkspacePremium),
}

/**
 * Application root: shows onboarding until complete, then the main shell with bottom navigation.
 * The Sky screen is always the persistent background; other destinations open as overlays/sheets so
 * the live sky keeps animating behind them (matching the prototype's single-surface feel).
 */
@Composable
fun StarGazeRoot(onboardingViewModel: OnboardingViewModel = hiltViewModel()) {
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (onboardingComplete) {
            MainShell()
        } else {
            OnboardingScreen(onFinished = onboardingViewModel::completeOnboarding)
        }
    }
}
