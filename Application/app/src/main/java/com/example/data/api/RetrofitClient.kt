package com.example.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 10.0.2.2 maps to host machine's localhost when running in the Android emulator
    private const val BASE_URL = "http://10.0.2.2:5221/api/"

    @Volatile
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    @Volatile
    private var onUnauthorizedListener: (() -> Unit)? = null

    fun setOnUnauthorizedListener(listener: () -> Unit) {
        onUnauthorizedListener = listener
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
        val response = chain.proceed(requestBuilder.build())
        if (response.code == 401) {
            onUnauthorizedListener?.invoke()
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
