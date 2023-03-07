package com.jehutyno.yomikata.screens

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
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
import mu.KLogging
import org.jetbrains.anko.*
import java.io.File


/**
 * Created by valentin on 30/11/2016.
 */
class PrefsActivity : AppCompatActivity(), FileChooserDialog.ChooserListener {

    companion object : KLogging()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(defaultSharedPreferences.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
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

        private fun getResetAlert() : AlertDialog.Builder {
            val builder = AlertDialog.Builder(context)

            builder.setMessage(R.string.prefs_reinit_sure)
            builder.setPositiveButton(R.string.ok) { _, _ ->
                CopyUtils.reinitDataBase(activity)
                requireActivity().toast(R.string.prefs_reinit_done)
                requireActivity().finish()
            }
            builder.setNegativeButton(R.string.cancel_caps) { _, _ -> }

            return builder
        }

        private fun getDeleteVoicesAlert() : AlertDialog.Builder {
            val builder = AlertDialog.Builder(context)

            builder.setMessage(R.string.prefs_delete_voices_sure)
            builder.setPositiveButton(R.string.ok) { _, _ ->
                FileUtils.deleteFolder(activity, "Voices")
                for (i in 0 until 7) {
                    requireActivity().defaultSharedPreferences.edit()
                            .putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(i)}_$i", false).apply()
                }
                requireActivity().toast(getString(R.string.voices_reinit_done))
                requireActivity().finish()
            }
            builder.setNegativeButton(R.string.cancel_caps) { _, _ -> }

            return builder
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
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
                    return super.onPreferenceTreeClick(preference)    // to allow fragment creation
                }
                "delete_voices" -> {
                    getDeleteVoicesAlert().show()
                    return super.onPreferenceTreeClick(preference)
                }
                "reset_tuto" -> {
                    PreferencesManager(activity).resetAll()
                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().finish()
                    return true
                }
                "privacy" -> {
                    val privacyString = "https://cdn.rawgit.com/jehutyno/privacy-policies/56b6fcf3/PrivacyPolicyYomikataZ.html"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyString))
                    startActivity(browserIntent)
                    return true
                }
                else -> {
                    return true
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
        alert {
            title = getString(R.string.backup)
            message = getString(R.string.backup_sure)
            okButton { CopyUtils.copyEncryptedBddToSd(this@PrefsActivity) }
            cancelButton { }
        }.show()
    }

    override fun onSelect(path: String?) {
        if (path?.endsWith(".yomikata")!!) {
            importYomikata(path)
        } else if (path.endsWith(".yomikataz")) {
            importYomikataZ(path)
        } else {
            alert {
                title = getString(R.string.use_yomikata_file)
                okButton { }
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

            doAsync {
                wordTables.forEach {
                    val wordtable = migrationSource.getWordTable(it)
                    progressDialog.incrementProgressBy(1)
                    wordtable.forEach {
                        val source = WordSource(this@PrefsActivity)
                        if (it.counterTry > 0 || it.priority > 0)
                            source.restoreWord(it.word, it.prononciation, it)
                    }
                }
                File(toPath + toName).delete()

                uiThread {
                    progressDialog.dismiss()
                    alert {
                        title = getString(R.string.restore_success)
                        okButton { }
                        message = getString(R.string.restore_success_message)
                    }.show()
                }

            }
        } catch (exception: Exception) {
            alert {
                title = getString(R.string.restore_error)
                message = getString(R.string.restore_error_message)
                okButton { }
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