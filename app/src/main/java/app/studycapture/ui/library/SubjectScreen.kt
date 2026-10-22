package app.studycapture.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.studycapture.R
import app.studycapture.data.db.MaterialRow
import app.studycapture.data.db.SubjectEntity
import app.studycapture.data.db.TopicSummaryRow
import app.studycapture.data.repo.LibraryRepository
import app.studycapture.ui.appViewModel
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.ConfirmDialog
import app.studycapture.ui.components.Overline
import app.studycapture.ui.components.ScreenHeader
import app.studycapture.ui.components.StudyCard
import app.studycapture.ui.materialTitle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectState(
    val subject: SubjectEntity? = null,
    val topics: List<TopicSummaryRow> = emptyList(),
    val materials: List<MaterialRow> = emptyList(),
)

class SubjectViewModel(private val subjectId: String, private val library: LibraryRepository) : ViewModel() {
    val state: StateFlow<SubjectState> = combine(
        library.observeSubject(subjectId),
        library.observeTopics(subjectId, System.currentTimeMillis()),
        library.observeMaterials(subjectId),
    ) { s, t, m -> SubjectState(s, t, m) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectState())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            library.deleteSubject(subjectId)
            onDone()
        }
    }
}

@Composable
fun SubjectScreen(
    subjectId: String,
    onBack: () -> Unit,
    onOpenMaterial: (String) -> Unit,
    onReviewTopic: (String) -> Unit,
) {
    val vm = appViewModel { SubjectViewModel(subjectId, it.library) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = state.subject?.name.orEmpty(),
                onBack = onBack,
                subtitle = pluralStringResource(R.plurals.materials, state.materials.size, state.materials.size),
            ) {
                IconButton(onClick = { confirmDelete = true }) { Icon(AppIcons.Trash, stringResource(R.string.subject_delete)) }
            }
        }
        items(state.topics, key = { it.id }) { topic ->
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline(topic.name)
                        Text(
                            pluralStringResource(R.plurals.materials, topic.materialCount, topic.materialCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (topic.dueCount > 0) {
                        TextButton(onClick = { onReviewTopic(topic.id) }) { Text(stringResource(R.string.library_review_n, topic.dueCount)) }
                    }
                }
                state.materials.filter { it.topicId == topic.id }.forEach { m ->
                    StudyCard(onClick = { onOpenMaterial(m.id) }) {
                        Column {
                            Text(materialTitle(m.title), style = MaterialTheme.typography.titleMedium)
                            Text(
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

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.subject_delete),
            body = stringResource(R.string.subject_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}
