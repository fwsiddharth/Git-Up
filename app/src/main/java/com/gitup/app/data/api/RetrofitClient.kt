package com.gitup.app.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private const val BASE_URL = "https://api.github.com/"
    
    fun createApiService(token: String): GitHubApiService {
        // Trim token to remove any whitespace, newlines, or other invisible characters
        val cleanToken = token.trim()
        
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "token $cleanToken")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()
            chain.proceed(request)
        }
        
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        // Only add logging in debug builds
        // Note: Logging is disabled by default for production
        // Uncomment the lines below if you need to debug network issues
        /*
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        clientBuilder.addInterceptor(loggingInterceptor)
        */
        
        val client = clientBuilder.build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(GitHubApiService::class.java)
    }
}
