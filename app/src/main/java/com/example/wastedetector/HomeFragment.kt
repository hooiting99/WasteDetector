package com.example.wastedetector

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeFragment : Fragment() {
    private lateinit var detectBtn: Button
    private lateinit var cameraOption: LinearLayout
    private lateinit var imageOption: LinearLayout
    private lateinit var cancelBtn: ImageView

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

    private fun showImageSourceDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_layout, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        cameraOption = dialog.findViewById(R.id.cameraOption)!!
        imageOption = dialog.findViewById(R.id.imageOption)!!
        cancelBtn = dialog.findViewById(R.id.cancelButton)!!

        // Set icons and click listeners for the buttons
        cameraOption.setOnClickListener {
            navigateToCameraActivity()
            dialog.dismiss()
        }

        imageOption.setOnClickListener {
            navigateToImageActivity()
            dialog.dismiss()
        }

        cancelBtn.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun navigateToImageActivity() {
        val intent = Intent(requireContext(), ImageDetectionActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCameraActivity() {
        val intent = Intent(requireContext(), CameraDetectionActivity::class.java)
        startActivity(intent)
    }

}