package com.example.wastedetector

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wastedetector.fragment.Category
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.skydoves.progressview.ProgressView

class CategoryAdapter(
    private val categories: List<Category>,
    private val onItemClick: (Category) -> Unit) :
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryImageView: ImageView = itemView.findViewById(R.id.categoryImageView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val missionProgress: TextView = itemView.findViewById(R.id.missionProgress)
        private val progressView: ProgressView = itemView.findViewById(R.id.progressView)
        private val displayImages: ImageView = itemView.findViewById(R.id.iconView)

        private val userId = Firebase.auth.currentUser?.uid
        private val db = FirebaseFirestore.getInstance()

        fun bind(category: Category) {
            val dbRef = db.collection("users").document(userId.toString()).collection(category.name.lowercase())
            categoryImageView.setImageResource(category.imageResId)
            categoryTextView.text = category.name

            dbRef.get()
                .addOnSuccessListener { querySnapshot ->
                    // Get the size of images saved
                    val count = querySnapshot.size()

                    // Update the progress of recycle mission
                    val currentMax = progressView.max
                    val nextMax = getNextMaxValue(count)

                    if (nextMax > currentMax) {
                        progressView.max = nextMax.toFloat()
                    }
                    progressView.progress = count.toFloat()
                    missionProgress.text = "Mission: $count/${progressView.max.toInt()}"
                    Log.d("CAT_ADP", "Number of images for ${category.name.lowercase()}: $count")
                }
                .addOnFailureListener { e ->
                    Log.e("CAT_ADP", "Error getting the counts: $e")
                }

//            Display all the images history of selected category
            displayImages.setOnClickListener {
                onItemClick(category)
            }
        }
    }

    // Used to keep update the mission target
    private fun getNextMaxValue(count: Int): Int {
        var nextMax = 100
        while (nextMax <= count) {
            nextMax *= 3
        }
        return nextMax
    }
}