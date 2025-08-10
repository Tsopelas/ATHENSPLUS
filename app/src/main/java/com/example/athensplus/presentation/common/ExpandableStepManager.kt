package com.example.athensplus.presentation.common

import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.core.ui.MapStyleUtils
import com.example.athensplus.core.ui.MapUiUtils
import com.example.athensplus.domain.model.TransitStep
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class ExpandableStepManager(
    private val fragment: Fragment
) {
    
    fun setupExpandableStep(
        stepContainer: LinearLayout,
        expandArrow: ImageView,
        expandedContentContainer: LinearLayout,
        step: TransitStep,
        isExpanded: Boolean = false
    ) {
        // Set initial state
        expandedContentContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        updateArrowRotation(expandArrow, isExpanded)
        
        // Make the arrow clickable and focusable
        expandArrow.isClickable = true
        expandArrow.isFocusable = true
        
        // Set click listener ONLY on the dropdown arrow
        expandArrow.setOnClickListener {
            val wasExpanded = expandedContentContainer.visibility == View.VISIBLE
            toggleExpansion(expandArrow, expandedContentContainer, !wasExpanded)
            
            if (!wasExpanded) {
                setupExpandedContent(expandedContentContainer, step)
            }
        }
    }
    
    private fun toggleExpansion(
        expandArrow: ImageView,
        expandedContentContainer: LinearLayout,
        shouldExpand: Boolean
    ) {
        if (shouldExpand) {
            expandedContentContainer.visibility = View.VISIBLE
            animateArrowRotation(expandArrow, 180f)
        } else {
            expandedContentContainer.visibility = View.GONE
            animateArrowRotation(expandArrow, 0f)
        }
    }
    
    private fun animateArrowRotation(arrow: ImageView, targetRotation: Float) {
        ObjectAnimator.ofFloat(arrow, "rotation", targetRotation).apply {
            duration = 200
            start()
        }
    }
    
    private fun updateArrowRotation(arrow: ImageView, isExpanded: Boolean) {
        arrow.rotation = if (isExpanded) 180f else 0f
    }
    
    private fun setupExpandedContent(expandedContentContainer: LinearLayout, step: TransitStep) {
        val walkingMapContainer = expandedContentContainer.findViewById<View>(R.id.walking_map_container)
        val transitDetailsContainer = expandedContentContainer.findViewById<LinearLayout>(R.id.transit_details_container)
        val generalDetailsContainer = expandedContentContainer.findViewById<LinearLayout>(R.id.general_details_container)
        
        // Hide all containers first
        walkingMapContainer?.visibility = View.GONE
        transitDetailsContainer?.visibility = View.GONE
        generalDetailsContainer?.visibility = View.GONE
        
        when (step.mode) {
            "WALKING" -> {
                setupWalkingDirections(walkingMapContainer, step)
            }
            "TRANSIT", "TRANSIT_DETAIL" -> {
                setupTransitDetails(transitDetailsContainer, step)
            }
            else -> {
                setupGeneralDetails(generalDetailsContainer, step)
            }
        }
    }
    
    private fun setupWalkingDirections(walkingMapContainer: View?, step: TransitStep) {
        walkingMapContainer?.visibility = View.VISIBLE
        
        val mapView = walkingMapContainer?.findViewById<MapView>(R.id.walking_directions_map)
        mapView?.let { setupWalkingMap(it, step) }
    }
    
    private fun setupWalkingMap(mapView: MapView, step: TransitStep) {
        mapView.onCreate(null)
        mapView.getMapAsync { googleMap ->
            try {
                // Apply app theme map style and UI settings
                MapStyleUtils.applyAppThemeMapStyle(fragment.requireContext(), googleMap)
                MapUiUtils.applyDefaultUiSettings(googleMap)
                
                // For now, show a placeholder until we implement walking directions API
                val athensCenter = LatLng(37.9838, 23.7275)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(athensCenter)
                        .title("Walking Directions")
                        .snippet("From ${step.departureStop ?: "Start"} to ${step.arrivalStop ?: "Destination"}")
                )
                
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(athensCenter, 15f))
                
                // TODO: Implement actual walking directions from Google Directions API
                // This would involve:
                // 1. Getting start/end coordinates from addresses
                // 2. Fetching walking route from Google Directions API
                // 3. Drawing polyline on the map
                // 4. Adding markers for start/end points
                
            } catch (e: Exception) {
                android.util.Log.e("ExpandableStepManager", "Error setting up walking map", e)
            }
        }
        
        mapView.onResume()
    }
    
    private fun setupTransitDetails(transitDetailsContainer: LinearLayout?, step: TransitStep) {
        transitDetailsContainer?.visibility = View.VISIBLE
        
        // TODO: Implement transit-specific details
        // This could include:
        // - Real-time arrival information
        // - Alternative lines/routes
        // - Station facilities
        // - Accessibility information
    }
    
    private fun setupGeneralDetails(generalDetailsContainer: LinearLayout?, step: TransitStep) {
        generalDetailsContainer?.visibility = View.VISIBLE
        
        // TODO: Implement general step details
        // This could include:
        // - Step-by-step instructions
        // - Estimated time breakdown
        // - Additional helpful information
    }
    
    fun onResume() {
        // Resume any active maps
    }
    
    fun onPause() {
        // Pause any active maps
    }
    
    fun onDestroy() {
        // Clean up any resources
    }
}
