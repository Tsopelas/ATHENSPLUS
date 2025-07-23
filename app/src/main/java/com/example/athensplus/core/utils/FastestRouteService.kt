package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.domain.model.TransitStep
import com.example.athensplus.domain.model.BusRouteAlternative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL

class FastestRouteService(
    @Suppress("UNUSED_PARAMETER") context: Context, 
    private val apiKey: String,
    private val locationService: LocationService? = null
) {
    
    private val enhancedBusTimesService = EnhancedBusTimesService(apiKey, locationService)
    
    @Suppress("UNUSED")
    suspend fun getFastestRoute(
        from: String,
        to: String
    ): List<TransitStep> = withContext(Dispatchers.IO) {
        try {
            val alternatives = enhancedBusTimesService.getEnhancedBusRoutes(from, to, 3)
            
            if (alternatives.isNotEmpty()) {
                val bestAlternative = alternatives.minByOrNull { 
                    it.waitTime + (it.totalDuration.replace(" min", "").toIntOrNull() ?: 0) * 60 
                } ?: alternatives.first()
                
                Log.d("FastestRouteService", "Selected best route with ${bestAlternative.waitTime}s wait time")
                return@withContext bestAlternative.steps
            }

            return@withContext getFallbackRoute(from, to)
            
        } catch (e: Exception) {
            Log.e("FastestRouteService", "Error getting enhanced route, falling back", e)
            return@withContext getFallbackRoute(from, to)
        }
    }
    
    private suspend fun getFallbackRoute(from: String, to: String): List<TransitStep> = withContext(Dispatchers.IO) {
        try {
            val origin = if (from.contains("Current Location", ignoreCase = true)) {
                val currentLocation = locationService?.getCurrentLocation()
                if (currentLocation != null) {
                    Log.d("FastestRouteService", "Using actual location: ${currentLocation.latitude}, ${currentLocation.longitude}")
                    "${currentLocation.latitude},${currentLocation.longitude}"
                } else {
                    Log.w("FastestRouteService", "Could not get current location, using fallback")
                    "37.9838,23.7275"
                }
            } else {
                URLEncoder.encode(from, "UTF-8")
            }
            
            val destination = URLEncoder.encode(to, "UTF-8")

            var url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&mode=transit&departure_time=now&key=$apiKey"
            
            Log.d("FastestRouteService", "Requesting transit directions from: $from to: $to")
            Log.d("FastestRouteService", "URL: $url")
            
            var response = URL(url).readText()
            Log.d("FastestRouteService", "API Response: $response")
            
            var json = JSONObject(response)
            var status = json.getString("status")

            if (status != "OK" || json.getJSONArray("routes").length() == 0) {
                Log.w("FastestRouteService", "Transit failed, trying walking directions")
                url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&mode=walking&key=$apiKey"
                
                Log.d("FastestRouteService", "Requesting walking directions")
                Log.d("FastestRouteService", "URL: $url")
                
                response = URL(url).readText()
                Log.d("FastestRouteService", "Walking API Response: $response")
                
                json = JSONObject(response)
                status = json.getString("status")
            }
            
            if (status != "OK") {
                Log.e("FastestRouteService", "API Error: $status")
                when (status) {
                    "ZERO_RESULTS" -> Log.w("FastestRouteService", "No routes found between the specified locations")
                    "OVER_QUERY_LIMIT" -> Log.e("FastestRouteService", "API quota exceeded")
                    "REQUEST_DENIED" -> Log.e("FastestRouteService", "API key invalid or restricted")
                    "INVALID_REQUEST" -> Log.e("FastestRouteService", "Invalid request parameters")
                    else -> Log.e("FastestRouteService", "Unknown API error: $status")
                }
                return@withContext emptyList()
            }
            
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                Log.w("FastestRouteService", "No routes found")
                return@withContext emptyList()
            }
            
            val steps = mutableListOf<TransitStep>()
            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            
            if (legs.length() == 0) {
                Log.w("FastestRouteService", "No legs found in route")
                return@withContext emptyList()
            }
            
            val leg = legs.getJSONObject(0)
            val totalDuration = leg.getJSONObject("duration").getString("text")
            val totalDistance = leg.getJSONObject("distance").getString("text")
            
            Log.d("FastestRouteService", "Total route: $totalDuration, $totalDistance")
            
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

                val currentTime = System.currentTimeMillis() / 1000
                val waitTime = if (departureTimeValue > currentTime) {
                    (departureTimeValue - currentTime) / 60
                } else 0
                
                // Log departure time information for debugging
                if (travelMode == "TRANSIT") {
                    Log.d("FastestRouteService", "Transit step: $line")
                    Log.d("FastestRouteService", "Departure time: $departureTime")
                    Log.d("FastestRouteService", "Departure time value: $departureTimeValue")
                    Log.d("FastestRouteService", "Wait time: $waitTime minutes")
                    Log.d("FastestRouteService", "Current time: $currentTime")
                }

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
                        departureTimeValue = departureTimeValue,
                        waitTime = if (waitTime > 0) "$waitTime min" else null,
                        waitTimeMinutes = waitTime.toInt()
                    )
                )
            }
            
            Log.d("FastestRouteService", "Found ${steps.size} steps")
            steps
            
        } catch (e: Exception) {
            Log.e("FastestRouteService", "Error getting directions", e)
            emptyList()
        }
    }
    
    @Suppress("UNUSED")
    suspend fun getRouteAlternatives(
        from: String,
        to: String,
        maxAlternatives: Int = 5
    ): List<BusRouteAlternative> = withContext(Dispatchers.IO) {
        try {
            val alternatives = enhancedBusTimesService.getEnhancedBusRoutes(from, to, maxAlternatives)
            
            alternatives.map { alternative ->
                BusRouteAlternative(
                    routeId = "route_${System.currentTimeMillis()}",
                    steps = alternative.steps,
                    totalDuration = alternative.totalDuration,
                    totalDistance = alternative.totalDistance,
                    departureTime = alternative.departureTime,
                    arrivalTime = alternative.arrivalTime,
                    waitTime = alternative.waitTime,
                    busLines = alternative.busLines,
                    reliability = calculateReliability(alternative),
                    price = calculatePrice(alternative),
                    accessibility = checkAccessibility(alternative),
                    realTimeUpdates = true
                )
            }
        } catch (e: Exception) {
            Log.e("FastestRouteService", "Error getting route alternatives", e)
            emptyList()
        }
    }
    
    private fun calculateReliability(alternative: EnhancedBusTimesService.RouteAlternative): String {
        // Simple reliability calculation based on number of transfers and wait time
        val transferCount = alternative.steps.count { it.mode == "TRANSIT" }
        val avgWaitTime = alternative.waitTime / 60 // Convert to minutes
        
        return when {
            transferCount <= 1 && avgWaitTime <= 5 -> "High"
            transferCount <= 2 && avgWaitTime <= 10 -> "Medium"
            else -> "Low"
        }
    }

    // i have to improve this by fetching prices depending on the route but idk how
    private fun calculatePrice(alternative: EnhancedBusTimesService.RouteAlternative): String {
        val transitSteps = alternative.steps.count { it.mode == "TRANSIT" }
        return when (transitSteps) {
            0 -> "Free"
            1 -> "€1.20"
            2 -> "€1.20"
            else -> "€1.20"
        }
    }
    
    private fun checkAccessibility(alternative: EnhancedBusTimesService.RouteAlternative): Boolean {
        return alternative.steps.any { step ->
            step.vehicleType?.contains("ACCESSIBLE", ignoreCase = true) == true ||
            step.line?.contains("ACCESSIBLE", ignoreCase = true) == true
        }
    }

    suspend fun testDirections(): List<TransitStep> = withContext(Dispatchers.IO) {
        try {
            val testOrigin = locationService?.getCurrentLocation()?.let { 
                "${it.latitude},${it.longitude}" 
            } ?: "37.9838,23.7275"
            val testDestination = "Acropolis Museum, Athens"
            val encodedDestination = URLEncoder.encode(testDestination, "UTF-8")
            
            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$testOrigin&destination=$encodedDestination&mode=transit&departure_time=now&key=$apiKey"
            
            Log.d("FastestRouteService", "Testing API with URL: $url")
            
            val response = URL(url).readText()
            Log.d("FastestRouteService", "Test API Response: $response")
            
            val json = JSONObject(response)
            val status = json.getString("status")
            
            if (status != "OK") {
                Log.e("FastestRouteService", "Test API Error: $status")
                return@withContext emptyList()
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                Log.w("FastestRouteService", "Test: No routes found")
                return@withContext emptyList()
            }
            
            val steps = mutableListOf<TransitStep>()
            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            
            if (legs.length() == 0) {
                Log.w("FastestRouteService", "Test: No legs found in route")
                return@withContext emptyList()
            }
            
            val leg = legs.getJSONObject(0)
            val totalDuration = leg.getJSONObject("duration").getString("text")
            val totalDistance = leg.getJSONObject("distance").getString("text")
            
            Log.d("FastestRouteService", "Test: Total route: $totalDuration, $totalDistance")
            
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
            
            Log.d("FastestRouteService", "Test: Found ${steps.size} steps")
            steps
            
        } catch (e: Exception) {
            Log.e("FastestRouteService", "Test: Error getting directions", e)
            emptyList()
        }
    }
} 