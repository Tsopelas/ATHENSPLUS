package com.example.athensplus.presentation.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.athensplus.domain.model.MetroStation

class MetroLineJourneyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var journeySteps: List<JourneyStep> = emptyList()
    
    data class JourneyStep(
        val station: MetroStation,
        val lineColor: Int,
        val isInterchange: Boolean = false,
        val direction: String? = null
    )
    
    fun setJourney(steps: List<JourneyStep>) {
        journeySteps = steps
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (journeySteps.isEmpty()) return
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2
        
        // Draw one continuous line from top to bottom
        paint.strokeWidth = 6f
        paint.strokeCap = Paint.Cap.ROUND
        
        // Draw the main continuous line in the first step's color
        paint.color = journeySteps.first().lineColor
        canvas.drawLine(centerX, 0f, centerX, height, paint)
        
        // Draw colored segments on top if there are color changes
        for (i in 0 until journeySteps.size - 1) {
            val currentStep = journeySteps[i]
            val nextStep = journeySteps[i + 1]
            
            // Only draw colored segment if there's a color change
            if (currentStep.lineColor != nextStep.lineColor) {
                val segmentHeight = height / (journeySteps.size - 1)
                val startY = i * segmentHeight
                val endY = (i + 1) * segmentHeight
                
                // Draw colored segment
                paint.color = currentStep.lineColor
                canvas.drawLine(centerX, startY, centerX, endY, paint)
            }
        }
        
        // Draw station markers evenly distributed
        for (i in journeySteps.indices) {
            val step = journeySteps[i]
            val y = if (journeySteps.size == 1) {
                height / 2
            } else {
                (i * height) / (journeySteps.size - 1)
            }
            
            if (step.isInterchange) {
                // Draw interchange station (larger, split color)
                stationPaint.color = step.lineColor
                canvas.drawCircle(centerX, y, 12f, stationPaint)
                
                // Draw border in previous line color
                if (i > 0) {
                    val prevColor = journeySteps[i - 1].lineColor
                    stationPaint.color = prevColor
                    stationPaint.style = Paint.Style.STROKE
                    stationPaint.strokeWidth = 3f
                    canvas.drawCircle(centerX, y, 12f, stationPaint)
                    stationPaint.style = Paint.Style.FILL
                }
            } else {
                // Draw regular station
                stationPaint.color = step.lineColor
                canvas.drawCircle(centerX, y, 8f, stationPaint)
            }
            
            // Draw direction arrow
            if (step.direction != null && i < journeySteps.size - 1) {
                val nextY = if (journeySteps.size == 1) {
                    height / 2
                } else {
                    ((i + 1) * height) / (journeySteps.size - 1)
                }
                val arrowY = y + (nextY - y) / 2
                drawArrow(canvas, centerX, arrowY, step.lineColor)
            }
        }
    }
    
    private fun drawArrow(canvas: Canvas, x: Float, y: Float, color: Int) {
        arrowPaint.color = color
        arrowPaint.style = Paint.Style.FILL
        
        val path = Path()
        path.moveTo(x, y - 8)
        path.lineTo(x - 6, y - 2)
        path.lineTo(x + 6, y - 2)
        path.close()
        
        canvas.drawPath(path, arrowPaint)
    }
} 