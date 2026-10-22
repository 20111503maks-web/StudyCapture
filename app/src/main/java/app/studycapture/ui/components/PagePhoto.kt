package app.studycapture.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.studycapture.R
import app.studycapture.model.BBox
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Image request keyed by file and version: a retried page rewrites the same file,
 * so the memory cache must not keep showing the old image.
 */
@Composable
fun pageImageRequest(path: String, version: Long): ImageRequest =
    ImageRequest.Builder(LocalPlatformContext.current)
        .data(File(path))
        .memoryCacheKey("$path@$version")
        .build()

/**
 * The page photo, fitted into its box, with an optional frame around the
 * region a block was read from — so the user can compare text with the original.
 */
@Composable
fun PagePhoto(path: String?, highlight: BBox?, modifier: Modifier = Modifier, version: Long = 0L) {
    val amber = Color(0xFFF2A93B)
    val imageSize = produceState<Pair<Int, Int>?>(null, path) {
        value = path?.let {
            withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(it, opts)
                if (opts.outWidth > 0) opts.outWidth to opts.outHeight else null
            }
        }
    }.value

    BoxWithConstraints(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(Color(0xFF2A2D33))) {
        if (path == null) return@BoxWithConstraints
        AsyncImage(
            model = pageImageRequest(path, version),
            contentDescription = stringResource(R.string.check_original_photo),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(8.dp),
        )
        val size = imageSize
        if (highlight != null && size != null) {
            val density = LocalDensity.current
            val boxW = with(density) { maxWidth.toPx() } - with(density) { 16.dp.toPx() }
            val boxH = with(density) { maxHeight.toPx() } - with(density) { 16.dp.toPx() }
            val scale = minOf(boxW / size.first, boxH / size.second)
            val left = with(density) { 8.dp.toPx() } + (boxW - size.first * scale) / 2
            val top = with(density) { 8.dp.toPx() } + (boxH - size.second * scale) / 2
            Canvas(Modifier.fillMaxSize()) {
                val topLeft = Offset(left + highlight.left * scale - 4, top + highlight.top * scale - 4)
                val rect = Size(highlight.width * scale + 8, highlight.height * scale + 8)
                drawRoundRect(amber.copy(alpha = 0.18f), topLeft, rect, CornerRadius(8f))
                drawRoundRect(amber, topLeft, rect, CornerRadius(8f), style = Stroke(width = 5f))
            }
        }
    }
}
