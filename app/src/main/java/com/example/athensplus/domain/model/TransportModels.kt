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
    val vehicleType: String? = null,
    val totalRouteDuration: String? = null,
    val totalRouteDistance: String? = null,
    val departureTime: String? = null,
    val departureTimeValue: Long = 0L,
    // Enhanced bus times fields
    val waitTime: String? = null,
    val waitTimeMinutes: Int = 0,
    val alternativeLines: List<String> = emptyList(),
    val frequency: String? = null,
    val reliability: String? = null,
    val crowdLevel: String? = null,
    val price: String? = null,
    val accessibility: Boolean = false,
    val realTimeUpdates: Boolean = false
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

// New data classes for enhanced bus times
data class BusRouteAlternative(
    val routeId: String,
    val steps: List<TransitStep>,
    val totalDuration: String,
    val totalDistance: String,
    val departureTime: Long,
    val arrivalTime: Long,
    val waitTime: Long,
    val busLines: List<String>,
    val reliability: String,
    val price: String?,
    val accessibility: Boolean,
    val realTimeUpdates: Boolean
)

data class BusDepartureInfo(
    val line: String,
    val destination: String,
    val departureTime: Long,
    val waitMinutes: Int,
    val stopName: String,
    val vehicleType: String?,
    val frequency: String?,
    val reliability: String?,
    val crowdLevel: String?,
    val price: String?,
    val accessibility: Boolean,
    val realTimeUpdates: Boolean
) 