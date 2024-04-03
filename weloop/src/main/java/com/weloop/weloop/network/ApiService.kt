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
        private const val AUTH = "x-api-key"
        private const val PROJECT_ID = "project-id"
    }

    @GET("/v1/mobile/notification-count")
    suspend fun requestNotification(@Header(AUTH) apiKey: String, @Header(PROJECT_ID) projectId: String, @Query("email") email: String): Response<Notification>

    @GET("/v1/mobile/widget-visibility")
    suspend fun getWidgetVisibility(@Header(AUTH) apiKey: String, @Header(PROJECT_ID) projectId: String, @Query("email") email: String): Response<WidgetVisibility>

    @POST("/v1/mobile/register-device")
    suspend fun registerDeviceForNotification(@Header(AUTH) apiKey: String, @Header(PROJECT_ID) projectId: String, @Body body: RequestBody): Response<Unit>
}
