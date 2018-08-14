package com.zoniklalessimo.seatingplanner.tablePlan

import android.graphics.Point
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.zoniklalessimo.seatingplanner.*
import kotlinx.android.synthetic.main.activity_construct_empty_plan.*

class ConstructEmptyPlanActivity : AppCompatActivity(), TableScene, OnTableDragListener {

    companion object {
        private const val LOG_TAG = "ConstructEmptyPlanAct"
    }

    override var tabbedTable: EmptyTableView? = null
        set(value) {
            if (value == null) {
                if (sideOptionsPresent) {
                    Log.w(LOG_TAG, "Variable 'tabbedTable' was not set to 'null' by method 'resetTabbed' " +
                            "though this is convention since it ensures that variables are synchronized.")
                    field?.resetTabbed()
                    return
                }
            } else {
                if (field != null) {
                    field?.resetHighlight()
                }
            }
            field = value
        }

    override var movableTable: EmptyTableView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_construct_empty_plan)

        slideSideOptionsOut()
        apply_changes.setOnClickListener {
            updateSeatCount()
            updateSeparators()
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
            resetActionStates()
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
        val id = spawnTable(root, layoutInflater)

        rootConstraints.clone(root)
        rootConstraints.connectTable(id, options_guide.id)
        rootConstraints.applyTo(root)

        return id
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
