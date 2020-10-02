package com.weloop.weloop.model

import android.os.Build

/* Created by *-----* Alexandre Thauvin *-----* */

data class DeviceInfo(
    val osVersion: String? = System.getProperty("os.version"),
    val apiVersion: String = Build.VERSION.SDK_INT.toString(),
    val deviceName: String = Build.DEVICE,
    val model: String = Build.MODEL,
    val productOverall: String = Build.PRODUCT,
    var screenHeight: String = "",
    var screenWidth: String = "",
    var weloopLocation: String = "")