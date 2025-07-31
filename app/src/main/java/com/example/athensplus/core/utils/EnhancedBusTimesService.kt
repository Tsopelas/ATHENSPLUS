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
            val instruction = step.getString("html_instructions")
            val duration = step.getJSONObject("duration").getString("text")
            val distance = step.optJSONObject("distance")?.getString("text")
            
            val transitDetails = step.optJSONObject("transit_details")
            val line = transitDetails?.optJSONObject("line")?.optString("short_name") ?: ""
            val departureStop = transitDetails?.optJSONObject("departure_stop")?.optString("name") ?: ""
            val arrivalStop = transitDetails?.optJSONObject("arrival_stop")?.optString("name") ?: ""
            val vehicleType = transitDetails?.optJSONObject("line")?.optJSONObject("vehicle")?.optString("type")
            
            val departureTime = transitDetails?.optJSONObject("departure_time")?.optString("text") ?: ""
            val departureTimeValue = transitDetails?.optJSONObject("departure_time")?.optLong("value") ?: 0L
            
            steps.add(
                TransitStep(
                    mode = travelMode,
                    instruction = instruction,
                    duration = duration,
                    line = line,
                    departureStop = departureStop,
                    arrivalStop = arrivalStop,
                    walkingDistance = distance,
                    vehicleType = vehicleType,
                    totalRouteDuration = totalDuration,
                    totalRouteDistance = totalDistance,
                    departureTime = departureTime,
                    departureTimeValue = departureTimeValue
                )
            )
        }
        
        return steps
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
} 