/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.model

import com.alpha.settings.ui.ui.components.UiStyleIds
import com.android.internal.alpha.style.UserStyleSettings
import kotlin.math.roundToInt

enum class UiStyleMode {
    LIGHT,
    DARK,
}

/**
 * Slider-backed tuning (see [com.android.systemui.alpha.tiles.dialog.UiStyleContentManager]).
 * Int ranges match the detail screen sliders and map to [UserStyleSettings] floats.
 */
data class UiStyleTuning(
    val saturation: Int = 100,
    val lightness: Int = 100,
    val opacity: Int = 100,
    val strength: Int = 89,
    val angle: Int = 180,
) {
    fun toUserStyleSettings(): UserStyleSettings = UserStyleSettings(
        saturation.coerceIn(0, 200) / 100f,
        lightness.coerceIn(0, 200) / 100f,
        0.2f + opacity.coerceIn(0, 100) / 100f * 0.8f,
        0.2f + strength.coerceIn(0, 200) / 200f * 1.8f,
        (angle.coerceIn(0, 360) - 180).toFloat(),
    )

    companion object {
        fun fromUserStyleSettings(settings: UserStyleSettings): UiStyleTuning {
            val s = settings
            return UiStyleTuning(
                saturation = (s.saturation * 100f).roundToInt().coerceIn(0, 200),
                lightness = (s.lightness * 100f).roundToInt().coerceIn(0, 200),
                opacity = ((s.opacity - 0.2f) / 0.8f * 100f).roundToInt().coerceIn(0, 100),
                strength = ((s.strength - 0.2f) / 1.8f * 200f).roundToInt().coerceIn(0, 200),
                angle = (s.angle + 180f).roundToInt().coerceIn(0, 360),
            )
        }

        val Default: UiStyleTuning
            get() = fromUserStyleSettings(UserStyleSettings.DEFAULT)
    }
}

data class UiStyleEditorState(
    val styleId: String = UiStyleIds.SYSTEM_DEFAULT,
    val light: UiStyleTuning = UiStyleTuning.Default,
    val dark: UiStyleTuning = UiStyleTuning.Default,
) {
    fun tuningFor(mode: UiStyleMode): UiStyleTuning = when (mode) {
        UiStyleMode.LIGHT -> light
        UiStyleMode.DARK -> dark
    }
}
