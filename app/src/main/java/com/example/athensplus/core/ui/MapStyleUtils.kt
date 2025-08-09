package com.example.athensplus.core.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.athensplus.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions

object MapStyleUtils {
    fun applyAppThemeMapStyle(context: Context, googleMap: GoogleMap) {
        val isNight = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> (context.resources.configuration.uiMode and 0x30) == 0x20
        }
        val styleRes = if (isNight) R.raw.map_style /* will be overridden by raw-night/map_style.json at runtime */ else R.raw.map_style
        try {
            val style = MapStyleOptions.loadRawResourceStyle(context, styleRes)
            googleMap.setMapStyle(style)
        } catch (_: Exception) {
            // Ignore if style fails; fallback to default
        }
    }
}


