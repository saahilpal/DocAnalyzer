package com.nitrous.docanalyzer.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit

data class RemoteConfig(
    @SerializedName("api") val api: String,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

object RuntimeConfigManager {
    private const val TAG = "BOOT_Config"
    private const val CONFIG_URL = "https://raw.githubusercontent.com/saahilpal/document-analyzer-rag/master/dev-runtime/backend-url.json"
    private const val FALLBACK_URL = "https://doc-analyzer-api.example.com/" // Standard fallback

    private var _baseUrl: String? = null
    val baseUrl: String
        get() = _baseUrl ?: throw IllegalStateException("Base URL not initialized. Call fetchRemoteConfig() first.")

    suspend fun fetchRemoteConfig() {
        Log.d(TAG, "Starting remote config fetch from: $CONFIG_URL")
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(CONFIG_URL)
                .build()

            var attempts = 0
            while (_baseUrl == null && attempts < 3) {
                attempts++
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        Log.d(TAG, "Raw config received: $body")
                        val config = RetrofitClient.gson.fromJson(body, RemoteConfig::class.java)
                        
                        if (config?.api?.isNotBlank() == true) {
                            _baseUrl = formatBaseUrl(config.api)
                            Log.i(TAG, "Successfully fetched Base URL: $_baseUrl")
                            return@withContext
                        }
                    }
                    Log.w(TAG, "Attempt $attempts: Failed to fetch config. Code: ${response.code}")
                } catch (e: Exception) {
                    Log.e(TAG, "Attempt $attempts: Error fetching remote config", e)
                }
                if (_baseUrl == null) delay(2000)
            }

            if (_baseUrl == null) {
                Log.w(TAG, "All attempts failed. Using fallback URL: $FALLBACK_URL")
                _baseUrl = formatBaseUrl(FALLBACK_URL)
            }
        }
    }

    private fun formatBaseUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!url.endsWith("/")) url = "$url/"
        if (!url.endsWith("api/v1/")) {
            url = if (url.endsWith("/")) "${url}api/v1/" else "$url/api/v1/"
        }
        return url
    }
}
