package com.weloop.weloop.network

import com.google.gson.JsonSyntaxException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.net.UnknownHostException

internal class NetworkInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: UnknownHostException) {
            Timber.e("IOException, no internet or WeLoop is unreachable")
            return Response.Builder()
                .request(chain.request())
                .protocol(chain.connection()?.protocol() ?: okhttp3.Protocol.HTTP_1_1)
                .code(503) // Service Unavailable
                .body("Host is unreachable".toResponseBody(null))
                .message("Host Unreachable")
                .build()
        } catch (e: JsonSyntaxException){
            Timber.e("IOException, no internet or WeLoop is unreachable")
            return Response.Builder()
                .request(chain.request())
                .protocol(chain.connection()?.protocol() ?: okhttp3.Protocol.HTTP_1_1)
                .code(409) // Service Unavailable
                .body("Json exception".toResponseBody(null))
                .message("json exception")
                .build()
        }
    }
}
