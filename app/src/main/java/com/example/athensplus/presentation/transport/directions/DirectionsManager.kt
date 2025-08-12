package com.example.athensplus.presentation.transport.directions

import android.content.Context
import android.graphics.Color
import android.location.Geocoder
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.utils.BusTimesImprovementService
import com.example.athensplus.core.utils.FastestRouteService
import com.example.athensplus.core.utils.LocationService
import com.example.athensplus.core.utils.RouteSelectionMode
import com.example.athensplus.core.utils.RouteSelectionService
import android.app.Dialog
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.core.utils.TimetableService
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.example.athensplus.BuildConfig
import com.example.athensplus.domain.model.TransitStep
import com.example.athensplus.presentation.common.MetroLineJourneyColumnView
import com.example.athensplus.presentation.transport.routes.RouteDisplayManager
import com.example.athensplus.presentation.common.AddressAutocompleteManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DirectionsManager(
    private val fragment: Fragment,
    private val locationService: LocationService,
    private val stationManager: StationManager,
    private val timetableService: TimetableService,
    private val routeDisplayManager: RouteDisplayManager,
    private val addressAutocompleteManager: AddressAutocompleteManager,
    private val dialogManager: com.example.athensplus.presentation.transport.dialogs.TransportDialogManager
) {
    
    fun findRoute(destination: String) {
        if (destination.trim().isEmpty()) {
            Toast.makeText(fragment.context, fragment.getString(R.string.please_enter_destination), Toast.LENGTH_SHORT).show()
            return
        }
        
        showDirectionsDialog(destination)
    }
    
    fun showStationDirections(startStation: MetroStation, endStation: MetroStation) {
        showStationDirectionsDialog(startStation, endStation)
    }
    
    private fun showDirectionsDialog(destination: String) {
        try {
            val dialog = createDirectionsDialog()
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

            closeButton?.let { button ->
                val parent = button.parent as? ViewGroup
                if (parent != null) {
                    val textView = TextView(dialog.context).apply {
                        text = "✕"
                        textSize = 20f
                        setTextColor(android.graphics.Color.parseColor("#663399"))
                        gravity = android.view.Gravity.CENTER
                        setPadding(12, 12, 12, 12)
                        isClickable = true
                        isFocusable = true
                        layoutParams = button.layoutParams?.apply {
                            // Move closer to the edge by reducing margins
                            if (this is ViewGroup.MarginLayoutParams) {
                                marginEnd = 8
                                marginStart = 8
                            }
                        }
                        setOnClickListener {
                            android.util.Log.d("DirectionsManager", "TextView close button clicked!")
                            dialog.dismiss()
                        }
                    }
                    
                    val index = parent.indexOfChild(button)
                    parent.removeView(button)
                    parent.addView(textView, index)
                }
            }
            
            setupDialogContent(editFromLocation, editToLocation, destination)
            setupDialogAutocomplete(editFromLocation, editToLocation)
            
            val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
            val routeSelectionService = RouteSelectionService(fragment.requireContext(), apiKey, locationService)
            val routeSelectionUI = com.example.athensplus.presentation.common.RouteSelectionUI(fragment.requireContext(), fragment.lifecycleScope, routeSelectionService)
            
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

            updateButton?.setOnClickListener {
                refreshRouteSelection()
            }
            
            chooseOnMapButton?.setOnClickListener {
                dialogManager.showMapSelectionDialogForEditText(editToLocation)
            }
            
            dialog.show()
            
        } catch (e: Exception) {
            Toast.makeText(fragment.context, "Error showing directions dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createDirectionsDialog(): Dialog {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_directions)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set modal behavior: clicking outside closes dialog
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

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
        
        return dialog
    }
    
    private fun setupDialogContent(editFromLocation: EditText, editToLocation: EditText, destination: String) {
        editFromLocation.hint = fragment.getString(R.string.from_my_current_location)
        editFromLocation.setText("")
        editToLocation.setText(destination)
    }
    
    private fun setupDialogAutocomplete(editFromLocation: EditText, editToLocation: EditText) {
        addressAutocompleteManager.setupDialogAutocomplete(editFromLocation, editToLocation)
    }
    
    private fun displaySelectedRoutes(
        stepsContainer: LinearLayout,
        routes: List<BusTimesImprovementService.ImprovedRouteAlternative>,
        mode: RouteSelectionMode,
        context: Context,
        summaryContainer: LinearLayout,
        summaryText: TextView
    ) {
        routeDisplayManager.displaySelectedRoutes(stepsContainer, routes, mode, context, summaryContainer, summaryText)
    }
    
    private fun showStationDirectionsDialog(startStation: MetroStation, endStation: MetroStation) {
        Toast.makeText(fragment.context, "Directions from ${startStation.nameEnglish} to ${endStation.nameEnglish}", Toast.LENGTH_SHORT).show()
    }
} 