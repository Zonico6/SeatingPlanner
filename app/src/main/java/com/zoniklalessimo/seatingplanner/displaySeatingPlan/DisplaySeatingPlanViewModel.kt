package com.zoniklalessimo.seatingplanner.displaySeatingPlan

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zoniklalessimo.seatingplanner.PlanType
import com.zoniklalessimo.seatingplanner.SeatedDataTable
import com.zoniklalessimo.seatingplanner.core.plan.TableRow

class DisplaySeatingPlanViewModel : ViewModel() {
    private lateinit var seatedTables: List<MutableLiveData<SeatedDataTable>>
    val tableCount get() = seatedTables.size

    fun initSeatedTables(plan: PlanType): Boolean {
        if (!this::seatedTables.isInitialized) {
            seatedTables = plan.map {
                MutableLiveData<SeatedDataTable>().apply {
                    this.value = it
                }
            }
            return true
        }
        return false
    }
    fun getSeatedTables() = seatedTables as List<LiveData<SeatedDataTable>>

    /**
     * Seats one student at the place of the other and vice versa.
     */
    fun swapStudents(first: String, second: String) {
        // Assuming all names are unique
        var row = -1
        var seatInRow = -1
        for ((index, table) in seatedTables.withIndex()) {
            fun MutableLiveData<SeatedDataTable>.setWithNameAtIndex(name: String, index: Int) {
                val new = value!!
                val students = new.students.toMutableList()
                students[index] = name
                new.students = students
                value = new
            }
            fun trySwap(seat: Int, oldName: String, newName: String): Boolean {
                return if (seat == -1) {
                    false
                } else {
                    if (row == -1) {
                        row = index
                        seatInRow = seat
                        false
                    } else {
                        seatedTables[row].setWithNameAtIndex(newName, seatInRow)
                        seatedTables[index].setWithNameAtIndex(oldName, seat)
                        true
                    }
                }
            }
            val oneSeat = table.value!!.seatOf(first)
            if (trySwap(oneSeat, second, first)) {
                return
            }
            val otherSeat = table.value!!.seatOf(second)
            if (trySwap(otherSeat, first, second)) {
                return
            }
        }
    }
}