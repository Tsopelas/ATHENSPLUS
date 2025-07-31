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
        try {
            _binding = FragmentInfoBinding.inflate(inflater, container, false)
            
            initializeServices()
            setupMenuItems()
            
            return binding.root
        } catch (e: Exception) {
            // Fallback to a simple view if binding fails
            val textView = TextView(requireContext()).apply {
                text = getString(R.string.info_fragment_error_loading, e.message ?: "Unknown error")
                setPadding(32, 32, 32, 32)
            }
            return textView
        }
    }
    
    private fun initializeServices() {
        try {
            settingsService = SettingsService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupMenuItems() {
        try {
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
                showToast(getString(R.string.log_out_coming_soon))
            }
        } catch (e: Exception) {
            // Log the error but don't crash
            e.printStackTrace()
        }
    }
    
    private fun setupMyAccountItems() {
        try {
            binding.myRoutesButton.setOnClickListener {
                showToast(getString(R.string.my_routes_coming_soon))
            }
            
            binding.savedPlacesButton.setOnClickListener {
                showToast(getString(R.string.saved_places_coming_soon))
            }
            
            binding.recentRoutesButton.setOnClickListener {
                showToast(getString(R.string.recent_routes_coming_soon))
            }
            
            binding.personalInfoButton.setOnClickListener {
                showToast(getString(R.string.personal_info_coming_soon))
            }
            
            binding.subscriptionButton.setOnClickListener {
                showToast(getString(R.string.subscription_coming_soon))
            }
        } catch (e: Exception) {
            // Log the error but don't crash
            e.printStackTrace()
        }
    }
    

    
    private fun showThemeSettings() {
        try {
            if (!::settingsService.isInitialized) {
                showToast(getString(R.string.settings_not_available))
                return
            }
            val themes = settingsService.getAllThemes()
            val currentTheme = settingsService.getTheme()
            
            showSelectionDialog(
                title = getString(R.string.choose_theme),
                options = themes.map { settingsService.getThemeDisplayName(it) },
                selectedIndex = themes.indexOf(currentTheme)
            ) { selectedIndex ->
                lifecycleScope.launch {
                    try {
                        settingsService.updateTheme(themes[selectedIndex])
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(getString(R.string.error_showing_theme_settings, e.message ?: "Unknown error"))
        }
    }
    
    private fun showLanguageSettings() {
        try {
            if (!::settingsService.isInitialized) {
                showToast(getString(R.string.settings_not_available))
                return
            }
            val languages = settingsService.getAllLanguages()
            val currentLanguage = settingsService.getLanguage()
            
            showSelectionDialog(
                title = getString(R.string.choose_language),
                options = languages.map { settingsService.getLanguageDisplayName(it) },
                selectedIndex = languages.indexOf(currentLanguage)
            ) { selectedIndex ->
                lifecycleScope.launch {
                    try {
                        settingsService.updateLanguage(languages[selectedIndex])
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(getString(R.string.error_showing_language_settings, e.message ?: "Unknown error"))
        }
    }
    
    private fun showNotificationSettings() {
        try {
            if (!::settingsService.isInitialized) {
                showToast(getString(R.string.settings_not_available))
                return
            }
            
            val dialog = Dialog(requireContext())
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timetable, dialog.findViewById(android.R.id.content), false)
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
            
            titleGreek.text = getString(R.string.notifications)
            titleEnglish.text = getString(R.string.alert_preferences)
            
            container.removeAllViews()
            
            val settings = settingsService.getCurrentSettings().notificationSettings
            
            container.addView(createToggleItem(getString(R.string.service_alerts), settings.serviceAlertsEnabled) {
                lifecycleScope.launch { 
                    try {
                        settingsService.toggleServiceAlerts() 
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            
            container.addView(createToggleItem(getString(R.string.trip_reminders), settings.tripRemindersEnabled) {
                lifecycleScope.launch { 
                    try {
                        settingsService.toggleTripReminders() 
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            
            container.addView(createToggleItem(getString(R.string.arrival_notifications), settings.arrivalNotificationsEnabled) {
                lifecycleScope.launch { 
                    try {
                        settingsService.toggleArrivalNotifications() 
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            
            closeButton.setOnClickListener { dialog.dismiss() }
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(getString(R.string.error_showing_notification_settings, e.message ?: "Unknown error"))
        }
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