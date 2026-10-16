package io.github.wiiznokes.gitnote

import io.github.wiiznokes.gitnote.ui.screen.app.folderPathsForRepo
import io.github.wiiznokes.gitnote.ui.screen.app.folderPreferenceKey
import io.github.wiiznokes.gitnote.ui.screen.app.isFolderHidden
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderVisibilityTest {
    @Test
    fun preferencesAreSeparatedByRepository() {
        val saved = setOf(
            folderPreferenceKey("/notes", "A"),
            folderPreferenceKey("/notes2", "A"),
            folderPreferenceKey("/notes", "A/B"),
        )
        assertEquals(setOf("A", "A/B"), folderPathsForRepo("/notes", saved))
        assertEquals(setOf("A"), folderPathsForRepo("/notes2", saved))
    }

    @Test
    fun hidingParentHidesDescendantsButNotSimilarPrefix() {
        val hidden = setOf("A", "Other/sub")
        assertTrue(isFolderHidden("A", hidden))
        assertTrue(isFolderHidden("A/B", hidden))
        assertTrue(isFolderHidden("A/B/C", hidden))
        assertFalse(isFolderHidden("A2", hidden))
        assertFalse(isFolderHidden("Other", hidden))
        assertTrue(isFolderHidden("Other/sub/file", hidden))
    }

    @Test
    fun hiddenAndStarredSetsDoNotAlterTheActualFilePaths() {
        val repo = "/notes"
        val original = "worldbook_indexes/索伦蒂斯王国"
        val saved = setOf(folderPreferenceKey(repo, original))
        assertEquals(setOf(original), folderPathsForRepo(repo, saved))
        assertFalse(isFolderHidden(original, emptySet()))
    }
}
