package com.example.athensplus.presentation.common

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.BuildConfig
import com.example.athensplus.R
import com.example.athensplus.core.ui.MapStyleUtils
import com.example.athensplus.core.ui.MapUiUtils
import com.example.athensplus.core.utils.WalkingDirectionsService
import com.example.athensplus.domain.model.TransitStep
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch

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
        android.util.Log.d("ExpandableStepManager", "Setting up expandable step for: ${step.instruction} (mode: ${step.mode})")
        
        // Set initial state
        expandedContentContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        updateArrowRotation(expandArrow, isExpanded)
        
        // Ensure the arrow is visible
        expandArrow.visibility = View.VISIBLE
        
        // Make both the step container and arrow clickable for better UX
        stepContainer.isClickable = true
        stepContainer.isFocusable = true
        expandArrow.isClickable = true
        expandArrow.isFocusable = true
        
        val clickListener = View.OnClickListener {
            android.util.Log.d("ExpandableStepManager", "Step clicked! Current visibility: ${expandedContentContainer.visibility}")
            val wasExpanded = expandedContentContainer.visibility == View.VISIBLE
            android.util.Log.d("ExpandableStepManager", "Was expanded: $wasExpanded, toggling to: ${!wasExpanded}")
            toggleExpansion(expandArrow, expandedContentContainer, !wasExpanded)
            
            if (!wasExpanded) {
                android.util.Log.d("ExpandableStepManager", "Setting up expanded content for step mode: ${step.mode}")
                setupExpandedContent(expandedContentContainer, step)
            }
        }
        
        // Set click listener on both the step container and arrow
        stepContainer.setOnClickListener(clickListener)
        expandArrow.setOnClickListener(clickListener)
    }
    
    private fun toggleExpansion(
        expandArrow: ImageView,
        expandedContentContainer: LinearLayout,
        shouldExpand: Boolean
    ) {
        if (shouldExpand) {
            expandedContentContainer.visibility = View.VISIBLE
            animateArrowRotation(expandArrow, 180f)
            
            // Also show the separator line
            val separatorLine = expandedContentContainer.parent?.let { parent ->
                (parent as? android.view.ViewGroup)?.findViewById<View>(com.example.athensplus.R.id.separator_line)
            }
            separatorLine?.visibility = View.VISIBLE
        } else {
            expandedContentContainer.visibility = View.GONE
            animateArrowRotation(expandArrow, 0f)
            
            // Also hide the separator line
            val separatorLine = expandedContentContainer.parent?.let { parent ->
                (parent as? android.view.ViewGroup)?.findViewById<View>(com.example.athensplus.R.id.separator_line)
            }
            separatorLine?.visibility = View.GONE
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
        
        // Make the map container square (1:1 aspect ratio)
        walkingMapContainer?.post {
            val width = walkingMapContainer.width
            val layoutParams = walkingMapContainer.layoutParams
            layoutParams.height = width
            walkingMapContainer.layoutParams = layoutParams
        }
        
        val mapView = walkingMapContainer?.findViewById<MapView>(R.id.walking_directions_map)
        mapView?.let { setupWalkingMap(it, step) }
    }
    
    private fun setupWalkingMap(mapView: MapView, step: TransitStep) {
        mapView.onCreate(null)
        mapView.getMapAsync { googleMap ->
            try {
                // Configure UI settings for walking directions map
                googleMap.uiSettings.apply {
                    isZoomControlsEnabled = false
                    isCompassEnabled = false
                    isMyLocationButtonEnabled = false
                    isMapToolbarEnabled = false
                    isIndoorLevelPickerEnabled = false
                }
                
                // Always use normal map type to ensure road names and all features are visible
                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                
                // Detect current theme and apply appropriate styling
                val currentNightMode = fragment.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isDarkMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                
                if (isDarkMode) {
                    // Dark mode: Apply custom dark theme that preserves road names
                    try {
                        val style = com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                            fragment.requireContext(), 
                            R.raw.map_style_walking_directions
                        )
                        googleMap.setMapStyle(style)
                    } catch (e: Exception) {
                        android.util.Log.e("ExpandableStepManager", "Error applying walking directions map style", e)
                        // Fallback to default dark theme
                        MapStyleUtils.applyAppThemeMapStyle(fragment.requireContext(), googleMap)
                    }
                }
                // Light mode: No additional styling needed, uses default light Google Maps
                
                // Check if we have actual coordinates for the walking step
                if (step.startLocation != null && step.endLocation != null) {
                    android.util.Log.d("ExpandableStepManager", "Using actual coordinates for walking route: (${step.startLocation.latitude}, ${step.startLocation.longitude}) to (${step.endLocation.latitude}, ${step.endLocation.longitude})")
                    
                    // Create walking directions service
                    val walkingService = WalkingDirectionsService(BuildConfig.GOOGLE_MAPS_API_KEY)
                    
                    // Get and display walking route using coordinates
                    fragment.lifecycleScope.launch {
                        try {
                            android.util.Log.d("ExpandableStepManager", "Calling walking directions API with coordinates...")
                            val walkingRoute = walkingService.getWalkingRoute(step.startLocation, step.endLocation)
                            
                            if (walkingRoute != null) {
                                android.util.Log.d("ExpandableStepManager", "Walking route received successfully: ${walkingRoute.polylinePoints.size} points")
                                // Draw the route on the map
                                drawWalkingRoute(googleMap, walkingRoute, step)
                            } else {
                                android.util.Log.w("ExpandableStepManager", "Walking route API returned null, using fallback")
                                // Fallback to showing basic markers if route fetch fails
                                showFallbackMap(googleMap, step.startLocation, step.endLocation)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ExpandableStepManager", "Error fetching walking route", e)
                            showFallbackMap(googleMap, step.startLocation, step.endLocation)
                        }
                    }
                } else {
                                    // Fallback to address-based geocoding if coordinates are not available
                val (startLocation, endLocation) = extractWalkingLocations(step)
                
                // Add "Athens, Greece" to make addresses more specific
                val startLocationWithCity = if (startLocation != "Current Location") "$startLocation, Athens, Greece" else "Athens, Greece"
                val endLocationWithCity = "$endLocation, Athens, Greece"
                
                android.util.Log.d("ExpandableStepManager", "Using address geocoding for walking route from '$startLocationWithCity' to '$endLocationWithCity'")
                
                // Create walking directions service
                val walkingService = WalkingDirectionsService(BuildConfig.GOOGLE_MAPS_API_KEY)
                
                // Get and display walking route
                fragment.lifecycleScope.launch {
                    try {
                        android.util.Log.d("ExpandableStepManager", "Calling walking directions API with addresses...")
                        val walkingRoute = walkingService.getWalkingRoute(startLocationWithCity, endLocationWithCity)
                            
                            if (walkingRoute != null) {
                                android.util.Log.d("ExpandableStepManager", "Walking route received successfully: ${walkingRoute.polylinePoints.size} points")
                                // Draw the route on the map
                                drawWalkingRoute(googleMap, walkingRoute, step)
                            } else {
                                android.util.Log.w("ExpandableStepManager", "Walking route API returned null, using fallback")
                                // Fallback to showing basic markers if route fetch fails
                                showFallbackMap(googleMap, startLocationWithCity, endLocationWithCity)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ExpandableStepManager", "Error fetching walking route", e)
                            showFallbackMap(googleMap, startLocationWithCity, endLocationWithCity)
                        }
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ExpandableStepManager", "Error setting up walking map", e)
            }
        }
        
        mapView.onResume()
    }
    
    private fun extractWalkingLocations(step: TransitStep): Pair<String, String> {
        // For walking steps, we need to extract the actual locations from the instruction
        val instruction = step.instruction.lowercase()
        
        return when {
            // If it's a "Walk to X" instruction, extract the destination
            instruction.contains("walk to ") -> {
                val destination = instruction.substringAfter("walk to ").trim()
                // Try to get the actual departure stop name if available
                val startLocation = step.departureStop ?: "Current Location"
                Pair(startLocation, destination)
            }
            // If it's a "Walk (X km)" instruction, we need to infer from context
            instruction.contains("walk (") -> {
                // Try to get actual stop names from the step
                val startLocation = step.departureStop ?: "Current Location"
                val endLocation = step.arrivalStop ?: "Next Stop"
                Pair(startLocation, endLocation)
            }
            // Default fallback
            else -> {
                val startLocation = step.departureStop ?: "Current Location"
                val endLocation = step.arrivalStop ?: "Destination"
                Pair(startLocation, endLocation)
            }
        }
    }
    
    private fun drawWalkingRoute(googleMap: GoogleMap, route: com.example.athensplus.core.utils.WalkingRoute, step: TransitStep? = null) {
        try {
            // Add custom start marker (station-style with purple outline)
            googleMap.addMarker(
                MarkerOptions()
                    .position(route.startLocation)
                    .title("Start")
                    .snippet("Walking starts here")
                    .icon(createStationStyleMarker(0xFF663399.toInt())) // Purple color matching route line
                    .anchor(0.5f, 0.5f) // Center the marker on the position
                    .zIndex(2f) // Ensure markers appear above the route line
            )
            
            // Add custom end marker based on destination type
            val endMarkerIcon = getEndMarkerIcon(route.endLocation, step)
            googleMap.addMarker(
                MarkerOptions()
                    .position(route.endLocation)
                    .title("Destination") 
                    .snippet("${route.duration} • ${route.distance}")
                    .icon(endMarkerIcon)
                    .anchor(0.5f, 0.5f) // Center the marker on the position
                    .zIndex(2f) // Ensure markers appear above the route line
            )
            
            // Draw the walking route polyline
            if (route.polylinePoints.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(route.polylinePoints)
                    .color(0xFF663399.toInt()) // Purple color matching app theme
                    .width(12f) // Thicker line for better visibility
                    .geodesic(true)
                
                googleMap.addPolyline(polylineOptions)
                
                // Adjust camera to show the entire route
                val boundsBuilder = LatLngBounds.Builder()
                route.polylinePoints.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                
                val padding = 80 // pixels
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } else {
                // If no polyline points, just focus on start/end
                val boundsBuilder = LatLngBounds.Builder()
                boundsBuilder.include(route.startLocation)
                boundsBuilder.include(route.endLocation)
                val bounds = boundsBuilder.build()
                
                val padding = 80
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            }
            
            android.util.Log.d("ExpandableStepManager", "Successfully drew walking route with ${route.polylinePoints.size} points")
            
        } catch (e: Exception) {
            android.util.Log.e("ExpandableStepManager", "Error drawing walking route", e)
        }
    }
    
    private fun showFallbackMap(googleMap: GoogleMap, startLocation: String, endLocation: String) {
        // Try to geocode the locations for better fallback
        fragment.lifecycleScope.launch {
            try {
                val walkingService = WalkingDirectionsService(BuildConfig.GOOGLE_MAPS_API_KEY)
                
                // Try to geocode the addresses
                val startCoords = walkingService.geocodeAddress(startLocation)
                val endCoords = walkingService.geocodeAddress(endLocation)
                
                if (startCoords != null && endCoords != null) {
                    // Show markers at the geocoded locations
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(startCoords)
                            .title("Start")
                            .snippet(startLocation)
                            .icon(createStationStyleMarker(0xFF663399.toInt())) // Purple color matching route line
                            .anchor(0.5f, 0.5f) // Center the marker on the position
                            .zIndex(2f) // Ensure markers appear above the route line
                    )
                    
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(endCoords)
                            .title("Destination")
                            .snippet(endLocation)
                            .icon(createStationStyleMarker(0xFF663399.toInt())) // Purple color matching route line
                            .anchor(0.5f, 0.5f) // Center the marker on the position
                            .zIndex(2f) // Ensure markers appear above the route line
                    )
                    
                    // Adjust camera to show both points
                    val boundsBuilder = LatLngBounds.Builder()
                    boundsBuilder.include(startCoords)
                    boundsBuilder.include(endCoords)
                    val bounds = boundsBuilder.build()
                    
                    val padding = 80
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    
                    android.util.Log.d("ExpandableStepManager", "Fallback map: Showing geocoded locations")
                } else {
                    // Ultimate fallback to Athens center
                    val athensCenter = LatLng(37.9838, 23.7275)
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(athensCenter)
                            .title("Walking Directions")
                            .snippet("From $startLocation to $endLocation")
                    )
                    
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(athensCenter, 12f))
                    android.util.Log.w("ExpandableStepManager", "Fallback map: Using Athens center")
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpandableStepManager", "Error in fallback map", e)
                
                // Ultimate fallback to Athens center
                val athensCenter = LatLng(37.9838, 23.7275)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(athensCenter)
                        .title("Walking Directions")
                        .snippet("From $startLocation to $endLocation")
                )
                
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(athensCenter, 12f))
            }
        }
    }
    
    private fun showFallbackMap(googleMap: GoogleMap, startLocation: LatLng, endLocation: LatLng) {
        // Show custom markers for start and end points
        googleMap.addMarker(
            MarkerOptions()
                .position(startLocation)
                .title("Start")
                .snippet("Walking starts here")
                .icon(createStationStyleMarker(0xFF663399.toInt())) // Purple color matching route line
                .anchor(0.5f, 0.5f) // Center the marker on the position
                .zIndex(2f) // Ensure markers appear above the route line
        )
        
        // Add custom end marker based on destination type
        val endMarkerIcon = getEndMarkerIcon(endLocation, null)
        googleMap.addMarker(
            MarkerOptions()
                .position(endLocation)
                .title("Destination")
                .snippet("Walking ends here")
                .icon(endMarkerIcon)
        )
        
        // Adjust camera to show both points
        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(startLocation)
        boundsBuilder.include(endLocation)
        val bounds = boundsBuilder.build()
        
        val padding = 80
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        android.util.Log.w("ExpandableStepManager", "Using fallback map display with coordinates")
    }
    
    private fun getEndMarkerIcon(endLocation: LatLng, step: TransitStep?): BitmapDescriptor {
        if (step == null) {
            android.util.Log.d("ExpandableStepManager", "No step provided, using generic destination marker")
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_circle)
        }
        
        val instruction = step.instruction.lowercase()
        
        val markerType = when {
            // Check if walking to a metro station
            instruction.contains("metro") || 
            instruction.contains("metro station") -> {
                "metro_station"
            }
            // Check if walking to a bus station/stop (but not metro)
            instruction.contains("bus") || 
            (instruction.contains("station") && !instruction.contains("metro")) ||
            step.departureStop?.contains("Station", ignoreCase = true) == true -> {
                "bus_station"
            }
            // Check if this is the final destination (arrival)
            // This is typically the last walking step or when no specific transport is mentioned
            step.mode == "WALKING" && step.arrivalStop == null ||
            instruction.contains("arrival") || 
            instruction.contains("destination") ||
            instruction.contains("final") -> {
                "arrival"
            }
            // Default to generic destination marker
            else -> {
                "destination"
            }
        }
        
        android.util.Log.d("ExpandableStepManager", "Using marker type: $markerType for instruction: '${step.instruction}'")
        
        return when (markerType) {
            "metro_station" -> drawableToBitmap(R.drawable.ic_metro)
            "bus_station" -> drawableToBitmap(R.drawable.ic_transport)
            "arrival" -> drawableToBitmap(R.drawable.ic_arrival)
            else -> createStationStyleMarker(0xFF663399.toInt()) // Purple color matching route line
        }
    }
    
    private fun drawableToBitmap(drawableResId: Int): BitmapDescriptor {
        return try {
            val drawable = ContextCompat.getDrawable(fragment.requireContext(), drawableResId)
            if (drawable != null) {
                // Create a smaller bitmap for pin-like appearance
                val size = 32 // Small size for pin-like appearance
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                BitmapDescriptorFactory.fromBitmap(bitmap)
            } else {
                android.util.Log.e("ExpandableStepManager", "Failed to load drawable: $drawableResId")
                BitmapDescriptorFactory.defaultMarker()
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpandableStepManager", "Error converting drawable to bitmap: $drawableResId", e)
            BitmapDescriptorFactory.defaultMarker()
        }
    }
    
    private fun createStationStyleMarker(outlineColor: Int, radius: Float = 20f, outlineWidth: Float = 8f): BitmapDescriptor {
        val size = ((radius + outlineWidth) * 2).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        // Draw outline
        val outlinePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = outlineWidth
            color = outlineColor
        }
        canvas.drawCircle(center, center, radius, outlinePaint)

        // Draw white fill
        val fillPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            color = android.graphics.Color.WHITE
        }
        canvas.drawCircle(center, center, radius - outlineWidth / 2, fillPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
    
    private fun setupTransitDetails(transitDetailsContainer: LinearLayout?, step: TransitStep) {
        transitDetailsContainer?.visibility = View.VISIBLE
        
        // TODO: Implement transit-specific details
        // This could include:

    }
    
    private fun setupGeneralDetails(generalDetailsContainer: LinearLayout?, step: TransitStep) {
        generalDetailsContainer?.visibility = View.VISIBLE
        
        // TODO: Implement general step details

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
