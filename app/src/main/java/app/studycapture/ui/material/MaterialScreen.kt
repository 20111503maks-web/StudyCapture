package app.studycapture.ui.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studycapture.R
import app.studycapture.ui.appViewModel
import app.studycapture.ui.check.BlockText
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.ConfirmDialog
import app.studycapture.ui.components.Overline
import app.studycapture.ui.components.PagePhoto
import app.studycapture.ui.components.ScreenHeader
import app.studycapture.ui.components.StudyCard
import app.studycapture.ui.materialTitle

private enum class MaterialTab { PAGES, TEXT, SUMMARY }

private data class SourceTarget(val pageId: String, val blockIndex: Int?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialScreen(
    materialId: String,
    focusPageId: String?,
    focusBlockIndex: Int?,
    onBack: () -> Unit,
    onMakeCards: () -> Unit,
    onEditText: () -> Unit,
    onDeleted: () -> Unit,
) {
    val vm = appViewModel { MaterialViewModel(materialId, it.library, it.study) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(if (focusPageId != null) MaterialTab.TEXT.ordinal else MaterialTab.SUMMARY.ordinal) }
    var source by remember { mutableStateOf<SourceTarget?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Leave exactly once, whether the user deleted the material or its last page.
    var closed by remember { mutableStateOf(false) }
    val close: () -> Unit = { if (!closed) { closed = true; onDeleted() } }
    LaunchedEffect(state.loaded, state.material) {
        if (state.loaded && state.material == null) close()
    }

    // Jump to the block a search hit or flashcard pointed at.
    LaunchedEffect(state.loaded, focusPageId, focusBlockIndex) {
        if (!state.loaded || focusPageId == null || tab != MaterialTab.TEXT.ordinal) return@LaunchedEffect
        val index = state.blocks.indexOfFirst { it.pageId == focusPageId && it.blockIndex == focusBlockIndex }
        if (index >= 0) listState.scrollToItem(index)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val m = state.material
        ScreenHeader(
            title = materialTitle(m?.title.orEmpty()),
            onBack = onBack,
            subtitle = listOfNotNull(m?.subjectName, m?.topicName).joinToString(" › ").ifEmpty { null },
        ) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(AppIcons.Edit, stringResource(R.string.material_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.material_edit)) }, onClick = { menu = false; onEditText() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.material_delete)) }, onClick = { menu = false; confirmDelete = true })
                }
            }
        }
        SecondaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(pluralStringResource(R.plurals.pages, state.pages.size, state.pages.size)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.material_tab_text)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.material_tab_summary)) })
        }
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (MaterialTab.entries[tab]) {
                MaterialTab.PAGES -> pagesTab(state, vm::deletePage)
                MaterialTab.TEXT -> textTab(state, focusPageId, focusBlockIndex) { pageId, index -> source = SourceTarget(pageId, index) }
                MaterialTab.SUMMARY -> summaryTab(state, vm::generateSummary) { pageId, index -> source = SourceTarget(pageId, index) }
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onMakeCards, Modifier.weight(1f).height(52.dp)) {
                Icon(AppIcons.Cards, null)
                Text("  " + stringResource(R.string.material_make_cards))
            }
        }
    }

    source?.let { target ->
        state.page(target.pageId)?.let { page ->
            SourceSheet(
                page = page,
                pageNumber = state.pageNumber(page.id),
                block = target.blockIndex?.let { state.block(page.id, it) },
                onDismiss = { source = null },
            )
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.material_delete),
            body = stringResource(R.string.material_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { vm.deleteMaterial(close) },
            onDismiss = { confirmDelete = false },
        )
    }
}

private fun LazyListScope.pagesTab(state: MaterialState, onDeletePage: (String) -> Unit) {
    items(state.pages, key = { it.id }) { page ->
        var confirm by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline(stringResource(R.string.page_n, page.pageIndex + 1), Modifier.weight(1f))
                IconButton(onClick = { confirm = true }) { Icon(AppIcons.Trash, stringResource(R.string.page_delete)) }
            }
            val photo = page.processedPath ?: page.originalPath
            if (photo != null) {
                PagePhoto(photo, null, Modifier.fillMaxWidth().height(420.dp), version = page.updatedAt)
            } else {
                Text(stringResource(R.string.page_typed_text), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (confirm) {
            ConfirmDialog(
                title = stringResource(R.string.page_delete),
                body = stringResource(R.string.page_delete_body),
                confirmLabel = stringResource(R.string.action_delete),
                onConfirm = { onDeletePage(page.id) },
                onDismiss = { confirm = false },
            )
        }
    }
}

private fun LazyListScope.textTab(
    state: MaterialState,
    focusPageId: String?,
    focusBlockIndex: Int?,
    onOpenSource: (String, Int) -> Unit,
) {
    items(state.blocks, key = { it.rowId }) { block ->
        val focused = block.pageId == focusPageId && block.blockIndex == focusBlockIndex
        StudyCard(onClick = { onOpenSource(block.pageId, block.blockIndex) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (focused) Overline(stringResource(R.string.material_found_here))
                BlockText(block)
            }
        }
    }
}

private fun LazyListScope.summaryTab(state: MaterialState, onGenerate: () -> Unit, onOpenSource: (String, Int) -> Unit) {
    val summary = state.summary
    item {
        Text(
            stringResource(R.string.summary_engine_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (summary == null || summary.isEmpty) {
        item {
            StudyCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(if (summary == null) R.string.summary_none else R.string.summary_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onGenerate, enabled = !state.generating) {
                        Text(stringResource(if (state.generating) R.string.summary_working else R.string.summary_make))
                    }
                }
            }
        }
        return
    }
    summary.sections.forEach { section ->
        item {
            StudyCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    section.title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    section.points.forEach { point ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Overline(summaryKindLabel(point.kind))
                            Text(point.text, style = MaterialTheme.typography.bodyLarge)
                            app.studycapture.ui.components.SourceChip(
                                stringResource(R.string.page_short, state.pageNumber(point.pageId)),
                                onClick = { onOpenSource(point.pageId, point.blockIndex) },
                            )
                        }
                    }
                }
            }
        }
    }
    item {
        OutlinedButton(onClick = onGenerate, enabled = !state.generating) { Text(stringResource(R.string.summary_regenerate)) }
    }
}

@Composable
private fun summaryKindLabel(kind: String): String = stringResource(
    when (kind) {
        "DEFINITION" -> R.string.block_definition
        "FORMULA" -> R.string.block_formula
        "LIST_ITEM" -> R.string.block_list
        else -> R.string.summary_key_point
    },
)
