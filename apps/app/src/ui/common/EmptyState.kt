package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.graphics.Enso
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type

/** In place of a list with nothing in it: a brush circle round an icon, what is missing, and what to do about it. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Enso(Modifier.size(ENSO_SIZE))
            Icon(icon, description = null, tint = BjjTheme.palette.muted, size = ENSO_ICON)
        }
        BjjText(title, style = Type.h2, textAlign = TextAlign.Center)
        BjjText(text, color = BjjTheme.palette.muted, textAlign = TextAlign.Center)
        action()
    }
}

private val ENSO_SIZE = 96.dp
private val ENSO_ICON = 36.dp
