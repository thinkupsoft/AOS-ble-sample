package com.thinkup.blesample.renderers

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.thinkup.blesample.R


class DividerHelper(context: Context, @DrawableRes resource: Int = R.drawable.item_divider) : RecyclerView.ItemDecoration() {

    private val divider = ContextCompat.getDrawable(context, resource)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) == 0) {
            return
        }
        outRect.top = divider?.intrinsicHeight ?: 0
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        divider?.let { divider ->
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight

            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)

                val params = child.layoutParams as RecyclerView.LayoutParams

                val top = child.bottom + params.bottomMargin
                val bottom = top + divider.intrinsicHeight

                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }
    }
}