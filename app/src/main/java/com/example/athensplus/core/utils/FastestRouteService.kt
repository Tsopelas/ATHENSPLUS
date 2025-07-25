package com.example.athensplus.core.utils

import android.util.Log
import com.example.athensplus.domain.model.TransitStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL

class FastestRouteService(
    private val apiKey: String,
    private val locationService: LocationService? = null
) {
    
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