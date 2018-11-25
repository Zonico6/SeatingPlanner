package com.zoniklalessimo.seatingplanner.assigningRows

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.EmptyDataTable
import com.zoniklalessimo.seatingplanner.core.seating.Student
import java.util.*

class AssignRowsViewModel : ViewModel() {
    //region Tables
    private lateinit var tables: List<MutableLiveData<AssignRow>>
    fun getTablesLiveData(): List<LiveData<AssignRow>> = tables
    fun getTables(): List<AssignRow> = tables.map { it.value!! }

    fun initTables(value: List<AssignRow>): Boolean {
        return if (!this::tables.isInitialized) {
            tables = value.map {
                MutableLiveData<AssignRow>().apply {
                    this.value = it
                }
            }
            true
        } else
            false
    }

    fun removeStudentFromTable(table: LiveData<AssignRow>, student: String): Boolean {
        val row = table.value ?: return false
        val oldNames = row.students
        val newNames = oldNames - student

        tables.find { it == table }?.value = row.copy(students = newNames)

        students.value = students.value ?: emptyList<String>() - student

        return oldNames != newNames
    }

    fun addStudentToTable(table: LiveData<AssignRow>, student: String) {
        val row = table.value ?: return

        tables.find { it == table }?.value = row.copy(students = row.students + student)
    }
    //endregion

    //region Students
    private val students: MutableLiveData<List<String>> = MutableLiveData()

    fun getStudentNamesLiveData() = students as LiveData<List<String>>

    val studentNames: List<String>
        get() = students.value ?: emptyList()

    fun initStudents(value: List<String>): Boolean {
        if (students.value == null) {
            students.value = value
            return true
        }
        return false
    }

    fun removeStudent(name: String): Boolean {
        students.value = students.value ?: emptyList<String>() - name
        return true
    }
    //endregion
}

data class AssignRow(val students: List<String>, val xBias: Float, val yBias: Float, val seatCount: Int, val separators: SortedSet<Int>) {

    constructor(table: EmptyDataTable, students: List<String> = emptyList()) :
            this(students, table.xBias, table.yBias, table.seatCount, table.separators)
}