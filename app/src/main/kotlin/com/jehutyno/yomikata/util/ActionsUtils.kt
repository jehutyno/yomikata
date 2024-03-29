@file:Suppress("UNNECESSARY_NOT_NULL_ASSERTION")

package com.jehutyno.yomikata.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.firebase.storage.FirebaseStorage
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.YomikataZKApplication
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.wooplr.spotlight.SpotlightConfig
import com.wooplr.spotlight.SpotlightView
import com.wooplr.spotlight.prefs.PreferencesManager
import com.wooplr.spotlight.utils.SpotlightListener
import splitties.alertdialog.appcompat.*
import java.io.File
import java.util.*


/**
 * Created by valentin on 24/10/2016.
 */

fun reportError(context: Activity, word: Word, sentence: Sentence) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "message/rfc822"
    i.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>(context.getString(R.string.email_contact)))
    i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.error_mail_subject))
    val sb = StringBuilder()
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
    }
    sb.append(context.getString(R.string.error_mail_body_1))
            .append("App version: V").append(packageInfo.versionName).append("\n")
            .append("Word Id: ").append(word.id).append(" | ").append("Quiz Id: ").append(word.baseCategory)
            .append(" | ").append(word.japanese).append(" | ").append(word.reading).append("\n")
            .append("Sentence Id: ").append(sentence.id).append("\n")
            .append("JP: ").append(sentence.jap).append("\n")
            .append("EN: ").append(sentence.en).append("\n")
            .append("FR: ").append(sentence.fr)
            .append(context.getString(R.string.error_mail_comments))
    i.putExtra(Intent.EXTRA_TEXT, sb.toString())
    try {
        context.startActivity(Intent.createChooser(i, context.getString(R.string.error_mail_chooser)))
    } catch (ex: android.content.ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.mail_error), Toast.LENGTH_SHORT).show()
    }
    // TODO report error to be fixed
}

fun shareApp(context: Context) {
    try {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
        i.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_message))
        context.startActivity(Intent.createChooser(i, context.getString(R.string.share_choose)))
    } catch (e: Exception) {
        //e.toString();
    }

}

fun contactEmail(context: Context) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "message/rfc822"
    i.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>(context.getString(R.string.email_contact)))
    i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject))
    try {
        context.startActivity(Intent.createChooser(i, context.getString(R.string.mail_chooser)))
    } catch (ex: android.content.ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.mail_error), Toast.LENGTH_SHORT).show()
    }
}

fun contactFacebook(context: Context?) {
    try {
        val packageName = "com.facebook.katana"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context?.packageManager?.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") context?.packageManager?.getPackageInfo(packageName, 0)
        }
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/412201938791197")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/YomikataAndroid")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

fun contactPlayStore(context: Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + YomikataZKApplication.APP_PNAME)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun contactDiscord(context: Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.discord_link))).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun onTTSinit(context: Context?, status: Int, tts: TextToSpeech?): Int {
    var supported: Int = TextToSpeech.LANG_NOT_SUPPORTED
    try {
        // Initialize the TTS in Japanese if available
        if (status == TextToSpeech.SUCCESS) {
            supported = tts!!.isLanguageAvailable(Locale.JAPANESE)
            if (supported == TextToSpeech.LANG_MISSING_DATA || supported == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                tts.language = Locale.JAPANESE
            }
        } else {
            Toast.makeText(context, context?.getString(R.string.tts_init_failed), Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        supported = TextToSpeech.LANG_NOT_SUPPORTED
    }

    return supported
}

fun checkSpeechAvailability(context: Context, ttsSupported: Int, level: Int): SpeechAvailability {
    if (anyVoicesDownloaded(context, level))
        return SpeechAvailability.VOICES_AVAILABLE
    else if (ttsSupported < TextToSpeech.LANG_AVAILABLE)
        return SpeechAvailability.NOT_AVAILABLE
    else
        return SpeechAvailability.TTS_AVAILABLE
}


fun anyVoicesDownloaded(context: Context, level: Int): Boolean {
    return (0..getLevelDownloadVersion(level)).any {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        pref.getBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${it}_$level", false)
    }
}

fun sentenceNoFuri(sentence: Sentence): String {
    var sentenceNoFuri = sentence.jap
    try {
        while (sentenceNoFuri.indexOf("{") != -1) {
            sentenceNoFuri = sentenceNoFuri.replaceFirst("""\{.+?\}""".toRegex(), sentenceNoFuri.substring(sentenceNoFuri.indexOf("{") + 1, sentenceNoFuri.indexOf(";")))
        }
    } catch (e: StringIndexOutOfBoundsException) {
        Log.e("StringIssue", "NoFuriError in ${sentence.id}:${sentence.jap}")
    }
    return sentenceNoFuri
}

fun sentenceNoAnswerFuri(sentence: Sentence, word: Word): String {
    var sentenceNoFuri = sentence.jap
    sentenceNoFuri = sentenceNoFuri.replace("""\{${word.japanese};${word.reading}\}""".toRegex(), word.japanese)
    return sentenceNoFuri
}

fun sentenceFuri(sentence: Sentence): String {
    var sentenceFuri = sentence.jap
    try {
        while (sentenceFuri.indexOf("{") != -1) {
            sentenceFuri = sentenceFuri.replaceFirst(
                    """\{.+?\}""".toRegex(),
                    sentenceFuri.substring(sentenceFuri.indexOf(";") + 1, sentenceFuri.indexOf("}")))
        }
    } catch (e: StringIndexOutOfBoundsException) {
        Log.e("StringIssue","FuriError in ${sentence.id}:${sentence.jap}")
    }

    return sentenceFuri
}

fun getWordPositionInFuriSentence(sentenceJap: String, word: Word): Int {
    val wordWgrongPosition = sentenceJap.indexOf("{${word.japanese};${word.reading}}")
    if (wordWgrongPosition > 0 && wordWgrongPosition < sentenceJap.length) {
        val subSentence = sentenceJap.subSequence(0, wordWgrongPosition)
        var overdub = 0
        """\{""".toRegex().findAll(subSentence).forEach { overdub++ }
        """;.+?\}""".toRegex().findAll(subSentence).forEach { overdub += it.value.length }

        return wordWgrongPosition - overdub
    } else
        return 0
}

private fun createDlButton(activity: Activity, level: Int, finishedListener: () -> Unit) : Button {
    val dlButton = Button(activity)
    dlButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(activity, R.drawable.ic_download), null, null, null)
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless /* index 0 */)
            val ta = activity.obtainStyledAttributes(attrs)
            val drawableFromTheme = ta.getDrawable(0 /* index */)
            ta.recycle()
            dlButton.background = drawableFromTheme
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    dlButton.text = activity.getString(R.string.download_voices_action, getLevelDownloadSize(level))
    dlButton.compoundDrawablePadding = 20

    dlButton.setOnClickListener {
        val formattedMessage = activity.getString(R.string.download_voices_alert_message, getLevelDownloadSize(level))

        activity.alertDialog {
            titleResource = R.string.download_voices_alert
            message = formattedMessage
            okButton { inter ->
                inter.dismiss()
                launchVoicesDownload(activity, level) { finishedListener() }
            }
            cancelButton()
        }.show()
    }

    return dlButton
}

private fun createGpButton(activity: Activity) : Button {
    val gpButton = Button(activity)
    gpButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(activity, R.drawable.ic_google_play), null, null, null)
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless /* index 0 */)
            val ta = activity.obtainStyledAttributes(attrs)
            val drawableFromTheme = ta.getDrawable(0 /* index */)
            ta.recycle()
            gpButton.background = drawableFromTheme
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    gpButton.text = activity.getString(R.string.get_google_tts_action)
    gpButton.compoundDrawablePadding = 20

    gpButton.setOnClickListener {
        val manager = activity.packageManager
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.google_tts_uri)))
        val query = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") manager.queryIntentActivities(intent, 0)
        }
        if (query.size == 0) {
            val toast = Toast.makeText(activity,R.string.action_not_possible, Toast.LENGTH_LONG)
            toast.show()
        }
        else {
            activity.startActivity(intent)
        }
    }

    return gpButton
}

fun speechNotSupportedAlert(activity: Activity, level: Int, finishedListener: () -> Unit) {
    val pref = PreferenceManager.getDefaultSharedPreferences(activity)
    if (!pref.getBoolean(Prefs.DONT_SHOW_VOICES_POPUP.pref, false)) {
        val dlButton = createDlButton(activity, level, finishedListener)
        val gpButton = createGpButton(activity)

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        dlButton.layoutParams = params
        gpButton.layoutParams = params

        val container = LinearLayout(activity)
        container.orientation = LinearLayout.VERTICAL
        container.addView(dlButton)
        container.addView(gpButton)

        activity.alertDialog {
            titleResource = R.string.set_up_tts_title
            messageResource = R.string.set_up_tts

            neutralButton(R.string.dont_ask_voices) {
                pref.edit().putBoolean(Prefs.DONT_SHOW_VOICES_POPUP.pref, true).apply()
            }
            cancelButton()

            setView(container)
        }.show()
    }
}

fun launchVoicesDownload(activity: Activity, level: Int, finishedListener: () -> Unit) {
    val storage = FirebaseStorage.getInstance()
    val reference = storage.reference.child("Voices_level_$level.zip")
    val fileName = "voices_download_$level"
    val localFile = File.createTempFile(fileName, ".zip")
    val unzipPath = FileUtils.getDataDir(activity, "Voices").absolutePath

    val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
    progressBar.setPadding(40, progressBar.paddingTop, 40, progressBar.paddingBottom)
    progressBar.max = 100

    val progressAlertDialog = activity.alertDialog {
        titleResource = R.string.voice_download_progress
        messageResource = R.string.voices_download_progress_message
        setCancelable(false)
        setView(progressBar)
    }
    progressAlertDialog.show()

    reference.getFile(localFile).addOnSuccessListener {
        FileDownloadService.unzip(localFile.absolutePath, unzipPath)
        val file = File(localFile.absolutePath)
        file.delete()

        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        pref.edit().putBoolean(Prefs.VOICE_DOWNLOADED_LEVEL_V.pref +
                "${getLevelDownloadVersion(level)}_$level", true).apply()
        progressAlertDialog.dismiss()

        activity.alertDialog {
            titleResource = R.string.download_success
            messageResource = R.string.download_success_message

            okButton { finishedListener() }
            setOnCancelListener { finishedListener() }
        }.show()

    }.addOnFailureListener {
        progressAlertDialog.dismiss()

        activity.alertDialog {
            titleResource = R.string.download_failed
            messageResource = R.string.download_failed_message
            okButton()
        }.show()

    }.addOnProgressListener {
        val progress: Double = 100.0 * it.bytesTransferred / it.totalByteCount
        progressBar!!.progress = progress.toInt()
    }

}


fun spotlightWelcome(activity: Activity, target: View, title: String, message: String, listener: SpotlightListener) {
    SpotlightView.Builder(activity)
            .introAnimationDuration(0)
            .performClick(true)
            .fadeinTextDuration(400)
            .headingTvColor(ContextCompat.getColor(activity, R.color.colorAccent))
            .headingTvSize(32)
            .headingTvText(title)
            .subHeadingTvColor(ContextCompat.getColor(activity, R.color.spotlight_subhead))
            .subHeadingTvSize(16)
            .subHeadingTvText(message)
            .maskColor(ContextCompat.getColor(activity, R.color.blackTransparentDarker))
            .target(target)
            .lineAnimDuration(200)
            .lineAndArcColor(ContextCompat.getColor(activity, R.color.colorAccent))
            .dismissOnTouch(true)
            .dismissOnBackPress(true)
            .enableDismissAfterShown(true)
            .enableRevealAnimation(false)
            .usageId(title)
            .setListener(listener)
            .show()
}

fun spotlightTuto(activity: Activity, target: View?, title: String, message: String, listener: SpotlightListener): Boolean {
    val pm = PreferencesManager(activity)
    val ret = pm.isDisplayed(title)
    if (target != null) {
        SpotlightView.Builder(activity)
                .introAnimationDuration(200)
                .performClick(true)
                .fadeinTextDuration(400)
                .headingTvColor(ContextCompat.getColor(activity, R.color.colorAccent))
                .headingTvSize(21)
                .headingTvText(title)
                .subHeadingTvColor(ContextCompat.getColor(activity, R.color.spotlight_subhead))
                .subHeadingTvSize(16)
                .subHeadingTvText(message)
                .maskColor(ContextCompat.getColor(activity, R.color.blackTransparentDark))
                .target(target)
                .lineAnimDuration(200)
                .lineAndArcColor(ContextCompat.getColor(activity, R.color.colorAccent))
                .dismissOnTouch(true)
                .dismissOnBackPress(true)
                .enableDismissAfterShown(true)
                .usageId(title)
                .setListener(listener)
                .show()
    }

    return ret
}

fun spotlightConfig(activity: Activity): SpotlightConfig {
    val config = SpotlightConfig()
    config.introAnimationDuration = 200
    config.isPerformClick = true
    config.fadingTextDuration = 400
    config.headingTvColor = ContextCompat.getColor(activity, R.color.colorAccent)
    config.headingTvSize = 21
    config.subHeadingTvColor = ContextCompat.getColor(activity, R.color.spotlight_subhead)
    config.subHeadingTvSize = 16
    config.maskColor = ContextCompat.getColor(activity, R.color.blackTransparentDark)
    config.lineAnimationDuration = 200
    config.lineAndArcColor = ContextCompat.getColor(activity, R.color.colorAccent)
    config.isDismissOnTouch = true
    config.isDismissOnBackpress = true
    config.isDismissOnTouch = true

    return config
}