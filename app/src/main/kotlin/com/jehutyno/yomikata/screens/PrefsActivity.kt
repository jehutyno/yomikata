package com.jehutyno.yomikata.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.repository.local.WordSource
import com.jehutyno.yomikata.repository.local.YomikataDataBase
import com.jehutyno.yomikata.repository.migration.MigrationSource
import com.jehutyno.yomikata.repository.migration.MigrationTable
import com.jehutyno.yomikata.repository.migration.MigrationTables
import com.jehutyno.yomikata.repository.migration.OldDataBase
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.Extras.PERMISSIONS_STORAGE
import com.jehutyno.yomikata.util.Extras.REQUEST_EXTERNAL_STORAGE_BACKUP
import com.jehutyno.yomikata.util.Extras.REQUEST_EXTERNAL_STORAGE_RESTORE
import com.wooplr.spotlight.prefs.PreferencesManager
import kotlinx.coroutines.*
import mu.KLogging
import splitties.alertdialog.appcompat.*
import java.io.*


/**
 * Created by valentin on 30/11/2016.
 */
class PrefsActivity : AppCompatActivity() {

    companion object : KLogging()

    private lateinit var backupLauncher : ActivityResultLauncher<Intent>
    private lateinit var restoreLauncher : ActivityResultLauncher<Intent>

    // alertDialog for progressBar: shows progress of data backup or migration
    private var progressAlertDialog: AlertDialog? = null
    private lateinit var progressBar: ProgressBar
    private val MAX_PROGRESS = 100

    private fun showProgress() {
        if (progressAlertDialog == null) {
            progressBar.progress = 0

            progressAlertDialog = alertDialog {
                titleResource = R.string.progress_bdd_update_title
                messageResource = R.string.progress_bdd_update_message
                setCancelable(false)
                setView(progressBar)
            }
            progressAlertDialog!!.show()
        }
        else {
            throw Error("Tried to create multiple progress dialogs simultaneously")
        }
    }

    private fun updateProgress(newProgress: Int) {
        progressBar.progress = newProgress
        if (newProgress == MAX_PROGRESS) {
            finishProgress()
        }
    }

    private fun errorProgress(errorTitle: String?, errorMessage: String?) {
        (progressBar.parent as ViewGroup).removeView(progressBar)
        progressAlertDialog!!.dismiss()
        progressAlertDialog = null
        alertDialog {
            title = errorTitle
            message= "The following error occured:\n$errorMessage"
            okButton()
        }.show()
    }

    private fun finishProgress() {
        (progressBar.parent as ViewGroup).removeView(progressBar)
        progressAlertDialog!!.dismiss()
        progressAlertDialog = null
        alertDialog {
            titleResource = R.string.update_success_title
            messageResource = R.string.update_success_message
            okButton()
        }.show()
        // tell quizzes activity to start in home screen fragment
        val intent = Intent()
        intent.putExtra("gotoCategory", Categories.HOME)
        setResult(RESULT_OK, intent)
        YomikataDataBase.forceLoadDatabase(this)
    }

    private fun validateDatabaseFile(database: File) {
        var db : SQLiteDatabase? = null
        try {
            // attempt to open the database file
            db = SQLiteDatabase.openDatabase(database.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

            // check the database schema version
//            val cursor = db.rawQuery("PRAGMA schema_version", null)
//            if (cursor.moveToFirst()) {
//                val schemaVersion = cursor.getInt(0)
//                val expectedSchemaVersions = listOf(1, 2, 3)
//                if (!expectedSchemaVersions.contains(schemaVersion)) {
//                    throw IllegalStateException("Unexpected database schema version: $schemaVersion")
//                }
//            } else {
//                throw IllegalStateException("Unable to retrieve database schema version")
//            }
//            cursor.close()

            // check for tables
            val tableCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'" +
                                                                             "AND name IN (?, ?)",
                                            arrayOf("words", "quiz"))
            if (!tableCursor.moveToFirst()) {
                throw IllegalStateException("Database does not contain some table(s)")
            }
            tableCursor.close()

        } catch (e: SQLiteException) {
            throw IllegalStateException("Unable to open database file, please verify it is a database " +
                                        "file ending in .db", e)
        } finally {
            db?.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PrefsFragment()).commit()

        backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK)
                return@registerForActivityResult

            // start a progress dialog
            showProgress()

            CoroutineScope(Dispatchers.Main).launch {
                var data: ByteArray? = null
                val em = withContext(Dispatchers.IO) {
                    try {
                        data = YomikataDataBase.getRawData(this@PrefsActivity)
                    } catch (e: Exception) {
                        return@withContext e.message
                    }
                    return@withContext null
                }

                if (em != null) {
                    errorProgress("failed to create backup", em)
                    return@launch
                }
                updateProgress(50)

                result.data?.data?.also { uri ->
                    var outputStream: OutputStream? = null
                    try {
                        outputStream = contentResolver.openOutputStream(uri)!!
                        outputStream.write(data)
                    } catch (e: IOException) {
                        errorProgress("failed to create backup", e.message)
                        return@launch
                    } finally {
                        outputStream?.close()
                    }
                }
                updateProgress(100)
            }

        }

        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK)
                return@registerForActivityResult

            showProgress()

            CoroutineScope(Dispatchers.Main).launch {
                val inputStream =
                    result.data?.data?.let { uri ->
                        val contentResolver = this@PrefsActivity.contentResolver
                        contentResolver.openInputStream(uri)
                    }

                val buffer = ByteArray(4096)
                val outputStream = ByteArrayOutputStream()

                val titEm = withContext(Dispatchers.IO) {
                    val databaseFile = File.createTempFile("temp-db-", ".db")
                    val outputStreamTemp = FileOutputStream(databaseFile)
                    try {
                        var len: Int
                        while (inputStream?.read(buffer).also { len = it ?: -1 } != -1) {
                            outputStream.write(buffer, 0, len)
                        }
                        updateProgress(50)
                        val data = outputStream.toByteArray()

                        outputStreamTemp.write(data)

                        try {
                            validateDatabaseFile(databaseFile)
                        } catch (e: IllegalStateException) {
                            return@withContext Pair("Invalid file", e.message)
                        } catch (e: SQLiteException) {
                            return@withContext Pair("Something went wrong", e.message + e.cause?.message)
                        }

                        YomikataDataBase.overwriteDatabase(this@PrefsActivity, data)
                        updateProgress(75)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        return@withContext Pair("failed to restore database", e.message)
                    } finally {
                        inputStream?.close()
                        outputStream.close()
                        outputStreamTemp.close()
                    }
                    Pair(null, null)
                }

                if (titEm.first != null) {
                    errorProgress(titEm.first, titEm.second)
                    return@launch
                }
                updateProgress(100)
            }
        }

        // progressBar for database update
        progressBar = ProgressBar(this, null, android.R.style.Widget_ProgressBar_Horizontal)
        progressBar.setPadding(40, progressBar.paddingTop, 40, progressBar.paddingBottom)
        progressBar.max = 100
    }

    class PrefsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }

        /**
         * Checks if we have the Manifest.permission.WRITE_EXTERNAL_STORAGE permission.
         * If not, then prompt the user.
         * Returns true if permission exists, and false otherwise.
         */
        private fun checkAndRequestPermission(requestCode: Int): Boolean {
            val permission = ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        requireActivity(),
                        PERMISSIONS_STORAGE,
                        requestCode
                )
            }
            return permission == PackageManager.PERMISSION_GRANTED
        }

        private fun getResetAlert() : AlertDialog {
            return requireContext().alertDialog {
                messageResource = R.string.prefs_reinit_sure
                okButton {
                    YomikataDataBase.resetDatabase(requireContext())
                    YomikataDataBase.forceLoadDatabase(requireContext())
                    val toast = Toast.makeText(context, R.string.prefs_reinit_done, Toast.LENGTH_LONG)
                    toast.show()
                    requireActivity().finish()
                }
                cancelButton()
            }
        }

        private fun getDeleteVoicesAlert() : AlertDialog {
            return requireContext().alertDialog {
                messageResource = R.string.prefs_delete_voices_sure
                okButton {
                    FileUtils.deleteFolder(activity, "Voices")
                    val pref = PreferenceManager.getDefaultSharedPreferences(context)
                    for (i in 0 until 7) {
                        pref.edit().putBoolean(
                                "${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(i)}_$i", false
                        ).apply()
                    }
                    val toast = Toast.makeText(context, R.string.voices_reinit_done, Toast.LENGTH_LONG)
                    toast.show()
                    requireActivity().finish()
                }
                cancelButton()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                // Quiz Settings
                "font_size" -> {
                    return true
                }
                "input_change" -> {
                    return true
                }
                "speed" -> {
                    return true
                }
                "length" -> {
                    return true
                }
                "play_start" -> {
                    return true
                }
                "play_end" -> {
                    return true
                }

                // Tutorials
                "reset_tuto" -> {
                    PreferencesManager(activity).resetAll()
                    requireActivity().setResult(RESULT_OK)
                    requireActivity().finish()
                    return true
                }

                // Backup and Restore
                "backup" -> {
                    if (checkAndRequestPermission(REQUEST_EXTERNAL_STORAGE_BACKUP)) {
                        (activity as PrefsActivity).backupProgress()
                    }
                    return true
                }
                "restore" -> {
                    if (checkAndRequestPermission(REQUEST_EXTERNAL_STORAGE_RESTORE)) {
                        (activity as PrefsActivity).restoreProgress()
                    }
                    return true
                }
                "reset" -> {
                    getResetAlert().show()
                    return true
                }
                "delete_voices" -> {
                    getDeleteVoicesAlert().show()
                    return true
                }

                // Others
                "privacy" -> {
                    val privacyString = "https://cdn.rawgit.com/jehutyno/privacy-policies/56b6fcf3/PrivacyPolicyYomikataZ.html"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyString))
                    startActivity(browserIntent)
                    return true
                }

                else -> {
                    throw Error("unknown preference key: ${preference.key}")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQUEST_EXTERNAL_STORAGE_RESTORE -> restoreProgress()
                REQUEST_EXTERNAL_STORAGE_BACKUP -> backupProgress()
            }
        }
    }

    fun backupProgress() {
        fun createFile(pickerInitialUri: Uri?) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.sqlite3"
                putExtra(Intent.EXTRA_TITLE, "my_yomikataz.db")

                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker before your app creates the document.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (pickerInitialUri != null)
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
                }
            }
            backupLauncher.launch(intent)
        }

        createFile(null)
    }

    fun restoreProgress() {
        fun openFile(pickerInitialUri: Uri?) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.sqlite3"
                val mimeTypes = arrayOf("application/x-sqlite3", "*/*")
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                putExtra(Intent.EXTRA_TITLE, "Select Your Yomikata Database File")

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (pickerInitialUri != null)
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
                }
            }
            restoreLauncher.launch(intent)
        }

        openFile(null)
    }

    private fun importYomikata(path: String?) {
        val pathSegments = path?.split("/")
        val toPath = getString(R.string.db_path)
        val toName = pathSegments!![pathSegments.count() - 1] + ".decrypted"
        try {
            CopyUtils.reinitDataBase(this)
            CopyUtils.restoreEncryptedBdd(File(path), toPath + toName)
            val oldDatabase = OldDataBase.getDatabase(this)
            val migrationDao = oldDatabase.migrationDao()
            val migrationSource = MigrationSource(migrationDao)
            val wordTables = MigrationTable.allTables(MigrationTables.values())

            val progressBar1 = ProgressBar(this, null, android.R.style.Widget_ProgressBar_Horizontal)
            progressBar1.setPadding(40, progressBar1.paddingTop, 40, progressBar1.paddingBottom)
            progressBar1.max = wordTables.count()

            val progressAlertDialog = alertDialog {
                titleResource = R.string.progress_import_title
                messageResource = R.string.progress_import_message_y
                setCancelable(false)
                setView(progressBar1)
            }
            progressAlertDialog.show()

            MainScope().launch {
                wordTables.forEach {
                    val wordTable = migrationSource.getWordTable(it)
                    progressBar1.incrementProgressBy(1)
                    wordTable.forEach { word ->
                        val wordDao = YomikataDataBase.getDatabase(this@PrefsActivity).wordDao()
                        val source = WordSource(wordDao)
                        if (word.counterTry > 0 || word.priority > 0)
                            source.restoreWord(word.word, word.pronunciation, word)
                    }
                }
                File(toPath + toName).delete()

                progressAlertDialog.dismiss()
                alertDialog {
                    titleResource = R.string.restore_success
                    messageResource = R.string.restore_success_message
                    okButton()
                }.show()
            }
        } catch (exception: Exception) {
            alertDialog {
                titleResource = R.string.restore_error
                messageResource = R.string.restore_error_message
                okButton()
            }.show()
        }
    }

    private fun importYomikataZ(path: String) {
        YomikataDataBase.overwriteDatabase(this, path)
        finish()
    }

}
