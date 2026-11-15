package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.LocalContentColor
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type

/** A card over the page, for a question that must be answered before going on. */
@Composable
private fun DialogCard(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        // A dialog is a window of its own: the page's content colour does not reach it.
        CompositionLocalProvider(LocalContentColor provides BjjTheme.palette.text) {
            Column(
                Modifier
                    .widthIn(max = DIALOG_WIDTH)
                    .fillMaxWidth()
                    .woodblock(edge = Sizes.edgeLift)
                    .padding(Spacing.cardPad),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                BjjText(title, Modifier.semantics { heading() }, style = Type.h2)
                content()
            }
        }
    }
}

private val DIALOG_WIDTH = 420.dp

/** Asks before doing something that cannot be undone. [confirm] names the action; [danger] paints it as one. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    cancel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    DialogCard(title, onDismiss) {
        BjjText(text)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button(cancel, onDismiss, Modifier.weight(1f))
            Button(
                confirm,
                onConfirm,
                Modifier.weight(1f),
                kind = if (danger) ButtonKind.Danger else ButtonKind.Primary,
            )
        }
    }
}

/** A list to pick one thing from, where there are too many for a switch. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    DialogCard(title, onDismiss) {
        Column(Modifier.heightIn(max = CHOICES_HEIGHT).verticalScroll(rememberScrollState())) {
            for (option in options) {
                val interactions = remember { MutableInteractionSource() }
                val picked = option == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressShrink(interactions)
                        .clickable(interactions, indication = null, role = Role.RadioButton, onClick = {
                            onSelect(option)
                        })
                        .defaultMinSize(minHeight = Sizes.tap)
                        .padding(vertical = Spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BjjText(
                        label(option),
                        style = Type.body.copy(fontWeight = if (picked) FontWeight.ExtraBold else FontWeight.Normal),
                        color = if (picked) BjjTheme.palette.accent else BjjTheme.palette.text,
                    )
                }
            }
        }
    }
}

private val CHOICES_HEIGHT = 420.dp

/**
 * A control that shows the one thing picked and opens a list to pick another: what a form uses where there are
 * more choices than a switch can hold.
 */
@Composable
fun <T> ChoiceField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    var open by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    Field(label, modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .pressShrink(interactions)
                .drawBehind { block(p.bg, p.borderControl, Radius.md) }
                .clickable(interactions, indication = null, role = Role.DropdownList, onClick = { open = true })
                .defaultMinSize(minHeight = Sizes.tap)
                .padding(horizontal = Spacing.s3, vertical = Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BjjText(selected?.let { optionLabel(it) } ?: placeholder, Modifier.weight(1f))
            BjjText("▾", color = p.muted)
        }
    }
    if (open) {
        ChoiceDialog(
            title = label,
            options = options,
            selected = selected,
            label = optionLabel,
            onSelect = {
                open = false
                onSelect(it)
            },
            onDismiss = { open = false },
        )
    }
}
