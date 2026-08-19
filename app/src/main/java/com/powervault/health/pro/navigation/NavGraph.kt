package com.powervault.health.pro.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.powervault.health.pro.ui.screens.dashboard.DashboardScreen
import com.powervault.health.pro.ui.screens.drain.DrainScreen
import com.powervault.health.pro.ui.screens.history.HistoryScreen
import com.powervault.health.pro.ui.screens.rules.RulesScreen

/** 4 tab utama sesuai spec: Dashboard > Riwayat > Penguras > Aturan. */
sealed class PvTab(val route: String, val label: String) {
    data object Dashboard : PvTab("dashboard", "Dashboard")
    data object History : PvTab("history", "Riwayat")
    data object Drain : PvTab("drain", "Penguras")
    data object Rules : PvTab("rules", "Aturan")

    companion object {
        val all = listOf(Dashboard, History, Drain, Rules)
    }
}

@Composable
fun PowerVaultNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                PvTab.all.forEach { tab ->
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
            startDestination = PvTab.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(PvTab.Dashboard.route) { DashboardScreen() }
            composable(PvTab.History.route) { HistoryScreen() }
            composable(PvTab.Drain.route) { DrainScreen() }
            composable(PvTab.Rules.route) { RulesScreen() }
        }
    }
}

private fun tabIcon(tab: PvTab) = when (tab) {
    PvTab.Dashboard -> Icons.Filled.BatteryChargingFull
    PvTab.History -> Icons.Filled.BarChart
    PvTab.Drain -> Icons.Filled.TrendingDown
    PvTab.Rules -> Icons.Filled.Rule
}
