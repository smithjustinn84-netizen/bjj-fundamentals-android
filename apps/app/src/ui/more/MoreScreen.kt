package dev.justinsmith.bjj.ui.more

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.GroupTitle
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.RowLink
import dev.justinsmith.bjj.design.components.Segmented
import dev.justinsmith.bjj.design.components.block
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.Backup
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.Videos
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_blue_belt_test
import dev.justinsmith.bjj.resources.common_videos
import dev.justinsmith.bjj.resources.more_backup
import dev.justinsmith.bjj.resources.more_backup_detail
import dev.justinsmith.bjj.resources.more_blue_belt_detail
import dev.justinsmith.bjj.resources.more_group_add
import dev.justinsmith.bjj.resources.more_group_appearance
import dev.justinsmith.bjj.resources.more_group_data
import dev.justinsmith.bjj.resources.more_group_library
import dev.justinsmith.bjj.resources.more_new_item
import dev.justinsmith.bjj.resources.more_stored_note
import dev.justinsmith.bjj.resources.more_title
import dev.justinsmith.bjj.resources.more_videos_detail
import dev.justinsmith.bjj.resources.more_videos_empty
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.RowCard
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MoreRoute(nav: Nav) {
    val viewModel = koinViewModel<MoreViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        MoreUiState.Loading -> Loading()
        is MoreUiState.Ready -> MoreScreen(ready.videoCount, ready.theme, nav, viewModel::setTheme)
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, onClick: () -> Unit, detail: String = "") {
    val p = BjjTheme.palette
    RowLink(
        title,
        onClick,
        BjjIcons.Chevron,
        leading = {
            Box(
                Modifier.size(ICON_BOX).drawBehind {
                    block(p.accentSoft, p.accentSoft, Radius.md, 0.dp)
                },
                Alignment.Center,
            ) {
                Icon(icon, description = null, tint = p.accent)
            }
        },
    ) {
        if (detail.isNotEmpty()) BjjText(detail, style = Type.small, color = p.muted)
    }
}

private val ICON_BOX = 40.dp

/** Everything that has no tab of its own: videos, the belt test, adding items, appearance and backups. */
@Composable
fun MoreScreen(
    videoCount: Int,
    theme: ThemeChoice,
    nav: Nav,
    onTheme: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Page(modifier, maxWidth = Sizes.readingMax) {
        PageTitle(stringResource(Res.string.more_title))
        GroupTitle(stringResource(Res.string.more_group_library))
        RowCard {
            MoreRow(
                BjjIcons.Video,
                stringResource(Res.string.common_videos),
                { nav.open(Videos) },
                if (videoCount > 0) {
                    pluralStringResource(Res.plurals.more_videos_detail, videoCount, videoCount)
                } else {
                    stringResource(Res.string.more_videos_empty)
                },
            )
            Divider()
            MoreRow(
                BjjIcons.Belt,
                stringResource(Res.string.common_blue_belt_test),
                { nav.selectTab(LearnTab(LearnSection.BlueBelt)) },
                stringResource(Res.string.more_blue_belt_detail),
            )
        }
        GroupTitle(stringResource(Res.string.more_group_add))
        RowCard {
            ItemKind.entries.forEachIndexed { index, kind ->
                if (index > 0) Divider()
                val title = stringResource(Res.string.more_new_item, kind.label.lowercase())
                MoreRow(BjjIcons.Plus, title, { nav.open(Editor(kind)) })
            }
        }
        GroupTitle(stringResource(Res.string.more_group_appearance))
        Segmented(ThemeChoice.entries, theme, onTheme, { it.label }, Modifier.fillMaxWidth())
        GroupTitle(stringResource(Res.string.more_group_data))
        RowCard {
            MoreRow(
                BjjIcons.Backup,
                stringResource(Res.string.more_backup),
                { nav.open(Backup) },
                stringResource(Res.string.more_backup_detail),
            )
        }
        BjjText(
            stringResource(Res.string.more_stored_note),
            style = Type.small,
            color = BjjTheme.palette.muted,
        )
    }
}
