package app.studycapture.ui.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studycapture.R
import app.studycapture.data.repo.toModel
import app.studycapture.processing.MaterialProcessor
import app.studycapture.ui.appViewModel
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.ConfirmDialog
import app.studycapture.ui.components.PagePhoto
import app.studycapture.ui.components.ScreenHeader
import app.studycapture.ui.components.StudyCard
import app.studycapture.ui.materialTitle

/** Step "Show original + extracted content" → "Choose/Detect subject" → "Save". */
@Composable
fun CheckTextScreen(materialId: String, onBack: () -> Unit, onSaved: (String) -> Unit, onDeleted: () -> Unit) {
    val vm = appViewModel { CheckTextViewModel(materialId, it.library) }
    val state by vm.state.collectAsStateWithLifecycle()
    var focusedBlock by remember { mutableStateOf<Long?>(null) }
    var showSave by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val page = state.page

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(
            title = stringResource(R.string.check_title),
            onBack = onBack,
            subtitle = if (state.pages.size > 1) stringResource(R.string.page_of, state.pageIndex + 1, state.pages.size) else null,
        ) {
            IconButton(onClick = { confirmDiscard = true }) { Icon(AppIcons.Trash, stringResource(R.string.check_discard)) }
        }
        if (page != null && (page.originalPath != null || page.processedPath != null)) {
            val highlight = state.pageBlocks.firstOrNull { it.rowId == focusedBlock }?.toModel()?.box
            PagePhoto(page.processedPath ?: page.originalPath, highlight, Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp))
        }
        if (state.pages.size > 1) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.pages.size) { i ->
                    FilterChip(selected = i == state.pageIndex, onClick = { vm.selectPage(i); focusedBlock = null }, label = {
                        Text(stringResource(R.string.page_n, i + 1))
                    })
                }
            }
        }
        page?.let { p ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.language?.takeIf { it != "und" }?.let { AssistChip(onClick = {}, label = { Text(it.uppercase()) }) }
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.check_read_on_device)) })
            }
            if (p.error == MaterialProcessor.ERROR_NO_CYRILLIC) {
                Text(
                    stringResource(R.string.check_no_cyrillic),
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (p.error != null) {
                Text(
                    stringResource(R.string.processing_page_failed, p.pageIndex + 1, p.error),
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.pageBlocks.isEmpty()) {
                item {
                    StudyCard {
                        Text(stringResource(R.string.check_no_text), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(state.pageBlocks, key = { it.rowId }) { block ->
                EditableBlock(
                    block = block,
                    highlighted = block.rowId == focusedBlock,
                    onShowOnPhoto = { focusedBlock = block.rowId },
                    onEdit = { vm.edit(block, it) },
                    onConfirm = { vm.confirm(block) },
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            if (state.uncertainCount > 0) {
                Text(
                    stringResource(R.string.check_uncertain_left, state.uncertainCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = { showSave = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = state.material != null && state.blocks.isNotEmpty(),
            ) {
                Icon(AppIcons.Check, null)
                Text("  " + stringResource(R.string.check_save))
            }
        }
    }

    val material = state.material
    if (showSave && material != null) {
        SaveSheet(
            suggestedSubject = material.suggestedSubject,
            suggestedTopic = material.suggestedTopic,
            keywords = material.suggestionKeywords,
            initialTitle = materialTitle(material.title),
            existingSubjects = state.existingSubjects,
            loadPlacement = vm::currentPlacement,
            onDismiss = { showSave = false },
            onSave = { subject, topic, title -> vm.save(subject, topic, title) { showSave = false; onSaved(materialId) } },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.check_discard),
            body = stringResource(R.string.check_discard_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { vm.discard(onDeleted) },
            onDismiss = { confirmDiscard = false },
        )
    }
}
