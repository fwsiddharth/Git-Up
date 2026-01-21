package com.gitup.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gitup.app.data.model.GitHubUser
import com.gitup.app.data.model.Repository
import com.gitup.app.data.storage.AccountManager
import com.gitup.app.ui.theme.AccentColors
import com.gitup.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: GitHubUser?,
    repositories: List<Repository>,
    onNavigateToSettings: () -> Unit,
    onNavigateToRepository: (Repository) -> Unit,
    onNavigateToManageAccounts: () -> Unit
) {
    val context = LocalContext.current
    val accountManager = remember { AccountManager(context) }
    val activeAccount = accountManager.getActiveAccount()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    // Manage Accounts Icon
                    IconButton(onClick = onNavigateToManageAccounts) {
                        Icon(
                            Icons.Default.SwitchAccount,
                            contentDescription = "Manage Accounts",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Share Icon
                    IconButton(onClick = { /* Share profile */ }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Settings Icon
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Profile Header with gradient background
            item {
                ProfileHeaderWithGradient(user = user)
            }
            
            // Bio/Status
            item {
                if (user?.bio != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    BioSection(bio = user.bio)
                }
            }
            
            // User Details
            item {
                if (user != null && (user.location != null || user.company != null || user.blog != null || user.twitterUsername != null)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    UserDetailsCard(user = user)
                }
            }
            
            // Stats Cards (Repos, Followers, Following)
            item {
                Spacer(modifier = Modifier.height(20.dp))
                StatsCards(
                    repos = user?.publicRepos ?: repositories.size,
                    followers = user?.followers ?: 0,
                    following = user?.following ?: 0,
                    onReposClick = {
                        // Scroll to repositories section (already visible below)
                    },
                    onFollowersClick = {
                        // TODO: Navigate to followers list
                        // For now, we can show a toast or dialog
                    },
                    onFollowingClick = {
                        // TODO: Navigate to following list
                        // For now, we can show a toast or dialog
                    }
                )
            }
            
            // Popular Repositories
            if (repositories.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    PopularRepositoriesSection(
                        repositories = repositories,
                        onRepositoryClick = onNavigateToRepository
                    )
                }
            }
            
            // Contributions Graph Placeholder
            item {
                Spacer(modifier = Modifier.height(24.dp))
                ContributionsCard()
            }
        }
    }
}

@Composable
fun ProfileHeaderWithGradient(user: GitHubUser?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        // Gradient Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            AccentColors.purple.copy(alpha = 0.3f),
                            AccentColors.blue.copy(alpha = 0.2f)
                        )
                    )
                )
        )
        
        // Profile Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture with border
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.size(120.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user?.avatarUrl ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Name
            Text(
                text = user?.name ?: user?.login ?: "User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Username
            Text(
                text = "@${user?.login ?: ""}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BioSection(bio: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = AccentColors.blue,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun UserDetailsCard(user: GitHubUser) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (user.location != null) {
                DetailRowColored(
                    icon = Icons.Default.LocationOn,
                    text = user.location,
                    color = AccentColors.red
                )
            }
            if (user.company != null) {
                DetailRowColored(
                    icon = Icons.Default.Business,
                    text = user.company,
                    color = AccentColors.orange
                )
            }
            if (user.blog != null && user.blog.isNotEmpty()) {
                DetailRowColored(
                    icon = Icons.Default.Link,
                    text = user.blog,
                    color = AccentColors.green
                )
            }
            if (user.twitterUsername != null) {
                DetailRowColored(
                    icon = Icons.Default.AlternateEmail,
                    text = "@${user.twitterUsername}",
                    color = AccentColors.blue
                )
            }
        }
    }
}

@Composable
fun DetailRowColored(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatsCards(
    repos: Int,
    followers: Int,
    following: Int,
    onReposClick: () -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Folder,
            count = repos,
            label = "Repos",
            color = AccentColors.purple,
            onClick = onReposClick
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.People,
            count = followers,
            label = "Followers",
            color = AccentColors.blue,
            onClick = onFollowersClick
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PersonAdd,
            count = following,
            label = "Following",
            color = AccentColors.green,
            onClick = onFollowingClick
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PopularRepositoriesSection(
    repositories: List<Repository>,
    onRepositoryClick: (Repository) -> Unit
) {
    // Only show if there are repositories
    if (repositories.isEmpty()) return
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AccentColors.yellow
            )
            Text(
                text = "Popular Repositories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show only top repositories sorted by stars
            repositories
                .sortedByDescending { it.stars }
                .take(3)
                .forEach { repo ->
                    ColorfulRepoCard(
                        repository = repo,
                        onClick = { onRepositoryClick(repo) }
                    )
                }
        }
    }
}

@Composable
fun ColorfulRepoCard(repository: Repository, onClick: () -> Unit) {
    val cardColor = remember {
        listOf(
            AccentColors.purple,
            AccentColors.blue,
            AccentColors.green,
            AccentColors.orange,
            AccentColors.pink,
            AccentColors.teal
        ).random()
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = cardColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = repository.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (repository.private) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentColors.orange.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Private",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentColors.orange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            if (repository.description != null) {
                Text(
                    text = repository.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (repository.language != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(cardColor)
                        )
                        Text(
                            text = repository.language,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AccentColors.yellow
                    )
                    Text(
                        text = repository.stars.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CallSplit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = repository.forks.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ContributionsCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = AccentColors.green.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AccentColors.green,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Contributions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Activity graph",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Placeholder contribution grid
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(52) { col ->
                            val intensity = (0..4).random()
                            val color = when (intensity) {
                                0 -> MaterialTheme.colorScheme.surfaceVariant
                                1 -> AccentColors.green.copy(alpha = 0.2f)
                                2 -> AccentColors.green.copy(alpha = 0.4f)
                                3 -> AccentColors.green.copy(alpha = 0.6f)
                                else -> AccentColors.green.copy(alpha = 0.8f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Contribution activity over the past year",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

