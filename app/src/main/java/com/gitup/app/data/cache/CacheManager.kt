package com.gitup.app.data.cache

import android.content.Context
import com.gitup.app.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CacheManager(private val context: Context) {
    
    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "gitup_cache")
    
    // In-memory cache for instant access
    private val memoryCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    
    // Cache durations
    private val shortCacheDuration = TimeUnit.MINUTES.toMillis(5)  // 5 minutes for frequently changing data
    private val longCacheDuration = TimeUnit.MINUTES.toMillis(30)  // 30 minutes for stable data
    
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long
    )
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        // Clear expired cache on init
        clearExpiredCache()
    }
    
    // Generic save to both memory and disk cache
    private suspend fun <T> saveToCache(key: String, data: T, useLongCache: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            // Save to memory cache
            memoryCache[key] = CacheEntry(data as Any, System.currentTimeMillis())
            
            // Save to disk cache asynchronously
            val file = File(cacheDir, key)
            val json = gson.toJson(data)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Generic load from memory first, then disk
    private suspend inline fun <reified T> loadFromCache(key: String, useLongCache: Boolean = false): T? = withContext(Dispatchers.IO) {
        try {
            val cacheDuration = if (useLongCache) longCacheDuration else shortCacheDuration
            val now = System.currentTimeMillis()
            
            // Check memory cache first (instant)
            memoryCache[key]?.let { entry ->
                if (now - entry.timestamp <= cacheDuration) {
                    return@withContext entry.data as? T
                } else {
                    memoryCache.remove(key)
                }
            }
            
            // Check disk cache
            val file = File(cacheDir, key)
            if (!file.exists()) return@withContext null
            
            // Check if disk cache is expired
            val lastModified = file.lastModified()
            if (now - lastModified > cacheDuration) {
                file.delete()
                return@withContext null
            }
            
            // Load from disk and update memory cache
            val json = file.readText()
            val type = object : TypeToken<T>() {}.type
            val data: T = gson.fromJson(json, type)
            memoryCache[key] = CacheEntry(data as Any, lastModified)
            data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // User cache
    suspend fun cacheUser(token: String, user: GitHubUser) {
        saveToCache("user_$token", user, useLongCache = true)
    }
    
    suspend fun getCachedUser(token: String): GitHubUser? {
        return loadFromCache("user_$token", useLongCache = true)
    }
    
    // Repositories cache
    suspend fun cacheRepositories(token: String, repos: List<Repository>) {
        saveToCache("repos_$token", repos, useLongCache = false)
    }
    
    suspend fun getCachedRepositories(token: String): List<Repository>? {
        return loadFromCache("repos_$token", useLongCache = false)
    }
    
    // Repository contents cache
    suspend fun cacheRepoContents(owner: String, repo: String, path: String, branch: String, contents: List<RepoContent>) {
        val key = "contents_${owner}_${repo}_${branch}_${path.replace("/", "_")}"
        saveToCache(key, contents, useLongCache = false)
    }
    
    suspend fun getCachedRepoContents(owner: String, repo: String, path: String, branch: String): List<RepoContent>? {
        val key = "contents_${owner}_${repo}_${branch}_${path.replace("/", "_")}"
        return loadFromCache(key, useLongCache = false)
    }
    
    // File content cache
    suspend fun cacheFileContent(owner: String, repo: String, path: String, branch: String, content: RepoContent) {
        val key = "file_${owner}_${repo}_${branch}_${path.replace("/", "_")}"
        saveToCache(key, content, useLongCache = true)
    }
    
    suspend fun getCachedFileContent(owner: String, repo: String, path: String, branch: String): RepoContent? {
        val key = "file_${owner}_${repo}_${branch}_${path.replace("/", "_")}"
        return loadFromCache(key, useLongCache = true)
    }
    
    // Commits cache
    suspend fun cacheCommits(owner: String, repo: String, branch: String, commits: List<GitCommit>) {
        val key = "commits_${owner}_${repo}_$branch"
        saveToCache(key, commits, useLongCache = false)
    }
    
    suspend fun getCachedCommits(owner: String, repo: String, branch: String): List<GitCommit>? {
        val key = "commits_${owner}_${repo}_$branch"
        return loadFromCache(key, useLongCache = false)
    }
    
    // Languages cache
    suspend fun cacheLanguages(owner: String, repo: String, languages: Map<String, Int>) {
        val key = "languages_${owner}_$repo"
        saveToCache(key, languages, useLongCache = true)
    }
    
    suspend fun getCachedLanguages(owner: String, repo: String): Map<String, Int>? {
        val key = "languages_${owner}_$repo"
        return loadFromCache(key, useLongCache = true)
    }
    
    // Clear specific cache
    suspend fun clearUserCache(token: String) = withContext(Dispatchers.IO) {
        memoryCache.remove("user_$token")
        memoryCache.remove("repos_$token")
        File(cacheDir, "user_$token").delete()
        File(cacheDir, "repos_$token").delete()
    }
    
    suspend fun clearRepoCache(owner: String, repo: String) = withContext(Dispatchers.IO) {
        // Clear memory cache
        memoryCache.keys.removeAll { it.contains("${owner}_$repo") }
        
        // Clear disk cache
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.contains("${owner}_$repo")) {
                file.delete()
            }
        }
    }
    
    // Clear all cache
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        memoryCache.clear()
        cacheDir.listFiles()?.forEach { it.delete() }
    }
    
    // Clear expired cache
    private fun clearExpiredCache() {
        try {
            val now = System.currentTimeMillis()
            
            // Clear expired memory cache
            memoryCache.entries.removeIf { (_, entry) ->
                now - entry.timestamp > longCacheDuration
            }
            
            // Clear expired disk cache
            cacheDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > longCacheDuration) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Get cache size
    fun getCacheSize(): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
    
    // Preload cache (call this on app start for smooth experience)
    suspend fun preloadCache(token: String) = withContext(Dispatchers.IO) {
        try {
            // Preload user and repos into memory
            getCachedUser(token)
            getCachedRepositories(token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
