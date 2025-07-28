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
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.presentation.transport.directions.MetroDirectionsManager.MetroStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StationDirectionsDialogManager(
    private val fragment: Fragment,
    private val stationManager: StationManager,
    private val metroDirectionsManager: com.example.athensplus.presentation.transport.directions.MetroDirectionsManager
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

            val stepsContainer = dialog.findViewById<LinearLayout>(R.id.steps_container)
            val closeButton = dialog.findViewById<ImageButton?>(R.id.close_button)
            val editFromLocation = dialog.findViewById<EditText>(R.id.edit_from_location)
            val editToLocation = dialog.findViewById<EditText>(R.id.edit_to_location)
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
            
            setupDialogFields(editFromLocation, editToLocation, selectedStartStation, selectedEndStation)
            setupDialogBullets(dialog, selectedStartStation, selectedEndStation)
            
            val fetchDirections: () -> Unit = {
                (fragment as LifecycleOwner).lifecycleScope.launch {
                    try {
                        stepsContainer.removeAllViews()
                        
                        val loadingText = TextView(dialog.context)
                        loadingText.text = "Finding metro route..."
                        loadingText.setTextColor(0xFF663399.toInt())
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
                        
                        // Filter out redundant "Exit at" steps when there's a corresponding "Arrive at" step
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
                            val stepView = inflater.inflate(R.layout.item_transit_step_modern, stepsContainer, false)
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
                                line.text = step.line
                                line.visibility = View.VISIBLE
                                line.setTextColor(step.lineColor)
                            } else {
                                line.visibility = View.GONE
                            }
                            
                            icon.setImageResource(step.iconResource)
                            
                            if (index < filteredDirections.size - 1) {
                                connectingLine.visibility = View.VISIBLE
                            } else {
                                connectingLine.visibility = View.GONE
                            }
                            
                            // Show Greek station names for specific steps
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
                        
                        // Update summary
                        val totalStations = filteredDirections.count { it.instruction.contains("Take") }
                        summaryText.text = "Metro Route: $totalStations segments"
                        summaryContainer.visibility = View.VISIBLE
                        
                    } catch (e: Exception) {
                        val errorText = TextView(dialog.context)
                        errorText.text = "Error loading directions: ${e.message}"
                        errorText.setTextColor(Color.parseColor("#F44336"))
                        errorText.textSize = 16f
                        errorText.setPadding(24, 32, 24, 32)
                        errorText.gravity = Gravity.CENTER
                        stepsContainer.removeAllViews()
                        stepsContainer.addView(errorText)
                    }
                }
            }
            
            closeButton?.setOnClickListener { 
                dialog.dismiss()
                onClose()
            }
            
            updateButton?.setOnClickListener {
                onUpdateDirections()
            }
            
            chooseOnMapButton?.setOnClickListener {
                // Handle choose on map
            }
            
            // Auto-fetch directions when dialog opens
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
        selectedEndStation: MetroStation?
    ) {
        editFromLocation.setText(selectedStartStation?.nameEnglish ?: "")
        editToLocation.setText(selectedEndStation?.nameEnglish ?: "")
        editFromLocation.isEnabled = false
        editToLocation.isEnabled = false
    }
    
    private fun setupDialogBullets(
        dialog: Dialog,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?
    ) {
        val fastestButton = dialog.findViewById<LinearLayout>(R.id.button_fastest)
        val easiestButton = dialog.findViewById<LinearLayout>(R.id.button_easiest)
        val allRoutesButton = dialog.findViewById<LinearLayout>(R.id.button_all_routes)
        
        fastestButton?.visibility = View.GONE
        easiestButton?.visibility = View.GONE
        allRoutesButton?.visibility = View.GONE
    }
    
    private fun findStationByName(name: String): MetroStation? {
        return (com.example.athensplus.domain.model.StationData.metroLine1 + 
                com.example.athensplus.domain.model.StationData.metroLine2 + 
                com.example.athensplus.domain.model.StationData.metroLine3)
            .find { it.nameEnglish == name || it.nameGreek == name }
    }
} 