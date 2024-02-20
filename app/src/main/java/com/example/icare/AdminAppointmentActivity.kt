package com.example.icare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.icare.adapter.AdapterAdminIncoming
import com.example.icare.adapter.AdapterAdminRequest
import com.example.icare.adapter.AdapterAdminRequestHistory
import com.example.icare.databinding.ActivityAdminAppointmentBinding
import com.example.icare.model.ModelAppointment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import okhttp3.internal.notify

class AdminAppointmentActivity : AppCompatActivity() {
    private var binding: ActivityAdminAppointmentBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private lateinit var requestArrayList: ArrayList<ModelAppointment>

    private lateinit var incomingArrayList: ArrayList<ModelAppointment>

    private lateinit var adapterAdminIncoming: AdapterAdminIncoming

    private lateinit var adapterAdminRequest: AdapterAdminRequest
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_appointment)

        binding = ActivityAdminAppointmentBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        loadRequest()

        loadIncoming()

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val role = sharedPref.getString("role", "defaultRole")
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")

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
                startActivity(Intent(this@AdminAppointmentActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                drawer.closeDrawer(GravityCompat.START)
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminAppointmentActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminAppointmentActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminAppointmentActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminAppointmentActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminAppointmentActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminAppointmentActivity, HomeActivity::class.java)
                firebaseAuth!!.signOut()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            true
        }

        binding!!.titleDashboardUser.text = name
        binding!!.subtitleDashboardUser.text = email

        var userRoleIsOwner = false

        userRoleIsOwner = role == "O"

        val logMenuItem = navigationView.menu.findItem(R.id.nav_log)
        val moderationMenuItem = navigationView.menu.findItem(R.id.nav_moderation)
        val reportMenuItem = navigationView.menu.findItem(R.id.nav_report)
        logMenuItem.isVisible = userRoleIsOwner
        moderationMenuItem.isVisible = userRoleIsOwner
        reportMenuItem.isVisible = userRoleIsOwner
    }

    private fun loadIncoming() {
        var count = 0

        incomingArrayList = ArrayList()

        val ref = db.collection("Appointment").whereEqualTo("status", "A").orderBy("appointmentTime", Query.Direction.ASCENDING)
        ref.addSnapshotListener { result, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (result != null && !result.isEmpty) {
                incomingArrayList.clear()

                binding!!.noIncomingTv.visibility = View.GONE
                binding!!.incomingRv.visibility = View.VISIBLE

                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelAppointment::class.java)
                    model.appointmentId = document.id
                    count++
                    incomingArrayList.add(model!!)
                }
                adapterAdminIncoming =
                    AdapterAdminIncoming(this@AdminAppointmentActivity, incomingArrayList)

                binding!!.incomingRv.adapter = adapterAdminIncoming
            } else {
                binding!!.noIncomingTv.visibility = View.VISIBLE
                binding!!.incomingRv.visibility = View.GONE
            }
        }
    }

    private fun loadRequest() {
        var count = 0
        requestArrayList = ArrayList()
        val ref = db.collection("Appointment").whereEqualTo("status", "P").orderBy("requestTime", Query.Direction.DESCENDING)
        ref.addSnapshotListener { result, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (result != null && !result.isEmpty) {
                requestArrayList.clear()

                binding!!.noRequestTv.visibility = View.GONE
                binding!!.requestRv.visibility = View.VISIBLE

                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelAppointment::class.java)
                    count++
                    requestArrayList.add(model!!)
                }


                adapterAdminRequest =
                    AdapterAdminRequest(this@AdminAppointmentActivity, requestArrayList)

                binding!!.requestRv.adapter = adapterAdminRequest

            } else {
                binding!!.noRequestTv.visibility = View.VISIBLE
                binding!!.requestRv.visibility = View.GONE

            }
        }
    }
    fun onClick(v: View?) {
        startActivity(Intent(this@AdminAppointmentActivity, AdminRequestHistoryActivity::class.java))
    }

    fun onClick2(v: View?) {
        startActivity(Intent(this@AdminAppointmentActivity, AdminAppointmentHistoryActivity::class.java))
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