package com.gitup.app.data.model

import com.google.gson.annotations.SerializedName

data class GitHubUser(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url")
    val avatarUrl: String,
    val name: String?,
    val email: String?,
    val bio: String?,
    val company: String? = null,
    val location: String? = null,
    val blog: String? = null,
    @SerializedName("twitter_username")
    val twitterUsername: String? = null,
    @SerializedName("public_repos")
    val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)
