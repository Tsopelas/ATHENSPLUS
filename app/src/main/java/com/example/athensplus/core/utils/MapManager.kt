package com.example.athensplus.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.content.res.ResourcesCompat
import com.example.athensplus.R
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class MapManager(private val context: Context) {
    private var currentMarkers: MutableList<Marker> = mutableListOf()
    private var piraeusGateMarkers: MutableList<Marker> = mutableListOf()

    fun createStationMarker(
        outlineColor: Int, 
        outlineWidth: Float = 12f, 
        radius: Float = 24f, 
        isSelected: Boolean = false
    ): BitmapDescriptor {
        val actualRadius = if (isSelected) radius * 1.5f else radius
        val actualOutlineWidth = if (isSelected) outlineWidth * 1.5f else outlineWidth
        val size = ((actualRadius + actualOutlineWidth) * 2).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        val outlinePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = actualOutlineWidth
            color = outlineColor
        }
        canvas.drawCircle(center, center, actualRadius, outlinePaint)

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isSelected) Color.BLACK else Color.WHITE
        }
        canvas.drawCircle(center, center, actualRadius - actualOutlineWidth / 2, fillPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun createStationLabel(
        station: MetroStation, 
        color: Int, 
        textOffset: Float = 0f, 
        isFirstOrLast: Boolean = false
    ): BitmapDescriptor {
        val greekText = station.nameGreek
        val englishText = station.nameEnglish
        val greekFontSize = 54f
        val englishFontSize = 40f
        val padding = 10f
        val lineSpacing = 6f
        val labelOffset = 10f

        val greekTypeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
        val englishTypeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)

        val greekPaint = Paint().apply {
            isAntiAlias = true
            textSize = greekFontSize
            typeface = greekTypeface
            textAlign = Paint.Align.LEFT
        }

        val englishPaint = Paint().apply {
            isAntiAlias = true
            textSize = englishFontSize
            typeface = englishTypeface
            textAlign = Paint.Align.LEFT
        }

        val greekBounds = Rect()
        val englishBounds = Rect()
        greekPaint.getTextBounds(greekText, 0, greekText.length, greekBounds)
        englishPaint.getTextBounds(englishText, 0, englishText.length, englishBounds)

        val textWidth = maxOf(greekPaint.measureText(greekText), englishPaint.measureText(englishText))
        val textHeight = greekBounds.height() + englishBounds.height() + lineSpacing
        val width = (textWidth + 2 * padding).toInt()
        val height = if (isFirstOrLast) {
            (textHeight + 2 * padding).toInt()
        } else {
            (textHeight + 2 * padding + textOffset).toInt()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (isFirstOrLast) {
            val bgPaint = Paint().apply {
                this.color = color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 24f, 24f, bgPaint)
            greekPaint.color = Color.WHITE
            englishPaint.color = Color.WHITE
        } else if (station.isInterchange) {
            greekPaint.color = Color.BLACK
            englishPaint.color = Color.BLACK
        } else {
            greekPaint.color = color
            englishPaint.color = color
        }

        canvas.drawText(
            greekText,
            padding,
            padding + greekBounds.height() - labelOffset,
            greekPaint
        )

        canvas.drawText(
            englishText,
            padding,
            padding + greekBounds.height() + lineSpacing + englishBounds.height() - labelOffset,
            englishPaint
        )

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun drawMetroLines(googleMap: GoogleMap?) {
        try {
            googleMap?.clear()

            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(StationData.line1CurvedPoints)
                    .color(Color.parseColor("#009640"))
                    .width(22f)
                    .geodesic(false)
                    .jointType(JointType.ROUND)
            )

            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(StationData.line2CurvedPoints)
                    .color(Color.parseColor("#e30613"))
                    .width(22f)
                    .geodesic(false)
                    .jointType(JointType.ROUND)
            )

            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(StationData.line3CurvedPoints)
                    .color(Color.parseColor("#0057a8"))
                    .width(22f)
                    .geodesic(false)
                    .jointType(JointType.ROUND)
            )
        } catch (_: Exception) {
        }
    }

    fun addPortMarkers(googleMap: GoogleMap?) {
        piraeusGateMarkers.forEach { it.remove() }
        piraeusGateMarkers.clear()
        StationData.piraeusGates.forEach { (gate, latLng, description) ->
            val marker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(gate)
                    .snippet(description)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            marker?.let { piraeusGateMarkers.add(it) }
        }
    }

    fun clearMarkers() {
        currentMarkers.forEach { it.remove() }
        currentMarkers.clear()
    }

    fun addMarker(marker: Marker) {
        currentMarkers.add(marker)
    }

    fun findTextPosition(
        originalPosition: LatLng,
        text: String,
        existingLabels: List<Pair<LatLng, String>>,
        lines: List<List<LatLng>>
    ): LatLng {
        val baseOffset = 0.0015

        for (multiplier in 1..3) {
            val currentOffset = baseOffset * multiplier

            val offsets = listOf(
                Pair(0.0, currentOffset),
                Pair(0.0, -currentOffset),
                Pair(currentOffset, currentOffset),
                Pair(currentOffset, -currentOffset),
                Pair(-currentOffset, currentOffset),
                Pair(-currentOffset, -currentOffset)
            )

            for (offset in offsets) {
                val newPosition = LatLng(
                    originalPosition.latitude + offset.second,
                    originalPosition.longitude + offset.first
                )

                if (!isTextOverlappingLines(newPosition, text, lines) &&
                    !isTextOverlappingLabels(newPosition, text, existingLabels)) {
                    return newPosition
                }
            }
        }

        return LatLng(
            originalPosition.latitude + baseOffset * 3,
            originalPosition.longitude
        )
    }

    private fun isTextOverlappingLines(position: LatLng, text: String, lines: List<List<LatLng>>): Boolean {
        val textBounds = getTextBounds(position, text)

        for (line in lines) {
            for (i in 0 until line.size - 1) {
                val start = line[i]
                val end = line[i + 1]

                if (isLineNearRect(start, end, textBounds) || isLineIntersectingRect(start, end, textBounds)) {
                    return true
                }
            }
        }
        return false
    }

    private fun isLineNearRect(lineStart: LatLng, lineEnd: LatLng, rect: RectF): Boolean {
        val buffer = 0.0001f

        val expandedRect = RectF(
            rect.left - buffer,
            rect.top - buffer,
            rect.right + buffer,
            rect.bottom + buffer
        )

        val startPoint = PointF(lineStart.longitude.toFloat(), lineStart.latitude.toFloat())
        val endPoint = PointF(lineEnd.longitude.toFloat(), lineEnd.latitude.toFloat())

        return isPointInRect(startPoint, expandedRect) || isPointInRect(endPoint, expandedRect)
    }

    private fun isPointInRect(point: PointF, rect: RectF): Boolean {
        return point.x >= rect.left && point.x <= rect.right &&
                point.y >= rect.top && point.y <= rect.bottom
    }

    private fun isTextOverlappingLabels(position: LatLng, text: String, existingLabels: List<Pair<LatLng, String>>): Boolean {
        val textBounds = getTextBounds(position, text)

        for (label in existingLabels) {
            val otherBounds = getTextBounds(label.first, label.second)
            if (isRectsOverlapping(textBounds, otherBounds)) {
                return true
            }
        }
        return false
    }

    private fun getTextBounds(position: LatLng, text: String): RectF {
        val textWidth = text.length * 0.0004
        val textHeight = 0.0003

        val buffer = 0.0001

        return RectF(
            (position.longitude - (textWidth + buffer) / 2).toFloat(),
            (position.latitude - (textHeight + buffer) / 2).toFloat(),
            (position.longitude + (textWidth + buffer) / 2).toFloat(),
            (position.latitude + (textHeight + buffer) / 2).toFloat()
        )
    }

    private fun isLineIntersectingRect(lineStart: LatLng, lineEnd: LatLng, rect: RectF): Boolean {
        val lines = listOf(
            Pair(PointF(rect.left, rect.top), PointF(rect.right, rect.top)),
            Pair(PointF(rect.right, rect.top), PointF(rect.right, rect.bottom)),
            Pair(PointF(rect.right, rect.bottom), PointF(rect.left, rect.bottom)),
            Pair(PointF(rect.left, rect.bottom), PointF(rect.left, rect.top))
        )

        val p1 = PointF(lineStart.longitude.toFloat(), lineStart.latitude.toFloat())
        val p2 = PointF(lineEnd.longitude.toFloat(), lineEnd.latitude.toFloat())

        for (rectLine in lines) {
            if (isLinesIntersecting(p1, p2, rectLine.first, rectLine.second)) {
                return true
            }
        }
        return false
    }

    // this is so uni for real i grabbed it straight from data structures
    private fun isLinesIntersecting(p1: PointF, p2: PointF, p3: PointF, p4: PointF): Boolean {
        val denominator = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y)
        if (denominator == 0f) return false

        val ua = ((p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x)) / denominator
        val ub = ((p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x)) / denominator

        return ua in 0f..1f && ub in 0f..1f
    }

    private fun isRectsOverlapping(rect1: RectF, rect2: RectF): Boolean {
        return !(rect1.right < rect2.left ||
                rect1.left > rect2.right ||
                rect1.bottom < rect2.top ||
                rect1.top > rect2.bottom)
    }
} 