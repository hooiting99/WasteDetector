package com.example.wastedetector

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class InnerImageAdapter(private val context: Context, private val images: List<String>) :
    RecyclerView.Adapter<InnerImageAdapter.InnerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
        return InnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        val image = images[position]
        // Using Glide to load the image url into the view
        Glide.with(context).load(image).into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class InnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.innerImage)
    }
}
