package app.studycapture.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SubjectSummaryRow(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val materialCount: Int,
    val topicCount: Int,
    val dueCount: Int,
    val lastMaterialTitle: String?,
)

data class TopicSummaryRow(
    val id: String,
    val name: String,
    val materialCount: Int,
    val dueCount: Int,
)

data class MaterialRow(
    val id: String,
    val title: String,
    val status: MaterialStatus,
    val createdAt: Long,
    val topicId: String?,
    val topicName: String?,
    val subjectId: String?,
    val subjectName: String?,
    val subjectColor: Int?,
    val pageCount: Int,
)

@Dao
interface LibraryDao {
    @Query(
        """
        SELECT s.id, s.name, s.colorIndex,
          (SELECT COUNT(*) FROM materials m JOIN topics t ON m.topicId = t.id
             WHERE t.subjectId = s.id AND m.status = 'READY') AS materialCount,
          (SELECT COUNT(*) FROM topics t WHERE t.subjectId = s.id) AS topicCount,
          (SELECT COUNT(*) FROM flashcards f JOIN materials m ON f.materialId = m.id
             JOIN topics t ON m.topicId = t.id
             WHERE t.subjectId = s.id AND f.status = 'ACTIVE' AND f.dueAt <= :now) AS dueCount,
          (SELECT m.title FROM materials m JOIN topics t ON m.topicId = t.id
             WHERE t.subjectId = s.id AND m.status = 'READY'
             ORDER BY m.createdAt DESC LIMIT 1) AS lastMaterialTitle
        FROM subjects s ORDER BY s.name COLLATE NOCASE
        """,
    )
    fun observeSubjects(now: Long): Flow<List<SubjectSummaryRow>>

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE")
    suspend fun allSubjects(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observeSubject(id: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun subjectByName(name: String): SubjectEntity?

    @Insert
    suspend fun insertSubject(subject: SubjectEntity)

    @Query("SELECT m.id FROM materials m JOIN topics t ON m.topicId = t.id WHERE t.subjectId = :subjectId")
    suspend fun materialIdsForSubject(subjectId: String): List<String>

    @Query("SELECT id FROM materials")
    suspend fun allMaterialIds(): List<String>

    @Query("UPDATE materials SET suggestedSubject = NULL, suggestedTopic = NULL, suggestionKeywords = NULL")
    suspend fun clearSuggestions()

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: String)

    @Query(
        """
        SELECT t.id, t.name,
          (SELECT COUNT(*) FROM materials m WHERE m.topicId = t.id AND m.status = 'READY') AS materialCount,
          (SELECT COUNT(*) FROM flashcards f JOIN materials m ON f.materialId = m.id
             WHERE m.topicId = t.id AND f.status = 'ACTIVE' AND f.dueAt <= :now) AS dueCount
        FROM topics t WHERE t.subjectId = :subjectId ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun observeTopics(subjectId: String, now: Long): Flow<List<TopicSummaryRow>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun topicByName(subjectId: String, name: String): TopicEntity?

    @Insert
    suspend fun insertTopic(topic: TopicEntity)

    @Query(
        """
        SELECT m.id, m.title, m.status, m.createdAt, m.topicId,
          t.name AS topicName, s.id AS subjectId, s.name AS subjectName, s.colorIndex AS subjectColor,
          (SELECT COUNT(*) FROM pages p WHERE p.materialId = m.id) AS pageCount
        FROM materials m
        LEFT JOIN topics t ON m.topicId = t.id
        LEFT JOIN subjects s ON t.subjectId = s.id
        WHERE m.status = 'READY' AND (:subjectId IS NULL OR s.id = :subjectId)
        ORDER BY m.createdAt DESC LIMIT :limit
        """,
    )
    fun observeMaterials(subjectId: String?, limit: Int): Flow<List<MaterialRow>>

    @Query(
        """
        SELECT m.id, m.title, m.status, m.createdAt, m.topicId,
          t.name AS topicName, s.id AS subjectId, s.name AS subjectName, s.colorIndex AS subjectColor,
          (SELECT COUNT(*) FROM pages p WHERE p.materialId = m.id) AS pageCount
        FROM materials m
        LEFT JOIN topics t ON m.topicId = t.id
        LEFT JOIN subjects s ON t.subjectId = s.id
        WHERE m.status IN ('PROCESSING', 'NEEDS_REVIEW', 'FAILED')
        ORDER BY m.createdAt DESC
        """,
    )
    fun observeUnfinished(): Flow<List<MaterialRow>>

    @Query(
        """
        SELECT m.id, m.title, m.status, m.createdAt, m.topicId,
          t.name AS topicName, s.id AS subjectId, s.name AS subjectName, s.colorIndex AS subjectColor,
          (SELECT COUNT(*) FROM pages p WHERE p.materialId = m.id) AS pageCount
        FROM materials m
        LEFT JOIN topics t ON m.topicId = t.id
        LEFT JOIN subjects s ON t.subjectId = s.id
        WHERE m.id = :id
        """,
    )
    fun observeMaterialRow(id: String): Flow<MaterialRow?>

    /** Text of every saved material with its confirmed subject — the classifier learns from it. */
    @Query(
        """
        SELECT s.name AS subject, t.name AS topic,
          (SELECT GROUP_CONCAT(b.text, ' ') FROM blocks b JOIN pages p ON b.pageId = p.id
             WHERE p.materialId = m.id) AS text
        FROM materials m JOIN topics t ON m.topicId = t.id JOIN subjects s ON t.subjectId = s.id
        WHERE m.status = 'READY' ORDER BY m.createdAt DESC LIMIT 200
        """,
    )
    suspend fun labeledHistory(): List<LabeledRow>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun material(id: String): MaterialEntity?

    @Query("SELECT * FROM materials WHERE id = :id")
    fun observeMaterial(id: String): Flow<MaterialEntity?>

    @Insert
    suspend fun insertMaterial(material: MaterialEntity)

    @Update
    suspend fun updateMaterial(material: MaterialEntity)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteMaterial(id: String)

    @Query("SELECT COUNT(*) FROM materials WHERE status = 'READY'")
    fun observeReadyCount(): Flow<Int>
}

data class LabeledRow(val subject: String, val topic: String?, val text: String?)
