package com.weloop.weloop.network

import com.weloop.weloop.model.WidgetPreferences
import io.reactivex.Observable

/* Created by *-----* Alexandre Thauvin *-----* */

object ApiServiceImp {
    private val apiService = RetrofitClient.getClient().create(ApiService::class.java)

    fun getWidgetPreferences(apiKey: String): Observable<WidgetPreferences> {
        return apiService.getWidgetPreferences(apiKey)
    }
}