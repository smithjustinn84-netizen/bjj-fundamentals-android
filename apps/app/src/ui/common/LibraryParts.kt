package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.SEARCH_RESULT_LIMIT
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.endRole
import dev.justinsmith.bjj.core.endSideLabel
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.searchLibrary
import dev.justinsmith.bjj.core.techniqueLinks
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.RowLink
import dev.justinsmith.bjj.design.components.Section
import dev.justinsmith.bjj.design.components.Segmented
import dev.justinsmith.bjj.design.components.Tag
import dev.justinsmith.bjj.design.components.TextInput
import dev.justinsmith.bjj.design.components.woodblock
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.typeColor
import dev.justinsmith.bjj.nav.LearnSection

private val LearnSection.label: String
    get() = when (this) {
        LearnSection.Concepts -> "Concepts"
        LearnSection.Positions -> "Positions"
        LearnSection.Lessons -> "Lessons"
        LearnSection.BlueBelt -> "Blue belt"
    }

/** The switch at the top of every page behind the Learn tab. Techniques are reached from positions. */
@Composable
fun LearnSwitch(section: LearnSection, onSelect: (LearnSection) -> Unit, modifier: Modifier = Modifier) {
    Segmented(LearnSection.entries, section, onSelect, { it.label }, modifier.fillMaxWidth())
}

/** One box that finds any concept, position or technique by its name or its text. */
@Composable
fun LibrarySearch(lib: Library, onOpen: (ItemKind, String) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val hits = searchLibrary(lib, query, SEARCH_RESULT_LIMIT)
    val searching = query.isNotBlank()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        TextInput(
            value = query,
            onValueChange = { query = it },
            label = "Search your library",
            modifier = Modifier.woodblock(
                fill = BjjTheme.palette.surface,
                outline = BjjTheme.palette.borderControl,
                radius = dev.justinsmith.bjj.design.theme.Radius.md,
            ),
            placeholder = "Find a concept, position or technique…",
            imeAction = ImeAction.Search,
            onSurface = true,
        )
        if (searching && hits.isEmpty()) {
            // Said aloud as it changes, so a screen reader hears that nothing matched.
            BjjText(
                "Nothing matches “${query.trim()}”.",
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = BjjTheme.palette.muted,
            )
        }
        if (hits.isNotEmpty()) {
            RowCard {
                hits.forEachIndexed { index, hit ->
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

/** A card holding rows that run to its edges, with less padding than a card of prose. */
@Composable
fun RowCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().woodblock().padding(horizontal = Spacing.s4, vertical = Spacing.s1)) { content() }
}

/**
 * Offers starter items the library does not have, such as ones added to the app after it was set up.
 * Dismissing hides those items for good; one that goes missing later is offered again.
 */
@Composable
fun StarterOffer(
    noun: String,
    names: List<String>,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (names.isEmpty()) return
    val one = names.size == 1
    Card(modifier) {
        BjjText(
            "${names.size} starter ${if (one) "$noun is" else "${noun}s are"} not in your library:",
            style = Type.strong,
        )
        BjjText("${names.joinToString(", ")}.")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button(if (one) "Add it" else "Add them", onAdd, kind = ButtonKind.Primary)
            Button("Dismiss", onDismiss)
        }
    }
}

/** Which end of a technique a row names: where it leads, or where it comes from. */
enum class Direction { Out, In }

private fun otherEnd(technique: Transition, positionById: Map<String, Position>, direction: Direction): String {
    if (direction == Direction.In) return positionById[technique.fromId]?.let { "from ${it.name}" }.orEmpty()
    if (endsTheRoll(technique.type)) return "→ tap"
    val to = positionById[technique.toId] ?: return ""
    val side = endRole(positionById, technique)
    return if (side != null) "→ ${to.name}, ${endSideLabel(to.category, side)}" else "→ ${to.name}"
}

/** Techniques as a list of links, each with its type and other end. */
@Composable
fun TransitionList(
    transitions: List<Transition>,
    positionById: Map<String, Position>,
    direction: Direction,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        transitions.forEachIndexed { index, technique ->
            if (index > 0) Divider()
            RowLink(technique.name, { onOpen(technique.id) }, BjjIcons.Chevron) {
                Tag(technique.type.label, BjjTheme.palette.typeColor(technique.type))
                BjjText(
                    otherEnd(technique, positionById, direction),
                    style = Type.small,
                    color = BjjTheme.palette.muted,
                )
            }
        }
    }
}

/**
 * How a technique meets others in a roll: what to switch to when it is stopped, what defends it and what it
 * defends. Draws nothing for a technique with no links, which is every technique the user added.
 */
@Composable
fun TechniqueLinks(lib: Library, id: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val links = techniqueLinks(lib, id)
    if (links.isEmpty) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        if (links.followUps.isNotEmpty()) {
            Section("If they stop it, switch to") {
                links.followUps.forEachIndexed { index, followUp ->
                    if (index > 0) Divider()
                    RowLink(followUp.technique.name, { onOpen(followUp.technique.id) }, BjjIcons.Chevron) {
                        BjjText(followUp.whenText, style = Type.small, color = BjjTheme.palette.muted)
                    }
                }
            }
        }
        if (links.defences.isNotEmpty()) {
            Section("How it gets defended") { TransitionList(links.defences, lib.positionById, Direction.In, onOpen) }
        }
        if (links.answers.isNotEmpty()) {
            Section("What it defends") { TransitionList(links.answers, lib.positionById, Direction.In, onOpen) }
        }
    }
}

const val NO_TECHNIQUES_FROM_HERE = "No techniques from here yet."
