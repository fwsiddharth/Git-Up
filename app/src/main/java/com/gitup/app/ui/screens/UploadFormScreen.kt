package com.gitup.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitup.app.data.model.FieldType
import com.gitup.app.data.model.FormField
import com.gitup.app.ui.theme.Spacing
import com.gitup.app.ui.viewmodel.UploadFormViewModel
import com.gitup.app.ui.utils.formatFileSize

enum class UploadStep {
    FILE_INFO,
    CATEGORY_SELECT,
    FORM_DETAILS,
    UPLOADING,
    COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFormScreen(
    fileName: String,
    fileSize: Long,
    uploadPath: String,
    owner: String,
    repo: String,
    branch: String,
    manifestPath: String,
    fileBytes: ByteArray,
    onUploadComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: UploadFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formData = remember { mutableStateMapOf<String, Any>() }
    var currentStep by remember { mutableStateOf(UploadStep.FILE_INFO) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // Load manifest on first composition
    LaunchedEffect(Unit) {
        viewModel.loadManifestAndGenerateForm(
            owner = owner,
            repo = repo,
            branch = branch,
            manifestPath = manifestPath,
            fileBytes = fileBytes
        )
    }
    
    // Auto-advance through steps
    LaunchedEffect(uiState.isLoading, uiState.isSuccess) {
        if (uiState.isLoading && currentStep == UploadStep.FORM_DETAILS) {
            currentStep = UploadStep.UPLOADING
        } else if (uiState.isSuccess) {
            currentStep = UploadStep.COMPLETE
        }
    }
    
    // Handle errors
    LaunchedEffect(uiState.error) {
        if (uiState.error != null && currentStep == UploadStep.UPLOADING) {
            currentStep = UploadStep.FORM_DETAILS
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = when (currentStep) {
                                UploadStep.FILE_INFO -> "File Preview"
                                UploadStep.CATEGORY_SELECT -> "Select Category"
                                UploadStep.FORM_DETAILS -> "File Details"
                                UploadStep.UPLOADING -> "Uploading..."
                                UploadStep.COMPLETE -> "Complete"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentStep != UploadStep.UPLOADING && currentStep != UploadStep.COMPLETE) {
                            Text(
                                text = "Step ${currentStep.ordinal + 1} of 3",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                UploadStep.CATEGORY_SELECT -> currentStep = UploadStep.FILE_INFO
                                UploadStep.FORM_DETAILS -> currentStep = UploadStep.CATEGORY_SELECT
                                else -> onCancel()
                            }
                        },
                        enabled = currentStep != UploadStep.UPLOADING
                    ) {
                        Icon(
                            if (currentStep == UploadStep.FILE_INFO || currentStep == UploadStep.COMPLETE) 
                                Icons.Default.Close 
                            else 
                                Icons.Default.ArrowBack,
                            "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Step navigation bar
            if (currentStep != UploadStep.UPLOADING && currentStep != UploadStep.COMPLETE && uiState.formFields.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.m)
                    ) {
                        // Progress indicator
                        LinearProgressIndicator(
                            progress = { when (currentStep) {
                                UploadStep.FILE_INFO -> 0.33f
                                UploadStep.CATEGORY_SELECT -> 0.66f
                                UploadStep.FORM_DETAILS -> 1f
                                else -> 0f
                            } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.m))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            when (currentStep) {
                                UploadStep.FILE_INFO -> {
                                    Button(
                                        onClick = { currentStep = UploadStep.CATEGORY_SELECT },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Continue")
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                                UploadStep.CATEGORY_SELECT -> {
                                    Button(
                                        onClick = { 
                                            selectedCategory?.let { formData["category"] = it }
                                            currentStep = UploadStep.FORM_DETAILS 
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = selectedCategory != null
                                    ) {
                                        Text("Continue")
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                                UploadStep.FORM_DETAILS -> {
                                    OutlinedButton(
                                        onClick = onCancel,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.uploadWithManifest(
                                                fileName = fileName,
                                                fileSize = fileSize,
                                                uploadPath = uploadPath,
                                                formData = formData.toMap()
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = isFormValid(uiState.formFields, formData)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("Upload")
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.formFields.isEmpty() && !uiState.isLoadingManifest && !uiState.isSuccess && uiState.error == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.m)
                        ) {
                            CircularProgressIndicator()
                            Text("Loading manifest...")
                        }
                    }
                }
                else -> {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal) {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith
                                        slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "step_transition"
                    ) { step ->
                        when (step) {
                            UploadStep.FILE_INFO -> FileInfoStep(
                                fileName = fileName,
                                fileSize = fileSize,
                                uploadPath = uploadPath
                            )
                            UploadStep.CATEGORY_SELECT -> CategorySelectStep(
                                categories = uiState.manifestInfo?.categories ?: emptyList(),
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it }
                            )
                            UploadStep.FORM_DETAILS -> FormDetailsStep(
                                fileName = fileName,
                                fileSize = fileSize,
                                uploadPath = uploadPath,
                                formFields = uiState.formFields.filter { it.key != "category" },
                                formData = formData,
                                error = uiState.error
                            )
                            UploadStep.UPLOADING -> UploadingStep()
                            UploadStep.COMPLETE -> CompleteStep(
                                canRevert = uiState.canRevert,
                                onRevert = { viewModel.revertChanges() },
                                onDone = onUploadComplete
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileInfoStep(
    fileName: String,
    fileSize: Long,
    uploadPath: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.l)
    ) {
        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Large file icon
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Text(
            text = fileName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // File details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                DetailRow(
                    icon = Icons.Default.Storage,
                    label = "File Size",
                    value = formatFileSize(fileSize)
                )
                HorizontalDivider()
                DetailRow(
                    icon = Icons.Default.Folder,
                    label = "Upload Path",
                    value = uploadPath
                )
                HorizontalDivider()
                DetailRow(
                    icon = Icons.Default.Description,
                    label = "Type",
                    value = fileName.substringAfterLast(".", "Unknown")
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "This file will be uploaded to your repository and added to the manifest automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun CategorySelectStep(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        Text(
            text = "Choose a category",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Select where this file should be organized in your manifest",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(Spacing.s))
        
        categories.forEach { category ->
            CategoryCard(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
        
        if (categories.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "No categories found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "The manifest file doesn't contain any categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        },
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                Text(
                    text = category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

fun getCategoryIcon(category: String) = when (category.lowercase()) {
    "images", "image", "photos", "photo" -> Icons.Default.Image
    "videos", "video" -> Icons.Default.VideoLibrary
    "documents", "document", "docs", "doc" -> Icons.Default.Description
    "music", "audio" -> Icons.Default.MusicNote
    "code", "scripts" -> Icons.Default.Code
    "data", "database" -> Icons.Default.Storage
    else -> Icons.Default.Category
}

@Composable
fun FormDetailsStep(
    fileName: String,
    fileSize: Long,
    uploadPath: String,
    formFields: List<FormField>,
    formData: MutableMap<String, Any>,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        // Compact file preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(Spacing.l),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatFileSize(fileSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // Only show non-auto-generated, non-category fields
        val visibleFields = formFields.filter { !it.autoGenerated }
        
        if (visibleFields.isNotEmpty()) {
            Text(
                text = "Additional Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            visibleFields.forEach { field ->
                when (field.type) {
                    FieldType.TEXT -> {
                        TextFormField(
                            field = field,
                            value = formData[field.key] as? String ?: "",
                            onValueChange = { formData[field.key] = it }
                        )
                    }
                    FieldType.DROPDOWN -> {
                        DropdownFormField(
                            field = field,
                            value = formData[field.key] as? String ?: "",
                            onValueChange = { formData[field.key] = it }
                        )
                    }
                    FieldType.BOOLEAN -> {
                        BooleanFormField(
                            field = field,
                            value = formData[field.key] as? Boolean ?: false,
                            onValueChange = { formData[field.key] = it }
                        )
                    }
                    else -> {}
                }
            }
        }
        
        // Show auto-generated fields in a collapsed section
        val autoFields = formFields.filter { it.autoGenerated }
        if (autoFields.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Auto-generated fields",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${autoFields.size} fields will be generated automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Bottom padding for sticky bar
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun UploadingStep() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            
            Text(
                text = "Uploading file...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                UploadStepIndicator("Uploading file to repository", true)
                UploadStepIndicator("Updating manifest", false)
                UploadStepIndicator("Creating commit", false)
            }
        }
    }
}

@Composable
fun UploadStepIndicator(text: String, isActive: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun CompleteStep(
    canRevert: Boolean,
    onRevert: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            // Success animation
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = "Upload Successful!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "File uploaded and manifest updated successfully",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Spacing.m))
            
            if (canRevert) {
                OutlinedButton(
                    onClick = onRevert,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(Icons.Default.Undo, null)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Revert Changes")
                }
                
                Spacer(modifier = Modifier.height(Spacing.s))
            }
            
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TextFormField(
    field: FormField,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFormField(
    field: FormField,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(16.dp)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                field.options?.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BooleanFormField(
    field: FormField,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (value) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = field.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Switch(
                checked = value,
                onCheckedChange = onValueChange
            )
        }
    }
}

fun isFormValid(fields: List<FormField>, formData: Map<String, Any>): Boolean {
    return fields
        .filter { it.required && !it.autoGenerated }
        .all { field ->
            val value = formData[field.key]
            when (field.type) {
                FieldType.TEXT, FieldType.DROPDOWN -> {
                    value is String && value.isNotBlank()
                }
                else -> true
            }
        }
}
