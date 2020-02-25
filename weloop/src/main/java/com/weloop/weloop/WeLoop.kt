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
import android.view.Gravity
import android.view.View
import android.view.Window
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
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/* Created by *-----* Alexandre Thauvin *-----* */

class WeLoop : WebView {
    private var currentInvocationMethod = 0
    private var apiKey: String = ""
    private lateinit var floatingWidget: FloatingWidget
    private var webViewInterface = WebAppInterface()
    private val disposable = CompositeDisposable()
    private lateinit var token: String
    private lateinit var window: Window
    private lateinit var toto: ImageView

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        visibility = View.GONE
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.javaScriptEnabled = true
    }

    fun initialize(apiKey: String, floatingWidget: FloatingWidget, window: Window, t: ImageView) {
        this.toto =t
        this.floatingWidget = floatingWidget
        this.window = window
        this.apiKey = apiKey
        initWebAppListener()
        addJavascriptInterface(webViewInterface, "Android")
        ShakeDetector.create(context, OnShakeListener {
            invoke()
        })

        this.floatingWidget.setOnClickListener {
            takeScreenshot()
            //invoke()
        }

        loadUrl(URL + apiKey)
        initWidgetPreferences()
    }

    private fun initWebAppListener() {
        webViewInterface.addListener(object : WebAppInterface.WebAppListener {
            override fun closePanel() {
                visibility = View.GONE
            }

            override fun getCapture(): String {
                return ""
            }

            override fun getCurrentUser(): String {
                val map = mutableMapOf<String, String>()
                map["token"] = token
                map["apiKey"] = apiKey
                return JSONObject(map.toMap()).toString()
            }

            override fun setNotificationCount(number: Int) {
                floatingWidget.count = number
            }
        })
    }

    fun takeScreenshot(){
        val now = Date()
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.setDrawingCacheEnabled(true)
            val bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            toto.setImageBitmap(bitmap)

        } catch (e: Throwable) {
            e.printStackTrace();
        }
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
                        setMargins(0, 0, 10, 10)
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
            }
        )
    }

    fun authenticateUser(user: User) {
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            val str = user.email + "|" + user.firstName + "|" + user.lastName + "|" + user.id
            token = encrypt(str, apiKey)
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
                if (::floatingWidget.isInitialized) {
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

    private fun encrypt(strToEncrypt: String, secret: String): String {
        val iv = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val ivSpec = IvParameterSpec(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), apiKey.toByteArray(), 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        return Base64.encodeToString(cipher.doFinal(strToEncrypt.toByteArray()), 0)
    }

    private fun decrypt(strToDecrypt: String, secret: String): String {
        val iv = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val ivSpec = IvParameterSpec(iv)

        val factory = SecretKeyFactory.getInstance(TRANSFORMATION)
        val spec = PBEKeySpec(secret.toCharArray(), apiKey.toByteArray(), 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return String(cipher.doFinal(Base64.decode(strToDecrypt, 0)))
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

    fun invoke() {
        visibility = View.VISIBLE
    }

    companion object {
        const val FAB = 0
        const val SHAKE_GESTURE = 1
        const val MANUAL = 2
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val URL = "https://staging-widget.30kg-rice.cooking/home?appGuid="
    }
}