package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.core.utils.BusTimesImprovementService.ImprovedRouteAlternative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RouteSelectionMode {
    FASTEST,
    EASIEST,
    ALL_ROUTES
}

class RouteSelectionService(
    private val context: Context,
    private val apiKey: String,
    private val locationService: LocationService
) {
    
    private val busTimesImprovementService = BusTimesImprovementService(context, apiKey, locationService)

    /**
     * Get routes based on the specified mode
     * 
     * @param fromText Origin location
     * @param toText Destination location  
     * @param mode Route selection mode (FASTEST, EASIEST, ALL_ROUTES)
     * @return RouteSelectionResult with the selected routes
     */
    suspend fun getRoutes(
        fromText: String,
        toText: String,
        mode: RouteSelectionMode
    ): RouteSelectionResult = withContext(Dispatchers.IO) {
        try {
            val allAlternatives = busTimesImprovementService.getIndustryStandardRoutes(fromText, toText, 10)
            
            if (allAlternatives.isEmpty()) {
                return@withContext RouteSelectionResult.Error("No routes found")
            }
            
            // Calculate route metrics for all alternatives
            val routesWithMetrics = allAlternatives.map { alternative ->
                RouteMetrics(
                    route = alternative,
                    totalTime = calculateTotalTime(alternative),
                    transportChanges = calculateTransportChanges(alternative),
                    totalWalkingDistance = calculateTotalWalkingDistance(alternative)
                )
            }
            
            // Remove duplicate routes
            val uniqueRoutes = removeDuplicateRoutes(routesWithMetrics)
            
            // Filter out unrealistic routes (less than 5 minutes total time)
            val realisticRoutes = uniqueRoutes.filter { metrics ->
                val isValid = metrics.totalTime >= 5
                if (!isValid) {
                    Log.w("RouteSelectionService", "Filtering out unrealistic route: ${metrics.totalTime}min total time")
                }
                isValid
            }
            
            if (realisticRoutes.isEmpty()) {
                Log.w("RouteSelectionService", "No realistic routes found, using all routes")
                // If no realistic routes, use all routes but log the issue
                routesWithMetrics.forEach { metrics ->
                    Log.w("RouteSelectionService", "Unrealistic route: ${metrics.totalTime}min, ${metrics.transportChanges} changes, ${metrics.totalWalkingDistance}m walking")
                }
            }
            
            val routesToUse = if (realisticRoutes.isNotEmpty()) realisticRoutes else routesWithMetrics
            
            when (mode) {
                RouteSelectionMode.FASTEST -> {
                    val fastestRoute = routesToUse.minByOrNull { it.totalTime }
                    
                    // Debug logging to verify fastest route selection
                    Log.d("RouteSelectionService", "=== FASTEST ROUTE ANALYSIS ===")
                    routesToUse.forEach { metrics ->
                        Log.d("RouteSelectionService", "Route: ${metrics.totalTime}min, ${metrics.transportChanges} changes, ${metrics.totalWalkingDistance}m walking")
                    }
                    Log.d("RouteSelectionService", "Selected fastest: ${fastestRoute?.totalTime}min")
                    
                    if (fastestRoute != null) {
                        RouteSelectionResult.Success(
                            listOf(fastestRoute.route),
                            "Fastest Route",
                            "Fastest Route: ${fastestRoute.totalTime} min"
                        )
                    } else {
                        RouteSelectionResult.Error("No routes found")
                    }
                }
                
                RouteSelectionMode.EASIEST -> {
                    val easiestRoute = findEasiestRoute(routesToUse)
                    
                    // Debug logging to verify easiest route selection
                    Log.d("RouteSelectionService", "=== EASIEST ROUTE ANALYSIS ===")
                    routesToUse.forEach { metrics ->
                        Log.d("RouteSelectionService", "Route: ${metrics.totalTime}min, ${metrics.transportChanges} changes, ${metrics.totalWalkingDistance}m walking")
                    }
                    Log.d("RouteSelectionService", "Selected easiest: ${easiestRoute?.totalTime}min, ${easiestRoute?.transportChanges} changes, ${easiestRoute?.totalWalkingDistance}m walking")
                    
                    if (easiestRoute != null) {
                        RouteSelectionResult.Success(
                            listOf(easiestRoute.route),
                            "Easiest Route",
                            "Easiest Route: ${easiestRoute.totalTime} min"
                        )
                    } else {
                        RouteSelectionResult.Error("No routes found")
                    }
                }
                
                RouteSelectionMode.ALL_ROUTES -> {
                    RouteSelectionResult.Success(
                        allAlternatives,
                        "All Routes",
                        "Showing ${allAlternatives.size} available routes"
                    )
                }
            }
        } catch (e: Exception) {
            RouteSelectionResult.Error("Error finding routes: ${e.message}")
        }
    }
    
    /**
     * Calculate total time for a route (step duration + wait time)
     */
    private fun calculateTotalTime(alternative: ImprovedRouteAlternative): Int {
        val totalStepDuration = alternative.steps.sumOf { step ->
            parseDurationToMinutes(step.duration)
        }
        val waitTimeMinutes = (alternative.waitTime / 60).toInt()
        val totalTime = totalStepDuration + waitTimeMinutes
        
        // Debug logging for time calculation
        Log.d("RouteSelectionService", "=== TIME CALCULATION DEBUG ===")
        Log.d("RouteSelectionService", "Route steps:")
        alternative.steps.forEachIndexed { index, step ->
            val parsedDuration = parseDurationToMinutes(step.duration)
            Log.d("RouteSelectionService", "  Step $index: ${step.mode} - ${step.duration} (parsed: ${parsedDuration}min)")
        }
        Log.d("RouteSelectionService", "Total step duration: ${totalStepDuration}min")
        Log.d("RouteSelectionService", "Wait time: ${alternative.waitTime}s (${waitTimeMinutes}min)")
        Log.d("RouteSelectionService", "Total time: ${totalTime}min")
        
        return totalTime
    }
    
    /**
     * Parse duration string to minutes
     * Handles formats like "5 min", "5min", "5 minutes", "5min", etc.
     */
    private fun parseDurationToMinutes(duration: String): Int {
        return try {
            // Remove common time suffixes and extract the number
            val cleanDuration = duration
                .replace(" min", "", ignoreCase = true)
                .replace("min", "", ignoreCase = true)
                .replace(" minutes", "", ignoreCase = true)
                .replace(" minute", "", ignoreCase = true)
                .trim()
            
            cleanDuration.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Log.e("RouteSelectionService", "Error parsing duration: '$duration'", e)
            0
        }
    }
    
    /**
     * Calculate number of transport changes in a route
     * A change occurs when switching between different transport modes (walking, bus, metro, etc.)
     */
    private fun calculateTransportChanges(alternative: ImprovedRouteAlternative): Int {
        if (alternative.steps.size <= 1) return 0
        
        var changes = 0
        var previousMode: String? = null
        
        for (step in alternative.steps) {
            val currentMode = step.mode
            
            if (previousMode != null && previousMode != currentMode) {
                changes++
            }
            
            previousMode = currentMode
        }
        
        return changes
    }
    
    /**
     * Calculate total walking distance in meters
     */
    private fun calculateTotalWalkingDistance(alternative: ImprovedRouteAlternative): Int {
        return alternative.steps
            .filter { it.mode == "WALKING" }
            .sumOf { step ->
                step.walkingDistance?.replace(" m", "")?.toIntOrNull() ?: 0
            }
    }
    
    /**
     * Find the easiest route based on criteria:
     * 1. Least amount of transport changes (highest priority)
     * 2. Least amount of walking (second priority)
     * 3. If walking and transport changes are the same, pick the fastest route
     */
    private fun findEasiestRoute(routesWithMetrics: List<RouteMetrics>): RouteMetrics? {
        if (routesWithMetrics.isEmpty()) return null
        
        // Find the minimum number of transport changes
        val minTransportChanges = routesWithMetrics.minOf { it.transportChanges }
        
        // Filter routes with minimum transport changes
        val routesWithMinChanges = routesWithMetrics.filter { it.transportChanges == minTransportChanges }
        
        // Among routes with minimum changes, find the one with least walking
        val minWalkingDistance = routesWithMinChanges.minOf { it.totalWalkingDistance }
        val routesWithMinWalking = routesWithMinChanges.filter { it.totalWalkingDistance == minWalkingDistance }
        
        // If multiple routes have same transport changes and walking distance, pick the fastest
        return routesWithMinWalking.minByOrNull { it.totalTime }
    }

    /**
     * Remove duplicate routes based on their key characteristics.
     * Routes are considered duplicates if they have the same steps, total time, and transport changes.
     */
    private fun removeDuplicateRoutes(routes: List<RouteMetrics>): List<RouteMetrics> {
        val uniqueRoutes = mutableListOf<RouteMetrics>()
        val seenRouteSignatures = mutableSetOf<String>()

        for (route in routes) {
            val routeSignature = createRouteSignature(route)
            if (seenRouteSignatures.add(routeSignature)) {
                uniqueRoutes.add(route)
            } else {
                Log.d("RouteSelectionService", "Removing duplicate route: ${route.totalTime}min, ${route.transportChanges} changes")
            }
        }
        
        Log.d("RouteSelectionService", "Removed ${routes.size - uniqueRoutes.size} duplicate routes")
        return uniqueRoutes
    }
    
    /**
     * Create a unique signature for a route based on its characteristics
     */
    private fun createRouteSignature(route: RouteMetrics): String {
        val stepsSignature = route.route.steps.joinToString("|") { step ->
            "${step.mode}-${step.duration}-${step.line ?: ""}-${step.vehicleType ?: ""}"
        }
        return "$stepsSignature-${route.totalTime}-${route.transportChanges}"
    }
}

/**
 * Data class to hold route metrics for analysis
 */
private data class RouteMetrics(
    val route: ImprovedRouteAlternative,
    val totalTime: Int,
    val transportChanges: Int,
    val totalWalkingDistance: Int
)

sealed class RouteSelectionResult {
    data class Success(
        val routes: List<ImprovedRouteAlternative>,
        val title: String,
        val description: String
    ) : RouteSelectionResult()
    
    data class Error(val message: String) : RouteSelectionResult()
} 