package com.example.icare.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.AdminChatActivity
import com.example.icare.AdminUpdateAppointmentActivity
import com.example.icare.databinding.RowAdminIncomingBinding
import com.example.icare.model.ModelAppointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterAdminIncoming: RecyclerView.Adapter<AdapterAdminIncoming.HolderAppointment> {
    private val context: Context
    public var appointmentArrayList: ArrayList<ModelAppointment>

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private lateinit var binding: RowAdminIncomingBinding

    constructor(context: Context, appointmentArrayList: ArrayList<ModelAppointment>) {
        this.context = context
        this.appointmentArrayList = appointmentArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAppointment {
        binding = RowAdminIncomingBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderAppointment(binding.root)
    }

    override fun getItemCount(): Int {
        return appointmentArrayList.size
    }

    override fun onBindViewHolder(holder: HolderAppointment, position: Int) {
        val model = appointmentArrayList[position]
        val id = model.appointmentId
        val userId = model.userId
        val appointmentTime = model.appointmentTime
        val updateTime = model.updateTime
        val updateUserId = model.updateUserId
        val status = model.status
        val description = model.description
        val comment = model.comment

        holder.timeTv.text = "Appointment Time: $appointmentTime"
        holder.descriptionTv.text = "Description: $description"

        val ref = FirebaseDatabase.getInstance().getReference("Users").
        ref.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = "" + snapshot.child("name").value

                    holder.nameTv.text = "Name: $name"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    println("The read failed: " + databaseError.code)
                }
            })

        holder.chatBtn.setOnClickListener {
            val intent = Intent(context, AdminChatActivity::class.java)
            intent.putExtra("uid", userId)
            context.startActivity(intent)
        }

        holder.updateBtn.setOnClickListener {
            val intent = Intent(context, AdminUpdateAppointmentActivity::class.java)
            intent.putExtra("uid", id)
            context.startActivity(intent)
        }

    }

    private fun updateAppointment() {
    }

    private fun chat() {
    }

    inner class HolderAppointment(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameTv: TextView = binding.nameTv
        var timeTv: TextView = binding.appointmentTimeTv
        var descriptionTv: TextView = binding.descriptionTv
        var updateBtn: Button = binding.updateBtn
        var chatBtn: Button = binding.chatBtn
    }

}
