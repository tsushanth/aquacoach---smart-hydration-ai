package com.factory.aquacoachsmarthydrationai.data.repository

import com.factory.aquacoachsmarthydrationai.data.local.DailyTotal
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntry
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntryDao
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferencesRepository
import com.factory.aquacoachsmarthydrationai.util.DateUtils
import kotlinx.coroutines.flow.Flow

class HydrationRepository(
    private val waterEntryDao: WaterEntryDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    val userPreferencesFlow: Flow<UserPreferences> = userPreferencesRepository.userPreferencesFlow

    fun observeTodayTotal(): Flow<Int> =
        waterEntryDao.observeTotalForDay(DateUtils.todayEpochDay())

    fun observeTodayEntries(): Flow<List<WaterEntry>> =
        waterEntryDao.observeEntriesForDay(DateUtils.todayEpochDay())

    fun observeDailyTotals(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyTotal>> =
        waterEntryDao.observeDailyTotals(startEpochDay, endEpochDay)

    suspend fun logWater(amountMl: Int, timestampEpochMillis: Long = System.currentTimeMillis()) {
        waterEntryDao.insert(
            WaterEntry(
                amountMl = amountMl,
                timestampEpochMillis = timestampEpochMillis,
                dayEpochDay = DateUtils.todayEpochDay()
            )
        )
    }

    suspend fun deleteEntry(entry: WaterEntry) {
        waterEntryDao.delete(entry)
    }

    suspend fun setDailyGoalMl(goalMl: Int) = userPreferencesRepository.setDailyGoalMl(goalMl)

    suspend fun setUnitSystem(unitSystem: com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem) =
        userPreferencesRepository.setUnitSystem(unitSystem)

    suspend fun setRemindersEnabled(enabled: Boolean) =
        userPreferencesRepository.setRemindersEnabled(enabled)

    suspend fun setReminderIntervalMinutes(minutes: Int) =
        userPreferencesRepository.setReminderIntervalMinutes(minutes)

    suspend fun setReminderWindow(startHour: Int, endHour: Int) =
        userPreferencesRepository.setReminderWindow(startHour, endHour)

    suspend fun setBodyWeightKg(weightKg: Int) = userPreferencesRepository.setBodyWeightKg(weightKg)

    suspend fun setActivityLevel(level: com.factory.aquacoachsmarthydrationai.data.preferences.ActivityLevel) =
        userPreferencesRepository.setActivityLevel(level)

    suspend fun setHotClimate(hotClimate: Boolean) = userPreferencesRepository.setHotClimate(hotClimate)

    suspend fun setOnboardingComplete(complete: Boolean) =
        userPreferencesRepository.setOnboardingComplete(complete)
}
