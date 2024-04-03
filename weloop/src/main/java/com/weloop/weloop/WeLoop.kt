package com.weloop.weloop

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Patterns
import android.view.View
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.pedant.SweetAlert.SweetAlertDialog
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.pushy.sdk.Pushy
import timber.log.Timber
import java.io.ByteArrayOutputStream


/* Created by *-----* Alexandre Thauvin *-----* */

class MyWebViewClient : WebViewClient() {

//    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
//        // Example: Proceed with SSL error
//        view?.url?.let {
//            if (
//                it.contains("https://front.weloop.dev", ignoreCase = true)
//                || it.contains("https://auth.weloop.dev", ignoreCase = true)
//
//                ) {
//                handler?.proceed()
//            }
//
//        }
//    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        Timber.e("url: ${view?.url}")
        return false
    }
}

class WeLoop(
    private var mContext: Activity,
    private var mProjectId: String,
    private val mApiKey: String
) {
    private var mSideWidget: SideWidget? = null
    private var webViewInterface = WebAppInterface()
    private lateinit var mToken: String
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
    private lateinit var payloadReceiver: BroadcastReceiver
    private val pushyBroadcastReceiver = PushReceiver()
    private var notificationUrl: String? = null
    private var shouldSideWidgetBeDisplayed = false

    fun initialize(
        email: String,
        window: Window,
        weloopLocation: String?,
        sideWidget: SideWidget? = null,
        webView: WebView,
    ) {
        Timber.plant(Timber.DebugTree())
        mWebView = webView
        mWebView.isFocusableInTouchMode = true
        mWebView.isFocusable = true
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.javaScriptCanOpenWindowsAutomatically = true
        mWebView.settings.javaScriptEnabled = true
        mWebView.webViewClient = MyWebViewClient()
        mWebView.settings.userAgentString = "Mozilla/5.0 (Android 14; Mobile; rv:123.0) Gecko/123.0 Firefox/123.0"
        val displayMetrics = DisplayMetrics()
        window.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height: Int = displayMetrics.heightPixels
        val width: Int = displayMetrics.widthPixels

        initWebAppListener()

        mSideWidget = sideWidget
        mSideWidget?.setOnClickListener { invoke() }

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

        scope.launch(Dispatchers.Main) {
            val response = ApiServiceImp.getWidgetVisibility(
                email = email,
                apiKey = mApiKey,
                projectId = mProjectId
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    shouldSideWidgetBeDisplayed = it.fab
                    triggerWidgetVisibility()
                }
            } else {
                // TODO delete
                shouldSideWidgetBeDisplayed = true
                triggerWidgetVisibility()
                Timber.e("error when getting widget visibility")
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun registerPushNotification(
        activity: Activity,
        firstName: String,
        lastName: String,
        email: String,
        language: String
    ) {
        scope.launch(SupervisorJob()) {
            val deviceToken = Pushy.register(mContext)
            val response = ApiServiceImp.registerDeviceForNotification(
                registrationInfo = RegistrationInfo(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    language = language,
                    pushyId = deviceToken
                ),
                apiKey = mApiKey,
                projectId = mProjectId
            )
            if (!response.isSuccessful) {
                Timber.e("failed to register device for notification")
            }
            mContext.registerReceiver(pushyBroadcastReceiver, IntentFilter())

            val filter = IntentFilter()
            filter.addAction(INTENT_FILTER_PUSHY_RECEIVER_TO_WELOOP)

            payloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    intent.getStringExtra(NOTIFICATION_TITLE)?.let { title ->
                        intent.getStringExtra(NOTIFICATION_MESSAGE)?.let { message ->
                            notificationUrl = intent.getStringExtra(NOTIFICATION_URL)
                            displayNotification(
                                notificationTitle = title,
                                notificationMessage = message,
                                activity
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mContext.registerReceiver(payloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                mContext.registerReceiver(payloadReceiver, filter)
            }
        }
    }

    fun unregisterPushNotification() {
        Pushy.unregister(mContext)
        Timber.e("Pushy was unregistered")
        mContext.unregisterReceiver(payloadReceiver)
        mContext.unregisterReceiver(pushyBroadcastReceiver)
    }

    fun redirectToWeLoopFromPushNotification() {
        notificationUrl?.let {
            Timber.d("notification url: $notificationUrl")
            mWebView.visibility = View.VISIBLE
            mSideWidget?.visibility = View.GONE
            mWebView.post { mWebView.loadUrl(it) }
        }
    }

    private fun displayNotification(
        notificationTitle: String,
        notificationMessage: String,
        activity: Activity
    ) {
        val activityIntent = Intent(mContext, activity::class.java)
        activityIntent.action = INTENT_FILTER_WELOOP_NOTIFICATION
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val builder = NotificationCompat.Builder(mContext, "WeLoop Notification")
            .setAutoCancel(true)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setLights(Color.RED, 1000, 1000)
            .setVibrate(longArrayOf(0, 400, 250, 400))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(
                PendingIntent.getActivity(
                    mContext,
                    0,
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        Pushy.setNotificationChannel(builder, mContext)

        with(NotificationManagerCompat.from(mContext)) {
            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                // public fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                //                                        grantResults: IntArray)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(mContext, "Need notification permission", Toast.LENGTH_SHORT).show()
                return@with
            }
            // notificationId is a unique int for each notification that you must define.
            notify((Math.random() * 100000).toInt(), builder.build())
        }
    }

    private fun triggerWidgetVisibility() {
        if (shouldSideWidgetBeDisplayed) {
            mSideWidget?.visibility = View.VISIBLE
        } else
            mSideWidget?.visibility = View.GONE
    }

    private fun initWebAppListener() {
        webViewInterface.addListener(object : WebAppInterface.WebAppListener {
            override fun closePanel() {
                mWebView.post {
                    mWebView.visibility = View.GONE
                    if (shouldSideWidgetBeDisplayed) {
                        mSideWidget?.visibility = View.VISIBLE
                    }
                }
            }

            override fun getCapture() {
                if (screenshot.isNotEmpty()) {
                    mWebView.post {
                        mWebView.loadUrl(
                            "javascript:getCapture('data:im      90" +
                                    "; age/jpg;base64, ${screenshot}')"
                        )
                        screenShotAsked = false
                    }
                } else {
                    screenShotAsked = true
                }
            }

            override fun getCurrentUser() {
                if (this@WeLoop::mToken.isInitialized) {
                    mWebView.post { mWebView.loadUrl("javascript:GetCurrentUser({ appGuid: '$mProjectId', token: '$mToken'})") }
                } else {
                    mWebView.post { mWebView.loadUrl("javascript:GetCurrentUser({ appGuid: '$mProjectId'})") }
                }
            }

            override fun setNotificationCount(number: Int) {
                mSideWidget?.showNotificationDot(number > 0)
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

    fun invoke() {
        if (!isLoaded) {
            shouldShowDialog = true
            loadHome()
            isLoaded = true
        }
        mSideWidget?.visibility = View.GONE
        takeScreenshot()
        mWebView.visibility = View.VISIBLE
        // TODO uncomment
//        if (shouldShowDialog) {
//            dialog = SweetAlertDialog(mContext, SweetAlertDialog.PROGRESS_TYPE)
//            dialog.setCancelable(true)
//            dialog.show()
//        }
    }

    private fun takeScreenshot() {
        try {
            // create bitmap screen capture
            val v1 = mWindow.decorView.rootView
            v1.setDrawingCacheEnabled(true)
            scope.launch {
                val bitmap = Bitmap.createBitmap(v1.getDrawingCache())
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val result = Base64.encodeToString(byteArray, Base64.DEFAULT)
                screenshot = result!!
                if (screenShotAsked) {
                    mWebView.post {
                        mWebView.loadUrl("javascript:getCapture('data:image/jpg;base64, ${screenshot}')")
                        screenShotAsked = false
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun loadHome() {
        mWebView.post {
            mWebView.loadUrl(URL + mProjectId)
        }
    }

    fun authenticateUser(user: User) {
        if (Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            val str = user.email + "|" + user.firstName + "|" + user.lastName + "|" + user.id
            mToken = AES256Cryptor.encrypt(str, mProjectId)!!
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
                val result = ApiServiceImp.requestNotification(email, mApiKey, mProjectId)

                if (result.isSuccessful) {
                    result.body()?.let {
                        mNotificationListener.getNotification(it.count)
                    }
                } else {
                    Timber.e("error occurred while requesting notification")
                }

            }
        } else
            throw NotificationListenerNotInitializedException()
    }

    fun backButtonHasBeenPressed() {
        if (mWebView.visibility == View.VISIBLE) {
            if (shouldSideWidgetBeDisplayed) {
                mSideWidget?.visibility = View.VISIBLE
            }
        }
    }

    fun restartWebview() {
        notificationUrl?.let {
            mWebView.post {
                mWebView.loadUrl(it)

            }
        } ?: loadHome()
    }


    interface NotificationListener {
        fun getNotification(number: Int)
    }


    companion object {
        const val INTENT_FILTER_WELOOP_NOTIFICATION = "com.weloop.notification"
        private const val URL = "https://front.weloop.ai/"
    }
}
