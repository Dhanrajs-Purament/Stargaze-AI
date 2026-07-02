package com.stargaze.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point; bootstraps Hilt dependency injection. */
@HiltAndroidApp
class StarGazeApp : Application()
