package com.weloop.weloop

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import me.pushy.sdk.Pushy

class PushReceiver : BroadcastReceiver() {

    private var activity: Activity? = null

    fun initActivity(activity: Activity) {
        this.activity = activity
    }
    override fun onReceive(context: Context, intent: Intent) {
        // Attempt to extract the "title" property from the data payload, or fallback to app shortcut label
        val notificationTitle = if (intent.getStringExtra("title") != null) intent.getStringExtra("title") else context.packageManager.getApplicationLabel(context.applicationInfo).toString()

        // Attempt to extract the "message" property from the data payload: {"message":"Hello World!"}
        val notificationText = if (intent.getStringExtra("message") != null ) intent.getStringExtra("message") else "Test notification"

        val local = Intent()
        local.setAction("service.to.activity.transfer")
        local.putExtra("number", notificationText)
        context.sendBroadcast(local)


    }
}
