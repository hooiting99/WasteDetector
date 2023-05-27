package com.example.wastedetector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class CameraDetectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_detection)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateBack()
    }

    private fun navigateBack() {
        supportFragmentManager.popBackStack()
    }
}