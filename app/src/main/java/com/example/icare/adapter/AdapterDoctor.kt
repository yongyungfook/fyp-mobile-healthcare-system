package com.example.icare.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.databinding.ItemDoctorBinding
import com.example.icare.databinding.ItemNewsBinding
import com.example.icare.model.ModelDoctor
import com.example.icare.model.ModelNews

class AdapterDoctor(context: Context, private val doctorItems: List<ModelDoctor>) :
    RecyclerView.Adapter<AdapterDoctor.DoctorViewHolder>() {
    private val context: Context = context
    private lateinit var binding: ItemDoctorBinding

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = binding.doctorIv
        val name: TextView = binding.nameTv
        val job: TextView = binding.jobTv
        val image2: ImageView = binding.doctorIv2
        val name2: TextView = binding.nameTv2
        val job2: TextView = binding.jobTv2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        binding = ItemDoctorBinding.inflate(LayoutInflater.from(context), parent, false)
        return DoctorViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val newsItem = doctorItems[position]
        holder.image.setImageResource(newsItem.imageResId)
        holder.image2.setImageResource(newsItem.imageResId2)
        holder.name.text = newsItem.name
        holder.name2.text = newsItem.name2
        holder.job.text = newsItem.job
        holder.job2.text = newsItem.job2
    }

    override fun getItemCount(): Int {
        return doctorItems.size
    }
}