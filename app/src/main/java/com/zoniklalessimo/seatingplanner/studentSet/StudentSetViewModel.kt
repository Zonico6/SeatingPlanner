package com.zoniklalessimo.seatingplanner.studentSet

import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.zoniklalessimo.seatingplanner.core.seating.Student
import java.lang.IllegalStateException

class StudentSetViewModel : ViewModel() {
    private val students = mutableListOf<MutableLiveData<Student>>()
    private fun requireStudent(index: Int): Student =
        students[index].requireStudent()
    fun getStudents() = students.map { it.requireStudent() }

    val studentCount get() = students.size

    private fun LiveData<Student>.requireStudent(): Student =
            this.value
                ?: throw IllegalStateException("Null value among students.")

    fun getStudent(index: Int) = requireStudent(index)

    fun getStudentObserver(adapter: RecyclerView.Adapter<*>, index: Int) = Observer<Student> {
            adapter.notifyItemInserted(index)
        }

    fun bindWithAdapter(owner: LifecycleOwner, adapter: RecyclerView.Adapter<*>) {
        students.forEachIndexed { index, student ->
            student.observe(owner, getStudentObserver(adapter, index))
        }
    }

    fun addStudent(owner: LifecycleOwner, adapter: RecyclerView.Adapter<*>,
                   student: Student = Student("", emptyArray(), emptyArray())) {

        val liveData = MutableLiveData<Student>()

        liveData.observe(owner, getStudentObserver(adapter, students.size))

        liveData.value = student
        students.add(liveData)
    }

    fun renameStudent(newName: String, index: Int) {
        val oldStudent = requireStudent(index)
         // Rename
        students[index].value = Student(newName, oldStudent.neighbours, oldStudent.distants)

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

    fun addNeighbour(index: Int, newNeighbour: String) {
        val old = requireStudent(index)
        students[index].value = Student(old.name,
                old.neighbours + newNeighbour, old.distants)
    }

    fun addDistant(index: Int, newDistant: String) {
        val old = requireStudent(index)
        students[index].value = Student(old.name,
                old.distants + newDistant, old.distants)
    }
}