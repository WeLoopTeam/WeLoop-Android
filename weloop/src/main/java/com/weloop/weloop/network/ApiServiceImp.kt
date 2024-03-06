package com.weloop.weloop.network

import com.google.gson.Gson
import com.weloop.weloop.model.Notification
import com.weloop.weloop.model.RegistrationInfo
import com.weloop.weloop.model.WidgetVisibility
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/* Created by *-----* Alexandre Thauvin *-----* */

internal object ApiServiceImp {
    private val apiService = RetrofitClient.getClient().create(ApiService::class.java)

    suspend fun requestNotification(email: String, apiKey: String): Response<Notification> {
        return apiService.requestNotification(apiKey = apiKey, email = email)
    }

    suspend fun getWidgetVisibility(email: String, apiKey: String): Response<WidgetVisibility> {
        return apiService.getWidgetVisibility(apiKey = apiKey, email)
    }

    suspend fun registerDeviceForNotification(
        registrationInfo: RegistrationInfo,
        apiKey: String
    ): Response<Unit> {
        val body = Gson().toJson(registrationInfo).toRequestBody()
        return apiService.registerDeviceForNotification(apiKey = apiKey, body = body)

    }
}
