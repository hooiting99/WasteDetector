package com.example.wastedetector.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.wastedetector.R
import com.example.wastedetector.login.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userPoint: TextView
    private lateinit var logOut: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var db: FirebaseFirestore
    private var customView: View? = null
    private var loadingDialog: SweetAlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        userName = view.findViewById(R.id.userName)
        userEmail = view.findViewById(R.id.userEmail)
        userPoint = view.findViewById(R.id.userPoint)

        logOut = view.findViewById(R.id.logout)
        auth = Firebase.auth
        currentUser = auth.currentUser!!
        db = FirebaseFirestore.getInstance()

        showLoadingDialog()
        loadUserData()

        logOut.setOnClickListener {
            signOut()
        }

        return view
    }

    private fun showLoadingDialog() {
        customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_layout, null)
        loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog!!
            .setCustomView(customView)
            .setTitleText("Loading")
            .hideConfirmButton()
            .also {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
            }
            .show()
    }

    private fun loadUserData() {
        val userId = currentUser.uid
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val username = it.getString("username")
                    val email = currentUser.email
                    val point = it.getLong("point")

                    userName.text = username
                    userEmail.text = email
                    userPoint.text = "$point points"
                    loadingDialog?.dismissWithAnimation()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error retrieving user data", e)

                val contentTextView = customView?.findViewById<TextView>(R.id.contentTextView)
                val btn = customView?.findViewById<Button>(R.id.customBtn)
                loadingDialog?.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                loadingDialog?.titleText = "Opps..."
                contentTextView?.text = "Something wrong! Try clear cache and reload!"
                btn?.visibility = View.VISIBLE
            }
    }

    private fun signOut() {
        auth.signOut()
        val intent = Intent(activity, WelcomeScreen::class.java)
        startActivity(intent)
        this.activity?.finish()
    }

    companion object {
        const val TAG = "PRO_FRAG"
    }
}