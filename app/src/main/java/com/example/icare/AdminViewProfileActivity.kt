package com.example.icare

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import com.example.icare.databinding.ActivityAdminModerationBinding
import com.example.icare.databinding.ActivityAdminViewProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import java.sql.Date
import java.text.SimpleDateFormat

class AdminViewProfileActivity : AppCompatActivity() {
    private var binding: ActivityAdminViewProfileBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_view_profile)
        binding = ActivityAdminViewProfileBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        loadProfile()

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
                startActivity(Intent(this@AdminViewProfileActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@AdminViewProfileActivity, AdminAppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminViewProfileActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminViewProfileActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminViewProfileActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminViewProfileActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminViewProfileActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminViewProfileActivity, HomeActivity::class.java)
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
    private var roleName = ""

    private fun loadProfile() {
        val uid = intent.getStringExtra("uid").toString()
        val ref = FirebaseDatabase.getInstance().getReference("Users")

        // Attach a listener to read the data at our posts reference
        ref.child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val email = "" + snapshot.child("email").value
                    val name = "" + snapshot.child("name").value
                    val phone = "" + snapshot.child("phoneNumber").value
                    val timestamp = "" + snapshot.child("regDate").value
                    val gender = "" + snapshot.child("gender").value
                    val role = "" + snapshot.child("role").value

                    if(role[0] == 'U') {
                        binding!!.roleTv.text = "Role: User"
                        roleName = "user"
                    } else if(role[0] == 'D') {
                        binding!!.roleTv.text = "Role: Doctor"
                        roleName = "doctor"
                    } else {
                        binding!!.roleTv.text = "Role: Staff"
                        roleName = "staff"
                    }
                    binding!!.emailTv.text = email
                    binding!!.nameTv.text = name
                    binding!!.phoneTv.text = phone
                    val formatter = SimpleDateFormat("dd/MM/yyyy")
                    val dateString = formatter.format(Date(timestamp.toLong()))

                    var dateText = binding!!.dateTv.text.toString().trim()
                    binding!!.dateTv.text = "$dateText $dateString"
                    if(gender == "M") {
                        binding!!.genderTv.text = "Male"
                    } else {
                        binding!!.genderTv.text = "Female"
                    }

                    val statusText = binding!!.statusTv.text
                    if(role.length < 2) {
                        binding!!.statusTv.text = "$statusText Not banned"

                        binding!!.banBtn.setOnClickListener {
                            val builder= AlertDialog.Builder(this@AdminViewProfileActivity)
                            builder.setTitle("Ban")
                                .setMessage("Are you sure you want to ban this user: $name")
                                .setPositiveButton("Confirm") {a, d->
                                    progressDialog = ProgressDialog(this@AdminViewProfileActivity)
                                    progressDialog!!.setTitle("Please Wait...")
                                    progressDialog!!.setCanceledOnTouchOutside(false)
                                    progressDialog!!.setMessage("Banning...")
                                    progressDialog!!.show()
                                    val hashMap = HashMap<String, Any>()
                                    hashMap["role"] = role[0] + "B"

                                    ref.child(uid).updateChildren(hashMap)
                                        .addOnSuccessListener {
                                            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                            val userName = sharedPref.getString("name", "X")

                                            val log = hashMapOf(
                                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                                "time" to System.currentTimeMillis(),
                                                "detail" to "$name ($roleName) has been banned by $userName",
                                                "level" to 3
                                            )

                                            db.collection("Log").add(log)
                                            progressDialog!!.dismiss()

                                            Toast.makeText(this@AdminViewProfileActivity, "Account banned.", Toast.LENGTH_SHORT).show()
                                            recreate()
                                        }
                                        .addOnFailureListener { e -> //Data Entry Failed
                                            progressDialog!!.dismiss()
                                            Toast.makeText(this@AdminViewProfileActivity, "Unable to ban due to ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .setNegativeButton("Cancel") {a, d->
                                    a.dismiss()
                                }.show()

                        }
                    } else {
                        binding!!.statusTv.text = "$statusText Banned"
                        binding!!.banBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#90EE90"))
                        binding!!.banBtn.text = "Unban"
                        binding!!.banBtn.setOnClickListener {
                            val builder = AlertDialog.Builder(this@AdminViewProfileActivity)
                            builder.setTitle("Unban")
                                .setMessage("Are you sure you want to unban this user: $name")
                                .setPositiveButton("Confirm") { a, d ->
                                    progressDialog = ProgressDialog(this@AdminViewProfileActivity)
                                    progressDialog!!.setTitle("Please Wait...")
                                    progressDialog!!.setCanceledOnTouchOutside(false)
                                    progressDialog!!.setMessage("Unbanning...")
                                    progressDialog!!.show()
                                    val hashMap = HashMap<String, Any>()
                                    hashMap["role"] = role[0].toString()

                                    ref.child(uid).updateChildren(hashMap)
                                        .addOnSuccessListener { //Data added into DB
                                            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                            val userName = sharedPref.getString("name", "X")

                                            val log = hashMapOf(
                                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                                "time" to System.currentTimeMillis(),
                                                "detail" to "$name ($roleName) has been unbanned by $userName",
                                                "level" to 3
                                            )

                                            db.collection("Log").add(log)

                                            progressDialog!!.dismiss()
                                            Toast.makeText(
                                                this@AdminViewProfileActivity,
                                                "Account unbanned.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            recreate()
                                        }
                                        .addOnFailureListener { e -> //Data Entry Failed
                                            progressDialog!!.dismiss()
                                            Toast.makeText(
                                                this@AdminViewProfileActivity,
                                                "Unable to unban due to ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .setNegativeButton("Cancel") { a, d ->
                                    a.dismiss()
                                }.show()
                        }

                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    println("The read failed: " + databaseError.code)
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