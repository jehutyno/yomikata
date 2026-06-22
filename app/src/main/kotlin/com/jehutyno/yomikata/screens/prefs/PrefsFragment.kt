package com.jehutyno.yomikata.screens.prefs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.updatePadding
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import com.jehutyno.yomikata.util.language.AppLanguage
import com.jehutyno.yomikata.util.FileUtils

import com.jehutyno.yomikata.util.language.LanguageManager
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.analytics.Analytics
import com.jehutyno.yomikata.util.backup.RestartDialogMessage
import com.jehutyno.yomikata.util.backup.backupProgress
import com.jehutyno.yomikata.util.backup.getBackupLauncher
import com.jehutyno.yomikata.util.quiz.getLevelDownloadVersion
import com.jehutyno.yomikata.util.backup.getRestartDialog
import com.jehutyno.yomikata.util.backup.getRestoreLauncher
import com.jehutyno.yomikata.util.backup.restoreProgress
import com.jehutyno.yomikata.util.TutoId
import com.jehutyno.yomikata.util.resetAllTutos
import com.jehutyno.yomikata.ui.components.DialogButton
import com.jehutyno.yomikata.ui.components.DialogButtonStyle
import com.jehutyno.yomikata.ui.components.DialogIcon
import com.jehutyno.yomikata.ui.components.yomikataAlert

class PrefsFragment : PreferenceFragmentCompat() {

    private lateinit var backupLauncher: ActivityResultLauncher<Intent>
    private lateinit var restoreLauncher: ActivityResultLauncher<Intent>

    // Called after reset_tuto so the host can navigate to HOME and re-show tutorials.
    var onResetTuto: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result -> (requireActivity() as ComponentActivity).getBackupLauncher(result) }
        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit().putBoolean(Prefs.DB_RESTORE_ONGOING.pref, true).apply()
                (requireActivity() as ComponentActivity).getRestoreLauncher(result)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Dégagement pour la barre de navigation flottante : la liste défile sous la pill.
        val bottomPad = (com.jehutyno.yomikata.ui.components.FloatingNavBarHeight.value
                * resources.displayMetrics.density).toInt()
        listView?.apply {
            clipToPadding = false
            updatePadding(bottom = bottomPad)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>(Prefs.APP_LANGUAGE.pref)?.apply {
            value = LanguageManager.current.isoCode
            setOnPreferenceChangeListener { _, newValue ->
                val lang = AppLanguage.fromIsoCode(newValue as String)
                LanguageManager.current = lang
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(lang.isoCode)
                )
                true
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        findPreference<CheckBoxPreference>(Prefs.ANALYTICS_CONSENT.pref)?.apply {
            isChecked = Analytics.hasConsent(prefs)
            setOnPreferenceChangeListener { _, newValue ->
                // Route via Analytics pour appliquer les drapeaux Firebase + marquer "asked"
                Analytics.setConsent(prefs, newValue as Boolean)
                true
            }
        }
    }

    private fun getResetAlert(): Dialog {
        return requireContext().yomikataAlert(
            message = getString(R.string.prefs_reinit_sure),
            icon = DialogIcon.Warning,
            buttons = listOf(
                DialogButton(getString(android.R.string.cancel), DialogButtonStyle.Muted) {},
                DialogButton(getString(android.R.string.ok), DialogButtonStyle.Destructive) {
                    YomikataDatabase.resetDatabase(requireContext())
                    YomikataDatabase.forceLoadDatabase(requireContext())
                    requireActivity().getRestartDialog(RestartDialogMessage.RESET) {
                        YomikataDatabase.restoreLocalBackup(requireContext())
                    }.show()
                },
            ),
        )
    }

    private fun getDeleteVoicesAlert(): Dialog {
        return requireContext().yomikataAlert(
            message = getString(R.string.prefs_delete_voices_sure),
            icon = DialogIcon.Warning,
            buttons = listOf(
                DialogButton(getString(android.R.string.cancel), DialogButtonStyle.Muted) {},
                DialogButton(getString(android.R.string.ok), DialogButtonStyle.Destructive) {
                    FileUtils.deleteFolder(activity, "Voices")
                    val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    for (i in 0 until 7) {
                        pref.edit().putBoolean(
                            "${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(i)}_$i", false
                        ).apply()
                    }
                    Toast.makeText(requireContext(), R.string.voices_reinit_done, Toast.LENGTH_LONG).show()
                },
            ),
        )
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "font_size", "input_change", "speed", "length",
            "tap_to_reveal", "play_start", "play_end", "analytics_consent" -> return true

            "reset_tuto" -> {
                resetAllTutos(PreferenceManager.getDefaultSharedPreferences(requireActivity()))
                onResetTuto?.invoke()
                return true
            }

            "backup" -> {
                backupProgress(backupLauncher)
                return true
            }
            "restore" -> {
                requireContext().restoreProgress(restoreLauncher)
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

            Prefs.APP_LANGUAGE.pref -> return true

            "privacy" -> {
                val url = getString(R.string.url_privacy_policy)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return true
            }

            else -> throw Error("unknown preference key: ${preference.key}")
        }
    }
}
