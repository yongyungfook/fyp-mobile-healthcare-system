package com.example.icare.filters

import android.widget.Filter
import com.example.icare.adapter.AdapterAccount
import com.example.icare.adapter.AdapterInventory
import com.example.icare.model.ModelAccount
import com.example.icare.model.ModelInventory

class FilterInventory: Filter {
    private var filterList: ArrayList<ModelInventory>

    private var adapterInventory: AdapterInventory

    constructor(filterList: ArrayList<ModelInventory>, adapterInventory: AdapterInventory) : super() {
        this.filterList = filterList
        this.adapterInventory = adapterInventory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        if(constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().uppercase()
            val filteredModels:ArrayList<ModelInventory> = ArrayList()
            for(i in 0 until filterList.size) {
                if(filterList[i].itemName.uppercase().contains(constraint)) {
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
        adapterInventory.inventoryArrayList = results.values as ArrayList<ModelInventory>

        adapterInventory.notifyDataSetChanged()
    }
}