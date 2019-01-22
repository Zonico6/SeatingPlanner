package com.zoniklalessimo.seatingplanner.core.seating

typealias Priority = Int
typealias WishMap = MutableMap<Pair<String, String>, Priority>

/**
 * Holds unique wishes alongside a priority.
 *
 * @property neighbours Holds the wish for neighbours.
 * @property distants Holds the wish for distants.
 */
data class WishSet(val neighbours: List<Wish>, val distants: List<Wish>)
/**
 * Combine the wishes of all the studentMap of the [StudentSet] to one [WishSet].
 *
 * @return The constructed [WishSet] that yields all wishes uniquely, as pairs of names alongside a priority and
 * ordered by this priority.
 */
fun StudentSet.makeWishSet(): WishSet {
    val maxNeighbourLength = students.map {
        it.neighbours.size
    }.max() ?: 0

    val maxDistantLength = students.map {
        it.distants.size
    }.max() ?: 0

    val covered = mutableMapOf<String, MutableSet<String>>()
    val neighbourWishes = mutableListOf<Wish>()
    val distantWishes = mutableListOf<Wish>()

    // Iterate over all studentMap and fuse their wishes with their partners to form a 'WishSet'
    for (student in this.studentMap.values) {
        val coveredPartners = mutableSetOf<String>()
        fun makeWish(partner: String): Boolean {
            if (covered[partner]?.contains(student.name) == true)
                return true
            val otherStudent = studentMap[partner] ?: return false

            val cmpFirstPrior = student.priorityOf(partner) ?: 0
            val cmpSecondPrior = student.priorityOf(student.name) ?: 0

            val cmpPriorSum = cmpFirstPrior + cmpSecondPrior

            val priority = if ((cmpFirstPrior < 0 && cmpSecondPrior > 0)
                    || (cmpFirstPrior > 0 && cmpSecondPrior < 0)) {
                if (cmpPriorSum >= 0) {
                    -maxDistantLength - cmpPriorSum
                } else {
                    maxNeighbourLength + cmpPriorSum
                }
            } else {
                if (cmpFirstPrior < 0) {
                    -maxDistantLength * 2 - cmpPriorSum
                } else {
                    maxNeighbourLength * 2 - cmpPriorSum
                }
            }

            val wish = Wish(student.name, otherStudent.name, priority)
            when {
                wish.priority > 0 -> neighbourWishes.add(wish)
                wish.priority < 0 -> distantWishes.add(wish)
            }
            return true
        }
        for (neighbour in student.neighbours) {
            coveredPartners.add(neighbour)
            if (!makeWish(neighbour))
                println("The student " + student.name + " named some " + neighbour + " as his potential neighbour. " +
                        "However, this student is not registered and thus may not be named by others.")
        }
        for (distant in student.distants) {
            coveredPartners.add(distant)
            if (!makeWish(distant)) {
                println("The student " + student.name + " gave some " + distant + " as his potential distant. " +
                        "However, this student does not occur in the list of studentMap and thus may not be named by others.")
            }
        }
        covered[student.name] = coveredPartners
    }
    return WishSet(neighbourWishes.toList().sortedDescending(), distantWishes.toList().sortedDescending())
}

fun List<Wish>.toMap() = map {it.names to it.priority}.toMap()

/**
 * Represents a wish of two names alongside an associated priority.
 *
 * @property firstName The name of the first mentioned studentMap.
 * @property secondName The name of the second mentioned student.
 * @property priority The priority of the wish. This should never be negative!
 * You should mark distant-wishes outside of the wish-object instead.
 */
data class Wish(
        val firstName: String,
        val secondName: String,
        val priority: Priority
): Comparable<Wish> {
    val names get() = Pair(firstName, secondName)
    override fun compareTo(other: Wish) = this.priority.compareTo(other.priority)
}
// Overload put method
fun WishMap.put(wish: Wish) = put(wish.names, wish.priority)