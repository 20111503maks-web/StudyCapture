package app.studycapture.data.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Page images live in app-private storage (no other app can read them).
 * Originals are copied in once and never modified.
 */
class PageFileStore(private val context: Context) {
    private val root: File get() = File(context.filesDir, "pages")

    private fun materialDir(materialId: String) = File(root, materialId).apply { mkdirs() }

    fun originalFile(materialId: String, pageId: String) = File(materialDir(materialId), "$pageId-original")

    fun processedFile(materialId: String, pageId: String) = File(materialDir(materialId), "$pageId-processed.jpg")

    /** Copies [uri] into [target]. Throws IOException for unreadable, empty or oversized input. */
    suspend fun copyIn(uri: Uri, target: File) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        var total = 0L
        input.use { stream ->
            target.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) throw IOException("File is larger than 40 MB")
                    out.write(buffer, 0, read)
                }
            }
        }
        if (total == 0L) {
            target.delete()
            throw IOException("File is empty")
        }
    }

    fun deleteMaterial(materialId: String) {
        File(root, materialId).deleteRecursively()
    }

    fun deleteFiles(vararg paths: String?) {
        paths.filterNotNull().forEach { File(it).delete() }
    }

    fun deleteAll() {
        root.deleteRecursively()
    }

    fun usageBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun originalCount(): Int = root.walkTopDown().count { it.isFile && it.name.endsWith("-original") }

    private companion object {
        const val MAX_IMAGE_BYTES = 40L * 1024 * 1024
    }
}
