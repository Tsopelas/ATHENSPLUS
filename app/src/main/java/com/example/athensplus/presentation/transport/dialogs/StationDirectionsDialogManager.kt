package com.example.athensplus.presentation.transport.dialogs

import android.app.Dialog
import android.graphics.Color
import android.text.Html
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.presentation.transport.directions.MetroDirectionsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StationDirectionsDialogManager(
    private val fragment: Fragment,
    private val stationManager: StationManager,
    private val metroDirectionsManager: com.example.athensplus.presentation.transport.directions.MetroDirectionsManager,
    private val addressAutocompleteManager: com.example.athensplus.presentation.common.AddressAutocompleteManager
) {
    
    fun showStationDirectionsDialog(
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?,
        onClose: () -> Unit,
        onUpdateDirections: () -> Unit
    ) {
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
            
            // Set modal behavior: clicking outside closes dialog and blocks all background interaction
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
            
            // Ensure dialog is truly modal and blocks all touch events outside
            dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog.window?.setDimAmount(0.3f)
            
            dialog.setOnCancelListener {
                android.util.Log.d("StationDirectionsDialogManager", "Dialog cancelled!")
                onClose()
            }

            val stepsContainer = dialog.findViewById<LinearLayout>(R.id.steps_container)
            val closeButton = dialog.findViewById<ImageButton?>(R.id.close_button)
            
            closeButton?.let { button ->
                val parent = button.parent as? ViewGroup
                if (parent != null) {
                    val textView = TextView(dialog.context).apply {
                        text = "✕"
                        textSize = 20f
                        setTextColor(ContextCompat.getColor(dialog.context, R.color.transport_text_on_tinted))
                        gravity = android.view.Gravity.CENTER
                        setPadding(12, 12, 12, 12)
                        isClickable = true
                        isFocusable = true
                        layoutParams = button.layoutParams?.apply {
                            if (this is ViewGroup.MarginLayoutParams) {
                                marginEnd = 8
                                marginStart = 8
                            }
                        }
                        setOnClickListener {
                            android.util.Log.d("StationDirectionsDialogManager", "TextView close button clicked!")
                            dialog.dismiss()
                            onClose()
                        }
                    }
                    
                    val index = parent.indexOfChild(button)
                    parent.removeView(button)
                    parent.addView(textView, index)
                }
            }
            val editFromLocation = dialog.findViewById<EditText>(R.id.edit_from_location)
            val editToLocation = dialog.findViewById<EditText>(R.id.edit_to_location)
            val startLocationBullet = dialog.findViewById<ImageView>(R.id.start_location_bullet)
            val endLocationBullet = dialog.findViewById<ImageView>(R.id.end_location_bullet)
            val summaryText = dialog.findViewById<TextView>(R.id.summary_text)
            val summaryContainer = dialog.findViewById<LinearLayout>(R.id.summary_container)
            val updateButton = dialog.findViewById<ImageButton>(R.id.button_update_to)
            val chooseOnMapButton = dialog.findViewById<LinearLayout>(R.id.button_choose_on_map_to)
            val continuousJourneyColumnView = dialog.findViewById<com.example.athensplus.presentation.common.MetroLineJourneyColumnView>(R.id.continuous_journey_column_view)

            val fastestButton = dialog.findViewById<LinearLayout>(R.id.button_fastest)
            val easiestButton = dialog.findViewById<LinearLayout>(R.id.button_easiest)
            val allRoutesButton = dialog.findViewById<LinearLayout>(R.id.button_all_routes)
            
            if (stepsContainer == null || editFromLocation == null || editToLocation == null || summaryText == null || summaryContainer == null || updateButton == null || chooseOnMapButton == null) {
                Toast.makeText(fragment.context, "Error: Could not find dialog views", Toast.LENGTH_SHORT).show()
                return
            }
            
            setupDialogFields(editFromLocation, editToLocation, selectedStartStation, selectedEndStation, startLocationBullet, endLocationBullet)
            setupDialogBullets(dialog, selectedStartStation, selectedEndStation)
            
            setupDialogAutocomplete(editFromLocation, editToLocation)
            
            val fetchDirections: () -> Unit = {
                (fragment as LifecycleOwner).lifecycleScope.launch {
                    try {
                        stepsContainer.removeAllViews()
                        
                        val loadingText = TextView(dialog.context)
                        loadingText.text = "Finding metro route..."
                        loadingText.setTextColor(ContextCompat.getColor(dialog.context, R.color.transport_text_on_tinted))
                        loadingText.textSize = 16f
                        loadingText.setPadding(24, 32, 24, 32)
                        loadingText.gravity = Gravity.CENTER
                        stepsContainer.addView(loadingText)
                        
                        delay(500)
                        
                        stepsContainer.removeAllViews()
                        val inflater = LayoutInflater.from(dialog.context)
                        
                        val metroDirections = metroDirectionsManager.generateMetroDirections(
                            selectedStartStation ?: return@launch, 
                            selectedEndStation ?: return@launch
                        )
                        
                        val filteredDirections = metroDirections.filterIndexed { index, step ->
                            if (step.instruction.contains("Exit at")) {
                                val exitStation = step.instruction.substringAfter("Exit at ")
                                val hasArriveStep = metroDirections.any { it.instruction.contains("Arrive at $exitStation") }
                                !hasArriveStep
                            } else {
                                true
                            }
                        }
                        
                        filteredDirections.forEachIndexed { index, step ->
                            val stepView = inflater.inflate(R.layout.item_metro_direction_step, stepsContainer, false)
                            val icon = stepView.findViewById<ImageView>(R.id.step_icon)
                            val instruction = stepView.findViewById<TextView>(R.id.step_instruction)
                            val duration = stepView.findViewById<TextView>(R.id.step_duration)
                            val line = stepView.findViewById<TextView>(R.id.step_line)
                            val connectingLine = stepView.findViewById<View>(R.id.connecting_line)
                            val stationNameContainer = stepView.findViewById<LinearLayout>(R.id.station_name_container)
                            val stationNameGreek = stepView.findViewById<TextView>(R.id.station_name_greek)
                            
                            @Suppress("DEPRECATION")
                            instruction.text = Html.fromHtml(step.instruction)
                            duration.text = step.duration
                            
                            if (!step.line.isNullOrEmpty()) {
                                line.text = convertGreekBusLineToEnglish(step.line!!)
                                line.visibility = View.VISIBLE
                                line.setTextColor(android.graphics.Color.WHITE)
                            } else {
                                line.visibility = View.GONE
                            }
                            
                            icon.setImageResource(step.iconResource)
                            
                            if (index < filteredDirections.size - 1) {
                                connectingLine.visibility = View.VISIBLE
                            } else {
                                connectingLine.visibility = View.GONE
                            }
                            
                            when {
                                step.instruction.contains("Enter") -> {
                                    val stationName = step.instruction.substringAfter("Enter ").substringBefore(" station")
                                    val station = findStationByName(stationName)
                                    stationNameGreek.text = station?.nameEnglish ?: stationName
                                    stationNameContainer.visibility = View.VISIBLE
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
                            
                            stepsContainer.addView(stepView)
                        }
                        
                        val journeyNodes = filteredDirections.map { step ->
                            val stationName = when {
                                step.instruction.contains("Enter") -> step.instruction.substringAfter("Enter ").substringBefore(" station")
                                step.instruction.contains("At") && step.instruction.contains("change to") -> step.instruction.substringAfter("At ").substringBefore(",")
                                step.instruction.contains("Exit at") -> step.instruction.substringAfter("Exit at ")
                                step.instruction.contains("Arrive at") -> step.instruction.substringAfter("Arrive at ")
                                else -> null
                            }
                            val station = stationName?.let { findStationByName(it) }
                            val lineColor = when {
                                step.instruction.contains("Line 1") -> android.graphics.Color.parseColor("#009640")
                                step.instruction.contains("Line 2") -> android.graphics.Color.parseColor("#e30613")
                                step.instruction.contains("Line 3") -> android.graphics.Color.parseColor("#0057a8")
                                station != null && com.example.athensplus.domain.model.StationData.metroLine1.contains(station) -> android.graphics.Color.parseColor("#009640")
                                station != null && com.example.athensplus.domain.model.StationData.metroLine2.contains(station) -> android.graphics.Color.parseColor("#e30613")
                                station != null && com.example.athensplus.domain.model.StationData.metroLine3.contains(station) -> android.graphics.Color.parseColor("#0057a8")
                                else -> android.graphics.Color.parseColor("#009640")
                            }
                            val isInterchange = station?.isInterchange == true
                            com.example.athensplus.presentation.common.MetroLineJourneyColumnView.JourneyNode(
                                station = station ?: com.example.athensplus.domain.model.MetroStation("", "", com.google.android.gms.maps.model.LatLng(0.0, 0.0), false),
                                lineColor = lineColor,
                                isInterchange = isInterchange,
                                instruction = step.instruction
                            )
                        }
                        stepsContainer.post {
                            val stepPositions = mutableListOf<Float>()
                            val viewLocation = IntArray(2)
                            continuousJourneyColumnView?.getLocationInWindow(viewLocation)
                            val viewTopY = viewLocation[1]
                            for (i in 0 until stepsContainer.childCount) {
                                val stepView = stepsContainer.getChildAt(i)
                                val location = IntArray(2)
                                stepView.getLocationInWindow(location)
                                val stepCenterY = location[1] + (stepView.height / 2)
                                val relativeY = stepCenterY - viewTopY
                                stepPositions.add(relativeY.toFloat())
                            }
                            continuousJourneyColumnView?.setJourney(journeyNodes, stepPositions)
                            val params = continuousJourneyColumnView?.layoutParams
                            params?.height = stepsContainer.height
                            continuousJourneyColumnView?.layoutParams = params
                        }

                        val (estimatedMinutes, totalStations) = calculateRouteInfo(selectedStartStation, selectedEndStation, filteredDirections)
                        summaryText.text = "Estimated Time: ${estimatedMinutes} min, ${totalStations} stations"
                        summaryContainer.visibility = View.VISIBLE
                        
                    } catch (e: Exception) {
                        val errorText = TextView(dialog.context)
                        errorText.text = "Error loading directions: ${e.message}"
                        errorText.setTextColor(ContextCompat.getColor(dialog.context, R.color.transport_text_on_tinted))
                        errorText.textSize = 16f
                        errorText.setPadding(24, 32, 24, 32)
                        errorText.gravity = Gravity.CENTER
                        stepsContainer.removeAllViews()
                        stepsContainer.addView(errorText)
                    }
                }
            }

            android.util.Log.d("StationDirectionsDialogManager", "Close button replacement completed")
            
            updateButton?.setOnClickListener {
                val fromText = editFromLocation.text.toString().trim()
                val toText = editToLocation.text.toString().trim()
                
                android.util.Log.d("StationDirectionsDialogManager", "Update button clicked - From: '$fromText', To: '$toText'")

                onUpdateDirections()
            }

            fetchDirections()
            
            dialog.show()
            
        } catch (e: Exception) {
            Toast.makeText(fragment.context, "Error showing station directions dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupDialogFields(
        editFromLocation: EditText,
        editToLocation: EditText,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?,
        startLocationBullet: ImageView?,
        endLocationBullet: ImageView?
    ) {
        editFromLocation.setText(selectedStartStation?.nameEnglish ?: "")
        editToLocation.setText(selectedEndStation?.nameEnglish ?: "")
        editFromLocation.isEnabled = true
        editToLocation.isEnabled = true
        editFromLocation.isFocusableInTouchMode = true
        editToLocation.isFocusableInTouchMode = true

        android.util.Log.d("StationDirectionsDialogManager", "Edit fields setup - From enabled: ${editFromLocation.isEnabled}, To enabled: ${editToLocation.isEnabled}")
        android.util.Log.d("StationDirectionsDialogManager", "Edit fields setup - From focusable: ${editFromLocation.isFocusable}, To focusable: ${editToLocation.isFocusable}")
        android.util.Log.d("StationDirectionsDialogManager", "Edit fields setup - From focusableInTouchMode: ${editFromLocation.isFocusableInTouchMode}, To focusableInTouchMode: ${editToLocation.isFocusableInTouchMode}")

        selectedStartStation?.let { station ->
            val startColor = stationManager.getStationColor(station)
            editFromLocation.setTextColor(startColor)
            startLocationBullet?.setColorFilter(startColor)
            startLocationBullet?.setImageResource(R.drawable.circle_purple_filled)
        }
        
        selectedEndStation?.let { station ->
            val endColor = stationManager.getStationColor(station)
            editToLocation.setTextColor(endColor)
            endLocationBullet?.setColorFilter(endColor)
        }
    }
    
    private fun setupDialogBullets(
        dialog: Dialog,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?
    ) {
        val fastestButton = dialog.findViewById<LinearLayout>(R.id.button_fastest)
        val easiestButton = dialog.findViewById<LinearLayout>(R.id.button_easiest)
        val allRoutesButton = dialog.findViewById<LinearLayout>(R.id.button_all_routes)
        val chooseOnMapButton = dialog.findViewById<LinearLayout>(R.id.button_choose_on_map_to)
        val addStopsButton = dialog.findViewById<ImageButton>(R.id.button_add_from)
        
        fastestButton?.visibility = View.GONE
        easiestButton?.visibility = View.GONE
        allRoutesButton?.visibility = View.GONE
        chooseOnMapButton?.visibility = View.GONE
        addStopsButton?.visibility = View.GONE
    }
    
    private fun setupDialogAutocomplete(editFromLocation: EditText, editToLocation: EditText) {
        addressAutocompleteManager.setupDialogAutocomplete(editFromLocation, editToLocation)
    }
    
    private fun isStationName(input: String): Boolean {
        val allStations = com.example.athensplus.domain.model.StationData.metroLine1 + 
                          com.example.athensplus.domain.model.StationData.metroLine2 + 
                          com.example.athensplus.domain.model.StationData.metroLine3
        
        return allStations.any { 
            it.nameEnglish.equals(input, ignoreCase = true) || 
            it.nameGreek.equals(input, ignoreCase = true) ||
            it.nameEnglish.contains(input, ignoreCase = true) ||
            it.nameGreek.contains(input, ignoreCase = true)
        }
    }
    
    private fun findStationByName(name: String): MetroStation? {
        return (com.example.athensplus.domain.model.StationData.metroLine1 + 
                com.example.athensplus.domain.model.StationData.metroLine2 + 
                com.example.athensplus.domain.model.StationData.metroLine3)
            .find { it.nameEnglish == name || it.nameGreek == name }
    }
    
    private fun calculateRouteInfo(
        startStation: MetroStation?,
        endStation: MetroStation?,
        directions: List<MetroDirectionsManager.MetroStep>
    ): Pair<Int, Int> {
        if (startStation == null || endStation == null) {
            return Pair(0, 0)
        }

        val metroTimeTable = com.example.athensplus.core.utils.MetroTimeTable
        val totalStations = if (directions.any { it.instruction.contains("change to") }) {

            val interchangeStation = stationManager.findInterchangeStation(startStation, endStation)
            if (interchangeStation != null) {
                val stationsToInterchange = metroTimeTable.getStationCount(startStation, interchangeStation)
                val stationsFromInterchange = metroTimeTable.getStationCount(interchangeStation, endStation)
                val totalStations = stationsToInterchange + stationsFromInterchange
                android.util.Log.d("RouteCalculation", "Interchange route: $stationsToInterchange + $stationsFromInterchange = $totalStations stations")
                totalStations
            } else {
                val stationCount = metroTimeTable.getStationCount(startStation, endStation)
                android.util.Log.d("RouteCalculation", "Direct route: $stationCount stations")
                stationCount
            }
        } else {

            val stationCount = metroTimeTable.getStationCount(startStation, endStation)
            android.util.Log.d("RouteCalculation", "Direct route: $stationCount stations")
            stationCount
        }
        
        android.util.Log.d("RouteCalculation", "Total stations calculated: $totalStations")
        android.util.Log.d("RouteCalculation", "Start station: ${startStation?.nameEnglish}, End station: ${endStation?.nameEnglish}")

        val context = fragment.requireContext()
        
        val estimatedMinutes = if (directions.any { it.instruction.contains("change to") }) {
            val interchangeStation = stationManager.findInterchangeStation(startStation, endStation)
            android.util.Log.d("RouteCalculation", "Interchange route detected")
            android.util.Log.d("RouteCalculation", "Start: ${startStation.nameEnglish}, End: ${endStation.nameEnglish}, Interchange: ${interchangeStation?.nameEnglish}")
            if (interchangeStation != null) {
                val time = metroTimeTable.getTravelTimeWithInterchange(context, startStation, endStation, interchangeStation)
                android.util.Log.d("RouteCalculation", "Interchange route time: $time minutes")
                time
            } else {
                val time = metroTimeTable.getTravelTime(context, startStation, endStation)
                android.util.Log.d("RouteCalculation", "Direct route time: $time minutes")
                time
            }
        } else {
            android.util.Log.d("RouteCalculation", "Direct route detected")
            android.util.Log.d("RouteCalculation", "Start: ${startStation.nameEnglish}, End: ${endStation.nameEnglish}")
            val time = metroTimeTable.getTravelTime(context, startStation, endStation)
            android.util.Log.d("RouteCalculation", "Direct route time: $time minutes")
            time
        }
        
        return Pair(estimatedMinutes, totalStations)
    }
    
    private fun convertGreekBusLineToEnglish(greekLine: String): String {
        return greekLine
            .replace("χ", "X", ignoreCase = true)
            .replace("Χ", "X", ignoreCase = true)
            .replace("ε", "E", ignoreCase = true)
            .replace("Ε", "E", ignoreCase = true)
            .replace("α", "A", ignoreCase = true)
            .replace("Α", "A", ignoreCase = true)
            .replace("β", "B", ignoreCase = true)
            .replace("Β", "B", ignoreCase = true)
            .replace("μ", "M", ignoreCase = true)
            .replace("Μ", "M", ignoreCase = true)
            .trim()
    }
} 