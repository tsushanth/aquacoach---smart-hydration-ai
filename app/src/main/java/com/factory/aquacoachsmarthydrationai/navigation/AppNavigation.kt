package com.factory.aquacoachsmarthydrationai.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.factory.aquacoachsmarthydrationai.data.billing.BillingManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumManager
import com.factory.aquacoachsmarthydrationai.data.billing.PremiumState
import com.factory.aquacoachsmarthydrationai.data.preferences.UserPreferences
import com.factory.aquacoachsmarthydrationai.data.repository.HydrationRepository
import com.factory.aquacoachsmarthydrationai.ui.components.ProBadge
import com.factory.aquacoachsmarthydrationai.ui.history.HistoryScreen
import com.factory.aquacoachsmarthydrationai.ui.history.HistoryViewModel
import com.factory.aquacoachsmarthydrationai.ui.history.HistoryViewModelFactory
import com.factory.aquacoachsmarthydrationai.ui.home.HomeScreen
import com.factory.aquacoachsmarthydrationai.ui.home.HomeViewModel
import com.factory.aquacoachsmarthydrationai.ui.home.HomeViewModelFactory
import com.factory.aquacoachsmarthydrationai.ui.paywall.PaywallScreen
import com.factory.aquacoachsmarthydrationai.ui.paywall.PaywallViewModel
import com.factory.aquacoachsmarthydrationai.ui.paywall.PaywallViewModelFactory
import com.factory.aquacoachsmarthydrationai.ui.settings.SettingsScreen
import com.factory.aquacoachsmarthydrationai.ui.settings.SettingsViewModel
import com.factory.aquacoachsmarthydrationai.ui.settings.SettingsViewModelFactory

@Composable
fun AppNavigation(
    repository: HydrationRepository,
    billingManager: BillingManager,
    premiumManager: PremiumManager
) {
    val navController = rememberNavController()
    val prefs by repository.userPreferencesFlow.collectAsState(initial = UserPreferences())
    val premiumState by premiumManager.premiumStateFlow.collectAsState(initial = PremiumState())
    var firstRunPaywallShown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(prefs.onboardingComplete) {
        if (!prefs.onboardingComplete && !firstRunPaywallShown) {
            firstRunPaywallShown = true
            repository.setOnboardingComplete(true)
            navController.navigate(Screen.Paywall.route)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = {
                            if (screen == Screen.History && !premiumState.isPremium) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(screen.label)
                                    ProBadge()
                                }
                            } else {
                                Text(screen.label)
                            }
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
                HomeScreen(viewModel = viewModel)
            }
            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(repository, premiumManager)
                )
                HistoryScreen(
                    viewModel = viewModel,
                    onUnlockPremium = { navController.navigate(Screen.Paywall.route) }
                )
            }
            composable(Screen.Settings.route) {
                val context = LocalContext.current
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(repository, context, premiumManager)
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onUpgradeClick = { navController.navigate(Screen.Paywall.route) }
                )
            }
            composable(Screen.Paywall.route) {
                val viewModel: PaywallViewModel = viewModel(
                    factory = PaywallViewModelFactory(billingManager, premiumManager)
                )
                PaywallScreen(
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
