/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class UiStylePreviewAdjustments(
    val strength: Float = 1f,
    val opacity: Float = 1f,
    val saturation: Float = 1f,
    val lightness: Float = 1f,
    val angle: Float = 0f,
)

object UiStyleIds {
    /** Must match [com.android.systemui.alpha.style.UiStyleRepository.SYSTEM_DEFAULT]. */
    const val SYSTEM_DEFAULT = "system_default"
    const val OUTLINE = "outline"
    const val NEON = "neon"
    const val BEVEL = "bevel"
    const val GRADIENT = "gradient"
    const val REFLECTIVE = "reflective"
    const val SLASH = "slash"
    const val AEROGEL = "aerogel"
    const val METALLIC = "metallic"

    val ALL = listOf(
        SYSTEM_DEFAULT,
        OUTLINE,
        NEON,
        BEVEL,
        GRADIENT,
        REFLECTIVE,
        SLASH,
        AEROGEL,
        METALLIC,
    )

    fun normalize(id: String): String = id.takeIf { it in ALL } ?: SYSTEM_DEFAULT
}

private val TileShape = RoundedCornerShape(18.dp)

/** Static decorative preview only; live tuning is shown in the System UI volume panel. */
@Composable
fun QsTileUiStylePreview(
    styleId: String,
    modifier: Modifier = Modifier,
    tileLabel: String = "Wi‑Fi",
    isActive: Boolean = true,
    icon: ImageVector = Icons.Filled.Wifi,
) {
    val effectiveAdjustments = UiStylePreviewAdjustments()
    val material = MaterialTheme.colorScheme
    val alpha = rememberAlphaPreviewColorScheme()
    val id = UiStyleIds.normalize(styleId)
    val isDark =
        (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    val palette = produceQsTilePreviewPalette(id, isDark, alpha, effectiveAdjustments)

    val activeFill = palette.activeFill
    val inactiveFill = palette.inactiveFill
    val tertiary = previewTertiaryBlend(alpha.accent, material.tertiary)
    val neutral = palette.producedNeutral
    val inactiveDeep = lerp(inactiveFill, Color.Black, 0.42f)

    val iconTint = if (isActive) palette.onAccent else palette.onNeutral
    val labelColor = if (isActive) palette.onAccent else palette.onNeutral

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 148.dp, minHeight = PreviewDimensions.UiStyleTileMinHeight)
            .clip(TileShape)
    ) {
        TileStyleBackground(
            styleId = id,
            isActive = isActive,
            isDark = isDark,
            activeFill = activeFill,
            inactiveFill = inactiveFill,
            tertiary = tertiary,
            neutral = neutral,
            inactiveDeep = inactiveDeep,
            colors = alpha,
            adjustments = effectiveAdjustments,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tileLabel,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (id == UiStyleIds.SLASH) {
            SlashOverlayTile(
                isActive = isActive,
                isDark = isDark,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun UiStyleHeroPreview(
    styleId: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            ColumnishContent(
                styleId = styleId,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ColumnishContent(
    styleId: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        QsTileUiStylePreview(
            styleId = styleId,
            tileLabel = "Internet",
            modifier = Modifier
                .width(240.dp)
                .height(PreviewDimensions.UiStyleTileHeroLargeHeight),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QsTileUiStylePreview(
                styleId = styleId,
                tileLabel = "Bluetooth",
                isActive = false,
                modifier = Modifier
                    .weight(1f)
                    .height(PreviewDimensions.UiStyleTileHeroSmallHeight),
            )
            QsTileUiStylePreview(
                styleId = styleId,
                tileLabel = "Flashlight",
                modifier = Modifier
                    .weight(1f)
                    .height(PreviewDimensions.UiStyleTileHeroSmallHeight),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PreviewChip("Quick Settings")
            PreviewChip("Volume panel")
            PreviewChip(uiStyleTitle(styleId))
        }
    }
}

@Composable
private fun PreviewChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun TileStyleBackground(
    styleId: String,
    isActive: Boolean,
    isDark: Boolean,
    activeFill: Color,
    inactiveFill: Color,
    tertiary: Color,
    neutral: Color,
    inactiveDeep: Color,
    colors: AlphaPreviewColorScheme,
    adjustments: UiStylePreviewAdjustments,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(TileShape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val id = UiStyleIds.normalize(styleId)
            val corner = CornerRadius(18.dp.toPx(), 18.dp.toPx())
            val w = size.width
            val h = size.height
            // Base fill: renderer `materialColor` after produceColorScheme (active) vs surfaceEffect1 (inactive).
            val accent = activeFill
            val surfaceEffect1 = inactiveFill
            val body = if (isActive) accent else surfaceEffect1
            val bodySoft = lerp(body, Color.White, if (isActive) 0.1f else 0.05f)
            val bodyDark = lerp(body, Color.Black, if (isActive) 0.18f else 0.32f)

            fun offsets(angleDegrees: Float): Pair<Offset, Offset> {
                val radians = Math.toRadians(angleDegrees.toDouble())
                val dx = cos(radians).toFloat() * w / 2f
                val dy = sin(radians).toFloat() * h / 2f
                val cx = w / 2f
                val cy = h / 2f
                return Offset(cx - dx, cy - dy) to Offset(cx + dx, cy + dy)
            }

            when (id) {
                UiStyleIds.SYSTEM_DEFAULT -> {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to bodySoft,
                                0.5f to body,
                                1f to bodyDark,
                            ),
                            startY = 0f,
                            endY = h,
                        ),
                        cornerRadius = corner,
                    )
                }

                UiStyleIds.OUTLINE -> {
                    if (isActive) {
                        drawRoundRect(color = accent, cornerRadius = corner)
                        // Darker accent border — visible contrast against the filled accent background.
                        val border = lerp(accent, Color.Black, 0.44f)
                            .copy(alpha = (0.90f * adjustments.opacity).coerceIn(0.55f, 1f))
                        drawRoundRect(
                            color = border,
                            cornerRadius = corner,
                            style = Stroke(width = 2.4.dp.toPx()),
                        )
                    } else {
                        drawRoundRect(color = surfaceEffect1, cornerRadius = corner)
                        // Accent-derived border: stands out clearly against the neutral fill.
                        val accentBorder = lerp(accent, Color.Black, 0.20f)
                            .copy(alpha = (0.82f * adjustments.opacity).coerceIn(0.55f, 1f))
                        drawRoundRect(
                            color = accentBorder,
                            cornerRadius = corner,
                            style = Stroke(width = 2.4.dp.toPx()),
                        )
                    }
                }

                UiStyleIds.NEON -> {
                    val cx = w / 2f
                    val cy = h / 2f
                    val radius = max(w, h) * (0.72f + (adjustments.strength - 1f) * 0.1f)
                    val coreBase =
                        if (isActive) lerp(accent, Color.Black, 0.48f) else inactiveDeep
                    val core = lerp(coreBase, accent, if (isActive) 0.12f else 0.1f)
                    drawRoundRect(color = core.copy(alpha = 0.92f), cornerRadius = corner)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.35f to accent.copy(alpha = 0.08f * adjustments.opacity),
                                0.62f to accent.copy(alpha = 0.28f * adjustments.opacity),
                                0.88f to accent.copy(alpha = 0.52f * adjustments.opacity),
                                1f to accent.copy(alpha = 0.62f * adjustments.opacity),
                            ),
                            center = Offset(cx, cy),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                    val ringAlpha = (0.38f + 0.2f * adjustments.strength) * adjustments.opacity
                    drawRoundRect(
                        color = accent.copy(alpha = ringAlpha.coerceIn(0.15f, 0.85f)),
                        cornerRadius = corner,
                        style = Stroke(width = if (isActive) 2.2.dp.toPx() else 1.1.dp.toPx()),
                    )
                }

                UiStyleIds.BEVEL -> {
                    // Very subtle fill gradient (surfaceGradientAlpha ≈ 0.08 light / 0.05 dark).
                    val gradA = if (isDark) 0.05f else 0.08f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lerp(body, Color.White, gradA),
                                body,
                                lerp(body, Color.Black, gradA),
                            ),
                            startY = 0f,
                            endY = h,
                        ),
                        cornerRadius = corner,
                    )
                    // Bevel stroke following the rounded rect — highlight top, shadow bottom.
                    // Values from BevelLightTheme / BevelDarkTheme (bevelWidth = 2.5dp).
                    val highlight = if (isActive) {
                        if (isDark) Color.White.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.80f)
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.30f)
                    }
                    val shadow = if (isActive) {
                        if (isDark) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.95f)
                    } else {
                        if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.55f)
                    }
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(highlight, shadow),
                            startY = 0f,
                            endY = h,
                        ),
                        cornerRadius = corner,
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                    // Thin inner ring (outlineActiveAlpha 0.12/0.08, outlineInactiveAlpha 0.08/0.05).
                    val outlineAlpha = if (isActive) {
                        if (isDark) 0.08f else 0.12f
                    } else {
                        if (isDark) 0.05f else 0.08f
                    }
                    drawRoundRect(
                        color = Color.White.copy(alpha = outlineAlpha * adjustments.opacity),
                        cornerRadius = corner,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }

                UiStyleIds.GRADIENT -> {
                    val (start, end) = offsets(38f + adjustments.angle)
                    val light = lerp(accent, Color.White, 0.42f)
                    val deep = lerp(accent, Color.Black, 0.55f)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to light.copy(alpha = 0.95f * adjustments.opacity),
                                0.45f to accent.copy(alpha = 0.88f * adjustments.opacity),
                                0.82f to deep.copy(alpha = 0.92f * adjustments.opacity),
                                1f to lerp(deep, tertiary, 0.15f).copy(alpha = 0.85f * adjustments.opacity),
                            ),
                            start = start,
                            end = end,
                        ),
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.18f * adjustments.opacity),
                                0.5f to Color.Transparent,
                                1f to Color.Transparent,
                            ),
                            center = Offset(w * 0.25f, h * 0.18f),
                            radius = w * 0.85f,
                        ),
                        cornerRadius = corner,
                    )
                }

                UiStyleIds.REFLECTIVE -> {
                    val refl = 0.55f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to lerp(body, Color.White, 0.14f * refl),
                                0.12f to bodySoft,
                                0.55f to body,
                                1f to bodyDark,
                            ),
                            startY = 0f,
                            endY = h,
                        ),
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.18f * refl * adjustments.opacity),
                                0.12f to Color.White.copy(alpha = 0.06f * refl * adjustments.opacity),
                                0.4f to Color.Transparent,
                                1f to Color.Transparent,
                            ),
                            startY = 0f,
                            endY = h * 0.32f,
                        ),
                        cornerRadius = corner,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f * refl * adjustments.opacity),
                        start = Offset(12.dp.toPx(), 7.dp.toPx()),
                        end = Offset(w - 12.dp.toPx(), 7.dp.toPx()),
                        strokeWidth = 0.9.dp.toPx(),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.035f * adjustments.opacity),
                        cornerRadius = corner,
                        style = Stroke(width = 0.85.dp.toPx()),
                    )
                }

                UiStyleIds.SLASH -> {
                    // Vertical base only — diagonal “slash” comes from [SlashOverlayTile] alone
                    // so we don’t stack two diagonal motifs (gradient + overlay).
                    val base =
                        if (isActive) lerp(neutral, accent, 0.55f) else inactiveDeep
                    val dark = lerp(lerp(base, accent, 0.15f), Color(0xFF12141A), 0.5f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to lerp(base, Color.White, 0.05f),
                                0.5f to base,
                                1f to lerp(dark, tertiary, 0.1f),
                            ),
                            startY = 0f,
                            endY = h,
                        ),
                        cornerRadius = corner,
                    )
                }

                UiStyleIds.AEROGEL -> {
                    val glassTop = lerp(body, Color.White, 0.28f).copy(alpha = 0.88f)
                    val glassBot = lerp(body, Color.Black, 0.12f).copy(alpha = 0.9f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to glassTop,
                                0.45f to body.copy(alpha = 0.82f),
                                1f to glassBot,
                            ),
                            startY = 0f,
                            endY = h,
                        ),
                        cornerRadius = corner,
                    )
                    val topGlow = if (body.luminance() > 0.45f) 0.32f else 0.22f
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = topGlow * adjustments.opacity),
                                0.38f to lerp(accent, Color.White, 0.4f).copy(alpha = 0.18f * adjustments.opacity),
                                0.72f to accent.copy(alpha = 0.08f * adjustments.opacity),
                                1f to Color.Transparent,
                            ),
                            center = Offset(w * 0.5f, -h * 0.05f),
                            radius = w * 1.1f,
                        ),
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f * adjustments.opacity),
                        cornerRadius = corner,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }

                UiStyleIds.METALLIC -> {
                    val (start, end) = offsets(52f + adjustments.angle)
                    val shade = if (isActive) accent else inactiveDeep
                    val gleam = lerp(Color.White, accent, 0.2f)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to lerp(shade, Color.Black, 0.45f).copy(alpha = 0.95f * adjustments.opacity),
                                0.2f to shade.copy(alpha = 0.55f * adjustments.opacity),
                                0.48f to gleam.copy(alpha = 0.55f * adjustments.opacity),
                                0.72f to lerp(shade, Color.White, 0.25f).copy(alpha = 0.75f * adjustments.opacity),
                                1f to lerp(shade, Color.Black, 0.35f).copy(alpha = 0.9f * adjustments.opacity),
                            ),
                            start = start,
                            end = end,
                        ),
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.14f * adjustments.opacity),
                                0.5f to Color.Transparent,
                                1f to Color.Transparent,
                            ),
                            start = Offset(w * 0.1f, 0f),
                            end = Offset(w * 0.55f, h * 0.35f),
                        ),
                        cornerRadius = corner,
                    )
                }
            }
        }
    }
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

private fun tunedColor(
    base: Color,
    adjustments: UiStylePreviewAdjustments,
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(base.toArgb(), hsl)
    hsl[1] = (hsl[1] * adjustments.saturation).coerceIn(0f, 1f)
    hsl[2] = (hsl[2] * adjustments.lightness).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl)).copy(
        alpha = (base.alpha * adjustments.opacity).coerceIn(0f, 1f),
    )
}

/**
 * Slash overlay: a diagonal cut at [slashStartRatio] = 0.88 of the tile width, angled 20° from
 * vertical ([SlashLightTheme] / [SlashDarkTheme] in SystemUI). The small right strip is darkened
 * by [slashAlpha] and separated by a thin cut line at [cutLineAlpha].
 */
@Composable
private fun SlashOverlayTile(
    isActive: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    // From SlashLightTheme / SlashDarkTheme.
    val slashAlpha = if (isDark) 0.2f else 0.1f
    val cutLineAlpha = if (isDark) 0.2f else 0.4f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // 20° from vertical → shift in x per unit height = sin(20°) ≈ 0.342.
        val dx = (h / 2f) * sin(Math.toRadians(20.0)).toFloat()
        val topX = w * 0.88f - dx
        val botX = w * 0.88f + dx

        // Dark fill on the right portion (to the right of the slash line).
        val rightPath = Path().apply {
            moveTo(topX, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            lineTo(botX, h)
            close()
        }
        drawPath(path = rightPath, color = Color.Black.copy(alpha = slashAlpha))

        // Thin cut line.
        drawLine(
            color = Color.White.copy(alpha = cutLineAlpha),
            start = Offset(topX, 0f),
            end = Offset(botX, h),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}