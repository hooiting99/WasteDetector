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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.wastedetector.R
import com.example.wastedetector.adapter.RewardAdapter
import com.example.wastedetector.login.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userPoint: TextView
    private lateinit var logOut: ImageView
    private lateinit var rewardView: RecyclerView
    private var customView: View? = null
    private var loadingDialog: SweetAlertDialog? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var db: FirebaseFirestore
    private lateinit var userRef: DocumentReference
    private lateinit var rewardAdapter: RewardAdapter
    private val rewardsList = mutableListOf<Reward>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        userName = view.findViewById(R.id.userName)
        userEmail = view.findViewById(R.id.userEmail)
        userPoint = view.findViewById(R.id.userPoint)
        rewardView = view.findViewById(R.id.rewardView)
        rewardAdapter = RewardAdapter(rewardsList)
        rewardView.adapter = rewardAdapter
        rewardView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        logOut = view.findViewById(R.id.logout)
        auth = Firebase.auth
        currentUser = auth.currentUser!!
        db = FirebaseFirestore.getInstance()
        userRef = db.collection("users").document(currentUser.uid)

        showLoadingDialog() // Display Loading when retrieving data
        loadUserData() // Display the user data

        // Sign out
        logOut.setOnClickListener {
            signOut()
        }

        return view
    }

    // Configure and Display the loading dialog
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

        // Retrieve the user data and display them
        userRef.get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val username = it.getString("username")
                    val email = currentUser.email
                    val point = it.getLong("point")

                    // Update the field accordingly
                    userName.text = username
                    userEmail.text = email
                    userPoint.text = "$point points"

                    loadRewards(point) // Display the rewards and status
                } else {
                    showErrorDialog("User data not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error retrieving user data", e)
                showErrorDialog("Something wrong! Try clear cache and reload!")
            }
    }

    private fun loadRewards(pt: Long?) {

        // Get the all rewards detail from firestore
        db.collection("rewards")
            .orderBy("points")
            .get()
            .addOnSuccessListener {
                Log.d(TAG, "Document retrieve success: ${it.documents}")
                if (it.documents.isNotEmpty()) {
                    Log.d(TAG, "Reward Retrieved")

                    val dataList = mutableListOf<Reward>() // Store the list of rewards data

                    // Get the data of each reward
                    for (document in it.documents) {
                        val id = document.id
                        val name = document.getString("name") ?: ""
                        val url = document.getString("logoUrl") ?: ""
                        val points = document.getLong("points") ?: 0

                        // Verify if the user eligible for redemption
                        val isEligible: Boolean = pt!! > points || pt == points

                        // Verify if the reward not yet claimed by user
                        isRewardClaimed(id) { isClaimed ->
                            val reward = Reward(id, name, url, points, isEligible, isClaimed)
                            Log.d(TAG, reward.toString())
                            dataList.add(reward)

                            // Update the reward adapter when all rewards are processed
                            if (dataList.size == it.size()) {
                                // Sort the reward list according to points
                                dataList.sortBy { reward ->
                                    reward.points
                                }
                                rewardsList.clear()
                                rewardsList.addAll(dataList)
                                rewardAdapter.notifyDataSetChanged()
                                loadingDialog?.dismissWithAnimation() // Close the loading dialog
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "No reward found")
                }
            }
            .addOnFailureListener { e ->
                showErrorDialog("Failed to load rewards$e")
            }
    }

    private fun isRewardClaimed(id: String, callback: (Boolean) -> Unit) {
        val rewardRef = userRef.collection("claimedRewards")

        // Verify if the user do not have the rewardId
        rewardRef.whereEqualTo("rewardID", id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val isClaimed = !querySnapshot.isEmpty
                callback(isClaimed)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(false)
            }
    }

    // Configure and Display error dialog
    private fun showErrorDialog(error: String) {
        val contentTextView = customView?.findViewById<TextView>(R.id.contentTextView)
        val btn = customView?.findViewById<Button>(R.id.customBtn)
        loadingDialog?.changeAlertType(SweetAlertDialog.ERROR_TYPE)
        loadingDialog?.titleText = "Opps..."
        contentTextView?.text = error
        btn?.visibility = View.VISIBLE
        btn?.setOnClickListener {
            loadingDialog?.dismissWithAnimation()
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

// Data object to hold the data of reward
data class Reward(
    val id: String,
    val name: String,
    val url: String,
    val points: Long,
    val isEligible: Boolean,
    val isClaimed: Boolean
)