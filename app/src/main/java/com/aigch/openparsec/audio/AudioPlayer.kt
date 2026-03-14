package com.aigch.openparsec.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * Audio player using Android AudioTrack.
 * Ported from iOS audio.c AudioQueue implementation.
 *
 * Handles PCM audio playback at 48kHz stereo 16-bit,
 * matching the Parsec SDK audio callback format.
 */
class AudioPlayer {
    companion object {
        private const val TAG = "AudioPlayer"
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var isMuted = false
    private var isStarted = false

    fun init() {
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .setEncoding(AUDIO_FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        Log.d(TAG, "AudioPlayer initialized with buffer size: $bufferSize")
    }

    fun destroy() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        isStarted = false
    }

    fun clear() {
        audioTrack?.pause()
        audioTrack?.flush()
        isStarted = false
    }

    /**
     * Audio callback matching Parsec SDK audio_cb signature.
     * Writes PCM data to the AudioTrack.
     */
    fun audioCallback(pcm: ShortArray, frames: Int) {
        if (frames == 0 || isMuted) return

        val track = audioTrack ?: return
        track.write(pcm, 0, frames * 2) // stereo: 2 samples per frame

        if (!isStarted) {
            track.play()
            isStarted = true
        }
    }

    fun mute(muted: Boolean) {
        if (isMuted == muted) return
        isMuted = muted
        if (muted) {
            audioTrack?.pause()
            clear()
        }
    }
}
