package com.example.wastedetector.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.wastedetector.MainActivity
import com.example.wastedetector.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
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
            finish()
        }
    }

    private fun createAccount() {
        val emailStr = email.text.toString()
        val passStr = password.text.toString()
        val confPassStr = confPass.text.toString()

        if (emailStr.isEmpty() || passStr.isEmpty() || confPassStr.isEmpty()) {
            Toast.makeText(this, "Make sure all fields is filled!", Toast.LENGTH_SHORT).show()
            return
        }else if (!Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            email.requestFocus()
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }else if (passStr.length < 6) {
            password.requestFocus()
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }else if (passStr != confPassStr) {
            Toast.makeText(this, "Password and Confirm Password do not match", Toast.LENGTH_SHORT).show()
            return
        }else {
            auth.createUserWithEmailAndPassword(emailStr, passStr)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("auth", "createUserWithEmail:success")
                        Toast.makeText(this, "Register Successful", Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthUserCollisionException) {
                            // email already in use
                            Toast.makeText(applicationContext,
                                "Email already taken!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // If sign in fails, display a message to the user.
                        Log.w("auth", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            this,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}