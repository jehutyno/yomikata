package com.jehutyno.yomikata.repository.local

import androidx.room.*
import com.jehutyno.yomikata.model.*


@Entity(tableName = "quiz")
data class RoomQuiz (
    @PrimaryKey(autoGenerate = true) val _id: Long,
    @ColumnInfo val name_en: String,
    @ColumnInfo val name_fr: String,
    @ColumnInfo val category: Int,
    @ColumnInfo(defaultValue = "0") val isSelected: Boolean
) {
    companion object {
        fun from(quiz: Quiz): RoomQuiz {
            return RoomQuiz(quiz.id, quiz.nameEn, quiz.nameFr, quiz.category, quiz.isSelected)
        }
    }

    fun toQuiz(): Quiz {
        return Quiz(_id, name_en, name_fr, category, isSelected)
    }
}

@Entity(tableName = "words", foreignKeys = [
            ForeignKey(
                entity = RoomSentences::class,
                parentColumns = ["_id"],
                childColumns = ["sentence_id"],
                onDelete = ForeignKey.SET_NULL,
                onUpdate = ForeignKey.CASCADE
            )
        ]
)
data class RoomWords (
    @PrimaryKey(autoGenerate = true) val _id: Long,
    @ColumnInfo val japanese: String,
    @ColumnInfo val english: String,
    @ColumnInfo val french: String,
    @ColumnInfo val reading: String,
    @ColumnInfo(defaultValue = "0") val level: Int,
    @ColumnInfo(defaultValue = "0") val count_try: Int,
    @ColumnInfo(defaultValue = "0") val count_success: Int,
    @ColumnInfo(defaultValue = "0") val count_fail: Int,
    @ColumnInfo val is_kana: Int,      // 0 = kanji, 1 = normal hiragana/katakana,
                                       // 2 = single character used in hiragana/katakana quizzes
    @ColumnInfo(defaultValue = "-1") val repetition: Int,
    @ColumnInfo(defaultValue = "0") val points: Int,
    @ColumnInfo val base_category: Int,
    @ColumnInfo(defaultValue = "0") val isSelected: Int,
    @ColumnInfo(index = true) val sentence_id: Long?
) {
    companion object {
        fun from(word: Word): RoomWords {
            return RoomWords(word.id, word.japanese, word.english, word.french,
                             word.reading, word.level, word.countTry, word.countSuccess,
                             word.countFail, word.isKana, word.repetition, word.points,
                             word.baseCategory, word.isSelected, word.sentenceId)
        }
    }

    fun toWord(): Word {
        return Word(
            _id, japanese, english, french, reading, level, count_try, count_success,
            count_fail, is_kana, repetition, points, base_category, isSelected, sentence_id
        )
    }
}

@Entity(tableName = "quiz_word", primaryKeys = ["quiz_id", "word_id"],
        foreignKeys = [
            ForeignKey(
                entity = RoomQuiz::class,
                parentColumns = arrayOf("_id"),
                childColumns = arrayOf("quiz_id"),
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
            ),
            ForeignKey(
                entity = RoomWords::class,
                parentColumns = arrayOf("_id"),
                childColumns = arrayOf("word_id"),
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
            )
        ]
)
data class RoomQuizWord (
    val quiz_id: Long,
    @ColumnInfo(index = true) val word_id: Long
) {
    companion object {
        fun from(quizWord: QuizWord): RoomQuizWord {
            return RoomQuizWord(quizWord.quizId, quizWord.wordId)
        }
    }

    fun toQuizWord(): QuizWord {
        return QuizWord(quiz_id, word_id)
    }
}

// used to get a quiz together with a list of corresponding quiz_words
data class QuizWithQuizWords (
    @Embedded val quiz: RoomQuiz,
    @Relation(
        parentColumn = "_id",
        entityColumn = "quiz_id",
        associateBy = Junction(RoomQuizWord::class)
    )
    val quizWords: List<RoomQuizWord>
)

// used to get a word together with a list of corresponding quiz_words
data class WordWithQuizWords (
    @Embedded val word: RoomWords,
    @Relation(
        parentColumn = "_id",
        entityColumn = "word_id",
        associateBy = Junction(RoomQuizWord::class)
    )
    val quizWords: List<RoomQuizWord>
)

@Entity(tableName = "stat_entry")
data class RoomStatEntry (
    @PrimaryKey(autoGenerate = true) val _id: Long,
    @ColumnInfo val action: Int,
    @ColumnInfo val associatedId: Long,
    @ColumnInfo val date: Long,
    @ColumnInfo val result: Int
) {
    companion object {
        fun from(statEntry: StatEntry): RoomStatEntry {
            return RoomStatEntry(statEntry.id, statEntry.action, statEntry.associatedId,
                                 statEntry.date, statEntry.result)
        }
    }

    fun toStatEntry(): StatEntry {
        return StatEntry(_id, action, associatedId, date, result)
    }
}

@Entity(tableName = "kanji_solo")
data class RoomKanjiSolo (
    @PrimaryKey val kanji: String,
    @ColumnInfo val strokes: Int,
    @ColumnInfo val en: String,
    @ColumnInfo val fr: String,
    @ColumnInfo val kunyomi: String,
    @ColumnInfo val onyomi: String,
    @ColumnInfo val radical: String
) {
    companion object {
        fun from(kanjiSolo: KanjiSolo): RoomKanjiSolo {
            return RoomKanjiSolo(kanjiSolo.kanji, kanjiSolo.strokes, kanjiSolo.en,
                                 kanjiSolo.fr, kanjiSolo.kunyomi, kanjiSolo.onyomi, kanjiSolo.radical)
        }
    }

    fun toKanjiSolo(): KanjiSolo {
        return KanjiSolo(kanji, strokes, en, fr, kunyomi, onyomi, radical)
    }
}

@Entity(tableName = "radicals")
data class RoomRadicals (
    @PrimaryKey val radical: String,
    @ColumnInfo val strokes: Int,
    @ColumnInfo val reading: String,
    @ColumnInfo val en: String,
    @ColumnInfo val fr: String
) {
    companion object {
        fun from(radical: Radical): RoomRadicals {
            return RoomRadicals(radical.radical, radical.strokes,
                                radical.reading, radical.en, radical.fr)
        }
    }

    fun toRadical(): Radical {
        return Radical(radical, strokes, reading, en, fr)
    }
}

@Entity(tableName = "sentences")
data class RoomSentences (
    @PrimaryKey(autoGenerate = true) val _id: Long,
    @ColumnInfo val jap: String,
    @ColumnInfo val en: String,
    @ColumnInfo val fr: String,
    @ColumnInfo val level: Int      // used for voice file downloads
) {
    companion object {
        fun from(sentence: Sentence): RoomSentences {
            return RoomSentences(sentence.id, sentence.jap, sentence.en, sentence.fr, sentence.level)
        }
    }

    fun toSentence(): Sentence {
        return Sentence(_id, jap, en, fr, level)
    }
}

// not part of database
data class RoomKanjiSoloRadical (
    val kanji: String,
    val strokes: Int,
    val en: String,
    val fr: String,
    val kunyomi: String,
    val onyomi: String,
    val radical: String,
    val radStroke: Int,
    val radReading: String,
    val radEn: String,
    val radFr: String
)  {
    companion object {
        fun from(kanjiSolo: KanjiSolo, radical: Radical): RoomKanjiSoloRadical {
            return RoomKanjiSoloRadical(kanjiSolo.kanji, kanjiSolo.strokes, kanjiSolo.en,
                                        kanjiSolo.fr, kanjiSolo.kunyomi, kanjiSolo.onyomi, kanjiSolo.radical,
                                        radical.strokes, radical.reading, radical.en, radical.fr)
        }
    }

    fun toKanjiSoloRadical(): KanjiSoloRadical {
        return KanjiSoloRadical(kanji, strokes, en, fr, kunyomi, onyomi, radical,
                                radStroke, radReading, radEn, radFr)
    }

    fun toKanjiSolo(): KanjiSolo {
        return KanjiSolo(kanji, strokes, en, fr, kunyomi, onyomi, radical)
    }

    fun toRadical(): Radical {
        return Radical(radical, radStroke, radReading, radEn, radFr)
    }

}
