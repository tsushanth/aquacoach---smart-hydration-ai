package com.factory.aquacoachsmarthydrationai.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumState
import com.factory.aquacoachsmarthydrationai.data.preferences.ActivityLevel
import com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import com.factory.aquacoachsmarthydrationai.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val premiumState: PremiumState = PremiumState()
)

class SettingsViewModel(
    private val repository: HydrationRepository,
    private val appContext: Context,
    private val premiumManager: PremiumManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.userPreferencesFlow,
        premiumManager.premiumStateFlow
    ) { prefs, premium -> SettingsUiState(preferences = prefs, premiumState = premium) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun setDailyGoalMl(goalMl: Int) {
        viewModelScope.launch { repository.setDailyGoalMl(goalMl) }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch { repository.setUnitSystem(unitSystem) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRemindersEnabled(enabled)
            if (enabled) {
                val interval = repository.userPreferencesFlow.first().reminderIntervalMinutes
                ReminderScheduler.scheduleReminders(appContext, interval)
            } else {
                ReminderScheduler.cancelReminders(appContext)
            }
        }
    }

    fun setReminderIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setReminderIntervalMinutes(minutes)
            if (repository.userPreferencesFlow.first().remindersEnabled) {
                ReminderScheduler.scheduleReminders(appContext, minutes)
            }
        }
    }

    fun setReminderWindow(startHour: Int, endHour: Int) {
        viewModelScope.launch { repository.setReminderWindow(startHour, endHour) }
    }

    fun setBodyWeightKg(weightKg: Int) {
        viewModelScope.launch { repository.setBodyWeightKg(weightKg) }
    }

    fun setActivityLevel(level: ActivityLevel) {
        viewModelScope.launch { repository.setActivityLevel(level) }
    }

    fun setHotClimate(hotClimate: Boolean) {
        viewModelScope.launch { repository.setHotClimate(hotClimate) }
    }

    fun restorePurchases() {
        viewModelScope.launch { premiumManager.restorePurchases() }
    }
}

class SettingsViewModelFactory(
    private val repository: HydrationRepository,
    private val context: Context,
    private val premiumManager: PremiumManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(repository, context.applicationContext, premiumManager) as T
    }
}
