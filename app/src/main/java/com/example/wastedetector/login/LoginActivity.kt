package com.example.wastedetector.login

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wastedetector.MainActivity
import com.example.wastedetector.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var forgotPw : TextView
    private lateinit var loginBtn: Button
    private lateinit var redirectRegister: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email = findViewById(R.id.loginEmail)
        password = findViewById(R.id.loginPassword)
        forgotPw = findViewById(R.id.forgotPassword)
        loginBtn = findViewById(R.id.btnLogin)
        redirectRegister = findViewById(R.id.textRegister)

        auth = Firebase.auth // To authenticate the user

        forgotPw.setOnClickListener {
            sendResetPasswordEmail()
        }

        loginBtn.setOnClickListener {
            login()
        }

        redirectRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun sendResetPasswordEmail() {
        val dialog = Dialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_edittext_dialog, null)
        val emailView = dialogView.findViewById<EditText>(R.id.emailText)
        val sendBtn = dialogView.findViewById<TextView>(R.id.sendBtn)
        val cancelBtn = dialogView.findViewById<TextView>(R.id.cancelBtn)

        dialog.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(dialogView)
            setCancelable(false)
        }.show()

        sendBtn.setOnClickListener {
            val emailInput = emailView.text.toString()

            if (emailInput.isEmpty()) {
                Toast.makeText(this, "Email can't be blank", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                emailView.requestFocus()
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                auth.sendPasswordResetEmail(emailInput)
                    .addOnCompleteListener {
                        Toast.makeText(this, "Email Sent!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { 
                        Toast.makeText(this, "Something went wrong. Try Again!", Toast.LENGTH_SHORT).show()
                        Log.e("RESET", "Failed with error: $it")
                    }
            }
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    // Sign in using email and password input by user
    private fun login() {
        val emailStr = email.text.toString()
        val passStr = password.text.toString()

        if (emailStr.isEmpty() || passStr.isEmpty()) {
            Toast.makeText(this, "Email and Password can't be blank", Toast.LENGTH_SHORT).show()
            return
        }else if (!Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            email.requestFocus()
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }else if (passStr.length < 6) {
            password.requestFocus()
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }else {
            auth.signInWithEmailAndPassword(emailStr, passStr)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this, "Successfully Login", Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            this,
                            "Authentication failed. Please check your email and password",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}