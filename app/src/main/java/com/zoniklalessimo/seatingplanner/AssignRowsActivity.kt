package com.zoniklalessimo.seatingplanner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.zoniklalessimo.seatingplanner.assigningRows.AssignRow
import com.zoniklalessimo.seatingplanner.assigningRows.AssignRowsAdapter
import com.zoniklalessimo.seatingplanner.assigningRows.AssignRowsViewModel
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.EmptyDataTablePlan
import com.zoniklalessimo.seatingplanner.core.seating.Student
import com.zoniklalessimo.seatingplanner.scene.EmptyTableView
import com.zoniklalessimo.seatingplanner.scene.TablePlacer
import kotlinx.android.synthetic.main.activity_assign_rows.*
import java.io.Serializable
import java.util.ArrayList

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
        val inflater = layoutInflater

        // It's important that you add the views before you initialize the constraintSet
        val tableViews = List(model.getTablesLiveData().size) {
           val infl = inflater.inflate(R.layout.assign_students_row, root, false)
            infl.id = View.generateViewId()
            root.addView(infl)
            infl as FrameLayout
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(root)

        model.getTablesLiveData().zip(tableViews).forEach { (liveData, view) ->
            val tableView = view.findViewById<EmptyTableView>(R.id.table)
            tableView.seatCount = liveData.value!!.seatCount

            view.setOnClickListener {
                val students = liveData.value!!.students

                AlertDialog.Builder(this).
                        setItems(students.toTypedArray()) { _, i ->
                            model.removeStudentFromTable(liveData, students[i])
                            model.addStudent(students[i])
                            sidebar.adapter?.notifyItemInserted(model.studentNames.size - 1)
                        }.show()
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
        val adapter = AssignRowsAdapter(model) { view, position ->
            sidebar.startDragAndDrop(
                    null,
                    View.DragShadowBuilder(view),
                    model.studentNames[position],
                    0
            )
            true
        }
        sidebar.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@AssignRowsActivity)
            this.adapter = adapter
            setOnDragListener(SideBarsStudentDragListener(adapter))
        }
        //endregion
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.assign_rows_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.construct_plan -> {
                val intent = Intent(this, DisplaySeatingPlanActivity::class.java)
                intent.putParcelableArrayListExtra(SEATED_TABLE_EXTRA_NAME,
                        constructSeatingPlan(model.getTables(), model.studentNames, studentSet) as ArrayList<out Parcelable>)
                startActivity(intent)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}

class SideBarsStudentDragListener(private val adapter: AddRemoveStudentAdapter) : View.OnDragListener {
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
        // Entered this view:
        //   - change display number of taken seats
        // ---
        // Left this view:
        //   - Reset taken seats number

        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {}

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