package com.zoniklalessimo.seatingplanner

import android.graphics.Point
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import com.zoniklalessimo.seatingplanner.choosing.EmptyDataTable
import com.zoniklalessimo.seatingplanner.choosing.EmptyDataTablePlan
import com.zoniklalessimo.seatingplanner.scene.DisplacementInformation
import com.zoniklalessimo.seatingplanner.scene.EmptyTableView
import com.zoniklalessimo.seatingplanner.scene.OnTableDragListener
import com.zoniklalessimo.seatingplanner.scene.TableScene
import kotlinx.android.synthetic.main.activity_construct_empty_plan.*
import java.io.File

class EditEmptyPlanActivity : AppCompatActivity(), TableScene, OnTableDragListener {

    companion object {
        private const val LOG_TAG = "ConstructEmptyPlanAct"
    }

    override var tabbedTable: EmptyTableView? = null

    override var movableTables = hashSetOf<View>()

    lateinit var title: String

    lateinit var src: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_construct_empty_plan)

        // Map xml tag types to usable tag types
        for (child in root) {
            val tag = child.getTag(R.id.drag_disabled) ?: continue
            if (tag is String) {
                child.setTag(R.id.drag_disabled, tag == "true")
            }
        }

        slideSideOptionsOut()

        //region sideOptions UI controls
        edit_separators_table.setOnTouchListener(
                TableScene.EditSeparatorsTableOnTouchListener { tabbedTable })
        //endregion

        //region scene controls
        root.setOnClickListener {
            resetActionStates(root)
        }

        if (savedInstanceState == null) {
            val planData = intent.getSerializableExtra(resources.getString(R.string.table_plan_extra)) as EmptyDataTablePlan
            title = planData.name
            src = planData.src as File

            planData.tables.forEach {
                addTable(it)
            }
        } else {
            src = savedInstanceState.getSerializable(resources.getString(R.string.src_extra)) as File
            title = savedInstanceState.getString(resources.getString(R.string.name_extra))
        }

        //endregion scene controls

        optionsGuideEnd = resources.getDimension(R.dimen.side_options_width_empty_plan).toInt()

        //region Drag and Drop Listener
        root.setOnDragListener(this)
    }
    //region OnTableDragListener overrides
    override var displaceInfo: DisplacementInformation? = null
    override var shadowTouchPoint: Point? = null
    override val constraints
        get() = rootConstraints
    override val optionsGuide: Guideline
        get() = options_guide
    //endregion

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState ?: return
        outState.putSerializable(resources.getString(R.string.src_extra), src)
        outState.putString(resources.getString(R.string.name_extra), title)
    }

    override fun onPause() {
        super.onPause()

        save()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // android docs: https://developer.android.com/guide/topics/ui/menus.html
        val inflater = menuInflater
        inflater.inflate(R.menu.add_table_empty_plan_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_empty_table -> {
                addTable()
            }
            R.id.save -> {
                save()
            }
            R.id.save_as -> {
                // TODO
                save()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun save(location: File? = null) {
        val plan = EmptyDataTablePlan(title, getTables(), location ?: src)
        plan.save()

        Toast.makeText(this, R.string.saved_msg, Toast.LENGTH_LONG).show()
    }

    //region Xml callbacks
    fun addSeatToTabbed(v: View) {
        tabbedTable?.let {
            it.seatCount += 1
        }
        updateSideOptionViews()
    }

    fun removeSeatFromTabbed(v: View) {
        tabbedTable?.let {
            it.seatCount -= 1
        }
        updateSideOptionViews()
    }

    fun deleteTabbed(v: View) {
        val tabbed = tabbedTable
        if (tabbed != null) {
            tabbed.resetTabbed()
            root.removeView(tabbed)
        }
        slideSideOptionsOut()
    }
    //endregion

    //region loading
    fun getTables(): List<EmptyDataTable> {
        val tables = mutableListOf<EmptyDataTable>()
        for (child in root) {
            if (child is EmptyTableView) {
                val xBias = getBias(child.x, child.width.toFloat(), root.width.toFloat())
                val yBias = getBias(child.y, child.height.toFloat(), root.height.toFloat())
                tables.add(EmptyDataTable(xBias, yBias, child.seatCount, child.separators))
            }
        }
        return tables
    }

    fun addTable(table: EmptyDataTable) = addTable(table, spawnTable(root, layoutInflater))

    fun addTable(data: EmptyDataTable, table: EmptyTableView): Int {
        table.seatCount = data.seatCount
        table.separators = data.separators
        return addTable(table, data.xBias, data.yBias)
    }
    //endregion

    //region sideOption controls
    private fun addTable() = addTable(spawnTable(root, layoutInflater))

    override fun addTable(table: EmptyTableView, x: Float, y: Float): Int {
        val xBias = makeBias(x,
                table.tableWidth - table.horizontalFrame,
                options_guide.left.toFloat())

        val yBias = makeBias(y,
                table.tableHeight - table.verticalFrame,
                root.height.toFloat())

        rootConstraints.clone(root)
        rootConstraints.connectTable(table.id, options_guide.id, xBias, yBias)
        rootConstraints.applyTo(root)

        return table.id
    }

    private var optionsGuideEnd: Int = 0
    private val rootConstraints = ConstraintSet()

    private fun updateSideOptionViews() {
        edit_separators_table.seatCount = tabbedTable?.seatCount ?: 1
        edit_separators_table.separators = tabbedTable?.separators ?: sortedSetOf()
    }

    var sideOptionsPresent = true

    override fun slideSideOptionsOut() {
        if (sideOptionsPresent) {
            sideOptionsPresent = false
            TransitionManager.beginDelayedTransition(root)
            options_group.visibility = View.GONE
            rootConstraints.clone(root)
            rootConstraints.setGuidelineEnd(R.id.options_guide, 0)
            rootConstraints.applyTo(root)
        }
    }

    override fun slideSideOptionsIn() {
        if (!sideOptionsPresent) {
            sideOptionsPresent = true
            updateSideOptionViews()
            TransitionManager.beginDelayedTransition(root)
            options_group.visibility = View.VISIBLE
            rootConstraints.clone(root)
            rootConstraints.setGuidelineEnd(R.id.options_guide, optionsGuideEnd)
            rootConstraints.applyTo(root)
        }
    }
    //endregion
}
