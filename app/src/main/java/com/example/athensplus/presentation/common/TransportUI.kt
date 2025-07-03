package com.example.athensplus.presentation.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.example.athensplus.R
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.TimetableTable
import com.example.athensplus.core.utils.StationManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransportUI(
    private val fragment: Fragment,
    private val stationManager: StationManager
) {

    fun showLineMenu(
        binding: com.example.athensplus.databinding.FragmentTransportBinding,
        onLineSelected: (String) -> Unit
    ) {
        val inflater = LayoutInflater.from(fragment.requireContext())
        val popupView = LinearLayout(fragment.requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.dropdown_menu_background)
            elevation = resources.getDimension(R.dimen.cardview_default_elevation)
        }

        val popupWindow = PopupWindow(
            popupView,
            binding.buttonLinePicker.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = fragment.resources.getDimension(R.dimen.cardview_default_elevation)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        val menuItems = listOf(
            Triple("All Lines", 0, "#663399"),
            Triple("Line 1", R.drawable.dropdown_dot_green, "#2ECC40"),
            Triple("Line 2", R.drawable.dropdown_dot_red, "#FF4136"),
            Triple("Line 3", R.drawable.dropdown_dot_blue, "#0074D9")
        )

        menuItems.forEach { (title, dotRes, color) ->
            val itemView = inflater.inflate(R.layout.item_line_dropdown, null)
            val dot = itemView.findViewById<View>(R.id.line_dot)
            val text = itemView.findViewById<TextView>(R.id.line_text)

            if (dotRes != 0) {
                dot.setBackgroundResource(dotRes)
                dot.visibility = View.VISIBLE
            } else {
                dot.visibility = View.GONE
            }

            text.text = title
            text.setTextColor(Color.parseColor(color))

            itemView.setOnClickListener {
                onLineSelected(title)
                popupWindow.dismiss()
            }

            popupView.addView(itemView)
        }

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = popupView.measuredHeight

        popupWindow.showAsDropDown(
            binding.buttonLinePicker,
            0,
            -popupHeight - binding.buttonLinePicker.height
        )
    }

    fun showModeMenu(
        binding: com.example.athensplus.databinding.FragmentTransportBinding,
        onModeSelected: (String) -> Unit
    ) {
        val inflater = LayoutInflater.from(fragment.requireContext())
        val popupView = LinearLayout(fragment.requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.dropdown_menu_background)
            elevation = fragment.resources.getDimension(R.dimen.cardview_default_elevation)
        }

        val popupWindow = PopupWindow(
            popupView,
            binding.buttonModePicker.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = fragment.resources.getDimension(R.dimen.cardview_default_elevation)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        val menuItems = listOf(
            Triple("Metro", R.drawable.ic_metro, "#663399"),
            Triple("Bus Stops", R.drawable.ic_transport, "#663399"),
            Triple("Tram", R.drawable.ic_tram, "#663399")
        )

        menuItems.forEach { (title, iconRes, color) ->
            val itemView = inflater.inflate(R.layout.popup_menu_item, null).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(
                    fragment.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    fragment.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin) / 2,
                    fragment.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    fragment.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin) / 2
                )
            }

            val icon = itemView.findViewById<ImageView>(R.id.menu_item_icon)
            val text = itemView.findViewById<TextView>(R.id.menu_item_text)

            icon.setImageResource(iconRes)
            icon.setColorFilter(Color.parseColor(color))
            text.text = title
            text.setTextColor(Color.parseColor(color))

            itemView.setOnClickListener {
                onModeSelected(title)
                popupWindow.dismiss()
            }

            popupView.addView(itemView)
        }

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = popupView.measuredHeight

        popupWindow.showAsDropDown(
            binding.buttonModePicker,
            0,
            -popupHeight - binding.buttonModePicker.height
        )
    }

    fun showTimetableDialog(
        station: MetroStation,
        tables: List<TimetableTable>,
        waitTime: String? = null
    ) {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val greekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val englishNameText = dialog.findViewById<TextView>(R.id.station_name_english)
        greekNameText.text = station.nameGreek
        englishNameText.text = station.nameEnglish

        val lineColor = stationManager.getStationColor(station)
        greekNameText.setTextColor(lineColor)
        englishNameText.setTextColor(lineColor)

        val closeButton = dialog.findViewById<ImageView>(R.id.close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        val container = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        container.removeAllViews()

        tables.forEach { tableData ->
            val inflater = LayoutInflater.from(dialog.context)
            val tableView = inflater.inflate(R.layout.item_timetable_table, container, false)

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, 16)
            tableView.layoutParams = lp

            val title = tableView.findViewById<TextView>(R.id.direction_title)
            val tableLayout = tableView.findViewById<TableLayout>(R.id.timetable_table_layout)
            tableLayout.isStretchAllColumns = true

            title.text = tableData.direction
            title.setTextColor(lineColor)

            val headerRow = TableRow(dialog.context)
            tableData.headers.forEach { headerText ->
                val headerTextView = TextView(dialog.context).apply {
                    textSize = 15f
                    setTextColor(lineColor)
                    setPadding(12, 16, 12, 16)
                    gravity = Gravity.CENTER

                    if (headerText.contains("\n")) {
                        val parts = headerText.split("\n", limit = 2)
                        val mainText = parts[0]
                        val subText = parts[1]

                        val htmlText = "<b>$mainText</b><br/><small>$subText</small>"
                        text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    } else {
                        typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
                        text = headerText
                    }
                }
                headerRow.addView(headerTextView)
            }
            tableLayout.addView(headerRow)

            tableData.rows.forEach { rowData ->
                val tableRow = TableRow(dialog.context)
                rowData.forEach { cellData ->
                    val cellTextView = TextView(dialog.context).apply {
                        text = cellData
                        setTextColor(Color.BLACK)
                        setPadding(12, 16, 12, 16)
                        typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
                        gravity = Gravity.CENTER
                    }
                    tableRow.addView(cellTextView)
                }
                tableLayout.addView(tableRow)
            }
            container.addView(tableView)
        }

        if (waitTime != null) {
            val inflater = LayoutInflater.from(dialog.context)
            val waitTimeView = inflater.inflate(R.layout.item_timetable_table, container, false)

            val lpWait = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lpWait.setMargins(0, 0, 0, 16)
            waitTimeView.layoutParams = lpWait

            val title = waitTimeView.findViewById<TextView>(R.id.direction_title)
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val time = sdf.format(Date())
            title.text = "Estimated Wait Time ($time)"
            title.setTextColor(lineColor)

            val tableLayout = waitTimeView.findViewById<TableLayout>(R.id.timetable_table_layout)
            tableLayout.isStretchAllColumns = true
            tableLayout.removeAllViews()

            fun createCell(text: String, isHeader: Boolean = false): TextView {
                return TextView(dialog.context).apply {
                    this.text = text
                    this.setPadding(12, 16, 12, 16)
                    this.gravity = Gravity.CENTER
                    if (isHeader) {
                        this.typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
                        this.textSize = 15f
                        this.setTextColor(lineColor)
                    } else {
                        this.typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
                        this.setTextColor(Color.BLACK)
                    }
                }
            }

            val headerRow = TableRow(dialog.context)
            headerRow.addView(createCell("To", true))
            headerRow.addView(createCell("Wait Time", true))
            tableLayout.addView(headerRow)

            val airportRow = TableRow(dialog.context)
            airportRow.addView(createCell("Airport"))
            airportRow.addView(createCell("36'"))
            tableLayout.addView(airportRow)

            val dynamicRow = TableRow(dialog.context)
            dynamicRow.addView(createCell("D. Theatro"))
            dynamicRow.addView(createCell(waitTime))
            tableLayout.addView(dynamicRow)

            container.addView(waitTimeView)
        }

        dialog.show()
    }

    fun showDirectionsButton(
        binding: com.example.athensplus.databinding.FragmentTransportBinding,
        onDirectionsClick: () -> Unit
    ): Button {
        val directionsButton = Button(fragment.requireContext()).apply {
            text = "Directions"
            setBackgroundResource(R.drawable.rounded_directions_button)
            setTextColor(Color.WHITE)
            setPadding(32, 16, 32, 16)
            textSize = 16f
            elevation = 8f
            setOnClickListener { onDirectionsClick() }
        }

        val mapContainer = binding.mapView.parent as ViewGroup

        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 32)
        }

        if (mapContainer is FrameLayout) {
            val frameParams = FrameLayout.LayoutParams(layoutParams)
            frameParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            directionsButton.layoutParams = frameParams
        } else {
            directionsButton.layoutParams = layoutParams
        }

        mapContainer.addView(directionsButton)
        return directionsButton
    }

    fun drawRouteLine(
        googleMap: GoogleMap?,
        routePoints: List<LatLng>,
        steps: List<com.example.athensplus.domain.model.TransitStep>
    ) {
        if (routePoints.size < 2) return

        var currentStepIndex = 0

        for (i in 0 until routePoints.size - 1) {
            val currentStep = if (currentStepIndex < steps.size) steps[currentStepIndex] else steps.lastOrNull()

            val color = when (currentStep?.mode) {
                "WALKING" -> Color.GREEN
                "METRO" -> Color.BLUE
                "BUS" -> Color.rgb(255, 165, 0)
                "TRAM" -> Color.MAGENTA
                else -> Color.GRAY
            }

            val polylineOptions = PolylineOptions()
                .add(routePoints[i])
                .add(routePoints[i + 1])
                .width(6f)
                .color(color)

            googleMap?.addPolyline(polylineOptions)

            if (i > 0 && currentStepIndex < steps.size - 1) {
                currentStepIndex++
            }
        }
    }
} 