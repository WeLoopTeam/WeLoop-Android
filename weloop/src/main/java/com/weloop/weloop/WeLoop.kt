package com.weloop.weloop

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.github.tbouron.shakedetector.library.ShakeDetector
import com.github.tbouron.shakedetector.library.ShakeDetector.OnShakeListener
import com.weloop.weloop.network.ApiServiceImp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


/* Created by *-----* Alexandre Thauvin *-----* */

class WeLoop : WebView {
    private var currentInvocationMethod = 0
    private var apiKey: String = ""
    private lateinit var floatingWidget: FloatingWidget
    private lateinit var sensorMgr: SensorManager
    private lateinit var sensor: Sensor
    private val disposable = CompositeDisposable()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        visibility = View.GONE
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.javaScriptEnabled = true
    }


    fun initialize(apiKey: String, floatingWidget: FloatingWidget) {
        ShakeDetector.create(context, OnShakeListener{
            invoke()
        })

        this.floatingWidget = floatingWidget
        this.floatingWidget.setOnClickListener {
            invoke()
        }

        loadUrl(URL + apiKey)
        this.apiKey = apiKey
        disposable.add(ApiServiceImp.getWidgetPreferences(this.apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {

            }
            .subscribe {
                if (it.widgetIcon != null) {

                } else {
                    this.floatingWidget.backgroundTintList = ColorStateList.valueOf(
                        Color.rgb(
                            it.widgetPrimaryColor!!["r"]!!.toInt(),
                            it.widgetPrimaryColor!!["g"]!!.toInt(),
                            it.widgetPrimaryColor!!["b"]!!.toInt()
                        )
                    )
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

    fun resumeWeLoop(){
        ShakeDetector.start()
    }

    fun stopWeLoop(){
        ShakeDetector.stop()
    }

    fun destroyWeLoop(){
        ShakeDetector.destroy()
    }

    fun invoke() {
        visibility = View.VISIBLE
    }

    companion object {
        const val FAB = 0
        const val SHAKE_GESTURE = 1
        const val MANUAL = 2
        private const val URL = "https://staging-widget.30kg-rice.cooking/home?appGuid="
    }
}