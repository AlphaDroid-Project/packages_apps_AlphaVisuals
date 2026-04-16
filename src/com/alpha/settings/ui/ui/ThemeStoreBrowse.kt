/*
 * Copyright (C) 2025-2026 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.alpha.settings.ui.ui

import com.alpha.settings.ui.data.model.StandardComponents
import com.alpha.settings.ui.data.model.Theme
import com.alpha.settings.ui.data.model.ThemeCategory
import com.alpha.settings.ui.ui.components.PreviewDimensions

private const val ANDROID_THEME_CUSTOMIZATION_PREFIX = "android.theme.customization."

/**
 * Main [ThemeStoreScreen] browse: which catalog slices appear and in what order rows sort.
 * [includeThemeOnMainBrowse] is used by [com.alpha.settings.ui.viewmodel.ThemeStoreViewModel] so
 * browse rows are derived only from theme-store UI state (themes / search / category), not from
 * per-theme install-state updates.
 */
object ThemeStoreMainBrowsePolicy {
    const val OMIT_ICON_PACK_SECTION: Boolean = false
    const val OMIT_LOCKSCREEN_CLOCK_AND_SYSTEM_FONT_SECTIONS: Boolean = false

    fun browseCategoryKey(theme: Theme): String =
        PreviewDimensions.normalizeStoreCategoryForPreview(
            theme.category.removePrefix(ANDROID_THEME_CUSTOMIZATION_PREFIX),
        )

    fun includeThemeOnMainBrowse(theme: Theme): Boolean =
        when (browseCategoryKey(theme)) {
            "icon_packs" -> !OMIT_ICON_PACK_SECTION
            "font", "lockscreen_clock_font" -> !OMIT_LOCKSCREEN_CLOCK_AND_SYSTEM_FONT_SECTIONS
            "ui_style" -> false
            else -> true
        }
}

/**
 * Sections with at most this many themes use a non-lazy horizontal [Row] on the main store so
 * vertical scroll does not pay nested [androidx.compose.foundation.lazy.LazyRow] measurement.
 */
internal const val MAIN_STORE_THEME_STRIP_STATIC_ROW_MAX = 12

/** Browse section order (unknown categories sort after, alphabetically). */
internal val MAIN_STORE_BROWSE_CATEGORY_ORDER = listOf(
    "ui_style",
    "battery_style",
    "navbar",
    "lockscreen_clock_font",
    "font",
    "wifi_icons",
    "signal_icons",
    "icon_packs",
    "charging_animation",
    "back_gesture",
)

internal fun buildOrderedMainStoreCategoryRows(filteredThemes: List<Theme>): List<Pair<String, List<Theme>>> =
    filteredThemes
        .groupBy { it.category }
        .entries
        .sortedWith(
            compareBy<Map.Entry<String, List<Theme>>> { e ->
                val orderKey = PreviewDimensions.normalizeStoreCategoryForPreview(e.key)
                val i = MAIN_STORE_BROWSE_CATEGORY_ORDER.indexOf(orderKey)
                if (i >= 0) i else 1000
            }.thenBy { PreviewDimensions.normalizeStoreCategoryForPreview(it.key) },
        )
        .map { it.key to it.value }

/**
 * Categories up to and including charging animation when present, else up to battery style
 * (main store lists charging + back gesture directly under Wi‑Fi icons).
 */
internal fun browseCategoriesUpToBatteryStyle(categories: List<ThemeCategory>): List<ThemeCategory> {
    val charging = categories.indexOfFirst { it.id == StandardComponents.CHARGING_ANIMATION }
    if (charging >= 0) return categories.take(charging + 1)
    val battery = categories.indexOfFirst { it.id == StandardComponents.BATTERY_STYLE }
    return if (battery >= 0) categories.take(battery + 1) else categories
}

/**
 * Maps a [theme_engine_data] / ThemeEngine category key to the store's theme category id
 * (e.g. `signal_icons`, `wifi_icons`, `icon_packs`).
 */
internal fun storeCategoryIdForEngineCategoryKey(engineKey: String): String? {
    val raw = engineKey.lowercase()
    val s = if (raw.startsWith(ANDROID_THEME_CUSTOMIZATION_PREFIX)) {
        raw.removePrefix(ANDROID_THEME_CUSTOMIZATION_PREFIX)
    } else {
        raw
    }
    return when {
        s.contains("icon_pack") -> "icon_packs"
        s.contains("back_gesture") -> "back_gesture"
        s.contains("charging_animation") ||
            (s.contains("charging") && s.contains("animation")) -> "charging_animation"
        s.contains("battery_style") -> "battery_style"
        s.contains("lockscreen_clock_font") -> "lockscreen_clock_font"
        s == "font" -> "font"
        s.contains("wifi") || s.contains("wifibar") -> "wifi_icons"
        s.contains("signal") || s == "signal" -> "signal_icons"
        else -> null
    }
}

/**
 * Keeps only active overlay rows whose category belongs to a browse section at or before Battery Style.
 */
internal fun filterCategoryThemesForThemeStoreBrowse(
    categoryThemes: Map<String, String>,
    allCategories: List<ThemeCategory>,
    browseUpToBattery: List<ThemeCategory>
): Map<String, String> {
    if (allCategories.isEmpty()) return categoryThemes
    if (browseUpToBattery.size >= allCategories.size) return categoryThemes
    val allowed = browseUpToBattery.map { it.id }.toSet()
    return categoryThemes.filterKeys { key ->
        val bucket = storeCategoryIdForEngineCategoryKey(key)
        bucket == null || bucket in allowed
    }
}
