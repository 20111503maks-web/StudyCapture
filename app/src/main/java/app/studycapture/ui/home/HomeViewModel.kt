package app.studycapture.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studycapture.data.db.MaterialRow
import app.studycapture.data.db.WeakTopicRow
import app.studycapture.data.repo.LibraryRepository
import app.studycapture.data.repo.StudyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class HomeState(
    val loading: Boolean = true,
    val readyCount: Int = 0,
    val dueCount: Int = 0,
    val unfinished: List<MaterialRow> = emptyList(),
    val recent: MaterialRow? = null,
    val weakTopic: WeakTopicRow? = null,
    /** Last 7 days, oldest first: did the user review anything that day? */
    val studyDays: List<Boolean> = List(7) { false },
) {
    val isFirstRun get() = !loading && readyCount == 0 && unfinished.isEmpty()
}

class HomeViewModel(library: LibraryRepository, study: StudyRepository) : ViewModel() {
    private val now = System.currentTimeMillis()
    private val weekAgo = now - TimeUnit.DAYS.toMillis(7)
    private val twoWeeksAgo = now - TimeUnit.DAYS.toMillis(14)

    val state: StateFlow<HomeState> = combine(
        library.observeReadyCount(),
        study.observeDueCount(now),
        library.observeUnfinished(),
        library.observeMaterials(limit = 1),
        combine(study.observeWeakTopic(twoWeeksAgo), study.observeReviewTimes(weekAgo)) { weak, times -> weak to times },
    ) { ready, due, unfinished, recent, (weak, times) ->
        HomeState(
            loading = false,
            readyCount = ready,
            dueCount = due,
            unfinished = unfinished,
            recent = recent.firstOrNull(),
            weakTopic = weak,
            studyDays = studyDays(times),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun studyDays(times: List<Long>): List<Boolean> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = times.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
        return (6 downTo 0).map { today.minusDays(it.toLong()) in days }
    }
}
