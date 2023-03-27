package com.jehutyno.yomikata.screens

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceFragmentCompat
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.filechooser.FileChooserDialog
import com.jehutyno.yomikata.repository.local.WordSource
import com.jehutyno.yomikata.repository.migration.DatabaseHelper
import com.jehutyno.yomikata.repository.migration.MigrationSource
import com.jehutyno.yomikata.repository.migration.MigrationTable
import com.jehutyno.yomikata.repository.migration.MigrationTables
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.Extras.PERMISSIONS_STORAGE
import com.jehutyno.yomikata.util.Extras.REQUEST_EXTERNAL_STORAGE_BACKUP
import com.jehutyno.yomikata.util.Extras.REQUEST_EXTERNAL_STORAGE_RESTORE
import com.wooplr.spotlight.prefs.PreferencesManager
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.MainScope
import mu.KLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import splitties.alertdialog.appcompat.*
import java.io.File


/**
 * Created by valentin on 30/11/2016.
 */
class PrefsActivity : AppCompatActivity(), FileChooserDialog.ChooserListener {

    companion object : KLogging()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PrefsFragment()).commit()
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
        FileChooserDialog.Builder(FileChooserDialog.ChooserType.FILE_CHOOSER, this)
            .setTitle(getString(R.string.choose_file))
            .build().show(supportFragmentManager, null)
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
        alertDialog {
            titleResource = R.string.backup
            messageResource = R.string.backup_sure
            okButton {
                CopyUtils.copyEncryptedBddToSd(this@PrefsActivity)
            }
            cancelButton()
        }.show()
    }

    override fun onSelect(path: String?) {
        if (path?.endsWith(".yomikata")!!) {
            importYomikata(path)
        } else if (path.endsWith(".yomikataz")) {
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
            val migrationSource = MigrationSource(this, DatabaseHelper.getInstance(this, toName, toPath))
            val wordTables = MigrationTable.allTables(MigrationTables.values())

            val progressDialog: ProgressDialog = ProgressDialog(this)
            progressDialog.max = wordTables.count()
            progressDialog.setTitle(getString(R.string.progress_import_title))
            progressDialog.setMessage(getString(R.string.progress_import_message_y))
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setCancelable(false)
            progressDialog.show()

            MainScope().async {
                wordTables.forEach {
                    val wordtable = migrationSource.getWordTable(it)
                    progressDialog.incrementProgressBy(1)
                    wordtable.forEach {word ->
                        val source = WordSource(this@PrefsActivity)
                        if (word.counterTry > 0 || word.priority > 0)
                            source.restoreWord(word.word, word.prononciation, word)
                    }
                }
                File(toPath + toName).delete()

                withContext(Main) {
                    progressDialog.dismiss()
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

    private fun importYomikataZ(path: String?) {
        updateBDD(null, path!!, -1)
        finish()
    }


}