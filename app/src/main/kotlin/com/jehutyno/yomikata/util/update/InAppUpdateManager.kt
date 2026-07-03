package com.jehutyno.yomikata.util.update

import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.jehutyno.yomikata.R

/**
 * Mise à jour in-app via Google Play — parcours **strictement flexible**, jamais bloquant.
 *
 * Comportement (cf. critères d'acceptation) :
 *  - Au démarrage ([checkForUpdate]) : si une mise à jour est disponible **depuis au moins**
 *    [STALENESS_DAYS] jours, propose son téléchargement en arrière-plan. L'app reste
 *    pleinement utilisable ; l'utilisateur peut refuser ou reporter.
 *  - Comportement uniforme pour toutes les mises à jour (aucun niveau de priorité).
 *  - Aucune mise à jour disponible → aucun écran affiché.
 *  - Une fois la mise à jour téléchargée (en arrière-plan ou pendant une mise en veille de
 *    l'app), invite l'utilisateur à redémarrer via une Snackbar → [AppUpdateManager.completeUpdate]
 *    réalise l'installation.
 *  - L'invite de lancement n'est proposée **qu'une seule fois par session** (flag mémoire,
 *    remis à zéro à la mort du process).
 *
 * S'auto-enregistre comme observateur du cycle de vie de l'activité (désabonnement du
 * listener d'installation à `onDestroy`, re-vérification de l'état téléchargé à `onResume`).
 *
 * @param rootView   vue racine servant de parent à la Snackbar.
 * @param anchorView vue au-dessus de laquelle ancrer la Snackbar (ex. barre de nav flottante),
 *                   ou `null` pour laisser la Snackbar en bas d'écran.
 */
class InAppUpdateManager(
    activity: AppCompatActivity,
    private val rootView: View,
    private val anchorView: View? = null,
    // Injectable pour permettre le test sur émulateur via FakeAppUpdateManager (cf.
    // DebugUpdateDriver). En prod, défaut = vrai gestionnaire Play.
    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(activity.applicationContext),
) : DefaultLifecycleObserver {

    // Doit être enregistré avant que l'activité passe à STARTED : la construction depuis
    // onCreate garantit ce timing. Le résultat est volontairement ignoré — un refus ou un
    // report ne fait qu'annuler le flux, sans jamais bloquer l'usage de l'app.
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { /* no-op */ }

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            promptRestart()
        }
    }

    private var restartSnackbar: Snackbar? = null

    init {
        activity.lifecycle.addObserver(this)
        appUpdateManager.registerListener(installListener)
    }

    /**
     * À appeler au démarrage. Ne déclenche l'invite de téléchargement qu'une fois par session
     * et seulement si une MAJ flexible est disponible depuis ≥ [STALENESS_DAYS] jours.
     */
    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                val staleEnough = (info.clientVersionStalenessDays() ?: 0) >= STALENESS_DAYS
                if (available && flexibleAllowed && staleEnough && !promptedThisSession) {
                    promptedThisSession = true
                    startFlexibleUpdate(info)
                }
            }
            .addOnFailureListener { Log.w(TAG, "appUpdateInfo failed", it) }
    }

    private fun startFlexibleUpdate(info: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "startUpdateFlowForResult failed", e)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        // La MAJ peut avoir fini de se télécharger pendant que l'app était en arrière-plan
        // (le listener ne se déclenche alors pas) → on revérifie l'état à chaque retour.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                promptRestart()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        appUpdateManager.unregisterListener(installListener)
    }

    private fun promptRestart() {
        if (restartSnackbar?.isShown == true) return
        restartSnackbar = Snackbar.make(
            rootView,
            R.string.update_ready_message,
            Snackbar.LENGTH_INDEFINITE,
        ).apply {
            setAction(R.string.update_restart_action) { appUpdateManager.completeUpdate() }
            anchorView?.let { setAnchorView(it) }
            show()
        }
    }

    companion object {
        /** Jours de disponibilité d'une MAJ avant de commencer à proposer l'invite. */
        const val STALENESS_DAYS = 3

        private const val TAG = "InAppUpdate"

        /** N'affiche l'invite de lancement qu'une fois par session (durée de vie du process). */
        @Volatile private var promptedThisSession = false
    }
}
