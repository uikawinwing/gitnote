package io.github.wiiznokes.gitnote.ui.screen.app.edit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.TextVM
import org.yaml.snakeyaml.Yaml
import kotlin.math.max
import kotlin.math.min

private data class StructureStatus(
    val ok: Boolean,
    val message: String,
)

@Composable
fun StructuredTextEditor(
    vm: TextVM,
    textFocusRequester: FocusRequester,
    textContent: TextFieldValue,
    extension: String,
    isReadOnlyModeActive: Boolean,
) {
    val fontSize by vm.prefs.editorFontSize.getAsState()
    val showLineNumbers by vm.prefs.showLinesNumber.getAsState()
    val showIndentGuides by vm.prefs.showIndentGuides.getAsState()
    val validateStructure by vm.prefs.validateTextStructure.getAsState()

    val status = remember(textContent.text, extension, validateStructure) {
        if (validateStructure) {
            validateTextStructure(extension, textContent.text)
        } else {
            null
        }
    }

    val lineHeight = (fontSize * 1.55f).sp
    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onBackground,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = lineHeight,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()
                val minEditorHeight = maxHeight
                val minEditorWidth = maxWidth

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                        .heightIn(min = minEditorHeight)
                ) {
                    if (showLineNumbers) {
                        val lines = textContent.text.split("\n")
                        val digits = max(2, lines.size.toString().length)
                        val gutter = remember(textContent.text, showIndentGuides) {
                            lines.mapIndexed { index, line ->
                                val indent = if (showIndentGuides) indentDepth(line) else 0
                                val guides = if (indent > 0) {
                                    " " + "│".repeat(min(indent, 5))
                                } else {
                                    ""
                                }
                                (index + 1).toString().padStart(digits, ' ') + guides
                            }.joinToString("\n")
                        }

                        Text(
                            text = gutter,
                            modifier = Modifier.padding(start = 10.dp, top = 12.dp, end = 8.dp),
                            style = textStyle.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (fontSize - 2).coerceAtLeast(10).sp,
                            ),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll)
                    ) {
                        BasicTextField(
                            modifier = Modifier
                                .widthIn(min = minEditorWidth)
                                .heightIn(min = minEditorHeight)
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                                .focusRequester(textFocusRequester),
                            value = textContent,
                            onValueChange = { vm.onValueChange(it) },
                            readOnly = isReadOnlyModeActive,
                            textStyle = textStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        )
                    }
                }
            }
        }

        if (status != null) {
            Text(
                text = status.message,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (status.ok) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

private fun indentDepth(line: String): Int {
    var spaces = 0
    var tabs = 0
    for (char in line) {
        when (char) {
            ' ' -> spaces += 1
            '\t' -> tabs += 1
            else -> break
        }
    }
    return tabs + (spaces / 2)
}

private fun validateTextStructure(extension: String, text: String): StructureStatus? {
    return when (extension.lowercase()) {
        "yaml", "yml" -> validateYaml(text)
        "md", "markdown" -> validateMarkdown(text)
        else -> null
    }
}

private fun validateYaml(text: String): StructureStatus {
    if (text.isBlank()) return StructureStatus(true, "YAML structure ✓")

    return try {
        Yaml().load<Any?>(text)
        StructureStatus(true, "YAML structure ✓")
    } catch (e: Exception) {
        val detail = e.message
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(100)
            ?: "invalid structure"
        StructureStatus(false, "YAML: $detail")
    }
}

private fun validateMarkdown(text: String): StructureStatus {
    val lines = text.lines()
    var fence: String? = null
    var lastHeading = 0

    lines.forEachIndexed { index, raw ->
        val line = raw.trimStart()

        if (line.startsWith("```") || line.startsWith("~~~")) {
            val token = if (line.startsWith("```")) "```" else "~~~"
            fence = if (fence == null) token else if (fence == token) null else fence
            return@forEachIndexed
        }

        if (fence == null) {
            val hashes = line.takeWhile { it == '#' }.length
            if (hashes in 1..6 && line.getOrNull(hashes) == ' ') {
                if (lastHeading > 0 && hashes > lastHeading + 1) {
                    return StructureStatus(
                        false,
                        "Markdown: heading jumps H$lastHeading → H$hashes at line ${index + 1}"
                    )
                }
                lastHeading = hashes
            }
        }
    }

    return if (fence != null) {
        StructureStatus(false, "Markdown: unclosed $fence code fence")
    } else {
        StructureStatus(true, "Markdown structure ✓")
    }
}
