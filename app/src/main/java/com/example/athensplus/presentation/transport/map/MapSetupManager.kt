package com.example.athensplus.presentation.transport.map

import android.util.Log
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.core.utils.LocationService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions

class MapSetupManager(
    private val fragment: Fragment,
    private val locationService: LocationService
) : OnMapReadyCallback {
    
    private var onMapReadyCallback: ((GoogleMap) -> Unit)? = null
    
    fun setupMap(onMapReady: (GoogleMap) -> Unit) {
        onMapReadyCallback = onMapReady
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        setupMapStyle(googleMap)
        setupMapSettings(googleMap)
        setupInitialCamera(googleMap)
        setupLocationUI(googleMap)
        
        onMapReadyCallback?.invoke(googleMap)
    }
    
    private fun setupMapStyle(googleMap: GoogleMap) {
        try {
            val style = MapStyleOptions.loadRawResourceStyle(fragment.requireContext(), R.raw.map_style)
            googleMap.setMapStyle(style)
        } catch (e: Exception) {
            Log.w("MapSetupManager", "Failed to load map style", e)
        }
    }
    
    private fun setupMapSettings(googleMap: GoogleMap) {
        googleMap.isTrafficEnabled = false
        googleMap.isBuildingsEnabled = true
        googleMap.isIndoorEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.uiSettings.isIndoorLevelPickerEnabled = false
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isTiltGesturesEnabled = false
        googleMap.uiSettings.isRotateGesturesEnabled = false
    }
    
    private fun setupInitialCamera(googleMap: GoogleMap) {
        val ethnikiAmyna = LatLng(38.00031275851294, 23.78568239514545)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ethnikiAmyna, 10.5f))
    }
    
    private fun setupMapListeners(googleMap: GoogleMap) {
        // todo
    }
    
    private fun setupLocationUI(googleMap: GoogleMap) {
        locationService.setupLocationUI(googleMap)
    }
} 