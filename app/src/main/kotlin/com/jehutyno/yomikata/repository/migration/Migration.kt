package com.jehutyno.yomikata.repository.migration

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jehutyno.yomikata.repository.local.YomikataDataBase
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import com.jehutyno.yomikata.util.CopyUtils
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.UpdateProgressDialog
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes


/**
 * Handle old encrypted database
 *
 * Overwrites the normal database with the decrypted version of an old encrypted database
 * This must be called before allowing any Room calls, since Room will throw an error
 * when receiving an encrypted database.
 *
 * @param context Context
 * @param filePathEncrypted Path to old version of database which is encrypted
 *
 */
@Synchronized
private fun handleOldEncryptedDatabase(context: Context, filePathEncrypted: String) {
    // backup!
    YomikataDataBase.createLocalBackup(context)
    val filePath = YomikataDataBase.getDatabaseFile(context).absolutePath
    if (filePathEncrypted.isNotEmpty()) {
        val file = File(filePathEncrypted)
        try {
            CopyUtils.restoreEncryptedBdd(file, filePath)
        } catch (e: Exception) {
            return
        }
    }
}

/**
 * With version12.
 *
 * Executes the block of code with a provided instance of the database
 * at version 12. Will close and delete the v12 database file afterward.
 *
 * @param R any return value
 * @param block a lambda with SQLiteDatabase of version 12 as input
 * @receiver the return value R
 */
private fun <R> Context.withVersion12(block: (SQLiteDatabase) -> R) {
    val assetManager = assets
    val dbName = "yomikataz_version12.db"
    val dbPath = getDatabasePath(dbName).path

    // copy the assets database v12 into the database folder to use it
    if (!File(dbPath).exists()) {
        assetManager.open(dbName).use { inputStream ->
            FileOutputStream(dbPath).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    val databaseVersion12 = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

    try {
        block(databaseVersion12)
    } finally {
        databaseVersion12.close()
        File(dbPath).delete()
    }
}

/**
 * Import yomikata.
 *
 * Imports old versions of yomikata database. Merges the progress of words into the
 * database of version 12
 *
 * @param context Context
 * @param path String of absolute path to old (encrypted) database file
 * @param updateProgressDialog An optional progress dialog to update
 */
@Synchronized
fun importYomikata(context: Context, path: String, updateProgressDialog: UpdateProgressDialog?) {
    // decrypt file and place it in a temporary file
    val oldDecryptedPath = kotlin.io.path.createTempFile("temp-decrypted-db-", ".db")
    CopyUtils.restoreEncryptedBdd(File(path), oldDecryptedPath.pathString)
    importYomikata(context, oldDecryptedPath.readBytes(), updateProgressDialog)
}

/**
 * Import yomikata.
 *
 * Imports old versions of yomikata database. Merges the progress of words into the
 * database of version 12
 *
 * @param context Context
 * @param data Raw data bytes containing the decrypted database
 * @param updateProgressDialog An optional progress dialog to update
 */
@Synchronized
fun importYomikata(context: Context, data: ByteArray, updateProgressDialog: UpdateProgressDialog?) {
    // place data in temp file
    val oldDecryptedPath = kotlin.io.path.createTempFile("temp-db-", ".db")
    oldDecryptedPath.writeBytes(data)
    // open temp file as database
    val oldDatabase = SQLiteDatabase.openDatabase (
        oldDecryptedPath.pathString, null, SQLiteDatabase.OPEN_READONLY
    )
    // open version 12 of the database
    context.withVersion12 {newDatabase ->
        val migrationSource = MigrationSource(oldDatabase, newDatabase)
        val wordTables = MigrationTable.allTables(MigrationTables.values())

        // initialize progress dialog
        updateProgressDialog?.setMax(wordTables.count())
        var progress = 0
        updateProgressDialog?.updateProgress(progress)

        wordTables.forEach {
            val wordTable = migrationSource.getWordTable(it)
            wordTable.forEach { word ->
                if (word.counterTry > 0 || word.priority > 0)
                    migrationSource.restoreWord(word.word, word.pronunciation, word)
            }
            progress++
            updateProgressDialog?.updateProgress(progress)
        }
        // overwrite the real database file with the new version
        newDatabase.close()
        YomikataDataBase.overwriteDatabase(context, newDatabase.path)
    }
}

/**
 * Update old database to version 12.
 *
 * Databases of version <= 12 were not created with Room and therefore do not have
 * exported schemas. Any database with version < 12 will first be updated to version 12
 * using this function. The update happens by comparing the current database to a
 * "checkpoint" database (see assets folder) and then merging them by keeping the
 * current user-specific settings (word points, level, etc.) but updating it to contain
 * the new words, sentences, etc. that may exist.
 */
@Synchronized
fun updateOldDBtoVersion12(oldDatabase: SupportSQLiteDatabase, context: Context,
                           filePathEncrypted: String = "") {
    // Do not use any externally defined daos, entities, models, etc.
    // since they may change in the future.
    val pref = PreferenceManager.getDefaultSharedPreferences(context)
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed(
        {
            pref.edit().putBoolean(Prefs.DB_UPDATE_ONGOING.pref, true).apply()
        }, 2000)

//    File(context.getString(R.string.db_path) + UpdateSQLiteHelper.UPDATE_DATABASE_NAME).delete()

    handleOldEncryptedDatabase(context, filePathEncrypted)
    pref.edit().remove(Prefs.DB_UPDATE_FILE.pref).apply()

    var words: List<Wordv12>? = null
    var quizzes: List<Quizv12>? = null
    var kanjiSolo: List<KanjiSolov12>? = null
    var radicals: List<Radicalv12>? = null
    var quizWords: List<QuizWordv12>? = null
    var sentences: List<Sentencev12>? = null

    context.withVersion12 {checkPointDatabase ->
        // get new data (version 12)
        words = Wordv12.getAllItems(checkPointDatabase).sortedBy(Wordv12::id)
        quizzes = Quizv12.getAllItems(checkPointDatabase).sortedBy(Quizv12::id)
        kanjiSolo = KanjiSolov12.getAllItems(checkPointDatabase).sortedBy(KanjiSolov12::id)
        radicals = Radicalv12.getAllItems(checkPointDatabase).sortedBy(Radicalv12::id)
        quizWords = QuizWordv12.getAllItems(checkPointDatabase).sortedBy(QuizWordv12::id)
        sentences = Sentencev12.getAllItems(checkPointDatabase).sortedBy(Sentencev12::id)
    }

    // TODO: does old stats need to be updated in any way?
//    val oldStatsCount = oldDatabase.query("""SELECT COUNT(*) FROM stat_entry""").run {
//        this.moveToFirst()
//        this.getInt(0)
//    }
//    val oldKanjiSoloCount = oldDatabase.query("""SELECT COUNT(*) FROM kanji_solo""").run {
//        this.moveToFirst()
//        this.getInt(0)
//    }
//    val oldRadCount = oldDatabase.query("""SELECT COUNT(*) FROM radicals""").run {
//        this.moveToFirst()
//        this.getInt(0)
//    }

    var progress = 0

    val maxProgress =    // total number of rows to update, used to display a progressBar
        words!!.size + quizWords!!.size + quizzes!!.size + kanjiSolo!!.size + sentences!!.size

    val intent = Intent()
    intent.action = QuizzesActivity.UPDATE_INTENT
    intent.putExtra(QuizzesActivity.UPDATE_COUNT, maxProgress)
    intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, progress)
//    context.sendBroadcast(intent)

    fun updateProgress() {  // call each time an item is updated to synchronize progressBar
        progress++
        if (progress % 100 == 0) {
            intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, progress)
//            context.sendBroadcast(intent)
        }
    }

    // -- update method --
    // loop through newest list:
    //      if element exist in old list: update the non user-specific fields
    //      else (element does not exist in old list): add it

    val oldWords = Wordv12.getAllItems(oldDatabase).sortedBy(Wordv12::id).toMutableList()
    words!!.forEach { word ->
        val oldWord = oldWords.firstOrNull { it.id == word.id }
        oldWords.remove(oldWord)
        if (oldWord == null) {
            Wordv12.insertWord(oldDatabase, word, false)
        } else {
            Wordv12.updateWord(oldDatabase, oldWord.id, word)
        }

        updateProgress()
    }

    val quizIdsMap = mutableMapOf<Long, Long>() // store new indices, since they are coupled to
    // words via quiz_words
    val oldQuizzes = Quizv12.getAllItems(oldDatabase).sortedBy(Quizv12::id).toMutableList()
    quizzes!!.forEach { quiz ->
        val matchOldQuiz = oldQuizzes.firstOrNull { it.id == quiz.id }
        oldQuizzes.remove(matchOldQuiz)
        if (matchOldQuiz == null) {             // did not find in old quiz -> insert
            val insertId = Quizv12.insertQuiz(oldDatabase, quiz, false)
            quizIdsMap[quiz.id] = insertId
        } else {
            quizIdsMap[quiz.id] = quiz.id
        }

        updateProgress()
    }

    // Assuming all old words and quizzes keep their original id -> no problem
    // Any new ids or ids that changed must be handled here
    quizWords!!.forEach {
        if (!QuizWordv12.quizWordExists(oldDatabase, it.quizId, it.wordId)) {
            QuizWordv12.addQuizWord(oldDatabase, quizIdsMap[it.quizId]!!, it.wordId)
        }

        updateProgress()
    }

    // fully replace, this is safe since there is no user-specific data,
    // or any defined relations to other tables
    KanjiSolov12.deleteAll(oldDatabase)
    kanjiSolo!!.forEach {
        KanjiSolov12.addKanjiSolo(oldDatabase, it, true)

        updateProgress()
    }
    Radicalv12.deleteAll(oldDatabase)
    radicals!!.forEach {
        Radicalv12.addRadical(oldDatabase, it, true)

        updateProgress()
    }
    Sentencev12.deleteAll(oldDatabase)  // sentence ids are referenced in words!
    // however, since the word sentenceIds have already
    // been updated, fully replacing the sentences is fine
    sentences!!.forEach {
        Sentencev12.addSentence(oldDatabase, it, true)

        updateProgress()
    }

    intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, maxProgress + 1)
    intent.putExtra(QuizzesActivity.UPDATE_FINISHED, true)
//    context.sendBroadcast(intent)

    pref.edit().putBoolean(Prefs.DB_UPDATE_ONGOING.pref, false).apply()

}
