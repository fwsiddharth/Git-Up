package com.gitup.app.data.api

import com.gitup.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {
    
    @GET("user")
    suspend fun getCurrentUser(): Response<GitHubUser>
    
    @GET("user/repos")
    suspend fun getUserRepositories(
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated"
    ): Response<List<Repository>>
    
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getRepositoryContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String = "",
        @Query("ref") branch: String? = null
    ): Response<List<RepoContent>>
    
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") branch: String? = null
    ): Response<RepoContent>
    
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun uploadFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: FileUploadRequest
    ): Response<FileUploadResponse>
    
    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: Map<String, String>
    ): Response<Unit>
    
    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") branch: String? = null,
        @Query("per_page") perPage: Int = 100
    ): Response<List<GitCommit>>
    
    @GET("repos/{owner}/{repo}/commits/{sha}")
    suspend fun getCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): Response<GitCommitDetail>
    
    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: Map<String, String>
    ): Response<Unit>
    
    @PATCH("repos/{owner}/{repo}/git/refs/{ref}")
    suspend fun updateRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref", encoded = true) ref: String,
        @Body request: UpdateRefRequest
    ): Response<Unit>
    
    @GET("repos/{owner}/{repo}/languages")
    suspend fun getRepositoryLanguages(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Map<String, Int>>
}
