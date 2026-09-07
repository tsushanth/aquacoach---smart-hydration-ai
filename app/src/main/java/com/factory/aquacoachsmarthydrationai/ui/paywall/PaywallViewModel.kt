package com.factory.aquacoachsmarthydrationai.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.factory.aquacoachsmarthydrationai.data.billing.BillingConnectionState
import com.factory.aquacoachsmarthydrationai.data.billing.BillingManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.billing.PurchaseEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaywallUiState(
    val connectionState: BillingConnectionState = BillingConnectionState.Connecting,
    val products: Map<String, ProductDetails> = emptyMap(),
    val isPremium: Boolean = false,
    val isPurchasing: Boolean = false,
    val message: String? = null
)

class PaywallViewModel(
    private val billingManager: BillingManager,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val isPurchasing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PaywallUiState> = combine(
        billingManager.connectionState,
        billingManager.productDetails,
        premiumManager.premiumStateFlow,
        isPurchasing,
        message
    ) { connection, products, premium, purchasing, msg ->
        PaywallUiState(
            connectionState = connection,
            products = products,
            isPremium = premium.isPremium,
            isPurchasing = purchasing,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaywallUiState()
    )

    init {
        billingManager.startConnection()
        viewModelScope.launch {
            billingManager.purchaseEvents.collect { event ->
                when (event) {
                    is PurchaseEvent.Purchased -> {
                        isPurchasing.value = false
                        message.value = "Welcome to Premium! 🎉"
                    }
                    is PurchaseEvent.Pending -> {
                        isPurchasing.value = false
                        message.value = "Your purchase is pending. Premium unlocks once it's confirmed."
                    }
                    PurchaseEvent.Cancelled -> {
                        isPurchasing.value = false
                    }
                    is PurchaseEvent.Error -> {
                        isPurchasing.value = false
                        message.value = event.message
                    }
                    is PurchaseEvent.Restored -> {
                        isPurchasing.value = false
                        message.value = if (event.purchases.isNotEmpty()) {
                            "Purchases restored."
                        } else {
                            "No previous purchases found."
                        }
                    }
                }
            }
        }
    }

    fun purchase(activity: Activity, productId: String) {
        isPurchasing.value = true
        message.value = null
        billingManager.launchPurchaseFlow(activity, productId)
    }

    fun restorePurchases() {
        viewModelScope.launch {
            isPurchasing.value = true
            premiumManager.restorePurchases()
        }
    }

    fun retryConnection() {
        billingManager.startConnection()
    }
}

class PaywallViewModelFactory(
    private val billingManager: BillingManager,
    private val premiumManager: PremiumManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PaywallViewModel(billingManager, premiumManager) as T
    }
}
