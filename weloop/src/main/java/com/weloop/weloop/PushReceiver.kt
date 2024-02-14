package com.weloop.weloop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal const val NOTIFICATION_TITLE = "title"
internal const val NOTIFICATION_MESSAGE = "message"
internal const val NOTIFICATION_URL = "url"
internal const val INTENT_FILTER_PUSHY_RECEIVER_TO_WELOOP = "service.to.weloop.notification"

class PushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Attempt to extract the "title" property from the data payload, or fallback to app shortcut label
        val notificationTitle = if (intent.getStringExtra(NOTIFICATION_TITLE) != null)
            intent.getStringExtra(NOTIFICATION_TITLE)
        else
            context.packageManager.getApplicationLabel(context.applicationInfo).toString()

        // Attempt to extract the "message" property from the data payload: {"message":"Hello World!"}
        val notificationMessage =
            if (intent.getStringExtra(NOTIFICATION_MESSAGE) != null) intent.getStringExtra(NOTIFICATION_MESSAGE) else "WeLoop notification"

        val notificationUrl = intent.getStringExtra(NOTIFICATION_URL)

        val local = Intent()
        local.action = (INTENT_FILTER_PUSHY_RECEIVER_TO_WELOOP)
        local.putExtra(NOTIFICATION_TITLE, notificationTitle)
        local.putExtra(NOTIFICATION_MESSAGE, notificationMessage)
        local.putExtra(NOTIFICATION_URL, notificationUrl)
        local.setPackage(context.packageName)
        context.sendBroadcast(local)
    }
}
