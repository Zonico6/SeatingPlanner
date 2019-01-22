package com.zoniklalessimo.seatingplanner.core.seating

import java.io.Serializable

/**
 * Holds a number of studentMap. It's the very basis for most constructions so it either represents the group which
 * will become a table or it can be an entire collection of students. For example a school class.
 */
class StudentSet(students: Iterable<Student>) {
    // Turn the given studentNames into a map of the form 'name' to 'student'
    var studentMap: Map<String, Student> = students.map {it.name to it }.toMap()
        private set

    val students: Collection<Student>
        get() = studentMap.values

    val size get() = studentMap.size

    operator fun get(key: String) = studentMap[key]

    operator fun plus(otherStudents: Iterable<Student>) = StudentSet(studentMap.values + otherStudents)
    operator fun plus(otherStudents: StudentSet) = StudentSet(studentMap.values + otherStudents.studentMap.values)

    fun add(otherStudents: Iterable<Student>) {
        updateStudents(students + otherStudents)
    }

    /**
     * Update underlying map in the form of 'name' to 'student' to match the given Iterable.
     * This should be used with caution since every addition recreates re-assigns the underlying map!
     */
    fun updateStudents(studentsIter: Iterable<Student>) {
        studentMap = studentsIter.map {it.name to it }.toMap()
    }

    /**
     * Take the remaining groups and merge them to match the remaining sizesDescending.
     * The sizes of the groups in the end are sorted.
     *
     * @param sizesDescending The remaining sizesDescending. It's mandatory that those are sorted in descending order
     */
    private fun MutableList<MutableSet<String>>.trimGroups(sizesDescending: MutableList<Int>) {
        // TODO: Check if there are enough places to place all students
        val studentsDescending = sortedByDescending { it.size }.toMutableList()
        clear()
        while (studentsDescending.size > 0) {
            // Running out of group places, so just put the leftover junk together in the last one.
            if (sizesDescending.size == 1) {
                add(studentsDescending.flatten().toMutableSet())
                return
            }
            val group = studentsDescending.removeAt(0)
            // Begin with the biggest remaining list
            for (g in studentsDescending.toMutableList()) {
                if (group.size + g.size <= sizesDescending.first()) {
                    group.addAll(g)
                    studentsDescending.remove(g)
                }
            }
            add(group)
            sizesDescending.removeAt(0)
        }
    }

    /**
     * Split this set into several smaller sets of student names whose sizes match the provided groupSizes.
     *
     * @param groupsSizes Declares the sizes of the groups
     * @return The created groups of students, sorted
     */
    fun splitGroups(groupsSizes: Collection<Int>): Array<Set<String>> {
        val groupSizes = groupsSizes.toMutableList()
        groupSizes.sortDescending()
        // Names that have already been send to final group
        val coveredNames = mutableSetOf<String>()
        val finalGroups = mutableListOf<Set<String>>()
        val wishes = makeWishSet()
        val groups = mutableListOf<MutableSet<String>>()
        wishes@ for ((firstName, secondName) in wishes.neighbours) {
            if (coveredNames.contains(firstName) || coveredNames.contains(secondName)) {
                continue@wishes
            }
            // True: Index marks set with firstName; False: Index marks secondName; null: No set with a name found yet
            var firstN: Boolean? = null
            var index = 0

            // Look for a group containing a name
            groups@ for ((i, group) in groups.withIndex()) {
                when (firstN) {
                    null -> {
                        if (group.contains(firstName)) {
                            index = i
                            firstN = true
                            if (group.contains(secondName)) {
                                continue@wishes
                            }
                        } else if (group.contains(secondName)) {
                            index = i
                            firstN = false
                        }
                    }
                    true -> {
                        if (group.contains(secondName)) {
                            continue@wishes
                        }
                    }
                    false -> {
                        if (group.contains(firstName)) {
                            continue@wishes
                        }
                    }
                }
            }
            when (firstN) {
                null -> {
                    groups.add(mutableSetOf(firstName, secondName))
                    continue@wishes
                }
                true -> groups[index].add(secondName)
                false -> groups[index].add(firstName)
            }
            // This group may not be any bigger so send it to final groups and add it's members to covered names
            if (groups[index].size >= groupSizes[0]) {
                finalGroups.add(groups[index])
                coveredNames.addAll(groups[index])
                groups.removeAt(index)
                groupSizes.removeAt(0)
            }
        }
        groups.trimGroups(groupSizes)
        return groups.toTypedArray()
    }

    /**
     * Split this set into several smaller StudentSets names whose sizes match the provided groupSizes. This is method
     * is meant to provide an easy way of obtaining multiple StudentSets corresponding to sizes of tables within a plan.
     *
     * @param groupSizes Declares the sizes of the groups
     * @return The created groups of studentMap by names.
     */
    fun split(groupSizes: List<Int>): Array<StudentSet> {

        val groups = splitGroups(groupSizes)
        // Fill the remaining group spots with empty sets
        return (0 until groupSizes.size).map { groups.getOrElse(it) { emptySet() } }.map {
            StudentSet(it.map { name -> studentMap[name]!! })
        }.toTypedArray()
    }
}

/**
 * Describes a student by it's name and it's wishes.
 *
 * @param name The name of the student
 * @param neighbours The neighbour wishes of the Student ordered from important to unimportant.
 * @param distants The distant wishes of the Student ordered from important to unimportant.
 */
open class Student(val name: String, val neighbours: Array<String>, val distants: Array<String>) :
        Serializable {
    /**
     * Gives the priority of the given student/name. The priority is the index combined with
     * the size of the containing list, according to maskPriority. If the name isn't there, it return null.
     *
     * @param name The name of the student to search for.
     * @return The gotten priority. If the name wasn't found, this is null.
     */
    fun priorityOf(name: String): Priority? {
        val nIndex = neighbours.indexOf(name)
        val dIndex by lazy {
            distants.indexOf(name)
        }

        return when {
            nIndex != -1 -> maskPriority(nIndex, neighbours.size)
            dIndex != -1 -> -maskPriority(dIndex, distants.size)
            else -> null
        }
    }

    private fun maskPriority(index: Int, length: Int): Priority = index + length
}