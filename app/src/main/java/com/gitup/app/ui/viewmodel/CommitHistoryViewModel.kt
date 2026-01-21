package com.gitup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitup.app.data.model.GitCommit
import com.gitup.app.data.model.GitCommitDetail
import com.gitup.app.data.repository.GitHubRepository
import com.gitup.app.data.storage.AccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommitHistoryUiState(
    val isLoading: Boolean = false,
    val commits: List<GitCommit> = emptyList(),
    val selectedCommit: GitCommitDetail? = null,
    val isReverting: Boolean = false,
    val revertSuccess: Boolean = false,
    val error: String? = null,
    val revertPreview: GitCommitDetail? = null,
    val isLoadingPreview: Boolean = false
)

class CommitHistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GitHubRepository(application)
    private val accountManager = AccountManager(application)
    
    private val _uiState = MutableStateFlow(CommitHistoryUiState())
    val uiState: StateFlow<CommitHistoryUiState> = _uiState.asStateFlow()
    
    private var owner: String = ""
    private var repo: String = ""
    private var branch: String = ""
    
    fun loadCommits(owner: String, repo: String, branch: String) {
        this.owner = owner
        this.repo = repo
        this.branch = branch
        
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            if (account == null) {
                _uiState.value = _uiState.value.copy(error = "No active account")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.getCommits(
                token = account.token,
                owner = owner,
                repo = repo,
                branch = branch
            )
            
            result.fold(
                onSuccess = { commits ->
                    _uiState.value = CommitHistoryUiState(commits = commits)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load commits"
                    )
                }
            )
        }
    }
    
    fun loadCommitDetail(sha: String) {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            if (account == null) return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.getCommitDetail(
                token = account.token,
                owner = owner,
                repo = repo,
                sha = sha
            )
            
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedCommit = detail
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load commit detail"
                    )
                }
            )
        }
    }
    
    fun revertToCommit(commitSha: String) {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            if (account == null) {
                _uiState.value = _uiState.value.copy(error = "No active account")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isReverting = true, error = null, revertSuccess = false)
            
            val result = repository.revertToCommit(
                token = account.token,
                owner = owner,
                repo = repo,
                branch = branch,
                commitSha = commitSha
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isReverting = false,
                        revertSuccess = true,
                        error = null
                    )
                    // Automatically reload commits to show the updated state
                    kotlinx.coroutines.delay(1000) // Give GitHub a moment to process
                    loadCommits(owner, repo, branch)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isReverting = false,
                        revertSuccess = false,
                        error = error.message ?: "Failed to revert"
                    )
                }
            )
        }
    }
    
    fun clearSelectedCommit() {
        _uiState.value = _uiState.value.copy(selectedCommit = null)
    }
    
    fun clearRevertSuccess() {
        _uiState.value = _uiState.value.copy(revertSuccess = false)
    }
    
    fun loadRevertPreview(commitSha: String) {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            if (account == null) return@launch
            
            _uiState.value = _uiState.value.copy(isLoadingPreview = true)
            
            val result = repository.getCommitDetail(
                token = account.token,
                owner = owner,
                repo = repo,
                sha = commitSha
            )
            
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPreview = false,
                        revertPreview = detail
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPreview = false,
                        revertPreview = null
                    )
                }
            )
        }
    }
    
    fun clearRevertPreview() {
        _uiState.value = _uiState.value.copy(revertPreview = null)
    }
}
