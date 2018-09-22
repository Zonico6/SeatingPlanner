package com.zoniklalessimo.seatingplanner.scene

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.zoniklalessimo.seatingplanner.R
import java.util.*

interface TableScene : ActionStateUser, TablePlacer {
    companion object {
        const val LOG_TAG = "TableScene"
    }

    //region utils
    fun ConstraintSet.modify(modifications: ConstraintSet.() -> Unit, layout: ConstraintLayout) {
        clone(layout)
        modifications()
        applyTo(layout)
    }

    /**
     * The bounding rectangle of this view.
     */
    fun View.frame() = RectF(x, y, x + width, y + height)

    /**
     * The point at the center of the view after accounting for translation.
     */
    val View.center
        get() = PointF(x + width / 2, y + height / 2)
    /**
     * The point at the center of the views layout.
     */
    val View.middle
        get() = Point(left + width / 2, top + height / 2)
    //endregion

    //region helpers
    class EditSeparatorsTableOnTouchListener(val tabbedTable: () -> EmptyTableView?) : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (event.action != MotionEvent.ACTION_DOWN)
                return false

            (view as? EmptyTableView)?.let {
                val partition = view.closestPartitionTo(event.x)

                if (!it.separators.contains(partition)) {
                    it.addSeparator(partition)
                    tabbedTable()?.addSeparator(partition)
                } else {
                    it.removeSeparator(partition)
                    tabbedTable()?.removeSeparator(partition)
                }
            } ?: return false
            return true
        }
    }

    var shadowTouchPoint: Point?

    fun EmptyTableView.startTableDrag() {
        val touchX = if (touchedSeat != -1) {
            (priorArea(touchedSeat) + seatWidth / 2).toInt()
        } else {
            width / 2
        }
        val touch = Point(touchX, height / 2)
        val dragShadow = TableDragShadowBuilder(this, touch) { _, touchPoint ->
            shadowTouchPoint = touchPoint
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startDragAndDrop(null, dragShadow, this, 0)
        } else {
            @Suppress("DEPRECATION")
            startDrag(null, dragShadow, this, 0)
        }
    }

    /**
     * Add a table at the given position
     *
     * @param table The table, that's gonna be added
     * @param x The horizontal bias. If negative, it is interpreted as the absolute position.
     * @param y The vertical bias. If negative, it is interpreted as the absolute position.
     *
     * @return The id of the created table.
     */
    fun addTable(table: EmptyTableView, x: Float = 0.5f, y: Float = 0.5f): Int

    fun spawnTable(root: ViewGroup, inflater: LayoutInflater): EmptyTableView {
        val table = inflater.inflate(R.layout.empty_table_dark, root, false) as EmptyTableView

        table.id = View.generateViewId()
        table.setTag(R.id.table_state, ActionState.NONE)

        // Tabbing
        table.setOnClickListener {
            with (table) {
                when (actionState().states) {
                    ActionState.NONE -> setMovable()
                    ActionState.MOVABLE -> tabbed()
                    ActionState.TABBED -> resetActionState()
                    else -> { resetActionState() } // Something went wrong -> safely reset
                }
            }
        }
        //Dragging
        table.setOnLongClickListener { _ ->
            val seat = table.touchedSeat

            if (table.actionState().equals(ActionState.MOVABLE) && seat != -1) {
                fun makeNewTable(seatCount: Int, separators: SortedSet<Int>, xOffset: Float): Int {
                    val newTable = spawnTable(root, inflater)
                    newTable.setTag(R.id.drag_disabled, true)
                    newTable.tag = "drag_disabled_once"

                    newTable.seatCount = seatCount
                    newTable.separators = separators

                    // Negative so addTable() proceeds with it as absolute coordinates and not biases
                    return addTable(newTable, -table.left - xOffset, -table.top + 0f)
                }

                // First is the new table AFTER the touch
                var sep = table.separatorAfterSeat(seat)
                if (sep != table.seatCount) {
                    makeNewTable(table.seatCount - sep,
                            table.separatorsFromSeat(seat), sep * table.seatWidth)
                }

                // Then the table BEFORE it
                sep = table.separatorBeforeSeat(seat)
                if (sep != 0) {
                    makeNewTable(sep, table.separatorsToSeat(seat), 0f)
                }

                table.cut(table.sectionAround(seat))
            }
            table.startTableDrag()
            true
        }
        root.addView(table)
        return table
    }
    //endregion helpers
}

class TableDragShadowBuilder(view: View, private val touchPoint: Point?, private val onShadowMetricsProvided: (Point, Point) -> Unit) : View.DragShadowBuilder(view) {

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        (view as? EmptyTableView)?.let {
            outShadowSize.set(it.tableWidth.toInt() + it.horizontalFrame, it.tableHeight.toInt() + it.verticalFrame)
        }

        if (touchPoint != null)
            outShadowTouchPoint.set(touchPoint.x, touchPoint.y)
        else
            outShadowTouchPoint.set(outShadowSize.x / 2, outShadowSize.y / 2)

        onShadowMetricsProvided(outShadowSize, outShadowTouchPoint)
    }
}