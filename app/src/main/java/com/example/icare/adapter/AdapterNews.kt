package com.example.icare.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.databinding.ItemNewsBinding
import com.example.icare.model.ModelNews

class AdapterNews(context: Context, private val newsItems: List<ModelNews>) :
    RecyclerView.Adapter<AdapterNews.NewsViewHolder>() {
    private val context: Context = context
    private lateinit var binding: ItemNewsBinding

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val newsImage: ImageView = binding.newsImage
        val newsTitle: TextView = binding.newsTitle
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        binding = ItemNewsBinding.inflate(LayoutInflater.from(context), parent, false)
        return NewsViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsItems[position]
        holder.newsImage.setImageResource(newsItem.imageResId)
        holder.newsTitle.text = newsItem.title
    }

    override fun getItemCount(): Int {
        return newsItems.size
    }
}