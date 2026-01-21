# GitUp - Android GitHub Client

A modern, beautiful Android app for managing your GitHub repositories with a premium monochrome design and colorful accents.

## ✨ Features

### 🔐 Authentication
- **GitHub OAuth Login** - One-click login (30 seconds)
- **Personal Access Token** - Advanced option for power users
- **Secure Storage** - AES-256 encrypted token storage
- **Multiple Accounts** - Switch between GitHub accounts easily

### 📁 Repository Management
- Browse all your repositories
- View repository details (stars, forks, language)
- Search and filter repositories
- Support for private repositories

### 📂 File Browser
- Navigate repository file structure
- Beautiful folder/file icons
- Three-dot menu with actions:
  - Open files
  - Download files
  - Copy file paths
  - Share file URLs
  - View file info
  - Delete files

### 📝 Code Viewer/Editor
- **Syntax Highlighting** for 15+ languages:
  - Kotlin, Java, Python, JavaScript, TypeScript
  - C, C++, Ruby, Rust, Go, Swift
  - JSON, XML, HTML, CSS, YAML
  - Shell scripts, Gradle, Properties files
- **Mobile-Optimized Editor**:
  - Keyboard toolbar with programming characters
  - Auto-closing brackets and quotes
  - Line numbers
  - Search with highlighting
  - Font size control (10-24sp)
  - Horizontal scrolling for long lines
- **Edit & Commit** - Edit files and commit directly from the app
- **Special File Support** - .gitignore, Dockerfile, Makefile, etc.

### 🎬 Media Viewer
- **Images** - View PNG, JPG, WebP
- **GIFs** - Animated GIF support
- **Videos** - Built-in video player with controls

### 👤 Profile Screen
- GitHub-style profile with gradient header
- Real-time stats (repos, followers, following)
- Popular repositories showcase
- Contribution activity graph
- User details (location, company, links)
- Colorful, responsive design

### 📊 Commit History
- View commit history for any branch
- Commit messages and authors
- Timestamps and SHAs

### 📤 File Upload
- Upload files to repositories
- Update manifest files
- Commit with custom messages

## 🎨 Design

- **Pure Monochrome** - Black, white, and gray base
- **Colorful Accents** - Purple, blue, green, orange icons
- **Material Design 3** - Modern, premium feel
- **Compact UI** - Optimized spacing for mobile
- **Dark Mode** - Beautiful dark theme

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 34
- Kotlin 1.9.24
- Gradle 8.9

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/gitup.git
   cd gitup
   ```

2. **Set up OAuth (Optional but Recommended)**
   - See [QUICK_OAUTH_SETUP.md](QUICK_OAUTH_SETUP.md) for 5-minute setup
   - Or [OAUTH_SETUP_GUIDE.md](OAUTH_SETUP_GUIDE.md) for detailed guide

3. **Build the app**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 🔧 Configuration

### OAuth Setup (Recommended)

For the best user experience, set up GitHub OAuth:

1. Register OAuth App at https://github.com/settings/developers
2. Set callback URL to: `gitup://callback`
3. Update credentials in `GitHubOAuthHelper.kt`
4. Rebuild the app

See [QUICK_OAUTH_SETUP.md](QUICK_OAUTH_SETUP.md) for step-by-step instructions.

### Using Personal Access Token

Users can also login with a GitHub Personal Access Token:
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate new token with `repo` and `user` scopes
3. Enter token in the app

## 📱 Screenshots

*Coming soon*

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Video Player**: Media3 ExoPlayer
- **Security**: EncryptedSharedPreferences
- **Navigation**: Jetpack Navigation Compose

## 📦 Dependencies

- Jetpack Compose (Material 3)
- Retrofit 2.11.0
- Coil 2.7.0
- Media3 ExoPlayer 1.3.1
- AndroidX Security Crypto
- Kotlin Coroutines

## 🔒 Security

- **Token Encryption**: AES-256-GCM encryption
- **Secure Storage**: Android EncryptedSharedPreferences
- **HTTPS Only**: All API calls use HTTPS
- **No Plaintext Storage**: Tokens never stored in plaintext

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

If you have any questions or issues:
- Open an issue on GitHub
- Check the [OAUTH_SETUP_GUIDE.md](OAUTH_SETUP_GUIDE.md) for OAuth troubleshooting

## 🎯 Roadmap

- [ ] Pull request management
- [ ] Issue tracking
- [ ] Notifications
- [ ] Gist support
- [ ] Repository creation
- [ ] Branch management
- [ ] Merge conflict resolution
- [ ] Code review features

## ⭐ Show Your Support

Give a ⭐️ if this project helped you!

## 👨‍💻 Author

**Your Name**
- GitHub: [@yourusername](https://github.com/yourusername)

---

Made with ❤️ and Kotlin
