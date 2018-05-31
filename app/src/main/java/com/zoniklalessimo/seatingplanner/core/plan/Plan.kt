package com.zoniklalessimo.seatingplanner.core.plan

import com.zoniklalessimo.seatingplanner.core.seating.Student
import com.zoniklalessimo.seatingplanner.core.seating.StudentSet

interface EmptyPlan {
    val rows: Array<out EmptyRow>

    fun constructPlan(students: StudentSet): SeatingPlan {
        // sorted
        val studentSets = students.split(rows.map { it.availableSeats() })
        val populatedTables = rows.sortedByDescending { it.seatCount }.
                mapIndexed { i, row -> row.constructTableRow(studentSets[i]) }
        return SeatingPlan(populatedTables.toTypedArray())
    }

    fun assignToRow(student: Student, row: Int): Boolean {
        return if (rows[row].hasAvailableSeats()) {
            rows[row].assignedStudents.add(student)
            true
        } else false
    }
}

class SeatingPlan(val tables: Array<TableRow>) {
    fun swap(one: String, other: String) {
        // Assuming all the the names are unique
        var row = -1
        var seatInRow = -1
        for ((index, table) in tables.withIndex()) {
            fun trySwap(seat: Int, oldName: String, newName: String): Boolean {
                return if (seat == -1) {
                    false
                } else {
                    if (row == -1) {
                        row = index
                        seatInRow = seat
                        false
                    } else {
                        tables[row][seatInRow] = newName
                        tables[index][seat] = oldName
                        true
                    }
                }
            }
            val oneSeat = table.seatOf(one)
            if (trySwap(oneSeat, other, one)) {
                return
            }
            val otherSeat = table.seatOf(other)
            if (trySwap(otherSeat, one, other)) {
                return
            }
        }
    }
}
