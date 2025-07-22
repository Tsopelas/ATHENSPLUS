package com.example.athensplus.core.utils

import android.content.Context
import android.util.Log
import com.example.athensplus.domain.model.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*

class RealTimeInfoService(
    private val context: Context,
    private val apiKey: String
) {
    
    suspend fun getServiceAlerts(): List<ServiceAlert> = withContext(Dispatchers.IO) {
        try {
            val demoAlerts = generateDemoServiceAlerts()
            Log.d("RealTimeInfoService", "Retrieved ${demoAlerts.size} service alerts")
            demoAlerts
        } catch (e: Exception) {
            Log.e("RealTimeInfoService", "Error fetching service alerts", e)
            emptyList()
        }
    }
    
    suspend fun getLiveVehicles(
        bounds: Pair<LatLng, LatLng>? = null,
        vehicleTypes: List<VehicleType> = listOf(VehicleType.BUS, VehicleType.METRO, VehicleType.TRAM)
    ): List<LiveVehicle> = withContext(Dispatchers.IO) {
        try {
            val vehicles = mutableListOf<LiveVehicle>()
            
            vehicleTypes.forEach { type ->
                vehicles.addAll(generateDemoVehicles(type, bounds))
            }
            
            Log.d("RealTimeInfoService", "Retrieved ${vehicles.size} live vehicles")
            vehicles
        } catch (e: Exception) {
            Log.e("RealTimeInfoService", "Error fetching live vehicles", e)
            emptyList()
        }
    }
    
    fun getLiveVehicleUpdates(
        vehicleTypes: List<VehicleType> = listOf(VehicleType.BUS, VehicleType.METRO, VehicleType.TRAM)
    ): Flow<List<LiveVehicle>> = flow {
        while (true) {
            try {
                val vehicles = getLiveVehicles(vehicleTypes = vehicleTypes)
                emit(vehicles)
                delay(30000)
            } catch (e: Exception) {
                Log.e("RealTimeInfoService", "Error in live vehicle updates", e)
                delay(60000)
            }
        }
    }
    
    suspend fun getStationStatus(
        stationIds: List<String>? = null
    ): List<StationStatus> = withContext(Dispatchers.IO) {
        try {
            val allStations = StationData.metroLine1 + StationData.metroLine2 + StationData.metroLine3
            val stationsToCheck = if (stationIds != null) {
                allStations.filter { station -> 
                    stationIds.contains(station.nameEnglish) || stationIds.contains(station.nameGreek)
                }
            } else {
                allStations.take(10)
            }
            
            val statusList = stationsToCheck.map { station ->
                generateStationStatus(station)
            }
            
            Log.d("RealTimeInfoService", "Retrieved status for ${statusList.size} stations")
            statusList
        } catch (e: Exception) {
            Log.e("RealTimeInfoService", "Error fetching station status", e)
            emptyList()
        }
    }
    
    suspend fun getAlertsByLine(line: String): List<ServiceAlert> = withContext(Dispatchers.IO) {
        getServiceAlerts().filter { alert ->
            alert.affectedLines.any { it.contains(line, ignoreCase = true) }
        }
    }
    
    suspend fun getAlertsBySeverity(severity: AlertSeverity): List<ServiceAlert> = withContext(Dispatchers.IO) {
        getServiceAlerts().filter { it.severity == severity }
    }
    
    suspend fun getActiveAlerts(): List<ServiceAlert> = withContext(Dispatchers.IO) {
        getServiceAlerts().filter { it.isActive }
    }
    
    suspend fun getVehiclesByLine(line: String): List<LiveVehicle> = withContext(Dispatchers.IO) {
        getLiveVehicles().filter { it.line.contains(line, ignoreCase = true) }
    }
    
    suspend fun getNearbyVehicles(
        location: LatLng,
        radiusKm: Double = 2.0
    ): List<LiveVehicle> = withContext(Dispatchers.IO) {
        getLiveVehicles().filter { vehicle ->
            calculateDistance(location, vehicle.currentLocation) <= radiusKm * 1000
        }
    }
    
    private fun generateDemoServiceAlerts(): List<ServiceAlert> {
        val currentTime = Date()
        val oneHourLater = Date(currentTime.time + 3600000)
        val tomorrow = Date(currentTime.time + 86400000)
        
        return listOf(
            ServiceAlert(
                id = "alert_001",
                title = "Metro Line 2 Delays",
                description = "Due to technical issues at Omonia station, expect 5-10 minute delays on Line 2.",
                severity = AlertSeverity.MEDIUM,
                affectedLines = listOf("Line 2", "M2"),
                affectedStations = listOf("Omonia", "Panepistimio", "Syntagma"),
                startTime = currentTime,
                endTime = oneHourLater,
                isActive = true,
                category = AlertCategory.DELAY
            ),
            ServiceAlert(
                id = "alert_002",
                title = "Bus Route 040 Diverted",
                description = "Route 040 is diverted due to road works on Kifissias Avenue. Alternative stops available.",
                severity = AlertSeverity.HIGH,
                affectedLines = listOf("040"),
                affectedStations = listOf("Kifissia", "Marousi"),
                startTime = currentTime,
                endTime = tomorrow,
                isActive = true,
                category = AlertCategory.CLOSURE
            ),
            ServiceAlert(
                id = "alert_003",
                title = "Tram Maintenance Tonight",
                description = "Tram services will be limited between 00:30 and 05:00 for scheduled maintenance.",
                severity = AlertSeverity.LOW,
                affectedLines = listOf("T1", "T2", "T3"),
                affectedStations = listOf("Syntagma", "Nea Smyrni", "Voula"),
                startTime = Date(currentTime.time + 18000000),
                endTime = Date(currentTime.time + 36000000),
                isActive = false,
                category = AlertCategory.MAINTENANCE
            ),
            ServiceAlert(
                id = "alert_004",
                title = "Station Elevator Out of Service",
                description = "The elevator at Monastiraki station is temporarily out of service. Use escalators or stairs.",
                severity = AlertSeverity.MEDIUM,
                affectedLines = listOf("Line 1", "Line 3"),
                affectedStations = listOf("Monastiraki"),
                startTime = Date(currentTime.time - 7200000),
                endTime = null,
                isActive = true,
                category = AlertCategory.MAINTENANCE
            )
        )
    }
    
    private fun generateDemoVehicles(
        type: VehicleType,
        bounds: Pair<LatLng, LatLng>?
    ): List<LiveVehicle> {
        val vehicles = mutableListOf<LiveVehicle>()
        val random = Random()
        
        val athensCenter = LatLng(37.9838, 23.7275)
        val defaultBounds = Pair(
            LatLng(37.9500, 23.7000),
            LatLng(38.0200, 23.7600)
        )
        val actualBounds = bounds ?: defaultBounds
        
        val vehicleCount = when (type) {
            VehicleType.BUS -> 15
            VehicleType.METRO -> 8
            VehicleType.TRAM -> 6
            VehicleType.TROLLEY -> 4
        }
        
        repeat(vehicleCount) { index ->
            val lat = actualBounds.first.latitude + 
                      random.nextDouble() * (actualBounds.second.latitude - actualBounds.first.latitude)
            val lng = actualBounds.first.longitude + 
                      random.nextDouble() * (actualBounds.second.longitude - actualBounds.first.longitude)
            
            val lines = when (type) {
                VehicleType.BUS -> listOf("040", "550", "X95", "A1", "B2", "E22")
                VehicleType.METRO -> listOf("M1", "M2", "M3")
                VehicleType.TRAM -> listOf("T1", "T2", "T3")
                VehicleType.TROLLEY -> listOf("1", "3", "7", "11")
            }
            
            vehicles.add(
                LiveVehicle(
                    id = "${type.name.lowercase()}_${index + 1}",
                    line = lines.random(),
                    vehicleType = type,
                    currentLocation = LatLng(lat, lng),
                    direction = if (random.nextBoolean()) "Northbound" else "Southbound",
                    nextStopId = "stop_${random.nextInt(100)}",
                    nextStopName = generateRandomStopName(),
                    estimatedArrival = "${random.nextInt(15) + 1} min",
                    delayMinutes = random.nextInt(10),
                    occupancyLevel = OccupancyLevel.values().random(),
                    isAccessible = random.nextBoolean()
                )
            )
        }
        
        return vehicles
    }
    
    private fun generateStationStatus(station: MetroStation): StationStatus {
        val random = Random()
        
        return StationStatus(
            stationId = station.nameEnglish.replace(" ", "_").lowercase(),
            stationName = station.nameEnglish,
            stationType = VehicleType.METRO,
            isOperational = random.nextFloat() > 0.1,
            elevatorStatus = FacilityStatus(
                isWorking = random.nextFloat() > 0.2,
                maintenanceNote = if (random.nextFloat() < 0.1) "Scheduled maintenance" else null,
                estimatedRepairTime = if (random.nextFloat() < 0.05) 
                    Date(System.currentTimeMillis() + random.nextInt(86400000)) else null
            ),
            escalatorStatus = FacilityStatus(
                isWorking = random.nextFloat() > 0.15,
                maintenanceNote = null,
                estimatedRepairTime = null
            ),
            accessibilityStatus = AccessibilityStatus(
                wheelchairAccessible = random.nextFloat() > 0.2,
                audioAnnouncements = random.nextFloat() > 0.1,
                visualDisplays = random.nextFloat() > 0.05,
                tactileGuidance = random.nextFloat() > 0.3
            ),
            alertsCount = random.nextInt(3),
            lastUpdated = Date()
        )
    }
    
    private fun generateRandomStopName(): String {
        val stops = listOf(
            "Syntagma", "Omonia", "Monastiraki", "Thissio", "Piraeus",
            "Kifissia", "Marousi", "Chalandri", "Ambelokipi", "Megaro Moussikis",
            "Evangelismos", "Akropoli", "Syngrou-Fix", "Neos Kosmos", "Elliniko"
        )
        return stops.random()
    }
    
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0].toDouble()
    }
    
    fun getAlertIcon(severity: AlertSeverity): String {
        return when (severity) {
            AlertSeverity.LOW -> "ℹ️"
            AlertSeverity.MEDIUM -> "⚠️"
            AlertSeverity.HIGH -> "🚨"
            AlertSeverity.CRITICAL -> "🔴"
        }
    }
    
    fun getVehicleIcon(vehicleType: VehicleType): String {
        return when (vehicleType) {
            VehicleType.BUS -> "🚌"
            VehicleType.METRO -> "🚇"
            VehicleType.TRAM -> "🚊"
            VehicleType.TROLLEY -> "🚎"
        }
    }
    
    fun getOccupancyIcon(occupancy: OccupancyLevel): String {
        return when (occupancy) {
            OccupancyLevel.EMPTY -> "🟢"
            OccupancyLevel.LOW -> "🟡"
            OccupancyLevel.MEDIUM -> "🟠"
            OccupancyLevel.HIGH -> "🔴"
            OccupancyLevel.FULL -> "⛔"
        }
    }
} 