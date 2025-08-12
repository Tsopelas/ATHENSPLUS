package com.example.athensplus.core.utils

import android.util.Log
import com.example.athensplus.domain.model.TransitStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL


class EnhancedBusTimesService(
    private val apiKey: String,
    private val locationService: LocationService? = null
) {
    
    data class RouteAlternative(
        val steps: List<TransitStep>,
        val totalDuration: String,
        val totalDistance: String,
        val departureTime: Long,
        val arrivalTime: Long,
        val waitTime: Long,
        val busLines: List<String>
    )
    
    suspend fun getEnhancedBusRoutes(
        from: String,
        to: String,
        maxAlternatives: Int = 5
    ): List<RouteAlternative> = withContext(Dispatchers.IO) {
        try {
            val origin = if (from.contains("Current Location", ignoreCase = true)) {
                val currentLocation = locationService?.getCurrentLocation()
                if (currentLocation != null) {
                    "${currentLocation.latitude},${currentLocation.longitude}"
                } else {
                    "37.9838,23.7275"
                }
            } else {
                URLEncoder.encode(from, "UTF-8")
            }
            
            val destination = URLEncoder.encode(to, "UTF-8")
            val currentTime = System.currentTimeMillis() / 1000

            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$origin&destination=$destination&mode=transit&" +
                    "alternatives=true&departure_time=now&key=$apiKey"
            
            Log.d("EnhancedBusTimesService", "Requesting enhanced routes from: $from to: $to")
            Log.d("EnhancedBusTimesService", "URL: $url")
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            val status = json.getString("status")
            
            if (status != "OK") {
                Log.e("EnhancedBusTimesService", "API Error: $status")
                return@withContext emptyList()
            }
            
            val routes = json.getJSONArray("routes")
            val alternatives = mutableListOf<RouteAlternative>()

            for (i in 0 until minOf(routes.length(), maxAlternatives)) {
                val route = routes.getJSONObject(i)
                val legs = route.getJSONArray("legs")
                
                if (legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    val totalDuration = leg.getJSONObject("duration").getString("text")
                    val totalDistance = leg.getJSONObject("distance").getString("text")
                    
                    val steps = parseRouteSteps(leg, totalDuration, totalDistance)

                    val timingInfo = calculateTimingInfo(steps, currentTime)

                    val busLines = steps.filter { it.mode == "TRANSIT" && it.line != null }
                        .map { it.line!! }
                        .distinct()
                    
                    alternatives.add(RouteAlternative(
                        steps = steps,
                        totalDuration = totalDuration,
                        totalDistance = totalDistance,
                        departureTime = timingInfo.first,
                        arrivalTime = timingInfo.second,
                        waitTime = timingInfo.third,
                        busLines = busLines
                    ))
                }
            }

            alternatives.sortBy { it.departureTime }
            
            Log.d("EnhancedBusTimesService", "Found ${alternatives.size} route alternatives")
            alternatives
            
        } catch (e: Exception) {
            Log.e("EnhancedBusTimesService", "Error getting enhanced routes", e)
            emptyList()
        }
    }
    
    private fun parseRouteSteps(leg: JSONObject, totalDuration: String, totalDistance: String): List<TransitStep> {
        val steps = mutableListOf<TransitStep>()
        val stepsArray = leg.getJSONArray("steps")
        
        for (i in 0 until stepsArray.length()) {
            val step = stepsArray.getJSONObject(i)
            val travelMode = step.getString("travel_mode")
            val rawInstruction = step.getString("html_instructions")
            val duration = step.getJSONObject("duration").getString("text")
            val distance = step.optJSONObject("distance")?.getString("text")
            
            val transitDetails = step.optJSONObject("transit_details")
            val line = transitDetails?.optJSONObject("line")?.optString("short_name") ?: ""
            val departureStop = transitDetails?.optJSONObject("departure_stop")?.optString("name") ?: ""
            val arrivalStop = transitDetails?.optJSONObject("arrival_stop")?.optString("name") ?: ""
            val vehicleType = transitDetails?.optJSONObject("line")?.optJSONObject("vehicle")?.optString("type")
            val numStops = transitDetails?.optInt("num_stops") ?: 0
            
            val departureTime = transitDetails?.optJSONObject("departure_time")?.optString("text") ?: ""
            val departureTimeValue = transitDetails?.optJSONObject("departure_time")?.optLong("value") ?: 0L
            
            // Extract start and end coordinates for walking steps
            val (startLocation, endLocation) = extractStepCoordinates(step, travelMode)
            
            // Enhanced instruction processing
            val enhancedInstruction = enhanceInstructionFormat(
                travelMode, rawInstruction, departureStop, arrivalStop, line, vehicleType, numStops, distance
            )
            
            val enhancedSteps = createDetailedSteps(
                travelMode, enhancedInstruction, rawInstruction, departureStop, arrivalStop, 
                line, vehicleType, numStops, duration, distance, departureTime, departureTimeValue,
                totalDuration, totalDistance, startLocation, endLocation
            )
            
            steps.addAll(enhancedSteps)
        }
        
        return steps
    }
    
    private fun extractStepCoordinates(step: JSONObject, travelMode: String): Pair<com.google.android.gms.maps.model.LatLng?, com.google.android.gms.maps.model.LatLng?> {
        return try {
            val startLocation = step.getJSONObject("start_location")
            val endLocation = step.getJSONObject("end_location")
            
            val startLat = startLocation.getDouble("lat")
            val startLng = startLocation.getDouble("lng")
            val endLat = endLocation.getDouble("lat")
            val endLng = endLocation.getDouble("lng")
            
            val startCoord = com.google.android.gms.maps.model.LatLng(startLat, startLng)
            val endCoord = com.google.android.gms.maps.model.LatLng(endLat, endLng)
            
            Pair(startCoord, endCoord)
        } catch (e: Exception) {
            android.util.Log.w("EnhancedBusTimesService", "Could not extract coordinates for step", e)
            Pair(null, null)
        }
    }
    
    private fun enhanceInstructionFormat(
        mode: String,
        rawInstruction: String,
        departureStop: String,
        arrivalStop: String,
        line: String,
        vehicleType: String?,
        numStops: Int,
        distance: String?
    ): String {
        return when (mode) {
            "WALKING" -> {
                if (rawInstruction.contains("to", ignoreCase = true)) {
                    val destination = extractDestinationFromWalkingInstruction(rawInstruction)
                    "Walk to $destination"
                } else {
                    "Walk ($distance)"
                }
            }
            "TRANSIT" -> {
                val vehicleTypeName = when (vehicleType?.uppercase()) {
                    "BUS" -> "Bus"
                    "TRAM" -> "Tram"
                    "SUBWAY", "HEAVY_RAIL" -> "Metro"
                    else -> "Bus" // Default to bus for Athens
                }
                
                val direction = extractDirectionFromInstruction(rawInstruction)
                "$departureStop"
            }
            else -> rawInstruction
        }
    }
    
    private fun createDetailedSteps(
        mode: String,
        enhancedInstruction: String,
        rawInstruction: String,
        departureStop: String,
        arrivalStop: String,
        line: String,
        vehicleType: String?,
        numStops: Int,
        duration: String,
        distance: String?,
        departureTime: String,
        departureTimeValue: Long,
        totalDuration: String,
        totalDistance: String,
        startLocation: com.google.android.gms.maps.model.LatLng?,
        endLocation: com.google.android.gms.maps.model.LatLng?
    ): List<TransitStep> {
        val steps = mutableListOf<TransitStep>()
        
        if (mode == "TRANSIT" && numStops > 0 && departureStop.isNotEmpty() && arrivalStop.isNotEmpty()) {
            // Step 1: Board the vehicle - show station name and station direction
            val stationDirection = getStationDirection(departureStop)
            steps.add(
                TransitStep(
                    mode = mode,
                    instruction = enhancedInstruction, // This will be just the station name
                    duration = "towards $stationDirection", // Station direction goes in duration field
                    line = line,
                    departureStop = departureStop,
                    arrivalStop = arrivalStop,
                    walkingDistance = distance,
                    vehicleType = vehicleType,
                    totalRouteDuration = totalDuration,
                    totalRouteDistance = totalDistance,
                    departureTime = departureTime,
                    departureTimeValue = departureTimeValue,
                    startLocation = startLocation,
                    endLocation = endLocation
                )
            )
            
            // Step 2: Pass stations and get off
            val stationText = if (numStops == 1) "1 station" else "$numStops stations"
            val passStationsInstruction = "Pass $stationText and get off at $arrivalStop"
            
            steps.add(
                TransitStep(
                    mode = "TRANSIT_DETAIL",
                    instruction = passStationsInstruction,
                    duration = duration,
                    line = line,
                    departureStop = departureStop,
                    arrivalStop = arrivalStop,
                    walkingDistance = distance,
                    vehicleType = vehicleType,
                    totalRouteDuration = totalDuration,
                    totalRouteDistance = totalDistance,
                    departureTime = departureTime,
                    departureTimeValue = departureTimeValue,
                    numStops = numStops,
                    startLocation = startLocation,
                    endLocation = endLocation
                )
            )
        } else {
            // Regular step (walking or transit without detailed info)
            steps.add(
                TransitStep(
                    mode = mode,
                    instruction = enhancedInstruction,
                    duration = duration,
                    line = line,
                    departureStop = departureStop,
                    arrivalStop = arrivalStop,
                    walkingDistance = distance,
                    vehicleType = vehicleType,
                    totalRouteDuration = totalDuration,
                    totalRouteDistance = totalDistance,
                    departureTime = departureTime,
                    departureTimeValue = departureTimeValue,
                    numStops = numStops,
                    startLocation = startLocation,
                    endLocation = endLocation
                )
            )
        }
        
        return steps
    }
    
    private fun extractDestinationFromWalkingInstruction(instruction: String): String {
        // Remove HTML tags and extract destination
        val cleanInstruction = instruction.replace("<[^>]*>".toRegex(), "")
        
        return when {
            cleanInstruction.contains("to ", ignoreCase = true) -> {
                cleanInstruction.substringAfter("to ", "").trim()
            }
            cleanInstruction.contains("toward ", ignoreCase = true) -> {
                cleanInstruction.substringAfter("toward ", "").trim()
            }
            else -> cleanInstruction.trim()
        }
    }
    
    private fun extractDirectionFromInstruction(instruction: String): String {
        // Remove HTML tags
        val cleanInstruction = instruction.replace("<[^>]*>".toRegex(), "")
        
        return when {
            cleanInstruction.contains("towards ", ignoreCase = true) -> {
                cleanInstruction.substringAfter("towards ", "").trim()
            }
            cleanInstruction.contains("toward ", ignoreCase = true) -> {
                cleanInstruction.substringAfter("toward ", "").trim()
            }
            cleanInstruction.contains("to ", ignoreCase = true) -> {
                cleanInstruction.substringAfter("to ", "").trim()
            }
            else -> cleanInstruction.trim()
        }
    }
    
    private fun calculateTimingInfo(steps: List<TransitStep>, currentTime: Long): Triple<Long, Long, Long> {
        var firstDepartureTime = 0L
        var lastArrivalTime = 0L
        var totalWaitTime = 0L
        
        for (step in steps) {
            if (step.mode == "TRANSIT" && step.departureTimeValue > 0) {
                if (firstDepartureTime == 0L) {
                    firstDepartureTime = step.departureTimeValue
                }
                lastArrivalTime = step.departureTimeValue + (step.duration.replace(" min", "").toIntOrNull() ?: 0) * 60

                val waitTime = step.departureTimeValue - currentTime
                if (waitTime > 0) {
                    totalWaitTime += waitTime
                }
            }
        }
        
        return Triple(firstDepartureTime, lastArrivalTime, totalWaitTime)
    }
    
    private fun getStationDirection(stationName: String): String {
        // Find the station in the metro lines and determine its direction
        val allStations = com.example.athensplus.domain.model.StationData.metroLine1 + 
                         com.example.athensplus.domain.model.StationData.metroLine2 + 
                         com.example.athensplus.domain.model.StationData.metroLine3
        
        val station = allStations.find { 
            it.nameEnglish.equals(stationName, ignoreCase = true) || 
            it.nameGreek.equals(stationName, ignoreCase = true) 
        }
        
        if (station == null) {
            return "Unknown"
        }
        
        // Determine which line the station is on and its direction
        return when {
            com.example.athensplus.domain.model.StationData.metroLine1.contains(station) -> {
                val stationIndex = com.example.athensplus.domain.model.StationData.metroLine1.indexOf(station)
                val totalStations = com.example.athensplus.domain.model.StationData.metroLine1.size
                
                if (stationIndex < totalStations / 2) {
                    "Kifisia" // Towards the northern end
                } else {
                    "Piraeus" // Towards the southern end
                }
            }
            com.example.athensplus.domain.model.StationData.metroLine2.contains(station) -> {
                val stationIndex = com.example.athensplus.domain.model.StationData.metroLine2.indexOf(station)
                val totalStations = com.example.athensplus.domain.model.StationData.metroLine2.size
                
                if (stationIndex < totalStations / 2) {
                    "Anthoupoli" // Towards the northwestern end
                } else {
                    "Elliniko" // Towards the southeastern end
                }
            }
            com.example.athensplus.domain.model.StationData.metroLine3.contains(station) -> {
                val stationIndex = com.example.athensplus.domain.model.StationData.metroLine3.indexOf(station)
                val totalStations = com.example.athensplus.domain.model.StationData.metroLine3.size
                
                if (stationIndex < totalStations / 2) {
                    "Airport" // Towards the eastern end
                } else {
                    "Dimotiko Theatro" // Towards the western end
                }
            }
            else -> "Unknown"
        }
    }
} 