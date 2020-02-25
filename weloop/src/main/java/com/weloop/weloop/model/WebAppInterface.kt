package com.weloop.weloop.model

import android.webkit.JavascriptInterface

/* Created by *-----* Alexandre Thauvin *-----* */

class WebAppInterface {

    private lateinit var webAppListener: WebAppListener

    fun addListener(webAppListener: WebAppListener){
        this.webAppListener = webAppListener
    }

    @JavascriptInterface
    fun closePanel(){
        webAppListener.closePanel()
    }

    @JavascriptInterface
    fun getCurrentUser(): String{
        return webAppListener.getCurrentUser()
    }

    @JavascriptInterface
    fun getCapture(): String{
        return webAppListener.getCapture()
    }

    @JavascriptInterface
    fun setNotificationCount(number: Int){
        webAppListener.setNotificationCount(number)
    }

    interface WebAppListener{
        fun closePanel()
        fun getCurrentUser(): String
        fun getCapture(): String
        fun setNotificationCount(number: Int)
    }


}