package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.TextVM
import io.github.wiiznokes.gitnote.utils.getParentPath

val bottomBarHeight = 50.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultRow(
    vm: TextVM,
    modifier: Modifier = Modifier,
    isReadOnlyModeActive: Boolean,
    leftContent: @Composable () -> Unit = {}
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .scrollable(rememberScrollState(initial = 0), orientation = Orientation.Horizontal),
    ) {

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftContent()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val history = vm.historyManager.collectAsState().value

            SmallButton(
                onClick = { vm.undo() },
                enabled = !isReadOnlyModeActive && history.index > 0,
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "undo"
            )
            SmallButton(
                onClick = { vm.redo() },
                enabled = !isReadOnlyModeActive && history.size - 1 > history.index,
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "redo"
            )
        }

        val bottomSheetExpanded = rememberSaveable { mutableStateOf(false) }

        if (bottomSheetExpanded.value) {
            val findText = rememberSaveable { mutableStateOf("") }
            val replaceText = rememberSaveable { mutableStateOf("") }
            val matchCount = vm.matchCount(findText.value)

            ModalBottomSheet(onDismissRequest = { bottomSheetExpanded.value = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp),
                        text = stringResource(R.string.search_and_replace)
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = findText.value,
                        onValueChange = { findText.value = it },
                        label = { Text(stringResource(R.string.find_text)) },
                        singleLine = true,
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        value = replaceText.value,
                        onValueChange = { replaceText.value = it },
                        label = { Text(stringResource(R.string.replace_with)) },
                        singleLine = true,
                        enabled = !isReadOnlyModeActive,
                    )

                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.matches_count, matchCount)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { vm.findNext(findText.value) },
                            enabled = findText.value.isNotEmpty() && matchCount > 0,
                        ) {
                            Text(stringResource(R.string.find_next))
                        }

                        Button(
                            modifier = Modifier.padding(start = 8.dp),
                            onClick = {
                                vm.replaceCurrent(
                                    query = findText.value,
                                    replacement = replaceText.value
                                )
                            },
                            enabled = !isReadOnlyModeActive &&
                                findText.value.isNotEmpty() &&
                                matchCount > 0,
                        ) {
                            Text(stringResource(R.string.replace_current))
                        }
                    }

                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            vm.replaceAll(
                                query = findText.value,
                                replacement = replaceText.value
                            )
                        },
                        enabled = !isReadOnlyModeActive &&
                            findText.value.isNotEmpty() &&
                            matchCount > 0,
                    ) {
                        Text(stringResource(R.string.replace_all))
                    }

                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(
                            R.string.extension,
                            vm.previousNote.fileExtension().text
                        )
                    )
                    Text(
                        modifier = Modifier.padding(top = 6.dp),
                        text = stringResource(
                            R.string.parent_path,
                            getParentPath(vm.previousNote.relativePath)
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallButton(
                onClick = { bottomSheetExpanded.value = true },
                imageVector = Icons.Default.MoreVert,
                contentDescription = "more actions"
            )
        }
    }
}


@Composable
fun SmallSeparator(
) {
    VerticalDivider(
        modifier = Modifier.padding(horizontal = 5.dp),
        color = Color.Gray,
        thickness = 1.dp,
    )
}

@Composable
fun SmallButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    enabled: Boolean = true,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}