package com.example.athensplus.presentation.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.athensplus.R
import com.example.athensplus.domain.model.DirectionsResult
import com.example.athensplus.domain.model.TransitStep
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class DirectionsUI(private val context: Context) {

    fun showDirectionsDialog(
        directionsResult: DirectionsResult,
        onRouteDrawn: (List<LatLng>) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_directions)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val stepsContainer = dialog.findViewById<LinearLayout>(R.id.steps_container)
        val closeButton = dialog.findViewById<Button>(R.id.close_button)

        stepsContainer.removeAllViews()
        directionsResult.steps.forEach { step ->
            val stepView = createStepView(step)
            stepsContainer.addView(stepView)
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        
        onRouteDrawn(directionsResult.routePoints)
    }

    private fun createStepView(step: TransitStep): View {
        val stepView = LayoutInflater.from(context).inflate(R.layout.item_direction_step, null)
        
        val modeIcon = stepView.findViewById<ImageView>(R.id.mode_icon)
        val instructionText = stepView.findViewById<TextView>(R.id.instruction_text)
        val durationText = stepView.findViewById<TextView>(R.id.duration_text)
        val detailsText = stepView.findViewById<TextView>(R.id.details_text)

        when (step.mode) {
            "Walking" -> {
                modeIcon.setImageResource(R.drawable.ic_walking)
                modeIcon.setColorFilter(Color.parseColor("#666666"))
            }
            "Metro" -> {
                modeIcon.setImageResource(R.drawable.ic_metro)
                modeIcon.setColorFilter(Color.parseColor("#1976D2"))
            }
            "Tram" -> {
                modeIcon.setImageResource(R.drawable.ic_tram)
                modeIcon.setColorFilter(Color.parseColor("#4CAF50"))
            }
            "Bus" -> {
                modeIcon.setImageResource(R.drawable.ic_transport)
                modeIcon.setColorFilter(Color.parseColor("#FF9800"))
            }
        }

        instructionText.text = step.instruction
        durationText.text = step.duration

        val details = mutableListOf<String>()
        step.line?.let { details.add("Line: $it") }
        step.departureStop?.let { details.add("From: $it") }
        step.arrivalStop?.let { details.add("To: $it") }
        step.walkingDistance?.let { details.add("Distance: $it") }

        detailsText.text = details.joinToString(" • ")
        detailsText.visibility = if (details.isNotEmpty()) View.VISIBLE else View.GONE

        return stepView
    }

    fun drawRouteOnMap(
        routePoints: List<LatLng>,
        mapManager: com.example.athensplus.core.utils.MapManager
    ): PolylineOptions {
        return PolylineOptions()
            .addAll(routePoints)
            .color(Color.parseColor("#1976D2"))
            .width(8f)
            .geodesic(false)
    }
} 