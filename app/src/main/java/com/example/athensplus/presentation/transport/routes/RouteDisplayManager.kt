package com.example.athensplus.presentation.transport.routes

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.core.utils.BusTimesImprovementService
import com.example.athensplus.core.utils.RouteSelectionMode
import com.example.athensplus.domain.model.TransitStep
import com.example.athensplus.presentation.common.ExpandableStepManager
import com.example.athensplus.core.utils.SavedRoutesService
import android.widget.ImageButton

class RouteDisplayManager(
    private val fragment: Fragment
) {
    
    fun displaySelectedRoutes(
        stepsContainer: LinearLayout,
        routes: List<BusTimesImprovementService.ImprovedRouteAlternative>,
        mode: RouteSelectionMode,
        context: Context,
        summaryContainer: LinearLayout,
        summaryText: TextView,
        fromLocation: String = "",
        toLocation: String = "",
        onSaveRoute: ((String, String, String, String, List<TransitStep>) -> Unit)? = null
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
            
            val saveButton = summaryContainer.findViewById<ImageButton>(R.id.save_route_button)
            saveButton?.let { button ->
                val savedRoutesService = SavedRoutesService(context)
                
                fun updateButtonState() {
                    val isCurrentlySaved = savedRoutesService.isRouteSaved(fromLocation, toLocation)
                    button.setImageResource(if (isCurrentlySaved) R.drawable.ic_saves2 else R.drawable.ic_saves)
                }
                
                updateButtonState()
                
                button.setOnClickListener {
                    val isCurrentlySaved = savedRoutesService.isRouteSaved(fromLocation, toLocation)
                    
                    if (isCurrentlySaved) {
                        val existingRoute = savedRoutesService.getAllRoutes().find { 
                            it.fromLocation.equals(fromLocation, ignoreCase = true) && 
                            it.toLocation.equals(toLocation, ignoreCase = true) 
                        }
                        existingRoute?.let { route ->
                            savedRoutesService.deleteRoute(route.id)
                            button.setImageResource(R.drawable.ic_saves)
                        }
                    } else {
                        val routeSteps = route.steps.map { step ->
                            TransitStep(
                                mode = step.mode,
                                instruction = step.instruction,
                                duration = step.duration,
                                line = step.line,
                                departureStop = step.departureStop,
                                arrivalStop = step.arrivalStop,
                                nextArrival = step.nextArrival,
                                walkingDistance = step.walkingDistance,
                                vehicleType = step.vehicleType,
                                totalRouteDuration = step.totalRouteDuration,
                                totalRouteDistance = step.totalRouteDistance,
                                departureTime = step.departureTime,
                                departureTimeValue = step.departureTimeValue,
                                waitTime = step.waitTime,
                                waitTimeMinutes = step.waitTimeMinutes,
                                alternativeLines = step.alternativeLines,
                                frequency = step.frequency,
                                reliability = step.reliability,
                                crowdLevel = step.crowdLevel,
                                price = step.price,
                                accessibility = step.accessibility,
                                realTimeUpdates = step.realTimeUpdates,
                                numStops = step.numStops,
                                startLocation = step.startLocation,
                                endLocation = step.endLocation
                            )
                        }
                        
                        onSaveRoute?.invoke(fromLocation, toLocation, summaryInfo, route.totalDuration, routeSteps)
                        button.setImageResource(R.drawable.ic_saves2)
                    }
                }
            }
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
                
                // Hide bus numbers for station cards
                line.visibility = View.GONE
                
                icon.setImageResource(getIconForStep(step))
                
                // Get expandable components
                val stepContainer = stepView.findViewById<LinearLayout>(R.id.step_container)
                val expandArrow = stepView.findViewById<ImageView>(R.id.expand_arrow)
                val expandedContentContainer = stepView.findViewById<LinearLayout>(R.id.expanded_content_container)
                
                // Setup expandable functionality if components exist
                android.util.Log.d("RouteDisplayManager", "Setting up expandable for step: ${step.instruction}")
                if (stepContainer != null && expandArrow != null && expandedContentContainer != null) {
                    android.util.Log.d("RouteDisplayManager", "Found all expandable components for step: ${step.instruction}")
                    val expandableStepManager = ExpandableStepManager(fragment)
                    expandableStepManager.setupExpandableStep(
                        stepContainer, expandArrow, expandedContentContainer, step
                    )
                } else {
                    android.util.Log.w("RouteDisplayManager", "Missing expandable components! stepContainer: $stepContainer, expandArrow: $expandArrow, expandedContainer: $expandedContentContainer")
                }
                
                if (stepIndex < route.steps.size - 1) {
                    connectingLine.visibility = View.VISIBLE
                } else {
                    connectingLine.visibility = View.GONE
                }
                
                stepsContainer.addView(stepView)
            }
        }
    }
    
    fun getLineColorFromInstruction(instruction: String): Int {
        return when {
            instruction.contains("Line 1") || instruction.contains("Green") -> Color.parseColor("#009640")
            instruction.contains("Line 2") || instruction.contains("Red") -> Color.parseColor("#e30613")
            instruction.contains("Line 3") || instruction.contains("Blue") -> Color.parseColor("#0057a8")
            instruction.contains("Bus") -> Color.parseColor("#FF9800")
            instruction.contains("Tram") -> Color.parseColor("#9C27B0")
            else -> Color.parseColor("#009640")
        }
    }
    
    private fun getIconForStep(step: TransitStep): Int {
        return when {
            step.instruction.contains("Walk") -> R.drawable.ic_walking
            step.instruction.contains("Bus") -> R.drawable.ic_transport
            step.instruction.contains("Metro") || step.instruction.contains("Line") -> R.drawable.ic_metro
            step.instruction.contains("Tram") -> R.drawable.ic_tram
            step.instruction.contains("Enter") || step.instruction.contains("Exit") -> R.drawable.ic_metro
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