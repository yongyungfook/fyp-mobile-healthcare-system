package com.example.icare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.icare.databinding.ActivityAppointmentBinding
import com.example.icare.databinding.ActivityRequestAppointmentBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.util.*


class RequestAppointmentActivity : AppCompatActivity() {
    private var binding: ActivityRequestAppointmentBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_appointment)

        binding = ActivityRequestAppointmentBinding.inflate(layoutInflater)

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

        binding!!.dateEt.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { view, year, monthOfYear, dayOfMonth ->
                    val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                    binding!!.dateEt.setText(dat)
                    var check = ""
                    if(dayOfMonth >= 10) {
                        check = dat.substring(0, 2)
                    } else {
                        check = dat.substring(0, 1)
                    }
                    if(check.toInt() > day) {
                        requestFuture()
                    } else {
                        requestToday()
                    }
                },
                year,
                month,
                day
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        binding!!.submitBtn.setOnClickListener {
            requestAppointment()
        }

        val drawer = binding!!.myDrawerLayout
        val imageButton = binding!!.menuBtn
        imageButton.setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        val navigationView = binding!!.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            if (id == R.id.nav_home) {
                startActivity(Intent(this@RequestAppointmentActivity, UserHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@RequestAppointmentActivity, AppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                val intent = Intent(this@RequestAppointmentActivity, ProfileActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.nav_aboutus) {
                startActivity(Intent(this@RequestAppointmentActivity, AboutUsActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@RequestAppointmentActivity, HomeActivity::class.java)
                firebaseAuth!!.signOut()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            true
        }
    }

    private fun requestAppointment() {
        val time = binding!!.timeEt.text
        val date = binding!!.dateEt.text
        val comment = binding!!.commentEt.text.trim()

        if(TextUtils.isEmpty(time) || TextUtils.isEmpty(date)) {
            Toast.makeText(this, "Please select a date and a time!",
                Toast.LENGTH_SHORT).show()
        } else {
            val request = hashMapOf(
                "userId" to firebaseAuth!!.uid,
                "appointmentTime" to "$date $time",
                "requestTime" to System.currentTimeMillis(),
                "status" to "P",
                "description" to "$comment"
            )

            db.collection("Appointment").add(request)
                .addOnSuccessListener {
                    val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val userName = sharedPref.getString("name", "X")

                    val log = hashMapOf(
                        "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                        "time" to System.currentTimeMillis(),
                        "detail" to "$userName has submitted a new request (${it.id})",
                        "level" to 1
                    )

                    db.collection("Log").add(log)
                    Toast.makeText(this, "Request submitted, please wait for confirmation!",
                        Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this@RequestAppointmentActivity, AppointmentActivity::class.java))

                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload request!",
                        Toast.LENGTH_SHORT).show()
                }
        }


    }

    private fun requestToday() {
        binding!!.timeEt.isEnabled = true

        binding!!.timeEt.inputType = InputType.TYPE_NULL
        binding!!.timeEt.setOnClickListener {
            val cldr = Calendar.getInstance()
            val hour = cldr[Calendar.HOUR_OF_DAY]
            val minutes = cldr[Calendar.MINUTE]
            // time picker dialog
            val picker = TimePickerDialog(this,
                { tp, sHour, sMinute ->
                    if((sHour <= hour + 2)&&
                        (sMinute <= minutes)){
                        Toast.makeText(this, "Please select future time that is at least 2 hours from the current time!",
                            Toast.LENGTH_SHORT).show()
                    } else { binding!!.timeEt.setText("$sHour:$sMinute") }}, hour, minutes, true
            )
            picker.show()
        }
    }

    private fun requestFuture() {
        binding!!.timeEt.isEnabled = true

        binding!!.timeEt.inputType = InputType.TYPE_NULL
        binding!!.timeEt.setOnClickListener {
            val cldr = Calendar.getInstance()
            val hour = cldr[Calendar.HOUR_OF_DAY]
            val minutes = cldr[Calendar.MINUTE]
            val picker = TimePickerDialog(this,
                { tp, sHour, sMinute ->
                    binding!!.timeEt.setText("$sHour:$sMinute")}, hour, minutes, true
            )
            picker.show()
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