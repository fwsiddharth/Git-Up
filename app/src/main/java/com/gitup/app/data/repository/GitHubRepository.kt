package com.gitup.app.data.repository

import android.content.Context
import android.util.Base64
import com.gitup.app.data.api.GitHubApiService
import com.gitup.app.data.api.RetrofitClient
import com.gitup.app.data.cache.CacheManager
import com.gitup.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitHubRepository(context: Context) {
    
    private val cacheManager = CacheManager(context)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    suspend fun validateToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            // Check cache first (instant from memory)
            val cachedUser = cacheManager.getCachedUser(token)
            if (cachedUser != null) {
                return@withContext Result.success(cachedUser)
            }
            
            // Fetch from API
            val api = RetrofitClient.createApiService(token)
            val response = api.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                // Cache the result asynchronously
                cacheManager.cacheUser(token, user)
                Result.success(user)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Invalid token. Please check your GitHub token."
                    403 -> "Access denied. Your token may have expired."
                    404 -> "GitHub API not found. Please try again."
                    else -> "Authentication failed (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Unable to resolve host") == true -> 
                    "Network error. Check your internet connection."
                e.message?.contains("timeout") == true -> 
                    "Connection timeout. Please try again."
                e.message?.contains("Failed to connect") == true -> 
                    "Network error. Check your internet connection."
                else -> 
                    "Network error. Please try again."
            }
            Result.failure(Exception(errorMsg))
        }
    }
    
    suspend fun getUserRepositories(token: String, forceRefresh: Boolean = false): Result<List<Repository>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first (instant from memory) unless force refresh
            if (!forceRefresh) {
                val cachedRepos = cacheManager.getCachedRepositories(token)
                if (cachedRepos != null) {
                    // Return cached data immediately, refresh in background
                    backgroundScope.launch {
                        try {
                            val api = RetrofitClient.createApiService(token)
                            val response = api.getUserRepositories()
                            if (response.isSuccessful && response.body() != null) {
                                cacheManager.cacheRepositories(token, response.body()!!)
                            }
                        } catch (e: Exception) {
                            // Silent fail for background refresh
                        }
                    }
                    return@withContext Result.success(cachedRepos)
                }
            }
            
            // Fetch from API
            val api = RetrofitClient.createApiService(token)
            val response = api.getUserRepositories()
            if (response.isSuccessful && response.body() != null) {
                val repos = response.body()!!
                // Cache the result asynchronously
                cacheManager.cacheRepositories(token, repos)
                Result.success(repos)
            } else {
                Result.failure(Exception("Failed to fetch repositories"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRepositoryContents(
        token: String,
        owner: String,
        repo: String,
        path: String = "",
        branch: String? = null,
        forceRefresh: Boolean = false
    ): Result<List<RepoContent>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first unless force refresh
            if (!forceRefresh) {
                val cachedContents = cacheManager.getCachedRepoContents(owner, repo, path, branch ?: "main")
                if (cachedContents != null) {
                    return@withContext Result.success(cachedContents)
                }
            }
            
            // Fetch from API
            val api = RetrofitClient.createApiService(token)
            val response = api.getRepositoryContents(owner, repo, path, branch)
            if (response.isSuccessful && response.body() != null) {
                val contents = response.body()!!
                // Cache the result asynchronously
                cacheManager.cacheRepoContents(owner, repo, path, branch ?: "main", contents)
                Result.success(contents)
            } else {
                Result.failure(Exception("Failed to fetch contents"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFileContent(
        token: String,
        owner: String,
        repo: String,
        path: String,
        branch: String? = null,
        forceRefresh: Boolean = false
    ): Result<RepoContent> = withContext(Dispatchers.IO) {
        try {
            // Check cache first unless force refresh
            if (!forceRefresh) {
                val cachedFile = cacheManager.getCachedFileContent(owner, repo, path, branch ?: "main")
                if (cachedFile != null) {
                    return@withContext Result.success(cachedFile)
                }
            }
            
            // Fetch from API
            val api = RetrofitClient.createApiService(token)
            val response = api.getFileContent(owner, repo, path, branch)
            if (response.isSuccessful && response.body() != null) {
                val file = response.body()!!
                // Cache the result asynchronously
                cacheManager.cacheFileContent(owner, repo, path, branch ?: "main", file)
                Result.success(file)
            } else {
                Result.failure(Exception("Failed to fetch file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadFile(
        token: String,
        owner: String,
        repo: String,
        path: String,
        content: ByteArray,
        message: String,
        branch: String,
        sha: String? = null
    ): Result<FileUploadResponse> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.createApiService(token)
            val encodedContent = Base64.encodeToString(content, Base64.NO_WRAP)
            val request = FileUploadRequest(
                message = message,
                content = encodedContent,
                branch = branch,
                sha = sha
            )
            val response = api.uploadFile(owner, repo, path, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to upload file: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteFile(
        token: String,
        owner: String,
        repo: String,
        path: String,
        message: String,
        sha: String,
        branch: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.createApiService(token)
            val request = mapOf(
                "message" to message,
                "sha" to sha,
                "branch" to branch
            )
            val response = api.deleteFile(owner, repo, path, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete file: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCommits(
        token: String,
        owner: String,
        repo: String,
        branch: String? = null,
        forceRefresh: Boolean = false
    ): Result<List<GitCommit>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first unless force refresh
            if (!forceRefresh) {
                val cachedCommits = cacheManager.getCachedCommits(owner, repo, branch ?: "main")
                if (cachedCommits != null) {
                    return@withContext Result.success(cachedCommits)
                }
            }
            
            // Fetch from API
            val api = RetrofitClient.createApiService(token)
            val response = api.getCommits(owner, repo, branch)
            if (response.isSuccessful && response.body() != null) {
                val commits = response.body()!!
                // Cache the result asynchronously
                cacheManager.cacheCommits(owner, repo, branch ?: "main", commits)
                Result.success(commits)
            } else {
                Result.failure(Exception("Failed to fetch commits"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRepositoryLanguages(
        token: String,
        owner: String,
        repo: String,
        forceRefresh: Boolean = false
    ): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first unless force refresh
            if (!forceRefresh) {
                val cachedLanguages = cacheManager.getCachedLanguages(owner, repo)
                if (cachedLanguages != null) {
                    return@withContext Result.success(cachedLanguages)
                }
            }
            
            // Fetch from API
            val api = RetrofitClient.createApiService(token)
            val response = api.getRepositoryLanguages(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                val languages = response.body()!!
                // Cache the result asynchronously
                cacheManager.cacheLanguages(owner, repo, languages)
                Result.success(languages)
            } else {
                Result.failure(Exception("Failed to fetch languages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Clear cache methods
    suspend fun clearUserCache(token: String) {
        cacheManager.clearUserCache(token)
    }
    
    suspend fun clearRepoCache(owner: String, repo: String) {
        cacheManager.clearRepoCache(owner, repo)
    }
    
    suspend fun clearAllCache() {
        cacheManager.clearAllCache()
    }
    
    // Preload cache for smooth experience
    suspend fun preloadCache(token: String) {
        cacheManager.preloadCache(token)
    }
    
    suspend fun getCommitDetail(
        token: String,
        owner: String,
        repo: String,
        sha: String
    ): Result<GitCommitDetail> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.createApiService(token)
            val response = api.getCommit(owner, repo, sha)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch commit detail"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun revertToCommit(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        commitSha: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.createApiService(token)
            
            // First, verify the commit exists
            val commitCheck = api.getCommit(owner, repo, commitSha)
            if (!commitCheck.isSuccessful) {
                val errorMsg = when (commitCheck.code()) {
                    404 -> "Commit not found. Please check the commit SHA."
                    401 -> "Authentication failed. Please check your token."
                    403 -> "Access denied. Your token needs 'repo' scope."
                    else -> "Failed to verify commit: ${commitCheck.message()}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }
            
            // Now update the ref to point to the target commit
            val request = UpdateRefRequest(
                sha = commitSha,
                force = true
            )
            
            val refPath = "heads/$branch"
            val response = api.updateRef(owner, repo, refPath, request)
            
            if (response.isSuccessful) {
                // Verify the revert worked by checking the branch now points to the target commit
                kotlinx.coroutines.delay(500) // Give GitHub a moment
                val verifyResponse = api.getCommits(owner, repo, branch, 1)
                
                if (verifyResponse.isSuccessful) {
                    val latestCommit = verifyResponse.body()?.firstOrNull()
                    if (latestCommit?.sha == commitSha) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Revert completed but verification failed. The branch may not have updated correctly. Please refresh and check."))
                    }
                } else {
                    // Revert succeeded but we couldn't verify - still return success
                    Result.success(Unit)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                
                // Parse error message for better user feedback
                val errorMsg = when (response.code()) {
                    401 -> "Authentication failed. Please re-add your GitHub account."
                    403 -> "Permission denied. Your token needs 'repo' access with write permissions."
                    404 -> "Branch '$branch' not found in repository."
                    422 -> {
                        if (errorBody?.contains("protected") == true) {
                            "Branch '$branch' is protected. Disable branch protection to revert."
                        } else {
                            "Invalid request. The commit SHA or branch may be invalid."
                        }
                    }
                    else -> "Failed to revert (${response.code()}): ${response.message()}"
                }
                
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Unable to resolve host") == true -> 
                    "Network error. Please check your internet connection."
                e.message?.contains("timeout") == true -> 
                    "Request timed out. Please try again."
                else -> 
                    "Error: ${e.message ?: "Unknown error occurred"}"
            }
            Result.failure(Exception(errorMsg))
        }
    }
    
    fun decodeFileContent(content: String?): String? {
        if (content == null) return null
        return try {
            val decoded = Base64.decode(content.replace("\n", ""), Base64.DEFAULT)
            String(decoded)
        } catch (e: Exception) {
            null
        }
    }
}
