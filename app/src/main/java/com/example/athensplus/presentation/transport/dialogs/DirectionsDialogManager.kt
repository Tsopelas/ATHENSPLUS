package com.example.athensplus.presentation.transport.dialogs

import android.app.Dialog
import android.graphics.Color
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.BuildConfig
import com.example.athensplus.R
import com.example.athensplus.core.utils.BusTimesImprovementService
import com.example.athensplus.core.utils.FastestRouteService

import kotlinx.coroutines.launch

class DirectionsDialogManager(
    private val fragment: Fragment,
    private val locationService: com.example.athensplus.core.utils.LocationService,
    private val stationManager: com.example.athensplus.core.utils.StationManager,
    private val timetableService: com.example.athensplus.core.utils.TimetableService,
    private val addressAutocompleteManager: com.example.athensplus.presentation.common.AddressAutocompleteManager
) {
    
    fun showDirectionsDialog(
        destination: String,
        onClose: () -> Unit,
        onUpdateDirections: () -> Unit,
        onChooseOnMap: () -> Unit,
        onFetchDirections: (String, String) -> Unit
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
                android.util.Log.d("DirectionsDialogManager", "Dialog cancelled!")
                onClose()
            }
            
            val stepsContainer = dialog.findViewById<LinearLayout>(R.id.steps_container)
            val closeButton = dialog.findViewById<ImageButton?>(R.id.close_button)
            val continuousJourneyColumnView = dialog.findViewById<com.example.athensplus.presentation.common.MetroLineJourneyColumnView>(R.id.continuous_journey_column_view)
            
            // Hide metro column view for address-based searches and adjust layout
            continuousJourneyColumnView?.visibility = android.view.View.GONE
            
            // Find the root dialog view and adjust its padding to eliminate left spacing
            stepsContainer?.let { container ->
                // Traverse up 3 levels: stepsContainer -> horizontal container -> NestedScrollView -> root LinearLayout
                var currentView: ViewGroup? = container.parent as? ViewGroup // horizontal container
                currentView = currentView?.parent as? ViewGroup // NestedScrollView
                val rootView = currentView?.parent as? ViewGroup // root LinearLayout with padding
                
                // This should be the root LinearLayout with android:padding="20dp"
                rootView?.let { root ->
                    root.setPadding(
                        0, // Remove left padding
                        root.paddingTop, // Keep top padding
                        root.paddingRight, // Keep right padding  
                        root.paddingBottom // Keep bottom padding
                    )
                }
            }
            
            // Adjust steps container to fill full width when metro column is hidden
            stepsContainer?.let { container ->
                val layoutParams = container.layoutParams as? LinearLayout.LayoutParams
                layoutParams?.let { params ->
                    params.width = LinearLayout.LayoutParams.MATCH_PARENT // Fill full width instead of using weight
                    params.weight = 0f // Remove weight system
                    params.marginStart = 0 // Remove negative margin since metro column is hidden
                    container.layoutParams = params
                }
                
                // Also adjust the parent container's margin to remove all left spacing
                val parentContainer = container.parent as? ViewGroup
                parentContainer?.let { parent ->
                    val parentLayoutParams = parent.layoutParams as? ViewGroup.MarginLayoutParams
                    parentLayoutParams?.let { parentParams ->
                        parentParams.marginStart = 0 // Remove the 6dp margin from parent container
                        parent.layoutParams = parentParams
                    }
                }
            }

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
                            if (this is ViewGroup.MarginLayoutParams) {
                                marginEnd = 8
                                marginStart = 8
                            }
                        }
                        setOnClickListener {
                            android.util.Log.d("DirectionsDialogManager", "TextView close button clicked!")
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
            val summaryText = dialog.findViewById<TextView>(R.id.summary_text)
            val summaryContainer = dialog.findViewById<LinearLayout>(R.id.summary_container)
            val updateButton = dialog.findViewById<ImageButton>(R.id.button_update_to)
            val chooseOnMapButton = dialog.findViewById<ImageButton>(R.id.button_choose_on_map_from)


            
            if (stepsContainer == null || editFromLocation == null || editToLocation == null || summaryText == null || summaryContainer == null || updateButton == null || chooseOnMapButton == null) {
                Toast.makeText(fragment.context, "Error: Could not find dialog views", Toast.LENGTH_SHORT).show()
                return
            }
            
            val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
            
            var lastFocusedField: EditText? = null
            
            editFromLocation.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    lastFocusedField = editFromLocation
                }
            }
            
            editToLocation.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    lastFocusedField = editToLocation
                }
            }
            

            
            setupTestButton(dialog, stepsContainer, apiKey)
            setupDialogFields(editFromLocation, editToLocation, destination)
            setupDialogButtons(
                dialog, stepsContainer, summaryContainer, summaryText,
                editFromLocation, editToLocation, apiKey, lastFocusedField, onUpdateDirections, onChooseOnMap, onFetchDirections
            )

            dialog.show()
            
        } catch (e: Exception) {
            Toast.makeText(fragment.context, "Error showing directions dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupTestButton(dialog: Dialog, stepsContainer: LinearLayout, apiKey: String) {
        val testButton = dialog.findViewById<Button>(R.id.test_button)
        testButton?.setOnClickListener {
            fragment.lifecycleScope.launch {
                try {
                    val fastestRouteService = FastestRouteService(apiKey, locationService)
                    val routes = fastestRouteService.testDirections()
                    
                    if (routes.isNotEmpty()) {
                        displayRoute(stepsContainer, routes)
                    } else {
                        val noRoutesText = TextView(fragment.requireContext()).apply {
                            text = "No routes found"
                            setTextColor(Color.parseColor("#F44336"))
                            textSize = 16f
                            setPadding(24, 32, 24, 32)
                            gravity = Gravity.CENTER
                        }
                        stepsContainer.removeAllViews()
                        stepsContainer.addView(noRoutesText)
                    }
                } catch (e: Exception) {
                    val errorText = TextView(fragment.requireContext()).apply {
                        text = "Error: ${e.message}"
                        setTextColor(Color.parseColor("#F44336"))
                        textSize = 16f
                        setPadding(24, 32, 24, 32)
                        gravity = Gravity.CENTER
                    }
                    stepsContainer.removeAllViews()
                    stepsContainer.addView(errorText)
                }
            }
        }
    }
    
    private fun setupDialogFields(editFromLocation: EditText, editToLocation: EditText, destination: String) {
        editFromLocation.setText("Athens")
        editToLocation.setText(destination)
    }
    
    private fun setupDialogButtons(
        dialog: Dialog,
        stepsContainer: LinearLayout,
        summaryContainer: LinearLayout,
        summaryText: TextView,
        editFromLocation: EditText,
        editToLocation: EditText,
        apiKey: String,
        lastFocusedField: EditText?,
        onUpdateDirections: () -> Unit,
        onChooseOnMap: () -> Unit,
        onFetchDirections: (String, String) -> Unit
    ) {
        val updateButton = dialog.findViewById<ImageButton>(R.id.button_update_to)
        val chooseOnMapButton = dialog.findViewById<ImageButton>(R.id.button_choose_on_map_from)
        
        updateButton?.setOnClickListener {
            onUpdateDirections()
        }
        
        chooseOnMapButton?.setOnClickListener {
            val transportDialogManager = com.example.athensplus.presentation.transport.dialogs.TransportDialogManager(
                fragment, 
                locationService, 
                stationManager, 
                timetableService, 
                addressAutocompleteManager
            )
            transportDialogManager.showMapSelectionDialogForEditText(editToLocation)
        }
        
        editToLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val fromText = editFromLocation.text.toString()
                val toText = editToLocation.text.toString()
                onFetchDirections(fromText, toText)
                true
            } else {
                false
            }
        }
        

    }
    
    private fun displayRoute(stepsContainer: LinearLayout, steps: List<com.example.athensplus.domain.model.TransitStep>) {
        stepsContainer.removeAllViews()
        
        val inflater = LayoutInflater.from(fragment.requireContext())
        
        steps.forEach { step ->
            val stepView = inflater.inflate(R.layout.item_transit_step_modern, stepsContainer, false)
            val icon = stepView.findViewById<ImageView>(R.id.step_icon)
            val instruction = stepView.findViewById<TextView>(R.id.step_instruction)
            val duration = stepView.findViewById<TextView>(R.id.step_duration)
            val line = stepView.findViewById<TextView>(R.id.step_line)
            
            @Suppress("DEPRECATION")
            instruction.text = Html.fromHtml(step.instruction)
            duration.text = step.duration
            
            // Hide bus numbers for station cards
            line.visibility = View.GONE
            
            icon.setImageResource(getIconForStep(step))
            
            stepsContainer.addView(stepView)
        }
    }
    
    private fun getIconForStep(step: com.example.athensplus.domain.model.TransitStep): Int {
        return when {
            step.instruction.contains("Walk") -> R.drawable.ic_walking
            step.instruction.contains("Bus") -> R.drawable.ic_transport
            step.instruction.contains("Metro") -> R.drawable.ic_metro
            step.instruction.contains("Tram") -> R.drawable.ic_tram
            else -> R.drawable.ic_transport
        }
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