package com.example.athensplus.presentation.transport.timetables

import android.app.Dialog
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.utils.TimetableService
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimetableManager(
    private val fragment: Fragment,
    private val timetableService: TimetableService
) {
    
    fun showStationTimetable(station: MetroStation) {
        (fragment as LifecycleOwner).lifecycleScope.launch(Dispatchers.Main) {
            val timetableTables = withContext(Dispatchers.IO) {
                when {
                    StationData.metroLine1.contains(station) -> timetableService.parseLine1Timetable(station)
                    StationData.metroLine2.contains(station) -> timetableService.parseLine2Timetable(station)
                    StationData.metroLine3.contains(station) -> timetableService.parseLine3Timetable(station)
                    else -> emptyList()
                }
            }

            if (timetableTables.isNotEmpty()) {
                val waitTime = if (StationData.metroLine3.contains(station)) {
                    withContext(Dispatchers.Default) {
                        timetableService.parseWaitTime()
                    }
                } else null

                showTimetableDialog(station, timetableTables, waitTime)
            } else {
                Toast.makeText(fragment.context, fragment.getString(R.string.timetable_not_available), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun showAirportTimetable(station: MetroStation) {
        (fragment as LifecycleOwner).lifecycleScope.launch(Dispatchers.IO) {
            var stationForTimetable = station
            var instructionText: String? = null

            if (!StationData.metroLine3.contains(station)) {
                val interchangeStation = findNearestInterchangeToLine3(station)
                if (interchangeStation != null) {
                    stationForTimetable = interchangeStation
                    instructionText = "Take Metro to ${interchangeStation.nameEnglish} for Airport Line (Directions shown on map)"
                } else {
                    instructionText = "No direct route to Airport Line found."
                }
            }

            val times = timetableService.parseAirportTimetable(stationForTimetable)
            withContext(Dispatchers.Main) {
                if (times.isNotEmpty()) {
                    showAirportTimetableDialog(stationForTimetable, times, instructionText)
                } else {
                    Toast.makeText(fragment.context, fragment.getString(R.string.airport_timetable_not_available), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showTimetableDialog(station: MetroStation, timetableTables: List<Any>, waitTime: Any?) {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val stationGreekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val stationEnglishNameText = dialog.findViewById<TextView>(R.id.station_name_english)

        stationGreekNameText.text = station.nameGreek
        stationEnglishNameText.text = station.nameEnglish

        dialog.findViewById<TextView>(R.id.close_button)?.setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        timetableContainer?.removeAllViews()

        dialog.show()
    }
    
    private fun showAirportTimetableDialog(stationForTimetable: MetroStation, times: List<String>, instructionText: String?) {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val stationGreekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val stationEnglishNameText = dialog.findViewById<TextView>(R.id.station_name_english)
        val interchangeInfoText = dialog.findViewById<TextView>(R.id.interchange_info_text)

        stationGreekNameText.text = stationForTimetable.nameGreek
        stationEnglishNameText.text = stationForTimetable.nameEnglish

        dialog.findViewById<TextView>(R.id.close_button)?.setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        timetableContainer?.removeAllViews()

        if (instructionText != null) {
            interchangeInfoText?.text = if (instructionText.contains("No direct route")) 
                fragment.getString(R.string.no_direct_route_airport) 
            else 
                fragment.getString(R.string.take_metro_to_airport, stationForTimetable.nameEnglish)
            interchangeInfoText?.visibility = android.view.View.VISIBLE
        } else {
            interchangeInfoText?.visibility = android.view.View.GONE
        }

        val timesText = times.joinToString(separator = "  •  ")
        val cellTextView = TextView(dialog.context).apply {
            text = timesText
            setTextColor(Color.BLACK)
            setPadding(16, 16, 16, 16)
            typeface = ResourcesCompat.getFont(fragment.requireContext(), R.font.montserrat_regular)
            gravity = android.view.Gravity.CENTER
            isSingleLine = false
        }
        timetableContainer?.addView(cellTextView)

        dialog.show()
    }
    
    private fun findNearestInterchangeToLine3(station: MetroStation): MetroStation? {
        return StationData.metroLine3.find { it.isInterchange }
    }
} 