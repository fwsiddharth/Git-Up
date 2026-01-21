package com.gitup.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitup.app.data.model.GitCommit
import com.gitup.app.ui.components.CommitItemSkeleton
import com.gitup.app.ui.components.EmptyState
import com.gitup.app.ui.theme.Spacing
import com.gitup.app.ui.theme.AccentColors
import com.gitup.app.ui.viewmodel.CommitHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitHistoryScreen(
    owner: String,
    repo: String,
    branch: String,
    onNavigateBack: () -> Unit,
    viewModel: CommitHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRevertDialog by remember { mutableStateOf<GitCommit?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.loadCommits(owner, repo, branch)
    }
    
    LaunchedEffect(uiState.revertSuccess) {
        if (uiState.revertSuccess) {
            snackbarHostState.showSnackbar(
                message = "✓ Successfully reverted!",
                duration = SnackbarDuration.Short
            )
            viewModel.clearRevertSuccess()
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
                            "Commit History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$owner/$repo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.loadCommits(owner, repo, branch)
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s)
                    ) {
                        items(5) {
                            CommitItemSkeleton()
                        }
                    }
                }
                uiState.error != null -> {
                    EmptyState(
                        icon = Icons.Default.Error,
                        title = "Couldn't load commits",
                        subtitle = uiState.error ?: "Something went wrong",
                        actionLabel = "Retry",
                        onActionClick = { viewModel.loadCommits(owner, repo, branch) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.commits.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No commits yet",
                        subtitle = "Commits will appear here",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s)
                    ) {
                        items(uiState.commits) { commit ->
                            ModernCommitCard(
                                commit = commit,
                                onRevertClick = { showRevertDialog = commit }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Revert confirmation dialog with file preview
    showRevertDialog?.let { commit ->
        LaunchedEffect(commit.sha) {
            viewModel.loadRevertPreview(commit.sha)
        }
        
        DisposableEffect(Unit) {
            onDispose {
                viewModel.clearRevertPreview()
            }
        }
        
        AlertDialog(
            onDismissRequest = { 
                showRevertDialog = null
            },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Revert to this commit?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text("This will reset the '$branch' branch to:")
                    Text(
                        text = commit.commit.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SHA: ${commit.sha.take(7)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    
                    // Show file changes preview
                    val preview = uiState.revertPreview
                    when {
                        uiState.isLoadingPreview -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Loading file changes...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        preview != null && preview.files != null && preview.files.isNotEmpty() -> {
                            val files = preview.files
                            Divider()
                            Text(
                                text = "Files in this commit (${files.size}):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Show first 5 files
                            files.take(5).forEach { file ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (file.status) {
                                            "added" -> Icons.Default.Add
                                            "removed" -> Icons.Default.Delete
                                            "modified" -> Icons.Default.Edit
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = file.status,
                                        modifier = Modifier.size(14.dp),
                                        tint = when (file.status) {
                                            "added" -> MaterialTheme.colorScheme.primary
                                            "removed" -> MaterialTheme.colorScheme.error
                                            "modified" -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = file.filename,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "(+${file.additions}/-${file.deletions})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            if (files.size > 5) {
                                Text(
                                    text = "... and ${files.size - 5} more files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Divider()
                    
                    Text(
                        text = "⚠️ This will force-update the branch pointer. All commits after this point will be removed from the branch history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = "💡 The repository will be at the state of this commit. Pull changes locally to see updated files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revertToCommit(commit.sha)
                        showRevertDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Revert Branch")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRevertDialog = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Reverting progress dialog
    if (uiState.isReverting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Reverting...") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Reverting repository to selected commit...")
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernCommitCard(
    commit: GitCommit,
    onRevertClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
            // Commit icon in colored circle
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentColors.purple.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Commit,
                        contentDescription = null,
                        tint = AccentColors.purple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Commit message
                Text(
                    text = commit.commit.message.lines().first(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Author and time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = commit.commit.author.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(commit.commit.author.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // SHA chip - smaller
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = commit.sha.take(7),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            
            // Revert button - icon only
            FilledTonalIconButton(
                onClick = onRevertClick,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Revert",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)
        
        val now = Date()
        val diff = now.time - (date?.time ?: 0)
        
        when {
            diff < 60000 -> "just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val outputFormat = SimpleDateFormat("MMM dd", Locale.US)
                outputFormat.format(date ?: Date())
            }
        }
    } catch (e: Exception) {
        dateString
    }
}
