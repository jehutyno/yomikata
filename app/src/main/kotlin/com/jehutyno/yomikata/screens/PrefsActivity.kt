package com.jehutyno.yomikata.screens

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.filechooser.FileChooserDialog
import com.jehutyno.yomikata.repository.local.WordSource
import com.jehutyno.yomikata.repository.migration.DatabaseHelper
import com.jehutyno.yomikata.repository.migration.MigrationSource
import com.jehutyno.yomikata.repository.migration.MigrationTable
import com.jehutyno.yomikata.repository.migration.MigrationTables
import com.jehutyno.yomikata.util.*
import com.jehutyno.yomikata.util.Extras.PERMISSIONS_STORAGE
import com.jehutyno.yomikata.util.Extras.REQUEST_EXTERNAL_STORAGE_BACKPUP
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
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, PrefsFragment()).commit()
    }

    class PrefsFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            findPreference("backup").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE_BACKPUP
                    )
                } else {
                    (activity as PrefsActivity).backupProgress()
                }
                true
            }

            findPreference("restore").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE_RESTORE
                    )
                } else {
                    (activity as PrefsActivity).showChooser()
                }

                true
            }

            findPreference("reset").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                alert {
                    message = getString(R.string.prefs_reinit_sure)
                    okButton {
                        CopyUtils.reinitDataBase(activity)
                        toast(getString(R.string.prefs_reinit_done))
                        activity.finish()
                    }
                    cancelButton { }
                }.show()
                true
            }

            findPreference("delete_voices").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                alert {
                    message = getString(R.string.prefs_delete_voices_sure)
                    okButton {
                        FileUtils.deleteFolder(activity, "Voices")
                        activity.defaultSharedPreferences.edit().putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(0)}_0", false).apply()
                        activity.defaultSharedPreferences.edit().putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(1)}_1", false).apply()
                        activity.defaultSharedPreferences.edit().putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(2)}_2", false).apply()
                        activity.defaultSharedPreferences.edit().putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(3)}_3", false).apply()
                        activity.defaultSharedPreferences.edit().putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(4)}_4", false).apply()
                        activity.defaultSharedPreferences.edit().putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(5)}_5", false).apply()
                        activity.defaultSharedPreferences.edit().putBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(6)}_6", false).apply()
                        toast(getString(R.string.voices_reinit_done))
                        activity.finish()
                    }
                    cancelButton { }
                }.show()
                true
            }

            findPreference("reset_tuto").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                PreferencesManager(activity).resetAll()
                activity.setResult(Activity.RESULT_OK)
                activity.finish()
                true
            }

            findPreference("privacy").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cdn.rawgit.com/jehutyno/privacy-policies/56b6fcf3/PrivacyPolicyYomikataZ.html"))
                startActivity(browserIntent)
                true
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
                REQUEST_EXTERNAL_STORAGE_BACKPUP -> backupProgress()
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