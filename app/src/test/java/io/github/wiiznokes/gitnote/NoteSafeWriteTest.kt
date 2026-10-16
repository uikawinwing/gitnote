package io.github.wiiznokes.gitnote

import io.github.wiiznokes.gitnote.data.platform.NodeFs
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteSafeWriteTest {
    @Test
    fun replacesExistingNoteWithoutLeavingTemporaryFiles() {
        val folder = Files.createTempDirectory("gitnote-safe-write-")
        try {
            val path = folder.resolve("note.md")
            Files.writeString(path, "old content")
            val result = NodeFs.File.fromPath(path.toString()).writeSafely("new content")
            assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
            assertEquals("new content", Files.readString(path))
            Files.list(folder).use { files ->
                assertEquals(1L, files.count())
            }
        } finally {
            folder.toFile().deleteRecursively()
        }
    }

    @Test
    fun createsNewNoteAndItsParentDirectory() {
        val folder = Files.createTempDirectory("gitnote-new-note-")
        try {
            val path = folder.resolve("subfolder").resolve("note.md")
            assertTrue(NodeFs.File.fromPath(path.toString()).writeSafely("内容").isSuccess)
            assertEquals("内容", Files.readString(path))
        } finally {
            folder.toFile().deleteRecursively()
        }
    }

    @Test
    fun failedReplacementDoesNotDestroyExistingDirectory() {
        val folder = Files.createTempDirectory("gitnote-failed-note-")
        try {
            val target = folder.resolve("existing.md")
            Files.createDirectory(target)
            val kept = target.resolve("important.txt")
            Files.writeString(kept, "original")
            val result = NodeFs.File.fromPath(target.toString()).writeSafely("replacement")
            assertTrue(result.isFailure)
            assertEquals("original", Files.readString(kept))
            Files.list(folder).use { files ->
                assertEquals(1L, files.count())
            }
            assertFalse(Files.isRegularFile(target))
        } finally {
            folder.toFile().deleteRecursively()
        }
    }
}
