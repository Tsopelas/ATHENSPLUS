package com.example.athensplus.core.utils

import android.content.Context
import android.content.Intent
import com.example.athensplus.domain.model.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class TripPlanningService(
    private val context: Context,
    private val directionsService: DirectionsService,
    private val routeService: RouteService
) {
    
    suspend fun createMultiStopTrip(
        name: String,
        stops: List<TripStop>
    ): MultiStopTrip = withContext(Dispatchers.IO) {
        val optimizedStops = optimizeStopOrder(stops)
        val totalMetrics = calculateTripMetrics(optimizedStops)
        
        MultiStopTrip(
            id = UUID.randomUUID().toString(),
            name = name,
            stops = optimizedStops,
            totalDuration = totalMetrics.first,
            totalDistance = totalMetrics.second,
            estimatedCost = calculateEstimatedCost(optimizedStops),
            createdAt = Date(),
            optimized = true
        )
    }
    
    suspend fun scheduleTrip(
        fromAddress: String,
        toAddress: String,
        fromLocation: LatLng,
        toLocation: LatLng,
        departureTime: Date,
        reminderMinutes: Int,
        isRecurring: Boolean,
        recurringDays: List<DayOfWeek>
    ): ScheduledTrip = withContext(Dispatchers.IO) {
        val route = directionsService.findDirectionsToDestination(
            toAddress, fromLocation, "Metro"
        )
        
        val estimatedDuration = route?.totalDuration?.let { duration ->
            extractMinutesFromDuration(duration)
        } ?: 30
        
        val arrivalTime = Date(departureTime.time + (estimatedDuration * 60 * 1000))
        
        ScheduledTrip(
            id = UUID.randomUUID().toString(),
            fromAddress = fromAddress,
            toAddress = toAddress,
            fromLocation = fromLocation,
            toLocation = toLocation,
            scheduledDepartureTime = departureTime,
            scheduledArrivalTime = arrivalTime,
            reminderMinutes = reminderMinutes,
            isRecurring = isRecurring,
            recurringDays = recurringDays,
            route = route,
            isActive = true
        )
    }
    
    fun shareRoute(route: DirectionsResult, method: ShareMethod) {
        val shareText = buildRouteShareText(route)
        
        when (method) {
            ShareMethod.WHATSAPP -> shareViaWhatsApp(shareText)
            ShareMethod.SMS -> shareViaSMS(shareText)
            ShareMethod.EMAIL -> shareViaEmail(shareText)
            ShareMethod.GENERIC -> shareViaGeneric(shareText)
        }
    }
    
    suspend fun compareRoutes(
        fromLocation: LatLng,
        toLocation: LatLng,
        destinationQuery: String
    ): RouteComparison = withContext(Dispatchers.IO) {
        val routes = mutableListOf<DirectionsResult>()
        
        val metroRoute = directionsService.findDirectionsToDestination(
            destinationQuery, fromLocation, "Metro"
        )
        metroRoute?.let { routes.add(it) }
        
        val busRoute = directionsService.findDirectionsToDestination(
            destinationQuery, fromLocation, "Bus Stops"
        )
        busRoute?.let { routes.add(it) }
        
        val tramRoute = directionsService.findDirectionsToDestination(
            destinationQuery, fromLocation, "Tram"
        )
        tramRoute?.let { routes.add(it) }
        
        val metrics = calculateComparisonMetrics(routes)
        val recommendation = generateRecommendation(routes, metrics)
        
        RouteComparison(
            routes = routes,
            comparisonMetrics = metrics,
            recommendation = recommendation
        )
    }
    
    private suspend fun optimizeStopOrder(stops: List<TripStop>): List<TripStop> {
        if (stops.size <= 2) return stops
        
        val origin = stops.first()
        val destination = stops.last()
        val intermediateStops = stops.drop(1).dropLast(1)
        
        val optimizedIntermediate = intermediateStops.sortedBy { stop ->
            calculateDistance(origin.location, stop.location)
        }
        
        return listOf(origin) + optimizedIntermediate + destination
    }
    
    private suspend fun calculateTripMetrics(stops: List<TripStop>): Pair<String, String> {
        var totalDuration = 0
        var totalDistance = 0.0
        
        for (i in 0 until stops.size - 1) {
            val from = stops[i]
            val to = stops[i + 1]
            
            val route = directionsService.findDirectionsToDestination(
                to.address, from.location, "Metro"
            )
            
            route?.let {
                totalDuration += extractMinutesFromDuration(it.totalDuration)
                totalDistance += extractKilometersFromDistance(it.totalDistance)
            }
        }
        
        return "${totalDuration} min" to "${"%.1f".format(totalDistance)} km"
    }
    
    private fun calculateEstimatedCost(stops: List<TripStop>): String {
        val numberOfLegs = stops.size - 1
        val baseFare = 1.40
        val transferFee = 0.60
        
        val totalCost = baseFare + (transferFee * (numberOfLegs - 1))
        return "${"%.2f".format(totalCost)}€"
    }
    
    private fun calculateComparisonMetrics(routes: List<DirectionsResult>): ComparisonMetrics {
        val fastestRoute = routes.minByOrNull { 
            extractMinutesFromDuration(it.totalDuration) 
        } ?: routes.first()
        
        val shortestRoute = routes.minByOrNull { 
            extractKilometersFromDistance(it.totalDistance) 
        } ?: routes.first()
        
        val leastTransfersRoute = routes.minByOrNull { 
            it.steps.count { step -> step.mode == "TRANSIT" } 
        } ?: routes.first()
        
        val cheapestRoute = routes.minByOrNull { route ->
            route.steps.count { step -> step.mode == "TRANSIT" }
        } ?: routes.first()
        
        return ComparisonMetrics(
            fastestRoute = fastestRoute,
            shortestRoute = shortestRoute,
            leastTransfersRoute = leastTransfersRoute,
            cheapestRoute = cheapestRoute
        )
    }
    
    private fun generateRecommendation(
        routes: List<DirectionsResult>,
        metrics: ComparisonMetrics
    ): RouteRecommendation {
        val fastestDuration = extractMinutesFromDuration(metrics.fastestRoute.totalDuration)
        val shortestDistance = extractKilometersFromDistance(metrics.shortestRoute.totalDistance)
        
        val recommendedRoute = when {
            fastestDuration <= 20 -> metrics.fastestRoute
            shortestDistance <= 5.0 -> metrics.shortestRoute
            else -> metrics.leastTransfersRoute
        }
        
        val reason = when (recommendedRoute) {
            metrics.fastestRoute -> "Fastest route with minimal travel time"
            metrics.shortestRoute -> "Shortest distance for walking comfort"
            metrics.leastTransfersRoute -> "Fewest transfers for convenience"
            else -> "Balanced option considering all factors"
        }
        
        val score = calculateRouteScore(recommendedRoute)
        
        return RouteRecommendation(
            recommendedRoute = recommendedRoute,
            reason = reason,
            score = score
        )
    }
    
    private fun calculateRouteScore(route: DirectionsResult): Float {
        val duration = extractMinutesFromDuration(route.totalDuration).toFloat()
        val transfers = route.steps.count { it.mode == "TRANSIT" }.toFloat()
        val distance = extractKilometersFromDistance(route.totalDistance).toFloat()
        
        val durationScore = kotlin.math.max(0f, 100f - (duration * 2f))
        val transferScore = kotlin.math.max(0f, 100f - (transfers * 20f))
        val distanceScore = kotlin.math.max(0f, 100f - (distance * 10f))
        
        return (durationScore + transferScore + distanceScore) / 3f
    }
    
    private fun buildRouteShareText(route: DirectionsResult): String {
        return buildString {
            appendLine("🚇 Athens Plus Route")
            appendLine("Duration: ${route.totalDuration}")
            appendLine("Distance: ${route.totalDistance}")
            appendLine()
            appendLine("Steps:")
            route.steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. ${step.instruction}")
            }
            appendLine()
            appendLine("Shared via Athens Plus App")
        }
    }
    
    private fun shareViaWhatsApp(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(intent, "Share via WhatsApp"))
        } else {
            shareViaGeneric(text)
        }
    }
    
    private fun shareViaSMS(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra("sms_body", text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via SMS"))
    }
    
    private fun shareViaEmail(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Athens Transport Route")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via Email"))
    }
    
    private fun shareViaGeneric(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Route"))
    }
    
    private fun extractMinutesFromDuration(duration: String): Int {
        return duration.replace(" min", "").replace(" mins", "")
            .replace("hour", "60").replace("hours", "60")
            .filter { it.isDigit() || it == ' ' }
            .split(" ")
            .mapNotNull { it.toIntOrNull() }
            .sum()
    }
    
    private fun extractKilometersFromDistance(distance: String): Double {
        return when {
            distance.contains("km") -> {
                distance.replace(" km", "").replace(",", ".").toDoubleOrNull() ?: 0.0
            }
            distance.contains("m") -> {
                val meters = distance.replace(" m", "").replace(",", "").toDoubleOrNull() ?: 0.0
                meters / 1000.0
            }
            else -> 0.0
        }
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
}

enum class ShareMethod {
    WHATSAPP, SMS, EMAIL, GENERIC
} 