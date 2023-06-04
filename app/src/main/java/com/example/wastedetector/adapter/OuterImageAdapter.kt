package com.example.wastedetector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wastedetector.R
import com.example.wastedetector.fragment.OuterData

class OuterImageAdapter(private val context: Context, private val dateList: List<OuterData>) :
    RecyclerView.Adapter<OuterImageAdapter.OuterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.outer_item_layout, parent, false)
        return OuterViewHolder(view)
    }

    override fun onBindViewHolder(holder: OuterViewHolder, position: Int) {
        val outerData = dateList[position]
        holder.dateHeaderView.text = outerData.date

        // Create an adapter to display the images
        val innerAdapter = InnerImageAdapter(context, outerData.images)
        holder.innerImageView.adapter = innerAdapter
        // Display image in grid view of 3 columns
        holder.innerImageView.layoutManager = GridLayoutManager(context, 3)
    }

    override fun getItemCount(): Int {
        return dateList.size
    }

    inner class OuterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateHeaderView: TextView = itemView.findViewById(R.id.dateHeaderText)
        val innerImageView: RecyclerView = itemView.findViewById(R.id.innerImageView)
    }
}
