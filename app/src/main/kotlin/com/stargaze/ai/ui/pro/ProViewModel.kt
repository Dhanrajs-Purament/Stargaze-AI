package com.stargaze.ai.ui.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargaze.ai.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = settingsRepository.settings
        .map { it.isPro }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Activates Pro. In production this is the success callback of a Google Play Billing purchase
     * flow (or Telegram Stars on the mini-app surface). The billing client wiring is intentionally
     * the only integration point left for the store SDK; entitlement state lives here.
     */
    fun activatePro() = viewModelScope.launch {
        settingsRepository.setPro(true)
    }
}
