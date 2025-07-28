package com.example.athensplus.presentation.transport.timetables

import android.util.Log
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.domain.model.TransitStep

class WaitTimeManager(
    private val fragment: Fragment
) {
    
    fun showWaitTimeInfo(step: TransitStep, waitTimeText: TextView, nextDepartureText: TextView) {
        try {
            if (step.waitTime != null && step.waitTimeMinutes > 0) {
                val waitTimeString = "${step.waitTimeMinutes}m"
                
                waitTimeText.text = fragment.getString(R.string.wait_time_format, waitTimeString)

                val departureTime = step.departureTime ?: ""
                nextDepartureText.text = fragment.getString(R.string.next_departure_format, departureTime)

                if (step.reliability != null) {
                    val reliabilityIcon = when (step.reliability) {
                        "High" -> "🟢"
                        "Medium" -> "🟡"
                        "Low" -> "🔴"
                        else -> ""
                    }
                    waitTimeText.text = fragment.getString(R.string.reliability_wait_format, reliabilityIcon, waitTimeString)
                }
            } else if (step.departureTimeValue > 0 && !step.departureTime.isNullOrEmpty()) {
                val currentTime = System.currentTimeMillis() / 1000 // Current time in seconds
                val waitTimeSeconds = step.departureTimeValue - currentTime
                
                if (waitTimeSeconds > 0) {
                    val waitTimeMinutes = waitTimeSeconds / 60
                    val waitTimeSecondsRemaining = waitTimeSeconds % 60
                    
                    val waitTimeString = when {
                        waitTimeMinutes > 0 -> "${waitTimeMinutes}m ${waitTimeSecondsRemaining}s"
                        else -> "${waitTimeSecondsRemaining}s"
                    }
                    
                    waitTimeText.text = fragment.getString(R.string.wait_time_format, waitTimeString)

                    val departureTime = step.departureTime
                    nextDepartureText.text = fragment.getString(R.string.next_departure_format, departureTime)
                } else {
                    waitTimeText.text = fragment.getString(R.string.bus_departed)
                    nextDepartureText.text = ""
                }
            } else {
                if (!step.departureTime.isNullOrEmpty()) {
                    waitTimeText.text = fragment.getString(R.string.wait_time_check_schedule)
                    nextDepartureText.text = fragment.getString(R.string.next_departure_format, step.departureTime)
                } else {
                    waitTimeText.text = fragment.getString(R.string.wait_time_not_available)
                    nextDepartureText.text = ""
                }
            }

            if (step.frequency != null) {
                nextDepartureText.text = fragment.getString(R.string.departure_with_frequency, nextDepartureText.text, step.frequency)
            }
            
        } catch (e: Exception) {
            Log.e("WaitTimeManager", "Error calculating wait time", e)
            waitTimeText.text = fragment.getString(R.string.error_loading_wait_time)
            nextDepartureText.text = ""
        }
    }

    fun calculateWaitTime(step: TransitStep): String {
        return when {
            step.waitTime != null && step.waitTimeMinutes > 0 -> "${step.waitTimeMinutes}m"
            step.departureTimeValue > 0 && !step.departureTime.isNullOrEmpty() -> {
                val currentTime = System.currentTimeMillis() / 1000
                val waitTimeSeconds = step.departureTimeValue - currentTime
                
                if (waitTimeSeconds > 0) {
                    val waitTimeMinutes = waitTimeSeconds / 60
                    val waitTimeSecondsRemaining = waitTimeSeconds % 60
                    
                    when {
                        waitTimeMinutes > 0 -> "${waitTimeMinutes}m ${waitTimeSecondsRemaining}s"
                        else -> "${waitTimeSecondsRemaining}s"
                    }
                } else {
                    fragment.getString(R.string.bus_departed)
                }
            }
            !step.departureTime.isNullOrEmpty() -> fragment.getString(R.string.wait_time_check_schedule)
            else -> fragment.getString(R.string.wait_time_not_available)
        }
    }

    fun getReliabilityIcon(reliability: String?): String {
        return when (reliability) {
            "High" -> "🟢"
            "Medium" -> "🟡"
            "Low" -> "🔴"
            else -> ""
        }
    }

    fun formatDepartureTime(departureTime: String?): String {
        return departureTime ?: ""
    }

    fun shouldShowFrequency(step: TransitStep): Boolean {
        return step.frequency != null
    }
} 