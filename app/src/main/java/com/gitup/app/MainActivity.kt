package com.gitup.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gitup.app.data.model.Repository
import com.gitup.app.data.storage.AccountManager
import com.gitup.app.ui.screens.AddAccountScreen
import com.gitup.app.ui.screens.FileBrowserScreen
import com.gitup.app.ui.screens.FileViewerScreen
import com.gitup.app.ui.screens.ManageAccountsScreen
import com.gitup.app.ui.screens.ProfileScreen
import com.gitup.app.ui.screens.RepositoryListScreen
import com.gitup.app.ui.screens.WelcomeScreen
import com.gitup.app.ui.theme.GitUpTheme
import com.gitup.app.ui.screens.UploadFormScreen
import com.gitup.app.ui.screens.CommitHistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GitUpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GitUpApp()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun GitUpApp() {
    val navController = rememberNavController()
    val accountManager = AccountManager(navController.context)
    val context = navController.context
    
    // Store file upload data temporarily
    var pendingFileData by remember { mutableStateOf<Triple<String, ByteArray, String>?>(null) }
    var pendingRepoData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var pendingManifestPath by remember { mutableStateOf<String?>(null) }
    
    // Handle OAuth callback
    val activity = context as? ComponentActivity
    LaunchedEffect(Unit) {
        activity?.intent?.data?.let { uri ->
            if (uri.scheme == "gitup" && uri.host == "callback") {
                // Navigate to add_account and pass the URI
                navController.navigate("add_account?oauth_callback=true")
            }
        }
    }
    
    val startDestination = if (accountManager.hasAccounts()) {
        "repositories"
    } else {
        "welcome"
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("welcome") {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate("add_account") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        
        composable("add_account") {
            val viewModel: com.gitup.app.ui.viewmodel.AddAccountViewModel = viewModel()
            val activity = context as? ComponentActivity
            
            // Handle OAuth callback
            LaunchedEffect(Unit) {
                activity?.intent?.data?.let { uri ->
                    if (uri.scheme == "gitup" && uri.host == "callback") {
                        viewModel.handleOAuthCallback(uri)
                        // Clear the intent data
                        activity.intent = android.content.Intent()
                    }
                }
            }
            
            AddAccountScreen(
                onAccountAdded = {
                    navController.navigate("repositories") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    if (accountManager.hasAccounts()) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("welcome") {
                            popUpTo("add_account") { inclusive = true }
                        }
                    }
                },
                viewModel = viewModel
            )
        }
        
        composable("manage_accounts") {
            ManageAccountsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddAccount = {
                    navController.navigate("add_account")
                }
            )
        }
        
        composable("repositories") {
            RepositoryListScreen(
                onRepositorySelected = { repository ->
                    // Store repository in a simple way using owner/name
                    navController.navigate("file_browser/${repository.owner.login}/${repository.name}/${repository.defaultBranch}")
                },
                onManageAccounts = {
                    navController.navigate("profile")
                }
            )
        }
        
        composable("profile") {
            val viewModel: com.gitup.app.ui.viewmodel.RepositoryListViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            // Load data when profile opens
            LaunchedEffect(Unit) {
                viewModel.loadRepositories()
            }
            
            ProfileScreen(
                user = uiState.user,
                repositories = uiState.repositories,
                onNavigateToSettings = {
                    // Navigate to settings if you have one
                },
                onNavigateToRepository = { repository ->
                    navController.navigate("file_browser/${repository.owner.login}/${repository.name}/${repository.defaultBranch}")
                },
                onNavigateToManageAccounts = {
                    navController.navigate("manage_accounts")
                }
            )
        }
        
        composable("file_browser/{owner}/{repo}/{branch}") { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: return@composable
            val repoName = backStackEntry.arguments?.getString("repo") ?: return@composable
            val branch = backStackEntry.arguments?.getString("branch") ?: "main"
            
            // Create a minimal repository object for the file browser
            val repository = Repository(
                id = 0,
                name = repoName,
                fullName = "$owner/$repoName",
                owner = com.gitup.app.data.model.Owner(owner, ""),
                private = false,
                defaultBranch = branch,
                description = null,
                updatedAt = ""
            )
            
            FileBrowserScreen(
                repository = repository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUploadForm = { fileName, bytes, uploadPath, ownerParam, repoParam, branchParam, manifestPath ->
                    // Store data and navigate
                    pendingFileData = Triple(fileName, bytes, uploadPath)
                    pendingRepoData = Triple(ownerParam, repoParam, branchParam)
                    pendingManifestPath = manifestPath
                    navController.navigate("upload_form")
                },
                onNavigateToCommitHistory = { ownerParam, repoParam, branchParam ->
                    navController.navigate("commit_history/$ownerParam/$repoParam/$branchParam")
                },
                onNavigateToFileViewer = { ownerParam, repoParam, path, branchParam ->
                    // URL encode the path to handle special characters
                    val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                    navController.navigate("file_viewer/$ownerParam/$repoParam/$encodedPath/$branchParam")
                }
            )
        }
        
        composable("upload_form") {
            val fileData = pendingFileData
            val repoData = pendingRepoData
            val manifestPath = pendingManifestPath
            
            if (fileData != null && repoData != null && manifestPath != null) {
                val viewModel: com.gitup.app.ui.viewmodel.UploadFormViewModel = viewModel()
                
                UploadFormScreen(
                    fileName = fileData.first,
                    fileSize = fileData.second.size.toLong(),
                    uploadPath = fileData.third,
                    owner = repoData.first,
                    repo = repoData.second,
                    branch = repoData.third,
                    manifestPath = manifestPath,
                    fileBytes = fileData.second,
                    onUploadComplete = {
                        // Clear pending data
                        pendingFileData = null
                        pendingRepoData = null
                        pendingManifestPath = null
                        // Navigate back to file browser
                        navController.popBackStack()
                    },
                    onCancel = {
                        // Clear pending data
                        pendingFileData = null
                        pendingRepoData = null
                        pendingManifestPath = null
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            } else {
                // Error state - navigate back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        composable("commit_history/{owner}/{repo}/{branch}") { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: return@composable
            val repoName = backStackEntry.arguments?.getString("repo") ?: return@composable
            val branch = backStackEntry.arguments?.getString("branch") ?: "main"
            
            CommitHistoryScreen(
                owner = owner,
                repo = repoName,
                branch = branch,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("file_viewer/{owner}/{repo}/{path}/{branch}") { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: return@composable
            val repoName = backStackEntry.arguments?.getString("repo") ?: return@composable
            val encodedPath = backStackEntry.arguments?.getString("path") ?: return@composable
            val branch = backStackEntry.arguments?.getString("branch") ?: "main"
            
            // URL decode the path
            val path = java.net.URLDecoder.decode(encodedPath, "UTF-8")
            
            FileViewerScreen(
                owner = owner,
                repo = repoName,
                path = path,
                branch = branch,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
