package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.domain.model.BusRouteAlternative
import com.example.athensplus.domain.model.TransitStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class BusTimesImprovementService(
    private val context: Context,
    private val apiKey: String,
    private val locationService: LocationService? = null
) {
    
    private val enhancedBusTimesService = EnhancedBusTimesService(context, apiKey, locationService)
    private val realTimeBusService = RealTimeBusService(context, apiKey)
    
    data class ImprovedRouteAlternative(
        val routeId: String,
        val steps: List<TransitStep>,
        val totalDuration: String,
        val totalDistance: String,
        val departureTime: Long,
        val arrivalTime: Long,
        val waitTime: Long,
        val busLines: List<String>,
        val reliability: String,
        val price: String?,
        val accessibility: Boolean,
        val realTimeUpdates: Boolean,
        val crowdLevel: String?,
        val frequency: String?,
        val alternativeOptions: List<String>,
        val estimatedCrowding: String?,
        val environmentalImpact: String?
    )
    
    suspend fun getIndustryStandardRoutes(
        from: String,
        to: String,
        maxAlternatives: Int = 5
    ): List<ImprovedRouteAlternative> = withContext(Dispatchers.IO) {
        try {
            val alternatives = enhancedBusTimesService.getEnhancedBusRoutes(from, to, maxAlternatives)
            
            val improvedAlternatives = mutableListOf<ImprovedRouteAlternative>()
            
            for (alternative in alternatives) {
                val improvedAlternative = enhanceRouteAlternative(alternative)
                improvedAlternatives.add(improvedAlternative)
            }

            improvedAlternatives.sortWith(compareBy<ImprovedRouteAlternative> { it.waitTime }
                .thenBy { getReliabilityScore(it.reliability) }
                .thenBy { it.totalDuration.replace(" min", "").toIntOrNull() ?: 0 })
            
            Log.d("BusTimesImprovementService", "Found ${improvedAlternatives.size} improved route alternatives")
            improvedAlternatives
            
        } catch (e: Exception) {
            Log.e("BusTimesImprovementService", "Error getting industry standard routes", e)
            emptyList()
        }
    }
    
    private suspend fun enhanceRouteAlternative(
        alternative: EnhancedBusTimesService.RouteAlternative
    ): ImprovedRouteAlternative = withContext(Dispatchers.IO) {
        val crowdLevel = calculateCrowdLevel(alternative)
        val frequency = calculateFrequency(alternative)
        val alternativeOptions = findAlternativeOptions(alternative)
        val estimatedCrowding = estimateCrowding(alternative)
        val environmentalImpact = calculateEnvironmentalImpact(alternative)
        
        ImprovedRouteAlternative(
            routeId = "route_${System.currentTimeMillis()}_${Random().nextInt(1000)}",
            steps = enhanceTransitSteps(alternative.steps),
            totalDuration = alternative.totalDuration,
            totalDistance = alternative.totalDistance,
            departureTime = alternative.departureTime,
            arrivalTime = alternative.arrivalTime,
            waitTime = alternative.waitTime,
            busLines = alternative.busLines,
            reliability = calculateEnhancedReliability(alternative),
            price = calculateEnhancedPrice(alternative),
            accessibility = checkEnhancedAccessibility(alternative),
            realTimeUpdates = true,
            crowdLevel = crowdLevel,
            frequency = frequency,
            alternativeOptions = alternativeOptions,
            estimatedCrowding = estimatedCrowding,
            environmentalImpact = environmentalImpact
        )
    }
    
    private fun enhanceTransitSteps(steps: List<TransitStep>): List<TransitStep> {
        return steps.map { step ->
            if (step.mode == "TRANSIT") {
                step.copy(
                    frequency = calculateStepFrequency(step),
                    reliability = calculateStepReliability(step),
                    crowdLevel = estimateStepCrowding(step),
                    price = calculateStepPrice(step),
                    accessibility = checkStepAccessibility(step),
                    realTimeUpdates = true,
                    alternativeLines = findAlternativeLinesForStep(step)
                )
            } else {
                step
            }
        }
    }
    
    private fun calculateCrowdLevel(alternative: EnhancedBusTimesService.RouteAlternative): String {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isRushHour = (currentHour in 7..9) || (currentHour in 17..19)
        
        return when {
            isRushHour && alternative.waitTime < 300 -> "High"
            alternative.waitTime < 180 -> "Medium"
            else -> "Low"
        }
    }

    // note: i should probably remove this and passthrough to api for the info
    private fun calculateFrequency(alternative: EnhancedBusTimesService.RouteAlternative): String {
        val busLines = alternative.busLines
        val highFrequencyLines = listOf("E14", "E15", "E16", "E17", "E18", "E19", "E20")
        
        val highFreqCount = busLines.count { it in highFrequencyLines }
        val totalLines = busLines.size
        
        return when {
            highFreqCount >= totalLines * 0.7 -> "High (Every 5-10 min)"
            highFreqCount >= totalLines * 0.4 -> "Medium (Every 10-15 min)"
            else -> "Low (Every 15-30 min)"
        }
    }
    
    private fun findAlternativeOptions(alternative: EnhancedBusTimesService.RouteAlternative): List<String> {
        val alternatives = mutableListOf<String>()

        for (line in alternative.busLines) {
            when (line) {
                "E14" -> alternatives.addAll(listOf("E15", "E16"))
                "E15" -> alternatives.addAll(listOf("E14", "E17"))
                "E16" -> alternatives.addAll(listOf("E14", "E18"))
                "E17" -> alternatives.addAll(listOf("E15", "E19"))
                "E18" -> alternatives.addAll(listOf("E16", "E20"))
                "E19" -> alternatives.addAll(listOf("E17", "E20"))
                "E20" -> alternatives.addAll(listOf("E18", "E19"))
            }
        }
        
        return alternatives.distinct()
    }
    
    private fun estimateCrowding(alternative: EnhancedBusTimesService.RouteAlternative): String {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isRushHour = (currentHour in 7..9) || (currentHour in 17..19)
        val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        
        return when {
            isRushHour && !isWeekend -> "High crowding expected"
            isWeekend -> "Low crowding expected"
            else -> "Moderate crowding expected"
        }
    }
    
    private fun calculateEnvironmentalImpact(alternative: EnhancedBusTimesService.RouteAlternative): String {
        val transitSteps = alternative.steps.count { it.mode == "TRANSIT" }
        val walkingSteps = alternative.steps.count { it.mode == "WALKING" }
        
        return when {
            walkingSteps > transitSteps -> "Very low (mostly walking)"
            transitSteps <= 1 -> "Low (minimal transit)"
            transitSteps <= 2 -> "Medium (moderate transit)"
            else -> "High (multiple transfers)"
        }
    }
    
    private fun calculateEnhancedReliability(alternative: EnhancedBusTimesService.RouteAlternative): String {
        val transferCount = alternative.steps.count { it.mode == "TRANSIT" }
        val avgWaitTime = alternative.waitTime / 60 // Convert to minutes
        val hasHighFrequencyLines = alternative.busLines.any { 
            it in listOf("E14", "E15", "E16", "E17", "E18", "E19", "E20") 
        }
        
        return when {
            transferCount <= 1 && avgWaitTime <= 5 && hasHighFrequencyLines -> "Very High"
            transferCount <= 1 && avgWaitTime <= 10 -> "High"
            transferCount <= 2 && avgWaitTime <= 15 -> "Medium"
            else -> "Low"
        }
    }
    
    private fun calculateEnhancedPrice(alternative: EnhancedBusTimesService.RouteAlternative): String? {
        val transitSteps = alternative.steps.count { it.mode == "TRANSIT" }
        val hasMetro = alternative.steps.any { 
            it.vehicleType?.contains("SUBWAY", ignoreCase = true) == true ||
            it.line?.contains("Line", ignoreCase = true) == true
        }
        
        return when {
            transitSteps == 0 -> "Free"
            hasMetro -> "€1.20 (Metro fare)"
            transitSteps <= 2 -> "€1.20 (Bus fare)"
            else -> "€1.20 (Multiple transfers)"
        }
    }
    
    private fun checkEnhancedAccessibility(alternative: EnhancedBusTimesService.RouteAlternative): Boolean {
        return alternative.steps.any { step ->
            step.vehicleType?.contains("ACCESSIBLE", ignoreCase = true) == true ||
            step.line?.contains("ACCESSIBLE", ignoreCase = true) == true ||
            step.mode == "WALKING" // Walking is always accessible
        }
    }
    
    private fun calculateStepFrequency(step: TransitStep): String? {
        val line = step.line ?: return null
        
        return when {
            line in listOf("E14", "E15", "E16", "E17", "E18", "E19", "E20") -> "Every 5-10 min"
            line.contains("Line") -> "Every 3-5 min"
            else -> "Every 10-15 min"
        }
    }
    
    private fun calculateStepReliability(step: TransitStep): String? {
        val line = step.line ?: return null
        val waitMinutes = step.waitTimeMinutes
        
        return when {
            line in listOf("E14", "E15", "E16", "E17", "E18", "E19", "E20") && waitMinutes <= 5 -> "Very High"
            waitMinutes <= 5 -> "High"
            waitMinutes <= 10 -> "Medium"
            else -> "Low"
        }
    }
    
    private fun estimateStepCrowding(step: TransitStep): String? {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isRushHour = (currentHour in 7..9) || (currentHour in 17..19)
        
        return when {
            isRushHour -> "High"
            else -> "Low"
        }
    }
    
    private fun calculateStepPrice(step: TransitStep): String? {
        return when {
            step.vehicleType?.contains("SUBWAY", ignoreCase = true) == true -> "€1.20"
            step.vehicleType?.contains("BUS", ignoreCase = true) == true -> "€1.20"
            step.vehicleType?.contains("TRAM", ignoreCase = true) == true -> "€1.20"
            else -> null
        }
    }
    
    private fun checkStepAccessibility(step: TransitStep): Boolean {
        return step.vehicleType?.contains("ACCESSIBLE", ignoreCase = true) == true ||
               step.line?.contains("ACCESSIBLE", ignoreCase = true) == true ||
               step.mode == "WALKING"
    }
    
    private fun findAlternativeLinesForStep(step: TransitStep): List<String> {
        val line = step.line ?: return emptyList()
        
        return when (line) {
            "E14" -> listOf("E15", "E16")
            "E15" -> listOf("E14", "E17")
            "E16" -> listOf("E14", "E18")
            "E17" -> listOf("E15", "E19")
            "E18" -> listOf("E16", "E20")
            "E19" -> listOf("E17", "E20")
            "E20" -> listOf("E18", "E19")
            else -> emptyList()
        }
    }
    
    private fun getReliabilityScore(reliability: String): Int {
        return when (reliability) {
            "Very High" -> 0
            "High" -> 1
            "Medium" -> 2
            "Low" -> 3
            else -> 4
        }
    }
    
    suspend fun getBusStopRealTimeInfo(stopName: String): List<RealTimeBusService.RealTimeDeparture> {
        return realTimeBusService.getRealTimeDepartures(stopName, 10)
    }
    
    suspend fun getOptimalRouteForTime(
        from: String,
        to: String,
        departureTime: Long? = null
    ): ImprovedRouteAlternative? = withContext(Dispatchers.IO) {
        try {
            val alternatives = getIndustryStandardRoutes(from, to, 10)
            
            if (alternatives.isEmpty()) {
                return@withContext null
            }

            if (departureTime != null) {
                alternatives.minByOrNull { alternative -> 
                    kotlin.math.abs(alternative.departureTime - departureTime) 
                }
            } else {
                alternatives.first()
            }
            
        } catch (e: Exception) {
            Log.e("BusTimesImprovementService", "Error getting optimal route", e)
            null
        }
    }
} 