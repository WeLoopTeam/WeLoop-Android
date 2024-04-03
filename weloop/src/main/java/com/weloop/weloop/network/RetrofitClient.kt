package com.weloop.weloop.network


import com.weloop.weloop.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private var retrofit: Retrofit? = null
    private const val BASE_URL = "https://api.weloop.dev"

    fun getClient(): Retrofit {
        if (retrofit == null) {
            retrofit = if (BuildConfig.DEBUG) {
                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getOkHttpClientDebug())
                    .build()
            } else {
                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getOkHttpClientRelease())
                    .build()
            }
        }
        return retrofit!!
    }

    private fun getOkHttpClientRelease(): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(null)
            .addNetworkInterceptor { chain ->

                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .build()
                )
            }
            .callTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .connectTimeout(2, TimeUnit.MINUTES)
            .build()
    }

    private fun getOkHttpClientDebug(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .cache(null)
            .addInterceptor(httpLoggingInterceptor)
            .addNetworkInterceptor { chain ->

                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .build()
                )
            }
            .callTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .connectTimeout(2, TimeUnit.MINUTES)
            .build()
    }
}
