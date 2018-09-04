package com.zoniklalessimo.seatingplanner

import android.graphics.Point
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import com.zoniklalessimo.seatingplanner.choosing.EmptyDataTable
import com.zoniklalessimo.seatingplanner.scene.DisplacementInformation
import com.zoniklalessimo.seatingplanner.scene.EmptyTableView
import com.zoniklalessimo.seatingplanner.scene.OnTableDragListener
import com.zoniklalessimo.seatingplanner.scene.TableScene
import kotlinx.android.synthetic.main.activity_construct_empty_plan.*

class EditEmptyPlanActivity : AppCompatActivity(), TableScene, OnTableDragListener {

    companion object {
        private const val LOG_TAG = "ConstructEmptyPlanAct"
    }

    override var tabbedTable: EmptyTableView? = null

    override var movableTables = hashSetOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_construct_empty_plan)

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

        // Spawn one table because an empty scene is intimidating
        addTable()
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
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

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

    fun addTable(table: EmptyDataTable): Int {
        val view = spawnTable(root, layoutInflater)
        view.seatCount = table.seatCount
        view.separators = table.separators
        return addTable(view, table.xBias, table.yBias)
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
