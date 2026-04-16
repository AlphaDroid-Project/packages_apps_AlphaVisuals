/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alpha.settings.ui.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Store preview dimensions for the theme list.
 *
 * **Main browse screen** — consistent layout: one **preview square** ([MainListGlyphSlot] from parent
 * constraints), **ContentScale.Fit** (FIT_CENTER) for every raster/vector preview, and a single
 * base drawable size ([MainListIconSize]) with optional per-category scaling (see
 * [mainListIconSizeForCategory]).
 * Detail page may use different layouts / animations.
 *
 * - **Drawable box:** [mainListIconSizeForCategory] (defaults to [MainListIconSize]) —
 *   `Modifier.size(...)` for previews on the main list.
 *
 * **Main list**
 * - **Card preview area:** [MainListHorizontalPreviewHeight] / [MainListCategoryPreviewSize] set the
 *   outer `Modifier` on [ThemeStoreItemPreview]; [MainListGlyphSlot] fills that and uses a **square**
 *   with side = `min(width, height)` (see [MainListPreviewStandardSize] for strip/tile padding math).
 *
 * **Previews by type**
 * - **Wi‑Fi / signal / ui_style** — [ThemePackagePreview] `DrawableIcon`.
 * - **Icon packs** — main list [IconPackPreview]; detail [IconPackDetailTripletPreview] (BT / Location / NFC).
 * - **Navbar** — compact [NavbarPreview].
 * - **Charging static** — [ChargingAnimationStaticPreview].
 * - **Battery** — [BatteryStylePreview] paths capped by `iconSize` on main list (detail: full bounds).
 * - **App icon pack grid** — [AppIconPackPreview] `AppIcon` (grid uses its own tile sizes).
 * - **Lock clock font / system font** — [LockClockFontPreview] `TextView` (no Image scale).
 */
object PreviewDimensions {
    /**
     * Used with paddings for [MainListHorizontalPreviewHeight] / [MainListCategoryPreviewSize].
     * The on-screen preview square follows parent constraints — see [MainListGlyphSlot].
     */
    val MainListPreviewStandardSize = 52.dp

    /**
     * Single drawable layout size on the **main** theme list for every category (UI consistency).
     * Tune here to resize all main-row previews at once.
     */
    val MainListIconSize = 48.dp

    /** Vertical padding above/below the preview strip on horizontal theme cards. */
    private val MainListHorizontalPreviewVerticalPadding = 6.dp
    /** Outer preview tile on category list rows (padding around the glyph). */
    private val MainListCategoryPreviewOuterPadding = 8.dp

    /** Preview strip height on horizontal theme cards (limits [MainListGlyphSlot] height there). */
    val MainListHorizontalPreviewHeight = MainListPreviewStandardSize + MainListHorizontalPreviewVerticalPadding

    /** Preview tile on category list rows (non-animation). */
    val MainListCategoryPreviewSize = MainListPreviewStandardSize + MainListCategoryPreviewOuterPadding

    /**
     * [ThemeRepository] maps overlay categories to store ids (e.g. `wifi_icon` → `wifi_icons`) so PM
     * and catalog themes share one section. Preview scaling and [ThemeStoreItemPreview] use this.
     */
    fun normalizeStoreCategoryForPreview(category: String): String = when (category) {
        "wifi_icon" -> "wifi_icons"
        "signal_icon" -> "signal_icons"
        else -> category
    }

    private val mainListIconCategoryScale = mapOf(
        "wifi_icons" to 0.65f,
        "signal_icons" to 0.65f,
        "icon_packs" to 0.8f,
        "navbar" to 0.8f,
    )

    /** Main list inner icon size; categories not listed use [MainListIconSize]. */
    fun mainListIconSizeForCategory(category: String): Dp {
        val c = normalizeStoreCategoryForPreview(category)
        val m = mainListIconCategoryScale[c] ?: 1f
        return MainListIconSize * m
    }

    /**
     * Main list tile: \"Aa\" thumbnail (aligned with ThemePicker `font_component_option_thumbnail`).
     */
    const val ClockFontPreviewCompactThumbnailSp = 28f
    /**
     * Detail hero: max starting size for `ABC • abc • 123` before autosize; [LockClockFontPreview]
     * shrinks uniformly to fit width ([ClockFontPreviewDetailTitleMinSp] floor).
     */
    const val ClockFontPreviewDetailTitleSp = 18f
    /** Minimum text size (sp) when autosizing the detail clock-font line. */
    const val ClockFontPreviewDetailTitleMinSp = 8f
    /** Detail hero: subtitle under the accent divider. */
    const val ClockFontPreviewDetailBodySp = 18f

    /** Theme store main screen: gap below the search field before the first category row. */
    val ThemeStoreSearchToFirstRowSpacing = 32.dp

    /** Theme store main screen: vertical gap between category sections. */
    val ThemeStoreSectionSpacing = 24.dp

    /** Theme store: gap between section title and horizontal card strip. */
    val ThemeStoreSectionLabelToCardsSpacing = 6.dp

    /** Minimum height for [com.alpha.settings.ui.ui.components.QsTileUiStylePreview] when unconstrained. */
    val UiStyleTileMinHeight = 56.dp
    /** UI-style preview on theme store section cards. */
    val UiStyleTileSectionHeight = 56.dp
    /** UI-style preview on the style detail / editor screen (paired row). */
    val UiStyleTileDetailHeight = 68.dp
    /** [UiStyleHeroPreview] large single tile. */
    val UiStyleTileHeroLargeHeight = 80.dp
    /** [UiStyleHeroPreview] bottom row tiles. */
    val UiStyleTileHeroSmallHeight = 56.dp

    /**
     * Theme detail hero: **2×** [MainListHorizontalPreviewHeight] so the detail glyph area reads
     * larger than the main list strip. Charging animations keep their own layout.
     */
    val DetailStandardPreviewHeight = MainListHorizontalPreviewHeight * 2

    /** Scale factor for compact preview drawables on theme detail vs main list. */
    const val DetailCompactIconScale = 2f
}
