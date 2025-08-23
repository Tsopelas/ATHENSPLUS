package com.example.athensplus.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.athensplus.domain.model.SavedRoute
import com.example.athensplus.domain.model.TransitStep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.UUID

class SavedRoutesService(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("saved_routes", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveRoute(fromLocation: String, toLocation: String, routeSummary: String, totalDuration: String, steps: List<TransitStep>): String {
        val routeId = UUID.randomUUID().toString()
        val savedRoute = SavedRoute(
            id = routeId,
            fromLocation = fromLocation,
            toLocation = toLocation,
            routeSummary = routeSummary,
            totalDuration = totalDuration,
            steps = steps,
            savedAt = Date()
        )
        
        val routes = getAllRoutes().toMutableList()
        routes.add(0, savedRoute)
        
        val routesJson = gson.toJson(routes)
        sharedPreferences.edit().putString("routes", routesJson).commit()
        
        return routeId
    }
    
    fun getAllRoutes(): List<SavedRoute> {
        val routesJson = sharedPreferences.getString("routes", "[]")
        val type = object : TypeToken<List<SavedRoute>>() {}.type
        return try {
            gson.fromJson(routesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun deleteRoute(routeId: String): Boolean {
        val routes = getAllRoutes().toMutableList()
        val removed = routes.removeAll { it.id == routeId }
        if (removed) {
            val routesJson = gson.toJson(routes)
            sharedPreferences.edit().putString("routes", routesJson).commit()
        }
        return removed
    }
    
    fun toggleFavorite(routeId: String): Boolean {
        val routes = getAllRoutes().toMutableList()
        val routeIndex = routes.indexOfFirst { it.id == routeId }
        if (routeIndex != -1) {
            val route = routes[routeIndex]
            val updatedRoute = route.copy(isFavorite = !route.isFavorite)
            routes[routeIndex] = updatedRoute
            val routesJson = gson.toJson(routes)
            sharedPreferences.edit().putString("routes", routesJson).commit()
            return updatedRoute.isFavorite
        }
        return false
    }
    
    fun isRouteSaved(fromLocation: String, toLocation: String): Boolean {
        return getAllRoutes().any { 
            it.fromLocation.equals(fromLocation, ignoreCase = true) && 
            it.toLocation.equals(toLocation, ignoreCase = true) 
        }
    }
    
    fun getRouteById(routeId: String): SavedRoute? {
        return getAllRoutes().find { it.id == routeId }
    }
}
