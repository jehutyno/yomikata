package com.jehutyno.yomikata.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
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
import com.jehutyno.yomikata.filechooser.FileChooserDialog
import com.jehutyno.yomikata.repository.local.WordSource
import com.jehutyno.yomikata.repository.local.YomikataDataBase
import com.jehutyno.yomikata.repository.migration.*
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.Extras.PERMISSIONS_STORAGE
import com.jehutyno.yomikata.util.Extras.REQUEST_EXTERNAL_STORAGE_BACKUP
import com.jehutyno.yomikata.util.Extras.REQUEST_EXTERNAL_STORAGE_RESTORE
import com.wooplr.spotlight.prefs.PreferencesManager
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import mu.KLogging
import splitties.alertdialog.appcompat.*
import java.io.*


/**
 * Created by valentin on 30/11/2016.
 */
class PrefsActivity : AppCompatActivity(), FileChooserDialog.ChooserListener {

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

            val inputFile = YomikataDataBase.getDatabaseFile(this)
            val data = ByteArray(inputFile.length().toInt()) // create byte array with size of input file
            var inputStream: FileInputStream? = null

            try {
                inputStream = FileInputStream(inputFile)
                inputStream.read(data)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
            }

            result.data?.data?.also { uri ->
                val outputStream: OutputStream
                try {
                    outputStream = contentResolver.openOutputStream(uri)!!
                    outputStream.write(data)
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK)
                return@registerForActivityResult

            result.data?.data?.also { uri ->

                val contentResolver = this.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val buffer = ByteArray(4096)
                val outputStream = ByteArrayOutputStream()

                try {
                    var len: Int
                    while (inputStream?.read(buffer).also { len = it ?: -1 } != -1) {
                        outputStream.write(buffer, 0, len)
                    }
                    val data = outputStream.toByteArray()
                    YomikataDataBase.overwriteDatabase(this, data)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    inputStream?.close()
                    outputStream.close()
                }

            }

        }

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
        private fun checkAndRequestPermission(): Boolean {
            val permission = ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        requireActivity(),
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE_BACKUP
                )
            }
            return permission == PackageManager.PERMISSION_GRANTED
        }

        private fun getResetAlert() : AlertDialog {
            return requireContext().alertDialog {
                messageResource = R.string.prefs_reinit_sure
                okButton {
                    CopyUtils.reinitDataBase(activity)
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
                    if (checkAndRequestPermission()) {
                        (activity as PrefsActivity).backupProgress()
                    }
                    return true
                }
                "restore" -> {
                    if (checkAndRequestPermission()) {
                        (activity as PrefsActivity).showChooser()
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

    fun showChooser() {
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

    override fun onSaveInstanceState(outState: Bundle) {
        //No call for super(). Bug on API Level > 11.
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQUEST_EXTERNAL_STORAGE_RESTORE -> showChooser()
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

    override fun onSelect(path: String?) {
        if (path?.endsWith(".yomikata")!!) {
            importYomikata(path)
        } else if (path.endsWith(".yomikataz") || path.endsWith(".db")) {
            importYomikataZ(path)
        } else {
            alertDialog {
                titleResource = R.string.use_yomikata_file
                okButton()
            }.show()
        }
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


            val progressBar = ProgressBar(this, null, android.R.style.Widget_ProgressBar_Horizontal)
            progressBar.setPadding(40, progressBar.paddingTop, 40, progressBar.paddingBottom)
            progressBar.max = wordTables.count()

            val progressAlertDialog = alertDialog {
                titleResource = R.string.progress_import_title
                messageResource = R.string.progress_import_message_y
                setCancelable(false)
                setView(progressBar)
            }
            progressAlertDialog.show()

            MainScope().async {
                wordTables.forEach {
                    val wordTable = migrationSource.getWordTable(it)
                    progressBar.incrementProgressBy(1)
                    wordTable.forEach { word ->
                        val wordDao = YomikataDataBase.getDatabase(this@PrefsActivity).wordDao()
                        val source = WordSource(wordDao)
                        if (word.counterTry > 0 || word.priority > 0)
                            source.restoreWord(word.word, word.pronunciation, word)
                    }
                }
                File(toPath + toName).delete()

                withContext(Main) {
                    progressAlertDialog.dismiss()
                    alertDialog {
                        titleResource = R.string.restore_success
                        messageResource = R.string.restore_success_message
                        okButton()
                    }.show()
                }

            }
        } catch (exception: Exception) {
            alertDialog {
                titleResource = R.string.restore_error
                messageResource = R.string.restore_error_message
                okButton()
            }.show()
        }
    }

//    private fun importYomikataZ(path: String?) {
//        val file = File(path)
//        val toPath = getString(R.string.db_path)
//        val toName = SQLiteHelper.DATABASE_NAME
//        try {
//            CopyUtils.restoreEncryptedBdd(file, toPath + toName)
//            alert {
//                title(R.string.restore_success)
//                message(R.string.restore_success_message)
//                okButton {  }
//            }.show()
//        } catch(e: Exception) {
//            e.printStackTrace()
//            alert {
//                title(R.string.backup_error)
//                message(R.string.backup_error_message)
//                okButton {  }
//            }.show()
//        }
//
//    }

    private fun importYomikataZ(path: String) {
        YomikataDataBase.overwriteDatabase(this, path)
        finish()
    }

}
