package com.jehutyno.yomikata.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.repository.local.*
import com.jehutyno.yomikata.repository.migration.*
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource
import java.io.File


/**
 * Created by valentin on 03/10/2016.
 */

inline fun AppCompatActivity.fragmentTransaction(autocommit: Boolean = true, func: FragmentTransaction.() -> Unit): FragmentTransaction {
    val transaction = supportFragmentManager.beginTransaction()
    transaction.func()
    if (autocommit && !transaction.isEmpty) {
        transaction.commit()
    }
    return transaction
}

fun AppCompatActivity.addOrReplaceFragment(layoutId: Int, fragment: Fragment) {
    fragmentTransaction {
        if (supportFragmentManager.findFragmentById(layoutId) == null) {
            add(layoutId, fragment)
        } else {
            replace(layoutId, fragment)
        }
    }
}

fun Activity.hideSoftKeyboard() {
    val inputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (this.currentFocus != null)
        inputMethodManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken, 0)
}

fun Activity.migrateFromYomikata() {
    val toPath = getString(R.string.db_path)
    val toName = getString(R.string.db_name_yomi)
    val file = File("$toPath/$toName")
    if (file.exists()) {
        try {
            CopyUtils.reinitDataBase(this)
            val migrationDao = OldDataBase.getDatabase(this).migrationDao()
            val migrationSource = MigrationSource(migrationDao)
            val wordTables = MigrationTable.allTables(MigrationTables.values())

            MainScope().async {
                wordTables.forEach {
                    val wordTable = migrationSource.getWordTable(it)
                    wordTable.forEach { word ->
                        val wordDao = YomikataDataBase.getDatabase(this@migrateFromYomikata).wordDao()
                        val source = WordSource(wordDao)
                        if (word.counterTry > 0 || word.priority > 0)
                            source.restoreWord(word.word, word.pronunciation, word)
                    }
                }
                File(toPath + toName).delete()
            }
        } catch (exception: Exception) {
            alertDialog {
                titleResource = R.string.restore_error
                messageResource = R.string.restore_error_message
                okButton()
            }.show()
        }
    }
}

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
fun handleOldEncryptedDatabase(context: Context, filePathEncrypted: String) {
    val filePath = context.getDatabasePath("yomikataz.db").absolutePath
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
 * Update old database to version 12.
 * Databases of version <= 12 were not created with Room and therefore do not have
 * exported schemas. Any database with version < 12 will first be updated to version 12
 * using this function. The update happens by comparing the current database to a
 * "checkpoint" database (see assets folder) and then merging them by keeping the
 * current user-specific settings (word points, level, etc.) but updating it to contain
 * the new words, sentences, etc. that may exist.
 */
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

    val checkPointDataBase = SQLiteDatabase.openDatabase (
        "yomikataz_version12.db", null, OPEN_READONLY
    )

    // get new data (version 12)
    val words = Wordv12.getAllItems(checkPointDataBase).sortedBy(Wordv12::id)
    val quizzes = Quizv12.getAllItems(checkPointDataBase).sortedBy(Quizv12::id)
    val kanjiSolo = KanjiSolov12.getAllItems(checkPointDataBase).sortedBy(KanjiSolov12::id)
    val radicals = Radicalv12.getAllItems(checkPointDataBase).sortedBy(Radicalv12::id)
    val quizWords = QuizWordv12.getAllItems(checkPointDataBase).sortedBy(QuizWordv12::id)
    val sentences = Sentencev12.getAllItems(checkPointDataBase).sortedBy(Sentencev12::id)

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

    MainScope().async {
        var progress = 0

        val maxProgress =    // total number of rows to update, used to display a progressBar
                    words.size + quizWords.size + quizzes.size + kanjiSolo.size + sentences.size

        val intent = Intent()
        intent.action = QuizzesActivity.UPDATE_INTENT
        intent.putExtra(QuizzesActivity.UPDATE_COUNT, maxProgress)
        intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, progress)
        context.sendBroadcast(intent)

        fun updateProgress() {  // call each time an item is updated to synchronize progressBar
            progress++
            if (progress % 100 == 0) {
                intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, progress)
                context.sendBroadcast(intent)
            }
        }

        // -- update method --
        // loop through newest list:
        //      if element exist in old list: update the non user-specific fields
        //      else (element does not exist in old list): add it

        val oldWords = Wordv12.getAllItems(oldDatabase).sortedBy(Wordv12::id)
        words.forEach { word ->
            val oldWord = oldWords.firstOrNull { it.id == word.id }
            if (oldWord == null) {
                Wordv12.insertWord(oldDatabase, word, false)
            } else {
                Wordv12.updateWord(oldDatabase, oldWord.id, word)
            }

            updateProgress()
        }

        val quizIdsMap = mutableMapOf<Long, Long>() // store new indices, since they are coupled to
                                                    // words via quiz_words
        val oldQuizzes = Quizv12.getAllItems(oldDatabase).sortedBy(Quizv12::id)
        quizzes.forEach { quiz ->
            val matchOldQuiz = oldQuizzes.firstOrNull { it.id == quiz.id }
            if (matchOldQuiz == null) { // did not find in old quiz -> insert
                val insertId = Quizv12.insertQuiz(oldDatabase, quiz, false)
                quizIdsMap[quiz.id] = insertId
            } else {
                quizIdsMap[quiz.id] = quiz.id
            }

            updateProgress()
        }

        // Assuming all old words and quizzes keep their original id -> no problem
        // Any new ids or ids that changed must be handled here
        quizWords.forEach {
            if (QuizWordv12.quizWordExists(oldDatabase, it.quizId, it.wordId)) {
                QuizWordv12.addQuizWord(oldDatabase, quizIdsMap[it.quizId]!!, it.wordId)
            }

            updateProgress()
        }

        // fully replace, this is safe since there is no user-specific data,
        // or any defined relations to other tables
        KanjiSolov12.deleteAll(oldDatabase)
        kanjiSolo.forEach {
            KanjiSolov12.addKanjiSolo(oldDatabase, it, true)

            updateProgress()
        }
        Radicalv12.deleteAll(oldDatabase)
        radicals.forEach {
            Radicalv12.addRadical(oldDatabase, it, true)

            updateProgress()
        }
        Sentencev12.deleteAll(oldDatabase)  // sentence ids are referenced in words!
                                            // however, since the word sentenceIds have already
                                            // been updated, fully replacing the sentences is fine
        sentences.forEach {
            Sentencev12.addSentence(oldDatabase, it, true)

            updateProgress()
        }

        intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, maxProgress + 1)
        intent.putExtra(QuizzesActivity.UPDATE_FINISHED, true)
        context.sendBroadcast(intent)

        pref.edit().putBoolean(Prefs.DB_UPDATE_ONGOING.pref, false).apply()
    }

}
