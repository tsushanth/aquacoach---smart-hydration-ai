package com.factory.aquacoachsmarthydrationai.ui.history

import app.cash.turbine.test
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumSource
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumState
import com.factory.aquacoachsmarthydrationai.data.local.DailyTotal
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import com.factory.aquacoachsmarthydrationai.testutil.MainDispatcherRule
import com.factory.aquacoachsmarthydrationai.util.DateUtils
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: HydrationRepository
    private lateinit var premiumManager: PremiumManager
    private val totalsFlow = MutableStateFlow<List<DailyTotal>>(emptyList())
    private val prefsFlow = MutableStateFlow(UserPreferences(dailyGoalMl = 2000))
    private val premiumFlow = MutableStateFlow(PremiumState())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)
        every { repository.observeDailyTotals(any(), any()) } returns totalsFlow
        every { repository.userPreferencesFlow } returns prefsFlow
        every { premiumManager.premiumStateFlow } returns premiumFlow
    }

    @Test
    fun `initial state has 7 days and reflects premium flag`() = runTest(mainDispatcherRule.testDispatcher) {
        premiumFlow.value = PremiumState(isPremium = true, source = PremiumSource.LIFETIME)
        val viewModel = HistoryViewModel(repository, premiumManager)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(7, state.days.size)
            assertTrue(state.isPremium)
        }
    }

    @Test
    fun `average is computed across the window including empty days`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = DateUtils.todayEpochDay()
        totalsFlow.value = listOf(
            DailyTotal(dayEpochDay = today, totalMl = 1400),
            DailyTotal(dayEpochDay = today - 1, totalMl = 700)
        )
        val viewModel = HistoryViewModel(repository, premiumManager)

        viewModel.uiState.test {
            val state = awaitItem()
            // 1400 + 700 spread across 7 days (5 empty) = 2100 / 7 = 300
            assertEquals(300, state.averageMl)
        }
    }

    @Test
    fun `streak counts consecutive goal-met days ending today, ignoring an unmet today`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val today = DateUtils.todayEpochDay()
            prefsFlow.value = UserPreferences(dailyGoalMl = 2000)
            totalsFlow.value = listOf(
                DailyTotal(dayEpochDay = today, totalMl = 0),
                DailyTotal(dayEpochDay = today - 1, totalMl = 2000),
                DailyTotal(dayEpochDay = today - 2, totalMl = 2500),
                DailyTotal(dayEpochDay = today - 3, totalMl = 100)
            )
            val viewModel = HistoryViewModel(repository, premiumManager)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(2, state.currentStreak)
            }
        }

    @Test
    fun `streak stops counting at the first missed day before today`() = runTest(mainDispatcherRule.testDispatcher) {
        val today = DateUtils.todayEpochDay()
        prefsFlow.value = UserPreferences(dailyGoalMl = 2000)
        totalsFlow.value = listOf(
            DailyTotal(dayEpochDay = today, totalMl = 2500),
            DailyTotal(dayEpochDay = today - 1, totalMl = 100)
        )
        val viewModel = HistoryViewModel(repository, premiumManager)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.currentStreak)
        }
    }

    @Test
    fun `non-premium users see isPremium false in state`() = runTest(mainDispatcherRule.testDispatcher) {
        premiumFlow.value = PremiumState(isPremium = false)
        val viewModel = HistoryViewModel(repository, premiumManager)

        viewModel.uiState.test {
            assertFalse(awaitItem().isPremium)
        }
    }
}
