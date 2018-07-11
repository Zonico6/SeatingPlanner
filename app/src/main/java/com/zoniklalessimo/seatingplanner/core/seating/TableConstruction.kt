package com.zoniklalessimo.seatingplanner.core.seating

typealias MutableRow = MutableList<String>
typealias Row = List<String>

/**
 * Construct a table based on the given [StudentSet]
 *
 * @param students The [StudentSet], that the wishes and students get drawn from.
 */
fun constructStraightTable(students: StudentSet) =
        constructStraightTable(students.makeWishSet())

/**
 * Construct a table based on the given [WishSet]
 *
 * @param wishes The [WishSet], that the wishes and students get drawn from. It's important that these wishes are sorted
 * by priority in descendant order since the priority itself is ignored.
 * @return The constructed table with as many seats as there are students.
 */
fun constructStraightTable(wishes: WishSet): Array<String> {
    val parts = constructParts(wishes)
    return join(parts, wishes).toTypedArray()
}

private fun constructParts(wishes: WishSet): MutableList<MutableRow> {
    val parts = mutableListOf<MutableRow>()
    for (nw in wishes.neighbours) {
        parts.addWish(nw)
    }
    return parts
}

fun constructRoundTable(students: StudentSet) =
    constructRoundTable(students.makeWishSet())

fun constructRoundTable(wishes: WishSet): Array<String> {
    val parts = constructParts(wishes)
    val row = joinRoundTable(parts, wishes)
    return row.toTypedArray()
}

private fun joinRoundTable(rows: MutableList<MutableRow>, wishes: WishSet): Row {
    val distants = wishes.distants.toMap()
    // Currently it starts at the first entry of the list whereas it would actually be better to start at the row that
    // hates another the most and sit them to the beginning/end
    val retList = rows.removeAt(0)
    var lastRow: Row = retList

    while (rows.size > 2) {
        val nextRow = rows.takeNextDistantRow(lastRow, distants as WishMap)
        retList.addAll(nextRow)
        lastRow = nextRow
    }
    if (rows.size >= 2)
        return closeRoundTable(rows.first(), rows.last(), distants as WishMap)
    return retList
}
private fun closeRoundTable(firstRow: MutableRow, secondRow: MutableRow, distants: WishMap): Row {
    val (_, reversed) = roundDistantScore(firstRow, secondRow, distants)
    return firstRow + if (reversed) secondRow.reversed() else secondRow
}

private fun roundDistantScoreRigid(one: List<String>, other: List<String>, distants: WishMap): Int {
    // Account for contact on both ends by adding two distantScores, one as normal, the other 'from the other side'
    // i.e. with both lists revered and then normalizing the score by dividing by 2
    return (distantScoreRigid(one.reversed(), other.reversed(), distants) +
            distantScoreRigid(one, other, distants)) / 2
}

private fun roundDistantScore(rigid: MutableRow, other: List<String>, distants: WishMap): DistantScoreReturn {
    val score = roundDistantScoreRigid(rigid, other, distants)
    val reversedScore = roundDistantScoreRigid(rigid, other.reversed(), distants)
    return if (score > reversedScore) {
        DistantScoreReturn(score, false)
    } else {
        DistantScoreReturn(reversedScore, true)
    }
}

/**
 * Adds a wish to the the list of rows of students: Seats one student of the wish next to the other, skips it if
 * one name is already surrounded by neighbours or creates a new row with the wish's names.
 *
 * @receiver Represents the set of rows of students.
 * @param wish The wish that holds the names. The priority is ignored.
 * @return True if a row was created or carried.
 */
private fun MutableList<MutableRow>.addWish(wish: Wish): Boolean {
    var indexCarry: RowIndexCarry? = null
    var newName: String? = null
    for ((index, row) in this.withIndex()) {
        // One name is already in use so the for loop is redundant. Just check if newName is in another row as well.
        if (newName != null) {
            if (row.contains(newName))
                return false
            else
                continue
        }
        fun carryOrConnect(tail: ListTail) =
                if (indexCarry == null) {
                    indexCarry = RowIndexCarry(index, tail)
                    false
                } else {
                    this[indexCarry!!.index].connectRow(row, indexCarry!!.tail, tail)
                    true
                }

        val firstLocation = row.location(wish.firstName)
        val secondLocation = row.location(wish.secondName)
        // If both names are used: Skip, return false
        // If one is used: Let the other be the root of a new list
        if (firstLocation == ListLocation.MIDDLE) {
            if (secondLocation != ListLocation.NULL)
                return false
            newName = wish.secondName
        } else if (secondLocation == ListLocation.MIDDLE) {
            if (firstLocation != ListLocation.NULL)
                return false
            newName = wish.firstName
        } else if (firstLocation == ListLocation.NULL && secondLocation == ListLocation.NULL) {
            continue
            // Doesn't matter which is at the start, since it's gotta be the other in future iteration due to no doubles.
        } else if (firstLocation == ListLocation.START || secondLocation == ListLocation.START) {
            // Continue if carried, return if covered
            if (carryOrConnect(ListTail.START)) {
                removeAt(index)
                return true
            } else continue
        } else if (firstLocation == ListLocation.END || secondLocation == ListLocation.END) {
            if (carryOrConnect(ListTail.END)) {
                removeAt(index)
                return true
            } else continue
        }
    }
    if (newName != null) {
        add(mutableListOf(newName))
        return true
    }
    // If indexCarry is falls, do 'let' block, else the 'elvis' block
    indexCarry?.let {
        if (it.tail == ListTail.START) {
            if (this[it.index].first() == wish.firstName) {
                this[it.index].add(0, wish.secondName)
            } else {
                this[it.index].add(0, wish.firstName)
            }
        } else {
            if (this[it.index].last() == wish.firstName) {
                this[it.index].add(wish.secondName)
            } else {
                this[it.index].add(wish.firstName)
            }
        }
    } ?: add(mutableListOf(wish.firstName, wish.secondName))
    return true
}

private class RowIndexCarry(index: Int, var tail: ListTail) {
    constructor(index: Int) : this(
            kotlin.math.abs(index),
            if (index < 0) ListTail.END else ListTail.START)
    var index: Int = index
        // If value is negative, tail gets inverted.
        set(value) {
            if (value < 0) {
                field = -value
                tail = !tail
            } else
                field = value
        }

    operator fun component1() = index
    operator fun component2() = tail
    /* legacy code:

    var index
        get() = if (_index == null) 0 else kotlin.math.abs(_index!!)
        set(index) {
            _index = if (tail == ListTail.END) {
                if (index == 0) {
                    null
                } else {
                    -index
                }
            } else {
                index
            }
        }
    var tail
        get() = if (_index == null || _index!! < 0) ListTail.END else ListTail.START
        set(tail) {
            _index = if (tail == ListTail.END) {
                when (_index) {
                    0, null -> null
                    else -> -kotlin.math.abs(_index!!)
                }
            } else {
                when (_index) {
                    null -> 0
                    else -> kotlin.math.abs(_index!!)
                }
            }
        }*/
}

// The higher the score the worst they fit together.
private data class DistantScoreReturn(val score: Int, val reversed: Boolean)

private fun distantScoreRigid(one: List<String>, other: List<String>, distants: WishMap): Int {
    var score = 0f
    for ((firstIndex, first) in one.withIndex()) {
        for ((secIndex, second) in other.withIndex()) {
            score += (distants.getOrDefault(Pair(first, second), 0)) / (one.size - firstIndex + secIndex)
        }
    }
    return score.toInt()
}

private fun distantScore(rigid: MutableRow, other: List<String>, distants: WishMap): DistantScoreReturn {
    val score = distantScoreRigid(rigid, other, distants)
    val reversedScore = distantScoreRigid(rigid, other.reversed(), distants)
    return if (score > reversedScore) {
        DistantScoreReturn(score, false)
    } else {
        DistantScoreReturn(reversedScore, true)
    }
}

private fun MutableList<MutableRow>.takeNextDistantRow(otherRow: Row, distants: WishMap): Row {
    var nextRow = listOf<String>()
    var score: Int = Int.MAX_VALUE
    var reversed = false

    for (row in this) {
        if (row == otherRow)
            continue
        else {
            val (distantScore, rev) = distantScore(row, otherRow, distants)
            if (distantScore < score) {
                nextRow = row
                reversed = rev
                score = distantScore
            }
        }
    }
    this.remove(nextRow)
    return if (reversed) nextRow.reversed() else nextRow
}

/**
 * Takes the rows and joins them based on the students distant-wishes so that the students that don't want to sit next
 * to each other sit as much apart as possible.
 *
 * @param rows The rows that shall be joined.
 * @param wishes The [WishSet] with the distant wishes.
 * @return The joined, single row of students.
 */
private fun join(rows: MutableList<MutableRow>, wishes: WishSet): List<String> {
    val distants = wishes.distants.toMap()
    // Currently it starts at the first entry of the list whereas it would actually be better to start at the row that
    // hates another the most and sit them to the beginning/end
    val retList = rows.removeAt(0)
    var lastRow: List<String> = retList

    while (rows.size > 1) {
        val nextRow = rows.takeNextDistantRow(lastRow, distants as WishMap)
        retList.addAll(nextRow)
        lastRow = nextRow
    }
    if (rows.size >= 1)
        retList.addAll(rows.first())
    return retList
}

private enum class ListTail {
    START,
    END;
    operator fun not() = if (this == START) END else START
}

private fun MutableRow.connectRow(other: MutableRow, thisTail: ListTail, otherTail: ListTail) {
    if (thisTail == ListTail.START) {
        if (otherTail == ListTail.START) {
            addAll(0, other.reversed())
        } else {
            addAll(0, other)
        }
    } else {
        if (otherTail == ListTail.START) {
            addAll(other)
        } else {
            addAll(other.reversed())
        }
    }
}

private enum class ListLocation {
    START,
    MIDDLE,
    END,
    NULL,
}

private fun List<String>.location(name: String): ListLocation {
    val index = indexOf(name)
    return when (index) {
        -1 -> ListLocation.NULL
        size - 1 -> ListLocation.END
        0 -> ListLocation.START
        else -> ListLocation.MIDDLE
    }
}