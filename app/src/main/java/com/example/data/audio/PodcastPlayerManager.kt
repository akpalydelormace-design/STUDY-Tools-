package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PodcastPlayerState(
    val currentPodcastId: Long? = null,
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

class PodcastPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _playerState = MutableStateFlow(PodcastPlayerState())
    val playerState: StateFlow<PodcastPlayerState> = _playerState.asStateFlow()

    private var onPositionSavedListener: ((podcastId: Long, positionMs: Long) -> Unit)? = null

    fun setOnPositionSavedListener(listener: (podcastId: Long, positionMs: Long) -> Unit) {
        onPositionSavedListener = listener
    }

    fun prepareAndPlay(
        podcastId: Long,
        audioFilePath: String,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true
    ) {
        val file = File(audioFilePath)
        if (!file.exists()) {
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isPrepared = false,
                errorMessage = "Fichier audio introuvable hors connexion."
            )
            return
        }

        try {
            // Stop existing if different or re-preparing
            releaseMediaPlayer()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                if (startPositionMs > 0 && startPositionMs < duration) {
                    seekTo(startPositionMs.toInt())
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration.toLong()
                    )
                    onPositionSavedListener?.invoke(podcastId, duration.toLong())
                    stopPositionUpdates()
                }
            }

            val totalDuration = mediaPlayer?.duration?.toLong()?.coerceAtLeast(0L) ?: 0L

            _playerState.value = PodcastPlayerState(
                currentPodcastId = podcastId,
                isPlaying = autoPlay,
                isPrepared = true,
                currentPositionMs = startPositionMs,
                durationMs = totalDuration,
                errorMessage = null
            )

            if (autoPlay) {
                mediaPlayer?.start()
                startPositionUpdates()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isPrepared = false,
                errorMessage = "Erreur lors de la lecture audio: ${e.localizedMessage}"
            )
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        val currentId = _playerState.value.currentPodcastId ?: return

        if (mp.isPlaying) {
            mp.pause()
            val pos = mp.currentPosition.toLong()
            _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = pos)
            onPositionSavedListener?.invoke(currentId, pos)
            stopPositionUpdates()
        } else {
            mp.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startPositionUpdates()
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        val validPos = positionMs.coerceIn(0L, mp.duration.toLong())
        mp.seekTo(validPos.toInt())
        _playerState.value = _playerState.value.copy(currentPositionMs = validPos)
        _playerState.value.currentPodcastId?.let { id ->
            onPositionSavedListener?.invoke(id, validPos)
        }
    }

    fun rewind10Seconds() {
        val current = _playerState.value.currentPositionMs
        seekTo((current - 10000L).coerceAtLeast(0L))
    }

    fun forward10Seconds() {
        val current = _playerState.value.currentPositionMs
        val duration = _playerState.value.durationMs
        seekTo((current + 10000L).coerceAtMost(duration))
    }

    fun release() {
        saveCurrentPosition()
        releaseMediaPlayer()
        _playerState.value = PodcastPlayerState()
    }

    private fun saveCurrentPosition() {
        val mp = mediaPlayer ?: return
        val currentId = _playerState.value.currentPodcastId ?: return
        val pos = mp.currentPosition.toLong()
        onPositionSavedListener?.invoke(currentId, pos)
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        updateJob = scope.launch {
            while (true) {
                delay(500)
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val current = mp.currentPosition.toLong()
                        _playerState.value = _playerState.value.copy(currentPositionMs = current)
                    }
                }
            }
        }
    }

    private fun stopPositionUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun releaseMediaPlayer() {
        stopPositionUpdates()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
    }
}
