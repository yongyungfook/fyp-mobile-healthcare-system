package com.example.icare

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import com.example.icare.databinding.ActivityAdminEditInventoryBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import java.util.*

class AdminEditInventoryActivity: AppCompatActivity()  {
    private var binding: ActivityAdminEditInventoryBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var progressDialog: ProgressDialog? = null

    private var db = Firebase.firestore

    private var storageReference = Firebase.storage.reference

    private lateinit var imageUploader: ActivityResultLauncher<Intent>
    private lateinit var selectedImageUri: Uri
    private lateinit var existingImageUri: Uri
    private var oldName: String = ""
    private var oldStock: Int = 0
    private var oldDescription: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_inventory)

        binding = ActivityAdminEditInventoryBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please Wait...")
        progressDialog!!.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()

        loadItem()

        imageUploader = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // Handle the selected image URI
                    selectedImageUri = uri
                    Picasso.get().load(uri).into(binding!!.imageIv)
                }
            }
        }

        binding!!.uploadBtn.setOnClickListener {
            openImagePicker()
        }

        binding!!.submitBtn.setOnClickListener {
            if(TextUtils.isEmpty(binding!!.nameEt.text) || TextUtils.isEmpty(binding!!.descriptionEt.text) || TextUtils.isEmpty(binding!!.stockEt.text)) {
                Toast.makeText(this, "Please fill out all the field(s)!", Toast.LENGTH_SHORT).show()
            } else {
                uploadImage()
            }
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
                startActivity(Intent(this@AdminEditInventoryActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                startActivity(Intent(this@AdminEditInventoryActivity, AdminAppointmentActivity::class.java))
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminEditInventoryActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminEditInventoryActivity, AdminInventoryActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminEditInventoryActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminEditInventoryActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminEditInventoryActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminEditInventoryActivity, HomeActivity::class.java)
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

    private fun loadItem() {
        val id = intent.getStringExtra("id").toString()

        db.collection("Inventory").document(id).get().addOnSuccessListener{ document ->
            if(document.exists()) {
                binding!!.nameEt.setText(document.getString("itemName"))
                binding!!.descriptionEt.setText(document.getString("description"))
                binding!!.stockEt.setText(document.getLong("stock")!!.toInt().toString())
                oldName = document.getString("itemName").toString()
                oldStock = document.getLong("stock")!!.toInt()
                oldDescription = document.getString("description").toString()

                existingImageUri = document.getString("image")!!.toUri()
                Picasso.get().load(document.getString("image")!!.toUri()).into(binding!!.imageIv)
                selectedImageUri = document.getString("image")!!.toUri()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imageUploader.launch(intent)
    }

    private fun uploadImage() {
        progressDialog!!.setMessage("Uploading image and details...")
        progressDialog!!.show()

        // Create a reference to the Firebase Storage location
        val imageRef = storageReference.child("images/${UUID.randomUUID()}.jpg")

        if(selectedImageUri != existingImageUri) {
            imageRef.putFile(selectedImageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully, now get the download URL
                    imageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            // Use the image URL as needed
                            val imageUrl = uri.toString()

                            // Save the image URL to Firestore
                            uploadInventory(imageUrl)
                        }
                }
                .addOnFailureListener { exception ->
                    // Handle unsuccessful uploads
                }
        } else {
            uploadInventory("")
        }
    }

    private fun uploadInventory(imageUrl: String) {
        val id = intent.getStringExtra("id").toString()
        var editName = ""
        val item = hashMapOf(
            "itemName" to binding!!.nameEt.text.trim().toString(),
            "userId" to firebaseAuth!!.uid,
            "image" to if (selectedImageUri != existingImageUri) imageUrl else existingImageUri, // Use existing imageUrl if no new image is selected
            "addedTime" to System.currentTimeMillis(),
            "description" to binding!!.descriptionEt.text.trim().toString(),
            "stock" to binding!!.stockEt.text.trim().toString().toInt()
        )

        db.collection("Inventory").document(id).set(item)
            .addOnSuccessListener {
                val ref = FirebaseDatabase.getInstance().getReference("Users").child(firebaseAuth!!.currentUser!!.uid)
                ref.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        editName = snapshot.child("name").value.toString()

                        if(oldName != binding!!.nameEt.text.toString()) {
                            val item = hashMapOf(
                                "oldName" to oldName,
                                "newName" to binding!!.nameEt.text.toString(),
                                "updateTime" to System.currentTimeMillis(),
                                "updateUserId" to firebaseAuth!!.currentUser!!.uid,
                                "itemId" to id,
                                "itemName" to oldName,
                                "type" to "N",
                                "editName" to editName

                            )

                            db.collection("Prescription").add(item)

                            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            val userName = sharedPref.getString("name", "X")

                            val log = hashMapOf(
                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                "time" to System.currentTimeMillis(),
                                "detail" to "$userName edited the name of the item ($id) from $oldName to ${binding!!.nameEt.text}",
                                "level" to 1
                            )

                            db.collection("Log").add(log)
                        }

                        if(oldStock != binding!!.stockEt.text.toString().toInt()) {
                            val item = hashMapOf(
                                "oldStock" to oldStock,
                                "newStock" to binding!!.stockEt.text.toString().toInt(),
                                "updateTime" to System.currentTimeMillis(),
                                "updateUserId" to firebaseAuth!!.currentUser!!.uid,
                                "itemId" to id,
                                "itemName" to oldName,
                                "type" to "S",
                                "editName" to editName
                            )

                            db.collection("Prescription").add(item)

                            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            val userName = sharedPref.getString("name", "X")

                            val log = hashMapOf(
                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                "time" to System.currentTimeMillis(),
                                "detail" to "$userName edited the stock of the item ($id) from $oldStock to ${binding!!.stockEt.text.toString().toInt()}",
                                "level" to 1
                            )

                            db.collection("Log").add(log)
                        }

                        if(oldDescription != binding!!.descriptionEt.text.toString()) {
                            val item = hashMapOf(
                                "oldDescription" to oldDescription,
                                "newDescription" to binding!!.descriptionEt.text.toString(),
                                "updateTime" to System.currentTimeMillis(),
                                "updateUserId" to firebaseAuth!!.currentUser!!.uid,
                                "itemId" to id,
                                "itemDescription" to oldDescription,
                                "type" to "D",
                                "editName" to editName
                            )

                            db.collection("Prescription").add(item)

                            val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            val userName = sharedPref.getString("name", "X")

                            val log = hashMapOf(
                                "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                "time" to System.currentTimeMillis(),
                                "detail" to "$userName edited the description of the item ($id) from $oldDescription to ${binding!!.descriptionEt.text.toString()}",
                                "level" to 1
                            )

                            db.collection("Log").add(log)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })

                Toast.makeText(this, "Item uploaded!",
                    Toast.LENGTH_SHORT).show()
                progressDialog!!.dismiss()
                startActivity(Intent(this@AdminEditInventoryActivity, AdminInventoryActivity::class.java))

            }
            .addOnFailureListener {
                progressDialog!!.dismiss()
                Toast.makeText(this, "Failed to upload item!",
                    Toast.LENGTH_SHORT).show()
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
