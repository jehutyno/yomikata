package com.jehutyno.yomikata.repository.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class RoomTables (
    @ColumnInfo val quiz: RoomQuiz,
    @ColumnInfo val words: RoomWords,
    @ColumnInfo val quiz_word: RoomQuizWord,
    @ColumnInfo val stat_entry: RoomStatEntry,
    @ColumnInfo val kanji_solo: RoomKanjiSolo,
    @ColumnInfo val radicals: RoomRadicals,
    @ColumnInfo val sentences: RoomSentences
)

@Entity(tableName = "quiz")
data class RoomQuiz (
    @PrimaryKey val _id: Int,
    @ColumnInfo val name_en: String?,
    @ColumnInfo val name_fr: String?,
    @ColumnInfo val category: Int,
    @ColumnInfo val isSelected: Int
)

@Entity(tableName = "words")
data class RoomWords (
    @PrimaryKey val _id: Int,
    @ColumnInfo val japanese: String,
    @ColumnInfo val english: String,
    @ColumnInfo val french: String,
    @ColumnInfo val reading: String,
    @ColumnInfo val level: Int,
    @ColumnInfo val count_try: Int,
    @ColumnInfo val count_success: Int,
    @ColumnInfo val count_fail: Int,
    @ColumnInfo val is_kana: Boolean,
    @ColumnInfo val repetition: Int,
    @ColumnInfo val points: Int,
    @ColumnInfo val base_category: Int,
    @ColumnInfo val isSelected: Int,
    @ColumnInfo val sentence_id: Int,
)

@Entity(tableName = "quiz_word")
data class RoomQuizWord (
    @PrimaryKey val _id: Int,
    @ColumnInfo val quiz_id: Int,
    @ColumnInfo val word_id: Int,
)

@Entity(tableName = "stat_entry")
data class RoomStatEntry (
    @PrimaryKey val _id: Int,
    @ColumnInfo val action: Int,
    @ColumnInfo val associatedId: Int,
    @ColumnInfo val date: Int,
    @ColumnInfo val result: Int,
)

@Entity(tableName = "kanji_solo")
data class RoomKanjiSolo (
    @PrimaryKey val _id: Int,
    @ColumnInfo val kanji: String?,
    @ColumnInfo val strokes: Int,
    @ColumnInfo val en: String?,
    @ColumnInfo val fr: String?,
    @ColumnInfo val kunyomi: String?,
    @ColumnInfo val onyomi: String?,
    @ColumnInfo val radical: String?,
)

@Entity(tableName = "radicals")
data class RoomRadicals (
    @PrimaryKey val _id: Int,
    @ColumnInfo val strokes: Int,
    @ColumnInfo val radical: String?,
    @ColumnInfo val reading: String?,
    @ColumnInfo val en: String?,
    @ColumnInfo val fr: String?,
)

@Entity(tableName = "sentences")
data class RoomSentences (
    @PrimaryKey val _id: Int,
    @ColumnInfo val jap: String?,
    @ColumnInfo val en: String?,
    @ColumnInfo val fr: String?,
    @ColumnInfo val level: Int,
)
