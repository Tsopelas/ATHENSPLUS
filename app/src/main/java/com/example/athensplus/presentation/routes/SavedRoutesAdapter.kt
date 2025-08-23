package com.example.athensplus.presentation.routes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.athensplus.R
import com.example.athensplus.domain.model.SavedRoute
import java.text.SimpleDateFormat
import java.util.Locale

class SavedRoutesAdapter(
    private val onRouteClick: (SavedRoute) -> Unit,
    private val onDeleteClick: (SavedRoute) -> Unit,
    private val onFavoriteClick: (SavedRoute) -> Unit
) : ListAdapter<SavedRoute, SavedRoutesAdapter.ViewHolder>(SavedRouteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_route, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val routeContainer: View = itemView.findViewById(R.id.route_container)
        private val fromLocationText: TextView = itemView.findViewById(R.id.from_location_text)
        private val toLocationText: TextView = itemView.findViewById(R.id.to_location_text)
        private val routeSummaryText: TextView = itemView.findViewById(R.id.route_summary_text)
        private val savedDateText: TextView = itemView.findViewById(R.id.saved_date_text)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favorite_button)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

        fun bind(route: SavedRoute) {
            fromLocationText.text = route.fromLocation
            toLocationText.text = route.toLocation
            routeSummaryText.text = route.routeSummary
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            savedDateText.text = "Saved on ${dateFormat.format(route.savedAt)}"
            
            favoriteButton.setImageResource(
                if (route.isFavorite) R.drawable.ic_favorite else R.drawable.ic_saves
            )
            
            routeContainer.setOnClickListener { onRouteClick(route) }
            favoriteButton.setOnClickListener { onFavoriteClick(route) }
            deleteButton.setOnClickListener { onDeleteClick(route) }
        }
    }

    private class SavedRouteDiffCallback : DiffUtil.ItemCallback<SavedRoute>() {
        override fun areItemsTheSame(oldItem: SavedRoute, newItem: SavedRoute): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavedRoute, newItem: SavedRoute): Boolean {
            return oldItem == newItem
        }
    }
}
