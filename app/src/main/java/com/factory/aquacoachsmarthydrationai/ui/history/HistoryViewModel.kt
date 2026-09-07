package com.factory.aquacoachsmarthydrationai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import com.factory.aquacoachsmarthydrationai.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DayHistoryEntry(
    val epochDay: Long,
    val totalMl: Int,
    val goalMl: Int
)

data class HistoryUiState(
    val days: List<DayHistoryEntry> = emptyList(),
    val currentStreak: Int = 0,
    val averageMl: Int = 0,
    val isPremium: Boolean = false
)

private const val HISTORY_DAYS = 7

class HistoryViewModel(
    private val repository: HydrationRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val startEpochDay = DateUtils.todayEpochDay() - (HISTORY_DAYS - 1)
    private val endEpochDay = DateUtils.todayEpochDay()

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeDailyTotals(startEpochDay, endEpochDay),
        repository.userPreferencesFlow,
        premiumManager.premiumStateFlow
    ) { totals, prefs, premium ->
        val totalsByDay = totals.associate { it.dayEpochDay to it.totalMl }
        val days = (startEpochDay..endEpochDay).map { day ->
            DayHistoryEntry(
                epochDay = day,
                totalMl = totalsByDay[day] ?: 0,
                goalMl = prefs.dailyGoalMl
            )
        }

        var streak = 0
        for (day in days.reversed()) {
            if (day.totalMl >= day.goalMl && day.goalMl > 0) {
                streak++
            } else if (day.epochDay == DateUtils.todayEpochDay()) {
                continue
            } else {
                break
            }
        }

        val average = if (days.isNotEmpty()) days.sumOf { it.totalMl } / days.size else 0

        HistoryUiState(days = days, currentStreak = streak, averageMl = average, isPremium = premium.isPremium)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )
}

class HistoryViewModelFactory(
    private val repository: HydrationRepository,
    private val premiumManager: PremiumManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(repository, premiumManager) as T
    }
}
