package component

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory


class ExoPlayerAudio(context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val rendererAudio = MediaCodecAudioRenderer(MediaCodecSelector.DEFAULT, handler, null)
    private val trackSelector = DefaultTrackSelector(DefaultBandwidthMeter())
    private val dataSourceFactory = DefaultDataSourceFactory(context, "UserAgent")
    private val extractors = DefaultExtractorsFactory()

    val exoPlayer: ExoPlayer = ExoPlayerFactory.newInstance(arrayOf(rendererAudio), trackSelector)

    fun extractorMediaSource(uri: Uri) = ExtractorMediaSource(uri, dataSourceFactory, extractors, handler, null)
}