package com.example.athensplus.presentation.transport.map

import android.util.Log
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class MapDrawingManager(
    private val stationManager: StationManager
) {
    
    fun updateMap(
        googleMap: GoogleMap?,
        selectedMode: String,
        selectedLine: String,
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?
    ) {
        googleMap?.clear()
        Log.d("LineDrawing", "=== Starting updateMap ===")
        Log.d("LineDrawing", "Selected mode: $selectedMode, Selected line: $selectedLine")
        Log.d("LineDrawing", "Start station: ${selectedStartStation?.nameEnglish ?: "None"}")
        Log.d("LineDrawing", "End station: ${selectedEndStation?.nameEnglish ?: "None"}")
        
        if (selectedMode == "Metro") {
            when (selectedLine) {
                "All Lines" -> {
                    if (selectedStartStation != null && selectedEndStation != null) {
                        drawRouteForAllLines(googleMap, selectedStartStation, selectedEndStation)
                    } else {
                        // Draw all lines when no route is selected
                        drawAllMetroLines(googleMap)
                    }
                }
                "Line 1" -> drawRouteForSpecificLine(googleMap, selectedStartStation, selectedEndStation, "Line 1", StationData.line1CurvedPoints, 0xFF009640.toInt())
                "Line 2" -> drawRouteForSpecificLine(googleMap, selectedStartStation, selectedEndStation, "Line 2", StationData.line2CurvedPoints, 0xFFe30613.toInt())
                "Line 3" -> drawRouteForSpecificLine(googleMap, selectedStartStation, selectedEndStation, "Line 3", StationData.line3CurvedPoints, 0xFF0057a8.toInt())
            }
        }
    }
    
    private fun drawRouteForAllLines(
        googleMap: GoogleMap?,
        startStation: MetroStation,
        endStation: MetroStation
    ) {
        val startLine = getStationLine(startStation)
        val endLine = getStationLine(endStation)
        val interchangeStation = stationManager.findInterchangeStation(startStation, endStation)
        
        if (interchangeStation != null) {
            // Route with interchange - draw each segment on its respective line
            val interchangeLine = getStationLine(interchangeStation)
            Log.d("LineDrawing", "Route with interchange: $interchangeStation")
            Log.d("LineDrawing", "Start line: $startLine, Interchange line: $interchangeLine, End line: $endLine")
            
            // Draw first segment: start station to interchange
            when (startLine) {
                "Line 1" -> {
                    Log.d("LineDrawing", "Drawing first segment on Line 1: ${startStation.nameEnglish} to ${interchangeStation.nameEnglish}")
                    drawSegmentBetweenStations(googleMap, startStation, interchangeStation, StationData.line1CurvedPoints, 0xFF009640.toInt())
                }
                "Line 2" -> {
                    Log.d("LineDrawing", "Drawing first segment on Line 2: ${startStation.nameEnglish} to ${interchangeStation.nameEnglish}")
                    drawSegmentBetweenStations(googleMap, startStation, interchangeStation, StationData.line2CurvedPoints, 0xFFe30613.toInt())
                }
                "Line 3" -> {
                    Log.d("LineDrawing", "Drawing first segment on Line 3: ${startStation.nameEnglish} to ${interchangeStation.nameEnglish}")
                    drawSegmentBetweenStations(googleMap, startStation, interchangeStation, StationData.line3CurvedPoints, 0xFF0057a8.toInt())
                }
            }
            
            // Draw second segment: interchange to end station
            when (endLine) {
                "Line 1" -> {
                    Log.d("LineDrawing", "Drawing second segment on Line 1: ${interchangeStation.nameEnglish} to ${endStation.nameEnglish}")
                    drawSegmentBetweenStations(googleMap, interchangeStation, endStation, StationData.line1CurvedPoints, 0xFF009640.toInt())
                }
                "Line 2" -> {
                    Log.d("LineDrawing", "Drawing second segment on Line 2: ${interchangeStation.nameEnglish} to ${endStation.nameEnglish}")
                    drawSegmentBetweenStations(googleMap, interchangeStation, endStation, StationData.line2CurvedPoints, 0xFFe30613.toInt())
                }
                "Line 3" -> {
                    Log.d("LineDrawing", "Drawing second segment on Line 3: ${interchangeStation.nameEnglish} to ${endStation.nameEnglish}")
                    drawSegmentBetweenStations(googleMap, interchangeStation, endStation, StationData.line3CurvedPoints, 0xFF0057a8.toInt())
                }
            }
        } else {
            // Direct route - draw single segment
            Log.d("LineDrawing", "Direct route on $startLine: ${startStation.nameEnglish} to ${endStation.nameEnglish}")
            when (startLine) {
                "Line 1" -> drawSegmentBetweenStations(googleMap, startStation, endStation, StationData.line1CurvedPoints, 0xFF009640.toInt())
                "Line 2" -> drawSegmentBetweenStations(googleMap, startStation, endStation, StationData.line2CurvedPoints, 0xFFe30613.toInt())
                "Line 3" -> drawSegmentBetweenStations(googleMap, startStation, endStation, StationData.line3CurvedPoints, 0xFF0057a8.toInt())
            }
        }
    }
    
    private fun drawRouteForSpecificLine(
        googleMap: GoogleMap?,
        startStation: MetroStation?,
        endStation: MetroStation?,
        lineName: String,
        curvedPoints: List<LatLng>,
        color: Int
    ) {
        if (startStation != null && endStation != null) {
            val interchangeStation = stationManager.findInterchangeStation(startStation, endStation)
            if (interchangeStation != null) {
                // Route with interchange
                if (getStationLine(startStation) == lineName) {
                    drawSegmentBetweenStations(googleMap, startStation, interchangeStation, curvedPoints, color)
                }
                if (getStationLine(endStation) == lineName) {
                    drawSegmentBetweenStations(googleMap, interchangeStation, endStation, curvedPoints, color)
                }
            } else {
                // Direct route on specific line
                drawSegmentBetweenStations(googleMap, startStation, endStation, curvedPoints, color)
            }
        } else {
            // Draw full line when no route is selected
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(curvedPoints)
                    .color(color)
                    .width(22f)
                    .geodesic(false)
                    .jointType(JointType.ROUND)
            )
        }
    }
    
    private fun drawAllMetroLines(googleMap: GoogleMap?) {
        // This would need to be implemented based on your MapManager
        // For now, we'll draw each line individually
        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(StationData.line1CurvedPoints)
                .color(0xFF009640.toInt())
                .width(22f)
                .geodesic(false)
                .jointType(JointType.ROUND)
        )
        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(StationData.line2CurvedPoints)
                .color(0xFFe30613.toInt())
                .width(22f)
                .geodesic(false)
                .jointType(JointType.ROUND)
        )
        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(StationData.line3CurvedPoints)
                .color(0xFF0057a8.toInt())
                .width(22f)
                .geodesic(false)
                .jointType(JointType.ROUND)
        )
    }
    
    private fun drawSegmentBetweenStations(
        googleMap: GoogleMap?,
        start: MetroStation,
        end: MetroStation,
        curvedPoints: List<LatLng>,
        color: Int
    ) {
        val startPointIndex = findClosestCurvedPointIndex(start.coords, curvedPoints)
        val endPointIndex = findClosestCurvedPointIndex(end.coords, curvedPoints)

        Log.d("LineDrawing", "Drawing segment from ${start.nameEnglish} to ${end.nameEnglish}")
        Log.d("LineDrawing", "Start point index: $startPointIndex, End point index: $endPointIndex")
        Log.d("LineDrawing", "Total curved points: ${curvedPoints.size}")

        // For Line 3, the curved points are in reverse order compared to the station list
        val segmentPoints = if (curvedPoints == StationData.line3CurvedPoints) {
            if (startPointIndex >= endPointIndex) {
                val points = curvedPoints.subList(endPointIndex, startPointIndex + 1)
                Log.d("LineDrawing", "Line 3 reverse: Using points from $endPointIndex to ${startPointIndex + 1}, got ${points.size} points")
                points
            } else {
                val points = curvedPoints.subList(startPointIndex, endPointIndex + 1)
                Log.d("LineDrawing", "Line 3 normal: Using points from $startPointIndex to ${endPointIndex + 1}, got ${points.size} points")
                points
            }
        } else {
            if (startPointIndex <= endPointIndex) {
                val points = curvedPoints.subList(startPointIndex, endPointIndex + 1)
                Log.d("LineDrawing", "Normal order: Using points from $startPointIndex to ${endPointIndex + 1}, got ${points.size} points")
                points
            } else {
                val points = curvedPoints.subList(endPointIndex, startPointIndex + 1)
                Log.d("LineDrawing", "Normal reverse: Using points from $endPointIndex to ${startPointIndex + 1}, got ${points.size} points")
                points
            }
        }

        if (segmentPoints.size >= 2) {
            Log.d("LineDrawing", "Drawing polyline with ${segmentPoints.size} points")
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(segmentPoints)
                    .color(color)
                    .width(22f)
                    .geodesic(false)
                    .jointType(JointType.ROUND)
            )
        } else {
            Log.d("LineDrawing", "ERROR: Not enough points to draw line (${segmentPoints.size} points)")
        }
    }
    
    private fun findClosestCurvedPointIndex(stationCoords: LatLng, curvedPoints: List<LatLng>): Int {
        var closestIndex = 0
        var minDistance = Double.MAX_VALUE

        curvedPoints.forEachIndexed { index, point ->
            val distance = stationManager.getDistance(stationCoords, point)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }

        return closestIndex
    }
    
    private fun getStationLine(station: MetroStation): String {
        return when {
            StationData.metroLine1.contains(station) -> "Line 1"
            StationData.metroLine2.contains(station) -> "Line 2"
            StationData.metroLine3.contains(station) -> "Line 3"
            else -> "Unknown"
        }
    }
} 