package com.example.wastedetector.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.wastedetector.MainActivity
import com.example.wastedetector.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var redirectRegister: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email = findViewById(R.id.loginEmail)
        password = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.btnLogin)
        redirectRegister = findViewById(R.id.textRegister)

        auth = Firebase.auth

        loginBtn.setOnClickListener {
            login()
        }

        redirectRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

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