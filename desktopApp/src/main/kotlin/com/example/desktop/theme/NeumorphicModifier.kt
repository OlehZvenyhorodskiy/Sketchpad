package com.example.desktop.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neumorphic(
    isPressed: Boolean = false,
    cornerRadius: Dp = 16.dp,
    lightColor: Color = Color.White.copy(alpha = 0.5f),
    darkColor: Color = Color.Black.copy(alpha = 0.18f)
): Modifier = this.drawBehind {
    val cornerRadiusPx = cornerRadius.toPx()
    val shadowOffset = 4.dp.toPx()

    if (isPressed) {
        // Inner/pressed shadow simulation
        drawRoundRect(
            color = darkColor,
            topLeft = Offset(-shadowOffset / 2, -shadowOffset / 2),
            size = Size(size.width + shadowOffset, size.height + shadowOffset),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        drawRoundRect(
            color = lightColor,
            topLeft = Offset(shadowOffset / 2, shadowOffset / 2),
            size = Size(size.width - shadowOffset, size.height - shadowOffset),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    } else {
        // Light top-left reflection
        drawRoundRect(
            color = lightColor,
            topLeft = Offset(-shadowOffset, -shadowOffset),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        // Dark bottom-right shadow
        drawRoundRect(
            color = darkColor,
            topLeft = Offset(shadowOffset, shadowOffset),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    }
}
