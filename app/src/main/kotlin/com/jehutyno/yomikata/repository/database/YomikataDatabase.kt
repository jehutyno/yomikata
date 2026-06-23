package com.jehutyno.yomikata.repository.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase as RawSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jehutyno.yomikata.repository.database.dao.KanjiSoloDao
import com.jehutyno.yomikata.repository.database.dao.QuizDao
import com.jehutyno.yomikata.repository.database.dao.SentenceDao
import com.jehutyno.yomikata.repository.database.dao.StatsDao
import com.jehutyno.yomikata.repository.database.dao.WordDao
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.channels.FileLock


const val DATABASE_VERSION = 21

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
                                MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                                MIGRATION_16_21
                            )
                            .addCallback(object : RoomDatabase.Callback() {
                                // onOpen runs AFTER all migrations are committed, outside any
                                // Room transaction. ATTACH DATABASE is safe here (no WAL deadlock).
                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onOpen(db)
                                    // Room 2.7+ requires room_table_modification_log for
                                    // TriggerBasedInvalidationTracker. createAllTables() creates it
                                    // on fresh installs, but not during migrations from old versions.
                                    // onOpen runs before any Flow observer triggers syncTriggers().
                                    db.execSQL(
                                        "CREATE TABLE IF NOT EXISTS `room_table_modification_log` " +
                                        "(`table_id` INTEGER NOT NULL, `invalidated` INTEGER NOT NULL DEFAULT 0, " +
                                        "PRIMARY KEY(`table_id`))"
                                    )
                                    populateTranslationsIfNeeded(context, db)
                                    populatePosIfNeeded(context, db)
                                }
                            })
                            .fallbackToDestructiveMigrationFrom(
                                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
                            )
                            .build()
                }
            }
            return checkNotNull(INSTANCE) { "Database not initialized" }
        }

        /**
         * Force load database.
         *
         * Applies any pre-Room data migrations, then forces Room to open and migrate the database.
         *
         * @param context Context
         */
        @Synchronized
        fun forceLoadDatabase(context: Context) {
            // MIGRATION_18_19 is a data-only migration (translations).
            // ATTACH DATABASE is unreliable inside Room's transaction wrapper on Android,
            // so we apply the translation copy here with a raw SQLiteDatabase connection,
            // BEFORE Room opens the file. Room's MIGRATION_18_19 then becomes a no-op.
            applyTranslationsBeforeRoomMigration(context)
            getDatabase(context).openHelper.writableDatabase
        }

        /**
         * Pre-migration: if the database is at version 18 (word/quiz translation columns exist
         * but are empty), populate them from the bundled asset database using a raw Android
         * SQLiteDatabase connection. ATTACH DATABASE works fine outside of Room's transaction.
         *
         * Safe to call multiple times — exits immediately if the DB does not exist (fresh install)
         * or is already beyond version 18.
         */
        private fun applyTranslationsBeforeRoomMigration(context: Context) {
            if (INSTANCE != null) return  // Room already open, migrations already done
            val dbFile = getDatabaseFile(context)
            if (!dbFile.exists()) return  // Fresh install: Room will copy the full asset

            val rawDb = RawSQLiteDatabase.openDatabase(
                dbFile.absolutePath, null, RawSQLiteDatabase.OPEN_READWRITE
            )
            val currentVersion = rawDb.version

            if (currentVersion != 18) {
                rawDb.close()
                return  // Nothing to do: already migrated, or not yet at v18
            }

            // DB is at v18: translation columns exist but are empty.
            // Copy the v19 asset to a temp file and ATTACH it.
            val tempFile = File(context.cacheDir, "asset_translation_temp.db")
            try {
                context.assets.open("yomikataz_translations.db").use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                rawDb.execSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS assetdb")
                rawDb.beginTransaction()
                try {
                    // Populate word translations from asset (only rows still empty)
                    rawDb.execSQL("""
                        UPDATE words
                        SET german     = (SELECT a.german     FROM assetdb.words a WHERE a._id = words._id),
                            spanish    = (SELECT a.spanish    FROM assetdb.words a WHERE a._id = words._id),
                            portuguese = (SELECT a.portuguese FROM assetdb.words a WHERE a._id = words._id),
                            chinese    = (SELECT a.chinese    FROM assetdb.words a WHERE a._id = words._id)
                        WHERE german = '' OR spanish = '' OR portuguese = '' OR chinese = ''
                    """.trimIndent())
                    // Populate quiz name translations from asset
                    rawDb.execSQL("""
                        UPDATE quiz
                        SET name_de = (SELECT a.name_de FROM assetdb.quiz a WHERE a._id = quiz._id),
                            name_es = (SELECT a.name_es FROM assetdb.quiz a WHERE a._id = quiz._id),
                            name_pt = (SELECT a.name_pt FROM assetdb.quiz a WHERE a._id = quiz._id),
                            name_zh = (SELECT a.name_zh FROM assetdb.quiz a WHERE a._id = quiz._id)
                        WHERE name_de = '' OR name_es = '' OR name_pt = '' OR name_zh = ''
                    """.trimIndent())
                    rawDb.setTransactionSuccessful()
                } finally {
                    rawDb.endTransaction()
                }
                rawDb.execSQL("DETACH DATABASE assetdb")
            } finally {
                rawDb.close()
                tempFile.delete()
            }
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

        ///////// DEFINE MIGRATIONS /////////
        // do not use values, constants, entities, daos, etc. that may be changed externally

        /**
         * Migration 18 → 19 — schema-only version bump, no DDL changes.
         *
         * Translation data is populated by [populateTranslationsIfNeeded] in the
         * [RoomDatabase.Callback.onOpen] callback, which runs AFTER this transaction is
         * committed. ATTACH DATABASE cannot be used inside Room's write-transaction (it deadlocks
         * in WAL mode), but works fine in onOpen which executes outside any transaction.
         */
        fun createMigration18to19(context: Context): Migration = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No DDL. Translations are applied by the onOpen callback after commit.
            }
        }

        /**
         * Migration 19 → 20 — extract Parts of Speech from english column into dedicated pos column.
         *
         * For each word, the POS tokens (e.g. "(n)", "(v1,vt)") are extracted from the english
         * field, stored comma-separated in the new pos column, and stripped from english.
         * Self-contained: the regex is re-declared here so it does not depend on TranslationParser.
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE words ADD COLUMN pos TEXT NOT NULL DEFAULT ''")

                // Re-declare the POS regex inline (migration must be self-contained)
                val godanEndings = "utrnbmkgs"
                val possiblePopTokens = arrayOf(
                    "n", "n-suf", "n-adv",
                    "pn",
                    "vs", "vi", "vt",
                    "adj-na", "adj-no", "adj-i", "adj-t",
                    "adv", "adv-to", "n-adv", "n-t",
                    "pref", "suf",
                    "exp",
                    "num", "ctr",
                    "pol", "hum",
                    "abbr",
                    "int",
                    "v1", "vz",
                    "aux-v", "aux-adj",
                    *godanEndings.map { "v5$it" }.toTypedArray()
                ).distinct()
                val concat = possiblePopTokens.joinToString("|")
                val regexPop = Regex("""\(($concat)(,($concat))*\)\s*""")

                // Read all words
                val rows = mutableListOf<Triple<Long, String, String>>() // id, pos, cleaned english
                database.query("SELECT _id, english FROM words").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("_id")
                    val enIdx = cursor.getColumnIndexOrThrow("english")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val english = cursor.getString(enIdx)
                        // Collect all POS tokens found in this english string
                        val tokens = regexPop.findAll(english)
                            .flatMap { match ->
                                match.value.trim().removeSurrounding("(", ")")
                                    .trimEnd()
                                    .split(",")
                            }
                            .distinct()
                            .toList()
                        val posStr = tokens.joinToString(",")
                        val cleanedEnglish = regexPop.replace(english, "").trim()
                        rows.add(Triple(id, posStr, cleanedEnglish))
                    }
                }

                // Write back extracted POS and cleaned english
                val update = "UPDATE words SET pos = ?, english = ? WHERE _id = ?"
                rows.forEach { (id, pos, english) ->
                    database.execSQL(update, arrayOf<Any>(pos, english, id))
                }

                // Strip leading POS tokens from french translations (POS now lives in `pos`).
                // Only LEADING groups made entirely of whitelisted POS tokens are removed, so
                // content like "(compteur de jours)" / "(ma) femme" / mid-string "(crayon)" stays.
                val frPosTokens = listOf(
                    "adj-na", "adj-no", "adj-pn", "adj-i", "adj-t", "adj-o", "adj",
                    "n-adv", "n-suf", "n-pref", "n-t", "n",
                    "adv-to", "adv-no", "adv", "aux-adj", "aux-v",
                    "vs-s", "v1-su", "v5aru", "v5u", "v5t", "v5r", "v5k", "v5g", "v5s",
                    "v5m", "v5b", "v5z", "v5", "v1", "vs", "vt", "vi", "vz", "v",
                    "pn", "pref", "suf", "ctr", "exp", "conj", "int", "num",
                    "uk", "hum", "hon", "pol", "vulg", "sl", "col", "gram", "arch", "abbr",
                    "su", "s"   // stray tokens, only inside two malformed groups: (su,ctr), (vs,s,vi)
                )
                val frToken = frPosTokens.joinToString("|")
                val regexFrPos = Regex(
                    """^(?:\s*\(\s*(?:$frToken)(?:\s*,\s*(?:$frToken))*\s*\))+\s*""",
                    RegexOption.IGNORE_CASE
                )
                val frRows = mutableListOf<Pair<Long, String>>()
                database.query("SELECT _id, french FROM words").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("_id")
                    val frIdx = cursor.getColumnIndexOrThrow("french")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val french = cursor.getString(frIdx)
                        val cleaned = regexFrPos.replace(french, "").trim()
                        if (cleaned != french) frRows.add(id to cleaned)
                    }
                }
                frRows.forEach { (id, fr) ->
                    database.execSQL("UPDATE words SET french = ? WHERE _id = ?", arrayOf<Any>(fr, id))
                }
                // Two source rows have a malformed POS prefix with a missing ')': fix explicitly.
                database.execSQL(
                    "UPDATE words SET french = ? WHERE _id = 6526 AND french = ?",
                    arrayOf<Any>("à prédominance; ascendant; supériorité", "(adj-na,nà prédominance; ascendant; supériorité")
                )
                database.execSQL(
                    "UPDATE words SET french = ? WHERE _id = 6798 AND french = ?",
                    arrayOf<Any>("bondé; plein à craquer", "(adj-na,n,adj-no bondé; plein à craquer")
                )
            }
        }

        /**
         * Migration 20 → 21 — schema-only version bump, no DDL changes.
         *
         * POS data enrichment (JLPT4/5 words missing POS after migration 19→20) is applied by
         * [populatePosIfNeeded] in the [RoomDatabase.Callback.onOpen] callback, which runs
         * outside any Room transaction so ATTACH DATABASE works without WAL deadlock.
         */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Room 2.7+ requires room_table_modification_log for TriggerBasedInvalidationTracker.
                // This table is created by createAllTables() on fresh installs but NOT automatically
                // during migrations from older schema versions — so we create it explicitly here.
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `room_table_modification_log` " +
                    "(`table_id` INTEGER NOT NULL, `invalidated` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`table_id`))"
                )
            }
        }

        /**
         * Populates pos column for words that still have an empty pos, using the bundled
         * asset database as source. Runs outside any Room transaction (onOpen callback).
         * Safe to call multiple times — exits immediately if no words need updating.
         */
        fun populatePosIfNeeded(context: Context, db: SupportSQLiteDatabase) {
            val needsUpdate = db.query("SELECT COUNT(*) FROM words WHERE pos = '' LIMIT 1").use {
                it.moveToFirst() && it.getInt(0) > 0
            }
            if (!needsUpdate) return

            val tempFile = File(context.cacheDir, "asset_pos_temp.db")
            try {
                context.assets.open(DATABASE_FILE_NAME).use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                db.execSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS assetpos")
                db.execSQL("""
                    UPDATE words
                    SET pos = (SELECT a.pos FROM assetpos.words a WHERE a._id = words._id)
                    WHERE pos = ''
                      AND EXISTS (SELECT 1 FROM assetpos.words a WHERE a._id = words._id AND a.pos != '')
                """.trimIndent())
                db.execSQL("DETACH DATABASE assetpos")
            } finally {
                tempFile.delete()
            }
        }

        /**
         * Returns true if any translation columns still need to be populated.
         * Checks words (german) and sentences (de) to catch users who upgraded before
         * sentence translations were added to [yomikataz_translations.db].
         * Called by the onOpen callback to decide whether to run the translation copy.
         */
        fun needsTranslations(db: SupportSQLiteDatabase): Boolean {
            val wordsNeed = db.query("SELECT COUNT(*) FROM words WHERE german = '' LIMIT 1").use {
                it.moveToFirst() && it.getInt(0) > 0
            }
            if (wordsNeed) return true
            return db.query("SELECT COUNT(*) FROM sentences WHERE de = '' LIMIT 1").use {
                it.moveToFirst() && it.getInt(0) > 0
            }
        }

        /**
         * Populates word/quiz/sentence translation columns from [yomikataz_translations.db]
         * (the reference database bundled as a separate asset).  Runs OUTSIDE any Room
         * transaction so ATTACH DATABASE works without deadlocking in WAL mode.
         */
        fun populateTranslationsIfNeeded(context: Context, db: SupportSQLiteDatabase) {
            if (!needsTranslations(db)) return

            val tempFile = File(context.cacheDir, "translations_onopen_temp.db")
            try {
                context.assets.open("yomikataz_translations.db").use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                db.execSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS transdb")
                db.execSQL("""
                    UPDATE words
                    SET german     = (SELECT a.german     FROM transdb.words a WHERE a._id = words._id),
                        spanish    = (SELECT a.spanish    FROM transdb.words a WHERE a._id = words._id),
                        portuguese = (SELECT a.portuguese FROM transdb.words a WHERE a._id = words._id),
                        chinese    = (SELECT a.chinese    FROM transdb.words a WHERE a._id = words._id)
                    WHERE german = '' OR spanish = '' OR portuguese = '' OR chinese = ''
                """.trimIndent())
                db.execSQL("""
                    UPDATE quiz
                    SET name_de = (SELECT a.name_de FROM transdb.quiz a WHERE a._id = quiz._id),
                        name_es = (SELECT a.name_es FROM transdb.quiz a WHERE a._id = quiz._id),
                        name_pt = (SELECT a.name_pt FROM transdb.quiz a WHERE a._id = quiz._id),
                        name_zh = (SELECT a.name_zh FROM transdb.quiz a WHERE a._id = quiz._id)
                    WHERE name_de = '' OR name_es = '' OR name_pt = '' OR name_zh = ''
                """.trimIndent())
                db.execSQL("""
                    UPDATE sentences
                    SET de = (SELECT a.de FROM transdb.sentences a WHERE a._id = sentences._id),
                        es = (SELECT a.es FROM transdb.sentences a WHERE a._id = sentences._id),
                        pt = (SELECT a.pt FROM transdb.sentences a WHERE a._id = sentences._id),
                        zh = (SELECT a.zh FROM transdb.sentences a WHERE a._id = sentences._id)
                    WHERE de = '' OR es = '' OR pt = '' OR zh = ''
                """.trimIndent())
                db.execSQL("DETACH DATABASE transdb")
            } finally {
                tempFile.delete()
            }
        }

        // add translation columns for DE, ES, PT, ZH to all translatable tables
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // words: full language names (consistent with english / french)
                for (col in listOf("german", "spanish", "portuguese", "chinese")) {
                    database.execSQL("ALTER TABLE words ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                }
                // sentences: ISO codes (consistent with en / fr)
                for (col in listOf("de", "es", "pt", "zh")) {
                    database.execSQL("ALTER TABLE sentences ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                }
                // kanji_solo: ISO codes (consistent with en / fr)
                for (col in listOf("de", "es", "pt", "zh")) {
                    database.execSQL("ALTER TABLE kanji_solo ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                }
                // radicals: ISO codes (consistent with en / fr)
                for (col in listOf("de", "es", "pt", "zh")) {
                    database.execSQL("ALTER TABLE radicals ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                }
                // quiz: name_XX (consistent with name_en / name_fr)
                for (col in listOf("name_de", "name_es", "name_pt", "name_zh")) {
                    database.execSQL("ALTER TABLE quiz ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        /**
         * Migration 16 → 21 — single consolidated migration for production users (prod DB v16).
         *
         * Combines all changes from the 16→17→18→19→20→21 chain:
         *  - 16→17 : data cleanup (phantom word, double spaces)
         *  - 17→18 : ADD COLUMN for DE/ES/PT/ZH translations in all tables
         *  - 18→19 : translation data populated by [populateTranslationsIfNeeded] in onOpen
         *  - 19→20 : ADD COLUMN pos + POS extraction from english field
         *  - 20→21 : CREATE room_table_modification_log (Room 2.7+ TriggerBasedInvalidationTracker)
         */
        val MIGRATION_16_21 = object : Migration(16, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // --- 16→17: Data cleanup ---
                database.execSQL("DELETE FROM words WHERE _id = 3537")
                database.execSQL("""
                    UPDATE words SET english = TRIM(REPLACE(english, '  ', ' '))
                    WHERE english LIKE '%  %' OR english != TRIM(english)
                """.trimIndent())
                database.execSQL("""
                    UPDATE words SET french = TRIM(REPLACE(french, '  ', ' '))
                    WHERE french LIKE '%  %' OR french != TRIM(french)
                """.trimIndent())
                database.execSQL("""
                    UPDATE sentences SET en = TRIM(REPLACE(en, '  ', ' '))
                    WHERE en LIKE '%  %' OR en != TRIM(en)
                """.trimIndent())
                database.execSQL("""
                    UPDATE sentences SET fr = TRIM(REPLACE(fr, '  ', ' '))
                    WHERE fr LIKE '%  %' OR fr != TRIM(fr)
                """.trimIndent())

                // Word 4954 (先程) shipped with an empty english gloss in the v16 source data.
                // The asset and translations DB are fixed for fresh installs; this idempotent UPDATE
                // also repairs upgrading users (chinese/portuguese are repaired by the onOpen
                // translation populate, which only touches empty cells — english is not, hence this).
                database.execSQL(
                    "UPDATE words SET english = ? WHERE _id = 4954 AND english = ''",
                    arrayOf<Any>("a little while ago; just now")
                )

                // --- 17→18: Add multilingual columns ---
                for (col in listOf("german", "spanish", "portuguese", "chinese"))
                    database.execSQL("ALTER TABLE words ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                for (col in listOf("de", "es", "pt", "zh"))
                    database.execSQL("ALTER TABLE sentences ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                for (col in listOf("de", "es", "pt", "zh"))
                    database.execSQL("ALTER TABLE kanji_solo ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                for (col in listOf("de", "es", "pt", "zh"))
                    database.execSQL("ALTER TABLE radicals ADD COLUMN $col TEXT NOT NULL DEFAULT ''")
                for (col in listOf("name_de", "name_es", "name_pt", "name_zh"))
                    database.execSQL("ALTER TABLE quiz ADD COLUMN $col TEXT NOT NULL DEFAULT ''")

                // --- 18→19: Translations populated by onOpen (populateTranslationsIfNeeded) ---

                // --- 19→20: Extract POS from english field into dedicated pos column ---
                database.execSQL("ALTER TABLE words ADD COLUMN pos TEXT NOT NULL DEFAULT ''")
                val godanEndings = "utrnbmkgs"
                val possiblePosTokens = arrayOf(
                    "n", "n-suf", "n-adv", "pn", "vs", "vi", "vt",
                    "adj-na", "adj-no", "adj-i", "adj-t", "adv", "adv-to", "n-t",
                    "pref", "suf", "exp", "num", "ctr", "pol", "hum", "abbr", "int",
                    "v1", "vz", "aux-v", "aux-adj",
                    *godanEndings.map { "v5$it" }.toTypedArray()
                ).distinct()
                val concat = possiblePosTokens.joinToString("|")
                val regexPos = Regex("""\(($concat)(,($concat))*\)\s*""")
                val rows = mutableListOf<Triple<Long, String, String>>()
                database.query("SELECT _id, english FROM words").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("_id")
                    val enIdx = cursor.getColumnIndexOrThrow("english")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val english = cursor.getString(enIdx)
                        val tokens = regexPos.findAll(english)
                            .flatMap { it.value.trim().removeSurrounding("(", ")").trimEnd().split(",") }
                            .distinct().toList()
                        rows.add(Triple(id, tokens.joinToString(","), regexPos.replace(english, "").trim()))
                    }
                }
                rows.forEach { (id, pos, english) ->
                    database.execSQL("UPDATE words SET pos = ?, english = ? WHERE _id = ?", arrayOf<Any>(pos, english, id))
                }

                // --- Strip leading POS tokens from french translations (POS now lives in `pos`) ---
                // French uses the same JMdict POS notation as english, e.g. "(adj-na)(n) déplorable".
                // Only LEADING groups composed entirely of whitelisted POS tokens are removed, so
                // legitimate content keeps its parentheses: leading "(compteur de jours)" / "(ma) femme"
                // and mid-string "(crayon)" / "(à quelqu'un)" are untouched.
                val frPosTokens = listOf(
                    "adj-na", "adj-no", "adj-pn", "adj-i", "adj-t", "adj-o", "adj",
                    "n-adv", "n-suf", "n-pref", "n-t", "n",
                    "adv-to", "adv-no", "adv", "aux-adj", "aux-v",
                    "vs-s", "v1-su", "v5aru", "v5u", "v5t", "v5r", "v5k", "v5g", "v5s",
                    "v5m", "v5b", "v5z", "v5", "v1", "vs", "vt", "vi", "vz", "v",
                    "pn", "pref", "suf", "ctr", "exp", "conj", "int", "num",
                    "uk", "hum", "hon", "pol", "vulg", "sl", "col", "gram", "arch", "abbr",
                    "su", "s"   // stray tokens, only inside two malformed groups: (su,ctr), (vs,s,vi)
                )
                val frToken = frPosTokens.joinToString("|")
                val regexFrPos = Regex(
                    """^(?:\s*\(\s*(?:$frToken)(?:\s*,\s*(?:$frToken))*\s*\))+\s*""",
                    RegexOption.IGNORE_CASE
                )
                val frRows = mutableListOf<Pair<Long, String>>()
                database.query("SELECT _id, french FROM words").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("_id")
                    val frIdx = cursor.getColumnIndexOrThrow("french")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val french = cursor.getString(frIdx)
                        val cleaned = regexFrPos.replace(french, "").trim()
                        if (cleaned != french) frRows.add(id to cleaned)
                    }
                }
                frRows.forEach { (id, fr) ->
                    database.execSQL("UPDATE words SET french = ? WHERE _id = ?", arrayOf<Any>(fr, id))
                }
                // Two source rows have a malformed POS prefix with a missing ')': fix explicitly.
                database.execSQL(
                    "UPDATE words SET french = ? WHERE _id = 6526 AND french = ?",
                    arrayOf<Any>("à prédominance; ascendant; supériorité", "(adj-na,nà prédominance; ascendant; supériorité")
                )
                database.execSQL(
                    "UPDATE words SET french = ? WHERE _id = 6798 AND french = ?",
                    arrayOf<Any>("bondé; plein à craquer", "(adj-na,n,adj-no bondé; plein à craquer")
                )

                // --- 20→21: Room 2.7+ internal invalidation tracking table ---
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `room_table_modification_log` " +
                    "(`table_id` INTEGER NOT NULL, `invalidated` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`table_id`))"
                )
            }
        }

        // data cleanup: remove phantom word, fix double spaces and leading/trailing spaces
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove the phantom word (id=3537) whose all fields are empty/invalid
                database.execSQL("DELETE FROM words WHERE _id = 3537")

                // Fix double spaces and leading/trailing spaces in words
                database.execSQL("""
                    UPDATE words
                    SET english = TRIM(REPLACE(english, '  ', ' '))
                    WHERE english LIKE '%  %' OR english != TRIM(english)
                """.trimIndent())
                database.execSQL("""
                    UPDATE words
                    SET french = TRIM(REPLACE(french, '  ', ' '))
                    WHERE french LIKE '%  %' OR french != TRIM(french)
                """.trimIndent())

                // Fix double spaces and leading/trailing spaces in sentences
                database.execSQL("""
                    UPDATE sentences
                    SET en = TRIM(REPLACE(en, '  ', ' '))
                    WHERE en LIKE '%  %' OR en != TRIM(en)
                """.trimIndent())
                database.execSQL("""
                    UPDATE sentences
                    SET fr = TRIM(REPLACE(fr, '  ', ' '))
                    WHERE fr LIKE '%  %' OR fr != TRIM(fr)
                """.trimIndent())
            }
        }

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
                    database.execSQL(update, arrayOf<Any>(english, id))
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
                    database.execSQL(update, arrayOf<Any>(level, points, id))
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
