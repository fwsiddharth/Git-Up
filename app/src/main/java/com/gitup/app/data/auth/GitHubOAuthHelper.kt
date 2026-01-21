package com.gitup.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object GitHubOAuthHelper {
    
    // GitHub OAuth App credentials
    private const val CLIENT_ID = "Ov23liu97i7G9GDy20L2"
    private const val CLIENT_SECRET = "fa1d5faf8d5068a6567e26ee0e3ebc17d6403438"
    private const val REDIRECT_URI = "gitup://callback"
    
    // GitHub OAuth URLs
    private const val AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
    private const val TOKEN_URL = "https://github.com/login/oauth/access_token"
    
    /**
     * Opens browser for GitHub OAuth login
     */
    fun startOAuthFlow(context: Context) {
        val authUrl = buildAuthUrl()
        
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        
        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
    }
    
    /**
     * Builds the GitHub authorization URL
     */
    private fun buildAuthUrl(): String {
        return Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "repo user")
            .appendQueryParameter("state", generateRandomState())
            .build()
            .toString()
    }
    
    /**
     * Exchanges authorization code for access token
     */
    suspend fun exchangeCodeForToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .build()
            
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(Exception("Failed to exchange code for token"))
            }
            
            val json = JSONObject(responseBody)
            
            if (json.has("error")) {
                val error = json.getString("error_description")
                return@withContext Result.failure(Exception(error))
            }
            
            val accessToken = json.getString("access_token")
            Result.success(accessToken)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extracts authorization code from callback URI
     */
    fun extractCodeFromUri(uri: Uri): String? {
        return uri.getQueryParameter("code")
    }
    
    /**
     * Generates random state for CSRF protection
     */
    private fun generateRandomState(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
