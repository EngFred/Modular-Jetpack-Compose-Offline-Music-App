package com.engfred.musicplayer.feature_player.data.repository.controller

import android.content.Context
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Listens to the audio session and extracts bass intensity
 * that can be used for UI visualizations (e.g. pulsing animations).
 */
class BassVisualizer(private val context: Context) {

    private var visualizer: Visualizer? = null
    private val _bassIntensity = MutableStateFlow(0f)
    val bassIntensity: StateFlow<Float> = _bassIntensity.asStateFlow()

    fun attachToSession(sessionId: Int) {
        release()
        try {
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]

                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform == null) return
                        val amplitude = waveform.map { abs(it.toInt()) }.average().toFloat()
                        _bassIntensity.value = (amplitude / 128f).coerceIn(0f, 1f)
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null) return
                        val bass = abs(fft[2].toInt()) + abs(fft[3].toInt())
                        _bassIntensity.value = (bass / 256f).coerceIn(0f, 1f)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)

                enabled = true
            }
        } catch (e: Exception) {
            Log.e("BassVisualizer", "Failed to attach visualizer: ${e.message}")
        }
    }

    fun release() {
        visualizer?.release()
        visualizer = null
        _bassIntensity.value = 0f
    }
}
