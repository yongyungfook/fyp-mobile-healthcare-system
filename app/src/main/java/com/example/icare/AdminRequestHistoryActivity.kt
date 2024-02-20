package com.example.icare

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.GravityCompat
import com.example.icare.adapter.AdapterAdminIncoming
import com.example.icare.adapter.AdapterAdminRequestHistory
import com.example.icare.databinding.ActivityAdminAppointmentBinding
import com.example.icare.databinding.ActivityAdminRequestHistoryBinding
import com.example.icare.model.ModelAppointment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class AdminRequestHistoryActivity : AppCompatActivity() {
    private var binding: ActivityAdminRequestHistoryBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private lateinit var requestArrayList: ArrayList<ModelAppointment>

    private lateinit var adapterAdminRequest: AdapterAdminRequestHistory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_request_history)

        binding = ActivityAdminRequestHistoryBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        loadRequest()

        binding!!.searchEt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterAdminRequest.filter!!.filter(s)
                } catch (e: Exception) {

                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

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
                startActivity(Intent(this@AdminRequestHistoryActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@AdminRequestHistoryActivity, AdminAppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminRequestHistoryActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminRequestHistoryActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminRequestHistoryActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminRequestHistoryActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminRequestHistoryActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminRequestHistoryActivity, HomeActivity::class.java)
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

    private fun loadRequest() {
        var count = 0
        requestArrayList = ArrayList()
        val ref = db.collection("Appointment").orderBy("requestTime", Query.Direction.DESCENDING)
        ref.addSnapshotListener { result, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (result != null && !result.isEmpty) {
                count = 0

                requestArrayList = ArrayList()

                binding!!.noRequestTv.visibility = View.GONE
                binding!!.requestRv.visibility = View.VISIBLE

                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelAppointment::class.java)
                    if(model.status == "AF" || model.status == "AC" || model.status == "R" || model.status == "C" || model.status == "A") {
                        count++
                        model.appointmentId = document.id
                        requestArrayList.add(model!!)
                    }
                }

                if(count == 0) {
                    binding!!.noRequestTv.visibility = View.VISIBLE
                    binding!!.requestRv.visibility = View.GONE
                }

                adapterAdminRequest =
                    AdapterAdminRequestHistory(this@AdminRequestHistoryActivity, requestArrayList)

                binding!!.requestRv.adapter = adapterAdminRequest

            }
        }
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