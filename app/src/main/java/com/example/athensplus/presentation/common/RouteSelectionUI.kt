package com.example.athensplus.presentation.common

import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import com.example.athensplus.R
import com.example.athensplus.core.utils.BusTimesImprovementService.ImprovedRouteAlternative
import com.example.athensplus.core.utils.RouteSelectionMode
import com.example.athensplus.core.utils.RouteSelectionResult
import com.example.athensplus.core.utils.RouteSelectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RouteSelectionUI(
    private val context: Context,
    private val scope: CoroutineScope,
    private val routeSelectionService: RouteSelectionService
) {
    
    private var currentMode: RouteSelectionMode = RouteSelectionMode.FASTEST
    private var currentFromText: String = ""
    private var currentToText: String = ""
    
    fun setupRouteSelectionButtons(
        fastestButton: LinearLayout,
        easiestButton: LinearLayout,
        allRoutesButton: LinearLayout,
        stepsContainer: LinearLayout,
        fromText: String,
        toText: String,
        onRouteSelected: (List<ImprovedRouteAlternative>, RouteSelectionMode) -> Unit
    ) {
        currentFromText = fromText
        currentToText = toText

        updateButtonStates(fastestButton, easiestButton, allRoutesButton, RouteSelectionMode.FASTEST)

        fastestButton.setOnClickListener {
            currentMode = RouteSelectionMode.FASTEST
            updateButtonStates(fastestButton, easiestButton, allRoutesButton, currentMode)
            fetchRoutesForMode(stepsContainer, currentMode, currentFromText, currentToText, onRouteSelected)
        }
        
        easiestButton.setOnClickListener {
            currentMode = RouteSelectionMode.EASIEST
            updateButtonStates(fastestButton, easiestButton, allRoutesButton, currentMode)
            fetchRoutesForMode(stepsContainer, currentMode, currentFromText, currentToText, onRouteSelected)
        }
        
        allRoutesButton.setOnClickListener {
            currentMode = RouteSelectionMode.ALL_ROUTES
            updateButtonStates(fastestButton, easiestButton, allRoutesButton, currentMode)
            fetchRoutesForMode(stepsContainer, currentMode, currentFromText, currentToText, onRouteSelected)
        }

        fetchRoutesForMode(stepsContainer, RouteSelectionMode.FASTEST, currentFromText, currentToText, onRouteSelected)
    }
    
    private fun updateButtonStates(
        fastestButton: LinearLayout,
        easiestButton: LinearLayout,
        allRoutesButton: LinearLayout,
        selectedMode: RouteSelectionMode
    ) {
        fastestButton.apply {
            setBackgroundResource(R.drawable.modern_button_bg)
            getChildAt(0)?.let { child ->
                if (child is TextView) {
                    child.setTextColor(Color.parseColor("#666666"))
                }
            }
        }
        
        easiestButton.apply {
            setBackgroundResource(R.drawable.modern_button_bg)
            getChildAt(0)?.let { child ->
                if (child is TextView) {
                    child.setTextColor(Color.parseColor("#666666"))
                }
            }
        }
        
        allRoutesButton.apply {
            setBackgroundResource(R.drawable.modern_button_bg)
            getChildAt(0)?.let { child ->
                if (child is TextView) {
                    child.setTextColor(Color.parseColor("#666666"))
                }
            }
        }
        
        // Set selected button state - just change text color for selected
        when (selectedMode) {
            RouteSelectionMode.FASTEST -> {
                fastestButton.getChildAt(0)?.let { child ->
                    if (child is TextView) {
                        child.setTextColor(Color.parseColor("#663399"))
                    }
                }
            }
            RouteSelectionMode.EASIEST -> {
                easiestButton.getChildAt(0)?.let { child ->
                    if (child is TextView) {
                        child.setTextColor(Color.parseColor("#663399"))
                    }
                }
            }
            RouteSelectionMode.ALL_ROUTES -> {
                allRoutesButton.getChildAt(0)?.let { child ->
                    if (child is TextView) {
                        child.setTextColor(Color.parseColor("#663399"))
                    }
                }
            }
        }
    }
    
    private fun fetchRoutesForMode(
        stepsContainer: LinearLayout,
        mode: RouteSelectionMode,
        fromText: String,
        toText: String,
        onRouteSelected: (List<ImprovedRouteAlternative>, RouteSelectionMode) -> Unit
    ) {
        // Show loading state
        showLoadingState(stepsContainer, mode)
        
        scope.launch {
            try {
                when (val result = routeSelectionService.getRoutes(fromText, toText, mode)) {
                    is RouteSelectionResult.Success -> {
                        showRoutes(stepsContainer, result, mode, onRouteSelected)
                    }
                    is RouteSelectionResult.Error -> {
                        showErrorState(stepsContainer, result.message)
                    }
                }
            } catch (e: Exception) {
                showErrorState(stepsContainer, context.getString(R.string.error_fetching_routes, e.message))
            }
        }
    }
    
    private fun showLoadingState(stepsContainer: LinearLayout, mode: RouteSelectionMode) {
        stepsContainer.removeAllViews()
        
        val modeText = when (mode) {
            RouteSelectionMode.FASTEST -> context.getString(R.string.route_mode_fastest)
            RouteSelectionMode.EASIEST -> context.getString(R.string.route_mode_easiest)
            RouteSelectionMode.ALL_ROUTES -> context.getString(R.string.route_mode_all_routes)
        }
        
        val loadingText = TextView(context).apply {
            text = context.getString(R.string.finding_route_format, modeText)
            setTextColor(Color.parseColor("#663399"))
            textSize = 16f
            setPadding(24, 32, 24, 32)
            gravity = android.view.Gravity.CENTER
        }
        
        stepsContainer.addView(loadingText)
    }
    
    private fun showRoutes(
        stepsContainer: LinearLayout,
        result: RouteSelectionResult.Success,
        mode: RouteSelectionMode,
        onRouteSelected: (List<ImprovedRouteAlternative>, RouteSelectionMode) -> Unit
    ) {
        stepsContainer.removeAllViews()

        val headerText = TextView(context).apply {
            text = result.title
            setTextColor(Color.parseColor("#663399"))
            textSize = 18f
            setPadding(24, 16, 24, 16)
            gravity = android.view.Gravity.CENTER
        }
        stepsContainer.addView(headerText)

        val descriptionText = TextView(context).apply {
            text = result.description
            setTextColor(Color.parseColor("#666666"))
            textSize = 14f
            setPadding(24, 8, 24, 16)
            gravity = android.view.Gravity.CENTER
        }
        stepsContainer.addView(descriptionText)

        onRouteSelected(result.routes, mode)
    }
    
    private fun showErrorState(stepsContainer: LinearLayout, errorMessage: String) {
        stepsContainer.removeAllViews()
        
        val errorText = TextView(context).apply {
            text = errorMessage
            setTextColor(Color.parseColor("#F44336"))
            textSize = 14f
            setPadding(24, 16, 24, 16)
            gravity = android.view.Gravity.CENTER
        }
        
        stepsContainer.addView(errorText)
    }
} 