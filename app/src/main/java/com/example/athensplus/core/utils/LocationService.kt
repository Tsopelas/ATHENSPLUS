package com.example.athensplus.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationService(private val fragment: Fragment) {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationPermissionGranted = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    fun initialize() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(fragment.requireActivity())
        checkLocationPermission()
    }

    fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun setupLocationUI(googleMap: GoogleMap?) {
        if (googleMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                googleMap.isMyLocationEnabled = false
                googleMap.uiSettings.isMyLocationButtonEnabled = false
            }
        } catch (e: SecurityException) {
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Suppress unused parameter warning
        @Suppress("UNUSED_PARAMETER")
        val _permissions = permissions
        locationPermissionGranted = false
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                }
            }
        }
    }

    fun isLocationPermissionGranted(): Boolean = locationPermissionGranted

    suspend fun getCurrentLocation(): LatLng? {
        return if (locationPermissionGranted && fusedLocationClient != null) {
            try {
                // Check permission before accessing location
                if (ContextCompat.checkSelfPermission(
                        fragment.requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    suspendCancellableCoroutine { continuation ->
                        fusedLocationClient!!.lastLocation
                            .addOnSuccessListener { location ->
                                if (location != null) {
                                    continuation.resume(LatLng(location.latitude, location.longitude))
                                } else {
                                    continuation.resume(null)
                                }
                            }
                            .addOnFailureListener {
                                continuation.resume(null)
                            }
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
} 