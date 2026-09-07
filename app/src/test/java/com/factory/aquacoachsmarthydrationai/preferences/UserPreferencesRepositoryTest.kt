package com.factory.aquacoachsmarthydrationai.preferences

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.factory.aquacoachsmarthydrationai.data.preferences.ActivityLevel
import com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferencesRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Each test gets its own temp-file-backed DataStore (via [newRepository]) instead of sharing
 * the app-wide `preferencesDataStore` singleton, so tests can't leak state into one another
 * regardless of JUnit's execution order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    private fun TestScope.newRepository(): UserPreferencesRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File.createTempFile("user_prefs_test_", ".preferences_pb").apply { deleteOnExit() }
        }
        return UserPreferencesRepository(mockk<Context>(relaxed = true), dataStore)
    }

    @Test
    fun `default preferences are used before anything is written`() = runTest {
        val repository = newRepository()

        repository.userPreferencesFlow.test {
            val prefs = awaitItem()
            assertEquals(2000, prefs.dailyGoalMl)
            assertEquals(UnitSystem.ML, prefs.unitSystem)
            assertTrue(prefs.remindersEnabled)
            assertEquals(90, prefs.reminderIntervalMinutes)
            assertEquals(8, prefs.reminderStartHour)
            assertEquals(22, prefs.reminderEndHour)
            assertEquals(70, prefs.bodyWeightKg)
            assertEquals(ActivityLevel.MODERATE, prefs.activityLevel)
            assertFalse(prefs.hotClimate)
            assertFalse(prefs.onboardingComplete)
        }
    }

    @Test
    fun `setDailyGoalMl persists and is reflected in the flow`() = runTest {
        val repository = newRepository()

        repository.userPreferencesFlow.test {
            assertEquals(2000, awaitItem().dailyGoalMl)
            repository.setDailyGoalMl(3200)
            assertEquals(3200, awaitItem().dailyGoalMl)
        }
    }

    @Test
    fun `setUnitSystem persists the chosen unit`() = runTest {
        val repository = newRepository()

        repository.userPreferencesFlow.test {
            assertEquals(UnitSystem.ML, awaitItem().unitSystem)
            repository.setUnitSystem(UnitSystem.OZ)
            assertEquals(UnitSystem.OZ, awaitItem().unitSystem)
        }
    }

    @Test
    fun `setRemindersEnabled toggles reminder state`() = runTest {
        val repository = newRepository()

        repository.userPreferencesFlow.test {
            assertTrue(awaitItem().remindersEnabled)
            repository.setRemindersEnabled(false)
            assertFalse(awaitItem().remindersEnabled)
        }
    }

    @Test
    fun `setReminderWindow updates both start and end hour together`() = runTest {
        val repository = newRepository()

        repository.userPreferencesFlow.test {
            awaitItem()
            repository.setReminderWindow(startHour = 6, endHour = 20)
            val updated = awaitItem()
            assertEquals(6, updated.reminderStartHour)
            assertEquals(20, updated.reminderEndHour)
        }
    }

    @Test
    fun `setActivityLevel and setHotClimate and setBodyWeightKg persist independently`() = runTest {
        val repository = newRepository()

        repository.userPreferencesFlow.test {
            awaitItem()

            repository.setBodyWeightKg(85)
            assertEquals(85, awaitItem().bodyWeightKg)

            repository.setActivityLevel(ActivityLevel.HIGH)
            assertEquals(ActivityLevel.HIGH, awaitItem().activityLevel)

            repository.setHotClimate(true)
            assertTrue(awaitItem().hotClimate)
        }
    }

    @Test
    fun `setOnboardingComplete persists true`() = runTest {
        val repository = newRepository()

        repository.userPreferencesFlow.test {
            assertFalse(awaitItem().onboardingComplete)
            repository.setOnboardingComplete(true)
            assertTrue(awaitItem().onboardingComplete)
        }
    }

    @Test
    fun `preferences persist across repository instances sharing the same data store`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File.createTempFile("user_prefs_test_", ".preferences_pb").apply { deleteOnExit() }
        }
        val repository1 = UserPreferencesRepository(mockk<Context>(relaxed = true), dataStore)
        repository1.setDailyGoalMl(2750)
        repository1.setReminderIntervalMinutes(45)

        val repository2 = UserPreferencesRepository(mockk<Context>(relaxed = true), dataStore)
        repository2.userPreferencesFlow.test {
            val prefs = awaitItem()
            assertEquals(2750, prefs.dailyGoalMl)
            assertEquals(45, prefs.reminderIntervalMinutes)
        }
    }
}
