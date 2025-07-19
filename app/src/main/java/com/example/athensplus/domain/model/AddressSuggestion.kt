package com.example.athensplus.domain.model

data class AddressSuggestion(
    val id: String,
    val address: String,
    val description: String,
    val placeId: String? = null,
    val latLng: com.google.android.gms.maps.model.LatLng? = null,
    val area: String? = null,
    val streetName: String? = null,
    val streetNumber: String? = null,
    val postalCode: String? = null,
    val establishmentName: String? = null,
    val establishmentType: String? = null
) 