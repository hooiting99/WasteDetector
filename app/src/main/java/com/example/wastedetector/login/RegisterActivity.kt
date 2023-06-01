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
import com.example.wastedetector.ImageDetectionActivity
import com.example.wastedetector.MainActivity
import com.example.wastedetector.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var username: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confPass: EditText
    private lateinit var registerBtn: Button
    private lateinit var redirectLogin: TextView
    private lateinit var auth: FirebaseAuth // Create a Firebase authentication obj
    private lateinit var db: FirebaseFirestore // Create a Firestore db obj

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        username = findViewById(R.id.inputUsername)
        email = findViewById(R.id.inputEmail)
        password = findViewById(R.id.inputPassword)
        confPass = findViewById(R.id.inputConfirmPassword)
        registerBtn = findViewById(R.id.btnRegister)
        redirectLogin = findViewById(R.id.textLogin)

        auth = Firebase.auth // Initializing auth obj
        db = FirebaseFirestore.getInstance() // Initialize the instance

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
        val nameStr = username.text.toString()
        val emailStr = email.text.toString()
        val passStr = password.text.toString()
        val confPassStr = confPass.text.toString()

        // User input validation
        if (nameStr.isEmpty() || emailStr.isEmpty() || passStr.isEmpty() || confPassStr.isEmpty()) {
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

                        // Store the username in Firestore
                        val userId = auth.currentUser?.uid
                        val userRef = db.collection("users").document(userId.toString())
                        val userData = hashMapOf(
                            "username" to nameStr,
                        )
                        userRef
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "DocumentSnapshot successfully written!")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error writing document", e)
                            }

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthUserCollisionException) {
                            // email already in use
                            Toast.makeText(this, "Email already taken!", Toast.LENGTH_SHORT).show()
                        }
                        // If sign in fails, display a message to the user.
                        Log.w("auth", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT,).show()
                    }
                }
        }
    }

    companion object {
        const val TAG = "REGISTER"
    }
}