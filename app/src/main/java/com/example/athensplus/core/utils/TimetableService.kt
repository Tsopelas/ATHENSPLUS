@file:Suppress("SpellCheckingInspection")

package com.example.athensplus.core.utils

import android.content.Context
import com.example.athensplus.R
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.TimetableTable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimetableService(private val context: Context) {

    fun parseLine1Timetable(station: MetroStation): List<TimetableTable> {
        val inputStream = context.resources.openRawResource(R.raw.line1_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return emptyList()

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val result = mutableListOf<TimetableTable>()

        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_KIFISIA")) {
                val first = parts.getOrNull(2) ?: "-"
                val last = parts.getOrNull(4) ?: "-"
                val lastOmonia = parts.getOrNull(6)

                val rows = mutableListOf(listOf("Kifisia", first, last))
                lastOmonia?.let { rows.add(listOf("Omonia", "-", it)) }

                result.add(TimetableTable("Towards Kifisia", listOf("To", "First", "Last"), rows))
            } else if (line.startsWith("TOWARDS_PIRAEUS")) {
                val first = parts.getOrNull(2) ?: "-"
                val last = parts.getOrNull(4) ?: "-"
                result.add(TimetableTable("Towards Piraeus", listOf("To", "First", "Last"), listOf(listOf("Piraeus", first, last))))
            }
        }
        return result
    }

    fun parseLine2Timetable(station: MetroStation): List<TimetableTable> {
        val inputStream = context.resources.openRawResource(R.raw.line2_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return emptyList()

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val result = mutableListOf<TimetableTable>()

        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_ELLINIKO")) {
                val headers = listOf("First\n(Mon-Fri)", "First\n(Sat-Sun)", "Last\n(Sun-Thu)", "Last\n(Fri-Sat)")
                val row = listOf(
                    parts.getOrNull(2) ?: "-",
                    parts.getOrNull(4) ?: "-",
                    parts.getOrNull(6) ?: "-",
                    parts.getOrNull(8) ?: "-"
                )
                result.add(TimetableTable("Towards Elliniko", headers, listOf(row)))
            } else if (line.startsWith("TOWARDS_ANTHOUPOLI")) {
                val headers = listOf("First\n(All Days)", "Last\n(Sun-Thu)", "Last\n(Fri-Sat)")
                val row = listOf(
                    parts.getOrNull(2) ?: "-",
                    parts.getOrNull(4) ?: "-",
                    parts.getOrNull(6) ?: "-"
                )
                result.add(TimetableTable("Towards Anthoupoli", headers, listOf(row)))
            }
        }
        return result
    }

    fun parseLine3Timetable(station: MetroStation): List<TimetableTable> {
        val inputStream = context.resources.openRawResource(R.raw.line3_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return emptyList()

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val result = mutableListOf<TimetableTable>()

        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_AIRPORT")) {
                val headers = listOf("To", "First", "Last\n(Sun-Thu)", "Last\n(Fri-Sat)")

                val rowAirport = listOf(
                    "Airport",
                    parts.getOrNull(4) ?: "-",
                    parts.getOrNull(8) ?: "-",
                    parts.getOrNull(8) ?: "-"
                )

                val rowDPL = listOf(
                    "D. Plakentias",
                    parts.getOrNull(2) ?: "-",
                    parts.getOrNull(10) ?: "-",
                    parts.getOrNull(12) ?: "-"
                )
                result.add(TimetableTable("Towards Airport / D. Plakentias", headers, listOf(rowAirport, rowDPL)))
            } else if (line.startsWith("TOWARDS_DIMOTIKO_THEATRO")) {
                val headers = listOf("First", "Last")
                val row = listOf(parts.getOrNull(2) ?: "-", parts.getOrNull(4) ?: "-")
                result.add(TimetableTable("Towards Dimotiko Theatro", headers, listOf(row)))
            }
        }
        return result
    }

    fun parseAirportTimetable(station: MetroStation): List<String> {
        val inputStream = context.resources.openRawResource(R.raw.airport_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationLine = text.split("\n").find {
            val parts = it.split(";")
            parts.size > 1 && parts[1].trim().equals(station.nameEnglish, ignoreCase = true)
        }

        return stationLine?.split(";")?.drop(2)?.map { it.trim() } ?: emptyList()
    }

    fun parseWaitTime(): String {
        val waitTimeData = readWaitTimeData()
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowTimeStr = sdf.format(now.time)

        fun findWaitTime(calendar: Calendar, checkOvernight: Boolean): String? {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayTag = when (dayOfWeek) {
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY -> "MON-THU"
                Calendar.FRIDAY -> "FRI"
                Calendar.SATURDAY -> "SAT"
                Calendar.SUNDAY -> "SUN"
                else -> null
            }

            dayTag?.let {
                for (line in waitTimeData) {
                    val parts = line.split(";")
                    if (parts.size == 3 && parts[0] == it) {
                        val timeRange = parts[1].split("-")
                        val startTimeStr = timeRange[0].trim()
                        val endTimeStr = timeRange[1].trim()
                        val wait = parts[2]

                        val isOvernight = startTimeStr > endTimeStr
                        if (isOvernight) {
                            if (checkOvernight && nowTimeStr < endTimeStr) return wait
                            if (!checkOvernight && nowTimeStr >= startTimeStr) return wait
                        } else if (!checkOvernight) {
                            if (nowTimeStr >= startTimeStr && nowTimeStr < endTimeStr) return wait
                        }
                    }
                }
            }
            return null
        }

        if (now.get(Calendar.HOUR_OF_DAY) < 4) {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
            val waitTime = findWaitTime(yesterday, true)
            if (waitTime != null) return waitTime
        }

        val waitTime = findWaitTime(now, false)
        if (waitTime != null) return waitTime

        return "Not available at this time"
    }

    private fun readWaitTimeData(): List<String> {
        val inputStream = context.resources.openRawResource(R.raw.line3_averagewaittime)
        return inputStream.bufferedReader().use { it.readLines() }
    }
} 