package com.zoniklalessimo.seatingplanner.scene

import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import android.util.Log

interface TablePlacer {
    companion object {
        const val LOG_TAG = "TablePlacer"
    }

    // Make sure the biases are within range
    private fun checkBias(bias: Float): Float =
            when {
                bias < 0 -> {
                    Log.e(LOG_TAG, "Bias was less than 0: $bias")
                    0.001f
                }
                bias > 1 -> {
                    Log.e(LOG_TAG, "Bias was more than 1: $bias")
                    1f
                }
                else -> bias
            }

    //region positioning
    fun ConstraintSet.connectTable(tableId: Int, guideId: Int, xBias: Float = 0.5f, yBias: Float = 0.5f) {
        val biasX = checkBias(xBias)
        val biasY = checkBias(yBias)

        center(tableId, ConstraintSet.PARENT_ID, ConstraintSet.START, 0,
                guideId, ConstraintSet.START, 0, biasX)
        center(tableId, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0, biasY)
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
        getBias(-pos, length, size)
    else
        pos

    fun getBias(pos: Float, length: Float, size: Float) = pos / (size - length)

    fun ConstraintSet.prepareConstraintsForDrag(table: EmptyTableView) {
        clear(table.id)
        connect(table.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, table.left)
        connect(table.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, table.top)
    }

    fun ConstraintSet.restoreBiases(table: EmptyTableView, sideGuide: Guideline, height: Float) {
        val xBias = getBias(table.x, table.width + 0f, sideGuide.left + 0f)
        val yBias = getBias(table.y, table.height.toFloat(), height)

        connectTable(table.id, sideGuide.id, xBias, yBias)
    }
    //endregion
}