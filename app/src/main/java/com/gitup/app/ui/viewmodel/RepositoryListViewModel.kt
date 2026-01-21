package com.gitup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitup.app.data.model.Account
import com.gitup.app.data.model.GitHubUser
import com.gitup.app.data.model.Repository
import com.gitup.app.data.repository.GitHubRepository
import com.gitup.app.data.storage.AccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RepositoryListUiState(
    val isLoading: Boolean = false,
    val repositories: List<Repository> = emptyList(),
    val currentAccount: Account? = null,
    val user: GitHubUser? = null,
    val error: String? = null
)

class RepositoryListViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GitHubRepository(application)
    private val accountManager = AccountManager(application)
    
    private val _uiState = MutableStateFlow(RepositoryListUiState())
    val uiState: StateFlow<RepositoryListUiState> = _uiState.asStateFlow()
    
    fun loadRepositories() {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            if (account == null) {
                _uiState.value = RepositoryListUiState(error = "No active account")
                return@launch
            }
            
            _uiState.value = RepositoryListUiState(
                isLoading = true,
                currentAccount = account
            )
            
            // Load user info
            val userResult = repository.validateToken(account.token)
            val user = userResult.getOrNull()
            
            // Load repositories
            val reposResult = repository.getUserRepositories(account.token)
            
            reposResult.fold(
                onSuccess = { repos ->
                    _uiState.value = RepositoryListUiState(
                        repositories = repos,
                        currentAccount = account,
                        user = user
                    )
                },
                onFailure = { error ->
                    _uiState.value = RepositoryListUiState(
                        error = error.message ?: "Failed to load repositories",
                        currentAccount = account,
                        user = user
                    )
                }
            )
        }
    }
}
