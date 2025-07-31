package com.example.athensplus.presentation.transport.routes

import androidx.fragment.app.Fragment
import com.example.athensplus.databinding.FragmentTransportBinding

class RouteFindingManager(
    private val fragment: Fragment,
    private val binding: FragmentTransportBinding
) {
    
    fun findRoute() {
        val toText = binding.editTo.text.toString().trim()
        if (toText.isNotEmpty()) {
            // todo
        }
    }
} 