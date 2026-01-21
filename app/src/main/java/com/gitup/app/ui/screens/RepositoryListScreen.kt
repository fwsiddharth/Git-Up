package com.gitup.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gitup.app.data.model.Repository
import com.gitup.app.data.storage.AccountManager
import com.gitup.app.ui.components.EmptyState
import com.gitup.app.ui.components.RepositoryCardSkeleton
import com.gitup.app.ui.theme.Spacing
import com.gitup.app.ui.theme.AccentColors
import com.gitup.app.ui.viewmodel.RepositoryListViewModel
import com.gitup.app.ui.utils.getLanguageColor
import com.gitup.app.ui.utils.formatCount
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class RepoFilter {
    ALL, PRIVATE, PUBLIC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryListScreen(
    onRepositorySelected: (Repository) -> Unit,
    onManageAccounts: () -> Unit,
    viewModel: RepositoryListViewModel = viewModel()
) {
    val context = LocalContext.current
    val accountManager = remember { AccountManager(context) }
    val activeAccount = remember { accountManager.getActiveAccount() }
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RepoFilter.ALL) }
    
    // Filter repositories based on search and filter
    val filteredRepositories = remember(uiState.repositories, searchQuery, selectedFilter) {
        uiState.repositories
            .filter { repo ->
                val matchesSearch = searchQuery.isEmpty() || 
                    repo.name.contains(searchQuery, ignoreCase = true) ||
                    repo.owner.login.contains(searchQuery, ignoreCase = true) ||
                    repo.description?.contains(searchQuery, ignoreCase = true) == true
                
                val matchesFilter = when (selectedFilter) {
                    RepoFilter.ALL -> true
                    RepoFilter.PRIVATE -> repo.private
                    RepoFilter.PUBLIC -> !repo.private
                }
                
                matchesSearch && matchesFilter
            }
    }
    
    val showFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GitUp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRepositories() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AccentColors.green
                        )
                    }
                    IconButton(onClick = onManageAccounts) {
                        if (activeAccount?.avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(activeAccount.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Manage Accounts",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Manage Accounts",
                                tint = AccentColors.blue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Scroll to top")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            if (!uiState.isLoading && uiState.repositories.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search repositories...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.m, vertical = Spacing.xs),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == RepoFilter.ALL,
                            onClick = { selectedFilter = RepoFilter.ALL },
                            label = { Text("All") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == RepoFilter.PRIVATE,
                            onClick = { selectedFilter = RepoFilter.PRIVATE },
                            label = { Text("Private") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == RepoFilter.PUBLIC,
                            onClick = { selectedFilter = RepoFilter.PUBLIC },
                            label = { Text("Public") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            
            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            items(6) {
                                RepositoryCardSkeleton()
                            }
                        }
                    }
                    uiState.error != null -> {
                        EmptyState(
                            icon = Icons.Default.Error,
                            title = "Couldn't load repositories",
                            subtitle = uiState.error ?: "Something went wrong",
                            actionLabel = "Retry",
                            onActionClick = { viewModel.loadRepositories() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    filteredRepositories.isEmpty() && searchQuery.isNotEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "No results found",
                            subtitle = "Try a different search term",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.repositories.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.FolderOpen,
                            title = "No Repositories",
                            subtitle = "Your repositories will appear here",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            items(
                                items = filteredRepositories,
                                key = { it.id }
                            ) { repository ->
                                MonochromeRepositoryItem(
                                    repository = repository,
                                    onClick = { onRepositorySelected(repository) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadRepositories()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonochromeRepositoryItem(
    repository: Repository,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            // Header Row: Name + Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Repo name - smaller
                    Text(
                        text = repository.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Owner + Private badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = repository.owner.login,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (repository.private) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "PRIVATE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Arrow in a circle - smaller
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Description - 1 line only
            if (repository.description != null) {
                Text(
                    text = repository.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Stats in compact row (no cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                // Stars
                CompactStatItem(
                    icon = Icons.Default.Star,
                    value = formatCount(repository.stars),
                    iconColor = AccentColors.yellow
                )
                
                // Forks
                CompactStatItem(
                    icon = Icons.Default.CallSplit,
                    value = formatCount(repository.forks),
                    iconColor = AccentColors.blue
                )
                
                // Watchers
                CompactStatItem(
                    icon = Icons.Default.Visibility,
                    value = formatCount(repository.watchers),
                    iconColor = AccentColors.green
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Updated time
                Text(
                    text = formatTimeAgo(repository.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Bottom row: Language + Branch
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language badge
                if (repository.language != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getLanguageColor(repository.language).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, getLanguageColor(repository.language))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = getLanguageColor(repository.language),
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Text(
                                text = repository.language,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Show "Multiple" if no primary language
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Code,
                                null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Multiple",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Branch badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountTree,
                            null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = repository.defaultBranch,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    iconColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(14.dp),
            tint = iconColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatTimeAgo(dateString: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val date = format.parse(dateString) ?: return dateString
        
        val now = Date()
        val diff = now.time - date.time
        
        when {
            diff < 60000 -> "just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val outputFormat = SimpleDateFormat("MMM dd", Locale.US)
                outputFormat.format(date)
            }
        }
    } catch (e: Exception) {
        dateString
    }
}
