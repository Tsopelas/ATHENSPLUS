package com.example.athensplus.presentation.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.athensplus.R
import com.example.athensplus.domain.model.AddressSuggestion

class AddressSuggestionAdapter(
    private val suggestions: List<AddressSuggestion>,
    private val onSuggestionClick: (AddressSuggestion) -> Unit
) : RecyclerView.Adapter<AddressSuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val addressText: TextView = view.findViewById(R.id.address_text)
        val descriptionText: TextView = view.findViewById(R.id.description_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        
        // Format the address display
        val formattedAddress = formatAddressForDisplay(suggestion)
        holder.addressText.text = formattedAddress
        
        // Show additional context in description
        val contextInfo = buildContextInfo(suggestion)
        holder.descriptionText.text = contextInfo
        
        holder.itemView.setOnClickListener {
            onSuggestionClick(suggestion)
        }
    }

    override fun getItemCount() = suggestions.size
    
    private fun formatAddressForDisplay(suggestion: AddressSuggestion): String {
        val parts = mutableListOf<String>()
        
        // For establishments, show the establishment name first
        if (suggestion.establishmentName != null) {
            parts.add(suggestion.establishmentName)
        }
        
        // Then add street name and number
        if (suggestion.streetName != null) {
            val streetPart = if (suggestion.streetNumber != null) {
                "${suggestion.streetName} ${suggestion.streetNumber}"
            } else {
                suggestion.streetName
            }
            parts.add(streetPart)
        }
        
        // Add area and postal code if available
        if (suggestion.area != null) {
            val areaPart = if (suggestion.postalCode != null) {
                "${suggestion.area} ${suggestion.postalCode}"
            } else {
                suggestion.area
            }
            parts.add(areaPart)
        }
        
        return parts.joinToString(", ")
    }
    
    private fun buildContextInfo(suggestion: AddressSuggestion): String {
        val contextParts = mutableListOf<String>()
        
        // Add establishment type if available
        if (suggestion.establishmentType != null) {
            contextParts.add(suggestion.establishmentType)
        }
        
        // Add area context if different from main display
        if (suggestion.area != null && suggestion.area != "Athens") {
            if (suggestion.postalCode != null) {
                contextParts.add("${suggestion.area} ${suggestion.postalCode}")
            } else {
                contextParts.add(suggestion.area)
            }
        }
        
        // Add additional context from description if it contains useful info
        val description = suggestion.description
        if (description.contains("Athens, Greece") && !contextParts.contains("Athens")) {
            contextParts.add("Athens")
        }
        
        return if (contextParts.isNotEmpty()) {
            contextParts.joinToString(", ")
        } else {
            "Athens, Greece"
        }
    }
} 