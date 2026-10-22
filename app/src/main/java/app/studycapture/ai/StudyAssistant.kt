package app.studycapture.ai

import app.studycapture.understanding.CardSuggestion
import app.studycapture.understanding.ExtractiveSummarizer
import app.studycapture.understanding.FlashcardGenerator
import app.studycapture.understanding.LabeledText
import app.studycapture.understanding.PageText
import app.studycapture.understanding.SubjectClassifier
import app.studycapture.understanding.SubjectSuggestion
import app.studycapture.understanding.Summary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the UI may say about the engine that produced a result. */
data class EngineInfo(
    val id: String,
    val onDevice: Boolean,
    val requiresNetwork: Boolean,
    /** True when outputs only quote the user's pages (no generated sentences). */
    val extractiveOnly: Boolean,
)

/**
 * Provider seam for study features. v0.1 ships [OnDeviceStudyAssistant];
 * an on-device LLM or a consented cloud model can implement the same
 * interface later without touching screens (see docs/adr/0002).
 */
interface StudyAssistant {
    val engine: EngineInfo
    suspend fun summarize(pages: List<PageText>): Summary
    suspend fun suggestFlashcards(pages: List<PageText>): List<CardSuggestion>
    suspend fun suggestSubject(text: String, history: List<LabeledText>): SubjectSuggestion?
}

/** Free, private, offline. Built on core/understanding: it selects and quotes, it never invents. */
class OnDeviceStudyAssistant : StudyAssistant {
    override val engine = EngineInfo(
        id = "on-device-extractive-v1",
        onDevice = true,
        requiresNetwork = false,
        extractiveOnly = true,
    )

    override suspend fun summarize(pages: List<PageText>): Summary =
        withContext(Dispatchers.Default) { ExtractiveSummarizer().summarize(pages) }

    override suspend fun suggestFlashcards(pages: List<PageText>): List<CardSuggestion> =
        withContext(Dispatchers.Default) { FlashcardGenerator().suggest(pages) }

    override suspend fun suggestSubject(text: String, history: List<LabeledText>): SubjectSuggestion? =
        withContext(Dispatchers.Default) { SubjectClassifier(history).suggest(text, topN = 1).firstOrNull() }
}
