package player

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

interface Player {
    fun play(input: ByteArray)
    fun stop()
}

class VPPlayer(
    private val codec: Codec,
    sampleRate: Int = 16_000, // Sample rate in Hz
    channelConfig: Int = AudioFormat.CHANNEL_IN_MONO, // Channel configuration
    audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT, // Audio format
    mode: Int = AudioTrack.MODE_STREAM // Playback mode
) : Player {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val worker = CoroutineScope(dispatcher)

    private lateinit var audioTrack: AudioTrack

    init {
        worker.launch {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

            audioTrack =  AudioTrack(
                AudioManager.STREAM_MUSIC, // Stream type
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                mode
            )
        }
    }

    override fun play(input: ByteArray) {
        worker.launch {
            val decoded = codec.decode(input)
            audioTrack.write(decoded, 0, decoded.size)
            if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.play()
            }
        }
    }

    override fun stop() {
        worker.launch {
            audioTrack.stop()
        }
    }
}