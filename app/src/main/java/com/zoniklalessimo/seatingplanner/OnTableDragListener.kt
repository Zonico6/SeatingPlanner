package com.zoniklalessimo.seatingplanner

import android.graphics.PointF
import android.graphics.RectF
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.core.view.iterator
import com.zoniklalessimo.seatingplanner.tablePlan.EmptyTableView
import com.zoniklalessimo.seatingplanner.tablePlan.closestSeparatorTo

// TODO: Embed settings in android preference api
const val MOVE_TABLE_ON_ROW_BUILT = true
const val MOVE_TABLE_AMOUNT_PERCENT = 50

data class DisplacementInformation(
        val involved: MutableList<Pair<Int, PointF>>,
        var joinedRect: RectF?,
        var indicatorDisplaced: Boolean,
        var displacedAtSideOptions: Boolean,
        var displacedAtSeparator: Int?
) {
    constructor() : this(mutableListOf(), null, false, false, null)
}

interface OnTableDragListener : View.OnDragListener, TableScene {
    val optionsGuide: Guideline
    val constraints: ConstraintSet

    val sideOptionsPresent: Boolean?
        get() = null

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

            fun addToRow(base: RectF, new: View): RectF {
                val oldPos = PointF(new.x, new.y)

                new.y = base.top

                val offset = if (base.centerX() < new.center.x) {
                    displaceInfo!!.involved.add(new.id to oldPos)
                    new.x = base.left + base.width()
                    -table_move_amount
                } else {
                    displaceInfo!!.involved.add(0, new.id to oldPos)
                    new.x = base.left - new.width
                    table_move_amount
                }

                if (MOVE_TABLE_ON_ROW_BUILT && !displaceInfo!!.displacedAtSideOptions) {
                    for ((id, _) in displaceInfo!!.involved) {
                        root.getViewById(id).x += offset
                    }
                }

                base.offset(offset, 0f)
                base.union(new.frame())
                return base
            }

            fun indicateInsert(table: EmptyTableView) {
                val separator = table.closestSeparatorTo(event.x - table.x)
                if (separator == displaceInfo!!.displacedAtSeparator)
                    return
                val separatorSpot = table.x + table.priorArea(separator) + table.separatorWidth / 2
                val indicatorX = separatorSpot - indicator.width / 2

                val frame: RectF = table.frame()
                fun intersectionAreaWithOtherTables(): Float {
                    var area = 0f
                    val intersection = RectF()
                    layout@ for (child in root) {
                        when (indicator.id) { child.id, table.id -> continue@layout
                        }
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
                displaceInfo!!.displacedAtSeparator = separator
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

                    val intersectJoined: Boolean = RectF.intersects(displaceInfo!!.joinedRect
                        ?: RectF(), shadowRect)

                    val intersectIndicator: Boolean by lazy {
                        RectF.intersects(indicator.frame(), shadowRect)
                    }

                    if (displaceInfo!!.displacedAtSeparator != null) {
                        if (intersectJoined || intersectIndicator) {
                            val adjacent = root.getViewById(displaceInfo!!.involved.first().first)
                            if (adjacent is EmptyTableView) {
                                indicateInsert(adjacent)
                            }
                            return true
                        }
                    }
                    if (isDisplaced()) {
                        if (!intersectJoined &&
                                // If displaced at a movable table, indicator is not included in joinedRect
                                !(displaceInfo!!.displacedAtSeparator != null && intersectIndicator)) {
                            for ((id, pos) in displaceInfo!!.involved) {
                                val view = root.getViewById(id)
                                view.x = pos.x
                                view.y = pos.y

                            }
                            displaceInfo!!.involved.clear()

                            indicator.visibility = View.INVISIBLE
                            displaceInfo!!.joinedRect = null
                            displaceInfo!!.indicatorDisplaced = false
                            displaceInfo!!.displacedAtSeparator = null
                        } else return true
                    } else {
                        // setup
                        val rowRect = RectF()

                        val optionsGap = optionsGuide.left - event.x - shadowTouchPoint!!.x
                        if (optionsGap < 0) {
                            displaceInfo!!.displacedAtSideOptions = true
                            displaceIndicator(optionsGuide.left - indicator.width + 0f, top)
                            rowRect.set(indicator.x, top, indicator.x + indicator.width, bottom)
                            displaceInfo!!.involved.add(indicator.id to PointF(indicator.x, indicator.y))
                        } else {
                            var intersectArea = 0f
                            var adjacent: View? = null
                            for (child in root) {
                                if (child !is EmptyTableView ||
                                        child.id == indicator.id ||
                                        child.getTag(R.id.drag_disabled) == true)
                                    continue

                                // In order toBuild a row with the table that is overlapped most, first loop
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
                                    displaceInfo!!.involved.add(adjacent.id to PointF(adjacent.x, adjacent.y))
                                    indicateInsert(adjacent)
                                    return true
                                }

                                // Attaching to the side
                                displaceInfo!!.involved.add(indicator.id to PointF(indicator.x, indicator.y))
                                val newX = if (shadowRect.centerX() < adjacent.center.x) {
                                    displaceInfo!!.involved.add(adjacent.id to PointF(adjacent.x, adjacent.y))
                                    if (MOVE_TABLE_ON_ROW_BUILT)
                                        adjacent.x += table_move_amount
                                    adjacent.x - indicator.width
                                } else {
                                    displaceInfo!!.involved.add(0, adjacent.id to PointF(adjacent.x, adjacent.y))
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
                                        displaceInfo!!.involved.find { it.first == child.id } != null ||
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
                    if (displaceInfo!!.displacedAtSeparator != null) {
                        val inv = displaceInfo!!.involved.first()
                        indicator.x = inv.second.x - indicator.width / 2
                        indicator.y = inv.second.y

                        val table = root.getViewById(inv.first) as? EmptyTableView ?: return false
                        table.insertTable(table.closestSeparatorTo(indicator.center.x), indicator)
                        indicator.set(table)
                        root.removeView(table)
                    } else if (isDisplaced()) {
                        // Set x to start of row
                        indicator.x = root.getViewById(displaceInfo!!.involved.first().first).x

                        val viewsInRowThatHaveToBeRemovedAfterwards = mutableListOf<View>()

                        // Involved list resembles the order of the tables, so remember if you have passed
                        // Indicator and need to appending the tables instead of inserting at the start
                        var coveredInd = false
                        for ((id, _) in displaceInfo!!.involved) {
                            val view = root.getViewById(id)
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

                    constraints.modify({
                        restoreBiases(indicator, optionsGuide, root.height.toFloat())
                    }, root)
                    // Synchronize x and y with layout position
                    indicator.translationX = 0f
                    indicator.translationY = 0f

                    displaceInfo = null

                    for (child in root) {
                        child.setTag(R.id.drag_disabled, false)
                    }
                }
                // An unknown action type was received.
                else -> Log.e("DragDrop Example", "Unknown action type received by OnDragListener.")
            }
            return false
        } ?: return false
    }
}
