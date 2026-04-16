/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.ColorUtils

/**
 * Mirrors [com.android.systemui.alpha.style.themes.ColorParams] and the `applyParams` /
 * `produceColorScheme` logic in QS tile renderers (see SystemUI `QSAerogelStyleRenderer`, etc.).
 */
data class QsStyleColorParams(
    val baseLightness: Float,
    val baseSaturation: Float,
    val baseAlpha: Float,
    val forceLightContent: Boolean,
)

/**
 * Colors after style-specific transforms, for static theme-store tile previews.
 * Active/inactive **fills** follow Tile + renderer: active = transformed accent; inactive = [surfaceEffect1].
 */
data class QsTilePreviewPalette(
    /** Active tile base color (under style overlays). */
    val activeFill: Color,
    /** Inactive tile base ([AlphaPreviewColorScheme.surfaceEffect1]). */
    val inactiveFill: Color,
    val producedNeutral: Color,
    val producedNeutralVariant: Color,
    val onAccent: Color,
    val onNeutral: Color,
    val onNeutralVariant: Color,
)

/** Same HSL transform as [com.android.systemui.alpha.style.qs.renderers.QSAerogelStyleRenderer.applyParams]. */
internal fun applyQsStyleColorParams(
    color: Color,
    params: QsStyleColorParams,
    adjustments: UiStylePreviewAdjustments,
): Color {
    val argb = android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = (hsl[1] * params.baseSaturation * adjustments.saturation).coerceIn(0f, 1f)
    hsl[2] = (params.baseLightness * adjustments.lightness).coerceIn(0f, 1f)
    val newColor = Color(ColorUtils.HSLToColor(hsl))
    return newColor.copy(alpha = (params.baseAlpha * adjustments.opacity).coerceIn(0f, 1f))
}

/** Matches [com.android.systemui.alpha.style.qs.renderers.QSBevelStyleRenderer.tuneColor]. */
internal fun bevelTuneColor(color: Color, adjustments: UiStylePreviewAdjustments): Color {
    val argb = android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = (hsl[1] * adjustments.saturation).coerceIn(0f, 1f)
    hsl[2] = (hsl[2] * adjustments.lightness).coerceIn(0f, 1f)
    val newColor = Color(ColorUtils.HSLToColor(hsl))
    return newColor.copy(alpha = (color.alpha * adjustments.opacity).coerceIn(0f, 1f))
}

private fun onForced(params: QsStyleColorParams, fallback: Color): Color =
    if (params.forceLightContent) Color.White else fallback

/** Shared type so `if (dark) a else b` is not inferred as [Any] (would break `.activeParams`). */
private data class QsStyleDualParams(
    val activeParams: QsStyleColorParams,
    val inactiveParams: QsStyleColorParams,
)

private data class QsStyleSlashParams(
    val activeParams: QsStyleColorParams,
    val inactiveParams: QsStyleColorParams,
    val iconBgParams: QsStyleColorParams,
)

/**
 * Mirrors each renderer’s [com.android.systemui.alpha.style.qs.renderers.QSTileStyleRenderer.produceColorScheme].
 * [isDark] selects Light vs Dark theme objects from SystemUI.
 */
internal fun produceQsTilePreviewPalette(
    styleId: String,
    isDark: Boolean,
    base: AlphaPreviewColorScheme,
    adjustments: UiStylePreviewAdjustments,
): QsTilePreviewPalette {
    val id = UiStyleIds.normalize(styleId)
    val inactiveFill = base.surfaceEffect1

    fun identity(): QsTilePreviewPalette =
        QsTilePreviewPalette(
            activeFill = base.accent,
            inactiveFill = inactiveFill,
            producedNeutral = base.neutral,
            producedNeutralVariant = base.neutralVariant,
            onAccent = base.onAccent,
            onNeutral = base.onNeutral,
            onNeutralVariant = base.onNeutralVariant,
        )

    return when (id) {
        UiStyleIds.SYSTEM_DEFAULT, UiStyleIds.OUTLINE -> identity()

        UiStyleIds.BEVEL ->
            QsTilePreviewPalette(
                activeFill = bevelTuneColor(base.accent, adjustments),
                inactiveFill = inactiveFill,
                producedNeutral = bevelTuneColor(base.neutral, adjustments),
                producedNeutralVariant = bevelTuneColor(base.neutralVariant, adjustments),
                onAccent = base.onAccent,
                onNeutral = base.onNeutral,
                onNeutralVariant = base.onNeutralVariant,
            )

        UiStyleIds.AEROGEL -> {
            val t = if (isDark) aerogelDark else aerogelLight
            val activeFill = applyQsStyleColorParams(base.accent, t.activeParams, adjustments)
            val n = applyQsStyleColorParams(base.neutral, t.inactiveParams, adjustments)
            val nv = applyQsStyleColorParams(base.neutralVariant, t.inactiveParams, adjustments)
            QsTilePreviewPalette(
                activeFill = activeFill,
                inactiveFill = inactiveFill,
                producedNeutral = n,
                producedNeutralVariant = nv,
                onAccent = onForced(t.activeParams, base.onAccent),
                onNeutral = onForced(t.inactiveParams, base.onNeutral),
                onNeutralVariant = onForced(t.inactiveParams, base.onNeutralVariant),
            )
        }

        UiStyleIds.GRADIENT -> {
            val t = if (isDark) gradientDark else gradientLight
            val activeFill = applyQsStyleColorParams(base.accent, t.activeParams, adjustments)
            val n = applyQsStyleColorParams(base.neutral, t.inactiveParams, adjustments)
            val nv = applyQsStyleColorParams(base.neutralVariant, t.inactiveParams, adjustments)
            QsTilePreviewPalette(
                activeFill = activeFill,
                inactiveFill = inactiveFill,
                producedNeutral = n,
                producedNeutralVariant = nv,
                onAccent = onForced(t.activeParams, base.onAccent),
                onNeutral = onForced(t.inactiveParams, base.onNeutral),
                onNeutralVariant = onForced(t.inactiveParams, base.onNeutralVariant),
            )
        }

        UiStyleIds.METALLIC -> {
            val t = if (isDark) metallicDark else metallicLight
            val activeFill = applyQsStyleColorParams(base.accent, t.activeParams, adjustments)
            val n = applyQsStyleColorParams(base.neutral, t.inactiveParams, adjustments)
            val nv = applyQsStyleColorParams(base.neutralVariant, t.inactiveParams, adjustments)
            QsTilePreviewPalette(
                activeFill = activeFill,
                inactiveFill = inactiveFill,
                producedNeutral = n,
                producedNeutralVariant = nv,
                onAccent = onForced(t.activeParams, base.onAccent),
                onNeutral = onForced(t.inactiveParams, base.onNeutral),
                onNeutralVariant = onForced(t.inactiveParams, base.onNeutralVariant),
            )
        }

        UiStyleIds.NEON -> {
            val t = if (isDark) neonDark else neonLight
            val activeFill = applyQsStyleColorParams(base.accent, t.activeParams, adjustments)
            val n = applyQsStyleColorParams(base.neutral, t.inactiveParams, adjustments)
            val nv = applyQsStyleColorParams(base.neutralVariant, t.inactiveParams, adjustments)
            QsTilePreviewPalette(
                activeFill = activeFill,
                inactiveFill = inactiveFill,
                producedNeutral = n,
                producedNeutralVariant = nv,
                onAccent = onForced(t.activeParams, base.onAccent),
                onNeutral = onForced(t.inactiveParams, base.onNeutral),
                onNeutralVariant = onForced(t.inactiveParams, base.onNeutralVariant),
            )
        }

        UiStyleIds.REFLECTIVE -> {
            val t = if (isDark) reflectiveDark else reflectiveLight
            val activeFill = applyQsStyleColorParams(base.accent, t.activeParams, adjustments)
            val n = applyQsStyleColorParams(base.neutral, t.inactiveParams, adjustments)
            val nv = applyQsStyleColorParams(base.neutralVariant, t.inactiveParams, adjustments)
            QsTilePreviewPalette(
                activeFill = activeFill,
                inactiveFill = inactiveFill,
                producedNeutral = n,
                producedNeutralVariant = nv,
                onAccent = onForced(t.activeParams, base.onAccent),
                onNeutral = onForced(t.inactiveParams, base.onNeutral),
                onNeutralVariant = onForced(t.inactiveParams, base.onNeutralVariant),
            )
        }

        UiStyleIds.SLASH -> {
            val t = if (isDark) slashDark else slashLight
            val activeFill = applyQsStyleColorParams(base.accent, t.activeParams, adjustments)
            val n = applyQsStyleColorParams(base.neutral, t.inactiveParams, adjustments)
            val nv = applyQsStyleColorParams(base.neutralVariant, t.iconBgParams, adjustments)
            QsTilePreviewPalette(
                activeFill = activeFill,
                inactiveFill = inactiveFill,
                producedNeutral = n,
                producedNeutralVariant = nv,
                onAccent = onForced(t.activeParams, base.onAccent),
                onNeutral = onForced(t.inactiveParams, base.onNeutral),
                onNeutralVariant = onForced(t.iconBgParams, base.onNeutralVariant),
            )
        }

        else -> identity()
    }
}

// --- Same numeric values as SystemUI `com.android.systemui.alpha.style.themes.*` ---

private val aerogelLight = QsStyleDualParams(
    QsStyleColorParams(0.35f, 1.3f, 0.60f, true),
    QsStyleColorParams(0.90f, 0.1f, 0.50f, false),
)
private val aerogelDark = QsStyleDualParams(
    QsStyleColorParams(0.35f, 1.3f, 0.60f, true),
    QsStyleColorParams(0.15f, 0.1f, 0.50f, true),
)

private val gradientLight = QsStyleDualParams(
    QsStyleColorParams(0.4f, 1.3f, 1.0f, true),
    QsStyleColorParams(0.6f, 0.6f, 1.0f, false),
)
private val gradientDark = QsStyleDualParams(
    QsStyleColorParams(0.25f, 1.1f, 1.0f, true),
    QsStyleColorParams(0.12f, 0.3f, 1.0f, true),
)

private val metallicLight = QsStyleDualParams(
    QsStyleColorParams(0.40f, 1.1f, 1.0f, true),
    QsStyleColorParams(0.85f, 0.0f, 1.0f, false),
)
private val metallicDark = QsStyleDualParams(
    QsStyleColorParams(0.25f, 1.0f, 1.0f, true),
    QsStyleColorParams(0.18f, 0.1f, 1.0f, true),
)

private val neonLight = QsStyleDualParams(
    QsStyleColorParams(0.30f, 1.2f, 0.95f, true),
    QsStyleColorParams(0.30f, 1.0f, 0.90f, true),
)
private val neonDark = QsStyleDualParams(
    QsStyleColorParams(0.15f, 1.4f, 0.90f, true),
    QsStyleColorParams(0.10f, 1.0f, 0.80f, true),
)

private val reflectiveLight = QsStyleDualParams(
    QsStyleColorParams(0.45f, 1.2f, 1.0f, true),
    QsStyleColorParams(0.85f, 0.0f, 1.0f, false),
)
private val reflectiveDark = QsStyleDualParams(
    QsStyleColorParams(0.20f, 1.2f, 1.0f, true),
    QsStyleColorParams(0.15f, 0.1f, 1.0f, true),
)

private val slashLight = QsStyleSlashParams(
    QsStyleColorParams(0.55f, 0.9f, 1.0f, true),
    QsStyleColorParams(0.90f, 0.05f, 1.0f, false),
    QsStyleColorParams(0.80f, 0.2f, 1.0f, false),
)
private val slashDark = QsStyleSlashParams(
    QsStyleColorParams(0.30f, 0.9f, 1.0f, true),
    QsStyleColorParams(0.12f, 0.0f, 1.0f, true),
    QsStyleColorParams(0.25f, 0.2f, 1.0f, true),
)

/** Tertiary blend for gradient-style canvas (unchanged from preview helper). */
internal fun previewTertiaryBlend(accent: Color, tertiary: Color): Color =
    lerp(accent, tertiary, 0.42f)
