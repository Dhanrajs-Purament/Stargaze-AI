package com.stargaze.ai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargaze.ai.data.LocationRepository
import com.stargaze.ai.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    val onboardingComplete: StateFlow<Boolean> = settingsRepository.settings
        .map { it.onboardingComplete }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun completeOnboarding() = viewModelScope.launch {
        locationRepository.refresh()
        settingsRepository.setOnboardingComplete(true)
    }
}
