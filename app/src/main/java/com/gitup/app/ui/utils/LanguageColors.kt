package com.gitup.app.ui.utils

import androidx.compose.ui.graphics.Color

fun getLanguageColor(language: String): Color {
    return when (language.lowercase()) {
        "kotlin" -> Color(0xFF7F52FF)
        "java" -> Color(0xFFB07219)
        "javascript" -> Color(0xFFF1E05A)
        "typescript" -> Color(0xFF3178C6)
        "python" -> Color(0xFF3572A5)
        "swift" -> Color(0xFFFF5733)
        "go" -> Color(0xFF00ADD8)
        "rust" -> Color(0xFFDEA584)
        "c++" -> Color(0xFFF34B7D)
        "c" -> Color(0xFF555555)
        "ruby" -> Color(0xFF701516)
        "php" -> Color(0xFF4F5D95)
        "html" -> Color(0xFFE34C26)
        "css" -> Color(0xFF563D7C)
        "shell" -> Color(0xFF89E051)
        "dart" -> Color(0xFF00B4AB)
        "c#" -> Color(0xFF178600)
        "objective-c" -> Color(0xFF438EFF)
        else -> Color(0xFF888888)
    }
}
