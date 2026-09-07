package com.factory.aquacoachsmarthydrationai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntry
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentMl: Int = 0,
    val goalMl: Int = 2000,
    val unitSystem: com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem =
        com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem.ML,
    val todayEntries: List<WaterEntry> = emptyList()
)

class HomeViewModel(private val repository: HydrationRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTodayTotal(),
        repository.userPreferencesFlow,
        repository.observeTodayEntries()
    ) { total, prefs: UserPreferences, entries ->
        HomeUiState(
            currentMl = total,
            goalMl = prefs.dailyGoalMl,
            unitSystem = prefs.unitSystem,
            todayEntries = entries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(amountMl)
        }
    }

    fun deleteEntry(entry: WaterEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }
}

class HomeViewModelFactory(private val repository: HydrationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
