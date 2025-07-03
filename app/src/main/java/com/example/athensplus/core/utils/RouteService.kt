package com.example.athensplus.core.utils


import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.domain.model.DirectionsResult
import com.example.athensplus.domain.model.TransitStep
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RouteService(private val fragment: Fragment) {
    private val apiKey = "AIzaSyAbCaNy9okak33ITCpb1MWR_Idu6wqQq14"

    fun findRoute(
        destination: String,
        onSuccess: (DirectionsResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (destination.trim().isEmpty()) {
            onError("Please enter a destination")
            return
        }

        fragment.lifecycleScope.launch {
            try {
                val directionsResult = getDirections("Athens, Greece", destination)
                withContext(Dispatchers.Main) {
                    if (directionsResult != null) {
                        onSuccess(directionsResult)
                    } else {
                        onError("Could not find route to destination")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Error finding route: ${e.message}")
                }
            }
        }
    }

    private suspend fun getDirections(origin: String, destination: String): DirectionsResult? {
        return withContext(Dispatchers.IO) {
            try {
                val originEncoded = URLEncoder.encode("$origin, Athens, Greece", "UTF-8")
                val destinationEncoded = URLEncoder.encode("$destination, Athens, Greece", "UTF-8")

                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=$originEncoded" +
                        "&destination=$destinationEncoded" +
                        "&mode=transit" +
                        "&transit_mode=bus|subway" +
                        "&region=gr" +
                        "&language=en" +
                        "&key=$apiKey"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    parseDirections(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseDirections(response: String): DirectionsResult? {
        try {
            val json = JSONObject(response)
            val status = json.getString("status")

            if (status != "OK") {
                return null
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return null
            }

            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            val leg = legs.getJSONObject(0)

            val startLocation = leg.getJSONObject("start_location")
            val endLocation = leg.getJSONObject("end_location")
            val startLatLng = LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng"))
            val endLatLng = LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng"))

            val steps = leg.getJSONArray("steps")
            val transitSteps = mutableListOf<TransitStep>()
            val routePoints = mutableListOf<LatLng>()

            routePoints.add(startLatLng)

            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                val travelMode = step.getString("travel_mode")
                val duration = step.getJSONObject("duration").getString("text")
                val instructions = step.getString("html_instructions")
                    .replace("<[^>]*>".toRegex(), "")
                    .replace("&nbsp;", " ")

                val stepEndLocation = step.getJSONObject("end_location")
                routePoints.add(LatLng(stepEndLocation.getDouble("lat"), stepEndLocation.getDouble("lng")))

                when (travelMode) {
                    "WALKING" -> {
                        transitSteps.add(TransitStep("WALKING", instructions, duration))
                    }
                    "TRANSIT" -> {
                        val transitDetails = step.getJSONObject("transit_details")
                        val line = transitDetails.getJSONObject("line")
                        val vehicle = line.getJSONObject("vehicle")
                        val vehicleType = vehicle.getString("type")
                        val lineName = if (line.has("short_name")) line.getString("short_name") else line.getString("name")

                        val departureStop = transitDetails.getJSONObject("departure_stop").getString("name")
                        val arrivalStop = transitDetails.getJSONObject("arrival_stop").getString("name")

                        val mode = when (vehicleType) {
                            "SUBWAY", "METRO_RAIL" -> "METRO"
                            "BUS" -> "BUS"
                            "TRAM" -> "TRAM"
                            else -> "TRANSIT"
                        }

                        val detailedInstruction = "Take $lineName from $departureStop to $arrivalStop"
                        transitSteps.add(TransitStep(mode, detailedInstruction, duration, lineName, departureStop, arrivalStop))
                    }
                }
            }

            return DirectionsResult(
                startLocation = startLatLng,
                endLocation = endLatLng,
                routePoints = routePoints,
                steps = transitSteps,
                totalDuration = leg.getJSONObject("duration").getString("text"),
                totalDistance = leg.getJSONObject("distance").getString("text")
            )

        } catch (e: Exception) {
            return null
        }
    }
} 