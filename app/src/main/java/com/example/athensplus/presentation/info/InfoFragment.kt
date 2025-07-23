package com.example.athensplus.presentation.info

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.utils.*
import com.example.athensplus.databinding.FragmentInfoBinding
import kotlinx.coroutines.launch

class InfoFragment : Fragment() {
    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var settingsService: SettingsService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        
        initializeServices()
        setupMenuItems()
        
        return binding.root
    }
    
    private fun initializeServices() {
        try {
            settingsService = SettingsService(requireContext())
        } catch (e: Exception) {
            // to do
        }
    }
    
    private fun setupMenuItems() {
        setupMyAccountItems()

        binding.themeButton.setOnClickListener {
            showThemeSettings()
        }
        
        binding.languageButton.setOnClickListener {
            showLanguageSettings()
        }
        
        binding.notificationButton.setOnClickListener {
            showNotificationSettings()
        }
        
        binding.logoutButton.setOnClickListener {
            showToast("Log Out - Coming Soon")
        }
    }
    
    private fun setupMyAccountItems() {
        binding.myRoutesButton.setOnClickListener {
            showToast("My Routes - Coming Soon")
        }
        
        binding.savedPlacesButton.setOnClickListener {
            showToast("Saved Places - Coming Soon")
        }
        
        binding.recentRoutesButton.setOnClickListener {
            showToast("Recent Routes - Coming Soon")
        }
        
        binding.personalInfoButton.setOnClickListener {
            showToast("Personal Info - Coming Soon")
        }
        
        binding.subscriptionButton.setOnClickListener {
            showToast("Subscription - Coming Soon")
        }
    }
    

    
    private fun showThemeSettings() {
        if (!::settingsService.isInitialized) {
            showToast("Settings not available")
            return
        }
        val themes = settingsService.getAllThemes()
        val currentTheme = settingsService.getTheme()
        
        showSelectionDialog(
            title = "Choose Theme",
            options = themes.map { settingsService.getThemeDisplayName(it) },
            selectedIndex = themes.indexOf(currentTheme)
        ) { selectedIndex ->
            lifecycleScope.launch {
                settingsService.updateTheme(themes[selectedIndex])
            }
        }
    }
    
    private fun showLanguageSettings() {
        if (!::settingsService.isInitialized) {
            showToast("Settings not available")
            return
        }
        val languages = settingsService.getAllLanguages()
        val currentLanguage = settingsService.getLanguage()
        
        showSelectionDialog(
            title = "Choose Language",
            options = languages.map { settingsService.getLanguageDisplayName(it) },
            selectedIndex = languages.indexOf(currentLanguage)
        ) { selectedIndex ->
            lifecycleScope.launch {
                settingsService.updateLanguage(languages[selectedIndex])
            }
        }
    }
    
    private fun showNotificationSettings() {
        if (!::settingsService.isInitialized) {
            showToast("Settings not available")
            return
        }
        
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timetable, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val titleGreek = view.findViewById<TextView>(R.id.station_name_greek)
        val titleEnglish = view.findViewById<TextView>(R.id.station_name_english)
        val container = view.findViewById<LinearLayout>(R.id.timetable_container)
        val closeButton = view.findViewById<ImageView>(R.id.close_button)
        
        titleGreek.text = "Notifications"
        titleEnglish.text = "Alert Preferences"
        
        container.removeAllViews()
        
        val settings = settingsService.getCurrentSettings().notificationSettings
        
        container.addView(createToggleItem("Service Alerts", settings.serviceAlertsEnabled) {
            lifecycleScope.launch { settingsService.toggleServiceAlerts() }
        })
        
        container.addView(createToggleItem("Trip Reminders", settings.tripRemindersEnabled) {
            lifecycleScope.launch { settingsService.toggleTripReminders() }
        })
        
        container.addView(createToggleItem("Arrival Notifications", settings.arrivalNotificationsEnabled) {
            lifecycleScope.launch { settingsService.toggleArrivalNotifications() }
        })
        
        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    

    

    

    
    private fun createToggleItem(
        title: String, 
        isChecked: Boolean, 
        onToggle: () -> Unit
    ): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 12, 24, 12)
            gravity = android.view.Gravity.CENTER_VERTICAL
            
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 16f
                setTextColor(Color.parseColor("#663399"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            
            addView(Switch(requireContext()).apply {
                this.isChecked = isChecked
                setOnCheckedChangeListener { _, _ -> onToggle() }
            })
        }
    }
    
    private fun showSelectionDialog(
        title: String,
        options: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
            onSelected(which)
            dialog.dismiss()
        }
        builder.show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 