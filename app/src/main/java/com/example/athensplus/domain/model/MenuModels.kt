package com.example.athensplus.domain.model

data class AppSettings(
    val theme: AppTheme,
    val language: Language,
    val notificationSettings: NotificationSettings
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM_DEFAULT
}

enum class Language {
    ENGLISH, GREEK
}

data class NotificationSettings(
    val serviceAlertsEnabled: Boolean,
    val tripRemindersEnabled: Boolean,
    val arrivalNotificationsEnabled: Boolean,
    val emergencyAlertsEnabled: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStart: String,
    val quietHoursEnd: String
) 