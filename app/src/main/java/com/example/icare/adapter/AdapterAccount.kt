package com.example.icare.adapter

import android.app.AlertDialog
import android.app.ProgressDialog
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
import com.example.icare.AdminInventoryActivity
import com.example.icare.AdminViewProfileActivity
import com.example.icare.databinding.RowAccountBinding
import com.example.icare.filters.FilterAccount
import com.example.icare.model.ModelAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore

class AdapterAccount: RecyclerView.Adapter<AdapterAccount.HolderAccount>, Filterable {
    private val context: Context
    public var accountArrayList: ArrayList<ModelAccount>
    private var filterList: ArrayList<ModelAccount>

    private var db = Firebase.firestore
    private var progressDialog: ProgressDialog? = null

    private var filter: FilterAccount? = null
    private lateinit var binding: RowAccountBinding

    constructor(context: Context, accountArrayList: ArrayList<ModelAccount>) {
        this.context = context
        this.accountArrayList = accountArrayList
        this.filterList = accountArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAccount {
        binding = RowAccountBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderAccount(binding.root)
    }

    override fun getItemCount(): Int {
        return accountArrayList.size
    }

    override fun onBindViewHolder(holder: HolderAccount, position: Int) {
        val model = accountArrayList[position]
        val uid = model.uid
        val name = model.name
        val role = model.role
        val email = model.email
        val gender = model.gender
        val regDate = model.regDate
        val phoneNumber = model.phoneNumber
        var roleName = ""

        if(role != "UB" && role != "DB" && role != "AB") {
            if(role == "U") {
                holder.nameTv.text = "$name (user)"
                roleName = "user"
            } else if(role == "D") {
                holder.nameTv.text = "$name (doctor)"
                roleName = "doctor"
            } else {
                holder.nameTv.text = "$name (staff)"
                roleName = "staff"
            }
            holder.banBtn.setOnClickListener {
                val builder= AlertDialog.Builder(context)
                builder.setTitle("Ban")
                    .setMessage("Are you sure you want to ban this user: $name")
                    .setPositiveButton("Confirm") {a, d->
                        progressDialog = ProgressDialog(context)
                        progressDialog!!.setTitle("Please Wait...")
                        progressDialog!!.setCanceledOnTouchOutside(false)
                        progressDialog!!.setMessage("Banning...")
                        progressDialog!!.show()

                        banAccount(model, holder, role.toString())

                        val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val userName = sharedPref.getString("name", "X")

                        val log = hashMapOf(
                            "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                            "time" to System.currentTimeMillis(),
                            "detail" to "$name ($roleName) has been banned by $userName",
                            "level" to 3
                        )

                        db.collection("Log").add(log)

                        progressDialog!!.dismiss()
                        Toast.makeText(context, "Account banned.", Toast.LENGTH_SHORT).show()
                        notifyDataSetChanged();
                    }
                    .setNegativeButton("Cancel") {a, d->
                        a.dismiss()
                    }.show()
            }

        } else {
            if(role == "UB") {
                holder.nameTv.text = "$name (user)"
                roleName = "user"
            } else if(role == "AB") {
                holder.nameTv.text = "$name (staff)"
                roleName = "staff"
            } else {
                holder.nameTv.text = "$name (doctor)"
                roleName = "doctor"
            }
            holder.banBtn.text = "Unban"
            holder.banBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#90EE90"))
            holder.banBtn.setOnClickListener {
                val builder= AlertDialog.Builder(context)
                builder.setTitle("Unban")
                    .setMessage("Are you sure you want to unban this user: $name")
                    .setPositiveButton("Confirm") {a, d->
                        progressDialog = ProgressDialog(context)
                        progressDialog!!.setTitle("Please Wait...")
                        progressDialog!!.setCanceledOnTouchOutside(false)
                        progressDialog!!.setMessage("Unbanning...")
                        progressDialog!!.show()
                        unbanAccount(model, holder, role.toString())

                        val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val userName = sharedPref.getString("name", "X")

                        val log = hashMapOf(
                            "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                            "time" to System.currentTimeMillis(),
                            "detail" to "$name ($roleName) has been unbanned by $userName",
                            "level" to 3
                        )

                        db.collection("Log").add(log)
                            .addOnSuccessListener {

                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to upload item!",
                                    Toast.LENGTH_SHORT).show()
                            }

                        progressDialog!!.dismiss()
                        notifyDataSetChanged();
                    }
                    .setNegativeButton("Cancel") {a, d->
                        a.dismiss()
                    }.show()
            }
        }

        holder.viewBtn.setOnClickListener {
            val intent = Intent(context, AdminViewProfileActivity::class.java)
            intent.putExtra("uid", uid)
            context.startActivity(intent)
        }

    }

    private fun banAccount(model: ModelAccount, holder: HolderAccount, role: String) {
        val uid = model.uid

        val hashMap = HashMap<String, Any>()
        hashMap["role"] = "$role" + "B"

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid.toString()).updateChildren(hashMap)
            .addOnSuccessListener { //Data added into DB
            }
            .addOnFailureListener { e -> //Data Entry Failed
                Toast.makeText(context, "Unable to ban due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unbanAccount(model: ModelAccount, holder: HolderAccount, role: String) {
        val uid = model.uid

        val hashMap = HashMap<String, Any>()
        hashMap["role"] = role.first().toString()

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid.toString()).updateChildren(hashMap)
            .addOnSuccessListener { //Data added into DB
                Toast.makeText(context, "Account unbanned.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e -> //Data Entry Failed
                Toast.makeText(context, "Unable to unban due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    inner class HolderAccount(itemView: View): RecyclerView.ViewHolder(itemView) {
        var nameTv: TextView = binding.nameTv
        var viewBtn: Button = binding.viewBtn
        var banBtn: Button = binding.banBtn
    }

    override fun getFilter(): Filter {
        if(filter == null) {
            filter = FilterAccount(filterList, this)
        }

        return filter as FilterAccount
    }
}
