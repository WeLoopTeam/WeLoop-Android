package com.weloop.weloop.network

import com.weloop.weloop.model.Notification
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

    @GET("/v1/mobile/notification-count")
    suspend fun requestNotification(@Header(AUTH) apiKey: String, @Query("email") email: String): Response<Notification>

    @GET("/v1/mobile/widget-visibility")
    suspend fun getWidgetVisibility(@Header(AUTH) apiKey: String, @Query("email") email: String): Response<WidgetVisibility>

    @POST("/registerDeviceForNotification")
    suspend fun registerDeviceForNotification(@Header(AUTH) apiKey: String, @Body body: RequestBody): Response<Unit>
}
