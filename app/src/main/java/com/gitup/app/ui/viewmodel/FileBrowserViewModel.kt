package com.gitup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitup.app.data.model.RepoContent
import com.gitup.app.data.model.Repository
import com.gitup.app.data.repository.GitHubRepository
import com.gitup.app.data.storage.AccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FileBrowserUiState(
    val isLoading: Boolean = false,
    val contents: List<RepoContent> = emptyList(),
    val currentPath: String = "",
    val selectedManifest: RepoContent? = null,
    val showManifestSelected: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val error: String? = null,
    val languages: Map<String, Int> = emptyMap(),
    val isLoadingLanguages: Boolean = false
)

class FileBrowserViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GitHubRepository(application)
    private val accountManager = AccountManager(application)
    
    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()
    
    private var currentRepository: Repository? = null
    
    fun setRepository(repo: Repository) {
        currentRepository = repo
        loadLanguages()
    }
    
    private fun loadLanguages() {
        val repo = currentRepository ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLanguages = true)
            
            val account = accountManager.getActiveAccount()
            if (account != null) {
                val result = repository.getRepositoryLanguages(
                    token = account.token,
                    owner = repo.owner.login,
                    repo = repo.name
                )
                
                result.fold(
                    onSuccess = { languages ->
                        _uiState.value = _uiState.value.copy(
                            languages = languages,
                            isLoadingLanguages = false
                        )
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoadingLanguages = false)
                    }
                )
            }
        }
    }
    
    fun loadContents(path: String) {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            val repo = currentRepository
            
            if (account == null || repo == null) {
                _uiState.value = _uiState.value.copy(error = "No active account or repository")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.getRepositoryContents(
                token = account.token,
                owner = repo.owner.login,
                repo = repo.name,
                path = path,
                branch = repo.defaultBranch
            )
            
            result.fold(
                onSuccess = { contents ->
                    _uiState.value = FileBrowserUiState(
                        contents = contents.sortedWith(
                            compareBy<RepoContent> { !it.isDirectory }
                                .thenBy { it.name }
                        ),
                        currentPath = path,
                        selectedManifest = _uiState.value.selectedManifest
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load contents"
                    )
                }
            )
        }
    }
    
    fun navigateToFolder(path: String) {
        loadContents(path)
    }
    
    fun navigateUp() {
        val currentPath = _uiState.value.currentPath
        val parentPath = currentPath.substringBeforeLast("/", "")
        loadContents(parentPath)
    }
    
    fun selectManifest(manifest: RepoContent) {
        _uiState.value = _uiState.value.copy(
            selectedManifest = manifest,
            showManifestSelected = true
        )
    }
    
    fun dismissManifestMessage() {
        _uiState.value = _uiState.value.copy(showManifestSelected = false)
    }
    
    fun getSelectedManifestPath(): String? {
        return _uiState.value.selectedManifest?.path
    }
    
    fun deleteFile(content: RepoContent, commitMessage: String) {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            val repo = currentRepository
            
            if (account == null || repo == null) {
                _uiState.value = _uiState.value.copy(error = "No active account or repository")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            
            val result = repository.deleteFile(
                token = account.token,
                owner = repo.owner.login,
                repo = repo.name,
                path = content.path,
                message = commitMessage,
                sha = content.sha,
                branch = repo.defaultBranch
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deleteSuccess = true
                    )
                    // Reload the current directory
                    loadContents(_uiState.value.currentPath)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = "Failed to delete: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun clearDeleteSuccess() {
        _uiState.value = _uiState.value.copy(deleteSuccess = false)
    }
}
