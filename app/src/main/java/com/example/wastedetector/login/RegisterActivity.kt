package com.example.wastedetector.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.wastedetector.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confPass: EditText
    private lateinit var registerBtn: Button
    private lateinit var redirectLogin: TextView
    private lateinit var auth: FirebaseAuth // Create a Firebase authentication obj

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        email = findViewById(R.id.inputEmail)
        password = findViewById(R.id.inputPassword)
        confPass = findViewById(R.id.inputConfirmPassword)
        registerBtn = findViewById(R.id.btnRegister)
        redirectLogin = findViewById(R.id.textLogin)

        auth = Firebase.auth // Initializing auth obj

        registerBtn.setOnClickListener {
            createAccount()
        }

        redirectLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createAccount() {
        val emailStr = email.text.toString()
        val passStr = password.text.toString()
        val confPassStr = confPass.text.toString()
    }
}