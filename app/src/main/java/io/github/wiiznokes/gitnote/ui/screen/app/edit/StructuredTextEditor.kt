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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wiiznokes.gitnote.ui.viewmodel.edit.TextVM
import org.yaml.snakeyaml.Yaml
import kotlin.math.max

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
                        val gutter = remember(textContent.text) {
                            lines.indices.joinToString("\n") { index ->
                                (index + 1).toString().padStart(digits, ' ')
                            }
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

                    val editorLines = remember(textContent.text) {
                        splitLinesWithOffsets(textContent.text)
                    }
                    val indentUnit = remember(textContent.text) {
                        detectIndentUnit(editorLines.map { it.text })
                    }
                    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                    val rainbow = remember {
                        listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFFFFB86C),
                            Color(0xFFF1FA8C),
                            Color(0xFF50FA7B),
                            Color(0xFF8BE9FD),
                            Color(0xFFBD93F9),
                            Color(0xFFFF79C6),
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
                                .drawBehind {
                                    val layout = textLayout
                                    if (showIndentGuides && layout != null) {
                                        editorLines.forEach { editorLine ->
                                            val indentation = indentationOf(editorLine.text, indentUnit)
                                            if (indentation.guideOffsets.isEmpty()) return@forEach

                                            val firstVisualLine =
                                                layout.getLineForOffset(editorLine.startOffset)
                                            val lastOffset = if (editorLine.endOffset > editorLine.startOffset) {
                                                editorLine.endOffset - 1
                                            } else {
                                                editorLine.startOffset
                                            }
                                            val lastVisualLine = layout.getLineForOffset(lastOffset)
                                            val yStart = layout.getLineTop(firstVisualLine)
                                            val yEnd = layout.getLineBottom(lastVisualLine)

                                            indentation.guideOffsets
                                                .take(12)
                                                .forEachIndexed { levelIndex, localOffset ->
                                                    val globalOffset =
                                                        (editorLine.startOffset + localOffset)
                                                            .coerceAtMost(editorLine.endOffset)
                                                    val x = layout.getCursorRect(globalOffset).left

                                                    drawLine(
                                                        color = rainbow[levelIndex % rainbow.size]
                                                            .copy(alpha = 0.48f),
                                                        start = androidx.compose.ui.geometry.Offset(x, yStart),
                                                        end = androidx.compose.ui.geometry.Offset(x, yEnd),
                                                        strokeWidth = 1.dp.toPx(),
                                                    )
                                                }
                                        }
                                    }
                                }
                                .focusRequester(textFocusRequester),
                            value = textContent,
                            onValueChange = { vm.onValueChange(it) },
                            readOnly = isReadOnlyModeActive,
                            textStyle = textStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            onTextLayout = { textLayout = it },
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

private data class EditorLine(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private data class Indentation(
    val guideOffsets: List<Int>,
)

private fun splitLinesWithOffsets(text: String): List<EditorLine> {
    val result = mutableListOf<EditorLine>()
    var start = 0

    text.forEachIndexed { index, char ->
        if (char == '\n') {
            result += EditorLine(
                text = text.substring(start, index),
                startOffset = start,
                endOffset = index,
            )
            start = index + 1
        }
    }

    result += EditorLine(
        text = text.substring(start),
        startOffset = start,
        endOffset = text.length,
    )
    return result
}

/**
 * Infer indentation from the file itself instead of assuming 2 or 4 spaces.
 * The greatest common divisor of actual leading-space widths becomes one level.
 */
private fun detectIndentUnit(lines: List<String>): Int {
    val widths = lines.mapNotNull { line ->
        if (line.isBlank() || line.startsWith("\t")) return@mapNotNull null
        val spaces = line.takeWhile { it == ' ' }.length
        spaces.takeIf { it > 0 }
    }

    if (widths.isEmpty()) return 4

    val unit = widths.reduce(::greatestCommonDivisor)
    return unit.coerceIn(1, 8)
}

private fun greatestCommonDivisor(a: Int, b: Int): Int {
    var x = a
    var y = b
    while (y != 0) {
        val remainder = x % y
        x = y
        y = remainder
    }
    return x
}

private fun indentationOf(line: String, indentUnit: Int): Indentation {
    if (line.isBlank()) return Indentation(emptyList())

    val guideOffsets = mutableListOf<Int>()
    var visualColumns = 0
    var nextGuideColumn = indentUnit

    for ((index, char) in line.withIndex()) {
        when (char) {
            ' ' -> visualColumns += 1
            '\t' -> visualColumns += indentUnit
            else -> break
        }

        while (visualColumns >= nextGuideColumn) {
            guideOffsets += index + 1
            nextGuideColumn += indentUnit
        }
    }

    return Indentation(guideOffsets)
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
