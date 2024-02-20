package com.example.icare

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.icare.databinding.ActivityForgotPasswordBinding
import com.example.icare.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class ForgotPasswordActivity : AppCompatActivity() {
    private var binding: ActivityForgotPasswordBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private val db = Firebase.firestore

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please Wait...")
        progressDialog!!.setCanceledOnTouchOutside(false)

        binding!!.resetBtn.setOnClickListener {
            sendEmail()
        }
    }

    private fun sendEmail() {
        progressDialog!!.setMessage("Sending Email...")
        progressDialog!!.show()

        var email = binding!!.emailEt.text.toString().trim()

        firebaseAuth!!.sendPasswordResetEmail("" + email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val log = hashMapOf(
                        "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                        "time" to System.currentTimeMillis(),
                        "detail" to "System received a forgot password request for $email",
                        "level" to 1
                    )

                    db.collection("Log").add(log)

                    progressDialog!!.dismiss()
                    Toast.makeText(this@ForgotPasswordActivity, "An email will be sent if the account exists!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, "An email will be sent if the account exists!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
    }
}