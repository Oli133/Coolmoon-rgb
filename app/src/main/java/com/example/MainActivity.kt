package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.PresetsAndAiScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.ZonesScreen
import com.example.ui.theme.CoolMoonTheme
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.CoolMoonViewModel

const val ROUTE_STUDIO = "studio"
const val ROUTE_ZONES = "zones"
const val ROUTE_PRESETS = "presets"
const val ROUTE_CONNECT = "connect"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopDestinations = listOf(
    NavDestination(ROUTE_STUDIO, R.string.nav_studio, Icons.Filled.Tune, Icons.Outlined.Tune),
    NavDestination(ROUTE_ZONES, R.string.nav_zones, Icons.Filled.Hub, Icons.Outlined.Hub),
    NavDestination(ROUTE_PRESETS, R.string.nav_presets, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavDestination(ROUTE_CONNECT, R.string.nav_connect, Icons.Filled.BluetoothConnected, Icons.Outlined.BluetoothConnected)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoolMoonTheme {
                CoolMoonApp()
            }
        }
    }
}

@Composable
fun CoolMoonApp(
    viewModel: CoolMoonViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_STUDIO
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.lastStatusBanner) {
        val msg = state.lastStatusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianCanvas,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_STUDIO,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_STUDIO) {
                        StudioScreen(
                            state = state,
                            onTogglePower = viewModel::togglePower,
                            onSelectZone = viewModel::selectZoneForPreview,
                            onSelectEditingSecondary = viewModel::setEditingSecondaryColor,
                            onSelectColor = viewModel::selectColor,
                            onSelectEffect = viewModel::selectEffect,
                            onBrightnessChange = viewModel::updateBrightness,
                            onSpeedChange = viewModel::updateSpeed,
                            onToggleDirection = viewModel::toggleDirection,
                            onSelectFanCurve = viewModel::selectFanCurve,
                            onSavePreset = viewModel::saveCurrentAsPreset,
                            onNavigateToConnect = { navigateTo(ROUTE_CONNECT) }
                        )
                    }

                    composable(ROUTE_ZONES) {
                        ZonesScreen(
                            state = state,
                            onToggleMbSync = viewModel::toggleMbSync,
                            onSyncAllZones = viewModel::syncAllZonesToMaster,
                            onUpdateZone = viewModel::updateZone,
                            onIrKeyClick = viewModel::triggerIrKey
                        )
                    }

                    composable(ROUTE_PRESETS) {
                        PresetsAndAiScreen(
                            state = state,
                            onAiQueryChange = viewModel::updateAiQuery,
                            onRunAiArchitect = viewModel::runAiLightingArchitect,
                            onApplyAiPlan = { plan ->
                                viewModel.applyAiPlan(plan)
                                navigateTo(ROUTE_STUDIO)
                            },
                            onApplyPreset = { preset ->
                                viewModel.applyPreset(preset)
                                navigateTo(ROUTE_STUDIO)
                            },
                            onDeletePreset = viewModel::deletePreset
                        )
                    }

                    composable(ROUTE_CONNECT) {
                        ConnectScreen(
                            state = state,
                            requiredBlePermissions = viewModel.requiredBlePermissions(),
                            onRefreshCapabilities = viewModel::refreshHardwareCapabilities,
                            onStartBleScan = viewModel::startBleScan,
                            onStopBleScan = viewModel::stopBleScan,
                            onConnectBle = viewModel::connectBleDevice,
                            onDisconnectBle = viewModel::disconnectBle,
                            onUpdateWifiConfig = viewModel::updateWifiConfig,
                            onTestWifi = viewModel::testWifiConnection,
                            onSwitchMode = viewModel::switchConnectionMode
                        )
                    }
                }
            }
        }
    }
}
