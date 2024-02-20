package com.example.icare

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.icare.adapter.AdapterMessage
import com.example.icare.databinding.ActivityChatBinding
import com.example.icare.model.ModelMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class ChatActivity : AppCompatActivity() {
    private var binding: ActivityChatBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    var db = Firebase.firestore

    private lateinit var messageAdapter: AdapterMessage
    private lateinit var messageList: ArrayList<ModelMessage>

    private lateinit var chatRv: RecyclerView
    private lateinit var messageEt: EditText
    private lateinit var sendBtn: ImageView

    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        binding = ActivityChatBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")

        binding!!.titleDashboardUser.text = name
        binding!!.subtitleDashboardUser.text = email

        chatRv = binding!!.chatRv
        messageEt = binding!!.messageEt
        sendBtn = binding!!.sendIv
        messageList = ArrayList()
        messageAdapter = AdapterMessage(this@ChatActivity, messageList)

        chatRv.layoutManager = LinearLayoutManager(this)
        chatRv.adapter = messageAdapter

        val intent = intent
        val receiverUid = intent.getStringExtra("updateUserId")
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
                messageAdapter = AdapterMessage(this@ChatActivity, messageList)

                chatRv.adapter = messageAdapter

            }
        }

        sendBtn.setOnClickListener{
            val message = messageEt.text.trim().toString();
            val messageObject = ModelMessage(message, senderUid, System.currentTimeMillis())

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
                            Toast.makeText(this, "Failed to upload item!",
                                Toast.LENGTH_SHORT).show()
                        }

                    messageAdapter.notifyDataSetChanged()
                }
            }

            messageEt.setText("")
        }

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
                startActivity(Intent(this@ChatActivity, UserHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@ChatActivity, AppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@ChatActivity, ProfileActivity::class.java))
            } else if (id == R.id.nav_aboutus) {
                startActivity(Intent(this@ChatActivity, AboutUsActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@ChatActivity, HomeActivity::class.java)
                firebaseAuth!!.signOut()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            true
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