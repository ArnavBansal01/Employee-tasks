package com.example.data.api

import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 10.0.2.2 maps to host machine's localhost when running in the Android emulator
    private const val BASE_URL = "http://10.0.2.2:5221/api/"

    @Volatile
    private var token: String? = null

    @Volatile
    private var userRole: String? = null

    @Volatile
    private var refreshToken: String? = null

    @Volatile
    private var onUnauthorizedListener: (() -> Unit)? = null

    @Volatile
    private var onTokenRefreshedListener: ((String, String) -> Unit)? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun setUserRole(role: String?) {
        userRole = role
    }

    fun setRefreshToken(newRefreshToken: String?) {
        refreshToken = newRefreshToken
    }

    fun setOnUnauthorizedListener(listener: () -> Unit) {
        onUnauthorizedListener = listener
    }

    fun setOnTokenRefreshedListener(listener: (String, String) -> Unit) {
        onTokenRefreshedListener = listener
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
        userRole?.let { requestBuilder.addHeader("X-User-Role", it) }
        val response = chain.proceed(requestBuilder.build())
        if (response.code == 401) {
            onUnauthorizedListener?.invoke()
        }
        response
    }

    private val tokenAuthenticator = Authenticator { _, response ->
        if (response.code == 401) {
            // 1. Check if the backend explicitly revoked the token/role
            try {
                val responseBodyCopy = response.peekBody(2048).string()
                if (responseBodyCopy == "Role changed." || responseBodyCopy == "Token invalidated.") {
                    // Do not attempt token refresh, let the 401 propagate to trigger logout
                    return@Authenticator null
                }
            } catch (e: Exception) {
                // Ignore peeking errors
            }

            // 2. Refresh the Firebase token using Google REST API
            val currentRefreshToken = refreshToken
            if (!currentRefreshToken.isNullOrEmpty()) {
                val newToken = refreshFirebaseToken(currentRefreshToken)
                if (newToken != null) {
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                }
            }
        }
        null
    }

    private fun refreshFirebaseToken(refreshToken: String): String? {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val formBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url("https://securetoken.googleapis.com/v1/token?key=${com.example.BuildConfig.FIREBASE_API_KEY}")
            .post(formBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!.string())
                    val newIdToken = json.optString("id_token")
                    val newRefreshToken = json.optString("refresh_token")
                    if (!newIdToken.isNullOrEmpty() && !newRefreshToken.isNullOrEmpty()) {
                        setToken(newIdToken)
                        setRefreshToken(newRefreshToken)
                        onTokenRefreshedListener?.invoke(newIdToken, newRefreshToken)
                        return newIdToken
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
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
