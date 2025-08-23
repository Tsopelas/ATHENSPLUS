package com.example.athensplus.core.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WalkingRoute(
    val startLocation: LatLng,
    val endLocation: LatLng,
    val polylinePoints: List<LatLng>,
    val duration: String,
    val distance: String
)

class WalkingDirectionsService(private val apiKey: String) {
    
    companion object {
        private const val TAG = "WalkingDirectionsService"
        private const val DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json"
        private const val GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json"
    }
    
    suspend fun getWalkingRoute(startAddress: String, endAddress: String): WalkingRoute? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting walking route from '$startAddress' to '$endAddress'")

                val startCoords = geocodeAddress(startAddress)
                val endCoords = geocodeAddress(endAddress)
                
                if (startCoords == null || endCoords == null) {
                    Log.e(TAG, "Failed to geocode addresses: startCoords=$startCoords, endCoords=$endCoords")
                    return@withContext null
                }
                
                Log.d(TAG, "Successfully geocoded addresses: start=(${startCoords.latitude}, ${startCoords.longitude}), end=(${endCoords.latitude}, ${endCoords.longitude})")

                getWalkingDirections(startCoords, endCoords)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting walking route", e)
                null
            }
        }
    }
    
    suspend fun getWalkingRoute(startLocation: LatLng, endLocation: LatLng): WalkingRoute? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting walking route from coordinates (${startLocation.latitude}, ${startLocation.longitude}) to (${endLocation.latitude}, ${endLocation.longitude})")

                getWalkingDirections(startLocation, endLocation)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting walking route from coordinates", e)
                null
            }
        }
    }
    
    suspend fun geocodeAddress(address: String): LatLng? {
        return try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val url = "$GEOCODING_API_URL?address=$encodedAddress&key=$apiKey"
            
            Log.d(TAG, "Geocoding address: $address")
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonObject = JSONObject(response)
                
                val status = jsonObject.getString("status")
                if (status == "OK") {
                    val results = jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                        
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")
                        
                        Log.d(TAG, "Geocoded '$address' to ($lat, $lng)")
                        LatLng(lat, lng)
                    } else {
                        Log.w(TAG, "No geocoding results for address: $address")
                        null
                    }
                } else {
                    Log.e(TAG, "Geocoding API error: $status")
                    null
                }
            } else {
                Log.e(TAG, "Geocoding HTTP error: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding address: $address", e)
            null
        }
    }
    
    private suspend fun getWalkingDirections(start: LatLng, end: LatLng): WalkingRoute? {
        return try {
            val origin = "${start.latitude},${start.longitude}"
            val destination = "${end.latitude},${end.longitude}"
            
            val url = "$DIRECTIONS_API_URL?" +
                    "origin=$origin&" +
                    "destination=$destination&" +
                    "mode=walking&" +
                    "key=$apiKey"
            
            Log.d(TAG, "Getting walking directions from $origin to $destination")
            Log.d(TAG, "API URL: $url")
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Walking directions HTTP response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                Log.d(TAG, "Walking directions API response: $response")
                parseWalkingDirections(response, start, end)
            } else {
                Log.e(TAG, "Walking directions HTTP error: $responseCode")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting walking directions", e)
            null
        }
    }
    
    private fun parseWalkingDirections(response: String, start: LatLng, end: LatLng): WalkingRoute? {
        return try {
            val jsonObject = JSONObject(response)
            val status = jsonObject.getString("status")
            
            if (status != "OK") {
                Log.e(TAG, "Directions API error: $status")
                return null
            }
            
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() == 0) {
                Log.e(TAG, "No routes found")
                return null
            }
            
            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            val leg = legs.getJSONObject(0)
            
            // Get duration and distance
            val duration = leg.getJSONObject("duration").getString("text")
            val distance = leg.getJSONObject("distance").getString("text")
            
            // Get polyline points
            val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
            val polylinePoints = decodePolyline(overviewPolyline)
            
            Log.d(TAG, "Walking route found: $duration, $distance, ${polylinePoints.size} points")
            
            WalkingRoute(
                startLocation = start,
                endLocation = end,
                polylinePoints = polylinePoints,
                duration = duration,
                distance = distance
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing walking directions", e)
            null
        }
    }
    
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        
        return poly
    }
}
