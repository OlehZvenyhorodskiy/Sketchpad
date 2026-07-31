package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.models.CanvasReferenceDestination
import com.example.data.models.CanvasReferenceDestinationPage
import com.example.data.models.CanvasReferenceEntity
import com.example.data.models.CanvasReferenceNavigationRequest
import com.example.data.models.CanvasViewport

/** UI copy is injectable so app localization can be wired without coupling this component to resources. */
data class CanvasReferenceUiText(
    val addTitle: String = "Link to a note",
    val selectionLabel: (Int) -> String = { count ->
        if (count == 1) "1 selected item" else "$count selected items"
    },
    val searchHint: String = "Search sketchpads",
    val noDestinations: String = "No matching pages",
    val pageLabel: (Int) -> String = { pageIndex -> "Page ${pageIndex + 1}" },
    val pageCountLabel: (Int) -> String = { count -> if (count == 1) "1 page" else "$count pages" },
    val cancel: String = "Cancel",
    val frameTarget: String = "Frame the destination",
    val frameTargetHint: String = "Pan and zoom to the exact place this link should open.",
    val saveLink: String = "Save link",
    val linksTitle: String = "Linked notes",
    val noLinks: String = "This selection has no links yet.",
    val open: String = "Open",
    val delete: String = "Delete"
)

@Composable
fun CanvasReferenceDestinationDialog(
    destinations: List<CanvasReferenceDestination>,
    sourceSelectionSize: Int,
    preferredCanvasId: String? = null,
    onDestinationSelected: (CanvasReferenceDestinationPage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: CanvasReferenceUiText = CanvasReferenceUiText()
) {
    var query by remember { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val filteredDestinations = remember(destinations, query) {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            destinations
        } else {
            destinations.filter { destination ->
                destination.canvasTitle.contains(normalized, ignoreCase = true) ||
                    destination.pages.any {
                        text.pageLabel(it.pageIndex).contains(normalized, ignoreCase = true)
                    }
            }
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Link, contentDescription = null) },
        title = {
            Column {
                Text(text.addTitle)
                Text(
                    text = text.selectionLabel(sourceSelectionSize),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(text.searchHint) }
                )
                Spacer(Modifier.height(12.dp))
                if (filteredDestinations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text.noDestinations,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredDestinations.forEach { destination ->
                            item(key = "canvas-${destination.canvasId}") {
                                val isExpanded = expanded[destination.canvasId]
                                    ?: (destination.canvasId == preferredCanvasId || filteredDestinations.size == 1)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expanded[destination.canvasId] = !isExpanded
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = destination.canvasTitle,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = text.pageCountLabel(destination.pages.size),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) {
                                                Icons.Default.ExpandLess
                                            } else {
                                                Icons.Default.ExpandMore
                                            },
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                            if (expanded[destination.canvasId]
                                    ?: (destination.canvasId == preferredCanvasId || filteredDestinations.size == 1)
                            ) {
                                items(
                                    items = destination.pages,
                                    key = { page -> "page-${page.pageId}" }
                                ) { page ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onDestinationSelected(page) }
                                            .padding(start = 22.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = text.pageLabel(page.pageIndex),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = text.open)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text.cancel) }
        }
    )
}

/**
 * Display this after opening the selected destination page. Feed it the current editor viewport;
 * confirmation captures the exact world center and zoom into the link draft.
 */
@Composable
fun CanvasReferenceTargetCaptureBar(
    destination: CanvasReferenceDestinationPage,
    currentViewport: CanvasViewport,
    onConfirm: (CanvasViewport) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    text: CanvasReferenceUiText = CanvasReferenceUiText()
) {
    val safeViewport = currentViewport.normalized()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = text.frameTarget,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${destination.canvasTitle} | ${text.pageLabel(destination.pageIndex)} | " +
                        "${(safeViewport.zoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = text.frameTargetHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onCancel) { Text(text.cancel) }
            Button(onClick = { onConfirm(safeViewport) }) { Text(text.saveLink) }
        }
    }
}

data class CanvasReferenceListItem(
    val reference: CanvasReferenceEntity,
    val destinationCanvasTitle: String,
    val destinationPageIndex: Int
)

@Composable
fun CanvasReferenceListDialog(
    references: List<CanvasReferenceListItem>,
    onOpen: (CanvasReferenceNavigationRequest) -> Unit,
    onDelete: (referenceId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: CanvasReferenceUiText = CanvasReferenceUiText()
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Link, contentDescription = null) },
        title = { Text(text.linksTitle) },
        text = {
            if (references.isEmpty()) {
                Text(text.noLinks, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(references, key = { it.reference.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(item.reference.toNavigationRequest()) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.reference.label.ifBlank {
                                        item.destinationCanvasTitle
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.destinationCanvasTitle} | " +
                                        text.pageLabel(item.destinationPageIndex),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDelete(item.reference.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = text.delete,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = text.open)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text.cancel) }
        }
    )
}
