package com.example.athensplus.presentation.common

import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.athensplus.R
import com.example.athensplus.presentation.info.InfoFragment
import com.example.athensplus.presentation.transport.TransportFragment
import com.example.athensplus.presentation.explore.ExploreFragment
import android.view.animation.AccelerateInterpolator
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class BottomBar(
    private val bottomNavigation: BottomNavigationView, 
    private val activity: FragmentActivity,
    private val initialTabId: Int = R.id.navigation_transport
) {
    init {
        setupBottomNavigation()
        
        // Load the appropriate initial fragment based on saved state
        val initialFragment = when (initialTabId) {
            R.id.navigation_info -> InfoFragment()
            R.id.navigation_explore -> ExploreFragment()
            else -> TransportFragment()
        }
        
        // Initial load with fade in animation
        activity.supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in_fast, 0)
            .replace(R.id.fragment_container, initialFragment)
            .commit()
        bottomNavigation.selectedItemId = initialTabId
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            // Animate the clicked tab
            animateTabClick(item.itemId)
            
            when (item.itemId) {
                R.id.navigation_transport -> {
                    animateTabTransition(TransportFragment())
                    true
                }
                R.id.navigation_explore -> {
                    animateTabTransition(ExploreFragment())
                    true
                }
                R.id.navigation_info -> {
                    animateTabTransition(InfoFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun animateTabTransition(fragment: androidx.fragment.app.Fragment) {
        // Industry standard: fade out current, then fade in new
        val fragmentManager = activity.supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        
        if (currentFragment != null) {
            // Step 1: Fade out current fragment
            currentFragment.view?.animate()
                ?.alpha(0f)
                ?.setDuration(60) // Ultra fast fade out
                ?.setInterpolator(android.view.animation.AccelerateInterpolator())
                ?.withEndAction {
                    // Step 2: Replace and fade in new fragment
                    fragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.fade_in_fast,    // Fast fade in for new fragment
                            0,                      // No exit animation (already handled)
                            R.anim.fade_in_fast,    // Pop enter
                            0                       // Pop exit
                        )
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                }
                ?.start()
        } else {
            // First load - just fade in
            fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in_fast, 0)
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
    
    private fun animateTabClick(selectedItemId: Int) {
        try {
            // Find only the selected tab and animate it
            for (i in 0 until bottomNavigation.menu.size()) {
                val menuItem = bottomNavigation.menu.getItem(i)
                
                if (menuItem.itemId == selectedItemId) {
                    val tabView = findTabView(i)
                    // Only animate the clicked tab with iOS-style bounce
                    animateTabZoom(tabView)
                    break
                }
            }
        } catch (e: Exception) {
            // Silent fail for smooth UX
        }
    }
    
    private fun findTabView(position: Int): View? {
        try {
            // BottomNavigationView structure: BottomNavigationMenuView -> BottomNavigationItemView
            val menuView = bottomNavigation.getChildAt(0) as? ViewGroup
            return menuView?.getChildAt(position)
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun animateTabZoom(tabView: View?) {
        tabView?.let { view ->
            // Find icon and text within the tab
            val iconView = findViewByType(view, ImageView::class.java)
            val textView = findViewByType(view, TextView::class.java)
            
            // Soft, subtle bounce animation for icon
            iconView?.animate()
                ?.scaleX(1.08f)
                ?.scaleY(1.08f)
                ?.setDuration(100)
                ?.setInterpolator(android.view.animation.PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f))
                ?.withEndAction {
                    iconView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f))
                        .start()
                }
                ?.start()
            
            // Soft, subtle bounce animation for text with slight delay
            textView?.animate()
                ?.scaleX(1.06f)
                ?.scaleY(1.06f)
                ?.setDuration(100)
                ?.setStartDelay(15)
                ?.setInterpolator(android.view.animation.PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f))
                ?.withEndAction {
                    textView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f))
                        .start()
                }
                ?.start()
        }
    }
    
    private fun <T : View> findViewByType(parent: View, targetClass: Class<T>): T? {
        if (targetClass.isInstance(parent)) {
            @Suppress("UNCHECKED_CAST")
            return parent as T
        }
        if (parent is ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val result = findViewByType(child, targetClass)
                if (result != null) return result
            }
        }
        return null
    }
} 