package com.factory.aquacoachsmarthydrationai.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.factory.aquacoachsmarthydrationai.AquaCoachApplication
import kotlinx.coroutines.flow.first
import java.time.LocalTime

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as AquaCoachApplication
        val repository = app.hydrationRepository

        val prefs = repository.userPreferencesFlow.first()
        if (!prefs.remindersEnabled) return Result.success()

        val currentHour = LocalTime.now().hour
        val withinWindow = if (prefs.reminderStartHour <= prefs.reminderEndHour) {
            currentHour in prefs.reminderStartHour until prefs.reminderEndHour
        } else {
            currentHour >= prefs.reminderStartHour || currentHour < prefs.reminderEndHour
        }
        if (!withinWindow) return Result.success()

        val todayTotal = repository.observeTodayTotal().first()
        if (todayTotal >= prefs.dailyGoalMl) return Result.success()

        NotificationHelper.showReminderNotification(applicationContext)
        return Result.success()
    }
}
