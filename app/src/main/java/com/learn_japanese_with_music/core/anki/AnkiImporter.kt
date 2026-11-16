package com.learn_japanese_with_music.core.anki

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.github.luben.zstd.ZstdInputStream
import com.learn_japanese_with_music.core.data.database.SavedWord
import com.learn_japanese_with_music.core.data.database.SavedWordDao
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

data class AnkiImportResult(
    /** Words added to the saved list. */
    val imported: Int,
    /** Notes whose word + sentence is already saved (left untouched). */
    val alreadySaved: Int,
    /** Notes without a usable word. */
    val skipped: Int
)

/**
 * Reads an Anki deck package (.apkg) and adds its notes to the saved words.
 *
 * Handles both package layouts: the legacy one (`collection.anki2` / `.anki21`, plain SQLite)
 * and the current one (`collection.anki21b`, zstd-compressed SQLite). Media and scheduling
 * are ignored; imported words start as new cards.
 */
object AnkiImporter {

    private const val WORK_DIR = "anki_import_tmp"

    // Newest layout first. A current-format package also carries a dummy collection.anki2
    // that only says "please update Anki", so the order matters.
    private val COLLECTION_ENTRIES = listOf("collection.anki21b", "collection.anki21", "collection.anki2")

    private const val MAX_COLLECTION_BYTES = 1024L * 1024 * 1024
    private const val INSERT_CHUNK = 500

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        dao: SavedWordDao,
        splitMode: String
    ): AnkiImportResult {
        val workDir = File(context.cacheDir, WORK_DIR)
        workDir.deleteRecursively()
        if (!workDir.mkdirs()) throw IOException("Cannot create ${workDir.path}")

        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open the selected file")
            val dbFile = input.use { extractCollection(it, workDir) }
            val notes = readNotes(dbFile)

            val knownKeys = dao.getAllSavedWords().mapTo(HashSet()) { AnkiNoteMapper.duplicateKey(it) }
            val newWords = ArrayList<SavedWord>()
            var alreadySaved = 0
            var skipped = 0
            val now = System.currentTimeMillis()

            for (note in notes) {
                val word = AnkiNoteMapper.toSavedWord(note, splitMode, now)
                if (word == null) {
                    skipped++
                } else if (!knownKeys.add(AnkiNoteMapper.duplicateKey(word))) {
                    alreadySaved++
                } else {
                    newWords += word
                }
            }

            newWords.chunked(INSERT_CHUNK).forEach { dao.insertSavedWordsIfAbsent(it) }
            return AnkiImportResult(newWords.size, alreadySaved, skipped)
        } finally {
            workDir.deleteRecursively()
        }
    }

    /** Pulls the best collection database out of the zip and returns it as a plain SQLite file. */
    private fun extractCollection(input: InputStream, workDir: File): File {
        val found = HashMap<String, File>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (name in COLLECTION_ENTRIES) {
                    val target = File(workDir, name)
                    target.outputStream().use { copyLimited(zip, it) }
                    found[name] = target
                    if (name == COLLECTION_ENTRIES.first()) break
                } else if (found.isNotEmpty() && name != "media" && name != "meta") {
                    // Media files follow the collection; no need to read through them.
                    break
                }
            }
        }

        val name = COLLECTION_ENTRIES.firstOrNull { it in found }
            ?: throw IOException("This is not an Anki deck package (.apkg)")
        val file = found.getValue(name)
        if (!name.endsWith("b")) return file

        val plain = File(workDir, "collection.sqlite")
        ZstdInputStream(file.inputStream().buffered()).use { zstd ->
            plain.outputStream().use { copyLimited(zstd, it) }
        }
        return plain
    }

    private fun copyLimited(input: InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_COLLECTION_BYTES) throw IOException("The deck is too large to import")
            output.write(buffer, 0, read)
        }
    }

    private fun readNotes(dbFile: File): List<RawAnkiNote> {
        val db = SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        try {
            val fieldNames: Map<Long, List<String>>
            val deckNames: Map<Long, String>

            if (hasTable(db, "notetypes") && hasTable(db, "fields")) {
                val fieldsByType = HashMap<Long, MutableList<Pair<Int, String>>>()
                db.rawQuery(AnkiNoteMapper.SQL_FIELDS, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        fieldsByType.getOrPut(cursor.getLong(0)) { ArrayList() }
                            .add(cursor.getInt(1) to cursor.getString(2))
                    }
                }
                fieldNames = fieldsByType.mapValues { (_, fields) -> fields.sortedBy { it.first }.map { it.second } }

                val decks = HashMap<Long, String>()
                db.rawQuery(AnkiNoteMapper.SQL_DECKS, null).use { cursor ->
                    while (cursor.moveToNext()) decks[cursor.getLong(0)] = cursor.getString(1)
                }
                deckNames = decks
            } else {
                var models = ""
                var decks = ""
                db.rawQuery(AnkiNoteMapper.SQL_LEGACY_COL, null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        models = cursor.getString(0).orEmpty()
                        decks = cursor.getString(1).orEmpty()
                    }
                }
                fieldNames = AnkiNoteMapper.parseLegacyModels(models)
                deckNames = AnkiNoteMapper.parseLegacyDecks(decks)
            }

            // A note's deck is the deck of its first card.
            val noteDecks = HashMap<Long, Long>()
            db.rawQuery(AnkiNoteMapper.SQL_NOTE_DECKS, null).use { cursor ->
                while (cursor.moveToNext()) noteDecks.putIfAbsent(cursor.getLong(0), cursor.getLong(1))
            }

            val notes = ArrayList<RawAnkiNote>()
            db.rawQuery(AnkiNoteMapper.SQL_NOTES, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    notes += RawAnkiNote(
                        id = id,
                        fieldNames = fieldNames[cursor.getLong(1)].orEmpty(),
                        fields = AnkiNoteMapper.splitFields(cursor.getString(2).orEmpty()),
                        tags = cursor.getString(3).orEmpty(),
                        deckName = noteDecks[id]?.let { deckNames[it] }.orEmpty()
                    )
                }
            }
            return notes
        } finally {
            db.close()
        }
    }

    private fun hasTable(db: SQLiteDatabase, name: String): Boolean =
        db.rawQuery(AnkiNoteMapper.SQL_HAS_TABLE, arrayOf(name)).use { cursor ->
            cursor.moveToFirst() && cursor.getLong(0) > 0
        }
}
