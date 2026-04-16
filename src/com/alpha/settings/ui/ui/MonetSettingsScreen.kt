/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alpha.settings.ui.R
import com.alpha.settings.ui.monet.utils.ColorPickerDialog
import com.alpha.settings.ui.monet.utils.WallpaperColorPickerDialog
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "MonetSettingsScreen"

private const val OVERLAY_CATEGORY_ACCENT_COLOR = "android.theme.customization.accent_color"
private const val OVERLAY_CATEGORY_SYSTEM_PALETTE = "android.theme.customization.system_palette"
private const val OVERLAY_CATEGORY_THEME_STYLE = "android.theme.customization.theme_style"
private const val OVERLAY_CATEGORY_BG_COLOR = "android.theme.customization.bg_color"
private const val OVERLAY_COLOR_SOURCE = "android.theme.customization.color_source"
private const val OVERLAY_COLOR_BOTH = "android.theme.customization.color_both"
private const val OVERLAY_LUMINANCE_FACTOR = "android.theme.customization.luminance_factor"
private const val OVERLAY_CHROMA_FACTOR = "android.theme.customization.chroma_factor"
private const val OVERLAY_TINT_BACKGROUND = "android.theme.customization.tint_background"
private const val OVERLAY_FIDELITY = "android.theme.customization.fidelity"
private const val TIMESTAMP_FIELD = "_applied_timestamp"

private const val COLOR_SOURCE_PRESET = "preset"
private const val COLOR_SOURCE_HOME = "home_wallpaper"
private const val COLOR_SOURCE_BOTH_VALUE = "both"
private const val DEFAULT_STYLE = "TONAL_SPOT"
private const val STYLE_MONOCHROMATIC = "MONOCHROMATIC"
private const val STYLE_RAINBOW = "RAINBOW"
private const val DEFAULT_COLOR = 0xFF1B6EF3.toInt()

private val settingsHandler = Handler(Looper.getMainLooper())

private fun readSettingsJson(context: Context): JSONObject {
    val json = Settings.Secure.getStringForUser(
        context.contentResolver,
        Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
        UserHandle.USER_CURRENT,
    )
    return if (json.isNullOrEmpty()) JSONObject() else try {
        JSONObject(json)
    } catch (e: Exception) {
        JSONObject()
    }
}

private fun writeSettingsJsonDebounced(
    context: Context,
    obj: JSONObject,
    pendingRunnable: MutableList<Runnable>,
) {
    val json = obj.toString()
    pendingRunnable.firstOrNull()?.let { settingsHandler.removeCallbacks(it) }
    pendingRunnable.clear()
    val r = Runnable {
        pendingRunnable.clear()
        Settings.Secure.putStringForUser(
            context.contentResolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            json,
            UserHandle.USER_CURRENT,
        )
    }
    pendingRunnable.add(r)
    settingsHandler.postDelayed(r, 200)
}

private fun flushPending(context: Context, pendingRunnable: MutableList<Runnable>) {
    pendingRunnable.firstOrNull()?.let {
        settingsHandler.removeCallbacks(it)
        it.run()
    }
    pendingRunnable.clear()
}

private fun toSlider(factor: Double): Int = ((factor - 1.0) * 100.0).roundToInt()

private fun convertColorToHex(color: Int): String =
    String.format("%06X", 0xFFFFFF and color)

private fun parseHexColor(hex: String): Int = try {
    android.graphics.Color.parseColor("#$hex")
} catch (e: Exception) {
    DEFAULT_COLOR
}

private fun isBackgroundTintDisallowed(style: String): Boolean =
    style == STYLE_MONOCHROMATIC || style == STYLE_RAINBOW

private fun isBerryBlackActive(context: Context): Boolean {
    if (Settings.Secure.getInt(context.contentResolver, Settings.Secure.BERRY_BLACK_THEME, 0) != 1) {
        return false
    }
    val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightMask == Configuration.UI_MODE_NIGHT_YES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonetSettingsScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val pendingRunnable = remember { mutableListOf<Runnable>() }

    var themeStyle by remember { mutableStateOf(DEFAULT_STYLE) }
    var colorSource by remember { mutableStateOf(COLOR_SOURCE_HOME) }
    var accentColor by remember { mutableIntStateOf(DEFAULT_COLOR) }
    var bgColor by remember { mutableIntStateOf(DEFAULT_COLOR) }
    var luminance by remember { mutableIntStateOf(0) }
    var chroma by remember { mutableIntStateOf(0) }
    var tintBackground by remember { mutableStateOf(false) }
    var fidelity by remember { mutableStateOf(false) }
    var berryBlackActive by remember { mutableStateOf(false) }

    var showAccentPicker by remember { mutableStateOf(false) }
    var showBgPicker by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var showStyleDropdown by remember { mutableStateOf(false) }
    var showSourceDropdown by remember { mutableStateOf(false) }

    fun refreshFromSettings() {
        try {
            val obj = readSettingsJson(context)
            themeStyle = obj.optString(OVERLAY_CATEGORY_THEME_STYLE, DEFAULT_STYLE)

            val source = obj.optString(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_HOME)
            val both = obj.optInt(OVERLAY_COLOR_BOTH, 0) == 1
            colorSource = if (both && source == COLOR_SOURCE_HOME) COLOR_SOURCE_BOTH_VALUE else source

            if (obj.has(OVERLAY_CATEGORY_SYSTEM_PALETTE)) {
                accentColor = parseHexColor(obj.optString(OVERLAY_CATEGORY_SYSTEM_PALETTE))
            }

            if (obj.has(OVERLAY_CATEGORY_BG_COLOR)) {
                bgColor = obj.optInt(OVERLAY_CATEGORY_BG_COLOR, DEFAULT_COLOR)
            }

            luminance = toSlider(obj.optDouble(OVERLAY_LUMINANCE_FACTOR, 1.0))
            chroma = toSlider(obj.optDouble(OVERLAY_CHROMA_FACTOR, 1.0))
            tintBackground = obj.optInt(OVERLAY_TINT_BACKGROUND, 0) == 1
            fidelity = obj.optInt(OVERLAY_FIDELITY, 0) == 1
            berryBlackActive = isBerryBlackActive(context)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read theme overlay settings", e)
        }
    }

    fun putField(mutate: (JSONObject) -> Unit) {
        try {
            val obj = readSettingsJson(context)
            mutate(obj)
            writeSettingsJsonDebounced(context, obj, pendingRunnable)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update theme overlay settings", e)
        }
    }

    DisposableEffect(Unit) {
        refreshFromSettings()
        onDispose { flushPending(context, pendingRunnable) }
    }

    if (showAccentPicker) {
        ColorPickerDialog(
            initialColor = Color(accentColor),
            onDismiss = { showAccentPicker = false },
            onColorSelected = { color ->
                showAccentPicker = false
                val argb = (0xFF shl 24) or
                    ((color.red * 255).toInt() shl 16) or
                    ((color.green * 255).toInt() shl 8) or
                    (color.blue * 255).toInt()
                accentColor = argb
                val hex = convertColorToHex(argb)
                putField { obj ->
                    obj.put(OVERLAY_CATEGORY_ACCENT_COLOR, hex)
                    obj.put(OVERLAY_CATEGORY_SYSTEM_PALETTE, hex)
                }
            },
        )
    }

    if (showWallpaperPicker) {
        WallpaperColorPickerDialog(
            onDismiss = { showWallpaperPicker = false },
            onColorSelected = { color ->
                showWallpaperPicker = false
                val argb = (0xFF shl 24) or
                    ((color.red * 255).toInt() shl 16) or
                    ((color.green * 255).toInt() shl 8) or
                    (color.blue * 255).toInt()
                accentColor = argb
                val hex = convertColorToHex(argb)
                putField { obj ->
                    obj.put(OVERLAY_CATEGORY_ACCENT_COLOR, hex)
                    obj.put(OVERLAY_CATEGORY_SYSTEM_PALETTE, hex)
                    obj.put(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_PRESET)
                    obj.remove(OVERLAY_COLOR_BOTH)
                }
                colorSource = COLOR_SOURCE_PRESET
            },
        )
    }

    if (showBgPicker) {
        ColorPickerDialog(
            initialColor = Color(bgColor),
            onDismiss = { showBgPicker = false },
            onColorSelected = { color ->
                showBgPicker = false
                val argb = (0xFF shl 24) or
                    ((color.red * 255).toInt() shl 16) or
                    ((color.green * 255).toInt() shl 8) or
                    (color.blue * 255).toInt()
                bgColor = argb
                putField { obj -> obj.put(OVERLAY_CATEGORY_BG_COLOR, argb) }
            },
        )
    }

    val isMono = themeStyle == STYLE_MONOCHROMATIC
    val bgTintDisallowed = isBackgroundTintDisallowed(themeStyle) || berryBlackActive
    val chromaEnabled = !isMono
    val fidelityEnabled = !isMono
    val colorSourceEnabled = !isMono
    val accentEnabled = !isMono && colorSource == COLOR_SOURCE_PRESET
    val tintBgEnabled = !bgTintDisallowed
    val bgColorEnabled = tintBgEnabled && tintBackground

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.monet_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        flushPending(context, pendingRunnable)
                        onBackClick()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            MonetSwatchPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // -- Theme style --
                MonetDropdownRow(
                    label = stringResource(R.string.monet_engine_style_title),
                    value = styleDisplayName(themeStyle),
                    expanded = showStyleDropdown,
                    onToggle = { showStyleDropdown = !showStyleDropdown },
                    onDismiss = { showStyleDropdown = false },
                    enabled = true,
                ) {
                    STYLE_ENTRIES.forEach { (value, labelRes) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            leadingIcon = if (value == themeStyle) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            onClick = {
                                showStyleDropdown = false
                                themeStyle = value
                                putField { obj ->
                                    obj.put(OVERLAY_CATEGORY_THEME_STYLE, value)
                                    if (value == STYLE_MONOCHROMATIC) {
                                        obj.remove(OVERLAY_CHROMA_FACTOR)
                                        obj.remove(OVERLAY_FIDELITY)
                                    }
                                    if (isBackgroundTintDisallowed(value)) {
                                        obj.remove(OVERLAY_TINT_BACKGROUND)
                                        obj.remove(OVERLAY_CATEGORY_BG_COLOR)
                                        tintBackground = false
                                    }
                                }
                            },
                        )
                    }
                }

                // -- Color source --
                MonetDropdownRow(
                    label = stringResource(R.string.monet_engine_color_source_title),
                    value = sourceDisplayName(colorSource),
                    expanded = showSourceDropdown,
                    onToggle = {
                        if (colorSourceEnabled) showSourceDropdown = !showSourceDropdown
                    },
                    onDismiss = { showSourceDropdown = false },
                    enabled = colorSourceEnabled,
                    disabledReason = if (isMono) stringResource(R.string.monet_engine_controls_disabled_monochrome) else null,
                ) {
                    SOURCE_ENTRIES.forEach { (value, labelRes) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            leadingIcon = if (value == colorSource) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            onClick = {
                                showSourceDropdown = false
                                colorSource = value
                                putField { obj ->
                                    if (value == COLOR_SOURCE_BOTH_VALUE) {
                                        obj.put(OVERLAY_COLOR_BOTH, 1)
                                        obj.put(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_HOME)
                                    } else {
                                        obj.remove(OVERLAY_COLOR_BOTH)
                                        obj.put(OVERLAY_COLOR_SOURCE, value)
                                    }
                                    obj.put(TIMESTAMP_FIELD, System.currentTimeMillis())
                                    if (value != COLOR_SOURCE_PRESET) {
                                        obj.remove(OVERLAY_CATEGORY_ACCENT_COLOR)
                                        obj.remove(OVERLAY_CATEGORY_SYSTEM_PALETTE)
                                    } else {
                                        val hex = convertColorToHex(accentColor)
                                        obj.put(OVERLAY_CATEGORY_ACCENT_COLOR, hex)
                                        obj.put(OVERLAY_CATEGORY_SYSTEM_PALETTE, hex)
                                    }
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // -- Accent color --
                MonetColorRow(
                    label = stringResource(R.string.monet_engine_custom_color_title),
                    summary = stringResource(R.string.monet_engine_custom_color_summary),
                    color = Color(accentColor),
                    enabled = accentEnabled,
                    disabledReason = if (isMono) stringResource(R.string.monet_engine_controls_disabled_monochrome) else null,
                    onClick = { showAccentPicker = true },
                    onWallpaperPick = { showWallpaperPicker = true },
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // -- Chroma --
                MonetSliderRow(
                    label = stringResource(R.string.monet_engine_chroma_factor_title),
                    summary = if (chromaEnabled) {
                        stringResource(R.string.monet_engine_chroma_factor_summary)
                    } else {
                        stringResource(R.string.monet_engine_controls_disabled_monochrome)
                    },
                    value = chroma,
                    range = -80..100,
                    step = 5,
                    enabled = chromaEnabled,
                    onReset = {
                        chroma = 0
                        putField { obj -> obj.remove(OVERLAY_CHROMA_FACTOR) }
                    },
                    onValueChange = { v ->
                        chroma = v
                        putField { obj ->
                            if (v == 0) obj.remove(OVERLAY_CHROMA_FACTOR)
                            else obj.put(OVERLAY_CHROMA_FACTOR, 1.0 + v / 100.0)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))

                // -- Luminance --
                MonetSliderRow(
                    label = stringResource(R.string.monet_engine_luminance_factor_title),
                    summary = stringResource(R.string.monet_engine_luminance_factor_summary),
                    value = luminance,
                    range = -60..60,
                    step = 5,
                    enabled = true,
                    onReset = {
                        luminance = 0
                        putField { obj -> obj.remove(OVERLAY_LUMINANCE_FACTOR) }
                    },
                    onValueChange = { v ->
                        luminance = v
                        putField { obj ->
                            if (v == 0) obj.remove(OVERLAY_LUMINANCE_FACTOR)
                            else obj.put(OVERLAY_LUMINANCE_FACTOR, 1.0 + v / 100.0)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // -- Fidelity --
                MonetSwitchRow(
                    label = stringResource(R.string.monet_engine_fidelity_title),
                    summary = if (fidelityEnabled) {
                        stringResource(R.string.monet_engine_fidelity_summary)
                    } else {
                        stringResource(R.string.monet_engine_controls_disabled_monochrome)
                    },
                    checked = fidelity,
                    enabled = fidelityEnabled,
                    onCheckedChange = { on ->
                        fidelity = on
                        putField { obj ->
                            if (on) obj.put(OVERLAY_FIDELITY, 1)
                            else obj.remove(OVERLAY_FIDELITY)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))

                // -- Tint background --
                MonetSwitchRow(
                    label = stringResource(R.string.monet_engine_tint_background_title),
                    summary = when {
                        berryBlackActive ->
                            stringResource(R.string.monet_engine_tint_disabled_berry_black)
                        isBackgroundTintDisallowed(themeStyle) && themeStyle == STYLE_MONOCHROMATIC ->
                            stringResource(R.string.monet_engine_tint_disabled_monochrome)
                        isBackgroundTintDisallowed(themeStyle) ->
                            stringResource(R.string.monet_engine_tint_disabled_rainbow)
                        else ->
                            stringResource(R.string.monet_engine_tint_background_summary)
                    },
                    checked = tintBackground,
                    enabled = tintBgEnabled,
                    onCheckedChange = { on ->
                        tintBackground = on
                        putField { obj ->
                            if (on) obj.put(OVERLAY_TINT_BACKGROUND, 1)
                            else obj.remove(OVERLAY_TINT_BACKGROUND)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))

                // -- Background color --
                MonetColorRow(
                    label = stringResource(R.string.monet_engine_color_override_title),
                    summary = when {
                        berryBlackActive ->
                            stringResource(R.string.monet_engine_bg_color_disabled_berry_black)
                        isBackgroundTintDisallowed(themeStyle) ->
                            stringResource(R.string.monet_engine_bg_color_disabled_style)
                        else -> null
                    },
                    color = Color(bgColor),
                    enabled = bgColorEnabled,
                    disabledReason = null,
                    onClick = { showBgPicker = true },
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─── Preview ───────────────────────────────────────────────────────────────────

@Composable
fun MonetSwatchPreview(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme

    val swatches = listOf(
        scheme.primary to stringResource(R.string.monet_engine_preview_swatch_primary),
        scheme.secondary to stringResource(R.string.monet_engine_preview_swatch_secondary),
        scheme.tertiary to stringResource(R.string.monet_engine_preview_swatch_tertiary),
        scheme.surface to stringResource(R.string.monet_engine_preview_swatch_surface),
        scheme.error to stringResource(R.string.monet_engine_preview_swatch_error),
        scheme.outline to stringResource(R.string.monet_engine_preview_swatch_outline),
        scheme.inverseSurface to stringResource(R.string.monet_engine_preview_swatch_inverse),
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                swatches.forEach { (color, _) ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                }
            }
        }
    }
}

/** Compact swatch row for the main browse card (no rectangles, just circles). */
@Composable
fun MonetSwatchCompact(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.primaryContainer,
        scheme.error,
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

// ─── Setting rows ──────────────────────────────────────────────────────────────

@Composable
private fun MonetDropdownRow(
    label: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    enabled: Boolean,
    disabledReason: String? = null,
    content: @Composable () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            Box {
                OutlinedCard(
                    onClick = onToggle,
                    enabled = enabled,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                    content()
                }
            }
        }
        if (!enabled && disabledReason != null) {
            Text(
                text = disabledReason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun MonetColorRow(
    label: String,
    summary: String?,
    color: Color,
    enabled: Boolean,
    disabledReason: String?,
    onClick: () -> Unit,
    onWallpaperPick: (() -> Unit)? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                )
                if (summary != null || (!enabled && disabledReason != null)) {
                    Text(
                        text = (if (!enabled && disabledReason != null) disabledReason else summary).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onWallpaperPick != null && enabled) {
                    IconButton(onClick = onWallpaperPick) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (enabled) color else color.copy(alpha = 0.38f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun MonetSliderRow(
    label: String,
    summary: String,
    value: Int,
    range: IntRange,
    step: Int,
    enabled: Boolean,
    onReset: () -> Unit,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = onReset,
                    enabled = enabled,
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Reset",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f),
                    )
                }
                Text(
                    text = "${if (value > 0) "+" else ""}$value%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp),
                )
            }
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val stepped = (raw / step).roundToInt() * step
                val v = stepped.coerceIn(range.first, range.last)
                onValueChange(v)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
        )
    }
}

@Composable
private fun MonetSwitchRow(
    label: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled,
        )
    }
}

// ─── Data ──────────────────────────────────────────────────────────────────────

private val STYLE_ENTRIES = listOf(
    "TONAL_SPOT" to R.string.monet_engine_style_tonal_spot,
    "VIBRANT" to R.string.monet_engine_style_vibrant,
    "EXPRESSIVE" to R.string.monet_engine_style_expressive,
    "SPRITZ" to R.string.monet_engine_style_spritz,
    "RAINBOW" to R.string.monet_engine_style_rainbow,
    "FRUIT_SALAD" to R.string.monet_engine_style_fruit_salad,
    "MONOCHROMATIC" to R.string.monet_engine_style_monochromatic,
)

private val SOURCE_ENTRIES = listOf(
    COLOR_SOURCE_BOTH_VALUE to R.string.monet_engine_color_source_both,
    COLOR_SOURCE_HOME to R.string.monet_engine_color_source_home,
    "lock_wallpaper" to R.string.monet_engine_color_source_lock,
    COLOR_SOURCE_PRESET to R.string.monet_engine_color_source_preset,
)

@Composable
private fun styleDisplayName(value: String): String {
    val entry = STYLE_ENTRIES.find { it.first == value }
    return if (entry != null) stringResource(entry.second) else value
}

@Composable
private fun sourceDisplayName(value: String): String {
    val entry = SOURCE_ENTRIES.find { it.first == value }
    return if (entry != null) stringResource(entry.second) else value
}
