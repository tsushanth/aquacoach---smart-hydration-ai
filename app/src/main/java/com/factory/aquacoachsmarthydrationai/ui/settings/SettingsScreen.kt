package com.factory.aquacoachsmarthydrationai.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.factory.aquacoachsmarthydrationai.data.preferences.ActivityLevel
import com.factory.aquacoachsmarthydrationai.data.preferences.UnitSystem
import com.factory.aquacoachsmarthydrationai.ui.components.PremiumLockPrompt
import com.factory.aquacoachsmarthydrationai.ui.components.ProBadge
import com.factory.aquacoachsmarthydrationai.util.SmartGoalCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onUpgradeClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    val isPremium = uiState.premiumState.isPremium

    var goalText by remember { mutableStateOf(prefs.dailyGoalMl.toString()) }
    var weightText by remember { mutableStateOf(prefs.bodyWeightKg.toString()) }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(prefs.dailyGoalMl) { goalText = prefs.dailyGoalMl.toString() }
    LaunchedEffect(prefs.bodyWeightKg) { weightText = prefs.bodyWeightKg.toString() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            PremiumStatusCard(isPremium = isPremium, onUpgradeClick = onUpgradeClick)

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "Daily Goal") {
                val goalError = goalText.isNotEmpty() && (goalText.toIntOrNull() ?: 0) <= 0
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { input ->
                        goalText = input.filter { it.isDigit() }
                        goalText.toIntOrNull()?.takeIf { it > 0 }?.let { viewModel.setDailyGoalMl(it) }
                    },
                    label = { Text("Goal (ml)") },
                    singleLine = true,
                    isError = goalError,
                    supportingText = {
                        if (goalError) Text("Enter a goal greater than 0")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "Smart Goal Calculator", proBadge = !isPremium) {
                if (!isPremium) {
                    PremiumLockPrompt(
                        message = "Get a personalized daily goal based on your body weight, activity level and climate.",
                        onUnlockClick = onUpgradeClick
                    )
                } else {
                    val weightError = weightText.isNotEmpty() && (weightText.toIntOrNull() ?: 0) <= 0
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { input ->
                            weightText = input.filter { it.isDigit() }
                            weightText.toIntOrNull()?.takeIf { it > 0 }?.let { viewModel.setBodyWeightKg(it) }
                        },
                        label = { Text("Body weight (kg)") },
                        singleLine = true,
                        isError = weightError,
                        supportingText = {
                            if (weightError) Text("Enter a weight greater than 0")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Activity Level", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        ActivityLevel.entries.forEach { level ->
                            FilterChip(
                                selected = prefs.activityLevel == level,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setActivityLevel(level)
                                },
                                label = { Text(level.displayName) },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .semantics {
                                        contentDescription = "${level.displayName} activity level"
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Hot / humid climate", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.hotClimate,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setHotClimate(it)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Hot or humid climate"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    val recommended = SmartGoalCalculator.recommendedGoalMl(
                        bodyWeightKg = prefs.bodyWeightKg,
                        activityLevel = prefs.activityLevel,
                        hotClimate = prefs.hotClimate
                    )
                    Text(
                        text = "Recommended: $recommended ml/day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setDailyGoalMl(recommended)
                    }) {
                        Text("Apply Recommended Goal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "Units") {
                Row {
                    FilterChip(
                        selected = prefs.unitSystem == UnitSystem.ML,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setUnitSystem(UnitSystem.ML)
                        },
                        label = { Text("Milliliters (ml)") },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .semantics { contentDescription = "Milliliters unit" }
                    )
                    FilterChip(
                        selected = prefs.unitSystem == UnitSystem.OZ,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setUnitSystem(UnitSystem.OZ)
                        },
                        label = { Text("Fluid ounces (oz)") },
                        modifier = Modifier.semantics { contentDescription = "Fluid ounces unit" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "Reminders") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Enable reminders", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = prefs.remindersEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setRemindersEnabled(it)
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Enable hydration reminders"
                        }
                    )
                }

                if (prefs.remindersEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Remind every ${prefs.reminderIntervalMinutes} minutes",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Custom interval & active hours",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProBadge()
                        }
                        PremiumLockPrompt(
                            message = "Unlock custom reminder intervals and active-hours scheduling.",
                            onUnlockClick = onUpgradeClick
                        )
                    } else {
                        Slider(
                            value = prefs.reminderIntervalMinutes.toFloat(),
                            onValueChange = { viewModel.setReminderIntervalMinutes(it.toInt()) },
                            onValueChangeFinished = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            valueRange = 30f..180f,
                            steps = 4,
                            modifier = Modifier.semantics {
                                contentDescription =
                                    "Reminder interval, ${prefs.reminderIntervalMinutes} minutes"
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active hours: ${prefs.reminderStartHour}:00 - ${prefs.reminderEndHour}:00",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Start hour", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = prefs.reminderStartHour.toFloat(),
                            onValueChange = {
                                viewModel.setReminderWindow(it.toInt(), prefs.reminderEndHour)
                            },
                            onValueChangeFinished = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.semantics {
                                contentDescription = "Reminder start hour, ${prefs.reminderStartHour}:00"
                            }
                        )
                        Text(text = "End hour", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = prefs.reminderEndHour.toFloat(),
                            onValueChange = {
                                viewModel.setReminderWindow(prefs.reminderStartHour, it.toInt())
                            },
                            onValueChangeFinished = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.semantics {
                                contentDescription = "Reminder end hour, ${prefs.reminderEndHour}:00"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.restorePurchases()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Purchases")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PremiumStatusCard(isPremium: Boolean, onUpgradeClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isPremium) "AquaCoach Premium" else "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isPremium) {
                        "All premium features are unlocked."
                    } else {
                        "Unlock Smart Goals, custom reminders & full history."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!isPremium) {
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUpgradeClick()
                }) { Text("Upgrade") }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    proBadge: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (proBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ProBadge()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
