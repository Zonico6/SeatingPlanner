package com.zoniklalessimo.seatingplanner

import android.graphics.PointF
import android.graphics.RectF
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.constraint.Guideline
import android.util.Log
import android.view.DragEvent
import android.view.View
import com.zoniklalessimo.seatingplanner.tablePlan.EmptyTableView

// TODO: Embed setting in android preference api
const val MOVE_TABLE_ON_ROW_BUILT = true

data class DisplacementInformation(
        val involved: MutableList<Pair<Int, PointF>>,
        var joinedRect: RectF?,
        var indicatorDisplaced: Boolean
) {
    constructor() : this(mutableListOf(), null, false)
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
                    -new.width / 2f
                } else {
                    displaceInfo!!.involved.add(0, new.id to oldPos)
                    new.x = base.left - new.width
                    new.width / 2f
                }

                if (MOVE_TABLE_ON_ROW_BUILT) {
                    for ((id, _) in displaceInfo!!.involved) {
                        root.getViewById(id).x += offset
                    }
                }

                base.offset(offset, 0f)
                base.union(new.frame)
                return base
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

                    if (isDisplaced()) {
                        if (!displaceInfo!!.joinedRect!!.intersects(left, top, right, bottom)) {
                            for ((id, pos) in displaceInfo!!.involved) {
                                val view = root.getViewById(id)
                                view.x = pos.x
                                view.y = pos.y

                            }
                            displaceInfo!!.involved.clear()

                            indicator.visibility = View.INVISIBLE
                            displaceInfo!!.joinedRect = null
                            displaceInfo!!.indicatorDisplaced = false
                        } else return true
                    } else {
                        // setup
                        val rowRect = RectF()

                        val optionsGap = optionsGuide.left - event.x - shadowTouchPoint!!.x
                        if (optionsGap < 0) {
                            displaceIndicator(optionsGuide.left - indicator.width + 0f, top)
                            rowRect.set(indicator.x, top, indicator.x + indicator.width, bottom)
                            displaceInfo!!.involved.add(indicator.id to PointF(indicator.x, indicator.y))
                        } else {
                            var intersectArea = 0f
                            var adjacent: View? = null
                            // TODO: Use Android-KTX to cycle through children
                            for (i in 0 until root.childCount) {
                                val child = root.getChildAt(i)
                                if (child as? EmptyTableView == null || child.id == indicator.id) continue

                                // In order toBuild a row with the table that is overlapped most, first loop
                                // through every table and then work with the one with the biggest intersection
                                val intersection = RectF()
                                if (intersection.setIntersect(shadowRect, child.frame)) {
                                    val area = intersection.width() * intersection.height()
                                    if (adjacent == null || area > intersectArea) {
                                        intersectArea = area
                                        adjacent = child
                                    }
                                }
                            }
                            // Null if no overlapping table found
                            adjacent?.let {
                                displaceInfo!!.involved.add(indicator.id to PointF(indicator.x, indicator.y))
                                val newX = if (shadowRect.centerX() < adjacent.center.x) {
                                    displaceInfo!!.involved.add(adjacent.id to PointF(adjacent.x, adjacent.y))
                                    if (MOVE_TABLE_ON_ROW_BUILT)
                                        adjacent.x += indicator.width / 2
                                    adjacent.x - indicator.width
                                } else {
                                    displaceInfo!!.involved.add(0, adjacent.id to PointF(adjacent.x, adjacent.y))
                                    if (MOVE_TABLE_ON_ROW_BUILT)
                                        adjacent.x -= indicator.width / 2
                                    adjacent.x + adjacent.width
                                }
                                displaceIndicator(newX, adjacent.y)
                                rowRect.set(adjacent.frame)
                                rowRect.union(indicator.frame)
                            } ?: return true
                        }
                        // loop
                        var finished = false
                        while (!finished) {
                            // TODO: Use Android-KTX to cycle through children. And use functional methods like filter!!
                            for (i in 0 until root.childCount) {
                                // TODO: Handle what happens if row is at sideOptions,
                                // TODO: so it cannot be moved any more in that direction
                                val child = root.getChildAt(i)
                                if (child as? EmptyTableView == null ||
                                        // If the child is the one other table from setup, skip it
                                        displaceInfo!!.involved.find { it.first == child.id } != null) continue

                                if (RectF.intersects(rowRect, child.frame)) {
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
                    if (isDisplaced()) {
                        // Set x to start of row
                        indicator.x = root.getViewById(displaceInfo!!.involved.first().first).x

                        val viewsInRowThatHaveToBeRemovedAfterwards = mutableListOf<View>()

                        // Involved list resembles the order of the tables so remember if you have passed
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
                }
            // An unknown action type was received.
                else -> Log.e("DragDrop Example", "Unknown action type received by OnDragListener.")
            }
            return false
        } ?: return false
    }
}
































