package com.example.athensplus.presentation.transport.ui

import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.databinding.FragmentTransportBinding
import com.example.athensplus.domain.model.MetroStation

class TransportStateManager(
    private val fragment: Fragment,
    private val binding: FragmentTransportBinding,
    private val stationManager: StationManager
) {
    
    fun updateSelectionIndicator(
        selectedStartStation: MetroStation?,
        selectedEndStation: MetroStation?
    ) {
        val startText = binding.startStationText
        val endText = binding.endStationText
        val swapButton = binding.swapStationsButton
        val enterButton = binding.enterButton
        val interchangeContainer = binding.interchangeContainer
        val interchangeText = binding.interchangeStationText
        val secondArrow = binding.secondArrow
        val resetButton = binding.resetMapButton
        val airportTimetableButton = binding.airportTimetableButton
        val harborGateMapButton = binding.harborGateMapButton

        when {
            selectedStartStation == null -> {
                startText.text = fragment.getString(R.string.select_station)
                startText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.transport_text_on_tinted))
                endText.text = fragment.getString(R.string.select_station)
                endText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.transport_text_on_tinted))
                swapButton.isEnabled = false
                enterButton.visibility = View.VISIBLE
                interchangeContainer.visibility = View.GONE
                secondArrow.visibility = View.GONE
                resetButton.visibility = View.GONE
                airportTimetableButton.visibility = View.GONE
                harborGateMapButton.visibility = View.GONE
            }
            selectedEndStation == null -> {
                val startStationColor = stationManager.getStationColor(selectedStartStation)
                
                startText.text = selectedStartStation.nameEnglish
                startText.setTextColor(startStationColor)
                endText.text = fragment.getString(R.string.select_station)
                endText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.transport_text_on_tinted))
                swapButton.isEnabled = false
                enterButton.visibility = View.VISIBLE
                interchangeContainer.visibility = View.GONE
                secondArrow.visibility = View.GONE
                resetButton.visibility = View.VISIBLE
                airportTimetableButton.visibility = View.GONE
                harborGateMapButton.visibility = View.GONE
            }
            else -> {
                val startStationColor = stationManager.getStationColor(selectedStartStation)
                val endStationColor = stationManager.getStationColor(selectedEndStation)
                
                startText.text = selectedStartStation.nameEnglish
                startText.setTextColor(startStationColor)
                endText.text = selectedEndStation.nameEnglish
                endText.setTextColor(endStationColor)
                swapButton.isEnabled = true
                enterButton.visibility = View.VISIBLE
                resetButton.visibility = View.VISIBLE

                if (selectedEndStation.nameEnglish == "Airport") {
                    airportTimetableButton.visibility = View.VISIBLE
                } else airportTimetableButton.visibility = View.GONE

                if (selectedEndStation.nameEnglish == "Piraeus") {
                    harborGateMapButton.visibility = View.VISIBLE
                } else {
                    harborGateMapButton.visibility = View.GONE
                }

                val interchangeStation = stationManager.findInterchangeStation(selectedStartStation, selectedEndStation)
                if (interchangeStation != null) {
                    interchangeContainer.visibility = View.VISIBLE
                    secondArrow.visibility = View.VISIBLE
                    interchangeText.text = interchangeStation.nameEnglish
                    interchangeText.setTextColor(stationManager.getStationColor(interchangeStation))
                } else {
                    interchangeContainer.visibility = View.GONE
                    secondArrow.visibility = View.GONE
                }
            }
        }

        if (selectedStartStation != null && selectedEndStation != null) {
            enterButton.setBackgroundResource(R.drawable.rounded_button_bg)
            enterButton.setColorFilter(Color.WHITE)
        } else {
            enterButton.setBackgroundResource(R.drawable.rounded_button_bg_gray)
            enterButton.setColorFilter(Color.parseColor("#666666"))
        }
    }
} 