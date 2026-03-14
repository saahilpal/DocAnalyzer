package com.nitrous.docanalyzer.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nitrous.docanalyzer.auth.data.AuthManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var _authManager: AuthManager? = null
    private var _retrofit: Retrofit? = null
    private var _apiService: ApiService? = null
    private var _okHttpClient: OkHttpClient? = null

    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val secureLogger = SecureLogger()

    fun init(context: Context) {
        if (_authManager == null) {
            _authManager = AuthManager(context)
        }
    }

    fun rebuild() {
        val authManager = _authManager ?: return
        val baseUrl = try {
            // Use the base URL exactly as provided by RuntimeConfigManager.
            // RuntimeConfigManager already ensures it contains /api/v1/ and ends with /
            RuntimeConfigManager.baseUrl
        } catch (e: Exception) {
            return
        }

        val authInterceptor = AuthInterceptor(authManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(secureLogger)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        _okHttpClient = okHttpClient
        _retrofit = retrofit
        _apiService = retrofit.create(ApiService::class.java)
        
        android.util.Log.d("RetrofitClient", "Rebuilt with Base URL: $baseUrl")
    }

    val apiService: ApiService
        get() = _apiService ?: throw IllegalStateException("RetrofitClient not initialized")

    val okHttpClient: OkHttpClient
        get() = _okHttpClient ?: throw IllegalStateException("RetrofitClient not initialized")

    val authManager: AuthManager
        get() = _authManager ?: throw IllegalStateException("RetrofitClient not initialized")

    fun logout() {
        _authManager?.clearSession()
    }
}
