package com.example.athensplus.core.utils

import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.example.athensplus.domain.model.DirectionsResult
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.example.athensplus.domain.model.TransitStep
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class DirectionsService(private val context: Context) {
    private val geocoder = Geocoder(context, Locale.getDefault())
    private val stationManager = StationManager()

    suspend fun findDirectionsToDestination(
        destinationQuery: String,
        currentLocation: LatLng?,
        selectedMode: String
    ): DirectionsResult? = withContext(Dispatchers.IO) {
        try {
            val destinationLocation = geocodeLocation(destinationQuery) ?: return@withContext null
            val startLocation = currentLocation ?: getDefaultStartLocation()
            
            return@withContext when (selectedMode) {
                "Metro" -> findMetroRoute(startLocation, destinationLocation)
                "Tram" -> findTramRoute(startLocation, destinationLocation)
                "Bus Stops" -> findBusRoute(startLocation, destinationLocation)
                else -> findMetroRoute(startLocation, destinationLocation)
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun geocodeLocation(query: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                LatLng(address.latitude, address.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultStartLocation(): LatLng {
        return LatLng(37.9838, 23.7275) // Athens center
    }

    private suspend fun findMetroRoute(start: LatLng, end: LatLng): DirectionsResult {
        val nearestStartStation = findNearestMetroStation(start)
        val nearestEndStation = findNearestMetroStation(end)
        
        val routePoints = calculateMetroRoute(nearestStartStation, nearestEndStation)
        val steps = createMetroSteps(nearestStartStation, nearestEndStation, start, end)
        
        return DirectionsResult(
            startLocation = start,
            endLocation = end,
            routePoints = routePoints,
            steps = steps,
            totalDuration = calculateTotalDuration(steps),
            totalDistance = calculateTotalDistance(start, end, steps)
        )
    }

    private suspend fun findTramRoute(start: LatLng, end: LatLng): DirectionsResult {
        val nearestStartStation = findNearestTramStation(start)
        val nearestEndStation = findNearestTramStation(end)
        
        val routePoints = calculateTramRoute(nearestStartStation, nearestEndStation)
        val steps = createTramSteps(nearestStartStation, nearestEndStation, start, end)
        
        return DirectionsResult(
            startLocation = start,
            endLocation = end,
            routePoints = routePoints,
            steps = steps,
            totalDuration = calculateTotalDuration(steps),
            totalDistance = calculateTotalDistance(start, end, steps)
        )
    }

    private suspend fun findBusRoute(start: LatLng, end: LatLng): DirectionsResult {
        val nearestStartStop = findNearestBusStop(start)
        val nearestEndStop = findNearestBusStop(end)
        
        val routePoints = calculateBusRoute(nearestStartStop, nearestEndStop)
        val steps = createBusSteps(nearestStartStop, nearestEndStop, start, end)
        
        return DirectionsResult(
            startLocation = start,
            endLocation = end,
            routePoints = routePoints,
            steps = steps,
            totalDuration = calculateTotalDuration(steps),
            totalDistance = calculateTotalDistance(start, end, steps)
        )
    }

    private fun findNearestMetroStation(location: LatLng): MetroStation {
        val allStations = StationData.metroLine1 + StationData.metroLine2 + StationData.metroLine3
        return allStations.minByOrNull { station ->
            stationManager.getDistance(location, station.coords)
        } ?: StationData.metroLine1.first()
    }

    private fun findNearestTramStation(location: LatLng): MetroStation {
        val tramStations = StationData.tramStations
        return tramStations.minByOrNull { station ->
            stationManager.getDistance(location, station.coords)
        } ?: tramStations.first()
    }

    private fun findNearestBusStop(location: LatLng): MetroStation {
        val busStops = StationData.busStops
        val nearestBusStop = busStops.minByOrNull { stop ->
            stationManager.getDistance(location, stop.coords)
        } ?: busStops.first()
        
        return MetroStation(
            nameGreek = nearestBusStop.name,
            nameEnglish = nearestBusStop.name,
            coords = nearestBusStop.coords
        )
    }

    private fun calculateMetroRoute(startStation: MetroStation, endStation: MetroStation): List<LatLng> {
        val interchangeStation = stationManager.findInterchangeStation(startStation, endStation)
        
        val routePoints = mutableListOf<LatLng>()
        
        if (interchangeStation != null) {
            routePoints.addAll(getRoutePointsBetweenStations(startStation, interchangeStation))
            routePoints.addAll(getRoutePointsBetweenStations(interchangeStation, endStation))
        } else {
            routePoints.addAll(getRoutePointsBetweenStations(startStation, endStation))
        }
        
        return routePoints
    }

    private fun calculateTramRoute(startStation: MetroStation, endStation: MetroStation): List<LatLng> {
        return getRoutePointsBetweenStations(startStation, endStation)
    }

    private fun calculateBusRoute(startStop: MetroStation, endStop: MetroStation): List<LatLng> {
        return getRoutePointsBetweenStations(startStop, endStop)
    }

    private fun getRoutePointsBetweenStations(start: MetroStation, end: MetroStation): List<LatLng> {
        val allStations = StationData.metroLine1 + StationData.metroLine2 + StationData.metroLine3
        val startIndex = allStations.indexOf(start)
        val endIndex = allStations.indexOf(end)
        
        return if (startIndex != -1 && endIndex != -1) {
            if (startIndex <= endIndex) {
                allStations.subList(startIndex, endIndex + 1).map { it.coords }
            } else {
                allStations.subList(endIndex, startIndex + 1).map { it.coords }.reversed()
            }
        } else {
            listOf(start.coords, end.coords)
        }
    }

    private fun createMetroSteps(
        startStation: MetroStation,
        endStation: MetroStation,
        startLocation: LatLng,
        endLocation: LatLng
    ): List<TransitStep> {
        val steps = mutableListOf<TransitStep>()
        
        val walkingToStart = stationManager.getDistance(startLocation, startStation.coords)
        if (walkingToStart > 0.1) {
            steps.add(TransitStep(
                mode = "Walking",
                instruction = "Walk to ${startStation.nameEnglish} station",
                duration = "${(walkingToStart * 12).toInt()} min",
                walkingDistance = "${(walkingToStart * 1000).toInt()}m"
            ))
        }
        
        val interchangeStation = stationManager.findInterchangeStation(startStation, endStation)
        if (interchangeStation != null) {
            steps.add(TransitStep(
                mode = "Metro",
                instruction = "Take metro from ${startStation.nameEnglish} to ${interchangeStation.nameEnglish}",
                duration = "8 min",
                line = "Line 1",
                departureStop = startStation.nameEnglish,
                arrivalStop = interchangeStation.nameEnglish
            ))
            
            steps.add(TransitStep(
                mode = "Metro",
                instruction = "Take metro from ${interchangeStation.nameEnglish} to ${endStation.nameEnglish}",
                duration = "6 min",
                line = "Line 2",
                departureStop = interchangeStation.nameEnglish,
                arrivalStop = endStation.nameEnglish
            ))
        } else {
            steps.add(TransitStep(
                mode = "Metro",
                instruction = "Take metro from ${startStation.nameEnglish} to ${endStation.nameEnglish}",
                duration = "12 min",
                line = "Line 1",
                departureStop = startStation.nameEnglish,
                arrivalStop = endStation.nameEnglish
            ))
        }
        
        val walkingToEnd = stationManager.getDistance(endStation.coords, endLocation)
        if (walkingToEnd > 0.1) {
            steps.add(TransitStep(
                mode = "Walking",
                instruction = "Walk to destination",
                duration = "${(walkingToEnd * 12).toInt()} min",
                walkingDistance = "${(walkingToEnd * 1000).toInt()}m"
            ))
        }
        
        return steps
    }

    private fun createTramSteps(
        startStation: MetroStation,
        endStation: MetroStation,
        startLocation: LatLng,
        endLocation: LatLng
    ): List<TransitStep> {
        val steps = mutableListOf<TransitStep>()
        
        val walkingToStart = stationManager.getDistance(startLocation, startStation.coords)
        if (walkingToStart > 0.1) {
            steps.add(TransitStep(
                mode = "Walking",
                instruction = "Walk to ${startStation.nameEnglish} tram stop",
                duration = "${(walkingToStart * 12).toInt()} min",
                walkingDistance = "${(walkingToStart * 1000).toInt()}m"
            ))
        }
        
        steps.add(TransitStep(
            mode = "Tram",
            instruction = "Take tram from ${startStation.nameEnglish} to ${endStation.nameEnglish}",
            duration = "15 min",
            line = "Tram Line",
            departureStop = startStation.nameEnglish,
            arrivalStop = endStation.nameEnglish
        ))
        
        val walkingToEnd = stationManager.getDistance(endStation.coords, endLocation)
        if (walkingToEnd > 0.1) {
            steps.add(TransitStep(
                mode = "Walking",
                instruction = "Walk to destination",
                duration = "${(walkingToEnd * 12).toInt()} min",
                walkingDistance = "${(walkingToEnd * 1000).toInt()}m"
            ))
        }
        
        return steps
    }

    private fun createBusSteps(
        startStop: MetroStation,
        endStop: MetroStation,
        startLocation: LatLng,
        endLocation: LatLng
    ): List<TransitStep> {
        val steps = mutableListOf<TransitStep>()
        
        val walkingToStart = stationManager.getDistance(startLocation, startStop.coords)
        if (walkingToStart > 0.1) {
            steps.add(TransitStep(
                mode = "Walking",
                instruction = "Walk to ${startStop.nameEnglish} bus stop",
                duration = "${(walkingToStart * 12).toInt()} min",
                walkingDistance = "${(walkingToStart * 1000).toInt()}m"
            ))
        }
        
        steps.add(TransitStep(
            mode = "Bus",
            instruction = "Take bus from ${startStop.nameEnglish} to ${endStop.nameEnglish}",
            duration = "20 min",
            line = "Bus Line",
            departureStop = startStop.nameEnglish,
            arrivalStop = endStop.nameEnglish
        ))
        
        val walkingToEnd = stationManager.getDistance(endStop.coords, endLocation)
        if (walkingToEnd > 0.1) {
            steps.add(TransitStep(
                mode = "Walking",
                instruction = "Walk to destination",
                duration = "${(walkingToEnd * 12).toInt()} min",
                walkingDistance = "${(walkingToEnd * 1000).toInt()}m"
            ))
        }
        
        return steps
    }

    private fun calculateTotalDuration(steps: List<TransitStep>): String {
        val totalMinutes = steps.sumOf { step ->
            step.duration.replace(" min", "").toIntOrNull() ?: 0
        }
        return "${totalMinutes} min"
    }

    private fun calculateTotalDistance(start: LatLng, end: LatLng, steps: List<TransitStep>): String {
        val totalDistance = stationManager.getDistance(start, end) * 1000
        return "${totalDistance.toInt()}m"
    }
} 