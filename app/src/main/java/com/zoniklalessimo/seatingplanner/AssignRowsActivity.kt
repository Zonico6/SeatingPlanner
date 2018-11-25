package com.zoniklalessimo.seatingplanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.assigningRows.AssignRow
import com.zoniklalessimo.seatingplanner.assigningRows.AssignRowsAdapter
import com.zoniklalessimo.seatingplanner.assigningRows.AssignRowsViewModel
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.EmptyDataTablePlan
import com.zoniklalessimo.seatingplanner.core.seating.Student
import com.zoniklalessimo.seatingplanner.scene.EmptyTableView
import com.zoniklalessimo.seatingplanner.scene.TablePlacer
import kotlinx.android.synthetic.main.activity_assign_rows.*

const val StudentsExtraKey = "students"

class AssignRowsActivity : AppCompatActivity(), TablePlacer {

    private lateinit var model: AssignRowsViewModel

    private lateinit var plan: EmptyDataTablePlan
    private lateinit var studentSet: List<Student>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_rows)

        model = ViewModelProviders.of(this).get(AssignRowsViewModel::class.java)

        //region Retrieve parameter
        if (savedInstanceState == null) {
            plan = intent.getParcelableExtra<EmptyDataTablePlan>(getString(R.string.table_plan_extra))
            val tables = plan.tables.map { AssignRow(it, emptyList()) }

            model.initTables(tables)

            @Suppress("UNCHECKED_CAST")
            studentSet = intent.getSerializableExtra(StudentsExtraKey) as List<Student>

            model.initStudents(studentSet.map { it.name })
        }
        //endregion

        //region Scene Setup
        val constraintSet = ConstraintSet()
        constraintSet.clone(root)

        val inflater = layoutInflater
        model.getTablesLiveData().forEach { liveData ->
            val view = inflater.inflate(R.layout.assign_students_row, root, true)

            view.setOnClickListener {
                val students = liveData.value?.students ?: emptyList()

                AlertDialog.Builder(this).
                        setItems(students.toTypedArray()) { _, i ->
                            model.removeStudentFromTable(liveData, students[i])
                        }
            }

            view.setOnDragListener(
                    TablesStudentDragListener(
                            view.findViewById(R.id.table),
                            getColor(R.color.tableAcceptStudentHighlight)) {
                        model.addStudentToTable(liveData, it)
                    })

            constraintSet.connectTable(view.id, sidebar_guide.id,
                    liveData.value!!.xBias, liveData.value!!.yBias)

            liveData.observe(this, Observer {
                val text = view.findViewById<TextView>(R.id.student_names)
                text.text = it.students.joinToString(limit = 3)
            })
        }
        constraintSet.applyTo(root)
        //endregion

        //region Side Bar
        val adapter = AssignRowsAdapter(model) { _, position ->
            sidebar.startDragAndDrop(
                    null,
                    View.DragShadowBuilder(),
                    model.studentNames[position],
                    0
            )
            true
        }
        sidebar.setOnDragListener(SideBarsStudentDragListener(adapter))
        sidebar.adapter = adapter
        //endregion
    }
}

class SideBarsStudentDragListener(val adapter: AddRemoveStudentAdapter) : View.OnDragListener {
    interface AddRemoveStudentAdapter {
        fun hideStudent(student: String): Boolean
        fun showStudent(student: String): Boolean
        fun deleteStudent(student: String): Boolean
    }

    override fun onDrag(view: View, event: DragEvent): Boolean {

        val student = event.localState as String

        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                adapter.hideStudent(student)
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                if (event.result) {
                    adapter.deleteStudent(student)
                } else {
                    adapter.showStudent(student)
                }
            }

            else -> return false
        }
        return true
    }
}

private class TablesStudentDragListener(
        val tableView: EmptyTableView,
        val acceptDropHighlightColor: Int,
        val onAcceptStudent: (student: String) -> Unit) : View.OnDragListener {
    override fun onDrag(view: View, event: DragEvent): Boolean {
        // TODO: ---
        // Entered this view:
        //   - change display number of taken seats
        // ---
        // Left this view:
        //   - Reset taken seats number

        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {
                tableView.highlight(acceptDropHighlightColor)
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                tableView.resetHighlight()
            }

            DragEvent.ACTION_DROP -> {
                onAcceptStudent(event.localState as String)
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                tableView.resetHighlight()
            }

            else -> return false
        }
        return true
    }
}