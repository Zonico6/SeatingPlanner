package com.zoniklalessimo.seatingplanner

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zoniklalessimo.seatingplanner.tablePlan.EmptyTableView
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.sqrt

interface TableScene {
    companion object {
        const val LOG_TAG = "TableScene"
    }

    //region helpers
    fun ConstraintSet.connectTable(tableId: Int, guideId: Int, xBias: Float = 0.5f, yBias: Float = 0.5f) {
        center(tableId, ConstraintSet.PARENT_ID, ConstraintSet.START, 0,
                guideId, ConstraintSet.START, 0, xBias)
        center(tableId, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0, yBias)
    }

    var shadowTouchPoint: Point?

    fun View.startTableDrag() {
        val dragShadow = TableDragShadowBuilder(this) { _, touchPoint ->
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

        // Tabbing
        table.setOnClickListener {
            with (table) {
                when (actionState().states) {
                    TableScene.ActionState.NONE,
                    TableScene.ActionState.TABBED -> setMovable()
                    TableScene.ActionState.MOVABLE -> tabbed()
                    else -> { resetActionState() } // Something went wrong -> safely reset
                }
            }
        }
        //Dragging
        table.setOnLongClickListener { _ ->
            val seat = table.touchedSeat

            if (table.actionState().equals(TableScene.ActionState.MOVABLE) && seat != -1) {
                fun makeNewTable(seatCount: Int, separators: SortedSet<Int>, xOffset: Float): Int {
                    val newTable = spawnTable(root, inflater)
                    newTable.setTag(R.id.drag_disabled, true)

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

                table.seatCount = table.sectionAround(seat)
                table.clearSeparators()

                // This fixes the shadow in that it's the appropriate size, however it screws the table's
                // size after it's been placed to be as big as the entire table before the split up
                // val left = table.left + sep * table.seatWidth.toInt()
                // table.layout(left, table.top,
                //        left + table.tableWidth.toInt() + table.horizontalFrame, table.bottom)
            }
            table.startTableDrag()
            true
        }
        root.addView(table)
        return table
    }

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
    //endregion helpers

    //region positioning
    fun ConstraintSet.prepareConstraintsForDrag(table: EmptyTableView) {
        clear(table.id)
        connect(table.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, table.left)
        connect(table.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, table.top)
    }

    fun ConstraintSet.restoreBiases(table: EmptyTableView, sideGuide: Guideline, height: Float) {
        var xBias = table.x / (sideGuide.left - table.width).toFloat()
        var yBias = table.y / (height - table.height)

        // Make sure biases are between 0 and 1
        xBias = Math.min(Math.max(xBias, 0.0000000000001f), 0.999999999999f)
        yBias = Math.min(Math.max(yBias, 0.0000000000001f), 0.999999999999f)


        center(table.id, ConstraintSet.PARENT_ID, ConstraintSet.START, 0,
                sideGuide.id, ConstraintSet.START, 0, xBias)
        center(table.id, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0, yBias)

        // Prevent view from inflating to screen size
        constrainWidth(table.id, table.tableWidth.toInt() + table.horizontalFrame)
        constrainHeight(table.id, table.tableHeight.toInt() + table.verticalFrame)
    }
    //endregion

    //region Highlights
    fun slideSideOptionsIn()
    fun slideSideOptionsOut()

    //region Tabbed
    var tabbedTable: EmptyTableView?

    fun EmptyTableView.tabbed(@Suppress("DEPRECATION") color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                            context.getColor(R.color.tableTabbedHighlight) else
                                            resources.getColor(R.color.tableTabbedHighlight),
                              resetOthers: Boolean = true) {
        if (resetOthers) {
            resetMovableWithoutHighlight()
        }
        tabbedTable = this
        highlight(color)
        slideSideOptionsIn()
    }

    fun EmptyTableView.resetTabbed() {
        resetHighlight()
        resetTabbedWithoutHighlight()
    }

    fun resetTabbedWithoutHighlight() {
        slideSideOptionsOut()
        tabbedTable = null
    }

    fun EmptyTableView.isTabbed() = this == tabbedTable
    //endregion

    //region Movable
    var movableTable: EmptyTableView?

    fun EmptyTableView.setMovable(@Suppress("DEPRECATION") color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                                context.getColor(R.color.tableMoveHighlight) else
                                                resources.getColor(R.color.tableMoveHighlight), resetOthers: Boolean = true) {
        if (resetOthers) {
            resetTabbedWithoutHighlight()
        }
        highlight(color)
        movableTable = this
    }

    fun EmptyTableView.resetMovable() {
        resetHighlight()
        resetMovableWithoutHighlight()
    }

    fun resetMovableWithoutHighlight() {
        movableTable = null
    }

    fun EmptyTableView.isMovable() = movableTable == this
    //endregion

    //region Action State
    fun EmptyTableView.resetActionState() {
        resetMovable()
        resetTabbed()
    }
    fun resetActionStates() {
        movableTable?.resetMovable()
        tabbedTable?.resetTabbed()
    }

    fun EmptyTableView.actionState(): ActionState {
        var state = ActionState()
        if (isTabbed())
            state += ActionState.TABBED
        if (isMovable())
            state += ActionState.MOVABLE
        return state
    }

    class ActionState(var states: Byte = NONE) {
        companion object {
            const val NONE: Byte = 0x00
            const val TABBED: Byte = 0x01
            const val MOVABLE: Byte = 0x02
        }

        val isSingleState = sqrt(states.toFloat()) % 1 == 0f

        //region Operator functions
        operator fun plus(other: ActionState) = ActionState(this.states or other.states)
        operator fun plus(other: Byte) = ActionState(this.states or other)

        operator fun minus(other: ActionState) = ActionState(this.states xor other.states)
        operator fun minus(other: Byte) = ActionState(this.states xor other)

        //endregion
        fun equals(other: Byte?) = states == other

        fun contains(other: ActionState) = contains(other.states)
        fun contains(otherStates: Byte) = states and otherStates > 0
    }
    //endregion
    //endregion
}

class TableDragShadowBuilder(view: View, private val onShadowMetricsProvided: (Point, Point) -> Unit) : View.DragShadowBuilder(view) {

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        (view as? EmptyTableView)?.let {
            outShadowSize.set(it.tableWidth.toInt() + it.horizontalFrame, it.tableHeight.toInt() + it.verticalFrame)
        }

        onShadowMetricsProvided(outShadowSize, outShadowTouchPoint)
    }
}