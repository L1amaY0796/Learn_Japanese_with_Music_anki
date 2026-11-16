package com.learn_japanese_with_music.core.anki

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.FileProvider
import com.learn_japanese_with_music.core.data.database.SavedWord
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes saved words to an Anki deck package (.apkg): a zip holding a `collection.anki2`
 * SQLite database and a `media` index. Importable by Anki desktop, AnkiDroid and AnkiMobile.
 *
 * All functions do file I/O and must be called off the main thread.
 */
object AnkiExporter {

    const val FILE_NAME = "UtaLearning.apkg"

    /** Sent with the "open in Anki" intent; AnkiDroid registers an import handler for it. */
    const val APKG_MIME_TYPE = "application/apkg"

    // Must match the <cache-path> in res/xml/file_paths.xml.
    private const val SHARE_DIR = "anki_export"
    private const val WORK_DIR = "anki_export_tmp"

    /** Builds the package in the app cache and returns it. */
    fun exportToFile(context: Context, words: List<SavedWord>): File {
        val collection = AnkiDeckBuilder.build(words)

        val workDir = File(context.cacheDir, WORK_DIR)
        workDir.deleteRecursively()
        if (!workDir.mkdirs()) throw IOException("Cannot create ${workDir.path}")

        try {
            val dbFile = File(workDir, "collection.anki2")
            writeCollection(dbFile, collection)

            val shareDir = File(context.cacheDir, SHARE_DIR)
            shareDir.mkdirs()
            val apkg = File(shareDir, FILE_NAME)
            ZipOutputStream(apkg.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("collection.anki2"))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()

                // No audio or images, so the media index is empty.
                zip.putNextEntry(ZipEntry("media"))
                zip.write("{}".toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            return apkg
        } finally {
            workDir.deleteRecursively()
        }
    }

    /** Builds the package and copies it to a location the user picked (Storage Access Framework). */
    fun exportToUri(context: Context, words: List<SavedWord>, destination: Uri) {
        val apkg = exportToFile(context, words)
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: throw IOException("Cannot open $destination")
        output.use { out -> apkg.inputStream().use { it.copyTo(out) } }
    }

    /** An intent that hands the package to an installed Anki app for import. */
    fun createImportIntent(context: Context, apkg: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkg)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APKG_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writeCollection(dbFile: File, collection: AnkiCollection) {
        val db = SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            // NO_LOCALIZED_COLLATORS keeps Android from adding its own android_metadata table.
            SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        try {
            // Keep everything in the single .anki2 file (no -wal side file to lose).
            try {
                db.rawQuery("PRAGMA journal_mode=DELETE", null).use { it.moveToFirst() }
            } catch (e: Exception) {
                // Not fatal: closing the database checkpoints a write-ahead log anyway.
            }

            db.beginTransaction()
            try {
                AnkiDeckBuilder.SCHEMA.forEach { db.execSQL(it) }

                db.execSQL(
                    AnkiDeckBuilder.INSERT_COL,
                    arrayOf<Any>(
                        collection.createdSeconds,
                        collection.modifiedMillis,
                        collection.modifiedMillis,
                        collection.conf,
                        collection.models,
                        collection.decks,
                        collection.dconf
                    )
                )

                val modifiedSeconds = collection.modifiedMillis / 1000
                val insertNote = db.compileStatement(AnkiDeckBuilder.INSERT_NOTE)
                val insertCard = db.compileStatement(AnkiDeckBuilder.INSERT_CARD)
                for (note in collection.notes) {
                    insertNote.clearBindings()
                    insertNote.bindLong(1, note.id)
                    insertNote.bindString(2, note.guid)
                    insertNote.bindLong(3, AnkiDeckBuilder.MODEL_ID)
                    insertNote.bindLong(4, modifiedSeconds)
                    insertNote.bindString(5, note.tags)
                    insertNote.bindString(6, note.fields)
                    insertNote.bindString(7, note.sortField)
                    insertNote.bindLong(8, note.checksum)
                    insertNote.executeInsert()

                    // One card per note; reusing the note id as the card id keeps both unique.
                    insertCard.clearBindings()
                    insertCard.bindLong(1, note.id)
                    insertCard.bindLong(2, note.id)
                    insertCard.bindLong(3, AnkiDeckBuilder.DECK_ID)
                    insertCard.bindLong(4, modifiedSeconds)
                    insertCard.bindLong(5, note.position.toLong())
                    insertCard.executeInsert()
                }
                insertNote.close()
                insertCard.close()

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
        }
    }
}
