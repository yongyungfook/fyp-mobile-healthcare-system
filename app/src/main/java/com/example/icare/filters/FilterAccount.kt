package com.example.icare.filters

import android.util.Log
import android.widget.Filter
import com.example.icare.adapter.AdapterAccount
import com.example.icare.model.ModelAccount

class FilterAccount: Filter {
    private var filterList: ArrayList<ModelAccount>

    private var adapterAccount: AdapterAccount

    constructor(filterList: ArrayList<ModelAccount>, adapterAccount: AdapterAccount) : super() {
        this.filterList = filterList
        this.adapterAccount = adapterAccount
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        if(constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().uppercase().trim()
            val filteredModels:ArrayList<ModelAccount> = ArrayList()
            for(i in 0 until filterList.size) {
                if(filterList[i].name.toString().uppercase().contains(constraint)) {
                    filteredModels.add(filterList[i])
                } else {
                    Log.d("FilterAccount", "Item ${filterList[i].name} excluded.")
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
        adapterAccount.accountArrayList = results.values as ArrayList<ModelAccount>

        adapterAccount.notifyDataSetChanged()
    }
}