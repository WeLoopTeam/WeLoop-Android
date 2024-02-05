package com.weloop.weloop

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.weloop.weloop.databinding.SideWidgetBinding

class SideWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    private val sideWidgetBinding = SideWidgetBinding.inflate(LayoutInflater.from(context), this, true)
    init {
        inflate(context, R.layout.side_widget, this)
        visibility = View.GONE
    }

    fun showNotificationDot(shouldShow: Boolean){
        if (shouldShow){
            sideWidgetBinding.notificationDot.visibility = View.VISIBLE
        }
        else {
            sideWidgetBinding.notificationDot.visibility = View.GONE
        }
    }

}
