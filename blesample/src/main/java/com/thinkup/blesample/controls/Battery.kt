package com.thinkup.blesample.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.thinkup.blesample.R
import kotlinx.android.synthetic.main.battery.view.*

class Battery(context: Context, attributeSet: AttributeSet?) : FrameLayout(context, attributeSet) {

    private var level = 0

    init {
        inflate(context, R.layout.battery, this)
    }

    fun setLevel(level: Int) {
        this.level = level
        setWillNotDraw(false)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        batteryBackground.setImageLevel(level)
        batteryOverlay.layoutParams.height = height - ((level * height) / 100)
        setSizedColorText(
            batteryLevel, level.toString(), "%",
            Color.parseColor(getColor()),
            1F, 0.50F
        )
    }

    /**
     * use color resources
     */
    private fun getColor(): String {
        return when {
            level <= 15 -> "#FF0000"
            level <= 45 -> "#FFFF00"
            else -> "#00FF00"
        }
    }


    /**
     * Move to [Spannable.kt] class
     */

    private fun setSizedColorText(
        view: TextView,
        textStart: String,
        textEnd: String,
        color: Int,
        sizeRangeStart: Float,
        sizeRangeEnd: Float
    ) {
        val spannable = SpannableString("$textStart$textEnd")

        spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, 0)

        spannable.setSpan(
            RelativeSizeSpan(sizeRangeStart),
            0,
            textStart.length,
            0
        )
        spannable.setSpan(
            RelativeSizeSpan(sizeRangeEnd),
            textStart.length,
            textStart.length + textEnd.length,
            0
        )
        view.setText(spannable, TextView.BufferType.SPANNABLE)
    }
}