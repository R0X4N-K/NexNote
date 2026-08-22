package io.github.r0x4nk.nexnote.data.db

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NexNoteDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NexNoteDatabase::class.java
    )

    @Test
    fun migration5To6ValidatesAuthenticSchemaAndPreservesData() {
        val name = "migration-5-6"
        createSeededDatabase(name, 5)

        val migrated = helper.runMigrationsAndValidate(
            name,
            6,
            true,
            NexNoteDatabase.MIGRATION_5_6
        )

        migrated.use {
            assertRepresentativeData(it, expectVaultColumn = false)
            assertEquals(
                setOf("index_notes_isDeleted_isPinned_lastModifiedDate"),
                it.userIndexNames("notes")
            )
            assertFalse("index_notes_sort" in it.userIndexNames("notes"))
            assertTagForeignKey(it)
        }
    }

    @Test
    fun migration6To7AddsNormalDefaultAndPreservesData() {
        val name = "migration-6-7"
        createSeededDatabase(name, 6)

        val migrated = helper.runMigrationsAndValidate(
            name,
            7,
            true,
            NexNoteDatabase.MIGRATION_6_7
        )

        migrated.use {
            assertRepresentativeData(it, expectVaultColumn = true)
            assertEquals(
                setOf("index_notes_isDeleted_isInVault_isPinned_lastModifiedDate"),
                it.userIndexNames("notes")
            )
            assertTagForeignKey(it)
        }
    }

    @Test
    fun migration5To7RunsVerifiedChainAndPreservesData() {
        val name = "migration-5-7"
        createSeededDatabase(name, 5)

        val migrated = helper.runMigrationsAndValidate(
            name,
            7,
            true,
            NexNoteDatabase.MIGRATION_5_6,
            NexNoteDatabase.MIGRATION_6_7
        )

        migrated.use {
            assertRepresentativeData(it, expectVaultColumn = true)
            assertEquals(
                setOf("index_notes_isDeleted_isInVault_isPinned_lastModifiedDate"),
                it.userIndexNames("notes")
            )
            assertTagForeignKey(it)
        }
    }

    @Test
    fun migration7To8AddsEmptyStatisticsIndexAndPreservesData() {
        val name = "migration-7-8"
        createSeededDatabase(name, 7)

        val migrated = helper.runMigrationsAndValidate(
            name,
            8,
            true,
            NexNoteDatabase.MIGRATION_7_8
        )

        migrated.use {
            assertRepresentativeData(it, expectVaultColumn = true)
            assertEquals(0, it.longQuery("SELECT COUNT(*) FROM note_statistics_index"))
            assertEquals(
                setOf("index_note_statistics_index_sourceLastModifiedDate"),
                it.userIndexNames("note_statistics_index")
            )
        }
    }

    @Test
    fun migration5To8RunsCompleteVerifiedChain() {
        val name = "migration-5-8"
        createSeededDatabase(name, 5)

        helper.runMigrationsAndValidate(
            name,
            8,
            true,
            NexNoteDatabase.MIGRATION_5_6,
            NexNoteDatabase.MIGRATION_6_7,
            NexNoteDatabase.MIGRATION_7_8
        ).use {
            assertRepresentativeData(it, expectVaultColumn = true)
            assertEquals(0, it.longQuery("SELECT COUNT(*) FROM note_statistics_index"))
        }
    }

    @Test
    fun migration8To9AddsFilteredSearchAndCoveringTagIndex() {
        val name = "migration-8-9"
        createSeededDatabase(name, 8)

        val migrated = helper.runMigrationsAndValidate(
            name,
            9,
            true,
            NexNoteDatabase.MIGRATION_8_9
        )

        migrated.use {
            assertEquals(1, it.ftsMatchCount("normal*"))
            assertEquals(0, it.ftsMatchCount("deleted*"))
            assertEquals(0, it.ftsMatchCount("encrypted*"))
            assertEquals(
                11L,
                it.longQuery(
                    "SELECT rowid FROM notes_fts WHERE notes_fts MATCH 'normal*'"
                )
            )
            assertEquals(
                setOf("index_note_tag_cross_ref_tagName_noteId"),
                it.userIndexNames("note_tag_cross_ref")
            )
            assertEquals(
                setOf(
                    "index_notes_isDeleted_isInVault_isPinned_lastModifiedDate",
                    "index_notes_active_pinned_modified_asc",
                    "index_notes_isDeleted_isInVault_creationDate"
                ),
                it.userIndexNames("notes")
            )
            assertTrue(it.userIndexNames("note_statistics_index").isEmpty())

            it.execSQL("UPDATE notes SET isDeleted = 1 WHERE id = 11")
            assertEquals(0, it.ftsMatchCount("normal*"))
            it.execSQL("UPDATE notes SET isDeleted = 0 WHERE id = 11")
            assertEquals(1, it.ftsMatchCount("normal*"))
            it.execSQL("UPDATE notes SET isInVault = 1 WHERE id = 11")
            assertEquals(0, it.ftsMatchCount("normal*"))
        }
    }

    @Test
    fun migration5To9RunsCompleteVerifiedChain() {
        val name = "migration-5-9"
        createSeededDatabase(name, 5)

        helper.runMigrationsAndValidate(
            name,
            9,
            true,
            NexNoteDatabase.MIGRATION_5_6,
            NexNoteDatabase.MIGRATION_6_7,
            NexNoteDatabase.MIGRATION_7_8,
            NexNoteDatabase.MIGRATION_8_9
        ).use {
            assertRepresentativeData(it, expectVaultColumn = true)
            assertEquals(1, it.ftsMatchCount("normal*"))
        }
    }

    private fun createSeededDatabase(name: String, version: Int) {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        helper.createDatabase(name, version).use { database ->
            val vaultColumn = if (version >= 7) ", isInVault" else ""
            val vaultPlaceholder = if (version >= 7) ", ?" else ""
            database.execSQL(
                """
                INSERT INTO notes (
                    id, title, content, isMarkdown, creationDate, lastModifiedDate,
                    timezone, isDeleted, deletedDate, isPinned, imagePathsRaw,
                    backgroundColor, isPreviewMode$vaultColumn
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?$vaultPlaceholder)
                """.trimIndent(),
                mutableListOf<Any?>(
                    11L,
                    "Normal title",
                    "Normal body #alpha",
                    1,
                    100L,
                    200L,
                    "Europe/Rome",
                    0,
                    null,
                    1,
                    "[\"images/note_11_img_1.jpg\"]",
                    0x102030,
                    1
                ).apply { if (version >= 7) add(0) }.toTypedArray()
            )
            if (version >= 7) {
                database.execSQL(
                    """
                    INSERT INTO notes (
                        id, title, content, isMarkdown, creationDate, lastModifiedDate,
                        timezone, isDeleted, deletedDate, isPinned, imagePathsRaw,
                        backgroundColor, isPreviewMode, isInVault
                    ) VALUES (13, 'Encrypted title', 'Encrypted Vault body', 1, 500, 600,
                        'UTC', 0, NULL, 0, '', NULL, 0, 1)
                    """.trimIndent()
                )
            }
            database.execSQL(
                """
                INSERT INTO notes (
                    id, title, content, isMarkdown, creationDate, lastModifiedDate,
                    timezone, isDeleted, deletedDate, isPinned, imagePathsRaw,
                    backgroundColor, isPreviewMode$vaultColumn
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?$vaultPlaceholder)
                """.trimIndent(),
                mutableListOf<Any?>(
                    12L,
                    "Deleted title",
                    "Deleted body",
                    0,
                    300L,
                    400L,
                    "UTC",
                    1,
                    450L,
                    0,
                    "[\"images/note_12_img_2.png\"]",
                    null,
                    0
                ).apply { if (version >= 7) add(0) }.toTypedArray()
            )
            database.execSQL(
                """
                INSERT INTO templates (
                    id, name, content, isMarkdown, category, isPredefined, iconName
                ) VALUES (21, 'Template', 'Template body', 1, 'custom', 0, 'note')
                """.trimIndent()
            )
            database.execSQL(
                "INSERT INTO tags (name, createdDate, lastUpdatedDate) " +
                    "VALUES ('alpha', 500, 600)"
            )
            database.execSQL(
                "INSERT INTO note_tag_cross_ref (noteId, tagName) VALUES (11, 'alpha')"
            )
        }
    }

    private fun assertRepresentativeData(
        database: SupportSQLiteDatabase,
        expectVaultColumn: Boolean
    ) {
        database.query(
            "SELECT title, content, isDeleted, deletedDate, imagePathsRaw, " +
                "backgroundColor, isPreviewMode" +
                (if (expectVaultColumn) ", isInVault" else "") +
                " FROM notes WHERE id IN (11, 12) ORDER BY id"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Normal title", cursor.getString(0))
            assertEquals("Normal body #alpha", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
            assertTrue(cursor.isNull(3))
            assertEquals("[\"images/note_11_img_1.jpg\"]", cursor.getString(4))
            assertEquals(0x102030, cursor.getInt(5))
            assertEquals(1, cursor.getInt(6))
            if (expectVaultColumn) assertEquals(0, cursor.getInt(7))

            assertTrue(cursor.moveToNext())
            assertEquals("Deleted title", cursor.getString(0))
            assertEquals(1, cursor.getInt(2))
            assertEquals(450L, cursor.getLong(3))
            assertEquals("[\"images/note_12_img_2.png\"]", cursor.getString(4))
            assertTrue(cursor.isNull(5))
            if (expectVaultColumn) assertEquals(0, cursor.getInt(7))
            assertFalse(cursor.moveToNext())
        }
        assertEquals(1, database.longQuery("SELECT COUNT(*) FROM templates"))
        assertEquals(1, database.longQuery("SELECT COUNT(*) FROM tags"))
        assertEquals(1, database.longQuery("SELECT COUNT(*) FROM note_tag_cross_ref"))
    }

    private fun assertTagForeignKey(database: SupportSQLiteDatabase) {
        database.query("PRAGMA foreign_key_list(`note_tag_cross_ref`)").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("notes", cursor.getString(cursor.getColumnIndexOrThrow("table")))
            assertEquals("noteId", cursor.getString(cursor.getColumnIndexOrThrow("from")))
            assertEquals("id", cursor.getString(cursor.getColumnIndexOrThrow("to")))
            assertEquals("CASCADE", cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
        }
    }

    private fun SupportSQLiteDatabase.userIndexNames(table: String): Set<String> =
        query("PRAGMA index_list(`$table`)").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (!name.startsWith("sqlite_autoindex_")) add(name)
                }
            }
        }

    private fun SupportSQLiteDatabase.longQuery(sql: String): Long =
        query(sql).use { cursor -> cursor.singleLong() }

    private fun SupportSQLiteDatabase.ftsMatchCount(matchQuery: String): Long =
        query("SELECT COUNT(*) FROM notes_fts WHERE notes_fts MATCH ?", arrayOf(matchQuery))
            .use { cursor -> cursor.singleLong() }

    private fun Cursor.singleLong(): Long {
        assertTrue(moveToFirst())
        return getLong(0)
    }
}
