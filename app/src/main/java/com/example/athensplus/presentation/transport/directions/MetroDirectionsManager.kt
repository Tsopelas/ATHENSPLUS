package com.example.athensplus.presentation.transport.directions

import android.graphics.Color
import androidx.fragment.app.Fragment
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.google.android.gms.maps.model.LatLng

class MetroDirectionsManager(
    private val fragment: Fragment
) {
    
    data class MetroStep(
        val instruction: String,
        val duration: String,
        val line: String? = null,
        val lineColor: Int = Color.parseColor("#663399"),
        val iconResource: Int = com.example.athensplus.R.drawable.ic_walking
    )
    
    fun generateMetroDirections(startStation: MetroStation, endStation: MetroStation): List<MetroStep> {
        val steps = mutableListOf<MetroStep>()
        
        val startLine = getStationLine(startStation)
        val endLine = getStationLine(endStation)
        
        val startLineColor = getStationColor(startStation)
        val endLineColor = getStationColor(endStation)
        
        steps.add(MetroStep(
            instruction = "Enter ${startStation.nameEnglish} station",
            duration = "Look for $startLine platform",
            iconResource = com.example.athensplus.R.drawable.ic_metro
        ))
        
        if (startLine == endLine) {
            val direction = getDirection(startStation, endStation, startLine)
            val stationCount = getStationCount(startStation, endStation, startLine)
            
            steps.add(MetroStep(
                instruction = "Take $startLine towards $direction",
                duration = "Travel $stationCount stations, get off at ${endStation.nameEnglish}",
                line = startLine,
                lineColor = startLineColor,
                iconResource = com.example.athensplus.R.drawable.ic_metro
            ))
        } else {
            val interchangeStation = findInterchangeStation(startStation, endStation)
            
            if (interchangeStation != null) {
                val direction1 = getDirection(startStation, interchangeStation, startLine)
                val direction2 = getDirection(interchangeStation, endStation, endLine)
                val stationCount1 = getStationCount(startStation, interchangeStation, startLine)
                val stationCount2 = getStationCount(interchangeStation, endStation, endLine)
                
                steps.add(MetroStep(
                    instruction = "Take $startLine towards $direction1",
                    duration = "Travel $stationCount1 stations, get off at ${interchangeStation.nameEnglish}",
                    line = startLine,
                    lineColor = startLineColor,
                    iconResource = com.example.athensplus.R.drawable.ic_metro
                ))
                
                steps.add(MetroStep(
                    instruction = "At ${interchangeStation.nameEnglish}, change to $endLine towards $direction2",
                    duration = "Travel $stationCount2 stations, get off at ${endStation.nameEnglish}",
                    line = endLine,
                    lineColor = endLineColor,
                    iconResource = com.example.athensplus.R.drawable.ic_metro
                ))
                
                steps.add(MetroStep(
                    instruction = "At ${interchangeStation.nameEnglish}, exit the train",
                    duration = "Follow transfer signs",
                    iconResource = com.example.athensplus.R.drawable.ic_metro
                ))
                
                steps.add(MetroStep(
                    instruction = "Transfer instructions: Follow signs to $endLine platform",
                    duration = "Look for $endLine direction signs",
                    iconResource = com.example.athensplus.R.drawable.ic_metro
                ))
            } else {
                steps.add(MetroStep(
                    instruction = "No direct route available",
                    duration = "Please select different stations",
                    iconResource = com.example.athensplus.R.drawable.ic_metro
                ))
            }
        }
        
        steps.add(MetroStep(
            instruction = "Exit at ${endStation.nameEnglish}",
            duration = "Follow exit signs",
            iconResource = com.example.athensplus.R.drawable.ic_metro
        ))
        
        steps.add(MetroStep(
            instruction = "Arrive at ${endStation.nameEnglish}",
            duration = "You have reached your destination",
            iconResource = com.example.athensplus.R.drawable.ic_metro
        ))
        
        return steps
    }
    
    private fun getStationLine(station: MetroStation): String {
        return when {
            StationData.metroLine1.contains(station) -> "Line 1"
            StationData.metroLine2.contains(station) -> "Line 2"
            StationData.metroLine3.contains(station) -> "Line 3"
            else -> "Unknown"
        }
    }
    
    private fun getStationColor(station: MetroStation): Int {
        return when {
            StationData.metroLine1.contains(station) -> Color.parseColor("#009640")
            StationData.metroLine2.contains(station) -> Color.parseColor("#e30613")
            StationData.metroLine3.contains(station) -> Color.parseColor("#0057a8")
            else -> Color.parseColor("#663399")
        }
    }
    
    private fun getDirection(startStation: MetroStation, endStation: MetroStation, line: String): String {
        val lineStations = when (line) {
            "Line 1" -> StationData.metroLine1
            "Line 2" -> StationData.metroLine2
            "Line 3" -> StationData.metroLine3
            else -> emptyList()
        }
        
        val startIndex = lineStations.indexOf(startStation)
        val endIndex = lineStations.indexOf(endStation)
        
        return if (startIndex < endIndex) {
            lineStations.last().nameEnglish
        } else {
            lineStations.first().nameEnglish
        }
    }
    
    private fun getStationCount(startStation: MetroStation, endStation: MetroStation, line: String): Int {
        val lineStations = when (line) {
            "Line 1" -> StationData.metroLine1
            "Line 2" -> StationData.metroLine2
            "Line 3" -> StationData.metroLine3
            else -> emptyList()
        }
        
        val startIndex = lineStations.indexOf(startStation)
        val endIndex = lineStations.indexOf(endStation)
        
        return kotlin.math.abs(endIndex - startIndex)
    }
    
    private fun findInterchangeStation(startStation: MetroStation, endStation: MetroStation): MetroStation? {
        val startLine = getStationLine(startStation)
        val endLine = getStationLine(endStation)
        
        if (startLine == endLine) return null
        
        val interchangeStations = when {
            (startLine == "Line 1" && endLine == "Line 2") || (startLine == "Line 2" && endLine == "Line 1") -> {
                StationData.metroLine1.filter { it.isInterchange && StationData.metroLine2.contains(it) }
            }
            (startLine == "Line 1" && endLine == "Line 3") || (startLine == "Line 3" && endLine == "Line 1") -> {
                StationData.metroLine1.filter { it.isInterchange && StationData.metroLine3.contains(it) }
            }
            (startLine == "Line 2" && endLine == "Line 3") || (startLine == "Line 3" && endLine == "Line 2") -> {
                StationData.metroLine2.filter { it.isInterchange && StationData.metroLine3.contains(it) }
            }
            else -> emptyList()
        }
        
        return interchangeStations.firstOrNull()
    }
} 