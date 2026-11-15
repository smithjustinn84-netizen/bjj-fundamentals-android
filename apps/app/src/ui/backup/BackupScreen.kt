package dev.justinsmith.bjj.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.BackupFile
import dev.justinsmith.bjj.core.BackupTable
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.services.tableCounts
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.ConfirmDialog
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Notice
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.ui.common.BackLink
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.home.formatDay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupRoute(nav: Nav) {
    val backup = koinViewModel<BackupViewModel>()
    val state by backup.state.collectAsStateWithLifecycle()
    val files = rememberBackupFiles(onSave = backup::exported, onOpen = backup::picked)
    WithSnapshot { snapshot, app ->
        BackupScreen(
            state = state,
            counts = snapshot.tableCounts,
            // The day a waiting backup was made, in words.
            pendingDay = state.pending?.takeIf { it.exportedAt > 0 }?.let {
                formatDay(dev.justinsmith.bjj.core.localDayKey(it.exportedAt, app.zone))
            },
            onBack = nav.back,
            onExport = { backup.export(files.save) },
            onImport = files.open,
            onConfirmImport = backup::confirmImport,
            onCancelImport = backup::cancelImport,
            onResetProgress = backup::resetProgress,
            onEraseAll = backup::eraseAll,
        )
    }
}

/** What a backup holds, table by table, for the question before it replaces everything. */
private fun summary(backup: BackupFile): String =
    tableCounts(backup.data).entries.joinToString("\n") { (table, count) -> "${table.label}: $count" }

@Composable
fun BackupScreen(
    state: BackupUiState,
    counts: Map<BackupTable, Int>,
    pendingDay: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onResetProgress: () -> Unit,
    onEraseAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingReset by remember { mutableStateOf(false) }
    var confirmingErase by remember { mutableStateOf(false) }
    Page(modifier, maxWidth = Sizes.readingMax) {
        BackLink("More", onBack)
        PageHead(
            "Backup",
            about =
            "Your data lives only in this app, on this device. Clearing the app's storage or removing the app " +
                "loses it, so export a backup now and then.",
        )
        if (state.message.isNotEmpty()) {
            Notice(state.message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, isError = state.isError)
        }
        Card {
            BjjText("Export", style = Type.h2)
            BjjText(
                "Save everything (concepts, positions, techniques, video bookmarks, your training log and review " +
                    "progress) as one file. The web version of the app reads the same file.",
            )
            Button("Export backup", onExport, kind = ButtonKind.Primary)
        }
        Card {
            BjjText("Import", style = Type.h2)
            BjjText("Restore from a backup file. This replaces everything currently stored.")
            Button("Import backup…", onImport)
        }
        Card {
            BjjText("What's stored", style = Type.h2)
            BackupTable.entries.forEachIndexed { index, table ->
                if (index > 0) Divider()
                Row(Modifier.fillMaxWidth()) {
                    BjjText(table.label, Modifier.weight(1f))
                    BjjText("${counts[table] ?: 0}", style = Type.strong)
                }
            }
        }
        Card {
            BjjText("Start over", style = Type.h2)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Button("Reset review progress", { confirmingReset = true }, Modifier.weight(1f))
                Button("Erase everything", { confirmingErase = true }, Modifier.weight(1f), ButtonKind.Danger)
            }
        }
    }
    ImportDialog(state.pending, pendingDay, onConfirmImport, onCancelImport)
    if (confirmingReset) {
        StartOverDialog(
            title = "Reset review progress?",
            text = "Reset all review progress? Your positions, techniques, concepts and videos are kept.",
            confirm = "Reset",
            onConfirm = onResetProgress,
            onClose = { confirmingReset = false },
        )
    }
    if (confirmingErase) {
        StartOverDialog(
            title = "Erase everything?",
            text = "Erase EVERYTHING and start over with the starter content? Your notes, videos, edits and progress " +
                "will be deleted. This cannot be undone.",
            confirm = "Erase",
            onConfirm = onEraseAll,
            onClose = { confirmingErase = false },
        )
    }
}

/** Asks before a waiting backup replaces everything, saying what it holds. */
@Composable
private fun ImportDialog(pending: BackupFile?, pendingDay: String?, onConfirm: () -> Unit, onCancel: () -> Unit) {
    if (pending == null) return
    ConfirmDialog(
        title = "Replace everything?",
        text = "Replace ALL current data with this backup from ${pendingDay ?: "an unknown date"}?\n\n" +
            "${summary(pending)}\n\nThis cannot be undone. Export first if you want to keep what you have now.",
        confirm = "Replace",
        cancel = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onCancel,
    )
}

@Composable
private fun StartOverDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onClose: () -> Unit) {
    ConfirmDialog(
        title = title,
        text = text,
        confirm = confirm,
        cancel = "Cancel",
        onConfirm = {
            onClose()
            onConfirm()
        },
        onDismiss = onClose,
    )
}
