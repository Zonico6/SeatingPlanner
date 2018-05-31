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

            val wish = Wish(student.name, otherStudent.name,
                    /* It's kinda unnecessary to call 'priorityOf' on the current student, since we actually
                       have this information already and could technically call maskPriority directly.
                       However we would need to keep track of the studentMap index.*/
                    (student.priorityOf(partner) ?: 0) + (otherStudent.priorityOf(student.name) ?: 0))
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
    return WishSet(neighbourWishes.toList().sorted(), distantWishes.toList().sortedDescending())
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