package com.laundryhub.mobile.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Builds Retrofit. Adds "Authorization: Bearer <token>" to every request. */
object ApiClient {

    private var cached: LaundryApi? = null
    private var cachedFor: String? = null

    val api: LaundryApi
        @Synchronized get() {
            val url = Session.serverUrl
            val existing = cached
            if (existing != null && cachedFor == url) return existing

            val http = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val token = Session.token
                    val withAuth = if (token != null) {
                        request.newBuilder().header("Authorization", "Bearer $token").build()
                    } else request
                    chain.proceed(withAuth)
                }
                .build()

            val created = Retrofit.Builder()
                .baseUrl(url)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LaundryApi::class.java)
            cached = created
            cachedFor = url
            return created
        }
}
