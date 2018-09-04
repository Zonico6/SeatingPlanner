package com.zoniklalessimo.seatingplanner.choosing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import kotlin.properties.Delegates.observable

class ChoosePlanDialogViewModel : ViewModel() {
    private var entries = MutableLiveData<List<ChoosePlanEntry>>()

    fun getEntries(): LiveData<List<ChoosePlanEntry>> = entries

    var src: File? by observable(null as File?) { _, _, new ->
        if (new != null)
            fetchEntries(new)
        else
            entries = MutableLiveData()
    }

    private fun fetchEntries(src: File) {

    }
}

data class ChoosePlanEntry(val name: String, val rows: Int, val seats: Int, val src: File)