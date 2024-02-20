package com.example.icare.adapter

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.databinding.RowAdminAppointmentHistoryBinding
import com.example.icare.databinding.RowAdminRequestHistoryBinding
import com.example.icare.filters.FilterAppointment
import com.example.icare.filters.FilterRequest
import com.example.icare.model.ModelAppointment
import com.example.icare.model.ModelInventory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat

class AdapterAdminAppointmentHistory: RecyclerView.Adapter<AdapterAdminAppointmentHistory.HolderAppointment>, Filterable {
    private val context: Context
    public var appointmentArrayList: ArrayList<ModelAppointment>
    private var filterList: ArrayList<ModelAppointment>
    private var filter: FilterAppointment? = null
    private lateinit var binding: RowAdminAppointmentHistoryBinding

    constructor(context: Context, appointmentArrayList: ArrayList<ModelAppointment>) {
        this.context = context
        this.appointmentArrayList = appointmentArrayList
        this.filterList = appointmentArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAppointment {
        binding = RowAdminAppointmentHistoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderAppointment(binding.root)
    }

    override fun getItemCount(): Int {
        return appointmentArrayList.size
    }

    override fun onBindViewHolder(holder: HolderAppointment, position: Int) {
        var prescriptionList = ArrayList<String>()

        val model = appointmentArrayList[position]
        val uid = model.appointmentId
        val userId = model.userId
        val appointmentTime = model.appointmentTime
        val description = model.description
        val doctor = model.doctor
        val comment = model.comment
        val reason = model.reason
        val updateUserId = model.updateUserId
        val status = model.status

        holder.appointmentTimeTv.text = "Appointment Time: $appointmentTime"
        holder.descriptionTv.text = "Description: $description"
        holder.uidTv.text = "UID: $uid"

        if (status != "AC") {

            holder.doctorTv.visibility = View.VISIBLE
            holder.statusTv.visibility = View.GONE
            holder.doctorTv.text = "Doctor: $doctor"
            holder.commentTv.text = "Comment: $comment"
            holder.statusTv.layoutParams = (holder.statusTv.layoutParams as RelativeLayout.LayoutParams).apply { width = 0; height = 0 }

            var db = Firebase.firestore

            db.collection("Prescription").whereEqualTo("appointmentId", uid)
                .addSnapshotListener { result, e ->
                if (e != null) {
                    // Handle errors
                    return@addSnapshotListener
                }

                if (result != null && !result.isEmpty) {
                    for (document in result) {
                        // Access document data using document.data
                        val prescription = document.get("itemName")
                        prescriptionList.add(prescription.toString())
                    }
                    holder.prescriptionTv.text = "Prescription: " + prescriptionList.joinToString(separator = ", ")
                } else {
                    holder.prescriptionTv.text = "Prescription: -"
                }
            }
        } else {
            holder.commentTv.text = "Reason: $reason"
            holder.doctorTv.visibility = View.GONE
            holder.statusTv.visibility = View.VISIBLE
            holder.prescriptionTv.text = "Prescription: -"

            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(updateUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = "" + snapshot.child("name").value

                        holder.statusTv.text = "Status: Cancelled by $name"
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        println("The read failed: " + databaseError.code)
                    }
                })

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

    }
    inner class HolderAppointment(itemView: View): RecyclerView.ViewHolder(itemView) {
        var uidTv: TextView = binding.uidTv
        var nameTv: TextView = binding.nameTv
        var appointmentTimeTv: TextView = binding.appointmentTimeTv
        var doctorTv: TextView = binding.doctorTv
        var descriptionTv: TextView = binding.descriptionTv
        var commentTv: TextView = binding.commentTv
        var statusTv: TextView = binding.statusTv
        var prescriptionTv: TextView = binding.prescriptionTv
    }

    override fun getFilter(): Filter {
        if(filter == null) {
            filter = FilterAppointment(filterList, this)
        }

        return filter as FilterAppointment
    }
}
