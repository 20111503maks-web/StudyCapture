package app.studycapture.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studycapture.data.db.MaterialRow
import app.studycapture.data.db.SearchHitRow
import app.studycapture.data.db.SubjectSummaryRow
import app.studycapture.data.repo.LibraryRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class LibraryState(
    val subjects: List<SubjectSummaryRow> = emptyList(),
    val recent: List<MaterialRow> = emptyList(),
    val unfinished: List<MaterialRow> = emptyList(),
)

/** null = not searching; empty list = searched, nothing found. */
data class SearchState(val query: String = "", val hits: List<SearchHitRow>? = null)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryViewModel(private val library: LibraryRepository) : ViewModel() {
    private val now = System.currentTimeMillis()
    private val query = MutableStateFlow("")
    val queryText: StateFlow<String> = query.asStateFlow()

    val state: StateFlow<LibraryState> = combine(
        library.observeSubjects(now),
        library.observeMaterials(limit = 6),
        library.observeUnfinished(),
    ) { subjects, recent, unfinished -> LibraryState(subjects, recent, unfinished) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    val search: StateFlow<SearchState> = query
        .debounce(250)
        .distinctUntilChanged()
        .mapLatest { q -> if (q.isBlank()) SearchState(q, null) else SearchState(q, library.search(q)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun onQueryChange(value: String) {
        query.value = value
    }
}
