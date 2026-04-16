/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Theme-store preview aligned with
 * [com.android.systemui.alpha.style.common.AlphaColorScheme] and QS [TileColors] in SystemUI:
 *
 * - **Active tile background** in SystemUI is [accent] ([ColorScheme.primary]); overlays draw on top.
 * - **Inactive tile background** is [surfaceEffect1] ([LocalAndroidColorScheme.surfaceEffect1] in
 *   SystemUI). We approximate it from Material surface-container steps when Android tokens are absent.
 * - **Inactive icon tint** uses [neutral] (SystemUI maps this to surface **effect2**).
 * - **Inactive labels** use [onSurface] (same as Tile inactive label color).
 */
@Immutable
data class AlphaPreviewColorScheme(
    val accent: Color,
    val onAccent: Color,
    /** Inactive primary label color — [ColorScheme.onSurface], matches QS tile labels. */
    val onSurface: Color,
    /**
     * Inactive QS tile fill — matches [AndroidColorScheme.surfaceEffect1] (see `surface_effect_1`).
     * Not the same as [neutral]; inactive background uses this, icon tint uses [neutral].
     */
    val surfaceEffect1: Color,
    /** Surface effect2 analog — inactive icon / neutral accents (see [AlphaColorScheme.neutral]). */
    val neutral: Color,
    val onNeutral: Color,
    val neutralVariant: Color,
    val onNeutralVariant: Color,
    val outline: Color,
    val surface: Color,
    val thumb: Color,
)

@Composable
fun rememberAlphaPreviewColorScheme(): AlphaPreviewColorScheme {
    val scheme = MaterialTheme.colorScheme
    return remember(
        scheme.primary,
        scheme.surface,
        scheme.surfaceContainerLow,
        scheme.surfaceContainer,
        scheme.surfaceContainerHigh,
        scheme.surfaceContainerHighest,
        scheme.outline,
        scheme.onPrimary,
        scheme.onSurface,
        scheme.onSurfaceVariant,
    ) {
        alphaPreviewColorSchemeFromMaterial(scheme)
    }
}

/** Exposed for previews/tests without remember(). */
fun alphaPreviewColorSchemeFromMaterial(scheme: ColorScheme): AlphaPreviewColorScheme {
    // Order matches typical elevation: panel < tile < icon chip (see SystemUI AndroidColorScheme).
    val surfaceEffect1 = lerp(
        scheme.surfaceContainerLow,
        scheme.surfaceContainer,
        0.62f,
    )
    // Effect2 / neutral2 analog — between container and high (MaterialColorResolver neutral2 feel).
    val neutral = lerp(
        scheme.surfaceContainer,
        scheme.surfaceContainerHigh,
        0.52f,
    )
    val neutralVariant = lerp(
        scheme.surfaceContainerHigh,
        scheme.surfaceContainerHighest,
        0.48f,
    )
    return AlphaPreviewColorScheme(
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        onSurface = scheme.onSurface,
        surfaceEffect1 = surfaceEffect1,
        neutral = neutral,
        onNeutral = scheme.onSurface,
        neutralVariant = neutralVariant,
        onNeutralVariant = scheme.onSurfaceVariant,
        outline = scheme.outline,
        surface = scheme.surface,
        thumb = scheme.primary,
    )
}
