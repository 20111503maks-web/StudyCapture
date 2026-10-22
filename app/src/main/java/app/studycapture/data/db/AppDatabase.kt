package app.studycapture.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubjectEntity::class,
        TopicEntity::class,
        MaterialEntity::class,
        PageEntity::class,
        BlockEntity::class,
        BlockFts::class,
        FlashcardEntity::class,
        ReviewLogEntity::class,
        SummaryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun pageDao(): PageDao
    abstract fun studyDao(): StudyDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "studycapture.db")
                // Schema changes must ship with a Migration; never wipe user notes.
                .build()
    }
}
