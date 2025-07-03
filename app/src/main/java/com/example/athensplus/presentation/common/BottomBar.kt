package com.example.athensplus.presentation.common

import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.athensplus.R
import com.example.athensplus.presentation.info.InfoFragment
import com.example.athensplus.presentation.transport.TransportFragment
import com.example.athensplus.presentation.explore.ExploreFragment

class BottomBar(private val bottomNavigation: BottomNavigationView, private val activity: FragmentActivity) {
    init {
        setupBottomNavigation()
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TransportFragment())
            .commit()
        bottomNavigation.selectedItemId = R.id.navigation_transport
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_transport -> {
                    activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, TransportFragment())
                        .commit()
                    true
                }
                R.id.navigation_explore -> {
                    activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ExploreFragment())
                        .commit()
                    true
                }
                R.id.navigation_info -> {
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