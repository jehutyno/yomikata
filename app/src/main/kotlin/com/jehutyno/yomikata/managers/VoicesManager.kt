package com.jehutyno.yomikata.managers

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.*
import component.ExoPlayerAudio
import org.jetbrains.anko.toast

/**
 * Created by valentinlanfranchi on 01/09/2017.
 */
class VoicesManager(val context: Activity) {

    val exoPlayerAudio: ExoPlayerAudio = ExoPlayerAudio(context)
    private val exoPlayer = exoPlayerAudio.exoPlayer

    fun speakSentence(sentence: Sentence, ttsSupported: Int, tts: TextToSpeech?) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            context.toast(context.getString(R.string.message_adjuste_volume))
        }
        val speechAvailability = checkSpeechAvailability(context, ttsSupported, sentence.level)
        when (speechAvailability) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                try {
                    exoPlayer.prepare(exoPlayerAudio.extractorMediaSource(Uri.parse("${FileUtils.getDataDir(context, "Voices").absolutePath}/s_${sentence.id}.mp3")))
                    exoPlayer.playWhenReady = true
                } catch (e: Exception) {
                    if (speechAvailability == SpeechAvailability.TTS_AVAILABLE)
                        tts?.speak(sentenceNoFuri(sentence), TextToSpeech.QUEUE_FLUSH, null)
                    else
                        speechNotSupportedAlert(context, sentence.level, {})
                }
            }
            SpeechAvailability.TTS_AVAILABLE -> tts?.speak(sentenceNoFuri(sentence), TextToSpeech.QUEUE_FLUSH, null)
            else -> speechNotSupportedAlert(context, sentence.level, {})
        }
    }

    fun speakWord(word: Word, ttsSupported: Int, tts: TextToSpeech?) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            context.toast(context.getString(R.string.message_adjuste_volume))
        }
        val level = getCateogryLevel(word.baseCategory)
        val speechAvailability = checkSpeechAvailability(context, ttsSupported, level)

        when (speechAvailability) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                try {
                    exoPlayer.prepare(exoPlayerAudio.extractorMediaSource(Uri.parse("${FileUtils.getDataDir(context, "Voices").absolutePath}/w_${word.id}.mp3")))
                    exoPlayer.playWhenReady = true
                } catch (e: Exception) {
                    if (speechAvailability == SpeechAvailability.TTS_AVAILABLE) {
                        tts?.speak(
                            if (word.isKana >= 1)
                                word.japanese.split("/")[0].split(";")[0]
                            else
                                word.reading.split("/")[0].split(";")[0],
                            TextToSpeech.QUEUE_FLUSH, null)
                    } else
                        speechNotSupportedAlert(context, level, {})
                }
            }
            SpeechAvailability.TTS_AVAILABLE -> {
                tts?.speak(
                    if (word.isKana >= 1)
                        word.japanese.split("/")[0].split(";")[0]
                    else
                        word.reading.split("/")[0].split(";")[0],
                    TextToSpeech.QUEUE_FLUSH, null)
            }
            else -> speechNotSupportedAlert(context, level, {})
        }
    }

    fun releasePlayer() {
        exoPlayer.release()
    }

}