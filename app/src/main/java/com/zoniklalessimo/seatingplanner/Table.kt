package com.zoniklalessimo.seatingplanner

import java.util.*

interface Table {
    var seatCount: Int
    var separators: SortedSet<Int>

    fun insertTable(at: Int, other: Table) {
        val oldSeats = seatCount
        seatCount += other.seatCount
        // Shift all separators so they are still correct after the insertion
        separators = (separators.mapIndexed { index, e -> if (index >= at) e + other.seatCount else e } +
                other.separators.map { it + oldSeats }).toSortedSet()
        separators.add(oldSeats)
    }
}

fun Table.append(other: Table) {
    val oldSeats = seatCount
    seatCount += other.seatCount
    separators.add(oldSeats)
    separators.addAll(other.separators.map { it + oldSeats })
}

fun Table.insertAtStart(other: Table) {
    seatCount += other.seatCount
    separators = (other.separators + other.seatCount + separators.map { it + other.seatCount }).toSortedSet()
}

fun Table.separatorsTillSeat(seat: Int) = separators.filter { it < seat }.toSortedSet()
fun Table.separatorsFromSeat(seat: Int) = separators.mapNotNull {if (it - seat <= 0) null else it - seat}.toSortedSet()

fun Table.separatorString(concatenate: String): String {
    val builder = StringBuilder((separators.size - 1) * concatenate.length + 1)
    for (i in separators) {
        builder.append(concatenate).append(i)
    }
    builder.deleteCharAt(0)
    return builder.toString()
}
fun Table.separatorString(concatenate: Char = ','): String {
    if (separators.isEmpty()) return String()

    val builder = StringBuilder(separators.size * 2 - 1)
    for (i in separators) {
        builder.append(concatenate).append(i)
    }

    builder.deleteCharAt(0)
    return builder.toString()
}

fun Table.assignSeparators(sepsStr: CharSequence) {
    var currentNumber = StringBuilder(2)
    for (i in sepsStr) {
        if (i.isDigit()) {
            currentNumber.append(i)
        } else if (currentNumber.isNotEmpty()) {
            separators.add(currentNumber.toString().toInt())
            currentNumber = StringBuilder(2)
        }
    }
}

fun Table.addSeparator(pos: Int): Boolean = separators.add(pos)
fun Table.clearSeparators() = separators.clear()

fun Table.separatorBeforeSeat(seat: Int): Int {
    var carry = 0
    for (e in separators) {
        if (e > seat)
            return carry
        carry = e
    }
    return carry
}
fun Table.separatorAfterSeat(seat: Int): Int {
    for (e in separators) {
        if (e > seat)
            return e
    }
    return seatCount
}

fun Table.sectionAround(seat: Int): Int {
    var carry = 0
    for (e in separators) {
        if (e > seat) {
            return e - carry
        }
        carry = e
    }
    return seatCount - carry
}

fun Table.separatorSpliceAt(seat: Int): Array<SortedSet<Int>> {
    val firstSeps: SortedSet<Int> = sortedSetOf()
    val newSeps = sortedSetOf<Int>()
    var first = true
    for (i in separators) {
        if (i > seat && first) {
            firstSeps.addAll(newSeps)
            newSeps.clear()
            first = false
        } else {
            newSeps.add(i)
        }
    }
    return arrayOf(firstSeps, newSeps)
}
