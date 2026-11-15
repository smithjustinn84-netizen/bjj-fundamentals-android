package dev.justinsmith.bjj.ui.backup

import androidx.compose.runtime.Composable

/** The system's way of saving a file somewhere the user picks, and of picking one to read. */
class BackupFiles(
    /** Asks where to save a file of this name holding this text. */
    val save: (filename: String, text: String) -> Unit,
    /** Asks for a file to read. */
    val open: () -> Unit,
)

/**
 * The file pickers. [onSave] hears whether the file was written (false also when the user backed out), and
 * [onOpen] the text of the file picked, or null when none was or it could not be read.
 */
@Composable
expect fun rememberBackupFiles(onSave: (Boolean) -> Unit, onOpen: (String?) -> Unit): BackupFiles
