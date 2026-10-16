package io.github.wiiznokes.gitnote.ui.screen.app

/**
 * Folder display preferences belong to the local Android app, not to the note
 * repository. Including the repository root keeps different repos independent.
 */
internal fun folderPreferencePrefix(repoPath: String): String =
    "${repoPath.length}:$repoPath:"

internal fun folderPreferenceKey(repoPath: String, relativePath: String): String =
    folderPreferencePrefix(repoPath) + relativePath

internal fun folderPathsForRepo(repoPath: String, savedKeys: Set<String>): Set<String> {
    val prefix = folderPreferencePrefix(repoPath)
    return savedKeys.asSequence()
        .filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix) }
        .toSet()
}

internal fun isFolderHidden(path: String, hiddenPaths: Set<String>): Boolean =
    hiddenPaths.any { hidden -> path == hidden || path.startsWith("$hidden/") }
