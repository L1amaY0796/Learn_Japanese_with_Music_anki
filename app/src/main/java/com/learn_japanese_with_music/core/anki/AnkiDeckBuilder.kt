package com.learn_japanese_with_music.core.anki

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.learn_japanese_with_music.core.data.database.SavedWord
import java.security.MessageDigest

/** One row of the Anki `notes` table (plus the position of its single card). */
data class AnkiNote(
    val id: Long,
    val guid: String,
    val tags: String,
    val fields: String,
    val sortField: String,
    val checksum: Long,
    val position: Int
)

/** Everything needed to fill an Anki `collection.anki2` database. */
data class AnkiCollection(
    val createdSeconds: Long,
    val modifiedMillis: Long,
    val conf: String,
    val models: String,
    val decks: String,
    val dconf: String,
    val notes: List<AnkiNote>
)

/**
 * Turns saved words into the contents of an Anki collection (legacy schema 11, the format
 * every Anki client can import from an .apkg file).
 *
 * This file is plain Kotlin on purpose (no Android classes) so that the deck contents can be
 * produced and checked off-device. Writing the SQLite file and the zip lives in [AnkiExporter].
 */
object AnkiDeckBuilder {

    // Fixed ids: re-importing a newer export updates the same deck / note type instead of
    // creating duplicates. Never change these once an export has been released.
    const val MODEL_ID = 1731750000101L
    const val DECK_ID = 1731750000202L
    const val DECK_NAME = "Uta Learning"
    const val MODEL_NAME = "Uta Learning Vocabulary"
    const val ROOT_TAG = "UtaLearning"

    private const val FIELD_SEPARATOR = "\u001f"

    /** Field order of the note type. Only append; reordering breaks existing notes. */
    val FIELD_NAMES = listOf(
        "Word",
        "Reading",
        "DictionaryForm",
        "PartOfSpeech",
        "JLPT",
        "Meaning",
        "ContextMeaning",
        "Grammar",
        "Sentence",
        "Examples",
        "Notes",
        "Song",
        "Artist"
    )

    val SCHEMA = listOf(
        """
        CREATE TABLE col (
            id integer primary key, crt integer not null, mod integer not null,
            scm integer not null, ver integer not null, dty integer not null,
            usn integer not null, ls integer not null, conf text not null,
            models text not null, decks text not null, dconf text not null,
            tags text not null
        )
        """.trimIndent(),
        """
        CREATE TABLE notes (
            id integer primary key, guid text not null, mid integer not null,
            mod integer not null, usn integer not null, tags text not null,
            flds text not null, sfld integer not null, csum integer not null,
            flags integer not null, data text not null
        )
        """.trimIndent(),
        """
        CREATE TABLE cards (
            id integer primary key, nid integer not null, did integer not null,
            ord integer not null, mod integer not null, usn integer not null,
            type integer not null, queue integer not null, due integer not null,
            ivl integer not null, factor integer not null, reps integer not null,
            lapses integer not null, left integer not null, odue integer not null,
            odid integer not null, flags integer not null, data text not null
        )
        """.trimIndent(),
        """
        CREATE TABLE revlog (
            id integer primary key, cid integer not null, usn integer not null,
            ease integer not null, ivl integer not null, lastIvl integer not null,
            factor integer not null, time integer not null, type integer not null
        )
        """.trimIndent(),
        "CREATE TABLE graves (usn integer not null, oid integer not null, type integer not null)",
        "CREATE INDEX ix_notes_usn on notes (usn)",
        "CREATE INDEX ix_cards_usn on cards (usn)",
        "CREATE INDEX ix_revlog_usn on revlog (usn)",
        "CREATE INDEX ix_cards_nid on cards (nid)",
        "CREATE INDEX ix_cards_sched on cards (did, queue, due)",
        "CREATE INDEX ix_revlog_cid on revlog (cid)",
        "CREATE INDEX ix_notes_csum on notes (csum)"
    )

    const val INSERT_COL =
        "INSERT INTO col VALUES (1, ?, ?, ?, 11, 0, 0, 0, ?, ?, ?, ?, '{}')"
    const val INSERT_NOTE =
        "INSERT INTO notes VALUES (?, ?, ?, ?, -1, ?, ?, ?, ?, 0, '')"
    const val INSERT_CARD =
        "INSERT INTO cards VALUES (?, ?, ?, 0, ?, -1, 0, 0, ?, 0, 0, 0, 0, 0, 0, 0, 0, '')"

    private val FRONT_TEMPLATE = """
        <div class="word">{{Word}}</div>
        {{#Sentence}}<div class="sentence">{{Sentence}}</div>{{/Sentence}}
        {{#Song}}<div class="song">♪ {{Song}}{{#Artist}} — {{Artist}}{{/Artist}}</div>{{/Song}}
    """.trimIndent()

    private val BACK_TEMPLATE = """
        {{#Reading}}<div class="reading">{{Reading}}</div>{{/Reading}}
        <div class="word">{{Word}}</div>
        {{#DictionaryForm}}<div class="dict">原形: {{DictionaryForm}}</div>{{/DictionaryForm}}
        <div class="badges">{{#PartOfSpeech}}<span class="badge">{{PartOfSpeech}}</span>{{/PartOfSpeech}}{{#JLPT}}<span class="badge jlpt">JLPT {{JLPT}}</span>{{/JLPT}}</div>
        {{#Sentence}}<div class="sentence">{{Sentence}}</div>{{/Sentence}}
        <hr id="answer">
        <div class="detail">
        {{#Meaning}}<div class="title">一般意思</div><div class="body">{{Meaning}}</div>{{/Meaning}}
        {{#ContextMeaning}}<div class="title">歌詞中的用法</div><div class="body">{{ContextMeaning}}</div>{{/ContextMeaning}}
        {{#Grammar}}<div class="title">文法說明</div><div class="body">{{Grammar}}</div>{{/Grammar}}
        {{#Examples}}<div class="title">常見用法</div><div class="body">{{Examples}}</div>{{/Examples}}
        {{#Notes}}<div class="title">個人筆記</div><div class="body">{{Notes}}</div>{{/Notes}}
        </div>
        {{#Song}}<div class="song">♪ {{Song}}{{#Artist}} — {{Artist}}{{/Artist}}</div>{{/Song}}
    """.trimIndent()

    private val CARD_CSS = """
        .card { font-family: "Noto Sans JP", "Hiragino Sans", "Yu Gothic", "Noto Sans CJK JP", sans-serif; font-size: 18px; line-height: 1.7; text-align: center; color: #1f1f1f; background-color: #fdfbff; }
        .word { font-size: 44px; font-weight: bold; line-height: 1.3; }
        .reading { font-size: 20px; color: #5b6b8c; }
        .dict { font-size: 15px; color: #777; margin-top: 4px; }
        .badges { margin: 12px 0; }
        .badge { display: inline-block; padding: 2px 10px; margin: 0 3px; border-radius: 8px; font-size: 13px; background: #dbe4ff; }
        .badge.jlpt { background: #ffe0cc; }
        .sentence { margin: 14px 0; font-size: 20px; }
        .sentence b { color: #d9534f; }
        .detail { text-align: left; max-width: 640px; margin: 0 auto; }
        .title { margin-top: 16px; font-size: 15px; font-weight: bold; color: #3f5aa9; }
        .body { margin-top: 4px; }
        .ex { margin-bottom: 12px; }
        .ex-jp { font-weight: bold; }
        .ex-reading { font-size: 15px; color: #5b6b8c; }
        .ex-tr { font-size: 15px; }
        .song { margin-top: 20px; font-size: 13px; color: #888; }
        .nightMode.card, .night_mode.card, .nightMode .card, .night_mode .card { color: #e6e6e6; background-color: #1c1b1f; }
        .nightMode .reading, .night_mode .reading, .nightMode .ex-reading, .night_mode .ex-reading { color: #a9b8e0; }
        .nightMode .badge, .night_mode .badge { background: #3a4670; }
        .nightMode .badge.jlpt, .night_mode .badge.jlpt { background: #6b4630; }
        .nightMode .title, .night_mode .title { color: #9db2ff; }
        .nightMode .sentence b, .night_mode .sentence b { color: #ff8a80; }
    """.trimIndent()

    private const val LATEX_PRE =
        "\\documentclass[12pt]{article}\n\\special{papersize=3in,5in}\n\\usepackage[utf8]{inputenc}\n" +
            "\\usepackage{amssymb,amsmath}\n\\pagestyle{empty}\n\\setlength{\\parindent}{0in}\n\\begin{document}\n"
    private const val LATEX_POST = "\\end{document}"

    private const val CONF_JSON =
        """{"activeDecks":[1],"addToCur":true,"collapseTime":1200,"curDeck":1,"curModel":"$MODEL_ID","dueCounts":true,"estTimes":true,"newBury":true,"newSpread":0,"nextPos":1,"sortBackwards":false,"sortType":"noteFld","timeLim":0}"""

    private const val DCONF_JSON =
        """{"1":{"autoplay":true,"id":1,"lapse":{"delays":[10],"leechAction":0,"leechFails":8,"minInt":1,"mult":0},"maxTaken":60,"mod":0,"name":"Default","new":{"bury":true,"delays":[1,10],"initialFactor":2500,"ints":[1,4,7],"order":1,"perDay":20,"separate":true},"replayq":true,"rev":{"bury":true,"ease4":1.3,"fuzz":0.05,"ivlFct":1,"maxIvl":36500,"minSpace":1,"perDay":100},"timer":0,"usn":0}}"""

    private val gson = Gson()
    private val JLPT_LEVEL = Regex("N[1-5]")

    fun build(words: List<SavedWord>, nowMillis: Long = System.currentTimeMillis()): AnkiCollection {
        val nowSeconds = nowMillis / 1000
        val usedIds = HashSet<Long>()

        // Oldest first, so the cards are introduced in the order the words were collected.
        val notes = words.sortedBy { it.timestamp }.mapIndexed { index, word ->
            var id = if (word.timestamp > 0) word.timestamp else nowMillis
            while (!usedIds.add(id)) id++

            AnkiNote(
                id = id,
                guid = guidFor(word),
                tags = tagsFor(word),
                fields = fieldsFor(word).joinToString(FIELD_SEPARATOR),
                sortField = word.word,
                checksum = fieldChecksum(word.word),
                position = index + 1
            )
        }

        return AnkiCollection(
            createdSeconds = nowSeconds,
            modifiedMillis = nowMillis,
            conf = CONF_JSON,
            models = modelsJson(nowSeconds),
            decks = decksJson(nowSeconds),
            dconf = DCONF_JSON,
            notes = notes
        )
    }

    // ---------------------------------------------------------------------------------------
    // Notes
    // ---------------------------------------------------------------------------------------

    /** Must stay in the same order as [FIELD_NAMES]. */
    private fun fieldsFor(word: SavedWord): List<String> {
        val dictionaryForm = word.dictionaryForm.takeIf { it.isNotBlank() && it != word.word }.orEmpty()
        val jlpt = word.jlptLevel.trim()
            .takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
            ?.let { if (JLPT_LEVEL.matches(it.uppercase())) it.uppercase() else it }
            .orEmpty()
        val grammar = word.grammarExplanation.replace(Regex("""\s*(\d+\.)"""), "\n$1").trim()

        return listOf(
            escape(word.word),
            escape(word.reading),
            escape(dictionaryForm),
            escape(parseStringList(word.partOfSpeech).filter { it != "*" }.take(2).joinToString("・")),
            escape(jlpt),
            escape(word.generalMeaning),
            escape(word.contextualMeaning),
            escape(grammar),
            highlight(word.contextLine, word.word),
            examplesHtml(word.commonUsages),
            escape(word.notes),
            escape(word.songTitle.takeUnless { it == "Unknown" }.orEmpty()),
            escape(word.songArtist.takeUnless { it == "Unknown" }.orEmpty())
        )
    }

    /** Stable per saved word, so importing a later export updates the note instead of duplicating it. */
    private fun guidFor(word: SavedWord): String {
        val key = listOf(word.word, word.contextLine, word.splitMode).joinToString(FIELD_SEPARATOR)
        return sha1(key).take(8).joinToString("") { "%02x".format(it) }
    }

    private fun tagsFor(word: SavedWord): String {
        val tags = mutableListOf(ROOT_TAG)
        val jlpt = word.jlptLevel.trim().uppercase()
        if (JLPT_LEVEL.matches(jlpt)) tags += "JLPT::$jlpt"
        val song = sanitizeTag(word.songTitle)
        if (song.isNotEmpty() && song != "Unknown") tags += "song::$song"
        // Anki stores tags space separated, with a leading and trailing space.
        return tags.joinToString(separator = " ", prefix = " ", postfix = " ")
    }

    private fun sanitizeTag(raw: String): String =
        raw.trim().replace(Regex("""[\s\u3000"]+"""), "_")

    private fun examplesHtml(commonUsagesJson: String): String {
        val usages = try {
            gson.fromJson(commonUsagesJson, JsonArray::class.java)
        } catch (e: Exception) {
            null
        } ?: return ""

        val html = StringBuilder()
        for (element in usages) {
            if (!element.isJsonObject) continue
            val usage = element.asJsonObject
            val japanese = usage.stringOrEmpty("japanese")
            val reading = usage.stringOrEmpty("reading")
            val translation = usage.stringOrEmpty("translation")
            if (japanese.isEmpty() && translation.isEmpty()) continue

            html.append("<div class=\"ex\">")
            html.append("<div class=\"ex-jp\">").append(escape(japanese)).append("</div>")
            if (reading.isNotEmpty()) {
                html.append("<div class=\"ex-reading\">").append(escape(reading)).append("</div>")
            }
            html.append("<div class=\"ex-tr\">").append(escape(translation)).append("</div>")
            html.append("</div>")
        }
        return html.toString()
    }

    private fun JsonObject.stringOrEmpty(key: String): String {
        val value = get(key) ?: return ""
        return if (value.isJsonPrimitive) value.asString.trim() else ""
    }

    private fun parseStringList(json: String): List<String> = try {
        gson.fromJson(json, JsonArray::class.java)
            ?.filter { it.isJsonPrimitive }
            ?.map { it.asString }
            ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    /** The lyric line with the studied word in bold. */
    private fun highlight(sentence: String, word: String): String {
        val index = if (word.isEmpty()) -1 else sentence.indexOf(word)
        if (index < 0) return escape(sentence)
        // Escape the pieces without trimming them, so spaces around the word survive.
        return escapeRaw(sentence.substring(0, index).trimStart()) +
            "<b>" + escapeRaw(word) + "</b>" +
            escapeRaw(sentence.substring(index + word.length).trimEnd())
    }

    private fun escape(text: String): String = escapeRaw(text.trim())

    /** Anki fields are HTML, and must not contain the field separator. */
    private fun escapeRaw(text: String): String =
        text.replace(FIELD_SEPARATOR, "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\r\n", "\n")
            .replace("\n", "<br>")

    /** Anki's duplicate-check value: the first 8 hex digits of the SHA-1 of the first field. */
    private fun fieldChecksum(text: String): Long {
        val digest = sha1(text)
        var value = 0L
        for (i in 0 until 4) value = (value shl 8) or (digest[i].toLong() and 0xff)
        return value
    }

    private fun sha1(text: String): ByteArray =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))

    // ---------------------------------------------------------------------------------------
    // Note type and deck
    // ---------------------------------------------------------------------------------------

    private fun modelsJson(nowSeconds: Long): String {
        val fields = JsonArray()
        FIELD_NAMES.forEachIndexed { index, name ->
            fields.add(JsonObject().apply {
                addProperty("name", name)
                addProperty("ord", index)
                addProperty("font", "Arial")
                addProperty("size", 20)
                addProperty("rtl", false)
                addProperty("sticky", false)
                add("media", JsonArray())
            })
        }

        val template = JsonObject().apply {
            addProperty("name", "Recognition")
            addProperty("ord", 0)
            addProperty("qfmt", FRONT_TEMPLATE)
            addProperty("afmt", BACK_TEMPLATE)
            addProperty("bqfmt", "")
            addProperty("bafmt", "")
            add("did", JsonNull.INSTANCE)
        }

        // Card 1 is generated when field 0 (Word) is non-empty.
        val requirement = JsonArray().apply {
            add(0)
            add("any")
            add(JsonArray().apply { add(0) })
        }

        val model = JsonObject().apply {
            addProperty("id", MODEL_ID)
            addProperty("name", MODEL_NAME)
            addProperty("type", 0)
            addProperty("mod", nowSeconds)
            addProperty("usn", -1)
            addProperty("sortf", 0)
            addProperty("did", DECK_ID)
            add("tmpls", JsonArray().apply { add(template) })
            add("flds", fields)
            addProperty("css", CARD_CSS)
            addProperty("latexPre", LATEX_PRE)
            addProperty("latexPost", LATEX_POST)
            addProperty("latexsvg", false)
            add("req", JsonArray().apply { add(requirement) })
            add("tags", JsonArray())
            add("vers", JsonArray())
        }

        return JsonObject().apply { add(MODEL_ID.toString(), model) }.toString()
    }

    private fun decksJson(nowSeconds: Long): String {
        fun deck(id: Long, name: String, usn: Int) = JsonObject().apply {
            addProperty("id", id)
            addProperty("name", name)
            addProperty("mod", nowSeconds)
            addProperty("usn", usn)
            addProperty("desc", "")
            addProperty("dyn", 0)
            addProperty("conf", 1)
            addProperty("collapsed", false)
            addProperty("extendNew", 0)
            addProperty("extendRev", 50)
            for (counter in listOf("newToday", "revToday", "lrnToday", "timeToday")) {
                add(counter, JsonArray().apply { add(0); add(0) })
            }
        }

        return JsonObject().apply {
            add("1", deck(1, "Default", 0))
            add(DECK_ID.toString(), deck(DECK_ID, DECK_NAME, -1))
        }.toString()
    }
}
