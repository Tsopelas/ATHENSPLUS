package com.example.athensplus.ui.bottombar

import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.athensplus.R
import com.example.athensplus.ui.info.InfoFragment
import com.example.athensplus.ui.transport.TransportFragment
import com.example.athensplus.ui.explore.ExploreFragment

class BottomBar(private val bottomNavigation: BottomNavigationView, private val activity: FragmentActivity) {
    init {
        setupBottomNavigation()
        // Set initial fragment to Transport
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TransportFragment())
            .commit()
        // Set the transport item as selected
        bottomNavigation.selectedItemId = R.id.navigation_transport
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_transport -> {
                    // Navigate to transport fragment
                    activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, TransportFragment())
                        .commit()
                    true
                }
                R.id.navigation_explore -> {
                    // Navigate to explore fragment
                    activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ExploreFragment())
                        .commit()
                    true
                }
                R.id.navigation_info -> {
                    // Navigate to info fragment
                    activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, InfoFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
} 