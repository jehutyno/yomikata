package com.jehutyno.yomikata.repository.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jehutyno.yomikata.model.KanjiSolo
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Radical
import org.jetbrains.anko.db.*

/**
 * Created by valentin on 10/01/2017.
 */
class KanjiSoloSource(var context: Context) : KanjiSoloRepository {

    override fun createKanjiSoloTable(db: SQLiteDatabase?) {
        if (db == null) {
            context.database.use{
                createTable(SQLiteTables.KANJI_SOLO.tableName, true,
                    SQLiteKanjiSolo.ID.column to INTEGER.plus(PRIMARY_KEY),
                    SQLiteKanjiSolo.KANJI.column to TEXT,
                    SQLiteKanjiSolo.STROKES.column to INTEGER,
                    SQLiteKanjiSolo.EN.column to TEXT,
                    SQLiteKanjiSolo.FR.column to TEXT,
                    SQLiteKanjiSolo.KUNYOMI.column to TEXT,
                    SQLiteKanjiSolo.ONYOMI.column to TEXT,
                    SQLiteKanjiSolo.RADICAL.column to TEXT
                )
            }
        } else db
            .createTable(SQLiteTables.KANJI_SOLO.tableName, true,
                SQLiteKanjiSolo.ID.column to INTEGER.plus(PRIMARY_KEY),
                SQLiteKanjiSolo.KANJI.column to TEXT,
                SQLiteKanjiSolo.STROKES.column to INTEGER,
                SQLiteKanjiSolo.EN.column to TEXT,
                SQLiteKanjiSolo.FR.column to TEXT,
                SQLiteKanjiSolo.KUNYOMI.column to TEXT,
                SQLiteKanjiSolo.ONYOMI.column to TEXT,
                SQLiteKanjiSolo.RADICAL.column to TEXT
            )
    }

    override fun createRadicalsTable(db: SQLiteDatabase?) {
        if (db == null) {
            context.database.use {
                createTable(SQLiteTables.RADICALS.tableName, true,
                    SQLiteRadicals.ID.column to INTEGER.plus(PRIMARY_KEY),
                    SQLiteRadicals.STROKES.column to INTEGER,
                    SQLiteRadicals.RADICAL.column to TEXT,
                    SQLiteRadicals.READING.column to TEXT,
                    SQLiteRadicals.EN.column to TEXT,
                    SQLiteRadicals.FR.column to TEXT
                )
            }
        }else db
            .createTable(SQLiteTables.RADICALS.tableName, true,
                SQLiteRadicals.ID.column to INTEGER.plus(PRIMARY_KEY),
                SQLiteRadicals.STROKES.column to INTEGER,
                SQLiteRadicals.RADICAL.column to TEXT,
                SQLiteRadicals.READING.column to TEXT,
                SQLiteRadicals.EN.column to TEXT,
                SQLiteRadicals.FR.column to TEXT
            )
    }

    override fun kanjiSoloCount(db: SQLiteDatabase?): Int {
        var radCount = 0
        if (db == null) {
            context.database.use {
                select(SQLiteTables.KANJI_SOLO.tableName, *SQLiteTable.allColumns(SQLiteKanjiSolo.values()))
                    .exec {
                        radCount = count
                    }
            }
        } else db
            .select(SQLiteTables.KANJI_SOLO.tableName, *SQLiteTable.allColumns(SQLiteKanjiSolo.values()))
            .exec {
                radCount = count
            }

        return radCount
    }

    override fun radicalsCount(db: SQLiteDatabase?): Int {
        var radCount = 0
        if (db == null) {
            context.database.use {
                select(SQLiteTables.RADICALS.tableName, *SQLiteTable.allColumns(SQLiteRadicals.values()))
                    .exec {
                        radCount = count
                    }
            }
        } else db
            .select(SQLiteTables.RADICALS.tableName, *SQLiteTable.allColumns(SQLiteRadicals.values()))
            .exec {
                radCount = count
            }

        return radCount
    }

    override fun addKanjiSolo(kanjiSolo: KanjiSolo) {
        val values = ContentValues()
        values.put(SQLiteKanjiSolo.ID.column, kanjiSolo.id)
        values.put(SQLiteKanjiSolo.KANJI.column, kanjiSolo.kanji)
        values.put(SQLiteKanjiSolo.STROKES.column, kanjiSolo.strokes)
        values.put(SQLiteKanjiSolo.EN.column, kanjiSolo.en)
        values.put(SQLiteKanjiSolo.FR.column, kanjiSolo.fr)
        values.put(SQLiteKanjiSolo.KUNYOMI.column, kanjiSolo.kunyomi)
        values.put(SQLiteKanjiSolo.ONYOMI.column, kanjiSolo.onyomi)
        values.put(SQLiteKanjiSolo.RADICAL.column, kanjiSolo.radical)
        context.database.use {
            insert(SQLiteTables.KANJI_SOLO.tableName, null, values)
        }
    }

    override fun addRadical(radical: Radical) {
        val values = ContentValues()
        values.put(SQLiteRadicals.ID.column, radical.id)
        values.put(SQLiteRadicals.STROKES.column, radical.strokes)
        values.put(SQLiteRadicals.RADICAL.column, radical.radical)
        values.put(SQLiteRadicals.READING.column, radical.reading)
        values.put(SQLiteRadicals.EN.column, radical.en)
        values.put(SQLiteRadicals.FR.column, radical.fr)
        context.database.use {
            insert(SQLiteTables.RADICALS.tableName, null, values)
        }
    }

    override fun getSoloByKanji(kanji: String): KanjiSolo? {
        var radical: KanjiSolo? = null
        context.database.use {
            select(SQLiteTables.KANJI_SOLO.tableName, *SQLiteTable.allColumns(SQLiteKanjiSolo.values()))
                .where("${SQLiteKanjiSolo.KANJI.column} = '$kanji'")
                .exec {
                    if (count == 1) {
                        moveToFirst()
                        radical = KanjiSolo(getLong(0), getString(1), getInt(2), getString(3),
                            getString(4), getString(5), getString(6), getString(7))
                    }
                }
        }
        return radical
    }

    override fun getSoloByKanjiRadical(kanji: String): KanjiSoloRadical? {
        var radical: KanjiSoloRadical? = null
        var query = "select ${SQLiteTables.KANJI_SOLO.tableName}.${SQLiteTable.allColumns(SQLiteKanjiSolo.values()).joinToString(",${SQLiteTables.KANJI_SOLO.tableName}.")}," +
            "${SQLiteTables.RADICALS.tableName}.${SQLiteRadicals.STROKES.column}," +
            "${SQLiteTables.RADICALS.tableName}.${SQLiteRadicals.READING.column}," +
            "${SQLiteTables.RADICALS.tableName}.${SQLiteRadicals.EN.column}," +
            "${SQLiteTables.RADICALS.tableName}.${SQLiteRadicals.FR.column}" +
            " " +
            "from ${SQLiteTables.KANJI_SOLO.tableName} join ${SQLiteTables.RADICALS.tableName} " +
            "ON ${SQLiteTables.KANJI_SOLO.tableName}.${SQLiteKanjiSolo.RADICAL.column} = ${SQLiteTables.RADICALS.tableName}.${SQLiteRadicals.RADICAL.column} " +
            "where ${SQLiteTables.KANJI_SOLO.tableName}.${SQLiteKanjiSolo.KANJI.column} = '${kanji.trim()}'"

        context.database.use {
            val cursor = rawQuery(query, null)
            if (cursor.count == 1) {
                cursor.moveToFirst()
                radical = KanjiSoloRadical(cursor.getLong(0), cursor.getString(1), cursor.getInt(2), cursor.getString(3),
                    cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getInt(8),
                    cursor.getString(9), cursor.getString(10), cursor.getString(11))
            }
            cursor.close()
        }
        return radical
    }

    override fun getKanjiRadical(radicalString: String): Radical? {
        var radical: Radical? = null
        context.database.use {
            select(SQLiteTables.RADICALS.tableName, *SQLiteTable.allColumns(SQLiteRadicals.values()))
                .where("${SQLiteRadicals.RADICAL.column} = '$radicalString'")
                .exec {
                    if (count == 1) {
                        moveToFirst()
                        radical = Radical(getLong(0), getInt(1), getString(2), getString(3),
                            getString(4), getString(5))
                    }
                }
        }
        return radical
    }

}