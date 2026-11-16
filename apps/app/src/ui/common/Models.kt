package dev.justinsmith.bjj.ui.common

import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.SEARCH_RESULT_LIMIT
import dev.justinsmith.bjj.core.SearchHit
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.cardName
import dev.justinsmith.bjj.core.endRole
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.searchLibrary
import dev.justinsmith.bjj.core.techniqueLinks
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.design.graphics.hasPositionArtwork

// What a ViewModel hands a screen in place of the library: each thing to draw with its lookups already done.
// A ViewModel builds these from a snapshot with the functions here; a screen only draws them.

/** Something with a name that a tap opens. */
data class Named(val id: String, val name: String)

/** A study card as a chip: what it is, its name and how well it is known. */
data class CardLink(val item: StudyItem, val name: String, val progress: Progress)

fun cardLink(lib: Library, item: StudyItem): CardLink =
    CardLink(item, cardName(lib, item).orEmpty(), progressOf(reviewOf(lib, item.kind, item.id)))

/** Which end of a technique a row names: where it leads, or where it comes from. */
enum class Direction { Out, In }

/** The end of a technique that its row names. */
sealed interface OtherEnd {
    /** A position the library no longer has. */
    data object Unknown : OtherEnd

    data class From(val position: String) : OtherEnd

    /** It ends the roll. */
    data object Tap : OtherEnd

    /** Where it leads, and on which side of it when that is known. */
    data class To(val position: String, val category: PositionCategory, val side: PositionRole?) : OtherEnd
}

/** A technique as a row in a list: its name, its type and its other end. */
data class TechniqueRow(val id: String, val name: String, val type: TransitionType, val otherEnd: OtherEnd)

private fun otherEnd(lib: Library, technique: Transition, direction: Direction): OtherEnd {
    if (direction == Direction.In) {
        return lib.positionById[technique.fromId]?.let { OtherEnd.From(it.name) } ?: OtherEnd.Unknown
    }
    if (endsTheRoll(technique.type)) return OtherEnd.Tap
    val to = lib.positionById[technique.toId] ?: return OtherEnd.Unknown
    return OtherEnd.To(to.name, to.category, endRole(lib, technique))
}

fun techniqueRows(lib: Library, techniques: List<Transition>, direction: Direction): List<TechniqueRow> =
    techniques.map { TechniqueRow(it.id, it.name, it.type, otherEnd(lib, it, direction)) }

/** A technique to switch to, and what the other person does that makes it the one. */
data class FollowUpRow(val id: String, val name: String, val whenText: String)

/** How a technique meets others in a roll. Empty for every technique the user added. */
data class TechniqueLinksUi(
    val followUps: List<FollowUpRow> = emptyList(),
    val defences: List<TechniqueRow> = emptyList(),
    val answers: List<TechniqueRow> = emptyList(),
) {
    val isEmpty: Boolean get() = followUps.isEmpty() && defences.isEmpty() && answers.isEmpty()
}

fun techniqueLinksUi(lib: Library, id: String): TechniqueLinksUi {
    val links = techniqueLinks(lib, id)
    return TechniqueLinksUi(
        followUps = links.followUps.map { FollowUpRow(it.technique.id, it.technique.name, it.whenText) },
        defences = techniqueRows(lib, links.defences, Direction.In),
        answers = techniqueRows(lib, links.answers, Direction.In),
    )
}

/**
 * A technique drawn as its two ends: the position it starts from and the one it reaches. [toId] and [toName]
 * are null for one that ends the roll, whose second picture is a tap.
 */
data class TechniquePicture(
    val type: TransitionType,
    val fromId: String,
    val fromName: String,
    val toId: String?,
    val toName: String?,
)

/** The picture of a technique, or null when either end has no drawing, such as a position the user added. */
fun techniquePicture(lib: Library, technique: Transition): TechniquePicture? {
    val from = lib.positionById[technique.fromId]?.takeIf { hasPositionArtwork(it.id) } ?: return null
    if (endsTheRoll(technique.type)) return TechniquePicture(technique.type, from.id, from.name, null, null)
    val to = lib.positionById[technique.toId]?.takeIf { hasPositionArtwork(it.id) } ?: return null
    return TechniquePicture(technique.type, from.id, from.name, to.id, to.name)
}

/** The search box's results for the [query] they were worked out for. */
data class SearchUi(val query: String = "", val hits: List<SearchHit> = emptyList())

fun searchUi(lib: Library, query: String): SearchUi = SearchUi(query, searchLibrary(lib, query, SEARCH_RESULT_LIMIT))
