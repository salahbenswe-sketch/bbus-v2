package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BusTrackingData
import com.example.data.repository.BusRepository
import com.example.util.AlertStation
import com.example.util.BusAlertManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BusTrackerUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedBusIndex: Int = 1,
    val bus1Data: BusTrackingData? = null,
    val errorMessage: String? = null,
    val isAutoRefreshEnabled: Boolean = true,
    val isProximityAlertEnabled: Boolean = true,
    val enabledStationsMap: Map<String, Boolean> = emptyMap(),
    val lastTriggeredAlert: String? = null,
    val reCenterTimestamp: Long = 0L,
    val reCenterBusIndex: Int = 1,
    val lastSuccessTimestamp: Long? = null
) {
    val data: BusTrackingData?
        get() = bus1Data

    val hasAnyData: Boolean
        get() = bus1Data != null
}

class BusTrackerViewModel(
    application: Application,
    private val repository: BusRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, BusRepository())

    private val _uiState = MutableStateFlow(BusTrackerUiState())
    val uiState: StateFlow<BusTrackerUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        val prefs = application.getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)
        val initialAlertEnabled = prefs.getBoolean("is_proximity_alert_enabled", true)
        
        // تحميل إعدادات التفعيل/التنفيذ لكل محطة على حدة
        val stationsMap = mutableMapOf<String, Boolean>()
        for (station in AlertStation.entries) {
            val prefKey = "station_alert_enabled_${station.name}"
            stationsMap[station.name] = prefs.getBoolean(prefKey, true)
        }

        _uiState.update { 
            it.copy(
                isProximityAlertEnabled = initialAlertEnabled,
                enabledStationsMap = stationsMap
            ) 
        }

        loadData(isInitial = true)
        startPolling()
        updateFcmSubscriptions()
    }

    fun selectBus(busIndex: Int) {
        _uiState.update {
            it.copy(
                selectedBusIndex = 1,
                reCenterBusIndex = 1,
                reCenterTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun refresh() {
        loadData(isInitial = false)
    }

    fun triggerReCenter(busIndex: Int? = null) {
        _uiState.update {
            it.copy(
                reCenterBusIndex = 1,
                reCenterTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun toggleAutoRefresh(enabled: Boolean? = null) {
        val newState = enabled ?: !_uiState.value.isAutoRefreshEnabled
        _uiState.update { it.copy(isAutoRefreshEnabled = newState) }
        if (newState) {
            startPolling()
        } else {
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    fun updateFcmSubscriptions() {
        val prefs = getApplication<Application>().getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)
        val isGlobalEnabled = prefs.getBoolean("is_proximity_alert_enabled", true)
        val hasAnyEnabled = AlertStation.entries.any {
            prefs.getBoolean("station_alert_enabled_${it.name}", true)
        }

        if (isGlobalEnabled && hasAnyEnabled) {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("bus_alerts")
            } catch (_: Exception) {}
        } else {
            try {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("bus_alerts")
            } catch (_: Exception) {}
        }
    }

    fun toggleProximityAlert(enabled: Boolean? = null) {
        val newState = enabled ?: !_uiState.value.isProximityAlertEnabled
        _uiState.update { it.copy(isProximityAlertEnabled = newState) }

        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_proximity_alert_enabled", newState).apply()

        if (!newState) {
            BusAlertManager.stopSoundAlert()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancelAll()
        }

        updateFcmSubscriptions()
    }

    fun isStationAlertEnabled(stationKey: String): Boolean {
        return _uiState.value.enabledStationsMap[stationKey] ?: true
    }

    fun toggleStationAlert(stationKey: String, enabled: Boolean? = null) {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)
        val currentVal = prefs.getBoolean("station_alert_enabled_$stationKey", true)
        val newVal = enabled ?: !currentVal

        prefs.edit().putBoolean("station_alert_enabled_$stationKey", newVal).apply()

        _uiState.update { state ->
            val updatedMap = state.enabledStationsMap.toMutableMap()
            updatedMap[stationKey] = newVal
            state.copy(enabledStationsMap = updatedMap)
        }

        if (!newVal) {
            try {
                val station = AlertStation.valueOf(stationKey)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(station.notificationId)
            } catch (_: Exception) {}
        }

        updateFcmSubscriptions()
    }

    fun toggleStationGroup(stations: List<AlertStation>, enabled: Boolean) {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val keys = stations.map { it.name }

        for (key in keys) {
            editor.putBoolean("station_alert_enabled_$key", enabled)
        }
        editor.apply()

        _uiState.update { state ->
            val updatedMap = state.enabledStationsMap.toMutableMap()
            for (key in keys) {
                updatedMap[key] = enabled
            }
            state.copy(enabledStationsMap = updatedMap)
        }

        if (!enabled) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            for (station in stations) {
                notificationManager?.cancel(station.notificationId)
            }
        }

        updateFcmSubscriptions()
    }

    fun enableAllStations() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val newMap = mutableMapOf<String, Boolean>()
        
        for (station in AlertStation.entries) {
            newMap[station.name] = true
            editor.putBoolean("station_alert_enabled_${station.name}", true)
        }
        editor.apply()
        _uiState.update { it.copy(enabledStationsMap = newMap) }
        updateFcmSubscriptions()
    }

    fun disableAllStations() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val newMap = mutableMapOf<String, Boolean>()
        
        for (station in AlertStation.entries) {
            newMap[station.name] = false
            editor.putBoolean("station_alert_enabled_${station.name}", false)
        }
        editor.apply()
        _uiState.update { it.copy(enabledStationsMap = newMap) }

        BusAlertManager.stopSoundAlert()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()

        updateFcmSubscriptions()
    }

    fun testSoundAlert(station: AlertStation = AlertStation.DOURA) {
        BusAlertManager.triggerSoundAlert(getApplication(), station)
        BusAlertManager.triggerTestSystemNotification(
            getApplication(),
            station,
            "🚌 تجربة إشعار: الحافلة اقتربت من محطة ${station.title}!",
            "اختبار شاشات الإشعار والصوت الخاص بمحطة ${station.title}"
        )
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(2000)
                if (_uiState.value.isAutoRefreshEnabled) {
                    loadDataSilently()
                }
            }
        }
    }

    private fun loadData(isInitial: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isInitial) it.copy(isLoading = true, errorMessage = null)
                else it.copy(isRefreshing = true, errorMessage = null)
            }

            val result = repository.fetchBus1()
            val newBus1 = result.getOrNull()

            if (newBus1 != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        bus1Data = newBus1,
                        errorMessage = null,
                        lastSuccessTimestamp = System.currentTimeMillis()
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "تعذر الاتصال بالخادم"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    private suspend fun loadDataSilently() {
        val result = repository.fetchBus1()
        val newBus1 = result.getOrNull()

        if (newBus1 != null) {
            _uiState.update {
                it.copy(
                    bus1Data = newBus1,
                    errorMessage = null,
                    lastSuccessTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        BusAlertManager.stopSoundAlert()
    }
}
