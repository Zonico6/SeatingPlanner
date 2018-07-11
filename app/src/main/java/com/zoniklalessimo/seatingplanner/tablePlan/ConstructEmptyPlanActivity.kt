package com.zoniklalessimo.seatingplanner.tablePlan

import android.graphics.Point
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.zoniklalessimo.seatingplanner.R
import com.zoniklalessimo.seatingplanner.TableScene
import kotlinx.android.synthetic.main.activity_construct_empty_plan.*
//import androidx.core.view.*

const val NEW_TABLE_SEAT_COUNT: Int = 4

class ConstructEmptyPlanActivity : AppCompatActivity(), TableScene {

    companion object {
        private const val LOG_TAG = "ConstructEmptyPlanAct"
    }

    override var shadowTouchPoint: Point? = null

    override var tabbedTable: EmptyTableView? = null
        set(value) {
            if (value == null) {
                if (sideOptionsPresent) {
                    Log.w(LOG_TAG, "Variable 'tabbedTable' was set to 'null' not by method 'resetTabbed' " +
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_construct_empty_plan)

        slideSideOptionsOut()

        helper_btn.setOnClickListener {
            Log.d(LOG_TAG, "translation x: " + sample_table.translationX + ", y: " + sample_table.translationY)
            if (sample_table.x == sample_table.translationX) {
                Log.d(LOG_TAG, "x and translationX are the same: " + sample_table.x)
            } else {
                Log.d(LOG_TAG, "x and translationX are NOT the same!!")
            }
            Log.d(LOG_TAG, "sample_table: width: " + sample_table.width + ", height: " + sample_table.height)
            Log.d(LOG_TAG, "sample_table.left = " + sample_table.left + ", sample_table.x = " + sample_table.x)
            Log.d(LOG_TAG, "sample_table-seatSizes: w=" + sample_table.seatWidth + ", h=" + sample_table.seatHeight)
        }

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
            tabbedTable?.resetTabbed()
        }

        sample_table.setOnClickListener {
            sample_table.tabbed()
        }

        sample_table.setOnLongClickListener {
            sample_table.startTableDrag()
            true
        }
        //endregion scene controls

        Log.d(LOG_TAG, "In onCreate: sample_table.left = " + sample_table.left + ", sample_table.x = " + sample_table.x)

        optionsGuideEnd = resources.getDimension(R.dimen.side_options_width_empty_plan).toInt()

        // Handle drag and drop events
        root.setOnDragListener { _, event ->
            when (event.action) {

                DragEvent.ACTION_DRAG_STARTED -> {
                    // Hide original view
                    val draggedView = event.localState as View
                    draggedView.visibility = View.INVISIBLE
                    rootConstraints.modify ({
                        prepareConstraintsForDrag(draggedView)
                    })
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    // Display view at sideOptions in order to provide feedback that the table can't be placed inside the sideOptions
                    // Determine if view was dragged into sideOptions area and then display toast
                    if (sideOptionsPresent && shadowTouchPoint != null) {
                        val optionsGap = options_guide.left - event.x - shadowTouchPoint!!.x
                        val draggedView = event.localState as View
                        if (optionsGap <= 0) {
                            draggedView.visibility = View.VISIBLE
                            draggedView.y = event.y - shadowTouchPoint!!.y
                            draggedView.x = (options_guide.left - draggedView.width).toFloat()
                        } else {
                            draggedView.visibility = View.INVISIBLE
                        }
                    }
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DROP -> {
                    // Move old view to new position and make it visible
                    val draggedView = event.localState as View

                    val optionsGap = options_guide.left - event.x - shadowTouchPoint!!.x

                        draggedView.x = if (optionsGap < 0) {
                            (options_guide.left - draggedView.width).toFloat()
                        } else {
                            event.x - shadowTouchPoint!!.x
                        }
                        draggedView.y = event.y - shadowTouchPoint!!.y

                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_ENTERED,
                DragEvent.ACTION_DRAG_EXITED -> {
                    // returns true; the value is ignored.
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    shadowTouchPoint = null
                    val draggedView = event.localState as View

                    draggedView.visibility = View.VISIBLE

                    rootConstraints.modify ({
                        restoreBiases(draggedView, options_guide, root.height.toFloat())
                    })
                    // Synchronize x and y with layout position
                    draggedView.translationX = 0f
                    draggedView.translationY = 0f
                }
                // An unknown action type was received.
                else -> Log.e("DragDrop Example", "Unknown action type received by OnDragListener.")
            }
            false
        }
    }

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
                val id = addTable()

                rootConstraints.clone(root)
                rootConstraints.connectTable(id, options_guide.id)
                rootConstraints.applyTo(root)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun addTable(): Int {
        val table = EmptyTableView(this)

        // table.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // TODO: Miscellaneous/Aesthetics
        table.seatCount = NEW_TABLE_SEAT_COUNT

        table.seatWidth = resources.getDimension(R.dimen.table_seat_width)
        table.seatHeight = resources.getDimension(R.dimen.table_seat_height)

        table.id = View.generateViewId()
        table.setOnClickListener {
            table.tabbed()
        }
        table.setOnLongClickListener {
            table.startTableDrag()
            true
        }
        root.addView(table)
        return table.id
    }

    private var optionsGuideEnd: Int = 0
    private val rootConstraints = ConstraintSet()
    private var sideOptionsPresent = true

    private inline fun ConstraintSet.modify(modifications: ConstraintSet.() -> Unit, layout: ConstraintLayout = root) {
        clone(layout)
        modifications()
        applyTo(layout)
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
