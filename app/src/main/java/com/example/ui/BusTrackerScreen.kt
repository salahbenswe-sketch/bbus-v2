package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import com.example.ui.components.AppInfoCard
import com.example.ui.components.BusHeader
import com.example.ui.components.BusDirectionCard
import com.example.ui.components.BusInfoGrid
import com.example.ui.components.BusLiveCard
import com.example.ui.components.BusMapView
import com.example.ui.components.ProximityAlertCard
import com.example.ui.components.StationDetailSheet
import com.example.ui.components.StationListScreen
import com.example.ui.theme.BusAccentCyan
import com.example.ui.theme.BusPrimary
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.viewmodel.BusTrackerViewModel
import com.example.util.AlertStation

private const val TAB_MAP = 0
private const val TAB_STATIONS = 1
private const val TAB_ALERTS = 2
private const val TAB_INFO = 3

@Composable
fun BusTrackerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BusTrackerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_MAP) }
    var selectedStation by rememberSaveable { mutableIntStateOf(-1) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleProximityAlert(true)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                BusHeader(
                    data = uiState.data,
                    isRefreshing = uiState.isRefreshing,
                    isAutoRefresh = uiState.isAutoRefreshEnabled,
                    onRefresh = viewModel::refresh,
                    onToggleAutoRefresh = { viewModel.toggleAutoRefresh() },
                    onReCenter = { viewModel.triggerReCenter() },
                    onBackClick = onNavigateBack,
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                    NavigationBar(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F2937),
                        tonalElevation = 0.dp
                    ) {
                        val navItems = listOf(
                            Triple("الخريطة", Icons.Default.Map, TAB_MAP),
                            Triple("المحطات", Icons.Default.Route, TAB_STATIONS),
                            Triple("التنبيهات", Icons.Default.NotificationsActive, TAB_ALERTS),
                            Triple("عن التطبيق", Icons.Default.Info, TAB_INFO)
                        )
                        navItems.forEach { (label, icon, tab) ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = BusPrimary,
                                    indicatorColor = BusPrimary,
                                    unselectedIconColor = Color(0xFF6B7280),
                                    unselectedTextColor = Color(0xFF6B7280)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    uiState.isLoading && !uiState.hasAnyData -> LoadingView(Modifier.fillMaxSize())
                    uiState.errorMessage != null && !uiState.hasAnyData -> ErrorView(
                        errorMessage = uiState.errorMessage ?: "تعذر تحميل بيانات الحافلة",
                        onRetry = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "tracker_tab_transition"
                    ) { tab ->
                        when (tab) {
                            TAB_MAP -> {
                                Box(Modifier.fillMaxSize()) {
                                    BusMapView(
                                        bus1Data = uiState.bus1Data,
                                        selectedBusIndex = 1,
                                        reCenterTimestamp = uiState.reCenterTimestamp,
                                        reCenterBusIndex = 1,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    BusDirectionCard(
                                        data = uiState.data,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 14.dp)
                                    )
                                    Surface(
                                        onClick = { viewModel.triggerReCenter() },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(top = 14.dp, start = 14.dp)
                                            .size(48.dp)
                                            .testTag("fab_center_bus")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.GpsFixed,
                                                contentDescription = "التركيز على الحافلة",
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    BusLiveCard(
                                        data = uiState.data,
                                        onShowStations = { selectedTab = TAB_STATIONS },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    )
                                }
                            }
                            TAB_STATIONS -> StationListScreen(
                                data = uiState.data,
                                enabledStationsMap = uiState.enabledStationsMap,
                                onStationClick = { selectedStation = it.ordinal }
                            )
                            TAB_ALERTS -> Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProximityAlertCard(
                                    isEnabled = uiState.isProximityAlertEnabled,
                                    enabledStationsMap = uiState.enabledStationsMap,
                                    lastTriggeredAlert = uiState.lastTriggeredAlert,
                                    onToggle = { viewModel.toggleProximityAlert() },
                                    onToggleStationAlert = viewModel::toggleStationAlert,
                                    onToggleStationGroup = viewModel::toggleStationGroup,
                                    onEnableAllStations = viewModel::enableAllStations,
                                    onDisableAllStations = viewModel::disableAllStations,
                                    onTestSound = viewModel::testSoundAlert
                                )
                            }
                            else -> Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.data?.let { data ->
                                    BusInfoGrid(
                                        data = data,
                                        isProximityAlertEnabled = uiState.isProximityAlertEnabled,
                                        enabledStationsMap = uiState.enabledStationsMap,
                                        lastTriggeredAlert = uiState.lastTriggeredAlert,
                                        onToggleProximityAlert = { viewModel.toggleProximityAlert() },
                                        onToggleStationAlert = viewModel::toggleStationAlert,
                                        onEnableAllStations = viewModel::enableAllStations,
                                        onDisableAllStations = viewModel::disableAllStations,
                                        onTestSound = viewModel::testSoundAlert
                                    )
                                }
                                AppInfoCard()
                            }
                        }
                    }
                }

                if (uiState.errorMessage != null && uiState.hasAnyData) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "تعذر تحديث الموقع • عرض آخر بيانات متاحة",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            AlertStation.entries.getOrNull(selectedStation)?.let { station ->
                StationDetailSheet(
                    station = station,
                    data = uiState.data,
                    enabledStationsMap = uiState.enabledStationsMap,
                    onToggleAlert = { key, enabled -> viewModel.toggleStationAlert(key, enabled) },
                    onToggleGroup = { list, enabled -> viewModel.toggleStationGroup(list, enabled) },
                    onTestSound = viewModel::testSoundAlert,
                    onDismiss = { selectedStation = -1 }
                )
            }
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = BusAccentCyan, strokeWidth = 3.dp)
            Text(
                text = "جارٍ تحديد موقع الحافلة...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = StatusOfflineRed,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "لا يمكن الاتصال بالحافلة الآن",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("إعادة المحاولة", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
