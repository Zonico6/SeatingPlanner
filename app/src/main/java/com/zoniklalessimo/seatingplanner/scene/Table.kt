package com.zoniklalessimo.seatingplanner.scene

import java.util.*

interface Table {
    var seatCount: Int
    var separators: SortedSet<Int>

    fun insertTable(at: Int, other: Table) {
        val oldSeats = seatCount
        seatCount += other.seatCount
        // Shift all separators so they are still correct after the insertion
        separators = (separators.mapIndexed { index, e -> if (index > at) e + other.seatCount else e } +
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

fun Table.separatorsToSeat(seat: Int): SortedSet<Int> {
    val ret = sortedSetOf<Int>()
    for (i in separators) {
        if (i > seat)
            break
        ret.add(i)
    }
    return ret
}

fun Table.separatorsFromSeat(seat: Int): SortedSet<Int> {
    var carry = -1
    val ret = sortedSetOf<Int>()
    for (i in separators) {
        if (carry != -1) {
            ret.add(i - carry)
            continue
        }
        if (i > seat) {
            carry = i
        }
    }
    return ret
}

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

fun Table.closestSeparator(seat: Int): Int {
    val before = separatorBeforeSeat(seat)
    val after = separatorAfterSeat(seat)
    return if (seat - before < after - seat)
        before
    else
        after
}

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

/**
 * Get the separators enclosing the section around this seat.
 * I.e. Get the indices of the separator before and after this seat.
 */
fun Table.sectionAround(seat: Int): Pair<Int, Int> {
    var carry = 0
    for (e in separators) {
        if (e > seat) {
            return Pair(carry, e)
        }
        carry = e
    }
    return Pair(carry, seatCount)
}

/**
 * Get the number of seats between the separator before the given seat and after it.
 */
fun Table.sectionLength(seat: Int): Int {
    var carry = 0
    for (e in separators) {
        if (e > seat) {
            return e - carry
        }
        carry = e
    }
    return seatCount - carry
}

/**
 * Get two sets of separators
 *
 * @param seat The seat, which is being split at
 *
 * @return Two sets of separators, split at the first separator before the seat
 */
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
