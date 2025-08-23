package com.example.athensplus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.athensplus.databinding.ActivityMainBinding
import com.example.athensplus.presentation.common.BottomBar
import com.example.athensplus.core.ui.ThemeUtils
import com.example.athensplus.core.utils.SettingsService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply persisted theme before setting content view
        ThemeUtils.applyTheme(SettingsService(this).getTheme())
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preserve the selected tab across theme changes
        val savedTabId = savedInstanceState?.getInt(KEY_SELECTED_TAB, R.id.navigation_transport) 
            ?: R.id.navigation_transport
        
        BottomBar(binding.bottomNavigation, this, savedTabId)
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save currently selected tab
        outState.putInt(KEY_SELECTED_TAB, binding.bottomNavigation.selectedItemId)
    }
    
    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }
}