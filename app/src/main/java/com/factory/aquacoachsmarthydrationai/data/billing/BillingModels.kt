package com.factory.aquacoachsmarthydrationai.data.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase

private const val BASE_ID = "com.factory.aquacoachsmarthydrationai"

data class PremiumProduct(
    val productId: String,
    val billingProductType: String,
    val displayName: String,
    val fallbackPriceLabel: String
)

/**
 * Weekly/monthly are defined for id-scheme completeness but have no assigned price yet
 * and are not surfaced on [com.factory.aquacoachsmarthydrationai.ui.paywall.PaywallScreen];
 * only yearly, lifetime and the small IAP were priced in the product spec.
 */
object PremiumProducts {
    val WEEKLY = PremiumProduct(
        productId = "$BASE_ID.subscription.weekly",
        billingProductType = BillingClient.ProductType.SUBS,
        displayName = "Weekly",
        fallbackPriceLabel = "N/A"
    )
    val MONTHLY = PremiumProduct(
        productId = "$BASE_ID.subscription.monthly",
        billingProductType = BillingClient.ProductType.SUBS,
        displayName = "Monthly",
        fallbackPriceLabel = "N/A"
    )
    val YEARLY = PremiumProduct(
        productId = "$BASE_ID.subscription.yearly",
        billingProductType = BillingClient.ProductType.SUBS,
        displayName = "Yearly",
        fallbackPriceLabel = "$31.99/year"
    )
    val LIFETIME = PremiumProduct(
        productId = "$BASE_ID.subscription.lifetime",
        billingProductType = BillingClient.ProductType.INAPP,
        displayName = "Lifetime",
        fallbackPriceLabel = "$63.98"
    )
    val SMALL_IAP = PremiumProduct(
        productId = "$BASE_ID.small_iap",
        billingProductType = BillingClient.ProductType.INAPP,
        displayName = "Support Pack",
        fallbackPriceLabel = "$2.24"
    )

    val ALL = listOf(WEEKLY, MONTHLY, YEARLY, LIFETIME, SMALL_IAP)
}

sealed interface BillingConnectionState {
    data object Disconnected : BillingConnectionState
    data object Connecting : BillingConnectionState
    data object Connected : BillingConnectionState
    data class Error(val message: String) : BillingConnectionState
}

sealed interface PurchaseEvent {
    data class Purchased(val purchase: Purchase) : PurchaseEvent
    data class Pending(val purchase: Purchase) : PurchaseEvent
    data class Restored(val purchases: List<Purchase>) : PurchaseEvent
    data object Cancelled : PurchaseEvent
    data class Error(val message: String) : PurchaseEvent
}
