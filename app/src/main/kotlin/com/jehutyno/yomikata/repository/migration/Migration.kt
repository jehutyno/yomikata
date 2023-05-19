package com.jehutyno.yomikata.repository.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import com.jehutyno.yomikata.util.CopyUtils
import com.jehutyno.yomikata.util.UpdateProgressDialog
import java.io.File
import kotlin.io.path.deleteIfExists
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
 */
@Synchronized
private fun handleOldEncryptedDatabase(context: Context, filePathEncrypted: String) {
    // backup!
    YomikataDatabase.createLocalBackup(context)
    val filePath = YomikataDatabase.getDatabaseFile(context).absolutePath
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
 * Get temp version13.
 *
 * Returns a temporary file of the version 13 database from the assets folder.
 *
 * @param context Context
 * @return Temporary file of version 13 database
 */
private fun getTempVersion13(context: Context): File {
    val assetManager = context.assets
    val dbAssetName = "yomikataz_version13.db"
    val tempDbFile = File.createTempFile("temp-v13-db-", ".db")

    // copy the assets database v13 into a temporary file
    assetManager.open(dbAssetName).use { inputStream ->
        inputStream.copyTo(tempDbFile.outputStream())
    }

    return tempDbFile
}

/**
 * Import yomikata.
 *
 * Imports old versions of yomikata database. Merges the progress of words into the
 * database of version 13
 *
 * @param context Context
 * @param path String of absolute path to old (encrypted) database file
 * @param updateProgressDialog An optional progress dialog to update
 */
@Synchronized
fun importYomikata(context: Context, path: String,
                   updateProgressDialog: UpdateProgressDialog?, create_backup: Boolean = true) {
    // decrypt file and place it in a temporary file
    val oldDecryptedPath = kotlin.io.path.createTempFile("temp-decrypted-db-", ".db")
    CopyUtils.restoreEncryptedBdd(File(path), oldDecryptedPath.pathString)
    importYomikata(context, oldDecryptedPath.readBytes(), updateProgressDialog, create_backup)
    oldDecryptedPath.deleteIfExists()
}

/**
 * Import yomikata.
 *
 * Imports old versions of yomikata database. Merges the progress of words into the
 * database of version 13
 *
 * @param context Context
 * @param data Raw data bytes containing the decrypted database
 * @param updateProgressDialog An optional progress dialog to update
 */
@Synchronized
fun importYomikata(context: Context, data: ByteArray,
                   updateProgressDialog: UpdateProgressDialog?, create_backup: Boolean = true) {
    // place data in temp file
    val oldDecryptedPath = kotlin.io.path.createTempFile("temp-db-", ".db")
    oldDecryptedPath.writeBytes(data)
    // open temp file as database
    val oldDatabase = SQLiteDatabase.openDatabase (
        oldDecryptedPath.pathString, null, OPEN_READONLY
    )
    // open version 13 of the database
    val v13File = getTempVersion13(context)
    val newDatabase = SQLiteDatabase.openDatabase(v13File.absolutePath, null, OPEN_READWRITE)
    try {
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
        YomikataDatabase.overwriteDatabase(context, v13File.absolutePath, create_backup)
    } finally {
        oldDatabase.close()
        oldDecryptedPath.deleteIfExists()
        newDatabase.close()
        v13File.delete()
    }
}

/**
 * Update old database to version 13.
 *
 * Databases of version <= 13 were not created with Room and therefore do not have
 * exported schemas. Any database with version < 13 will first be updated to version 13
 * using this function. The update happens by comparing the current database to a
 * "checkpoint" database (see assets folder) and then merging them by keeping the
 * current user-specific settings (word points, level, etc.) but updating it to contain
 * the new words, sentences, etc. that may exist.
 */
@Synchronized
fun updateOldDBtoVersion13(oldDatabase: SupportSQLiteDatabase, context: Context,
                           updateProgressDialog: UpdateProgressDialog?) {
    // Do not use any externally (outside of migration folder) defined daos, entities, models, etc.
    // since they may change in the future.

    // get version13 which will be used as the correct database to merge with user's oldDatabase
    val v13File = getTempVersion13(context)
    val newDatabase = SQLiteDatabase.openDatabase(v13File.absolutePath, null, OPEN_READWRITE)
    try {
        val words = Wordv13.getAllItems(newDatabase).sortedBy(Wordv13::id)
        val quizzes = Quizv13.getAllItems(newDatabase).sortedBy(Quizv13::id)
        val kanjiSolo = KanjiSolov13.getAllItems(newDatabase).sortedBy(KanjiSolov13::id)
        val radicals = Radicalv13.getAllItems(newDatabase).sortedBy(Radicalv13::id)
        val quizWords = QuizWordv13.getAllItems(newDatabase).sortedBy(QuizWordv13::id)
        val sentences = Sentencev13.getAllItems(newDatabase).sortedBy(Sentencev13::id)

        val oldQuizWordsSize = oldDatabase.query("""SELECT COUNT(*) FROM quiz_word""").use {
            it.moveToFirst()
            it.getInt(0)
        }

        var progress = 0
        val maxProgress =    // total number of rows to update, used to display a progressBar
            words.size + quizzes.size + quizWords.size + oldQuizWordsSize +
            kanjiSolo.size + radicals.size + sentences.size
        updateProgressDialog?.setMax(maxProgress)

        fun updateProgress() {  // call each time an item is updated to synchronize progressBar
            progress++
            if (progress % 100 == 0) {
                updateProgressDialog?.updateProgress(progress)
            }
        }

        // -- update method --
        // loop through newest list:
        //      if element exists in old list:
        //          override user-specific values of new with old values
        //
        // any values that exist in old but not in new will be discarded,
        // except for quiz and quiz_words which correspond to user selections

        // NOTE: AUTOINCREMENT is on for words, quiz, and quiz_word: use resetAutoIncrement !!

        val oldWords = Wordv13.getAllItems(oldDatabase).sortedBy(Wordv13::id).toMutableList()
        Wordv13.deleteAll(oldDatabase)
        Wordv13.resetAutoIncrement(oldDatabase)
        // store all new ids, which is needed in quiz_words update (see further down)
        val wordIdsMap = mutableMapOf<Long, Long>()
        words.forEach { word ->
            val oldWord = oldWords.firstOrNull { it.baseEquals(word) }
            oldWords.remove(oldWord)

            val updatedWord =
                if (oldWord != null) {
                    Wordv13(word, oldWord)
                } else {
                    word
                }
            val newId = Wordv13.insertWord(oldDatabase, updatedWord, true)
            if (oldWord != null)
                wordIdsMap[oldWord.id] = newId

            updateProgress()
        }

        // store new indices, since they are coupled to
        // words via quiz_words
        val oldQuizzes = Quizv13.getAllItems(oldDatabase).sortedBy(Quizv13::id).toMutableList()
        Quizv13.deleteAll(oldDatabase)
        QuizWordv13.resetAutoIncrement(oldDatabase)
        quizzes.forEach { quiz ->
            val matchOldQuiz = oldQuizzes.firstOrNull { it.baseEquals(quiz) }
            oldQuizzes.remove(matchOldQuiz)

            // always insert new quiz (only isSelected column is user-specific, but doesn't matter)
            Quizv13.insertQuiz(oldDatabase, quiz, true)

            updateProgress()
        }
        // any oldQuizzes that remain have to be added if they are user created quizzes (category == 8)
        // store their original quizIds and their new quizId in a map
        val quizIdsMap = mutableMapOf<Long, Long>()
        oldQuizzes.forEach { oldQuiz ->
            if (oldQuiz.category == 8) {
                quizIdsMap[oldQuiz.id] = Quizv13.insertQuiz(oldDatabase, oldQuiz, false)
            }
            // other categories are discarded
        }

        // All new sentences and words have their new quiz_words inserted.
        // Any user selection quiz ids must be additionally added
        val oldQuizWords = QuizWordv13.getAllItems(oldDatabase)
        QuizWordv13.deleteAll(oldDatabase)
        QuizWordv13.resetAutoIncrement(oldDatabase)
        quizWords.forEach { quiz_word ->
            QuizWordv13.insertQuizWord(oldDatabase, quiz_word, true)

            updateProgress()
        }
        // search for old quiz_words that correspond to user selections (quizId is in quizIdsMap)
        oldQuizWords.forEach { old_quiz_word ->
            if (old_quiz_word.quizId in quizIdsMap.keys) {
                // use safe check (?) in case any quiz_words refer to non-existing words
                wordIdsMap[old_quiz_word.wordId]?.also {
                    val updatedQuizWord = QuizWordv13(0, quizIdsMap[old_quiz_word.quizId]!!, it)
                    // change word and quiz id to new ones
                    QuizWordv13.insertQuizWord(oldDatabase, updatedQuizWord, false)
                }
            }

            updateProgress()
        }

        // fully replace, this is safe since there is no user-specific data,
        // or any defined relations to other tables
        KanjiSolov13.deleteAll(oldDatabase)
        kanjiSolo.forEach {
            KanjiSolov13.addKanjiSolo(oldDatabase, it, true)

            updateProgress()
        }
        Radicalv13.deleteAll(oldDatabase)
        radicals.forEach {
            Radicalv13.addRadical(oldDatabase, it, true)

            updateProgress()
        }
        // sentence ids are referenced by words, which needs to be consistently updated
        // however, since the words use the sentence_id of the new (version 13) table,
        // this is already taken care of. Therefore, simply replace all with new values
        Sentencev13.deleteAll(oldDatabase)
        sentences.forEach {
            Sentencev13.addSentence(oldDatabase, it, true)

            updateProgress()
        }
    } finally {
        newDatabase.close()
        v13File.delete()
    }

}
