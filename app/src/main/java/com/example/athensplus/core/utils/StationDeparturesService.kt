package com.example.athensplus.core.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class BusDeparture(
    val busCode: String,
    val direction: String,
    val arrivalTime: String,
    val departureTime: String,
    val isExpress: Boolean = false,
    val isNightLine: Boolean = false
)

class StationDeparturesService(private val apiKey: String) {
    
    companion object {
        private const val TAG = "StationDeparturesService"
    }
    
    suspend fun getStationDepartures(stationName: String): List<BusDeparture> = withContext(Dispatchers.IO) {
        try {
            val encodedStation = URLEncoder.encode(stationName, "UTF-8")
            val currentTime = System.currentTimeMillis() / 1000

            val destinations = listOf(
                "Syntagma", "Omonia", "Monastiraki", "Thiseio", "Acropolis", "Panepistimio", "Akadimia",
                "Kifisia", "Marousi", "Chalandri", "Agia Paraskevi", "Holargos", "Nomismatokopio",
                "Piraeus", "Keratsini", "Nikea", "Perama", "Drapetsona", "Korydallos",
                "Airport", "Koropi", "Pallini", "Paiania", "Markopoulo",
                "Elliniko", "Alimos", "Glyfada", "Voula", "Vouliagmeni", "Varkiza",
                "Anthoupoli", "Peristeri", "Aigaleo", "Petroupoli", "Ilion", "Kaminia",
                // Additional destinations to catch more routes
                "Nea Ionia", "Irakleio", "Metamorfosi", "Lykovrysi", "Pefki", "Melissia",
                "Vrilissia", "Penteli", "Dionysos", "Ekali", "Agios Stefanos",
                "Kallithea", "Tavros", "Moschato", "Faliro", "Palaio Faliro",
                "Kaisariani", "Vyronas", "Zografou", "Ilisia", "Kallimarmaro",
                "Goudi", "Katehaki", "Ethniki Amyna", "Holargos", "Nomismatokopio",
                "Aghia Paraskevi", "Cholargos", "Ethniki Amyna", "Katehaki",
                // Bus-specific destinations
                "Evrou 36", "Evrou 36 Athens", "Evrou 36, Athens", "Evrou 36 Station",
                "Alexandras", "Alexandras Avenue", "Alexandras, Athens",
                "Patision", "Patision Avenue", "Patision, Athens",
                "Vasilissis Sofias", "Vasilissis Sofias Avenue",
                "Kifisias", "Kifisias Avenue", "Kifisias, Athens",
                "Mesogeion", "Mesogeion Avenue", "Mesogeion, Athens",
                "Vouliagmenis", "Vouliagmenis Avenue", "Vouliagmenis, Athens",
                "Syngrou", "Syngrou Avenue", "Syngrou, Athens",
                "Pireos", "Pireos Avenue", "Pireos, Athens",
                "Acharnon", "Acharnon Avenue", "Acharnon, Athens",
                "Lenorman", "Lenorman Avenue", "Lenorman, Athens",
                "Iera Odos", "Iera Odos, Athens",
                "Athinon", "Athinon Avenue", "Athinon, Athens",
                "Peiraios", "Peiraios Avenue", "Peiraios, Athens",
                "Kifisou", "Kifisou Avenue", "Kifisou, Athens",
                "Leoforos Alexandras", "Leoforos Alexandras, Athens",
                "Leoforos Patision", "Leoforos Patision, Athens",
                "Leoforos Kifisias", "Leoforos Kifisias, Athens",
                "Leoforos Mesogeion", "Leoforos Mesogeion, Athens",
                "Leoforos Vouliagmenis", "Leoforos Vouliagmenis, Athens",
                "Leoforos Syngrou", "Leoforos Syngrou, Athens",
                "Leoforos Pireos", "Leoforos Pireos, Athens",
                "Leoforos Acharnon", "Leoforos Acharnon, Athens",
                "Leoforos Lenorman", "Leoforos Lenorman, Athens",
                "Leoforos Iera Odos", "Leoforos Iera Odos, Athens",
                "Leoforos Athinon", "Leoforos Athinon, Athens",
                "Leoforos Peiraios", "Leoforos Peiraios, Athens",
                "Leoforos Kifisou", "Leoforos Kifisou, Athens"
            )
            
            val allDepartures = mutableListOf<BusDeparture>()
            
            for (destination in destinations) {
                val encodedDestination = URLEncoder.encode(destination, "UTF-8")
                
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=$encodedStation&destination=$encodedDestination&mode=transit&" +
                        "alternatives=true&departure_time=now&key=$apiKey"
                
                Log.d(TAG, "Fetching departures from $stationName to $destination")
                
                try {
                    val response = URL(url).readText()
                    val json = JSONObject(response)
                    val status = json.getString("status")
                    
                    if (status == "OK") {
                        val routes = json.getJSONArray("routes")
                        
                        for (i in 0 until routes.length()) {
                            val route = routes.getJSONObject(i)
                            val legs = route.getJSONArray("legs")
                            
                            for (j in 0 until legs.length()) {
                                val leg = legs.getJSONObject(j)
                                val steps = leg.getJSONArray("steps")
                                
                                for (k in 0 until steps.length()) {
                                    val step = steps.getJSONObject(k)
                                    val travelMode = step.getString("travel_mode")
                                    
                                    if (travelMode == "TRANSIT") {
                                        val transitDetails = step.optJSONObject("transit_details")
                                        if (transitDetails != null) {
                                            val line = transitDetails.optJSONObject("line")
                                            val departureStop = transitDetails.optJSONObject("departure_stop")
                                            val arrivalStop = transitDetails.optJSONObject("arrival_stop")
                                            val departureTime = transitDetails.optJSONObject("departure_time")
                                            
                                            if (line != null && departureStop != null && departureTime != null) {
                                                val busCode = line.optString("short_name", "")
                                                val lineName = line.optString("name", "")
                                                val departureStopName = departureStop.optString("name", "")
                                                val arrivalStopName = arrivalStop?.optString("name", "") ?: destination
                                                val departureTimeText = departureTime.optString("text", "")
                                                val departureTimeValue = departureTime.optLong("value", 0L)

                                                val isCorrectStation = isStationMatch(departureStopName, stationName) && busCode.isNotEmpty()
                                                
                                                if (isCorrectStation) {
                                                    val direction = "$departureStopName-$arrivalStopName"

                                                    val currentTimeSeconds = System.currentTimeMillis() / 1000
                                                    val timeUntilDeparture = (departureTimeValue - currentTimeSeconds) / 60
                                                    val arrivalTimeText = if (timeUntilDeparture > 0) "${timeUntilDeparture.toInt()}'" else "Now"

                                                    val isExpress = lineName.contains("EXPRESS", ignoreCase = true)
                                                    val isNightLine = lineName.contains("NIGHT", ignoreCase = true)
                                                    
                                                    val departure = BusDeparture(
                                                        busCode = busCode,
                                                        direction = direction,
                                                        arrivalTime = arrivalTimeText,
                                                        departureTime = departureTimeText,
                                                        isExpress = isExpress,
                                                        isNightLine = isNightLine
                                                    )

                                                    if (!allDepartures.any { it.busCode == busCode && it.direction == direction }) {
                                                        allDepartures.add(departure)
                                                        Log.d(TAG, "Found departure: $busCode from $departureStopName to $arrivalStopName in ${arrivalTimeText}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "API returned status: $status for destination: $destination")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching departures to $destination", e)
                    // Continue with next destination
                }
            }

            val uniqueDepartures = allDepartures
                .groupBy { it.busCode }
                .mapValues { (_, departures) ->
                    departures.minByOrNull { departure ->
                        val timeStr = departure.arrivalTime.replace("'", "").replace("Now", "0")
                        timeStr.toIntOrNull() ?: 999
                    }
                }
                .values
                .filterNotNull()
                .sortedBy { departure ->
                    val timeStr = departure.arrivalTime.replace("'", "").replace("Now", "0")
                    timeStr.toIntOrNull() ?: 999
                }
            
            Log.d(TAG, "Found ${uniqueDepartures.size} unique departures for $stationName: ${uniqueDepartures.map { "${it.busCode}(${it.arrivalTime})" }}")
            uniqueDepartures
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching station departures for $stationName", e)
            emptyList()
        }
    }
    
    private fun isStationMatch(apiStationName: String, targetStationName: String): Boolean {

        val normalizedApi = apiStationName.lowercase().trim()
        val normalizedTarget = targetStationName.lowercase().trim()

        if (normalizedApi == normalizedTarget) return true

        if (normalizedApi.contains(normalizedTarget) || normalizedTarget.contains(normalizedApi)) return true

        val variations = mapOf(
            "alex" to listOf("alexandras", "alexandras avenue", "leoforos alexandras"),
            "evrou 36" to listOf("evrou 36", "evrou 36 station", "evrou 36, athens"),
            "syntagma" to listOf("syntagma", "syntagma square", "plateia syntagmatos"),
            "omonia" to listOf("omonia", "omonia square", "plateia omonias"),
            "kifisia" to listOf("kifisia", "kifisia station", "kifisia, athens"),
            "piraeus" to listOf("piraeus", "piraeus port", "piraeus, athens"),
            "airport" to listOf("airport", "athens airport", "eleftherios venizelos airport")
        )

        for ((key, values) in variations) {
            if (normalizedTarget.contains(key) || key.contains(normalizedTarget)) {
                if (values.any { normalizedApi.contains(it) || it.contains(normalizedApi) }) {
                    return true
                }
            }
        }
        
        return false
    }
}
