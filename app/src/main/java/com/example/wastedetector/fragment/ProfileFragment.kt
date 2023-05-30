package com.example.wastedetector.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.wastedetector.R
import com.example.wastedetector.login.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private lateinit var logOut: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        logOut = view.findViewById(R.id.logout)
        auth = Firebase.auth
        logOut.setOnClickListener {
            signOut()
        }

        return view
    }

    private fun signOut() {
        auth.signOut()
        val intent = Intent(activity, WelcomeScreen::class.java)
        startActivity(intent)
        this.activity?.finish()
    }

    companion object {

    }
}