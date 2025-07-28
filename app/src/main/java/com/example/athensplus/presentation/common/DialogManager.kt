package com.example.athensplus.presentation.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.utils.BusTimesImprovementService
import com.example.athensplus.core.utils.FastestRouteService
import com.example.athensplus.core.utils.LocationService
import com.example.athensplus.presentation.transport.directions.MetroDirectionsManager
import com.example.athensplus.core.utils.RouteSelectionMode
import com.example.athensplus.core.utils.RouteSelectionService
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.core.utils.TimetableService
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.example.athensplus.domain.model.TransitStep
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DialogManager(
    private val fragment: Fragment,
    private val locationService: LocationService,
    private val stationManager: StationManager,
    private val timetableService: TimetableService,
    private val metroDirectionsManager: MetroDirectionsManager
) {
    
    fun showDirectionsDialog(destination: String, apiKey: String) {
        try {
            val dialog = Dialog(fragment.requireContext())
            dialog.setContentView(R.layout.dialog_directions)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val displayMetrics = fragment.requireContext().resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val dialogHeight = (screenHeight * 0.8).toInt()
            
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dialogHeight
            )
            
            val stepsContainer = dialog.findViewById<LinearLayout>(R.id.steps_container)
            val closeButton = dialog.findViewById<ImageButton?>(R.id.close_button)
            val editFromLocation = dialog.findViewById<EditText>(R.id.edit_from_location)
            val editToLocation = dialog.findViewById<EditText>(R.id.edit_to_location)
            val summaryText = dialog.findViewById<TextView>(R.id.summary_text)
            val summaryContainer = dialog.findViewById<LinearLayout>(R.id.summary_container)
            val updateButton = dialog.findViewById<ImageButton>(R.id.button_update_to)
            val chooseOnMapButton = dialog.findViewById<LinearLayout>(R.id.button_choose_on_map_to)

            val fastestButton = dialog.findViewById<LinearLayout>(R.id.button_fastest)
            val easiestButton = dialog.findViewById<LinearLayout>(R.id.button_easiest)
            val allRoutesButton = dialog.findViewById<LinearLayout>(R.id.button_all_routes)
            
            if (stepsContainer == null || editFromLocation == null || editToLocation == null || summaryText == null || summaryContainer == null || updateButton == null || chooseOnMapButton == null) {
                Toast.makeText(fragment.context, "Error: Could not find dialog views", Toast.LENGTH_SHORT).show()
                return
            }
            
            val routeSelectionService = RouteSelectionService(fragment.requireContext(), apiKey, locationService)
            val routeSelectionUI = RouteSelectionUI(fragment.requireContext(), fragment.lifecycleScope, routeSelectionService)
            
            setupDirectionsDialogContent(
                dialog,
                stepsContainer,
                editFromLocation,
                editToLocation,
                summaryText,
                summaryContainer,
                updateButton,
                chooseOnMapButton,
                fastestButton,
                easiestButton,
                allRoutesButton,
                destination,
                apiKey,
                routeSelectionUI
            )
            
            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(fragment.context, "Error opening directions dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun showStationDirectionsDialog(selectedStartStation: MetroStation?, selectedEndStation: MetroStation?) {
        try {
            val dialog = Dialog(fragment.requireContext())
            dialog.setContentView(R.layout.dialog_directions)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val displayMetrics = fragment.requireContext().resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val dialogHeight = (screenHeight * 0.8).toInt()
            
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dialogHeight
            )

            val stepsContainer = dialog.findViewById<LinearLayout>(R.id.steps_container)
            val closeButton = dialog.findViewById<ImageButton?>(R.id.close_button)
            val editFromLocation = dialog.findViewById<EditText>(R.id.edit_from_location)
            val editToLocation = dialog.findViewById<EditText>(R.id.edit_to_location)
            val summaryText = dialog.findViewById<TextView>(R.id.summary_text)
            val summaryContainer = dialog.findViewById<LinearLayout>(R.id.summary_container)
            val updateButton = dialog.findViewById<ImageButton>(R.id.button_update_to)
            val chooseOnMapButton = dialog.findViewById<LinearLayout>(R.id.button_choose_on_map_to)
            val continuousJourneyColumnView = dialog.findViewById<MetroLineJourneyColumnView>(R.id.continuous_journey_column_view)

            val fastestButton = dialog.findViewById<LinearLayout>(R.id.button_fastest)
            val easiestButton = dialog.findViewById<LinearLayout>(R.id.button_easiest)
            val allRoutesButton = dialog.findViewById<LinearLayout>(R.id.button_all_routes)
            
            if (stepsContainer == null || editFromLocation == null || editToLocation == null || summaryText == null || summaryContainer == null || updateButton == null || chooseOnMapButton == null) {
                Toast.makeText(fragment.context, "Error: Could not find dialog views", Toast.LENGTH_SHORT).show()
                return
            }
            
            setupStationDirectionsDialogContent(
                dialog,
                stepsContainer,
                editFromLocation,
                editToLocation,
                summaryText,
                summaryContainer,
                updateButton,
                chooseOnMapButton,
                fastestButton,
                easiestButton,
                allRoutesButton,
                continuousJourneyColumnView,
                selectedStartStation,
                selectedEndStation
            )
            
            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(fragment.context, "Error opening station directions dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun showMapSelectionDialog(isSelectingForDestination: Boolean, onLocationSelected: (String) -> Unit) {
        try {
            val dialog = Dialog(fragment.requireContext())
            dialog.setContentView(R.layout.dialog_map_selection)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val mapView = dialog.findViewById<MapView>(R.id.map_selection_view)
            val titleText = dialog.findViewById<TextView>(R.id.map_selection_title)
            val closeButton = dialog.findViewById<ImageButton>(R.id.close_map_button)
            val confirmButton = dialog.findViewById<LinearLayout>(R.id.confirm_location_button)
            val selectedLocationText = dialog.findViewById<TextView>(R.id.selected_location_text)

            var selectedAddress: String? = null

            titleText?.text = if (isSelectingForDestination) "Choose Destination" else "Choose Starting Point"

            setupMapSelectionDialog(
                dialog,
                mapView,
                closeButton,
                confirmButton,
                selectedLocationText,
                selectedAddress,
                onLocationSelected
            )

            dialog.show()
        } catch (e: Exception) {
            Log.e("DialogManager", "Error showing map selection dialog", e)
            Toast.makeText(fragment.requireContext(), "Error opening map selection", Toast.LENGTH_SHORT).show()
        }
    }

    fun showStationTimetable(station: MetroStation) {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            val timetableTables = withContext(Dispatchers.IO) {
                when {
                    StationData.metroLine1.contains(station) -> timetableService.parseLine1Timetable(station)
                    StationData.metroLine2.contains(station) -> timetableService.parseLine2Timetable(station)
                    StationData.metroLine3.contains(station) -> timetableService.parseLine3Timetable(station)
                    else -> emptyList()
                }
            }

            if (timetableTables.isNotEmpty()) {
                val waitTime = if (StationData.metroLine3.contains(station)) {
                    withContext(Dispatchers.Default) {
                        timetableService.parseWaitTime()
                    }
                } else null

                showTimetableDialog(station, timetableTables, waitTime)
            } else {
                Toast.makeText(fragment.context, fragment.getString(R.string.timetable_not_available), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showAirportTimetable(station: MetroStation) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            var stationForTimetable = station
            var instructionText: String? = null

            if (!StationData.metroLine3.contains(station)) {
                val interchangeStation = stationManager.findNearestInterchangeToLine3(station)
                if (interchangeStation != null) {
                    stationForTimetable = interchangeStation
                    instructionText = "Take Metro to ${interchangeStation.nameEnglish} for Airport Line (Directions shown on map)"
                } else {
                    instructionText = "No direct route to Airport Line found."
                }
            }

            val times = timetableService.parseAirportTimetable(stationForTimetable)
            withContext(Dispatchers.Main) {
                if (times.isNotEmpty()) {
                    showAirportTimetableDialog(stationForTimetable, times, instructionText)
                } else {
                    Toast.makeText(fragment.context, fragment.getString(R.string.airport_timetable_not_available), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun showPiraeusGateMap() {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<TextView>(R.id.station_name_greek).text = fragment.getString(R.string.piraeus_greek)
        dialog.findViewById<TextView>(R.id.station_name_english).text = fragment.getString(R.string.piraeus_english)
        dialog.findViewById<ImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        timetableContainer.removeAllViews()

        val messageText = TextView(fragment.requireContext()).apply {
            text = fragment.getString(R.string.piraeus_gate_map)
            textSize = 16f
            setTextColor(Color.parseColor("#009640"))
            gravity = Gravity.CENTER
            setPadding(32, 64, 32, 64)
        }
        timetableContainer.addView(messageText)

        dialog.show()
    }

    private fun setupDirectionsDialogContent(
        dialog: Dialog,
        stepsContainer: LinearLayout,
        editFromLocation: EditText,
        editToLocation: EditText,
        summaryText: TextView,
        summaryContainer: LinearLayout,
        updateButton: ImageButton,
        chooseOnMapButton: LinearLayout,
        fastestButton: LinearLayout?,
        easiestButton: LinearLayout?,
        allRoutesButton: LinearLayout?,
        destination: String,
        apiKey: String,
        routeSelectionUI: RouteSelectionUI
    ) {
        editFromLocation.hint = fragment.getString(R.string.from_my_current_location)
        editFromLocation.setText("")
        editFromLocation.isEnabled = true
        editFromLocation.isFocusableInTouchMode = true
        editFromLocation.setTextColor(Color.parseColor("#663399"))
        editToLocation.setText(destination)
        editToLocation.isEnabled = true
        editToLocation.isFocusableInTouchMode = true
        editToLocation.setTextColor(Color.parseColor("#663399"))

        val refreshRouteSelection: () -> Unit = {
            val fromText = editFromLocation.text.toString().trim().ifEmpty { fragment.getString(R.string.from_my_current_location) }
            val toText = editToLocation.text.toString().trim()
            
            if (fastestButton != null && easiestButton != null && allRoutesButton != null) {
                routeSelectionUI.setupRouteSelectionButtons(
                    fastestButton,
                    easiestButton,
                    allRoutesButton,
                    stepsContainer,
                    fromText,
                    toText
                ) { routes, mode ->
                    displaySelectedRoutes(stepsContainer, routes, mode, dialog.context, summaryContainer, summaryText)
                }
            }
        }

        refreshRouteSelection()
        
        val fetchDirections: () -> Unit = {
            val fromText = editFromLocation.text.toString().trim().ifEmpty { 
                fragment.getString(R.string.from_my_current_location)
            }
            val toText = editToLocation.text.toString().trim()
            
            if (toText.isEmpty()) {
                val errorText = TextView(dialog.context)
                errorText.text = fragment.getString(R.string.please_enter_destination)
                errorText.setTextColor(0xFF663399.toInt())
                errorText.textSize = 16f
                errorText.setPadding(24, 32, 24, 32)
                errorText.gravity = Gravity.CENTER
                stepsContainer.removeAllViews()
                stepsContainer.addView(errorText)
            } else {
                fragment.lifecycleScope.launch {
                    try {
                        stepsContainer.removeAllViews()
                        
                        val loadingText = TextView(dialog.context)
                        loadingText.text = fragment.getString(R.string.finding_fastest_route)
                        loadingText.setTextColor(0xFF663399.toInt())
                        loadingText.textSize = 16f
                        loadingText.setPadding(24, 32, 24, 32)
                        loadingText.gravity = Gravity.CENTER
                        stepsContainer.addView(loadingText)
                        
                        val busTimesImprovementService = BusTimesImprovementService(apiKey, locationService)

                        val alternatives = busTimesImprovementService.getIndustryStandardRoutes(fromText, toText, 5)
                        
                        stepsContainer.removeAllViews()
                        val inflater = LayoutInflater.from(dialog.context)
                        
                        if (alternatives.isNotEmpty()) {
                            val bestAlternative = alternatives.minByOrNull { alternative: BusTimesImprovementService.ImprovedRouteAlternative -> 
                                alternative.waitTime + (alternative.totalDuration.replace(" min", "").toIntOrNull() ?: 0) * 60 
                            } ?: alternatives.first()

                            for ((index, step) in bestAlternative.steps.withIndex()) {
                                val stepView = inflater.inflate(R.layout.item_transit_step_modern, stepsContainer, false)
                                val icon = stepView.findViewById<ImageView>(R.id.step_icon)
                                val instruction = stepView.findViewById<TextView>(R.id.step_instruction)
                                val duration = stepView.findViewById<TextView>(R.id.step_duration)
                                val line = stepView.findViewById<TextView>(R.id.step_line)
                                val connectingLine = stepView.findViewById<View>(R.id.connecting_line)
                                val stepContainer = stepView.findViewById<LinearLayout>(R.id.step_container)
                                val waitTimeContainer = stepView.findViewById<LinearLayout>(R.id.wait_time_container)
                                val waitTimeText = stepView.findViewById<TextView>(R.id.wait_time_text)
                                val nextDepartureText = stepView.findViewById<TextView>(R.id.next_departure_text)
                                
                                @Suppress("DEPRECATION")
                                instruction.text = Html.fromHtml(step.instruction)
                                duration.text = step.duration
                                
                                if (!step.line.isNullOrEmpty()) {
                                    line.text = step.line
                                    line.visibility = View.VISIBLE
                                } else {
                                    line.visibility = View.GONE
                                }
                                
                                if (index == bestAlternative.steps.size - 1) {
                                    connectingLine.visibility = View.GONE
                                }
                                
                                icon.setImageResource(when {
                                    step.mode == "WALKING" -> R.drawable.ic_walking
                                    step.mode == "TRANSIT" && step.vehicleType?.equals("BUS", ignoreCase = true) == true -> R.drawable.ic_transport
                                    step.mode == "TRANSIT" && step.vehicleType?.equals("TRAM", ignoreCase = true) == true -> R.drawable.ic_tram
                                    step.mode == "TRANSIT" && step.vehicleType?.equals("SUBWAY", ignoreCase = true) == true -> R.drawable.ic_metro
                                    step.mode == "TRANSIT" -> R.drawable.ic_metro
                                    else -> R.drawable.ic_walking
                                })

                                if (step.mode == "TRANSIT") {
                                    stepContainer.setOnClickListener {
                                        if (waitTimeContainer.visibility == View.VISIBLE) {
                                            waitTimeContainer.visibility = View.GONE
                                        } else {
                                            waitTimeContainer.visibility = View.VISIBLE
                                            showWaitTimeInfo(step, waitTimeText, nextDepartureText)
                                        }
                                    }
                                }
                                
                                stepsContainer.addView(stepView)
                            }

                        } else {
                            summaryContainer.visibility = View.GONE
                            val errorText = TextView(dialog.context)
                            errorText.text = fragment.getString(R.string.no_directions_found)
                            errorText.setTextColor(0xFF663399.toInt())
                            errorText.textSize = 16f
                            errorText.setPadding(24, 32, 24, 32)
                            errorText.gravity = Gravity.CENTER
                            stepsContainer.addView(errorText)
                        }
                    } catch (e: Exception) {
                        stepsContainer.removeAllViews()
                        val errorText = TextView(dialog.context)
                        errorText.text = fragment.getString(R.string.error_finding_directions, e.message ?: "Unknown error")
                        errorText.setTextColor(0xFF663399.toInt())
                        errorText.textSize = 16f
                        errorText.setPadding(24, 32, 24, 32)
                        errorText.gravity = Gravity.CENTER
                        stepsContainer.addView(errorText)
                    }
                }
            }
        }
        
        dialog.findViewById<ImageButton>(R.id.close_button)?.setOnClickListener { dialog.dismiss() }
        
        updateButton.setOnClickListener {
            stepsContainer.removeAllViews()
            val loadingText = TextView(dialog.context).apply {
                text = fragment.getString(R.string.updating_directions)
                setTextColor(0xFF663399.toInt())
                textSize = 16f
                setPadding(24, 32, 24, 32)
                gravity = Gravity.CENTER
            }
            stepsContainer.addView(loadingText)

            refreshRouteSelection()

            fragment.lifecycleScope.launch {
                kotlinx.coroutines.delay(100)
                fetchDirections()
            }
        }
        
        fetchDirections()
    }

    private fun setupStationDirectionsDialogContent(
        dialog: Dialog,
        stepsContainer: LinearLayout,
        editFromLocation: EditText,
        editToLocation: EditText,
        summaryText: TextView,
        summaryContainer: LinearLayout,
        updateButton: ImageButton,
        chooseOnMapButton: LinearLayout,
        fastestButton: LinearLayout?,
        easiestButton: LinearLayout?,
        allRoutesButton: LinearLayout?,
        continuousJourneyColumnView: MetroLineJourneyColumnView?,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?
    ) {
        editFromLocation.hint = fragment.getString(R.string.from_my_current_location)
        editFromLocation.setText(selectedStartStation?.nameEnglish ?: "")
        editFromLocation.isEnabled = false
        editFromLocation.isFocusableInTouchMode = false
        editFromLocation.setTextColor(stationManager.getStationColor(selectedStartStation ?: return))
        
        editToLocation.setText(selectedEndStation?.nameEnglish ?: "")
        editToLocation.isEnabled = false
        editToLocation.isFocusableInTouchMode = false
        editToLocation.setTextColor(stationManager.getStationColor(selectedEndStation ?: return))

        val startBullet = dialog.findViewById<ImageView>(R.id.start_location_bullet)
        val endBullet = dialog.findViewById<ImageView>(R.id.end_location_bullet)
        
        startBullet?.setColorFilter(stationManager.getStationColor(selectedStartStation ?: return))
        endBullet?.setColorFilter(stationManager.getStationColor(selectedEndStation ?: return))
        
        val fetchDirections: () -> Unit = {
            fragment.lifecycleScope.launch {
                try {
                    stepsContainer.removeAllViews()
                    
                    val loadingText = TextView(dialog.context)
                    loadingText.text = "Finding metro route..."
                    loadingText.setTextColor(0xFF663399.toInt())
                    loadingText.textSize = 16f
                    loadingText.setPadding(24, 32, 24, 32)
                    loadingText.gravity = Gravity.CENTER
                    stepsContainer.addView(loadingText)
                    
                    kotlinx.coroutines.delay(500)
                    
                    stepsContainer.removeAllViews()
                    val inflater = LayoutInflater.from(dialog.context)
                    
                    val metroDirections = metroDirectionsManager.generateMetroDirections(selectedStartStation ?: return@launch, selectedEndStation ?: return@launch)
                    
                    // Filter out redundant "Exit at" steps when there's a corresponding "Arrive at" step
                    val filteredDirections = metroDirections.filterIndexed { index: Int, step: com.example.athensplus.presentation.transport.directions.MetroDirectionsManager.MetroStep ->
                        if (step.instruction.contains("Exit at")) {
                            val exitStation = step.instruction.substringAfter("Exit at ")
                            // Check if there's an "Arrive at" step for the same station
                            val hasArriveStep = metroDirections.any { arriveStep: com.example.athensplus.presentation.transport.directions.MetroDirectionsManager.MetroStep ->
                                arriveStep.instruction.contains("Arrive at $exitStation")
                            }
                            // Only keep "Exit at" if there's no corresponding "Arrive at"
                            !hasArriveStep
                        } else {
                            true // Keep all other steps
                        }
                    }
                    
                    // Create journey nodes from filtered metro directions
                    val journeyNodes = createJourneyNodes(filteredDirections)
                    
                    // Add step views to the container and track them
                    val stepViews = mutableListOf<View>()
                    for ((index, step) in filteredDirections.withIndex()) {
                        val stepView = inflater.inflate(R.layout.item_metro_direction_step, stepsContainer, false)
                        
                        val stationNameGreek = stepView.findViewById<TextView>(R.id.station_name_greek)
                        val stationNameContainer = stepView.findViewById<LinearLayout>(R.id.station_name_container)
                        val directionInstruction = stepView.findViewById<TextView>(R.id.step_instruction)
                        val additionalInfo = stepView.findViewById<TextView>(R.id.step_duration)
                        val connectingLine = stepView.findViewById<View>(R.id.connecting_line)
                        
                        // Hide connecting line for the last step
                        if (index == metroDirections.size - 1) {
                            connectingLine.visibility = View.GONE
                        }
                        
                        // Set station names based on step type
                        when {
                            step.instruction.contains("Enter") -> {
                                val stationName = step.instruction.substringAfter("Enter ").substringBefore(" station")
                                val station = findStationByName(stationName)
                                stationNameGreek.text = station?.nameEnglish ?: stationName
                                stationNameContainer.visibility = View.VISIBLE
                            }
                            step.instruction.contains("Take") && step.instruction.contains("towards") -> {
                                // Don't show station name for "towards" steps
                                stationNameContainer.visibility = View.GONE
                            }
                            step.instruction.contains("At") && step.instruction.contains("change to") -> {
                                val stationName = step.instruction.substringAfter("At ").substringBefore(",")
                                val station = findStationByName(stationName)
                                stationNameGreek.text = station?.nameEnglish ?: stationName
                                stationNameContainer.visibility = View.VISIBLE
                            }
                            step.instruction.contains("Exit at") -> {
                                val stationName = step.instruction.substringAfter("Exit at ")
                                val station = findStationByName(stationName)
                                stationNameGreek.text = station?.nameEnglish ?: stationName
                                stationNameContainer.visibility = View.VISIBLE
                            }
                            step.instruction.contains("Arrive at") -> {
                                val stationName = step.instruction.substringAfter("Arrive at ")
                                val station = findStationByName(stationName)
                                stationNameGreek.text = station?.nameEnglish ?: stationName
                                stationNameContainer.visibility = View.VISIBLE
                            }
                            else -> {
                                stationNameContainer.visibility = View.GONE
                            }
                        }
                        
                        // Set direction instruction
                        if (step.instruction.contains("Take") && step.instruction.contains("towards")) {
                            // For intermediate steps, show "Take direction towards..." in bold
                            val destination = step.instruction.substringAfter("towards ").substringBefore(" (")
                            directionInstruction.text = "Take direction towards $destination"
                            directionInstruction.setTypeface(null, android.graphics.Typeface.BOLD)
                        } else {
                            directionInstruction.text = step.instruction
                            directionInstruction.setTypeface(null, android.graphics.Typeface.NORMAL)
                        }
                        
                        // Set additional info
                        additionalInfo.text = step.duration
                        
                        stepsContainer.addView(stepView)
                        stepViews.add(stepView)
                    }
                    
                    // Add top and bottom padding for aesthetics
                    val paddingPx = (16 * fragment.requireContext().resources.displayMetrics.density).toInt()
                    stepsContainer.setPadding(0, paddingPx, 0, paddingPx)
                    
                    // Calculate actual step positions after views are laid out
                    stepsContainer.post {
                        try {
                            val stepPositions = mutableListOf<Float>()
                            val viewLocation = IntArray(2)
                            continuousJourneyColumnView?.getLocationInWindow(viewLocation)
                            val viewTopY = viewLocation[1]
                            
                            for (stepView in stepViews) {
                                // Get the center Y position of each step view
                                val location = IntArray(2)
                                stepView.getLocationInWindow(location)
                                val stepCenterY = location[1] + (stepView.height / 2)
                                // Make position relative to the journey column view
                                val relativeY = stepCenterY - viewTopY
                                stepPositions.add(relativeY.toFloat())
                            }
                            
                            // Set the journey line with actual step positions
                            continuousJourneyColumnView?.setJourney(journeyNodes, stepPositions)
                            
                            // Ensure the journey line matches the steps height
                            val params = continuousJourneyColumnView?.layoutParams
                            params?.height = stepsContainer.height
                            continuousJourneyColumnView?.layoutParams = params
                        } catch (e: Exception) {
                            android.util.Log.e("DialogManager", "Error calculating step positions: ${e.message}", e)
                            // Fallback to evenly distributed positions
                            val fallbackPositions = (0 until journeyNodes.size).map { i ->
                                (i * stepsContainer.height.toFloat()) / (journeyNodes.size - 1).coerceAtLeast(1)
                            }
                            continuousJourneyColumnView?.setJourney(journeyNodes, fallbackPositions)
                        }
                    }
                    
                } catch (e: Exception) {
                    stepsContainer.removeAllViews()
                    val errorText = TextView(dialog.context)
                    errorText.text = "Error generating metro directions: ${e.message}"
                    errorText.setTextColor(0xFF663399.toInt())
                    errorText.textSize = 16f
                    errorText.setPadding(24, 32, 24, 32)
                    errorText.gravity = Gravity.CENTER
                    stepsContainer.addView(errorText)
                }
            }
        }
        
        dialog.findViewById<ImageButton>(R.id.close_button)?.setOnClickListener { dialog.dismiss() }
        
        fastestButton?.visibility = View.GONE
        easiestButton?.visibility = View.GONE
        allRoutesButton?.visibility = View.GONE
        chooseOnMapButton.visibility = View.GONE
        
        // Show the go/enter button for metro station directions
        updateButton.visibility = View.VISIBLE
        updateButton.setOnClickListener {
            // For metro station directions, the go button provides navigation functionality
            selectedStartStation?.let { startStation ->
                selectedEndStation?.let { endStation ->
                    // Show a confirmation dialog or start navigation
                    val message = "Navigate from ${startStation.nameEnglish} to ${endStation.nameEnglish}?"
                    Toast.makeText(dialog.context, message, Toast.LENGTH_SHORT).show()
                    
                    // Here you could add more sophisticated navigation functionality:
                    // - Open Google Maps with the route
                    // - Show detailed station information
                    // - Start turn-by-turn navigation
                    // - Show real-time updates and delays
                    // - Open station timetable
                }
            } ?: run {
                Toast.makeText(dialog.context, "Please select both start and end stations", Toast.LENGTH_SHORT).show()
            }
        }
        
        fetchDirections()
    }

    private fun setupMapSelectionDialog(
        dialog: Dialog,
        mapView: MapView?,
        closeButton: ImageButton?,
        confirmButton: LinearLayout?,
        selectedLocationText: TextView?,
        selectedAddress: String?,
        onLocationSelected: (String) -> Unit
    ) {
        mapView?.onCreate(null)
        mapView?.getMapAsync { googleMap ->
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            googleMap.uiSettings.apply {
                isMapToolbarEnabled = true
                isCompassEnabled = true
                isZoomControlsEnabled = true
                isMyLocationButtonEnabled = true
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
                isTiltGesturesEnabled = true
                isRotateGesturesEnabled = true
            }
            fragment.lifecycleScope.launch {
                val currentLocation = locationService.getCurrentLocation()
                val startLocation = currentLocation ?: LatLng(37.9838, 23.7275)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 15f))
                if (locationService.isLocationPermissionGranted()) {
                    try {
                        // Lint: Call requires permission which may be rejected by user
                        googleMap.isMyLocationEnabled = true
                    } catch (e: SecurityException) {
                        Log.w("DialogManager", "Location permission denied for map", e)
                    }
                }
            }

            fun cleanAddress(addr: String): String {
                var result = addr
                if (result.endsWith(", Greece")) result = result.removeSuffix(", Greece")
                result = result.replace(Regex(", ?\\d{5}"), "")
                return result.trim()
            }

            fun updateMostAccurateAddress(center: LatLng) {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val geocoder = Geocoder(fragment.requireContext(), Locale.getDefault())
                    val addresses = try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(center.latitude, center.longitude, 5)
                    } catch (e: Exception) {
                        null
                    }
                    val addressList = addresses?.mapNotNull { it.getAddressLine(0) }?.map { cleanAddress(it) }?.distinct() ?: emptyList()
                    // Prefer address with a street number
                    val bestAddress = addressList.firstOrNull { it.contains(Regex("\\d+")) } ?: addressList.firstOrNull()
                    val coordString = "Location (${String.format(Locale.US, "%.4f", center.latitude)}, ${String.format(Locale.US, "%.4f", center.longitude)})"
                    val toShow = bestAddress ?: coordString
                    withContext(Dispatchers.Main) {
                        selectedLocationText?.text = toShow
                        // selectedAddress = toShow // This would need to be handled differently
                    }
                }
            }

            updateMostAccurateAddress(googleMap.cameraPosition.target)
            googleMap.setOnCameraIdleListener {
                updateMostAccurateAddress(googleMap.cameraPosition.target)
            }
        }

        closeButton?.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton?.setOnClickListener {
            val addressToUse = selectedLocationText?.text?.toString() ?: ""
            onLocationSelected(addressToUse)
            dialog.dismiss()
        }
    }

    private fun showTimetableDialog(station: MetroStation, timetableTables: List<Any>, waitTime: String?) {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val stationGreekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val stationEnglishNameText = dialog.findViewById<TextView>(R.id.station_name_english)

        val stationColor = stationManager.getStationColor(station)
        stationGreekNameText.text = station.nameGreek
        stationEnglishNameText.text = station.nameEnglish
        stationGreekNameText.setTextColor(stationColor)
        stationEnglishNameText.setTextColor(stationColor)

        dialog.findViewById<ImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        timetableContainer.removeAllViews()

        // Add timetable content here based on timetableTables
        // This would need to be implemented based on the specific timetable structure

        dialog.show()
    }

    private fun showAirportTimetableDialog(stationForTimetable: MetroStation, times: List<String>, instructionText: String?) {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val stationGreekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val stationEnglishNameText = dialog.findViewById<TextView>(R.id.station_name_english)
        val interchangeInfoText = dialog.findViewById<TextView>(R.id.interchange_info_text)

        val stationColor = stationManager.getStationColor(stationForTimetable)
        stationGreekNameText.text = stationForTimetable.nameGreek
        stationEnglishNameText.text = stationForTimetable.nameEnglish
        stationGreekNameText.setTextColor(stationColor)
        stationEnglishNameText.setTextColor(stationColor)

        dialog.findViewById<ImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        timetableContainer.removeAllViews()

        if (instructionText != null) {
            interchangeInfoText.text = if (instructionText.contains("No direct route")) fragment.getString(R.string.no_direct_route_airport) else fragment.getString(R.string.take_metro_to_airport, stationForTimetable.nameEnglish)
            interchangeInfoText.visibility = View.VISIBLE
        } else {
            interchangeInfoText.visibility = View.GONE
        }

        val inflater = LayoutInflater.from(dialog.context)
        val airportView = inflater.inflate(R.layout.item_airport_timetable, timetableContainer, false)
        val title = airportView.findViewById<TextView>(R.id.direction_title)
        val timesContainer = airportView.findViewById<LinearLayout>(R.id.times_container)

        title.text = fragment.getString(R.string.departures_from_to, stationForTimetable.nameEnglish)
        title.setTextColor(Color.parseColor("#0057a8"))

        val timesText = times.joinToString(separator = "  •  ")

        val cellTextView = TextView(dialog.context).apply {
            text = timesText
            setTextColor(Color.BLACK)
            setPadding(16, 16, 16, 16)
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
            gravity = Gravity.CENTER
            isSingleLine = false
        }
        timesContainer.addView(cellTextView)
        timetableContainer.addView(airportView)

        dialog.show()
    }

    private fun showWaitTimeInfo(step: TransitStep, waitTimeText: TextView, nextDepartureText: TextView) {
        try {
            if (step.waitTime != null && step.waitTimeMinutes > 0) {
                val waitTimeString = "${step.waitTimeMinutes}m"
                
                waitTimeText.text = fragment.getString(R.string.wait_time_format, waitTimeString)

                val departureTime = step.departureTime ?: ""
                nextDepartureText.text = fragment.getString(R.string.next_departure_format, departureTime)

                if (step.reliability != null) {
                    val reliabilityIcon = when (step.reliability) {
                        "High" -> "🟢"
                        "Medium" -> "🟡"
                        "Low" -> "🔴"
                        else -> ""
                    }
                    waitTimeText.text = fragment.getString(R.string.reliability_wait_format, reliabilityIcon, waitTimeString)
                }
            } else if (step.departureTimeValue > 0 && !step.departureTime.isNullOrEmpty()) {
                val currentTime = System.currentTimeMillis() / 1000 // Current time in seconds
                val waitTimeSeconds = step.departureTimeValue - currentTime
                
                if (waitTimeSeconds > 0) {
                    val waitTimeMinutes = waitTimeSeconds / 60
                    val waitTimeSecondsRemaining = waitTimeSeconds % 60
                    
                    val waitTimeString = when {
                        waitTimeMinutes > 0 -> "${waitTimeMinutes}m ${waitTimeSecondsRemaining}s"
                        else -> "${waitTimeSecondsRemaining}s"
                    }
                    
                    waitTimeText.text = fragment.getString(R.string.wait_time_format, waitTimeString)

                    val departureTime = step.departureTime
                    nextDepartureText.text = fragment.getString(R.string.next_departure_format, departureTime)
                } else {
                    waitTimeText.text = fragment.getString(R.string.bus_departed)
                    nextDepartureText.text = ""
                }
            } else {
                if (!step.departureTime.isNullOrEmpty()) {
                    waitTimeText.text = fragment.getString(R.string.wait_time_check_schedule)
                    nextDepartureText.text = fragment.getString(R.string.next_departure_format, step.departureTime)
                } else {
                    waitTimeText.text = fragment.getString(R.string.wait_time_not_available)
                    nextDepartureText.text = ""
                }
            }

            if (step.frequency != null) {
                nextDepartureText.text = fragment.getString(R.string.departure_with_frequency, nextDepartureText.text, step.frequency)
            }
            
        } catch (e: Exception) {
            Log.e("DialogManager", "Error calculating wait time", e)
            waitTimeText.text = fragment.getString(R.string.error_loading_wait_time)
            nextDepartureText.text = ""
        }
    }

    private fun displaySelectedRoutes(
        stepsContainer: LinearLayout,
        routes: List<BusTimesImprovementService.ImprovedRouteAlternative>,
        mode: RouteSelectionMode,
        context: Context,
        summaryContainer: LinearLayout,
        summaryText: TextView
    ) {
        stepsContainer.removeAllViews()
        
        if (routes.isEmpty()) {
            val noRoutesText = TextView(context).apply {
                text = fragment.getString(R.string.no_routes_found_destination)
                setTextColor(Color.parseColor("#F44336"))
                textSize = 16f
                setPadding(24, 32, 24, 32)
                gravity = Gravity.CENTER
            }
            stepsContainer.addView(noRoutesText)
            return
        }
        
        val inflater = LayoutInflater.from(context)

        if (routes.isNotEmpty()) {
            val route = routes.first()
            val summaryInfo = when (mode) {
                RouteSelectionMode.FASTEST -> "Fastest Route: ${route.totalDuration}"
                RouteSelectionMode.EASIEST -> "Easiest Route: ${route.totalDuration}"
                RouteSelectionMode.ALL_ROUTES -> "${routes.size} Routes Available"
            }
            
            summaryText.text = summaryInfo
            summaryContainer.visibility = View.VISIBLE
        } else {
            summaryContainer.visibility = View.GONE
        }

        routes.forEachIndexed { index, route ->
            if (routes.size > 1) {
                val routeHeader = TextView(context).apply {
                    text = fragment.getString(R.string.route_header, index + 1, route.totalDuration)
                    setTextColor(Color.parseColor("#663399"))
                    textSize = 16f
                    setPadding(24, 16, 24, 8)
                    gravity = Gravity.CENTER
                }
                stepsContainer.addView(routeHeader)
            }

            if (routes.size > 1) {
                val routeDetails = TextView(context).apply {
                    text = buildString {
                        append("• ${route.busLines.joinToString(", ")}")
                        append(" • ${route.waitTime / 60}min wait")
                        append(" • ${route.reliability}")
                        route.frequency?.let { frequency -> append(" • $frequency") }
                        route.crowdLevel?.let { crowd -> append(" • $crowd") }
                    }
                    setTextColor(Color.parseColor("#666666"))
                    textSize = 14f
                    setPadding(32, 4, 24, 8)
                }
                stepsContainer.addView(routeDetails)
            }

            route.steps.forEachIndexed { stepIndex, step ->
                val stepView = inflater.inflate(R.layout.item_transit_step_modern, stepsContainer, false)
                val icon = stepView.findViewById<ImageView>(R.id.step_icon)
                val instruction = stepView.findViewById<TextView>(R.id.step_instruction)
                val duration = stepView.findViewById<TextView>(R.id.step_duration)
                val line = stepView.findViewById<TextView>(R.id.step_line)
                val connectingLine = stepView.findViewById<View>(R.id.connecting_line)
                
                @Suppress("DEPRECATION")
                instruction.text = Html.fromHtml(step.instruction)
                duration.text = step.duration
                
                if (!step.line.isNullOrEmpty()) {
                    line.text = step.line
                    line.visibility = View.VISIBLE
                } else {
                    line.visibility = View.GONE
                }
                
                if (stepIndex == route.steps.size - 1) {
                    connectingLine.visibility = View.GONE
                }
                
                icon.setImageResource(when {
                    step.mode == "WALKING" -> R.drawable.ic_walking
                    step.mode == "TRANSIT" && step.vehicleType?.equals("BUS", ignoreCase = true) == true -> R.drawable.ic_transport
                    step.mode == "TRANSIT" && step.vehicleType?.equals("TRAM", ignoreCase = true) == true -> R.drawable.ic_tram
                    step.mode == "TRANSIT" && step.vehicleType?.equals("SUBWAY", ignoreCase = true) == true -> R.drawable.ic_metro
                    step.mode == "TRANSIT" -> R.drawable.ic_metro
                    else -> R.drawable.ic_walking
                })
                
                stepsContainer.addView(stepView)
            }

            if (routes.size > 1 && index < routes.size - 1) {
                val separator = View(context).apply {
                    setBackgroundColor(Color.parseColor("#E0E0E0"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    ).apply {
                        setMargins(24, 16, 24, 16)
                    }
                }
                stepsContainer.addView(separator)
            }
        }
    }
    
    private fun createJourneyNodes(metroDirections: List<MetroDirectionsManager.MetroStep>): List<MetroLineJourneyColumnView.JourneyNode> {
        val journeyNodes = mutableListOf<MetroLineJourneyColumnView.JourneyNode>()
        var previousLineColor = Color.parseColor("#009640")
        
        for (step in metroDirections) {
            val node = when {
                step.instruction.contains("Enter") -> {
                    val stationName = step.instruction.substringAfter("Enter ").substringBefore(" station")
                    val station = findStationByName(stationName)
                    val lineColor = getStationLineColor(station)
                    previousLineColor = lineColor
                    MetroLineJourneyColumnView.JourneyNode(
                        station = station ?: MetroStation("", "", com.google.android.gms.maps.model.LatLng(0.0, 0.0), false),
                        lineColor = lineColor,
                        isInterchange = false,
                        instruction = step.instruction
                    )
                }
                step.instruction.contains("Take") && step.instruction.contains("towards") -> {
                    val stationName = step.instruction.substringAfter("get off at ")
                    val station = findStationByName(stationName)
                    val lineColor = getLineColorFromInstruction(step.instruction) ?: getStationLineColor(station)
                    previousLineColor = lineColor
                    MetroLineJourneyColumnView.JourneyNode(
                        station = station ?: MetroStation("", "", com.google.android.gms.maps.model.LatLng(0.0, 0.0), false),
                        lineColor = lineColor,
                        isInterchange = station?.isInterchange ?: false,
                        instruction = step.instruction
                    )
                }
                step.instruction.contains("At") && step.instruction.contains("change to") -> {
                    val stationName = step.instruction.substringAfter("At ").substringBefore(",")
                    val station = findStationByName(stationName)
                    val lineColor = getStationLineColor(station)
                    previousLineColor = lineColor
                    MetroLineJourneyColumnView.JourneyNode(
                        station = station ?: MetroStation("", "", com.google.android.gms.maps.model.LatLng(0.0, 0.0), false),
                        lineColor = lineColor,
                        isInterchange = true,
                        instruction = step.instruction
                    )
                }
                step.instruction.contains("Exit at") -> {
                    val stationName = step.instruction.substringAfter("Exit at ")
                    val station = findStationByName(stationName)
                    val lineColor = getStationLineColor(station)
                    previousLineColor = lineColor
                    MetroLineJourneyColumnView.JourneyNode(
                        station = station ?: MetroStation("", "", com.google.android.gms.maps.model.LatLng(0.0, 0.0), false),
                        lineColor = lineColor,
                        isInterchange = false,
                        instruction = step.instruction
                    )
                }
                step.instruction.contains("Arrive at") -> {
                    val stationName = step.instruction.substringAfter("Arrive at ")
                    val station = findStationByName(stationName)
                    val lineColor = getStationLineColor(station)
                    previousLineColor = lineColor
                    MetroLineJourneyColumnView.JourneyNode(
                        station = station ?: MetroStation("", "", com.google.android.gms.maps.model.LatLng(0.0, 0.0), false),
                        lineColor = lineColor,
                        isInterchange = false,
                        instruction = step.instruction
                    )
                }
                else -> {
                    val lineColor = getLineColorFromInstruction(step.instruction) ?: previousLineColor
                    previousLineColor = lineColor
                    MetroLineJourneyColumnView.JourneyNode(
                        station = MetroStation("", "", com.google.android.gms.maps.model.LatLng(0.0, 0.0), false),
                        lineColor = lineColor,
                        isInterchange = false,
                        instruction = step.instruction
                    )
                }
            }
            journeyNodes.add(node)
        }
        
        return journeyNodes
    }
    
    private fun findStationByName(name: String): com.example.athensplus.domain.model.MetroStation? {
        return (com.example.athensplus.domain.model.StationData.metroLine1 + 
                com.example.athensplus.domain.model.StationData.metroLine2 + 
                com.example.athensplus.domain.model.StationData.metroLine3)
            .find { it.nameEnglish == name || it.nameGreek == name }
    }
    
    private fun getStationLineColor(station: com.example.athensplus.domain.model.MetroStation?): Int {
        return when {
            station == null -> Color.parseColor("#009640")
            com.example.athensplus.domain.model.StationData.metroLine1.contains(station) -> Color.parseColor("#009640")
            com.example.athensplus.domain.model.StationData.metroLine2.contains(station) -> Color.parseColor("#e30613")
            com.example.athensplus.domain.model.StationData.metroLine3.contains(station) -> Color.parseColor("#0057a8")
            else -> Color.parseColor("#009640")
        }
    }
    
    private fun getLineColorFromInstruction(instruction: String): Int? {
        return when {
            instruction.contains("Line 1") -> Color.parseColor("#009640")
            instruction.contains("Line 2") -> Color.parseColor("#e30613")
            instruction.contains("Line 3") -> Color.parseColor("#0057a8")
            else -> null
        }
    }
} 