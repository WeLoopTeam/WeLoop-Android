package com.weloop.weloop.network

import com.weloop.weloop.model.WidgetPreferences
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

/* Created by *-----* Alexandre Thauvin *-----* */

interface ApiService {
    @GET("/widget/{apiKey}")
    fun getWidgetPreferences(@Path("apiKey") apiKey: String): Observable<WidgetPreferences>
}