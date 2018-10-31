package com.zoniklalessimo.seatingplanner.choosingStudentSet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zoniklalessimo.seatingplanner.ChooseDialogViewModel
import com.zoniklalessimo.seatingplanner.schema.StudentSet

interface ChooseStudentSetViewModel : ChooseDialogViewModel {
    val studentSets: MutableLiveData<List<StudentSet>>
    val setsCount: Int get() = studentSets.value?.size ?: 0

    fun getStudentSets(): LiveData<List<StudentSet>> {
        if (studentSets.value == null) {
            fetchSets()
        }
        return studentSets
    }

    override fun createNewItem(title: String) {
        createSet(title)
    }

    fun createSet(title: String) {

    }

    fun onCleared() {

    }

    fun getSetAt(i: Int): StudentSet? {

    }
}