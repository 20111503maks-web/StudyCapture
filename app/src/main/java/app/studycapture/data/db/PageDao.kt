package app.studycapture.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SearchHitRow(
    val pageId: String,
    val blockIndex: Int,
    val materialId: String,
    val materialTitle: String,
    val pageIndex: Int,
    val subjectName: String?,
    val snippet: String,
)

@Dao
interface PageDao {
    @Insert
    suspend fun insertPages(pages: List<PageEntity>)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("SELECT * FROM pages WHERE materialId = :materialId ORDER BY pageIndex")
    suspend fun pages(materialId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE materialId = :materialId ORDER BY pageIndex")
    fun observePages(materialId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun page(id: String): PageEntity?

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deletePage(id: String)

    @Query("SELECT * FROM blocks WHERE pageId = :pageId ORDER BY blockIndex")
    fun observeBlocks(pageId: String): Flow<List<BlockEntity>>

    @Query(
        """
        SELECT b.* FROM blocks b JOIN pages p ON b.pageId = p.id
        WHERE p.materialId = :materialId ORDER BY p.pageIndex, b.blockIndex
        """,
    )
    suspend fun blocksForMaterial(materialId: String): List<BlockEntity>

    @Query(
        """
        SELECT b.* FROM blocks b JOIN pages p ON b.pageId = p.id
        WHERE p.materialId = :materialId ORDER BY p.pageIndex, b.blockIndex
        """,
    )
    fun observeBlocksForMaterial(materialId: String): Flow<List<BlockEntity>>

    @Insert
    suspend fun insertBlocks(blocks: List<BlockEntity>)

    @Query("DELETE FROM blocks WHERE pageId = :pageId")
    suspend fun deleteBlocks(pageId: String)

    @Transaction
    suspend fun replaceBlocks(pageId: String, blocks: List<BlockEntity>) {
        deleteBlocks(pageId)
        insertBlocks(blocks)
    }

    @Query("UPDATE blocks SET text = :text, uncertain = 0, editedByUser = 1 WHERE pageId = :pageId AND blockIndex = :blockIndex")
    suspend fun editBlock(pageId: String, blockIndex: Int, text: String)

    @Query("UPDATE blocks SET uncertain = 0 WHERE pageId = :pageId AND blockIndex = :blockIndex")
    suspend fun confirmBlock(pageId: String, blockIndex: Int)

    /** [match] must come from SearchQueryParser, which strips FTS operators. */
    @Query(
        """
        SELECT b.pageId, b.blockIndex, p.materialId, m.title AS materialTitle, p.pageIndex,
          s.name AS subjectName,
          snippet(blocks_fts, '⟪', '⟫', '…', -1, 12) AS snippet
        FROM blocks_fts
        JOIN blocks b ON b.rowId = blocks_fts.rowid
        JOIN pages p ON p.id = b.pageId
        JOIN materials m ON m.id = p.materialId
        LEFT JOIN topics t ON t.id = m.topicId
        LEFT JOIN subjects s ON s.id = t.subjectId
        WHERE blocks_fts MATCH :match AND m.status = 'READY'
        ORDER BY m.createdAt DESC, p.pageIndex, b.blockIndex
        LIMIT 60
        """,
    )
    suspend fun search(match: String): List<SearchHitRow>
}
