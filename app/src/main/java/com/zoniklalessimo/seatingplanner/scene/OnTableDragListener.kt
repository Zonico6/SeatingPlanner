package com.zoniklalessimo.seatingplanner.scene

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.iterator
import com.zoniklalessimo.seatingplanner.R

// TODO: Embed settings in android preference api
const val MOVE_TABLE_ON_ROW_BUILT = true
const val MOVE_TABLE_AMOUNT_PERCENT = 50

data class DisplacementInformation(
        val row: MutableList<Pair<EmptyTableView, PointF>>,
        var joinedRect: RectF?,
        var indicatorDisplaced: Boolean,
        var displacedAtSide: Int,
        var insertAtSeparator: Int?
) {
    companion object {
        const val SIDE_NONE: Int = 0x00
        const val SIDE_LEFT: Int = 0x01
        const val SIDE_TOP: Int = 0x02
        const val SIDE_RIGHT: Int = 0x04
        const val SIDE_BOTTOM: Int = 0x08
    }

    constructor() : this(mutableListOf(), null, false, SIDE_NONE, null)

    fun addSide(side: Int): Int {
        displacedAtSide = displacedAtSide or side
        return displacedAtSide
    }
}

interface OnTableDragListener : View.OnDragListener, TableScene {
    val optionsGuide: Guideline
    val constraints: ConstraintSet

    var displaceInfo: DisplacementInformation?


    override fun onDrag(root: View?, event: DragEvent): Boolean {
        // Make sure that root is not null and smart cast it to ConstraintLayout
        @Suppress("CAST_NEVER_SUCCEEDS")
        (root as? ConstraintLayout)?.let { _ ->

            val indicator = event.localState as EmptyTableView
            val table_move_amount = indicator.width.toFloat() * MOVE_TABLE_AMOUNT_PERCENT / 100

            fun displaceIndicator(newX: Float, newY: Float) {
                indicator.x = newX
                indicator.y = newY

                indicator.visibility = View.VISIBLE
                displaceInfo!!.indicatorDisplaced = true
            }

            fun isDisplaced() =
                    if (displaceInfo!!.indicatorDisplaced) {
                        indicator.visibility = View.VISIBLE
                        true
                    } else {
                        indicator.visibility = View.INVISIBLE
                        false
                    }

            fun addToRow(base: RectF, new: EmptyTableView): RectF {
                val oldPos = PointF(new.x, new.y)

                new.y = base.top

                val offset = if (base.centerX() < new.center.x) {
                    displaceInfo!!.row.add(new to oldPos)
                    new.x = base.left + base.width()
                    -table_move_amount
                } else {
                    displaceInfo!!.row.add(0, new to oldPos)
                    new.x = base.left - new.width
                    table_move_amount
                }

                if (MOVE_TABLE_ON_ROW_BUILT && displaceInfo!!.displacedAtSide == DisplacementInformation.SIDE_NONE) {
                    for ((table, _) in displaceInfo!!.row) {
                        table.x += offset
                    }
                }

                base.offset(offset, 0f)
                base.union(new.frame())
                return base
            }

            fun indicateInsert(table: EmptyTableView) {
                val separator = table.closestSeparatorTo(event.x - table.x)
                if (separator == displaceInfo!!.insertAtSeparator)
                    return
                val separatorSpot = table.x + table.priorArea(separator) + table.separatorWidth / 2
                val indicatorX = separatorSpot - indicator.width / 2

                val frame: RectF = table.frame()
                fun intersectionAreaWithOtherTables(): Float {
                    var area = 0f
                    val intersection = RectF()
                    layout@ for (child in root) {
                        if (child.id == indicator.id ||
                                child.id == table.id ||
                                child.getTag(R.id.drag_disabled) == true)
                            continue@layout
                        intersection.setIntersect(frame, child.frame())
                        area += intersection.width() * intersection.height()
                    }
                    return area
                }
                frame.offsetTo(indicatorX, table.y - indicator.height)
                val topOverlap = intersectionAreaWithOtherTables()
                frame.offsetTo(indicatorX, table.y + table.height)
                val bottomOverlap = intersectionAreaWithOtherTables()

                val indicatorY = if (topOverlap <= bottomOverlap)
                    table.y - indicator.height
                else
                    table.y + table.height

                displaceIndicator(indicatorX, indicatorY)
                displaceInfo!!.joinedRect = table.frame()
                displaceInfo!!.insertAtSeparator = separator
            }

            fun resetIndication() {
                for ((table, pos) in displaceInfo!!.row) {
                    table.x = pos.x
                    table.y = pos.y
                }
                displaceInfo!!.row.clear()

                indicator.visibility = View.INVISIBLE
                displaceInfo!!.joinedRect = null
                displaceInfo!!.indicatorDisplaced = false
                displaceInfo!!.insertAtSeparator = null
            }

            when (event.action) {

                DragEvent.ACTION_DRAG_STARTED -> {
                    // Hide original view
                    indicator.visibility = View.INVISIBLE
                    displaceInfo = DisplacementInformation()
                    constraints.modify({
                        prepareConstraintsForDrag(indicator)
                    }, root)
                    return true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    val left = event.x - shadowTouchPoint!!.x
                    val top = event.y - shadowTouchPoint!!.y
                    val right = left + indicator.width
                    val bottom = top + indicator.height
                    val shadowRect = RectF(left, top, right, bottom)

                    fun displaceIfOutside(side: Int) {
                        when (side) {
                            DisplacementInformation.SIDE_RIGHT -> displaceIndicator(optionsGuide.left - indicator.width + 0f, top)
                            DisplacementInformation.SIDE_LEFT -> displaceIndicator(0f, top)
                            DisplacementInformation.SIDE_TOP -> displaceIndicator(left, 0f)
                            DisplacementInformation.SIDE_BOTTOM -> displaceIndicator(left, root.height.toFloat())
                        }
                    }

                    val outsideScreenAtSide = when {
                        right > optionsGuide.left -> DisplacementInformation.SIDE_RIGHT
                        left < 0 -> DisplacementInformation.SIDE_LEFT
                        top < 0 -> DisplacementInformation.SIDE_TOP
                        bottom > root.height -> DisplacementInformation.SIDE_BOTTOM
                        else -> DisplacementInformation.SIDE_NONE
                    }

                    val intersectJoined: Boolean = RectF.intersects(displaceInfo!!.joinedRect
                        ?: RectF(), shadowRect)

                    if (isDisplaced()) {
                        if (displaceInfo!!.displacedAtSide != DisplacementInformation.SIDE_NONE) {
                            displaceIfOutside(outsideScreenAtSide)
                            if (outsideScreenAtSide == DisplacementInformation.SIDE_NONE)
                                resetIndication()
                        } else if (!intersectJoined) {
                            resetIndication()
                            // Update indication of insertion
                        } else if (displaceInfo!!.insertAtSeparator != null) {
                            val adjacent = displaceInfo!!.row.first().first
                            indicateInsert(adjacent)
                            return true
                        } else return true
                    } else {
                        // setup
                        val rowRect = RectF()

                        displaceIfOutside(outsideScreenAtSide)

                        displaceInfo!!.addSide(outsideScreenAtSide)
                        if (outsideScreenAtSide != DisplacementInformation.SIDE_NONE) {
                            with(indicator) {
                                displaceInfo!!.row.add(this to PointF(x, y))
                                rowRect.set(x, y, x + width, y + height)
                            }
                        } else {
                            var intersectArea = 0f
                            var adjacent: View? = null
                            for (child in root) {
                                if (child !is EmptyTableView ||
                                        child.id == indicator.id ||
                                        child.getTag(R.id.drag_disabled) == true)
                                    continue

                                // In order to build a row with the table that is overlapped most, first loop
                                // through every table and then work with the one with the biggest intersection
                                val intersection = RectF()
                                if (intersection.setIntersect(shadowRect, child.frame())) {
                                    val area = intersection.width() * intersection.height()
                                    if (adjacent == null || area > intersectArea) {
                                        intersectArea = area
                                        adjacent = child
                                    }
                                }
                            }
                            // Null if no overlapping table found
                            (adjacent as? EmptyTableView)?.let {
                                // Movable Table
                                if (adjacent.getTag(R.id.table_state) == ActionState.MOVABLE) {
                                    displaceInfo!!.row.add(adjacent to PointF(adjacent.x, adjacent.y))
                                    indicateInsert(adjacent)
                                    return true
                                }

                                // Attaching to the side
                                displaceInfo!!.row.add(indicator to PointF(indicator.x, indicator.y))
                                val newX = if (shadowRect.centerX() < adjacent.center.x) {
                                    displaceInfo!!.row.add(adjacent to PointF(adjacent.x, adjacent.y))
                                    if (MOVE_TABLE_ON_ROW_BUILT)
                                        adjacent.x += table_move_amount
                                    adjacent.x - indicator.width
                                } else {
                                    displaceInfo!!.row.add(0, adjacent to PointF(adjacent.x, adjacent.y))
                                    if (MOVE_TABLE_ON_ROW_BUILT)
                                        adjacent.x -= table_move_amount
                                    adjacent.x + adjacent.width
                                }
                                displaceIndicator(newX, adjacent.y)
                                rowRect.set(adjacent.frame())
                                rowRect.union(indicator.frame())
                            } ?: return true
                        }

                        // loop
                        var finished = false
                        while (!finished) {
                            for (child in root) {
                                if (child !is EmptyTableView ||
                                        // If the child is the one table from setup, skip it
                                        displaceInfo!!.row.find { it.first == child } != null ||
                                        child.getTag(R.id.drag_disabled) == true) continue

                                if (RectF.intersects(rowRect, child.frame())) {
                                    rowRect.set(
                                            addToRow(rowRect, child))
                                    continue
                                }
                            }
                            finished = true
                        }
                        displaceInfo!!.joinedRect = rowRect
                    }
                    return true
                }

                DragEvent.ACTION_DROP -> {
                    // Movable insert
                    if (displaceInfo!!.insertAtSeparator != null) {
                        val inv = displaceInfo!!.row.first()
                        indicator.x = inv.second.x - indicator.width / 2
                        indicator.y = inv.second.y

                        val table = inv.first as? EmptyTableView
                            ?: return false
                        table.insertTable(table.closestSeparatorTo(indicator.center.x), indicator)
                        indicator.set(table)
                        root.removeView(table)
                    } else if (isDisplaced()) {
                        // Set xBias to start of row
                        indicator.x = displaceInfo!!.row.first().first.x

                        val viewsInRowThatHaveToBeRemovedAfterwards = mutableListOf<View>()

                        // Involved list resembles the order of the tables, so remember if you have passed
                        // Indicator and need to appending the tables instead of inserting at the start
                        var coveredInd = false
                        for ((view, _) in displaceInfo!!.row) {
                            if (view == indicator) {
                                coveredInd = true
                                continue

                            } else if (coveredInd) {
                                indicator.append(view as Table)
                            } else {
                                indicator.insertAtStart(view as Table)
                            }
                            viewsInRowThatHaveToBeRemovedAfterwards.add(view)
                        }
                        viewsInRowThatHaveToBeRemovedAfterwards.forEach {
                            root.removeView(it)
                        }
                    } else {
                        indicator.x = event.x - shadowTouchPoint!!.x
                        indicator.y = event.y - shadowTouchPoint!!.y
                    }
                    return true
                }

                DragEvent.ACTION_DRAG_ENTERED,
                DragEvent.ACTION_DRAG_EXITED -> {
                    // returns true; the value is ignored.
                    return true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    // Reset the previous positions of the tables. If the ids are not there, that means that it was fused to one row
                    shadowTouchPoint = null
                    indicator.visibility = View.VISIBLE

                    val params = indicator.layoutParams
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    indicator.layoutParams = params

                    constraints.modify({
                        restoreBiases(indicator, optionsGuide, root.height.toFloat())
                    }, root)
                    // Synchronize xBias and yBias with layout position
                    indicator.translationX = 0f
                    indicator.translationY = 0f

                    displaceInfo = null

                    while (true) {
                        val once = root.findViewWithTag<EmptyTableView>("drag_disabled_once")
                            ?: break
                        once.tag = null
                        once.setTag(R.id.drag_disabled, false)
                    }

                }
                // An unknown action type was received.
                else -> Log.e("DragDrop Example", "Unknown action type received by OnDragListener.")
            }
            return false
        } ?: return false
    }
}
