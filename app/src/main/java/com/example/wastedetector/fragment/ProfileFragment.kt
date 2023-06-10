package com.example.wastedetector.fragment

import android.content.ActivityNotFoundException
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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlin.math.floor
import kotlin.math.sqrt

class ProfileFragment : Fragment(), View.OnClickListener {

    private lateinit var profileImage: ShapeableImageView
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userLevel: TextView
    private lateinit var userPoint: TextView
    private lateinit var verifyEmail: MaterialCardView
    private lateinit var earnPoint: MaterialCardView
    private lateinit var carbonEmission: TextView
    private lateinit var emissionInfo: ImageView
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

        profileImage = view.findViewById(R.id.profileImage)
        userName = view.findViewById(R.id.userName)
        userEmail = view.findViewById(R.id.userEmail)
        userLevel = view.findViewById(R.id.userLevel)
        userPoint = view.findViewById(R.id.userPoint)
        carbonEmission = view.findViewById(R.id.carbonEmission)
        emissionInfo = view.findViewById(R.id.emissionInfo)
        rewardView = view.findViewById(R.id.rewardView)
        rewardAdapter = RewardAdapter(rewardsList)
        rewardView.adapter = rewardAdapter
        rewardView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        verifyEmail = view.findViewById(R.id.verifyEmail)
        earnPoint = view.findViewById(R.id.earnPoint)
        logOut = view.findViewById(R.id.logout)

        // Initialize the current user and firestore instance
        auth = Firebase.auth
        currentUser = auth.currentUser!!
        currentUser.reload()
        db = FirebaseFirestore.getInstance()
        userRef = db.collection("users").document(currentUser.uid)

        showLoadingDialog() // Display Loading when retrieving data
        loadUserData() // Display the user data

        emissionInfo.setOnClickListener(this) // Display way of calculating CO2e saved
        verifyEmail.setOnClickListener(this) // Verify user email
        earnPoint.setOnClickListener(this) // Display way to get point through recycle
        logOut.setOnClickListener(this) // Sign out

        return view
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.emissionInfo -> {
                displayCalMethod()
            }
            R.id.verifyEmail -> {
                sendEmailVerification()
            }
            R.id.earnPoint -> {
                displayEarnPtMethod()
            }
            R.id.logout -> {
                try{
                    signOut()
                }catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
        }
    }

    private fun displayEarnPtMethod() {
        customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_layout, null)
        val contentTextView = customView?.findViewById<TextView>(R.id.contentTextView)
        val btn = customView?.findViewById<Button>(R.id.customBtn)
        loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.CUSTOM_IMAGE_TYPE)
        loadingDialog!!
            .setTitleText("How to earn point")
            .setCustomImage(R.drawable.email)
            .setCustomView(customView)
            .also {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
            }
            .show()

        contentTextView?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        contentTextView?.textSize = 12F
        contentTextView?.text = "In our app, you can earn points by recycling waste! Here's how:\n\n" +
                "1. Start by detecting waste: Use the waste detection feature to identify different types of waste items.\n\n" +
                "2. Check recyclability: Once you detect an item, our app will let you know if it's recyclable or not.\n\n" +
                "3. Recycle and earn points: If the item is recyclable, you'll see a 'Recycle' button. Click on it to recycle the waste.\n\n" +
                "4. Earn points: Each time you successfully recycle an item, you'll earn points. The more you recycle, the more points you can accumulate.\n\n" +
                "5. Track your progress: Keep an eye on your points in the app. You can see your total points earned and use them to unlock rewards or participate in challenges.\n\n" +
                "Remember, recycling helps protect the environment and reduces waste. So, start recycling today and earn points for your positive impact!"
    }

    private fun displayCalMethod() {
        TODO("Not yet implemented")
    }

    private fun sendEmailVerification() {

        // Check if the email is verified
        // Sent verification email if not
        currentUser.reload()
        if (!currentUser.isEmailVerified) {
            currentUser.sendEmailVerification()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d(TAG, "Email is sent to ${currentUser.email}")
                        showVerificationDialog("The verification email is sent. Please check!")
                    } else {
                        Log.e(TAG, "Failed to send verification email")
                    }
                }
        } else {
            Log.d(TAG, "Email already verified")
            showVerificationDialog("Your email is verified.")
        }
    }

    // Tell user the status of email verification
    private fun showVerificationDialog(s: String) {
        customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_layout, null)
        val contentTextView = customView?.findViewById<TextView>(R.id.contentTextView)
        val btn = customView?.findViewById<Button>(R.id.customBtn)
        loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.CUSTOM_IMAGE_TYPE)
        loadingDialog!!
            .setTitleText("Verify Email")
            .setCustomImage(R.drawable.email)
            .setCustomView(customView)
            .hideConfirmButton()
            .also {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
            }
            .show()

        contentTextView?.text = s
        btn?.visibility = View.VISIBLE
        btn?.setOnClickListener {
            loadingDialog?.dismissWithAnimation()
        }
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
                    val point = it.getLong("point") ?: 0
                    val emission = it.getDouble("emission")
                    val level = floor(sqrt(point.toDouble() / 100)).toInt() + 1
                    val image: String = if (level<7) {
                        "profile$level"
                    } else {
                        "profile"
                    }
                    val res = resources.getIdentifier(image, "drawable", context?.packageName)

                    // Update the field accordingly
                    profileImage.setImageResource(res)
                    userName.text = username
                    userEmail.text = email
                    userLevel.text = "Level $level"
                    userPoint.text = "$point points"
                    println("emission$emission")
                    carbonEmission.text = "$emission kg CO2e saved"

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

//    private fun calEmission(): String {
//        val categories = listOf("cardboard", "glass", "metal", "plastic", "paper")
//
//        for (category in categories) {
//            val dbRef = userRef.collection(category)
//        }
//
//    }

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
                            val reward = Reward(id, name, url, points, isEligible, isClaimed, currentUser.isEmailVerified)
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
    val isClaimed: Boolean,
    val isVerified: Boolean
)