package app.studycapture.data.repo

import app.studycapture.understanding.Summary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StoredPoint(val text: String, val kind: String, val pageId: String, val blockIndex: Int)

@Serializable
data class StoredSection(val title: String?, val points: List<StoredPoint>)

/** Persisted summary. Every point keeps the page and block it was quoted from. */
@Serializable
data class StoredSummary(val sections: List<StoredSection>) {
    val isEmpty: Boolean get() = sections.all { it.points.isEmpty() }
}

object SummaryJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(summary: Summary): String = json.encodeToString(
        StoredSummary.serializer(),
        StoredSummary(
            summary.sections.map { section ->
                StoredSection(
                    section.title,
                    section.points.map { StoredPoint(it.text, it.kind.name, it.source.pageId, it.source.blockIndex) },
                )
            },
        ),
    )

    fun decode(raw: String): StoredSummary? = runCatching { json.decodeFromString(StoredSummary.serializer(), raw) }.getOrNull()
}
