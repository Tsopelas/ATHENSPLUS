package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.domain.model.TransitStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL

class FastestRouteService(
    private val context: Context, 
    private val apiKey: String,
    private val locationService: LocationService? = null
) {
    
    suspend fun getFastestRoute(
        from: String,
        to: String
    ): List<TransitStep> = withContext(Dispatchers.IO) {
        try {
            // Handle current location case
            val origin = if (from.contains("Current Location", ignoreCase = true)) {
                // Get actual current location
                val currentLocation = locationService?.getCurrentLocation()
                if (currentLocation != null) {
                    Log.d("FastestRouteService", "Using actual location: ${currentLocation.latitude}, ${currentLocation.longitude}")
                    "${currentLocation.latitude},${currentLocation.longitude}"
                } else {
                    Log.w("FastestRouteService", "Could not get current location, using fallback")
                    "37.9838,23.7275" // Athens center coordinates as fallback
                }
            } else {
                URLEncoder.encode(from, "UTF-8")
            }
            
            val destination = URLEncoder.encode(to, "UTF-8")
            
            // Try transit first
            var url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&mode=transit&departure_time=now&key=$apiKey"
            
            Log.d("FastestRouteService", "Requesting transit directions from: $from to: $to")
            Log.d("FastestRouteService", "URL: $url")
            
            var response = URL(url).readText()
            Log.d("FastestRouteService", "API Response: $response")
            
            var json = JSONObject(response)
            var status = json.getString("status")
            
            // If transit fails, try walking
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
            val stepsArray = leg.getJSONArray("steps")
            
            for (i in 0 until stepsArray.length()) {
                val step = stepsArray.getJSONObject(i)
                val travelMode = step.getString("travel_mode")
                val instruction = step.getString("html_instructions")
                val duration = step.getJSONObject("duration").getString("text")
                
                // Get distance if available
                val distance = step.optJSONObject("distance")?.getString("text")
                
                // Get transit details if this is a transit step
                val transitDetails = step.optJSONObject("transit_details")
                val line = transitDetails?.optJSONObject("line")?.optString("short_name") ?: ""
                val departureStop = transitDetails?.optJSONObject("departure_stop")?.optString("name") ?: ""
                val arrivalStop = transitDetails?.optJSONObject("arrival_stop")?.optString("name") ?: ""
                val vehicleType = transitDetails?.optJSONObject("line")?.optJSONObject("vehicle")?.optString("type")
                
                steps.add(
                    TransitStep(
                        mode = travelMode,
                        instruction = instruction,
                        duration = duration,
                        line = line,
                        departureStop = departureStop,
                        arrivalStop = arrivalStop,
                        walkingDistance = distance,
                        vehicleType = vehicleType
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
    
    // Test function to verify API is working
    suspend fun testDirections(): List<TransitStep> = withContext(Dispatchers.IO) {
        try {
            // Use actual current location for testing
            val testOrigin = locationService?.getCurrentLocation()?.let { 
                "${it.latitude},${it.longitude}" 
            } ?: "37.9838,23.7275" // Athens center as fallback
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
            
            // Parse the same way as main function
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
                
                steps.add(
                    TransitStep(
                        mode = travelMode,
                        instruction = instruction,
                        duration = duration,
                        line = line,
                        departureStop = departureStop,
                        arrivalStop = arrivalStop,
                        walkingDistance = distance,
                        vehicleType = vehicleType
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