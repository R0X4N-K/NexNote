package io.github.r0x4nk.nexnote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity

@Database(
    entities = [NoteEntity::class, TemplateEntity::class, TagEntity::class, NoteTagCrossRef::class],
    version  = 7,
    exportSchema = true
)
abstract class NexNoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun noteContentPatchDao(): NoteContentPatchDao
    abstract fun templateDao(): TemplateDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: NexNoteDatabase? = null

        /**
         * Normalises the notes sort index name to Room's entity-derived name.
         * Some older migrations created the same index under `index_notes_sort`;
         * keeping only the canonical name lets Room validate migrated schemas.
         */
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notes_isDeleted_isPinned_lastModifiedDate " +
                        "ON notes (isDeleted, isPinned, lastModifiedDate)"
                )
                db.execSQL("DROP INDEX IF EXISTS index_notes_sort")
            }
        }

        /**
         * Adds the Vault membership flag. Existing notes remain normal notes.
         * The sort index is rebuilt with isInVault in the filtered prefix so
         * normal-note queries can exclude Vault rows efficiently.
         */
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isInVault INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notes_isDeleted_isInVault_isPinned_lastModifiedDate " +
                        "ON notes (isDeleted, isInVault, isPinned, lastModifiedDate)"
                )
                db.execSQL("DROP INDEX IF EXISTS index_notes_isDeleted_isPinned_lastModifiedDate")
            }
        }

        fun getDatabase(context: Context): NexNoteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): NexNoteDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NexNoteDatabase::class.java,
                "nexnote.db"
            )
                .addMigrations(
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .build()
    }
}
