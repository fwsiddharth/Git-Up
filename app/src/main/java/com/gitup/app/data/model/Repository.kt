package com.gitup.app.data.model

import com.google.gson.annotations.SerializedName

data class Repository(
    val id: Long,
    val name: String,
    @SerializedName("full_name")
    val fullName: String,
    val owner: Owner,
    val private: Boolean,
    @SerializedName("default_branch")
    val defaultBranch: String,
    val description: String?,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("stargazers_count")
    val stars: Int = 0,
    @SerializedName("forks_count")
    val forks: Int = 0,
    @SerializedName("watchers_count")
    val watchers: Int = 0,
    val language: String? = null,
    @SerializedName("open_issues_count")
    val openIssues: Int = 0,
    val size: Int = 0
)

data class Owner(
    val login: String,
    @SerializedName("avatar_url")
    val avatarUrl: String
)
