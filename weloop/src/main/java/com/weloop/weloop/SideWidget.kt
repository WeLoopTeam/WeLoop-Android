package com.weloop.weloop

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.weloop.weloop.databinding.NotificationDotBinding
import com.weloop.weloop.databinding.SideWidgetBinding

class SideWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    private lateinit var notificationDotBinding: NotificationDotBinding

    init {
        SideWidgetBinding.inflate(LayoutInflater.from(context), this, true)
        inflate(context, R.layout.side_widget, this)
        visibility = View.GONE
    }

    fun showNotificationDot(shouldShow: Boolean) {
        if (shouldShow) {
            if (!::notificationDotBinding.isInitialized) {
                notificationDotBinding = NotificationDotBinding.inflate(LayoutInflater.from(context), this, true)
            }
            notificationDotBinding.notificationDot.visibility = View.VISIBLE
        } else {
            notificationDotBinding.notificationDot.visibility = View.GONE
        }
    }

}
