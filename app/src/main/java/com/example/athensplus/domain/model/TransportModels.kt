package com.example.athensplus.domain.model

import com.google.android.gms.maps.model.LatLng

data class TransitStep(
    val mode: String, 
    val instruction: String,
    val duration: String,
    val line: String? = null,
    val departureStop: String? = null,
    val arrivalStop: String? = null,
    val nextArrival: String? = null,
    val walkingDistance: String? = null,
    val vehicleType: String? = null
)

data class MetroStation(
    val nameGreek: String,
    val nameEnglish: String,
    val coords: LatLng,
    val isInterchange: Boolean = false
)

data class TramStation(
    val name: String,
    val coords: LatLng
)

data class BusStop(
    val name: String,
    val coords: LatLng
)

data class TimetableTable(
    val direction: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

data class BusInfo(
    val number: String,
    val destination: String,
    val arrivalTime: String,
    val status: String 
)

data class DirectionsResult(
    val startLocation: LatLng,
    val endLocation: LatLng,
    val routePoints: List<LatLng>,
    val steps: List<TransitStep>,
    val totalDuration: String,
    val totalDistance: String
) 