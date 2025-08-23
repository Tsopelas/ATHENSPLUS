package com.example.athensplus.presentation.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.athensplus.R
import com.example.athensplus.core.utils.SavedRoutesService
import com.example.athensplus.domain.model.SavedRoute
import java.text.SimpleDateFormat
import java.util.Locale

class RoutesFragment : Fragment() {

    private lateinit var savedRoutesService: SavedRoutesService
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var adapter: SavedRoutesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_routes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        savedRoutesService = SavedRoutesService(requireContext())
        
        recyclerView = view.findViewById(R.id.saved_items_recycler)
        emptyStateContainer = view.findViewById(R.id.empty_state_container)
        
        setupRecyclerView()
        loadSavedRoutes()
    }

    private fun setupRecyclerView() {
        adapter = SavedRoutesAdapter(
            onRouteClick = { route -> showRouteDetails(route) },
            onDeleteClick = { route -> deleteRoute(route) },
            onFavoriteClick = { route -> toggleFavorite(route) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadSavedRoutes() {
        val routes = savedRoutesService.getAllRoutes()
        updateUI(routes)
    }

    private fun updateUI(routes: List<SavedRoute>) {
        if (routes.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE
            adapter.submitList(routes)
        }
    }

    private fun showRouteDetails(route: SavedRoute) {
        // TODO: Implement route details dialog
    }

    private fun deleteRoute(route: SavedRoute) {
        savedRoutesService.deleteRoute(route.id)
        loadSavedRoutes()
    }

    private fun toggleFavorite(route: SavedRoute) {
        savedRoutesService.toggleFavorite(route.id)
        loadSavedRoutes()
    }

    override fun onResume() {
        super.onResume()
        loadSavedRoutes()
    }

    companion object {
        fun newInstance(): RoutesFragment {
            return RoutesFragment()
        }
    }
}
