package com.weloop.weloop

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
    fun getCurrentUser(){
       webAppListener.getCurrentUser()
    }

    @JavascriptInterface
    fun getCapture(){
        webAppListener.getCapture()
    }

    @JavascriptInterface
    fun setNotificationCount(number: Int){
        webAppListener.setNotificationCount(number)
    }

    @JavascriptInterface
    fun IsLoaded(){
        webAppListener.loadingFinished()
    }

    @JavascriptInterface
    fun getDeviceInfo(){
        webAppListener.getDeviceInfo()
    }

    interface WebAppListener{
        fun closePanel()
        fun getCurrentUser()
        fun getCapture()
        fun setNotificationCount(number: Int)
        fun loadingFinished()
        fun getDeviceInfo()
    }


}
