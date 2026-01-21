# File Viewer/Editor Feature Implementation

## Overview
Added comprehensive file viewing and editing capabilities to the GitUp app, supporting multiple file types including code, images, GIFs, and videos.

## Features Implemented

### 1. File Type Support
- **CODE FILES**: Editable text files with syntax highlighting support
  - Kotlin (.kt), Java (.java)
  - JavaScript (.js), TypeScript (.ts)
  - Python (.py), C/C++ (.c, .cpp, .h)
  - XML (.xml), JSON (.json)
  - Markdown (.md), Text (.txt)
  - Gradle (.gradle), YAML (.yml, .yaml)
  - HTML (.html), CSS (.css, .scss)

- **IMAGE FILES**: Static image viewing
  - JPEG (.jpg, .jpeg)
  - PNG (.png)
  - WebP (.webp)

- **GIF FILES**: Animated GIF playback
  - GIF (.gif)

- **VIDEO FILES**: Video playback with controls
  - MP4 (.mp4)
  - MOV (.mov)
  - AVI (.avi)
  - MKV (.mkv)

### 2. Code Editor Features
- View mode: Read-only display with monospace font
- Edit mode: Full text editing capabilities
- Commit dialog: Save changes with custom commit message
- File info bar showing filename and edit status
- Syntax-aware display (monospace font, proper line spacing)

### 3. Media Viewer Features
- Image viewer with fit-to-screen scaling
- GIF viewer with animation support
- Video player with ExoPlayer integration
- Black background for media files
- Proper content scaling

### 4. Navigation Integration
- Click any file in FileBrowserScreen to open in viewer
- URL-encoded paths for special character support
- Back navigation to file browser
- Breadcrumb path display in top bar

## Files Created/Modified

### New Files
1. `FileViewerScreen.kt` - Main file viewer UI with support for all file types
2. `FileViewerViewModel.kt` - ViewModel handling file loading and commit operations

### Modified Files
1. `build.gradle.kts` - Added dependencies:
   - `androidx.media3:media3-exoplayer:1.5.0` - Video playback
   - `androidx.media3:media3-ui:1.5.0` - Video player UI
   - `io.coil-kt:coil-gif:2.7.0` - GIF decoding

2. `FileBrowserScreen.kt` - Added file click navigation:
   - New parameter: `onNavigateToFileViewer`
   - Click handler for files (directories still navigate to folder)

3. `MainActivity.kt` - Added file viewer route:
   - New route: `file_viewer/{owner}/{repo}/{path}/{branch}`
   - URL encoding/decoding for file paths
   - Import for FileViewerScreen

## Technical Details

### Caching Integration
- File contents are cached using the existing CacheManager
- Instant loading from cache when available
- Background refresh for updated content

### Commit Workflow
1. User clicks Edit button
2. Edits file content in BasicTextField
3. Clicks Save (checkmark icon)
4. Enters commit message in dialog
5. ViewModel commits changes via GitHub API
6. File SHA is updated for future edits

### Error Handling
- Loading states with progress indicator
- Error messages with retry button
- Network error detection
- Invalid file type handling

## Usage

### Opening a File
1. Navigate to a repository
2. Browse to any folder
3. Click on a file (not a folder)
4. File opens in appropriate viewer based on extension

### Editing Code Files
1. Open a code file
2. Click the Edit icon (pencil)
3. Make changes to the content
4. Click the Save icon (checkmark)
5. Enter commit message
6. Click Commit

### Viewing Media
- Images and GIFs display automatically
- Videos require play button press
- Pinch to zoom (for images)
- Video controls (play/pause/seek)

## Build Instructions

### Using Android Studio (Recommended)
1. Open the GitUp project in Android Studio
2. Sync Gradle files
3. Build and run on device/emulator

### Using Command Line
If you encounter Gradle wrapper issues, try:
```bash
# In Android Studio terminal or after fixing gradlew
./gradlew assembleDebug
```

Note: The gradlew script may have quote escaping issues on some systems. Use Android Studio's built-in Gradle for best results.

## Testing Checklist
- [ ] Open text/code files (.kt, .java, .json, .md, etc.)
- [ ] Edit code files and commit changes
- [ ] View images (.jpg, .png, .webp)
- [ ] View animated GIFs
- [ ] Play videos (.mp4, .mov)
- [ ] Test back navigation
- [ ] Test error handling (network errors, invalid files)
- [ ] Verify commit success messages
- [ ] Test with files containing special characters in path

## Future Enhancements
- Syntax highlighting for code files
- Line numbers in code editor
- Search/replace in code editor
- Diff view for changes before commit
- Download option for media files
- Share functionality
- Full-screen mode for media
- Zoom controls for images
