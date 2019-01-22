package com.zoniklalessimo.seatingplanner.displaySeatingPlan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.zoniklalessimo.seatingplanner.R
import com.zoniklalessimo.seatingplanner.scene.EmptyTableView

class SeatedTableView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
    : EmptyTableView(context, attrs, defStyleAttr, defStyleRes) {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, 0)

    constructor(context: Context) :
            this(context, null)

    enum class NameLetters {
        ALL,
        INITIALS,
        FIRST,
        FIRST_TWO
    }

    var drawNames: NameLetters = NameLetters.FIRST_TWO
        set(value) {
            field = value
            invalidate()
        }

    var students: List<String?> = listOf()
        set(value) {
            field = value
            invalidate()
        }

    var textColor: Int
        get() = textPaint.color
        set(value) {
            textPaint.color = value
            invalidate()
        }
    var textSize: Float
        get() = textPaint.textSize
        set(value) {
            textPaint.textSize = value
            invalidate()
        }

    private var textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
    }

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SeatedTableView, defStyleAttr, defStyleRes)

        try {
            textColor = a.getColor(R.styleable.SeatedTableView_textColor, Color.BLACK)
            textSize = a.getDimension(R.styleable.SeatedTableView_textSize, 20f)
        } finally {
            a.recycle()
        }
    }

    private fun growStudentsToSeatCount() {
        // When invoked before initialization, students is actually null
        students ?: return
        val sizeDiff = seatCount - students.size
        if (sizeDiff <= 0)
            return
        students += List(sizeDiff) { null }
    }

    private var stringsToDraw: List<String> = listOf()
    private fun updateStringsToDraw() {
        // When invoked before initialization, students is actually null
        stringsToDraw = students?.map {
            if (it == null)
                return@map String()

            getDrawLetters(it)
        }
    }

    override fun invalidate() {
        growStudentsToSeatCount()
        updateStringsToDraw()
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        for (i in 0 until seatCount) {
            val drawText = stringsToDraw[i]
            val center = priorArea(i + 1) - seatWidth / 2
            canvas.drawText(
                    drawText,
                    center,
                    height / 1.5f,
                    textPaint
            )
        }
    }

    private fun getDrawLetters(name: String) = when (drawNames) {
            NameLetters.FIRST -> name.first().toString()
            NameLetters.FIRST_TWO -> name.slice(0..1)
            NameLetters.ALL -> name
            NameLetters.INITIALS -> name.split(' ').fold(StringBuilder()) { builder, part ->
                builder.append(part.first())
            }.toString()
        }
}