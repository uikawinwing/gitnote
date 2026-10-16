package io.github.wiiznokes.gitnote.ui.screen.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Embedded
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.GetStringDialog
import io.github.wiiznokes.gitnote.ui.component.RequestConfirmationDialog
import kotlinx.coroutines.launch

data class DrawerFolderModel(
    @Embedded val noteFolder: NoteFolder,
    val noteCount: Int,
)

private fun ancestorsOf(path: String): Set<String> {
    if (path.isEmpty()) return emptySet()
    val parts = path.split("/")
    return (0 until (parts.size - 1))
        .map { index -> parts.take(index + 1).joinToString("/") }
        .toSet()
}

private fun isVisibleInTree(path: String, expanded: Set<String>): Boolean {
    val parent = path.substringBeforeLast("/", missingDelimiterValue = "")
    if (parent.isEmpty()) return true

    var current = parent
    while (current.isNotEmpty()) {
        if (current !in expanded) return false
        current = current.substringBeforeLast("/", missingDelimiterValue = "")
    }
    return true
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DrawerScreen(
    drawerState: DrawerState,
    currentNoteFolderRelativePath: String,
    drawerFolders: List<DrawerFolderModel>,
    pinnedPaths: Set<String>,
    hiddenPaths: Set<String>,
    openFolder: (String) -> Unit,
    deleteFolder: (NoteFolder) -> Unit,
    createNoteFolder: (relativeParentPath: String, name: String) -> Boolean,
    togglePinned: (String) -> Unit,
    toggleHidden: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var expandedFolders by remember { mutableStateOf(ancestorsOf(currentNoteFolderRelativePath)) }
    var showHidden by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentNoteFolderRelativePath) {
        expandedFolders = expandedFolders + ancestorsOf(currentNoteFolderRelativePath)
    }

    fun selectFolder(path: String) {
        openFolder(path)
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val childParents = remember(drawerFolders) {
        drawerFolders
            .map { it.noteFolder.relativePath.substringBeforeLast("/", missingDelimiterValue = "") }
            .toSet()
    }

    val visibleFolders = remember(drawerFolders, expandedFolders, hiddenPaths, showHidden) {
        drawerFolders.filter { folder ->
            val path = folder.noteFolder.relativePath
            isVisibleInTree(path, expandedFolders) &&
                (showHidden || !isFolderHidden(path, hiddenPaths))
        }
    }
    val favoriteFolders = remember(drawerFolders, pinnedPaths, hiddenPaths, showHidden) {
        drawerFolders.filter { folder ->
            val path = folder.noteFolder.relativePath
            path in pinnedPaths && (showHidden || !isFolderHidden(path, hiddenPaths))
        }.sortedBy { it.noteFolder.relativePath.lowercase() }
    }
    val hasHiddenFolders = remember(drawerFolders, hiddenPaths) {
        drawerFolders.any { it.noteFolder.relativePath in hiddenPaths }
    }

    Scaffold(
        topBar = {
            FolderTreeTopBar(
                currentPath = currentNoteFolderRelativePath,
                selectRoot = { selectFolder("") },
                createNoteFolder = createNoteFolder
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (favoriteFolders.isNotEmpty()) {
                item(key = "favorites_heading") {
                    Text(
                        text = "Starred folders",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(
                    favoriteFolders,
                    key = { "favorite:" + it.noteFolder.relativePath }
                ) { favorite ->
                    val path = favorite.noteFolder.relativePath
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectFolder(path) }
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null)
                        Text(
                            text = favorite.noteFolder.fullName(),
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { togglePinned(path) }) {
                            Icon(Icons.Rounded.Star, contentDescription = "Remove favorite")
                        }
                    }
                }
            }

            if (hasHiddenFolders) {
                item(key = "show_hidden_folders") {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showHidden = !showHidden }
                    ) {
                        Icon(
                            imageVector = if (showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null
                        )
                        Text(
                            text = if (showHidden) "Hide hidden folders" else "Show hidden folders",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            items(
                visibleFolders,
                key = { it.noteFolder.id }
            ) { drawerFolder ->
                val path = drawerFolder.noteFolder.relativePath
                val depth = path.count { it == '/' }
                val hasChildren = path in childParents
                val expanded = path in expandedFolders
                val selected = path == currentNoteFolderRelativePath

                Box {
                    val dropDownExpanded = remember { mutableStateOf(false) }
                    val deleteConfirmation = remember { mutableStateOf(false) }
                    val clickPosition = remember { mutableStateOf(Offset.Zero) }
                    val pinned = path in pinnedPaths
                    val explicitlyHidden = path in hiddenPaths

                    RequestConfirmationDialog(
                        expanded = deleteConfirmation,
                        text = "Delete folder \"$path\" and all its contents? This cannot be undone."
                    ) { deleteFolder(drawerFolder.noteFolder) }

                    CustomDropDown(
                        expanded = dropDownExpanded,
                        shape = MaterialTheme.shapes.medium,
                        options = listOf(
                            CustomDropDownModel(
                                text = if (pinned) "Unstar folder" else "Star folder",
                                onClick = { togglePinned(path) }
                            ),
                            CustomDropDownModel(
                                text = if (explicitlyHidden) "Show folder" else "Hide folder",
                                onClick = { toggleHidden(path) }
                            ),
                            CustomDropDownModel(
                                text = stringResource(R.string.delete_this_folder),
                                onClick = { deleteConfirmation.value = true }
                            )
                        ),
                        clickPosition = clickPosition
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .combinedClickable(
                                onLongClick = { dropDownExpanded.value = true },
                                onClick = { selectFolder(path) }
                            )
                            .pointerInteropFilter {
                                clickPosition.value = Offset(it.x, it.y)
                                false
                            }
                            .padding(
                                start = (8 + depth * 18).dp,
                                end = 8.dp,
                                top = 7.dp,
                                bottom = 7.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasChildren) {
                                IconButton(
                                    modifier = Modifier.size(44.dp),
                                    onClick = {
                                        expandedFolders = if (expanded) {
                                            expandedFolders - path
                                        } else {
                                            expandedFolders + path
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (expanded) {
                                            Icons.Rounded.KeyboardArrowDown
                                        } else {
                                            Icons.Rounded.KeyboardArrowRight
                                        },
                                        contentDescription = if (expanded) "Collapse folder" else "Expand folder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Icon(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(24.dp),
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = drawerFolder.noteFolder.fullName(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isFolderHidden(path, hiddenPaths)) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        Text(
                            text = drawerFolder.noteCount.toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )

                        IconButton(
                            modifier = Modifier.size(34.dp),
                            onClick = { dropDownExpanded.value = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Folder actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowNFoldersNavigation(
    currentPath: String,
    openFolder: (String) -> Unit,
    createNoteFolder: (relativeParentPath: String, name: String) -> Boolean,
) {
    FolderTreeTopBar(
        currentPath = currentPath,
        selectRoot = { openFolder("") },
        createNoteFolder = createNoteFolder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderTreeTopBar(
    currentPath: String,
    selectRoot: () -> Unit,
    createNoteFolder: (relativeParentPath: String, name: String) -> Boolean,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = selectRoot) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        title = {
            Text(
                text = currentPath.ifEmpty { "Folders" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        },
        actions = {
            val showCreateNewFolder = rememberSaveable { mutableStateOf(false) }

            IconButton(onClick = { showCreateNewFolder.value = true }) {
                Icon(
                    imageVector = Icons.Rounded.CreateNewFolder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GetStringDialog(
                expanded = showCreateNewFolder,
                label = stringResource(R.string.new_folder_label),
                actionText = stringResource(R.string.create_new_folder),
                unExpandedOnValidation = false
            ) {
                if (createNoteFolder(currentPath, it)) {
                    showCreateNewFolder.value = false
                }
            }
        }
    )
}
