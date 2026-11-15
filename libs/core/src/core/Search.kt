package dev.justinsmith.bjj.core

data class SearchHit(
    val kind: ItemKind,
    val id: String,
    val name: String,
    /** One line saying what the item is, to tell similar names apart. */
    val detail: String,
)

/** How many results to show: enough to find the item, few enough to stay on one phone screen. */
const val SEARCH_RESULT_LIMIT = 12

/** A hit, and everything about the item that a search can match besides its name. */
private class Candidate(val hit: SearchHit, val body: List<String>)

private fun candidates(lib: Library): List<Candidate> =
    lib.concepts.map { c ->
        Candidate(
            SearchHit(ItemKind.Concept, c.id, c.name, c.summary),
            listOf(c.summary) + c.keyPoints + c.drill + c.notes,
        )
    } + lib.positions.map { p ->
        Candidate(
            SearchHit(ItemKind.Position, p.id, p.name, p.summary),
            listOf(p.summary) + p.topGoals + p.bottomGoals + p.notes,
        )
    } + lib.transitions.map { t ->
        val from = lib.positionById[t.fromId]?.name
        val detail = if (from != null) "${t.type.label} from $from" else t.type.label
        Candidate(
            SearchHit(ItemKind.Transition, t.id, t.name, detail),
            listOf(detail) + t.steps + t.commonMistakes + techniqueDrill(t) + t.notes,
        )
    }

/** Every kind of space that separates words, as the web app's `\s` has it: the regex's own `\s` knows only ASCII's. */
private val SPACES = Regex("[\\s\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]+")

/**
 * Items matching every word of the query, ignoring case. Items whose name holds all the words come first,
 * then those matched through their text; within each group the library's learning order is kept.
 */
fun searchLibrary(lib: Library, query: String, limit: Int): List<SearchHit> {
    val words = query.lowercase().split(SPACES).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    val byName = mutableListOf<SearchHit>()
    val byText = mutableListOf<SearchHit>()
    for (candidate in candidates(lib)) {
        val name = candidate.hit.name.lowercase()
        if (words.all { it in name }) {
            byName += candidate.hit
        } else {
            val text = "$name ${candidate.body.joinToString(" ").lowercase()}"
            if (words.all { it in text }) byText += candidate.hit
        }
    }
    return (byName + byText).take(maxOf(0, limit))
}
