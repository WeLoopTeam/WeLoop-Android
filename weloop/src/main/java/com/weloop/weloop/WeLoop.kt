package com.weloop.weloop

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView


/* Created by *-----* Alexandre Thauvin *-----* */

class WeLoop: WebView{
    private var currentInvocationMethod = 0
    enum class InvocationMethod
    private var apiKey: String = ""

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)


    fun initialize(apiKey: String){

    }

    fun authenticateUser(user: User){

    }

    fun set(){

    }

    fun invoke(url: String){
        visibility = View.VISIBLE
        loadUrl(url)
    }

    companion object {

    }
}