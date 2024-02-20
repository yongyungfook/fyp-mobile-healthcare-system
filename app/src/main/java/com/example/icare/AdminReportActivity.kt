package com.example.icare
import android.Manifest
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.example.icare.databinding.ActivityAdminReportBinding
import com.example.icare.model.ModelAccount
import com.example.icare.model.ModelAppointment
import com.example.icare.model.ModelPrescription
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AdminReportActivity : AppCompatActivity() {
    private var binding: ActivityAdminReportBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private lateinit var appointmentArrayList: ArrayList<ModelAppointment>

    private lateinit var inventoryArrayList: ArrayList<ModelPrescription>

    private lateinit var doctorArrayList: ArrayList<ModelAppointment>
    
    private lateinit var doctorList: ArrayList<String>

    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1

    private var data: List<Any> = emptyList()
    private var fileName: String = ""
    private var fileType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_report)

        binding = ActivityAdminReportBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding!!.addBtn.setOnClickListener {
            val listItems = arrayOf(
                "Weekly Appointment Report",
                "Monthly Appointment Report",
                "Weekly Inventory Report",
                "Monthly Inventory Report",
                "Weekly Doctor Report",
                "Monthly Doctor Report"
            )

            val checkedItem = intArrayOf(0)

            AlertDialog.Builder(this@AdminReportActivity)
                .setTitle("Choose an Item")
                .setSingleChoiceItems(listItems, checkedItem[0]) { dialog, which ->
                    checkedItem[0] = which

                    val type = listItems[which]

                    if (type == "Weekly Doctor Report" || type == "Monthly Doctor Report") {
                        // Display another dialog for doctor selection
                        showDoctorSelectionDialog(type)
                    } else {
                        // Generate report for other types
                        setTime(type, "")
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
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
                startActivity(Intent(this@AdminReportActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@AdminReportActivity, AdminAppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminReportActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminReportActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                drawer.closeDrawer(GravityCompat.START)
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminReportActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminReportActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminReportActivity, HomeActivity::class.java)
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

    private fun showDoctorSelectionDialog(type: String) {
        doctorList = ArrayList()
        var selectedDoctor = ""
        val ref= FirebaseDatabase.getInstance().getReference("Users")
        ref.orderByChild("role").equalTo("D").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                doctorList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelAccount::class.java)
                    if(model!!.role == "D") {
                        doctorList.add(model.name.toString())
                    }
                }

                var selectedDoctor = doctorList[0] // Initialize with the first doctor, you can use a variable to track the selected doctor

                AlertDialog.Builder(this@AdminReportActivity)
                    .setTitle("Select a Doctor")
                    .setSingleChoiceItems(doctorList.toTypedArray(), 0) { dialog, which ->
                        // Handle the selected doctor
                        selectedDoctor = doctorList[which]
                    }
                    .setPositiveButton("OK") { _, _ ->
                        setTime(type, selectedDoctor)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun setTime(type: String, doctor: String) {
        var startTimestamp: Long = 0
        var endTimestamp: Long = 0
        val currentDate = Calendar.getInstance()
        if(type[0] == 'W') {
            currentDate.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            currentDate.set(Calendar.HOUR_OF_DAY, 0)
            currentDate.set(Calendar.MINUTE, 0)
            currentDate.set(Calendar.SECOND, 0)
            currentDate.set(Calendar.MILLISECOND, 0)

            // Get the timestamp for the start of the week
            startTimestamp = currentDate.timeInMillis

            // Move to the end of the week (Sunday)
            currentDate.add(Calendar.DAY_OF_WEEK, 6)
            currentDate.set(Calendar.HOUR_OF_DAY, 23)
            currentDate.set(Calendar.MINUTE, 59)
            currentDate.set(Calendar.SECOND, 59)

            // Get the timestamp for the end of the week
            endTimestamp = currentDate.timeInMillis

            // Print the timestamps
            Log.d("Start", "" + startTimestamp)
            Log.d("End", "" + endTimestamp)
        } else {
            currentDate.set(Calendar.DAY_OF_MONTH, 1)
            currentDate.set(Calendar.HOUR_OF_DAY, 0)
            currentDate.set(Calendar.MINUTE, 0)
            currentDate.set(Calendar.SECOND, 0)
            currentDate.set(Calendar.MILLISECOND, 0)

            // Get the timestamp for the start of the month
            startTimestamp = currentDate.timeInMillis

            // Move to the end of the month
            currentDate.add(Calendar.MONTH, 1)
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            currentDate.set(Calendar.HOUR_OF_DAY, 23)
            currentDate.set(Calendar.MINUTE, 59)
            currentDate.set(Calendar.SECOND, 59)

            // Get the timestamp for the end of the month
            endTimestamp = currentDate.timeInMillis

            // Print the timestamps
            Log.d("Start", "" + startTimestamp)
            Log.d("End", "" + endTimestamp)
        }

        if(type == "Weekly Appointment Report" || type == "Monthly Appointment Report") {
            getAppointmentData(startTimestamp, endTimestamp, type)
        } else if (type == "Weekly Inventory Report" || type == "Monthly Inventory Report") {
            getInventoryData(startTimestamp, endTimestamp, type)
        } else if (type == "Weekly Doctor Report" || type == "Monthly Doctor Report") {
            getDoctorData(startTimestamp, endTimestamp, doctor, type)
        }
    }

    private fun getDoctorData(startTimestamp: Long, endTimestamp: Long, doctor: String, type: String) {
        var count = 0
        doctorArrayList = ArrayList()

        val startDate = Date(startTimestamp)
        val endDate = Date(endTimestamp)
        val format = SimpleDateFormat("dd-MM-yyyy")
        val startTime = format.format(startDate)
        val endTime = format.format(endDate)

        val ref = db.collection("Appointment").whereGreaterThanOrEqualTo("requestTime", startTimestamp)
            .whereLessThanOrEqualTo("requestTime", endTimestamp).whereEqualTo("doctor", doctor).orderBy("requestTime", Query.Direction.DESCENDING)
        ref.get().addOnSuccessListener { result ->
            if (result != null && !result.isEmpty) {
                count = 0

                doctorArrayList.clear()
                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelAppointment::class.java)
                    model.appointmentId = document.id
                    count++
                    doctorArrayList.add(model!!)
                }

                generateReport(type, doctor, startTime, endTime)
            } else {
                Toast.makeText(this@AdminReportActivity, "Insufficient data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getInventoryData(startTimestamp: Long, endTimestamp: Long, type: String) {
        var count = 0
        inventoryArrayList = ArrayList()

        val startDate = Date(startTimestamp)
        val endDate = Date(endTimestamp)
        val format = SimpleDateFormat("dd-MM-yyyy")
        val startTime = format.format(startDate)
        val endTime = format.format(endDate)

        val ref = db.collection("Prescription").whereGreaterThanOrEqualTo("updateTime", startTimestamp)
            .whereLessThanOrEqualTo("updateTime", endTimestamp).orderBy("updateTime", Query.Direction.DESCENDING)
        ref.get().addOnSuccessListener { result ->
            if (result != null && !result.isEmpty) {
                count = 0

                inventoryArrayList.clear()
                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelPrescription::class.java)
                    model.prescriptionId = document.id
                    if(model.type == "S" || model.type == "P") {
                        count++
                        inventoryArrayList.add(model!!)
                    }
                }
                Log.d("Count: ", count.toString())
                generateReport(type, "", startTime, endTime)
            } else {
                Toast.makeText(this@AdminReportActivity, "Insufficient data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAppointmentData(startTimestamp: Long, endTimestamp: Long, type: String) {
        var count = 0
        appointmentArrayList = ArrayList()

        val startDate = Date(startTimestamp)
        val endDate = Date(endTimestamp)
        val format = SimpleDateFormat("dd-MM-yyyy")
        val startTime = format.format(startDate)
        val endTime = format.format(endDate)

        val ref = db.collection("Appointment").whereGreaterThanOrEqualTo("requestTime", startTimestamp)
            .whereLessThanOrEqualTo("requestTime", endTimestamp).orderBy("requestTime", Query.Direction.DESCENDING)
        ref.get().addOnSuccessListener { result ->
            if (result != null && !result.isEmpty) {
                count = 0

                appointmentArrayList.clear()
                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelAppointment::class.java)
                    model.appointmentId = document.id
                    count++
                    appointmentArrayList.add(model!!)
                }

                Log.d("Count: ", count.toString())

                generateReport(type, "", startTime, endTime)

            } else {
                Toast.makeText(this@AdminReportActivity, "Insufficient data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateReport(type: String, doctor: String, startTime: String, endTime: String) {
        val date = System.currentTimeMillis()
        val format = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        val time = format.format(date)
        var fileType = ""
        when (type) {
            "Weekly Appointment Report", "Monthly Appointment Report" -> {
                data = appointmentArrayList
                if(type[0] == 'W') {
                    fileName = "weekly_appointment_report_$time.pdf"
                    fileType = "Weekly Appointment Report ($startTime to $endTime)"
                } else {
                    fileName = "monthly_appointment_report_$time.pdf"
                    fileType = "Monthly Appointment Report ($startTime to $endTime)"
                }
            }
            "Weekly Inventory Report", "Monthly Inventory Report" -> {
                data = inventoryArrayList
                if(type[0] == 'W') {
                    fileName = "weekly_inventory_report_$time.pdf"
                    fileType = "Weekly Inventory Report ($startTime to $endTime)"
                } else {
                    fileName = "monthly_inventory_report_$time.pdf"
                    fileType = "Monthly Inventory Report ($startTime to $endTime)"
                }
            }
            "Weekly Doctor Report", "Monthly Doctor Report" -> {
                // Filter appointments for the selected doctor
                data = doctorArrayList
                if(type[0] == 'W') {
                    fileName = "weekly_${doctor}_report_$time.pdf"
                    fileType = "Weekly Doctor Report - $doctor ($startTime to $endTime)"
                } else {
                    fileName = "monthly_${doctor}_report_$time.pdf"
                    fileType = "Monthly Doctor Report - $doctor ($startTime to $endTime)"
                }
            }
        }

        if (ContextCompat.checkSelfPermission(
                this@AdminReportActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@AdminReportActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_EXTERNAL_STORAGE_REQUEST_CODE
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                generatePDF(fileType)
            }
        }
    }

    private fun generatePDF(fileType: String) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val resolver = contentResolver
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                throw UnsupportedOperationException("VERSION.SDK_INT < Q")
            }

            try {
                uri?.let {
                    val outputStream = resolver.openOutputStream(it)
                    if (outputStream != null) {
                        val writer = PdfWriter(outputStream)
                        val pdf = PdfDocument(writer)
                        val document = Document(pdf)
                        document.setFontSize(10f)

                        val context: Context = applicationContext
                        val logoDrawable = ContextCompat.getDrawable(context, R.drawable.img_logo)
                        val logo = Image(ImageDataFactory.create(drawableToBytes(logoDrawable)))
                        logo.scaleToFit(100f, 100f) // Adjust width and height as needed

                        // Create a paragraph for the logo and set alignment to left
                        val logoParagraph = Paragraph().add(logo).setTextAlignment(TextAlignment.CENTER)
                        document.add(logoParagraph)

                        // Add Time and Report Type fields to a new paragraph with right alignment
                        val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm").format(Date())
                        val timeParagraph = Paragraph("Time: $currentTime").setTextAlignment(
                            TextAlignment.LEFT)

                        val reportTypeParagraph = Paragraph("Report Type: $fileType").setTextAlignment(TextAlignment.LEFT) // Replace with your report type

                        document.add(timeParagraph)
                        document.add(reportTypeParagraph)

                        var tasksCount = data.size // assuming data is a list

                        when (data.firstOrNull()) {
                            is ModelAppointment -> {
                                val table = Table(
                                    UnitValue.createPercentArray(floatArrayOf(15f, 15f, 15f, 15f, 10f, 15f, 15f))
                                ).useAllAvailableWidth()

                                // Add header cells
                                addCellWithFixedWidth(table, "Username", 15f)
                                addCellWithFixedWidth(table, "Description", 15f)
                                addCellWithFixedWidth(table, "Request Time", 15f)
                                addCellWithFixedWidth(table, "Schedule Time", 15f)
                                addCellWithFixedWidth(table, "Status", 10f)
                                addCellWithFixedWidth(table, "Doctor", 15f)
                                addCellWithFixedWidth(table, "Comment", 15f)

                                for (item in data) {
                                    when (item) {
                                        is ModelAppointment -> {
                                            val ref =
                                                FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid")
                                                    .equalTo(item.userId)
                                            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    if (snapshot.exists()) {
                                                        for (ds in snapshot.children) {
                                                            val user = ds.getValue(ModelAccount::class.java)
                                                            if (user != null) {
                                                                val nameText = user.name.toString()
                                                                val requestDate = java.util.Date(item.requestTime)
                                                                val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
                                                                val requestTime = format.format(requestDate)
                                                                var statusText = ""
                                                                if (item.status == "A") {
                                                                    statusText = "Accepted"
                                                                } else if (item.status == "P") {
                                                                    statusText = "Pending"
                                                                } else if (item.status == "C") {
                                                                    statusText = "Cancelled by user"
                                                                } else if (item.status == "AF") {
                                                                    statusText = "Finished"
                                                                } else if (item.status == "AC") {
                                                                    statusText = "Cancelled by admin"
                                                                } else if (item.status == "R") {
                                                                    statusText = "Rejected"
                                                                }

                                                                addCellWithFixedWidth(table, nameText, 15f)
                                                                addCellWithFixedWidth(table, item.description, 15f)
                                                                addCellWithFixedWidth(table, requestTime, 15f)
                                                                addCellWithFixedWidth(table, item.appointmentTime, 15f)
                                                                addCellWithFixedWidth(table, statusText, 10f)
                                                                if (item.doctor != null && item.doctor != "") {
                                                                    addCellWithFixedWidth(table, item.doctor, 15f)
                                                                } else {
                                                                    addCellWithFixedWidth(table, "-", 15f)
                                                                }
                                                                if (item.comment != null && item.comment != "") {
                                                                    addCellWithFixedWidth(table, item.comment, 15f)
                                                                } else  if(item.comment != null && item.comment != ""){
                                                                    addCellWithFixedWidth(table, item.reason, 15f)
                                                                } else {
                                                                    addCellWithFixedWidth(table, "-", 15f)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Decrement the counter and close the document when all tasks are completed
                                                    tasksCount--
                                                    if (tasksCount == 0) {
                                                        document.add(table)
                                                        document.close()
                                                        outputStream.close()

                                                        showDownloadNotification(it, fileName)

                                                        Toast.makeText(
                                                            this@AdminReportActivity,
                                                            "PDF report generated and saved",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    // Handle error
                                                    // Decrement the counter even if there's an error
                                                    tasksCount--
                                                    if (tasksCount == 0) {
                                                        document.add(table)
                                                        document.close()
                                                        outputStream.close()

                                                        showDownloadNotification(it, fileName)

                                                        Toast.makeText(
                                                            this@AdminReportActivity,
                                                            "PDF report generated and saved",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                            is ModelPrescription -> {
                                val table = Table(
                                    UnitValue.createPercentArray(floatArrayOf(18f, 16f, 16f, 17f, 17f, 8f, 8f))
                                ).useAllAvailableWidth()

                                addCellWithFixedWidth(table, "Update Time", 18f)
                                addCellWithFixedWidth(table, "Item Name", 16f)
                                addCellWithFixedWidth(table, "Type", 16f)
                                addCellWithFixedWidth(table, "Issued to*", 17f)
                                addCellWithFixedWidth(table, "Updated by*", 17f)
                                addCellWithFixedWidth(table, "Old Stock", 8f)
                                addCellWithFixedWidth(table, "New Stock", 8f)

                                for (item in data) {
                                    when (item) {
                                        is ModelPrescription -> {
                                            var updateName = ""

                                            if (item.updateUserId != null && item.updateUserId != "") {
                                                val ref =
                                                    FirebaseDatabase.getInstance()
                                                        .getReference("Users").orderByChild("uid")
                                                        .equalTo(item.updateUserId)
                                                ref.addListenerForSingleValueEvent(object :
                                                    ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        if (snapshot.exists()) {
                                                            val user =
                                                                snapshot.child(item.updateUserId).getValue(ModelAccount::class.java)

                                                            updateName = user?.name.toString()

                                                            val updateDate = java.util.Date(item.updateTime)
                                                            val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
                                                            val updateTime = format.format(updateDate)
                                                            var typeText = ""

                                                            if (item.type == "P") {
                                                                typeText = "Prescription"
                                                            } else if (item.type == "S") {
                                                                typeText = "Update by admin"
                                                            }
                                                            addCellWithFixedWidth(table, updateTime, 18f)
                                                            addCellWithFixedWidth(table, item.itemName, 16f)
                                                            addCellWithFixedWidth(table, typeText, 16f)

                                                            if (item.name != null && item.name != "") {
                                                                addCellWithFixedWidth(table, item.name, 17f)
                                                                addCellWithFixedWidth(table, "-", 17f)
                                                            } else {
                                                                addCellWithFixedWidth(table, "-", 17f)
                                                                addCellWithFixedWidth(table, updateName, 17f)
                                                            }
                                                            addCellWithFixedWidth(table, item.oldStock.toString(), 8f)
                                                            addCellWithFixedWidth(table, item.newStock.toString(), 8f)

                                                            // Decrement the counter and close the document when all tasks are completed
                                                            tasksCount--
                                                            if (tasksCount == 0) {
                                                                document.add(table)
                                                                document.close()
                                                                outputStream.close()

                                                                showDownloadNotification(it, fileName)

                                                                Toast.makeText(
                                                                    this@AdminReportActivity,
                                                                    "PDF report generated and saved",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        // Handle error
                                                        // Decrement the counter even if there's an error
                                                        tasksCount--
                                                        if (tasksCount == 0) {
                                                            document.add(table)
                                                            document.close()
                                                            outputStream.close()

                                                            showDownloadNotification(it, fileName)

                                                            Toast.makeText(
                                                                this@AdminReportActivity,
                                                                "PDF report generated and saved",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun addCellWithFixedWidth(table: Table, text: String, width: Float) {
        val cell = Cell().add(Paragraph(text)).setWidth(width).setKeepTogether(false)
        table.addCell(cell)
    }

    private fun showDownloadNotification(uri: Uri, fileName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a NotificationChannel for Android Oreo and above
            val channel = NotificationChannel(
                "download_channel",
                "Download Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, "download_channel")
            .setContentTitle("PDF Downloaded")
            .setContentText("Open $fileName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }

    // Handle the permission result
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    generatePDF(fileType)
                } else {
                    Toast.makeText(
                        this,
                        "Permission to write to external storage denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun drawableToBytes(drawable: Drawable?): ByteArray {
        val bitmap = (drawable as BitmapDrawable).bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
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