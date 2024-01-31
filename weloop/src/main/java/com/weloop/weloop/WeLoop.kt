package com.weloop.weloop

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getColor
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.weloop.weloop.model.DeviceInfo
import com.weloop.weloop.model.NotificationListenerNotInitializedException
import com.weloop.weloop.model.RegistrationInfo
import com.weloop.weloop.model.User
import com.weloop.weloop.network.ApiServiceImp
import com.weloop.weloop.utils.AES256Cryptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.pushy.sdk.Pushy
import java.io.ByteArrayOutputStream
import java.util.Date


/* Created by *-----* Alexandre Thauvin *-----* */

class WeLoop(private var mContext: Context, private var mApiKey: String) {
    private var mCurrentInvocationMethod = 0
    private lateinit var mFloatingWidget: FloatingWidget
    private var webViewInterface = WebAppInterface()
    private lateinit var mToken: String
    private var isPreferencesLoaded = false
    private lateinit var mWindow: Window
    private var screenshot: String = ""
    private var screenShotAsked = false
    private lateinit var dialog: SweetAlertDialog
    private var shouldShowDialog = false
    private lateinit var mNotificationListener: NotificationListener
    private var deviceInfo = DeviceInfo()
    private var isLoaded = false
    private lateinit var mWebView: WebView
    private var loopNotification = false

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activity: Activity? = null

    fun initialize(
        window: Window,
        weloopLocation: String?,
        webView: WebView
    ) {
        mWebView = webView
        mWebView.isFocusableInTouchMode = true
        mWebView.isFocusable = true
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.javaScriptCanOpenWindowsAutomatically = true
        mWebView.settings.javaScriptEnabled = true
        val displayMetrics = DisplayMetrics()
        window.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height: Int = displayMetrics.heightPixels
        val width: Int = displayMetrics.widthPixels

        initWebAppListener()

        deviceInfo.screenHeight = height.toString()
        deviceInfo.screenWidth = width.toString()
        deviceInfo.weloopLocation = if (deviceInfo.weloopLocation.isNullOrEmpty()) {
            "Not found"
        } else {
            deviceInfo.weloopLocation
        }
        deviceInfo.weloopLocation = weloopLocation

        mWindow = window
        mWebView.addJavascriptInterface(webViewInterface, "Android")
    }

    fun initWidgetPreferences(floatingWidget: FloatingWidget) {
        mFloatingWidget = floatingWidget
        mFloatingWidget.setOnClickListener {
            invoke()
        }
        scope.launch {
            val response = ApiServiceImp.getWidgetPreferences(mApiKey)

            if (response.isSuccessful){
                response.body()?.let {
                    if (it.widgetPrimaryColor != null) {
                        mFloatingWidget.backgroundTintList = ColorStateList.valueOf(
                            Color.rgb(
                                it.widgetPrimaryColor!!["r"]!!.toInt(),
                                it.widgetPrimaryColor!!["g"]!!.toInt(),
                                it.widgetPrimaryColor!!["b"]!!.toInt()
                            )
                        )
                    } else {
                        if (Build.VERSION.SDK_INT in 21..22) {
                            mFloatingWidget.backgroundTintList =
                                ColorStateList.valueOf(
                                    getColor(
                                        mContext,
                                        R.color.defaultColorWidget
                                    )
                                )
                        } else {
                            mFloatingWidget.backgroundTintList =
                                ColorStateList.valueOf(mContext.getColor(R.color.defaultColorWidget))
                        }
                    }
                    if (it.widgetIcon != null) {
                        Glide.with(mContext)
                            .asBitmap()
                            .load(it.widgetIcon)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    floatingWidget.setImageBitmap(resource)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // this is called when imageView is cleared on lifecycle call or for
                                    // some other reason.
                                    // if you are referencing the bitmap somewhere else too other than this imageView
                                    // clear it here as you can no longer have the bitmap
                                }
                            })
                    }
                    if (it.widgetPosition.equals("right", ignoreCase = true)) {
                        val params = CoordinatorLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 40, 40)
                            gravity = Gravity.END or Gravity.BOTTOM
                        }
                        mFloatingWidget.layoutParams = params
                    } else {
                        val params = CoordinatorLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(40, 0, 0, 40)
                            gravity = Gravity.START or Gravity.BOTTOM
                        }
                        mFloatingWidget.layoutParams = params
                    }
                    isPreferencesLoaded = true
                    if (mCurrentInvocationMethod == FAB) {
                        mFloatingWidget.visibility = View.VISIBLE
                    }
                }
            }
            else {
                Log.e(TAG, "error during initialization")
            }
        }
    }

    fun registerForPushNotification(activity: Activity, firstName: String, lastName: String, email: String, language: String) {
        scope.launch {
            val deviceToken = Pushy.register(activity)
            val response = ApiServiceImp.registerDeviceForNotification(
                registrationInfo = RegistrationInfo(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    language = language,
                    pushyId = deviceToken
                ),
                apiKey = mApiKey
            )
            if (!response.isSuccessful){
                Log.e(TAG, "failed to register device for notification")
            }
            val broadcastReceiver = PushReceiver()
            broadcastReceiver.initActivity(activity)
            activity.registerReceiver(broadcastReceiver, IntentFilter())

            val filter = IntentFilter()
            filter.addAction("service.to.activity.transfer")
            val updateUIReciver: BroadcastReceiver


            updateUIReciver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    //UI update here
                    displayNotification(intent.getStringExtra("number").toString(), activity)
                }
            }

            activity.registerReceiver(updateUIReciver, filter)
        }
    }

    private fun displayNotification(notificationText: String, activity: Activity) {
        // Prepare a notification with vibration, sound and lights
        val builder = NotificationCompat.Builder(mContext)
            .setAutoCancel(true)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("title")
            .setContentText(notificationText)
            .setLights(Color.RED, 1000, 1000)
            .setVibrate(longArrayOf(0, 400, 250, 400))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
         .setContentIntent(PendingIntent.getActivity(mContext, 0, Intent(mContext, activity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        // Automatically configure a Notification Channel for devices running Android O+
        Pushy.setNotificationChannel(builder, mContext)

        // Get an instance of the NotificationManager service
        val notificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build the notification and display it
        //
        // Use a random notification ID so multiple
        // notifications don't overwrite each other
        notificationManager.notify((Math.random() * 100000).toInt(), builder.build())
    }

    private fun renderInvocation() {
        when (mCurrentInvocationMethod) {
            FAB -> {
                if (::mFloatingWidget.isInitialized && isPreferencesLoaded) {
                    mFloatingWidget.visibility = View.VISIBLE
                }
            }
            else -> {
                if (::mFloatingWidget.isInitialized) {
                    mFloatingWidget.visibility = View.GONE
                }
            }
        }
    }

    private fun initWebAppListener() {
        webViewInterface.addListener(object : WebAppInterface.WebAppListener {
            override fun closePanel() {
                mWebView.post {
                    mWebView.visibility = View.GONE
                    if (::mFloatingWidget.isInitialized && mCurrentInvocationMethod == FAB) {
                        mFloatingWidget.visibility = View.VISIBLE
                    }
                }
            }

            override fun getCapture() {
                if (screenshot.isNotEmpty()) {
                    mWebView.post {
                        mWebView.loadUrl(
                            "javascript:getCapture('data:im      90" +
                                    "; age/jpg;base64, ${screenshot}')"
                        ); screenShotAsked = false
                    }
                } else {
                    screenShotAsked = true
                }
            }

            override fun getCurrentUser() {
                if (this@WeLoop::mToken.isInitialized) {
                    mWebView.post { mWebView.loadUrl("javascript:GetCurrentUser({ appGuid: '$mApiKey', token: '$mToken'})") }
                } else {
                    mWebView.post { mWebView.loadUrl("javascript:GetCurrentUser({ appGuid: '$mApiKey'})") }
                }
            }

            override fun setNotificationCount(number: Int) {
                if (::mFloatingWidget.isInitialized) {
                    mFloatingWidget.count = number
                }
                if (::mNotificationListener.isInitialized) {
                    mNotificationListener.getNotification(number)
                }
            }

            override fun loadingFinished() {
                isLoaded = true
                shouldShowDialog = false
                if (::dialog.isInitialized) {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }
            }

            override fun getDeviceInfo() {
                val json = Gson().toJson(deviceInfo)
                mWebView.post { mWebView.loadUrl("javascript:getDeviceInfo({ value: '$json'})") }
            }
        })
    }

    internal class TakeScreenshotTask(val weLoop: WeLoop, val bitmap: Bitmap) :
        AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String? {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            weLoop.screenshot = result!!
            if (weLoop.screenShotAsked) {
                weLoop.mWebView.post {
                    weLoop.mWebView.loadUrl("javascript:getCapture('data:image/jpg;base64, ${weLoop.screenshot}')"); weLoop.screenShotAsked =
                    false
                }
            }
        }
    }

    fun invoke() {
        if (!isLoaded) {
            loadHome()
        }
        if (::mFloatingWidget.isInitialized) {
            mFloatingWidget.visibility = View.GONE
        }
        TakeScreenshotTask(this, takeScreenshot()!!).execute()
        mWebView.visibility = View.VISIBLE
        if (shouldShowDialog) {
            dialog = SweetAlertDialog(mContext, SweetAlertDialog.PROGRESS_TYPE)
            dialog.setCancelable(true)
            dialog.show()
        }
    }

    private fun takeScreenshot(): Bitmap? {
        val now = Date()
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg"

            // create bitmap screen capture
            val v1 = mWindow.decorView.rootView
            v1.setDrawingCacheEnabled(true)
            return Bitmap.createBitmap(v1.getDrawingCache())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    private fun loadHome() {
        mWebView.post { mWebView.loadUrl(URL + mApiKey); shouldShowDialog = true }
    }

    fun authenticateUser(user: User) {
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            val str = user.email + "|" + user.firstName + "|" + user.lastName + "|" + user.id
            mToken = AES256Cryptor.encrypt(str, mApiKey)!!
        } else {
            Toast.makeText(mContext, "email incorrect", Toast.LENGTH_LONG).show()
        }
    }

    fun addNotificationListener(notificationListener: NotificationListener) {
        mNotificationListener = notificationListener
    }

    fun startRequestingNotificationsEveryTwoMinutes(email: String) {
        stopRequestingNotificationsEveryTwoMinutes()
        loopNotification = true
        job = scope.launch {
            while (loopNotification) {
                requestNotification(email)
                delay(120000)
            }
        }
    }

    fun stopRequestingNotificationsEveryTwoMinutes() {
        loopNotification = false
        job?.cancel()
        job = null
    }

    @Throws
    fun requestNotification(email: String) {
        if (::mNotificationListener.isInitialized) {
            scope.launch {
                val result = ApiServiceImp.requestNotification(email, mApiKey)

                if (result.isSuccessful){
                    result.body()?.let {
                        mNotificationListener.getNotification(it.count)
                    }
                }
                else {
                    Log.e(TAG, "error occurred while requesting notification")
                }

            }
        } else
            throw NotificationListenerNotInitializedException()
    }

    fun setInvocationMethod(invocationMethod: Int) {
        mCurrentInvocationMethod = invocationMethod
        renderInvocation()
    }


    interface NotificationListener {
        fun getNotification(number: Int)
    }


    companion object {
        const val MANUAL = 0
        const val FAB = 1
        private const val URL = "https://widget.weloop.io/home?appGuid="
        private const val TAG = "WeLoop"
    }
}
