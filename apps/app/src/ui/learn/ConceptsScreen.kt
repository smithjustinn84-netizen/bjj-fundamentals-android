package dev.justinsmith.bjj.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.Icon
import dev.justinsmith.bjj.design.components.ProgressDot
import dev.justinsmith.bjj.design.components.Tile
import dev.justinsmith.bjj.design.graphics.ConceptArtwork
import dev.justinsmith.bjj.design.graphics.hasConceptArtwork
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_concepts
import dev.justinsmith.bjj.resources.learn_concepts_about
import dev.justinsmith.bjj.resources.learn_concepts_empty
import dev.justinsmith.bjj.resources.learn_concepts_new
import dev.justinsmith.bjj.resources.learn_concepts_study
import dev.justinsmith.bjj.resources.learn_starter_concepts_missing
import dev.justinsmith.bjj.ui.common.Columns
import dev.justinsmith.bjj.ui.common.LibrarySearch
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConceptsRoute(nav: Nav) {
    val viewModel = koinViewModel<ConceptsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        ConceptsUiState.Loading -> Loading()

        is ConceptsUiState.Ready -> ConceptsScreen(
            state = ready,
            nav = nav,
            onSearch = viewModel::search,
            onAddStarter = viewModel::addStarter,
            onDismissStarter = viewModel::dismissStarter,
        )
    }
}

/** Every concept in learning order: its picture, its name, what it is, and how well it is known. */
@Composable
fun ConceptsScreen(
    state: ConceptsUiState.Ready,
    nav: Nav,
    onSearch: (String) -> Unit,
    onAddStarter: () -> Unit,
    onDismissStarter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val concepts = state.concepts
    Page(modifier) {
        LearnSwitch(LearnSection.Concepts, { nav.selectTab(LearnTab(it)) })
        PageHead(
            stringResource(Res.string.common_concepts),
            about = stringResource(Res.string.learn_concepts_about),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (concepts.isNotEmpty()) {
                Button(
                    stringResource(Res.string.learn_concepts_study),
                    { nav.selectTab(ReviewTab(Deck.Concept.key)) },
                    Modifier.weight(1f),
                    ButtonKind.Primary,
                )
            }
            Button(
                stringResource(Res.string.learn_concepts_new),
                { nav.open(Editor(ItemKind.Concept)) },
                Modifier.weight(1f),
            )
        }
        LibrarySearch(state.search, onSearch, { kind, id -> nav.open(itemKey(kind, id)) })
        StarterOffer(Res.plurals.learn_starter_concepts_missing, state.missingStarter, onAddStarter, onDismissStarter)
        if (concepts.isEmpty()) {
            BjjText(stringResource(Res.string.learn_concepts_empty), color = BjjTheme.palette.muted)
        } else {
            ProgressLegend()
            Columns(concepts) { concept ->
                Tile(
                    name = concept.name,
                    onClick = { nav.open(ConceptDetail(concept.id)) },
                    detail = concept.summary,
                    picture = if (hasConceptArtwork(concept.id)) ({ ConceptArtwork(concept.id) }) else null,
                ) {
                    ProgressDot(concept.progress)
                    Icon(BjjIcons.Chevron, description = null, tint = BjjTheme.palette.muted)
                }
            }
        }
    }
}
