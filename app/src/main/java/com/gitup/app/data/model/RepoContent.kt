package com.gitup.app.data.model

import com.google.gson.annotations.SerializedName

data class RepoContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val type: String, // "file" or "dir"
    @SerializedName("download_url")
    val downloadUrl: String?,
    @SerializedName("html_url")
    val htmlUrl: String,
    val content: String? = null,
    val encoding: String? = null
) {
    val isDirectory: Boolean
        get() = type == "dir"
    
    val isFile: Boolean
        get() = type == "file"
}
