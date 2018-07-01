package com.zoniklalessimo.seatingplanner

import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.view.View
import com.zoniklalessimo.seatingplanner.tablePlan.EmptyTableView

interface TableScene {
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
        /*clear(table.id)
        connectTable(table.id, sideGuide.id)
        setHorizontalBias(table.id, xBias)
        setVerticalBias(table.id, yBias)*/
    }
    //endregion

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

    fun EmptyTableView.resetTabbed() {
        resetHighlight()
        slideSideOptionsOut()
        tabbedTable = null
    }

    fun EmptyTableView.highlight(color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                             context.getColor(R.color.tableHighlight)
                                         else Color.GREEN,
                                 width: Int) {
        setPadding(width, width, width, width)
        setBackgroundColor(color)
    }

    fun EmptyTableView.resetHighlight() {
        setPadding(0, 0, 0, 0)
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun EmptyTableView.isHighlighted() = paddingLeft < 0
    //endregion scene sideOptions controls
}

class TableDragShadowBuilder(view: View, private val onShadowMetricsProvided: (Point, Point) -> Unit) : View.DragShadowBuilder(view) {

    override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
        super.onProvideShadowMetrics(outShadowSize, outShadowTouchPoint)
        if (outShadowSize != null && outShadowTouchPoint != null) {
            onShadowMetricsProvided(outShadowSize, outShadowTouchPoint)
        }
    }
}