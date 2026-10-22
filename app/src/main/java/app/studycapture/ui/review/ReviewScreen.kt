package app.studycapture.ui.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studycapture.R
import app.studycapture.srs.Rating
import app.studycapture.ui.appViewModel
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.EmptyState
import app.studycapture.ui.components.SourceChip

@Composable
fun ReviewScreen(
    topicId: String?,
    onOpenSource: (materialId: String, pageId: String?, blockIndex: Int?) -> Unit,
    onDone: () -> Unit,
) {
    val vm = appViewModel { ReviewViewModel(topicId, it.study) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.nav_review), style = MaterialTheme.typography.headlineLarge)
        when {
            state.loading -> Unit
            state.queue.isEmpty() -> EmptyState(AppIcons.Cards, stringResource(R.string.review_none_title), stringResource(R.string.review_none_body))
            state.finished -> Finished(state.reviewed, state.again, onDone)
            else -> Session(state, vm::reveal, vm::rate, onOpenSource)
        }
    }
}

@Composable
private fun Session(
    state: ReviewState,
    onReveal: () -> Unit,
    onRate: (RatingOption) -> Unit,
    onOpenSource: (String, String?, Int?) -> Unit,
) {
    val card = state.current ?: return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { state.index.toFloat() / state.queue.size },
                modifier = Modifier.weight(1f).height(8.dp),
            )
            Spacer(Modifier.padding(6.dp))
            Text("${state.index + 1} / ${state.queue.size}", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            listOfNotNull(card.subjectName, card.materialTitle.ifBlank { null }).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().height(340.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(card.front, style = MaterialTheme.typography.headlineSmall)
                AnimatedVisibility(state.revealed, enter = fadeIn() + expandVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(card.back, style = MaterialTheme.typography.bodyLarge)
                        if (card.pageId != null) {
                            SourceChip(stringResource(R.string.review_source), { onOpenSource(card.materialId, card.pageId, card.blockIndex) })
                        }
                    }
                }
            }
        }
        if (!state.revealed) {
            Button(
                onClick = onReveal,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface),
            ) { Text(stringResource(R.string.review_show_answer), style = MaterialTheme.typography.titleMedium) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.options.forEach { option ->
                    RatingButton(option, Modifier.weight(1f)) { onRate(option) }
                }
            }
        }
        Text(
            stringResource(R.string.review_fsrs_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RatingButton(option: RatingOption, modifier: Modifier, onClick: () -> Unit) {
    val label = stringResource(
        when (option.rating) {
            Rating.Again -> R.string.rating_again
            Rating.Hard -> R.string.rating_hard
            Rating.Good -> R.string.rating_good
            Rating.Easy -> R.string.rating_easy
        },
    )
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(option.label, style = MaterialTheme.typography.labelSmall)
        }
    }
    if (option.rating == Rating.Good) {
        Button(onClick = onClick, modifier = modifier.height(64.dp), contentPadding = ButtonDefaults.TextButtonContentPadding) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(64.dp),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
            colors = if (option.rating == Rating.Again) {
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
        ) { content() }
    }
}

@Composable
private fun Finished(reviewed: Int, again: Int, onDone: () -> Unit) {
    EmptyState(
        icon = AppIcons.Check,
        title = stringResource(R.string.review_done_title),
        body = if (again > 0) stringResource(R.string.review_done_again, reviewed, again) else stringResource(R.string.review_done_body, reviewed),
    ) {
        OutlinedButton(onClick = onDone) { Text(stringResource(R.string.action_close)) }
    }
}
