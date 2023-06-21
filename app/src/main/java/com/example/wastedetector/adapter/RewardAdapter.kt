package com.example.wastedetector.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.wastedetector.R
import com.example.wastedetector.fragment.Reward
import com.example.wastedetector.login.RegisterActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class RewardAdapter(private val rewardsList: List<Reward>) :
    RecyclerView.Adapter<RewardAdapter.RewardViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val reward = rewardsList[position]

        // Load and display the SVG image using Glide
        Glide.with(holder.itemView).load(reward.url).into(holder.rewardImage)
        holder.rewardName.text = reward.name
        holder.rewardPoint.text = "${reward.points} points"

        // Update the button according to conditions
        if (!reward.isVerified && reward.isEligible) {
            holder.claimBtn.text = "Verify to Claim"
            holder.claimBtn.isEnabled = false
        }else if (reward.isClaimed) {
            holder.claimBtn.text = "Claimed"
            holder.claimBtn.isEnabled = false
        } else if (!reward.isEligible) {
            holder.claimBtn.text = "Claim"
            holder.claimBtn.isEnabled = false
        } else {
            holder.claimBtn.text = "Claim"
            holder.claimBtn.isEnabled = true
            holder.claimBtn.setOnClickListener {v ->
                val userId = Firebase.auth.currentUser?.uid
                val db = FirebaseFirestore.getInstance()
                val dbRef = db.collection("users").document(userId.toString()).collection("claimedRewards")
                val rewardID = hashMapOf(
                    "rewardID" to reward.id
                )

                dbRef
                    .add(rewardID)
                    .addOnSuccessListener {
                        val customView = LayoutInflater.from(v.context).inflate(R.layout.dialog_custom_layout, null)
                        // Find the TextView in the custom layout
                        val contentTextView = customView.findViewById<TextView>(R.id.contentTextView)
                        val btn = customView.findViewById<Button>(R.id.customBtn)
                        val dialog = SweetAlertDialog(v.context, SweetAlertDialog.SUCCESS_TYPE)
                            .setCustomView(customView)
                            .setTitleText("Congratulation")
                            .hideConfirmButton()
                            .also {
                                it.setCancelable(false)
                                it.setCanceledOnTouchOutside(false)
                            }
                        "Check your email for details of rewards redemption!".also { s->
                            contentTextView.text = s }
                        btn.setOnClickListener {
                            dialog.dismissWithAnimation()
                        }
                        dialog.show()
                        Log.d(RegisterActivity.TAG, "Reward id successfully added!")
                    }
                    .addOnFailureListener { e ->
                        Log.w(RegisterActivity.TAG, "Error adding the id", e)
                    }
            }
        }
    }

    override fun getItemCount(): Int {
        return rewardsList.size
    }

    inner class RewardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rewardImage: ImageView = view.findViewById(R.id.rewardImage)
        val rewardName: TextView = view.findViewById(R.id.rewardName)
        val rewardPoint: TextView = view.findViewById(R.id.rewardPoints)
        val claimBtn: Button = view.findViewById(R.id.claimBtn)
    }
}