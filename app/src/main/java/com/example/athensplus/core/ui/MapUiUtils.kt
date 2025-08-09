package com.example.athensplus.core.ui

import com.google.android.gms.maps.GoogleMap

object MapUiUtils {
    fun applyDefaultUiSettings(googleMap: GoogleMap) {
        googleMap.isTrafficEnabled = false
        googleMap.isBuildingsEnabled = true
        googleMap.isIndoorEnabled = false

        googleMap.uiSettings.apply {
            isMapToolbarEnabled = true
            isCompassEnabled = true
            isZoomControlsEnabled = false // Using custom zoom controls
            isMyLocationButtonEnabled = false // Using custom location button
            isIndoorLevelPickerEnabled = false
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isTiltGesturesEnabled = true
            isRotateGesturesEnabled = true
        }
    }
}


