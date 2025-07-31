package com.example.athensplus.core.utils

import android.content.Context
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import org.json.JSONObject
import java.io.IOException

object MetroTimeTable {
    
    private var timeTableJson: JSONObject? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            val inputStream = context.assets.open("metro_timetable.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            timeTableJson = JSONObject(jsonString)
            isInitialized = true
        } catch (e: IOException) {
            android.util.Log.e("MetroTimeTable", "Error loading metro timetable: ${e.message}", e)
            // Fallback to empty JSON if file not found
            timeTableJson = JSONObject("{}")
        }
    }

    private fun getTimeTableJson(context: Context): JSONObject {
        if (!isInitialized) {
            initialize(context)
        }
        return timeTableJson ?: JSONObject("{}")
    }

    fun getTravelTime(context: Context, startStation: MetroStation, endStation: MetroStation): Int {
        val line = getStationLine(startStation)
        val direction = getDirection(startStation, endStation, line)
        
        val lineKey = when (line) {
            "Line 1" -> "line1"
            "Line 2" -> "line2"
            "Line 3" -> "line3"
            else -> return 0
        }
        
        val directionKey = when (line) {
            "Line 1" -> if (direction == "Kifisia") "piraeus_to_kifisia" else "kifisia_to_piraeus"
            "Line 2" -> if (direction == "Elliniko") "anthoupoli_to_elliniko" else "elliniko_to_anthoupoli"
            "Line 3" -> if (direction == "Airport") "municipal_theater_to_airport" else "airport_to_municipal_theater"
            else -> return 0
        }
        
        android.util.Log.d("MetroTimeTable", "Line: $line, Direction: $direction, LineKey: $lineKey, DirectionKey: $directionKey")
        
        val startStationName = getStationGreekName(startStation)
        val endStationName = getStationGreekName(endStation)
        
        android.util.Log.d("MetroTimeTable", "Looking up stations: $startStationName -> $endStationName")
        
        val timeTableJson = getTimeTableJson(context)
        val times = timeTableJson.optJSONObject(lineKey)?.optJSONObject(directionKey)
        
        if (times == null) {
            return 0
        }
        
        val startTime = times.optInt(startStationName, -1)
        val endTime = times.optInt(endStationName, -1)
        
        android.util.Log.d("MetroTimeTable", "Start station: $startStationName, Start time: $startTime")
        android.util.Log.d("MetroTimeTable", "End station: $endStationName, End time: $endTime")
        
        if (startTime == -1 || endTime == -1) {
            android.util.Log.e("MetroTimeTable", "Station not found in timetable: $startStationName or $endStationName")
            return 0
        }
        
        val travelTime = kotlin.math.abs(endTime - startTime)
        android.util.Log.d("MetroTimeTable", "Calculated travel time: $travelTime minutes")
        return travelTime
    }

    fun getTravelTimeWithInterchange(
        context: Context,
        startStation: MetroStation, 
        endStation: MetroStation, 
        interchangeStation: MetroStation
    ): Int {
        val startLine = getStationLine(startStation)
        val endLine = getStationLine(endStation)
        
        if (startLine == endLine) {
            return getTravelTime(context, startStation, endStation)
        }
        
        val timeToInterchange = getTravelTime(context, startStation, interchangeStation)
        val timeFromInterchange = getTravelTime(context, interchangeStation, endStation)
        val interchangeTime = 4 // 4 minutes for interchange
        
        val totalTime = timeToInterchange + timeFromInterchange + interchangeTime
        android.util.Log.d("MetroTimeTable", "Interchange calculation: $timeToInterchange + $timeFromInterchange + $interchangeTime = $totalTime minutes")
        
        return totalTime
    }

    fun getStationCount(startStation: MetroStation, endStation: MetroStation): Int {
        val line = getStationLine(startStation)
        val lineStations = when (line) {
            "Line 1" -> StationData.metroLine1
            "Line 2" -> StationData.metroLine2
            "Line 3" -> StationData.metroLine3
            else -> emptyList()
        }
        
        val startIndex = lineStations.indexOf(startStation)
        val endIndex = lineStations.indexOf(endStation)
        
        if (startIndex == -1 || endIndex == -1) return 0
        
        return kotlin.math.abs(endIndex - startIndex)
    }

    fun getStationCountWithInterchange(
        startStation: MetroStation, 
        endStation: MetroStation, 
        interchangeStation: MetroStation
    ): Int {
        val startLine = getStationLine(startStation)
        val endLine = getStationLine(endStation)
        
        if (startLine == endLine) {
            return getStationCount(startStation, endStation)
        }
        
        val stationsToInterchange = getStationCount(startStation, interchangeStation)
        val stationsFromInterchange = getStationCount(interchangeStation, endStation)
        
        return stationsToInterchange + stationsFromInterchange
    }
    
    private fun getStationLine(station: MetroStation): String {
        return when {
            StationData.metroLine1.contains(station) -> "Line 1"
            StationData.metroLine2.contains(station) -> "Line 2"
            StationData.metroLine3.contains(station) -> "Line 3"
            else -> "Unknown"
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
            when (line) {
                "Line 1" -> "Kifisia"
                "Line 2" -> "Elliniko"
                "Line 3" -> "Airport"
                else -> ""
            }
        } else {
            when (line) {
                "Line 1" -> "Piraeus"
                "Line 2" -> "Anthoupoli"
                "Line 3" -> "Municipal Theater"
                else -> ""
            }
        }
    }
    
    private fun getStationGreekName(station: MetroStation): String {
        val greekName = station.nameGreek.uppercase()
            .replace("Ά", "Α")
            .replace("Έ", "Ε") 
            .replace("Ή", "Η")
            .replace("Ί", "Ι")
            .replace("Ό", "Ο")
            .replace("Ύ", "Υ")
            .replace("Ώ", "Ω")
            .replace("ά", "Α")
            .replace("έ", "Ε")
            .replace("ή", "Η")
            .replace("ί", "Ι")
            .replace("ό", "Ο")
            .replace("ύ", "Υ")
            .replace("ώ", "Ω")
        
        android.util.Log.d("MetroTimeTable", "Station: ${station.nameEnglish} -> Greek name: $greekName")
        return greekName
    }
} 