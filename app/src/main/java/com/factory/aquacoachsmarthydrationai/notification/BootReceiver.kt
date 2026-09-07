package com.factory.aquacoachsmarthydrationai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.factory.aquacoachsmarthydrationai.AquaCoachApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as AquaCoachApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                val prefs = app.hydrationRepository.userPreferencesFlow.first()
                if (prefs.remindersEnabled) {
                    ReminderScheduler.scheduleReminders(context, prefs.reminderIntervalMinutes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
