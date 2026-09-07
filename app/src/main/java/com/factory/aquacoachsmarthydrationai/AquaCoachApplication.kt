package com.factory.aquacoachsmarthydrationai

import android.app.Application
import com.factory.aquacoachsmarthydrationai.data.billing.BillingManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.local.AppDatabase
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferencesRepository
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import com.factory.aquacoachsmarthydrationai.notification.NotificationHelper
import com.factory.aquacoachsmarthydrationai.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AquaCoachApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }
    val hydrationRepository: HydrationRepository by lazy {
        HydrationRepository(database.waterEntryDao(), userPreferencesRepository)
    }
    val billingManager: BillingManager by lazy { BillingManager(this) }
    val premiumManager: PremiumManager by lazy { PremiumManager(this, billingManager) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        premiumManager.startObserving()

        applicationScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            if (prefs.remindersEnabled) {
                ReminderScheduler.scheduleReminders(this@AquaCoachApplication, prefs.reminderIntervalMinutes)
            }
        }
    }
}
