package com.zoniklalessimo.seatingplanner.core.seating

/**
 * Holds a number of studentMap. It's the very basis for most constructions so it either represents the group which
 * will become a table or it can be an entire collection of students. For example a school class.
 */
open class StudentSet(students: Iterable<Student>) {
    // Turn the given studentNames into a map of the form 'name' to 'student'
    var studentMap: Map<String, Student> = students.map {it.name to it }.toMap()
        private set

    fun getStudents(): Collection<Student> = studentMap.values

    val size get() = studentMap.size

    operator fun get(key: String) = studentMap[key]

    operator fun plus(otherStudents: Iterable<Student>) = StudentSet(studentMap.values + otherStudents)
    operator fun plus(otherStudents: StudentSet) = StudentSet(studentMap.values + otherStudents.studentMap.values)

    fun add(otherStudents: Iterable<Student>) {
        updateStudents(getStudents() + otherStudents)
    }

    /**
     * Update underlying map in the form of 'name' to 'student' to match the given Iterable.
     * This should be used with caution since every addition recreates re-assigns the underlying map!
     */
    fun updateStudents(studentsIter: Iterable<Student>) {
        studentMap = studentsIter.map {it.name to it }.toMap()
    }

    /**
     * Take the remaining groups and somehow merge them to match the remaining groupSizes.
     * The sizes of the groups in the end are sorted.
     *
     * @param groupSizes The remaining groupSizes. It's mandatory that those are sorted
     */
    private fun MutableList<MutableSet<String>>.trimGroups(groupSizes: MutableList<Int>) {
        val sortedGroups = sortedBy { it.size }.toMutableList()
        clear()
        while (sortedGroups.size > 0) {
            // Running out of group places, so just put the leftover junk together in the last one.
            if (groupSizes.size == 1) {
                addAll(sortedGroups)
                return
            }
            val group = sortedGroups.removeAt(0)
            for (g in sortedGroups) {
                if (group.size + g.size <= groupSizes.first()) {
                    group.addAll(g)
                    sortedGroups.remove(g)
                    add(group)
                    groupSizes.removeAt(0)
                    break
                }
            }
            add(group)
            groupSizes.removeAt(0)
        }
    }

    /**
     * Split this set into several smaller sets of student names whose sizes match the provided groupSizes.
     *
     * @param groupSizes Declares the sizes of the groups
     * @return The created groups of students, sorted
     */
    private fun splitGroups(groupSizes: MutableList<Int>): Array<Set<String>> {
        groupSizes.sort()
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
            // Groups is gonna grow with time as new names are added after this loop
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
        val groups = splitGroups(groupSizes.toMutableList())
        val retGroups = arrayOfNulls<StudentSet>(groups.size)
        for ((index, group) in groups.withIndex()) {
            retGroups[index] = StudentSet(group.map { studentMap[it]!! })
        }
        @Suppress("UNCHECKED_CAST")
        return retGroups as Array<StudentSet>
    }
}

/**
 * Describes a student by it's name and it's wishes.
 *
 * @param name The name of the student
 * @param neighbours The neighbour wishes of the Student ordered from important to unimportant.
 * @param distants The distant wishes of the Student ordered from important to unimportant.
 */
class Student(val name: String, val neighbours: Array<String>, val distants: Array<String>) {
    /**
     * Gives the priority of the given student/name. The priority is the index combined with
     * the size of the containing list, according to maskPriority. If the name isn't there, it return null.
     *
     * @param name The name of the student to search for.
     * @return The gotten priority. If the name wasn't found, this is null.
     */
    fun priorityOf(name: String): Priority? {
        var index = neighbours.indexOf(name)
        return if (index != -1) {
            maskPriority(index, neighbours.size)
        } else {
            index = distants.indexOf(name)
            if (index != -1) -maskPriority(index, distants.size) else null
        }
    }
    private fun maskPriority(index: Int, length: Int): Priority = index + length
}