package com.example.athensplus.presentation.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.core.ui.ThemeUtils
import com.example.athensplus.core.utils.SettingsService
import com.example.athensplus.domain.model.AppTheme
import kotlinx.coroutines.launch

class InfoFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val themeButton = view.findViewById<LinearLayout>(R.id.theme_button)
        themeButton.setOnClickListener { showThemePicker() }
    }

    private fun showThemePicker() {
        val context = requireContext()
        val settings = SettingsService(context)

        // Inflate custom dialog
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_theme_picker, null)
        
        // Get theme cards
        val lightCard = dialogView.findViewById<LinearLayout>(R.id.light_theme_card)
        val darkCard = dialogView.findViewById<LinearLayout>(R.id.dark_theme_card)
        val systemCard = dialogView.findViewById<LinearLayout>(R.id.system_theme_card)
        
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent to show rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set click listeners for theme cards
        lightCard.setOnClickListener {
            selectTheme(AppTheme.LIGHT, settings, dialog)
        }
        
        darkCard.setOnClickListener {
            selectTheme(AppTheme.DARK, settings, dialog)
        }
        
        systemCard.setOnClickListener {
            selectTheme(AppTheme.SYSTEM_DEFAULT, settings, dialog)
        }
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun selectTheme(theme: AppTheme, settings: SettingsService, dialog: AlertDialog) {
        lifecycleScope.launch {
            settings.updateTheme(theme)
            ThemeUtils.applyTheme(theme)
        }
        dialog.dismiss()
    }
}
