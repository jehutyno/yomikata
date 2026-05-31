package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.databinding.FragmentQuizBinding
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.util.Prefs
import android.widget.SeekBar

class SettingsUIManager(
    private val binding: FragmentQuizBinding,
    private val voicesManager: VoicesManager,
    private val presenter: QuizContract.Presenter
) {

    var isSettingsOpen = false
    private val settingsAnimationOffset = -900f

    fun setUp(tts: TextToSpeech?) {
        val audioManager = binding.root.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        binding.seekVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.seekVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p1, 0)
                presenter.onSpeakSentence()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        binding.seekSpeed.max = 250
        val pref = PreferenceManager.getDefaultSharedPreferences(binding.root.context)
        val rate = pref.getInt(Prefs.TTS_RATE.pref, 50)
        binding.seekSpeed.progress = rate
        tts?.setSpeechRate((rate + 50).toFloat() / 100)
        binding.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                pref.edit().putInt(Prefs.TTS_RATE.pref, p1).apply()
                tts?.setSpeechRate((p1 + 50).toFloat() / 100)
                presenter.onSpeakSentence()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
        binding.settingsContainer.translationY = settingsAnimationOffset
        binding.settingsClose.setOnClickListener {
            close()
        }
    }

    fun close() {
        binding.settingsContainer.animate().setDuration(300).translationY(settingsAnimationOffset).withEndAction {
            isSettingsOpen = false
            binding.settingsContainer.visibility = GONE
        }.start()
    }

    fun openIfAvailable(speechAvailability: com.jehutyno.yomikata.util.SpeechAvailability) {
        if (speechAvailability == com.jehutyno.yomikata.util.SpeechAvailability.VOICES_AVAILABLE) {
            binding.settingsSpeed.visibility = GONE
            binding.seekSpeed.visibility = GONE
        } else {
            binding.settingsSpeed.visibility = VISIBLE
            binding.seekSpeed.visibility = VISIBLE
        }
        binding.settingsContainer.animate().setDuration(300).translationY(0f).withStartAction {
            binding.settingsContainer.visibility = VISIBLE
            isSettingsOpen = true
        }.start()
    }

}
