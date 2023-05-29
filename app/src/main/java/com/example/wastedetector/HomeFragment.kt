package com.example.wastedetector

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeFragment : Fragment() {
    private lateinit var detectBtn: Button
    private lateinit var cameraOption: LinearLayout
    private lateinit var imageOption: LinearLayout
    private lateinit var cancelBtn: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        detectBtn = view.findViewById(R.id.startDetect)

        detectBtn.setOnClickListener {
            showImageSourceDialog()
        }

        return view
    }

//    private fun showImageSourceDialog() {
//        val options = arrayOf("Camera", "Image")
//        val builder = AlertDialog.Builder(requireContext())
//        builder.setTitle("Choose Detection Option")
//            .setItems(options) { dialog, which ->
//                when (which) {
//                    0 -> {
//                        // Camera option selected
//                        navigateToCameraActivity()
//                    }
//                    1 -> {
//                        // Image option selected
//                        navigateToImageActivity()
//                    }
//                }
//                dialog.dismiss()
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }.setI
//        builder.create().show()
//    }


    private fun showImageSourceDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext()).create()
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_layout, null)
        dialogBuilder.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(100, WindowManager.LayoutParams.WRAP_CONTENT)
            setView(dialogView)
            setCancelable(false)
        }.show()

        cameraOption = dialogView.findViewById(R.id.cameraOption)!!
        imageOption = dialogView.findViewById(R.id.imageOption)!!
        cancelBtn = dialogView.findViewById(R.id.cancelButton)!!

        // Set icons and click listeners for the buttons
        cameraOption.setOnClickListener {
            navigateToCameraActivity()
            dialogBuilder.dismiss()
        }

        imageOption.setOnClickListener {
            navigateToImageActivity()
            dialogBuilder.dismiss()
        }

        cancelBtn.setOnClickListener{
            dialogBuilder.dismiss()
        }
    }

    private fun navigateToImageActivity() {
        val intent = Intent(requireContext(), ImageDetectionActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCameraActivity() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        intent.putExtra("activity_type", "real_time")
        startActivity(intent)
    }

}