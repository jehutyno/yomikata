package com.jehutyno.yomikata.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jehutyno.yomikata.ui.theme.AccentOnOrange
import com.jehutyno.yomikata.ui.theme.AccentOrange
import com.jehutyno.yomikata.ui.theme.BorderDefault
import com.jehutyno.yomikata.ui.theme.Correct
import com.jehutyno.yomikata.ui.theme.RadiusMd
import com.jehutyno.yomikata.ui.theme.RadiusXl
import com.jehutyno.yomikata.ui.theme.SurfacePrimary
import com.jehutyno.yomikata.ui.theme.TextMuted
import com.jehutyno.yomikata.ui.theme.TextPrimary
import com.jehutyno.yomikata.ui.theme.TextSecondary
import com.jehutyno.yomikata.ui.theme.TypeBody
import com.jehutyno.yomikata.ui.theme.TypeListTitle
import com.jehutyno.yomikata.ui.theme.TypeScreenTitle
import com.jehutyno.yomikata.ui.theme.Wrong
import com.jehutyno.yomikata.ui.theme.YomikataTheme

/**
 * Visual style of a [DialogButton].
 *
 * - [Primary]: action principale — pill orange plein (AccentOrange / AccentOnOrange).
 * - [Muted]: action secondaire / annulation — texte en sourdine (TextMuted).
 * - [Destructive]: action irréversible — contour rouge (Wrong).
 */
enum class DialogButtonStyle { Primary, Muted, Destructive }

/**
 * An icon shown next to the dialog title to reinforce its intent.
 *
 * Mapped to a Material icon + design token tint. Kept as an enum so call sites
 * (including imperative ones) don't need to import Compose icon classes.
 */
enum class DialogIcon(internal val image: ImageVector, internal val tint: Color) {
    Success(Icons.Filled.CheckCircle, Correct),
    Warning(Icons.Filled.Warning, Wrong),
}

/**
 * A button in a [YomikataDialogContent] / [YomikataDialog].
 *
 * @param onClick action to run when tapped. In pure-Compose usage the caller is
 * responsible for dismissing; the imperative bridge dismisses automatically.
 */
data class DialogButton(
    val text: String,
    val style: DialogButtonStyle = DialogButtonStyle.Muted,
    val onClick: () -> Unit,
)

/**
 * Pure-Compose dialog harmonisé au Design System v2.
 *
 * Wraps [YomikataDialogContent] in a Compose [Dialog]. Visibility is hoisted to
 * the caller (show by including this composable, hide by removing it). For imperative
 * (Activity/Fragment) call sites use `Context.yomikataAlert(...)` instead.
 */
@Composable
fun YomikataDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    message: String? = null,
    icon: DialogIcon? = null,
    buttons: List<DialogButton> = emptyList(),
    properties: DialogProperties = DialogProperties(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        YomikataDialogContent(
            title = title,
            message = message,
            icon = icon,
            buttons = buttons,
            content = content,
        )
    }
}

/**
 * The card surface of a Yomikata dialog, without any window/scrim wrapper.
 *
 * Used directly inside the imperative bridge (hosted in a [ComposeView]) and by
 * [YomikataDialog]. Surface bleu nuit, bordure standard, coins radius_xl.
 */
@Composable
fun YomikataDialogContent(
    title: String? = null,
    message: String? = null,
    icon: DialogIcon? = null,
    buttons: List<DialogButton> = emptyList(),
    modifier: Modifier = Modifier,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(RadiusXl),
        color = SurfacePrimary,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = modifier.widthIn(max = 360.dp),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon.image,
                            contentDescription = null,
                            tint = icon.tint,
                            modifier = Modifier.size(20.dp).padding(end = 9.dp),
                        )
                    }
                    Text(text = title, style = TypeScreenTitle, color = TextPrimary)
                }
            }

            if (message != null) {
                Text(
                    text = message,
                    style = TypeBody.copy(fontSize = TypeListTitle.fontSize),
                    color = TextSecondary,
                    modifier = Modifier.padding(top = if (title != null) 8.dp else 0.dp),
                )
            }

            if (content != null) {
                Column(modifier = Modifier.padding(top = if (title != null || message != null) 14.dp else 0.dp)) {
                    content()
                }
            }

            if (buttons.isNotEmpty()) {
                DialogButtonBar(
                    buttons = buttons,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun DialogButtonBar(buttons: List<DialogButton>, modifier: Modifier = Modifier) {
    val primary = buttons.firstOrNull { it.style == DialogButtonStyle.Primary }
    val others = buttons.filter { it !== primary }

    if (buttons.size <= 2) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            others.forEach { DialogButtonView(it) }
            primary?.let { DialogButtonView(it) }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            primary?.let { DialogButtonView(it, Modifier.fillMaxWidth()) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                others.forEach { DialogButtonView(it, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DialogButtonView(button: DialogButton, modifier: Modifier = Modifier) {
    when (button.style) {
        DialogButtonStyle.Primary -> Button(
            onClick = button.onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = AccentOnOrange,
            ),
            shape = RoundedCornerShape(RadiusMd),
            modifier = modifier,
        ) {
            Text(button.text, style = TypeListTitle, color = AccentOnOrange)
        }

        DialogButtonStyle.Destructive -> OutlinedButton(
            onClick = button.onClick,
            border = BorderStroke(1.dp, Wrong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Wrong),
            shape = RoundedCornerShape(RadiusMd),
            modifier = modifier,
        ) {
            Text(button.text, style = TypeListTitle, color = Wrong, textAlign = TextAlign.Center)
        }

        DialogButtonStyle.Muted -> TextButton(
            onClick = button.onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = TextMuted),
            shape = RoundedCornerShape(RadiusMd),
            modifier = modifier,
        ) {
            Text(button.text, style = TypeListTitle, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}

@Preview(name = "Confirmation — 2 boutons", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun YomikataDialogConfirmPreview() {
    YomikataTheme {
        YomikataDialogContent(
            title = "Vous avez vu tous les mots",
            message = "Que voulez-vous faire ?",
            buttons = listOf(
                DialogButton("Quitter", DialogButtonStyle.Muted) {},
                DialogButton("Relancer", DialogButtonStyle.Primary) {},
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Fin de session — 3 boutons", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun YomikataDialogThreePreview() {
    YomikataTheme {
        YomikataDialogContent(
            title = "Session terminée · 6 mots",
            icon = DialogIcon.Success,
            buttons = listOf(
                DialogButton("Continuer", DialogButtonStyle.Primary) {},
                DialogButton("Revoir erreurs", DialogButtonStyle.Muted) {},
                DialogButton("Quitter", DialogButtonStyle.Muted) {},
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Destructif", showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun YomikataDialogDestructivePreview() {
    YomikataTheme {
        YomikataDialogContent(
            title = "Réinitialiser les données ?",
            message = "Cette action est irréversible.",
            icon = DialogIcon.Warning,
            buttons = listOf(
                DialogButton("Annuler", DialogButtonStyle.Muted) {},
                DialogButton("Réinitialiser", DialogButtonStyle.Destructive) {},
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
