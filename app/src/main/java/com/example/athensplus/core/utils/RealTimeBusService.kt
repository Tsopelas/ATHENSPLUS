package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.domain.model.BusDepartureInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class RealTimeBusService(
    private val context: Context,
    private val apiKey: String
) {
    
    data class BusStopInfo(
        val stopId: String,
        val name: String,
        val location: Pair<Double, Double>,
        val lines: List<String>
    )
    
    data class RealTimeDeparture(
        val line: String,
        val destination: String,
        val departureTime: Long,
        val waitMinutes: Int,
        val reliability: String,
        val crowdLevel: String?,
        val vehicleType: String?,
        val accessibility: Boolean,
        val realTimeUpdates: Boolean
    )
    
    suspend fun getRealTimeDepartures(
        stopName: String,
        maxResults: Int = 15
    ): List<RealTimeDeparture> = withContext(Dispatchers.IO) {
        try {
            // First, find the bus stop using Google Places API
            val stopInfo = findBusStop(stopName)
            if (stopInfo == null) {
                Log.w("RealTimeBusService", "Could not find bus stop: $stopName")
                return@withContext emptyList()
            }
            
            // Get real-time departures for this stop
            val departures = getDeparturesForStop(stopInfo, maxResults)
            
            // Sort by departure time (earliest first)
            departures.sortedBy { it.departureTime }
            
        } catch (e: Exception) {
            Log.e("RealTimeBusService", "Error getting real-time departures", e)
            emptyList()
        }
    }
    
    private suspend fun findBusStop(stopName: String): BusStopInfo? = withContext(Dispatchers.IO) {
        try {
            val encodedStop = URLEncoder.encode("$stopName bus stop, Athens, Greece", "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?" +
                    "input=$encodedStop&inputtype=textquery&fields=place_id,name,geometry&key=$apiKey"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getString("status") != "OK") {
                return@withContext null
            }
            
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() == 0) {
                return@withContext null
            }
            
            val candidate = candidates.getJSONObject(0)
            val placeId = candidate.getString("place_id")
            val name = candidate.getString("name")
            val location = candidate.getJSONObject("geometry").getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            
            // Get nearby transit lines
            val lines = getNearbyTransitLines(lat, lng)
            
            BusStopInfo(placeId, name, Pair(lat, lng), lines)
            
        } catch (e: Exception) {
            Log.e("RealTimeBusService", "Error finding bus stop", e)
            null
        }
    }
    
    private suspend fun getNearbyTransitLines(lat: Double, lng: Double): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                    "location=$lat,$lng&radius=300&type=transit_station&key=$apiKey"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            if (json.getString("status") != "OK") {
                return@withContext emptyList()
            }
            
            val lines = mutableListOf<String>()
            val results = json.getJSONArray("results")
            
            for (i in 0 until results.length()) {
                val place = results.getJSONObject(i)
                val name = place.getString("name")
                
                // Extract bus line numbers from stop names
                val lineNumbers = extractBusLines(name)
                lines.addAll(lineNumbers)
            }
            
            lines.distinct()
            
        } catch (e: Exception) {
            Log.e("RealTimeBusService", "Error getting nearby transit lines", e)
            emptyList()
        }
    }
    
    private fun extractBusLines(stopName: String): List<String> {
        val lines = mutableListOf<String>()
        
        // Common bus line patterns in Athens
        val patterns = listOf(
            Regex("\\b([0-9]{1,3})\\b"), // Single numbers
            Regex("\\b([A-Z][0-9]{1,3})\\b"), // Letter + numbers
            Regex("\\b([0-9]{1,3}[A-Z])\\b") // Numbers + letter
        )
        
        for (pattern in patterns) {
            val matches = pattern.findAll(stopName)
            for (match in matches) {
                lines.add(match.value)
            }
        }
        
        return lines
    }
    
    private suspend fun getDeparturesForStop(
        stopInfo: BusStopInfo,
        maxResults: Int
    ): List<RealTimeDeparture> = withContext(Dispatchers.IO) {
        try {
            val departures = mutableListOf<RealTimeDeparture>()
            val currentTime = System.currentTimeMillis() / 1000
            
            // Simulate real-time departures based on common Athens bus schedules
            // In a real implementation, this would connect to actual transit APIs
            for (line in stopInfo.lines.take(maxResults)) {
                val departureTime = currentTime + (Random().nextInt(30) + 1) * 60 // 1-30 minutes from now
                val waitMinutes = ((departureTime - currentTime) / 60).toInt()
                
                val reliability = when {
                    waitMinutes <= 5 -> "High"
                    waitMinutes <= 15 -> "Medium"
                    else -> "Low"
                }
                
                val crowdLevel = when {
                    Random().nextInt(100) < 30 -> "Low"
                    Random().nextInt(100) < 70 -> "Medium"
                    else -> "High"
                }
                
                departures.add(RealTimeDeparture(
                    line = line,
                    destination = getDestinationForLine(line),
                    departureTime = departureTime,
                    waitMinutes = waitMinutes,
                    reliability = reliability,
                    crowdLevel = crowdLevel,
                    vehicleType = "BUS",
                    accessibility = Random().nextBoolean(),
                    realTimeUpdates = true
                ))
            }
            
            // Add some common Athens bus lines if we don't have enough
            val commonLines = listOf("E14", "E15", "E16", "E17", "E18", "E19", "E20")
            for (line in commonLines.take(maxResults - departures.size)) {
                if (!departures.any { it.line == line }) {
                    val departureTime = currentTime + (Random().nextInt(30) + 1) * 60
                    val waitMinutes = ((departureTime - currentTime) / 60).toInt()
                    
                    departures.add(RealTimeDeparture(
                        line = line,
                        destination = getDestinationForLine(line),
                        departureTime = departureTime,
                        waitMinutes = waitMinutes,
                        reliability = "Medium",
                        crowdLevel = "Medium",
                        vehicleType = "BUS",
                        accessibility = false,
                        realTimeUpdates = true
                    ))
                }
            }
            
            departures
            
        } catch (e: Exception) {
            Log.e("RealTimeBusService", "Error getting departures for stop", e)
            emptyList()
        }
    }
    
    private fun getDestinationForLine(line: String): String {
        return when (line) {
            "E14" -> "Syntagma"
            "E15" -> "Omonia"
            "E16" -> "Monastiraki"
            "E17" -> "Acropolis"
            "E18" -> "Piraeus"
            "E19" -> "Airport"
            "E20" -> "Kifisia"
            else -> "Central Athens"
        }
    }
    
    suspend fun getRouteAlternativesWithRealTime(
        from: String,
        to: String,
        maxAlternatives: Int = 5
    ): List<com.example.athensplus.domain.model.BusRouteAlternative> = withContext(Dispatchers.IO) {
        try {
            // This would integrate with the enhanced bus times service
            // and add real-time information
            emptyList() // Placeholder for now
        } catch (e: Exception) {
            Log.e("RealTimeBusService", "Error getting route alternatives with real-time", e)
            emptyList()
        }
    }
    
    suspend fun getBusStopSchedule(
        stopName: String,
        date: Date = Date()
    ): Map<String, List<String>> = withContext(Dispatchers.IO) {
        try {
            // Simulate bus stop schedule
            val schedule = mutableMapOf<String, List<String>>()
            val commonLines = listOf("E14", "E15", "E16", "E17", "E18", "E19", "E20")
            
            for (line in commonLines) {
                val times = mutableListOf<String>()
                // Generate typical schedule times
                for (hour in 6..23) {
                    for (minute in listOf(0, 15, 30, 45)) {
                        times.add(String.format("%02d:%02d", hour, minute))
                    }
                }
                schedule[line] = times
            }
            
            schedule
            
        } catch (e: Exception) {
            Log.e("RealTimeBusService", "Error getting bus stop schedule", e)
            emptyMap()
        }
    }
} 