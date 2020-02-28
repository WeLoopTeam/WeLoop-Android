package com.weloop.weloop

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.tbouron.shakedetector.library.ShakeDetector
import com.github.tbouron.shakedetector.library.ShakeDetector.OnShakeListener
import com.weloop.weloop.model.User
import com.weloop.weloop.model.WebAppInterface
import com.weloop.weloop.network.ApiServiceImp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.schedule
import kotlin.concurrent.thread


/* Created by *-----* Alexandre Thauvin *-----* */

class WeLoop : WebView {
    private var currentInvocationMethod = 0
    private var apiKey: String = ""
    private lateinit var floatingWidget: FloatingWidget
    private var webViewInterface = WebAppInterface()
    private val disposable = CompositeDisposable()
    private lateinit var token: String
    private var isPreferencesLoaded = false
    private lateinit var window: Window
    private var screenshot: String = ""

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        visibility = View.GONE
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.javaScriptEnabled = true
    }

    fun initialize(apiKey: String, floatingWidget: FloatingWidget, window: Window) {
        this.floatingWidget = floatingWidget
        this.floatingWidget.visibility = View.GONE
        this.window = window
        this.apiKey = apiKey
        initWebAppListener()
        addJavascriptInterface(webViewInterface, "Android")
        ShakeDetector.create(context, OnShakeListener {
            invoke()
        })

        this.floatingWidget.setOnClickListener {
            invoke()
        }

        loadUrl(URL + apiKey)
        initWidgetPreferences()
    }

    private fun initWebAppListener() {
        webViewInterface.addListener(object : WebAppInterface.WebAppListener {
            override fun closePanel() {
                this@WeLoop.post { visibility = View.GONE ; floatingWidget.visibility = View.VISIBLE ; loadHome() }
            }

            override fun getCapture(){
                Timer("SettingUp", false).schedule(3000) {
                    this@WeLoop.post { loadUrl("javascript:getCapture('data:image/jpg;base64, $screenshot')") }
                }
            }

            override fun getCurrentUser(){
                this@WeLoop.post { loadUrl("javascript:GetCurrentUser({ appGuid: '$apiKey', token: '$token'})") }
            }

            override fun setNotificationCount(number: Int) {
                floatingWidget.count = number
            }
        })
    }

    private fun takeScreenshot(): Bitmap?{
        val now = Date()
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg"

            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.setDrawingCacheEnabled(true)
            return Bitmap.createBitmap(v1.getDrawingCache())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    private fun initWidgetPreferences() {
        disposable.add(ApiServiceImp.getWidgetPreferences(this.apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {

            }
            .subscribe {
                if (it.widgetPrimaryColor != null) {
                    this.floatingWidget.backgroundTintList = ColorStateList.valueOf(
                        Color.rgb(
                            it.widgetPrimaryColor!!["r"]!!.toInt(),
                            it.widgetPrimaryColor!!["g"]!!.toInt(),
                            it.widgetPrimaryColor!!["b"]!!.toInt()
                        )
                    )
                } else {
                    this.floatingWidget.backgroundTintList =
                        ColorStateList.valueOf(context.getColor(R.color.defaultColorWidget))
                }
                if (it.widgetIcon != null) {
                    Glide.with(context)
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
                    this.floatingWidget.layoutParams = params
                } else {
                    val params = CoordinatorLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(40, 0, 0, 40)
                        gravity = Gravity.START or Gravity.BOTTOM
                    }
                    this.floatingWidget.layoutParams = params
                }
                isPreferencesLoaded = true
                this.floatingWidget.visibility = View.VISIBLE
            }
        )
    }

    fun authenticateUser(user: User) {
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            val str = user.email + "|" + user.firstName + "|" + user.lastName + "|" + user.id
            token = AES256Cryptor.encrypt(str, apiKey)
        } else {
            Toast.makeText(context, "email incorrecte", Toast.LENGTH_LONG).show()
        }
    }

    fun setInvocationMethod(invocationMethod: Int) {
        this.currentInvocationMethod = invocationMethod
        renderInvocation()
    }

    private fun renderInvocation() {
        when (currentInvocationMethod) {
            FAB -> {
                if (::floatingWidget.isInitialized && isPreferencesLoaded) {
                    floatingWidget.visibility = View.VISIBLE
                }
                ShakeDetector.stop()
            }
            SHAKE_GESTURE -> {
                if (::floatingWidget.isInitialized) {
                    floatingWidget.visibility = View.GONE
                }
                ShakeDetector.start()
            }
            else -> {
                if (::floatingWidget.isInitialized) {
                    floatingWidget.visibility = View.GONE
                }
                ShakeDetector.stop()
            }
        }
    }

    fun resumeWeLoop() {
        ShakeDetector.start()
    }

    fun stopWeLoop() {
        ShakeDetector.stop()
    }

    fun destroyWeLoop() {
        ShakeDetector.destroy()
    }

    private fun loadHome(){
        this.post { loadUrl(URL + apiKey) }
    }

    fun invoke() {
        floatingWidget.visibility = View.GONE
        val bitmap = takeScreenshot()
        val byteArrayOutputStream =  ByteArrayOutputStream()
        thread {
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream .toByteArray()
            screenshot = Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        visibility = View.VISIBLE
    }

    companion object {
        const val FAB = 0
        const val SHAKE_GESTURE = 1
        const val MANUAL = 2
        private const val URL = "https://staging-widget.30kg-rice.cooking/home?appGuid="
    }
}