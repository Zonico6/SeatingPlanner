package com.zoniklalessimo.seatingplanner.tablePlan

import android.graphics.Point
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.transition.TransitionManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import com.zoniklalessimo.seatingplanner.DisplacementInformation
import com.zoniklalessimo.seatingplanner.OnTableDragListener
import com.zoniklalessimo.seatingplanner.R
import com.zoniklalessimo.seatingplanner.TableScene
import kotlinx.android.synthetic.main.activity_construct_empty_plan.*

class ConstructEmptyPlanActivity : AppCompatActivity(), TableScene, OnTableDragListener {

    companion object {
        private const val LOG_TAG = "ConstructEmptyPlanAct"
    }

    override var tabbedTable: EmptyTableView? = null

    override var movableTables = hashSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_construct_empty_plan)

        for (child in root) {
            val tag = child.getTag(R.id.drag_disabled) ?: continue
            if (tag is String)
                child.setTag(R.id.drag_disabled, tag.toBoolean())
        }

        slideSideOptionsOut()

        helper_btn.setOnClickListener {
            Log.d(LOG_TAG, "The coords of all the tables are: ")
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i) as? EmptyTableView ?: continue

                Log.d(LOG_TAG, "x: " + child.x)
                Log.d(LOG_TAG, "y: " + child.y)
            }
        }

        //region sideOptions UI controls
        edit_separators_table.setOnTouchListener { view, event ->
            if (event.action != MotionEvent.ACTION_DOWN)
                return@setOnTouchListener false

            (view as? EmptyTableView)?.let {
                val partition = view.closestPartitionTo(event.x)

                if (!it.separators.contains(partition)) {
                    it.addSeparator(partition)
                    tabbedTable?.addSeparator(partition)
                } else {
                    it.removeSeparator(partition)
                    tabbedTable?.removeSeparator(partition)
                }
            } ?: return@setOnTouchListener false
            true
        }
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

    private fun addTable(): Int {
        return addTable(spawnTable(root, layoutInflater))
    }

    override fun addTable(table: EmptyTableView, x: Float, y: Float): Int {
        val xBias = if (x < 0) {
            -x / (options_guide.left.toFloat() - table.tableWidth - table.horizontalFrame)
        } else
            x

        val yBias = if (y < 0) {
            -y / (root.height.toFloat() - table.tableHeight - table.verticalFrame)
        } else
            y

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

    private fun toggleSideOptions() {
        sideOptionsPresent = if (sideOptionsPresent) {
            slideSideOptionsOut()
            false
        } else {
            slideSideOptionsIn()
            true
        }
    }

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
}
