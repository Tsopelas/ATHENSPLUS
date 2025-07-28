package com.example.athensplus.presentation.common

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.athensplus.R
import com.example.athensplus.core.utils.AddressAutocompleteService
import com.example.athensplus.core.utils.LocationService
import com.example.athensplus.domain.model.AddressSuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddressAutocompleteManager(
    private val fragment: Fragment,
    private val locationService: LocationService,
    private val addressAutocompleteService: AddressAutocompleteService
) {
    
    private var currentAddressPopup: PopupWindow? = null
    private var isSettingTextProgrammatically: Boolean = false

    fun setupAutocomplete(editText: EditText) {
        var searchJob: Job? = null
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isSettingTextProgrammatically) {
                    return
                }
                
                val query = s?.toString()?.trim() ?: ""

                searchJob?.cancel()
                
                if (query.length >= 2) {
                    Log.d("AddressAutocompleteManager", "Triggering autocomplete for query: '$query'")
                    searchJob = fragment.lifecycleScope.launch {
                        delay(200)
                        try {
                            val userLocation = locationService.getCurrentLocation()
                            Log.d("AddressAutocompleteManager", "User location: $userLocation")
                            val suggestions = addressAutocompleteService.getAddressSuggestions(query, userLocation)
                            Log.d("AddressAutocompleteManager", "Got ${suggestions.size} suggestions for '$query'")
                            if (suggestions.isNotEmpty()) {
                                showAddressSuggestions(suggestions, editText)
                            } else {
                                Log.d("AddressAutocompleteManager", "No suggestions found for '$query', hiding dropdown")
                                hideAddressSuggestions()
                            }
                        } catch (e: Exception) {
                            Log.e("AddressAutocompleteManager", "Error getting address suggestions for '$query'", e)
                            hideAddressSuggestions()
                        }
                    }
                } else {
                    Log.d("AddressAutocompleteManager", "Query too short: '$query', hiding dropdown")
                    hideAddressSuggestions()
                }
            }
        })

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideAddressSuggestions()
            }
        }
    }

    fun setupDialogAutocomplete(editFromLocation: EditText, editToLocation: EditText) {
        var searchJobTo: Job? = null
        editToLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isSettingTextProgrammatically) {
                    return
                }
                
                val query = s?.toString()?.trim() ?: ""

                searchJobTo?.cancel()
                
                if (query.length >= 2) {
                    searchJobTo = fragment.lifecycleScope.launch {
                        delay(200)
                        try {
                            val userLocation = locationService.getCurrentLocation()
                            val suggestions = addressAutocompleteService.getAddressSuggestions(query, userLocation)
                            if (suggestions.isNotEmpty()) {
                                showAddressSuggestions(suggestions, editToLocation)
                            } else {
                                hideAddressSuggestions()
                            }
                        } catch (e: Exception) {
                            Log.e("AddressAutocompleteManager", "Error getting address suggestions for dialog", e)
                            hideAddressSuggestions()
                        }
                    }
                } else {
                    hideAddressSuggestions()
                }
            }
        })

        var searchJobFrom: Job? = null
        editFromLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Skip if we're setting text programmatically
                if (isSettingTextProgrammatically) {
                    return
                }
                
                val query = s?.toString()?.trim() ?: ""

                searchJobFrom?.cancel()
                
                if (query.length >= 2) {
                    searchJobFrom = fragment.lifecycleScope.launch {
                        delay(200)
                        try {
                            val userLocation = locationService.getCurrentLocation()
                            val suggestions = addressAutocompleteService.getAddressSuggestions(query, userLocation)
                            if (suggestions.isNotEmpty()) {
                                showAddressSuggestions(suggestions, editFromLocation)
                            } else {
                                hideAddressSuggestions()
                            }
                        } catch (e: Exception) {
                            Log.e("AddressAutocompleteManager", "Error getting address suggestions for dialog", e)
                            hideAddressSuggestions()
                        }
                    }
                } else {
                    hideAddressSuggestions()
                }
            }
        })
    }

    private fun showAddressSuggestions(suggestions: List<AddressSuggestion>, editText: EditText) {
        try {
            hideAddressSuggestions()
            
            val popupView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.popup_address_suggestions, fragment.view as ViewGroup, false)
            val recyclerView = popupView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.suggestions_recycler_view)
            
            recyclerView.layoutManager = LinearLayoutManager(fragment.requireContext())
            recyclerView.adapter = AddressSuggestionAdapter(suggestions) { suggestion ->
                isSettingTextProgrammatically = true
                editText.setText(suggestion.address)
                editText.setSelection(suggestion.address.length)
                hideAddressSuggestions()
                fragment.lifecycleScope.launch {
                    delay(100)
                    isSettingTextProgrammatically = false
                }
            }

            val searchBarWidth = editText.width + 50 + 20
            val finalWidth = (searchBarWidth * 1.1).toInt()
            
            val popupWindow = PopupWindow(
                popupView,
                finalWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                isOutsideTouchable = true
                isFocusable = false
                elevation = 0f
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                animationStyle = android.R.style.Animation_Dialog
            }

            popupWindow.showAsDropDown(editText, 0, 0)

            currentAddressPopup = popupWindow

            popupWindow.setOnDismissListener {
                currentAddressPopup = null
            }
        } catch (e: Exception) {
            Log.e("AddressAutocompleteManager", "Error showing address suggestions", e)
        }
    }
    
    private fun hideAddressSuggestions() {
        try {
            currentAddressPopup?.let { popup ->
                if (popup.isShowing) {
                    popup.dismiss()
                }
            }
            currentAddressPopup = null
        } catch (e: Exception) {
            Log.e("AddressAutocompleteManager", "Error hiding address suggestions", e)
            currentAddressPopup = null
        }
    }
} 