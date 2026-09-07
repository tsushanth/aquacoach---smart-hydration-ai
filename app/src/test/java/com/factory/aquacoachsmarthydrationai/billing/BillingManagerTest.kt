package com.factory.aquacoachsmarthydrationai.billing

import android.app.Activity
import android.content.Context
import app.cash.turbine.test
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.factory.aquacoachsmarthydrationai.data.billing.BillingConnectionState
import com.factory.aquacoachsmarthydrationai.data.billing.BillingManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumProducts
import com.factory.aquacoachsmarthydrationai.data.billing.PurchaseEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerTest {

    @get:Rule
    val mainDispatcherRule = com.factory.aquacoachsmarthydrationai.testutil.MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var billingClient: BillingClient
    private lateinit var billingManager: BillingManager

    private fun billingResult(code: Int = BillingClient.BillingResponseCode.OK, message: String = "") =
        BillingResult.newBuilder().setResponseCode(code).setDebugMessage(message).build()

    private fun productDetails(productId: String, hasOffer: Boolean = true): ProductDetails {
        val details = mockk<ProductDetails>(relaxed = true)
        every { details.productId } returns productId
        every { details.productType } returns BillingClient.ProductType.SUBS
        if (hasOffer) {
            val offer = mockk<ProductDetails.SubscriptionOfferDetails>(relaxed = true)
            every { offer.offerToken } returns "offer-token"
            every { details.subscriptionOfferDetails } returns listOf(offer)
        } else {
            every { details.subscriptionOfferDetails } returns null
        }
        return details
    }

    /** Makes the mocked client immediately answer queryProductDetails with an empty, OK result. */
    private fun stubEmptyProductDetails() {
        every { billingClient.queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>().onProductDetailsResponse(billingResult(), emptyList())
        }
    }

    /** Makes the mocked client immediately answer queryPurchasesAsync with no purchases for any product type. */
    private fun stubEmptyPurchases() {
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(billingResult(), emptyList())
        }
    }

    @Before
    fun setup() {
        // The Play Billing library's BillingFlowParams.Builder always builds a default
        // SubscriptionUpdateParams internally (even when no old-SKU replacement is requested),
        // and that build() validates its fields via android.text.TextUtils.isEmpty(). Because
        // this is a plain JVM unit test (isReturnDefaultValues=true), that unmocked framework
        // call would otherwise always return false and trip a spurious IllegalArgumentException.
        mockkStatic(android.text.TextUtils::class)
        every { android.text.TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }

        context = mockk(relaxed = true)
        billingClient = mockk(relaxed = true)
        billingManager = BillingManager(context) { billingClient }
    }

    @After
    fun teardown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `startConnection transitions to Connected and loads products on success`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns false
        val details = productDetails(PremiumProducts.YEARLY.productId)
        every { billingClient.queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>().onProductDetailsResponse(billingResult(), listOf(details))
        }
        stubEmptyPurchases()

        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }

        billingManager.connectionState.test {
            assertEquals(BillingConnectionState.Disconnected, awaitItem())

            billingManager.startConnection()
            assertEquals(BillingConnectionState.Connecting, awaitItem())

            listenerSlot.captured.onBillingSetupFinished(billingResult())
            assertEquals(BillingConnectionState.Connected, awaitItem())

            advanceUntilIdle()
        }

        assertEquals(details, billingManager.productDetails.value[PremiumProducts.YEARLY.productId])
    }

    @Test
    fun `startConnection surfaces an Error state when setup fails`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns false
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }

        billingManager.connectionState.test {
            assertEquals(BillingConnectionState.Disconnected, awaitItem())

            billingManager.startConnection()
            assertEquals(BillingConnectionState.Connecting, awaitItem())

            listenerSlot.captured.onBillingSetupFinished(
                billingResult(BillingClient.BillingResponseCode.ERROR, "boom")
            )
            assertEquals(BillingConnectionState.Error("boom"), awaitItem())
        }
    }

    @Test
    fun `startConnection reuses an already-ready client without reconnecting`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns true
        stubEmptyProductDetails()
        stubEmptyPurchases()

        billingManager.startConnection()
        advanceUntilIdle()

        verify(exactly = 0) { billingClient.startConnection(any()) }
        verify { billingClient.queryProductDetailsAsync(any(), any()) }
    }

    @Test
    fun `refreshPurchases emits Restored with purchases from both product types`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns true
        val subPurchase = mockk<Purchase>(relaxed = true)
        every { subPurchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { subPurchase.isAcknowledged } returns true
        every { subPurchase.products } returns listOf(PremiumProducts.YEARLY.productId)

        val inAppPurchase = mockk<Purchase>(relaxed = true)
        every { inAppPurchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { inAppPurchase.isAcknowledged } returns true
        every { inAppPurchase.products } returns listOf(PremiumProducts.LIFETIME.productId)

        // refreshPurchases() queries SUBS then INAPP sequentially; distinguish by call order
        // since QueryPurchasesParams exposes no public product-type getter to match against.
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(billingResult(), listOf(subPurchase))
        } andThenAnswer {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(billingResult(), listOf(inAppPurchase))
        }

        billingManager.purchaseEvents.test {
            billingManager.refreshPurchases()
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Restored)
            val restored = event as PurchaseEvent.Restored
            assertEquals(2, restored.purchases.size)
        }
    }

    @Test
    fun `refreshPurchases emits an Error event when the query throws`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns true
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } throws RuntimeException("network down")

        billingManager.purchaseEvents.test {
            billingManager.refreshPurchases()
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Error)
        }
    }

    @Test
    fun `refreshPurchases does nothing when the client is not ready`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns false

        billingManager.refreshPurchases()
        advanceUntilIdle()

        verify(exactly = 0) { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }
    }

    @Test
    fun `launchPurchaseFlow emits an Error when product details are unavailable`() = runTest(mainDispatcherRule.testDispatcher) {
        val activity = mockk<Activity>(relaxed = true)

        billingManager.purchaseEvents.test {
            billingManager.launchPurchaseFlow(activity, PremiumProducts.YEARLY.productId)
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Error)
        }
    }

    @Test
    fun `launchPurchaseFlow emits an Error when a subscription has no offer token`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns false
        val details = productDetails(PremiumProducts.YEARLY.productId, hasOffer = false)
        every { billingClient.queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>().onProductDetailsResponse(billingResult(), listOf(details))
        }
        stubEmptyPurchases()
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }

        billingManager.startConnection()
        listenerSlot.captured.onBillingSetupFinished(billingResult())
        advanceUntilIdle()

        val activity = mockk<Activity>(relaxed = true)
        billingManager.purchaseEvents.test {
            billingManager.launchPurchaseFlow(activity, PremiumProducts.YEARLY.productId)
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Error)
        }
    }

    @Test
    fun `launchPurchaseFlow starts the billing flow when a product and offer are available`() = runTest(mainDispatcherRule.testDispatcher) {
        every { billingClient.isReady } returns false
        val details = productDetails(PremiumProducts.YEARLY.productId)
        every { billingClient.queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>().onProductDetailsResponse(billingResult(), listOf(details))
        }
        stubEmptyPurchases()
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers { }
        every { billingClient.launchBillingFlow(any(), any()) } returns billingResult()

        billingManager.startConnection()
        listenerSlot.captured.onBillingSetupFinished(billingResult())
        advanceUntilIdle()

        val activity = mockk<Activity>(relaxed = true)
        billingManager.launchPurchaseFlow(activity, PremiumProducts.YEARLY.productId)

        val flowParamsSlot = slot<BillingFlowParams>()
        verify { billingClient.launchBillingFlow(activity, capture(flowParamsSlot)) }
    }

    @Test
    fun `onPurchasesUpdated acknowledges a new purchase and emits Purchased`() = runTest(mainDispatcherRule.testDispatcher) {
        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.isAcknowledged } returns false
        every { purchase.products } returns listOf(PremiumProducts.YEARLY.productId)
        every { purchase.purchaseToken } returns "token-123"

        every { billingClient.acknowledgePurchase(any(), any()) } answers {
            secondArg<AcknowledgePurchaseResponseListener>().onAcknowledgePurchaseResponse(billingResult())
        }

        billingManager.purchaseEvents.test {
            (billingManager as PurchasesUpdatedListener).onPurchasesUpdated(billingResult(), mutableListOf(purchase))
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Purchased)
        }
        verify { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any()) }
    }

    @Test
    fun `onPurchasesUpdated consumes the small IAP instead of acknowledging`() = runTest(mainDispatcherRule.testDispatcher) {
        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.isAcknowledged } returns false
        every { purchase.products } returns listOf(PremiumProducts.SMALL_IAP.productId)
        every { purchase.purchaseToken } returns "token-456"

        every { billingClient.consumeAsync(any(), any()) } answers {
            secondArg<ConsumeResponseListener>().onConsumeResponse(billingResult(), "token-456")
        }

        billingManager.purchaseEvents.test {
            (billingManager as PurchasesUpdatedListener).onPurchasesUpdated(billingResult(), mutableListOf(purchase))
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Purchased)
        }
        verify { billingClient.consumeAsync(any<ConsumeParams>(), any()) }
        verify(exactly = 0) { billingClient.acknowledgePurchase(any(), any()) }
    }

    @Test
    fun `onPurchasesUpdated emits Pending for a pending purchase`() = runTest(mainDispatcherRule.testDispatcher) {
        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.purchaseState } returns Purchase.PurchaseState.PENDING

        billingManager.purchaseEvents.test {
            (billingManager as PurchasesUpdatedListener).onPurchasesUpdated(billingResult(), mutableListOf(purchase))
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Pending)
        }
    }

    @Test
    fun `onPurchasesUpdated emits Cancelled on user cancellation`() = runTest(mainDispatcherRule.testDispatcher) {
        billingManager.purchaseEvents.test {
            (billingManager as PurchasesUpdatedListener).onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.USER_CANCELED), null
            )
            assertEquals(PurchaseEvent.Cancelled, awaitItem())
        }
    }

    @Test
    fun `onPurchasesUpdated emits a network Error on service disconnects`() = runTest(mainDispatcherRule.testDispatcher) {
        billingManager.purchaseEvents.test {
            (billingManager as PurchasesUpdatedListener).onPurchasesUpdated(
                billingResult(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED), null
            )
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Error)
        }
    }

    @Test
    fun `onPurchasesUpdated emits Error when OK response has no purchases`() = runTest(mainDispatcherRule.testDispatcher) {
        billingManager.purchaseEvents.test {
            (billingManager as PurchasesUpdatedListener).onPurchasesUpdated(billingResult(), mutableListOf())
            val event = awaitItem()
            assertTrue(event is PurchaseEvent.Error)
        }
    }

    @Test
    fun `endConnection delegates to the underlying billing client`() {
        billingManager.endConnection()
        verify { billingClient.endConnection() }
    }
}
