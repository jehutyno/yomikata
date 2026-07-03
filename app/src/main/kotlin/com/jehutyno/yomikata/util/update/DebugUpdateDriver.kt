package com.jehutyno.yomikata.util.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager

/**
 * Harness de **test sur émulateur** pour la mise à jour in-app (parcours flexible).
 *
 * Sur un émulateur sans Google Play, le vrai `AppUpdateManager` renvoie toujours
 * `UPDATE_NOT_AVAILABLE` → rien à voir. Ce driver fournit un [FakeAppUpdateManager]
 * pré-configuré (MAJ disponible + ancienneté), qu'on injecte dans [InAppUpdateManager],
 * puis simule automatiquement le parcours utilisateur pour rendre la Snackbar de
 * redémarrage visible.
 *
 * **DEBUG UNIQUEMENT** : n'est appelé que sous `if (BuildConfig.DEBUG)` → R8 élimine
 * cet objet et toute référence à `FakeAppUpdateManager` de l'APK release.
 *
 * Pour tester les différents cas, modifier [SIMULATED_STALENESS_DAYS] :
 *  - `>= InAppUpdateManager.STALENESS_DAYS` (3) → l'invite s'affiche.
 *  - `<  InAppUpdateManager.STALENESS_DAYS`      → aucune invite (gate d'ancienneté).
 */
object DebugUpdateDriver {

    private const val TAG = "InAppUpdate"

    /** Ancienneté simulée de la MAJ (jours). Comparée à [InAppUpdateManager.STALENESS_DAYS]. */
    private const val SIMULATED_STALENESS_DAYS = 3

    /** versionCode fictif de la MAJ « disponible » (doit être > versionCode courant). */
    private const val AVAILABLE_VERSION_CODE = 999_999

    private const val FAKE_TOTAL_BYTES = 15_000_000L

    /** Crée un [FakeAppUpdateManager] configuré comme si une MAJ flexible était disponible. */
    fun createFake(context: Context): FakeAppUpdateManager =
        FakeAppUpdateManager(context.applicationContext).apply {
            setUpdateAvailable(AVAILABLE_VERSION_CODE)
            setClientVersionStalenessDays(SIMULATED_STALENESS_DAYS)
            setTotalBytesToDownload(FAKE_TOTAL_BYTES)
        }

    /**
     * Simule le parcours flexible complet une fois [InAppUpdateManager.checkForUpdate] lancé :
     * acceptation utilisateur → téléchargement → `DOWNLOADED` (déclenche la Snackbar de
     * redémarrage de [InAppUpdateManager]). No-op si le flux n'a pas démarré (gate d'ancienneté
     * non franchi), ce qui permet aussi de vérifier le cas « aucune invite ».
     */
    fun autoDrive(fake: FakeAppUpdateManager) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (fake.isConfirmationDialogVisible) {
                Log.d(TAG, "FAKE: user accepts update")
                fake.userAcceptsUpdate()
                fake.downloadStarts()
            } else {
                Log.d(TAG, "FAKE: no flow started (staleness gate not met) → no prompt")
            }
        }, 2_000)
        handler.postDelayed({
            if (fake.isConfirmationDialogVisible || fake.typeForUpdateInProgress != null) {
                Log.d(TAG, "FAKE: download completes → restart Snackbar should appear")
                fake.setBytesDownloaded(FAKE_TOTAL_BYTES)
                fake.downloadCompletes()
            }
        }, 5_000)
    }
}
