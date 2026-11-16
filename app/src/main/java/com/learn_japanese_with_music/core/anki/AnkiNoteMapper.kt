package com.learn_japanese_with_music.core.anki

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.learn_japanese_with_music.core.data.database.SavedWord

/** A note as read from an Anki collection, with the field names of its note type. */
data class RawAnkiNote(
    val id: Long,
    val fieldNames: List<String>,
    val fields: List<String>,
    val tags: String,
    val deckName: String
)

/**
 * Turns Anki notes into saved words.
 *
 * Notes exported by this app ([AnkiDeckBuilder]) map back field by field. Notes from any other
 * deck are mapped by guessing from the field names (Expression / Reading / Meaning / ...).
 *
 * Plain Kotlin on purpose (no Android classes), like [AnkiDeckBuilder]; reading the .apkg file
 * lives in [AnkiImporter].
 */
object AnkiNoteMapper {

    // Queries shared by both collection layouts.
    const val SQL_NOTES = "SELECT id, mid, flds, tags FROM notes"
    const val SQL_NOTE_DECKS = "SELECT nid, did FROM cards"
    const val SQL_HAS_TABLE = "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = ?"

    // Legacy layout (collection.anki2 / .anki21): note types and decks are JSON in `col`.
    const val SQL_LEGACY_COL = "SELECT models, decks FROM col"

    // Current layout (collection.anki21b): they have their own tables. NOT INDEXED avoids the
    // name indexes, which use Anki's custom "unicase" collation that plain SQLite does not have.
    const val SQL_FIELDS = "SELECT ntid, ord, name FROM fields NOT INDEXED"
    const val SQL_DECKS = "SELECT id, name FROM decks NOT INDEXED"

    private const val FIELD_SEPARATOR = "\u001f"

    private val WORD_NAMES = listOf("word", "expression", "vocabulary", "vocab", "term", "単語", "表面", "kanji", "japanese", "front")
    private val READING_NAMES = listOf("reading", "kana", "furigana", "hiragana", "読み", "よみ", "pronunciation")
    private val MEANING_NAMES = listOf("meaning", "definition", "glossary", "translation", "english", "意味", "中文", "chinese", "裏面", "back")
    private val SENTENCE_NAMES = listOf("sentence", "example", "context", "例文")

    private val JLPT_TAG = Regex("""(?i)(?:^|.*[:_\-])N([1-5])""")
    private val FURIGANA = Regex(""" ?([^\s\[\]]+)\[[^\]]*]""")

    private val gson = Gson()

    /** Field names per note type id, from the legacy `col.models` JSON. */
    fun parseLegacyModels(modelsJson: String): Map<Long, List<String>> {
        val models = parseObject(modelsJson) ?: return emptyMap()
        val result = HashMap<Long, List<String>>()
        for ((key, value) in models.entrySet()) {
            val id = key.toLongOrNull() ?: continue
            if (!value.isJsonObject) continue
            val fields = value.asJsonObject.get("flds")
            if (fields == null || !fields.isJsonArray) continue
            result[id] = fields.asJsonArray
                .filter { it.isJsonObject }
                .map { it.asJsonObject }
                .sortedBy { field -> field.get("ord")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0 }
                .map { field -> field.get("name")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty() }
        }
        return result
    }

    /** Deck names per deck id, from the legacy `col.decks` JSON. */
    fun parseLegacyDecks(decksJson: String): Map<Long, String> {
        val decks = parseObject(decksJson) ?: return emptyMap()
        val result = HashMap<Long, String>()
        for ((key, value) in decks.entrySet()) {
            val id = key.toLongOrNull() ?: continue
            if (!value.isJsonObject) continue
            val name = value.asJsonObject.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            result[id] = name
        }
        return result
    }

    fun splitFields(flds: String): List<String> = flds.split(FIELD_SEPARATOR)

    /** The identity used to decide whether a word is already saved. */
    fun duplicateKey(word: SavedWord): String = word.word.trim() + FIELD_SEPARATOR + word.contextLine.trim()

    /** Returns null when the note has no usable word. */
    fun toSavedWord(note: RawAnkiNote, splitMode: String, nowMillis: Long = System.currentTimeMillis()): SavedWord? {
        val names = note.fieldNames.map { normalize(it) }
        val isOwnFormat = listOf("Word", "Sentence", "ContextMeaning", "Examples").all { normalize(it) in names }

        val used = HashSet<Int>()
        fun pick(candidates: List<String>): Int? {
            val index = findField(names, candidates, used) ?: return null
            used += index
            return index
        }
        fun html(index: Int?): String = index?.let { note.fields.getOrNull(it) }.orEmpty()
        fun text(index: Int?): String = htmlToText(html(index))
        fun named(name: String): Int? = names.indexOf(normalize(name)).takeIf { it >= 0 }

        // The first field is Anki's own notion of "the front" when nothing matches by name.
        val wordIndex = pick(WORD_NAMES) ?: 0.also { used += 0 }
        val readingIndex = pick(READING_NAMES)
        val sentenceIndex = pick(SENTENCE_NAMES)
        val meaningIndex = pick(MEANING_NAMES)
            ?: (0 until note.fields.size).firstOrNull { it !in used }?.also { used += it }

        var word = text(wordIndex)
        var sentence = text(sentenceIndex)
        if (!isOwnFormat) {
            // Other decks often write readings inline as 漢字[かんじ].
            word = FURIGANA.replace(word, "$1").trim()
            sentence = FURIGANA.replace(sentence, "$1").trim()
        }
        word = word.replace("\n", " ").trim()
        sentence = sentence.replace("\n", " ").trim()
        if (word.isEmpty()) return null

        val tags = note.tags.split(' ').filter { it.isNotBlank() }
        val jlpt = text(named("JLPT")).uppercase().takeIf { Regex("N[1-5]").matches(it) }
            ?: tags.firstNotNullOfOrNull { tag -> JLPT_TAG.matchEntire(tag)?.let { "N" + it.groupValues[1] } }
            ?: "unknown"

        val partOfSpeech = JsonArray()
        text(named("PartOfSpeech")).split('・', '/', ',').map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { partOfSpeech.add(it) }

        val deckName = note.deckName.split("::", FIELD_SEPARATOR).last().trim()
        val song = text(named("Song")).ifEmpty { if (isOwnFormat) "Unknown" else deckName.ifEmpty { "Anki" } }
        val artist = text(named("Artist")).ifEmpty { if (isOwnFormat) "Unknown" else "Anki" }

        return SavedWord(
            word = word,
            contextLine = sentence,
            splitMode = splitMode,
            reading = text(readingIndex).replace("\n", " "),
            dictionaryForm = text(named("DictionaryForm")).ifEmpty { word },
            partOfSpeech = partOfSpeech.toString(),
            jlptLevel = jlpt,
            generalMeaning = text(meaningIndex),
            contextualMeaning = text(named("ContextMeaning")),
            grammarExplanation = text(named("Grammar")),
            commonUsages = parseExamples(html(named("Examples"))),
            notes = text(named("Notes")),
            songTitle = song,
            songArtist = artist,
            // Anki note ids are creation times in milliseconds; keep them so the order survives.
            timestamp = if (note.id in 1_000_000_000_000L..nowMillis) note.id else nowMillis
        )
    }

    /** Exact name match first, then "name contains"; candidates are in priority order. */
    private fun findField(names: List<String>, candidates: List<String>, used: Set<Int>): Int? {
        for (candidate in candidates) {
            val index = names.indices.firstOrNull { it !in used && names[it] == candidate }
            if (index != null) return index
        }
        for (candidate in candidates) {
            val index = names.indices.firstOrNull { it !in used && names[it].contains(candidate) }
            if (index != null) return index
        }
        return null
    }

    private fun normalize(name: String): String = name.lowercase().replace(Regex("""[\s_\-]"""), "")

    /** Reverses [AnkiDeckBuilder]'s example markup into the JSON list stored in [SavedWord.commonUsages]. */
    private fun parseExamples(html: String): String {
        val usages = JsonArray()
        fun part(block: String, cssClass: String): String =
            Regex("""(?is)<div class="$cssClass">(.*?)</div>""").find(block)?.let { htmlToText(it.groupValues[1]) }.orEmpty()

        for (block in html.split("<div class=\"ex\">").drop(1)) {
            val japanese = part(block, "ex-jp")
            val translation = part(block, "ex-tr")
            if (japanese.isEmpty() && translation.isEmpty()) continue
            usages.add(JsonObject().apply {
                addProperty("japanese", japanese)
                addProperty("reading", part(block, "ex-reading"))
                addProperty("translation", translation)
            })
        }

        // Edited by hand in Anki into something else: keep the text rather than lose it.
        if (usages.size() == 0) {
            val plain = htmlToText(html)
            if (plain.isNotEmpty()) {
                usages.add(JsonObject().apply {
                    addProperty("japanese", plain)
                    addProperty("reading", "")
                    addProperty("translation", "")
                })
            }
        }
        return usages.toString()
    }

    /** Anki fields are HTML; the app stores plain text. */
    fun htmlToText(html: String): String {
        if (html.isEmpty()) return ""
        var text = html.replace(Regex("""(?is)<(style|script)\b.*?</\1>"""), "")
        text = text.replace(Regex("""\[sound:[^\]]*]"""), "")
        text = text.replace(Regex("""(?i)<br\s*/?>|</(div|p|li|tr|h[1-6])>"""), "\n")
        text = text.replace(Regex("""<[^>]+>"""), "")
        text = decodeEntities(text)
        return text.lines().joinToString("\n") { it.trim() }
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        val numeric = Regex("""&#(x[0-9a-fA-F]+|\d+);""").replace(text) { match ->
            val code = match.groupValues[1]
            val value = if (code.startsWith("x")) code.drop(1).toIntOrNull(16) else code.toIntOrNull()
            if (value != null && Character.isValidCodePoint(value)) String(Character.toChars(value)) else match.value
        }
        return numeric
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun parseObject(json: String): JsonObject? = try {
        gson.fromJson(json, JsonObject::class.java)
    } catch (e: Exception) {
        null
    }
}
