package com.factory.aquacoachsmarthydrationai.data.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.premiumDataStore by preferencesDataStore(name = "aquacoach_premium")

enum class PremiumSource { NONE, SUBSCRIPTION, LIFETIME }

data class PremiumState(
    val isPremium: Boolean = false,
    val source: PremiumSource = PremiumSource.NONE,
    val isSupporter: Boolean = false
)

class PremiumManager(
    context: Context,
    private val billingManager: BillingManager,
    private val dataStore: DataStore<Preferences> = context.premiumDataStore
) {
    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val SOURCE = stringPreferencesKey("premium_source")
        val IS_SUPPORTER = booleanPreferencesKey("is_supporter")
    }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val premiumStateFlow: Flow<PremiumState> = dataStore.data.map { prefs ->
        PremiumState(
            isPremium = prefs[Keys.IS_PREMIUM] ?: false,
            source = prefs[Keys.SOURCE]?.let {
                runCatching { PremiumSource.valueOf(it) }.getOrDefault(PremiumSource.NONE)
            } ?: PremiumSource.NONE,
            isSupporter = prefs[Keys.IS_SUPPORTER] ?: false
        )
    }

    init {
        billingManager.purchaseEvents.onEach { event ->
            when (event) {
                is PurchaseEvent.Purchased -> applyPurchases(listOf(event.purchase))
                is PurchaseEvent.Restored -> applyPurchases(event.purchases, isFullSync = true)
                else -> Unit
            }
        }.launchIn(managerScope)
    }

    fun startObserving() {
        billingManager.startConnection()
    }

    suspend fun restorePurchases() {
        billingManager.refreshPurchases()
    }

    private suspend fun applyPurchases(purchases: List<Purchase>, isFullSync: Boolean = false) {
        val active = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        val hasLifetime = active.any { it.products.contains(PremiumProducts.LIFETIME.productId) }
        val hasSubscription = active.any { purchase ->
            purchase.products.any { id -> id in SUBSCRIPTION_PRODUCT_IDS }
        }
        val hasSupporterPack = active.any { it.products.contains(PremiumProducts.SMALL_IAP.productId) }

        dataStore.edit { prefs ->
            when {
                hasLifetime -> {
                    prefs[Keys.IS_PREMIUM] = true
                    prefs[Keys.SOURCE] = PremiumSource.LIFETIME.name
                }
                hasSubscription -> {
                    prefs[Keys.IS_PREMIUM] = true
                    prefs[Keys.SOURCE] = PremiumSource.SUBSCRIPTION.name
                }
                isFullSync -> {
                    // Only a full sync against Play's purchase list may revoke access —
                    // a single new-purchase event never implies everything else expired.
                    prefs[Keys.IS_PREMIUM] = false
                    prefs[Keys.SOURCE] = PremiumSource.NONE.name
                }
            }
            if (hasSupporterPack) {
                prefs[Keys.IS_SUPPORTER] = true
            }
        }
    }

    companion object {
        private val SUBSCRIPTION_PRODUCT_IDS = setOf(
            PremiumProducts.WEEKLY.productId,
            PremiumProducts.MONTHLY.productId,
            PremiumProducts.YEARLY.productId
        )
    }
}
