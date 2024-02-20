package com.example.icare

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import com.example.icare.adapter.AdapterUserHistory
import com.example.icare.databinding.ActivityAppointmentBinding
import com.example.icare.model.ModelAppointment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore

class AppointmentActivity : AppCompatActivity() {
    private var binding: ActivityAppointmentBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private var updateUserId: String = ""

    private lateinit var appointmentArrayList: ArrayList<ModelAppointment>

    private lateinit var adapterUserHistory: AdapterUserHistory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        binding = ActivityAppointmentBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")

        binding!!.titleDashboardUser.text = name
        binding!!.subtitleDashboardUser.text = email

        updateUserId = loadRequest()

        loadHistory()

        binding!!.addBtn.setOnClickListener{
            startActivity(Intent(this@AppointmentActivity, RequestAppointmentActivity::class.java))
        }

        binding!!.cancelBtn.setOnClickListener {
            cancelRequest()
        }

        binding!!.chatBtn.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("updateUserId", updateUserId)
            startActivity(intent)
        }

        val headerView = binding!!.navigationView.getHeaderView(0)
        val nameTv : TextView = headerView.findViewById(R.id.nameTv)
        val emailTv : TextView = headerView.findViewById(R.id.emailTv)

        nameTv.text = name
        emailTv.text = email

        val drawer = binding!!.myDrawerLayout
        val imageButton = binding!!.menuBtn
        imageButton.setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        val navigationView = binding!!.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            if (id == R.id.nav_home) {
                startActivity(Intent(this@AppointmentActivity, UserHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                drawer.closeDrawer(GravityCompat.START)
            } else if (id == R.id.nav_profile) {
                val intent = Intent(this@AppointmentActivity, ProfileActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.nav_aboutus) {
                startActivity(Intent(this@AppointmentActivity, AboutUsActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AppointmentActivity, HomeActivity::class.java)
                firebaseAuth!!.signOut()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            true
        }
    }

    private fun loadRequest(): String {
        var count = 0

        val ref = db.collection("Appointment").whereIn("status", listOf("P", "A")).whereEqualTo("userId", firebaseAuth!!.uid)
        ref.addSnapshotListener { result, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (result != null && !result.isEmpty) {
                binding!!.requestRl.visibility = View.VISIBLE
                binding!!.noRequestTv.visibility = View.GONE
                binding!!.addBtn.visibility = View.GONE

                for (document in result) {
                    count++

                    val data = document.data

                    val time = data["appointmentTime"]
                    val description = data["description"]
                    val status = data["status"]


                    binding!!.dateTv.text = "Appointment Time: $time"
                    binding!!.descriptionTv.text = "Description: $description"
                    if (status == "P") {
                        binding!!.statusTv.text = "Status: Pending"
                        binding!!.cancelBtn.visibility = View.VISIBLE
                        binding!!.chatBtn.visibility = View.GONE
                    } else if (status == "A") {
                        binding!!.statusTv.text = "Status: Accepted"
                        binding!!.cancelBtn.visibility = View.GONE
                        binding!!.chatBtn.visibility = View.VISIBLE
                        updateUserId = data["updateUserId"].toString()

                    }
                }
            } else {
                binding!!.noRequestTv.visibility = View.VISIBLE
                binding!!.requestRl.visibility = View.GONE
                binding!!.addBtn.visibility = View.VISIBLE
            }
        }

        return updateUserId
    }

    private fun loadHistory() {
        appointmentArrayList = ArrayList()
        var count = 0
        val ref = db.collection("Appointment").whereEqualTo("userId", firebaseAuth!!.currentUser!!.uid).whereEqualTo("status", "AF")
            .whereEqualTo("userId", firebaseAuth!!.uid)
        ref.addSnapshotListener { result, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (result != null && !result.isEmpty) {
                appointmentArrayList.clear()

                binding!!.appointmentRv.visibility = View.VISIBLE
                binding!!.noHistoryTv.visibility = View.GONE

                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelAppointment::class.java)
                    if(model.status == "AF") {
                        appointmentArrayList.add(model!!)
                    }
                }

                adapterUserHistory =
                    AdapterUserHistory(this@AppointmentActivity, appointmentArrayList)

                binding!!.appointmentRv.adapter = adapterUserHistory

            } else {
                binding!!.noHistoryTv.visibility = View.VISIBLE
                binding!!.appointmentRv.visibility = View.GONE
            }
        }
    }

    private fun cancelRequest() {
        val builder= AlertDialog.Builder(this@AppointmentActivity)
        builder.setTitle("Cancel")
            .setMessage("Are you sure you want to cancel your request?")
            .setPositiveButton("Confirm") {a, d->
                db.collection("Appointment")
                    .whereEqualTo("status", "P")
                    .whereEqualTo("userId", firebaseAuth!!.uid)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            db.collection("Appointment").document(document.id)
                                .update("status", "C")
                                .addOnSuccessListener {
                                    val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                    val userName = sharedPref.getString("name", "X")
                                            val log = hashMapOf(
                                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                                "time" to System.currentTimeMillis(),
                                                "detail" to "Request (${document.id}) made by $userName has been cancelled by themselves",
                                                "level" to 2
                                            )

                                            db.collection("Log").add(log)
                                                .addOnSuccessListener {

                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this, "Failed to upload item!",
                                                        Toast.LENGTH_SHORT).show()
                                                }
                                    Toast.makeText(this@AppointmentActivity, "Requested cancelled.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    // Update failed
                                    Toast.makeText(this@AppointmentActivity, "Unable to cancel due to ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Document retrieval failed
                        Toast.makeText(this@AppointmentActivity, "Unable to retrieve documents due to ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel") {a, d->
                a.dismiss()
            }.show()
    }

    private fun clearSharedPreferences() {
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove("email")
        editor.remove("name")
        editor.remove("role")
        editor.apply()
    }
}