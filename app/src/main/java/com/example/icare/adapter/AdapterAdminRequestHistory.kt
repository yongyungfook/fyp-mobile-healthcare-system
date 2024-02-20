package com.example.icare.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.databinding.RowAdminRequestHistoryBinding
import com.example.icare.filters.FilterAccount
import com.example.icare.filters.FilterRequest
import com.example.icare.model.ModelAccount
import com.example.icare.model.ModelAppointment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat

class AdapterAdminRequestHistory: RecyclerView.Adapter<AdapterAdminRequestHistory.HolderAppointment>, Filterable {
    private val context: Context
    public var appointmentArrayList: ArrayList<ModelAppointment>
    private var filterList: ArrayList<ModelAppointment>
    private var filter: FilterRequest? = null

    private lateinit var binding: RowAdminRequestHistoryBinding

    constructor(context: Context, appointmentArrayList: ArrayList<ModelAppointment>) {
        this.context = context
        this.appointmentArrayList = appointmentArrayList
        this.filterList = appointmentArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAppointment {
        binding = RowAdminRequestHistoryBinding.inflate(LayoutInflater.from(context), parent, false)
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
        val requestTimestamp = model.requestTime
        val updateTimestamp = model.updateTime
        val updateUserId = model.updateUserId
        val status = model.status
        val description = model.description
        val reason = model.reason

        val requestDate = java.util.Date(requestTimestamp)
        val updateDate = java.util.Date(updateTimestamp)
        val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
        val requestTime = format.format(requestDate)
        val updateTime = format.format(updateDate)

        holder.uidTv.text = "UID: $id"
        holder.scheduleTimeTv.text = "Schedule Time: $appointmentTime"
        holder.descriptionTv.text = "Description: $description"
        holder.submitTimeTv.text = "Submit Time: $requestTime"
        holder.updateTimeTv.text = "Update Time: $updateTime"

        if(status == "A" || status == "AF") {
            holder.statusTv.text = "Status: Accepted"
            holder.reasonTv.text = "Reason: -"

        } else if(status == "C") {
            holder.statusTv.text = "Status: Cancelled"
            holder.reasonTv.text = "Reason: -"
            holder.updateTv.layoutParams = (holder.updateTv.layoutParams as RelativeLayout.LayoutParams).apply { width = 0; height = 0 }
            holder.updateTimeTv.layoutParams = (holder.updateTimeTv.layoutParams as RelativeLayout.LayoutParams).apply { width = 0; height = 0 }

        } else if(status == "CA") {
            holder.statusTv.text = "Status: Cancelled by admin"
            holder.reasonTv.text = "Reason: $reason"

        } else {
            holder.statusTv.text = "Status: Rejected"
            holder.reasonTv.text = "Reason: $reason"
        }

        val ref = FirebaseDatabase.getInstance().getReference("Users")
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

        val ref2 = FirebaseDatabase.getInstance().getReference("Users")
        ref2.child(updateUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = "" + snapshot.child("name").value

                    holder.updateTv.text = "Name: $name"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    println("The read failed: " + databaseError.code)
                }
            })

    }

    private var db = Firebase.firestore

    inner class HolderAppointment(itemView: View): RecyclerView.ViewHolder(itemView) {
        var uidTv: TextView = binding.uidTv
        var nameTv: TextView = binding.nameTv
        var scheduleTimeTv: TextView = binding.scheduleTimeTv
        var submitTimeTv: TextView = binding.submitTimeTv
        var descriptionTv: TextView = binding.descriptionTv
        var statusTv: TextView = binding.statusTv
        var reasonTv: TextView = binding.reasonTv
        var updateTv: TextView = binding.updateTv
        var updateTimeTv: TextView = binding.updateTimeTv
    }

    override fun getFilter(): Filter {
        if(filter == null) {
            filter = FilterRequest(filterList, this)
        }

        return filter as FilterRequest
    }

}
