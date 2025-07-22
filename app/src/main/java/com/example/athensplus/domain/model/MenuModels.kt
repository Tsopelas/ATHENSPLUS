package com.example.athensplus.domain.model

import com.google.android.gms.maps.model.LatLng
import java.util.Date

data class MultiStopTrip(
    val id: String,
    val name: String,
    val stops: List<TripStop>,
    val totalDuration: String,
    val totalDistance: String,
    val estimatedCost: String,
    val createdAt: Date,
    val optimized: Boolean = false
)

data class TripStop(
    val id: String,
    val location: LatLng,
    val address: String,
    val arrivalTime: String?,
    val departureTime: String?,
    val stopDuration: Int = 0,
    val notes: String = ""
)

data class ScheduledTrip(
    val id: String,
    val fromAddress: String,
    val toAddress: String,
    val fromLocation: LatLng,
    val toLocation: LatLng,
    val scheduledDepartureTime: Date,
    val scheduledArrivalTime: Date,
    val reminderMinutes: Int,
    val isRecurring: Boolean,
    val recurringDays: List<DayOfWeek>,
    val route: DirectionsResult?,
    val isActive: Boolean
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

data class RouteComparison(
    val routes: List<DirectionsResult>,
    val comparisonMetrics: ComparisonMetrics,
    val recommendation: RouteRecommendation
)

data class ComparisonMetrics(
    val fastestRoute: DirectionsResult,
    val shortestRoute: DirectionsResult,
    val leastTransfersRoute: DirectionsResult,
    val cheapestRoute: DirectionsResult
)

data class RouteRecommendation(
    val recommendedRoute: DirectionsResult,
    val reason: String,
    val score: Float
)

data class ServiceAlert(
    val id: String,
    val title: String,
    val description: String,
    val severity: AlertSeverity,
    val affectedLines: List<String>,
    val affectedStations: List<String>,
    val startTime: Date,
    val endTime: Date?,
    val isActive: Boolean,
    val category: AlertCategory
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class AlertCategory {
    DELAY, CLOSURE, MAINTENANCE, STRIKE, WEATHER, EMERGENCY, INFORMATION
}

data class LiveVehicle(
    val id: String,
    val line: String,
    val vehicleType: VehicleType,
    val currentLocation: LatLng,
    val direction: String,
    val nextStopId: String,
    val nextStopName: String,
    val estimatedArrival: String,
    val delayMinutes: Int,
    val occupancyLevel: OccupancyLevel,
    val isAccessible: Boolean
)

enum class VehicleType {
    BUS, METRO, TRAM, TROLLEY
}

enum class OccupancyLevel {
    EMPTY, LOW, MEDIUM, HIGH, FULL
}

data class StationStatus(
    val stationId: String,
    val stationName: String,
    val stationType: VehicleType,
    val isOperational: Boolean,
    val elevatorStatus: FacilityStatus,
    val escalatorStatus: FacilityStatus,
    val accessibilityStatus: AccessibilityStatus,
    val alertsCount: Int,
    val lastUpdated: Date
)

data class FacilityStatus(
    val isWorking: Boolean,
    val maintenanceNote: String?,
    val estimatedRepairTime: Date?
)

data class AccessibilityStatus(
    val wheelchairAccessible: Boolean,
    val audioAnnouncements: Boolean,
    val visualDisplays: Boolean,
    val tactileGuidance: Boolean
)

data class AppSettings(
    val theme: AppTheme,
    val language: Language,
    val notificationSettings: NotificationSettings,
    val mapSettings: MapSettings
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM_DEFAULT
}

enum class Language {
    ENGLISH, GREEK
}

data class NotificationSettings(
    val serviceAlertsEnabled: Boolean,
    val tripRemindersEnabled: Boolean,
    val arrivalNotificationsEnabled: Boolean,
    val emergencyAlertsEnabled: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStart: String,
    val quietHoursEnd: String
)

data class MapSettings(
    val mapStyle: MapStyle,
    val showTrafficLayer: Boolean,
    val showAccessibilityInfo: Boolean,
    val showRealTimeVehicles: Boolean,
    val defaultZoomLevel: Float
)

enum class MapStyle {
    NORMAL, SATELLITE, TERRAIN, HYBRID
} 