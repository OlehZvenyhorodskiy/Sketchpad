package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemedPanel

@Composable
fun BottomLeftOverlay(
    currentPage: Int,
    totalPages: Int,
    zoomPercentage: Int,
    onPageIndicatorClick: () -> Unit,
    onZoomIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ThemedPanel(
        modifier = modifier,
        shadowElevation = 4.dp,
        tonalElevation = 4.dp,
        paperRotation = 0f
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page Count Indicator Button
            Text(
                text = "📄 ${currentPage + 1} / $totalPages",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onPageIndicatorClick() }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Zoom Percentage Button
            Text(
                text = "🔍 $zoomPercentage%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { onZoomIndicatorClick() }
            )
        }
    }
}

