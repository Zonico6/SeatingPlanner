package com.zoniklalessimo.seatingplanner.tablePlan

import android.graphics.Point
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.transition.TransitionManager
import kotlinx.android.synthetic.main.activity_construct_empty_plan.*
import com.zoniklalessimo.seatingplanner.R
import com.zoniklalessimo.seatingplanner.TableScene
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast

const val NEW_TABLE_SEAT_COUNT: Int = 4
// TODO:
//      Bugs:
//          Furthermore, When the drag shadow goes into sideOptions, the view is positioned and made visible again, but NOT hidden again -> hide view when leaving sideOptions
//          Setting seatCount, etc. doesn't work -> Probably because the proper event doesn't fire -> Button on keyboard is not called enter but simply next
class ConstructEmptyPlanActivity : AppCompatActivity(), TableScene {

    companion object {
        private const val TAG = "ConstructEmptyPlanAct."
    }

    override var shadowTouchPoint: Point? = null

    override var tabbedTable: EmptyTableView? = null
        set(value) {
            if (value == null) {
                if (sideOptionsPresent) {
                    Log.w(TAG, "Variable 'tabbedTable' was set to 'null' not by method 'resetTabbed' " +
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

        // DEBUG
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

        //TODO
        seat_count.setOnEditorActionListener { tv, actionId, event ->
            ifFinishedTyping(actionId, event) {
                tabbedTable?.let {
                    updateSeatCount()
                    true
                } ?: false
            }
        }
        rotation_tv.setOnEditorActionListener { tv, actionId, event ->
            ifFinishedTyping(actionId, event) {
                Toast.makeText(this, "This feature isn't supported yet!", Toast.LENGTH_LONG).show()
                false
            }
        }
        separators.setOnEditorActionListener { tv, actionId, event ->
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

        val appRes = resources
        optionsGuideEnd = appRes.getDimension(R.dimen.side_options_width_empty_plan).toInt()

        // Handle drag and drop events
        root.setOnDragListener { _, event ->
            when (event.action) {

                DragEvent.ACTION_DRAG_STARTED -> {
                    // Hide original view
                    val draggedView = event.localState as View
                    draggedView.visibility = View.INVISIBLE
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    // Determine if view was dragged into sideOptions area and then display toast
                    if (sideOptionsPresent && shadowTouchPoint != null) {
                        val optionsGap = options_guide.left - event.x - shadowTouchPoint!!.x
                        val draggedView = event.localState as View
                        if (optionsGap <= 0) {
                            draggedView.visibility = View.VISIBLE
                            draggedView.y = event.y - shadowTouchPoint!!.y
                            draggedView.x = (options_guide.left - draggedView.width).toFloat()

                            /*if (currentToast == null) {
                                val toast = Toast.makeText(this@ConstructEmptyPlanActivity,
                                        this@ConstructEmptyPlanActivity.getString(R.string.cant_place_here),
                                        Toast.LENGTH_SHORT)
                                currentToast = toast
                                toast.show()
                            }*/
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

                    draggedView.visibility = View.VISIBLE
                    return@setOnDragListener true
                }

                DragEvent.ACTION_DRAG_ENTERED,
                DragEvent.ACTION_DRAG_EXITED,
                DragEvent.ACTION_DRAG_ENDED -> {
                    // returns true; the value is ignored.
                    return@setOnDragListener true
                }

                // An unknown action type was received.
                else -> Log.e("DragDrop Example", "Unknown action type received by OnDragListener.")
            }
            false
        }
    }

    private fun updateSeatCount() {
        tabbedTable?.apply {
            val text = seat_count.text.toString()
            text.filter { it.isDigit() }
            this.seatCount = text.toInt()
        }
    }

    private fun updateSeparators() {
        val text = separators.text
        tabbedTable?.assignSeperators(text)
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
                // TODO: Ok final decision on how to store tables:
                // Connect all sides to parent except for right, connect that one to the guide
                // Then store the position in bias values of the views
                val id = addTable()

                constrSet.clone(root)
                constrSet.connectTable(id, options_guide.id)
                constrSet.applyTo(root)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun addTable(): Int {
        val table = EmptyTableView(this)

        // TODO: Miscellaneous/Aesthatics
        table.seatCount = NEW_TABLE_SEAT_COUNT
        table.seatWidth = sample_table.seatWidth
        table.seatHeight = sample_table.seatHeight
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
    private val constrSet = ConstraintSet()
    private var sideOptionsPresent = true

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
            Log.e(TAG, "Tried to updateSideViewOptions without a tabbed view.")
            e.printStackTrace()
        }
    }
    override fun slideSideOptionsOut() {
        if (sideOptionsPresent) {
            sideOptionsPresent = false
            TransitionManager.beginDelayedTransition(root)
            options_group.visibility = View.GONE
            constrSet.clone(root)
            constrSet.setGuidelineEnd(R.id.options_guide, 0)
            constrSet.applyTo(root)
        }
    }
    override fun slideSideOptionsIn() {
        if (!sideOptionsPresent) {
            sideOptionsPresent = true
            updateSideOptionViews()
            TransitionManager.beginDelayedTransition(root)
            options_group.visibility = View.VISIBLE
            constrSet.clone(root)
            constrSet.setGuidelineEnd(R.id.options_guide, optionsGuideEnd)
            constrSet.applyTo(root)
        }
    }
}
