package com.jehutyno.yomikata.audio

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.FileUtils
import com.jehutyno.yomikata.util.SpeechAvailability
import com.jehutyno.yomikata.util.checkSpeechAvailability
import com.jehutyno.yomikata.util.sentenceNoFuri
import com.jehutyno.yomikata.util.speechNotSupportedAlert
import com.jehutyno.yomikata.util.quiz.getCategoryLevel


/**
 * Created by valentinlanfranchi on 01/09/2017.
 */
class VoicesManager(val context: Activity) {

    private val exoPlayerAudio: ExoPlayerAudio = ExoPlayerAudio(context)
    private val exoPlayer = exoPlayerAudio.exoPlayer

    fun speakSentence(sentence: Sentence, ttsSupported: Int, tts: TextToSpeech?) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            val toast = Toast.makeText(context, R.string.message_adjuste_volume, Toast.LENGTH_LONG)
            toast.show()
        }
        when (checkSpeechAvailability(context, ttsSupported, sentence.level)) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                try {
                    exoPlayer.setMediaItem(exoPlayerAudio.extractorMediaSource(Uri.parse("${FileUtils.getDataDir(context, "Voices").absolutePath}/s_${sentence.id}.mp3")))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } catch (e: Exception) {
                    speechNotSupportedAlert(context, sentence.level) {}
                }
            }
            SpeechAvailability.TTS_AVAILABLE -> {
                tts?.speak(sentenceNoFuri(sentence), TextToSpeech.QUEUE_FLUSH, null, null)
            }
            else -> speechNotSupportedAlert(context, sentence.level) {}
        }
    }

    fun speakWord(word: Word, ttsSupported: Int, tts: TextToSpeech?) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            val toast = Toast.makeText(context, R.string.message_adjuste_volume, Toast.LENGTH_SHORT)
            toast.show()
        }
        val level = getCategoryLevel(word.baseCategory)

        when (checkSpeechAvailability(context, ttsSupported, level)) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                try {
                    exoPlayer.setMediaItem(exoPlayerAudio.extractorMediaSource(Uri.parse("${FileUtils.getDataDir(context, "Voices").absolutePath}/w_${word.id}.mp3")))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } catch (e: Exception) {
                    speechNotSupportedAlert(context, level) {}
                }
            }
            SpeechAvailability.TTS_AVAILABLE -> {
                tts?.speak(
                    if (word.isKana >= 1)
                        word.japanese.split("/")[0].split(";")[0]
                    else
                        word.reading.split("/")[0].split(";")[0],
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
            }
            else -> speechNotSupportedAlert(context, level) {}
        }
    }

    fun releasePlayer() {
        exoPlayer.release()
    }

}
