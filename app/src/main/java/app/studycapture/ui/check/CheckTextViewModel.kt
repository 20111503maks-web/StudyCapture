package app.studycapture.ui.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studycapture.data.db.BlockEntity
import app.studycapture.data.db.MaterialEntity
import app.studycapture.data.db.PageEntity
import app.studycapture.data.repo.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CheckState(
    val material: MaterialEntity? = null,
    val pages: List<PageEntity> = emptyList(),
    val blocks: List<BlockEntity> = emptyList(),
    val pageIndex: Int = 0,
    val existingSubjects: List<String> = emptyList(),
) {
    val page: PageEntity? get() = pages.getOrNull(pageIndex)
    val pageBlocks: List<BlockEntity> get() = blocks.filter { it.pageId == page?.id }
    val uncertainCount: Int get() = blocks.count { it.uncertain }
}

class CheckTextViewModel(private val materialId: String, private val library: LibraryRepository) : ViewModel() {
    private val pageIndex = MutableStateFlow(0)
    private val subjects = MutableStateFlow<List<String>>(emptyList())

    val state: StateFlow<CheckState> = combine(
        library.observeMaterial(materialId),
        library.observePages(materialId),
        library.observeBlocks(materialId),
        pageIndex,
        subjects,
    ) { material, pages, blocks, index, subjectNames ->
        CheckState(material, pages, blocks, index.coerceIn(0, (pages.size - 1).coerceAtLeast(0)), subjectNames)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckState())

    init {
        viewModelScope.launch { subjects.value = library.subjectNames() }
    }

    fun selectPage(index: Int) {
        pageIndex.value = index
    }

    fun edit(block: BlockEntity, text: String) {
        viewModelScope.launch { library.editBlock(block.pageId, block.blockIndex, text) }
    }

    fun confirm(block: BlockEntity) {
        viewModelScope.launch { library.confirmBlock(block.pageId, block.blockIndex) }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch { library.deletePage(pageId) }
    }

    fun save(subject: String, topic: String, title: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            library.saveMaterial(materialId, subject, topic, title)
            onSaved()
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            library.deleteMaterial(materialId)
            onDone()
        }
    }

    /** Current subject/topic names when an already saved material is reopened for editing. */
    suspend fun currentPlacement(): Pair<String?, String?> {
        val row = library.observeMaterialRow(materialId).first()
        return row?.subjectName to row?.topicName
    }
}
