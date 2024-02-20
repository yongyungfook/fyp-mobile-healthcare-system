package com.example.icare.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.AdminViewProfileActivity
import com.example.icare.databinding.RowUserHistoryBinding
import com.example.icare.filters.FilterAccount
import com.example.icare.model.ModelAppointment
import com.google.firebase.database.FirebaseDatabase

class AdapterUserHistory: RecyclerView.Adapter<AdapterUserHistory.HolderUserHistory> {
    private val context: Context
    public var appointmentArrayList: ArrayList<ModelAppointment>

    private var filter: FilterAccount? = null
    private lateinit var binding: RowUserHistoryBinding

    constructor(context: Context, appointmentArrayList: ArrayList<ModelAppointment>) {
        this.context = context
        this.appointmentArrayList = appointmentArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderUserHistory {
        binding = RowUserHistoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderUserHistory(binding.root)
    }

    override fun getItemCount(): Int {
        return appointmentArrayList.size
    }

    override fun onBindViewHolder(holder: HolderUserHistory, position: Int) {
        val model = appointmentArrayList[position]
        val time = model.appointmentTime
        val doctor = model.doctor
        val comment = model.comment
        val status = model.status

        holder.timeTv.text = "Appointment Time: $time"
        holder.doctorTv.text = "Doctor: $doctor"
        holder.commentTv.text = "Comment: $comment"
    }


    inner class HolderUserHistory(itemView: View): RecyclerView.ViewHolder(itemView) {
        var timeTv: TextView = binding.dateTv
        var doctorTv: TextView = binding.doctorTv
        var commentTv: TextView = binding.commentTv
    }

}
