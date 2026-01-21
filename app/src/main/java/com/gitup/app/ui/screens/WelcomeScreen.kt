package com.gitup.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitup.app.ui.components.AnimatedGitHubLogo
import com.gitup.app.ui.theme.AccentColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to GitUp",
            description = "Manage your GitHub repositories on the go",
            icon = Icons.Outlined.Rocket,
            iconColor = AccentColors.purple
        ),
        OnboardingPage(
            title = "Browse Repositories",
            description = "Access all your repos with stats",
            icon = Icons.Outlined.Folder,
            iconColor = AccentColors.blue
        ),
        OnboardingPage(
            title = "Track Commits",
            description = "Stay updated with commit history",
            icon = Icons.Outlined.History,
            iconColor = AccentColors.green
        ),
        OnboardingPage(
            title = "Manage Files",
            description = "Browse and upload files easily",
            icon = Icons.Outlined.Description,
            iconColor = AccentColors.orange
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        delay(150)
        showContent = true
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Logo and app name
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.9f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LogoWithAnimation(size = 64.dp)
                    
                    Text(
                        text = "GitUp",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Swipeable content area with HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) { page ->
                OnboardingPageContent(pages[page])
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Spacer(modifier = Modifier.weight(0.2f))
            
            // Page indicators
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, delayMillis = 200))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    pages.indices.forEach { index ->
                        PageIndicator(
                            isActive = index == pagerState.currentPage,
                            color = if (index == pagerState.currentPage) pages[index].iconColor 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Buttons
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, delayMillis = 300)) + 
                        slideInVertically(tween(400, delayMillis = 300)) { it / 3 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onGetStarted()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(
                            onClick = onGetStarted,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                "Skip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoWithAnimation(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .offset(y = offsetY.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedGitHubLogo(
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon and title in row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = page.iconColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = page.iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = page.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun PageIndicator(
    isActive: Boolean,
    color: androidx.compose.ui.graphics.Color
) {
    val width by animateDpAsState(
        targetValue = if (isActive) 20.dp else 6.dp,
        animationSpec = tween(250),
        label = "indicator_width"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(250),
        label = "indicator_alpha"
    )
    
    Box(
        modifier = Modifier
            .width(width)
            .height(6.dp)
            .alpha(alpha)
            .background(
                color = color,
                shape = RoundedCornerShape(3.dp)
            )
    )
}
