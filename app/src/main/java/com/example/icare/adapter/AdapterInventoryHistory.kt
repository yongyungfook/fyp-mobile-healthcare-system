package com.example.icare.adapter

import android.content.Context
import android.graphics.text.LineBreaker
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.databinding.RowInventoryHistoryBinding
import com.example.icare.filters.FilterPrescription
import com.example.icare.model.ModelPrescription
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat

class AdapterInventoryHistory: RecyclerView.Adapter<AdapterInventoryHistory.HolderPrescription>, Filterable {
    private val context: Context
    public var prescriptionArrayList: ArrayList<ModelPrescription>
    private var filterList: ArrayList<ModelPrescription>

    private var filter: FilterPrescription? = null

    private var db = Firebase.firestore
    private val storage = FirebaseStorage.getInstance()
    private lateinit var binding: RowInventoryHistoryBinding

    constructor(context: Context, prescriptionArrayList: ArrayList<ModelPrescription>) {
        this.context = context
        this.prescriptionArrayList = prescriptionArrayList
        this.filterList = prescriptionArrayList

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPrescription {
        binding = RowInventoryHistoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPrescription(binding.root)
    }

    override fun getItemCount(): Int {
        return prescriptionArrayList.size
    }

    override fun onBindViewHolder(holder: HolderPrescription, position: Int) {
        val model = prescriptionArrayList[position]
        val id = model.itemId
        val itemName = model.itemName
        val name = model.name
        val updateUserId = model.updateUserId
        val oldStock = model.oldStock
        val newStock = model.newStock
        val oldName = model.oldName
        val newName = model.newName
        val oldDescription = model.oldDescription
        val newDescription = model.newDescription
        val appointmentId = model.appointmentId
        val updateTimestamp = model.updateTime
        val type = model.type

        val updateDate = java.util.Date(updateTimestamp)
        val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
        val updateTime = format.format(updateDate)

        holder.timeTv.text = updateTime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.updateTv.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
        }
        if(type == "P") {
            holder.updateTv.text = "A stock of $itemName is issued to ${name.substringAfter("by ")} ($appointmentId), the updated stock left is $newStock"
        } else if(type == "A") {
            holder.updateTv.text = "${model.detail}"
        } else {
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(updateUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = "" + snapshot.child("name").value

                        if(type == "N") {
                            holder.updateTv.text =
                                "$name updated the name of the item, from $oldName to $newName"
                        } else if(type == "S") {
                            holder.updateTv.text =
                                "$name updated the stock of the item $itemName, from $oldStock to $newStock"
                        } else if(type == "D") {
                            holder.updateTv.text =
                                "$name updated the description of the item $itemName, from $oldDescription to $newDescription"
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        println("The read failed: " + databaseError.code)
                    }
                })
        }
    }
    inner class HolderPrescription(itemView: View): RecyclerView.ViewHolder(itemView) {
        var updateTv: TextView = binding.updateTv
        var timeTv: TextView = binding.timeTv
    }

    override fun getFilter(): Filter {
        if(filter == null) {
            filter = FilterPrescription(filterList, this)
        }

        return filter as FilterPrescription
    }


}
