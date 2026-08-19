package com.voltcare.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voltcare.app.ui.screens.dashboard.DashboardScreen
import com.voltcare.app.ui.screens.drain.DrainScreen
import com.voltcare.app.ui.screens.history.HistoryScreen
import com.voltcare.app.ui.screens.rules.RulesScreen
import com.voltcare.app.ui.screens.stress.StressTestScreen
import com.voltcare.app.ui.screens.update.UpdateCheckAction

/** Route non-tab (tidak muncul di bottom nav), dibuka via FAB di Dashboard. */
private const val ROUTE_STRESS_TEST = "stress_test"

/** 4 tab utama sesuai spec: Dashboard > Riwayat > Penguras > Aturan. */
sealed class VcTab(val route: String, val label: String) {
    data object Dashboard : VcTab("dashboard", "Dashboard")
    data object History : VcTab("history", "Riwayat")
    data object Drain : VcTab("drain", "Penguras")
    data object Rules : VcTab("rules", "Aturan")

    companion object {
        val all = listOf(Dashboard, History, Drain, Rules)
    }
}

@Composable
fun VoltCareNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                VcTab.all.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = VcTab.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(VcTab.Dashboard.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen()
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        UpdateCheckAction()
                    }
                    FloatingActionButton(
                        onClick = { navController.navigate(ROUTE_STRESS_TEST) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = "Tes Baterai")
                    }
                }
            }
            composable(VcTab.History.route) { HistoryScreen() }
            composable(VcTab.Drain.route) { DrainScreen() }
            composable(VcTab.Rules.route) { RulesScreen() }
            composable(ROUTE_STRESS_TEST) {
                StressTestScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun tabIcon(tab: VcTab) = when (tab) {
    VcTab.Dashboard -> Icons.Filled.BatteryChargingFull
    VcTab.History -> Icons.Filled.BarChart
    VcTab.Drain -> Icons.Filled.TrendingDown
    VcTab.Rules -> Icons.Filled.Rule
}
