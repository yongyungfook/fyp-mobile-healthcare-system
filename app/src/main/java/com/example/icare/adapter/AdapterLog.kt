package com.example.icare.adapter

import android.app.ProgressDialog
import android.content.Context
import android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.databinding.RowLogBinding
import com.example.icare.filters.FilterLog
import com.example.icare.model.ModelAppointment
import com.example.icare.model.ModelLog
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat

class AdapterLog(context: Context, logArrayList: ArrayList<ModelLog>) :
    RecyclerView.Adapter<AdapterLog.HolderLog>(), Filterable {

    private val context: Context = context
    public var logArrayList: ArrayList<ModelLog> = logArrayList
    private var filterList: ArrayList<ModelLog> = logArrayList
    private var filter: FilterLog? = null

    private var progressDialog: ProgressDialog? = null

    private var db = Firebase.firestore

    private var isSelectMode: Boolean = false
    private var selectedItems: HashSet<Int> = HashSet()

    private lateinit var binding: RowLogBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderLog {
        binding = RowLogBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderLog(binding.root)
    }

    override fun getItemCount(): Int {
        return logArrayList.size
    }

    override fun onBindViewHolder(holder: HolderLog, position: Int) {
        val model = logArrayList[position]
        val id = model.logId
        val uid = model.userId
        val detail = model.detail
        val timestamp = model.time
        val level = model.level

        val updateDate = java.util.Date(timestamp)
        val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
        val updateTime = format.format(updateDate)

        holder.timeTv.text = updateTime
        holder.logTv.text = detail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.logTv.justificationMode = JUSTIFICATION_MODE_INTER_WORD
        }

        // Toggle visibility of checkboxes based on selection mode
        holder.checkBox.visibility = if (isSelectMode) View.VISIBLE else View.GONE

        // Handle checkbox state
        holder.checkBox.isChecked = selectedItems.contains(position)

        // Handle checkbox click
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
            }
        }
    }

    inner class HolderLog(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var logTv: TextView = binding.logTv
        var timeTv: TextView = binding.timeTv
        var checkBox: CheckBox = binding.checkbox
    }
    fun toggleSelectMode() {
        isSelectMode = !isSelectMode
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun deleteSelectedItems() {
        if (selectedItems.isNotEmpty()) {
            progressDialog = ProgressDialog(context)
            progressDialog!!.setTitle("Please Wait...")
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.setMessage("Deleting...")
            progressDialog!!.show()

            val selectedLogs = selectedItems.map { logArrayList[it] }

            val logCollection = db.collection("Log")
            var deletionCount = 0

            for (selectedLog in selectedLogs) {
                logCollection.document(selectedLog.logId).get()
                    .addOnSuccessListener { documentSnapshot ->
                        val model = documentSnapshot.toObject(ModelLog::class.java)
                        model!!.logId = documentSnapshot.id
                        if (model?.logId == selectedLog.logId) {
                            // Delete the document
                            logCollection.document(model.logId!!).delete()
                                .addOnSuccessListener {
                                    deletionCount++
                                    if (deletionCount == selectedLogs.size) {
                                        // All deletions are completed, dismiss ProgressDialog
                                        progressDialog!!.dismiss()
                                        isSelectMode = !isSelectMode
                                        notifyDataSetChanged()
                                    }
                                }
                        }
                    }
            }
        } else {
            isSelectMode = !isSelectMode
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterLog(filterList, this)
        }
        return filter as FilterLog
    }
}