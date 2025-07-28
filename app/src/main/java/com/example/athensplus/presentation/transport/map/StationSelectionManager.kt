package com.example.athensplus.presentation.transport.map

import android.graphics.Color
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.core.utils.MapManager
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class StationSelectionManager(
    private val fragment: Fragment,
    private val mapManager: MapManager,
    private val stationManager: StationManager
) {
    
    private var startStationMarker: Marker? = null
    private var endStationMarker: Marker? = null
    
    fun updateMarkers(
        googleMap: GoogleMap?,
        zoom: Float,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?,
        selectedLine: String
    ) {
        mapManager.clearMarkers()
        val showAll = zoom >= 12.5f
        if (!showAll) return

        fun addMarkers(line: List<MetroStation>, color: Int) {
            val existingLabels = mutableListOf<Pair<com.google.android.gms.maps.model.LatLng, String>>()

            line.forEachIndexed { index, station ->
                val interchangeStation = if (selectedStartStation != null && selectedEndStation != null) {
                    stationManager.findInterchangeStation(selectedStartStation!!, selectedEndStation!!)
                } else null

                if (selectedStartStation != null && selectedEndStation != null) {
                    // Check if this station is on the route
                    val isOnRoute = if (interchangeStation != null) {
                        // For routes with interchange, use the new method
                        isStationOnRouteWithInterchange(station, selectedStartStation!!, selectedEndStation!!, interchangeStation)
                    } else {
                        // For direct routes, check if station is between start and end
                        stationManager.isStationOnRoute(station, selectedStartStation!!, selectedEndStation!!)
                    }
                    
                    // Only show stations that are on the route, or are the selected stations, or are the interchange station
                    if (!isOnRoute && station != selectedStartStation && station != selectedEndStation && station != interchangeStation) {
                        return@forEachIndexed
                    }
                }

                val isSelected = station == selectedStartStation || station == selectedEndStation || station == interchangeStation
                val isFirstOrLast = (index == 0 || index == line.size - 1)

                val circleMarker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(station.coords)
                        .title("${station.nameGreek} / ${station.nameEnglish}")
                        .icon(mapManager.createStationMarker(color, isSelected = isSelected))
                        .anchor(0.5f, 0.5f)
                        .zIndex(if (isSelected) 3f else if (station.isInterchange) 2f else 1f)
                )

                circleMarker?.let {
                    mapManager.addMarker(it)
                    when (station) {
                        selectedStartStation -> startStationMarker = it
                        selectedEndStation -> endStationMarker = it
                    }
                }

                val textPosition = mapManager.findTextPosition(
                    station.coords,
                    station.nameEnglish,
                    existingLabels,
                    listOf(StationData.line1CurvedPoints, StationData.line2CurvedPoints, StationData.line3CurvedPoints)
                )

                val textMarker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(textPosition)
                        .icon(mapManager.createStationLabel(station, color, 0f, isFirstOrLast))
                        .anchor(0f, 0.5f)
                        .zIndex(1f)
                )

                textMarker?.let {
                    mapManager.addMarker(it)
                }

                existingLabels.add(Pair(textPosition, station.nameEnglish))
            }
        }

        when (selectedLine) {
            "All Lines" -> {
                addMarkers(StationData.metroLine1, Color.parseColor("#009640"))
                addMarkers(StationData.metroLine2, Color.parseColor("#e30613"))
                addMarkers(StationData.metroLine3, Color.parseColor("#0057a8"))
            }
            "Line 1" -> addMarkers(StationData.metroLine1, Color.parseColor("#009640"))
            "Line 2" -> addMarkers(StationData.metroLine2, Color.parseColor("#e30613"))
            "Line 3" -> addMarkers(StationData.metroLine3, Color.parseColor("#0057a8"))
        }
    }
    
    fun showStationMenu(
        marker: Marker,
        googleMap: GoogleMap,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?,
        onStationSelected: (MetroStation) -> Unit,
        onStationDeselected: (MetroStation) -> Unit,
        onShowTimetable: (MetroStation) -> Unit,
        onShowAirportRoute: (MetroStation) -> Unit,
        onShowHarborRoute: (MetroStation) -> Unit
    ) {
        val station = findStationByMarker(marker)
        if (station == null) return

        val popupView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.station_menu_layout, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            isTouchable = true
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        // Set station names
        val stationNameGreek = popupView.findViewById<TextView>(R.id.station_name_greek)
        val stationNameEnglish = popupView.findViewById<TextView>(R.id.station_name_english)
        stationNameGreek.text = station.nameGreek
        stationNameEnglish.text = station.nameEnglish

        // Get line color for this station
        val lineColor = getStationLineColor(station)
        stationNameGreek.setTextColor(lineColor)
        stationNameEnglish.setTextColor(lineColor)

        setupStationMenuButtons(
            popupView, station, selectedStartStation, selectedEndStation,
            onStationSelected, onStationDeselected, onShowTimetable, onShowAirportRoute, onShowHarborRoute,
            popupWindow
        )

        // Position popup near the marker
        val markerPosition = marker.position
        val screenLocation = googleMap.projection.toScreenLocation(markerPosition)
        
        // Measure the popup view to get its dimensions
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        // Calculate popup position to appear above the marker
        val popupX = screenLocation.x - (popupView.measuredWidth / 2)
        val popupY = screenLocation.y - popupView.measuredHeight - 50 // 50px above marker

        // Get the map view from the fragment
        val mapView = fragment.view?.findViewById<com.google.android.gms.maps.MapView>(com.example.athensplus.R.id.mapView)
        
        if (mapView != null) {
            // Get the map view's position relative to the fragment root
            val mapViewLocation = IntArray(2)
            mapView.getLocationInWindow(mapViewLocation)
            
            // Adjust coordinates to account for map view's position
            val adjustedX = popupX + mapViewLocation[0]
            val adjustedY = popupY + mapViewLocation[1]
            
            // Position relative to the fragment root with adjusted coordinates
            popupWindow.showAtLocation(fragment.view, Gravity.NO_GRAVITY, adjustedX, adjustedY)
        } else {
            // Fallback to fragment root if map view not found
            popupWindow.showAtLocation(fragment.view, Gravity.NO_GRAVITY, popupX, popupY)
        }
    }
    
    private fun setupStationMenuButtons(
        popupView: View,
        station: MetroStation,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?,
        onStationSelected: (MetroStation) -> Unit,
        onStationDeselected: (MetroStation) -> Unit,
        onShowTimetable: (MetroStation) -> Unit,
        onShowAirportRoute: (MetroStation) -> Unit,
        onShowHarborRoute: (MetroStation) -> Unit,
        popupWindow: PopupWindow
    ) {
        val startButton = popupView.findViewById<CardView>(R.id.start_button)
        val timetableButton = popupView.findViewById<CardView>(R.id.timetable_button)
        val airportButton = popupView.findViewById<CardView>(R.id.airport_button)
        val harborButton = popupView.findViewById<CardView>(R.id.harbor_button)

        // Debug: Check if buttons are found
        android.util.Log.d("StationSelectionManager", "Start button: ${startButton != null}")
        android.util.Log.d("StationSelectionManager", "Timetable button: ${timetableButton != null}")
        android.util.Log.d("StationSelectionManager", "Airport button: ${airportButton != null}")
        android.util.Log.d("StationSelectionManager", "Harbor button: ${harborButton != null}")

        // Get line color for this station
        val lineColor = getStationLineColor(station)

        // Set button background colors to match the line
        startButton?.getChildAt(0)?.let { (it as LinearLayout).setBackgroundColor(lineColor) }
        timetableButton?.getChildAt(0)?.let { (it as LinearLayout).setBackgroundColor(lineColor) }
        airportButton?.getChildAt(0)?.let { (it as LinearLayout).setBackgroundColor(lineColor) }
        harborButton?.getChildAt(0)?.let { (it as LinearLayout).setBackgroundColor(lineColor) }

        startButton?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Start button clicked")
            onStationSelected(station)
            popupWindow.dismiss()
        }

        timetableButton?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Timetable button clicked")
            onShowTimetable(station)
            popupWindow.dismiss()
        }

        airportButton?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Airport button clicked")
            onShowAirportRoute(station)
            popupWindow.dismiss()
        }

        harborButton?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Harbor button clicked")
            onShowHarborRoute(station)
            popupWindow.dismiss()
        }

        // Also set click listeners on the inner LinearLayouts to ensure touch events are captured
        startButton?.getChildAt(0)?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Start button inner clicked")
            onStationSelected(station)
            popupWindow.dismiss()
        }

        timetableButton?.getChildAt(0)?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Timetable button inner clicked")
            onShowTimetable(station)
            popupWindow.dismiss()
        }

        airportButton?.getChildAt(0)?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Airport button inner clicked")
            onShowAirportRoute(station)
            popupWindow.dismiss()
        }

        harborButton?.getChildAt(0)?.setOnClickListener {
            android.util.Log.d("StationSelectionManager", "Harbor button inner clicked")
            onShowHarborRoute(station)
            popupWindow.dismiss()
        }
    }
    
    private fun findStationByMarker(marker: Marker): MetroStation? {
        val allStations = StationData.metroLine1 + StationData.metroLine2 + StationData.metroLine3
        return allStations.find { it.coords == marker.position }
    }
    
    private fun getStationLineColor(station: MetroStation): Int {
        return when {
            StationData.metroLine1.contains(station) -> Color.parseColor("#009640") // Green for Line 1
            StationData.metroLine2.contains(station) -> Color.parseColor("#e30613") // Red for Line 2
            StationData.metroLine3.contains(station) -> Color.parseColor("#0057a8") // Blue for Line 3
            else -> Color.parseColor("#663399") // Default purple
        }
    }
    
    private fun isStationOnRouteWithInterchange(
        station: MetroStation,
        startStation: MetroStation,
        endStation: MetroStation,
        interchangeStation: MetroStation
    ): Boolean {
        val isOnFirstSegment = stationManager.isStationOnRoute(station, startStation, interchangeStation)
        val isOnSecondSegment = stationManager.isStationOnRoute(station, interchangeStation, endStation)

        if (station == startStation || station == endStation || station == interchangeStation) {
            return true
        }
        
        return isOnFirstSegment || isOnSecondSegment
    }
    
    fun clearMarkers() {
        mapManager.clearMarkers()
        startStationMarker = null
        endStationMarker = null
    }
} 