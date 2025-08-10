package com.example.athensplus.presentation.transport.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.location.Geocoder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.utils.LocationService
import com.example.athensplus.core.utils.StationManager
import com.example.athensplus.core.utils.TimetableService
import com.example.athensplus.domain.model.MetroStation
import com.example.athensplus.domain.model.StationData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.example.athensplus.core.ui.MapStyleUtils
import com.example.athensplus.core.ui.MapUiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TransportDialogManager(
    private val fragment: Fragment,
    private val locationService: LocationService,
    private val stationManager: StationManager,
    private val timetableService: TimetableService,
    private val addressAutocompleteManager: com.example.athensplus.presentation.common.AddressAutocompleteManager
) {
    
    private var mapSelectionDialog: Dialog? = null
    private var isSelectingForDestination: Boolean = true
    
    fun showMapSelectionDialog() {
        try {
            isSelectingForDestination = true
            createMapSelectionDialog()
        } catch (e: Exception) {
            Log.e("TransportDialogManager", "Error showing map selection dialog", e)
            Toast.makeText(fragment.requireContext(), "Error opening map selection", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun showMapSelectionDialogForEditText(editText: EditText) {
        try {
            createMapSelectionDialogForEditText(editText)
        } catch (e: Exception) {
            Log.e("TransportDialogManager", "Error showing map selection dialog for edit text", e)
            Toast.makeText(fragment.requireContext(), "Error opening map selection", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun showStationTimetable(station: MetroStation) {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
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
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            var stationForTimetable = station
            var instructionText: String? = null

            if (!StationData.metroLine3.contains(station)) {
                val interchangeStation = stationManager.findNearestInterchangeToLine3(station)
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
    
    fun showPiraeusGateMap() {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Set modal behavior: clicking outside closes dialog
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        dialog.findViewById<TextView>(R.id.station_name_greek).text = fragment.getString(R.string.piraeus_greek)
        dialog.findViewById<TextView>(R.id.station_name_english).text = fragment.getString(R.string.piraeus_english)
        dialog.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        timetableContainer.removeAllViews()

        val messageText = TextView(fragment.requireContext()).apply {
            text = fragment.getString(R.string.piraeus_gate_map)
            textSize = 16f
            setTextColor(Color.parseColor("#009640"))
            gravity = Gravity.CENTER
            setPadding(32, 64, 32, 64)
        }
        timetableContainer.addView(messageText)

        dialog.show()
    }
    
    private fun createMapSelectionDialog() {
        mapSelectionDialog = Dialog(fragment.requireContext())
        mapSelectionDialog?.setContentView(R.layout.dialog_map_selection)
        mapSelectionDialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mapSelectionDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set modal behavior: clicking outside closes dialog
        mapSelectionDialog?.setCancelable(true)
        mapSelectionDialog?.setCanceledOnTouchOutside(true)

        val mapView = mapSelectionDialog?.findViewById<MapView>(R.id.map_selection_view)
        val titleText = mapSelectionDialog?.findViewById<TextView>(R.id.map_selection_title)
        val closeButton = mapSelectionDialog?.findViewById<ImageButton>(R.id.close_map_button)
        val confirmButton = mapSelectionDialog?.findViewById<LinearLayout>(R.id.confirm_location_button)
        val selectedLocationText = mapSelectionDialog?.findViewById<TextView>(R.id.selected_location_text)

        var selectedAddress: String? = null

        titleText?.text = if (isSelectingForDestination) "Choose Destination" else "Choose Starting Point"

        setupMapView(mapView, selectedLocationText) { address ->
            selectedAddress = address
        }

        closeButton?.setOnClickListener {
            mapSelectionDialog?.dismiss()
        }

        confirmButton?.setOnClickListener {
            val addressToUse = selectedAddress ?: ""
            mapSelectionDialog?.dismiss()
        }

        mapSelectionDialog?.show()
    }
    
    private fun createMapSelectionDialogForEditText(editText: EditText) {
        mapSelectionDialog = Dialog(fragment.requireContext())
        mapSelectionDialog?.setContentView(R.layout.dialog_map_selection)
        mapSelectionDialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mapSelectionDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set modal behavior: clicking outside closes dialog
        mapSelectionDialog?.setCancelable(true)
        mapSelectionDialog?.setCanceledOnTouchOutside(true)

        val mapView = mapSelectionDialog?.findViewById<MapView>(R.id.map_selection_view)
        val titleText = mapSelectionDialog?.findViewById<TextView>(R.id.map_selection_title)
        val closeButton = mapSelectionDialog?.findViewById<ImageButton>(R.id.close_map_button)
        val confirmButton = mapSelectionDialog?.findViewById<LinearLayout>(R.id.confirm_location_button)
        val selectedLocationText = mapSelectionDialog?.findViewById<TextView>(R.id.selected_location_text)

        var selectedAddress: String? = null

        titleText?.text = "Choose Location"

        setupMapView(mapView, selectedLocationText) { address ->
            selectedAddress = address
        }

        closeButton?.setOnClickListener {
            mapSelectionDialog?.dismiss()
        }

        confirmButton?.setOnClickListener {
            val addressToUse = selectedAddress ?: ""
            editText.setText(addressToUse)
            editText.clearFocus()
            mapSelectionDialog?.dismiss()
        }

        mapSelectionDialog?.show()
    }
    
    private fun setupMapView(
        mapView: MapView?,
        selectedLocationText: TextView?,
        onAddressSelected: (String) -> Unit
    ) {
        mapView?.onCreate(null)
        mapView?.getMapAsync { googleMap ->
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            MapStyleUtils.applyAppThemeMapStyle(fragment.requireContext(), googleMap)
            MapUiUtils.applyDefaultUiSettings(googleMap)
            
            fragment.lifecycleScope.launch {
                val currentLocation = locationService.getCurrentLocation()
                val startLocation = currentLocation ?: LatLng(37.9838, 23.7275)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 15f))
                if (locationService.isLocationPermissionGranted()) {
                    try {
                        googleMap.isMyLocationEnabled = true
                    } catch (e: SecurityException) {
                        Log.w("TransportDialogManager", "Location permission denied for map", e)
                    }
                }
            }

            fun cleanAddress(addr: String): String {
                var result = addr
                if (result.endsWith(", Greece")) result = result.removeSuffix(", Greece")
                result = result.replace(Regex(", ?\\d{5}"), "")
                return result.trim()
            }

            fun updateMostAccurateAddress(center: LatLng) {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val geocoder = Geocoder(fragment.requireContext(), Locale.getDefault())
                    val addresses = try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(center.latitude, center.longitude, 5)
                    } catch (e: Exception) {
                        null
                    }
                    val addressList = addresses?.mapNotNull { it.getAddressLine(0) }?.map { cleanAddress(it) }?.distinct() ?: emptyList()
                    val bestAddress = addressList.firstOrNull { it.contains(Regex("\\d+")) } ?: addressList.firstOrNull()
                    val coordString = "Location (${String.format(Locale.US, "%.4f", center.latitude)}, ${String.format(Locale.US, "%.4f", center.longitude)})"
                    val toShow = bestAddress ?: coordString
                    withContext(Dispatchers.Main) {
                        selectedLocationText?.text = toShow
                        onAddressSelected(toShow)
                    }
                }
            }

            updateMostAccurateAddress(googleMap.cameraPosition.target)
            googleMap.setOnCameraIdleListener {
                updateMostAccurateAddress(googleMap.cameraPosition.target)
            }
        }
    }
    
    private fun showTimetableDialog(station: MetroStation, timetableTables: List<Any>, waitTime: String?) {
        val dialog = Dialog(fragment.requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Set modal behavior: clicking outside closes dialog
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val stationGreekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val stationEnglishNameText = dialog.findViewById<TextView>(R.id.station_name_english)

        val stationColor = stationManager.getStationColor(station)
        stationGreekNameText.text = station.nameGreek
        stationEnglishNameText.text = station.nameEnglish
        stationGreekNameText.setTextColor(stationColor)
        stationEnglishNameText.setTextColor(stationColor)

        dialog.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val container = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        container.removeAllViews()

        @Suppress("UNCHECKED_CAST")
        val tables = timetableTables as? List<com.example.athensplus.domain.model.TimetableTable> ?: emptyList()
        
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
            title.setTextColor(stationColor)

            val headerRow = TableRow(dialog.context)
            tableData.headers.forEach { headerText ->
                val headerTextView = TextView(dialog.context).apply {
                    textSize = 15f
                    setTextColor(stationColor)
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
                        setTextColor(ContextCompat.getColor(context, R.color.transport_text_on_tinted))
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

            val waitTimeTitle = waitTimeView.findViewById<TextView>(R.id.direction_title)
            val waitTimeTableLayout = waitTimeView.findViewById<TableLayout>(R.id.timetable_table_layout)

            waitTimeTitle.text = "Average Wait Time"
            waitTimeTitle.setTextColor(stationColor)

            val waitTimeRow = TableRow(dialog.context)
            val waitTimeCell = TextView(dialog.context).apply {
                text = waitTime
                setTextColor(ContextCompat.getColor(context, R.color.transport_text_on_tinted))
                setPadding(12, 16, 12, 16)
                typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
                gravity = Gravity.CENTER
            }
            waitTimeRow.addView(waitTimeCell)
            waitTimeTableLayout.addView(waitTimeRow)
            container.addView(waitTimeView)
        }

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
        
        // Set modal behavior: clicking outside closes dialog
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val stationGreekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val stationEnglishNameText = dialog.findViewById<TextView>(R.id.station_name_english)
        val interchangeInfoText = dialog.findViewById<TextView>(R.id.interchange_info_text)

        val stationColor = stationManager.getStationColor(stationForTimetable)
        stationGreekNameText.text = stationForTimetable.nameGreek
        stationEnglishNameText.text = stationForTimetable.nameEnglish
        stationGreekNameText.setTextColor(stationColor)
        stationEnglishNameText.setTextColor(stationColor)

        if (instructionText != null) {
            interchangeInfoText?.text = instructionText
            interchangeInfoText?.visibility = View.VISIBLE
        } else {
            interchangeInfoText?.visibility = View.GONE
        }

        dialog.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val container = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        container.removeAllViews()

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

        title.text = "Airport Departures"
        title.setTextColor(stationColor)

        val headerRow = TableRow(dialog.context)
        val headerTextView = TextView(dialog.context).apply {
            text = "Departure Times"
            textSize = 15f
            setTextColor(stationColor)
            setPadding(12, 16, 12, 16)
            gravity = Gravity.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
        }
        headerRow.addView(headerTextView)
        tableLayout.addView(headerRow)

        val timesPerRow = 4
        times.chunked(timesPerRow).forEach { timeGroup ->
            val tableRow = TableRow(dialog.context)
        val cellTextView = TextView(dialog.context).apply {
                text = timeGroup.joinToString("  •  ")
            setTextColor(ContextCompat.getColor(context, R.color.transport_text_on_tinted))
                setPadding(12, 16, 12, 16)
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
            gravity = Gravity.CENTER
            }
            tableRow.addView(cellTextView)
            tableLayout.addView(tableRow)
        }

        container.addView(tableView)

        dialog.show()
    }
    
    fun onResume() {
        mapSelectionDialog?.findViewById<MapView>(R.id.map_selection_view)?.onResume()
    }
    
    fun onPause() {
        mapSelectionDialog?.findViewById<MapView>(R.id.map_selection_view)?.onPause()
    }
    
    fun onDestroy() {
        mapSelectionDialog?.findViewById<MapView>(R.id.map_selection_view)?.onDestroy()
    }
} 