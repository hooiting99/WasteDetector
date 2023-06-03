package com.example.wastedetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.wastedetector.fragment.GalleryFragment
import com.example.wastedetector.fragment.HomeFragment
import com.example.wastedetector.fragment.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_PERMISSIONS: Int = 1
    }
    private var bottomNav : BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        Ensure the fragment state is null before recreating fragment transaction
        if (savedInstanceState == null) {
            reqPermissions()
            loadFragment(HomeFragment())
        }

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav?.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.gallery -> {
                    loadFragment(GalleryFragment())
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> {false}
            }
        }
    }

    // Get required camera permissions from user
    private fun reqPermissions() {
        val cameraPermission = Manifest.permission.CAMERA
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        val cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            cameraPermission
        ) == PackageManager.PERMISSION_GRANTED

        val storagePermissionGranted = ContextCompat.checkSelfPermission(
            this,
            storagePermission
        ) == PackageManager.PERMISSION_GRANTED

        if (!cameraPermissionGranted || !storagePermissionGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(cameraPermission, storagePermission),
                REQUEST_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions are granted, continue with app initialization
                loadFragment(HomeFragment())
            } else {
                // Permissions are not granted, show a message and quit the app
                Toast.makeText(
                    this,
                    "Required permissions are not granted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    // Navigate to selected fragment
    private  fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container,fragment)
        transaction.commit()
    }

    fun disableBottomNavigation() {
        bottomNav?.visibility = View.GONE
    }

    fun enableBottomNavigation() {
        bottomNav?.visibility = View.VISIBLE
    }
}