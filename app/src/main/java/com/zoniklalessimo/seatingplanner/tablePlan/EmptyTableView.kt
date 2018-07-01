package com.zoniklalessimo.seatingplanner.tablePlan

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.zoniklalessimo.seatingplanner.R
import java.util.*

open class EmptyTableView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
        View(context, attrs, defStyleAttr, defStyleRes) {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, 0)
    constructor(context: Context) :
            this(context, null)

    companion object {
        const val LOG_TAG = "EmptyTableView"
    }
    // TODO: Add seatColors[], whose colors get cycled through when drawing the seats
    // region Attributes

    //region Seat attributes
    var seatCount: Int = 1
        set(value) {
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
    var tableCornerRadius = 30f
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
    var separators: SortedSet<Int> = sortedSetOf()
        set(seps) {
            val oldSize = field.size
            field = seps
            invalidate()
            if (separatorWidth != dividerWidth && oldSize != seps.size)
                requestLayout()
        }
    fun separatorString(concatenate: String): String {
        val builder = StringBuilder((separators.size - 1) * concatenate.length + 1)
        for (i in separators) {
            builder.append(concatenate).append(i)
        }
        builder.deleteCharAt(0)
        return builder.toString()
    }
    fun separatorString(concatenate: Char = ','): String {
        if (separators.isEmpty()) return String()

        val builder = StringBuilder(separators.size * 2 - 1)
        for (i in separators) {
            builder.append(concatenate).append(i)
        }

        builder.deleteCharAt(0)
        return builder.toString()
    }

    fun assignSeparators(sepsStr: CharSequence) {
        var currentNumber = StringBuilder(2)
        for (i in sepsStr) {
            if (i.isDigit()) {
                currentNumber.append(i)
            } else if (currentNumber.isNotEmpty()) {
                separators.add(currentNumber.toString().toInt())
                currentNumber = StringBuilder(2)
            }
        }
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
    var tableBorderColor: Int
        get() = tableBorderPaint.color
        set(color) {
            tableBorderPaint.color = color
            invalidate()
        }
    var borderSize: Float
        get() = tableBorderPaint.strokeWidth
        set(width) {
            tableBorderPaint.strokeWidth = width
            invalidate()
            requestLayout()
        }
    //endregion

    //region Miscellaneous
    private val partitionSpace: Float
        get(): Float {
            val sepCount = separators.size
            return sepCount * separatorWidth + dividerWidth * (seatCount - sepCount - 1)
        }

    private val horizontalFrame: Int
        get() = paddingRight + paddingLeft + borderSize.toInt() * 2
    private val verticalFrame: Int
        get() = paddingTop + paddingBottom + borderSize.toInt() * 2
    //endregion

    //endregion Attributes

    //region paints

    private var tableBorderPaint = Paint()
    private var seatPaint: Paint = Paint()
    private var dividerPaint: Paint = Paint()
    private var separatorPaint: Paint = Paint()

    //endregion paints

    //region shapes

    private lateinit var tableBorderRect: RectF
    private lateinit var tableRect: RectF

    //endregion shapes

    init {
        val a: TypedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.EmptyTableView, defStyleAttr, defStyleRes)

        try {
            seatCount = a.getInteger(R.styleable.EmptyTableView_seatCount,          seatCount)
            seatWidth = a.getDimension(R.styleable.EmptyTableView_seatWidth,        seatWidth)
            seatHeight = a.getDimension(R.styleable.EmptyTableView_seatHeight,      seatHeight)
            seatColor = a.getInt(R.styleable.EmptyTableView_seatColor,              Color.DKGRAY)

            dividerColor = a.getColor(R.styleable.EmptyTableView_dividerColor,      Color.GRAY)
            dividerWidth = a.getDimension(R.styleable.EmptyTableView_dividerWidth,  4f)

            separatorColor = a.getColor(R.styleable.EmptyTableView_separatorColor,  Color.BLACK)
            separatorWidth = a.getDimension(R.styleable.EmptyTableView_separatorWidth,  0f)

            assignSeparators(a.getString(R.styleable.EmptyTableView_separators) ?: "")

            tableBorderColor = a.getColor(R.styleable.EmptyTableView_borderColor,     Color.GRAY)
            borderSize = a.getDimension(R.styleable.EmptyTableView_borderWidth, 0f)

            tableCornerRadius = a.getDimension(R.styleable.EmptyTableView_cornerRadius, tableCornerRadius)
        } finally {
            a.recycle()
        }

        // Paints
        dividerPaint.strokeWidth = dividerWidth
        dividerPaint.color = dividerColor

        seatPaint.color = seatColor

        tableBorderPaint.style = Paint.Style.STROKE
        tableBorderPaint.color = tableBorderColor
    }

    override fun postInvalidate() {
        super.postInvalidate()

        updateShapes()
    }

    //region Measuring
    /**
     * @return The desired width without border and padding
     */
    private fun desiredTableWidth(measureSpec: Int): Int {
        val size = MeasureSpec.getSize(measureSpec)
        val mode = MeasureSpec.getMode(measureSpec)

        Log.d(LOG_TAG, MeasureSpec.toString(mode))

        // Size == 0 would mess things up, so if that's present, return and leave seatWidth
        if (size == 0) {
            if (seatWidth == -1f) { // Size of measureSpec 0, seatWidth -1 -> No way of telling the size
                Log.e(LOG_TAG, "MeasureSpec->size and seatWidth both have placeholder values " +
                        "so determining a size isn't possible.")
            }
            return tableWidth.toInt()
        }

        var requestedWidth = size - borderSize * 2 - paddingLeft - paddingRight

        // AT_MOST: wrap_content
        if (mode == MeasureSpec.AT_MOST) {
            if (seatWidth == -1f) {
                Log.w(LOG_TAG, "MeasureSpec was \'AT_MOST\', which requires seatWidth to be set." +
                                "However it wasn't so \'EXACTLY\' was used instead.")
            } else {
                requestedWidth = Math.min(tableWidth, requestedWidth)
            }
        }
        return requestedWidth.toInt()
        //seatWidth = (requestedWidth - partitionSpace) / seatCount
    }

    /**
     * @return The desired height without border and padding
     */
    private fun desiredTableHeight(measureSpec: Int): Int {
        val size = MeasureSpec.getSize(measureSpec)
        val mode = MeasureSpec.getMode(measureSpec)

        // Size == 0 would mess things up, so if that's present, return and leave seatHeight
        if (size == 0) {
            if (seatHeight == -1f) { // Size of measureSpec 0, seatWidth -1 -> No way of telling the size
                Log.e(LOG_TAG, "MeasureSpec->size and seatHeight both have placeholder values " +
                        "so determining a size isn't possible.")
            }
            return tableHeight.toInt()
        }

        val desiredHeight = size - borderSize * 2

        if (mode == MeasureSpec.AT_MOST) {
            if (seatHeight == -1f) {
                Log.w(LOG_TAG,
                        "MeasureSpec was \'AT_MOST\', which requires seatHeight to be set." +
                                "However it wasn't so \'EXACTLY\' was used instead.")
            } else {
                return Math.min(seatHeight, desiredHeight).toInt()
            }
        }
        return desiredHeight.toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
                desiredTableWidth(widthMeasureSpec) + horizontalFrame,
                desiredTableHeight(heightMeasureSpec) + verticalFrame)
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
        canvas.drawRoundRect(tableBorderRect, tableCornerRadius, tableCornerRadius, tableBorderPaint)

        for (i in 1 until seatCount) {
            if (separators.contains(i))
                canvas.drawSeparator(i)
            else
                canvas.drawDivider(i)
        }
    }

    private fun priorArea(index: Int): Float {
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
}