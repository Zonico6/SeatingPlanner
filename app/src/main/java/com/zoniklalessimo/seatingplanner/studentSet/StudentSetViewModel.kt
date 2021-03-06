package com.zoniklalessimo.seatingplanner.studentSet

import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.google.flatbuffers.FlatBufferBuilder
import com.zoniklalessimo.seatingplanner.core.seating.Student
import com.zoniklalessimo.seatingplanner.schema.StudentSet
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class StudentSetViewModel : ViewModel() {
    private var src = MutableLiveData<File>()
    fun getSrc() = src as LiveData<File>
    /**
     * Sets src to value only if it wasn't set before
     * @return True if new value was set, False otherwise
     */
    fun initSrc(value: File): Boolean {
        if (src.value == null) {
            src.value = value
            return true
        }
        return false
    }

    private var name = MutableLiveData<String>()
    fun getName() = name as LiveData<String>
    /**
     * Sets name to value only if it wasn't set before
     * @return True if new value was set, False otherwise
     */
    fun initName(value: String): Boolean {
        if (name.value == null) {
            name.value = value
            return true
        }
        return false
    }

    private val students = mutableListOf<MutableLiveData<OpenableStudent>>()
    /**
     * Sets students to value only if it wasn't set before
     * @return True if new value was set, False otherwise
     */
    fun initStudents(value: Array<Student>): Boolean =
        if (students.isEmpty()) {
            students.addAll(value.map { student ->
                val liveData = MutableLiveData<OpenableStudent>()

                liveData.value = OpenableStudent(student, OpenableStudent.CLOSED)

                liveData
            })
            true
        } else
            false

    private fun requireStudent(index: Int): OpenableStudent = students[index].requireStudent()
    fun getStudents() = students.map { it.requireStudent() }

    val studentCount get() = students.size

    private fun LiveData<OpenableStudent>.requireStudent(): OpenableStudent =
            this.value
                ?: throw IllegalStateException("Null value among students.")

    fun getStudent(index: Int) = requireStudent(index)
    fun setStudent(index: Int, value: OpenableStudent) {
        students[index].value = value
    }
    fun postStudent(index: Int, value: OpenableStudent) {
        students[index].postValue(value)
    }

    fun getNames() = students.map {
        it.requireStudent().name
    }

    fun save() {
        val builder = FlatBufferBuilder()

        val bSetName = builder.createString(name.value)
        val bSetSrc = builder.createString(src.value?.absolutePath)

        val students = getStudents().map { student ->
            val neighbours = student.neighbours.map(builder::createString)
            val distants = student.distants.map(builder::createString)

            val bNeighbours = com.zoniklalessimo.seatingplanner.schema.Student.createNeighboursVector(builder, neighbours.toIntArray())
            val bDistants = com.zoniklalessimo.seatingplanner.schema.Student.createDistantsVector(builder, distants.toIntArray())
            val bName = builder.createString(student.name)

            com.zoniklalessimo.seatingplanner.schema.Student.createStudent(builder, bName, bNeighbours, bDistants)
        }
        val bStudents = StudentSet.createStudentsVector(builder, students.toIntArray())

        val set = StudentSet.createStudentSet(builder, bSetSrc, bSetName, bStudents)

        builder.finish(set)

        val bytes = builder.sizedByteArray()

        FileOutputStream(src.value).use {
            it.write(bytes)
        }
    }

    fun getStudentObserver(adapter: RecyclerView.Adapter<*>, index: Int) = Observer<OpenableStudent> {
            adapter.notifyItemChanged(index)
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

        adapter.notifyItemInserted(students.size)
    }

    fun removeStudent(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>, index: Int) {
        students.removeAt(index)
        adapter.notifyItemRemoved(index)
    }

    fun indexOf(studentName: String): Int {
        for ((index, other) in getNames().withIndex()) {
            if (other == studentName) {
                return index
            }
        }
        return -1
    }

    fun renameStudent(index: Int, newName: String) {
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
    fun openWishes(index: Int, type: Int) {
        when (type) {
            OpenableStudent.NEIGHBOURS_OPENED -> openNeighbours(index)
            OpenableStudent.DISTANTS_OPENED -> openDistants(index)
            OpenableStudent.CLOSED -> close(index)
        }
    }

    fun appendNeighbour(index: Int, newNeighbour: String, openNeighbours: Boolean) {
        students[index].value = requireStudent(index).withAppendNeighbour(newNeighbour, openNeighbours)
    }

    fun appendDistant(index: Int, newDistant: String, openNeighbours: Boolean) {
        students[index].value = requireStudent(index).withAppendDistant(newDistant, openNeighbours)
    }

    fun removeNeighbour(index: Int, wish: String) {
        students[index].value = requireStudent(index).withRemoveNeighbour(wish)
    }
    fun removeDistant(index: Int, wish: String) {
        students[index].value = requireStudent(index). withRemoveDistant(wish)
    }
    fun removeOpen(index: Int, wish: String) {
        students[index].value = requireStudent(index).withRemoveOpen(wish)
    }

    /**
     * Swaps a wish from neighbours to distants or vise verca.
     *
     * @return True if wish was swapped, false otherwise. For example when the wish did not appear
     * among all wishes.
     *
     * @param openOtherWishes Open the wishes that the wish got swapped to
     */
    fun swapWish(index: Int, wishName: String, openOtherWishes: Boolean = false): Boolean {
        // Mirrors the OpenableStudent implementation of swapWish to a big extend so it can determine
        // If the wish was swapped and what to return
        val student = requireStudent(index)
        val neighbourI = student.neighbours.indexOf(wishName)
        val distantI = student.distants.indexOf(wishName)

        val newNeighbours: Array<String>
        val newDistants: Array<String>
        val open: Int

        if (neighbourI == distantI) // Both -1, i.e. not among the wishes
            return false
        else if (neighbourI != -1) {
            newNeighbours = student.neighbours.filter { it != wishName }.toTypedArray()

            newDistants = student.distants.toMutableList().apply {
                add(min(neighbourI, student.distants.size), wishName)
            }.toTypedArray()

            open = OpenableStudent.DISTANTS_OPENED
        } else {
            newDistants = student.distants.filter { it != wishName }.toTypedArray()

            newNeighbours = student.neighbours.toMutableList().apply {
                add(min(distantI, student.neighbours.size), wishName)
            }.toTypedArray()

            open = OpenableStudent.NEIGHBOURS_OPENED
        }

        students[index].value = OpenableStudent(student.name, newNeighbours, newDistants,
                if (openOtherWishes) open else student.opened)
        return true
    }

    fun moveWish(index: Int, oldWishIndex: Int, newWishIndex: Int) {
        students[index].value = requireStudent(index).withMoveOpenWish(oldWishIndex, newWishIndex)
    }

    fun getOpenedWishes(index: Int): Array<String>? =
            requireStudent(index).openWishes

    fun isClosed(index: Int): Boolean =
            requireStudent(index).isClosed
    fun isOpened(index: Int): Boolean =
            requireStudent(index).isOpened
    fun hasNeighboursOpened(index: Int) =
            requireStudent(index).hasNeighboursOpened
    fun hasDistantsOpened(index: Int) =
            requireStudent(index).hasDistantsOpened
}

class OpenableStudent(name: String, neighbours: Array<String>, distants: Array<String>, val opened: Int = CLOSED) :
        Student(name, neighbours, distants) {
    companion object {
        const val CLOSED = 0x0
        const val NEIGHBOURS_OPENED = 0x1
        const val DISTANTS_OPENED = 0x2
    }

    constructor(student: Student, opened: Int) : this(student.name, student.neighbours, student.distants, opened)

    val isOpened get() = opened != CLOSED
    val isClosed get() = !isOpened

    val hasNeighboursOpened get() = opened == NEIGHBOURS_OPENED
    val hasDistantsOpened get() = opened == DISTANTS_OPENED

    val openWishes: Array<String>?
        get() = when (opened) {
            CLOSED -> null
            NEIGHBOURS_OPENED -> neighbours
            DISTANTS_OPENED -> distants
            else -> throw IllegalStateException("Unknown value for opened.")
        }

    fun withName(name: String) =
            OpenableStudent(name, neighbours, distants, opened)

    fun withOpen(opened: Int) =
            OpenableStudent(name, neighbours, distants, opened)

    /**
     * Returns the student with the neighbours replaced with the supplied ones.
     *
     * @param neighbours The new neighbours.
     * @param openNeighbours If true, neighbours will be opened afterwards, if false, it will be kept as is.
     */
    fun withNeighbours(neighbours: Array<String>, openNeighbours: Boolean = false) =
            OpenableStudent(name, neighbours, distants, if (openNeighbours) NEIGHBOURS_OPENED else opened)

    /**
     * Returns the student with the distants replaced with the supplied ones.
     *
     * @param distants The new distants.
     * @param openDistants If true, distants will be opened afterwards, if false, it will be kept as is.
     */
    fun withDistants(distants: Array<String>, openDistants: Boolean = false) =
            OpenableStudent(name, neighbours, distants, if (openDistants) DISTANTS_OPENED else opened)

    fun withOpenWishes(newWishes: Array<String>): OpenableStudent =
            when (opened) {
                OpenableStudent.NEIGHBOURS_OPENED -> withNeighbours(newWishes)
                OpenableStudent.DISTANTS_OPENED -> withDistants(newWishes)
                OpenableStudent.CLOSED -> throw UnsupportedOperationException("Setting open wishes while wishes were closed.")
                else -> throw IllegalStateException("Unknown value for opened.")
            }

    /**
     * Does this student mention the given wish?
     *
     * @return True if the wish is either among the neighbours or among the distants, false otherwise.
     */
    fun contains(wish: String) =
            neighbours.contains(wish) || distants.contains(wish)
}

fun OpenableStudent.withSwapWishes(wishName: String): OpenableStudent {
    val neighbourI = neighbours.indexOf(wishName)
    val distantI = distants.indexOf(wishName)

    val newNeighbours: Array<String>
    val newDistants: Array<String>

    if (neighbourI == distantI) // Both -1, i.e. not among the wishes
        return this
    else if (neighbourI != -1) {
        newNeighbours = neighbours.filter { it != wishName }.toTypedArray()

        newDistants = distants.toMutableList().apply {
            add(min(neighbourI, distants.size), wishName)
        }.toTypedArray()
    } else {
        newDistants = distants.filter { it != wishName }.toTypedArray()

        newNeighbours = neighbours.toMutableList().apply {
            add(min(distantI, neighbours.size), wishName)
        }.toTypedArray()
    }

    return OpenableStudent(name, newNeighbours, newDistants, opened)
}

fun OpenableStudent.withNeighboursOpen(): OpenableStudent =
        withOpen(OpenableStudent.NEIGHBOURS_OPENED)

fun OpenableStudent.withDistantsOpen() =
        withOpen(OpenableStudent.DISTANTS_OPENED)

fun OpenableStudent.asClosed(): OpenableStudent =
        OpenableStudent(name, neighbours, distants, OpenableStudent.CLOSED)

fun OpenableStudent.withAppendNeighbour(neighbour: String, openNeighbours: Boolean = false) =
        withInsertNeighbour(neighbour, neighbours.size, openNeighbours)

fun OpenableStudent.withInsertNeighbour(neighbour: String, index: Int, openNeighbours: Boolean = false): OpenableStudent {
    val newNeighbours = neighbours.toMutableList()
    newNeighbours.add(index, neighbour)
    return withNeighbours(newNeighbours.toTypedArray(), openNeighbours)
}

fun OpenableStudent.withAppendDistant(distant: String, openDistants: Boolean = false) =
        withInsertDistant(distant, distants.size, openDistants)

fun OpenableStudent.withInsertDistant(distant: String, index: Int, openDistants: Boolean = false): OpenableStudent {
    val newDistants = distants.toMutableList()
    newDistants.add(index, distant)
    return withDistants(newDistants.toTypedArray(), openDistants)
}

private fun Array<String>.withRemove(index: Int) =
        toMutableList().apply {
            removeAt(index)
        }.toTypedArray()
private fun Array<String>.withRemove(elem: String) =
        toMutableList().apply {
            remove(elem)
        }.toTypedArray()

fun OpenableStudent.withRemoveNeighbour(index: Int) =
        withNeighbours(neighbours.withRemove(index))
fun OpenableStudent.withRemoveNeighbour(wish: String) =
        withNeighbours(neighbours.withRemove(wish))

fun OpenableStudent.withRemoveDistant(index: Int) =
        withDistants(distants.withRemove(index))
fun OpenableStudent.withRemoveDistant(wish: String) =
        withDistants(distants.withRemove(wish))

fun OpenableStudent.withRemoveOpen(wish: String) =
        openWishes?.let {
            withOpenWishes(it.withRemove(wish))
        } ?: this

fun OpenableStudent.withMoveOpenWish(oldIndex: Int, newIndex: Int): OpenableStudent {
    val oldWishes = openWishes ?: return this
    return withOpenWishes(oldWishes.withMoveElement(oldIndex, newIndex))
}

private fun Array<String>.withMoveElement(oldIndex: Int, newIndex: Int): Array<String> {
    val list = toMutableList()
    val old = list.removeAt(oldIndex)
    list.add(newIndex, old)
    return list.toTypedArray()
}