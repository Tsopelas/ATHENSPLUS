package com.example.athensplus.presentation.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.athensplus.R

class SearchMenuBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.search_menu_dialog, container, false)

        val searchEditText = view.findViewById<EditText>(R.id.search_edit_text)
        val clearButton = view.findViewById<ImageButton>(R.id.search_clear_button)
        val addButton = view.findViewById<ImageButton>(R.id.search_add_button)
        val suggestionsList = view.findViewById<RecyclerView>(R.id.search_suggestions_list)

        clearButton.setOnClickListener { searchEditText.setText("") }
        // addButton.setOnClickListener { /* TODO: Add action */ }

        // TODO: Set up RecyclerView adapter, chips, and menu actions

        return view
    }
} 