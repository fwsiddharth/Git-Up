package com.gitup.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitup.app.data.model.RepoContent
import com.gitup.app.data.model.Repository
import com.gitup.app.ui.theme.Spacing
import com.gitup.app.ui.viewmodel.FileBrowserViewModel
import com.gitup.app.ui.utils.formatFileSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModernFileBrowserScreen(
    repository: Repository,
    onNavigateBack: () -> Unit,
    onNavigateToUploadForm: (String, ByteArray, String, String, String, String, String) -> Unit,
    onNavigateToCommitHistory: (String, String, String) -> Unit,
    viewModel: FileBrowserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<RepoContent?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                
                val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: it.lastPathSegment ?: "file"
                
                if (bytes != null) {
                    val uploadPath = if (uiState.currentPath.isEmpty()) {
                        fileName
                    } else {
                        "${uiState.currentPath}/$fileName"
                    }
                    
                    val manifestPath = viewModel.getSelectedManifestPath()
                    if (manifestPath != null) {
                        onNavigateToUploadForm(
                            fileName,
                            bytes,
                            uploadPath,
                            repository.owner.login,
                            repository.name,
                            repository.defaultBranch,
                            manifestPath
                        )
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Please select a manifest.json file first",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error: ${e.message}")
                }
            }
        }
    }
    
    LaunchedEffect(repository) {
        viewModel.setRepository(repository)
        viewModel.loadContents("")
    }
    
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            snackbarHostState.showSnackbar("File deleted successfully")
            viewModel.clearDeleteSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            repository.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.currentPath.isEmpty()) "/" else "/${uiState.currentPath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentPath.isEmpty()) {
                            onNavigateBack()
                        } else {
                            viewModel.navigateUp()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onNavigateToCommitHistory(
                            repository.owner.login,
                            repository.name,
                            repository.defaultBranch
                        )
                    }) {
                        Icon(Icons.Default.History, "Commit history")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.selectedManifest != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    icon = { Icon(Icons.Default.Upload, "Upload") },
                    text = { Text("Upload File") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Manifest selection banner
                AnimatedVisibility(
                    visible = uiState.selectedManifest != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.m),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Manifest: ${uiState.selectedManifest?.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                when {
                    uiState.isLoading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            items(5) {
                                FileItemSkeleton()
                            }
                        }
                    }
                    uiState.error != null -> {
                        EmptyState(
                            icon = Icons.Default.Error,
                            title = "Couldn't load contents",
                            subtitle = uiState.error ?: "Something went wrong",
                            actionLabel = "Retry",
                            onActionClick = { viewModel.loadContents(uiState.currentPath) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    uiState.contents.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.FolderOpen,
                            title = "Empty folder",
                            subtitle = "No files here yet",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            items(uiState.contents) { content ->
                                ModernFileItem(
                                    content = content,
                                    isManifestSelected = content.path == uiState.selectedManifest?.path,
                                    onClick = {
                                        if (content.isDirectory) {
                                            viewModel.navigateToFolder(content.path)
                                        } else if (content.name.endsWith(".json")) {
                                            viewModel.selectManifest(content)
                                        }
                                    },
                                    onLongClick = {
                                        if (content.isFile) {
                                            showDeleteDialog = content
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete dialog
    showDeleteDialog?.let { content ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete file?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Are you sure you want to delete:")
                    Text(
                        text = content.name,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "⚠️ This action cannot be undone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(content, "Delete ${content.name}")
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (uiState.isDeleting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Deleting...") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Deleting file...")
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModernFileItem(
    content: RepoContent,
    isManifestSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 3.dp
        ),
        colors = if (isManifestSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    content.isDirectory -> Icons.Default.Folder
                    content.name.endsWith(".json") -> Icons.Default.Description
                    content.name.endsWith(".png") || content.name.endsWith(".jpg") -> Icons.Default.Image
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (content.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = content.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (content.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (content.isFile) {
                    Text(
                        text = formatFileSize(content.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (content.isDirectory) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (isManifestSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
