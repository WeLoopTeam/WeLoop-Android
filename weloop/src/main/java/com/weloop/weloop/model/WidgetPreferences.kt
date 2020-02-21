package com.weloop.weloop.model

import com.google.gson.annotations.SerializedName

/* Created by *-----* Alexandre Thauvin *-----* */

data class WidgetPreferences(
    @SerializedName("Widget_PrimaryColor")
    var widgetPrimaryColor: HashMap<String, String>? = null,
    @SerializedName("Widget_Icon")
    var widgetIcon: String? = null,
    @SerializedName("Widget_Position")
    var widgetPosition: String = "",
    @SerializedName("Widget_Message")
    var widgetMessage: String = "",
    @SerializedName("Language")
    var language: String = "",
    @SerializedName("SSO_Widget")
    var ssoWidget: String = "",
    @SerializedName("UserCount")
    var userCount: Int = 0)