/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.systemui

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Refreshes the **SystemUI volume dialog** while style tuning is written to [android.provider.Settings].
 * [AudioManager.adjustStreamVolume] with [AudioManager.ADJUST_SAME] and [AudioManager.FLAG_SHOW_UI]
 * shows/refreshes the UI without changing level (typically resets auto-dismiss).
 *
 * Quick Settings is intentionally not opened from AlphaVisuals; live feedback is the volume panel.
 */
object SystemUiLivePreview {
    private const val TAG = "SystemUiLivePreview"

    private val mainHandler = Handler(Looper.getMainLooper())

    fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    fun pulseVolumeDialog(context: Context) {
        runOnMainThread {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI,
                )
            } catch (e: Exception) {
                Log.w(TAG, "pulseVolumeDialog failed", e)
            }
        }
    }
}
