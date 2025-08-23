package com.example.athensplus.domain.model

import java.util.Date

data class SavedRoute(
    val id: String = "",
    val fromLocation: String,
    val toLocation: String,
    val routeSummary: String,
    val totalDuration: String,
    val steps: List<TransitStep>,
    val savedAt: Date = Date(),
    val isFavorite: Boolean = false
)
