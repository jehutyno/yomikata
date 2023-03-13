package com.jehutyno.yomikata.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.local.*
import com.jehutyno.yomikata.repository.migration.DatabaseHelper
import com.jehutyno.yomikata.repository.migration.MigrationSource
import com.jehutyno.yomikata.repository.migration.MigrationTable
import com.jehutyno.yomikata.repository.migration.MigrationTables
import com.jehutyno.yomikata.screens.quizzes.QuizzesActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
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
    val context = this
    val toPath = getString(R.string.db_path)
    val toName = getString(R.string.db_name_yomi)
    val file = File("$toPath/$toName")
    if (file.exists()) {
        try {
            CopyUtils.reinitDataBase(this)
            val migrationSource = MigrationSource(this, DatabaseHelper.getInstance(this, toName, toPath))
            val wordTables = MigrationTable.allTables(MigrationTables.values())

            MainScope().async {
                wordTables.forEach {
                    val wordTable = migrationSource.getWordTable(it)
                    wordTable.forEach { word ->
                        val source = WordSource(context)
                        if (word.counterTry > 0 || word.priority > 0)
                            source.restoreWord(word.word, word.prononciation, word)
                    }
                }
                File(toPath + toName).delete()
            }
        } catch (exception: Exception) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.restore_error)
            builder.setMessage(R.string.restore_error_message)
            builder.setPositiveButton(R.string.ok) {_, _ -> }
            builder.show()
        }
    }
}

fun Context.updateBDD(db: SQLiteDatabase?, filePathEncrypted: String, oldVersion: Int) {
    val pref = PreferenceManager.getDefaultSharedPreferences(this)
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed(
        {
            pref.edit().putBoolean(Prefs.DB_UPDATE_ONGOING.pref, true).apply()
        }, 2000)
    var filePath = ""
    File(getString(R.string.db_path) + UpdateSQLiteHelper.UPDATE_DATABASE_NAME).delete()
    if (filePathEncrypted.isNotEmpty()) {
        val file = File(filePathEncrypted)
        filePath = filePathEncrypted.replace(".yomikataz", ".import")
        try {
            CopyUtils.restoreEncryptedBdd(file, filePath)
        } catch (e: Exception) {
            return
        }
    }
    pref.edit().putString(Prefs.DB_UPDATE_FILE.pref, filePath).apply()
    pref.edit().putInt(Prefs.DB_UPDATE_OLD_VERSION.pref, oldVersion).apply()
    val updateSource = UpdateSource(this, filePath)
    val updateWords = updateSource.getAllWords().sortedBy(Word::id)
    val stats = updateSource.getAllStatEntries()
    val updateQuizzes = updateSource.getAddedQuizzes()
    val updateKanjiSolo = updateSource.getAllKanjiSolo()
    val updateRadicals = updateSource.getAllRadicals()
    val updateQuizwords = updateSource.getAddedQuizWords()
    val updateSentences = updateSource.getAllSentences()
    val wordSource = WordSource(this)
    val quizSource = QuizSource(this)
    val kanjiSoloSource = KanjiSoloSource(this)
    val sentenceSource = SentenceSource(this)
    val statSource = StatsSource(this)
    val quizIdsMap = mutableMapOf<Long, Long>()
    kanjiSoloSource.createKanjiSoloTable(db)
    kanjiSoloSource.createRadicalsTable(db)
    val kanjiSoloCount = kanjiSoloSource.kanjiSoloCount(db)
    val radCount = kanjiSoloSource.radicalsCount(db)

    MainScope().async {
        var i = 0
        val intent = Intent()
        intent.action = QuizzesActivity.UPDATE_INTENT
        val totalCount =
            updateWords.size + (if (filePath.isEmpty()) 0 else stats.size) + updateQuizwords.size + updateQuizzes.size + (if (kanjiSoloCount < updateKanjiSolo.size) updateKanjiSolo.size else 0) + (updateSentences.size) // Update sentences
        intent.putExtra(QuizzesActivity.UPDATE_COUNT, totalCount)
        intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
        sendBroadcast(intent)
        if (oldVersion <= 8) {
            sentenceSource.createSentencesTable()
            wordSource.migration_8to9()
        }
        val words = wordSource.getAllWords(null).sortedBy(Word::id)

        updateWords.forEach {
            val word = words.getOrNull(i)
            if (filePath.isEmpty())
                wordSource.updateWord(it, word)
            else
                wordSource.updateWordProgression(it, word)
            i++
            if (i % 100 == 0) {
                intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                sendBroadcast(intent)
            }
        }
        if (filePath.isNotEmpty()) {
            statSource.removeAllStats()
            stats.forEach {
                statSource.addStatEntry(it)
                i++
                if (i % 100 == 0) {
                    intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                    sendBroadcast(intent)
                }
            }
        }

        updateQuizzes.forEach {
            val quiz = quizSource.getQuiz(it.id)
            if (quiz == null) {
                val id = quizSource.saveQuiz(it.getName(), it.category)
                quizIdsMap[id] = it.id
            } else {
                quizIdsMap[quiz.id] = it.id
            }
            i++
            if (i % 100 == 0) {
                intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                sendBroadcast(intent)
            }
        }

        updateQuizwords.forEach {
            if (wordSource.getQuizWordFromIds(it.quizId, it.wordId) == null) {
                if (it.quizId > 96) {
                    wordSource.addQuizWord(quizIdsMap[it.quizId]!!, it.wordId)
                } else {
                    wordSource.addQuizWord(it.quizId, it.wordId)
                }
            }
            i++
            if (i % 100 == 0) {
                intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                sendBroadcast(intent)
            }
        }

        if (kanjiSoloCount < updateKanjiSolo.size) {
            updateKanjiSolo.forEach {
                kanjiSoloSource.addKanjiSolo(it)
                i++
                if (i % 100 == 0) {
                    intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                    sendBroadcast(intent)
                }
            }
        }

        if (radCount < updateRadicals.size) {
            updateRadicals.forEach {
                kanjiSoloSource.addRadical(it)
                i++
                if (i % 100 == 0) {
                    intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                    sendBroadcast(intent)
                }
            }
        }

        if (oldVersion <= 8) {
            updateSentences.forEach {
                sentenceSource.addSentence(it)
                i++
                if (i % 100 == 0) {
                    intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                    sendBroadcast(intent)
                }
            }
        } else if (filePath.isEmpty()) {
            val sentences = sentenceSource.getAllSentences(null).sortedBy(Sentence::id)
            updateSentences.forEach {
                val sentence = sentences.getOrNull(i)
                sentenceSource.updateSentence(it, sentence)
                i++
                if (i % 100 == 0) {
                    intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, i)
                    sendBroadcast(intent)
                }
            }
        }

        intent.putExtra(QuizzesActivity.UPDATE_PROGRESS, totalCount + 1)
        intent.putExtra(QuizzesActivity.UPDATE_FINISHED, true)
        sendBroadcast(intent)

        pref.edit().putBoolean(Prefs.DB_UPDATE_ONGOING.pref, false).apply()
        pref.edit().putString(Prefs.DB_UPDATE_FILE.pref, "").apply()
        if (filePath.isEmpty())
            File(getString(R.string.db_path) + UpdateSQLiteHelper.UPDATE_DATABASE_NAME).delete()
        else
            File(filePath).delete()
    }

}

