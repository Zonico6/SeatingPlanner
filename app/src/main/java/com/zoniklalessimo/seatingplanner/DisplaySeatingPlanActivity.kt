package com.zoniklalessimo.seatingplanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.assigningRows.AssignRow
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.EmptyDataTable
import com.zoniklalessimo.seatingplanner.core.seating.Student
import com.zoniklalessimo.seatingplanner.core.seating.StudentSet
import com.zoniklalessimo.seatingplanner.core.seating.constructStraightTable
import com.zoniklalessimo.seatingplanner.displaySeatingPlan.DisplaySeatingPlanViewModel
import com.zoniklalessimo.seatingplanner.displaySeatingPlan.SeatedTableView
import com.zoniklalessimo.seatingplanner.scene.modify
import kotlinx.android.synthetic.main.activity_display_seating_plan.*
import java.util.*

const val SEATED_TABLE_EXTRA_NAME = "seated_table_extra_name"

typealias PlanType = List<SeatedDataTable>

class DisplaySeatingPlanActivity : AppCompatActivity() {

    private lateinit var model: DisplaySeatingPlanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_seating_plan)

        model = ViewModelProviders.of(this).get(DisplaySeatingPlanViewModel::class.java)
        val tables = intent.getParcelableArrayListExtra<SeatedDataTable>(SEATED_TABLE_EXTRA_NAME)
        model.initSeatedTables(tables)

        val tableViews= List(model.tableCount) {
            (layoutInflater.inflate(R.layout.seated_table, root, false) as SeatedTableView).apply {
                id = View.generateViewId()
                drawNames = SeatedTableView.NameLetters.FIRST_TWO

                root.addView(this)
            }
        }

        model.getSeatedTables().forEachIndexed { index, liveData ->
            val view = tableViews[index]
            view.separators = liveData.value?.separators ?: sortedSetOf()
            view.seatCount = liveData.value?.seatCount ?: 1

            liveData.observe(this, Observer {
                updateTable(view, it)
            })
        }
    }

    private fun updateTable(view: SeatedTableView, table: SeatedDataTable) = view.run {
        modify(root) {
            center(view.id, ConstraintSet.PARENT_ID, ConstraintSet.START, 0,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, 0, table.xBias)
            center(view.id, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0,
                    ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0, table.yBias)

        }
        students = table.students
    }

    private fun modify(layout: ConstraintLayout, block: ConstraintSet.() -> Unit) =
            ConstraintSet().modify(layout, block)
}

class SeatedDataTable(x: Float, y: Float, seatCount: Int, separators: SortedSet<Int>, var students: List<String?>) :
        EmptyDataTable(x, y, seatCount, separators), Parcelable {
    constructor(parcel: Parcel) : this(EmptyDataTable(parcel), parcel.createStringArrayList())

    constructor(table: EmptyDataTable, students: List<String?>)
            : this(table.xBias, table.yBias, table.seatCount, table.separators, students)

    constructor(table: AssignRow, students: List<String?>) :
            this(table.xBias, table.yBias, table.seatCount, table.separators, students)

    operator fun get(index: Int) = students[index]

    fun seatOf(student: String): Int = students.indexOf(student)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeStringList(students)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SeatedDataTable> {
        override fun createFromParcel(parcel: Parcel): SeatedDataTable {
            return SeatedDataTable(parcel)
        }

        override fun newArray(size: Int): Array<SeatedDataTable?> {
            return arrayOfNulls(size)
        }
    }
}

fun constructSeatingPlan(tables: Iterable<AssignRow>, otherStudents: List<String>, studentLookupList: List<Student>): PlanType {
    val studentLookup = studentLookupList.map { it.name to it }.toMap()
    val students = StudentSet(otherStudents.map { studentLookup[it]!! })
    val studentSets = students.split(tables.map { it.availableSeats })
    /*return tables.sortedByDescending { it.seatCount }.
            mapIndexed { i, row ->
                val studs = studentSets[i] + row.students.map { studentLookup[it]!! }
                SeatedDataTable(row, studs.students.map { it.name }.toList())
            }*/
    // constructStraightTable keeps screwing things up horribly
    return tables.sortedByDescending { it.seatCount }.
            mapIndexed { i, row ->
                val studs = constructStraightTable(studentSets[i] +
                        row.students.map { studentLookup[it]!! })
                SeatedDataTable(row, studs.toList())
            }
}