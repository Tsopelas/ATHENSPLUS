package com.example.athensplus.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.athensplus.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsService(private val context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "athens_plus_settings", Context.MODE_PRIVATE
    )
    
    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()
    
    suspend fun updateTheme(theme: AppTheme) = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(theme = theme)
    }
    
    suspend fun updateLanguage(language: Language) = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(language = language)
    }
    
    suspend fun updateNotificationSettings(
        notificationSettings: NotificationSettings
    ) = withContext(Dispatchers.IO) {
        with(preferences.edit()) {
            putBoolean(KEY_SERVICE_ALERTS, notificationSettings.serviceAlertsEnabled)
            putBoolean(KEY_TRIP_REMINDERS, notificationSettings.tripRemindersEnabled)
            putBoolean(KEY_ARRIVAL_NOTIFICATIONS, notificationSettings.arrivalNotificationsEnabled)
            putBoolean(KEY_EMERGENCY_ALERTS, notificationSettings.emergencyAlertsEnabled)
            putBoolean(KEY_QUIET_HOURS, notificationSettings.quietHoursEnabled)
            putString(KEY_QUIET_START, notificationSettings.quietHoursStart)
            putString(KEY_QUIET_END, notificationSettings.quietHoursEnd)
            apply()
        }
        _settingsFlow.value = _settingsFlow.value.copy(notificationSettings = notificationSettings)
    }
    
    suspend fun updateMapSettings(mapSettings: MapSettings) = withContext(Dispatchers.IO) {
        with(preferences.edit()) {
            putString(KEY_MAP_STYLE, mapSettings.mapStyle.name)
            putBoolean(KEY_SHOW_TRAFFIC, mapSettings.showTrafficLayer)
            putBoolean(KEY_SHOW_ACCESSIBILITY, mapSettings.showAccessibilityInfo)
            putBoolean(KEY_SHOW_REAL_TIME, mapSettings.showRealTimeVehicles)
            putFloat(KEY_DEFAULT_ZOOM, mapSettings.defaultZoomLevel)
            apply()
        }
        _settingsFlow.value = _settingsFlow.value.copy(mapSettings = mapSettings)
    }
    
    suspend fun toggleServiceAlerts() = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.notificationSettings
        val updated = current.copy(serviceAlertsEnabled = !current.serviceAlertsEnabled)
        updateNotificationSettings(updated)
    }
    
    suspend fun toggleTripReminders() = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.notificationSettings
        val updated = current.copy(tripRemindersEnabled = !current.tripRemindersEnabled)
        updateNotificationSettings(updated)
    }
    
    suspend fun toggleArrivalNotifications() = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.notificationSettings
        val updated = current.copy(arrivalNotificationsEnabled = !current.arrivalNotificationsEnabled)
        updateNotificationSettings(updated)
    }
    
    suspend fun toggleTrafficLayer() = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.mapSettings
        val updated = current.copy(showTrafficLayer = !current.showTrafficLayer)
        updateMapSettings(updated)
    }
    
    suspend fun toggleAccessibilityInfo() = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.mapSettings
        val updated = current.copy(showAccessibilityInfo = !current.showAccessibilityInfo)
        updateMapSettings(updated)
    }
    
    suspend fun toggleRealTimeVehicles() = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.mapSettings
        val updated = current.copy(showRealTimeVehicles = !current.showRealTimeVehicles)
        updateMapSettings(updated)
    }
    
    suspend fun setQuietHours(enabled: Boolean, startTime: String, endTime: String) = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.notificationSettings
        val updated = current.copy(
            quietHoursEnabled = enabled,
            quietHoursStart = startTime,
            quietHoursEnd = endTime
        )
        updateNotificationSettings(updated)
    }
    
    suspend fun setDefaultZoomLevel(zoomLevel: Float) = withContext(Dispatchers.IO) {
        val current = _settingsFlow.value.mapSettings
        val updated = current.copy(defaultZoomLevel = zoomLevel)
        updateMapSettings(updated)
    }
    
    suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        preferences.edit().clear().apply()
        _settingsFlow.value = getDefaultSettings()
    }
    
    fun getCurrentSettings(): AppSettings = _settingsFlow.value
    
    fun getTheme(): AppTheme = _settingsFlow.value.theme
    
    fun getLanguage(): Language = _settingsFlow.value.language
    
    fun isServiceAlertsEnabled(): Boolean = 
        _settingsFlow.value.notificationSettings.serviceAlertsEnabled
    
    fun isTripRemindersEnabled(): Boolean = 
        _settingsFlow.value.notificationSettings.tripRemindersEnabled
    
    fun isTrafficLayerEnabled(): Boolean = 
        _settingsFlow.value.mapSettings.showTrafficLayer
    
    fun isAccessibilityInfoEnabled(): Boolean = 
        _settingsFlow.value.mapSettings.showAccessibilityInfo
    
    fun isRealTimeVehiclesEnabled(): Boolean = 
        _settingsFlow.value.mapSettings.showRealTimeVehicles
    
    fun getDefaultZoomLevel(): Float = 
        _settingsFlow.value.mapSettings.defaultZoomLevel
    
    fun getMapStyle(): MapStyle = 
        _settingsFlow.value.mapSettings.mapStyle
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            theme = AppTheme.valueOf(
                preferences.getString(KEY_THEME, AppTheme.LIGHT.name) ?: AppTheme.LIGHT.name
            ),
            language = Language.valueOf(
                preferences.getString(KEY_LANGUAGE, Language.ENGLISH.name) ?: Language.ENGLISH.name
            ),
            notificationSettings = NotificationSettings(
                serviceAlertsEnabled = preferences.getBoolean(KEY_SERVICE_ALERTS, true),
                tripRemindersEnabled = preferences.getBoolean(KEY_TRIP_REMINDERS, true),
                arrivalNotificationsEnabled = preferences.getBoolean(KEY_ARRIVAL_NOTIFICATIONS, false),
                emergencyAlertsEnabled = preferences.getBoolean(KEY_EMERGENCY_ALERTS, true),
                quietHoursEnabled = preferences.getBoolean(KEY_QUIET_HOURS, false),
                quietHoursStart = preferences.getString(KEY_QUIET_START, "22:00") ?: "22:00",
                quietHoursEnd = preferences.getString(KEY_QUIET_END, "07:00") ?: "07:00"
            ),
            mapSettings = MapSettings(
                mapStyle = MapStyle.valueOf(
                    preferences.getString(KEY_MAP_STYLE, MapStyle.NORMAL.name) ?: MapStyle.NORMAL.name
                ),
                showTrafficLayer = preferences.getBoolean(KEY_SHOW_TRAFFIC, false),
                showAccessibilityInfo = preferences.getBoolean(KEY_SHOW_ACCESSIBILITY, true),
                showRealTimeVehicles = preferences.getBoolean(KEY_SHOW_REAL_TIME, true),
                defaultZoomLevel = preferences.getFloat(KEY_DEFAULT_ZOOM, 15.0f)
            )
        )
    }
    
    private fun getDefaultSettings(): AppSettings {
        return AppSettings(
            theme = AppTheme.LIGHT,
            language = Language.ENGLISH,
            notificationSettings = NotificationSettings(
                serviceAlertsEnabled = true,
                tripRemindersEnabled = true,
                arrivalNotificationsEnabled = false,
                emergencyAlertsEnabled = true,
                quietHoursEnabled = false,
                quietHoursStart = "22:00",
                quietHoursEnd = "07:00"
            ),
            mapSettings = MapSettings(
                mapStyle = MapStyle.NORMAL,
                showTrafficLayer = false,
                showAccessibilityInfo = true,
                showRealTimeVehicles = true,
                defaultZoomLevel = 15.0f
            )
        )
    }
    
    fun getThemeDisplayName(theme: AppTheme): String {
        return when (theme) {
            AppTheme.LIGHT -> "Light Theme"
            AppTheme.DARK -> "Dark Theme"
            AppTheme.SYSTEM_DEFAULT -> "System Default"
        }
    }
    
    fun getLanguageDisplayName(language: Language): String {
        return when (language) {
            Language.ENGLISH -> "English"
            Language.GREEK -> "Ελληνικά"
        }
    }
    
    fun getMapStyleDisplayName(style: MapStyle): String {
        return when (style) {
            MapStyle.NORMAL -> "Normal"
            MapStyle.SATELLITE -> "Satellite"
            MapStyle.TERRAIN -> "Terrain"
            MapStyle.HYBRID -> "Hybrid"
        }
    }
    
    fun getAllThemes(): List<AppTheme> = AppTheme.values().toList()
    
    fun getAllLanguages(): List<Language> = Language.values().toList()
    
    fun getAllMapStyles(): List<MapStyle> = MapStyle.values().toList()
    
    fun exportSettings(): String {
        val settings = getCurrentSettings()
        return buildString {
            appendLine("Athens Plus Settings Export")
            appendLine("Theme: ${getThemeDisplayName(settings.theme)}")
            appendLine("Language: ${getLanguageDisplayName(settings.language)}")
            appendLine("Service Alerts: ${settings.notificationSettings.serviceAlertsEnabled}")
            appendLine("Trip Reminders: ${settings.notificationSettings.tripRemindersEnabled}")
            appendLine("Map Style: ${getMapStyleDisplayName(settings.mapSettings.mapStyle)}")
            appendLine("Traffic Layer: ${settings.mapSettings.showTrafficLayer}")
            appendLine("Real-time Vehicles: ${settings.mapSettings.showRealTimeVehicles}")
        }
    }
    
    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_SERVICE_ALERTS = "service_alerts_enabled"
        private const val KEY_TRIP_REMINDERS = "trip_reminders_enabled"
        private const val KEY_ARRIVAL_NOTIFICATIONS = "arrival_notifications_enabled"
        private const val KEY_EMERGENCY_ALERTS = "emergency_alerts_enabled"
        private const val KEY_QUIET_HOURS = "quiet_hours_enabled"
        private const val KEY_QUIET_START = "quiet_hours_start"
        private const val KEY_QUIET_END = "quiet_hours_end"
        private const val KEY_MAP_STYLE = "map_style"
        private const val KEY_SHOW_TRAFFIC = "show_traffic_layer"
        private const val KEY_SHOW_ACCESSIBILITY = "show_accessibility_info"
        private const val KEY_SHOW_REAL_TIME = "show_real_time_vehicles"
        private const val KEY_DEFAULT_ZOOM = "default_zoom_level"
    }
} 