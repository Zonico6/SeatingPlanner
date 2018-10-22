package com.zoniklalessimo.seatingplanner.scene

import android.os.Build
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.View
import com.zoniklalessimo.seatingplanner.R
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.sqrt

interface ActionStateUser {
    fun slideSideOptionsIn()
    fun slideSideOptionsOut()

    //region Tabbed
    var tabbedTable: EmptyTableView?

    fun EmptyTableView.tabbed(@Suppress("DEPRECATION") color: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        context.getColor(R.color.tableTabbedHighlight) else
        resources.getColor(R.color.tableTabbedHighlight)) {
        if (isMovable())
            resetMovableWithoutHighlight()
        tabbedTable?.resetTabbed()
        tabbedTable = this
        highlight(color)
        slideSideOptionsIn()
        setTag(R.id.table_state, ActionState.TABBED)
    }

    fun EmptyTableView.resetTabbed() {
        resetHighlight()
        this.resetTabbedWithoutHighlight()
    }

    fun EmptyTableView.resetTabbedWithoutHighlight() {
        if (this == tabbedTable) {
            setTag(R.id.table_state, ActionState.NONE)
            tabbedTable = null
            slideSideOptionsOut()
        }
    }

    fun resetTabbed() {
        tabbedTable?.resetHighlight()
        resetTabbedWithoutHighlight()
    }

    fun resetTabbedWithoutHighlight() {
        slideSideOptionsOut()
        tabbedTable?.setTag(R.id.table_state, ActionState.NONE)
        tabbedTable = null
    }

    fun EmptyTableView.isTabbed() = if (this == tabbedTable) {
        setTag(R.id.table_state, ActionState.TABBED)
        true
    } else
        false
    //endregion

    //region Movable
    val movableTables: HashSet<View>

    fun EmptyTableView.setMovable(color: Int = context.getColor(R.color.tableMoveHighlight)) {
        if (isTabbed())
            resetTabbedWithoutHighlight()
        highlight(color)
        movableTables.add(this)
        this.setTag(R.id.table_state, ActionState.MOVABLE)
    }

    fun resetMovables() {
        movableTables.forEach {
            val movable = (it as? EmptyTableView) ?: return@forEach
            movable.resetHighlight()
            movable.setTag(R.id.table_state, ActionState.NONE)
        }
        movableTables.clear()
    }

    fun EmptyTableView.resetMovable() {
        resetHighlight()
        resetMovableWithoutHighlight()
    }

    fun EmptyTableView.resetMovableWithoutHighlight() {
        // When making changes here, make sure to check
        // if you need to make those in [resetMovables()] as well
        if (movableTables.contains(this)) {
            setTag(R.id.table_state, ActionState.NONE)
            movableTables.remove(this)
        }
    }

    fun EmptyTableView.isMovable() = if (movableTables.contains(this)) {
        setTag(R.id.table_state, ActionState.MOVABLE)
        true
    } else {
        false
    }
    //endregion

    //region Action State
    fun EmptyTableView.resetActionState() {
        this.resetMovable()
        this.resetTabbed()
    }

    fun resetActionStates(root: ConstraintLayout) {
        resetMovables()
        resetTabbed()
    }

    fun EmptyTableView.actionState(): ActionState {
        var state = ActionState()
        if (isTabbed())
            state += ActionState.TABBED
        if (isMovable())
            state += ActionState.MOVABLE
        return state
    }
    //endregion
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