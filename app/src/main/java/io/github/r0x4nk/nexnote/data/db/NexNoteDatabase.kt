package io.github.r0x4nk.nexnote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteSearchFtsEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteStatisticsIndexEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity

@Database(
    entities = [
        NoteEntity::class,
        TemplateEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        NoteStatisticsIndexEntity::class,
        NoteSearchFtsEntity::class
    ],
    version  = 9,
    exportSchema = true
)
abstract class NexNoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun homeNoteDao(): HomeNoteDao
    abstract fun noteContentPatchDao(): NoteContentPatchDao
    abstract fun noteStatisticsDao(): NoteStatisticsDao
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

        /** Adds the derived per-note statistics index; existing notes are indexed in background. */
        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_statistics_index (
                        noteId INTEGER NOT NULL,
                        creationDate INTEGER NOT NULL,
                        sourceLastModifiedDate INTEGER NOT NULL,
                        characterCount INTEGER NOT NULL,
                        wordCount INTEGER NOT NULL,
                        tagNamesRaw TEXT NOT NULL,
                        formatVersion INTEGER NOT NULL,
                        PRIMARY KEY(noteId),
                        FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_note_statistics_index_sourceLastModifiedDate " +
                        "ON note_statistics_index (sourceLastModifiedDate)"
                )
            }
        }

        /** Adds full-text note search and the covering tag-filter index. */
        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // The exact virtual-table declaration is aligned with the Room FTS entity.
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(" +
                        "`title` TEXT NOT NULL, `content` TEXT NOT NULL, " +
                        "tokenize=unicode61 `remove_diacritics=0`, " +
                        "content=`notes`, prefix=`2,3`)"
                )
                db.execSQL(
                    "INSERT INTO notes_fts(rowid, title, content) " +
                        "SELECT id, title, content FROM notes " +
                        "WHERE isDeleted = 0 AND isInVault = 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_note_tag_cross_ref_tagName_noteId " +
                        "ON note_tag_cross_ref (tagName, noteId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notes_active_pinned_modified_asc " +
                        "ON notes (isDeleted ASC, isInVault ASC, isPinned DESC, " +
                        "lastModifiedDate ASC)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notes_isDeleted_isInVault_creationDate " +
                        "ON notes (isDeleted, isInVault, creationDate)"
                )
                db.execSQL("DROP INDEX IF EXISTS index_note_tag_cross_ref_tagName")
                db.execSQL("DROP INDEX IF EXISTS index_note_statistics_index_sourceLastModifiedDate")
                installNoteSearchSyncTriggers(db)
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
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                .addCallback(NOTE_SEARCH_SYNC_CALLBACK)
                .build()

        /** Installs the filtered FTS triggers for new, migrated, and test databases. */
        internal val NOTE_SEARCH_SYNC_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                installNoteSearchSyncTriggers(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                installNoteSearchSyncTriggers(db)
            }
        }

        /** Replaces Room's broad content-sync triggers with active normal-note filtering. */
        private fun installNoteSearchSyncTriggers(db: SupportSQLiteDatabase) {
            NOTE_SEARCH_TRIGGER_NAMES.forEach { name ->
                db.execSQL("DROP TRIGGER IF EXISTS `$name`")
            }
            db.execSQL(
                """
                CREATE TRIGGER `${NOTE_SEARCH_TRIGGER_NAMES[0]}`
                BEFORE UPDATE OF title, content, isDeleted, isInVault ON `notes`
                WHEN OLD.isDeleted = 0 AND OLD.isInVault = 0
                BEGIN
                    DELETE FROM `notes_fts` WHERE `docid` = OLD.`id`;
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER `${NOTE_SEARCH_TRIGGER_NAMES[1]}`
                BEFORE DELETE ON `notes`
                WHEN OLD.isDeleted = 0 AND OLD.isInVault = 0
                BEGIN
                    DELETE FROM `notes_fts` WHERE `docid` = OLD.`id`;
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER `${NOTE_SEARCH_TRIGGER_NAMES[2]}`
                AFTER UPDATE OF title, content, isDeleted, isInVault ON `notes`
                WHEN NEW.isDeleted = 0 AND NEW.isInVault = 0
                BEGIN
                    INSERT INTO `notes_fts`(`docid`, `title`, `content`)
                    VALUES (NEW.`id`, NEW.`title`, NEW.`content`);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER `${NOTE_SEARCH_TRIGGER_NAMES[3]}`
                AFTER INSERT ON `notes`
                WHEN NEW.isDeleted = 0 AND NEW.isInVault = 0
                BEGIN
                    INSERT INTO `notes_fts`(`docid`, `title`, `content`)
                    VALUES (NEW.`id`, NEW.`title`, NEW.`content`);
                END
                """.trimIndent()
            )
        }

        private val NOTE_SEARCH_TRIGGER_NAMES = listOf(
            "room_fts_content_sync_notes_fts_BEFORE_UPDATE",
            "room_fts_content_sync_notes_fts_BEFORE_DELETE",
            "room_fts_content_sync_notes_fts_AFTER_UPDATE",
            "room_fts_content_sync_notes_fts_AFTER_INSERT"
        )
    }
}
