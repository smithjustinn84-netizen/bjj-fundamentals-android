package dev.justinsmith.bjj.ui.map

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.MapLayout
import dev.justinsmith.bjj.core.MapOrientation
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.layoutPositionMap
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.TechniqueRow
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.techniqueRows

/** A position as the map shows it: its box, and what is listed beside the map once it is selected. */
data class MapPosition(
    val id: String,
    val name: String,
    val category: PositionCategory,
    val summary: String,
    val progress: Progress,
    /** The techniques that start here, and the ones from elsewhere that end here. */
    val outgoing: List<TechniqueRow>,
    val incoming: List<TechniqueRow>,
)

/** The map of positions: where everything sits, each way the window can take it, and each position's details. */
data class MapUi(
    /** The layout running across the page and the one running down it; the window picks. */
    val layouts: Map<MapOrientation, MapLayout>,
    val positions: Map<String, MapPosition>,
)

fun mapUi(lib: Library): MapUi = MapUi(
    layouts = MapOrientation.entries.associateWith { layoutPositionMap(lib.positions, lib.transitions, it) },
    positions = lib.positions.associate { position ->
        val (outgoing, incoming) = connections(lib, position.id)
        position.id to MapPosition(
            id = position.id,
            name = position.name,
            category = position.category,
            summary = position.summary,
            progress = progressOf(reviewOf(lib, CardKind.Position, position.id)),
            outgoing = techniqueRows(lib, outgoing, Direction.Out),
            incoming = techniqueRows(lib, incoming, Direction.In),
        )
    },
)
