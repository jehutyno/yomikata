package com.jehutyno.yomikata.repository.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jehutyno.yomikata.model.*


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
    @PrimaryKey val _id: Long,
    @ColumnInfo val name_en: String,
    @ColumnInfo val name_fr: String,
    @ColumnInfo val category: Int,
    @ColumnInfo val isSelected: Boolean
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

@Entity(tableName = "words")
data class RoomWords (
    @PrimaryKey val _id: Long,
    @ColumnInfo val japanese: String,
    @ColumnInfo val english: String,
    @ColumnInfo val french: String,
    @ColumnInfo val reading: String,
    @ColumnInfo val level: Int,
    @ColumnInfo val count_try: Int,
    @ColumnInfo val count_success: Int,
    @ColumnInfo val count_fail: Int,
    @ColumnInfo val is_kana: Int,
    @ColumnInfo val repetition: Int,
    @ColumnInfo val points: Int,
    @ColumnInfo val base_category: Int,
    @ColumnInfo val isSelected: Int,
    @ColumnInfo val sentence_id: Long
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
        return Word(_id, japanese, english, french, reading, level, count_try, count_success,
                    count_fail, is_kana, repetition, points, base_category, isSelected, sentence_id)
    }
}

@Entity(tableName = "quiz_word")
data class RoomQuizWord (
    @PrimaryKey val _id: Long,
    @ColumnInfo val quiz_id: Long,
    @ColumnInfo val word_id: Long,
) {
    companion object {
        fun from(quizWord: QuizWord): RoomQuizWord {
            return RoomQuizWord(quizWord.id, quizWord.quizId, quizWord.wordId)
        }
    }

    fun toQuizWord(): QuizWord {
        return QuizWord(_id, quiz_id, word_id)
    }
}

@Entity(tableName = "stat_entry")
data class RoomStatEntry (
    @PrimaryKey val _id: Long,
    @ColumnInfo val action: Int,
    @ColumnInfo val associatedId: Long,
    @ColumnInfo val date: Long,
    @ColumnInfo val result: Int,
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
    @PrimaryKey val _id: Long,
    @ColumnInfo val kanji: String?,
    @ColumnInfo val strokes: Int,
    @ColumnInfo val en: String?,
    @ColumnInfo val fr: String?,
    @ColumnInfo val kunyomi: String?,
    @ColumnInfo val onyomi: String?,
    @ColumnInfo val radical: String?,
) {
    companion object {
        fun from(kanjiSolo: KanjiSolo): RoomKanjiSolo {
            return RoomKanjiSolo(kanjiSolo.id, kanjiSolo.kanji, kanjiSolo.strokes, kanjiSolo.en,
                                 kanjiSolo.fr, kanjiSolo.kunyomi, kanjiSolo.onyomi, kanjiSolo.radical)
        }
    }

    fun toKanjiSolo(): KanjiSolo {
        return KanjiSolo(_id, kanji!!, strokes, en!!, fr!!, kunyomi!!, onyomi!!, radical!!)
    }
}

@Entity(tableName = "radicals")
data class RoomRadicals (
    @PrimaryKey val _id: Long,
    @ColumnInfo val strokes: Int,
    @ColumnInfo val radical: String?,
    @ColumnInfo val reading: String?,
    @ColumnInfo val en: String?,
    @ColumnInfo val fr: String?,
) {
    companion object {
        fun from(radical: Radical): RoomRadicals {
            return RoomRadicals(radical.id, radical.strokes, radical.radical,
                                radical.reading, radical.en, radical.fr)
        }
    }

    fun toRadical(): Radical {
        return Radical(_id, strokes, radical!!, reading!!, en!!, fr!!)
    }
}

@Entity(tableName = "sentences")
data class RoomSentences (
    @PrimaryKey val _id: Long,
    @ColumnInfo val jap: String?,
    @ColumnInfo val en: String?,
    @ColumnInfo val fr: String?,
    @ColumnInfo val level: Int,
) {
    companion object {
        fun from(sentence: Sentence): RoomSentences {
            return RoomSentences(sentence.id, sentence.jap, sentence.en, sentence.fr, sentence.level)
        }
    }

    fun toSentence(): Sentence {
        return Sentence(_id, jap!!, en!!, fr!!, level)
    }
}

// not part of database
data class RoomKanjiSoloRadical (
    @PrimaryKey val _id: Long,
    @ColumnInfo val kanji: String,
    @ColumnInfo val strokes: Int,
    @ColumnInfo val en: String,
    @ColumnInfo val fr: String,
    @ColumnInfo val kunyomi: String,
    @ColumnInfo val onyomi: String,
    @ColumnInfo val radical: String,
    @ColumnInfo val radStroke: Int,
    @ColumnInfo val radReading: String,
    @ColumnInfo val radEn: String,
    @ColumnInfo val radFr: String
)  {
    companion object {
        fun from(kanjiSolo: KanjiSolo, radical: Radical): RoomKanjiSoloRadical {
            return RoomKanjiSoloRadical(kanjiSolo.id, kanjiSolo.kanji, kanjiSolo.strokes, kanjiSolo.en,
                                        kanjiSolo.fr, kanjiSolo.kunyomi, kanjiSolo.onyomi, kanjiSolo.radical,
                                        radical.strokes, radical.reading, radical.en, radical.fr)
        }
    }

    fun toKanjiSoloRadical(): KanjiSoloRadical {
        return KanjiSoloRadical(_id, kanji, strokes, en, fr, kunyomi, onyomi, radical,
                                radStroke, radReading, radEn, radFr)
    }
}
