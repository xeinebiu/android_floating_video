package com.xeinebiu.floating.video.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class XFrameLayout : FrameLayout {
    var dispatchTouchListener: ((ev: MotionEvent?) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        dispatchTouchListener?.invoke(ev)
        return super.dispatchTouchEvent(ev)
    }
}