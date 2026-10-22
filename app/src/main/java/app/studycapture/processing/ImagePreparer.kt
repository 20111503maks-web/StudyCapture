package app.studycapture.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * "Cleaning image" stage for one page:
 * decode with EXIF rotation applied, downscale to a memory-safe size and
 * stretch contrast when the photo is dark or washed out.
 * Edge detection and perspective correction happen earlier, in the
 * ML Kit document scanner; gallery imports are used as they are.
 */
class ImagePreparer(private val maxSide: Int = 2400) {

    fun prepare(original: File, processedOut: File): Bitmap {
        val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(original)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
        }
        val cleaned = stretchContrast(decoded)
        if (cleaned !== decoded) decoded.recycle()
        processedOut.outputStream().use { cleaned.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return cleaned
    }

    /** Linear stretch between the 2nd and 98th luminance percentiles; no-op on well-exposed pages. */
    private fun stretchContrast(source: Bitmap): Bitmap {
        val (low, high) = luminancePercentiles(source)
        if (high - low >= GOOD_RANGE || high <= low) return source.ensureArgb()
        val scale = 255f / (high - low)
        val offset = -low * scale
        val matrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(source, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        return out
    }

    private fun luminancePercentiles(bitmap: Bitmap): Pair<Int, Int> {
        val histogram = IntArray(256)
        val step = max(1, max(bitmap.width, bitmap.height) / 256)
        var samples = 0
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val c = bitmap.getPixel(x, y)
                val lum = (0.299 * (c shr 16 and 0xff) + 0.587 * (c shr 8 and 0xff) + 0.114 * (c and 0xff)).toInt()
                histogram[lum]++
                samples++
            }
        }
        fun percentile(p: Double): Int {
            val target = (samples * p).toInt()
            var acc = 0
            for (i in 0..255) {
                acc += histogram[i]
                if (acc >= target) return i
            }
            return 255
        }
        return percentile(0.02) to percentile(0.98)
    }

    private fun Bitmap.ensureArgb(): Bitmap =
        if (config == Bitmap.Config.ARGB_8888) this else copy(Bitmap.Config.ARGB_8888, false)

    private companion object {
        const val GOOD_RANGE = 170
    }
}
