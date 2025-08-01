@file:Suppress("SpellCheckingInspection","unused", "RedundantSuppression")

package com.example.athensplus.presentation.transport

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.athensplus.BuildConfig
import com.example.athensplus.core.utils.AddressAutocompleteService
import com.example.athensplus.core.utils.BusTimesImprovementService
import com.example.athensplus.core.utils.LocationService
import com.example.athensplus.core.utils.MapManager
import com.example.athensplus.core.utils.RouteSelectionMode
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.core.utils.TimetableService
import com.example.athensplus.databinding.FragmentTransportBinding
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.example.athensplus.domain.model.TransitStep
import com.example.athensplus.presentation.common.AddressAutocompleteManager
import com.example.athensplus.presentation.transport.ui.ControlSetupManager
import com.example.athensplus.presentation.transport.dialogs.DirectionsDialogManager
import com.example.athensplus.presentation.transport.directions.DirectionsManager
import com.example.athensplus.presentation.common.DirectionsUI
import com.example.athensplus.presentation.transport.map.MapDrawingManager
import com.example.athensplus.presentation.transport.map.MapSetupManager
import com.example.athensplus.presentation.transport.directions.MetroDirectionsManager
import com.example.athensplus.presentation.transport.routes.RouteDisplayManager
import com.example.athensplus.presentation.transport.routes.RouteFindingManager
import com.example.athensplus.presentation.transport.dialogs.StationDirectionsDialogManager
import com.example.athensplus.presentation.transport.map.StationSelectionManager
import com.example.athensplus.presentation.transport.timetables.TimetableManager
import com.example.athensplus.presentation.transport.dialogs.TransportDialogManager
import com.example.athensplus.presentation.transport.ui.TransportStateManager
import com.example.athensplus.presentation.transport.ui.TransportUI
import com.example.athensplus.presentation.transport.timetables.WaitTimeManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.util.Locale

class TransportFragment : Fragment() {
    private var _binding: FragmentTransportBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationService: LocationService
    private lateinit var timetableService: TimetableService
    private lateinit var mapManager: MapManager
    private lateinit var stationManager: StationManager
    private lateinit var transportUI: TransportUI
    private lateinit var directionsUI: DirectionsUI
    private lateinit var addressAutocompleteService: AddressAutocompleteService
    private lateinit var waitTimeManager: WaitTimeManager
    private lateinit var routeDisplayManager: RouteDisplayManager
    private lateinit var transportStateManager: TransportStateManager
    private lateinit var addressAutocompleteManager: AddressAutocompleteManager

    private lateinit var mapDrawingManager: MapDrawingManager
    private lateinit var stationSelectionManager: StationSelectionManager
    private lateinit var directionsManager: DirectionsManager
    private lateinit var dialogManager: TransportDialogManager
    private lateinit var mapSetupManager: MapSetupManager
    private lateinit var controlSetupManager: ControlSetupManager
    private lateinit var routeFindingManager: RouteFindingManager
    private lateinit var directionsDialogManager: DirectionsDialogManager
    private lateinit var metroDirectionsManager: MetroDirectionsManager
    private lateinit var timetableManager: TimetableManager
    private lateinit var stationDirectionsDialogManager: StationDirectionsDialogManager

    private var googleMap: GoogleMap? = null
    private lateinit var geocoder: Geocoder
    private var selectedMode: String = "Metro"
    private var selectedLine: String = "All Lines"
    private var selectedStartStation: MetroStation? = null
    private var selectedEndStation: MetroStation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransportBinding.inflate(inflater, container, false)

        try {
            initializeServices()
            initializeManagers()
            setupMap()
            setupControls()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return binding.root
    }
    
    private fun initializeServices() {
            locationService = LocationService(this)
            timetableService = TimetableService(requireContext())
            mapManager = MapManager(requireContext())
            stationManager = StationManager()
            transportUI = TransportUI(this, stationManager)
            directionsUI = DirectionsUI(requireContext())
            
            addressAutocompleteService = AddressAutocompleteService(BuildConfig.GOOGLE_MAPS_API_KEY)
            waitTimeManager = WaitTimeManager(this)
            routeDisplayManager = RouteDisplayManager(this)
            transportStateManager = TransportStateManager(this, binding, stationManager)
            addressAutocompleteManager = AddressAutocompleteManager(this, locationService, addressAutocompleteService)
    }
    
    private fun initializeManagers() {
        mapDrawingManager = MapDrawingManager(stationManager)
        stationSelectionManager = StationSelectionManager(this, mapManager, stationManager)
        dialogManager = TransportDialogManager(this, locationService, stationManager, timetableService, addressAutocompleteManager)
        directionsManager = DirectionsManager(this, locationService, stationManager, timetableService, routeDisplayManager, addressAutocompleteManager, dialogManager)
        mapSetupManager = MapSetupManager(this, locationService)
        controlSetupManager = ControlSetupManager(this, binding, transportUI)
        routeFindingManager = RouteFindingManager(this, binding)
        directionsDialogManager = DirectionsDialogManager(this, locationService)
        metroDirectionsManager = MetroDirectionsManager(this)
        timetableManager = TimetableManager(this, timetableService)
        stationDirectionsDialogManager = StationDirectionsDialogManager(this, stationManager, metroDirectionsManager, addressAutocompleteManager)
    }
    
    private fun setupMap() {
        binding.mapView.onCreate(null)
            locationService.initialize()
            geocoder = Geocoder(requireContext(), Locale.getDefault())
    }

    private fun setupControls() {
        controlSetupManager.setupControls(
            selectedMode = selectedMode,
            selectedLine = selectedLine,
            onModeChanged = { mode ->
                selectedMode = mode
                controlSetupManager.updateModeUI(mode)
                updateMap()
            },
            onLineChanged = { line ->
                selectedLine = line
                controlSetupManager.updateLineUI(line)
                updateMap()
            },
            onDirectionsClicked = {
                findRoute()
            },
            onResetMapClicked = {
                resetMap()
            },
            onAirportTimetableClicked = { _ ->
                selectedStartStation?.let { dialogManager.showAirportTimetable(it) }
            },
            onHarborGateMapClicked = {
                dialogManager.showPiraeusGateMap()
            },
            onChooseOnMapClicked = {
                dialogManager.showMapSelectionDialogForEditText(binding.editTo)
            }
        )
        setupSelectionControls()
        setupAutocomplete()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapSetupManager.setupMap { googleMap ->
            this.googleMap = googleMap
            setupMapListeners(googleMap)
            updateMap()
            updateMarkers(googleMap.cameraPosition.zoom)
            googleMap.setOnCameraIdleListener {
                val zoom = googleMap.cameraPosition.zoom
                updateMarkers(zoom)
            }
        }
        binding.mapView.getMapAsync(mapSetupManager)
        updateSelectionIndicator()
    }

    private fun updateMap() {
        mapDrawingManager.updateMap(
            googleMap,
            selectedMode,
            selectedLine,
            selectedStartStation,
            selectedEndStation
        )
    }
    
    private fun setupMapListeners(googleMap: GoogleMap) {
        googleMap.setOnMarkerClickListener { marker ->
            showStationMenu(marker)
            true
        }

        googleMap.setOnMapClickListener { _ ->
            // todo
        }
    }

    private fun getStationLine(station: MetroStation): String {
        return when {
            StationData.metroLine1.contains(station) -> "Line 1"
            StationData.metroLine2.contains(station) -> "Line 2"
            StationData.metroLine3.contains(station) -> "Line 3"
            else -> "Unknown"
        }
    }
    
    private fun isStationOnRouteWithInterchange(
        station: MetroStation,
        startStation: MetroStation,
        endStation: MetroStation,
        interchangeStation: MetroStation
    ): Boolean {
        val isOnFirstSegment = stationManager.isStationOnRoute(station, startStation, interchangeStation)

        val isOnSecondSegment = stationManager.isStationOnRoute(station, interchangeStation, endStation)

        if (station == startStation || station == endStation || station == interchangeStation) {
            return true
        }
        
        return isOnFirstSegment || isOnSecondSegment
    }

    private fun updateMarkers(zoom: Float) {
        googleMap?.let { map ->
            stationSelectionManager.updateMarkers(
                map,
                zoom,
                selectedStartStation,
                selectedEndStation,
                selectedLine
            )
        }
    }

    private fun findRoute() {
        val toText = binding.editTo.text.toString().trim()
        directionsManager.findRoute(toText)
    }

    private fun showDirectionsDialog(destination: String) {
        directionsDialogManager.showDirectionsDialog(
            destination = destination,
            onClose = { },
            onUpdateDirections = { },
            onChooseOnMap = { },
            onFetchDirections = { _, _ ->
                // todo
            }
        )
    }

    private fun showStationDirectionsDialog() {
        stationDirectionsDialogManager.showStationDirectionsDialog(
            selectedStartStation = selectedStartStation,
            selectedEndStation = selectedEndStation,
            onClose = { },
            onUpdateDirections = { 
            }
        )
    }

    private fun setupDialogAutocomplete(editFromLocation: EditText, editToLocation: EditText) {
        addressAutocompleteManager.setupDialogAutocomplete(editFromLocation, editToLocation)
    }

    private fun showStationMenu(marker: Marker) {
        googleMap?.let { map ->
            stationSelectionManager.showStationMenu(
                marker,
                map,
                selectedStartStation,
                selectedEndStation,
            onStationSelected = { station ->
                if (selectedStartStation == null) {
                    selectedStartStation = station
                } else if (selectedEndStation == null && station != selectedStartStation) {
                    selectedEndStation = station
                }
                updateMap()
                updateMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                updateSelectionIndicator()
            },
            onStationDeselected = { station ->
                if (station == selectedStartStation) {
                    selectedStartStation = null
                    selectedEndStation = null
                } else {
                    selectedEndStation = null
                }
                updateMap()
                updateMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                updateSelectionIndicator()
            },
            onShowTimetable = { station ->
                android.util.Log.d("TransportFragment", "Timetable callback called for station: ${station.nameEnglish}")
                dialogManager.showStationTimetable(station)
            },
            onShowAirportRoute = { station ->
                selectedStartStation = station
                selectedEndStation = StationData.metroLine3.find { it.nameEnglish == "Airport" }
                updateMap()
                updateMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                updateSelectionIndicator()
            },
            onShowHarborRoute = { station ->
                selectedStartStation = station
                selectedEndStation = StationData.metroLine1.find { it.nameEnglish == "Piraeus" }
                updateMap()
                updateMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                updateSelectionIndicator()
            }
        )
        }
    }

    private fun showStationTimetable(station: MetroStation) {
        timetableManager.showStationTimetable(station)
    }

    private fun showAirportTimetable(station: MetroStation) {
        timetableManager.showAirportTimetable(station)
    }

    private fun setupSelectionControls() {
        binding.swapStationsButton.setOnClickListener {
            if (selectedStartStation != null && selectedEndStation != null) {
                val temp = selectedStartStation
                selectedStartStation = selectedEndStation
                selectedEndStation = temp
                updateMap()
                updateMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                updateSelectionIndicator()
            }
        }

        binding.enterButton.setOnClickListener {
            if (selectedStartStation != null && selectedEndStation != null) {
                showStationDirectionsDialog()
            }
        }
    }

    private fun resetMap() {
        selectedStartStation = null
        selectedEndStation = null

        stationSelectionManager.clearMarkers()
        googleMap?.clear()
        updateMap()
        updateMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
        updateSelectionIndicator()

        binding.airportTimetableButton.visibility = View.GONE
        binding.harborGateMapButton.visibility = View.GONE
    }

    private fun updateSelectionIndicator() {
        transportStateManager.updateSelectionIndicator(selectedStartStation, selectedEndStation)
    }

    private fun showWaitTimeInfo(step: TransitStep, waitTimeText: TextView, nextDepartureText: TextView) {
        waitTimeManager.showWaitTimeInfo(step, waitTimeText, nextDepartureText)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        dialogManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        dialogManager.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        dialogManager.onDestroy()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    @Deprecated("Deprecated in Java")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationService.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (locationService.isLocationPermissionGranted()) {
            locationService.setupLocationUI(googleMap)
        }
    }

    private fun setupAutocomplete() {
        addressAutocompleteManager.setupAutocomplete(binding.editTo)
    }

    private fun displaySelectedRoutes(
        stepsContainer: LinearLayout,
        routes: List<BusTimesImprovementService.ImprovedRouteAlternative>,
        mode: RouteSelectionMode,
        context: Context,
        summaryContainer: LinearLayout,
        summaryText: TextView
    ) {
        routeDisplayManager.displaySelectedRoutes(stepsContainer, routes, mode, context, summaryContainer, summaryText)
    }

    private fun getLineColorFromInstruction(instruction: String): Int {
        return routeDisplayManager.getLineColorFromInstruction(instruction)
    }
}
