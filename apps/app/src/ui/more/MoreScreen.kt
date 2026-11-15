package dev.justinsmith.bjj.ui.more

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.plural
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
import dev.justinsmith.bjj.nav.Videos
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.RowCard
import dev.justinsmith.bjj.ui.common.WithSnapshot

@Composable
fun MoreRoute(nav: Nav) {
    WithSnapshot { snapshot, viewModel ->
        MoreScreen(snapshot.videos.size, snapshot.themeChoice, nav, viewModel::setTheme)
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
        PageTitle("More")
        GroupTitle("Your library")
        RowCard {
            MoreRow(
                BjjIcons.Video,
                "Videos",
                { nav.open(Videos) },
                if (videoCount > 0) {
                    "${plural(videoCount, "bookmark")} at the moment a detail is explained"
                } else {
                    "Bookmark the moment a detail is explained"
                },
            )
            Divider()
            MoreRow(
                BjjIcons.Belt,
                "Blue belt test",
                { nav.selectTab(LearnTab(LearnSection.BlueBelt)) },
                "The techniques you will be asked to show",
            )
        }
        GroupTitle("Add your own")
        RowCard {
            ItemKind.entries.forEachIndexed { index, kind ->
                if (index > 0) Divider()
                MoreRow(BjjIcons.Plus, "New ${kind.label.lowercase()}", { nav.open(Editor(kind)) })
            }
        }
        GroupTitle("Appearance")
        Segmented(ThemeChoice.entries, theme, onTheme, { it.label }, Modifier.fillMaxWidth())
        GroupTitle("Your data")
        RowCard {
            MoreRow(BjjIcons.Backup, "Backup and restore", {
                nav.open(Backup)
            }, "Export everything as a file, or bring one back")
        }
        BjjText(
            "Everything is stored on this device only. Export a backup now and then.",
            style = Type.small,
            color = BjjTheme.palette.muted,
        )
    }
}
