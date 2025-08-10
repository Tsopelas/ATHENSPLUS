

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
    val waitTime: String? = null,
    val waitTimeMinutes: Int = 0,
    val alternativeLines: List<String> = emptyList(),
    val frequency: String? = null,
    val reliability: String? = null,
    val crowdLevel: String? = null,
    val price: String? = null,
    val accessibility: Boolean = false,
    val realTimeUpdates: Boolean = false,
    val numStops: Int = 0
)

data class MetroStation(
    val nameGreek: String,
    val nameEnglish: String,
    val coords: LatLng,
    val isInterchange: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetroStation

        if (nameGreek != other.nameGreek) return false
        if (nameEnglish != other.nameEnglish) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nameGreek.hashCode()
        result = 31 * result + nameEnglish.hashCode()
        return result
    }
}



data class TimetableTable(
    val direction: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

@Suppress("unused")
data class DirectionsResult(
    val startLocation: LatLng,
    val endLocation: LatLng,
    val routePoints: List<LatLng>,
    val steps: List<TransitStep>,
    val totalDuration: String,
    val totalDistance: String
)
