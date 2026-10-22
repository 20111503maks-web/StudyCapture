package app.studycapture.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studycapture.R
import app.studycapture.data.db.MaterialStatus
import app.studycapture.data.db.SearchHitRow
import app.studycapture.ui.appViewModel
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.EmptyState
import app.studycapture.ui.components.Overline
import app.studycapture.ui.components.StudyCard
import app.studycapture.ui.components.SubjectBadge
import app.studycapture.ui.materialTitle

@Composable
fun LibraryScreen(
    onOpenSubject: (String) -> Unit,
    onOpenMaterial: (id: String, pageId: String?, blockIndex: Int?) -> Unit,
    onOpenUnfinished: (id: String, processing: Boolean) -> Unit,
) {
    val vm = appViewModel { LibraryViewModel(it.library) }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.queryText.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text(stringResource(R.string.nav_library), style = MaterialTheme.typography.headlineLarge) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.library_search_hint)) },
                leadingIcon = { Icon(AppIcons.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) { Icon(AppIcons.Close, stringResource(R.string.action_clear)) }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        }
        val hits = search.hits
        if (query.isNotBlank() && hits != null) {
            if (hits.isEmpty()) {
                item { EmptyState(AppIcons.Search, stringResource(R.string.search_none_title), stringResource(R.string.search_none_body)) }
            } else {
                item { Overline(pluralStringResource(R.plurals.search_results, hits.size, hits.size)) }
                items(hits, key = { "${it.pageId}-${it.blockIndex}" }) { hit ->
                    SearchHit(hit) { onOpenMaterial(hit.materialId, hit.pageId, hit.blockIndex) }
                }
            }
            return@LazyColumn
        }

        if (state.unfinished.isNotEmpty()) {
            item { Overline(stringResource(R.string.library_unfinished), Modifier.padding(top = 8.dp)) }
            items(state.unfinished, key = { "u-" + it.id }) { m ->
                val processing = m.status == MaterialStatus.PROCESSING
                StudyCard(onClick = { onOpenUnfinished(m.id, processing) }) {
                    Column {
                        Text(materialTitle(m.title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(
                                when (m.status) {
                                    MaterialStatus.PROCESSING -> R.string.home_processing
                                    MaterialStatus.FAILED -> R.string.home_failed
                                    else -> R.string.home_needs_check
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { Overline(stringResource(R.string.library_subjects), Modifier.padding(top = 8.dp)) }
        if (state.subjects.isEmpty()) {
            item { EmptyState(AppIcons.Library, stringResource(R.string.library_empty_title), stringResource(R.string.library_empty_body)) }
        }
        items(state.subjects, key = { it.id }) { s ->
            StudyCard(onClick = { onOpenSubject(s.id) }, contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubjectBadge(s.name, s.colorIndex)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium)
                        val meta = pluralStringResource(R.plurals.materials, s.materialCount, s.materialCount) +
                            (s.lastMaterialTitle?.let { " · " + stringResource(R.string.library_last, it) } ?: "")
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    if (s.dueCount > 0) {
                        Text(
                            stringResource(R.string.library_due, s.dueCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Icon(AppIcons.Chevron, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.recent.isNotEmpty()) {
            item { Overline(stringResource(R.string.library_recent), Modifier.padding(top = 8.dp)) }
            items(state.recent, key = { "r-" + it.id }) { m ->
                StudyCard(onClick = { onOpenMaterial(m.id, null, null) }) {
                    Column {
                        Text(materialTitle(m.title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(m.subjectName, m.topicName).joinToString(" › ") + " · " +
                                pluralStringResource(R.plurals.pages, m.pageCount, m.pageCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** A hit shows the page it was found on; matched words are bold (FTS snippet markers). */
@Composable
private fun SearchHit(hit: SearchHitRow, onClick: () -> Unit) {
    StudyCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                listOfNotNull(hit.subjectName, materialTitle(hit.materialTitle)).joinToString(" › ") +
                    " · " + stringResource(R.string.page_n, hit.pageIndex + 1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(highlightSnippet(hit.snippet), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun highlightSnippet(snippet: String) = buildAnnotatedString {
    var rest = snippet
    while (rest.isNotEmpty()) {
        val start = rest.indexOf('⟪')
        val end = if (start >= 0) rest.indexOf('⟫', start) else -1
        if (start < 0 || end < 0) {
            append(rest)
            break
        }
        append(rest.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(rest.substring(start + 1, end)) }
        rest = rest.substring(end + 1)
    }
}
