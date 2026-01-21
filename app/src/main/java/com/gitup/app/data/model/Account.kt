package com.gitup.app.data.model

data class Account(
    val id: String,
    val username: String,
    val token: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = false
)
