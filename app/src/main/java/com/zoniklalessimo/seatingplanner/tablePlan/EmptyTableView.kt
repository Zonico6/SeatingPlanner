package com.zoniklalessimo.seatingplanner.tablePlan

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
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

    /* Set defaults here and pass them as the defaults when obtaining the attributes
    var seatCount: Int = 1

    var dividerColor: Int = Color.GRAY
    var dividerWidth: Float = 0f

    var tableBorderColor: Int = Color.GRAY
    var borderSize: Float = 0f

    var seatWidth: Float = -1f
    var seatHeight: Float = -1f
    var seatColor: Int = Color.BLACK

    val tableWidth get() = seatWidth * seatCount
    val tableHeight get() = seatHeight */

    // TODO: Add seatColors[], whose colors get cycled through when drawing the seats
    // region Attributes

    // seat dimensions
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

    // table dimensions
    var tableCornerRadius = 15f
        set(value) {
            field = value
            invalidate()
        }
    // The table's width without border and padding
    val tableWidth get() = seatWidth * seatCount + dividerWidth * (seatCount - 1)
    // The table's height without border and padding
    val tableHeight get() = seatHeight

    // divider
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

    // separators
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

    // tableBorder
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

    var seatColor: Int
        get() = seatPaint.color
        set(color) {
            seatPaint.color = color
            invalidate()
        }

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

            assignSeperators(a.getString(R.styleable.EmptyTableView_separators) ?: "")

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

    fun assignSeperators(sepsStr: CharSequence) {
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

    override fun postInvalidate() {
        super.postInvalidate()

        updateShapes()
    }

    private fun updateSeatWidth(measureSpec: Int) {
        val requestedWidth = MeasureSpec.getSize(measureSpec) - borderSize * 2

        // AT_MOST: match_parent
        if (MeasureSpec.getMode(measureSpec) == MeasureSpec.AT_MOST) {
            if (seatWidth == -1f) {
                Log.w(LOG_TAG,
                        "MeasureSpec was \'AT_MOST\', which requires seatWidth to be set." +
                                "However it wasn't so \'EXACTLY\' was used instead.")
            } else {
                val receivedTableWidth = seatWidth * seatCount + dividerWidth * (seatCount - 1)
                val resolvedWidth = Math.min(receivedTableWidth, requestedWidth)
                seatWidth = resolvedWidth / seatCount
                return
            }
        }
        seatWidth = (requestedWidth - dividerWidth * (seatCount - 1)) / seatCount
    }
    private fun updateSeatHeight(measureSpec: Int) {
        val requestedHeight = MeasureSpec.getSize(measureSpec) - borderSize * 2
        if (MeasureSpec.getMode(measureSpec) == MeasureSpec.AT_MOST) {
            if (seatHeight == -1f) {
                Log.w(LOG_TAG,
                        "MeasureSpec was \'AT_MOST\', which requires seatHeight to be set." +
                                "However it wasn't so \'EXACTLY\' was used instead.")
            } else {
                seatHeight = Math.min(seatHeight, requestedHeight)
                return
            }
        }
        seatHeight = requestedHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Width
        updateSeatWidth(widthMeasureSpec)
        updateSeatHeight(heightMeasureSpec)

        updateShapes()

        setMeasuredDimension(
                tableWidth.toInt() + paddingRight + paddingLeft + borderSize.toInt() * 2,
                tableHeight.toInt() + paddingTop + paddingBottom + borderSize.toInt() * 2)
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

    private fun prior(index: Int): Float {
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
            drawPartition(prior(index), dividerWidth, dividerPaint)

    private fun Canvas.drawSeparator(index: Int) =
            drawPartition(prior(index), separatorWidth, separatorPaint)

    private fun Canvas.drawPartition(priorWidth: Float, width: Float, paint: Paint) {
        val x = paddingLeft + borderSize + priorWidth
        val y = paddingTop.toFloat() + borderSize
        drawRect(x, y, x + width, y + seatHeight, paint)
    }
}