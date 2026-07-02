package com.stargaze.ai.ui.pro

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargaze.ai.billing.BillingRepository
import com.stargaze.ai.billing.ProProducts
import com.stargaze.ai.billing.PurchaseState
import com.stargaze.ai.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingRepository.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val products = billingRepository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseState: StateFlow<PurchaseState> = billingRepository.purchaseState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PurchaseState.Idle)

    fun purchase(activity: Activity, productId: String) = billingRepository.purchase(activity, productId)

    fun restorePurchases() = billingRepository.restorePurchases()

    fun resetPurchaseState() = billingRepository.resetPurchaseState()

    /** For testing / support only: resets entitlement locally without a refund flow. */
    fun revokePro() = viewModelScope.launch { settingsRepository.setPro(false) }

    companion object {
        val PRO_PRODUCT_IDS = listOf(ProProducts.ANNUAL, ProProducts.MONTHLY, ProProducts.LIFETIME)
    }
}
