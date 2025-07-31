package com.example.athensplus.presentation.transport.ui

import android.graphics.Color
import android.view.View
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.presentation.transport.ui.TransportUI
import com.example.athensplus.databinding.FragmentTransportBinding
import com.example.athensplus.domain.model.MetroStation

class ControlSetupManager(
    private val fragment: Fragment,
    private val binding: FragmentTransportBinding,
    private val transportUI: TransportUI
) {
    
    fun setupControls(
        selectedMode: String,
        selectedLine: String,
        onModeChanged: (String) -> Unit,
        onLineChanged: (String) -> Unit,
        onDirectionsClicked: () -> Unit,
        onResetMapClicked: () -> Unit,
        onAirportTimetableClicked: (MetroStation?) -> Unit,
        onHarborGateMapClicked: () -> Unit,
        onChooseOnMapClicked: () -> Unit
    ) {
        setupDirectionsButton(onDirectionsClicked)
        setupModePicker(selectedMode, onModeChanged)
        setupLinePicker(selectedLine, onLineChanged)
        setupResetButton(onResetMapClicked)
        setupAirportTimetableButton(onAirportTimetableClicked)
        setupHarborGateMapButton(onHarborGateMapClicked)
        setupChooseOnMapButton(onChooseOnMapClicked)
    }
    
    private fun setupDirectionsButton(onDirectionsClicked: () -> Unit) {
        binding.buttonDirections.setOnClickListener {
            onDirectionsClicked()
        }
    }
    
    private fun setupModePicker(selectedMode: String, onModeChanged: (String) -> Unit) {
        binding.buttonModePicker.setOnClickListener {
            transportUI.showModeMenu(binding) { mode: String ->
                onModeChanged(mode)
            }
        }
    }
    
    private fun setupLinePicker(selectedLine: String, onLineChanged: (String) -> Unit) {
        binding.buttonLinePicker.setOnClickListener {
            transportUI.showLineMenu(binding) { line: String ->
                onLineChanged(line)
            }
        }
    }
    
    private fun setupResetButton(onResetMapClicked: () -> Unit) {
        binding.resetMapButton.setOnClickListener {
            onResetMapClicked()
        }
    }
    
    private fun setupAirportTimetableButton(onAirportTimetableClicked: (MetroStation?) -> Unit) {
        binding.airportTimetableButton.setOnClickListener {
            onAirportTimetableClicked(null)
        }
    }
    
    private fun setupHarborGateMapButton(onHarborGateMapClicked: () -> Unit) {
        binding.harborGateMapButton.setOnClickListener {
            onHarborGateMapClicked()
        }
    }
    
    private fun setupChooseOnMapButton(onChooseOnMapClicked: () -> Unit) {
        binding.chooseOnMapCard.setOnClickListener {
            onChooseOnMapClicked()
        }
    }
    
    fun updateModeUI(mode: String) {
        binding.modeText.text = mode
        binding.modeIcon.setImageResource(when(mode) {
            "Metro" -> R.drawable.ic_metro
            "Bus Stops" -> R.drawable.ic_transport
            "Tram" -> R.drawable.ic_tram
            else -> R.drawable.ic_metro
        })
    }
    
    fun updateLineUI(line: String) {
        binding.lineText.text = line
        binding.lineText.setTextColor(when(line) {
            "Line 1" -> Color.parseColor("#2ECC40")
            "Line 2" -> Color.parseColor("#FF4136")
            "Line 3" -> Color.parseColor("#0074D9")
            else -> Color.parseColor("#2ECC40")
        })
        
        val buttonDot = binding.buttonLinePicker.findViewById<View>(R.id.line_dot)
        when(line) {
            "Line 1" -> {
                buttonDot.setBackgroundResource(R.drawable.dropdown_dot_green)
                buttonDot.visibility = View.VISIBLE
            }
            "Line 2" -> {
                buttonDot.setBackgroundResource(R.drawable.dropdown_dot_red)
                buttonDot.visibility = View.VISIBLE
            }
            "Line 3" -> {
                buttonDot.setBackgroundResource(R.drawable.dropdown_dot_blue)
                buttonDot.visibility = View.VISIBLE
            }
            else -> buttonDot.visibility = View.GONE
        }
    }
} 