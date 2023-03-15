package com.jehutyno.yomikata.repository.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.SentenceRepository
import org.jetbrains.anko.db.*

/**
 * Created by valentinlanfranchi on 19/05/2017.
 */
class SentenceSource(var context: Context) : SentenceRepository {

    override fun createSentencesTable() {
            context.database.use{
                createTable(SQLiteTables.SENTENCES.tableName, true,
                    SQLiteSentences.ID.column to INTEGER + PRIMARY_KEY,
                    SQLiteSentences.JAP.column to TEXT,
                    SQLiteSentences.EN.column to TEXT,
                    SQLiteSentences.FR.column to TEXT,
                    SQLiteSentences.LEVEL.column to INTEGER
                )
            }
    }

    override fun addSentence(sentence: Sentence) {
        val values = ContentValues()
        values.put(SQLiteSentences.ID.column, sentence.id)
        values.put(SQLiteSentences.JAP.column, sentence.jap)
        values.put(SQLiteSentences.EN.column, sentence.en)
        values.put(SQLiteSentences.FR.column, sentence.fr)
        values.put(SQLiteSentences.LEVEL.column, sentence.level)
        context.database.use {
            insert(SQLiteTables.SENTENCES.tableName, null, values)
        }
    }


    override fun getRandomSentence(word: Word, maxLevel: Int): Sentence? {
        var sentence: Sentence? = null
        val query = "select ${SQLiteTable.allColumns(SQLiteSentences.values()).joinToString(",")} " +
            "from ${SQLiteTables.SENTENCES.tableName} " +
            "where ${SQLiteSentences.JAP.column} like '%{${word.japanese};${word.reading}}%' " +
            "and ${SQLiteSentences.LEVEL.column} <= $maxLevel" +
            " order by RANDOM() limit 1"

        context.database.use {
            val cursor = rawQuery(query, null)
            if (cursor.count > 0) {
                sentence = cursor.parseSingle(getSentencesParser())

            }
            cursor.close()
        }

        return sentence
    }

    override fun getSentenceById(id: Long): Sentence {
        var sentence: Sentence? = null
        context.database.use {
            select(SQLiteTables.SENTENCES.tableName).whereArgs(
                    "${SQLiteSentences.ID.column} = $id").limit(1).exec {
                sentence = parseSingle(getSentencesParser())
            }
        }
        return sentence!!
    }

    override fun getAllSentences(db: SQLiteDatabase?): List<Sentence> {
        var sentences = listOf<Sentence>()
        if (db == null) {
            context.database.use {
                select(SQLiteTables.SENTENCES.tableName, *SQLiteTable.allColumns(SQLiteSentences.values()))
                    .exec {
                        sentences = parseList(getSentencesParser())
                    }
            }
        } else {
            db.select(SQLiteTables.SENTENCES.tableName, *SQLiteTable.allColumns(SQLiteSentences.values()))
                .exec {
                    sentences = parseList(getSentencesParser())
                }
        }
        return sentences
    }


    override fun updateSentence(updateSentence: Sentence, sentence: Sentence?) {
        context.database.use {
            if (sentence != null && updateSentence.jap != sentence!!.jap) {
                update(SQLiteTables.SENTENCES.tableName,
                    SQLiteSentences.JAP.column to updateSentence.jap).whereArgs(
                    "${SQLiteSentences.ID.column} = '${updateSentence.id}'").exec()
            }
            if (sentence != null && updateSentence.en != sentence!!.en) {
                update(SQLiteTables.SENTENCES.tableName,
                    SQLiteSentences.EN.column to updateSentence.en).whereArgs(
                    "${SQLiteSentences.ID.column} = '${updateSentence.id}'").exec()
            }
            if (sentence != null && updateSentence.fr != sentence!!.fr) {
                update(SQLiteTables.SENTENCES.tableName,
                    SQLiteSentences.FR.column to updateSentence.fr).whereArgs(
                    "${SQLiteSentences.ID.column} = '${updateSentence.id}'").exec()
            }

            if (sentence == null) {
                insert(SQLiteTables.SENTENCES.tableName,
                    SQLiteSentences.ID.column to updateSentence.id,
                    SQLiteSentences.JAP.column to updateSentence.jap,
                    SQLiteSentences.EN.column to updateSentence.en,
                    SQLiteSentences.FR.column to updateSentence.fr,
                    SQLiteSentences.LEVEL.column to updateSentence.level)
            }
        }
    }


}

fun getSentencesParser(): RowParser<Sentence> {
    val rowParser = rowParser { id: Long, jap: String, en: String, fr: String, level: Int ->
        Sentence(id, jap, en, fr, level)
    }

    return rowParser
}


