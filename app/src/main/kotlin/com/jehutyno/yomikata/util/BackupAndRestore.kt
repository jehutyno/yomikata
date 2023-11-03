package com.jehutyno.yomikata.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import com.jehutyno.yomikata.repository.migration.importYomikata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


fun backupProgress(backupLauncher: ActivityResultLauncher<Intent>) {
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

fun Context.restoreProgress(restoreLauncher: ActivityResultLauncher<Intent>) {
    fun openFile(pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.sqlite3"
            val mimeTypes = arrayOf("application/x-sqlite3", "*/*")
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            putExtra(Intent.EXTRA_TITLE, getString(R.string.choose_file))

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


fun Activity.getBackupLauncher(result: ActivityResult) {
    if (result.resultCode != Activity.RESULT_OK)
        return

    val updateProgressDialog = UpdateProgressDialog(this)
    updateProgressDialog.prepare(getString(R.string.backup_progress))
    updateProgressDialog.finishDialog = alertDialog {
        titleResource = R.string.backup_success
        okButton()
    }

    val uri = result.data?.data
    if (uri != null)
        handleBackup(uri, updateProgressDialog)
}

/**
 * Handle backup
 *
 * Create a backup of the current database file to the given uri.
 *
 * @param uri Uri
 * @param updateProgressDialog Optional dialog to display progress on screen
 */
private fun Activity.handleBackup(uri: Uri, updateProgressDialog: UpdateProgressDialog? = null) {
    // start a progress dialog
    updateProgressDialog?.show()

    CoroutineScope(Dispatchers.Main).launch {
        var data: ByteArray? = null
        var errorMessage : String? = null

        var stop: Boolean = withContext(Dispatchers.IO) {
            try {
                data = YomikataDatabase.getRawData(this@handleBackup)
            } catch (e: Exception) {
                errorMessage = e.message
                return@withContext true
            }
            return@withContext false
        }
        if (stop) {
            updateProgressDialog?.error(getString(R.string.backup_error), errorMessage)
            return@launch
        }

        updateProgressDialog?.updateProgress(50)

        stop = withContext(Dispatchers.IO) {
            var outputStream: OutputStream? = null
            try {
                outputStream = contentResolver.openOutputStream(uri)!!
                outputStream.write(data)
            } catch (e: IOException) {
                errorMessage = e.message
                return@withContext true
            } finally {
                outputStream?.close()
            }
            return@withContext false
        }
        if (stop) {
            updateProgressDialog?.error(getString(R.string.backup_error), errorMessage)
            return@launch
        }

        updateProgressDialog?.updateProgress(100)
    }
}


/**
 * Get restore launcher
 *
 * Used for registerForActivityResult(ActivityResultContracts.StartActivityForResult())
 *
 * @param result ActivityResult
 * @param create_backup If true -> create backup and add undo button to dialog
 *                      If false -> don't create backup and don't show undo button
 */
fun Activity.getRestoreLauncher(result: ActivityResult, create_backup: Boolean = true) {
    if (result.resultCode != Activity.RESULT_OK)
        return

    val updateProgressDialog = UpdateProgressDialog(this)
    updateProgressDialog.finishDialog =
        if (create_backup)
            getRestartDialog(RestartDialogMessage.RESTORE) { YomikataDatabase.restoreLocalBackup(this) }
        else
            getRestartDialog(RestartDialogMessage.RESTORE, null)
    updateProgressDialog.prepare(getString(R.string.restoring_progress), getString(R.string.do_not_close_app))
    updateProgressDialog.show()

    val updateProgressDialogMigrate = UpdateProgressDialog(this)
    updateProgressDialogMigrate.prepare(getString(R.string.migrating), getString(R.string.may_take_a_while))
    updateProgressDialogMigrate.destroyOnFinish = true

    CoroutineScope(Dispatchers.Main).launch {
        val inputStream =
            result.data?.data?.let { uri ->
                val contentResolver = this@getRestoreLauncher.contentResolver
                contentResolver.openInputStream(uri)
            }

        val success = handleRestore(inputStream!!, updateProgressDialog, create_backup)
        if (!success)
            return@launch

        // do migration
        YomikataDatabase.setUpdateProgressDialog(updateProgressDialogMigrate)
        updateProgressDialogMigrate.show()
        val successAndMessage = withContext(Dispatchers.IO) {
            try {
                YomikataDatabase.forceLoadDatabase(this@getRestoreLauncher)
                return@withContext Pair(true, "")
            } catch (e: Exception) {
                return@withContext Pair(false, e.message)
            }
        }
        val migrationSuccess = successAndMessage.first
        try {
            updateProgressDialogMigrate.destroy()
            YomikataDatabase.setUpdateProgressDialog(null)
        } finally {
            if (!migrationSuccess) {
                YomikataDatabase.restoreLocalBackup(this@getRestoreLauncher)
                val errorMessage = successAndMessage.second
                updateProgressDialog.error(getString(R.string.restore_error),
                    getString(R.string.migration_failed) + ":\n" + errorMessage
                )
            } else {
                updateProgressDialog.updateProgress(100)
            }
            PreferenceManager.getDefaultSharedPreferences(this@getRestoreLauncher).edit()
                                        .putBoolean(Prefs.DB_RESTORE_ONGOING.pref, false).apply()
        }
    }
}

/**
 * Handle restore
 *
 * @param inputStream InputStream containing data to restore
 * @param updateProgressDialog Optional dialog to show progress
 * @return True if success, False if failure
 */
private suspend fun Activity.handleRestore(inputStream: InputStream,
           updateProgressDialog: UpdateProgressDialog? = null, create_backup: Boolean = true)
                                                        : Boolean = withContext(Dispatchers.IO) {
    val buffer = ByteArray(4096)
    val outputStream = ByteArrayOutputStream()

    // if flag = true -> original db was overwritten. If error occurs -> recover local backup
    var flag = false

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
            withContext(Dispatchers.Main) {
                updateProgressDialog?.error(getString(R.string.invalid_file), e.message)
            }
            return@withContext false
        } catch (e: SQLiteException) {
            withContext(Dispatchers.Main) {
                updateProgressDialog?.error(getString(R.string.restore_error), e.message + e.cause?.message)
            }
            return@withContext false
        }

        if (type == DatabaseType.OLD_YOMIKATA) {
            importYomikata(this@handleRestore, data, null, create_backup)
        } else {
            YomikataDatabase.overwriteDatabase(this@handleRestore, data, create_backup)
        }
        flag = true

        updateProgressDialog?.updateProgress(75)
    } catch (e: IOException) {
        if (flag) {
            YomikataDatabase.restoreLocalBackup(this@handleRestore)
        }
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            updateProgressDialog?.error(getString(R.string.restore_error), e.message)
        }
        return@withContext false
    } finally {
        inputStream.close()
        outputStream.close()
        outputStreamTemp.close()
        databaseFile.delete()
    }
    return@withContext true
}

enum class RestartDialogMessage {
    RESTORE,
    RESET,
    UNDO
}

fun Context.getRestartDialog(message: RestartDialogMessage, undoCallback: (() -> Unit)?): AlertDialog {
    return alertDialog {
        when(message) {
            RestartDialogMessage.RESTORE -> {
                titleResource = R.string.restore_success_message
                messageResource = R.string.ask_to_restart
            }
            RestartDialogMessage.RESET -> {
                titleResource = R.string.your_data_has_been_reset
                if (undoCallback != null)
                    messageResource = R.string.undo_to_restore
            }
            RestartDialogMessage.UNDO -> {
                titleResource = R.string.changes_undone
                messageResource = R.string.ask_to_restart
            }
        }
        setCancelable(false)
        positiveButton(R.string.alert_restart) {
            triggerRebirth()
        }
        if (undoCallback != null) {
            negativeButton(R.string.undo) {
                undoCallback()
                getRestartDialog(RestartDialogMessage.UNDO, null).show()
            }
        }
    }
}

fun Context.triggerRebirth() {
    val packageManager: PackageManager = this.packageManager
    val intent = packageManager.getLaunchIntentForPackage(this.packageName)
    val componentName = intent!!.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    this.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}
