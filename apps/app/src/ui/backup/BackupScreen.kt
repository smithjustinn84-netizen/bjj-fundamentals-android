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
import dev.justinsmith.bjj.core.BackupProblem
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
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.backup_about
import dev.justinsmith.bjj.resources.backup_erase_button
import dev.justinsmith.bjj.resources.backup_erase_confirm
import dev.justinsmith.bjj.resources.backup_erase_text
import dev.justinsmith.bjj.resources.backup_erase_title
import dev.justinsmith.bjj.resources.backup_erased
import dev.justinsmith.bjj.resources.backup_export
import dev.justinsmith.bjj.resources.backup_export_about
import dev.justinsmith.bjj.resources.backup_export_button
import dev.justinsmith.bjj.resources.backup_import
import dev.justinsmith.bjj.resources.backup_import_about
import dev.justinsmith.bjj.resources.backup_import_button
import dev.justinsmith.bjj.resources.backup_problem_bad_field
import dev.justinsmith.bjj.resources.backup_problem_newer
import dev.justinsmith.bjj.resources.backup_problem_no_data
import dev.justinsmith.bjj.resources.backup_problem_no_schema_version
import dev.justinsmith.bjj.resources.backup_problem_not_a_list
import dev.justinsmith.bjj.resources.backup_problem_not_a_row
import dev.justinsmith.bjj.resources.backup_problem_not_an_object
import dev.justinsmith.bjj.resources.backup_problem_not_json
import dev.justinsmith.bjj.resources.backup_problem_not_ours
import dev.justinsmith.bjj.resources.backup_progress_reset
import dev.justinsmith.bjj.resources.backup_rejected
import dev.justinsmith.bjj.resources.backup_replace_confirm
import dev.justinsmith.bjj.resources.backup_replace_text
import dev.justinsmith.bjj.resources.backup_replace_title
import dev.justinsmith.bjj.resources.backup_reset_button
import dev.justinsmith.bjj.resources.backup_reset_confirm
import dev.justinsmith.bjj.resources.backup_reset_text
import dev.justinsmith.bjj.resources.backup_reset_title
import dev.justinsmith.bjj.resources.backup_restored
import dev.justinsmith.bjj.resources.backup_saved
import dev.justinsmith.bjj.resources.backup_start_over
import dev.justinsmith.bjj.resources.backup_stored
import dev.justinsmith.bjj.resources.backup_title
import dev.justinsmith.bjj.resources.backup_unknown_date
import dev.justinsmith.bjj.resources.common_cancel
import dev.justinsmith.bjj.resources.tab_more
import dev.justinsmith.bjj.ui.common.BackLink
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupRoute(nav: Nav) {
    val backup = koinViewModel<BackupViewModel>()
    val state by backup.uiState.collectAsStateWithLifecycle()
    val files = rememberBackupFiles(onSave = backup::exported, onOpen = backup::picked)
    BackupScreen(
        state = state,
        onBack = nav.back,
        onExport = { backup.export(files.save) },
        onImport = files.open,
        onConfirmImport = backup::confirmImport,
        onCancelImport = backup::cancelImport,
        onResetProgress = backup::resetProgress,
        onEraseAll = backup::eraseAll,
    )
}

@Composable
private fun messageText(message: BackupMessage): String = when (message) {
    BackupMessage.Saved -> stringResource(Res.string.backup_saved)
    BackupMessage.Restored -> stringResource(Res.string.backup_restored)
    BackupMessage.ProgressReset -> stringResource(Res.string.backup_progress_reset)
    BackupMessage.Erased -> stringResource(Res.string.backup_erased)
    is BackupMessage.Rejected -> stringResource(Res.string.backup_rejected, problemText(message.problem))
}

/** What is wrong with a file offered as a backup, in words. Tables and fields go by their names in the file. */
@Composable
private fun problemText(problem: BackupProblem): String = when (problem) {
    BackupProblem.NotJson -> stringResource(Res.string.backup_problem_not_json)

    BackupProblem.NotAnObject -> stringResource(Res.string.backup_problem_not_an_object)

    BackupProblem.NotOurs -> stringResource(Res.string.backup_problem_not_ours)

    BackupProblem.NoSchemaVersion -> stringResource(Res.string.backup_problem_no_schema_version)

    is BackupProblem.Newer -> stringResource(Res.string.backup_problem_newer, problem.schemaVersion)

    BackupProblem.NoData -> stringResource(Res.string.backup_problem_no_data)

    is BackupProblem.NotAList -> stringResource(Res.string.backup_problem_not_a_list, problem.table.key)

    is BackupProblem.NotARow ->
        stringResource(Res.string.backup_problem_not_a_row, problem.table.key, problem.entry)

    is BackupProblem.BadField ->
        stringResource(Res.string.backup_problem_bad_field, problem.table.key, problem.entry, problem.field)
}

/** What a backup holds, table by table, for the question before it replaces everything. */
private fun summary(backup: BackupFile): String =
    tableCounts(backup.data).entries.joinToString("\n") { (table, count) -> "${table.label}: $count" }

@Composable
fun BackupScreen(
    state: BackupUiState,
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
        BackLink(stringResource(Res.string.tab_more), onBack)
        PageHead(
            stringResource(Res.string.backup_title),
            about = stringResource(Res.string.backup_about),
        )
        state.message?.let {
            Notice(messageText(it), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, isError = state.isError)
        }
        Card {
            BjjText(stringResource(Res.string.backup_export), style = Type.h2)
            BjjText(stringResource(Res.string.backup_export_about))
            Button(stringResource(Res.string.backup_export_button), onExport, kind = ButtonKind.Primary)
        }
        Card {
            BjjText(stringResource(Res.string.backup_import), style = Type.h2)
            BjjText(stringResource(Res.string.backup_import_about))
            Button(stringResource(Res.string.backup_import_button), onImport)
        }
        Card {
            BjjText(stringResource(Res.string.backup_stored), style = Type.h2)
            BackupTable.entries.forEachIndexed { index, table ->
                if (index > 0) Divider()
                Row(Modifier.fillMaxWidth()) {
                    BjjText(table.label, Modifier.weight(1f))
                    BjjText("${state.counts[table] ?: 0}", style = Type.strong)
                }
            }
        }
        Card {
            BjjText(stringResource(Res.string.backup_start_over), style = Type.h2)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Button(stringResource(Res.string.backup_reset_button), { confirmingReset = true }, Modifier.weight(1f))
                Button(
                    stringResource(Res.string.backup_erase_button),
                    { confirmingErase = true },
                    Modifier.weight(1f),
                    ButtonKind.Danger,
                )
            }
        }
    }
    ImportDialog(state.pending, state.pendingDay, onConfirmImport, onCancelImport)
    if (confirmingReset) {
        StartOverDialog(
            title = stringResource(Res.string.backup_reset_title),
            text = stringResource(Res.string.backup_reset_text),
            confirm = stringResource(Res.string.backup_reset_confirm),
            onConfirm = onResetProgress,
            onClose = { confirmingReset = false },
        )
    }
    if (confirmingErase) {
        StartOverDialog(
            title = stringResource(Res.string.backup_erase_title),
            text = stringResource(Res.string.backup_erase_text),
            confirm = stringResource(Res.string.backup_erase_confirm),
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
        title = stringResource(Res.string.backup_replace_title),
        text = stringResource(
            Res.string.backup_replace_text,
            pendingDay ?: stringResource(Res.string.backup_unknown_date),
            summary(pending),
        ),
        confirm = stringResource(Res.string.backup_replace_confirm),
        cancel = stringResource(Res.string.common_cancel),
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
        cancel = stringResource(Res.string.common_cancel),
        onConfirm = {
            onClose()
            onConfirm()
        },
        onDismiss = onClose,
    )
}
