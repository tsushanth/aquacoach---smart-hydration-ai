package com.factory.aquacoachsmarthydrationai.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "BillingManager"

class BillingManager(
    context: Context,
    billingClientFactory: (PurchasesUpdatedListener) -> BillingClient = { listener ->
        @Suppress("DEPRECATION")
        BillingClient.newBuilder(context.applicationContext)
            .setListener(listener)
            .enablePendingPurchases()
            .build()
    }
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val billingClient: BillingClient = billingClientFactory(this)

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    fun startConnection() {
        if (billingClient.isReady) {
            managerScope.launch {
                queryProductDetails()
                refreshPurchases()
            }
            return
        }
        _connectionState.value = BillingConnectionState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.Connected
                    managerScope.launch {
                        queryProductDetails()
                        refreshPurchases()
                    }
                } else {
                    _connectionState.value = BillingConnectionState.Error(
                        result.debugMessage.ifBlank { "Unable to connect to Google Play." }
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.Disconnected
            }
        })
    }

    private suspend fun queryProductDetails() {
        // The Play Billing Library requires every product in a single query to share the same
        // ProductType, so SUBS and INAPP products must be queried separately and merged.
        try {
            val allDetails = mutableMapOf<String, ProductDetails>()
            PremiumProducts.ALL.groupBy { it.billingProductType }.forEach { (_, products) ->
                val productList = products.map { product ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(product.productId)
                        .setProductType(product.billingProductType)
                        .build()
                }
                val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
                val result = billingClient.queryProductDetails(params)
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    result.productDetailsList.orEmpty().forEach { allDetails[it.productId] = it }
                } else {
                    Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
                }
            }
            _productDetails.value = allDetails
        } catch (e: Exception) {
            Log.e(TAG, "queryProductDetails error", e)
        }
    }

    suspend fun refreshPurchases() {
        if (!billingClient.isReady) return
        try {
            val subs = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            )
            val inApp = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            )
            val purchases = subs.purchasesList + inApp.purchasesList
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    acknowledgeOrConsume(purchase)
                }
            }
            _purchaseEvents.emit(PurchaseEvent.Restored(purchases))
        } catch (e: Exception) {
            Log.e(TAG, "refreshPurchases error", e)
            _purchaseEvents.emit(PurchaseEvent.Error("No network connection. Please try again."))
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = _productDetails.value[productId]
        if (details == null) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error("Product unavailable. Check your connection and try again."))
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                _purchaseEvents.tryEmit(PurchaseEvent.Error("No subscription offer is currently available."))
                return
            }
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseEvents.tryEmit(PurchaseEvent.Error(result.debugMessage.ifBlank { "Unable to start purchase." }))
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val updated = purchases.orEmpty()
                if (updated.isEmpty()) {
                    _purchaseEvents.tryEmit(PurchaseEvent.Error("No purchase data returned."))
                } else {
                    updated.forEach(::handlePurchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseEvents.tryEmit(PurchaseEvent.Cancelled)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                managerScope.launch { refreshPurchases() }
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                _purchaseEvents.tryEmit(PurchaseEvent.Error("No network connection. Please try again."))
            }
            else -> {
                _purchaseEvents.tryEmit(
                    PurchaseEvent.Error(billingResult.debugMessage.ifBlank { "Purchase failed." })
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                managerScope.launch {
                    if (!purchase.isAcknowledged) {
                        acknowledgeOrConsume(purchase)
                    }
                    _purchaseEvents.emit(PurchaseEvent.Purchased(purchase))
                }
            }
            Purchase.PurchaseState.PENDING -> {
                _purchaseEvents.tryEmit(PurchaseEvent.Pending(purchase))
            }
            else -> Unit
        }
    }

    private suspend fun acknowledgeOrConsume(purchase: Purchase) {
        try {
            if (purchase.products.contains(PremiumProducts.SMALL_IAP.productId)) {
                val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                billingClient.consumePurchase(params)
            } else {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acknowledge/consume purchase", e)
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
