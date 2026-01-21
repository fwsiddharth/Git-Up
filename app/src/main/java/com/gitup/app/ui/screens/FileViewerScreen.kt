package com.gitup.app.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.gitup.app.ui.theme.AccentColors
import com.gitup.app.ui.viewmodel.FileViewerViewModel

enum class FileType {
    CODE, IMAGE, GIF, VIDEO, UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    owner: String,
    repo: String,
    path: String,
    branch: String,
    onNavigateBack: () -> Unit,
    viewModel: FileViewerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf("") }
    var showCommitDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showLineNumbers by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(14) }
    var showMoreMenu by remember { mutableStateOf(false) }
    
    val fileType = remember(path) {
        when {
            path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) || 
            path.endsWith(".png", true) || path.endsWith(".webp", true) -> FileType.IMAGE
            path.endsWith(".gif", true) -> FileType.GIF
            path.endsWith(".mp4", true) || path.endsWith(".mov", true) || 
            path.endsWith(".avi", true) || path.endsWith(".mkv", true) -> FileType.VIDEO
            path.endsWith(".kt", true) || path.endsWith(".java", true) ||
            path.endsWith(".js", true) || path.endsWith(".ts", true) ||
            path.endsWith(".py", true) || path.endsWith(".cpp", true) ||
            path.endsWith(".c", true) || path.endsWith(".h", true) ||
            path.endsWith(".hpp", true) || path.endsWith(".cc", true) ||
            path.endsWith(".cxx", true) || path.endsWith(".rb", true) ||
            path.endsWith(".rs", true) || path.endsWith(".xml", true) ||
            path.endsWith(".json", true) || path.endsWith(".md", true) ||
            path.endsWith(".txt", true) || path.endsWith(".gradle", true) ||
            path.endsWith(".yml", true) || path.endsWith(".yaml", true) ||
            path.endsWith(".html", true) || path.endsWith(".css", true) ||
            path.endsWith(".scss", true) || path.endsWith(".sh", true) ||
            path.endsWith(".properties", true) || path.endsWith(".kts", true) -> FileType.CODE
            // Files without extensions (gitignore, dockerfile, etc.)
            path.substringAfterLast("/").let { fileName ->
                fileName == ".gitignore" || fileName == ".gitattributes" ||
                fileName == "Dockerfile" || fileName == "Makefile" ||
                fileName == "README" || fileName == "LICENSE" ||
                fileName == ".env" || fileName == ".dockerignore" ||
                fileName.startsWith(".") && !fileName.contains(".")
            } -> FileType.CODE
            else -> FileType.UNKNOWN
        }
    }
    
    LaunchedEffect(owner, repo, path, branch) {
        viewModel.loadFile(owner, repo, path, branch)
    }
    
    LaunchedEffect(uiState.content) {
        if (uiState.content != null && !isEditing) {
            editedContent = uiState.content ?: ""
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = path.substringAfterLast("/"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = path.substringBeforeLast("/", ""),
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
                    if (fileType == FileType.CODE && !uiState.isLoading) {
                        if (isEditing) {
                            IconButton(onClick = { 
                                showCommitDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Check,
                                    "Save",
                                    tint = AccentColors.green
                                )
                            }
                            IconButton(onClick = { 
                                isEditing = false
                                editedContent = uiState.content ?: ""
                            }) {
                                Icon(Icons.Default.Close, "Cancel")
                            }
                        } else {
                            // Search button
                            IconButton(onClick = { showSearchDialog = true }) {
                                Icon(Icons.Default.Search, "Search")
                            }
                            // More options menu
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "More")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            isEditing = true
                                            showMoreMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, null, tint = AccentColors.blue)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (showLineNumbers) "Hide Line Numbers" else "Show Line Numbers") },
                                        onClick = {
                                            showLineNumbers = !showLineNumbers
                                            showMoreMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.FormatListNumbered, null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Increase Font") },
                                        onClick = {
                                            if (fontSize < 24) fontSize += 2
                                            showMoreMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Decrease Font") },
                                        onClick = {
                                            if (fontSize > 10) fontSize -= 2
                                            showMoreMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Remove, null)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    ErrorMessage(
                        error = uiState.error ?: "Unknown error",
                        onRetry = {
                            viewModel.loadFile(owner, repo, path, branch)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    when (fileType) {
                        FileType.CODE -> {
                            CodeViewer(
                                content = if (isEditing) editedContent else (uiState.content ?: ""),
                                isEditing = isEditing,
                                onContentChange = { editedContent = it },
                                fileName = path.substringAfterLast("/"),
                                showLineNumbers = showLineNumbers,
                                fontSize = fontSize,
                                searchQuery = searchQuery
                            )
                        }
                        FileType.IMAGE -> {
                            ImageViewer(url = uiState.downloadUrl ?: "")
                        }
                        FileType.GIF -> {
                            GifViewer(url = uiState.downloadUrl ?: "")
                        }
                        FileType.VIDEO -> {
                            VideoPlayer(url = uiState.downloadUrl ?: "")
                        }
                        FileType.UNKNOWN -> {
                            UnsupportedFileType(
                                fileName = path.substringAfterLast("/"),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Commit dialog
    if (showCommitDialog) {
        CommitDialog(
            fileName = path.substringAfterLast("/"),
            onDismiss = { showCommitDialog = false },
            onCommit = { message ->
                viewModel.commitChanges(
                    owner = owner,
                    repo = repo,
                    path = path,
                    branch = branch,
                    content = editedContent,
                    message = message
                )
                showCommitDialog = false
                isEditing = false
            }
        )
    }
    
    // Show success message
    LaunchedEffect(uiState.commitSuccess) {
        if (uiState.commitSuccess) {
            // You can show a snackbar here
        }
    }
    
    // Search dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search in File") },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Enter search term") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { showSearchDialog = false }) {
                    Text("Search")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    searchQuery = ""
                    showSearchDialog = false
                }) {
                    Text("Clear")
                }
            }
        )
    }
}

@Composable
fun CodeViewer(
    content: String,
    isEditing: Boolean,
    onContentChange: (String) -> Unit,
    fileName: String,
    showLineNumbers: Boolean = true,
    fontSize: Int = 14,
    searchQuery: String = ""
) {
    val scrollState = rememberScrollState()
    var textFieldValue by remember(content) { 
        mutableStateOf(TextFieldValue(content))
    }
    
    // Update parent when text changes
    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text != content) {
            onContentChange(textFieldValue.text)
        }
    }
    
    // Determine language from file extension
    val language = remember(fileName) {
        when {
            fileName.endsWith(".kt") || fileName.endsWith(".kts") -> "kotlin"
            fileName.endsWith(".java") -> "java"
            fileName.endsWith(".js") || fileName.endsWith(".ts") -> "javascript"
            fileName.endsWith(".py") -> "python"
            fileName.endsWith(".c") || fileName.endsWith(".h") -> "c"
            fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx") || fileName.endsWith(".hpp") -> "cpp"
            fileName.endsWith(".rb") -> "ruby"
            fileName.endsWith(".rs") -> "rust"
            fileName.endsWith(".json") -> "json"
            fileName.endsWith(".xml") -> "xml"
            fileName.endsWith(".html") -> "html"
            fileName.endsWith(".css") || fileName.endsWith(".scss") -> "css"
            fileName.endsWith(".sh") -> "shell"
            fileName.endsWith(".yml") || fileName.endsWith(".yaml") -> "yaml"
            fileName.endsWith(".gradle") -> "gradle"
            fileName.endsWith(".properties") -> "properties"
            // Files without extensions
            fileName == ".gitignore" || fileName == ".gitattributes" || 
            fileName == ".dockerignore" || fileName == ".env" -> "text"
            fileName == "Dockerfile" -> "dockerfile"
            fileName == "Makefile" -> "makefile"
            else -> "text"
        }
    }
    
    // Count lines
    val lineCount = remember(content) {
        content.count { it == '\n' } + 1
    }
    
    // Count search matches
    val matchCount = remember(content, searchQuery) {
        if (searchQuery.isEmpty()) 0
        else content.split(searchQuery).size - 1
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        // File info bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AccentColors.purple
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$lineCount lines • ${content.length} chars" + 
                               if (matchCount > 0) " • $matchCount matches" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isEditing) {
                    Surface(
                        color = AccentColors.orange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "EDITING",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentColors.orange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Code content with line numbers
        Row(modifier = Modifier.weight(1f)) {
            // Line numbers
            if (showLineNumbers && !isEditing) {
                val lines = content.lines()
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.width(56.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(scrollState)
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        lines.forEachIndexed { index, line ->
                            Text(
                                text = "${index + 1}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    lineHeight = (fontSize + 6).sp
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // Code content - disable text wrapping for better alignment
            Box(modifier = Modifier.fillMaxSize()) {
                val horizontalScrollState = rememberScrollState()
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Code editor/viewer
                    Box(modifier = Modifier.weight(1f)) {
                        if (isEditing) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { textFieldValue = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .horizontalScroll(horizontalScrollState)
                                    .padding(16.dp),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = (fontSize + 6).sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.None,
                                    autoCorrect = false
                                )
                            )
                        } else {
                            // Syntax highlighted view with search highlighting
                            val highlightedText = remember(content, language, searchQuery) {
                                val syntaxHighlighted = highlightSyntax(content, language)
                                if (searchQuery.isNotEmpty()) {
                                    highlightSearchResults(syntaxHighlighted, content, searchQuery)
                                } else {
                                    syntaxHighlighted
                                }
                            }
                            
                            Text(
                                text = highlightedText,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .horizontalScroll(horizontalScrollState)
                                    .padding(16.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize + 6).sp
                                ),
                                softWrap = false,
                                maxLines = Int.MAX_VALUE
                            )
                        }
                    }
                    
                    // Mobile keyboard toolbar (only in edit mode)
                    if (isEditing) {
                        KeyboardToolbar(
                            onInsertText = { text ->
                                val newText = textFieldValue.text.substring(0, textFieldValue.selection.start) +
                                        text +
                                        textFieldValue.text.substring(textFieldValue.selection.end)
                                val newSelection = textFieldValue.selection.start + text.length
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newSelection)
                                )
                            },
                            onUndo = {
                                // Simple undo - could be enhanced with history
                            },
                            onRedo = {
                                // Simple redo - could be enhanced with history
                            }
                        )
                    }
                }
            }
        }
    }
}

// Mobile-friendly keyboard toolbar
@Composable
fun KeyboardToolbar(
    onInsertText: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Common programming characters
            ToolbarButton("Tab", "    ", onInsertText)
            ToolbarButton("{", "{}", onInsertText, moveCursor = -1)
            ToolbarButton("}", "}", onInsertText)
            ToolbarButton("(", "()", onInsertText, moveCursor = -1)
            ToolbarButton(")", ")", onInsertText)
            ToolbarButton("[", "[]", onInsertText, moveCursor = -1)
            ToolbarButton("]", "]", onInsertText)
            ToolbarButton("<", "<>", onInsertText, moveCursor = -1)
            ToolbarButton(">", ">", onInsertText)
            ToolbarButton("\"", "\"\"", onInsertText, moveCursor = -1)
            ToolbarButton("'", "''", onInsertText, moveCursor = -1)
            ToolbarButton("=", "=", onInsertText)
            ToolbarButton(";", ";", onInsertText)
            ToolbarButton(":", ":", onInsertText)
            ToolbarButton(",", ",", onInsertText)
            ToolbarButton(".", ".", onInsertText)
            ToolbarButton("/", "/", onInsertText)
            ToolbarButton("\\", "\\", onInsertText)
            ToolbarButton("|", "|", onInsertText)
            ToolbarButton("&", "&", onInsertText)
            ToolbarButton("*", "*", onInsertText)
            ToolbarButton("+", "+", onInsertText)
            ToolbarButton("-", "-", onInsertText)
            ToolbarButton("_", "_", onInsertText)
            ToolbarButton("$", "$", onInsertText)
            ToolbarButton("#", "#", onInsertText)
            ToolbarButton("@", "@", onInsertText)
            ToolbarButton("!", "!", onInsertText)
            ToolbarButton("?", "?", onInsertText)
            ToolbarButton("%", "%", onInsertText)
        }
    }
}

@Composable
fun ToolbarButton(
    label: String,
    insertText: String,
    onInsert: (String) -> Unit,
    moveCursor: Int = 0
) {
    Button(
        onClick = { onInsert(insertText) },
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = if (label.length == 1) FontFamily.Monospace else FontFamily.Default
        )
    }
}

// Highlight search results
fun highlightSearchResults(annotatedString: AnnotatedString, originalText: String, query: String): AnnotatedString {
    if (query.isEmpty()) return annotatedString
    
    return buildAnnotatedString {
        append(annotatedString)
        
        var startIndex = 0
        while (startIndex < originalText.length) {
            val index = originalText.indexOf(query, startIndex, ignoreCase = true)
            if (index == -1) break
            
            addStyle(
                style = SpanStyle(
                    background = AccentColors.yellow.copy(alpha = 0.4f),
                    color = Color.Black
                ),
                start = index,
                end = index + query.length
            )
            
            startIndex = index + query.length
        }
    }
}

// Syntax highlighting function
fun highlightSyntax(code: String, language: String): AnnotatedString {
    // Special handling for JSON
    if (language == "json") {
        return highlightJson(code)
    }
    
    return buildAnnotatedString {
        val keywords = when (language) {
            "kotlin" -> listOf(
                "package", "import", "class", "interface", "object", "fun", "val", "var",
                "if", "else", "when", "for", "while", "do", "return", "break", "continue",
                "try", "catch", "finally", "throw", "private", "public", "protected",
                "internal", "open", "abstract", "override", "suspend", "inline", "data",
                "sealed", "enum", "companion", "init", "constructor", "this", "super",
                "null", "true", "false", "is", "in", "as"
            )
            "java" -> listOf(
                "package", "import", "class", "interface", "extends", "implements",
                "public", "private", "protected", "static", "final", "abstract", "void",
                "int", "long", "double", "float", "boolean", "char", "String",
                "if", "else", "switch", "case", "for", "while", "do", "return",
                "break", "continue", "try", "catch", "finally", "throw", "throws",
                "new", "this", "super", "null", "true", "false"
            )
            "javascript" -> listOf(
                "const", "let", "var", "function", "class", "extends", "import", "export",
                "if", "else", "switch", "case", "for", "while", "do", "return",
                "break", "continue", "try", "catch", "finally", "throw", "async", "await",
                "new", "this", "null", "undefined", "true", "false", "typeof", "instanceof"
            )
            "python" -> listOf(
                "def", "class", "import", "from", "as", "if", "elif", "else", "for",
                "while", "return", "break", "continue", "try", "except", "finally",
                "raise", "with", "pass", "lambda", "yield", "async", "await",
                "True", "False", "None", "and", "or", "not", "in", "is"
            )
            "c" -> listOf(
                "auto", "break", "case", "char", "const", "continue", "default", "do",
                "double", "else", "enum", "extern", "float", "for", "goto", "if",
                "int", "long", "register", "return", "short", "signed", "sizeof", "static",
                "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
                "include", "define", "ifdef", "ifndef", "endif", "NULL", "true", "false"
            )
            "cpp" -> listOf(
                "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor",
                "bool", "break", "case", "catch", "char", "class", "const", "constexpr",
                "const_cast", "continue", "decltype", "default", "delete", "do", "double",
                "dynamic_cast", "else", "enum", "explicit", "export", "extern", "false",
                "float", "for", "friend", "goto", "if", "inline", "int", "long", "mutable",
                "namespace", "new", "noexcept", "not", "not_eq", "nullptr", "operator", "or",
                "or_eq", "private", "protected", "public", "register", "reinterpret_cast",
                "return", "short", "signed", "sizeof", "static", "static_assert", "static_cast",
                "struct", "switch", "template", "this", "throw", "true", "try", "typedef",
                "typeid", "typename", "union", "unsigned", "using", "virtual", "void",
                "volatile", "while", "xor", "xor_eq", "include", "define", "ifdef", "ifndef", "endif"
            )
            "ruby" -> listOf(
                "alias", "and", "begin", "break", "case", "class", "def", "defined", "do",
                "else", "elsif", "end", "ensure", "false", "for", "if", "in", "module",
                "next", "nil", "not", "or", "redo", "rescue", "retry", "return", "self",
                "super", "then", "true", "undef", "unless", "until", "when", "while", "yield",
                "require", "include", "extend", "attr_accessor", "attr_reader", "attr_writer",
                "private", "protected", "public"
            )
            "rust" -> listOf(
                "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else",
                "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop",
                "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static",
                "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while",
                "abstract", "become", "box", "do", "final", "macro", "override", "priv",
                "typeof", "unsized", "virtual", "yield", "i8", "i16", "i32", "i64", "i128",
                "u8", "u16", "u32", "u64", "u128", "f32", "f64", "bool", "char", "str",
                "String", "Vec", "Option", "Result", "Some", "None", "Ok", "Err"
            )
            else -> emptyList()
        }
        
        var currentIndex = 0
        val lines = code.split("\n")
        
        lines.forEachIndexed { lineIndex, line ->
            var linePos = 0
            
            // Check for comments
            val commentStart = when (language) {
                "python", "ruby" -> line.indexOf("#")
                "html", "xml" -> line.indexOf("<!--")
                else -> line.indexOf("//")
            }
            
            val multiLineCommentStart = when (language) {
                "kotlin", "java", "javascript", "css", "c", "cpp", "rust" -> line.indexOf("/*")
                else -> -1
            }
            
            // Check for strings
            var inString = false
            var stringChar = ' '
            
            while (linePos < line.length) {
                val char = line[linePos]
                
                // Handle strings
                if ((char == '"' || char == '\'') && (linePos == 0 || line[linePos - 1] != '\\')) {
                    if (!inString) {
                        inString = true
                        stringChar = char
                        val stringStart = linePos
                        var stringEnd = linePos + 1
                        
                        while (stringEnd < line.length) {
                            if (line[stringEnd] == stringChar && line[stringEnd - 1] != '\\') {
                                stringEnd++
                                break
                            }
                            stringEnd++
                        }
                        
                        withStyle(SpanStyle(color = AccentColors.green)) {
                            append(line.substring(stringStart, stringEnd))
                        }
                        linePos = stringEnd
                        inString = false
                        continue
                    }
                }
                
                // Handle comments
                if (commentStart != -1 && linePos >= commentStart) {
                    withStyle(SpanStyle(color = Color.Gray)) {
                        append(line.substring(linePos))
                    }
                    break
                }
                
                // Handle keywords
                var foundKeyword = false
                for (keyword in keywords) {
                    if (linePos + keyword.length <= line.length) {
                        val word = line.substring(linePos, linePos + keyword.length)
                        val nextChar = if (linePos + keyword.length < line.length) 
                            line[linePos + keyword.length] else ' '
                        val prevChar = if (linePos > 0) line[linePos - 1] else ' '
                        
                        if (word == keyword && !nextChar.isLetterOrDigit() && !prevChar.isLetterOrDigit()) {
                            withStyle(SpanStyle(color = AccentColors.purple, fontWeight = FontWeight.Bold)) {
                                append(keyword)
                            }
                            linePos += keyword.length
                            foundKeyword = true
                            break
                        }
                    }
                }
                
                if (foundKeyword) continue
                
                // Handle numbers
                if (char.isDigit()) {
                    val numStart = linePos
                    var numEnd = linePos
                    while (numEnd < line.length && (line[numEnd].isDigit() || line[numEnd] == '.')) {
                        numEnd++
                    }
                    withStyle(SpanStyle(color = AccentColors.orange)) {
                        append(line.substring(numStart, numEnd))
                    }
                    linePos = numEnd
                    continue
                }
                
                // Handle function calls (word followed by '(')
                if (char.isLetter() || char == '_') {
                    val wordStart = linePos
                    var wordEnd = linePos
                    while (wordEnd < line.length && (line[wordEnd].isLetterOrDigit() || line[wordEnd] == '_')) {
                        wordEnd++
                    }
                    
                    val word = line.substring(wordStart, wordEnd)
                    val nextNonSpace = line.substring(wordEnd).indexOfFirst { !it.isWhitespace() }
                    val isFunction = nextNonSpace != -1 && line[wordEnd + nextNonSpace] == '('
                    
                    if (isFunction) {
                        withStyle(SpanStyle(color = AccentColors.blue)) {
                            append(word)
                        }
                    } else {
                        append(word)
                    }
                    linePos = wordEnd
                    continue
                }
                
                // Handle annotations/decorators
                if (char == '@' && (language == "kotlin" || language == "java" || language == "python" || language == "rust")) {
                    val wordStart = linePos
                    var wordEnd = linePos + 1
                    while (wordEnd < line.length && (line[wordEnd].isLetterOrDigit() || line[wordEnd] == '_')) {
                        wordEnd++
                    }
                    withStyle(SpanStyle(color = AccentColors.yellow)) {
                        append(line.substring(wordStart, wordEnd))
                    }
                    linePos = wordEnd
                    continue
                }
                
                // Handle preprocessor directives for C/C++
                if (char == '#' && (language == "c" || language == "cpp") && linePos == line.indexOfFirst { !it.isWhitespace() }) {
                    val wordStart = linePos
                    var wordEnd = linePos + 1
                    while (wordEnd < line.length && (line[wordEnd].isLetterOrDigit() || line[wordEnd] == '_')) {
                        wordEnd++
                    }
                    withStyle(SpanStyle(color = AccentColors.pink)) {
                        append(line.substring(wordStart, wordEnd))
                    }
                    linePos = wordEnd
                    continue
                }
                
                // Default: append character as-is
                append(char)
                linePos++
            }
            
            // Add newline except for last line
            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

// Special JSON syntax highlighter with cleaner colors
fun highlightJson(json: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < json.length) {
            val char = json[i]
            
            when {
                // Skip whitespace
                char.isWhitespace() -> {
                    append(char)
                    i++
                }
                
                // Handle strings (keys and values)
                char == '"' -> {
                    val stringStart = i
                    i++ // Skip opening quote
                    val stringBuilder = StringBuilder("\"")
                    
                    while (i < json.length && json[i] != '"') {
                        if (json[i] == '\\' && i + 1 < json.length) {
                            stringBuilder.append(json[i])
                            stringBuilder.append(json[i + 1])
                            i += 2
                        } else {
                            stringBuilder.append(json[i])
                            i++
                        }
                    }
                    
                    if (i < json.length) {
                        stringBuilder.append('"')
                        i++ // Skip closing quote
                    }
                    
                    val stringContent = stringBuilder.toString()
                    
                    // Check if this is a key (followed by :) or a value
                    var nextNonSpace = i
                    while (nextNonSpace < json.length && json[nextNonSpace].isWhitespace()) {
                        nextNonSpace++
                    }
                    
                    val isKey = nextNonSpace < json.length && json[nextNonSpace] == ':'
                    
                    if (isKey) {
                        // Keys in softer blue
                        withStyle(SpanStyle(color = Color(0xFF64B5F6))) {
                            append(stringContent)
                        }
                    } else {
                        // String values in softer green
                        withStyle(SpanStyle(color = Color(0xFF81C784))) {
                            append(stringContent)
                        }
                    }
                }
                
                // Handle numbers
                char.isDigit() || (char == '-' && i + 1 < json.length && json[i + 1].isDigit()) -> {
                    val numStart = i
                    if (char == '-') i++
                    
                    while (i < json.length && (json[i].isDigit() || json[i] == '.' || json[i] == 'e' || json[i] == 'E' || json[i] == '+' || json[i] == '-')) {
                        i++
                    }
                    
                    withStyle(SpanStyle(color = Color(0xFFFFB74D))) {
                        append(json.substring(numStart, i))
                    }
                }
                
                // Handle boolean values
                json.substring(i).startsWith("true") -> {
                    withStyle(SpanStyle(color = Color(0xFFBA68C8))) {
                        append("true")
                    }
                    i += 4
                }
                
                json.substring(i).startsWith("false") -> {
                    withStyle(SpanStyle(color = Color(0xFFBA68C8))) {
                        append("false")
                    }
                    i += 5
                }
                
                // Handle null
                json.substring(i).startsWith("null") -> {
                    withStyle(SpanStyle(color = Color(0xFF90A4AE))) {
                        append("null")
                    }
                    i += 4
                }
                
                // Handle brackets and braces - subtle gray
                char == '{' || char == '}' || char == '[' || char == ']' -> {
                    withStyle(SpanStyle(color = Color(0xFFBDBDBD))) {
                        append(char)
                    }
                    i++
                }
                
                // Handle colon and comma - very subtle
                char == ':' || char == ',' -> {
                    withStyle(SpanStyle(color = Color(0xFF9E9E9E))) {
                        append(char)
                    }
                    i++
                }
                
                // Default
                else -> {
                    append(char)
                    i++
                }
            }
        }
    }
}

@Composable
fun ImageViewer(url: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun GifViewer(url: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .decoderFactory(
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        ImageDecoderDecoder.Factory()
                    } else {
                        GifDecoder.Factory()
                    }
                )
                .build(),
            contentDescription = "GIF",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun UnsupportedFileType(
    fileName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Cannot preview this file type",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CommitDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onCommit: (String) -> Unit
) {
    var commitMessage by remember { mutableStateOf("Update $fileName") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Commit Changes")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a commit message:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    placeholder = { Text("Commit message") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCommit(commitMessage) },
                enabled = commitMessage.isNotBlank()
            ) {
                Text("Commit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
