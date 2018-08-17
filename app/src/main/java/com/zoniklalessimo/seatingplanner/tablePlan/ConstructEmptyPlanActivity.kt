package com.zoniklalessimo.seatingplanner.tablePlan

import android.graphics.Point
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.transition.TransitionManager
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zoniklalessimo.seatingplanner.*
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

        slideSideOptionsOut()

        apply_changes.setOnClickListener {
            updateSeatCount()
            updateSeparators()
        }

        helper_btn.setOnClickListener {
            Log.d(LOG_TAG, "The coords of all the tables are: ")
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i) as? EmptyTableView ?: continue

                Log.d(LOG_TAG, "x: " + child.x)
                Log.d(LOG_TAG, "y: " + child.y)
            }
        }

        //region sideOptions UI controls

        fun ifFinishedTyping(actionId: Int, event: KeyEvent?, then: () -> Boolean): Boolean {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed) {
                    // the user is done typing.
                    return then()
                }
            }
            return false
        }

        seat_count.setOnEditorActionListener { _, actionId, event ->
            ifFinishedTyping(actionId, event) {
                tabbedTable?.let {
                    updateSeatCount()
                    true
                } ?: false
            }
        }
        rotation_tv.setOnEditorActionListener { _, actionId, event ->
            ifFinishedTyping(actionId, event) {
                Toast.makeText(this, "This feature isn't supported yet!", Toast.LENGTH_LONG).show()
                false
            }
        }
        separators.setOnEditorActionListener { _, actionId, event ->
            ifFinishedTyping(actionId, event) {
                tabbedTable?.let {
                    updateSeparators()
                    true
                } ?: false
            }
        }

        //endregion sideOptions UI controls

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

    private fun updateSeatCount() {
        tabbedTable?.apply {
            var text = seat_count.text.toString()
            text = text.filter { it.isDigit() }
            this.seatCount = text.toInt()
        }
    }

    private fun updateSeparators() {
        val text = separators.text
        tabbedTable?.assignSeparators(text)
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

        Log.d(LOG_TAG, "Bias is: $xBias and $yBias")

        rootConstraints.clone(root)
        rootConstraints.connectTable(table.id, options_guide.id, xBias, yBias)
        rootConstraints.applyTo(root)

        return table.id
    }

    private var optionsGuideEnd: Int = 0
    private val rootConstraints = ConstraintSet()
    override var sideOptionsPresent = true

    private fun updateSideOptionViews() {
        try {
            seat_count.setText(String.format(this.getString(R.string.seats), tabbedTable!!.seatCount))
            separators.setText(String.format(this.getString(R.string.separators), tabbedTable!!.separatorString()))
            // TODO: Rotation
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Tried to updateSideViewOptions without a tabbed view.")
            e.printStackTrace()
        }
    }

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
