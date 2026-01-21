package com.gitup.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitup.app.data.auth.GitHubOAuthHelper
import com.gitup.app.data.model.Account
import com.gitup.app.data.repository.GitHubRepository
import com.gitup.app.data.storage.AccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AddAccountUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isOAuthFlow: Boolean = false
)

class AddAccountViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GitHubRepository(application)
    private val accountManager = AccountManager(application)
    
    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()
    
    fun addAccount(token: String, isOAuth: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = AddAccountUiState(isLoading = true)
            
            val result = repository.validateToken(token)
            
            result.fold(
                onSuccess = { user ->
                    val account = Account(
                        id = UUID.randomUUID().toString(),
                        username = user.login,
                        token = token,
                        avatarUrl = user.avatarUrl,
                        isActive = !accountManager.hasAccounts(), // First account is active
                        loginMethod = if (isOAuth) "OAuth" else "PAT"
                    )
                    accountManager.saveAccount(account)
                    _uiState.value = AddAccountUiState(isSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = AddAccountUiState(
                        error = error.message ?: "Failed to validate token"
                    )
                }
            )
        }
    }
    
    fun startOAuthFlow() {
        _uiState.value = AddAccountUiState(isOAuthFlow = true)
    }
    
    fun handleOAuthCallback(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = AddAccountUiState(isLoading = true)
            
            val code = GitHubOAuthHelper.extractCodeFromUri(uri)
            
            if (code == null) {
                _uiState.value = AddAccountUiState(
                    error = "Failed to get authorization code"
                )
                return@launch
            }
            
            // Exchange code for token
            val tokenResult = GitHubOAuthHelper.exchangeCodeForToken(code)
            
            tokenResult.fold(
                onSuccess = { token ->
                    // Validate token and create account (mark as OAuth)
                    addAccount(token, isOAuth = true)
                },
                onFailure = { error ->
                    _uiState.value = AddAccountUiState(
                        error = error.message ?: "Failed to exchange code for token"
                    )
                }
            )
        }
    }
}
