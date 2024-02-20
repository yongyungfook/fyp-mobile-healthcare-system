    package com.example.icare

    import android.content.Context
    import android.content.Intent
    import android.os.Bundle
    import android.text.Editable
    import android.text.TextWatcher
    import android.view.View
    import android.widget.TextView
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.GravityCompat
    import com.example.icare.adapter.AdapterInventory
    import com.example.icare.adapter.AdapterInventoryHistory
    import com.example.icare.databinding.ActivityAdminInventoryBinding
    import com.example.icare.databinding.ActivityAdminInventoryHistoryBinding
    import com.example.icare.model.ModelInventory
    import com.example.icare.model.ModelPrescription
    import com.google.firebase.Firebase
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.Query
    import com.google.firebase.firestore.firestore

    class AdminInventoryHistoryActivity: AppCompatActivity()  {
        private var binding: ActivityAdminInventoryHistoryBinding? = null

        private var firebaseAuth: FirebaseAuth? = null

        private var db = Firebase.firestore

        private lateinit var prescriptionArrayList: ArrayList<ModelPrescription>

        private lateinit var adapterInventoryHistory: AdapterInventoryHistory

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_admin_inventory_history)

            binding = ActivityAdminInventoryHistoryBinding.inflate(layoutInflater)

            setContentView(binding!!.root)

            firebaseAuth = FirebaseAuth.getInstance()

            loadHistory()

            binding!!.searchEt.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    try {
                        adapterInventoryHistory.filter!!.filter(s)
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
                    startActivity(Intent(this@AdminInventoryHistoryActivity, AdminHomeActivity::class.java))
                } else if (id == R.id.nav_appointment) {
                    startActivity(Intent(this@AdminInventoryHistoryActivity, AdminAppointmentActivity::class.java))
                } else if (id == R.id.nav_profile) {
                    startActivity(Intent(this@AdminInventoryHistoryActivity, AdminProfileActivity::class.java))
                } else if (id == R.id.nav_inventory) {
                    startActivity(Intent(this@AdminInventoryHistoryActivity, AdminInventoryActivity::class.java))
                } else if (id == R.id.nav_report) {
                    startActivity(Intent(this@AdminInventoryHistoryActivity, AdminReportActivity::class.java))
                } else if (id == R.id.nav_log) {
                    startActivity(Intent(this@AdminInventoryHistoryActivity, AdminLogActivity::class.java))
                } else if (id == R.id.nav_moderation) {
                    startActivity(Intent(this@AdminInventoryHistoryActivity, AdminModerationActivity::class.java))
                } else if (id == R.id.nav_logout) {
                    val log = hashMapOf(
                        "userId" to firebaseAuth!!.currentUser!!.uid,
                        "time" to System.currentTimeMillis(),
                        "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                        "level" to 1
                    )

                    db.collection("Log").add(log)


                    clearSharedPreferences()
                    val intent = Intent(this@AdminInventoryHistoryActivity, HomeActivity::class.java)
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

        private fun loadHistory() {
            prescriptionArrayList = ArrayList()
            val ref = db.collection("Prescription").orderBy("updateTime", Query.Direction.DESCENDING)
            ref.addSnapshotListener { result, e ->
                if (e != null) {
                    // Handle errors
                    return@addSnapshotListener
                }

                if (result != null && !result.isEmpty) {
                    binding!!.noInventoryTv.visibility = View.GONE
                    binding!!.inventoryRv.visibility = View.VISIBLE

                    prescriptionArrayList = ArrayList()

                    for (document in result) {
                        // Access document data using document.data
                        val model = document.toObject(ModelPrescription::class.java)
                        model.itemId = document.id
                        prescriptionArrayList.add(model!!)
                    }

                    adapterInventoryHistory = AdapterInventoryHistory(this@AdminInventoryHistoryActivity, prescriptionArrayList)

                    binding!!.inventoryRv.adapter = adapterInventoryHistory
                } else {
                    binding!!.noInventoryTv.visibility = View.VISIBLE
                    binding!!.inventoryRv.visibility = View.GONE
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
