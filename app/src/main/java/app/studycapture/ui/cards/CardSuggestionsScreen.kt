package app.studycapture.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.studycapture.R
import app.studycapture.data.db.CardStatus
import app.studycapture.data.db.FlashcardEntity
import app.studycapture.data.repo.StudyRepository
import app.studycapture.ui.appViewModel
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.EmptyState
import app.studycapture.ui.components.EngineNote
import app.studycapture.ui.components.Overline
import app.studycapture.ui.components.ScreenHeader
import app.studycapture.ui.components.SourceChip
import app.studycapture.ui.theme.StudyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardsState(val cards: List<FlashcardEntity> = emptyList(), val generating: Boolean = true, val generatedCount: Int? = null)

class CardSuggestionsViewModel(private val materialId: String, private val study: StudyRepository) : ViewModel() {
    private val generating = MutableStateFlow(false)
    private val generated = MutableStateFlow<Int?>(null)

    init {
        // Suggest once; reopening the screen keeps pending suggestions and the user's decisions.
        viewModelScope.launch { if (!study.hasSuggestions(materialId)) generate() }
    }

    val state: StateFlow<CardsState> = combine(study.observeCards(materialId), generating, generated) { c, g, n -> CardsState(c, g, n) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardsState())

    fun generate() {
        viewModelScope.launch {
            generating.value = true
            try {
                generated.value = study.suggestCards(materialId)
            } finally {
                generating.value = false
            }
        }
    }

    fun approve(card: FlashcardEntity, front: String? = null, back: String? = null) = viewModelScope.launch { study.approve(card.id, front, back) }
    fun discard(card: FlashcardEntity) = viewModelScope.launch { study.discard(card.id) }
    fun undo(card: FlashcardEntity) = viewModelScope.launch { study.unapprove(card.id) }
    fun approveAll() = viewModelScope.launch {
        state.value.cards.filter { it.status == CardStatus.SUGGESTED }.forEach { study.approve(it.id) }
    }
}

/** Generated cards are only suggestions: nothing reaches Review until it is approved here. */
@Composable
fun CardSuggestionsScreen(
    materialId: String,
    onBack: () -> Unit,
    onOpenSource: (pageId: String, blockIndex: Int?) -> Unit,
    onStartReview: () -> Unit,
) {
    val vm = appViewModel { CardSuggestionsViewModel(materialId, it.study) }
    val state by vm.state.collectAsStateWithLifecycle()
    val suggested = state.cards.count { it.status == CardStatus.SUGGESTED }
    val approved = state.cards.count { it.status == CardStatus.ACTIVE }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(stringResource(R.string.cards_title), onBack, subtitle = stringResource(R.string.cards_subtitle))
        EngineNote(stringResource(R.string.cards_engine_note), Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!state.generating && state.cards.isEmpty()) {
                item { EmptyState(AppIcons.Cards, stringResource(R.string.cards_none_title), stringResource(R.string.cards_none_body)) }
            }
            items(state.cards, key = { it.id }) { card ->
                CardItem(
                    card = card,
                    onApprove = { f, b -> vm.approve(card, f, b) },
                    onDiscard = { vm.discard(card) },
                    onUndo = { vm.undo(card) },
                    onSource = { card.pageId?.let { onOpenSource(it, card.blockIndex) } },
                )
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.cards_summary, approved, approved + suggested),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.approveAll() }, enabled = suggested > 0, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(stringResource(R.string.cards_approve_all))
                }
                Button(onClick = onStartReview, enabled = approved > 0, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(stringResource(R.string.cards_start_review))
                }
            }
        }
    }
}

@Composable
private fun CardItem(
    card: FlashcardEntity,
    onApprove: (String?, String?) -> Unit,
    onDiscard: () -> Unit,
    onUndo: () -> Unit,
    onSource: () -> Unit,
) {
    var editing by remember(card.id) { mutableStateOf(false) }
    var front by remember(card.id, card.front) { mutableStateOf(card.front) }
    var back by remember(card.id, card.back) { mutableStateOf(card.back) }
    val active = card.status == CardStatus.ACTIVE
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) StudyTheme.colors.success else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (editing) {
                OutlinedTextField(front, { front = it }, label = { Text(stringResource(R.string.cards_front)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(back, { back = it }, label = { Text(stringResource(R.string.cards_back)) }, modifier = Modifier.fillMaxWidth())
            } else {
                Overline(stringResource(R.string.cards_front))
                Text(card.front, style = MaterialTheme.typography.titleMedium)
                Overline(stringResource(R.string.cards_back))
                Text(card.back, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.pageId != null) SourceChip(stringResource(R.string.cards_source), onSource)
                Spacer(Modifier.weight(1f))
                when {
                    active -> {
                        Icon(AppIcons.Check, null, Modifier.size(16.dp), tint = StudyTheme.colors.success)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.cards_added), style = MaterialTheme.typography.labelLarge, color = StudyTheme.colors.success)
                        TextButton(onClick = onUndo) { Text(stringResource(R.string.action_undo)) }
                    }
                    editing -> {
                        TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) }
                        Button(onClick = { onApprove(front.trim(), back.trim()); editing = false }, enabled = front.isNotBlank() && back.isNotBlank()) {
                            Text(stringResource(R.string.cards_approve))
                        }
                    }
                    else -> {
                        TextButton(onClick = onDiscard) { Text(stringResource(R.string.cards_discard)) }
                        TextButton(onClick = { editing = true }) { Text(stringResource(R.string.action_edit)) }
                        Button(onClick = { onApprove(null, null) }) { Text(stringResource(R.string.cards_approve)) }
                    }
                }
            }
        }
    }
}
