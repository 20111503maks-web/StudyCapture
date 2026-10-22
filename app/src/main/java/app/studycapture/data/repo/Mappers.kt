package app.studycapture.data.repo

import app.studycapture.data.db.BlockEntity
import app.studycapture.data.db.DueCardRow
import app.studycapture.data.db.FlashcardEntity
import app.studycapture.model.BBox
import app.studycapture.model.BlockType
import app.studycapture.model.TextBlock
import app.studycapture.srs.CardState
import app.studycapture.srs.SrsCard
import java.time.Instant

fun TextBlock.toEntity(pageId: String) = BlockEntity(
    pageId = pageId,
    blockIndex = index,
    type = type.name,
    text = text,
    confidence = confidence,
    uncertain = uncertain,
    left = box?.left,
    top = box?.top,
    right = box?.right,
    bottom = box?.bottom,
)

fun BlockEntity.toModel() = TextBlock(
    index = blockIndex,
    type = runCatching { BlockType.valueOf(type) }.getOrDefault(BlockType.PARAGRAPH),
    text = text,
    confidence = confidence,
    box = if (left != null && top != null && right != null && bottom != null) BBox(left, top, right, bottom) else null,
    uncertain = uncertain,
)

private fun srsCard(state: Int?, step: Int?, stability: Double?, difficulty: Double?, due: Long?, last: Long?) =
    SrsCard(
        state = CardState.entries.firstOrNull { it.value == state } ?: CardState.Learning,
        step = step,
        stability = stability,
        difficulty = difficulty,
        due = Instant.ofEpochMilli(due ?: 0L),
        lastReview = last?.let(Instant::ofEpochMilli),
    )

fun FlashcardEntity.toSrs() = srsCard(srsState, srsStep, stability, difficulty, dueAt, lastReviewAt)

fun DueCardRow.toSrs() = srsCard(srsState, srsStep, stability, difficulty, dueAt, lastReviewAt)

fun FlashcardEntity.withSrs(card: SrsCard, now: Long) = copy(
    srsState = card.state.value,
    srsStep = card.step,
    stability = card.stability,
    difficulty = card.difficulty,
    dueAt = card.due.toEpochMilli(),
    lastReviewAt = card.lastReview?.toEpochMilli(),
    updatedAt = now,
)
