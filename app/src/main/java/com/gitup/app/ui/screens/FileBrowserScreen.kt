package com.gitup.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitup.app.data.model.RepoContent
import com.gitup.app.data.model.Repository
import com.gitup.app.ui.components.EmptyState
import com.gitup.app.ui.components.FileItemSkeleton
import com.gitup.app.ui.theme.Spacing
import com.gitup.app.ui.theme.AccentColors
import com.gitup.app.ui.viewmodel.FileBrowserViewModel
import com.gitup.app.ui.utils.formatFileSize
import com.gitup.app.ui.utils.getLanguageColor
import com.gitup.app.ui.utils.formatCount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    repository: Repository,
    onNavigateBack: () -> Unit,
    onNavigateToUploadForm: (String, ByteArray, String, String, String, String, String) -> Unit,
    onNavigateToCommitHistory: (String, String, String) -> Unit,
    onNavigateToFileViewer: (String, String, String, String) -> Unit,
    viewModel: FileBrowserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<RepoContent?>(null) }
    var showFileInfoDialog by remember { mutableStateOf<RepoContent?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                
                // Get actual filename from URI
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
                    
                    // Auto-detect manifest.json in root
                    val manifestPath = "manifest.json"
                    
                    // Navigate to upload form
                    onNavigateToUploadForm(
                        fileName,
                        bytes,
                        uploadPath,
                        repository.owner.login,
                        repository.name,
                        repository.defaultBranch,
                        manifestPath
                    )
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error reading file: ${e.message}")
                }
            }
        }
    }
    
    LaunchedEffect(repository) {
        viewModel.setRepository(repository)
        viewModel.loadContents("")
    }
    
    LaunchedEffect(uiState.showManifestSelected) {
        if (uiState.showManifestSelected) {
            snackbarHostState.showSnackbar(
                "Manifest selected: ${uiState.selectedManifest?.name}",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissManifestMessage()
        }
    }
    
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            snackbarHostState.showSnackbar(
                "File deleted successfully",
                duration = SnackbarDuration.Short
            )
            viewModel.clearDeleteSuccess()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = repository.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Breadcrumb path
                        Text(
                            text = if (uiState.currentPath.isEmpty()) "/" else "/${uiState.currentPath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View commit history"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.currentPath.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    icon = { Icon(Icons.Default.Upload, null) },
                    text = { Text("Upload File") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                // Content area
                when {
                    uiState.isLoading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            items(6) {
                                FileItemSkeleton()
                            }
                        }
                    }
                    uiState.error != null -> {
                        EmptyState(
                            icon = Icons.Default.Error,
                            title = "Error Loading Files",
                            subtitle = uiState.error ?: "Unknown error",
                            actionLabel = "Retry",
                            onActionClick = { viewModel.loadContents(uiState.currentPath) }
                        )
                    }
                    uiState.contents.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.FolderOpen,
                            title = "Empty Folder",
                            subtitle = "This folder doesn't contain any files or folders"
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            items(
                                items = uiState.contents,
                                key = { it.path }
                            ) { content ->
                                FileContentItem(
                                    content = content,
                                    onClick = {
                                        if (content.isDirectory) {
                                            viewModel.navigateToFolder(content.path)
                                        } else {
                                            // Navigate to file viewer
                                            onNavigateToFileViewer(
                                                repository.owner.login,
                                                repository.name,
                                                content.path,
                                                repository.defaultBranch
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (content.isFile) {
                                            showDeleteDialog = content
                                        }
                                    },
                                    onMenuAction = { action ->
                                        when (action) {
                                            "download" -> {
                                                content.downloadUrl?.let { url ->
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        data = android.net.Uri.parse(url)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            }
                                            "copy_path" -> {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("File Path", content.path)
                                                clipboard.setPrimaryClip(clip)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Path copied to clipboard")
                                                }
                                            }
                                            "share" -> {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, content.htmlUrl ?: content.path)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share file"))
                                            }
                                            "info" -> {
                                                showFileInfoDialog = content
                                            }
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
    
    // Delete confirmation dialog
    showDeleteDialog?.let { content ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete file?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete:")
                    Text(
                        text = content.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ This action cannot be undone. The file will be permanently deleted from the repository.",
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
    
    // Deleting progress dialog
    if (uiState.isDeleting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Deleting...") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("Deleting file from repository...")
                }
            },
            confirmButton = {}
        )
    }
    
    // File info dialog
    showFileInfoDialog?.let { content ->
        AlertDialog(
            onDismissRequest = { showFileInfoDialog = null },
            icon = { Icon(Icons.Default.Info, null, tint = AccentColors.blue) },
            title = { Text("File Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow("Name", content.name)
                    InfoRow("Path", content.path)
                    InfoRow("Size", formatFileSize(content.size))
                    InfoRow("Type", content.type)
                    if (content.sha != null) {
                        InfoRow("SHA", content.sha.take(8) + "...")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileInfoDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileContentItem(
    content: RepoContent,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMenuAction: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in colored circle - smaller
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (content.isDirectory) {
                    AccentColors.blue.copy(alpha = 0.15f)
                } else {
                    getFileIconColor(content.name).copy(alpha = 0.15f)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            content.isDirectory -> Icons.Default.Folder
                            content.name.endsWith(".json") -> Icons.Default.Description
                            content.name.endsWith(".md") -> Icons.Default.Article
                            content.name.endsWith(".kt") -> Icons.Default.Code
                            content.name.endsWith(".java") -> Icons.Default.Code
                            content.name.endsWith(".xml") -> Icons.Default.Code
                            content.name.endsWith(".gif") || 
                            content.name.endsWith(".png") || 
                            content.name.endsWith(".jpg") -> Icons.Default.Image
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = if (content.isDirectory) {
                            AccentColors.blue
                        } else {
                            getFileIconColor(content.name)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = content.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (content.isFile) {
                    Text(
                        text = formatFileSize(content.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Three dots menu for files
            if (content.isFile) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.widthIn(min = 200.dp, max = 240.dp),
                        offset = DpOffset(x = 0.dp, y = 0.dp)
                    ) {
                        // Open
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Open",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                showMenu = false
                                onClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = AccentColors.blue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        // Download
                        if (content.downloadUrl != null) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Download",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onMenuAction("download")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = AccentColors.green,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        
                        // Copy Path
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Copy Path",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                showMenu = false
                                onMenuAction("copy_path")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = AccentColors.purple,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        // Share URL
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Share URL",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                showMenu = false
                                onMenuAction("share")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = AccentColors.teal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        // File Info
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "File Info",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                showMenu = false
                                onMenuAction("info")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AccentColors.blue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // Delete
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Delete",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onLongClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@Composable
fun RepositoryHeaderCard(
    repository: Repository,
    languages: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = repository.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (repository.description != null) {
                    Text(
                        text = repository.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Quick stats in compact row
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactStat(Icons.Default.Star, formatCount(repository.stars))
                CompactStat(Icons.Default.CallSplit, formatCount(repository.forks))
            }
        }
    }
}

@Composable
fun CompactStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}


fun getFileIconColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") -> AccentColors.purple
        fileName.endsWith(".java") -> AccentColors.orange
        fileName.endsWith(".xml") -> AccentColors.green
        fileName.endsWith(".json") -> AccentColors.yellow
        fileName.endsWith(".md") -> AccentColors.blue
        fileName.endsWith(".gif") || 
        fileName.endsWith(".png") || 
        fileName.endsWith(".jpg") -> AccentColors.pink
        else -> AccentColors.teal
    }
}
