package com.gitup.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedGitHubLogo(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eye_animation")
    
    // Eye movement animation - moves in all directions (left, right, up, down)
    val eyeOffsetX by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eyeX"
    )
    
    val eyeOffsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eyeY"
    )
    
    Canvas(modifier = modifier.size(120.dp)) {
        val scale = size.width / 256f
        
        // Draw the main GitHub logo body
        val bodyPath = Path().apply {
            // Main body path from the SVG
            moveTo(169f * scale, 144.4f * scale)
            // Right eye area
            cubicTo(173.5f * scale, 144.4f * scale, 177.3f * scale, 146.6f * scale, 180.5f * scale, 151.1f * scale)
            cubicTo(183.8f * scale, 155.6f * scale, 185.4f * scale, 161f * scale, 185.4f * scale, 167.5f * scale)
            cubicTo(185.4f * scale, 179.5f * scale, 183.8f * scale, 179.5f * scale, 180.5f * scale, 183.9f * scale)
            cubicTo(177.2f * scale, 188.4f * scale, 173.4f * scale, 190.6f * scale, 169f * scale, 190.6f * scale)
            cubicTo(164.2f * scale, 190.6f * scale, 160.2f * scale, 188.4f * scale, 156.9f * scale, 183.9f * scale)
            cubicTo(153.6f * scale, 179f * scale, 152f * scale, 174f * scale, 152f * scale, 167.5f * scale)
            cubicTo(152f * scale, 155.5f * scale, 153.6f * scale, 155.5f * scale, 156.9f * scale, 151.1f * scale)
            cubicTo(160.2f * scale, 146.6f * scale, 164.2f * scale, 144.4f * scale, 169f * scale, 144.4f * scale)
            close()
            
            // Main body
            moveTo(227f * scale, 84.4f * scale)
            cubicTo(239.7f * scale, 98.1f * scale, 246f * scale, 114.7f * scale, 246f * scale, 134.2f * scale)
            cubicTo(246f * scale, 146.9f * scale, 244.6f * scale, 158.2f * scale, 241.6f * scale, 168.3f * scale)
            cubicTo(238.7f * scale, 178.4f * scale, 235f * scale, 186.6f * scale, 230.6f * scale, 192.9f * scale)
            cubicTo(226.1f * scale, 199.2f * scale, 220.7f * scale, 204.8f * scale, 214.2f * scale, 209.6f * scale)
            cubicTo(207.7f * scale, 214.4f * scale, 201.7f * scale, 217.9f * scale, 196.2f * scale, 220.1f * scale)
            cubicTo(190.7f * scale, 222.3f * scale, 184.5f * scale, 224f * scale, 177.5f * scale, 225.2f * scale)
            cubicTo(170.5f * scale, 226.4f * scale, 165.2f * scale, 227.1f * scale, 161.6f * scale, 227.2f * scale)
            cubicTo(158f * scale, 227.4f * scale, 154.2f * scale, 227.5f * scale, 150.1f * scale, 227.5f * scale)
            cubicTo(149.1f * scale, 227.5f * scale, 146f * scale, 227.6f * scale, 140.9f * scale, 227.8f * scale)
            cubicTo(135.8f * scale, 228f * scale, 131.5f * scale, 228.1f * scale, 128.1f * scale, 228.1f * scale)
            cubicTo(124.7f * scale, 228.1f * scale, 120.4f * scale, 228f * scale, 115.3f * scale, 227.8f * scale)
            cubicTo(110.2f * scale, 227.6f * scale, 107.1f * scale, 227.5f * scale, 106.1f * scale, 227.5f * scale)
            cubicTo(102f * scale, 227.5f * scale, 98.2f * scale, 227.4f * scale, 94.6f * scale, 227.2f * scale)
            cubicTo(91f * scale, 227f * scale, 85.7f * scale, 226.3f * scale, 78.7f * scale, 225.2f * scale)
            cubicTo(71.7f * scale, 224f * scale, 65.5f * scale, 222.3f * scale, 60f * scale, 220.1f * scale)
            cubicTo(54.5f * scale, 217.9f * scale, 48.5f * scale, 214.4f * scale, 42f * scale, 209.6f * scale)
            cubicTo(35.5f * scale, 204.8f * scale, 30f * scale, 199.2f * scale, 25.6f * scale, 192.9f * scale)
            cubicTo(21.1f * scale, 186.6f * scale, 17.5f * scale, 178.4f * scale, 14.6f * scale, 168.3f * scale)
            cubicTo(11.7f * scale, 158.2f * scale, 10.2f * scale, 146.8f * scale, 10.2f * scale, 134.2f * scale)
            cubicTo(10.2f * scale, 114.7f * scale, 16.5f * scale, 98.1f * scale, 29.2f * scale, 84.4f * scale)
            cubicTo(27.8f * scale, 83.7f * scale, 27.8f * scale, 76.9f * scale, 28.9f * scale, 63.9f * scale)
            cubicTo(30.1f * scale, 50.9f * scale, 32.9f * scale, 38.9f * scale, 37.4f * scale, 28f * scale)
            cubicTo(53.1f * scale, 29.7f * scale, 72.6f * scale, 38.6f * scale, 95.9f * scale, 54.7f * scale)
            cubicTo(103.8f * scale, 52.7f * scale, 114.5f * scale, 51.6f * scale, 128.2f * scale, 51.6f * scale)
            cubicTo(142.6f * scale, 51.6f * scale, 153.3f * scale, 52.6f * scale, 160.5f * scale, 54.7f * scale)
            cubicTo(171.1f * scale, 47.5f * scale, 181.3f * scale, 41.7f * scale, 191f * scale, 37.3f * scale)
            cubicTo(200.8f * scale, 32.8f * scale, 207.8f * scale, 30.3f * scale, 212.3f * scale, 29.6f * scale)
            lineTo(219f * scale, 28.1f * scale)
            cubicTo(223.5f * scale, 39f * scale, 226.3f * scale, 51f * scale, 227.5f * scale, 64f * scale)
            cubicTo(228.5f * scale, 76.9f * scale, 228.4f * scale, 83.7f * scale, 227f * scale, 84.4f * scale)
            close()
            
            moveTo(128.5f * scale, 216.2f * scale)
            cubicTo(156.9f * scale, 216.2f * scale, 178.3f * scale, 212.8f * scale, 192.9f * scale, 205.9f * scale)
            cubicTo(207.4f * scale, 199.1f * scale, 214.7f * scale, 185f * scale, 214.7f * scale, 163.8f * scale)
            cubicTo(214.7f * scale, 151.5f * scale, 210.1f * scale, 141.2f * scale, 200.9f * scale, 133f * scale)
            cubicTo(196.1f * scale, 128.5f * scale, 190.5f * scale, 125.8f * scale, 184.2f * scale, 124.8f * scale)
            cubicTo(177.9f * scale, 123.8f * scale, 168.2f * scale, 123.8f * scale, 155.2f * scale, 124.8f * scale)
            cubicTo(142.2f * scale, 125.8f * scale, 133.3f * scale, 126.3f * scale, 128.5f * scale, 126.3f * scale)
            lineTo(128f * scale, 126.3f * scale)
            lineTo(127.5f * scale, 126.3f * scale)
            cubicTo(122f * scale, 126.3f * scale, 114.9f * scale, 126f * scale, 106.2f * scale, 125.3f * scale)
            cubicTo(97.5f * scale, 124.6f * scale, 90.6f * scale, 124.2f * scale, 85.7f * scale, 124f * scale)
            cubicTo(80.7f * scale, 123.8f * scale, 75.3f * scale, 124.4f * scale, 69.5f * scale, 125.8f * scale)
            cubicTo(63.7f * scale, 127.2f * scale, 58.9f * scale, 129.6f * scale, 55.1f * scale, 133f * scale)
            cubicTo(46.2f * scale, 140.9f * scale, 41.8f * scale, 151.1f * scale, 41.8f * scale, 163.8f * scale)
            cubicTo(41.8f * scale, 185f * scale, 49f * scale, 199f * scale, 63.4f * scale, 205.9f * scale)
            cubicTo(77.8f * scale, 212.7f * scale, 99.1f * scale, 216.2f * scale, 127.5f * scale, 216.2f * scale)
            close()
            
            // Left eye area
            moveTo(87.5f * scale, 144.4f * scale)
            cubicTo(92f * scale, 144.4f * scale, 95.8f * scale, 146.6f * scale, 99f * scale, 151.1f * scale)
            cubicTo(102.3f * scale, 155.6f * scale, 103.9f * scale, 161f * scale, 103.9f * scale, 167.5f * scale)
            cubicTo(103.9f * scale, 179.5f * scale, 102.3f * scale, 179.5f * scale, 99f * scale, 183.9f * scale)
            cubicTo(95.7f * scale, 188.4f * scale, 92.9f * scale, 190.6f * scale, 87.5f * scale, 190.6f * scale)
            cubicTo(82.7f * scale, 190.6f * scale, 78.7f * scale, 188.4f * scale, 75.4f * scale, 183.9f * scale)
            cubicTo(72.1f * scale, 179f * scale, 70.5f * scale, 174f * scale, 70.5f * scale, 167.5f * scale)
            cubicTo(70.5f * scale, 155.5f * scale, 72.1f * scale, 155.5f * scale, 75.4f * scale, 151.1f * scale)
            cubicTo(78.7f * scale, 146.6f * scale, 82.7f * scale, 144.4f * scale, 87.5f * scale, 144.4f * scale)
            close()
        }
        
        drawPath(
            path = bodyPath,
            color = color,
            style = Fill
        )
        
        // Draw animated eyes (the two circles)
        // Right eye
        drawCircle(
            color = Color.Black,
            radius = 8f * scale,
            center = Offset(
                169f * scale + eyeOffsetX * scale,
                167.5f * scale + eyeOffsetY * scale
            )
        )
        
        // Left eye
        drawCircle(
            color = Color.Black,
            radius = 8f * scale,
            center = Offset(
                87.5f * scale + eyeOffsetX * scale,
                167.5f * scale + eyeOffsetY * scale
            )
        )
    }
}
