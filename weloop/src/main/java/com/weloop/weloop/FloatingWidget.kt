package com.weloop.weloop

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton

/* Created by *-----* Alexandre Thauvin *-----* */

class FloatingWidget: FloatingActionButton {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        visibility = View.VISIBLE
        this.setImageDrawable(context.getDrawable(R.drawable.ic_logo_white))
    }
}