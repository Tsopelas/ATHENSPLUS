@file:Suppress("unused", "RedundantSuppression", "RedundantSuppression")

package com.example.athensplus.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.athensplus.R
import com.example.athensplus.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class SettingsService(context: Context) {
    private val appContext: Context = context.applicationContext
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "athens_plus_settings", Context.MODE_PRIVATE
    )
    
    private val _settingsFlow = MutableStateFlow(loadSettings())
    
    suspend fun updateTheme(theme: AppTheme) = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(theme = theme)
    }
    
    suspend fun updateLanguage(language: Language) = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(language = language)
    }
    
    private suspend fun updateNotificationSettings(
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
    

    
    fun getCurrentSettings(): AppSettings = _settingsFlow.value
    
    fun getTheme(): AppTheme = _settingsFlow.value.theme
    
    fun getLanguage(): Language = _settingsFlow.value.language
    

    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            theme = AppTheme.valueOf(
                preferences.getString(KEY_THEME, AppTheme.SYSTEM_DEFAULT.name)
                    ?: AppTheme.SYSTEM_DEFAULT.name
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
            )
        )
    }
    
    fun getThemeDisplayName(theme: AppTheme): String {
        return when (theme) {
            AppTheme.LIGHT -> appContext.getString(R.string.theme_light)
            AppTheme.DARK -> appContext.getString(R.string.theme_dark)
            AppTheme.SYSTEM_DEFAULT -> appContext.getString(R.string.theme_system)
        }
    }
    
    fun getLanguageDisplayName(language: Language): String {
        return when (language) {
            Language.ENGLISH -> "English"
            Language.GREEK -> "Ελληνικά"
        }
    }
    
    fun getAllThemes(): List<AppTheme> = AppTheme.entries.toList()
    
    fun getAllLanguages(): List<Language> = Language.entries.toList()

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
    }
} 