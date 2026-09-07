package com.factory.aquacoachsmarthydrationai.repository

import app.cash.turbine.test
import com.factory.aquacoachsmarthydrationai.data.local.DailyTotal
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntry
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntryDao
import com.factory.aquacoachsmarthydrationai.data.preferences.ActivityLevel
import com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferencesRepository
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HydrationRepositoryTest {

    private lateinit var dao: WaterEntryDao
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var repository: HydrationRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        repository = HydrationRepository(dao, userPreferencesRepository)
    }

    @Test
    fun `observeTodayTotal delegates to the dao for today's epoch day`() = runTest {
        every { dao.observeTotalForDay(any()) } returns flowOf(1500)

        repository.observeTodayTotal().test {
            assertEquals(1500, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeTodayEntries delegates to the dao`() = runTest {
        val entries = listOf(WaterEntry(id = 1, amountMl = 250, timestampEpochMillis = 10, dayEpochDay = 5))
        every { dao.observeEntriesForDay(any()) } returns flowOf(entries)

        repository.observeTodayEntries().test {
            assertEquals(entries, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeDailyTotals passes through the requested range`() = runTest {
        val totals = listOf(DailyTotal(dayEpochDay = 1, totalMl = 500))
        every { dao.observeDailyTotals(1, 7) } returns flowOf(totals)

        repository.observeDailyTotals(1, 7).test {
            assertEquals(totals, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `logWater inserts an entry stamped with today's epoch day`() = runTest {
        val entrySlot = slot<WaterEntry>()
        coEvery { dao.insert(capture(entrySlot)) } returns 1L

        repository.logWater(amountMl = 350, timestampEpochMillis = 123456L)

        coVerify { dao.insert(any()) }
        assertEquals(350, entrySlot.captured.amountMl)
        assertEquals(123456L, entrySlot.captured.timestampEpochMillis)
    }

    @Test
    fun `deleteEntry delegates to the dao`() = runTest {
        val entry = WaterEntry(id = 4, amountMl = 200, timestampEpochMillis = 1, dayEpochDay = 1)
        coEvery { dao.delete(entry) } returns Unit

        repository.deleteEntry(entry)

        coVerify { dao.delete(entry) }
    }

    @Test
    fun `setDailyGoalMl delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setDailyGoalMl(any()) } returns Unit

        repository.setDailyGoalMl(2500)

        coVerify { userPreferencesRepository.setDailyGoalMl(2500) }
    }

    @Test
    fun `setUnitSystem delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setUnitSystem(any()) } returns Unit

        repository.setUnitSystem(UnitSystem.OZ)

        coVerify { userPreferencesRepository.setUnitSystem(UnitSystem.OZ) }
    }

    @Test
    fun `setRemindersEnabled delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setRemindersEnabled(any()) } returns Unit

        repository.setRemindersEnabled(false)

        coVerify { userPreferencesRepository.setRemindersEnabled(false) }
    }

    @Test
    fun `setReminderIntervalMinutes delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setReminderIntervalMinutes(any()) } returns Unit

        repository.setReminderIntervalMinutes(30)

        coVerify { userPreferencesRepository.setReminderIntervalMinutes(30) }
    }

    @Test
    fun `setReminderWindow delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setReminderWindow(any(), any()) } returns Unit

        repository.setReminderWindow(7, 21)

        coVerify { userPreferencesRepository.setReminderWindow(7, 21) }
    }

    @Test
    fun `setBodyWeightKg delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setBodyWeightKg(any()) } returns Unit

        repository.setBodyWeightKg(80)

        coVerify { userPreferencesRepository.setBodyWeightKg(80) }
    }

    @Test
    fun `setActivityLevel delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setActivityLevel(any()) } returns Unit

        repository.setActivityLevel(ActivityLevel.HIGH)

        coVerify { userPreferencesRepository.setActivityLevel(ActivityLevel.HIGH) }
    }

    @Test
    fun `setHotClimate delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setHotClimate(any()) } returns Unit

        repository.setHotClimate(true)

        coVerify { userPreferencesRepository.setHotClimate(true) }
    }

    @Test
    fun `setOnboardingComplete delegates to the preferences repository`() = runTest {
        coEvery { userPreferencesRepository.setOnboardingComplete(any()) } returns Unit

        repository.setOnboardingComplete(true)

        coVerify { userPreferencesRepository.setOnboardingComplete(true) }
    }

    @Test
    fun `userPreferencesFlow exposes the underlying preferences flow`() = runTest {
        val prefs = UserPreferences(dailyGoalMl = 3000)
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(prefs)
        val freshRepository = HydrationRepository(dao, userPreferencesRepository)

        freshRepository.userPreferencesFlow.test {
            assertEquals(prefs, awaitItem())
            awaitComplete()
        }
    }
}
