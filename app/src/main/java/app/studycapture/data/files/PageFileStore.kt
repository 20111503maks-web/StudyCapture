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
 * Path getters never create directories, so a late background write after a
 * deletion cannot resurrect a deleted material's folder (writers call [ensureParent]).
 */
class PageFileStore(private val context: Context) {
    private val root: File get() = File(context.filesDir, "pages")

    private fun materialDir(materialId: String) = File(root, materialId)

    fun originalFile(materialId: String, pageId: String) = File(materialDir(materialId), "$pageId-original")

    fun processedFile(materialId: String, pageId: String) = File(materialDir(materialId), "$pageId-processed.jpg")

    fun ensureParent(file: File) {
        file.parentFile?.mkdirs()
    }

    /**
     * Copies [uri] into [target]. Throws IOException for unreadable, empty or oversized input;
     * a partial copy is always removed.
     */
    suspend fun copyIn(uri: Uri, target: File) = withContext(Dispatchers.IO) {
        ensureParent(target)
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open input")
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
            if (total == 0L) throw IOException("File is empty")
        } catch (t: Throwable) {
            target.delete()
            throw t
        }
    }

    /**
     * The document scanner returns file:// URIs inside our cache directory. Once copied,
     * that second copy is deleted so it cannot outlive a page deletion.
     */
    fun deleteScannerOutput(uri: Uri) {
        if (uri.scheme != "file") return
        val file = uri.path?.let(::File) ?: return
        if (file.canonicalPath.startsWith(context.cacheDir.canonicalPath + File.separator)) file.delete()
    }

    fun deleteMaterial(materialId: String) {
        materialDir(materialId).deleteRecursively()
    }

    fun deleteFiles(vararg paths: String?) {
        paths.filterNotNull().forEach { File(it).delete() }
    }

    /**
     * Removes folders of materials that no longer exist (e.g. left by a write racing a deletion).
     * Folders touched in the last hour are kept: an import copies files before its row exists.
     */
    fun deleteOrphans(liveMaterialIds: Set<String>) {
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        root.listFiles()
            ?.filter { it.name !in liveMaterialIds && it.lastModified() < cutoff }
            ?.forEach { it.deleteRecursively() }
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
