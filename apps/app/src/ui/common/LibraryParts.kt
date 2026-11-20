package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.endSideLabel
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Bullets
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.RowLink
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.Tag
import dev.justinsmith.bjj.design.components.TextInput
import dev.justinsmith.bjj.design.components.pressShrink
import dev.justinsmith.bjj.design.components.woodblock
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.typeColor
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.learn_from_position
import dev.justinsmith.bjj.resources.learn_leads_to
import dev.justinsmith.bjj.resources.learn_leads_to_side
import dev.justinsmith.bjj.resources.learn_leads_to_tap
import dev.justinsmith.bjj.resources.learn_links_answers
import dev.justinsmith.bjj.resources.learn_links_defences
import dev.justinsmith.bjj.resources.learn_links_follow_ups
import dev.justinsmith.bjj.resources.learn_nothing_here
import dev.justinsmith.bjj.resources.learn_search_clear
import dev.justinsmith.bjj.resources.learn_search_label
import dev.justinsmith.bjj.resources.learn_search_no_match
import dev.justinsmith.bjj.resources.learn_search_placeholder
import org.jetbrains.compose.resources.stringResource

/**
 * One box that finds any concept, position or technique by its name or its text. What is typed stays here and
 * is sent to [onSearch] as it changes; [search] is what the screen's ViewModel found for it.
 */
@Composable
fun LibrarySearch(
    search: SearchUi,
    onSearch: (String) -> Unit,
    onOpen: (ItemKind, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // What was typed comes back with the page after the app was put away; the results have to be asked for again.
    val currentOnSearch by rememberUpdatedState(onSearch)
    LaunchedEffect(Unit) { if (query != search.query) currentOnSearch(query) }
    val hits = search.hits
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        val clearLabel = stringResource(Res.string.learn_search_clear)
        TextInput(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            label = stringResource(Res.string.learn_search_label),
            placeholder = stringResource(Res.string.learn_search_placeholder),
            imeAction = ImeAction.Search,
            onSurface = true,
            leading = { Icon(BjjIcons.Search, description = null, tint = BjjTheme.palette.muted, size = Sizes.icon) },
            trailing = if (query.isNotEmpty()) {
                {
                    val interactions = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .pressShrink(interactions)
                            .clickable(interactions, indication = null, role = Role.Button) {
                                query = ""
                                onSearch("")
                            }
                            .size(Sizes.tap)
                            .semantics { contentDescription = clearLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(BjjIcons.Close, description = null, tint = BjjTheme.palette.muted, size = Sizes.iconSmall)
                    }
                }
            } else {
                null
            },
        )
        if (search.query.isNotBlank() && hits.isEmpty()) {
            // Said aloud as it changes, so a screen reader hears that nothing matched.
            BjjText(
                stringResource(Res.string.learn_search_no_match, search.query.trim()),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = BjjTheme.palette.muted,
            )
        }
        if (hits.isNotEmpty()) {
            RowCard {
                // Keyed, so a hit that stays as the search narrows keeps its row rather than taking a neighbour's.
                hits.forEachIndexed { index, hit ->
                    key(hit.kind, hit.id) {
                        if (index > 0) Divider()
                        RowLink(hit.name, { onOpen(hit.kind, hit.id) }, BjjIcons.Chevron) {
                            Tag(hit.kind.label, BjjTheme.palette.typeTransition)
                            BjjText(hit.detail, style = Type.small, color = BjjTheme.palette.muted, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

/** A card holding rows that run to its edges, with less padding than a card of prose. */
@Composable
fun RowCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().woodblock().padding(horizontal = Spacing.s4, vertical = Spacing.s1)) { content() }
}

/** The other end of a technique, in words. */
@Composable
private fun OtherEnd.text(): String = when (this) {
    OtherEnd.Unknown -> ""

    is OtherEnd.From -> stringResource(Res.string.learn_from_position, position)

    OtherEnd.Tap -> stringResource(Res.string.learn_leads_to_tap)

    is OtherEnd.To -> if (side != null) {
        stringResource(Res.string.learn_leads_to_side, position, endSideLabel(category, side))
    } else {
        stringResource(Res.string.learn_leads_to, position)
    }
}

/** Techniques as a list of links, each with its type and other end. */
@Composable
fun TransitionList(rows: List<TechniqueRow>, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, technique ->
            key(technique.id) {
                if (index > 0) Divider()
                RowLink(technique.name, { onOpen(technique.id) }, BjjIcons.Chevron) {
                    Tag(technique.type.label, BjjTheme.palette.typeColor(technique.type))
                    BjjText(technique.otherEnd.text(), style = Type.small, color = BjjTheme.palette.muted)
                }
            }
        }
    }
}

/**
 * How a technique meets others in a roll: what to switch to when it is stopped, what defends it and what it
 * defends. Draws nothing for a technique with no links, which is every technique the user added.
 */
@Composable
fun TechniqueLinks(links: TechniqueLinksUi, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    if (links.isEmpty) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        if (links.followUps.isNotEmpty()) {
            Section(stringResource(Res.string.learn_links_follow_ups)) {
                links.followUps.forEachIndexed { index, followUp ->
                    key(followUp.id) {
                        if (index > 0) Divider()
                        RowLink(followUp.name, { onOpen(followUp.id) }, BjjIcons.Chevron) {
                            BjjText(followUp.whenText, style = Type.small, color = BjjTheme.palette.muted)
                        }
                    }
                }
            }
        }
        if (links.defences.isNotEmpty()) {
            Section(stringResource(Res.string.learn_links_defences)) { TransitionList(links.defences, onOpen) }
        }
        if (links.answers.isNotEmpty()) {
            Section(stringResource(Res.string.learn_links_answers)) { TransitionList(links.answers, onOpen) }
        }
    }
}

/** A list of points, or a line saying there are none yet. */
@Composable
fun PointList(items: List<String>, modifier: Modifier = Modifier, numbered: Boolean = false) {
    if (items.isEmpty()) {
        BjjText(stringResource(Res.string.learn_nothing_here), modifier, color = BjjTheme.palette.muted)
    } else {
        Bullets(items, modifier, numbered)
    }
}
