package com.nitrous.docanalyzer.network

import android.util.Log
import com.nitrous.docanalyzer.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class SecureLogger : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val t1 = System.nanoTime()
        
        Log.d("OkHttp", String.format("--> %s %s", request.method, request.url))
        
        request.headers.forEach { (name, _) ->
            if (name.equals("Authorization", ignoreCase = true) || name.equals("Cookie", ignoreCase = true)) {
                Log.d("OkHttp", "$name: Bearer *******")
            }
        }

        val response = chain.proceed(request)
        val t2 = System.nanoTime()
        
        Log.d("OkHttp", String.format("<-- %d %s (%.1fms)", 
            response.code, response.request.url, (t2 - t1) / 1e6))

        return response
    }
}
