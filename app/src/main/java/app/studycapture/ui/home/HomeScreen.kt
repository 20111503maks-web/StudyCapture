package app.studycapture.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studycapture.R
import app.studycapture.data.db.MaterialRow
import app.studycapture.data.db.MaterialStatus
import app.studycapture.ui.appViewModel
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.Overline
import app.studycapture.ui.components.StudyCard
import app.studycapture.ui.components.SubjectBadge
import app.studycapture.ui.materialTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    onCapture: () -> Unit,
    onOpenMaterial: (String) -> Unit,
    onContinue: (String) -> Unit,
    onProcessing: (String) -> Unit,
    onReview: (topicId: String?) -> Unit,
    onSearch: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.library, it.study) }
    val state by vm.state.collectAsStateWithLifecycle()
    when {
        state.loading -> Unit
        state.isFirstRun -> FirstRunContent(onCapture)
        else -> Dashboard(state, onOpenMaterial, onContinue, onProcessing, onReview, onSearch)
    }
}

@Composable
private fun Dashboard(
    state: HomeState,
    onOpenMaterial: (String) -> Unit,
    onContinue: (String) -> Unit,
    onProcessing: (String) -> Unit,
    onReview: (String?) -> Unit,
    onSearch: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.home_today), style = MaterialTheme.typography.headlineLarge)
                }
                FilledIconButton(
                    onClick = onSearch,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) { Icon(AppIcons.Search, stringResource(R.string.library_search_hint)) }
            }
        }
        item { DueCard(state.dueCount) { onReview(null) } }
        state.unfinished.firstOrNull()?.let { m ->
            item {
                MaterialLink(
                    overline = stringResource(R.string.home_continue),
                    material = m,
                    detail = stringResource(
                        when (m.status) {
                            MaterialStatus.PROCESSING -> R.string.home_processing
                            MaterialStatus.FAILED -> R.string.home_failed
                            else -> R.string.home_needs_check
                        },
                    ),
                    onClick = { if (m.status == MaterialStatus.NEEDS_REVIEW) onContinue(m.id) else onProcessing(m.id) },
                )
            }
        }
        state.recent?.let { m ->
            item {
                MaterialLink(
                    overline = stringResource(R.string.home_recent),
                    material = m,
                    detail = pluralStringResource(R.plurals.pages, m.pageCount, m.pageCount),
                    onClick = { onOpenMaterial(m.id) },
                )
            }
        }
        state.weakTopic?.let { weak ->
            item {
                StudyCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Overline(stringResource(R.string.home_weak))
                        Text(weak.topicName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.home_weak_detail, weak.again, weak.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { onReview(weak.topicId) }, Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_weak_action))
                        }
                    }
                }
            }
        }
        item { RhythmCard(state.studyDays) }
    }
}

@Composable
private fun DueCard(due: Int, onStart: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (due > 0) {
                Text("$due", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    pluralStringResource(R.plurals.reviews_due, due, due),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text(stringResource(R.string.home_start_review)) }
            } else {
                Text(stringResource(R.string.home_no_reviews), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    stringResource(R.string.home_no_reviews_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun MaterialLink(overline: String, material: MaterialRow, detail: String, onClick: () -> Unit) {
    StudyCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(material.subjectName ?: material.title.ifBlank { "?" }, material.subjectColor ?: 0)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Overline(overline)
                Text(materialTitle(material.title), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(AppIcons.Chevron, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Calm study rhythm — no streak counter, no pressure (see design notes). */
@Composable
private fun RhythmCard(days: List<Boolean>) {
    val studied = days.count { it }
    StudyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_rhythm, studied), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.home_rhythm_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { active ->
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ) {}
                }
            }
        }
    }
}
