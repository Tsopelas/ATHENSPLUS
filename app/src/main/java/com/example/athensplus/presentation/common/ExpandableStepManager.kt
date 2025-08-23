package com.example.athensplus.presentation.common

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.BuildConfig
import com.example.athensplus.R
import com.example.athensplus.core.ui.MapStyleUtils
import com.example.athensplus.core.ui.MapUiUtils
import com.example.athensplus.core.utils.WalkingDirectionsService
import com.example.athensplus.core.utils.StationDeparturesService
import com.example.athensplus.core.utils.BusDeparture
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

        expandedContentContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        updateArrowRotation(expandArrow, isExpanded)

        expandArrow.visibility = View.VISIBLE

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

            val separatorLine = expandedContentContainer.parent?.let { parent ->
                (parent as? android.view.ViewGroup)?.findViewById<View>(com.example.athensplus.R.id.separator_line)
            }
            separatorLine?.visibility = View.VISIBLE
        } else {
            expandedContentContainer.visibility = View.GONE
            animateArrowRotation(expandArrow, 0f)

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
                googleMap.uiSettings.apply {
                    isZoomControlsEnabled = false
                    isCompassEnabled = false
                    isMyLocationButtonEnabled = false
                    isMapToolbarEnabled = false
                    isIndoorLevelPickerEnabled = false
                }

                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL

                val currentNightMode = fragment.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isDarkMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                
                if (isDarkMode) {
                    try {
                        val style = com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                            fragment.requireContext(), 
                            R.raw.map_style_walking_directions
                        )
                        googleMap.setMapStyle(style)
                    } catch (e: Exception) {
                        android.util.Log.e("ExpandableStepManager", "Error applying walking directions map style", e)
                        MapStyleUtils.applyAppThemeMapStyle(fragment.requireContext(), googleMap)
                    }
                }

                if (step.startLocation != null && step.endLocation != null) {
                    android.util.Log.d("ExpandableStepManager", "Using actual coordinates for walking route: (${step.startLocation.latitude}, ${step.startLocation.longitude}) to (${step.endLocation.latitude}, ${step.endLocation.longitude})")

                    val walkingService = WalkingDirectionsService(BuildConfig.GOOGLE_MAPS_API_KEY)

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

                val (startLocation, endLocation) = extractWalkingLocations(step)

                val startLocationWithCity = if (startLocation != "Current Location") "$startLocation, Athens, Greece" else "Athens, Greece"
                val endLocationWithCity = "$endLocation, Athens, Greece"
                
                android.util.Log.d("ExpandableStepManager", "Using address geocoding for walking route from '$startLocationWithCity' to '$endLocationWithCity'")

                val walkingService = WalkingDirectionsService(BuildConfig.GOOGLE_MAPS_API_KEY)

                fragment.lifecycleScope.launch {
                    try {
                        android.util.Log.d("ExpandableStepManager", "Calling walking directions API with addresses...")
                        val walkingRoute = walkingService.getWalkingRoute(startLocationWithCity, endLocationWithCity)
                            
                            if (walkingRoute != null) {
                                android.util.Log.d("ExpandableStepManager", "Walking route received successfully: ${walkingRoute.polylinePoints.size} points")
                                drawWalkingRoute(googleMap, walkingRoute, step)
                            } else {
                                android.util.Log.w("ExpandableStepManager", "Walking route API returned null, using fallback")
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
        val instruction = step.instruction.lowercase()
        
        return when {
            instruction.contains("walk to ") -> {
                val destination = instruction.substringAfter("walk to ").trim()
                val startLocation = step.departureStop ?: "Current Location"
                Pair(startLocation, destination)
            }

            instruction.contains("walk (") -> {
                // Try to get actual stop names from the step
                val startLocation = step.departureStop ?: "Current Location"
                val endLocation = step.arrivalStop ?: "Next Stop"
                Pair(startLocation, endLocation)
            }

            else -> {
                val startLocation = step.departureStop ?: "Current Location"
                val endLocation = step.arrivalStop ?: "Destination"
                Pair(startLocation, endLocation)
            }
        }
    }
    
    private fun drawWalkingRoute(googleMap: GoogleMap, route: com.example.athensplus.core.utils.WalkingRoute, step: TransitStep? = null) {
        try {

            googleMap.addMarker(
                MarkerOptions()
                    .position(route.startLocation)
                    .title("Start")
                    .snippet("Walking starts here")
                    .icon(createStationStyleMarker(0xFF663399.toInt()))
                    .anchor(0.5f, 0.5f)
                    .zIndex(2f)
            )

            val endMarkerIcon = getEndMarkerIcon(route.endLocation, step)
            googleMap.addMarker(
                MarkerOptions()
                    .position(route.endLocation)
                    .title("Destination") 
                    .snippet("${route.duration} • ${route.distance}")
                    .icon(endMarkerIcon)
                    .anchor(0.5f, 0.5f)
                    .zIndex(2f)
            )

            if (route.polylinePoints.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(route.polylinePoints)
                    .color(0xFF663399.toInt())
                    .width(12f)
                    .geodesic(true)
                
                googleMap.addPolyline(polylineOptions)

                val boundsBuilder = LatLngBounds.Builder()
                route.polylinePoints.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                
                val padding = 80
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } else {
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

        fragment.lifecycleScope.launch {
            try {
                val walkingService = WalkingDirectionsService(BuildConfig.GOOGLE_MAPS_API_KEY)

                val startCoords = walkingService.geocodeAddress(startLocation)
                val endCoords = walkingService.geocodeAddress(endLocation)
                
                if (startCoords != null && endCoords != null) {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(startCoords)
                            .title("Start")
                            .snippet(startLocation)
                            .icon(createStationStyleMarker(0xFF663399.toInt()))
                            .anchor(0.5f, 0.5f)
                            .zIndex(2f)
                    )
                    
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(endCoords)
                            .title("Destination")
                            .snippet(endLocation)
                            .icon(createStationStyleMarker(0xFF663399.toInt()))
                            .anchor(0.5f, 0.5f)
                            .zIndex(2f)
                    )

                    val boundsBuilder = LatLngBounds.Builder()
                    boundsBuilder.include(startCoords)
                    boundsBuilder.include(endCoords)
                    val bounds = boundsBuilder.build()
                    
                    val padding = 80
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    
                    android.util.Log.d("ExpandableStepManager", "Fallback map: Showing geocoded locations")
                } else {
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
        
        // Clear existing content
        transitDetailsContainer?.removeAllViews()
        
        // Add header
        val headerText = TextView(fragment.requireContext()).apply {
            text = "Real-time departures from ${step.instruction}"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(fragment.requireContext().getColor(R.color.transport_text_on_tinted))
            setPadding(0, 0, 0, 16)
        }
        transitDetailsContainer?.addView(headerText)
        
        // Add loading indicator
        val loadingText = TextView(fragment.requireContext()).apply {
            text = "Loading departures..."
            textSize = 14f
            setTextColor(fragment.requireContext().getColor(R.color.transport_text_on_tinted))
            setPadding(0, 8, 0, 8)
        }
        transitDetailsContainer?.addView(loadingText)
        
        // Fetch and display departures
        fragment.lifecycleScope.launch {
            try {
                val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                val departuresService = StationDeparturesService(apiKey)
                // Use departureStop if available, otherwise fall back to instruction
                val stationName = step.departureStop?.ifEmpty { step.instruction } ?: step.instruction
                android.util.Log.d("ExpandableStepManager", "Fetching departures for station: '$stationName' (instruction: '${step.instruction}', departureStop: '${step.departureStop}')")
                
                var departures = departuresService.getStationDepartures(stationName)
                
                // If no departures found, try with common variations
                if (departures.isEmpty()) {
                    val variations = when {
                        stationName.lowercase().contains("alex") -> listOf("Alexandras", "Alexandras Avenue", "Leoforos Alexandras")
                        stationName.lowercase().contains("evrou") -> listOf("Evrou 36", "Evrou 36 Station", "Evrou 36, Athens")
                        stationName.lowercase().contains("syntagma") -> listOf("Syntagma", "Syntagma Square", "Plateia Syntagmatos")
                        stationName.lowercase().contains("omonia") -> listOf("Omonia", "Omonia Square", "Plateia Omonias")
                        else -> emptyList()
                    }
                    
                    for (variation in variations) {
                        if (departures.isEmpty()) {
                            android.util.Log.d("ExpandableStepManager", "Trying variation: '$variation'")
                            departures = departuresService.getStationDepartures(variation)
                        }
                    }
                }
                
                // Remove loading text
                transitDetailsContainer?.removeView(loadingText)
                
                if (departures.isEmpty()) {
                    val noDeparturesText = TextView(fragment.requireContext()).apply {
                        text = "No departures found for $stationName"
                        textSize = 14f
                        setTextColor(fragment.requireContext().getColor(R.color.transport_text_on_tinted))
                        setPadding(0, 8, 0, 8)
                    }
                    transitDetailsContainer?.addView(noDeparturesText)
                    
                    // Add debug info
                    val debugText = TextView(fragment.requireContext()).apply {
                        text = "Debug: Checked station '$stationName'"
                        textSize = 12f
                        setTextColor(fragment.requireContext().getColor(android.R.color.darker_gray))
                        setPadding(0, 4, 0, 8)
                    }
                    transitDetailsContainer?.addView(debugText)
                } else {
                    // Add each departure
                    departures.forEach { departure ->
                        val departureView = createDepartureView(departure)
                        transitDetailsContainer?.addView(departureView)
                        
                        // Add separator line
                        val separator = View(fragment.requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1
                            )
                            setBackgroundColor(fragment.requireContext().getColor(R.color.bottom_nav_separator))
                            setPadding(0, 8, 0, 8)
                        }
                        transitDetailsContainer?.addView(separator)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpandableStepManager", "Error loading departures", e)
                transitDetailsContainer?.removeView(loadingText)
                
                val errorText = TextView(fragment.requireContext()).apply {
                    text = "Error loading departures"
                    textSize = 14f
                    setTextColor(fragment.requireContext().getColor(android.R.color.holo_red_dark))
                    setPadding(0, 8, 0, 8)
                }
                transitDetailsContainer?.addView(errorText)
            }
        }
    }
    
    private fun createDepartureView(departure: BusDeparture): View {
        val inflater = LayoutInflater.from(fragment.requireContext())
        val view = inflater.inflate(R.layout.item_bus_departure, null)
        
        val busCode = view.findViewById<TextView>(R.id.bus_code)
        val busDirection = view.findViewById<TextView>(R.id.bus_direction)
        val arrivalTime = view.findViewById<TextView>(R.id.arrival_time)
        
        busCode.text = departure.busCode
        busDirection.text = departure.direction
        
        // Format arrival time
        val timeText = when {
            departure.arrivalTime == "Now" -> "Now"
            departure.arrivalTime.endsWith("'") -> departure.arrivalTime
            else -> "${departure.arrivalTime}'"
        }
        arrivalTime.text = timeText
        
        // Add express/night line indicators
        if (departure.isExpress || departure.isNightLine) {
            val suffix = when {
                departure.isExpress && departure.isNightLine -> " (EXPRESS-NIGHTLINE)"
                departure.isExpress -> " (EXPRESS)"
                departure.isNightLine -> " (NIGHT LINE)"
                else -> ""
            }
            busDirection.text = "${departure.direction}$suffix"
        }
        
        return view
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
