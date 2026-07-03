package com.stargaze.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stargaze.ai.data.SettingsRepository
import com.stargaze.ai.data.UserSettings
import com.stargaze.ai.ui.StarGazeRoot
import com.stargaze.ai.ui.theme.StarColors
import com.stargaze.ai.ui.theme.StarGazeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single-activity host. All UI is Compose; navigation is internal. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = UserSettings())
            StarGazeTheme(
                highContrast = settings.highContrast,
                textScale = settings.textScale,
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = StarColors.Bg) {
                    StarGazeRoot()
                }
            }
        }
    }
}
