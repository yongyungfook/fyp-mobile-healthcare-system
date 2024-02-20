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
import androidx.core.view.GravityCompat
import com.example.icare.databinding.ActivityAdminAddInventoryBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import java.util.*

class AdminAddInventoryActivity: AppCompatActivity()  {
    private var binding: ActivityAdminAddInventoryBinding? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var progressDialog: ProgressDialog? = null

    private var db = Firebase.firestore

    private var storageReference = Firebase.storage.reference

    private lateinit var imageUploader: ActivityResultLauncher<Intent>
    private lateinit var selectedImageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_inventory)

        binding = ActivityAdminAddInventoryBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please Wait...")
        progressDialog!!.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()

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
            } else if (!::selectedImageUri.isInitialized) {
                // Check if the image has been selected
                Toast.makeText(this, "Please upload an image!", Toast.LENGTH_SHORT).show()
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
                startActivity(Intent(this@AdminAddInventoryActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_appointment) {
                drawer.closeDrawer(GravityCompat.START)
            } else if (id == R.id.nav_profile) {
                startActivity(Intent(this@AdminAddInventoryActivity, AdminProfileActivity::class.java))
            } else if (id == R.id.nav_inventory) {
                startActivity(Intent(this@AdminAddInventoryActivity, AdminHomeActivity::class.java))
            } else if (id == R.id.nav_report) {
                startActivity(Intent(this@AdminAddInventoryActivity, AdminReportActivity::class.java))
            } else if (id == R.id.nav_log) {
                startActivity(Intent(this@AdminAddInventoryActivity, AdminLogActivity::class.java))
            } else if (id == R.id.nav_moderation) {
                startActivity(Intent(this@AdminAddInventoryActivity, AdminModerationActivity::class.java))
            } else if (id == R.id.nav_logout) {
                val log = hashMapOf(
                    "userId" to firebaseAuth!!.currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$name (${firebaseAuth!!.currentUser!!.uid}) has logged out from the system",
                    "level" to 1
                )

                db.collection("Log").add(log)

                clearSharedPreferences()
                val intent = Intent(this@AdminAddInventoryActivity, HomeActivity::class.java)
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imageUploader.launch(intent)
    }

    private fun uploadImage() {
        progressDialog!!.setMessage("Uploading image and details...")
        progressDialog!!.show()

        // Create a reference to the Firebase Storage location
        val imageRef = storageReference.child("images/${UUID.randomUUID()}.jpg")

        // Upload the image
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
    }

    private fun uploadInventory(imageUrl: String) {
        val item = hashMapOf(
            "itemName" to binding!!.nameEt.text.trim().toString(),
            "userId" to firebaseAuth!!.uid,
            "image" to imageUrl,
            "addedTime" to System.currentTimeMillis(),
            "description" to binding!!.descriptionEt.text.trim().toString(),
            "stock" to binding!!.stockEt.text.trim().toString().toInt()
        )

        db.collection("Inventory").add(item)
            .addOnSuccessListener { documentReference ->
                val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val userName = sharedPref.getString("name", "X")

                val log = hashMapOf(
                    "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                    "time" to System.currentTimeMillis(),
                    "detail" to "$userName added a new item\n" +
                            "Name: ${binding!!.nameEt.text.trim()}\n" +
                            "Description: ${binding!!.descriptionEt.text.trim()}\n" +
                            "Stock: ${binding!!.stockEt.text.trim().toString().toInt()}",
                    "level" to 1
                )

                db.collection("Log").add(log)

                val item = hashMapOf(
                    "detail" to "$userName added a new item\n" +
                            "Name: ${binding!!.nameEt.text.trim()}\n" +
                            "Description: ${binding!!.descriptionEt.text.trim()}\n" +
                            "Stock: ${binding!!.stockEt.text.trim().toString().toInt()}",
                    "updateTime" to System.currentTimeMillis(),
                    "updateUserId" to firebaseAuth!!.currentUser!!.uid,
                    "itemId" to documentReference.id,
                    "itemName" to binding!!.nameEt.text.trim().toString(),
                    "type" to "A",

                )

                db.collection("Prescription").add(item)

                Toast.makeText(this, "Item uploaded!",
                    Toast.LENGTH_SHORT).show()
                progressDialog!!.dismiss()
                startActivity(Intent(this@AdminAddInventoryActivity, AdminInventoryActivity::class.java))

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
