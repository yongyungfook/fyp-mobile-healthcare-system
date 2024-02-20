package com.example.icare.filters

import android.widget.Filter
import com.example.icare.adapter.AdapterAccount
import com.example.icare.adapter.AdapterInventory
import com.example.icare.adapter.AdapterInventoryHistory
import com.example.icare.model.ModelAccount
import com.example.icare.model.ModelInventory
import com.example.icare.model.ModelPrescription

class FilterPrescription: Filter {
    private var filterList: ArrayList<ModelPrescription>

    private var adapterInventoryHistory: AdapterInventoryHistory

    constructor(filterList: ArrayList<ModelPrescription>, adapterInventoryHistory: AdapterInventoryHistory) : super() {
        this.filterList = filterList
        this.adapterInventoryHistory = adapterInventoryHistory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        if(constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().uppercase()
            val filteredModels:ArrayList<ModelPrescription> = ArrayList()
            for(i in 0 until filterList.size) {
                if(filterList[i].itemName.uppercase().contains(constraint) || filterList[i].name.uppercase().contains(constraint) || filterList[i].oldName.uppercase().contains(constraint) || filterList[i].newName.uppercase().contains(constraint) || filterList[i].editName.uppercase().contains(constraint)) {
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
        adapterInventoryHistory.prescriptionArrayList = results.values as ArrayList<ModelPrescription>

        adapterInventoryHistory.notifyDataSetChanged()
    }
}