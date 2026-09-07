package com.factory.aquacoachsmarthydrationai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aquacoach_preferences")

enum class UnitSystem { ML, OZ }

data class UserPreferences(
    val dailyGoalMl: Int = 2000,
    val unitSystem: UnitSystem = UnitSystem.ML,
    val remindersEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 90,
    val reminderStartHour: Int = 8,
    val reminderEndHour: Int = 22,
    val bodyWeightKg: Int = 70,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val hotClimate: Boolean = false,
    val onboardingComplete: Boolean = false
)

enum class ActivityLevel(val displayName: String, val extraMlPerKg: Double) {
    LOW("Sedentary", 30.0),
    MODERATE("Moderately Active", 35.0),
    HIGH("Very Active", 40.0)
}

class UserPreferencesRepository(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {

    private object Keys {
        val DAILY_GOAL_ML = intPreferencesKey("daily_goal_ml")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INTERVAL_MINUTES = intPreferencesKey("reminder_interval_minutes")
        val REMINDER_START_HOUR = intPreferencesKey("reminder_start_hour")
        val REMINDER_END_HOUR = intPreferencesKey("reminder_end_hour")
        val BODY_WEIGHT_KG = intPreferencesKey("body_weight_kg")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val HOT_CLIMATE = booleanPreferencesKey("hot_climate")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            dailyGoalMl = prefs[Keys.DAILY_GOAL_ML] ?: 2000,
            unitSystem = prefs[Keys.UNIT_SYSTEM]?.let { UnitSystem.valueOf(it) } ?: UnitSystem.ML,
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            reminderIntervalMinutes = prefs[Keys.REMINDER_INTERVAL_MINUTES] ?: 90,
            reminderStartHour = prefs[Keys.REMINDER_START_HOUR] ?: 8,
            reminderEndHour = prefs[Keys.REMINDER_END_HOUR] ?: 22,
            bodyWeightKg = prefs[Keys.BODY_WEIGHT_KG] ?: 70,
            activityLevel = prefs[Keys.ACTIVITY_LEVEL]?.let { ActivityLevel.valueOf(it) }
                ?: ActivityLevel.MODERATE,
            hotClimate = prefs[Keys.HOT_CLIMATE] ?: false,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false
        )
    }

    suspend fun setDailyGoalMl(goalMl: Int) {
        dataStore.edit { it[Keys.DAILY_GOAL_ML] = goalMl }
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { it[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setReminderIntervalMinutes(minutes: Int) {
        dataStore.edit { it[Keys.REMINDER_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setReminderWindow(startHour: Int, endHour: Int) {
        dataStore.edit {
            it[Keys.REMINDER_START_HOUR] = startHour
            it[Keys.REMINDER_END_HOUR] = endHour
        }
    }

    suspend fun setBodyWeightKg(weightKg: Int) {
        dataStore.edit { it[Keys.BODY_WEIGHT_KG] = weightKg }
    }

    suspend fun setActivityLevel(level: ActivityLevel) {
        dataStore.edit { it[Keys.ACTIVITY_LEVEL] = level.name }
    }

    suspend fun setHotClimate(hotClimate: Boolean) {
        dataStore.edit { it[Keys.HOT_CLIMATE] = hotClimate }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }
}
