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
    
    companion object {
        private const val TAG = "MapSetupManager"
    }
    
    private var onMapReadyCallback: ((GoogleMap) -> Unit)? = null
    
    fun setupMap(onMapReady: (GoogleMap) -> Unit) {
        onMapReadyCallback = onMapReady
        Log.d(TAG, "Setup map called with API key: ${com.example.athensplus.BuildConfig.GOOGLE_MAPS_API_KEY.take(10)}...")
        Log.d(TAG, "Full API Key: ${com.example.athensplus.BuildConfig.GOOGLE_MAPS_API_KEY}")
        Log.d(TAG, "Package name: ${fragment.requireContext().packageName}")
        
        // Log additional debugging info
        try {
            val packageInfo = fragment.requireContext().packageManager.getPackageInfo(
                fragment.requireContext().packageName, 0
            )
            Log.d(TAG, "App version: ${packageInfo.versionName}")
            Log.d(TAG, "App version code: ${packageInfo.versionCode}")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package info", e)
        }
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        try {
            Log.d(TAG, "onMapReady called - Map loaded successfully!")
            Log.d(TAG, "API Key being used: ${com.example.athensplus.BuildConfig.GOOGLE_MAPS_API_KEY}")
            setupMapStyle(googleMap)
            setupMapSettings(googleMap)
            setupInitialCamera(googleMap)
            setupLocationUI(googleMap)
            
            onMapReadyCallback?.invoke(googleMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up map", e)
            Log.e(TAG, "Exception details: ${e.message}")
            Log.e(TAG, "Exception stack trace: ${e.stackTraceToString()}")
        }
    }
    
    private fun setupMapStyle(googleMap: GoogleMap) {
        try {
            val style = MapStyleOptions.loadRawResourceStyle(fragment.requireContext(), R.raw.map_style)
            googleMap.setMapStyle(style)
            Log.d("MapSetupManager", "Map style loaded successfully")
        } catch (e: Exception) {
            Log.w("MapSetupManager", "Failed to load map style", e)
        }
    }
    
    private fun setupMapSettings(googleMap: GoogleMap) {
        try {
            Log.d(TAG, "Setting up map settings")
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
            Log.d(TAG, "Map settings configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up map settings", e)
        }
    }
    
    private fun setupInitialCamera(googleMap: GoogleMap) {
        try {
            val ethnikiAmyna = LatLng(38.00031275851294, 23.78568239514545)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ethnikiAmyna, 10.5f))
            Log.d("MapSetupManager", "Initial camera position set successfully")
        } catch (e: Exception) {
            Log.e("MapSetupManager", "Error setting initial camera position", e)
        }
    }
    
    private fun setupMapListeners(googleMap: GoogleMap) {
        // todo
    }
    
    private fun setupLocationUI(googleMap: GoogleMap) {
        locationService.setupLocationUI(googleMap)
    }
} 