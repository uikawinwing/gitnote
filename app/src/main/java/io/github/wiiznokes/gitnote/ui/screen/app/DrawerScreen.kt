package io.github.wiiznokes.gitnote.ui.screen.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Embedded
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.NoteFolder
import io.github.wiiznokes.gitnote.ui.component.CustomDropDown
import io.github.wiiznokes.gitnote.ui.component.CustomDropDownModel
import io.github.wiiznokes.gitnote.ui.component.GetStringDialog
import io.github.wiiznokes.gitnote.ui.component.SimpleIcon
import io.github.wiiznokes.gitnote.ui.component.SimpleSpacer
import io.github.wiiznokes.gitnote.ui.theme.IconDefaultSize
import io.github.wiiznokes.gitnote.ui.theme.LocalSpaces
import io.github.wiiznokes.gitnote.utils.getParentPath
import kotlinx.coroutines.launch


private const val TAG = "DrawerScreen"

data class DrawerFolderModel(
    @Embedded val noteFolder: NoteFolder,
    val noteCount: Int,
)


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DrawerScreen(
    drawerState: DrawerState,
    currentNoteFolderRelativePath: String,
    drawerFolders: List<DrawerFolderModel>,
    openFolder: (String) -> Unit,
    deleteFolder: (NoteFolder) -> Unit,
    createNoteFolder: (relativeParentPath: String, name: String) -> Boolean,
) {
    val scope = rememberCoroutineScope()

    fun selectFolder(path: String) {
        openFolder(path)
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = drawerState.isOpen || currentNoteFolderRelativePath.isNotEmpty()) {
        if (currentNoteFolderRelativePath.isEmpty()) {
            scope.launch { drawerState.close() }
        } else {
            openFolder(getParentPath(currentNoteFolderRelativePath))
        }
    }

    Scaffold(
        topBar = {
            RowNFoldersNavigation(
                currentPath = currentNoteFolderRelativePath,
                openFolder = openFolder,
                createNoteFolder = createNoteFolder
            )
        },
        floatingActionButton = {
            if (currentNoteFolderRelativePath.isNotEmpty()) {
                FloatingActionButton(
                    modifier = Modifier,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        openFolder(getParentPath(currentNoteFolderRelativePath))
                    }
                ) {
                    SimpleIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.padding(paddingValues = paddingValues),
            state = listState
        ) {
            items(
                drawerFolders,
                key = { it.noteFolder.id }
            ) { drawerNoteFolder ->
                Box {
                    val dropDownExpanded = remember { mutableStateOf(false) }
                    val clickPosition = remember { mutableStateOf(Offset.Zero) }

                    CustomDropDown(
                        expanded = dropDownExpanded,
                        shape = MaterialTheme.shapes.medium,
                        options = listOf(
                            CustomDropDownModel(
                                text = stringResource(R.string.delete_this_folder),
                                onClick = {
                                    deleteFolder(drawerNoteFolder.noteFolder)
                                }
                            ),
                        ),
                        clickPosition = clickPosition
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onLongClick = {
                                    dropDownExpanded.value = true
                                },
                                onClick = {
                                    selectFolder(drawerNoteFolder.noteFolder.relativePath)
                                }
                            )
                            .pointerInteropFilter {
                                clickPosition.value = Offset(it.x, it.y)
                                false
                            }
                            .padding(
                                start = LocalSpaces.current.smallPadding,
                                end = 4.dp,
                                top = LocalSpaces.current.smallPadding,
                                bottom = LocalSpaces.current.smallPadding
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleIcon(
                            modifier = Modifier.size(IconDefaultSize),
                            imageVector = Icons.Rounded.Folder
                        )

                        SimpleSpacer(width = LocalSpaces.current.smallPadding)

                        Text(
                            text = drawerNoteFolder.noteFolder.fullName(),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = drawerNoteFolder.noteCount.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = {
                                openFolder(drawerNoteFolder.noteFolder.relativePath)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowRight,
                                contentDescription = null,
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
    val containers = if (currentPath.isEmpty()) emptyList() else currentPath.split('/')

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = {
                    openFolder("")
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = null,
                )
            }
        },
        title = {
            LazyRow {
                itemsIndexed(containers) { index, item ->
                    if (index != 0) {
                        Text(
                            text = " › ",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                        )
                    }

                    Text(
                        modifier = Modifier.clickable {
                            val path = containers
                                .take(index + 1)
                                .joinToString("/")
                            openFolder(path)
                        },
                        text = item,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (index == containers.lastIndex) {
                                TextDecoration.None
                            } else {
                                TextDecoration.Underline
                            }
                        )
                    )
                }
            }
        },
        actions = {
            val showCreateNewFolder = rememberSaveable {
                mutableStateOf(false)
            }

            IconButton(onClick = {
                showCreateNewFolder.value = true
            }) {
                SimpleIcon(
                    imageVector = Icons.Rounded.CreateNewFolder
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
