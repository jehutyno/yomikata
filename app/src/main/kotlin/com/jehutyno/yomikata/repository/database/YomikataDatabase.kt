package com.jehutyno.yomikata.repository.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jehutyno.yomikata.dao.KanjiSoloDao
import com.jehutyno.yomikata.dao.QuizDao
import com.jehutyno.yomikata.dao.SentenceDao
import com.jehutyno.yomikata.dao.StatsDao
import com.jehutyno.yomikata.dao.WordDao
import com.jehutyno.yomikata.repository.migration.OldMigrations
import com.jehutyno.yomikata.util.UpdateProgressDialog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.channels.FileLock


const val DATABASE_VERSION = 16

@Database(entities = [RoomKanjiSolo::class, RoomQuiz::class, RoomSentences::class,
                      RoomStatEntry::class, RoomWords::class, RoomQuizWord::class,
                      RoomRadicals::class],
          version = DATABASE_VERSION, exportSchema = true)
abstract class YomikataDatabase : RoomDatabase() {
    abstract fun kanjiSoloDao(): KanjiSoloDao
    abstract fun quizDao(): QuizDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun statsDao(): StatsDao
    abstract fun wordDao(): WordDao

    companion object {
        // file name should be the same for assets folder and database folder!
        private const val DATABASE_FILE_NAME = "yomikataz.db"
        private const val DATABASE_LOCAL_BACKUP_FILE_NAME = "yomikataz_backup.db"
        private var INSTANCE: YomikataDatabase? = null
        // WARNING: when creating from asset/file, Room will validate the schema, which
        // causes any gaps in the AUTOINCREMENT id columns to disappear (eg. ids 1, 2, 4 -> 1, 2, 3)
        // make sure there are no gaps in the asset databases to ensure consistency with migrations
        // update: this does not happen if the id is a foreign key constraint
        fun getDatabase(context: Context): YomikataDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE =
                        Room.databaseBuilder(
                            context,
                            YomikataDatabase::class.java,
                            DATABASE_FILE_NAME
                        )
                            .createFromAsset(DATABASE_FILE_NAME)
                            .addMigrations(
                                *OldMigrations.getOldMigrations(),
                                OldMigrations.MIGRATION_12_13(context),
                                MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16
                            )
                            .build()
                }
            }
            return INSTANCE!!
        }

        /**
         * Force load database.
         *
         * Will force the database to be loaded (and migrated if needed).
         * Room may still keep the old version loaded as well, so it is best to restart the app anyway.
         *
         * @param context Context
         */
        @Synchronized
        fun forceLoadDatabase(context: Context) {
            getDatabase(context).openHelper.writableDatabase
        }

        /**
         * Overwrite database
         *
         * Overwrites the current database with a new database.
         * If you want the loaded database to be instantiated and migrated immediately,
         * you should call forceLoadDatabase
         *
         * @param context Context
         * @param externalDatabasePath The absolute path to an external database which will
         * @param create_backup Create local backup if true
         * replace the currently loaded database
         */
        @Synchronized
        fun overwriteDatabase(context: Context, externalDatabasePath: String, create_backup: Boolean = true) {
            if (create_backup)
                createLocalBackup(context)
            // acquire lock
            var outputStream: OutputStream? = null
            var lock: FileLock? = null
            try {
                outputStream = getDatabaseFile(context).outputStream()
                lock = outputStream.channel.lock()

                getDatabase(context).close()

                // overwrite current with external data
                FileInputStream(externalDatabasePath).use { input ->
                    input.copyTo(outputStream)
                }

                INSTANCE = null
            } finally {
                lock?.release()
                outputStream?.close()
            }
        }

        @Synchronized
        fun overwriteDatabase(context: Context, data: ByteArray, create_backup: Boolean = true) {
            if (create_backup)
                createLocalBackup(context)
            // acquire lock
            var outputStream: FileOutputStream? = null
            var lock: FileLock? = null
            try {
                outputStream = FileOutputStream(getDatabaseFile(context))
                lock = outputStream.channel.lock()

                getDatabase(context).close()
                // overwrite current with external data
                outputStream.write(data)

                INSTANCE = null
            } finally {
                lock?.release()
                outputStream?.close()
            }
        }

        /**
         * Reset database
         *
         * Replace the database with the default one from the assets folder.
         * @param context Context
         */
        @Synchronized
        fun resetDatabase(context: Context) {
            createLocalBackup(context)
            getDatabase(context).close()
            getDatabaseFile(context).delete()
            INSTANCE = null
        }

        @Synchronized
        fun createLocalBackup(context: Context) {
            val currentDatabaseFile = getDatabaseFile(context)
            val backupFile = context.getDatabasePath(DATABASE_LOCAL_BACKUP_FILE_NAME)
            getDatabase(context).close()    // close to make sure transactions are finished
            currentDatabaseFile.copyTo(backupFile, overwrite = true)
        }

        @Synchronized
        fun restoreLocalBackup(context: Context): Boolean {
            val backupFile = context.getDatabasePath(DATABASE_LOCAL_BACKUP_FILE_NAME)
            if (!backupFile.exists())
                return false
            // create temp file and then move it to prevent corrupt database
            // if app closes/crashes during copy operation
            val tempFile = File.createTempFile("temp-backup--", ".db")
            try {
                backupFile.copyTo(tempFile, overwrite = true)
                getDatabase(context).close()
            } catch(e: Exception) {
                tempFile.delete()
                throw e
            }
            // rename temp file to database file
            if (!tempFile.renameTo(getDatabaseFile(context))) {
                throw Exception("Rename failed")
            }

            return true
        }

        @Synchronized
        fun getRawData(context: Context): ByteArray {
            val dbFile = getDatabaseFile(context)
            val data = ByteArray(dbFile.length().toInt()) // create byte array with size of input file

            var inputStream: FileInputStream? = null
            try {
                inputStream = FileInputStream(dbFile)

                getDatabase(context).close()

                inputStream.read(data)
            } finally {
                inputStream?.close()
            }
            return data
        }

        fun getDatabaseFile(context: Context): File {
            return context.getDatabasePath(DATABASE_FILE_NAME)
        }

        fun setUpdateProgressDialog(updateProgressDialog: UpdateProgressDialog?) {
            OldMigrations.updateProgressDialogGetter = { updateProgressDialog }
        }

        ///////// DEFINE MIGRATIONS /////////
        // do not use values, constants, entities, daos, etc. that may be changed externally

        // clean up english and french translations of words
        val MIGRATION_15_16 = object: Migration(15, 16) {
            /**
             * Remove ";(P)" at the end of many english translations
             */
            override fun migrate(database: SupportSQLiteDatabase) {
                var idAndEnglish = arrayListOf<Pair<Long, String>>()
                database.query("SELECT _id, english FROM words").use {
                    val idIndex = it.getColumnIndexOrThrow("_id")
                    val englishIndex = it.getColumnIndexOrThrow("english")

                    while (it.moveToNext()) {
                        idAndEnglish.add(Pair(
                            it.getLong(idIndex),
                            it.getString(englishIndex)
                        ))
                    }
                }

                /**
                 * @param str String
                 * @return New string with trailing ;(P) removed
                 */
                fun removeP(str: String): String {
                    val regex = Regex(";?\\(P\\)$")
                    return regex.replace(str, "")
                }

                idAndEnglish = idAndEnglish.map { (id, english) ->
                    Pair(id, removeP(english))
                } as ArrayList


                val update = """UPDATE words SET english = ? WHERE _id = ?"""
                idAndEnglish.forEach { (id, english) ->
                    database.execSQL(update, arrayOf(english, id))
                }
            }
        }

        // migrate points and level system
        val MIGRATION_14_15 = object: Migration(14, 15) {
            /**
             * Old system:
             *      Points reset back to zero when leveling up
             * New system:
             *      Points keep increasing and level is determined by points uniquely
             */
            override fun migrate(database: SupportSQLiteDatabase) {
                // fix a small issue with foreign key constraints
                database.execSQL("""UPDATE words SET sentence_id = NULL WHERE _id = 3537""")

                // go through all words and change level and words
                // repetition is kept, it will be recalculated when the word is tested anyway
                val idLevelPoints = mutableListOf<Triple<Long, Int, Int>>()

                database.query("""SELECT _id, level, points FROM words""").use {
                    val idIndex = it.getColumnIndexOrThrow("_id")
                    val levelIndex = it.getColumnIndexOrThrow("level")
                    val pointsIndex = it.getColumnIndexOrThrow("points")

                    // add all words to list
                    while (it.moveToNext()) {
                        idLevelPoints.add(Triple(
                            it.getLong(idIndex),
                            it.getInt(levelIndex),
                            it.getInt(pointsIndex)
                        ))
                    }
                }

                // minimum points requirements for level
                val low = 0
                val medium = 200
                val high = 400
                val master = 600
                val max = 850

                fun getPointsForLevel(level: Int): Int {
                    return when(level) {
                        0 -> low
                        1 -> medium
                        2 -> high
                        3 -> master
                        else -> max
                    }
                }
                fun getNewLevelFromNewPoints(points: Int): Int {
                    return   if (points < medium)   0
                        else if (points < high)     1
                        else if (points < master)   2
                        else                        3
                }
                val newIdLevelPoints = mutableListOf<Triple<Long, Int, Int>>()
                // change the level & points for each word
                idLevelPoints.forEach {
                    val currentLevel = it.second
                    if (currentLevel == -1)
                        return@forEach
                    val percent = it.third
                        .coerceAtLeast(0)   // make sure points are not negative
                        .toFloat()                      // use points as percent between 0 and 100
                    val basePoints = getPointsForLevel(currentLevel)

                    // change to new values
                    val difference = getPointsForLevel(currentLevel + 1) - basePoints
                    val newPoints = (basePoints + (difference.toFloat() * percent / 100f).toInt())
                                    .coerceIn(0, max)  // make sure points are in valid range

                    newIdLevelPoints.add(Triple(
                        it.first,
                        getNewLevelFromNewPoints(newPoints),
                        newPoints
                    ))
                }

                // update database
                val update = "UPDATE words SET level = ?, points = ? WHERE _id = ?"
                newIdLevelPoints.forEach { (id, level, points) ->
                    database.execSQL(update, arrayOf(level, points, id))
                }
            }
        }

        // migrate from anko sqlite to room
        val MIGRATION_13_14 = object : Migration(13, 14) {

            // drop and recreate tables in order to make types more consistent
            override fun migrate(database: SupportSQLiteDatabase) {
                // quiz     isSelected is a boolean, but Room stores it as an INTEGER in the db
                database.execSQL(
                    """
                    CREATE TABLE NEW_quiz (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name_en TEXT NOT NULL,
                    name_fr TEXT NOT NULL,
                    category INTEGER NOT NULL,
                    isSelected INTEGER NOT NULL DEFAULT (0)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO NEW_quiz ( _id, name_en, name_fr, category, isSelected )
                    SELECT                 _id, name_en, name_fr, category, isSelected
                    FROM quiz
                        
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE quiz")
                database.execSQL("ALTER TABLE NEW_quiz RENAME TO quiz")

                // words
                database.execSQL(
                    """
                    CREATE TABLE NEW_words (
                      _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      japanese TEXT NOT NULL,
                      english TEXT NOT NULL,
                      french TEXT NOT NULL,
                      reading TEXT NOT NULL,
                      level INTEGER NOT NULL DEFAULT (0),
                      count_try INTEGER NOT NULL DEFAULT (0),
                      count_success INTEGER NOT NULL DEFAULT (0),
                      count_fail INTEGER NOT NULL DEFAULT (0),
                      is_kana INTEGER NOT NULL,
                      repetition INTEGER NOT NULL DEFAULT (-1),
                      points INTEGER NOT NULL DEFAULT (0),
                      base_category INTEGER NOT NULL,
                      isSelected INTEGER NOT NULL DEFAULT (0),
                      sentence_id INTEGER,
                      FOREIGN KEY(sentence_id) REFERENCES sentences(_id) ON UPDATE CASCADE ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO NEW_words (
                        _id, japanese, english, french, reading, level, count_try,
                        count_success, count_fail, is_kana, repetition, points,
                        base_category, isSelected, sentence_id
                    )
                    SELECT 
                        _id, japanese, english, french, reading, level, count_try,
                        count_success, count_fail, is_kana, repetition, points,
                        base_category, isSelected, sentence_id 
                    FROM words
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE words")
                database.execSQL("ALTER TABLE NEW_words RENAME TO words")

                // add words sentence_id index
                database.execSQL(
                    """CREATE INDEX IF NOT EXISTS index_words_sentence_id ON words (sentence_id)"""
                )

                // quiz word        remove _id column, make quiz_id & word_id both primary keys
                database.execSQL(
                    """
                    CREATE TABLE NEW_quiz_word (
                        quiz_id INTEGER NOT NULL,
                        word_id INTEGER NOT NULL,
                        PRIMARY KEY(quiz_id, word_id),
                        FOREIGN KEY(quiz_id) REFERENCES quiz(_id) ON UPDATE CASCADE ON DELETE CASCADE,
                        FOREIGN KEY(word_id) REFERENCES words(_id) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO NEW_quiz_word ( quiz_id, word_id )
                    SELECT                      quiz_id, word_id
                    FROM quiz_word
                    """.trimIndent()
                )
                database.execSQL("""DROP TABLE quiz_word""")
                database.execSQL("""ALTER TABLE NEW_quiz_word RENAME TO quiz_word""")

                // add quiz_word indices
                database.execSQL(
                    """CREATE INDEX IF NOT EXISTS index_quiz_word_word_id ON quiz_word (word_id)"""
                )

                // stat entry
                database.execSQL(
                    """
                    CREATE TABLE NEW_stat_entry (
                        _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        "action" INTEGER NOT NULL,
                        associatedId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        result INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO NEW_stat_entry ( _id, "action", associatedId, date, result )
                    SELECT                       _id, "action", associatedId, date, result
                    FROM stat_entry
                    """.trimIndent()
                )
                database.execSQL("""DROP TABLE stat_entry""")
                database.execSQL("""ALTER TABLE NEW_stat_entry RENAME TO stat_entry""")

                // kanji solo
                database.execSQL(
                    """
                    CREATE TABLE NEW_kanji_solo (
                        kanji TEXT PRIMARY KEY NOT NULL,
                        strokes INTEGER NOT NULL,
                        en TEXT NOT NULL,
                        fr TEXT NOT NULL,
                        kunyomi TEXT NOT NULL,
                        onyomi TEXT NOT NULL,
                        radical TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO NEW_kanji_solo ( 
                                    kanji, strokes, en, fr, kunyomi, onyomi, radical
                                )
                    SELECT          kanji, strokes, en, fr, kunyomi, onyomi, radical
                    FROM kanji_solo
                    """.trimIndent()
                )
                database.execSQL("""DROP TABLE kanji_solo""")
                database.execSQL("""ALTER TABLE NEW_kanji_solo RENAME TO kanji_solo""")

                // radicals
                database.execSQL(
                    """
                    CREATE TABLE NEW_radicals (
                        radical TEXT PRIMARY KEY NOT NULL,
                        strokes INTEGER NOT NULL,
                        reading TEXT NOT NULL,
                        en TEXT NOT NULL,
                        fr TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO NEW_radicals ( radical, strokes, reading, en, fr )
                    SELECT                     radical, strokes, reading, en, fr
                    FROM radicals
                """.trimIndent()
                )
                database.execSQL("""DROP TABLE radicals""")
                database.execSQL("""ALTER TABLE NEW_radicals RENAME TO radicals""")

                // sentences
                database.execSQL(
                    """
                    CREATE TABLE NEW_sentences (
                        _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        jap TEXT NOT NULL,
                        en TEXT NOT NULL,
                        fr TEXT NOT NULL,
                        level INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO NEW_sentences (_id, jap, en, fr, level )
                    SELECT                     _id, jap, en, fr, level
                    FROM sentences
                """.trimIndent()
                )
                database.execSQL("""DROP TABLE sentences""")
                database.execSQL("""ALTER TABLE NEW_sentences RENAME TO sentences""")
            }

        }

    }

}
