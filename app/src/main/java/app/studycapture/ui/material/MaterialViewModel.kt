package app.studycapture.ui.material

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studycapture.data.db.BlockEntity
import app.studycapture.data.db.MaterialRow
import app.studycapture.data.db.PageEntity
import app.studycapture.data.repo.LibraryRepository
import app.studycapture.data.repo.StoredSummary
import app.studycapture.data.repo.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MaterialState(
    val material: MaterialRow? = null,
    val pages: List<PageEntity> = emptyList(),
    val blocks: List<BlockEntity> = emptyList(),
    val summary: StoredSummary? = null,
    val generating: Boolean = false,
    val loaded: Boolean = false,
) {
    fun pageNumber(pageId: String): Int = pages.indexOfFirst { it.id == pageId } + 1
    fun block(pageId: String, index: Int): BlockEntity? = blocks.firstOrNull { it.pageId == pageId && it.blockIndex == index }
    fun page(pageId: String): PageEntity? = pages.firstOrNull { it.id == pageId }
}

class MaterialViewModel(
    private val materialId: String,
    private val library: LibraryRepository,
    private val study: StudyRepository,
) : ViewModel() {
    private val generating = MutableStateFlow(false)

    val state: StateFlow<MaterialState> = combine(
        library.observeMaterialRow(materialId),
        library.observePages(materialId),
        library.observeBlocks(materialId),
        study.observeSummary(materialId),
        generating,
    ) { m, p, b, s, g -> MaterialState(m, p, b, s, g, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MaterialState())

    fun generateSummary() {
        if (generating.value) return
        viewModelScope.launch {
            generating.value = true
            try {
                study.generateSummary(materialId)
            } finally {
                generating.value = false
            }
        }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch { library.deletePage(pageId) }
    }

    fun deleteMaterial(onDone: () -> Unit) {
        viewModelScope.launch {
            library.deleteMaterial(materialId)
            onDone()
        }
    }
}
