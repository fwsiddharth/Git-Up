package com.gitup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitup.app.data.repository.GitHubRepository
import com.gitup.app.data.storage.AccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FileViewerUiState(
    val isLoading: Boolean = false,
    val content: String? = null,
    val downloadUrl: String? = null,
    val sha: String? = null,
    val error: String? = null,
    val commitSuccess: Boolean = false,
    val isCommitting: Boolean = false
)

class FileViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GitHubRepository(application)
    private val accountManager = AccountManager(application)
    
    private val _uiState = MutableStateFlow(FileViewerUiState())
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()
    
    fun loadFile(owner: String, repo: String, path: String, branch: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val token = accountManager.getActiveAccount()?.token
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No active account"
                )
                return@launch
            }
            
            val result = repository.getFileContent(token, owner, repo, path, branch)
            result.fold(
                onSuccess = { file ->
                    val decodedContent = repository.decodeFileContent(file.content)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        content = decodedContent,
                        downloadUrl = file.downloadUrl,
                        sha = file.sha,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load file"
                    )
                }
            )
        }
    }
    
    fun commitChanges(
        owner: String,
        repo: String,
        path: String,
        branch: String,
        content: String,
        message: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCommitting = true, error = null)
            
            val token = accountManager.getActiveAccount()?.token
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    isCommitting = false,
                    error = "No active account"
                )
                return@launch
            }
            
            val sha = _uiState.value.sha
            if (sha == null) {
                _uiState.value = _uiState.value.copy(
                    isCommitting = false,
                    error = "File SHA not available"
                )
                return@launch
            }
            
            val result = repository.uploadFile(
                token = token,
                owner = owner,
                repo = repo,
                path = path,
                content = content.toByteArray(),
                message = message,
                branch = branch,
                sha = sha
            )
            
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isCommitting = false,
                        commitSuccess = true,
                        content = content,
                        sha = response.content.sha
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isCommitting = false,
                        error = exception.message ?: "Failed to commit changes"
                    )
                }
            )
        }
    }
}
