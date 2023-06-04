package com.example.wastedetector.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wastedetector.MainActivity
import com.example.wastedetector.adapter.OuterImageAdapter
import com.example.wastedetector.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ImageFragment : Fragment() {

    private lateinit var mainActivity: MainActivity
    private lateinit var imageView: RecyclerView
    private lateinit var imageAdapter: OuterImageAdapter
    private val outerDataList = mutableListOf<OuterData>()
    private var selectedCategory: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCategory = arguments?.getSerializable(ARG_CATEGORY) as Category?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image, container, false)

        imageView = view.findViewById(R.id.outerImageView)

        // Create an adapter to whole set of images based on date created
        imageAdapter = OuterImageAdapter(requireContext(), outerDataList)
        imageView.adapter = imageAdapter
        imageView.layoutManager = LinearLayoutManager(requireContext())

        // Load all images from Firestore
        loadImagesFromFirestore()

        return view
    }

    private fun loadImagesFromFirestore() {

        val userId = Firebase.auth.currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dbRef = db.collection("users").document(userId.toString())
                    .collection(selectedCategory?.name!!.lowercase())
                val querySnapshot = dbRef
                    .orderBy("date")
                    .get()
                    .await()

                val loadedImages = TreeMap<String, MutableList<String>>()

                for (document in querySnapshot.documents) {
                    val imageUrl = document.getString("imageUrl") ?: continue
                    val dateTimestamp = document.getLong("date")
                    val dateTaken = Date(dateTimestamp!!)
                    val dateKey = formatDate(dateTaken)

                    // Create a data structure to map the url to the date
                    if (loadedImages.containsKey(dateKey)) {
                        loadedImages[dateKey]?.add(imageUrl)
                    } else {
                        val imageUrlList = mutableListOf<String>()
                        imageUrlList.add(imageUrl)
                        loadedImages[dateKey] = imageUrlList
                    }
                }

                val dataList = mutableListOf<OuterData>()

                for (key in loadedImages.keys) {
                    val images = loadedImages[key] ?: emptyList()
                    dataList.add(OuterData(key, images))
                }

                // Update the UI on the main thread
                launch(Dispatchers.Main) {
                    outerDataList.clear()
                    outerDataList.addAll(dataList)
                    imageAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Format the date into dd/mmmm/yyyy standard
    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }

    override fun onStart() {
        super.onStart()
        mainActivity = activity as MainActivity
        mainActivity.disableBottomNavigation()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainActivity.enableBottomNavigation()
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: Category): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putSerializable(ARG_CATEGORY, category)
            fragment.arguments = args
            return fragment
        }
    }
}

data class OuterData(val date: String, val images: List<String>)