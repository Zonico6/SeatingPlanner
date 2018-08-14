@file:Suppress("DEPRECATION")

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
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.sqrt

interface TableScene {
    companion object {
        const val LOG_TAG = "TableScene: "
    }

    //region helpers
    fun ConstraintSet.connectTable(tableId: Int, guideId: Int) {
        connect(tableId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        connect(tableId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        connect(tableId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        connect(tableId, ConstraintSet.END, guideId, ConstraintSet.START, 0)
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

    fun spawnTable(root: ViewGroup, inflater: LayoutInflater): Int {
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
        table.setOnLongClickListener {
            table.startTableDrag()
            true
        }
        root.addView(table)
        return table.id
    }

    fun ConstraintSet.modify(modifications: ConstraintSet.() -> Unit, layout: ConstraintLayout) {
        clone(layout)
        modifications()
        applyTo(layout)
    }

    /**
     * The bounding rectangle of this view.
     */
    val View.frame
        get() = RectF(x, y, x + width, y + height)

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
    fun ConstraintSet.prepareConstraintsForDrag(table: View) {
        clear(table.id)
        connect(table.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, table.left)
        connect(table.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, table.top)
    }

    fun ConstraintSet.restoreBiases(table: View, sideGuide: Guideline, height: Float) {
        val xBias = table.x / (sideGuide.left - table.width).toFloat()
        val yBias = table.y / (height - table.height)

        center(table.id, ConstraintSet.PARENT_ID, ConstraintSet.START, 0,
                sideGuide.id, ConstraintSet.START, 0, xBias)
        center(table.id, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0, yBias)

        // Prevent view from inflating to screen size
        constrainWidth(table.id, table.measuredWidth)
        constrainHeight(table.id, table.measuredHeight)
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

    fun EmptyTableView.setMovable(color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
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

        fun contains(other: ActionState) = contains(other.states)
        fun contains(otherStates: Byte) = states and otherStates > 0
    }
    //endregion
    //endregion
}

class TableDragShadowBuilder(view: View, private val onShadowMetricsProvided: (Point, Point) -> Unit) : View.DragShadowBuilder(view) {

    override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
        super.onProvideShadowMetrics(outShadowSize, outShadowTouchPoint)
        if (outShadowSize != null && outShadowTouchPoint != null) {
            onShadowMetricsProvided(outShadowSize, outShadowTouchPoint)
        }
    }
}