package com.factory.aquacoachsmarthydrationai.billing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.android.billingclient.api.Purchase
import com.factory.aquacoachsmarthydrationai.data.billing.BillingManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumProducts
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumSource
import com.factory.aquacoachsmarthydrationai.data.billing.PurchaseEvent
import com.factory.aquacoachsmarthydrationai.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Each test wires its manager(s) to a fresh temp-file-backed DataStore (via [newManager]) instead
 * of the app-wide `premiumDataStore` singleton, so tests can't leak state into one another
 * regardless of JUnit's execution order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PremiumManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun purchase(vararg productIds: String): Purchase {
        val p = mockk<Purchase>(relaxed = true)
        every { p.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { p.products } returns productIds.toList()
        return p
    }

    private fun TestScope.newManager(
        purchaseEvents: MutableSharedFlow<PurchaseEvent>
    ): Pair<PremiumManager, BillingManager> {
        val billingManager = mockk<BillingManager>(relaxed = true)
        every { billingManager.purchaseEvents } returns purchaseEvents
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File.createTempFile("premium_test_", ".preferences_pb").apply { deleteOnExit() }
        }
        return PremiumManager(mockk<Context>(relaxed = true), billingManager, dataStore) to billingManager
    }

    @Test
    fun `initial state has no premium access`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, _) = newManager(events)

        manager.premiumStateFlow.test {
            val state = awaitItem()
            assertFalse(state.isPremium)
            assertEquals(PremiumSource.NONE, state.source)
            assertFalse(state.isSupporter)
        }
    }

    @Test
    fun `a lifetime purchase grants premium with LIFETIME source`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, _) = newManager(events)

        manager.premiumStateFlow.test {
            assertFalse(awaitItem().isPremium)

            events.emit(PurchaseEvent.Purchased(purchase(PremiumProducts.LIFETIME.productId)))

            val updated = awaitItem()
            assertTrue(updated.isPremium)
            assertEquals(PremiumSource.LIFETIME, updated.source)
        }
    }

    @Test
    fun `a subscription purchase grants premium with SUBSCRIPTION source`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, _) = newManager(events)

        manager.premiumStateFlow.test {
            assertFalse(awaitItem().isPremium)

            events.emit(PurchaseEvent.Purchased(purchase(PremiumProducts.YEARLY.productId)))

            val updated = awaitItem()
            assertTrue(updated.isPremium)
            assertEquals(PremiumSource.SUBSCRIPTION, updated.source)
        }
    }

    @Test
    fun `a supporter pack purchase sets isSupporter without granting premium`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, _) = newManager(events)

        manager.premiumStateFlow.test {
            assertFalse(awaitItem().isPremium)

            events.emit(PurchaseEvent.Purchased(purchase(PremiumProducts.SMALL_IAP.productId)))

            val updated = awaitItem()
            assertTrue(updated.isSupporter)
            assertFalse(updated.isPremium)
        }
    }

    @Test
    fun `a full sync restore with no active purchases revokes premium`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, _) = newManager(events)

        manager.premiumStateFlow.test {
            assertFalse(awaitItem().isPremium)

            events.emit(PurchaseEvent.Purchased(purchase(PremiumProducts.YEARLY.productId)))
            assertTrue(awaitItem().isPremium)

            events.emit(PurchaseEvent.Restored(emptyList()))

            val revoked = awaitItem()
            assertFalse(revoked.isPremium)
            assertEquals(PremiumSource.NONE, revoked.source)
        }
    }

    @Test
    fun `a single non-full-sync purchase event never revokes existing premium`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, _) = newManager(events)

        manager.premiumStateFlow.test {
            assertFalse(awaitItem().isPremium)

            events.emit(PurchaseEvent.Purchased(purchase(PremiumProducts.LIFETIME.productId)))
            assertTrue(awaitItem().isPremium)

            // An unrelated single purchase event (not a full restore sync) must not revoke access.
            events.emit(PurchaseEvent.Purchased(purchase(PremiumProducts.SMALL_IAP.productId)))

            val afterSupporterPurchase = awaitItem()
            assertTrue(afterSupporterPurchase.isPremium)
            assertTrue(afterSupporterPurchase.isSupporter)
        }
    }

    @Test
    fun `premium status persists across manager instances sharing the same data store`() = runTest(mainDispatcherRule.testDispatcher) {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File.createTempFile("premium_test_", ".preferences_pb").apply { deleteOnExit() }
        }

        val billingManager1 = mockk<BillingManager>(relaxed = true)
        val events1 = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        every { billingManager1.purchaseEvents } returns events1
        val manager1 = PremiumManager(mockk<Context>(relaxed = true), billingManager1, dataStore)

        manager1.premiumStateFlow.test {
            assertFalse(awaitItem().isPremium)
            events1.emit(PurchaseEvent.Purchased(purchase(PremiumProducts.LIFETIME.productId)))
            assertTrue(awaitItem().isPremium)
        }

        val billingManager2 = mockk<BillingManager>(relaxed = true)
        every { billingManager2.purchaseEvents } returns MutableSharedFlow(extraBufferCapacity = 4)
        val manager2 = PremiumManager(mockk<Context>(relaxed = true), billingManager2, dataStore)

        manager2.premiumStateFlow.test {
            val persisted = awaitItem()
            assertTrue(persisted.isPremium)
            assertEquals(PremiumSource.LIFETIME, persisted.source)
        }
    }

    @Test
    fun `startObserving delegates to the billing manager connection`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, billingManager) = newManager(events)

        manager.startObserving()

        verify { billingManager.startConnection() }
    }

    @Test
    fun `restorePurchases delegates to the billing manager refresh`() = runTest(mainDispatcherRule.testDispatcher) {
        val events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
        val (manager, billingManager) = newManager(events)
        coEvery { billingManager.refreshPurchases() } returns Unit

        manager.restorePurchases()

        coVerify { billingManager.refreshPurchases() }
    }
}
