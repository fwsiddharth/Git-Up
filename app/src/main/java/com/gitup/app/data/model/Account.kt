package com.gitup.app.data.model

data class Account(
    val id: String,
    val username: String,
    val token: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val loginMethod: String? = null // "OAuth" or "PAT", null for legacy accounts
) {
    // Helper to get login method with fallback
    fun getAuthMethod(): String = loginMethod ?: "PAT"
}
