package com.factory.aquacoachsmarthydrationai.ui.settings

import android.content.Context
import app.cash.turbine.test
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumState
import com.factory.aquacoachsmarthydrationai.data.preferences.ActivityLevel
import com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import com.factory.aquacoachsmarthydrationai.notification.ReminderScheduler
import com.factory.aquacoachsmarthydrationai.testutil.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: HydrationRepository
    private lateinit var premiumManager: PremiumManager
    private lateinit var context: Context
    private val prefsFlow = MutableStateFlow(UserPreferences())
    private val premiumFlow = MutableStateFlow(PremiumState())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        premiumManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { repository.userPreferencesFlow } returns prefsFlow
        every { premiumManager.premiumStateFlow } returns premiumFlow
        mockkObject(ReminderScheduler)
        every { ReminderScheduler.scheduleReminders(any(), any()) } just Runs
        every { ReminderScheduler.cancelReminders(any()) } just Runs
    }

    @After
    fun teardown() {
        unmockkObject(ReminderScheduler)
    }

    private fun newViewModel() = SettingsViewModel(repository, context, premiumManager)

    @Test
    fun `initial state combines preferences and premium status`() = runTest(mainDispatcherRule.testDispatcher) {
        prefsFlow.value = UserPreferences(dailyGoalMl = 2200)
        premiumFlow.value = PremiumState(isPremium = true)
        val viewModel = newViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2200, state.preferences.dailyGoalMl)
            assertTrue(state.premiumState.isPremium)
        }
    }

    @Test
    fun `setDailyGoalMl delegates to the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.setDailyGoalMl(any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.setDailyGoalMl(2800)

        coVerify { repository.setDailyGoalMl(2800) }
    }

    @Test
    fun `setUnitSystem delegates to the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.setUnitSystem(any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.setUnitSystem(UnitSystem.OZ)

        coVerify { repository.setUnitSystem(UnitSystem.OZ) }
    }

    @Test
    fun `enabling reminders persists the flag and schedules work`() = runTest(mainDispatcherRule.testDispatcher) {
        prefsFlow.value = UserPreferences(reminderIntervalMinutes = 60)
        coEvery { repository.setRemindersEnabled(true) } returns Unit
        val viewModel = newViewModel()

        viewModel.setRemindersEnabled(true)

        coVerify { repository.setRemindersEnabled(true) }
        verify { ReminderScheduler.scheduleReminders(context, 60) }
    }

    @Test
    fun `disabling reminders persists the flag and cancels work`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.setRemindersEnabled(false) } returns Unit
        val viewModel = newViewModel()

        viewModel.setRemindersEnabled(false)

        coVerify { repository.setRemindersEnabled(false) }
        verify { ReminderScheduler.cancelReminders(context) }
        verify(exactly = 0) { ReminderScheduler.scheduleReminders(any(), any()) }
    }

    @Test
    fun `setReminderIntervalMinutes reschedules only when reminders are enabled`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prefsFlow.value = UserPreferences(remindersEnabled = true)
            coEvery { repository.setReminderIntervalMinutes(any()) } returns Unit
            val viewModel = newViewModel()

            viewModel.setReminderIntervalMinutes(45)

            coVerify { repository.setReminderIntervalMinutes(45) }
            verify { ReminderScheduler.scheduleReminders(context, 45) }
        }

    @Test
    fun `setReminderIntervalMinutes does not reschedule when reminders are disabled`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prefsFlow.value = UserPreferences(remindersEnabled = false)
            coEvery { repository.setReminderIntervalMinutes(any()) } returns Unit
            val viewModel = newViewModel()

            viewModel.setReminderIntervalMinutes(45)

            coVerify { repository.setReminderIntervalMinutes(45) }
            verify(exactly = 0) { ReminderScheduler.scheduleReminders(any(), any()) }
        }

    @Test
    fun `setReminderWindow delegates to the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.setReminderWindow(any(), any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.setReminderWindow(9, 19)

        coVerify { repository.setReminderWindow(9, 19) }
    }

    @Test
    fun `setBodyWeightKg delegates to the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.setBodyWeightKg(any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.setBodyWeightKg(90)

        coVerify { repository.setBodyWeightKg(90) }
    }

    @Test
    fun `setActivityLevel delegates to the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.setActivityLevel(any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.setActivityLevel(ActivityLevel.LOW)

        coVerify { repository.setActivityLevel(ActivityLevel.LOW) }
    }

    @Test
    fun `setHotClimate delegates to the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.setHotClimate(any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.setHotClimate(true)

        coVerify { repository.setHotClimate(true) }
    }

    @Test
    fun `restorePurchases delegates to the premium manager`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { premiumManager.restorePurchases() } returns Unit
        val viewModel = newViewModel()

        viewModel.restorePurchases()

        coVerify { premiumManager.restorePurchases() }
    }
}
