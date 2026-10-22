package app.studycapture.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import app.studycapture.model.BBox
import app.studycapture.model.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Provider seam for text recognition. Any engine (on-device or, later, a
 * cloud vision model behind user consent) returns plain lines with confidence.
 */
interface OcrEngine {
    val id: String
    suspend fun recognize(bitmap: Bitmap): List<OcrLine>
}

internal fun Rect.toBBox() = BBox(left, top, right, bottom)

/** Google ML Kit on-device recognizer. Latin script only; bundled model, works offline. */
class MlKitLatinOcr : OcrEngine {
    override val id = "mlkit-latin"
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognize(bitmap: Bitmap): List<OcrLine> {
        val text = client.process(InputImage.fromBitmap(bitmap, 0)).await()
        return text.textBlocks.flatMap { block ->
            block.lines.map { line ->
                OcrLine(text = line.text, confidence = line.confidence, box = line.boundingBox?.toBBox())
            }
        }
    }
}
