package com.jehutyno.yomikata.screens.settings

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.screens.prefs.PrefsFragment
import com.jehutyno.yomikata.ui.settings.SettingsScreen
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.contactDiscord
import com.jehutyno.yomikata.util.contactFacebook
import com.jehutyno.yomikata.util.contactPlayStore
import com.jehutyno.yomikata.util.shareApp

class SettingsFragment : Fragment() {

    private var prefsContainerId = View.NO_ID
    private var nightMode by mutableStateOf(
        AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val versionName = resolveVersionName()

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    SettingsScreen(
                        nightMode = nightMode,
                        versionName = versionName,
                        onNightModeToggle = { enabled -> toggleNightMode(enabled) },
                        onDiscord = { contactDiscord(requireContext()) },
                        onFacebook = { contactFacebook(requireContext()) },
                        onPlayStore = { contactPlayStore(requireContext()) },
                        onShare = { shareApp(requireContext()) },
                    )
                }
            }
        }

        val prefsContainer = FrameLayout(requireContext()).also { fl ->
            fl.id = View.generateViewId()
            prefsContainerId = fl.id
        }

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            addView(composeView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(prefsContainer, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            ))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.findFragmentById(prefsContainerId) == null) {
            val prefsFragment = PrefsFragment().also { f ->
                f.onResetTuto = {
                    (activity as? com.jehutyno.yomikata.screens.quizzes.QuizzesActivity)
                        ?.let { qa ->
                            qa.navigateTo(com.jehutyno.yomikata.ui.components.BottomNavDestination.HOME)
                            qa.tutos()
                        }
                }
            }
            childFragmentManager.beginTransaction()
                .add(prefsContainerId, prefsFragment)
                .commit()
        }
    }

    private fun toggleNightMode(enabled: Boolean) {
        nightMode = enabled
        val mode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit().putInt(Prefs.DAY_NIGHT_MODE.pref, mode).apply()
        activity?.recreate()
    }

    private fun resolveVersionName(): String {
        return try {
            val pm = requireContext().packageManager
            val pkg = requireContext().packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName ?: ""
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).versionName ?: ""
            }
        } catch (_: Exception) { "" }
    }
}
