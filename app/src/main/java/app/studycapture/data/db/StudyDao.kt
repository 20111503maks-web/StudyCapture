package app.studycapture.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class WeakTopicRow(
    val topicId: String,
    val topicName: String,
    val subjectName: String,
    val again: Int,
    val total: Int,
)

data class DueCardRow(
    val id: String,
    val materialId: String,
    val pageId: String?,
    val blockIndex: Int?,
    val front: String,
    val back: String,
    val srsState: Int?,
    val srsStep: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val dueAt: Long?,
    val lastReviewAt: Long?,
    val materialTitle: String,
    val subjectName: String?,
)

@Dao
interface StudyDao {
    @Insert
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun card(id: String): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE materialId = :materialId AND status != 'DISCARDED' ORDER BY createdAt")
    fun observeCards(materialId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE materialId = :materialId AND status = 'ACTIVE'")
    suspend fun activeCards(materialId: String): List<FlashcardEntity>

    @Query("DELETE FROM flashcards WHERE materialId = :materialId AND status = 'SUGGESTED'")
    suspend fun deleteSuggestions(materialId: String)

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'ACTIVE' AND dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query(
        """
        SELECT f.id, f.materialId, f.pageId, f.blockIndex, f.front, f.back, f.srsState, f.srsStep,
          f.stability, f.difficulty, f.dueAt, f.lastReviewAt, m.title AS materialTitle, s.name AS subjectName
        FROM flashcards f JOIN materials m ON m.id = f.materialId
        LEFT JOIN topics t ON t.id = m.topicId LEFT JOIN subjects s ON s.id = t.subjectId
        WHERE f.status = 'ACTIVE' AND f.dueAt <= :now AND (:topicId IS NULL OR m.topicId = :topicId)
        ORDER BY f.dueAt LIMIT :limit
        """,
    )
    suspend fun dueCards(now: Long, topicId: String?, limit: Int): List<DueCardRow>

    @Insert
    suspend fun insertLog(log: ReviewLogEntity)

    @Transaction
    suspend fun recordReview(card: FlashcardEntity, log: ReviewLogEntity) {
        updateCard(card)
        insertLog(log)
    }

    @Query("SELECT reviewedAt FROM review_logs WHERE reviewedAt >= :since")
    fun observeReviewTimes(since: Long): Flow<List<Long>>

    /** A topic where at least half of the recent answers were "Again" (min. 3 answers). */
    @Query(
        """
        SELECT t.id AS topicId, t.name AS topicName, s.name AS subjectName,
          SUM(CASE WHEN l.rating = 1 THEN 1 ELSE 0 END) AS again, COUNT(*) AS total
        FROM review_logs l
        JOIN flashcards f ON f.id = l.cardId
        JOIN materials m ON m.id = f.materialId
        JOIN topics t ON t.id = m.topicId
        JOIN subjects s ON s.id = t.subjectId
        WHERE l.reviewedAt >= :since
        GROUP BY t.id
        HAVING COUNT(*) >= 3 AND SUM(CASE WHEN l.rating = 1 THEN 1 ELSE 0 END) * 2 >= COUNT(*)
        ORDER BY CAST(SUM(CASE WHEN l.rating = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*) DESC
        LIMIT 1
        """,
    )
    fun observeWeakTopic(since: Long): Flow<WeakTopicRow?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE materialId = :materialId")
    fun observeSummary(materialId: String): Flow<SummaryEntity?>

    @Query("DELETE FROM summaries")
    suspend fun deleteAllSummaries()

    @Query("DELETE FROM summaries WHERE materialId = :materialId")
    suspend fun deleteSummary(materialId: String)

    /** Cards quote their page; review logs go with them (cascade). */
    @Query("DELETE FROM flashcards WHERE pageId = :pageId")
    suspend fun deleteCardsForPage(pageId: String)

    @Query("DELETE FROM flashcards WHERE status != 'ACTIVE'")
    suspend fun deleteUnapprovedCards()
}
