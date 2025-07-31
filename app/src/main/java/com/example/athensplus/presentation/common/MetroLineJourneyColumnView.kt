package com.example.athensplus.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.athensplus.domain.model.MetroStation

class MetroLineJourneyColumnView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var journeyNodes: List<JourneyNode> = emptyList()
    private var stepPositions: List<Float> = emptyList()

    data class JourneyNode(
        val station: MetroStation,
        val lineColor: Int,
        val isInterchange: Boolean = false,
        val instruction: String = ""
    )

    fun setJourney(nodes: List<JourneyNode>, positions: List<Float>) {
        journeyNodes = nodes
        stepPositions = positions
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (journeyNodes.isEmpty() || stepPositions.isEmpty()) return
        
        try {
            val width = width.toFloat()
            val height = height.toFloat()
            val centerX = width / 2

            paint.strokeWidth = 20f
            paint.strokeCap = Paint.Cap.ROUND
            
            if (stepPositions.size >= 2) {
                for (i in 0 until journeyNodes.size - 1) {
                    val currentNode = journeyNodes[i]
                    val nextNode = journeyNodes[i + 1]
                    val currentY = stepPositions[i]
                    val nextY = stepPositions[i + 1]

                    paint.color = currentNode.lineColor
                    canvas.drawLine(centerX, currentY, centerX, nextY, paint)
                }
            }

            for (i in journeyNodes.indices) {
                val node = journeyNodes[i]
                val y = stepPositions[i]
                
                if (node.isInterchange) {
                    stationPaint.style = Paint.Style.FILL
                    stationPaint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(centerX, y, 24f, stationPaint)

                    stationPaint.style = Paint.Style.STROKE
                    stationPaint.strokeWidth = 14f
                    stationPaint.color = node.lineColor
                    canvas.drawCircle(centerX, y, 24f, stationPaint)
                } else if (node.instruction.contains("Enter") || node.instruction.contains("Arrive at")) {
                    stationPaint.style = Paint.Style.FILL
                    stationPaint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(centerX, y, 24f, stationPaint)

                    stationPaint.style = Paint.Style.STROKE
                    stationPaint.strokeWidth = 14f
                    stationPaint.color = node.lineColor
                    canvas.drawCircle(centerX, y, 24f, stationPaint)
                } else {
                    stationPaint.style = Paint.Style.FILL
                    stationPaint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(centerX, y, 18f, stationPaint)

                    stationPaint.style = Paint.Style.STROKE
                    stationPaint.strokeWidth = 10f
                    stationPaint.color = node.lineColor
                    canvas.drawCircle(centerX, y, 18f, stationPaint)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MetroLineJourneyColumnView", "Error in onDraw: ${e.message}", e)
        }
    }
} 