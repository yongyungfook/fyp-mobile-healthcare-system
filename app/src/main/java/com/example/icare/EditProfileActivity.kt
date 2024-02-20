package com.example.icare

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import com.example.icare.databinding.ActivityEditProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore

class EditProfileActivity : AppCompatActivity() {
    private var binding: ActivityEditProfileBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private lateinit var progressDialog: ProgressDialog

    private val db = Firebase.firestore

    private var oldName: String = ""
    private var oldEmail: String = ""
    private var oldPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

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

        loadMenu(name.toString())

        loadUserInfo()

        binding!!.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private fun loadMenu(name: String) {
        val drawer = binding!!.myDrawerLayout
        val imageButton = binding!!.menuBtn
        imageButton.setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        val navigationView = binding!!.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            if (id == R.id.nav_home) {
                val intent = Intent(this@EditProfileActivity, UserHomeActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@EditProfileActivity, AppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@EditProfileActivity, ProfileActivity::class.java))
            } else if (id == R.id.nav_aboutus) {
                startActivity(Intent(this@EditProfileActivity, AboutUsActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@EditProfileActivity, HomeActivity::class.java)
                firebaseAuth!!.signOut()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            true
        }
    }

    private fun loadUserInfo() {
        //Check in DB
        val ref = FirebaseDatabase.getInstance().getReference("Users")

        // Attach a listener to read the data at our posts reference
        ref.child(firebaseAuth?.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    oldName = "${snapshot.child("name").value}"
                    oldPhone = "${snapshot.child("phoneNumber").value}"


                    binding!!.nameEt.setText(oldName)
                    binding!!.phoneNumberEt.setText(oldPhone)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    println("The read failed: " + databaseError.code)
                }
            })
    }

    private var newName = ""
    private var newPhone = ""

    private fun validateData() {
        newName = binding!!.nameEt.text.toString().trim()
        newPhone = binding!!.phoneNumberEt.text.toString().trim()
        if(newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Please fill out all info", Toast.LENGTH_SHORT).show()
        } else if(!Regex("^01\\d{8,9}\$").matches(newPhone)) {
            Toast.makeText(this, "Please enter Malaysia phone number! (for example: 0123456789)", Toast.LENGTH_LONG).show()
        } else {
            updateProfile()
        }
    }

    private fun updateProfile() {
        progressDialog.setMessage("Processing...")
        progressDialog.show()

        val hashMap = HashMap<String, Any>()
        hashMap["name"] = "$newName"
        hashMap["phoneNumber"] = "$newPhone"

        if(oldName != newName) {
            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val userName = sharedPref.getString("name", "X")

            val editor = sharedPref.edit()
            editor.putString("name", newName)
            editor.apply()

            val log = hashMapOf(
                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                "time" to System.currentTimeMillis(),
                "detail" to "${FirebaseAuth.getInstance().currentUser!!.uid} updated their name from $oldName to $newName",
                "level" to 1
            )

            db.collection("Log").add(log)
        }

        if(oldPhone != newPhone) {
            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val userName = sharedPref.getString("name", "X")

            val editor = sharedPref.edit()
            editor.putString("phone", newPhone)
            editor.apply()

            val log = hashMapOf(
                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                "time" to System.currentTimeMillis(),
                "detail" to "${FirebaseAuth.getInstance().currentUser!!.uid} updated their phone from $oldPhone to $newPhone",
                "level" to 1
            )

            db.collection("Log").add(log)
        }

        //Set data to DB
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        Log.d("Details 5:", "$oldName")
        ref.child(firebaseAuth!!.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener { //Data added into D

                progressDialog.dismiss()
                Toast.makeText(this@EditProfileActivity, "Profile Updated.", Toast.LENGTH_SHORT).show()

                //Since User Account is Created, go back to Login
                startActivity(Intent(this@EditProfileActivity, ProfileActivity::class.java))
                finish()

            }
            .addOnFailureListener { e -> //Data Entry Failed
                progressDialog.dismiss()
                Toast.makeText(this@EditProfileActivity, "" + e.message, Toast.LENGTH_SHORT).show()
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