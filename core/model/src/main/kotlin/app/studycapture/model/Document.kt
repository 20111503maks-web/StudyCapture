package app.studycapture.model

/** Pixel rectangle on the processed page image. */
data class BBox(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** One line as returned by an OCR engine, before any structure is detected. */
data class OcrLine(
    val text: String,
    /** 0..1, or null when the engine does not report confidence. */
    val confidence: Float?,
    val box: BBox?,
)

enum class BlockType {
    HEADING,
    SUBHEADING,
    PARAGRAPH,
    DEFINITION,
    FORMULA,
    LIST_ITEM,
    TABLE_ROW,
    EXAMPLE,
    TASK,
    CAPTION,
    HIGHLIGHT,
}

/**
 * A structural unit of a page. [text] is always exactly what OCR (or the user)
 * produced — understanding code may classify it but never rewrites it.
 */
data class TextBlock(
    val index: Int,
    val type: BlockType,
    val text: String,
    val confidence: Float?,
    val box: BBox?,
    /** True when OCR was unsure; the UI must offer a comparison with the photo. */
    val uncertain: Boolean,
)

/** Points at the exact block a statement came from. */
data class SourceRef(val pageId: String, val blockIndex: Int)

/** Where a piece of generated content comes from. Shown to the user, always. */
enum class Provenance {
    /** Text copied verbatim from the user's pages. */
    MATERIAL,
    /** A model rephrasing or explaining material. */
    AI_EXPLANATION,
    /** Knowledge that is not in the user's pages. */
    AI_EXTRA,
}
