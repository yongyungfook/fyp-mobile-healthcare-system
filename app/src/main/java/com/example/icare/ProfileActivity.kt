package com.example.icare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.icare.databinding.ActivityProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.firestore
import java.sql.Date
import java.text.SimpleDateFormat

class ProfileActivity : AppCompatActivity() {
    private var binding: ActivityProfileBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        binding = ActivityProfileBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")

        binding!!.titleDashboardUser.text = name
        binding!!.subtitleDashboardUser.text = email

        val headerView = binding!!.navigationView.getHeaderView(0)
        val nameTv : TextView = headerView.findViewById(R.id.nameTv)
        val emailTv : TextView = headerView.findViewById(R.id.emailTv)

        nameTv.text = name
        emailTv.text = email

        binding!!.editBtn.setOnClickListener {
            startActivity(Intent(this@ProfileActivity, EditProfileActivity::class.java))
        }

        loadMenu(name.toString())

        loadProfile()
    }

    private fun loadMenu(name: String) {
        val drawer = binding!!.myDrawerLayout
        val imageButton = binding!!.menuBtn
        imageButton.setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        val navigationView = binding!!.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            if (id == R.id.nav_home) {
                val intent = Intent(this@ProfileActivity, UserHomeActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@ProfileActivity, AppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                drawer.closeDrawer(GravityCompat.START)
            } else if (id == R.id.nav_aboutus) {
                startActivity(Intent(this@ProfileActivity, AboutUsActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@ProfileActivity, HomeActivity::class.java)
                firebaseAuth!!.signOut()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            true
        }
    }

    private fun loadProfile() {
        val firebaseUser = firebaseAuth!!.currentUser

        //Check in DB
        val ref = FirebaseDatabase.getInstance().getReference("Users")

        // Attach a listener to read the data at our posts reference
        ref.child(firebaseUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val email = "" + snapshot.child("email").value
                    val name = "" + snapshot.child("name").value
                    val phone = "" + snapshot.child("phoneNumber").value
                    val timestamp = "" + snapshot.child("regDate").value
                    binding!!.emailTv.text = email
                    binding!!.nameTv.text = name
                    binding!!.phoneTv.text = phone
                    val formatter = SimpleDateFormat("dd/MM/yyyy")
                    val dateString = formatter.format(Date(timestamp.toLong()))

                    var dateText = binding!!.dateTv.text.toString().trim()
                    binding!!.dateTv.text = "$dateText $dateString"
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