package com.zoniklalessimo.seatingplanner.core.plan

import com.zoniklalessimo.seatingplanner.core.seating.Student
import com.zoniklalessimo.seatingplanner.core.seating.StudentSet
import com.zoniklalessimo.seatingplanner.core.seating.constructStraightTable

open class Table(var students: Array<String?>) {
    constructor(students: Array<String?>, seatCount: Int) :
            this(students) {
        this.seatCount = seatCount
    }
    constructor(seatCount: Int) : this(arrayOfNulls(seatCount))

    operator fun get(index: Int) = students[index]
    operator fun set(index: Int, value: String) {
        students[index] = value
    }

    var seatCount
        get() = students.size
        set(value) {
            when {
                students.size > value -> shorten(value)
                students.size < value -> inflate(value)
            }
        }
    protected fun shorten(newSize: Int) {
        val studentsList = students.reversed().toMutableList()
        while (students.size > newSize) {
            // If there are no empty seats remaining, cut the row
            if (!studentsList.remove(null)) {
                studentsList.filterIndexed {i, _ -> i < newSize }
                students = studentsList.toTypedArray()
            }
        }
        students = studentsList.toTypedArray()
    }
    protected fun inflate(newSize: Int) {
        students = Array(newSize) {students.getOrNull(it)}
    }

    val emptySeats get() = students.count {it == null}
    fun getEmptySeats() = students.mapIndexedNotNull {i, e -> e ?: i }

    fun seatOf(name: String) = students.indexOf(name)
    fun replace(first: String, second: String): Boolean {
        val firstIndex = students.indexOf(first)
        if (firstIndex != -1) {
            students[firstIndex] = second
            return true
        }
        val secondIndex = students.indexOf(second)
        if (secondIndex != -1) {
            students[secondIndex] = first
            return true
        }
        return false
    }
    fun swap(first: String, second: String) {
        val firstIndex = students.indexOf(first)
        val secondIndex = students.indexOf(second)
        if (firstIndex != -1 && secondIndex != -1) {
            students[firstIndex] = second
            students[secondIndex] = first
        }
    }
}

/**
 * One row with separators. Capable of yielding the single, separated tables.
 */
open class TableRow(val tables: IntArray, students: Array<String?>) : Table(students) {

    constructor(tables: Array<Table>) :
            this(tables.map { it.seatCount }.toIntArray(),
                    tables.fold(arrayOf()) { a, e -> a + e.students})

    fun getSingleTables(): List<Table> {
        val plTables = mutableListOf<Table>()
        var currentIndex = 0
        for (tableSize in tables) {
            val newStudents = students.sliceArray(currentIndex..currentIndex + tableSize)
            currentIndex += tableSize
            plTables += Table(newStudents)
        }
        return plTables
    }
    // TODO: When shortening or inflating, tables don't get updated. One should take strip or add the tableSizes in the end of the array.
}
// TODO: Add round tables

open class EmptyRow(val tables: IntArray, val assignedStudents: MutableList<Student>) {
    constructor(tables: IntArray) : this(tables, mutableListOf())

    val seatCount get() = tables.sum()
    fun hasAvailableSeats() = assignedStudents.size < seatCount
    fun availableSeats() = seatCount - assignedStudents.size

    fun constructTableRow(students: StudentSet): TableRow {
        val row = constructStraightTable(students + assignedStudents)
        @Suppress("UNCHECKED_CAST")
        return TableRow(tables, row as Array<String?>)
    }
}