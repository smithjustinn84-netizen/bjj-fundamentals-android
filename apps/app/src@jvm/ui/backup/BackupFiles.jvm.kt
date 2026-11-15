package dev.justinsmith.bjj.ui.backup

import androidx.compose.runtime.Composable

/** The JVM only runs tests, which have no file picker: saving reports failure and opening finds nothing. */
@Composable
actual fun rememberBackupFiles(onSave: (Boolean) -> Unit, onOpen: (String?) -> Unit): BackupFiles =
    BackupFiles(save = { _, _ -> onSave(false) }, open = { onOpen(null) })
