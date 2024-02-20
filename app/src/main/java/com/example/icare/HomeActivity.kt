package com.example.icare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.icare.databinding.ActivityHomeBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity() {
    private var binding: ActivityHomeBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        binding = ActivityHomeBinding.inflate(layoutInflater)

        firebaseAuth = FirebaseAuth.getInstance()

        setContentView(binding!!.root)

        binding!!.loginBtn.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding!!.registerBtn.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        Log.d("Email= ", sharedPref.getString("email", "").toString())
        if(sharedPref.getString("email", "") != null) {
            val ref = FirebaseDatabase.getInstance().getReference("Users")

            if(firebaseAuth!!.currentUser != null) {
                // Attach a listener to read the data at our posts reference
                ref.child(firebaseAuth?.uid!!)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            Log.d("Fail: ", "A")
                            val role = "${snapshot.child("role").value}"
                            val oldToken = "${snapshot.child("token").value}"
                            val name = "${snapshot.child("name").value}"
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    if(task.result != oldToken) {
                                        val token = task.result

                                        val hashMap = HashMap<String, Any?>()
                                        hashMap["token"] = token

                                        //Set data to DB
                                        val ref = FirebaseDatabase.getInstance().getReference("Users")

                                        ref.child(firebaseAuth?.uid!!)
                                            .updateChildren(hashMap)
                                            .addOnSuccessListener {
                                                val log = hashMapOf(
                                                    "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                                    "time" to System.currentTimeMillis(),
                                                    "detail" to "The FCM token of $name has been updated.",
                                                    "level" to 2
                                                )

                                                db.collection("Log").add(log)
                                            }
                                    }
                                }
                            }
                            Log.d("Fail: ", "B")
                            if (role == "U") {
                                startActivity(
                                    Intent(
                                        this@HomeActivity,
                                        UserHomeActivity::class.java
                                    )
                                )
                            } else if (role == "A" || role == "D" || role == "O") {
                                startActivity(
                                    Intent(
                                        this@HomeActivity,
                                        AdminHomeActivity::class.java
                                    )
                                )
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            println("The read failed: " + databaseError.code)
                        }
                    })
            }
        }
    }
}