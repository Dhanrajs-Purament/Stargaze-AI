package com.stargaze.ai.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.stargaze.ai.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Local product identifiers configured in Google Play Console. */
object ProProducts {
    const val ANNUAL = "stargaze_pro_annual"
    const val MONTHLY = "stargaze_pro_monthly"
    const val LIFETIME = "stargaze_pro_lifetime"

    val ALL = listOf(ANNUAL, MONTHLY, LIFETIME)
}

/** A normalised view of a Pro SKU returned by Google Play. */
data class ProSku(
    val productId: String,
    val name: String,
    val description: String,
    val formattedPrice: String,
    val productDetails: ProductDetails,
    val isSubscription: Boolean,
)

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Loading : PurchaseState
    data object Purchased : PurchaseState
    data class Error(val message: String) : PurchaseState
}

/** Real Google Play Billing integration for the Pro paywall. */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val _products = MutableStateFlow<List<ProSku>>(emptyList())
    val products: StateFlow<List<ProSku>> = _products.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob())

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else {
            _purchaseState.value = PurchaseState.Error(billingResult.debugMessage.ifBlank { "Purchase failed" })
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        scope.launch {
            settingsRepository.settings.first().let { _isPro.value = it.isPro }
            settingsRepository.settings.collect { _isPro.value = it.isPro }
        }
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() = startConnection()
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    restorePurchases()
                }
            }
        })
    }

    private fun loadProducts() {
        val productList = ProProducts.ALL.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(
                    if (id == ProProducts.LIFETIME) {
                        BillingClient.ProductType.INAPP
                    } else {
                        BillingClient.ProductType.SUBS
                    },
                )
                .build()
        }
        scope.launch {
            val result = billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(productList).build(),
            )
            result.productDetailsList?.let { details ->
                _products.value = details.mapNotNull { mapProductDetails(it) }
            }
        }
    }

    private fun mapProductDetails(details: ProductDetails): ProSku? {
        val offer = details.subscriptionOfferDetails?.firstOrNull()
        val oneTime = details.oneTimePurchaseOfferDetails
        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            ?: oneTime?.formattedPrice
            ?: return null
        return ProSku(
            productId = details.productId,
            name = details.name,
            description = details.description,
            formattedPrice = price,
            productDetails = details,
            isSubscription = details.productType == BillingClient.ProductType.SUBS,
        )
    }

    /** Launch the Google Play billing flow for the given product id. */
    fun purchase(activity: Activity, productId: String) {
        val sku = _products.value.firstOrNull { it.productId == productId }
        if (sku == null) {
            _purchaseState.value = PurchaseState.Error("Product not available")
            return
        }
        _purchaseState.value = PurchaseState.Loading
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(sku.productDetails)
            .apply {
                if (sku.isSubscription) {
                    sku.productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { setOfferToken(it) }
                }
            }
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    /** Restore previous purchases and update entitlement state. */
    fun restorePurchases() {
        scope.launch {
            listOf(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            ).forEach { params ->
                runCatching { billingClient.queryPurchasesAsync(params) }
                    .onSuccess { handlePurchases(it.purchasesList) }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var proPurchased = false
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                proPurchased = true
                if (!purchase.isAcknowledged) {
                    scope.launch {
                        runCatching {
                            billingClient.acknowledgePurchase(
                                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                            )
                        }
                    }
                }
            }
        }
        if (proPurchased) {
            scope.launch { settingsRepository.setPro(true) }
            _purchaseState.value = PurchaseState.Purchased
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    /** Call from Application/Activity to avoid leaking the coroutine scope. */
    fun close() = scope.cancel()
}
