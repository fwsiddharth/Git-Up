package com.gitup.app.data.model

data class FileUploadRequest(
    val message: String,
    val content: String, // Base64 encoded
    val branch: String,
    val sha: String? = null // Required for updates
)

data class FileUploadResponse(
    val content: FileContent,
    val commit: Commit
)

data class FileContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long
)

data class Commit(
    val sha: String,
    val message: String
)
