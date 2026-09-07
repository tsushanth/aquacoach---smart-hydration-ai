package com.factory.aquacoachsmarthydrationai.util

import com.factory.aquacoachsmarthydrationai.data.preferences.ActivityLevel
import kotlin.math.roundToInt

/**
 * Estimates a recommended daily water intake using body weight and activity level,
 * with a climate adjustment. This is a heuristic (not medical advice).
 */
object SmartGoalCalculator {

    private const val HOT_CLIMATE_BONUS_ML = 350
    private const val MIN_GOAL_ML = 1200
    private const val MAX_GOAL_ML = 5000

    fun recommendedGoalMl(
        bodyWeightKg: Int,
        activityLevel: ActivityLevel,
        hotClimate: Boolean
    ): Int {
        val base = bodyWeightKg * activityLevel.extraMlPerKg
        val withClimate = base + if (hotClimate) HOT_CLIMATE_BONUS_ML else 0
        return withClimate.roundToInt().coerceIn(MIN_GOAL_ML, MAX_GOAL_ML)
    }
}
