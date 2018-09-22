package com.zoniklalessimo.seatingplanner.scene

import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline

interface TablePlacer {
    //region positioning
    fun ConstraintSet.connectTable(tableId: Int, guideId: Int, xBias: Float = 0.5f, yBias: Float = 0.5f) {
        center(tableId, ConstraintSet.PARENT_ID, ConstraintSet.START, 0,
                guideId, ConstraintSet.START, 0, xBias)
        center(tableId, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0, yBias)
    }

    /**
     * Calculate the appropriate bias value
     *
     * @param pos The table's position. If this is positive, it is treated, as-is, as the bias, if it's
     * negative, the real bias is calculated and returned
     * @param length The length of the table
     * @param size The length or height of the container
     */
    fun makeBias(pos: Float, length: Float, size: Float) = if (pos < 0)
    // Don't use length and height values because those may not be calculated yet
    // if the table comes from a movable drag
        getBias(pos, length, size)
    else
        pos

    fun getBias(pos: Float, length: Float, size: Float) = pos / (size - length)

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
    }
    //endregion
}