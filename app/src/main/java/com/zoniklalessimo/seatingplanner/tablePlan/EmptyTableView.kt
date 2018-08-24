package com.zoniklalessimo.seatingplanner.tablePlan

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.zoniklalessimo.seatingplanner.*
import java.util.*

open class EmptyTableView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
        View(context, attrs, defStyleAttr, defStyleRes), Table {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, 0)

    constructor(context: Context) :
            this(context, null)

    companion object {
        const val LOG_TAG = "EmptyTableView"
    }

    // region Attributes

    //region Seat attributes
    private var seatCountChanged = false
    final override var seatCount: Int = 1
        set(value) {
            seatCountChanged = true
            field = value
            invalidate()
            requestLayout()
        }
    var seatWidth: Float = -1f
        set(value) {
            field = value
            invalidate()
            requestLayout()
        }
    var seatHeight: Float = -1f
        set(value) {
            field = value
            invalidate()
            requestLayout()
        }
    var seatColor: Int
        get() = seatPaint.color
        set(color) {
            seatPaint.color = color
            invalidate()
        }
    //endregion

    //region Table dimensions
    var cornerRadius = 30f
        set(value) {
            field = value
            invalidate()
        }
    // The table's width without border and padding
    val tableWidth get(): Float = seatWidth * seatCount + partitionSpace
    // The table's height without border and padding
    val tableHeight get() = seatHeight

    //endregion

    //region Divider
    var dividerColor: Int
        get() = dividerPaint.color
        set(color) {
            dividerPaint.color = color
            invalidate()
        }
    var dividerWidth: Float
        get() = dividerPaint.strokeWidth
        set(width) {
            dividerPaint.strokeWidth = width
            invalidate()
            requestLayout()
        }
    //endregion

    //region Separators
    /**
     * The separators that separate several tables within one [EmptyTableView]. A separator is represented by
     * an Integer, which dictates the seat that the separator is displayed behind.
     */
    override var separators: SortedSet<Int> = sortedSetOf()
        set(separators) {
            val oldSize = field.size
            field = separators
            invalidate()
            if (separatorWidth != dividerWidth && oldSize != separators.size)
                requestLayout()
        }

    var separatorColor: Int
        get() = separatorPaint.color
        set(color) {
            separatorPaint.color = color
            invalidate()
        }
    var separatorWidth: Float
        get() = separatorPaint.strokeWidth
        set(width) {
            separatorPaint.strokeWidth = width
            invalidate()
            requestLayout()
        }
    //endregion

    //region Table border
    /**
     * The color of the border.
     * For retrieving the displayed color, after applying the highlight, call displayedBorderColor()
     */
    var borderColor: Int = Color.GRAY
        set(color) {
            field = color
            updateBorderPaintColor()
        }
    var borderSize: Float
        get() = borderPaint.strokeWidth
        set(width) {
            borderPaint.strokeWidth = width
            invalidate()
            requestLayout()
        }
    //endregion

    //region Highlighting
    var highlightColor: Int? = null
        private set(value) {
            field = value
            updateBorderPaintColor()
        }

    fun highlight(color: Int) {
        highlightColor = color
    }

    fun resetHighlight() {
        highlightColor = null
    }

    val isHighlighted
        get() = highlightColor != null
    //endregion

    //region Miscellaneous
    private val partitionSpace: Float
        get(): Float {
            val sepCount = separators.size
            return sepCount * separatorWidth + dividerWidth * (seatCount - sepCount - 1)
        }

    val horizontalFrame: Int
        get() = paddingRight + paddingLeft + borderSize.toInt() * 2
    val verticalFrame: Int
        get() = paddingTop + paddingBottom + borderSize.toInt() * 2
    //endregion

    //endregion Attributes

    //region paints

    private var borderPaint = Paint()
    private var seatPaint: Paint = Paint()
    private var dividerPaint: Paint = Paint()
    private var separatorPaint: Paint = Paint()

    private fun updateBorderPaintColor() {
        val oldColor = borderPaint.color
        borderPaint.color = highlightColor ?: borderColor
        // (borderColor + highlightColor) / 2
        // (sqrt((borderColor * borderColor + highlightColor * highlightColor) * 0.5)).roundToInt()
        if (oldColor != borderPaint.color)
            invalidate()
    }

    //endregion paints

    //region shape

    private lateinit var tableBorderRect: RectF
    private lateinit var tableRect: RectF

    //endregion shapes

    init {
        val a: TypedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.EmptyTableView, defStyleAttr, defStyleRes)

        try {
            seatCount = a.getInteger(R.styleable.EmptyTableView_seatCount, seatCount)
            seatWidth = a.getDimension(R.styleable.EmptyTableView_seatWidth, seatWidth)
            seatHeight = a.getDimension(R.styleable.EmptyTableView_seatHeight, seatHeight)
            seatColor = a.getInt(R.styleable.EmptyTableView_seatColor, Color.DKGRAY)

            dividerColor = a.getColor(R.styleable.EmptyTableView_dividerColor, Color.GRAY)
            dividerWidth = a.getDimension(R.styleable.EmptyTableView_dividerWidth, 4f)

            separatorColor = a.getColor(R.styleable.EmptyTableView_separatorColor, Color.BLACK)
            separatorWidth = a.getDimension(R.styleable.EmptyTableView_separatorWidth, 0f)

            assignSeparators(a.getString(R.styleable.EmptyTableView_separators) ?: "")

            borderColor = a.getColor(R.styleable.EmptyTableView_borderColor, borderColor)
            borderSize = a.getDimension(R.styleable.EmptyTableView_borderWidth, 0f)

            cornerRadius = a.getDimension(R.styleable.EmptyTableView_cornerRadius, cornerRadius)
        } finally {
            a.recycle()
        }

        // Paints
        dividerPaint.strokeWidth = dividerWidth
        dividerPaint.color = dividerColor

        seatPaint.color = seatColor

        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = borderColor
    }

    fun set(value: Table) {
        seatCount = value.seatCount
        separators = value.separators
    }

    override fun postInvalidate() {
        super.postInvalidate()

        updateShapes()
    }

    //region Measuring
    private fun desiredSize(measureSpec: Int, size: Int): Int {
        val specSize = MeasureSpec.getSize(measureSpec)
        val specMode = MeasureSpec.getMode(measureSpec)

        if (specSize == 0) {
            if (size == -1) {
                Log.e(LOG_TAG,
                        "MeasureSpec->size and seatHeight both have placeholder values, can't determine size.")
            }
            return size
        }

        if (specMode == MeasureSpec.AT_MOST) {
            if (size == -1) {
                Log.w(LOG_TAG,
                        "MeasureSpec was \'AT_MOST\'. Since size was not set, " +
                                "\'EXACTLY\' was used instead.")
            } else {
                return Math.min(size, specSize)
            }
        }
        return specSize
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (!changed && seatCountChanged) {
            seatWidth = (right - left - horizontalFrame - partitionSpace) / seatCount
        }
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = desiredSize(widthMeasureSpec, if (tableWidth.toInt() == -1) -1 else tableWidth.toInt() + horizontalFrame)
        val desiredHeight = desiredSize(heightMeasureSpec, if (tableHeight.toInt() == -1) -1 else tableHeight.toInt() + verticalFrame)

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        seatWidth = (w - horizontalFrame - partitionSpace) / seatCount
        seatHeight = h - verticalFrame + 0.0f

        updateShapes()
    }

    private fun updateShapes() {
        tableBorderRect = RectF(
                paddingLeft.toFloat() + borderSize / 2,
                paddingTop.toFloat() + borderSize / 2,
                paddingLeft + tableWidth + borderSize * 1.5f,
                paddingTop + tableHeight + borderSize * 1.5f)

        val x2 = paddingLeft + borderSize
        val y2 = paddingTop + borderSize
        tableRect = RectF(x2, y2, x2 + tableWidth, y2 + tableHeight)
    }
    //endregion

    //region Drawing
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return

        canvas.drawRect(tableRect, seatPaint)
        canvas.drawRoundRect(tableBorderRect, cornerRadius, cornerRadius, borderPaint)

        for (i in 1 until seatCount) {
            if (separators.contains(i))
                canvas.drawSeparator(i)
            else
                canvas.drawDivider(i)
        }
    }

    /**
     * The area on the table that is left from the given position.
     *
     * @param index The position in form of the index of the partition.
     * @return The length of area left from the index.
     */
    fun priorArea(index: Int): Float {
        var seps = 0
        for (i in separators) {
            if (i < index)
                seps += 1
            else
                break
        }
        return seatWidth * index + separatorWidth * seps + dividerWidth * (index - seps)
    }

    private fun Canvas.drawDivider(index: Int) =
            drawPartition(priorArea(index), dividerWidth, dividerPaint)

    private fun Canvas.drawSeparator(index: Int) =
            drawPartition(priorArea(index), separatorWidth, separatorPaint)

    private fun Canvas.drawPartition(priorWidth: Float, width: Float, paint: Paint) {
        val x = paddingLeft + borderSize + priorWidth
        val y = paddingTop.toFloat() + borderSize
        drawRect(x, y, x + width, y + seatHeight, paint)
    }
    //endregion

    //endregion
    /**
     * Reduce the table to the snippet between the two seats.
     *
     * @param start The partition position that's gonna be the start of the new table
     * @param end The partition position that's gonna be the end of the new table
     */
    fun cut(start: Int, end: Int) {
        touchedSeat = Math.max(touchedSeat - start, -1)

        separators = separators.mapNotNull {
            if (it <= start || it >= end)
                null
            else {
                it - start
            }
        }.toSortedSet()

        seatCount = end - start
    }

    var touchedSeat = -1
        private set

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchedSeat = when (event.action) {
            MotionEvent.ACTION_DOWN -> seatAt(event.x)
            MotionEvent.ACTION_UP -> -1
            else -> touchedSeat
        }
        return super.onTouchEvent(event)
    }
}

fun EmptyTableView.cut(ends: Pair<Int, Int>) = cut(ends.first, ends.second)

/**
 * Get the seat at the specified position
 *
 * @param x The relative position on the view
 * @return The index of the corresponding seat
 */
fun EmptyTableView.seatAt(x: Float) = (x / width * seatCount).toInt()

/**
 * The closest separator to the specified position
 *
 * @param x The relative position on the table
 * @return The corresponding separator
 */
fun EmptyTableView.closestSeparatorTo(x: Float): Int {
    val seat = seatAt(x)
    val before = separatorBeforeSeat(seat)
    val after = separatorAfterSeat(seat)

    val priorBefore = priorArea(before) + separatorWidth
    val priorAfter = priorArea(after)

    return if (x - priorBefore < priorAfter - x)
        before
    else
        after
}

fun EmptyTableView.addSeparator(pos: Int): Boolean {
    val ret = separators.add(pos)
    invalidate()
    requestLayout()
    return ret
}

fun EmptyTableView.removeSeparator(pos: Int): Boolean {
    val ret = separators.remove(pos)
    invalidate()
    requestLayout()
    return ret
}

fun EmptyTableView.containsSeparator(separator: Int) = separators.contains(separator)

fun EmptyTableView.clearSeparators() {
    separators.clear()
    invalidate()
    requestLayout()
}

fun EmptyTableView.closestPartitionTo(x: Float) = Math.round((x / width * seatCount))