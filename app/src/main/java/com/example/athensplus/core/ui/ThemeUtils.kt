package com.example.athensplus.core.ui

import androidx.appcompat.app.AppCompatDelegate
import com.example.athensplus.domain.model.AppTheme

object ThemeUtils {
    fun applyTheme(appTheme: AppTheme) {
        val mode = when (appTheme) {
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.SYSTEM_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}


