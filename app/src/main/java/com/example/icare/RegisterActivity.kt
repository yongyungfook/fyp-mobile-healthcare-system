package com.example.icare

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.icare.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {
    //View Binding
    private var binding: ActivityRegisterBinding? = null

    //Firebase Authentication
    private var firebaseAuth: FirebaseAuth? = null

    //Progress Dialog
    private var progressDialog: ProgressDialog? = null

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        //init Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()


        //Setup Process Dialog
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please Wait...")
        progressDialog!!.setCanceledOnTouchOutside(false)


        binding!!.registerBtn.setOnClickListener {
            validateData()
        }
    }

    private var name = ""
    private var email = ""
    private var phoneNumber = ""
    private var password = ""

    private fun validateData() {
        /* Before creating account, check for data validation */
        var emailCheck = true

        //Get data
        name = binding!!.nameEt.text.toString().trim()
        email = binding!!.emailEt.text.toString().trim()
        phoneNumber = binding!!.phoneNumberEt.text.toString().trim()
        password = binding!!.passwordEt.text.toString().trim()
        val confirmPassword = binding!!.reenterPasswordEt.text.toString().trim()

        if(TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email Field Cannot be Empty!", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name Field Cannot be Empty!", Toast.LENGTH_SHORT).show()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email Pattern!", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Password Field Cannot be Empty!", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Confirm Password Field Cannot be Empty!", Toast.LENGTH_SHORT).show()
        } else if (password != confirmPassword) {
            Toast.makeText(this, "Password does not match!", Toast.LENGTH_SHORT).show()
        } else if (!Regex("^01\\d{8,9}\$").matches(phoneNumber)) {
            Toast.makeText(this, "Please enter Malaysia phone number! (for example: 0123456789)", Toast.LENGTH_LONG).show()
        } else if (!Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}\$").matches(password)) {
            Toast.makeText(this, "Ensure password entered contains at least 8 characters, with at least 1 number, 1 uppercase letter, and 1 lowercase letter!", Toast.LENGTH_LONG).show()
        } else if (!binding!!.agreeCb.isChecked){
            Toast.makeText(this, "Please agree to iCare's terms and conditions!", Toast.LENGTH_SHORT).show()
        } else {
            createUserAccount()
        }

    }

    private fun createUserAccount() {
        //Show Progress Dialog
        progressDialog!!.setMessage("Creating Account...")
        progressDialog!!.show()

        //Create User in FirebaseAuth
        firebaseAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { //Account Creation Success, Adding into Real-time Firebase DB
                updateUserInfo()
            }
            .addOnFailureListener { e -> //Account Creation Failure
                progressDialog!!.dismiss()
                Toast.makeText(this@RegisterActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserInfo() {
        progressDialog!!.setMessage("Saving User Info...")

        //Timestamp
        val timestamp = System.currentTimeMillis()

        //Get current User UID, Since User is Registered
        val uid = firebaseAuth!!.uid

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                val hashMap = HashMap<String, Any?>()
                hashMap["uid"] = uid
                hashMap["email"] = email
                hashMap["name"] = name
                hashMap["phoneNumber"] = phoneNumber
                hashMap["role"] = "U" //Possible values are user, admin for DB
                hashMap["regDate"] = timestamp
                hashMap["token"] = token

                //Set data to DB
                val ref = FirebaseDatabase.getInstance().getReference("Users")

                ref.child(uid!!)
                    .setValue(hashMap)
                    .addOnSuccessListener {
                        val log = hashMapOf(
                            "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                            "time" to System.currentTimeMillis(),
                            "detail" to "A new account ($uid) has been registered\n" +
                                    "Name: $name\n" +
                                    "Email: $email\n" +
                                    "Phone Number: $phoneNumber",
                            "level" to 1
                        )

                        db.collection("Log").add(log)

                        progressDialog!!.dismiss()
                        Toast.makeText(this@RegisterActivity, "Account Created.", Toast.LENGTH_SHORT).show()

                        //Since User Account is Created, go back to Login //KIV
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e -> //Data Entry Failed
                        progressDialog!!.dismiss()
                        Toast.makeText(this@RegisterActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}