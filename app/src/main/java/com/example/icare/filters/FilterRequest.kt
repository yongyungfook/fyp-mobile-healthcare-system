package com.example.icare.filters

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Filter
import com.example.icare.adapter.AdapterAdminRequestHistory
import com.example.icare.model.ModelAppointment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat

class FilterRequest(
    private var filterList: ArrayList<ModelAppointment>,
    private var adapterAppointment: AdapterAdminRequestHistory
) : Filter() {

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().uppercase().trim()
            val filteredModels: ArrayList<ModelAppointment> = ArrayList()
            var count = 0

            for (i in 0 until filterList.size) {
                val ref = FirebaseDatabase.getInstance().getReference("Users").
                ref.child(filterList[i].userId)
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val date = java.util.Date(filterList[i].requestTime)
                        val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
                        val requestTime = format.format(date)

                        if (snapshot.child("name").value.toString().uppercase()
                                .contains(constraint) || filterList[i].description
                                .uppercase().contains(constraint) || requestTime.contains(constraint)
                        ) {
                            filteredModels.add(filterList[i])
                        } else {
                            Log.d("FilterAccount", "Item ${snapshot.child("name").value} excluded.")
                        }

                        count++

                        if (count == filterList.size) {
                            // Notify that filtering is complete when all data is processed
                            results.count = filteredModels.size
                            results.values = filteredModels
                            publishFilterResults(results)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        println("The read failed: " + databaseError.code)
                        count++
                    }
                })
            }
        } else {
            results.count = filterList.size
            results.values = filterList
            publishFilterResults(results)
        }

        return results
    }

    private fun publishFilterResults(results: FilterResults) {
        Handler(Looper.getMainLooper()).post {
            adapterAppointment.appointmentArrayList = results.values as ArrayList<ModelAppointment>
            adapterAppointment.notifyDataSetChanged()
        }
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        // This method is intentionally left blank,
        // as the filtering results are published in performFiltering
    }
}