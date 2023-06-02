package com.example.wastedetector.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wastedetector.CategoryAdapter
import com.example.wastedetector.R
import java.io.Serializable

class GalleryFragment : Fragment() {

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        recyclerView = view.findViewById(R.id.cardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create a list of categories
        val categories = listOf(
            Category(R.drawable.cardboard, "Cardboard"),
            Category(R.drawable.glass, "Glass"),
            Category(R.drawable.metal, "Metal"),
            Category(R.drawable.plastic, "Plastic"),
            Category(R.drawable.paper, "Paper"),
            Category(R.drawable.trash, "Trash")
        )

        // Create the adapter and attach it to the RecyclerView
        categoryAdapter = CategoryAdapter(categories) {
            navigateToImagesFragment(it)
        }
        recyclerView.adapter = categoryAdapter


        return view
    }

    private fun navigateToImagesFragment(category: Category) {
        val fragment = ImageFragment.newInstance(category)
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

data class Category(val imageResId: Int, val name: String): Serializable