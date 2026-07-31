package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass surface inspired by iOS / Liquid Glass design.
 *
 * On API 31+ applies real Gaussian blur via [Modifier.blur] to an isolated background layer
 * so child content (buttons, text, icons) stays crisp and unblurred.
 * On older APIs falls back to a semi-transparent gradient overlay.
 *
 * Includes a subtle "glare" highlight gradient at the top edge to
 * mimic a light reflection on glass.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    // Semi-transparent frosted background gradient
    val glassBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    // Top "glare" highlight — thin white-to-transparent stripe
    val glareBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.22f),
            Color.Transparent
        )
    )

    // Build the blur modifier conditionally for API 31+
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(blurRadius)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(16.dp, shape = shape, spotColor = Color(0x40000000))
            .clip(shape)
    ) {
        // Isolated background layer that receives the blur modifier
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(blurModifier)
                .background(Color.White.copy(alpha = 0.12f))
                .background(glassBrush)
        )

        // Subtle border stroke overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color.White.copy(alpha = 0.4f), shape)
        )

        // Glare highlight at the very top of the panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.TopCenter)
                .background(glareBrush)
        )

        // Crisp, unblurred content layer
        content()
    }
}
