package com.factory.aquacoachsmarthydrationai.ui.home

import app.cash.turbine.test
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntry
import com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import com.factory.aquacoachsmarthydrationai.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: HydrationRepository
    private val totalFlow = MutableStateFlow(0)
    private val prefsFlow = MutableStateFlow(UserPreferences())
    private val entriesFlow = MutableStateFlow<List<WaterEntry>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.observeTodayTotal() } returns totalFlow
        every { repository.userPreferencesFlow } returns prefsFlow
        every { repository.observeTodayEntries() } returns entriesFlow
    }

    @Test
    fun `initial state combines total, preferences and entries`() = runTest(mainDispatcherRule.testDispatcher) {
        totalFlow.value = 750
        prefsFlow.value = UserPreferences(dailyGoalMl = 2500, unitSystem = UnitSystem.OZ)
        val entry = WaterEntry(id = 1, amountMl = 750, timestampEpochMillis = 1, dayEpochDay = 1)
        entriesFlow.value = listOf(entry)

        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(750, state.currentMl)
            assertEquals(2500, state.goalMl)
            assertEquals(UnitSystem.OZ, state.unitSystem)
            assertEquals(listOf(entry), state.todayEntries)
        }
    }

    @Test
    fun `uiState reflects updates emitted by the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = HomeViewModel(repository)

        viewModel.uiState.test {
            assertEquals(0, awaitItem().currentMl)

            totalFlow.value = 400
            assertEquals(400, awaitItem().currentMl)

            prefsFlow.value = UserPreferences(dailyGoalMl = 3000)
            assertEquals(3000, awaitItem().goalMl)
        }
    }

    @Test
    fun `logWater calls the repository with the given amount`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.logWater(any()) } returns Unit
        val viewModel = HomeViewModel(repository)

        viewModel.logWater(500)

        // `any()` for the timestamp: it defaults to System.currentTimeMillis() at the call site,
        // so asserting an exact value here would race the same default evaluated again.
        coVerify { repository.logWater(500, any()) }
    }

    @Test
    fun `deleteEntry calls the repository with the given entry`() = runTest(mainDispatcherRule.testDispatcher) {
        val entry = WaterEntry(id = 2, amountMl = 100, timestampEpochMillis = 1, dayEpochDay = 1)
        coEvery { repository.deleteEntry(entry) } returns Unit
        val viewModel = HomeViewModel(repository)

        viewModel.deleteEntry(entry)

        coVerify { repository.deleteEntry(entry) }
    }
}
