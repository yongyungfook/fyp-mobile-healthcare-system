    package com.example.icare.adapter

    import android.app.AlertDialog
    import android.app.ProgressDialog
    import android.content.Context
    import android.os.AsyncTask
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.*
    import androidx.core.util.rangeTo
    import androidx.recyclerview.widget.RecyclerView
    import com.example.icare.databinding.RowAdminRequestBinding
    import com.example.icare.model.ModelAppointment
    import com.google.firebase.Firebase
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.DataSnapshot
    import com.google.firebase.database.DatabaseError
    import com.google.firebase.database.FirebaseDatabase
    import com.google.firebase.database.ValueEventListener
    import com.google.firebase.firestore.firestore
    import okhttp3.MediaType.Companion.toMediaType
    import okhttp3.OkHttpClient
    import okhttp3.Request
    import okhttp3.RequestBody.Companion.toRequestBody
    import java.text.SimpleDateFormat

    class AdapterAdminRequest: RecyclerView.Adapter<AdapterAdminRequest.HolderAppointment> {
        private val context: Context
        public var appointmentArrayList: ArrayList<ModelAppointment>

        private lateinit var binding: RowAdminRequestBinding

        constructor(context: Context, appointmentArrayList: ArrayList<ModelAppointment>) {
            this.context = context
            this.appointmentArrayList = appointmentArrayList
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAppointment {
            binding = RowAdminRequestBinding.inflate(LayoutInflater.from(context), parent, false)
            return HolderAppointment(binding.root)
        }

        override fun getItemCount(): Int {
            return appointmentArrayList.size
        }

        override fun onBindViewHolder(holder: HolderAppointment, position: Int) {
            val model = appointmentArrayList[position]
            val id = model.appointmentId
            val userId = model.userId
            val appointmentTime = model.appointmentTime
            val requestTimestamp = model.requestTime
            val updateTime = model.updateTime
            val updateUserId = model.updateUserId
            val status = model.status
            val description = model.description
            val comment = model.comment

            val date = java.util.Date(requestTimestamp)
            val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
            val requestTime = format.format(date)

            holder.scheduleTimeTv.text = "Schedule Time: $appointmentTime"
            holder.descriptionTv.text = "Description: $description"
            holder.submitTimeTv.text = "Submit Time: $requestTime"

            val ref = FirebaseDatabase.getInstance().getReference("Users").
            ref.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = "" + snapshot.child("name").value

                        holder.nameTv.text = "Name: $name"
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        println("The read failed: " + databaseError.code)
                    }
                })

            holder.acceptBtn.setOnClickListener {
                acceptRequest(userId)
            }

            holder.rejectBtn.setOnClickListener {
                rejectRequest(userId)
            }

        }

        private var db = Firebase.firestore

        private fun rejectRequest(userId: String) {
            val inputEditTextField = EditText(context)
            var reason = ""
            val builder= AlertDialog.Builder(context)
            builder.setTitle("Reject")
                .setMessage("Are you sure you want to reject this request? Enter the reason.")
                .setView(inputEditTextField)
                .setPositiveButton("Confirm") {a, d->
                    reason = inputEditTextField.text.toString()
                    var progressDialog = ProgressDialog(context)
                    progressDialog!!.setTitle("Please Wait...")
                    progressDialog!!.setCanceledOnTouchOutside(false)
                    progressDialog!!.setMessage("Rejecting...")
                    progressDialog!!.show()

                    db.collection("Appointment")
                        .whereEqualTo("status", "P")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                db.collection("Appointment").document(document.id)
                                    .update(mapOf("status" to "R", "reason" to reason, "updateUserId" to FirebaseAuth.getInstance().currentUser!!.uid, "updateTime" to System.currentTimeMillis()))
                                    .addOnSuccessListener {

                                        val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                        val userName = sharedPref.getString("name", "X")

                                        val ref = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(userId)
                                        ref.addListenerForSingleValueEvent(object: ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val log = hashMapOf(
                                                    "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                                    "time" to System.currentTimeMillis(),
                                                    "detail" to "Request (${document.id}) made by ${snapshot.child(userId).child("name").value.toString()} has been rejected by $userName",
                                                    "level" to 2
                                                )

                                                db.collection("Log").add(log)
                                                    .addOnSuccessListener {

                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(context, "Failed to upload item!",
                                                            Toast.LENGTH_SHORT).show()
                                                    }


                                                SendRejectNotificationAsyncTask(context, snapshot.child(userId).child("token").value.toString(), reason).execute()

                                                progressDialog.dismiss()

                                                Toast.makeText(context, "Request rejected.", Toast.LENGTH_SHORT).show()
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }

                                        })
                                    }
                                    .addOnFailureListener { e ->
                                        // Update failed
                                        Toast.makeText(context, "Unable to accept due to ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            // Document retrieval failed
                            Toast.makeText(context, "Unable to retrieve documents due to ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel") {a, d->
                    a.dismiss()
                }.show()

        }

        private fun acceptRequest(userId: String) {
            val builder= AlertDialog.Builder(context)
            builder.setTitle("Accept")
                .setMessage("Are you sure you want to accept this request? The user will be notified.")
                .setPositiveButton("Confirm") {a, d->
                    var progressDialog = ProgressDialog(context)
                    progressDialog!!.setTitle("Please Wait...")
                    progressDialog!!.setCanceledOnTouchOutside(false)
                    progressDialog!!.setMessage("Accepting...")
                    progressDialog!!.show()

                    db.collection("Appointment")
                        .whereEqualTo("status", "P")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                db.collection("Appointment").document(document.id)
                                    .update(mapOf("status" to "A", "updateUserId" to FirebaseAuth.getInstance().currentUser!!.uid, "updateTime" to System.currentTimeMillis()))
                                    .addOnSuccessListener {
                                        val ref = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(userId)
                                        ref.addListenerForSingleValueEvent(object: ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                                val userName = sharedPref.getString("name", "X")


                                                val log = hashMapOf(
                                                    "userId" to FirebaseAuth.getInstance().currentUser!!.uid,
                                                    "time" to System.currentTimeMillis(),
                                                    "detail" to "Request (${document.id}) made by ${snapshot.child(userId).child("name").value.toString()} has been accepted by $userName",
                                                    "level" to 2
                                                )

                                                db.collection("Log").add(log)

                                                Log.d("Token: ", "${snapshot.child(userId).child("token").value.toString()}")
                                                SendAcceptNotificationAsyncTask(context, snapshot.child(userId).child("token").value.toString()).execute()

                                                progressDialog.dismiss()

                                                Toast.makeText(context, "Request accepted.", Toast.LENGTH_SHORT).show()
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }

                                        })
                                    }
                                    .addOnFailureListener { e ->
                                        // Update failed
                                        Toast.makeText(context, "Unable to accept due to ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            // Document retrieval failed
                            Toast.makeText(context, "Unable to retrieve documents due to ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel") {a, d->
                    a.dismiss()
                }.show()

        }

        private class SendAcceptNotificationAsyncTask(
            private val context: Context,
            private val token: String
        ) : AsyncTask<Void, Void, Boolean>() {

            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    val fcmToken = token
                    val title = "Your appointment has been accepted!"
                    val body = "Please reach the clinic on time, we are looking forward to see you!"
                    val serverKey = "AAAADf-jbH8:APA91bEifGaEZSbQbqk91W-hAQumfwaECT-9OGz8cliZs4YoC_rHHlUbt4KlfD7AEevWcUm7ZddhK57Ch0SK12Nh4zv9k7JAApY7LwkYqdbrRZrue7qWk7LjFsPTFdmuDO7U-IIgRTtb"
                    val url = "https://fcm.googleapis.com/fcm/send"

                    val json = """
                        {
                            "to": "$fcmToken",
                            "notification": {
                                "title": "$title",
                                "body": "$body"
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

        private class SendRejectNotificationAsyncTask(
            private val context: Context,
            private val token: String,
            private val reason: String
        ) : AsyncTask<Void, Void, Boolean>() {

            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    val fcmToken = token
                    val title = "Your appointment has been rejected!"
                    val body = "The reason is $reason, please schedule another appointment!"
                    val serverKey = "AAAADf-jbH8:APA91bEifGaEZSbQbqk91W-hAQumfwaECT-9OGz8cliZs4YoC_rHHlUbt4KlfD7AEevWcUm7ZddhK57Ch0SK12Nh4zv9k7JAApY7LwkYqdbrRZrue7qWk7LjFsPTFdmuDO7U-IIgRTtb"
                    val url = "https://fcm.googleapis.com/fcm/send"

                    val json = """
                        {   
                            "to": "$fcmToken",
                            "notification": {
                                "title": "$title",
                                "body": "$body"
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

        inner class HolderAppointment(itemView: View): RecyclerView.ViewHolder(itemView) {
            var nameTv: TextView = binding.nameTv
            var scheduleTimeTv: TextView = binding.scheduleTimeTv
            var submitTimeTv: TextView = binding.submitTimeTv
            var descriptionTv: TextView = binding.descriptionTv
            var acceptBtn: Button = binding.acceptBtn
            var rejectBtn: Button = binding.rejectBtn
        }

    }
