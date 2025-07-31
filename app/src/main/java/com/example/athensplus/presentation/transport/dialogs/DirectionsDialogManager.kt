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
import com.example.athensplus.core.utils.RouteSelectionService
import com.example.athensplus.presentation.common.RouteSelectionUI
import kotlinx.coroutines.launch

class DirectionsDialogManager(
    private val fragment: Fragment,
    private val locationService: com.example.athensplus.core.utils.LocationService
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

            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)

            dialog.setOnCancelListener {
                android.util.Log.d("DirectionsDialogManager", "Dialog cancelled!")
                onClose()
            }

            dialog.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            
            val stepsContainer = dialog.findViewById<LinearLayout>(R.id.steps_container)
            val closeButton = dialog.findViewById<ImageButton?>(R.id.close_button)

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
            val chooseOnMapButton = dialog.findViewById<LinearLayout>(R.id.button_choose_on_map_to)

            val fastestButton = dialog.findViewById<LinearLayout>(R.id.button_fastest)
            val easiestButton = dialog.findViewById<LinearLayout>(R.id.button_easiest)
            val allRoutesButton = dialog.findViewById<LinearLayout>(R.id.button_all_routes)
            
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
            
            val routeSelectionService = RouteSelectionService(fragment.requireContext(), apiKey, locationService)
            val routeSelectionUI = RouteSelectionUI(fragment.requireContext(), fragment.lifecycleScope, routeSelectionService)
            
            setupTestButton(dialog, stepsContainer, apiKey)
            setupDialogFields(editFromLocation, editToLocation, destination)
            setupDialogButtons(
                dialog, stepsContainer, summaryContainer, summaryText,
                editFromLocation, editToLocation, fastestButton, easiestButton, allRoutesButton,
                routeSelectionUI, apiKey, lastFocusedField, onUpdateDirections, onChooseOnMap, onFetchDirections
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
        fastestButton: LinearLayout,
        easiestButton: LinearLayout,
        allRoutesButton: LinearLayout,
        routeSelectionUI: RouteSelectionUI,
        apiKey: String,
        lastFocusedField: EditText?,
        onUpdateDirections: () -> Unit,
        onChooseOnMap: () -> Unit,
        onFetchDirections: (String, String) -> Unit
    ) {
        val updateButton = dialog.findViewById<ImageButton>(R.id.button_update_to)
        val chooseOnMapButton = dialog.findViewById<LinearLayout>(R.id.button_choose_on_map_to)
        
        updateButton?.setOnClickListener {
            onUpdateDirections()
        }
        
        chooseOnMapButton?.setOnClickListener {
            onChooseOnMap()
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
        
        routeSelectionUI.setupRouteSelectionButtons(
            fastestButton, easiestButton, allRoutesButton, stepsContainer,
            editFromLocation.text.toString(), editToLocation.text.toString()
        ) { routes, mode ->
            // todo
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
            
            if (!step.line.isNullOrEmpty()) {
                line.text = step.line
                line.visibility = View.VISIBLE
            } else {
                line.visibility = View.GONE
            }
            
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
} 