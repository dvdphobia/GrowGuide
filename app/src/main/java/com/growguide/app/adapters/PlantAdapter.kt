package com.growguide.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.growguide.app.R
import com.growguide.app.models.Plant
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for the plant list in MainActivity.
 * Displays plant photo, name, type, and creation date in a card layout.
 */
class PlantAdapter(
    private val plants: List<Plant>,
    private val onPlantClick: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    inner class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImage: ImageView = itemView.findViewById(R.id.plantPhotoImage)
        val nameText: TextView = itemView.findViewById(R.id.plantNameText)
        val typeText: TextView = itemView.findViewById(R.id.plantTypeText)
        val dateText: TextView = itemView.findViewById(R.id.plantDateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.nameText.text = plant.name
        holder.typeText.text = plant.type

        val date = plant.createdAt?.toDate()
        if (date != null) {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            holder.dateText.text = formatter.format(date)
        }

        // Show photo if available, otherwise show a leaf emoji as placeholder
        if (plant.photoUrl.isNotBlank() && plant.photoUrl.startsWith("/")) {
            val file = java.io.File(plant.photoUrl)
            if (file.exists()) {
                holder.photoImage.setImageURI(android.net.Uri.fromFile(file))
            }
        } else {
            holder.photoImage.setImageDrawable(null)
            holder.photoImage.background = null
            holder.photoImage.setPadding(0, 0, 0, 0)
            // Use a TextView overlay or just leave it empty for now
            // For simplicity, we'll use a background drawable
            holder.photoImage.setBackgroundResource(R.drawable.bg_card)
        }

        holder.itemView.setOnClickListener { onPlantClick(plant) }
    }

    override fun getItemCount(): Int = plants.size
}
