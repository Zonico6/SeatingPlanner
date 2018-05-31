package com.zoniklalessimo.seatingplanner

import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.support.constraint.ConstraintSet
import android.util.Log
import android.view.View
import com.zoniklalessimo.seatingplanner.tablePlan.EmptyTableView

interface TableScene {
    //region helpers
    fun ConstraintSet.connectTable(tableId: Int, guideId: Int) {
        connect(tableId, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        connect(tableId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(tableId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(tableId, ConstraintSet.RIGHT, guideId, ConstraintSet.LEFT)
    }

    var shadowTouchPoint: Point?

    fun View.startTableDrag() {
        val dragShadow = TableDragShadowBuilder(this) { _, touchPoint ->
            shadowTouchPoint = touchPoint
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startDragAndDrop(null, dragShadow, this, 0)
        } else {
            startDrag(null, dragShadow, this, 0)
        }
    }
    //endregion helpers

    //region scene sideOptions controls
    fun slideSideOptionsIn()
    fun slideSideOptionsOut()

    var tabbedTable: EmptyTableView?

    fun EmptyTableView.tabbed(color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                            context.getColor(R.color.tableHighlight)
                                        else Color.GREEN,
                              width: Int = 12) {
        tabbedTable = this
        highlight(color, width)
        slideSideOptionsIn()
    }

    fun EmptyTableView.highlight(color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                             context.getColor(R.color.tableHighlight)
                                         else Color.GREEN,
                                 width: Int) {
        setPadding(width, width, width, width)
        setBackgroundColor(color)
    }

    fun EmptyTableView.resetTabbed() {
        resetHighlight()
        slideSideOptionsOut()
        tabbedTable = null
    }

    fun EmptyTableView.resetHighlight() {
        setPadding(0, 0, 0, 0)
        setBackgroundColor(Color.TRANSPARENT)
    }
    //endregion scene sideOptions controls
}

class TableDragShadowBuilder(view: View, val onShadowMetricsProvided: (Point, Point) -> Unit) : View.DragShadowBuilder(view) {
    /* lateinit var shadowTouchPoint: Point
        private set
    lateinit var shadowSize: Point
        private set */

    override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
        super.onProvideShadowMetrics(outShadowSize, outShadowTouchPoint)
        if (outShadowSize != null && outShadowTouchPoint != null) {
            Log.d("TableDragShadowBuilder:", "Called onProvideShadowMetrics.")
            onShadowMetricsProvided(outShadowSize, outShadowTouchPoint)
        }
        /* if (outShadowSize != null) {
            shadowSize = outShadowSize
        }
        if (outShadowTouchPoint != null) {
            shadowTouchPoint = outShadowTouchPoint
        }*/
    }
}