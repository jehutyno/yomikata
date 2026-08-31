package com.jehutyno.yomikata.ui.components

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/** Walks the [ContextWrapper] chain to find the hosting [ComponentActivity]. */
private fun Context.findComponentActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Imperative bridge to show a [YomikataDialogContent] from Activity/Fragment code.
 *
 * Hosts the Compose card in a [ComposeView] inside a transparent [Dialog] (the dialog
 * provides the dimmed scrim; the rounded surface comes from the composable). This is the
 * "Niveau 2" replacement for the Splitties `alertDialog { }` DSL when the pill/destructive
 * button styling of the Design System v2 is wanted.
 *
 * Each [DialogButton] dismisses the dialog before running its `onClick`, mirroring the
 * default AppCompat behaviour. Call [Dialog.show] on the returned dialog.
 *
 * @param onCancel invoked when the dialog is cancelled (tap outside / back) if [cancelable].
 * @param onBackKey if non-null, intercepts the BACK key while the dialog is shown.
 */
fun Context.yomikataAlert(
    title: String? = null,
    message: String? = null,
    icon: DialogIcon? = null,
    cancelable: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onBackKey: (() -> Unit)? = null,
    buttons: List<DialogButton> = emptyList(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
): Dialog {
    val activity = findComponentActivity()
        ?: error("yomikataAlert requires a ComponentActivity context")

    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.setCancelable(cancelable)
    if (onCancel != null) dialog.setOnCancelListener { onCancel() }
    if (onBackKey != null) {
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onBackKey()
                true
            } else false
        }
    }

    // Wrap each button so tapping dismisses first, then runs the action.
    val wrappedButtons = buttons.map { btn ->
        btn.copy(onClick = {
            dialog.dismiss()
            btn.onClick()
        })
    }

    val composeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(activity)
        setViewTreeViewModelStoreOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
        // DisposeOnDetachedFromWindow (et NON DisposeOnViewTreeLifecycleDestroyed) : la vue
        // vit dans un Dialog dont la durée de vie est bornée par dismiss() / destruction de
        // la fenêtre — le détachement couvre les deux cas, sans risque de fuite.
        // La stratégie lifecycle plantait (IllegalStateException « ... is already destroyed »
        // dans installForLifecycle) quand un dialog était demandé depuis un callback
        // asynchrone arrivé après onDestroy de l'activité hôte : le ComposeView s'attachait
        // alors à un LifecycleOwner déjà DESTROYED. Crashlytics, FATAL, v2.1.0.
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            YomikataTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    YomikataDialogContent(
                        title = title,
                        message = message,
                        icon = icon,
                        buttons = wrappedButtons,
                        modifier = Modifier.fillMaxWidth(),
                        content = content,
                    )
                }
            }
        }
    }

    dialog.setContentView(composeView)
    dialog.window?.setLayout(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )
    return dialog
}
