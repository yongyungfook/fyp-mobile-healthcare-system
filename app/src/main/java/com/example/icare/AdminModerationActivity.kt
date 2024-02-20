package com.example.icare

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.core.view.GravityCompat
import com.example.icare.adapter.AdapterAccount
import com.example.icare.databinding.ActivityAdminModerationBinding
import com.example.icare.model.ModelAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore

class AdminModerationActivity : AppCompatActivity() {
    private var binding: ActivityAdminModerationBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private lateinit var accountArrayList: ArrayList<ModelAccount>

    private lateinit var adapterAccount: AdapterAccount

    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_moderation)

        binding = ActivityAdminModerationBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        loadAccount()

        binding!!.searchEt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterAccount.filter!!.filter(s)
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
                startActivity(Intent(this@AdminModerationActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@AdminModerationActivity, AdminAppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminModerationActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminModerationActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminModerationActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminModerationActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                drawer.closeDrawer(GravityCompat.START)
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminModerationActivity, HomeActivity::class.java)
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

    private fun loadAccount() {
        accountArrayList = ArrayList()

        val ref= FirebaseDatabase.getInstance().getReference("Users").orderByChild("name")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                accountArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelAccount::class.java)
                    if(model!!.role != "O") {
                        accountArrayList.add(model!!)
                    }
                }
                adapterAccount = AdapterAccount(this@AdminModerationActivity, accountArrayList)

                binding!!.accountRv.adapter = adapterAccount
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
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