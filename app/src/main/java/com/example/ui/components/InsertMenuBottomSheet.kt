package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ShapeType
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertMenuBottomSheet(
    drawWithFingers: Boolean,
    onDrawWithFingersChange: (Boolean) -> Unit,
    onInsertImageClick: () -> Unit,
    onInsertTextClick: () -> Unit,
    onInsertShapeClick: (ShapeType) -> Unit,
    onInsertChartClick: () -> Unit,
    onPasteContentClick: () -> Unit,
    onRecognizeShapeClick: () -> Unit = {},
    onPlotFunctionClick: () -> Unit = {},
    onLatexConvertClick: () -> Unit = {},
    onInsertCodeClick: () -> Unit = {},
    onStudyDeckClick: () -> Unit = {},
    onSummarizeNotesClick: () -> Unit = {},
    onCreateQuizClick: () -> Unit = {},
    onSuggestLinksClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.insert_elements),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Insert Image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onInsertImageClick()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.insert_image),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Insert Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onInsertTextClick()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.insert_text_block),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Insert Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onInsertChartClick()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.insert_chart_grid),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Shapes selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.insert_shape),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ShapeType.entries.forEach { shapeType ->
                        Surface(
                            onClick = {
                                onInsertShapeClick(shapeType)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (shapeType) {
                                        ShapeType.CIRCLE -> "●"
                                        ShapeType.SQUARE -> "■"
                                        ShapeType.TRIANGLE -> "▲"
                                        ShapeType.ARROW -> "➔"
                                        ShapeType.STAR -> "★"
                                        ShapeType.BOLD_ARROW -> "🡺"
                                        ShapeType.HEXAGON -> "⬡"
                                        ShapeType.PENTAGON -> "⬟"
                                        ShapeType.CLOUD -> "☁"
                                        ShapeType.SPEECH_BUBBLE -> "💬"
                                    },
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = when (shapeType) {
                                        ShapeType.CIRCLE -> stringResource(R.string.shape_circle)
                                        ShapeType.SQUARE -> stringResource(R.string.shape_square)
                                        ShapeType.TRIANGLE -> stringResource(R.string.shape_triangle)
                                        ShapeType.ARROW -> stringResource(R.string.shape_arrow)
                                        ShapeType.STAR -> stringResource(R.string.shape_star)
                                        ShapeType.BOLD_ARROW -> stringResource(R.string.shape_bold_arrow)
                                        ShapeType.HEXAGON -> stringResource(R.string.shape_hexagon)
                                        ShapeType.PENTAGON -> stringResource(R.string.shape_pentagon)
                                        ShapeType.CLOUD -> stringResource(R.string.shape_cloud)
                                        ShapeType.SPEECH_BUBBLE -> stringResource(R.string.shape_speech_bubble)
                                    },
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Paste content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPasteContentClick()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.paste_content),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(R.string.study_tools),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyToolButton(stringResource(R.string.vectorize), { onRecognizeShapeClick(); onDismiss() }, Modifier.weight(1f))
                StudyToolButton(stringResource(R.string.plot_function), { onPlotFunctionClick(); onDismiss() }, Modifier.weight(1f))
                StudyToolButton(stringResource(R.string.latex_tool), { onLatexConvertClick(); onDismiss() }, Modifier.weight(1f))
                StudyToolButton(stringResource(R.string.code_lab_tool), { onInsertCodeClick(); onDismiss() }, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyToolButton(stringResource(R.string.summary_tool), { onSummarizeNotesClick(); onDismiss() }, Modifier.weight(1f))
                StudyToolButton(stringResource(R.string.quiz_tool), { onCreateQuizClick(); onDismiss() }, Modifier.weight(1f))
                StudyToolButton(stringResource(R.string.find_links_tool), { onSuggestLinksClick(); onDismiss() }, Modifier.weight(1f))
                StudyToolButton(stringResource(R.string.study_cards_tool), { onStudyDeckClick(); onDismiss() }, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gesture hint
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.undo_redo_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StudyToolButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
