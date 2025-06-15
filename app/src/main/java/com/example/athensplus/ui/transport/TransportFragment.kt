package com.example.athensplus.ui.transport

import android.app.Dialog
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.databinding.FragmentTransportBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.*

class TransportFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentTransportBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null
    private var directionsButton: Button? = null
    private var currentTransitInstructions: List<TransitStep> = emptyList()
    private lateinit var geocoder: Geocoder
    
    // Google API Key - same as used for Maps
    private val apiKey = "AIzaSyAbCaNy9okak33ITCpb1MWR_Idu6wqQq14"

    data class TransitStep(
        val mode: String, // "WALKING", "BUS", "METRO", "TRANSIT"
        val instruction: String,
        val duration: String,
        val line: String? = null
    )

    data class MetroStation(
        val name: String,
        val coords: LatLng,
        val lines: List<String> // M1, M2, M3
    )

    // Real Athens Metro Stations with actual coordinates and lines
    private val metroStations = listOf(
        // Line 1 (Green) - Piraeus to Kifissia
        MetroStation("Piraeus", LatLng(37.9420, 23.6425), listOf("M1")),
        MetroStation("Faliro", LatLng(37.9475, 23.6580), listOf("M1")),
        MetroStation("Kallithea", LatLng(37.9520, 23.6720), listOf("M1")),
        MetroStation("Tavros", LatLng(37.9565, 23.6860), listOf("M1")),
        MetroStation("Petralona", LatLng(37.9610, 23.6980), listOf("M1")),
        MetroStation("Thiseio", LatLng(37.9750, 23.7190), listOf("M1")),
        MetroStation("Monastiraki", LatLng(37.9755, 23.7255), listOf("M1", "M3")),
        MetroStation("Omonia", LatLng(37.9840, 23.7280), listOf("M1", "M2")),
        MetroStation("Victoria", LatLng(37.9908, 23.7383), listOf("M1")),
        MetroStation("Attiki", LatLng(37.9950, 23.7450), listOf("M1")),
        MetroStation("Agios Nikolaos", LatLng(38.0020, 23.7520), listOf("M1")),
        MetroStation("Kato Patissia", LatLng(38.0080, 23.7580), listOf("M1")),
        MetroStation("Agios Eleftherios", LatLng(38.0140, 23.7640), listOf("M1")),
        MetroStation("Ano Patissia", LatLng(38.0200, 23.7700), listOf("M1")),
        MetroStation("Perissos", LatLng(38.0260, 23.7760), listOf("M1")),
        MetroStation("Pefkakia", LatLng(38.0320, 23.7820), listOf("M1")),
        MetroStation("Nea Ionia", LatLng(38.0380, 23.7880), listOf("M1")),
        MetroStation("Herakleio", LatLng(38.0440, 23.7940), listOf("M1")),
        MetroStation("Eirini", LatLng(38.0500, 23.8000), listOf("M1")),
        MetroStation("Neratziotissa", LatLng(38.0560, 23.8060), listOf("M1")),
        MetroStation("Kifissia", LatLng(38.0620, 23.8120), listOf("M1")),

        // Line 2 (Red) - Anthoupoli to Elliniko
        MetroStation("Anthoupoli", LatLng(38.0650, 23.6850), listOf("M2")),
        MetroStation("Peristeri", LatLng(38.0580, 23.6920), listOf("M2")),
        MetroStation("Agios Antonios", LatLng(38.0510, 23.6990), listOf("M2")),
        MetroStation("Sepolia", LatLng(38.0440, 23.7060), listOf("M2")),
        MetroStation("Attiki", LatLng(38.0370, 23.7130), listOf("M2")),
        MetroStation("Larissa Station", LatLng(38.0300, 23.7200), listOf("M2")),
        MetroStation("Metaxourghio", LatLng(38.0230, 23.7270), listOf("M2")),
        // Omonia already defined above
        MetroStation("Panepistimio", LatLng(37.9770, 23.7340), listOf("M2")),
        MetroStation("Syntagma", LatLng(37.9755, 23.7348), listOf("M2", "M3")),
        MetroStation("Acropoli", LatLng(37.9715, 23.7267), listOf("M2")),
        MetroStation("Sygrou-Fix", LatLng(37.9520, 23.7240), listOf("M2")),
        MetroStation("Neos Kosmos", LatLng(37.9450, 23.7280), listOf("M2")),
        MetroStation("Agios Ioannis", LatLng(37.9380, 23.7320), listOf("M2")),
        MetroStation("Dafni", LatLng(37.9310, 23.7360), listOf("M2")),
        MetroStation("Agios Dimitrios", LatLng(37.9240, 23.7400), listOf("M2")),
        MetroStation("Ilioupoli", LatLng(37.9170, 23.7440), listOf("M2")),
        MetroStation("Alimos", LatLng(37.9100, 23.7480), listOf("M2")),
        MetroStation("Argyroupoli", LatLng(37.9030, 23.7520), listOf("M2")),
        MetroStation("Elliniko", LatLng(37.8960, 23.7560), listOf("M2")),

        // Line 3 (Blue) - Nikaia to Airport
        MetroStation("Nikaia", LatLng(37.9640, 23.6440), listOf("M3")),
        MetroStation("Korydallos", LatLng(37.9680, 23.6520), listOf("M3")),
        MetroStation("Agia Varvara", LatLng(37.9720, 23.6600), listOf("M3")),
        MetroStation("Agia Marina", LatLng(37.9760, 23.6680), listOf("M3")),
        MetroStation("Eleonas", LatLng(37.9800, 23.6760), listOf("M3")),
        MetroStation("Kerameikos", LatLng(37.9780, 23.7080), listOf("M3")),
        // Monastiraki already defined above
        // Syntagma already defined above
        MetroStation("Evangelismos", LatLng(37.9794, 23.7416), listOf("M3")),
        MetroStation("Megaro Moussikis", LatLng(37.9833, 23.7500), listOf("M3")),
        MetroStation("Ambelokipi", LatLng(37.9872, 23.7583), listOf("M3")),
        MetroStation("Panormou", LatLng(37.9911, 23.7667), listOf("M3")),
        MetroStation("Katehaki", LatLng(37.9950, 23.7750), listOf("M3")),
        MetroStation("Ethniki Amyna", LatLng(37.9989, 23.7833), listOf("M3")),
        MetroStation("Holargos", LatLng(38.0028, 23.7917), listOf("M3")),
        MetroStation("Nomismatokopio", LatLng(38.0067, 23.8000), listOf("M3")),
        MetroStation("Agia Paraskevi", LatLng(38.0106, 23.8083), listOf("M3")),
        MetroStation("Halandri", LatLng(38.0145, 23.8167), listOf("M3")),
        MetroStation("Doukissis Plakentias", LatLng(38.0184, 23.8250), listOf("M3")),
        MetroStation("Airport", LatLng(37.9364, 23.9445), listOf("M3"))
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransportBinding.inflate(inflater, container, false)
        geocoder = Geocoder(requireContext(), Locale.getDefault())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.transport_map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        
        // Set up directions button click listener
        binding.buttonDirections.setOnClickListener {
            showTransitDirections()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Set map type to normal
        googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        
        // Disable traffic layer to focus on transit
        googleMap?.isTrafficEnabled = false
        
        // Enable zoom controls
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        
        // Configure map settings
        googleMap?.uiSettings?.isMapToolbarEnabled = false
        
        // Center on Athens and zoom to show transit
        val athensCenter = LatLng(37.9755, 23.7348) // Syntagma Square
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(athensCenter, 15f))
    }

    private fun showTransitDirections() {
        val fromText = binding.editFrom.text.toString().trim()
        val toText = binding.editTo.text.toString().trim()
        
        if (fromText.isEmpty() || toText.isEmpty()) {
            Toast.makeText(context, "Please enter both 'From' and 'To' locations", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Clear previous markers and polylines
        googleMap?.clear()
        hideDirectionsButton()
        
        // Show loading message
        Toast.makeText(context, "Finding best route...", Toast.LENGTH_SHORT).show()
        
        // Get directions from Google Directions API
        lifecycleScope.launch {
            try {
                val directionsResult = getGoogleDirections(fromText, toText)
                
                withContext(Dispatchers.Main) {
                    if (directionsResult != null) {
                        displayDirectionsResult(directionsResult)
                        Toast.makeText(context, "Route found!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not find route between locations", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error finding route: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TransportFragment", "Error getting directions", e)
                }
            }
        }
    }
    
    private suspend fun getGoogleDirections(origin: String, destination: String): DirectionsResult? {
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
                
                Log.d("TransportFragment", "Directions URL: $url")
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                Log.d("TransportFragment", "Response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    
                    Log.d("TransportFragment", "API Response: ${response.take(500)}...")
                    
                    parseDirectionsResponse(response)
                } else {
                    Log.e("TransportFragment", "HTTP Error: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("TransportFragment", "Error calling Directions API", e)
                null
            }
        }
    }
    
    private fun parseDirectionsResponse(response: String): DirectionsResult? {
        try {
            val json = JSONObject(response)
            val status = json.getString("status")
            
            if (status != "OK") {
                Log.e("TransportFragment", "Directions API error: $status")
                return null
            }
            
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                Log.e("TransportFragment", "No routes found")
                return null
            }
            
            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            val leg = legs.getJSONObject(0)
            
            // Get start and end locations
            val startLocation = leg.getJSONObject("start_location")
            val endLocation = leg.getJSONObject("end_location")
            val startLatLng = LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng"))
            val endLatLng = LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng"))
            
            // Parse steps
            val steps = leg.getJSONArray("steps")
            val transitSteps = mutableListOf<TransitStep>()
            val routePoints = mutableListOf<LatLng>()
            
            routePoints.add(startLatLng)
            
            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                val travelMode = step.getString("travel_mode")
                val duration = step.getJSONObject("duration").getString("text")
                val instructions = step.getString("html_instructions")
                    .replace("<[^>]*>".toRegex(), "") // Remove HTML tags
                    .replace("&nbsp;", " ")
                
                // Add step end location to route
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
                        transitSteps.add(TransitStep(mode, detailedInstruction, duration, lineName))
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
            Log.e("TransportFragment", "Error parsing directions response", e)
            return null
        }
    }
    
    data class DirectionsResult(
        val startLocation: LatLng,
        val endLocation: LatLng,
        val routePoints: List<LatLng>,
        val steps: List<TransitStep>,
        val totalDuration: String,
        val totalDistance: String
    )
    
    private fun displayDirectionsResult(result: DirectionsResult) {
        // Add markers for start and end points
        googleMap?.addMarker(
            MarkerOptions()
                .position(result.startLocation)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        
        googleMap?.addMarker(
            MarkerOptions()
                .position(result.endLocation)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        
        // Draw route on map
        drawRoute(result.routePoints, result.steps)
        
        // Store instructions for popup
        currentTransitInstructions = result.steps
        
        // Adjust camera to show the route
        val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
        result.routePoints.forEach { builder.include(it) }
        val bounds = builder.build()
        
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        
        // Show directions button
        showDirectionsButton()
    }
    
    private fun drawRoute(routePoints: List<LatLng>, steps: List<TransitStep>) {
        if (routePoints.size < 2) return
        
        // Draw route segments with different colors based on transport mode
        var currentStepIndex = 0
        
        for (i in 0 until routePoints.size - 1) {
            val currentStep = if (currentStepIndex < steps.size) steps[currentStepIndex] else steps.lastOrNull()
            
            val color = when (currentStep?.mode) {
                "WALKING" -> android.graphics.Color.GREEN
                "METRO" -> android.graphics.Color.BLUE
                "BUS" -> android.graphics.Color.rgb(255, 165, 0) // Orange
                "TRAM" -> android.graphics.Color.MAGENTA
                else -> android.graphics.Color.GRAY
            }
            
            val polylineOptions = PolylineOptions()
                .add(routePoints[i])
                .add(routePoints[i + 1])
                .width(6f)
                .color(color)
            
            googleMap?.addPolyline(polylineOptions)
            
            // Move to next step when we've drawn enough segments for current step
            if (i > 0 && currentStepIndex < steps.size - 1) {
                currentStepIndex++
            }
        }
    }
    
    private fun showDirectionsButton() {
        hideDirectionsButton()
        
        directionsButton = Button(requireContext()).apply {
            text = "Directions"
            setBackgroundResource(R.drawable.rounded_directions_button)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(32, 16, 32, 16)
            textSize = 16f
            elevation = 8f
            setOnClickListener {
                showDirectionsPopup()
            }
        }
        
        // Find the map fragment container (FrameLayout)
        val mapContainer = binding.root.findViewById<View>(R.id.transport_map).parent as ViewGroup
        
        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 32) // 32dp from bottom
        }
        
        if (mapContainer is FrameLayout) {
            val frameParams = FrameLayout.LayoutParams(layoutParams)
            frameParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            directionsButton?.layoutParams = frameParams
        } else {
            directionsButton?.layoutParams = layoutParams
        }
        
        mapContainer.addView(directionsButton)
    }
    
    private fun hideDirectionsButton() {
        directionsButton?.let { button ->
            (button.parent as? ViewGroup)?.removeView(button)
        }
        directionsButton = null
    }
    
    private fun showDirectionsPopup() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_directions)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val instructionsContainer = dialog.findViewById<LinearLayout>(R.id.instructions_container)
        val closeButton = dialog.findViewById<Button>(R.id.button_close)
        
        currentTransitInstructions.forEach { step ->
            val stepView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_transit_step, instructionsContainer, false)
            
            val modeIcon = stepView.findViewById<TextView>(R.id.text_mode)
            val instruction = stepView.findViewById<TextView>(R.id.text_instruction)
            val duration = stepView.findViewById<TextView>(R.id.text_duration)
            val line = stepView.findViewById<TextView>(R.id.text_line)
            
            when (step.mode) {
                "WALKING" -> modeIcon.text = "🚶"
                "BUS" -> modeIcon.text = "🚌"
                "METRO" -> modeIcon.text = "🚇"
                "TRAM" -> modeIcon.text = "🚋"
                else -> modeIcon.text = "🚌"
            }
            
            instruction.text = step.instruction
            duration.text = step.duration
            
            if (step.line != null) {
                line.text = step.line
                line.visibility = View.VISIBLE
            } else {
                line.visibility = View.GONE
            }
            
            instructionsContainer.addView(stepView)
        }
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideDirectionsButton()
        _binding = null
    }
}