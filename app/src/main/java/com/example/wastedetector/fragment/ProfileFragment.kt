package com.example.wastedetector.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
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
            .setCustomImage(R.drawable.earn_point)
            .setCustomView(customView)
            .hideConfirmButton()
            .also {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
            }
            .show()

        contentTextView?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        contentTextView?.textSize = 14F
        contentTextView?.text = buildString {
            append("In our app, you can earn points by recycling waste! Here's how:\n\n")
            append("1. Start by detecting waste: Use the waste detection feature to identify different types of waste items.\n\n")
            append("2. Check recyclability: Once you detect an item, our app will let you know if it's recyclable or not.\n\n")
            append("3. Recycle and earn points: If the item is recyclable, you'll see a 'Recycle' button. Click on it to recycle the waste.\n\n")
            append("4. Earn points: Each time you successfully recycle an item, you'll earn points. The more you recycle, the more points you can accumulate.\n\n")
            append("5. Track your progress: Keep an eye on your points in the app. You can see your total points earned and use them to unlock rewards or participate in challenges.\n\n")
            append("Remember, recycling helps protect the environment and reduces waste. So, start recycling today and earn points for your positive impact!")
        }
        contentTextView?.movementMethod = ScrollingMovementMethod()
        btn?.setOnClickListener {
            loadingDialog?.dismissWithAnimation()
        }
    }

    private fun displayCalMethod() {
        customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_layout, null)
        val contentTextView = customView?.findViewById<TextView>(R.id.contentTextView)
        val btn = customView?.findViewById<Button>(R.id.customBtn)
        loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.CUSTOM_IMAGE_TYPE)
        loadingDialog!!
            .setTitleText("Carbon Emission")
            .setCustomImage(R.drawable.carbon)
            .setCustomView(customView)
            .hideConfirmButton()
            .also {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
            }
            .show()

        contentTextView?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        contentTextView?.textSize = 14F
        contentTextView?.text = buildString {
        append("The CO2e (carbon dioxide equivalent) value displayed is a estimation on the carbon footprint/emission that you can offset through recycling. " +
                "It is calculated based on the weight of the recycled waste which measured in kg of CO2e. ")
        append("The emission factor used in calculation assumes the following:\n\n")
        append("- Cardboard:\nIt is assumed as e-commerce box that is 9 x 9 x 9 inches weighing 291g.\n\n")
        append("- Glass:\nIt is assumed as glass bottle like wine bottle weighing 600 to 900g.\n\n")
        append("- Paper:\nIt is assumed as a paper cup or piece of A4 paper weighing 60 to 80g.\n\n")
        append("- Plastic:\nIt is assumed as a plastic bag average weighing 32.5g or 1.5ml bottles weighing 44 to 633g.\n\n")
        append("- Metal:\nIt is assumed as aluminium/steel cans that weighing around 15g up to 425g.\n\n")
        append("CO2e is a unit used to measure the impact of greenhouse gas emissions on the environment. ")
        append("This value is just a reference for you to get an idea on how recycling reduce carbon emission.\n\n")
        append("By recycling waste, you are contributing to the reduction of CO2e emissions. ")
        append("Recycling helps conserve resources, reduce energy consumption, and minimize pollution.\n\n")
    }

        contentTextView?.movementMethod = ScrollingMovementMethod()
        btn?.setOnClickListener {
            loadingDialog?.dismissWithAnimation()
        }
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
        btn?.setOnClickListener {
            loadingDialog?.dismissWithAnimation()
        }
    }

    // Configure and Display the loading dialog
    private fun showLoadingDialog() {
        customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_layout, null)
        val btn = customView?.findViewById<Button>(R.id.customBtn)
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
        btn?.visibility = View.INVISIBLE
    }

    private fun loadUserData() {

        // Retrieve the user data and display them
        userRef.get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val username = it.getString("username")
                    val email = currentUser.email
                    val point = it.getLong("point") ?: 0
                    val emission = it.getDouble("emission") ?: 0
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
                    userLevel.text = buildString {
                        append("Level ")
                        append(level)
                    }
                    userPoint.text = buildString {
                        append(point)
                        append(" points")
                    }
                    carbonEmission.text = buildString {
                        append(emission)
                        append(" kg CO2e saved")
                    }

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
                            val reward = Reward(id, name, url, points, isEligible, isClaimed, currentUser.isEmailVerified)
                            Log.d(TAG, reward.toString())
                            dataList.add(reward)

                            // Update the reward adapter when all rewards are processed
                            if (dataList.size == it.size()) {
                                // Sort the reward list according to points
                                dataList.sortBy { r ->
                                    r.points
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