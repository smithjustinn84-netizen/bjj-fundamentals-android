package dev.justinsmith.bjj.ui.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val JSON = "application/json"

@Composable
actual fun rememberBackupFiles(onSave: (Boolean) -> Unit, onOpen: (String?) -> Unit): BackupFiles {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by rememberUpdatedState(onSave)
    val opened by rememberUpdatedState(onOpen)
    // The text waits here while the user picks where it goes.
    var pending by remember { mutableStateOf<String?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        val text = pending
        pending = null
        scope.launch { saved(uri != null && text != null && context.write(uri, text)) }
    }
    // Some file apps give a backup a plain-text or unknown type, so anything may be picked; the app checks it.
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        scope.launch { opened(uri?.let { context.read(it) }) }
    }
    return remember(create, pick) {
        BackupFiles(
            save = { filename, text ->
                pending = text
                create.launch(filename)
            },
            open = { pick.launch(arrayOf(JSON, "text/*", "application/octet-stream")) },
        )
    }
}

private suspend fun Context.write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
    try {
        contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
    } catch (_: IOException) {
        false
    }
}

private suspend fun Context.read(uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
    } catch (_: IOException) {
        null
    }
}
