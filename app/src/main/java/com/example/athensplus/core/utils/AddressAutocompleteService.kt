package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.domain.model.AddressSuggestion
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class AddressAutocompleteService(
    private val context: Context,
    private val apiKey: String
) {
    
    suspend fun getAddressSuggestions(query: String, userLocation: com.google.android.gms.maps.model.LatLng? = null): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        try {
            if (query.length < 2) return@withContext emptyList()
            
            val suggestions = mutableListOf<AddressSuggestion>()
            
            // Use single, precise query with proper parameters for maximum accuracy
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            
            // Build URL with industry-standard parameters
            val urlBuilder = StringBuilder("https://maps.googleapis.com/maps/api/place/autocomplete/json?")
            urlBuilder.append("input=$encodedQuery&")
            urlBuilder.append("types=geocode|establishment&") // Simplified types for better compatibility
            urlBuilder.append("components=country:gr&") // Greece only
            urlBuilder.append("language=en&") // Prefer English results
            urlBuilder.append("key=$apiKey")
            
            // Add location bias if user location is available (less restrictive)
            if (userLocation != null) {
                urlBuilder.append("&location=${userLocation.latitude},${userLocation.longitude}&")
                urlBuilder.append("radius=100000&") // Increased radius to 100km
                // Removed strictbounds to be less restrictive
            }
            
            val url = urlBuilder.toString()
            Log.d("AddressAutocomplete", "Requesting suggestions for: '$query'")
            Log.d("AddressAutocomplete", "URL: $url")
            
            try {
                val response = URL(url).readText()
                val jsonResponse = JSONObject(response)
                
                Log.d("AddressAutocomplete", "API Response status: ${jsonResponse.getString("status")}")
                Log.d("AddressAutocomplete", "API Response: $response")
                
                if (jsonResponse.getString("status") == "OK") {
                    val predictions = jsonResponse.getJSONArray("predictions")
                    Log.d("AddressAutocomplete", "Found ${predictions.length()} predictions")
                    
                    for (i in 0 until predictions.length()) {
                        val prediction = predictions.getJSONObject(i)
                        val placeId = prediction.getString("place_id")
                        val description = prediction.getString("description")
                        
                        Log.d("AddressAutocomplete", "Prediction $i: $description")
                        
                        // Parse the description to extract information
                        val parsedInfo = parseDescription(description)
                        
                        // Only add if not a duplicate
                        if (!isDuplicateSuggestion(suggestions, parsedInfo)) {
                            suggestions.add(
                                AddressSuggestion(
                                    id = UUID.randomUUID().toString(),
                                    address = parsedInfo.cleanAddress,
                                    description = description,
                                    placeId = placeId,
                                    latLng = null, // We'll get this later if needed
                                    area = parsedInfo.area,
                                    streetName = parsedInfo.streetName,
                                    streetNumber = parsedInfo.streetNumber,
                                    postalCode = parsedInfo.postalCode,
                                    establishmentName = parsedInfo.establishmentName,
                                    establishmentType = parsedInfo.establishmentType
                                )
                            )
                        }
                    }
                } else {
                    Log.w("AddressAutocomplete", "API returned status: ${jsonResponse.getString("status")}")
                    if (jsonResponse.has("error_message")) {
                        Log.w("AddressAutocomplete", "Error message: ${jsonResponse.getString("error_message")}")
                    }
                    
                    // If first attempt failed and we had location bias, try without it
                    if (userLocation != null && suggestions.isEmpty()) {
                        Log.d("AddressAutocomplete", "Trying fallback without location bias")
                        val fallbackUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json?" +
                                "input=$encodedQuery&" +
                                "types=geocode|establishment&" +
                                "components=country:gr&" +
                                "language=en&" +
                                "key=$apiKey"
                        
                        try {
                            val fallbackResponse = URL(fallbackUrl).readText()
                            val fallbackJsonResponse = JSONObject(fallbackResponse)
                            
                            Log.d("AddressAutocomplete", "Fallback API Response status: ${fallbackJsonResponse.getString("status")}")
                            
                            if (fallbackJsonResponse.getString("status") == "OK") {
                                val fallbackPredictions = fallbackJsonResponse.getJSONArray("predictions")
                                Log.d("AddressAutocomplete", "Found ${fallbackPredictions.length()} fallback predictions")
                                
                                for (i in 0 until fallbackPredictions.length()) {
                                    val prediction = fallbackPredictions.getJSONObject(i)
                                    val placeId = prediction.getString("place_id")
                                    val description = prediction.getString("description")
                                    
                                    Log.d("AddressAutocomplete", "Fallback Prediction $i: $description")
                                    
                                    val parsedInfo = parseDescription(description)
                                    
                                    if (!isDuplicateSuggestion(suggestions, parsedInfo)) {
                                        suggestions.add(
                                            AddressSuggestion(
                                                id = UUID.randomUUID().toString(),
                                                address = parsedInfo.cleanAddress,
                                                description = description,
                                                placeId = placeId,
                                                latLng = null,
                                                area = parsedInfo.area,
                                                streetName = parsedInfo.streetName,
                                                streetNumber = parsedInfo.streetNumber,
                                                postalCode = parsedInfo.postalCode,
                                                establishmentName = parsedInfo.establishmentName,
                                                establishmentType = parsedInfo.establishmentType
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AddressAutocomplete", "Error in fallback request", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AddressAutocomplete", "Error getting address suggestions", e)
            }
            
            // Sort by relevance and distance
            val sortedSuggestions = suggestions.sortedWith(compareBy<AddressSuggestion> { suggestion ->
                val queryLower = query.lowercase()
                val streetNameLower = suggestion.streetName?.lowercase() ?: ""
                val addressLower = suggestion.address.lowercase()
                val establishmentNameLower = suggestion.establishmentName?.lowercase() ?: ""
                
                when {
                    // Exact establishment name match (highest priority for places)
                    establishmentNameLower == queryLower -> 0
                    // Establishment name starts with query
                    establishmentNameLower.startsWith(queryLower) -> 1
                    // Exact street name match
                    streetNameLower == queryLower -> 2
                    // Street name starts with query
                    streetNameLower.startsWith(queryLower) -> 3
                    // Address starts with query
                    addressLower.startsWith(queryLower) -> 4
                    // Establishment name contains query
                    establishmentNameLower.contains(queryLower) -> 5
                    // Street name contains query
                    streetNameLower.contains(queryLower) -> 6
                    // Address contains query
                    addressLower.contains(queryLower) -> 7
                    else -> 8
                }
            }.thenBy { suggestion ->
                // Secondary: prefer addresses with numbers if query has numbers
                val queryHasNumber = query.contains(Regex("\\d+"))
                val suggestionHasNumber = suggestion.streetNumber != null
                when {
                    queryHasNumber && suggestionHasNumber -> 0
                    !queryHasNumber && !suggestionHasNumber -> 0
                    else -> 1
                }
            })
            
            Log.d("AddressAutocomplete", "Found ${sortedSuggestions.size} unique suggestions")
            sortedSuggestions.take(5) // Limit to top 5 for better UX
        } catch (e: Exception) {
            Log.e("AddressAutocomplete", "Error getting address suggestions", e)
            emptyList()
        }
    }
    

    

    
    private fun parseDescription(description: String): ParsedInfo {
        var cleanAddress = description
        var streetName: String? = null
        var streetNumber: String? = null
        var area: String? = null
        var postalCode: String? = null
        var establishmentName: String? = null
        var establishmentType: String? = null
        
        // Remove ", Greece" suffix
        if (cleanAddress.endsWith(", Greece")) {
            cleanAddress = cleanAddress.removeSuffix(", Greece")
        }
        
        // Remove postal codes from the end
        cleanAddress = cleanAddress.replace(Regex(", ?\\d{5}(?=,|$)"), "")
        
        // Split by commas to analyze parts
        val parts = cleanAddress.split(",").map { it.trim() }
        
        if (parts.isNotEmpty()) {
            val firstPart = parts[0]
            
            // Check if first part looks like an establishment (contains common establishment keywords)
            if (isLikelyEstablishment(firstPart)) {
                establishmentName = firstPart
                establishmentType = determineEstablishmentType(firstPart)
                
                // Look for street information in subsequent parts
                for (i in 1 until parts.size) {
                    val part = parts[i]
                    if (part.contains(Regex("\\d+")) && streetName == null) {
                        // This looks like a street with number
                        val streetMatch = Regex("(.+?)\\s+(\\d+)").find(part)
                        if (streetMatch != null) {
                            streetName = streetMatch.groupValues[1].trim()
                            streetNumber = streetMatch.groupValues[2]
                        }
                    } else if (area == null && !part.contains(Regex("\\d+"))) {
                        area = part
                    }
                }
            } else {
                // This looks like a regular address
                val streetMatch = Regex("(.+?)\\s+(\\d+)").find(firstPart)
                if (streetMatch != null) {
                    streetName = streetMatch.groupValues[1].trim()
                    streetNumber = streetMatch.groupValues[2]
                } else {
                    streetName = firstPart
                }
                
                // Look for area in subsequent parts
                for (i in 1 until parts.size) {
                    val part = parts[i]
                    if (area == null && !part.contains(Regex("\\d+"))) {
                        area = part
                    }
                }
            }
        }
        
        return ParsedInfo(
            cleanAddress = cleanAddress,
            streetName = streetName,
            streetNumber = streetNumber,
            area = area,
            postalCode = postalCode,
            establishmentName = establishmentName,
            establishmentType = establishmentType
        )
    }
    
    private fun isLikelyEstablishment(text: String): Boolean {
        val establishmentKeywords = listOf(
            "restaurant", "cafe", "bar", "hotel", "bank", "pharmacy", "hospital", "clinic",
            "school", "university", "department", "ministry", "police", "museum", "theater",
            "cinema", "mall", "store", "gas", "metro", "bus", "airport", "port",
            "ταβέρνα", "εστιατόριο", "καφέ", "καφενείο", "μπαρ", "ξενοδοχείο", "τράπεζα",
            "φαρμακείο", "νοσοκομείο", "κλινική", "σχολείο", "πανεπιστήμιο", "τμήμα",
            "υπουργείο", "αστυνομία", "μουσείο", "θέατρο", "κινηματογράφος", "εμπορικό",
            "κατάστημα", "βενζινάδικο", "μετρό", "λεωφορείο", "αεροδρόμιο", "λιμάνι"
        )
        
        val textLower = text.lowercase()
        return establishmentKeywords.any { keyword -> textLower.contains(keyword) }
    }
    

    
    private fun determineEstablishmentType(establishmentName: String?): String? {
        if (establishmentName == null) return null
        
        val name = establishmentName.lowercase()
        
        return when {
            name.contains("restaurant") || name.contains("ταβέρνα") || name.contains("εστιατόριο") -> "Restaurant"
            name.contains("cafe") || name.contains("καφέ") || name.contains("καφενείο") -> "Cafe"
            name.contains("bar") || name.contains("μπαρ") -> "Bar"
            name.contains("hotel") || name.contains("ξενοδοχείο") -> "Hotel"
            name.contains("bank") || name.contains("τράπεζα") -> "Bank"
            name.contains("pharmacy") || name.contains("φαρμακείο") -> "Pharmacy"
            name.contains("hospital") || name.contains("νοσοκομείο") -> "Hospital"
            name.contains("clinic") || name.contains("κλινική") -> "Clinic"
            name.contains("school") || name.contains("σχολείο") -> "School"
            name.contains("university") || name.contains("πανεπιστήμιο") -> "University"
            name.contains("department") || name.contains("τμήμα") -> "Department"
            name.contains("ministry") || name.contains("υπουργείο") -> "Ministry"
            name.contains("police") || name.contains("αστυνομία") -> "Police"
            name.contains("museum") || name.contains("μουσείο") -> "Museum"
            name.contains("theater") || name.contains("θέατρο") -> "Theater"
            name.contains("cinema") || name.contains("κινηματογράφος") -> "Cinema"
            name.contains("mall") || name.contains("mall") || name.contains("εμπορικό") -> "Shopping"
            name.contains("store") || name.contains("κατάστημα") -> "Store"
            name.contains("gas") || name.contains("βενζινάδικο") -> "Gas Station"
            name.contains("metro") || name.contains("μετρό") -> "Metro Station"
            name.contains("bus") || name.contains("λεωφορείο") -> "Bus Stop"
            name.contains("airport") || name.contains("αεροδρόμιο") -> "Airport"
            name.contains("port") || name.contains("λιμάνι") -> "Port"
            else -> "Place"
        }
    }
    

    
    private fun isDuplicateSuggestion(existingSuggestions: List<AddressSuggestion>, newInfo: ParsedInfo): Boolean {
        return existingSuggestions.any { existing ->
            // For establishments, check if same establishment name and area
            if (newInfo.establishmentName != null && existing.establishmentName != null) {
                existing.establishmentName.equals(newInfo.establishmentName, ignoreCase = true) &&
                existing.area == newInfo.area
            }
            // For addresses, check same street name and number
            else if (newInfo.streetName != null && existing.streetName != null) {
                existing.streetName == newInfo.streetName &&
                existing.streetNumber == newInfo.streetNumber &&
                existing.area == newInfo.area
            }
            // Fallback: exact address match
            else {
                existing.address.equals(newInfo.cleanAddress, ignoreCase = true)
            }
        }
    }
    
    private fun calculateDistance(point1: com.google.android.gms.maps.model.LatLng, point2: com.google.android.gms.maps.model.LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0].toDouble()
    }
    

    
    private data class ParsedInfo(
        val cleanAddress: String,
        val streetName: String?,
        val streetNumber: String?,
        val area: String?,
        val postalCode: String?,
        val establishmentName: String?,
        val establishmentType: String?
    )
} 