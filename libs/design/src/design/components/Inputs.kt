package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.jsRoundToInt
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type

/** A label over a control, with an optional quieter line saying what to put there. */
@Composable
fun Field(label: String, modifier: Modifier = Modifier, hint: String? = null, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        BjjText(label, style = Type.label)
        content()
        if (hint != null) BjjText(hint, style = Type.small, color = BjjTheme.palette.muted)
    }
}

/** Where text is typed: paper a shade darker than the card, with the ink line round it. */
@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    lines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = if (lines > 1) ImeAction.Default else ImeAction.Next,
    onSurface: Boolean = false,
) {
    val p = BjjTheme.palette
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                block(if (onSurface) p.surface else p.bg, p.borderControl, Radius.md)
                if (focused) focusRing(p.accent, Radius.md)
            }
            .defaultMinSize(minHeight = Sizes.tap)
            .padding(horizontal = Spacing.s3, vertical = Spacing.s3)
            .semantics { contentDescription = label },
        textStyle = Type.body.copy(color = p.text),
        singleLine = lines == 1,
        minLines = lines,
        maxLines = if (lines == 1) 1 else Int.MAX_VALUE,
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType ==
                KeyboardType.Text
            ) {
                KeyboardCapitalization.Sentences
            } else {
                KeyboardCapitalization.None
            },
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        interactionSource = interactions,
        cursorBrush = SolidColor(p.accent),
        decorationBox = { field ->
            Box(contentAlignment = if (lines == 1) Alignment.CenterStart else Alignment.TopStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) BjjText(placeholder, color = p.muted)
                field()
            }
        },
    )
}

/** A labelled text field. */
@Composable
fun TextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    lines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Field(label, modifier, hint) { TextInput(value, onValueChange, label, lines = lines, keyboardType = keyboardType) }
}

/**
 * A slider over whole numbers from [min] to [max]: a track with a round ink-lined handle, dragged or tapped.
 * A screen reader moves it a step at a time.
 */
@Composable
fun NumberSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val p = BjjTheme.palette
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    var width by remember { mutableFloatStateOf(0f) }
    var dragAt by remember { mutableFloatStateOf(0f) }
    val span = (max - min).coerceAtLeast(1)

    fun valueAt(x: Float): Int = if (width <=
        0f
    ) {
        current
    } else {
        (min + jsRoundToInt((x / width * span).toDouble())).coerceIn(min, max)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(Sizes.tap)
            .onSizeChanged { width = it.width.toFloat() }
            .pointerInput(min, max) { detectTapGestures { change(valueAt(it.x)) } }
            .draggable(
                state = rememberDraggableState { delta ->
                    dragAt += delta
                    change(valueAt(dragAt))
                },
                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                onDragStarted = { dragAt = it.x },
            )
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), min.toFloat()..max.toFloat(), span - 1)
                setProgress { target ->
                    change(jsRoundToInt(target.toDouble()).coerceIn(min, max))
                    true
                }
            }
            .drawBehind {
                val track = SLIDER_TRACK.toPx()
                val knob = SLIDER_KNOB.toPx()
                val line = Sizes.line.toPx()
                val y = size.height / 2
                val x = knob / 2 + (size.width - knob) * (current - min) / span
                val round = androidx.compose.ui.geometry.CornerRadius(track)
                drawRoundRect(p.well, Offset(0f, y - track / 2), Size(size.width, track), round)
                drawRoundRect(p.fill, Offset(0f, y - track / 2), Size(x, track), round)
                drawRoundRect(
                    p.outline,
                    Offset(0f, y - track / 2),
                    Size(size.width, track),
                    round,
                    style = Stroke(line),
                )
                drawCircle(p.raised, knob / 2, Offset(x, y))
                drawCircle(p.outline, knob / 2 - line / 2, Offset(x, y), style = Stroke(line))
            },
    )
}

private val SLIDER_TRACK = 10.dp
private val SLIDER_KNOB = 28.dp
