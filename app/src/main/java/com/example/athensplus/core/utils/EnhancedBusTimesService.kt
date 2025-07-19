package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.domain.model.TransitStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class EnhancedBusTimesService(
    private val context: Context,
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
                    "37.9838,23.7275" // Athens center fallback
                }
            } else {
                URLEncoder.encode(from, "UTF-8")
            }
            
            val destination = URLEncoder.encode(to, "UTF-8")
            val currentTime = System.currentTimeMillis() / 1000
            
            // Get multiple route alternatives
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
            
            // Process each route alternative
            for (i in 0 until minOf(routes.length(), maxAlternatives)) {
                val route = routes.getJSONObject(i)
                val legs = route.getJSONArray("legs")
                
                if (legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    val totalDuration = leg.getJSONObject("duration").getString("text")
                    val totalDistance = leg.getJSONObject("distance").getString("text")
                    
                    val steps = parseRouteSteps(leg, totalDuration, totalDistance)
                    
                    // Calculate timing information
                    val timingInfo = calculateTimingInfo(steps, currentTime)
                    
                    // Extract bus lines used
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
            
            // Sort by actual departure time (earliest first)
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
                
                // Calculate wait time for this transit step
                val waitTime = step.departureTimeValue - currentTime
                if (waitTime > 0) {
                    totalWaitTime += waitTime
                }
            }
        }
        
        return Triple(firstDepartureTime, lastArrivalTime, totalWaitTime)
    }
    
    suspend fun getBusStopDepartures(
        stopName: String,
        maxResults: Int = 10
    ): List<BusDeparture> = withContext(Dispatchers.IO) {
        try {
            // Use Google Places API to find the bus stop
            val encodedStop = URLEncoder.encode("$stopName bus stop, Athens, Greece", "UTF-8")
            val placesUrl = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?" +
                    "input=$encodedStop&inputtype=textquery&fields=place_id,geometry&key=$apiKey"
            
            val placesResponse = URL(placesUrl).readText()
            val placesJson = JSONObject(placesResponse)
            
            if (placesJson.getString("status") != "OK") {
                return@withContext emptyList()
            }
            
            val candidates = placesJson.getJSONArray("candidates")
            if (candidates.length() == 0) {
                return@withContext emptyList()
            }
            
            val placeId = candidates.getJSONObject(0).getString("place_id")
            
            // Get nearby transit stops using Places API
            val location = candidates.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            
            val nearbyUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                    "location=$lat,$lng&radius=500&type=transit_station&key=$apiKey"
            
            val nearbyResponse = URL(nearbyUrl).readText()
            val nearbyJson = JSONObject(nearbyResponse)
            
            val departures = mutableListOf<BusDeparture>()
            
            if (nearbyJson.getString("status") == "OK") {
                val results = nearbyJson.getJSONArray("results")
                
                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val placeName = place.getString("name")
                    
                    // Get transit details for this stop
                    val stopDepartures = getTransitDepartures(placeName, lat, lng)
                    departures.addAll(stopDepartures)
                }
            }
            
            // Sort by departure time and limit results
            departures.sortBy { it.departureTime }
            departures.take(maxResults)
            
        } catch (e: Exception) {
            Log.e("EnhancedBusTimesService", "Error getting bus stop departures", e)
            emptyList()
        }
    }
    
    private suspend fun getTransitDepartures(
        stopName: String,
        lat: Double,
        lng: Double
    ): List<BusDeparture> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis() / 1000
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$lat,$lng&destination=$lat,$lng&mode=transit&" +
                    "departure_time=now&key=$apiKey"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getString("status") != "OK") {
                return@withContext emptyList()
            }
            
            val departures = mutableListOf<BusDeparture>()
            val routes = json.getJSONArray("routes")
            
            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val legs = route.getJSONArray("legs")
                
                if (legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    val steps = leg.getJSONArray("steps")
                    
                    for (j in 0 until steps.length()) {
                        val step = steps.getJSONObject(j)
                        val transitDetails = step.optJSONObject("transit_details")
                        
                        if (transitDetails != null) {
                            val line = transitDetails.optJSONObject("line")?.optString("short_name") ?: ""
                            val destination = transitDetails.optJSONObject("arrival_stop")?.optString("name") ?: ""
                            val departureTime = transitDetails.optJSONObject("departure_time")?.optLong("value") ?: 0L
                            
                            if (departureTime > currentTime) {
                                val waitTime = departureTime - currentTime
                                val waitMinutes = (waitTime / 60).toInt()
                                
                                departures.add(BusDeparture(
                                    line = line,
                                    destination = destination,
                                    departureTime = departureTime,
                                    waitMinutes = waitMinutes,
                                    stopName = stopName
                                ))
                            }
                        }
                    }
                }
            }
            
            departures
            
        } catch (e: Exception) {
            Log.e("EnhancedBusTimesService", "Error getting transit departures", e)
            emptyList()
        }
    }
    
    data class BusDeparture(
        val line: String,
        val destination: String,
        val departureTime: Long,
        val waitMinutes: Int,
        val stopName: String
    )
} 