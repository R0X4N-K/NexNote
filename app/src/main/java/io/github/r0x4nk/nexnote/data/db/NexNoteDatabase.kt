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
         * Adds the composite index on (isDeleted, isPinned, lastModifiedDate) that
         * was introduced in version 2. Adding an index never changes data, so no
         * rows need to be migrated.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notes_isDeleted_isPinned_lastModifiedDate " +
                        "ON notes (isDeleted, isPinned, lastModifiedDate)"
                )
            }
        }

        /**
         * Adds the nullable INTEGER column [backgroundColor] for per-note background
         * colors. Existing rows receive NULL (no custom color) automatically.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN backgroundColor INTEGER DEFAULT NULL")
            }
        }

        /**
         * Introduces the tag system: creates the `tags` table and the
         * `note_tag_cross_ref` join table with a cascade-delete foreign key on
         * the note side.
         *
         * Existing notes are NOT retroactively indexed — tags will be populated
         * the first time each note is opened and saved in the editor. This avoids
         * a potentially long migration on large note databases.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags (
                        name            TEXT    NOT NULL PRIMARY KEY,
                        createdDate     INTEGER NOT NULL,
                        lastUpdatedDate INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_tag_cross_ref (
                        noteId  INTEGER NOT NULL,
                        tagName TEXT    NOT NULL,
                        PRIMARY KEY (noteId, tagName),
                        FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_note_tag_cross_ref_tagName " +
                        "ON note_tag_cross_ref (tagName)"
                )
            }
        }

        /**
         * Adds the [isPreviewMode] column that remembers whether a note was last
         * viewed in Markdown preview mode. Existing rows default to 0 (edit mode).
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN isPreviewMode INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Normalises the notes sort index name to Room's entity-derived name.
         * Some older migrations created the same index under `index_notes_sort`;
         * keeping only the canonical name lets Room validate migrated schemas.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
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
        private val MIGRATION_6_7 = object : Migration(6, 7) {
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
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .build()
    }
}
