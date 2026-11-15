package dev.justinsmith.bjj.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.services.Snapshot
import org.koin.compose.viewmodel.koinViewModel

/**
 * What every Route of a page that shows the library starts with: the shared ViewModel, and its snapshot once
 * the first read has landed. Until then the page says it is loading.
 */
@Composable
fun WithSnapshot(content: @Composable (Snapshot, AppViewModel) -> Unit) {
    val viewModel = koinViewModel<AppViewModel>()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    when (val loaded = snapshot) {
        null -> Loading()
        else -> content(loaded, viewModel)
    }
}

/** The video actions a page hands its bookmark rows, from the shared ViewModel. */
fun AppViewModel.videoActions() = VideoActions(open = ::open, add = ::addVideo, remove = ::removeVideo)
