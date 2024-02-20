package com.example.icare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.icare.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {
    private var binding: ActivityLoginBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var progressDialog: ProgressDialog? = null

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please Wait...")
        progressDialog!!.setCanceledOnTouchOutside(false)

        binding!!.signInBtn.setOnClickListener {
            validateData()
        }

        binding!!.forgotBtn.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
        }
    }

    private var email = ""
    private var password = ""

    private fun validateData() {
        email = binding!!.emailEt.text.toString().trim()
        password = binding!!.passwordEt.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email Pattern!", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(password) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Password Field Cannot be Empty!", Toast.LENGTH_SHORT).show()
        } else {
            loginUser()
        }
    }

    private fun loginUser() {
        progressDialog!!.setMessage("Logging In...")
        progressDialog!!.show()

        //Login User
        firebaseAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkUser()
                } else {
                    progressDialog!!.dismiss()
                    Toast.makeText(this@LoginActivity, "Incorrect email or password!", Toast.LENGTH_SHORT).show()

                    val log = hashMapOf(
                        "time" to System.currentTimeMillis(),
                        "detail" to "A failed login attempt for the email: $email",
                        "level" to 1
                    )

                    db.collection("Log").add(log)
                }
            }
    }

    private fun checkUser() {
        progressDialog!!.setMessage("Checking User...")
        //Get current user
        val firebaseUser = firebaseAuth!!.currentUser

        //Check in DB
        val ref: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog!!.dismiss()
                    val uid = "" + snapshot.child("uid").value
                    val role = "" + snapshot.child("role").value
                    val name = "" + snapshot.child("name").value
                    val gender = "" + snapshot.child("gender").value
                    val email = "" + snapshot.child("email").value
                    val phone = "" + snapshot.child("phoneNumber").value
                    val oldToken = "" + snapshot.child("token").value

                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if(task.result != oldToken) {
                                val token = task.result

                                val hashMap = HashMap<String, Any?>()
                                hashMap["token"] = token

                                //Set data to DB
                                val ref = FirebaseDatabase.getInstance().getReference("Users")

                                ref.child(uid!!)
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

                    if (role == "U") {
                        val intent = Intent(this@LoginActivity, UserHomeActivity::class.java)

                        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("role", role) // Replace with the actual user role
                        editor.putString("name", name)
                        editor.putString("email", email)
                        editor.apply()


                        val log = hashMapOf(
                            "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                            "time" to System.currentTimeMillis(),
                            "detail" to "$name ($uid) has logged into the system",
                            "level" to 1
                        )

                        db.collection("Log").add(log)

                        startActivity(intent)
                    } else if (role == "A" || role == "D" || role == "O") {
                        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("role", role) // Replace with the actual user role
                        editor.putString("name", name)
                        editor.putString("email", email)
                        editor.apply()

                        val log = hashMapOf(
                            "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                            "time" to System.currentTimeMillis(),
                            "detail" to "$name ($uid) has logged into the system",
                            "level" to 1
                        )

                        db.collection("Log").add(log)

                        startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Your account has been deactivated.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}