package app.studycapture.data.repo

import app.studycapture.ai.StudyAssistant
import app.studycapture.data.db.AppDatabase
import app.studycapture.data.db.CardStatus
import app.studycapture.data.db.DueCardRow
import app.studycapture.data.db.FlashcardEntity
import app.studycapture.data.db.ReviewLogEntity
import app.studycapture.data.db.SummaryEntity
import app.studycapture.srs.FsrsScheduler
import app.studycapture.srs.Rating
import app.studycapture.srs.SrsCard
import app.studycapture.understanding.PageText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class StudyRepository(
    private val db: AppDatabase,
    private val assistant: StudyAssistant,
    private val scheduler: FsrsScheduler,
) {
    private val studyDao get() = db.studyDao()
    private val pageDao get() = db.pageDao()

    val engine get() = assistant.engine

    private suspend fun pageTexts(materialId: String): List<PageText> {
        val blocks = pageDao.blocksForMaterial(materialId)
        return pageDao.pages(materialId).map { page ->
            PageText(page.id, blocks.filter { it.pageId == page.id }.map { it.toModel() })
        }
    }

    fun observeSummary(materialId: String): Flow<StoredSummary?> =
        studyDao.observeSummary(materialId).map { it?.let { entity -> SummaryJson.decode(entity.json) } }

    suspend fun generateSummary(materialId: String) {
        val summary = assistant.summarize(pageTexts(materialId))
        studyDao.upsertSummary(
            SummaryEntity(materialId, SummaryJson.encode(summary), assistant.engine.id, System.currentTimeMillis()),
        )
    }

    fun observeCards(materialId: String) = studyDao.observeCards(materialId)

    /** Replaces earlier unapproved suggestions; approved cards are never touched. Returns the count. */
    suspend fun suggestCards(materialId: String): Int {
        val now = System.currentTimeMillis()
        val existing = studyDao.decidedFronts(materialId).map { it.lowercase() }.toSet()
        val suggestions = assistant.suggestFlashcards(pageTexts(materialId))
            .filter { it.front.lowercase() !in existing }
        studyDao.deleteSuggestions(materialId)
        studyDao.insertCards(
            suggestions.map {
                FlashcardEntity(
                    id = UUID.randomUUID().toString(), materialId = materialId,
                    pageId = it.source.pageId, blockIndex = it.source.blockIndex,
                    front = it.front, back = it.back, kind = it.kind.name, status = CardStatus.SUGGESTED,
                    srsState = null, srsStep = null, stability = null, difficulty = null,
                    dueAt = null, lastReviewAt = null, createdAt = now, updatedAt = now,
                )
            },
        )
        return suggestions.size
    }

    suspend fun hasSuggestions(materialId: String): Boolean = studyDao.suggestedCount(materialId) > 0

    suspend fun approve(cardId: String, front: String? = null, back: String? = null) {
        val card = studyDao.card(cardId) ?: return
        val now = System.currentTimeMillis()
        val fresh = scheduler.newCard(Instant.ofEpochMilli(now))
        studyDao.updateCard(
            card.copy(front = front ?: card.front, back = back ?: card.back, status = CardStatus.ACTIVE)
                .withSrs(fresh, now),
        )
    }

    suspend fun discard(cardId: String) {
        val card = studyDao.card(cardId) ?: return
        studyDao.updateCard(card.copy(status = CardStatus.DISCARDED, updatedAt = System.currentTimeMillis()))
    }

    /** Moves an approved card back to suggestions (undo); its review history is kept. */
    suspend fun unapprove(cardId: String) {
        val card = studyDao.card(cardId) ?: return
        studyDao.updateCard(card.copy(status = CardStatus.SUGGESTED, updatedAt = System.currentTimeMillis()))
    }

    suspend fun dueCards(topicId: String?, limit: Int = 200): List<DueCardRow> =
        studyDao.dueCards(System.currentTimeMillis(), topicId, limit)

    fun preview(card: SrsCard, now: Instant = Instant.now()): Map<Rating, SrsCard> = scheduler.preview(card, now)

    /**
     * Records an answer. Pass the card from [preview] that the button showed, so the stored
     * due date equals the label (fuzzing draws new randomness on every call).
     */
    suspend fun review(cardId: String, rating: Rating, previewed: SrsCard?, durationMs: Long?): SrsCard? {
        val card = studyDao.card(cardId) ?: return null
        val nowMs = System.currentTimeMillis()
        val before = card.toSrs()
        val after = previewed ?: scheduler.review(before, rating, Instant.ofEpochMilli(nowMs))
        studyDao.recordReview(
            card.withSrs(after, nowMs),
            ReviewLogEntity(
                cardId = cardId, rating = rating.value, reviewedAt = nowMs, durationMs = durationMs,
                stateBefore = before.state.value, stabilityAfter = after.stability,
                difficultyAfter = after.difficulty, dueAfter = after.due.toEpochMilli(),
            ),
        )
        return after
    }

    fun observeDueCount() = studyDao.observeDueCount()
    fun observeReviewTimes(since: Long) = studyDao.observeReviewTimes(since)
    fun observeWeakTopic(since: Long) = studyDao.observeWeakTopic(since)
}
