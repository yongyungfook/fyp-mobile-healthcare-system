package com.example.icare

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.adapter.AdapterAdminRequest
import com.example.icare.adapter.AdapterMessage
import com.example.icare.databinding.ActivityAdminChatBinding
import com.example.icare.model.ModelMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.sql.Date
import java.text.SimpleDateFormat

class AdminChatActivity : AppCompatActivity() {
    private var binding: ActivityAdminChatBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var db = Firebase.firestore

    private lateinit var messageAdapter: AdapterMessage
    private lateinit var messageList: ArrayList<ModelMessage>

    private lateinit var chatRv: RecyclerView
    private lateinit var messageEt: EditText
    private lateinit var sendBtn: ImageView

    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_chat)

        binding = ActivityAdminChatBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        chatRv = binding!!.chatRv
        messageEt = binding!!.messageEt
        sendBtn = binding!!.sendIv
        messageList = ArrayList()
        messageAdapter = AdapterMessage(this, messageList)

        chatRv.layoutManager = LinearLayoutManager(this)
        chatRv.adapter = messageAdapter

        val intent = intent
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = firebaseAuth!!.currentUser?.uid

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid



        val ref = db.collection("Chat").document(senderRoom!!).collection("Message").orderBy("timestamp", Query.Direction.ASCENDING)
        ref.addSnapshotListener { result, e ->

            if (e != null) {
                // Handle errors
                return@addSnapshotListener
            }

            if (result != null && !result.isEmpty) {
                messageList.clear()

                for (document in result) {
                    // Access document data using document.data
                    val model = document.toObject(ModelMessage::class.java)
                    messageList.add(model)
                }
                messageAdapter = AdapterMessage(this@AdminChatActivity, messageList)

                chatRv.adapter = messageAdapter

            }
        }

        sendBtn.setOnClickListener{
            val message = messageEt.text.trim().toString();
            val messageObject = ModelMessage(message, senderUid, System.currentTimeMillis())

            val ref = FirebaseDatabase.getInstance().getReference("Users")

            // Attach a listener to read the data at our posts reference
            ref.child(receiverUid!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val token = "" + snapshot.child("token").value

                        db.collection("Chat").document(senderRoom!!).collection("Message").add(messageObject).addOnSuccessListener {
                            db.collection("Chat").document(receiverRoom!!).collection("Message").add(messageObject).addOnSuccessListener {
                                val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                val userName = sharedPref.getString("name", "X")

                                val log = hashMapOf(
                                    "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                    "time" to System.currentTimeMillis(),
                                    "detail" to "$userName sent a new message: \"$message\"",
                                    "level" to 1
                                )

                                db.collection("Log").add(log)
                                    .addOnSuccessListener {

                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AdminChatActivity, "Failed to upload item!",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                SendMessageNotificationAsyncTask(
                                    this@AdminChatActivity, token, message).execute()
                                messageAdapter.notifyDataSetChanged()
                            }
                        }

                        messageEt.setText("")
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        println("The read failed: " + databaseError.code)
                    }
                })
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
                startActivity(Intent(this@AdminChatActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@AdminChatActivity, AdminAppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminChatActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminChatActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminChatActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminChatActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminChatActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminChatActivity, HomeActivity::class.java)
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

    private fun clearSharedPreferences() {
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove("email")
        editor.remove("name")
        editor.remove("role")
        editor.apply()
    }

    private class SendMessageNotificationAsyncTask(
        private val context: Context,
        private val token: String,
        private val message: String
    ) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void?): Boolean {
            try {
                val fcmToken = token
                val title = "Your received a new message!"
                val body = message
                val serverKey = "AAAADf-jbH8:APA91bEifGaEZSbQbqk91W-hAQumfwaECT-9OGz8cliZs4YoC_rHHlUbt4KlfD7AEevWcUm7ZddhK57Ch0SK12Nh4zv9k7JAApY7LwkYqdbrRZrue7qWk7LjFsPTFdmuDO7U-IIgRTtb"
                val url = "https://fcm.googleapis.com/fcm/send"

                val json = """
                        {
                            "to": "$fcmToken",
                            "notification": {
                                "title": "$title",
                                "body": "\"$body\""
                            }
                        }
                    """.trimIndent()

                val client = OkHttpClient()
                val requestBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "key=$serverKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                Log.e("Notification", "Failed to send notification. Response code: ${response.code}")
                Log.e("Notification", "Response body: ${response.body?.string()}")
                return response.isSuccessful
            } catch (e: Exception) {
                return false
            }
        }

        override fun onPostExecute(result: Boolean) {
            // Handle the result on the main thread
            if (result) {
                Toast.makeText(context, "Notification sent successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to send notification", Toast.LENGTH_SHORT).show()
            }
        }
    }
}