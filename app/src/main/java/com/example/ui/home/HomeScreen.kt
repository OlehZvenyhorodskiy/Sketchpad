package com.example.ui.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.components.BackgroundColorPicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.models.CanvasEntity
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCanvasClick: (String) -> Unit,
    onOpenThemeSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val canvases by viewModel.canvases.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Мої канви, 1: Шаблони
    var canvasToRename by remember { mutableStateOf<CanvasEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }
    var profileAvatarInput by remember { mutableStateOf("🎓") }

    var showCreateCanvasDialog by remember { mutableStateOf(false) }
    val defaultNewCanvasTitle = stringResource(R.string.new_canvas_default_title)
    var newCanvasTitle by remember(defaultNewCanvasTitle) { mutableStateOf(defaultNewCanvasTitle) }
    var selectedPreset by remember { mutableStateOf(com.example.data.models.PageSizePreset.UNLIMITED) }
    var selectedColorInt by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }
    var selectedPattern by remember { mutableStateOf(com.example.data.models.BackgroundPattern.DOTTED) }

    // File picker for import PDF or photo
    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importPdfOrImage(context, it) { canvasId ->
                onCanvasClick(canvasId)
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some document providers grant access without exposing a persistable permission.
            }
            profileAvatarInput = it.toString()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text(stringResource(R.string.search_by_title)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Sketchpad",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.home_tagline),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setSearchActive(!isSearchActive) }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }

                        IconButton(onClick = onOpenThemeSettings) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = stringResource(R.string.theme_and_appearance))
                        }

                        OutlinedButton(
                            onClick = { importPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.import_content))
                        }

                        // Local profile button
                        Box {
                            IconButton(onClick = { showAccountMenu = true }) {
                                LocalProfileAvatar(
                                    avatar = userAvatar,
                                    name = userName,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(userName ?: stringResource(R.string.user), fontWeight = FontWeight.Bold)
                                            Text(stringResource(R.string.local_profile), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    leadingIcon = {
                                        LocalProfileAvatar(
                                            avatar = userAvatar,
                                            name = userName,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    onClick = {
                                        showAccountMenu = false
                                        profileNameInput = userName.orEmpty()
                                        profileAvatarInput = userAvatar ?: "🎓"
                                        showProfileDialog = true
                                    }
                                )
                                androidx.compose.material3.HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_local_profile)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showAccountMenu = false
                                        profileNameInput = userName.orEmpty()
                                        profileAvatarInput = userAvatar ?: "🎓"
                                        showProfileDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.theme_and_appearance)) },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                    onClick = {
                                        showAccountMenu = false
                                        onOpenThemeSettings()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(stringResource(R.string.my_canvases_count, canvases.size)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(stringResource(R.string.note_templates)) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateCanvasDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.create_canvas))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (selectedTabIndex == 0) {
                // My Canvases Tab
                if (canvases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_canvases),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.no_canvases_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(canvases, key = { it.id }) { canvas ->
                            CanvasCardItem(
                                canvas = canvas,
                                onClick = { onCanvasClick(canvas.id) },
                                onRenameClick = {
                                    canvasToRename = canvas
                                    renameInputText = canvas.title
                                },
                                onDuplicateClick = { viewModel.duplicateCanvas(canvas.id) },
                                onDeleteClick = { viewModel.deleteCanvas(canvas.id) }
                            )
                        }
                    }
                }
            } else {
                // Templates Tab
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TemplateCardItem(
                        title = stringResource(R.string.template_blank_title),
                        description = stringResource(R.string.template_blank_description),
                        icon = Icons.Default.Description,
                        onClick = {
                            viewModel.createTemplateCanvas("BLANK") { canvasId ->
                                onCanvasClick(canvasId)
                            }
                        }
                    )

                    TemplateCardItem(
                        title = stringResource(R.string.template_graph_title),
                        description = stringResource(R.string.template_graph_description),
                        icon = Icons.Default.GridOn,
                        onClick = {
                            viewModel.createTemplateCanvas("GRID_CHART") { canvasId ->
                                onCanvasClick(canvasId)
                            }
                        }
                    )

                    TemplateCardItem(
                        title = stringResource(R.string.template_cornell_title),
                        description = stringResource(R.string.template_cornell_description),
                        icon = Icons.Default.Edit,
                        onClick = {
                            viewModel.createTemplateCanvas("LECTURE_NOTES") { canvasId ->
                                onCanvasClick(canvasId)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showProfileDialog) {
        val emojiOptions = listOf("🎓", "📚", "🧠", "✏️", "🦉", "🚀", "🔬", "🧪", "📐", "💻", "🌌", "☕")
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(R.string.local_profile)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LocalProfileAvatar(
                        avatar = profileAvatarInput,
                        name = profileNameInput,
                        modifier = Modifier.size(80.dp)
                    )
                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text(stringResource(R.string.profile_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.ready_avatar), style = MaterialTheme.typography.labelLarge)
                    emojiOptions.chunked(6).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowOptions.forEach { emoji ->
                                OutlinedButton(
                                    onClick = { profileAvatarInput = emoji },
                                    modifier = Modifier.size(44.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = if (profileAvatarInput == emoji) {
                                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    } else {
                                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                                    }
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { avatarPickerLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.choose_photo_gallery))
                    }
                    Text(
                        stringResource(R.string.profile_local_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLocalProfile(profileNameInput, profileAvatarInput)
                        showProfileDialog = false
                    },
                    enabled = profileNameInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Rename Dialog
    canvasToRename?.let { canvas ->
        AlertDialog(
            onDismissRequest = { canvasToRename = null },
            title = { Text(stringResource(R.string.rename_canvas)) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.canvas_name)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInputText.isNotBlank()) {
                        viewModel.renameCanvas(canvas, renameInputText.trim())
                    }
                    canvasToRename = null
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { canvasToRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // New Canvas Creation Dialog
    if (showCreateCanvasDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCanvasDialog = false },
            title = { Text(stringResource(R.string.new_canvas_settings), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCanvasTitle,
                        onValueChange = { newCanvasTitle = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.note_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(stringResource(R.string.sheet_format), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Pair(stringResource(R.string.unlimited), com.example.data.models.PageSizePreset.UNLIMITED),
                            Pair(stringResource(R.string.a4_portrait_short), com.example.data.models.PageSizePreset.A4_VERTICAL),
                            Pair(stringResource(R.string.a4_landscape_short), com.example.data.models.PageSizePreset.A4_HORIZONTAL)
                        )
                        presets.forEach { (label, preset) ->
                            val isSelected = selectedPreset == preset
                            OutlinedButton(
                                onClick = { selectedPreset = preset },
                                modifier = Modifier.weight(1f),
                                colors = if (isSelected) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(label, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                    BackgroundColorPicker(
                        selectedColor = selectedColorInt,
                        onSelectColor = { selectedColorInt = it }
                    )

                    Text(stringResource(R.string.grid_pattern), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val patternOpts = listOf(
                            Pair(stringResource(R.string.dots), com.example.data.models.BackgroundPattern.DOTTED),
                            Pair(stringResource(R.string.lines), com.example.data.models.BackgroundPattern.LINED),
                            Pair(stringResource(R.string.blank), com.example.data.models.BackgroundPattern.BLANK)
                        )
                        patternOpts.forEach { (lbl, pat) ->
                            val isSel = selectedPattern == pat
                            OutlinedButton(
                                onClick = { selectedPattern = pat },
                                modifier = Modifier.weight(1f),
                                colors = if (isSel) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(lbl, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showCreateCanvasDialog = false
                    viewModel.createNewCanvas(
                        title = newCanvasTitle.ifBlank { defaultNewCanvasTitle },
                        pageSizePreset = selectedPreset,
                        pattern = selectedPattern,
                        bgColor = selectedColorInt
                    ) { canvasId ->
                        onCanvasClick(canvasId)
                    }
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCanvasDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun LocalProfileAvatar(
    avatar: String?,
    name: String?,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (avatar?.startsWith("content://") == true || avatar?.startsWith("file://") == true) {
                AsyncImage(
                    model = avatar,
                    contentDescription = stringResource(R.string.local_profile_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape)
                )
            } else {
                val fallback = name?.trim()?.take(2)?.uppercase().orEmpty().ifBlank { "🎓" }
                Text(
                    text = avatar?.ifBlank { fallback } ?: fallback,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CanvasCardItem(
    canvas: CanvasEntity,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember(Locale.getDefault()) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column {
            // Thumbnail Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!canvas.thumbnailPath.isNullOrEmpty() && File(canvas.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(canvas.thumbnailPath),
                        contentDescription = canvas.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing Grid Preview Graphic
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridStep = 24.dp.toPx()
                            val lineColor = Color(0x1A000000)
                            var x = 0f
                            while (x < size.width) {
                                drawLine(lineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                                x += gridStep
                            }
                            var y = 0f
                            while (y < size.height) {
                                drawLine(lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                                y += gridStep
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = canvas.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormat.format(Date(canvas.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.options),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicate)) },
                            leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicateClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateCardItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
