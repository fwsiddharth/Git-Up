# Quick OAuth Setup (5 Minutes)

## Step 1: Register on GitHub (2 min)
1. Go to: https://github.com/settings/developers
2. Click "OAuth Apps" → "New OAuth App"
3. Fill in:
   - Name: `GitUp`
   - Homepage: `https://github.com/yourusername/gitup`
   - Callback: `gitup://callback` ⚠️ **Must be exact**
4. Click "Register application"

## Step 2: Get Credentials (1 min)
1. Copy **Client ID** (looks like `Ov23liXXXXXXXXXXXXXX`)
2. Click "Generate a new client secret"
3. Copy **Client Secret** (save it now - you can't see it again!)

## Step 3: Update Code (1 min)
Open: `GitUp/app/src/main/java/com/gitup/app/data/auth/GitHubOAuthHelper.kt`

Replace:
```kotlin
private const val CLIENT_ID = "Ov23liXXXXXXXXXXXXXX"
private const val CLIENT_SECRET = "your_client_secret_here"
```

With your actual values:
```kotlin
private const val CLIENT_ID = "Ov23liYourActualClientID"
private const val CLIENT_SECRET = "your_actual_secret_1234567890abcdef"
```

## Step 4: Build (1 min)
```bash
cd GitUp
./gradlew assembleDebug
```

## Done! 🎉

Now users can login with one click:
- Click "Login with GitHub"
- Authorize
- Done!

**No more manual token creation!**
