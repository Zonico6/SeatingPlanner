package com.zoniklalessimo.seatingplanner.studentSet

import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.zoniklalessimo.seatingplanner.core.seating.Student
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException

class StudentSetViewModel : ViewModel() {
    private val students = mutableListOf<MutableLiveData<OpenableStudent>>()
    private fun requireStudent(index: Int): OpenableStudent =
        students[index].requireStudent()
    fun getStudents() = students.map { it.requireStudent() }

    val studentCount get() = students.size

    private fun LiveData<OpenableStudent>.requireStudent(): OpenableStudent =
            this.value
                ?: throw IllegalStateException("Null value among students.")

    fun getStudent(index: Int) = requireStudent(index)

    fun getStudentObserver(adapter: RecyclerView.Adapter<*>, index: Int) = Observer<OpenableStudent> {
            adapter.notifyItemInserted(index)
        }

    fun bindWithAdapter(owner: LifecycleOwner, adapter: RecyclerView.Adapter<*>) {
        students.forEachIndexed { index, student ->
            student.observe(owner, getStudentObserver(adapter, index))
        }
    }

    fun addStudent(owner: LifecycleOwner, adapter: RecyclerView.Adapter<*>,
                   student: OpenableStudent = OpenableStudent("", emptyArray(), emptyArray())) {

        val liveData = MutableLiveData<OpenableStudent>()

        liveData.observe(owner, getStudentObserver(adapter, students.size))

        liveData.value = student
        students.add(liveData)
    }

    fun renameStudent(newName: String, index: Int) {
        val oldStudent = requireStudent(index)
         // Rename
        students[index].value = OpenableStudent(newName, oldStudent.neighbours, oldStudent.distants)

        // Update name in all other students' wishes
        students.forEach {
            val student = it.requireStudent()

            val iNeighbour = student.neighbours.indexOf(oldStudent.name)
            val iDistant = student.distants.indexOf(oldStudent.name)

            if (iNeighbour != -1)
                student.neighbours[iNeighbour] = newName
            if (iDistant != -1)
                student.distants[iDistant] = newName

            it.value = student
        }
    }




    fun close(index: Int) {
        students[index].value = requireStudent(index).asClosed()
    }

    fun openNeighbours(index: Int) {
        students[index].value = requireStudent(index).withNeighboursOpen()
    }
    fun openDistants(index: Int) {
        students[index].value = requireStudent(index).withDistantsOpen()
    }

    fun appendNeighbour(index: Int, newNeighbour: String, openNeighbours: Boolean) {
        students[index].value = requireStudent(index).withAppendNeighbour(newNeighbour, openNeighbours)
    }

    fun appendDistant(index: Int, newDistant: String, openNeighbours: Boolean) {
        students[index].value = requireStudent(index).withAppendDistant(newDistant, openNeighbours)
    }

    fun getOpenedWishes(index: Int): Array<String>? =
            requireStudent(index).openedWishes

    fun isClosed(index: Int): Boolean =
            requireStudent(index).isClosed
    fun isOpened(index: Int): Boolean =
            requireStudent(index).isOpened
    fun hasNeighboursOpened(index: Int) =
            requireStudent(index).areNeighboursOpened
    fun hasDistantsOpened(index: Int) =
            requireStudent(index).areDistantsOpened
}

class OpenableStudent(name: String, neighbours: Array<String>, distants: Array<String>,
              private val opened: Int = CLOSED) : Student(name, neighbours, distants) {
    companion object {
        const val CLOSED = 0x0
        const val NEIGHBOURS_OPENED = 0x1
        const val DISTANTS_OPENED = 0x2
    }

    val isOpened get() = opened != CLOSED
    val isClosed get() = !isOpened

    val areNeighboursOpened get() = opened == NEIGHBOURS_OPENED
    val areDistantsOpened get() = opened == DISTANTS_OPENED

    val openedWishes: Array<String>?
        get() = when (opened) {
            CLOSED -> null
            NEIGHBOURS_OPENED -> neighbours
            DISTANTS_OPENED -> distants
            else -> throw IllegalStateException("Unknown value for opened.")
        }

    fun withName(name: String) =
            OpenableStudent(name, neighbours, distants, opened)

    fun withOpened(opened: Int) =
            OpenableStudent(name, neighbours, distants, opened)

    /**
     * Returns the student with the neighbours replaced with the supplied ones.
     *
     * @param neighbours The new neighbours.
     * @param openNeighbours Set the opened variable to have opened neighbours within the new student.
     */
    fun withNeighbours(neighbours: Array<String>, openNeighbours: Boolean = false) =
            OpenableStudent(name, neighbours, distants, if (openNeighbours) NEIGHBOURS_OPENED else CLOSED)

    /**
     * Returns the student with the distants replaced with the supplied ones.
     *
     * @param distants The new distants.
     * @param openDistants Set the opened variable to have opened distants within the new student.
     */
    fun withDistants(distants: Array<String>, openDistants: Boolean = false) =
            OpenableStudent(name, neighbours, distants, if (openDistants) DISTANTS_OPENED else CLOSED)

    fun withOpenedWishes(newWishes: Array<String>): OpenableStudent =
            when (opened) {
                OpenableStudent.NEIGHBOURS_OPENED -> withNeighbours(newWishes)
                OpenableStudent.DISTANTS_OPENED -> withDistants(newWishes)
                OpenableStudent.CLOSED -> throw UnsupportedOperationException("Setting open wishes while wishes were closed.")
                else -> throw IllegalStateException("Unknown value for opened.")
            }
}

fun OpenableStudent.withNeighboursOpen(): OpenableStudent =
        withOpened(OpenableStudent.NEIGHBOURS_OPENED)
fun OpenableStudent.withDistantsOpen() =
        withOpened(OpenableStudent.DISTANTS_OPENED)

fun OpenableStudent.asClosed(): OpenableStudent =
        OpenableStudent(name, neighbours, distants, OpenableStudent.CLOSED)

fun OpenableStudent.withAppendNeighbour(neighbour: String, openNeighbours: Boolean = false) =
        withNewNeighbour(neighbour, neighbours.size, openNeighbours)
fun OpenableStudent.withNewNeighbour(neighbour: String, index: Int, openNeighbours: Boolean = false): OpenableStudent {
    val newNeighbours = neighbours.toMutableList()
    newNeighbours.add(index, neighbour)
    return withNeighbours(newNeighbours.toTypedArray(), openNeighbours)
}

fun OpenableStudent.withAppendDistant(distant: String, openDistants: Boolean = false) =
        withNewDistant(distant, distants.size, openDistants)
fun OpenableStudent.withNewDistant(distant: String, index: Int, openDistants: Boolean = false): OpenableStudent {
    val newDistants = distants.toMutableList()
    newDistants.add(index, distant)
    return withDistants(newDistants.toTypedArray(), openDistants)
}