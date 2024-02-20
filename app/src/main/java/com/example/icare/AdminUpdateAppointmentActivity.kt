package com.example.icare

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.view.GravityCompat
import com.anurag.multiselectionspinner.MultiSelectionSpinnerDialog
import com.anurag.multiselectionspinner.MultiSpinner
import com.example.icare.adapter.AdapterAccount
import com.example.icare.adapter.AdapterAdminIncoming
import com.example.icare.adapter.AdapterInventory
import com.example.icare.databinding.ActivityAdminHomeBinding
import com.example.icare.databinding.ActivityAdminInventoryBinding
import com.example.icare.databinding.ActivityAdminUpdateAppointmentBinding
import com.example.icare.model.ModelAccount
import com.example.icare.model.ModelAppointment
import com.example.icare.model.ModelInventory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat

class AdminUpdateAppointmentActivity : AppCompatActivity(),
    MultiSelectionSpinnerDialog.OnMultiSpinnerSelectionListener {
    private var binding: ActivityAdminUpdateAppointmentBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private val arrayList = ArrayList<String>()

    private var contentList : MutableList<String> = ArrayList()

    private var selectedDoctor: String? = null

    private var selectedPrescriptions: MutableList<String> = ArrayList()

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_update_appointment)

        binding = ActivityAdminUpdateAppointmentBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please Wait...")
        progressDialog!!.setCanceledOnTouchOutside(false)

        val intent = intent
        val id = intent.getStringExtra("uid")

        loadAppointment(id!!)

        val spinner: Spinner = binding!!.doctorDd

        loadDoctor()

        val multiSpinner : MultiSpinner = binding!!.prescriptionDd

        loadMedicine()
        multiSpinner!!.setAdapterWithOutImage(this@AdminUpdateAppointmentActivity,contentList,this@AdminUpdateAppointmentActivity)
        multiSpinner!!.initMultiSpinner(this,multiSpinner)

        // Set up the OnItemSelectedListener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Get the selected item from the Spinner
                selectedDoctor = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case where nothing is selected (optional)
            }
        }

        binding!!.completeBtn.setOnClickListener {
            checkData(id)
        }

        binding!!.cancelBtn.setOnClickListener {
            cancelAppointment(id)
        }



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
                startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminAppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminUpdateAppointmentActivity, HomeActivity::class.java)
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

    private fun loadMedicine() {
        val ref = db.collection("Inventory").orderBy("itemName").whereNotEqualTo("stock", 0)
        ref.addSnapshotListener { result, e ->
            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (result != null && !result.isEmpty) {
                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelInventory::class.java)
                    Log.d("Item : ", "${model?.itemName!!}")
                    contentList.add(model?.itemName!!)
                }
            }
        }
    }

    override fun OnMultiSpinnerItemSelected(chosenItems: MutableList<String>?) {
        selectedPrescriptions.clear()
        chosenItems?.let { selectedPrescriptions.addAll(it) }

        for (i in chosenItems!!.indices){

            Log.e("chosenItems",chosenItems[i])
        }
    }

    private var comment: String = ""

    private fun cancelAppointment(id: String) {
        comment = binding!!.commentEt.text.trim().toString()

        if(TextUtils.isEmpty(comment)) {
            Toast.makeText(this, "Reason Field Cannot be Empty!", Toast.LENGTH_SHORT).show()
        } else {
            val builder= AlertDialog.Builder(this)
            builder.setTitle("Accept")
                .setMessage("Are you sure you want to cancel the appointment?")
                .setPositiveButton("Confirm") {a, d->
                    progressDialog!!.setMessage("Processing...")
                    progressDialog!!.show()
                    db.collection("Appointment").document(id)
                        .update(mapOf("status" to "AC", "reason" to comment, "updateUserId" to firebaseAuth!!.currentUser!!.uid))
                        .addOnSuccessListener {
                            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            val name = sharedPref.getString("name", "").toString()

                            val log = hashMapOf(
                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                "time" to System.currentTimeMillis(),
                                "detail" to "Appointment with ${binding!!.nameTv.text.substring(10)} has been cancelled by $name due to $comment",
                                "level" to 2
                            )

                            db.collection("Log").add(log)

                            Toast.makeText(this, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                            progressDialog!!.dismiss()
                            startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminAppointmentActivity::class.java))
                        }
                        .addOnFailureListener { e ->
                            // Update failed
                            Toast.makeText(this, "Unable to cancel due to ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                }
                .setNegativeButton("Cancel") {a, d->
                    a.dismiss()
                    progressDialog!!.dismiss()
                }.show()
        }
    }

    private fun checkData(id: String) {
        comment = binding!!.commentEt.text.trim().toString()

        if(TextUtils.isEmpty(comment)) {
            Toast.makeText(this, "Reason Field Cannot be Empty!", Toast.LENGTH_SHORT).show()
        } else if(selectedDoctor == "") {
            Toast.makeText(this, "Please select the doctor that handled the appointment!", Toast.LENGTH_SHORT).show()
        } else {

            Log.d("Selected prescription", "= $selectedPrescriptions")
            val builder= AlertDialog.Builder(this)
            builder.setTitle("Accept")
                .setMessage("Are you sure you want to complete the appointment?")
                .setPositiveButton("Confirm") {a, d->
                    progressDialog!!.setMessage("Processing...")
                    progressDialog!!.show()

                    db.collection("Appointment").document(id)
                                    .update(mapOf("status" to "AF", "comment" to comment, "doctor" to selectedDoctor))
                                    .addOnSuccessListener {
                                        if(selectedPrescriptions.isNotEmpty()) {
                                            for(e in selectedPrescriptions) {
                                                val intent = intent
                                                val id = intent.getStringExtra("uid")

                                                val query = db.collection("Inventory").whereEqualTo("itemName", e)

                                                query.get()
                                                    .addOnSuccessListener { documents ->
                                                        if (!documents.isEmpty) {
                                                            // Assuming there is only one document with the given itemName
                                                            val documentSnapshot = documents.documents[0]

                                                            // Get the current value
                                                            val currentValue = documentSnapshot.getLong("stock") ?: 0

                                                            // Update the field with the current value - 1
                                                            documentSnapshot.reference.update("stock", currentValue.toInt() - 1)

                                                            val fullName = binding!!.nameTv.text
                                                            val requestByText = "Request by "
                                                            var name = ""
                                                            if (fullName != null && fullName.startsWith(requestByText)) {
                                                                name =
                                                                    fullName.substring(requestByText.length)
                                                            }

                                                            val item = hashMapOf(
                                                                "appointmentId" to id,
                                                                "itemName" to e,
                                                                "updateTime" to System.currentTimeMillis(),
                                                                "updateUserId" to firebaseAuth?.currentUser!!.uid,
                                                                "oldStock" to currentValue.toInt(),
                                                                "newStock" to currentValue.toInt() - 1,
                                                                "name" to name,
                                                                "type" to "P"
                                                            )

                                                            db.collection("Prescription").add(item)

                                                            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                                            name = sharedPref.getString("name", "").toString()

                                                            val log = hashMapOf(
                                                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                                                "time" to System.currentTimeMillis(),
                                                                "detail" to "Appointment with${binding!!.nameTv.text.substring(10)} has been completed by $name",
                                                                "level" to 2
                                                            )

                                                            db.collection("Log").add(log)

                                                        }
                                                    }
                                            }
                                        }
                                        Toast.makeText(this, "Appointment completed.", Toast.LENGTH_SHORT).show()
                                        progressDialog!!.dismiss()
                                        startActivity(Intent(this@AdminUpdateAppointmentActivity, AdminAppointmentActivity::class.java))
                                    }
                                    .addOnFailureListener { e ->
                                        progressDialog!!.dismiss()
                                        Toast.makeText(this, "Unable to complete due to ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                }
                .setNegativeButton("Cancel") {a, d->
                    a.dismiss()
                    progressDialog!!.dismiss()
                }.show()
        }

    }

    private fun loadDoctor() {
        val ref = FirebaseDatabase.getInstance().getReference("Users").orderByChild("role").equalTo("D")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()) {
                    arrayList.add("")

                    for(ds in snapshot.children) {
                        val model = ds.getValue(ModelAccount::class.java)

                        arrayList.add(model?.name!!)
                    }

                    // Set up ArrayAdapter here after the data has been loaded
                    val spinner: Spinner = binding!!.doctorDd
                    ArrayAdapter<String>(
                        this@AdminUpdateAppointmentActivity,
                        android.R.layout.simple_spinner_item,
                        arrayList
                    ).also { adapter ->
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

    }

    private fun loadAppointment(id: String) {
        val ref = db.collection("Appointment").document(id).get()
        ref.addOnSuccessListener { document ->
            if(document != null) {
                val appointmentModel = document.toObject(ModelAppointment::class.java)

                val ref = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(appointmentModel!!.userId)
                ref.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        binding!!.nameTv.text = "Request by " + snapshot.child(appointmentModel!!.userId).child("name").getValue(String::class.java)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })

                val ref2 = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(appointmentModel!!.updateUserId)
                ref2.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        binding!!.approveTv.text = "Approved by " + snapshot.child(appointmentModel!!.updateUserId).child("name").getValue(String::class.java)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })

                binding!!.appointmentTimeTv.text = "Appointment Time: " + appointmentModel.appointmentTime

                val date = java.util.Date(appointmentModel.requestTime)
                val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
                val requestTime = format.format(date)

                binding!!.requestTimeTv.text = "Request Time: $requestTime"
                binding!!.descriptionTv.text = "Description: " + appointmentModel.description



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