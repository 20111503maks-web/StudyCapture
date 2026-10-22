package app.studycapture.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

// IDs are UUID strings and every row carries updatedAt so a future sync layer
// can merge changes from several devices (see ARCHITECTURE.md, "Sync readiness").

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "topics",
    foreignKeys = [ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subjectId")],
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class SourceType { CAMERA, GALLERY, TEXT }

enum class MaterialStatus { PROCESSING, NEEDS_REVIEW, READY, FAILED }

@Entity(
    tableName = "materials",
    foreignKeys = [ForeignKey(entity = TopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("topicId")],
)
data class MaterialEntity(
    @PrimaryKey val id: String,
    val topicId: String?,
    val title: String,
    val sourceType: SourceType,
    val status: MaterialStatus,
    /** Subject suggested by the on-device classifier; the user confirms it on save. */
    val suggestedSubject: String? = null,
    val suggestedTopic: String? = null,
    val suggestionKeywords: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Real processing stages. The UI only ever shows what is stored here. */
enum class PageStage { CAPTURED, CLEANING, READING, UNDERSTANDING, READY, FAILED }

@Entity(
    tableName = "pages",
    foreignKeys = [ForeignKey(entity = MaterialEntity::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("materialId")],
)
data class PageEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val pageIndex: Int,
    /** The untouched original. Never deleted unless the user deletes the page. */
    val originalPath: String?,
    val processedPath: String?,
    val stage: PageStage,
    val language: String?,
    val ocrEngine: String?,
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "blocks",
    foreignKeys = [ForeignKey(entity = PageEntity::class, parentColumns = ["id"], childColumns = ["pageId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["pageId", "blockIndex"], unique = true)],
)
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val pageId: String,
    val blockIndex: Int,
    val type: String,
    val text: String,
    val confidence: Float?,
    val uncertain: Boolean,
    val left: Int?,
    val top: Int?,
    val right: Int?,
    val bottom: Int?,
    val editedByUser: Boolean = false,
)

/** Offline full-text index over block text. unicode61 folds case for Cyrillic too. */
@Fts4(contentEntity = BlockEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "blocks_fts")
data class BlockFts(val text: String)

enum class CardStatus { SUGGESTED, ACTIVE, DISCARDED }

@Entity(
    tableName = "flashcards",
    foreignKeys = [ForeignKey(entity = MaterialEntity::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("materialId"), Index("status", "dueAt")],
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val pageId: String?,
    val blockIndex: Int?,
    val front: String,
    val back: String,
    val kind: String,
    val status: CardStatus,
    /** FSRS state, see core/srs. Null until the card is approved. */
    val srsState: Int?,
    val srsStep: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val dueAt: Long?,
    val lastReviewAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "review_logs",
    foreignKeys = [ForeignKey(entity = FlashcardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cardId"), Index("reviewedAt")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val rating: Int,
    val reviewedAt: Long,
    val durationMs: Long?,
    val stateBefore: Int?,
    val stabilityAfter: Double?,
    val difficultyAfter: Double?,
    val dueAfter: Long,
)

/** Generated summary stored as JSON (see data/repo/SummaryJson.kt). */
@Entity(
    tableName = "summaries",
    foreignKeys = [ForeignKey(entity = MaterialEntity::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.CASCADE)],
)
data class SummaryEntity(
    @PrimaryKey val materialId: String,
    val json: String,
    val engine: String,
    val createdAt: Long,
)
