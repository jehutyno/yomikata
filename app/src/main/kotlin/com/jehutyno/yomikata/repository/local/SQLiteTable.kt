package com.jehutyno.yomikata.repository.local

/**
 * Created by valentin on 13/10/2016.
 */
interface SQLiteTable {
    val column: String
    
    companion object {
        fun <T: SQLiteTable> allColumns(columns: Array<T>): Array<String> {
            return columns.flatMap { arrayOf(it.column).asIterable() }.toTypedArray()
        }
    }
}

enum class SQLiteTables(val tableName: String) {
        QUIZ("quiz"),
        WORDS("words"),
        QUIZ_WORD("quiz_word"),
        STAT_ENTRY("stat_entry"),
        KANJI_SOLO("kanji_solo"),
        RADICALS("radicals"),
        SENTENCES("sentences")
}

enum class SQLiteQuiz(override val column: String) : SQLiteTable {
    ID("_id"),
    NAME_EN("name_en"),
    NAME_FR("name_fr"),
    CATEGORY("category"),
    IS_SELECTED("isSelected")
}
enum class SQLiteWord(override val column: String) : SQLiteTable {
    ID("_id"),
    JAPANESE("japanese"),
    ENGLISH("english"),
    FRENCH("french"),
    READING("reading"),
    LEVEL("level"),
    COUNT_TRY("count_try"),
    COUNT_SUCCESS("count_success"),
    COUNT_FAIL("count_fail"),
    IS_KANA("is_kana"),
    REPETITION("repetition"),
    POINTS("points"),
    BASE_CATEGORY("base_category"),
    IS_SELECTED("isSelected"),
    SENTENCE_ID("sentence_id")
}
enum class SQLiteQuizWord(override val column: String) : SQLiteTable {
    ID("_id"),
    QUIZ_ID("quiz_id"),
    WORD_ID("word_id")
}
enum class SQLiteStatEntry(override val column: String) : SQLiteTable {
    ID("_id"),
    ACTION("action"),
    ASSOCIATED_ID("associatedId"),
    DATE("date"),
    RESULT("result")
}
enum class SQLiteKanjiSolo(override val column: String) : SQLiteTable {
    ID("_id"),
    KANJI("kanji"),
    STROKES("strokes"),
    EN("en"),
    FR("fr"),
    KUNYOMI("kunyomi"),
    ONYOMI("onyomi"),
    RADICAL("radical")
}
enum class SQLiteRadicals(override val column: String) : SQLiteTable {
    ID("_id"),
    STROKES("strokes"),
    RADICAL("radical"),
    READING("reading"),
    EN("en"),
    FR("fr")
}
enum class SQLiteSentences(override val column: String) : SQLiteTable {
    ID("_id"),
    JAP("jap"),
    EN("en"),
    FR("fr"),
    LEVEL("level")
}