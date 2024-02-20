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
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.AdminAppointmentActivity
import com.example.icare.AdminEditInventoryActivity
import com.example.icare.AdminInventoryActivity
import com.example.icare.AdminViewProfileActivity
import com.example.icare.databinding.RowAccountBinding
import com.example.icare.databinding.RowInventoryBinding
import com.example.icare.filters.FilterAccount
import com.example.icare.filters.FilterInventory
import com.example.icare.model.ModelAccount
import com.example.icare.model.ModelInventory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class AdapterInventory: RecyclerView.Adapter<AdapterInventory.HolderInventory>, Filterable {
    private val context: Context
    public var inventoryArrayList: ArrayList<ModelInventory>
    private var filterList: ArrayList<ModelInventory>
    private var firebaseAuth: FirebaseAuth? = FirebaseAuth.getInstance()
    private var filter: FilterInventory? = null

    private var db = Firebase.firestore
    private val storage = FirebaseStorage.getInstance()
    private lateinit var binding: RowInventoryBinding

    constructor(context: Context, inventoryArrayList: ArrayList<ModelInventory>) {
        this.context = context
        this.inventoryArrayList = inventoryArrayList
        this.filterList = inventoryArrayList

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderInventory {
        binding = RowInventoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderInventory(binding.root)
    }

    override fun getItemCount(): Int {
        return inventoryArrayList.size
    }

    override fun onBindViewHolder(holder: HolderInventory, position: Int) {
        val model = inventoryArrayList[position]
        val id = model.itemId
        val name = model.itemName
        val image = model.image
        val stock = model.stock
        val description = model.description

        holder.nameTv.text = "Name: $name"
        holder.stockTv.text = "Stock: $stock"
        holder.descriptionTv.text = "Description: $description"

        if (image != null) {
            Picasso.get().load(image).into(holder.itemIv)
        }

        holder.updateBtn.setOnClickListener {
            val intent = Intent(context, AdminEditInventoryActivity::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }

        holder.deleteBtn.setOnClickListener {
            val builder= AlertDialog.Builder(context)
            builder.setTitle("Delete")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Confirm") {a, d->
                    Toast.makeText(context, "Deleting", Toast.LENGTH_SHORT).show()

                    db.collection("Inventory")
                        .document(id)
                        .delete()
                        .addOnSuccessListener {
                            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            val userName = sharedPref.getString("name", "X")

                            val log = hashMapOf(
                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                "time" to System.currentTimeMillis(),
                                "detail" to "Item named $name ($id) has been deleted by $userName",
                                "level" to 2
                            )

                            db.collection("Log").add(log)

                            val item = hashMapOf(
                                "detail" to "$userName deleted an item\n" +
                                        "Name: ${name}\n" +
                                        "Description: ${description}\n" +
                                        "Stock: $stock",
                                "updateTime" to System.currentTimeMillis(),
                                "updateUserId" to firebaseAuth!!.currentUser!!.uid,
                                "itemId" to id,
                                "itemName" to name,
                                "type" to "A",

                                )

                            db.collection("Prescription").add(item)

                            Toast.makeText(context, "Item deleted.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // Document retrieval failed
                            Toast.makeText(context, "Unable to retrieve documents due to ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel") {a, d->
                    a.dismiss()
                }.show()
        }

    }
    inner class HolderInventory(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameTv: TextView = binding.nameTv
        var descriptionTv: TextView = binding.descriptionTv
        var stockTv: TextView = binding.stockTv
        var itemIv: ImageView = binding.itemIv
        var updateBtn: Button = binding.updateBtn
        var deleteBtn: Button = binding.deleteBtn
    }

    override fun getFilter(): Filter {
        if(filter == null) {
            filter = FilterInventory(filterList, this)
        }

        return filter as FilterInventory
    }


}
