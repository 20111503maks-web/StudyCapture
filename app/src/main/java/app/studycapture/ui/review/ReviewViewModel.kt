package app.studycapture.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studycapture.data.db.DueCardRow
import app.studycapture.data.repo.StudyRepository
import app.studycapture.data.repo.toSrs
import app.studycapture.srs.Rating
import app.studycapture.srs.SrsCard
import app.studycapture.srs.formatInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class RatingOption(val rating: Rating, val label: String, val next: SrsCard)

data class ReviewState(
    val loading: Boolean = true,
    val queue: List<DueCardRow> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val options: List<RatingOption> = emptyList(),
    val reviewed: Int = 0,
    val again: Int = 0,
) {
    val current: DueCardRow? get() = queue.getOrNull(index)
    val finished: Boolean get() = !loading && current == null
}

/**
 * One review session. The queue is fixed at start; cards answered "Again" come back
 * in a later session (FSRS schedules them minutes ahead), so the session always ends.
 */
class ReviewViewModel(private val topicId: String?, private val study: StudyRepository) : ViewModel() {
    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()
    private var shownAt = System.currentTimeMillis()
    private var saving = false
    private var revealedAt = 0L

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ReviewState(loading = false, queue = study.dueCards(topicId))
        }
    }

    /** Reloads the queue when the tab is reopened, unless a card is in progress. */
    fun refreshIfIdle() {
        val s = _state.value
        if (!s.loading && !s.revealed && (s.finished || s.index == 0)) load()
    }

    fun reveal() {
        revealedAt = System.currentTimeMillis()
        val card = _state.value.current ?: return
        val now = Instant.now()
        val options = study.preview(card.toSrs(), now).entries.sortedBy { it.key.value }.map { (rating, next) ->
            RatingOption(rating, formatInterval(now, next.due), next)
        }
        _state.value = _state.value.copy(revealed = true, options = options)
    }

    fun rate(option: RatingOption) {
        val s = _state.value
        val card = s.current ?: return
        if (saving) return // ignore double taps: one answer per card
        saving = true
        val duration = System.currentTimeMillis() - shownAt
        // The previewed schedule is reused so the label equals what is stored — unless the
        // answer stayed open so long that "now" has moved on; then schedule afresh.
        val previewed = option.next.takeIf { System.currentTimeMillis() - revealedAt < STALE_PREVIEW_MS }
        viewModelScope.launch {
            try {
                study.review(card.id, option.rating, previewed, duration)
            } finally {
                saving = false
            }
            _state.value = s.copy(
                index = s.index + 1,
                revealed = false,
                options = emptyList(),
                reviewed = s.reviewed + 1,
                again = s.again + if (option.rating == Rating.Again) 1 else 0,
            )
            shownAt = System.currentTimeMillis()
        }
    }

    private companion object {
        const val STALE_PREVIEW_MS = 10 * 60 * 1000L
    }
}
