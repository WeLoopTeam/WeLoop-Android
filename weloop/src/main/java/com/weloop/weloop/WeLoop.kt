package com.weloop.weloop

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.tbouron.shakedetector.library.ShakeDetector
import com.github.tbouron.shakedetector.library.ShakeDetector.OnShakeListener
import com.weloop.weloop.model.User
import com.weloop.weloop.network.ApiServiceImp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/* Created by *-----* Alexandre Thauvin *-----* */

class WeLoop : WebView {
    private var currentInvocationMethod = 0
    private var apiKey: String = ""
    private lateinit var floatingWidget: FloatingWidget
    private val disposable = CompositeDisposable()
    private lateinit var token: String

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        visibility = View.GONE
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.javaScriptEnabled = true
    }

    fun initialize(apiKey: String, floatingWidget: FloatingWidget) {
        this.floatingWidget = floatingWidget
        this.apiKey = apiKey
        ShakeDetector.create(context, OnShakeListener {
            invoke()
        })

        this.floatingWidget.setOnClickListener {
            invoke()
        }

        loadUrl(URL + apiKey)
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

    private fun encrypt(strToEncrypt: String, secretKey: String): String {
        val iv = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val ivspec = IvParameterSpec(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secretKey.toCharArray(), apiKey.toByteArray(), 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.getEncoded(), "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec)
        return Base64.encodeToString(cipher.doFinal(strToEncrypt.toByteArray()), 0)
    }

    private fun decrypt(strToDecrypt: String, secretKey: String): String {
        val iv = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val ivspec = IvParameterSpec(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secretKey.toCharArray(), apiKey.toByteArray(), 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.getEncoded(), "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec)
        return String(cipher.doFinal(Base64.decode(strToDecrypt, 0)))
    }

    fun resumeWeLoop() {
        ShakeDetector.start()
        Toast.makeText(context, decrypt(token, apiKey), Toast.LENGTH_LONG)
            .show()
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
        private const val SECRET_KEY = "SECRET_KEY"
        private const val URL = "https://staging-widget.30kg-rice.cooking/home?appGuid="
    }
}