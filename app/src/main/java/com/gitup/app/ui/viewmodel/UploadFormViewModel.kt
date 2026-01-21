package com.gitup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitup.app.data.model.FormField
import com.gitup.app.data.model.ManifestInfo
import com.gitup.app.data.repository.GitHubRepository
import com.gitup.app.data.repository.ManifestRepository
import com.gitup.app.data.storage.AccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UploadFormUiState(
    val isLoading: Boolean = false,
    val isLoadingManifest: Boolean = false,
    val isSuccess: Boolean = false,
    val formFields: List<FormField> = emptyList(),
    val manifestInfo: ManifestInfo? = null,
    val error: String? = null,
    val uploadedFilePath: String? = null,
    val uploadedFileSha: String? = null,
    val previousManifestContent: String? = null,
    val previousManifestSha: String? = null,
    val canRevert: Boolean = false
)

class UploadFormViewModel(application: Application) : AndroidViewModel(application) {
    
    private val githubRepository = GitHubRepository(application)
    private val manifestRepository = ManifestRepository()
    private val accountManager = AccountManager(application)
    
    private val _uiState = MutableStateFlow(UploadFormUiState())
    val uiState: StateFlow<UploadFormUiState> = _uiState.asStateFlow()
    
    private var owner: String = ""
    private var repo: String = ""
    private var branch: String = ""
    private var manifestPath: String = ""
    private var fileBytes: ByteArray? = null
    
    fun loadManifestAndGenerateForm(
        owner: String,
        repo: String,
        branch: String,
        manifestPath: String,
        fileBytes: ByteArray
    ) {
        this.owner = owner
        this.repo = repo
        this.branch = branch
        this.manifestPath = manifestPath
        this.fileBytes = fileBytes
        
        viewModelScope.launch {
            _uiState.value = UploadFormUiState(isLoadingManifest = true)
            
            val account = accountManager.getActiveAccount()
            if (account == null) {
                _uiState.value = UploadFormUiState(error = "No active account")
                return@launch
            }
            
            val result = githubRepository.getFileContent(
                token = account.token,
                owner = owner,
                repo = repo,
                path = manifestPath,
                branch = branch
            )
            
            result.fold(
                onSuccess = { repoContent ->
                    val content = githubRepository.decodeFileContent(repoContent.content)
                    if (content != null) {
                        val manifestInfo = manifestRepository.parseManifest(content)
                        if (manifestInfo != null) {
                            val formFields = manifestRepository.generateFormFields(manifestInfo)
                            _uiState.value = UploadFormUiState(
                                isLoadingManifest = false,
                                formFields = formFields,
                                manifestInfo = manifestInfo.copy(sha = repoContent.sha)
                            )
                        } else {
                            _uiState.value = UploadFormUiState(error = "Failed to parse manifest")
                        }
                    } else {
                        _uiState.value = UploadFormUiState(error = "Failed to decode manifest content")
                    }
                },
                onFailure = { error ->
                    _uiState.value = UploadFormUiState(
                        error = error.message ?: "Failed to load manifest"
                    )
                }
            )
        }
    }
    
    fun uploadWithManifest(
        fileName: String,
        fileSize: Long,
        uploadPath: String,
        formData: Map<String, Any>
    ) {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            val manifestInfo = _uiState.value.manifestInfo
            val bytes = fileBytes
            
            if (account == null || manifestInfo == null || bytes == null) {
                _uiState.value = _uiState.value.copy(error = "Missing required data")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Step 1: Upload the file
                val uploadResult = githubRepository.uploadFile(
                    token = account.token,
                    owner = owner,
                    repo = repo,
                    path = uploadPath,
                    content = bytes,
                    message = "Add $fileName via GitUp",
                    branch = branch
                )
                
                if (uploadResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to upload file: ${uploadResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                val uploadedFileSha = uploadResult.getOrNull()?.content?.sha
                
                // Step 2: Get current manifest
                val manifestResult = githubRepository.getFileContent(
                    token = account.token,
                    owner = owner,
                    repo = repo,
                    path = manifestPath,
                    branch = branch
                )
                
                if (manifestResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to fetch manifest: ${manifestResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                val manifestContent = manifestResult.getOrNull()
                val currentManifest = githubRepository.decodeFileContent(manifestContent?.content)
                val previousManifestSha = manifestContent?.sha
                
                if (currentManifest == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to decode manifest"
                    )
                    return@launch
                }
                
                // Step 3: Generate new entry
                val newEntry = manifestRepository.generateManifestEntry(
                    formData = formData,
                    fileName = fileName,
                    filePath = uploadPath,
                    fileSize = fileSize,
                    baseUrl = manifestInfo.baseUrl,
                    manifestInfo = manifestInfo
                )
                
                // Step 4: Update manifest
                val selectedCategory = formData["category"] as? String
                val actualCategory = manifestInfo.categories.find { 
                    it.equals(selectedCategory, ignoreCase = true) 
                } ?: selectedCategory?.lowercase() ?: manifestInfo.categories.firstOrNull() ?: "default"
                
                val updatedManifest = manifestRepository.updateManifest(
                    currentContent = currentManifest,
                    newEntry = newEntry,
                    category = actualCategory
                )
                
                // Step 5: Upload updated manifest
                val manifestUploadResult = githubRepository.uploadFile(
                    token = account.token,
                    owner = owner,
                    repo = repo,
                    path = manifestPath,
                    content = updatedManifest.toByteArray(),
                    message = "Update manifest: Add $fileName via GitUp",
                    branch = branch,
                    sha = manifestContent?.sha
                )
                
                if (manifestUploadResult.isSuccess) {
                    fileBytes = null
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        uploadedFilePath = uploadPath,
                        uploadedFileSha = uploadedFileSha,
                        previousManifestContent = currentManifest,
                        previousManifestSha = previousManifestSha,
                        canRevert = true
                    )
                } else {
                    val errorMsg = manifestUploadResult.exceptionOrNull()?.message ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update manifest: $errorMsg\n\nFile was uploaded but manifest update failed."
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun revertChanges() {
        viewModelScope.launch {
            val account = accountManager.getActiveAccount()
            val state = _uiState.value
            
            if (account == null || !state.canRevert) {
                _uiState.value = _uiState.value.copy(error = "Cannot revert changes")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Step 1: Delete the uploaded file
                if (state.uploadedFilePath != null && state.uploadedFileSha != null) {
                    val deleteResult = githubRepository.deleteFile(
                        token = account.token,
                        owner = owner,
                        repo = repo,
                        path = state.uploadedFilePath,
                        message = "Revert: Remove file via GitUp",
                        sha = state.uploadedFileSha,
                        branch = branch
                    )
                    
                    if (deleteResult.isFailure) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to delete file: ${deleteResult.exceptionOrNull()?.message}"
                        )
                        return@launch
                    }
                }
                
                // Step 2: Restore previous manifest
                if (state.previousManifestContent != null) {
                    // Get current manifest SHA
                    val manifestResult = githubRepository.getFileContent(
                        token = account.token,
                        owner = owner,
                        repo = repo,
                        path = manifestPath,
                        branch = branch
                    )
                    
                    val currentManifestSha = manifestResult.getOrNull()?.sha
                    
                    val restoreResult = githubRepository.uploadFile(
                        token = account.token,
                        owner = owner,
                        repo = repo,
                        path = manifestPath,
                        content = state.previousManifestContent.toByteArray(),
                        message = "Revert: Restore previous manifest via GitUp",
                        branch = branch,
                        sha = currentManifestSha
                    )
                    
                    if (restoreResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            canRevert = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to restore manifest: ${restoreResult.exceptionOrNull()?.message}"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Revert error: ${e.message}"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clear file bytes to prevent memory leak
        fileBytes = null
    }
}
