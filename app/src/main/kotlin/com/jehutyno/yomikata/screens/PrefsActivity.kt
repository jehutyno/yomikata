package com.jehutyno.yomikata.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.repository.local.YomikataDataBase
import com.jehutyno.yomikata.repository.migration.importYomikata
import com.jehutyno.yomikata.util.*
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PrefsFragment()).commit()

        backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK)
                return@registerForActivityResult

            val updateProgressDialog = UpdateProgressDialog(this)
            updateProgressDialog.prepare("creating backup")
            updateProgressDialog.finishDialog = alertDialog {
                title = "Successfully created backup"
                okButton()
            }

            val uri = result.data?.data
            if (uri != null)
                handleBackup(uri, updateProgressDialog)
        }

        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK)
                return@registerForActivityResult

            val updateProgressDialog = UpdateProgressDialog(this)
            updateProgressDialog.finishCallback = {
                // tell quizzes activity to start in home screen fragment
                val intent = Intent()
                intent.putExtra("gotoCategory", Categories.HOME)
                setResult(RESULT_OK, intent)
            }
            updateProgressDialog.finishDialog = alertDialog {
                title = "Successfully restored your backup"
                message = "Please restart the app to reload your data"
                setCancelable(false)
                positiveButton(R.string.alert_restart) {
                    triggerRebirth()
                }
            }
            updateProgressDialog.prepare("Restoring your backup", "Do not close the app")
            updateProgressDialog.show()

            val updateProgressDialogMigrate = UpdateProgressDialog(this@PrefsActivity)
            updateProgressDialogMigrate.prepare("Migrating your database", "This may take a while...")
            updateProgressDialogMigrate.destroyOnFinish = true

            CoroutineScope(Dispatchers.Main).launch {
                val inputStream =
                    result.data?.data?.let { uri ->
                        val contentResolver = this@PrefsActivity.contentResolver
                        contentResolver.openInputStream(uri)
                    }

                val success = withContext(Dispatchers.IO) {
                    return@withContext handleRestore(inputStream!!, updateProgressDialog)
                }
                if (!success)
                    return@launch

                // do migration
                YomikataDataBase.updateProgressDialogGetter = {
                    updateProgressDialogMigrate
                }
                updateProgressDialogMigrate.show()
                withContext(Dispatchers.IO) {
                    YomikataDataBase.forceLoadDatabase(this@PrefsActivity)
                }
                updateProgressDialogMigrate.destroy()
                YomikataDataBase.updateProgressDialogGetter = null

                updateProgressDialog.updateProgress(100)
            }
        }

    }

    /**
     * Handle backup
     *
     * Create a backup of the current database file to the given uri.
     *
     * @param uri Uri
     * @param updateProgressDialog Optional dialog to display progress on screen
     */
    private fun handleBackup(uri: Uri, updateProgressDialog: UpdateProgressDialog? = null) {
        // start a progress dialog
        updateProgressDialog?.show()

        CoroutineScope(Dispatchers.Main).launch {
            var data: ByteArray? = null
            var stop = withContext(Dispatchers.IO) {
                try {
                    data = YomikataDataBase.getRawData(this@PrefsActivity)
                } catch (e: Exception) {
                    updateProgressDialog?.error("failed to create backup", e.message)
                    return@withContext true
                }
                return@withContext false
            }
            if (stop)
                return@launch

            updateProgressDialog?.updateProgress(50)

            stop = withContext(Dispatchers.IO) {
                var outputStream: OutputStream? = null
                try {
                    outputStream = contentResolver.openOutputStream(uri)!!
                    outputStream.write(data)
                } catch (e: IOException) {
                    updateProgressDialog?.error("failed to create backup", e.message)
                    return@withContext true
                } finally {
                    outputStream?.close()
                }
                return@withContext false
            }
            if (stop)
                return@launch

            updateProgressDialog?.updateProgress(100)
        }
    }

    /**
     * Handle restore
     *
     * @param inputStream InputStream containing data to restore
     * @param updateProgressDialog Optional dialog to show progress
     * @return True if success, False if failure
     */
    private fun handleRestore(inputStream: InputStream, updateProgressDialog: UpdateProgressDialog? = null): Boolean {
        val buffer = ByteArray(4096)
        val outputStream = ByteArrayOutputStream()

        val databaseFile = File.createTempFile("temp-db-", ".db")
        val outputStreamTemp = FileOutputStream(databaseFile)   // for validation
        try {
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                outputStream.write(buffer, 0, len)
            }
            updateProgressDialog?.updateProgress(50)
            val data = outputStream.toByteArray()

            outputStreamTemp.write(data)
            val type: DatabaseType?
            try {
                type = validateDatabase(databaseFile)
            } catch (e: IllegalStateException) {
                updateProgressDialog?.error("Invalid file", e.message)
                return false
            } catch (e: SQLiteException) {
                updateProgressDialog?.error("Something went wrong", e.message + e.cause?.message)
                return false
            }

            if (type == DatabaseType.OLD_YOMIKATA) {
                importYomikata(this@PrefsActivity, data, null)
            } else {
                YomikataDataBase.overwriteDatabase(this@PrefsActivity, data)
            }

            updateProgressDialog?.updateProgress(75)
        } catch (e: IOException) {
            e.printStackTrace()
            updateProgressDialog?.error("failed to restore database", e.message)
            return false
        } finally {
            inputStream.close()
            outputStream.close()
            outputStreamTemp.close()
        }
        return true
    }

    class PrefsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
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
                    (activity as PrefsActivity).backupProgress()
                    return true
                }
                "restore" -> {
                    (activity as PrefsActivity).restoreProgress()
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

    private fun triggerRebirth() {
        val packageManager: PackageManager = this.packageManager
        val intent = packageManager.getLaunchIntentForPackage(this.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        this.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

}
