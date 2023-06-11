package com.example.wastedetector.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.wastedetector.MainActivity
import com.example.wastedetector.R
import com.example.wastedetector.adapter.OuterImageAdapter
import com.google.android.material.appbar.MaterialToolbar
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
    private lateinit var topBar: MaterialToolbar
    private lateinit var imageView: RecyclerView
    private lateinit var textView: TextView
    private lateinit var imageAdapter: OuterImageAdapter
    private val outerDataList = mutableListOf<OuterData>()
    private var selectedCategory: Category? = null
    private var customView: View? = null
    private var loadingDialog: SweetAlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCategory = arguments?.getSerializable(ARG_CATEGORY) as Category?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image, container, false)

        topBar = view.findViewById(R.id.topAppBar)
        imageView = view.findViewById(R.id.outerImageView)
        textView = view.findViewById(R.id.textView)

        // Create an adapter to whole set of images based on date created
        imageAdapter = OuterImageAdapter(requireContext(), outerDataList)
        imageView.adapter = imageAdapter
        imageView.layoutManager = LinearLayoutManager(requireContext())

        showLoadingDialog() // Display Loading when retrieving data
        loadImagesFromFirestore() // Load all images from Firestore

        topBar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack() // Close and navigate back
        }

        return view
    }

    private fun showLoadingDialog() {
        customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_layout, null)
        val btn = customView?.findViewById<Button>(R.id.customBtn)
        loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog!!
            .setCustomView(customView)
            .setTitleText("Loading")
            .hideConfirmButton()
            .also {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
            }
            .show()
        btn?.visibility = View.INVISIBLE
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

                if (querySnapshot.isEmpty) {
                    // Update the UI on the main thread
                    launch(Dispatchers.Main) {
                        loadingDialog?.dismissWithAnimation()
                        imageView.visibility = View.GONE
                    }
                } else {
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
                        loadingDialog?.dismissWithAnimation()
                    }
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