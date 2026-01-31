package component

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class ExoPlayerAudio(context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    fun extractorMediaSource(uri: Uri): MediaItem = MediaItem.fromUri(uri)
}