package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neumorphic(
    isPressed: Boolean = false,
    cornerRadius: Dp = 16.dp,
    lightColor: Color = Color.White.copy(alpha = 0.7f),
    darkColor: Color = Color.Black.copy(alpha = 0.15f)
): Modifier = this.drawBehind {
    val cornerRadiusPx = cornerRadius.toPx()
    val shadowOffset = 6.dp.toPx()
    val blurRadius = 8.dp.toPx()

    drawIntoCanvas { canvas ->
        val lightPaint = Paint().apply {
            val frameworkPaint = asFrameworkPaint()
            frameworkPaint.color = lightColor.toArgb()
            frameworkPaint.setMaskFilter(
                android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
            )
        }
        val darkPaint = Paint().apply {
            val frameworkPaint = asFrameworkPaint()
            frameworkPaint.color = darkColor.toArgb()
            frameworkPaint.setMaskFilter(
                android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
            )
        }

        if (isPressed) {
            canvas.drawRoundRect(
                left = shadowOffset,
                top = shadowOffset,
                right = size.width + shadowOffset,
                bottom = size.height + shadowOffset,
                radiusX = cornerRadiusPx,
                radiusY = cornerRadiusPx,
                paint = lightPaint
            )
            canvas.drawRoundRect(
                left = -shadowOffset,
                top = -shadowOffset,
                right = size.width - shadowOffset,
                bottom = size.height - shadowOffset,
                radiusX = cornerRadiusPx,
                radiusY = cornerRadiusPx,
                paint = darkPaint
            )
        } else {
            canvas.drawRoundRect(
                left = -shadowOffset,
                top = -shadowOffset,
                right = size.width - shadowOffset,
                bottom = size.height - shadowOffset,
                radiusX = cornerRadiusPx,
                radiusY = cornerRadiusPx,
                paint = lightPaint
            )
            canvas.drawRoundRect(
                left = shadowOffset,
                top = shadowOffset,
                right = size.width + shadowOffset,
                bottom = size.height + shadowOffset,
                radiusX = cornerRadiusPx,
                radiusY = cornerRadiusPx,
                paint = darkPaint
            )
        }
    }
}
