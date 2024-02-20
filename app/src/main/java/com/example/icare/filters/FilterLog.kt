package com.example.icare.filters

import android.widget.Filter
import com.example.icare.adapter.AdapterAccount
import com.example.icare.adapter.AdapterInventory
import com.example.icare.adapter.AdapterLog
import com.example.icare.model.ModelAccount
import com.example.icare.model.ModelInventory
import com.example.icare.model.ModelLog

class FilterLog: Filter {
    private var filterList: ArrayList<ModelLog>

    private var adapterLog: AdapterLog

    constructor(filterList: ArrayList<ModelLog>, adapterLog: AdapterLog) : super() {
        this.filterList = filterList
        this.adapterLog = adapterLog
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        if(constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().uppercase()
            val filteredModels:ArrayList<ModelLog> = ArrayList()
            for(i in 0 until filterList.size) {
                if(filterList[i].detail.uppercase().contains(constraint) || filterList[i].detail.uppercase().contains(constraint)) {
                    filteredModels.add(filterList[i])
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            results.count = filterList.size
            results.values = filterList
        }

        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        adapterLog.logArrayList = results.values as ArrayList<ModelLog>

        adapterLog.notifyDataSetChanged()
    }
}