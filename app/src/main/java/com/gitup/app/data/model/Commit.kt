package com.gitup.app.data.model

import com.google.gson.annotations.SerializedName

data class GitCommit(
    val sha: String,
    val commit: GitCommitInfo,
    val author: GitHubUser?,
    val committer: GitHubUser?,
    val parents: List<ParentCommit>
)

data class GitCommitInfo(
    val message: String,
    val author: CommitAuthor,
    val committer: CommitAuthor
)

data class CommitAuthor(
    val name: String,
    val email: String,
    val date: String
)

data class ParentCommit(
    val sha: String,
    val url: String
)

data class GitCommitDetail(
    val sha: String,
    val commit: GitCommitInfo,
    val author: GitHubUser?,
    val committer: GitHubUser?,
    val parents: List<ParentCommit>,
    val files: List<CommitFile>?
)

data class CommitFile(
    val filename: String,
    val status: String,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    @SerializedName("blob_url")
    val blobUrl: String?,
    @SerializedName("raw_url")
    val rawUrl: String?,
    val patch: String?
)

data class UpdateRefRequest(
    val sha: String,
    val force: Boolean
)
