package com.weloop.weloop.network

import com.weloop.weloop.model.Notification
import com.weloop.weloop.model.WidgetPreferences
import com.weloop.weloop.model.WidgetVisibility
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/* Created by *-----* Alexandre Thauvin *-----* */

interface ApiService {
    companion object {
        private const val AUTH = "Authorization"
    }
    @GET("/widget")
    suspend fun getWidgetPreferences(@Header(AUTH) apiKey: String): Response<WidgetPreferences>

    @GET("/widgetnotifications/count")
    suspend fun requestNotification(@Header(AUTH) apiKey: String, @Query("email") email: String): Response<Notification>

    @GET("/getWidgetVisibility")
    suspend fun getWidgetVisibility(@Header(AUTH) apiKey: String): Response<WidgetVisibility>

    @POST("/registerDeviceForNotification")
    suspend fun registerDeviceForNotification(@Header(AUTH) apiKey: String, @Body body: RequestBody): Response<Unit>
}
