package com.example.athensplus.core.utils

import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import kotlin.math.pow

class StationManager {

    fun findInterchangeStation(start: MetroStation, end: MetroStation): MetroStation? {
        if (StationData.metroLine1.contains(start) && StationData.metroLine1.contains(end) ||
            StationData.metroLine2.contains(start) && StationData.metroLine2.contains(end) ||
            StationData.metroLine3.contains(start) && StationData.metroLine3.contains(end)) {
            return null
        }

        val interchangeStations = listOf(StationData.metroLine1, StationData.metroLine2, StationData.metroLine3)
            .flatten()
            .filter { it.isInterchange }

        val validInterchanges = interchangeStations.filter { interchange ->
            val isOnStartLine = StationData.metroLine1.contains(start) && StationData.metroLine1.contains(interchange) ||
                               StationData.metroLine2.contains(start) && StationData.metroLine2.contains(interchange) ||
                               StationData.metroLine3.contains(start) && StationData.metroLine3.contains(interchange)
            val isOnEndLine = StationData.metroLine1.contains(end) && StationData.metroLine1.contains(interchange) ||
                             StationData.metroLine2.contains(end) && StationData.metroLine2.contains(interchange) ||
                             StationData.metroLine3.contains(end) && StationData.metroLine3.contains(interchange)
            isOnStartLine && isOnEndLine
        }

        if (validInterchanges.isEmpty()) return null

        return validInterchanges.minByOrNull { interchange ->
            val startLine = when {
                StationData.metroLine1.contains(start) -> StationData.metroLine1
                StationData.metroLine2.contains(start) -> StationData.metroLine2
                StationData.metroLine3.contains(start) -> StationData.metroLine3
                else -> return@minByOrNull Int.MAX_VALUE
            }
            
            val endLine = when {
                StationData.metroLine1.contains(end) -> StationData.metroLine1
                StationData.metroLine2.contains(end) -> StationData.metroLine2
                StationData.metroLine3.contains(end) -> StationData.metroLine3
                else -> return@minByOrNull Int.MAX_VALUE
            }

            val startIndex = startLine.indexOf(start)
            val endIndex = endLine.indexOf(end)
            val interchangeStartIndex = startLine.indexOf(interchange)
            val interchangeEndIndex = endLine.indexOf(interchange)

            val distanceToInterchange = kotlin.math.abs(startIndex - interchangeStartIndex)
            val distanceFromInterchange = kotlin.math.abs(endIndex - interchangeEndIndex)

            distanceToInterchange + distanceFromInterchange
        }
    }

    fun isStationOnRoute(station: MetroStation, start: MetroStation, end: MetroStation): Boolean {
        val line = when {
            StationData.metroLine1.contains(start) && StationData.metroLine1.contains(end) -> StationData.metroLine1
            StationData.metroLine2.contains(start) && StationData.metroLine2.contains(end) -> StationData.metroLine2
            StationData.metroLine3.contains(start) && StationData.metroLine3.contains(end) -> StationData.metroLine3
            else -> return false
        }

        val startIndex = line.indexOf(start)
        val endIndex = line.indexOf(end)
        val stationIndex = line.indexOf(station)

        return if (startIndex <= endIndex) {
            stationIndex in startIndex..endIndex
        } else {
            stationIndex in endIndex..startIndex
        }
    }

    fun findNearestInterchangeToLine3(station: MetroStation): MetroStation? {
        val line3Interchanges = StationData.metroLine3.filter { it.isInterchange }

        val stationLine = when {
            StationData.metroLine1.contains(station) -> StationData.metroLine1
            StationData.metroLine2.contains(station) -> StationData.metroLine2
            else -> null
        }

        return stationLine?.let { line ->
            line3Interchanges.firstOrNull { interchange ->
                line.contains(interchange)
            }
        }
    }

    fun getStationColor(station: MetroStation): Int {
        return when {
            StationData.metroLine1.contains(station) -> android.graphics.Color.parseColor("#009640")
            StationData.metroLine2.contains(station) -> android.graphics.Color.parseColor("#e30613")
            StationData.metroLine3.contains(station) -> android.graphics.Color.parseColor("#0057a8")
            else -> android.graphics.Color.parseColor("#663399")
        }
    }

    fun getDistance(point1: com.google.android.gms.maps.model.LatLng, point2: com.google.android.gms.maps.model.LatLng): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = kotlin.math.sin(dlat / 2).pow(2) + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * kotlin.math.sin(dlon / 2).pow(2)
        val c = 2 * kotlin.math.asin(kotlin.math.sqrt(a))

        val r = 6371
        return c * r
    }
} 