package com.factory.aquacoachsmarthydrationai.ui.paywall

import android.app.Activity
import app.cash.turbine.test
import com.android.billingclient.api.Purchase
import com.factory.aquacoachsmarthydrationai.data.billing.BillingConnectionState
import com.factory.aquacoachsmarthydrationai.data.billing.BillingManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumState
import com.factory.aquacoachsmarthydrationai.data.billing.PurchaseEvent
import com.factory.aquacoachsmarthydrationai.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var billingManager: BillingManager
    private lateinit var premiumManager: PremiumManager
    private val connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Connecting)
    private val productDetails = MutableStateFlow(emptyMap<String, com.android.billingclient.api.ProductDetails>())
    private val premiumFlow = MutableStateFlow(PremiumState())
    private val purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)

    @Before
    fun setup() {
        billingManager = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)
        every { billingManager.connectionState } returns connectionState
        every { billingManager.productDetails } returns productDetails
        every { billingManager.purchaseEvents } returns purchaseEvents
        every { premiumManager.premiumStateFlow } returns premiumFlow
        every { billingManager.startConnection() } answers { }
    }

    @Test
    fun `initial state mirrors billing connection and premium status`() = runTest(mainDispatcherRule.testDispatcher) {
        connectionState.value = BillingConnectionState.Connected
        premiumFlow.value = PremiumState(isPremium = true)
        val viewModel = PaywallViewModel(billingManager, premiumManager)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(BillingConnectionState.Connected, state.connectionState)
            assertTrue(state.isPremium)
            assertFalse(state.isPurchasing)
            assertNull(state.message)
        }
    }

    @Test
    fun `init starts the billing connection`() {
        PaywallViewModel(billingManager, premiumManager)

        verify { billingManager.startConnection() }
    }

    @Test
    fun `purchase sets isPurchasing and launches the billing flow`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)
        val activity = mockk<Activity>(relaxed = true)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.purchase(activity, "product-id")

            val purchasing = awaitItem()
            assertTrue(purchasing.isPurchasing)
        }
        verify { billingManager.launchPurchaseFlow(activity, "product-id") }
    }

    @Test
    fun `a Purchased event clears isPurchasing and sets a welcome message`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)
        val purchase = mockk<Purchase>(relaxed = true)

        viewModel.uiState.test {
            awaitItem() // initial

            purchaseEvents.emit(PurchaseEvent.Purchased(purchase))

            val updated = awaitItem()
            assertFalse(updated.isPurchasing)
            assertTrue(updated.message!!.contains("Premium"))
        }
    }

    @Test
    fun `a Pending event surfaces a pending message`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)
        val purchase = mockk<Purchase>(relaxed = true)

        viewModel.uiState.test {
            awaitItem()

            purchaseEvents.emit(PurchaseEvent.Pending(purchase))

            val updated = awaitItem()
            assertFalse(updated.isPurchasing)
            assertTrue(updated.message!!.contains("pending"))
        }
    }

    @Test
    fun `a Cancelled event clears isPurchasing without setting a message`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)
        val activity = mockk<Activity>(relaxed = true)

        viewModel.uiState.test {
            awaitItem()
            viewModel.purchase(activity, "product-id")
            assertTrue(awaitItem().isPurchasing)

            purchaseEvents.emit(PurchaseEvent.Cancelled)

            val updated = awaitItem()
            assertFalse(updated.isPurchasing)
            assertNull(updated.message)
        }
    }

    @Test
    fun `an Error event surfaces the error message`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)

        viewModel.uiState.test {
            awaitItem()

            purchaseEvents.emit(PurchaseEvent.Error("Something went wrong"))

            val updated = awaitItem()
            assertFalse(updated.isPurchasing)
            assertEquals("Something went wrong", updated.message)
        }
    }

    @Test
    fun `a Restored event with purchases reports purchases restored`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)
        val purchase = mockk<Purchase>(relaxed = true)

        viewModel.uiState.test {
            awaitItem()

            purchaseEvents.emit(PurchaseEvent.Restored(listOf(purchase)))

            assertEquals("Purchases restored.", awaitItem().message)
        }
    }

    @Test
    fun `a Restored event with no purchases reports none found`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)

        viewModel.uiState.test {
            awaitItem()

            purchaseEvents.emit(PurchaseEvent.Restored(emptyList()))

            assertEquals("No previous purchases found.", awaitItem().message)
        }
    }

    @Test
    fun `restorePurchases delegates to the premium manager`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { premiumManager.restorePurchases() } returns Unit
        val viewModel = PaywallViewModel(billingManager, premiumManager)

        viewModel.restorePurchases()

        coVerify { premiumManager.restorePurchases() }
    }

    @Test
    fun `retryConnection re-triggers the billing connection`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PaywallViewModel(billingManager, premiumManager)

        viewModel.retryConnection()

        verify(exactly = 2) { billingManager.startConnection() } // once on init, once on retry
    }
}
